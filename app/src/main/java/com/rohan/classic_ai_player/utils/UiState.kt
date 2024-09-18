package com.rohan.classic_ai_player.utils


sealed class UiState {
    object Idle : UiState()
    object Ready : UiState()
}

//sealed class UiState<out R> {
//    object Idle : UiState<Nothing>()
//    object Loading : UiState<Nothing>()
//    data class Success<T>(val data: T) : UiState<T>()
//    //    data class Error : UiState<Exception>(val exception:Exception):String()
//    data class Error<T>(val data: String) : UiState<T>()
//}


sealed class PlayerUiEvents {
    object PlayPause : PlayerUiEvents()
    data class SelectedAudioChange(val index: Int) : PlayerUiEvents()
    data class SeekTo(val position: Float) : PlayerUiEvents()
    object SeekToNext : PlayerUiEvents()
    object Backward : PlayerUiEvents()
    object Forward : PlayerUiEvents()
    data class UpdateProgress(val newProgress: Float) : PlayerUiEvents()
}
