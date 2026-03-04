# DocuMind - Production Release Checklist

A comprehensive guide to prepare DocuMind for Google Play Store release.

---

## Table of Contents

1. [Pre-Release Checklist](#pre-release-checklist)
2. [App Configuration](#app-configuration)
3. [Model Setup for Production](#model-setup-for-production)
4. [Build & Sign](#build--sign)
5. [Play Console Setup](#play-console-setup)
6. [Store Listing](#store-listing)
7. [Testing](#testing)
8. [Post-Launch](#post-launch)

---

## Pre-Release Checklist

### Code Quality

- [ ] Remove all debug logs (`Log.d`, `Log.v`)
- [ ] Remove hardcoded test strings
- [ ] Ensure no TODO comments in production code
- [ ] Run lint checks: `./gradlew lint`
- [ ] Fix all critical lint warnings

### Performance

- [ ] Test on low-end devices (2GB RAM)
- [ ] Profile memory usage with large documents
- [ ] Test LLM inference time (<5 seconds expected)
- [ ] Verify no memory leaks (LeakCanary)

### Security

- [ ] Enable ProGuard/R8 minification
- [ ] Verify no sensitive data in logs
- [ ] Remove debug build flags
- [ ] Audit third-party library permissions

---

## App Configuration

### 1. Update Version Info

Edit `app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        versionCode = 1          // Increment for each release
        versionName = "1.0.0"    // Semantic versioning
    }
}
```

### 2. Configure Release Build

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
        signingConfig = signingConfigs.getByName("release")
    }
}
```

### 3. Add ProGuard Rules

Create/update `app/proguard-rules.pro`:

```proguard
# MediaPipe
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# Apache POI
-dontwarn org.apache.poi.**
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.xmlbeans.**
-keep class org.apache.xmlbeans.** { *; }

# PDFBox
-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn org.bouncycastle.**

# Jsoup
-keep class org.jsoup.** { *; }

# Keep model classes
-keep class com.documind.app.domain.model.** { *; }
```

### 4. Update Manifest for Release

```xml
<application
    android:name=".DocuMindApp"
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@style/Theme.Documind"
    android:largeHeap="true"
    android:networkSecurityConfig="@xml/network_security_config">
```

---

## Model Setup for Production

### Step 1: Download Official Model

1. Go to [Kaggle Gemma Models](https://www.kaggle.com/models/google/gemma)
2. Download `gemma-3-1b-it` in MediaPipe `.task` format
3. Accept Google's license agreement

### Step 2: Place Model in Asset Pack

```bash
# Create directory if needed
mkdir -p model_pack/src/main/assets/

# Copy model file
cp ~/Downloads/gemma3-1b.task model_pack/src/main/assets/gemma3-1b.task
```

### Step 3: Verify Asset Pack Config

`model_pack/build.gradle.kts`:
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

### Step 4: Include in App

`app/build.gradle.kts`:
```kotlin
android {
    assetPacks += listOf(":model_pack")
}
```

---

## Build & Sign

### Step 1: Create Keystore (First Time Only)

```bash
keytool -genkey -v -keystore documind-release.jks \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -alias documind
```

**Important:** Store keystore securely. You cannot update your app without it!

### Step 2: Configure Signing

Option A: In `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("path/to/documind-release.jks")
            storePassword = "your-store-password"
            keyAlias = "documind"
            keyPassword = "your-key-password"
        }
    }
}
```

Option B: Using environment variables (recommended for CI):

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("KEYSTORE_FILE") ?: "release.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
        keyAlias = System.getenv("KEY_ALIAS") ?: ""
        keyPassword = System.getenv("KEY_PASSWORD") ?: ""
    }
}
```

### Step 3: Build AAB (Android App Bundle)

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

### Step 4: Test AAB Locally

```bash
# Install bundletool
brew install bundletool

# Generate APKs
bundletool build-apks --bundle=app-release.aab --output=app.apks \
    --ks=documind-release.jks --ks-key-alias=documind

# Install on connected device
bundletool install-apks --apks=app.apks
```

---

## Play Console Setup

### Step 1: Create Developer Account

1. Go to [Google Play Console](https://play.google.com/console)
2. Pay $25 one-time registration fee
3. Complete identity verification

### Step 2: Create App

1. Click "Create app"
2. Fill in:
   - App name: **DocuMind**
   - Default language: English
   - App type: App
   - Free or paid: Free

### Step 3: Complete App Content

Required sections:

| Section | Action |
|---------|--------|
| Privacy policy | Upload URL to privacy policy |
| App access | Select "All functionality available" |
| Ads | Declare no ads |
| Content ratings | Complete questionnaire |
| Target audience | 18+ (AI content) |
| News apps | Not a news app |
| COVID-19 apps | Not COVID-related |
| Data safety | Complete data practices form |
| Government apps | Not a government app |

### Step 4: Data Safety Declaration

For DocuMind, declare:

| Question | Answer |
|----------|--------|
| Does app collect user data? | No |
| Does app share data with third parties? | No |
| Is data encrypted in transit? | N/A (no network for docs) |
| Can users request data deletion? | N/A (no data stored) |

**Key message:** All processing happens on-device. No data leaves the user's phone.

---

## Store Listing

### App Details

```
App Name: DocuMind - AI Document Reader
Short Description (80 chars max):
Privacy-first AI assistant. Read and analyze documents offline.

Full Description (4000 chars max):
DocuMind is your personal AI reading assistant that works 100% offline.

📱 WORKS COMPLETELY OFFLINE
All document processing and AI analysis happens directly on your device. Your documents never leave your phone - guaranteed privacy.

📄 SUPPORTED FORMATS
• PDF documents
• Word documents (DOCX)
• Web pages (URL)
• Pasted text

🤖 INTELLIGENT Q&A
Ask questions about your documents and get instant answers powered by on-device AI (Gemma-3).

🔒 PRIVACY FIRST
• No cloud uploads
• No data collection
• No account required
• No internet needed (except URL extraction)

⚡ KEY FEATURES
• Extract text from PDF and Word files
• Analyze web articles offline
• Smart text chunking for large documents
• Beautiful, modern interface
• Dark mode support

Perfect for:
• Students reviewing study materials
• Professionals analyzing reports
• Researchers processing papers
• Anyone who values privacy

Download DocuMind and experience private, on-device AI document analysis.
```

### Graphics Assets

| Asset | Requirements |
|-------|--------------|
| App icon | 512x512 PNG |
| Feature graphic | 1024x500 PNG |
| Screenshots | Min 2, max 8 per device type |
| Phone screenshots | 16:9 or 9:16 aspect ratio |
| Tablet screenshots | 16:9 aspect ratio |

### Screenshot Suggestions

1. **Loading screen** - Show "Works 100% Offline" badge
2. **Home screen** - Document source selection
3. **PDF loaded** - Show word count, document info
4. **Chat in action** - Q&A conversation
5. **Privacy note** - "Your Privacy, Protected" section

---

## Testing

### Internal Testing Track

1. Upload AAB to Internal testing
2. Add testers by email
3. Share opt-in link
4. Collect feedback

### Closed Testing (Beta)

1. Create closed testing track
2. Set up tester groups
3. Gather crash reports
4. Monitor ANRs (App Not Responding)

### Pre-Launch Report

Google automatically tests on:
- Various device configurations
- Multiple Android versions
- Accessibility checks
- Security scans

Review reports before production release.

### Manual Test Cases

| Test Case | Expected |
|-----------|----------|
| Install from Play Store | Model downloads with app |
| Open PDF file | Text extracted, word count shown |
| Open DOCX file | Text extracted from paragraphs/tables |
| Load URL | Web content fetched and cleaned |
| Paste text | Text loaded immediately |
| Ask question | AI response within 5 seconds |
| Large document (>10K words) | Warning shown, works correctly |
| No internet | App works (except URL) |
| Kill and restart | State not persisted (expected) |

---

## Post-Launch

### Monitor Metrics

| Metric | Target |
|--------|--------|
| Crash-free rate | >99% |
| ANR rate | <0.5% |
| Install success rate | >95% |
| User rating | >4.0 stars |

### Respond to Reviews

- Respond to negative reviews within 24 hours
- Thank users for positive feedback
- Document common issues for future fixes

### Plan Updates

1. **v1.1** - Performance improvements
2. **v1.2** - Additional document formats
3. **v2.0** - Conversation history persistence

---

## Quick Commands Reference

```bash
# Build debug APK
./gradlew assembleDebug

# Build release AAB
./gradlew bundleRelease

# Run lint
./gradlew lint

# Run tests
./gradlew test

# Check dependencies
./gradlew dependencies

# Clean build
./gradlew clean
```

---

## Checklist Summary

### Before Upload

- [ ] Version code incremented
- [ ] Version name updated
- [ ] ProGuard enabled
- [ ] Model file in asset pack
- [ ] Keystore backed up securely
- [ ] AAB tested with bundletool

### Play Console

- [ ] App created
- [ ] Store listing complete
- [ ] Privacy policy uploaded
- [ ] Data safety declared
- [ ] Content rating received
- [ ] Screenshots uploaded
- [ ] Internal testing passed
- [ ] Pre-launch report reviewed

### Release

- [ ] AAB uploaded to production
- [ ] Release notes written
- [ ] Rollout started (suggest 10% → 50% → 100%)
- [ ] Monitoring dashboard checked

---

## Support

For issues or questions:
- Create GitHub issue
- Check MediaPipe documentation
- Review Play Console help

**Good luck with your release!** 🚀
