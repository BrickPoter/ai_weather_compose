package com.mosi.mosc.launcher.ai_weather.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// ═══════════════════════════════════════════════════════
// 主题配置 — 整个 App 的颜色/字体统一在这里管理
//
// @Composable 是 Compose 的核心注解:
// 标记这个函数是一个"可组合函数" — 它描述了一段 UI,
// Compose 编译器会自动处理它的刷新和重组
// ═══════════════════════════════════════════════════════

/**
 * App 主题 — 所有页面的祖先, 提供颜色和字体
 *
 * MaterialTheme { ... } 包裹的内容会自动继承里面的颜色方案。
 * 所以我们写 UI 时直接用 Color(0xFF...) 或者
 * MaterialTheme.colorScheme.primary 都可以 (本 Demo 用前者)。
 *
 * @param darkTheme 是否使用暗色主题 (车机永远 true)
 * @param dynamicColor 是否使用 Android 12+ 动态取色 (车机关闭)
 * @param content 子内容 — 整个 App 的 UI 树
 */
@Composable
fun AiLauncherTheme(
    darkTheme: Boolean = true,            // 默认暗色 (车机)
    dynamicColor: Boolean = false,        // 关闭动态取色 (车机用固定色板)
    content: @Composable () -> Unit       // 这个参数是一个函数, 接收所有子 UI
) {
    // colorScheme 告诉 Material3: 用什么颜色画按钮/文字/背景...
    // 我们用 Color.kt 里定义的 AccentBlue 等替换了默认的紫色
    val colorScheme = darkColorScheme(
        primary = AccentBlue,           // 主色 (按钮/选中态)
        secondary = AccentCyan,         // 副色
        tertiary = AccentGreen,         // 第三色
        background = BgPrimary,         // 背景色
        surface = BgSecondary,          // 表面色 (卡片/面板)
        onPrimary = TextPrimary,        // 主色上的文字颜色
        onSecondary = TextPrimary,      // 副色上的文字颜色
        onTertiary = TextPrimary,
        onBackground = TextPrimary,     // 背景上的文字颜色
        onSurface = TextPrimary         // 表面上的文字颜色
    )

    // MaterialTheme 是 Material3 设计系统的根组件
    // 它把 colorScheme 和 typography 注入到整个 UI 树中
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,     // Type.kt 中定义的字体
        content = content            // 把子 UI 传进去渲染
    )
}
