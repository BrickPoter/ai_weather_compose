package com.mosi.mosc.launcher.ai_weather.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.mosi.mosc.ai_weather.weather_srv.CardUIData
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutWeatherCardExpandBinding
import com.mosi.mosc.launcher.ai_weather.ui.ExtraInfoRecycler
import com.mosi.mosc.launcher.ai_weather.utils.Utils


class ExpandWeatherCardView(context: Context): WeatherCardView(context) {
    
    private val binding = LayoutWeatherCardExpandBinding.inflate(
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
        val extraInfo = data.extraInfo
        val lp = binding.extraInfo.layoutParams as ConstraintLayout.LayoutParams
        if (warnInfo.isEmpty()) {
            binding.warn.visibility = View.GONE
            binding.indicator.visibility = View.GONE

            lp.topMargin = Utils.dp2px(context, 392)
            lp.height = Utils.dp2px(context, 632)

            binding.extraInfo.apply {
                setLayoutType(ExtraInfoRecycler.TYPE_STACK)
                setStackTopMargin(48)
                setStackItemSpace(48)
                setItems(extraInfo.subList(0, extraInfo.size.coerceAtMost(6)))
            }
        } else {
            lp.topMargin = Utils.dp2px(context, 524)
            lp.height = Utils.dp2px(context, 500)

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

            binding.extraInfo.apply {
                setLayoutType(ExtraInfoRecycler.TYPE_STACK)
                setStackTopMargin(64)
                setStackItemSpace(80)
                setItems(extraInfo.subList(0, extraInfo.size.coerceAtMost(4)))
            }
        }

        // 设置卡片背景
        val weatherType = Utils.getWeatherType(baseInfo.icon)
        binding.root.setBackgroundResource(
            Utils.getFullCardBackgroundResourceId(weatherType)
        )
    }
}
