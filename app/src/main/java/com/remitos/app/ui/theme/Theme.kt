package com.remitos.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Blue800,
    onPrimary = Color.White,
    primaryContainer = Blue50,
    onPrimaryContainer = Blue900,
    secondary = BrandBlue,
    onSecondary = Color.White,
    secondaryContainer = Blue100,
    onSecondaryContainer = Blue900,
    tertiary = BrandRed,
    onTertiary = Color.White,
    tertiaryContainer = Red50,
    onTertiaryContainer = Red900,
    background = SurfaceLight,
    onBackground = Neutral900,
    surface = SurfaceLight,
    onSurface = Neutral900,
    surfaceVariant = SurfaceDim,
    onSurfaceVariant = Neutral600,
    outline = Neutral400,
    outlineVariant = Neutral200,
    error = Error500,
    onError = Color.White,
    errorContainer = Error100,
    onErrorContainer = Color(0xFFB71C1C),
    inverseSurface = Blue900,
    inverseOnSurface = Blue50,
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
