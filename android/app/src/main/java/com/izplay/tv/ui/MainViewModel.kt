package com.izplay.tv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.izplay.tv.data.model.*
import com.izplay.tv.data.remote.RemoteConfigClient
import com.izplay.tv.data.repository.ContentRepository
import com.izplay.tv.data.repository.SettingsStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class UiState(
    val loading: Boolean = false,
    val error: String? = null,
    val configured: Boolean = false,
    val config: ProviderConfig? = null,

    // ── Canais ao vivo ──────────────────────────────
    val categories: List<Category> = emptyList(),
    val allChannels: List<Channel> = emptyList(),
    val selectedCategoryId: String? = null,
    val selectedChannel: Channel? = null,
    val searchQuery: String = "",
    val favorites: Set<String> = emptySet(),
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

    // ── Séries ────────────────────────────────────────
    val seriesLoading: Boolean = false,
    val seriesCategories: List<Category> = emptyList(),
    val allSeries: List<SeriesItem> = emptyList(),
    val selectedSeriesCategoryId: String? = null,
    val seriesSearchQuery: String = "",
    val selectedSeries: SeriesItem? = null,
    val seriesSeasons: List<Season> = emptyList(),
    val seriesDetailLoading: Boolean = false
) {
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

    companion object {
        const val FAVORITES_ID = "__favorites__"
    }
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ContentRepository()
    private val store = SettingsStore(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var epgJob: Job? = null

    // Host(s) do provedor vindos do Painel Admin (o admin troca a DNS lá). Fallback local.
    private var serverHosts: List<String> = RemoteConfigClient.FALLBACK_HOSTS

    init {
        viewModelScope.launch {
            val hosts = RemoteConfigClient().fetchHosts()
            if (hosts.isNotEmpty()) serverHosts = hosts
        }
        viewModelScope.launch {
            val favs = store.favoritesFlow.first()
            _state.value = _state.value.copy(favorites = favs)
            val config = store.configFlow.first()
            if (config != null && config.isValid) {
                _state.value = _state.value.copy(configured = true, config = config)
                loadCatalog(config)
            }
        }
    }

    fun saveAndLoad(config: ProviderConfig) {
        viewModelScope.launch {
            store.saveConfig(config)
            _state.value = _state.value.copy(configured = true, config = config)
            loadCatalog(config)
        }
    }

    fun logout() {
        viewModelScope.launch {
            store.clearConfig()
            _state.value = UiState()
        }
    }

    /** Login Xtream pelo painel: o host vem da config remota (admin gerencia); o usuário
     *  só digita usuário e senha. */
    fun loginXtream(user: String, pass: String) {
        val host = serverHosts.firstOrNull() ?: "http://cxst.shop"
        saveAndLoad(ProviderConfig(ProviderConfig.Mode.XTREAM, "", host, user.trim(), pass, ""))
    }

    fun loginM3u(url: String) {
        saveAndLoad(ProviderConfig(ProviderConfig.Mode.M3U, url.trim(), "", "", "", ""))
    }

    private fun loadCatalog(config: ProviderConfig) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { repo.loadCatalog(config) }
                .onSuccess { catalog ->
                    val firstCat = catalog.categories.firstOrNull()?.id
                    _state.value = _state.value.copy(
                        loading = false,
                        categories = catalog.categories,
                        allChannels = catalog.channels,
                        selectedCategoryId = firstCat,
                        selectedChannel = catalog.channels.firstOrNull()
                    )
                    catalog.channels.firstOrNull()?.let { fetchEpg(config, it) }
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = e.message ?: "Erro ao carregar catálogo"
                    )
                }
        }
    }

    fun selectCategory(id: String?) {
        _state.value = _state.value.copy(selectedCategoryId = id, searchQuery = "")
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
        }
    }

    private fun fetchEpg(config: ProviderConfig, channel: Channel) {
        epgJob?.cancel()
        epgJob = viewModelScope.launch {
            _state.value = _state.value.copy(epgLoading = true)
            val streamId = channel.epgChannelId ?: channel.id
            val entries = repo.loadEpg(config, streamId)
            _state.value = _state.value.copy(epg = entries, epgLoading = false)
        }
    }

    fun setSearch(q: String) {
        _state.value = _state.value.copy(searchQuery = q)
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            store.toggleFavorite(channel.id)
            _state.value = _state.value.copy(favorites = store.favoritesFlow.first())
        }
    }

    fun retry() {
        viewModelScope.launch {
            store.configFlow.first()?.let { loadCatalog(it) }
        }
    }

    // ── VOD ────────────────────────────────────────────────────────────────

    fun loadVodIfNeeded() {
        if (_state.value.allVod.isNotEmpty() || _state.value.vodLoading) return
        viewModelScope.launch {
            val config = store.configFlow.first() ?: return@launch
            _state.value = _state.value.copy(vodLoading = true)
            runCatching { repo.loadVod(config) }
                .onSuccess { catalog ->
                    _state.value = _state.value.copy(
                        vodLoading = false,
                        vodCategories = catalog.categories,
                        allVod = catalog.items,
                        selectedVodCategoryId = catalog.categories.firstOrNull()?.id
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(vodLoading = false)
                }
        }
    }

    fun selectVodCategory(id: String?) {
        _state.value = _state.value.copy(selectedVodCategoryId = id, vodSearchQuery = "")
    }

    fun selectVod(item: VodItem?) {
        _state.value = _state.value.copy(selectedVod = item)
    }

    fun setVodSearch(q: String) {
        _state.value = _state.value.copy(vodSearchQuery = q)
    }

    // ── Séries ─────────────────────────────────────────────────────────────

    fun loadSeriesIfNeeded() {
        if (_state.value.allSeries.isNotEmpty() || _state.value.seriesLoading) return
        viewModelScope.launch {
            val config = store.configFlow.first() ?: return@launch
            _state.value = _state.value.copy(seriesLoading = true)
            runCatching { repo.loadSeries(config) }
                .onSuccess { catalog ->
                    _state.value = _state.value.copy(
                        seriesLoading = false,
                        seriesCategories = catalog.categories,
                        allSeries = catalog.items,
                        selectedSeriesCategoryId = catalog.categories.firstOrNull()?.id
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(seriesLoading = false)
                }
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
        if (item != null) loadSeriesDetail(item.id)
    }

    private fun loadSeriesDetail(seriesId: String) {
        viewModelScope.launch {
            val config = store.configFlow.first() ?: return@launch
            _state.value = _state.value.copy(seriesDetailLoading = true)
            val seasons = repo.loadSeriesDetail(config, seriesId)
            _state.value = _state.value.copy(
                seriesSeasons = seasons,
                seriesDetailLoading = false
            )
        }
    }

    fun setSeriesSearch(q: String) {
        _state.value = _state.value.copy(seriesSearchQuery = q)
    }
}
