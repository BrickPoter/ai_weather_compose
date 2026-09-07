package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HourlyBeen(
    //天气现象
    val condition: String,
    val conditionId: String,
    val date: Long,
    //温度
    val temp: String,
    //降水概率
    val pop: String,
    //风向
    val windDir: WindDirBeen,
    //湿度
    val humidity: String,
    //气压
    val pressure: String,
    //降水量
    val qpf: String,
    //体感温度
    val realFeel: String,
    //降雪量
    val snow: String,
    val updateTime: Long,
    //紫外线
    val uvi: String,
    val windDegrees: String,
    //风速
    val windSpeed: String,
    //风级
    val windLevel: String,
    val wp: String,
    val dayNight: Boolean,
    val iconDay: String,
    val iconNight: String,
): Parcelable