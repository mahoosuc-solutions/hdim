# Measure Builder - Getting Started Guide

**Estimated Time:** 15 minutes
**Level:** Beginner
**Last Updated:** January 18, 2026
**Version:** 1.0

---

## Overview

Welcome to Measure Builder! This guide will walk you through creating your first quality measure using our intuitive visual interface. By the end of this guide, you'll be able to:

- Navigate the Measure Builder interface
- Create a new measure from scratch
- Configure algorithm blocks
- Set up measure parameters
- Generate CQL code
- Publish your measure

**What is Measure Builder?**

Measure Builder is a visual development environment for creating HEDIS and CQL-based quality measures without needing to write complex code. It provides:

- **Visual Algorithm Designer** - Drag-and-drop interface for measure logic
- **Interactive Configuration** - Sliders for parameters and thresholds
- **Automatic CQL Generation** - No code required
- **Real-Time Validation** - Catch errors immediately
- **One-Click Publishing** - Deploy measures instantly

---

## Prerequisites

Before you begin, ensure you have:

- ✅ Access to Measure Builder (your organization has enabled it)
- ✅ User account with EVALUATOR role or higher
- ✅ Basic understanding of quality measures (HEDIS measures are ideal for learning)
- ✅ Web browser (Chrome 90+, Firefox 88+, Safari 14+)
- ✅ JavaScript enabled in your browser

**Check Your Access:**

1. Log in to your organization's clinical portal
2. Look for "Measure Builder" in the main menu
3. If you don't see it, contact your administrator

---

## Key Concepts

### Measure Basics

A **quality measure** evaluates clinical performance against specific criteria:

```
┌─────────────────────────────────────────┐
│  Measure Structure                      │
├─────────────────────────────────────────┤
│  1. Population Definition                │
│     Who are we measuring? (age, disease)│
│                                          │
│  2. Algorithm Logic                      │
│     What are we checking for? (care gap)│
│                                          │
│  3. Thresholds & Targets                │
│     What is acceptable? (benchmarks)    │
│                                          │
│  4. CQL Code                             │
│     How do we evaluate? (auto-generated) │
└─────────────────────────────────────────┘
```

### Algorithm Blocks

Measures are built from visual blocks that represent logical operations:

| Block Type | Purpose | Example |
|-----------|---------|---------|
| **Condition** | Filter population | Age > 18 |
| **Observation** | Measure a value | Blood pressure reading |
| **Procedure** | Check if action taken | Medication dispensed |
| **Connector** | Combine logic | AND, OR |
| **Calculation** | Compute result | Average, Sum |

---

## Step-by-Step: Create Your First Measure

### Step 1: Navigate to Measure Builder (2 minutes)

1. Log in to your clinical portal
2. Click **"Measure Builder"** in the left sidebar
3. You'll see the main dashboard with:
   - **Recent Measures** - Your previous work
   - **Templates** - Pre-built measure starters
   - **My Measures** - All your created measures

**Screenshot location:** Main dashboard showing the three sections

### Step 2: Create New Measure (3 minutes)

**Option A: From Scratch**
1. Click **"New Measure"** button (top right)
2. Enter measure details:
   - **Name:** e.g., "Diabetes Care Quality"
   - **Description:** Brief summary of what you're measuring
   - **Category:** Select from HEDIS categories
   - **Version:** Default is 1.0
3. Click **"Create"**

**Option B: From Template**
1. Click **"Use Template"** on any template
2. Review the pre-configured structure
3. Customize for your needs

**Screenshot location:** New Measure dialog showing fields

### Step 3: Configure Population (3 minutes)

Now you're in the visual builder. First, define your population:

1. **Click** the "Population" section at the top
2. **Add a Condition Block:**
   - Click **"Add Block"**
   - Select **"Condition"**
   - Choose criteria: e.g., "Age >= 18"
3. **Add another condition** if needed: e.g., "Diagnosis = Diabetes"
4. **Connect blocks** with AND/OR logic
5. **Click Green checkmark** to validate

**Valid Population Example:**
```
[Age >= 18] AND [ICD10 = E11*]
→ Patients aged 18+ with diabetes diagnosis
```

**Screenshot location:** Visual canvas with sample blocks

### Step 4: Add Algorithm Logic (4 minutes)

Now define what you're checking for:

1. **Click** in the main canvas area
2. **Drag a Procedure block** onto canvas
3. **Configure the block:**
   - Click the block to edit
   - Set procedure: e.g., "Hemoglobin A1C Test"
   - Set date range: "Within last 12 months"
4. **Add additional blocks** for your measure logic
5. **Connect blocks** to show relationships
6. **Validate** by clicking green checkmark

**Sample Algorithm:**
```
[Population: Diabetic patients]
        ↓
[Did they have A1C test?]
        ↓
[Is A1C < 9%?]
        ↓
[Measure passes if YES]
```

**Screenshot location:** Canvas showing connected blocks

### Step 5: Configure Sliders & Parameters (3 minutes)

Set the specific thresholds and parameters:

1. **Click** the slider icon on your measure
2. **Configure Thresholds:**
   - **Range Slider:** Min (e.g., 0) to Max (e.g., 15%)
   - **Threshold Value:** Benchmark (e.g., 9%)
3. **Distribution Settings:**
   - If applicable, set weight distribution across sub-populations
4. **Time Period:**
   - Select lookback period: 1 month, 6 months, 1 year, 3 years
5. **Click "Apply"** to save settings

**Screenshot location:** Slider configuration panel

### Step 6: Generate & Review CQL (2 minutes)

The system automatically generates CQL from your visual blocks:

1. **Click** "View CQL" button
2. **Review the generated code** (read-only)
3. **Check for any issues** - System will flag invalid CQL
4. **Optional:** Click "Copy" to use in other systems
5. **Click "Done"** to close the viewer

**Sample Generated CQL:**
```cql
define "Denominator":
  AgeInYearsAt(end of "Measurement Period") >= 18
    and exists ([Condition: "Diabetes"])

define "Numerator":
  exists ([Observation: "Hemoglobin A1C"]
    where timing after "Measurement Period")
    and ([Observation: "Hemoglobin A1C"].value < 9 '%')
```

**Screenshot location:** CQL viewer showing generated code

### Step 7: Test & Validate (2 minutes)

Before publishing, validate your measure:

1. **Click** "Validate" button
2. System checks:
   - ✅ All blocks are properly connected
   - ✅ No logical errors (circular references, dead ends)
   - ✅ CQL is syntactically correct
   - ✅ Population not empty
3. **Review any warnings** (orange icons)
4. **Fix issues** if needed (system will guide you)
5. **Click "Ready"** when validation passes

**Screenshot location:** Validation results showing green checkmarks

### Step 8: Publish Your Measure (2 minutes)

Now your measure is ready to go:

1. **Click** "Publish" button (top right)
2. **Confirm details:**
   - Name: "Diabetes Care Quality v1.0"
   - Complexity rating: Auto-calculated
   - Categories: Your selections
3. **Click "Publish Now"**
4. **Success!** Your measure is now available for:
   - Running evaluations
   - Sharing with your team
   - Exporting to other systems

**What happens after publishing:**
- Measure moves to "Published" status
- You can still edit (creates new version)
- Evaluations can be run against patients
- Results appear in your dashboard

---

## Common Workflows

### Workflow 1: Create a Simple Preventive Care Measure

**Goal:** Measure colorectal cancer screening rates

**Steps:**
1. Create new measure: "Colorectal Cancer Screening"
2. Population: Ages 50-75, no prior diagnosis
3. Add: Colonoscopy or fecal test procedure block
4. Threshold: Completed within 10 years
5. Generate CQL
6. Publish

**Time:** ~10 minutes

### Workflow 2: Build a Medication Management Measure

**Goal:** Measure appropriate medication for diagnosed condition

**Steps:**
1. Create measure: "Diabetes Medication Management"
2. Population: Diabetic patients
3. Add observation blocks for A1C levels
4. Add procedure blocks for medication dispensing
5. Set logic: If A1C high, was medication adjusted?
6. Generate and test CQL
7. Publish

**Time:** ~15 minutes

### Workflow 3: Create a Complex Risk Stratification Measure

**Goal:** Segment patients by risk levels

**Steps:**
1. Create measure: "Risk Stratification - Chronic Conditions"
2. Population: All patients with chronic conditions
3. Add multiple condition blocks (diabetes, heart disease, etc.)
4. Add observation blocks for biomarkers
5. Create weighted scoring logic
6. Set thresholds for risk categories (low/medium/high)
7. Use distribution slider to weight segments
8. Publish

**Time:** ~25 minutes

---

## Tips & Best Practices

### 1. Start Simple
- Begin with single-condition measures
- Add complexity gradually
- Test at each stage

### 2. Use Meaningful Names
- Block names: "Has diabetes diagnosis" not "cond1"
- Measure names: Specific and descriptive
- Examples: ✅ "Diabetic A1C Testing Rate" vs ❌ "Quality Measure 1"

### 3. Test with Real Data
- After publishing, run a test evaluation
- Check results make sense
- Adjust thresholds if needed

### 4. Version Your Work
- System auto-versions on changes
- Keep descriptive update notes
- Previous versions remain accessible

### 5. Document Your Logic
- Use block descriptions to explain "why"
- Include benchmarks and data sources
- Make it easy for others to understand

### 6. Leverage Templates
- Don't start from scratch for common measures
- Modify existing templates for your needs
- Saves time and ensures consistency

### 7. Validate Early & Often
- Use "Validate" button after each major change
- Fix warnings immediately
- Publish only when validation passes

---

## Troubleshooting

### Problem: "Validation Failed - Circular Reference"
**Solution:**
1. Review your block connections
2. Ensure blocks flow from left to right
3. Remove any feedback loops
4. Re-validate

### Problem: "Cannot Generate CQL"
**Solution:**
1. Check that all blocks are connected
2. Ensure each block has required fields filled
3. Validate population is not empty
4. Refresh page and retry

### Problem: "Measure Won't Publish"
**Solution:**
1. Run full validation (should see green checkmarks)
2. Check that complexity rating is within allowed range
3. Ensure all required fields have values
4. Contact administrator if still blocked

### Problem: "Slider Won't Move"
**Solution:**
1. Check browser zoom level (should be 100%)
2. Try different browser
3. Clear browser cache
4. Refresh page

---

## Next Steps

**After Creating Your First Measure:**

1. ✅ **Run an Evaluation** - See results with real patient data
2. ✅ **View Results Dashboard** - Understand what passed/failed
3. ✅ **Share with Colleagues** - Get feedback on your logic
4. ✅ **Export Results** - Send to leadership
5. ✅ **Iterate & Improve** - Create v2.0 with improvements
6. ✅ **Explore Advanced Features** - Learn about distributions, time periods

---

## Advanced Topics

These topics are covered in separate guides:

- **Advanced CQL Editing** - Manual code customization (see Administrator Guide)
- **Performance Optimization** - Working with 100+ blocks (see Troubleshooting Guide)
- **Multi-Tenant Measures** - Sharing across organizations
- **API Integration** - Programmatic measure creation

---

## Key Shortcuts

| Action | Shortcut |
|--------|----------|
| Save measure | `Ctrl+S` (or `Cmd+S` on Mac) |
| Validate | `Ctrl+Alt+V` |
| Undo | `Ctrl+Z` |
| Redo | `Ctrl+Y` |
| Zoom in | `Ctrl+Plus` |
| Zoom out | `Ctrl+Minus` |

---

## Support & Resources

**Need Help?**

- **Chat Support:** Click the chat icon in bottom-right corner
- **Email:** support@healthdatainmotion.com
- **Phone:** 1-800-HDI-HELP (1-800-434-4357)
- **Documentation:** See other guides in Help menu
- **Video Tutorials:** Available in Help → Videos

**Share Feedback:**

Found a bug? Have a suggestion? We'd love to hear it!

- Click Help → Send Feedback
- Or email: feedback@healthdatainmotion.com

---

## Glossary

| Term | Definition |
|------|-----------|
| **Algorithm** | The logic flow of your measure |
| **Block** | Visual element representing an operation |
| **CQL** | Clinical Quality Language (auto-generated code) |
| **Denominator** | Total population evaluated |
| **Numerator** | Subgroup that met criteria |
| **Threshold** | Benchmark or cutoff value |
| **Validation** | Checking measure for errors |
| **Version** | Different iterations of your measure |

---

## Related Documents

- **Administrator Manual:** `docs/admin/MEASURE_BUILDER_ADMINISTRATION.md`
- **Troubleshooting Guide:** `docs/user/guides/MEASURE_BUILDER_TROUBLESHOOTING.md`
- **API Documentation:** `backend/MEASURE_BUILDER_API.md`
- **Architecture Guide:** `docs/architecture/SYSTEM_ARCHITECTURE.md`

---

**Status:** ✅ Complete
**Last Updated:** January 18, 2026
**Next Review:** After user feedback (1 week)

---

*This guide is part of the Measure Builder Documentation Suite. For the complete guide, see the Help menu in Measure Builder.*
