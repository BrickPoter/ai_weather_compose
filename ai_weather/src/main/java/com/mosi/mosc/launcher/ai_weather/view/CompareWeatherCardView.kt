package com.mosi.mosc.launcher.ai_weather.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.mosi.mosc.ai_weather.been.CardUIData
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutWeatherCardCompareBinding
import com.mosi.mosc.launcher.ai_weather.ui.ExtraInfoRecycler
import com.mosi.mosc.launcher.ai_weather.utils.Utils

class CompareWeatherCardView(context: Context) : WeatherCardView(context){

    private val binding = LayoutWeatherCardCompareBinding.inflate(
        LayoutInflater.from(context)
    )

    override val root: View get()= binding.root

    override fun onBind(dataList: List<CardUIData>) {
        val localData = dataList[0]
        val targetData = dataList[1]

        // 本地天气信息
        val localBaseInfo = localData.baseInfo
        val localExtraInfo = localData.extraInfo
        binding.header.bindData(localBaseInfo.location, localBaseInfo.showTime)
        binding.baseInfoRange.apply {
            bindData(
                localBaseInfo.highTemp,
                localBaseInfo.lowTemp,
                localBaseInfo.tempInfo,
                localBaseInfo.icon
            )
        }
        binding.extraInfo.setLayoutType(ExtraInfoRecycler.TYPE_INLINE)  // 单行模式
        binding.extraInfo.setItems(localExtraInfo)

        // 分隔线
        val dividerLp = binding.divider.layoutParams
        dividerLp.height = Utils.dp2px(context, 2)
        binding.divider.layoutParams = dividerLp

        // 目的地天气信息
        val targetBaseInfo = targetData.baseInfo
        val targetExtraInfo = targetData.extraInfo
        binding.header2.bindData(targetBaseInfo.location, targetBaseInfo.showTime)
        binding.baseInfoRange2.apply {
            bindData(
                targetBaseInfo.highTemp,
                targetBaseInfo.lowTemp,
                targetBaseInfo.tempInfo,
                targetBaseInfo.icon
            )
        }
        binding.extraInfo2.setLayoutType(ExtraInfoRecycler.TYPE_INLINE)  // 单行模式
        binding.extraInfo2.setItems(targetExtraInfo)


        // 设置卡片背景
        val localWeatherType = Utils.getWeatherType(localData.baseInfo.icon)
        val targetWeatherType = Utils.getWeatherType(targetData.baseInfo.icon)
        binding.bgLocal.setBackgroundResource(
            Utils.getTopBackgroundResourceId(localWeatherType)
        )
        binding.bgTarget.setBackgroundResource(
            Utils.getBottomBackgroundResourceId(targetWeatherType)
        )
    }
}
