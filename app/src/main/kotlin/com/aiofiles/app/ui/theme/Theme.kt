package com.aiofiles.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * App theme that uses Material You dynamic colors on Android 12+
 * and falls back to static color schemes on older versions.
 *
 * @param darkTheme Whether to use dark theme (defaults to system setting)
 * @param dynamicColor Whether to use dynamic colors (default true on Android 12+)
 * @param content The app content
 */
@Composable
fun AioFilesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

/**
 * Light fallback color scheme.
 * Inspired by a green/teal "file viewer" aesthetic.
 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF386A20),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB8F397),
    onPrimaryContainer = Color(0xFF072100),
    secondary = Color(0xFF546249),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E7CC),
    onSecondaryContainer = Color(0xFF121F0D),
    tertiary = Color(0xFF386A20),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB8F397),
    onTertiaryContainer = Color(0xFF072100),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surface = Color(0xFFFDFDF5),
    onSurface = Color(0xFF1A1C18),
    surfaceContainer = Color(0xFFF0F4EB),
    surfaceContainerHigh = Color(0xFFEAEFE5),
    surfaceContainerHighest = Color(0xFFE4E9E0),
)

/**
 * Dark fallback color scheme.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9DD382),
    onPrimary = Color(0xFF0B3900),
    primaryContainer = Color(0xFF1F510A),
    onPrimaryContainer = Color(0xFFB8F397),
    secondary = Color(0xFFBCC9AC),
    onSecondary = Color(0xFF27341F),
    secondaryContainer = Color(0xFF3D4B35),
    onSecondaryContainer = Color(0xFFD7E7CC),
    tertiary = Color(0xFF9DD382),
    onTertiary = Color(0xFF0B3900),
    tertiaryContainer = Color(0xFF1F510A),
    onTertiaryContainer = Color(0xFFB8F397),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surface = Color(0xFF1A1C18),
    onSurface = Color(0xFFE4E9E0),
    surfaceContainer = Color(0xFF1E211B),
    surfaceContainerHigh = Color(0xFF292C26),
    surfaceContainerHighest = Color(0xFF333730),
)
