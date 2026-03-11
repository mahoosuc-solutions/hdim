# ABM Scenario Engine — Design Spec

**Date:** 2026-03-10
**Status:** Approved
**Author:** Aaron + Claude

## Overview

Account-based marketing engine for HDIM that combines research-driven prospect intelligence, archetype-based demo customization, and event-triggered multi-channel outreach. Designed for one-person operation targeting 10-15 healthcare organizations.

## Goals

1. Build research dossiers on 10-15 target organizations with scoring and trigger monitoring
2. Define 5 demo scenario archetypes with reusable seed data and scripted walkthroughs
3. Map each target to an archetype + org-specific overlays (name, volume, measures, geography)
4. Execute event-triggered outreach sequences (email + LinkedIn + Loom) when trigger events fire
5. Maintain a repeatable monitoring cadence to refresh intelligence and rotate targets

## Non-Goals

- Automated research scraping or tooling (manual, template-driven)
- CRM system or database (markdown files and tables)
- Mass outreach (this is 10-15 targets, personalized)

---

## 1. Target Selection & Research Dossier

### Scoring Criteria (18+ out of 25 to qualify)

| Dimension | 1 (Low) | 3 (Medium) | 5 (High) |
|-----------|---------|------------|----------|
| **VBC Exposure** | Fee-for-service only | 1-2 VBC contracts | Active MSSP/ACO REACH/MA Stars |
| **Quality Pain Signals** | No public data | Some quality reporting | Published gaps, Star drops, HEDIS shortfalls |
| **Technology Readiness** | Proprietary/legacy EHR | Partial FHIR capability | Epic/Cerner/Meditech with FHIR R4 |
| **Decision Velocity** | >$500M or 12+ month procurement | $100M-$500M | <$100M, identifiable buyer, <6 month cycle |
| **Trigger Recency** | No recent activity | 90+ day old news | Trigger within 30 days |

### Research Dossier Template

Each target gets one markdown file with these sections:

```markdown
# [Organization Name]

## Score: [X]/25
| VBC Exposure | Quality Pain | Tech Readiness | Decision Velocity | Trigger Recency |
|---|---|---|---|---|
| X | X | X | X | X |

## Organization Profile
- **Type:** [FQHC / MA Plan / ACO / Health System / Specialty Network]
- **Size:** [X patients/lives]
- **Geography:** [State, metro area]
- **EHR:** [Epic / Cerner / Meditech / Other]
- **Payer Mix:** [Medicaid X% / Medicare X% / Commercial X%]
- **Revenue:** [Estimated range]

## Decision Makers
| Name | Title | LinkedIn | Recent Activity |
|------|-------|----------|-----------------|
| | | | |

## VBC Contracts
- [List known programs, attribution size, contract dates]

## Quality Performance
- [Published Star ratings, HEDIS rates, CMS public data, accreditation status]

## Recent Intelligence
| Date | Source | Summary | URL |
|------|--------|---------|-----|
| | | | |

## Pain Hypothesis
[Based on research: what specific problem does HDIM solve for them?]

## Trigger Events
| Date | Trigger Type | Description | Urgency |
|------|-------------|-------------|---------|
| | | | |

## Archetype Match
- **Primary:** [Archetype name]
- **Overlay Notes:** [Org-specific customizations]

## Outreach Status
| Touch | Date | Channel | Content | Response |
|-------|------|---------|---------|----------|
| T1 | | | | |
| T2 | | | | |
| T3 | | | | |

## Next Action
[What to do next and when]
```

---

## 2. Demo Scenario Archetypes

Five archetypes covering the target universe. Each archetype is 80% reusable, 20% customized per target via overlays.

### Archetype 1: "FQHC Closing the Gap"

- **Persona:** Quality Director at a community health center or PCA
- **Population:** 15K-40K patients, Medicaid-heavy, high chronic disease burden
- **Key Measures:** Diabetes (HbA1c), hypertension (CBP), cervical cancer screening (CCS), depression screening (DSF)
- **Monday Morning Scenario:** "3 care gaps closed over the weekend from weekend clinic visits. Your HEDIS dashboard shows diabetes control improved from 52% to 58% this quarter. But 847 patients are overdue for HbA1c — here's the outreach list prioritized by risk."
- **Pain Hook:** Manual chart abstraction, no real-time gap visibility, measure reporting takes weeks
- **Seed Profile:** 20K patients, 65% Medicaid, diabetes prevalence 18%, hypertension 32%, 40% open care gaps

### Archetype 2: "MA Plan Chasing 4 Stars"

- **Persona:** VP of Quality at a Medicare Advantage plan
- **Population:** 50K-200K attributed lives, mixed risk
- **Key Measures:** Stars-weighted (medication adherence, readmissions, care gap closure rates)
- **Monday Morning Scenario:** "You're at 3.5 stars. Bonus threshold is 4.0 — that's $12M in quality bonus revenue at stake. HDIM shows the 6 measures where you're within 2 percentage points of the threshold. Here are the 3,200 members who could move the needle, ranked by closability."
- **Pain Hook:** Fragmented data across delegated providers, no line-of-sight to which members move which measures
- **Seed Profile:** 100K lives, Stars measures weighted, 6 measures near threshold, member-level closability scores

### Archetype 3: "ACO Under the Gun"

- **Persona:** Quality Director or Population Health lead at an MSSP/ACO REACH entity
- **Population:** 30K-75K attributed lives
- **Key Measures:** ACO quality gate measures (preventive screening, chronic disease management, patient safety)
- **Monday Morning Scenario:** "Shared savings are contingent on hitting quality gates. You're 60 days from reporting deadline. HDIM shows you're passing 8/10 measures but failing 2. Here's the gap — 1,400 patients need flu shots and 600 need colorectal screening. Here's the provider-level breakdown so your care coordinators know who to call."
- **Pain Hook:** Attribution lag, no real-time quality gate tracking, care coordinators working off stale lists
- **Seed Profile:** 50K attributed lives, 10 quality gate measures, 2 failing, provider-level attribution

### Archetype 4: "Health System Integrating Acquisitions"

- **Persona:** CMO or CIO at a multi-hospital system that recently acquired practices/facilities
- **Population:** 100K-450K patients across multiple EHR instances
- **Key Measures:** System-wide quality benchmarking, MIPS reporting, payer contract quality tiers
- **Monday Morning Scenario:** "You acquired 3 practices last year running Meditech, eCW, and athena. Your Epic instance doesn't see their patients. HDIM aggregates across all four systems — here's your first unified quality dashboard. Facility B is 15 points below system average on diabetes control. Here's why."
- **Pain Hook:** Multi-EHR data silos post-M&A, no unified quality view, manual reconciliation
- **Seed Profile:** 200K patients, 4 source systems, facility-level benchmarking, variation highlighting

### Archetype 5: "Specialty Network Proving Value"

- **Persona:** Network Director or VP of a specialty care network (cardiology, oncology, behavioral health)
- **Population:** 5K-30K patients in a focused clinical domain
- **Key Measures:** Specialty-specific quality measures, referral loop closure, outcome tracking
- **Monday Morning Scenario:** "Your cardiology network has 40 payer contracts that each define quality differently. HDIM normalizes them. Here are the 5 measures that appear in 80% of your contracts — focus here. And 312 patients have open referral loops where follow-up hasn't been documented."
- **Pain Hook:** Payer-specific measure confusion, can't demonstrate value to referring systems, referral leakage
- **Seed Profile:** 15K patients, 40 normalized contracts, 5 common measures, referral loop tracking

### Overlay Fields (Per Target)

| Field | Source |
|-------|--------|
| Organization name | Dossier |
| Patient/member volume | Dossier (scales seed data) |
| Priority measures | Dossier quality performance section |
| Geography/state | Dossier (Medicaid program names, local context) |
| Opening narrative | Dossier pain hypothesis + specific trigger event |

---

## 3. Outreach Campaign Design

### Trigger Categories

| Trigger | Urgency | Response Window |
|---------|---------|-----------------|
| Star Rating Drop | Critical | Within 1 week |
| Quality Report Published | High | Within 2 weeks |
| Leadership Change | High | Within 2 weeks |
| VBC Contract Announcement | Medium | Within 30 days |
| M&A / Expansion | Medium | Within 30 days |
| Conference Presentation | Low | Within 30 days |

### 3-Touch Multi-Channel Sequence

**Touch 1 — Day 0 (Trigger detected)**
- **LinkedIn:** Engage with their content (like, thoughtful comment referencing the news)
- **Email:** Personalized cold email referencing the specific trigger + pain hypothesis
- Template: "I saw [ORG] published [SPECIFIC THING]. [1-sentence pain hypothesis]. We built something that addresses exactly this — would a 15-minute walkthrough be worth your time?"

**Touch 2 — Day 5-7**
- **LinkedIn:** Send connection request with personalized note referencing Touch 1 topic
- **Email:** Follow-up with a specific data point from their demo archetype ("For an org your size, we typically see X care gaps recoverable in the first 90 days")

**Touch 3 — Day 14-18**
- **LinkedIn:** Share relevant content and tag/mention if appropriate
- **Email:** Final touch — offer customized demo, include 60-90 second Loom video of their archetype scenario

### Content Assets Per Archetype

| Asset | Purpose | Count |
|-------|---------|-------|
| Email template set | 3 emails with merge fields per trigger type | 5 archetypes x 6 triggers = 30 sets |
| LinkedIn comment bank | Thoughtful comments for engaging with target content | 5-6 per archetype |
| Loom script | 60-90 second demo walkthrough narration | 1 per archetype |
| Leave-behind PDF | 1-page "What HDIM Does for [Org Type]" | 1 per archetype |

**Practical note:** Start with email templates for the 2-3 most common trigger types per archetype. Build comment banks and Loom scripts as you encounter real triggers. Don't build all 30 email sets upfront.

---

## 4. File Organization

```
docs/abm/
├── README.md                          # How to use this system
├── scoring-criteria.md                # Target selection rubric (5 dimensions)
├── archetypes/
│   ├── 01-fqhc-closing-the-gap.md     # Seed profile + scenario script + email templates
│   ├── 02-ma-plan-chasing-4-stars.md
│   ├── 03-aco-under-the-gun.md
│   ├── 04-health-system-integrating.md
│   └── 05-specialty-network-value.md
├── targets/
│   ├── _tracker.md                    # Master outreach tracker table (see schema below)
│   ├── _template.md                   # Blank dossier template
│   └── [target-NN-org-name.md]        # One per target
├── templates/
│   ├── email-sequences/
│   │   ├── star-rating-drop.md        # 3-email sequence per trigger type
│   │   ├── quality-report-published.md
│   │   ├── leadership-change.md
│   │   ├── vbc-contract.md
│   │   ├── merger-expansion.md
│   │   └── conference-presentation.md
│   ├── linkedin-comment-banks/
│   │   ├── fqhc-comments.md
│   │   ├── ma-plan-comments.md
│   │   ├── aco-comments.md
│   │   ├── health-system-comments.md
│   │   └── specialty-network-comments.md
│   └── loom-scripts/
│       ├── fqhc-walkthrough.md
│       ├── ma-plan-walkthrough.md
│       ├── aco-walkthrough.md
│       ├── health-system-walkthrough.md
│       └── specialty-network-walkthrough.md
└── monitoring/
    ├── weekly-scan-checklist.md        # What to check each week
    └── trigger-log.md                  # Timestamped trigger events
```

### Tracker Table Schema (`_tracker.md`)

```markdown
| # | Target | Archetype | Score | Trigger | Trigger Date | T1 Date | T1 Channel | T2 Date | T2 Channel | T3 Date | T3 Channel | Response | Status | Next Action |
|---|--------|-----------|-------|---------|-------------|---------|------------|---------|------------|---------|------------|----------|--------|-------------|
```

**Status values:** `new` | `triggered` | `in-sequence` | `responded` | `demo-booked` | `dormant`

### Demo Seed Data

```
backend/modules/shared/demo-seed/src/main/resources/
├── archetypes/
│   ├── fqhc-profile.json              # 20K patients, Medicaid-heavy
│   ├── ma-plan-profile.json           # 100K lives, Stars-weighted
│   ├── aco-profile.json               # 50K attributed, quality gates
│   ├── health-system-profile.json     # 200K multi-EHR, benchmarking
│   └── specialty-network-profile.json # 15K focused domain
└── overlays/
    ├── _overlay-template.json          # Merge fields: org name, volume, geography
    └── [per-target overlays as created]
```

---

## 5. Monitoring & Iteration

### Weekly Scan (15 minutes)

**Sources:**
- Google Alerts (1 per active target org name)
- CMS.gov (Star ratings October, quality data releases, ACO performance)
- LinkedIn (decision-maker activity, org page posts, quality/pop health job postings)
- Google News (org name + "quality" or "acquisition" or "contract")
- NCQA/HEDIS (accreditation changes, new measure year announcements)

**Output:** Update `trigger-log.md`. If trigger fires, initiate matching outreach sequence.

### Monthly Review (30 minutes)

- Re-score all active targets
- Rotate cold targets out (no triggers for 60+ days), replace with fresh prospects
- Review outreach tracker — move T3 no-response targets to dormant
- Update dossiers with new intelligence
- Assess archetype coverage — need a new one?

### Success Metrics

| Metric | Target | Timeline |
|--------|--------|----------|
| Dossiers completed | 10-15 | First 2 weeks |
| Outreach sequences initiated | 5+ | First month |
| Response rate | 15-25% | Ongoing |
| Demo meetings booked | 2-3 | First 6 weeks |
| Pipeline generated | $150K-$300K | 90 days |

### Iteration Triggers

- Response rate < 10% after 30 days: revisit email templates, test different pain hooks
- Wrong archetype assigned often: may need 6th archetype or adjust existing
- Triggers drying up: expand sources (trade publications, state health dept releases)
- Demos not converting: review scenario scripts, test Monday morning story resonance

---

## 6. Workflow: End-to-End for One Target

1. **Select** — Score prospect against rubric. Must hit 18+.
2. **Research** — Copy `_template.md`, fill dossier (30-60 min). Document pain hypothesis.
3. **Map** — Assign archetype. Note overlay customizations.
4. **Seed** — Configure demo overlay (org name, volume, measures). Verify demo runs.
5. **Monitor** — Add to weekly scan. Set Google Alert.
6. **Trigger fires** — Pull matching email sequence. Customize merge fields from dossier.
7. **Execute** — Run 3-touch sequence across email + LinkedIn.
8. **Track** — Update `_tracker.md` after each touch.
9. **Convert** — Demo meeting booked. Run archetype walkthrough with org-specific overlay and opening narrative.
10. **Iterate** — Monthly review: re-score, rotate, refresh.

---

## Implementation Sequence

1. **Foundation:** Create directory structure, scoring criteria, dossier template, tracker
2. **Archetypes:** Write 5 archetype files (scenario scripts, seed profiles)
3. **Research:** Complete 10-15 target dossiers with scoring and archetype mapping
4. **Templates:** Write email sequences for top 2-3 trigger types per archetype
5. **Seed Data:** Create archetype JSON seed profiles and overlay template
6. **Outreach Assets:** LinkedIn comment banks, Loom scripts (build as triggers appear)
7. **Monitoring:** Set up Google Alerts, weekly checklist, trigger log
8. **Execute:** Begin outreach on first triggered targets
