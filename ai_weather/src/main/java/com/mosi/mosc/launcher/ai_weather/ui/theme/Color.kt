package com.mosi.mosc.launcher.ai_weather.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════
// 车机暗色主题色板
//
// Color(0xFF______) 是 Compose 的颜色定义方式:
//   0x = 十六进制前缀
//   FF = 完全不透明 (00=完全透明)
//   后面 6 位 = RGB, 如 0A0E14 = R:0A G:0E B:14
//
// 车机屏幕通常是暗色背景 + 亮色文字, 减少驾驶时的视觉干扰
// ═══════════════════════════════════════════════════════

// ── 背景色 (从深到浅) ──
// 最深的主背景 (屏幕底色)
val BgPrimary = Color(0xFF0A0E14)
// 次级背景 (面板/侧边栏底色)
val BgSecondary = Color(0xFF141B22)
// 卡片背景
val BgCard = Color(0xFF1A2332)
// 卡片悬停时稍微亮一点 (本 Demo 暂未使用)
val BgCardHover = Color(0xFF1E2A3C)

// ── 边框色 ──
val BorderCard = Color(0xFF2A3A4E)

// ── 文字色 (从亮到暗) ──
// 主要文字 (标题/重要信息)
val TextPrimary = Color(0xE5000000)
// 次要文字 (副标题/描述)
val TextSecondary = Color(0x99000000)

// 降雨概率文字
val Probability = Color(0xFF2895FE)
// 预警信息文字与背景颜色
val AlertRedText = Color(0xE5D75353)
val AlertRedBg = Color(0x14EB572B)
val AlertOrangeText = Color(0xFFFC8906)
val AlertOrangeBg = Color(0x14FA8407)
val AlertBg = Color(0x80FFFFFF)

// ── 各类型卡片的强调色 ──
// 每种卡片类型有自己的主题色, 用于边框、标签、图标
val AccentBlue = Color(0xFF3B8BEB)      // 导航
val AccentCyan = Color(0xFF22C5D6)      // 天气
val AccentGreen = Color(0xFF34C759)     // 信号/在线
val AccentOrange = Color(0xFFF5A623)    // 气候/警告
val AccentRed = Color(0xFFE0454B)       // 日历/危险
val AccentPurple = Color(0xFF9B59B6)    // 音乐
val AccentTeal = Color(0xFF1ABC9C)      // 车外世界

// ── 保留原有 Material3 颜色 (模板自带的, 保持兼容) ──
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
