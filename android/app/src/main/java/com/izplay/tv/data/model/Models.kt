package com.izplay.tv.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AppProfile(
    val id: String,
    val name: String,
    val avatar: Int = 0,
    val genres: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

/** Uma categoria de canais / VOD / séries. */
@Serializable
data class Category(
    val id: String,
    val name: String,
    val channelCount: Int = 0
)

/** Um canal individual com seu stream e metadados. */
@Serializable
data class Channel(
    val id: String,
    val number: Int,
    val name: String,
    val logoUrl: String?,
    val streamUrl: String,
    val categoryId: String,
    val epgChannelId: String? = null,
    var isFavorite: Boolean = false
)

/** Um programa do guia (EPG). */
@Serializable
data class EpgEntry(
    val channelId: String,
    val title: String,
    val description: String? = null,
    val start: Long,   // epoch millis
    val end: Long
)

/** Item de VOD (filme). */
@Serializable
data class VodItem(
    val id: String,
    val name: String,
    val posterUrl: String?,
    val streamUrl: String,
    val categoryId: String,
    val rating: String? = null,
    val plot: String? = null,
    val year: String? = null,
    val durationSecs: Int = 0,
    /** Unix timestamp enviado pelo Xtream no campo "added". */
    val addedAt: Long = 0L,
    val backdropUrl: String? = null,
    val backdropMobileUrl: String? = null,
    val backdropPositionX: Float = 65f,
    val backdropPositionY: Float = 50f,
    val backdropScale: Float = 1f,
    val overlayOpacity: Float = 0.82f
)

/** Série (conjunto de temporadas). */
@Serializable
data class SeriesItem(
    val id: String,
    val name: String,
    val coverUrl: String?,
    val categoryId: String,
    val rating: String? = null,
    val plot: String? = null,
    val year: String? = null
)

/** Temporada de uma série. */
@Serializable
data class Season(
    val seasonNumber: Int,
    val episodes: List<Episode>
)

data class SeriesDetail(
    val series: SeriesItem,
    val seasons: List<Season>
)

/** Episódio de uma série. */
@Serializable
data class Episode(
    val id: String,
    val title: String,
    val episodeNum: Int,
    val streamUrl: String,
    val durationSecs: Int = 0,
    val plot: String? = null,
    val stillUrl: String? = null
)

/** Configuração do provedor (M3U direto ou API Xtream). */
@Serializable
data class ProviderConfig(
    val mode: Mode,
    val m3uUrl: String = "",
    val xtreamHost: String = "",
    val xtreamUser: String = "",
    val xtreamPass: String = "",
    val epgUrl: String = ""
) {
    enum class Mode { M3U, XTREAM }

    val isValid: Boolean
        get() = when (mode) {
            Mode.M3U -> m3uUrl.isNotBlank()
            Mode.XTREAM -> xtreamHost.isNotBlank() &&
                    xtreamUser.isNotBlank() && xtreamPass.isNotBlank()
        }
}
