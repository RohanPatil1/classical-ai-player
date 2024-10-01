package com.rohan.classic_ai_player.player.service

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.rohan.classic_ai_player.data.model.Music
import com.rohan.classic_ai_player.utils.MusicState
import com.rohan.classic_ai_player.utils.ZPlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class MusicPlayerHandler(
    private val exoPlayer: ExoPlayer,
    private val coroutineScope: CoroutineScope,
) : Player.Listener {

    init {
        exoPlayer.addListener(this)
    }

    private val _musicPlayerState = MutableStateFlow<MusicState>(MusicState.Idle)
    val musicPlayerState = _musicPlayerState.asStateFlow()

    private val job: Job? = null

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

//    suspend fun currentPosition(position: (Long) -> Unit) {
//        job.run {
//            while (true) {
//                position(exoPlayer.currentPosition)
//                delay(1000)
//            }
//        }
//    }

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
        exoPlayer.removeListener(this)
    }
//    private var progressUpdateJob: Job? = null
//
//    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
//    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
//
//    private val _currentTrack = MutableStateFlow<MediaItem?>(null)
//    val currentTrack: StateFlow<MediaItem?> = _currentTrack.asStateFlow()
//
//    private val _playlist = MutableStateFlow<List<MediaItem>>(emptyList())
//    val playlist: StateFlow<List<MediaItem>> = _playlist.asStateFlow()
//
//    private val _isShuffleOn = MutableStateFlow(false)
//    val isShuffleOn: StateFlow<Boolean> = _isShuffleOn.asStateFlow()
//
//    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
//    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()
//
//    init {
//        exoPlayer.addListener(this)
//    }
//
//    fun setPlaylist(musicList: List<Music>) {
//        val mediaItems = musicList.map { it.toMediaItem() }
//        _playlist.value = mediaItems
//        exoPlayer.setMediaItems(mediaItems)
//        exoPlayer.prepare()
//    }
//
//    fun playMusic(music: Music) {
//        val mediaItem = music.toMediaItem()
//        exoPlayer.setMediaItem(mediaItem)
//        exoPlayer.prepare()
//        _currentTrack.value = mediaItem
//        play()
//    }
//
//    fun play() {
//        exoPlayer.play()
//        startProgressUpdate()
//    }
//
//    fun pause() {
//        exoPlayer.pause()
//        stopProgressUpdate()
//    }
//
//    fun skipToNext() {
//        exoPlayer.seekToNext()
//        val ds = exoPlayer.mediaItemCount
//        updateCurrentTrack()
//    }
//
//    fun skipToPrevious() {
//        exoPlayer.seekToPreviousMediaItem()
//        updateCurrentTrack()
//    }
//
//    fun seekTo(position: Long) {
//        exoPlayer.seekTo(position)
//        updateCurrentTrack()
//    }
//
//    fun toggleShuffle() {
//        _isShuffleOn.value = !_isShuffleOn.value
//        exoPlayer.shuffleModeEnabled = _isShuffleOn.value
//    }
//
//    fun setRepeatMode(repeatMode: Int) {
//        _repeatMode.value = repeatMode
//        exoPlayer.repeatMode = repeatMode
//    }
//
//
//    private fun startProgressUpdate() {
//        progressUpdateJob?.cancel()
//        progressUpdateJob = coroutineScope.launch(Dispatchers.Main) {
//            while (true) {
//                _playerState.value =
//                    PlayerState.Playing(exoPlayer.currentPosition, exoPlayer.duration)
//                delay(1000) // Update every second
//            }
//        }
//    }
//
//    private fun stopProgressUpdate() {
//        progressUpdateJob?.cancel()
//    }
//
//    fun release() {
//        stopProgressUpdate()
//        exoPlayer.release()
//    }
//
//    private fun Music.toMediaItem() = MediaItem.Builder()
//        .setUri(this.contentUri)
//        .setMediaMetadata(
//            MediaMetadata.Builder()
//                .setAlbumArtist(this.artistName)
//                .setDisplayTitle(this.songName)
//                .setSubtitle(this.albumName)
//                .build()
//        )
//        .setMediaId(this.musicId.toString())
//        .build()
//
//
//
//    private fun updatePlayerState() {
//        _playerState.value = when {
//            exoPlayer.isPlaying -> PlayerState.Playing(exoPlayer.currentPosition, exoPlayer.duration)
//            exoPlayer.playbackState == Player.STATE_ENDED -> PlayerState.Ended
//            else -> PlayerState.Paused(exoPlayer.currentPosition, exoPlayer.duration)
//        }
//    }
//
//    private fun updateCurrentTrack() {
//        _currentTrack.value = exoPlayer.currentMediaItem
//        _playerState.value = PlayerState.Playing(exoPlayer.currentPosition, exoPlayer.duration)
//    }
//
//    override fun onPlaybackStateChanged(playbackState: Int) {
//        updatePlayerState()
//    }
//
//    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
//        _currentTrack.value = mediaItem
//    }

//    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
//    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
//
//    private val _currentTrack = MutableStateFlow<MediaItem?>(null)
//    val currentTrack: StateFlow<MediaItem?> = _currentTrack.asStateFlow()
//
//    private val _playlist = MutableStateFlow<List<MediaItem>>(emptyList())
//    val playlist: StateFlow<List<MediaItem>> = _playlist.asStateFlow()

//
//    init {
//        exoPlayer.addListener(this)
//    }
//
//
//    fun playMusic(music: Music) {
//        val mediaItem = music.toMediaItem()
//        exoPlayer.setMediaItem(mediaItem)
//        exoPlayer.prepare()
//
//        play()
//    }
//
//    private fun Music.toMediaItem() = MediaItem.Builder()
//        .setUri(this.contentUri)
//        .setMediaMetadata(
//            MediaMetadata.Builder()
//                .setAlbumArtist(this.artistName)
//                .setDisplayTitle(this.songName)
//                .setSubtitle(this.albumName)
//                .build()
//        )
//        .setMediaId(this.musicId.toString())
//        .build()
//
//
//    fun prepareMusicToPlayList(musicList: List<Music>): List<MediaItem> {
//        if (musicList.isNotEmpty()) {
//            val mediaItems = musicList.map { music ->
//                MediaItem.Builder()
//                    .setUri(music.contentUri)
//                    .setMediaMetadata(
//                        MediaMetadata.Builder()
//                            .setAlbumArtist(music.artistName)
//                            .setDisplayTitle(music.songName)
//                            .setSubtitle(music.albumName)
//                            .build()
//                    )
//                    .setMediaId(music.musicId.toString())
//                    .build()
//            }
//            _playlist.value = mediaItems
//            exoPlayer.setMediaItems(mediaItems)
//            exoPlayer.prepare()
//
//            return mediaItems
//        }
//
//        return emptyList()
//    }
//
//    fun play() {
//        exoPlayer.play()
//        startProgressUpdate()
//    }
//
//    fun pause() {
//        exoPlayer.pause()
//        stopProgressUpdate()
//    }
//
//    fun playPause() {
//        if (exoPlayer.isPlaying) pause() else play()
//    }
//
//    fun seekTo(position: Long) {
//        exoPlayer.seekTo(position)
//    }
//
//    fun skipToNext() {
//        exoPlayer.seekToNextMediaItem()
//    }
//
//    fun skipToPrevious() {
//        exoPlayer.seekToPreviousMediaItem()
//    }
//
//    fun skipToIndex(index: Int) {
//        if (index in 0 until exoPlayer.mediaItemCount) {
//            exoPlayer.seekTo(index, 0)
//        }
//    }
//
//    fun setRepeatMode(repeatMode: Int) {
//        exoPlayer.repeatMode = repeatMode
//    }
//
//    fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
//        exoPlayer.shuffleModeEnabled = shuffleModeEnabled
//    }
//
//
//    suspend fun handlePlayerState(
//        playerState: PlayerState,
//        selectedMediaIndex: Int = -1,
//        seekPosition: Long = 0,
//    ) {
//
//        when (playerState) {
//            PlayerState.Backward -> {
//                exoPlayer.seekBack()
//            }
//
//            PlayerState.Forward -> {
//                exoPlayer.seekForward()
//            }
//
//            PlayerState.PlayPause -> {
//                playPauseMusic()
//            }
//
//            PlayerState.SeekTo -> {
//                exoPlayer.seekTo(seekPosition)
//            }
//
//            PlayerState.SeekToNext -> {
//                exoPlayer.seekToNext()
//            }
//
//            PlayerState.SelectedMusicChange -> {
//                when (selectedMediaIndex) {
//                    exoPlayer.currentMediaItemIndex -> {
//                        // same music
//                        playPauseMusic()
//                    }
//
//                    else -> {
//                        exoPlayer.seekToDefaultPosition(selectedMediaIndex)
//                        _musicStateFlow.value = MusicState.Playing(isPlaying = true)
//                        exoPlayer.playWhenReady = true
//                        startProgressUpdate()
//                    }
//                }
//            }
//
//            PlayerState.Stop -> {
//                stopProgressUpdate()
//            }
//
//            is PlayerState.UpdateProgress -> {
//                exoPlayer.seekTo(
//                    (exoPlayer.duration * playerState.newProgress).toLong()
//                )
//            }
//        }
//
//    }
//
//
//    override fun onPlaybackStateChanged(playbackState: Int) {
//        _playerState.value = when (playbackState) {
//            Player.STATE_IDLE -> PlayerState.Idle
//            Player.STATE_BUFFERING -> PlayerState.Buffering
//            Player.STATE_READY -> if (exoPlayer.playWhenReady) PlayerState.Playing(
//                exoPlayer.currentPosition,
//                exoPlayer.duration
//            ) else PlayerState.Paused(
//                exoPlayer.currentPosition,
//                exoPlayer.duration
//            )
//
//            Player.STATE_ENDED -> PlayerState.Ended
//            else -> PlayerState.Error("Unknown playback state")
//        }
//    }
//
//    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
//        _currentTrack.value = mediaItem
//    }
//

//
//    sealed class PlayerState {
//        object Idle : PlayerState()
//        object Buffering : PlayerState()
//        data class Playing(val currentPosition: Long, val duration: Long) : PlayerState()
//        data class Paused(val currentPosition: Long, val duration: Long) : PlayerState()
//        object Ended : PlayerState()
//        data class Error(val errorMessage: String) : PlayerState()
//    }

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