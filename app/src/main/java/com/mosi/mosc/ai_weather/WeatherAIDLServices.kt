package com.mosi.mosc.ai_weather

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.mosi.mosc.ai_weather.been.CardBaseInfo
import com.mosi.mosc.ai_weather.been.IAIDLWeatherCallback
import com.mosi.mosc.ai_weather.been.IAIDLWeatherService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.mosi.mosc.ai_weather.been.CardUIData
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive

//进行服务拉起
class WeatherAIDLServices: Service() {
    private val TAG = "WeatherAIDLServices"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val binder = object : IAIDLWeatherService.Stub() {
        override fun sendWeatherData(
            requestId: String?,
            data: String?,
            callback: IAIDLWeatherCallback?
        ) {
            Log.d(TAG, "收到异步请求: requestId=$requestId  data = $data")
            if (callback == null) {
                Log.e(TAG, "Callback is null")
                return
            }

            serviceScope.launch {

                val result = DataAnalysisManager.parseData(data?:"",resources)
                ensureActive()
                try {
                    (result[DataAnalysisResultKey.CARD_UI_DATA] as? CardUIData)?.let{
                        callback.onCardUIDateResult(it)
                    }
//                    callback.onCardBaseInfoResilt(result["baseInfo"] as? CardBaseInfo)
                }catch (e: Exception){
                    Log.e(TAG, "Callback baseInfo is error")
                }
            }
        }
//        override fun sendWeatherData(data: String) {
//            Log.d("WeatherService", "收到天气数据: data=${data}")
//            // 处理业务逻辑
//            serviceScope.launch {
//                DataAnalysisManager.parseData(data,resources)
//            }
//        }

        override fun requestWeatherData(
            requestId: String?,
            data: String,
            callback: IAIDLWeatherCallback?
        ) {
            Log.d(TAG, "收到异步请求: requestId=$requestId")
            if (callback == null) {
                Log.e(TAG, "Callback is null")
                return
            }

            serviceScope.launch {

                val result = DataAnalysisManager.parseData(data,resources)
                ensureActive()
                try {
                    (result[DataAnalysisResultKey.CARD_UI_DATA] as? CardUIData)?.let{
                        callback.onCardUIDateResult(it)
                    }
//                    callback.onCardBaseInfoResilt(result["baseInfo"] as? CardBaseInfo)
                }catch (e: Exception){
                    Log.e(TAG, "Callback baseInfo is error")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

    override fun onDestroy(){
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind action=${intent?.action}")
        return binder
    }
}