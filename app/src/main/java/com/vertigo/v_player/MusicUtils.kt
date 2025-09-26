package com.vertigo.v_player

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log

object MusicUtils {

    private const val TAG = "MusicUtils"

    fun getLocalSongs(context: Context): List<Song> {
        val songs = mutableListOf<Song>()
        val contentResolver: ContentResolver = context.contentResolver

        val musicUri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor: Cursor? = contentResolver.query(musicUri, projection, selection, null, sortOrder)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idColumn = it.getColumnIndex(MediaStore.Audio.Media._ID)
                    val titleColumn = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
                    val artistColumn = it.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                    val albumColumn = it.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                    val durationColumn = it.getColumnIndex(MediaStore.Audio.Media.DURATION)
                    val dataColumn = it.getColumnIndex(MediaStore.Audio.Media.DATA)
                    val albumIdColumn = it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                    do {
                        try {
                            val id = it.getLong(idColumn)
                            var title = it.getString(titleColumn) ?: "Unknown Title"
                            var artist = it.getString(artistColumn) ?: "Unknown Artist"
                            var album = it.getString(albumColumn) ?: "Unknown Album"
                            val duration = it.getLong(durationColumn)
                            val data = it.getString(dataColumn)
                            val albumId = it.getLong(albumIdColumn)

                            // Skip if data path is null
                            if (data.isNullOrEmpty()) continue

                            val albumArtUri = getAlbumArtUri(albumId)

                            val song = Song(
                                id = id.toString(),
                                title = title,
                                artist = artist,
                                album = album,
                                uri = Uri.parse(data),
                                duration = duration,
                                albumArt = albumArtUri?.toString()
                            )

                            songs.add(song)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading song data: ${e.message}")
                        }
                    } while (it.moveToNext())
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception - permission denied: ${e.message}")
            throw e // Re-throw to handle in MainActivity
        } catch (e: Exception) {
            Log.e(TAG, "Error querying media store: ${e.message}")
        }

        return songs
    }

    private fun getAlbumArtUri(albumId: Long): Uri? {
        return if (albumId < 0) {
            null
        } else {
            ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
        }
    }

    fun formatDuration(duration: Long): String {
        if (duration <= 0) return "00:00"

        val minutes = (duration / 1000) / 60
        val seconds = (duration / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}