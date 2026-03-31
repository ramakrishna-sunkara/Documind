package com.documind.prescription.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.documind.prescription.ui.theme.ErrorRed

@Composable
fun VoiceInputButton(
    isListening: Boolean,
    isAvailable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isListening -> ErrorRed
            isAvailable -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "bgColor"
    )
    
    val iconColor by animateColorAsState(
        targetValue = when {
            isListening -> Color.White
            isAvailable -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        },
        label = "iconColor"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(ErrorRed.copy(alpha = 0.3f))
            )
        }
        
        IconButton(
            onClick = onClick,
            enabled = isAvailable,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = iconColor
            )
        ) {
            Icon(
                imageVector = if (isAvailable) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = if (isListening) "Stop listening" else "Start voice input"
            )
        }
    }
}
