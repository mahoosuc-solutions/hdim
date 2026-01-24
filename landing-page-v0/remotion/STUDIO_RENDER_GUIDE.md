# Remotion Studio Rendering Guide

**Status:** Remotion Studio is running at http://localhost:3002

## Why Use Studio Instead of CLI?

The CLI render is stuck at "Getting composition" due to WSL2/Linux browser integration issues. Remotion Studio uses a different rendering path that works on WSL2.

## Step-by-Step Rendering Instructions

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
