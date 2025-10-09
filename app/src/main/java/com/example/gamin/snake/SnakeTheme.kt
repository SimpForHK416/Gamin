package com.example.gamin.snake


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 🎨 Màu sáng
private val SnakeLightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),        // Xanh lá đậm
    onPrimary = Color.White,
    secondary = Color(0xFF81C784),      // Xanh lá nhạt
    onSecondary = Color.Black,
    background = Color(0xFFF1F8E9),     // Nền sáng xanh lá nhạt
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

// 🌑 Màu tối
private val SnakeDarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color.Black,
    secondary = Color(0xFF388E3C),
    onSecondary = Color.White,
    background = Color(0xFF1B1B1B),
    onBackground = Color.White,
    surface = Color(0xFF2C2C2C),
    onSurface = Color.White
)

@Composable
fun SnakeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) SnakeDarkColors else SnakeLightColors

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
