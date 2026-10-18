package com.mosi.mosc.launcher.ai_weather.compose.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.mosi.mosc.launcher.ai_weather.R

object AppTextStyles {

    // 温度数字字体
    val DinBegFont = FontFamily(
        Font(R.font.dinbeg_0)
    )

    // 基础信息-当前温度
    val TitleCurrentTemp = TextStyle(
        fontFamily = DinBegFont,
        fontSize = 128.sp,
        lineHeight = 160.sp
    )

    // 基础信息-最高最低温（title）
    val TitleTemp = TextStyle(
        fontFamily = DinBegFont,
        fontSize = 104.sp,
        lineHeight = 136.sp
    )

    // 基础信息-最高最低温（content）
    val ContentTemp = TextStyle(
        fontFamily = DinBegFont,
        fontSize = 40.sp,
        lineHeight = 60.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,  // 行内垂直居中
            trim = LineHeightStyle.Trim.None
        )
    )

    // 其他文字
    val ContentChinese_34 = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 34.sp,
        lineHeight = 50.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,  // 行内垂直居中
            trim = LineHeightStyle.Trim.None
        )
    )

    // 其他文字
    val ContentChinese_36 = TextStyle(
        fontSize = 36.sp,
        lineHeight = 50.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,  // 行内垂直居中
            trim = LineHeightStyle.Trim.None
        )
    )
}