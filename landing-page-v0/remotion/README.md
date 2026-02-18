# HDIM Product Demo Video - Remotion Project

Professional product demo video showcasing HDIM's FHIR-native healthcare quality platform.

## Video Specifications

- **Duration:** 60s / 90s / 120s (3 versions)
- **Resolution:** 1920x1080 (Full HD)
- **Frame Rate:** 30 fps
- **Format:** MP4 (H.264)
- **Style:** Problem → Solution → Demo narrative

## Quick Start

### Render Videos (Docker - Recommended for WSL2)

```bash
# Install dependencies
npm install

# Render all 3 versions via Docker
npm run docker:render

# Render specific version
npm run docker:render:90s  # 90-second version
npm run docker:render:60s  # 60-second version
npm run docker:render:120s # 120-second version

# Copy to landing page
npm run copy:videos
```

### Alternative: Node.js SSR Rendering

```bash
# Bypasses Docker, uses Remotion Node.js API directly
npm run ssr:render:90s  # 90-second version
npm run ssr:render:60s  # 60-second version
npm run ssr:render:120s # 120-second version
npm run ssr:render:all  # All 3 versions
```

### Alternative: CLI Rendering (May fail on WSL2)

```bash
# Direct CLI rendering (works on macOS/native Linux, may hang on WSL2)
npm run render:90s
npm run render:60s
npm run render:120s
npm run render:all
```

### Preview in Remotion Studio

```bash
# Start Remotion Studio (live preview with timeline scrubbing)
npm run dev
# Opens http://localhost:3002
```

## Project Structure

```
src/
├── Root.tsx                      # Composition registry
├── Video.tsx                     # 90s default composition
├── VideoShort.tsx                # 60s social media cut
├── VideoLong.tsx                 # 120s YouTube cut
├── compositions/
│   ├── ProblemScene.tsx          # Scene 1: Problem (0-25s)
│   ├── SolutionScene.tsx         # Scene 2: Solution (25-50s)
│   └── DemoScene.tsx             # Scene 3: Demo (50-90s)
├── components/
│   ├── BrandedContainer.tsx      # Gradient backgrounds
│   ├── AnimatedMetric.tsx        # Count-up number animations
│   ├── GlowHighlight.tsx         # Attention-grabbing highlights
│   └── ScreenshotWithOverlay.tsx # Hybrid screenshot + overlays
└── assets/
    └── screenshots/              # Symlink to ../../public/images/dashboard/
```

## Scene Breakdown

### Scene 1: Problem (0-25s)
**Narrative:** Establish healthcare quality pain points

- Frame 1 (0-5s): Hook - "Managing healthcare quality in 2026?"
- Frame 2 (5-10s): Data scattered across 15+ systems
- Frame 3 (10-15s): Weeks of manual HEDIS calculations
- Frame 4 (15-20s): Millions in quality bonuses missed
- Frame 5 (20-25s): Transition - "There's a better way..."

**Visuals:** Dark blue gradient, emoji icons (📊 ⏱️ 💸), white typography

### Scene 2: Solution (25-50s)
**Narrative:** Introduce HDIM as the solution

- Frame 1 (25-30s): HDIM logo with glow effect
- Frame 2 (30-38s): Three pillars (FHIR, CQL, AI Care Gaps)
- Frame 3 (38-45s): Before/After comparison (3 months vs 2 seconds)
- Frame 4 (45-50s): Transition to demo

**Visuals:** Light teal gradient, glassmorphism cards, progress bar animations

### Scene 3: Demo (50-90s)
**Narrative:** Real Clinical Portal dashboard walkthrough

- Frame 1 (50-58s): Provider Dashboard (20 patients, 76% quality score)
- Frame 2 (58-68s): Care Gap Management (13 gaps, 8.2x/5.8x/12.5x ROI)
- Frame 3 (68-78s): HEDIS Measures (6 active measures, BCS 74.2%)
- Frame 4 (78-85s): Mobile Care Gaps (HIGH urgency alerts)
- Frame 5 (85-90s): Call to Action (Benefits + URL)

**Visuals:** Real screenshots with animated overlays, zoom/pan effects

## Rendering Options

### 1. Docker Rendering (Recommended for WSL2)

**Why:** Isolates rendering from WSL2 Chrome Headless Shell issues

```bash
# Build Docker image (first time only)
npm run docker:build

# Render specific version
npm run docker:render:90s  # 90-second version
npm run docker:render:60s  # 60-second version
npm run docker:render:120s # 120-second version

# Render all versions
npm run docker:render

# Debug inside container
npm run docker:shell
```

**Outputs:** Videos appear in `out/` directory on host machine via volume mount.

**Performance:** ~8-12 minutes per 90s video on WSL2 (4 CPU cores, 8GB RAM)

### 2. Node.js SSR API (Programmatic)

**Why:** Bypasses CLI entirely, uses Remotion's Node.js API directly

```bash
npm run ssr:render:90s  # 90-second version
npm run ssr:render:60s  # 60-second version
npm run ssr:render:120s # 120-second version
npm run ssr:render:all  # All 3 versions
```

**Outputs:** Videos saved to `out/` directory

**Features:**
- Real-time progress tracking
- WSL2-friendly Chromium flags (`--no-sandbox`, `--disable-dev-shm-usage`)
- 50% CPU concurrency (prevents system overload)

### 3. GitHub Actions (Automated CI/CD)

**Why:** Zero local setup, free for public repos, automated on changes

**Manual Trigger:**
1. Go to GitHub → Actions → "Render Remotion Videos"
2. Click "Run workflow"
3. Select composition (60s/90s/120s/all)
4. Download artifacts when complete

**Auto Trigger:**
- **On push to main:** Renders when `remotion/**` files change
- **Scheduled:** Weekly on Sundays at 2 AM UTC
- **Auto-commit:** Videos automatically committed to `public/videos/` on main branch

**Outputs:**
- Artifacts downloadable for 30 days
- Videos auto-committed to repo (with `[skip ci]` to prevent loops)

### 4. CLI Rendering (May Fail on WSL2)

**Why:** Direct Remotion CLI (works on macOS/native Linux, may hang on WSL2)

```bash
npm run render:60s
npm run render:90s
npm run render:120s
npm run render:all
```

**Known Issue:** Hangs at "Getting composition" on WSL2 due to Chrome Headless Shell process forking limitations. Use Docker or SSR instead.

### Render Output

Videos are rendered to `out/` directory:
- `hdim-demo-60s.mp4` (~45-60 MB)
- `hdim-demo-90s.mp4` (~70-90 MB)
- `hdim-demo-120s.mp4` (~95-120 MB)

### Copy to Landing Page

```bash
# Copy all rendered videos to /public/videos/
npm run copy:videos
```

## Development Workflow

### 1. Preview in Remotion Studio

```bash
npm run dev
# Opens http://localhost:3000
# Use timeline scrubber to preview animations
# Hot reload on file changes
```

### 2. Adjust Timing

All timing is frame-based (30 fps):
- 1 second = 30 frames
- 0.5 seconds = 15 frames
- 2 seconds = 60 frames

Edit frame values in scene files:
```typescript
// Example: Show overlay at 5s for 2s duration
{
  startFrame: 150,  // 5s * 30fps = 150
  duration: 60,     // 2s * 30fps = 60
}
```

### 3. Update Screenshots

Screenshots are symlinked from `/public/images/dashboard/`:
```bash
# Update source screenshot
cp new-dashboard.png /public/images/dashboard/main.png

# Video automatically uses new screenshot (no code changes needed)
```

## Component Usage

### BrandedContainer

Gradient backgrounds matching landing page:

```typescript
<BrandedContainer variant="blue-teal" opacity={1}>
  {/* Your content */}
</BrandedContainer>
```

Variants: `blue-teal`, `dark-blue`, `light`

### AnimatedMetric

Count-up number animations:

```typescript
<AnimatedMetric
  from={0}
  to={76}
  suffix="%"
  duration={45}  // frames
  fontSize="3rem"
  glowOnComplete={true}
/>
```

### GlowHighlight

Attention-grabbing highlight boxes:

```typescript
<GlowHighlight
  width={400}
  height={120}
  borderColor="green"
  pulseCount={2}
  startFrame={60}
/>
```

### ScreenshotWithOverlay

Hybrid real screenshots + React overlays:

```typescript
<ScreenshotWithOverlay
  screenshot="/assets/screenshots/main.png"
  zoomLevel={1.05}
  panDirection="left"
  overlays={[
    {
      type: 'glow-highlight',
      startFrame: 60,
      duration: 120,
      position: { x: 20, y: 30 },
      props: { width: 400, height: 120 }
    }
  ]}
/>
```

## YouTube Upload

After rendering, upload to YouTube with metadata:

### Title
```
HDIM Platform Demo - FHIR-Native Healthcare Quality Management in 90 Seconds
```

### Description
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

Try the interactive demo: https://www.healthdatainmotion.com
```

### Tags
```
HEDIS, FHIR, healthcare quality, care gaps, CQL, value-based care, quality measures, NCQA, Star Ratings, healthcare analytics
```

### Closed Captions

Export SRT file from video or create manually for accessibility.

## Troubleshooting

### Render hangs on WSL2 ("Getting composition...")

**Symptom:** CLI rendering hangs indefinitely at "Getting composition" stage

**Cause:** WSL2 Chrome Headless Shell process forking limitations

**Solutions:**
1. **Use Docker rendering** (recommended):
   ```bash
   npm run docker:render:90s
   ```

2. **Use SSR rendering**:
   ```bash
   npm run ssr:render:90s
   ```

3. **Use GitHub Actions** (zero local setup):
   - Go to GitHub → Actions → "Render Remotion Videos" → Run workflow

4. **Use Remotion Studio** (manual):
   ```bash
   npm run dev
   # Render via UI at http://localhost:3002
   ```

### Screenshots not loading

Verify symlink:
```bash
ls -la src/assets/screenshots/
# Should show: screenshots -> ../../../public/images/dashboard
```

### Render fails in Docker

Check Docker resources:
```bash
docker stats hdim-remotion-renderer
# Should show: 4GB mem_limit, 2.0 CPUs
```

Increase if needed in `docker-compose.yml`:
```yaml
mem_limit: 8g  # Increase to 8GB
cpus: 4.0      # Increase to 4 CPUs
```

### SSR render fails with "Cannot find module"

Install SSR dependencies:
```bash
npm install @remotion/bundler @remotion/renderer
```

### Preview not updating

Clear Remotion cache:
```bash
rm -rf node_modules/.remotion
npm run dev
```

### GitHub Actions workflow fails

Check logs in Actions tab. Common issues:
- Missing `package-lock.json` (commit it to repo)
- Insufficient GitHub Actions minutes (check quota)
- Invalid composition name (must be 60s/90s/120s/all)

## Performance

- Rendering time (M1 Mac): ~8-12 minutes per 90s video
- File size: ~10 Mbps bitrate = ~75 MB for 90s
- Quality: 1080p H.264 suitable for YouTube

## Credits

- Design: Based on HDIM landing page design system
- Screenshots: Real Angular Clinical Portal production screenshots
- Framework: Remotion 4.0.409
- Built: January 24, 2026
