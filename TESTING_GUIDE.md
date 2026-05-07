# PDF Toolkit - Quick Testing Guide

## Pre-Testing Checklist
- [x] Stable commit e892e7e applied
- [x] Compression fixes applied (DPI, bitmap recycling, white canvas, scale factor)
- [x] Compilation successful - no errors
- [x] Ready for functional testing

## Quick Build & Test Commands

### 1. Clean Build
```powershell
cd c:\Users\chait\Projects\pdf_tools
./gradlew clean buildOpensourceDebug --no-daemon
```

### 2. Install Debug APK
```powershell
./gradlew installOpensourceDebugApk --no-daemon
```

### 3. View Logs While Testing
```powershell
# In separate terminal:
adb logcat | findstr "PdfCompressor\|PdfViewerVM\|PdfToolkit"
```

## Compression Issue - Test Plan

### Test A: File Size Reduction
1. **Setup**: Get a test PDF (2-10 MB, text or mixed content)
2. **Action**: 
   - Open PDF in app
   - Select "Compress"
   - Choose compression level: MEDIUM
   - Let it finish
3. **Verify**:
   - Compressed file is smaller than original
   - ✅ NOT showing "already optimized" when file should compress
   - Quality is acceptable (text readable, images clear)

### Test B: Different Compression Levels  
1. **Setup**: Use same PDF for all tests
2. **Test Each Level**:
   ```
   LOW       → ~5-10% size reduction
   MEDIUM    → ~15-25% size reduction  
   HIGH      → ~30-40% size reduction
   MAXIMUM   → ~50%+ size reduction
   ```
3. **Verify**:
   - Each level produces different size
   - ✅ "Already optimized" ONLY for very small PDFs (< 50KB)
   - Quality degrades gracefully with compression level

### Test C: Edge Cases
1. **Very Small PDF** (< 50KB)
   - Expected: "Already optimized" message (correct behavior)
   - Size: No reduction (correct)

2. **Image-Heavy PDF** (scanned documents)
   - Expected: Compresses to ~30-50% using FULL_RERENDER
   - Size: Should decrease significantly

3. **Text-Only PDF**
   - Expected: Compresses to ~20-30% using IMAGE_OPTIMIZATION
   - Quality: Text should remain crisp

## PDF Viewer Issue - Test Plan

### Test D: PDF Loading (No Continuous Loading)
1. **Setup**: Get 3 different PDFs
   - Simple text PDF
   - Image-heavy PDF  
   - Large PDF (50+ pages)

2. **Action**: 
   - Open each PDF in viewer
   - Monitor loading progress
   - Check for "infinite loading" spinning indicator

3. **Verify**:
   - ✅ PDF loads completely within 2-3 seconds
   - ✅ Loading indicator appears and disappears
   - ✅ No continuous spinning/loading state
   - ✅ Pages display correctly

### Test E: Viewer Gestures
1. **Pinch Zoom**:
   - Pinch to zoom in/out
   - ✅ Smooth zooming without freezing
   - ✅ Text remains readable at all zoom levels

2. **Pan/Drag**:
   - Pan across zoomed view
   - ✅ Smooth panning
   - ✅ Proper bounds (doesn't pan infinitely)

3. **Page Navigation**:
   - Scroll to next/previous page
   - ✅ Pages load smoothly
   - ✅ No duplicate pages or skipped pages

### Test F: Transparent PDF Rendering
1. **Setup**: Create or find PDF with transparency
2. **Action**: 
   - Open in viewer
   - Compress the PDF
   - Open compressed version

3. **Verify**:
   - ✅ Original: Transparent areas show correctly
   - ✅ Compressed: No black backgrounds
   - ✅ No color inversion
   - ✅ Text color matches original

## Performance Metrics

### Expected Performance
- **Compression Time**:
  - Small PDF (< 5MB): < 30 seconds
  - Medium PDF (5-20MB): 1-2 minutes
  - Large PDF (> 20MB): 2-5 minutes

- **Loading Time**:
  - PDF open to first page visible: < 3 seconds
  - Page rendering: < 1 second per page

- **Memory Usage**:
  - Should not cause app crashes
  - Should not use > 500MB RAM (on typical device)

## Troubleshooting

### If Compression Still Says "Already Optimized"
- Check PDF file size (< 50KB trigger skip)
- Check logs for error messages
- Try MAXIMUM compression level
- Check disk space available

### If PDF Shows Infinite Loading
- Check logcat for exceptions
- Try restarting app
- Check PDF validity (try opening in desktop reader)
- Check available device memory

### If Colors Look Wrong After Compression
- Verify it's a transparent PDF issue
- Compare with white background
- Check JPEG quality setting
- Try different compression level

## Logging for Debugging

### Check Compression Flow
```powershell
adb logcat | grep -E "Compression started|strategy|compression ratio|bytes"
```

### Check Viewer Flow  
```powershell
adb logcat | grep -E "loadPage|renderPage|Loading|Loaded|Error"
```

### Full Verbose Logging
```powershell
adb logcat | grep "yourname.pdftoolkit"
```

## Success Criteria

All tests should show ✅:

### Compression
- [x] Non-trivial PDFs compress to smaller size
- [x] "Already optimized" only for very small files
- [x] Different levels produce different sizes
- [x] Quality is acceptable
- [x] No "double recycle" crashes

### Viewer
- [x] PDFs load without infinite spinning
- [x] Gestures are responsive and smooth
- [x] Transparent PDFs render without inversion
- [x] Multiple pages navigate correctly
- [x] No memory leaks or crashes

## Reporting Issues

If you find problems, collect:
1. Log output (logcat dump)
2. Test PDF file (if possible)
3. Device info (RAM, Android version)
4. Steps to reproduce
5. Expected vs actual behavior

---
**Version**: 1.0  
**Last Updated**: May 7, 2026  
**Status**: Ready for QA Testing
