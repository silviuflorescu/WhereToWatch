package com.coroutine.mastery.wheretowatch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PopcornColorScheme = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    primaryContainer = CoralLight,
    onPrimaryContainer = CoralDark,
    secondary = Amber,
    onSecondary = Cocoa,
    secondaryContainer = CoralLight,
    onSecondaryContainer = CoralDark,
    background = Cream,
    onBackground = Cocoa,
    surface = Cream,
    onSurface = Cocoa,
    surfaceVariant = CreamDark,
    onSurfaceVariant = CocoaLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Cream,
    surfaceContainerHigh = CreamDark,
    surfaceContainerHighest = CreamDark,
    outline = SandDark,
    outlineVariant = Sand,
)

@Composable
fun WhereToWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PopcornColorScheme,
        typography = Typography,
        content = content,
    )
}
