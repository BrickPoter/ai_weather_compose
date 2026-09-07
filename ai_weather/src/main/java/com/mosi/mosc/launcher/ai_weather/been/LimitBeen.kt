package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LimitBeen(
    val date: String,
    val prompt: String,
): Parcelable
