package com.erp.pda.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * iOS 風格圓角（10-14dp 大圓角）
 * - extraSmall: 6dp（iOS icon 角）
 * - small: 8dp（小卡片）
 * - medium: 12dp（標準卡片/按鈕）
 * - large: 16dp（大卡片/模態）
 * - extraLarge: 20dp（特大，接近 iOS sheet）
 */
val IosShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)
