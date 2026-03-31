# Architecture Rules

Rules enforcing Clean Architecture + MVVM patterns for DocuMind.

## ARCH-001: Repository Single Source of Truth
- **Severity**: error
- **Pattern**: `class \w+ViewModel.*\{[^}]*(?:retrofit|okhttp|room|dao|api)[^}]*\}`
- **Message**: ViewModel should not directly access data sources. Use Repository pattern.
- **Fix**: Inject Repository into ViewModel instead of direct data source access.

### Bad
```kotlin
class DocuMindViewModel(
    private val pdfDao: PdfDao,
    private val apiService: ApiService
) : ViewModel()
```

### Good
```kotlin
class DocuMindViewModel(
    private val contentRepository: ContentRepository
) : ViewModel()
```

---

## ARCH-002: UseCase Single Responsibility
- **Severity**: warning
- **Pattern**: `class \w+UseCase[^{]*\{[^}]*fun \w+[^}]*fun \w+[^}]*fun \w+`
- **Message**: UseCase should have only one public function. Split into multiple UseCases.
- **Fix**: Create separate UseCase classes for each operation.

### Bad
```kotlin
class ContentUseCase {
    fun extractPdf(uri: Uri): Result<String> { }
    fun extractDocx(uri: Uri): Result<String> { }
    fun extractWeb(url: String): Result<String> { }
}
```

### Good
```kotlin
class ExtractPdfUseCase {
    operator fun invoke(uri: Uri): Result<String> { }
}

class ExtractDocxUseCase {
    operator fun invoke(uri: Uri): Result<String> { }
}
```

---

## ARCH-003: Business Logic in UseCase
- **Severity**: warning
- **Pattern**: `class \w+ViewModel[^{]*\{[^}]*(if|when|for|while)[^}]*(if|when|for|while)[^}]*(if|when|for|while)`
- **Message**: Complex business logic should be in UseCase, not ViewModel.
- **Fix**: Extract business logic into dedicated UseCase classes.

### Bad
```kotlin
class DocuMindViewModel : ViewModel() {
    fun processContent(uri: Uri) {
        if (isPdf(uri)) {
            val text = extractPdf(uri)
            if (text.length > MAX_LENGTH) {
                text = chunk(text)
            }
            // More logic...
        }
    }
}
```

### Good
```kotlin
class DocuMindViewModel(
    private val processContentUseCase: ProcessContentUseCase
) : ViewModel() {
    fun processContent(uri: Uri) {
        viewModelScope.launch {
            processContentUseCase(uri).collect { result ->
                _uiState.update { it.copy(content = result) }
            }
        }
    }
}
```

---

## ARCH-004: Layer Separation
- **Severity**: error
- **Pattern**: `package com\.documind\.app\.ui[^;]*import com\.documind\.app\.data\.(?!model)`
- **Message**: UI layer should not import from data layer directly. Use domain layer.
- **Fix**: Access data through domain models and use cases.

### Bad
```kotlin
package com.documind.app.ui.screens

import com.documind.app.data.extractor.PdfExtractor
```

### Good
```kotlin
package com.documind.app.ui.screens

import com.documind.app.domain.usecase.ExtractContentUseCase
import com.documind.app.domain.model.ContentItem
```

---

## ARCH-005: ViewModel State Management
- **Severity**: error
- **Pattern**: `class \w+ViewModel[^{]*\{[^}]*var \w+\s*:\s*\w+\s*=`
- **Message**: ViewModel state should use StateFlow/SharedFlow, not mutable vars.
- **Fix**: Use private MutableStateFlow with public StateFlow exposure.

### Bad
```kotlin
class DocuMindViewModel : ViewModel() {
    var isLoading: Boolean = false
    var content: String = ""
}
```

### Good
```kotlin
class DocuMindViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
}
```
