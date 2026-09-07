package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ConditionBeen(
    //天气现象
    val condition: String,
    val conditionId: String,
    //湿度
    val humidity: String,
    //气压
    val pressure: String,
    //体感温度
    val realFeel: String,
    val updateTime: String,
    //温度
    val temp: String,
    //能见度
    val vis: String,
    //紫外线
    val uvi: String,
    //风速
    val windSpeed: String,
    //风向
    val windDir: WindDirBeen,
    //风级
    val windLevel: String,
    val windDegrees: String,
    //日出
    val sunRise: Long,
    //日落
    val sunSet: Long,
    //
    val precipitation: String?,
    //提示
    val tips: String?,
    //
    val windGustLevel: String?,
    val windGustSpeed: String?,
    val windGustDir: String?,
    val cloudCover: String?,
    val pressureTendency: String?,
    val icon: String?,
): Parcelable
