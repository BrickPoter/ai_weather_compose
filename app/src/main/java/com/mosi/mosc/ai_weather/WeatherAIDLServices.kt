package com.mosi.mosc.ai_weather

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

// 语音相关
import com.fawvw.aisds.common.protocol.SubTaskReportRequest
import com.fawvw.aisds.common.protocol.SubTaskReportStatus
import com.fawvw.datacentersdk.DataCenterSdk
import com.fawvw.datacentersdk.IDataCallback
import com.fawvw.speech.sdk.IDmAgentCallback
import com.fawvw.speech.sdk.SpeechSdk
import com.fawvw.aisds.common.protocol.toMasterAgentStatus
import com.fawvw.aisds.common.protocol.toSubTaskStatus

import com.google.gson.Gson
import com.google.gson.GsonBuilder

// AIDL with com.mosi.mosc.launcher.ai_weather
import com.mosi.mosc.ai_weather.been.ALLDataBeen
import com.mosi.mosc.ai_weather.been.CardUIData
import com.mosi.mosc.ai_weather.been.IAIDLWeatherCallback
import com.mosi.mosc.ai_weather.been.IAIDLWeatherService
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlin.isInitialized

//进行服务拉起
class WeatherAIDLServices: Service() {
    private data class SubTaskMessageEvent(
        val zone: Int,
        val messageId: String?,
        val taskId: String?,
        val message: String?,
    )

    private val gson: Gson by lazy { Gson() }
    private val TAG = "WeatherAIDLServices"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    /** 串行队列：保证 content 拼接 / UI 回调按入队顺序处理，避免并发改 buffer */
    private val subTaskMessageChannel = Channel<SubTaskMessageEvent>(Channel.UNLIMITED)
    private lateinit var speechSdk: SpeechSdk
    private var uiProxyCallback: IAIDLWeatherCallback? = null
    private val mHandlerr = Handler(Looper.getMainLooper())
    private val REGISTER_RETRY_DELAY_MS = 1000L
    private val REGISTER_RETRY_MAX_TIMES = 30   // 最多重试 30 次，防止无限循环

//    private lateinit var ttsHelper: TtsHelper
    private val ttsContentBuffer = ArrayList<String>()


    private fun registerDM(retryCount: Int = 0) {
        if (speechSdk.isBinderSuccess) {
            speechSdk.registerDmAgentCallback(dmAgentStubCallback)
            Log.d(TAG, "registerDM success")
            return
        }

        if (retryCount >= REGISTER_RETRY_MAX_TIMES) {
            Log.w(TAG, "registerDM failed after $REGISTER_RETRY_MAX_TIMES retries, give up")
            return
        }

        Log.d(TAG, "registerDM false, retry ${retryCount + 1} in ${REGISTER_RETRY_DELAY_MS}ms")

        // 延迟后重试
        mHandlerr.postDelayed({
            registerDM(retryCount + 1)
        }, REGISTER_RETRY_DELAY_MS)
    }

    private fun unregisterDM(){
        if( speechSdk.isBinderSuccess){
            speechSdk.unregisterDmAgentCallback(dmAgentStubCallback)
            Log.d(TAG, "unregisterDM success")
        }
        else{
            Log.d(TAG, "unregisterDM false")
        }
    }

    private fun registerCarDataCenter(retryCount: Int=0){

        DataCenterSdk.registerDataListener("naviStatusBean", dataCallback)
    }

    private val dataCallback = object : IDataCallback.Stub() {
        override fun onDataItemChange(dataKey:String?, data: String?){

        }

        override fun onDataChange(data: String?){

        }
    }

    private fun startSubTaskMessageConsumer() {
        serviceScope.launch {
            for (event in subTaskMessageChannel) {
                handleSubTaskMessage(event)
            }
        }
    }

    private suspend fun handleSubTaskMessage(event: SubTaskMessageEvent) {

        val allDataBeen = try {
            gson.fromJson(event.message, ALLDataBeen::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "DM天气数据JSON解析失败", e)
            return
        }

        if (allDataBeen == null) {
            Log.w(TAG, "DM天气数据解析结果为空")
            return
        }

        val agentId = allDataBeen.header.agentId
        val dataType = allDataBeen.payload.dataType
        if (!agentId.equals("weather", ignoreCase = true)) {
            return
        }

        // semantic result process and send to UI layout
        if (dataType.equals("semantic", ignoreCase = true)) {
            coroutineContext.ensureActive()
            val query = allDataBeen.payload.data.query
            Log.d(TAG, "semantic weather query [$query] to process")
            val result = DataAnalysisManager.parseData(allDataBeen, resources)
            Log.d(TAG, "semantic weather result size: ${result.size}, result: $result")

            coroutineContext.ensureActive()
            try {
                (result[DataAnalysisResultKey.CARD_UI_DATA] as? List<CardUIData>).let {
                    uiProxyCallback?.onCardUIDateResult(it)
                }
                Log.d(TAG, "uiProxyCallback callback: $uiProxyCallback run")
            } catch (e: Exception) {
                Log.e(TAG, "uiProxyCallback callback: $uiProxyCallback is error", e)
            }

            coroutineContext.ensureActive()
            try {
                val dir = File(getExternalFilesDir(null) ?: filesDir, "weather_log")
                if (!dir.exists()) dir.mkdirs()
                val f = File(dir, "message_${System.currentTimeMillis()}.txt")
                if(event.message is String) {
                    f.writeText(event.message, Charsets.UTF_8)   // text 就是你要保存的那个大 message
                    Log.d(TAG, "saved: ${f.absolutePath}, ${f.length()} bytes")
                }
            } catch (e: Exception) {
                Log.e(TAG, "save txt failed", e)
            }


            coroutineContext.ensureActive()
            val pretty = GsonBuilder().setPrettyPrinting().create().toJson(allDataBeen)
            Log.d(TAG, "allDataBeen len=${pretty.length}")
            try {
                val dir = File(getExternalFilesDir(null) ?: filesDir, "weather_log")
                if (!dir.exists()) dir.mkdirs()
                val f = File(dir, "allDataBeen_${System.currentTimeMillis()}.json")
                f.writeText(pretty)
                Log.d(TAG, "saved: ${f.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "save failed", e)
            }
        }

        // content result process and tts（单消费者内顺序执行，无需额外 Mutex）
        if (dataType.equals("content", ignoreCase = true)) {
            coroutineContext.ensureActive()
            ttsContentBuffer.add(allDataBeen.payload.data.content)
            if (allDataBeen.payload.data.isFinal) {
                val ttsId = "tts_${System.currentTimeMillis()}"
                val ttsMessageId = allDataBeen.header.messageId
                val contentTaskId = allDataBeen.payload.data.taskId
                val contentFullText = ttsContentBuffer.joinToString("")
                Log.d(TAG, "received content: $contentFullText")
                // MVP：播报收口后上报 History（来自DM回调，需要使用触发当轮的上行 messageId，协议要求同轮共用）
                reportSpokenContent(event.messageId, contentTaskId, contentFullText)
                ttsContentBuffer.clear()
            }
        }
    }

    /** MVP：content 收口后上报播报全文到 History（SUCCESS final） */
    private fun reportSpokenContent(messageId: String?, taskId: String?, spokenText: String) {
        if (messageId.isNullOrBlank() || taskId.isNullOrBlank()) {
            Log.w(TAG, "reportSpokenContent skip: messageId/taskId empty, messageId=$messageId taskId=$taskId")
            return
        }
        if (spokenText.isEmpty()) {
            Log.w(TAG, "reportSpokenContent skip: spokenText empty, taskId=$taskId")
            return
        }
        if (!::speechSdk.isInitialized || !speechSdk.isBinderSuccess) {
            Log.w(TAG, "reportSpokenContent skip: speechSdk not ready")
            return
        }
        try {
            val ok = speechSdk.updateSubTaskStatus(
                SubTaskReportRequest(
                    messageId = messageId,
                    taskId = taskId,
                    status = SubTaskReportStatus.SUCCESS.value,
                    spokenText = spokenText,
                    displayText = spokenText
                )
            )
            Log.d(TAG, "reportSpokenContent ok=$ok messageId=$messageId taskId=$taskId len=${spokenText.length}")
        } catch (t: Throwable) {
            // SDK 内部 StringExtKt 静态初始化缺依赖时会抛 Error，避免拖垮整个天气进程
            Log.e(TAG, "reportSpokenContent failed messageId=$messageId taskId=$taskId", t)
        }
    }


    private val weatherServiceStubBinder = object : IAIDLWeatherService.Stub() {
        override fun sendWeatherData(data: String?) {
        }

        override fun requestWeatherData(callback: IAIDLWeatherCallback? ) {
            Log.d(TAG, "weatherServiceStubBinders.requestWeatherData start")
            if (callback == null) {
                Log.e(TAG, "requestWeatherData fail, callback is null")
                return
            }
            // get callback from UI layOUT
            uiProxyCallback = callback
            Log.d(TAG, "requestWeatherData get uiProxyCallback $uiProxyCallback")

            // register
            registerDM(0)
        }
    }

    private val dmAgentStubCallback = object : IDmAgentCallback.Stub() {
        override fun onMasterAgentMessage(zone: Int, messageId: String?, message: String?) {
            // Master 级帧：拒识帧 / 思考流 / DAG 规划(plan) / 结束(terminate)
            // Log.d(TAG,"dmAgentStubCallback.onMasterAgentMessage start")

        }

        // callback from cloud, when cloud agent send data to edge
        override fun onSubTaskMessage(zone: Int, messageId: String?, taskId: String?, message: String?) {
            Log.d(TAG, "dmAgentStubCallback.onSubTaskMessage start")
            // Binder 回调里只入队，由单一消费者串行处理，避免并发改 ttsContentBuffer / 乱序刷 UI
            val result = subTaskMessageChannel.trySend(
                SubTaskMessageEvent(zone, messageId, taskId, message)
            )
            if (result.isFailure) {
                Log.w(TAG, "onSubTaskMessage enqueue failed: $result")
            }
        }

        override fun onMasterAgentStatusChange(zone: Int, messageId: String?, status: Int) {
            Log.d(TAG, "dmAgentStubCallback.onMasterAgentStatusChange status: ${status.toMasterAgentStatus()}")
        }

        override fun onSubTaskStatusChange(zone: Int, messageId: String?, taskId: String?, status: Int) {
            Log.d(TAG,"dmAgentStubCallback.onSubTaskStatusChange status: ${status.toSubTaskStatus()}")
        }

        override fun onAllSessionsFinished() {

        }

    }


    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate, Version 1.3.1")
        startSubTaskMessageConsumer()
        // 构造 speech sdk
        speechSdk = SpeechSdk(applicationContext)
        Log.d(TAG, "speechSdk init, speechSdk.isBinderSuccess = ${speechSdk.isBinderSuccess}")
        // 初始化 dataCenterSDK接口
//        while (DataCenterSdk.init(applicationContext) == false) {
//            Log.d(TAG, "DataCenterSdk.init false, retry...")
//        }
//        Log.d(TAG, "DataCenterSdk.init succeed")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand, intent: $intent")
        when (intent?.action) {
            "com.mosi.mosc.ai_weather.REGISTER" -> {
                speechSdk = SpeechSdk(applicationContext)
                Log.d(TAG, "speechSdk init, speechSdk.isBinderSuccess = ${speechSdk.isBinderSuccess}, speechSdk.isInitialized = ${::speechSdk.isInitialized}")
                registerDM(0)
            }
            "com.mosi.mosc.ai_weather.UNREGISTER" -> {
                unregisterDM()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy(){
        Log.d(TAG, "onDestroy")
        subTaskMessageChannel.close()
        serviceScope.cancel()
        // 关闭hander正在排队的任务
        mHandlerr.removeCallbacksAndMessages(null)
        // 注销DM
        unregisterDM()
        if (::speechSdk.isInitialized) {
            // 云端speech接口，解绑服务、清空所有回调、移除重连任务。
            speechSdk.release()
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind action=${intent?.action}")
        return weatherServiceStubBinder
    }
}