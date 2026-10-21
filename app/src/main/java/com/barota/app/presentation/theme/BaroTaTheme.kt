package com.barota.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = lightColorScheme(
    primary = Color(0xFF1466E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5EEFF),
    onPrimaryContainer = Color(0xFF073E91),
    background = Color(0xFFF6F8FC),
    surface = Color.White,
    onSurface = Color(0xFF17243B),
    onSurfaceVariant = Color(0xFF586679),
    outline = Color(0xFFD5DDEA)
)

@Composable
fun BaroTaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
