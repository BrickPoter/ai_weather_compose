package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 一个任务在一个时间角色下需要使用的一组天气对象
 * 同一任务、同一时间下有多个天气对象时，放入同一个 binding 的 `weatherObjects`。
 * 只有时间不同才拆分为多个 binding。例如比较今天和明天温度时，会生成两个 binding。
 */
@Parcelize
data class BindingsBeen(
    // 同一时间下需要使用的天气对象机器码
    // 基础天气信息 包含
    // 实时天气 condition
    // 日预报 forecast
    // 小时预报 hourly
    val weatherObjects: List<String>,
    //该组天气对象共同对应的时间
    val time: TimeBeen
): Parcelable