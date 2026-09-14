package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HeaderBeen(
    val agentId: String,
    val sessionId: String,
    val messageId: String,
    val messageStatus: String,
    val time: String,
    val topic: String
    ):Parcelable
