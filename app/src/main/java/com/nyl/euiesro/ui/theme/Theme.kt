package com.nyl.euiesro.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DA3FF),
    secondary = Color(0xFF7DB9FF),
    tertiary = Color(0xFF28C4D9),
    background = Color(0xFF071A2B),
    surface = Color(0xFF0D1F2E),
    surfaceVariant = Color(0xFF1B2D3E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFEAF4FF),
    onSurface = Color(0xFFEAF4FF),
    primaryContainer = Color(0xFF17365A),
    onPrimaryContainer = Color(0xFFEAF4FF)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFE4584B),
    secondary = Color(0xFFF28B76),
    tertiary = Color(0xFFE8C9C1),
    background = Color(0xFFF2F3F5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF8F2F0),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1F2937),
    onSurface = Color(0xFF1F2937),
    primaryContainer = Color(0xFFFDF1EE),
    onPrimaryContainer = Color(0xFF1F2937),
    secondaryContainer = Color(0xFFFDF1EE),
    onSecondaryContainer = Color(0xFF1F2937),
    error = Color(0xFFE4584B),
    onError = Color.White
)

@Composable
fun EuIesroTheme(
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}