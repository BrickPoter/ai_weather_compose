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
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.fawvw.hmi.ailauncher.pluginlib.BusinessType
import com.fawvw.hmi.ailauncher.pluginlib.CardPriority
import com.fawvw.hmi.ailauncher.pluginlib.CardRequestInfo
import com.fawvw.hmi.ailauncher.pluginlib.CardSize
import com.fawvw.hmi.ailauncher.pluginlib.IAiCloudMessageListener
import com.fawvw.hmi.ailauncher.pluginlib.ICard
import com.fawvw.hmi.ailauncher.pluginlib.ICardManager
import com.fawvw.hmi.ailauncher.pluginlib.ICardPlugin
import com.fawvw.hmi.ailauncher.pluginlib.TargetScreen
import com.fawvw.hmi.ailauncher.pluginlib.TriggerSource
import com.fawvw.hmi.ailauncher.pluginlib.VisibleState
import com.mosi.mosc.ai_weather.been.CardBaseInfo
import com.mosi.mosc.ai_weather.been.CardUIData
import com.mosi.mosc.ai_weather.been.IAIDLWeatherCallback
import com.mosi.mosc.ai_weather.been.IAIDLWeatherService

class WeatherCardPlugin : ICardPlugin, CreateViewFactory.CreateViewListener {
    companion object {
        const val KEY_CARD_ID = "WEATHER_CARD"
    }

    private val TAG = "WeatherCardPlugin"

    private var mContent: Context? = null
    private var mCardManager: ICardManager? = null

    private var weatherService : IAIDLWeatherService? = null

    private val callAIDLBack: IAIDLWeatherCallback by lazy { object : IAIDLWeatherCallback.Stub() {
        override fun onCardBaseInfoResilt(result: CardBaseInfo?) {
            Log.d(TAG,"onCardBaseInfoResilt is running result = $result")
            //返回解析的基础数据
            CreateViewFactory.showBaseCard(result,mContent)

            mCardManager?.requestShow(BusinessType.WEATHER, CardRequestInfo(cardId = KEY_CARD_ID,
                category = "weather",
                contentSummary ="卡片正文",
                cardSize = CardSize.ONE_EIGHT,
                supportedSizes = listOf(CardSize.ONE_FOUR, CardSize.ONE_EIGHT),
                source = TriggerSource.RECOMMEND,
                targetScreen = TargetScreen.DRIVER_SCREEN,
                reason = "显示",
                priority = CardPriority.MEDIUM
            ))
        }

        override fun onCardUIDateResult(cardUIData: CardUIData?) {
            TODO("Not yet implemented")
        }
    } }

    private var isBound = false

    private var retryCount = 0

    private val connection by lazy { object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            Log.d(TAG,"ServiceConnection onServiceConnected is running")
            weatherService = IAIDLWeatherService.Stub.asInterface(service)
            try {
                Log.d(TAG,"ServiceConnection mCardManager = $mCardManager")
                mCardManager?.getAiCloudService()?.registerMessageListener(BusinessType.WEATHER,object :
                    IAiCloudMessageListener {
                    override fun dispatchMessage(message: String) {
//                        weatherService?.sendWeatherData("开始请求")
                        Log.d(TAG,"ServiceConnection getAiCloudService registerMessageListener dispatchMessage = $message")
                        weatherService?.requestWeatherData("req_${System.currentTimeMillis()}",message,callAIDLBack)
                    }

                })
            }catch (e: Exception){

            }
            isBound = true

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG,"onServiceDisconnected is running name = $name")
            weatherService = null
            mCardManager?.getAiCloudService()?.unregisterMessageListener(BusinessType.WEATHER)
            isBound = false
        }

    } }

    private fun bindAIDLService() {
        retryCount = 0
        // 第一步：发唤醒广播（带 FLAG_INCLUDE_STOPPED_PACKAGES）
        try {
            val wakeIntent = Intent("com.mosi.mosc.ai_weather.WAKE_UP").apply {
                setPackage("com.mosi.mosc.ai_weather")
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            mContent?.sendBroadcast(wakeIntent)
            Log.d(TAG, "已发送唤醒广播")
        } catch (e: Exception) {
            Log.e(TAG, "发送唤醒广播失败: ${e.message}")
        }

        // 第二步：延迟一点再 bindService，给服务端 App 起来的时间
        mHandle.postDelayed({
            try {
                val bindIntent = Intent("com.mosi.mosc.ai_weather.APP_AIDL_SERVICE").apply {
                    setPackage("com.mosi.mosc.ai_weather")
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                }
                val result = mContent?.bindService(bindIntent, connection, Context.BIND_AUTO_CREATE)?:false
                Log.d(TAG, "bindService result = $result")

                // 如果还是失败，安排重试
                if (!result) {
                    retryCount++
                    if (retryCount <= 3) {
                        Log.d(TAG, "bindService 失败，第 ${retryCount} 次重试")
                        mHandle.postDelayed({ bindAIDLService() }, 1000)
                    }
                }
            } catch (e: SecurityException) {
                retryCount = 0
                Log.e(TAG, "bindService SecurityException（检查 queries/exported）", e)
            }
        }, 300)
    }


    private val mHandle: Handler = object : Handler(Looper.getMainLooper()) {
        override fun dispatchMessage(msg: Message) {
            super.dispatchMessage(msg)
            when (msg.what) {
                1 -> {
                    removeMessages(0)
                    sendEmptyMessageDelayed(0, 20000)
                }

                0 -> {
                    //20s 内无操作 则关闭
                    //mCardManager?.requestHide(cardID)
                }
            }
        }
    }

    override fun initialize(
        context: Context,
        cardManager: ICardManager,
        lifecycleOwner: LifecycleOwner
    ) {
        Log.d(TAG,"initialize is running")
        mContent = context
        mCardManager = cardManager
        bindAIDLService()
    }

    override fun unInitialize() {
        Log.d(TAG,"unInitialize is running")
    }

    override fun getSupportedCardIds(): List<String> {
        return mutableListOf(KEY_CARD_ID)
    }

    override fun createCard(
        cardId: String,
        triggerSource: TriggerSource
    ): ICard {
        Log.d(TAG,"createCard is running")
        if (triggerSource == TriggerSource.USER) {
            mHandle.sendEmptyMessage(1)
        }
        return object : ICard{
            override fun getCardId(): String {
                Log.d(TAG,"getCardId is running")
                return cardId
            }

            override fun getSupportedSizes(): List<CardSize> {
                Log.d(TAG,"getSupportedSizes is running")
                return mutableListOf(CardSize.ONE_FOUR, CardSize.ONE_EIGHT)
            }

            override fun getView(cardSize: CardSize): View {
                Log.d(TAG,"getView is running  cardSize == $cardSize")
                return CreateViewFactory.createView(mContent)
            }

            override fun getCardSize(): CardSize {
                Log.d(TAG,"getCardSize is running")
                return CardSize.ONE_FOUR
            }

            override fun getCardPriority(): CardPriority {
                Log.d(TAG,"getCardPriority is running")
                return CardPriority.MEDIUM
            }

            override fun getBusinessType(): BusinessType {
                Log.d(TAG,"getBusinessType is running")
                return BusinessType.WEATHER
            }

            override fun onRefresh() {
                Log.d(TAG,"onRefresh is running")
            }

            override fun onVisibleStateChange(state: VisibleState) {
                Log.d(TAG,"onVisibleStateChange is running state =$state")
                //通知卡片状态
                when (state){
                    VisibleState.SHOW -> {

                    }
                    VisibleState.HIDE -> {
                        CreateViewFactory.destroyView()
                    }
                }
            }

        }
    }

    override fun destroyCard(cardId: String) {
        Log.d(TAG,"destroyCard is running")
        CreateViewFactory.destroyView()
    }

    override fun getBusinessType(): BusinessType {
        Log.d(TAG,"getBusinessType is running")
        return BusinessType.WEATHER
    }

}