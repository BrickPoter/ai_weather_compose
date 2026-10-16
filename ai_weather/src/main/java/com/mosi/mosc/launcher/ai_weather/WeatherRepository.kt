package com.mosi.mosc.launcher.ai_weather

import com.mosi.mosc.ai_weather.weather_srv.CardUIData
import com.mosi.mosc.launcher.ai_weather.enums.WeatherDisplayMode

class WeatherRepository {
    // weather_server 解析的数据
    private var data: List<CardUIData>? = null
    private var refreshAction: (() -> Unit)? = null

    val currentData: List<CardUIData>?
        get() = data

    fun update(newData: List<CardUIData>) {
        data = newData
    }

    fun setRefreshAction(action: () -> Unit) {
        refreshAction = action
    }

    fun requestRefresh() {
        refreshAction?.invoke()
    }

    /** 插件整体反注册时用：数据 + 刷新回调一起清掉 */
    fun clear() {
        data = null
        refreshAction = null
    }

    fun getDisplayMode(): WeatherDisplayMode {
        if (data.isNullOrEmpty()) return WeatherDisplayMode.BASE

        // 天气对比卡片
        if (data!!.size > 1) return WeatherDisplayMode.COMPARE

        // 其他卡片
        val cardUIData = data!![0]
        val granularity = cardUIData.granularity
        val forecastInfo = cardUIData.forecastInfo
        val extraInfo = cardUIData.extraInfo

        if (granularity.lowercase().replace("-","").replace("_","") == "multiday") {
            return WeatherDisplayMode.FORECAST
        }
        if (forecastInfo.isNotEmpty()) return WeatherDisplayMode.REALTIME
        if (extraInfo.size > 2) return WeatherDisplayMode.EXPAND
        return WeatherDisplayMode.BASE
    }
}