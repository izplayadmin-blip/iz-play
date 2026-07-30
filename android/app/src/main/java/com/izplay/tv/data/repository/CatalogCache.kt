package com.izplay.tv.data.repository

import android.content.Context
import com.izplay.tv.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File

@Serializable
data class HomeSnapshot(
    val key: String,
    val liveCategories: List<Category>,
    val channels: List<Channel>,
    val vodCategories: List<Category>,
    val vod: List<VodItem>,
    val seriesCategories: List<Category>,
    val series: List<SeriesItem>
)

/**
 * Cache em disco do catálogo (canais, filmes e séries) para boot instantâneo em
 * TV boxes fracas: o app mostra a última lista salva na hora e atualiza da rede
 * em segundo plano. A chave é derivada da conta ativa — trocar de conta invalida.
 */
class CatalogCache(context: Context) {

    @Serializable
    private data class CachedLive(val key: String, val categories: List<Category>, val channels: List<Channel>)

    @Serializable
    private data class CachedVod(val key: String, val categories: List<Category>, val items: List<VodItem>)

    @Serializable
    private data class CachedSeries(val key: String, val categories: List<Category>, val items: List<SeriesItem>)

    private val dir = File(context.filesDir, "catalog-cache").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }

    fun keyFor(config: ProviderConfig): String =
        listOf(config.mode.name, config.xtreamHost, config.xtreamUser, config.m3uUrl)
            .joinToString("|").hashCode().toString()

    suspend fun loadLive(key: String): Catalog? =
        read<CachedLive>(FILE_LIVE)?.takeIf { it.key == key }?.let { Catalog(it.categories, it.channels) }

    suspend fun saveLive(key: String, catalog: Catalog) =
        write(FILE_LIVE, CachedLive(key, catalog.categories, catalog.channels))

    suspend fun loadVod(key: String): VodCatalog? =
        read<CachedVod>(FILE_VOD)?.takeIf { it.key == key }?.let { VodCatalog(it.categories, it.items) }

    suspend fun saveVod(key: String, catalog: VodCatalog) =
        write(FILE_VOD, CachedVod(key, catalog.categories, catalog.items))

    suspend fun loadSeries(key: String): SeriesCatalog? =
        read<CachedSeries>(FILE_SERIES)?.takeIf { it.key == key }?.let { SeriesCatalog(it.categories, it.items) }

    suspend fun saveSeries(key: String, catalog: SeriesCatalog) =
        write(FILE_SERIES, CachedSeries(key, catalog.categories, catalog.items))

    suspend fun loadHome(key: String): HomeSnapshot? =
        read<HomeSnapshot>(FILE_HOME)?.takeIf { it.key == key }

    suspend fun saveHome(
        key: String,
        live: Catalog,
        vod: VodCatalog,
        series: SeriesCatalog
    ) = write(
        FILE_HOME,
        HomeSnapshot(
            key = key,
            liveCategories = live.categories,
            channels = live.channels.take(48),
            vodCategories = vod.categories,
            vod = vod.items.sortedByDescending { it.addedAt }.take(160),
            seriesCategories = series.categories,
            series = series.items.take(160)
        )
    )

    suspend fun clear() = withContext(Dispatchers.IO) {
        runCatching { dir.listFiles()?.forEach { it.delete() } }
        Unit
    }

    /** Idade do cache em ms (Long.MAX_VALUE se não existir) — usada para decidir refresh. */
    fun liveAgeMs(): Long = ageMs(FILE_LIVE)
    fun vodAgeMs(): Long = ageMs(FILE_VOD)
    fun seriesAgeMs(): Long = ageMs(FILE_SERIES)

    /** Config central do painel: cache local para abrir o app mesmo sem o painel no ar. */
    suspend fun loadClientConfig(): ClientConfig? = read<ClientConfig>(FILE_CONFIG)
    suspend fun saveClientConfig(config: ClientConfig) = write(FILE_CONFIG, config)

    private fun ageMs(name: String): Long {
        val file = File(dir, name)
        return if (file.exists()) System.currentTimeMillis() - file.lastModified() else Long.MAX_VALUE
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend inline fun <reified T> read(name: String): T? = withContext(Dispatchers.IO) {
        val file = File(dir, name)
        if (!file.exists()) return@withContext null
        runCatching { file.inputStream().buffered().use { json.decodeFromStream<T>(it) } }.getOrNull()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend inline fun <reified T> write(name: String, value: T) = withContext(Dispatchers.IO) {
        // grava em arquivo temporário e renomeia para nunca deixar cache corrompido
        val tmp = File(dir, "$name.tmp")
        runCatching {
            tmp.outputStream().buffered().use { json.encodeToStream(value, it) }
            if (!tmp.renameTo(File(dir, name))) tmp.delete()
        }.onFailure { tmp.delete() }
        Unit
    }

    private companion object {
        const val FILE_LIVE = "live.json"
        const val FILE_VOD = "vod.json"
        const val FILE_SERIES = "series.json"
        const val FILE_HOME = "home.json"
        const val FILE_CONFIG = "client-config.json"
    }
}
