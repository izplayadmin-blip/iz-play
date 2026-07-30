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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.izplay.tv.data.model.Category
import com.izplay.tv.data.model.VodItem
import com.izplay.tv.ui.MainViewModel
import com.izplay.tv.ui.components.TvCard
import com.izplay.tv.ui.components.TvSearchField
import com.izplay.tv.ui.components.rememberTvFocus
import com.izplay.tv.ui.theme.*
import java.util.Locale

@Composable
fun MoviesScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadVodIfNeeded() }

    Box(Modifier.fillMaxSize().background(PanelBlack)) {
        Row(Modifier.fillMaxSize()) {
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
                            TvSearchField(
                                query = state.vodSearchQuery,
                                placeholder = "Buscar filmes...",
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

        }
        // O clique num filme abre a tela de detalhe em TELA CHEIA (MovieDetailScreen),
        // renderizada na raiz do HomeScreen — acima da sidebar, estilo Max Player.
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
    val context = LocalContext.current
    val posterRequest = remember(vod.posterUrl) {
        vod.posterUrl?.takeIf { it.isNotBlank() }?.let {
            ImageRequest.Builder(context).data(it).size(300, 450)
                .memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(false).build()
        }
    }
    TvCard(onClick = onClick, shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.background(if (selected) IzRed.copy(alpha = 0.12f) else PanelDark)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(Color(0xFF1A1A1A))
        ) {
            if (posterRequest != null) {
                AsyncImage(
                    model = posterRequest,
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
            val displayRating = vod.rating?.replace(',', '.')?.toDoubleOrNull()
                ?.takeIf { it > 0.0 }?.let { String.format(Locale("pt", "BR"), "%.1f", it) }
            if (displayRating != null) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text("★ $displayRating", color = Color(0xFFFFD700), fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
        if (!vod.year.isNullOrBlank() && !vod.year.equals("null", ignoreCase = true)) {
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
