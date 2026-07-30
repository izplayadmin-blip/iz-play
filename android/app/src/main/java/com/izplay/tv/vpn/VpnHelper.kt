package com.izplay.tv.vpn

import android.content.Context
import android.content.Intent

/**
 * Camada preparada para a "Rota protegida" (WireGuard).
 *
 * Estado atual — por decisão de segurança, o app NÃO embute chave privada nem
 * credencial de VPS: a arquitetura correta é o painel/backend provisionar um
 * peer por dispositivo (futuro). Até lá, a integração aceitável é abrir o app
 * oficial WireGuard quando instalado; os servidores (IP público/endpoint) vêm
 * da config central e são exibidos com status na tela de Configurações.
 */
object VpnHelper {

    private val WIREGUARD_PACKAGES = listOf("com.wireguard.android")

    fun isWireGuardInstalled(context: Context): Boolean =
        WIREGUARD_PACKAGES.any { pkg ->
            runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
        }

    /** Abre o app WireGuard, se instalado. Retorna false quando não há como abrir. */
    fun openWireGuard(context: Context): Boolean {
        val intent = WIREGUARD_PACKAGES.firstNotNullOfOrNull {
            context.packageManager.getLaunchIntentForPackage(it)
        } ?: return false
        return runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
    }
}
