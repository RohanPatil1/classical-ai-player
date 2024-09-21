package com.rohan.classic_ai_player.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rohan.classic_ai_player.data.model.Converters
import com.rohan.classic_ai_player.data.model.Music
import com.rohan.classic_ai_player.data.model.Playlist

@Database(entities = [Music::class, Playlist::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao
    abstract fun playlistDao(): PlaylistDao
}