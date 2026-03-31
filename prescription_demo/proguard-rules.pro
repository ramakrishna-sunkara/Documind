# ProGuard rules for prescription_demo library

# Keep Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.documind.prescription.domain.** { *; }
