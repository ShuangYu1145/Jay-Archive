package love.yu.jaychou.data

import org.json.JSONObject

/** Translation packs override display fields, never identity, ordering or lookup names. */
object AlbumLocalization {
    val languages = listOf("zh-Hans", "zh-Hant", "en", "ja", "ko")

    fun apply(catalog: AlbumCatalog, jsonPacks: Map<String, String>, language: String): AlbumCatalog {
        val packs = jsonPacks.mapValues { JSONObject(it.value).optJSONObject("albums") ?: JSONObject() }
        val fallback = packs["zh-Hans"] ?: JSONObject()
        val selected = packs[language] ?: fallback
        return catalog.copy(albums = catalog.albums.map { album ->
            val base = fallback.optJSONObject(album.id) ?: JSONObject()
            val translated = selected.optJSONObject(album.id) ?: JSONObject()
            fun text(key: String, original: String): String = translated.nonBlank(key)
                ?: base.nonBlank(key) ?: original
            val variants = packs.values.mapNotNull { it.optJSONObject(album.id) }
            album.copy(
                title = text("title", album.title),
                editionSummary = text("summary", album.editionSummary),
                editionDateNote = text("dateNote", album.editionDateNote),
                sourceLabel = text("sourceLabel", album.sourceLabel),
                searchAliases = (listOf(album.originalTitle, album.title, album.englishTitle) +
                    variants.flatMap { variant -> listOfNotNull(variant.nonBlank("title")) + variant.aliases() }).distinct(),
                tracks = album.tracks.map { track ->
                    val key = track.number.toString()
                    val baseTrack = base.optJSONObject("tracks")?.optJSONObject(key)
                    val translatedTrack = translated.optJSONObject("tracks")?.optJSONObject(key)
                    track.copy(
                        title = translatedTrack?.nonBlank("title") ?: baseTrack?.nonBlank("title") ?: track.title,
                        searchAliases = (listOf(track.originalTitle, track.title) + variants.flatMap { variant ->
                            val value = variant.optJSONObject("tracks")?.optJSONObject(key)
                            listOfNotNull(value?.nonBlank("title")) + value?.aliases().orEmpty()
                        }).distinct(),
                    )
                },
            )
        })
    }

    private fun JSONObject.nonBlank(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.aliases(): List<String> = optJSONArray("aliases")?.let { array ->
        (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
    }.orEmpty()
}
