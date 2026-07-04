package com.izplay.tv.data.remote

import com.izplay.tv.data.model.Category
import com.izplay.tv.data.model.Channel

/**
 * Parser de playlists M3U/M3U8 no formato estendido (#EXTINF).
 * Extrai tvg-id, tvg-logo, group-title e o nome do canal.
 */
object M3uParser {

    private val attrRegex = Regex("""(\S+?)="(.*?)"""")

    data class Result(
        val categories: List<Category>,
        val channels: List<Channel>
    )

    fun parse(content: String): Result {
        val channels = mutableListOf<Channel>()
        val categoryCounts = linkedMapOf<String, Int>()

        val lines = content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .iterator()

        var number = 0
        while (lines.hasNext()) {
            val line = lines.next()
            if (!line.startsWith("#EXTINF", ignoreCase = true)) continue

            val attrs = attrRegex.findAll(line)
                .associate { it.groupValues[1] to it.groupValues[2] }
            val displayName = line.substringAfterLast(',').trim()
            val group = attrs["group-title"]?.ifBlank { "SEM CATEGORIA" } ?: "SEM CATEGORIA"

            // próxima linha não-comentário é a URL
            var url: String? = null
            while (lines.hasNext()) {
                val candidate = lines.next()
                if (candidate.startsWith("#")) continue
                url = candidate
                break
            }
            if (url.isNullOrBlank()) continue

            number++
            channels += Channel(
                id = attrs["tvg-id"]?.ifBlank { null } ?: "ch_$number",
                number = number,
                name = displayName.ifBlank { "Canal $number" },
                logoUrl = attrs["tvg-logo"]?.ifBlank { null },
                streamUrl = url,
                categoryId = group,
                epgChannelId = attrs["tvg-id"]?.ifBlank { null }
            )
            categoryCounts[group] = (categoryCounts[group] ?: 0) + 1
        }

        val categories = categoryCounts.map { (name, count) ->
            Category(id = name, name = name, channelCount = count)
        }
        return Result(categories, channels)
    }
}
