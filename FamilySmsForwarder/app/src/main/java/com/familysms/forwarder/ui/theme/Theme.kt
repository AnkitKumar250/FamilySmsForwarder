package com.familysms.forwarder.ui.theme

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

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealOnPrimaryContainer,
    secondary = SlateSecondary,
    onSecondary = SlateOnSecondary,
    secondaryContainer = SlateSecondaryContainer,
    onSecondaryContainer = SlateOnSecondaryContainer,
    tertiary = AmberTertiary,
    tertiaryContainer = AmberTertiaryContainer,
    background = SurfaceLight,
    surface = SurfaceLight,
    surfaceContainerLow = Color.White,
    surfaceContainer = SurfaceContainerLight,
    outline = OutlineLight,
    error = ErrorRed,
    errorContainer = ErrorRedContainer
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AD0E3),
    onPrimary = Color(0xFF003642),
    primaryContainer = Color(0xFF004E5F),
    onPrimaryContainer = TealPrimaryContainer,
    secondary = Color(0xFFB1CBD4),
    onSecondary = Color(0xFF1C343B),
    secondaryContainer = Color(0xFF334A52),
    onSecondaryContainer = SlateSecondaryContainer,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A)
)

@Composable
fun FamilySmsForwarderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
