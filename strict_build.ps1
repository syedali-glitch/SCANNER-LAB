$ErrorActionPreference = "Stop"

Write-Host "Starting STRICT Local Build (Matching CI/CD Rules)..." -ForegroundColor Cyan

# 1. Clean Project (Ensure fresh environment like CI)
Write-Host "Step 1: Cleaning Project..."
.\gradlew clean

if ($LASTEXITCODE -ne 0) {
    Write-Error "Clean failed!"
    exit 1
}

# 2. Build Debug with Warnings as Errors (configured in build.gradle.kts)
Write-Host "Step 2: Building Debug APK (Strict Mode)..."
# --warning-mode=all ensures all deprecation warnings are shown
.\gradlew assembleDebug --warning-mode=all --stacktrace

if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed! Please fix all warnings and errors."
    exit 1
}

# 3. Run Lint (Abort on Error configured in build.gradle.kts)
Write-Host "Step 3: Running Lint Checks..."
.\gradlew lint

if ($LASTEXITCODE -ne 0) {
    Write-Error "Lint failed!"
    exit 1
}

Write-Host "STRICT BUILD SUCCESSFUL! 🎉" -ForegroundColor Green
Write-Host "APK Location: app\build\outputs\apk\debug\app-debug.apk"
