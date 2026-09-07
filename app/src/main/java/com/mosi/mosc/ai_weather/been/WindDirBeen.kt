package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WindDirBeen(
    val code: String,
    val name: String,
    ): Parcelable