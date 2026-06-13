package com.documind.app.ui.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.documind.app.data.analytics.AnalyticsManager
import com.documind.app.data.analytics.CrashAnalytics
import com.documind.app.data.preferences.OnboardingPreferences
import com.documind.app.data.llm.ModelFileCopier
import com.documind.app.data.llm.RagPipelineManager
import com.documind.app.data.llm.ModelState
import com.documind.app.data.llm.ModelStatusManager
import com.documind.app.domain.model.ChatMessage
import com.documind.app.domain.model.DocumentContent
import com.documind.app.domain.model.ExtractionState
import com.documind.app.domain.model.QueryState
import com.documind.app.domain.model.UiScreen
import com.documind.app.domain.usecase.ExtractContentUseCase
import com.documind.app.domain.usecase.ProcessQueryUseCase
import com.documind.app.util.ErrorCategory
import com.documind.app.util.UserFacingErrors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream

class DocuMindViewModel(
    private val appContext: Context,
    private val onboardingPreferences: OnboardingPreferences,
    private val modelStatusManager: ModelStatusManager,
    private val ragPipelineManager: RagPipelineManager,
    private val modelFileCopier: ModelFileCopier = ModelFileCopier(appContext),
    private val extractContentUseCase: ExtractContentUseCase = ExtractContentUseCase(),
    private val processQueryUseCase: ProcessQueryUseCase
) : ViewModel() {
    
    companion object {
        private const val DEMO_ASSET_PATH: String = "demo/sample_document.txt"
        private const val DEMO_DOCUMENT_LABEL: String = "Confidential Agreement (Demo)"
    }
    
    private var isRagInitializing: Boolean = false
    private var pendingQuery: String? = null
    private var isIndexingDocument: Boolean = false
    
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
    
    private val _isLlmReady = MutableStateFlow(false)
    val isLlmReady: StateFlow<Boolean> = _isLlmReady.asStateFlow()
    
    private var modelInitStartTime = 0L
    private var queryStartTime = 0L
    
    init {
        val initialScreen: UiScreen = if (onboardingPreferences.isOnboardingCompleted()) {
            UiScreen.Home
        } else {
            UiScreen.Onboarding
        }
        _currentScreen.value = initialScreen
        try {
            AnalyticsManager.logScreenView(
                when (initialScreen) {
                    UiScreen.Onboarding -> "Onboarding"
                    else -> "Home"
                }
            )
        } catch (_: Exception) {
        }
        CrashAnalytics.log("DocuMindViewModel init screen=$initialScreen")
        checkModelAndInitialize()
    }
    
    fun completeOnboarding() {
        onboardingPreferences.setOnboardingCompleted()
        _currentScreen.value = UiScreen.Home
        try {
            AnalyticsManager.logScreenView("Home")
        } catch (_: Exception) {
        }
    }
    
    fun completeOnboardingAndTryDemo() {
        onboardingPreferences.setOnboardingCompleted()
        _currentScreen.value = UiScreen.Home
        loadDemoDocument()
    }
    
    private fun checkModelAndInitialize() {
        viewModelScope.launch {
            try {
                modelInitStartTime = System.currentTimeMillis()
                modelStatusManager.checkModelStatus()
                modelStatusManager.modelState.collect { state ->
                    when (state) {
                        is ModelState.Ready -> {
                            CrashAnalytics.logModelDownloadPhase("viewmodel_ready", "ready", progress = 100)
                            _isLlmReady.value = true
                        }
                        is ModelState.Error -> {
                            _isLlmReady.value = false
                            CrashAnalytics.recordNonFatal(
                                throwable = IllegalStateException(state.message),
                                phase = "model_state_error"
                            )
                            try {
                                AnalyticsManager.logModelInitFailed(state.message)
                            } catch (_: Exception) {
                            }
                        }
                        is ModelState.Downloading -> {
                            CrashAnalytics.logModelDownloadPhase(
                                phase = "viewmodel_downloading",
                                status = "downloading",
                                progress = state.progress
                            )
                        }
                        else -> Unit
                    }
                }
            } catch (e: Exception) {
                CrashAnalytics.recordNonFatal(e, "check_model_and_initialize")
            }
        }
    }

    private suspend fun ensureRagPipelineReady(): Result<Unit> {
        if (ragPipelineManager.isReady()) {
            return Result.success(Unit)
        }
        if (isRagInitializing) {
            return Result.failure(IllegalStateException(UserFacingErrors.ragNotReady()))
        }
        val modelPath = modelStatusManager.getModelPath()
        val geckoPath = modelStatusManager.getGeckoModelPath()
        val tokenizerPath = modelStatusManager.getTokenizerPath()
        if (modelPath == null || geckoPath == null || tokenizerPath == null) {
            return Result.failure(
                IllegalStateException(UserFacingErrors.modelNotReady(_modelState.value))
            )
        }
        isRagInitializing = true
        CrashAnalytics.logRagPhase("ensure_start")
        return try {
            val localPaths = modelFileCopier.ensureLocalCopies(
                llmSourcePath = modelPath,
                geckoSourcePath = geckoPath,
                tokenizerSourcePath = tokenizerPath
            ).getOrElse { error ->
                CrashAnalytics.recordNonFatal(error, "model_file_copy")
                return Result.failure(error)
            }
            CrashAnalytics.logRagPhase("copy_complete")
            ragPipelineManager.initialize(
                llmModelPath = localPaths.llmPath,
                geckoModelPathValue = localPaths.geckoPath,
                tokenizerPathValue = localPaths.tokenizerPath
            ).also { result ->
                if (result.isSuccess) {
                    CrashAnalytics.logRagPhase("initialize_success")
                    val duration = System.currentTimeMillis() - modelInitStartTime
                    try {
                        AnalyticsManager.logModelInitialized(duration)
                    } catch (_: Exception) {
                    }
                } else {
                    result.exceptionOrNull()?.let { error ->
                        CrashAnalytics.recordNonFatal(error, "rag_initialize_failed")
                    }
                }
            }.mapError { error ->
                Exception(UserFacingErrors.forThrowable(error, ErrorCategory.MODEL))
            }
        } catch (throwable: Throwable) {
            CrashAnalytics.recordNonFatal(throwable, "ensure_rag_pipeline")
            Result.failure(
                Exception(UserFacingErrors.forThrowable(throwable, ErrorCategory.MODEL))
            )
        } finally {
            isRagInitializing = false
        }
    }
    
    fun extractPdf(inputStream: InputStream, fileName: String) {
        viewModelScope.launch {
            _extractionState.value = ExtractionState.Extracting
            extractContentUseCase.extractFromPdf(inputStream, fileName).fold(
                onSuccess = { document -> handleExtractionSuccess(document) },
                onFailure = { error ->
                    handleExtractionFailure(
                        sourceType = com.documind.app.data.extractor.SourceType.PDF,
                        sourceLabel = "PDF",
                        error = error
                    )
                }
            )
        }
    }
    
    fun extractDocx(inputStream: InputStream, fileName: String) {
        viewModelScope.launch {
            _extractionState.value = ExtractionState.Extracting
            extractContentUseCase.extractFromDocx(inputStream, fileName).fold(
                onSuccess = { document -> handleExtractionSuccess(document) },
                onFailure = { error ->
                    handleExtractionFailure(
                        sourceType = com.documind.app.data.extractor.SourceType.DOCX,
                        sourceLabel = "Word document",
                        error = error
                    )
                }
            )
        }
    }
    
    fun extractUrl(url: String) {
        viewModelScope.launch {
            _extractionState.value = ExtractionState.Extracting
            extractContentUseCase.extractFromUrl(url).fold(
                onSuccess = { document -> handleExtractionSuccess(document) },
                onFailure = { error ->
                    handleExtractionFailure(
                        sourceType = com.documind.app.data.extractor.SourceType.URL,
                        sourceLabel = "web page",
                        error = error
                    )
                }
            )
        }
    }
    
    fun loadDemoDocument() {
        viewModelScope.launch {
            _extractionState.value = ExtractionState.Extracting
            try {
                val demoText: String = appContext.assets.open(DEMO_ASSET_PATH).bufferedReader().use { reader ->
                    reader.readText()
                }
                extractContentUseCase.extractFromText(demoText, DEMO_DOCUMENT_LABEL).fold(
                    onSuccess = { document -> handleExtractionSuccess(document) },
                    onFailure = { error ->
                        handleExtractionFailure(
                            sourceType = com.documind.app.data.extractor.SourceType.TEXT,
                            sourceLabel = "demo document",
                            error = error,
                            category = ErrorCategory.DEMO
                        )
                    }
                )
            } catch (e: Exception) {
                val errorMsg = UserFacingErrors.forMessage(
                    "Demo document not found",
                    ErrorCategory.DEMO
                )
                _extractionState.value = ExtractionState.Error(errorMsg)
                CrashAnalytics.recordNonFatal(e, "demo_load_failed")
            }
        }
    }
    
    fun extractText(text: String) {
        _extractionState.value = ExtractionState.Extracting
        extractContentUseCase.extractFromText(text).fold(
            onSuccess = { document -> handleExtractionSuccess(document) },
            onFailure = { error ->
                handleExtractionFailure(
                    sourceType = com.documind.app.data.extractor.SourceType.TEXT,
                    sourceLabel = "text",
                    error = error,
                    category = ErrorCategory.VALIDATION
                )
            }
        )
    }

    private fun handleExtractionFailure(
        sourceType: com.documind.app.data.extractor.SourceType,
        sourceLabel: String,
        error: Throwable,
        category: ErrorCategory = ErrorCategory.EXTRACTION
    ) {
        val errorMsg = when (category) {
            ErrorCategory.DEMO, ErrorCategory.VALIDATION ->
                UserFacingErrors.forThrowable(error, category)
            else ->
                UserFacingErrors.extractionFailed(sourceLabel, error)
        }
        AnalyticsManager.logDocumentExtractionFailed(sourceType, errorMsg)
        CrashAnalytics.recordNonFatal(error, "extraction_failed")
        _extractionState.value = ExtractionState.Error(errorMsg)
    }
    
    private fun handleExtractionSuccess(document: DocumentContent) {
        _currentDocument.value = document
        _currentScreen.value = UiScreen.Chat
        _messages.value = emptyList()
        pendingQuery = null
        _queryState.value = QueryState.Idle
        indexDocument(document)
    }

    private fun indexDocument(document: DocumentContent) {
        if (isIndexingDocument) {
            return
        }
        viewModelScope.launch {
            isIndexingDocument = true
            _extractionState.value = ExtractionState.Indexing
            try {
                if (!_isLlmReady.value) {
                    _extractionState.value = ExtractionState.Error(
                        UserFacingErrors.modelNotReady(_modelState.value)
                    )
                    return@launch
                }
                ensureRagPipelineReady().fold(
                    onSuccess = {
                        ragPipelineManager.indexChunks(document.chunks).fold(
                            onSuccess = {
                                _extractionState.value = ExtractionState.Success(document)
                                AnalyticsManager.logDocumentLoaded(
                                    sourceType = document.sourceType,
                                    wordCount = document.wordCount,
                                    isLarge = document.isLargeDocument
                                )
                                AnalyticsManager.logScreenView("Chat")
                                pendingQuery?.let { queuedQuery ->
                                    pendingQuery = null
                                    executeQuery(document, queuedQuery)
                                }
                            },
                            onFailure = { error ->
                                val errorMsg = UserFacingErrors.forThrowable(error, ErrorCategory.INDEXING)
                                _extractionState.value = ExtractionState.Error(errorMsg)
                                CrashAnalytics.recordNonFatal(error, "document_indexing")
                            }
                        )
                    },
                    onFailure = { error ->
                        val errorMsg = UserFacingErrors.forThrowable(error, ErrorCategory.MODEL)
                        _extractionState.value = ExtractionState.Error(errorMsg)
                        try {
                            AnalyticsManager.logModelInitFailed(errorMsg)
                        } catch (_: Exception) {
                        }
                    }
                )
            } finally {
                isIndexingDocument = false
            }
        }
    }
    
    fun sendQuery(query: String) {
        val document = _currentDocument.value ?: return
        if (query.isBlank()) {
            return
        }
        viewModelScope.launch {
            val userMessage = ChatMessage(content = query, isUser = true)
            _messages.value = _messages.value + userMessage
            AnalyticsManager.logQuerySent(query.length)
            val indexingState = _extractionState.value
            when {
                indexingState is ExtractionState.Indexing || isIndexingDocument -> {
                    pendingQuery = query
                    _messages.value = _messages.value + infoMessage(
                        "Preparing your document for on-device AI. I'll answer as soon as indexing finishes."
                    )
                    return@launch
                }
                indexingState is ExtractionState.Error -> {
                    _messages.value = _messages.value + errorMessage(
                        "Could not prepare this document: ${indexingState.message}"
                    )
                    AnalyticsManager.logQueryFailed(indexingState.message)
                    return@launch
                }
                !_isLlmReady.value || !ragPipelineManager.isReady() -> {
                    _messages.value = _messages.value + errorMessage(
                        UserFacingErrors.modelNotReady(_modelState.value)
                    )
                    AnalyticsManager.logQueryFailed("Model not ready")
                    return@launch
                }
                !ragPipelineManager.hasIndexedDocument() -> {
                    _messages.value = _messages.value + errorMessage(
                        UserFacingErrors.documentNotIndexed()
                    )
                    AnalyticsManager.logQueryFailed("Document not indexed")
                    return@launch
                }
            }
            executeQuery(document, query)
        }
    }

    private suspend fun executeQuery(document: DocumentContent, query: String) {
        _queryState.value = QueryState.Processing
        queryStartTime = System.currentTimeMillis()
        _messages.value = _messages.value + ChatMessage(
            content = "",
            isUser = false,
            isLoading = true
        )
        processQueryUseCase.process(document, query).fold(
            onSuccess = { response ->
                val duration = System.currentTimeMillis() - queryStartTime
                _messages.value = _messages.value.dropLast(1) + ChatMessage(
                    content = response,
                    isUser = false
                )
                _queryState.value = QueryState.Idle
                AnalyticsManager.logQueryResponseReceived(response.length, duration)
            },
            onFailure = { error ->
                val errorMsg = UserFacingErrors.queryFailed(error)
                _messages.value = _messages.value.dropLast(1) + errorMessage(errorMsg)
                _queryState.value = QueryState.Error(errorMsg)
                AnalyticsManager.logQueryFailed(errorMsg)
            }
        )
    }

    private fun errorMessage(content: String): ChatMessage {
        return ChatMessage(content = content, isUser = false, isError = true)
    }

    private fun infoMessage(content: String): ChatMessage {
        return ChatMessage(content = content, isUser = false, isInfo = true)
    }
    
    fun retryDocumentIndexing() {
        val document = _currentDocument.value ?: return
        _queryState.value = QueryState.Idle
        indexDocument(document)
    }
    
    fun retryLastQuery() {
        val document = _currentDocument.value ?: return
        val lastUserQuery = _messages.value.lastOrNull { message -> message.isUser }?.content ?: return
        viewModelScope.launch {
            _queryState.value = QueryState.Idle
            executeQuery(document, lastUserQuery)
        }
    }
    
    fun clearDocument() {
        pendingQuery = null
        isIndexingDocument = false
        _currentDocument.value = null
        _messages.value = emptyList()
        _extractionState.value = ExtractionState.Idle
        _queryState.value = QueryState.Idle
        _currentScreen.value = UiScreen.Home
        AnalyticsManager.logDocumentCleared()
        AnalyticsManager.logScreenView("Home")
    }
    
    fun dismissExtractionError() {
        _extractionState.value = ExtractionState.Idle
    }
    
    fun skipModelLoading() {
        _currentScreen.value = UiScreen.Home
        AnalyticsManager.logModelSkipped()
        AnalyticsManager.logScreenView("Home")
    }
    
    fun setActivity(activity: Activity?) {
        modelStatusManager.setActivity(activity)
    }
    
    fun requestCellularDownload(activity: Activity) {
        modelStatusManager.requestCellularDownload(activity)
        AnalyticsManager.logEvent("cellular_download_requested", null)
    }
    
    fun startModelDownload() {
        modelStatusManager.startDownload()
        AnalyticsManager.logEvent("model_download_started", null)
    }
    
    override fun onCleared() {
        super.onCleared()
        modelStatusManager.cleanup()
        ragPipelineManager.close()
    }
    
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val modelStatusManager = ModelStatusManager(context)
            val ragPipelineManager = RagPipelineManager(context)
            val processQueryUseCase = ProcessQueryUseCase(ragPipelineManager)
            return DocuMindViewModel(
                appContext = context.applicationContext,
                onboardingPreferences = OnboardingPreferences(context.applicationContext),
                modelStatusManager = modelStatusManager,
                ragPipelineManager = ragPipelineManager,
                processQueryUseCase = processQueryUseCase
            ) as T
        }
    }
}

private fun <T> Result<T>.mapError(transform: (Throwable) -> Exception): Result<T> {
    return fold(
        onSuccess = { Result.success(it) },
        onFailure = { error -> Result.failure(transform(error)) }
    )
}
