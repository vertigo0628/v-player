package com.vertigo.v_player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SongAdapter(
    private var songList: List<Song>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(song: Song, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songList[position]
        holder.bind(song, position)
    }

    override fun getItemCount(): Int = songList.size

    fun updateSongs(songs: List<Song>) {
        songList = songs
        notifyDataSetChanged()
    }

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val albumArt: ImageView = itemView.findViewById(R.id.album_art)
        private val title: TextView = itemView.findViewById(R.id.song_title)
        private val artist: TextView = itemView.findViewById(R.id.song_artist)
        private val duration: TextView = itemView.findViewById(R.id.song_duration)

        fun bind(song: Song, position: Int) {
            title.text = song.title
            artist.text = song.artist
            duration.text = MusicUtils.formatDuration(song.duration)

            if (song.albumArt != null) {
                Glide.with(itemView.context)
                    .load(song.albumArt)
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note)
                    .into(albumArt)
            } else {
                albumArt.setImageResource(R.drawable.ic_music_note)
            }

            itemView.setOnClickListener {
                listener.onItemClick(song, position)
            }
        }
    }
}