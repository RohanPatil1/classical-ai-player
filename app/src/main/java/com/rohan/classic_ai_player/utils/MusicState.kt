package com.rohan.classic_ai_player.utils

sealed class MusicState {
    object Idle : MusicState()
    data class Ready(val duration: Long) : MusicState()
    data class InProgress(val progress: Long) : MusicState()
    data class Buffering(val progress: Long) : MusicState()
    data class Playing(val isPlaying: Boolean) : MusicState()
    data class CurrentPlaying(val itemIndex: Int) : MusicState()
}