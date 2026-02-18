# YouTube Upload Guide - Care Gap Closure Videos

## Overview

This guide helps you upload the Care Gap Closure videos to YouTube and integrate YouTube embedding as an alternative to self-hosted videos.

## Why YouTube?

**Benefits:**
- ✅ YouTube's global CDN (faster than Vercel in some regions)
- ✅ Adaptive bitrate streaming (automatic quality adjustment)
- ✅ No bandwidth costs to Vercel
- ✅ Professional video player with captions support
- ✅ Video analytics (views, watch time, engagement)
- ✅ Embeddable on any platform (email, social media, etc.)

**Trade-offs:**
- ❌ YouTube branding (can be minimized)
- ❌ Requires YouTube account setup
- ❌ Videos must be public or unlisted

## Step 1: Prepare Videos for Upload

Both videos are already optimized and ready for YouTube upload:

**Files to upload:**
- `public/videos/care-gap-closure-30s.mp4` (1.9 MB, 30 seconds)
- `public/videos/care-gap-closure.mp4` (2.9 MB, 80 seconds)

## Step 2: Upload to YouTube

### Create YouTube Account (if needed)

1. Go to https://www.youtube.com
2. Sign in with Google account (or create one)
3. Click profile icon → "YouTube Studio"
4. Navigate to "Create" → "Upload videos"

### Upload 30-Second Video

**Video Details:**
- **Title:** "HDIM Care Gap Closure Demo - 30 Second Preview"
- **Description:**
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

- **Visibility:** **Unlisted** (not searchable, only accessible via link)
- **Category:** Science & Technology
- **Tags:** healthcare, FHIR, HEDIS, care gaps, quality measures, value-based care
- **Thumbnail:** Upload `public/videos/care-gap-closure-30s-thumb.png`

### Upload 80-Second Video

**Video Details:**
- **Title:** "HDIM Care Gap Closure Demo - Eleanor's Story"
- **Description:**
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

- **Visibility:** **Unlisted** (not searchable, only accessible via link)
- **Category:** Science & Technology
- **Tags:** healthcare, FHIR, HEDIS, care gaps, quality measures, Eleanor Anderson, breast cancer screening
- **Thumbnail:** Upload `public/videos/care-gap-closure-thumb.png`

## Step 3: Get YouTube Embed URLs

After uploading, YouTube provides embed URLs:

**Format:** `https://www.youtube.com/embed/VIDEO_ID`

**Example:**
- 30s video: `https://www.youtube.com/embed/abc123def45`
- 80s video: `https://www.youtube.com/embed/xyz789ghi01`

**Get the VIDEO_ID:**
1. Click on uploaded video in YouTube Studio
2. Copy the URL: `https://www.youtube.com/watch?v=VIDEO_ID`
3. The `VIDEO_ID` is the part after `v=`

## Step 4: Update VideoPlayer Component

Create an enhanced VideoPlayer that supports both self-hosted and YouTube videos.

**File:** `components/VideoPlayer.tsx`

```typescript
'use client';

import { useState } from 'react';
import { Play, X } from 'lucide-react';
import Image from 'next/image';

interface VideoPlayerProps {
  videoSrc: string;
  thumbnailSrc: string;
  title: string;
  description: string;
  youtubeId?: string; // Optional YouTube video ID
  preferYouTube?: boolean; // If true, use YouTube by default
}

export default function VideoPlayer({
  videoSrc,
  thumbnailSrc,
  title,
  description,
  youtubeId,
  preferYouTube = false
}: VideoPlayerProps) {
  const [isPlaying, setIsPlaying] = useState(false);
  const [useYouTube, setUseYouTube] = useState(preferYouTube && !!youtubeId);

  // Determine which video source to use
  const effectiveVideoSrc = useYouTube && youtubeId
    ? `https://www.youtube.com/embed/${youtubeId}?autoplay=1&rel=0&modestbranding=1`
    : videoSrc;

  // Modal overlay when video is playing
  if (isPlaying) {
    return (
      <div
        className="fixed inset-0 z-50 bg-black/90 flex items-center justify-center p-4"
        onClick={() => setIsPlaying(false)}
      >
        <div
          className="relative w-full max-w-6xl"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Close button */}
          <button
            onClick={() => setIsPlaying(false)}
            className="absolute -top-12 right-0 text-white hover:text-gray-300 transition-colors focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-black rounded"
            aria-label="Close video player"
          >
            <X className="w-8 h-8" />
          </button>

          {/* Video element - Self-hosted or YouTube */}
          {useYouTube && youtubeId ? (
            <iframe
              src={effectiveVideoSrc}
              className="w-full aspect-video rounded-lg shadow-2xl"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
              allowFullScreen
              title={title}
            />
          ) : (
            <video
              src={videoSrc}
              controls
              autoPlay
              className="w-full rounded-lg shadow-2xl"
              aria-label={`${title} - ${description}`}
            >
              <track kind="captions" />
              Your browser doesn't support video playback.
            </video>
          )}
        </div>
      </div>
    );
  }

  // Thumbnail with play button overlay
  return (
    <div
      className="relative rounded-2xl overflow-hidden shadow-2xl cursor-pointer group"
      onClick={() => setIsPlaying(true)}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          setIsPlaying(true);
        }
      }}
      aria-label={`Play video: ${title}`}
    >
      {/* Thumbnail image */}
      <Image
        src={thumbnailSrc}
        alt={title}
        width={1408}
        height={768}
        loading="lazy"
        className="w-full"
      />

      {/* Play button overlay */}
      <div className="absolute inset-0 flex items-center justify-center bg-black/30 group-hover:bg-black/40 transition-colors">
        <div
          className="w-20 h-20 bg-white rounded-full flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform"
          aria-hidden="true"
        >
          <Play className="w-8 h-8 text-primary ml-1" fill="currentColor" />
        </div>
      </div>

      {/* Video source indicator (optional) */}
      {youtubeId && (
        <div className="absolute top-4 right-4 bg-black/60 backdrop-blur-sm px-3 py-1 rounded-full text-xs text-white">
          {useYouTube ? 'YouTube' : 'Direct'}
        </div>
      )}

      {/* Accessibility: Screen reader description */}
      <span className="sr-only">{description}</span>
    </div>
  );
}
```

## Step 5: Update Page Components

### Home Page (80-second video)

**File:** `app/page.tsx` (line ~590)

```typescript
<VideoPlayer
  videoSrc="/videos/care-gap-closure.mp4"
  thumbnailSrc="/videos/care-gap-closure-thumb.png"
  title="Eleanor's Story - They Caught It Early"
  description="Watch HDIM close a care gap in real-time"
  youtubeId="YOUR_80S_VIDEO_ID"  // Add after YouTube upload
  preferYouTube={false}  // Use self-hosted by default, YouTube as fallback
/>
```

### Demo Page (30-second video)

**File:** `app/demo/page.tsx` (line ~590)

```typescript
<VideoPlayer
  videoSrc="/videos/care-gap-closure-30s.mp4"
  thumbnailSrc="/videos/care-gap-closure-30s-thumb.png"
  title="Care Gap Closure Demo - 30 Second Preview"
  description="Watch HDIM identify Eleanor Anderson's overdue mammogram screening and close the care gap with automated workflow"
  youtubeId="YOUR_30S_VIDEO_ID"  // Add after YouTube upload
  preferYouTube={false}  // Use self-hosted by default
/>
```

## Step 6: Environment Configuration (Optional)

Create environment variables to toggle between self-hosted and YouTube:

**File:** `.env.local`

```bash
# Video hosting preference
NEXT_PUBLIC_VIDEO_PREFER_YOUTUBE=false

# YouTube video IDs
NEXT_PUBLIC_YOUTUBE_CARE_GAP_30S=abc123def45
NEXT_PUBLIC_YOUTUBE_CARE_GAP_80S=xyz789ghi01
```

**Update VideoPlayer usage:**

```typescript
<VideoPlayer
  videoSrc="/videos/care-gap-closure.mp4"
  thumbnailSrc="/videos/care-gap-closure-thumb.png"
  title="Eleanor's Story - They Caught It Early"
  description="Watch HDIM close a care gap in real-time"
  youtubeId={process.env.NEXT_PUBLIC_YOUTUBE_CARE_GAP_80S}
  preferYouTube={process.env.NEXT_PUBLIC_VIDEO_PREFER_YOUTUBE === 'true'}
/>
```

## Step 7: YouTube Embed Optimization

### Recommended YouTube URL Parameters

```
?autoplay=1          # Start playing automatically when modal opens
&rel=0               # Don't show related videos from other channels
&modestbranding=1    # Minimal YouTube branding
&cc_load_policy=1    # Show captions by default (if available)
&iv_load_policy=3    # Hide video annotations
&playsinline=1       # Play inline on iOS (no fullscreen)
```

**Optimized embed URL:**
```
https://www.youtube.com/embed/VIDEO_ID?autoplay=1&rel=0&modestbranding=1&playsinline=1
```

## Step 8: Add Captions (Optional)

YouTube supports automatic captions, but you can add professional captions:

1. In YouTube Studio, select your video
2. Click "Subtitles" → "Add language"
3. Choose "English"
4. Click "Add" → "Upload file"
5. Upload `.srt` caption file

**Caption file locations:**
- `public/videos/care-gap-closure-30s.srt` (if created)
- `public/videos/care-gap-closure.srt` (if created)

## Step 9: Testing

### Test Self-Hosted Video
1. Set `preferYouTube={false}`
2. Visit page, click video thumbnail
3. Verify self-hosted video plays

### Test YouTube Video
1. Set `preferYouTube={true}`
2. Visit page, click video thumbnail
3. Verify YouTube iframe loads and plays

### Test Fallback
1. Provide `youtubeId` but keep `preferYouTube={false}`
2. Video should use self-hosted by default
3. If self-hosted fails, could add logic to fallback to YouTube

## Step 10: Analytics

### YouTube Analytics

Access video analytics in YouTube Studio:
- Views and watch time
- Average view duration
- Traffic sources
- Demographics
- Retention rate

### Vercel Analytics

Compare bandwidth usage before/after YouTube:
- Go to Vercel dashboard → Analytics
- Check bandwidth usage for `/videos/*` paths
- Monitor reduction in video traffic

## Deployment Checklist

After uploading to YouTube:

- [ ] Upload 30-second video to YouTube (unlisted)
- [ ] Upload 80-second video to YouTube (unlisted)
- [ ] Copy YouTube video IDs
- [ ] Update VideoPlayer component with YouTube support
- [ ] Update home page with YouTube ID
- [ ] Update demo page with YouTube ID
- [ ] Test self-hosted playback
- [ ] Test YouTube playback
- [ ] Verify mobile responsive
- [ ] Commit changes
- [ ] Deploy to Vercel production
- [ ] Verify production playback
- [ ] Monitor YouTube analytics

## Benefits Summary

**Self-hosted videos (current):**
- ✅ 1.9 MB / 2.9 MB (compressed)
- ✅ Instant loading
- ✅ No external dependencies
- ✅ Full control over player

**YouTube videos (additional option):**
- ✅ Global CDN (faster in some regions)
- ✅ Adaptive quality (480p, 720p, 1080p)
- ✅ Zero bandwidth cost
- ✅ Professional analytics
- ✅ Captions support
- ✅ Embeddable anywhere

**Hybrid approach (recommended):**
- Use self-hosted by default (fast, no branding)
- Offer YouTube as fallback or preference
- Let users choose based on connection speed
- Monitor which performs better in analytics

## Next Steps

1. **Create YouTube account** (if not already done)
2. **Upload both videos** following this guide
3. **Copy video IDs** from YouTube Studio
4. **Update VideoPlayer component** with YouTube support
5. **Test both versions** locally
6. **Deploy to production** with dual-source support
7. **Monitor analytics** to see which performs better

## Questions?

- YouTube upload issues: Check https://support.google.com/youtube
- Video encoding questions: See `VIDEO_COMPRESSION_SUMMARY.md`
- VideoPlayer component: See `components/VideoPlayer.tsx`
