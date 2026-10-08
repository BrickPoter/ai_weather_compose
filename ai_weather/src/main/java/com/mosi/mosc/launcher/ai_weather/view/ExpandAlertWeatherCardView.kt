package com.mosi.mosc.launcher.ai_weather.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.mosi.mosc.ai_weather.been.CardUIData
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutWeatherCardExpandAlertBinding
import com.mosi.mosc.launcher.ai_weather.ui.ExtraInfoRecycler
import com.mosi.mosc.launcher.ai_weather.utils.Utils


class ExpandAlertWeatherCardView(context: Context): WeatherCardView(context) {
    
    private val binding = LayoutWeatherCardExpandAlertBinding.inflate(
        LayoutInflater.from(context)
    )

    override val root: View get()= binding.root

    override fun onBind(dataList: List<CardUIData>) {
        val data = dataList[0]
        val baseInfo = data.baseInfo
        binding.header.bindData(baseInfo.location, baseInfo.showTime)
        if (baseInfo.currentTemp == null) {
            binding.baseInfoCurrent.visibility = View.GONE
            binding.baseInfoRange.visibility = View.VISIBLE
            binding.baseInfoRange.apply {
                bindData(
                    baseInfo.highTemp,
                    baseInfo.lowTemp,
                    baseInfo.tempInfo,
                    baseInfo.icon
                )
            }
        } else {
            binding.baseInfoCurrent.visibility = View.VISIBLE
            binding.baseInfoRange.visibility = View.GONE
            binding.baseInfoCurrent.apply {
                bindData(
                    baseInfo.currentTemp,
                    baseInfo.highTemp,
                    baseInfo.lowTemp,
                    baseInfo.tempInfo,
                    baseInfo.icon
                )
            }
        }

        val warnInfo = data.warnInfo
        binding.warn.apply {
            setPagerMode(true)
            setItems(warnInfo)
        }
        if (warnInfo.size > 1) {
            binding.indicator.apply {
                attachToRecyclerView(binding.warn)
                visibility = View.VISIBLE
            }
        }

        val extraInfo = data.extraInfo
        binding.extraInfo.apply {
            setLayoutType(ExtraInfoRecycler.TYPE_STACK)
            setStackTopMargin(64)
            setStackItemSpace(80)
            setItems(extraInfo.subList(0, extraInfo.size.coerceAtMost(4)))
        }

        // 设置卡片背景
        val weatherType = Utils.getWeatherType(baseInfo.icon)
        binding.root.setBackgroundResource(
            Utils.getFullCardBackgroundResourceId(weatherType)
        )
    }
}
