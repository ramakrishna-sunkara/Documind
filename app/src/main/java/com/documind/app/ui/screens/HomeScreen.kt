package com.documind.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.documind.app.data.llm.ModelState
import com.documind.app.domain.model.ExtractionState
import com.documind.app.ui.components.SourceCard
import com.documind.app.ui.components.SourceCardColors
import com.documind.app.ui.components.SupportBottomSheet
import com.documind.app.ui.components.SupportButton
import com.documind.app.ui.theme.GradientEnd
import com.documind.app.ui.theme.GradientMiddle
import com.documind.app.ui.theme.GradientStart
import com.documind.app.ui.theme.OfflineBadge
import com.documind.app.ui.theme.SuccessGreen
import com.documind.app.ui.theme.WarningAmber

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    extractionState: ExtractionState,
    modelState: ModelState,
    isLlmReady: Boolean,
    onExtractPdf: (java.io.InputStream, String) -> Unit,
    onExtractDocx: (java.io.InputStream, String) -> Unit,
    onExtractUrl: (String) -> Unit,
    onExtractText: (String) -> Unit,
    onDismissError: () -> Unit,
    onStartModelDownload: () -> Unit = {},
    onRequestCellularDownload: () -> Unit = {},
    onPrescriptionDemoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showUrlDialog by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showSupportSheet by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }
    var textInput by remember { mutableStateOf("") }
    
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                if (inputStream != null) {
                    onExtractPdf(inputStream, "Document.pdf")
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to open PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    val docxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                if (inputStream != null) {
                    onExtractDocx(inputStream, "Document.docx")
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to open Word document", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header with Offline badge and action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OfflineBadge()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrescriptionDemoButton(onClick = onPrescriptionDemoClick)
                    SupportButton(onClick = { showSupportSheet = true })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // AI Model Status Banner
            ModelStatusBanner(
                modelState = modelState,
                isLlmReady = isLlmReady,
                onDownloadClick = onStartModelDownload,
                onCellularClick = onRequestCellularDownload
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientStart, GradientMiddle, GradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "D",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    ),
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "DocuMind",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Select a document source to begin",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (extractionState is ExtractionState.Extracting) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = GradientMiddle,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Extracting content...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SourceCard(
                        icon = Icons.Default.PictureAsPdf,
                        label = "PDF",
                        onClick = { pdfLauncher.launch(arrayOf("application/pdf")) },
                        accentColor = SourceCardColors.PDF
                    )
                    SourceCard(
                        icon = Icons.Default.Description,
                        label = "Word",
                        onClick = {
                            docxLauncher.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                )
                            )
                        },
                        accentColor = SourceCardColors.Word
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SourceCard(
                        icon = Icons.Default.Link,
                        label = "URL",
                        onClick = { showUrlDialog = true },
                        accentColor = SourceCardColors.URL
                    )
                    SourceCard(
                        icon = Icons.Default.ContentPaste,
                        label = "Text",
                        onClick = { showTextDialog = true },
                        accentColor = SourceCardColors.Text
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            PrivacyNote()
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Enter URL",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            },
            text = {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Website URL") },
                    placeholder = { Text("https://example.com/article") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (urlInput.isNotBlank()) {
                            val url = if (!urlInput.startsWith("http")) {
                                "https://$urlInput"
                            } else {
                                urlInput
                            }
                            onExtractUrl(url)
                            showUrlDialog = false
                            urlInput = ""
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Extract", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Paste Text",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Document text") },
                    placeholder = { Text("Paste your text here...") },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onExtractText(textInput)
                            showTextDialog = false
                            textInput = ""
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Load", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (extractionState is ExtractionState.Error) {
        AlertDialog(
            onDismissRequest = onDismissError,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Extraction Error",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = { Text(extractionState.message) },
            confirmButton = {
                Button(
                    onClick = onDismissError,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("OK", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
    
    // Support/Donate bottom sheet
    if (showSupportSheet) {
        SupportBottomSheet(
            onDismiss = { showSupportSheet = false }
        )
    }
}

@Composable
private fun OfflineBadge() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = OfflineBadge.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(OfflineBadge, CircleShape)
            )
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = OfflineBadge
            )
            Text(
                text = "Works 100% Offline",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = OfflineBadge
            )
        }
    }
}

@Composable
private fun PrescriptionDemoButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF10B981).copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalPharmacy,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color(0xFF10B981)
            )
            Text(
                text = "Rx Demo",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF10B981)
            )
        }
    }
}

@Composable
private fun PrivacyNote() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your Privacy, Protected",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "All processing happens on your device. Your documents never leave your phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ModelStatusBanner(
    modelState: ModelState,
    isLlmReady: Boolean,
    onDownloadClick: () -> Unit,
    onCellularClick: () -> Unit
) {
    val isError = modelState is ModelState.Error
    val errorMessage = if (isError) (modelState as ModelState.Error).message else ""
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            isLlmReady -> SuccessGreen.copy(alpha = 0.12f)
            modelState is ModelState.Downloading -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            modelState is ModelState.WaitingForWifi -> WarningAmber.copy(alpha = 0.12f)
            isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        when {
            // AI Ready
            isLlmReady -> {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = SuccessGreen
                    )
                    Text(
                        text = "AI Ready",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = SuccessGreen
                    )
                }
            }
            
            // Downloading
            modelState is ModelState.Downloading -> {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Downloading AI Model...",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${modelState.progress}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { modelState.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
            
            // Waiting for WiFi
            modelState is ModelState.WaitingForWifi -> {
                Row(
                    modifier = Modifier
                        .clickable { onCellularClick() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = WarningAmber
                    )
                    Text(
                        text = "Waiting for WiFi - Tap to use mobile data",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = WarningAmber
                    )
                }
            }
            
            // Error - tap to retry
            isError -> {
                Row(
                    modifier = Modifier
                        .clickable { onDownloadClick() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Tap to retry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Idle - need to download
            else -> {
                Row(
                    modifier = Modifier
                        .clickable { onDownloadClick() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tap to download AI Model (~500MB)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
