package com.izplay.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.izplay.tv.data.model.Channel
import com.izplay.tv.data.model.EpgEntry
import com.izplay.tv.player.VideoPlayer
import com.izplay.tv.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlayerPanel(
    channel: Channel?,
    isFavorite: Boolean,
    epg: List<EpgEntry>,
    epgLoading: Boolean = false,
    suspended: Boolean = false,
    streamFallback: String? = null,
    onToggleFavorite: () -> Unit,
    onReconnect: () -> Unit,
    onFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showInfo by remember(channel?.id) { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxSize()
            .background(PanelBlack)
            .padding(16.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.54f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
        ) {
            if (suspended) {
                // Em tela cheia o player do painel é REMOVIDO da composição (não só
                // pausado): o SurfaceView dele mantinha o último frame por cima do
                // fullscreen, e TV boxes costumam ter um único decodificador de
                // hardware — precisa ser liberado para a tela cheia usar.
                Box(Modifier.fillMaxSize().background(Color.Black))
            } else {
                VideoPlayer(
                    streamUrl = channel?.streamUrl,
                    fallbackUrl = streamFallback,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            channel?.name ?: "Selecione um canal",
            color = TextPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            maxLines = 1
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton(if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder, "FAVORITO", Modifier.weight(1f), onToggleFavorite)
            ActionButton(Icons.Filled.Refresh, "RECONECTAR", Modifier.weight(1f), onReconnect)
            ActionButton(Icons.Filled.Fullscreen, "TELA CHEIA", Modifier.weight(1f), onFullscreen)
            ActionButton(Icons.Filled.Info, "INFO", Modifier.weight(1f)) { if (channel != null) showInfo = true }
        }

        Spacer(Modifier.height(16.dp))

        Text("PROGRAMACAO", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxSize()) {
            when {
                epgLoading -> CircularProgressIndicator(color = IzRed, modifier = Modifier.size(24.dp).align(Alignment.TopStart).padding(top = 4.dp))
                epg.isEmpty() -> Text(
                    "Sem informacoes de programacao",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(epg) { entry -> EpgRow(entry) }
                }
            }
        }
    }
    if (showInfo && channel != null) {
        val now = System.currentTimeMillis()
        val current = epg.firstOrNull { now in it.start..it.end }
        val next = epg.firstOrNull { it.start > now }
        Dialog(onDismissRequest = { showInfo = false }) {
            Column(
                Modifier.width(560.dp).clip(RoundedCornerShape(18.dp)).background(PanelElevated).padding(24.dp)
            ) {
                Text("INFORMACOES DO CANAL", color = IzRed, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                Text(channel.name, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(14.dp))
                Text("Canal ${channel.number}  •  ID ${channel.id}", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                Text("AGORA", color = IzRed, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text(current?.title ?: "Programacao nao fornecida", color = TextPrimary, fontSize = 15.sp)
                current?.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(6.dp)); Text(it, color = TextSecondary, fontSize = 13.sp, maxLines = 4)
                }
                Spacer(Modifier.height(14.dp))
                Text("A SEGUIR", color = IzRed, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text(next?.title ?: "Sem proxima programacao", color = TextPrimary, fontSize = 14.sp)
                Spacer(Modifier.height(22.dp))
                TvCard(onClick = { showInfo = false }, shape = RoundedCornerShape(10.dp), focusScale = 1.03f) {
                    Box(Modifier.fillMaxWidth().height(48.dp).background(IzRed), contentAlignment = Alignment.Center) {
                        Text("VOLTAR", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun EpgRow(entry: EpgEntry) {
    val now = System.currentTimeMillis()
    val isNow = now in entry.start..entry.end
    val fmt = SimpleDateFormat("HH:mm", Locale("pt", "BR"))
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isNow) RowSelected else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(if (isNow) IzRed else PanelDark))
        Spacer(Modifier.width(12.dp))
        Text(fmt.format(Date(entry.start)), color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(46.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            entry.title,
            color = if (isNow) TextPrimary else TextSecondary,
            fontWeight = if (isNow) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    // Foco oficial (borda vermelha + zoom) via TvCard — antes o D-pad chegava
    // nos botões mas não dava pra ver qual estava selecionado.
    TvCard(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp), focusScale = 1.03f) { focused ->
        Column(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(if (focused) IzRed.copy(alpha = 0.32f) else PanelElevated),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(5.dp))
            Text(label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}
