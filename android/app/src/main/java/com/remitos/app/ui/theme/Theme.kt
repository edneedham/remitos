package com.remitos.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.remitos.app.R

/* Inter is downloaded at runtime via the Google Fonts provider. The cert array
 * lives in res/values/font_certs.xml. Falling back to the default sans-serif if
 * the provider isn't reachable is automatic via GoogleFont. */
private val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val Inter = GoogleFont("Inter")

private val InterFamily: FontFamily = FontFamily(
    Font(googleFont = Inter, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal),
    Font(googleFont = Inter, fontProvider = GoogleFontsProvider, weight = FontWeight.Medium),
    Font(googleFont = Inter, fontProvider = GoogleFontsProvider, weight = FontWeight.SemiBold),
    Font(googleFont = Inter, fontProvider = GoogleFontsProvider, weight = FontWeight.Bold),
)

private val LightColorScheme = lightColorScheme(
    primary = Tokens.Color.brandPrimary,
    onPrimary = Color.White,
    primaryContainer = Tokens.Color.brandPrimary,
    onPrimaryContainer = Color.White,
    secondary = Tokens.Color.brandPrimary,
    onSecondary = Color.White,
    secondaryContainer = Tokens.Color.brandPrimary,
    onSecondaryContainer = Color.White,
    tertiary = Tokens.Color.brandRed,
    onTertiary = Color.White,
    tertiaryContainer = Tokens.Color.brandRedSubtle,
    onTertiaryContainer = Tokens.Color.errorOn,
    background = Color.White,
    onBackground = Tokens.Color.n900,
    surface = Color.White,
    onSurface = Tokens.Color.n900,
    surfaceVariant = Tokens.Color.n100,
    onSurfaceVariant = Tokens.Color.n700,
    outline = Tokens.Color.n500,
    outlineVariant = Tokens.Color.n300,
    error = Tokens.Color.error,
    onError = Color.White,
    errorContainer = Tokens.Color.errorSubtle,
    onErrorContainer = Tokens.Color.errorOn,
    inverseSurface = Tokens.Color.brandPrimary,
    inverseOnSurface = Color.White,
    inversePrimary = Color.White,
    surfaceTint = Tokens.Color.brandPrimary,
)

@Composable
fun RemitosTheme(
    content: @Composable () -> Unit,
) {
    val typography = remember { buildRemitosTypography(InterFamily) }
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = typography,
        shapes = RemitosShapes,
        content = content,
    )
}
