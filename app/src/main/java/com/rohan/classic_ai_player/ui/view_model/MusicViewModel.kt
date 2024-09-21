package com.rohan.classic_ai_player.ui.view_model

import android.annotation.SuppressLint
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.rohan.classic_ai_player.data.model.AudioStats
import com.rohan.classic_ai_player.data.model.Music
import com.rohan.classic_ai_player.data.model.Playlist
import com.rohan.classic_ai_player.data.repository.MusicRepository
import com.rohan.classic_ai_player.player.service.MusicPlayerHandler
import com.rohan.classic_ai_player.utils.DataResult
import com.rohan.classic_ai_player.utils.MusicState
import com.rohan.classic_ai_player.utils.PlayerState
import com.rohan.classic_ai_player.utils.PlayerUiEvents
import com.rohan.classic_ai_player.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private val audioDummy = Music(
    123, "", "".toUri(), "", "", 0, 0f
)

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val musicPlayerHandler: MusicPlayerHandler,
    private val repository: MusicRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    var duration by savedStateHandle.saveable { mutableLongStateOf(0L) }
    var progress by savedStateHandle.saveable { mutableFloatStateOf(0f) }
    var progressString by savedStateHandle.saveable { mutableStateOf("00:00") }
    var isPlaying by savedStateHandle.saveable { mutableStateOf(false) }
    var currSelectedMusic by savedStateHandle.saveable { mutableStateOf(audioDummy) }
    var musicList by savedStateHandle.saveable { mutableStateOf(listOf<Music>()) }


    private val _playerUiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Idle)
    val playerUiState: StateFlow<UiState> = _playerUiState.asStateFlow()

    init {
        getMusicList()
        initMusicState()
    }

    private fun getMusicList() {
        viewModelScope.launch {

            when (val musicDataList = repository.fetchMusicList()) {
//                is DataResult.Error -> {
//                    _homeNewsUiState.postValue(UiState.Error(apiResult.exception.message.toString()))
//                }
//                is DataResult.Success -> {
//                    newsDataList = apiResult.data.newDataList
//                    _homeNewsUiState.postValue(UiState.Success(data = apiResult.data))
//                }

                is DataResult.Success -> {
                    musicList = musicDataList.data.musicDataList
                    setMusicList()
                }

                is DataResult.Error -> {
                    // something went wrong
                }

            }


        }


    }

    private fun setMusicList() {
        musicList.map { music ->
            MediaItem.Builder()
                .setUri(music.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setAlbumArtist(music.artist)
                        .setDisplayTitle(music.name)
                        .setSubtitle(music.metaData)
                        .build()
                )
                .build()

        }.also {
            musicPlayerHandler.setMusicPlaylist(it)
        }
    }

    private fun initMusicState() {
        viewModelScope.launch {
            musicPlayerHandler.musicState.collectLatest { mediaState ->
                when (mediaState) {
                    MusicState.Idle -> {
                        _playerUiState.value = UiState.Idle
                    }

                    is MusicState.Buffering -> {
                        computeProgress(mediaState.progress)
                    }

                    is MusicState.CurrentPlaying -> {
                        currSelectedMusic = musicList[mediaState.itemIndex]
                    }

                    is MusicState.InProgress -> {
                        computeProgress(mediaState.progress)
                    }

                    is MusicState.Playing -> {
                        isPlaying = mediaState.isPlaying
                    }

                    is MusicState.Ready -> {
                        duration = mediaState.duration
                        _playerUiState.value = UiState.Ready
                    }
                }
            }
        }
    }

    fun onPlayerUiChanged(uiEvents: PlayerUiEvents) = viewModelScope.launch {
        when (uiEvents) {
            PlayerUiEvents.Backward -> musicPlayerHandler.handlePlayerState(PlayerState.Backward)
            PlayerUiEvents.Forward -> musicPlayerHandler.handlePlayerState(PlayerState.Forward)
            PlayerUiEvents.SeekToNext -> musicPlayerHandler.handlePlayerState(PlayerState.SeekToNext)
            is PlayerUiEvents.PlayPause -> {
                musicPlayerHandler.handlePlayerState(
                    PlayerState.PlayPause
                )
            }

            is PlayerUiEvents.SeekTo -> {
                musicPlayerHandler.handlePlayerState(
                    PlayerState.SeekTo,
                    seekPosition = ((duration * uiEvents.position) / 100f).toLong()
                )
            }

            is PlayerUiEvents.SelectedAudioChange -> {
                musicPlayerHandler.handlePlayerState(
                    PlayerState.SelectedMusicChange,
                    selectedMediaIndex = uiEvents.index
                )
            }

            is PlayerUiEvents.UpdateProgress -> {
                musicPlayerHandler.handlePlayerState(
                    PlayerState.UpdateProgress(
                        uiEvents.newProgress
                    )
                )
                progress = uiEvents.newProgress
            }
        }
    }


    private fun computeProgress(currentProgress: Long) {
        progress =
            if (currentProgress > 0) ((currentProgress.toFloat() / duration.toFloat()) * 100f)
            else 0f
        progressString = formatDuration(currentProgress)
    }

    @SuppressLint("DefaultLocale")
    fun formatDuration(duration: Long): String {
        val minute = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
        val seconds = (minute) - minute * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES)
        return String.format("%02d:%02d", minute, seconds)
    }

    override fun onCleared() {
        viewModelScope.launch {
            musicPlayerHandler.handlePlayerState(PlayerState.Stop)
        }

        super.onCleared()
    }


    //--------------------------------------------------------------------------------

    init {
        repository.getAllMusic()
    }


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
    private val _uiState = MutableStateFlow<UIState>(UIState.Idle)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()


    fun getMusicById(id: Int) {
        performOperation {
            val music = repository.getMusicById(id)
            _selectedMusic.value = music
        }
    }

    fun updateAudioStats(id: Int, audioStats: AudioStats) {
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
            _uiState.value = UIState.Loading
            try {
                operation()
                _uiState.value = UIState.Success
            } catch (e: Exception) {
                _uiState.value = UIState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    sealed class UIState {
        object Idle : UIState()
        object Loading : UIState()
        object Success : UIState()
        data class Error(val message: String) : UIState()
    }

}