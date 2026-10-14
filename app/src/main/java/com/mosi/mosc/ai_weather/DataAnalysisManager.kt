package com.mosi.mosc.ai_weather

import android.content.res.Resources
import android.util.Log
import com.google.gson.Gson
import com.mosi.mosc.ai_weather.analysis.BaseInfoParser
import com.mosi.mosc.ai_weather.analysis.BindingContext
import com.mosi.mosc.ai_weather.analysis.ExtraInfoParser
import com.mosi.mosc.ai_weather.analysis.ForecastInfoParser
import com.mosi.mosc.ai_weather.analysis.WarnInfoParser
import com.mosi.mosc.ai_weather.analysis.WeatherObjectParser
import com.mosi.mosc.ai_weather.been.AIBaseBeen
import com.mosi.mosc.ai_weather.been.ALLDataBeen
import com.mosi.mosc.ai_weather.been.BindingsBeen
import com.mosi.mosc.ai_weather.been.CardBaseInfo
import com.mosi.mosc.ai_weather.been.CardUIData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.Locale

object DataAnalysisManager {
    private val gson: Gson by lazy { Gson() }
    private val weatherObjectParser = WeatherObjectParser()
    private val baseInfoParser = BaseInfoParser()
    private val warnInfoParser = WarnInfoParser()
    private val forecastInfoParser = ForecastInfoParser()
    private val extraInfoParser = ExtraInfoParser()
    private const val TAG = "DataAnalysisManager"

    /**
     * 解析DM数据，并按项目原有架构生成卡片信息。
     * DataAnalysisManager只负责binding遍历、模块组合和兼容结果路由。
     */
    suspend fun parseData(
        data: ALLDataBeen,
        resources: Resources
    ): HashMap<String, Any?> = withContext(Dispatchers.Default) {
        val parsingContext = currentCoroutineContext()
        val result = hashMapOf<String, Any?>(
            DataAnalysisResultKey.BASE_INFO to null,
            DataAnalysisResultKey.COMPARE_INFO to null,
            DataAnalysisResultKey.DISPLAY_PLAN to emptyList<Map<String, Any?>>(),
            DataAnalysisResultKey.CARD_UI_DATA to emptyList<CardUIData>()
        )

        parsingContext.ensureActive()
        val allDataBeen = data
        val payload = allDataBeen.payload
        val aiBaseBeen = payload.data

        if (!payload.dataType.equals("semantic", ignoreCase = true) ||
            !aiBaseBeen.domain.equals("WEATHER", ignoreCase = true)
        ) {
            Log.w(
                TAG,
                "忽略非目标天气消息: dataType=${payload.dataType}, " +
                        "domain=${aiBaseBeen.domain}, intention=${aiBaseBeen.intention}, " +
                        "taskState=${aiBaseBeen.taskState}"
            )
            return@withContext result
        }

        var baseCardInfo: CardBaseInfo? = null
        var compareCardInfo: CardBaseInfo? = null
        val displayPlan = mutableListOf<Map<String, Any?>>()
        val cardUIDataList = mutableListOf<CardUIData>()

        aiBaseBeen.slots.bindings.forEachIndexed { bindingIndex, binding ->
            parsingContext.ensureActive()
            val context = BindingContext.create(aiBaseBeen, binding, resources)
            if (context == null) {
                displayPlan += bindingResult(
                    bindingIndex = bindingIndex,
                    frame = aiBaseBeen,
                    binding = binding,
                    values = emptyMap(),
                    missingObjects = binding.weatherObjects,
                    status = "invalid_time"
                )
                return@forEachIndexed
            }

            val weatherObjectResult = weatherObjectParser.parse(context)
            val bindingData = weatherObjectResult.values
            val missingObjects = weatherObjectResult.missingObjects
            val baseInfoResult = baseInfoParser.parse(context, bindingData)

            // 兼容旧CardBaseInfo回调；只有真正生成基础模块时才赋值。
            baseInfoResult.legacyCardInfo?.let { cardInfo ->
                when (binding.time.role.lowercase(Locale.ROOT)) {
                    "comparison" -> compareCardInfo = cardInfo
                    "baseline" -> baseCardInfo = cardInfo
                    "target" -> if (baseCardInfo == null) baseCardInfo = cardInfo
                    else -> if (baseCardInfo == null) baseCardInfo = cardInfo
                }
            }

            val warnInfoList = warnInfoParser.parse(context)
            val forecastInfoList = forecastInfoParser.parse(context)

            // 已经进入前三个模块的对象不重复放进额外信息模块。
            val consumedObjects = baseInfoResult.consumedObjects.toMutableSet()
            if (context.granularity == "multi_day" || context.granularity == "period") {
                consumedObjects += context.requestedObjects.intersect(
                    FORECAST_MODULE_OBJECTS
                )
            }
            if (bindingData.containsKey("weather_alert")) {
                consumedObjects += "weather_alert"
            }

            val extraInfoList = extraInfoParser.parse(
                context = context,
                bindingData = bindingData,
                consumedObjects = consumedObjects,
                hasAlert = warnInfoList.isNotEmpty()
            )
            cardUIDataList += CardUIData(
                baseInfo = baseInfoResult.cardInfo,
                extraInfo = extraInfoList,
                forecastInfo = forecastInfoList,
                warnInfo = warnInfoList,
                granularity = context.granularity
            )

            displayPlan += bindingResult(
                bindingIndex = bindingIndex,
                context = context,
                values = bindingData,
                missingObjects = missingObjects,
                status = when {
                    bindingData.isEmpty() -> "not_found"
                    missingObjects.isEmpty() -> "matched"
                    else -> "partial"
                }
            )
        }

        result[DataAnalysisResultKey.BASE_INFO] = baseCardInfo
        result[DataAnalysisResultKey.COMPARE_INFO] = compareCardInfo
        result[DataAnalysisResultKey.DISPLAY_PLAN] = displayPlan
        parsingContext.ensureActive()
        result[DataAnalysisResultKey.CARD_UI_DATA] = cardUIDataList
        Log.d(TAG, "parseData result=$result")
        result
    }

    private fun bindingResult(
        bindingIndex: Int,
        context: BindingContext,
        values: Map<String, Any?>,
        missingObjects: List<String>,
        status: String
    ): Map<String, Any?> = bindingResult(
        bindingIndex = bindingIndex,
        frame = context.frame,
        binding = context.binding,
        values = values,
        missingObjects = missingObjects,
        status = status
    )

    private fun bindingResult(
        bindingIndex: Int,
        frame: AIBaseBeen,
        binding: BindingsBeen,
        values: Map<String, Any?>,
        missingObjects: List<String>,
        status: String
    ): Map<String, Any?> = linkedMapOf(
        "bindingIndex" to bindingIndex,
        "taskId" to frame.taskId,
        "query" to frame.query,
        "city" to linkedMapOf(
            "name" to frame.bizData.city.name,
            "secondaryName" to frame.bizData.city.secondaryName
        ),
        "role" to binding.time.role,
        "expression" to binding.time.expression,
        "granularity" to binding.time.granularity,
        "startTime" to binding.time.startTime,
        "endTime" to binding.time.endTime,
        "weatherObjects" to binding.weatherObjects,
        "matchStatus" to status,
        "missingObjects" to missingObjects,
        "data" to values
    )

    private val FORECAST_MODULE_OBJECTS = setOf(
        "weather_condition",
        "temperature",
        "precipitation_probability"
    )
}
