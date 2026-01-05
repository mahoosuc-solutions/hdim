#!/bin/bash
# Disk Space Monitoring Script for HDIM Development Environment
# Run periodically to prevent disk space issues

set -e

# Color codes
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}=== HDIM Disk Space Monitor ===${NC}"
echo ""

# Check Windows drive (C:)
echo -e "${YELLOW}Windows C: Drive:${NC}"
WIN_USAGE=$(df -h /mnt/c | tail -1 | awk '{print $5}' | tr -d '%')
WIN_AVAIL=$(df -h /mnt/c | tail -1 | awk '{print $4}')

if [ "$WIN_USAGE" -gt 90 ]; then
    echo -e "  ${RED}CRITICAL: ${WIN_USAGE}% used (${WIN_AVAIL} available)${NC}"
    echo -e "  ${RED}ACTION REQUIRED: Run WSL disk compaction!${NC}"
elif [ "$WIN_USAGE" -gt 80 ]; then
    echo -e "  ${YELLOW}WARNING: ${WIN_USAGE}% used (${WIN_AVAIL} available)${NC}"
else
    echo -e "  ${GREEN}OK: ${WIN_USAGE}% used (${WIN_AVAIL} available)${NC}"
fi

# Check WSL disk
echo ""
echo -e "${YELLOW}WSL Disk:${NC}"
WSL_USAGE=$(df -h / | tail -1 | awk '{print $5}' | tr -d '%')
WSL_AVAIL=$(df -h / | tail -1 | awk '{print $4}')

if [ "$WSL_USAGE" -gt 90 ]; then
    echo -e "  ${RED}CRITICAL: ${WSL_USAGE}% used (${WSL_AVAIL} available)${NC}"
elif [ "$WSL_USAGE" -gt 80 ]; then
    echo -e "  ${YELLOW}WARNING: ${WSL_USAGE}% used (${WSL_AVAIL} available)${NC}"
else
    echo -e "  ${GREEN}OK: ${WSL_USAGE}% used (${WSL_AVAIL} available)${NC}"
fi

# Check Docker disk usage
echo ""
echo -e "${YELLOW}Docker Resources:${NC}"
if command -v docker &> /dev/null && docker info &> /dev/null; then
    DOCKER_TOTAL=$(docker system df --format "{{.Size}}" | head -1)
    DOCKER_RECLAIMABLE=$(docker system df --format "{{.Reclaimable}}" | head -1)
    echo "  Images: $(docker system df --format '{{.Size}}' | sed -n '1p') (reclaimable: $(docker system df --format '{{.Reclaimable}}' | sed -n '1p'))"
    echo "  Volumes: $(docker system df --format '{{.Size}}' | sed -n '3p') (reclaimable: $(docker system df --format '{{.Reclaimable}}' | sed -n '3p'))"
    echo "  Build Cache: $(docker system df --format '{{.Size}}' | sed -n '4p') (reclaimable: $(docker system df --format '{{.Reclaimable}}' | sed -n '4p'))"
else
    echo "  Docker not running or not installed"
fi

# Recommendations
echo ""
echo -e "${CYAN}=== Recommendations ===${NC}"

if [ "$WIN_USAGE" -gt 85 ]; then
    echo ""
    echo -e "${YELLOW}To free Windows disk space:${NC}"
    echo "  1. Run Docker cleanup: docker system prune -a --volumes"
    echo "  2. Compact WSL disks (from PowerShell as Admin):"
    echo "     powershell.exe -File scripts/compact-wsl-disks.ps1"
    echo "  3. Clear Windows temp: del /q/s %TEMP%\\*"
fi

if docker system df --format "{{.Reclaimable}}" 2>/dev/null | grep -qE '[0-9]+GB'; then
    echo ""
    echo -e "${YELLOW}Docker cleanup available:${NC}"
    echo "  docker container prune -f  # Remove stopped containers"
    echo "  docker image prune -f      # Remove dangling images"
    echo "  docker volume prune -f     # Remove unused volumes"
    echo "  docker builder prune -f    # Clear build cache"
fi

echo ""
echo -e "${GREEN}Script complete.${NC}"
