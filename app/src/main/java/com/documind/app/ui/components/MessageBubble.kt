package com.documind.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.documind.app.domain.model.ChatMessage
import com.documind.app.ui.theme.DocumindDimens
import com.documind.app.ui.theme.DocumindGradients
import com.documind.app.ui.theme.GradientMiddle
import com.documind.app.ui.theme.ErrorRed
import com.documind.app.ui.theme.OnDeviceBadge

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser: Boolean = message.isUser
    val isErrorMessage: Boolean = !isUser && message.isError
    val isInfoMessage: Boolean = !isUser && message.isInfo
    val bubbleRadius = DocumindDimens.BubbleRadius
    val shape = if (isUser) {
        RoundedCornerShape(bubbleRadius, bubbleRadius, 4.dp, bubbleRadius)
    } else {
        RoundedCornerShape(bubbleRadius, bubbleRadius, bubbleRadius, 4.dp)
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Text(
                text = "DocuMind",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
            )
        }
        Box(modifier = Modifier.widthIn(max = 320.dp)) {
            if (isUser) {
                Surface(
                    shape = shape,
                    modifier = Modifier
                        .clip(shape)
                        .background(brush = DocumindGradients.userMessage())
                ) {
                    Box(
                        modifier = Modifier
                            .background(brush = DocumindGradients.userMessage())
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = message.content,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Surface(
                    shape = shape,
                    color = when {
                        isErrorMessage -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f)
                        isInfoMessage -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        else -> MaterialTheme.colorScheme.surfaceContainer
                    },
                    shadowElevation = if (isErrorMessage || isInfoMessage) 0.dp else 2.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        if (message.isLoading) {
                            Text(
                                text = "Thinking on your device...",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LoadingDots()
                        } else {
                            if (isErrorMessage || isInfoMessage) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isErrorMessage) {
                                            Icons.Default.ErrorOutline
                                        } else {
                                            Icons.Default.Info
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isErrorMessage) ErrorRed else MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (isErrorMessage) "Something went wrong" else "Please wait",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = if (isErrorMessage) {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        } else {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                text = message.content,
                                color = when {
                                    isErrorMessage -> MaterialTheme.colorScheme.onErrorContainer
                                    isInfoMessage -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (!isErrorMessage && !isInfoMessage) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OnDeviceFooter()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnDeviceFooter() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Memory,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = OnDeviceBadge
        )
        Text(
            text = "Processed on device",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun LoadingDots(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, easing = LinearEasing, delayMillis = index * 150),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .background(color = GradientMiddle, shape = CircleShape)
            )
        }
    }
}
