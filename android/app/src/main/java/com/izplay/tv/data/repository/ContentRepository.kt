package com.izplay.tv.data.repository

import com.izplay.tv.data.model.*
import com.izplay.tv.data.remote.M3uParser
import com.izplay.tv.data.remote.XtreamClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class Catalog(
    val categories: List<Category>,
    val channels: List<Channel>
)

data class VodCatalog(
    val categories: List<Category>,
    val items: List<VodItem>
)

data class SeriesCatalog(
    val categories: List<Category>,
    val items: List<SeriesItem>
)

class ContentRepository(
    private val http: OkHttpClient = OkHttpClient()
) {
    suspend fun loadCatalog(config: ProviderConfig): Catalog {
        require(config.isValid) { "Configuração de provedor inválida" }
        return when (config.mode) {
            ProviderConfig.Mode.M3U -> loadFromM3u(config.m3uUrl)
            ProviderConfig.Mode.XTREAM -> loadFromXtream(config)
        }
    }

    suspend fun loadEpg(config: ProviderConfig, streamId: String): List<EpgEntry> {
        if (config.mode != ProviderConfig.Mode.XTREAM) return emptyList()
        return runCatching { XtreamClient(config, http).fetchShortEpg(streamId) }
            .getOrDefault(emptyList())
    }

    suspend fun loadVod(config: ProviderConfig): VodCatalog {
        if (config.mode != ProviderConfig.Mode.XTREAM) return VodCatalog(emptyList(), emptyList())
        val client = XtreamClient(config, http)
        val cats = client.fetchVodCategories()
        val items = client.fetchVodStreams()
        val counts = items.groupingBy { it.categoryId }.eachCount()
        return VodCatalog(
            cats.map { it.copy(channelCount = counts[it.id] ?: 0) },
            items
        )
    }

    suspend fun loadSeries(config: ProviderConfig): SeriesCatalog {
        if (config.mode != ProviderConfig.Mode.XTREAM) return SeriesCatalog(emptyList(), emptyList())
        val client = XtreamClient(config, http)
        val cats = client.fetchSeriesCategories()
        val items = client.fetchSeries()
        val counts = items.groupingBy { it.categoryId }.eachCount()
        return SeriesCatalog(
            cats.map { it.copy(channelCount = counts[it.id] ?: 0) },
            items
        )
    }

    suspend fun loadSeriesDetail(config: ProviderConfig, seriesId: String): List<Season> {
        if (config.mode != ProviderConfig.Mode.XTREAM) return emptyList()
        return runCatching { XtreamClient(config, http).fetchSeriesInfo(seriesId) }
            .getOrDefault(emptyList())
    }

    private suspend fun loadFromM3u(url: String): Catalog = withContext(Dispatchers.IO) {
        val body = http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) error("Falha ao baixar M3U: HTTP ${resp.code}")
            resp.body?.string().orEmpty()
        }
        val result = M3uParser.parse(body)
        Catalog(result.categories, result.channels)
    }

    private suspend fun loadFromXtream(config: ProviderConfig): Catalog {
        val client = XtreamClient(config, http)
        val cats = client.fetchCategories()
        val channels = client.fetchChannels()
        val counts = channels.groupingBy { it.categoryId }.eachCount()
        val catsWithCount = cats.map { it.copy(channelCount = counts[it.id] ?: 0) }
        return Catalog(catsWithCount, channels)
    }
}
