package com.documind.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// Light: cool slate — cards (#FFF) pop clearly against it. Dark: deep navy.
private val LightBackground = Color(0xFFEEF2F7)

@Composable
fun DocumindScreenBackground(
    modifier: Modifier = Modifier,
    showOrbs: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) SurfaceDark else LightBackground
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = background)
    ) {
        content()
    }
}
