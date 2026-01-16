# HDIM Platform - Video Production Guide

**Version**: 1.0  
**Last Updated**: January 2026  
**Purpose**: Complete guide for producing professional demo videos using the HDIM platform

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Quick Start - Video Production](#quick-start---video-production)
4. [Detailed Workflow](#detailed-workflow)
5. [Recording Options](#recording-options)
6. [Demo Orchestration with Screenshots](#demo-orchestration-with-screenshots)
7. [DevOps Agent Validation](#devops-agent-validation)
8. [Post-Production](#post-production)
9. [Troubleshooting](#troubleshooting)
10. [Best Practices](#best-practices)

---

## Overview

This guide covers the complete process for producing professional demo videos using the HDIM platform. The platform provides:

- **Automated Demo Orchestration** - Full demo execution with data seeding and validation
- **Phase-Based Screenshot Capture** - BEFORE, DURING, and AFTER screenshots
- **DevOps Agent Validation** - Ensures demo data authenticity
- **Screen Recording Integration** - Capture the entire demo process
- **Automated Workflows** - One-command execution

### Video Production Workflow

```
1. Environment Setup
   ↓
2. Start Demo Services
   ↓
3. Run Demo Orchestration (with DevOps Agent)
   ↓
4. Capture Phase-Based Screenshots
   ↓
5. Record Screen During Demo Execution
   ↓
6. Validate Data Authenticity
   ↓
7. Generate Final Video
```

---

## Prerequisites

### Required Software

- **Docker Desktop** - Container orchestration
- **Node.js 18+** - For screenshot capture scripts
- **Screen Recording Tool** - Choose one:
  - Windows: Game Bar (Windows + G) or OBS Studio
  - macOS: QuickTime Player or OBS Studio
  - Linux: OBS Studio or SimpleScreenRecorder
- **Terminal Access** - Bash, PowerShell, or similar

### System Requirements

- **RAM**: Minimum 8GB (16GB recommended)
- **Disk Space**: 10GB free for Docker images and data
- **Network**: Internet connection for initial setup

### Platform Access

- **Base URL**: `http://localhost:4200`
- **Admin Credentials**: `demo_admin@hdim.ai` / `demo123`

---

## Quick Start - Video Production

### Option 1: Automated Full Demo (Recommended)

```bash
# 1. Navigate to project root
cd /home/webemo-aaron/projects/hdim-master

# 2. Start Docker (if not running)
sudo service docker start  # WSL2/Linux
# OR open Docker Desktop on macOS/Windows

# 3. Run complete demo orchestration with screenshots
./scripts/run-demo-screenshots.sh

# 4. In another terminal, start screen recording
# Windows: Press Windows + G, then Windows + Alt + R
# macOS: Open QuickTime, File > New Screen Recording
# Linux: Start OBS Studio

# 5. Execute demo scenarios manually or via orchestrator
# The screenshots will be captured automatically at each phase
```

### Option 2: Manual Step-by-Step

```bash
# 1. Start services
docker compose -f demo/docker-compose.demo.yml up -d

# 2. Wait for services (60-90 seconds)
docker compose -f demo/docker-compose.demo.yml ps

# 3. Validate FHIR data (DevOps Agent)
curl -X POST http://localhost:8090/api/v1/devops/fhir-validation/validate

# 4. Start screen recording

# 5. Capture screenshots at each phase
node scripts/capture-screenshots.js --phase BEFORE --scenario hedis-evaluation
# ... execute demo scenario ...
node scripts/capture-screenshots.js --phase DURING --scenario hedis-evaluation
# ... wait for data to load ...
node scripts/capture-screenshots.js --phase AFTER --scenario hedis-evaluation

# 6. Stop screen recording
```

---

## Detailed Workflow

### Phase 1: Environment Preparation

#### 1.1 Verify Docker Status

```bash
# Check Docker is running
docker ps

# If not running, start it:
# WSL2/Linux:
sudo service docker start

# macOS/Windows:
# Open Docker Desktop application
```

#### 1.2 Pre-Flight Check

```bash
cd /home/webemo-aaron/projects/hdim-master
./scripts/pre-flight-check.sh
```

**Expected Output:**
```
✅ Docker is running
✅ Required ports are available
✅ Node.js is installed
✅ Pre-flight check passed!
```

#### 1.3 Prepare Recording Environment

**For Windows (WSL2):**
- Use Windows Game Bar (Windows + G)
- Or install OBS Studio on Windows host

**For macOS:**
- Use QuickTime Player (built-in)
- Or install OBS Studio

**For Linux:**
- Install OBS Studio: `sudo apt-get install obs-studio`
- Or use SimpleScreenRecorder

---

### Phase 2: Start Demo Services

#### 2.1 Start All Services

```bash
docker compose -f demo/docker-compose.demo.yml up -d
```

#### 2.2 Monitor Service Health

```bash
# Watch service startup
docker compose -f demo/docker-compose.demo.yml ps

# Check logs for any errors
docker compose -f demo/docker-compose.demo.yml logs -f
```

**Wait for all services to be healthy** (typically 60-90 seconds)

#### 2.3 Verify Services Are Ready

```bash
# Check clinical portal
curl http://localhost:4200

# Check DevOps Agent
curl http://localhost:8090/actuator/health

# Check FHIR service
curl http://localhost:8085/fhir/metadata
```

---

### Phase 3: Validate Demo Data

#### 3.1 Run FHIR Data Validation (DevOps Agent)

```bash
# Validate demo data authenticity
curl -X POST http://localhost:8090/api/v1/devops/fhir-validation/validate \
  -H "Content-Type: application/json" | jq
```

**Expected Response:**
```json
{
  "overallStatus": "PASS",
  "totalChecks": 25,
  "passedChecks": 23,
  "failedChecks": 0,
  "warningChecks": 2,
  "resourceCountChecks": [...],
  "codeSystemChecks": [...],
  "authenticityChecks": [...],
  "complianceChecks": [...],
  "relationshipChecks": [...]
}
```

#### 3.2 Verify Data Seeding

```bash
# Check patient count
curl "http://localhost:8085/fhir/Patient?_summary=count" | jq '.total'

# Should show at least 50 patients
```

---

### Phase 4: Prepare Screen Recording

#### 4.1 Configure Recording Tool

**Windows Game Bar:**
1. Press `Windows + G` to open Game Bar
2. Click Settings (gear icon)
3. Configure:
   - Resolution: 1920x1080
   - Frame Rate: 30 fps
   - Audio: Enable microphone if needed
4. Set output directory: `docs/demo-recordings/`

**OBS Studio:**
1. Open OBS Studio
2. Add Source: Display Capture
3. Configure:
   - Resolution: 1920x1080
   - FPS: 30
   - Output: `docs/demo-recordings/`
4. Set Recording Format: MP4

**QuickTime (macOS):**
1. Open QuickTime Player
2. File > New Screen Recording
3. Click Options:
   - Quality: Maximum
   - Microphone: Enable if needed
4. Click Record

#### 4.2 Test Recording

Record 5 seconds and verify:
- Video quality is good
- Audio is clear (if enabled)
- File saves correctly
- No lag or stuttering

---

### Phase 5: Execute Demo with Screenshot Capture

#### 5.1 Start Screen Recording

**Before starting**, begin your screen recording tool.

#### 5.2 Capture BEFORE Phase Screenshots

```bash
# Capture initial state before demo execution
node scripts/capture-screenshots.js \
  --phase BEFORE \
  --scenario hedis-evaluation \
  --user-type care-manager \
  --output-dir docs/screenshots/scenarios/hedis-evaluation/before
```

**What to Show in Video:**
- Navigate to the clinical portal
- Show the initial empty/loading state
- Explain what we're about to demonstrate

#### 5.3 Execute Demo Scenario

**For HEDIS Evaluation Scenario:**
1. Log in as care manager: `demo_admin@hdim.ai` / `demo123`
2. Navigate to Quality Measures dashboard
3. Show the HEDIS measures overview
4. Select a specific measure (e.g., Diabetes Care)
5. Show patient list with care gaps
6. Demonstrate gap closure workflow

**While executing**, the demo orchestrator will:
- Process data
- Update dashboards
- Generate insights

#### 5.4 Capture DURING Phase Screenshots

```bash
# Capture loading/processing states
node scripts/capture-screenshots.js \
  --phase DURING \
  --scenario hedis-evaluation \
  --user-type care-manager \
  --output-dir docs/screenshots/scenarios/hedis-evaluation/during
```

**What to Show in Video:**
- Data loading animations
- Progress indicators
- Partial data appearing
- System processing indicators

#### 5.5 Capture AFTER Phase Screenshots

```bash
# Capture final state with complete data
node scripts/capture-screenshots.js \
  --phase AFTER \
  --scenario hedis-evaluation \
  --user-type care-manager \
  --output-dir docs/screenshots/scenarios/hedis-evaluation/after
```

**What to Show in Video:**
- Complete dashboards with data
- Final results and insights
- Time savings calculations
- Additional insights uncovered

#### 5.6 Validate Results

```bash
# Check validation status
curl http://localhost:8090/api/v1/devops/fhir-validation/status | jq '.overallStatus'

# View screenshot results
ls -la docs/screenshots/scenarios/hedis-evaluation/*/
```

#### 5.7 Stop Screen Recording

Stop your recording tool and save the video file.

---

### Phase 6: Multiple Scenario Recording

For a complete demo video, repeat Phase 5 for each scenario:

1. **HEDIS Evaluation** (3-5 minutes)
2. **Patient Journey** (4-6 minutes)
3. **Risk Stratification** (3-4 minutes)
4. **Multi-Tenant Admin** (2-3 minutes)

**Between scenarios:**
```bash
# Clear data between scenarios (if needed)
# This is handled automatically by demo orchestrator
```

---

## Recording Options

### Option A: Windows Game Bar (WSL2 Recommended)

**Advantages:**
- Built into Windows 10/11
- No additional software needed
- Good performance
- Easy to use

**Steps:**
1. Press `Windows + G` to open Game Bar
2. Click Record button or press `Windows + Alt + R`
3. Record your demo
4. Press `Windows + Alt + R` again to stop
5. Video saved to: `C:\Users\<username>\Videos\Captures\`

**Configuration:**
- Settings > Captures
- Set recording quality: 1080p, 30fps
- Enable microphone if needed

### Option B: OBS Studio (Cross-Platform)

**Advantages:**
- Professional features
- Multiple sources
- Live streaming capability
- Advanced audio mixing

**Installation:**
```bash
# Windows: Download from https://obsproject.com/
# macOS: brew install --cask obs
# Linux: sudo apt-get install obs-studio
```

**Configuration:**
1. Add Source: Display Capture
2. Settings > Output:
   - Recording Format: MP4
   - Encoder: x264
   - Bitrate: 6000 Kbps
3. Settings > Video:
   - Base Resolution: 1920x1080
   - Output Resolution: 1920x1080
   - FPS: 30

### Option C: QuickTime Player (macOS)

**Advantages:**
- Built into macOS
- Simple interface
- Good quality

**Steps:**
1. Open QuickTime Player
2. File > New Screen Recording
3. Click Record button
4. Select area or full screen
5. Click Stop in menu bar

---

## Demo Orchestration with Screenshots

### Automated Orchestration

The demo orchestrator service can handle the complete workflow:

```bash
# Start demo orchestrator (when implemented)
# This will:
# 1. Build and start services
# 2. Seed demo data
# 3. Validate FHIR data
# 4. Execute scenarios
# 5. Capture screenshots at each phase
# 6. Generate reports
```

### Manual Orchestration

For manual control:

```bash
# 1. Start services
docker compose -f demo/docker-compose.demo.yml up -d

# 2. Wait for health
sleep 90

# 3. Validate data
curl -X POST http://localhost:8090/api/v1/devops/fhir-validation/validate

# 4. Capture BEFORE screenshots
node scripts/capture-screenshots.js --phase BEFORE --scenario hedis-evaluation

# 5. Execute scenario (manually in browser)

# 6. Capture DURING screenshots
node scripts/capture-screenshots.js --phase DURING --scenario hedis-evaluation

# 7. Wait for completion

# 8. Capture AFTER screenshots
node scripts/capture-screenshots.js --phase AFTER --scenario hedis-evaluation
```

---

## DevOps Agent Validation

### Understanding Validation Results

The DevOps Agent validates:
- **Resource Counts**: Minimum required resources present
- **Code Systems**: Required codes for quality measures
- **Data Authenticity**: Realistic values and formats
- **FHIR Compliance**: R4 compliance and profile validation
- **Relationships**: Patient-observation relationships

### Viewing Validation Status

```bash
# Get validation status
curl http://localhost:8090/api/v1/devops/fhir-validation/status | jq

# Check specific validation checks
curl http://localhost:8090/api/v1/devops/fhir-validation/status | \
  jq '.resourceCountChecks[] | select(.status != "PASS")'
```

### Validation in Video

**Show in your video:**
1. Navigate to DevOps Agent UI (when available)
2. Show validation dashboard
3. Highlight PASS/FAIL status
4. Explain what each check validates
5. Show data authenticity metrics

---

## Post-Production

### Organizing Files

```
docs/
├── demo-recordings/
│   ├── hedis-evaluation_20260115_143022.mp4
│   ├── patient-journey_20260115_150145.mp4
│   └── risk-stratification_20260115_151530.mp4
├── screenshots/
│   └── scenarios/
│       ├── hedis-evaluation/
│       │   ├── before/
│       │   ├── during/
│       │   └── after/
│       └── ...
```

### Editing Recommendations

1. **Intro (10-15 seconds)**
   - HDIM logo
   - Brief platform overview
   - Scenario introduction

2. **Main Content**
   - BEFORE phase (show initial state)
   - DURING phase (show processing)
   - AFTER phase (show results)
   - Highlight key insights

3. **Outro (10-15 seconds)**
   - Summary of benefits
   - Time savings highlighted
   - Call to action

### Video Specifications

- **Resolution**: 1920x1080 (Full HD)
- **Frame Rate**: 30 fps
- **Format**: MP4 (H.264)
- **Aspect Ratio**: 16:9
- **Audio**: 48kHz, Stereo (if included)

---

## Troubleshooting

### Services Not Starting

**Problem**: Docker containers fail to start

**Solution**:
```bash
# Check Docker status
docker ps

# Check logs
docker compose -f demo/docker-compose.demo.yml logs

# Restart services
docker compose -f demo/docker-compose.demo.yml restart
```

### Screenshot Capture Fails

**Problem**: Screenshots not capturing or empty

**Solution**:
```bash
# Verify portal is accessible
curl http://localhost:4200

# Check all services are healthy
docker compose -f demo/docker-compose.demo.yml ps

# Increase timeout in script
# Edit scripts/capture-screenshots.js
# Increase CONFIG.timeout value
```

### Validation Fails

**Problem**: FHIR validation returns FAIL status

**Solution**:
```bash
# Check FHIR service
curl http://localhost:8085/fhir/metadata

# Verify data was seeded
curl "http://localhost:8085/fhir/Patient?_summary=count"

# Re-seed data if needed
# (Implementation depends on seeding service)
```

### Recording Quality Issues

**Problem**: Video is blurry or laggy

**Solutions**:
- Reduce screen resolution to 1280x720
- Close unnecessary applications
- Use hardware acceleration (if available)
- Record in smaller segments

### Audio Issues

**Problem**: No audio or poor quality

**Solutions**:
- Check microphone permissions
- Test audio before recording
- Use external microphone for better quality
- Adjust audio levels in recording software

---

## Best Practices

### Pre-Recording Checklist

- [ ] Docker is running and healthy
- [ ] All services are up (check `docker ps`)
- [ ] Demo data is seeded and validated
- [ ] Browser is ready (logged in, correct viewport size)
- [ ] Recording tool is configured and tested
- [ ] Script/narration is prepared
- [ ] Desktop is clean (close unnecessary apps)
- [ ] Notifications are disabled

### During Recording

1. **Speak Clearly**: If narrating, speak slowly and clearly
2. **Pause Between Actions**: Give viewers time to process
3. **Highlight Key Features**: Use cursor to point out important elements
4. **Show Data Loading**: Don't skip loading states - they show real-time processing
5. **Demonstrate Value**: Focus on time savings and insights

### Screenshot Organization

- Use consistent naming: `scenario-phase-userType-timestamp.png`
- Organize by scenario and phase
- Keep BEFORE/DURING/AFTER sets together
- Document which screenshots are used in final video

### Video Length Guidelines

- **Short Demo**: 2-3 minutes (single scenario)
- **Standard Demo**: 5-7 minutes (2-3 scenarios)
- **Full Demo**: 10-15 minutes (all scenarios)
- **Deep Dive**: 20-30 minutes (detailed walkthrough)

### Quality Standards

- **Resolution**: Minimum 1080p (1920x1080)
- **Frame Rate**: 30 fps minimum
- **Audio**: Clear, no background noise
- **Lighting**: Good screen visibility
- **Stability**: No shaking or sudden movements

---

## Quick Reference Commands

```bash
# Start everything
docker compose -f demo/docker-compose.demo.yml up -d

# Check status
docker compose -f demo/docker-compose.demo.yml ps

# Validate data
curl -X POST http://localhost:8090/api/v1/devops/fhir-validation/validate

# Capture BEFORE screenshots
node scripts/capture-screenshots.js --phase BEFORE --scenario hedis-evaluation

# Capture DURING screenshots
node scripts/capture-screenshots.js --phase DURING --scenario hedis-evaluation

# Capture AFTER screenshots
node scripts/capture-screenshots.js --phase AFTER --scenario hedis-evaluation

# View screenshots
ls -la docs/screenshots/scenarios/*/

# Stop everything
docker compose -f demo/docker-compose.demo.yml down
```

---

## Additional Resources

- **Screenshot Guide**: `docs/SCREENSHOT_CAPTURE_RECORDING_GUIDE.md`
- **Demo Startup Guide**: `docs/marketing/demo/DEMO_STARTUP_GUIDE.md`
- **DevOps Agent Validation**: `docs/DEVOPS_AGENT_FHIR_VALIDATION.md`
- **Quick Start**: `docs/marketing/demo/QUICK_START.md`

---

## Support

For issues or questions:
1. Check troubleshooting section above
2. Review service logs: `docker compose logs <service-name>`
3. Check validation status: DevOps Agent UI
4. Review documentation in `docs/` directory

---

**Last Updated**: January 2026  
**Version**: 1.0
