package com.dabb.business.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = DeepTeal,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = WarmAmber,
    onSecondary = androidx.compose.ui.graphics.Color.Black,
    background = SoftCream,
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFEAE7E2),
    error = androidx.compose.ui.graphics.Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = DeepTeal,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = WarmAmber,
    background = Charcoal,
    surface = Charcoal,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF3D3D3D)
)

@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
