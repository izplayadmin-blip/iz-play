package com.izplay.tv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izplay.tv.player.VideoPlayer
import com.izplay.tv.ui.theme.IzRed
import com.izplay.tv.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Reprodução em TELA CHEIA global: renderizada na raiz do app, acima de tudo
 * (inclusive da sidebar). Feita para controle remoto:
 * - Voltar sai da tela cheia (BackHandler);
 * - qualquer tecla mostra a barra de título, que se esconde sozinha em 4s;
 * - OK/click também alterna a barra.
 */
@Composable
fun FullscreenPlayback(
    streamUrl: String,
    title: String? = null,
    subtitle: String? = null,
    fallbackUrl: String? = null,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val surfaceFocus = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var barVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) { runCatching { surfaceFocus.requestFocus() } }
    LaunchedEffect(lastInteraction) {
        barVisible = true
        delay(4000)
        barVisible = false
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(surfaceFocus)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) lastInteraction = System.currentTimeMillis()
                false
            }
            .clickable(interactionSource = interaction, indication = null) {
                lastInteraction = System.currentTimeMillis()
            }
            .focusable(interactionSource = interaction)
    ) {
        VideoPlayer(
            streamUrl = streamUrl,
            fallbackUrl = fallbackUrl,
            enableP2p = subtitle == "AO VIVO",
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(visible = barVisible, enter = fadeIn(), exit = fadeOut()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.82f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 28.dp, vertical = 20.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                subtitle.uppercase(),
                                color = IzRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                        Text(
                            title ?: "",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                    Text(
                        "VOLTAR para sair",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
