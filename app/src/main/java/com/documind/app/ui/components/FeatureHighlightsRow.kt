package com.documind.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.documind.app.ui.theme.DocumindDimens
import com.documind.app.ui.theme.OnDeviceBadge
import com.documind.app.ui.theme.Primary40
import com.documind.app.ui.theme.PrivacyGreen

private data class FeatureHighlight(
    val icon: ImageVector,
    val value: String,
    val label: String,
    val tint: Color
)

private val highlights: List<FeatureHighlight> = listOf(
    FeatureHighlight(
        icon = Icons.Default.Memory,
        value = "Gemma 1B",
        label = "On-Device AI",
        tint = OnDeviceBadge
    ),
    FeatureHighlight(
        icon = Icons.Default.CloudOff,
        value = "0",
        label = "Cloud Upload",
        tint = Primary40
    ),
    FeatureHighlight(
        icon = Icons.Default.Shield,
        value = "100%",
        label = "Private",
        tint = PrivacyGreen
    )
)

@Composable
fun FeatureHighlightsRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        highlights.forEach { highlight ->
            FeatureHighlightCard(
                highlight = highlight,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FeatureHighlightCard(
    highlight: FeatureHighlight,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = highlight.tint.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = highlight.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(18.dp),
                    tint = highlight.tint
                )
            }
            Text(
                text = highlight.value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = highlight.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
