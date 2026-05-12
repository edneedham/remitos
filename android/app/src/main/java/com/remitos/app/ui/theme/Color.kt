package com.remitos.app.ui.theme

import androidx.compose.ui.graphics.Color

/* All color values are derived from `Tokens`, which is generated from
 * design-tokens/tokens.json. Edit that file (and run :app:genTokens) to change
 * any of these.
 *
 * The named aliases below exist for compatibility with code written before
 * Tokens.kt landed. Prefer `Tokens.Color.*` in new code.
 */

// Brand
val BrandBlue: Color = Tokens.Color.brandPrimary
val BrandRed: Color = Tokens.Color.brandRed

// Blue palette – kept for legacy callers. Numbered shades map to the closest
// equivalent in the shared token set.
val Blue900: Color = Tokens.Color.brandPrimaryPressed
val Blue800: Color = Tokens.Color.brandPrimaryHover
val Blue700: Color = Tokens.Color.brandPrimaryHover
val Blue600: Color = Tokens.Color.brandPrimary
val Blue500: Color = Tokens.Color.brandPrimary
val Blue400: Color = Tokens.Color.brandPrimary
val Blue300: Color = Tokens.Color.brandPrimarySubtle
val Blue200: Color = Tokens.Color.brandPrimarySubtle
val Blue100: Color = Tokens.Color.brandPrimarySubtle
val Blue50: Color = Tokens.Color.brandPrimarySurface

// Red palette – kept for legacy callers.
val Red900: Color = Tokens.Color.errorOn
val Red800: Color = Tokens.Color.errorOn
val Red700: Color = Tokens.Color.brandRedHover
val Red600: Color = Tokens.Color.brandRed
val Red500: Color = Tokens.Color.brandRed
val Red400: Color = Tokens.Color.brandRed
val Red300: Color = Tokens.Color.brandRedSubtle
val Red200: Color = Tokens.Color.brandRedSubtle
val Red100: Color = Tokens.Color.brandRedSubtle
val Red50: Color = Tokens.Color.brandRedSubtle

// Neutral palette.
val Neutral50: Color = Tokens.Color.n50
val Neutral100: Color = Tokens.Color.n100
val Neutral200: Color = Tokens.Color.n200
val Neutral300: Color = Tokens.Color.n300
val Neutral400: Color = Tokens.Color.n400
val Neutral500: Color = Tokens.Color.n500
val Neutral600: Color = Tokens.Color.n600
val Neutral700: Color = Tokens.Color.n700
val Neutral800: Color = Tokens.Color.n800
val Neutral900: Color = Tokens.Color.n900

// Semantic.
val Success500: Color = Tokens.Color.success
val Success100: Color = Tokens.Color.successSubtle
val Warning500: Color = Tokens.Color.warning
val Warning100: Color = Tokens.Color.warningSubtle
val Error500: Color = Tokens.Color.error
val Error100: Color = Tokens.Color.errorSubtle

// Surface & background.
val SurfaceLight: Color = Tokens.Color.surfaceBackground
val SurfaceDim: Color = Tokens.Color.surfaceMuted

// Disabled button colors.
val DisabledButtonBackground: Color = Tokens.Color.buttonDisabledBackground
val DisabledButtonContent: Color = Tokens.Color.buttonDisabledContent
