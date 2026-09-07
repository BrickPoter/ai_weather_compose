package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AIBaseBeen(
    val requestId: String,
    val service: String,
    val operation: String,
    val slots: SlotsBeen,
    val data: DataBeen
): Parcelable