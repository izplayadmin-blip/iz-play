package com.izplay.tv.data.remote

import android.util.Base64
import com.izplay.tv.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
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
    private val http: OkHttpClient = OkHttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val base = config.xtreamHost.trimEnd('/')

    private fun api(action: String, extra: String = ""): String =
        "$base/player_api.php?username=${config.xtreamUser}" +
                "&password=${config.xtreamPass}&action=$action$extra"

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            resp.body?.string().orEmpty()
        }
    }

    // ─── Canais ao vivo ────────────────────────────────────────────────────

    suspend fun fetchCategories(): List<Category> {
        val body = get(api("get_live_categories"))
        val arr = json.parseToJsonElement(body) as? JsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val obj = el.jsonObject
            val id = obj["category_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val name = obj["category_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            Category(id = id, name = name)
        }
    }

    suspend fun fetchChannels(): List<Channel> {
        val body = get(api("get_live_streams"))
        val arr = json.parseToJsonElement(body) as? JsonArray ?: return emptyList()
        return arr.mapIndexedNotNull { index, el ->
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
                streamUrl = "$base/live/${config.xtreamUser}/${config.xtreamPass}/$streamId.m3u8",
                categoryId = catId,
                epgChannelId = epgId
            )
        }
    }

    // ─── EPG (guia de programação) ─────────────────────────────────────────

    suspend fun fetchShortEpg(streamId: String, limit: Int = 12): List<EpgEntry> {
        val body = get(api("get_short_epg", "&stream_id=$streamId&limit=$limit"))
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: return emptyList()
        val listings = root["epg_listings"]?.jsonArray ?: return emptyList()

        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        return listings.mapNotNull { el ->
            val obj = el.jsonObject
            val startRaw = obj["start"]?.jsonPrimitive?.content
                ?: obj["start_timestamp"]?.jsonPrimitive?.content?.toLongOrNull()
                    ?.let { return@mapNotNull EpgEntry(
                        channelId = streamId,
                        title = decodeb64(obj["title"]?.jsonPrimitive?.content),
                        description = decodeb64(obj["description"]?.jsonPrimitive?.content),
                        start = it * 1000L,
                        end = (obj["stop_timestamp"]?.jsonPrimitive?.content?.toLongOrNull() ?: it) * 1000L
                    )}
                ?: return@mapNotNull null
            val endRaw = obj["end"]?.jsonPrimitive?.content ?: return@mapNotNull null

            val start = runCatching { fmt.parse(startRaw)?.time }.getOrNull() ?: return@mapNotNull null
            val end = runCatching { fmt.parse(endRaw)?.time }.getOrNull() ?: return@mapNotNull null

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

    suspend fun fetchVodCategories(): List<Category> {
        val body = get(api("get_vod_categories"))
        val arr = json.parseToJsonElement(body) as? JsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val obj = el.jsonObject
            val id = obj["category_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val name = obj["category_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            Category(id = id, name = name)
        }
    }

    suspend fun fetchVodStreams(): List<VodItem> {
        val body = get(api("get_vod_streams"))
        val arr = json.parseToJsonElement(body) as? JsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val obj = el.jsonObject
            val streamId = obj["stream_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.content ?: "Filme"
            val poster = obj["stream_icon"]?.jsonPrimitive?.content?.ifBlank { null }
            val catId = obj["category_id"]?.jsonPrimitive?.content ?: ""
            val rating = obj["rating"]?.jsonPrimitive?.content?.ifBlank { null }
            val plot = obj["plot"]?.jsonPrimitive?.content?.ifBlank { null }
            val year = obj["year"]?.jsonPrimitive?.content?.ifBlank { null }
            val dur = obj["duration_secs"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val ext = obj["container_extension"]?.jsonPrimitive?.content ?: "mp4"
            VodItem(
                id = streamId,
                name = name,
                posterUrl = poster,
                streamUrl = "$base/movie/${config.xtreamUser}/${config.xtreamPass}/$streamId.$ext",
                categoryId = catId,
                rating = rating,
                plot = plot,
                year = year,
                durationSecs = dur
            )
        }
    }

    // ─── Séries ────────────────────────────────────────────────────────────

    suspend fun fetchSeriesCategories(): List<Category> {
        val body = get(api("get_series_categories"))
        val arr = json.parseToJsonElement(body) as? JsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val obj = el.jsonObject
            val id = obj["category_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val name = obj["category_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            Category(id = id, name = name)
        }
    }

    suspend fun fetchSeries(): List<SeriesItem> {
        val body = get(api("get_series"))
        val arr = json.parseToJsonElement(body) as? JsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val obj = el.jsonObject
            val id = obj["series_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.content ?: "Série"
            val cover = obj["cover"]?.jsonPrimitive?.content?.ifBlank { null }
            val catId = obj["category_id"]?.jsonPrimitive?.content ?: ""
            val rating = obj["rating"]?.jsonPrimitive?.content?.ifBlank { null }
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

    suspend fun fetchSeriesInfo(seriesId: String): List<Season> {
        val body = get(api("get_series_info", "&series_id=$seriesId"))
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: return emptyList()
        val episodesMap = root["episodes"]?.jsonObject ?: return emptyList()

        return episodesMap.entries.mapNotNull { (seasonKey, seasonArr) ->
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
                    streamUrl = "$base/series/${config.xtreamUser}/${config.xtreamPass}/$epId.$ext",
                    durationSecs = dur,
                    plot = plot,
                    stillUrl = still
                )
            }.sortedBy { it.episodeNum }
            Season(seasonNum, eps)
        }.sortedBy { it.seasonNumber }
    }
}
