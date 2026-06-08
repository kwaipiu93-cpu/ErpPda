package com.erp.pda.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.erp.pda.ui.theme.*

/**
 * iOS 風格導航欄 — 白色背景，藍色標題
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    // iOS-style: thin divider at bottom instead of shadow
    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = IosLabel
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = IosBlue
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = IosWhite,
            titleContentColor = IosLabel
        )
    )
    HorizontalDivider(color = IosGray5, thickness = androidx.compose.ui.unit.Dp.Hairline)
}
