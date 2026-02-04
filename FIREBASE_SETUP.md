# Firebase Setup Instructions

## Google Services JSON File Missing

Firebase Crashlytics requires a `google-services.json` configuration file. The app will still run without it, but crash reporting won't work until you add it.

## Setup Steps

### 1. Create Firebase Project
1. Go to https://console.firebase.google.com/
2. Click "Add project" or use existing project
3. Enter project name: `QR PDF Tools` (or your preferred name)
4. Follow the wizard (Google Analytics optional)

### 2. Add Android App to Firebase
1. In Firebase Console, click "Add app" → Android icon
2. Register app with package name: `com.plainlabs.qrpdftools`
3. Download `google-services.json` file

### 3. Add google-services.json to Your Project
1. Place the downloaded `google-services.json` file here:
   ```
   app/google-services.json
   ```
2. Commit and push:
   ```bash
   git add app/google-services.json
   git commit -m "Add Firebase configuration"
   git push
   ```

### 4. Build and Test
1. GitHub Actions will build the APK with Firebase integrated
2. Install APK on device
3. Crashlytics will automatically collect and send crashes

## Viewing Crash Reports

1. Go to Firebase Console: https://console.firebase.google.com/
2. Select your project
3. Click "Crashlytics" in left menu
4. View crash reports with full stack traces, device info, and timestamps

## How It Works

- **Automatic**: All crashes are automatically collected and uploaded
- **Detailed**: Full stack traces, device info, Android version, app version
- **Real-time**: View crashes within minutes of occurrence
- **No code needed**: Just add google-services.json

## Temporary Workaround

Until you add `google-services.json`, the app uses:
- File-based logging (stores crashes in app data folder)
- Webhook reporting (if webhook URL is configured)

After Firebase is set up, you'll get **professional crash analytics** in Firebase Console! 🎯
