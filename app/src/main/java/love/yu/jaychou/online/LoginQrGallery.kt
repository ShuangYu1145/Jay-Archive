package love.yu.jaychou.online

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.graphics.drawable.toBitmap
import coil.decode.SvgDecoder
import coil.imageLoader
import coil.request.ImageRequest
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LoginQrGallery {
    /** Writes an ordinary PNG, with a white quiet zone, to Pictures/杰作. */
    suspend fun save(context: Context, image: ByteArray, provider: MusicProvider): Uri = withContext(Dispatchers.IO) {
        val result = context.imageLoader.execute(ImageRequest.Builder(context).data(image)
            .decoderFactory(SvgDecoder.Factory()).allowHardware(false).size(1024).build())
        val original = result.drawable?.toBitmap() ?: throw IOException("QR image could not be decoded")
        val scale = (512f / minOf(original.width, original.height)).coerceAtLeast(1f)
        val width = (original.width * scale).toInt()
        val height = (original.height * scale).toInt()
        val bitmap = Bitmap.createBitmap(width + 64, height + 64, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            drawBitmap(original, null, Rect(32, 32, width + 32, height + 32), Paint().apply { isFilterBitmap = false })
        }
        val name = "杰作-${provider.name}-QR-${SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.ROOT).format(Date())}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/杰作")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                @Suppress("DEPRECATION")
                val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "杰作")
                if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Gallery directory could not be created")
                @Suppress("DEPRECATION")
                put(MediaStore.Images.Media.DATA, File(directory, name).absolutePath)
            }
        }
        val resolver = context.contentResolver
        var saved: Uri? = null
        var complete = false
        try {
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Gallery entry could not be created")
            saved = uri
            resolver.openOutputStream(uri)?.use {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("QR image could not be written")
            } ?: throw IOException("Gallery entry could not be opened")
            if (Build.VERSION.SDK_INT >= 29 && resolver.update(uri, ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }, null, null) != 1) throw IOException("Gallery entry could not be published")
            complete = true
            uri
        } finally {
            bitmap.recycle()
            if (!complete && saved != null) resolver.delete(saved, null, null)
        }
    }
}
