# Quick Start Guide - Push to GitHub

Follow these steps to get your app building on GitHub:

## 1️⃣ Initialize Git Repository

```bash
cd e:\2ndScannerConverter

# Initialize git
git init

# Add all files
git add .

# Create initial commit
git commit -m "Initial commit: Scanner Lab Converter v1.0.0"
```

## 2️⃣ Create GitHub Repository

### Option A: Using GitHub Website
1. Go to https://github.com/new
2. Name: `2ndScannerConverter`
3. Description: "Premium document scanner and converter with OCR"
4. Public or Private: Choose your preference
5. **Do NOT** initialize with README (we have one)
6. Click **Create repository**

### Option B: Using GitHub CLI
```bash
# Install GitHub CLI first: https://cli.github.com/
gh repo create 2ndScannerConverter --public --source=. --push
```

## 3️⃣ Connect and Push

```bash
# Add GitHub as remote (replace YOUR_USERNAME)
git remote add origin https://github.com/YOUR_USERNAME/2ndScannerConverter.git

# Push to GitHub
git branch -M main
git push -u origin main
```

## 4️⃣ Watch the Build

1. Go to your repository on GitHub
2. Click **Actions** tab
3. You'll see "Android CI/CD" workflow running
4. Wait 8-12 minutes for first build
5. Once complete, download APK from **Artifacts**

## 5️⃣ Download Your APK

### From Actions Tab:
1. Click on the green checkmark
2. Scroll to **Artifacts** section
3. Click `scanner-lab-debug` to download
4. Extract and install on Android device

### From Releases (after tagging):
```bash
# Create a release tag
git tag v1.0.0
git push origin v1.0.0

# GitHub will automatically:
# - Build the APK
# - Create a Release
# - Upload the APK
```

Then download from: `https://github.com/YOUR_USERNAME/2ndScannerConverter/releases`

---

## 🎯 Quick Commands Reference

```bash
# First time setup
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/YOUR_USERNAME/2ndScannerConverter.git
git push -u origin main

# Regular workflow
git add .
git commit -m "Your commit message"
git push

# Create release
git tag v1.0.0
git push origin v1.0.0
```

---

## ✅ What Happens Automatically

When you push code:
- ✅ GitHub Actions starts building
- ✅ Compiles Debug APK
- ✅ Compiles Release APK
- ✅ Runs Lint checks
- ✅ Uploads APKs as downloadable artifacts
- ✅ Caches dependencies for faster future builds

When you create a tag `v*.*.*`:
- ✅ Builds signed Release APK (if secrets configured)
- ✅ Creates GitHub Release
- ✅ Attaches APK to release
- ✅ Generates release notes

---

## 📱 Installing the APK

### On Your Android Device:
1. Download APK from GitHub
2. Enable "Install from Unknown Sources" in Settings
3. Open the APK file
4. Click Install
5. Enjoy your Scanner Lab Converter! 🎉

---

## 🔒 Optional: Set Up APK Signing

See `GITHUB_ACTIONS.md` for detailed instructions on:
- Generating a keystore
- Encoding it to Base64
- Adding secrets to GitHub
- Automatic signed APK releases

---

## 💡 Tips

- First build takes ~10-15 minutes (downloading dependencies)
- Subsequent builds take ~3-5 minutes (using cache)
- GitHub Actions is **FREE** for public repositories
- You can have multiple branches building simultaneously
- Download APKs are available for 90 days by default

---

## 🐛 Troubleshooting

**Build failed?**
- Check the Actions log for errors
- Ensure all files were committed
- Verify Gradle wrapper is included

**Can't find APK?**
- Build must complete successfully (green checkmark)
- Look in Artifacts section at bottom of workflow run
- Wait for entire workflow to finish

**Push rejected?**
- Make sure you replaced `YOUR_USERNAME` with your actual GitHub username
- Check repository exists and you have push access

---

## 🎉 Success!

Once your first build completes, you'll have:
- ✅ Automated builds on every push
- ✅ Downloadable APKs from GitHub
- ✅ Professional CI/CD setup
- ✅ Ready for production releases

Share the repository link with others and they can download your app! 🚀
