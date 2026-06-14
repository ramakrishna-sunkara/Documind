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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.documind.app.ui.theme.AiReadyGreen
import com.documind.app.ui.theme.DocumindGradients
import com.documind.app.ui.theme.GradientEnd
import com.documind.app.ui.theme.GradientMiddle
import com.documind.app.ui.theme.GradientStart

@Composable
fun HomeHeroSection(
    isLlmReady: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_orb")
    val orbOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbFloat"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(brush = DocumindGradients.brand())
    ) {
        // Decorative blurred orbs inside the card
        Box(
            modifier = Modifier
                .size(140.dp)
                .offset(x = 220.dp, y = (-20).dp)
                .blur(50.dp)
                .background(
                    color = GradientEnd.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(100.dp)
                .offset(x = (-10).dp, y = (orbOffset + 60).dp)
                .blur(40.dp)
                .background(
                    color = GradientMiddle.copy(alpha = 0.4f),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            // Top row: logo + AI status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "DocuMind",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = Color.White
                    )
                }
                if (isLlmReady) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(AiReadyGreen, CircleShape)
                            )
                            Text(
                                text = "AI Ready",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Your Private\nDocument AI",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Chat with PDFs, Word docs & web pages — processed entirely on your device",
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 18.sp
                ),
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Privacy badges row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroBadge(icon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(11.dp), tint = Color.White.copy(alpha = 0.9f)) }, label = "100% Private")
                HeroBadge(icon = { Icon(Icons.Default.CloudOff, null, modifier = Modifier.size(11.dp), tint = Color.White.copy(alpha = 0.9f)) }, label = "No Cloud")
                HeroBadge(icon = { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(11.dp), tint = Color.White.copy(alpha = 0.9f)) }, label = "Offline")
            }
        }
    }
}

@Composable
private fun HeroBadge(
    icon: @Composable () -> Unit,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                ),
                color = Color.White.copy(alpha = 0.95f)
            )
        }
    }
}
