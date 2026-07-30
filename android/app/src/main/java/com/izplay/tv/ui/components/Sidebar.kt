package com.izplay.tv.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izplay.tv.R
import com.izplay.tv.ui.theme.IzRedDark
import com.izplay.tv.ui.theme.TextPrimary

enum class NavItem(val label: String, val icon: ImageVector) {
    INICIO("Início", Icons.Outlined.Home),
    FAVORITOS("Minha lista", Icons.Outlined.StarBorder),
    CANAIS("Ao vivo", Icons.Outlined.LiveTv),
    FILMES("Filmes", Icons.Outlined.Movie),
    SERIES("Séries", Icons.Outlined.PlayCircle),
    CONFIG("Configuração", Icons.Outlined.Settings),
    NOTIFICACOES("Notificações", Icons.Outlined.Notifications),
    USUARIOS("Perfis", Icons.Outlined.AccountCircle),
}

private val NAV_TOP = listOf(
    NavItem.INICIO,
    NavItem.FAVORITOS,
    NavItem.CANAIS,
    NavItem.FILMES,
    NavItem.SERIES
)

private val NAV_BOTTOM = listOf(
    NavItem.CONFIG,
    NavItem.NOTIFICACOES,
    NavItem.USUARIOS
)

private val COLLAPSED_WIDTH = 94.dp
private val EXPANDED_WIDTH = 230.dp

@Composable
fun Sidebar(
    selected: NavItem,
    clock: String,
    date: String,
    profileName: String = "Perfil",
    onSelect: (NavItem) -> Unit,
    selectedFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val firstFocus = remember { FocusRequester() }
    var navHasFocus by remember { mutableStateOf(false) }
    val expanded = navHasFocus
    val width = if (expanded) EXPANDED_WIDTH else COLLAPSED_WIDTH
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(width)
            .background(Brush.verticalGradient(listOf(Color(0xFF070707), Color(0xFF0B0B0B))))
            .focusGroup()
            .onFocusChanged {
                navHasFocus = it.hasFocus
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(82.dp),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = expanded,
                animationSpec = tween(durationMillis = 170),
                label = "sidebarBrand"
            ) { isExpanded ->
                if (isExpanded) {
                    Image(
                        painter = painterResource(R.drawable.iz_sidebar_brand_official),
                        contentDescription = "IZ Play",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.width(178.dp).height(44.dp)
                    )
                } else {
                    // Recorte literal do mesmo wordmark oficial: apenas a palavra PLAY some.
                    Image(
                        painter = painterResource(R.drawable.iz_sidebar_mark_official),
                        contentDescription = "IZ",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.width(52.dp).height(44.dp)
                    )
                }
            }
        }

        Box(
            Modifier
                .width(44.dp)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.12f))
        )
        Spacer(Modifier.height(12.dp))

        NAV_TOP.forEach { item ->
            val itemModifier = Modifier
                .then(if (item == NavItem.INICIO) Modifier.focusRequester(firstFocus) else Modifier)
                .then(
                    if (item == selected && selectedFocusRequester != null) {
                        Modifier.focusRequester(selectedFocusRequester)
                    } else {
                        Modifier
                    }
                )
            NavIcon(
                item = item,
                active = item == selected,
                expanded = expanded,
                onFocus = {},
                onClick = {
                    onSelect(item)
                },
                modifier = itemModifier
            )
            Spacer(Modifier.height(6.dp))
        }

        Spacer(Modifier.weight(1f))

        NAV_BOTTOM.forEach { item ->
            val itemModifier = if (item == selected && selectedFocusRequester != null) {
                Modifier.focusRequester(selectedFocusRequester)
            } else {
                Modifier
            }
            NavIcon(
                item = item,
                active = item == selected,
                expanded = expanded,
                onFocus = {},
                onClick = {
                    onSelect(item)
                },
                modifier = itemModifier,
            )
            Spacer(Modifier.height(6.dp))
        }

        Row(
            Modifier
                .width(if (expanded) 206.dp else 62.dp)
                .padding(bottom = 12.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF171717))
                    .border(1.5.dp, Color(0xFFD60000), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    profileName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }
            if (expanded) {
                Spacer(Modifier.width(13.dp))
                Text(
                    profileName,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun NavIcon(
    item: NavItem,
    active: Boolean,
    expanded: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val context = LocalContext.current
    val economical = remember(context) { context.isIzLowMode() }
    val scale by animateFloatAsState(
        if (focused && !economical) 1.06f else 1f,
        animationSpec = tween(if (economical) 0 else 120),
        label = "navScale"
    )
    val shape = RoundedCornerShape(10.dp)
    val bg = when {
        active -> Color(0xFF180606)
        focused -> Color.White.copy(alpha = 0.04f)
        else -> Color.Transparent
    }

    Row(
        modifier
            .width(if (expanded) 210.dp else 68.dp)
            .height(58.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(bg)
            .then(
                if (focused) Modifier.border(2.dp, Color(0xFFE00000), shape)
                else if (active) Modifier.border(1.dp, Color(0xFFB80000), shape)
                else Modifier
            )
            .onFocusChanged { if (it.isFocused) onFocus() }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = if (expanded) 17.dp else 0.dp),
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            item.icon,
            contentDescription = item.label,
            tint = TextPrimary.copy(alpha = if (focused || active) 1f else 0.72f),
            modifier = Modifier.size(28.dp)
        )
        if (expanded) {
            Spacer(Modifier.width(16.dp))
            Text(
                item.label,
                color = TextPrimary.copy(alpha = if (focused || active) 1f else 0.82f),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                maxLines = 1
            )
        }
    }
}
