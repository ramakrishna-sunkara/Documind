package com.documind.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun DocumindScreenBackground(
    modifier: Modifier = Modifier,
    showOrbs: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDark: Boolean = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = DocumindGradients.screenBackground())
    ) {
        if (showOrbs) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .offset(x = (-80).dp, y = (-40).dp)
                    .blur(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                OrbBlue.copy(alpha = if (isDark) 0.18f else 0.12f),
                                OrbBlue.copy(alpha = 0f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .offset(x = 240.dp, y = 120.dp)
                    .blur(70.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                OrbPurple.copy(alpha = if (isDark) 0.15f else 0.10f),
                                OrbPurple.copy(alpha = 0f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .offset(x = 40.dp, y = 500.dp)
                    .blur(60.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                OrbGreen.copy(alpha = if (isDark) 0.12f else 0.08f),
                                OrbGreen.copy(alpha = 0f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }
        content()
    }
}
