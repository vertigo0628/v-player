package com.vertigo.v_player

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.io.IOException

class MusicService : Service(), MediaPlayer.OnCompletionListener {

    companion object {
        private const val TAG = "MusicService"
    }

    private val mediaPlayer = MediaPlayer()
    private val songList = mutableListOf<Song>()
    private var currentPosition = 0
    private val binder = MusicBinder()

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer.setOnCompletionListener(this)
    }

    override fun onBind(intent: Intent): IBinder = binder

    fun setSongList(songs: List<Song>) {
        songList.clear()
        songList.addAll(songs)
    }

    fun playSong(position: Int) {
        if (songList.isEmpty() || position < 0 || position >= songList.size) {
            return
        }

        currentPosition = position
        val song = songList[position]

        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(this, song.uri)
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: IOException) {
            Log.e(TAG, "Error playing song: ${e.message}")
        }
    }

    fun play() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    fun next() {
        if (songList.isNotEmpty()) {
            currentPosition = (currentPosition + 1) % songList.size
            playSong(currentPosition)
        }
    }

    fun previous() {
        if (songList.isNotEmpty()) {
            currentPosition = (currentPosition - 1 + songList.size) % songList.size
            playSong(currentPosition)
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer.seekTo(position)
    }

    fun isPlaying(): Boolean = mediaPlayer.isPlaying

    fun getCurrentPosition(): Int = mediaPlayer.currentPosition

    fun getDuration(): Int = mediaPlayer.duration

    fun getCurrentSong(): Song? {
        return if (songList.isEmpty() || currentPosition < 0 || currentPosition >= songList.size) {
            null
        } else {
            songList[currentPosition]
        }
    }

    fun getSongList(): List<Song> = songList

    override fun onCompletion(mp: MediaPlayer?) {
        next()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}