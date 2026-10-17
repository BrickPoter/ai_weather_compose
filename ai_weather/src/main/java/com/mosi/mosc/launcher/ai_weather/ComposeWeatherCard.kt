package com.mosi.mosc.launcher.ai_weather

import android.content.Context
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.doOnLayout
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
 * 宿主拿到的始终是同一个 [composeView]，卡片模式、数据和可见性都通过 Compose 状态原地更新，
 * 不再像 View 版本那样创建/移除具体的 WeatherCardView。
 */
class ComposeWeatherCard(
    context: Context,
    private val repository: WeatherRepository,
    private val cardId: String = KEY_CARD_ID
) : ICard {

    companion object {
        const val TAG = "ComposeWeatherCard"
        val SUPPORTED_SIZES = listOf(CardSize.ONE_FOUR, CardSize.ONE_EIGHT)
        const val KEY_CARD_ID = "composeWeather_1"
    }
    private val composeView = ComposeView(context)
    private val uiState = mutableStateOf(CardUIState())

    /** 宿主最后一次传入的槽位尺寸；null = 宿主还没取过 View。 */
    @Volatile
    private var hostSize: CardSize? = null

    /** 宿主实际用来承载卡片的槽位尺寸；null = getView 还没调用。 */
    @Volatile
    var viewCardSize: CardSize? = null
        private set

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

    /** 当前有可见的 Compose 内容，可以直接原地刷新。 */
    val canRefreshInPlace: Boolean
        get() = !contentReleased && viewCardSize != null

    init {
        Log.d(TAG, "ComposeWeatherCard init")

        composeView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        composeView.visibility = View.GONE
        composeView.setContent {
            ComposeWeatherCardContent(uiState.value)
        }
    }

    override fun getCardId() = cardId

    override fun getSupportedSizes(): List<CardSize> = SUPPORTED_SIZES

    override fun getView(cardSize: CardSize): View {
        Log.d(TAG, "getView() Called, cardSize = $cardSize")
        hostSize = cardSize
        viewCardSize = cardSize
        contentReleased = false
        render(repository.currentData)

        composeView.doOnLayout { v ->
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
        return composeView
    }

    override fun getCardSize(): CardSize = dataMode.cardSize

    override fun getCardPriority(): CardPriority = dataMode.priority

    override fun getBusinessType(): BusinessType = BusinessType.WEATHER

    override fun onRefresh() {
        // 当前 launcher 不回调 onVisibleStateChange，getView 后即视为有可刷新内容。
        if (contentReleased || hostSize == null) return
        repository.requestRefresh()
        render(repository.currentData)
    }

    override fun onVisibleStateChange(state: VisibleState) {
        when (state) {
            VisibleState.SHOW -> {
                contentReleased = false
                onMain {
                    renderNow(repository.currentData)
                }
            }

            VisibleState.HIDE -> {
                contentReleased = true
                onMain {
                    // 与 WeatherCard.releaseContent() 对齐：隐藏时释放 Compose 内容资源，
                    // 但保留 container 和 ComposeView，下一次 SHOW 直接重新组合。
                    composeView.disposeComposition()
                    composeView.visibility = View.GONE
                    uiState.value = uiState.value.copy(visible = false)
                }
            }
        }
    }

    /** 宿主 destroyCard 时调用。 */
    fun release() {
        contentReleased = true
        hostSize = null
        viewCardSize = null
        onMain {
            composeView.disposeComposition()
            composeView.visibility = View.GONE
            uiState.value = CardUIState()
        }
    }

    /** 数据更新：原地刷新。 */
    fun onDataChanged() {
        syncDataMode()
        Log.d(TAG, "onDataChanged Called, dataMode=$dataMode, attached=${hostSize != null}")
        if (hostSize != null) {
            render(repository.currentData)
            composeView.requestLayout()
        }
    }

    /** 只同步数据模式，不触发 Compose 内容切换（等待 launcher 重排槽位时使用）。 */
    fun syncDataMode() {
        dataMode = repository.getDisplayMode()
    }

    /**
     * 当前槽位能否直接承载 [mode]。
     *
     * BASE 在 ONE_FOUR 槽位下会自动渲染成 BASE_LARGE，因此这两种组合不需要重新 requestShow。
     */
    fun canDisplayInCurrentSlot(mode: WeatherDisplayMode): Boolean {
        val currentSize = viewCardSize ?: return false
        val desiredSize = if (mode == WeatherDisplayMode.BASE && currentSize == CardSize.ONE_FOUR) {
            CardSize.ONE_FOUR
        } else {
            mode.cardSize
        }
        return desiredSize == currentSize
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
        uiState.value = CardUIState(
            mode = displayMode,
            data = if (canRender) renderableData else emptyList(),
            visible = showContent
        )
        composeView.visibility = if (showContent) View.VISIBLE else View.GONE

        if (showContent) {
            // GONE -> VISIBLE 时重新触发 ComposeView.onMeasure，确保释放过的 composition 被重建。
            composeView.requestLayout()
        }
    }

    private fun WeatherDisplayMode.canRender(data: List<CardUIData>): Boolean {
        return this != WeatherDisplayMode.COMPARE || data.size >= 2
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            composeView.post(block)
        }
    }
}

private data class CardUIState(
    val mode: WeatherDisplayMode = WeatherDisplayMode.BASE,
    val data: List<CardUIData> = emptyList(),
    val visible: Boolean = false
)

@Composable
private fun ComposeWeatherCardContent(state: CardUIState) {
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
