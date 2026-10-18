package com.mosi.mosc.launcher.ai_weather

import com.mosi.mosc.ai_weather.weather_srv.CardUIData
import com.mosi.mosc.launcher.ai_weather.enums.WeatherCardType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 服务端最近一次下发的天气快照。
 *
 * items 与 type 始终一起更新，避免 UI 在两次读取之间看到“新数据 + 旧类型”的中间状态。
 */
data class WeatherSnapshot(
    val items: List<CardUIData>,
    val type: WeatherCardType,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val preferredSize get() = type.preferredSize
    val supportedSizes get() = type.supportedSizes
}

/**
 * 插件侧数据仓库。
 *
 * 数据只由 WeatherCardPlugin 在 AIDL onCardUIDateResult() 回调中写入；
 * UI 通过 snapshot 的 StateFlow 订阅，不再直接读取可变的 List 字段。
 */
class WeatherRepository {
    private val _snapshot = MutableStateFlow<WeatherSnapshot?>(null)
    val snapshot: StateFlow<WeatherSnapshot?> = _snapshot.asStateFlow()

    val currentSnapshot: WeatherSnapshot?
        get() = _snapshot.value

    /**
     * 保存最近一次解析结果并发布新快照。
     *
     * 空列表视为无效回调，不覆盖上一份可用快照。
     */
    fun update(newData: List<CardUIData>): WeatherSnapshot? {
        if (newData.isEmpty()) return null

        val items = newData.toList()
        val snapshot = WeatherSnapshot(
            items = items,
            type = WeatherCardType.from(items)
        )
        _snapshot.value = snapshot
        return snapshot
    }

    fun clear() {
        _snapshot.value = null
    }
}
