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

// Brand palette: forest-green primary (matches the reference design's
// hero card, active badge, and accent color throughout), warm neutral
// secondary, a slightly lighter green tertiary so the hero gradient reads
// as green-to-green rather than green-to-purple. Full schemes are
// hand-tuned below rather than left to lightColorScheme()/darkColorScheme()
// defaults, which only derive tonal surfaces from primary and tend to
// read flat/muddy — this is the "ajeeb" (odd) look on Home that needed fixing.
private val SeedPrimary = Color(0xFF2E7D32)
private val SeedOnPrimary = Color(0xFFFFFFFF)
private val SeedPrimaryContainer = Color(0xFFD7F0D9)
private val SeedOnPrimaryContainer = Color(0xFF08350F)

private val SeedSecondary = Color(0xFF57624F)
private val SeedSecondaryContainer = Color(0xFFDAEBCE)
private val SeedOnSecondaryContainer = Color(0xFF141F0D)

private val SeedTertiary = Color(0xFF43A047)
private val SeedTertiaryContainer = Color(0xFFC8F0C4)
private val SeedOnTertiaryContainer = Color(0xFF042906)

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
    primary = Color(0xFF8BD68F),
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF0F5C1B),
    onPrimaryContainer = SeedPrimaryContainer,
    secondary = Color(0xFFBFCFB2),
    secondaryContainer = Color(0xFF3F4A38),
    onSecondaryContainer = SeedSecondaryContainer,
    tertiary = Color(0xFFA3D9A0),
    tertiaryContainer = Color(0xFF255828),
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
 * Fixed brand colors — deliberately NOT theme.colorScheme.primary/tertiary.
 * Those two swap to pale mint tones in dark mode (correct for M3 text/icon
 * roles, since a light color needs to sit on a dark surface) but look
 * washed-out and "ajeeb" when used as a big filled banner background —
 * the hero card, the persistent Scan Now bar, and any colored stat tile
 * with a fixed light pastel background all need a color that stays a rich
 * green in both light and dark mode instead of following the scheme.
 */
object BrandColors {
    val HeroGradientStart = Color(0xFF1B5E20)
    val HeroGradientEnd = Color(0xFF2E7D32)
    val ScanBar = Color(0xFF2E7D32)
    // Fixed-pastel stat tiles (Today's Summary) always sit on a light
    // background regardless of app theme, so their text needs a fixed
    // dark color too — MaterialTheme.colorScheme.onSurface would turn
    // near-white in dark mode and become unreadable on that light tile.
    val TileValueText = Color(0xFF1B2A1F)
    val TileLabelText = Color(0xFF54604F)
}

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
        // Only background/surface go pure black for OLED power savings.
        // surfaceContainer* must stay a step lighter than pure black or
        // every AtmCard blends into the page and the whole screen reads as
        // a flat, contrastless black slab (the "ajeeb" look on Home) —
        // cards need to visibly sit on top of the background, not merge
        // into it.
        colorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLow = Color(0xFF0D0D10),
            surfaceContainer = Color(0xFF17171C),
            surfaceContainerHigh = Color(0xFF1F1F25)
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
