package com.documind.app.domain.model

sealed class UiScreen {
    data object Loading : UiScreen()
    data object Onboarding : UiScreen()
    data object Home : UiScreen()
    data object Chat : UiScreen()
}

sealed class ExtractionState {
    data object Idle : ExtractionState()
    data object Extracting : ExtractionState()
    data object Indexing : ExtractionState()
    data class Success(val document: DocumentContent) : ExtractionState()
    data class Error(val message: String) : ExtractionState()
}

sealed class QueryState {
    data object Idle : QueryState()
    data object Processing : QueryState()
    data class Error(val message: String) : QueryState()
}
