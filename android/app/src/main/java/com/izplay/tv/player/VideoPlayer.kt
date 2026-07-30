package com.izplay.tv.player

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.izplay.tv.data.remote.AppHttpClient
import kotlinx.coroutines.delay

/**
 * Player de vídeo baseado em ExoPlayer (Media3).
 * Recria o MediaItem quando a URL muda e libera o player ao sair de composição.
 *
 * `fallbackUrl` (opcional): mesma mídia roteada pelo gateway central
 * (/video-gateway/proxy?url=DIRETA). Se a reprodução DIRETA falhar (DNS
 * bloqueada, 403, rede), troca automaticamente uma única vez para o gateway —
 * protege a banda da VPS usando o gateway só quando é preciso.
 */
@Composable
fun VideoPlayer(
    streamUrl: String?,
    fallbackUrl: String? = null,
    enableP2p: Boolean = false,
    initialPositionMs: Long = 0L,
    seekToMs: Long? = null,
    onProgress: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playbackCandidates = remember(streamUrl, fallbackUrl, enableP2p) {
        SwarmCloudManager.playbackCandidates(streamUrl, fallbackUrl, enableP2p)
    }
    var candidateIndex by remember(playbackCandidates) { mutableIntStateOf(0) }
    val currentUrl = playbackCandidates.getOrNull(candidateIndex)
    var lastPositionMs by remember(streamUrl) { mutableStateOf(initialPositionMs) }

    val exoPlayer = remember {
        val dataSourceFactory = OkHttpDataSource.Factory(AppHttpClient.create())
            .setDefaultRequestProperties(AppHttpClient.defaultHeaders)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory))
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        5_000, 15_000,
                        500, 1_000
                    )
                    .build()
            )
            .build()
            .apply { playWhenReady = true }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (candidateIndex < playbackCandidates.lastIndex) {
                    candidateIndex += 1
                    Log.w(TAG, "Playback route failed; trying the next safe route")
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(currentUrl) {
        val url = currentUrl
        if (!url.isNullOrBlank()) {
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .apply {
                    val original = streamUrl
                        ?.substringBefore('#')
                        ?.substringBefore('?')
                        ?.lowercase()
                    if (original?.endsWith(".m3u8") == true) {
                        setMimeType(MimeTypes.APPLICATION_M3U8)
                    } else if (original?.endsWith(".mpd") == true) {
                        setMimeType(MimeTypes.APPLICATION_MPD)
                    }
                }
                .build()
            exoPlayer.setMediaItem(mediaItem)
            val resumeAt = maxOf(initialPositionMs, lastPositionMs)
            if (resumeAt > 0L) exoPlayer.seekTo(resumeAt)
            exoPlayer.prepare()
            exoPlayer.play()
        } else {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    LaunchedEffect(seekToMs) {
        seekToMs?.let {
            val target = it.coerceAtLeast(0L)
            lastPositionMs = target
            exoPlayer.seekTo(target)
        }
    }

    LaunchedEffect(exoPlayer, currentUrl) {
        while (true) {
            val position = exoPlayer.currentPosition.coerceAtLeast(0L)
            val duration = exoPlayer.duration.coerceAtLeast(0L)
            lastPositionMs = position
            onProgress(position, duration)
            delay(500)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            if (enableP2p) SwarmCloudManager.stopCurrentStream()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        }
    )
}

private const val TAG = "VideoPlayer"
