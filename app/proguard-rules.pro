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

-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn java.awt.**
-dontwarn org.apache.logging.log4j.**
-dontwarn javax.xml.stream.**
-dontwarn com.github.javaparser.**
-dontwarn net.sf.saxon.**
-dontwarn org.apache.maven.**
-dontwarn org.apache.tools.ant.**
-dontwarn org.slf4j.**
-dontwarn com.sun.org.apache.xml.internal.**
-dontwarn org.w3c.dom.**
-dontwarn org.xml.sax.**

# Preserve iText
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# Retrofit/OkHttp (if used later)
-dontwarn okhttp3.**
-dontwarn retrofit2.**
