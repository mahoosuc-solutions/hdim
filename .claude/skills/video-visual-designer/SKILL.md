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
- When invoked by the `/video:render-role-video` command (Agent 4 in the pipeline)

## Required Context

1. **Validated config** from `video-medical-reviewer` (all text fields finalized)
2. **Screenshot files** in `landing-page-v0/remotion/public/screenshots/{role}/` (to identify what UI elements to highlight)
3. **Overlay type API** from `ScreenshotWithOverlay.tsx` (documented below)

## Overlay Types Available

The `ScreenshotWithOverlay` component renders 4 overlay types:

### `glow-highlight`
**Visual:** Pulsing gradient border box that draws attention to a specific UI element.
**Props:**
```typescript
{ width: number, height: number, borderColor: 'green' | 'blue' | 'red', pulseCount: number }
```
**Best for:** Interactive UI elements (buttons, rows, filters, cards)
**Position:** Must align with the actual UI element in the screenshot
**Typical size:** width 120-400px, height 40-200px depending on target element

### `metric`
**Visual:** Animated count-up number with glow effect on completion.
**Props:**
```typescript
{ from: number, to: number, decimals: number, suffix: string, duration: number, label: string, fontSize: string }
```
**Best for:** Numeric proof points (gap counts, percentages, time savings)
**Position:** Top-right quadrant preferred (eye naturally goes there for data)
**Note:** `decimals` must match precision of `from`/`to` values

### `badge`
**Visual:** Colored rounded pill with pop-in animation.
**Props:**
```typescript
{ text: string, backgroundColor: string, color: string, fontSize: string }
```
**Best for:** State changes ("Gap Closed!", "HEDIS BCS-E", "HIGH Urgency")
**Position:** Near the relevant UI element, slightly offset
**Typography:** fontSize typically `1.2rem` to `1.6rem`

### `text`
**Visual:** Semi-transparent dark pill with explanatory text.
**Props:**
```typescript
{ text: string, fontSize: string }
```
**Best for:** Clinical context not visible in the screenshot
**Position:** Edges or corners where it won't obscure important UI
**Typography:** fontSize typically `1.2rem` to `1.5rem`

## Design Principles

### 1. One Focal Point Per Scene
- Maximum 2 overlays visible at any moment (1 primary + 1 supporting)
- Maximum 3 overlays total per scene (they appear at different times)
- **Never** 0 overlays (every scene needs visual guidance)
- **Never** 4+ overlays (visual clutter destroys comprehension)

### 2. Visual Variety Across Scenes
No two adjacent scenes should use the same PRIMARY overlay type:

```
Scene 1:  glow-highlight (dashboard widget)
Scene 2:  badge          (urgency count)
Scene 3:  glow-highlight (filter button)      -- OK, scene 2 was badge
Scene 4:  text           (clinical context)
Scene 5:  glow-highlight (action button)
Scene 6:  metric         (time saved)
Scene 7:  badge          ("Gap Closed!")
Scene 8:  glow-highlight (patient record)
Scene 9:  text           (outreach context)
Scene 10: metric         (compliance improvement)
```

### 3. Timing Progression
Overlays appear AFTER the viewer has oriented to the screenshot:

| Overlay Role | `startFrame` | `duration` | Why |
|---|---|---|---|
| Primary overlay | 25-40 | 120-150 | Appears after ~1s orientation, stays for impact |
| Supporting overlay | 50-70 | 90-120 | Appears after primary establishes context |
| Late punch-line | 80-100 | 80-90 | The "aha!" moment — reveals the key insight |

**Never** start an overlay at frame 0 (jarring — screenshot hasn't fully appeared).
**Never** start after frame 120 (too late — scene is 195 frames total).
**Constraint:** `startFrame + duration` must be `<= 195` (scene duration).

### 4. Color Language
`glow-highlight` borderColor follows the narrative arc:

| Act | Scenes | Color | Meaning |
|---|---|---|---|
| Status Quo | 1-2 | `blue` | Neutral — showing the current state |
| Discovery | 3-5 | `blue` or `red` | `red` for urgency/problem indicators, `blue` for navigation |
| Resolution | 6-8 | `green` | Success — the problem is being solved |
| Impact | 9-10 | `green` | Confidence — measurable improvement |

Badge colors should match the semantic meaning:
- Red backgrounds (`rgba(239, 68, 68, 0.9)`) for urgency/problems
- Green backgrounds (`rgba(16, 185, 129, 0.9)`) for success/completion
- Blue backgrounds (`rgba(59, 130, 246, 0.9)`) for informational/neutral
- Role accent color for branded badges

### 5. Position Hierarchy
Standard positions (percentage-based, origin top-left):

| Purpose | x% | y% | Notes |
|---|---|---|---|
| Primary metric | 68-78 | 8-15 | Top-right — eye goes here for data |
| Status badge | 65-80 | 8-15 | Top-right — same zone as metrics |
| Context text | 4-15 | 75-85 | Bottom-left — supplementary info |
| Glow highlight | Varies | Varies | Must match actual UI element position in screenshot |
| Action badge | 45-60 | 40-55 | Center — for key moments ("Gap Closed!") |

**Critical rule:** Glow-highlight positions must correspond to actual UI elements in the screenshot. Review the screenshot to identify where buttons, rows, cards, and filters appear.

### 6. Ken Burns Motion
Each scene has `zoomLevel` (1.0-1.06) and `panDirection` ('left' | 'right' | 'none'):

- **Alternate pan direction** between scenes to avoid monotony
- **Zoom level 1.02-1.04** for overview screens (dashboards, tables)
- **Zoom level 1.04-1.06** for detail screens (patient records, forms, dialogs)
- **Zoom level 1.0** (no zoom) for dialog/modal screenshots

**Pan alternation pattern:**
```
Scene 1: left   Scene 2: right  Scene 3: none
Scene 4: left   Scene 5: right  Scene 6: none
Scene 7: left   Scene 8: right  Scene 9: left
Scene 10: none
```

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

## Validation Checklist

Before finalizing the visual design:

- [ ] Every scene has 1-3 overlays (never 0, never 4+)
- [ ] No two adjacent scenes share the same primary overlay type
- [ ] All `startFrame` values are between 20 and 120
- [ ] All `duration` values are between 60 and 160
- [ ] No overlay extends past scene duration: `startFrame + duration <= 195`
- [ ] `glow-highlight` borderColor follows the act-based color scheme
- [ ] `panDirection` alternates (no 3+ consecutive same direction)
- [ ] `zoomLevel` is between 1.0 and 1.06
- [ ] `metric` overlay `decimals` matches `from`/`to` precision
- [ ] `badge` text is <=25 characters (fits in pill at 1.4rem)
- [ ] No overlay obscures the NarrativeText bar (bottom 8% of screen — avoid y > 88%)

## Integration

- **Input from:** `video-medical-reviewer` (validated config with finalized text)
- **Output to:** `video-pipeline-coordinator` (final config ready to render)
- **Depends on:** `frontend-design` skill (motion and visual principles)
