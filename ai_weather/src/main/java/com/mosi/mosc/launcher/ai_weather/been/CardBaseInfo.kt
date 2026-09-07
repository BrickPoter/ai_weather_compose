package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 卡片显示基本信息
 */
@Parcelize
data class CardBaseInfo (
    // 天气图标
    var icon: String?,
    // 位置信息
    var location: String?,
    // 显示的时间
    var showTime: String?,
    // 当前温度
    var currentTemp: String?,
    // 最高温度
    var highTemp: String?,
    // 最低温度
    var lowTemp: String?,
    // 天气信息
    var tempInfo: String?

): Parcelable