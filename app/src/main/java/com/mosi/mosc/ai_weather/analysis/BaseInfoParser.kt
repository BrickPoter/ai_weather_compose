package com.mosi.mosc.ai_weather.analysis

import com.mosi.mosc.ai_weather.been.CardBaseInfo
import com.mosi.mosc.ai_weather.formatter.WeatherTimeFormatter

/** 单个binding的基础天气模块解析结果。 */
internal data class BaseInfoParseResult(
    /** CardUIData始终使用的完整基础信息。 */
    val cardInfo: CardBaseInfo,
    /** 只有binding真正命中基础对象时，才用于兼容旧base/compare回调。 */
    val legacyCardInfo: CardBaseInfo?,
    /** 已被基础模块消费、不应再进入extraInfo的weatherObjects。 */
    val consumedObjects: Set<String>
)

/** 负责生成基础天气模块并补齐CardUIData所需的完整字段。 */
internal class BaseInfoParser {
    fun parse(
        context: BindingContext,
        bindingData: Map<String, Any?>
    ): BaseInfoParseResult {
        val matchedObjects = BASE_OBJECTS.filterTo(linkedSetOf()) {
            bindingData.containsKey(it)
        }
        val parsed = buildParsedBaseInfo(context, matchedObjects)
        val buildsLegacyModule = context.granularity != "multi_day" &&
                matchedObjects.isNotEmpty()

        return BaseInfoParseResult(
            cardInfo = completeBaseInfo(
                parsed = parsed,
                requiredDailyInfo = buildRequiredBaseInfo(context)
            ),
            legacyCardInfo = parsed.takeIf { buildsLegacyModule },
            consumedObjects = matchedObjects.takeIf { buildsLegacyModule }
                ?: emptySet()
        )
    }

    /** 只写入weatherObjects实际匹配到的基础字段。 */
    private fun buildParsedBaseInfo(
        context: BindingContext,
        matchedObjects: Set<String>
    ): CardBaseInfo {
        val cardInfo = emptyCardInfo(context)
        if ("temperature" in matchedObjects) {
            when (context.granularity) {
                "instant", "period" -> if (context.hourlies.isNotEmpty()) {
                    cardInfo.currentTemp = context.hourlies.first().temp
                    cardInfo.highTemp = numericExtreme(
                        context.hourlies.map { it.temp },
                        findMaximum = true
                    )
                    cardInfo.lowTemp = numericExtreme(
                        context.hourlies.map { it.temp },
                        findMaximum = false
                    )
                }
                "day", "multi_day" -> targetDateForecast(context)?.let { forecast ->
                    // 基础卡片表示binding起始日当天，不对多天forecast求整体极值。
                    cardInfo.highTemp = forecast.tempDay
                    cardInfo.lowTemp = forecast.tempNight
                    if (context.currentMatches) {
                        cardInfo.currentTemp = context.frame.bizData.condition.temp
                    }
                }
            }
        }

        if ("weather_condition" in matchedObjects) {
            when (context.granularity) {
                "instant", "period" -> if (context.hourlies.isNotEmpty()) {
                    val first = context.hourlies.first()
                    val last = context.hourlies.last()
                    cardInfo.icon = first.condition
                    cardInfo.tempInfo = mergeConditions(first.condition, last.condition)
                }
                "day", "multi_day" -> targetDateForecast(context)?.let { forecast ->
                    cardInfo.icon = forecast.conditionDay
                    cardInfo.tempInfo = mergeConditions(
                        forecast.conditionDay,
                        forecast.conditionNight
                    )
                }
            }
        }
        return cardInfo
    }

    /**
     * 保留weatherObjects已解析的基础字段，并用整日基础数据补齐空字段。
     * 这里只补CardUIData，不修改bindingData/displayPlan的实际匹配结果。
     */
    private fun completeBaseInfo(
        parsed: CardBaseInfo,
        requiredDailyInfo: CardBaseInfo
    ): CardBaseInfo {
        return CardBaseInfo(
            icon = preferredValue(requiredDailyInfo.icon, parsed.icon),
            location = preferredValue(parsed.location, requiredDailyInfo.location),
            showTime = preferredValue(parsed.showTime, requiredDailyInfo.showTime),
            currentTemp = preferredValue(requiredDailyInfo.currentTemp, parsed.currentTemp),
            highTemp = preferredValue(requiredDailyInfo.highTemp, parsed.highTemp),
            lowTemp = preferredValue(requiredDailyInfo.lowTemp, parsed.lowTemp),
            tempInfo = preferredValue(requiredDailyInfo.tempInfo, parsed.tempInfo)
        )
    }

    /**
     * CardUIData的baseInfo统一表示binding起始日期的整日概况：
     * 天气现象和高低温优先取目标日forecast，forecast缺失时才使用hourly兜底。
     */
    private fun buildRequiredBaseInfo(context: BindingContext): CardBaseInfo {
        val frame = context.frame
        val condition = frame.bizData.condition
        val dayStart = WeatherTimeFormatter.startOfNaturalDay(context.startTime)
        val dayEnd = WeatherTimeFormatter.endOfNaturalDay(context.startTime)
        val hourlies = frame.bizData.hourly
            .filter { it.date in dayStart..dayEnd }
            .sortedBy { it.date }
        val dailyForecast = targetDateForecast(context)
        val hourlyTemperatures = hourlies.map { it.temp }
        val highTemp = dailyForecast?.tempDay?.takeIf { it.isNotBlank() }
            ?: numericExtreme(hourlyTemperatures, findMaximum = true)
            ?: condition.temp
        val lowTemp = dailyForecast?.tempNight?.takeIf { it.isNotBlank() }
            ?: numericExtreme(hourlyTemperatures, findMaximum = false)
            ?: condition.temp
        val tempInfo = dailyForecast?.let {
            mergeConditions(it.conditionDay, it.conditionNight)
        } ?: preferredValue(condition.condition, condition.tips)

        return CardBaseInfo(
            icon = preferredValue(dailyForecast?.conditionDay, condition.condition),
            location = preferredValue(
                frame.bizData.city.name,
                frame.bizData.city.secondaryName
            ),
            showTime = context.baseShowTime,
            currentTemp = condition.temp,
            highTemp = highTemp,
            lowTemp = lowTemp,
            tempInfo = tempInfo
        )
    }

    private fun emptyCardInfo(context: BindingContext): CardBaseInfo {
        val frame = context.frame
        return CardBaseInfo(
            icon = null,
            location = preferredValue(
                frame.bizData.city.name,
                frame.bizData.city.secondaryName
            ),
            showTime = context.baseShowTime,
            currentTemp = null,
            highTemp = null,
            lowTemp = null,
            tempInfo = null
        )
    }

    /** 按binding起始时间定位对应自然日的forecast。 */
    private fun targetDateForecast(context: BindingContext) =
        context.frame.bizData.forecast
            .filter {
                it.predictDate in WeatherTimeFormatter.startOfNaturalDay(context.startTime)..
                        WeatherTimeFormatter.endOfNaturalDay(context.startTime)
            }
            .minByOrNull { it.predictDate }

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

    private companion object {
        val BASE_OBJECTS = setOf("weather_condition", "temperature")
    }
}
