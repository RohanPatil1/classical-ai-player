package com.rohan.classic_ai_player.data.repository

import android.util.Log
import com.rohan.classic_ai_player.data.db.MusicDao
import com.rohan.classic_ai_player.data.db.PlaylistDao
import com.rohan.classic_ai_player.data.model.Music
import com.rohan.classic_ai_player.data.source.MusicContentResolver
import com.rohan.classic_ai_player.utils.DataResult
import javax.inject.Inject

class MusicRepository @Inject constructor(
    private val musicContentResolver: MusicContentResolver,
    private val musicDao: MusicDao,
//    private val playlistDao: PlaylistDao,
) {


    suspend fun getAllMusic(): DataResult<List<Music>> {
        return try {
            val cachedMusicList = musicDao.getAllMusic()
            if (cachedMusicList.isNullOrEmpty()) {
                println("FRESH START")

                // fresh start
                val musicListContentResolver = musicContentResolver.fetchMusicList()
                insertAll(musicListContentResolver)
                println("FRESH DATA SENT")
                DataResult.Success(musicListContentResolver)
            } else {
                println("GIVING THE CACHED LIST")

                // return the cached list
                DataResult.Success(cachedMusicList)
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message.toString())
            DataResult.Error(e)
        }
    }

//    fun getAllMusic(): Flow<List<Music>> = flow {
//        // Get the initial list of music from the Room database
//        val initialList = musicDao.getAllMusic().firstOrNull() ?: emptyList()
//
//        if (initialList.isEmpty()) {
//            println("FIRST TIME - GETTING FROM CONTENT RESOLVER")
//            // If the database is empty, fetch music from the content resolver
//            val musicList = musicContentResolver.fetchMusicList()
//
//
//            // Emit the newly fetched music list
//            emit(musicList)
//            println("DATA EMITTED 1")
//
////            // Insert the fetched music list into the database
////            insertAll(musicList)
//        } else {
//            println("NOT FIRST TIME - GETTING FROM ROOM DAO")
//            // If the database has data, emit the initial list from Room
//            emit(initialList)
//            println("DATA EMITTED 2")
//        }
//
//        // Continue listening to the database for any future updates
//        emitAll(musicDao.getAllMusic())
//    }.flowOn(Dispatchers.IO)


    private suspend fun insertAll(musicList: List<Music>) = musicDao.insertAll(musicList)


//    suspend fun getMusicById(id: Long): Music? = musicDao.getMusicById(id)
//
//    suspend fun updateAudioStats(id: Long, audioStats: AudioStats) =
//        musicDao.updateAudioStats(id, audioStats)
//
//    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()
//
//    suspend fun createPlaylist(playlist: Playlist) = playlistDao.createPlaylist(playlist)
//
//    suspend fun getPlaylistById(playlistId: Int): Playlist? =
//        playlistDao.getPlaylistById(playlistId)

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