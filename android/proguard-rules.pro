# Keep all domain models
-keep class com.airmouse.domain.model.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Keep Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.** { *; }

# Keep TensorFlow Lite
-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep Serialization
-keep class kotlinx.serialization.** { *; }
-keep class kotlinx.serialization.json.** { *; }

# Keep Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class *

# Keep Coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep Timber
-keep class com.jakewharton.timber.** { *; }

# Keep USB Serial
-keep class com.hoho.android.usbserial.** { *; }
-keep class com.felhr.usbserial.** { *; }

# Keep QR Code
-keep class com.journeyapps.** { *; }
-keep class com.google.zxing.** { *; }

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Remove logs in release
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}