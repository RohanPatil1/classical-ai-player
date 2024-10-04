@file:OptIn(SavedStateHandleSaveableApi::class)

package com.rohan.classic_ai_player.ui.view_model

import android.annotation.SuppressLint
import androidx.compose.runtime.mutableLongStateOf
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


    private val musicPlayerHandler: MusicPlayerHandler =
        MusicPlayerHandler(exoPlayer, viewModelScope, musicNormalizer)

    private val _appCurrentPlayList = MutableStateFlow<List<Music>>(listOf())
    val appCurrentPlayList = _appCurrentPlayList.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists = _playlists.asStateFlow()

    private val _progress = MutableStateFlow<Float>(0f)
    val progress = _progress.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    var duration by savedStateHandle.saveable { mutableLongStateOf(0L) }

    private val _currSelectedMusic = MutableStateFlow<Music?>(null)
    val currSelectedMusic = _currSelectedMusic.asStateFlow()

    private val _currSelectedPlaylistMusic = MutableStateFlow<List<Music>>(emptyList())
    val currSelectedPlaylistMusic = _currSelectedPlaylistMusic.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _selectedMusicIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedMusicIds = _selectedMusicIds.asStateFlow()


    private var isMusicListLoaded = false

    private val _exoPlayer = exoPlayer
    private val mExoPlayer = _exoPlayer

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
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {

            val dataResult = repository.getAllMusic()
            _appCurrentPlayList.value = dataResult

            isMusicListLoaded = _appCurrentPlayList.value.isNotEmpty()
            _isLoading.value = false
            withContext(Dispatchers.Main) {
                setMediaItemList(_appCurrentPlayList.value)

                if (isMusicListLoaded) _currSelectedMusic.value = dataResult[0]

            }
            println("VM POSTED VALUE")
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
            PlayerUiEvents.Backward -> {}

            PlayerUiEvents.Forward -> musicPlayerHandler.handlePlayerState(ZPlayerState.Forward)

            PlayerUiEvents.SeekToNext -> {}

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
                val n: Int
                if (currSelectedPlaylistMusic.value.isNotEmpty()) {
                    // current item from playlist
                    n = currSelectedPlaylistMusic.value.size
                    _currSelectedMusic.value = currSelectedPlaylistMusic.value[uiEvents.index % n]
                } else {
                    n = appCurrentPlayList.value.size
                    _currSelectedMusic.value = appCurrentPlayList.value[uiEvents.index % n]
                }
                currSelectedMusic.value?.let { musicPlayerHandler.applyNormalization(it) }
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
            clearSelection()
            repository.createPlaylist(playlistName = name, musicIds = musicIdList)
            fetchAllPlaylists()
        }
    }


    private fun addMusicListToPlaylist(playlistId: Int, musicIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addMusicListToPlaylist(playlistId, musicIds)
        }
    }

    private fun fetchAllPlaylists() {
        viewModelScope.launch(Dispatchers.IO) {
            val dataResult = repository.getAllPlaylist()
            _playlists.value = dataResult
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

}