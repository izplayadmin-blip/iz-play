package com.izplay.tv.ui.components

import android.app.ActivityManager
import android.content.Context

/**
 * Ativa automaticamente o perfil econômico em aparelhos com pouca memória.
 * Também cobre boxes antigas que não declaram corretamente isLowRamDevice.
 */
fun Context.isIzLowMode(): Boolean {
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val heapMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L)
    return manager?.isLowRamDevice == true || heapMb <= 192L
}
