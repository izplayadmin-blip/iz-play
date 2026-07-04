package com.izplay.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, "Fechar", tint = TextSecondary, modifier = Modifier.size(22.dp))
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                CategoryRow(
                    name = "FAVORITOS",
                    count = favoritesCount,
                    active = selectedId == "__favorites__",
                    onClick = { onSelect("__favorites__") }
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
private fun CategoryRow(name: String, count: Int, active: Boolean, onClick: () -> Unit) {
    val f = rememberTvFocus()
    Row(
        Modifier
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxHeight()
            .width(330.dp)
            .background(PanelDarker)
    ) {
        // Cabeçalho: botão que abre o drawer de categorias
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenCategories)
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
        // Busca compacta
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = 10.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PanelDark)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text("Buscar canal...", color = TextSecondary, fontSize = 13.sp)
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearch,
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(IzRed),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(channels) { ch ->
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
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Text(channel.name.take(2).uppercase(), color = TextPrimary, fontSize = 12.sp)
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
