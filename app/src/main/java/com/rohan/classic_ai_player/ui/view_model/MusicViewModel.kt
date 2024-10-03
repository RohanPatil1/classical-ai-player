@file:OptIn(SavedStateHandleSaveableApi::class)

package com.rohan.classic_ai_player.ui.view_model

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.rohan.classic_ai_player.data.model.Music
import com.rohan.classic_ai_player.data.model.Playlist
import com.rohan.classic_ai_player.data.repository.MusicRepository
import com.rohan.classic_ai_player.player.normalizer.MusicNormalizer
import com.rohan.classic_ai_player.player.service.MusicPlayerHandler
import com.rohan.classic_ai_player.utils.DataResult
import com.rohan.classic_ai_player.utils.MusicState
import com.rohan.classic_ai_player.utils.PlayerUiEvents
import com.rohan.classic_ai_player.utils.ZPlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@UnstableApi
@HiltViewModel
class MusicViewModel @Inject constructor(
    private val exoPlayer: ExoPlayer,
    private val repository: MusicRepository,
    private val musicNormalizer: MusicNormalizer,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val musicDummy = Music(
        musicId = 0L,
        albumArt = Uri.EMPTY,
        contentUri = Uri.EMPTY,
        songName = "",
        artistName = "",
        duration = 0,
        title = ""
    )

    val playlistDummy = Playlist(playlistName = "", musicIds = emptyList())
    private val musicPlayerHandler: MusicPlayerHandler =
        MusicPlayerHandler(exoPlayer, viewModelScope, musicNormalizer)

    private val _appCurrentPlayList = MutableStateFlow<List<Music>>(listOf())
    val appCurrentPlayList = _appCurrentPlayList.asStateFlow()


    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists = _playlists.asStateFlow()

    private val _progress = MutableStateFlow<Float>(0f)
    val progress = _progress.asStateFlow()

    private val _uiState = MutableLiveData<UIState>()
    val uiState: LiveData<UIState> = _uiState

    var duration by savedStateHandle.saveable { mutableLongStateOf(0L) }
    var progressString by savedStateHandle.saveable { mutableStateOf("00:00") }

    private val _currSelectedMusic = MutableStateFlow<Music>(musicDummy)
    val currSelectedMusic = _currSelectedMusic.asStateFlow()

    private val _currSelectedPlaylistMusic = MutableStateFlow<List<Music>>(emptyList())
    val currSelectedPlaylistMusic = _currSelectedPlaylistMusic.asStateFlow()

    private val _isPlaying = MutableStateFlow<Boolean>(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _selectedMusicIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedMusicIds = _selectedMusicIds.asStateFlow()


    private var isMusicListLoaded = false

    private val _exoPlayer = exoPlayer
    val mExoPlayer = _exoPlayer

    init {

        initMusicState()
    }

    fun loadMusicIfNeeded() {
        println("LOADED IF NEEDED : ${isMusicListLoaded}")
        if (!isMusicListLoaded) {
            fetchMusicList()
            fetchAllPlaylists()
        }
    }

    private fun fetchMusicList() {
        _uiState.value = UIState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            when (val dataResult = repository.getAllMusic()) {
                is DataResult.Success -> {

                    _appCurrentPlayList.value = dataResult.data
                    _uiState.postValue(UIState.Success(data = dataResult.data))
                    isMusicListLoaded = _appCurrentPlayList.value.isNotEmpty()

                    withContext(Dispatchers.Main) {
                        setMediaItemList(_appCurrentPlayList.value)

                        if (isMusicListLoaded) _currSelectedMusic.value = dataResult.data[0]
                    }
                    println("VM POSTED VALUE")
                }

                is DataResult.Error -> {
                    _uiState.postValue(dataResult.exception.message?.let { UIState.Error(it) })
                }

                else -> {}
            }
        }
    }


    private fun setMediaItemList(musicList: List<Music>) {
        musicPlayerHandler.setMusicPlaylist(musicList)
    }


    private fun initMusicState() {
        viewModelScope.launch {
            musicPlayerHandler.musicPlayerState.collectLatest { mediaState ->
                when (mediaState) {
                    MusicState.Idle -> {
                    }

                    is MusicState.Buffering -> {
                        computeProgress(mediaState.progress)
                    }

                    is MusicState.CurrentPlaying -> {
                        //_currSelectedMusic.value = appCurrentPlayList.value[mediaState.itemIndex]
                    }

                    is MusicState.InProgress -> {
                        computeProgress(mediaState.progress)
                    }

                    is MusicState.Playing -> {
                        _isPlaying.value = mediaState.isPlaying

                    }

                    is MusicState.Ready -> {
                        duration = mediaState.duration
                    }
                }
            }
        }
    }

    fun onPlayerUiChanged(uiEvents: PlayerUiEvents) = viewModelScope.launch {
        when (uiEvents) {
            PlayerUiEvents.Backward -> {
//                musicPlayerHandler.handlePlayerState(ZPlayerState.Backward)
            }
            PlayerUiEvents.Forward -> musicPlayerHandler.handlePlayerState(ZPlayerState.Forward)
            PlayerUiEvents.SeekToNext -> {
                //musicPlayerHandler.handlePlayerState(ZPlayerState.SeekToNext, )
            }
            is PlayerUiEvents.PlayPause -> {
                musicPlayerHandler.handlePlayerState(
                    ZPlayerState.PlayPause
                )
            }

            is PlayerUiEvents.SeekTo -> {
                musicPlayerHandler.handlePlayerState(
                    ZPlayerState.SeekTo,
                    seekPosition = ((duration * uiEvents.position) / 100f).toLong()
                )
            }

            is PlayerUiEvents.SelectedAudioChange -> {
                var n = 1
                if (currSelectedPlaylistMusic.value.isNotEmpty()) {
                    // current item from playlist
                    n = currSelectedPlaylistMusic.value.size
                    _currSelectedMusic.value = currSelectedPlaylistMusic.value[uiEvents.index % n]
                } else {
                    n = appCurrentPlayList.value.size
                    _currSelectedMusic.value = appCurrentPlayList.value[uiEvents.index % n]
                }
                musicPlayerHandler.applyNormalization(_currSelectedMusic.value)
                musicPlayerHandler.handlePlayerState(
                    ZPlayerState.SelectedMusicChange,
                    selectedMediaIndex = uiEvents.index % n
                )
            }

            is PlayerUiEvents.UpdateProgress -> {
                musicPlayerHandler.handlePlayerState(
                    ZPlayerState.UpdateProgress(
                        uiEvents.newProgress
                    )
                )
                _progress.value = uiEvents.newProgress

            }
        }
    }


    private fun computeProgress(currentProgress: Long) {
        _progress.value =
            if (currentProgress > 0) ((currentProgress.toFloat() / duration.toFloat()) * 100f)
            else 0f
        progressString = formatDuration(currentProgress)

        println("PROGRESS VM: ${_progress.value}")
        println("TOTAL DURATION VM: ${currSelectedMusic.value.duration}")
    }

    @SuppressLint("DefaultLocale")
    fun formatDuration(duration: Long): String {
        val minute = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
        val seconds = (minute) - minute * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES)
        return String.format("%02d:%02d", minute, seconds)
    }


    fun createNewPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val musicIdList = _selectedMusicIds.value.toList()
            repository.createPlaylist(playlistName = name, musicIds = musicIdList)
            fetchAllPlaylists()
            clearSelection()
        }
    }

//    fun addMusicToPlaylist(playlist: Playlist, music: Music) {
//        viewModelScope.launch(Dispatchers.IO) {
//            repository.addMusicToPlaylist(playlist, music)
//        }
//    }


    fun addMusicListToPlaylist(playlistId: Int, musicIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addMusicListToPlaylist(playlistId, musicIds)
        }
    }

    private fun fetchAllPlaylists() {
        viewModelScope.launch(Dispatchers.IO) {
            when (val dataResult = repository.getAllPlaylist()) {
                is DataResult.Success -> {
                    _playlists.value = dataResult.data
                    println("VM PLAYLIST POSTED VALUE")
                }

                is DataResult.Error -> {
                    // _uiState.postValue(dataResult.exception.message?.let { UIState.Error(it) })
                }

                else -> {}
            }
        }
    }


    fun removeMusicFromPlaylist(playlist: Playlist, music: Music) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeMusicFromPlaylist(playlist, music)
        }
    }

    fun toggleMusicSelection(musicId: Long) {
        _selectedMusicIds.value = _selectedMusicIds.value.toMutableSet().apply {
            if (contains(musicId)) remove(musicId) else add(musicId)
        }
    }

    fun clearSelection() {
        _selectedMusicIds.value = emptySet()
    }

    fun addSelectedMusicToPlaylist(playlistId: Int) {
        addMusicListToPlaylist(playlistId, _selectedMusicIds.value.toList())
        clearSelection()
    }


    fun playPlaylist(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            val musicList = repository.getMusicListFromIds(playlist.musicIds)
            _currSelectedPlaylistMusic.value = musicList
            if (musicList.isNotEmpty()) _currSelectedMusic.value = musicList[0]

            // set music list to exoplayer
            withContext(Dispatchers.Main) {
                exoPlayer.clearMediaItems()
                musicPlayerHandler.setMusicPlaylist(musicList)

            }
        }
    }

    fun resetPlaylistSelection() {
        _currSelectedPlaylistMusic.value = emptyList()
        mExoPlayer.clearMediaItems()

        _currSelectedMusic.value = appCurrentPlayList.value.first()
        musicPlayerHandler.setMusicPlaylist(appCurrentPlayList.value)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            musicPlayerHandler.handlePlayerState(ZPlayerState.Stop)
        }

        musicPlayerHandler.release()
    }


//    fun playMusic(newMusicIndex: Int) {
//        playbackPair.previous = playbackPair.current
//        playbackPair.current = newMusicIndex
//
//        musicPlayerHandler.playMusic(allMusicList[playbackPair.current])
//    }
//
//    fun nextMusic() {
//        val currIndex = playbackPair.current
//        val nextIndex = if (isShuffleMode) getRandomIndex(currIndex) else currIndex + 1
//        val n = allMusicList.size
//        playbackPair.previous = playbackPair.current
//        playbackPair.current = nextIndex % n
//
//        musicPlayerHandler.playMusic(allMusicList[playbackPair.current])
//    }
//
//    fun prevMusic() {
//        val prev = playbackPair.previous
//        playbackPair.previous = playbackPair.current
//        playbackPair.current = prev
//
//        musicPlayerHandler.playMusic(allMusicList[playbackPair.current])
//    }
//
//    private fun getRandomIndex(currIndex: Int): Int {
//        return if (allMusicList.size > 1) {
//            var newIndex: Int
//            do {
//                newIndex = Random.nextInt(allMusicList.size)
//            } while (newIndex == currIndex)
//            newIndex
//        } else {
//            0
//        }
//    }
//
//
//    private fun computeProgress(currentProgress: Long) {
//        progress =
//            if (currentProgress > 0) ((currentProgress.toFloat() / duration.toFloat()) * 100f)
//            else 0f
//        progressString = formatDuration(currentProgress)
//    }
//
//    @SuppressLint("DefaultLocale")
//    fun formatDuration(duration: Long): String {
//        val minute = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
//        val seconds = (minute) - minute * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES)
//        return String.format("%02d:%02d", minute, seconds)
//    }
//
//    override fun onCleared() {
//        viewModelScope.launch {
//            musicPlayerHandler.handlePlayerState(PlayerState.Stop)
//        }
//
//        super.onCleared()
//    }

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
    data class Success(val data: List<Music>) : UIState()
    data class Error(val message: String) : UIState()
}

//
//sealed class UIState {
//    object Loading : UIState()
//    data class Success<out T>(val data: T) : UIState()
//    data class Error(val message: String) : UIState()
//}

