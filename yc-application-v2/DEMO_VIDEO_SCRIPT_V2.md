# HDIM Demo Video Script v2

**Updated: December 2025 | Platform v1.5.0**

---

## Video Specifications

| Spec | Requirement |
|------|-------------|
| Duration | 1 minute (primary), 2 minutes (extended) |
| Resolution | 1920x1080 (1080p) |
| Format | MP4, H.264 |
| Audio | Clear voiceover, no background music |
| Hosting | YouTube (unlisted) |

---

## 1-MINUTE VERSION (YC Primary)

### Scene 1: Hook (0:00 - 0:10)
**Visual:** HDIM logo, then split screen showing code + platform metrics

**Voiceover:**
> "We built a $1.7 million healthcare platform for $46,000 using AI-assisted development. 162,000 lines of code. 28 microservices. One founder. Three months."

**On-screen text:** "37x Cost Reduction | AI-Native Development"

---

### Scene 2: Problem (0:10 - 0:20)
**Visual:** Legacy system mockup with "PROCESSING..." spinner, then "Results available tomorrow"

**Voiceover:**
> "Healthcare quality teams wait 24 to 48 hours for measure results. By then, the patient is gone. Legacy systems cost $50,000 a month and take a year to implement."

**On-screen text:** "$50K+/month | 6-12 month deployment | Overnight batch"

---

### Scene 3: Solution (0:20 - 0:30)
**Visual:** HDIM dashboard loading instantly with metrics

**Voiceover:**
> "HDIM calculates all 61 HEDIS quality measures in under 200 milliseconds. Real-time, at point of care, for any EHR."

**On-screen text:** "61 HEDIS Measures | <200ms | Any EHR"

---

### Scene 4: Live Demo (0:30 - 0:50)
**Visual:** Screen recording of clinical portal

**Action Sequence:**
1. Search for patient "Maria Garcia"
2. Patient 360 view loads instantly
3. Show care gaps with priority indicators
4. Click "Schedule Mammogram" to address gap
5. Gap status updates in real-time

**Voiceover:**
> "Here's a diabetic patient with two open care gaps. The system calculated her health score, identified missing screenings, and prioritized actions—all before the chart finished loading. One click schedules the mammogram and updates her care plan."

---

### Scene 5: Close (0:50 - 1:00)
**Visual:** Architecture diagram, then pricing comparison, then logo

**Voiceover:**
> "FHIR-native. HIPAA-compliant with MFA. Starting at $80 a month versus $50,000. HDIM: Quality measurement that keeps up with care."

**On-screen text:**
- "$80/month vs $50K+/month"
- "healthdatainmotion.com"
- "Seeking $1.5M Seed"

---

## 2-MINUTE VERSION (Extended for Interview)

### Additional Scene: Why We Built This (1:00 - 1:20)
**Visual:** Founder headshot or office setting

**Voiceover:**
> "I lost my mother to breast cancer at 54. As an enterprise architect serving millions of patients through health information exchanges, I watched organizations struggle to afford the quality tools that could have caught her cancer earlier. Every platform was too expensive, too slow, or locked to one vendor."

---

### Additional Scene: AI Advantage (1:20 - 1:35)
**Visual:** Development metrics comparison

**Voiceover:**
> "Using AI-assisted development, I built what would normally take a team of ten people eighteen months. Same code quality—534 test files, zero critical vulnerabilities. This isn't a shortcut; it's a sustainable competitive advantage that compounds with every feature."

**On-screen comparison:**
```
Traditional:  9.5 FTEs → 18 months → $1.7M
HDIM:         1 FTE   → 3 months  → $46K
```

---

### Additional Scene: Unit Economics (1:35 - 1:45)
**Visual:** Financial metrics with benchmark comparisons

**Voiceover:**
> "Our unit economics are best-in-class. LTV to CAC of 15 to 1 versus the industry benchmark of 5 to 1. CAC payback in 4 months versus 18. We can profitably serve organizations that enterprise vendors can't touch."

---

### Additional Scene: Ask (1:45 - 2:00)
**Visual:** Roadmap timeline, then logo

**Voiceover:**
> "We're seeking $1.5 million to bring HDIM to every healthcare organization that needs it. First pilots. SOC2 certification. Then scale. Help us give smaller organizations the quality tools they deserve."

**On-screen text:**
- "Seeking $1.5M Seed"
- "$6M Pre-Money Valuation"
- "aaron@hdim.health"

---

## FUNCTIONALITY SHOWCASE SCENES

### Optional: Custom Measure Builder (30 seconds)
**Visual:** Monaco CQL editor in action

**Action Sequence:**
1. Open Measure Builder
2. Show CQL syntax highlighting
3. Select FHIR value sets
4. Run test against sample patients
5. Publish measure

**Voiceover:**
> "Clinical users create custom quality measures without code. Our Monaco-based CQL editor provides IntelliSense, value set binding, and automated testing. New measures deploy in hours, not weeks."

---

### Optional: MFA Security Flow (20 seconds)
**Visual:** MFA setup and login

**Action Sequence:**
1. Enable MFA in settings
2. Scan QR code with authenticator
3. Enter TOTP code
4. Show recovery codes

**Voiceover:**
> "HIPAA-compliant security with TOTP multi-factor authentication. Eight recovery codes for account access. SOC2-ready infrastructure from day one."

---

### Optional: Risk Stratification (30 seconds)
**Visual:** Patient risk dashboard

**Action Sequence:**
1. Show 7-dimension risk assessment
2. Highlight high-risk factors
3. Show 5 validated risk models
4. Display predicted outcomes

**Voiceover:**
> "Five validated risk models—Charlson, Elixhauser, LACE, HCC, and Frailty—calculate in real-time. Seven-dimension risk stratification identifies patients who need intervention now, not next month."

---

### Optional: Health Scoring (20 seconds)
**Visual:** 5-component health score visualization

**Action Sequence:**
1. Show radar chart with 5 components
2. Highlight weak areas
3. Show trend over time

**Voiceover:**
> "Our 5-component health score provides a holistic patient view: Physical, Mental, Social, Preventive, and Chronic Disease management. Weighted scoring identifies the biggest opportunities for improvement."

---

## RECORDING CHECKLIST

### Environment Setup
- [ ] Demo environment running
- [ ] Sample patient data loaded (50+ patients)
- [ ] All 61 measures verified
- [ ] MFA enabled on demo account
- [ ] Browser profile clean (incognito)
- [ ] Resolution: 1920x1080
- [ ] Zoom: 100%

### Technical Setup
- [ ] Screen recording software (OBS/Loom)
- [ ] External microphone tested
- [ ] Quiet recording environment
- [ ] Backup demo environment ready

### Content Checklist
- [ ] Script practiced (read aloud 3x)
- [ ] All clicks rehearsed
- [ ] Timing verified (stopwatch)
- [ ] Backup takes recorded

### Post-Production
- [ ] Trim dead air
- [ ] Add transitions
- [ ] Include timer overlay for <200ms
- [ ] Export at 1080p H.264
- [ ] Upload to YouTube (unlisted)
- [ ] Test link works

---

## KEY DEMO FLOWS

### Flow 1: Real-Time Quality (Must Show)
```
Login → Dashboard → Search "Maria Garcia" →
Patient 360 (instant load) → Care Gaps tab →
Click "Schedule Mammogram" → Success notification
```
**Duration:** 20-25 seconds
**Key point:** Emphasize instant load time

### Flow 2: Measure Builder (Optional)
```
Measure Builder → New Measure →
CQL Editor (type sample code) →
Value Set Picker → Test → Publish
```
**Duration:** 25-30 seconds
**Key point:** Self-service, no code deployments

### Flow 3: Risk Dashboard (Optional)
```
High-Risk Patients → Select patient →
Risk Models panel → Show all 5 scores →
Predicted outcomes → Intervention recommendations
```
**Duration:** 20-25 seconds
**Key point:** Multiple validated models

---

## TALKING POINTS FOR LIVE DEMO

### If Asked About Architecture:
> "28 microservices, all stateless, Kubernetes-ready. The CQL engine is template-driven—we can add new measures in hours. HAPI FHIR 7.x for the data layer, Kafka for events, PostgreSQL with row-level tenant isolation."

### If Asked About Security:
> "TOTP multi-factor authentication with recovery codes. JWT tokens with 15-minute expiry. HIPAA-compliant caching—we reduced cache TTLs by 99.7%. Vulnerability scanning runs on every commit. Zero critical CVEs."

### If Asked About AI Development:
> "I use Claude and other AI tools for code generation, but every line is reviewed and tested. 534 test files. The key insight is that AI handles the boilerplate while I focus on architecture and business logic. This methodology scales—every new feature benefits from it."

### If Asked About Competition:
> "Epic and Cerner charge $50K+ a month and take a year to implement. We deploy in days for $80 to $10K a month. The mid-market—15,000+ organizations between enterprise and FQHCs—is completely underserved. That's our wedge."

---

*Script Version: 2.0*
*Platform Version: v1.5.0*
