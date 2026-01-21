# HDIM Care Gap Demo - Customer Walkthrough Guide

**Version**: 1.0
**Last Updated**: December 2025
**Duration**: 15-20 minutes

---

## Executive Summary

This demo showcases HDIM's real-time care gap identification and closure workflow. You'll see how our CQL-native, FHIR R4 platform enables care managers to:

- **Identify** care gaps in real-time across patient populations
- **Prioritize** patients by risk and gap severity
- **Act** with one-click interventions and scheduling
- **Track** gap closure and quality measure improvement

**Key Differentiators Demonstrated**:
- CQL-native execution (no proprietary translation)
- FHIR R4 native architecture (no v2→FHIR conversion)
- Sub-second gap identification
- Real-time quality measure impact

---

## Pre-Demo Checklist

Before starting the demo, verify:

- [ ] Demo environment is running: `./start-demo.sh --status`
- [ ] Browser open to http://localhost:4200
- [ ] Demo data loaded (10 patients, 16 care gaps)
- [ ] Screen sharing ready (if remote demo)

---

## Demo Script

### Part 1: The Problem (2 minutes)

**Talking Points**:

> "Let me show you the challenge your care managers face every day. In a typical organization, care gap data is scattered across 15+ systems. A care manager might spend 20-30% of their time just hunting for information."

*Show the login screen*

> "Your quality team runs batch reports weekly or monthly. By the time a care manager sees a gap, the patient might have already addressed it elsewhere—or the opportunity to intervene has passed."

**Login**:
- Username: `demo_admin`
- Password: `demo123`

---

### Part 2: The Dashboard Overview (3 minutes)

*After login, the Care Gap Dashboard appears*

**Talking Points**:

> "This is what changes with HDIM. One screen. Real-time data. Everything a care manager needs to act."

**Point out key elements**:

1. **Summary Cards** (top of screen):
   - Total Open Gaps: 18
   - High Priority: 10
   - Overdue: 12
   - Closed This Month: (shows progress)

2. **Gap Distribution Chart**:
   > "Notice the breakdown by HEDIS measure. Your quality officer can see exactly where the organization stands."

3. **Patient Worklist**:
   > "This is the care manager's daily worklist. Pre-sorted by priority—highest risk patients first. No more hunting through spreadsheets."

**Click on the Priority column to sort**:
   > "Care managers can re-sort by any column. Days overdue, measure type, last contact date."

---

### Part 3: The Hero Patient - Maria Garcia (5 minutes)

**Talking Points**:

> "Let me show you a real workflow. Maria Garcia is our hero patient today. She's 57 years old with two open care gaps."

**Click on Maria Garcia's row** to open patient detail view.

**Patient Detail Screen**:

1. **Demographics Panel**:
   > "Contact information, preferred communication method, language preference—everything needed for outreach, pulled from the EHR in real-time via FHIR R4."

2. **Care Gaps Panel**:
   > "Maria has two gaps: Colorectal Cancer Screening—overdue by 127 days—and Breast Cancer Screening at 45 days."

3. **Priority Indicator**:
   > "The system flagged the colonoscopy as HIGH priority. Why? She's 57 with a family history of colon cancer. The CQL logic identified this automatically."

**Highlight the CQL transparency feature**:

> "Here's what makes HDIM different. Click this 'View Logic' button."

*Show CQL expression*

> "This is the actual NCQA HEDIS CQL logic that determined Maria has a gap. No black box. No proprietary interpretation. The same logic that runs in NCQA's reference playground runs here. This is audit-ready—your quality team can verify every determination."

---

### Part 4: Taking Action (4 minutes)

**Talking Points**:

> "Now the care manager is ready to act. One click."

**Click the "Log Intervention" button**

**Intervention Modal**:

1. **Contact Method**:
   > "Select the outreach method—phone, text, email, letter. The system tracks which methods work best for each patient."

   *Select "Phone Call"*

2. **Outcome**:
   > "Did you reach them? Did they schedule? Log the outcome right here."

   *Select "Scheduled Appointment"*

3. **Scheduling Integration**:
   > "Notice the appointment scheduler. HDIM can integrate with your scheduling system to book the colonoscopy right from this screen."

   *Enter appointment date*

4. **Notes**:
   > "Add notes for the next touchpoint. This becomes part of the care gap history."

**Click "Save Intervention"**

**Show the result**:
> "Watch the status change. The gap moves from 'Open' to 'Pending Closure' with the scheduled date. When the procedure is completed and the claim comes through, the system will automatically close the gap."

---

### Part 5: Real-Time Impact (3 minutes)

**Talking Points**:

> "Here's where it gets powerful. Let's look at the impact."

**Navigate to Quality Dashboard** (or show metrics panel)

**Point out**:

1. **Gap Closure Trend**:
   > "This chart shows gap closure rate over time. Your quality officer can see if outreach efforts are working."

2. **HEDIS Measure Performance**:
   > "Each HEDIS measure has a target. Green means on track, yellow means at risk, red needs attention."

3. **Projected Star Rating Impact**:
   > "This is the big one. Based on current gap closure rates, HDIM projects your Star Rating trajectory. You can see exactly how closing Maria's colorectal screening gap moves the needle."

**Show the calculation**:
> "Every gap has a value. Colorectal cancer screening affects CDC-COL measure. Closing this gap for Maria—and the 247 other patients with similar gaps—could improve this measure by 0.3 points. That's the difference between 3.5 and 4.0 stars for some organizations."

---

### Part 6: The Technical Differentiator (2 minutes)

**For technical audience, show**:

1. **FHIR R4 Native**:
   > "All patient data comes directly from FHIR R4 resources. No HL7 v2 translation layer losing data."

   *Show network inspector briefly if technical audience*

2. **Performance**:
   > "This gap evaluation happened in under 500 milliseconds per patient. Traditional batch processing takes hours."

3. **Architecture**:
   > "Running on 6 microservices right now: Gateway, FHIR Server, Patient Service, Care Gap Service, Quality Measure Service, and CQL Engine. Production scales to 28 services."

---

### Part 7: Summary & Next Steps (2 minutes)

**Talking Points**:

> "Let me summarize what you just saw:
>
> 1. **Real-time identification**: Care gaps appear the moment data changes in your EHR
> 2. **Intelligent prioritization**: CQL-native logic flags high-risk patients automatically
> 3. **One-click action**: Care managers spend time with patients, not hunting for data
> 4. **Measurable impact**: Direct line from intervention to Star Rating improvement
>
> The typical ROI we see is 40% faster gap closure, 12-point HEDIS improvement, and $2M+ in quality bonus recovery."

**Call to Action**:

> "Next step is connecting to your data. We can do a proof of concept with de-identified data in about 2 weeks. Would you like to schedule that?"

---

## Demo Data Reference

### Patients in Demo

| Patient | Age/Gender | Primary Gap | Priority | Days Overdue |
|---------|------------|-------------|----------|--------------|
| Maria Garcia | 57F | Colorectal Cancer Screening | HIGH | 127 |
| Sarah Johnson | 64F | Breast Cancer Screening | HIGH | 380 |
| Linda Chen | 69F | Diabetes HbA1c | HIGH | 95 |
| Michael Williams | 62M | Statin Therapy | HIGH | 60 |
| Patricia Davis | 46F | Cervical Cancer Screening | MEDIUM | 730 |
| James Thompson | 74M | Annual Wellness Visit | MEDIUM | 400 |
| Barbara Martinez | 66F | Osteoporosis Screening | MEDIUM | 180 |
| William Anderson | 49M | Depression Follow-up | HIGH | 30 |
| Susan Taylor | 59F | Diabetic Eye Exam | MEDIUM | 365 |
| David Brown | 72M | Kidney Health Evaluation | HIGH | 120 |

### HEDIS Measures Represented

| Measure ID | Measure Name | Gaps in Demo |
|------------|--------------|--------------|
| COL | Colorectal Cancer Screening | 2 |
| BCS | Breast Cancer Screening | 2 |
| CDC | Comprehensive Diabetes Care | 1 |
| CCS | Cervical Cancer Screening | 1 |
| SPC | Statin Therapy for CVD | 2 |
| AWV | Annual Wellness Visit | 1 |
| OSW | Osteoporosis Screening | 1 |
| DSF | Depression Screening Follow-up | 1 |
| EED | Diabetic Eye Exam | 2 |
| KED | Kidney Health Evaluation | 2 |
| BPD | Controlling High Blood Pressure | 1 |

---

## Handling Common Questions

### "How does this integrate with our EHR?"

> "HDIM supports any EHR with FHIR R4 APIs—Epic, Cerner, Allscripts, and others. For EHRs still on HL7 v2, we have a translation service, but we recommend FHIR where available for data fidelity."

### "Is this HIPAA compliant?"

> "Absolutely. All PHI is cached with a 5-minute TTL maximum—that's enforced at the infrastructure level. Full audit logging on every access. We support BAA agreements and are pursuing SOC 2 Type II certification."

### "How long does implementation take?"

> "A typical implementation is 8-12 weeks. The POC phase with de-identified data takes about 2 weeks. Production deployment after that depends on EHR integration complexity."

### "What about data accuracy? We've had issues with false positives."

> "Great question. That's exactly why we're CQL-native. False positives usually come from proprietary measure interpretation—someone translated NCQA logic incorrectly. With HDIM, we run the exact CQL that NCQA publishes. Same logic, same results. We see 15-20% fewer 'phantom gaps' than typical solutions."

### "What's the pricing model?"

> "We price per member per month (PMPM), which scales with your population. Contact sales for specific pricing based on your member count."

---

## Troubleshooting

### Services Not Starting

```bash
# Check logs
docker compose -f docker-compose.demo.yml logs -f

# Restart services
./start-demo.sh --stop
./start-demo.sh --build
```

### Demo Data Not Loading

```bash
# Manually seed data
./seed-demo-data.sh --wait
```

### Port Conflicts

If ports are in use, modify `docker-compose.demo.yml` port mappings or stop conflicting services.

---

## Technical Specifications

| Component | Technology | Demo Version |
|-----------|------------|--------------|
| Backend | Spring Boot 3.x (Java 21) | 1.0 |
| Frontend | Angular 17 | 1.0 |
| FHIR Server | HAPI FHIR 7.x | R4 |
| CQL Engine | cql-engine | 2.x |
| Database | PostgreSQL 15 | Latest |
| Cache | Redis 7 | Latest |
| Container | Docker 24.0+ | Required |

---

## Support

For demo issues or questions:
- Email: support@healthdatainmotion.com
- Documentation: https://docs.healthdatainmotion.com
- Sales: sales@healthdatainmotion.com

---

*This demo guide is confidential and intended for authorized sales and implementation teams only.*
