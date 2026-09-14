package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PayloadBeen(val dataType: String,private val index: Int,val data: AIBaseBeen):Parcelable