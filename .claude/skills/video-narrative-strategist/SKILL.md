---
name: video-narrative-strategist
description: Plans the narrative arc for HDIM Remotion marketing videos. Analyzes the target role persona, maps pain points to a 4-act story structure, and produces a 10-scene story brief that drives all downstream content creation. Use when starting a new role video or revising an existing video's narrative flow.
---

# Video Narrative Strategist

## What This Skill Does

Plans the story arc for a single HDIM role video by analyzing the target persona and mapping their real-world pain points to a 10-scene visual narrative. The output is a **story brief** — a structured document that the clinical-writer skill transforms into production config.

**This skill does NOT write final copy.** It defines *what* each scene shows and *why*, not the exact words.

## When This Skill Activates

- "Plan the narrative for a [role] video"
- "Create a story brief for [role]"
- "What story should the [role] video tell?"
- When invoked by the `/video:render-role-video` command (Agent 1 in the pipeline)

## Required Context

Before starting, load the target persona from the `hdim-customer-scenarios` skill:

| Video Role | Persona to Load |
|---|---|
| `care-manager` | Persona 2: Quality Coordinator + Persona 3: Provider (Care Manager variant) |
| `cmo` | Persona 1: Health Plan CMO / VP Quality |
| `quality-analyst` | Persona 2: Quality Coordinator (analyst variant) |
| `provider` | Persona 3: Healthcare Provider |
| `data-analyst` | Persona 5: IT / Analytics Leader (analytics variant) |
| `admin` | Persona 5: IT / Analytics Leader (admin variant) |
| `ai-user` | Persona 3: Provider + Persona 2: Coordinator (AI-assisted variant) |

Also load the existing config file for reference:
```
landing-page-v0/remotion/src/compositions/role-videos/{role}-video.config.ts
```

And verify screenshots exist:
```
landing-page-v0/remotion/public/screenshots/{role}/
```

## The 4-Act Narrative Structure

Every role video follows this arc:

### Act 1: Status Quo (Scenes 1-2)
**Purpose:** Show the daily frustration the viewer ALREADY experiences.
**Emotional beat:** Recognition — "That's exactly my problem."

- Scene 1: The dashboard or starting screen — the first thing this persona sees each morning
- Scene 2: The overview of the problem space — too many gaps, too many measures, too much manual work

### Act 2: Discovery (Scenes 3-5)
**Purpose:** The platform reveals the most impactful problem to solve RIGHT NOW.
**Emotional beat:** Relief — "Finally, I can see what matters."

- Scene 3: A filter, search, or AI recommendation that narrows focus
- Scene 4: The specific patient/measure/gap that needs attention
- Scene 5: The detail view showing clinical context + actionable options

### Act 3: Resolution (Scenes 6-8)
**Purpose:** The user solves the problem in one workflow — fast, confident, complete.
**Emotional beat:** Satisfaction — "That took 8 seconds, not 8 days."

- Scene 6: The action dialog/intervention/evaluation
- Scene 7: Confirmation — the problem is resolved, with success feedback
- Scene 8: The patient/measure record reflecting the change

### Act 4: Impact (Scenes 9-10)
**Purpose:** Zoom out to show system-wide and financial impact.
**Emotional beat:** Confidence — "This is working. I can prove it."

- Scene 9: Broader view — outreach campaigns, population trends, report exports
- Scene 10: Return to dashboard — compliance improved, metrics updated, story complete

## Scene Brief Format

For each scene, produce:

```json
{
  "sceneNumber": 1,
  "act": "Status Quo",
  "screenshotPath": "screenshots/care-manager/care-manager-01-dashboard-overview.png",
  "platformPage": "/dashboard",
  "whatViewerSees": "Summary cards showing open care gaps, compliance rate, and today's priorities",
  "whyItMatters": "Care managers check this every morning — it sets the tone for the day",
  "clinicalContext": "45 open care gaps across a 5,000-member panel, 9 high-urgency",
  "proofMetric": "45 open gaps / 9 high-urgency",
  "suggestedOverlayFocus": "Highlight the care gap count widget",
  "narrativeArcRole": "Establishes the scale of the daily workload"
}
```

## Validation Rules

Before finalizing the story brief:

1. **Screenshot match:** Every `screenshotPath` must match a real file in `landing-page-v0/remotion/public/screenshots/{role}/`
2. **Page match:** Every `platformPage` must match a real route in the Angular clinical portal
3. **Persona alignment:** Every `whyItMatters` must connect to a pain point from the loaded persona
4. **Arc completeness:** All 4 acts must be represented. No act should have 0 scenes.
5. **Metric specificity:** Every `proofMetric` must be a concrete number or range — no vague claims
6. **Story coherence:** Reading the 10 `narrativeArcRole` values in sequence should tell a complete story

## Available Screenshot Paths by Role

Each role has exactly 10 screenshots following the naming pattern:
```
screenshots/{role}/{role}-{##}-{description}.png
```

Numbers 01 through 10. Verify the actual filenames before referencing them.

## Available Platform Routes

Key routes from the Angular clinical portal:

| Route | What It Shows |
|---|---|
| `/dashboard` | Summary cards, compliance rate, care gap widget |
| `/care-gaps` | Care gap table with filters, urgency, patient details |
| `/patients` | Patient list / patient detail demographics |
| `/care-recommendations` | Clinical recommendations with AI context |
| `/risk-stratification` | Risk distribution, high-risk cohort drill-down |
| `/quality-measures` | HEDIS measure list with evaluation status |
| `/outreach-campaigns` | Automated outreach for gap closure |
| `/pre-visit` | Pre-visit planning dashboard |
| `/ai-assistant` | Natural language AI query interface |
| `/visualization/quality-constellation` | Longitudinal quality visualization |
| `/admin` | Tenant settings, user management |
| `/audit-logs` | PHI access audit trail |

## Output Format

```markdown
# Story Brief: [Role Title]

**Persona:** [Primary persona name from hdim-customer-scenarios]
**Story:** "[One-sentence summary — e.g., 'A care manager closes a mammography gap in 8 seconds']"
**Core Pain Point:** [The #1 pain point this video addresses]
**Proof of Value:** [The single metric that proves HDIM solves it]

## Scene Plan

| # | Act | Page | What Viewer Sees | Why It Matters | Proof Metric |
|---|-----|------|------------------|----------------|--------------|
| 1 | Status Quo | /dashboard | ... | ... | ... |
| 2 | Status Quo | /care-gaps | ... | ... | ... |
| ... | ... | ... | ... | ... | ... |
| 10 | Impact | /dashboard | ... | ... | ... |

## Narrative Arc Summary
[2-3 sentences explaining how the 10 scenes flow as a story a buyer would recognize]
```

## Integration

- **Input from:** `/video:render-role-video` command (role name as argument)
- **Output to:** `video-clinical-writer` skill (story brief as context)
- **Depends on:** `hdim-customer-scenarios` skill (persona data)
