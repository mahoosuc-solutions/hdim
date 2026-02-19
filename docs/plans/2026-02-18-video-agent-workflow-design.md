# Design: 5 AI Agent Video Production Workflow

**Date:** 2026-02-18
**Status:** Design
**Goal:** Systematic, repeatable pipeline for producing high-quality Remotion marketing videos that showcase HDIM's ability to solve healthcare quality reporting problems and save time for patient outcomes.

---

## Problem

The current 14 role videos were generated in a single pass. Each video's config (`RoleStoryConfig`) contains narrative captions, headlines, metrics, overlays, and CTA copy — but this content was never:

1. **Strategically planned** against the buyer's decision journey
2. **Reviewed for clinical accuracy** (HEDIS measure codes, patient scenarios, statistics)
3. **Optimized for brand consistency** (data-driven claims with citations)
4. **Visually designed** with intentional overlay hierarchy and motion variety
5. **Prepared for distribution** with platform-specific marketing assets

The result: technically functional videos with content that could be sharper, more clinically credible, and more persuasive.

---

## Architecture: Sequential Pipeline with Quality Gates

Five specialized AI agents form a pipeline. Each operates on the same artifact — the `RoleStoryConfig` TypeScript config object — and passes a progressively refined version to the next agent.

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  1. Narrative    │ ──> │  2. Clinical     │ ──> │  3. Medical      │
│     Strategist   │     │     Writer       │     │     Reviewer     │
│                  │     │                  │     │                  │
│  Story arc +     │     │  Headlines,      │     │  Accuracy        │
│  scene plan      │     │  captions,       │     │  validation,     │
│                  │     │  metrics, CTA    │     │  HEDIS check     │
└─────────────────┘     └─────────────────┘     └────────┬─────────┘
                                                          │
                                                          ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  5. Pipeline     │ <── │  4. Visual       │ <── │  Validated       │
│     Coordinator  │     │     Designer     │     │  Config          │
│                  │     │                  │     │                  │
│  Render + QA +   │     │  Overlay         │     │                  │
│  distribution    │     │  positions,      │     │                  │
│                  │     │  timing, zoom    │     │                  │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

**Why sequential, not parallel:** Each agent's output depends on the previous agent's decisions. The narrative arc determines what captions to write; the captions determine what to highlight visually; the visual design determines what renders well on screen.

**Feedback loop:** If Agent 3 (Medical Reviewer) rejects a claim, the config returns to Agent 2 (Clinical Writer) for revision before proceeding.

---

## Agent Specifications

### Agent 1: Narrative Strategist

**Skill:** `.claude/skills/video-narrative-strategist/SKILL.md`
**Depends on:** `hdim-customer-scenarios` skill (persona data, pain points, workflows)

**Input:** Role type (e.g., `care-manager`)
**Output:** Story brief — a JSON document defining the 10-scene narrative arc

**Process:**
1. Load the target persona from `hdim-customer-scenarios` (goals, pain points, success metrics)
2. Map persona's top 3 pain points to a 4-act narrative structure:
   - **Act 1 (Scenes 1-2):** Status quo — the daily frustration
   - **Act 2 (Scenes 3-5):** Discovery — finding the problem in the platform
   - **Act 3 (Scenes 6-8):** Resolution — solving it with one workflow
   - **Act 4 (Scenes 9-10):** Impact — measurable outcome + system-wide effect
3. For each scene, specify:
   - Which platform page to show (must match existing screenshot)
   - What user action is depicted
   - What clinical problem this solves
   - What metric proves the value
4. Validate that the arc tells a complete story a buyer would recognize as their own experience

**Quality gate:** Every scene must answer "why does a [role] care about this screen?"

**Example output (care-manager, scene 4):**
```json
{
  "sceneNumber": 4,
  "page": "/care-gaps (Eleanor Anderson row)",
  "userAction": "Identifies highest-priority patient",
  "clinicalProblem": "Mammography screening overdue — BCS-E HEDIS measure at risk",
  "proofMetric": "60 days overdue, Gap closure: 8 seconds vs 30+ day manual process",
  "narrativeArc": "Act 2 — Discovery: The platform surfaces the most impactful gap first"
}
```

---

### Agent 2: Clinical Writer

**Skill:** `.claude/skills/video-clinical-writer/SKILL.md`
**Depends on:** `brand-voice` skill (data-driven principles, no vague claims) + Agent 1 output

**Input:** Story brief from Agent 1 + existing `RoleStoryConfig` as starting point
**Output:** Complete `RoleStoryConfig` with all text fields filled

**Writes these fields:**
| Field | Rules | Example |
|-------|-------|---------|
| `titleSlide.headline` | Persona's burning question (ends with ?) | "How Do I Close More Gaps Today?" |
| `titleSlide.subheadline` | Platform's promise (specific, measurable) | "From 73% manual gap closure to 8-second resolution" |
| `problemSlide.statement` | Cost of inaction (specific $ or time) | "Care managers spend 73% of their time on manual outreach..." |
| `problemSlide.metric` | Single shocking number | "73% of care gaps take >30 days to close manually" |
| `scenes[].narrativeCaption` | 12-18 words, clinically precise, active voice | "High-urgency filter isolates 9 gaps affecting Star Ratings — prioritized by financial impact" |
| `cta.headline` | Transformation promise | "Close Care Gaps in Seconds" |
| `cta.highlightText` | Accent-colored differentiator | "Not Weeks" |
| `cta.stats[]` | 3 proof points with real numbers | `[{value: "8 sec", label: "Gap Closure"}, ...]` |

**Brand voice enforcement rules:**
1. Every `metric` must cite a source (industry report, CMS data, or platform measurement)
2. No "up to X%" — use exact numbers or ranges ("35-40%", not "up to 40%")
3. No superlatives ("best", "leading", "revolutionary") — use comparative data instead
4. Every `narrativeCaption` must be actionable — describes what the user IS DOING, not what the platform IS
5. CTA stats must be independently verifiable

**Quality gate:** Run the `brand-voice` Voice Consistency Checklist against every text field.

---

### Agent 3: Medical Reviewer

**Skill:** `.claude/skills/video-medical-reviewer/SKILL.md`
**Depends on:** Healthcare domain knowledge + `hdim-customer-scenarios` workflows

**Input:** Complete `RoleStoryConfig` from Agent 2
**Output:** Annotated config with per-field validation status

**Review checklist:**

| Check | What to validate | Example issue |
|-------|-----------------|---------------|
| HEDIS accuracy | Measure codes match real CMS measures | "BCS-E" must be Breast Cancer Screening |
| Clinical plausibility | Patient scenarios make medical sense | "Age 63 mammography overdue" is plausible (40-74 age range) |
| Statistics accuracy | Numbers match known industry data | "73% manual" — needs CMS or NCQA citation |
| Terminology | Medical terms used correctly | "Care gap" vs "quality gap" vs "measure gap" |
| Regulatory | No HIPAA-sensitive patterns in marketing | No real patient names, MRNs, or dates of birth |
| CMS alignment | Measure descriptions match CMS definitions | Quality Measure descriptions match HEDIS MY2024 specs |
| Workflow realism | Described actions match real clinical workflows | Care managers DO close gaps; they don't write CQL |

**Per-field output:**
```json
{
  "field": "scenes[3].narrativeCaption",
  "status": "REVISED",
  "original": "Eleanor Anderson's mammography is 60 days overdue — highest BCS priority",
  "revised": "Mammography screening 60 days overdue — BCS-E measure, highest gap-closure priority",
  "reason": "Remove fictional patient name from marketing content (HIPAA pattern). Use clinical description instead."
}
```

**Quality gate:** Config cannot proceed to Agent 4 until all fields are `APPROVED` or `REVISED` (no `REJECTED` remaining).

---

### Agent 4: Visual Designer

**Skill:** `.claude/skills/video-visual-designer/SKILL.md`
**Depends on:** `frontend-design` skill (motion principles, visual hierarchy) + screenshot analysis

**Input:** Medically-validated config from Agent 3
**Output:** Config with optimized `overlays[]` arrays for all 10 scenes

**Design principles:**
1. **One focal point per scene** — never more than 2 overlays visible simultaneously
2. **Visual variety** — alternate between overlay types across scenes (glow → metric → badge → text)
3. **Timing progression** — overlays appear 30-60 frames into a scene (after the viewer orients to the screenshot)
4. **Color consistency** — `glow-highlight` border color follows the narrative arc:
   - Act 1-2: `blue` (neutral/discovery)
   - Act 3: `green` (success/resolution)
   - Act 4: `green` (impact/outcome)
   - Problem indicators: `red` (urgency)
5. **Position hierarchy** — important metrics go top-right; context badges go bottom-left; glow highlights go where the user's eye should land
6. **Ken Burns motion** — `zoomLevel` 1.02-1.06, `panDirection` alternates to avoid monotony

**Overlay selection guide:**
| Overlay type | When to use | Duration (frames) |
|---|---|---|
| `glow-highlight` | Draw attention to a specific UI element (button, row, filter) | 90-120 |
| `metric` | Animate a number that proves impact (count-up animation) | 120-150 |
| `badge` | Announce a state change ("Gap Closed!", "HEDIS BCS-E") | 90 |
| `text` | Provide clinical context not visible in the screenshot | 120 |

**Quality gate:** No two adjacent scenes should use the same overlay pattern. Every scene must have exactly 1-3 overlays (never 0, never 4+).

---

### Agent 5: Pipeline Coordinator

**Skill:** `.claude/skills/video-pipeline-coordinator/SKILL.md`
**Command:** `.claude/commands/render-role-video.md`
**Depends on:** `content-optimizer` skill (platform formatting) + render infrastructure

**Input:** Final validated config from Agent 4
**Output:** Rendered MP4s + distribution package

**Pipeline steps:**
1. **Write config** — Save to `landing-page-v0/remotion/src/compositions/role-videos/{role}-video.config.ts`
2. **Validate TypeScript** — `npx tsc --noEmit` to catch type errors
3. **Preview** — `npm run dev` in Remotion Studio for visual verification
4. **Render** — `npm run render:{role}` (default 90s) and `npm run render:{role}:short` (61s)
5. **Copy** — `npm run copy:videos` to landing page public directory
6. **Generate distribution assets** using `content-optimizer`:
   - YouTube: title (≤70 chars), description (500 words), 5 tags
   - LinkedIn: data-driven hook post (≤3000 chars)
   - Twitter/X: 5-tweet thread with video card
7. **Final QA checklist:**
   - [ ] Video duration matches expected (±1s)
   - [ ] File size reasonable (10-20 MB for 90s at 1080p)
   - [ ] No blank frames or missing screenshots
   - [ ] Brand voice compliance confirmed
   - [ ] All text fields readable at 1080p

---

## Invocation: `/render-role-video` Command

The command `/render-role-video {role}` orchestrates all 5 agents sequentially:

```
/render-role-video care-manager
```

**Flow:**
1. Invokes `video-narrative-strategist` with role → produces story brief
2. Invokes `video-clinical-writer` with story brief → produces config draft
3. Invokes `video-medical-reviewer` with config draft → produces validated config
4. Invokes `video-visual-designer` with validated config → produces final config
5. Invokes `video-pipeline-coordinator` with final config → renders + distributes

**Options:**
- `/render-role-video care-manager --skip-render` — produce config only, skip rendering
- `/render-role-video care-manager --from-step 3` — resume from Agent 3 (medical review)
- `/render-role-video all` — run pipeline for all 7 roles sequentially

---

## Files to Create

| File | Type | Lines (est.) | Purpose |
|------|------|-------------|---------|
| `.claude/skills/video-narrative-strategist/SKILL.md` | Skill | 150-200 | Story arc planning agent |
| `.claude/skills/video-clinical-writer/SKILL.md` | Skill | 200-250 | Content writing agent with brand voice |
| `.claude/skills/video-medical-reviewer/SKILL.md` | Skill | 150-200 | Clinical accuracy validation agent |
| `.claude/skills/video-visual-designer/SKILL.md` | Skill | 200-250 | Overlay optimization agent |
| `.claude/skills/video-pipeline-coordinator/SKILL.md` | Skill | 150-200 | Render + distribution orchestrator |
| `.claude/commands/render-role-video.md` | Command | 80-120 | Orchestration command |

**Total new code:** ~1,000-1,200 lines of skill definitions + command

---

## Integration with Existing Infrastructure

| Existing Asset | How Agents Use It |
|---|---|
| `RoleStoryConfig` type system | All agents read/write this format |
| `hdim-customer-scenarios` skill | Agent 1 loads persona data |
| `brand-voice` skill | Agent 2 enforces text quality |
| `frontend-design` skill | Agent 4 applies visual principles |
| `content-optimizer` skill | Agent 5 generates distribution assets |
| `npm run render:*` scripts | Agent 5 invokes for rendering |
| 70 screenshots in `public/screenshots/` | Agent 4 references for overlay positioning |
| Existing 7 config files | Starting point for Agent 2 refinement |

---

## Verification

1. **Unit test:** Each agent produces valid output when given sample input
2. **Integration test:** Full pipeline produces a renderable config (TypeScript compiles, Remotion renders)
3. **Quality test:** Rendered video reviewed against brand voice checklist
4. **Medical accuracy:** HEDIS measure codes validated against CMS HEDIS MY2024 Value Set Directory
5. **Visual quality:** No overlays obscure critical UI elements; text readable at 1080p
6. **Distribution:** LinkedIn post, Twitter thread, YouTube description all format correctly for their platforms
