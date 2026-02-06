package com.remitos.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Navy800,
    onPrimary = Color.White,
    primaryContainer = Teal50,
    onPrimaryContainer = Navy900,
    secondary = Teal500,
    onSecondary = Color.White,
    secondaryContainer = Teal100,
    onSecondaryContainer = Navy900,
    tertiary = Teal400,
    onTertiary = Color.White,
    tertiaryContainer = Teal50,
    onTertiaryContainer = Navy800,
    background = SurfaceLight,
    onBackground = Navy900,
    surface = SurfaceLight,
    onSurface = Navy900,
    surfaceVariant = SurfaceDim,
    onSurfaceVariant = Navy600,
    outline = Neutral400,
    outlineVariant = Neutral200,
    error = Error500,
    onError = Color.White,
    errorContainer = Error100,
    onErrorContainer = Color(0xFFB71C1C),
    inverseSurface = Navy800,
    inverseOnSurface = Neutral100,
    inversePrimary = Teal300,
    surfaceTint = Teal500,
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
