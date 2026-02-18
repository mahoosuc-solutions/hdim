# YouTube Upload Checklist - aaron@mahoosuc.solutions

## Pre-Upload Preparation

**Videos Ready to Upload:**
- ✅ `public/videos/care-gap-closure-30s.mp4` (1.9 MB, 30 seconds)
- ✅ `public/videos/care-gap-closure.mp4` (2.9 MB, 80 seconds)

**Account:** aaron@mahoosuc.solutions

## Step 1: Access YouTube Studio

1. Open browser: https://studio.youtube.com
2. Sign in with: aaron@mahoosuc.solutions
3. Click "CREATE" → "Upload videos"

## Step 2: Upload 30-Second Video

**File:** `public/videos/care-gap-closure-30s.mp4`

### Video Details:

**Title:**
```
HDIM Care Gap Closure Demo - 30 Second Preview
```

**Description:**
```
Watch HDIM identify Eleanor Anderson's overdue mammogram screening and close the care gap with automated workflow in just 30 seconds.

HDIM is a FHIR-native platform for healthcare quality excellence. This demo shows:
- Real-time care gap detection
- Automated patient identification
- Quick action workflows
- Care gap closure tracking

Learn more: https://www.healthdatainmotion.com

#HealthIT #HEDIS #FHIR #QualityMeasures #ValueBasedCare
```

**Settings:**
- Visibility: **Unlisted** (not searchable, only accessible via link)
- Category: Science & Technology
- Comments: Disabled
- Age restriction: None

**Tags:**
```
healthcare, FHIR, HEDIS, care gaps, quality measures, value-based care, HDIM
```

**Thumbnail:**
Upload: `public/videos/care-gap-closure-30s-thumb.png`

### After Upload:

1. Wait for processing to complete
2. Copy the video URL: `https://www.youtube.com/watch?v=VIDEO_ID`
3. Extract VIDEO_ID (the part after `v=`)
4. **Save here:** `30s Video ID: ___________________________`

## Step 3: Upload 80-Second Video

**File:** `public/videos/care-gap-closure.mp4`

### Video Details:

**Title:**
```
HDIM Care Gap Closure Demo - Eleanor's Story
```

**Description:**
```
Watch HDIM close a care gap in real-time with Eleanor Anderson's mammogram screening.

This 80-second demo shows the complete care gap closure workflow:
1. Setup - Care Gap Manager Dashboard
2. Identification - Eleanor's 60-day overdue mammogram
3. Action - Schedule screening with quick actions
4. Impact - Statistics update, gap closed
5. Outcome - 8-second closure, ROI metrics

HDIM is a FHIR-native platform that helps healthcare organizations identify and close care gaps quickly.

Learn more: https://www.healthdatainmotion.com
Try interactive demo: https://www.healthdatainmotion.com/demo

#HealthIT #HEDIS #FHIR #QualityMeasures #CareGaps #ValueBasedCare #HealthcareAnalytics
```

**Settings:**
- Visibility: **Unlisted** (not searchable, only accessible via link)
- Category: Science & Technology
- Comments: Disabled
- Age restriction: None

**Tags:**
```
healthcare, FHIR, HEDIS, care gaps, quality measures, Eleanor Anderson, breast cancer screening, value-based care, HDIM
```

**Thumbnail:**
Upload: `public/videos/care-gap-closure-thumb.png`

### After Upload:

1. Wait for processing to complete
2. Copy the video URL: `https://www.youtube.com/watch?v=VIDEO_ID`
3. Extract VIDEO_ID (the part after `v=`)
4. **Save here:** `80s Video ID: ___________________________`

## Step 4: Update Landing Page Components

Once you have both VIDEO_IDs, I'll update these files:

### `app/demo/page.tsx` (30-second video)

```typescript
<VideoPlayer
  videoSrc="/videos/care-gap-closure-30s.mp4"
  thumbnailSrc="/videos/care-gap-closure-30s-thumb.png"
  title="Care Gap Closure Demo - 30 Second Preview"
  description="Watch HDIM identify Eleanor Anderson's overdue mammogram screening and close the care gap with automated workflow"
  youtubeId="YOUR_30S_VIDEO_ID"  // ← Add ID here
  preferYouTube={false}  // Self-hosted by default, YouTube as fallback
/>
```

### `app/page.tsx` (80-second video)

```typescript
<VideoPlayer
  videoSrc="/videos/care-gap-closure.mp4"
  thumbnailSrc="/videos/care-gap-closure-thumb.png"
  title="Eleanor's Story - They Caught It Early"
  description="Watch HDIM close a care gap in real-time"
  youtubeId="YOUR_80S_VIDEO_ID"  // ← Add ID here
  preferYouTube={false}  // Self-hosted by default, YouTube as fallback
/>
```

## Step 5: Test YouTube Playback

After updating the code:

1. Test locally: `npm run dev`
2. Visit http://localhost:3000 and http://localhost:3000/demo
3. Click video thumbnails
4. Videos should still play from self-hosted files (preferYouTube=false)
5. Change `preferYouTube={true}` temporarily to test YouTube embeds
6. Verify both sources work

## Step 6: Deploy to Production

```bash
# Commit changes
git add app/page.tsx app/demo/page.tsx
git commit -m "feat(videos): Add YouTube video IDs for dual-source playback

- Add YouTube IDs to VideoPlayer components
- Maintain self-hosted as default (preferYouTube=false)
- YouTube available as high-bandwidth alternative

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"

# Push and deploy
git push origin master
vercel --prod
```

## Verification Checklist

After deployment:

- [ ] 30s video uploaded to YouTube (unlisted)
- [ ] 80s video uploaded to YouTube (unlisted)
- [ ] Both VIDEO_IDs copied
- [ ] `app/page.tsx` updated with 80s VIDEO_ID
- [ ] `app/demo/page.tsx` updated with 30s VIDEO_ID
- [ ] Local testing passed (both self-hosted and YouTube)
- [ ] Production deployment successful
- [ ] Production verification (self-hosted videos still default)

## YouTube Studio Links

- **Upload videos:** https://studio.youtube.com/channel/UC[YOUR_CHANNEL_ID]/videos/upload
- **Video manager:** https://studio.youtube.com/channel/UC[YOUR_CHANNEL_ID]/videos
- **Analytics:** https://studio.youtube.com/channel/UC[YOUR_CHANNEL_ID]/analytics

## Notes

- **Unlisted videos** are not searchable but accessible via direct link
- YouTube processing takes 1-5 minutes per video
- Keep self-hosted as default (preferYouTube=false) for fastest loading
- YouTube serves as global CDN alternative for users in regions where Vercel is slower
- No bandwidth cost to Vercel when users choose YouTube

---

**Ready to upload?** Follow Steps 1-3 above, then provide me with the two VIDEO_IDs and I'll update the code.
