package com.rohan.classic_ai_player.data.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.rohan.classic_ai_player.data.db.MusicDao
import com.rohan.classic_ai_player.data.db.MusicDatabase
import com.rohan.classic_ai_player.data.db.PlaylistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(application: Application): MusicDatabase =
        Room.databaseBuilder(application, MusicDatabase::class.java, "classicMusicDatabase")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideMusicDao(db: MusicDatabase): MusicDao = db.musicDao()


    @Provides
    @Singleton
    fun providePlaylistDao(db: MusicDatabase): PlaylistDao = db.playlistDao()

    @Provides
    @Singleton
    fun getMyApplicationContext(@ApplicationContext context: Context): Context = context

}