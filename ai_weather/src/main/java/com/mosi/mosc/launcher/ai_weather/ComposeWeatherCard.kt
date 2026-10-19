package com.mosi.mosc.launcher.ai_weather

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.fawvw.hmi.ailauncher.pluginlib.BusinessType
import com.fawvw.hmi.ailauncher.pluginlib.CardPriority
import com.fawvw.hmi.ailauncher.pluginlib.CardSize
import com.fawvw.hmi.ailauncher.pluginlib.ICard
import com.fawvw.hmi.ailauncher.pluginlib.VisibleState
import com.mosi.mosc.launcher.ai_weather.compose.cardview.BaseCarLargeView
import com.mosi.mosc.launcher.ai_weather.compose.cardview.BaseCardView
import com.mosi.mosc.launcher.ai_weather.compose.cardview.CompareCardView
import com.mosi.mosc.launcher.ai_weather.compose.cardview.ExpandCardView
import com.mosi.mosc.launcher.ai_weather.compose.cardview.ForecastCardView
import com.mosi.mosc.launcher.ai_weather.compose.cardview.RealtimeCardView
import com.mosi.mosc.launcher.ai_weather.enums.WeatherCardType
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 天气卡片的 ICard 适配层。
 *
 * 职责：
 * 1. 向宿主提供稳定的 View；
 * 2. 记录宿主传入的槽位尺寸；
 * 3. 按 WeatherSnapshot + CardSize 选择 Compose 内容；
 * 4. 在 destroyCard 时释放 Compose composition。
 *
 */
class ComposeWeatherCard(
    context: Context,
    private val repository: WeatherRepository,
    private val cardId: String = KEY_CARD_ID,
    private val onViewReady: () -> Unit = {}
) : ICard {

    companion object {
        const val TAG = "ComposeWeatherCard"
        const val KEY_CARD_ID = "weather_1"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val slotSize = MutableStateFlow<CardSize?>(null)
    private val composeView = ComposeView(context)

    private var compositionInstalled = false

    init {
        Log.d(TAG, "ComposeWeatherCard init")
        installComposition()
    }

    /** 宿主当前是否把这张卡的 View 挂到了窗口上。 */
    val isAttached: Boolean
        get() = composeView.isAttachedToWindow

    /** 宿主最近一次传给 getView() 的槽位尺寸。 */
    val currentSlotSize: CardSize?
        get() = slotSize.value

    override fun getCardId(): String = cardId

    override fun getSupportedSizes(): List<CardSize> =
        repository.currentSnapshot?.type?.supportedSizes
            ?: WeatherCardType.BASE.supportedSizes

    override fun getView(cardSize: CardSize): View {
        Log.d(TAG, "getView() Called, cardSize = $cardSize")
        slotSize.value = cardSize
        installComposition()
        composeView.visibility = View.VISIBLE
        composeView.requestLayout()
        onViewReady()
        return composeView
    }

    override fun getCardSize(): CardSize =
        currentSlotSize
            ?: repository.currentSnapshot?.type?.preferredSize
            ?: CardSize.ONE_EIGHT

    override fun getCardPriority(): CardPriority =
        repository.currentSnapshot?.type?.priority ?: CardPriority.MEDIUM

    override fun getBusinessType(): BusinessType = BusinessType.WEATHER

    /**
     * 宿主当前不会调用 onRefresh()，这里只重新排版当前快照，不主动请求服务端。
     * 新数据统一由 WeatherRepository.snapshot 推送到 Compose。
     */
    override fun onRefresh() {
        composeView.requestLayout()
    }

    /**
     * 显隐由宿主调度。HIDE 时不释放 composition，避免宿主再次 SHOW 时需要重建整棵 UI；
     * 最终资源释放放在 destroyCard() -> release()。
     */
    override fun onVisibleStateChange(state: VisibleState) {
        if (state == VisibleState.SHOW) {
            installComposition()
            composeView.visibility = View.VISIBLE
            onRefresh()
        }
    }

    /** 当前槽位能否直接承载这份快照。 */
    fun canRender(snapshot: WeatherSnapshot?): Boolean {
        val currentSize = currentSlotSize ?: return false
        return snapshot != null && currentSize in snapshot.type.supportedSizes
    }

    /**
     * ICard 没有 onDestroy 回调，宿主销毁卡片时由 WeatherCardPlugin.destroyCard() 调用。
     */
    fun onDestroy() {
        runOnMain {
            slotSize.value = null
            if (compositionInstalled) {
                composeView.disposeComposition()
                compositionInstalled = false
            }
            composeView.visibility = View.GONE
        }
    }

    private fun installComposition() {
        if (compositionInstalled) return
        composeView.setContent {
            val snapshot by repository.snapshot.collectAsState()
            val size by slotSize.collectAsState()
            WeatherCardContent(latestSnapshot = snapshot, slotSize = size)
        }
        compositionInstalled = true
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}

@Composable
private fun WeatherCardContent(
    latestSnapshot: WeatherSnapshot?,
    slotSize: CardSize?
) {
    var displayedSnapshot by remember { mutableStateOf<WeatherSnapshot?>(null) }
    val canDisplayLatest = latestSnapshot != null &&
        (slotSize == null || slotSize in latestSnapshot.type.supportedSizes)

    LaunchedEffect(latestSnapshot, slotSize) {
        when {
            latestSnapshot == null -> displayedSnapshot = null
            canDisplayLatest -> displayedSnapshot = latestSnapshot
        }
    }

    // 类型变化需要宿主重新编排到 1/4 时，先保留旧内容，避免在 1/8 槽位里短暂渲染大卡。
    val snapshot = if (canDisplayLatest) latestSnapshot else displayedSnapshot
    if (snapshot == null || snapshot.items.isEmpty()) return

    val renderSize = slotSize ?: snapshot.type.preferredSize
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        when (snapshot.type) {
            WeatherCardType.BASE -> {
                if (renderSize == CardSize.ONE_FOUR) {
                    BaseCarLargeView(cardUIDataList = snapshot.items)
                } else {
                    BaseCardView(cardUIDataList = snapshot.items)
                }
            }

            WeatherCardType.COMPARE -> {
                CompareCardView(cardUIDataList = snapshot.items)
            }

            WeatherCardType.EXPAND -> {
                ExpandCardView(cardUIDataList = snapshot.items)
            }

            WeatherCardType.FORECAST -> {
                ForecastCardView(cardUIDataList = snapshot.items)
            }

            WeatherCardType.REALTIME -> {
                RealtimeCardView(cardUIDataList = snapshot.items)
            }
        }
    }
}
