---
id: "user-medication-management"
title: "User Guide: Medication Management"
portalType: "user"
path: "user/guides/workflows/clinical/medications.md"
category: "user-guide"
subcategory: "workflow"
tags: ["medication-management", "drug-interactions", "medication-safety", "adherence", "medication-reconciliation"]
summary: "View, manage, and monitor patient medications including safety checks, interactions, and adherence tracking."
estimatedReadTime: 9
difficulty: "intermediate"
targetAudience: ["physician", "nurse", "pharmacist", "clinical-staff"]
prerequisites: ["clinical-documentation", "clinical-alerts"]
relatedGuides: ["clinical-docs", "patient-communication", "lab-results"]
lastUpdated: "2025-12-02"
---

# Medication Management

Medications are cornerstone of treatment for chronic diseases. This guide covers viewing, managing, and optimizing patient medications.

## Viewing Patient Medications

### Current Medications List
1. From patient detail page, click **Medications** tab
2. System displays all current medications:
   - **Drug name**: Generic and brand names
   - **Dose**: Strength (e.g., 500 mg)
   - **Frequency**: How often taken (e.g., twice daily)
   - **Route**: How given (oral, injection, etc.)
   - **Start date**: When patient started
   - **Indication**: What it's for (e.g., hypertension)
   - **Status**: Active or discontinued
   - **Source**: Prescribed by [provider] or from patient report

### Medication Details
Click on any medication to see:
- **Full information**: Drug name, dose, frequency
- **Indication**: Clinical reason for medication
- **Allergies**: Any known allergies to this class
- **Interactions**: Other drugs it interacts with
- **Side effects**: Possible adverse effects
- **Patient education**: How to take, what to expect
- **Refill status**: How many refills remaining
- **Pharmacy**: Where patient fills prescription
- **Adherence**: Patient's compliance (if tracked)

## Medication Safety Checking

### Allergy Checking
System automatically checks:
1. Does patient have documented allergies?
2. Is this medication from an allergenic class?
3. Has patient had adverse reaction to this drug before?

**If Allergy Found**:
1. System alerts: 🔴 "ALLERGY ALERT"
2. Shows specific allergy and reaction
3. Recommend alternative medication
4. Do not prescribe if documented allergy

**Example**: Patient allergic to penicillin. Trying to prescribe amoxicillin (penicillin-based). System alerts, recommend alternative like azithromycin.

### Drug Interaction Checking
System checks for interactions between:
1. **All patient medications**
2. **Newly prescribed medications**
3. **Over-the-counter drugs** (if in system)
4. **Supplements** (if reported)

**Interaction Levels**:
- 🔴 **Critical**: Do NOT use together (dangerous)
- 🟠 **Major**: Use with caution, requires monitoring
- 🟡 **Moderate**: Possible interaction, monitor patient
- 🟢 **Minor**: Possible but unlikely to be clinically significant

**If Critical Interaction Found**:
1. System prevents prescribing (often)
2. If must use, requires physician override with documentation
3. Document why benefit outweighs risk
4. Plan for monitoring

**If Major Interaction Found**:
1. System alerts and suggests alternatives
2. If continue with both, document rationale
3. Monitor for symptoms
4. May need lab monitoring (e.g., INR with warfarin)

**Common Interactions**:
- Warfarin + NSAIDs (bleeding risk)
- ACE inhibitor + Potassium supplement (high K risk)
- Metformin + Contrast dye (kidney risk - hold before imaging)
- Statins + Certain antifungals (muscle risk)

### Dosing Appropriateness
System checks:
1. Is dose appropriate for patient age?
2. Is dose safe for patient's kidney function?
3. Is dose safe for patient's liver function?
4. Is dose appropriate for patient weight (some drugs)?

**Alerts for Dosing Issues**:
- 🟠 "High dose for elderly patient" (renal impairment risk)
- 🟠 "Dose reduction recommended for renal impairment"
- 🟠 "Avoid in hepatic impairment"

**Action**: If dose inappropriate, adjust or choose alternative.

## Adding Medications

### New Prescription
1. From patient detail, click **Add Medication** or **New Prescription**
2. Search for drug:
   - Type drug name (generic or brand)
   - System autocompletes
   - Verify correct drug
3. Select dose:
   - Show available strengths
   - Select appropriate dose
   - Verify kidney/liver function allows dose
4. Select frequency:
   - Once daily, twice daily, three times daily, etc.
   - As needed (PRN)
   - Special schedules (weekly, twice weekly, etc.)
5. Provide duration:
   - Open-ended (ongoing)
   - For specific period (e.g., 10 days for antibiotics)
   - Chronic condition (ongoing)
6. Number of refills:
   - For chronic drugs: automatic refill (usually)
   - For acute drugs: no refills
   - Set specific number if needed
7. Add indication:
   - "Hypertension"
   - "Type 2 diabetes management"
   - "Anxiety"
8. Add instructions/notes:
   - "Take with food"
   - "Avoid dairy within 2 hours"
   - Patient-specific instructions

### Safety Checks Before Prescribing
System automatically checks:
1. ✅ Allergies (stop if allergy exists)
2. ✅ Drug interactions (alert if major/critical)
3. ✅ Dosing appropriateness (alert if inappropriate)
4. ✅ Drug duplicates (alert if already on similar medication)
5. ✅ Contraindications (alert if patient condition contraindicates)

**Example Contraindication**: NSAID with heart failure (NSAIDs worsen heart failure). System alerts.

### Finalizing Prescription
1. Review all safety alerts
2. Address each alert (adjust dose, change drug, or override with documentation)
3. Click **Prescribe** or **Send to Pharmacy**
4. Prescription sent electronically to pharmacy
5. Patient receives notification
6. Medication appears in patient's current medication list

## Medication Changes

### Updating Current Medication

**Dose Change**:
1. From medications list, click medication to edit
2. Click **Edit Dose**
3. Enter new dose
4. Reason: "Patient's BP still elevated; increasing dose"
5. New dose takes effect
6. Patient notified
7. Prior dose documented in history

**Frequency Change**:
1. Click **Edit Frequency**
2. Change frequency (e.g., once daily → twice daily)
3. Reason documented
4. Change takes effect
5. History maintained

**Discontinuing Medication**:
1. Click medication to edit
2. Click **Discontinue**
3. Reason required:
   - "Allergy identified"
   - "Resolved condition (hypertension controlled)"
   - "Patient no longer needs"
   - "Switching to [new drug]"
4. Discontinuation date (usually today)
5. Medication marked discontinued
6. Patient notified
7. Appears in medication history

### Medication Reconciliation
Ensure medication list is accurate:

**When to Reconcile**:
- Hospital discharge (patient on new meds)
- Patient starts new provider
- Patient has multiple medications
- Patient reports medication confusion
- During comprehensive review

**How to Reconcile**:
1. Ask patient to bring all medications/bottles
2. Compare:
   - What patient bringing (what they actually have)
   - What system shows (what we prescribed)
   - Do they match?
3. Document discrepancies:
   - Patient not filling prescriptions? (cost? side effects?)
   - Patient taking extra medications? (supplements? others' meds?)
   - Doses different? (patient confused?)
4. Correct medication list:
   - Remove drugs patient not taking
   - Add drugs patient is actually taking
   - Update doses to what patient actually taking
5. Educate patient on correct regimen
6. Provide written list to patient

## Medication Adherence

### Tracking Adherence
System can track:
1. **Refill patterns**: Is patient refilling on schedule?
2. **Pharmacy fill data**: Did pharmacy fill prescription?
3. **Patient report**: Does patient say they're taking it?
4. **Clinical response**: Are patients achieving treatment goals?

### Identifying Adherence Issues

**Signs of Non-Adherence**:
- Medications not refilled on schedule
- Pharmacy shows no fills in past 3 months
- Patient admits not taking regularly
- Lab values not at goal despite medication
- Blood pressure/blood sugar not controlled

### Addressing Adherence

**Step 1: Ask Non-Judgmentally**
"Some of my patients find it hard to take medications regularly. How's that going for you?"

**Step 2: Understand Barriers**
Why isn't patient taking medication?
- Cost: Can't afford copay?
- Side effects: Is medication making them feel bad?
- Complexity: Too many medications to remember?
- Skepticism: Does patient believe medication helps?
- Practical: Difficulty swallowing? Trouble with bottles?

**Step 3: Problem-Solve**
- **Cost**: Look for generic options, patient assistance programs
- **Side effects**: Try different medication or dose
- **Complexity**: Simplify regimen; use pill organizer or blister packs
- **Skepticism**: Discuss benefits and expected results
- **Practical**: Different formulations (liquid, smaller pills)

**Step 4: Follow Up**
- "Let's try this and you report back in 2 weeks"
- "Call if you're having trouble with side effects"
- "I'll have pharmacy call you with refill reminders"
- Document adherence plan

## Drug Interactions and Monitoring

### Common Major Interactions

**Warfarin + NSAIDs** (Bleeding Risk):
- NSAIDs interfere with warfarin
- Increases bleeding risk significantly
- Alternative: Acetaminophen instead of ibuprofen
- If must use: Monitor INR closely (every 1-2 weeks)

**ACE Inhibitor + Potassium Supplement** (High Potassium):
- Both raise potassium levels
- Can cause dangerous high K
- Alternative: Remove supplement, use low-K diet
- If must use: Check K level monthly

**Metformin + Contrast Dye** (Kidney Damage):
- Before procedures requiring contrast: Hold metformin 48 hours before
- After procedure: Wait 48 hours, normal kidney function confirmation
- Restart after confirmed normal kidney function

**Statin + Strong CYP3A4 Inhibitors** (Muscle Damage):
- Some antifungals, antibiotics, protease inhibitors increase statin levels
- Risk of muscle breakdown (rhabdomyolysis)
- Alternative: Lower statin dose or different statin
- If must use: Monitor for muscle pain/weakness

### Lab Monitoring with Medications

**Warfarin**:
- Baseline INR before starting
- INR every 2-4 weeks until stable
- Then monthly or every 3 months once stable
- Target INR 2-3 for most indications

**Statins**:
- Baseline lipids and liver function
- Lipids repeat in 4-6 weeks
- Liver function if patient reports muscle pain
- Annual lipids and liver function once stable

**ACE Inhibitors/ARBs**:
- Baseline potassium and kidney function
- Recheck 1-2 weeks after starting
- Annual or if dose increased

**Metformin**:
- Baseline kidney function
- Recheck if dose increased or kidney disease risk
- Avoid if eGFR <30

**Thyroid Medication**:
- Baseline TSH
- TSH 6-8 weeks after starting or dose change
- Annual once stable

## Special Populations

### Elderly Patients
- ✅ Start low, go slow (lower starting doses)
- ✅ Fewer medications if possible (deprescribing)
- ✅ Check for interactions more carefully
- ✅ Monitor for confusion, falls (medication side effects)
- ⚠️ Avoid: Antihistamines, anticholinergics, NSAIDs long-term

### Renal Impairment
- Many medications need dose reduction if kidneys impaired
- System alerts if dose inappropriate for renal function
- Check: eGFR before prescribing
- Common: Antibiotics, ACE inhibitors, NSAIDs, metformin

### Hepatic Impairment
- Some medications metabolized by liver; risky if liver disease
- Avoid: Acetaminophen (liver toxic), certain statins
- Consider: Dose reduction for drugs metabolized by liver

### Pregnancy
- Many medications teratogenic (cause birth defects)
- Certain medications safe in pregnancy
- Consult pregnancy category or OB before prescribing
- Common safe: Prenatal vitamins, certain antihypertensives
- Avoid: ACE inhibitors, NSAIDs, warfarin

## Best Practices

### Medication Management Excellence
1. ✅ Always check allergies before prescribing
2. ✅ Review drug interactions thoroughly
3. ✅ Verify appropriate dosing for patient
4. ✅ Keep medication list up to date
5. ✅ Ask about non-adherence non-judgmentally
6. ✅ Problem-solve adherence barriers
7. ✅ Monitor for effectiveness and side effects
8. ✅ Educate patient on purpose and use
9. ✅ Order appropriate lab monitoring
10. ✅ Deprescribe when appropriate (fewer is better)

## Troubleshooting Medication Issues

### "System Won't Let Me Prescribe - Says Allergy"
**Solution**:
1. Verify allergy in system is correct
2. If allergy documented incorrectly, correct it
3. If true allergy, choose alternative medication
4. If must prescribe despite allergy, system may allow override with documentation

### "Patient Says They're Not Taking Medication"
**Approach**:
1. Ask why (understand barrier)
2. If cost: Look for generics or assistance
3. If side effects: Switch medication or dose
4. If don't believe it helps: Discuss benefits
5. Simplify regimen if too many medications
6. Follow up in 2-4 weeks

### "Drug Interaction Alert - What Do I Do?"
**Approach by Level**:
- **Critical**: Don't prescribe (change drug)
- **Major**: Consider alternative; if continue, monitor
- **Moderate**: Can usually continue; monitor for symptoms
- **Minor**: Usually can continue

## See Also

- [Clinical Documentation](./clinical-docs.md)
- [Patient Communication](./patient-communication.md)
- [Lab Results & Review](./lab-results.md)
- [Clinical Alerts](../features/core/alerts.md)

## Need Help?

### Self-Service Resources
- **Drug Information**: Click medication name for details
- **Interaction Checker**: Built-in with alerts
- **Dosing Guidance**: System flags inappropriate doses
- **References**: Pharmacology reference materials

### Support Contacts
- **Drug Questions**: Pharmacist
- **Clinical Questions**: Attending physician
- **Patient Education**: Pharmacist or nurse
- **Technical Issues**: IT Help Desk

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0
**Feedback**: Have suggestions? Contact [documentation team]
