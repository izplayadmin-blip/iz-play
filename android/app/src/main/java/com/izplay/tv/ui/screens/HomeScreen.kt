package com.izplay.tv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.izplay.tv.data.model.Channel
import com.izplay.tv.data.model.SeriesItem
import com.izplay.tv.data.model.VodItem
import com.izplay.tv.player.VideoPlayer
import com.izplay.tv.ui.MainViewModel
import com.izplay.tv.ui.UiState
import com.izplay.tv.ui.ConnectionQuality
import com.izplay.tv.ui.components.*
import com.izplay.tv.ui.theme.*
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private val PAGE_PADDING = 44.dp
private const val HOME_RAIL_LIMIT = 8

private enum class HomePosterType { MOVIE, SERIES }

private data class HomePosterEntry(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val rating: String?,
    val type: HomePosterType
)

private fun recommendationKey(title: String): String {
    val normalized = Normalizer.normalize(title.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("\\b(19|20)\\d{2}\\b"), " ")
        .replace(Regex("\\b(s|t)\\d{1,2}\\b"), " ")
        .replace(Regex("\\b(temporada|season|volume|vol|parte)\\s*\\d*\\b"), " ")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
    val words = normalized.split(' ').filter {
        it.length > 2 && it !in setOf("das", "dos", "uma", "para", "com", "globoplay")
    }
    return when {
        words.isEmpty() -> normalized
        words.first() == "malhacao" -> "malhacao"
        else -> words.take(3).joinToString(" ")
    }
}

private fun deduplicatePosters(items: List<HomePosterEntry>, limit: Int = HOME_RAIL_LIMIT): List<HomePosterEntry> {
    val used = HashSet<String>()
    val result = ArrayList<HomePosterEntry>(limit)
    for (item in items) {
        if (used.add(recommendationKey(item.title))) result += item
        if (result.size == limit) break
    }
    return result
}

private fun formatRatingPtBr(value: String?): String? {
    val number = value?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it > 0.0 } ?: return null
    return String.format(Locale("pt", "BR"), "%.1f", number)
}

@Composable
fun HomeScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var nav by remember { mutableStateOf(NavItem.INICIO) }
    var drawerOpen by remember { mutableStateOf(false) }
    val selectedSidebarFocus = remember { FocusRequester() }
    var homeEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(70)
        homeEntered = true
    }
    val homeEntrance by animateFloatAsState(
        targetValue = if (homeEntered) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "homeEntrance"
    )

    // Um unico dono define o foco inicial. A Sidebar nao disputa mais o foco
    // com o conteudo enquanto os catalogos e imagens terminam de recompor.
    LaunchedEffect(state.startupLoading) {
        if (!state.startupLoading) {
            delay(100)
            runCatching { selectedSidebarFocus.requestFocus() }
        }
    }

    val now = Date()
    val clock = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(now)
    val date = SimpleDateFormat("EEE dd/MM", Locale("pt", "BR")).format(now).uppercase()

    Box(Modifier.fillMaxSize().background(PanelBlack)) {
        Row(
            Modifier.fillMaxSize().graphicsLayer {
                alpha = homeEntrance
            }
        ) {
            Sidebar(
                selected = nav,
                clock = clock,
                date = date,
                profileName = state.activeProfile?.name ?: "Perfil",
                profileAvatar = state.activeProfile?.avatar ?: 0,
                selectedFocusRequester = selectedSidebarFocus,
                onSelect = { item ->
                    nav = item
                    if (item != NavItem.CANAIS) drawerOpen = false
                    when (item) {
                        NavItem.CANAIS -> {
                            vm.loadLiveIfNeeded()
                            drawerOpen = true
                        }
                        NavItem.FAVORITOS -> vm.loadHomeCatalogs()
                        else -> Unit
                    }
                }
            )

            Box(Modifier.weight(1f).fillMaxHeight().focusGroup()) {
                when (nav) {
                    NavItem.INICIO -> StartDashboard(vm, state) { nav = it }
                    NavItem.FAVORITOS -> FavoritesLibraryScreen(vm, state)
                    NavItem.FILMES -> MoviesScreen(vm)
                    NavItem.SERIES -> SeriesScreen(vm)
                    NavItem.CONFIG -> SettingsScreen(
                        vm = vm,
                        state = state,
                        onExitToSidebar = {
                            runCatching { selectedSidebarFocus.requestFocus() }
                        },
                    )
                    NavItem.NOTIFICACOES -> PlaceholderScreen(
                        "Notificacoes",
                        "Avisos do suporte, novidades e alertas de lista.",
                        Icons.Filled.Notifications
                    )
                    NavItem.USUARIOS -> UserScreen(vm, state)
                    else -> LiveTvContent(vm, state, drawerOpen) { drawerOpen = it }
                }
            }
        }

        // ── Overlays globais (cobrem a sidebar; Voltar fecha) ──────────────
        state.selectedVod?.let { vod ->
            MovieDetailScreen(
                vod = vod,
                loading = state.vodDetailLoading,
                isFavorite = vod.id in state.vodFavorites,
                onPlay = { vm.playVod(vod) },
                onToggleFavorite = { vm.toggleVodFavorite(vod) },
                onClose = { vm.selectVod(null) }
            )
        }
        state.selectedSeries?.let { series ->
            SeriesDetailScreen(
                series = series,
                seasons = state.seriesSeasons,
                loading = state.seriesDetailLoading,
                isFavorite = series.id in state.seriesFavorites,
                onPlayEpisode = { ep -> vm.playEpisode(series, ep) },
                onToggleFavorite = { vm.toggleSeriesFavorite(series) },
                onClose = { vm.selectSeries(null) }
            )
        }
        state.playing?.let { media ->
            FullscreenPlayback(
                streamUrl = media.url,
                title = media.title,
                subtitle = media.subtitle,
                fallbackUrl = media.fallbackUrl,
                initialPositionMs = media.initialPositionMs,
                onProgress = vm::recordPlaybackProgress,
                onClose = vm::stopPlayback
            )
        }
    }
}

private enum class FavoriteFilter(val label: String) {
    ALL("TODOS"), CHANNELS("CANAIS"), MOVIES("FILMES"), SERIES("SERIES")
}

@Composable
private fun FavoritesLibraryScreen(vm: MainViewModel, state: UiState) {
    val channels = state.allChannels.filter { it.id in state.favorites }
    val movies = state.allVod.filter { it.id in state.vodFavorites }
    val series = state.allSeries.filter { it.id in state.seriesFavorites }
    var filter by remember { mutableStateOf(FavoriteFilter.ALL) }
    val selectedFilterEmpty = when (filter) {
        FavoriteFilter.ALL -> channels.isEmpty() && movies.isEmpty() && series.isEmpty()
        FavoriteFilter.CHANNELS -> channels.isEmpty()
        FavoriteFilter.MOVIES -> movies.isEmpty()
        FavoriteFilter.SERIES -> series.isEmpty()
    }

    if (channels.isEmpty() && movies.isEmpty() && series.isEmpty()) {
        Box(Modifier.fillMaxSize().background(PanelBlack), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Star, null, tint = IzYellow, modifier = Modifier.size(54.dp))
                Spacer(Modifier.height(14.dp))
                Text("Seus favoritos", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(7.dp))
                Text("Favorite canais, filmes e series para encontra-los aqui.", color = TextSecondary, fontSize = 14.sp)
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(PanelBlack).padding(horizontal = 28.dp, vertical = 20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(28.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(IzRed))
                    Spacer(Modifier.width(10.dp))
                    Text("SUA BIBLIOTECA", color = IzRed, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    "Favoritos de ${state.activeProfile?.name ?: "você"}",
                    color = TextPrimary,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "${channels.size} canais  •  ${movies.size} filmes  •  ${series.size} séries",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Row(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FavoriteFilter.entries.forEach { item ->
                    FavoriteFilterButton(item.label, filter == item) { filter = item }
                }
            }
        }
        Spacer(Modifier.height(18.dp))

        if (selectedFilterEmpty) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum conteúdo salvo neste filtro.", color = TextSecondary, fontSize = 14.sp)
            }
        } else when (filter) {
            FavoriteFilter.ALL -> FavoritesOverview(vm, state, channels, movies, series)
            FavoriteFilter.CHANNELS -> FavoriteChannelsPane(vm, state, channels, Modifier.fillMaxSize())
            FavoriteFilter.MOVIES -> FavoritePosterGrid(
                title = "FILMES FAVORITOS",
                items = movies.map { FavoritePosterData(it.id, it.name, it.posterUrl, it.rating) },
                onClick = { id -> movies.firstOrNull { it.id == id }?.let(vm::selectVod) }
            )
            FavoriteFilter.SERIES -> FavoritePosterGrid(
                title = "SÉRIES FAVORITAS",
                items = series.map { FavoritePosterData(it.id, it.name, it.coverUrl, it.rating) },
                onClick = { id -> series.firstOrNull { it.id == id }?.let(vm::selectSeries) }
            )
        }
    }
}

private data class FavoritePosterData(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val rating: String?
)

@Composable
private fun FavoritesOverview(
    vm: MainViewModel,
    state: UiState,
    channels: List<Channel>,
    movies: List<com.izplay.tv.data.model.VodItem>,
    series: List<com.izplay.tv.data.model.SeriesItem>
) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        if (channels.isNotEmpty()) {
            FavoriteChannelsPane(vm, state, channels, Modifier.weight(1.08f).fillMaxHeight())
        } else {
            FavoriteEmptyColumn("CANAIS FAVORITOS", Modifier.weight(1.08f).fillMaxHeight())
        }
        FavoritePosterColumn(
            title = "FILMES FAVORITOS",
            emptyLabel = "Nenhum filme salvo",
            items = movies.map { FavoritePosterData(it.id, it.name, it.posterUrl, it.rating) },
            modifier = Modifier.weight(0.62f).fillMaxHeight(),
            onClick = { id -> movies.firstOrNull { it.id == id }?.let(vm::selectVod) }
        )
        FavoritePosterColumn(
            title = "SÉRIES FAVORITAS",
            emptyLabel = "Nenhuma série salva",
            items = series.map { FavoritePosterData(it.id, it.name, it.coverUrl, it.rating) },
            modifier = Modifier.weight(0.62f).fillMaxHeight(),
            onClick = { id -> series.firstOrNull { it.id == id }?.let(vm::selectSeries) }
        )
    }
}

@Composable
private fun FavoriteChannelsPane(
    vm: MainViewModel,
    state: UiState,
    channels: List<Channel>,
    modifier: Modifier = Modifier
) {
    val selected = state.selectedChannel?.takeIf { current -> channels.any { it.id == current.id } }
        ?: channels.firstOrNull()
    LaunchedEffect(channels.firstOrNull()?.id) {
        if (state.selectedChannel?.id !in channels.map { it.id }) channels.firstOrNull()?.let(vm::selectChannel)
    }
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(PanelDark).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("CANAIS FAVORITOS", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text("${channels.size} AO VIVO", color = IzRed, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().weight(0.50f).clip(RoundedCornerShape(12.dp)).background(Color.Black)
        ) {
            VideoPlayer(
                streamUrl = selected?.streamUrl,
                fallbackUrl = state.streamFallback(selected?.streamUrl),
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Column {
                    Text(selected?.name ?: "Selecione um canal", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    val current = state.epg.firstOrNull { System.currentTimeMillis() in it.start..it.end }
                    Text(current?.title ?: "Ao vivo", color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            Modifier.fillMaxWidth().weight(0.42f).focusGroup(),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                FavoriteChannelRow(channel, selected?.id == channel.id) { vm.selectChannel(channel) }
            }
        }
        Spacer(Modifier.height(9.dp))
        TvCard(
            onClick = { selected?.let { vm.playLive(it.streamUrl, it.name) } },
            shape = RoundedCornerShape(10.dp),
            focusScale = 1.02f
        ) { focused ->
            Box(
                Modifier.fillMaxWidth().height(43.dp)
                    .background(if (focused) IzRed else IzRed.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                Text("ASSISTIR EM TELA CHEIA", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun FavoriteChannelRow(channel: Channel, selected: Boolean, onClick: () -> Unit) {
    TvCard(onClick = onClick, shape = RoundedCornerShape(10.dp), focusScale = 1.02f) { focused ->
        Row(
            Modifier.fillMaxWidth().height(58.dp)
                .background(if (selected || focused) RowSelected else PanelElevated)
                .padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("%03d".format(channel.number), color = if (selected) IzRed else TextSecondary, fontSize = 10.sp, modifier = Modifier.width(34.dp))
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(channel.logoUrl, null, Modifier.size(36.dp), contentScale = ContentScale.Fit)
                } else Text(channel.name.take(2).uppercase(), color = PanelBlack, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Text(channel.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (selected) Icon(Icons.Filled.PlayArrow, null, tint = IzRed, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun FavoritePosterColumn(
    title: String,
    emptyLabel: String,
    items: List<FavoritePosterData>,
    modifier: Modifier,
    onClick: (String) -> Unit
) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(PanelDark).padding(12.dp)) {
        Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(emptyLabel, color = TextSecondary, fontSize = 12.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().focusGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.id }) { item -> FavoriteTallCard(item) { onClick(item.id) } }
            }
        }
    }
}

@Composable
private fun FavoriteTallCard(item: FavoritePosterData, onClick: () -> Unit) {
    TvCard(onClick = onClick, shape = RoundedCornerShape(12.dp), focusScale = 1.04f) {
        Box(Modifier.fillMaxWidth().height(210.dp).background(PanelElevated)) {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(item.imageUrl, item.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.96f))))
                    .padding(12.dp)
            ) {
                Column {
                    Text(item.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    item.rating?.takeIf { (it.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0 }?.let {
                        Text("★ $it", color = IzYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritePosterGrid(title: String, items: List<FavoritePosterData>, onClick: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize().focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            gridItems(items, key = { it.id }) { item -> FavoriteTallCard(item) { onClick(item.id) } }
        }
    }
}

@Composable
private fun FavoriteEmptyColumn(title: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(PanelDark).padding(14.dp)) {
        Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum canal salvo", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FavoriteFilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
    TvCard(onClick = onClick, shape = RoundedCornerShape(20.dp), focusScale = 1.04f) {
        Box(
            Modifier
                .background(if (selected) IzRed else PanelElevated)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StartDashboard(
    vm: MainViewModel,
    state: UiState,
    onOpen: (NavItem) -> Unit
) {
    LaunchedEffect(Unit) { vm.loadHomeCatalogs() }

    val profileName = state.activeProfile?.name ?: "você"
    val tastes = state.activeProfile?.genres.orEmpty()
    val vodCategoryNames = remember(state.vodCategories) {
        state.vodCategories.associate { it.id to it.name }
    }
    val seriesCategoryNames = remember(state.seriesCategories) {
        state.seriesCategories.associate { it.id to it.name }
    }
    val personalizedMovies = remember(state.allVod, tastes, vodCategoryNames) {
        state.allVod.asSequence()
            // O provedor já entrega o catálogo em ordem editorial/recente.
            // Limitar antes de normalizar e pontuar evita bloquear a thread
            // principal (e, portanto, o D-pad) com dezenas de milhares de itens.
            .take(1_200)
            .filter {
                recommendationMatchesProfile(
                    title = it.name,
                    category = vodCategoryNames[it.categoryId].orEmpty(),
                    tastes = tastes,
                ) &&
                    !it.posterUrl.isNullOrBlank() &&
                    recommendationRating(it.rating) >= 6.5
            }
            .sortedWith(
                compareByDescending<VodItem> {
                    preferenceScore(vodCategoryNames[it.categoryId].orEmpty(), tastes)
                }.thenByDescending { recommendationRating(it.rating) }
            )
            .take(80)
            .toList()
    }
    val personalizedSeries = remember(state.allSeries, tastes, seriesCategoryNames) {
        state.allSeries.asSequence()
            .take(800)
            .filter {
                recommendationMatchesProfile(
                    title = it.name,
                    category = seriesCategoryNames[it.categoryId].orEmpty(),
                    tastes = tastes,
                ) &&
                    !it.coverUrl.isNullOrBlank() &&
                    recommendationRating(it.rating) >= 6.5
            }
            .sortedWith(
                compareByDescending<SeriesItem> {
                    preferenceScore(seriesCategoryNames[it.categoryId].orEmpty(), tastes)
                }.thenByDescending { recommendationRating(it.rating) }
            )
            .take(80)
            .toList()
    }
    // O ViewModel entrega o filme global ja resolvido, enriquecido e com a arte
    // em cache antes de liberar a Home. Nao existe Hero provisório na interface.
    val heroMovie = state.heroVod
    val continueWatching = remember(
        state.recentVodIds,
        state.recentSeriesIds,
        state.allVod,
        state.allSeries
    ) {
        if (state.recentVodIds.isEmpty() && state.recentSeriesIds.isEmpty()) {
            emptyList()
        } else {
            val wantedVod = state.recentVodIds.toSet()
            val wantedSeries = state.recentSeriesIds.toSet()
            val vodById = state.allVod.asSequence().filter { it.id in wantedVod }.associateBy { it.id }
            val seriesById = state.allSeries.asSequence().filter { it.id in wantedSeries }.associateBy { it.id }
            buildList {
                state.recentVodIds.mapNotNull(vodById::get).forEach {
                    add(HomePosterEntry(it.id, it.name, it.posterUrl, it.rating, HomePosterType.MOVIE))
                }
                state.recentSeriesIds.mapNotNull(seriesById::get).forEach {
                    add(HomePosterEntry(it.id, it.name, it.coverUrl, it.rating, HomePosterType.SERIES))
                }
            }.take(8)
        }
    }
    // Só preparamos candidatos suficientes para preencher as fileiras visíveis.
    // Evita normalizar milhares de títulos na thread principal de TV boxes simples.
    val movieEntries = remember(personalizedMovies) {
        personalizedMovies.take(80).map {
            HomePosterEntry(it.id, it.name, it.posterUrl, it.rating, HomePosterType.MOVIE)
        }
    }
    val seriesEntries = remember(personalizedSeries) {
        personalizedSeries.take(80).map {
            HomePosterEntry(it.id, it.name, it.coverUrl, it.rating, HomePosterType.SERIES)
        }
    }
    val chosenForProfile = remember(movieEntries, seriesEntries) {
        deduplicatePosters(buildList {
            val max = maxOf(movieEntries.size, seriesEntries.size)
            repeat(max) { index ->
                movieEntries.getOrNull(index)?.let(::add)
                seriesEntries.getOrNull(index)?.let(::add)
            }
        })
    }
    val mostWatched = remember(movieEntries, seriesEntries) {
        deduplicatePosters(
            (movieEntries + seriesEntries).sortedByDescending {
                it.rating?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            }
        )
    }
    val featuredMovies = remember(state.allVod) {
        deduplicatePosters(
            state.allVod.asReversed().take(80).map {
                HomePosterEntry(it.id, it.name, it.posterUrl, it.rating, HomePosterType.MOVIE)
            }
        )
    }
    val featuredSeries = remember(seriesEntries) { deduplicatePosters(seriesEntries) }

    fun openPoster(entry: HomePosterEntry) {
        when (entry.type) {
            HomePosterType.MOVIE -> state.allVod.firstOrNull { it.id == entry.id }?.let(vm::selectVod)
            HomePosterType.SERIES -> state.allSeries.firstOrNull { it.id == entry.id }?.let(vm::selectSeries)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(PanelBlack),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            if (heroMovie != null) HeroBanner(
                eyebrow = "DESTAQUE",
                title = heroMovie.name,
                subtitle = heroMovie.plot ?: "O filme mais assistido no IZ Play.",
                imageUrl = heroMovie.posterUrl,
                backdropUrl = heroMovie.backdropUrl,
                backdropMobileUrl = heroMovie.backdropMobileUrl,
                backdropPositionX = heroMovie.backdropPositionX,
                backdropPositionY = heroMovie.backdropPositionY,
                backdropScale = heroMovie.backdropScale,
                overlayOpacity = heroMovie.overlayOpacity,
                year = heroMovie.year,
                rating = heroMovie.rating,
                durationSecs = heroMovie.durationSecs,
                category = heroMovie.categoryId.let(vodCategoryNames::get),
                onWatch = { vm.selectVod(heroMovie) }
            ) else Box(
                Modifier.fillMaxWidth().height(430.dp).background(Color(0xFF050505))
            )
        }

        if (chosenForProfile.isNotEmpty()) {
            item {
                ContentRail("Escolhidos para $profileName", "PARA VOCE") {
                    items(chosenForProfile, key = { "${it.type}-${it.id}" }) { entry ->
                        PosterTile(entry.title, entry.imageUrl, entry.rating) { openPoster(entry) }
                    }
                }
            }
        }

        if (continueWatching.isNotEmpty()) {
            item {
                ContentRail("Continuar assistindo", "PARA VOCE") {
                    items(continueWatching, key = { "${it.type}-${it.id}" }) { entry ->
                        PosterTile(entry.title, entry.imageUrl, entry.rating) { openPoster(entry) }
                    }
                }
            }
        }

        if (mostWatched.isNotEmpty()) {
            item {
                ContentRail("Mais assistidos no IZ Play", "EM ALTA") {
                    items(mostWatched, key = { "${it.type}-${it.id}" }) { entry ->
                        PosterTile(entry.title, entry.imageUrl, entry.rating) { openPoster(entry) }
                    }
                }
            }
        }

        if (featuredMovies.isNotEmpty()) {
            item {
                ContentRail("Filmes em destaque", "NOVIDADES") {
                    items(featuredMovies, key = { it.id }) { entry ->
                        PosterTile(entry.title, entry.imageUrl, entry.rating) { openPoster(entry) }
                    }
                }
            }
        }

        if (featuredSeries.isNotEmpty()) {
            item {
                ContentRail("Séries em destaque", "PARA VOCE") {
                    items(featuredSeries, key = { it.id }) { entry ->
                        PosterTile(entry.title, entry.imageUrl, entry.rating) { openPoster(entry) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBanner(
    eyebrow: String,
    title: String,
    subtitle: String,
    imageUrl: String?,
    backdropUrl: String?,
    backdropMobileUrl: String?,
    backdropPositionX: Float,
    backdropPositionY: Float,
    backdropScale: Float,
    overlayOpacity: Float,
    year: String?,
    rating: String?,
    durationSecs: Int,
    category: String?,
    onWatch: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val compact = configuration.screenWidthDp < 600
    val backdrop = (if (compact) backdropMobileUrl ?: backdropUrl else backdropUrl)
        ?.takeIf { it.isNotBlank() }
    val focalAlignment = BiasAlignment(
        horizontalBias = (backdropPositionX.coerceIn(0f, 100f) / 50f) - 1f,
        verticalBias = (backdropPositionY.coerceIn(0f, 100f) / 50f) - 1f
    )
    val imageRequest = backdrop?.let {
        ImageRequest.Builder(context)
            .data(it)
            .size(if (compact) 900 else 1280, if (compact) 1200 else 720)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }
    val backgroundRequest = imageRequest ?: imageUrl?.takeIf { it.isNotBlank() }?.let {
        ImageRequest.Builder(context)
            .data(it).size(960, 540)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false).build()
    }
    val backgroundPainter = rememberAsyncImagePainter(backgroundRequest)
    val artworkReady = backgroundRequest == null || backgroundPainter.state is AsyncImagePainter.State.Success
    var revealHero by remember(title, backgroundRequest) { mutableStateOf(false) }
    LaunchedEffect(artworkReady, title) {
        if (artworkReady) {
            delay(80)
            revealHero = true
        }
    }
    val revealProgress by animateFloatAsState(
        targetValue = if (revealHero) 1f else 0f,
        animationSpec = tween(durationMillis = 480),
        label = "heroReveal"
    )
    val posterFallback = backdrop == null && !imageUrl.isNullOrBlank()
    val baseBlack = Color(0xFF050505)
    val overlay = overlayOpacity.coerceIn(0f, 1f)

    Box(
        Modifier
            .fillMaxWidth()
            .height(if (compact) 520.dp else 430.dp)
            .background(baseBlack)
    ) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                alpha = revealProgress
            }
        ) {
        if (imageRequest != null) {
            Image(
                painter = backgroundPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = focalAlignment,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = backdropScale.coerceIn(1f, 1.3f)
                    scaleY = backdropScale.coerceIn(1f, 1.3f)
                }
            )
        } else if (posterFallback) {
            Image(
                painter = backgroundPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = 1.12f
                    scaleY = 1.12f
                    alpha = 0.28f
                }
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(
                        baseBlack.copy(alpha = (overlay + 0.12f).coerceAtMost(1f)),
                        baseBlack.copy(alpha = overlay),
                        baseBlack.copy(alpha = overlay * 0.76f),
                        baseBlack.copy(alpha = overlay * 0.24f),
                        baseBlack.copy(alpha = overlay * 0.06f)
                    )
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Transparent, baseBlack.copy(alpha = 0.70f), baseBlack),
                    startY = if (compact) 220f else 190f
                )
            )
        )

        if (posterFallback && !compact) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl).size(420, 630)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(false).build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 52.dp)
                    .width(190.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF111111))
            )
        }

        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = PAGE_PADDING, end = 24.dp)
                .widthIn(max = 670.dp)
        ) {
            Text(eyebrow, color = IzRed, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                title.uppercase(),
                color = TextPrimary,
                fontSize = if (compact) 36.sp else 52.sp,
                lineHeight = if (compact) 36.sp else 50.sp,
                letterSpacing = 0.2.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(16.dp))
            Text(
                subtitle,
                color = TextPrimary.copy(alpha = 0.78f),
                fontSize = if (compact) 13.sp else 15.sp,
                lineHeight = if (compact) 18.sp else 21.sp,
                maxLines = if (compact) 2 else 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                year?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }?.let { MetaChip(it) }
                rating?.takeIf { (it.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0 }?.let { MetaChip("★ $it") }
                if (!compact && durationSecs > 0) MetaChip("${durationSecs / 3600}h ${(durationSecs % 3600) / 60}min")
                if (!compact) {
                    category
                        ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                        ?.let { MetaChip(it.uppercase()) }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroButton("Assistir", Icons.Filled.PlayArrow, Color.White, Color.Black, onWatch)
                HeroButton("Explorar catalogo", Icons.Filled.Movie, PanelElevated, TextPrimary, onWatch)
            }
        }
    }
}
}

@Composable
private fun MetaChip(label: String) {
    Text(
        label,
        color = TextPrimary.copy(alpha = 0.74f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp)
    )
}

@Composable
private fun HeroButton(label: String, icon: ImageVector, background: Color, foreground: Color, onClick: () -> Unit) {
    TvCard(onClick = onClick, shape = RoundedCornerShape(8.dp), focusScale = 1.03f) {
        Row(
            Modifier.height(48.dp).background(background).padding(horizontal = 22.dp),
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
    TvCard(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp), focusScale = 1.03f) {
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
            contentPadding = PaddingValues(horizontal = PAGE_PADDING, vertical = 8.dp),
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
                Text("%03d - Ao vivo".format(channel.number), color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun PosterTile(title: String, imageUrl: String?, rating: String?, onClick: () -> Unit) {
    val context = LocalContext.current
    val request = remember(imageUrl) {
        imageUrl?.takeIf { it.isNotBlank() }?.let {
            ImageRequest.Builder(context)
                .data(it)
                .size(300, 450)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(false)
                .build()
        }
    }
    val displayRating = formatRatingPtBr(rating)
    TvCard(onClick = onClick, focusScale = 1.05f) { focused ->
        Box(
            Modifier
                .width(150.dp)
                .aspectRatio(2f / 3f)
                .background(Brush.verticalGradient(listOf(Color(0xFF171717), Color(0xFF090909))))
        ) {
            if (request != null) {
                AsyncImage(
                    model = request,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Filled.Movie,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.45f),
                    modifier = Modifier.size(42.dp).align(Alignment.Center)
                )
            }
            if (displayRating != null) {
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Star, null, tint = IzYellow, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(displayRating, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (focused) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.94f))
                            )
                        )
                        .padding(start = 10.dp, end = 10.dp, top = 32.dp, bottom = 10.dp)
                ) {
                    Text(
                        title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
    BackHandler(enabled = drawerOpen) { onDrawerToggle(false) }
    var showSpeedTest by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            val catLabel = when (state.selectedCategoryId) {
                UiState.FAVORITES_ID -> "FAVORITOS"
                else -> state.categories.firstOrNull { it.id == state.selectedCategoryId }?.name ?: "â€”"
            }
            ChannelColumn(
                categoryLabel = catLabel,
                channels = state.visibleChannels,
                selectedChannelId = state.selectedChannel?.id,
                totalLabel = "${state.visibleChannels.size} canais",
                searchQuery = state.searchQuery,
                onSearch = vm::setSearch,
                onOpenCategories = { onDrawerToggle(true) },
                onSelect = vm::selectChannel,
                showSearch = false
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
                    else -> Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TvSearchField(
                                query = state.searchQuery,
                                placeholder = "Buscar canal...",
                                onQuery = vm::setSearch,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(12.dp))
                            NetworkStatusButton(state, onClick = { showSpeedTest = true; vm.runSpeedTest() })
                        }
                        Row(Modifier.weight(1f)) {
                            val ch = state.selectedChannel
                            PlayerPanel(
                                channel = ch,
                                isFavorite = ch != null && ch.id in state.favorites,
                                epg = state.epg,
                                epgLoading = state.epgLoading,
                                suspended = state.playing != null || state.fullscreenTransition,
                                streamFallback = state.streamFallback(ch?.streamUrl),
                                onToggleFavorite = { ch?.let(vm::toggleFavorite) },
                                onReconnect = { ch?.let(vm::selectChannel) },
                                onFullscreen = { ch?.let { vm.playLive(it.streamUrl, it.name) } },
                                modifier = Modifier.weight(1f)
                            )
                            LiveRightRail(state = state, onChannel = vm::selectChannel)
                        }
                    }
                }
            }
        }

        if (showSpeedTest) SpeedTestDialog(
            state = state,
            onRetest = vm::runSpeedTest,
            onClose = { showSpeedTest = false }
        )

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

private fun preferenceScore(category: String, tastes: List<String>): Int {
    val normalized = category.lowercase()
    val aliases = mapOf(
        "Ação" to listOf("acao", "ação", "action"),
        "Comédia" to listOf("comedia", "comédia", "humor"),
        "Drama" to listOf("drama"),
        "Romance" to listOf("romance"),
        "Suspense" to listOf("suspense", "thriller"),
        "Terror" to listOf("terror", "horror"),
        "Ficção científica" to listOf("ficcao", "ficção", "sci-fi"),
        "Animação" to listOf("animacao", "animação", "infantil", "kids"),
        "Documentários" to listOf("documentario", "documentário"),
        "Novelas" to listOf("novela"),
        "Esportes" to listOf("esporte", "futebol"),
        "Nacional" to listOf("nacional", "brasil")
    )
    return tastes.fold(0) { score, taste ->
        score + if (aliases[taste].orEmpty().any { it in normalized }) 10 else 0
    }
}

private fun recommendationRating(value: String?): Double =
    value
        ?.takeUnless { it.equals("null", ignoreCase = true) }
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        ?.coerceIn(0.0, 10.0)
        ?: 0.0

private fun recommendationMatchesProfile(
    title: String,
    category: String,
    tastes: List<String>,
): Boolean {
    if (preferenceScore(category, tastes) <= 0) return false

    // Alguns fornecedores classificam Malhação em categorias genéricas como
    // Nacional, Juvenil ou Drama. Isso não transforma a novela em uma escolha
    // válida para quem não marcou explicitamente Novelas.
    val normalizedTitle = Normalizer.normalize(
        title.lowercase(Locale.ROOT),
        Normalizer.Form.NFD,
    ).replace(Regex("\\p{M}+"), "")
    val requiresNovelTaste = "malhacao" in normalizedTitle
    return !requiresNovelTaste || tastes.any { it.equals("Novelas", ignoreCase = true) }
}

@Composable
private fun NetworkStatusButton(state: UiState, onClick: () -> Unit) {
    val color = when (state.connection.quality) {
        ConnectionQuality.GOOD -> Color(0xFF20D67A)
        ConnectionQuality.FAIR -> Color(0xFFFFC107)
        ConnectionQuality.POOR -> IzRed
        ConnectionQuality.UNKNOWN -> TextSecondary
    }
    TvCard(onClick = onClick, shape = RoundedCornerShape(12.dp), focusScale = 1.05f) {
        Box(Modifier.size(48.dp).background(PanelElevated), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Wifi, "Teste de conexao", tint = color, modifier = Modifier.size(25.dp))
        }
    }
}

@Composable
private fun SpeedTestDialog(state: UiState, onRetest: () -> Unit, onClose: () -> Unit) {
    val c = state.connection
    Dialog(onDismissRequest = onClose) {
        Column(Modifier.width(620.dp).clip(RoundedCornerShape(18.dp)).background(PanelElevated).padding(26.dp)) {
            Text("Teste de conexao", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    "DOWNLOAD" to "%.1f Mbps".format(c.downloadMbps),
                    "UPLOAD" to "%.1f Mbps".format(c.uploadMbps),
                    "PING" to "%.0f ms".format(c.pingMs),
                    "JITTER" to "%.1f ms".format(c.jitterMs),
                    "PERDA" to "%.0f%%".format(c.lossPct)
                ).forEach { (label, value) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(PanelDark).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(value, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text(label, color = TextSecondary, fontSize = 9.sp)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(if (c.testing) "Medindo sua conexao..." else "Resultado enviado ao suporte para diagnostico.", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailLikeButton(if (c.testing) "TESTANDO..." else "TESTAR NOVAMENTE", enabled = !c.testing, onClick = onRetest, modifier = Modifier.weight(1f))
                DetailLikeButton("FECHAR", enabled = true, onClick = onClose, modifier = Modifier.weight(0.45f))
            }
        }
    }
}

@Composable
private fun DetailLikeButton(label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TvCard(onClick = { if (enabled) onClick() }, modifier = modifier, shape = RoundedCornerShape(10.dp), focusScale = 1.03f) {
        Box(Modifier.fillMaxWidth().height(48.dp).background(if (enabled) IzRed else PanelDark), contentAlignment = Alignment.Center) {
            Text(label, color = if (enabled) Color.White else TextSecondary, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LiveRightRail(state: UiState, onChannel: (Channel) -> Unit) {
    val now = System.currentTimeMillis()
    val upcoming = state.epg.filter { it.start > now }.distinctBy { it.start to it.title }.take(6)
    val recents = state.recentChannelIds.mapNotNull { id -> state.allChannels.firstOrNull { it.id == id } }.take(8)
    Column(Modifier.width(250.dp).fillMaxHeight().padding(end = 12.dp, bottom = 16.dp)) {
        Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(PanelDark).padding(14.dp)) {
            Text("PROXIMOS PROGRAMAS", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            if (upcoming.isEmpty()) Text("Programacao indisponivel", color = TextSecondary, fontSize = 11.sp)
            upcoming.forEach { ep ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(ep.start)), color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(42.dp))
                    Text(ep.title, color = TextPrimary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.clip(RoundedCornerShape(14.dp)).background(PanelDark).padding(14.dp)) {
            Text("ULTIMOS CANAIS", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            recents.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { channel ->
                        TvCard(onClick = { onChannel(channel) }, shape = RoundedCornerShape(9.dp), focusScale = 1.08f) {
                            Box(
                                Modifier
                                    .width(48.dp)
                                    .height(44.dp)
                                    .background(PanelElevated)
                                    .padding(5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(channel.name.take(2).uppercase(), color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                channel.logoUrl
                                    ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                                    ?.let { logoUrl ->
                                        AsyncImage(
                                            model = logoUrl,
                                            contentDescription = "Logo de ${channel.name}",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
