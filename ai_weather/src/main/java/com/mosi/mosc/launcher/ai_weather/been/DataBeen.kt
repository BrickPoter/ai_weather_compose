package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import com.mosi.mosc.ai_weather.been.AQIBeen
import kotlinx.parcelize.Parcelize

@Parcelize
data class DataBeen(
    // 城市名称
    val city: CityBeen,
    // 实时天气
    val condition: ConditionBeen,
    // 日级天气预报
    val forecast: List<ForecastBeen>,
    // 小时级天气预报
    val hourly: List<HourlyBeen>,
    // 当前空气质量
    val aqi: AQIBeen,
    // 生活指数
    val liveIndex: List<LiveIndexBeen>,
    // 天气预警
    val alert: List<ConditionBeen>,
    // 当前限行信息
    val currentLimit: LimitBeen,
    // 今日天气摘要
    val todayWeather: String?,
    // 空气质量预报
    val aqiForecastResponse: List<AqiForecastResponseBeen>,
    // 日级限行信息
    val limit: List<LimitBeen>,
): Parcelable