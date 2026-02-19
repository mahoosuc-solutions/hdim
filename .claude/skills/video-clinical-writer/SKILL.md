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
- When invoked by the `/video:render-role-video` command (Agent 2 in the pipeline)

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

**CTA stat color patterns by role accent:**

| Role | Accent | Stat backgrounds |
|---|---|---|
| care-manager | `#10B981` | Emerald shades |
| cmo | `#3B82F6` | Blue shades |
| quality-analyst | `#8B5CF6` | Purple shades |
| provider | `#06B6D4` | Cyan shades |
| data-analyst | `#F59E0B` | Amber shades |
| admin | `#EF4444` | Red shades |
| ai-user | `#A855F7` | Purple shades |

**Example:**
```typescript
cta: {
  headline: 'Close Care Gaps in Seconds',
  highlightText: 'Not Weeks',
  stats: [
    { value: '8 sec', backgroundColor: 'rgba(16, 185, 129, 0.15)', borderColor: 'rgba(16, 185, 129, 0.4)', textColor: 'white', glowColor: 'rgba(16, 185, 129, 0.3)' },
    { value: '35-40%', backgroundColor: 'rgba(16, 185, 129, 0.15)', borderColor: 'rgba(16, 185, 129, 0.4)', textColor: 'white', glowColor: 'rgba(16, 185, 129, 0.3)' },
    { value: '99.9%', backgroundColor: 'rgba(16, 185, 129, 0.15)', borderColor: 'rgba(16, 185, 129, 0.4)', textColor: 'white', glowColor: 'rgba(16, 185, 129, 0.3)' },
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
- [ ] Conservative estimates provided where applicable

**Tone Checklist:**
- [ ] Practical and actionable (not inspirational)
- [ ] Describes what the USER does (not what the PLATFORM does)
- [ ] Honest about what the screenshot actually shows
- [ ] Value-first (not sales-first)

**Healthcare-Specific Checklist:**
- [ ] HEDIS measure codes are real (BCS-E, CDC, HbA1c, CBP, COL)
- [ ] Patient scenarios are clinically plausible
- [ ] Workflow actions match what this role actually does
- [ ] No fictional patient names in captions (use clinical descriptions instead)

## Output Format

The output is a complete `RoleStoryConfig` TypeScript object with all text fields filled. Output as a fenced code block that can be directly pasted into the config file:

```typescript
import { RoleStoryConfig } from '../../types/role-story.types';

export const {camelCase}Config: RoleStoryConfig = {
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
