# In-App Review System Implementation

This document describes the smart in-app review system implemented for the PDF Toolkit Android application.

## Overview

The review system intelligently prompts users to rate the app based on:
- **Feature usage** (≥3 times across PDF Viewer, Merge, Split, Lock, Compress)
- **Session time** (≥10 minutes)
- **Cooldown period** (>3 days since last prompt)
- **User has not rated** (permanent suppression after rating)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    ReviewIntegration                        │
│              (High-level API for apps)                      │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
┌───────▼────────┐            ┌────────▼────────┐
│  UsageTracker  │            │  ReviewManager  │
│                │            │                 │
│ - Track usage  │            │ - Trigger logic │
│ - Session time │            │ - Play Review   │
│                │            │ - Play Store    │
└───────┬────────┘            └────────┬────────┘
        │                              │
        │         ┌────────────────────┘
        │         │
┌───────▼─────────▼────────────────┐
│      ReviewPreferences             │
│                                    │
│  - Thread-safe SharedPreferences   │
│  - Usage counts, session time    │
│  - Last prompt, hasRated flag      │
└────────────────────────────────────┘
```

## Files Created

### Core Components (src/main)

1. **`ReviewPreferences.kt`** - Thread-safe data storage
   - Feature usage tracking (PDF Viewer, Merge, Split, Lock, Compress)
   - Session time persistence
   - Last review prompt timestamp
   - `hasRated` flag for permanent suppression
   - Uses `Mutex` for thread-safe SharedPreferences access

2. **`UsageTracker.kt`** - Feature usage and session tracking
   - Atomic counters for thread-safe tracking
   - Automatic foreground/background detection via `ReviewLifecycleTracker`
   - Periodic session persistence (every 30 seconds)
   - Accumulates session time across app restarts

3. **`ReviewLifecycleTracker.kt`** - Application lifecycle handler
   - Implements `Application.ActivityLifecycleCallbacks`
   - Tracks app foreground/background state
   - Triggers session time save on background
   - Resume tracking on foreground

4. **`ReviewIntegration.kt`** - Simple integration API
   - One-line initialization: `ReviewIntegration.initialize(app)`
   - Feature tracking methods
   - Review request with callbacks
   - Debug utilities

5. **`ReviewLogger.kt`** - Structured logging
   - Trigger condition logging
   - Flow success/failure logging
   - Debug/Info/Error log levels
   - All logs tagged with "InAppReview"

### Flavor-Specific Components

**Play Store** (`src/playstore/ReviewManager.kt`):
- Uses Google Play In-App Review API
- Native in-app review dialog
- Fallback to Play Store on API failure

**F-Droid** (`src/fdroid/ReviewManager.kt`):
- Redirects to Play Store web page
- No Google Play Services dependency
- Same trigger logic

### Test Suite (src/test)

**`ReviewSystemTest.kt`** - Comprehensive unit tests:
1. Multiple feature usage accumulation
2. Review trigger threshold validation
3. Session time tracking across app cycles
4. Data persistence across app restarts
5. Already rated user suppression
6. Cooldown period enforcement
7. Thread-safe concurrent operations
8. Review statistics calculation

## Integration Guide

### 1. Initialize in Application Class

Already added to `PdfToolkitApplication.kt`:

```kotlin
override fun onCreate() {
    super.onCreate()
    
    // Initialize In-App Review system
    ReviewIntegration.initialize(this)
    
    // ... other initialization
}
```

### 2. Track Feature Usage

Add tracking calls in your feature screens:

```kotlin
// PDF Viewer screen
ReviewIntegration.trackPdfViewerUsage(context)

// Merge screen (after successful merge)
ReviewIntegration.trackMergeUsage(context)

// Split screen (after successful split)
ReviewIntegration.trackSplitUsage(context)

// Lock/Protect screen
ReviewIntegration.trackLockUsage(context)

// Compress screen
ReviewIntegration.trackCompressUsage(context)
```

### 3. Request Review at Appropriate Times

Best practices for review timing:
- After user completes a successful PDF operation
- When user navigates to home screen after task
- Avoid interrupting active workflows

```kotlin
// Example: After successful PDF merge
ReviewIntegration.requestReview(activity) { success ->
    if (success) {
        Log.d("Review", "Review flow shown or Play Store opened")
    }
}
```

### 4. Check Review Eligibility (Optional)

For custom UI or analytics:

```kotlin
ReviewIntegration.shouldRequestReview(context) { shouldRequest ->
    if (shouldRequest) {
        // Show your own pre-review UI or analytics
    }
}
```

### 5. Get Statistics (Debug/Analytics)

```kotlin
ReviewIntegration.getReviewStats(context) { stats ->
    Log.d("Review", """
        Total usage: ${stats.totalFeatureUsage}
        Session time: ${stats.totalSessionTimeMinutes} min
        Can request: ${stats.canRequestReview}
        Has rated: ${stats.hasRated}
    """.trimIndent())
}
```

## Trigger Conditions

Review prompt only appears when **ALL** conditions are met:

| Condition | Threshold | Description |
|-----------|-----------|-------------|
| Feature Usage | ≥3 times | Total across all 5 tracked features |
| Session Time | ≥10 minutes | Cumulative foreground time |
| Cooldown | >3 days | Since last prompt |
| Rated Status | false | User has not rated yet |

## Behavior

### After Review Flow
- `hasRated` flag set to `true`
- Session time reset to 0
- All future prompts permanently suppressed
- Usage counts preserved (for analytics)

### API Failure Handling
- If Google Play In-App Review API fails → redirects to Play Store
- If Play Store app unavailable → opens browser with Play Store URL
- User still marked as rated to prevent spam

### No Spam Protection
- 3-day cooldown between prompts
- Single `hasRated` flag prevents all future prompts
- Review only triggers after meaningful engagement

## Thread Safety

All components are thread-safe:
- `ReviewPreferences` uses `Mutex` with `withLock`
- `UsageTracker` uses `AtomicLong`/`AtomicBoolean`
- All coroutine operations use `Dispatchers.IO` for disk I/O
- No race conditions in concurrent feature tracking

## Testing

### Unit Tests

Run tests with:
```bash
./gradlew test
```

### Manual Testing Scenarios

1. **Fresh Install Test**
   - Install app fresh
   - Verify no immediate review prompt
   - Use 3+ features for 10+ minutes
   - Verify prompt appears

2. **Already Rated Test**
   - Complete review flow
   - Use app extensively
   - Verify no further prompts

3. **Cooldown Test**
   - Trigger review prompt
   - Close and reopen app immediately
   - Verify prompt doesn't reappear
   - Wait 3+ days
   - Verify prompt can appear again

4. **App Restart Test**
   - Track some usage
   - Kill app and restart
   - Verify usage counts persisted
   - Verify session time accumulates

5. **Background Test**
   - Start using app
   - Send to background (home button)
   - Return to app
   - Verify session time tracked correctly

## Logging

Enable debug logging:

```kotlin
ReviewIntegration.setLogLevel(ReviewLogger.LogLevel.DEBUG)
```

Log output example:
```
D/InAppReview: Feature used: PDF_VIEWER
D/InAppReview: Trigger Check:
    - Feature Usage: 5 (min: 3)
    - Session Time: 650s (min: 600s)
    - Days Since Last Prompt: 4 (min: 3)
    - Has Rated: false
    - Should Trigger: true
I/InAppReview: Review flow launched
I/InAppReview: Review flow completed successfully
I/InAppReview: User marked as rated - all future prompts suppressed
```

## Configuration

Modify thresholds in `ReviewManager.kt`:

```kotlin
companion object {
    const val MIN_FEATURE_USAGE = 3
    const val MIN_SESSION_TIME_MS = 10 * 60 * 1000L // 10 minutes
    const val DAYS_BETWEEN_PROMPTS = 3L
}
```

## Reset for Testing

Clear all review data:

```kotlin
ReviewIntegration.resetAllData(context)
```

**Warning**: This permanently deletes all tracking data. Only for testing.

## API Reference

### ReviewIntegration (Main API)

| Method | Description |
|--------|-------------|
| `initialize(app)` | Initialize the review system |
| `trackPdfViewerUsage(ctx)` | Track PDF viewer usage |
| `trackMergeUsage(ctx)` | Track merge usage |
| `trackSplitUsage(ctx)` | Track split usage |
| `trackLockUsage(ctx)` | Track lock/protect usage |
| `trackCompressUsage(ctx)` | Track compress usage |
| `requestReview(activity, callback)` | Request review if conditions met |
| `shouldRequestReview(ctx, callback)` | Check if review should be shown |
| `getReviewStats(ctx, callback)` | Get current statistics |
| `markAsRated(ctx)` | Manually mark user as rated |
| `hasUserRated(ctx, callback)` | Check if user has rated |
| `setLogLevel(level)` | Set logging verbosity |
| `resetAllData(ctx)` | Reset all data (testing) |

## Troubleshooting

### Review Not Showing

1. Check logcat for "InAppReview" logs
2. Verify thresholds are met
3. Check if `hasRated` is false in preferences
4. Verify Play Store API availability (Play Store flavor only)

### Data Not Persisting

1. Ensure `ReviewIntegration.initialize()` is called
2. Check `UsageTracker.onAppBackground()` is triggered
3. Verify SharedPreferences file `review_preferences` exists

### Thread Safety Issues

All operations are thread-safe by design. If you see issues:
1. Check you're using the singleton instances
2. Avoid creating multiple `UsageTracker` instances
3. Use `ReviewIntegration` methods rather than direct class instantiation
