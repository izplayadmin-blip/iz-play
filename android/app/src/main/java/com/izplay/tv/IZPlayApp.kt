package com.izplay.tv

import android.app.Application
import android.app.ActivityManager
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.p2pengine.core.p2p.P2pConfig
import com.p2pengine.core.tracking.TrackerZone
import com.p2pengine.sdk.P2pEngine

/**
 * Configuração global do Coil pensada para TV box fraca (2GB de RAM):
 * - RGB_565: metade da memória por bitmap (posters/logos não precisam de alfa);
 * - cache de memória contido para não disputar RAM com o catálogo;
 * - cache em disco para logos/posters não baixarem de novo a cada boot;
 * - sem crossfade: menos trabalho de composição ao rolar as fileiras.
 */
class IZPlayApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        initializeSwarmCloud()
    }

    private fun initializeSwarmCloud() {
        val token = BuildConfig.SWARMCLOUD_TOKEN
        val setTopBox = isTelevisionDevice()
        if (token.isBlank()) {
            Log.i(TAG, "SwarmCloud disabled: no local token; setTopBox=$setTopBox")
            return
        }

        val config = P2pConfig.Builder()
            .trackerZone(TrackerZone.USA)
            .insertTimeOffsetTag(0.0)
            .isSetTopBox(setTopBox)
            .build()

        runCatching { P2pEngine.init(this, token, config) }
            .onSuccess { Log.i(TAG, "SwarmCloud initialized; setTopBox=$setTopBox") }
            .onFailure { Log.w(TAG, "SwarmCloud initialization failed; direct playback remains available") }
    }

    private fun isTelevisionDevice(): Boolean {
        val mode = (getSystemService(Context.UI_MODE_SERVICE) as UiModeManager).currentModeType
        return mode == Configuration.UI_MODE_TYPE_TELEVISION ||
            packageManager.hasSystemFeature("android.software.leanback")
    }

    override fun newImageLoader(): ImageLoader {
        val lowRam = (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
            ?.isLowRamDevice == true || Runtime.getRuntime().maxMemory() <= 192L * 1024 * 1024
        return ImageLoader.Builder(this)
        .allowRgb565(true)
        .bitmapConfig(Bitmap.Config.RGB_565)
        .crossfade(false)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(if (lowRam) 0.06 else 0.10)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(if (lowRam) 64L * 1024 * 1024 else 96L * 1024 * 1024)
                .build()
        }
        .respectCacheHeaders(false)
        .build()
    }

    private companion object {
        const val TAG = "IZPlayApp"
    }
}
