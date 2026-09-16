package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CardExtraInfo(
    val title: String?,
    val content: String?
) : Parcelable{
}