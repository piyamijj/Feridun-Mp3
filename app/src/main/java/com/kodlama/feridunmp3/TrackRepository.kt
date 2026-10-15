package com.kodlama.feridunmp3

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import android.os.Bundle

/** MediaStore üzerinden .mp3, .m4a ve .wav dosyalarını tarar. */
object TrackRepository {

    const val EXTRA_DURATION = "duration_ms"

    fun load(context: Context): List<MediaItem> {
        val unknownArtist = context.getString(R.string.unknown_artist)
        val M = MediaStore.Audio.Media.MIME_TYPE
        val N = MediaStore.Audio.Media.DISPLAY_NAME
        val selection = "${MediaStore.Audio.Media.DURATION} > 0 AND " +
            "${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND " +
            "${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND " +
            "${MediaStore.Audio.Media.IS_ALARM} = 0 AND " +
            "($M IN (?,?,?,?,?) OR $N LIKE ? OR $N LIKE ? OR $N LIKE ?)"
        val args = arrayOf(
            "audio/mpeg", "audio/mp4", "audio/x-m4a", "audio/x-wav", "audio/wav",
            "%.mp3", "%.m4a", "%.wav"
        )
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        val result = ArrayList<MediaItem>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, args,
            null
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val artistRaw = c.getString(artistCol)
                val artist = if (artistRaw.isNullOrBlank() || artistRaw == "<unknown>") unknownArtist else artistRaw
                val extras = Bundle().apply { putLong(EXTRA_DURATION, c.getLong(durCol)) }
                result.add(
                    MediaItem.Builder()
                        .setMediaId(id.toString())
                        .setUri(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(c.getString(titleCol) ?: "")
                                .setArtist(artist)
                                .setIsPlayable(true)
                                .setExtras(extras)
                                .build()
                        )
                        .build()
                )
            }
        }
        val collator = java.text.Collator.getInstance(java.util.Locale.getDefault())
        result.sortWith { a, b ->
            collator.compare(a.mediaMetadata.title?.toString() ?: "", b.mediaMetadata.title?.toString() ?: "")
        }
        return result
    }
}
