package love.yu.jaychou.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Keep an owned, bounded copy independent of the document provider's URI permission. */
object BackgroundImageStore {
    suspend fun import(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "backgrounds").apply { mkdirs() }
        val id = UUID.randomUUID()
        val source = File(directory, "$id.tmp")
        val target = File(directory, "$id.png")
        try {
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Image unavailable")
            input.use { stream ->
                source.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > 30L * 1024 * 1024) throw IOException("Image exceeds 30 MB")
                        output.write(buffer, 0, count)
                    }
                }
            }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.path, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) throw IOException("Invalid image")
            options.inJustDecodeBounds = false
            options.inSampleSize = 1
            while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 2560) {
                options.inSampleSize *= 2
            }
            val decoded = BitmapFactory.decodeFile(source.path, options) ?: throw IOException("Invalid image")
            val orientation = runCatching {
                ExifInterface(source.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix().apply {
                when (orientation) {
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                    ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
                }
            }
            val bitmap = try {
                if (matrix.isIdentity) decoded
                else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            } finally { if (!matrix.isIdentity) decoded.recycle() }
            try {
                target.outputStream().use {
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("Unable to save image")
                }
            } finally { bitmap.recycle() }
            target.absolutePath
        } catch (error: Exception) {
            target.delete()
            throw error
        } finally { source.delete() }
    }

    suspend fun remove(context: Context, path: String?) = withContext(Dispatchers.IO) {
        if (path != null) {
            val file = File(path)
            if (file.canonicalFile.parentFile == File(context.filesDir, "backgrounds").canonicalFile) file.delete()
        }
    }
}
