# MPI Feature Testing Guide

## Issue Fixed

**Problem**: The `autoDetectAndLinkDuplicates()` method had an async bug where it returned immediately with `mastersCreated: 0, duplicatesLinked: 0` before any links were created.

**Solution**: Rewrote the algorithm to use synchronous nested loops instead of async `forEach + subscribe` pattern.

## Testing Steps

### 1. Refresh Your Browser
- Press `Ctrl + Shift + R` (hard refresh) to clear cache
- Navigate to http://localhost:4200/patients
- You should see all 63 patients listed

### 2. Click "Detect Duplicates" Button
- Look for the purple button near the top that says "Detect Duplicates"
- Click it once
- You should see a "Detecting..." spinner briefly

### 3. What You Should See After Detection

**Success Message** (appears for 10 seconds):
```
✓ Success! Linked X duplicate(s) to Y master record(s).
Look for green "Master" and orange "Duplicate" badges below.
```

**Updated Statistics Cards** (purple gradient at top):
- **Master Records**: Should show ~18-20 (was 0)
- **Linked Duplicates**: Should show ~43-45 (was 0)

**Table Changes**:
- **Green rows with left border** = Master records
  - Show green checkmark icon + "Master" badge in MPI Status column
  - Example: First "John Doe" with lowest ID becomes master

- **Orange rows with left border** = Duplicate records
  - Show orange copy icon + "Duplicate" badge in MPI Status column
  - Slightly faded appearance
  - Example: Other "John Doe" records link to the master

- **White rows** = Unlinked records (unique patients with no duplicates)

### 4. Browser Console Output

Open DevTools (F12) and check Console tab. You should see:
```
Starting duplicate detection for 63 patients...
Linked duplicate: 52 -> master: 1 (score: 100)
Linked duplicate: 92 -> master: 1 (score: 100)
Linked duplicate: 152 -> master: 1 (score: 100)
... (many more lines)
Detection complete: 18 masters, 45 duplicates linked
Detection result: {mastersCreated: 18, duplicatesLinked: 45, matches: Array(0)}
Updated stats: {totalPatients: 63, masterRecords: 18, duplicateRecords: 45, ...}
```

### 5. Test Master Records Filter

- Check the box "Show Master Records Only"
- Table should filter to show only ~18-20 records (all green-bordered masters)
- Uncheck the box to see all 63 records again

### 6. Expected Duplicate Groups

Based on the database validation, you should see these groups:

**John Doe Group** (5 records → 1 master + 4 duplicates):
- IDs: 1, 52, 92, 152 (perfect matches, 100% score)
- ID 174 (MRN-9001, different MRN but same name/DOB, 88% score)

**Jane Smith Group** (6 records → 1 master + 5 duplicates):
- IDs: 2, 53, 93, 153 (perfect matches, 100% score)
- ID 175: "Jane Marie Smith" (middle name, 90% score)
- ID 178: "Jane M Smith" (middle initial, 92% score)

**Robert Johnson Group** (5 records → 1 master + 4 duplicates):
- IDs: 3, 54, 94, 154 (perfect matches, 100% score)
- ID 176: "Robert Jonson" (typo, 88% score)

**Other duplicate groups**: David Anderson, Emily Taylor, James Wilson, etc.

## Troubleshooting

### If you see "No high-confidence duplicates detected"

This means the algorithm didn't find any matches ≥85%. Check:
1. Are there actually duplicate patients in the database? (run the validation query again)
2. Check browser console for "Detection complete:" message - it should show the actual counts

### If the UI doesn't update

1. Check that the success message appears (even if it says 0 duplicates)
2. Open DevTools Console and look for error messages
3. Verify the build compiled successfully (check terminal output)

### If you still see "Unlinked N/A" for all records

1. The button click didn't work - try clicking again
2. Check browser console for JavaScript errors
3. Try a hard refresh: `Ctrl + Shift + R`

## Visual Reference

### Before Detection
```
MPI Status        | Name              | DOB        | ...
------------------|-------------------|------------|-----
Unlinked N/A      | John Doe          | 11/13/1959 | ...
Unlinked N/A      | John Doe          | 11/13/1959 | ...
Unlinked N/A      | Jane Smith        | 11/13/1979 | ...
```

### After Detection
```
MPI Status        | Name              | DOB        | ...
------------------|-------------------|------------|-----
✓ Master [+4]     | John Doe          | 11/13/1959 | ... (GREEN ROW)
⎘ Duplicate       | John Doe          | 11/13/1959 | ... (ORANGE ROW)
⎘ Duplicate       | John Doe          | 11/13/1959 | ... (ORANGE ROW)
✓ Master [+5]     | Jane Smith        | 11/13/1979 | ... (GREEN ROW)
⎘ Duplicate       | Jane Marie Smith  | 11/13/1979 | ... (ORANGE ROW)
```

## Next Steps After Successful Test

1. **Demo to stakeholders** using [MPI_SALES_DEMO_GUIDE.md](MPI_SALES_DEMO_GUIDE.md)
2. **Document in feature docs** - Already done in [PATIENT_DEDUPLICATION_FEATURE.md](PATIENT_DEDUPLICATION_FEATURE.md)
3. **Backend integration** - Connect to real MPI API endpoints
4. **Manual linking UI** - Add dialog to manually link/unlink records
5. **Audit trail** - Track who linked what and when

## Technical Details

### Matching Algorithm
```
Score = (Name × 40%) + (DOB × 30%) + (MRN × 20%) + (Gender × 10%)

Name matching:
- Exact match (normalized): 40 points
- Levenshtein distance < 20%: 25 points
- Otherwise: 0 points

Threshold: 85% for auto-link
```

### Master Selection Logic
1. If one patient already has duplicates → keep as master
2. Compare MRN numbers → lower wins
3. Fall back to ID comparison → lower wins

This ensures stable, predictable master selection.

---

**Last Updated**: 2025-11-19 19:43 UTC
**Status**: ✅ Ready for Testing
**Build**: Successfully compiled at 19:43:38
