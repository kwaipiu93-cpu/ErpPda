package com.erp.pda.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * iOS 風格 Light Color Scheme
 * - 背景：iOS systemGray6 (#F2F2F7)
 * - 導航欄：白色 + 毛玻璃效果（用 surface）
 * - Tab Bar：白色半透明
 * - 分隔線：systemGray4 (#D1D1D6)
 */
private val IosColorScheme = lightColorScheme(
    // Primary — iOS Blue
    primary = IosBlue,
    onPrimary = IosWhite,
    primaryContainer = IosBlue.copy(alpha = 0.12f),
    onPrimaryContainer = IosBlueDark,

    // Secondary — iOS Green
    secondary = IosGreen,
    onSecondary = IosWhite,
    secondaryContainer = IosGreen.copy(alpha = 0.12f),
    onSecondaryContainer = Color(0xFF1B5E20),

    // Tertiary — iOS Orange
    tertiary = IosOrange,
    onTertiary = IosWhite,
    tertiaryContainer = IosOrange.copy(alpha = 0.12f),
    onTertiaryContainer = Color(0xFF7F3A00),

    // Background / Surface
    background = IosGray6,
    onBackground = IosLabel,
    surface = IosWhite,
    onSurface = IosLabel,
    surfaceVariant = IosGray5,
    onSurfaceVariant = IosSecondaryLabel,

    // Outline
    outline = IosGray4,
    outlineVariant = IosGray5,

    // Error
    error = IosRed,
    onError = IosWhite,
    errorContainer = IosRed.copy(alpha = 0.12f),
    onErrorContainer = Color(0xFF7F1D1D),

    // Inverse
    inverseSurface = IosLabel,
    inverseOnSurface = IosWhite,
    inversePrimary = IosBlueDark
)

@Composable
fun ErpPdaTheme(content: @Composable () -> Unit) {
    val colorScheme = IosColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // iOS 風格：亮色 status bar（黑字）
            window.statusBarColor = IosWhite.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ErpTypography,
        shapes = IosShapes,
        content = content
    )
}
