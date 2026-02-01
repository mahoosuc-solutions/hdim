# Region-Based Screenshot Capture and Validation

## Objective

Enhance screenshot capture to define, capture, and validate specific regions of each page type, ensuring all critical data areas are fully loaded before capture.

## Current State

- Full-page screenshots are captured
- Basic data validation exists
- No region-specific capture or validation

## Solution

Define regions for each page type, capture region screenshots, and validate each region contains data.

## Page Region Definitions

### Dashboard Pages

**Regions:**
1. **Top Stats Region** (High Priority)
   - Selectors: `.statistics-grid`, `app-stat-card`, `.metrics-grid`
   - Validation: 4+ stat cards with numeric values > 0

2. **Care Gaps Section** (High Priority)
   - Selectors: `.care-gaps-section`, `.care-gaps-card`, `.care-gaps-list`
   - Validation: Care gap items OR empty state message

3. **Charts/Trends Region** (Medium Priority)
   - Selectors: `canvas`, `svg`, `[class*="chart"]`
   - Validation: Chart elements rendered

4. **Data Tables Region** (Medium Priority)
   - Selectors: `table tbody tr`, `mat-table tbody tr`
   - Validation: 1+ table rows with data

### Patient List Pages

**Regions:**
1. **Top Stats Row** (High Priority)
   - Selectors: `.statistics-row`, `.stat-card`
   - Validation: 4+ stat cards with values

2. **Patient Table** (High Priority)
   - Selectors: `table tbody tr`, `mat-table tbody tr`
   - Validation: 5+ patient rows

### Care Gap Pages

**Regions:**
1. **Summary Stats** (High Priority)
   - Selectors: `.care-gap-summary`, `app-stat-card`
   - Validation: Stat cards with gap counts

2. **Care Gap List** (High Priority)
   - Selectors: `.care-gaps-list`, `.care-gap-item`
   - Validation: 3+ gap items OR empty state

### Quality Measure Pages

**Regions:**
1. **Header Stats** (High Priority)
   - Selectors: `.header-stats`, `.stat-card`
   - Validation: 2+ stat cards

2. **Measures Grid** (High Priority)
   - Selectors: `.measures-grid`, `.measure-card`
   - Validation: 5+ measure items

## Implementation Plan

### Phase 1: Define Region Configuration

**File:** `scripts/capture-screenshots.js`

Add `PAGE_REGIONS` constant with region definitions for each page type.

### Phase 2: Add Region Validation

**File:** `scripts/capture-screenshots.js`

Add `validateRegionData()` function to check regions have data.

### Phase 3: Add Region Capture

**File:** `scripts/capture-screenshots.js`

Add `captureRegionScreenshot()` function to capture specific regions.

### Phase 4: Integrate with Main Capture

**File:** `scripts/capture-screenshots.js`

Update `captureScreenshot()` to:
1. Capture full-page screenshot (existing)
2. Capture region screenshots (new)
3. Validate all high-priority regions (new)

## Files to Modify

1. `scripts/capture-screenshots.js` - Add region definitions, validation, and capture functions

## Success Criteria

- All high-priority regions captured
- All high-priority regions validated for data
- Region screenshots saved in `regions/` subdirectory
- Full-page screenshots still captured
- Validation reports show region status
