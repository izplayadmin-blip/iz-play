package com.izplay.tv.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.izplay.tv.data.model.ProviderConfig
import com.izplay.tv.data.model.AppProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "izplay_settings")

data class PlaybackPreferences(
    val dnsProvider: String = "Automático (sistema)",
    val player: String = "Interno",
    val quality: String = "Automática",
    val streamType: String = "Automático",
    val bufferSeconds: Int = 30,
    val fallbackMpv: Boolean = true,
    val parentalControl: Boolean = false
)

/** Guarda config do provedor e favoritos de canais, filmes e series no DataStore. */
class SettingsStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val MODE = stringPreferencesKey("mode")
        val M3U = stringPreferencesKey("m3u_url")
        val HOST = stringPreferencesKey("xtream_host")
        val USER = stringPreferencesKey("xtream_user")
        val PASS = stringPreferencesKey("xtream_pass")
        val EPG = stringPreferencesKey("epg_url")
        val FAVS = stringSetPreferencesKey("favorites")
        val VOD_FAVS = stringSetPreferencesKey("vod_favorites")
        val SERIES_FAVS = stringSetPreferencesKey("series_favorites")
        val PLAYER = stringPreferencesKey("player")
        val QUALITY = stringPreferencesKey("quality")
        val STREAM_TYPE = stringPreferencesKey("stream_type")
        val BUFFER_SECONDS = intPreferencesKey("buffer_seconds")
        val FALLBACK_MPV = booleanPreferencesKey("fallback_mpv")
        val PARENTAL_CONTROL = booleanPreferencesKey("parental_control")
        val DNS_PROVIDER = stringPreferencesKey("dns_provider")
    }

    val configFlow: Flow<ProviderConfig?> = context.dataStore.data.map { p ->
        val mode = p[Keys.MODE] ?: return@map null
        ProviderConfig(
            mode = ProviderConfig.Mode.valueOf(mode),
            m3uUrl = p[Keys.M3U] ?: "",
            xtreamHost = p[Keys.HOST] ?: "",
            xtreamUser = p[Keys.USER] ?: "",
            xtreamPass = p[Keys.PASS] ?: "",
            epgUrl = p[Keys.EPG] ?: ""
        )
    }

    suspend fun saveConfig(config: ProviderConfig) {
        context.dataStore.edit { p ->
            p[Keys.MODE] = config.mode.name
            p[Keys.M3U] = config.m3uUrl
            p[Keys.HOST] = config.xtreamHost
            p[Keys.USER] = config.xtreamUser
            p[Keys.PASS] = config.xtreamPass
            p[Keys.EPG] = config.epgUrl
        }
    }

    /** Limpa a config do provedor (logout) — mantém os favoritos. */
    suspend fun clearConfig() {
        context.dataStore.edit { p ->
            p.remove(Keys.MODE)
            p.remove(Keys.M3U)
            p.remove(Keys.HOST)
            p.remove(Keys.USER)
            p.remove(Keys.PASS)
            p.remove(Keys.EPG)
        }
    }

    val favoritesFlow: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.FAVS] ?: emptySet() }
    val vodFavoritesFlow: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.VOD_FAVS] ?: emptySet() }
    val seriesFavoritesFlow: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.SERIES_FAVS] ?: emptySet() }

    suspend fun toggleFavorite(channelId: String) {
        context.dataStore.edit { p ->
            val current = p[Keys.FAVS] ?: emptySet()
            p[Keys.FAVS] = if (channelId in current) current - channelId
            else current + channelId
        }
    }

    val playbackPreferencesFlow: Flow<PlaybackPreferences> =
        context.dataStore.data.map { preferences ->
            PlaybackPreferences(
                dnsProvider = preferences[Keys.DNS_PROVIDER] ?: "Automático (sistema)",
                player = preferences[Keys.PLAYER] ?: "Interno",
                quality = preferences[Keys.QUALITY] ?: "Automática",
                streamType = preferences[Keys.STREAM_TYPE] ?: "Automático",
                bufferSeconds = preferences[Keys.BUFFER_SECONDS] ?: 30,
                fallbackMpv = preferences[Keys.FALLBACK_MPV] ?: true,
                parentalControl = preferences[Keys.PARENTAL_CONTROL] ?: false
            )
        }

    suspend fun savePlaybackPreferences(value: PlaybackPreferences) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DNS_PROVIDER] = value.dnsProvider
            preferences[Keys.PLAYER] = value.player
            preferences[Keys.QUALITY] = value.quality
            preferences[Keys.STREAM_TYPE] = value.streamType
            preferences[Keys.BUFFER_SECONDS] = value.bufferSeconds
            preferences[Keys.FALLBACK_MPV] = value.fallbackMpv
            preferences[Keys.PARENTAL_CONTROL] = value.parentalControl
        }
    }

    suspend fun toggleVodFavorite(vodId: String) {
        context.dataStore.edit { p ->
            val current = p[Keys.VOD_FAVS] ?: emptySet()
            p[Keys.VOD_FAVS] = if (vodId in current) current - vodId else current + vodId
        }
    }

    suspend fun toggleSeriesFavorite(seriesId: String) {
        context.dataStore.edit { p ->
            val current = p[Keys.SERIES_FAVS] ?: emptySet()
            p[Keys.SERIES_FAVS] = if (seriesId in current) current - seriesId else current + seriesId
        }
    }

    suspend fun currentFavorites(): Set<String> = favoritesFlow.first()

    private fun accountSuffix(account: String): String =
        account.trim().lowercase().hashCode().toUInt().toString(16)

    private fun profilesKey(account: String) =
        stringPreferencesKey("profiles_${accountSuffix(account)}")

    private fun activeProfileKey(account: String) =
        stringPreferencesKey("active_profile_${accountSuffix(account)}")

    suspend fun loadProfiles(account: String): List<AppProfile> {
        val raw = context.dataStore.data.first()[profilesKey(account)] ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(AppProfile.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    suspend fun saveProfiles(account: String, profiles: List<AppProfile>) {
        context.dataStore.edit { preferences ->
            preferences[profilesKey(account)] =
                json.encodeToString(ListSerializer(AppProfile.serializer()), profiles.take(6))
        }
    }

    suspend fun loadActiveProfileId(account: String): String? =
        context.dataStore.data.first()[activeProfileKey(account)]

    suspend fun saveActiveProfileId(account: String, profileId: String) {
        context.dataStore.edit { it[activeProfileKey(account)] = profileId }
    }

    private fun profileSetKey(account: String, profileId: String, type: String) =
        stringSetPreferencesKey("${type}_${accountSuffix(account)}_${profileId.hashCode().toUInt().toString(16)}")

    suspend fun loadProfileSet(account: String, profileId: String, type: String): Set<String> =
        context.dataStore.data.first()[profileSetKey(account, profileId, type)] ?: emptySet()

    suspend fun saveProfileSet(account: String, profileId: String, type: String, values: Set<String>) {
        context.dataStore.edit { it[profileSetKey(account, profileId, type)] = values }
    }

    private fun profileListKey(account: String, profileId: String, type: String) =
        stringPreferencesKey("${type}_${accountSuffix(account)}_${profileId.hashCode().toUInt().toString(16)}")

    suspend fun loadProfileList(account: String, profileId: String, type: String): List<String> =
        context.dataStore.data.first()[profileListKey(account, profileId, type)]
            ?.split(',')
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?: emptyList()

    suspend fun saveProfileList(account: String, profileId: String, type: String, values: List<String>) {
        context.dataStore.edit {
            it[profileListKey(account, profileId, type)] =
                values.filter(String::isNotBlank).distinct().take(20).joinToString(",")
        }
    }

    suspend fun toggleProfileSet(account: String, profileId: String, type: String, value: String): Set<String> {
        var updated = emptySet<String>()
        context.dataStore.edit { preferences ->
            val key = profileSetKey(account, profileId, type)
            val current = preferences[key] ?: emptySet()
            updated = if (value in current) current - value else current + value
            preferences[key] = updated
        }
        return updated
    }
}
