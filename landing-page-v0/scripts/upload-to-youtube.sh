#!/bin/bash

# YouTube Upload Interactive Script
# Uploads Care Gap Closure videos to YouTube and updates landing page components

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Video files
VIDEO_30S="$PROJECT_ROOT/public/videos/care-gap-closure-30s.mp4"
VIDEO_80S="$PROJECT_ROOT/public/videos/care-gap-closure.mp4"
THUMB_30S="$PROJECT_ROOT/public/videos/care-gap-closure-30s-thumb.png"
THUMB_80S="$PROJECT_ROOT/public/videos/care-gap-closure-thumb.png"

# Page files to update
DEMO_PAGE="$PROJECT_ROOT/app/demo/page.tsx"
HOME_PAGE="$PROJECT_ROOT/app/page.tsx"

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║     YouTube Upload Assistant - HDIM Care Gap Closure        ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo

# Check if videos exist
echo -e "${BLUE}[1/7]${NC} Verifying video files..."
if [[ ! -f "$VIDEO_30S" ]]; then
    echo -e "${RED}✗ Error: 30-second video not found at $VIDEO_30S${NC}"
    exit 1
fi
if [[ ! -f "$VIDEO_80S" ]]; then
    echo -e "${RED}✗ Error: 80-second video not found at $VIDEO_80S${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Both video files found${NC}"
echo -e "  - 30s: $(ls -lh "$VIDEO_30S" | awk '{print $5}')"
echo -e "  - 80s: $(ls -lh "$VIDEO_80S" | awk '{print $5}')"
echo

# Display upload instructions
echo -e "${BLUE}[2/7]${NC} YouTube Upload Instructions"
echo -e "${YELLOW}════════════════════════════════════════════════════════${NC}"
echo
echo -e "Please complete the following steps in YouTube Studio:"
echo -e "  1. Open: ${CYAN}https://studio.youtube.com${NC}"
echo -e "  2. Sign in with: ${CYAN}aaron@mahoosuc.solutions${NC}"
echo -e "  3. Click ${GREEN}CREATE${NC} → ${GREEN}Upload videos${NC}"
echo
echo -e "${YELLOW}Press Enter when you're ready to upload the first video...${NC}"
read

# 30-second video upload
echo
echo -e "${BLUE}[3/7]${NC} Upload 30-Second Video"
echo -e "${YELLOW}════════════════════════════════════════════════════════${NC}"
echo
echo -e "${CYAN}Video File:${NC}"
echo -e "  Location: $VIDEO_30S"
echo -e "  Size: $(ls -lh "$VIDEO_30S" | awk '{print $5}')"
echo -e "  Duration: 30 seconds"
echo
echo -e "${CYAN}Title:${NC}"
echo -e "  ${GREEN}HDIM Care Gap Closure Demo - 30 Second Preview${NC}"
echo
echo -e "${CYAN}Description:${NC}"
cat << 'EOF'
Watch HDIM identify Eleanor Anderson's overdue mammogram screening and close the care gap with automated workflow in just 30 seconds.

HDIM is a FHIR-native platform for healthcare quality excellence. This demo shows:
- Real-time care gap detection
- Automated patient identification
- Quick action workflows
- Care gap closure tracking

Learn more: https://www.healthdatainmotion.com

#HealthIT #HEDIS #FHIR #QualityMeasures #ValueBasedCare
EOF
echo
echo -e "${CYAN}Settings:${NC}"
echo -e "  Visibility: ${YELLOW}Unlisted${NC} (not searchable, link-only access)"
echo -e "  Category: Science & Technology"
echo -e "  Comments: Disabled"
echo
echo -e "${CYAN}Tags:${NC}"
echo -e "  healthcare, FHIR, HEDIS, care gaps, quality measures, value-based care, HDIM"
echo
echo -e "${CYAN}Thumbnail:${NC}"
echo -e "  Upload: $THUMB_30S"
echo
echo -e "${YELLOW}Upload this video now, then return here.${NC}"
echo -e "${YELLOW}Press Enter when upload is complete...${NC}"
read

# Get 30s video ID
echo
while true; do
    echo -e "${CYAN}Enter the 30-second video ID:${NC}"
    echo -e "  (From URL: https://www.youtube.com/watch?v=${YELLOW}VIDEO_ID${NC})"
    read -p "  Video ID: " VIDEO_ID_30S

    if [[ -z "$VIDEO_ID_30S" ]]; then
        echo -e "${RED}Error: Video ID cannot be empty${NC}"
        continue
    fi

    echo -e "${GREEN}✓ 30s Video ID saved: $VIDEO_ID_30S${NC}"
    break
done

# 80-second video upload
echo
echo -e "${BLUE}[4/7]${NC} Upload 80-Second Video"
echo -e "${YELLOW}════════════════════════════════════════════════════════${NC}"
echo
echo -e "${CYAN}Video File:${NC}"
echo -e "  Location: $VIDEO_80S"
echo -e "  Size: $(ls -lh "$VIDEO_80S" | awk '{print $5}')"
echo -e "  Duration: 80 seconds"
echo
echo -e "${CYAN}Title:${NC}"
echo -e "  ${GREEN}HDIM Care Gap Closure Demo - Eleanor's Story${NC}"
echo
echo -e "${CYAN}Description:${NC}"
cat << 'EOF'
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
EOF
echo
echo -e "${CYAN}Settings:${NC}"
echo -e "  Visibility: ${YELLOW}Unlisted${NC} (not searchable, link-only access)"
echo -e "  Category: Science & Technology"
echo -e "  Comments: Disabled"
echo
echo -e "${CYAN}Tags:${NC}"
echo -e "  healthcare, FHIR, HEDIS, care gaps, quality measures, Eleanor Anderson, breast cancer screening, value-based care, HDIM"
echo
echo -e "${CYAN}Thumbnail:${NC}"
echo -e "  Upload: $THUMB_80S"
echo
echo -e "${YELLOW}Upload this video now, then return here.${NC}"
echo -e "${YELLOW}Press Enter when upload is complete...${NC}"
read

# Get 80s video ID
echo
while true; do
    echo -e "${CYAN}Enter the 80-second video ID:${NC}"
    echo -e "  (From URL: https://www.youtube.com/watch?v=${YELLOW}VIDEO_ID${NC})"
    read -p "  Video ID: " VIDEO_ID_80S

    if [[ -z "$VIDEO_ID_80S" ]]; then
        echo -e "${RED}Error: Video ID cannot be empty${NC}"
        continue
    fi

    echo -e "${GREEN}✓ 80s Video ID saved: $VIDEO_ID_80S${NC}"
    break
done

# Summary
echo
echo -e "${BLUE}[5/7]${NC} Video IDs Captured"
echo -e "${YELLOW}════════════════════════════════════════════════════════${NC}"
echo -e "  30s video: ${GREEN}$VIDEO_ID_30S${NC}"
echo -e "  80s video: ${GREEN}$VIDEO_ID_80S${NC}"
echo

# Update demo page
echo -e "${BLUE}[6/7]${NC} Updating Landing Page Components..."

# Create backup
cp "$DEMO_PAGE" "$DEMO_PAGE.backup"
cp "$HOME_PAGE" "$HOME_PAGE.backup"
echo -e "${GREEN}✓ Backups created${NC}"

# Update demo page (30s video)
if grep -q "youtubeId=" "$DEMO_PAGE"; then
    # Update existing youtubeId
    sed -i "s|youtubeId=\"[^\"]*\"|youtubeId=\"$VIDEO_ID_30S\"|" "$DEMO_PAGE"
    echo -e "${GREEN}✓ Updated existing YouTube ID in demo page${NC}"
else
    # Add youtubeId before closing VideoPlayer tag
    sed -i "/description=\"Watch HDIM identify Eleanor Anderson/a\\          youtubeId=\"$VIDEO_ID_30S\"" "$DEMO_PAGE"
    echo -e "${GREEN}✓ Added YouTube ID to demo page${NC}"
fi

# Update home page (80s video)
if grep -q "youtubeId=" "$HOME_PAGE"; then
    # Update existing youtubeId
    sed -i "s|youtubeId=\"[^\"]*\"|youtubeId=\"$VIDEO_ID_80S\"|" "$HOME_PAGE"
    echo -e "${GREEN}✓ Updated existing YouTube ID in home page${NC}"
else
    # Add youtubeId before closing VideoPlayer tag
    sed -i "/description=\"Watch HDIM close a care gap/a\\          youtubeId=\"$VIDEO_ID_80S\"" "$HOME_PAGE"
    echo -e "${GREEN}✓ Added YouTube ID to home page${NC}"
fi

# Show changes
echo
echo -e "${CYAN}Changes Preview:${NC}"
echo -e "${YELLOW}────────────────────────────────────────────────────────${NC}"
echo -e "${BLUE}Demo Page (30s video):${NC}"
grep -A 5 "care-gap-closure-30s" "$DEMO_PAGE" | grep "youtubeId" || echo "  (No changes needed)"
echo
echo -e "${BLUE}Home Page (80s video):${NC}"
grep -A 5 "Eleanor's Story" "$HOME_PAGE" | grep "youtubeId" || echo "  (No changes needed)"
echo

# Test build
echo -e "${BLUE}[7/7]${NC} Testing Build..."
if npm run build > /tmp/youtube-build.log 2>&1; then
    echo -e "${GREEN}✓ Build successful${NC}"
else
    echo -e "${RED}✗ Build failed. Restoring backups...${NC}"
    mv "$DEMO_PAGE.backup" "$DEMO_PAGE"
    mv "$HOME_PAGE.backup" "$HOME_PAGE"
    echo -e "${RED}Changes reverted. Check /tmp/youtube-build.log for errors.${NC}"
    exit 1
fi

# Remove backups
rm "$DEMO_PAGE.backup" "$HOME_PAGE.backup"

# Final summary
echo
echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                  Upload Complete! ✓                          ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo
echo -e "${CYAN}Video IDs:${NC}"
echo -e "  30s: ${GREEN}$VIDEO_ID_30S${NC}"
echo -e "  80s: ${GREEN}$VIDEO_ID_80S${NC}"
echo
echo -e "${CYAN}Files Updated:${NC}"
echo -e "  ✓ app/demo/page.tsx (30s video)"
echo -e "  ✓ app/page.tsx (80s video)"
echo
echo -e "${CYAN}YouTube URLs:${NC}"
echo -e "  30s: ${BLUE}https://www.youtube.com/watch?v=$VIDEO_ID_30S${NC}"
echo -e "  80s: ${BLUE}https://www.youtube.com/watch?v=$VIDEO_ID_80S${NC}"
echo
echo -e "${YELLOW}Next Steps:${NC}"
echo -e "  1. Test locally: ${CYAN}npm run dev${NC}"
echo -e "  2. Verify both videos work (self-hosted by default)"
echo -e "  3. Optionally test YouTube: Change ${CYAN}preferYouTube={true}${NC}"
echo -e "  4. Commit: ${CYAN}git add app/page.tsx app/demo/page.tsx${NC}"
echo -e "  5. Commit: ${CYAN}git commit -m \"feat(videos): Add YouTube video IDs\"${NC}"
echo -e "  6. Deploy: ${CYAN}vercel --prod${NC}"
echo
echo -e "${GREEN}Videos are now ready for dual-source playback!${NC}"
echo -e "${CYAN}Self-hosted (default) + YouTube (alternative)${NC}"
echo
