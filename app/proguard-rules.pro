# Add project specific ProGuard rules here.
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Preserve all annotated classes (e.g. Room entities)
-keep @androidx.room.Entity class *
-keep @androidx.room.Database class *
-keep @androidx.room.Dao class *

# Preserve coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.CoroutineExceptionHandler {
    <init>(...);
}

# Preserve ML Kit
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# Strict R8 Rules for Maximum Shrinking

# 1. Apache POI: Keep only what is absolutely necessary
# Allow R8 to strip most of POI, but warn about missing classes (we don't use 90% of it)
-keep class org.apache.poi.xwpf.usermodel.** { *; }
-keep class org.apache.poi.xssf.usermodel.** { *; }
-keep class org.apache.poi.ss.usermodel.** { *; }
-keep class org.apache.poi.ooxml.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class com.microsoft.schemas.** { *; }

-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn schemaorg_apache_xmlbeans.**
-dontwarn java.awt.**
-dontwarn javax.xml.**

# 2. iText: Aggressive stripping
-keep class com.itextpdf.kernel.** { *; }
-keep class com.itextpdf.layout.** { *; }
-dontwarn com.itextpdf.**

# 3. Coroutines (Release mode optimizations)
-assumenosideeffects class kotlinx.coroutines.DebugKt {
    boolean getASSERTIONS_ENABLED() return false;
}

# 4. Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# 5. General Optimization
-optimizationpasses 5
-allowaccessmodification

# Retrofit/OkHttp (if used later)
-dontwarn okhttp3.**
-dontwarn retrofit2.**
