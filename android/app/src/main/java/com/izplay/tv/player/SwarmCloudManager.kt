package com.izplay.tv.player

import android.net.Uri
import android.util.Log
import com.p2pengine.core.p2p.P2pStatisticsListener
import com.p2pengine.sdk.P2pEngine

/** SwarmCloud adapter used only by full-screen HLS live playback. */
internal object SwarmCloudManager {
    data class Statistics(
        val channelId: String = "",
        val connected: Boolean = false,
        val peers: Int = 0,
        val p2pDown: Long = 0,
        val p2pUp: Long = 0,
        val httpDown: Long = 0,
    )

    private val pilotChannelIds = setOf("82755", "1168916")
    @Volatile private var statistics = Statistics()
    @Volatile private var listenerRegistered = false

    fun snapshot(): Statistics = statistics

    fun playbackCandidates(
        originalUrl: String?,
        gatewayFallbackUrl: String?,
        enableP2p: Boolean,
    ): List<String> {
        if (originalUrl.isNullOrBlank()) return emptyList()

        val directAndGateway = listOfNotNull(originalUrl, gatewayFallbackUrl)
            .filter { it.isNotBlank() }
            .distinct()
        if (!enableP2p || !originalUrl.isHlsOrDashManifest()) {
            return directAndGateway
        }
        val channelId = originalUrl.substringBefore('?').substringAfterLast('/')
            .substringBefore('.').takeIf { it in pilotChannelIds }
            ?: return directAndGateway
        statistics = Statistics(channelId = channelId)
        registerStatisticsListener()

        val parsed = runCatching {
            P2pEngine.instance?.parseStreamUrl(originalUrl, "live-$channelId")
        }.onFailure {
            Log.w(TAG, "SwarmCloud URL resolution failed; using direct channel")
        }.getOrNull()

        if (parsed.isNullOrBlank() || parsed == originalUrl) {
            Log.i(TAG, "SwarmCloud unavailable; using direct channel")
            return directAndGateway
        }

        val host = runCatching { Uri.parse(parsed).host }.getOrNull()
        val loopback = host == "127.0.0.1" || host == "localhost" || host == "::1"
        Log.i(TAG, "SwarmCloud live URL resolved; loopback=$loopback")
        return (listOf(parsed) + directAndGateway).distinct()
    }

    fun stopCurrentStream() {
        runCatching { P2pEngine.instance?.stopP2p() }
        statistics = Statistics()
    }

    private fun registerStatisticsListener() {
        if (listenerRegistered) return
        val engine = P2pEngine.instance ?: return
        engine.addP2pStatisticsListener(object : P2pStatisticsListener {
            override fun onHttpDownloaded(bytes: Int) {
                statistics = statistics.copy(httpDown = statistics.httpDown + bytes.coerceAtLeast(0))
            }

            override fun onP2pDownloaded(bytes: Int, speed: Int) {
                statistics = statistics.copy(p2pDown = statistics.p2pDown + bytes.coerceAtLeast(0))
            }

            override fun onP2pUploaded(bytes: Int, speed: Int) {
                statistics = statistics.copy(p2pUp = statistics.p2pUp + bytes.coerceAtLeast(0))
            }

            override fun onPeers(peers: List<String>) {
                statistics = statistics.copy(peers = peers.size)
            }

            override fun onServerConnected(connected: Boolean) {
                statistics = statistics.copy(connected = connected)
            }
        })
        listenerRegistered = true
    }

    private fun String.isHlsOrDashManifest(): Boolean {
        val clean = substringBefore('#').substringBefore('?').lowercase()
        return clean.endsWith(".m3u8") || clean.endsWith(".mpd")
    }

    private const val TAG = "SwarmCloud"
}
