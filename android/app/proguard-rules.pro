# Keep ML Kit models
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# --- Retrofit / OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keepattributes Signature
-keepattributes Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# --- Gson (Retrofit converter) ---
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.remitos.app.network.** { *; }

# --- Hilt aggregated modules ---
-keep class dagger.hilt.internal.aggregatedroot.** { *; }

# --- WorkManager + Hilt Worker ---
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}
