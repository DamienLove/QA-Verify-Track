# QA Verify & Track - Testing Guide

## 🚀 Quick Start - Fixed Issues

### ✅ What's Fixed:
1. **User profiles now auto-create** - FREE profile is created automatically on login
2. **Added "Create Issue" button** - Floating Action Button (FAB) in DashboardScreen
3. **Fallback profile creation** - HomeViewModel ensures profile exists even if signup failed

---

## 📱 Install the App

1. Uninstall any previous version
2. Install: `androidApp/app/build/outputs/apk/qa/debug/app-qa-debug.apk`
3. Open the app

---

## 🆓 Testing FREE Account (Automatic)

### Step 1: Create Account
1. Open the app
2. Tap **"Sign Up"**
3. Email: `test-free@example.com`
4. Password: `Test123!`
5. Submit

### Step 2: Verify FREE Profile Created
**The profile should now auto-create!** Check Firebase:
1. Go to Firebase Console: https://console.firebase.google.com
2. Select your project
3. Go to **Firestore Database**
4. Look for `user_profiles` collection
5. You should see your user document with:
   - `tier: "FREE"`
   - `repoLimit: 2`
   - `showAds: true`

### Step 3: Test FREE Features
✅ **Should Work:**
- See "0/2 repos" badge in HomeScreen
- Add first repository
- Add second repository
- View issues in dashboard
- Mark issues fixed/open/blocked
- **NEW: Tap FAB (+) button to create new issue**

❌ **Should Be Locked (shows lock icon):**
- AI Analysis button
- Merge/Deny PR buttons
- Export button
- Advanced Filters button

🎯 **Should See Ads:**
- Banner ad at bottom of HomeScreen
- Banner ad at bottom of DashboardScreen
- Interstitial ad after 5 issue actions

---

## 💎 Testing PRO Account

### Option A: If Auto-Creation Worked
1. Go to Firebase Console > Firestore Database
2. Find your user in `user_profiles/{userId}`
3. Edit the document
4. Change these fields:
   - `tier: "PRO"`
   - `repoLimit: -1`
   - `showAds: false`
   - `aiAnalysisRemaining: -1`
   - Add: `subscriptionExpiresAt: 1893456000000`
5. Save
6. **Force close and reopen the app**

### Option B: If Auto-Creation Failed (Manual Creation)
1. Go to Firebase Console > Firestore Database
2. If no `user_profiles` collection exists, create it:
   - Click "Start collection"
   - Collection ID: `user_profiles`
3. Add document:
   - Document ID: Your Firebase Auth UID (get from Authentication > Users)
   - Add fields:
     ```
     userId: [your UID] (string)
     email: test-free@example.com (string)
     tier: PRO (string)
     repoLimit: -1 (number)
     aiAnalysisRemaining: -1 (number)
     showAds: false (boolean)
     subscriptionExpiresAt: 1893456000000 (number)
     createdAt: 1736726400000 (number)
     ```
4. Save
5. **Force close and reopen the app**

### Verify PRO Features
✅ **Should Work:**
- See "X/∞ repos" badge
- Add unlimited repositories
- No ads visible
- AI button shows robot icon (works)
- Merge/Deny buttons show action icons (work)
- Export button shows download icon (works)
- Advanced Filters button works
- **NEW: FAB (+) button to create issues**

---

## 🐛 Creating New Issues

### In DashboardScreen:
1. Navigate to a repository
2. Look for the **green circular button** at bottom-right (FAB)
3. Tap the **+** button
4. You'll be taken to QuickIssueScreen
5. Fill in issue details
6. Submit

---

## 🔍 Troubleshooting

### Problem: No user_profiles collection in Firestore
**Solution:**
1. The app should create it automatically on login
2. If not, force close app and reopen
3. Check Firebase Console again
4. If still missing, manually create it (see Option B above)

### Problem: Profile exists but tier is not changing
**Solution:**
1. Force close the app completely
2. Clear app data (Settings > Apps > QA Verify & Track > Clear Data)
3. Reopen and login again

### Problem: Can't see FAB button to create issues
**Solution:**
1. Make sure you're in DashboardScreen (not HomeScreen)
2. Navigate to a repository first
3. Look for green circular button at bottom-right
4. If not visible, try rotating device or scrolling

### Problem: Ads not showing for FREE users
**Solution:**
- Ads may take a few minutes to load on first launch
- Check your internet connection
- AdMob needs time to serve first ad impression

---

## 📊 Testing Checklist

### FREE Account
- [ ] Sign up creates account
- [ ] user_profiles/{userId} document exists in Firestore
- [ ] HomeScreen shows "0/2 repos"
- [ ] Can add 2 repositories
- [ ] 3rd repository shows paywall
- [ ] Banner ads visible
- [ ] Lock icons on Pro features
- [ ] FAB (+) button creates new issue
- [ ] After 5 issue actions, interstitial ad shows

### PRO Account
- [ ] Firestore tier field shows "PRO"
- [ ] HomeScreen shows "X/∞ repos"
- [ ] No ads visible anywhere
- [ ] Can add 3+ repositories
- [ ] AI button works (robot icon)
- [ ] Merge/Deny buttons work
- [ ] Export button creates CSV
- [ ] Advanced Filters sheet opens
- [ ] FAB (+) button creates new issue
- [ ] ProfileScreen shows PRO badge

---

## 🎯 Key Visual Indicators

### FREE User:
```
┌─────────────────────────┐
│ Repositories     0/2    │ ← Limited
│ ┌─────────────────────┐ │
│ │ Repo 1              │ │
│ └─────────────────────┘ │
│                         │
│ 🔒 AI  🔒 Merge        │ ← Locked
│                         │
│ [Banner Ad]             │ ← Ads
│                    [+]  │ ← FAB button
└─────────────────────────┘
```

### PRO User:
```
┌─────────────────────────┐
│ Repositories     3/∞    │ ← Unlimited
│ ┌─────────────────────┐ │
│ │ Repo 1              │ │
│ │ Repo 2              │ │
│ │ Repo 3              │ │
│ └─────────────────────┘ │
│                         │
│ 🤖 AI  ✓ Merge         │ ← Unlocked
│                         │
│ (No ads)                │ ← Ad-free
│                    [+]  │ ← FAB button
└─────────────────────────┘
```

---

Last Updated: 2026-01-12
Version: 3.1 (versionCode 4)
