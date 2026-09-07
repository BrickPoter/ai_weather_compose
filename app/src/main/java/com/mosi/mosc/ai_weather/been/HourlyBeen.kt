package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HourlyBeen(
    val condition: String,
    val conditionId: String,
    val date: Long,
    val temp: String,
    val pop: String,
    val windDir: WindDirBeen,
    val humidity: String,
    val pressure: String,
    val qpf: String,
    val realFeel: String,
    val snow: String,
    val updateTime: Long,
    val uvi: String,
    val windDegrees: String,
    val windSpeed: String,
    val windLevel: String,
    val wp: String,
    val dayNight: Boolean,
    val iconDay: String,
    val iconNight: String,
): Parcelable