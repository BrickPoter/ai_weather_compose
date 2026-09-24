package com.mosi.mosc.launcher.ai_weather.utils

import android.content.Context
import com.mosi.mosc.launcher.ai_weather.R

object Utils {
    fun dp2px(context: Context, dp: Int): Int {
        val f = dp * context.resources.displayMetrics.density
        val px =  if (f >= 0) f + 0.5F else f - 0.5F
        return px.toInt()
    }

    fun getAlertImageId(level: Int): Int {
        return when(level) {
            1 -> R.drawable.alert_yellow
            2 -> R.drawable.alert_orange
            3 -> R.drawable.alert_red
            else -> R.drawable.alert_blue
        }
    }

    fun getImageId(str: String) : Int {
        return when(str) {
            "晴" -> R.drawable.clear_day
            "多云" -> R.drawable.clear_cloudy_day
            "晴转多云" -> R.drawable.clear_cloudy_day
            "小雨" -> R.drawable.rain_light
            else -> R.drawable.bg_rect_alert_info
        }
    }

}