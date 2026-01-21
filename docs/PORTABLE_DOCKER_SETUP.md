# Portable Docker Development Environment Setup

> **Status**: Setup Guide for Multi-Laptop Docker Development
> **Target**: WD-Black 1TB External Drive
> **Updated**: January 20, 2026

## Overview

This guide sets up a portable Docker development environment for HDIM running on an external WD-Black USB drive. This allows you to develop on multiple laptops without the 590GB Docker storage consuming precious C: drive space.

### Problem This Solves

- ❌ **Before**: Docker VHDX consuming 590GB on C: drive (100% full)
- ✅ **After**: Docker data on external drive, C: drive freed

### Architecture

```
Laptop 1 ─┐
          ├──→ WD-Black (1TB) ──→ Docker Data
Laptop 2 ─┤                  ──→ HDIM Volumes
          └──→ Project Code (local to each laptop)
```

---

## Phase 1: Windows Setup (Admin PowerShell)

### Step 1.1: Connect External Drive

1. Connect the WD-Black USB drive to your Windows machine
2. Note the drive letter (e.g., `X:`, `D:`, etc.)
3. Verify it's recognized in File Explorer

### Step 1.2: Run Windows Setup Script

Open **PowerShell as Administrator** and run:

```powershell
cd C:\Users\YourUsername\path\to\hdim-master
.\scripts\setup-laptop-windows.ps1 -DriveLetter X
```

**What it does:**
- Stops Docker Desktop
- Backs up existing Docker configuration
- Creates `daemon.json` pointing to external drive
- Initializes directory structure
- Restarts Docker Desktop

**Expected output:**
```
================================
Docker Desktop Setup (Windows)
================================

✓ Administrator privileges confirmed
✓ External drive found: X:\DevEnvironment
✓ Docker Desktop is installed
✓ Docker Desktop stopped
✓ Backed up to: C:\Users\...\daemon.json.backup.20260120-143022
...
✓ Setup Complete!
```

### Step 1.3: Verify Windows Setup

```powershell
# Should show Docker using external drive path
docker info | Select-String "Docker Root"

# Should show external drive in list
docker volume ls
```

---

## Phase 2: WSL Setup (Ubuntu Terminal)

### Step 2.1: Mount External Drive

Open WSL/Ubuntu terminal and run:

```bash
# First, check if device exists
lsblk

# Mount the drive
sudo mount /dev/sdd /mnt/wd-black

# Verify mount
df -h /mnt/wd-black
```

**Expected output:**
```
Filesystem      Size  Used Avail Use% Mounted on
/dev/sdd       1007G  100G  900G  11% /mnt/wd-black
```

### Step 2.2: Run WSL Mount Setup Script

```bash
cd ~/projects/hdim-master

# Make permanent and configure permissions
sudo ./scripts/setup-wsl-mount.sh /dev/sdd /mnt/wd-black
```

**What it does:**
- Mounts /dev/sdd to /mnt/wd-black
- Adds entry to `/etc/fstab` for permanent mounting
- Sets proper docker group permissions
- Verifies mount is working

**Expected output:**
```
================================================
WSL Mount Configuration
================================================

✓ Device found
✓ Filesystem found: ext4
✓ Created mount point: /mnt/wd-black
✓ Successfully mounted /dev/sdd to /mnt/wd-black
✓ Mount point is accessible
✓ Added to fstab
✓ Write permissions OK

Mount point: /mnt/wd-black
Device: /dev/sdd
Filesystem: ext4
```

### Step 2.3: Verify Permanent Mounting

Edit `/etc/fstab` to confirm entry:

```bash
cat /etc/fstab | grep sdd
```

Should show:
```
/dev/sdd /mnt/wd-black ext4 defaults,nofail 0 0
```

---

## Phase 3: Docker Configuration

### Step 3.1: Copy Docker Compose Override

From WSL:

```bash
cd ~/projects/hdim-master

# Copy the template from external drive
cp /mnt/wd-black/config/docker-compose.override.yml .

# Verify it was copied
cat docker-compose.override.yml | head -20
```

### Step 3.2: Verify Docker is Using External Drive

```bash
# Check Docker root directory
docker info --format='{{.DockerRootDir}}'

# Should output something like:
# /mnt/wd-black/docker-data
```

### Step 3.3: Create Required Directories

```bash
# Ensure all required directories exist
mkdir -p /mnt/wd-black/{docker-data,hdim-volumes/{postgres,redis,kafka},hdim-logs}

# Verify permissions
ls -la /mnt/wd-black/
```

---

## Phase 4: Pre-Build Verification

### Step 4.1: Run Pre-Build Check

```bash
./scripts/pre-build-check.sh
```

**Expected output:**
```
================================================
Pre-Build System Check
================================================

✓ Docker daemon is running
✓ Docker is using external drive
✓ Sufficient space for build
✓ Docker Compose v2.x available
✓ Found docker-compose.override.yml
✓ Dockerfiles found: 714

================================================
✓ All pre-build checks passed!
Ready to build HDIM services
================================================
```

### Step 4.2: Test Docker

```bash
# Quick test
docker run --rm hello-world

# Should see "Hello from Docker!"
```

---

## Phase 5: HDIM Project Setup

### Step 5.1: Start Docker Compose

From HDIM project root:

```bash
docker compose up -d

# Watch logs
docker compose logs -f
```

### Step 5.2: Verify Services

```bash
# List running containers
docker compose ps

# Should show multiple containers for HDIM services
```

### Step 5.3: Check Volumes

```bash
# View volumes
docker volume ls

# Should see hdim volumes from override.yml
```

---

## Maintenance Scripts

### Weekly: Docker Cleanup

```bash
./scripts/docker-cleanup.sh
```

Removes:
- Stopped containers
- Dangling images
- Unused volumes (with `--aggressive`)
- Build cache

### Daily: Disk Monitoring

```bash
./scripts/disk-monitor.sh --threshold 85 --check-external
```

Alerts if:
- C: drive > 85% full
- WSL filesystem > 85% full
- External drive > 85% full

### Monthly: VHDX Compaction (Windows PowerShell)

```powershell
.\scripts\compact-vhdx-monthly.ps1
```

Reclaims 50-200GB by compacting the WSL virtual disk.

---

## Switching Between Laptops

### Before Unplugging

1. **Stop all containers:**
   ```bash
   docker compose down
   ```

2. **Run cleanup:**
   ```bash
   ./scripts/docker-cleanup.sh --aggressive
   ```

3. **Backup volumes (optional):**
   ```bash
   # Create backup of database volumes
   tar czf ~/hdim-backup-$(date +%Y%m%d).tar.gz \
       /mnt/wd-black/hdim-volumes/
   ```

4. **Unmount in WSL:**
   ```bash
   sudo umount /mnt/wd-black
   ```

5. **Safely eject in Windows:**
   - Right-click external drive in File Explorer
   - Select "Eject"

### Plugging Into New Laptop

1. **Connect external drive**

2. **Run Windows setup:**
   ```powershell
   .\scripts\setup-laptop-windows.ps1 -DriveLetter X
   ```

3. **Mount in WSL:**
   ```bash
   sudo mount /dev/sdd /mnt/wd-black
   ```

4. **Verify:**
   ```bash
   docker ps
   df -h /mnt/wd-black
   ```

5. **Start services:**
   ```bash
   docker compose up -d
   ```

---

## Troubleshooting

### "No write permission on /mnt/wd-black"

**Fix:**
```bash
sudo chown -R $USER:docker /mnt/wd-black
chmod 755 /mnt/wd-black
```

### "Docker is still using C: drive"

**Check:**
```bash
# Verify daemon.json has correct path
cat ~/.docker/daemon.json | grep data-root

# Restart Docker if needed
```

### "WSL won't mount /dev/sdd"

**Check:**
```bash
# Verify device exists
lsblk | grep sdd

# Try manual mount with specific options
sudo mount -t ext4 -o defaults /dev/sdd /mnt/wd-black

# Check for existing mount
mount | grep wd-black
```

### "External drive not showing in Windows"

**Check:**
```powershell
# In PowerShell (Admin)
Get-Disk | Format-Table Number, Size, PartitionStyle
```

If drive shows but isn't assigned a letter:
```powershell
Get-Partition -DiskNumber 3 | New-Partition -DriveLetter X -UseMaximumSize
Format-Volume -DriveLetter X -FileSystem NTFS
```

### "Build fails with out of space"

**Actions:**
1. Run cleanup: `./scripts/docker-cleanup.sh --aggressive`
2. Check space: `docker system df`
3. Compact VHDX: `.\scripts\compact-vhdx-monthly.ps1`
4. Check external drive: `df -h /mnt/wd-black`

---

## Automation (Optional)

### Windows Task Scheduler (Monthly Compaction)

1. Open **Task Scheduler**
2. Create Basic Task:
   - **Name:** "HDIM VHDX Compaction"
   - **Trigger:** Monthly (first Sunday)
   - **Action:** Run PowerShell
   - **Program:** `powershell.exe`
   - **Arguments:** `-NoProfile -ExecutionPolicy Bypass -File C:\path\to\compact-vhdx-monthly.ps1`

### Linux Cron (Weekly Cleanup & Daily Monitoring)

```bash
# Edit crontab
crontab -e

# Add these lines:
0 2 * * 0 cd ~/projects/hdim-master && ./scripts/docker-cleanup.sh >> ~/docker-cleanup.log 2>&1
0 3 * * * cd ~/projects/hdim-master && ./scripts/disk-monitor.sh --check-external >> ~/disk-monitor.log 2>&1
```

---

## Performance Tips

1. **Build Optimization:**
   - Create `.dockerignore` files to reduce build context
   - Use build cache effectively
   - Build one service at a time

2. **Storage Optimization:**
   - Run cleanup monthly
   - Compact VHDX quarterly
   - Monitor disk space weekly

3. **Multi-Laptop Workflow:**
   - Keep source code local (don't sync)
   - Keep volumes on external drive
   - Use volume backups for safety

---

## Quick Reference

| Task | Command |
|------|---------|
| Check external drive space | `df -h /mnt/wd-black` |
| Check Docker storage | `docker system df` |
| Run pre-build check | `./scripts/pre-build-check.sh` |
| Clean Docker | `./scripts/docker-cleanup.sh` |
| Monitor disk | `./scripts/disk-monitor.sh --check-external` |
| Start HDIM | `docker compose up -d` |
| Stop HDIM | `docker compose down` |
| View logs | `docker compose logs -f` |

---

## Architecture Summary

```
┌─────────────────────────────────────────────────────┐
│ Laptop (Local Filesystem)                            │
├─────────────────────────────────────────────────────┤
│ /home/user/projects/hdim-master  (source code)      │
│ /home/user/.docker/              (docker config)    │
│ C:\Users\user\.docker\           (Windows config)   │
└────────────────────┬────────────────────────────────┘
                     │
                     │ Docker references
                     ▼
┌─────────────────────────────────────────────────────┐
│ WD-Black External Drive (via mount)                 │
├─────────────────────────────────────────────────────┤
│ /mnt/wd-black/docker-data/       (Docker root)      │
│ ├─ images/                        (500 GB max)      │
│ ├─ volumes/                       (200 GB max)      │
│ ├─ build-cache/                   (50 GB max)       │
│ /mnt/wd-black/hdim-volumes/       (HDIM persistence)│
│ ├─ postgres/                      (50-100 GB)       │
│ ├─ redis/                         (1-5 GB)          │
│ └─ kafka/                         (10-20 GB)        │
└─────────────────────────────────────────────────────┘
```

---

## Support & Questions

For issues related to:
- **HDIM Services**: See [HDIM Documentation](./README.md)
- **Docker Setup**: Check [Docker Documentation](https://docs.docker.com)
- **WSL Issues**: See [WSL Documentation](https://learn.microsoft.com/en-us/windows/wsl/)

---

_Last Updated: January 20, 2026_
_Created for HDIM Healthcare Platform Development_
