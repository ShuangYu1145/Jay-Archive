package love.yu.jaychou.data

import android.content.Context
import org.json.JSONObject
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

data class CompositionText(val title: String, val artist: String, val album: String, val lyricist: String)

data class Composition(
    val id: String,
    val text: CompositionText,
    val traditional: CompositionText,
    val releaseDate: String,
    val coverUrl: String,
    val artworkSourceUrl: String,
) {
    fun display(language: String): CompositionText = if (language == "zh-Hans") text else traditional

    private val searchText = normalize(listOf(text, traditional).joinToString(" ") {
        "${it.title} ${it.artist} ${it.album}"
    } + " $releaseDate")

    fun matches(query: String): Boolean = query.trim().split(Regex("\\s+"))
        .all { normalize(it) in searchText }

    companion object {
        private fun normalize(value: String) = Normalizer.normalize(value, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
    }
}

data class CompositionCatalog(val sourceUrl: String, val songs: List<Composition>)

object CompositionRepository {
    fun load(context: Context): CompositionCatalog =
        context.assets.open("compositions.json").bufferedReader().use { decode(it.readText()) }

    fun decode(json: String): CompositionCatalog {
        val root = JSONObject(json)
        fun JSONObject.text() = CompositionText(getString("title"), getString("artist"),
            getString("album"), getString("lyricist"))
        val array = root.getJSONArray("songs")
        val songs = (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            Composition(item.getString("id"), item.text(), item.getJSONObject("traditional").text(),
                item.getString("releaseDate"), item.optString("coverUrl"),
                item.optString("artworkSourceUrl"))
        }.sortedByDescending { it.releaseDate }
        require(songs.map { it.id }.distinct().size == songs.size) { "Duplicate composition IDs" }
        songs.forEach {
            require(it.id.isNotBlank() && it.text.title.isNotBlank() && it.text.artist.isNotBlank())
            LocalDate.parse(it.releaseDate)
        }
        return CompositionCatalog(root.getString("sourceUrl"), songs)
    }
}
