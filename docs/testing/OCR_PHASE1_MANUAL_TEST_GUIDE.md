# OCR Phase 1 Manual Testing Guide

**Document Version:** 1.0
**Date Created:** January 24, 2026
**Component:** DocumentUploadComponent
**Issue Reference:** Phase 1 - Issue #249
**Test Duration:** Approximately 90-120 minutes

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Test Environment Setup](#test-environment-setup)
3. [Test Cases](#test-cases)
   - [Functional Tests (TC1-TC6)](#functional-tests)
   - [Accessibility Tests (AT1-AT3)](#accessibility-tests)
   - [HIPAA Compliance Tests (HC1-HC2)](#hipaa-compliance-tests)
   - [Performance Tests (PT1-PT2)](#performance-tests)
4. [Test Summary](#test-summary)
5. [Issue Reporting](#issue-reporting)

---

## Prerequisites

### Required Software

- [ ] **Modern Web Browser** (Chrome 120+, Firefox 120+, or Edge 120+)
- [ ] **NVDA Screen Reader** (version 2023.3+) - [Download](https://www.nvaccess.org/download/)
- [ ] **Browser DevTools** (for console log inspection)
- [ ] **Test Files** (see Test Data section below)

### Backend Verification

Before starting tests, verify the backend is running:

1. **Open terminal** and navigate to backend directory:
   ```bash
   cd backend
   ```

2. **Check Docker containers** are running:
   ```bash
   docker compose ps
   ```

3. **Verify documentation-service** is healthy:
   ```bash
   docker compose logs documentation-service | tail -20
   ```
   Expected: `Started ClinicalDocumentationServiceApplication`

4. **Test API endpoints** (optional):
   ```bash
   # Check health endpoint
   curl -X GET http://localhost:8001/api/health

   # Expected response: {"status": "UP"}
   ```

### Frontend Verification

1. **Open terminal** and navigate to clinical-portal:
   ```bash
   cd apps/clinical-portal
   ```

2. **Verify development server** is running:
   ```bash
   npm start
   ```
   Expected: `Angular Live Development Server is listening on localhost:4200`

3. **Open browser** and navigate to:
   ```
   http://localhost:4200
   ```

4. **Verify no console errors** on page load:
   - Open DevTools (F12)
   - Check Console tab
   - Expected: No red error messages

### Test Data

Prepare the following test files in a local directory (e.g., `~/test-files/`):

| File Name | Type | Size | Purpose |
|-----------|------|------|---------|
| `sample-lab-report.pdf` | PDF | 2 MB | Valid PDF upload |
| `sample-xray.png` | PNG | 1.5 MB | Valid image upload |
| `sample-ecg.jpg` | JPG | 1.2 MB | Valid image upload |
| `sample-scan.tiff` | TIFF | 3 MB | Valid TIFF upload |
| `large-file.pdf` | PDF | 12 MB | Oversized file test |
| `invalid-file.docx` | DOCX | 500 KB | Unsupported format |

**Create test files:**
```bash
# Create test directory
mkdir -p ~/test-files

# Generate test PDFs
echo "Sample Lab Report" | ps2pdf - ~/test-files/sample-lab-report.pdf

# Generate large file (> 10 MB)
dd if=/dev/zero of=~/test-files/large-file.pdf bs=1M count=12

# Use existing images or download samples from public domain sources
```

### Login and Navigation

1. **Navigate to login page:**
   ```
   http://localhost:4200/login
   ```

2. **Login credentials** (test environment):
   - Username: `test-evaluator@example.com`
   - Password: `test-password`
   - Tenant: `test-tenant`

3. **Navigate to Clinical Documents:**
   - Click **Sidebar Menu** > **Clinical Documents**
   - Select or create a **test patient** (e.g., Patient ID: `test-patient-123`)
   - Click **View Documents** > **Upload Document** button

4. **Verify upload component** is visible:
   - Blue "Upload Document" button is displayed
   - Hint text: "Accepted: PDF, PNG, JPG, JPEG, TIFF (Max 10 MB)"

---

## Test Environment Setup

### Browser Configuration

1. **Clear browser cache:**
   - Chrome: DevTools > Application > Clear Storage
   - Firefox: DevTools > Storage > Clear All

2. **Disable browser extensions** (for clean testing)

3. **Enable accessibility features:**
   - Windows: Settings > Accessibility > Narrator (for backup testing)
   - Screen reader users: Start NVDA (Insert+Ctrl+N)

### DevTools Setup

1. **Open DevTools** (F12)

2. **Open Console tab** (Ctrl+Shift+J)

3. **Clear console** (Ctrl+L)

4. **Enable "Preserve log"** checkbox (top-left of Console)

5. **Open Network tab** (Ctrl+Shift+E)
   - Enable "Preserve log" checkbox
   - Filter: `upload` or `ocr-status`

---

## Test Cases

### Functional Tests

---

#### TC1: Upload Valid PDF File

**Objective:** Verify successful PDF upload and OCR processing initiation

**Prerequisites:**
- User is logged in
- DocumentUploadComponent is visible
- Test file `sample-lab-report.pdf` is available

**Test Steps:**

1. Click **"Upload Document"** button
   - [ ] File picker dialog opens

2. Select `sample-lab-report.pdf` (2 MB)
   - [ ] File picker closes

3. Observe upload progress indicator
   - [ ] Blue progress bar appears with indeterminate animation
   - [ ] Screen reader announces "Uploading sample-lab-report.pdf..."

4. Wait for upload completion (should be < 3 seconds)
   - [ ] Progress bar disappears
   - [ ] File appears in "Uploaded Documents" list
   - [ ] File icon shows PDF symbol (picture_as_pdf)
   - [ ] Upload date is displayed

5. Observe OCR status chip
   - [ ] Chip appears with label "OCR Queued" (orange color)
   - [ ] Chip transitions to "Processing OCR..." (blue color) within 2 seconds
   - [ ] Chip updates to "OCR Complete" (green color) within 10-30 seconds

6. Open DevTools Console
   - [ ] Verify LoggerService messages (NOT console.log):
     ```
     [INFO] [DocumentUploadComponent] Uploading file
     [INFO] [DocumentUploadComponent] File uploaded successfully
     [INFO] [DocumentUploadComponent] Starting OCR status polling
     [INFO] [DocumentUploadComponent] OCR status update
     ```

**Expected Results:**
- [ ] File uploads successfully
- [ ] OCR status updates automatically (PENDING → PROCESSING → COMPLETED)
- [ ] No console.log statements in console (only LoggerService)
- [ ] No error messages displayed
- [ ] File appears in uploaded files list

**Actual Results:**
- [ ] All expected results passed
- [ ] Some expected results failed (specify below)

**Notes:**
_Record any deviations, unexpected behavior, or observations here_

---

**Result:** ☐ PASS &nbsp;&nbsp; ☐ FAIL

---

#### TC2: Upload Valid Image File (PNG, JPG, TIFF)

**Objective:** Verify successful image file upload and OCR processing

**Prerequisites:**
- User is logged in
- DocumentUploadComponent is visible
- Test files available: `sample-xray.png`, `sample-ecg.jpg`, `sample-scan.tiff`

**Test Steps:**

**Subtest 2A: PNG Upload**

1. Click **"Upload Document"** button
2. Select `sample-xray.png` (1.5 MB)
3. Verify upload progress indicator appears
4. Wait for upload completion
   - [ ] File appears in uploaded list
   - [ ] File icon shows image symbol (image)
   - [ ] OCR status chip shows "OCR Queued"

5. Wait for OCR status updates
   - [ ] Status transitions: PENDING → PROCESSING → COMPLETED
   - [ ] Updates occur automatically every 2 seconds

**Subtest 2B: JPG Upload**

6. Click **"Upload Document"** button
7. Select `sample-ecg.jpg` (1.2 MB)
8. Verify upload and OCR processing
   - [ ] File uploads successfully
   - [ ] OCR status updates automatically

**Subtest 2C: TIFF Upload**

9. Click **"Upload Document"** button
10. Select `sample-scan.tiff` (3 MB)
11. Verify upload and OCR processing
    - [ ] File uploads successfully
    - [ ] OCR status updates automatically
    - [ ] TIFF file is accepted (matches allowedFileTypes)

**Expected Results:**
- [ ] All image formats (PNG, JPG, TIFF) upload successfully
- [ ] File icons display correctly (image symbol for all)
- [ ] OCR processing initiates for all image types
- [ ] OCR status polling works for multiple files
- [ ] No console.log statements in console

**Actual Results:**
- [ ] All expected results passed
- [ ] Some expected results failed (specify below)

**Notes:**
_Record any format-specific issues or observations_

---

**Result:** ☐ PASS &nbsp;&nbsp; ☐ FAIL

---

#### TC3: Reject Oversized File (> 10 MB)

**Objective:** Verify file size validation prevents oversized uploads

**Prerequisites:**
- User is logged in
- DocumentUploadComponent is visible
- Test file `large-file.pdf` (12 MB) is available

**Test Steps:**

1. Click **"Upload Document"** button
   - [ ] File picker dialog opens

2. Select `large-file.pdf` (12 MB)
   - [ ] File picker closes

3. Observe error message display
   - [ ] Red error message appears immediately (no upload progress)
   - [ ] Error icon (warning/error) is visible
   - [ ] Error message text reads:
     ```
     File size exceeds 10 MB limit (12.0 MB)
     ```

4. Verify screen reader announces error
   - [ ] Error message is within `role="alert"` element
   - [ ] NVDA announces error immediately

5. Verify file is NOT uploaded
   - [ ] File does NOT appear in "Uploaded Documents" list
   - [ ] No OCR status chip appears
   - [ ] No network request to `/upload` endpoint (check Network tab)

6. Verify error persistence
   - [ ] Error message remains visible until next file selection

7. Clear error by uploading valid file
   - Click **"Upload Document"** again
   - Select `sample-lab-report.pdf` (2 MB)
   - [ ] Error message disappears
   - [ ] File uploads successfully

**Expected Results:**
- [ ] Oversized file is rejected immediately (client-side validation)
- [ ] Error message displays file size in human-readable format
- [ ] No upload request is sent to backend
- [ ] Error is announced to screen readers
- [ ] User can retry with valid file

**Actual Results:**
- [ ] All expected results passed
- [ ] Some expected results failed (specify below)

**Notes:**
_Record error message wording, any upload attempts, or validation bypass attempts_

---

**Result:** ☐ PASS &nbsp;&nbsp; ☐ FAIL

---

#### TC4: Reject Unsupported File Type

**Objective:** Verify file type validation prevents unsupported formats

**Prerequisites:**
- User is logged in
- DocumentUploadComponent is visible
- Test file `invalid-file.docx` (500 KB) is available

**Test Steps:**

1. Click **"Upload Document"** button
   - [ ] File picker dialog opens

2. Observe file picker filter
   - [ ] Only allowed file types are shown by default (.pdf, .png, .jpg, .jpeg, .tiff)
   - [ ] "All files" option is available (to select DOCX)

3. Change filter to "All files" and select `invalid-file.docx`
   - [ ] File picker closes

4. Observe error message display
   - [ ] Red error message appears immediately
   - [ ] Error message text reads:
     ```
     Unsupported file type: application/vnd.openxmlformats-officedocument.wordprocessingml.document.
     Please upload PDF or image files.
     ```

5. Verify file is NOT uploaded
   - [ ] File does NOT appear in uploaded list
   - [ ] No upload progress indicator appears
   - [ ] No network request to `/upload` endpoint

6. Test additional unsupported formats (optional):
   - `.txt` file:
     - [ ] Rejected with "Unsupported file type" error
   - `.zip` file:
     - [ ] Rejected with "Unsupported file type" error

7. Clear error by uploading valid file
   - Select `sample-lab-report.pdf`
   - [ ] Error clears and file uploads successfully

**Expected Results:**
- [ ] Unsupported file types are rejected immediately
- [ ] Error message clearly states acceptable formats
- [ ] File picker pre-filters to allowed types
- [ ] No upload request is sent to backend
- [ ] User can retry with valid file

**Actual Results:**
- [ ] All expected results passed
- [ ] Some expected results failed (specify below)

**Notes:**
_Record any file types that bypass validation, or unclear error messages_

---

**Result:** ☐ PASS &nbsp;&nbsp; ☐ FAIL

---

#### TC5: Retry Failed OCR

**Objective:** Verify OCR retry functionality for failed processing

**Prerequisites:**
- User is logged in
- DocumentUploadComponent is visible
- A file with OCR status "FAILED" exists (may require backend simulation or wait for timeout)

**Test Steps:**

**Setup: Simulate OCR Failure** (choose one method):

**Method A: Backend Simulation** (recommended)
1. Configure backend to simulate OCR failure:
   - Temporarily modify OcrService to return FAILED status
   - Or upload a corrupted image file

**Method B: Natural Failure**
1. Upload an extremely large image (close to 10 MB)
2. Wait for OCR timeout (may take 2-5 minutes)

**Main Test:**

3. Verify failed OCR status display
   - [ ] File appears in uploaded list
   - [ ] OCR status chip shows "OCR Failed" (orange/yellow color)
   - [ ] Retry button (refresh icon) appears next to file

4. Verify retry button accessibility
   - [ ] Retry button has aria-label: "Retry OCR for [filename]"
   - [ ] Tooltip displays on hover: "Retry OCR"

5. Click **Retry** button (refresh icon)
   - [ ] Button click registers (no delay or unresponsiveness)

6. Observe OCR status updates
   - [ ] OCR status chip resets to "OCR Queued"
   - [ ] Status automatically transitions to "Processing OCR..."
   - [ ] Status polling restarts (check Network tab for new `/ocr-status` requests)

7. Wait for OCR completion
   - [ ] Status updates to "OCR Complete" (if backend fixed)
   - [ ] OR status returns to "OCR Failed" (if issue persists)

8. Verify logging in DevTools Console
   - [ ] LoggerService message: `[INFO] [DocumentUploadComponent] Retrying OCR processing`
   - [ ] LoggerService message: `[INFO] [DocumentUploadComponent] OCR retry initiated`
   - [ ] No console.log statements

9. Test multiple retry attempts
   - If OCR fails again, click Retry button again
   - [ ] Retry button remains functional for multiple attempts
   - [ ] No limit on retry count (user can retry indefinitely)

**Expected Results:**
- [ ] Retry button appears only for FAILED OCR status
- [ ] Retry button triggers new OCR processing
- [ ] OCR status resets and polling restarts
- [ ] Retry is logged correctly
- [ ] User can retry multiple times

**Actual Results:**
- [ ] All expected results passed
- [ ] Some expected results failed (specify below)

**Notes:**
_Record number of retry attempts, any error messages, or polling behavior_

---

**Result:** ☐ PASS &nbsp;&nbsp; ☐ FAIL

---

#### TC6: Multiple File Uploads

**Objective:** Verify component handles multiple sequential uploads correctly

**Prerequisites:**
- User is logged in
- DocumentUploadComponent is visible
- Multiple test files available (PDF, PNG, JPG)

**Test Steps:**

1. Upload **first file**: `sample-lab-report.pdf`
   - [ ] File uploads successfully
   - [ ] OCR status polling starts
   - [ ] File appears in uploaded list

2. **Immediately** upload **second file**: `sample-xray.png` (do NOT wait for OCR completion)
   - [ ] Second file uploads successfully
   - [ ] Second file appears in uploaded list
   - [ ] Both files show independent OCR status chips

3. Verify independent OCR status polling
   - [ ] First file: OCR status updates independently
   - [ ] Second file: OCR status updates independently
   - [ ] Status updates do NOT interfere with each other

4. Upload **third file**: `sample-ecg.jpg`
   - [ ] Third file uploads successfully
   - [ ] All three files display in uploaded list
   - [ ] All three files have active OCR status polling

5. Verify uploaded files list display
   - [ ] Files are listed in upload order (newest first OR oldest first - note order)
   - [ ] Each file shows:
     - [ ] Correct file icon (PDF vs image)
     - [ ] Correct filename
     - [ ] Upload date/time
     - [ ] Current OCR status

6. Wait for all OCR processes to complete
   - [ ] All three files eventually show "OCR Complete"
   - [ ] No files show "OCR Failed"
   - [ ] Status updates occur at ~2 second intervals for all files

7. Verify no resource leaks or performance degradation
   - Open DevTools > Performance tab
   - Observe memory usage
   - [ ] No excessive memory consumption
   - [ ] No console errors or warnings
   - [ ] Network tab shows regular polling (no stalled requests)

8. Test mixed upload/failure scenario (optional):
   - Upload a 4th file that will fail OCR
   - [ ] Failed file shows retry button
   - [ ] Successful files remain unaffected

**Expected Results:**
- [ ] Component handles multiple uploads without issues
- [ ] Each file has independent OCR status tracking
- [ ] Uploaded files list displays all files correctly
- [ ] No interference between concurrent OCR polling
- [ ] No memory leaks or performance degradation

**Actual Results:**
- [ ] All expected results passed
- [ ] Some expected results failed (specify below)

**Notes:**
_Record number of files tested, any UI lag, or polling issues_

---

**Result:** ☐ PASS &nbsp;&nbsp; ☐ FAIL

---

### Accessibility Tests

---

#### AT1: Keyboard Navigation

**Objective:** Verify full component functionality using keyboard only (no mouse)

**Prerequisites:**
- User is logged in
- DocumentUploadComponent is visible
- Test file `sample-lab-report.pdf` is available
- **Do NOT use mouse** for this test

**Test Steps:**

1. Navigate to upload component using keyboard
   - Press **Tab** repeatedly to focus upload button
   - [ ] Upload button receives visible focus indicator (blue outline or highlight)

2. Activate upload button with keyboard
   - Press **Enter** or **Space** while upload button is focused
   - [ ] File picker dialog opens

3. Select file using keyboard
   - Use file picker keyboard shortcuts (OS-specific):
     - Windows: Arrow keys to navigate, Enter to select
     - macOS: Arrow keys, Space to preview, Enter to select
   - Select `sample-lab-report.pdf`
   - [ ] File picker closes and upload begins

4. Observe upload progress (keyboard-accessible feedback)
   - [ ] Upload progress is announced by screen reader
   - [ ] Focus remains manageable during upload

5. Navigate to uploaded file in list
   - Press **Tab** to move focus to uploaded files list
   - [ ] Focus indicator appears on file list item

6. Navigate to retry button (if OCR fails)
   - Press **Tab** to move focus to retry button
   - [ ] Retry button receives visible focus indicator
   - Press **Enter** to activate retry
   - [ ] OCR retry initiates

7. Test error scenario with keyboard
   - Press **Tab** to focus upload button
   - Press **Enter** to open file picker
   - Select `large-file.pdf` (12 MB oversized file)
   - [ ] Error message appears
   - [ ] Error is announced by screen reader
   - Press **Tab** to navigate away from error
   - [ ] Focus moves to next interactive element (not trapped)

8. Verify no keyboard traps
   - Press **Tab** repeatedly through entire component
   - Press **Shift+Tab** to navigate backward
   - [ ] Focus moves through all interactive elements
   - [ ] Focus can exit component area
   - [ ] No focus traps or infinite loops

9. Verify keyboard shortcuts (if any)
   - Check for component-specific shortcuts
   - [ ] Document any shortcuts found
   - [ ] Shortcuts work as expected

**Expected Results:**
- [ ] All functionality accessible via keyboard
- [ ] Visible focus indicators on all interactive elements
- [ ] No keyboard traps
- [ ] Focus order is logical (top to bottom, left to right)
- [ ] Enter/Space activate buttons consistently
- [ ] Error messages do not trap keyboard focus

**Actual Results:**
- [ ] All expected results passed
- [ ] Some expected results failed (specify below)

**Notes:**
_Record any keyboard navigation issues, missing focus indicators, or unusual behavior_

---

**Result:** ☐ PASS &nbsp;&nbsp; ☐ FAIL

---

#### AT2: Screen Reader (NVDA)

**Objective:** Verify component is fully operable and understandable using NVDA screen reader

**Prerequisites:**
- NVDA screen reader installed and running (Insert+Ctrl+N to start)
- User is logged in
- DocumentUploadComponent is visible
- Test file `sample-lab-report.pdf` is available
- **Headphones recommended** (to hear NVDA announcements)

**NVDA Quick Reference:**
- **Insert+Down Arrow** - Read current line
- **Insert+F7** - List all form fields
- **Tab** - Next interactive element
- **Shift+Tab** - Previous interactive element

**Test Steps:**

1. Navigate to upload component
   - Press **Tab** to move to upload button
   - Press **Insert+Down Arrow** to read current element
   - [ ] NVDA announces: "Upload clinical document, button"

2. Read upload hint text
   - Press **Down Arrow** to read next line
   - [ ] NVDA announces: "Accepted: PDF, PNG, JPG, JPEG, TIFF (Max 10 MB)"

3. Activate upload button
   - Press **Enter** on upload button
   - [ ] NVDA announces file picker opening (OS-level announcement)

4. Select file and observe upload announcements
   - Select `sample-lab-report.pdf` in file picker
   - Wait for upload to start
   - [ ] NVDA announces: "Uploading sample-lab-report.pdf..." (from aria-live region)

5. Listen for OCR status announcements
   - Wait for OCR status chip to appear
   - [ ] NVDA announces: "OCR Queued" (from aria-live region)
   - Wait for status transition
   - [ ] NVDA announces: "Processing OCR..." (from aria-live region)
   - Wait for completion
   - [ ] NVDA announces: "OCR Complete" (from aria-live region)

6. Navigate to uploaded files list
   - Press **Tab** to move to uploaded files list
   - Press **Insert+Down Arrow** to read heading
   - [ ] NVDA announces: "Uploaded Documents, heading level 3"

7. Navigate to uploaded file item
   - Press **Down Arrow** to move to first file
   - [ ] NVDA announces: Filename (e.g., "sample-lab-report.pdf")
   - Press **Down Arrow** again
   - [ ] NVDA announces: Upload date and OCR status

8. Test retry button with screen reader
   - Upload a file that will fail OCR (or use backend simulation)
   - Navigate to retry button
   - Press **Insert+Down Arrow** to read retry button
   - [ ] NVDA announces: "Retry OCR for [filename], button, Retry OCR" (aria-label + tooltip)

9. Test error message announcement
   - Attempt to upload `large-file.pdf` (12 MB)
   - [ ] NVDA immediately announces: "Error: File size exceeds 10 MB limit (12.0 MB)" (from role="alert")
   - [ ] Error announcement is clear and actionable

10. Verify form fields list
    - Press **Insert+F7** to open NVDA Elements List
    - Navigate to "Form Fields" tab
    - [ ] Upload button is listed
    - [ ] Retry buttons (if present) are listed
    - [ ] All buttons have descriptive labels

11. Verify no unlabeled elements
    - Navigate through entire component with screen reader
    - [ ] No "unlabeled" or "button" announcements (all buttons have aria-labels)
    - [ ] No "clickable" or "graphic" without context

**Expected Results:**
- [ ] All interactive elements have descriptive labels
- [ ] Upload progress is announced (aria-live)
- [ ] OCR status updates are announced (aria-live)
- [ ] Error messages are announced immediately (role="alert")
- [ ] Uploaded files list is navigable with screen reader
- [ ] No unlabeled buttons or links
- [ ] File icons are marked aria-hidden="true" (decorative)

**Actual Results:**
- [ ] All expected results passed
- [ ] Some expected results failed (specify below)

**Notes:**
_Record any missing labels, unclear announcements, or NVDA-specific issues_

---

**Result:** ☐ PASS &nbsp;&nbsp; ☐ FAIL

---

#### AT3: Error Announcements

**Objective:** Verify error messages are immediately announced to assistive technologies

**Prerequisites:**
- NVDA screen reader running (or Windows Narrator)
- User is logged in
- DocumentUploadComponent is visible

**Test Steps:**

1. Test oversized file error announcement
   - Click upload button (or press Enter with keyboard)
   - Select `large-file.pdf` (12 MB)
   - **Immediately listen** for screen reader announcement
   - [ ] Screen reader announces error within 1 second
   - [ ] Announcement is clear: "File size exceeds 10 MB limit (12.0 MB)"

2. Test unsupported file type error announcement
   - Click upload button
   - Select `invalid-file.docx`
   - **Immediately listen** for screen reader announcement
   - [ ] Screen reader announces error within 1 second
   - [ ] Announcement is clear: "Unsupported file type... Please upload PDF or image files."

3. Verify error message persistence
   - After error is announced, press **Insert+Down Arrow** (NVDA) to re-read
   - [ ] Error message can be re-read on demand
   - [ ] Error message remains visible until cleared

4. Test error clearing announcement (optional)
   - Upload a valid file after error
   - [ ] Error message disappears (no announcement expected)
   - [ ] Success flow proceeds normally

5. Inspect error message markup
   - Open DevTools > Elements tab
   - Inspect error message container
   - [ ] Container has `role="alert"` attribute
   - [ ] Error text is inside alert container (not aria-hidden)

6. Test with Windows Narrator (optional)
   - Close NVDA
   - Start Windows Narrator (Ctrl+Win+Enter)
   - Repeat steps 1-2
   - [ ] Narrator announces errors immediately
   - [ ] Announcements are clear and complete

7. Test with browser zoom (visual accessibility)
   - Set browser zoom to 200% (Ctrl+Plus)
   - Trigger error by uploading oversized file
   - [ ] Error message is fully visible (no text cutoff)
   - [ ] Error icon is visible
   - [ ] Error message wraps correctly

**Expected Results:**
- [ ] Error messages use `role="alert"` for immediate announcement
- [ ] Errors are announced within 1 second of occurrence
- [ ] Error announcements are complete and actionable
- [ ] Error messages persist until user takes action
- [ ] Errors are announced by multiple screen readers (NVDA, Narrator)
- [ ] Error messages are visible at high zoom levels

**Actual Results:**
- [ ] All expected results passed
- [ ] Some expected results failed (specify below)

**Notes:**
_Record announcement timing, clarity issues, or screen reader compatibility problems_

---

**Result:** ☐ PASS &nbsp;&nbsp; ☐ FAIL

---

### HIPAA Compliance Tests

---

#### HC1: No PHI in Console Logs

**Objective:** Verify no Protected Health Information (PHI) is exposed in browser console logs

**Prerequisites:**
- User is logged in
- DocumentUploadComponent is visible
- DevTools Console is open and cleared
- Test file available: `sample-lab-report.pdf`

**Test Steps:**

1. Enable DevTools console filtering
   - Open DevTools (F12) > Console tab
   - Clear console (Ctrl+L)
   - Enable "Preserve log" checkbox
   - Set filter to "All levels" (Verbose, Info, Warnings, Errors)

2. Upload a document with potential PHI
   - Select `sample-lab-report.pdf`
   - File uploads successfully
   - Wait for OCR completion

3. Inspect console output for PHI violations
   - Review ALL console messages
   - Check for **patient identifiers**:
     - [ ] No patient names in logs
     - [ ] No patient IDs in logs (e.g., "patient-123")
     - [ ] No medical record numbers (MRN)
     - [ ] No social security numbers (SSN)

   - Check for **document metadata**:
     - [ ] No OCR extracted text in logs
     - [ ] No file content or previews
     - [ ] No document attachmentIds containing PHI

   - Check for **LoggerService vs console.log**:
     - [ ] All logs are from LoggerService (format: `[INFO] [ComponentName] message`)
     - [ ] NO raw `console.log`, `console.error`, or `console.warn` statements
     - [ ] NO stack traces containing PHI

4. Verify LoggerService PHI filtering
   - Look for logs like:
     ```
     [INFO] [DocumentUploadComponent] Uploading file
     [INFO] [DocumentUploadComponent] File uploaded successfully
     [INFO] [DocumentUploadComponent] Starting OCR status polling
     ```
   - [ ] Logs contain generic identifiers only (e.g., documentId, attachmentId)
   - [ ] Logs do NOT contain patient names, file contents, or OCR text

5. Test error scenario PHI filtering
   - Upload `large-file.pdf` to trigger error
   - Inspect error message in console:
     ```
     [ERROR] [DocumentUploadComponent] File upload failed
     ```
   - [ ] Error logs do NOT contain patient information
   - [ ] Error logs do NOT contain sensitive file metadata

6. Verify production build (critical for HIPAA)
   - Build Angular app for production:
     ```bash
     npm run build:prod
     ```
   - Serve production build:
     ```bash
     npx http-server dist/clinical-portal
     ```
   - Navigate to production app (e.g., `http://localhost:8080`)
   - Repeat upload test
   - [ ] Production build has NO console.log statements
   - [ ] LoggerService is disabled in production (or logs are sanitized)

7. Search for console statement violations (code inspection)
   - Open project in code editor
   - Search for `console.log` in DocumentUploadComponent files:
     ```bash
     grep -r "console\." apps/clinical-portal/src/app/components/document-upload/
     ```
   - [ ] Search returns NO results (or only TypeScript interfaces/types)

**Expected Results:**
- [ ] No PHI appears in console logs (patient names, IDs, MRNs, SSNs)
- [ ] No OCR extracted text in console logs
- [ ] All logs use LoggerService (NOT console.log)
- [ ] LoggerService automatically filters PHI
- [ ] Production build has no console.log statements
- [ ] ESLint enforces no-console rule (build fails if console.log detected)

**Actual Results:**
- [ ] All expected results passed
- [ ] Some expected results failed (specify below)

**HIPAA Violations Found (if any):**
_Describe exact console output containing PHI, file path, and remediation needed_

**Notes:**
_Record any borderline cases (e.g., non-PHI identifiers that may be concerning)_

---

**Result:** ☐ PASS &nbsp;&nbsp; ☐ FAIL
**HIPAA Compliance:** ☐ COMPLIANT &nbsp;&nbsp; ☐ **VIOLATION - CRITICAL**

---

#### HC2: Audit Logging

**Objective:** Verify all document uploads and OCR operations are logged to backend audit trail

**Prerequisites:**
- User is logged in
- DocumentUploadComponent is visible
- Backend audit logging is enabled
- Access to backend logs or audit database

**Test Steps:**

**Setup: Access Audit Logs**

1. Open backend audit logs (choose one method):

   **Method A: Docker Logs**
   ```bash
   docker compose logs -f documentation-service | grep -i audit
   ```

   **Method B: Database Query**
   ```bash
   docker exec -it hdim-postgres psql -U healthdata -d documentation_db

   SELECT * FROM audit_log
   WHERE action_type IN ('DOCUMENT_UPLOAD', 'OCR_PROCESSING', 'OCR_RETRY')
   ORDER BY created_at DESC
   LIMIT 20;
   ```

   **Method C: Audit API Endpoint** (if available)
   ```bash
   curl -X GET http://localhost:8001/api/audit-logs?userId=test-user&limit=20
   ```

**Main Test:**

2. Upload a document
   - Upload `sample-lab-report.pdf`
   - Wait for upload completion
   - [ ] Upload completes successfully

3. Verify upload audit entry
   - Check audit logs for DOCUMENT_UPLOAD event
   - [ ] Audit log entry exists
   - [ ] Entry contains required fields:
     - [ ] `userId` (e.g., "test-evaluator@example.com")
     - [ ] `tenantId` (e.g., "test-tenant")
     - [ ] `action` ("DOCUMENT_UPLOAD" or "UPLOAD")
     - [ ] `resourceType` ("ClinicalDocument" or "DocumentAttachment")
     - [ ] `resourceId` (documentId or attachmentId)
     - [ ] `timestamp` (ISO 8601 format)
     - [ ] `ipAddress` (client IP)
     - [ ] `userAgent` (browser info)

4. Verify OCR processing audit entry
   - Wait for OCR to complete
   - Check audit logs for OCR_PROCESSING event
   - [ ] Audit log entry exists
   - [ ] Entry contains:
     - [ ] `action` ("OCR_PROCESSING" or "OCR_COMPLETED")
     - [ ] `resourceId` (attachmentId)
     - [ ] `ocrStatus` ("COMPLETED" or "FAILED")
     - [ ] `timestamp`

5. Test OCR retry audit logging
   - Trigger OCR failure (or use backend simulation)
   - Click retry button
   - Check audit logs for OCR_RETRY event
   - [ ] Audit log entry exists for retry attempt
   - [ ] Entry contains:
     - [ ] `action` ("OCR_RETRY")
     - [ ] `resourceId` (attachmentId)
     - [ ] `timestamp`

6. Verify audit log completeness
   - Upload 2-3 files in sequence
   - Check audit logs
   - [ ] All uploads are logged (no missing entries)
   - [ ] Entries are in chronological order
   - [ ] No duplicate entries (same timestamp + action)

7. Test failed upload audit logging (optional)
   - Disconnect network (or use DevTools > Network > Offline)
   - Attempt to upload file
   - Restore network
   - Check audit logs
   - [ ] Failed upload is logged
   - [ ] Entry contains error message or status

8. Verify audit log retention (HIPAA requirement)
   - Query oldest audit logs
   - [ ] Audit logs are retained for at least 6 years (HIPAA §164.316(b)(2)(i))
   - [ ] No audit logs are automatically deleted (unless documented retention policy)

**Expected Results:**
- [ ] All document uploads are logged to audit trail
- [ ] All OCR processing events are logged
- [ ] OCR retry attempts are logged
- [ ] Audit logs contain required HIPAA fields (user, timestamp, action, resource)
- [ ] Audit logs are tamper-evident (write-once, no updates)
- [ ] Audit logs are retained per HIPAA requirements (6+ years)
- [ ] Failed operations are also audited

**Actual Results:**
- [ ] All expected results passed
- [ ] Some expected results failed (specify below)

**Audit Log Sample:**
_Paste sample audit log entry here for verification_

```json
{
  "auditId": "audit-123",
  "userId": "test-evaluator@example.com",
  "tenantId": "test-tenant",
  "action": "DOCUMENT_UPLOAD",
  "resourceType": "DocumentAttachment",
  "resourceId": "attachment-456",
  "timestamp": "2026-01-24T10:30:45.123Z",
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0..."
}
```

**Notes:**
_Record any missing audit entries, incomplete data, or compliance concerns_

---

**Result:** ☐ PASS &nbsp;&nbsp; ☐ FAIL
**HIPAA Compliance:** ☐ COMPLIANT &nbsp;&nbsp; ☐ **NON-COMPLIANT - CRITICAL**

---

### Performance Tests

---

#### PT1: Upload Performance (< 5 sec for 5 MB file)

**Objective:** Verify file upload completes within performance benchmarks

**Prerequisites:**
- User is logged in
- DocumentUploadComponent is visible
- Test file: `medium-file.pdf` (exactly 5 MB)
- DevTools Network tab is open

**Test Data Preparation:**

Create a 5 MB test file:
```bash
dd if=/dev/zero of=~/test-files/medium-file.pdf bs=1M count=5
```

**Test Steps:**

1. Open DevTools > Network tab
   - Click **Clear** (trash icon) to clear network log
   - Enable **Preserve log** checkbox
   - Filter: `upload`

2. Prepare to start timer
   - Open stopwatch or phone timer app
   - Or use DevTools > Performance tab

3. Upload file and start timer
   - Click **Upload Document** button
   - Select `medium-file.pdf` (5 MB)
   - **Immediately start timer** when file picker closes

4. Monitor upload progress
   - Observe progress bar animation
   - Watch Network tab for POST request to `/api/clinical-documents/{id}/upload`

5. Stop timer when upload completes
   - **Stop timer** when:
     - [ ] Progress bar disappears
     - [ ] File appears in uploaded list
     - [ ] Network tab shows request status: 200 OK

6. Record upload time
   - **Upload Duration:** ______ seconds

7. Verify upload time in DevTools
   - Click on upload request in Network tab
   - Navigate to **Timing** tab
   - [ ] Record **Total Time:** ______ ms
   - [ ] Breakdown:
     - Queueing: ______ ms
     - Sending: ______ ms
     - Waiting: ______ ms
     - Receiving: ______ ms

8. Repeat test 3 times for consistency
   - Upload the same file 3 times
   - Record times:
     - Test 1: ______ seconds
     - Test 2: ______ seconds
     - Test 3: ______ seconds
   - Calculate average: ______ seconds

9. Test network throttling (edge case)
   - Open DevTools > Network tab
   - Set throttling to **Fast 3G**
   - Upload `medium-file.pdf` again
   - [ ] Record upload time with throttling: ______ seconds
   - [ ] Component handles slow network gracefully (progress indicator visible)

**Expected Results:**
- [ ] 5 MB file uploads in < 5 seconds (average of 3 tests)
- [ ] Upload time is consistent across tests (variance < 20%)
- [ ] Network tab shows reasonable breakdown (no excessive waiting time)
- [ ] Slow network conditions are handled gracefully (no timeout errors)

**Actual Results:**
- [ ] All expected results passed
- [ ] Some expected results failed (specify below)

**Performance Data:**

| Test | File Size | Upload Time | Status |
|------|-----------|-------------|--------|
| 1    | 5 MB      | _____ sec   | PASS/FAIL |
| 2    | 5 MB      | _____ sec   | PASS/FAIL |
| 3    | 5 MB      | _____ sec   | PASS/FAIL |
| Avg  | 5 MB      | _____ sec   | PASS/FAIL |

**Notes:**
_Record any network conditions, backend load, or upload delays_

---

**Result:** ☐ PASS &nbsp;&nbsp; ☐ FAIL

---

#### PT2: OCR Status Polling (2-second interval)

**Objective:** Verify OCR status polling occurs at correct intervals without excessive requests

**Prerequisites:**
- User is logged in
- DocumentUploadComponent is visible
- DevTools Network tab is open
- Test file: `sample-lab-report.pdf`

**Test Steps:**

1. Open DevTools > Network tab
   - Click **Clear** to clear network log
   - Enable **Preserve log** checkbox
   - Filter: `ocr-status`

2. Upload a document to trigger OCR polling
   - Upload `sample-lab-report.pdf`
   - Wait for upload completion
   - [ ] File uploads successfully

3. Observe OCR status polling in Network tab
   - Watch for repeated GET requests to `/api/clinical-documents/attachments/{id}/ocr-status`
   - [ ] Polling requests appear in Network tab

4. Measure polling interval
   - Note timestamps of first 5 polling requests:
     - Request 1: ____:____.____ (timestamp)
     - Request 2: ____:____.____ (timestamp)
     - Request 3: ____:____.____ (timestamp)
     - Request 4: ____:____.____ (timestamp)
     - Request 5: ____:____.____ (timestamp)

   - Calculate intervals:
     - Interval 1-2: ______ ms
     - Interval 2-3: ______ ms
     - Interval 3-4: ______ ms
     - Interval 4-5: ______ ms
     - Average interval: ______ ms

5. Verify polling interval is ~2000 ms (2 seconds)
   - [ ] Average interval is between 1800-2200 ms (10% tolerance)
   - [ ] Intervals are consistent (variance < 500 ms)

6. Verify polling stops on completion
   - Wait for OCR to complete (status = "COMPLETED")
   - Watch Network tab for 10 seconds after completion
   - [ ] Polling stops immediately when status is "COMPLETED"
   - [ ] No additional polling requests after completion

7. Verify polling stops on failure
   - Upload a file that will fail OCR (or use backend simulation)
   - Wait for OCR status to become "FAILED"
   - Watch Network tab for 10 seconds after failure
   - [ ] Polling stops when status is "FAILED"
   - [ ] No polling requests after failure

8. Test multiple concurrent polling sessions
   - Upload 3 files simultaneously
   - [ ] Each file has independent polling (3 separate request streams)
   - [ ] Polling intervals remain ~2 seconds per file
   - [ ] No request collisions or overlaps

9. Verify no excessive requests
   - Upload a file and wait for OCR completion (e.g., 30 seconds)
   - Count total polling requests:
     - Total OCR duration: ______ seconds
     - Total polling requests: ______ requests
     - Expected requests: (duration / 2) = ______ requests
     - [ ] Actual requests ≤ expected + 2 (margin for timing)

10. Test long-running OCR (edge case)
    - Upload a large image that takes 60+ seconds to process
    - [ ] Polling continues for entire duration (no timeout)
    - [ ] Polling remains at 2-second intervals (no increase)
    - [ ] No memory leaks or performance degradation

**Expected Results:**
- [ ] OCR status polling occurs at ~2 second intervals
- [ ] Polling stops when OCR completes or fails
- [ ] No excessive polling requests (efficient resource usage)
- [ ] Multiple files poll independently
- [ ] Long-running OCR is handled correctly

**Actual Results:**
- [ ] All expected results passed
- [ ] Some expected results failed (specify below)

**Polling Data:**

| Metric | Value | Status |
|--------|-------|--------|
| Average polling interval | ______ ms | PASS/FAIL |
| Interval variance | ______ ms | PASS/FAIL |
| Polling stops on completion | Yes/No | PASS/FAIL |
| Total requests (30 sec OCR) | ______ | PASS/FAIL |

**Notes:**
_Record any polling irregularities, request failures, or timing issues_

---

**Result:** ☐ PASS &nbsp;&nbsp; ☐ FAIL

---

## Test Summary

**Test Execution Date:** _____________
**Tester Name:** _____________
**Environment:** ☐ Local Development &nbsp; ☐ Staging &nbsp; ☐ Production
**Browser:** ☐ Chrome ______ &nbsp; ☐ Firefox ______ &nbsp; ☐ Edge ______
**Operating System:** ☐ Windows ______ &nbsp; ☐ macOS ______ &nbsp; ☐ Linux ______

---

### Test Results Summary

| Category | Total | Passed | Failed | Pass Rate |
|----------|-------|--------|--------|-----------|
| **Functional Tests** (TC1-TC6) | 6 | _____ | _____ | _____% |
| **Accessibility Tests** (AT1-AT3) | 3 | _____ | _____ | _____% |
| **HIPAA Compliance Tests** (HC1-HC2) | 2 | _____ | _____ | _____% |
| **Performance Tests** (PT1-PT2) | 2 | _____ | _____ | _____% |
| **TOTAL** | **12** | _____ | _____ | _____% |

---

### Critical Issues Found

**HIPAA Compliance Violations:**
- [ ] None
- [ ] HC1: PHI in console logs - **CRITICAL**
- [ ] HC2: Missing audit logs - **CRITICAL**

**Accessibility Violations:**
- [ ] None
- [ ] AT1: Keyboard navigation broken - **HIGH**
- [ ] AT2: Screen reader issues - **HIGH**
- [ ] AT3: Error announcements missing - **MEDIUM**

**Functional Issues:**
- [ ] None
- [ ] TC1-TC6: (Specify issue number and description)

**Performance Issues:**
- [ ] None
- [ ] PT1: Upload time exceeds 5 seconds
- [ ] PT2: Polling interval incorrect

---

### Issue Details

**Issue #1:**
- Test Case: _____________
- Severity: ☐ CRITICAL &nbsp; ☐ HIGH &nbsp; ☐ MEDIUM &nbsp; ☐ LOW
- Description: _____________________________________________
- Steps to Reproduce: _____________________________________________
- Expected: _____________________________________________
- Actual: _____________________________________________

**Issue #2:**
- Test Case: _____________
- Severity: ☐ CRITICAL &nbsp; ☐ HIGH &nbsp; ☐ MEDIUM &nbsp; ☐ LOW
- Description: _____________________________________________
- Steps to Reproduce: _____________________________________________
- Expected: _____________________________________________
- Actual: _____________________________________________

**Issue #3:**
- Test Case: _____________
- Severity: ☐ CRITICAL &nbsp; ☐ HIGH &nbsp; ☐ MEDIUM &nbsp; ☐ LOW
- Description: _____________________________________________
- Steps to Reproduce: _____________________________________________
- Expected: _____________________________________________
- Actual: _____________________________________________

_Add additional issues as needed_

---

### Overall Test Status

**Overall Result:** ☐ PASS &nbsp; ☐ PASS WITH MINOR ISSUES &nbsp; ☐ FAIL

**HIPAA Compliance Status:** ☐ COMPLIANT &nbsp; ☐ NON-COMPLIANT

**Recommendation:**
- ☐ **Approve for Production** - All tests passed, no critical issues
- ☐ **Approve with Conditions** - Minor issues found, document workarounds
- ☐ **Reject - Retest Required** - Critical issues found, fixes needed

**Notes/Comments:**
_____________________________________________
_____________________________________________
_____________________________________________

---

### Tester Sign-Off

**Tester Signature:** _____________
**Date:** _____________
**QA Lead Approval:** _____________
**Date:** _____________

---

## Issue Reporting

If you discover issues during testing, please report them using the following format:

**GitHub Issue Template:**

```markdown
**Title:** [OCR Phase 1] [Component] Brief description

**Test Case:** TC# or AT# or HC# or PT#

**Severity:**
- [ ] CRITICAL - Blocks release, HIPAA violation, or accessibility blocker
- [ ] HIGH - Major functionality broken
- [ ] MEDIUM - Feature partially works
- [ ] LOW - Minor UI/UX issue

**Environment:**
- Browser: Chrome 120 / Firefox 120 / Edge 120
- OS: Windows 11 / macOS 14 / Linux
- Environment: Local / Staging / Production

**Steps to Reproduce:**
1. Navigate to...
2. Click...
3. Observe...

**Expected Result:**
_What should happen_

**Actual Result:**
_What actually happened_

**Screenshots/Logs:**
_Attach screenshots, console logs, or network traces_

**HIPAA Impact:**
- [ ] YES - PHI exposed or audit trail missing
- [ ] NO - Functional issue only

**Suggested Fix (optional):**
_Any ideas on how to resolve the issue_
```

**Report issues to:**
- GitHub: [hdim-master/issues](https://github.com/your-org/hdim-master/issues)
- Email: qa-team@example.com
- Slack: #hdim-qa-testing

---

## Appendix: Test File Preparation Script

Create all test files using this script:

```bash
#!/bin/bash
# create-test-files.sh - Generate OCR Phase 1 test files

TEST_DIR=~/test-files
mkdir -p "$TEST_DIR"

# 1. Valid PDF (2 MB)
echo "Sample Lab Report" | ps2pdf - "$TEST_DIR/sample-lab-report.pdf"
truncate -s 2M "$TEST_DIR/sample-lab-report.pdf"

# 2. Valid PNG (1.5 MB)
convert -size 800x600 xc:white "$TEST_DIR/sample-xray.png"
truncate -s 1.5M "$TEST_DIR/sample-xray.png"

# 3. Valid JPG (1.2 MB)
convert -size 800x600 xc:white "$TEST_DIR/sample-ecg.jpg"
truncate -s 1.2M "$TEST_DIR/sample-ecg.jpg"

# 4. Valid TIFF (3 MB)
convert -size 800x600 xc:white "$TEST_DIR/sample-scan.tiff"
truncate -s 3M "$TEST_DIR/sample-scan.tiff"

# 5. Oversized PDF (12 MB)
dd if=/dev/zero of="$TEST_DIR/large-file.pdf" bs=1M count=12

# 6. Unsupported DOCX (500 KB)
echo "Invalid file" > "$TEST_DIR/invalid-file.docx"
truncate -s 500K "$TEST_DIR/invalid-file.docx"

# 7. Medium PDF for performance test (5 MB)
dd if=/dev/zero of="$TEST_DIR/medium-file.pdf" bs=1M count=5

echo "Test files created in $TEST_DIR"
ls -lh "$TEST_DIR"
```

**Run script:**
```bash
chmod +x create-test-files.sh
./create-test-files.sh
```

---

**End of Manual Test Guide**

_For questions or issues with this test guide, contact the Development Team or QA Lead._
