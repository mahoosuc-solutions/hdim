# Video Agent Workflow Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create 5 Claude Code skills + 1 command that form a sequential AI agent pipeline for producing high-quality Remotion marketing videos.

**Architecture:** Five skills (narrative-strategist, clinical-writer, medical-reviewer, visual-designer, pipeline-coordinator) each refine the same `RoleStoryConfig` TypeScript config object. The `/render-role-video` command orchestrates them sequentially. Each agent draws from existing skills (`hdim-customer-scenarios`, `brand-voice`, `frontend-design`, `content-optimizer`).

**Tech Stack:** Claude Code skills (markdown), Claude Code commands (markdown), existing Remotion/TypeScript infrastructure.

---

## Task 1: Create the Narrative Strategist Skill

**Files:**
- Create: `.claude/skills/video-narrative-strategist/SKILL.md`

**Step 1: Create the skill directory**

```bash
mkdir -p .claude/skills/video-narrative-strategist
```

**Step 2: Write the skill file**

Create `.claude/skills/video-narrative-strategist/SKILL.md` with the following content:

```markdown
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
- When invoked by the `/render-role-video` command (Agent 1 in the pipeline)

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

## Output Format

```markdown
# Story Brief: [Role Title]

**Persona:** [Primary persona name from hdim-customer-scenarios]
**Story:** "[One-sentence summary — e.g., 'A care manager closes Eleanor Anderson's mammography gap in 8 seconds']"
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

- **Input from:** `/render-role-video` command (role name as argument)
- **Output to:** `video-clinical-writer` skill (story brief as context)
- **Depends on:** `hdim-customer-scenarios` skill (persona data)
```

**Step 3: Verify the skill file exists and has correct frontmatter**

```bash
head -5 .claude/skills/video-narrative-strategist/SKILL.md
```

Expected: YAML frontmatter with `name: video-narrative-strategist`

**Step 4: Commit**

```bash
git add .claude/skills/video-narrative-strategist/SKILL.md
git commit -m "feat(skills): add video-narrative-strategist — story arc planning agent"
```

---

## Task 2: Create the Clinical Writer Skill

**Files:**
- Create: `.claude/skills/video-clinical-writer/SKILL.md`

**Step 1: Create the skill directory**

```bash
mkdir -p .claude/skills/video-clinical-writer
```

**Step 2: Write the skill file**

Create `.claude/skills/video-clinical-writer/SKILL.md` with the following content:

```markdown
---
name: video-clinical-writer
description: Writes production-ready text content for HDIM Remotion marketing videos. Transforms story briefs into complete RoleStoryConfig text fields — headlines, narrative captions, problem statements, metrics, and CTA copy. Enforces brand-voice principles (data-driven, no vague claims, specific numbers). Use when writing or revising video script content.
---

# Video Clinical Writer

## What This Skill Does

Transforms a story brief (from `video-narrative-strategist`) into production-ready text content for a `RoleStoryConfig` TypeScript config object. Writes every human-readable text field in the config: headlines, narrative captions, problem statements, metrics, and CTA copy.

**This skill writes text, not visual overlays.** Overlay design is handled by `video-visual-designer`.

## When This Skill Activates

- "Write the video script for [role]"
- "Improve the captions for the [role] video"
- "Rewrite the CTA for [role]"
- When invoked by the `/render-role-video` command (Agent 2 in the pipeline)

## Required Context

1. **Story brief** from `video-narrative-strategist` (defines the 10-scene arc)
2. **Existing config** from `landing-page-v0/remotion/src/compositions/role-videos/{role}-video.config.ts` (starting point to refine)
3. **Brand voice principles** from `brand-voice` skill (loaded automatically)

## Text Fields to Write

### Title Slide

| Field | Rules | Length |
|---|---|---|
| `titleSlide.headline` | The persona's burning question. Must end with `?`. Use `\n` for line break. Enclose in double quotes for visual emphasis. | 6-12 words |
| `titleSlide.subheadline` | The platform's specific promise. Include a concrete before/after comparison. | 12-20 words |

**Example:**
```typescript
titleSlide: {
  headline: '"How Do I Close\nMore Gaps Today?"',
  subheadline: 'A care manager closes a mammography gap in 8 seconds — not 8 days',
  durationFrames: 90,
}
```

### Problem Slide

| Field | Rules | Length |
|---|---|---|
| `problemSlide.statement` | The cost of the status quo. Must include a specific time or dollar amount. Describe what the persona DOES manually, not what the platform DOES. | 15-25 words |
| `problemSlide.metric` | A single shocking statistic. Format: "[number]% of [things] [consequence]" or "[number] [unit] to [action]". Must be citeable. | 8-15 words |

**Example:**
```typescript
problemSlide: {
  statement: 'Care managers spend 40% of their day hunting through spreadsheets to find which patients need outreach',
  metric: '73% of gaps take >30 days to close manually',
  durationFrames: 120,
}
```

### Scene Narrative Captions (10 scenes)

| Field | Rules | Length |
|---|---|---|
| `scenes[N].narrativeCaption` | Active voice. Describes what the user IS DOING in this scene. Must include one clinical term or metric. No marketing fluff. | 12-18 words |

**Rules for narrative captions:**
1. **Active voice only:** "Care manager filters by urgency" NOT "Urgency filter is applied"
2. **One clinical term per caption:** Include a HEDIS measure code, medical term, or healthcare workflow term
3. **One number per caption:** A count, percentage, time, or dollar amount
4. **No platform feature names:** Describe the ACTION, not the UI element
5. **Progression:** Captions should read as a coherent story when listed in sequence

**Good examples:**
- "Morning overview — summary cards show today's 45 care gap priorities at a glance"
- "High-urgency filter isolates 9 gaps affecting Star Ratings — prioritized by financial impact"
- "BCS-E mammography screening 60 days overdue — clinical context shows intervention options"

**Bad examples:**
- "The care gap dashboard" (no action, no metric)
- "Using our powerful AI engine" (marketing fluff)
- "Gap detail view" (too vague, no clinical context)

### CTA Slide

| Field | Rules | Length |
|---|---|---|
| `cta.headline` | Transformation promise — what changes. Imperative mood. | 4-8 words |
| `cta.highlightText` | Contrasting phrase — what it replaces. Same grammatical structure as headline. | 2-4 words |
| `cta.stats[].value` | 3 proof point numbers. Each independently verifiable. Format: number + unit. | 2-6 chars each |
| `cta.ctaText` | Action verb + noun. No "Learn More" — be specific. | 3-6 words |

**Example:**
```typescript
cta: {
  headline: 'Close Care Gaps in Seconds',
  highlightText: 'Not Weeks',
  stats: [
    { value: '8 sec', backgroundColor: '...', borderColor: '...', textColor: 'white', glowColor: '...' },
    { value: '35-40%', backgroundColor: '...', borderColor: '...', textColor: 'white', glowColor: '...' },
    { value: '99.9%', backgroundColor: '...', borderColor: '...', textColor: 'white', glowColor: '...' },
  ],
  ctaText: 'See a Live Demo',
  ctaUrl: 'healthdatainmotion.com',
  durationFrames: 540,
}
```

## Brand Voice Enforcement

Before finalizing, run every text field against these checks from the `brand-voice` skill:

**Data Checklist (every field must pass):**
- [ ] Every claim has a specific number
- [ ] No superlatives ("best", "leading", "revolutionary")
- [ ] No "up to X%" — use exact numbers or ranges
- [ ] No vague verbs ("improve", "enhance", "optimize") without a measured outcome

**Tone Checklist:**
- [ ] Practical and actionable (not inspirational)
- [ ] Describes what the USER does (not what the PLATFORM does)
- [ ] Honest about what the screenshot actually shows
- [ ] Value-first (not sales-first)

**Healthcare-Specific Checklist:**
- [ ] HEDIS measure codes are real (BCS-E, CDC, HbA1c)
- [ ] Patient scenarios are clinically plausible
- [ ] Workflow actions match what this role actually does
- [ ] No fictional patient names in captions (use clinical descriptions)

## Output Format

The output is a complete `RoleStoryConfig` TypeScript object with all text fields filled. Output as a fenced code block that can be directly pasted into the config file:

```typescript
export const {role}Config: RoleStoryConfig = {
  role: { ... },
  titleSlide: { ... },
  problemSlide: { ... },
  scenes: [ ... ],  // 10 scenes with narrativeCaption filled
  cta: { ... },
};
```

**Note:** Leave `overlays[]` arrays as-is from the existing config. The `video-visual-designer` skill handles overlay optimization separately.

## Integration

- **Input from:** `video-narrative-strategist` (story brief) + existing config file
- **Output to:** `video-medical-reviewer` (complete config for accuracy validation)
- **Depends on:** `brand-voice` skill (enforcement rules)
```

**Step 3: Verify the skill file**

```bash
head -5 .claude/skills/video-clinical-writer/SKILL.md
```

Expected: YAML frontmatter with `name: video-clinical-writer`

**Step 4: Commit**

```bash
git add .claude/skills/video-clinical-writer/SKILL.md
git commit -m "feat(skills): add video-clinical-writer — brand-voice content writing agent"
```

---

## Task 3: Create the Medical Reviewer Skill

**Files:**
- Create: `.claude/skills/video-medical-reviewer/SKILL.md`

**Step 1: Create the skill directory**

```bash
mkdir -p .claude/skills/video-medical-reviewer
```

**Step 2: Write the skill file**

Create `.claude/skills/video-medical-reviewer/SKILL.md` with the following content:

```markdown
---
name: video-medical-reviewer
description: Validates clinical accuracy, HEDIS compliance, and regulatory safety of HDIM Remotion marketing video content. Reviews every text field in a RoleStoryConfig for medical plausibility, correct terminology, accurate statistics, and HIPAA-safe marketing patterns. Use when reviewing video content before rendering.
---

# Video Medical Reviewer

## What This Skill Does

Reviews a complete `RoleStoryConfig` for clinical accuracy, regulatory compliance, and healthcare domain correctness. Every text field is validated against real HEDIS specifications, CMS definitions, and healthcare workflow realities.

**This is a quality gate.** Content cannot proceed to visual design or rendering until all fields pass review.

## When This Skill Activates

- "Review the [role] video for medical accuracy"
- "Validate the clinical claims in [role] config"
- "Check HEDIS codes in the video content"
- When invoked by the `/render-role-video` command (Agent 3 in the pipeline)

## Review Checklist

For EVERY text field in the config, validate against these 7 checks:

### 1. HEDIS Measure Accuracy
Verify that all HEDIS measure codes reference real CMS/NCQA measures:

| Code Used | Must Match |
|---|---|
| BCS / BCS-E | Breast Cancer Screening (women 50-74) |
| CDC / HbA1c | Comprehensive Diabetes Care (HbA1c control) |
| CBP | Controlling High Blood Pressure |
| COL | Colorectal Cancer Screening (50-75) |
| CIS | Childhood Immunization Status |
| WCV | Well-Child Visits |
| AWC | Adolescent Well-Care Visits |
| PPC | Prenatal and Postpartum Care |
| FUM / FUH | Follow-Up After ED Visit / Hospitalization |
| CWP | Appropriate Testing for Pharyngitis |

**Reject** if a measure code doesn't match a real HEDIS MY2024 measure.

### 2. Clinical Plausibility
Verify that patient scenarios make medical sense:

- Age ranges match screening guidelines (e.g., mammography 40-74, colonoscopy 45-75)
- Conditions and medications are compatible (e.g., HbA1c is for diabetes, not heart disease)
- Lab values are realistic (HbA1c range: 4.0-14.0%, typical target <7.0%)
- Time intervals are plausible (a mammogram isn't "60 days overdue" if the patient is 25)

### 3. Statistics Validation
Verify that all numeric claims are defensible:

| Claim Type | Validation |
|---|---|
| "X% of gaps take >Y days" | Must cite NCQA State of Healthcare Quality Report or equivalent |
| "X% time savings" | Must be calculable from described workflow improvement |
| "$X million penalty" | Must match HHS OCR enforcement data |
| "X patients/members" | Must be plausible for pilot scale (1,000-10,000 members) |

**Accept ranges** like "35-40%" (honest). **Reject** precision without citation like "73.2%" (false precision).

### 4. Terminology Correctness
Verify healthcare terms are used correctly:

| Term | Correct Usage | Common Error |
|---|---|---|
| Care gap | A specific measure where a patient is non-compliant | "Quality gap" (informal) |
| Measure | A HEDIS/CMS quality measure with defined numerator/denominator | "Metric" (ambiguous) |
| Star rating | CMS Medicare Advantage quality rating (1-5 stars) | "Quality score" (vague) |
| QRDA | Quality Reporting Document Architecture (I=individual, III=aggregate) | "Quality report" (informal) |
| CQL | Clinical Quality Language (measure logic) | "Query language" |

### 5. Regulatory Safety (HIPAA Marketing)
Verify that marketing content does not create HIPAA exposure:

- **No real patient names** — use clinical descriptions ("mammography screening overdue") not "Eleanor Anderson"
- **No specific dates of birth or MRNs** — acceptable in demo screenshots, not in caption text
- **No facility names** that could identify real organizations
- **Disclaimer language** is NOT required in video content (it's marketing, not a BAA)

**Note:** Screenshot images from the demo environment may show fictional patient names (Eleanor Anderson, Michael Chen). This is acceptable in the screenshots themselves — only the TEXT overlays and captions must be HIPAA-safe.

### 6. CMS Alignment
Verify that measure descriptions match current CMS definitions:

- HEDIS MY2024 specifications (current measurement year)
- ECDS transition timeline references are accurate (2025 pilot, 2026 expansion, 2030 full digital)
- Star rating calculation methodology is correctly described

### 7. Workflow Realism
Verify that described actions match real clinical workflows:

| Role | CAN Do | CANNOT Do |
|---|---|---|
| Care Manager | Close gaps, coordinate outreach, schedule screenings | Write CQL, configure FHIR, modify measures |
| CMO | View dashboards, set priorities, approve strategies | Close individual gaps, run evaluations |
| Quality Analyst | Run evaluations, generate reports, analyze trends | Make clinical decisions, close gaps |
| Provider | Order tests, prescribe, document encounters | Configure system, run batch evaluations |
| Data Analyst | Build reports, analyze populations, visualize trends | Make clinical decisions, close gaps |
| Admin | Configure tenants, manage users, view audit logs | Run evaluations, close gaps |
| AI User | Query AI, review recommendations, accept/reject suggestions | Override clinical decisions |

## Per-Field Review Output

For each text field, assign one status:

```
APPROVED — Clinically accurate, no changes needed
REVISED  — Accuracy issue found, replacement text provided
REJECTED — Fundamental inaccuracy, cannot be fixed with minor edit (return to clinical-writer)
```

**Output format:**

```markdown
## Medical Review: [Role] Video Config

### Summary
- Fields reviewed: [N]
- APPROVED: [N]
- REVISED: [N]
- REJECTED: [N]
- **Gate status:** PASS / FAIL (FAIL if any REJECTED remain)

### Field Reviews

| Field | Status | Note |
|---|---|---|
| `titleSlide.headline` | APPROVED | Question accurately reflects persona pain point |
| `problemSlide.metric` | REVISED | Changed "73%" to "70-75%" (range more defensible without primary source) |
| `scenes[3].narrativeCaption` | REVISED | Removed patient name "Eleanor Anderson" from caption text |
| ... | ... | ... |

### Revisions Applied
[For each REVISED field, show original → revised with explanation]

### Rejection Details
[For each REJECTED field, explain why and what the clinical-writer should fix]
```

## Feedback Loop

If any field is `REJECTED`:
1. Return the entire config to `video-clinical-writer` with rejection details
2. Clinical writer revises ONLY the rejected fields
3. Medical reviewer re-reviews ONLY the revised fields
4. Repeat until all fields are `APPROVED` or `REVISED`

Maximum iterations: 3. If still failing after 3 rounds, flag for human review.

## Integration

- **Input from:** `video-clinical-writer` (complete config)
- **Output to:** `video-visual-designer` (validated config)
- **Depends on:** `hdim-customer-scenarios` (workflow validation reference)
```

**Step 3: Verify**

```bash
head -5 .claude/skills/video-medical-reviewer/SKILL.md
```

**Step 4: Commit**

```bash
git add .claude/skills/video-medical-reviewer/SKILL.md
git commit -m "feat(skills): add video-medical-reviewer — clinical accuracy validation agent"
```

---

## Task 4: Create the Visual Designer Skill

**Files:**
- Create: `.claude/skills/video-visual-designer/SKILL.md`

**Step 1: Create the skill directory**

```bash
mkdir -p .claude/skills/video-visual-designer
```

**Step 2: Write the skill file**

Create `.claude/skills/video-visual-designer/SKILL.md` with the following content:

```markdown
---
name: video-visual-designer
description: Optimizes overlay placement, timing, visual hierarchy, and Ken Burns motion for HDIM Remotion marketing videos. Designs the overlays[] arrays in each scene to maximize visual impact while maintaining readability. Applies frontend-design principles for animation, color, and composition. Use when designing or refining video overlay layouts.
---

# Video Visual Designer

## What This Skill Does

Designs the `overlays[]` arrays for each of the 10 scenes in a `RoleStoryConfig`. Determines which overlay types to use (glow-highlight, metric, badge, text), where to place them on screen, when they appear/disappear, and how Ken Burns zoom/pan creates visual flow.

**This skill designs visual elements, not text content.** The text values in overlays come from the clinical-writer; this skill controls placement, timing, and type selection.

## When This Skill Activates

- "Design the overlays for the [role] video"
- "Optimize the visual hierarchy for [role]"
- "Fix the overlay layout on scene [N]"
- When invoked by the `/render-role-video` command (Agent 4 in the pipeline)

## Required Context

1. **Validated config** from `video-medical-reviewer` (all text fields finalized)
2. **Screenshot files** in `landing-page-v0/remotion/public/screenshots/{role}/` (to identify what UI elements to highlight)
3. **Component API** from `landing-page-v0/remotion/src/components/ScreenshotWithOverlay.tsx` (overlay type definitions)

## Overlay Types Available

The `ScreenshotWithOverlay` component renders 4 overlay types:

### `glow-highlight`
**Visual:** Pulsing gradient border box that draws attention to a specific UI element.
**Props:** `{ width: number, height: number, borderColor: 'green' | 'blue' | 'red', pulseCount: number }`
**Best for:** Interactive UI elements (buttons, rows, filters, cards)
**Position:** Must align with the actual UI element in the screenshot

### `metric`
**Visual:** Animated count-up number with glow effect on completion.
**Props:** `{ from: number, to: number, decimals: number, suffix: string, duration: number, label: string, fontSize: string }`
**Best for:** Numeric proof points (gap counts, percentages, time savings)
**Position:** Top-right quadrant preferred (eye naturally goes there for data)

### `badge`
**Visual:** Colored rounded pill with pop-in animation.
**Props:** `{ text: string, backgroundColor: string, color: string, fontSize: string }`
**Best for:** State changes ("Gap Closed!", "HEDIS BCS-E", "HIGH Urgency")
**Position:** Near the relevant UI element, slightly offset

### `text`
**Visual:** Semi-transparent dark pill with explanatory text.
**Props:** `{ text: string, fontSize: string }`
**Best for:** Clinical context not visible in the screenshot
**Position:** Edges or corners where it won't obscure important UI

## Design Principles

### 1. One Focal Point Per Scene
- Maximum 2 overlays visible at any moment (1 primary + 1 supporting)
- Maximum 3 overlays total per scene (they appear at different times)
- **Never** 0 overlays (every scene needs visual guidance)
- **Never** 4+ overlays (visual clutter destroys comprehension)

### 2. Visual Variety Across Scenes
No two adjacent scenes should use the same PRIMARY overlay type:

```
Scene 1: glow-highlight (dashboard widget)
Scene 2: badge (urgency count)
Scene 3: glow-highlight (filter button)     ← OK: scene 2 was badge
Scene 4: text (clinical context)
Scene 5: glow-highlight (action button)
Scene 6: metric (time saved)
Scene 7: badge ("Gap Closed!")
Scene 8: glow-highlight (patient record)
Scene 9: text (outreach context)
Scene 10: metric (compliance improvement)
```

### 3. Timing Progression
Overlays appear AFTER the viewer has oriented to the screenshot:

| Overlay | `startFrame` | `duration` | Why |
|---|---|---|---|
| Primary overlay | 25-40 | 120-150 | Appears after 1s orientation, stays for impact |
| Supporting overlay | 50-70 | 90-120 | Appears after primary establishes context |
| Late badge/metric | 80-100 | 80-90 | Punch line — the "aha!" moment |

**Never** start an overlay at frame 0 (jarring) or after frame 120 (too late — scene is 195f total).

### 4. Color Language
`glow-highlight` borderColor follows the narrative arc:

| Act | Scenes | Color | Meaning |
|---|---|---|---|
| Status Quo | 1-2 | `blue` | Neutral — showing the current state |
| Discovery | 3-5 | `blue` or `red` | `red` for urgency indicators, `blue` for navigation |
| Resolution | 6-8 | `green` | Success — the problem is being solved |
| Impact | 9-10 | `green` | Confidence — measurable improvement |

### 5. Position Hierarchy
Standard positions (percentage-based, origin top-left):

| Purpose | x% | y% | Notes |
|---|---|---|---|
| Primary metric | 68-78 | 8-15 | Top-right — eye goes here for data |
| Status badge | 65-80 | 8-15 | Top-right — same zone as metrics |
| Context text | 4-15 | 75-85 | Bottom-left — supplementary info |
| Glow highlight | Varies | Varies | Must match actual UI element position |
| Action badge | 45-60 | 40-55 | Center — for "Gap Closed!" moments |

### 6. Ken Burns Motion
Each scene has `zoomLevel` (1.0-1.06) and `panDirection` ('left' | 'right' | 'none'):

- **Alternate pan direction** between scenes: left, right, none, left, right...
- **Zoom level 1.02-1.04** for overview screens (dashboards, tables)
- **Zoom level 1.04-1.06** for detail screens (patient records, forms)
- **Zoom level 1.0** (no zoom) for dialog/modal screenshots

## Output Format

Output the complete `scenes[]` array with optimized overlays. Use the validated `narrativeCaption` from the medical reviewer — do not change text content.

```typescript
scenes: [
  {
    screenshot: 'screenshots/{role}/{role}-01-*.png',
    narrativeCaption: '[from medical reviewer — do not modify]',
    overlays: [
      {
        type: 'glow-highlight',
        startFrame: 30,
        duration: 140,
        position: { x: 14, y: 25 },
        props: { width: 380, height: 120, borderColor: 'blue', pulseCount: 2 },
      },
    ],
    zoomLevel: 1.03,
    panDirection: 'left',
    durationFrames: 195,
  },
  // ... 9 more scenes
]
```

## Validation Rules

Before finalizing:
- [ ] Every scene has 1-3 overlays
- [ ] No two adjacent scenes share the same primary overlay type
- [ ] All `startFrame` values are between 20 and 120
- [ ] All `duration` values are between 60 and 160
- [ ] No overlay extends past scene `durationFrames` (195): `startFrame + duration <= 195`
- [ ] `glow-highlight` borderColor follows the act-based color scheme
- [ ] `panDirection` alternates (no 3+ consecutive same direction)
- [ ] `zoomLevel` is between 1.0 and 1.06

## Integration

- **Input from:** `video-medical-reviewer` (validated config)
- **Output to:** `video-pipeline-coordinator` (final config ready to render)
- **Depends on:** `frontend-design` skill (motion and visual principles)
```

**Step 3: Verify**

```bash
head -5 .claude/skills/video-visual-designer/SKILL.md
```

**Step 4: Commit**

```bash
git add .claude/skills/video-visual-designer/SKILL.md
git commit -m "feat(skills): add video-visual-designer — overlay optimization agent"
```

---

## Task 5: Create the Pipeline Coordinator Skill

**Files:**
- Create: `.claude/skills/video-pipeline-coordinator/SKILL.md`

**Step 1: Create the skill directory**

```bash
mkdir -p .claude/skills/video-pipeline-coordinator
```

**Step 2: Write the skill file**

Create `.claude/skills/video-pipeline-coordinator/SKILL.md` with the following content:

```markdown
---
name: video-pipeline-coordinator
description: Orchestrates the final stages of HDIM Remotion video production — assembles validated config, renders MP4s, generates distribution assets, and runs QA checks. Use when rendering videos or preparing video content for distribution across platforms.
---

# Video Pipeline Coordinator

## What This Skill Does

Takes a finalized `RoleStoryConfig` (validated by medical-reviewer, visually designed by visual-designer), writes it to the config file, renders the video, and generates distribution assets for YouTube, LinkedIn, and Twitter/X.

**This is the final agent in the pipeline.** By the time content reaches this skill, all text is medically reviewed and all overlays are visually optimized.

## When This Skill Activates

- "Render the [role] video"
- "Generate distribution assets for the [role] video"
- "Run the full video pipeline for [role]"
- When invoked by the `/render-role-video` command (Agent 5 in the pipeline)

## Pipeline Steps

### Step 1: Write Config File

Save the final `RoleStoryConfig` to:
```
landing-page-v0/remotion/src/compositions/role-videos/{role}-video.config.ts
```

The file must:
- Import `RoleStoryConfig` from `../../types/role-story.types`
- Export a named constant: `export const {camelCase}Config: RoleStoryConfig = { ... }`
- Match the naming convention of existing configs (e.g., `careManagerConfig`, `cmoConfig`, `providerConfig`)

### Step 2: TypeScript Validation

Run from the `landing-page-v0/remotion/` directory:

```bash
npx tsc --noEmit
```

Expected: No errors. If type errors appear, fix the config file before proceeding.

Common type errors:
- Missing required field in `CTASlideConfig.stats[]` (needs all 5 color fields)
- `Overlay.type` not matching union type (must be exactly `'glow-highlight' | 'metric' | 'text' | 'badge'`)
- `panDirection` typo (must be `'left' | 'right' | 'none'`)

### Step 3: Render Videos

From the `landing-page-v0/remotion/` directory:

```bash
# Default variant (90s, 2700 frames)
npm run render:{role}

# Short variant (61s, 1833 frames)
npm run render:{role}:short
```

**Render script mapping** (from `package.json`):

| Role | Default Script | Short Script |
|---|---|---|
| care-manager | `render:care-manager` | `render:care-manager:short` |
| cmo | `render:cmo` | `render:cmo:short` |
| quality-analyst | `render:quality-analyst` | `render:quality-analyst:short` |
| provider | `render:provider` | `render:provider:short` |
| data-analyst | `render:data-analyst` | `render:data-analyst:short` |
| admin | `render:admin` | `render:admin:short` |
| ai-user | `render:ai-user` | `render:ai-user:short` |

**Expected output:**
- `out/role-{role}.mp4` — 10-20 MB, ~90 seconds
- `out/role-{role}-short.mp4` — 8-15 MB, ~61 seconds

### Step 4: Copy to Landing Page

```bash
npm run copy:videos
```

This copies all `out/*.mp4` files to `../public/videos/` for the landing page to serve.

### Step 5: Generate Distribution Assets

Using the `content-optimizer` skill, generate platform-specific assets:

#### YouTube
```markdown
**Title:** [Role Title]: [CTA headline] — HDIM Platform Demo (≤70 chars)
**Description:** (500 words)
  - Hook: Problem statement from `problemSlide.statement`
  - What you'll see: Scene-by-scene summary (10 bullet points from `narrativeCaption`)
  - Key results: Stats from `cta.stats[]`
  - CTA: Link to demo
**Tags:** 5 relevant tags (HEDIS, healthcare quality, [role-specific], care gaps, quality measures)
```

#### LinkedIn
```markdown
**Post format:** Data-driven hook (from brand-voice principles)
  - Line 1: Shocking metric from `problemSlide.metric`
  - Line 2-3: What the video shows (the story arc)
  - Line 4: Key result from `cta.stats[0]`
  - Line 5: CTA — "Watch the 90-second demo [link]"
**Length:** ≤3000 chars
```

#### Twitter/X Thread
```markdown
**5-tweet thread:**
  1. Hook: `problemSlide.metric` + "Watch what happens next 👇"
  2. Act 1-2 summary (Status Quo + Discovery)
  3. Act 3 summary (Resolution) — "8 seconds. That's it."
  4. Act 4 summary (Impact) — key metric
  5. CTA: "Full 90s demo: [link]" + key stats
```

### Step 6: Final QA Checklist

Before declaring the video complete:

- [ ] **Duration:** Default variant is 88-92 seconds (±2s from target 90s)
- [ ] **Duration:** Short variant is 59-63 seconds (±2s from target 61s)
- [ ] **File size:** Default 10-20 MB, Short 8-15 MB (H.264 1080p)
- [ ] **No blank frames:** Scrub through video, verify every scene has a screenshot
- [ ] **Text readability:** All overlays readable at 1080p (font ≥1.2rem)
- [ ] **Brand voice:** All captions pass `brand-voice` checklist
- [ ] **Medical accuracy:** Confirmed by `video-medical-reviewer` gate
- [ ] **Visual variety:** No two adjacent scenes share overlay type
- [ ] **Distribution assets:** YouTube, LinkedIn, Twitter/X copy generated

## Output Report

After pipeline completion, output:

```markdown
## Pipeline Complete: [Role] Video

### Rendered Files
- `out/role-{role}.mp4` — [X] MB, [Y]s
- `out/role-{role}-short.mp4` — [X] MB, [Y]s

### QA Results
- [X/9] checks passed
- Issues: [list any failures]

### Distribution Assets
- YouTube: Title + Description + Tags (ready to paste)
- LinkedIn: Post copy (ready to paste)
- Twitter/X: 5-tweet thread (ready to paste)

### Config File
- `landing-page-v0/remotion/src/compositions/role-videos/{role}-video.config.ts`
- Last modified: [timestamp]
```

## Integration

- **Input from:** `video-visual-designer` (final config)
- **Output:** Rendered MP4s + distribution copy
- **Depends on:** `content-optimizer` skill (distribution formatting), Remotion render infrastructure
```

**Step 3: Verify**

```bash
head -5 .claude/skills/video-pipeline-coordinator/SKILL.md
```

**Step 4: Commit**

```bash
git add .claude/skills/video-pipeline-coordinator/SKILL.md
git commit -m "feat(skills): add video-pipeline-coordinator — render + distribution agent"
```

---

## Task 6: Create the `/render-role-video` Command

**Files:**
- Create: `.claude/commands/video/render-role-video.md`

**Step 1: Create the command directory**

```bash
mkdir -p .claude/commands/video
```

**Step 2: Write the command file**

Create `.claude/commands/video/render-role-video.md` with the following content:

```markdown
---
name: video:render-role-video
description: Orchestrate the 5-agent video production pipeline — narrative strategy, clinical writing, medical review, visual design, and rendering — to produce a high-quality Remotion marketing video for a specific HDIM role.
category: video
priority: high
---

# HDIM Video Production Pipeline

You are orchestrating the full 5-agent video production pipeline. Execute each agent in strict sequence. Do not skip agents unless `--from-step` is specified.

## Command Signature

```
/video:render-role-video <role> [--from-step <1-5>] [--skip-render] [--all]
```

## Parse Arguments

| Variable | Default | Source |
|---|---|---|
| `ROLE` | (required) | First positional arg: `care-manager`, `cmo`, `quality-analyst`, `provider`, `data-analyst`, `admin`, `ai-user` |
| `FROM_STEP` | `1` | `--from-step <N>` — resume from agent N |
| `SKIP_RENDER` | `false` | `--skip-render` — produce config only |
| `ALL_ROLES` | `false` | `--all` — run pipeline for all 7 roles |

## Validate Arguments

```
VALID_ROLES = [care-manager, cmo, quality-analyst, provider, data-analyst, admin, ai-user]
```

If `ROLE` not in `VALID_ROLES` and `--all` not set, abort with: "Invalid role. Valid roles: care-manager, cmo, quality-analyst, provider, data-analyst, admin, ai-user"

If `--all` is set, iterate through all 7 roles sequentially.

## Pipeline Execution

### Agent 1: Narrative Strategist (video-narrative-strategist skill)

**Invoke:** Load the `video-narrative-strategist` skill.

**Input:** `ROLE` name
**Action:** Plan the 10-scene narrative arc using persona data from `hdim-customer-scenarios`.
**Output:** Story brief document

**Announce:** "Agent 1/5: Planning narrative arc for {ROLE} video..."

### Agent 2: Clinical Writer (video-clinical-writer skill)

**Invoke:** Load the `video-clinical-writer` skill.

**Input:** Story brief from Agent 1 + existing config from `landing-page-v0/remotion/src/compositions/role-videos/{ROLE}-video.config.ts`
**Action:** Write/refine all text fields (headline, captions, metrics, CTA copy).
**Output:** Complete `RoleStoryConfig` with all text fields filled

**Announce:** "Agent 2/5: Writing clinical script for {ROLE} video..."

### Agent 3: Medical Reviewer (video-medical-reviewer skill)

**Invoke:** Load the `video-medical-reviewer` skill.

**Input:** Complete config from Agent 2
**Action:** Validate every text field for clinical accuracy, HEDIS compliance, HIPAA safety.
**Output:** Annotated config with APPROVED/REVISED/REJECTED per field

**If any field is REJECTED:** Return to Agent 2 with rejection details. Max 3 iterations.

**Announce:** "Agent 3/5: Medical review for {ROLE} video..."

### Agent 4: Visual Designer (video-visual-designer skill)

**Invoke:** Load the `video-visual-designer` skill.

**Input:** Medically-validated config from Agent 3
**Action:** Optimize overlay types, positions, timing, Ken Burns motion for all 10 scenes.
**Output:** Final config with optimized `overlays[]`, `zoomLevel`, `panDirection`

**Announce:** "Agent 4/5: Designing visual overlays for {ROLE} video..."

### Agent 5: Pipeline Coordinator (video-pipeline-coordinator skill)

**Invoke:** Load the `video-pipeline-coordinator` skill.

**Input:** Final config from Agent 4
**Action:**
1. Write config to `{ROLE}-video.config.ts`
2. Run `npx tsc --noEmit` (type check)
3. If `SKIP_RENDER` is false: Run `npm run render:{ROLE}` and `npm run render:{ROLE}:short`
4. Generate YouTube/LinkedIn/Twitter distribution assets
5. Run QA checklist

**If `SKIP_RENDER` is true:** Skip rendering, only write config and generate distribution copy.

**Announce:** "Agent 5/5: Rendering and distributing {ROLE} video..."

## Completion Report

After all agents complete, output:

```markdown
## Video Pipeline Complete: {ROLE}

| Agent | Status | Duration |
|---|---|---|
| 1. Narrative Strategist | ✅ Complete | — |
| 2. Clinical Writer | ✅ Complete | — |
| 3. Medical Reviewer | ✅ Passed (N/N approved) | — |
| 4. Visual Designer | ✅ Complete | — |
| 5. Pipeline Coordinator | ✅ Rendered | — |

### Files
- Config: `landing-page-v0/remotion/src/compositions/role-videos/{ROLE}-video.config.ts`
- Video (default): `landing-page-v0/remotion/out/role-{ROLE}.mp4`
- Video (short): `landing-page-v0/remotion/out/role-{ROLE}-short.mp4`

### Distribution Assets
[YouTube title + description]
[LinkedIn post]
[Twitter thread]
```
```

**Step 3: Verify**

```bash
head -5 .claude/commands/video/render-role-video.md
```

Expected: YAML frontmatter with `name: video:render-role-video`

**Step 4: Commit**

```bash
git add .claude/commands/video/render-role-video.md
git commit -m "feat(commands): add /video:render-role-video — 5-agent orchestration command"
```

---

## Task 7: Integration Verification

**Files:**
- Read (verify): All 5 skills + 1 command created in Tasks 1-6

**Step 1: Verify all files exist**

```bash
ls -la .claude/skills/video-narrative-strategist/SKILL.md
ls -la .claude/skills/video-clinical-writer/SKILL.md
ls -la .claude/skills/video-medical-reviewer/SKILL.md
ls -la .claude/skills/video-visual-designer/SKILL.md
ls -la .claude/skills/video-pipeline-coordinator/SKILL.md
ls -la .claude/commands/video/render-role-video.md
```

Expected: All 6 files exist.

**Step 2: Verify skill frontmatter is parseable**

For each skill, verify the `---` YAML frontmatter block has `name:` and `description:`:

```bash
head -4 .claude/skills/video-narrative-strategist/SKILL.md
head -4 .claude/skills/video-clinical-writer/SKILL.md
head -4 .claude/skills/video-medical-reviewer/SKILL.md
head -4 .claude/skills/video-visual-designer/SKILL.md
head -4 .claude/skills/video-pipeline-coordinator/SKILL.md
head -4 .claude/commands/video/render-role-video.md
```

Expected: Each starts with `---` followed by `name:` on the next line.

**Step 3: Verify cross-references are consistent**

Check that each skill references the correct upstream and downstream agents:

| Skill | References (must exist) |
|---|---|
| `video-narrative-strategist` | `hdim-customer-scenarios`, `video-clinical-writer` |
| `video-clinical-writer` | `video-narrative-strategist`, `brand-voice`, `video-medical-reviewer` |
| `video-medical-reviewer` | `video-clinical-writer`, `video-visual-designer`, `hdim-customer-scenarios` |
| `video-visual-designer` | `video-medical-reviewer`, `video-pipeline-coordinator`, `frontend-design` |
| `video-pipeline-coordinator` | `video-visual-designer`, `content-optimizer` |
| `render-role-video` (command) | All 5 skills |

**Step 4: Test command invocation (dry run)**

Invoke `/video:render-role-video care-manager --skip-render` to verify:
1. The command loads
2. Each skill is referenced correctly
3. Config file path resolves

**Step 5: Final commit (tag the milestone)**

```bash
git add -A
git commit -m "feat(video-pipeline): complete 5-agent video production workflow

5 skills + 1 command for systematic video content production:
- video-narrative-strategist: Story arc planning
- video-clinical-writer: Brand-voice content writing
- video-medical-reviewer: Clinical accuracy validation
- video-visual-designer: Overlay optimization
- video-pipeline-coordinator: Render + distribution

Orchestrated by /video:render-role-video command.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Summary

| Task | Files | Estimated Lines | Purpose |
|---|---|---|---|
| 1 | `.claude/skills/video-narrative-strategist/SKILL.md` | ~180 | Story arc planning agent |
| 2 | `.claude/skills/video-clinical-writer/SKILL.md` | ~220 | Content writing with brand voice |
| 3 | `.claude/skills/video-medical-reviewer/SKILL.md` | ~200 | Clinical accuracy gate |
| 4 | `.claude/skills/video-visual-designer/SKILL.md` | ~230 | Overlay optimization |
| 5 | `.claude/skills/video-pipeline-coordinator/SKILL.md` | ~190 | Render + distribution |
| 6 | `.claude/commands/video/render-role-video.md` | ~120 | Orchestration command |
| 7 | (verification only) | 0 | Integration testing |

**Total:** ~1,140 lines across 6 new files + 7 commits.
