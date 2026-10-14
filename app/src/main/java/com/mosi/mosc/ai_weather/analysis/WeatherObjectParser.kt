package com.mosi.mosc.ai_weather.analysis

import com.mosi.mosc.ai_weather.been.ForecastBeen
import com.mosi.mosc.ai_weather.been.HourlyBeen
import com.mosi.mosc.ai_weather.been.LiveIndexBeen
import com.mosi.mosc.ai_weather.formatter.WeatherTimeFormatter
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** 单个binding的weatherObjects解析结果。 */
internal data class WeatherObjectParseResult(
    val values: Map<String, Any?>,
    val missingObjects: List<String>
)

/** 负责按weatherObject机器码从bizData中提取数据。 */
internal class WeatherObjectParser {
    suspend fun parse(context: BindingContext): WeatherObjectParseResult {
        val parsingContext = currentCoroutineContext()
        val values = linkedMapOf<String, Any?>()
        val missingObjects = mutableListOf<String>()

        context.binding.weatherObjects.forEach { weatherObject ->
            parsingContext.ensureActive()
            val value = parseObject(context, weatherObject)
            if (isMeaningful(value)) {
                values[weatherObject] = value
            } else {
                missingObjects += weatherObject
            }
        }

        return WeatherObjectParseResult(
            values = values,
            missingObjects = missingObjects
        )
    }

    private fun parseObject(context: BindingContext, weatherObject: String): Any? {
        val frame = context.frame
        val forecastResult = context.forecasts
        val hourlyResult = context.hourlies
        return when (weatherObject) {
            "temperature" -> when (context.granularity) {
                "instant", "period" -> hourlyRecords(hourlyResult) {
                    linkedMapOf("time" to it.date, "value" to it.temp)
                }
                "day", "multi_day" -> forecastRecords(forecastResult) {
                    linkedMapOf(
                        "time" to it.predictDate,
                        "day" to it.tempDay,
                        "night" to it.tempNight,
                        "maximum" to preferredValue(it.maxTemp, it.tempDay),
                        "minimum" to preferredValue(it.minTemp, it.tempNight)
                    )
                }
                else -> null
            }

            "apparent_temperature" -> when (context.granularity) {
                "instant", "period" -> hourlyRecords(hourlyResult) {
                    linkedMapOf("time" to it.date, "value" to it.realFeel)
                }.ifEmptyOrElse {
                    currentValue(context) { frame.bizData.condition.realFeel }
                }
                else -> currentValue(context) { frame.bizData.condition.realFeel }
            }

            "precipitation_probability" -> when (context.granularity) {
                "instant", "period" -> hourlyRecords(hourlyResult) {
                    linkedMapOf("time" to it.date, "value" to it.pop)
                }
                "day", "multi_day" -> forecastRecords(forecastResult) {
                    linkedMapOf("time" to it.predictDate, "value" to it.pop)
                }
                else -> null
            }

            // 降水量和降雪量仅在小时预报中定义。
            "precipitation_amount" -> hourlyRecords(hourlyResult) {
                linkedMapOf("time" to it.date, "value" to it.qpf)
            }
            "snowfall_amount" -> hourlyRecords(hourlyResult) {
                linkedMapOf("time" to it.date, "value" to it.snow)
            }

            "humidity" -> when (context.granularity) {
                "instant", "period" -> hourlyRecords(hourlyResult) {
                    linkedMapOf("time" to it.date, "value" to it.humidity)
                }.ifEmptyOrElse {
                    currentValue(context) { frame.bizData.condition.humidity }
                }
                "day", "multi_day" -> forecastRecords(forecastResult) {
                    linkedMapOf("time" to it.predictDate, "value" to it.humidity)
                }
                else -> null
            }

            "pressure" -> when (context.granularity) {
                "instant", "period" -> hourlyRecords(hourlyResult) {
                    linkedMapOf("time" to it.date, "value" to it.pressure)
                }.ifEmptyOrElse {
                    currentValue(context) { frame.bizData.condition.pressure }
                }
                else -> currentValue(context) { frame.bizData.condition.pressure }
            }

            "weather_condition" -> when (context.granularity) {
                "instant", "period" -> hourlyRecords(hourlyResult) {
                    linkedMapOf(
                        "time" to it.date,
                        "value" to it.condition,
                        "code" to it.conditionId,
                        "iconDay" to it.iconDay,
                        "iconNight" to it.iconNight
                    )
                }
                "day", "multi_day" -> forecastRecords(forecastResult) {
                    linkedMapOf(
                        "time" to it.predictDate,
                        "day" to it.conditionDay,
                        "dayCode" to it.conditionIdDay,
                        "night" to it.conditionNight,
                        "nightCode" to it.conditionIdNight
                    )
                }
                else -> null
            }

            "wind" -> when (context.granularity) {
                "instant", "period" -> hourlyRecords(hourlyResult) {
                    linkedMapOf(
                        "time" to it.date,
                        "direction" to windDirection(it.windDir.code, it.windDir.name),
                        "level" to it.windLevel,
                        "speed" to it.windSpeed,
                        "degrees" to it.windDegrees
                    )
                }.ifEmptyOrElse {
                    currentValue(context) {
                        linkedMapOf(
                            "direction" to windDirection(
                                frame.bizData.condition.windDir.code,
                                frame.bizData.condition.windDir.name
                            ),
                            "level" to frame.bizData.condition.windLevel,
                            "speed" to frame.bizData.condition.windSpeed,
                            "degrees" to frame.bizData.condition.windDegrees
                        )
                    }
                }
                "day", "multi_day" -> forecastRecords(forecastResult, ::forecastWind)
                else -> null
            }

            "visibility" -> currentValue(context) { frame.bizData.condition.vis }

            "ultraviolet" -> {
                val numericalValue = when (context.granularity) {
                    "instant", "period" -> hourlyRecords(hourlyResult) {
                        linkedMapOf("time" to it.date, "value" to it.uvi)
                    }.ifEmptyOrElse {
                        currentValue(context) { frame.bizData.condition.uvi }
                    }
                    else -> currentValue(context) { frame.bizData.condition.uvi }
                }
                val advice = matchingLiveIndex(context, "21")
                if (numericalValue == null && advice.isEmpty()) null else linkedMapOf(
                    "value" to numericalValue,
                    "advice" to advice
                )
            }

            "sunrise_sunset" -> when (context.granularity) {
                "day", "multi_day" -> forecastRecords(forecastResult) {
                    linkedMapOf(
                        "time" to it.predictDate,
                        "sunrise" to it.sunrise,
                        "sunset" to it.sunset
                    )
                }
                else -> currentValue(context) {
                    linkedMapOf(
                        "sunrise" to frame.bizData.condition.sunRise,
                        "sunset" to frame.bizData.condition.sunSet
                    )
                }
            }

            "moon_phase" -> forecastRecords(forecastResult) {
                linkedMapOf("time" to it.predictDate, "value" to it.moonPhase)
            }

            "weather_alert" -> frame.bizData.alert
                .mapNotNull { it.content }
                .ifEmpty { null }

            "air_quality" -> {
                val aqi = frame.bizData.aqi
                if (aqi.updateTime !in context.startTime..context.endTime) {
                    null
                } else {
                    linkedMapOf(
                        "time" to aqi.updateTime,
                        "value" to aqi.aqiValue,
                        "level" to aqi.aqiValueText,
                        "rank" to aqi.rank,
                        "description" to aqi.aqiDesc
                    )
                }
            }

            "carbon_monoxide" -> pollutant(context, "co")
            "nitrogen_dioxide" -> pollutant(context, "no2")
            "ozone" -> pollutant(context, "o3")
            "pm10" -> pollutant(context, "pm10")
            "pm2_5" -> pollutant(context, "pm25")
            "sulfur_dioxide" -> pollutant(context, "so2")

            "air_quality_forecast" -> frame.bizData.aqiForecastResponse
                .filter {
                    WeatherTimeFormatter.dateInRange(
                        it.date,
                        context.startTime,
                        context.endTime
                    )
                }
                .map {
                    linkedMapOf(
                        "date" to it.date,
                        "publishTime" to it.publishTime,
                        "value" to it.value
                    )
                }.ifEmpty { null }

            // 生活指数通过liveIndex[].code.code匹配，保留数字编码。
            "allergy_index" -> lifeIndex(context, "32")
            "cold_risk_index" -> lifeIndex(context, "12")
            "exercise_index" -> lifeIndex(context, "26")
            "makeup_index" -> lifeIndex(context, "7")
            "clothing_index" -> lifeIndex(context, "20")
            "fishing_index" -> lifeIndex(context, "28")
            "traffic_index" -> lifeIndex(context, "5")
            "travel_index" -> lifeIndex(context, "14")
            "car_wash_index" -> lifeIndex(context, "17")
            "air_pollution_dispersion" -> lifeIndex(context, "18")

            "traffic_restriction" -> {
                val current = frame.bizData.currentLimit.takeIf {
                    WeatherTimeFormatter.dateInRange(
                        it.date,
                        context.startTime,
                        context.endTime
                    )
                }?.let {
                    linkedMapOf("date" to it.date, "prompt" to it.prompt)
                }
                val daily = frame.bizData.limit.filter {
                    WeatherTimeFormatter.dateInRange(
                        it.date,
                        context.startTime,
                        context.endTime
                    )
                }.map {
                    linkedMapOf("date" to it.date, "prompt" to it.prompt)
                }
                if (current == null && daily.isEmpty()) null else linkedMapOf(
                    "current" to current,
                    "daily" to daily
                )
            }

            else -> null
        }
    }

    private fun forecastRecords(
        values: List<ForecastBeen>,
        mapper: (ForecastBeen) -> Map<String, Any?>
    ): List<Map<String, Any?>> = values.map(mapper)

    private fun hourlyRecords(
        values: List<HourlyBeen>,
        mapper: (HourlyBeen) -> Map<String, Any?>
    ): List<Map<String, Any?>> = values.map(mapper)

    private fun currentValue(
        context: BindingContext,
        value: () -> Any?
    ): Map<String, Any?>? {
        if (!context.currentMatches) return null
        return linkedMapOf("time" to context.conditionTime, "value" to value())
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

    private fun pollutant(context: BindingContext, type: String): Map<String, Any?>? {
        val aqi = context.frame.bizData.aqi
        if (aqi.updateTime !in context.startTime..context.endTime) return null
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
        context: BindingContext,
        code: String
    ): List<Map<String, Any?>>? {
        return matchingLiveIndex(context, code).ifEmpty { null }
    }

    private fun matchingLiveIndex(
        context: BindingContext,
        code: String
    ): List<Map<String, Any?>> = context.frame.bizData.liveIndex.filter {
        it.code.code == code && it.day in context.startTime..context.endTime
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

    private fun preferredValue(primary: String?, fallback: String?): String? {
        return primary?.takeIf { it.isNotBlank() } ?: fallback
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
