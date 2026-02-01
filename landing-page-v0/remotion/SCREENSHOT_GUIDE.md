# Care Gap Closure Video - Screenshot Capture Guide

This guide provides step-by-step instructions for capturing the screenshots needed for the Care Gap Closure Remotion video.

## Required Screenshots

All screenshots should be captured at **1920x1080 resolution or higher** for optimal quality.

### 1. care-gap-dashboard.png
**Scene:** Setup Scene (0-10s)
**URL:** `http://localhost:4200/care-gaps`

**Capture Requirements:**
- Full Care Gap Manager page showing:
  - Summary statistics cards at the top (total gaps, high urgency, etc.)
  - Care gaps table with multiple patient rows
  - Eleanor Anderson visible in the table (row should show):
    - Patient name: Eleanor Anderson
    - Age: 63
    - Measure: Breast Cancer Screening (BCS)
    - Urgency: HIGH (red badge)
    - Days overdue: 60 days
    - Status: Open

**Setup Steps:**
1. Navigate to Care Gap Manager page
2. Ensure sample data includes Eleanor Anderson with the correct attributes
3. Set browser window to 1920x1080
4. Capture full viewport (Ctrl+Shift+S or F12 → Screenshot)
5. Save as `care-gap-dashboard.png`

---

### 2. care-gap-table-eleanor.png
**Scene:** Identification Scene (10-25s)
**URL:** `http://localhost:4200/care-gaps`

**Capture Requirements:**
- Zoomed view of the care gaps table
- Eleanor Anderson's row centered and in focus
- Other patient rows visible but can be slightly blurred in post-processing
- Columns visible:
  - Patient name
  - Measure name
  - Urgency badge (HIGH)
  - Days overdue (60)
  - Action buttons

**Setup Steps:**
1. Same page as Screenshot #1
2. Use browser DevTools to zoom to table section (Ctrl+Shift+C)
3. Highlight Eleanor's table row
4. Capture zoomed view showing her row prominently
5. Save as `care-gap-table-eleanor.png`

**Alternative:** Use the same screenshot as #1 if zooming is not feasible. Remotion will handle the zoom animation.

---

### 3. care-gap-closure-dialog.png
**Scene:** Action Scene (25-45s)
**URL:** `http://localhost:4200/care-gaps` (with dialog opened)

**Capture Requirements:**
- Care Gap Closure Dialog opened for Eleanor Anderson
- Dialog showing:
  - Title: "Close Care Gap - Eleanor Anderson"
  - Patient info section
  - **Quick Action Buttons** (primary focus):
    - "Schedule Screening" (purple/primary button)
    - "Already Done" (green button)
    - "Patient Declined" (red button)
  - Form fields:
    - Closure reason textarea
    - Intervention type dropdown
  - "Close Gap" submit button

**Setup Steps:**
1. Navigate to Care Gap Manager
2. Click action button on Eleanor's row (kebab menu → "Close Gap")
3. Dialog should open
4. Ensure Quick Action buttons are visible and prominent
5. Capture dialog in opened state
6. Save as `care-gap-closure-dialog.png`

**Note:** If dialog doesn't exist yet, create a mockup in Figma/design tool matching the HDIM design system (teal/blue/purple palette).

---

### 4. care-gap-dashboard-updated.png
**Scene:** Impact Scene (45-60s)
**URL:** `http://localhost:4200/care-gaps`

**Capture Requirements:**
- Same view as Screenshot #1 BUT:
  - Summary statistics UPDATED:
    - Total gaps: **44** (was 45)
    - High urgency: **8** (was 9)
  - Eleanor Anderson's row **REMOVED** from table
  - Next patient gap visible in table

**Setup Steps:**
1. Same page as Screenshot #1
2. **Option A (Recommended):** Edit screenshot #1 in image editor:
   - Update "45" to "44" in total gaps card
   - Update "9" to "8" in high urgency card
   - Remove Eleanor's row from table
3. **Option B:** Use sample data manipulation:
   - Close Eleanor's gap via API/UI
   - Refresh page
   - Capture updated state
4. Save as `care-gap-dashboard-updated.png`

---

## Screenshot Optimization

After capturing screenshots, optimize them:

```bash
# Convert to PNG (if not already)
convert screenshot.jpg screenshot.png

# Optimize file size (lossless compression)
pngquant --quality=80-95 screenshot.png -o screenshot-optimized.png

# Or use ImageMagick
convert screenshot.png -strip -quality 85 screenshot-optimized.png
```

## Placement

Copy all screenshots to:
```
/mnt/wdblack/dev/projects/hdim-master/landing-page-v0/remotion/public/screenshots/
```

## Verification

After placing screenshots, verify they load in Remotion Studio:

```bash
cd /mnt/wdblack/dev/projects/hdim-master/landing-page-v0/remotion
npm run dev
```

Navigate to the CareGapClosure composition and verify all scenes display correctly.

## Troubleshooting

**Problem:** Screenshots not loading in Remotion
- Verify file names match exactly (case-sensitive)
- Check file permissions: `chmod 644 *.png`
- Ensure files are in `remotion/public/screenshots/` directory

**Problem:** Screenshots appear blurry
- Increase capture resolution to 2560x1440 or higher
- Use Retina/HiDPI screenshot tools
- Avoid JPEG compression, use PNG only

**Problem:** Dialog screenshot missing Quick Actions
- Ensure Clinical Portal is running latest build
- Check that BulkActionDialog component is implemented
- Use Figma mockup as fallback

## Quick Action Buttons Reference

For reference, the Quick Action buttons should appear as:

```typescript
// Schedule Screening - Primary action
backgroundColor: '#8B5CF6' (purple)
text: 'Schedule Screening'
icon: Calendar icon

// Already Done
backgroundColor: '#22C55E' (green)
text: 'Already Done'
icon: Checkmark icon

// Patient Declined
backgroundColor: '#EF4444' (red)
text: 'Patient Declined'
icon: X icon
```

---

## Next Steps

After capturing screenshots:

1. Replace placeholder screenshots in `remotion/public/screenshots/`
2. Test video in Remotion Studio: `npm run dev`
3. Render video: `npm run docker:render:caregap`
4. Extract thumbnail: `ffmpeg -i out/care-gap-closure.mp4 -ss 00:00:03 -vframes 1 -q:v 2 ../public/videos/care-gap-closure-thumb.png`
5. Integrate into landing page

---

_Last Updated: January 24, 2026_
