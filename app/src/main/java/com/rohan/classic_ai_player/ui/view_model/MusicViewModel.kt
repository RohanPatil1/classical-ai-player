package com.rohan.classic_ai_player.ui.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.rohan.classic_ai_player.data.model.Music
import com.rohan.classic_ai_player.data.repository.MusicRepository
import com.rohan.classic_ai_player.player.service.MusicPlayerHandler
import com.rohan.classic_ai_player.utils.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.LinkedList
import javax.inject.Inject
import kotlin.random.Random

data class Pair(var current: Int, var previous: Int)


@HiltViewModel
class MusicViewModel @Inject constructor(
    private val exoPlayer: ExoPlayer,
    private val repository: MusicRepository,
) : ViewModel() {


    private val musicPlayerHandler: MusicPlayerHandler =
        MusicPlayerHandler(exoPlayer, viewModelScope)

    private val _uiState = MutableLiveData<UIState>()
    val uiState: LiveData<UIState> = _uiState

    var allMusicList = emptyList<Music>()
    private var isMusicListLoaded = false
    private var isShuffleMode = false

    private var playbackPair = Pair(0, 0)

    fun loadMusicIfNeeded() {
        println("LOADED IF NEEDED : ${isMusicListLoaded}")
        if (!isMusicListLoaded) fetchMusicList()
    }

    private fun fetchMusicList() {
        _uiState.value = UIState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            when (val dataResult = repository.getAllMusic()) {
                is DataResult.Success -> {
                    //_allMusicList.postValue(dataResult.data)
                    allMusicList = dataResult.data
                    _uiState.postValue(UIState.Success(data = dataResult.data))
                    isMusicListLoaded = allMusicList.isNotEmpty()
                    println("VM POSTED VALUE")
                }

                is DataResult.Error -> {
                    _uiState.postValue(dataResult.exception.message?.let { UIState.Error(it) })
                }
            }
        }
    }

    fun playMusic(newMusicIndex: Int) {
        playbackPair.previous = playbackPair.current
        playbackPair.current = newMusicIndex

        musicPlayerHandler.playMusic(allMusicList[playbackPair.current])
    }

    fun nextMusic() {
        val currIndex = playbackPair.current
        val nextIndex = if (isShuffleMode) getRandomIndex(currIndex) else currIndex + 1
        val n = allMusicList.size
        playbackPair.previous = playbackPair.current
        playbackPair.current = nextIndex % n

        musicPlayerHandler.playMusic(allMusicList[playbackPair.current])
    }

    fun prevMusic() {
        val prev = playbackPair.previous
        playbackPair.previous = playbackPair.current
        playbackPair.current = prev

        musicPlayerHandler.playMusic(allMusicList[playbackPair.current])
    }

    private fun getRandomIndex(currIndex: Int): Int {
        return if (allMusicList.size > 1) {
            var newIndex: Int
            do {
                newIndex = Random.nextInt(allMusicList.size)
            } while (newIndex == currIndex)
            newIndex
        } else {
            0
        }
    }

    // State for all music
//     val allMusicList = repository.getAllMusic()
//        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
//
//    // State for screen
//    private val _songsScreenUiState = MutableStateFlow<UIState>(UIState.Loading)
//    val songScreenUiState = _songsScreenUiState.asStateFlow()


    /// get entire list of musics
    //  fun loadSongs() {

//        viewModelScope.launch {
//            delay(4999)
//            _allMusicList.value = listOf(Music())
//        }

//         _songsScreenUiState.value = UIState.Loading
//        viewModelScope.launch {
//            repository.getAllMusic().collect { musicList ->
//
//                _songsScreenUiState.value = UIState.Success(musicList)
//                println("DATA RECEIVED VM")
//            }
//        }
    //  }

//    private fun setMediaToExoplayer(musicList: List<Music>) {
//        val mediaItemList = musicPlayerHandler.prepareMusicToMediaList(musicList)
//        _allMediaList.value = mediaItemList
//        _screenState.value = UIState.Success(mediaItemList)
//    }
}


//    val playerState: StateFlow<MusicPlayerHandler.PlayerState> = musicPlayerHandler.playerState
//    val playlist: StateFlow<List<MediaItem>> = musicPlayerHandler.playlist
//
//    // State for all music
//    val allMusicList: StateFlow<List<Music>> = repository.getAllMusic()
//        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
//
//    // State for all playlists
//    val playlists: StateFlow<List<Playlist>> = repository.getAllPlaylists()
//        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
//
//    // State for the currently selected music
//    private val _selectedMusic = MutableStateFlow<Music?>(null)
//    val selectedMusic: StateFlow<Music?> = _selectedMusic.asStateFlow()
//
//    // State for the currently selected playlist
//    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
//    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()
//
//    // Operation state for UI feedback
//    private val _screenState = MutableStateFlow<UIState>(UIState.Idle)
//    val screenState: StateFlow<UIState> = _screenState.asStateFlow()
//
//    init {
//        // fetch all the musics and put in db
//        repository.getAllMusic()
//
//        // set all the musics in exoplayer
//        setMusicToExoplayer()
//    }
//
//    fun playPause() = musicPlayerHandler.playPause()
//    fun skipToNext() = musicPlayerHandler.skipToNext()
//    fun skipToPrevious() = musicPlayerHandler.skipToPrevious()
//    fun seekTo(position: Long) = musicPlayerHandler.seekTo(position)
//    fun playTrackAtIndex(index: Int) = musicPlayerHandler.skipToIndex(index)
//    fun setRepeatMode(repeatMode: Int) = musicPlayerHandler.setRepeatMode(repeatMode)
//    fun setShuffleModeEnabled(enabled: Boolean) = musicPlayerHandler.setShuffleModeEnabled(enabled)
//
//
//    private fun setMusicToExoplayer() {
//        _screenState.value = UIState.Loading
//        viewModelScope.launch {
//            allMusicList.collect { musicList ->
//                if (musicList.isNotEmpty()) {
//                    val mediaItems = musicList.map { music ->
//                        MediaItem.Builder()
//                            .setUri(music.uri)
//                            .setMediaId(music.id.toString())
//                            .build()
//                    }
//                    musicPlayerHandler.setPlaylist(mediaItems)
//                }
//            }
//        }
//        _screenState.value = UIState.Success
//    }
//
//
//    fun getMusicById(id: Long) {
//        performOperation {
//            val music = repository.getMusicById(id)
//            if (music != null) {
//            _selectedMusic.value = music
//            } else {
//                _screenState.value = UIState.Error("Unknown error occurred")
//            }
//        }
//    }
//
//    fun updateAudioStats(id: Long, audioStats: AudioStats) {
//        performOperation {
//            repository.updateAudioStats(id, audioStats)
//            // Refresh the selected music to reflect the changes
//            _selectedMusic.value = repository.getMusicById(id)
//        }
//    }
//
//    fun createPlaylist(playlist: Playlist) {
//        performOperation {
//            repository.createPlaylist(playlist)
//        }
//    }
//
//    fun getPlaylistById(playlistId: Int) {
//        performOperation {
//            val playlist = repository.getPlaylistById(playlistId)
//            _selectedPlaylist.value = playlist
//        }
//    }
//
//
//    private fun performOperation(operation: suspend () -> Unit) {
//        viewModelScope.launch {
//            _screenState.value = UIState.Loading
//            try {
//                operation()
//                _screenState.value = UIState.Success
//            } catch (e: Exception) {
//                _screenState.value = UIState.Error(e.message ?: "Unknown error occurred")
//            }
//        }
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        musicPlayerHandler.release()
//    }
//
sealed class UIState {
    object Loading : UIState()
    data class Success<out T>(val data: T) : UIState()
    data class Error(val message: String) : UIState()
}

