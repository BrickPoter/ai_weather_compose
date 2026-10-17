package com.mosi.mosc.launcher.ai_weather.compose.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════
// 字体排版设置
//
// Typography 定义了全局的文字风格
// Material3 会自动把这些风格应用到对应的组件上:
//   bodyLarge → 正文
//   titleLarge → 大标题
//   labelSmall → 小标签
// ═══════════════════════════════════════════════════════

val Typography = Typography(
    // 正文样式: 16sp 字号, 正常粗细, 24sp 行高
    // sp = scale-independent pixels, 会跟随系统字体大小缩放
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,   // 系统默认字体
        fontWeight = FontWeight.Normal,    // 正常粗细 (不是粗体)
        fontSize = 16.sp,                  // 字号
        lineHeight = 24.sp,                // 行高 (字号 × 1.5)
        letterSpacing = 0.5.sp             // 字间距
    )
    // 注: 本 Demo 大部分文字直接指定 fontSize, 不依赖 Typography
    // 这里保留模板默认设置作为 fallback
)
