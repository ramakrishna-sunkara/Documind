package com.documind.prescription.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.documind.prescription.data.PrescriptionAIManager
import com.documind.prescription.data.PrescriptionRepository
import com.documind.prescription.data.SpeechManager
import com.documind.prescription.domain.ChatMessage
import com.documind.prescription.domain.Prescription
import com.documind.prescription.domain.QuickAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrescriptionViewModel(
    private val prescriptionRepository: PrescriptionRepository,
    private val aiManager: PrescriptionAIManager,
    private val speechManager: SpeechManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "PrescriptionViewModel"
    }
    
    private val _prescriptions = MutableStateFlow<List<Prescription>>(emptyList())
    val prescriptions: StateFlow<List<Prescription>> = _prescriptions.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _showChatSheet = MutableStateFlow(false)
    val showChatSheet: StateFlow<Boolean> = _showChatSheet.asStateFlow()
    
    val speechState: StateFlow<SpeechManager.SpeechState> = speechManager.speechState
    val ttsState: StateFlow<SpeechManager.TTSState> = speechManager.ttsState
    
    private val _isAIReady = MutableStateFlow(false)
    val isAIReady: StateFlow<Boolean> = _isAIReady.asStateFlow()
    
    init {
        loadPrescriptions()
        initializeSpeech()
    }
    
    private fun loadPrescriptions() {
        viewModelScope.launch {
            _isLoading.value = true
            prescriptionRepository.loadPrescriptions().fold(
                onSuccess = { list ->
                    _prescriptions.value = list
                    _isAIReady.value = aiManager.isOfflineReady()
                    Log.d(TAG, "Loaded ${list.size} prescriptions")
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load prescriptions: ${error.message}", error)
                }
            )
            _isLoading.value = false
        }
    }
    
    private fun initializeSpeech() {
        speechManager.initializeSpeechRecognizer()
        speechManager.initializeTTS()
    }
    
    fun refreshPrescriptions() {
        prescriptionRepository.clearCache()
        loadPrescriptions()
    }
    
    fun openChatSheet() {
        _showChatSheet.value = true
    }
    
    fun closeChatSheet() {
        _showChatSheet.value = false
        stopListening()
        stopSpeaking()
    }
    
    fun sendMessage(content: String) {
        if (content.isBlank() || _isProcessing.value) return
        
        viewModelScope.launch {
            val userMessage = ChatMessage(
                content = content,
                isUser = true
            )
            _messages.value = _messages.value + userMessage
            
            _isProcessing.value = true
            
            val loadingMessage = ChatMessage(
                content = "",
                isUser = false,
                isLoading = true
            )
            _messages.value = _messages.value + loadingMessage
            
            try {
                val response = aiManager.generateResponse(content)
                
                val aiMessage = ChatMessage(
                    content = response.content,
                    isUser = false,
                    source = response.source
                )
                _messages.value = _messages.value.dropLast(1) + aiMessage
                
                Log.d(TAG, "AI response generated via ${response.source}")
            } catch (e: Exception) {
                Log.e(TAG, "AI generation failed: ${e.message}", e)
                val errorMessage = ChatMessage(
                    content = "Sorry, I couldn't process your request. Please try again.",
                    isUser = false,
                    isError = true
                )
                _messages.value = _messages.value.dropLast(1) + errorMessage
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    fun handleQuickAction(action: QuickAction) {
        sendMessage(action.prompt)
    }
    
    fun startListening() {
        speechManager.startListening()
    }
    
    fun stopListening() {
        speechManager.stopListening()
    }
    
    fun speak(text: String) {
        speechManager.speak(text)
    }
    
    fun stopSpeaking() {
        speechManager.stopSpeaking()
    }
    
    fun resetSpeechState() {
        speechManager.resetSpeechState()
    }
    
    fun isSpeechAvailable(): Boolean = speechManager.isSpeechAvailable()
    
    fun clearMessages() {
        _messages.value = emptyList()
    }
    
    override fun onCleared() {
        super.onCleared()
        speechManager.release()
    }
    
    class Factory(
        private val prescriptionRepository: PrescriptionRepository,
        private val aiManager: PrescriptionAIManager,
        private val speechManager: SpeechManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PrescriptionViewModel::class.java)) {
                return PrescriptionViewModel(
                    prescriptionRepository,
                    aiManager,
                    speechManager
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
