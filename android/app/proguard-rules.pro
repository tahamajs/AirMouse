# ==============================================================================
# AirMouse Pro — R8 Proguard Optimization & Shrinking Rules
# ==============================================================================

# ---- Kotlin Coroutines ----
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ---- Kotlin Metadata & Annotations (needed for reflection & serialization) ----
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault, InnerClasses, EnclosingMethod, Signature, *Annotation*
-keep class kotlin.Metadata { *; }

# ---- OkHttp / Okio ----
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclassmembers class okhttp3.internal.** { *; }

# ---- kotlinx.serialization ----
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- Gson & Serialized DTOs ----
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}

# ---- AirMouse Application, Entry Points & Core Components ----
-keep class com.airmouse.AirMouseApplication { *; }
-keep class com.airmouse.presentation.MainActivity { *; }
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver

# ---- AirMouse Data & Domain Models (preserved for serialization & DataStore) ----
-keep class com.airmouse.data.model.** { *; }
-keep class com.airmouse.domain.model.** { *; }

# ---- Room Database & DAOs ----
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ---- Third-Party UI & Charting ----
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**
-dontwarn com.journeyapps.barcodescanner.**
-dontwarn com.google.zxing.**

# ---- Native Methods ----
-keepclasseswithmembernames class * {
    native <methods>;
}

# ---- Strip Debug Logs in Release ----
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
