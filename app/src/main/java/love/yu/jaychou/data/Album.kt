package love.yu.jaychou.data

import android.content.Context
import org.json.JSONObject

enum class ReleaseCategory(private val sourceLabel: String) {
    STUDIO("录音室 CD"), LIVE("演唱会 CD"), EP("EP 专辑"), SINGLE("单曲"), OTHER("原声与其他");
    val label: String get() = AppText.fromOriginal(sourceLabel)
}

data class Track(
    val number: Int, val title: String, val disc: Int = 1, val sourceTrackNumber: Int = number,
    val originalTitle: String = title,
    val searchAliases: List<String> = emptyList(),
)

data class Album(
    val id: String,
    val title: String,
    val englishTitle: String,
    val releaseDate: String,
    val sourceDateRaw: String,
    val sourceUrl: String,
    val coverUrl: String,
    val coverAsset: String,
    val tracks: List<Track>,
    val category: ReleaseCategory = ReleaseCategory.STUDIO,
    val sourceLabel: String = "jvrmusic，QQ Music，Apple Music",
    val editionSummary: String = "",
    val editionDateNote: String = "",
    val originalTitle: String = title,
    val searchAliases: List<String> = emptyList(),
) {
    val year: String get() = releaseDate.take(4)
    val summary: String get() = editionSummary
    val dateNote: String get() = editionDateNote
}

data class AlbumCatalog(val verifiedAt: String, val albums: List<Album>) {
    fun find(id: String?): Album? = albums.find { it.id == id }
}

object AlbumRepository {
    const val artistSource = "https://www.jvrmusic.com/artist/profile/1150822038412333056"
    const val albumSource = "https://www.jvrmusic.com/artist/gallery/list/1150822038412333056?type=album"
    const val portraitUrl = "https://www.jvrmusic.com/upload/2041392644382396416/mH76_2611n1.jpg"

    fun load(context: Context): AlbumCatalog {
        val studio = decode(context.assets.open("albums.json").bufferedReader().use { it.readText() })
        val extra = decode(context.assets.open("extended-albums.json").bufferedReader().use { it.readText() })
        val albums = (studio.albums + extra.albums).sortedByDescending { it.releaseDate }
        require(albums.map { it.id }.distinct().size == albums.size) { "Duplicate release IDs" }
        val catalog = AlbumCatalog(studio.verifiedAt, albums)
        val packs = AlbumLocalization.languages.associateWith { language ->
            context.assets.open("i18n/$language.json").bufferedReader().use { it.readText() }
        }
        val language = AppLanguage.contentTag(context.resources.configuration.locales)
        return AlbumLocalization.apply(catalog, packs, language)
    }

    fun decode(json: String): AlbumCatalog {
        val root = JSONObject(json)
        val array = root.getJSONArray("albums")
        val albums = (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val songs = item.getJSONArray("tracks")
            Album(
                id = item.getString("id"), title = item.getString("title"),
                englishTitle = item.getString("englishTitle"),
                releaseDate = item.getString("releaseDate"),
                sourceDateRaw = item.getString("sourceDateRaw"),
                sourceUrl = item.getString("sourceUrl"), coverUrl = item.getString("coverUrl"),
                coverAsset = item.getString("coverAsset"),
                tracks = (0 until songs.length()).map {
                    val song = songs.optJSONObject(it)
                    if (song == null) Track(it + 1, songs.getString(it))
                    else Track(it + 1, song.getString("title"), song.optInt("disc", 1), song.optInt("sourceTrackNumber", it + 1))
                },
                category = ReleaseCategory.entries.find { it.name == item.optString("category") } ?: ReleaseCategory.STUDIO,
                sourceLabel = item.optString("sourceLabel", "杰威尔音乐"),
                editionSummary = item.optString("summary"), editionDateNote = item.optString("dateNote"),
            )
        }.sortedByDescending { it.releaseDate }
        require(albums.map { it.id }.distinct().size == albums.size) { "Duplicate album IDs" }
        return AlbumCatalog(root.getString("verifiedAt"), albums)
    }
}
