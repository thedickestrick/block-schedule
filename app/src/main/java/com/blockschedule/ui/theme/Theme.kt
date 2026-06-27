package com.blockschedule.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF3B5BDB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE3FF),
    onPrimaryContainer = Color(0xFF00164E),
    secondary = Color(0xFF5F3DC4),
    background = Color(0xFFF8F9FC),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFEEF1F6),
    onSurfaceVariant = Color(0xFF44474E),
    error = Color(0xFFC92A2A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFADC0FF),
    onPrimary = Color(0xFF002A77),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDCE3FF),
    secondary = Color(0xFFD0BCFF),
    background = Color(0xFF121316),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1C1D21),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF2A2C31),
    onSurfaceVariant = Color(0xFFC4C6CF),
    error = Color(0xFFFF9C9C)
)

// Slightly larger, high-contrast type for readability.
private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 18.sp),
    bodyMedium = TextStyle(fontSize = 16.sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun BlockScheduleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
