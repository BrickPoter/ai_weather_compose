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
import kotlinx.coroutines.launch

//进行服务拉起
class WeatherAIDLServices: Service() {
    private val TAG = "WeatherAIDLServices"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val binder = object : IAIDLWeatherService.Stub() {
        override fun sendWeatherData(data: String) {
            Log.d("WeatherService", "收到天气数据: data=${data}")
            // 处理业务逻辑

        }

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

            serviceScope.launch(Dispatchers.IO) {
                DataAnalysisManager.pauseData(data)
                val result = DataAnalysisManager.requestShowInfo()
                try {
                    callback.onCardBaseInfoResilt(result["baseInfo"] as? CardBaseInfo)
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

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind action=${intent?.action}")
        return binder
    }
}