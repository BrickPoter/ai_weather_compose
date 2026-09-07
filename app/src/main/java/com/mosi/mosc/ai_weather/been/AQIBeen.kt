package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AQIBeen(
    val updateTime: Long,
    val no2: String,
    val o3: String,
    val pm10: String,
    val pm25: String,
    val so2C: String,
    val aqiValue: String,
    val aqiValueText: String,
    val cityName: String,
    val rank: String,
    val coC: String,
    val no2C: String,
    val o3C: String,
    val pm10C: String,
    val pm25C: String,
    val so2: String,
    val lv: String?,
    val aqiDesc: String?,
):Parcelable