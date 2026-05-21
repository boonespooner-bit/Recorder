package com.recorder.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Extended palette used by screens but not part of the Material color system
val PrimaryAmber = Primary
val PrimaryAmberDark = PrimaryVariant
val OnPrimaryDark = OnPrimary
val BackgroundDark = Background
val SurfaceDark = Surface
val SurfaceVariantDark = SurfaceVariant
val OnBackgroundDark = OnBackground
val OnSurfaceDark = OnSurface
val OnSurfaceVariantDark = Color(0xFFAAAAAA)
val ErrorRed = Error
val ChordGold = Color(0xFFFFD54F)
val LyricsGray = Color(0xFFBBBBBB)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = Color(0xFF4A3800),
    onPrimaryContainer = Color(0xFFFFDEA0),
    secondary = Secondary,
    onSecondary = Color(0xFF000000),
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = Color(0xFFAAAAAA),
    error = Error,
    onError = Color(0xFF000000),
    outline = Color(0xFF555555),
)

@Composable
fun RecorderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = RecorderTypography,
        content = content
    )
}
