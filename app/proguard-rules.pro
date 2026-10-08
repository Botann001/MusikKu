# ==============================================================================
# Aturan ProGuard / R8 untuk MusikKu Release Build
# ==============================================================================

# ─── Room Database ─────────────────────────────────────────────────────────────
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# ─── Retrofit & Gson (Jamendo API DTOs) ─────────────────────────────────────────
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.example.musikku.data.remote.dto.** { *; }
-keep,allowobfuscation,allowshrinking class retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ─── AndroidX Media3 (ExoPlayer & MediaSession) ────────────────────────────────
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.common.** { *; }
-dontwarn androidx.media3.**

# ─── WorkManager ───────────────────────────────────────────────────────────────
-keep class androidx.work.Worker
-keep class androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ─── Coil (Image Loading) ──────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ─── Jetpack Compose ──────────────────────────────────────────────────────────
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# ─── Kotlin Coroutines ────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.Dispatchers {
    public static <fields>;
}
