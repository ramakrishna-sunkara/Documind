# Error Handling Rules

Rules enforcing proper error handling patterns for DocuMind.

## ERROR-001: Use Result Wrapper
- **Severity**: warning
- **Pattern**: `suspend fun \w+\([^)]*\)\s*:\s*(?!Result<|Flow<)`
- **Message**: Suspend functions performing I/O should return Result<T> for error handling.
- **Fix**: Wrap return type in Result<T> and use runCatching.

### Bad
```kotlin
suspend fun extractPdf(uri: Uri): String {
    return contentResolver.openInputStream(uri)?.use { stream ->
        PDDocument.load(stream).use { doc ->
            PDFTextStripper().getText(doc)
        }
    } ?: throw IOException("Cannot open file")
}
```

### Good
```kotlin
suspend fun extractPdf(uri: Uri): Result<String> = runCatching {
    withContext(Dispatchers.IO) {
        contentResolver.openInputStream(uri)?.use { stream ->
            PDDocument.load(stream).use { doc ->
                PDFTextStripper().getText(doc)
            }
        } ?: throw IOException("Cannot open file")
    }
}
```

---

## ERROR-002: No Silent Exception Swallowing
- **Severity**: error
- **Pattern**: `catch\s*\([^)]*\)\s*\{\s*\}`
- **Message**: Exceptions must not be silently swallowed. Log or handle them.
- **Fix**: Add logging or proper error handling in catch block.

### Bad
```kotlin
try {
    processContent(uri)
} catch (e: Exception) {
    // Silently ignored
}
```

### Good
```kotlin
try {
    processContent(uri)
} catch (e: Exception) {
    Log.e(TAG, "Failed to process content: ${e.message}", e)
    _uiState.update { it.copy(error = "Failed to process content") }
}
```

---

## ERROR-003: User-Friendly Error Messages
- **Severity**: warning
- **Pattern**: `UiState\.\w+\([^)]*error\s*=\s*\w+\.message`
- **Message**: Don't expose raw exception messages to users. Use user-friendly messages.
- **Fix**: Map technical errors to user-friendly messages.

### Bad
```kotlin
_uiState.update { 
    it.copy(error = exception.message) 
}
```

### Good
```kotlin
_uiState.update { 
    it.copy(error = mapErrorToUserMessage(exception)) 
}

private fun mapErrorToUserMessage(e: Throwable): String = when (e) {
    is IOException -> "Unable to read file. Please try again."
    is OutOfMemoryError -> "File is too large to process."
    else -> "Something went wrong. Please try again."
}
```
