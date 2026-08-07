package com.apptimemachine.ui.theme

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

private val SeedPrimary = Color(0xFF4A5FE8)
private val SeedSecondary = Color(0xFF5D6470)
private val SeedTertiary = Color(0xFF7D5296)

private val LightColors = lightColorScheme(
    primary = SeedPrimary,
    secondary = SeedSecondary,
    tertiary = SeedTertiary
)

private val DarkColors = darkColorScheme(
    primary = SeedPrimary,
    secondary = SeedSecondary,
    tertiary = SeedTertiary
)

/**
 * Part 1.4A Design Style: "Dynamic Color Support", Light/Dark/AMOLED.
 * AMOLED mode forces pure-black surfaces on top of the dark scheme for
 * OLED power savings, per Part 3.2 Appearance settings.
 */
@Composable
fun AppTimeMachineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    amoledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    var colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    if (darkTheme && amoledMode) {
        colorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color.Black
        )
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
        typography = AppTypography,
        content = content
    )
}
