package com.vertigo.v_player

import android.net.Uri

data class Song(
    var id: String,
    var title: String,
    var artist: String,
    var album: String,
    var uri: Uri,
    var duration: Long,
    var albumArt: String?
)