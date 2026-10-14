package com.mosi.mosc.ai_weather.analysis

import com.mosi.mosc.ai_weather.been.CardWarnInfo

/** 负责将DM下发的天气预警转换为卡片预警模块。 */
internal class WarnInfoParser {
    fun parse(context: BindingContext): List<CardWarnInfo> {
        // 预警属于强提醒信息：只要DM返回有效alert就生成，不受weatherObjects限制。
        return context.frame.bizData.alert.mapNotNull { alert ->
            val content = alert.content?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            CardWarnInfo(
                icon = null,
                title = "恶劣天气预警",
                content = content
            )
        }
    }
}
