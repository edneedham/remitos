package com.remitos.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

/* Material 3 typography wired to the shared type scale (`Tokens.Type`).
 *
 * Font family is supplied via `RemitosFontFamily`, which downloads Inter via
 * Google Fonts (see Theme.kt). All sizes / weights / letter-spacings come from
 * the generated `Tokens.Type` table.
 */
fun buildRemitosTypography(family: FontFamily): Typography {
    fun style(s: Tokens.Type.Style): TextStyle =
        TextStyle(
            fontFamily = family,
            fontWeight = s.weight,
            fontSize = s.size,
            lineHeight = s.lineHeight,
            letterSpacing = s.letterSpacing,
        )

    return Typography(
        displayLarge = style(Tokens.Type.displayLarge),
        displayMedium = style(Tokens.Type.displayMedium),
        displaySmall = style(Tokens.Type.displaySmall),
        headlineLarge = style(Tokens.Type.headlineLarge),
        headlineMedium = style(Tokens.Type.headlineMedium),
        headlineSmall = style(Tokens.Type.headlineSmall),
        titleLarge = style(Tokens.Type.titleLarge),
        titleMedium = style(Tokens.Type.titleMedium),
        titleSmall = style(Tokens.Type.titleSmall),
        bodyLarge = style(Tokens.Type.bodyLarge),
        bodyMedium = style(Tokens.Type.bodyMedium),
        bodySmall = style(Tokens.Type.bodySmall),
        labelLarge = style(Tokens.Type.labelLarge),
        labelMedium = style(Tokens.Type.labelMedium),
        labelSmall = style(Tokens.Type.labelSmall),
    )
}

/* Backwards-compatible top-level value; uses the default font family until
 * Inter is wired in (RemitosTheme overrides this with the downloadable Inter
 * family). */
val RemitosTypography: Typography = buildRemitosTypography(FontFamily.Default)
