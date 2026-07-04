package com.izplay.tv.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Config remota vinda do Painel Admin (mesma fonte do web/desktop).
 * Fornece a(s) DNS/host do provedor que o admin gerencia centralmente.
 * Assim o login do app fica só usuário/senha e a troca de servidor é feita no painel.
 *
 * Lê os campos `defaultDns` (host principal) e `dnsServers` (lista) do
 * `GET /api/client/config`. Se o painel não responder, cai nos fallbacks.
 */
class RemoteConfigClient(
    private val configUrl: String = DEFAULT_URL,
    private val http: OkHttpClient = OkHttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Retorna a lista de hosts (primeiro = principal). Vazia se não conseguir. */
    suspend fun fetchHosts(): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = http.newCall(Request.Builder().url(configUrl).build()).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                resp.body?.string().orEmpty()
            }
            val root = json.parseToJsonElement(body) as? JsonObject ?: return@runCatching emptyList()
            val hosts = mutableListOf<String>()
            (root["dnsServers"] as? JsonArray)?.forEach { el ->
                (el as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }?.let { hosts.add(normalize(it)) }
            }
            (root["defaultDns"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
                ?.let { hosts.add(0, normalize(it)) }
            hosts.distinct()
        }.getOrDefault(emptyList())
    }

    private fun normalize(host: String): String {
        var s = host.trim().trimEnd('/')
        if (!s.startsWith("http", ignoreCase = true)) s = "http://$s"
        return s
    }

    companion object {
        /** Painel Admin — mesma origem de config do web/desktop. */
        const val DEFAULT_URL = "https://admin.izplay.tv/api/client/config"

        /** Fallbacks se o painel não responder (as duas DNS em uso hoje; cxst como principal). */
        val FALLBACK_HOSTS = listOf("http://cxst.shop", "http://sopvrt.shop")
    }
}
