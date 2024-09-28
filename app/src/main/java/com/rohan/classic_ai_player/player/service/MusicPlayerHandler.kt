package com.rohan.classic_ai_player.player.service

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.rohan.classic_ai_player.data.model.Music
import com.rohan.classic_ai_player.utils.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicPlayerHandler(
    private val exoPlayer: ExoPlayer,
    private val coroutineScope: CoroutineScope,
) : Player.Listener {

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _currentTrack = MutableStateFlow<MediaItem?>(null)
    val currentTrack: StateFlow<MediaItem?> = _currentTrack.asStateFlow()

    private val _playlist = MutableStateFlow<List<MediaItem>>(emptyList())
    val playlist: StateFlow<List<MediaItem>> = _playlist.asStateFlow()

    private var progressUpdateJob: Job? = null

    init {
        exoPlayer.addListener(this)
    }


    fun playMusic(music: Music) {
        val mediaItem = music.toMediaItem()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        play()
    }

    private fun Music.toMediaItem() = MediaItem.Builder()
        .setUri(this.contentUri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setAlbumArtist(this.artistName)
                .setDisplayTitle(this.songName)
                .setSubtitle(this.albumName)
                .build()
        )
        .setMediaId(this.musicId.toString())
        .build()


    fun prepareMusicToPlayList(musicList: List<Music>): List<MediaItem> {
        if (musicList.isNotEmpty()) {
            val mediaItems = musicList.map { music ->
                MediaItem.Builder()
                    .setUri(music.contentUri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setAlbumArtist(music.artistName)
                            .setDisplayTitle(music.songName)
                            .setSubtitle(music.albumName)
                            .build()
                    )
                    .setMediaId(music.musicId.toString())
                    .build()
            }
            _playlist.value = mediaItems
            exoPlayer.setMediaItems(mediaItems)
            exoPlayer.prepare()

            return mediaItems
        }

        return emptyList()
    }

    fun play() {
        exoPlayer.play()
        startProgressUpdate()
    }

    fun pause() {
        exoPlayer.pause()
        stopProgressUpdate()
    }

    fun playPause() {
        if (exoPlayer.isPlaying) pause() else play()
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    fun skipToNext() {
        exoPlayer.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        exoPlayer.seekToPreviousMediaItem()
    }

    fun skipToIndex(index: Int) {
        if (index in 0 until exoPlayer.mediaItemCount) {
            exoPlayer.seekTo(index, 0)
        }
    }

    fun setRepeatMode(repeatMode: Int) {
        exoPlayer.repeatMode = repeatMode
    }

    fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        exoPlayer.shuffleModeEnabled = shuffleModeEnabled
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        _playerState.value = when (playbackState) {
            Player.STATE_IDLE -> PlayerState.Idle
            Player.STATE_BUFFERING -> PlayerState.Buffering
            Player.STATE_READY -> if (exoPlayer.playWhenReady) PlayerState.Playing(
                exoPlayer.currentPosition,
                exoPlayer.duration
            ) else PlayerState.Paused(
                exoPlayer.currentPosition,
                exoPlayer.duration
            )

            Player.STATE_ENDED -> PlayerState.Ended
            else -> PlayerState.Error("Unknown playback state")
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        _currentTrack.value = mediaItem
    }

    private fun startProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = coroutineScope.launch(Dispatchers.Main) {
            while (true) {
                _playerState.value =
                    PlayerState.Playing(exoPlayer.currentPosition, exoPlayer.duration)
                delay(1000) // Update every second
            }
        }
    }

    private fun stopProgressUpdate() {
        progressUpdateJob?.cancel()
    }

    fun release() {
        stopProgressUpdate()
        exoPlayer.release()
    }

    sealed class PlayerState {
        object Idle : PlayerState()
        object Buffering : PlayerState()
        data class Playing(val currentPosition: Long, val duration: Long) : PlayerState()
        data class Paused(val currentPosition: Long, val duration: Long) : PlayerState()
        object Ended : PlayerState()
        data class Error(val errorMessage: String) : PlayerState()
    }

    /*
        private val _musicStateFlow: MutableStateFlow<MusicState> = MutableStateFlow(MusicState.Idle)
        val musicState: StateFlow<MusicState> = _musicStateFlow.asStateFlow()

        private var job: Job? = null

        init {

            exoPlayer.addListener(this)

        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                ExoPlayer.STATE_BUFFERING -> {
                    _musicStateFlow.value = MusicState.Buffering(exoPlayer.currentPosition)
                }

                ExoPlayer.STATE_READY -> {
                    _musicStateFlow.value = MusicState.Ready(exoPlayer.duration)
                }

                Player.STATE_ENDED -> {
                }

                Player.STATE_IDLE -> {
                }
            }

            super.onPlaybackStateChanged(playbackState)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _musicStateFlow.value = MusicState.Playing(isPlaying = isPlaying)
            _musicStateFlow.value = MusicState.CurrentPlaying(exoPlayer.currentMediaItemIndex)

            if (isPlaying) {
                GlobalScope.launch(Dispatchers.Main) {
                    startProgressUpdate()
                }
            } else {
                stopProgressUpdate()
            }
            super.onIsPlayingChanged(isPlaying)
        }


        fun setMusic(mediaItem: MediaItem) {
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }

        fun setMusicPlaylist(mediaList: List<MediaItem>) {
            exoPlayer.setMediaItems(mediaList)
            exoPlayer.prepare()
        }

        suspend fun handlePlayerState(
            playerState: PlayerState,
            selectedMediaIndex: Int = -1,
            seekPosition: Long = 0,
        ) {

            when (playerState) {
                PlayerState.Backward -> {
                    exoPlayer.seekBack()
                }

                PlayerState.Forward -> {
                    exoPlayer.seekForward()
                }

                PlayerState.PlayPause -> {
                    playPauseMusic()
                }

                PlayerState.SeekTo -> {
                    exoPlayer.seekTo(seekPosition)
                }

                PlayerState.SeekToNext -> {
                    exoPlayer.seekToNext()
                }

                PlayerState.SelectedMusicChange -> {
                    when (selectedMediaIndex) {
                        exoPlayer.currentMediaItemIndex -> {
                            // same music
                            playPauseMusic()
                        }

                        else -> {
                            exoPlayer.seekToDefaultPosition(selectedMediaIndex)
                            _musicStateFlow.value = MusicState.Playing(isPlaying = true)
                            exoPlayer.playWhenReady = true
                            startProgressUpdate()
                        }
                    }
                }

                PlayerState.Stop -> {
                    stopProgressUpdate()
                }

                is PlayerState.UpdateProgress -> {
                    exoPlayer.seekTo(
                        (exoPlayer.duration * playerState.newProgress).toLong()
                    )
                }
            }

        }


        private suspend fun playPauseMusic() {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
                stopProgressUpdate()
            } else {
                exoPlayer.play()
                _musicStateFlow.value = MusicState.Playing(
                    isPlaying = true
                )
                startProgressUpdate()
            }
        }

        private suspend fun startProgressUpdate() = job.run {
            while (true) {
                delay(500)
                _musicStateFlow.value = MusicState.InProgress(exoPlayer.currentPosition)
            }
        }

        private fun stopProgressUpdate() {
            job?.cancel()
            _musicStateFlow.value = MusicState.Playing(isPlaying = false)
        }

        sealed class MusicState {
            object Idle : MusicState()
            data class Ready(val duration: Long) : MusicState()
            data class InProgress(val progress: Long) : MusicState()
            data class Buffering(val progress: Long) : MusicState()
            data class Playing(val isPlaying: Boolean) : MusicState()
            data class CurrentPlaying(val itemIndex: Int) : MusicState()
        }*/

}