package com.rohan.classic_ai_player.data.model

import androidx.room.TypeConverter
import com.google.gson.Gson

class Converters {

    @TypeConverter
    fun fromAudioStats(audioStats: AudioStats?): String? {
        return audioStats?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun toAudioStats(audioStatsString: String?): AudioStats? {
        return audioStatsString?.let { Gson().fromJson(it, AudioStats::class.java) }
    }

    @TypeConverter
    fun fromMusicIds(musicIds: List<Int>): String {
        return musicIds.joinToString(separator = ",")
    }

    @TypeConverter
    fun toMusicIds(data: String): List<Int> {
        return if (data.isEmpty()) {
            emptyList()
        } else {
            data.split(",").map { it.toInt() }
        }
    }
}