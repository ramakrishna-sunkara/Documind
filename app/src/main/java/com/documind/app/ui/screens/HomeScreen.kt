package com.documind.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.documind.app.data.analytics.CrashAnalytics
import com.documind.app.data.llm.ModelState
import com.documind.app.domain.model.ExtractionState
import com.documind.app.ui.components.BrandLogo
import com.documind.app.ui.components.DocumindLoadingCard
import com.documind.app.ui.components.ErrorDialog
import com.documind.app.ui.components.InputBottomSheet
import com.documind.app.ui.components.SourceCardColors
import com.documind.app.ui.components.SourceCardItem
import com.documind.app.ui.components.SourceListSection
import com.documind.app.ui.theme.AiReadyGreen
import com.documind.app.ui.theme.DocumindDimens
import com.documind.app.ui.theme.SuccessGreen
import com.documind.app.ui.theme.WarningAmber
import com.documind.app.ui.theme.privacyNoteBackground
import com.documind.app.util.DocumentNameResolver

@OptIn(ExperimentalMaterial3Api::class)
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
    var showUrlSheet by remember { mutableStateOf(false) }
    var showTextSheet by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }
    var textInput by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }
    var textError by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    val urlSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val textSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                val stream = context.contentResolver.openInputStream(it)
                if (stream != null) {
                    val name = DocumentNameResolver.resolveDisplayName(context, it, "Document.pdf")
                    onExtractPdf(stream, name)
                } else {
                    Toast.makeText(context, "Could not open PDF.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to open PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val docxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                val stream = context.contentResolver.openInputStream(it)
                if (stream != null) {
                    val name = DocumentNameResolver.resolveDisplayName(context, it, "Document.docx")
                    onExtractDocx(stream, name)
                } else {
                    Toast.makeText(context, "Could not open Word file.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to open Word document", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val sourceItems = listOf(
        SourceCardItem(
            icon = Icons.Default.PictureAsPdf,
            label = "PDF Document",
            subtitle = "Contracts, reports, research papers",
            accentColor = SourceCardColors.PDF,
            onClick = { pdfLauncher.launch(arrayOf("application/pdf")) }
        ),
        SourceCardItem(
            icon = Icons.Default.Description,
            label = "Word Document",
            subtitle = "DOCX files, proposals, essays",
            accentColor = SourceCardColors.Word,
            onClick = {
                docxLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            }
        ),
        SourceCardItem(
            icon = Icons.Default.Link,
            label = "Web Article",
            subtitle = "Paste a URL to extract content",
            accentColor = SourceCardColors.URL,
            onClick = { urlError = null; showUrlSheet = true }
        ),
        SourceCardItem(
            icon = Icons.Default.ContentPaste,
            label = "Paste Text",
            subtitle = "Any text content or notes",
            accentColor = SourceCardColors.Text,
            onClick = { textError = null; showTextSheet = true }
        )
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 32.dp)
        ) {
            // ── App header (left-aligned, compact) ──────────────────────
            AppHeader(isLlmReady = isLlmReady)

            Spacer(modifier = Modifier.height(28.dp))

            if (extractionState is ExtractionState.Extracting) {
                DocumindLoadingCard(
                    title = "Reading document",
                    subtitle = "Extracting text on-device — nothing is uploaded."
                )
            } else {
                // ── Demo CTA ─────────────────────────────────────────────
                DemoCard(onClick = onLoadDemoDocument)

                Spacer(modifier = Modifier.height(28.dp))

                // ── Section label ─────────────────────────────────────────
                Text(
                    text = "IMPORT DOCUMENT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // ── Source list ───────────────────────────────────────────
                SourceListSection(items = sourceItems)
            }

            // ── Model status (only when AI not ready) ─────────────────
            if (!isLlmReady) {
                Spacer(modifier = Modifier.height(16.dp))
                ModelStatusBanner(
                    modelState = modelState,
                    onDownloadClick = onStartModelDownload,
                    onCellularClick = onRequestCellularDownload
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            PrivacyFooter()
        }
    }

    if (showUrlSheet) {
        InputBottomSheet(
            title = "Import from URL",
            subtitle = "Paste a link to extract and chat with its content",
            value = urlInput,
            onValueChange = { urlInput = it; urlError = null },
            label = "Website URL",
            placeholder = "https://example.com/article",
            confirmLabel = "Extract Content",
            sheetState = urlSheetState,
            onDismiss = { showUrlSheet = false; urlInput = ""; urlError = null },
            onConfirm = {
                if (urlInput.isBlank()) {
                    urlError = "Please enter a URL"
                } else {
                    val url = if (!urlInput.startsWith("http")) "https://$urlInput" else urlInput
                    onExtractUrl(url)
                    showUrlSheet = false; urlInput = ""; urlError = null
                }
            },
            errorMessage = urlError
        )
    }

    if (showTextSheet) {
        InputBottomSheet(
            title = "Paste Text",
            subtitle = "Paste any text content to analyze with on-device AI",
            value = textInput,
            onValueChange = { textInput = it; textError = null },
            label = "Document text",
            placeholder = "Paste your text here…",
            confirmLabel = "Load Document",
            sheetState = textSheetState,
            onDismiss = { showTextSheet = false; textInput = ""; textError = null },
            onConfirm = {
                if (textInput.isBlank()) {
                    textError = "Please paste some text first"
                } else {
                    onExtractText(textInput)
                    showTextSheet = false; textInput = ""; textError = null
                }
            },
            isMultiline = true,
            errorMessage = textError
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

// ── Subcomponents ────────────────────────────────────────────────────────────

@Composable
private fun AppHeader(isLlmReady: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BrandLogo(size = 48.dp, iconSize = 24.dp, showShadow = false)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "DocuMind",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "On-device document AI",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isLlmReady) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(AiReadyGreen, CircleShape)
                )
                Text(
                    text = "Ready",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = AiReadyGreen
                )
            }
        }
    }
}

@Composable
private fun DemoCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        shape = RoundedCornerShape(DocumindDimens.CardRadius),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
        border = BorderStroke(
            1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        ),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Primary accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(color = MaterialTheme.colorScheme.primary)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Try a live demo",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Chat with a sample agreement — no file needed. Great for a first look.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "Open →",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyFooter() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = "All AI processing is on-device. Your documents never leave your phone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ModelStatusBanner(
    modelState: ModelState,
    onDownloadClick: () -> Unit,
    onCellularClick: () -> Unit
) {
    val isError = modelState is ModelState.Error
    val errorMessage = if (isError) (modelState as ModelState.Error).message else ""

    LaunchedEffect(modelState) {
        if (modelState is ModelState.Downloading && modelState.progress >= 99) {
            CrashAnalytics.logModelDownloadPhase("home_ui_downloading_99", "transferring", modelState.progress)
        }
    }

    Surface(
        shape = RoundedCornerShape(DocumindDimens.CardRadius),
        color = when {
            modelState is ModelState.Downloading -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            modelState is ModelState.WaitingForWifi -> WarningAmber.copy(alpha = 0.1f)
            isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        when {
            modelState is ModelState.Downloading -> Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Downloading AI model",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${modelState.progress}%",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { modelState.progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Butt,
                    drawStopIndicator = {}
                )
            }
            modelState is ModelState.WaitingForWifi -> Row(
                modifier = Modifier.clickable { onCellularClick() }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Wifi, null, modifier = Modifier.size(18.dp), tint = WarningAmber)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Waiting for WiFi", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = WarningAmber)
                    Text("Tap to use mobile data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            isError -> Row(
                modifier = Modifier.clickable { onDownloadClick() }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                Column(modifier = Modifier.weight(1f)) {
                    Text(errorMessage, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.error)
                    Text("Tap to retry", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> Row(
                modifier = Modifier.clickable { onDownloadClick() }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Download AI model to begin", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                    Text("~670 MB  ·  Required for on-device AI", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
