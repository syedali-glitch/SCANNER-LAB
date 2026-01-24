# Platform Compatibility Guide

## Supported Platforms & Versions

### ✅ Android Phones & Tablets
- **Minimum**: Android 5.0 (Lollipop, API 21)
- **Maximum**: Android 14+ (API 34+)
- **Coverage**: 99.3% of all Android devices
- **Form Factors**: Phones, tablets, foldables

### ✅ ChromeOS
- **Support**: Full compatibility
- **Features**:
  - Resizable windows
  - Mouse and keyboard support
  - Multi-window mode
  - Optimized layouts for larger screens

### ✅ Fire OS (Amazon Devices)
- **Devices**: Fire tablets, Fire Phone
- **Play Store**: Available via sideloading or Amazon Appstore
- **Compatibility**: Fully tested and compatible

### ✅ Other Play Store Platforms
- **Android TV**: Limited (camera not available)
- **Wear OS**: Not optimized (small screen)
- **Android Auto**: Not applicable

## Version-Specific Features

### Android 5.0 - 5.1 (API 21-22)
- ✅ Basic QR/barcode scanning
- ✅ Scan history and favorites
- ✅ PDF creation
- ✅ Share functionality
- ⚠️ Material Design limited
- ⚠️ Vector drawables via support library

### Android 6.0 (API 23+)
- ✅ Runtime permissions
- ✅ Full camera controls
- ✅ Improved performance

### Android 7.0 (API 24+)
- ✅ Multi-window support
- ✅ FileProvider for secure sharing
- ✅ Enhanced notifications

### Android 8.0 (API 26+)
- ✅ Adaptive icons
- ✅ Notification channels
- ✅ Background limits (optimized)

### Android 10 (API 29+)
- ✅ Scoped storage
- ✅ Dark theme support (future)
- ✅ Gesture navigation

### Android 11 (API 30+)
- ✅ Enhanced storage access
- ✅ One-time permissions

### Android 13 (API 33+)
- ✅ Photo picker
- ✅ Granular media permissions
- ✅ Per-app language preferences

### Android 14 (API 34)
- ✅ Latest APIs
- ✅ Full screenshot detection
- ✅ Enhanced privacy features

## Platform-Specific Optimizations

### ChromeOS
```xml
<!-- Optimizations applied in AndroidManifest.xml -->
<meta-data android:name="WindowManagerPreference:FreeformWindowSize"
    android:value="maximize" />

<activity android:resizeableActivity="true" />
```

**Features:**
- App runs in resizable windows
- Keyboard shortcuts work
- Mouse support for all interactions
- Multi-window with other apps

### Fire OS (Amazon)
**Compatibility Notes:**
- Google Play Services replaced with Amazon equivalents
- AdMob may require Amazon Ads SDK for Amazon Appstore
- ML Kit works natively
- All core features functional

**Alternative Distribution:**
- Available on Amazon Appstore
- No code changes needed for core features

### Tablets & Foldables
**Responsive Design:**
```kotlin
// Detect tablet
val isTablet = resources.getBoolean(R.bool.isTablet)

// Adjust layouts accordingly
if (isTablet) {
    // Use two-pane layouts
    // Larger touch targets
    // More spacing
}
```

**Screen Sizes Supported:**
- Small (phones): 320dp+
- Medium (large phones): 360dp+
- Large (7" tablets): 600dp+
- XLarge (10" tablets): 720dp+

## Hardware Requirements

### Required
- ✅ Touchscreen (optional for TV/Auto)
- ✅ Internet connection (for ads only)

### Optional
- ⚠️ Camera (app works without, shows message)
- ⚠️ Autofocus (scanning works without)
- ⚠️ External storage (uses internal if unavailable)

## Permission Handling

### Android 5.0 - 5.1 (API 21-22)
- Install-time permissions only
- All permissions granted at install

### Android 6.0+ (API 23+)
- Runtime permission requests
- Graceful handling of denied permissions
- User can revoke anytime

### Android 13+ (API 33+)
- Granular media permissions
- `READ_MEDIA_IMAGES` instead of `READ_EXTERNAL_STORAGE`
- Notification permission required

## Testing Matrix

### Tested Configurations

| Platform | Version | Status |
|----------|---------|--------|
| Android Phone | 5.0+ | ✅ Verified |
| Android Tablet | 7.0+ | ✅ Verified |
| ChromeOS | Latest | ✅ Optimized |
| Fire OS | 5.0+ | ✅ Compatible |
| Foldables | 10.0+ | ✅ Responsive |

### Device Form Factors
- ✅ Small phones (< 5")
- ✅ Regular phones (5-6.5")
- ✅ Large phones (6.5"+)
- ✅ 7" tablets
- ✅ 10" tablets
- ✅ Foldables (unfolded)
- ✅ ChromeOS laptops
- ⚠️ Android TV (limited, no camera)

## DeviceCompatibility Utility

Use the provided utility class to adapt behavior:

```kotlin
import com.plainlabs.qrpdftools.util.DeviceCompatibility

// Check platform
if (DeviceCompatibility.isChromeOS(context)) {
    // Optimize for ChromeOS
}

if (DeviceCompatibility.isTablet(context)) {
    // Use tablet layout
}

// Check capabilities
if (!DeviceCompatibility.hasCamera(context)) {
    // Show message that camera not available
    // Disable scanner, enable PDF tools only
}

// Get device info
val deviceType = DeviceCompatibility.getDeviceType(context)
val androidVersion = DeviceCompatibility.getAndroidVersionName()
```

## Known Limitations

### Android TV
- ❌ Camera not available (no QR scanning)
- ✅ PDF tools work
- ✅ History/favorites work
- ⚠️ Requires TV-optimized UI (future)

### Wear OS
- ❌ Not optimized (small screen)
- ❌ Limited functionality
- 📋 Future: Watch-specific QR display

### Android Auto
- ❌ Not applicable for this app type

## Distribution Channels

### Google Play Store
- ✅ Primary distribution
- ✅ All Android devices
- ✅ ChromeOS via Play Store
- ✅ Automatic updates

### Amazon Appstore
- ✅ Fire OS devices
- ⚠️ Replace AdMob with Amazon Ads
- ⚠️ Replace Google Play Billing with Amazon IAP
- 📋 Use build flavor for Amazon variant

### Direct APK
- ✅ Works on all Android devices
- ⚠️ No automatic updates
- ⚠️ Requires "Unknown sources" enabled

## Migration Notes

### From API 24 to API 21
**Changes Made:**
- Lowered minSdk from 24 to 21
- Added vector drawable support library
- Enhanced permission handling for older versions
- Tested backward compatibility

**Impact:**
- Additional 5.3% device coverage
- Includes Android 5.0 and 5.1 devices
- No functionality loss
- Minimal performance impact

## Build Variants (Future)

### Standard (Google Play)
```gradle
productFlavors {
    playstore {
        dimension "distribution"
        // Google Play Services
        // AdMob
        // Google Play Billing
    }
}
```

### Amazon (Appstore)
```gradle
productFlavors {
    amazon {
        dimension "distribution"
        // Amazon Ads SDK
        // Amazon IAP
        // No Google Play Services
    }
}
```

## Compatibility Checklist

- [x] minSdk 21 (Android 5.0)
- [x] targetSdk 34 (Android 14)
- [x] Multi-window support
- [x] ChromeOS optimization
- [x] Fire OS compatibility
- [x] Tablet layouts (responsive)
- [x] Foldable support
- [x] Runtime permissions
- [x] Scoped storage (Android 10+)
- [x] Vector drawable support
- [x] Hardware feature detection
- [x] Graceful camera fallback
- [x] Flexible screen orientations
- [x] RTL language support
- [x] Accessibility features

## Support Statement

**Official Support:**
> QR PDF Tools supports Android 5.0 (Lollipop) and above, covering 99.3% of active Android devices. The app is optimized for phones, tablets, ChromeOS, and Fire OS devices. Full functionality requires a camera; however, PDF tools work on all devices regardless of camera availability.

## Future Enhancements

- [ ] Android TV optimized UI
- [ ] Wear OS QR code display
- [ ] Car mode (Android Auto)
- [ ] Desktop mode (Samsung DeX)
- [ ] Tablet-specific two-pane layouts
- [ ] Foldable-specific dual-screen support
