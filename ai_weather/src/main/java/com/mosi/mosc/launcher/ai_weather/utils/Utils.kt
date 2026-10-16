package com.mosi.mosc.launcher.ai_weather.utils

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.mosi.mosc.launcher.ai_weather.R
import com.mosi.mosc.launcher.ai_weather.enums.WeatherType
import com.mosi.mosc.launcher.ai_weather.ui.theme.AlertBg
import com.mosi.mosc.launcher.ai_weather.ui.theme.AlertOrangeBg
import com.mosi.mosc.launcher.ai_weather.ui.theme.AlertRedBg

object Utils {
    fun dp2px(context: Context, dp: Int): Int {
        val f = dp * context.resources.displayMetrics.density
        val px =  if (f >= 0) f + 0.5F else f - 0.5F
        return px.toInt()
    }

    fun getAlertImageResourceId(str: String): Int {
        return when(str) {
            "黄色" -> R.drawable.alert_yellow
            "橙色" -> R.drawable.alert_orange
            "红色" -> R.drawable.alert_red
            else -> R.drawable.alert_blue
        }
    }

    fun getAlertBgColor(str: String?): Color {
        return when(str) {
            "红色", "红色预警" -> AlertRedBg
            "橙色", "橙色预警","黄色", "黄色预警" -> AlertOrangeBg
            else -> AlertBg
        }
    }

    fun getExtraInfoImageResourceId(title: String?) : Int{
        return when(title) {
            "过敏指数" -> R.drawable.allergy_index
            "紫外线" -> R.drawable.ultraviolet
            "api", "空气质量" -> R.drawable.aqi
            "穿衣指数" -> R.drawable.clothing_index
            "湿度" -> R.drawable.humidity
            "降水量" -> R.drawable.precipitation_amount
            "降水概率", "降雨概率" -> R.drawable.precipitation_probability
            "日出" -> R.drawable.sunrise
            "日落" -> R.drawable.sunset
            "风力风向"-> R.drawable.wind
            else -> R.drawable.allergy_index
        }
    }

    fun getFullCardBackgroundResourceId(weatherType: WeatherType) : Int {
        return when(weatherType) {
            WeatherType.CLEAR -> R.drawable.clear
            WeatherType.SNOW -> R.drawable.snow
            WeatherType.SHADE, WeatherType.RAIN -> R.drawable.rain_shade
            WeatherType.DUST -> R.drawable.dust
        }
    }

    fun getHalfBackgroundResourceId(weatherType: WeatherType) : Int {
        return when(weatherType) {
            WeatherType.CLEAR -> R.drawable.clear_half
            WeatherType.SNOW -> R.drawable.snow_half
            WeatherType.SHADE, WeatherType.RAIN -> R.drawable.rain_shade_half
            WeatherType.DUST -> R.drawable.dust_half
        }
    }

    fun getTopBackgroundResourceId(weatherType: WeatherType) : Int {
        return when(weatherType) {
            WeatherType.CLEAR -> R.drawable.clear_half_top
            WeatherType.SNOW -> R.drawable.snow_half_top
            WeatherType.SHADE, WeatherType.RAIN -> R.drawable.rain_shade_half_top
            WeatherType.DUST -> R.drawable.dust_half_top
        }
    }

    fun getBottomBackgroundResourceId(weatherType: WeatherType) : Int {
        return when(weatherType) {
            WeatherType.CLEAR -> R.drawable.clear_half_bottom
            WeatherType.SNOW -> R.drawable.snow_half_bottom
            WeatherType.SHADE, WeatherType.RAIN -> R.drawable.rain_shade_half_bottom
            WeatherType.DUST -> R.drawable.dust_half_bottom
        }
    }

    fun getWeatherType(code: String?): WeatherType = when (code) {
        "晴", "少云", "多云", "大部晴朗" -> WeatherType.CLEAR
        "雪", "小雪", "中雪", "大雪", "暴雪", "雨夹雪", "冰雹", "冰粒", "冰针",
        "阵雪", "小阵雪", "小雪-中雪", "中雪-大雪", "大雪-暴雪" -> WeatherType.SNOW
        "雾", "浓雾", "强浓雾", "大雾", "特强浓雾", "冻雾",  "阴", "雷电" -> WeatherType.SHADE
        "雨", "小雨", "中雨", "大雨", "暴雨", "大暴雨", "特大暴雨",
        "小雨-中雨", "中雨-大雨", "大雨-暴雨", "暴雨-大暴雨","大暴雨-特大暴雨",
        "冻雨","雷阵雨", "雷阵雨并伴有冰雹", "阵雨", "局部阵雨", "小阵雨", "强阵雨" -> WeatherType.RAIN
        "雾霾", "尘卷风", "浮尘", "沙尘" -> WeatherType.DUST

        else -> WeatherType.CLEAR
    }

    fun getWeatherIconResourceId(condition: String?): Int = when (condition) {
        // 晴天-clear
        "多云" -> R.drawable.clear_cloudy_day
        "少云", "大部晴朗" -> R.drawable.clear_partly_cloudy_day
        "晴" -> R.drawable.clear_day
        // 雪-snow
        "阵雪", "小阵雪" -> R.drawable.snow_showers
        "小雪", "中雪", "雪" -> R.drawable.snow_light
        "冰雹", "冰粒" -> R.drawable.snow_hail
        "小雪-中雪", "中雪-大雪" -> R.drawable.snow_moderate
        "大雪-暴雪" -> R.drawable.snow_blizzard
        "大雪", "暴雪" -> R.drawable.snow_heavy
        "雨夹雪" -> R.drawable.snow_freezing_rain
        "冰针" -> R.drawable.snow_ice_needle
        // 阴-shade
        "雾", "浓雾", "强浓雾", "大雾", "特强浓雾", "冻雾" -> R.drawable.shade_fog
        "阴" -> R.drawable.shade_shade
        "雷电" -> R.drawable.shade_thunder
        // 雨 - rain
        "小雨-中雨", "中雨-大雨" -> R.drawable.rain_light
        "大雨-暴雨", "暴雨-大暴雨","大暴雨-特大暴雨" -> R.drawable.rain_heavy
        "大雨", "暴雨", "大暴雨", "特大暴雨" -> R.drawable.rain_torrential
        "冻雨" -> R.drawable.rain_freezing_rain
        "雷阵雨", "雷阵雨并伴有冰雹" -> R.drawable.rain_thunder_shower
        "阵雨", "局部阵雨", "小阵雨" -> R.drawable.rain_showers
        "强阵雨" -> R.drawable.rain_heavy_shower
        "中雨", "雨", "小雨" -> R.drawable.rain_moderate
        // 沙尘-dust
        "雾霾" -> R.drawable.dust_smog
        "尘卷风" -> R.drawable.dust_devil
        "浮尘" -> R.drawable.dust_floating_dust
        "沙尘" -> R.drawable.dust_dust

        // 没有匹配到图标
        else -> R.drawable.shade_shade
    }
}