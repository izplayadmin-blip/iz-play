package com.izplay.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.izplay.tv.data.model.Category
import com.izplay.tv.data.model.VodItem
import com.izplay.tv.player.VideoPlayer
import com.izplay.tv.ui.MainViewModel
import com.izplay.tv.ui.components.TvCard
import com.izplay.tv.ui.components.rememberTvFocus
import com.izplay.tv.ui.theme.*

@Composable
fun MoviesScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadVodIfNeeded() }

    Row(Modifier.fillMaxSize().background(PanelBlack)) {
        VodCategorySidePanel(
            categories = listOf(Category("__all__", "TODOS")) + state.vodCategories,
            selectedId = state.selectedVodCategoryId ?: "__all__",
            onSelect = { id ->
                vm.selectVodCategory(if (id == "__all__") null else id)
                vm.selectVod(null)
            }
        )

        Box(Modifier.weight(1f).fillMaxHeight()) {
            when {
                state.vodLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = IzRed)
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        VodSearchBar(
                            query = state.vodSearchQuery,
                            onQuery = vm::setVodSearch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        if (state.visibleVod.isEmpty()) {
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Text("Nenhum filme encontrado", color = TextSecondary, fontSize = 15.sp)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 140.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.visibleVod, key = { it.id }) { vod ->
                                    MovieCard(
                                        vod = vod,
                                        selected = vod.id == state.selectedVod?.id,
                                        onClick = { vm.selectVod(vod) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.selectedVod != null) {
            MovieDetailPanel(
                vod = state.selectedVod!!,
                onClose = { vm.selectVod(null) }
            )
        }
    }
}

@Composable
private fun VodCategorySidePanel(
    categories: List<Category>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    LazyColumn(
        Modifier
            .width(180.dp)
            .fillMaxHeight()
            .background(PanelDark)
            .padding(vertical = 8.dp)
    ) {
        item {
            Text(
                "CATEGORIAS",
                color = IzRed,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
        items(categories, key = { it.id }) { cat ->
            val active = cat.id == selectedId
            val f = rememberTvFocus()
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        when {
                            f.focused -> IzRed.copy(alpha = 0.30f)
                            active -> IzRed.copy(alpha = 0.18f)
                            else -> Color.Transparent
                        }
                    )
                    .clickable(interactionSource = f.source, indication = null) { onSelect(cat.id) }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (active) Box(
                    Modifier.width(3.dp).height(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(IzRed)
                )
                Spacer(Modifier.width(if (active) 8.dp else 11.dp))
                Text(
                    cat.name,
                    color = if (active) TextPrimary else TextSecondary,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun MovieCard(vod: VodItem, selected: Boolean, onClick: () -> Unit) {
    TvCard(onClick = onClick, shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.background(if (selected) IzRed.copy(alpha = 0.12f) else PanelDark)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(Color(0xFF1A1A1A))
        ) {
            if (!vod.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = vod.posterUrl,
                    contentDescription = vod.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(40.dp).align(Alignment.Center)
                )
            }
            if (!vod.rating.isNullOrBlank()) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text("★ ${vod.rating}", color = Color(0xFFFFD700), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (selected) {
                Box(Modifier.fillMaxSize().background(IzRed.copy(alpha = 0.15f)))
            }
        }
        Text(
            vod.name,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
        if (!vod.year.isNullOrBlank()) {
            Text(
                vod.year!!,
                color = TextSecondary,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp)
            )
        }
        }
    }
}

@Composable
private fun MovieDetailPanel(vod: VodItem, onClose: () -> Unit) {
    var playing by remember(vod.id) { mutableStateOf(false) }

    Column(
        Modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(Color(0xFF0F0F0F))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color.Black)
        ) {
            if (playing) {
                VideoPlayer(streamUrl = vod.streamUrl, modifier = Modifier.fillMaxSize())
            } else {
                if (!vod.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = vod.posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                    Alignment.Center
                ) {
                    IconButton(onClick = { playing = true }) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Assistir",
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }
            }
        }

        Column(
            Modifier.fillMaxSize().padding(16.dp)
        ) {
            Text(vod.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 2)
            Spacer(Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!vod.year.isNullOrBlank())
                    InfoBadge(vod.year!!, PanelDark)
                if (!vod.rating.isNullOrBlank())
                    InfoBadge("★ ${vod.rating}", IzRed.copy(alpha = 0.25f))
                if (vod.durationSecs > 0)
                    InfoBadge(formatDuration(vod.durationSecs), PanelDark)
            }

            Spacer(Modifier.height(12.dp))

            if (!vod.plot.isNullOrBlank()) {
                Text("SINOPSE", color = IzRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(vod.plot!!, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { playing = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = IzRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("ASSISTIR", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Text("FECHAR", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun InfoBadge(text: String, bg: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun VodSearchBar(query: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PanelDark)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp),
            singleLine = true,
            decorationBox = { inner ->
                if (query.isEmpty()) Text("Buscar filmes...", color = TextSecondary, fontSize = 14.sp)
                inner()
            },
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatDuration(secs: Int): String {
    val h = secs / 3600
    val m = (secs % 3600) / 60
    return if (h > 0) "${h}h ${m}min" else "${m}min"
}
