package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CityBeen(
    val cityId: String,
    val name: String,
    val counName: String,
    val upName: String?,
    val secondaryName: String,
    val ianaTimeZone: String,
    val parentCity: String,
    val provinceName: String,
): Parcelable
