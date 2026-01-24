# Remotion Product Demo Video - Design Document

**Date:** January 24, 2026
**Author:** AI Assistant (Claude Code) + User Collaboration
**Status:** Design Complete - Ready for Implementation
**Video Duration:** 60s / 90s / 120s (modular)
**Purpose:** Create professional product demo video for HDIM landing page and YouTube

---

## Executive Summary

This document outlines the complete design for a Remotion-based product demo video that combines **product walkthrough** (Clinical Portal dashboard) with **explainer animation** (FHIR → CQL → Care Gaps workflow). The video will be rendered in three lengths (60s, 90s, 120s) for different distribution channels: social media, landing page, and YouTube.

**Key Design Decisions:**
- ✅ Problem → Solution → Demo narrative structure
- ✅ Hybrid approach: Real screenshots + animated React overlays
- ✅ Brand consistency: Matches landing page gradients, fonts, colors
- ✅ Modular composition: Export multiple lengths from shared components
- ✅ Text overlays only (no voiceover) with background music
- ✅ YouTube optimization: 1080p, H.264, proper metadata

---

## Architecture Overview

### Project Structure

```
landing-page-v0/
├── remotion/                          # New standalone Remotion project
│   ├── src/
│   │   ├── Root.tsx                  # Main composition registry
│   │   ├── Video.tsx                 # 90s default composition
│   │   ├── VideoShort.tsx            # 60s social media cut
│   │   ├── VideoLong.tsx             # 120s YouTube cut
│   │   ├── compositions/
│   │   │   ├── ProblemScene.tsx      # Scene 1: Problem (0-25s)
│   │   │   ├── SolutionScene.tsx     # Scene 2: Solution (25-50s)
│   │   │   └── DemoScene.tsx         # Scene 3: Dashboard Demo (50-90s)
│   │   ├── components/
│   │   │   ├── ScreenshotWithOverlay.tsx  # Hybrid screenshot component
│   │   │   ├── AnimatedMetric.tsx         # Counting number animations
│   │   │   ├── GlowHighlight.tsx          # Attention-grabbing highlights
│   │   │   ├── ArrowPointer.tsx           # Animated arrow annotations
│   │   │   └── BrandedContainer.tsx       # Gradient background wrapper
│   │   └── assets/
│   │       ├── screenshots/          # Symlink to ../../public/images/dashboard/
│   │       └── music/                # Background music tracks
│   ├── package.json
│   ├── remotion.config.ts
│   └── tsconfig.json
├── public/
│   └── videos/                        # Rendered video output
│       ├── hdim-demo-60s.mp4
│       ├── hdim-demo-90s.mp4
│       └── hdim-demo-120s.mp4
└── app/
    └── page.tsx                       # Landing page integration
```

### Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Remotion | 4.x | React-based video rendering framework |
| TypeScript | 5.x | Type safety for compositions |
| Tailwind CSS | 3.4.x | Reuse landing page styles |
| Framer Motion | (optional) | Advanced animation effects |
| React | 19.x | Component architecture |

### Why Standalone Project?

**Benefits of separate Remotion project:**
1. **Independent build process** - Remotion has its own Webpack bundler
2. **Faster iteration** - No Next.js rebuild needed during video editing
3. **Cleaner deployment** - Render videos once, commit MP4s to `/public/videos/`
4. **Avoid conflicts** - Next.js SSR vs Remotion's video rendering requirements
5. **Simpler CI/CD** - Don't render videos on every Vercel deploy

---

## Scene-by-Scene Breakdown

### Scene 1: Problem (0-25 seconds)

**Narrative Arc:** Establish the pain points HDIM solves

#### Frame 1: Hook (0-5s)
- **Visual:** Fade in from black to gradient background (primary blue #0066CC → teal #00CC88)
- **Text:** "Managing healthcare quality in 2026?" (large, centered)
- **Animation:** Text subtle pulse effect (scale 1.0 → 1.02 → 1.0)
- **Background:** Blurred screenshot of fragmented systems (15% opacity)
- **Music:** Intro builds (low energy, anticipatory)

#### Frame 2: Pain Point #1 - Data Silos (5-10s)
- **Visual:** Split screen layout
  - **Left:** Animated icon of scattered databases (bouncing between 5 systems)
  - **Right:** Text overlay
- **Text Primary:** "Data scattered across 15+ systems"
- **Text Secondary:** "EHRs, claims, labs, pharmacies..."
- **Animation:** Connecting lines appear and break (showing disconnection)
- **Transition In:** Slide from right
- **Color Theme:** Red tint overlay (danger/problem)

#### Frame 3: Pain Point #2 - Manual Work (10-15s)
- **Visual:** Split screen layout
  - **Left:** Calendar icon with spinning clock hands (fast spin = time wasted)
  - **Right:** Text overlay
- **Text Primary:** "Weeks of manual HEDIS calculations"
- **Text Secondary:** "Quality teams drowning in spreadsheets"
- **Background:** Faded screenshot of complex Excel spreadsheet (10% opacity)
- **Animation:** Clock hands accelerate, then stop abruptly
- **Transition In:** Slide from right
- **Color Theme:** Red/orange tint

#### Frame 4: Pain Point #3 - Missed Revenue (15-20s)
- **Visual:** Split screen layout
  - **Left:** Dollar sign with downward arrow (animated drop)
  - **Right:** Text overlay
- **Text Primary:** "Millions in quality bonuses... missed"
- **Text Secondary:** "Care gaps undetected until too late"
- **Animation:** Dollar sign drops and bounces (heavy physics)
- **Effect:** Red glow pulsing around metrics
- **Transition In:** Slide from right
- **Color Theme:** Dark red tint (maximum urgency)

#### Frame 5: Transition to Solution (20-25s)
- **Visual:** All three pain points shrink to corners (scale 1.0 → 0.3)
- **Text:** "There's a better way..." (center, fade in)
- **Animation:** Pain points fade to 30% opacity
- **Background:** Gradient brightens (darker blue → brighter teal)
- **Music:** Transition to uplifting chord progression
- **Prepare:** Stage for solution scene

---

### Scene 2: Solution (25-50 seconds)

**Narrative Arc:** Introduce HDIM as the solution with three core pillars

#### Frame 1: Introduce HDIM (25-30s)
- **Visual:** HDIM logo appears center screen (large scale)
- **Text Below:** "HDIM - The FHIR-Native Quality Platform"
- **Animation:** Logo pulses with subtle glow effect (green accent #00CC88)
- **Background:** Gradient shifts from teal → blue (reverse of problem scene)
- **Tagline Fade In:** "From fragmented to connected in seconds" (30s mark)
- **Music:** Uplifting melody begins

#### Frame 2: How It Works - Three Pillars (30-38s)
- **Visual:** Logo shrinks to top-left corner (scale 1.0 → 0.3)
- **Layout:** Three cards slide in from right (staggered timing)

**Card 1: FHIR Integration (32s)**
- **Icon:** Database with animated connection lines
- **Title:** "FHIR-Native Architecture"
- **Subtitle:** "Connect 47 systems automatically"
- **Animation:** Data dots flowing between systems along connection lines
- **Color:** Blue accent (#0066CC)
- **Entrance:** Slide from right + fade in (200ms duration)

**Card 2: CQL Engine (34s)**
- **Icon:** Lightning bolt (pulsing energy)
- **Title:** "Real-Time CQL Execution"
- **Subtitle:** "Evaluate quality measures instantly"
- **Animation:** Gauge filling up rapidly (0% → 100% in 1 second)
- **Color:** Teal accent (#00CC88)
- **Entrance:** Slide from right + fade in (150ms delay from Card 1)

**Card 3: Care Gap Detection (36s)**
- **Icon:** Heart with pulse line (animated heartbeat)
- **Title:** "AI-Powered Gap Detection"
- **Subtitle:** "Identify interventions with ROI"
- **Animation:** Alert badges appearing (green checkmarks, ROI tags)
- **Color:** Green accent (#00CC88)
- **Entrance:** Slide from right + fade in (150ms delay from Card 2)

#### Frame 3: Before/After Comparison (38-45s)
- **Visual:** Split screen vertical divide (sharp line down center)

**Left Side (BEFORE - Red Tint):**
- **Label:** "Traditional Approach"
- **Metric:** "3 months to identify gaps"
- **Animation:** Spinning loading icon (slow, frustrating)
- **Progress Bar:** Fills very slowly (10% over 3 seconds)
- **Color:** Red overlay (#EF4444, 30% opacity)

**Right Side (AFTER - Green Tint):**
- **Label:** "With HDIM"
- **Metric:** "2 seconds for real-time alerts"
- **Animation:** Progress bar fills instantly (0% → 100% in 0.5s)
- **Checkmark:** Large green checkmark appears (bounce effect)
- **Color:** Green overlay (#10B981, 30% opacity)

**Transition:** Vertical wipe from left to right (emphasize contrast)

#### Frame 4: Transition to Demo (45-50s)
- **Visual:** Cards fade out (opacity 1.0 → 0.0)
- **Text:** "See it in action..." (center, large)
- **Animation:** Smooth zoom effect pulling viewer forward
- **Background:** Gradient prepares for dashboard (fade to dark blue base)
- **Music:** Build to crescendo (anticipation for demo)

---

### Scene 3: Dashboard Demo (50-90 seconds)

**Narrative Arc:** Show real Clinical Portal dashboard with animated overlays

#### Frame 1: Provider Dashboard Overview (50-58s)

**Base Screenshot:** `main.png` (Provider Dashboard)
- **Animation:** Fade in + slight zoom (scale 1.0 → 1.05 for depth)
- **"LIVE" Badge:** Pulsing in top-right corner (green glow, 1s pulse cycle)

**Animated Overlays (Sequential):**

**52s: Highlight "20 PATIENTS TODAY"**
- **Effect:** Glowing border box around metric (3px gradient border)
- **Box Shadow:** `0 0 20px rgba(0, 204, 136, 0.5)`
- **Animation:** Fade in + scale (0.95 → 1.0 in 300ms)

**54s: Count Up Animation**
- **Effect:** Number animates from 0 → 20
- **Duration:** 1 second
- **Easing:** `ease-out-cubic`
- **Font:** Same as landing page headings (Inter, bold)

**55s: Highlight "76% QUALITY SCORE"**
- **Effect:** Glowing border box (same style as above)
- **Color:** Green accent gradient

**56s: Percentage Count Up**
- **Effect:** 0% → 76%
- **Duration:** 1.5 seconds
- **Visual:** Progress arc fills around number

**57s: Arrow to "Quick Actions"**
- **Effect:** Animated curved arrow (SVG path animation)
- **Text Label:** "One-click workflows" (tooltip style)
- **Animation:** Draw path + bounce entry
- **Color:** Accent teal (#00CC88)

#### Frame 2: Care Gap Management (58-68s)

**Base Screenshot:** `care-gaps.png` (Care Gap Management Dashboard)
- **Animation:** Smooth crossfade from main.png (500ms transition)
- **Pan Effect:** Slow left-to-right pan across screenshot (Ken Burns effect)
- **Scale:** 1.05 (slight zoom to create motion)

**Animated Overlays (Sequential):**

**60s: Highlight "13 total care gaps"**
- **Effect:** Glowing border around summary stat
- **Pulse:** 2 pulses (attention-grabbing)

**61s: Breakdown Badge**
- **Effect:** Badge slides in from top
- **Text:** "6 HIGH • 5 MEDIUM • 2 LOW"
- **Color Coding:** Red dots (high), orange (medium), green (low)
- **Animation:** Stagger each segment (150ms delay)

**63s: Focus on ROI Interventions**
- **Effect:** Zoom in to interventions section (scale 1.05 → 1.15)
- **Blur Background:** Rest of screenshot blurs (focus effect)

**64s: "8.2x ROI" Badge**
- **Effect:** Badge animates in with glow
- **Animation:** Scale from 0 → 1.2 → 1.0 (bounce)
- **Glow:** Pulsing gold glow (#FFC107)

**65s: "5.8x ROI" Badge**
- **Effect:** Same as above
- **Delay:** 300ms after first badge

**66s: "12.5x ROI" Badge**
- **Effect:** Same as above
- **Delay:** 300ms after second badge

**67s: Text Overlay**
- **Text:** "Prioritized by impact & cost-effectiveness"
- **Position:** Bottom third (subtitle style)
- **Background:** Semi-transparent dark overlay for readability

#### Frame 3: HEDIS Quality Measures (68-78s)

**Base Screenshot:** `measures.png` (HEDIS Measures Library)
- **Animation:** Smooth crossfade from care-gaps.png
- **Zoom:** Slow zoom in on measure cards (1.0 → 1.08)

**Animated Overlays (Sequential):**

**70s: Highlight BCS Card**
- **Effect:** Glowing border around BCS measure card
- **Color:** Blue gradient border

**71s: BCS Metric Animation**
- **Effect:** "74.2%" counts up from 0%
- **Star Rating:** Stars fill in sequentially (⭐⭐⭐⭐⭐)
- **Duration:** 1 second for number + 0.5s for each star

**73s: Highlight COL Card**
- **Effect:** Glowing border (same style)
- **Previous Highlight:** BCS fades to 50% opacity

**74s: COL Metric Animation**
- **Effect:** "72.5%" counts up
- **Star Rating:** ⭐⭐⭐⭐ (4 stars)

**75s: Text Overlay**
- **Text:** "6 active measures tracked in real-time"
- **Position:** Top third
- **Background:** Semi-transparent gradient overlay

**76s: CMS Star Ratings Badge**
- **Effect:** Badge slides in from right
- **Text:** "CMS Star Ratings Impact"
- **Icon:** Star icon (gold)

**77s: All Measures Glow**
- **Effect:** All 6 measure cards glow simultaneously
- **Animation:** Synchronized pulse (2 pulses)
- **Color:** Gradient rainbow effect across cards

#### Frame 4: Mobile Care Gaps (78-85s)

**Base Screenshot:** `mobile.png` (Care Gap List Entry)
- **Animation:** Zoom out from measures.png, then pan to mobile screenshot
- **Phone Mockup:** Screenshot appears inside phone frame (for context)
- **Orientation:** Portrait orientation (centered)

**Animated Overlays (Sequential):**

**80s: "HIGH" Urgency Badge Pulse**
- **Effect:** Red urgency badge pulses (scale 1.0 → 1.15 → 1.0)
- **Glow:** Red glow effect (#EF4444)
- **Cycles:** 3 pulses

**81s: Text Callout**
- **Text:** "Depression screening overdue"
- **Animation:** Slide in from right with arrow pointing to text
- **Style:** Tooltip/callout box

**82s: Countdown Timer**
- **Text:** "30 days" with clock icon
- **Animation:** Numbers tick down briefly (30 → 29 → 28, then stop)
- **Color:** Orange (warning)

**83s: Text Overlay**
- **Text:** "Full functionality on mobile devices"
- **Position:** Bottom third
- **Icon:** Mobile phone icon

**84s: Finger Tap Animation**
- **Effect:** Animated finger tap on action button
- **Animation:** Finger appears, taps button (ripple effect), disappears
- **Purpose:** Show interactivity

#### Frame 5: Call to Action (85-90s)

**Visual:** All screenshots fade out to gradient background

**85s: Background Transition**
- **Effect:** Fade to landing page gradient (blue → teal)
- **Animation:** Smooth crossfade (1 second)

**86s: HDIM Logo**
- **Position:** Center screen (large)
- **Animation:** Fade in + subtle glow

**Text Overlays (Sequential):**

**86s:**
- **Text:** "Close care gaps 40% faster"
- **Animation:** Fade in from bottom
- **Icon:** Checkmark (green)

**87s:**
- **Text:** "Improve HEDIS scores 12+ points"
- **Animation:** Fade in from bottom (150ms delay)
- **Icon:** Trending up arrow (green)

**88s:**
- **Text:** "Try the interactive demo"
- **Animation:** Fade in + scale (emphasize CTA)
- **Font Size:** Larger than previous text
- **Color:** White with subtle glow

**89s:**
- **URL:** "hdim-landing-page.vercel.app"
- **Animation:** Fade in
- **Style:** Sans-serif, clean, professional
- **Color:** Light teal (#00CC88)

**90s:**
- **Effect:** Fade to black (1 second)
- **Music:** Clean fade-out

---

## Component Specifications

### ScreenshotWithOverlay.tsx

**Purpose:** Hybrid component that displays real screenshots with animated React overlays

```typescript
interface ScreenshotWithOverlayProps {
  screenshot: string;           // Path to screenshot image
  overlays: Overlay[];          // Array of animated overlay elements
  zoomLevel?: number;           // 1.0 = no zoom, 1.1 = 10% zoom
  panDirection?: 'left' | 'right' | 'none';  // Ken Burns pan
  blurBackground?: boolean;     // Blur non-focused areas
}

interface Overlay {
  type: 'glow-highlight' | 'metric' | 'arrow' | 'badge' | 'text';
  startFrame: number;           // When to show overlay
  duration: number;             // How long to show (in frames)
  position: { x: number; y: number };  // Position on screenshot (%)
  props: any;                   // Type-specific props
}
```

**Features:**
- Smooth zoom using Remotion's `interpolate()`
- Ken Burns pan effect (slow left/right movement)
- Overlay timing based on current frame
- Responsive scaling for 1080p output
- Blur effect for focus areas

**Example Usage:**
```tsx
<ScreenshotWithOverlay
  screenshot="/screenshots/main.png"
  zoomLevel={1.05}
  panDirection="none"
  overlays={[
    {
      type: 'glow-highlight',
      startFrame: 60,
      duration: 120,
      position: { x: 20, y: 30 },
      props: { borderColor: 'green' }
    },
    {
      type: 'metric',
      startFrame: 90,
      duration: 60,
      position: { x: 20, y: 30 },
      props: { from: 0, to: 20, suffix: '' }
    }
  ]}
/>
```

### AnimatedMetric.tsx

**Purpose:** Count-up number animation with formatting

```typescript
interface AnimatedMetricProps {
  from: number;                 // Starting value
  to: number;                   // Ending value
  suffix?: string;              // '%', 'x', 'pts', etc.
  prefix?: string;              // '$', etc.
  duration: number;             // Animation duration (frames)
  delay?: number;               // Delay before starting (frames)
  decimals?: number;            // Number of decimal places
  fontSize?: string;            // Font size (Tailwind class)
  glowOnComplete?: boolean;     // Glow effect when reaching target
}
```

**Features:**
- Counting animation with easing (`ease-out-cubic`)
- Number formatting (commas for thousands)
- Suffix/prefix support
- Glow effect on final value
- Color customization
- Font size responsive

**Example Usage:**
```tsx
<AnimatedMetric
  from={0}
  to={76}
  suffix="%"
  duration={45}  // 1.5 seconds at 30fps
  fontSize="text-4xl"
  glowOnComplete={true}
/>
```

### GlowHighlight.tsx

**Purpose:** Attention-grabbing highlight box around UI elements

```typescript
interface GlowHighlightProps {
  width: number;                // Width in pixels
  height: number;               // Height in pixels
  borderColor?: 'green' | 'blue' | 'red';  // Border gradient color
  pulseCount?: number;          // Number of pulse animations
  borderRadius?: number;        // Border radius in pixels
}
```

**Features:**
- Animated border with gradient (primary → accent)
- Box shadow glow: `0 0 20px rgba(0, 204, 136, 0.5)`
- Pulse animation (scale 1.0 → 1.05 → 1.0)
- Customizable colors matching brand palette

**CSS Implementation:**
```css
.glow-highlight {
  border: 3px solid;
  border-image: linear-gradient(135deg, #0066CC, #00CC88) 1;
  box-shadow: 0 0 20px rgba(0, 204, 136, 0.5);
  border-radius: 8px;
  animation: pulse 1s ease-in-out 2;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}
```

### ArrowPointer.tsx

**Purpose:** Animated arrow with label pointing to features

```typescript
interface ArrowPointerProps {
  from: { x: number; y: number };   // Start position (%)
  to: { x: number; y: number };     // End position (%)
  label: string;                     // Text label
  curveIntensity?: number;           // How curved (0 = straight, 1 = very curved)
  drawDuration?: number;             // Path animation duration (frames)
}
```

**Features:**
- Animated curved arrow (SVG path animation)
- Text label in tooltip-style box
- Bounce effect on entry
- Color: Accent green (#00CC88)
- Bezier curve control

**SVG Path Animation:**
```tsx
// Animate path drawing using stroke-dashoffset
const pathLength = useRef(0);

<path
  d="M x1,y1 Q cx,cy x2,y2"  // Quadratic bezier curve
  stroke="#00CC88"
  strokeWidth={3}
  fill="none"
  strokeDasharray={pathLength.current}
  strokeDashoffset={interpolate(
    frame,
    [startFrame, startFrame + drawDuration],
    [pathLength.current, 0]
  )}
/>
```

### BrandedContainer.tsx

**Purpose:** Reusable gradient background matching landing page

```typescript
interface BrandedContainerProps {
  children: React.ReactNode;
  variant?: 'blue-teal' | 'dark-blue' | 'light';
  opacity?: number;             // Background opacity
}
```

**Gradient Variants:**
```css
/* blue-teal: Primary gradient (hero, CTA) */
background: linear-gradient(135deg, #0066CC 0%, #00CC88 100%);

/* dark-blue: Problem section */
background: linear-gradient(135deg, #003D7A 0%, #0066CC 100%);

/* light: Solution section */
background: linear-gradient(135deg, #00CC88 0%, #66D9B8 100%);
```

**Outputs exact CSS from Tailwind config to ensure consistency**

---

## Rendering Configuration

### remotion.config.ts

```typescript
import { Config } from '@remotion/cli/config';

Config.setCodec('h264');
Config.setVideoImageFormat('png');
Config.setPixelFormat('yuv420p');

export default {
  // Video output settings
  codec: 'h264',
  width: 1920,
  height: 1080,
  fps: 30,

  // Audio settings
  audioCodec: 'aac',
  audioBitrate: '192k',

  // Quality settings
  videoBitrate: '10M',
  enforceAudioTrack: true,

  // Compositions
  compositions: [
    {
      id: 'Main',
      component: 'Video',
      durationInFrames: 2700,  // 90 seconds at 30fps
      width: 1920,
      height: 1080,
      fps: 30,
      defaultProps: {},
      outputFile: 'hdim-demo-90s.mp4'
    },
    {
      id: 'Short',
      component: 'VideoShort',
      durationInFrames: 1800,  // 60 seconds at 30fps
      width: 1920,
      height: 1080,
      fps: 30,
      defaultProps: {},
      outputFile: 'hdim-demo-60s.mp4'
    },
    {
      id: 'Long',
      component: 'VideoLong',
      durationInFrames: 3600,  // 120 seconds at 30fps
      width: 1920,
      height: 1080,
      fps: 30,
      defaultProps: {},
      outputFile: 'hdim-demo-120s.mp4'
    }
  ],

  // Browser settings
  browser: 'chrome',
  chromiumOptions: {
    headless: true
  },

  // Performance
  concurrency: 4,  // Parallel rendering threads
  overwrite: true
};
```

### package.json Scripts

```json
{
  "name": "hdim-remotion-video",
  "version": "1.0.0",
  "scripts": {
    "dev": "remotion studio",
    "render:60s": "remotion render Main hdim-demo-60s.mp4 --composition=Short",
    "render:90s": "remotion render Main hdim-demo-90s.mp4 --composition=Main",
    "render:120s": "remotion render Main hdim-demo-120s.mp4 --composition=Long",
    "render:all": "npm run render:60s && npm run render:90s && npm run render:120s",
    "copy:videos": "cp out/*.mp4 ../public/videos/",
    "build": "npm run render:all && npm run copy:videos",
    "preview": "remotion preview"
  },
  "dependencies": {
    "@remotion/cli": "^4.0.0",
    "remotion": "^4.0.0",
    "react": "^19.2.3",
    "react-dom": "^19.2.3"
  },
  "devDependencies": {
    "@types/react": "^19.2.9",
    "typescript": "^5.0.0",
    "tailwindcss": "^3.4.3"
  }
}
```

### Asset Management

**Screenshot Symlink Strategy:**

```bash
# Create symlink to avoid duplicating large PNG files
cd remotion/src/assets/
ln -s ../../../public/images/dashboard screenshots

# Benefits:
# - No duplication of large files
# - Changes to screenshots automatically reflected in video
# - Single source of truth
```

**Directory Structure:**
```
remotion/src/assets/
├── screenshots/           # Symlink → ../../../public/images/dashboard/
│   ├── main.png          # Provider Dashboard (210KB)
│   ├── care-gaps.png     # Care Gap Management (148KB)
│   ├── measures.png      # HEDIS Measures (157KB)
│   └── mobile.png        # Mobile Care Gap (22KB)
└── music/
    └── background.mp3    # Licensed background music
```

---

## Modular Composition Strategy

### Video.tsx (90s Default)

```typescript
import { AbsoluteFill, Sequence } from 'remotion';
import { ProblemScene } from './compositions/ProblemScene';
import { SolutionScene } from './compositions/SolutionScene';
import { DemoScene } from './compositions/DemoScene';

export const Video: React.FC = () => {
  return (
    <AbsoluteFill>
      {/* Scene 1: Problem (0-25s = 0-750 frames) */}
      <Sequence from={0} durationInFrames={750}>
        <ProblemScene />
      </Sequence>

      {/* Scene 2: Solution (25-50s = 750-1500 frames) */}
      <Sequence from={750} durationInFrames={750}>
        <SolutionScene />
      </Sequence>

      {/* Scene 3: Demo (50-90s = 1500-2700 frames) */}
      <Sequence from={1500} durationInFrames={1200}>
        <DemoScene />
      </Sequence>
    </AbsoluteFill>
  );
};
```

### VideoShort.tsx (60s Social Media Cut)

```typescript
export const VideoShort: React.FC = () => {
  return (
    <AbsoluteFill>
      {/* Trimmed timing: 15s problem, 15s solution, 30s demo */}

      {/* Scene 1: Problem (0-15s = 0-450 frames) */}
      <Sequence from={0} durationInFrames={450}>
        <ProblemScene variant="short" />
      </Sequence>

      {/* Scene 2: Solution (15-30s = 450-900 frames) */}
      <Sequence from={450} durationInFrames={450}>
        <SolutionScene variant="short" />
      </Sequence>

      {/* Scene 3: Demo (30-60s = 900-1800 frames) */}
      <Sequence from={900} durationInFrames={900}>
        <DemoScene variant="short" />
      </Sequence>
    </AbsoluteFill>
  );
};
```

### VideoLong.tsx (120s YouTube Cut)

```typescript
export const VideoLong: React.FC = () => {
  return (
    <AbsoluteFill>
      {/* Extended timing: 30s problem, 30s solution, 60s demo */}

      {/* Scene 1: Problem (0-30s = 0-900 frames) */}
      <Sequence from={0} durationInFrames={900}>
        <ProblemScene variant="long" />
      </Sequence>

      {/* Scene 2: Solution (30-60s = 900-1800 frames) */}
      <Sequence from={900} durationInFrames={900}>
        <SolutionScene variant="long" />
      </Sequence>

      {/* Scene 3: Demo (60-120s = 1800-3600 frames) */}
      <Sequence from={1800} durationInFrames={1800}>
        <DemoScene variant="long" />
      </Sequence>
    </AbsoluteFill>
  );
};
```

**Variant Strategy:**
- `variant="short"`: Faster transitions, skip some overlays
- `variant="default"`: Balanced pacing (90s)
- `variant="long"`: More breathing room, additional annotations

---

## Landing Page Integration

### Option 1: Hero Section Video Background

**Location:** `app/page.tsx` - Hero section

```tsx
<section className="relative min-h-screen">
  {/* Video background with overlay */}
  <div className="absolute inset-0 opacity-30 z-0">
    <video
      autoPlay
      loop
      muted
      playsInline
      className="w-full h-full object-cover"
      poster="/videos/hdim-demo-poster.jpg"
    >
      <source src="/videos/hdim-demo-90s.mp4" type="video/mp4" />
    </video>
  </div>

  {/* Existing hero content on top */}
  <div className="relative z-10">
    {/* Current hero text, CTAs, dashboard preview card */}
  </div>
</section>
```

**Benefits:**
- ✅ Immediate visual impact
- ✅ Sets professional tone
- ✅ Continuous loop creates energy
- ✅ Doesn't interrupt user flow

**Considerations:**
- Ensure video is muted (autoplay requirements)
- Provide poster image for initial load
- Low opacity (20-30%) to not overwhelm text

### Option 2: Dashboard Preview Section (Recommended)

**Location:** `app/page.tsx` - Section 9 (Dashboard Preview)

```tsx
<section id="dashboard-preview" className="py-20">
  <div className="max-w-5xl mx-auto px-6">
    {/* Section header */}
    <div className="text-center mb-12">
      <span className="badge">Real-Time Command Center</span>
      <h2>See Your Quality Data Come Alive</h2>
      <p>Watch how HDIM transforms fragmented data into actionable insights</p>
    </div>

    {/* Video embed */}
    <div className="aspect-video rounded-xl overflow-hidden shadow-2xl">
      <iframe
        width="100%"
        height="100%"
        src="https://www.youtube.com/embed/[VIDEO_ID]?rel=0&modestbranding=1"
        title="HDIM Platform Demo"
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
        allowFullScreen
        className="w-full h-full"
      />
    </div>

    {/* Fallback: Show screenshots if video fails to load */}
    <noscript>
      <img
        src="/images/dashboard/main.png"
        alt="HDIM Provider Dashboard"
        className="rounded-xl shadow-2xl"
      />
    </noscript>
  </div>
</section>
```

**Benefits:**
- ✅ Primary placement for video content
- ✅ User intentionally engages
- ✅ Can track engagement metrics
- ✅ YouTube hosting (CDN, analytics)

**YouTube Embed Parameters:**
- `rel=0` - Don't show related videos at end
- `modestbranding=1` - Minimal YouTube branding
- `autoplay=0` - Don't autoplay (user choice)
- `cc_load_policy=1` - Show captions by default

### Option 3: Eleanor's Story Section

**Location:** `app/page.tsx` - Patient Stories section

```tsx
<div className="relative aspect-video rounded-xl overflow-hidden">
  {/* Replace static thumbnail with video */}
  <iframe
    width="100%"
    height="100%"
    src="https://www.youtube.com/embed/[VIDEO_ID]?start=78&end=85"
    // Start at 78s (mobile care gaps section)
    // End at 85s (before CTA)
    title="Patient Care Gap Detection - Eleanor's Story"
    allow="accelerometer; clipboard-write; encrypted-media"
    allowFullScreen
    className="w-full h-full"
  />

  {/* Overlay with Eleanor's quote */}
  <div className="absolute bottom-0 left-0 right-0 p-6 bg-gradient-to-t from-black/80">
    <p className="text-white text-lg">
      "They caught it early. That's why I'm still here."
    </p>
    <p className="text-white/80 text-sm">- Eleanor Martinez, Breast Cancer Survivor</p>
  </div>
</div>
```

**Benefits:**
- ✅ Contextual placement (patient story)
- ✅ Specific video timestamp (mobile care gaps)
- ✅ Emotional connection

**YouTube Timestamp Parameters:**
- `start=78` - Begin at Frame 4 (mobile care gaps)
- `end=85` - Stop before final CTA
- Loops this 7-second segment

### Performance Optimization

**Lazy Loading Strategy:**

```tsx
'use client';

import { useEffect, useRef, useState } from 'react';

const VideoSection = () => {
  const [loadVideo, setLoadVideo] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setLoadVideo(true);
          observer.disconnect();
        }
      },
      { threshold: 0.1 }
    );

    if (ref.current) {
      observer.observe(ref.current);
    }

    return () => observer.disconnect();
  }, []);

  return (
    <div ref={ref} className="aspect-video">
      {loadVideo ? (
        <iframe src="..." />
      ) : (
        <div className="bg-gray-200 animate-pulse" />
      )}
    </div>
  );
};
```

**Benefits:**
- ✅ Faster initial page load
- ✅ Reduced bandwidth for users who don't scroll
- ✅ Better Lighthouse score
- ✅ Improved Core Web Vitals

---

## YouTube Upload & Optimization

### Video Metadata

**Title:**
```
HDIM Platform Demo - FHIR-Native Healthcare Quality Management in 90 Seconds
```

**Description:**
```
See how HDIM transforms healthcare quality measurement from fragmented to connected in seconds.

⏱️ Timeline:
0:00 - The Problem: Data silos, manual work, missed revenue
0:25 - The Solution: FHIR-native, CQL-powered, AI-driven
0:50 - Live Demo: Real Clinical Portal dashboard walkthrough

✅ Key Features:
• FHIR-Native Architecture - Connect 47+ systems automatically
• Real-Time CQL Engine - Evaluate HEDIS measures instantly
• AI-Powered Care Gaps - Identify interventions with proven ROI

📊 Dashboard Highlights:
• Provider Dashboard with live quality metrics (76% score)
• Care Gap Management with ROI interventions (8.2x, 5.8x, 12.5x)
• HEDIS Quality Measures (BCS, COL, CBP, CDC, EED, SPC)
• Mobile care gap alerts with priority scoring

🔗 Try the interactive demo: https://hdim-landing-page.vercel.app

📧 Contact: sales@hdim.io
🌐 Website: https://hdim-landing-page.vercel.app

---

HDIM is the FHIR-native platform for healthcare quality measurement, care gap detection, and HEDIS evaluation. Built for health plans, ACOs, health systems, and Medicaid MCOs.

#HealthcareIT #HEDIS #FHIR #QualityMeasurement #CareGaps #ValueBasedCare #HealthTech #NCQA #StarRatings #HealthcareAnalytics
```

**Tags (Maximum 500 characters):**
```
HEDIS, FHIR, healthcare quality, care gaps, CQL, value-based care, quality measures, NCQA, Star Ratings, healthcare analytics, health plans, ACO, HEDIS evaluation, FHIR R4, clinical quality, healthcare data, interoperability, HL7, quality improvement, healthcare IT, health systems, Medicaid, Medicare, population health
```

**Category:** Science & Technology

**Thumbnail:**
- Export frame from 85s (HDIM logo + CTA text)
- Resolution: 1280x720 (16:9 aspect ratio)
- Include text: "FHIR-Native Quality Platform"
- Brand colors: Blue + Teal gradient
- High contrast for small preview

### Closed Captions (SRT File)

```srt
1
00:00:00,000 --> 00:00:05,000
Managing healthcare quality in 2026?

2
00:00:05,000 --> 00:00:10,000
Data scattered across 15+ systems

3
00:00:10,000 --> 00:00:15,000
Weeks of manual HEDIS calculations

4
00:00:15,000 --> 00:00:20,000
Millions in quality bonuses... missed

5
00:00:20,000 --> 00:00:25,000
There's a better way...

6
00:00:25,000 --> 00:00:30,000
HDIM - The FHIR-Native Quality Platform

7
00:00:30,000 --> 00:00:32,000
FHIR-Native Architecture

8
00:00:32,000 --> 00:00:34,000
Real-Time CQL Execution

9
00:00:36,000 --> 00:00:38,000
AI-Powered Gap Detection

10
00:00:38,000 --> 00:00:45,000
Traditional: 3 months | HDIM: 2 seconds

11
00:00:50,000 --> 00:00:58,000
Provider Dashboard - 20 patients today, 76% quality score

12
00:00:58,000 --> 00:01:08,000
Care Gap Management - 8.2x ROI interventions

13
00:01:08,000 --> 00:01:18,000
HEDIS Quality Measures - 6 active measures

14
00:01:18,000 --> 00:01:25,000
Mobile care gap alerts - HIGH priority

15
00:01:26,000 --> 00:01:28,000
Close care gaps 40% faster

16
00:01:28,000 --> 00:01:30,000
Try the interactive demo at hdim-landing-page.vercel.app
```

### Render Settings

**Export from Remotion:**

```bash
# High-quality YouTube render
remotion render Main hdim-demo-90s.mp4 \
  --codec=h264 \
  --video-bitrate=10M \
  --audio-bitrate=192k \
  --pixel-format=yuv420p \
  --image-format=png \
  --overwrite

# Expected output:
# - File size: ~70-90 MB
# - Resolution: 1920x1080
# - Frame rate: 30fps
# - Duration: 90 seconds
# - Codec: H.264
# - Audio: AAC 192kbps
```

**YouTube Recommended Upload Specs:**
- Container: MP4
- Video codec: H.264
- Frame rate: 30fps (native, not converted)
- Bitrate: 8-12 Mbps for 1080p
- Audio codec: AAC-LC
- Audio bitrate: 192+ kbps
- Aspect ratio: 16:9 (1920x1080)

### File Size Estimates

| Version | Duration | File Size (Estimated) | Bitrate |
|---------|----------|----------------------|---------|
| Short (60s) | 60 seconds | 45-60 MB | 10 Mbps |
| Main (90s) | 90 seconds | 70-90 MB | 10 Mbps |
| Long (120s) | 120 seconds | 95-120 MB | 10 Mbps |

**Upload Time Estimates (at 50 Mbps upload speed):**
- 60s video: ~10 seconds
- 90s video: ~15 seconds
- 120s video: ~20 seconds

---

## Analytics & Tracking

### YouTube Analytics (Built-in)

**Metrics to monitor:**
- View count
- Watch time (average % viewed)
- Audience retention graph (identify drop-off points)
- Traffic sources (landing page vs organic YouTube)
- Demographics (age, gender, geography)
- Playback locations (embedded vs YouTube.com)

**Key Performance Indicators:**
- **Target:** >50% average view duration (45+ seconds for 90s video)
- **Target:** >25% click-through rate on end screen CTA
- **Target:** >10% engagement rate (likes, comments, shares)

### Landing Page Integration Tracking

**Vercel Analytics:**

```tsx
'use client';

import { track } from '@vercel/analytics';

const VideoSection = () => {
  const handleVideoPlay = () => {
    track('video_played', {
      video: 'hdim-demo-90s',
      location: 'dashboard-section'
    });
  };

  const handleVideoComplete = () => {
    track('video_completed', {
      video: 'hdim-demo-90s',
      location: 'dashboard-section'
    });
  };

  return (
    <iframe
      src="https://www.youtube.com/embed/[VIDEO_ID]?enablejsapi=1"
      onLoad={() => {
        // YouTube IFrame API integration
        setupYouTubeTracking();
      }}
    />
  );
};
```

**Events to track:**
1. `video_loaded` - Video iframe loaded
2. `video_played` - User clicked play
3. `video_25_percent` - Watched 25% (Problem section complete)
4. `video_50_percent` - Watched 50% (Solution section complete)
5. `video_75_percent` - Watched 75% (Demo in progress)
6. `video_completed` - Watched 90%+ (Full video)
7. `cta_clicked_after_video` - Clicked "Try Demo" after video

**A/B Testing Opportunities:**
- Video placement (hero vs dashboard section)
- Video autoplay (on vs off)
- Video length (60s vs 90s vs 120s)
- Thumbnail image (different frames)

---

## Deployment Workflow

### Step-by-Step Process

#### 1. Set Up Remotion Project

```bash
# Navigate to landing page directory
cd /mnt/wdblack/dev/projects/hdim-master/landing-page-v0

# Create Remotion project
mkdir remotion
cd remotion

# Initialize with Remotion CLI
npx create-video --typescript
# Choose template: "Hello World"
# Choose package manager: npm

# Install additional dependencies
npm install tailwindcss framer-motion
```

#### 2. Create Symlink to Screenshots

```bash
# Create assets directory
mkdir -p src/assets

# Symlink to dashboard screenshots
cd src/assets
ln -s ../../../public/images/dashboard screenshots

# Verify symlink
ls -la screenshots/
# Should show: main.png, care-gaps.png, measures.png, mobile.png
```

#### 3. Build Compositions

```bash
# Create composition files
touch src/compositions/ProblemScene.tsx
touch src/compositions/SolutionScene.tsx
touch src/compositions/DemoScene.tsx

# Create component files
touch src/components/ScreenshotWithOverlay.tsx
touch src/components/AnimatedMetric.tsx
touch src/components/GlowHighlight.tsx
touch src/components/ArrowPointer.tsx
touch src/components/BrandedContainer.tsx
```

#### 4. Develop with Studio

```bash
# Start Remotion Studio (live preview)
npm run dev
# Opens http://localhost:3000

# Hot reload as you build components
# Preview animations in real-time
# Scrub timeline to test transitions
```

#### 5. Render Videos Locally

```bash
# Render all three versions
npm run render:all

# Or render individually:
npm run render:60s   # Social media cut
npm run render:90s   # Landing page default
npm run render:120s  # YouTube extended

# Rendering time estimate (on M1 Mac):
# - 60s video: ~5-8 minutes
# - 90s video: ~8-12 minutes
# - 120s video: ~12-15 minutes
```

#### 6. Copy Videos to Public Directory

```bash
# Copy rendered videos to landing page public folder
npm run copy:videos

# Verify output
ls -lh ../public/videos/
# hdim-demo-60s.mp4  (~50 MB)
# hdim-demo-90s.mp4  (~75 MB)
# hdim-demo-120s.mp4 (~100 MB)
```

#### 7. Upload to YouTube

**Manual Upload Process:**

1. Go to YouTube Studio: https://studio.youtube.com
2. Click "Create" → "Upload videos"
3. Select `hdim-demo-90s.mp4`
4. While uploading, fill in metadata:
   - Title: "HDIM Platform Demo - FHIR-Native Healthcare Quality Management"
   - Description: (see YouTube Metadata section above)
   - Thumbnail: Upload custom thumbnail (1280x720)
   - Playlist: Create "HDIM Platform Demos"
   - Tags: (see Tags section above)
   - Category: Science & Technology
   - Visibility: Public
5. Add to end screen:
   - CTA: "Try Interactive Demo" → https://hdim-landing-page.vercel.app
   - Subscribe button
   - Related video (upload 60s version as well)
6. Upload closed captions (SRT file)
7. Publish video
8. Copy video ID from URL: `https://www.youtube.com/watch?v=[VIDEO_ID]`

**Repeat for 60s and 120s versions**

#### 8. Update Landing Page

```bash
# Navigate back to landing page
cd /mnt/wdblack/dev/projects/hdim-master/landing-page-v0

# Edit app/page.tsx
# Add video embed to Dashboard Preview section (Option 2)
# Replace [VIDEO_ID] with actual YouTube video ID

git add app/page.tsx
git commit -m "feat(landing-page): Integrate Remotion product demo video"
```

#### 9. Deploy to Vercel

```bash
# Build locally to test
npm run build

# Verify video loads correctly
npm run start
# Open http://localhost:3000
# Scroll to Dashboard Preview section
# Verify YouTube video plays

# Deploy to production
vercel --prod

# Deployment URL: https://hdim-landing-page.vercel.app
```

#### 10. Update Documentation

```bash
# Update DEPLOYMENT.md
# Add section:
## Video Content
- Product demo video: 90 seconds
- YouTube URL: https://www.youtube.com/watch?v=[VIDEO_ID]
- Rendered with Remotion 4.x
- Placement: Dashboard Preview section

# Update PUNCH_LIST.md
# Mark video enhancement as complete:
## 🎬 Video Content ✅ COMPLETED
- [x] Product Demo Video created using Remotion
- [x] 60/90/120 second versions rendered
- [x] Uploaded to YouTube with metadata
- [x] Integrated into Dashboard Preview section

git add docs/DEPLOYMENT.md PUNCH_LIST.md
git commit -m "docs: Update deployment docs with video integration"
git push origin master
```

---

## Maintenance & Updates

### When Screenshots Change

**Scenario:** Clinical Portal UI is updated, new screenshots captured

**Update Process:**

```bash
# 1. Replace screenshots in source location
cp /docs/screenshots/quality-manager/new-dashboard.png \
   /public/images/dashboard/main.png

# 2. Remotion automatically picks up new screenshots (via symlink)
cd remotion

# 3. Re-render affected videos
npm run render:90s

# 4. Copy to public folder
npm run copy:videos

# 5. Re-upload to YouTube
# - Go to YouTube Studio
# - Find existing video
# - Click "Replace video" (keeps same video ID and URL)
# - Upload new hdim-demo-90s.mp4
# - Preserves views, comments, likes

# 6. No code changes needed (same YouTube embed ID)
```

**Benefits:**
- Zero landing page code changes
- Keeps same YouTube URL and analytics
- Preserves view count and engagement

### Localization (Future Enhancement)

**Text Overlay Externalization:**

```typescript
// src/data/text-overlays.ts
export const textOverlays = {
  en: {
    problem: {
      hook: "Managing healthcare quality in 2026?",
      painPoint1: "Data scattered across 15+ systems",
      painPoint2: "Weeks of manual HEDIS calculations",
      painPoint3: "Millions in quality bonuses... missed"
    },
    solution: {
      title: "HDIM - The FHIR-Native Quality Platform",
      pillar1: "FHIR-Native Architecture",
      pillar2: "Real-Time CQL Execution",
      pillar3: "AI-Powered Gap Detection"
    },
    // ... etc
  },
  es: {
    problem: {
      hook: "¿Gestionando la calidad de la atención médica en 2026?",
      // ... Spanish translations
    }
  }
};
```

**Render Multiple Language Versions:**

```bash
# Set language environment variable
export LANG=es
npm run render:90s-spanish

# Output: hdim-demo-90s-es.mp4
```

**Benefits:**
- Same video timing and animations
- Only text changes (easy to translate)
- Can target international markets

---

## Technical Specifications Summary

| Specification | Value |
|--------------|-------|
| **Video Resolution** | 1920x1080 (Full HD) |
| **Frame Rate** | 30 fps |
| **Video Codec** | H.264 |
| **Audio Codec** | AAC 192 kbps |
| **Video Bitrate** | 10 Mbps |
| **Pixel Format** | yuv420p |
| **Container** | MP4 |
| **Aspect Ratio** | 16:9 |
| **Color Space** | sRGB |
| **60s File Size** | ~45-60 MB |
| **90s File Size** | ~70-90 MB |
| **120s File Size** | ~95-120 MB |

---

## Timeline & Effort Estimate

### Development Phases

| Phase | Tasks | Estimated Time |
|-------|-------|----------------|
| **1. Setup** | Install Remotion, configure project, create symlinks | 1 hour |
| **2. Components** | Build 5 core components (Screenshot, Metric, Glow, Arrow, Container) | 3 hours |
| **3. Problem Scene** | Implement 5 frames with animations | 2 hours |
| **4. Solution Scene** | Implement 4 frames with three-pillar animation | 2 hours |
| **5. Demo Scene** | Implement 5 frames with screenshot overlays | 3 hours |
| **6. Audio** | Source background music, sync to animations | 1 hour |
| **7. Rendering** | Render all 3 versions, test playback | 1 hour |
| **8. YouTube** | Upload, metadata, captions, thumbnails | 1 hour |
| **9. Integration** | Add to landing page, test, deploy | 1 hour |
| **10. Documentation** | Update docs, commit changes | 30 min |

**Total Estimated Time:** 14.5 hours (2 working days)

**Rendering Time (Not Included):** 25-35 minutes for all 3 videos

---

## Success Criteria

### Video Quality Metrics

✅ **Visual Quality:**
- 1080p resolution with no compression artifacts
- Smooth 30fps playback with no dropped frames
- Brand colors accurate (#0066CC blue, #00CC88 teal)
- Text readable at all sizes (minimum 24px font on 1080p)
- Screenshots sharp and clear (no pixelation)

✅ **Animation Quality:**
- Smooth transitions (no jerky movements)
- Proper easing curves (no linear animations)
- Synchronized timing across elements
- Professional pacing (not too fast, not too slow)
- Attention-grabbing highlights without being distracting

✅ **Content Accuracy:**
- All metrics match real Clinical Portal data
- HEDIS measure names correct (BCS, COL, CBP, CDC, EED, SPC)
- ROI values accurate (8.2x, 5.8x, 12.5x)
- No placeholder or demo content visible

### User Engagement Metrics (Post-Launch)

✅ **YouTube Metrics:**
- Average view duration: >50% (45+ seconds for 90s video)
- Click-through rate: >5% to landing page
- Engagement rate: >2% (likes, comments, shares)

✅ **Landing Page Metrics:**
- Video play rate: >30% of visitors who scroll to section
- CTA clicks after video: >10% increase vs no video baseline
- Time on page: >20% increase with video present

✅ **Business Impact:**
- Demo requests: >15% increase post-video launch
- Conversion rate: >5% improvement on landing page

---

## Risk Mitigation

### Potential Issues & Solutions

| Risk | Mitigation Strategy |
|------|---------------------|
| **Rendering fails due to memory** | Use concurrency=1 for initial render, increase if stable |
| **Video file size too large** | Reduce bitrate to 8 Mbps, or render at 720p for social media |
| **YouTube upload blocked** | Ensure no copyrighted music (use licensed tracks) |
| **Slow page load with video** | Use lazy loading, only load when in viewport |
| **Video doesn't match brand** | Review with stakeholders before YouTube upload |
| **Screenshots outdated** | Create update workflow (re-render, re-upload) |
| **Music licensing issues** | Use Epidemic Sound or Artlist (licensed platforms) |
| **Accessibility concerns** | Provide closed captions (SRT file) |

---

## Appendix

### Recommended Music Tracks

**Epidemic Sound (Licensed):**
- "Uplifting Corporate" by Corporate Music Zone (2:30)
- "Tech Innovation" by Inspiring Audio (2:15)
- "Modern Business" by Professional Audio (2:45)

**Artlist (Licensed):**
- "Corporate Inspire" by Adi Goldstein (2:20)
- "Technology Future" by ASHUTOSH (2:10)
- "Healthcare Progress" by Philip Ayers (2:40)

**License Requirements:**
- YouTube monetization allowed
- Commercial use permitted
- Attribution not required

### Font Stack (From Landing Page)

```css
font-family:
  -apple-system,
  BlinkMacSystemFont,
  "Segoe UI",
  Roboto,
  "Helvetica Neue",
  Arial,
  sans-serif;
```

**Font Weights Used:**
- Regular: 400
- Medium: 500
- Semibold: 600
- Bold: 700

### Color Palette (From Landing Page)

```css
/* Primary Colors */
--primary: #0066CC;          /* Blue */
--primary-600: #0052A3;      /* Darker blue */
--primary-800: #003D7A;      /* Very dark blue */

/* Accent Colors */
--accent: #00CC88;           /* Teal/green */
--accent-light: #66D9B8;     /* Light teal */

/* Semantic Colors */
--success: #10B981;          /* Green */
--warning: #F59E0B;          /* Orange */
--error: #EF4444;            /* Red */

/* Neutral Colors */
--gray-50: #F9FAFB;
--gray-100: #F3F4F6;
--gray-900: #111827;
```

### Gradient Definitions

```css
/* Primary gradient (hero, CTA) */
background: linear-gradient(135deg, #0066CC 0%, #00CC88 100%);

/* Dark gradient (problem section) */
background: linear-gradient(135deg, #003D7A 0%, #0066CC 100%);

/* Light gradient (solution section) */
background: linear-gradient(135deg, #00CC88 0%, #66D9B8 100%);
```

---

## Next Steps

1. **Review & Approve Design** - Stakeholder sign-off on this document
2. **Set Up Remotion Project** - Initialize project structure
3. **Build Core Components** - Start with BrandedContainer, ScreenshotWithOverlay
4. **Develop Problem Scene** - First 25 seconds
5. **Develop Solution Scene** - Next 25 seconds
6. **Develop Demo Scene** - Final 40 seconds
7. **Render & Test** - Export all 3 versions, verify quality
8. **Upload to YouTube** - With proper metadata and captions
9. **Integrate to Landing Page** - Add video embed
10. **Deploy & Monitor** - Push to production, track engagement

---

**Document Version:** 1.0
**Last Updated:** January 24, 2026
**Status:** ✅ Design Complete - Ready for Implementation
**Estimated Completion:** 2 working days from start
