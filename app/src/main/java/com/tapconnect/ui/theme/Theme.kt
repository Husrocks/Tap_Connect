package com.tapconnect.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AccentIndigo,
    background = AppBg,
    surface = CardBg,
    onPrimary = Color.White,
    onBackground = TextH,
    onSurface = TextH
)

@Composable
fun TapConnectTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
