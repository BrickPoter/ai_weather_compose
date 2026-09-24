package com.mosi.mosc.launcher.ai_weather.model

data class ForecastInfoItemData(
    val time: String,
    val imageId: Int,
    val probability: String,
    val highTemp: String,
    val lowTemp: String,
    val condition: String
)