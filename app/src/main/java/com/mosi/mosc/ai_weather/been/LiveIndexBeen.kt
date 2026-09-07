package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LiveIndexBeen(
    val code:WindDirBeen,
    val level: String,
    val day: Long,
    val updateTime: Long,
    val status: String,
    val desc: String,
    val content: String?,
): Parcelable