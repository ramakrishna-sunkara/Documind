package com.documind.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.documind.app.domain.model.UiScreen
import com.documind.app.ui.screens.ChatScreen
import com.documind.app.ui.screens.HomeScreen
import com.documind.app.ui.screens.LoadingScreen
import com.documind.app.ui.theme.DocumindTheme
import com.documind.app.ui.viewmodel.DocuMindViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DocumindTheme {
                DocuMindMainContent()
            }
        }
    }
}

@Composable
fun DocuMindMainContent() {
    val viewModel: DocuMindViewModel = viewModel(
        factory = DocuMindViewModel.Factory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext
        )
    )
    
    val currentScreen by viewModel.currentScreen.collectAsState()
    val modelState by viewModel.modelState.collectAsState()
    val extractionState by viewModel.extractionState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val queryState by viewModel.queryState.collectAsState()
    val currentDocument by viewModel.currentDocument.collectAsState()
    
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (currentScreen) {
            is UiScreen.Loading -> {
                LoadingScreen(
                    modelState = modelState,
                    onSkipModel = viewModel::skipModelLoading,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is UiScreen.Home -> {
                HomeScreen(
                    extractionState = extractionState,
                    onExtractPdf = viewModel::extractPdf,
                    onExtractDocx = viewModel::extractDocx,
                    onExtractUrl = viewModel::extractUrl,
                    onExtractText = viewModel::extractText,
                    onDismissError = viewModel::dismissExtractionError,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is UiScreen.Chat -> {
                currentDocument?.let { document ->
                    ChatScreen(
                        document = document,
                        messages = messages,
                        queryState = queryState,
                        onSendQuery = viewModel::sendQuery,
                        onClearDocument = viewModel::clearDocument
                    )
                }
            }
        }
    }
}