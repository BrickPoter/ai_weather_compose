package com.mosi.mosc.launcher.ai_weather.enums

import com.fawvw.hmi.ailauncher.pluginlib.CardPriority
import com.fawvw.hmi.ailauncher.pluginlib.CardSize

enum class WeatherDisplayMode(
    val cardSize: CardSize,
    val priority: CardPriority,
    val summary: String,
    val showReason: String
) {

    BASE(
        CardSize.ONE_EIGHT,
        CardPriority.MEDIUM,
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
        CardPriority.MEDIUM,
        "额外天气信息",
        "显示额外天气信息"
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

}