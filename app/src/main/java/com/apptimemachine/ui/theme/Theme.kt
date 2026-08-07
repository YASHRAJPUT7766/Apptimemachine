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

// Brand palette: indigo primary (matches the launcher icon / reference
// design), warm neutral secondary, purple tertiary for variety in charts
// and category chips. Full schemes are hand-tuned below rather than left
// to lightColorScheme()/darkColorScheme() defaults, which only derive
// tonal surfaces from primary and tend to read flat/muddy — this is the
// "ajeeb" (odd) look on Home that needed fixing.
private val SeedPrimary = Color(0xFF4A5FE8)
private val SeedOnPrimary = Color(0xFFFFFFFF)
private val SeedPrimaryContainer = Color(0xFFE0E3FF)
private val SeedOnPrimaryContainer = Color(0xFF00105C)

private val SeedSecondary = Color(0xFF5B5D72)
private val SeedSecondaryContainer = Color(0xFFE0E1F9)
private val SeedOnSecondaryContainer = Color(0xFF181A2C)

private val SeedTertiary = Color(0xFF7D5296)
private val SeedTertiaryContainer = Color(0xFFF6D9FF)
private val SeedOnTertiaryContainer = Color(0xFF320046)

private val LightColors = lightColorScheme(
    primary = SeedPrimary,
    onPrimary = SeedOnPrimary,
    primaryContainer = SeedPrimaryContainer,
    onPrimaryContainer = SeedOnPrimaryContainer,
    secondary = SeedSecondary,
    secondaryContainer = SeedSecondaryContainer,
    onSecondaryContainer = SeedOnSecondaryContainer,
    tertiary = SeedTertiary,
    tertiaryContainer = SeedTertiaryContainer,
    onTertiaryContainer = SeedOnTertiaryContainer,
    background = Color(0xFFFBFAFF),
    surface = Color(0xFFFBFAFF),
    surfaceContainer = Color(0xFFF1F0FA),
    surfaceContainerHigh = Color(0xFFEBEAF6),
    surfaceContainerLow = Color(0xFFF6F5FC),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF767680),
    outlineVariant = Color(0xFFC7C5D0)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF10218F),
    primaryContainer = Color(0xFF2D3EA6),
    onPrimaryContainer = SeedPrimaryContainer,
    secondary = Color(0xFFC4C5DD),
    secondaryContainer = Color(0xFF434559),
    onSecondaryContainer = SeedSecondaryContainer,
    tertiary = Color(0xFFE9B6FF),
    tertiaryContainer = Color(0xFF633B7B),
    onTertiaryContainer = SeedTertiaryContainer,
    background = Color(0xFF131318),
    surface = Color(0xFF131318),
    surfaceContainer = Color(0xFF1F1F27),
    surfaceContainerHigh = Color(0xFF292A32),
    surfaceContainerLow = Color(0xFF1A1A21),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    outline = Color(0xFF90909A),
    outlineVariant = Color(0xFF46464F)
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
