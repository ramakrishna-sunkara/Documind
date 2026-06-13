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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.documind.app.data.analytics.CrashAnalytics
import com.documind.app.data.llm.ModelState
import com.documind.app.domain.model.ExtractionState
import com.documind.app.ui.components.AiStatusPill
import com.documind.app.ui.components.ErrorDialog
import com.documind.app.ui.components.SourceCard
import com.documind.app.ui.components.SourceCardColors
import com.documind.app.ui.components.TryDemoCard
import com.documind.app.ui.components.TrustBadgesRow
import com.documind.app.ui.theme.DocumindDimens
import com.documind.app.ui.theme.DocumindGradients
import com.documind.app.ui.theme.PrivacyNoteBackground
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
    onLoadDemoDocument: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showUrlDialog by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }
    var textInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                if (inputStream != null) {
                    onExtractPdf(inputStream, "Document.pdf")
                } else {
                    Toast.makeText(
                        context,
                        "Could not open the PDF file. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
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
                } else {
                    Toast.makeText(
                        context,
                        "Could not open the Word file. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
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
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLlmReady) {
                    AiStatusPill()
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            TrustBadgesRow(modifier = Modifier.fillMaxWidth())
            if (!isLlmReady) {
                Spacer(modifier = Modifier.height(12.dp))
                ModelStatusBanner(
                    modelState = modelState,
                    isLlmReady = isLlmReady,
                    onDownloadClick = onStartModelDownload,
                    onCellularClick = onRequestCellularDownload
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(brush = DocumindGradients.brand()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "D",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp
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
                text = "Chat with your documents — privately, on your phone",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Zero cloud upload. Powered by on-device AI.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (extractionState !is ExtractionState.Extracting) {
                TryDemoCard(onClick = onLoadDemoDocument)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Or choose a source",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (extractionState is ExtractionState.Extracting) {
                Surface(
                    shape = RoundedCornerShape(DocumindDimens.CardRadius),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
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
                        subtitle = "Contracts, reports",
                        onClick = { pdfLauncher.launch(arrayOf("application/pdf")) },
                        accentColor = SourceCardColors.PDF
                    )
                    SourceCard(
                        icon = Icons.Default.Description,
                        label = "Word",
                        subtitle = "DOCX files",
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
                        subtitle = "Web articles",
                        onClick = { showUrlDialog = true },
                        accentColor = SourceCardColors.URL
                    )
                    SourceCard(
                        icon = Icons.Default.ContentPaste,
                        label = "Text",
                        subtitle = "Paste content",
                        onClick = { showTextDialog = true },
                        accentColor = SourceCardColors.Text
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
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
                        if (urlInput.isBlank()) {
                            Toast.makeText(context, "Please enter a website URL.", Toast.LENGTH_SHORT).show()
                        } else {
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
                        if (textInput.isBlank()) {
                            Toast.makeText(context, "Please paste some text first.", Toast.LENGTH_SHORT).show()
                        } else {
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
        ErrorDialog(
            title = "Could Not Open Document",
            message = extractionState.message,
            onDismiss = onDismissError
        )
    }
    
}

@Composable
private fun PrivacyNote() {
    Surface(
        shape = RoundedCornerShape(DocumindDimens.ChipRadius),
        color = PrivacyNoteBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your Privacy, Protected",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "All AI processing happens on your device. Documents never leave your phone — ideal for legal, financial, and confidential content.",
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
    LaunchedEffect(modelState) {
        if (modelState is ModelState.Downloading && modelState.progress >= 99) {
            CrashAnalytics.logModelDownloadPhase(
                phase = "home_ui_downloading_99",
                status = "transferring",
                progress = modelState.progress
            )
        }
    }
    
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
                            text = "Downloading AI Models...",
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
                        text = "Tap to download AI models (~670MB)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
