package com.documind.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    iconSize: Dp = 40.dp,
    showShadow: Boolean = true
) {
    val bgColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showShadow) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        spotColor = bgColor.copy(alpha = 0.3f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(CircleShape)
            .background(color = bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = "DocuMind",
            modifier = Modifier.size(iconSize),
            tint = Color.White
        )
    }
}
