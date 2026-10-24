package love.yu.jaychou.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Own a local copy so the avatar survives picker permissions and source-photo removal. */
object UserAvatarStore {
    suspend fun import(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "profile").apply { mkdirs() }
        val target = File(directory, "avatar-${UUID.randomUUID()}.image")
        try {
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Image unavailable")
            input.use { source ->
                target.outputStream().use { destination ->
                    val buffer = ByteArray(8192)
                    var size = 0L
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        size += read
                        if (size > 20L * 1024 * 1024) throw IOException("Image exceeds 20 MB")
                        destination.write(buffer, 0, read)
                    }
                }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Invalid image")
            target.absolutePath
        } catch (error: Exception) {
            target.delete()
            throw error
        }
    }

    suspend fun remove(context: Context, path: String?) = withContext(Dispatchers.IO) {
        if (path != null) {
            val file = File(path)
            if (file.canonicalFile.parentFile == File(context.filesDir, "profile").canonicalFile) file.delete()
        }
    }
}
