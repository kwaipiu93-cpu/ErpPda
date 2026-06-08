package com.erp.pda.ui.theme

import androidx.compose.ui.graphics.Color

// ── iOS System Colors ──

// Primary — iOS Blue
val IosBlue = Color(0xFF007AFF)
val IosBlueDark = Color(0xFF0055CC)

// Semantic Colors
val IosGreen = Color(0xFF34C759)
val IosOrange = Color(0xFFFF9500)
val IosRed = Color(0xFFFF3B30)
val IosPink = Color(0xFFFF2D55)
val IosPurple = Color(0xFFAF52DE)
val IosTeal = Color(0xFF5AC8FA)
val IosIndigo = Color(0xFF5856D6)
val IosYellow = Color(0xFFFFCC00)
val IosMint = Color(0xFF00C7BE)

// Grays
val IosGray2 = Color(0xFFAEAEB2)   // placeholder text
val IosGray3 = Color(0xFFC7C7CC)   // disabled
val IosGray4 = Color(0xFFD1D1D6)   // separator
val IosGray5 = Color(0xFFE5E5EA)   // grouped bg
val IosGray6 = Color(0xFFF2F2F7)   // system bg

// Surfaces
val IosWhite = Color(0xFFFFFFFF)
val IosBlack = Color(0xFF000000)
val IosLabel = Color(0xFF1C1C1E)         // primary label
val IosSecondaryLabel = Color(0xFF3C3C43).copy(alpha = 0.6f)  // secondary
val IosTertiaryLabel = Color(0xFF3C3C43).copy(alpha = 0.3f)   // placeholder

// ── Backward compat aliases ──
val Primary = IosBlue
val PrimaryVariant = IosBlueDark
val OnPrimary = IosWhite
val Secondary = IosGreen
val OnSecondary = IosWhite
val Background = IosGray6
val Surface = IosWhite
val OnBackground = IosLabel
val OnSurface = IosLabel
val Success = IosGreen
val Warning = IosOrange
val Error = IosRed

// Dashboard tile accents
val TileBlue = IosBlue
val TileGreen = IosGreen
val TileOrange = IosOrange
val TilePurple = IosPurple
val TileRed = IosRed
val TileTeal = IosTeal
val TileIndigo = IosIndigo
val TileCyan = IosMint
val TileAmber = IosYellow
