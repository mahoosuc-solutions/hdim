#!/bin/bash

# Screen Recording Script for Screenshot Capture Demo
# Records the demonstration of phase-based screenshot capture functionality

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
OUTPUT_DIR="${OUTPUT_DIR:-docs/demo-recordings}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTPUT_FILE="${OUTPUT_DIR}/screenshot-capture-demo_${TIMESTAMP}.mp4"
RESOLUTION="${RESOLUTION:-1920x1080}"
FPS="${FPS:-30}"

echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  HDIM Screenshot Capture Demo - Screen Recording${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Check for recording tools
RECORDER=""
if command -v ffmpeg &> /dev/null; then
    RECORDER="ffmpeg"
    echo -e "${GREEN}✓ Found ffmpeg${NC}"
elif command -v obs &> /dev/null; then
    RECORDER="obs"
    echo -e "${GREEN}✓ Found OBS Studio${NC}"
elif command -v simplescreenrecorder &> /dev/null; then
    RECORDER="simplescreenrecorder"
    echo -e "${GREEN}✓ Found SimpleScreenRecorder${NC}"
else
    echo -e "${YELLOW}⚠ No screen recording tool found in PATH${NC}"
    echo ""
    echo "Available options:"
    echo "1. Install ffmpeg: sudo apt-get install ffmpeg"
    echo "2. Use Windows screen recorder (Windows + G on Windows 10/11)"
    echo "3. Use OBS Studio (download from https://obsproject.com/)"
    echo ""
    echo "For WSL2, you can also:"
    echo "- Use Windows Game Bar (Windows + G) to record"
    echo "- Use Windows Snipping Tool (Windows + Shift + S) for screenshots"
    echo "- Install OBS Studio on Windows host"
    echo ""
    exit 1
fi

# Create output directory
mkdir -p "$OUTPUT_DIR"
echo -e "${GREEN}✓ Output directory: ${OUTPUT_DIR}${NC}"
echo ""

# Display recording instructions
echo -e "${BLUE}Recording Configuration:${NC}"
echo "  Resolution: $RESOLUTION"
echo "  Frame Rate: $FPS fps"
echo "  Output: $OUTPUT_FILE"
echo ""

if [ "$RECORDER" = "ffmpeg" ]; then
    echo -e "${YELLOW}⚠ ffmpeg screen recording requires X11 display${NC}"
    echo ""
    echo "For WSL2, you have these options:"
    echo ""
    echo "Option 1: Use Windows Screen Recorder"
    echo "  1. Press Windows + G to open Game Bar"
    echo "  2. Click Record button or press Windows + Alt + R"
    echo "  3. Record your demo"
    echo "  4. Stop recording (Windows + Alt + R again)"
    echo ""
    echo "Option 2: Use OBS Studio (Windows)"
    echo "  1. Install OBS Studio on Windows"
    echo "  2. Configure Display Capture source"
    echo "  3. Start recording"
    echo ""
    echo "Option 3: Manual Recording Steps"
    echo "  1. Start your demo environment"
    echo "  2. Open terminal and run screenshot capture commands"
    echo "  3. Use Windows screen recorder to capture the session"
    echo ""
    
    # Provide manual steps
    echo -e "${BLUE}Manual Recording Steps:${NC}"
    echo ""
    echo "1. Start demo services:"
    echo "   cd /home/webemo-aaron/projects/hdim-master"
    echo "   docker compose -f demo/docker-compose.demo.yml up -d"
    echo ""
    echo "2. Wait for services to be healthy:"
    echo "   docker compose -f demo/docker-compose.demo.yml ps"
    echo ""
    echo "3. Test screenshot capture (BEFORE phase):"
    echo "   node scripts/capture-screenshots.js --phase BEFORE --scenario hedis-evaluation --user-type care-manager --output-dir docs/screenshots/scenarios/hedis-evaluation/before"
    echo ""
    echo "4. Test screenshot capture (DURING phase):"
    echo "   node scripts/capture-screenshots.js --phase DURING --scenario hedis-evaluation --user-type care-manager --output-dir docs/screenshots/scenarios/hedis-evaluation/during"
    echo ""
    echo "5. Test screenshot capture (AFTER phase):"
    echo "   node scripts/capture-screenshots.js --phase AFTER --scenario hedis-evaluation --user-type care-manager --output-dir docs/screenshots/scenarios/hedis-evaluation/after"
    echo ""
    echo "6. View captured screenshots:"
    echo "   ls -la docs/screenshots/scenarios/hedis-evaluation/*/"
    echo ""
    
elif [ "$RECORDER" = "obs" ]; then
    echo "Starting OBS Studio..."
    echo "Please configure OBS with:"
    echo "  - Display Capture source"
    echo "  - Resolution: $RESOLUTION"
    echo "  - FPS: $FPS"
    echo "  - Output: $OUTPUT_FILE"
    echo ""
    obs &
    
elif [ "$RECORDER" = "simplescreenrecorder" ]; then
    echo "Starting SimpleScreenRecorder..."
    simplescreenrecorder &
fi

echo ""
echo -e "${GREEN}Ready to record!${NC}"
echo ""
echo "When you're done recording, press Ctrl+C to exit this script."
echo ""

# Keep script running
trap 'echo -e "\n${GREEN}Recording session ended.${NC}"; exit 0' INT TERM
while true; do
    sleep 1
done
