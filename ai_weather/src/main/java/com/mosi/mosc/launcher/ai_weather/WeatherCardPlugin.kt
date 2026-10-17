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
        const val KEY_CARD_ID = ComposeWeatherCard.KEY_CARD_ID

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

    /** requestShow 与 createCard 共用同一个实例，宿主拿到的 View 也始终是同一个 ComposeView */
    private var weatherCard: ComposeWeatherCard? = null
    private var requestedMode: WeatherDisplayMode? = null
    private var showLatchToken: Runnable? = null

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
        if (mContent == null || mCardManager == null) {
            Log.w(TAG, "onCardUIDateResult ignored: plugin is not initialized")
            return
        }
        if (uiDataList.isNullOrEmpty()) {
            Log.w(TAG, "数据为空：卡片内容不更新")
            return
        }

        repository.update(uiDataList)

        val mode = repository.getDisplayMode()
        val card = weatherCard
        val visible = isCardVisible()

        if (visible && card != null && card.canRefreshInPlace) {
            if (card.canDisplayInCurrentSlot(mode)) {
                // 已经显示且槽位尺寸不变：只更新 Compose 状态，不打扰 launcher 重新编排。
                Log.d(TAG, "card visible, refresh in place: mode=$mode")
                card.onDataChanged()
                clearShowLatch()
            } else {
                // BASE(1/8) ↔ 整卡(1/4) 等尺寸档位变化时仍要让 launcher 重排槽位，
                // 宿主会重新 getView，卡片会读取这次最新数据。
                Log.d(
                    TAG,
                    "card visible but slot=${card.viewCardSize} cannot render mode=$mode, requestShow"
                )
                card.syncDataMode()
                requestShowIfNeeded(mode)
            }
            return
        }

        // 未显示（或只有 ICard 实例、还没 getView）：按时序请求 launcher 显示。
        requestShowIfNeeded(mode)
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
        requestedMode = null
        repository.clear()
        mContent = null
        mCardManager = null
    }

    override fun getSupportedCardIds(): List<String> = listOf(KEY_CARD_ID)

    override fun createCard(cardId: String, triggerSource: TriggerSource): ICard {
        Log.d(TAG, "createCard cardId=$cardId trigger=$triggerSource")
        val card = ensureCard()
            ?: throw IllegalStateException("WeatherCardPlugin.initialize() not called")
        card.syncDataMode()

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
        clearShowLatch()
    }

    override fun getBusinessType(): BusinessType = BusinessType.WEATHER

    // ---------------------------------------------------------------- 内部实现

    private fun ensureCard(): ComposeWeatherCard? {
        val ctx = mContent ?: return null
        return weatherCard ?: ComposeWeatherCard(ctx, repository).also { weatherCard = it }
    }

    /** 宿主当前是否已经把这张卡放进桌面。 */
    private fun isCardVisible(): Boolean {
        val manager = mCardManager ?: return false
        return try {
            manager.getCardVisibleState(KEY_CARD_ID) == VisibleState.SHOW
        } catch (e: Exception) {
            Log.w(TAG, "getCardVisibleState failed", e)
            false
        }
    }

    /** 同一模式只保持一次未完成的申请，避免 AIDL 连续回调导致重复 requestShow。 */
    private fun requestShowIfNeeded(mode: WeatherDisplayMode) {
        if (requestedMode == mode) return
        requestShow(mode)
    }

    private fun requestShow(mode: WeatherDisplayMode) {
        val manager = mCardManager ?: return

        clearShowLatch()
        requestedMode = mode
        try {
            manager.requestShow(
                BusinessType.WEATHER,
                CardRequestInfo(
                    cardId = KEY_CARD_ID,
                    category = "weather",
                    contentSummary = mode.summary,
                    cardSize = mode.cardSize,
                    supportedSizes = ComposeWeatherCard.SUPPORTED_SIZES,
                    source = TriggerSource.RECOMMEND,
                    targetScreen = TargetScreen.DRIVER_SCREEN,
                    reason = mode.showReason,
                    priority = mode.priority
                )
            )
            Log.d(TAG, "requestShow sent: mode=$mode size=${mode.cardSize}")
        } catch (e: Exception) {
            Log.e(TAG, "requestShow failed", e)
            requestedMode = null
            showLatchToken = null
            return
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
