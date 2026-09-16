package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CardWarnInfo(
    val icon: String?,
    val title: String?,
    val content: String?
): Parcelable {
}