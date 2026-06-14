# DocuMind - AI Reading Assistant

An Android app that lets you chat with your documents using on-device AI. All processing happens locally on your phone - your documents never leave your device.

[![Get it on Google Play](https://img.shields.io/badge/Google%20Play-DocuMind-414141?logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.documind.app)

> **AI Mobile Hackathon 2026 — Top 50** · Stage 3 submitted  
> [Pitch deck](https://docs.google.com/presentation/d/1XS6u6dGVxh_ym1Pf9bK-s628i8rEEdEI7Mpm7j_mQw8/edit?usp=sharing) · [Demo video](https://youtube.com/shorts/Yzo5NK-9MUs) · Top 20 announcement: 18 June 2026

## Features

- **On-Device AI**: Uses Gemma 1B model via MediaPipe for intelligent Q&A
- **100% Offline**: Works without internet after initial model download
- **Privacy First**: Documents are processed locally, never uploaded
- **Multiple Sources**: Supports PDF, Word (DOCX), URLs, and pasted text
- **Smart Extraction**: Extracts and processes text from various document formats

## Screenshots

| Onboarding | Home | Paste text |
|------------|------|------------|
| ![Onboarding — Privacy First](docs/screenshots/01-onboarding-privacy.jpg) | ![Home — AI Ready](docs/screenshots/02-home-ready.jpeg) | ![Paste text import](docs/screenshots/03-home-paste-text.jpeg) |

| Indexing (on-device) | Chat Q&A |
|----------------------|----------|
| ![Preparing on-device AI](docs/screenshots/04-chat-indexing.jpeg) | ![Ask key points — on-device answer](docs/screenshots/05-chat-qa.jpeg) |

## Demo video

**Watch:** [DocuMind demo — YouTube Shorts](https://youtube.com/shorts/Yzo5NK-9MUs)

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture
- **AI**: MediaPipe Tasks GenAI (Gemma 1B)
- **Model Delivery**: Google Play Asset Delivery (fast-follow)
- **Document Parsing**:
  - PDF: PDFBox Android
  - Word: Apache POI
  - URL: Jsoup
- **Analytics**: Firebase Analytics + Crashlytics
- **Updates**: In-App Update API

## Project Structure

```
Documind/
├── app/
│   └── src/main/java/com/documind/app/
│       ├── data/
│       │   ├── analytics/      # Firebase Analytics
│       │   ├── extractor/      # PDF, DOCX, URL extractors
│       │   ├── fcm/            # Push notifications
│       │   ├── llm/            # LLM management
│       │   ├── processor/      # Text processing
│       │   └── update/         # In-app updates
│       ├── domain/
│       │   ├── model/          # Data models
│       │   └── usecase/        # Business logic
│       └── ui/
│           ├── components/     # Reusable UI components
│           ├── screens/        # App screens
│           ├── theme/          # Material theme
│           └── viewmodel/      # ViewModels
├── model_pack/                 # Asset pack for AI model
│   └── src/main/assets/
│       └── gemma3-1b-it-int4.task  # AI model (not in git, download from Kaggle)
└── gradle/
    └── libs.versions.toml      # Dependency versions
```

## Setup

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 21
- Android SDK 36

### Build

1. Clone the repository:
```bash
git clone git@github.com:ramakrishna-sunkara/Documind.git
cd Documind
```

2. Download AI models (~670 MB total — not in git due to GitHub size limits):
   - Get `gemma3-1b-it-int4.task` (~555 MB) from [Kaggle Gemma 3](https://www.kaggle.com/models/google/gemma-3/tfLite)
   - Run `./scripts/download_rag_models.sh` for Gecko embedder + tokenizer
   - Place all files in `model_pack/src/main/assets/` (see `model_pack/src/main/assets/README.md`)

3. Add Firebase config:
   - Create a Firebase project
   - Download `google-services.json`
   - Place it in `app/`

4. Build:
```bash
./gradlew :app:assembleFreeDebug
```

### Release Build

```bash
./gradlew :app:bundleFreeRelease
```

The AAB will be at: `app/build/outputs/bundle/freeRelease/app-free-release.aab`

## Model Delivery

AI models (~670 MB) are delivered via Google Play Asset Delivery:

- **Delivery Type**: `fast-follow` — downloads automatically after app install from Play Store
- **Fallback**: Users can manually trigger download from the Home screen
- **Offline**: Once downloaded, works completely offline

### For reviewers

Models are not committed to this repo (GitHub 100 MB file limit). To verify the app without building from source:

1. **Install from [Google Play](https://play.google.com/store/apps/details?id=com.documind.app)** — recommended
2. **Watch the [demo video](https://youtube.com/shorts/Yzo5NK-9MUs)**
3. **Read the [pitch deck](https://docs.google.com/presentation/d/1XS6u6dGVxh_ym1Pf9bK-s628i8rEEdEI7Mpm7j_mQw8/edit?usp=sharing)**

To build locally, follow **Setup** above and install via Play Internal Testing or a release AAB with asset packs.

## Privacy

- All document processing happens on-device
- No documents or queries are sent to external servers
- Only anonymous analytics are collected (can be disabled)
- See [Privacy Policy](PRIVACY_POLICY.md)

## License

Copyright © 2024 Ram Apps. All rights reserved.

## Links

| | |
|---|---|
| **Google Play** | https://play.google.com/store/apps/details?id=com.documind.app |
| **Demo video** | https://youtube.com/shorts/Yzo5NK-9MUs |
| **Pitch deck** | https://docs.google.com/presentation/d/1XS6u6dGVxh_ym1Pf9bK-s628i8rEEdEI7Mpm7j_mQw8/edit?usp=sharing |
| **Hackathon** | [AI Mobile Hackathon 2026](https://aimobilehackathon.com) · Top 50 shortlist |

## Support

- Buy Me a Coffee: https://buymeacoffee.com/ramandroid
