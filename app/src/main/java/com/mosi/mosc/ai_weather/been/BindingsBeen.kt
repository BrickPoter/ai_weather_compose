package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BindingsBeen(
    //基础天气信息 包含
    // 实时天气 condition
    // 日预报 forecast
    // 小时预报 hourly
    val weatherObjects: List<String>,
    val time:TimeBeen
):Parcelable