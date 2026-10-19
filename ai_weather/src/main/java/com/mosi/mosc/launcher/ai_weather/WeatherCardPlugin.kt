package com.mosi.mosc.launcher.ai_weather

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.fawvw.hmi.ailauncher.pluginlib.BusinessType
import com.fawvw.hmi.ailauncher.pluginlib.CardRequestInfo
import com.fawvw.hmi.ailauncher.pluginlib.CardSize
import com.fawvw.hmi.ailauncher.pluginlib.ICard
import com.fawvw.hmi.ailauncher.pluginlib.ICardManager
import com.fawvw.hmi.ailauncher.pluginlib.ICardPlugin
import com.fawvw.hmi.ailauncher.pluginlib.TargetScreen
import com.fawvw.hmi.ailauncher.pluginlib.TriggerSource
import com.mosi.mosc.ai_weather.weather_srv.CardUIData
import com.mosi.mosc.ai_weather.weather_srv.IAIDLWeatherCallback
import com.mosi.mosc.ai_weather.weather_srv.IAIDLWeatherService
import com.mosi.mosc.launcher.ai_weather.enums.WeatherCardType

/**
 * 插件生命周期与显示调度。
 *
 * 数据链路只有一条：
 * WeatherAIDLServices.onCardUIDateResult() -> WeatherRepository -> ComposeWeatherCard。
 * 这里不再通过 ICardManager.getCardVisibleState() 判断可见性，也不实现自动收起；
 * 显隐由宿主调度。
 */
class WeatherCardPlugin : ICardPlugin {

    companion object {
        const val TAG = "WeatherCardPlugin"
        const val KEY_CARD_ID = ComposeWeatherCard.KEY_CARD_ID

        private const val RETRY_MAX = 30
        private const val SHOW_REQUEST_TIMEOUT_MS = 3_000L
    }

    private var hostContext: Context? = null
    private var cardManager: ICardManager? = null
    private var weatherService: IAIDLWeatherService? = null

    /** bindService 已经成功发起，或当前已经连接。 */
    private var bound = false
    private var retryCount = 0

    private val repository = WeatherRepository()
    private var weatherCard: ComposeWeatherCard? = null

    private var pendingShowType: WeatherCardType? = null
    private var pendingShowSize: CardSize? = null
    private var showRequestToken: Runnable? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    // ---------------------------------------------------------------- 数据入口

    private val weatherCallback = object : IAIDLWeatherCallback.Stub() {
        override fun onCardUIDateResult(uiDataList: List<CardUIData>?) {
            Log.d(TAG, "uiDataList: size=${uiDataList?.size}, data = $uiDataList")
            mainHandler.post { onWeatherData(uiDataList) }
        }
    }

    private fun onWeatherData(uiDataList: List<CardUIData>?) {
        if (hostContext == null || cardManager == null) {
            Log.w(TAG, "onCardUIDateResult ignored: plugin is not initialized")
            return
        }
        if (uiDataList.isNullOrEmpty()) {
            Log.w(TAG, "onCardUIDateResult ignored: empty data")
            return
        }

        val snapshot = repository.update(uiDataList) ?: return
        val card = weatherCard

        if (card != null && card.isAttached && card.canRender(snapshot)) {
            Log.d(
                TAG,
                "refresh in place: type=${snapshot.type}, slot=${card.currentSlotSize}"
            )
            card.onRefresh()
            clearShowRequest()
            return
        }

        Log.d(
            TAG,
            "requestShow: type=${snapshot.type}, attached=${card?.isAttached}, " +
                "slot=${card?.currentSlotSize}"
        )
        requestShow(snapshot)
    }

    // ---------------------------------------------------------------- 绑定服务端

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (hostContext == null) return
            Log.d(TAG, "onServiceConnected")
            weatherService = IAIDLWeatherService.Stub.asInterface(service)
            bound = true
            retryCount = 0
            requestWeatherDataSafely()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "onServiceDisconnected")
            weatherService = null
            // 绑定关系仍然存在，保留 bound=true 以便 unInitialize() 能正确 unbind。
        }
    }

    private fun bindAidlService() {
        mainHandler.postDelayed({
            val context = hostContext ?: return@postDelayed
            try {
                val bindIntent = Intent("com.mosi.mosc.ai_weather.APP_AIDL_SERVICE").apply {
                    setPackage("com.mosi.mosc.ai_weather")
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                }
                val result = context.bindService(
                    bindIntent,
                    connection,
                    Context.BIND_AUTO_CREATE
                )
                Log.d(TAG, "bindService result=$result retry=$retryCount")
                if (result) {
                    bound = true
                } else {
                    scheduleRebind()
                }
            } catch (e: Exception) {
                Log.e(TAG, "bindService failed", e)
                scheduleRebind()
            }
        }, 300L)
    }

    private fun scheduleRebind() {
        if (retryCount >= RETRY_MAX) {
            Log.w(TAG, "bindService give up after $RETRY_MAX retries")
            return
        }
        retryCount++
        mainHandler.postDelayed({ bindAidlService() }, 1_000L)
    }

    private fun requestWeatherDataSafely() {
        val service = weatherService ?: return
        try {
            service.requestWeatherData(weatherCallback)
        } catch (e: Exception) {
            Log.e(TAG, "requestWeatherData failed", e)
        }
    }

    // ---------------------------------------------------------------- ICardPlugin

    override fun initialize(
        context: Context,
        cardManager: ICardManager,
        lifecycleOwner: LifecycleOwner
    ) {
        Log.d(TAG, "initialize: composeVersion 0.0.2")
        Log.d(TAG, "ctx=${context.javaClass.simpleName}")
        hostContext = context

        // 日志打印：获取屏幕信息
        val dm = context.resources.displayMetrics
        Log.i(TAG, "screen=${dm.widthPixels}x${dm.heightPixels}px " +
                "density=${dm.density} dpi=${dm.densityDpi} " +
                "screenDp=${context.resources.configuration.screenWidthDp}x" +
                "${context.resources.configuration.screenHeightDp}")

        this.cardManager = cardManager
        retryCount = 0
        bindAidlService()
    }

    override fun unInitialize() {
        Log.d(TAG, "unInitialize")
        mainHandler.removeCallbacksAndMessages(null)
        clearShowRequest()

        if (bound) {
            try {
                hostContext?.unbindService(connection)
            } catch (e: Exception) {
                Log.w(TAG, "unbindService failed", e)
            }
        }

        bound = false
        weatherService = null
        weatherCard?.onDestroy()
        weatherCard = null
        repository.clear()
        hostContext = null
        cardManager = null
    }

    override fun getSupportedCardIds(): List<String> = listOf(KEY_CARD_ID)

    override fun createCard(cardId: String, triggerSource: TriggerSource): ICard {
        require(cardId == KEY_CARD_ID) {
            "Unsupported weather card id: $cardId"
        }
        val context = hostContext
            ?: throw IllegalStateException("WeatherCardPlugin.initialize() not called")

        Log.d(TAG, "createCard cardId=$cardId trigger=$triggerSource")
        val card = weatherCard ?: ComposeWeatherCard(
            context = context,
            repository = repository,
            cardId = KEY_CARD_ID,
            onViewReady = { clearShowRequest() }
        ).also { weatherCard = it }
        return card
    }

    override fun destroyCard(cardId: String) {
        if (cardId != KEY_CARD_ID) return
        Log.d(TAG, "destroyCard cardId=$cardId")
        clearShowRequest()
        weatherCard?.onDestroy()
        weatherCard = null
    }

    override fun getBusinessType(): BusinessType = BusinessType.WEATHER

    // ---------------------------------------------------------------- 显示调度

    private fun requestShow(snapshot: WeatherSnapshot) {
        val manager = cardManager ?: return
        val type = snapshot.type

        if (pendingShowType == type && pendingShowSize == type.preferredSize) {
            return
        }
        clearShowRequest()

        pendingShowType = type
        pendingShowSize = type.preferredSize
        try {
            manager.requestShow(
                BusinessType.WEATHER,
                CardRequestInfo(
                    cardId = KEY_CARD_ID,
                    category = "weather",
                    contentSummary = type.summary,
                    cardSize = type.preferredSize,
                    supportedSizes = type.supportedSizes,
                    source = TriggerSource.RECOMMEND,
                    targetScreen = TargetScreen.DRIVER_SCREEN,
                    reason = type.showReason,
                    priority = type.priority
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "requestShow failed", e)
            clearShowRequest()
            return
        }

        // 宿主没有响应时解锁，下一次数据回调可以重试。
        val token = Runnable { clearShowRequest() }
        showRequestToken = token
        mainHandler.postDelayed(token, SHOW_REQUEST_TIMEOUT_MS)
    }

    private fun clearShowRequest() {
        showRequestToken?.let { mainHandler.removeCallbacks(it) }
        showRequestToken = null
        pendingShowType = null
        pendingShowSize = null
    }
}
