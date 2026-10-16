package com.mosi.mosc.launcher.ai_weather.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.mosi.mosc.ai_weather.weather_srv.CardUIData
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutWeatherCardBaseLargeBinding
import com.mosi.mosc.launcher.ai_weather.ui.ExtraInfoRecycler
import com.mosi.mosc.launcher.ai_weather.utils.Utils

class BaseLargeWeatherCardView(context: Context): WeatherCardView(context) {

    private val binding = LayoutWeatherCardBaseLargeBinding.inflate(
        LayoutInflater.from(context)
    )

    override val root: View get()= binding.root

    override fun onBind(dataList: List<CardUIData>) {
        val data = dataList[0]
        val baseInfo = data.baseInfo
        binding.header.bindData(baseInfo.location, baseInfo.showTime)

        val iconImageId = Utils.getWeatherIconResourceId(baseInfo.icon)
        binding.imageWeather.setImageResource(iconImageId)
        binding.textCurrentTemp.text = "${baseInfo.currentTemp}°"
        binding.textTempHighLow.text = "${baseInfo.highTemp}°/${baseInfo.lowTemp}°"
        binding.textTempInfo.text = baseInfo.tempInfo

        val extraInfo = data.extraInfo
        binding.extraInfo.apply {
            setLayoutType(ExtraInfoRecycler.TYPE_STACK)
            setStackTopMargin(80)
            setItems(extraInfo.subList(0, extraInfo.size.coerceAtMost(2)))
        }

        // 设置卡片背景
        val weatherType = Utils.getWeatherType(baseInfo.icon)
        binding.root.setBackgroundResource(
            Utils.getFullCardBackgroundResourceId(weatherType)
        )
    }


}
