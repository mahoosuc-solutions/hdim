# BSL Launch Email Templates — March 9, 2026

Use these templates for the v3.0.0 BSL 1.1 launch of HealthData-in-Motion at HIMSS 2026.

Personalize the `[bracketed]` sections for each recipient.

---

## Template A: Pilot Customers / Providers (NYC, Maine)

**Subject:** HDIM is now open source — free to evaluate for your quality programs

**From:** info@mahoosuc.solutions

---

Hi [First Name],

I wanted to share some news — we just open-sourced HealthData-in-Motion (HDIM), the healthcare interoperability platform I've been building at Mahoosuc Solutions.

Starting today, the full platform is available under the Business Source License 1.1. That means your team can download, install, and evaluate the entire stack — HEDIS quality measure evaluation, FHIR R4 ingestion, care gap detection, clinical decision support — at no cost for development, testing, and proof-of-concept work.

**Why this matters for [Organization Name]:**

- Automate HEDIS measure evaluation against your patient population
- Identify care gaps before they become Stars rating penalties
- Run CQL-based quality measures across FHIR R4 data — no manual chart review
- 51 microservices, fully Dockerized, deploys in minutes

**What's included:**

- Full source code: https://github.com/mahoosuc-solutions/hdim
- Live platform overview: https://landing-page-ecru-five-65.vercel.app
- Interactive API docs (62+ documented endpoints with Swagger UI)
- HIPAA-compliant architecture with multi-tenant isolation

I'm at HIMSS in Las Vegas this week if you'd like to see a live demo. Otherwise I'd welcome 30 minutes on your calendar when I'm back to walk through how HDIM could fit into [your quality measurement workflow / your HEDIS reporting process / your care gap closure program].

Would that be useful?

Best,
Aaron
Mahoosuc Solutions
info@mahoosuc.solutions

---

## Template B: Health IT Vendors (InterSystems, Rhapsody, etc.)

**Subject:** Open-source HEDIS/FHIR platform — potential integration opportunity

**From:** info@mahoosuc.solutions

---

Hi [First Name],

I've been following what [InterSystems / Rhapsody Health] is doing in [healthcare interoperability / integration engine space] and wanted to introduce something that may complement your work.

Today we released HealthData-in-Motion (HDIM) as open source under BSL 1.1 — it's an enterprise platform for HEDIS quality measure evaluation, FHIR R4 data processing, and care gap detection. We announced it here at HIMSS 2026.

**Where I see a fit with [Company]:**

- HDIM consumes FHIR R4 bundles and runs CQL-based quality measures — it could sit downstream of [HealthShare / Rhapsody] integration engines
- IHE gateway support (XDS.b, XCA, PIXv3) for HIE participation
- Open APIs (62+ documented endpoints) designed for integration
- Multi-tenant architecture that supports managed service delivery

**The model:**

- Source code is open for evaluation, development, and testing
- Production deployments require a commercial license from Grateful House Inc.
- We're looking for integration partners who want to offer quality measurement capabilities to their existing customer base

I'm at HIMSS this week — happy to grab coffee and walk through the architecture. [I'll be at / I'm available at] [location/time if applicable]. Otherwise, let's find 30 minutes after the conference.

Source code: https://github.com/mahoosuc-solutions/hdim
Platform overview: https://landing-page-ecru-five-65.vercel.app

Best,
Aaron
Mahoosuc Solutions
info@mahoosuc.solutions

---

## Template C: Investors / Advisors

**Subject:** HDIM v3.0.0 launched — BSL open source, live at HIMSS

**From:** info@mahoosuc.solutions

---

Hi [First Name],

Quick update — today we released HealthData-in-Motion v3.0.0 under the Business Source License 1.1. The announcement is live at HIMSS 2026 in Las Vegas.

**What happened:**

- Full platform source code is now publicly available
- Licensed under BSL 1.1 (same model as CockroachDB, Sentry, HashiCorp)
- Free for development, testing, and evaluation
- Production use requires a commercial agreement with Grateful House Inc.
- Converts to Apache 2.0 on March 7, 2030 (4-year window)

**Why BSL and why now:**

- Open-core is the fastest path to enterprise adoption in healthcare IT — let quality teams evaluate without procurement friction
- The 4-year conversion window protects commercial licensing revenue while building community trust
- HIMSS timing puts this in front of 45,000+ healthcare IT decision-makers

**The business:**

- Grateful House Inc. (C-Corp) owns the IP and handles licensing
- Mahoosuc Solutions provides commercial implementation, managed deployments, and consulting
- Target: $50-100K in pilot revenue by Q2, $500K-1M ARR by year-end

**Platform at a glance:**

- 51 Java microservices + Angular clinical portal
- HEDIS quality measure evaluation, FHIR R4, care gap detection, clinical decision support
- HIPAA-compliant, multi-tenant, fully Dockerized
- 62+ documented API endpoints, 613+ automated tests

Source code: https://github.com/mahoosuc-solutions/hdim
Landing page: https://landing-page-ecru-five-65.vercel.app
Trust center: https://landing-page-ecru-five-65.vercel.app/resources/trust-center

Happy to discuss. I'll be at HIMSS through [day], then back in [Maine/NYC] next week.

Best,
Aaron

---

## Template D: Developer Community / Open-Source Announcement

**Subject:** We just open-sourced a 51-service healthcare quality platform (FHIR R4, HEDIS, CQL)

**From:** info@mahoosuc.solutions

---

Hi [First Name / Community],

Today we released HealthData-in-Motion (HDIM) — an enterprise healthcare interoperability platform — as open source under the Business Source License 1.1.

**What it is:**

HDIM evaluates clinical quality measures (HEDIS/CQL) against FHIR R4 patient data to identify care gaps, perform risk stratification, and generate quality reports. It's built for health plans, ACOs, and health systems doing value-based care.

**Tech stack:**

- 51 Java 21 / Spring Boot 3.x microservices
- Angular 17+ clinical portal
- PostgreSQL 16, Redis 7, Apache Kafka 3.x
- HAPI FHIR 7.x (R4), OpenTelemetry tracing
- Docker Compose — deploys in under 5 minutes
- 613+ automated tests, 62+ documented API endpoints (OpenAPI 3.0 + Swagger UI)

**Get started:**

```bash
git clone https://github.com/mahoosuc-solutions/hdim.git
cd hdim
docker compose up -d
# Gateway at localhost:8001, Clinical Portal at localhost:4200
```

**License:**

- BSL 1.1 — free for development, testing, evaluation, education, and research
- Production use requires a commercial license from Grateful House Inc.
- Converts to Apache License 2.0 on March 7, 2030

**Links:**

- GitHub: https://github.com/mahoosuc-solutions/hdim
- Landing page: https://landing-page-ecru-five-65.vercel.app
- API docs: Available via Swagger UI on each service
- Architecture overview: https://landing-page-ecru-five-65.vercel.app/resources/architecture

We'd love feedback, issues, and stars. If you're working in healthcare interoperability, HEDIS reporting, or FHIR tooling, take a look and let us know what you think.

Aaron
Mahoosuc Solutions
GitHub: https://github.com/mahoosuc-solutions/hdim

---

## Personalization Notes

### For InterSystems contacts
- Mention HealthShare Health Connect and IRIS for Health
- Integration angle: HDIM as a quality measurement layer downstream of their integration engine
- They care about IHE profiles — highlight XDS.b, XCA, PIXv3 support

### For Rhapsody Health contacts
- Mention Rhapsody Integration Engine and Corepoint
- Integration angle: HDIM consumes FHIR bundles that Rhapsody transforms from HL7v2/CDA
- They care about interoperability standards compliance

### For NYC providers
- Lead with HEDIS Stars impact and care gap closure
- Mention multi-payer quality reporting
- If they're an ACO, emphasize risk stratification and value-based care analytics

### For Maine providers
- Smaller organizations — emphasize Docker simplicity and low infrastructure requirements
- Local angle: "Built in Maine, for providers who need enterprise-grade tools without enterprise-grade budgets"
- Mention OR-HIE participation via IHE XCA federation
