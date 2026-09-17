package com.mosi.mosc.ai_weather

import android.content.res.Resources
import android.util.Log
import com.google.gson.Gson
import com.mosi.mosc.ai_weather.been.AIBaseBeen
import com.mosi.mosc.ai_weather.been.ALLDataBeen
import com.mosi.mosc.ai_weather.been.CardBaseInfo
import com.mosi.mosc.ai_weather.been.CardExtraInfo
import com.mosi.mosc.ai_weather.been.CardForecastInfo
import com.mosi.mosc.ai_weather.been.CardUIData
import com.mosi.mosc.ai_weather.been.CardWarnInfo
import com.mosi.mosc.ai_weather.been.ForecastBeen
import com.mosi.mosc.ai_weather.been.HourlyBeen
import com.mosi.mosc.ai_weather.been.LiveIndexBeen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DataAnalysisManager {
    private val gson: Gson by lazy { Gson() }
    private const val TAG = "DataAnalysisManager"
    private val chinaTimeZone: TimeZone = TimeZone.getTimeZone("GMT+08:00")

    /**
     * 解析DM数据，并按项目原有架构生成卡片信息：
     * bindings -> time/role -> weatherObjects -> when分支。
     *
     * baseInfo/compareInfo用于兼容当前AIDL卡片回调；displayPlan保存每个binding
     * 实际请求的全部天气对象，前端后续可以按weatherObject机器码直接取值。
    */
    suspend fun parseData(
        data: String,
        resources: Resources
    ): HashMap<String, Any?> = withContext(Dispatchers.Default) {
        val parsingContext = currentCoroutineContext()
        val result = hashMapOf<String, Any?>(
            DataAnalysisResultKey.BASE_INFO to null,
            DataAnalysisResultKey.COMPARE_INFO to null,
            DataAnalysisResultKey.DISPLAY_PLAN to emptyList<Map<String, Any?>>(),
            DataAnalysisResultKey.CARD_UI_DATA to null
        )

        parsingContext.ensureActive()
        val allDataBeen = try {
            gson.fromJson(data, ALLDataBeen::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "DM天气数据JSON解析失败", e)
            return@withContext result
        }
        parsingContext.ensureActive()
        if (allDataBeen == null) {
            Log.w(TAG, "DM天气数据解析结果为空")
            return@withContext result
        }
        val payload = allDataBeen.payload
        val aiBaseBeen = payload.data

        if (!payload.dataType.equals("semantic", ignoreCase = true) ||
            !aiBaseBeen.domain.equals("WEATHER", ignoreCase = true) ||
            !aiBaseBeen.intention.equals("SHOW_DATA", ignoreCase = true) ||
            !aiBaseBeen.taskState.equals("completed", ignoreCase = true)
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
        var warnInfo: CardWarnInfo? = null
        val forecastInfoList = mutableListOf<CardForecastInfo>()
        val extraInfoList = mutableListOf<CardExtraInfo>()

        aiBaseBeen.slots.bindings.forEachIndexed { bindingIndex, bindingsBeen ->
            parsingContext.ensureActive()
            val startTime = parseIsoTime(bindingsBeen.time.startTime)
            if (startTime == null) {
                displayPlan += bindingResult(
                    bindingIndex, aiBaseBeen, bindingsBeen.time.role,
                    bindingsBeen.time.expression, bindingsBeen.time.granularity,
                    bindingsBeen.time.startTime, bindingsBeen.time.endTime,
                    bindingsBeen.weatherObjects, emptyMap(), bindingsBeen.weatherObjects,
                    "invalid_time"
                )
                return@forEachIndexed
            }

            val granularity = bindingsBeen.time.granularity.lowercase(Locale.ROOT)
            val endTime = parseIsoTime(bindingsBeen.time.endTime) ?: when (granularity) {
                "day", "multi_day" -> endOfNaturalDay(startTime)
                else -> startTime
            }
            val forecastResult = when (granularity) {
                "day", "multi_day" -> aiBaseBeen.bizData.forecast
                    .filter { it.predictDate in startTime..endTime }
                    .sortedBy { it.predictDate }
                else -> emptyList()
            }
            val hourlyResult = when (granularity) {
                "instant", "period" -> aiBaseBeen.bizData.hourly
                    .filter { it.date in startTime..endTime }
                    .sortedBy { it.date }
                else -> emptyList()
            }
            val conditionTime = aiBaseBeen.bizData.condition.updateTime.toLongOrNull()
            val currentMatches = conditionTime?.let { it in startTime..endTime } == true
            val cardInfo = emptyCardInfo(aiBaseBeen, bindingsBeen.time.expression)
            val bindingData = linkedMapOf<String, Any?>()
            val missingObjects = mutableListOf<String>()

            // 每种weatherObject的数据处理逻辑直接写在项目原有when架构下。
            bindingsBeen.weatherObjects.forEach { weatherObject ->
                parsingContext.ensureActive()
                val value: Any? = when (weatherObject) {
                    "temperature" -> {
                        // 温度：instant/period取hourly，day/multi_day取forecast。
                        when (granularity) {
                            "instant", "period" -> hourlyRecords(hourlyResult) {
                                linkedMapOf("time" to it.date, "value" to it.temp)
                            }.also { records ->
                                if (records.isNotEmpty()) {
                                    cardInfo.currentTemp = hourlyResult.first().temp
                                    cardInfo.highTemp = numericExtreme(
                                        hourlyResult.map { it.temp }, true
                                    )
                                    cardInfo.lowTemp = numericExtreme(
                                        hourlyResult.map { it.temp }, false
                                    )
                                }
                            }
                            "day", "multi_day" -> forecastRecords(forecastResult) {
                                linkedMapOf(
                                    "time" to it.predictDate,
                                    "day" to it.tempDay,
                                    "night" to it.tempNight,
                                    "maximum" to preferredValue(it.maxTemp, it.tempDay),
                                    "minimum" to preferredValue(it.minTemp, it.tempNight)
                                )
                            }.also { records ->
                                if (records.isNotEmpty()) {
                                    cardInfo.highTemp = maximumTemperature(forecastResult)
                                    cardInfo.lowTemp = minimumTemperature(forecastResult)
                                    if (currentMatches) {
                                        cardInfo.currentTemp = aiBaseBeen.bizData.condition.temp
                                    }
                                }
                            }
                            else -> null
                        }
                    }

                    "apparent_temperature" -> {
                        // 体感温度：协议定义condition.realFeel和hourly.realFeel。
                        when (granularity) {
                            "instant", "period" -> hourlyRecords(hourlyResult) {
                                linkedMapOf("time" to it.date, "value" to it.realFeel)
                            }.ifEmptyOrElse {
                                currentValue(currentMatches, conditionTime) {
                                    aiBaseBeen.bizData.condition.realFeel
                                }
                            }
                            else -> currentValue(currentMatches, conditionTime) {
                                aiBaseBeen.bizData.condition.realFeel
                            }
                        }
                    }

                    "precipitation_probability" -> when (granularity) {
                        "instant", "period" -> hourlyRecords(hourlyResult) {
                            linkedMapOf("time" to it.date, "value" to it.pop)
                        }
                        "day", "multi_day" -> forecastRecords(forecastResult) {
                            linkedMapOf("time" to it.predictDate, "value" to it.pop)
                        }
                        else -> null
                    }

                    "precipitation_amount" -> {
                        // 降水量仅在小时预报中定义。
                        hourlyRecords(hourlyResult) {
                            linkedMapOf("time" to it.date, "value" to it.qpf)
                        }
                    }

                    "snowfall_amount" -> {
                        // 降雪量仅在小时预报中定义。
                        hourlyRecords(hourlyResult) {
                            linkedMapOf("time" to it.date, "value" to it.snow)
                        }
                    }

                    "humidity" -> when (granularity) {
                        "instant", "period" -> hourlyRecords(hourlyResult) {
                            linkedMapOf("time" to it.date, "value" to it.humidity)
                        }.ifEmptyOrElse {
                            currentValue(currentMatches, conditionTime) {
                                aiBaseBeen.bizData.condition.humidity
                            }
                        }
                        "day", "multi_day" -> forecastRecords(forecastResult) {
                            linkedMapOf("time" to it.predictDate, "value" to it.humidity)
                        }
                        else -> null
                    }

                    "pressure" -> when (granularity) {
                        "instant", "period" -> hourlyRecords(hourlyResult) {
                            linkedMapOf("time" to it.date, "value" to it.pressure)
                        }.ifEmptyOrElse {
                            currentValue(currentMatches, conditionTime) {
                                aiBaseBeen.bizData.condition.pressure
                            }
                        }
                        else -> currentValue(currentMatches, conditionTime) {
                            aiBaseBeen.bizData.condition.pressure
                        }
                    }

                    "weather_condition" -> {
                        // 天气现象。
                        when (granularity) {
                            "instant", "period" -> hourlyRecords(hourlyResult) {
                                linkedMapOf(
                                    "time" to it.date, "value" to it.condition,
                                    "code" to it.conditionId, "iconDay" to it.iconDay,
                                    "iconNight" to it.iconNight
                                )
                            }.also { records ->
                                if (records.isNotEmpty()) {
                                    val first = hourlyResult.first()
                                    val last = hourlyResult.last()
                                    cardInfo.icon = if (first.dayNight) first.iconDay else first.iconNight
                                    cardInfo.tempInfo = mergeConditions(
                                        first.condition, last.condition
                                    )
                                }
                            }
                            "day", "multi_day" -> forecastRecords(forecastResult) {
                                linkedMapOf(
                                    "time" to it.predictDate,
                                    "day" to it.conditionDay,
                                    "dayCode" to it.conditionIdDay,
                                    "night" to it.conditionNight,
                                    "nightCode" to it.conditionIdNight
                                )
                            }.also { records ->
                                if (records.isNotEmpty()) {
                                    cardInfo.icon = forecastResult.first().conditionIdDay
                                    cardInfo.tempInfo = mergeConditions(
                                        forecastResult.first().conditionDay,
                                        forecastResult.last().conditionNight
                                    )
                                }
                            }
                            else -> null
                        }
                    }

                    "wind" -> when (granularity) {
                        "instant", "period" -> hourlyRecords(hourlyResult) {
                            linkedMapOf(
                                "time" to it.date,
                                "direction" to windDirection(it.windDir.code, it.windDir.name),
                                "level" to it.windLevel,
                                "speed" to it.windSpeed,
                                "degrees" to it.windDegrees
                            )
                        }.ifEmptyOrElse {
                            currentValue(currentMatches, conditionTime) {
                                linkedMapOf(
                                    "direction" to windDirection(
                                        aiBaseBeen.bizData.condition.windDir.code,
                                        aiBaseBeen.bizData.condition.windDir.name
                                    ),
                                    "level" to aiBaseBeen.bizData.condition.windLevel,
                                    "speed" to aiBaseBeen.bizData.condition.windSpeed,
                                    "degrees" to aiBaseBeen.bizData.condition.windDegrees
                                )
                            }
                        }
                        "day", "multi_day" -> forecastRecords(
                            forecastResult, ::forecastWind
                        )
                        else -> null
                    }

                    "visibility" -> currentValue(currentMatches, conditionTime) {
                        aiBaseBeen.bizData.condition.vis
                    }

                    "ultraviolet" -> {
                        // 紫外线数值及code.code=21的生活建议。
                        val numericalValue = when (granularity) {
                            "instant", "period" -> hourlyRecords(hourlyResult) {
                                linkedMapOf("time" to it.date, "value" to it.uvi)
                            }.ifEmptyOrElse {
                                currentValue(currentMatches, conditionTime) {
                                    aiBaseBeen.bizData.condition.uvi
                                }
                            }
                            else -> currentValue(currentMatches, conditionTime) {
                                aiBaseBeen.bizData.condition.uvi
                            }
                        }
                        val advice = matchingLiveIndex(
                            aiBaseBeen, "21", startTime, endTime
                        )
                        if (numericalValue == null && advice.isEmpty()) null else linkedMapOf(
                            "value" to numericalValue,
                            "advice" to advice
                        )
                    }

                    "sunrise_sunset" -> when (granularity) {
                        "day", "multi_day" -> forecastRecords(forecastResult) {
                            linkedMapOf(
                                "time" to it.predictDate,
                                "sunrise" to it.sunrise,
                                "sunset" to it.sunset
                            )
                        }
                        else -> currentValue(currentMatches, conditionTime) {
                            linkedMapOf(
                                "sunrise" to aiBaseBeen.bizData.condition.sunRise,
                                "sunset" to aiBaseBeen.bizData.condition.sunSet
                            )
                        }
                    }

                    "moon_phase" -> forecastRecords(forecastResult) {
                        linkedMapOf("time" to it.predictDate, "value" to it.moonPhase)
                    }

                    "weather_alert" -> {
                        // 天气预警来自顶层alert数组。
                        aiBaseBeen.bizData.alert.mapNotNull { it.content }.ifEmpty { null }
                    }

                    "air_quality" -> {
                        val aqi = aiBaseBeen.bizData.aqi
                        if (aqi.updateTime !in startTime..endTime) null else linkedMapOf(
                            "time" to aqi.updateTime,
                            "value" to aqi.aqiValue,
                            "level" to aqi.aqiValueText,
                            "rank" to aqi.rank,
                            "description" to aqi.aqiDesc
                        )
                    }

                    "carbon_monoxide" -> pollutant(aiBaseBeen, startTime, endTime, "co")
                    "nitrogen_dioxide" -> pollutant(aiBaseBeen, startTime, endTime, "no2")
                    "ozone" -> pollutant(aiBaseBeen, startTime, endTime, "o3")
                    "pm10" -> pollutant(aiBaseBeen, startTime, endTime, "pm10")
                    "pm2_5" -> pollutant(aiBaseBeen, startTime, endTime, "pm25")
                    "sulfur_dioxide" -> pollutant(aiBaseBeen, startTime, endTime, "so2")

                    "air_quality_forecast" -> aiBaseBeen.bizData.aqiForecastResponse
                        .filter { dateInRange(it.date, startTime, endTime) }
                        .map {
                            linkedMapOf(
                                "date" to it.date,
                                "publishTime" to it.publishTime,
                                "value" to it.value
                            )
                        }.ifEmpty { null }

                    // 生活指数通过liveIndex[].code.code匹配，保留数字编码。
                    "allergy_index" -> lifeIndex(aiBaseBeen, "32", startTime, endTime)
                    "cold_risk_index" -> lifeIndex(aiBaseBeen, "12", startTime, endTime)
                    "exercise_index" -> lifeIndex(aiBaseBeen, "26", startTime, endTime)
                    "makeup_index" -> lifeIndex(aiBaseBeen, "7", startTime, endTime)
                    "clothing_index" -> lifeIndex(aiBaseBeen, "20", startTime, endTime)
                    "fishing_index" -> lifeIndex(aiBaseBeen, "28", startTime, endTime)
                    "traffic_index" -> lifeIndex(aiBaseBeen, "5", startTime, endTime)
                    "travel_index" -> lifeIndex(aiBaseBeen, "14", startTime, endTime)
                    "car_wash_index" -> lifeIndex(aiBaseBeen, "17", startTime, endTime)
                    "air_pollution_dispersion" -> lifeIndex(
                        aiBaseBeen, "18", startTime, endTime
                    )

                    "traffic_restriction" -> {
                        val current = aiBaseBeen.bizData.currentLimit.takeIf {
                            dateInRange(it.date, startTime, endTime)
                        }?.let { linkedMapOf("date" to it.date, "prompt" to it.prompt) }
                        val daily = aiBaseBeen.bizData.limit.filter {
                            dateInRange(it.date, startTime, endTime)
                        }.map { linkedMapOf("date" to it.date, "prompt" to it.prompt) }
                        if (current == null && daily.isEmpty()) null else linkedMapOf(
                            "current" to current,
                            "daily" to daily
                        )
                    }

                    else -> null
                }

                if (isMeaningful(value)) {
                    bindingData[weatherObject] = value
                } else {
                    missingObjects += weatherObject
                }
            }

            val requestedObjects = bindingsBeen.weatherObjects.toSet()
            val baseObjects = setOf(
                "weather_condition",
                "temperature"
            )
            val matchedBaseObjects = requestedObjects.intersect(baseObjects)
                .filter { bindingData.containsKey(it) }

            // 基础模块只响应instant/period/day；multi_day由多日期模块承载。
            if (granularity != "multi_day" && matchedBaseObjects.isNotEmpty()) {
                // 兼容旧CardBaseInfo回调；只有真正生成基础模块时才赋值。
                when (bindingsBeen.time.role.lowercase(Locale.ROOT)) {
                    "comparison" -> compareCardInfo = cardInfo
                    "baseline" -> baseCardInfo = cardInfo
                    "target" -> if (baseCardInfo == null) baseCardInfo = cardInfo
                    else -> if (baseCardInfo == null) baseCardInfo = cardInfo
                }
            }

            // 预警模块只有weatherObjects明确包含weather_alert时才生成。
            if (bindingData.containsKey("weather_alert")) {
                val contents = aiBaseBeen.bizData.alert.mapNotNull { it.content }
                if (contents.isNotEmpty()) {
                    warnInfo = CardWarnInfo(
                        icon = null,
                        title = "恶劣天气预警",
                        content = contents.joinToString("\n")
                    )
                }
            }

            // 多日期预报：一条forecast转换为一个前端CardForecastInfo。
            if (granularity == "multi_day" && forecastResult.isNotEmpty()) {
                forecastResult.forEach { forecast ->
                    forecastInfoList += CardForecastInfo(
                        time = formatTime(forecast.predictDate, "MM月dd日"),
                        icon = forecast.conditionIdDay.takeIf {
                            "weather_condition" in requestedObjects
                        },
                        probability = forecast.pop.takeIf {
                            "precipitation_probability" in requestedObjects
                        },
                        highTemp = preferredValue(forecast.maxTemp, forecast.tempDay)
                            .takeIf { "temperature" in requestedObjects },
                        lowTemp = preferredValue(forecast.minTemp, forecast.tempNight)
                            .takeIf { "temperature" in requestedObjects },
                        condition = mergeConditions(
                            forecast.conditionDay,
                            forecast.conditionNight
                        ).takeIf {
                            "weather_condition" in requestedObjects
                        }
                    )
                }
            }

            // 小时时间段预报复用同一个CardForecastInfo列表。
            if (granularity == "period" && hourlyResult.isNotEmpty()) {
                hourlyResult.forEach { hourly ->
                    forecastInfoList += CardForecastInfo(
                        time = formatTime(hourly.date, "HH:mm"),
                        icon = if ("weather_condition" in requestedObjects) {
                            if (hourly.dayNight) hourly.iconDay else hourly.iconNight
                        } else null,
                        probability = hourly.pop.takeIf {
                            "precipitation_probability" in requestedObjects
                        },
                        highTemp = hourly.temp.takeIf {
                            "temperature" in requestedObjects
                        },
                        lowTemp = null,
                        condition = hourly.condition.takeIf {
                            "weather_condition" in requestedObjects
                        }
                    )
                }
            }

            // 已经进入前三个模块的对象不重复放进额外信息模块。
            val consumedObjects = mutableSetOf<String>()
            if (granularity != "multi_day" && matchedBaseObjects.isNotEmpty()) {
                consumedObjects += matchedBaseObjects
            }
            if (granularity == "multi_day") {
                consumedObjects += requestedObjects.intersect(
                    setOf(
                        "weather_condition",
                        "temperature",
                        "precipitation_probability"
                    )
                )
            }
            if (granularity == "period") {
                consumedObjects += requestedObjects.intersect(
                    setOf(
                        "weather_condition",
                        "temperature",
                        "precipitation_probability"
                    )
                )
            }
            if (bindingData.containsKey("weather_alert")) {
                consumedObjects += "weather_alert"
            }

            bindingData.forEach { (weatherObject, value) ->
                if (weatherObject !in consumedObjects) {
                    extraInfoList += CardExtraInfo(
                        title = weatherObjectTitle(weatherObject, resources),
                        content = cardExtraContent(weatherObject, value)
                    )
                }
            }

            displayPlan += bindingResult(
                bindingIndex, aiBaseBeen, bindingsBeen.time.role,
                bindingsBeen.time.expression, bindingsBeen.time.granularity,
                bindingsBeen.time.startTime, bindingsBeen.time.endTime,
                bindingsBeen.weatherObjects, bindingData, missingObjects,
                when {
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
        result[DataAnalysisResultKey.CARD_UI_DATA] = CardUIData(
            // 前端模型规定baseInfo为非空，因此兜底也必须生成完整基础天气。
            baseInfo = baseCardInfo ?: buildRequiredBaseInfo(aiBaseBeen),
            extraInfo = extraInfoList,
            forecastInfo = forecastInfoList,
            // 前端模型规定为非空；无预警时所有字段均为null，前端据此隐藏。
            warnInfo = warnInfo ?: CardWarnInfo(null, null, null)
        )
        Log.d(TAG, "parseData result=$result")
        result
    }

    /**
     * CardUIData要求baseInfo始终存在。优先使用主binding匹配的日预报，
     * 匹配不到时再使用condition实时数据，避免只返回location的占位对象。
     */
    private fun buildRequiredBaseInfo(frame: AIBaseBeen): CardBaseInfo {
        val binding = frame.slots.bindings.firstOrNull {
            !it.time.role.equals("comparison", ignoreCase = true)
        } ?: frame.slots.bindings.firstOrNull()
        val startTime = parseIsoTime(binding?.time?.startTime)
        val endTime = parseIsoTime(binding?.time?.endTime)
            ?: startTime?.let(::endOfNaturalDay)
        val forecasts = if (startTime != null && endTime != null) {
            frame.bizData.forecast
                .filter { it.predictDate in startTime..endTime }
                .sortedBy { it.predictDate }
        } else {
            emptyList()
        }
        val firstForecast = forecasts.firstOrNull()
        val lastForecast = forecasts.lastOrNull()
        val condition = frame.bizData.condition

        return CardBaseInfo(
            icon = firstForecast?.conditionIdDay
                ?.takeIf { it.isNotBlank() }
                ?: condition.icon,
            location = preferredValue(
                frame.bizData.city.name,
                frame.bizData.city.secondaryName
            ),
            showTime = binding?.time?.expression ?: "现在",
            currentTemp = condition.temp,
            highTemp = maximumTemperature(forecasts) ?: condition.temp,
            lowTemp = minimumTemperature(forecasts) ?: condition.temp,
            tempInfo = if (firstForecast != null && lastForecast != null) {
                mergeConditions(
                    firstForecast.conditionDay,
                    lastForecast.conditionNight
                )
            } else {
                preferredValue(condition.condition, condition.tips)
            }
        )
    }

    private fun emptyCardInfo(frame: AIBaseBeen, expression: String): CardBaseInfo {
        return CardBaseInfo(
            icon = null,
            location = preferredValue(
                frame.bizData.city.name,
                frame.bizData.city.secondaryName
            ),
            showTime = expression,
            currentTemp = null,
            highTemp = null,
            lowTemp = null,
            tempInfo = null
        )
    }

    private fun bindingResult(
        bindingIndex: Int,
        frame: AIBaseBeen,
        role: String,
        expression: String,
        granularity: String,
        startTime: String,
        endTime: String,
        requestedObjects: List<String>,
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
        "role" to role,
        "expression" to expression,
        "granularity" to granularity,
        "startTime" to startTime,
        "endTime" to endTime,
        "weatherObjects" to requestedObjects,
        "matchStatus" to status,
        "missingObjects" to missingObjects,
        "data" to values
    )

    private fun forecastRecords(
        values: List<ForecastBeen>,
        mapper: (ForecastBeen) -> Map<String, Any?>
    ): List<Map<String, Any?>> = values.map(mapper)

    private fun hourlyRecords(
        values: List<HourlyBeen>,
        mapper: (HourlyBeen) -> Map<String, Any?>
    ): List<Map<String, Any?>> = values.map(mapper)

    private fun currentValue(
        matches: Boolean,
        time: Long?,
        value: () -> Any?
    ): Map<String, Any?>? {
        if (!matches) return null
        return linkedMapOf("time" to time, "value" to value())
    }

    private fun forecastWind(item: ForecastBeen): Map<String, Any?> = linkedMapOf(
        "time" to item.predictDate,
        "day" to linkedMapOf(
            "direction" to windDirection(item.windDirDay.code, item.windDirDay.name),
            "level" to item.windLevelDay,
            "speed" to item.windSpeedDay,
            "degrees" to item.windDegreesDay
        ),
        "night" to linkedMapOf(
            "direction" to windDirection(item.windDirNight.code, item.windDirNight.name),
            "level" to item.windLevelNight,
            "speed" to item.windSpeedNight,
            "degrees" to item.windDegreesNight
        )
    )

    private fun windDirection(code: String, name: String): Map<String, String> {
        return linkedMapOf("code" to code, "name" to name)
    }

    private fun pollutant(
        frame: AIBaseBeen,
        startTime: Long,
        endTime: Long,
        type: String
    ): Map<String, Any?>? {
        val aqi = frame.bizData.aqi
        if (aqi.updateTime !in startTime..endTime) return null
        val (subIndex, concentration) = when (type) {
            "co" -> aqi.co to aqi.coC
            "no2" -> aqi.no2 to aqi.no2C
            "o3" -> aqi.o3 to aqi.o3C
            "pm10" -> aqi.pm10 to aqi.pm10C
            "pm25" -> aqi.pm25 to aqi.pm25C
            "so2" -> aqi.so2 to aqi.so2C
            else -> null to null
        }
        if (subIndex.isNullOrBlank() && concentration.isNullOrBlank()) return null
        return linkedMapOf(
            "time" to aqi.updateTime,
            "subIndex" to subIndex,
            "concentration" to concentration
        )
    }

    private fun lifeIndex(
        frame: AIBaseBeen,
        code: String,
        startTime: Long,
        endTime: Long
    ): List<Map<String, Any?>>? {
        return matchingLiveIndex(frame, code, startTime, endTime).ifEmpty { null }
    }

    private fun matchingLiveIndex(
        frame: AIBaseBeen,
        code: String,
        startTime: Long,
        endTime: Long
    ): List<Map<String, Any?>> = frame.bizData.liveIndex.filter {
        it.code.code == code && it.day in startTime..endTime
    }.map(::liveIndexValue)

    private fun liveIndexValue(item: LiveIndexBeen): Map<String, Any?> = linkedMapOf(
        "code" to linkedMapOf("code" to item.code.code, "name" to item.code.name),
        "level" to item.level,
        "day" to item.day,
        "updateTime" to item.updateTime,
        "status" to item.status,
        "desc" to item.desc,
        "content" to item.content
    )

    private fun maximumTemperature(forecasts: List<ForecastBeen>): String? {
        return numericExtreme(
            forecasts.map { preferredValue(it.maxTemp, it.tempDay) },
            true
        )
    }

    private fun minimumTemperature(forecasts: List<ForecastBeen>): String? {
        return numericExtreme(
            forecasts.map { preferredValue(it.minTemp, it.tempNight) },
            false
        )
    }

    private fun preferredValue(primary: String?, fallback: String?): String? {
        return primary?.takeIf { it.isNotBlank() } ?: fallback
    }

    private fun numericExtreme(values: List<String?>, findMaximum: Boolean): String? {
        val parsed = values.mapNotNull { value ->
            value?.toDoubleOrNull()?.let { number -> value to number }
        }
        if (parsed.isEmpty()) return values.firstOrNull { !it.isNullOrBlank() }
        return if (findMaximum) {
            parsed.maxByOrNull { it.second }?.first
        } else {
            parsed.minByOrNull { it.second }?.first
        }
    }

    private fun mergeConditions(first: String?, last: String?): String? {
        val start = first?.takeIf { it.isNotBlank() }
        val end = last?.takeIf { it.isNotBlank() }
        return when {
            start == null -> end
            end == null || start == end -> start
            else -> "${start}转${end}"
        }
    }

    private fun weatherObjectTitle(type: String, resources: Resources): String {
        val titleResId = when (type) {
            "weather_condition" -> R.string.weather_object_title_weather_condition
            "temperature" -> R.string.weather_object_title_temperature
            "apparent_temperature" -> R.string.weather_object_title_apparent_temperature
            "precipitation_probability" ->
                R.string.weather_object_title_precipitation_probability
            "precipitation_amount" -> R.string.weather_object_title_precipitation_amount
            "snowfall_amount" -> R.string.weather_object_title_snowfall_amount
            "humidity" -> R.string.weather_object_title_humidity
            "pressure" -> R.string.weather_object_title_pressure
            "wind" -> R.string.weather_object_title_wind
            "visibility" -> R.string.weather_object_title_visibility
            "ultraviolet" -> R.string.weather_object_title_ultraviolet
            "sunrise_sunset" -> R.string.weather_object_title_sunrise_sunset
            "moon_phase" -> R.string.weather_object_title_moon_phase
            "air_quality" -> R.string.weather_object_title_air_quality
            "carbon_monoxide" -> R.string.weather_object_title_carbon_monoxide
            "nitrogen_dioxide" -> R.string.weather_object_title_nitrogen_dioxide
            "ozone" -> R.string.weather_object_title_ozone
            "pm10" -> R.string.weather_object_title_pm10
            "pm2_5" -> R.string.weather_object_title_pm2_5
            "sulfur_dioxide" -> R.string.weather_object_title_sulfur_dioxide
            "air_quality_forecast" -> R.string.weather_object_title_air_quality_forecast
            "allergy_index" -> R.string.weather_object_title_allergy_index
            "cold_risk_index" -> R.string.weather_object_title_cold_risk_index
            "exercise_index" -> R.string.weather_object_title_exercise_index
            "makeup_index" -> R.string.weather_object_title_makeup_index
            "clothing_index" -> R.string.weather_object_title_clothing_index
            "fishing_index" -> R.string.weather_object_title_fishing_index
            "traffic_index" -> R.string.weather_object_title_traffic_index
            "travel_index" -> R.string.weather_object_title_travel_index
            "car_wash_index" -> R.string.weather_object_title_car_wash_index
            "air_pollution_dispersion" ->
                R.string.weather_object_title_air_pollution_dispersion
            "traffic_restriction" -> R.string.weather_object_title_traffic_restriction
            else -> return type
        }
        return resources.getString(titleResId)
    }

    private fun weatherObjectUnit(type: String): String? = when (type) {
        "temperature", "apparent_temperature" -> "℃"
        "precipitation_probability", "humidity" -> "%"
        "precipitation_amount", "snowfall_amount" -> "mm"
        "pressure" -> "hPa"
        "visibility" -> "km"
        else -> null
    }

    private fun cardExtraContent(type: String, value: Any?): String? {
        val summary = summaryWeatherValue(value)
        val unit = weatherObjectUnit(type)
        val mainContent = summary?.let { "$it${unit.orEmpty()}" }
        val description = descriptionFromValue(value)
        return listOfNotNull(mainContent, description)
            .distinct()
            .joinToString("；")
            .takeIf { it.isNotBlank() }
    }

    private fun formatTime(timestamp: Long, pattern: String): String {
        return SimpleDateFormat(pattern, Locale.CHINA).apply {
            timeZone = chinaTimeZone
        }.format(Date(timestamp))
    }

    /** 从嵌套Map/List中提取适合卡片直接展示的首要值。 */
    private fun summaryWeatherValue(value: Any?): String? {
        return when (value) {
            null -> null
            is String -> value.takeIf { it.isNotBlank() }
            is Number, is Boolean -> value.toString()
            is List<*> -> value.asSequence()
                .mapNotNull(::summaryWeatherValue)
                .firstOrNull()
            is Map<*, *> -> {
                val priorityKeys = listOf(
                    "status",
                    "level",
                    "value",
                    "prompt",
                    "condition",
                    "direction",
                    "name",
                    "subIndex",
                    "concentration",
                    "sunrise",
                    "sunset"
                )
                priorityKeys.asSequence()
                    .mapNotNull { key -> summaryWeatherValue(value[key]) }
                    .firstOrNull()
                    ?: value.entries.asSequence()
                        .filterNot { it.key == "time" || it.key == "date" }
                        .mapNotNull { summaryWeatherValue(it.value) }
                        .firstOrNull()
            }
            else -> value.toString()
        }
    }

    private fun descriptionFromValue(value: Any?): String? {
        return when (value) {
            is List<*> -> value.asSequence()
                .mapNotNull(::descriptionFromValue)
                .firstOrNull()
            is Map<*, *> -> {
                listOf("desc", "description", "content").asSequence()
                    .mapNotNull { key -> value[key] as? String }
                    .firstOrNull { it.isNotBlank() }
                    ?: value.values.asSequence()
                        .mapNotNull(::descriptionFromValue)
                        .firstOrNull()
            }
            else -> null
        }
    }

    private fun parseIsoTime(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        )
        formats.forEach { pattern ->
            try {
                return SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                }.parse(value)?.time
            } catch (_: Exception) {
                // 尝试下一个ISO格式。
            }
        }
        return null
    }

    private fun dateInRange(value: String?, startTime: Long, endTime: Long): Boolean {
        if (value.isNullOrBlank()) return false
        return try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                isLenient = false
                timeZone = chinaTimeZone
            }.parse(value)?.time ?: return false
            timestamp in startTime..endTime
        } catch (_: Exception) {
            false
        }
    }

    private fun endOfNaturalDay(startTime: Long): Long {
        val calendar = java.util.Calendar.getInstance(chinaTimeZone).apply {
            time = Date(startTime)
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    private fun isMeaningful(value: Any?): Boolean = when (value) {
        null -> false
        is Collection<*> -> value.isNotEmpty()
        is Map<*, *> -> value.isNotEmpty()
        else -> true
    }

    private inline fun <T> List<T>.ifEmptyOrElse(block: () -> Any?): Any? {
        return if (isEmpty()) block() else this
    }
}
