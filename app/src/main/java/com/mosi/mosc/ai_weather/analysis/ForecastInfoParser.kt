package com.mosi.mosc.ai_weather.analysis

import com.mosi.mosc.ai_weather.been.CardForecastInfo
import com.mosi.mosc.ai_weather.formatter.WeatherTimeFormatter

/** 负责生成多日和小时时间段预报模块。 */
internal class ForecastInfoParser {
    fun parse(context: BindingContext): List<CardForecastInfo> {
        return when (context.granularity) {
            "multi_day" -> parseMultiDay(context)
            "period" -> parsePeriod(context)
            else -> emptyList()
        }
    }

    /** 多日预报只返回weatherObjects明确请求的字段。 */
    private fun parseMultiDay(context: BindingContext): List<CardForecastInfo> {
        val requestedObjects = context.requestedObjects
        return context.forecasts.mapIndexed { index, forecast ->
            CardForecastInfo(
                time = if (index < WEEKDAY_DISPLAY_COUNT) {
                    WeatherTimeFormatter.formatWeekday(forecast.predictDate)
                } else {
                    WeatherTimeFormatter.formatTime(forecast.predictDate, "MM/dd")
                },
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

    /** period固定返回完整小时预报，不受weatherObjects限制。 */
    private fun parsePeriod(context: BindingContext): List<CardForecastInfo> {
        return context.hourlies.map { hourly ->
            CardForecastInfo(
                time = WeatherTimeFormatter.formatTime(hourly.date, "HH:mm"),
                icon = if (hourly.dayNight) hourly.iconDay else hourly.iconNight,
                probability = hourly.pop,
                highTemp = hourly.temp,
                lowTemp = null,
                condition = hourly.condition
            )
        }
    }

    private fun preferredValue(primary: String?, fallback: String?): String? {
        return primary?.takeIf { it.isNotBlank() } ?: fallback
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
        const val WEEKDAY_DISPLAY_COUNT = 4
    }
}
