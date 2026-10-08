package com.mosi.mosc.launcher.ai_weather

import com.fawvw.hmi.ailauncher.pluginlib.CardPriority
import com.fawvw.hmi.ailauncher.pluginlib.CardSize
import com.mosi.mosc.ai_weather.been.CardUIData

enum class WeatherDisplayMode(
    val cardSize: CardSize,
    val priority: CardPriority,
    val summary: String,
    val showReason: String
) {

    BASE(
        CardSize.ONE_EIGHT,
        CardPriority.HIGH,
        "基础天气卡片",
        "显示基础卡片信息"
    ),
    BASE_LARGE(
        CardSize.ONE_FOUR,
        CardPriority.MEDIUM,
        "基础天气信息大卡",
        "显示大卡基础天气信息"
    ),
    COMPARE(
        CardSize.ONE_FOUR,
        CardPriority.MEDIUM,
        "天气对比卡片",
        "显示天气对比卡片"
    ),

    EXPAND(
        CardSize.ONE_FOUR,
        CardPriority.HIGH,
        "额外天气信息",
        "显示额外天气信息"
    ),

    // 天气比较
    EXPAND_ALERT(
        CardSize.ONE_FOUR,
        CardPriority.MEDIUM,
        "额外天气信息及预警",
        "显示额外天气信息及预警"
    ),

    FORECAST(
        CardSize.ONE_FOUR,
        CardPriority.MEDIUM,
        "未来几天天气预报",
        "显示天气预报信息",
    ),

    REALTIME(
        CardSize.ONE_FOUR,
        CardPriority.MEDIUM,
        "实时天气信息",
        "显示实时天气预报信息",
    ),

    REALTIME_ALERT(
        CardSize.ONE_FOUR,
        CardPriority.MEDIUM,
        "实时天气信息及预警",
        "显示实时天气信息及预警"
    );

    companion object {
        fun from(data: List<CardUIData>?): WeatherDisplayMode {
            if (data == null) return WeatherDisplayMode.BASE

            // 天气对比卡片
            if (data.size > 1) return WeatherDisplayMode.COMPARE

            // 其他卡片
            val cardUIData = data[0]
            val warnInfo = cardUIData.warnInfo
            val granularity = cardUIData.granularity
            val forecastInfo = cardUIData.forecastInfo
            val extraInfo = cardUIData.extraInfo

            if (granularity.lowercase().replace("-","").replace("_","") == "multi-day") {
                return WeatherDisplayMode.FORECAST
            }
            if (forecastInfo.isEmpty() && warnInfo.isEmpty() && extraInfo.size <= 2) return WeatherDisplayMode.BASE

            return if (warnInfo.isNotEmpty()) {
                if (forecastInfo.isNotEmpty()) {
                    WeatherDisplayMode.REALTIME_ALERT
                } else {
                    WeatherDisplayMode.EXPAND_ALERT
                }
            } else {
                if (forecastInfo.isNotEmpty()) {
                    WeatherDisplayMode.REALTIME
                } else {
                    WeatherDisplayMode.EXPAND
                }
            }
        }
    }
}