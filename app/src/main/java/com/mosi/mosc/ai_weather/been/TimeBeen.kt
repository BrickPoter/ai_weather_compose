package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TimeBeen(
    val role: String,
    val expression: String,
    val granularity: String,
    val startTime: String,
    val endTime: String
): Parcelable
