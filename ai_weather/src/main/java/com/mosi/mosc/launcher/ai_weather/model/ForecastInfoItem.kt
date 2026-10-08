package com.mosi.mosc.launcher.ai_weather.model

data class ForecastInfoItem(
    val time: String,
    val probability: String?,
    val highTemp: String?,
    val lowTemp: String?,
    val condition: String
)