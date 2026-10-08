package com.mosi.mosc.launcher.ai_weather

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.fawvw.hmi.ailauncher.pluginlib.BusinessType
import com.fawvw.hmi.ailauncher.pluginlib.CardPriority
import com.fawvw.hmi.ailauncher.pluginlib.CardSize
import com.fawvw.hmi.ailauncher.pluginlib.ICard
import com.fawvw.hmi.ailauncher.pluginlib.VisibleState
import com.mosi.mosc.ai_weather.been.CardUIData
import com.mosi.mosc.launcher.ai_weather.view.BaseLargeWeatherCardView
import com.mosi.mosc.launcher.ai_weather.view.BaseWeatherCardView
import com.mosi.mosc.launcher.ai_weather.view.CompareWeatherCardView
import com.mosi.mosc.launcher.ai_weather.view.ExpandAlertWeatherCardView
import com.mosi.mosc.launcher.ai_weather.view.ExpandWeatherCardView
import com.mosi.mosc.launcher.ai_weather.view.ForecastWeatherCardView
import com.mosi.mosc.launcher.ai_weather.view.RealtimeAlertWeatherCardView
import com.mosi.mosc.launcher.ai_weather.view.RealtimeWeatherCardView
import com.mosi.mosc.launcher.ai_weather.view.WeatherCardView

class WeatherCard(
    private val context: Context,
    private val repository: WeatherRepository,
    private val cardId: String = KEY_CARD_ID
): ICard {
    companion object {
        private val SUPPORTED_SIZES = listOf(CardSize.ONE_FOUR, CardSize.ONE_EIGHT)
        const val KEY_CARD_ID = "weather_1"
    }

    /**
     * 交给宿主的「稳定 View」：卡片生命周期内不替换。
     * 宿主只在 createCard → getView 取一次，之后靠它原地更新。
     */
    private val container = FrameLayout(context)
    /** 当前内容视图，随展示模式切换；null 表示无内容 */
    private var content: WeatherCardView ?= null
    /** 当前展示模式；launcher 可能在非主线程读，故加 @Volatile */
    @Volatile
    var currentMode: WeatherDisplayMode = WeatherDisplayMode.BASE
        private set

    /** 是否可见（保护态），由 onVisibleStateChange 维护 */
    private var visible = false

    override fun getCardId() = cardId

    override fun getSupportedSizes(): List<CardSize> = SUPPORTED_SIZES

    override fun getView(cardSize: CardSize): View {
        // 尺寸由 requestShow 上报的 cardSize 决定，宿主按它给槽位；
        // 这里只保证内容与当前数据一致，并始终返回同一个容器
        render(repository.currentData)
        return container
    }

    override fun getCardSize(): CardSize = currentMode.cardSize

    override fun getCardPriority(): CardPriority = currentMode.priority

    override fun getBusinessType(): BusinessType = BusinessType.WEATHER

    override fun onRefresh() {
        if (!visible) return
        repository.requestRefresh()
        render(repository.currentData)
    }

    override fun onVisibleStateChange(state: VisibleState) {
        when (state) {
            VisibleState.SHOW -> {
                visible = true
                render(repository.currentData)
            }
            VisibleState.HIDE -> {
                visible = false
                releaseContent()                 // 释放内容资源，但保留 container
            }
        }
    }

    /** 宿主 destroyCard 时调用 */
    fun release() = clearContent()

    /** 数据更新：原地刷新 */
    fun onDataChanged() {
        render(repository.currentData)
        if (visible) container.requestLayout()
    }

    private fun createView(mode: WeatherDisplayMode) : WeatherCardView {
        return when(mode) {
            WeatherDisplayMode.BASE -> BaseWeatherCardView(context)
            WeatherDisplayMode.BASE_LARGE -> BaseLargeWeatherCardView(context)
            WeatherDisplayMode.COMPARE -> CompareWeatherCardView(context)
            WeatherDisplayMode.EXPAND -> ExpandWeatherCardView(context)
            WeatherDisplayMode.EXPAND_ALERT -> ExpandAlertWeatherCardView(context)
            WeatherDisplayMode.FORECAST -> ForecastWeatherCardView(context)
            WeatherDisplayMode.REALTIME -> RealtimeWeatherCardView(context)
            WeatherDisplayMode.REALTIME_ALERT -> RealtimeAlertWeatherCardView(context)
        }
    }

    private fun render(data: List<CardUIData>?) {
        val mode = WeatherDisplayMode.from(data)
        if (content == null || mode != currentMode) {
            swapContent(mode)
        }
        content?.bind(data)
    }

    private fun clearContent() {
        content?.let { container.removeView(it.root); it.release() }
        content = null
    }

    private fun swapContent(mode: WeatherDisplayMode) {
        content?.let {
            container.removeView(it.root)
            it.release()
        }
        content = createView(mode).also {
            container.addView(
                it.root,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        currentMode = mode
    }

    private fun releaseContent() {
        content?.let {
            container.removeView(it.root)
            it.release()
        }
        content = null
    }
}