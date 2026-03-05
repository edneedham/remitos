package com.remitos.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlue,
    onPrimaryContainer = Color.White,
    secondary = BrandBlue,
    onSecondary = Color.White,
    secondaryContainer = BrandBlue,
    onSecondaryContainer = Color.White,
    tertiary = BrandRed,
    onTertiary = Color.White,
    tertiaryContainer = Red100,
    onTertiaryContainer = Red900,
    background = Color.White,
    onBackground = Neutral900,
    surface = Color.White,
    onSurface = Neutral900,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral700,
    outline = Neutral500,
    outlineVariant = Neutral300,
    error = Error500,
    onError = Color.White,
    errorContainer = Error100,
    onErrorContainer = Color(0xFF8B0000),
    inverseSurface = BrandBlue,
    inverseOnSurface = Color.White,
    inversePrimary = Blue300,
    surfaceTint = BrandBlue,
)

@Composable
fun RemitosTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = RemitosTypography,
        shapes = RemitosShapes,
        content = content,
    )
}
