# Code Quality Rules

General code quality rules for DocuMind.

## QUALITY-001: Use Android Log
- **Severity**: error
- **Pattern**: `println\s*\(|print\s*\(`
- **Message**: Use android.util.Log instead of println/print for logging.
- **Fix**: Replace with Log.d(TAG, message) or appropriate log level.

### Bad
```kotlin
fun processContent(uri: Uri) {
    println("Processing: $uri")
}
```

### Good
```kotlin
fun processContent(uri: Uri) {
    Log.d(TAG, "Processing: $uri")
}

companion object {
    private const val TAG = "ContentProcessor"
}
```

---

## QUALITY-002: Null Safety
- **Severity**: warning
- **Pattern**: `!!\s*\.|as\s+\w+(?!\?)`
- **Message**: Avoid !! operator and unsafe casts. Use safe alternatives.
- **Fix**: Use ?.let { }, ?: return, or safe cast (as?).

### Bad
```kotlin
fun processUri(uri: Uri?) {
    val path = uri!!.path
    val file = path as String
}
```

### Good
```kotlin
fun processUri(uri: Uri?) {
    uri?.path?.let { path ->
        // Safe access
    } ?: return
}
```

---

## QUALITY-003: Naming Conventions
- **Severity**: warning
- **Pattern**: `const val [a-z]|fun [A-Z]|class [a-z]`
- **Message**: Follow naming conventions: camelCase functions, PascalCase classes, SCREAMING_SNAKE constants.
- **Fix**: Rename to follow conventions.

### Bad
```kotlin
const val maxRetries = 3
fun ProcessContent() { }
class contentProcessor { }
```

### Good
```kotlin
const val MAX_RETRIES = 3
fun processContent() { }
class ContentProcessor { }
```

---

## QUALITY-004: No God Classes
- **Severity**: warning
- **Pattern**: `class \w+ViewModel[^{]*\{`
- **Message**: ViewModels with many responsibilities should be split by feature.
- **Note**: This is a heuristic check. Manual review required for classes > 400 lines.
- **Fix**: Extract feature-specific logic into separate ViewModels or UseCases.

### Bad
```kotlin
// 800+ lines
class MainViewModel : ViewModel() {
    // Content processing
    // AI chat
    // File management
    // Settings
    // Analytics
}
```

### Good
```kotlin
class ContentViewModel : ViewModel() { /* Content processing */ }
class ChatViewModel : ViewModel() { /* AI chat */ }
class SettingsViewModel : ViewModel() { /* Settings */ }
```

---

## QUALITY-005: Immutable Data Classes
- **Severity**: info
- **Pattern**: `data class \w+\([^)]*var\s+\w+`
- **Message**: Prefer immutable (val) properties in data classes.
- **Fix**: Use val instead of var, create new instances for changes.

### Bad
```kotlin
data class ChatMessage(
    val id: String,
    var content: String,
    var isRead: Boolean
)
```

### Good
```kotlin
data class ChatMessage(
    val id: String,
    val content: String,
    val isRead: Boolean
) {
    fun markAsRead() = copy(isRead = true)
}
```
