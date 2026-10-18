package com.mosi.mosc.launcher.ai_weather.enums

import com.fawvw.hmi.ailauncher.pluginlib.CardPriority
import com.fawvw.hmi.ailauncher.pluginlib.CardSize
import com.mosi.mosc.ai_weather.weather_srv.CardUIData
import java.util.Locale

/**
 * 天气卡片的内容形态。
 *
 * 这里只描述“数据应该显示成哪一种卡片”，不再把 BASE_LARGE 这类布局差异放进数据类型。
 * BASE 在拿到 1/8 或 1/4 槽位时，由 ComposeWeatherCard.getView(cardSize) 决定具体布局。
 */
enum class WeatherCardType(
    val preferredSize: CardSize,
    val supportedSizes: List<CardSize>,
    val summary: String,
    val showReason: String,
    val priority: CardPriority
) {
    BASE(
        preferredSize = CardSize.ONE_EIGHT,
        supportedSizes = listOf(CardSize.ONE_EIGHT, CardSize.ONE_FOUR),
        summary = "基础天气卡片",
        showReason = "显示基础卡片信息",
        priority = CardPriority.MEDIUM
    ),
    COMPARE(
        preferredSize = CardSize.ONE_FOUR,
        supportedSizes = listOf(CardSize.ONE_FOUR),
        summary = "天气对比卡片",
        showReason = "显示天气对比卡片",
        priority = CardPriority.MEDIUM
    ),
    EXPAND(
        preferredSize = CardSize.ONE_FOUR,
        supportedSizes = listOf(CardSize.ONE_FOUR),
        summary = "额外天气信息",
        showReason = "显示额外天气信息",
        priority = CardPriority.MEDIUM
    ),
    FORECAST(
        preferredSize = CardSize.ONE_FOUR,
        supportedSizes = listOf(CardSize.ONE_FOUR),
        summary = "未来几天天气预报",
        showReason = "显示天气预报信息",
        priority = CardPriority.MEDIUM
    ),
    REALTIME(
        preferredSize = CardSize.ONE_FOUR,
        supportedSizes = listOf(CardSize.ONE_FOUR),
        summary = "实时天气信息",
        showReason = "显示实时天气预报信息",
        priority = CardPriority.MEDIUM
    );

    companion object {
        /**
         * 根据天气数据推断应该属于那种类型。
         *
         * 服务端保证对比卡按“基准数据在前、对比数据在后”返回，因此这里只判断数据组数。
         */
        fun from(data: List<CardUIData>): WeatherCardType {
            if (data.size > 1) return COMPARE

            val card = data.firstOrNull() ?: return BASE
            val granularity = card.granularity
                .lowercase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "")

            return when {
                granularity == "multiday" -> FORECAST
                card.forecastInfo.isNotEmpty() -> REALTIME
                card.extraInfo.size > 2 -> EXPAND
                else -> BASE
            }
        }
    }
}
