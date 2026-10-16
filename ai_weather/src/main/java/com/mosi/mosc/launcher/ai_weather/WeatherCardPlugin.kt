package com.mosi.mosc.launcher.ai_weather

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.fawvw.hmi.ailauncher.pluginlib.BusinessType
import com.fawvw.hmi.ailauncher.pluginlib.CardRequestInfo
import com.fawvw.hmi.ailauncher.pluginlib.ICard
import com.fawvw.hmi.ailauncher.pluginlib.ICardManager
import com.fawvw.hmi.ailauncher.pluginlib.ICardPlugin
import com.fawvw.hmi.ailauncher.pluginlib.TargetScreen
import com.fawvw.hmi.ailauncher.pluginlib.TriggerSource
import com.fawvw.hmi.ailauncher.pluginlib.VisibleState
import com.mosi.mosc.ai_weather.weather_srv.CardUIData
import com.mosi.mosc.ai_weather.weather_srv.IAIDLWeatherCallback
import com.mosi.mosc.ai_weather.weather_srv.IAIDLWeatherService
import com.mosi.mosc.launcher.ai_weather.enums.WeatherDisplayMode

class WeatherCardPlugin : ICardPlugin {

    companion object {
        const val TAG = "WeatherCardPlugin"
        const val KEY_CARD_ID = WeatherCard.KEY_CARD_ID

        private const val MSG_IDLE_HIDE = 1
        private const val IDLE_HIDE_DELAY_MS = 20_000L      // 用户调起后无操作自动收起
        private const val RETRY_MAX = 30
        private const val SHOW_LATCH_TIMEOUT_MS = 3_000L    // 宿主未响应时解锁
    }

    private var mContent: Context? = null
    private var mCardManager: ICardManager? = null
    private var weatherService: IAIDLWeatherService? = null
    private var isBound = false
    private var retryCount = 0

    private val repository = WeatherRepository()

    /** requestShow 与 createCard 共用同一个实例，宿主拿到的 View 也始终是同一个 container */
    private var weatherCard: WeatherCard? = null
    private var requestedMode: WeatherDisplayMode? = null
    /** 上一次已申请过的数据模式（用于判断"数据形态变化后要重新申请"） */
    private var lastRequestedMode: WeatherDisplayMode? = null
    private var showLatchToken: Runnable? = null
    private var wasVisible = false

    private val mHandle: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_IDLE_HIDE -> {
                    Log.d(TAG, "idle timeout -> requestHide")
                    mCardManager?.requestHide(KEY_CARD_ID)
                    clearShowLatch()
                }
            }
        }
    }

    // ---------------------------------------------------------------- 数据入口

    /** 服务端 requestWeatherData 的异步结果 */
    private val callAIDLBack = object : IAIDLWeatherCallback.Stub() {
        override fun onCardUIDateResult(uiDataList: List<CardUIData>?) {
            Log.d(TAG, "onCardUIDateResult Called")
            Log.d(TAG, "uiDataList = $uiDataList")
            Log.d(TAG, "uiDataList's size=${uiDataList?.size ?: 0}")
            mHandle.post { onWeatherData(uiDataList) }
        }
    }

    private fun onWeatherData(uiDataList: List<CardUIData>?) {
        if (uiDataList.isNullOrEmpty()) {
            Log.w(TAG, "数据为空：卡片内容不更新")
//            Log.w(TAG, "数据为空：释放内容并收起卡片")
//            repository.update(emptyList())
//            weatherCard?.onDataChanged()
//            clearShowLatch()
//            try {
//                mCardManager?.requestHide(KEY_CARD_ID)
//            } catch (e: Exception) {
//                Log.e(TAG, "requestHide failed", e)
//            }
            return
        }

        repository.update(uiDataList)
        ensureCard()?.onDataChanged()   // 首次：只更新 dataMode，不 render；已上屏：原地刷新
        maybeShowCard()
    }

    // ---------------------------------------------------------------- 绑定服务端
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "onServiceConnected")
            weatherService = IAIDLWeatherService.Stub.asInterface(service)
            isBound = true

            // ICard.onRefresh() 会被转发到这里（注意：launcher 目前不会主动调 onRefresh）
            repository.setRefreshAction { requestWeatherDataSafely("refresh") }

            // ★ 首次拉取：绑上就请求一次，否则永远没有数据
            requestWeatherDataSafely("onConnected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "onServiceDisconnected")
            weatherService = null
            isBound = false
        }
    }

    /**
     * 主动向天气服务端拉取一次数据（异步，不阻塞调用线程）。
     *
     * 链路说明：
     * - 请求经 AIDL 发给天气服务端，结果通过 [callAIDLBack].onCardUIDateResult 异步回传；
     * - 只有 [weatherService] 已就绪（即 onServiceConnected 已回调）时才有效，
     *   未就绪会直接跳过并打日志，不会产生任何回调 —— 此时依赖 onServiceConnected 里的首次拉取兜底。
     *
     * 调用来源（仅是排查标记，不影响逻辑）：
     * - "onConnected"：服务绑定成功后的首次拉取
     * - "userTrigger"：用户主动调起卡片，需要最新数据
     * - "refresh"：ICard.onRefresh() 转发过来的刷新请求
     *
     * @param from 调用来源标记，仅用于日志定位问题。
     */
    private fun requestWeatherDataSafely(from: String) {
        Log.d(TAG, "requestWeatherDataSafely() Called")
        val svc = weatherService
        if (svc == null) {
            Log.w(TAG, "requestWeatherData 跳过（$from）：service 未就绪")
            return
        }
        try {
            Log.d(TAG, "requestWeatherData")
            svc.requestWeatherData(callAIDLBack)
        } catch (e: Exception) {
            Log.e(TAG, "requestWeatherData failed", e)
        }
    }

    private fun bindAIDLService() {

        mHandle.postDelayed({
            try {
                val bindIntent = Intent("com.mosi.mosc.ai_weather.APP_AIDL_SERVICE").apply {
                    setPackage("com.mosi.mosc.ai_weather")
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                }
                val result = mContent?.bindService(bindIntent, connection, Context.BIND_AUTO_CREATE) ?: false
                Log.d(TAG, "bindService result=$result retry=$retryCount")
                if (!result) {
                    retryCount++
                    if (retryCount <= RETRY_MAX) {
                        mHandle.postDelayed({ bindAIDLService() }, 1000)
                    }
                }
            } catch (e: SecurityException) {
                retryCount = 0
                Log.e(TAG, "bindService SecurityException（检查 queries / exported）", e)
            }
        }, 300)
    }

    // ---------------------------------------------------------------- ICardPlugin

    override fun initialize(context: Context, cardManager: ICardManager, lifecycleOwner: LifecycleOwner) {
        Log.d(TAG, "initialize: ctx=${context.javaClass.simpleName}")
        mContent = context

        val dm = context.resources.displayMetrics
        Log.i(TAG, "screen=${dm.widthPixels}x${dm.heightPixels}px " +
                "density=${dm.density} dpi=${dm.densityDpi} " +
                "screenDp=${context.resources.configuration.screenWidthDp}x" +
                "${context.resources.configuration.screenHeightDp}")

        mCardManager = cardManager
        retryCount = 0
        bindAIDLService()
    }

    override fun unInitialize() {
        Log.d(TAG, "unInitialize")
        mHandle.removeCallbacksAndMessages(null)
        clearShowLatch()

        if (isBound) {
            try {
                mContent?.unbindService(connection)
            } catch (e: Exception) {
                Log.w(TAG, "unbindService failed", e)
            }
        }

        isBound = false
        weatherService = null
        weatherCard?.release()
        weatherCard = null
        wasVisible = false
        requestedMode = null
        repository.clear()
        mContent = null
        mCardManager = null
        lastRequestedMode = null
    }

    override fun getSupportedCardIds(): List<String> = listOf(KEY_CARD_ID)

    override fun createCard(cardId: String, triggerSource: TriggerSource): ICard {
        Log.d(TAG, "createCard cardId=$cardId trigger=$triggerSource")
        val card = ensureCard()
            ?: throw IllegalStateException("WeatherCardPlugin.initialize() not called")

        if (triggerSource == TriggerSource.USER) {
            // 用户主动调起 → 要最新数据
            requestWeatherDataSafely("userTrigger")
            mHandle.removeMessages(MSG_IDLE_HIDE)
            mHandle.sendEmptyMessageDelayed(MSG_IDLE_HIDE, IDLE_HIDE_DELAY_MS)
        }
        return card
    }

    override fun destroyCard(cardId: String) {
        Log.d(TAG, "destroyCard cardId=$cardId")
        mHandle.removeMessages(MSG_IDLE_HIDE)
        weatherCard?.release()
        weatherCard = null
        wasVisible = false
        lastRequestedMode = null
        clearShowLatch()
    }

    override fun getBusinessType(): BusinessType = BusinessType.WEATHER

    // ---------------------------------------------------------------- 内部实现

    private fun ensureCard(): WeatherCard? {
        val ctx = mContent ?: return null
        return weatherCard ?: WeatherCard(ctx, repository).also { weatherCard = it }
    }

    /** 已可见 → 只解锁；不可见 → 同一内容不重复申请，否则 requestShow */
    private fun maybeShowCard() {
        val card = weatherCard ?: return
        val manager = mCardManager ?: return
        val mode = card.dataMode                       // ★ 用数据期望模式（不再用 currentMode）

        val visible = try {
            manager.getCardVisibleState(KEY_CARD_ID) == VisibleState.SHOW
        } catch (e: Exception) {
            Log.w(TAG, "getCardVisibleState failed", e)
            false
        }

        if (visible) {
            wasVisible = true
//            clearShowLatch()
            // ★ 数据形态变了（BASE↔整卡、整卡之间切换）→ 重新 requestShow，让宿主重新编排
            if (lastRequestedMode != mode) {
                Log.d(TAG, "mode 变化：$lastRequestedMode -> $mode，重新 requestShow")
                requestShow(mode)
            } else {
                clearShowLatch()
            }
            return
        }
        if (wasVisible) {                    // 观察到 SHOW → HIDE 跳变，解锁
            wasVisible = false
            clearShowLatch()
        }

//        val mode = card.currentMode
        if (requestedMode == mode) return
        requestShow(mode)
    }

    private fun requestShow(mode: WeatherDisplayMode) {
        val card = ensureCard() ?: return
        val manager = mCardManager ?: return

        clearShowLatch()
        requestedMode = mode
        lastRequestedMode = mode
        try {
            manager.requestShow(
                BusinessType.WEATHER,
                CardRequestInfo(
                    cardId = KEY_CARD_ID,
                    category = "weather",
                    contentSummary = mode.summary,
                    cardSize = mode.cardSize,
                    supportedSizes = card.getSupportedSizes(),
                    source = TriggerSource.RECOMMEND,
                    targetScreen = TargetScreen.DRIVER_SCREEN,
                    reason = mode.showReason,
                    priority = card.getCardPriority()
                )
            )
            Log.d(TAG, "requestShow sent: mode=$mode size=${card.getCardSize()}")
        } catch (e: Exception) {
            Log.e(TAG, "requestShow failed", e)
        }

        // 宿主未响应（拒绝/无槽位）时解锁，避免一次失败后不再申请
        val token = Runnable {
            requestedMode = null
            showLatchToken = null
        }
        showLatchToken = token
        mHandle.postDelayed(token, SHOW_LATCH_TIMEOUT_MS)
    }

    private fun clearShowLatch() {
        showLatchToken?.let { mHandle.removeCallbacks(it) }
        showLatchToken = null
        requestedMode = null
    }
}