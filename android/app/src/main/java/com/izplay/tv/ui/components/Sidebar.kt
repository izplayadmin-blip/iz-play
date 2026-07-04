package com.izplay.tv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izplay.tv.ui.theme.IzRed
import com.izplay.tv.ui.theme.IzRedDark
import com.izplay.tv.ui.theme.IzRedDeep
import com.izplay.tv.ui.theme.TextPrimary

enum class NavItem(val label: String, val icon: ImageVector) {
    INICIO("Início", Icons.Filled.Home),
    CANAIS("Canais de TV", Icons.Filled.LiveTv),
    FILMES("Filmes", Icons.Filled.Movie),
    SERIES("Séries", Icons.Filled.PlayCircle),
    FAVORITOS("Favoritos", Icons.Filled.Star),
    CONFIG("Configurações", Icons.Filled.Settings),
    NOTIFICACOES("Notificações", Icons.Filled.Notifications),
    USUARIOS("Usuários", Icons.Filled.AccountCircle),
}

private val NAV_ORDER = listOf(
    NavItem.INICIO,
    NavItem.CANAIS,
    NavItem.FILMES,
    NavItem.SERIES,
    NavItem.FAVORITOS,
    NavItem.CONFIG,
    NavItem.NOTIFICACOES,
    NavItem.USUARIOS
)

private val COLLAPSED_WIDTH = 80.dp
private val EXPANDED_WIDTH = 236.dp

/**
 * Sidebar retrátil no estilo web/desktop: colapsada mostra só os ícones; ao receber foco
 * (D-pad do controle) expande e revela os rótulos. Navegável por setas — o primeiro item
 * recebe foco automático ao abrir o app, e a seta pra direita entra no conteúdo.
 */
@Composable
fun Sidebar(
    selected: NavItem,
    clock: String,
    date: String,
    onSelect: (NavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Expande quando qualquer item da sidebar está focado (TV) — igual ao hover do desktop.
    var expanded by remember { mutableStateOf(false) }
    val width by animateDpAsState(if (expanded) EXPANDED_WIDTH else COLLAPSED_WIDTH, label = "sidebarWidth")

    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(width)
            .background(Brush.verticalGradient(listOf(IzRed, IzRedDark, IzRedDeep)))
            .focusGroup()
            .onFocusChanged { expanded = it.hasFocus },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Logo ──
        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 22.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("iZ", fontSize = 28.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            if (expanded) {
                Spacer(Modifier.width(6.dp))
                Text(
                    "PLAY",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 3.sp
                )
            }
        }

        NAV_ORDER.forEach { item ->
            NavRow(
                item = item,
                active = item == selected,
                expanded = expanded,
                onClick = { onSelect(item) },
                modifier = if (item == NavItem.INICIO) Modifier.focusRequester(firstFocus) else Modifier
            )
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.weight(1f))

        if (expanded) {
            Column(
                Modifier.fillMaxWidth().padding(bottom = 18.dp, start = 18.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(clock, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(date, color = TextPrimary.copy(alpha = 0.8f), fontSize = 10.sp)
            }
        } else {
            Text(
                clock,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun NavRow(
    item: NavItem,
    active: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val bg by animateColorAsState(
        when {
            focused -> Color.White.copy(alpha = 0.22f)
            active -> Color.Black.copy(alpha = 0.30f)
            else -> Color.Transparent
        },
        label = "navBg"
    )

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = if (expanded) 14.dp else 0.dp),
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            item.icon,
            contentDescription = item.label,
            tint = if (focused || active) TextPrimary else TextPrimary.copy(alpha = 0.82f),
            modifier = Modifier.size(26.dp)
        )
        if (expanded) {
            Spacer(Modifier.width(14.dp))
            Text(
                item.label,
                color = if (focused || active) TextPrimary else TextPrimary.copy(alpha = 0.82f),
                fontWeight = if (active || focused) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
