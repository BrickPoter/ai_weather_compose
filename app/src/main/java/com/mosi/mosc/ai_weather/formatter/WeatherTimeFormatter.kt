package com.mosi.mosc.ai_weather.formatter

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** 天气业务统一使用的时间解析、范围计算和展示格式化工具。 */
internal object WeatherTimeFormatter {
    private val chinaTimeZone: TimeZone = TimeZone.getTimeZone("GMT+08:00")

    fun formatTime(timestamp: Long, pattern: String): String {
        return SimpleDateFormat(pattern, Locale.CHINA).apply {
            timeZone = chinaTimeZone
        }.format(Date(timestamp))
    }

    /** 固定返回“周一”等中文格式，避免不同系统返回“星期一”等不同文案。 */
    fun formatWeekday(timestamp: Long): String {
        val weekDays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
        val calendar = Calendar.getInstance(chinaTimeZone).apply {
            time = Date(timestamp)
        }
        return weekDays[calendar.get(Calendar.DAY_OF_WEEK) - 1]
    }

    fun formatClockTime(value: Any?): String? {
        val timestamp = when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        } ?: return null
        return formatTime(timestamp, "HH:mm")
    }

    fun parseIsoTime(value: String?): Long? {
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

    fun dateInRange(value: String?, startTime: Long, endTime: Long): Boolean {
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

    fun startOfNaturalDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance(chinaTimeZone).apply {
            time = Date(timestamp)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun endOfNaturalDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance(chinaTimeZone).apply {
            time = Date(timestamp)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }
}
