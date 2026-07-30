package com.izplay.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.izplay.tv.data.model.Category
import com.izplay.tv.data.model.Channel
import com.izplay.tv.ui.theme.*

@Composable
fun CategoryColumn(
    categories: List<Category>,
    favoritesCount: Int,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(PanelDark)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Categorias", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            val closeFocus = rememberTvFocus()
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (closeFocus.focused) IzRed else Color.Transparent)
                    .clickable(interactionSource = closeFocus.source, indication = null, onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Close, "Fechar",
                    tint = if (closeFocus.focused) Color.White else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        // Ao abrir o drawer, o foco do D-pad PRECISA entrar aqui — sem isso as setas
        // continuavam navegando o conteúdo atrás e a categoria nunca mudava.
        val firstFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
        LazyColumn(Modifier.fillMaxSize().focusGroup()) {
            item {
                CategoryRow(
                    name = "FAVORITOS",
                    count = favoritesCount,
                    active = selectedId == "__favorites__",
                    onClick = { onSelect("__favorites__") },
                    modifier = Modifier.focusRequester(firstFocus)
                )
            }
            items(categories) { cat ->
                CategoryRow(
                    name = cat.name,
                    count = cat.channelCount,
                    active = cat.id == selectedId,
                    onClick = { onSelect(cat.id) }
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    name: String,
    count: Int,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val f = rememberTvFocus()
    Row(
        modifier
            .fillMaxWidth()
            .background(
                when {
                    f.focused -> IzRed.copy(alpha = 0.28f)
                    active -> IzRed.copy(alpha = 0.12f)
                    else -> Color.Transparent
                }
            )
            .clickable(interactionSource = f.source, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            color = if (active) IzRed else TextPrimary,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            count.toString(),
            color = if (active) IzRed else TextSecondary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun ChannelColumn(
    categoryLabel: String,
    channels: List<Channel>,
    selectedChannelId: String?,
    totalLabel: String,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onOpenCategories: () -> Unit,
    onSelect: (Channel) -> Unit,
    showSearch: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(channels) {
        channels.asSequence()
            .mapNotNull { it.logoUrl?.takeIf(String::isNotBlank) }
            .distinct()
            .take(24)
            .forEach { logoUrl ->
                context.imageLoader.enqueue(
                    ImageRequest.Builder(context)
                        .data(logoUrl)
                        .memoryCacheKey(channelLogoCacheKey(logoUrl))
                        .diskCacheKey(channelLogoCacheKey(logoUrl))
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .size(96, 96)
                        .build()
                )
            }
    }
    Column(
        modifier
            .fillMaxHeight()
            .width(330.dp)
            .background(PanelDarker)
    ) {
        // Cabeçalho: botão que abre o drawer de categorias (com destaque de foco)
        val headerFocus = rememberTvFocus()
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (headerFocus.focused) IzRed.copy(alpha = 0.26f) else Color.Transparent)
                .clickable(interactionSource = headerFocus.source, indication = null, onClick = onOpenCategories)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Menu, "Categorias", tint = IzRed, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("CATEGORIA", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(categoryLabel, color = IzRed, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        // Busca no padrão TV: só abre o teclado ao apertar OK (TvSearchField)
        if (showSearch) {
            TvSearchField(
                query = searchQuery,
                placeholder = "Buscar canal...",
                onQuery = onSearch,
                modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 10.dp)
            )
        }
        LazyColumn(Modifier.weight(1f)) {
            items(channels, key = { it.id }) { ch ->
                ChannelRow(ch, ch.id == selectedChannelId) { onSelect(ch) }
            }
        }
        Text(
            totalLabel,
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ChannelRow(channel: Channel, active: Boolean, onClick: () -> Unit) {
    val f = rememberTvFocus()
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                when {
                    f.focused -> IzRed.copy(alpha = 0.26f)
                    active -> RowSelected
                    else -> Color.Transparent
                }
            )
            .clickable(interactionSource = f.source, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (active) {
            Box(Modifier.width(3.dp).height(40.dp).background(IzRed))
            Spacer(Modifier.width(13.dp))
        } else {
            Spacer(Modifier.width(16.dp))
        }
        Text(
            "%03d".format(channel.number),
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(14.dp))
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF1C1C20)),
            contentAlignment = Alignment.Center
        ) {
            if (channel.logoUrl != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(channel.logoUrl)
                        .memoryCacheKey(channelLogoCacheKey(channel.logoUrl))
                        .diskCacheKey(channelLogoCacheKey(channel.logoUrl))
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .size(96, 96)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(36.dp),
                    loading = { ChannelInitials(channel.name) },
                    error = { ChannelInitials(channel.name) },
                    success = { SubcomposeAsyncImageContent() }
                )
            } else {
                ChannelInitials(channel.name)
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            channel.name,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun channelLogoCacheKey(url: String): String =
    "channel-logo:${url.trim().lowercase().hashCode()}"

@Composable
private fun ChannelInitials(name: String) {
    Text(
        name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase().ifBlank { "TV" },
        color = TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black
    )
}
