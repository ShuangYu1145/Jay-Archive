package love.yu.jaychou.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import love.yu.jaychou.online.MusicProvider
import love.yu.jaychou.online.OnlineSong

object OnlineMedia {
    fun item(song: OnlineSong): MediaItem = MediaItem.Builder()
        .setMediaId(song.key).setUri(Uri.Builder().scheme("jay-online").authority(song.provider.name).appendPath(song.id)
            .appendQueryParameter("mediaMid", song.mediaMid).build())
        .setMediaMetadata(MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).setAlbumTitle(song.album)
            .setArtworkUri(song.cover.takeIf(String::isNotBlank)?.let(Uri::parse))
            .setExtras(Bundle().apply {
                putString("provider", song.provider.name); putString("id", song.id)
                putStringArrayList("artists", ArrayList(song.artists)); putString("mediaMid", song.mediaMid)
                putLong("durationMs", song.durationMs)
            }).build()).build()

    fun song(item: MediaItem): OnlineSong? {
        val metadata = item.mediaMetadata
        val extras = metadata.extras ?: return null
        val provider = runCatching { MusicProvider.valueOf(extras.getString("provider").orEmpty()) }.getOrNull() ?: return null
        return OnlineSong(provider, extras.getString("id").orEmpty(), metadata.title?.toString().orEmpty(),
            extras.getStringArrayList("artists").orEmpty(), metadata.albumTitle?.toString().orEmpty(),
            metadata.artworkUri?.toString().orEmpty(), extras.getLong("durationMs"), extras.getString("mediaMid").orEmpty())
    }
}
