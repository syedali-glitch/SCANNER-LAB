# Add project specific ProGuard rules here.
# Keep Apache POI classes
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**

# Keep Apache PDFBox classes
-keep class org.apache.pdfbox.** { *; }
-dontwarn org.apache.pdfbox.**

# Keep iText classes
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# Keep ML Kit classes
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# library-specific dontwarn rules to fix R8 missing class errors
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**
-dontwarn org.osgi.framework.**
-dontwarn net.sf.saxon.**
-dontwarn aQute.bnd.annotation.spi.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.fop.**
-dontwarn org.apache.xmlbeans.**

# Optimize and obfuscate
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
