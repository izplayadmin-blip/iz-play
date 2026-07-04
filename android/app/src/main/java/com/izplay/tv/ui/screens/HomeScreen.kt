package com.izplay.tv.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.izplay.tv.data.model.Channel
import com.izplay.tv.ui.MainViewModel
import com.izplay.tv.ui.UiState
import com.izplay.tv.ui.components.*
import com.izplay.tv.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PAGE_PADDING = 28.dp

@Composable
fun HomeScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var nav by remember { mutableStateOf(NavItem.INICIO) }
    var drawerOpen by remember { mutableStateOf(false) }

    val now = Date()
    val clock = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(now)
    val date = SimpleDateFormat("EEE dd/MM", Locale("pt", "BR")).format(now).uppercase()

    Row(Modifier.fillMaxSize().background(PanelBlack)) {
        Sidebar(
            selected = nav,
            clock = clock,
            date = date,
            onSelect = { item ->
                nav = item
                when (item) {
                    NavItem.CANAIS -> drawerOpen = true
                    NavItem.FAVORITOS -> vm.selectCategory(UiState.FAVORITES_ID)
                    else -> Unit
                }
            }
        )

        Box(Modifier.weight(1f).fillMaxHeight().focusGroup()) {
            when (nav) {
                NavItem.INICIO -> StartDashboard(vm, state) { nav = it }
                NavItem.FILMES -> MoviesScreen(vm)
                NavItem.SERIES -> SeriesScreen(vm)
                NavItem.CONFIG -> SettingsScreen(vm, state)
                NavItem.NOTIFICACOES -> PlaceholderScreen(
                    "Notificações",
                    "Avisos do suporte, novidades e alertas de lista.",
                    Icons.Filled.Notifications
                )
                NavItem.USUARIOS -> PlaceholderScreen(
                    "Usuários",
                    "Perfis, avatar e recomendações por pessoa.",
                    Icons.Filled.Person
                )
                else -> LiveTvContent(vm, state, drawerOpen) { drawerOpen = it }
            }
        }
    }
}

@Composable
private fun StartDashboard(
    vm: MainViewModel,
    state: UiState,
    onOpen: (NavItem) -> Unit
) {
    LaunchedEffect(Unit) {
        vm.loadVodIfNeeded()
        vm.loadSeriesIfNeeded()
    }

    val heroMovie = state.allVod.firstOrNull()
    val heroSeries = state.allSeries.firstOrNull()
    val recentChannels = state.recentChannelIds.mapNotNull { id -> state.allChannels.firstOrNull { it.id == id } }
    val hotChannels = state.allChannels.take(12)
    val hotMovies = state.allVod.take(14)
    val hotSeries = state.allSeries.take(14)

    LazyColumn(
        Modifier.fillMaxSize().background(PanelBlack),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            HeroBanner(
                title = heroMovie?.name ?: heroSeries?.name ?: state.selectedChannel?.name ?: "IZ Play",
                subtitle = "Seu entretenimento, seus perfis e uma experiência adaptada ao aparelho.",
                imageUrl = heroMovie?.posterUrl ?: heroSeries?.coverUrl ?: state.selectedChannel?.logoUrl,
                onWatch = {
                    when {
                        heroMovie != null -> onOpen(NavItem.FILMES)
                        heroSeries != null -> onOpen(NavItem.SERIES)
                        else -> onOpen(NavItem.CANAIS)
                    }
                }
            )
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_PADDING, vertical = 16.dp)
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                QuickAction("Canais de TV", Icons.Filled.LiveTv, Modifier.weight(1f)) { onOpen(NavItem.CANAIS) }
                QuickAction("Filmes", Icons.Filled.Movie, Modifier.weight(1f)) { onOpen(NavItem.FILMES) }
                QuickAction("Séries", Icons.Filled.Tv, Modifier.weight(1f)) { onOpen(NavItem.SERIES) }
                QuickAction("Usuários", Icons.Filled.Person, Modifier.weight(1f)) { onOpen(NavItem.USUARIOS) }
            }
        }

        if (recentChannels.isNotEmpty()) {
            item {
                ContentRail("Continuar assistindo", "PARA VOCÊ") {
                    items(recentChannels, key = { it.id }) { ch ->
                        ChannelTile(ch) { vm.selectChannel(ch); onOpen(NavItem.CANAIS) }
                    }
                }
            }
        }

        item {
            ContentRail("Mais assistidos no IZ Play", "AO VIVO") {
                items(hotChannels, key = { it.id }) { ch ->
                    ChannelTile(ch) { vm.selectChannel(ch); onOpen(NavItem.CANAIS) }
                }
            }
        }

        if (hotMovies.isNotEmpty()) {
            item {
                ContentRail("Filmes em destaque", "HOT") {
                    items(hotMovies, key = { it.id }) { vod ->
                        PosterTile(vod.name, vod.posterUrl, vod.rating) { onOpen(NavItem.FILMES) }
                    }
                }
            }
        }

        if (hotSeries.isNotEmpty()) {
            item {
                ContentRail("Séries que estão bombando", "HOT") {
                    items(hotSeries, key = { it.id }) { series ->
                        PosterTile(series.name, series.coverUrl, series.rating) { onOpen(NavItem.SERIES) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBanner(title: String, subtitle: String, imageUrl: String?, onWatch: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(PanelDarker)
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(PanelBlack, PanelBlack.copy(alpha = 0.82f), Color.Transparent))
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, PanelBlack), startY = 90f)
            )
        )
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = PAGE_PADDING, end = 24.dp)
                .widthIn(max = 720.dp)
        ) {
            Text("CONTINUE ASSISTINDO", color = IzYellow, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            Text(title, color = TextPrimary, fontSize = 40.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Text(subtitle, color = TextSecondary, fontSize = 15.sp, maxLines = 2)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroButton("Assistir", Icons.Filled.PlayArrow, Color.White, Color.Black, onWatch)
                HeroButton("Explorar catálogo", Icons.Filled.Movie, PanelElevated, TextPrimary, onWatch)
            }
        }
    }
}

@Composable
private fun HeroButton(label: String, icon: ImageVector, background: Color, foreground: Color, onClick: () -> Unit) {
    TvCard(onClick = onClick, shape = RoundedCornerShape(8.dp)) {
        Row(
            Modifier.height(46.dp).background(background).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = foreground, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = foreground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TvCard(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(76.dp)
                .background(Brush.horizontalGradient(listOf(IzRed, IzRedDark)))
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Text(label.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ContentRail(title: String, kicker: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Row(
            Modifier.padding(start = PAGE_PADDING, bottom = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Text(kicker, color = IzYellow, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.5.sp)
        }
        LazyRow(
            Modifier.fillMaxWidth().focusGroup(),
            contentPadding = PaddingValues(horizontal = PAGE_PADDING),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ChannelTile(channel: Channel, onClick: () -> Unit) {
    TvCard(onClick = onClick) {
        Row(
            Modifier.width(232.dp).height(84.dp).background(PanelElevated).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(model = channel.logoUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(48.dp))
                } else {
                    Text(channel.name.take(2).uppercase(), color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(channel.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text("%03d • Ao vivo".format(channel.number), color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun PosterTile(title: String, imageUrl: String?, rating: String?, onClick: () -> Unit) {
    TvCard(onClick = onClick) {
        Column(Modifier.width(134.dp).background(PanelElevated)) {
            Box(Modifier.fillMaxWidth().height(190.dp).background(Color.Black)) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(model = imageUrl, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.PlayArrow, null, tint = TextSecondary, modifier = Modifier.size(42.dp).align(Alignment.Center))
                }
                if (!rating.isNullOrBlank()) {
                    Row(
                        Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.72f)).padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Star, null, tint = IzYellow, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(rating, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(9.dp))
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, subtitle: String, icon: ImageVector) {
    Box(Modifier.fillMaxSize().background(PanelBlack), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(86.dp).clip(RoundedCornerShape(24.dp)).background(IzRed), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(42.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 32.sp)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, color = TextSecondary, fontSize = 15.sp)
        }
    }
}

@Composable
private fun LiveTvContent(
    vm: MainViewModel,
    state: UiState,
    drawerOpen: Boolean,
    onDrawerToggle: (Boolean) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            val catLabel = when (state.selectedCategoryId) {
                UiState.FAVORITES_ID -> "FAVORITOS"
                else -> state.categories.firstOrNull { it.id == state.selectedCategoryId }?.name ?: "—"
            }
            ChannelColumn(
                categoryLabel = catLabel,
                channels = state.visibleChannels,
                selectedChannelId = state.selectedChannel?.id,
                totalLabel = "${state.visibleChannels.size} canais",
                searchQuery = state.searchQuery,
                onSearch = vm::setSearch,
                onOpenCategories = { onDrawerToggle(true) },
                onSelect = vm::selectChannel
            )

            Box(Modifier.weight(1f).fillMaxHeight()) {
                when {
                    state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = IzRed)
                    }
                    state.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Erro ao carregar", color = TextPrimary, fontSize = 18.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(state.error, color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    else -> {
                        val ch = state.selectedChannel
                        PlayerPanel(
                            channel = ch,
                            isFavorite = ch != null && ch.id in state.favorites,
                            epg = state.epg,
                            epgLoading = state.epgLoading,
                            onToggleFavorite = { ch?.let(vm::toggleFavorite) },
                            onReconnect = { ch?.let(vm::selectChannel) },
                            onFullscreen = {}
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = drawerOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDrawerToggle(false) }
            )
        }
        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it })
        ) {
            CategoryColumn(
                categories = state.categories,
                favoritesCount = state.favorites.size,
                selectedId = state.selectedCategoryId,
                onSelect = {
                    vm.selectCategory(it)
                    onDrawerToggle(false)
                },
                onClose = { onDrawerToggle(false) }
            )
        }
    }
}
