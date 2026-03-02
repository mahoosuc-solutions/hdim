# Investor Repo Restructure — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Restructure the public hdim-investor repo from 7 flat files into a 13-file, 4-folder information architecture that tells the "full-stack enterprise proof" story for all investor audiences.

**Architecture:** Progressive-disclosure content structure. README is the landing page (5-second scan). Four folders serve different audience paths: executive/ (business investors), platform/ (product understanding), traction/ (execution proof), technical/ (DD deep-dives).

**Tech Stack:** Markdown files in git repo at `/mnt/wdblack/dev/projects/hdim-investor/`

**Source Material:** Pull content from main repo at `/mnt/wdblack/dev/projects/hdim-master/docs/` — investor docs, CLAUDE.md, feature docs, commit history.

---

### Task 1: Clean slate — remove old files, create folder structure

**Files:**
- Delete: `pitch-deck/INVESTOR-PITCH-DECK.md`
- Delete: `pitch-deck/INVESTOR-PITCH-DECK-SHORT.md`
- Delete: `due-diligence/INVESTOR-DUE-DILIGENCE-PACKAGE.md`
- Delete: `technical/INVESTOR-CODE-SAMPLES.md`
- Delete: `technical/PRODUCTION-READINESS-CHECKLIST.md`
- Delete: `technical/TECHNICAL-COMPETITIVE-ANALYSIS.md`
- Delete: `deployment/HOSPITAL-DEPLOYMENT-GUIDE.md`
- Create dirs: `executive/`, `platform/`, `traction/`
- Keep: `technical/` (reuse directory)

**Step 1: Remove old files and create new directories**

```bash
cd /mnt/wdblack/dev/projects/hdim-investor
rm -rf pitch-deck/ due-diligence/ deployment/
rm technical/INVESTOR-CODE-SAMPLES.md technical/PRODUCTION-READINESS-CHECKLIST.md technical/TECHNICAL-COMPETITIVE-ANALYSIS.md
mkdir -p executive platform traction
```

**Step 2: Commit the cleanup**

```bash
git add -A
git commit -m "chore: remove old structure for full restructure"
```

---

### Task 2: Write README.md — the landing page

**Files:**
- Overwrite: `README.md`

**Content spec:**

The README must achieve 5-second comprehension. Structure:

1. **Title + tagline** (1 line): `# HDIM — HealthData-in-Motion` + `Real-time healthcare quality measurement. Production-proven.`
2. **Metrics table** (immediate credibility): 59 services, 1,171 test classes, 80+ HEDIS measures, 62 API endpoints, <2s evaluation, 90-day deployment, CVE-remediated + ZAP-scanned
3. **What HDIM Does** (3 short paragraphs): Problem → Solution → Why different. No jargon. A non-technical angel investor should understand this.
4. **Platform Capabilities** (grid/table): Quality Measures, Care Gap Detection, Revenue Cycle (Wave-1), FHIR Interoperability, Custom Measure Builder, CMO Onboarding, Clinical Portal, Operations Orchestration, Security & Compliance
5. **For Investors** (navigation table): 4 rows mapping audience → document → read time
6. **Technology Highlights** (bullet list): Java 21, Spring Boot 3, PostgreSQL 16, Kafka, FHIR R4, event sourcing, 29 databases
7. **Contact** section
8. Footer: `Confidential — For Investor Use Only | March 2026`

**Source material:**
- Current README.md in investor repo (structure reference)
- `/mnt/wdblack/dev/projects/hdim-master/CLAUDE.md` (tech stack, service counts)
- `/mnt/wdblack/dev/projects/hdim-master/docs/INVESTOR_PACKAGE_README.md` (existing investor narrative)

**Step 1: Write the file**

Write complete README.md with all sections above.

**Step 2: Verify links**

All internal links must point to files that will exist after all tasks complete. Use relative paths.

**Step 3: Commit**

```bash
git add README.md
git commit -m "docs: add progressive-disclosure README landing page"
```

---

### Task 3: Write executive/ONE-PAGER.md

**Files:**
- Create: `executive/ONE-PAGER.md`

**Content spec:**

Must fit on one screen (~40 lines). Designed to paste into an email or forward to a partner.

1. **Header**: HDIM — Real-Time Healthcare Quality Measurement
2. **The Problem** (2 sentences): Healthcare orgs lose $500K-2M/year per HEDIS gap. Current solutions take 18-24 months to deploy and evaluate overnight, not real-time.
3. **The Solution** (4 bullets): Real-time HEDIS evaluation (<2s), 90-day deployment, works with any EHR via FHIR R4, now includes revenue cycle capabilities
4. **Proof Points** (metrics table): 59 services, 1,171 test classes, 80+ measures, Wave-1 revenue cycle shipped, CVE-remediated security posture
5. **The Ask**: Seed/Series A, use of funds (sales team, customer success, pilots)
6. **Contact**: Aaron Bentley + email

**Source material:**
- Current `due-diligence/INVESTOR-DUE-DILIGENCE-PACKAGE.md` lines 187-219 (existing 1-page summary)

**Step 1: Write the file**
**Step 2: Commit**

```bash
git add executive/ONE-PAGER.md
git commit -m "docs: add email-friendly one-pager for investor outreach"
```

---

### Task 4: Write executive/PITCH-DECK.md

**Files:**
- Create: `executive/PITCH-DECK.md`

**Content spec:**

12-slide refreshed meeting deck. Base on existing short pitch deck but update:

- **Slide 1 (Problem)**: Keep as-is, strong framing
- **Slide 2 (Solution)**: Add custom measure creation, CMO onboarding. Not just "evaluate measures" but "create, evaluate, and operationalize"
- **Slide 3 (Product)**: Full capabilities grid. Add Wave-1 revenue cycle, operations orchestration, measure builder UI. Update: 59 services, 1,171 test classes, 80+ measures
- **Slide 4 (Market)**: Keep TAM/SAM/SOM. Add health plan segment emphasis (800+ targets)
- **Slide 5 (Competitive)**: Update table. Add revenue cycle row. Show HDIM now spans quality + revenue (competitors don't)
- **Slide 6 (Business Model)**: Keep pricing, unit economics
- **Slide 7 (Founder)**: Keep Aaron bio. Add: "Built twice — Node.js then Java. 444 commits in February 2026 alone."
- **Slide 8 (Traction)**: Major update. Add: Wave-1 shipped, measure builder, CMO onboarding, security hardening, 360 assurance. Show velocity.
- **Slide 9 (Financials)**: Keep projections
- **Slide 10 (The Ask)**: Keep $5-7M Series A structure
- **Slide 11 (Why Now)**: Add CMS interoperability rules, FHIR mandate timeline
- **Slide 12 (Next Steps)**: Contact + available materials

**Source material:**
- Current `pitch-deck/INVESTOR-PITCH-DECK-SHORT.md` (base structure)
- Current `pitch-deck/INVESTOR-PITCH-DECK.md` (extended content)
- Main repo commit history since Feb 3 (traction data)

**Step 1: Write the file**
**Step 2: Commit**

```bash
git add executive/PITCH-DECK.md
git commit -m "docs: add refreshed 12-slide pitch deck with March 2026 updates"
```

---

### Task 5: Write executive/FAQ.md

**Files:**
- Create: `executive/FAQ.md`

**Content spec:**

Organized by category. Each answer: 2-4 sentences max, then a link to the deeper document.

**Business Questions:**
- What is HDIM? → platform/PLATFORM-OVERVIEW.md
- What's the market opportunity? → executive/PITCH-DECK.md Slide 4
- What's the business model? → executive/PITCH-DECK.md Slide 6
- How much are you raising? → $5-7M Series A
- What's the ROI for customers? → $722K/year savings, 22-day payback

**Technical Questions:**
- Is this production-ready? → traction/PRODUCTION-READINESS.md
- What's your security posture? → platform/SECURITY-COMPLIANCE.md
- How do you handle HIPAA? → Engineered, not added. 5-min cache TTL, audit trails, PHI filtering
- What's the technical moat? → Event sourcing (18-24 months to replicate)

**New Capability Questions:**
- Can customers create their own measures? → Yes, measure builder UI shipped Feb 2026
- Do you handle revenue cycle? → Wave-1: claims, remittance, price transparency
- What's CMO onboarding? → Dashboard + acceptance playbooks for health plan CMOs

**Competition Questions:**
- What if Epic/Optum competes? → 2+ year replication timeline
- How are you different from Arcadia/Inovalon? → Real-time vs. batch, FHIR-native vs. ETL

**Response matrix table** at the bottom mapping question → document → section.

**Source material:**
- Current `due-diligence/INVESTOR-DUE-DILIGENCE-PACKAGE.md` lines 62-108 (response matrix), lines 222-243 (FAQ)

**Step 1: Write the file**
**Step 2: Commit**

```bash
git add executive/FAQ.md
git commit -m "docs: add investor FAQ with response matrix"
```

---

### Task 6: Write platform/PLATFORM-OVERVIEW.md

**Files:**
- Create: `platform/PLATFORM-OVERVIEW.md`

**Content spec:**

The "full-stack proof" document. Walks through every platform layer with status indicators. Non-technical tone with technical specifics.

**Sections:**

1. **Executive Summary** (3 sentences): HDIM is a complete healthcare quality platform. Every layer — from data ingestion to clinical dashboards — is built, tested, and hardened. This document walks through each capability.

2. **Platform Layer Map** (visual table):
   - Data Ingestion: FHIR R4, ADT events, HL7 → Status: Production
   - Quality Engine: CQL evaluation, 80+ HEDIS measures, custom measure creation → Status: Production
   - Care Gap Detection: Real-time identification, closure workflows, patient engagement → Status: Production
   - Revenue Cycle: Claims processing, remittance reconciliation, price transparency → Status: Wave-1 Complete
   - Clinical Portal: Angular 17, CMO dashboards, operations orchestration → Status: Production
   - API Gateway: 4 modularized gateways, rate limiting, auth → Status: Production
   - Security: HIPAA, CVE remediation, ZAP scanning, audit trails → Status: Hardened
   - Infrastructure: 59 services, 29 databases, Kafka, Redis → Status: Production

3. **For each layer** (2-3 paragraphs): What it does, why it matters for customers, what was recently shipped.

4. **Integration Points**: How layers connect (event-driven via Kafka, FHIR-native data flow)

5. **What's Next**: Pilot deployments, additional EHR connectors, expanded measure library

**Source material:**
- `/mnt/wdblack/dev/projects/hdim-master/CLAUDE.md` (tech stack, capabilities)
- `/mnt/wdblack/dev/projects/hdim-master/docs/INVESTOR_PITCH_DECK_2026.md` (product slides)
- Commit history (Wave-1, measure builder, CMO onboarding details)

**Step 1: Write the file**
**Step 2: Commit**

```bash
git add platform/PLATFORM-OVERVIEW.md
git commit -m "docs: add full-stack platform overview"
```

---

### Task 7: Write platform/ARCHITECTURE.md

**Files:**
- Create: `platform/ARCHITECTURE.md`

**Content spec:**

Technical deep-dive for CTOs and architects. Condensed from current TECHNICAL-COMPETITIVE-ANALYSIS.md (884 lines → ~300 lines).

**Sections:**

1. **Architecture Overview**: Event sourcing + CQRS, why it matters for healthcare (auditability, temporal queries, HIPAA compliance)
2. **Event Sourcing Pattern**: Immutable event store via Kafka, event replay, projection materialization. Include the ASCII diagram from current doc.
3. **Database-per-Service**: 29 independent databases, schema isolation, Liquibase migrations. Why: tenant isolation at kernel level.
4. **Gateway Architecture**: 4 modularized gateways (API, Clinical, Admin, Internal), gateway-core shared module, header-based trust authentication.
5. **FHIR R4 Native**: Direct CQL execution on FHIR resources, no ETL translation loss, 100% attribute preservation.
6. **Multi-Tenant Isolation**: Database-level, cache-level (5-min TTL), query-level (tenantId filter on all queries).
7. **Replication Timeline** (table): Event sourcing 18-24mo, FHIR native 9-12mo, 80+ CQL measures 12-18mo, database-per-service 12mo.

**Source material:**
- Current `technical/TECHNICAL-COMPETITIVE-ANALYSIS.md` (condense Parts 1-3)

**Step 1: Write the file**
**Step 2: Commit**

```bash
git add platform/ARCHITECTURE.md
git commit -m "docs: add architecture deep-dive for technical DD"
```

---

### Task 8: Write platform/SECURITY-COMPLIANCE.md

**Files:**
- Create: `platform/SECURITY-COMPLIANCE.md`

**Content spec:**

New document showcasing the security hardening work. This differentiates HDIM from competitors who just say "HIPAA compliant."

**Sections:**

1. **Security Posture Summary** (table): HIPAA status, CVE status, security scanning, compliance gates, audit coverage
2. **HIPAA §164.312 Compliance**: Not retrofitted — engineered. Cache TTL ≤5 min, encryption in transit (TLS 1.3), session audit logging, PHI filtering on browser console, automatic logoff with audit trail.
3. **CVE Remediation**: Wave-based approach. NVD CVE closeout automation. Prioritized burn-down tracking. Evidence manifests.
4. **Security Scanning**: ZAP baseline workflows. Compliance evidence gate in CI/CD. Automated scanning on every PR.
5. **360 Platform Assurance**: Checklist + rubric + evidence index + sign-off process. Monthly compliance snapshots.
6. **Audit Trail**: 100% API call audit coverage via HTTP interceptor. Session timeout logging. 6-year retention capability.
7. **Multi-Tenant Security**: Database isolation, query-level tenantId enforcement, header-based tenant validation.

**Source material:**
- Main repo commits: CVE remediation, ZAP, 360 assurance, compliance cadence
- `/mnt/wdblack/dev/projects/hdim-master/CLAUDE.md` (HIPAA section)
- `/mnt/wdblack/dev/projects/hdim-master/backend/HIPAA-CACHE-COMPLIANCE.md`

**Step 1: Write the file**
**Step 2: Commit**

```bash
git add platform/SECURITY-COMPLIANCE.md
git commit -m "docs: add security and compliance posture document"
```

---

### Task 9: Write traction/DEVELOPMENT-VELOCITY.md

**Files:**
- Create: `traction/DEVELOPMENT-VELOCITY.md`

**Content spec:**

Shows execution pace. This is the "we ship fast" proof.

**Sections:**

1. **Velocity Summary**: 444 commits in February 2026. 7 infrastructure phases complete. Solo technical execution ready to scale.
2. **Monthly Commit History** (table): Show commit counts by month for last 6 months if available.
3. **Infrastructure Phases** (timeline): Phase 1-7 with dates and achievements. Highlight cumulative improvement (90%+ faster feedback loops).
4. **Test Suite Growth**: 600 → 1,171 test classes. 8,000+ test methods. Zero regressions.
5. **CI/CD Performance**: 42.5% faster PR feedback (40m → 23-25m). 4 parallel test jobs. 21-path change detection.
6. **What This Means for Customers**: Fast iteration = fast feature delivery. Enterprise-grade CI/CD = reliable releases.

**Source material:**
- `git log --oneline --since="2026-02-03" | wc -l` (commit count)
- `/mnt/wdblack/dev/projects/hdim-master/CLAUDE.md` (Phase 6-7 details)

**Step 1: Write the file**
**Step 2: Commit**

```bash
git add traction/DEVELOPMENT-VELOCITY.md
git commit -m "docs: add development velocity and execution proof"
```

---

### Task 10: Write traction/PRODUCT-MILESTONES.md

**Files:**
- Create: `traction/PRODUCT-MILESTONES.md`

**Content spec:**

Timeline of what shipped and when. Proves continuous delivery.

**Sections:**

1. **Milestone Timeline** (table): Date → Milestone → Impact
   - Q3-Q4 2025: Platform architecture (51 services, 29 databases)
   - Jan 2026: API documentation (62 endpoints, OpenAPI 3.0)
   - Jan 2026: HIPAA audit infrastructure (100% API audit coverage)
   - Jan 2026: Gateway modularization (4 gateways, shared core)
   - Feb 2026: CI/CD optimization Phase 7 (42.5% faster)
   - Feb 2026: Wave-1 Revenue Cycle (claims, remittance, price transparency)
   - Feb 2026: Custom Measure Builder (create measure UI, metadata dialog)
   - Feb 2026: CMO Onboarding (dashboard, acceptance playbooks)
   - Feb 2026: Operations Orchestration (16-class gateway framework)
   - Feb 2026: Security Hardening (CVE remediation, ZAP, 360 assurance)
   - Feb 2026: Quality Measure expansion (52 → 80+ HEDIS measures, 7 custom metadata columns)

2. **Recent Highlights** (expanded descriptions of Feb 2026 work)

3. **Upcoming** (Q2 2026): Pilot deployments, EHR integrations, expanded measure library

**Source material:**
- Main repo commit history
- `/mnt/wdblack/dev/projects/hdim-master/CLAUDE.md` (build notes section)

**Step 1: Write the file**
**Step 2: Commit**

```bash
git add traction/PRODUCT-MILESTONES.md
git commit -m "docs: add product milestone timeline"
```

---

### Task 11: Write traction/PRODUCTION-READINESS.md

**Files:**
- Create: `traction/PRODUCTION-READINESS.md`

**Content spec:**

Updated production readiness checklist. Based on current PRODUCTION-READINESS-CHECKLIST.md but expanded with new capabilities.

**Sections:**

1. **Overall Status** (table): Infrastructure ✅, Security ✅, Testing ✅, Documentation ✅, Performance ✅, Monitoring ✅, Revenue Cycle ✅ (new), Compliance Gates ✅ (new)

2. **Infrastructure** (checklist): 59 services compile, Docker builds, health checks, Liquibase migrations, Kafka, event processing. All checked.

3. **Security** (checklist): HIPAA controls, CVE remediation complete, ZAP scans passing, 360 assurance signed off, compliance evidence gates in CI/CD.

4. **Testing** (checklist): 1,171 test classes, 8,000+ methods, 6 test modes (testUnit through testAll), zero regressions, entity-migration validation.

5. **Revenue Cycle** (checklist — NEW): Wave-1 validation gates passing, p95 performance budgets enforced, claims processing tested, remittance reconciliation validated, ADT payload handling verified.

6. **Frontend** (checklist): Angular 17 builds, no console.log (HIPAA), session timeout, global error handler, accessibility baseline.

7. **Deployment** (checklist): Docker Compose, Kubernetes-ready, monitoring dashboards, alerting configured.

**Source material:**
- Current `technical/PRODUCTION-READINESS-CHECKLIST.md`
- Main repo Wave-1 commits (validation gates, performance budgets)

**Step 1: Write the file**
**Step 2: Commit**

```bash
git add traction/PRODUCTION-READINESS.md
git commit -m "docs: add updated production readiness checklist"
```

---

### Task 12: Write technical/CODE-SAMPLES.md

**Files:**
- Create: `technical/CODE-SAMPLES.md`

**Content spec:**

Updated code samples. Keep best examples from current version, add new patterns.

**Patterns to include:**

1. **Event Sourcing** (keep from current): CareGapEventApplicationService
2. **CQRS Projection** (keep from current): Event → materialized view
3. **Multi-Tenant Isolation** (keep from current): tenantId filtering
4. **HIPAA Audit Logging** (keep from current): @Audited annotation
5. **FHIR R4 Native** (keep from current): FHIR resource handling
6. **Operations Orchestration** (NEW): Gateway operations framework pattern
7. **Custom Measure Creation** (NEW): Quality measure metadata columns, PatientHealth enhancement
8. **Revenue Cycle** (NEW): Price transparency API, clearinghouse retry with backoff

Each sample: Why It Matters (2 sentences) → Code (20-40 lines) → What This Proves (1 sentence)

**Source material:**
- Current `technical/INVESTOR-CODE-SAMPLES.md`
- Main repo: `backend/modules/services/` (actual source code for new patterns)

**Step 1: Write the file**
**Step 2: Commit**

```bash
git add technical/CODE-SAMPLES.md
git commit -m "docs: add updated code samples with Wave-1 and operations patterns"
```

---

### Task 13: Write technical/COMPETITIVE-ANALYSIS.md

**Files:**
- Create: `technical/COMPETITIVE-ANALYSIS.md`

**Content spec:**

Refreshed competitive positioning. Condensed from current 884-line version to ~400 lines. Key updates:

1. **Head-to-Head Table**: Add revenue cycle row (HDIM: Wave-1 shipped, competitors: not available or separate product)
2. **Capability Breadth**: HDIM now spans quality measurement + care gap detection + revenue cycle + custom measure creation. Competitors are point solutions.
3. **Moat Timeline** (updated): Add revenue cycle and custom measure builder to replication timeline
4. **Security Comparison**: HDIM has CVE remediation evidence, ZAP scanning, 360 assurance. Competitors claim "HIPAA compliant" without evidence.
5. **Deployment Speed**: Keep 90-day vs. 18-24 month comparison

**Source material:**
- Current `technical/TECHNICAL-COMPETITIVE-ANALYSIS.md` (condense)

**Step 1: Write the file**
**Step 2: Commit**

```bash
git add technical/COMPETITIVE-ANALYSIS.md
git commit -m "docs: add refreshed competitive analysis with expanded capabilities"
```

---

### Task 14: Write technical/DEPLOYMENT-GUIDE.md

**Files:**
- Create: `technical/DEPLOYMENT-GUIDE.md`

**Content spec:**

Refreshed deployment guide. Keep core structure from current HOSPITAL-DEPLOYMENT-GUIDE.md, update with:

1. Updated service count (59 services)
2. Wave-1 revenue cycle deployment steps
3. Security hardening verification (ZAP scan, CVE check)
4. Performance budget validation (p95 enforcement)
5. CMO onboarding dashboard setup

**Source material:**
- Current `deployment/HOSPITAL-DEPLOYMENT-GUIDE.md`

**Step 1: Write the file**
**Step 2: Commit**

```bash
git add technical/DEPLOYMENT-GUIDE.md
git commit -m "docs: add updated deployment guide with Wave-1 and security verification"
```

---

### Task 15: Update .gitignore and final commit

**Files:**
- Verify: `.gitignore` (should be fine)
- Verify: `LICENSE` (keep as-is)

**Step 1: Verify all 13 files exist**

```bash
cd /mnt/wdblack/dev/projects/hdim-investor
find . -name "*.md" -not -path "./.git/*" | sort
```

Expected output:
```
./README.md
./executive/FAQ.md
./executive/ONE-PAGER.md
./executive/PITCH-DECK.md
./platform/ARCHITECTURE.md
./platform/PLATFORM-OVERVIEW.md
./platform/SECURITY-COMPLIANCE.md
./technical/CODE-SAMPLES.md
./technical/COMPETITIVE-ANALYSIS.md
./technical/DEPLOYMENT-GUIDE.md
./traction/DEVELOPMENT-VELOCITY.md
./traction/PRODUCT-MILESTONES.md
./traction/PRODUCTION-READINESS.md
```

**Step 2: Verify all internal links resolve**

Check every `[text](path)` link in every file points to a file that exists.

**Step 3: Push to GitHub**

```bash
git push origin main
```
