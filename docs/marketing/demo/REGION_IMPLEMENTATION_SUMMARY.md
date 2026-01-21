# Region-Based Screenshot Capture Implementation Summary

## Overview

Enhanced the screenshot capture script to define, capture, and validate specific regions of each page type, ensuring all critical data areas are fully loaded before capture.

## Implementation Details

### 1. Region Definitions

Added `PAGE_REGIONS` constant with region definitions for:
- **Dashboard pages**: top-stats, care-gaps, charts, tables
- **Patient list pages**: top-stats, patient-table
- **Care gap pages**: summary-stats, care-gap-list
- **Quality measure pages**: header-stats, measures-grid
- **Results pages**: results-table

Each region includes:
- `name`: Identifier for the region
- `selector`: CSS selector(s) to find the region
- `priority`: 'high', 'medium', or 'low'
- `validation`: Rules for validating data presence
  - `minElements`: Minimum number of elements expected
  - `dataCheck`: Custom async function to validate data

### 2. New Functions

#### `validateRegionData(page, region)`
- Validates that a region contains data
- Checks minimum element count
- Runs custom data check if provided
- Returns boolean indicating data presence

#### `captureRegionScreenshot(page, region, pageInfo, outputDir)`
- Captures screenshot of a specific region
- Gets bounding box of region elements
- Validates data before capture
- Saves to `regions/` subdirectory
- Returns boolean indicating success

#### `captureAllRegions(page, pageType, pageInfo, outputDir)`
- Captures all regions for a page type
- Returns summary: `{ captured, validated }`

### 3. Integration

Updated `captureScreenshot()` function to:
1. Capture full-page screenshot (existing behavior)
2. Capture region screenshots (new)
3. Validate all high-priority regions (new)
4. Log region capture results

### 4. Configuration

Added `CONFIG.captureRegions` flag (default: `true`) to enable/disable region capture.

## Output Structure

```
docs/screenshots/
├── care-manager/
│   ├── care-manager-dashboard-overview.png (full page)
│   └── regions/
│       ├── care-manager-dashboard-overview-top-stats.png
│       ├── care-manager-dashboard-overview-care-gaps.png
│       ├── care-manager-dashboard-overview-charts.png
│       └── care-manager-dashboard-overview-tables.png
└── ...
```

## Validation Rules

### High Priority Regions
- Must have minimum number of elements
- Custom data check validates content
- Warnings logged if data missing
- Screenshot still captured (non-blocking)

### Medium/Low Priority Regions
- Should have data but not blocking
- Captured if present
- No warnings if missing

## Usage

Region capture is enabled by default. To disable:

```javascript
const CONFIG = {
  captureRegions: false, // Disable region capture
  // ... other config
};
```

## Next Steps

1. Test region capture with demo environment
2. Verify region screenshots are captured correctly
3. Validate data presence in all high-priority regions
4. Adjust selectors if needed based on actual page structure
5. Add more regions for other page types as needed

## Files Modified

- `scripts/capture-screenshots.js`: Added region definitions, validation, and capture functions

## Files Created

- `docs/marketing/demo/REGION_BASED_SCREENSHOT_PLAN.md`: Detailed plan document
- `docs/marketing/demo/REGION_IMPLEMENTATION_SUMMARY.md`: This summary
