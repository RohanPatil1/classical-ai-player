package com.rohan.classic_ai_player.data.repository

import com.rohan.classic_ai_player.data.db.MusicDao
import com.rohan.classic_ai_player.data.db.PlaylistDao
import com.rohan.classic_ai_player.data.model.AudioStats
import com.rohan.classic_ai_player.data.model.Music
import com.rohan.classic_ai_player.data.model.Playlist
import com.rohan.classic_ai_player.data.source.MusicContentResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MusicRepository @Inject constructor(
    private val musicContentResolver: MusicContentResolver,
    private val musicDao: MusicDao,
    private val playlistDao: PlaylistDao,
) {

    fun getAllMusic(): Flow<List<Music>> = flow {

        // First check if the database is empty
        val initialList = musicDao.getAllMusic().first()

        if (initialList.isEmpty()) {

            // If empty, fetch from content resolver
            val musicList = musicContentResolver.fetchMusicList()

            // Insert into database
            insertAll(musicList)

            // Emit the fetched list
            emit(musicList)
        } else {

            // If not empty, emit the initial list
            emit(initialList)
        }

        // After initial emit, collect and emit any future changes from the database
        emitAll(musicDao.getAllMusic())
    }


    private suspend fun insertAll(musicList: List<Music>) = musicDao.insertAll(musicList)

    suspend fun getMusicById(id: Long): Music? = musicDao.getMusicById(id)

    suspend fun updateAudioStats(id: Long, audioStats: AudioStats) =
        musicDao.updateAudioStats(id, audioStats)

    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    suspend fun createPlaylist(playlist: Playlist) = playlistDao.createPlaylist(playlist)

    suspend fun getPlaylistById(playlistId: Int): Playlist? =
        playlistDao.getPlaylistById(playlistId)

//    suspend fun fetchMusicList(): DataResult<MusicData> {
//        return try {
//            withContext(Dispatchers.IO) {
//                val dataList = musicContentResolver.getAudioData()
//                DataResult.Success(MusicData(musicDataList = dataList, success = true))
//            }
//        } catch (e: Exception) {
//            Log.d(TAG, e.message.toString())
//            DataResult.Error(e)
//        }
//    }


    companion object {
        const val TAG = "MusicRepository"
    }
}