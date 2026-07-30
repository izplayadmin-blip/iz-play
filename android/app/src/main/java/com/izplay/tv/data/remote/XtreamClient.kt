package com.izplay.tv.data.remote

import android.util.Log
import android.util.Base64
import com.izplay.tv.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Cliente para painéis no padrão Xtream Codes.
 *
 * Endpoints de canais ao vivo:
 *   get_live_categories / get_live_streams
 *   get_short_epg?stream_id=X&limit=12
 *
 * Endpoints de VOD (filmes):
 *   get_vod_categories / get_vod_streams
 *
 * Endpoints de séries:
 *   get_series_categories / get_series / get_series_info?series_id=X
 */
class XtreamClient(
    private val config: ProviderConfig,
    private val http: OkHttpClient = AppHttpClient.create(),
    apiBase: String? = null
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Base dos STREAMS: sempre o host direto do provedor. Regra da ARQUITETURA.md —
     *  URLs de mídia nunca apontam para rota proxificada de catálogo (dá 403/tela preta
     *  no transcode); o fallback de reprodução usa /video-gateway/proxy?url=DIRETA. */
    private val base = config.xtreamHost.trimEnd('/')

    /** Base das chamadas de CATÁLOGO (player_api.php): pode ser o gateway central,
     *  que resolve TV box com a DNS direta bloqueada e aproveita o cache da VPS. */
    private val catalogBase = (apiBase?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() }) ?: base

    private fun queryValue(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun api(action: String, extra: String = ""): String =
        "$catalogBase/player_api.php?username=${queryValue(config.xtreamUser)}" +
                "&password=${queryValue(config.xtreamPass)}&action=${queryValue(action)}$extra"

    private fun streamUrl(kind: String, streamId: String, extension: String): String =
        base.toHttpUrl().newBuilder()
            .addPathSegment(kind)
            .addPathSegment(config.xtreamUser)
            .addPathSegment(config.xtreamPass)
            .addPathSegment("$streamId.$extension")
            .build()
            .toString()

    private fun ratingFrom(vararg objects: JsonObject): String? {
        val fields = listOf(
            "rating" to false,
            "imdb_rating" to false,
            "tmdb_rating" to false,
            "rating_10based" to false,
            "rating_5based" to true
        )
        for ((key, fiveBased) in fields) {
            val raw = objects.firstNotNullOfOrNull { obj ->
                (obj[key] as? JsonPrimitive)?.content?.trim()
                    ?.takeIf { it.isNotBlank() && !it.equals("null", true) }
            } ?: continue
            val parsed = raw.replace(',', '.').toDoubleOrNull() ?: continue
            if (parsed <= 0.0) continue
            val tenBased = if (fiveBased && parsed <= 5.0) parsed * 2.0 else parsed
            if (tenBased > 10.0) continue
            return if (tenBased % 1.0 == 0.0) tenBased.toInt().toString()
            else String.format(Locale.US, "%.1f", tenBased)
        }
        return null
    }

    /**
     * Rede + parse + mapeamento SEMPRE fora da thread principal: listas IPTV chegam a
     * dezenas de MB e o parse na main thread congelava a UI em TV boxes fracas.
     * decodeFromStream evita ainda a cópia intermediária do corpo inteiro em String.
     */
    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun getJson(url: String): JsonElement = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(url).withPlayerHeaders().build()).execute().use { resp ->
            val safeUrl = url.toHttpUrl()
            val route = "${safeUrl.scheme}://${safeUrl.host}:${safeUrl.port}${safeUrl.encodedPath}"
            if (!resp.isSuccessful) {
                Log.w("IZCatalog", "$route -> HTTP ${resp.code}")
                error("HTTP ${resp.code}")
            }
            val stream = resp.body?.byteStream() ?: error("Resposta vazia")
            val decoded = json.decodeFromStream<JsonElement>(stream)
            val size = (decoded as? JsonArray)?.size
            Log.i("IZCatalog", "$route -> JSON ${size?.let { "$it itens" } ?: "objeto"}")
            decoded
        }
    }

    // ─── Canais ao vivo ────────────────────────────────────────────────────

    suspend fun fetchCategories(): List<Category> = withContext(Dispatchers.Default) {
        val arr = getJson(api("get_live_categories")) as? JsonArray ?: return@withContext emptyList()
        arr.mapNotNull { el ->
            val obj = el.jsonObject
            val id = obj["category_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val name = obj["category_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            Category(id = id, name = name)
        }
    }

    suspend fun fetchChannels(): List<Channel> = withContext(Dispatchers.Default) {
        val arr = getJson(api("get_live_streams")) as? JsonArray ?: return@withContext emptyList()
        arr.mapIndexedNotNull { index, el ->
            val obj = el.jsonObject
            val streamId = obj["stream_id"]?.jsonPrimitive?.content ?: return@mapIndexedNotNull null
            val name = obj["name"]?.jsonPrimitive?.content ?: "Canal"
            val logo = obj["stream_icon"]?.jsonPrimitive?.content?.ifBlank { null }
            val catId = obj["category_id"]?.jsonPrimitive?.content ?: ""
            val num = obj["num"]?.jsonPrimitive?.content?.toIntOrNull() ?: (index + 1)
            val epgId = obj["epg_channel_id"]?.jsonPrimitive?.content?.ifBlank { null }
            Channel(
                id = streamId,
                number = num,
                name = name,
                logoUrl = logo,
                streamUrl = streamUrl("live", streamId, "m3u8"),
                categoryId = catId,
                epgChannelId = epgId
            )
        }
    }

    // ─── EPG (guia de programação) ─────────────────────────────────────────

    suspend fun fetchShortEpg(streamId: String, limit: Int = 12): List<EpgEntry> = withContext(Dispatchers.Default) {
        // `get_short_epg` e o formato moderno; boxes/painéis antigos expõem o guia
        // pelo alias `get_simple_data_table`. Aceita o primeiro que tiver registros.
        val listings = listOf(
            "get_short_epg" to "&stream_id=$streamId&limit=$limit",
            "get_simple_data_table" to "&stream_id=$streamId"
        ).firstNotNullOfOrNull { (action, extra) ->
            runCatching {
                getJson(api(action, extra)).jsonObject["epg_listings"]?.jsonArray
                    ?.takeIf { it.isNotEmpty() }
            }.getOrNull()
        } ?: return@withContext emptyList()

        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        listings.mapNotNull { el ->
            val obj = el.jsonObject
            // Paineis Xtream variam entre timestamps e datas textuais. Os timestamps
            // sao preferidos porque nao sofrem com timezone configurado incorretamente.
            val startTs = obj["start_timestamp"]?.jsonPrimitive?.content?.toLongOrNull()
            val endTs = (obj["stop_timestamp"] ?: obj["end_timestamp"])
                ?.jsonPrimitive?.content?.toLongOrNull()
            val start = startTs?.times(1000L) ?: obj["start"]?.jsonPrimitive?.content
                ?.let { runCatching { fmt.parse(it)?.time }.getOrNull() }
                ?: return@mapNotNull null
            val end = endTs?.times(1000L)
                ?: (obj["end"] ?: obj["stop"])?.jsonPrimitive?.content
                    ?.let { runCatching { fmt.parse(it)?.time }.getOrNull() }
                ?: start

            EpgEntry(
                channelId = streamId,
                title = decodeb64(obj["title"]?.jsonPrimitive?.content),
                description = decodeb64(obj["description"]?.jsonPrimitive?.content),
                start = start,
                end = end
            )
        }
    }

    private fun decodeb64(value: String?): String {
        if (value.isNullOrBlank()) return "Sem título"
        return runCatching {
            String(Base64.decode(value, Base64.DEFAULT), Charsets.UTF_8).trim()
        }.getOrDefault(value)
    }

    // ─── VOD (filmes) ──────────────────────────────────────────────────────

    suspend fun fetchVodCategories(): List<Category> = withContext(Dispatchers.Default) {
        val arr = getJson(api("get_vod_categories")) as? JsonArray ?: return@withContext emptyList()
        arr.mapNotNull { el ->
            val obj = el.jsonObject
            val id = obj["category_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val name = obj["category_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            Category(id = id, name = name)
        }
    }

    suspend fun fetchVodStreams(): List<VodItem> = withContext(Dispatchers.Default) {
        val arr = getJson(api("get_vod_streams")) as? JsonArray ?: return@withContext emptyList()
        arr.mapNotNull { el ->
            val obj = el.jsonObject
            val streamId = obj["stream_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.content ?: "Filme"
            val poster = obj["stream_icon"]?.jsonPrimitive?.content?.ifBlank { null }
            val catId = obj["category_id"]?.jsonPrimitive?.content ?: ""
            val rating = ratingFrom(obj)
            val plot = obj["plot"]?.jsonPrimitive?.content?.ifBlank { null }
            val year = obj["year"]?.jsonPrimitive?.content?.ifBlank { null }
            val dur = obj["duration_secs"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val addedAt = obj["added"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
            val backdrop = (obj["backdrop_path"] as? JsonArray)
                ?.firstOrNull()?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                ?: obj["backdrop"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            val ext = obj["container_extension"]?.jsonPrimitive?.content ?: "mp4"
            VodItem(
                id = streamId,
                name = name,
                posterUrl = poster,
                streamUrl = streamUrl("movie", streamId, ext),
                categoryId = catId,
                rating = rating,
                plot = plot,
                year = year,
                durationSecs = dur,
                addedAt = addedAt,
                backdropUrl = backdrop
            )
        }
    }

    /** A listagem VOD costuma omitir sinopse/duracao. O detalhe e carregado somente
     *  ao abrir o filme para nao disparar milhares de requisicoes no catalogo. */
    suspend fun fetchVodInfo(vod: VodItem): VodItem = withContext(Dispatchers.Default) {
        val root = runCatching {
            getJson(api("get_vod_info", "&vod_id=${vod.id}")).jsonObject
        }.getOrNull() ?: return@withContext vod
        val info = root["info"] as? JsonObject ?: JsonObject(emptyMap())
        val movie = root["movie_data"] as? JsonObject ?: JsonObject(emptyMap())

        fun value(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
            (info[key] ?: movie[key] ?: root[key])
                ?.let { it as? JsonPrimitive }
                ?.content
                ?.trim()
                ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        }
        fun durationSeconds(): Int {
            value("duration_secs", "duration_seconds")?.toIntOrNull()?.let { return it }
            val parts = value("duration")?.split(':')?.mapNotNull(String::toIntOrNull) ?: return 0
            return when (parts.size) {
                3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
                2 -> parts[0] * 60 + parts[1]
                else -> 0
            }
        }
        fun imageValue(vararg keys: String): String? {
            keys.forEach { key ->
                val raw = info[key] ?: movie[key] ?: root[key]
                val candidate = when (raw) {
                    is JsonArray -> raw.firstOrNull()?.jsonPrimitive?.content
                    is JsonPrimitive -> raw.content
                    else -> null
                }?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                if (candidate != null) return candidate
            }
            return null
        }
        fun floatValue(default: Float, vararg keys: String): Float =
            value(*keys)?.toFloatOrNull() ?: default

        vod.copy(
            plot = value("plot", "description", "overview") ?: vod.plot,
            rating = ratingFrom(info, movie, root) ?: vod.rating?.takeUnless { it.toDoubleOrNull() == 0.0 },
            year = value("year", "releasedate", "release_date")?.take(4) ?: vod.year,
            durationSecs = durationSeconds().takeIf { it > 0 } ?: vod.durationSecs,
            posterUrl = imageValue("movie_image", "cover_big", "stream_icon") ?: vod.posterUrl,
            backdropUrl = imageValue("backdropUrl", "backdrop_url", "backdrop_path", "backdrop")
                ?: vod.backdropUrl,
            backdropMobileUrl = imageValue("backdropMobileUrl", "backdrop_mobile_url", "backdrop_mobile")
                ?: vod.backdropMobileUrl,
            backdropPositionX = floatValue(vod.backdropPositionX, "backdropPositionX", "backdrop_position_x")
                .coerceIn(0f, 100f),
            backdropPositionY = floatValue(vod.backdropPositionY, "backdropPositionY", "backdrop_position_y")
                .coerceIn(0f, 100f),
            backdropScale = floatValue(vod.backdropScale, "backdropScale", "backdrop_scale")
                .coerceIn(1f, 1.3f),
            overlayOpacity = floatValue(vod.overlayOpacity, "overlayOpacity", "overlay_opacity")
                .coerceIn(0f, 1f)
        )
    }

    // ─── Séries ────────────────────────────────────────────────────────────

    suspend fun fetchSeriesCategories(): List<Category> = withContext(Dispatchers.Default) {
        val arr = getJson(api("get_series_categories")) as? JsonArray ?: return@withContext emptyList()
        arr.mapNotNull { el ->
            val obj = el.jsonObject
            val id = obj["category_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val name = obj["category_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            Category(id = id, name = name)
        }
    }

    suspend fun fetchSeries(): List<SeriesItem> = withContext(Dispatchers.Default) {
        val arr = getJson(api("get_series")) as? JsonArray ?: return@withContext emptyList()
        arr.mapNotNull { el ->
            val obj = el.jsonObject
            val id = obj["series_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.content ?: "Série"
            val cover = obj["cover"]?.jsonPrimitive?.content?.ifBlank { null }
            val catId = obj["category_id"]?.jsonPrimitive?.content ?: ""
            val rating = ratingFrom(obj)
            val plot = obj["plot"]?.jsonPrimitive?.content?.ifBlank { null }
            val year = obj["year"]?.jsonPrimitive?.content?.ifBlank { null }
            SeriesItem(
                id = id,
                name = name,
                coverUrl = cover,
                categoryId = catId,
                rating = rating,
                plot = plot,
                year = year
            )
        }
    }

    suspend fun fetchSeriesInfo(series: SeriesItem): SeriesDetail = withContext(Dispatchers.Default) {
        val root = runCatching { getJson(api("get_series_info", "&series_id=${series.id}")).jsonObject }.getOrNull()
            ?: return@withContext SeriesDetail(series, emptyList())
        val info = root["info"] as? JsonObject ?: JsonObject(emptyMap())
        fun detail(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
            (info[key] ?: root[key])?.let { it as? JsonPrimitive }?.content?.trim()
                ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        }
        val detailedSeries = series.copy(
            plot = detail("plot", "description", "overview") ?: series.plot,
            rating = ratingFrom(info, root) ?: series.rating?.takeUnless { it.toDoubleOrNull() == 0.0 },
            year = detail("year", "releaseDate", "releasedate")?.take(4) ?: series.year,
            coverUrl = detail("cover_big", "cover", "movie_image") ?: series.coverUrl
        )
        val episodesMap = root["episodes"]?.jsonObject
            ?: return@withContext SeriesDetail(detailedSeries, emptyList())

        val seasons = episodesMap.entries.mapNotNull { (seasonKey, seasonArr) ->
            val seasonNum = seasonKey.toIntOrNull() ?: return@mapNotNull null
            val eps = seasonArr.jsonArray.mapNotNull { el ->
                val obj = el.jsonObject
                val epId = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val title = obj["title"]?.jsonPrimitive?.content ?: "Episódio"
                val epNum = obj["episode_num"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val ext = obj["container_extension"]?.jsonPrimitive?.content ?: "mp4"
                val dur = obj["duration_secs"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val plot = obj["plot"]?.jsonPrimitive?.content?.ifBlank { null }
                val still = obj["info"]?.jsonObject?.get("movie_image")?.jsonPrimitive?.content?.ifBlank { null }
                Episode(
                    id = epId,
                    title = title,
                    episodeNum = epNum,
                    streamUrl = streamUrl("series", epId, ext),
                    durationSecs = dur,
                    plot = plot,
                    stillUrl = still
                )
            }.sortedBy { it.episodeNum }
            Season(seasonNum, eps)
        }.sortedBy { it.seasonNumber }
        SeriesDetail(detailedSeries, seasons)
    }
}
