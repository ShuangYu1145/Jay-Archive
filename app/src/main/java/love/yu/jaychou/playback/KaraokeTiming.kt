package love.yu.jaychou.playback

/** UTF-16 layout offsets with sub-character timing, including multi-character word tags. */
internal data class KaraokeGlyph(val startOffset: Int, val endOffset: Int, val startMs: Double, val endMs: Double) {
    fun progress(positionMs: Long): Float = when {
        positionMs < startMs -> 0f
        endMs <= startMs -> 1f
        else -> ((positionMs - startMs) / (endMs - startMs)).toFloat().coerceIn(0f, 1f)
    }
}

internal fun karaokeGlyphs(line: EmbeddedLyricLine): List<KaraokeGlyph> {
    val raw = line.words.joinToString("") { it.text }
    val leading = raw.length - raw.trimStart().length
    var offset = -leading
    return buildList {
        for (word in line.words) {
            val count = word.text.codePointCount(0, word.text.length)
            var local = 0
            repeat(count) { index ->
                val width = Character.charCount(word.text.codePointAt(local))
                val start = offset + local
                val duration = (word.endMs - word.startMs).coerceAtLeast(0).toDouble()
                if (start >= 0 && start + width <= line.text.length) add(KaraokeGlyph(
                    start, start + width, word.startMs + duration * index / count,
                    word.startMs + duration * (index + 1) / count))
                local += width
            }
            offset += word.text.length
        }
    }
}
