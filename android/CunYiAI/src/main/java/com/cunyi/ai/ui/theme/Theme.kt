package com.cunyi.ai.ui.theme

import androidx.compose.ui.graphics.Color

// ========== 老年人友好配色 ==========

// 主色 - 深绿色（老年友好，减少视觉疲劳）
val PrimaryGreen = Color(0xFF1B5E20)
val PrimaryGreenDark = Color(0xFF0D3D14)
val PrimaryGreenLight = Color(0xFF4CAF50)

// 强调色 - 橙黄色（高对比度，吸引注意）
val AccentOrange = Color(0xFFFF6D00)
val AccentYellow = Color(0xFFFFC107)

// 背景色 - 浅色系
val BackgroundLight = Color(0xFFF5F5F5)
val BackgroundWhite = Color(0xFFFFFFFF)
val BackgroundDark = Color(0xFF121212)

// 文字颜色 - 高对比度
val TextPrimary = Color(0xFF212121)
val TextSecondary = Color(0xFF424242)
val TextOnPrimary = Color(0xFFFFFFFF)
val TextOnDark = Color(0xFFE0E0E0)

// 警报颜色 - 清晰醒目
val AlertRed = Color(0xFFD32F2F)
val AlertOrange = Color(0xFFFF5722)
val AlertYellow = Color(0xFFFFC107)
val AlertGreen = Color(0xFF388E3C)

// 输入框
val InputBackground = Color(0xFFE8F5E9)
val InputBorder = Color(0xFF81C784)

// ========== 尺寸常量（老年人友好）============

object Dimensions {
    // 字体大小 (sp)
    const val FontSizeSmall = 16
    const val FontSizeBody = 18
    const val FontSizeTitle = 24
    const val FontSizeHeadline = 28
    const val FontSizeLarge = 32

    // 按钮尺寸 (dp)
    const val ButtonHeight = 72
    const val ButtonHeightLarge = 88
    const val ButtonMinWidth = 120

    // 图标大小 (dp)
    const val IconSizeSmall = 32
    const val IconSizeMedium = 48
    const val IconSizeLarge = 64

    // 间距 (dp)
    const val SpacingXS = 4
    const val SpacingS = 8
    const val SpacingM = 16
    const val SpacingL = 24
    const val SpacingXL = 32

    // 圆角 (dp)
    const val CornerRadius = 16
    const val CornerRadiusLarge = 24

    // 卡片 (dp)
    const val CardElevation = 4
    const val CardPadding = 20
}
