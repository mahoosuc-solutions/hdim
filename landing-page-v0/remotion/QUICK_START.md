# Care Gap Closure Video - Quick Start

**Problem:** You need Eleanor Anderson patient data for screenshot capture.

**Solution:** Follow this guide to create her test data and capture screenshots.

---

## Step-by-Step: Create Eleanor & Capture Screenshots

### 1️⃣ Create Eleanor Anderson Test Data (5-10 minutes)

**Option A: Use Database SQL (Fastest)**

```bash
# Connect to PostgreSQL
docker exec -it hdim-postgres psql -U healthdata -d patient_db

# Run this SQL:
```

```sql
-- Create Eleanor Anderson
INSERT INTO patients (id, tenant_id, first_name, last_name, date_of_birth, gender, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'tenant-001',  -- Replace with your tenant ID
    'Eleanor',
    'Anderson',
    '1961-03-15',
    'FEMALE',
    NOW(),
    NOW()
) RETURNING id;
-- Copy the returned ID

-- Create her care gap (replace [patient-id] with ID from above)
INSERT INTO care_gaps (id, tenant_id, patient_id, measure_id, measure_name, status, urgency, days_overdue, due_date, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'tenant-001',  -- Same tenant ID
    '[patient-id]',  -- Replace with Eleanor's patient ID
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

**Option B: Modify Existing Patient (Even Faster)**

```sql
-- Find any patient with a BCS gap
SELECT p.id, p.first_name, p.last_name, cg.measure_name, cg.days_overdue
FROM care_gaps cg
JOIN patients p ON cg.patient_id = p.id
WHERE cg.measure_id = 'BCS-E' AND cg.status = 'OPEN'
LIMIT 1;

-- Update that patient to be Eleanor Anderson
UPDATE patients
SET first_name = 'Eleanor',
    last_name = 'Anderson',
    date_of_birth = '1961-03-15'
WHERE id = '[patient-id-from-above]';

-- Update care gap to 60 days overdue
UPDATE care_gaps
SET days_overdue = 60,
    urgency = 'HIGH',
    due_date = NOW() - INTERVAL '60 days'
WHERE patient_id = '[patient-id-from-above]';
```

**Verify:**
```bash
# Open Clinical Portal
# Navigate to: http://localhost:4200/care-gaps
# Verify Eleanor Anderson appears in table with HIGH urgency, 60 days overdue
```

---

### 2️⃣ Capture 4 Screenshots (15-30 minutes)

Open Clinical Portal: `http://localhost:4200/care-gaps`

**Screenshot 1: care-gap-dashboard.png**
- Full Care Gap Manager page
- Eleanor visible in table
- Summary stats showing realistic numbers
- Browser window: 1920x1080
- Save as PNG

**Screenshot 2: care-gap-table-eleanor.png**
- Same page, zoomed to table section
- Eleanor's row centered
- Can be same as Screenshot 1 (Remotion will zoom)

**Screenshot 3: care-gap-closure-dialog.png**
- Click action button on Eleanor's row
- Closure dialog opens
- Shows Quick Action buttons
- Capture dialog in opened state

**Screenshot 4: care-gap-dashboard-updated.png**
- Edit Screenshot 1 in image editor:
  - Change "45" to "44" in total gaps
  - Change "9" to "8" in high urgency
  - Remove Eleanor's row from table
- Or close Eleanor's gap and recapture

**Copy screenshots:**
```bash
cp screenshots/*.png /mnt/wdblack/dev/projects/hdim-master/landing-page-v0/remotion/public/screenshots/
```

---

### 3️⃣ Preview Video (2 minutes)

```bash
cd /mnt/wdblack/dev/projects/hdim-master/landing-page-v0/remotion
npm run dev

# Browser opens: http://localhost:3000
# Select "CareGapClosure" from dropdown
# Click play to preview
```

**Verify:**
- All 5 scenes display
- Overlays appear correctly
- Text is readable
- Animations smooth

---

### 4️⃣ Render Video (10-15 minutes)

```bash
cd /mnt/wdblack/dev/projects/hdim-master/landing-page-v0/remotion

# Render video (80 seconds)
npm run docker:render:caregap

# Wait for rendering to complete...
# Output: out/care-gap-closure.mp4
```

---

### 5️⃣ Generate Thumbnail & Deploy (5 minutes)

```bash
# Extract thumbnail (frame at 3 seconds)
ffmpeg -i out/care-gap-closure.mp4 -ss 00:00:03 -vframes 1 -q:v 2 \
  ../public/videos/care-gap-closure-thumb.png

# Copy video to landing page
cp out/care-gap-closure.mp4 ../public/videos/

# Verify files
ls -lh ../public/videos/care-gap-closure*

# Test landing page
cd ..
npm run dev
# Open: http://localhost:3000
# Scroll to "See It In Action" section
# Click thumbnail → Video plays

# Deploy to production
git add .
git commit -m "feat(landing-page): Add care gap closure demo video"
git push origin master
vercel --prod
```

---

## Common Issues & Quick Fixes

### Issue: Eleanor doesn't appear in Care Gap Manager

**Fix:**
```sql
-- Verify patient exists
SELECT * FROM patients WHERE last_name = 'Anderson';

-- Verify care gap exists
SELECT * FROM care_gaps WHERE patient_id = (SELECT id FROM patients WHERE last_name = 'Anderson');

-- Check tenant ID matches logged-in user
```

### Issue: Screenshots look different from expected

**Fix:**
- Use placeholders anyway (video will still render)
- Update screenshots later when Clinical Portal UI is ready
- Create mockups in Figma matching HDIM design system

### Issue: Docker rendering fails

**Fix:**
```bash
cd remotion
rm -rf node_modules package-lock.json
npm install
npm run docker:build
npm run docker:render:caregap
```

### Issue: Video too large (>30 MB)

**Fix:**
```bash
# Use short variant (60s instead of 80s)
npm run docker:render:caregap:short

# Or compress video
ffmpeg -i out/care-gap-closure.mp4 -b:v 1.5M care-gap-closure-compressed.mp4
```

---

## Documentation Reference

| File | Purpose |
|------|---------|
| `QUICK_START.md` | This file - Quick reference |
| `ELEANOR_ANDERSON_TEST_DATA.md` | Patient data setup (4 options) |
| `SCREENSHOT_GUIDE.md` | Screenshot capture details |
| `RENDERING_GUIDE.md` | Complete rendering guide |

---

## Total Time Estimate

| Task | Time |
|------|------|
| Create Eleanor test data | 5-10 min |
| Capture screenshots | 15-30 min |
| Preview in Remotion Studio | 2 min |
| Render video | 10-15 min |
| Generate thumbnail & deploy | 5 min |
| **Total** | **~40-60 min** |

---

**Next:** Start with Step 1 - Create Eleanor Anderson test data

---

_Last Updated: January 24, 2026_
