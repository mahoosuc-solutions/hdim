# HDIM Clinical Portal - Demo Recording Script

**Duration**: 5-7 minutes  
**Preparation Time**: 10 minutes  
**Video Quality**: 1080p, 30 FPS recommended  
**Audio**: Voiceover with background music (optional)

---

## Pre-Recording Checklist

- [ ] All Docker services running and healthy
- [ ] Clinical portal accessible at http://localhost:4200
- [ ] Browser zoomed to 125% for better visibility
- [ ] Recording software configured (OBS, QuickTime, etc.)
- [ ] Microphone tested and levels set
- [ ] Screen resolution: 1920x1080 minimum
- [ ] Close unnecessary tabs/notifications
- [ ] Have demo credentials ready: `demo_admin` / `demo123`

**Verify Services:**
```bash
docker compose -f docker-compose.demo.yml ps
# All services should show "healthy"
```

---

## Part 1: Introduction & Demo Setup (0:00-0:30)

**Script:**
> "Welcome to HDIM - HealthData-in-Motion. This is our enterprise healthcare interoperability platform designed to help healthcare organizations identify and close clinical care gaps while maintaining complete HIPAA compliance.
>
> Today, we're going to walk through a real-world example of how a clinical team would use HDIM to identify a patient's missing preventive care and take action to address it."

**Actions:**
1. Show slides/title card (optional)
2. Click on clinical portal: http://localhost:4200
3. Show clean login screen
4. Explain: "Our platform uses secure, role-based access with HttpOnly cookies for HIPAA compliance"

---

## Part 2: Authentication & Role-Based Access (0:30-1:00)

**Script:**
> "Let's log in as a clinical administrator. Our system supports both email and username-based authentication, which makes it easy for team members to access the portal."

**Actions:**
1. **Show Login Form** (5 seconds)
   - Highlight username/email field
   - Highlight password field
   - Note: "HIPAA-compliant security with HttpOnly cookies"

2. **Login with Email** (10 seconds)
   - Input: `demo_admin@hdim.ai`
   - Input: `demo123`
   - Click Login
   - Show success message

3. **Show Role Permissions** (5 seconds)
   - Screenshot of "Admin" role badge
   - Explain: "This user has ADMIN and EVALUATOR permissions"
   - Note: "Our system enforces role-based access at every API call"

---

## Part 3: Patient Dashboard & Search (1:00-2:00)

**Script:**
> "Once logged in, the clinical team sees our patient dashboard. This shows a summary of all patients in the organization, along with key clinical indicators and care gaps.
>
> Let me search for our hero patient, Maria Garcia, who has some important preventive care gaps we need to address."

**Actions:**
1. **Show Dashboard Overview** (5 seconds)
   - Pan across patient list
   - Show: patient name, age, gender, clinical summary
   - Show: care gap indicators (red flags)

2. **Search for Patient** (10 seconds)
   - Click search box
   - Type: "Maria Garcia" or "MRN-2024-4521"
   - Show search results
   - Click on Maria Garcia to open her record

3. **Patient Summary** (15 seconds)
   - Show patient demographics
   - Show patient photo/avatar
   - Show key conditions:
     - Diabetes Type 2
     - Hypertension
     - Colorectal screening gap (127 days overdue)
   - Show: "Last visit: 2024-11-15"

---

## Part 4: Care Gaps & Clinical Details (2:00-3:30)

**Script:**
> "Now let's dive into Maria's care gaps. HDIM automatically identifies when preventive care is overdue based on clinical guidelines like HEDIS quality measures.
>
> Maria has two important care gaps: colorectal cancer screening and breast cancer screening. Both are now overdue, which increases her health risk.
>
> Let's see the details and recommendations for each gap."

**Actions:**
1. **Care Gaps Tab** (15 seconds)
   - Click on "Care Gaps" or "Clinical Issues" section
   - Show list of active care gaps:
     - Colorectal Cancer Screening (G0121, 127 days overdue)
     - Breast Cancer Screening (77081, 45 days overdue)
   - Expand each to show details:
     - Recommended procedure
     - CPT codes
     - Clinical guideline
     - Last screening date

2. **Medications & Conditions** (15 seconds)
   - Scroll to show current medications
   - Show condition list with dates
   - Show recent observations/vital signs
   - Note: "All data is FHIR R4 compliant"

3. **Health Metrics & Trends** (15 seconds)
   - Show any charts/graphs:
     - Blood pressure trend
     - Weight trend
     - Other relevant vitals
   - Explain: "Historical data helps us understand patterns"

---

## Part 5: Quality Measures & Evaluations (3:30-5:00)

**Script:**
> "HDIM includes automated quality measure evaluation. Our system runs HEDIS quality measures against patient data to assess compliance with clinical guidelines.
>
> For Maria, we can see her status on important preventive care measures. The system calculates her numerator and denominator status, helping the care team understand exactly where she stands."

**Actions:**
1. **Run Quality Evaluation** (20 seconds)
   - Click "Evaluations" or "Quality Measures" tab
   - Click "Run Evaluation" or similar
   - Show process: system evaluating measures
   - Show results:
     - Measure: Colorectal Cancer Screening (HEDIS)
     - Status: NOT MET (showing days overdue)
     - Measure: Breast Cancer Screening
     - Status: NOT MET

2. **Measure Details** (20 seconds)
   - Click on a measure to expand details
   - Show:
     - Measure name and code
     - Clinical criteria
     - Patient's numerator status
     - Patient's denominator status
     - Next action items
     - Link to clinical guidelines

3. **Reports & Analytics** (10 seconds)
   - Show Reports tab
   - Show sample report:
     - Organization's overall measure performance
     - Trending data
     - Drill-down to specific patients
   - Explain: "This helps leadership understand quality performance"

---

## Part 6: Close & Call-to-Action (5:00-5:30)

**Script:**
> "This is HDIM in action. In just a few minutes, we've identified Maria's care gaps, reviewed her clinical details, evaluated her against quality measures, and generated insights that drive care coordination.
>
> With HDIM, healthcare organizations can:
> - Identify care gaps automatically
> - Ensure HEDIS quality measure compliance
> - Make data-driven clinical decisions
> - Improve patient outcomes
> - Maintain complete HIPAA compliance
>
> The result is better care, better outcomes, and better business performance for healthcare organizations.
>
> To learn more about HDIM and schedule a demo, visit our website or contact our sales team."

**Actions:**
1. Show logo/branding (5 seconds)
2. Show contact information (10 seconds)
3. Optional: Show quick stats:
   - "1,000+ demo patients loaded"
   - "100+ HEDIS measures supported"
   - "Multi-tenant architecture"
   - "HIPAA-compliant"
4. Fade to black

---

## Post-Recording Steps

### Editing
- [ ] Add title card (0:00-0:03)
- [ ] Add music (optional, background)
- [ ] Add captions for accessibility
- [ ] Add on-screen callouts for key features
- [ ] Add closing slide with contact info
- [ ] Color correction and brightness adjustment
- [ ] Audio normalization

### Quality Assurance
- [ ] Review for clarity and pacing
- [ ] Check audio levels throughout
- [ ] Verify all text is readable at 1080p
- [ ] Check for any sensitive data in background
- [ ] Test playback on multiple devices

### Upload & Distribution
- [ ] Export to MP4 (H.264 codec)
- [ ] Create thumbnail image
- [ ] Write YouTube/Vimeo description
- [ ] Add timestamps in description:
  - 0:00 - Introduction
  - 0:30 - Login
  - 1:00 - Patient Dashboard
  - 2:00 - Care Gaps
  - 3:30 - Quality Measures
  - 5:00 - Summary
- [ ] Add relevant tags
- [ ] Share with team for feedback

---

## Alternative Demo Variations

### Quick Demo (3 minutes)
- Skip Parts 3-4, focus on login → care gaps → evaluation
- Target: Executive briefing

### Technical Demo (10 minutes)
- Include API calls in terminal
- Show gateway logs
- Demonstrate multi-tenant isolation
- Target: System integrators, architects

### Prospect Demo (7 minutes)
- Follow this script as-is
- Customize with prospect's patient data if available
- Target: Sales meetings

---

## Talking Points

### Problem We Solve
"Healthcare organizations struggle to identify care gaps, manage quality measures, and coordinate care - all while maintaining HIPAA compliance."

### Solution
"HDIM provides an integrated platform that automates care gap detection, quality measure evaluation, and clinical decision support in one HIPAA-compliant system."

### Key Benefits
1. **Identify Care Gaps** - Automated detection with FHIR data
2. **Quality Measure Compliance** - HEDIS and other quality framework support
3. **Clinical Decision Support** - AI-powered recommendations
4. **Multi-tenant Architecture** - Suitable for enterprise deployments
5. **HIPAA Compliance** - Full security and audit controls

### Proof Points
- "1,000 demo patients loaded with realistic clinical data"
- "100+ HEDIS quality measures pre-configured"
- "Sub-second query performance for patient data"
- "Fully FHIR R4 compliant"

---

## Common Questions During Demo

**Q: Is all this data real?**  
A: "This is demo data that mimics real-world clinical scenarios. In production, HDIM integrates with your actual EHR and data systems."

**Q: How long does it take to set up?**  
A: "HDIM can be deployed in 2-4 weeks depending on your infrastructure. We provide managed deployment options."

**Q: What about data security?**  
A: "HDIM is fully HIPAA-compliant with encryption at rest and in transit, audit logging, and role-based access control."

**Q: Can we integrate with our existing EHR?**  
A: "Yes, HDIM supports FHIR R4 and can integrate with any EHR system that supports FHIR APIs."

---

## Recording Tips

1. **Speak clearly** - Avoid filler words
2. **Slow down** - Give viewers time to read
3. **Point and click deliberately** - Don't rush through screens
4. **Pause between sections** - Gives editing opportunities
5. **Record multiple takes** - Use the best one
6. **Mute notifications** - Disable Slack, email, etc. during recording
7. **Use consistent terminology** - "Clinical portal", "Quality measures", "Care gaps"

---

## Related Files
- `DEMO_WALKTHROUGH.md` - User guide for demo stack
- `docs/gtm/HUMAN_IMPACT_NARRATIVES.md` - Messaging frameworks
- Video Production Package: `docs/video/CARE_GAP_VIDEO_PRODUCTION_PACKAGE.md`

---

**Last Updated**: December 31, 2025  
**Status**: Ready for recording  
**Target Audience**: Sales prospects, healthcare executives
