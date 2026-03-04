# Idea.MD - Scanner & Converter Studio Specification

## 1. Project Overview
**App Name**: Scanner Lab (Powered by PlainLabs)
**Core Function**: A premium, privacy-focused document productivity suite that combines high-fidelity scanning, intelligent OCR, PDF utilities, and office file conversion into a single offline-capable application.
**Target Audience**: Professionals, students, and power users who need reliable document processing without subscription fatigue or cloud dependency.

## 2. UI/UX Design Language: "Stealth Protocol"
The application utilizes a dark, high-contrast aesthetic designed for prolonged usage and professional appeal.

### Color Palette
| Token | Hex | Usage |
| :--- | :--- | :--- |
| **Obsidian Black** | `#0F172A` | Main Background (Deepest layer) |
| **Deep Slate** | `#1E293B` | Surface Layer (Cards, Bottom Sheets) |
| **Midnight Grey** | `#334155` | Borders, Dividers, Inactive Icons |
| **Emerald Signal** | `#10B981` | Primary Actions (Save, Confirm, Active State) |
| **Electric Sky** | `#38BDF8` | Secondary Accents (Selection, Links) |
| **Crimson Alert** | `#F43F5E` | Destructive Actions (Delete, Remove Page) |
| **Text Primary** | `#F8FAFC` | Main Headings (High Contrast) |
| **Text Secondary** | `#94A3B8` | Subtitles, Metadata |

### Typography & Iconography
- **Font Family**: System Default Sans-Serif (Medium/Bold weights for headers).
- **Icons**: Minimalist, outline-style vector assets with uniform stroke width.
    - **Active**: Tinted with Emerald Signal or Electric Sky.
    - **Inactive**: Tinted with Midnight Grey.
- **Card Design**: `MaterialCardView` with `24dp` corner radius, `1dp` stroke (#33475569), and `4dp` elevation.

### UX Principles
1.  **Direct Action**: No hidden menus. All tools are accessible via the **Tools Hub** or **File Viewer** context actions.
2.  **Visual Feedback**: Ripple effects on touch, loading spinners (`ProgressBar`) for async ops, and Toast confirmations.
3.  **100% Offline Privacy**: The application is strictly offline. All processing (OCR, Translation, Conversion) happens on-device with zero network connectivity. No data ever leaves the device.

---

## 3. Core Features & Engines

### A. Document Scanning
**Engine**: `CameraX` + Custom `ImageProcessor`
*   **Auto-Edge Detection**:
    *   **Logic**: OpenCV (Canny Edge + Houghton Transform) finds the largest quadrilateral in the frame.
    *   **UI**: Real-time overlay with green polygon guides.
*   **Perspective Correction**:
    *   **Logic**: `Matrix.setPolyToPoly` transforms the 4 detected corners to a registered rectangle.
*   **Filters**:
    *   **Magic Filter v2**: Histogram stretching + Saturation boost to whiten backgrounds and darken text.
    *   **Shadow Remover**: Divisive Normalization (Blur/Scale trick) to estimate and subtract illumination.
    *   **B&W**: High-contrast grayscale thresholding.

### B. Specialized Scanning Modes
**Engine**: Custom State Machines & ML Kit
*   **ID Card Mode**:
    *   **Logic**: Captures Front & Back images separately, then stitches them onto a single A4 canvas using `Canvas.drawBitmap`.
*   **Passport Mode**:
    *   **Logic**: Uses **ML Kit Text Recognition** to parse the MRZ (Machine Readable Zone) and extract data (Name, DOB, Expiry).
*   **Book Mode**:
    *   **Logic**: `BookPageProcessor` splits a landscape capture into two portrait pages (Left/Right) automatically.

### C. PDF Tools
**Engine**: `OpenPDF` (LibrePDF) & `Apache PDFBox`
*   **Merge**: Concatenates multiple PDF InputStreams.
*   **Split**: Extracts pages from a source PDF into new files.
*   **Compress**: Re-encodes embedded images with lower JPEG quality and resolution.
*   **Watermark**: Overlays text on existing PDF pages using `PdfContentByte` (Absolute positioning, Rotation).
*   **Security**: Encrypts PDF with 128-bit AES using User/Owner passwords.
*   **Organize**: Removes or reorders pages based on a passed index list (e.g., `3,1,2`).

### D. File Conversion (Office Suite)
**Engine**: `Apache POI` (Ported for Android)
*   **Image to PDF**: Native Android `PdfDocument`.
*   **Image to Excel (XLSX)**: Creates a spreadsheet, inserts images into cells, and adjusts row heights.
*   **Image to Word (DOCX)**: Creates a document, creates a run, and embeds the image as a picture.
*   **Image to PowerPoint (PPTX)**: Creates a slide deck, adding one slide per image (AWT-free implementation).
*   **PDF to Text**: Extracts raw text using `PDFTextStripper` (PDFBox).

### E. Advanced AI Tools
**Engine**: Custom Algorithms & ML Kit
*   **Handwriting Recognition**:
    *   **Logic**: **ML Kit Digital Ink Recognition**. Captures touch strokes on a `HandwritingDrawingView` and converts them to digital text strings.
*   **Magic Eraser (Inpainting)**:
    *   **Logic**: **Fast Marching Method (FMM)** by Alexandru Telea (Custom Kotlin Implementation). Reconstructs masked areas using pixel information from the boundary inwards.
*   **Anti-Counterfeit**:
    *   **Logic**: Overlays a repeating, semi-transparent pattern ("SECURE COPY") rotated at 45 degrees over the image bitmap.

---

## 4. Architecture & Technical Stack

### Foundation
*   **Language**: Kotlin (100%)
*   **Min SDK**: 26 (Android 8.0)
*   **Target SDK**: 34 (Android 14)
*   **Build System**: Gradle (Kotlin DSL)

### Key Libraries
| Library | Purpose |
| :--- | :--- |
| **CameraX** | High-performance camera preview and image capture. |
| **Material Components** | Modern UI widgets (Cards, Buttons, Chips). |
| **Coroutines** | Async background processing (File I/O, heavy computation). |
| **ViewBinding** | Type-safe view interaction. |
| **OpenPDF** | PDF manipulation (Merge, Split, Watermark). |
| **Apache POI** | MS Office document generation (Word, Excel, PPT). |
| **ML Kit** | On-device Machine Learning (OCR, Digital Ink, Barcode). |
| **Gson** | JSON serialization (Metadata, Settings). |

### Storage Strategy
*   **Scoped Storage**: Adheres to Android 10+ privacy standards.
*   **Internal Storage**: Used for sensitive data (Private Space) and temporary cache.
*   **MediaStore**: Used for exporting final documents to the public "Documents" folder.

### Project Structure
*   `com.scanner.lab`
    *   `ui/` -> Custom Views (ScannerOverlay, AnnotationView, HandwritingDrawingView)
    *   `utils/` -> Helper Object Classes (ImageProcessor, PdfUtilityTools, DocxConverter)
    *   `tools/` -> Feature-specific Activities (SignatureActivity, TranslatorActivity, FingerRemovalActivity)
    *   `models/` -> Data classes (FileModel, FilterMode)
