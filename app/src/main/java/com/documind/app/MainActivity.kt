package com.documind.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.documind.app.data.llm.ModelState
import com.documind.app.data.analytics.CrashAnalytics
import com.documind.app.data.update.InAppUpdateManager
import com.documind.app.data.update.UpdateState
import kotlinx.coroutines.flow.MutableStateFlow
import com.documind.app.domain.model.ExtractionState
import com.documind.app.domain.model.UiScreen
import com.documind.app.ui.screens.ChatScreen
import com.documind.app.ui.screens.HomeScreen
import com.documind.app.ui.screens.LoadingScreen
import com.documind.app.ui.screens.OnboardingScreen
import com.documind.app.ui.theme.DocumindScreenBackground
import com.documind.app.ui.theme.DocumindTheme
import com.documind.app.ui.viewmodel.DocuMindViewModel

class MainActivity : ComponentActivity() {
    
    private lateinit var inAppUpdateManager: InAppUpdateManager
    
    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        inAppUpdateManager.handleUpdateResult(result.resultCode)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashAnalytics.log("MainActivity onCreate")
        
        try {
            // Initialize In-App Update Manager
            inAppUpdateManager = InAppUpdateManager(this)
            inAppUpdateManager.setUpdateLauncher(updateLauncher)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Update manager init failed: ${e.message}")
        }

        enableEdgeToEdge()
        setContent {
            DocumindTheme {
                DocuMindMainContent(if (::inAppUpdateManager.isInitialized) inAppUpdateManager else null)
            }
        }
        
        // Check for updates when app starts
        try {
            if (::inAppUpdateManager.isInitialized) {
                inAppUpdateManager.checkForUpdate()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Update check failed: ${e.message}")
        }
    }
    
    override fun onResume() {
        super.onResume()
        try {
            if (::inAppUpdateManager.isInitialized) {
                inAppUpdateManager.resumeUpdate()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Resume update failed: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        CrashAnalytics.log("MainActivity onDestroy")
        super.onDestroy()
        try {
            if (::inAppUpdateManager.isInitialized) {
                inAppUpdateManager.cleanup()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Cleanup failed: ${e.message}")
        }
    }
}

@Composable
fun DocuMindMainContent(inAppUpdateManager: InAppUpdateManager?) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    val viewModel: DocuMindViewModel = viewModel(
        factory = DocuMindViewModel.Factory(context.applicationContext)
    )
    
    // Set activity reference for cellular download permission
    DisposableEffect(activity) {
        viewModel.setActivity(activity)
        onDispose {
            viewModel.setActivity(null)
        }
    }
    
    val currentScreen by viewModel.currentScreen.collectAsState()
    val modelState by viewModel.modelState.collectAsState()
    val isLlmReady by viewModel.isLlmReady.collectAsState()
    val extractionState by viewModel.extractionState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val queryState by viewModel.queryState.collectAsState()
    val currentDocument by viewModel.currentDocument.collectAsState()
    
    // Update state - handle nullable manager
    val updateState by (inAppUpdateManager?.updateState
        ?: MutableStateFlow<UpdateState>(UpdateState.Idle)).collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show snackbar when update is ready to install
    LaunchedEffect(updateState) {
        if (updateState is UpdateState.ReadyToInstall && inAppUpdateManager != null) {
            val result = snackbarHostState.showSnackbar(
                message = "Update downloaded! Restart to apply.",
                actionLabel = "RESTART",
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                inAppUpdateManager.completeUpdate()
            }
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        DocumindScreenBackground(modifier = Modifier.padding(innerPadding)) {
        when (currentScreen) {
            is UiScreen.Loading -> {
                LoadingScreen(
                    modelState = modelState,
                    onSkipModel = viewModel::skipModelLoading,
                    onRequestCellularDownload = {
                        activity?.let { viewModel.requestCellularDownload(it) }
                    }
                )
            }
            is UiScreen.Onboarding -> {
                OnboardingScreen(
                    extractionState = extractionState,
                    onComplete = viewModel::completeOnboarding,
                    onTryDemo = viewModel::completeOnboardingAndTryDemo,
                    onDismissError = viewModel::dismissExtractionError
                )
            }
            is UiScreen.Home -> {
                HomeScreen(
                    extractionState = extractionState,
                    modelState = modelState,
                    isLlmReady = isLlmReady,
                    onExtractPdf = viewModel::extractPdf,
                    onExtractDocx = viewModel::extractDocx,
                    onExtractUrl = viewModel::extractUrl,
                    onExtractText = viewModel::extractText,
                    onDismissError = viewModel::dismissExtractionError,
                    onStartModelDownload = viewModel::startModelDownload,
                    onRequestCellularDownload = {
                        activity?.let { viewModel.requestCellularDownload(it) }
                    },
                    onLoadDemoDocument = viewModel::loadDemoDocument
                )
            }
            is UiScreen.Chat -> {
                val document = currentDocument
                if (document != null) {
                    ChatScreen(
                        document = document,
                        messages = messages,
                        queryState = queryState,
                        extractionState = extractionState,
                        onSendQuery = viewModel::sendQuery,
                        onRetryIndexing = viewModel::retryDocumentIndexing,
                        onRetryQuery = viewModel::retryLastQuery,
                        onClearDocument = viewModel::clearDocument
                    )
                } else {
                    ChatRecoveryScreen(onBack = viewModel::clearDocument)
                }
            }
        }
        }
    }
}

@Composable
private fun ChatRecoveryScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = "The document session was lost. Go back and open the document again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
        )
        Button(onClick = onBack) {
            Text(text = "Back to Home")
        }
    }
}