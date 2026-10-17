package com.mosi.mosc.launcher.ai_weather

import android.content.Context
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.fawvw.hmi.ailauncher.pluginlib.BusinessType
import com.fawvw.hmi.ailauncher.pluginlib.CardPriority
import com.fawvw.hmi.ailauncher.pluginlib.CardSize
import com.fawvw.hmi.ailauncher.pluginlib.ICard
import com.fawvw.hmi.ailauncher.pluginlib.VisibleState
import com.mosi.mosc.ai_weather.weather_srv.CardUIData
import com.mosi.mosc.launcher.ai_weather.compose.cardview.BaseCarLargeView
import com.mosi.mosc.launcher.ai_weather.compose.cardview.BaseCardView
import com.mosi.mosc.launcher.ai_weather.compose.cardview.CompareCardView
import com.mosi.mosc.launcher.ai_weather.compose.cardview.ExpandCardView
import com.mosi.mosc.launcher.ai_weather.compose.cardview.ForecastCardView
import com.mosi.mosc.launcher.ai_weather.compose.cardview.RealtimeCardView
import com.mosi.mosc.launcher.ai_weather.enums.WeatherDisplayMode

/**
 * [WeatherCard] 的 Compose 版本。
 *
 * 宿主拿到的始终是同一个 [container]，卡片模式、数据和可见性都通过 Compose 状态原地更新，
 * 不再像 View 版本那样创建/移除具体的 WeatherCardView。
 */
class ComposeWeatherCard(
    context: Context,
    private val repository: WeatherRepository,
    private val cardId: String = KEY_CARD_ID
) : ICard {

    companion object {
        const val TAG = "ComposeWeatherCard"
        private val SUPPORTED_SIZES = listOf(CardSize.ONE_FOUR, CardSize.ONE_EIGHT)
        const val KEY_CARD_ID = "composeWeather_1"
    }

    /** 交给宿主的稳定 View；卡片生命周期内不替换。 */
    private val container = FrameLayout(context)
    private val composeView = ComposeView(context)
    private val uiState = mutableStateOf(CardUiState())

    /**
     * 部分 launcher 宿主没有给 View 树设置 ViewTreeLifecycleOwner。
     * 这种情况下由卡片临时把 owner 挂到根 View 上，保证 ComposeView 可以创建 Recomposer。
     */
    private val fallbackLifecycleOwner = CardLifecycleOwner()
    private var fallbackLifecycleRoot: View? = null

    /** 宿主最后一次传入的槽位尺寸；null = 宿主还没取过 View。 */
    private var hostSize: CardSize? = null

    /** 是否可见，由 onVisibleStateChange 维护。 */
    @Volatile
    private var visible = false

    /** 内容是否已释放（HIDE / release 后为 true，getView / SHOW 后恢复）。 */
    @Volatile
    private var contentReleased = true

    /** 数据期望的展示模式，供 requestShow / getCardSize 使用。 */
    @Volatile
    var dataMode: WeatherDisplayMode = repository.getDisplayMode()
        private set

    /** 当前实际渲染的模式（受槽位尺寸影响，可能与 dataMode 不同）。 */
    @Volatile
    var currentMode: WeatherDisplayMode = WeatherDisplayMode.BASE
        private set

    init {
        Log.d(TAG, "ComposeWeatherCard init")

        container.addView(
            composeView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        composeView.visibility = View.GONE
        composeView.setContent {
            ComposeWeatherCardContent(uiState.value)
        }

        container.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    ensureLifecycleOwner()
                }

                override fun onViewDetachedFromWindow(v: View) {
                    // ComposeView 默认在 detach 时释放 composition，此处无需额外处理。
                }
            }
        )
    }

    override fun getCardId() = cardId

    override fun getSupportedSizes(): List<CardSize> = SUPPORTED_SIZES

    override fun getView(cardSize: CardSize): View {
        Log.d(TAG, "getView() Called, cardSize = $cardSize")
        hostSize = cardSize
        contentReleased = false
        ensureLifecycleOwner()
        render(repository.currentData)

        container.doOnLayout { v ->
            val dm = v.resources.displayMetrics
            Log.d(
                TAG,
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
                contentReleased = false
                onMain {
                    ensureLifecycleOwner()
                    fallbackLifecycleOwner.resume()
                    renderNow(repository.currentData)
                }
            }

            VisibleState.HIDE -> {
                visible = false
                contentReleased = true
                onMain {
                    // 与 WeatherCard.releaseContent() 对齐：隐藏时释放 Compose 内容资源，
                    // 但保留 container 和 ComposeView，下一次 SHOW 直接重新组合。
                    composeView.disposeComposition()
                    composeView.visibility = View.GONE
                    fallbackLifecycleOwner.pause()
                    uiState.value = uiState.value.copy(visible = false)
                    releaseFallbackLifecycleOwner()
                }
            }
        }
    }

    /** 宿主 destroyCard 时调用。 */
    fun release() {
        visible = false
        contentReleased = true
        hostSize = null
        onMain {
            composeView.disposeComposition()
            composeView.visibility = View.GONE
            fallbackLifecycleOwner.pause()
            uiState.value = CardUiState()
            releaseFallbackLifecycleOwner()
        }
    }

    /** 数据更新：原地刷新。 */
    fun onDataChanged() {
        dataMode = repository.getDisplayMode()
        Log.d(TAG, "onDataChanged Called, dataMode=$dataMode, attached=${hostSize != null}")
        if (hostSize != null) {
            render(repository.currentData)
            container.requestLayout()
        }
    }

    private fun render(data: List<CardUIData>?) {
        onMain { renderNow(data) }
    }

    private fun renderNow(data: List<CardUIData>?) {
        dataMode = repository.getDisplayMode()

        var displayMode = dataMode
        if (displayMode == WeatherDisplayMode.BASE && hostSize == CardSize.ONE_FOUR) {
            displayMode = WeatherDisplayMode.BASE_LARGE
        }

        currentMode = displayMode
        Log.d(TAG, "render dataMode=$dataMode, renderMode=$displayMode, hostSize=$hostSize")

        val renderableData = data.orEmpty().toList()
        val canRender = renderableData.isNotEmpty() && displayMode.canRender(renderableData)
        val showContent = !contentReleased && canRender
        uiState.value = CardUiState(
            mode = displayMode,
            data = if (canRender) renderableData else emptyList(),
            visible = showContent
        )
        composeView.visibility = if (showContent) View.VISIBLE else View.GONE

        if (showContent) {
            fallbackLifecycleOwner.resume()
            // GONE -> VISIBLE 时重新触发 ComposeView.onMeasure，确保释放过的 composition 被重建。
            composeView.requestLayout()
        }
    }

    private fun WeatherDisplayMode.canRender(data: List<CardUIData>): Boolean {
        return this != WeatherDisplayMode.COMPARE || data.size >= 2
    }

    private fun ensureLifecycleOwner() {
        if (!container.isAttachedToWindow) return
        val root = container.rootView
        if (root.findViewTreeLifecycleOwner() == null) {
            root.setViewTreeLifecycleOwner(fallbackLifecycleOwner)
            fallbackLifecycleRoot = root
        }
    }

    private fun releaseFallbackLifecycleOwner() {
        val root = fallbackLifecycleRoot ?: return
        if (root.findViewTreeLifecycleOwner() === fallbackLifecycleOwner) {
            root.setViewTreeLifecycleOwner(null)
        }
        fallbackLifecycleRoot = null
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            container.post(block)
        }
    }

    private class CardLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this).apply {
            currentState = Lifecycle.State.CREATED
        }

        override val lifecycle: Lifecycle
            get() = registry

        fun resume() {
            if (registry.currentState != Lifecycle.State.RESUMED) {
                registry.currentState = Lifecycle.State.RESUMED
            }
        }

        fun pause() {
            if (registry.currentState != Lifecycle.State.CREATED) {
                registry.currentState = Lifecycle.State.CREATED
            }
        }
    }
}

private data class CardUiState(
    val mode: WeatherDisplayMode = WeatherDisplayMode.BASE,
    val data: List<CardUIData> = emptyList(),
    val visible: Boolean = false
)

@Composable
private fun ComposeWeatherCardContent(state: CardUiState) {
    if (!state.visible || state.data.isEmpty()) return

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        when (state.mode) {
            WeatherDisplayMode.BASE -> BaseCardView(cardUIDataList = state.data)
            WeatherDisplayMode.BASE_LARGE -> BaseCarLargeView(cardUIDataList = state.data)
            WeatherDisplayMode.COMPARE -> CompareCardView(cardUIDataList = state.data)
            WeatherDisplayMode.EXPAND -> ExpandCardView(cardUIDataList = state.data)
            WeatherDisplayMode.FORECAST -> ForecastCardView(cardUIDataList = state.data)
            WeatherDisplayMode.REALTIME -> RealtimeCardView(cardUIDataList = state.data)
        }
    }
}
