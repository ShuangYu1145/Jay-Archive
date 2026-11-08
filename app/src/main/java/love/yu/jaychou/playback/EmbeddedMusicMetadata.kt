package love.yu.jaychou.playback

import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.CodingErrorAction

data class EmbeddedLyricWord(val startMs: Long, val endMs: Long, val text: String)
data class EmbeddedLyricLine(val startMs: Long?, val text: String, val words: List<EmbeddedLyricWord> = emptyList())
data class EmbeddedMusicMetadata(
    val sampleRate: Int = 0,
    val bitsPerSample: Int = 0,
    val lyrics: List<EmbeddedLyricLine> = emptyList(),
    val durationMs: Long = 0L,
) {
    companion object {
        private const val MAX_TEXT_BYTES = 1_048_576
        private val lyricKeys = setOf("LYRICS", "UNSYNCEDLYRICS", "UNSYNCED LYRICS", "SYNCEDLYRICS", "LRC")
        private val timeTag = Regex("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")
        private val infoTag = Regex("^\\[(?:ti|ar|al|by|offset|length|re|ve|tool):.*]$", RegexOption.IGNORE_CASE)

        /** Reads FLAC STREAMINFO and Vorbis comments only; pictures/audio frames are skipped. */
        fun read(input: InputStream): EmbeddedMusicMetadata {
            val stream = DataInputStream(input)
            if (stream.readInt() != 0x664C6143) throw IOException("Not a FLAC stream")
            var sampleRate = 0
            var bits = 0
            var duration = 0L
            var lyrics = emptyList<EmbeddedLyricLine>()
            var last: Boolean
            do {
                val header = stream.readUnsignedByte()
                last = header and 0x80 != 0
                val type = header and 0x7f
                val length = (stream.readUnsignedByte() shl 16) or
                    (stream.readUnsignedByte() shl 8) or stream.readUnsignedByte()
                if ((type == 0 && length == 34) || (type == 4 && length <= MAX_TEXT_BYTES)) {
                    val block = ByteArray(length).also { stream.readFully(it) }
                    if (type == 0) {
                        var packed = 0L
                        for (index in 10..17) packed = (packed shl 8) or (block[index].toLong() and 255)
                        sampleRate = (packed ushr 44).toInt()
                        bits = ((packed ushr 36) and 31).toInt() + 1
                        if (sampleRate > 0) duration = (packed and 0xfffffffffL) * 1_000 / sampleRate
                    } else {
                        val comments = ByteBuffer.wrap(block).order(ByteOrder.LITTLE_ENDIAN)
                        fun field(): ByteArray {
                            if (comments.remaining() < 4) throw IOException("Truncated FLAC comment")
                            val size = comments.int
                            if (size < 0 || size > comments.remaining()) throw IOException("Invalid FLAC comment size")
                            return ByteArray(size).also { comments.get(it) }
                        }
                        field() // vendor
                        if (comments.remaining() < 4) throw IOException("Missing FLAC comment count")
                        val count = comments.int
                        if (count < 0 || count > comments.remaining() / 4) throw IOException("Invalid FLAC comment count")
                        repeat(count) {
                            val bytes = field()
                            val separator = bytes.indexOf('='.code.toByte())
                            if (separator > 0) {
                                val key = String(bytes, 0, separator, Charsets.US_ASCII).uppercase(java.util.Locale.ROOT)
                                if (key in lyricKeys) {
                                    val payload = bytes.copyOfRange(separator + 1, bytes.size)
                                    val text = try {
                                        Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                                            .decode(ByteBuffer.wrap(payload)).toString()
                                    } catch (_: java.nio.charset.CharacterCodingException) {
                                        String(payload, charset("GB18030"))
                                    }
                                    val candidate = parseLyrics(text)
                                    if (candidate.count { it.words.isNotEmpty() } > lyrics.count { it.words.isNotEmpty() } || lyrics.isEmpty()) {
                                        lyrics = candidate
                                    }
                                }
                            }
                        }
                    }
                } else {
                    var remaining = length
                    while (remaining > 0) {
                        val skipped = stream.skipBytes(remaining)
                        if (skipped == 0) { stream.readByte(); remaining-- } else remaining -= skipped
                    }
                }
            } while (!last)
            return EmbeddedMusicMetadata(sampleRate, bits, lyrics, duration)
        }

        /** Supports plain lyrics, repeated LRC stamps, offsets, and inline word timing. */
        fun parseLyrics(text: String): List<EmbeddedLyricLine> {
            val offset = Regex("\\[offset:([+-]?\\d+)]", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            fun time(match: MatchResult): Long {
                val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0
                return (match.groupValues[1].toLong() * 60_000 + match.groupValues[2].toLong() * 1_000 + fraction + offset).coerceAtLeast(0)
            }
            val lines = text.removePrefix("\uFEFF").trimEnd('\u0000').lineSequence().flatMap { raw ->
                val line = raw.trim()
                if (line.isEmpty() || infoTag.matches(line)) return@flatMap emptySequence()
                val stamps = timeTag.findAll(line).toList()
                if (stamps.isEmpty()) return@flatMap sequenceOf(EmbeddedLyricLine(null, line))
                val segments = stamps.mapIndexed { index, stamp ->
                    line.substring(stamp.range.last + 1, stamps.getOrNull(index + 1)?.range?.first ?: line.length)
                }
                val body = segments.joinToString("").trim()
                if (body.isBlank()) return@flatMap emptySequence()
                val enhanced = segments.dropLast(1).any { it.isNotEmpty() }
                if (enhanced) {
                    val words = stamps.mapIndexedNotNull { index, stamp ->
                        segments[index].takeIf { it.isNotEmpty() }?.let {
                            val start = time(stamp)
                            val end = stamps.getOrNull(index + 1)?.let(::time) ?: start + 1_000
                            EmbeddedLyricWord(start, end.coerceAtLeast(start), it)
                        }
                    }
                    sequenceOf(EmbeddedLyricLine(time(stamps.first()), body, words))
                } else stamps.asSequence().map { EmbeddedLyricLine(time(it), body) }
            }.toList()
            return if (lines.any { it.startMs != null }) lines.sortedBy { it.startMs ?: Long.MAX_VALUE } else lines
        }
    }
}
