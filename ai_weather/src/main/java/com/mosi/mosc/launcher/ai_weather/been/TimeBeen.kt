package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TimeBeen(
    // 用户要求查询或展示的目标时间 `target`
    // 比较任务中的基准时间 `baseline`
    // 比较任务中的对比时间 `comparison`
    val role: String,
    // 用户原始时间表达，例如“明天”
    val expression: String,
    //天气数据粒度：`instant`、`period`、`day`、`multi_day`
    val granularity: String,
    // startTime 和 endTime 可作为匹配天气数据的时间窗口依据。
    // 绝对开始时间，ISO 8601，带 `+08:00` 时区
    val startTime: String,
    // 绝对结束时间；`instant` 粒度为 `null`
    val endTime: String
): Parcelable
