package com.izplay.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Cartão navegável por D-pad: quando recebe foco (setas do controle) ganha borda branca e
 * um leve zoom — o "cursor" visual padrão de Android TV. Reaproveitado por todas as telas
 * para manter a navegação igual à do web/desktop. `content` recebe o estado de foco para
 * poder ajustar seu próprio visual se quiser.
 */
@Composable
fun TvCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable BoxScope.(focused: Boolean) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "tvCardScale")

    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .then(if (focused) Modifier.border(2.5.dp, Color.White, shape) else Modifier)
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
