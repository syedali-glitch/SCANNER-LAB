# GitHub Actions Build Guide

## Automated Builds

This project includes GitHub Actions workflows for automated builds:

### 1. Continuous Integration (android-build.yml)
**Triggers on:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Manual trigger (workflow_dispatch)

**What it does:**
- ✅ Builds Debug APK
- ✅ Builds Release APK (unsigned)
- ✅ Runs Lint checks
- ✅ Uploads APKs as artifacts
- ✅ Caches Gradle dependencies for faster builds

**Download APKs:**
1. Go to Actions tab
2. Click on latest workflow run
3. Scroll to Artifacts section
4. Download `scanner-lab-debug` or `scanner-lab-release`

### 2. Release Build (release.yml)
**Triggers on:**
- Tag push matching `v*` (e.g., v1.0.0)
- Manual trigger

**What it does:**
- ✅ Builds Release APK
- ✅ Signs APK (if secrets configured)
- ✅ Creates GitHub Release
- ✅ Uploads APK to release

**How to create a release:**
```bash
# Tag the commit
git tag v1.0.0
git push origin v1.0.0

# GitHub Actions will automatically:
# 1. Build the APK
# 2. Sign it (if secrets configured)
# 3. Create a GitHub Release
# 4. Upload the APK
```

---

## Setting Up Signing (Optional)

To automatically sign your APKs in GitHub Actions:

### 1. Generate Keystore
```bash
keytool -genkey -v -keystore scanner-lab-keystore.jks \
  -alias scanner-lab -keyalg RSA -keysize 2048 -validity 10000
```

### 2. Encode Keystore to Base64
```bash
# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("scanner-lab-keystore.jks"))

# Linux/Mac
base64 scanner-lab-keystore.jks | tr -d '\n'
```

### 3. Add GitHub Secrets
Go to your repository → Settings → Secrets and variables → Actions

Add these secrets:
- `SIGNING_KEY` - The base64 encoded keystore
- `ALIAS` - Your keystore alias (e.g., "scanner-lab")
- `KEY_STORE_PASSWORD` - Your keystore password
- `KEY_PASSWORD` - Your key password

---

## Manual Build on GitHub

### Using GitHub Actions Interface

1. Go to **Actions** tab
2. Click **Android CI/CD** or **Release Build**
3. Click **Run workflow** button
4. Select branch
5. Click **Run workflow**
6. Wait for build to complete
7. Download APK from Artifacts section

---

## Build Status Badges

Add to your README:

```markdown
![Android CI/CD](https://github.com/YOUR_USERNAME/2ndScannerConverter/workflows/Android%20CI/CD/badge.svg)
```

Replace `YOUR_USERNAME` with your GitHub username.

---

## Local Testing Before Push

Test your workflow locally using [act](https://github.com/nektos/act):

```bash
# Install act (requires Docker)
choco install act-cli  # Windows
brew install act       # Mac

# Run the workflow
act -j build
```

---

## Troubleshooting

### Build Fails
- Check JDK version (should be 17)
- Verify Gradle wrapper is executable
- Check dependency versions

### Artifacts Not Uploaded
- Ensure workflow completed successfully
- Check artifact retention period (default 90 days)
- Verify upload path is correct

### Signing Fails
- Verify all secrets are set correctly
- Check base64 encoding has no line breaks
- Ensure keystore passwords match

---

## Build Times

Expected build times on GitHub Actions:

| Build Type | Time |
|------------|------|
| Debug (cached) | ~3-5 minutes |
| Debug (clean) | ~8-12 minutes |
| Release (cached) | ~4-6 minutes |
| Release (clean) | ~10-15 minutes |

First build is slower due to dependency downloads. Subsequent builds use cache.

---

## Cost

GitHub Actions is **FREE** for public repositories with unlimited minutes!

For private repositories:
- Free tier: 2,000 minutes/month
- Each build: ~5-10 minutes
- ~200-400 builds/month free

---

## Questions?

Open an issue or check the [GitHub Actions documentation](https://docs.github.com/en/actions).
