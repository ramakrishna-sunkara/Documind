package com.documind.prescription.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class SpeechManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SpeechManager"
    }
    
    sealed class SpeechState {
        data object Idle : SpeechState()
        data object Listening : SpeechState()
        data class Result(val text: String) : SpeechState()
        data class Error(val message: String) : SpeechState()
        data class PartialResult(val text: String) : SpeechState()
    }
    
    sealed class TTSState {
        data object Idle : TTSState()
        data object Speaking : TTSState()
        data object Initializing : TTSState()
        data class Error(val message: String) : TTSState()
    }
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTTSReady = false
    
    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()
    
    private val _ttsState = MutableStateFlow<TTSState>(TTSState.Idle)
    val ttsState: StateFlow<TTSState> = _ttsState.asStateFlow()
    
    fun initializeSpeechRecognizer(): Boolean {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available on this device")
            return false
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())
        }
        
        Log.d(TAG, "Speech recognizer initialized")
        return true
    }
    
    fun initializeTTS() {
        _ttsState.value = TTSState.Initializing
        
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                isTTSReady = result != TextToSpeech.LANG_MISSING_DATA && 
                             result != TextToSpeech.LANG_NOT_SUPPORTED
                
                if (isTTSReady) {
                    textToSpeech?.setOnUtteranceProgressListener(createUtteranceListener())
                    _ttsState.value = TTSState.Idle
                    Log.d(TAG, "TTS initialized successfully")
                } else {
                    _ttsState.value = TTSState.Error("Language not supported")
                    Log.e(TAG, "TTS language not supported")
                }
            } else {
                isTTSReady = false
                _ttsState.value = TTSState.Error("TTS initialization failed")
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }
    
    fun startListening() {
        if (speechRecognizer == null) {
            if (!initializeSpeechRecognizer()) {
                _speechState.value = SpeechState.Error("Speech recognition not available")
                return
            }
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        _speechState.value = SpeechState.Listening
        speechRecognizer?.startListening(intent)
        Log.d(TAG, "Started listening")
    }
    
    fun stopListening() {
        speechRecognizer?.stopListening()
        _speechState.value = SpeechState.Idle
        Log.d(TAG, "Stopped listening")
    }
    
    fun speak(text: String) {
        if (!isTTSReady) {
            Log.w(TAG, "TTS not ready, initializing...")
            initializeTTS()
            return
        }
        
        val cleanText = text
            .replace("**", "")
            .replace("*", "")
            .replace("#", "")
            .replace("•", "")
        
        val utteranceId = UUID.randomUUID().toString()
        _ttsState.value = TTSState.Speaking
        
        textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        Log.d(TAG, "Started speaking: ${cleanText.take(50)}...")
    }
    
    fun stopSpeaking() {
        textToSpeech?.stop()
        _ttsState.value = TTSState.Idle
        Log.d(TAG, "Stopped speaking")
    }
    
    fun resetSpeechState() {
        _speechState.value = SpeechState.Idle
    }
    
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }
            
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }
            
            override fun onRmsChanged(rmsdB: Float) {}
            
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
            }
            
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                Log.e(TAG, "Speech recognition error: $errorMessage ($error)")
                _speechState.value = SpeechState.Error(errorMessage)
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                Log.d(TAG, "Speech result: $text")
                _speechState.value = SpeechState.Result(text)
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    Log.d(TAG, "Partial result: $text")
                    _speechState.value = SpeechState.PartialResult(text)
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
    
    private fun createUtteranceListener(): UtteranceProgressListener {
        return object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _ttsState.value = TTSState.Speaking
            }
            
            override fun onDone(utteranceId: String?) {
                _ttsState.value = TTSState.Idle
            }
            
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _ttsState.value = TTSState.Error("Speech synthesis error")
            }
            
            override fun onError(utteranceId: String?, errorCode: Int) {
                _ttsState.value = TTSState.Error("Speech synthesis error: $errorCode")
            }
        }
    }
    
    fun isSpeechAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)
    
    fun isTTSAvailable(): Boolean = isTTSReady
    
    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isTTSReady = false
        
        Log.d(TAG, "Speech manager released")
    }
}
