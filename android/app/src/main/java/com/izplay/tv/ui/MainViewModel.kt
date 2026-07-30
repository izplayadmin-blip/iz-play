package com.izplay.tv.ui

import android.app.Application
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.izplay.tv.BuildConfig
import com.izplay.tv.data.model.*
import com.izplay.tv.data.remote.RemoteConfigClient
import com.izplay.tv.data.repository.Catalog
import com.izplay.tv.data.repository.CatalogCache
import com.izplay.tv.data.repository.ContentRepository
import com.izplay.tv.data.repository.HomeSnapshot
import com.izplay.tv.data.repository.PlaybackPreferences
import com.izplay.tv.data.repository.SeriesCatalog
import com.izplay.tv.data.repository.SettingsStore
import com.izplay.tv.data.repository.VodCatalog
import com.izplay.tv.player.SwarmCloudManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import com.izplay.tv.data.remote.AppDns
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Mídia tocando em tela cheia (overlay global acima de tudo, inclusive da sidebar).
 *  `fallbackUrl` = mesma mídia roteada pelo gateway central (usada se a direta falhar). */
data class PlayingMedia(
    val url: String,
    val title: String,
    val subtitle: String? = null,
    val fallbackUrl: String? = null,
    val mediaType: String = "live",
    val contentId: String = "",
    val initialPositionMs: Long = 0L,
)

/** Status da infraestrutura central (null = ainda não verificado). */
data class InfraStatus(
    val checking: Boolean = false,
    val gatewayOk: Boolean? = null,
    val videoGatewayOk: Boolean? = null,
    val proxyOk: Boolean? = null,
    val superPeerOk: Boolean? = null,
    val vpnOnline: Map<String, Boolean> = emptyMap()
)

enum class ConnectionQuality { UNKNOWN, GOOD, FAIR, POOR }

data class ConnectionMetrics(
    val testing: Boolean = false,
    val quality: ConnectionQuality = ConnectionQuality.UNKNOWN,
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val pingMs: Double = 0.0,
    val jitterMs: Double = 0.0,
    val lossPct: Double = 0.0,
    val lastTestAt: Long = 0L
)

data class UiState(
    val loading: Boolean = false,
    val startupLoading: Boolean = false,
    val startupFailed: Boolean = false,
    val startupMessage: String = "Preparando o IZ Play...",
    val startupProgress: Float = 0f,
    val error: String? = null,
    val configured: Boolean = false,
    val config: ProviderConfig? = null,
    val profiles: List<AppProfile> = emptyList(),
    val activeProfileId: String? = null,
    val profileGateVisible: Boolean = false,

    // ── Config central do Painel Admin ──────────────
    val clientConfig: ClientConfig? = null,
    val infra: InfraStatus = InfraStatus(),
    val connection: ConnectionMetrics = ConnectionMetrics(),
    val playbackPreferences: PlaybackPreferences = PlaybackPreferences(),

    // ── Reprodução em tela cheia (global) ───────────
    val playing: PlayingMedia? = null,
    val fullscreenTransition: Boolean = false,

    // ── Canais ao vivo ──────────────────────────────
    val categories: List<Category> = emptyList(),
    val allChannels: List<Channel> = emptyList(),
    val selectedCategoryId: String? = null,
    val selectedChannel: Channel? = null,
    val liveCatalogComplete: Boolean = false,
    val searchQuery: String = "",
    val favorites: Set<String> = emptySet(),
    val vodFavorites: Set<String> = emptySet(),
    val seriesFavorites: Set<String> = emptySet(),
    val recentChannelIds: List<String> = emptyList(),
    val epg: List<EpgEntry> = emptyList(),
    val epgLoading: Boolean = false,

    // ── VOD (filmes) ─────────────────────────────────
    val vodLoading: Boolean = false,
    val vodCategories: List<Category> = emptyList(),
    val allVod: List<VodItem> = emptyList(),
    val selectedVodCategoryId: String? = null,
    val vodSearchQuery: String = "",
    val selectedVod: VodItem? = null,
    val vodDetailLoading: Boolean = false,
    val heroVod: VodItem? = null,
    val vodCatalogComplete: Boolean = false,
    val recentVodIds: List<String> = emptyList(),

    // ── Séries ────────────────────────────────────────
    val seriesLoading: Boolean = false,
    val seriesCategories: List<Category> = emptyList(),
    val allSeries: List<SeriesItem> = emptyList(),
    val seriesCatalogComplete: Boolean = false,
    val selectedSeriesCategoryId: String? = null,
    val seriesSearchQuery: String = "",
    val selectedSeries: SeriesItem? = null,
    val recentSeriesIds: List<String> = emptyList(),
    val seriesSeasons: List<Season> = emptyList(),
    val seriesDetailLoading: Boolean = false
) {
    val activeProfile: AppProfile?
        get() = profiles.firstOrNull { it.id == activeProfileId }

    val visibleChannels: List<Channel>
        get() {
            val base = when (selectedCategoryId) {
                FAVORITES_ID -> allChannels.filter { it.id in favorites }
                null -> allChannels
                else -> allChannels.filter { it.categoryId == selectedCategoryId }
            }
            return if (searchQuery.isBlank()) base
            else base.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

    val visibleVod: List<VodItem>
        get() {
            val base = if (selectedVodCategoryId == null) allVod
            else allVod.filter { it.categoryId == selectedVodCategoryId }
            return if (vodSearchQuery.isBlank()) base
            else base.filter { it.name.contains(vodSearchQuery, ignoreCase = true) }
        }

    val visibleSeries: List<SeriesItem>
        get() {
            val base = if (selectedSeriesCategoryId == null) allSeries
            else allSeries.filter { it.categoryId == selectedSeriesCategoryId }
            return if (seriesSearchQuery.isBlank()) base
            else base.filter { it.name.contains(seriesSearchQuery, ignoreCase = true) }
        }

    /** Fallback de reprodução via gateway central para uma URL direta do provedor. */
    fun streamFallback(directUrl: String?): String? =
        directUrl?.let { clientConfig?.videoProxyUrl(it) }

    companion object {
        const val FAVORITES_ID = "__favorites__"
    }
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ContentRepository()
    private val store = SettingsStore(app)
    private val cache = CatalogCache(app)

    private fun cleanCategories(items: List<Category>): List<Category> = items.map { category ->
        category.copy(name = category.name
            .replace(Regex("[\\p{So}\\p{Sk}]"), "")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-', '|', '/'))
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var epgJob: Job? = null

    // Host(s) do provedor vindos do Painel Admin (o admin troca a DNS lá). Fallback local.
    private var serverHosts: List<String> = emptyList()

    // Base de catálogo usada no último carregamento bem-sucedido (gateway ou direta);
    // o EPG segue a mesma rota para não misturar caminhos.
    private var activeApiBase: String? = null

    // Cliente curto para probes de host e checagem de infraestrutura (nunca 45s).
    private val probeClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(6, TimeUnit.SECONDS)
        .build()

    init {
        viewModelScope.launch {
            // 1) Config central: cache local primeiro (abre mesmo sem painel),
            //    depois rede — sempre tenta atualizar na abertura do app.
            cache.loadClientConfig()?.let { applyClientConfig(it) }
            val favs = store.favoritesFlow.first()
            val vodFavs = store.vodFavoritesFlow.first()
            val seriesFavs = store.seriesFavoritesFlow.first()
            val playbackPreferences = store.playbackPreferencesFlow.first()
            AppDns.provider = playbackPreferences.dnsProvider
            _state.value = _state.value.copy(
                favorites = favs,
                vodFavorites = vodFavs,
                seriesFavorites = seriesFavs,
                playbackPreferences = playbackPreferences
            )
            val config = store.configFlow.first()
            if (config != null && config.isValid) {
                _state.value = _state.value.copy(
                    configured = true,
                    config = config,
                    startupLoading = true,
                    startupMessage = "Verificando as melhores rotas..."
                )
                viewModelScope.launch { refreshClientConfigForStartup() }
                bootFromCacheOrNetwork(config)
            } else {
                refreshClientConfigForStartup()
            }
        }
        viewModelScope.launch {
            delay(12_000)
            while (true) {
                sendTelemetry("heartbeat", _state.value.selectedChannel)
                delay(60_000L)
            }
        }
    }

    private fun applyClientConfig(cfg: ClientConfig) {
        if (cfg.providerHosts.isNotEmpty()) serverHosts = cfg.providerHosts
        _state.value = _state.value.copy(clientConfig = cfg)
    }

    /** Config efetiva: painel → cache → fallback seguro embutido (só URLs públicas). */
    private fun effectiveConfig(): ClientConfig =
        _state.value.clientConfig ?: RemoteConfigClient.FALLBACK_CONFIG

    private suspend fun refreshClientConfigForStartup() {
        withTimeoutOrNull(11_000) { RemoteConfigClient().fetchConfig() }?.let {
            cache.saveClientConfig(it)
            applyClientConfig(it)
        }
    }

    /** Prepara canais, filmes, séries e as primeiras artes antes de liberar a Home. */
    private suspend fun bootFromCacheOrNetwork(config: ProviderConfig) {
        updateStartup("Verificando seus conteúdos...", 0.08f)
        val cacheKey = cache.keyFor(config)
        val home = cache.loadHome(cacheKey)
        if (home != null && home.channels.isNotEmpty() && home.vod.isNotEmpty() && home.series.isNotEmpty()) {
            applyHomeSnapshot(config, home)
            loadAccountProfiles(config)
            updateStartup("Tudo pronto!", 1f)
            delay(80)
            _state.value = _state.value.copy(startupLoading = false, loading = false)
            return
        }

        val cached = cache.loadLive(cacheKey)
        if (cached != null && cached.channels.isNotEmpty()) {
            updateStartup("Carregando canais...", 0.20f)
            applyCatalog(config, cached, preserveSelection = false)
        } else {
            updateStartup("Atualizando canais...", 0.20f)
            loadLiveForStartup(config)
        }

        updateStartup("Preparando filmes...", 0.48f)
        loadVodInternal()
        updateStartup("Organizando séries...", 0.72f)
        loadSeriesInternal()

        if (!startupCatalogsComplete()) {
            updateStartup("Tentando uma rota alternativa...", 0.82f)
            refreshClientConfigForStartup()
            val activeConfig = _state.value.config ?: config
            if (_state.value.allChannels.isEmpty()) loadLiveForStartup(activeConfig)
            if (_state.value.allVod.isEmpty()) loadVodInternal()
            if (_state.value.allSeries.isEmpty()) loadSeriesInternal()
        }

        if (!startupCatalogsComplete()) {
            _state.value = _state.value.copy(
                startupLoading = true,
                startupFailed = true,
                startupMessage = "Não foi possível carregar todo o catálogo. Verifique a conexão e tente novamente.",
                startupProgress = 0f,
                loading = false
            )
            return
        }

        updateStartup("Montando sua Home...", 0.90f)
        loadAccountProfiles(_state.value.config ?: config)

        _state.value.selectedChannel?.let { fetchEpg(_state.value.config ?: config, it) }
        updateStartup("Tudo pronto!", 1f)
        cache.saveHome(
            cacheKey,
            Catalog(_state.value.categories, _state.value.allChannels),
            VodCatalog(_state.value.vodCategories, _state.value.allVod),
            SeriesCatalog(_state.value.seriesCategories, _state.value.allSeries)
        )
        delay(220)
        _state.value = _state.value.copy(startupLoading = false, loading = false)
        viewModelScope.launch { warmHomeImages() }
        viewModelScope.launch {
            // Dá prioridade total à primeira renderização e ao foco do controle.
            // Os catálogos grandes são atualizados em sequência depois da entrada.
            delay(5_000)
            val activeConfig = _state.value.config ?: config
            if (cache.vodAgeMs() > VOD_TTL_MS) refreshVodSilently(activeConfig)
            if (cache.seriesAgeMs() > VOD_TTL_MS) refreshSeriesSilently(activeConfig)
        }

        if (cached != null && cache.liveAgeMs() > LIVE_TTL_MS) {
            refreshCatalogSilently(_state.value.config ?: config)
        }
    }

    fun saveAndLoad(config: ProviderConfig) {
        viewModelScope.launch {
            store.saveConfig(config)
            _state.value = _state.value.copy(configured = true, config = config, startupLoading = true)
            viewModelScope.launch { refreshClientConfigForStartup() }
            bootFromCacheOrNetwork(config)
        }
    }

    fun logout() {
        viewModelScope.launch {
            val cfg = _state.value.clientConfig
            store.clearConfig()
            cache.clear()
            _state.value = UiState(clientConfig = cfg)
            cfg?.let { cache.saveClientConfig(it) }
        }
    }

    /** Login Xtream pelo painel: o host vem da config remota (admin gerencia); o usuário
     *  só digita usuário e senha. */
    fun loginXtream(user: String, pass: String) {
        val host = serverHosts.firstOrNull()
        if (host == null) {
            _state.value = _state.value.copy(
                error = "Não foi possível consultar a configuração do Painel Admin."
            )
            return
        }
        saveAndLoad(
            ProviderConfig(
                ProviderConfig.Mode.XTREAM,
                "",
                host,
                user.trim(),
                pass,
                ""
            )
        )
    }

    fun loginM3u(url: String) {
        saveAndLoad(ProviderConfig(ProviderConfig.Mode.M3U, url.trim(), "", "", "", ""))
    }

    // ── Reprodução em tela cheia ───────────────────────────────────────────

    fun playVod(vod: VodItem) {
        rememberOnDemand(vodId = vod.id)
        viewModelScope.launch {
            val resume = loadResumePosition("vod", vod.id)
            _state.value = _state.value.copy(
                playing = PlayingMedia(
                    url = vod.streamUrl,
                    title = vod.name,
                    subtitle = "FILME",
                    fallbackUrl = _state.value.streamFallback(vod.streamUrl),
                    mediaType = "vod",
                    contentId = vod.id,
                    initialPositionMs = resume,
                )
            )
        }
    }

    fun playEpisode(series: SeriesItem, episode: Episode) {
        rememberOnDemand(seriesId = series.id)
        viewModelScope.launch {
            val resume = loadResumePosition("episode", episode.id)
            _state.value = _state.value.copy(
                playing = PlayingMedia(
                    url = episode.streamUrl,
                    title = episode.title,
                    subtitle = series.name,
                    fallbackUrl = _state.value.streamFallback(episode.streamUrl),
                    mediaType = "episode",
                    contentId = episode.id,
                    initialPositionMs = resume,
                )
            )
        }
    }

    private fun rememberOnDemand(vodId: String? = null, seriesId: String? = null) {
        val current = _state.value
        val vodIds = vodId?.let { (listOf(it) + current.recentVodIds).distinct().take(12) }
            ?: current.recentVodIds
        val seriesIds = seriesId?.let { (listOf(it) + current.recentSeriesIds).distinct().take(12) }
            ?: current.recentSeriesIds
        _state.value = current.copy(recentVodIds = vodIds, recentSeriesIds = seriesIds)
        viewModelScope.launch {
            val config = _state.value.config ?: return@launch
            val profileId = _state.value.activeProfileId ?: return@launch
            val account = profileAccountKey(config)
            store.saveProfileList(account, profileId, "recent_vod", vodIds)
            store.saveProfileList(account, profileId, "recent_series", seriesIds)
        }
    }

    fun playLive(url: String, title: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(fullscreenTransition = true)
            delay(450)
            _state.value = _state.value.copy(
                fullscreenTransition = false,
                playing = PlayingMedia(
                    url = url,
                    title = title,
                    subtitle = "AO VIVO",
                    fallbackUrl = _state.value.streamFallback(url),
                )
            )
        }
    }

    private suspend fun loadResumePosition(mediaType: String, contentId: String): Long {
        val config = _state.value.config ?: return 0L
        val profileId = _state.value.activeProfileId ?: return 0L
        val progress = store.loadPlaybackProgress(
            profileAccountKey(config),
            profileId,
            mediaType,
            contentId,
        ) ?: return 0L
        // Conteúdo praticamente concluído recomeça do início.
        return progress.positionMs.takeIf {
            it >= 10_000L && it < progress.durationMs - 30_000L
        } ?: 0L
    }

    private var lastPlaybackProgressWriteAt = 0L

    fun recordPlaybackProgress(positionMs: Long, durationMs: Long) {
        val media = _state.value.playing ?: return
        if (media.mediaType == "live" || media.contentId.isBlank() || durationMs <= 0L) return
        val now = System.currentTimeMillis()
        if (now - lastPlaybackProgressWriteAt < 5_000L) return
        lastPlaybackProgressWriteAt = now
        viewModelScope.launch {
            val config = _state.value.config ?: return@launch
            val profileId = _state.value.activeProfileId ?: return@launch
            store.savePlaybackProgress(
                profileAccountKey(config),
                profileId,
                media.mediaType,
                media.contentId,
                positionMs,
                durationMs,
            )
        }
    }

    fun stopPlayback() {
        _state.value = _state.value.copy(playing = null)
    }

    // ── Catálogo de canais ─────────────────────────────────────────────────

    private fun applyCatalog(activeConfig: ProviderConfig, catalog: Catalog, preserveSelection: Boolean) {
        val cur = _state.value
        val selCat = cur.selectedCategoryId
            ?.takeIf { preserveSelection && (it == UiState.FAVORITES_ID || catalog.categories.any { c -> c.id == it }) }
            ?: catalog.categories.firstOrNull()?.id
        val selCh = cur.selectedChannel
            ?.takeIf { sel -> preserveSelection && catalog.channels.any { it.id == sel.id } }
            ?: catalog.channels.firstOrNull()
        _state.value = cur.copy(
            loading = false,
            error = null,
            config = activeConfig,
            categories = cleanCategories(catalog.categories),
            allChannels = catalog.channels,
            liveCatalogComplete = true,
            selectedCategoryId = selCat,
            selectedChannel = selCh
        )
    }

    private fun loadCatalog(config: ProviderConfig) {
        viewModelScope.launch {
            bootFromCacheOrNetwork(config)
        }
    }

    private fun applyHomeSnapshot(config: ProviderConfig, home: HomeSnapshot) {
        _state.value = _state.value.copy(
            config = config,
            categories = cleanCategories(home.liveCategories),
            allChannels = home.channels,
            liveCatalogComplete = false,
            selectedCategoryId = home.liveCategories.firstOrNull()?.id,
            selectedChannel = home.channels.firstOrNull(),
            vodCategories = cleanCategories(home.vodCategories),
            allVod = home.vod,
            vodCatalogComplete = false,
            selectedVodCategoryId = home.vodCategories.firstOrNull()?.id,
            seriesCategories = cleanCategories(home.seriesCategories),
            allSeries = home.series,
            seriesCatalogComplete = false,
            selectedSeriesCategoryId = home.seriesCategories.firstOrNull()?.id
        )
    }

    private suspend fun loadFullCatalogCaches(config: ProviderConfig, key: String) {
        cache.loadLive(key)?.takeIf { it.channels.isNotEmpty() }
            ?.let { applyCatalog(config, it, preserveSelection = true) }
        cache.loadVod(key)?.takeIf { it.items.isNotEmpty() }?.let {
            _state.value = _state.value.copy(
                vodCategories = cleanCategories(it.categories),
                allVod = it.items,
                vodCatalogComplete = true
            )
        }
        cache.loadSeries(key)?.takeIf { it.items.isNotEmpty() }?.let {
            _state.value = _state.value.copy(
                seriesCategories = cleanCategories(it.categories),
                allSeries = it.items,
                seriesCatalogComplete = true
            )
        }
    }

    private fun updateStartup(message: String, progress: Float) {
        _state.value = _state.value.copy(
            startupLoading = true,
            startupFailed = false,
            startupMessage = message,
            startupProgress = progress.coerceIn(0f, 1f),
            error = null
        )
    }

    private fun startupCatalogsComplete(): Boolean =
        _state.value.allChannels.isNotEmpty() &&
            _state.value.allVod.isNotEmpty() &&
            _state.value.allSeries.isNotEmpty()

    fun retryStartup() {
        val config = _state.value.config ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(startupFailed = false, error = null)
            refreshClientConfigForStartup()
            bootFromCacheOrNetwork(config)
        }
    }

    private suspend fun loadLiveForStartup(config: ProviderConfig) {
        _state.value = _state.value.copy(loading = true)
        runCatching { tryConfigs(config) { cfg, api -> repo.loadCatalog(cfg, api) } }
            .onSuccess { attempt ->
                if (attempt.config != config) store.saveConfig(attempt.config)
                cache.saveLive(cache.keyFor(attempt.config), attempt.value)
                applyCatalog(attempt.config, attempt.value, preserveSelection = false)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Não foi possível atualizar os canais"
                )
            }
    }

    private suspend fun warmHomeImages() {
        val app = getApplication<Application>()
        val snapshot = _state.value
        val urls = buildList {
            snapshot.allVod.firstOrNull()?.posterUrl?.let(::add)
            snapshot.allVod.take(2).mapNotNullTo(this) { it.posterUrl }
            snapshot.allSeries.firstOrNull()?.coverUrl?.let(::add)
        }.filter { it.isNotBlank() }.distinct()

        withTimeoutOrNull(2_000) {
            urls.map { url ->
                async(Dispatchers.IO) {
                    app.imageLoader.execute(
                        ImageRequest.Builder(app)
                            .data(url)
                            .size(300, 450)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .networkCachePolicy(CachePolicy.ENABLED)
                            .build()
                    )
                }
            }.awaitAll()
        }
    }

    private suspend fun loadAccountProfiles(config: ProviderConfig) {
        val account = profileAccountKey(config)
        val profiles = store.loadProfiles(account)
        val savedActive = store.loadActiveProfileId(account)
        val active = savedActive?.takeIf { id -> profiles.any { it.id == id } }
            ?: profiles.firstOrNull()?.id
        _state.value = _state.value.copy(
            profiles = profiles,
            activeProfileId = active,
            profileGateVisible = profiles.isEmpty() ||
                profiles.firstOrNull { it.id == active }?.genres.orEmpty().size < 3
        )
        active?.let { applyProfileData(account, it) }
    }

    private fun profileAccountKey(config: ProviderConfig): String =
        if (config.mode == ProviderConfig.Mode.XTREAM) config.xtreamUser
        else config.m3uUrl

    fun openProfilePicker() {
        _state.value = _state.value.copy(profileGateVisible = true)
    }

    fun closeProfilePicker() {
        if (_state.value.profiles.isNotEmpty()) {
            _state.value = _state.value.copy(profileGateVisible = false)
        }
    }

    fun createProfile(name: String, avatar: Int, genres: List<String>) {
        val cleanName = name.trim().take(24)
        if (cleanName.isBlank() || genres.size < 3 || _state.value.profiles.size >= 6) return
        viewModelScope.launch {
            val config = _state.value.config ?: return@launch
            val profile = AppProfile(
                id = "profile-${System.currentTimeMillis()}",
                name = cleanName,
                avatar = avatar.coerceIn(0, 11),
                genres = genres.distinct().take(6)
            )
            val profiles = (_state.value.profiles + profile).take(6)
            val account = profileAccountKey(config)
            store.saveProfiles(account, profiles)
            store.saveActiveProfileId(account, profile.id)
            if (_state.value.profiles.isEmpty()) {
                store.saveProfileSet(account, profile.id, "channel_favorites", _state.value.favorites)
                store.saveProfileSet(account, profile.id, "vod_favorites", _state.value.vodFavorites)
                store.saveProfileSet(account, profile.id, "series_favorites", _state.value.seriesFavorites)
            }
            _state.value = _state.value.copy(
                profiles = profiles,
                activeProfileId = profile.id,
                profileGateVisible = false
            )
        }
    }

    fun selectProfile(profile: AppProfile) {
        viewModelScope.launch {
            val config = _state.value.config ?: return@launch
            val account = profileAccountKey(config)
            store.saveActiveProfileId(account, profile.id)
            applyProfileData(account, profile.id)
            _state.value = _state.value.copy(
                activeProfileId = profile.id,
                profileGateVisible = profile.genres.size < 3
            )
        }
    }

    fun completeProfile(profile: AppProfile, name: String, avatar: Int, genres: List<String>) {
        val cleanName = name.trim().take(24)
        if (cleanName.isBlank() || genres.size < 3) return
        viewModelScope.launch {
            val config = _state.value.config ?: return@launch
            val updated = profile.copy(
                name = cleanName,
                avatar = avatar.coerceIn(0, 11),
                genres = genres.distinct().take(6)
            )
            val profiles = _state.value.profiles.map { if (it.id == profile.id) updated else it }
            store.saveProfiles(profileAccountKey(config), profiles)
            _state.value = _state.value.copy(profiles = profiles, profileGateVisible = false)
        }
    }

    private suspend fun applyProfileData(account: String, profileId: String) {
        _state.value = _state.value.copy(
            favorites = store.loadProfileSet(account, profileId, "channel_favorites"),
            vodFavorites = store.loadProfileSet(account, profileId, "vod_favorites"),
            seriesFavorites = store.loadProfileSet(account, profileId, "series_favorites"),
            recentVodIds = store.loadProfileList(account, profileId, "recent_vod"),
            recentSeriesIds = store.loadProfileList(account, profileId, "recent_series"),
            recentChannelIds = emptyList()
        )
    }

    /** Atualização em segundo plano, sem tela de loading e sem perder a seleção atual. */
    private fun refreshCatalogSilently(config: ProviderConfig) {
        viewModelScope.launch {
            runCatching { tryConfigs(config) { cfg, api -> repo.loadCatalog(cfg, api) } }
                .onSuccess { attempt ->
                    if (attempt.config != config) store.saveConfig(attempt.config)
                    cache.saveLive(cache.keyFor(attempt.config), attempt.value)
                    applyCatalog(attempt.config, attempt.value, preserveSelection = true)
                }
            // falha silenciosa: o usuário continua com o cache
        }
    }

    private suspend fun hostReachable(url: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            probeClient.newCall(Request.Builder().url(url).head().build()).execute().use {
                it.isSuccessful
            }
        }.getOrElse {
            runCatching {
                probeClient.newCall(Request.Builder().url(url).build()).execute().use {
                    it.isSuccessful
                }
            }.getOrDefault(false)
        }
    }

    private data class Attempt<T>(val config: ProviderConfig, val apiBase: String?, val value: T)

    /**
     * Cascata de fontes de catálogo, na ordem:
     * 1. Gateway central (config do painel) falando Xtream — evita a DNS direta
     *    bloqueada em TV box e usa o cache serve-stale da VPS;
     * 2. Host direto configurado; 3. demais hosts do painel/fallback.
     * Streams SEMPRE são construídos no host direto (regra da ARQUITETURA.md).
     */
    private suspend fun <T> tryConfigs(
        config: ProviderConfig,
        block: suspend (ProviderConfig, String?) -> T
    ): Attempt<T> {
        if (config.mode != ProviderConfig.Mode.XTREAM) {
            return Attempt(config, null, block(config, null))
        }

        val directConfigs = (listOf(config.xtreamHost) + serverHosts + RemoteConfigClient.FALLBACK_HOSTS)
            .filter { it.isNotBlank() }
            .distinct()
            .map { config.copy(xtreamHost = it.trimEnd('/')) }

        val gatewayBase = effectiveConfig().catalogGatewayBase
        val candidates = buildList {
            directConfigs.forEach { add(null to it) }
            if (!gatewayBase.isNullOrBlank()) add(gatewayBase to directConfigs.first())
        }

        fun probeUrl(candidate: Pair<String?, ProviderConfig>): String =
            candidate.first?.let { "${it.trimEnd('/')}/health" } ?: candidate.second.xtreamHost

        // Probe rápido (6s) EM PARALELO joga fontes mortas pro fim da fila — nada é
        // descartado, mas com 6+ DNS o app não fica preso somando os timeouts em série.
        val ordered = if (candidates.size <= 1) candidates else coroutineScope {
            val probed = candidates
                .map { cand -> cand to async { hostReachable(probeUrl(cand)) } }
                .map { (cand, deferred) -> cand to deferred.await() }
            probed.filter { it.second }.map { it.first } + probed.filterNot { it.second }.map { it.first }
        }

        var lastError: Throwable? = null
        for ((api, candidate) in ordered) {
            val result = runCatching { block(candidate, api) }
            if (result.isSuccess) {
                activeApiBase = api
                return Attempt(candidate, api, result.getOrThrow())
            }
            lastError = result.exceptionOrNull()
        }
        throw lastError ?: IllegalStateException("Erro ao carregar catalogo")
    }

    fun selectCategory(id: String?) {
        _state.value = _state.value.copy(selectedCategoryId = id, searchQuery = "")
    }

    fun loadLiveIfNeeded() {
        if (_state.value.liveCatalogComplete) return
        viewModelScope.launch {
            val config = _state.value.config ?: return@launch
            cache.loadLive(cache.keyFor(config))?.takeIf { it.channels.isNotEmpty() }
                ?.let { applyCatalog(config, it, preserveSelection = true) }
        }
    }

    fun selectChannel(channel: Channel) {
        val recents = (listOf(channel.id) + _state.value.recentChannelIds)
            .distinct().take(6)
        _state.value = _state.value.copy(
            selectedChannel = channel,
            recentChannelIds = recents,
            epg = emptyList()
        )
        viewModelScope.launch {
            store.configFlow.first()?.let { config -> fetchEpg(config, channel) }
            sendTelemetry("watch_channel", channel)
        }
    }

    private fun fetchEpg(config: ProviderConfig, channel: Channel) {
        epgJob?.cancel()
        epgJob = viewModelScope.launch {
            _state.value = _state.value.copy(epgLoading = true)
            val primary = repo.loadEpg(config, channel.id, activeApiBase)
            // Gateway vazio nao significa que o provedor nao possui guia. Algumas
            // instalacoes nao armazenam EPG no cache do gateway, entao tenta direto.
            val direct = if (primary.isEmpty() && activeApiBase != null)
                repo.loadEpg(config, channel.id, null) else emptyList()
            val alternateId = channel.epgChannelId
                ?.takeIf { it.isNotBlank() && it != channel.id }
            val alternate = if (primary.isEmpty() && direct.isEmpty() && alternateId != null) {
                repo.loadEpg(config, alternateId, activeApiBase).ifEmpty {
                    if (activeApiBase != null) repo.loadEpg(config, alternateId, null) else emptyList()
                }
            } else emptyList()
            val entries = primary.ifEmpty { direct }.ifEmpty { alternate }
            _state.value = _state.value.copy(epg = entries, epgLoading = false)
        }
    }

    fun setSearch(q: String) {
        _state.value = _state.value.copy(searchQuery = q)
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            val config = _state.value.config
            val profileId = _state.value.activeProfileId
            val updated = if (config != null && profileId != null) {
                store.toggleProfileSet(profileAccountKey(config), profileId, "channel_favorites", channel.id)
            } else {
                store.toggleFavorite(channel.id)
                store.favoritesFlow.first()
            }
            _state.value = _state.value.copy(favorites = updated)
        }
    }

    fun retry() {
        viewModelScope.launch {
            store.configFlow.first()?.let { loadCatalog(it) }
        }
    }

    // ── Infraestrutura (Proxy/Gateway, P2P, Super Peer, VPN) ───────────────

    /** Checagem assíncrona com timeouts curtos; chamada ao abrir a tela Config. */
    fun checkInfra() {
        if (_state.value.infra.checking) return
        val cfg = effectiveConfig()
        viewModelScope.launch {
            _state.value = _state.value.copy(infra = _state.value.infra.copy(checking = true))
            coroutineScope {
                val gateway = async { cfg.catalogGatewayBase?.let { hostReachable("$it/health") } }
                val video = async { cfg.videoGatewayBase?.let { hostReachable("$it/health") } }
                val proxy = async { cfg.proxyUrl.takeIf { it.isNotBlank() }?.let { hostReachable(it) } }
                val superPeer = async {
                    cfg.swarmCloud.superPeerUrl.takeIf { it.isNotBlank() }
                        ?.let { hostReachable(it.trimEnd('/') + "/stats") }
                }
                val vpns = cfg.vpnServers.associate { server ->
                    server.id to async {
                        server.url.takeIf { it.isNotBlank() }
                            ?.let { hostReachable(it.trimEnd('/') + "/status") } ?: false
                    }
                }
                _state.value = _state.value.copy(
                    infra = InfraStatus(
                        checking = false,
                        gatewayOk = gateway.await(),
                        videoGatewayOk = video.await(),
                        proxyOk = proxy.await(),
                        superPeerOk = superPeer.await(),
                        vpnOnline = vpns.mapValues { (_, d) -> d.await() }
                    )
                )
            }
        }
    }

    // ── Catálogos da home (VOD + séries) ────────────────────────────────────

    private var homeExtrasJob: Job? = null

    /**
     * Home: carrega VOD e depois SÉRIES em SEQUÊNCIA (não concorrente). Em box de
     * 2GB, dois parses grandes ao mesmo tempo geram pressão de memória/GC e atrasam
     * tudo — inclusive os filmes aparecerem. VOD vem primeiro (é o que o usuário vê
     * primeiro na fileira "Filmes em destaque").
     */
    fun loadHomeCatalogs() {
        if (homeExtrasJob?.isActive == true) return
        homeExtrasJob = viewModelScope.launch {
            loadVodInternal()
            loadSeriesInternal()
        }
    }

    // ── VOD ────────────────────────────────────────────────────────────────

    fun loadVodIfNeeded() {
        viewModelScope.launch { loadVodInternal() }
    }

    private suspend fun loadVodInternal() {
        if (_state.value.vodCatalogComplete || _state.value.vodLoading) return
        val config = store.configFlow.first() ?: return
        val key = cache.keyFor(config)

        val cached = cache.loadVod(key)?.takeIf { it.items.isNotEmpty() }
        if (cached != null) {
            _state.value = _state.value.copy(
                vodCategories = cleanCategories(cached.categories),
                allVod = cached.items,
                vodCatalogComplete = true,
                selectedVodCategoryId = _state.value.selectedVodCategoryId
                    ?: cached.categories.firstOrNull()?.id
            )
            if (cache.vodAgeMs() <= VOD_TTL_MS) return
            return
        }

        _state.value = _state.value.copy(vodLoading = true)
        runCatching { tryConfigs(config) { cfg, api -> repo.loadVod(cfg, api) } }
            .onSuccess { attempt ->
                if (attempt.config != config) store.saveConfig(attempt.config)
                cache.saveVod(cache.keyFor(attempt.config), attempt.value)
                _state.value = _state.value.copy(
                    vodLoading = false,
                    config = attempt.config,
                    vodCategories = cleanCategories(attempt.value.categories),
                    allVod = attempt.value.items,
                    selectedVodCategoryId = _state.value.selectedVodCategoryId
                        ?: attempt.value.categories.firstOrNull()?.id
                )
            }
            .onFailure {
                _state.value = _state.value.copy(vodLoading = false)
            }
    }

    fun updatePlaybackPreferences(transform: (PlaybackPreferences) -> PlaybackPreferences) {
        val updated = transform(_state.value.playbackPreferences)
        AppDns.provider = updated.dnsProvider
        _state.value = _state.value.copy(playbackPreferences = updated)
        viewModelScope.launch { store.savePlaybackPreferences(updated) }
    }

    fun clearContentCache() {
        viewModelScope.launch { cache.clear() }
    }

    private suspend fun refreshVodSilently(config: ProviderConfig) {
        runCatching { tryConfigs(config) { cfg, api -> repo.loadVod(cfg, api) } }
            .onSuccess { attempt ->
                if (attempt.config != config) store.saveConfig(attempt.config)
                cache.saveVod(cache.keyFor(attempt.config), attempt.value)
                _state.value = _state.value.copy(
                    config = attempt.config,
                    vodCategories = cleanCategories(attempt.value.categories),
                    allVod = attempt.value.items,
                    vodCatalogComplete = true,
                    selectedVodCategoryId = _state.value.selectedVodCategoryId
                        ?: attempt.value.categories.firstOrNull()?.id
                )
            }
    }

    fun selectVodCategory(id: String?) {
        _state.value = _state.value.copy(selectedVodCategoryId = id, vodSearchQuery = "")
    }

    fun selectVod(item: VodItem?) {
        _state.value = _state.value.copy(selectedVod = item, vodDetailLoading = false)
        if (item == null || item.plot.isUsefulMetadata()) return
        _state.value = _state.value.copy(vodDetailLoading = true)
        viewModelScope.launch {
            val config = _state.value.config ?: store.configFlow.first() ?: return@launch
            var detailed = repo.loadVodDetail(config, item, activeApiBase)
            if (!detailed.plot.isUsefulMetadata()) {
                val candidates = (listOf(config.xtreamHost) + serverHosts)
                    .filter { it.isNotBlank() }.distinct()
                for (host in candidates) {
                    val candidate = repo.loadVodDetail(config.copy(xtreamHost = host), item, null)
                    if (candidate.plot.isUsefulMetadata()) {
                        detailed = candidate
                        break
                    }
                }
            }
            // Nao reabre nem troca o filme se o usuario fechou/navegou durante a rede.
            if (_state.value.selectedVod?.id == item.id) {
                _state.value = _state.value.copy(
                    selectedVod = detailed,
                    vodDetailLoading = false,
                    allVod = _state.value.allVod.map { if (it.id == detailed.id) detailed else it }
                )
            }
        }
    }

    private fun String?.isUsefulMetadata(): Boolean =
        !isNullOrBlank() && !equals("null", ignoreCase = true)

    fun loadHeroDetail(item: VodItem?) {
        if (item == null || _state.value.heroVod?.id == item.id) return
        viewModelScope.launch {
            val config = _state.value.config ?: store.configFlow.first() ?: return@launch
            val detailed = runCatching {
                repo.loadVodDetail(config, item, activeApiBase)
            }.getOrDefault(item)
            if (_state.value.allVod.any { it.id == item.id }) {
                _state.value = _state.value.copy(heroVod = detailed)
            }
        }
    }

    fun toggleVodFavorite(vod: VodItem) {
        viewModelScope.launch {
            val config = _state.value.config
            val profileId = _state.value.activeProfileId
            val updated = if (config != null && profileId != null) {
                store.toggleProfileSet(profileAccountKey(config), profileId, "vod_favorites", vod.id)
            } else {
                store.toggleVodFavorite(vod.id)
                store.vodFavoritesFlow.first()
            }
            _state.value = _state.value.copy(vodFavorites = updated)
        }
    }

    fun toggleSeriesFavorite(series: SeriesItem) {
        viewModelScope.launch {
            val config = _state.value.config
            val profileId = _state.value.activeProfileId
            val updated = if (config != null && profileId != null) {
                store.toggleProfileSet(profileAccountKey(config), profileId, "series_favorites", series.id)
            } else {
                store.toggleSeriesFavorite(series.id)
                store.seriesFavoritesFlow.first()
            }
            _state.value = _state.value.copy(seriesFavorites = updated)
        }
    }

    fun setVodSearch(q: String) {
        _state.value = _state.value.copy(vodSearchQuery = q)
    }

    // ── Séries ─────────────────────────────────────────────────────────────

    fun loadSeriesIfNeeded() {
        viewModelScope.launch { loadSeriesInternal() }
    }

    private suspend fun loadSeriesInternal() {
        if (_state.value.seriesCatalogComplete || _state.value.seriesLoading) return
        val config = store.configFlow.first() ?: return
        val key = cache.keyFor(config)

        val cached = cache.loadSeries(key)?.takeIf { it.items.isNotEmpty() }
        if (cached != null) {
            _state.value = _state.value.copy(
                seriesCategories = cleanCategories(cached.categories),
                allSeries = cached.items,
                seriesCatalogComplete = true,
                selectedSeriesCategoryId = _state.value.selectedSeriesCategoryId
                    ?: cached.categories.firstOrNull()?.id
            )
            if (cache.seriesAgeMs() <= VOD_TTL_MS) return
            return
        }

        _state.value = _state.value.copy(seriesLoading = true)
        runCatching { tryConfigs(config) { cfg, api -> repo.loadSeries(cfg, api) } }
            .onSuccess { attempt ->
                if (attempt.config != config) store.saveConfig(attempt.config)
                cache.saveSeries(cache.keyFor(attempt.config), attempt.value)
                _state.value = _state.value.copy(
                    seriesLoading = false,
                    config = attempt.config,
                    seriesCategories = cleanCategories(attempt.value.categories),
                    allSeries = attempt.value.items,
                    seriesCatalogComplete = true,
                    selectedSeriesCategoryId = _state.value.selectedSeriesCategoryId
                        ?: attempt.value.categories.firstOrNull()?.id
                )
            }
            .onFailure {
                _state.value = _state.value.copy(seriesLoading = false)
            }
    }

    private suspend fun refreshSeriesSilently(config: ProviderConfig) {
        runCatching { tryConfigs(config) { cfg, api -> repo.loadSeries(cfg, api) } }
            .onSuccess { attempt ->
                if (attempt.config != config) store.saveConfig(attempt.config)
                cache.saveSeries(cache.keyFor(attempt.config), attempt.value)
                _state.value = _state.value.copy(
                    config = attempt.config,
                    seriesCategories = cleanCategories(attempt.value.categories),
                    allSeries = attempt.value.items,
                    seriesCatalogComplete = true,
                    selectedSeriesCategoryId = _state.value.selectedSeriesCategoryId
                        ?: attempt.value.categories.firstOrNull()?.id
                )
            }
    }

    fun selectSeriesCategory(id: String?) {
        _state.value = _state.value.copy(selectedSeriesCategoryId = id, seriesSearchQuery = "")
    }

    fun selectSeries(item: SeriesItem?) {
        _state.value = _state.value.copy(
            selectedSeries = item,
            seriesSeasons = emptyList()
        )
        if (item != null) loadSeriesDetail(item)
    }

    private fun loadSeriesDetail(series: SeriesItem) {
        viewModelScope.launch {
            val config = store.configFlow.first() ?: return@launch
            _state.value = _state.value.copy(seriesDetailLoading = true)
            val primary = repo.loadSeriesDetail(config, series, activeApiBase)
            val gatewayBase = effectiveConfig().catalogGatewayBase
            val detail = if (
                primary.seasons.isEmpty() &&
                !gatewayBase.isNullOrBlank() &&
                gatewayBase != activeApiBase
            ) {
                repo.loadSeriesDetail(config, series, gatewayBase)
            } else {
                primary
            }
            _state.value = _state.value.copy(
                selectedSeries = detail.series,
                allSeries = _state.value.allSeries.map { if (it.id == detail.series.id) detail.series else it },
                seriesSeasons = detail.seasons,
                seriesDetailLoading = false
            )
            cache.saveSeries(cache.keyFor(config), SeriesCatalog(_state.value.seriesCategories, _state.value.allSeries))
        }
    }

    fun runSpeedTest() {
        if (_state.value.connection.testing) return
        viewModelScope.launch {
            _state.value = _state.value.copy(connection = _state.value.connection.copy(testing = true))
            val metrics = withContext(Dispatchers.IO) {
                val samples = mutableListOf<Double>()
                repeat(4) {
                    val started = System.nanoTime()
                    val ok = runCatching {
                        probeClient.newCall(Request.Builder().url(RemoteConfigClient.DEFAULT_URL).head().build())
                            .execute().use { it.isSuccessful }
                    }.getOrDefault(false)
                    if (ok) samples += (System.nanoTime() - started) / 1_000_000.0
                }
                val download = runCatching {
                    val started = System.nanoTime()
                    val bytes = probeClient.newCall(Request.Builder()
                        .url("https://speed.cloudflare.com/__down?bytes=2000000").build())
                        .execute().use { it.body?.bytes()?.size ?: 0 }
                    (bytes * 8.0 / 1_000_000.0) / ((System.nanoTime() - started) / 1_000_000_000.0)
                }.getOrDefault(0.0)
                val upload = runCatching {
                    val payload = ByteArray(256 * 1024)
                    val started = System.nanoTime()
                    probeClient.newCall(Request.Builder().url("https://speed.cloudflare.com/__up")
                        .post(payload.toRequestBody("application/octet-stream".toMediaType())).build())
                        .execute().use { it.isSuccessful }
                    (payload.size * 8.0 / 1_000_000.0) / ((System.nanoTime() - started) / 1_000_000_000.0)
                }.getOrDefault(0.0)
                val ping = samples.average().takeIf { !it.isNaN() } ?: 0.0
                val jitter = if (samples.size > 1) samples.zipWithNext { a, b -> kotlin.math.abs(a - b) }.average() else 0.0
                val loss = (4 - samples.size) * 25.0
                val quality = when {
                    download >= 15 && ping in 1.0..100.0 && loss < 5 -> ConnectionQuality.GOOD
                    download >= 5 && ping in 1.0..180.0 && loss < 25 -> ConnectionQuality.FAIR
                    else -> ConnectionQuality.POOR
                }
                ConnectionMetrics(false, quality, download, upload, ping, jitter, loss, System.currentTimeMillis())
            }
            _state.value = _state.value.copy(connection = metrics)
            sendTelemetry("speed_test")
        }
    }

    private suspend fun sendTelemetry(type: String, channel: Channel? = null) = withContext(Dispatchers.IO) {
        val clientConfig = effectiveConfig()
        val base = clientConfig.telemetryBase ?: return@withContext
        val config = _state.value.config
        val rawId = Settings.Secure.getString(getApplication<Application>().contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        val deviceId = MessageDigest.getInstance("SHA-256").digest(rawId.toByteArray())
            .joinToString("") { "%02x".format(it) }.take(24)
        val c = _state.value.connection
        val appVersion = runCatching {
            getApplication<Application>().packageManager
                .getPackageInfo(getApplication<Application>().packageName, 0)
                .versionName
        }.getOrNull().orEmpty()
        val payload = buildJsonObject {
            put("type", type); put("platform", "Android TV")
            put("deviceProfile", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
            put("hardwareProfile", "${Build.DEVICE} / Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            put("version", appVersion); put("appVersion", appVersion)
            put("deviceId", deviceId); put("user", config?.xtreamUser.orEmpty().take(80))
            put("watching", channel?.name.orEmpty())
            put("quality", c.quality.name.lowercase()); put("downloadMbps", c.downloadMbps)
            put("uploadMbps", c.uploadMbps); put("pingMs", c.pingMs); put("jitterMs", c.jitterMs)
            put("lossPct", c.lossPct); put("channelId", channel?.id.orEmpty())
            put("channelName", channel?.name.orEmpty())
            put("p2pEnabled", clientConfig.swarmCloud.enabled)
            put("p2pTokenConfigured", BuildConfig.SWARMCLOUD_TOKEN.isNotBlank())
            put("superPeerConfigured", clientConfig.swarmCloud.superPeerUrl.isNotBlank())
        }.toString().toRequestBody("application/json".toMediaType())
        runCatching {
            val endpoint = if (type == "heartbeat") "/telemetry/ping" else "/telemetry/report"
            probeClient.newCall(Request.Builder().url(base.trimEnd('/') + endpoint).post(payload).build())
                .execute().use { }
        }
        val swarmStats = SwarmCloudManager.snapshot()
        if (type == "watch_channel" || (type == "heartbeat" && swarmStats.channelId.isNotBlank())) {
            val p2pPayload = buildJsonObject {
                put("user", config?.xtreamUser.orEmpty().take(80))
                put("deviceId", deviceId)
                put("channel", channel?.name.orEmpty())
                put("channelId", channel?.id.orEmpty())
                put("active", swarmStats.connected)
                put("token", BuildConfig.SWARMCLOUD_TOKEN.isNotBlank())
                put("appId", clientConfig.swarmCloud.appId)
                put("platform", "Android TV")
                put("p2pDown", swarmStats.p2pDown / 1024)
                put("p2pUp", swarmStats.p2pUp / 1024)
                put("httpDown", swarmStats.httpDown / 1024)
                put("peers", swarmStats.peers)
                put("swarmId", swarmStats.channelId.takeIf { it.isNotBlank() }?.let { "live-$it" }.orEmpty())
                put("reason", when {
                    !clientConfig.swarmCloud.enabled -> "p2p_disabled"
                    swarmStats.channelId.isBlank() -> "fora_do_piloto"
                    !swarmStats.connected -> "aguardando_swarm"
                    swarmStats.peers == 0 -> "conectado_sem_peer"
                    swarmStats.p2pDown == 0L -> "peer_sem_trafego"
                    else -> "trafego_p2p"
                })
            }.toString().toRequestBody("application/json".toMediaType())
            runCatching {
                probeClient.newCall(
                    Request.Builder().url(base.trimEnd('/') + "/telemetry/p2p").post(p2pPayload).build()
                ).execute().use { }
            }
        }
    }

    fun setSeriesSearch(q: String) {
        _state.value = _state.value.copy(seriesSearchQuery = q)
    }

    private companion object {
        // Idade máxima do cache antes de atualizar em segundo plano
        const val LIVE_TTL_MS = 6 * 60 * 60 * 1000L    // canais: 6h
        const val VOD_TTL_MS = 12 * 60 * 60 * 1000L    // filmes/séries: 12h
    }
}
