package com.documind.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.documind.app.data.llm.LocalLLMManager
import com.documind.app.data.llm.ModelState
import com.documind.app.data.llm.ModelStatusManager
import com.documind.app.domain.model.ChatMessage
import com.documind.app.domain.model.DocumentContent
import com.documind.app.domain.model.ExtractionState
import com.documind.app.domain.model.QueryState
import com.documind.app.domain.model.UiScreen
import com.documind.app.domain.usecase.ExtractContentUseCase
import com.documind.app.domain.usecase.ProcessQueryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream

class DocuMindViewModel(
    private val modelStatusManager: ModelStatusManager,
    private val llmManager: LocalLLMManager,
    private val extractContentUseCase: ExtractContentUseCase = ExtractContentUseCase(),
    private val processQueryUseCase: ProcessQueryUseCase
) : ViewModel() {
    
    private val _currentScreen = MutableStateFlow<UiScreen>(UiScreen.Loading)
    val currentScreen: StateFlow<UiScreen> = _currentScreen.asStateFlow()
    
    private val _modelState = modelStatusManager.modelState
    val modelState: StateFlow<ModelState> = _modelState
    
    private val _extractionState = MutableStateFlow<ExtractionState>(ExtractionState.Idle)
    val extractionState: StateFlow<ExtractionState> = _extractionState.asStateFlow()
    
    private val _queryState = MutableStateFlow<QueryState>(QueryState.Idle)
    val queryState: StateFlow<QueryState> = _queryState.asStateFlow()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _currentDocument = MutableStateFlow<DocumentContent?>(null)
    val currentDocument: StateFlow<DocumentContent?> = _currentDocument.asStateFlow()
    
    init {
        checkModelAndInitialize()
    }
    
    private fun checkModelAndInitialize() {
        viewModelScope.launch {
            modelStatusManager.checkModelStatus()
            
            modelStatusManager.modelState.collect { state ->
                when (state) {
                    is ModelState.Ready -> {
                        initializeLLM()
                    }
                    is ModelState.Error -> {
                        _currentScreen.value = UiScreen.Loading
                    }
                    else -> {
                        _currentScreen.value = UiScreen.Loading
                    }
                }
            }
        }
    }
    
    private fun initializeLLM() {
        viewModelScope.launch {
            val modelPath = modelStatusManager.getModelPath()
            if (modelPath != null) {
                llmManager.initialize(modelPath).fold(
                    onSuccess = {
                        _currentScreen.value = UiScreen.Home
                    },
                    onFailure = {
                        // Stay on loading screen with error
                    }
                )
            }
        }
    }
    
    fun extractPdf(inputStream: InputStream, fileName: String) {
        viewModelScope.launch {
            _extractionState.value = ExtractionState.Extracting
            
            extractContentUseCase.extractFromPdf(inputStream, fileName).fold(
                onSuccess = { document ->
                    handleExtractionSuccess(document)
                },
                onFailure = { error ->
                    _extractionState.value = ExtractionState.Error(
                        error.message ?: "Failed to extract PDF"
                    )
                }
            )
        }
    }
    
    fun extractDocx(inputStream: InputStream, fileName: String) {
        viewModelScope.launch {
            _extractionState.value = ExtractionState.Extracting
            
            extractContentUseCase.extractFromDocx(inputStream, fileName).fold(
                onSuccess = { document ->
                    handleExtractionSuccess(document)
                },
                onFailure = { error ->
                    _extractionState.value = ExtractionState.Error(
                        error.message ?: "Failed to extract Word document"
                    )
                }
            )
        }
    }
    
    fun extractUrl(url: String) {
        viewModelScope.launch {
            _extractionState.value = ExtractionState.Extracting
            
            extractContentUseCase.extractFromUrl(url).fold(
                onSuccess = { document ->
                    handleExtractionSuccess(document)
                },
                onFailure = { error ->
                    _extractionState.value = ExtractionState.Error(
                        error.message ?: "Failed to extract URL content"
                    )
                }
            )
        }
    }
    
    fun extractText(text: String) {
        _extractionState.value = ExtractionState.Extracting
        
        extractContentUseCase.extractFromText(text).fold(
            onSuccess = { document ->
                handleExtractionSuccess(document)
            },
            onFailure = { error ->
                _extractionState.value = ExtractionState.Error(
                    error.message ?: "Invalid text input"
                )
            }
        )
    }
    
    private fun handleExtractionSuccess(document: DocumentContent) {
        _currentDocument.value = document
        _extractionState.value = ExtractionState.Success(document)
        _currentScreen.value = UiScreen.Chat
        _messages.value = emptyList()
    }
    
    fun sendQuery(query: String) {
        val document = _currentDocument.value ?: return
        
        viewModelScope.launch {
            val userMessage = ChatMessage(content = query, isUser = true)
            _messages.value = _messages.value + userMessage
            
            if (!llmManager.isReady()) {
                val errorMessage = ChatMessage(
                    content = "AI model is not loaded. Please download the Gemma model file and restart the app to enable AI responses.",
                    isUser = false
                )
                _messages.value = _messages.value + errorMessage
                return@launch
            }
            
            _queryState.value = QueryState.Processing
            
            val loadingMessage = ChatMessage(
                content = "",
                isUser = false,
                isLoading = true
            )
            _messages.value = _messages.value + loadingMessage
            
            processQueryUseCase.process(document, query).fold(
                onSuccess = { response ->
                    val aiMessage = ChatMessage(content = response, isUser = false)
                    _messages.value = _messages.value.dropLast(1) + aiMessage
                    _queryState.value = QueryState.Idle
                },
                onFailure = { error ->
                    val errorMessage = ChatMessage(
                        content = "Error: ${error.message ?: "Failed to generate response"}",
                        isUser = false
                    )
                    _messages.value = _messages.value.dropLast(1) + errorMessage
                    _queryState.value = QueryState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }
    
    fun clearDocument() {
        _currentDocument.value = null
        _messages.value = emptyList()
        _extractionState.value = ExtractionState.Idle
        _queryState.value = QueryState.Idle
        _currentScreen.value = UiScreen.Home
    }
    
    fun dismissExtractionError() {
        _extractionState.value = ExtractionState.Idle
    }
    
    fun skipModelLoading() {
        _currentScreen.value = UiScreen.Home
    }
    
    override fun onCleared() {
        super.onCleared()
        modelStatusManager.cleanup()
        llmManager.close()
    }
    
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val modelStatusManager = ModelStatusManager(context)
            val llmManager = LocalLLMManager(context)
            val processQueryUseCase = ProcessQueryUseCase(llmManager)
            
            return DocuMindViewModel(
                modelStatusManager = modelStatusManager,
                llmManager = llmManager,
                processQueryUseCase = processQueryUseCase
            ) as T
        }
    }
}
