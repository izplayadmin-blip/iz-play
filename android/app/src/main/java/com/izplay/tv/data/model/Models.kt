package com.izplay.tv.data.model

/** Uma categoria de canais / VOD / séries. */
data class Category(
    val id: String,
    val name: String,
    val channelCount: Int = 0
)

/** Um canal individual com seu stream e metadados. */
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
data class EpgEntry(
    val channelId: String,
    val title: String,
    val description: String? = null,
    val start: Long,   // epoch millis
    val end: Long
)

/** Item de VOD (filme). */
data class VodItem(
    val id: String,
    val name: String,
    val posterUrl: String?,
    val streamUrl: String,
    val categoryId: String,
    val rating: String? = null,
    val plot: String? = null,
    val year: String? = null,
    val durationSecs: Int = 0
)

/** Série (conjunto de temporadas). */
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
data class Season(
    val seasonNumber: Int,
    val episodes: List<Episode>
)

/** Episódio de uma série. */
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
