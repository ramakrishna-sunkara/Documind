plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.documind.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.documind.app"
        minSdk = 30
        targetSdk = 36
        versionCode = 32
        versionName = "1.0.32"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
        
        // Build config fields
        buildConfigField("String", "VERSION_DISPLAY", "\"1.0.29\"")
        buildConfigField("Boolean", "ENABLE_ANALYTICS", "true")
    }
    
    // Signing configurations
    signingConfigs {
        create("release") {
            // For CI/CD: Use environment variables
            //storeFile = file(System.getenv("KEYSTORE_FILE") ?: "release.jks")
            //storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            //keyAlias = System.getenv("KEY_ALIAS") ?: ""
            //keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            
            // For local development: Create your keystore and update paths
            storeFile = file("../keystore/documind-release.jks")
            storePassword = "Harshini@123"
            keyAlias = "documind"
            keyPassword = "Harshini@123"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            //applicationIdSuffix = ".debug"
            //versionNameSuffix = "-debug"
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Uncomment when keystore is configured
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    // Build variants
    flavorDimensions += "version"
    productFlavors {
        create("free") {
            dimension = "version"
            applicationIdSuffix = ""
            versionNameSuffix = ""
            buildConfigField("Boolean", "IS_PREMIUM", "false")
        }
        // Future: Premium version with additional features
        // create("premium") {
        //     dimension = "version"
        //     applicationIdSuffix = ".premium"
        //     versionNameSuffix = "-premium"
        //     buildConfigField("Boolean", "IS_PREMIUM", "true")
        // }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
        }
    }
    
    // Lint options
    lint {
        abortOnError = false
        checkReleaseBuilds = true
        warningsAsErrors = false
        disable += "MissingTranslation"
    }
    
    // Bundle options for Play Store
    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
    
    assetPacks += ":model_pack"

    firebaseCrashlytics {
        mappingFileUploadEnabled = true
        nativeSymbolUploadEnabled = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.lottie.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    
    // AI & Model Delivery
    implementation(libs.mediapipe.tasks.genai)
    implementation(libs.localagents.rag) {
        exclude(group = "org.json", module = "json")
    }
    implementation(libs.play.asset.delivery)
    
    // In-App Update
    implementation(libs.play.app.update)
    
    // Document Extraction
    implementation(libs.pdfbox.android)
    implementation(libs.poi.ooxml)
    implementation(libs.jsoup)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.crashlytics.ndk)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Configure Kotlin compiler
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}