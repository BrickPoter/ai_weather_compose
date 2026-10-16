package com.mosi.mosc.launcher.ai_weather

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.doOnLayout
import com.fawvw.hmi.ailauncher.pluginlib.BusinessType
import com.fawvw.hmi.ailauncher.pluginlib.CardPriority
import com.fawvw.hmi.ailauncher.pluginlib.CardSize
import com.fawvw.hmi.ailauncher.pluginlib.ICard
import com.fawvw.hmi.ailauncher.pluginlib.VisibleState
import com.mosi.mosc.ai_weather.weather_srv.CardUIData
import com.mosi.mosc.launcher.ai_weather.enums.WeatherDisplayMode
import com.mosi.mosc.launcher.ai_weather.view.BaseLargeWeatherCardView
import com.mosi.mosc.launcher.ai_weather.view.BaseWeatherCardView
import com.mosi.mosc.launcher.ai_weather.view.CompareWeatherCardView
import com.mosi.mosc.launcher.ai_weather.view.ExpandWeatherCardView
import com.mosi.mosc.launcher.ai_weather.view.ForecastWeatherCardView
import com.mosi.mosc.launcher.ai_weather.view.RealtimeWeatherCardView
import com.mosi.mosc.launcher.ai_weather.view.WeatherCardView

class WeatherCard(
    private val context: Context,
    private val repository: WeatherRepository,
    private val cardId: String = KEY_CARD_ID
): ICard {
    companion object {
        const val TAG = "WeatherCard"
        private val SUPPORTED_SIZES = listOf(CardSize.ONE_FOUR, CardSize.ONE_EIGHT)
        const val KEY_CARD_ID = "weather_1"
    }
    init {
        Log.d(TAG, "WeatherCard init")
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

    /** 宿主最后一次传入的槽位尺寸；null = 宿主还没取过 View（首次尚未上屏） */
    private var hostSize: CardSize? = null
    /** 数据期望的展示模式（由数据推导，供 requestShow / getCardSize 使用） */
    @Volatile
    var dataMode: WeatherDisplayMode = WeatherDisplayMode.BASE
        private set
    /** 当前内容视图实际渲染的模式（受槽位尺寸影响，可能与 dataMode 不同） */
    private var renderedMode: WeatherDisplayMode? = null

    /** 是否可见（保护态），由 onVisibleStateChange 维护 */
    private var visible = false

    override fun getCardId() = cardId

    override fun getSupportedSizes(): List<CardSize> = SUPPORTED_SIZES

    override fun getView(cardSize: CardSize): View {
        Log.d(TAG, "getView() Called, cardSize = $cardSize")
        hostSize = cardSize                     // 记住宿主槽位尺寸
        render(repository.currentData)          // 首次显示只在这里 render（唯一一次）
        container.doOnLayout { v ->        // ← 新增
            val dm = v.resources.displayMetrics
            Log.d(TAG,
                "cardSize=$cardSize mode=$currentMode " +
                        "card=${v.width}x${v.height}px " +
                        "dp=${v.width / dm.density}x${v.height / dm.density} " +
                        "density=${dm.density} dpi=${dm.densityDpi} " +
                        "screen=${dm.widthPixels}x${dm.heightPixels}px"
            )
        }
        return container
    }

    override fun getCardSize(): CardSize = dataMode.cardSize

    override fun getCardPriority(): CardPriority = dataMode.priority

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
        dataMode = repository.getDisplayMode()
        Log.d(TAG, "onDataChanged Called, dataMode=$dataMode, attached=${hostSize != null}")
//        render(repository.currentData)
        if (hostSize != null) {
            render(repository.currentData)
            container.requestLayout()
        }
//        if (visible) container.requestLayout()
    }

    private fun createView(mode: WeatherDisplayMode) : WeatherCardView {
        return when(mode) {
            WeatherDisplayMode.BASE -> BaseWeatherCardView(context)
            WeatherDisplayMode.BASE_LARGE -> BaseLargeWeatherCardView(context)
            WeatherDisplayMode.COMPARE -> CompareWeatherCardView(context)
            WeatherDisplayMode.EXPAND -> ExpandWeatherCardView(context)
            WeatherDisplayMode.FORECAST -> ForecastWeatherCardView(context)
            WeatherDisplayMode.REALTIME -> RealtimeWeatherCardView(context)
        }
    }

    private fun render(data: List<CardUIData>?) {
        Log.d(TAG, "render Called, data = $data")
//        val mode = WeatherDisplayMode.from(data)
        var displayMode = repository.getDisplayMode()
        if (displayMode == WeatherDisplayMode.BASE && hostSize == CardSize.ONE_FOUR) {
            displayMode = WeatherDisplayMode.BASE_LARGE
        }
        Log.d(TAG, "render Called, dataMode=$repository.getDisplayMode(), renderMode=$displayMode, hostSize=$hostSize")
        if (content == null || displayMode != currentMode) {
            swapContent(displayMode)
        }
        content?.bind(data)
    }

    private fun clearContent() {
        content?.let { container.removeView(it.root); it.release() }
        content = null
        renderedMode = null
        hostSize = null           // ★ 下次宿主再 getView 时重新 render
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
        renderedMode = mode  // 用于判断是否要换内容视图
    }

    private fun releaseContent() {
        content?.let {
            container.removeView(it.root)
            it.release()
        }
        content = null
        renderedMode = null
    }
}