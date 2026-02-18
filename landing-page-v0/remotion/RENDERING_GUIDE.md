# Care Gap Closure Video - Rendering Guide

Complete guide for rendering the Care Gap Closure Remotion video and integrating it into the landing page.

## Prerequisites

- Docker and Docker Compose installed
- Screenshots captured (see `SCREENSHOT_GUIDE.md`)
- Node.js 18+ installed (for local rendering)

## Rendering Options

### Option 1: Docker Rendering (Recommended for Production)

Docker rendering is recommended because it:
- Uses consistent environment across all systems
- Handles Node.js SSR issues on WSL2/Linux
- Produces high-quality H.264 output
- Avoids local dependency conflicts

**Steps:**

1. **Prepare screenshots** (if not already done):
   ```bash
   cd /mnt/wdblack/dev/projects/hdim-master/landing-page-v0/remotion
   npm run prepare:screenshots
   ```

2. **Build Docker image**:
   ```bash
   npm run docker:build
   ```

3. **Render the video** (80-second default version):
   ```bash
   npm run docker:render:caregap
   ```

4. **Or render the short version** (60 seconds):
   ```bash
   npm run docker:render:caregap:short
   ```

5. **Verify output**:
   ```bash
   ls -lh out/care-gap-closure.mp4
   ffprobe out/care-gap-closure.mp4
   ```

**Expected output:**
- File: `out/care-gap-closure.mp4`
- Duration: 80 seconds (default) or 60 seconds (short)
- Resolution: 1920x1080
- Codec: H.264
- Frame rate: 30 fps
- File size: ~15-25 MB

---

### Option 2: Local Rendering (For Development/Testing)

Local rendering is faster for iterating on animations but may have compatibility issues.

**Steps:**

1. **Install dependencies**:
   ```bash
   cd /mnt/wdblack/dev/projects/hdim-master/landing-page-v0/remotion
   npm install
   ```

2. **Render locally**:
   ```bash
   npm run render:caregap
   ```

3. **Or use SSR rendering** (if Docker has issues):
   ```bash
   npm run ssr:render:caregap
   ```

---

### Option 3: Remotion Studio (For Preview/Debugging)

Use Remotion Studio to preview animations and debug timing issues before rendering.

**Steps:**

1. **Start Remotion Studio**:
   ```bash
   cd /mnt/wdblack/dev/projects/hdim-master/landing-page-v0/remotion
   npm run dev
   ```

2. **Open browser**:
   - Navigate to `http://localhost:3000`
   - Select "CareGapClosure" composition from dropdown
   - Click play to preview

3. **Adjust timing** (if needed):
   - Edit scene components in `src/compositions/care-gap-closure/`
   - Modify overlay `startFrame` and `duration` values
   - Refresh Remotion Studio to see changes

4. **Render from Studio UI**:
   - Click "Render" button in top right
   - Select output format (MP4)
   - Click "Start Render"

---

## Post-Rendering Steps

### 1. Generate Video Thumbnail

After rendering, extract a thumbnail frame for the landing page:

```bash
cd /mnt/wdblack/dev/projects/hdim-master/landing-page-v0/remotion

# Extract frame at 3-second mark (Identification Scene - Eleanor's row highlighted)
ffmpeg -i out/care-gap-closure.mp4 -ss 00:00:03 -vframes 1 -q:v 2 \
  ../public/videos/care-gap-closure-thumb.png

# Verify thumbnail
ls -lh ../public/videos/care-gap-closure-thumb.png
```

**Expected output:**
- File: `public/videos/care-gap-closure-thumb.png`
- Resolution: 1920x1080
- Format: PNG
- File size: ~500 KB - 2 MB

### 2. Copy Video to Landing Page

```bash
cd /mnt/wdblack/dev/projects/hdim-master/landing-page-v0/remotion

# Copy rendered video
cp out/care-gap-closure.mp4 ../public/videos/

# Verify files
ls -lh ../public/videos/care-gap-closure*
```

### 3. Test Landing Page Locally

```bash
cd /mnt/wdblack/dev/projects/hdim-master/landing-page-v0

# Start Next.js dev server
npm run dev

# Open http://localhost:3000
```

**Verification checklist:**
- [ ] Scroll to "See It In Action" section
- [ ] Video thumbnail displays correctly
- [ ] Click thumbnail → Modal opens
- [ ] Video plays with controls
- [ ] Audio works (if applicable)
- [ ] Close button (X) dismisses modal
- [ ] Escape key closes modal
- [ ] Video is responsive on mobile (test with DevTools)

---

## Optimization

### Reduce File Size (Optional)

If the video file is too large (>20 MB), compress it:

```bash
cd /mnt/wdblack/dev/projects/hdim-master/landing-page-v0/public/videos

# Compress with FFmpeg (reduce bitrate)
ffmpeg -i care-gap-closure.mp4 -b:v 2M -maxrate 2M -bufsize 4M \
  care-gap-closure-compressed.mp4

# Compare file sizes
ls -lh care-gap-closure*.mp4

# Replace if smaller
mv care-gap-closure-compressed.mp4 care-gap-closure.mp4
```

### Optimize Thumbnail

```bash
cd /mnt/wdblack/dev/projects/hdim-master/landing-page-v0/public/videos

# Optimize PNG
pngquant --quality=80-95 care-gap-closure-thumb.png -o care-gap-closure-thumb-opt.png
mv care-gap-closure-thumb-opt.png care-gap-closure-thumb.png

# Or use ImageMagick
convert care-gap-closure-thumb.png -strip -quality 85 care-gap-closure-thumb-opt.png
```

---

## Deployment

### 1. Commit Changes

```bash
cd /mnt/wdblack/dev/projects/hdim-master/landing-page-v0

git add remotion/src/CareGapClosureVideo.tsx \
        remotion/src/compositions/care-gap-closure/ \
        remotion/src/Root.tsx \
        remotion/package.json \
        remotion/public/screenshots/care-gap-*.png \
        remotion/SCREENSHOT_GUIDE.md \
        remotion/RENDERING_GUIDE.md \
        public/videos/care-gap-closure.mp4 \
        public/videos/care-gap-closure-thumb.png \
        components/VideoPlayer.tsx \
        app/page.tsx

git commit -m "feat(landing-page): Add care gap closure demo video

- Create Remotion composition showing authentic care gap workflow
- Implement video player modal component
- Replace placeholder thumbnail with working video
- Add Eleanor's mammogram screening scenario (60s overdue → scheduled)
- 5 narrative scenes: Setup → Identification → Action → Impact → Outcome

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

### 2. Push to Repository

```bash
git push origin master
```

### 3. Deploy to Vercel

Vercel will automatically detect the push and deploy the updated landing page.

**Or manually trigger deployment:**
```bash
cd /mnt/wdblack/dev/projects/hdim-master/landing-page-v0
vercel --prod
```

### 4. Verify Production

After deployment:
- Navigate to `https://www.healthdatainmotion.com`
- Scroll to "See It In Action" section
- Click video thumbnail
- Verify video plays correctly

---

## Troubleshooting

### Problem: Docker rendering fails with "Cannot find module"

**Solution:**
```bash
cd /mnt/wdblack/dev/projects/hdim-master/landing-page-v0/remotion
rm -rf node_modules package-lock.json
npm install
npm run docker:build
npm run docker:render:caregap
```

### Problem: Video is too large (>50 MB)

**Solution:** Reduce bitrate or duration
```bash
# Reduce bitrate to 1.5 Mbps
ffmpeg -i out/care-gap-closure.mp4 -b:v 1.5M care-gap-closure-optimized.mp4

# Or use short variant (60s instead of 80s)
npm run docker:render:caregap:short
```

### Problem: Screenshot paths incorrect in Remotion

**Solution:** Verify file names match exactly
```bash
ls -la remotion/public/screenshots/care-gap-*.png

# Expected files:
# - care-gap-dashboard.png
# - care-gap-table-eleanor.png
# - care-gap-closure-dialog.png
# - care-gap-dashboard-updated.png
```

### Problem: Video doesn't play on landing page

**Solution:** Check file paths and permissions
```bash
ls -la public/videos/care-gap-closure*

# Verify files exist:
# - care-gap-closure.mp4
# - care-gap-closure-thumb.png

# Check Next.js static file serving
curl http://localhost:3000/videos/care-gap-closure.mp4 -I
```

### Problem: Modal doesn't close on Escape key

**Solution:** Add keyboard event listener to VideoPlayer component (already implemented).

---

## Performance Monitoring

After deployment, monitor video performance:

1. **Vercel Analytics**: Check video load times
2. **Lighthouse**: Run audit on landing page
3. **WebPageTest**: Measure video streaming performance

**Optimization targets:**
- Video load time: <3 seconds
- Thumbnail load time: <1 second
- Modal open time: <200ms

---

## Next Steps

After successful rendering and deployment:

1. ✅ Video working on production
2. 📊 Monitor user engagement metrics
3. 🎨 Create additional video variants (60s for social media)
4. 🔄 Update screenshots quarterly with latest UI improvements
5. 📝 Add closed captions for accessibility

---

_Last Updated: January 24, 2026_
