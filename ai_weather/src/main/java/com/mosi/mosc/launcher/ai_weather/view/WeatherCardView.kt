package com.mosi.mosc.launcher.ai_weather.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.mosi.mosc.ai_weather.been.CardUIData

abstract class WeatherCardView(protected val context: Context){

    abstract val root: View

    fun bind(dateList: List<CardUIData>?) {
        if (dateList == null) {
            root.visibility = View.GONE
            return
        }
        root.visibility = View.VISIBLE
        onBind(dateList)
    }

    protected abstract fun onBind(dataList: List<CardUIData>)

    /** 尺寸交给宿主容器：容器已定尺寸时卡片就填满它 */
    fun attachTo(parent: ViewGroup, width: Int, height: Int) {
        parent.addView(
            root,
            ViewGroup.LayoutParams(width, height)
        )
    }

    open fun release() {

    }
}
