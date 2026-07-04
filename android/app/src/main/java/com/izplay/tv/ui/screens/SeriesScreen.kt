package com.izplay.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
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
import com.izplay.tv.data.model.Episode
import com.izplay.tv.data.model.Season
import com.izplay.tv.data.model.SeriesItem
import com.izplay.tv.player.VideoPlayer
import com.izplay.tv.ui.MainViewModel
import com.izplay.tv.ui.components.TvCard
import com.izplay.tv.ui.components.rememberTvFocus
import com.izplay.tv.ui.theme.*

@Composable
fun SeriesScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadSeriesIfNeeded() }

    Row(Modifier.fillMaxSize().background(PanelBlack)) {
        SeriesCategorySidePanel(
            categories = listOf(Category("__all__", "TODAS")) + state.seriesCategories,
            selectedId = state.selectedSeriesCategoryId ?: "__all__",
            onSelect = { id ->
                vm.selectSeriesCategory(if (id == "__all__") null else id)
                vm.selectSeries(null)
            }
        )

        Box(Modifier.weight(1f).fillMaxHeight()) {
            when {
                state.seriesLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = IzRed)
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        SeriesSearchBar(
                            query = state.seriesSearchQuery,
                            onQuery = vm::setSeriesSearch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        if (state.visibleSeries.isEmpty()) {
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Text("Nenhuma série encontrada", color = TextSecondary, fontSize = 15.sp)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 140.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.visibleSeries, key = { it.id }) { series ->
                                    SeriesCard(
                                        series = series,
                                        selected = series.id == state.selectedSeries?.id,
                                        onClick = { vm.selectSeries(series) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.selectedSeries != null) {
            SeriesDetailPanel(
                series = state.selectedSeries!!,
                seasons = state.seriesSeasons,
                loading = state.seriesDetailLoading,
                onClose = { vm.selectSeries(null) }
            )
        }
    }
}

@Composable
private fun SeriesCategorySidePanel(
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
private fun SeriesCard(series: SeriesItem, selected: Boolean, onClick: () -> Unit) {
    TvCard(onClick = onClick, shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.background(if (selected) IzRed.copy(alpha = 0.12f) else PanelDark)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(Color(0xFF1A1A1A))
        ) {
            if (!series.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = series.coverUrl,
                    contentDescription = series.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Filled.Tv,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(40.dp).align(Alignment.Center)
                )
            }
            if (!series.rating.isNullOrBlank()) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text("★ ${series.rating}", color = Color(0xFFFFD700), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (selected) {
                Box(Modifier.fillMaxSize().background(IzRed.copy(alpha = 0.15f)))
            }
        }
        Text(
            series.name,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
        if (!series.year.isNullOrBlank()) {
            Text(
                series.year!!,
                color = TextSecondary,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp)
            )
        }
        }
    }
}

@Composable
private fun SeriesDetailPanel(
    series: SeriesItem,
    seasons: List<Season>,
    loading: Boolean,
    onClose: () -> Unit
) {
    var selectedSeason by remember(series.id) { mutableIntStateOf(0) }
    var playingEpisode by remember(series.id) { mutableStateOf<Episode?>(null) }

    Column(
        Modifier
            .width(340.dp)
            .fillMaxHeight()
            .background(Color(0xFF0F0F0F))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(Color.Black)
        ) {
            val ep = playingEpisode
            if (ep != null) {
                VideoPlayer(streamUrl = ep.streamUrl, modifier = Modifier.fillMaxSize())
            } else {
                if (!series.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = series.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                    Alignment.Center
                ) {
                    Icon(Icons.Filled.Tv, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                }
            }
        }

        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Text(series.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2)
            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!series.year.isNullOrBlank())
                    SeriesChip(series.year!!)
                if (!series.rating.isNullOrBlank())
                    SeriesChip("★ ${series.rating}", highlight = true)
            }

            if (!series.plot.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    series.plot!!,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            when {
                loading -> Box(Modifier.fillMaxWidth().height(60.dp), Alignment.Center) {
                    CircularProgressIndicator(color = IzRed, modifier = Modifier.size(28.dp))
                }
                seasons.isEmpty() -> Text("Sem episódios disponíveis", color = TextSecondary, fontSize = 12.sp)
                else -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(seasons.indices.toList()) { idx ->
                            val active = idx == selectedSeason
                            val f = rememberTvFocus()
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) IzRed else if (f.focused) IzRedDark else PanelDark)
                                    .clickable(interactionSource = f.source, indication = null) { selectedSeason = idx }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    "T${seasons[idx].seasonNumber}",
                                    color = TextPrimary,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    val eps = seasons.getOrNull(selectedSeason)?.episodes ?: emptyList()
                    LazyColumn(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(eps, key = { it.id }) { ep ->
                            EpisodeRow(
                                episode = ep,
                                playing = ep.id == playingEpisode?.id,
                                onClick = { playingEpisode = ep }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

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
private fun EpisodeRow(episode: Episode, playing: Boolean, onClick: () -> Unit) {
    val f = rememberTvFocus()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    f.focused -> IzRed.copy(alpha = 0.34f)
                    playing -> IzRed.copy(alpha = 0.2f)
                    else -> PanelDark
                }
            )
            .clickable(interactionSource = f.source, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (playing) IzRed else Color(0xFF2A2A2A)),
            Alignment.Center
        ) {
            if (playing) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            } else {
                Text(
                    "${episode.episodeNum}",
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                episode.title,
                color = if (playing) TextPrimary else TextSecondary,
                fontWeight = if (playing) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (episode.durationSecs > 0) {
                Text(
                    "${episode.durationSecs / 60}min",
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = if (playing) IzRed else TextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SeriesChip(text: String, highlight: Boolean = false) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (highlight) IzRed.copy(alpha = 0.25f) else PanelDark)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            color = if (highlight) Color(0xFFFFD700) else TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SeriesSearchBar(query: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier) {
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
                if (query.isEmpty()) Text("Buscar séries...", color = TextSecondary, fontSize = 14.sp)
                inner()
            },
            modifier = Modifier.weight(1f)
        )
    }
}
