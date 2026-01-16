# Region-Based Screenshot Capture and Validation Plan

## Objective

Define specific regions on each page type, capture screenshots of those regions, and validate that each region contains fully loaded data before capturing.

## Page Region Definitions

### Dashboard Pages (`/dashboard`)

**Regions:**

1. **Top Stats Region** (Priority: High)
   - Selector: `.statistics-grid`, `app-stat-card`, `.metrics-grid`
   - Contains: Stat cards with numbers (Total Patients, Evaluations, Compliance, etc.)
   - Validation: At least 4 stat cards visible with numeric values > 0
   - Position: Top of page, above fold

2. **Care Gaps Section** (Priority: High)
   - Selector: `.care-gaps-section`, `.care-gaps-card`, `.care-gaps-list`
   - Contains: Urgent care gaps, patient names, gap descriptions
   - Validation: At least 1 care gap item OR "No urgent care gaps" message
   - Position: Below stats, top-middle

3. **Charts/Trends Region** (Priority: Medium)
   - Selector: `canvas`, `svg`, `[class*="chart"]`, `.compliance-trend-chart`
   - Contains: Compliance trends, performance charts
   - Validation: Chart element visible and rendered
   - Position: Middle section

4. **Data Tables Region** (Priority: Medium)
   - Selector: `table tbody tr`, `mat-table tbody tr`, `.recent-activity-table`
   - Contains: Recent evaluations, activity lists
   - Validation: At least 1 table row with data
   - Position: Bottom section

### Patient List Pages (`/patients`)

**Regions:**

1. **Top Stats Row** (Priority: High)
   - Selector: `.statistics-row`, `.stat-card`
   - Contains: Total Patients, Active Patients, Average Age, Gender Distribution
   - Validation: At least 4 stat cards with numeric values
   - Position: Top of page

2. **Search/Filter Region** (Priority: Medium)
   - Selector: `.filter-card`, `.search-field`, `.filter-form`
   - Contains: Search input, filter dropdowns
   - Validation: Search field visible and enabled
   - Position: Below stats

3. **Patient Table Region** (Priority: High)
   - Selector: `table tbody tr`, `mat-table tbody tr`, `.patient-list`
   - Contains: Patient rows with names, MRNs, details
   - Validation: At least 5 table rows with patient data
   - Position: Main content area

### Care Gap Pages (`/care-gaps`)

**Regions:**

1. **Summary Stats Region** (Priority: High)
   - Selector: `.care-gap-summary`, `.statistics-row`, `app-stat-card`
   - Contains: Total gaps, high/medium/low priority counts
   - Validation: Stat cards show gap counts > 0
   - Position: Top of page

2. **Care Gap List Region** (Priority: High)
   - Selector: `.care-gaps-list`, `.care-gap-item`, `table tbody tr`
   - Contains: Care gap cards or table rows
   - Validation: At least 3 care gap items OR empty state message
   - Position: Main content

3. **Filter/Search Region** (Priority: Low)
   - Selector: `.filter-card`, `.search-field`
   - Contains: Search and filter controls
   - Validation: Controls visible
   - Position: Above list

### Quality Measure Pages (`/quality-measures`)

**Regions:**

1. **Header Stats Region** (Priority: High)
   - Selector: `.header-stats`, `.stat-card`, `.page-header`
   - Contains: Attributed Patients count, Active Measures count
   - Validation: At least 2 stat cards with values
   - Position: Top header

2. **Measures Grid/Table Region** (Priority: High)
   - Selector: `.measures-grid`, `.measure-card`, `table tbody tr`
   - Contains: Quality measure cards or table rows
   - Validation: At least 5 measure items visible
   - Position: Main content

3. **Filter Region** (Priority: Medium)
   - Selector: `.filters-card`, `.filters-row`
   - Contains: Search and category filters
   - Validation: Filter controls visible
   - Position: Above measures

### Patient Detail Pages (`/patients/{id}`)

**Regions:**

1. **Patient Header Region** (Priority: High)
   - Selector: `.patient-header`, `.patient-info`, `.patient-name`
   - Contains: Patient name, MRN, demographics
   - Validation: Patient name and MRN visible
   - Position: Top of page

2. **Vitals/Summary Region** (Priority: High)
   - Selector: `.vitals-section`, `.summary-card`, `app-stat-card`
   - Contains: Vital signs, summary stats
   - Validation: At least 3 vital signs or summary cards
   - Position: Top-middle

3. **Conditions/Medications Region** (Priority: Medium)
   - Selector: `.conditions-list`, `.medications-list`, `table tbody tr`
   - Contains: Active conditions, medications
   - Validation: List items or table rows visible
   - Position: Middle section

### Results/Evaluation Pages (`/results`, `/cql-results`)

**Regions:**

1. **Results Table Region** (Priority: High)
   - Selector: `table tbody tr`, `mat-table tbody tr`, `.results-list`
   - Contains: Evaluation results, quality measure results
   - Validation: At least 5 result rows with data
   - Position: Main content

2. **Filter/Chart Region** (Priority: Medium)
   - Selector: `.filters-card`, `canvas`, `svg`
   - Contains: Filters and result charts
   - Validation: Filters visible OR charts rendered
   - Position: Above table

## Implementation Strategy

### Region Definition Structure

```javascript
const PAGE_REGIONS = {
  dashboard: [
    {
      name: 'top-stats',
      selector: '.statistics-grid, app-stat-card, .metrics-grid',
      priority: 'high',
      validation: {
        minElements: 4,
        dataCheck: (elements) => {
          // Check stat cards have numeric values
          return elements.some(el => {
            const text = el.textContent;
            return /\d+/.test(text) && parseInt(text) > 0;
          });
        }
      },
      position: { top: 0, height: 300 } // Approximate pixel position
    },
    {
      name: 'care-gaps',
      selector: '.care-gaps-section, .care-gaps-card, .care-gaps-list',
      priority: 'high',
      validation: {
        minElements: 1,
        dataCheck: (elements) => {
          // Check for care gap items or empty state
          return elements.length > 0;
        }
      },
      position: { top: 300, height: 400 }
    },
    // ... more regions
  ],
  patients: [
    // ... patient page regions
  ],
  // ... other page types
};
```

### Region Capture Function

```javascript
async function captureRegionScreenshot(page, region, pageType, outputDir, pageInfo) {
  try {
    // Wait for region to be visible
    await page.waitForSelector(region.selector, { timeout: 10000 });
    
    // Get element bounding box
    const element = await page.$(region.selector);
    if (!element) {
      log(`Warning: Region ${region.name} not found`, 'warning');
      return false;
    }
    
    const box = await element.boundingBox();
    if (!box) {
      log(`Warning: Region ${region.name} has no bounding box`, 'warning');
      return false;
    }
    
    // Validate region has data
    const hasData = await validateRegionData(page, region);
    if (!hasData && region.priority === 'high') {
      log(`Warning: High-priority region ${region.name} may not have data`, 'warning');
    }
    
    // Capture region screenshot
    const filename = `${pageInfo.name}-${region.name}.png`;
    const filepath = path.join(outputDir, 'regions', filename);
    
    await page.screenshot({
      path: filepath,
      clip: {
        x: box.x,
        y: box.y,
        width: box.width,
        height: Math.min(box.height, 2000) // Limit height
      }
    });
    
    log(`  Captured region: ${region.name}`, 'success');
    return true;
  } catch (error) {
    log(`  Failed to capture region ${region.name}: ${error.message}`, 'error');
    return false;
  }
}
```

### Region Validation Function

```javascript
async function validateRegionData(page, region) {
  try {
    const elements = await page.$$(region.selector);
    
    if (elements.length < region.validation.minElements) {
      return false;
    }
    
    // Run custom data check if provided
    if (region.validation.dataCheck) {
      const results = await Promise.all(
        elements.map(el => page.evaluate(region.validation.dataCheck, el))
      );
      return results.some(result => result === true);
    }
    
    // Default: check element has text content
    const hasContent = await Promise.all(
      elements.map(el => el.textContent().then(text => text.trim().length > 0))
    );
    
    return hasContent.some(has => has);
  } catch (e) {
    return false;
  }
}
```

## Files to Modify

1. **`scripts/capture-screenshots.js`**
   - Add `PAGE_REGIONS` configuration
   - Add `captureRegionScreenshot()` function
   - Add `validateRegionData()` function
   - Add `captureAllRegions()` function
   - Update `captureScreenshot()` to capture regions
   - Create `regions/` subdirectory for region screenshots

## Validation Rules

### High Priority Regions
- Must have data before proceeding
- Wait up to 30 seconds for data
- Log warning if no data found
- Still capture screenshot but mark as incomplete

### Medium Priority Regions
- Should have data but not blocking
- Wait up to 15 seconds
- Log info if no data found

### Low Priority Regions
- Optional validation
- Capture if present
- No blocking if missing

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

## Success Criteria

- All high-priority regions captured for each page
- All high-priority regions validated for data
- Region screenshots saved separately
- Full-page screenshots still captured
- Validation reports generated
