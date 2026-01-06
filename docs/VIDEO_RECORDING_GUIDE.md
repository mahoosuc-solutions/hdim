# HDIM Demo Video Recording Guide

**Version**: 1.0
**Last Updated**: January 2026
**Audience**: Marketing, Sales Engineers, Demo Operators

---

## Overview

This guide provides step-by-step instructions for recording professional demo videos of the HDIM platform. Following these guidelines ensures consistent, high-quality videos that effectively communicate HDIM's value proposition.

---

## Equipment Setup

### Recommended Software

| Category | Recommended | Alternatives |
|----------|-------------|--------------|
| Screen Recording | OBS Studio | Loom, Screenflow, Camtasia |
| Video Editing | DaVinci Resolve (free) | Premiere Pro, Final Cut Pro |
| Audio | Audacity | Adobe Audition |
| Graphics | Canva | Photoshop, Figma |

### Hardware Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| Display | 1920x1080 | 2560x1440 |
| CPU | 4 cores | 8+ cores |
| RAM | 16 GB | 32 GB |
| Storage | 50 GB free | SSD with 100 GB free |
| Microphone | Built-in | USB condenser mic |

### Audio Setup

- **Microphone**: Use an external USB microphone for best quality
- **Environment**: Record in a quiet room with soft surfaces
- **Pop filter**: Use a pop filter to reduce plosives (p, b sounds)
- **Levels**: Target -12dB to -6dB peaks during test recording

---

## Recording Settings

### OBS Studio Configuration

```
Video:
- Base Resolution: 1920x1080
- Output Resolution: 1920x1080
- FPS: 30

Output:
- Recording Format: MP4
- Encoder: x264 or NVENC (GPU)
- Rate Control: CRF
- CRF Value: 18 (high quality)

Audio:
- Sample Rate: 48 kHz
- Channels: Stereo
- Audio Bitrate: 320 kbps
```

### Browser Setup

```
Chrome Settings:
- Zoom: 100%
- Font Size: Default
- Hardware Acceleration: Enabled
- Extensions: Disable all except essential

Display:
- Resolution: 1920x1080
- Scaling: 100%
- Full Screen: F11

Clear Before Recording:
- Cache and cookies
- History
- Download history
```

---

## Pre-Recording Checklist

### Environment Setup

- [ ] Demo services running (`docker compose ps` shows all healthy)
- [ ] Correct scenario loaded (`./demo-cli.sh status`)
- [ ] Baseline snapshot created (`./demo-cli.sh snapshot create "recording-baseline"`)
- [ ] Demo mode enabled in URL (`?demo=true`)
- [ ] Logged in as appropriate demo user

### Technical Checks

- [ ] Screen resolution is 1920x1080
- [ ] Browser zoom is 100%
- [ ] No browser extensions visible
- [ ] No desktop notifications enabled
- [ ] Recording software configured and tested
- [ ] Microphone levels tested
- [ ] Enough disk space (10+ GB)

### Content Preparation

- [ ] Script printed or on second monitor
- [ ] Practice run completed
- [ ] Timing verified against script checkpoints
- [ ] All demo data displays correctly
- [ ] Backup plan ready if issues occur

---

## Recording Process

### Step 1: Final Preparation (5 minutes before)

```bash
# Verify demo status
./demo-cli.sh status

# Restore to known good state
./demo-cli.sh snapshot restore "recording-baseline"

# Open demo URL
open "http://localhost:4200?demo=true"
```

### Step 2: Start Recording

1. Start OBS recording
2. Wait 3 seconds (will be trimmed in editing)
3. Begin narration with enthusiasm
4. Follow script timing precisely

### Step 3: During Recording

**Do**:
- Speak at a natural, conversational pace
- Pause 2-3 seconds on key metrics
- Click deliberately, not frantically
- Smile - it comes through in your voice
- Follow the exact script sequence

**Don't**:
- Rush through transitions
- Click before speaking about an element
- Mumble or trail off at sentence ends
- Over-explain technical details
- Deviate from the script without good reason

### Step 4: End Recording

1. Complete final call-to-action
2. Hold screen for 3 seconds
3. Stop recording
4. Immediately review the footage

---

## Script Execution Tips

### Opening (First 30 Seconds)

The opening is critical - it must hook viewers immediately.

**Strong Opening Example**:
> "What if you could evaluate quality measures for 5,000 patients in just 12 seconds? Today I'll show you how HDIM makes that possible."

**Weak Opening Example**:
> "Hi, I'm going to show you our quality measure software today."

### Pacing Guidelines

| Section | Target Time | Speaking Rate |
|---------|-------------|---------------|
| Opening hook | 0-30s | Energetic, fast |
| Problem statement | 30-60s | Measured, serious |
| Solution demo | 1:00-4:00 | Natural, confident |
| Results/ROI | 4:00-4:30 | Impactful, slower |
| Closing CTA | 4:30-5:00 | Clear, direct |

### Handling Key Metrics

When showing important numbers:

1. **Pause** - Give viewers time to read
2. **Verbalize** - Say the number out loud
3. **Contextualize** - Explain what it means
4. **Connect** - Link to business value

**Example**:
> *[Click to show results]*
> *[Pause 2 seconds]*
> "Notice we've identified 247 care gaps across our 5,000-patient population."
> *[Pause 1 second]*
> "Each of these represents a revenue opportunity and a chance to improve patient outcomes."

---

## Handling Mistakes

### Minor Mistakes (Keep Recording)

If you:
- Stumble on a word but recover quickly
- Miss a minor detail you can add later
- Experience brief UI lag

**Action**: Keep recording. These can be fixed in editing.

### Major Mistakes (Restart)

If you:
- Lose your place completely
- Demo shows incorrect data
- Significant technical error occurs

**Action**:
```bash
# Stop recording
# Restore snapshot
./demo-cli.sh snapshot restore "recording-baseline"

# Refresh browser, start again
```

### Recovery Phrases

Keep these ready if you need to recover mid-recording:

- "Let me show you that one more time..."
- "Here's where it gets really interesting..."
- "Notice what happens when we..."

---

## Post-Recording Process

### Immediate Review

1. Watch full recording within 5 minutes
2. Check for:
   - Audio quality throughout
   - All UI elements visible
   - No unexpected popups or errors
   - Timing matches script targets

### Quality Checklist

- [ ] Audio is clear with no background noise
- [ ] Screen is sharp and readable
- [ ] Mouse movements are smooth
- [ ] All clicks are visible and intentional
- [ ] Transitions are clean
- [ ] No technical glitches visible
- [ ] Timing is within target range

### File Management

```
recordings/
├── 2026-01-15/
│   ├── hedis-demo-take-1.mp4
│   ├── hedis-demo-take-1-notes.txt
│   ├── hedis-demo-take-2.mp4
│   └── hedis-demo-take-2-notes.txt
└── 2026-01-16/
    └── patient-journey-take-1.mp4
```

---

## Editing Guidelines

### Basic Edits

1. **Trim** - Remove start/end dead time
2. **Cuts** - Remove major mistakes
3. **Audio** - Normalize levels to -6dB
4. **Color** - Apply consistent color grade

### Graphics to Add

| Element | When | Duration |
|---------|------|----------|
| Logo intro | Start | 3-5 seconds |
| Title card | After intro | 3 seconds |
| Key metric callouts | During demo | 2-3 seconds |
| Contact CTA | End | 5 seconds |
| End card | Final | 5 seconds |

### Audio Enhancements

1. **Background music**: Subtle, corporate-style music at -20dB
2. **Transitions**: Soft whoosh sounds
3. **Emphasis**: Subtle chime on key metrics
4. **Normalize**: Even levels throughout

### Export Settings

```
Video:
- Codec: H.264
- Resolution: 1920x1080
- Frame Rate: 30 fps
- Bitrate: 10-15 Mbps

Audio:
- Codec: AAC
- Sample Rate: 48 kHz
- Bitrate: 320 kbps

Format:
- Container: MP4
- Web: Additional WebM version for embedding
```

---

## Distribution

### File Naming Convention

```
HDIM-[Scenario]-Demo-[Version]-[Date].mp4

Examples:
HDIM-HEDIS-Evaluation-Demo-v1-20260115.mp4
HDIM-Patient-Journey-Demo-v2-20260120.mp4
```

### Distribution Channels

| Channel | Format | Resolution | Notes |
|---------|--------|------------|-------|
| Website | MP4/WebM | 1080p | With captions |
| YouTube | MP4 | 1080p | Full description, chapters |
| LinkedIn | MP4 | 1080p | Native upload, subtitles |
| Email | Thumbnail + Link | - | Link to hosted video |
| Presentations | MP4 | 1080p | Embedded or linked |

### Accessibility

- [ ] Closed captions added
- [ ] Transcript available
- [ ] Audio descriptions for key visuals
- [ ] Thumbnail with text description

---

## Troubleshooting During Recording

### Issue: Demo Page Won't Load

```bash
# Check services
docker compose -f docker-compose.demo.yml ps

# Restart if needed
docker compose -f docker-compose.demo.yml restart gateway-service
```

### Issue: Demo Data Looks Wrong

```bash
# Restore to baseline
./demo-cli.sh snapshot restore "recording-baseline"

# Reload page
# F5 or Cmd+Shift+R
```

### Issue: Slow Performance

1. Close unnecessary applications
2. Disable browser extensions
3. Check Docker resource allocation
4. Increase RAM allocation to Docker

### Issue: Recording Stuttering

1. Lower recording quality temporarily
2. Use hardware encoding (NVENC/QuickSync)
3. Close resource-intensive applications
4. Record to SSD, not HDD

---

## Appendix: Quick Reference Card

Print this for quick reference during recordings:

```
┌────────────────────────────────────────────────┐
│           HDIM DEMO RECORDING CARD             │
├────────────────────────────────────────────────┤
│ BEFORE RECORDING:                              │
│ □ ./demo-cli.sh status                         │
│ □ ./demo-cli.sh snapshot create "baseline"    │
│ □ Browser: localhost:4200?demo=true           │
│ □ Resolution: 1920x1080                        │
│ □ Audio test: -12dB to -6dB                    │
├────────────────────────────────────────────────┤
│ DURING RECORDING:                              │
│ • Pause 2-3 sec on key metrics                 │
│ • Speak naturally, don't rush                  │
│ • Click deliberately                           │
│ • Follow script timing                         │
├────────────────────────────────────────────────┤
│ IF SOMETHING GOES WRONG:                       │
│ ./demo-cli.sh snapshot restore "baseline"     │
│ Refresh browser, restart                       │
├────────────────────────────────────────────────┤
│ AFTER RECORDING:                               │
│ □ Review immediately                           │
│ □ Check audio quality                          │
│ □ Save with notes                              │
└────────────────────────────────────────────────┘
```

---

**Last Updated**: January 2026
