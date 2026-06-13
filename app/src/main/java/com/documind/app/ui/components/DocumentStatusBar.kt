package com.documind.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.documind.app.data.extractor.SourceType
import com.documind.app.domain.model.DocumentContent
import com.documind.app.ui.theme.CardPdf
import com.documind.app.ui.theme.CardText
import com.documind.app.ui.theme.CardUrl
import com.documind.app.ui.theme.CardWord
import com.documind.app.ui.theme.DocumindDimens
import com.documind.app.ui.theme.OnDeviceBadge
import com.documind.app.ui.theme.WarningAmber
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DocumentStatusBar(
    document: DocumentContent,
    modifier: Modifier = Modifier
) {
    val wordCountFormatted: String = NumberFormat.getNumberInstance(Locale.getDefault())
        .format(document.wordCount)
    val sourceIcon = when (document.sourceType) {
        SourceType.PDF -> Icons.Default.PictureAsPdf
        SourceType.DOCX -> Icons.Default.Description
        SourceType.URL -> Icons.Default.Link
        SourceType.TEXT -> Icons.AutoMirrored.Filled.TextSnippet
    }
    val sourceColor = when (document.sourceType) {
        SourceType.PDF -> CardPdf
        SourceType.DOCX -> CardWord
        SourceType.URL -> CardUrl
        SourceType.TEXT -> CardText
    }
    val sourceLabel = when (document.sourceType) {
        SourceType.PDF -> "PDF"
        SourceType.DOCX -> "Word"
        SourceType.URL -> "Web"
        SourceType.TEXT -> "Text"
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = sourceColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = sourceIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = sourceColor
                        )
                        Text(
                            text = sourceLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = sourceColor
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.sourceName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$wordCountFormatted words",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (document.isLargeDocument) {
                Surface(
                    shape = RoundedCornerShape(DocumindDimens.ChipRadius),
                    color = WarningAmber.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = WarningAmber
                        )
                        Text(
                            text = "Large",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = WarningAmber
                        )
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = OnDeviceBadge
                    )
                    Text(
                        text = "Private",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = OnDeviceBadge
                    )
                }
            }
        }
    }
}
