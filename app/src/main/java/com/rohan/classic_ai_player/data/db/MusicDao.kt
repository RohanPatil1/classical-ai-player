package com.rohan.classic_ai_player.data.db


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rohan.classic_ai_player.data.model.AudioStats
import com.rohan.classic_ai_player.data.model.Music

@Dao
interface MusicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(musicList: List<Music>)

    @Query("SELECT * FROM music_table")
    fun getAllMusic(): List<Music>?

    @Query("SELECT * FROM music_table WHERE musicId = :id LIMIT 1")
    suspend fun getMusicById(id: Long): Music?

    @Query("UPDATE music_table SET audioStats = :audioStats WHERE musicId = :id")
    suspend fun updateAudioStats(id: Long, audioStats: AudioStats)

    @Query("SELECT * FROM music_table WHERE musicId IN (:musicIds)")
    suspend fun getMusicListFromIds(musicIds: List<Long>): List<Music>
}