package com.rohan.classic_ai_player.utils

//
//sealed class PlayerState {
//    object Idle : PlayerState()
//    object PlayPause : PlayerState()
//    object SelectedMusicChange : PlayerState()
//    object Backward : PlayerState()
//    object SeekToNext : PlayerState()
//    object Forward : PlayerState()
//    object SeekTo : PlayerState()
//    object Stop : PlayerState()
//    data class UpdateProgress(val newProgress: Float) : PlayerState()
//}

//sealed class PlayerState {
//    object Idle : PlayerState()
//    object Buffering : PlayerState()
//    data class Playing(val currentPosition: Long, val duration: Long) : PlayerState()
//    data class Paused(val currentPosition: Long, val duration: Long) : PlayerState()
//    object Ended : PlayerState()
//    data class Error(val errorMessage: String) : PlayerState()
//}

enum class MusicPlayerState {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
    IsPLAYING,
    PAUSED,
}

sealed class ZPlayerState {
    object PlayPause : ZPlayerState()
    object SelectedMusicChange : ZPlayerState()
    object Backward : ZPlayerState()
    object SeekToNext : ZPlayerState()
    object Forward : ZPlayerState()
    object SeekTo : ZPlayerState()
    object Stop : ZPlayerState()
    data class UpdateProgress(val newProgress: Float) : ZPlayerState()
}