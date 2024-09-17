package com.rohan.classic_ai_player.utils


sealed class PlayerState {
    object PlayPause : PlayerState()
    object SelectedMusicChange : PlayerState()
    object Backward : PlayerState()
    object SeekToNext : PlayerState()
    object Forward : PlayerState()
    object SeekTo : PlayerState()
    object Stop : PlayerState()
    data class UpdateProgress(val newProgress: Float) : PlayerState()
}