package com.izplay.tv.data.remote

import com.izplay.tv.data.model.ClientConfig
import com.izplay.tv.data.model.SwarmCloudInfo
import com.izplay.tv.data.model.VpnServerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Config central vinda do Painel Admin (mesma fonte do web/desktop):
 * DNS do provedor, Proxy/Gateway, P2P/SwarmCloud, Super Peer e VPN WireGuard.
 * O admin muda no painel e todos os clientes seguem, sem atualizar o app.
 *
 * Parse defensivo: aceita `swarmCloud` aninhado (formato atual do painel) e o
 * formato plano antigo (`swarmCloudEnabled`/`swarmCloudKey`...). Se o painel
 * não responder, o chamador usa cache local e por fim os fallbacks fixos.
 */
class RemoteConfigClient(
    private val configUrl: String = DEFAULT_URL,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Config completa; null se o painel não responder ou o corpo for inválido. */
    suspend fun fetchConfig(): ClientConfig? = withContext(Dispatchers.IO) {
        runCatching {
            val body = http.newCall(Request.Builder().url(configUrl).build()).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                resp.body?.string().orEmpty()
            }
            parseConfig(json.parseToJsonElement(body).jsonObject)
        }.getOrNull()
    }

    /** Compatibilidade: só a lista de hosts do provedor (primeiro = principal). */
    suspend fun fetchHosts(): List<String> = fetchConfig()?.providerHosts ?: emptyList()

    private fun parseConfig(root: JsonObject): ClientConfig {
        fun str(key: String): String =
            (root[key] as? JsonPrimitive)?.content?.trim().orEmpty()

        val dnsServers = (root["dnsServers"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.content?.trim()?.takeIf(String::isNotBlank) }
            ?: emptyList()

        val swarmObj = root["swarmCloud"] as? JsonObject
        val swarm = if (swarmObj != null) {
            SwarmCloudInfo(
                enabled = (swarmObj["enabled"] as? JsonPrimitive)?.content?.toBoolean() ?: false,
                token = (swarmObj["token"] as? JsonPrimitive)?.content?.trim().orEmpty(),
                appId = (swarmObj["appId"] as? JsonPrimitive)?.content?.trim().orEmpty(),
                superPeerUrl = (swarmObj["superPeerUrl"] as? JsonPrimitive)?.content?.trim().orEmpty()
            )
        } else {
            // formato plano legado do painel
            SwarmCloudInfo(
                enabled = str("swarmCloudEnabled").toBoolean(),
                token = str("swarmCloudKey"),
                appId = str("swarmCloudAppId"),
                superPeerUrl = str("superPeerUrl")
            )
        }

        val vpns = (root["vpnServers"] as? JsonArray)?.mapNotNull { el ->
            val obj = (el as? JsonObject) ?: return@mapNotNull null
            fun v(key: String) = (obj[key] as? JsonPrimitive)?.content?.trim().orEmpty()
            VpnServerInfo(
                id = v("id"), name = v("name"), country = v("country"),
                url = v("url"), endpoint = v("endpoint"), subnet = v("subnet")
            ).takeIf { it.id.isNotBlank() || it.url.isNotBlank() }
        } ?: emptyList()

        return ClientConfig(
            defaultDns = str("defaultDns"),
            dnsServers = dnsServers,
            proxyUrl = str("proxyUrl"),
            telemetryUrl = str("telemetryUrl"),
            gatewayUrl = str("gatewayUrl"),
            videoGatewayUrl = str("videoGatewayUrl"),
            protectedGatewayUrl = str("protectedGatewayUrl"),
            webPlayerUrl = str("webPlayerUrl"),
            swarmCloud = swarm,
            vpnServers = vpns
        )
    }

    companion object {
        /** Painel Admin — mesma origem de config do web/desktop. */
        const val DEFAULT_URL = "http://admin.izplay.tv/api/client/config"

        /**
         * DNS do provedor com rotação: se uma cair (bloqueio/404), a cascata tenta
         * a próxima automaticamente. O ideal é o painel controlar isso via
         * defaultDns/dnsServers; esta lista é a rede de segurança local quando o
         * painel não responde. Ordem = ordem fornecida pelo operador.
         */
        val FALLBACK_HOSTS: List<String> = emptyList()

        /** Fallback seguro da config quando painel e cache falham (só URLs públicas). */
        val FALLBACK_CONFIG = ClientConfig(
            gatewayUrl = "/gateway",
            telemetryUrl = "http://admin.izplay.tv/api",
            videoGatewayUrl = "/video-gateway",
            webPlayerUrl = "https://web.izplay.tv/"
        )
    }
}
