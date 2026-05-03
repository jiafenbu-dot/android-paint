# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /sdk/tools/proguard/proguard-android.txt

# Keep model classes
-keep class com.jiafenbu.androidpaint.model.** { *; }
-keep class com.jiafenbu.androidpaint.brush.** { *; }
-keep class com.jiafenbu.androidpaint.engine.** { *; }
-keep class com.jiafenbu.androidpaint.command.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Compose
-dontwarn androidx.compose.**
