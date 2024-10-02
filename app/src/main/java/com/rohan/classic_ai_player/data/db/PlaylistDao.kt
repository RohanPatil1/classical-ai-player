package com.rohan.classic_ai_player.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rohan.classic_ai_player.data.model.Playlist


@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaylist(playlist: Playlist)

    @Query("SELECT * FROM playlist_table")
    fun getAllPlaylists(): List<Playlist>

    @Query("SELECT * FROM playlist_table WHERE playlistId = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: Int): Playlist?

    @Query("UPDATE playlist_table SET musicIds = musicIds || ',' || :musicId WHERE playlistId = :playlistId")
    suspend fun addMusicToPlaylist(playlistId: Int, musicId: Long)

    @Query("UPDATE playlist_table SET musicIds = REPLACE(musicIds, :musicId || ',', '') WHERE playlistId = :playlistId")
    suspend fun removeMusicFromPlaylist(playlistId: Int, musicId: Long)

    @Query(
        "UPDATE playlist_table SET musicIds = CASE " +
                "WHEN musicIds = '' THEN :newMusicIds " +
                "ELSE musicIds || ',' || :newMusicIds END " +
                "WHERE playlistId = :playlistId"
    )
    suspend fun addMultipleMusicToPlaylist(playlistId: Int, newMusicIds: String)
}