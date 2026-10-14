package com.mosi.mosc.ai_weather.analysis

import android.content.res.Resources
import com.mosi.mosc.ai_weather.R
import com.mosi.mosc.ai_weather.been.CardExtraInfo
import com.mosi.mosc.ai_weather.formatter.WeatherTimeFormatter
import kotlin.random.Random

/** 负责额外信息模块的转换、兜底、去重和数量补齐。 */
internal class ExtraInfoParser(
    private val random: Random = Random.Default
) {
    fun parse(
        context: BindingContext,
        bindingData: Map<String, Any?>,
        consumedObjects: Set<String>,
        hasAlert: Boolean
    ): List<CardExtraInfo> {
        val parsedExtraInfo = mutableListOf<CardExtraInfo>()
        bindingData.forEach { (weatherObject, value) ->
            if (weatherObject !in consumedObjects) {
                if (weatherObject == "sunrise_sunset") {
                    parsedExtraInfo += sunriseSunsetExtraInfo(value, context.resources)
                } else {
                    parsedExtraInfo += CardExtraInfo(
                        title = weatherObjectTitle(weatherObject, context.resources),
                        content = cardExtraContent(
                            type = weatherObject,
                            value = value,
                            windDirectionName = context.frame.bizData.condition.windDir.name,
                            windLevel = context.frame.bizData.condition.windLevel
                        )
                    )
                }
            }
        }

        val validExtraInfo = parsedExtraInfo
            .filterNot { it.content.isNullOrBlank() }
            .toMutableList()
        val supplementalCandidates by lazy {
            supplementalExtraInfoCandidates(context)
        }
        val ruleBasedExtraInfo = when (validExtraInfo.size) {
            0 -> fallbackExtraInfo(context).toMutableList()
            1 -> supplementExtraInfo(
                current = validExtraInfo,
                candidates = supplementalCandidates,
                allowedTypes = setOf("ultraviolet", "clothing_index"),
                targetSize = 2
            )
            2 -> validExtraInfo
            in 3..5 -> supplementExtraInfo(
                current = validExtraInfo,
                candidates = supplementalCandidates,
                allowedTypes = SUPPLEMENTAL_EXTRA_INFO_TYPES,
                targetSize = 6
            )
            else -> validExtraInfo
        }

        if (!hasAlert) return ruleBasedExtraInfo

        // 预警卡片固定使用6个额外信息：保留原顺序，不足时随机补齐，超过时取前6个。
        return supplementExtraInfo(
            current = ruleBasedExtraInfo.take(6),
            candidates = supplementalCandidates,
            allowedTypes = SUPPLEMENTAL_EXTRA_INFO_TYPES,
            targetSize = 6
        ).take(6)
    }

    /** extraInfo无有效内容时，默认提供紫外线和穿衣指数。 */
    private fun fallbackExtraInfo(context: BindingContext): List<CardExtraInfo> {
        // 保持原兜底规则：未提供endTime时，生活指数匹配到起始日当天结束。
        val fallbackEndTime = WeatherTimeFormatter.parseIsoTime(
            context.binding.time.endTime
        ) ?: WeatherTimeFormatter.endOfNaturalDay(context.startTime)
        val ultraviolet = fallbackLifeIndexStatus(
            context, "21", endTime = fallbackEndTime
        ) ?: context.frame.bizData.condition.uvi.takeIf { it.isNotBlank() }
        val clothing = fallbackLifeIndexStatus(
            context, "20", endTime = fallbackEndTime
        )

        return listOfNotNull(
            ultraviolet?.let {
                CardExtraInfo(
                    title = context.resources.getString(
                        R.string.weather_object_title_ultraviolet
                    ),
                    content = it
                )
            },
            clothing?.let {
                CardExtraInfo(
                    title = context.resources.getString(
                        R.string.weather_object_title_clothing_index
                    ),
                    content = it
                )
            }
        )
    }

    /**
     * 从候选类型中随机补充extraInfo，并按展示标题去重。
     * 候选数据为空时不生成占位项，因此上游数据不足时最终数量可能小于目标值。
     */
    private fun supplementExtraInfo(
        current: List<CardExtraInfo>,
        candidates: Map<String, CardExtraInfo>,
        allowedTypes: Set<String>,
        targetSize: Int
    ): MutableList<CardExtraInfo> {
        val result = current.toMutableList()
        if (result.size >= targetSize) return result

        val existingTitles = result.mapNotNull { item ->
            item.title?.trim()?.takeIf { it.isNotEmpty() }
        }.toMutableSet()
        val availableCandidates = candidates.entries
            .filter { (type, item) ->
                type in allowedTypes &&
                    !item.content.isNullOrBlank() &&
                    item.title?.trim()?.let { it !in existingTitles } == true
            }
            .shuffled(random)

        availableCandidates.forEach { (_, item) ->
            if (result.size >= targetSize) return@forEach
            val title = item.title?.trim() ?: return@forEach
            if (existingTitles.add(title)) {
                result += item
            }
        }
        return result
    }

    /** 构造允许用于补齐extraInfo的有效候选项。 */
    private fun supplementalExtraInfoCandidates(
        context: BindingContext
    ): Map<String, CardExtraInfo> {
        val result = linkedMapOf<String, CardExtraInfo>()
        val frame = context.frame
        val condition = frame.bizData.condition

        fun addCandidate(type: String, value: Any?) {
            val content = cardExtraContent(
                type = type,
                value = value,
                windDirectionName = condition.windDir.name,
                windLevel = condition.windLevel
            )?.takeIf { it.isNotBlank() } ?: return
            result[type] = CardExtraInfo(
                title = weatherObjectTitle(type, context.resources),
                content = content
            )
        }

        val aqi = frame.bizData.aqi
        addCandidate(
            "air_quality",
            linkedMapOf(
                "value" to aqi.aqiValue,
                "level" to aqi.aqiValueText,
                "rank" to aqi.rank,
                "description" to aqi.aqiDesc
            )
        )
        addCandidate("wind", Unit)
        addCandidate("humidity", linkedMapOf("value" to condition.humidity))

        val precipitationProbability = context.hourlies.firstOrNull()?.pop
            ?: context.forecasts.firstOrNull()?.pop
            ?: frame.bizData.hourly.firstOrNull()?.pop
            ?: frame.bizData.forecast.firstOrNull()?.pop
        addCandidate(
            "precipitation_probability",
            precipitationProbability?.let { linkedMapOf("value" to it) }
        )

        val ultraviolet = fallbackLifeIndexStatus(context, "21")
            ?: condition.uvi.takeIf { it.isNotBlank() }
        addCandidate(
            "ultraviolet",
            ultraviolet?.let { linkedMapOf("status" to it) }
        )

        val clothing = fallbackLifeIndexStatus(context, "20")
        addCandidate(
            "clothing_index",
            clothing?.let { linkedMapOf("status" to it) }
        )

        val allergy = fallbackLifeIndexStatus(context, "32")
        addCandidate(
            "allergy_index",
            allergy?.let { linkedMapOf("status" to it) }
        )

        return result
    }

    private fun fallbackLifeIndexStatus(
        context: BindingContext,
        code: String,
        startTime: Long = context.startTime,
        endTime: Long = context.endTime
    ): String? {
        val candidates = context.frame.bizData.liveIndex
            .filter { it.code.code == code }
            .sortedBy { it.day }
        val selected = candidates.firstOrNull {
            it.day in startTime..endTime
        } ?: candidates.firstOrNull()
        return selected?.status?.takeIf { it.isNotBlank() }
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

    private fun cardExtraContent(
        type: String,
        value: Any?,
        windDirectionName: String,
        windLevel: String
    ): String? {
        // 白名单对象展示天气数据本身；其他额外信息只展示status。
        if (type !in EXTRA_OBJECTS_USING_WEATHER_VALUE) {
            return statusFromValue(value)
        }
        if (type == "wind") {
            return windContent(windDirectionName, windLevel)
        }

        val summary = summaryWeatherValue(value)
        val unit = weatherObjectUnit(type)
        val mainContent = summary?.let { "$it${unit.orEmpty()}" }
        val description = descriptionFromValue(value)
        return listOfNotNull(mainContent, description)
            .distinct()
            .joinToString("；")
            .takeIf { it.isNotBlank() }
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

    /** 从嵌套Map/List中查找第一个status；没有status时不使用其他字段兜底。 */
    private fun statusFromValue(value: Any?): String? {
        return when (value) {
            is List<*> -> value.asSequence()
                .mapNotNull(::statusFromValue)
                .firstOrNull()
            is Map<*, *> -> value["status"]
                ?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: value.values.asSequence()
                    .mapNotNull(::statusFromValue)
                    .firstOrNull()
            else -> null
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

    /** 使用condition中的风向名称和风力等级生成类似“西北风2级”的文案。 */
    private fun windContent(directionName: String, level: String): String? {
        val direction = directionName.trim().takeIf { it.isNotEmpty() }
        val normalizedLevel = level.trim()
            .takeIf { it.isNotEmpty() }
            ?.let { if (it.endsWith("级")) it else "${it}级" }
        return listOfNotNull(direction, normalizedLevel)
            .joinToString("")
            .takeIf { it.isNotEmpty() }
    }

    /** 将日出和日落拆成两个额外信息项，content仅保留时分。 */
    private fun sunriseSunsetExtraInfo(
        value: Any?,
        resources: Resources
    ): List<CardExtraInfo> {
        val times = sunriseSunsetTimes(value) ?: return emptyList()
        val result = mutableListOf<CardExtraInfo>()
        times.first?.let {
            result += CardExtraInfo(
                title = resources.getString(R.string.weather_object_title_sunrise),
                content = it
            )
        }
        times.second?.let {
            result += CardExtraInfo(
                title = resources.getString(R.string.weather_object_title_sunset),
                content = it
            )
        }
        return result
    }

    private fun sunriseSunsetTimes(value: Any?): Pair<String?, String?>? {
        return when (value) {
            is List<*> -> value.asSequence()
                .mapNotNull(::sunriseSunsetTimes)
                .firstOrNull()
            is Map<*, *> -> {
                val sunrise = WeatherTimeFormatter.formatClockTime(value["sunrise"])
                val sunset = WeatherTimeFormatter.formatClockTime(value["sunset"])
                if (sunrise != null || sunset != null) {
                    sunrise to sunset
                } else {
                    value.values.asSequence()
                        .mapNotNull(::sunriseSunsetTimes)
                        .firstOrNull()
                }
            }
            else -> null
        }
    }

    private companion object {
        val EXTRA_OBJECTS_USING_WEATHER_VALUE = setOf(
            "precipitation_probability",
            "precipitation_amount",
            "snowfall_amount",
            "humidity",
            "wind",
            "sunrise_sunset",
            "air_quality"
        )

        val SUPPLEMENTAL_EXTRA_INFO_TYPES = setOf(
            "air_quality",
            "wind",
            "humidity",
            "precipitation_probability",
            "ultraviolet",
            "clothing_index",
            "allergy_index"
        )
    }
}
