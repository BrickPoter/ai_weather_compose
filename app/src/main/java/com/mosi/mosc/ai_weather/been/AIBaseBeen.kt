package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AIBaseBeen(
    val hasStream: String,
    val taskState: String,
    val taskId: String,
    val domain: String,
    val intention: String,
    val query: String,
    //展示语义
    // 说明任务
    // 天气对象和对应时间
    val slots: SlotsBeen,
    // 天气接口返回的完整  按照 `slots` 抽取数据
    val bizData: DataBeen
): Parcelable