package com.rohan.classic_ai_player.utils


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


sealed class PlayerUiEvents {
    object PlayPause : PlayerUiEvents()
    data class SelectedAudioChange(val index: Int) : PlayerUiEvents()
    data class SeekTo(val position: Float) : PlayerUiEvents()
    object SeekToNext : PlayerUiEvents()
    object Backward : PlayerUiEvents()
    object Forward : PlayerUiEvents()
    data class UpdateProgress(val newProgress: Float) : PlayerUiEvents()
}
