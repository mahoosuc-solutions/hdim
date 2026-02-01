# Eleanor Anderson - Test Data Setup Guide

This guide explains how to create Eleanor Anderson's patient record and care gap in the Clinical Portal for screenshot capture.

## Patient Story Background

**From Landing Page:**
> "Eleanor was 18 months overdue for her mammogram. No one noticed across three different health systems. When she finally scheduled it herself, Stage III. Aggressive treatment. Uncertain prognosis.
>
> At 10 months overdue, HDIM identified Eleanor's BCS-E measure gap. Care coordinator scheduled her mammogram within a week. Stage I detected. Lumpectomy. Five years cancer-free."

**For Video Demo:**
We'll show the "good timeline" - HDIM catching Eleanor's gap at **60 days overdue** (before it becomes critical).

---

## Patient Data Requirements

### Patient Record
- **Name:** Eleanor Anderson
- **Age:** 63
- **Gender:** Female
- **Date of Birth:** Calculate to make current age 63 (e.g., 1961-03-15)
- **Medical Record Number:** ELA-2024-001 (or auto-generated)

### Care Gap Record
- **Measure:** Breast Cancer Screening (BCS-E)
- **Status:** Open
- **Urgency:** HIGH (red badge)
- **Days Overdue:** 60 days
- **Last Screening Date:** ~15 months ago (calculate from today)
- **Due Date:** 60 days ago (calculate from today)

---

## Option 1: Create via Clinical Portal UI (Recommended)

### Step 1: Create Patient Record

1. Navigate to Clinical Portal: `http://localhost:4200`
2. Log in with admin credentials
3. Go to **Patient Management** → **Add New Patient**
4. Enter patient details:
   ```
   First Name: Eleanor
   Last Name: Anderson
   Date of Birth: 1961-03-15
   Gender: Female
   Tenant ID: [your-tenant-id]
   ```
5. Save patient record

### Step 2: Create Care Gap

1. Navigate to **Care Gap Manager**: `http://localhost:4200/care-gaps`
2. Click **"Add Care Gap"** (or trigger evaluation)
3. Enter care gap details:
   ```
   Patient: Eleanor Anderson
   Measure: BCS-E (Breast Cancer Screening)
   Status: Open
   Urgency: HIGH
   Days Overdue: 60
   Due Date: [60 days ago from today]
   Last Screening: [~15 months ago]
   ```
4. Save care gap record

---

## Option 2: Create via API (Backend)

If the UI doesn't have "Add Care Gap" functionality, use the backend API:

### Create Patient via API

```bash
# Set variables
TENANT_ID="tenant-001"
BASE_URL="http://localhost:8084" # Patient Service port

# Create patient
curl -X POST "${BASE_URL}/api/v1/patients" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: ${TENANT_ID}" \
  -d '{
    "firstName": "Eleanor",
    "lastName": "Anderson",
    "dateOfBirth": "1961-03-15",
    "gender": "FEMALE",
    "tenantId": "'"${TENANT_ID}"'"
  }'
```

### Create Care Gap via API

```bash
# Set variables
CARE_GAP_URL="http://localhost:8086" # Care Gap Service port
PATIENT_ID="[patient-id-from-above]"
DUE_DATE=$(date -d "60 days ago" +%Y-%m-%d)

# Create care gap
curl -X POST "${CARE_GAP_URL}/api/v1/care-gaps" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: ${TENANT_ID}" \
  -d '{
    "patientId": "'"${PATIENT_ID}"'",
    "measureId": "BCS-E",
    "measureName": "Breast Cancer Screening",
    "status": "OPEN",
    "urgency": "HIGH",
    "daysOverdue": 60,
    "dueDate": "'"${DUE_DATE}"'",
    "tenantId": "'"${TENANT_ID}"'"
  }'
```

---

## Option 3: Create via Database SQL (Direct)

If APIs aren't available, insert directly into database:

### SQL Script

```sql
-- Connect to patient database
-- docker exec -it hdim-postgres psql -U healthdata -d patient_db

-- 1. Create Patient
INSERT INTO patients (
    id,
    tenant_id,
    first_name,
    last_name,
    date_of_birth,
    gender,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'tenant-001',
    'Eleanor',
    'Anderson',
    '1961-03-15',
    'FEMALE',
    NOW(),
    NOW()
) RETURNING id;

-- Save the patient ID returned above

-- 2. Create Care Gap
INSERT INTO care_gaps (
    id,
    tenant_id,
    patient_id,
    measure_id,
    measure_name,
    status,
    urgency,
    days_overdue,
    due_date,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'tenant-001',
    '[patient-id-from-above]', -- Replace with actual patient ID
    'BCS-E',
    'Breast Cancer Screening',
    'OPEN',
    'HIGH',
    60,
    NOW() - INTERVAL '60 days',
    NOW(),
    NOW()
);
```

---

## Option 4: Use Existing Test Data (Fastest)

If you already have test patients with care gaps, you can:

### Find Existing Patient with BCS Gap

```sql
-- Search for existing BCS gaps
SELECT
    p.first_name,
    p.last_name,
    cg.measure_name,
    cg.urgency,
    cg.days_overdue,
    cg.status
FROM care_gaps cg
JOIN patients p ON cg.patient_id = p.id
WHERE cg.measure_id = 'BCS-E'
  AND cg.status = 'OPEN'
ORDER BY cg.days_overdue DESC
LIMIT 5;
```

### Modify Existing Patient

If you find a suitable patient, just update their name:

```sql
UPDATE patients
SET first_name = 'Eleanor',
    last_name = 'Anderson',
    date_of_birth = '1961-03-15'
WHERE id = '[existing-patient-id]';

-- Update care gap to 60 days overdue
UPDATE care_gaps
SET days_overdue = 60,
    urgency = 'HIGH',
    due_date = NOW() - INTERVAL '60 days'
WHERE patient_id = '[existing-patient-id]'
  AND measure_id = 'BCS-E';
```

---

## Verification

After creating Eleanor Anderson's record, verify it appears correctly:

### 1. Check Care Gap Manager UI

Navigate to: `http://localhost:4200/care-gaps`

**Expected:**
- Table shows "Eleanor Anderson" row
- Age: 63
- Measure: "Breast Cancer Screening" or "BCS-E"
- Urgency badge: **HIGH** (red)
- Days overdue: **60**
- Status: Open

### 2. Check Summary Statistics

At the top of Care Gap Manager, verify:
- **Total Gaps:** Should include Eleanor's gap
- **High Urgency:** Should show at least 1 (Eleanor's gap)

### 3. Test Quick Actions

Click on Eleanor's row → Open closure dialog:
- Should show patient name: "Eleanor Anderson, 63"
- Quick Action buttons should be visible:
  - "Schedule Screening" (purple)
  - "Already Done" (green)
  - "Patient Declined" (red)

---

## Creating Additional Test Gaps (Optional)

For a more realistic dashboard, create 44 additional care gaps (so total = 45):

```sql
-- Example: Create multiple gaps with different urgencies
INSERT INTO care_gaps (id, tenant_id, patient_id, measure_id, measure_name, status, urgency, days_overdue, due_date, created_at)
SELECT
    gen_random_uuid(),
    'tenant-001',
    (SELECT id FROM patients ORDER BY RANDOM() LIMIT 1),
    'CCS-E',
    'Colorectal Cancer Screening',
    'OPEN',
    CASE
        WHEN random() < 0.2 THEN 'HIGH'
        WHEN random() < 0.6 THEN 'MEDIUM'
        ELSE 'LOW'
    END,
    (random() * 100 + 10)::int,
    NOW() - (random() * 100 + 10)::int * INTERVAL '1 day',
    NOW()
FROM generate_series(1, 44);
```

**Summary Stats After:**
- Total gaps: **45**
- High urgency: **9** (Eleanor + 8 others)
- Medium urgency: ~18
- Low urgency: ~18

---

## Screenshot Capture Checklist

Once Eleanor Anderson exists in the system:

- [ ] Patient visible in Care Gap Manager table
- [ ] Name shows as "Eleanor Anderson"
- [ ] Age shows as 63
- [ ] Measure shows as "Breast Cancer Screening" or "BCS-E"
- [ ] Urgency badge is **HIGH** (red)
- [ ] Days overdue shows **60**
- [ ] Summary stats show realistic numbers (45 total, 9 high urgency)
- [ ] Quick Actions dialog opens with 3 buttons
- [ ] Can capture all 4 required screenshots

---

## Troubleshooting

### Problem: Patient doesn't appear in Care Gap Manager

**Solution:**
- Check patient was created: `SELECT * FROM patients WHERE last_name = 'Anderson';`
- Check care gap exists: `SELECT * FROM care_gaps WHERE patient_id = '[eleanor-patient-id]';`
- Verify tenant filtering: Ensure logged-in user has same tenant ID

### Problem: Care gap shows but urgency is wrong

**Solution:**
```sql
UPDATE care_gaps
SET urgency = 'HIGH',
    days_overdue = 60
WHERE patient_id = (SELECT id FROM patients WHERE last_name = 'Anderson');
```

### Problem: Quick Actions dialog doesn't show buttons

**Solution:**
- Verify BulkActionDialog component is implemented (see Issue #241)
- Check browser console for JavaScript errors
- Fallback: Create mockup screenshot in Figma

---

## Next Steps

After creating Eleanor Anderson's test data:

1. Follow `SCREENSHOT_GUIDE.md` to capture 4 screenshots
2. Replace placeholder screenshots in `remotion/public/screenshots/`
3. Preview in Remotion Studio: `npm run dev`
4. Render video: `npm run docker:render:caregap`

---

_Last Updated: January 24, 2026_
