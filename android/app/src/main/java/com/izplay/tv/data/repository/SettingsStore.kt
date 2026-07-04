package com.izplay.tv.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.izplay.tv.data.model.ProviderConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "izplay_settings")

/** Guarda config do provedor e ids de canais favoritos no DataStore. */
class SettingsStore(private val context: Context) {

    private object Keys {
        val MODE = stringPreferencesKey("mode")
        val M3U = stringPreferencesKey("m3u_url")
        val HOST = stringPreferencesKey("xtream_host")
        val USER = stringPreferencesKey("xtream_user")
        val PASS = stringPreferencesKey("xtream_pass")
        val EPG = stringPreferencesKey("epg_url")
        val FAVS = stringSetPreferencesKey("favorites")
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

    suspend fun toggleFavorite(channelId: String) {
        context.dataStore.edit { p ->
            val current = p[Keys.FAVS] ?: emptySet()
            p[Keys.FAVS] = if (channelId in current) current - channelId
            else current + channelId
        }
    }

    suspend fun currentFavorites(): Set<String> = favoritesFlow.first()
}
