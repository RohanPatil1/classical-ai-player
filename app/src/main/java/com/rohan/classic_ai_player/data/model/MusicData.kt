package com.rohan.classic_ai_player.data.model
import com.google.gson.annotations.SerializedName

data class MusicData(
    @SerializedName("data")  val musicDataList: List<Music>,
    val success: Boolean
)