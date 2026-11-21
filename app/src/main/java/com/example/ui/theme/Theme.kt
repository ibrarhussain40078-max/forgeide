package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val ForgeDarkColorScheme = darkColorScheme(
    primary = CyanElectric,
    onPrimary = ObsidianVoid,
    primaryContainer = Color(0xFF0C3554),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = IndigoGlow,
    onSecondary = ObsidianVoid,
    secondaryContainer = IndigoSurface,
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = EmeraldPass,
    onTertiary = ObsidianVoid,
    tertiaryContainer = EmeraldSurface,
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianPanel,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianVoid,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianPanel,
    surfaceContainerHigh = ObsidianElevated,
    outline = ObsidianBorder,
    outlineVariant = Color(0xFF1E293B),
    error = RoseError,
    onError = ObsidianVoid,
    errorContainer = RoseSurface,
    onErrorContainer = Color(0xFFFECDD3)
)

private val ForgeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun ForgeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ForgeDarkColorScheme,
        typography = ForgeTypography,
        shapes = ForgeShapes,
        content = content
    )
}
