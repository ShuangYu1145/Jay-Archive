package love.yu.jaychou

import java.io.File
import java.util.Locale
import love.yu.jaychou.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AlbumLocalizationTest {
    private val raw = AlbumCatalog("2026-10-17", listOf("albums.json", "extended-albums.json").flatMap {
        AlbumRepository.decode(File("src/main/assets/$it").readText()).albums
    })
    private val packs = AlbumLocalization.languages.associateWith {
        File("src/main/assets/i18n/$it.json").readText()
    }

    @Test fun allFivePacksCoverEveryReleaseAndTrackWithCompleteProse() {
        packs.forEach { (language, json) ->
            val albums = JSONObject(json).getJSONObject("albums")
            assertEquals(language, raw.albums.map { it.id }.toSet(), albums.keys().asSequence().toSet())
            raw.albums.forEach { album ->
                val translation = albums.getJSONObject(album.id)
                listOf("title", "summary", "dateNote", "sourceLabel").forEach {
                    assertTrue("$language ${album.id} $it", translation.getString(it).isNotBlank())
                }
                val tracks = translation.getJSONObject("tracks")
                assertEquals(album.tracks.size, tracks.length())
                album.tracks.forEach { assertTrue(tracks.getJSONObject(it.number.toString()).getString("title").isNotBlank()) }
            }
        }
    }

    @Test fun displayTranslationPreservesIdsOriginalNamesDatesAndDiscOrder() {
        AlbumLocalization.languages.forEach { language ->
            val translated = AlbumLocalization.apply(raw, packs, language)
            raw.albums.zip(translated.albums).forEach { (source, result) ->
                assertEquals(source.id, result.id)
                assertEquals(source.title, result.originalTitle)
                assertEquals(source.releaseDate, result.releaseDate)
                assertEquals(source.coverUrl, result.coverUrl)
                assertEquals(source.sourceUrl, result.sourceUrl)
                assertEquals(source.tracks.map { Triple(it.number, it.disc, it.sourceTrackNumber) },
                    result.tracks.map { Triple(it.number, it.disc, it.sourceTrackNumber) })
                assertEquals(source.tracks.map { it.title }, result.tracks.map { it.originalTitle })
            }
        }
    }

    @Test fun missingNullAndBlankFieldsFallBackIndividuallyToSimplifiedChinese() {
        val album = raw.albums.first()
        val partial = JSONObject().put("albums", JSONObject().put(album.id, JSONObject()
            .put("title", "")
            .put("summary", "Translated summary")
            .put("dateNote", JSONObject.NULL)))
        val result = AlbumLocalization.apply(raw, packs + ("ko" to partial.toString()), "ko").albums.first()
        val base = JSONObject(packs.getValue("zh-Hans")).getJSONObject("albums").getJSONObject(album.id)
        assertEquals(base.getString("title"), result.title)
        assertEquals("Translated summary", result.summary)
        assertEquals(base.getString("dateNote"), result.dateNote)
        assertEquals(album.tracks.first().title, result.tracks.first().title)
        assertEquals(raw.albums.size, AlbumLocalization.apply(raw, packs, "unsupported").albums.size)
    }

    @Test fun searchesOriginalTraditionalAndEnglishNamesInEveryDisplayLanguage() {
        AlbumLocalization.languages.forEach { language ->
            val index = AlbumSearchIndex(AlbumLocalization.apply(raw, packs, language))
            listOf("最伟大的作品", "最偉大的作品", "Greatest Works of Art").forEach { query ->
                assertTrue("$language $query", index.search(query).albums.any { it.id == "1551833317039542272" })
            }
            listOf("发如雪", "髮如雪").forEach { query ->
                assertTrue("$language $query", index.search(query).songs.any { it.track.originalTitle == "发如雪" })
            }
        }
    }

    @Test fun chineseScriptsAndRegionalFallbackResolveConsistently() {
        assertEquals("zh-Hant", AppLanguage.contentTag(Locale.forLanguageTag("zh-TW")))
        assertEquals("zh-Hant", AppLanguage.contentTag(Locale.forLanguageTag("zh-HK")))
        assertEquals("zh-Hans", AppLanguage.contentTag(Locale.forLanguageTag("zh-Hans-HK")))
        assertEquals("zh-Hans", AppLanguage.contentTag(Locale.forLanguageTag("zh-CN")))
        assertEquals("en", AppLanguage.contentTag(Locale.US))
        assertEquals("ja", AppLanguage.contentTag(Locale.JAPAN))
        assertEquals("ko", AppLanguage.contentTag(Locale.KOREA))
        assertEquals("zh-Hans", AppLanguage.contentTag(Locale.FRENCH))
    }
}
