package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DataBeen(
    val city:CityBeen,
    // 实时天气
    val condition:ConditionBeen,
    // 日预报
    val forecast: List<ForecastBeen>,
    // 小时预报
    val hourly: List<HourlyBeen>,
    val aqi: AQIBeen,
    //
    val liveIndex: List<LiveIndexBeen>,
    //警报信息
    val alert: List<AlertBeen>,
    val currentLimit: LimitBeen,
    val todayWeather: String?,
    val aqiForecastResponse: List<AqiForecastResponseBeen>,
    val limit: List<LimitBeen>,
): Parcelable