# Rewarded Ad Integration - Usage Guide

## Overview
The app now includes **rewarded ads** as a voluntary monetization layer. Users can watch short video ads to unlock premium features like PDF merge/share without payment.

## Implementation Details

### AdManager Enhancements
[AdManager.kt](file:///c:/Users/pc/Desktop/PlainLabs/app/src/main/java/com/plainlabs/qrpdftools/ads/AdManager.kt) now supports:
- `preloadRewardedAd()` - Preloads rewarded video ad
- `showRewardedAd(activity, onRewardEarned, onAdDismissed)` - Shows ad with callbacks
- `isRewardedAdReady()` - Checks if ad is loaded and ready

### RewardedFeatureDialog
[RewardedFeatureDialog.kt](file:///c:/Users/pc/Desktop/PlainLabs/app/src/main/java/com/plainlabs/qrpdftools/ui/RewardedFeatureDialog.kt) provides:
- User-friendly dialog explaining the feature unlock
- "Watch Ad" button (enabled when ad is ready)
- Link to "Remove Ads" purchase
- Reward callback on successful ad view

## Usage Example

### Gating a Feature Behind Rewarded Ad

```kotlin
// In your activity/fragment
fun onPdfMergeClick() {
    val dialog = RewardedFeatureDialog(
        context = requireContext(),
        featureName = "PDF Merge",
        featureDescription = "Combine multiple PDFs into one document",
        adManager = adManager,
        onRewardEarned = {
            // User watched ad, grant access
            proceedWithPdfMerge()
            Toast.makeText(
                requireContext(),
                "Feature unlocked! You can merge PDFs now.",
                Toast.LENGTH_SHORT
            ).show()
        },
        onRemoveAdsClick = {
            // User wants to remove ads permanently
            showSettings()
        }
    )
    dialog.show()
}

private fun proceedWithPdfMerge() {
    // Launch PDF merge activity
    startActivity(Intent(requireContext(), PdfMergeActivity::class.java))
}
```

### Alternative: Direct Ad Show (No Dialog)

```kotlin
fun unlockFeatureWithAd() {
    if (adManager.isRewardedAdReady()) {
        adManager.showRewardedAd(
            activity = this,
            onRewardEarned = {
                // Grant feature access
                unlockPremiumFeature()
            },
            onAdDismissed = {
                // Ad closed without reward
                Toast.makeText(this, "Watch the full ad to unlock", Toast.LENGTH_SHORT).show()
            }
        )
    } else {
        Toast.makeText(this, "Ad loading, please try again", Toast.LENGTH_SHORT).show()
        adManager.preloadRewardedAd()
    }
}
```

## Recommended Feature Gates

### High-Value Features (Best for Rewarded Ads)
1. **PDF Merge** - Combine 2+ PDFs (limited to 1 merge per ad)
2. **PDF Split** - Extract pages from PDF
3. **Batch Export** - Export all scans to CSV/JSON
4. **Advanced Share** - Share to multiple apps simultaneously
5. **QR Code Generation** - Create custom QR codes from text

### Implementation Pattern

```kotlin
class FeatureUnlockManager(
    private val context: Context,
    private val adManager: AdManager
) {
    
    private val unlockedFeatures = mutableSetOf<String>()
    
    fun requestFeatureAccess(
        feature: String,
        featureDescription: String,
        onGranted: () -> Unit
    ) {
        // Check if user removed ads
        if (!adManager.shouldShowAds()) {
            onGranted()
            return
        }
        
        // Check if already unlocked this session
        if (unlockedFeatures.contains(feature)) {
            onGranted()
            return
        }
        
        // Show rewarded ad dialog
        RewardedFeatureDialog(
            context = context,
            featureName = feature,
            featureDescription = featureDescription,
            adManager = adManager,
            onRewardEarned = {
                unlockedFeatures.add(feature)
                onGranted()
            },
            onRemoveAdsClick = {
                // Navigate to settings
            }
        ).show()
    }
}
```

## Best Practices

### 1. Timing
- ✅ Show rewarded ad opportunity BEFORE user attempts the action
- ✅ Make it clear what they'll unlock
- ❌ Don't interrupt ongoing tasks

### 2. Frequency
- ✅ Allow unlimited rewarded ads (user-initiated)
- ✅ Grant access per session or per use
- ❌ Don't force rewarded ads

### 3. UX
- ✅ Clearly communicate the value
- ✅ Always offer IAP alternative
- ✅ Handle ad load failures gracefully
- ❌ Don't penalize users if ad fails to load

### 4. Free Users Without Ads Removed
- They see the "Watch Ad" button
- Can unlock features repeatedly
- Encouraged to purchase Remove Ads

### 5. Paid Users (Ads Removed)
- `showRewardedAd()` immediately calls `onRewardEarned()`
- No ad shown, instant feature access
- Seamless premium experience

## Monetization Strategy Summary

| Ad Type | Trigger | Frequency | CPM Estimate |
|---------|---------|-----------|--------------|
| **Banner** | Always visible | Continuous | $0.10-$0.50 |
| **Interstitial** | First scan/save | 1-2 per session | $1-$3 |
| **Rewarded** | User-initiated | Unlimited | $2-$5 |
| **IAP** | Remove Ads button | One-time $1.99 | 100% revenue |

## Testing

### Test with Google Test Ads
The app uses test ad unit IDs by default:
```kotlin
const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
```

### Test Scenarios
1. ✓ Ad loads successfully → user watches → reward granted
2. ✓ Ad fails to load → graceful fallback message
3. ✓ User closes ad early → no reward
4. ✓ User has removed ads → instant reward
5. ✓ Ad not ready → disable button, show "Loading..."

## Revenue Projections

**Assumptions:** 50,000 MAU, 10% use rewarded features
- Rewarded ad views: 5,000/month
- CPM: $3.50 (average)
- **Revenue: $17.50/month from rewarded ads**

Combined with banner ($125) + interstitial ($200) + IAP ($1,990):
**Total: ~$2,332/month**

## Future Enhancements
- Session-based feature unlocks (resets daily)
- Premium tier with unlimited access
- Cloud sync requiring rewarded ad/IAP
- Custom QR code designs via rewarded ad
