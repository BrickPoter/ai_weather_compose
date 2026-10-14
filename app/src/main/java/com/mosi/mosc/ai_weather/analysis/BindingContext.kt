package com.mosi.mosc.ai_weather.analysis

import android.content.res.Resources
import com.mosi.mosc.ai_weather.been.AIBaseBeen
import com.mosi.mosc.ai_weather.been.BindingsBeen
import com.mosi.mosc.ai_weather.been.ForecastBeen
import com.mosi.mosc.ai_weather.been.HourlyBeen
import com.mosi.mosc.ai_weather.formatter.WeatherTimeFormatter
import java.util.Locale

/**
 * 单个binding解析期间共享的上下文。
 * 时间范围及forecast/hourly只在创建时计算一次，后续解析模块直接复用。
 */
internal data class BindingContext(
    val frame: AIBaseBeen,
    val binding: BindingsBeen,
    val resources: Resources,
    val granularity: String,
    val startTime: Long,
    val endTime: Long,
    val forecasts: List<ForecastBeen>,
    val hourlies: List<HourlyBeen>,
    val conditionTime: Long?,
    val currentMatches: Boolean,
    val requestedObjects: Set<String>
) {
    val baseShowTime: String
        get() = if (granularity == "period") {
            WeatherTimeFormatter.formatTime(startTime, "M月d日")
        } else {
            binding.time.expression
        }

    companion object {
        private const val DEFAULT_MULTI_DAY_COUNT = 8

        fun create(
            frame: AIBaseBeen,
            binding: BindingsBeen,
            resources: Resources
        ): BindingContext? {
            val startTime = WeatherTimeFormatter.parseIsoTime(binding.time.startTime)
                ?: return null
            val granularity = binding.time.granularity.lowercase(Locale.ROOT)
            val endTime = WeatherTimeFormatter.parseIsoTime(binding.time.endTime)
                ?: when (granularity) {
                    "day", "multi_day" -> WeatherTimeFormatter.endOfNaturalDay(startTime)
                    else -> startTime
                }
            val forecasts = when (granularity) {
                "day","multi_day" -> frame.bizData.forecast
                    .filter { it.predictDate in startTime..endTime }
                    .sortedBy { it.predictDate }
//                "multi_day" -> frame.bizData.forecast
//                    .filter {
//                        it.predictDate >= WeatherTimeFormatter.startOfNaturalDay(startTime)
//                    }
//                    .sortedBy { it.predictDate }
//                    .take(DEFAULT_MULTI_DAY_COUNT)
                else -> emptyList()
            }
            val hourlies = when (granularity) {
                "instant", "period" -> frame.bizData.hourly
                    .filter { it.date in startTime..endTime }
                    .sortedBy { it.date }
                else -> emptyList()
            }
            val conditionTime = frame.bizData.condition.updateTime.toLongOrNull()

            return BindingContext(
                frame = frame,
                binding = binding,
                resources = resources,
                granularity = granularity,
                startTime = startTime,
                endTime = endTime,
                forecasts = forecasts,
                hourlies = hourlies,
                conditionTime = conditionTime,
                currentMatches = conditionTime?.let { it in startTime..endTime } == true,
                requestedObjects = binding.weatherObjects.toSet()
            )
        }
    }
}
