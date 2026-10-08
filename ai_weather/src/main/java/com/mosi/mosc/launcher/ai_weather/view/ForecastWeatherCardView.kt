package com.mosi.mosc.launcher.ai_weather.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.mosi.mosc.ai_weather.been.CardUIData
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutWeatherCardForecastBinding
import com.mosi.mosc.launcher.ai_weather.utils.Utils

class ForecastWeatherCardView(context: Context): WeatherCardView(context){

    private val binding = LayoutWeatherCardForecastBinding.inflate(
        LayoutInflater.from(context)
    )

    override val root: View get()= binding.root

    override fun onBind(dataList: List<CardUIData>) {
        val data = dataList[0]
        val baseInfo = data.baseInfo
        binding.header.bindData(baseInfo.location, null)
        val forecastInfo = data.forecastInfo
        if (!forecastInfo.isEmpty()) {
            binding.forecast.apply {
                setItems(forecastInfo.subList(0, forecastInfo.size.coerceAtMost(7)))
                setItemSpace(28)
            }
        }
        val warnInfo = data.warnInfo
        if (!warnInfo.isEmpty()) {
            binding.warn.apply {
                setItems(warnInfo)
                setPagerMode(true)
                visibility = View.VISIBLE
            }
        }
        if (warnInfo.size > 1) {
            binding.indicator.apply {
                attachToRecyclerView(binding.warn)
                visibility = View.VISIBLE
            }
        }

        // 设置卡片背景
        val weatherType = Utils.getWeatherType(baseInfo.icon)
        binding.root.setBackgroundResource(
            Utils.getFullCardBackgroundResourceId(weatherType)
        )
    }


}
