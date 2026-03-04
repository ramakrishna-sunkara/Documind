# DocuMind - Universal AI Reading Assistant

A privacy-focused Android application that extracts text from documents (PDF, DOCX, URL, Text) and enables intelligent Q&A using an on-device Large Language Model (Gemma-3 1B) via MediaPipe.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Key Components](#key-components)
- [Setup & Installation](#setup--installation)
- [Model Setup](#model-setup)
- [How It Works](#how-it-works)
- [Dependencies](#dependencies)
- [Troubleshooting](#troubleshooting)

---

## Overview

**DocuMind** is a proof-of-concept Android application demonstrating:

- **Multi-format Document Extraction**: PDF, DOCX, Web URLs, and plain text
- **On-Device AI Processing**: No cloud dependency, complete privacy
- **Modern Android Architecture**: MVVM + Clean Architecture with Jetpack Compose
- **Play Asset Delivery**: Large model file distribution via Google Play

### Key Features

| Feature | Description |
|---------|-------------|
| PDF Extraction | Extract text from PDF files using PDFBox-Android |
| Word Document Support | Parse DOCX files using Apache POI |
| URL Content Extraction | Scrape and clean web page content using Jsoup |
| Text Paste | Direct text input support |
| On-Device LLM | Gemma-3 1B model via MediaPipe LlmInference |
| Privacy-First | All processing happens locally on device |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        UI Layer (Compose)                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │LoadingScreen│  │ HomeScreen  │  │      ChatScreen         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      ViewModel Layer                             │
│                    ┌──────────────────┐                          │
│                    │ DocuMindViewModel│                          │
│                    └──────────────────┘                          │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Domain Layer                               │
│  ┌────────────────────┐       ┌─────────────────────┐           │
│  │ExtractContentUseCase│       │ ProcessQueryUseCase │           │
│  └────────────────────┘       └─────────────────────┘           │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Data Layer                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ Extractors  │  │TextProcessor│  │     LLM Components      │  │
│  │ PDF/DOCX/URL│  │  Chunking   │  │LocalLLMManager/ModelMgr │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Data Flow

```
User Input → Extractor → TextProcessor → Chunks → LLM → Response
     │            │            │           │        │        │
     │            │            │           │        │        └── AI-generated answer
     │            │            │           │        └── MediaPipe LlmInference
     │            │            │           └── 1000-char segments with 100-char overlap
     │            │            └── Clean & normalize text
     │            └── PDF/DOCX/URL/Text extraction
     └── File picker / URL input / Text paste
```

---

## Tech Stack

| Category | Technology | Version |
|----------|------------|---------|
| Language | Kotlin | 2.0.21 |
| UI Framework | Jetpack Compose | BOM 2024.09.00 |
| Architecture | MVVM + Clean Architecture | - |
| AI Runtime | MediaPipe Tasks GenAI | 0.10.32 |
| LLM Model | Gemma-3 1B | .task format |
| PDF Parsing | PDFBox-Android | 2.0.26.0 |
| DOCX Parsing | Apache POI | 5.2.3 |
| Web Scraping | Jsoup | 1.17.2 |
| Asset Delivery | Play Asset Delivery | 2.1.0 |
| Build System | Gradle (Kotlin DSL) | 8.x |
| Min SDK | 30 (Android 11) | - |
| Target SDK | 36 | - |

---

## Project Structure

```
Documind/
├── app/
│   ├── build.gradle.kts                 # App module configuration
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/                      # Local model storage (development)
│       └── java/com/documind/app/
│           ├── DocuMindApp.kt           # Application class
│           ├── MainActivity.kt          # Single activity entry point
│           │
│           ├── data/                    # Data Layer
│           │   ├── extractor/
│           │   │   ├── ContentExtractor.kt    # Interface & enums
│           │   │   ├── PdfExtractor.kt        # PDFBox implementation
│           │   │   ├── DocxExtractor.kt       # Apache POI implementation
│           │   │   └── UrlExtractor.kt        # Jsoup implementation
│           │   ├── processor/
│           │   │   └── TextProcessor.kt       # Text cleaning & chunking
│           │   └── llm/
│           │       ├── LocalLLMManager.kt     # MediaPipe LLM wrapper
│           │       └── ModelStatusManager.kt  # Asset pack state management
│           │
│           ├── domain/                  # Domain Layer
│           │   ├── model/
│           │   │   ├── DocumentContent.kt     # Document data model
│           │   │   ├── ChatMessage.kt         # Chat message model
│           │   │   └── UiState.kt             # UI state definitions
│           │   └── usecase/
│           │       ├── ExtractContentUseCase.kt
│           │       └── ProcessQueryUseCase.kt
│           │
│           ├── ui/                      # UI Layer
│           │   ├── screens/
│           │   │   ├── LoadingScreen.kt       # Model loading UI
│           │   │   ├── HomeScreen.kt          # Source selection
│           │   │   └── ChatScreen.kt          # Q&A interface
│           │   ├── components/
│           │   │   ├── SourceCard.kt          # Clickable source cards
│           │   │   ├── MessageBubble.kt       # Chat bubbles
│           │   │   └── DocumentStatusBar.kt   # Document info bar
│           │   ├── viewmodel/
│           │   │   └── DocuMindViewModel.kt   # Main ViewModel
│           │   └── theme/
│           │       ├── Color.kt
│           │       ├── Theme.kt
│           │       └── Type.kt
│           │
│           └── util/
│               └── Constants.kt
│
├── model_pack/                          # Asset Pack Module
│   ├── build.gradle.kts                 # Asset pack configuration
│   └── src/main/assets/
│       └── gemma3-1b.task              # LLM model file (user provided)
│
├── gradle/
│   └── libs.versions.toml              # Version catalog
│
├── build.gradle.kts                    # Project-level build
├── settings.gradle.kts                 # Module includes
└── README.md                           # This file
```

---

## Key Components

### 1. Content Extractors

**Interface Definition:**
```kotlin
interface ContentExtractor {
    val sourceType: SourceType
    suspend fun extract(source: Any): Result<String>
}
```

| Extractor | Library | Key Method |
|-----------|---------|------------|
| `PdfExtractor` | PDFBox-Android | `PDFTextStripper().getText(document)` |
| `DocxExtractor` | Apache POI | `XWPFDocument.paragraphs.joinToString()` |
| `UrlExtractor` | Jsoup | `Jsoup.connect(url).get().body().text()` |

### 2. TextProcessor

Handles text normalization and chunking for LLM context windows:

```kotlin
class TextProcessor {
    fun cleanText(text: String): String      // Remove special chars, normalize whitespace
    fun countWords(text: String): Int        // Word count for UI display
    fun chunkText(text: String): List<String> // 1000-char chunks, 100-char overlap
    fun isLargeDocument(text: String): Boolean // >10,000 words warning
}
```

**Chunking Strategy:**
- Chunk size: 1000 characters
- Overlap: 100 characters (ensures context continuity)
- Word boundary aware (doesn't cut mid-word)

### 3. LocalLLMManager

Wrapper for MediaPipe's LlmInference:

```kotlin
class LocalLLMManager(context: Context) {
    suspend fun initialize(modelPath: String): Result<Unit>
    suspend fun generateResponse(context: String, query: String): Result<String>
    fun isReady(): Boolean
    fun close()
}
```

**LLM Configuration:**
```kotlin
LlmInference.LlmInferenceOptions.builder()
    .setModelPath(modelPath)
    .setMaxTokens(1024)
    .build()
```

> **Note:** MediaPipe `tasks-genai:0.10.32` only exposes `setModelPath` and `setMaxTokens`. Methods like `setTemperature` and `setTopK` are not available in the current API.

**System Prompt:**
```
You are DocuMind, a helpful AI assistant for document analysis.
Your responses must be:
1. Based ONLY on the provided document context
2. Concise and to the point
3. If information is not found in the context, say "Information not found in the document"
4. No speculation or external knowledge
```

### 4. ModelStatusManager

Manages LLM model availability with multiple fallback paths:

```kotlin
sealed class ModelState {
    data object Idle : ModelState()
    data class Downloading(val progress: Int) : ModelState()
    data object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}
```

**Model Resolution Order:**
1. `{app.filesDir}/gemma3-1b.task` (manual copy)
2. `{externalFilesDir}/gemma3-1b.task` (adb push)
3. `{app.assets}/gemma3-1b.task` (bundled in APK)
4. Play Asset Delivery (production)

### 5. DocuMindViewModel

Central state management:

```kotlin
class DocuMindViewModel : ViewModel() {
    val currentScreen: StateFlow<UiScreen>      // Loading/Home/Chat
    val modelState: StateFlow<ModelState>       // LLM status
    val extractionState: StateFlow<ExtractionState>
    val messages: StateFlow<List<ChatMessage>>
    val currentDocument: StateFlow<DocumentContent?>
    
    fun extractPdf(inputStream: InputStream, fileName: String)
    fun extractDocx(inputStream: InputStream, fileName: String)
    fun extractUrl(url: String)
    fun extractText(text: String)
    fun sendQuery(query: String)
    fun clearDocument()
}
```

---

## Setup & Installation

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Android SDK 30+
- Physical Android device (emulators don't support MediaPipe LLM)

### Build Steps

1. **Clone/Open the project** in Android Studio

2. **Sync Gradle**:
   ```bash
   ./gradlew --refresh-dependencies
   ```

3. **Build the APK**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on device**:
   ```bash
   ./gradlew installDebug
   ```

---

## Model Setup

### Download the Model

Download Gemma-3 1B in MediaPipe `.task` format from:
- [Kaggle - Google Gemma](https://www.kaggle.com/models/google/gemma)
- [HuggingFace LiteRT Community](https://huggingface.co/litert-community)

### Local Development Setup

**Option A: ADB Push (Recommended)**

```bash
# Push model to device
adb push gemma3-1b.task /data/local/tmp/

# Copy to app's files directory
adb shell run-as com.documind.app cp /data/local/tmp/gemma3-1b.task /data/data/com.documind.app/files/
```

**Option B: Bundle in APK (Slow builds, 500MB+ APK)**

```bash
# Copy to app assets
cp gemma3-1b.task app/src/main/assets/
```

### Production Setup (Play Store)

1. Place model in `model_pack/src/main/assets/gemma3-1b.task`
2. Build Android App Bundle (AAB)
3. Upload to Play Console
4. Play Asset Delivery handles model distribution

---

## How It Works

### Document Processing Flow

```
1. USER SELECTS SOURCE
   └─→ PDF: File picker → ContentResolver → InputStream
   └─→ DOCX: File picker → ContentResolver → InputStream
   └─→ URL: Text input → String URL
   └─→ Text: Text input → String content

2. EXTRACTION
   └─→ PdfExtractor: PDFBox loads PDF → PDFTextStripper extracts text
   └─→ DocxExtractor: POI loads DOCX → Iterate paragraphs & tables
   └─→ UrlExtractor: Jsoup fetches URL → Remove scripts/nav → Get body text

3. TEXT PROCESSING
   └─→ Clean whitespace and special characters
   └─→ Count words for UI display
   └─→ Chunk into 1000-char segments with 100-char overlap
   └─→ Flag if document exceeds 10,000 words

4. DOCUMENT READY
   └─→ Store DocumentContent in ViewModel
   └─→ Navigate to Chat screen
   └─→ Display word count and source info
```

### Query Processing Flow

```
1. USER ENTERS QUESTION
   └─→ Add user message to chat list
   └─→ Show loading indicator

2. CONTEXT BUILDING
   └─→ Select relevant chunks (up to 4000 chars)
   └─→ Build prompt with system instructions + context + query

3. LLM INFERENCE
   └─→ MediaPipe LlmInference.generateResponse()
   └─→ Model processes on-device (GPU/NPU accelerated)

4. RESPONSE DISPLAY
   └─→ Replace loading with AI response
   └─→ Update chat list state
```

### Prompt Template

```
You are DocuMind, a helpful AI assistant for document analysis.
Your responses must be:
1. Based ONLY on the provided document context
2. Concise and to the point
3. If information is not found in the context, say "Information not found in the document"
4. No speculation or external knowledge

Always prioritize accuracy over completeness.

--- DOCUMENT CONTEXT START ---
{chunked_document_text}
--- DOCUMENT CONTEXT END ---

User Question: {user_query}

Answer:
```

---

## Dependencies

### Version Catalog (libs.versions.toml)

```toml
[versions]
mediapipeGenai = "0.10.32"
playAssetDelivery = "2.1.0"
pdfboxAndroid = "2.0.26.0"
poiOoxml = "5.2.3"
jsoup = "1.17.2"

[libraries]
mediapipe-tasks-genai = { group = "com.google.mediapipe", name = "tasks-genai" }
play-asset-delivery = { group = "com.google.android.play", name = "asset-delivery-ktx" }
pdfbox-android = { group = "com.tom-roush", name = "pdfbox-android" }
poi-ooxml = { group = "org.apache.poi", name = "poi-ooxml" }
jsoup = { group = "org.jsoup", name = "jsoup" }
```

### Packaging Exclusions

Required to avoid META-INF conflicts from Apache POI:

```kotlin
packaging {
    resources {
        excludes += "META-INF/DEPENDENCIES"
        excludes += "META-INF/NOTICE"
        excludes += "META-INF/LICENSE"
        excludes += "META-INF/NOTICE.txt"
        excludes += "META-INF/LICENSE.txt"
        excludes += "META-INF/INDEX.LIST"
    }
}
```

---

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| "API_NOT_AVAILABLE" (-5) | Play Asset Delivery only works from Play Store | Use ADB push or bundle in app assets |
| Model not found | Model file not in expected location | Follow [Model Setup](#model-setup) instructions |
| OutOfMemoryError | Large document + LLM memory | Reduce chunk size, enable `android:largeHeap="true"` |
| PDF extraction fails | PDFBox not initialized | Ensure `PDFBoxResourceLoader.init(context)` in Application |
| DOCX parsing slow | Apache POI overhead | Expected for large documents |
| Emulator crash | MediaPipe LLM not supported | Use physical device only |

### Logging

Enable debug logging:

```kotlin
// In ModelStatusManager
Log.d("ModelStatusManager", "Model path: $modelPath")

// In LocalLLMManager  
Log.d("LocalLLMManager", "Initializing LLM...")
```

### Memory Optimization

For large documents:
- Documents >10,000 words show warning
- Only first N chunks sent to LLM (maxContextLength = 4000 chars)
- Process chunks sequentially, not in parallel

---

## License

This project is for educational/POC purposes. See individual library licenses:
- MediaPipe: Apache 2.0
- PDFBox: Apache 2.0
- Apache POI: Apache 2.0
- Jsoup: MIT

---

## Future Enhancements

- [ ] Semantic search for relevant chunk selection
- [ ] Support for more document formats (EPUB, TXT, RTF)
- [ ] Conversation history persistence
- [ ] Multiple document comparison
- [ ] Export chat/summaries
- [ ] Larger model support (Gemma-2 2B)
