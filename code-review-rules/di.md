# Dependency Injection Rules

Rules enforcing Hilt DI best practices for DocuMind.

## DI-001: Constructor Injection
- **Severity**: error
- **Pattern**: `@Inject\s+lateinit\s+var`
- **Message**: Prefer constructor injection over field injection.
- **Fix**: Move dependencies to constructor parameters.

### Bad
```kotlin
@HiltViewModel
class DocuMindViewModel : ViewModel() {
    @Inject lateinit var llmManager: LlmManager
    @Inject lateinit var contentProcessor: ContentProcessor
}
```

### Good
```kotlin
@HiltViewModel
class DocuMindViewModel @Inject constructor(
    private val llmManager: LlmManager,
    private val contentProcessor: ContentProcessor
) : ViewModel()
```

---

## DI-002: Singleton for Managers
- **Severity**: warning
- **Pattern**: `class \w+Manager[^@]*@Inject\s+constructor(?!.*@Singleton)`
- **Message**: Manager classes should typically be @Singleton scoped.
- **Fix**: Add @Singleton annotation to manager class.

### Bad
```kotlin
class LlmManager @Inject constructor(
    private val modelStatusManager: ModelStatusManager
) {
    // Heavy initialization
}
```

### Good
```kotlin
@Singleton
class LlmManager @Inject constructor(
    private val modelStatusManager: ModelStatusManager
) {
    // Heavy initialization happens once
}
```

---

## DI-003: Application Context
- **Severity**: warning
- **Pattern**: `@Inject\s+constructor\([^)]*context:\s*Context(?!.*@ApplicationContext)`
- **Message**: Inject @ApplicationContext when Activity context is not specifically needed.
- **Fix**: Use @ApplicationContext annotation for Context parameter.

### Bad
```kotlin
@Singleton
class FileManager @Inject constructor(
    private val context: Context
) {
    fun getFilesDir() = context.filesDir
}
```

### Good
```kotlin
@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getFilesDir() = context.filesDir
}
```
