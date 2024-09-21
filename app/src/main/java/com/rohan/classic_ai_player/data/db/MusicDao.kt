package com.rohan.classic_ai_player.data.db


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rohan.classic_ai_player.data.model.AudioStats
import com.rohan.classic_ai_player.data.model.Music
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(musicList: List<Music>)

    @Query("SELECT * FROM music_table")
    fun getAllMusic(): Flow<List<Music>>

    @Query("SELECT * FROM music_table WHERE id = :id LIMIT 1")
    suspend fun getMusicById(id: Int): Music?

    @Query("UPDATE music_table SET audioStats = :audioStats WHERE id = :id")
    suspend fun updateAudioStats(id: Int, audioStats: AudioStats)
}