package com.mosi.mosc.launcher.ai_weather.utils

import com.mosi.mosc.ai_weather.weather_srv.CardBaseInfo
import com.mosi.mosc.ai_weather.weather_srv.CardExtraInfo
import com.mosi.mosc.ai_weather.weather_srv.CardForecastInfo
import com.mosi.mosc.ai_weather.weather_srv.CardUIData
import com.mosi.mosc.ai_weather.weather_srv.CardWarnInfo

fun getUIData(mode: String): List<CardUIData> {
    return when(mode) {
        "BASE", "BASE_LARGE" -> listOf(
            CardUIData(baseInfo1, extraInfo1, "day", emptyForecastInfo, emptyWarnInfo)
        )
        "COMPARE" -> listOf(
            CardUIData(baseInfo1, extraInfo1, "day", emptyForecastInfo, emptyWarnInfo),
            CardUIData(baseInfo2, extraInfo2, "day", emptyForecastInfo, emptyWarnInfo)
        )
        "EXPAND" -> listOf(
            CardUIData(baseInfo1, extraInfo2, "day", emptyForecastInfo, emptyWarnInfo)
        )
        "EXPAND_ALERT" -> listOf(
            CardUIData(baseInfo1, extraInfo2, "day", emptyForecastInfo, warnInfo2)
        )
        "FORECAST" -> listOf(
            CardUIData(baseInfo1, extraInfo1, "multiday", multidayForecast, emptyWarnInfo)
        )
        "FORECAST_ALERT" -> listOf(
            CardUIData(baseInfo1, extraInfo1, "multiday", multidayForecast, warnInfo2)
        )
        "REALTIME" -> listOf(
            CardUIData(baseInfo1, extraInfo1, "day", realtimeForecast, emptyWarnInfo)
        )
        "REALTIME_ALERT" -> listOf(
            CardUIData(baseInfo1, extraInfo1, "day", realtimeForecast, warnInfo1)
        )

        else -> listOf(
            CardUIData(baseInfo1, extraInfo1, "day", realtimeForecast, warnInfo1)
        )
    }
}

val baseInfo1 = CardBaseInfo(
    location = "成都",
    currentTemp = "27",
    highTemp = "29",
    lowTemp = "23",
    tempInfo = "多云",
    icon = "多云",
    showTime = "今日"
)

val baseInfo2 = CardBaseInfo(
    location = "重庆",
    currentTemp = null,
    highTemp = "29",
    lowTemp = "23",
    tempInfo = "阵雨",
    icon = "阵雨",
    showTime = "今日"
)

val emptyWarnInfo = emptyList<CardWarnInfo>()
val warnInfo1 = listOf(
    CardWarnInfo("","蓝色", "盘州市气象台发布雷电黄色预警信号(Ⅱ级)")
)

val warnInfo2 = listOf(
    CardWarnInfo("","蓝色", "这是一条蓝色预警信息"),
    CardWarnInfo("","黄色", "这是一条黄色预警信息"),
    CardWarnInfo("","橙色", "盘州市气象台2026年10月14日14时08分发布雷电黄色预警信号：预计未来6小时，我市大部乡镇（街道）将受雷电天气影响，局地伴有大风、短时强降水等强对流天气，请注意防范。（预警信息来源：国家预警信息发布中心"),
    CardWarnInfo("","红色","这是一条红色预警信息")
)

val extraInfo1 = listOf(
    CardExtraInfo("紫外线", "紫外线中等"),
    CardExtraInfo("穿衣指数", "薄长袖"),
)

val extraInfo2 = listOf(
    CardExtraInfo("降水概率", "50%"),
    CardExtraInfo("风力风向", "阵风6级"),
    CardExtraInfo("日出", "6:00"),
    CardExtraInfo("日落", "20:00"),
    CardExtraInfo("穿衣指数", "薄长袖"),
    CardExtraInfo("紫外线指数", "非常高"),
    CardExtraInfo("空气质量", "良好")
)

val emptyForecastInfo = emptyList<CardForecastInfo>()
val realtimeForecast = ArrayList<CardForecastInfo>().apply {
    repeat(4) {
        val data = CardForecastInfo(
            time = "12:00",
            icon = "中雨",
            probability = "50%",
            highTemp = "22",
            lowTemp = null,
            condition = "小雨"
        )
        add(data)
    }
    add(CardForecastInfo(
        time = "19:00",
        icon = "阴转雷阵雨",
        probability = "",
        highTemp = "22",
        lowTemp = null,
        condition = "多云"
    ))
}

val multidayForecast = ArrayList<CardForecastInfo>().apply {
    repeat(7) {
        val data = CardForecastInfo(
            time = "周一",
            icon = "中雨",
            probability = "50%",
            highTemp = "22",
            lowTemp = "18",
            condition = "小雨"
        )
        add(data)
    }
    add(CardForecastInfo(
        time = "09/28",
        icon = "多云",
        probability = "",
        highTemp = "22",
        lowTemp = "18",
        condition = "阴转雷阵雨"
    ))
}