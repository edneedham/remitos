// AUTO-GENERATED FROM design-tokens/tokens.json — DO NOT EDIT.
// Run `./gradlew :app:genTokens` after editing tokens.json.
@file:Suppress("unused", "MayBeConstant")

package com.remitos.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Dp

object Tokens {
    object Color {
        // Brand
        val brandPrimary = Color(0xFF006CFF)
        val brandPrimaryHover = Color(0xFF0058D6)
        val brandPrimaryPressed = Color(0xFF0048B3)
        val brandPrimarySubtle = Color(0xFFDBEAFE)
        val brandPrimarySurface = Color(0xFFF0F5FF)
        val brandRed = Color(0xFFD0001E)
        val brandRedHover = Color(0xFFA8001B)
        val brandRedSubtle = Color(0xFFFBE4E8)

        // Neutrals
        val n50 = Color(0xFFFAFAFA)
        val n100 = Color(0xFFF5F5F5)
        val n200 = Color(0xFFE8E8E8)
        val n300 = Color(0xFFBDBDBD)
        val n400 = Color(0xFF9E9E9E)
        val n500 = Color(0xFF757575)
        val n600 = Color(0xFF616161)
        val n700 = Color(0xFF424242)
        val n800 = Color(0xFF2D2D2D)
        val n900 = Color(0xFF1A1A1A)

        // Semantic
        val success = Color(0xFF1F9D55)
        val successSubtle = Color(0xFFD1FAE5)
        val successOn = Color(0xFF065F46)
        val warning = Color(0xFFB45309)
        val warningSubtle = Color(0xFFFEF3C7)
        val warningOn = Color(0xFF7C2D12)
        val error = Color(0xFFD0001E)
        val errorSubtle = Color(0xFFFEE2E2)
        val errorOn = Color(0xFF7F1D1D)
        val info = Color(0xFF006CFF)
        val infoSubtle = Color(0xFFDBEAFE)
        val infoOn = Color(0xFF1E3A8A)

        // Surface
        val surfaceBackground = Color(0xFFFFFFFF)
        val surfaceMuted = Color(0xFFF9FAFB)
        val surfaceBorder = Color(0xFFE5E7EB)
        val surfaceText = Color(0xFF111827)
        val surfaceTextMuted = Color(0xFF4B5563)

        // Buttons
        val buttonDisabledBackground = Color(0xFFE1E2E4)
        val buttonDisabledContent = Color(0xFF919194)
    }

    object Radius {
        val xs: Dp = 6.dp
        val sm: Dp = 8.dp
        val md: Dp = 12.dp
        val lg: Dp = 16.dp
        val xl: Dp = 24.dp
        val pill: Dp = 9999.dp
    }

    object Spacing {
        val item: Dp = 8.dp
        val section: Dp = 16.dp
        val screen: Dp = 20.dp
        val large: Dp = 24.dp
        val xlarge: Dp = 32.dp
        val huge: Dp = 48.dp
    }

    object Type {
        const val family: String = "Inter"
        data class Style(val size: TextUnit, val lineHeight: TextUnit, val weight: FontWeight, val letterSpacing: TextUnit)
        val displayLarge = Style(32.sp, 40.sp, FontWeight(700), -0.5.sp)
        val displayMedium = Style(28.sp, 36.sp, FontWeight(700), -0.25.sp)
        val displaySmall = Style(24.sp, 32.sp, FontWeight(600), 0.sp)
        val headlineLarge = Style(22.sp, 28.sp, FontWeight(700), 0.sp)
        val headlineMedium = Style(20.sp, 26.sp, FontWeight(600), 0.sp)
        val headlineSmall = Style(18.sp, 24.sp, FontWeight(600), 0.sp)
        val titleLarge = Style(18.sp, 24.sp, FontWeight(600), 0.sp)
        val titleMedium = Style(16.sp, 22.sp, FontWeight(500), 0.1.sp)
        val titleSmall = Style(14.sp, 20.sp, FontWeight(500), 0.1.sp)
        val bodyLarge = Style(16.sp, 24.sp, FontWeight(400), 0.15.sp)
        val bodyMedium = Style(14.sp, 20.sp, FontWeight(400), 0.25.sp)
        val bodySmall = Style(12.sp, 16.sp, FontWeight(400), 0.4.sp)
        val labelLarge = Style(14.sp, 20.sp, FontWeight(500), 0.1.sp)
        val labelMedium = Style(12.sp, 16.sp, FontWeight(500), 0.5.sp)
        val labelSmall = Style(11.sp, 16.sp, FontWeight(500), 0.5.sp)
    }
}
