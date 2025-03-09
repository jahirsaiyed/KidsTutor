package com.example.kidstutor.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4CAF50),        // Bright Green
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8E6B9),
    onPrimaryContainer = Color(0xFF002105),
    
    secondary = Color(0xFFFF9800),      // Orange
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = Color(0xFF2E1500),
    
    tertiary = Color(0xFF2196F3),       // Blue
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBBDEFB),
    onTertiaryContainer = Color(0xFF001E36),
    
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    
    error = Color(0xFFE91E63),          // Pink
    onError = Color.White,
    errorContainer = Color(0xFFF8BBD0),
    onErrorContainer = Color(0xFF410012)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),        // Light Green
    onPrimary = Color(0xFF003910),
    primaryContainer = Color(0xFF005317),
    onPrimaryContainer = Color(0xFFB8E6B9),
    
    secondary = Color(0xFFFFB74D),      // Light Orange
    onSecondary = Color(0xFF4A2700),
    secondaryContainer = Color(0xFF693C00),
    onSecondaryContainer = Color(0xFFFFE0B2),
    
    tertiary = Color(0xFF64B5F6),       // Light Blue
    onTertiary = Color(0xFF003354),
    tertiaryContainer = Color(0xFF004A77),
    onTertiaryContainer = Color(0xFFBBDEFB),
    
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    
    error = Color(0xFFF06292),          // Light Pink
    onError = Color(0xFF680020),
    errorContainer = Color(0xFF93002F),
    onErrorContainer = Color(0xFFF8BBD0)
)

@Composable
fun KidsTutorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}