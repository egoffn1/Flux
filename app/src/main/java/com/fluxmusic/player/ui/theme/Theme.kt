package com.fluxmusic.player.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

object FluxColors {
    // Google Green (primary)
    val GoogleGreen = Color(0xFF1DB954)
    val GoogleGreenLight = Color(0xFF1ED760)
    val GoogleGreenDark = Color(0xFF14833B)
    
    // Google Music accent colors
    val AccentRed = Color(0xFFEA4335)
    val AccentBlue = Color(0xFF4285F4)
    val AccentYellow = Color(0xFFFBBC05)
    
    // Dark theme surfaces
    val DarkSurface = Color(0xFF121212)
    val DarkSurfaceVariant = Color(0xFF1E1E1E)
    val DarkBackground = Color(0xFF000000)
    
    // Light theme surfaces
    val LightSurface = Color(0xFFFAFAFA)
    val LightSurfaceVariant = Color(0xFFF5F5F5)
    val LightBackground = Color(0xFFFFFFFF)
}

private val DarkColorScheme = darkColorScheme(
    primary = FluxColors.GoogleGreen,
    onPrimary = Color.Black,
    primaryContainer = FluxColors.GoogleGreenDark,
    onPrimaryContainer = Color.White,
    secondary = FluxColors.AccentBlue,
    onSecondary = Color.White,
    tertiary = FluxColors.AccentRed,
    onTertiary = Color.White,
    background = FluxColors.DarkBackground,
    onBackground = Color.White,
    surface = FluxColors.DarkSurface,
    onSurface = Color.White,
    surfaceVariant = FluxColors.DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB3B3B3),
    outline = Color(0xFF404040),
    outlineVariant = Color(0xFF2A2A2A)
)

private val LightColorScheme = lightColorScheme(
    primary = FluxColors.GoogleGreen,
    onPrimary = Color.White,
    primaryContainer = FluxColors.GoogleGreenLight,
    onPrimaryContainer = Color.Black,
    secondary = FluxColors.AccentBlue,
    onSecondary = Color.White,
    tertiary = FluxColors.AccentRed,
    onTertiary = Color.White,
    background = FluxColors.LightBackground,
    onBackground = Color.Black,
    surface = FluxColors.LightSurface,
    onSurface = Color.Black,
    surfaceVariant = FluxColors.LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF666666),
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFEEEEEE)
)

@Composable
fun FluxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}