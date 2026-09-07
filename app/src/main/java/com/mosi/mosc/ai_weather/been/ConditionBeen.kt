package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ConditionBeen(
    val condition: String,
    val conditionId: String,
    val humidity: String,
    val pressure: String,
    val realFeel: String,
    val updateTime: String,
    val temp: String,
    val vis: String,
    val uvi: String,
    val windSpeed: String,
    val windDir: WindDirBeen,
    val windLevel: String,
    val windDegrees: String,
    val sunRise: Long,
    val sunSet: Long,
    val precipitation: String?,
    val tips: String?,
    val windGustLevel: String?,
    val windGustSpeed: String?,
    val windGustDir: String?,
    val cloudCover: String?,
    val pressureTendency: String?,
    val icon: String?,
): Parcelable
