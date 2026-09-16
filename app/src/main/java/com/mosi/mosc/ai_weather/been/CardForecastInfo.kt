package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CardForecastInfo(
    // 时间： 日期 或者 小时
    val time: String?,
    // 天气图标
    val icon: String?,
    // 降水概率
    val probability: String?,
    // 最高温度
    val highTemp: String?,
    // 最低温度
    val lowTemp: String?,
    // 实时天气信息
    val condition: String?,
) : Parcelable{
}