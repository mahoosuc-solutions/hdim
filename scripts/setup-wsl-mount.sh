#!/bin/bash

################################################################################
# setup-wsl-mount.sh
#
# WSL2 mount configuration script
# Sets up permanent mounting of WD-Black external drive with proper permissions
#
# Usage: sudo ./scripts/setup-wsl-mount.sh [--device /dev/sdd] [--mount-point /mnt/wd-black]
#
# This script:
# 1. Mounts the external drive (sdd) to /mnt/wd-black
# 2. Sets up fstab entry for permanent mounting
# 3. Configures proper permissions for Docker
# 4. Verifies mount and accessibility
#
################################################################################

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
DEVICE="${1:-/dev/sdd}"
MOUNT_POINT="${2:-/mnt/wd-black}"
FSTYPE="ext4"
MOUNT_OPTIONS="defaults,nofail"

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}ERROR: This script must be run with sudo${NC}"
    exit 1
fi

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}WSL Mount Configuration${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# Check device exists
echo -e "${YELLOW}Checking device: $DEVICE${NC}"
if [ ! -b "$DEVICE" ]; then
    echo -e "${RED}ERROR: Device not found: $DEVICE${NC}"
    echo "Available devices:"
    lsblk -d
    exit 1
fi
echo -e "${GREEN}✓ Device found${NC}"
echo ""

# Check filesystem
echo -e "${YELLOW}Checking filesystem...${NC}"
FILESYSTEM=$(blkid -s TYPE -o value "$DEVICE")
echo "  Filesystem type: $FILESYSTEM"
if [ "$FILESYSTEM" != "ext4" ]; then
    echo -e "${YELLOW}⚠ WARNING: Filesystem is $FILESYSTEM, expected ext4${NC}"
    echo "  This may work but ext4 is recommended"
fi
echo ""

# Create mount point if needed
echo -e "${YELLOW}Setting up mount point...${NC}"
if [ ! -d "$MOUNT_POINT" ]; then
    mkdir -p "$MOUNT_POINT"
    echo -e "${GREEN}✓ Created mount point: $MOUNT_POINT${NC}"
else
    echo "  Mount point already exists: $MOUNT_POINT"
fi
echo ""

# Check if already mounted
echo -e "${YELLOW}Checking if already mounted...${NC}"
if mountpoint -q "$MOUNT_POINT"; then
    echo "  Already mounted to $MOUNT_POINT"
    ALREADY_MOUNTED=true
else
    echo "  Not currently mounted"
    ALREADY_MOUNTED=false
fi
echo ""

# Mount if not already mounted
if [ "$ALREADY_MOUNTED" = false ]; then
    echo -e "${YELLOW}Mounting device...${NC}"
    if mount -t "$FSTYPE" -o "$MOUNT_OPTIONS" "$DEVICE" "$MOUNT_POINT"; then
        echo -e "${GREEN}✓ Successfully mounted $DEVICE to $MOUNT_POINT${NC}"
    else
        echo -e "${RED}ERROR: Failed to mount device${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✓ Device already mounted${NC}"
fi
echo ""

# Check mount is accessible
echo -e "${YELLOW}Verifying mount...${NC}"
if [ ! -r "$MOUNT_POINT" ]; then
    echo -e "${RED}ERROR: Mount point is not readable${NC}"
    exit 1
fi
if [ ! -w "$MOUNT_POINT" ]; then
    echo -e "${YELLOW}⚠ WARNING: Mount point is not writable${NC}"
    echo "  Attempting to fix permissions..."
    chmod 755 "$MOUNT_POINT"
fi

if [ -x "$(command -v docker)" ]; then
    DOCKER_GID=$(getent group docker | cut -d: -f3)
    if [ -n "$DOCKER_GID" ]; then
        chown -R root:$DOCKER_GID "$MOUNT_POINT"
        echo -e "${GREEN}✓ Set docker group ownership${NC}"
    fi
fi
echo -e "${GREEN}✓ Mount point is accessible${NC}"
echo ""

# Setup fstab entry
echo -e "${YELLOW}Setting up permanent mount (fstab)...${NC}"
FSTAB_ENTRY="$DEVICE $MOUNT_POINT $FSTYPE $MOUNT_OPTIONS 0 0"

if grep -q "$DEVICE" /etc/fstab; then
    echo "  Entry already exists in fstab"
else
    echo "  Adding to /etc/fstab..."
    echo "$FSTAB_ENTRY" >> /etc/fstab
    echo -e "${GREEN}✓ Added to fstab${NC}"
fi
echo ""

# Show mount statistics
echo -e "${YELLOW}Mount statistics:${NC}"
df -h "$MOUNT_POINT"
echo ""

# Show available space
AVAILABLE=$(df -h "$MOUNT_POINT" | tail -1 | awk '{print $4}')
TOTAL=$(df -h "$MOUNT_POINT" | tail -1 | awk '{print $2}')
echo -e "${YELLOW}Disk usage:${NC}"
echo "  Total: $TOTAL"
echo "  Available: $AVAILABLE"
echo ""

# List current contents
echo -e "${YELLOW}Current contents:${NC}"
if [ -d "$MOUNT_POINT" ] && [ "$(ls -A $MOUNT_POINT 2>/dev/null)" ]; then
    du -sh "$MOUNT_POINT"/* 2>/dev/null | head -10
    echo ""
else
    echo "  (empty)"
    echo ""
fi

# Test write permissions
echo -e "${YELLOW}Testing write permissions...${NC}"
TEST_FILE="$MOUNT_POINT/.mount-test-$(date +%s)"
if touch "$TEST_FILE" 2>/dev/null; then
    rm -f "$TEST_FILE"
    echo -e "${GREEN}✓ Write permissions OK${NC}"
else
    echo -e "${RED}✗ Cannot write to mount point${NC}"
    echo "  Run: sudo chown \$USER:docker $MOUNT_POINT"
    echo "  Then: chmod g+w $MOUNT_POINT"
fi
echo ""

# Final summary
echo -e "${BLUE}================================================${NC}"
echo -e "${GREEN}✓ WSL Mount Configuration Complete!${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""
echo "Mount point: $MOUNT_POINT"
echo "Device: $DEVICE"
echo "Filesystem: $FILESYSTEM"
echo ""
echo "To verify on next boot, run:"
echo "  df $MOUNT_POINT"
echo ""
