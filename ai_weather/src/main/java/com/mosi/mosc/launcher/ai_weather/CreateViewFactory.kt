package com.mosi.mosc.launcher.ai_weather

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.mosi.mosc.ai_weather.been.CardUIData
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutWeatherCardBinding
import com.mosi.mosc.launcher.ai_weather.model.AlertItem
import com.mosi.mosc.launcher.ai_weather.model.ExtraInfoItemData
import java.lang.ref.WeakReference
import androidx.core.view.isVisible
import com.fawvw.hmi.ailauncher.pluginlib.CardSize
import com.mosi.mosc.launcher.ai_weather.model.ForecastInfoItemData

@SuppressLint("StaticFieldLeak")
object CreateViewFactory {
    private var cardUIViewRef: WeakReference<View>? = null
    private var cardUIBindingRef: WeakReference<LayoutWeatherCardBinding> ?= null

    fun createView (context: Context?): View {
        return createCardView(context)
    }

    fun getCardSize(): CardSize {
        val binding = cardUIBindingRef?.get() ?: return CardSize.ONE_EIGHT
        return if (binding.forecast.isVisible) {
            CardSize.ONE_FOUR
        } else {
            CardSize.ONE_EIGHT
        }
    }

    fun showAlertInfo(alertList: List<AlertItem>?, context: Context?) {
        if (cardUIBindingRef == null) {
            createCardView(context)
        }
        val binding = cardUIBindingRef?.get() ?: return
        alertList ?: return

        binding.alert.apply {
            visibility = View.VISIBLE
            setItems(alertList)
            setPagerMode(true)
        }
        if (alertList.size > 1) {
            binding.indicator.apply {
                visibility = View.VISIBLE
                attachToRecyclerView(binding.alert)
            }
        }
    }

    fun refreshView(){
        cardUIViewRef?.get()?.requestLayout()
    }

    fun showOrdinaryCard(uiData: CardUIData?, context: Context?) {
        if (cardUIBindingRef == null || cardUIViewRef == null) {
            createCardView(context)
        }
        val binding = cardUIBindingRef?.get() ?: return
        uiData ?: return

        // 显示卡片Header
        binding.header.apply {
            visibility = View.VISIBLE
            bindData(uiData.baseInfo.location, uiData.baseInfo.showTime)
        }

        // 显示卡片基础信息，有两种形式
        // 第一种形式：实时温度 + 最高最低温度
        if (uiData.baseInfo.currentTemp == null) {
            binding.baseInfoRange.apply {
                binding.baseInfoCurrent.visibility = View.GONE
                visibility = View.VISIBLE
                bindData(
                    uiData.baseInfo.highTemp,
                    uiData.baseInfo.lowTemp,
                    uiData.baseInfo.tempInfo)
            }
        } else {
            // 第二种形式：最高温度和最低温度
            binding.baseInfoCurrent.apply {
                binding.baseInfoRange.visibility = View.GONE
                visibility = View.VISIBLE
                bindData(
                    uiData.baseInfo.currentTemp,
                    uiData.baseInfo.highTemp,
                    uiData.baseInfo.lowTemp,
                    uiData.baseInfo.tempInfo
                )
            }
        }

        // 显示天气天气预报
//        val forecastInfoList = arrayListOf<ForecastInfoItemData>()
//        uiData.forecastInfo.forEach {
//            forecastInfoList.add(
//                ForecastInfoItemData(
//                    it.time.toString(),
//                    0,
//                    it.probability.toString(),
//                    it.highTemp.toString(),
//                    it.lowTemp.toString(),
//                    it.condition.toString()
//                )
//            )
//        }
//
//        if (!forecastInfoList.isEmpty()) {
//            binding.forecast.apply {
//                visibility = View.VISIBLE
//                setDataList(forecastInfoList)
//            }
//        }
        if (!uiData.forecastInfo.isEmpty()) {
            binding.forecast.apply {
                visibility = View.VISIBLE
                setDataList(uiData.forecastInfo)
            }
        }
//
        // 显示预警信息
//        binding.warn.apply {
//            visibility = View.VISIBLE
//            bindData(uiData.warnInfo?.title, uiData.warnInfo?.content)
//        }

        // 显示选择性信息
        val extraInfoList = arrayListOf<ExtraInfoItemData>()
        uiData.extraInfo.forEach {
            extraInfoList.add(ExtraInfoItemData(it.title, it.content))
        }

        if (!extraInfoList.isEmpty()) {
            binding.extraInfo.apply {
                visibility = View.VISIBLE
                setDataList(extraInfoList)
            }
        }
    }

    private fun createCardView(context: Context?): View {
        cardUIBindingRef?.get()?.let { return it.root }

        val ctx = context ?: return View(context)
        val binding = LayoutWeatherCardBinding.inflate(LayoutInflater.from(ctx))
        cardUIBindingRef = WeakReference(binding)
        cardUIViewRef = WeakReference(binding.root)
        return binding.root
    }

    fun destroyView(){
        cardUIViewRef = null
        cardUIBindingRef = null
    }

    interface CreateViewListener{

    }
}
