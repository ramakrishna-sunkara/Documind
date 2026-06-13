package com.documind.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object DocumindGradients {
    val brandColors: List<Color> = listOf(GradientStart, GradientMiddle, GradientEnd)

    @Composable
    fun brand(): Brush {
        return Brush.linearGradient(colors = brandColors)
    }

    @Composable
    fun brandHorizontal(): Brush {
        return Brush.horizontalGradient(colors = brandColors)
    }

    @Composable
    fun screenBackground(): Brush {
        val isDark = isSystemInDarkTheme()
        return if (isDark) {
            Brush.verticalGradient(
                colors = listOf(BackgroundGradientTopDark, BackgroundGradientBottomDark)
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(BackgroundGradientTop, BackgroundGradientBottom)
            )
        }
    }

    @Composable
    fun userMessage(): Brush {
        return Brush.linearGradient(
            colors = listOf(GradientStart, GradientMiddle)
        )
    }
}
