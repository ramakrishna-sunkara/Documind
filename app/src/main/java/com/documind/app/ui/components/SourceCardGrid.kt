package com.documind.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class SourceCardItem(
    val icon: ImageVector,
    val label: String,
    val subtitle: String,
    val accentColor: androidx.compose.ui.graphics.Color,
    val onClick: () -> Unit
)

@Composable
fun SourceCardGrid(
    topRow: List<SourceCardItem>,
    bottomRow: List<SourceCardItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SourceCardRow(items = topRow)
        SourceCardRow(items = bottomRow)
    }
}

@Composable
private fun SourceCardRow(
    items: List<SourceCardItem>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            SourceCard(
                icon = item.icon,
                label = item.label,
                subtitle = item.subtitle,
                onClick = item.onClick,
                accentColor = item.accentColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
