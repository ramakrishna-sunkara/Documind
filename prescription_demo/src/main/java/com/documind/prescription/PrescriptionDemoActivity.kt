package com.documind.prescription

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.documind.prescription.data.LocalLLMWrapper
import com.documind.prescription.data.ModelLoadState
import com.documind.prescription.data.ModelManager
import com.documind.prescription.data.PrescriptionAIManager
import com.documind.prescription.data.PrescriptionRepository
import com.documind.prescription.data.SpeechManager
import com.documind.prescription.ui.AIChatScreen
import com.documind.prescription.ui.PrescriptionListScreen
import com.documind.prescription.ui.theme.PrescriptionDemoTheme
import com.documind.prescription.ui.viewmodel.PrescriptionViewModel
import kotlinx.coroutines.launch

class PrescriptionDemoActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "PrescriptionDemo"
        const val EXTRA_PRESCRIPTIONS_JSON = "prescriptions_json"
    }
    
    private lateinit var viewModel: PrescriptionViewModel
    private lateinit var modelManager: ModelManager
    private lateinit var llmWrapper: LocalLLMWrapper
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Audio permission granted")
        } else {
            Toast.makeText(
                this,
                "Voice input requires microphone permission",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        initializeDependencies()
        checkAudioPermission()
        initializeModel()
        
        setContent {
            val modelState by modelManager.modelState.collectAsState()
            
            PrescriptionDemoTheme {
                PrescriptionDemoContent(
                    viewModel = viewModel,
                    modelState = modelState
                )
            }
        }
    }
    
    private fun initializeDependencies() {
        modelManager = ModelManager(applicationContext)
        llmWrapper = LocalLLMWrapper(applicationContext)
        
        val prescriptionRepository = PrescriptionRepository(applicationContext)
        val aiManager = PrescriptionAIManager(llmWrapper, prescriptionRepository)
        val speechManager = SpeechManager(applicationContext)
        
        val customJson = intent.getStringExtra(EXTRA_PRESCRIPTIONS_JSON)
        if (!customJson.isNullOrBlank()) {
            lifecycleScope.launch {
                prescriptionRepository.loadPrescriptionsFromJson(customJson)
            }
        }
        
        viewModel = ViewModelProvider(
            this,
            PrescriptionViewModel.Factory(prescriptionRepository, aiManager, speechManager)
        )[PrescriptionViewModel::class.java]
    }
    
    private fun checkAudioPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "Audio permission already granted")
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                Toast.makeText(
                    this,
                    "Voice input needs microphone access",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    
    private fun initializeModel() {
        lifecycleScope.launch {
            modelManager.modelState.collect { state ->
                when (state) {
                    is ModelLoadState.Ready -> {
                        val modelPath = modelManager.getModelPath()
                        if (modelPath != null) {
                            Log.d(TAG, "Model ready at: $modelPath")
                            llmWrapper.initialize(modelPath)
                        }
                    }
                    is ModelLoadState.Copying -> {
                        Log.d(TAG, "Copying model: ${state.progress}%")
                    }
                    is ModelLoadState.Error -> {
                        Log.e(TAG, "Model error: ${state.message}")
                    }
                    else -> {}
                }
            }
        }
        
        lifecycleScope.launch {
            modelManager.checkAndLoadModel()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        llmWrapper.close()
    }
}

@Composable
private fun PrescriptionDemoContent(
    viewModel: PrescriptionViewModel,
    modelState: ModelLoadState
) {
    val prescriptions by viewModel.prescriptions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showChat by viewModel.showChatSheet.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val speechState by viewModel.speechState.collectAsState()
    val ttsState by viewModel.ttsState.collectAsState()
    
    val isAIReady = modelState is ModelLoadState.Ready || modelState is ModelLoadState.Error
    val modelStatusText = when (modelState) {
        is ModelLoadState.Idle -> "Initializing..."
        is ModelLoadState.Loading -> "Loading model..."
        is ModelLoadState.Copying -> "Copying model: ${modelState.progress}%"
        is ModelLoadState.Ready -> "AI Ready (Offline)"
        is ModelLoadState.Error -> "AI Ready (Online)"
    }
    
    Surface(modifier = Modifier.fillMaxSize()) {
        if (showChat) {
            AIChatScreen(
                messages = messages,
                isProcessing = isProcessing,
                speechState = speechState,
                ttsState = ttsState,
                isSpeechAvailable = viewModel.isSpeechAvailable(),
                onSendMessage = viewModel::sendMessage,
                onQuickAction = viewModel::handleQuickAction,
                onStartListening = viewModel::startListening,
                onStopListening = viewModel::stopListening,
                onSpeak = viewModel::speak,
                onStopSpeaking = viewModel::stopSpeaking,
                onBack = viewModel::closeChatSheet
            )
        } else {
            PrescriptionListScreen(
                prescriptions = prescriptions,
                isLoading = isLoading,
                isAIReady = isAIReady,
                modelStatusText = modelStatusText,
                onAIChatClick = viewModel::openChatSheet,
                onRefresh = viewModel::refreshPrescriptions
            )
        }
    }
}
