package com.mosi.mosc.launcher.ai_weather.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.mosi.mosc.ai_weather.been.CardUIData
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutWeatherCardRealtimeBinding
import com.mosi.mosc.launcher.ai_weather.ui.ExtraInfoRecycler
import com.mosi.mosc.launcher.ai_weather.utils.Utils

class RealtimeWeatherCardView(context: Context): WeatherCardView(context) {

    private val binding = LayoutWeatherCardRealtimeBinding.inflate(
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
        val forecastInfo = data.forecastInfo
        val lp = binding.forecast.layoutParams as ConstraintLayout.LayoutParams

        if (warnInfo.isEmpty()) {
            lp.topMargin = Utils.dp2px(context, 398)
            lp.height = Utils.dp2px(context, 440)

            binding.forecast.apply {
                visibility = View.VISIBLE
                setItems(forecastInfo.subList(0, forecastInfo.size.coerceAtMost(6)))
                setItemSpace(40)
            }
        } else {
            lp.topMargin = Utils.dp2px(context, 544)
            lp.height = Utils.dp2px(context, 296)

            binding.warn.apply {
                visibility = View.VISIBLE
                setPagerMode(true)
                setItems(warnInfo)
            }
            if (warnInfo.size > 1) {
                binding.indicator.apply {
                    attachToRecyclerView(binding.warn)
                    visibility = View.VISIBLE
                }
            }

            binding.forecast.apply {
                visibility = View.VISIBLE
                setItems(forecastInfo.subList(0, forecastInfo.size.coerceAtMost(6)))
                setItemSpace(28)
            }
        }

        val extraInfo = data.extraInfo
        binding.extraInfo.apply {
            setLayoutType(ExtraInfoRecycler.TYPE_INLINE)
            setItems(extraInfo.subList(0, extraInfo.size.coerceAtMost(2)))
        }

        // 设置卡片背景
        val weatherType = Utils.getWeatherType(baseInfo.icon)
        binding.root.setBackgroundResource(
            Utils.getFullCardBackgroundResourceId(weatherType)
        )
    }
}
