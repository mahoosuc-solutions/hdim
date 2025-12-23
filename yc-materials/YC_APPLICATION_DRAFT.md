# Y Combinator Application Draft - HDIM

## Company

**Company name:** HDIM (HealthData-in-Motion)

**Company URL:** healthdatainmotion.com

**One-liner (max 70 chars):**
Real-time clinical quality APIs with 61 HEDIS measures and custom CQL builder

---

## What does your company do? (max 200 chars)

HDIM is the "Stripe for healthcare quality": 61 HEDIS measures + custom measure builder, sub-second CQL evaluation, real-time care gap alerts. FHIR-native, deploys in days.

---

## Category
Healthcare / Developer Tools / B2B SaaS

---

## What is your company going to make?

We're building modern infrastructure for healthcare quality measurement. Today, health systems pay $50K-500K/month for legacy platforms (Epic Healthy Planet, Cerner HealtheIntent) that calculate quality measures in overnight batches. By the time they identify a care gap, the patient has left.

HDIM calculates **61 HEDIS quality measures** in sub-second time at point of care. Our platform includes:
- **Custom Measure Builder** - Clinical users create measures with a VS Code-like CQL editor, FHIR value set picker, and automated testing against sample patients
- **Real-time Clinical Decision Support** - WebSocket-based alerts to care teams when health scores change or care gaps are detected
- **5-component Health Scoring** - Holistic patient view (Physical, Mental, Social, Preventive, Chronic Disease)
- **5 Validated Risk Models** - Charlson, Elixhauser, LACE, HCC, and Frailty indices for risk stratification

We deploy in days instead of months, plug into existing infrastructure via FHIR/IHE standards, and are SMART on FHIR OAuth 2.0 certified.

**Technical differentiators:**
- **27 microservices** with event-driven Kafka architecture (vs. monolithic legacy)
- **Template-driven CQL engine** - Add measures in hours, not weeks of code deployments
- **Enterprise Kubernetes infrastructure** - Auto-scaling (HPA), high availability (PDB), full observability (Prometheus/Grafana/Jaeger)
- **Multi-tenant isolation** - Verified with 41 security test cases, zero critical CVEs
- **Parallel batch processing** - 1,000+ patients/minute population health calculations

**Business model:**
- SaaS tiers: $80/mo (small practices) → $10K+/mo (health systems)
- Per-measure and per-patient pricing at scale ($0.50-2 PPPM)
- Implementation services for enterprise ($10-50K)

---

## Why did you pick this idea to work on?

I lost my mother to breast cancer at 54. Her cousins shared the same fate. I believe with better data and earlier intervention, she and many others could have lived longer, more productive lives.

My path to this problem wasn't direct. I started on the factory floor at Borg Warner Automotive, earned my CS degree at night from Baker College Online, and built automation systems at Porous Material Inc. But when I moved into healthcare IT, I found my calling—and my frustration.

As Integration Architect at HealthInfoNet (Maine's HIE), I built the systems connecting healthcare providers across the state. As Enterprise Architect at Healthix (NY's largest HIE), I designed interoperability infrastructure serving millions. At Verato, I helped organizations solve their hardest patient matching problems. Throughout all of it, I saw the same pattern: **the organizations that needed quality measurement tools the most could afford them the least.**

Every platform we wanted was either too expensive ($50K+/month), too hard to implement (6-12 months), or locked to a single EHR vendor. I started using AI-assisted coding with a specific goal: leverage modern tools to build what was previously impossible for resource-constrained organizations.

HDIM is the result—61 HEDIS measures, a custom measure builder, real-time clinical decision support, and enterprise infrastructure. Built by one person with AI tools, designed for the organizations I've spent my career serving.

The value-based care market is $1.5T and growing. CMS is pushing everyone toward quality-based payments, but the infrastructure is 20 years old. Epic and Cerner have near-monopolies on EHRs, but their quality modules are bolted-on afterthoughts that cost more than some organizations' entire IT budget.

We're building what Stripe did for payments: take something that requires expensive enterprise contracts and million-dollar implementations and turn it into an API call.

---

## What do you understand that others don't?

**Insight 1: Quality measurement should be real-time, not batch.**
Everyone assumes quality measures are calculated weekly/monthly because that's how legacy systems work. But CQL can evaluate in milliseconds - the batch paradigm exists because of 1990s database architecture, not clinical necessity. When only 8% of patients receive all recommended preventive care (AHRQ), the timing of insights matters. A care gap identified 24 hours after the patient leaves is a missed opportunity.

**Insight 2: Small organizations will pay for simplicity, not just savings.**
As David Nash, MD notes, "most third-party software is designed and priced for large organizations." But FQHCs and rural clinics aren't just priced out - they're complexity-averse. When the industry standard is 6-8 months to implementation, a platform that deploys in days is worth paying for even at similar price points.

**Insight 3: The real lock-in isn't data, it's measure logic.**
Health systems are terrified of switching quality platforms because their custom measure definitions are trapped in proprietary formats. HDIM uses standard CQL - measures are portable, testable, and shareable. This removes the biggest barrier to adoption while building an ecosystem of reusable clinical logic.

---

## Who are your competitors?

**Epic Healthy Planet** - Market leader, $50K+/month, requires Epic EHR, 6-12 month implementations
**Cerner HealtheIntent** - Similar to Epic, Cerner-only, declining market share post-Oracle
**Innovaccer** - Well-funded startup ($200M+), SaaS, but still proprietary data model
**Health Catalyst** - Analytics-focused, expensive, long implementations

**Our advantages:**

| Capability | HDIM | Competitors |
|------------|------|-------------|
| **Price** | $80-10K/mo | $50K-500K/mo |
| **Deployment** | Days | 6-12 months |
| **HEDIS Measures** | 61 (complete MY2024) | Varies, often partial |
| **Custom Measures** | Self-service builder | Code deployments |
| **Real-time CDS** | Sub-second WebSocket | Overnight batch |
| **Risk Models** | 5 validated indices | Usually 1-2 |
| **EHR Lock-in** | Works with ANY EHR | Single vendor |
| **Standards** | FHIR-native, SMART on FHIR | Proprietary |

---

## How do you know people want this?

1. **Quality is late today:** Only 8% of patients get all recommended preventive care (AHRQ) because batch processing delivers insights 24-72 hours after the visit.
2. **Implementation pain:** 6-8 month implementations are industry standard (ACO Health Solutions). We deploy in days.
3. **Cost barrier:** "Most third-party software is designed and priced for large organizations" (David Nash, MD). FQHCs and small practices need an $80/month option, not $50K+.
4. **Workflow matters:** Physicians juggle 6-20 external tools and avoid them if they break EHR workflow; EHR-integrated alerts improve prescribing by 41% (PROMPT-HF).
5. **Standards tailwind:** NCQA is going all-digital by 2030; CQL cut measure build/test time by ~90% (NCQA). We're already 100% CQL/FHIR-native.

---

## What's the business model?

**Pricing tiers:**
- Starter: $80/month (10 providers, 5 measures)
- Growth: $500/month (50 providers, all measures)
- Professional: $2,000/month (200 providers, priority support)
- Enterprise: $10,000+/month (unlimited, SLA, dedicated support)

**Unit economics:**
- Per-patient-per-month pricing at scale ($0.50-2 PPPM)
- Implementation fees for enterprise ($10-50K)
- Custom measure development ($5-20K per measure)

**Target ACV:** $20K-100K for mid-market, $200K+ for enterprise

---

## Progress / current state

**Platform built and production-ready:**
- **27 microservices** with 61 HEDIS measures (complete HEDIS MY2024 coverage)
- **Custom Measure Builder** with Monaco CQL editor, value set picker, automated testing, and publishing workflow
- **5 validated risk models** (Charlson, Elixhauser, LACE, HCC, Frailty) with 154 TDD tests
- **Real-time CDS** with WebSocket alerts, 5-component health scoring, 7-dimension risk stratification
- **Angular clinical portal** with 15+ pages (Dashboard, Patients, Care Gaps, Measure Builder, Agent Builder)

**Enterprise infrastructure:**
- **Kubernetes-ready** with HPA (11 services auto-scale), PDB (high availability), multi-environment overlays
- **Full observability** stack: Prometheus metrics, Grafana dashboards, Jaeger distributed tracing
- **Automated deployment** with rollback scripts and comprehensive smoke tests (21 services validated)

**Security & compliance:**
- **SMART on FHIR** OAuth 2.0/OIDC authentication
- **Zero critical CVEs** (patched Spring Boot, PostgreSQL, Logback vulnerabilities)
- **Multi-tenant isolation** verified with 41 security test cases
- **500+ integration tests** across all services

**Ready for:** Pilot deployments with health systems, ACOs, and FQHCs

---

## How much have you raised?

Self-funded. I've invested my own time and resources to build a production-grade platform:
- 27 microservices with 61 HEDIS measures
- Custom Measure Builder with CQL editor and automated testing
- Enterprise Kubernetes infrastructure with full observability
- 5 validated risk models and real-time clinical decision support
- 500+ integration tests, zero critical CVEs

Now seeking funding to bring HDIM to every healthcare system that needs it: reducing costs, improving outcomes, and helping people live longer, healthier lives.

---

## Where do you see the company in 5 years?

HDIM becomes the default quality measurement infrastructure for value-based care, similar to how Stripe became default payment infrastructure for internet businesses.

**Year 1:** 20 paying customers, $300K ARR, SOC 2 certified
**Year 2:** 100 customers, $2M ARR, first enterprise deals
**Year 3:** 300 customers, $8M ARR, HITRUST certification
**Year 5:** 1,000+ customers, $40M ARR, international expansion

**Already built** (accelerating our roadmap):
- Risk adjustment (HCC risk scoring with V24/V28 crosswalk)
- Clinical decision support (real-time WebSocket CDS with rule engine)
- Population health analytics (batch processing 1,000+ patients/minute)

**Next to build:**
- Prior authorization automation
- Payer integration APIs
- Mobile clinician app

---

## Why will you win?

1. **Complete measure library, not a prototype:** 61 HEDIS measures operational today (CDC, CBP, COL, BCS, CCS, and 56 more). Competitors show demos with 3-4 measures; we have production-ready coverage for the entire HEDIS MY2024 specification.

2. **Custom Measure Builder:** Clinical users can create, test, and publish custom measures without code. Monaco CQL editor + FHIR value set picker + automated testing against sample patients. No other platform offers this self-service capability.

3. **Technical moat:** NCQA reports that CQL "reduced the time to specify, implement and test new quality measures by as much as 90%." Our template-driven engine adds measures in hours. We have 154 TDD tests on our risk models alone.

4. **Enterprise-ready infrastructure:** Kubernetes with auto-scaling (HPA on 11 services), high availability (PDB), full observability (Prometheus/Grafana/Jaeger), and automated rollback. This isn't a demo—it's production infrastructure.

5. **Pricing disruption:** We can profitably serve the 30M+ patients at FQHCs and small practices that Epic/Cerner can't touch. Entry point: $80/month vs. $50K+/month.

6. **Standards bet:** FHIR-native with SMART on FHIR OAuth 2.0. Integration gets easier every year while competitors' proprietary data models become liabilities. NCQA's 2030 all-digital roadmap validates this direction.

7. **Real-time, not batch:** Sub-second CQL evaluation at point of care. WebSocket-based clinical alerts. 5-component health scoring updated in real-time. The PROMPT-HF trial showed EHR-integrated alerts improve prescribing by 41%.

8. **Founder-market fit:** Years of public health IT experience building solutions for resource-constrained organizations. I've lived this problem—told "Epic costs $500K" when entire IT budgets are $200K.

---

## What's your unfair advantage?

1. **Factory floor to Enterprise Architect.** I started on the manufacturing floor at Borg Warner, earned my CS degree while working, and worked my way up to Enterprise Architect at Healthix (NY's largest HIE). I know what it takes to build systems that actually work in production.

2. **I've architected HIE infrastructure serving millions.** As Integration Architect and MPI Architect at HealthInfoNet, I built the systems connecting healthcare providers across Maine. As Enterprise Architect at Healthix, I designed interoperability infrastructure for New York. I understand healthcare integration at scale.

3. **AI-augmented solo founder with production results.** I built this entire platform using AI-assisted development: 27 microservices, 61 HEDIS measures, 500+ tests, enterprise Kubernetes infrastructure. This proves one motivated founder with modern tools can build what previously required a team of 20.

4. **Deep domain expertise from the trenches.** Not just theory—I've spent years at HealthInfoNet, Healthix, and Verato solving real healthcare data problems: patient matching, identity resolution, system integration. I know where the bodies are buried.

5. **Personal mission.** This isn't a business opportunity I identified—it's a problem I've been trying to solve for years. My mother's death from breast cancer drives me to ensure others have access to the early detection and care coordination that might have saved her.

6. **Built for real-world integration.** HDIM is designed by someone who has spent years integrating healthcare systems. It speaks FHIR, HL7v2, and IHE profiles because I've implemented all of them in production. It's built to overlay existing infrastructure, not replace it.

7. **Production-grade from day one.** This isn't a demo or MVP—it's enterprise infrastructure with auto-scaling, observability, security testing, and zero critical CVEs. Healthcare buyers can trust it because it's built to their standards.

---

## Founders

**Aaron Bentley**
- Role: Founder & CEO

**Career Arc (Factory Floor → Enterprise Architect → Founder):**
- Started on the factory floor at Borg Warner Automotive in Ithaca, NY
- Earned CS degree from Baker College Online while working full-time
- Porous Material Inc.: Combined mechanical experience with software to build automation programs on embedded platforms
- **HealthInfoNet (Maine's HIE):** Integration Architect, Lead Developer, and MPI Architect—built the systems that connect healthcare providers across the state
- **Healthix (NY's largest HIE):** Enterprise Architect—designed large-scale healthcare interoperability infrastructure
- **Verato:** Integration Consultant and Problem Solver—helped healthcare organizations solve complex identity and data matching challenges

**Technical Depth:**
- Deep expertise in healthcare interoperability: FHIR R4, HL7v2, CQL, IHE profiles
- Master Patient Index (MPI) architecture—critical for patient matching across systems
- Built HDIM: 27 microservices, 61 HEDIS measures, 5 risk models, 500+ tests, enterprise Kubernetes infrastructure
- Leveraged AI-assisted development to build what would typically require a 20-person team

**Why Me:**
- I've spent my career connecting healthcare systems for organizations that can't afford enterprise solutions
- I know what clinicians actually need vs. what vendors sell them
- I've architected HIE infrastructure serving millions of patients—I understand scale

**Personal Mission:**
- Lost my mother to breast cancer at 54. This work honors her memory and aims to prevent similar outcomes through better data and earlier intervention.

---

## What do you need from YC?

1. **Credibility for enterprise sales:** Healthcare is conservative. YC stamp helps close deals with risk-averse buyers.

2. **Network for pilots:** Connections to health systems, ACOs, or healthcare founders who can provide early feedback and LOIs.

3. **Fundraising for long sales cycles:** Healthcare deals take 6-12 months. We need runway to survive the sales cycle before we can scale.

4. **Advice on GTM:** How to navigate enterprise sales as a small team. When to hire sales vs. founder-led. How to price.

---

## Demo Video

- Script and shot list: [DEMO_VIDEO_SCRIPT.md](DEMO_VIDEO_SCRIPT.md)
- Demo test flows: [DEMO_TEST_SCENARIOS.md](DEMO_TEST_SCENARIOS.md)
- Pending: add unlisted 1-minute video link after recording

---

## Additional Materials

- Technical architecture: [ARCHITECTURE_ONEPAGER.md](ARCHITECTURE_ONEPAGER.md)
- Product roadmap: [PRODUCT_ROADMAP.md](PRODUCT_ROADMAP.md)
- Product positioning: [PRODUCT_POSITIONING.md](PRODUCT_POSITIONING.md)
- Sample customer outreach: [CUSTOMER_OUTREACH.md](CUSTOMER_OUTREACH.md)
- Submission checklist: [YC_SUBMISSION_CHECKLIST.md](YC_SUBMISSION_CHECKLIST.md)
- GitHub repo: private; share on request with YC reviewers

---

## Technical Platform Summary

### Platform Metrics

| Category | Count | Details |
|----------|-------|---------|
| Backend Microservices | 27 | Full healthcare interoperability stack |
| HEDIS Measures | 61 | Complete HEDIS MY2024 coverage |
| Risk Models | 5 | Charlson, Elixhauser, LACE, HCC, Frailty |
| Clinical Portal Pages | 15+ | Dashboard, Patients, Care Gaps, Measure Builder, Agent Builder |
| Kubernetes Services | 21 | Auto-scaling, PDB, HPA configured |
| Integration Tests | 500+ | Comprehensive coverage across all services |
| Risk Model TDD Tests | 154 | Validated clinical algorithms |
| Critical CVEs | 0 | All critical/high vulnerabilities patched |

### Backend Services (27 Microservices)

**Core Clinical:**
- CQL Engine Service (61 measures, template-driven evaluation)
- Quality Measure Service (CDS, health scoring, risk stratification)
- Care Gap Service (detection, prioritization, matching)
- Patient Service (aggregation, timeline, health status)
- FHIR Service (HAPI FHIR R4 server)

**Integration & Events:**
- Gateway Service (SMART on FHIR OAuth 2.0, centralized auth)
- Event Router Service (Kafka event distribution)
- Event Processing Service (real-time event handling)
- CDR Processor Service (HL7v2/FHIR message processing)
- EHR Connector Service (Epic, Cerner, Meditech adapters)

**Analytics & AI:**
- Predictive Analytics Service (readmission risk, cost prediction)
- AI Assistant Service (Claude-powered clinical assistant)
- Agent Builder Service (custom AI agent creation)
- Agent Runtime Service (agent execution)
- Analytics Service (dashboards, reports)

**Specialty:**
- HCC Service (V24/V28 HCC risk adjustment)
- Prior Auth Service (prior authorization workflow)
- Consent Service (patient consent management)
- SDOH Service (social determinants of health)
- QRDA Export Service (CMS reporting)

### Clinical Decision Support Stack

**5-Component Health Scoring:**
- Physical Health (30% weight): Vitals, labs, BMI
- Mental Health (25% weight): PHQ-9, GAD-7 assessments
- Social Determinants (15% weight): Housing, food, transportation
- Preventive Care (15% weight): Screenings, immunizations
- Chronic Disease Management (15% weight): Control metrics

**7-Dimension Risk Stratification:**
- Chronic conditions (FHIR Condition resources)
- Mental health screenings (database)
- Uncontrolled vitals/labs (HbA1c, BP, cholesterol)
- Medication adherence (MedicationStatement)
- Open care gaps (urgency-weighted)
- Healthcare utilization (ED visits, hospitalizations)
- Deteriorating trends (ChronicDiseaseMonitoring)

**Risk Models:**
- Charlson Comorbidity Index (10-year mortality)
- Elixhauser Comorbidity Index (hospital mortality, 31 categories)
- LACE Index (30-day readmission risk)
- HCC Risk Score (CMS risk adjustment, V24/V28)
- Frailty Index (functional decline)

### Infrastructure

**Kubernetes (Production-Ready):**
- 11 services with HPA (auto-scaling)
- 11 services with PDB (high availability)
- 3-tier resource allocation
- Multi-environment overlays (staging, production)

**Observability:**
- Prometheus (metrics, 15-day retention)
- Grafana (dashboards, JVM metrics)
- Jaeger (distributed tracing, OTLP)

**Security:**
- SMART on FHIR OAuth 2.0/OIDC
- JWT validation at gateway
- HMAC-signed trusted headers
- Multi-tenant isolation (41 test cases)
- Zero critical CVEs

### Custom Measure Builder

**Workflow:**
1. Create draft with metadata
2. Edit CQL in Monaco editor (VS Code experience)
3. Bind FHIR value sets (10,000+ clinical codes)
4. Test against sample patient cohorts
5. Review pre-publish checklist
6. Publish with semantic versioning

**Features:**
- Syntax highlighting and validation
- Full-screen editing mode
- Automated test execution
- Immutable audit trail
- Batch operations (publish, delete, export)
