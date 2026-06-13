package com.documind.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.unit.dp
import com.documind.app.ui.theme.DocumindDimens
import com.documind.app.ui.theme.OnDeviceBadge
import com.documind.app.ui.theme.PrivacyGreen
import com.documind.app.ui.theme.TrustBadgeBackground
import com.documind.app.ui.theme.TrustBadgeBorder

@Composable
fun TrustBadgesRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TrustBadge(
            icon = Icons.Default.Psychology,
            label = "On-Device AI",
            tint = OnDeviceBadge
        )
        TrustBadge(
            icon = Icons.Default.CloudOff,
            label = "No Cloud Upload",
            tint = MaterialTheme.colorScheme.primary
        )
        TrustBadge(
            icon = Icons.Default.Security,
            label = "Works Offline",
            tint = PrivacyGreen
        )
    }
}

@Composable
private fun TrustBadge(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(DocumindDimens.ChipRadius),
        color = TrustBadgeBackground,
        border = BorderStroke(1.dp, TrustBadgeBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = tint
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
