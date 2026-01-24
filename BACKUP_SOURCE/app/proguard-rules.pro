# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep data classes
-keep class com.plainlabs.qrpdftools.data.** { *; }
-keep class com.plainlabs.qrpdftools.domain.** { *; }

# Keep Room entities
-keep @androidx.room.Entity class * { *; }

# Keep Google services
-keep class com.google.android.gms.** { *; }
-keep class com.google.mlkit.** { *; }

# Keep billing classes
-keep class com.android.billingclient.** { *; }

# iText PDF
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# CameraX
-keep class androidx.camera.** { *; }
