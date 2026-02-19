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
- When invoked by the `/video:render-role-video` command (Agent 5 in the pipeline)

## Pipeline Steps

### Step 1: Write Config File

Save the final `RoleStoryConfig` to:
```
landing-page-v0/remotion/src/compositions/role-videos/{role}-video.config.ts
```

The file must:
- Import `RoleStoryConfig` from `../../types/role-story.types`
- Export a named constant matching existing convention:

| Role | Export Name |
|---|---|
| care-manager | `careManagerConfig` |
| cmo | `cmoConfig` |
| quality-analyst | `qualityAnalystConfig` |
| provider | `providerConfig` |
| data-analyst | `dataAnalystConfig` |
| admin | `adminConfig` |
| ai-user | `aiUserConfig` |

### Step 2: TypeScript Validation

Run from the `landing-page-v0/remotion/` directory:

```bash
npx tsc --noEmit
```

Expected: No errors. If type errors appear, fix the config file before proceeding.

Common type errors and fixes:
- Missing field in `CTASlideConfig.stats[]` — each stat needs all 5 fields: `value`, `backgroundColor`, `borderColor`, `textColor`, `glowColor`
- `Overlay.type` not matching union — must be exactly `'glow-highlight' | 'metric' | 'text' | 'badge'`
- `panDirection` typo — must be exactly `'left' | 'right' | 'none'`
- `borderColor` in glow-highlight — must be exactly `'green' | 'blue' | 'red'`

### Step 3: Render Videos

From the `landing-page-v0/remotion/` directory:

```bash
# Default variant (~90s, ~2700 frames)
npm run render:{role}

# Short variant (~61s, ~1833 frames)
npm run render:{role}:short
```

**Render script mapping** (from package.json):

| Role | Default Script | Short Script |
|---|---|---|
| care-manager | `render:care-manager` | `render:care-manager:short` |
| cmo | `render:cmo` | `render:cmo:short` |
| quality-analyst | `render:quality-analyst` | `render:quality-analyst:short` |
| provider | `render:provider` | `render:provider:short` |
| data-analyst | `render:data-analyst` | `render:data-analyst:short` |
| admin | `render:admin` | `render:admin:short` |
| ai-user | `render:ai-user` | `render:ai-user:short` |

**Expected output files:**
- `out/role-{role}.mp4` — 10-20 MB, ~90 seconds
- `out/role-{role}-short.mp4` — 8-15 MB, ~61 seconds

**Render time:** 2-5 minutes per variant on WSL2 with 50% CPU concurrency.

### Step 4: Copy to Landing Page

```bash
npm run copy:videos
```

This copies all `out/*.mp4` files to `../public/videos/` for the landing page to serve.

### Step 5: Generate Distribution Assets

Using the `content-optimizer` skill principles, generate platform-specific assets:

#### YouTube
```
Title: [Role Title]: [CTA headline] | HDIM Platform Demo
  - Must be <=70 characters
  - Include role name and key action

Description: (400-600 words)
  - Paragraph 1: Problem statement from problemSlide.statement
  - Paragraph 2: What this video shows (the story arc summary)
  - Paragraph 3: Key results from cta.stats[]
  - Bullet list: 10 scene summaries from narrativeCaption values
  - CTA: "Request a demo at healthdatainmotion.com"
  - Tags line: 5 relevant tags

Tags: HEDIS, healthcare quality measures, [role-specific tag], care gaps, quality reporting
```

#### LinkedIn
```
Post format: Data-driven hook (brand-voice principles)
  - Line 1: Shocking metric from problemSlide.metric
  - Line 2-3: What the video shows (2 sentences from story arc)
  - Line 4: Key result from cta.stats[0]
  - Line 5: CTA with video link
  - Length: <=3000 characters
  - No hashtag spam — max 3 relevant hashtags at end
```

#### Twitter/X Thread
```
5-tweet thread:
  Tweet 1: Hook — problemSlide.metric + "Here's what happens next"
  Tweet 2: Act 1-2 summary (Status Quo + Discovery) — 2 sentences
  Tweet 3: Act 3 summary (Resolution) — the speed moment
  Tweet 4: Act 4 summary (Impact) — the proof metric
  Tweet 5: CTA with video link + key stats from cta
  Each tweet: <=280 characters
```

### Step 6: Final QA Checklist

Before declaring the video complete:

- [ ] **Duration:** Default variant is 88-92 seconds (target: ~90s)
- [ ] **Duration:** Short variant is 59-63 seconds (target: ~61s)
- [ ] **File size:** Default 10-20 MB, Short 8-15 MB (H.264 at 1080p)
- [ ] **No blank frames:** Video should not have any frames without a screenshot or slide
- [ ] **Text readability:** All overlay text readable at 1080p (minimum font ~1.2rem)
- [ ] **Brand voice:** All captions confirmed by clinical-writer with brand-voice checklist
- [ ] **Medical accuracy:** Confirmed PASS by video-medical-reviewer gate
- [ ] **Visual variety:** No two adjacent scenes share primary overlay type
- [ ] **Distribution assets:** YouTube, LinkedIn, Twitter/X copy generated and ready

## Output Report

After pipeline completion, output:

```markdown
## Pipeline Complete: [Role Title] Video

### Rendered Files
| File | Size | Duration |
|---|---|---|
| `out/role-{role}.mp4` | [X] MB | [Y]s |
| `out/role-{role}-short.mp4` | [X] MB | [Y]s |

### QA Results
- [X/9] checks passed
- Issues: [list any failures, or "None"]

### Distribution Assets

#### YouTube
**Title:** [generated title]
**Description:** [first 200 chars...]
**Tags:** [tag list]

#### LinkedIn
[Full post text]

#### Twitter/X
[5-tweet thread]

### Config File
`landing-page-v0/remotion/src/compositions/role-videos/{role}-video.config.ts`
```

## Integration

- **Input from:** `video-visual-designer` (final config with optimized overlays)
- **Output:** Rendered MP4s + distribution copy for 3 platforms
- **Depends on:** `content-optimizer` skill (platform formatting rules), Remotion render infrastructure
