package com.izplay.tv.data.repository

import com.izplay.tv.data.model.*
import com.izplay.tv.data.remote.AppHttpClient
import com.izplay.tv.data.remote.M3uParser
import com.izplay.tv.data.remote.XtreamClient
import com.izplay.tv.data.remote.withPlayerHeaders
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
    private val http: OkHttpClient = AppHttpClient.create()
) {
    private fun cleanCategory(category: Category): Category = category.copy(
        name = category.name
            .replace(Regex("[\\p{So}\\p{Sk}]"), "")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-', '|', '/')
    )
    suspend fun loadCatalog(config: ProviderConfig, apiBase: String? = null): Catalog {
        require(config.isValid) { "Configuração de provedor inválida" }
        return when (config.mode) {
            ProviderConfig.Mode.M3U -> loadFromM3u(config.m3uUrl)
            ProviderConfig.Mode.XTREAM -> loadFromXtream(config, apiBase)
        }
    }

    suspend fun loadEpg(config: ProviderConfig, streamId: String, apiBase: String? = null): List<EpgEntry> {
        if (config.mode != ProviderConfig.Mode.XTREAM) return emptyList()
        return runCatching { XtreamClient(config, http, apiBase).fetchShortEpg(streamId) }
            .getOrDefault(emptyList())
    }

    suspend fun loadVodDetail(config: ProviderConfig, vod: VodItem, apiBase: String? = null): VodItem {
        if (config.mode != ProviderConfig.Mode.XTREAM) return vod
        return runCatching { XtreamClient(config, http, apiBase).fetchVodInfo(vod) }
            .getOrDefault(vod)
    }

    suspend fun loadVod(config: ProviderConfig, apiBase: String? = null): VodCatalog {
        if (config.mode != ProviderConfig.Mode.XTREAM) return VodCatalog(emptyList(), emptyList())
        val client = XtreamClient(config, http, apiBase)
        val cats = client.fetchVodCategories()
        val items = client.fetchVodStreams()
        check(items.isNotEmpty()) { "Catálogo de filmes vazio nesta rota" }
        val counts = items.groupingBy { it.categoryId }.eachCount()
        return VodCatalog(
            cats.map { cleanCategory(it.copy(channelCount = counts[it.id] ?: 0)) },
            items
        )
    }

    suspend fun loadSeries(config: ProviderConfig, apiBase: String? = null): SeriesCatalog {
        if (config.mode != ProviderConfig.Mode.XTREAM) return SeriesCatalog(emptyList(), emptyList())
        val client = XtreamClient(config, http, apiBase)
        val cats = client.fetchSeriesCategories()
        val items = client.fetchSeries()
        check(items.isNotEmpty()) { "Catálogo de séries vazio nesta rota" }
        val counts = items.groupingBy { it.categoryId }.eachCount()
        return SeriesCatalog(
            cats.map { cleanCategory(it.copy(channelCount = counts[it.id] ?: 0)) },
            items
        )
    }

    suspend fun loadSeriesDetail(config: ProviderConfig, series: SeriesItem, apiBase: String? = null): SeriesDetail {
        if (config.mode != ProviderConfig.Mode.XTREAM) return SeriesDetail(series, emptyList())
        return runCatching { XtreamClient(config, http, apiBase).fetchSeriesInfo(series) }
            .getOrDefault(SeriesDetail(series, emptyList()))
    }

    private suspend fun loadFromM3u(url: String): Catalog = withContext(Dispatchers.IO) {
        val body = http.newCall(Request.Builder().url(url).withPlayerHeaders().build()).execute().use { resp ->
            if (!resp.isSuccessful) error("Falha ao baixar M3U: HTTP ${resp.code}")
            resp.body?.string().orEmpty()
        }
        val result = M3uParser.parse(body)
        Catalog(result.categories.map(::cleanCategory), result.channels)
    }

    private suspend fun loadFromXtream(config: ProviderConfig, apiBase: String?): Catalog {
        val client = XtreamClient(config, http, apiBase)
        val cats = client.fetchCategories()
        val channels = client.fetchChannels()
        check(channels.isNotEmpty()) { "Catálogo de canais vazio nesta rota" }
        val counts = channels.groupingBy { it.categoryId }.eachCount()
        val catsWithCount = cats.map { cleanCategory(it.copy(channelCount = counts[it.id] ?: 0)) }
        return Catalog(catsWithCount, channels)
    }
}
