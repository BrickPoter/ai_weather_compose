package com.mosi.mosc.launcher.ai_weather.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.mosi.mosc.ai_weather.weather_srv.CardUIData
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
        val warnInfo = data.warnInfo
        val lp = binding.forecast.layoutParams
        if (warnInfo.isEmpty()) {
            binding.warn.visibility = View.GONE
            binding.indicator.visibility = View.GONE
            lp.height = Utils.dp2px(context, 836)
            binding.forecast.apply {
                setItems(forecastInfo.subList(0, forecastInfo.size.coerceAtMost(8)))
                setItemSpace(28)
            }
        } else {
            lp.height = Utils.dp2px(context, 728)
            binding.forecast.apply {
                setItems(forecastInfo.subList(0, forecastInfo.size.coerceAtMost(8)))
                setItemSpace(28)
                }

            binding.warn.apply {
                setItems(warnInfo)
                setPagerMode(true)
                visibility = View.VISIBLE
            }

            if (warnInfo.size > 1) {
                binding.indicator.apply {
                    attachToRecyclerView(binding.warn)
                    visibility = View.VISIBLE
                }
            }
        }

        // 设置卡片背景
        val weatherType = Utils.getWeatherType(baseInfo.icon)
        binding.root.setBackgroundResource(
            Utils.getFullCardBackgroundResourceId(weatherType)
        )
    }


}
