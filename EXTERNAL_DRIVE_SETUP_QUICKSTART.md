# External Drive Setup - Quick Start Guide

**Status**: Ready for Implementation
**Created**: January 20, 2026
**Target**: Multi-Laptop Development on WD-Black External Drive

## 🎯 What We've Built

A complete portable Docker environment for HDIM that:

✅ **Frees 590GB** from your C: drive (currently 100% full)
✅ **Works across multiple laptops** with USB external drive
✅ **Prevents future disk issues** with 4-layer monitoring
✅ **Automates maintenance** with weekly/monthly scripts
✅ **Maintains HDIM data consistency** across machines

---

## 📦 What Was Created

### Scripts (in `/scripts/`)

| Script | Purpose | How Often |
|--------|---------|-----------|
| `setup-laptop-windows.ps1` | Configure Docker on Windows | Once per laptop |
| `setup-wsl-mount.sh` | Mount external drive in WSL | Once per laptop |
| `setup-external-drive.sh` | Initialize external drive structure | Once per drive |
| `docker-cleanup.sh` | Remove unused Docker artifacts | Weekly |
| `disk-monitor.sh` | Alert on disk space issues | Daily |
| `pre-build-check.sh` | Validate setup before building | Before each build |
| `compact-vhdx-monthly.ps1` | Reclaim disk space | Monthly |

### Documentation

| Document | Location | Purpose |
|----------|----------|---------|
| Comprehensive Guide | `docs/PORTABLE_DOCKER_SETUP.md` | Full setup + troubleshooting |
| Quick Start | This file | Get started in 15 minutes |

### Configuration Templates (created on external drive)

```
/mnt/wd-black/config/
├── daemon.json                          # Docker config template
├── docker-compose.override.yml          # Volume binding template
├── wsl-mounts.sh                        # WSL mount helper
└── mount-external.service               # Optional systemd service
```

---

## 🚀 Getting Started (15 Minutes)

### Step 1: Windows Setup (Admin PowerShell)

```powershell
# From project root
cd C:\Users\YourUsername\path\to\hdim-master

# Run Windows setup (Admin)
.\scripts\setup-laptop-windows.ps1 -DriveLetter X
```

**What it does:**
- ✓ Configures Docker to use external drive
- ✓ Backs up existing Docker config
- ✓ Creates necessary directories
- ✓ Restarts Docker

**Time**: ~5 minutes

### Step 2: WSL/Linux Setup

```bash
cd ~/projects/hdim-master

# Mount external drive
sudo mount /dev/sdd /mnt/wd-black

# Configure permanent mounting
sudo ./scripts/setup-wsl-mount.sh /dev/sdd /mnt/wd-black
```

**What it does:**
- ✓ Mounts /dev/sdd to /mnt/wd-black
- ✓ Adds to /etc/fstab for persistence
- ✓ Sets proper permissions

**Time**: ~3 minutes

### Step 3: Verify Setup

```bash
# Check external drive
df -h /mnt/wd-black

# Verify Docker
docker info | grep "Docker Root"

# Run pre-build check
./scripts/pre-build-check.sh
```

**Expected Results:**
- ✓ Docker Root Dir shows `/mnt/wd-black`
- ✓ All pre-build checks pass
- ✓ 100+ GB available space

**Time**: ~2 minutes

### Step 4: Start HDIM

```bash
docker compose up -d
docker compose ps
```

**Time**: ~5 minutes (pulling images for first time)

---

## 📊 Before & After

### Before (Current State)

```
C: Drive: 100% full (7.8GB available)
├── Docker VHDX: 590GB
├── WSL ext4.vhdx: 217GB
└── Other files: 173GB

Problems:
❌ Cannot build services
❌ System unstable
❌ Cannot add new tools/data
```

### After (With Setup)

```
C: Drive: ~30% full (700GB available)
├── Docker config: < 100MB
├── WSL system: ~217GB
└── Other files: Normal

/mnt/wd-black: 1TB
├── docker-data: ~500GB (images)
├── hdim-volumes: ~100GB (databases)
└── Free space: ~400GB (for growth)

Benefits:
✅ Build all HDIM services (714 Dockerfiles)
✅ Portable across 3+ laptops
✅ Automatic cleanup prevents issues
✅ Real-time monitoring
```

---

## 🛡️ Four-Layer Protection

### Layer 1: Pre-Build Validation

```bash
./scripts/pre-build-check.sh
```

Prevents builds when:
- ✓ Docker is not using external drive
- ✓ Insufficient space (< 100GB)
- ✓ C: drive > 90% full

### Layer 2: Daily Monitoring

```bash
./scripts/disk-monitor.sh --check-external
```

Alerts when:
- ⚠ Drives exceed 85% capacity
- ⚠ Docker space running low

### Layer 3: Weekly Cleanup

```bash
./scripts/docker-cleanup.sh
```

Removes:
- Old stopped containers
- Dangling images
- Unused volumes

### Layer 4: Monthly Compaction (Windows)

```powershell
.\scripts\compact-vhdx-monthly.ps1
```

Reclaims:
- 50-200GB from WSL disk compaction
- Defragments ext4.vhdx
- Maintains ~70% disk availability

---

## 🔄 Multi-Laptop Workflow

### Using Same Drive on Different Laptop

**Step 1: Prepare Drive**

```bash
# On current laptop
docker compose down
./scripts/docker-cleanup.sh --aggressive
sudo umount /mnt/wd-black
```

**Step 2: Plug Into New Laptop**

```powershell
# On new laptop (Windows PowerShell Admin)
.\scripts\setup-laptop-windows.ps1 -DriveLetter X
```

```bash
# On new laptop (Linux/WSL)
sudo mount /dev/sdd /mnt/wd-black
docker compose up -d
```

**Result:**
- ✅ Same Docker images available immediately
- ✅ Same database volumes persistent
- ✅ Ready to develop in <5 minutes

---

## 📋 Maintenance Schedule

### Daily (Automated via Cron)

```bash
# Add to crontab -e
0 3 * * * cd ~/projects/hdim-master && ./scripts/disk-monitor.sh --check-external
```

### Weekly (Automated via Cron)

```bash
# Add to crontab -e
0 2 * * 0 cd ~/projects/hdim-master && ./scripts/docker-cleanup.sh
```

### Monthly (Manual or Task Scheduler)

```powershell
# Run manually or schedule in Task Scheduler
.\scripts\compact-vhdx-monthly.ps1
```

### Before Building

```bash
./scripts/pre-build-check.sh
```

---

## 🐛 Quick Troubleshooting

### "Permission denied on /mnt/wd-black"

```bash
sudo chown -R $USER:docker /mnt/wd-black
chmod 755 /mnt/wd-black
```

### "Docker still using C: drive"

```bash
# Verify daemon.json
cat ~/.docker/daemon.json | grep data-root

# Restart Docker if needed
docker info  # Check if updated
```

### "WSL won't mount /dev/sdd"

```bash
# Check device exists
lsblk | grep sdd

# Try manual mount
sudo mount -t ext4 /dev/sdd /mnt/wd-black
```

### "Not enough space to build"

```bash
# Clean aggressively
./scripts/docker-cleanup.sh --aggressive

# Check space
docker system df
df -h /mnt/wd-black
```

See **`docs/PORTABLE_DOCKER_SETUP.md`** for complete troubleshooting.

---

## 📁 Directory Structure

```
hdim-master/
├── scripts/
│   ├── setup-laptop-windows.ps1      ← Run this first (Windows Admin)
│   ├── setup-wsl-mount.sh             ← Run this second (WSL sudo)
│   ├── docker-cleanup.sh              ← Run weekly
│   ├── disk-monitor.sh                ← Run daily
│   ├── pre-build-check.sh             ← Run before builds
│   ├── setup-external-drive.sh        ← For drive initialization
│   └── compact-vhdx-monthly.ps1       ← Run monthly (Windows)
│
├── docs/
│   └── PORTABLE_DOCKER_SETUP.md       ← Complete guide
│
├── EXTERNAL_DRIVE_SETUP_QUICKSTART.md ← This file
│
└── docker-compose.override.yml        ← Copy from external drive

/mnt/wd-black/  (External Drive)
├── docker-data/                       ← Docker images & containers
│   ├── images/
│   ├── volumes/
│   └── build-cache/
│
├── hdim-volumes/                      ← Persistent HDIM data
│   ├── postgres/
│   ├── redis/
│   └── kafka/
│
├── config/                            ← Templates & configs
│   ├── daemon.json
│   ├── docker-compose.override.yml
│   └── wsl-mounts.sh
│
└── README.md                          ← Generated by setup script
```

---

## ✅ Verification Checklist

After setup, verify each item:

- [ ] Windows setup script ran successfully
- [ ] WSL mount setup script ran successfully
- [ ] `docker info` shows data-root on external drive
- [ ] `df -h /mnt/wd-black` shows 1TB available
- [ ] `./scripts/pre-build-check.sh` passes all checks
- [ ] `docker compose ps` shows running containers
- [ ] External drive accessible from both Windows and WSL

---

## 🎓 Key Concepts

### Why External Drive?

- **Performance**: USB 3.0 is fast enough for Docker
- **Portability**: Move entire dev environment between laptops
- **Capacity**: 1TB dedicated to development (vs. crowding C: drive)
- **Isolation**: Keeps system drive clean and stable

### Why Multiple Monitoring Layers?

- **Pre-build**: Catches issues before they cause build failures
- **Daily**: Early warning system for space issues
- **Weekly**: Prevents accumulation of orphaned images
- **Monthly**: Reclaims space from sparse disk areas

### How Multi-Laptop Sharing Works

1. **Docker data** (images, containers) stored on external drive
2. **Database volumes** (PostgreSQL, Redis) stored on external drive
3. **Source code** kept locally on each laptop (optional)
4. **Laptop switching**: Unplug, plug into new laptop, continue developing

---

## 🚨 Important Notes

⚠️ **Before Unplugging Drive:**
- Always run `docker compose down`
- Always run `docker-cleanup.sh`
- Always run `sudo umount /mnt/wd-black`
- Safely eject from Windows

⚠️ **Data Safety:**
- Backup critical volumes before major changes
- External drive is not redundant - keep a backup
- Monthly compaction is recommended

⚠️ **Performance:**
- USB 3.0 is adequate for HDIM
- Local SSD would be faster (USB-C or Thunderbolt)
- Network drives would be slower

---

## 📞 Getting Help

### If Setup Fails

1. Check **`docs/PORTABLE_DOCKER_SETUP.md`** troubleshooting section
2. Verify prerequisites (Docker installed, WSL2 available)
3. Run scripts individually to find which step fails
4. Check script output for specific error messages

### If Performance Issues

1. Run `docker system df` to check storage
2. Run `./scripts/docker-cleanup.sh --dry-run` to see what would be cleaned
3. Monitor with `./scripts/disk-monitor.sh --threshold 80`
4. Consider compact VHDX if WSL disk is large

### For HDIM-Specific Issues

See main HDIM documentation in `docs/README.md` and `CLAUDE.md`

---

## 📈 Next Steps

1. **Immediate** (Today):
   - Run Windows setup script
   - Run WSL mount setup script
   - Verify with pre-build check

2. **This Week**:
   - Build and test first HDIM services
   - Set up cron jobs for automation
   - Verify multi-laptop switching (if available)

3. **This Month**:
   - Run first monthly VHDX compaction
   - Monitor disk usage patterns
   - Test disaster recovery (from backup)

---

## 📊 Monitoring Commands

Keep these handy:

```bash
# Check external drive space
df -h /mnt/wd-black

# Check Docker storage usage
docker system df

# Check C: drive (if on Windows)
df -h /mnt/c

# List Docker images
docker images | head -20

# Monitor in real-time
watch -n 2 'df -h /mnt/wd-black && echo "---" && docker system df'
```

---

## 🎉 Success Indicators

After setup, you should see:

✅ C: drive usage drops from 100% to ~30%
✅ External drive shows 400-600GB available
✅ Can build all 51 HDIM services
✅ Docker Compose services start without errors
✅ All 4 monitoring layers working

---

**Your portable Docker development environment is ready!**

Start with the quick setup above, reference the full guide as needed, and enjoy stable development across multiple laptops.

_Questions? See `docs/PORTABLE_DOCKER_SETUP.md` for complete documentation_
