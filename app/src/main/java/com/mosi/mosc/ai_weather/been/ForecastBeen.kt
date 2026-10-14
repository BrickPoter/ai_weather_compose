package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ForecastBeen(
    //白天天气现象
    val conditionDay: String,
    val conditionIdDay: String,
    //夜晚天气现象
    val conditionNight: String,
    val conditionIdNight: String,
    val moonset: String,
    val moonrise: String,
    val moonPhase: String,
    val sunrise: Long,
    val sunset: Long,
    val predictDate: Long,
    // 白天风信息
    val windDirDay: WindDirBeen,
    // 白天风速
    val windSpeedDay: String,
    // 白天风等级
    val windLevelDay: String,
    // 夜晚风信息
    val windDirNight: WindDirBeen,
    // 夜晚风速
    val windSpeedNight: String,
    // 夜晚风等级
    val windLevelNight: String,
    val updateTime: Long,
    val humidity: String,
    val tempDay: String,
    val tempNight: String,
    val windDegreesDay: String,
    val windDegreesNight: String,
    val maxTemp: String,
    val minTemp: String,
    val realFeelTempMax: String,
    val realFeelTempMin: String,
    val source: String,
    val windGustDir: String,
    val rain: String,
    val pop: String,
    val cloudCover: String,
    val snowProb: String,
    val precProb: String,
    val thunProb: String,
    val rainProb: String,
    val ice: String,
    val iceProb: String,
    val snow: String,
    val windGustPow: String,
    val totalLiquid: String,
    val nightWindGustDir: String,
    val nightRain: String,
    val nightCloudCover: String,
    val nightSnowProb: String,
    val nightPrecProb: String,
    val nightThunProb: String,
    val nighTrainProb: String,
    val nightIce: String,
    val nightIceProb: String,
    val nightSnow: String,
    val nightWindGustPow: String,
    val nightTotalLiquid: String,
): Parcelable