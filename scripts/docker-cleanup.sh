#!/bin/bash

################################################################################
# docker-cleanup.sh
#
# Automated Docker cleanup and maintenance script
# Removes unused images, volumes, containers, and build cache
#
# Usage: ./scripts/docker-cleanup.sh [--aggressive] [--dry-run]
# Options:
#   --aggressive : Remove ALL unused resources (may be disruptive)
#   --dry-run    : Show what would be deleted without actually deleting
#
# Typically run weekly via cron to prevent disk space issues
#
################################################################################

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# Options
AGGRESSIVE=false
DRY_RUN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --aggressive) AGGRESSIVE=true; shift ;;
        --dry-run) DRY_RUN=true; shift ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}Docker Cleanup & Maintenance${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}DRY RUN MODE - No changes will be made${NC}"
    echo ""
fi

# Check Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}ERROR: Docker daemon is not running${NC}"
    exit 1
fi

# Show current usage
echo -e "${YELLOW}Current Docker Disk Usage:${NC}"
docker system df
echo ""

# Stop dangling containers
echo -e "${YELLOW}Removing stopped containers...${NC}"
if [ "$DRY_RUN" = true ]; then
    STOPPED=$(docker ps -q -a -f "status=exited")
    if [ -n "$STOPPED" ]; then
        echo "Would remove containers: $STOPPED"
    else
        echo "No stopped containers to remove"
    fi
else
    docker container prune -f --filter "until=24h" || true
    echo -e "${GREEN}✓ Stopped containers removed${NC}"
fi
echo ""

# Remove dangling images
echo -e "${YELLOW}Removing dangling images...${NC}"
if [ "$DRY_RUN" = true ]; then
    DANGLING=$(docker images -q -f "dangling=true")
    if [ -n "$DANGLING" ]; then
        echo "Would remove dangling images: $DANGLING"
    else
        echo "No dangling images to remove"
    fi
else
    docker image prune -f || true
    echo -e "${GREEN}✓ Dangling images removed${NC}"
fi
echo ""

# Remove unused volumes
echo -e "${YELLOW}Analyzing unused volumes...${NC}"
if [ "$AGGRESSIVE" = true ]; then
    if [ "$DRY_RUN" = true ]; then
        UNUSED_VOLS=$(docker volume ls -q -f "dangling=true")
        if [ -n "$UNUSED_VOLS" ]; then
            echo "Would remove volumes: $UNUSED_VOLS"
        else
            echo "No unused volumes to remove"
        fi
    else
        docker volume prune -f || true
        echo -e "${GREEN}✓ Unused volumes removed${NC}"
    fi
else
    echo "⚠ Skipping volume cleanup (use --aggressive to enable)"
fi
echo ""

# Remove build cache
echo -e "${YELLOW}Checking build cache...${NC}"
CACHE_SIZE=$(docker system df --format "{{json .BuilderSize}}")
echo "Current build cache: $CACHE_SIZE bytes"

if [ "$AGGRESSIVE" = true ]; then
    if [ "$DRY_RUN" = true ]; then
        echo "Would clear build cache"
    else
        docker builder prune -f -a || true
        echo -e "${GREEN}✓ Build cache cleared${NC}"
    fi
else
    echo "⚠ Skipping build cache (use --aggressive to enable)"
fi
echo ""

# Prune networks (safe - only unused networks)
echo -e "${YELLOW}Removing unused networks...${NC}"
if [ "$DRY_RUN" = true ]; then
    echo "Would clean up unused networks"
else
    docker network prune -f || true
    echo -e "${GREEN}✓ Unused networks removed${NC}"
fi
echo ""

# Show final usage
echo -e "${YELLOW}Final Docker Disk Usage:${NC}"
docker system df
echo ""

# Show available space if external drive configured
if [ -d "/mnt/wd-black" ]; then
    echo -e "${YELLOW}External Drive Space (/mnt/wd-black):${NC}"
    df -h /mnt/wd-black
    echo ""
fi

echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}Docker cleanup complete!${NC}"
echo -e "${GREEN}================================================${NC}"
