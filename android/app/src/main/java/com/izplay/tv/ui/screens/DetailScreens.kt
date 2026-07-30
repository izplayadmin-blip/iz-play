package com.izplay.tv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.izplay.tv.data.model.Episode
import com.izplay.tv.data.model.Season
import com.izplay.tv.data.model.SeriesItem
import com.izplay.tv.data.model.VodItem
import com.izplay.tv.ui.components.TvCard
import com.izplay.tv.ui.components.rememberTvFocus
import com.izplay.tv.ui.theme.*

/**
 * Telas de detalhe em TELA CHEIA (estilo Max Player): backdrop grande com scrim,
 * título, badges, sinopse e ação principal com foco inicial no controle remoto.
 * Renderizadas na raiz do HomeScreen, acima da sidebar. Voltar fecha.
 */

private val DETAIL_PADDING = 48.dp

@Composable
fun MovieDetailScreen(
    vod: VodItem,
    loading: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(vod.id) { runCatching { playFocus.requestFocus() } }

    DetailBackdrop(imageUrl = vod.posterUrl) {
        Row(
            Modifier.fillMaxSize().padding(DETAIL_PADDING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(end = 36.dp)) {
                DetailKicker("FILME")
                Spacer(Modifier.height(10.dp))
                Text(
                    vod.name,
                    color = TextPrimary,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!vod.year.isNullOrBlank() && !vod.year.equals("null", true)) DetailBadge(vod.year!!)
                    vod.rating?.takeIf { (it.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0 }
                        ?.let { DetailBadge("★ $it", highlight = true) }
                    if (vod.durationSecs > 0) DetailBadge(formatDetailDuration(vod.durationSecs))
                }
                if (loading) {
                    Spacer(Modifier.height(18.dp))
                    Text("SINOPSE", color = IzRed, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Buscando informações do filme...", color = TextSecondary, fontSize = 14.sp)
                } else if (!vod.plot.isNullOrBlank() && !vod.plot.equals("null", true)) {
                    Spacer(Modifier.height(18.dp))
                    Text("SINOPSE", color = IzRed, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        vod.plot!!,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 560.dp)
                    )
                } else {
                    Spacer(Modifier.height(18.dp))
                    Text("SINOPSE", color = IzRed, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Sinopse nao fornecida pelo catalogo.", color = TextSecondary, fontSize = 14.sp)
                }
                Spacer(Modifier.height(26.dp))
                Row(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailActionButton(
                        label = "ASSISTIR",
                        icon = Icons.Filled.PlayArrow,
                        background = IzRed,
                        modifier = Modifier.focusRequester(playFocus),
                        onClick = onPlay
                    )
                    DetailActionButton(
                        label = if (isFavorite) "REMOVER FAVORITO" else "FAVORITAR",
                        icon = Icons.Filled.Star,
                        background = if (isFavorite) IzRed.copy(alpha = 0.72f) else PanelElevated,
                        onClick = onToggleFavorite
                    )
                    DetailActionButton(
                        label = "VOLTAR",
                        icon = Icons.Filled.ArrowBack,
                        background = PanelElevated,
                        onClick = onClose
                    )
                }
            }
            DetailPoster(imageUrl = vod.posterUrl)
        }
    }
}

@Composable
fun SeriesDetailScreen(
    series: SeriesItem,
    seasons: List<Season>,
    loading: Boolean,
    isFavorite: Boolean,
    onPlayEpisode: (Episode) -> Unit,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    var selectedSeason by remember(series.id) { mutableIntStateOf(0) }

    DetailBackdrop(imageUrl = series.coverUrl) {
        Row(Modifier.fillMaxSize().padding(DETAIL_PADDING)) {
            // Coluna esquerda: identidade da série
            Column(Modifier.weight(0.52f).padding(end = 36.dp), verticalArrangement = Arrangement.Center) {
                DetailKicker("SÉRIE")
                Spacer(Modifier.height(10.dp))
                Text(
                    series.name,
                    color = TextPrimary,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!series.year.isNullOrBlank() && !series.year.equals("null", true)) DetailBadge(series.year!!)
                    series.rating?.takeIf { (it.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0 }
                        ?.let { DetailBadge("★ $it", highlight = true) }
                    if (seasons.isNotEmpty()) DetailBadge("${seasons.size} temporada${if (seasons.size > 1) "s" else ""}")
                }
                if (!series.plot.isNullOrBlank() && !series.plot.equals("null", true)) {
                    Spacer(Modifier.height(18.dp))
                    Text("SINOPSE", color = IzRed, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        series.plot!!,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (!loading) {
                    Spacer(Modifier.height(18.dp))
                    Text("INFORMACOES", color = IzRed, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Informacoes nao fornecidas pelo catalogo.", color = TextSecondary, fontSize = 14.sp)
                }
                Spacer(Modifier.height(26.dp))
                Row(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailActionButton(
                        label = if (isFavorite) "REMOVER FAVORITO" else "FAVORITAR",
                        icon = Icons.Filled.Star,
                        background = if (isFavorite) IzRed.copy(alpha = 0.72f) else PanelElevated,
                        onClick = onToggleFavorite
                    )
                    DetailActionButton(
                        label = "VOLTAR",
                        icon = Icons.Filled.ArrowBack,
                        background = PanelElevated,
                        onClick = onClose
                    )
                }
            }

            // Coluna direita: temporadas + episódios
            Column(Modifier.weight(0.48f).fillMaxHeight()) {
                when {
                    loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = IzRed)
                    }
                    seasons.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("Sem episódios disponíveis", color = TextSecondary, fontSize = 14.sp)
                    }
                    else -> {
                        LazyRow(
                            Modifier.focusGroup(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(seasons.indices.toList()) { idx ->
                                val active = idx == selectedSeason
                                var focused by remember { mutableStateOf(false) }
                                val interaction = remember { MutableInteractionSource() }
                                Box(
                                    Modifier
                                        .onFocusChanged { focused = it.isFocused }
                                        .focusable(interactionSource = interaction)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                focused -> IzRed
                                                active -> IzRed.copy(alpha = 0.55f)
                                                else -> PanelElevated
                                            }
                                        )
                                        .clickable(
                                            interactionSource = interaction,
                                            indication = null
                                        ) { selectedSeason = idx }
                                        .padding(horizontal = 16.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        "Temporada ${seasons[idx].seasonNumber}",
                                        color = TextPrimary,
                                        fontWeight = if (active || focused) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        val eps = seasons.getOrNull(selectedSeason)?.episodes ?: emptyList()
                        LazyColumn(
                            Modifier.weight(1f).focusGroup(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(eps, key = { it.id }) { ep ->
                                DetailEpisodeRow(
                                    episode = ep,
                                    requestInitialFocus = ep.id == eps.firstOrNull()?.id,
                                    onClick = { onPlayEpisode(ep) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Peças compartilhadas ────────────────────────────────────────────────────

@Composable
private fun DetailBackdrop(imageUrl: String?, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(PanelBlack)) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.30f)
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(PanelBlack, PanelBlack.copy(alpha = 0.86f), PanelBlack.copy(alpha = 0.45f))
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, PanelBlack), startY = 320f)
            )
        )
        content()
    }
}

@Composable
private fun DetailKicker(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(26.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(IzRed))
        Spacer(Modifier.width(10.dp))
        Text(text, color = IzRed, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
    }
}

@Composable
private fun DetailBadge(text: String, highlight: Boolean = false) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (highlight) IzRed.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text,
            color = if (highlight) Color(0xFFFFD700) else TextPrimary.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DetailActionButton(
    label: String,
    icon: ImageVector,
    background: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TvCard(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(8.dp), focusScale = 1.03f) {
        Row(
            Modifier.height(50.dp).background(background).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(9.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
    }
}

@Composable
private fun DetailPoster(imageUrl: String?) {
    if (imageUrl.isNullOrBlank()) return
    Box(
        Modifier
            .width(220.dp)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0C0F16))
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun DetailEpisodeRow(
    episode: Episode,
    requestInitialFocus: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }

    // O detalhe e exibido sobre a grade de series. Sem solicitar o foco aqui, algumas
    // TV boxes mantem o foco no card que ficou atras do overlay e o D-pad nao entra
    // na lista de episodios. O primeiro episodio tambem recebe foco ao trocar temporada.
    LaunchedEffect(requestInitialFocus, episode.id) {
        if (requestInitialFocus) runCatching { focusRequester.requestFocus() }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable(interactionSource = interaction)
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) IzRed.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.06f))
            .then(
                if (focused) Modifier.border(2.dp, IzRed, RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(7.dp)).background(if (focused) IzRed else PanelElevated),
            Alignment.Center
        ) {
            Text("${episode.episodeNum}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                episode.title,
                color = TextPrimary,
                fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (episode.durationSecs > 0) {
                Text("${episode.durationSecs / 60}min", color = TextSecondary, fontSize = 11.sp)
            }
        }
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = if (focused) Color.White else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun formatDetailDuration(secs: Int): String {
    val h = secs / 3600
    val m = (secs % 3600) / 60
    return if (h > 0) "${h}h ${m}min" else "${m}min"
}
