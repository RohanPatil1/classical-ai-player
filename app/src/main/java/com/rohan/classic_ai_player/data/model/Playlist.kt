package com.rohan.classic_ai_player.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters


@Entity(tableName = "playlist_table")
@TypeConverters(MusicIdsConverter::class)
data class Playlist(
    @PrimaryKey(autoGenerate = true) val playlistId: Int = 0,
    val playlistName: String,
    val musicIds: List<Long> = emptyList(), // Store the list of music IDs
)

class MusicIdsConverter {
    @TypeConverter
    fun fromString(value: String): List<Long> {
        return value.split(",").map { it.toLong() }
    }

    @TypeConverter
    fun toString(musicIds: List<Long>): String {
        return musicIds.joinToString(",")
    }

}