package com.mosi.mosc.ai_weather.been


import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CardUIData(
    // 基础信息
    val baseInfo: CardBaseInfo,
    // 选择性信息
    val extraInfo: List<CardExtraInfo>,
    // 预报信息
    val forecastInfo: List<CardForecastInfo>,
    // 警报信息
    val warnInfo: CardWarnInfo
): Parcelable {
}