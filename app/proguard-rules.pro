# ===================================================================
# DocuMind ProGuard Rules
# ===================================================================

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Hide original source file name
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# ===================================================================
# App-specific rules
# ===================================================================

# Keep domain models
-keep class com.documind.app.domain.model.** { *; }
-keep class com.documind.app.data.extractor.SourceType { *; }
-keep class com.documind.app.data.llm.ModelState { *; }

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ===================================================================
# MediaPipe LLM Inference
# ===================================================================

-keep class com.google.mediapipe.** { *; }
-keep interface com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# ===================================================================
# Google Play Core (Asset Delivery, In-App Update, In-App Review)
# ===================================================================

-keep class com.google.android.play.core.** { *; }
-dontwarn com.google.android.play.core.**

# In-App Update
-keep class com.documind.app.data.update.** { *; }

# ===================================================================
# Firebase
# ===================================================================

# Firebase Analytics
-keep class com.google.firebase.analytics.** { *; }
-keep class com.google.android.gms.measurement.** { *; }
-dontwarn com.google.firebase.analytics.**

# Firebase Cloud Messaging
-keep class com.google.firebase.messaging.** { *; }
-dontwarn com.google.firebase.messaging.**

# Firebase Core
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Firebase Crashlytics
-keep class com.google.firebase.crashlytics.** { *; }
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep FirebaseMessagingService
-keep class com.documind.app.data.fcm.DocuMindMessagingService { *; }

# Keep AnalyticsManager
-keep class com.documind.app.data.analytics.AnalyticsManager { *; }
-keep class com.documind.app.data.analytics.AnalyticsManager$* { *; }

# ===================================================================
# Apache PDFBox Android
# ===================================================================

-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-keep class com.tom_roush.harmony.** { *; }
-dontwarn com.tom_roush.**
-dontwarn org.bouncycastle.**
-dontwarn javax.xml.**

# ===================================================================
# Apache POI (Word document processing)
# ===================================================================

-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class org.w3.x2000.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }

-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn org.etsi.**
-dontwarn org.w3.**
-dontwarn com.microsoft.**
-dontwarn schemaorg_apache_xmlbeans.**

# POI uses Java AWT which is not available on Android
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**

# ===================================================================
# Jsoup HTML Parser
# ===================================================================

-keep class org.jsoup.** { *; }
-keeppackagenames org.jsoup.nodes
-dontwarn org.jsoup.**

# ===================================================================
# Kotlin Coroutines
# ===================================================================

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ===================================================================
# Jetpack Compose
# ===================================================================

-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }

# ===================================================================
# AndroidX and Material Components
# ===================================================================

-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ===================================================================
# Serialization (for StateFlow, etc.)
# ===================================================================

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===================================================================
# Enums
# ===================================================================

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===================================================================
# Native methods
# ===================================================================

-keepclasseswithmembernames class * {
    native <methods>;
}

# ===================================================================
# Parcelable
# ===================================================================

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ===================================================================
# R8 Full Mode compatibility
# ===================================================================

-allowaccessmodification
-repackageclasses

# ===================================================================
# Remove logging in release
# ===================================================================

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ===================================================================
# Suppress warnings for missing classes
# ===================================================================

-dontwarn org.slf4j.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.**
-dontwarn javax.xml.stream.**
-dontwarn com.sun.**
-dontwarn sun.**