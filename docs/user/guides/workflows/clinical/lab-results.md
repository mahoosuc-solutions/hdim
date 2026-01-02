---
id: "user-lab-results"
title: "User Guide: Lab Results & Orders"
portalType: "user"
path: "user/guides/workflows/clinical/lab-results.md"
category: "user-guide"
subcategory: "workflow"
tags: ["lab-results", "lab-orders", "result-interpretation", "trending", "follow-up"]
summary: "Review lab results, interpret findings, track trends, and follow up appropriately on abnormal values."
estimatedReadTime: 7
difficulty: "intermediate"
targetAudience: ["physician", "nurse", "clinical-staff"]
prerequisites: ["clinical-documentation", "medication-management"]
relatedGuides: ["medication-management", "patient-communication", "orders-referrals"]
lastUpdated: "2025-12-02"
---

# Lab Results & Orders

Lab results provide critical data for diagnosing and monitoring patients. This guide covers ordering, reviewing, and acting on lab results.

## Viewing Lab Results

### Recent Results Tab
1. From patient detail page, click **Lab Results** or **Labs** tab
2. Shows most recent results, typically last 1-2 years
3. Organized by test type:
   - Metabolic panel
   - Lipid panel
   - Thyroid function
   - Hemoglobin A1C
   - Other tests
4. Each result shows:
   - **Test name**: What was tested
   - **Date**: When sample collected
   - **Value**: The actual result
   - **Reference range**: Normal values
   - **Status**: Normal/Abnormal/Critical

### Interpreting Results

**Result Status**:
- 🟢 **Normal** (Green): Within normal range
- 🟡 **Low** (Yellow): Below normal range
- 🟡 **High** (Yellow): Above normal range
- 🔴 **Critical** (Red): Dangerously abnormal

**Example Reading**:
```
Hemoglobin A1C: 8.2% (Normal <5.7%, Goal for diabetics <7%)
Status: HIGH
Interpretation: Patient's diabetes control is suboptimal
Action: Intensify diabetes management
```

## Trending Lab Values

### Viewing Trends
1. Click on specific test name
2. System shows historical trend (usually last 1-3 years)
3. Graph displays values over time
4. Can see direction: Improving, worsening, or stable

**Benefit**: One result can be misleading; trends show real patterns

**Example**:
- A1C was 9.2% (2 years ago)
- A1C was 8.5% (1 year ago)
- A1C is 8.2% (today)
- **Trend**: Improving gradually (good sign!)

### Using Trends Clinically
1. **Assess response to treatment**: Did medication/lifestyle change help?
2. **Identify patterns**: Does patient control worse in winter?
3. **Set realistic goals**: If trending toward goal, continue strategy
4. **Change strategy if needed**: If worsening despite treatment, need different approach

## Interpreting Common Lab Panels

### Hemoglobin A1C (Diabetes Control)
- **Normal**: <5.7%
- **Prediabetes**: 5.7-6.4%
- **Diabetes goal**: <7% (some patients 6.5-8%)
- **Action if high**: Intensify diabetes management
- **Action if low**: Risk of hypoglycemia; consider reducing medications

### Lipid Panel (Cholesterol)
- **Total Cholesterol**: <200 mg/dL
- **LDL** ("bad"): Goal depends on risk (usually <100, <70 for high-risk)
- **HDL** ("good"): >40 men, >50 women
- **Triglycerides**: <150 mg/dL
- **Action**: If lipids not at goal, intensify treatment

### Comprehensive Metabolic Panel (CMP)
- **Electrolytes** (sodium, potassium, chloride): Check for imbalances
- **Glucose**: Fasting <100, non-fasting <140
- **Kidney function** (creatinine, BUN): Check kidney health
- **Liver function** (AST, ALT): Check liver health
- **Calcium, Albumin**: Nutritional and bone health

### Thyroid Function (TSH)
- **Normal**: 0.4-4.0 mIU/L (varies by lab)
- **Low**: May indicate overactive (hyperthyroidism)
- **High**: May indicate underactive (hypothyroidism)
- **Action**: If abnormal, adjust levothyroxine dose

## Ordering Lab Tests

### When to Order Labs
- **Chronic disease management**: A1C annually for diabetes, lipids annually for CVD risk
- **New diagnosis workup**: Initial labs to establish baseline
- **Medication monitoring**: Check kidney function on ACE inhibitors, liver on statins
- **Symptom evaluation**: Labs help diagnose chest pain, weakness, etc.
- **Follow-up**: Recheck after treatment change to see if working

### Types of Tests

**Routine Preventive Labs**:
- Annual lipids (all adults)
- Annual A1C (diabetics)
- Annual metabolic panel for chronic disease patients
- Thyroid annually (high-risk)

**Testing by Condition**:
- **Diabetes**: A1C, comprehensive metabolic panel, lipids, urinalysis (protein), kidney function
- **Hypertension**: Metabolic panel (K, Cr), lipids, urinalysis
- **Heart disease**: Lipids, troponin (chest pain), BNP (heart failure), EKG
- **Thyroid disease**: TSH, Free T4, Free T3, Thyroid antibodies

### How to Order
See **Creating Orders** guide for detailed ordering instructions.

Key points:
- Select appropriate tests
- Provide clinical indication
- Set urgency (routine vs. urgent vs. STAT)
- Set lab location
- Send order

## Managing Lab Results

### Results Arrive
1. System alerts you when results available
2. Results appear in patient chart within 1-2 hours
3. Critical values trigger alerts (red alert)
4. Abnormal values highlighted

### Your Response Steps

**Step 1: Review Result** (immediately, or within 24 hours)
- Read the value
- Compare to reference range
- Check status (normal/abnormal)
- Review prior trend if available

**Step 2: Interpret Result** (medical judgment)
- Is this expected?
- Does it change your clinical thinking?
- Is it consistent with patient's presentation?
- Does it warrant action?

**Step 3: Decide Action** (varies by finding)
- **Normal**: No action needed (except documentation)
- **Abnormal but not urgent**: Plan adjustment (e.g., increase medication)
- **Abnormal and urgent**: Contact patient, possible urgent visit/ER
- **Critical**: Contact patient immediately, consider ER

**Step 4: Communicate with Patient**
- Share results in understandable way
- Explain significance
- Discuss plan
- Answer questions

**Step 5: Document Action**
- What you did in response
- Recommendations made
- Follow-up plan
- Example: "Reviewed A1C 8.2%. Patient aware. Discussed increasing metformin. Patient agrees. Will recheck A1C in 3 months."

## Abnormal Lab Values

### Minor Abnormalities (Unlikely to Cause Symptoms)
**Example**: Lipid panel slightly elevated

**Response**:
1. Message patient: "Your lipid panel is slightly above goal. Let's adjust your diet. We may also consider medication if diet alone doesn't help."
2. Schedule follow-up lab in 3 months
3. Adjust medications if appropriate
4. Provide lifestyle recommendations (diet, exercise)
5. Recheck in 3 months

### Moderate Abnormalities (May Require Action)
**Example**: A1C 8.5% in diabetic on one medication

**Response**:
1. Call patient to discuss
2. Review medication adherence
3. Adjust medications (add or increase dose)
4. Provide education/support
5. Recheck A1C in 6-8 weeks

### Severe Abnormalities (Urgent Action)
**Example**: Potassium 6.8 mEq/L (critically high), Hemoglobin 7.2 (severe anemia)

**Response**:
1. Contact patient IMMEDIATELY
2. Assess for symptoms (weakness, shortness of breath)
3. May need urgent visit or ER
4. Order follow-up testing if appropriate
5. Initiate treatment
6. Document urgency and actions

## Critical Value Protocol

### When System Alerts "Critical"
1. Read critical value alert
2. **Verify result is accurate**: Could there be lab error?
3. **Contact patient immediately**: By phone
4. **Assess for symptoms**: Is patient experiencing effects?
5. **Contact physician immediately**: If not already done
6. **Plan urgent intervention**: Possible ER, urgent visit, medications
7. **Document thoroughly**: All actions taken, patient response

**Critical Values Vary by Lab but Often Include**:
- Glucose <50 or >500 mg/dL
- Potassium <2.8 or >6.5 mEq/L
- Sodium <120 or >160 mEq/L
- Hemoglobin <7 g/dL
- Troponin (if heart attack suspected)
- Blood cultures positive (infection)

## Following Up on Tests

### Planned Follow-Up
After abnormal result, you should:
1. **Set specific follow-up date**: "Recheck A1C in 8 weeks"
2. **Document plan**: "Will recheck K in 1 week; will call with result"
3. **Schedule appointment**: If patient needs to be seen
4. **Set reminder**: So you don't forget
5. **Tell patient**: "We'll recheck this in [timeframe]"

### Tracking Follow-Up
Use system to:
1. Create follow-up task for yourself
2. Set due date (recheck date)
3. Assign to self or team member
4. System reminds you when due
5. Complete follow-up when due

## Trending and Goals

### Setting Goals
For chronic disease monitoring, set targets:
- **Diabetes**: A1C goal <7% (or higher if elderly)
- **Hypertension**: BP goal <130/80
- **Cholesterol**: LDL goal depends on risk (usually <100)
- **Thyroid**: TSH in normal range

### Tracking Progress
Each visit:
1. Check current value
2. Compare to goal
3. Compare to previous value
4. Assess trend direction
5. Adjust treatment if not on track

### Communicating Progress
Share results with patient:
- "Your A1C improved from 8.5 to 8.1 - good progress!"
- "Your BP is running 145/90; still above goal. Let's increase your medication."
- "Your cholesterol is at goal now; keep up what you're doing!"

## Lab Result Sharing with Patients

### Patient Portal Access
Patients can see:
- Lab results and values
- Reference ranges
- Status (normal/abnormal)
- Dates of tests

### What You Should Communicate
Beyond just sharing results:
1. **What it means**: Explain in understandable language
2. **Why it matters**: How does it affect their health?
3. **What to do**: Any actions they need to take?
4. **Questions**: Answer any patient questions
5. **Goals**: Help patient understand target values

## Best Practices

### Lab Result Management Excellence
1. ✅ Review results within 24 hours of availability
2. ✅ Act on abnormal results appropriately
3. ✅ Communicate results to patient promptly
4. ✅ Track trends over time
5. ✅ Set specific follow-up plans
6. ✅ Document your assessment and plan
7. ✅ Address critical values emergently
8. ✅ Ensure patient knows goals
9. ✅ Follow up on ordered tests
10. ✅ Use results to adjust treatment

## Troubleshooting Lab Issues

### "Lab Result Shows Critical Value - What Do I Do?"
1. Verify accuracy (could be lab error)
2. Contact patient immediately
3. Assess patient symptoms
4. Contact MD immediately
5. Plan urgent intervention
6. Document everything

### "Patient's Result Doesn't Match How They're Feeling"
**Example**: A1C is 6.2% (good) but patient says they're feeling shaky/sweaty

1. Believe patient's symptoms
2. Do not rely solely on labs
3. Assess for other conditions (anxiety, thyroid, other causes)
4. May need additional testing
5. Adjust treatment based on clinical picture, not just numbers

## See Also

- [Medication Management](./medications.md)
- [Patient Communication](./patient-communication.md)
- [Clinical Documentation](./clinical-docs.md)
- [Creating Orders & Referrals](../workflows/physician/orders-referrals.md)

## Need Help?

### Self-Service Resources
- **Normal Ranges**: Built into result display
- **Interpretation Help**: Click result for details
- **Trending**: View historical graph
- **References**: Lab reference materials

### Support Contacts
- **Questions about Results**: Attending physician
- **Clinical Interpretation**: Medical director or specialist
- **Technical Issues**: IT Help Desk
- **Lab Questions**: Contact lab directly

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0
**Feedback**: Have suggestions? Contact [documentation team]
