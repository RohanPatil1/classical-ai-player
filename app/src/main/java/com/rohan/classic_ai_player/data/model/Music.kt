package com.rohan.classic_ai_player.data.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey


//@Parcelize
//data class Music(
//    val id: Long,
//    val name: String,
//    val uri: Uri,
//    val artist: String,
//    val metaData: String,
//    val duration: Int,
//    val gainRequired: Float
//) : Parcelable


@Entity(tableName = "music_table")
data class Music(
    @PrimaryKey(autoGenerate = true) val musicId: Long = 0,
    val contentUri: Uri,
    val artistName: String,
    val songName: String,
    val title: String,
    val albumArt: Uri,
    val duration: Int,
    val audioStats: AudioStats? = null,
)

data class AudioStats(
    val integratedLoudness: Float,
    val loudnessRange: Float,
    val truePeak: Float,
)