package com.izplay.tv.data.model

import kotlinx.serialization.Serializable
import java.net.URLEncoder

/**
 * Configuração central distribuída pelo Painel Admin em
 * `GET http://admin.izplay.tv/api/client/config` — mesma fonte do web/desktop.
 * Nenhum segredo aqui: só URLs/IPs públicos e flags. Chaves privadas de VPN e
 * senhas NUNCA entram nesta config nem no APK (provisionamento por dispositivo
 * é responsabilidade futura do painel/backend).
 */
@Serializable
data class VpnServerInfo(
    val id: String = "",
    val name: String = "",
    val country: String = "",
    val url: String = "",
    val endpoint: String = "",
    val subnet: String = ""
)

@Serializable
data class SwarmCloudInfo(
    val enabled: Boolean = false,
    val token: String = "",
    val appId: String = "",
    val superPeerUrl: String = ""
)

@Serializable
data class ClientConfig(
    val defaultDns: String = "",
    val dnsServers: List<String> = emptyList(),
    val proxyUrl: String = "",
    val telemetryUrl: String = "",
    val gatewayUrl: String = "",
    val videoGatewayUrl: String = "",
    val protectedGatewayUrl: String = "",
    val webPlayerUrl: String = "",
    val swarmCloud: SwarmCloudInfo = SwarmCloudInfo(),
    val vpnServers: List<VpnServerInfo> = emptyList()
) {
    /** Hosts diretos do provedor (defaultDns primeiro), normalizados com esquema. */
    val providerHosts: List<String>
        get() = (listOf(defaultDns) + dnsServers)
            .map { it.trim().trimEnd('/') }
            .filter { it.isNotBlank() }
            .map { if (it.startsWith("http", ignoreCase = true)) it else "http://$it" }
            .distinct()

    /** Resolve caminho relativo ("/gateway") contra o webPlayerUrl; absoluto passa direto. */
    private fun resolve(pathOrUrl: String): String? {
        val value = pathOrUrl.trim().trimEnd('/')
        if (value.isEmpty()) return null
        if (value.startsWith("http", ignoreCase = true)) return value
        val base = webPlayerUrl.trim().trimEnd('/')
        if (base.isEmpty()) return null
        return base + (if (value.startsWith("/")) value else "/$value")
    }

    /** Base do gateway de catálogo (fala o protocolo Xtream: player_api.php etc.). */
    val catalogGatewayBase: String? get() = resolve(gatewayUrl)

    /** Base do gateway de mídia (/proxy, /transcode, /health). */
    val videoGatewayBase: String? get() = resolve(videoGatewayUrl)

    val telemetryBase: String? get() = resolve(telemetryUrl)

    /**
     * Rota de fallback de reprodução: o stream DIRETO do provedor embrulhado no
     * /proxy do gateway de mídia. Regra da ARQUITETURA.md: o parâmetro url é
     * SEMPRE a URL direta do provedor, nunca uma rota proxificada de catálogo.
     */
    fun videoProxyUrl(directStreamUrl: String): String? {
        if (directStreamUrl.isBlank()) return null
        val base = videoGatewayBase ?: return null
        return "$base/proxy?url=" + URLEncoder.encode(directStreamUrl, "UTF-8")
    }
}
