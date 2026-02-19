---
name: video:render-role-video
description: Orchestrate the 5-agent video production pipeline — narrative strategy, clinical writing, medical review, visual design, and rendering — to produce a high-quality Remotion marketing video for a specific HDIM role.
category: video
priority: high
---

# HDIM Video Production Pipeline

You are orchestrating the full 5-agent video production pipeline. Execute each agent in strict sequence. Do not skip agents unless `--from-step` is specified. Announce each agent before invoking it.

## Command Signature

```
/video:render-role-video <role> [--from-step <1-5>] [--skip-render] [--all]
```

## Parse Arguments

Read the user's arguments and set these variables:

| Variable | Default | Source |
|---|---|---|
| `ROLE` | (required) | First positional arg |
| `FROM_STEP` | `1` | `--from-step <N>` — resume from agent N |
| `SKIP_RENDER` | `false` | `--skip-render` — produce config only, skip rendering |
| `ALL_ROLES` | `false` | `--all` — run pipeline for all 7 roles sequentially |

## Valid Roles

```
care-manager, cmo, quality-analyst, provider, data-analyst, admin, ai-user
```

If `ROLE` is not in this list and `--all` is not set, abort with:
"Invalid role. Valid roles: care-manager, cmo, quality-analyst, provider, data-analyst, admin, ai-user"

If `--all` is set, iterate through all 7 roles in the order listed above.

## Key File Paths

| Purpose | Path |
|---|---|
| Config file | `landing-page-v0/remotion/src/compositions/role-videos/{ROLE}-video.config.ts` |
| Screenshots | `landing-page-v0/remotion/public/screenshots/{ROLE}/` |
| Type definitions | `landing-page-v0/remotion/src/types/role-story.types.ts` |
| Rendered output | `landing-page-v0/remotion/out/role-{ROLE}.mp4` |
| Render scripts | `landing-page-v0/remotion/package.json` |

## Pipeline Execution

Execute agents in order. If `--from-step N` is set, skip agents 1 through N-1.

---

### Agent 1: Narrative Strategist

**Announce:** "Agent 1/5: Planning narrative arc for {ROLE} video..."

**Invoke:** Load the `video-narrative-strategist` skill.

**Input:** ROLE name

**Action:**
1. Load the target persona from `hdim-customer-scenarios` skill
2. Review existing screenshots in `public/screenshots/{ROLE}/`
3. Map persona pain points to a 4-act, 10-scene narrative arc
4. Produce a story brief with scene-by-scene plan

**Output:** Story brief document (markdown table with 10 scenes)

**Quality gate:** Every scene answers "why does a {ROLE} care about this screen?"

---

### Agent 2: Clinical Writer

**Announce:** "Agent 2/5: Writing clinical script for {ROLE} video..."

**Invoke:** Load the `video-clinical-writer` skill.

**Input:** Story brief from Agent 1 + existing config file at `{ROLE}-video.config.ts`

**Action:**
1. Load `brand-voice` skill principles
2. Write/refine all text fields: headline, subheadline, problem statement, metric, 10 narrative captions, CTA copy
3. Run brand-voice Voice Consistency Checklist against every field
4. Output complete `RoleStoryConfig` with all text filled

**Output:** Complete TypeScript config object

**Quality gate:** Every text field passes brand-voice checklist (data-driven, active voice, specific numbers)

---

### Agent 3: Medical Reviewer

**Announce:** "Agent 3/5: Medical review for {ROLE} video..."

**Invoke:** Load the `video-medical-reviewer` skill.

**Input:** Complete config from Agent 2

**Action:**
1. Review every text field against 7 checks: HEDIS accuracy, clinical plausibility, statistics validation, terminology, HIPAA safety, CMS alignment, workflow realism
2. Mark each field: APPROVED, REVISED, or REJECTED
3. Apply revisions inline for REVISED fields

**Output:** Annotated config with review status per field

**Quality gate:** All fields must be APPROVED or REVISED (no REJECTED remaining).

**Feedback loop:** If any field is REJECTED, return to Agent 2 with rejection details. Max 3 iterations.

---

### Agent 4: Visual Designer

**Announce:** "Agent 4/5: Designing visual overlays for {ROLE} video..."

**Invoke:** Load the `video-visual-designer` skill.

**Input:** Medically-validated config from Agent 3

**Action:**
1. Load `frontend-design` skill principles
2. Review each screenshot to identify key UI elements
3. Design overlay types, positions, timing for all 10 scenes
4. Set Ken Burns zoom levels and pan directions
5. Validate visual variety and timing constraints

**Output:** Final config with optimized `overlays[]`, `zoomLevel`, `panDirection`

**Quality gate:** Visual variety checklist passes (no adjacent duplicate types, all timing valid)

---

### Agent 5: Pipeline Coordinator

**Announce:** "Agent 5/5: Rendering and distributing {ROLE} video..."

**Invoke:** Load the `video-pipeline-coordinator` skill.

**Input:** Final config from Agent 4

**Action:**
1. Write config to `{ROLE}-video.config.ts`
2. Run `npx tsc --noEmit` (TypeScript validation)
3. If `SKIP_RENDER` is false:
   - Run `npm run render:{ROLE}` (default ~90s variant)
   - Run `npm run render:{ROLE}:short` (short ~61s variant)
4. Generate distribution assets (YouTube, LinkedIn, Twitter/X)
5. Run final QA checklist (9 checks)

**Output:** Rendered MP4s + distribution copy + QA report

**If `SKIP_RENDER` is true:** Skip step 3, only write config and generate distribution copy.

---

## Completion Report

After all agents complete, output this summary:

```markdown
## Video Pipeline Complete: {ROLE}

| Agent | Status | Notes |
|---|---|---|
| 1. Narrative Strategist | ✅ | Story brief: [1-sentence summary] |
| 2. Clinical Writer | ✅ | [N] text fields written |
| 3. Medical Reviewer | ✅ | [N/N] approved, [N] revised |
| 4. Visual Designer | ✅ | [N] overlays across 10 scenes |
| 5. Pipeline Coordinator | ✅ | [QA result: N/9 passed] |

### Files
- Config: `landing-page-v0/remotion/src/compositions/role-videos/{ROLE}-video.config.ts`
- Video: `landing-page-v0/remotion/out/role-{ROLE}.mp4` ([X] MB, [Y]s)
- Short: `landing-page-v0/remotion/out/role-{ROLE}-short.mp4` ([X] MB, [Y]s)

### Distribution Assets
[YouTube title + description summary]
[LinkedIn post summary]
[Twitter thread summary]
```

## Error Handling

| Error | Action |
|---|---|
| Missing screenshots | Abort with: "Screenshots missing for {ROLE}. Run Playwright capture first." |
| TypeScript error | Fix config, re-validate, do not render until clean |
| Render failure | Check `remotion.config.ts` concurrency settings. Retry once. |
| Medical review REJECTED (3x) | Abort with: "Medical review failed 3 iterations. Flagged for human review." |
| Invalid role name | Abort with list of valid roles |
