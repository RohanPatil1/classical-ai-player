package com.rohan.classic_ai_player.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rohan.classic_ai_player.data.model.Playlist
import kotlinx.coroutines.flow.Flow


@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaylist(playlist: Playlist)

    @Query("SELECT * FROM playlist_table")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlist_table WHERE playlistId = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: Int): Playlist?
}