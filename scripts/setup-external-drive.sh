#!/bin/bash

################################################################################
# setup-external-drive.sh
#
# Sets up portable Docker development environment on external WD-Black drive
# for use across multiple laptops. Creates standardized directory structure.
#
# Usage: ./scripts/setup-external-drive.sh [mount-point]
# Example: ./scripts/setup-external-drive.sh /mnt/external-dev
#
# This script:
# 1. Creates standardized directory structure on external drive
# 2. Sets up Docker data-root location
# 3. Configures WSL mount points
# 4. Initializes monitoring and automation scripts
#
################################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default mount point
MOUNT_POINT="${1:-/mnt/wd-black}"

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}Portable Docker Setup${NC}"
echo -e "${BLUE}================================${NC}"
echo ""

# Check if mount point exists
if [ ! -d "$MOUNT_POINT" ]; then
    echo -e "${RED}ERROR: Mount point $MOUNT_POINT does not exist${NC}"
    echo "First mount the WD-Black drive:"
    echo "  sudo mount /dev/sdd $MOUNT_POINT"
    exit 1
fi

# Check if mount point is accessible
if [ ! -w "$MOUNT_POINT" ]; then
    echo -e "${RED}ERROR: No write permission on $MOUNT_POINT${NC}"
    echo "Please run: sudo chown -R \$USER:docker $MOUNT_POINT"
    exit 1
fi

echo -e "${YELLOW}Setting up directory structure on: $MOUNT_POINT${NC}"
echo ""

# Create directory structure
DIRS=(
    "$MOUNT_POINT/docker-data"
    "$MOUNT_POINT/docker-data/images"
    "$MOUNT_POINT/docker-data/volumes"
    "$MOUNT_POINT/docker-data/build-cache"
    "$MOUNT_POINT/wsl-distributions"
    "$MOUNT_POINT/projects"
    "$MOUNT_POINT/scripts"
    "$MOUNT_POINT/config"
    "$MOUNT_POINT/backups"
    "$MOUNT_POINT/hdim-volumes/postgres"
    "$MOUNT_POINT/hdim-volumes/redis"
    "$MOUNT_POINT/hdim-volumes/kafka"
    "$MOUNT_POINT/hdim-logs"
)

for dir in "${DIRS[@]}"; do
    if mkdir -p "$dir" 2>/dev/null; then
        echo -e "${GREEN}✓${NC} Created: $dir"
    else
        echo -e "${YELLOW}⚠${NC} Could not create: $dir (may need sudo)"
    fi
done

echo ""
echo -e "${YELLOW}Setting permissions...${NC}"

# Attempt to set permissions recursively
if sudo chown -R "$USER:docker" "$MOUNT_POINT" 2>/dev/null; then
    echo -e "${GREEN}✓${NC} Permissions updated"
else
    echo -e "${YELLOW}⚠${NC} Could not change ownership (may not be needed)"
fi

# Create configuration files
echo ""
echo -e "${YELLOW}Creating configuration files...${NC}"

# Docker daemon.json configuration template
cat > "$MOUNT_POINT/config/daemon.json" << 'EOF'
{
  "data-root": "/mnt/wd-black/docker-data",
  "builder": {
    "gc": {
      "defaultKeepStorage": "50GB",
      "enabled": true
    }
  },
  "max-concurrent-downloads": 3,
  "max-concurrent-uploads": 3,
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "experimental": false
}
EOF
echo -e "${GREEN}✓${NC} Created: daemon.json template"

# WSL mount configuration
cat > "$MOUNT_POINT/config/wsl-mounts.sh" << 'EOF'
#!/bin/bash
# Add to /etc/wsl.conf to make mounts persistent

# Create /etc/wsl.conf if it doesn't exist
if [ ! -f /etc/wsl.conf ]; then
    cat > /tmp/wsl.conf << 'WSLEOF'
[interop]
enabled=true
appendWindowsPath=true

[automount]
enabled=true
options="metadata,uid=1000,gid=1000,umask=022"
root=/mnt
WSLEOF

    echo "Review the contents above and add to /etc/wsl.conf"
    echo "Command: sudo nano /etc/wsl.conf"
fi
EOF
chmod +x "$MOUNT_POINT/config/wsl-mounts.sh"
echo -e "${GREEN}✓${NC} Created: wsl-mounts.sh"

# Create systemd service for mounting (optional)
cat > "$MOUNT_POINT/config/mount-external.service" << 'EOF'
[Unit]
Description=Mount external WD-Black drive
Before=docker.service
Wants=docker.service

[Service]
Type=oneshot
ExecStart=/usr/bin/mount /dev/sdd /mnt/external-dev
ExecStop=/usr/bin/umount /mnt/external-dev
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
EOF
echo -e "${GREEN}✓${NC} Created: mount-external.service (optional)"

# Docker Compose override template for HDIM
cat > "$MOUNT_POINT/config/docker-compose.override.yml" << 'EOF'
version: '3.8'

# Docker Compose override for using external drive volumes
# Place this in your HDIM project root to automatically use external drive

volumes:
  postgres-data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /mnt/external-dev/hdim-volumes/postgres

  redis-data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /mnt/external-dev/hdim-volumes/redis

  kafka-data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /mnt/external-dev/hdim-volumes/kafka

services:
  postgres:
    volumes:
      - postgres-data:/var/lib/postgresql/data

  redis:
    volumes:
      - redis-data:/data

  kafka:
    volumes:
      - kafka-data:/var/lib/kafka/data
EOF
echo -e "${GREEN}✓${NC} Created: docker-compose.override.yml template"

echo ""
echo -e "${YELLOW}Creating README...${NC}"

cat > "$MOUNT_POINT/README.md" << 'EOF'
# Portable Docker Development Environment - WD-Black Drive

This WD-Black external drive contains a portable Docker development environment configured for use across multiple laptops.

## Directory Structure

```
/mnt/wd-black/
├── docker-data/              # Docker Desktop data-root (images, containers, volumes)
│   ├── images/
│   ├── volumes/
│   └── build-cache/
├── hdim-volumes/             # HDIM service volumes
│   ├── postgres/
│   ├── redis/
│   └── kafka/
├── hdim-logs/                # Application logs
├── projects/                 # Optional: project source code
├── wsl-distributions/        # Optional: WSL distro exports for portability
├── backups/                  # Volume backups
├── scripts/                  # Automation scripts
├── config/                   # Configuration templates
└── README.md                 # This file
```

## Setup on New Laptop

### 1. Mount the Drive

**Windows/WSL:**
```powershell
# In PowerShell (Admin)
wsl --mount \\?\Global??\{DISK-GUID}
```

**Linux:**
```bash
sudo mount /dev/sdd /mnt/wd-black
```

### 2. Configure Docker Desktop

Windows:
1. Stop Docker Desktop
2. Edit `C:\Users\<username>\.docker\daemon.json`
3. Copy contents from `config/daemon.json` on this drive
4. Update path to actual mount point if different
5. Restart Docker Desktop

### 3. Configure WSL (if needed)

```bash
# Make mount permanent
echo "/dev/sdd /mnt/wd-black ext4 defaults 0 0" | sudo tee -a /etc/fstab
```

### 4. Configure HDIM Project

```bash
# Copy override template to project root
cp config/docker-compose.override.yml /path/to/hdim-master/
```

## Usage

### Start with HDIM

```bash
cd /path/to/hdim-master
docker compose up -d
```

All volumes automatically use external drive.

### Monitoring & Maintenance

```bash
# Check disk usage
df -h /mnt/wd-black
docker system df

# Clean up unused images/volumes (weekly)
docker system prune -a --volumes

# Backup volumes (before switching laptops)
./scripts/backup-volumes.sh
```

## Switching Between Laptops

Before unplugging or switching laptops:

```bash
# Stop all containers
docker compose down

# Backup volumes
./scripts/backup-volumes.sh

# Verify mount point is clear
df /mnt/wd-black
```

## Multi-Laptop Configuration

This drive is configured for portable use. Each laptop should have:

1. Docker Desktop installed
2. WSL2 with Ubuntu distribution
3. Same mount point: `/mnt/wd-black` (Linux) or custom for Windows

Configuration files in `config/` can be customized per laptop if needed.

## Troubleshooting

### Drive Not Mounting

```bash
# Check if drive is recognized
lsblk

# Check filesystem
sudo blkid

# If ext4 corrupted, run fsck (dangerous, requires sudo)
sudo fsck.ext4 -n /dev/sdd
```

### Docker Access Issues

```bash
# Fix permissions
sudo chown -R $(id -u):$(id -g) /mnt/wd-black

# Add user to docker group
sudo usermod -aG docker $USER
newgrp docker
```

### WSL Mount Issues

```bash
# Remount with proper options
sudo mount /dev/sdd /mnt/wd-black -t ext4 -o defaults

# Check fstab entry
cat /etc/fstab | grep sdd
```

## Last Updated

January 20, 2026

Created by: setup-external-drive.sh
EOF
echo -e "${GREEN}✓${NC} Created: README.md"

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Setup Complete!${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo "Next steps:"
echo ""
echo "1. Verify WD-Black is mounted:"
echo "   df -h /mnt/wd-black"
echo ""
echo "2. Update Docker daemon.json:"
echo "   Location: /mnt/wd-black/config/daemon.json"
echo "   Copy contents to your Docker Desktop configuration"
echo ""
echo "3. Make mount permanent (if needed):"
echo "   echo \"/dev/sdd /mnt/wd-black ext4 defaults 0 0\" | sudo tee -a /etc/fstab"
echo ""
echo "4. For HDIM project:"
echo "   cp $MOUNT_POINT/config/docker-compose.override.yml <HDIM-project>/docker-compose.override.yml"
echo ""
echo "5. Review:"
echo "   cat $MOUNT_POINT/README.md"
echo ""
