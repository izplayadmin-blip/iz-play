package com.izplay.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.izplay.tv.ui.theme.IzRed

/**
 * Cartão navegável por D-pad conforme o Focus System oficial do IZ Play: ao receber foco
 * (setas do controle) ganha **borda vermelha #CC0000 2dp + leve zoom** (nunca foco azul/branco
 * padrão do Android). Reaproveitado por todas as telas para manter a navegação igual à do
 * web/desktop. Escala padrão de card = 1.08; para botões passe `focusScale = 1.03`.
 * `content` recebe o estado de foco para ajustar seu próprio visual se quiser.
 */
@Composable
fun TvCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp), // radius token "card"
    focusScale: Float = 1.08f,
    content: @Composable BoxScope.(focused: Boolean) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        if (focused) focusScale else 1f,
        animationSpec = tween(150),
        label = "tvCardScale"
    )

    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .then(if (focused) Modifier.border(2.dp, IzRed, shape) else Modifier)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        content(focused)
    }
}

/** Estado de foco para linhas de lista (categorias, canais, episódios). O chamador aplica
 *  o `source` no `clickable` e usa `focused` para destacar a linha ao navegar com o D-pad. */
data class TvFocusState(val source: MutableInteractionSource, val focused: Boolean)

@Composable
fun rememberTvFocus(): TvFocusState {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    return TvFocusState(source, focused)
}
