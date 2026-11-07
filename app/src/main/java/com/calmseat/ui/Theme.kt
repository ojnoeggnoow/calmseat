package com.calmseat.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 어두운 바탕 + 시안 포인트
object CalmColors {
    val Background = Color(0xFF0B0F14)
    val Surface = Color(0xFF151B23)
    val SurfaceHigh = Color(0xFF1E2630)
    val Accent = Color(0xFF5FD3E0)
    val Warn = Color(0xFFFFB454)
    val Good = Color(0xFF7BD88F)
    val Text = Color(0xFFF2F5F7)
    val TextDim = Color(0xFF9AA5B1)
}

private val Scheme = darkColorScheme(
    primary = CalmColors.Accent,
    onPrimary = CalmColors.Background,
    background = CalmColors.Background,
    onBackground = CalmColors.Text,
    surface = CalmColors.Surface,
    onSurface = CalmColors.Text,
    surfaceVariant = CalmColors.SurfaceHigh,
    onSurfaceVariant = CalmColors.TextDim,
)

@Composable
fun CalmSeatTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
