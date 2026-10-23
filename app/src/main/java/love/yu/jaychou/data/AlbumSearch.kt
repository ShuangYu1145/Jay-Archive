package love.yu.jaychou.data

import java.text.Normalizer
import java.util.Locale

data class SongMatch(val album: Album, val track: Track)
data class AlbumSearchResults(val albums: List<Album>, val songs: List<SongMatch>)

/** Build once per catalogue; typing only scans normalized, in-memory titles. */
class AlbumSearchIndex(catalog: AlbumCatalog) {
    private val albums = catalog.albums.map {
        it to (listOf(it.title, it.englishTitle, it.originalTitle) + it.searchAliases).map(::normalize).distinct()
    }
    private val songs = catalog.albums.flatMap { album ->
        album.tracks.map { SongMatch(album, it) to (listOf(it.title, it.originalTitle) + it.searchAliases).map(::normalize).distinct() }
    }

    fun search(query: String): AlbumSearchResults {
        val needle = normalize(query)
        if (needle.isEmpty()) return AlbumSearchResults(emptyList(), emptyList())
        return AlbumSearchResults(
            albums.filter { (_, titles) -> titles.any { needle in it } }
                .sortedBy { (_, titles) -> if (needle in titles) 0 else 1 }.map { it.first },
            songs.filter { (_, titles) -> titles.any { needle in it } }
                .sortedBy { if (needle in it.second) 0 else 1 }.map { it.first },
        )
    }

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT).filterNot { it.isWhitespace() }
}
