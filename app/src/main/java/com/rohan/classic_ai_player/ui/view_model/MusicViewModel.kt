package com.rohan.classic_ai_player.ui.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.rohan.classic_ai_player.data.model.AudioStats
import com.rohan.classic_ai_player.data.model.Music
import com.rohan.classic_ai_player.data.model.Playlist
import com.rohan.classic_ai_player.data.repository.MusicRepository
import com.rohan.classic_ai_player.player.service.MusicPlayerHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MusicViewModel @Inject constructor(
    private val exoPlayer: ExoPlayer,
    private val repository: MusicRepository,
) : ViewModel() {

    private val musicPlayerHandler: MusicPlayerHandler =
        MusicPlayerHandler(exoPlayer, viewModelScope)

    val playerState: StateFlow<MusicPlayerHandler.PlayerState> = musicPlayerHandler.playerState
    val playlist: StateFlow<List<MediaItem>> = musicPlayerHandler.playlist

    // State for all music
    val allMusicList: StateFlow<List<Music>> = repository.getAllMusic()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // State for all playlists
    val playlists: StateFlow<List<Playlist>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // State for the currently selected music
    private val _selectedMusic = MutableStateFlow<Music?>(null)
    val selectedMusic: StateFlow<Music?> = _selectedMusic.asStateFlow()

    // State for the currently selected playlist
    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    // Operation state for UI feedback
    private val _screenState = MutableStateFlow<UIState>(UIState.Idle)
    val screenState: StateFlow<UIState> = _screenState.asStateFlow()

    init {
        // fetch all the musics and put in db
        repository.getAllMusic()

        // set all the musics in exoplayer
        setMusicToExoplayer()
    }

    fun playPause() = musicPlayerHandler.playPause()
    fun skipToNext() = musicPlayerHandler.skipToNext()
    fun skipToPrevious() = musicPlayerHandler.skipToPrevious()
    fun seekTo(position: Long) = musicPlayerHandler.seekTo(position)
    fun playTrackAtIndex(index: Int) = musicPlayerHandler.skipToIndex(index)
    fun setRepeatMode(repeatMode: Int) = musicPlayerHandler.setRepeatMode(repeatMode)
    fun setShuffleModeEnabled(enabled: Boolean) = musicPlayerHandler.setShuffleModeEnabled(enabled)


    private fun setMusicToExoplayer() {
        _screenState.value = UIState.Loading
        viewModelScope.launch {
            allMusicList.collect { musicList ->
                if (musicList.isNotEmpty()) {
                    val mediaItems = musicList.map { music ->
                        MediaItem.Builder()
                            .setUri(music.uri)
                            .setMediaId(music.id.toString())
                            .build()
                    }
                    musicPlayerHandler.setPlaylist(mediaItems)
                }
            }
        }
        _screenState.value = UIState.Success
    }


    fun getMusicById(id: Long) {
        performOperation {
            val music = repository.getMusicById(id)
            if (music != null) {
            _selectedMusic.value = music
            } else {
                _screenState.value = UIState.Error("Unknown error occurred")
            }
        }
    }

    fun updateAudioStats(id: Long, audioStats: AudioStats) {
        performOperation {
            repository.updateAudioStats(id, audioStats)
            // Refresh the selected music to reflect the changes
            _selectedMusic.value = repository.getMusicById(id)
        }
    }

    fun createPlaylist(playlist: Playlist) {
        performOperation {
            repository.createPlaylist(playlist)
        }
    }

    fun getPlaylistById(playlistId: Int) {
        performOperation {
            val playlist = repository.getPlaylistById(playlistId)
            _selectedPlaylist.value = playlist
        }
    }


    private fun performOperation(operation: suspend () -> Unit) {
        viewModelScope.launch {
            _screenState.value = UIState.Loading
            try {
                operation()
                _screenState.value = UIState.Success
            } catch (e: Exception) {
                _screenState.value = UIState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicPlayerHandler.release()
    }

    sealed class UIState {
        object Idle : UIState()
        object Loading : UIState()
        object Success : UIState()
        data class Error(val message: String) : UIState()
    }

}