package com.mosi.mosc.launcher.ai_weather

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.fawvw.hmi.ailauncher.pluginlib.BusinessType
import com.fawvw.hmi.ailauncher.pluginlib.CardPriority
import com.fawvw.hmi.ailauncher.pluginlib.CardSize
import com.fawvw.hmi.ailauncher.pluginlib.IAiCloudMessageListener
import com.fawvw.hmi.ailauncher.pluginlib.ICard
import com.fawvw.hmi.ailauncher.pluginlib.ICardManager
import com.fawvw.hmi.ailauncher.pluginlib.ICardPlugin
import com.fawvw.hmi.ailauncher.pluginlib.TriggerSource
import com.fawvw.hmi.ailauncher.pluginlib.VisibleState
import com.mosi.mosc.ai_weather.been.CardBaseInfo
import com.mosi.mosc.ai_weather.been.IAIDLWeatherCallback
import com.mosi.mosc.ai_weather.been.IAIDLWeatherService

class WeatherBusinessPlugin : ICardPlugin, CreateViewFactory.CreateViewListener {
    companion object {
        const val KEY_CARD_ID = "WEATHER_CARD"
    }

    private var mContent: Context? = null
    private var mCardManager: ICardManager? = null

    private var weatherService : IAIDLWeatherService? = null

    private val callAIDLBack: IAIDLWeatherCallback by lazy { object : IAIDLWeatherCallback.Stub() {
        override fun onCardBaseInfoResilt(result: CardBaseInfo?) {
            //返回解析的基础数据
            CreateViewFactory.showBaseCard(result)
        }
    } }

    private var isBound = false

    private val connection by lazy { object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            weatherService = IAIDLWeatherService.Stub.asInterface(service)
            try {

                mCardManager?.getAiCloudService()?.registerMessageListener(BusinessType.WEATHER,object :
                    IAiCloudMessageListener {
                    override fun dispatchMessage(message: String) {
//                        weatherService?.sendWeatherData(message)
                        weatherService?.requestWeatherData("req_${System.currentTimeMillis()}",message,callAIDLBack)
                    }

                })
            }catch (e: Exception){

            }
            isBound = true

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            weatherService = null
            mCardManager?.getAiCloudService()?.unregisterMessageListener(BusinessType.WEATHER)
            isBound = false
        }

    } }


    private var cardID = ""

    private fun bindAIDLService() {
        // ✅ 方式一：通过 Action 绑定（推荐）
        val intent = Intent("com.mosi.mosc.ai_weather.APP_AIDL_SERVICE")
        intent.setPackage("com.mosi.mosc.ai_weather") // 服务端包名
        mContent?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
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
                    mCardManager?.requestHide(cardID)
                }
            }
        }
    }

    override fun initialize(
        context: Context,
        cardManager: ICardManager,
        lifecycleOwner: LifecycleOwner
    ) {
        mContent = context
        mCardManager = cardManager
        bindAIDLService()
    }

    override fun unInitialize() {

    }

    override fun getSupportedCardIds(): List<String> {
        return mutableListOf(KEY_CARD_ID)
    }

    override fun createCard(
        cardId: String,
        triggerSource: TriggerSource
    ): ICard {
        if (triggerSource == TriggerSource.USER) {
            mHandle.sendEmptyMessage(1)
        }
        return object : ICard{
            override fun getCardId(): String {
                return cardId
            }

            override fun getSupportedSizes(): List<CardSize> {
                return mutableListOf(CardSize.ONE_FOUR, CardSize.ONE_EIGHT)
            }

            override fun getView(cardSize: CardSize): View {
                return CreateViewFactory.createView(cardSize, mContent)
            }

            override fun getCardSize(): CardSize {
                return CardSize.ONE_FOUR
            }

            override fun getCardPriority(): CardPriority {
                return CardPriority.MEDIUM
            }

            override fun getBusinessType(): BusinessType {
                return BusinessType.WEATHER
            }

            override fun onRefresh() {

            }

            override fun onVisibleStateChange(state: VisibleState) {
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
        CreateViewFactory.destroyView()
    }

    override fun getBusinessType(): BusinessType {
        return BusinessType.WEATHER
    }

}