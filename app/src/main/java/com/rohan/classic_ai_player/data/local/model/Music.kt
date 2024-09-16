package com.rohan.classic_ai_player.data.local.model

import android.net.Uri
import kotlinx.parcelize.Parcelize
import android.os.Parcelable


@Parcelize
data class Music(
    val id: Long,
    val name: String,
    val uri: Uri,
    val artist: String,
    val metaData: String,
    val duration: Int,
    val gainRequired: Float
) : Parcelable
