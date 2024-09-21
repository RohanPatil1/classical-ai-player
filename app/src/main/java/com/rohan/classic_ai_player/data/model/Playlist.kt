package com.rohan.classic_ai_player.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "playlist_table")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val playlistId: Int = 0,
    val playlistName: String,
    val musicIds: List<Int> = emptyList(), // Store the list of music IDs
)