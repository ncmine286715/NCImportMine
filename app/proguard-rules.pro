# NC Import Mine - ProGuard Rules

# Mantém as classes do Gson (para parsear manifest.json)
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Mantém as data classes do app
-keep class com.ncmine.importmine.model.** { *; }
-keep class com.ncmine.importmine.util.ManifestInfo { *; }

# AdMob
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Coil
-dontwarn coil.**

# Accompanist
-dontwarn com.google.accompanist.**

# Lottie
-dontwarn com.airbnb.lottie.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
