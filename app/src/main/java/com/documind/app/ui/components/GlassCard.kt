package com.documind.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.documind.app.ui.theme.DocumindDimens

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 2.dp,
    content: @Composable () -> Unit
) {
    val isDark: Boolean = isSystemInDarkTheme()
    val containerColor: Color = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)
    }
    val borderColor: Color = if (isDark) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    } else {
        Color.White.copy(alpha = 0.7f)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(DocumindDimens.CardRadius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = elevation,
        tonalElevation = 1.dp
    ) {
        Box(content = { content() })
    }
}
