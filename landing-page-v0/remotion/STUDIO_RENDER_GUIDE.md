# Remotion Rendering Guide

**Last Updated:** January 24, 2026

## Rendering Options (in order of preference)

### 1. Docker-Based Rendering (Recommended for WSL2)

```bash
npm run docker:render:90s
```

**Pros:**
- ✅ Works reliably on WSL2
- ✅ Automated, reproducible
- ✅ No manual steps required
- ✅ Isolated from host system issues

**Outputs:** `out/hdim-demo-90s.mp4`

**Performance:** ~8-12 minutes for 90s video

---

### 2. GitHub Actions (Recommended for CI/CD)

**Trigger:** GitHub → Actions → "Render Remotion Videos" → Run workflow

**Pros:**
- ✅ Zero local setup
- ✅ Free for public repos (2,000 minutes/month)
- ✅ Automated on code changes
- ✅ Videos uploaded as artifacts
- ✅ Auto-commits to `public/videos/` on main branch

**Performance:** ~15-20 minutes for 90s video

**Outputs:**
- Downloadable artifacts (30-day retention)
- Auto-committed to repo

---

### 3. Node.js SSR API (Programmatic)

```bash
npm run ssr:render:90s
```

**Pros:**
- ✅ Direct rendering via Remotion Node.js API
- ✅ No CLI, no browser issues
- ✅ Real-time progress tracking
- ✅ Fastest local option

**Outputs:** `out/hdim-demo-90s.mp4`

**Performance:** ~8-10 minutes for 90s video

---

### 4. Remotion Studio (Manual, Development Only)

**Status:** Remotion Studio runs at http://localhost:3002

```bash
npm run dev  # Opens http://localhost:3002
```

**Pros:**
- ✅ Visual preview with timeline scrubbing
- ✅ Works on WSL2 (different rendering path than CLI)
- ✅ Good for testing and iteration

**Cons:**
- ❌ Manual, not automatable
- ❌ Requires browser interaction
- ❌ Not suitable for production workflows

**Use for:** Previewing animations, testing timing, visual debugging

---

## Why Not CLI Rendering?

**Issue:** `npm run render:90s` hangs at "Getting composition" on WSL2

**Cause:** WSL2 Chrome Headless Shell process forking limitations

**Solution:** Use Docker, SSR, GitHub Actions, or Studio instead (see above)

---

## Docker Rendering (Recommended)

### Quick Start

```bash
# Render 90-second version
npm run docker:render:90s

# Render all 3 versions
npm run docker:render

# Copy to landing page
npm run copy:videos
```

### How It Works

1. Docker builds image with Chromium + FFmpeg
2. Mounts `out/` directory to host
3. Runs `npm run render:90s` inside container
4. Videos appear in host `out/` directory

### Customization

Edit `docker-compose.yml` to adjust resources:

```yaml
mem_limit: 8g  # Increase to 8GB if needed
cpus: 4.0      # Increase to 4 CPUs if available
```

---

## GitHub Actions Rendering

### Manual Trigger

1. Go to GitHub repository
2. Click **Actions** tab
3. Select **"Render Remotion Videos"** workflow
4. Click **"Run workflow"**
5. Choose composition: `60s`, `90s`, `120s`, or `all`
6. Click **"Run workflow"** button
7. Wait 15-20 minutes
8. Download artifacts from workflow run

### Auto Trigger

**On push to main:**
- Automatically renders when `remotion/**` files change
- Commits videos to `public/videos/`

**Scheduled:**
- Runs every Sunday at 2 AM UTC
- Keeps videos fresh

### Artifacts

- Available for 30 days after workflow run
- Download from Actions tab → Workflow run → Artifacts section
- Videos auto-committed to repo on main branch

---

## Node.js SSR Rendering

### Quick Start

```bash
# Install dependencies (first time only)
npm install @remotion/bundler @remotion/renderer

# Render 90-second version
npm run ssr:render:90s

# Render all 3 versions
npm run ssr:render:all
```

### Progress Tracking

SSR rendering shows real-time progress:

```
🎬 Rendering 90s composition...
📦 Bundling project...
🎥 Rendering 2700 frames at 30 fps...
Progress: 10% (270/2700 frames)
Progress: 20% (540/2700 frames)
...
Progress: 100% (2700/2700 frames)
✅ Video saved: out/hdim-demo-90s.mp4
```

### WSL2 Optimizations

SSR script uses WSL2-friendly Chromium flags:
- `--no-sandbox` (bypasses sandboxing on WSL2)
- `--disable-setuid-sandbox` (no SUID required)
- `--disable-dev-shm-usage` (uses /tmp instead of /dev/shm)

---

## Remotion Studio Rendering (Manual)

### Step-by-Step Instructions

### 1. Open Remotion Studio

```
URL: http://localhost:3002
```

Open this URL in your browser (Chrome/Edge recommended).

### 2. Select Composition

You'll see 4 compositions:
- **Main** - 90 second landing page version ⭐ **Render this first**
- **Short** - 60 second social media version
- **Long** - 120 second YouTube version
- **Test** - 5 second test (ignore this)

**Click on "Main"** to open the 90-second composition.

### 3. Preview the Video

- Use the **playback controls** at the bottom to preview
- Drag the **timeline scrubber** to jump to any frame
- Verify all scenes look correct:
  - 0-25s: Problem scene (dark blue gradient, pain points)
  - 25-50s: Solution scene (teal gradient, HDIM pillars)
  - 50-90s: Demo scene (dashboard screenshots with overlays)

### 4. Start Rendering

**Option A: Using Studio's Render Button (Recommended)**

1. Click the **"Render"** button in the top-right corner
2. In the render dialog:
   - **Codec:** H.264 (already configured)
   - **Quality:** 80-90 (good balance)
   - **Concurrency:** Leave default or set to 50%
   - **Output location:** Choose where to save (default: `out/hdim-demo-90s.mp4`)
3. Click **"Render video"**
4. Monitor progress bar (should take 5-15 minutes for 90s video)

**Option B: Using Studio's Export to Cloud**

If local rendering still fails:
1. Click **"Export"** → **"Remotion Lambda"** (requires AWS setup)
2. Or export the composition data and render elsewhere

### 5. Expected Render Output

**File:** `out/hdim-demo-90s.mp4`
- **Duration:** 90 seconds (2700 frames @ 30fps)
- **Resolution:** 1920x1080 (Full HD)
- **Codec:** H.264 (yuv420p)
- **Size:** ~70-90 MB
- **Quality:** YouTube-ready

### 6. Render Other Versions

Repeat steps 2-4 for:
- **Short** composition → `out/hdim-demo-60s.mp4`
- **Long** composition → `out/hdim-demo-120s.mp4`

### 7. Copy Videos to Landing Page

After all videos are rendered:

```bash
npm run copy:videos
```

This copies all videos from `out/` to `../public/videos/`.

## Troubleshooting Studio Rendering

### Issue: Render button grayed out
**Solution:** Click on a composition first (Main/Short/Long)

### Issue: Render fails with timeout error
**Solutions:**
- Increase timeout in render settings (try 60000ms)
- Lower concurrency to 25% (reduces memory usage)
- Close other applications to free memory

### Issue: Browser crashes during render
**Solutions:**
- Try rendering shorter segments:
  - Render 0-1500 frames (first 50s)
  - Render 1500-2700 frames (last 40s)
  - Merge videos with ffmpeg
- Restart browser and try again

### Issue: Screenshots not loading
**Verify symlink:**
```bash
ls -la public/screenshots/
# Should show 4 PNG files (main, care-gaps, measures, mobile)
```

### Issue: Video quality too low
**Adjust codec settings in Studio:**
- Set CRF to 18 (higher quality, larger file)
- Or set bitrate to 15 Mbps

## Alternative: FFmpeg Approach

If Studio rendering also fails, you can export frames and encode with FFmpeg:

```bash
# Export frames (0-2700)
npx remotion still src/index.ts Main --frame=0 out/frames/frame-0000.png
# ... repeat for each frame (or use a script)

# Encode with FFmpeg
ffmpeg -framerate 30 -i out/frames/frame-%04d.png -c:v libx264 -pix_fmt yuv420p -crf 23 out/hdim-demo-90s.mp4
```

## Current Studio Status

✅ **Studio running:** http://localhost:3002
✅ **All compositions loaded:** Main (90s), Short (60s), Long (120s)
✅ **Assets ready:** 4 screenshots symlinked in public/
✅ **Code validated:** All 3 scenes compile successfully

**Next:** Open http://localhost:3002 in your browser and follow steps 2-7 above.

---

**Need help?** Check Remotion Studio docs: https://www.remotion.dev/docs/studio
