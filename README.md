# QR Scanner + PDF Tools

A comprehensive Android application featuring QR/barcode scanning, PDF manipulation tools, with premium UI/UX and monetization features.

## Features

### Core Functionality
- **QR & Barcode Scanner**: Instant scanning with ML Kit, auto-save, and visual feedback
- **Scan History**: All scans auto-saved with timestamps, searchable
- **Favorites**: Star important scans for quick access
- **Share & Export**: Share via WhatsApp, Gmail, Drive, or copy to clipboard

### PDF Tools
- **PDF Scanner**: Camera-based document scanning with auto-crop
- **Merge PDFs**: Combine multiple PDF files
- **Split PDFs**: Extract page ranges from PDFs

### Premium Features
- **Remove Ads**: One-time in-app purchase to disable all advertisements
- **Restore Purchases**: Restore previous purchases across devices

## Technology Stack

### Core
- **Language**: Kotlin
- **Min SDK**: API 21 (Android 5.0 Lollipop) - **99.3% device coverage**
- **Target SDK**: API 34 (Android 14)
- **Architecture**: MVVM with Repository pattern

### Platform Support
- ✅ **Android Phones & Tablets**: Android 5.0+ (all versions)
- ✅ **ChromeOS**: Full support with resizable windows
- ✅ **Fire OS**: Amazon devices (Fire tablets)
- ✅ **Foldables**: Responsive layouts
- ⚠️ **Android TV**: Limited (no camera)

See [PLATFORM_COMPATIBILITY.md](PLATFORM_COMPATIBILITY.md) for complete details.

### Key Libraries
- **CameraX**: Camera operations and preview
- **ML Kit**: Barcode scanning (QR codes, barcodes, etc.)
- **iText 7**: PDF creation, merging, and splitting
- **Room Database**: Local storage for scan history
- **DataStore**: Preferences management
- **Google AdMob**: Advertisement integration
- **Google Play Billing v6**: In-app purchases
- **Material Design 3**: Modern UI components

## Project Structure

```
app/
├── src/main/
│   ├── java/com/plainlabs/qrpdftools/
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── entity/       # Room entities
│   │   │   │   ├── dao/          # Data Access Objects
│   │   │   │   ├── AppDatabase.kt
│   │   │   │   └── PreferencesManager.kt
│   │   │   └── repository/       # Repository pattern
│   │   ├── domain/
│   │   │   └── model/            # Domain models
│   │   ├── ui/
│   │   │   ├── main/             # Main activity & ViewModel
│   │   │   ├── scanner/          # Scanner components
│   │   │   ├── history/          # History bottom sheet
│   │   │   ├── favorites/        # Favorites bottom sheet
│   │   │   ├── settings/         # Settings bottom sheet
│   │   │   └── pdf/              # PDF tools
│   │   ├── util/                 # Utility classes
│   │   ├── ads/                  # AdMob integration
│   │   └── billing/              # Billing integration
│   └── res/
│       ├── layout/               # XML layouts
│       ├── drawable/             # Icons and drawables
│       ├── anim/                 # Animations
│       ├── values/               # Colors, strings, themes
│       └── xml/                  # File provider paths
```

## Setup Instructions

### 1. Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34

### 2. Clone and Build
```bash
git clone <repository-url>
cd PlainLabs
./gradlew build
```

### 3. Configure AdMob
Replace test Ad Unit IDs in `app/src/main/res/values/ad_ids.xml` with your own:
```xml
<string name="banner_ad_unit_id">YOUR_BANNER_AD_UNIT_ID</string>
```

Also update in `AndroidManifest.xml`:
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="YOUR_ADMOB_APP_ID"/>
```

### 4. Configure Billing
Set up your product IDs in Google Play Console to match:
- `remove_ads` - Remove ads IAP
- `premium_features` - Premium features IAP

### 5. Run
Connect an Android device or start an emulator, then click Run in Android Studio.

## UI/UX Design

### Color Palette
- **Primary Blue**: #1E90FF (actions, FAB, highlights)
- **Background White**: #FFFFFF
- **Secondary Gray**: #F0F0F0 (panels)
- **Text Dark**: #333333
- **Favorite Yellow**: #FFD700 (star icon)

### Key UI Elements
- **FAB**: Large circular button (64dp) with pulse animation when ready
- **Bottom Toolbar**: History, Favorites, Settings icons
- **Swipe Gestures**: Left to delete, right to favorite
- **Animations**: Success animation on scan, smooth transitions

## Monetization

### Revenue Layers

1. **Banner Ads**
   - Always visible at bottom of main screen
   - Non-intrusive, maintains user experience
   - Estimated CPM: $0.10-$0.50

2. **Interstitial Ads**
   - Shown after first scan/save
   - Limited to 1-2 per session
   - Estimated CPM: $1-$3

3. **Rewarded Ads** 🆕
   - **Voluntary, user-initiated**
   - Watch ad to unlock premium features (PDF merge, advanced sharing)
   - Unlimited use, user chooses when
   - Estimated CPM: $2-$5
   - See [REWARDED_ADS_GUIDE.md](REWARDED_ADS_GUIDE.md) for implementation

4. **Remove Ads IAP**
   - **$1.99 one-time purchase**
   - Disables all advertisements
   - Instant access to all features
   - Restore purchase functionality included

### Key Principle
Core QR/barcode scanning is **always frictionless** and ad-free. Ads never block main functionality.

## Permissions

Required permissions:
- **CAMERA**: For QR/barcode scanning and PDF document capture
- **READ_MEDIA_IMAGES**: For accessing PDFs (Android 13+)
- **READ_EXTERNAL_STORAGE**: For accessing PDFs (Android 12 and below)
- **INTERNET**: For loading advertisements

## Building for Release

1. Generate a signed APK/Bundle:
   ```bash
   ./gradlew bundleRelease
   ```

2. The release build will be in:
   ```
   app/build/outputs/bundle/release/
   ```

3. Upload to Google Play Console

## Testing

### Test Ads
The app uses test Ad Unit IDs by default. These will show test ads during development.

### Test Billing
Use Google Play Console's test tracks and license testing to test IAP functionality.

## Future Enhancements
- Cloud backup via Firebase
- OCR text extraction from PDFs
- Batch QR code generation
- Custom QR code designs
- Export scan history to CSV

## License
Copyright © 2026 PlainLabs. All rights reserved.

## Support
For issues and feature requests, please contact: support@plainlabs.com
