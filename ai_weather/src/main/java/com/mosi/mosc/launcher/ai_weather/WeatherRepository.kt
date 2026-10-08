package com.mosi.mosc.launcher.ai_weather

import com.mosi.mosc.ai_weather.been.CardUIData

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
}