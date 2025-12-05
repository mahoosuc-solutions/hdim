---
id: "user-physician-orders-referrals"
title: "User Guide: Creating Orders & Referrals"
portalType: "user"
path: "user/guides/workflows/physician/orders-referrals.md"
category: "user-guide"
subcategory: "workflow"
tags: ["physician", "orders", "referrals", "lab-orders", "external-providers"]
summary: "Step-by-step instructions for ordering labs, imaging, and other services, plus creating and tracking referrals to external providers."
estimatedReadTime: 9
difficulty: "beginner"
targetAudience: ["physician", "provider"]
prerequisites: ["daily-workflow", "responding-care-gaps"]
relatedGuides: ["responding-care-gaps", "clinical-documentation", "lab-results"]
lastUpdated: "2025-12-02"
---

# Creating Orders & Referrals

Orders and referrals are essential tools for getting your patients the care they need. This guide covers creating, sending, and tracking all types of orders and referrals.

## Quick Reference

| Task | When to Use | Time | Tracking |
|------|-------------|------|----------|
| Lab Order | Tests needed (A1C, lipids, etc.) | 2-3 min | Automatic |
| Imaging Order | X-ray, CT, ultrasound, MRI | 3-4 min | Automatic |
| Referral to Specialist | Need another provider | 3-5 min | Email/notification |
| Care Coordination | Complex multi-provider care | 5-10 min | Care coordinator |
| Mass Order | Routine orders for population | 5 min | Batch processing |

## Creating Lab Orders

### Step-by-Step Process

**Step 1: Start Order**
1. From patient detail page, click **New Order** button (or **Orders** tab)
2. Select **Lab Order**
3. Patient information auto-fills from chart
4. Verify patient demographic information is correct

**Step 2: Select Tests**
1. System shows list of available tests
2. Search by test name if needed:
   - Type "A1C" to find hemoglobin A1C
   - Type "Lipid" to find lipid panel
   - Type "BMP" to find basic metabolic panel
3. Check boxes next to desired tests:
   - Single tests: "Hemoglobin A1C"
   - Panels: "Comprehensive Metabolic Panel" (includes multiple tests)
   - Bundles: "Diabetes Monitoring Bundle" (A1C, microalbumin, lipids together)
4. Review selected tests in right panel
5. Uncheck any tests you don't need

**Step 3: Add Clinical Information**
1. **Clinical Indication** (required):
   - State the reason for ordering tests
   - Examples:
     - "Routine diabetes management; patient due for A1C"
     - "Follow-up on elevated blood pressure; need metabolic panel"
     - "Evaluate for new-onset chest pain"
   - Use complete sentences; don't abbreviate
   - Help lab staff understand medical necessity

2. **Notes** (optional):
   - Special instructions for lab technician
   - Patient-specific considerations
   - Examples:
     - "Patient has severe needle phobia; consider pediatric needle"
     - "Patient fasting status: fed"
     - "Draw carefully; patient has difficult venous access"

3. **Fasting Requirements** (if applicable):
   - Set fasting status for tests requiring it
   - Default: Most labs interpret this automatically
   - Examples: Lipid panels often need fasting

**Step 4: Set Urgency**
1. Select priority level:
   - **Routine** (standard, results in 24-48 hours): Normal diagnostic testing
   - **Urgent** (faster processing): Results needed within 24 hours
   - **STAT** (highest priority): Dangerous condition, results needed immediately
2. Most lab orders are routine
3. Use urgent/STAT judiciously (increases costs)

**Step 5: Set Timing** (optional)
1. **When to Perform**:
   - Immediately (today/ASAP)
   - Schedule for specific date
   - Repeating order (e.g., every 3 months for chronic disease)
2. For standing orders:
   - Check **Repeating Order**
   - Set frequency: Weekly, Monthly, Every 3 months, Every 6 months, Annually
   - Set end date or "Ongoing"
   - Example: A1C test ordered annually for diabetic patient

**Step 6: Select Ordering Provider**
1. Default: Your name (current physician)
2. Change if needed:
   - Dropdown shows you and covering providers
   - Select appropriate provider
   - Usually keep as yourself (responsible for order)

**Step 7: Select Lab/Facility**
1. System shows available lab options:
   - Internal lab (if your organization has lab)
   - External labs (regional labs, reference labs)
   - Quest Diagnostics, LabCorp, etc.
2. Select preferred lab:
   - Choose based on:
     - Patient preference
     - Insurance coverage
     - Convenience (location)
     - Test availability (specialized tests)
3. System may have default based on organization preferences

**Step 8: Verify and Send**
1. Review summary:
   - Patient name and demographics
   - Tests ordered
   - Clinical indication
   - Urgency
   - Lab selection
2. Click **Send Order** or **Place Order**
3. System shows confirmation: "Order successfully placed"
4. Order appears in patient's chart immediately
5. Lab receives order electronically

### Lab Order Examples

**Example 1: Routine Diabetes Management**
- **Test**: Hemoglobin A1C
- **Clinical Indication**: "Routine diabetes management; quarterly monitoring for patient with type 2 diabetes"
- **Urgency**: Routine
- **Fasting**: Not required
- **Lab**: Internal or patient's preferred lab
- **Result**: Check in patient chart within 48 hours; generates care gap closure if in goal range

**Example 2: New Hypertension Patient**
- **Tests**: Comprehensive Metabolic Panel, Lipid Panel, Urinalysis
- **Clinical Indication**: "Initial hypertension workup; assess for secondary causes and cardiac risk"
- **Urgency**: Routine
- **Fasting**: Yes (for lipid panel)
- **Follow-up**: Schedule patient for 2-week appointment to review results

**Example 3: Abnormal Finding - Requires Urgent Assessment**
- **Test**: Electrocardiogram, Troponin, BNP
- **Clinical Indication**: "Acute chest pain evaluation; rule out acute coronary syndrome"
- **Urgency**: STAT
- **Result**: Results arrive within 1-2 hours; clinically reviewed immediately

## Creating Imaging Orders

### Imaging Order Process
Imaging orders follow similar process to lab orders with some differences:

**Step 1: Start Order**
1. Click **New Order** > **Imaging Order**
2. Patient information auto-fills

**Step 2: Select Imaging Type**
1. Search or browse imaging types:
   - **X-ray**: Chest, abdomen, extremities
   - **Ultrasound**: Abdomen, pelvic, cardiac, thyroid
   - **CT Scan**: Head, chest, abdomen (with/without contrast)
   - **MRI**: Brain, spine, extremities
   - **Mammogram**: Breast screening/diagnostic
   - **Echocardiogram**: Cardiac imaging
   - **Nuclear Medicine**: Thyroid uptake, cardiac perfusion
2. Select specific imaging study

**Step 3: Add Details**
1. **Clinical Indication** (required):
   - Reason for imaging
   - Example: "Recurrent headaches; rule out intracranial pathology"
   - Example: "Acute abdominal pain; evaluate for appendicitis"

2. **Contrast Considerations**:
   - Select if contrast needed (for CT, MRI)
   - Note any contrast allergies in patient record
   - System flags if patient has renal impairment (affects contrast safety)

3. **Special Positioning/Instructions**:
   - Any difficulty with patient positioning
   - Anxiety requiring sedation
   - Claustrophobia (MRI)
   - Implants or metal (affecting MRI safety)

**Step 4: Urgency and Timing**
- Set to Routine, Urgent, or STAT
- Most imaging is routine
- Urgent for suspected acute problems
- STAT for life-threatening emergencies

**Step 5: Select Imaging Facility**
- Choose imaging center:
  - Hospital radiology department
  - Independent imaging center
  - Outpatient imaging facility
- Consider patient convenience and insurance

**Step 6: Send Order**
1. Verify all information correct
2. Click **Send Order**
3. Facility receives order
4. Patient receives appointment notification
5. Results appear in chart after completion

## Creating Referrals to Specialists

### When to Refer
Refer patients when they need specialist evaluation or treatment for:
- **Specialty Issues**: Complex problems beyond primary care scope
- **Procedural Needs**: Procedures you don't perform
- **Second Opinions**: Diagnostic confirmation
- **Long-term Management**: Ongoing specialist care
- **Training/Education**: Specialized patient education

### Referral Process

**Step 1: Initiate Referral**
1. From patient detail page, click **New Referral** (or **Referrals** tab)
2. Patient information auto-fills
3. Click **Create Referral**

**Step 2: Select Specialty**
1. Choose specialty needed:
   - Cardiology
   - Endocrinology
   - Gastroenterology
   - Rheumatology
   - Psychiatry
   - Orthopedics
   - Neurology
   - Pulmonology
   - Etc.
2. System shows available providers/practices

**Step 3: Select Specific Provider**
1. System lists providers in that specialty:
   - Internal specialists (if your organization has them)
   - External specialists (by geography/network)
2. Consider:
   - Patient's insurance coverage
   - Patient's preference (if expressed)
   - Provider availability
   - Specialty expertise
3. Click to select provider/practice

**Step 4: Provide Clinical Details**
1. **Reason for Referral** (required):
   - Clinical reason for specialty evaluation
   - Examples:
     - "Evaluate for diabetic neuropathy; discuss treatment options"
     - "Evaluate for atrial fibrillation; discuss anticoagulation"
     - "Perioperative clearance for upcoming surgery"

2. **Relevant Clinical History**:
   - Key findings or lab results
   - Current medications
   - Previous related treatments
   - Relevant negative findings
   - Example: "Patient has had recurrent UTIs (5 in past year); normal renal ultrasound 6 months ago"

3. **Specific Requests** (optional):
   - Particular tests you'd like
   - Procedures patient may need
   - Urgency of specific recommendations
   - Example: "Please discuss options for treating mild aortic stenosis; patient concerned about surgery"

4. **Urgency**:
   - Routine (standard referral)
   - Urgent (preferably within 2 weeks)
   - STAT (needs evaluation within days)

**Step 5: Approve and Send**
1. Review referral content
2. Verify provider contact information correct
3. Confirm patient demographics
4. Click **Send Referral**
5. Referral sent to provider (usually electronically)
6. Creates tracking item in your follow-up list

### Referral Follow-Up

**What Happens After**:
1. **Specialist Receives**: Notification of referral (electronic or paper)
2. **Appointment Scheduled**: Specialist or office staff contact patient
3. **Visit Occurs**: Patient sees specialist
4. **Report Sent Back**: Results/recommendations return to you
5. **You Review**: Check specialist's recommendations and incorporate into care

**System Tracking**:
- Referral shows status: "Pending" → "Scheduled" → "Completed"
- System reminds you if no response in 14 days
- You'll be notified when specialist's report arrives
- Close referral after reviewing results

**Important**: Don't assume specialist will follow up with you. You're responsible for:
- Ensuring patient gets seen (follow up if not scheduled in 2 weeks)
- Reviewing specialist's report
- Communicating results to patient
- Incorporating recommendations into care plan

## Mass Orders and Standing Orders

### Standing Orders for Chronic Disease Management

For common, recurring tests, consider standing orders:

**Step 1: Set Up Standing Order**
1. Create lab or imaging order as usual
2. Check **Repeating Order**
3. Set frequency:
   - Every 3 months
   - Every 6 months
   - Annually
4. Set end date or mark as ongoing
5. System automatically generates order at each interval

**Benefits**:
- Ensures tests don't get missed
- Reduces manual ordering
- Improves quality measure compliance
- Saves time

**Examples**:
- Annual A1C for all diabetics
- Annual lipid panel for all hypertensives
- Quarterly A1C for patients with A1C >8%
- Annual mammogram standing order

### Bulk Orders for Population

If ordering same test for multiple patients:

1. From Population Health view, click **Bulk Actions**
2. Select **Generate Orders**
3. Choose test type
4. System applies to all eligible patients in filtered group
5. Orders generated in batch
6. Each patient gets order in their chart

## Order and Referral Status Tracking

### Patient Detail Page
1. From patient chart, look for **Active Orders** section
2. Shows all pending/active orders:
   - **Lab Orders**: Status (pending, in progress, completed)
   - **Imaging Orders**: Status (ordered, scheduled, completed)
   - **Referrals**: Status (pending specialist response, scheduled, completed)

**Status Details**:
- **Pending**: Order sent, awaiting lab/specialist to begin
- **In Progress**: Lab or specialist actively processing
- **Completed**: Results back, available for review
- **Overdue**: Expected timeframe passed; follow up needed

### Dashboard Orders Widget
1. From home dashboard, see **Pending Orders** card
2. Shows your current open orders across all patients
3. Sort by:
   - Patient name
   - Order date
   - Urgency
   - Status

### Search and Reports
1. Use Advanced Search: **Show me: All orders overdue for past 5 days**
2. Identify where follow-up needed
3. System can generate reports of pending orders

## Troubleshooting Order and Referral Issues

### "System Won't Send Order - Says Missing Information"
**Solution**:
1. Check all fields with red asterisk (required)
2. Fill in Clinical Indication - always required
3. Verify patient demographics are complete
4. Try different test/provider selection
5. Contact IT if error message seems incorrect

### "Order Sent But Patient Didn't Receive Notification"
**Causes**:
1. Patient contact information not in system
2. System notification settings disabled
3. Email went to spam folder

**Solution**:
1. Verify patient has current phone/email in system
2. Call patient to confirm they know about order
3. Resend notification from order detail page
4. Check spam folder for email

### "Results Not Appearing in Chart"
**Causes**:
1. Lab hasn't completed testing yet
2. Results not yet sent to your system
3. Electronic interface not working
4. Results sent to wrong provider

**Solution**:
1. Check with lab directly for timing
2. Wait additional day
3. Contact IT about interface issues
4. Verify results went to correct provider

### "Can't Create Order - Patient Information Incomplete"
**Causes**:
1. Patient missing required demographics
2. Insurance information not on file
3. Address not verified

**Solution**:
1. Update patient demographics first
2. Complete required fields
3. Try ordering again
4. Contact registration/billing if insurance issue

## Best Practices

### Ordering Excellence
1. **Complete Documentation**: Always explain "why" in clinical indication
2. **Appropriate Urgency**: Use STAT sparingly; increases cost
3. **Consolidate Orders**: Order multiple tests together if related
4. **Patient Convenience**: Consider scheduling/location
5. **Insurance**: Verify coverage before ordering if possible

### Referral Excellence
1. **Clear Communication**: Provide sufficient clinical context for specialist
2. **Timely Referral**: Don't delay specialist evaluation when needed
3. **Follow-Up**: Ensure patient gets seen and results get back to you
4. **Close Loop**: Review specialist report and document your response
5. **Patient Communication**: Prepare patient for specialist visit

## See Also

- [Responding to Care Gaps](./responding-care-gaps.md)
- [Lab Results & Review](../../clinical/lab-results.md)
- [Clinical Documentation](../../clinical/clinical-docs.md)
- [Medication Management](../../clinical/medications.md)

## Need Help?

### Self-Service Resources
- **Help Guides**: Press **?** and search "orders" or "referrals"
- **Video Tutorials**: Learning Center > Ordering Tests & Referrals
- **FAQs**: Common ordering questions answered

### Support Contacts
- **Clinical Questions**: Peer physician or medical director
- **Technical Issues**: IT Help Desk
- **Ordering Issues**: Care management coordinator
- **Billing/Insurance**: Revenue cycle team

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0
**Feedback**: Have suggestions? Contact [documentation team]
