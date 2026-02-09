# PlainLabs Scanner (Privacy-First)

A high-performance, privacy-focused Android document scanner built with Kotlin and Jetpack libraries.

## 🚀 Key Features
- **Privacy-First Architecture:** No cloud uploads. All processing happens on-device.
- **Native PDF Engine:** Custom-built PDF generation using `android.graphics.pdf.PdfDocument`. No 3rd-party AGPL dependencies.
- **High-End Imaging:** "Magic" filter (saturation/contrast boost) and Perspective Correction support.
- **Scoped Storage Compliance:** Fully compatible with Android 10+ (API 29-35) using `MediaStore` and `ContentResolver`.
- **Modern UI:** "Muted Cyan" theme with dark mode support.

## 🛠 Tech Stack
- **Language:** Kotlin
- **Camera:** CameraX
- **ML:** ML Kit (On-Device Text Recognition & Object Detection)
- **PDF:** Native Android PDF + OpenPDF (LGPL) for merge/split only
- **Concurrency:** Coroutines & Flow
- **Database:** Room
- **Build:** Gradle 8.13 + AGP 8.7.3

## 📦 Build Instructions
1. Clone the repository.
2. Open in Android Studio (Jellyfish or later).
3. Sync Gradle.
4. Run `assembleDebug`.

## 🔒 License
Proprietary - PlainLabs Inc. (Patent Pending)
