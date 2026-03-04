# DocuMind - Technical Specification

## 1. System Requirements

### Hardware Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| RAM | 4 GB | 8 GB+ |
| Storage | 1 GB free | 2 GB+ free |
| Processor | ARM64-v8a | ARM64-v8a with NPU |
| GPU | OpenGL ES 3.1 | Adreno 650+ / Mali-G78+ |

### Software Requirements

| Requirement | Version |
|-------------|---------|
| Android OS | 11 (API 30) minimum |
| Google Play Services | Latest |
| JDK (build) | 17+ |
| Gradle | 8.x |
| AGP | 9.0.1 |

---

## 2. Module Architecture

### app Module

```
com.android.application
├── compileSdk: 36
├── minSdk: 30
├── targetSdk: 36
├── multiDexEnabled: true
└── assetPacks: [":model_pack"]
```

### model_pack Module

```
com.android.asset-pack
├── packName: "model_pack"
├── deliveryType: "install-time"
└── assets/
    └── gemma3-1b.task (~500 MB)
```

---

## 3. Content Extraction Technical Details

### 3.1 PDF Extraction (PDFBox-Android)

**Library**: `com.tom-roush:pdfbox-android:2.0.26.0`

**Initialization** (Required once per app lifecycle):
```kotlin
class DocuMindApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}
```

**Extraction Process**:
```kotlin
class PdfExtractor : StreamContentExtractor {
    override suspend fun extractFromStream(inputStream: InputStream): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                PDDocument.load(inputStream).use { document ->
                    PDFTextStripper().getText(document)
                }
            }
        }
    }
}
```

**Memory Considerations**:
- PDFBox loads entire PDF into memory
- Large PDFs (100+ pages) may require `largeHeap`
- Sequential page extraction available but slower

### 3.2 DOCX Extraction (Apache POI)

**Library**: `org.apache.poi:poi-ooxml:5.2.3`

**Exclusions Required**:
```kotlin
packaging {
    resources {
        excludes += listOf(
            "META-INF/DEPENDENCIES",
            "META-INF/NOTICE",
            "META-INF/LICENSE",
            "META-INF/NOTICE.txt",
            "META-INF/LICENSE.txt",
            "META-INF/INDEX.LIST"
        )
    }
}
```

**Extraction Process**:
```kotlin
class DocxExtractor : StreamContentExtractor {
    override suspend fun extractFromStream(inputStream: InputStream): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                XWPFDocument(inputStream).use { document ->
                    buildString {
                        // Extract paragraphs
                        document.paragraphs.forEach { paragraph ->
                            appendLine(paragraph.text)
                        }
                        // Extract tables
                        document.tables.forEach { table ->
                            table.rows.forEach { row ->
                                row.tableCells.forEach { cell ->
                                    append(cell.text).append("\t")
                                }
                                appendLine()
                            }
                        }
                    }
                }
            }
        }
    }
}
```

**Extracted Elements**:
- Paragraphs (body text)
- Tables (row-by-row, tab-separated)
- Headers and footers (included in paragraphs)

**Not Extracted**:
- Images/embedded objects
- Comments/track changes
- Formatting metadata

### 3.3 URL Extraction (Jsoup)

**Library**: `org.jsoup:jsoup:1.17.2`

**Extraction Process**:
```kotlin
class UrlExtractor : ContentExtractor {
    override suspend fun extract(source: Any): Result<String> {
        val url = source as String
        return withContext(Dispatchers.IO) {
            runCatching {
                val document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Android)")
                    .timeout(30_000)
                    .get()
                
                // Remove non-content elements
                document.select("script, style, nav, footer, header, aside").remove()
                
                val title = document.title()
                val body = document.body().text()
                
                "$title\n\n$body"
            }
        }
    }
}
```

**Removed Elements**:
- `<script>` - JavaScript code
- `<style>` - CSS definitions
- `<nav>` - Navigation menus
- `<footer>` - Page footers
- `<header>` - Page headers
- `<aside>` - Sidebars

---

## 4. Text Processing Algorithms

### 4.1 Text Cleaning

```kotlin
fun cleanText(text: String): String {
    return text
        .replace(Regex("\\s+"), " ")           // Normalize whitespace
        .replace(Regex("[\\x00-\\x1F]"), "")   // Remove control characters
        .replace(Regex("\\p{C}"), "")          // Remove invisible chars
        .trim()
}
```

### 4.2 Chunking Algorithm

**Parameters**:
- `CHUNK_SIZE = 1000` characters
- `OVERLAP = 100` characters

**Algorithm**:
```kotlin
fun chunkText(text: String, chunkSize: Int = 1000, overlap: Int = 100): List<String> {
    val chunks = mutableListOf<String>()
    var startIndex = 0
    
    while (startIndex < text.length) {
        var endIndex = minOf(startIndex + chunkSize, text.length)
        
        // Find word boundary (don't cut mid-word)
        if (endIndex < text.length) {
            val lastSpace = text.lastIndexOf(' ', endIndex)
            if (lastSpace > startIndex) {
                endIndex = lastSpace
            }
        }
        
        chunks.add(text.substring(startIndex, endIndex).trim())
        startIndex = endIndex - overlap
        
        // Prevent infinite loop
        if (startIndex <= 0 || endIndex == text.length) break
    }
    
    return chunks
}
```

**Overlap Rationale**:
- Ensures sentences aren't cut mid-context
- 10% overlap balances redundancy vs. continuity
- Prevents loss of information at chunk boundaries

### 4.3 Context Building for LLM

```kotlin
fun buildContextForQuery(chunks: List<String>, maxLength: Int = 4000): String {
    val context = StringBuilder()
    
    for (chunk in chunks) {
        if (context.length + chunk.length > maxLength) break
        context.append(chunk).append("\n\n")
    }
    
    return context.toString()
}
```

**Limitations**:
- First-chunk bias (early content prioritized)
- No semantic relevance ranking
- Fixed 4000-char context window

---

## 5. On-Device LLM Integration

### 5.1 MediaPipe LlmInference

**Library**: `com.google.mediapipe:tasks-genai:0.10.32`

**Initialization**:
```kotlin
private suspend fun initialize(modelPath: String): Result<Unit> {
    return withContext(Dispatchers.IO) {
        runCatching {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(MAX_TOKENS)
                .build()
            
            llmInference = LlmInference.createFromOptions(context, options)
        }
    }
}
```

**Configuration Options**:

| Option | Value | Purpose |
|--------|-------|---------|
| `modelPath` | String | Path to .task model file |
| `maxTokens` | 1024 | Maximum response length |

**Note**: MediaPipe `tasks-genai:0.10.32` only exposes `setModelPath` and `setMaxTokens`. Methods like `setTemperature`, `setTopK`, and other sampling parameters are not available in the current API.

### 5.2 Inference Execution

```kotlin
suspend fun generateResponse(documentContext: String, userQuery: String): Result<String> {
    if (llmInference == null) {
        return Result.failure(IllegalStateException("LLM not initialized"))
    }
    
    val prompt = buildPrompt(documentContext, userQuery)
    
    return withContext(Dispatchers.IO) {
        runCatching {
            llmInference!!.generateResponse(prompt)
        }
    }
}

private fun buildPrompt(context: String, query: String): String {
    return """
        $SYSTEM_PROMPT
        
        --- DOCUMENT CONTEXT START ---
        $context
        --- DOCUMENT CONTEXT END ---
        
        User Question: $query
        
        Answer:
    """.trimIndent()
}
```

### 5.3 Model File Requirements

**Supported Format**: `.task` (MediaPipe model bundle)

**Model**: Gemma-3 1B (gemma3-1b.task)
- Size: ~500 MB
- Architecture: Decoder-only transformer
- Quantization: INT8 (recommended for mobile)

---

## 6. Play Asset Delivery

### 6.1 Asset Pack Configuration

**model_pack/build.gradle.kts**:
```kotlin
plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("model_pack")
    dynamicDelivery {
        deliveryType.set("install-time")
    }
}
```

**Delivery Types**:

| Type | Behavior |
|------|----------|
| `install-time` | Downloaded with APK install |
| `fast-follow` | Downloaded after APK install |
| `on-demand` | Downloaded when requested by app |

### 6.2 State Management

```kotlin
sealed class ModelState {
    data object Idle : ModelState()
    data class Downloading(val progress: Int) : ModelState()
    data object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}
```

**State Transitions**:
```
Idle → checkModelStatus()
  ├─→ Local file exists → Ready
  ├─→ Asset pack available → Downloading(progress) → Ready
  └─→ No model found → Error("Model not available")
```

### 6.3 Fallback Strategy

For development/sideloaded APKs:

```kotlin
private suspend fun checkLocalModel(): String? {
    // Priority 1: Internal files directory
    val internalPath = File(context.filesDir, MODEL_FILE_NAME)
    if (internalPath.exists()) return internalPath.absolutePath
    
    // Priority 2: External files directory
    val externalPath = File(context.getExternalFilesDir(null), MODEL_FILE_NAME)
    if (externalPath?.exists() == true) return externalPath.absolutePath
    
    // Priority 3: Bundled in app assets (copy to filesDir)
    return copyFromAssetsIfAvailable()
}
```

---

## 7. UI State Machine

### 7.1 Screen Navigation

```
┌─────────────────────────────────────────────────────────────┐
│                    LoadingScreen                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ ModelState.Idle/Downloading → Show progress bar     │    │
│  │ ModelState.Ready → Navigate to HomeScreen           │    │
│  │ ModelState.Error → Show error + "Continue" button   │    │
│  └─────────────────────────────────────────────────────┘    │
└──────────────────────────┬──────────────────────────────────┘
                           │ Model ready OR user skips
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                     HomeScreen                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Four source cards: PDF | DOCX | URL | Text          │    │
│  │ Card tap → File picker / Dialog                     │    │
│  │ Extraction success → Navigate to ChatScreen         │    │
│  └─────────────────────────────────────────────────────┘    │
└──────────────────────────┬──────────────────────────────────┘
                           │ Document extracted
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                     ChatScreen                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Document status bar (source + word count)           │    │
│  │ Message list (scrollable)                           │    │
│  │ Input field + Send button                           │    │
│  │ Clear button → Return to HomeScreen                 │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 7.2 State Definitions

```kotlin
// Screen navigation
sealed class UiScreen {
    data object Loading : UiScreen()
    data object Home : UiScreen()
    data object Chat : UiScreen()
}

// Content extraction
sealed class ExtractionState {
    data object Idle : ExtractionState()
    data object Extracting : ExtractionState()
    data class Success(val document: DocumentContent) : ExtractionState()
    data class Error(val message: String) : ExtractionState()
}

// LLM query processing
sealed class QueryState {
    data object Idle : QueryState()
    data object Processing : QueryState()
    data class Error(val message: String) : QueryState()
}
```

---

## 8. Threading Model

### 8.1 Dispatcher Usage

| Operation | Dispatcher | Reason |
|-----------|------------|--------|
| PDF/DOCX extraction | `Dispatchers.IO` | File I/O bound |
| URL fetching | `Dispatchers.IO` | Network I/O bound |
| Text processing | `Dispatchers.Default` | CPU bound |
| LLM initialization | `Dispatchers.IO` | File loading |
| LLM inference | `Dispatchers.IO` | Long-running computation |
| UI state updates | `Dispatchers.Main` | UI thread requirement |

### 8.2 Coroutine Scopes

```kotlin
class DocuMindViewModel : ViewModel() {
    // All coroutines cancelled when ViewModel cleared
    fun extractPdf(inputStream: InputStream, fileName: String) {
        viewModelScope.launch {
            // Automatic cancellation on ViewModel destruction
        }
    }
}
```

---

## 9. Data Models

### 9.1 DocumentContent

```kotlin
data class DocumentContent(
    val rawText: String,                    // Original extracted text
    val cleanedText: String,                // Processed text
    val chunks: List<String>,               // 1000-char segments
    val wordCount: Int,                     // For UI display
    val sourceType: SourceType,             // PDF/DOCX/URL/TEXT
    val sourceName: String,                 // File name or URL
    val extractedAt: Long = System.currentTimeMillis()
)
```

### 9.2 ChatMessage

```kotlin
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,                    // true=user, false=AI
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false          // Show typing indicator
)
```

---

## 10. Error Handling

### 10.1 Error Categories

| Category | Example | Handling |
|----------|---------|----------|
| Extraction | Corrupt PDF | Show error dialog, return to Home |
| Network | URL timeout | Show retry option |
| LLM | Model not loaded | Show "AI unavailable" message |
| Memory | OOM on large file | Suggest smaller file |

### 10.2 Result Pattern

All data layer operations return `Result<T>`:

```kotlin
// Usage in ViewModel
extractContentUseCase.invoke(source).fold(
    onSuccess = { document -> 
        _extractionState.value = ExtractionState.Success(document)
        _currentScreen.value = UiScreen.Chat
    },
    onFailure = { error ->
        _extractionState.value = ExtractionState.Error(error.message ?: "Unknown error")
    }
)
```

---

## 11. Memory Management

### 11.1 Large Heap Configuration

**AndroidManifest.xml**:
```xml
<application android:largeHeap="true" ...>
```

### 11.2 Resource Cleanup

```kotlin
class LocalLLMManager {
    private var llmInference: LlmInference? = null
    
    fun close() {
        llmInference?.close()
        llmInference = null
    }
}

// In ViewModel
override fun onCleared() {
    super.onCleared()
    llmManager.close()
}
```

### 11.3 Document Size Limits

| Metric | Threshold | Action |
|--------|-----------|--------|
| Word count | >10,000 | Show warning banner |
| Chunk count | >100 | Use first 50 chunks |
| Context size | >4,000 chars | Truncate to limit |

---

## 12. Security Considerations

### 12.1 Permissions

```xml
<!-- Required for URL fetching -->
<uses-permission android:name="android.permission.INTERNET"/>

<!-- Required for file access on older devices -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32"/>
```

### 12.2 Data Privacy

- **No cloud processing**: All LLM inference on-device
- **No data persistence**: Documents not stored after session
- **No analytics**: No user tracking implemented
- **Scoped storage**: Uses ContentResolver, no broad file access

---

## 13. Build Variants

### Debug Configuration

```kotlin
buildTypes {
    debug {
        isMinifyEnabled = false
        isDebuggable = true
    }
}
```

### Release Configuration

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### ProGuard Rules (if enabled)

```proguard
# MediaPipe
-keep class com.google.mediapipe.** { *; }

# Apache POI
-dontwarn org.apache.poi.**
-keep class org.apache.poi.** { *; }

# PDFBox
-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.pdfbox.** { *; }
```

---

## 14. Testing Strategy

### Unit Tests

| Component | Framework | Focus |
|-----------|-----------|-------|
| TextProcessor | JUnit | Chunking, cleaning algorithms |
| Extractors | JUnit + Mockito | Stream handling, error cases |
| ViewModel | JUnit + Turbine | State transitions |

### Instrumentation Tests

| Component | Framework | Focus |
|-----------|-----------|-------|
| UI Screens | Espresso + Compose | Navigation, interactions |
| File Pickers | UI Automator | System picker integration |

### Manual Testing

- [ ] PDF extraction (multi-page, scanned, encrypted)
- [ ] DOCX extraction (tables, headers, complex formatting)
- [ ] URL extraction (JS-heavy sites, paywalls)
- [ ] LLM responses (accuracy, relevance, hallucination)
- [ ] Memory under stress (large documents)
- [ ] Device compatibility (various Android versions)

---

## Appendix A: API Reference

### ContentExtractor

```kotlin
interface ContentExtractor {
    val sourceType: SourceType
    suspend fun extract(source: Any): Result<String>
}

interface StreamContentExtractor : ContentExtractor {
    suspend fun extractFromStream(inputStream: InputStream): Result<String>
}
```

### LocalLLMManager

```kotlin
class LocalLLMManager(context: Context) {
    suspend fun initialize(modelPath: String): Result<Unit>
    suspend fun generateResponse(documentContext: String, userQuery: String): Result<String>
    fun isReady(): Boolean
    fun close()
}
```

### ModelStatusManager

```kotlin
class ModelStatusManager(context: Context) {
    val modelState: StateFlow<ModelState>
    suspend fun checkModelStatus()
    fun getModelPath(): String?
}
```

### TextProcessor

```kotlin
class TextProcessor {
    fun cleanText(text: String): String
    fun countWords(text: String): Int
    fun chunkText(text: String, chunkSize: Int = 1000, overlap: Int = 100): List<String>
    fun isLargeDocument(text: String): Boolean
    fun buildContextForQuery(chunks: List<String>, maxLength: Int = 4000): String
}
```

---

## Appendix B: Version History

| Version | Date | Changes |
|---------|------|---------|
| 0.1.0 | 2026-03 | Initial POC implementation |
