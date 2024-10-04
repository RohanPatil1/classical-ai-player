package com.rohan.classic_ai_player.player.service

import android.media.audiofx.LoudnessEnhancer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.rohan.classic_ai_player.data.model.Music
import com.rohan.classic_ai_player.player.normalizer.MusicNormalizer
import com.rohan.classic_ai_player.utils.MusicState
import com.rohan.classic_ai_player.utils.ZPlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


@UnstableApi
class MusicPlayerHandler(
    private val exoPlayer: ExoPlayer,
    private val coroutineScope: CoroutineScope,
    private val musicNormalizer: MusicNormalizer,
) : Player.Listener {

    private val job: Job? = null
    private val _musicPlayerState = MutableStateFlow<MusicState>(MusicState.Idle)
    val musicPlayerState = _musicPlayerState.asStateFlow()
    private val loudnessEnhancer = LoudnessEnhancer(exoPlayer.audioSessionId)


    init {
        exoPlayer.addListener(this)
        loudnessEnhancer.enabled = true
    }

    fun applyNormalization(music: Music) {
        val gain = music.audioStats?.let { musicNormalizer.calculateDynamicGain(it) } ?: 0f
        loudnessEnhancer.setTargetGain(gain.toInt())
    }


    override fun onPlaybackStateChanged(playbackState: Int) {

        when (playbackState) {
            ExoPlayer.STATE_BUFFERING -> {
                _musicPlayerState.value = MusicState.Buffering(exoPlayer.currentPosition)
            }

            ExoPlayer.STATE_READY -> {
                _musicPlayerState.value = MusicState.Ready(exoPlayer.duration)
            }

            Player.STATE_ENDED -> {
            }

            Player.STATE_IDLE -> {
                _musicPlayerState.value = MusicState.Idle
            }
        }

        super.onPlaybackStateChanged(playbackState)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {

        _musicPlayerState.value = MusicState.Playing(isPlaying = isPlaying)
        _musicPlayerState.value = MusicState.CurrentPlaying(exoPlayer.currentMediaItemIndex)

        if (isPlaying) {
            coroutineScope.launch(Dispatchers.Main) {
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


    fun setMusicPlaylist(musicItemList: List<Music>) {
        var mediaItemList = emptyList<MediaItem>()
        mediaItemList = musicItemList.map { music: Music ->
            music.toMediaItem()
        }
        exoPlayer.setMediaItems(mediaItemList)
        exoPlayer.seekTo(0, 0L)
        exoPlayer.prepare()
    }

    suspend fun handlePlayerState(
        playerState: ZPlayerState,
        selectedMediaIndex: Int = -1,
        seekPosition: Long = 0,
    ) {

        when (playerState) {
            ZPlayerState.Backward -> {
                exoPlayer.seekToPreviousMediaItem()
            }

            ZPlayerState.Forward -> {
                exoPlayer.seekForward()
            }

            ZPlayerState.PlayPause -> {
                playPauseMusic()
            }

            ZPlayerState.SeekTo -> {
                exoPlayer.seekTo(seekPosition)
            }

            ZPlayerState.SeekToNext -> {
                exoPlayer.seekToNextMediaItem()
            }

            ZPlayerState.SelectedMusicChange -> {
                when (selectedMediaIndex) {
                    exoPlayer.currentMediaItemIndex -> {
                        // same music
                        playPauseMusic()
                    }

                    else -> {
                        exoPlayer.seekToDefaultPosition(selectedMediaIndex)
                        _musicPlayerState.value = MusicState.Playing(isPlaying = true)
                        exoPlayer.playWhenReady = true
                        startProgressUpdate()
                    }
                }
            }

            ZPlayerState.Stop -> {
                stopProgressUpdate()
            }

            is ZPlayerState.UpdateProgress -> {
                exoPlayer.seekTo(
                    ((exoPlayer.duration * playerState.newProgress) / 100f).toLong()
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
            _musicPlayerState.value = MusicState.Playing(
                isPlaying = true
            )
            startProgressUpdate()
        }
    }


    private suspend fun startProgressUpdate() = job.run {
        while (true) {
            delay(500)
            _musicPlayerState.value = MusicState.InProgress(exoPlayer.currentPosition)
            println("HANDLER: ${exoPlayer.currentPosition}")
        }
    }

    private fun stopProgressUpdate() {
        job?.cancel()
        _musicPlayerState.value = MusicState.Playing(isPlaying = false)
    }

    private fun Music.toMediaItem() = MediaItem.Builder()
        .setUri(this.contentUri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setDisplayTitle(this.songName)
                .setArtist(this.artistName)
                .setArtworkUri(this.albumArt)
                .setTitle(this.title)
                .build()
        )
        .setMediaId(this.musicId.toString())
        .build()

    fun release() {
        exoPlayer.release()
        loudnessEnhancer.release()
        exoPlayer.removeListener(this)
    }

}