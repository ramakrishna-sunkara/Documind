# Coroutines & Flow Rules

Rules enforcing Kotlin Coroutines and Flow best practices for DocuMind.

## COROUTINE-001: No GlobalScope
- **Severity**: error
- **Pattern**: `GlobalScope\.(launch|async)`
- **Message**: GlobalScope creates unstructured concurrency. Use viewModelScope or lifecycleScope.
- **Fix**: Replace GlobalScope with appropriate lifecycle-aware scope.

### Bad
```kotlin
class DocuMindViewModel : ViewModel() {
    fun fetchData() {
        GlobalScope.launch {
            val data = repository.getData()
        }
    }
}
```

### Good
```kotlin
class DocuMindViewModel : ViewModel() {
    fun fetchData() {
        viewModelScope.launch {
            val data = repository.getData()
        }
    }
}
```

---

## COROUTINE-002: Flow Over LiveData
- **Severity**: warning
- **Pattern**: `MutableLiveData<|LiveData<`
- **Message**: Prefer StateFlow/SharedFlow over LiveData for new code.
- **Fix**: Replace LiveData with StateFlow for state, SharedFlow for events.

### Bad
```kotlin
class DocuMindViewModel : ViewModel() {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState
}
```

### Good
```kotlin
class DocuMindViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
}
```

---

## COROUTINE-003: Dispatcher for IO
- **Severity**: warning
- **Pattern**: `suspend fun \w+[^{]*\{[^}]*(openInputStream|openOutputStream|File\(|URL\(|HttpClient|OkHttpClient)`
- **Message**: IO operations should explicitly use Dispatchers.IO via flowOn or withContext.
- **Fix**: Wrap IO operations in withContext(Dispatchers.IO).

### Bad
```kotlin
suspend fun extractPdf(uri: Uri): String {
    return contentResolver.openInputStream(uri)?.use { stream ->
        PDDocument.load(stream).use { doc ->
            PDFTextStripper().getText(doc)
        }
    } ?: ""
}
```

### Good
```kotlin
suspend fun extractPdf(uri: Uri): String = withContext(Dispatchers.IO) {
    contentResolver.openInputStream(uri)?.use { stream ->
        PDDocument.load(stream).use { doc ->
            PDFTextStripper().getText(doc)
        }
    } ?: ""
}
```

---

## COROUTINE-004: Catch in Flow
- **Severity**: warning
- **Pattern**: `\.collect\s*\{[^}]*\}(?!.*\.catch)`
- **Message**: Flow collection should handle errors with catch operator.
- **Fix**: Add .catch { } operator before .collect { }.

### Bad
```kotlin
viewModelScope.launch {
    contentProcessor.extractContent(uri)
        .collect { result ->
            _uiState.update { it.copy(content = result) }
        }
}
```

### Good
```kotlin
viewModelScope.launch {
    contentProcessor.extractContent(uri)
        .catch { e -> _uiState.update { it.copy(error = e.message) } }
        .collect { result ->
            _uiState.update { it.copy(content = result) }
        }
}
```

---

## COROUTINE-005: SharedFlow for Events
- **Severity**: info
- **Pattern**: `sealed\s+(class|interface)\s+\w*Event[^{]*\{`
- **Message**: One-time events should use SharedFlow with replay=0, not StateFlow.
- **Fix**: Use MutableSharedFlow for events that should not be replayed.

### Bad
```kotlin
class DocuMindViewModel : ViewModel() {
    private val _events = MutableStateFlow<UiEvent?>(null)
    val events: StateFlow<UiEvent?> = _events.asStateFlow()
}
```

### Good
```kotlin
class DocuMindViewModel : ViewModel() {
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()
    
    fun sendEvent(event: UiEvent) {
        viewModelScope.launch { _events.emit(event) }
    }
}
```
