# HDIM — FAQ & Competitive Positioning

*For sales conversations, partnership discussions, and internal objection handling.*

---

## Frequently Asked Questions

### General

**Q: What is HDIM?**

HealthData-in-Motion (HDIM) is a real-time healthcare quality measurement platform. It evaluates clinical quality measures (HEDIS, CQL-based) against patient data, identifies care gaps, and surfaces actionable insights to providers at the point of care.

**Q: Who is HDIM for?**

Four primary audiences: (1) Medicare Advantage health plans optimizing Star Ratings, (2) ACOs maximizing shared savings through quality performance, (3) health systems managing MIPS and commercial quality programs, and (4) VBC enablement platforms that need an embeddable quality engine.

**Q: How is HDIM different from existing HEDIS vendors?**

Three fundamental differences: (1) Real-time evaluation — not annual batch. Gaps are identified within hours of data receipt, not months later. (2) FHIR R4 native — the entire data pipeline is FHIR, not proprietary. (3) Transparent CQL logic — measure definitions are readable, testable, and version-controlled, not a black box.

---

### Technical

**Q: What data formats does HDIM accept?**

FHIR R4 JSON is the primary format. HDIM can also accept data via Kafka events, bulk FHIR exports (NDJSON), and has a Data Ingestion Service for custom formats. If your source system speaks FHIR, integration is straightforward. If it doesn't, we provide the transformation layer.

**Q: How long does integration take?**

5–10 weeks for FHIR-native sources (EHR with FHIR APIs, FHIR-based data lakes). Add 2–4 weeks if data transformation from non-FHIR sources is needed. The longest phase is typically data mapping and validation, not technology setup.

**Q: Can HDIM run on-premises?**

Yes. HDIM deploys as Docker containers orchestrated by Docker Compose or Kubernetes. Full feature parity across SaaS, private cloud, and on-premises deployments. The platform has no cloud-specific dependencies.

**Q: What about scale? Can it handle millions of members?**

Yes. The architecture is horizontally scalable — each service scales independently via Kubernetes. Kafka provides event-driven throughput for large populations. CQL evaluation is <2 seconds per patient. Batch evaluation is parallelized across consumers.

**Q: Is the CQL engine proprietary?**

No. HDIM uses **CQL (Clinical Quality Language)**, an HL7 standard. Measures are expressed in standard CQL and can be created, modified, or validated by anyone who understands the specification. HDIM adds production infrastructure around CQL: a managed library, versioning, batch evaluation, event-driven triggers, and AI-assisted authoring.

**Q: Does HDIM replace my EHR?**

No. HDIM is not an EHR. It consumes data from EHRs (via FHIR APIs, CDS Hooks, or ADT feeds) and produces quality insights that flow back into EHR workflows. HDIM complements your EHR investment.

**Q: What FHIR resources are supported?**

20+ resource types including Patient, Observation, Condition, Procedure, MedicationRequest, Encounter, Coverage, Immunization, and more. Full list in the [Technical Specifications](06-technical-specifications.md#fhir-r4-conformance).

---

### Security & Compliance

**Q: Is HDIM HIPAA-compliant?**

Yes, by design. HIPAA controls are built into the architecture, not bolted on:
- PHI cache TTL ≤ 5 minutes
- 100% API audit coverage (automatic, no manual instrumentation)
- 15-minute idle session timeout with audit trail
- Tenant isolation at 6 layers (gateway, app, query, database, cache, events)
- No PHI in browser logs (ESLint-enforced, build fails on `console.log`)
- JWT HttpOnly cookies (prevents XSS token theft)

Full details in [Security & Compliance](05-security-and-compliance.md).

**Q: Will you sign a BAA?**

Yes. Standard BAA included with SaaS deployments. Custom BAA available for private cloud.

**Q: Has HDIM been penetration tested?**

Security audit tooling is built into the CI/CD pipeline. Customer-coordinated penetration testing is supported for private cloud and on-premises deployments.

**Q: How is tenant data isolated?**

Six layers of isolation: gateway routing, application-level tenant filter, SQL query scoping (`WHERE tenant_id = ?` on every query), separate database schemas, tenant-prefixed cache keys, and Kafka topic partitioning. Cross-tenant data access is architecturally impossible.

---

### Business

**Q: What does HDIM cost?**

Pricing is structured to deliver 5–10x ROI and varies by deployment model:
- **SaaS:** Per-member-per-month (PMPM)
- **Private Cloud:** Annual license + hosting
- **On-Premises:** Perpetual license + maintenance
- **OEM/Embedded:** Custom per-member licensing

Contact for specific pricing based on your member count and deployment preference.

**Q: What's the ROI?**

For a 100K-member MA plan: improving Star Rating by 0.5 stars generates $30–50M in additional CMS bonus revenue annually. HDIM's cost is a fraction of that uplift. Typical payback is 3–6 months for mid-size plans. Details in [Use Cases & ROI](04-use-cases-and-roi.md).

**Q: How fast can we go live?**

Demo in a day. Pilot with real data in 4–6 weeks. Full deployment in 3–6 months depending on data integration complexity and organizational readiness.

**Q: Do you handle measure updates when NCQA changes HEDIS specifications?**

CQL-based measure definitions are version-controlled. When NCQA publishes updated specifications, we update the CQL libraries. Because measures are expressed in standard CQL (not compiled code), updates are transparent and testable before deployment.

---

## Competitive Positioning

### HDIM vs. Legacy HEDIS Vendors (Cotiviti, Inovalon)

| Dimension | Legacy Vendors | HDIM |
|-----------|---------------|------|
| **Evaluation timing** | Annual batch (6–12 month lag) | Real-time (hours) |
| **Data format** | Proprietary ingestion | FHIR R4 native |
| **Measure logic** | Black box | Open CQL (testable, auditable) |
| **Provider engagement** | Portal + fax | CDS Hooks in EHR + real-time dashboards |
| **Deployment** | Vendor-hosted SaaS only | SaaS, private cloud, or on-premises |
| **Integration approach** | Heavy custom ETL | Standards-based (FHIR, CDS Hooks, SMART) |
| **Time to value** | 6–12 months | 5–10 weeks |
| **Cost structure** | Large annual contracts | Flexible PMPM or license |

**Positioning statement:** "HDIM isn't competing with Cotiviti for the same annual HEDIS submission contract. We're offering the next generation: real-time quality evaluation that makes HEDIS actionable year-round, not just at submission time."

### HDIM vs. Population Health Platforms (Arcadia, Lightbeam)

| Dimension | Pop Health Platforms | HDIM |
|-----------|---------------------|------|
| **Primary focus** | Broad analytics + reporting | Deep quality measurement + action |
| **CQL engine** | Limited or none | Production-grade, <2 second evaluation |
| **HEDIS calculation** | Dependent on vendor or manual | 10 measures with full calculators |
| **Event architecture** | Batch ETL | Event-sourced (CQRS), real-time |
| **Provider workflow** | Dashboards | CDS Hooks + pre-visit plans + dashboards |
| **Multi-tenancy** | Application-level | 6-layer database-level isolation |
| **OEM capable** | Rarely | API-first, white-label ready |

**Positioning statement:** "Population health platforms are wide but shallow on quality. HDIM goes deep — real CQL evaluation, real-time gap detection, real EHR integration via CDS Hooks. Use HDIM alongside your pop health platform, or let HDIM be the quality engine behind it."

### HDIM vs. Building In-House

| Dimension | Build In-House | HDIM |
|-----------|---------------|------|
| **Time to first measure** | 12–18 months | 5–10 weeks |
| **Engineering cost** | $2–5M/year (5–10 FTEs) | Fraction (licensing) |
| **Measure maintenance** | Annual re-engineering | Library update |
| **FHIR compliance** | Requires FHIR expertise | Built-in |
| **HIPAA architecture** | Must design from scratch | Built-in |
| **Multi-tenancy** | Complex to implement | Production-ready |
| **Event sourcing** | Significant engineering effort | Built-in (CQRS + Kafka) |
| **CDS Hooks** | Custom integration per EHR | Standard implementation |

**Positioning statement:** "Building a quality engine is a 2–3 year project and an ongoing maintenance burden. HDIM gives you a production-grade platform in weeks, maintained by a team whose entire focus is healthcare quality measurement."

---

## Objection Handling

### "We already have a HEDIS vendor."

"Good — that means quality measurement is a priority. HDIM doesn't replace your annual HEDIS submission. It makes HEDIS actionable during the measurement year. Your current vendor tells you your rates at year-end. HDIM tells your providers which patients need intervention today."

### "We're too small to need this."

"If you have 10,000 Medicare Advantage members, a half-star improvement is $3–5M in CMS revenue. HDIM's SaaS pricing scales with member count. The ROI math works at 10K members."

### "We want to build this ourselves."

"Most organizations underestimate three things: (1) the complexity of CQL evaluation at scale, (2) the ongoing maintenance of HEDIS measure specifications (NCQA changes them annually), and (3) the data engineering required for real-time FHIR pipelines. HDIM has 58 microservices, 40 databases, and 54 event topics. That's 2+ years and $5M+ of engineering. You could be live in 10 weeks."

### "Is the company big enough to support us?"

"Fair question. Three things to consider: (1) The codebase has 1,600+ REST endpoints, 40 databases, event sourcing, and complete HIPAA infrastructure — this is a mature, production-grade platform. (2) HDIM is built on open standards (FHIR R4, CQL, CDS Hooks) — there's no vendor lock-in. (3) Every deployment option includes on-premises, so your data and system are always under your control."

### "How do we know the quality calculations are accurate?"

"Every measure is expressed in CQL — an HL7 standard that's readable and testable. You can inspect the logic, run it against known patient data, compare results to your current vendor, and verify transparency that black-box solutions can't provide. HDIM also provides full event sourcing — every calculation is traceable to the exact data and logic version that produced it."

### "We need SOC 2 / HITRUST certification."

"HDIM's architecture maps to SOC 2 and HITRUST controls. The platform has built-in audit logging (100% coverage), encryption at rest and in transit, RBAC with 11 roles, and tenant isolation at 6 layers. Formal certification depends on deployment context — for SaaS, we pursue vendor certification. For on-premises, HDIM provides the controls and evidence that feed into your existing compliance program."

---

## Key Differentiators (Elevator Pitch)

1. **Real-time, not batch.** Gaps identified in hours, not months.
2. **FHIR-native, not proprietary.** Standards-based data and APIs throughout.
3. **Transparent CQL, not a black box.** You can read, test, and audit every measure.
4. **EHR-embedded, not another portal.** CDS Hooks surface gaps inside Epic/Cerner.
5. **Deploy anywhere.** SaaS, private cloud, or on-premises — same platform, same features.
6. **Event-sourced.** Full audit trail, state reconstruction, and real-time streaming.

---

*HealthData-in-Motion | https://healthdatainmotion.com | February 2026*
