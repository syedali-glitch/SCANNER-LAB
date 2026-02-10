# Scanner Lab - PROJECT BACKUP & SPECIFICATIONS
Version: 1.0.1 (Debug)
Last Updated: 2026-02-09

## 1. Project Overview
**Scanner Lab** is a high-performance Android utility suite designed for professional document processing. It adheres to an **Offline-First**, **Privacy-Centric** philosophy, processing all data locally on the device.

### Core Philosophy
- **Speed**: Native C++ and optimized Kotlin implementations.
- **Privacy**: No cloud uploads; all ML models run on-device.
- **Aesthetics**: "Stealth" Premium Dark Theme with Glassmorphism.

---

## 2. App Structure & Architecture
The application follows a **Single-Activity-Per-Feature** architecture to ensure modularity and memory efficiency, tied together by a central dashboard.

### Core Components
| Activity | Functionality | Key Engines/Libraries |
| :--- | :--- | :--- |
| **MainActivity** | Dashboard & Navigation Hub | `CoordinatorLayout`, `GridLayout` |
| **DocumentScannerActivity** | Camera Capture & Processing | `CameraX`, `ML Kit (Vision)`, `OpenCV` (planned) |
| **ToolsActivity** | **[NEW]** Central Utilites Hub | 17-Tool Grid, `NestedScrollView` |
| **ConverterActivity** | Image <-> Document Conversion | `Apache POI`, `iText/OpenPDF` |
| **PdfToolsActivity** | PDF Manipulation (Merge/Split) | `PdfDocument`, `OpenPDF` |
| **FileViewerActivity** | File Management & Preview | `MediaStore` API, Custom `FileProvider` |
| **QrScannerActivity** | QR/Barcode Scanning | `ML Kit (Barcode)`, `ClipBoard Manager` |
| **SettingsActivity** | App Configuration | `SharedPreferences`, `LocaleHelper` |

### Architectural Patterns
- **BaseActivity**: Enforces Language Context (`LocaleHelper`) and Theme Application across all screens.
- **ViewBinding**: Type-safe interaction with XML layouts.
- **Coroutines**: Asynchronous processing for all heavy lifting (PDF generation, OCR, Compression).

---

## 3. UI/UX Features & Design Language
The "Stealth Palette" defines the visual identity, focusing on deep contrast for professional use in all lighting conditions.

### Visual Elements
- **Glassmorphism**: Translucent card backgrounds (`#991E293B`) with white borders (`strokeWidth=1dp`) to mimic frosted glass.
- **Gradients**: 
  - *Scanner*: Cyan-to-Blue (`#00E5FF` -> `#2979FF`)
  - *Tools*: Purple-to-Pink (`#D500F9` -> `#FF4081`)
- **Typography**: Clean, sans-serif fonts (Roboto/Inter) with high readability on dark backgrounds.

### Interaction Design
- **Haptic Feedback**: `VIRTUAL_KEY` vibration on all major actions (Capture, Save, Tool Selection).
- **Micro-Animations**: 
  - *Pulse*: Buttons scale up/down on touch.
  - *Ripple*: Native Android ripple effects on list items (`?attr/selectableItemBackground`).
- **Transitions**: Smooth activity transitions (Fade/Slide).

---

## 4. Features & Engine Logic
A breakdown of the implemented features and the "under-the-hood" logic that powers them.

### A. Core Scanning Engine
| Feature | Engine / Logic | Status |
| :--- | :--- | :--- |
| **Auto-Edge Detection** | **ML Kit Vision**: Real-time contour detection to identify document boundaries. | ✅ Active |
| **Perspective Correction** | **Matrix Transformation**: `setPolyToPoly` to de-warp captured images based on 4 corner points. | ✅ Active |
| **Magic Color Filter** | **ColorMatrix**: Custom 4x5 matrix to boost saturation, contrast, and whiten backgrounds (Adaptive Thresholding). | ✅ Active |
| **Shadow Removal** | *Illumination Correction Map*: Estimating light distribution and flattening browsing. | 🚧 Placeholder |

### B. Intelligent OCR & Conversion
| Feature | Engine / Logic | Status |
| :--- | :--- | :--- |
| **Offline OCR** | **ML Kit Text Recognition v2**: On-device LSTM model for high-accuracy text extraction. | ✅ Active |
| **Scan to Excel** | **Apache POI (XSSF)**: Parcels extracted text/tables into `.xlsx` cells. | ✅ Active |
| **Scan to Word** | **Apache POI (XWPF)**: Writes text paragraphs into `.docx` structure. | ✅ Active |
| **Handwriting** | *Digital Ink Recognition*: Specialized ML model for cursive/handwritten text. | 🚧 Placeholder |

### C. PDF Tools & Security
| Feature | Engine / Logic | Status |
| :--- | :--- | :--- |
| **Native PDF Gen** | **Android PdfDocument**: Generates PDF pages directly from Bitmaps (Memory optimized). | ✅ Active |
| **PDF Encryption** | **OpenPDF / Bouncy Castle**: AES-256 encryption for password protection. | ✅ Active |
| **Merge/Split** | **OpenPDF**: Stream-based manipulation to stitch or separate PDF pages. | ✅ Active |
| **Private Space** | *AES-Encrypted Storage*: Biometric-locked folder with file encryption. | 🚧 Placeholder |

### D. Specialized Modes (Stage 2)
| Feature | Engine / Logic | Status |
| :--- | :--- | :--- |
| **ID Card Mode** | *Canvas Composition*: Stitches two captures (Front/Back) onto a single A4 canvas. | 🚧 Placeholder |
| **Passport Reader** | *MRZ Parser*: Extracts data from Machine Readable Zones (ICAO 9303 standard). | 🚧 Placeholder |
| **Finger Removal** | *Inpainting*: Uses Navier-Stokes or Telea algorithm to remove finger occlusions. | 🚧 Placeholder |

---
*Generated by Plainlabs Support Agent (Antigravity).*
