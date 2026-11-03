package com.fabin.evoca.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val EvocaLightColors = lightColorScheme(

    primary = EvocaGreen,
    onPrimary = Color.White,

    primaryContainer = Color(0xFFDDE7DE),
    onPrimaryContainer = EvocaDeepGreen,

    secondary = EvocaDeepGreen,
    onSecondary = Color.White,

    secondaryContainer = Color(0xFFE5E7DF),
    onSecondaryContainer = EvocaInk,

    background = EvocaBackground,
    onBackground = EvocaInk,

    surface = EvocaSurface,
    onSurface = EvocaInk,

    surfaceVariant = Color(0xFFEAE6DC),
    onSurfaceVariant = EvocaMuted,

    outline = EvocaBorder,

    error = EvocaEmber,
    onError = Color.White
)

@Composable
fun EvocaTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = EvocaLightColors

    val view = LocalView.current

    if (!view.isInEditMode) {
        val activity = view.context as? Activity

        activity?.window?.let { window ->
            WindowCompat.getInsetsController(
                window,
                view
            ).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EvocaTypography,
        content = content
    )
}