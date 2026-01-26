# Install Git for Windows - Quick Guide

## 🚀 Step 1: Download Git

**Option A: Using Browser**
1. Open: https://git-scm.com/download/win
2. Download will start automatically
3. Run the downloaded installer
4. Follow installation steps below

**Option B: Using winget (Faster)**
```powershell
winget install --id Git.Git -e --source winget
```

## 📦 Step 2: Install Git

### Installation Settings (Recommended):
1. **Select Components**: ✅ All default options
2. **Default Editor**: Choose your preferred editor (or keep default)
3. **PATH Environment**: ✅ "Git from the command line and also from 3rd-party software"
4. **HTTPS Backend**: ✅ Use OpenSSL library
5. **Line Ending Conversions**: ✅ "Checkout as-is, commit Unix-style line endings"
6. **Terminal Emulator**: ✅ Use MinTTY
7. **git pull behavior**: ✅ Default (fast-forward or merge)
8. Click **Install**

## ⏱️ Step 3: Restart Terminal

After installation:
1. Close all PowerShell/CMD windows
2. Open a NEW PowerShell window
3. Verify installation:
```powershell
git --version
```

Should show: `git version 2.x.x.windows.x`

## 🔧 Step 4: Configure Git (First Time Only)

```powershell
# Set your name
git config --global user.name "Your Name"

# Set your email (use your GitHub email)
git config --global user.email "your.email@example.com"

# Verify configuration
git config --list
```

## 🚀 Step 5: Push Your Changes

```powershell
cd e:\2ndScannerConverter

# Check status
git status

# Add updated files
git add .gitattributes .github/workflows/ gradlew GRADLEW_FIX.md WORKFLOWS_UPDATED.md

# Commit changes
git commit -m "Fix: Add automatic line ending handling for gradlew"

# Push to GitHub
git push
```

## 📊 What Happens After Push:

1. ✅ Files uploaded to GitHub
2. ✅ GitHub Actions automatically triggered
3. ✅ Line endings converted CRLF → LF
4. ✅ gradlew made executable
5. ✅ Build proceeds successfully
6. ✅ APKs created and uploaded (~10-15 min)

## 🎯 Monitor Your Build

After pushing, visit:
https://github.com/syedali-glitch/SCANNER-LAB/actions

You'll see a new workflow run starting!

## 🔐 Authentication (If Asked)

When you push, Git might ask for credentials:

**Option 1: GitHub Personal Access Token (Recommended)**
1. Go to: https://github.com/settings/tokens
2. Generate new token (classic)
3. Select scopes: `repo` (all)
4. Copy the token
5. Use token as password when pushing

**Option 2: GitHub CLI**
```powershell
# Install GitHub CLI
winget install --id GitHub.cli

# Login to GitHub
gh auth login

# Then use git push normally
```

## ✅ Verification Checklist

After installation:
- [ ] Git installed successfully
- [ ] `git --version` works
- [ ] User name and email configured
- [ ] Changed directory to project
- [ ] Staged files with `git add`
- [ ] Committed with `git commit`
- [ ] Pushed with `git push`
- [ ] Build started on GitHub Actions

## 🎉 Success!

Once pushed, your fixes will be applied and the build should succeed!

---

## ⚡ Quick Commands Reference

```powershell
# Install Git (winget)
winget install --id Git.Git -e

# Configure Git
git config --global user.name "Your Name"
git config --global user.email "your@email.com"

# Push changes
cd e:\2ndScannerConverter
git add .
git commit -m "Fix: Line ending handling"
git push
```

## 🆘 Troubleshooting

**Git not recognized after install:**
- Close and reopen PowerShell
- Or restart your computer

**Permission denied (publickey):**
- Use HTTPS URL: https://github.com/syedali-glitch/SCANNER-LAB.git
- Or set up SSH keys

**Authentication failed:**
- Use Personal Access Token instead of password
- Or use GitHub CLI: `gh auth login`

---

Ready to install and push! 🚀
