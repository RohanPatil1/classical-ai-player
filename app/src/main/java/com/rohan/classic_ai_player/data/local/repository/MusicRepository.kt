package com.rohan.classic_ai_player.data.local.repository

import android.util.Log
import com.rohan.classic_ai_player.data.local.model.MusicData
import com.rohan.classic_ai_player.data.local.source.MusicContentResolver
import com.rohan.classic_ai_player.utils.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MusicRepository @Inject constructor(
    private val musicContentResolver: MusicContentResolver //getting data from ContentResolver
) {

    suspend fun fetchMusicList(): DataResult<MusicData> {
        return try {
            withContext(Dispatchers.IO) {
                val dataList = musicContentResolver.getAudioData()
                DataResult.Success(MusicData(musicDataList = dataList, success = true))
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message.toString())
            DataResult.Error(e)
        }
    }


    companion object {
        const val TAG = "MusicRepository"
    }
}