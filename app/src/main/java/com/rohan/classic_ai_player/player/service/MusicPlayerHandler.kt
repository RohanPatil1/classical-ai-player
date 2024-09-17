package com.rohan.classic_ai_player.player.service

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.rohan.classic_ai_player.utils.MusicState
import com.rohan.classic_ai_player.utils.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicPlayerHandler @Inject constructor(
    private val exoPlayer: ExoPlayer,
) : Player.Listener {


    private val _musicStateFlow: MutableStateFlow<MusicState> = MutableStateFlow(MusicState.Idle)
    val musicState: StateFlow<MusicState> = _musicStateFlow.asStateFlow()

    private var job: Job? = null


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
            GlobalScope.launch(Dispatchers.IO) {
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

}