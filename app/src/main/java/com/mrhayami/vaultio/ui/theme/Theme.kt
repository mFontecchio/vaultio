package com.mrhayami.vaultio.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mrhayami.vaultio.data.ThemeBrand

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight
)

private fun energyScheme(seed: Color, isDark: Boolean): ColorScheme {
    val surfaceColor = if (isDark) Color(0xFF121212) else Color(0xFFFDFDFD)
    val onSurfaceColor = if (isDark) Color.White else Color.Black
    val onSeed = if (seed.luminance() > 0.55f) Color.Black else Color.White

    val primaryContainer = seed.copy(alpha = if (isDark) 0.3f else 0.15f).compositeOver(surfaceColor)
    val secondaryContainer = seed.copy(alpha = if (isDark) 0.2f else 0.1f).compositeOver(surfaceColor)

    return if (isDark) {
        darkColorScheme(
            primary = seed,
            onPrimary = onSeed,
            primaryContainer = primaryContainer,
            onPrimaryContainer = seed,
            secondary = seed,
            onSecondary = onSeed,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = seed,
            surface = surfaceColor,
            onSurface = onSurfaceColor,
            background = surfaceColor,
            onBackground = onSurfaceColor,
            surfaceVariant = seed.copy(alpha = 0.1f).compositeOver(surfaceColor),
            onSurfaceVariant = seed.copy(alpha = 0.7f).compositeOver(onSurfaceColor)
        )
    } else {
        lightColorScheme(
            primary = seed,
            onPrimary = onSeed,
            primaryContainer = primaryContainer,
            onPrimaryContainer = Color(
                red = seed.red * 0.4f,
                green = seed.green * 0.4f,
                blue = seed.blue * 0.4f
            ).compositeOver(Color.Black),
            secondary = seed,
            onSecondary = onSeed,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = Color(
                red = seed.red * 0.5f,
                green = seed.green * 0.5f,
                blue = seed.blue * 0.5f
            ).compositeOver(Color.Black),
            surface = surfaceColor,
            onSurface = onSurfaceColor,
            background = surfaceColor,
            onBackground = onSurfaceColor,
            surfaceVariant = seed.copy(alpha = 0.05f).compositeOver(surfaceColor),
            onSurfaceVariant = seed.copy(alpha = 0.8f).compositeOver(Color.Black)
        )
    }
}

@Composable
fun VaultioTheme(
    themeBrand: ThemeBrand = ThemeBrand.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeBrand) {
        ThemeBrand.DEFAULT -> {
            val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            if (useDynamic) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        ThemeBrand.GRASS -> energyScheme(EnergyGrass, darkTheme)
        ThemeBrand.FIRE -> energyScheme(EnergyFire, darkTheme)
        ThemeBrand.WATER -> energyScheme(EnergyWater, darkTheme)
        ThemeBrand.ELECTRIC -> energyScheme(EnergyLightning, darkTheme)
        ThemeBrand.PSYCHIC -> energyScheme(EnergyPsychic, darkTheme)
        ThemeBrand.FIGHTING -> energyScheme(EnergyFighting, darkTheme)
        ThemeBrand.DARKNESS -> energyScheme(EnergyDarkness, darkTheme)
        ThemeBrand.STEEL -> energyScheme(EnergyMetal, darkTheme)
        ThemeBrand.FAIRY -> energyScheme(EnergyFairy, darkTheme)
        ThemeBrand.DRAGON -> energyScheme(EnergyDragon, darkTheme)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
