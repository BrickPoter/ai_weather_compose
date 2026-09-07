package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AqiForecastResponseBeen(
    val date: String,
    val publishTime: String,
    val value: Int,
): Parcelable
