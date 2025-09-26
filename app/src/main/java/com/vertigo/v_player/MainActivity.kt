package com.vertigo.v_player

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager

class MainActivity : AppCompatActivity(), SongAdapter.OnItemClickListener {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    private lateinit var songsRecyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var songAdapter: SongAdapter
    private val songList = mutableListOf<Song>()

    private var musicService: MusicService? = null
    private var isServiceBound = false

    private lateinit var playerControls: LinearLayout
    private lateinit var currentSongTitle: TextView
    private lateinit var currentSongArtist: TextView
    private lateinit var songSeekBar: SeekBar
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnNext: ImageButton

    private val seekBarHandler = Handler()
    private lateinit var updateSeekBar: Runnable

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isServiceBound = true

            musicService?.setSongList(songList)
            updatePlayerUI()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
            musicService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        checkPermissions()
        setupPlayerControls()
    }

    private fun initializeViews() {
        songsRecyclerView = findViewById(R.id.songs_recycler_view)
        playerControls = findViewById(R.id.player_controls)
        currentSongTitle = findViewById(R.id.current_song_title)
        currentSongArtist = findViewById(R.id.current_song_artist)
        songSeekBar = findViewById(R.id.song_seekbar)
        btnPlayPause = findViewById(R.id.btn_play_pause)
        btnPrevious = findViewById(R.id.btn_previous)
        btnNext = findViewById(R.id.btn_next)

        songsRecyclerView.layoutManager = LinearLayoutManager(this)
        songAdapter = SongAdapter(songList, this)
        songsRecyclerView.adapter = songAdapter
    }

    private fun checkPermissions() {
        // Check if we have permission
        if (hasStoragePermission()) {
            // Permission already granted, load songs
            loadSongs()
            startMusicService()
        } else {
            // Request permission
            requestStoragePermission()
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires READ_MEDIA_AUDIO instead of READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, load songs
                    loadSongs()
                    startMusicService()
                    Toast.makeText(this, "Permission granted! Loading songs...", Toast.LENGTH_SHORT).show()
                } else {
                    // Permission denied
                    Toast.makeText(
                        this,
                        "Permission denied! The app cannot access your music files.",
                        Toast.LENGTH_LONG
                    ).show()

                    // Show empty state or instructions
                    showPermissionDeniedState()
                }
            }
        }
    }

    private fun showPermissionDeniedState() {
        // You can show a message or instructions to the user
        Toast.makeText(
            this,
            "Please grant storage permission in Settings to use this app",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun loadSongs() {
        Thread {
            try {
                val songs = MusicUtils.getLocalSongs(this@MainActivity)
                runOnUiThread {
                    songList.clear()
                    songList.addAll(songs)
                    songAdapter.updateSongs(songList)

                    if (songs.isEmpty()) {
                        Toast.makeText(
                            this@MainActivity,
                            "No songs found on device",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Loaded ${songs.size} songs",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: SecurityException) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Security exception: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Error loading songs: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun startMusicService() {
        val serviceIntent = Intent(this, MusicService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setupPlayerControls() {
        btnPlayPause.setOnClickListener { togglePlayPause() }
        btnPrevious.setOnClickListener { playPrevious() }
        btnNext.setOnClickListener { playNext() }

        songSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && isServiceBound) {
                    musicService?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        updateSeekBar = object : Runnable {
            override fun run() {
                if (isServiceBound && musicService?.isPlaying() == true) {
                    val currentPosition = musicService?.getCurrentPosition() ?: 0
                    val duration = musicService?.getDuration() ?: 0

                    if (duration > 0) {
                        songSeekBar.max = duration
                        songSeekBar.progress = currentPosition
                    }

                    seekBarHandler.postDelayed(this, 1000)
                }
            }
        }
    }

    override fun onItemClick(song: Song, position: Int) {
        if (isServiceBound) {
            musicService?.playSong(position)
            updatePlayerUI()
            startSeekBarUpdate()
        }
    }

    private fun togglePlayPause() {
        if (isServiceBound) {
            if (musicService?.isPlaying() == true) {
                musicService?.pause()
                btnPlayPause.setImageResource(R.drawable.ic_play)
            } else {
                musicService?.play()
                btnPlayPause.setImageResource(R.drawable.ic_pause)
                startSeekBarUpdate()
            }
        }
    }

    private fun playPrevious() {
        if (isServiceBound) {
            musicService?.previous()
            updatePlayerUI()
            startSeekBarUpdate()
        }
    }

    private fun playNext() {
        if (isServiceBound) {
            musicService?.next()
            updatePlayerUI()
            startSeekBarUpdate()
        }
    }

    private fun updatePlayerUI() {
        if (isServiceBound) {
            val currentSong = musicService?.getCurrentSong()
            if (currentSong != null) {
                playerControls.visibility = LinearLayout.VISIBLE
                currentSongTitle.text = currentSong.title
                currentSongArtist.text = currentSong.artist

                if (musicService?.isPlaying() == true) {
                    btnPlayPause.setImageResource(R.drawable.ic_pause)
                } else {
                    btnPlayPause.setImageResource(R.drawable.ic_play)
                }
            }
        }
    }

    private fun startSeekBarUpdate() {
        seekBarHandler.removeCallbacks(updateSeekBar)
        seekBarHandler.post(updateSeekBar)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        seekBarHandler.removeCallbacks(updateSeekBar)

        val serviceIntent = Intent(this, MusicService::class.java)
        stopService(serviceIntent)
    }
}