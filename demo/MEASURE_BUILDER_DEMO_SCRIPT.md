# Measure Builder - Demo Script

**Version:** 1.0
**Duration:** 15 minutes
**Audience:** Sales, Training, POC Evaluations
**Last Updated:** January 18, 2026

---

## Demo Overview

This script demonstrates Measure Builder's capabilities in a quick 15-minute format perfect for sales demonstrations and training sessions.

---

## Pre-Demo Checklist

- [ ] Environment is accessible and responsive
- [ ] Sample data is loaded (100+ patients)
- [ ] Browser is Chrome/Firefox (preferred)
- [ ] Resolution is 1920x1080 or higher
- [ ] Internet connection is stable
- [ ] No notifications enabled on computer

---

## Demo Flow

### Opening (1 minute)

**Talking Points:**
- "Welcome to Measure Builder - our visual quality measure development platform"
- "No CQL coding required - just drag and drop"
- "Perfect for HEDIS measures and custom quality measures"
- "Measures can be published in minutes, not days"

**Demo:**
- Show main dashboard
- Highlight "Recent Measures" and "Templates"
- Point out the help menu

---

### Scenario 1: Create Simple Measure (5 minutes)

**Objective:** Show basic workflow in real-time

**Steps:**

**Step 1: New Measure (1 min)**
```
1. Click "New Measure" button
2. Fill in:
   - Name: "Diabetes Care Quality - Live Demo"
   - Description: "Measures HbA1C testing and management"
   - Category: "Chronic Conditions"
3. Click "Create"
```

**Talking Points:**
- "Starting from scratch - no code templates needed"
- "All the common measure categories are available"
- "System guides you through each step"

---

**Step 2: Define Population (1.5 min)**
```
1. Click "Population" section
2. Click "Add Block"
3. Select "Condition"
4. Configure:
   - Condition: "ICD10 = E11* (Type 2 Diabetes)"
   - Click checkmark
5. Add another condition:
   - Condition: "Age >= 18"
   - Connect with AND
6. Click "Validate" - should show green checkmark
```

**Talking Points:**
- "We're defining who we're measuring - diabetic patients"
- "Blocks connect logically, system validates as you go"
- "You can see the validation status in real-time"
- "Helps catch errors early"

---

**Step 3: Add Algorithm Logic (1.5 min)**
```
1. Drag "Procedure" block to canvas
2. Configure:
   - Procedure: "Hemoglobin A1C Test"
   - Timeframe: "Within 12 months"
   - Status: "Completed"
3. Drag "Observation" block
4. Configure:
   - Observation: "HbA1c < 9%"
5. Connect blocks with arrow
6. Click "Validate" - all green
```

**Talking Points:**
- "Now we're adding the logic - what are we checking for?"
- "Is the patient getting tested?"
- "Is the result at goal?"
- "Visual blocks make the logic clear"
- "No complicated CQL syntax needed"

---

**Step 4: Configure Parameters (1 min)**
```
1. Click Slider icon
2. Configure thresholds:
   - Min: 0%
   - Max: 15%
   - Target: 9%
3. Leave time period as "12 months"
4. Click "Apply"
```

**Talking Points:**
- "These sliders let you easily adjust targets"
- "Perfect for what-if analysis"
- "You can experiment with different benchmarks"

---

### Step 5: Generate CQL (1 min)

```
1. Click "View CQL"
2. Show generated code (read-only)
3. Click "Copy" to copy code
4. Close viewer
```

**Talking Points:**
- "System automatically generates production-ready CQL"
- "No manual coding - it's all done for you"
- "You can use this CQL in other systems"
- "This would typically take hours to write manually"

---

### Demo Conclusion

**Talking Points:**
- "That's it! Measure is ready to go"
- "From idea to implementation in 5 minutes"
- "Typical CQL development takes 4-8 hours"
- "Reduces time-to-market significantly"

**Q&A Preparation:**

Common questions:
- Q: "Can we customize the CQL?"
  A: "Yes, for advanced users (ADMIN role only, requires special configuration)"

- Q: "What HEDIS measures are supported?"
  A: "All HEDIS measures can be modeled using our blocks"

- Q: "How do we validate the measure?"
  A: "System validates as you build; you can also test with real patient data"

- Q: "Can multiple people work on the same measure?"
  A: "Yes, with our version control system; we recommend one person per version"

---

## Alternative Demo: Advanced Features (if time allows)

### Show Complex Measure (3-5 minutes)

```
1. Open existing complex measure (150+ blocks)
2. Demonstrate:
   - Large algorithm rendering (Canvas mode)
   - Performance (still responsive)
   - Distribution sliders for sub-populations
   - Advanced CQL with calculations
```

**Talking Points:**
- "Even complex measures remain fast"
- "System auto-optimizes rendering for large measures"
- "You can segment populations by risk, demographics, etc."

---

## Common Demo Questions & Answers

**Q: "What if we make a mistake?"**
A: "Everything is versioned. You can restore previous versions at any time."

**Q: "Can this integrate with our EHR?"**
A: "Yes, via our REST API or direct FHIR integration."

**Q: "How many users can access this?"**
A: "Scales from 1 to 10,000+ users depending on your deployment."

**Q: "What about compliance - HIPAA, etc?"**
A: "Built-in from the ground up. Multi-tenant, audit logging, encryption at rest and in transit."

**Q: "How do we run evaluations?"**
A: "Upload patient data or connect directly to your EHR. Results available in minutes."

**Q: "Can we share measures across organizations?"**
A: "Yes, with permission controls. Measures can be published to a shared repository."

---

## Follow-Up Actions

After demo, offer:

1. **Live Evaluation** - Run measure against their data
2. **Free Trial** - 30-day access to test measures
3. **Proof of Concept** - Implement one real measure
4. **Training Session** - Full team onboarding
5. **Technical Review** - Architecture & integration discussion

---

## Key Talking Points to Emphasize

1. **Speed**: Minutes instead of hours/days
2. **No-Code**: Non-technical users can create measures
3. **Quality**: Automatic validation and testing
4. **Integration**: Works with EHRs, data systems, and APIs
5. **Compliance**: Built for healthcare (HIPAA, security)
6. **Flexibility**: Supports unlimited custom measures
7. **Scalability**: Grows with your organization

---

## Objection Handling

**"We're already using X tool"**
- "Understand. How's that working for you?"
- "Measure Builder offers [key difference]"
- "Many organizations use both for different purposes"

**"This seems too easy"**
- "That's the point! We handle complexity in the background"
- "Advanced users can still edit CQL directly"
- "Try a real measure from your portfolio"

**"What about our legacy measures?"**
- "We can import most formats automatically"
- "Or we can help migrate them with training"

**"How much does this cost?"**
- "Flexible pricing based on users and measures"
- "Free trial available"
- "ROI typically shown within 3-6 months"

---

## Related Materials

- **Getting Started Guide:** `docs/user/guides/MEASURE_BUILDER_GETTING_STARTED.md`
- **Troubleshooting:** `docs/user/guides/MEASURE_BUILDER_TROUBLESHOOTING.md`
- **Case Studies:** `demo/case-studies/`
- **Sample Measures:** `demo/sample-measures.json`

---

**Status:** ✅ Complete and Ready for Use
**Last Updated:** January 18, 2026
**Next Update:** After first customer demos
