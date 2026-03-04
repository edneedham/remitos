package com.remitos.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Blue800,
    onPrimary = Color.White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue900,
    secondary = BrandBlue,
    onSecondary = Color.White,
    secondaryContainer = Blue100,
    onSecondaryContainer = Blue900,
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
    inverseSurface = Blue900,
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
