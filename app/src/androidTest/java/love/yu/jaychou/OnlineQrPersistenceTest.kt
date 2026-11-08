package love.yu.jaychou

import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlinx.coroutines.runBlocking
import love.yu.jaychou.online.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class OnlineQrPersistenceTest {
    @Test fun pendingQrAndConfirmationAreEncryptedAndRestoredAfterStoreRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = "qr-test-" + UUID.randomUUID()
        val isolated = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences(name + suffix, mode)
        }
        try {
            val store = DeviceMusicAccountStore(isolated)
            store.setEndpoint(MusicProvider.QQ, "https://fixture.example.com")
            val pending = PendingMusicLogin(LoginQr("private-qr-key", "data:image/png;base64,aW1hZ2U="),
                "wechat", "https://fixture.example.com", 803, "qqmusic_session=private-confirmation")
            store.setPendingLogin(MusicProvider.QQ, pending)
            store.setSelectedProvider(MusicProvider.QQ)
            assertEquals(pending, DeviceMusicAccountStore(isolated).pendingLogin(MusicProvider.QQ))
            val encrypted = isolated.getSharedPreferences("online_music_accounts", 0).getString("QQ_pending_login", "").orEmpty()
            assertFalse(encrypted.contains(pending.qr.key))
            assertFalse(encrypted.contains(pending.credential))
            store.setEndpoint(MusicProvider.QQ, "https://other.example.com")
            assertNull(DeviceMusicAccountStore(isolated).pendingLogin(MusicProvider.QQ))
        } finally { context.deleteSharedPreferences("online_music_accounts" + suffix) }
    }

    @Test fun savedQrIsAPublishedPngInTheGalleryWithAWhiteQuietZone() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= 29)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = LoginQrGallery.save(context, qrTestImage(), MusicProvider.QQ)
        try {
            context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.RELATIVE_PATH, MediaStore.Images.Media.IS_PENDING), null, null, null)!!.use {
                assertTrue(it.moveToFirst())
                assertEquals("image/png", it.getString(0))
                assertTrue(it.getString(1).contains("Pictures/杰作"))
                assertEquals(0, it.getInt(2))
            }
            val bitmap = context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) }
            assertNotNull(bitmap)
            assertTrue(bitmap.width >= 576)
            assertEquals(Color.WHITE, bitmap.getPixel(0, 0))
            assertEquals(Color.BLACK, bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
            bitmap.recycle()
        } finally { context.contentResolver.delete(uri, null, null) }
    }
}

internal fun qrTestImage(): ByteArray {
    val bitmap = Bitmap.createBitmap(29, 29, Bitmap.Config.ARGB_8888)
    Canvas(bitmap).apply { drawColor(Color.WHITE); drawRect(6f, 6f, 23f, 23f, Paint().apply { color = Color.BLACK }) }
    return ByteArrayOutputStream().use { bytes ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        bitmap.recycle()
        bytes.toByteArray()
    }
}
