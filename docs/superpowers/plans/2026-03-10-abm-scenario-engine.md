# ABM Scenario Engine Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a research-driven ABM system with 5 demo archetypes, 10-15 scored target dossiers, and event-triggered outreach templates — all operable by one person.

**Architecture:** Markdown-based content system in `docs/abm/` with structured templates, scoring rubric, and outreach playbooks. Demo seed data as JSON profiles in `backend/modules/shared/demo-seed/`. No application code — this is a content and process system.

**Tech Stack:** Markdown, JSON (seed profiles), existing Docker Compose + demo-seed infrastructure.

**Spec:** `docs/superpowers/specs/2026-03-10-abm-scenario-engine-design.md`

**Existing Assets to Incorporate:**
- `docs/gtm/TARGET_SMB_ORGANIZATIONS.md` — 10 SMB targets with research
- `docs/investor/customer-target-list.md` — 50 tiered targets
- `docs/gtm/OUTREACH_TARGETS_DETAILED.md` — Ranked SMB targets
- `docs/gtm/SEGMENT_MESSAGING_V2.md` — Buyer personas and messaging
- `docs/sales/02-segments-and-usecases/segments/` — 6 segment sales kits

---

## Chunk 1: Foundation (Directory Structure, Scoring, Templates)

### Task 1: Create Directory Structure and README

**Files:**
- Create: `docs/abm/README.md`
- Create: `docs/abm/scoring-criteria.md`
- Create: `docs/abm/targets/_tracker.md`
- Create: `docs/abm/targets/_template.md`
- Create: `docs/abm/monitoring/weekly-scan-checklist.md`
- Create: `docs/abm/monitoring/trigger-log.md`

- [ ] **Step 1: Create directory structure**

```bash
mkdir -p docs/abm/{archetypes,targets,templates/email-sequences,templates/linkedin-comment-banks,templates/loom-scripts,monitoring}
```

- [ ] **Step 2: Write README.md**

Create `docs/abm/README.md` with:
- System overview (what this is, who operates it)
- Quick start: how to add a new target (copy template, research, score, assign archetype)
- Weekly/monthly cadence summary
- File layout reference
- Links to scoring criteria, tracker, archetypes

- [ ] **Step 3: Write scoring-criteria.md**

Create `docs/abm/scoring-criteria.md` with the 5-dimension rubric from the spec:
- VBC Exposure (1/3/5)
- Quality Pain Signals (1/3/5)
- Technology Readiness (1/3/5)
- Decision Velocity (1/3/5) — use corrected ranges: >$500M=1, $100-500M=3, <$100M=5
- Trigger Recency (1/3/5)
- Qualification threshold: 18+ out of 25
- Examples of scoring for each dimension

- [ ] **Step 4: Write target dossier template**

Create `docs/abm/targets/_template.md` with all sections from the spec:
- Score table (5 dimensions)
- Organization Profile (type, size, geography, EHR, payer mix, revenue)
- Decision Makers (name, title, LinkedIn, recent activity)
- VBC Contracts
- Quality Performance
- Recent Intelligence (date, source, summary, URL)
- Pain Hypothesis
- Trigger Events (date, type, description, urgency)
- Archetype Match (primary + overlay notes)
- Outreach Status (T1/T2/T3 tracking)
- Next Action

- [ ] **Step 5: Write tracker table**

Create `docs/abm/targets/_tracker.md` with:
- Master table header matching spec schema
- Status values legend: `new | triggered | in-sequence | responded | demo-booked | dormant`
- Empty rows ready for targets

- [ ] **Step 6: Write monitoring files**

Create `docs/abm/monitoring/weekly-scan-checklist.md`:
- Google Alerts check
- CMS.gov releases
- LinkedIn decision-maker activity
- Google News search patterns
- NCQA/HEDIS announcements
- Time estimate: 15 minutes

Create `docs/abm/monitoring/trigger-log.md`:
- Table: Date | Target | Trigger Type | Description | Action Taken | Outreach Initiated
- Empty, ready for entries

- [ ] **Step 7: Commit foundation**

```bash
git add docs/abm/
git commit -m "feat(abm): foundation — directory structure, scoring rubric, templates, monitoring"
```

---

### Task 2: Write 5 Archetype Files

**Files:**
- Create: `docs/abm/archetypes/01-fqhc-closing-the-gap.md`
- Create: `docs/abm/archetypes/02-ma-plan-chasing-4-stars.md`
- Create: `docs/abm/archetypes/03-aco-under-the-gun.md`
- Create: `docs/abm/archetypes/04-health-system-integrating.md`
- Create: `docs/abm/archetypes/05-specialty-network-value.md`

Each archetype file follows this structure:
```markdown
# Archetype N: [Name]

## Target Persona
- Title, org type, what keeps them up at night

## Population Profile
- Size range, payer mix, chronic condition prevalence, measure focus

## Demo Seed Parameters
- Patient count, payer distribution percentages
- Key measures with realistic gap rates
- Provider roster (PCP/specialist ratio)
- Pre-seeded "wins" for the Monday morning story

## Monday Morning Scenario Script
- Full walkthrough narrative (what they see when they log in)
- Key talking points at each screen
- "Aha moment" — where HDIM's value becomes undeniable
- Objection pre-emption built into the narrative

## Pain Hooks (by trigger type)
- Star Rating Drop: [specific hook]
- Quality Report Published: [specific hook]
- Leadership Change: [specific hook]
- VBC Contract: [specific hook]
- M&A: [specific hook]

## Overlay Fields
- What changes per target (org name, volume, geography, measures, opening narrative)

## Competitive Positioning
- What they're likely using today (Azara DRVS, Arcadia, manual Excel)
- Why HDIM is different for this archetype
```

- [ ] **Step 1: Write Archetype 01 — FQHC Closing the Gap**

Create `docs/abm/archetypes/01-fqhc-closing-the-gap.md`.

Persona: Quality Director at FQHC/PCA. Population: 15K-40K, Medicaid-heavy.
Key measures: HbA1c, CBP, CCS, DSF. Reference existing content from `docs/sales/02-segments-and-usecases/segments/small-practices-sales-kit.md` and `docs/gtm/SEGMENT_MESSAGING_V2.md` FQHC persona.

Include full Monday morning scenario script with specific numbers:
- 3 care gaps closed over weekend
- Diabetes control 52% → 58%
- 847 patients overdue for HbA1c
- Outreach list prioritized by risk score

Pain hooks for each of the 6 trigger types.
Competitive positioning vs Azara DRVS (most common in FQHC space).

- [ ] **Step 2: Write Archetype 02 — MA Plan Chasing 4 Stars**

Create `docs/abm/archetypes/02-ma-plan-chasing-4-stars.md`.

Persona: VP Quality at MA plan. Population: 50K-200K lives.
Key measures: Stars-weighted (medication adherence, readmissions, HEDIS).
Reference: `docs/sales/02-segments-and-usecases/segments/risk-based-organizations-sales-kit.md`, verified Stars data from memory (40% of Part C weight, 2025 nadir).

Monday morning scenario: 3.5 stars, 4.0 threshold = $12M bonus. 6 measures within 2 percentage points. 3,200 members ranked by closability.

- [ ] **Step 3: Write Archetype 03 — ACO Under the Gun**

Create `docs/abm/archetypes/03-aco-under-the-gun.md`.

Persona: Quality Director at MSSP/ACO REACH. Population: 30K-75K attributed.
Key measures: ACO quality gates (preventive, chronic, safety).
Reference: `docs/sales/02-segments-and-usecases/segments/accountable-care-organizations-sales-kit.md`, APP Plus confirmed data (6→8→9→11 measures by 2028).

Monday morning scenario: 8/10 passing, 2 failing, 60 days to deadline. 1,400 flu shots + 600 colorectal. Provider-level breakdown.

- [ ] **Step 4: Write Archetype 04 — Health System Integrating Acquisitions**

Create `docs/abm/archetypes/04-health-system-integrating.md`.

Persona: CMO/CIO at multi-hospital system post-M&A. Population: 100K-450K across multiple EHRs.
Key measures: System-wide benchmarking, MIPS, payer quality tiers.
Reference: `docs/sales/02-segments-and-usecases/segments/healthcare-systems-sales-kit.md`.

Monday morning scenario: 3 acquired practices (Meditech, eCW, athena) + Epic. First unified dashboard. Facility B 15 points below average on diabetes.

- [ ] **Step 5: Write Archetype 05 — Specialty Network Proving Value**

Create `docs/abm/archetypes/05-specialty-network-value.md`.

Persona: Network Director at specialty care network. Population: 5K-30K.
Key measures: Specialty-specific, referral loops, outcomes.
Reference: `docs/sales/02-segments-and-usecases/segments/specialty-care-sales-kit.md`.

Monday morning scenario: 40 payer contracts normalized. 5 common measures across 80% of contracts. 312 open referral loops.

- [ ] **Step 6: Commit archetypes**

```bash
git add docs/abm/archetypes/
git commit -m "feat(abm): 5 demo scenario archetypes with walkthrough scripts"
```

---

## Chunk 2: Outreach Templates

### Task 3: Write Email Sequence Templates (Top 3 Trigger Types)

**Files:**
- Create: `docs/abm/templates/email-sequences/star-rating-drop.md`
- Create: `docs/abm/templates/email-sequences/quality-report-published.md`
- Create: `docs/abm/templates/email-sequences/leadership-change.md`

Each email sequence file contains 3 emails (Touch 1, 2, 3) with merge fields:
- `[ORG_NAME]` — organization name
- `[DECISION_MAKER_FIRST]` — first name
- `[TRIGGER_DETAIL]` — specific trigger event
- `[PAIN_HYPOTHESIS]` — from dossier
- `[ARCHETYPE_STAT]` — relevant data point from archetype
- `[SENDER_NAME]` — Aaron

Priority: Start with the 3 highest-urgency trigger types. Build the remaining 3 (vbc-contract, merger-expansion, conference-presentation) when those triggers appear.

- [ ] **Step 1: Write star-rating-drop.md**

3-email sequence:
- T1 (Day 0): Reference specific rating change, empathize with bonus revenue impact, offer 15-min walkthrough
- T2 (Day 5-7): Data point — "Plans your size typically find X gaps recoverable in 90 days"
- T3 (Day 14-18): Loom video offer, final CTA

Tone: Consultative, not salesy. Lead with their problem, not HDIM features.

- [ ] **Step 2: Write quality-report-published.md**

3-email sequence:
- T1: Reference specific published metric, connect to HDIM capability
- T2: Case study data point from matching archetype
- T3: Demo offer with Loom

- [ ] **Step 3: Write leadership-change.md**

3-email sequence:
- T1: Congratulate new role, reference org's quality priorities
- T2: "New leaders often audit current tooling — here's what we see"
- T3: Demo offer

- [ ] **Step 4: Commit email templates**

```bash
git add docs/abm/templates/email-sequences/
git commit -m "feat(abm): email sequences for top 3 trigger types"
```

---

### Task 4: Write LinkedIn Comment Banks

**Files:**
- Create: `docs/abm/templates/linkedin-comment-banks/fqhc-comments.md`
- Create: `docs/abm/templates/linkedin-comment-banks/ma-plan-comments.md`
- Create: `docs/abm/templates/linkedin-comment-banks/aco-comments.md`
- Create: `docs/abm/templates/linkedin-comment-banks/health-system-comments.md`
- Create: `docs/abm/templates/linkedin-comment-banks/specialty-network-comments.md`

Each file: 5-6 thoughtful comments for engaging with target content on LinkedIn. Not promotional — demonstrate domain expertise and genuine interest.

Reference: `docs/outreach/2026-03-03-tier2-comment-bank.md` (existing 42 comments across 18 people) for tone and style.

- [ ] **Step 1: Write all 5 comment bank files**

Each file should include:
- Context line: "Use when [archetype persona] posts about [topic]"
- 5-6 comments, each 2-4 sentences
- Mix of: agreeing + adding insight, asking a smart question, sharing a relevant data point
- Never mention HDIM directly in comments — establish credibility first

- [ ] **Step 2: Commit comment banks**

```bash
git add docs/abm/templates/linkedin-comment-banks/
git commit -m "feat(abm): LinkedIn comment banks for 5 archetypes"
```

---

### Task 5: Write Loom Scripts

**Files:**
- Create: `docs/abm/templates/loom-scripts/fqhc-walkthrough.md`
- Create: `docs/abm/templates/loom-scripts/ma-plan-walkthrough.md`
- Create: `docs/abm/templates/loom-scripts/aco-walkthrough.md`
- Create: `docs/abm/templates/loom-scripts/health-system-walkthrough.md`
- Create: `docs/abm/templates/loom-scripts/specialty-network-walkthrough.md`

Each script: 60-90 second narration for a screen recording of the HDIM demo with that archetype's seed data.

- [ ] **Step 1: Write all 5 Loom scripts**

Each script should include:
- Opening hook (5 seconds): "If you're a [persona] at a [org type], here's what Monday morning looks like with HDIM."
- Screen narration (45-60 seconds): Walk through the Monday morning scenario from the archetype, calling out specific screens and data points
- Closing CTA (10-15 seconds): "Want to see this with your org's measures? I can set up a 15-minute walkthrough."
- [MERGE FIELDS] for org-specific customization

- [ ] **Step 2: Commit Loom scripts**

```bash
git add docs/abm/templates/loom-scripts/
git commit -m "feat(abm): Loom walkthrough scripts for 5 archetypes"
```

---

## Chunk 3: Target Research & Dossiers

### Task 6: Score and Select 10-15 Targets

**Files:**
- Modify: `docs/abm/targets/_tracker.md`

This task requires **web research** for each target. The implementer must:

1. Review existing target lists:
   - `docs/gtm/TARGET_SMB_ORGANIZATIONS.md` (10 targets with research)
   - `docs/investor/customer-target-list.md` (50 targets, 3 tiers)
   - `docs/gtm/OUTREACH_TARGETS_DETAILED.md` (ranked SMB targets)

2. Score each candidate against the rubric in `docs/abm/scoring-criteria.md`

3. Select the top 10-15 that score 18+, ensuring coverage across archetypes:
   - At least 2-3 FQHC/PCA targets (Archetype 1)
   - At least 2-3 MA plan targets (Archetype 2)
   - At least 2-3 ACO targets (Archetype 3)
   - At least 1-2 health system targets (Archetype 4)
   - At least 1-2 specialty network targets (Archetype 5)

- [ ] **Step 1: Score existing SMB targets from TARGET_SMB_ORGANIZATIONS.md**

The 10 existing SMB targets to score:
1. Greater Regional Health ACO (Iowa) — likely Archetype 3
2. IowaHealth+ ACO Network — likely Archetype 3
3. Missouri PCA — likely Archetype 1
4. WVPCA — likely Archetype 1
5. IPHCA — likely Archetype 1
6. Regional MA Plan (generic) — likely Archetype 2
7. BC Vermont — likely Archetype 2
8. MA League CHC — likely Archetype 1
9. OACHC — likely Archetype 1
10. Regional IPA (generic) — likely Archetype 5

For each: assign 1/3/5 per dimension, total score, pass/fail against 18 threshold.

- [ ] **Step 2: Score top candidates from customer-target-list.md**

Review Tier 1 and Tier 2 targets (35 total). Score the most promising against rubric. Focus on filling archetype gaps from Step 1 (especially Archetype 2 MA plans, Archetype 4 health systems, Archetype 5 specialty networks).

- [ ] **Step 3: Select final 10-15 and update tracker**

Update `docs/abm/targets/_tracker.md` with selected targets, their archetype assignment, score, and status = `new`.

- [ ] **Step 4: Commit selection**

```bash
git add docs/abm/targets/_tracker.md
git commit -m "feat(abm): initial target selection — N targets scored and mapped"
```

---

### Task 7: Research and Write Target Dossiers (Batch 1: Targets 1-5)

**Files:**
- Create: `docs/abm/targets/target-01-[org-name].md`
- Create: `docs/abm/targets/target-02-[org-name].md`
- Create: `docs/abm/targets/target-03-[org-name].md`
- Create: `docs/abm/targets/target-04-[org-name].md`
- Create: `docs/abm/targets/target-05-[org-name].md`

Each dossier requires 30-60 minutes of web research per target.

- [ ] **Step 1: Research and write dossier for Target 1**

Copy `docs/abm/targets/_template.md` to `target-01-[org-name].md`.

Research sources:
- Organization website (about, leadership, press releases)
- CMS.gov (Star ratings if MA, ACO performance if MSSP)
- LinkedIn (decision-maker profiles, recent posts, org page)
- Google News (org name + "quality" OR "acquisition" OR "value-based")
- State health department (if applicable)

Fill all sections. Write pain hypothesis based on findings. Identify trigger events. Assign archetype with overlay notes.

- [ ] **Step 2: Research and write dossier for Target 2**

Same process as Step 1.

- [ ] **Step 3: Research and write dossier for Target 3**

Same process as Step 1.

- [ ] **Step 4: Research and write dossier for Target 4**

Same process as Step 1.

- [ ] **Step 5: Research and write dossier for Target 5**

Same process as Step 1.

- [ ] **Step 6: Commit batch 1 dossiers**

```bash
git add docs/abm/targets/target-0[1-5]-*.md
git commit -m "feat(abm): target dossiers batch 1 (targets 1-5)"
```

---

### Task 8: Research and Write Target Dossiers (Batch 2: Targets 6-10)

**Files:**
- Create: `docs/abm/targets/target-06-[org-name].md` through `target-10-[org-name].md`

- [ ] **Steps 1-5: Research and write dossiers for Targets 6-10**

Same process as Task 7 for each target.

- [ ] **Step 6: Commit batch 2 dossiers**

```bash
git add docs/abm/targets/target-0[6-9]-*.md docs/abm/targets/target-10-*.md
git commit -m "feat(abm): target dossiers batch 2 (targets 6-10)"
```

---

### Task 9: Research and Write Target Dossiers (Batch 3: Targets 11-15, if applicable)

**Files:**
- Create: `docs/abm/targets/target-11-[org-name].md` through `target-15-[org-name].md`

- [ ] **Steps 1-5: Research and write dossiers for Targets 11-15**

Same process. Skip if only 10 targets selected.

- [ ] **Step 6: Commit batch 3 dossiers**

```bash
git add docs/abm/targets/target-1[1-5]-*.md
git commit -m "feat(abm): target dossiers batch 3 (targets 11-15)"
```

---

## Chunk 4: Demo Seed Data

### Task 10: Create Archetype Seed Profiles

**Files:**
- Create: `docs/abm/seed-profiles/fqhc-profile.json`
- Create: `docs/abm/seed-profiles/ma-plan-profile.json`
- Create: `docs/abm/seed-profiles/aco-profile.json`
- Create: `docs/abm/seed-profiles/health-system-profile.json`
- Create: `docs/abm/seed-profiles/specialty-network-profile.json`
- Create: `docs/abm/seed-profiles/_overlay-template.json`

**Note:** These are configuration profiles that describe the demo data to generate. Integration with the actual `hdim_demo_seed` MCP tool is a separate follow-up — these profiles define WHAT to seed, the existing tooling handles HOW.

- [ ] **Step 1: Define seed profile JSON schema**

```json
{
  "archetype": "fqhc-closing-the-gap",
  "displayName": "Community Health Center",
  "population": {
    "totalPatients": 20000,
    "payerMix": {
      "medicaid": 0.65,
      "medicare": 0.20,
      "commercial": 0.10,
      "uninsured": 0.05
    },
    "ageDistribution": {
      "0-17": 0.25,
      "18-44": 0.30,
      "45-64": 0.30,
      "65+": 0.15
    },
    "chronicConditionPrevalence": {
      "diabetes": 0.18,
      "hypertension": 0.32,
      "depression": 0.22,
      "obesity": 0.35
    }
  },
  "measures": [
    {
      "id": "HBA1C",
      "name": "Diabetes: HbA1c Control",
      "currentRate": 0.52,
      "targetRate": 0.65,
      "openGaps": 847,
      "recentClosures": 3
    }
  ],
  "providers": {
    "pcpCount": 25,
    "specialistCount": 8,
    "careCoordinatorCount": 6
  },
  "mondayMorningHighlights": {
    "gapsClosedThisWeek": 3,
    "rateImprovement": "52% → 58%",
    "topOutreachList": 847,
    "urgentAlerts": 2
  }
}
```

- [ ] **Step 2: Create all 5 archetype profiles**

Write each JSON file using the schema from Step 1, populated with the specific numbers from the archetype files in `docs/abm/archetypes/`.

- [ ] **Step 3: Create overlay template**

```json
{
  "targetOverlay": {
    "organizationName": "[ORG_NAME]",
    "geography": "[STATE, CITY]",
    "patientVolumeMultiplier": 1.0,
    "priorityMeasures": ["HBA1C", "CBP"],
    "localContext": "[Medicaid program name, local references]",
    "openingNarrative": "[From dossier pain hypothesis + trigger]"
  }
}
```

- [ ] **Step 4: Commit seed profiles**

```bash
git add docs/abm/seed-profiles/
git commit -m "feat(abm): demo seed profiles for 5 archetypes + overlay template"
```

---

## Chunk 5: Activation

### Task 11: Set Up Monitoring and Activate

- [ ] **Step 1: Set up Google Alerts**

For each selected target, create a Google Alert:
- Query: `"[Organization Name]" AND (quality OR HEDIS OR "value-based" OR acquisition OR "star rating")`
- Frequency: Weekly digest
- Sources: News, Web

Document the alerts created in `docs/abm/monitoring/trigger-log.md` header.

- [ ] **Step 2: Initial trigger scan**

For all 10-15 targets, do an initial scan of the trigger sources listed in `docs/abm/monitoring/weekly-scan-checklist.md`. Log any existing triggers in `trigger-log.md`.

- [ ] **Step 3: Initiate outreach on any triggered targets**

If any targets already have active triggers:
1. Pull matching email sequence from `docs/abm/templates/email-sequences/`
2. Fill merge fields from the target's dossier
3. Execute Touch 1 (LinkedIn engage + email)
4. Update `_tracker.md` status to `triggered` → `in-sequence`

- [ ] **Step 4: Final commit**

```bash
git add docs/abm/monitoring/ docs/abm/targets/_tracker.md
git commit -m "feat(abm): monitoring activated, initial trigger scan complete"
```

---

## Summary

| Chunk | Tasks | Commits | Estimated Time |
|-------|-------|---------|---------------|
| 1: Foundation | 1-2 | 2 | 2-3 hours |
| 2: Outreach Templates | 3-5 | 3 | 2-3 hours |
| 3: Target Research | 6-9 | 4 | 8-12 hours (research-heavy) |
| 4: Demo Seed Data | 10 | 1 | 1-2 hours |
| 5: Activation | 11 | 1 | 1-2 hours |
| **Total** | **11** | **11** | **14-22 hours** |

**Critical path:** Chunks 1-2 can be built without research. Chunk 3 is the bottleneck (requires web research per target). Chunk 4 depends on archetypes (Chunk 1). Chunk 5 depends on everything.

**Parallelization:** Tasks 3-5 (outreach templates) are independent of Task 2 (archetypes) and can be worked in parallel. Tasks 7-9 (dossier batches) are independent of each other.
