package com.rohan.classic_ai_player.data.repository

import com.rohan.classic_ai_player.data.db.MusicDao
import com.rohan.classic_ai_player.data.db.PlaylistDao
import com.rohan.classic_ai_player.data.model.Music
import com.rohan.classic_ai_player.data.model.Playlist
import com.rohan.classic_ai_player.data.source.MusicContentResolver
import com.rohan.classic_ai_player.player.normalizer.MusicNormalizer
import javax.inject.Inject

class MusicRepository @Inject constructor(
    private val musicContentResolver: MusicContentResolver,
    private val musicDao: MusicDao,
    private val playlistDao: PlaylistDao,
    private val musicNormalizer: MusicNormalizer,
) {

    suspend fun getAllMusic(): List<Music> {
        val cachedMusicList = musicDao.getAllMusic()
        if (cachedMusicList.isNullOrEmpty()) {
            println("FRESH START")

            // fresh start
            val musicListContentResolver = musicContentResolver.fetchMusicList()
            insertAll(musicListContentResolver)
            println("FRESH DATA SENT")
            return musicListContentResolver
        } else {
            println("GIVING THE CACHED LIST")

            // return the cached list
            return cachedMusicList
        }
    }

    suspend fun getMusicListFromIds(musicIds: List<Long>): List<Music> {
        return musicDao.getMusicListFromIds(musicIds)
    }

    fun getAllPlaylist(): List<Playlist> {
        val dataList = playlistDao.getAllPlaylists()
        return dataList
    }

    suspend fun createPlaylist(playlistName: String, musicIds: List<Long> = emptyList()) {
        normalizeMusicList(musicIds)
        playlistDao.createPlaylist(Playlist(musicIds = musicIds, playlistName = playlistName))
    }

    private suspend fun normalizeMusicList(musicIds: List<Long>) {
        musicIds.forEach { id ->
            val currMusic = musicDao.getMusicById(id)
            currMusic?.contentUri?.let {
                val audioStat = musicNormalizer.analyzeAudio(it)
                musicDao.updateAudioStats(currMusic.musicId, audioStat)
            }
        }
    }


    suspend fun addMusicListToPlaylist(playlistId: Int, musicIds: List<Long>) {
        normalizeMusicList(musicIds)
        val musicIdsString = toStringForUpdate(musicIds)
        playlistDao.addMultipleMusicToPlaylist(playlistId, musicIdsString)
    }

    suspend fun removeMusicFromPlaylist(playlist: Playlist, music: Music) {
        playlistDao.removeMusicFromPlaylist(playlist.playlistId, music.musicId)
    }

    suspend fun getPlaylistById(playlistId: Int): Playlist? {
        return playlistDao.getPlaylistById(playlistId)
    }

    private fun toStringForUpdate(musicIds: List<Long>): String {
        return musicIds.distinct().joinToString(",")
    }

    private suspend fun insertAll(musicList: List<Music>) = musicDao.insertAll(musicList)

    companion object {
        const val TAG = "MusicRepository"
    }
}