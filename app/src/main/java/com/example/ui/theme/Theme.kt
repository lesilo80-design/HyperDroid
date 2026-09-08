package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ProxmoxOrange,
    onPrimary = Slate950,
    primaryContainer = ProxmoxOrangeDark,
    onPrimaryContainer = PureWhite,
    secondary = CyberCyan,
    onSecondary = Slate950,
    secondaryContainer = CyberCyanDim,
    onSecondaryContainer = PureWhite,
    tertiary = StatusGreen,
    onTertiary = Slate950,
    background = Slate950,
    onBackground = Slate200,
    surface = Slate900,
    onSurface = Slate200,
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    outlineVariant = Slate800,
    error = StatusRed,
    onError = PureWhite,
    errorContainer = StatusRedBg,
    onErrorContainer = PureWhite
)

private val LightColorScheme = darkColorScheme(
    // Proxmox server UI is predominantly dark slate console; keeping consistent sleek dark palette
    primary = ProxmoxOrange,
    onPrimary = Slate950,
    primaryContainer = ProxmoxOrangeDark,
    onPrimaryContainer = PureWhite,
    secondary = CyberCyan,
    onSecondary = Slate950,
    secondaryContainer = CyberCyanDim,
    onSecondaryContainer = PureWhite,
    tertiary = StatusGreen,
    onTertiary = Slate950,
    background = Slate950,
    onBackground = Slate200,
    surface = Slate900,
    onSurface = Slate200,
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    outlineVariant = Slate800,
    error = StatusRed,
    onError = PureWhite,
    errorContainer = StatusRedBg,
    onErrorContainer = PureWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve high-tech server console look
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
