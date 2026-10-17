package love.yu.jaychou.data

import android.content.Context
import org.json.JSONObject

enum class ReleaseCategory(val label: String) {
    STUDIO("录音室 CD"), LIVE("演唱会 CD"), EP("EP 专辑"), SINGLE("单曲"), OTHER("原声与其他"),
}

data class Track(val number: Int, val title: String, val disc: Int = 1, val sourceTrackNumber: Int = number)

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
) {
    val year: String get() = releaseDate.take(4)
    val summary: String get() = editionSummary.ifBlank { when (id) {
        "2037128764168736768" -> "第 16 张录音室专辑。收录 12 首新作与特别收录曲《圣诞星》，以《太阳之子》为开篇。数字版于 2026 年 3 月 25 日发行，实体版于 4 月 10 日发行。"
        "1551833317039542272" -> "以《Intro》开篇，收录同名作品，以及此前发行的《说好不哭》《Mojito》等歌曲。官方介绍中的封面取景地为法国圣米歇尔山。"
        "1150824720824930304" -> "周杰伦的第 14 张录音室专辑。以《床边故事》开篇，包含《说走就走》《告白气球》等作品，共 10 首歌曲。"
        "1151759216223064064" -> "以《鞋子特大号》开篇。官方介绍以卓别林的幽默为视觉线索，专辑封面采用复古场景，并呈现两个周杰伦相遇的画面。"
        "1151769443223867392" -> "第 12 张录音室专辑，在 2012 年 12 月发行并收录 12 首作品。标题将专辑序号、发行年份与歌曲数量联系在一起，收录《红尘客栈》《明明就》等作品。"
        "1151771029539000320" -> "第 11 张录音室专辑，发行于 2011 年 11 月 11 日。以同名曲开场，歌曲涵盖《迷魂曲》《Mine Mine》《皮影戏》与《水手怕水》等不同题材。"
        "1151771608675913728" -> "2010 年发行的第 10 张录音室专辑。官方介绍以跨越世代与自我突破为线索，封面采用吸血鬼造型；收录《超人不会飞》《说了再见》等作品。"
        "1151772159916511232" -> "以魔术与想象为视觉主题的录音室专辑。由《龙战骑士》开篇、《稻香》收尾，收录《给我一首歌的时间》《花海》等 11 首作品。"
        "1151772542550282240" -> "2007 年发行的录音室专辑。以《牛仔很忙》开篇，收录《彩虹》《青花瓷》《蒲公英的约定》等歌曲，共 10 首。"
        "1151773902062620672" -> "以《夜的第七章》的侦探叙事开启专辑。官方介绍将古典音乐、电子音色与 Rap 相结合，此外收录《听妈妈的话》《千里之外》《菊花台》等作品。"
        "1151774270238625792" -> "第 6 张录音室专辑，以作曲家肖邦为命名线索，于 11 月 1 日发行。收录《夜曲》《发如雪》等作品，以及《飘移》《一路向北》两首曲目。"
        "1152151605810958336" -> "2004 年发行的第 5 张录音室专辑。收录同名曲《七里香》，以及《止战之殇》《外婆》《搁浅》等作品，原版共 10 首歌曲。"
        "1152152148126076928" -> "第 4 张录音室专辑，以周杰伦母亲的名字命名。《以父之名》为开篇，收录《晴天》《东风破》等歌曲；官方介绍以复古视觉和现代音乐的结合为线索。"
        "1152152649680949248" -> "第 3 张录音室专辑。官方介绍将标题与八度音阶和想象空间联系起来；以《半兽人》开篇，收录《暗号》《爷爷泡的茶》《最后的战役》等作品。"
        "1152155344949088256" -> "周杰伦的第 2 张录音室专辑，名称取自英文 Fantasy 的音译。以音乐中的想象力为线索，收录《爱在西元前》《双截棍》《安静》等 10 首作品。"
        "1152155845522493440" -> "2000 年发行的首张个人录音室专辑，是周杰伦从幕后音乐创作走向个人专辑的起点。收录《可爱女人》《星晴》《黑色幽默》《龙卷风》等 10 首作品。"
        else -> "周杰伦于 ${year} 年发行的录音室专辑。收录《${tracks.first().title}》《${tracks[1].title}》等 ${tracks.size} 首作品；下方按杰威尔官方专辑页的原始曲序列出。"
    } }
    val dateNote: String get() = editionDateNote.ifBlank { when (id) {
        "2037128764168736768" -> "此处使用首次数字发行日期；实体版发行于 2026-04-10。"
        "1151772159916511232" -> "日期沿用 JVR 官方目录（2008-10-14）；部分其他资料记为 10 月 15 日。"
        "1152152148126076928" -> "日期沿用 JVR 官方目录（2003-07-29）；部分其他资料记为 7 月 31 日。"
        else -> "日期依据 JVR 官方目录，以台湾当地日期（UTC+08:00）呈现。"
    } }
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
        return AlbumCatalog(studio.verifiedAt, albums)
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
