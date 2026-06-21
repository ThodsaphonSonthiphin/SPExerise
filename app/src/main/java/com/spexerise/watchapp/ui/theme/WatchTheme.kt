package com.spexerise.watchapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

private val WatchColors = Colors(
    primary = Color(0xFF5FB4FF),
    surface = Color.Black,
    onSurface = Color.White
)

@Composable
fun WatchAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = WatchColors, content = content)
}
