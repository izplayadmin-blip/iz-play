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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
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
    initialPositionMs: Long = 0L,
    onProgress: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> },
    onClose: () -> Unit
) {
    val surfaceFocus = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }
    var stopPlayerNow by remember { mutableStateOf<(() -> Unit)?>(null) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var barVisible by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(initialPositionMs) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var seekToMs by remember { mutableStateOf<Long?>(null) }
    val isLive = subtitle == "AO VIVO"

    fun closePlayback() {
        stopPlayerNow?.invoke()
        onClose()
    }

    BackHandler(onBack = ::closePlayback)

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
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    lastInteraction = System.currentTimeMillis()
                    when {
                        !isLive && durationMs > 0L && event.key == Key.DirectionLeft -> {
                            seekToMs = (positionMs - 10_000L).coerceAtLeast(0L)
                            positionMs = seekToMs ?: positionMs
                            true
                        }
                        !isLive && durationMs > 0L && event.key == Key.DirectionRight -> {
                            seekToMs = (positionMs + 10_000L).coerceAtMost(durationMs)
                            positionMs = seekToMs ?: positionMs
                            true
                        }
                        else -> false
                    }
                }
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
            initialPositionMs = initialPositionMs,
            seekToMs = seekToMs,
            onStopHandle = { stopPlayerNow = it },
            onProgress = { position, duration ->
                positionMs = position
                durationMs = duration
                onProgress(position, duration)
            },
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

        AnimatedVisibility(
            visible = barVisible && !isLive && durationMs > 0L,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                LinearProgressIndicator(
                    progress = { (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = IzRed,
                    trackColor = Color.White.copy(alpha = 0.25f),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatPlaybackTime(positionMs), color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        "◀ 10s    OK controles    10s ▶",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(formatPlaybackTime(durationMs), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatPlaybackTime(valueMs: Long): String {
    val totalSeconds = (valueMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
