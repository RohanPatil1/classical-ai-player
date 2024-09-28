package com.rohan.classic_ai_player.utils

import android.net.Uri
import com.rohan.classic_ai_player.data.model.Music

data class ClassicPlayerState(
    val isLoading: Boolean = false,
    val musicList: List<Music> = emptyList(),
    val selectedMusic: Music = Music(
        artistName = "",
        contentUri = Uri.EMPTY,
        songName = "",
        musicId = 0L,
        albumName = "",
        duration = 0
    ),
    val currentPosition: Int = 0,
)
