# Docker Startup Guide

## Issue: Docker Daemon Not Accessible

If you see the error:
```
Cannot connect to the Docker daemon at unix:///var/run/docker.sock
```

This means Docker is not running or not accessible. Follow these steps:

## Solutions by Environment

### WSL2 (Windows Subsystem for Linux)

**Option 1: Start Docker Service**
```bash
sudo service docker start
```

**Option 2: Use Docker Desktop**
- Install Docker Desktop for Windows
- Enable WSL2 integration
- Start Docker Desktop
- Docker will be accessible from WSL2

**Option 3: Add User to Docker Group**
```bash
sudo usermod -aG docker $USER
newgrp docker
```

### Linux

**Start Docker Service:**
```bash
sudo systemctl start docker
sudo systemctl enable docker  # Enable on boot
```

**Check Status:**
```bash
sudo systemctl status docker
```

### macOS

**Start Docker Desktop:**
- Open Docker Desktop application
- Wait for it to fully start (whale icon in menu bar)
- Docker will be accessible from terminal

## Verify Docker is Working

```bash
# Check Docker is accessible
docker ps

# Check Docker version
docker --version

# Check Docker Compose
docker compose version
```

## Expected Output

```bash
$ docker ps
CONTAINER ID   IMAGE     COMMAND   CREATED   STATUS    PORTS     NAMES
```

If you see this (empty or with containers), Docker is working!

## After Docker is Running

Once Docker is accessible, proceed with:

```bash
cd /home/webemo-aaron/projects/hdim-master

# Run pre-flight check
./scripts/pre-flight-check.sh

# Start demo services
./scripts/run-demo-screenshots.sh
```

## Troubleshooting

### Permission Denied

If you get permission errors:
```bash
sudo usermod -aG docker $USER
newgrp docker
```

### Docker Daemon Not Running

**WSL2:**
```bash
sudo service docker start
```

**Linux:**
```bash
sudo systemctl start docker
```

**macOS:**
- Open Docker Desktop

### Cannot Connect to Docker Socket

**Check socket permissions:**
```bash
ls -la /var/run/docker.sock
```

**Fix permissions:**
```bash
sudo chmod 666 /var/run/docker.sock
```

Or add user to docker group (recommended):
```bash
sudo usermod -aG docker $USER
newgrp docker
```

## Quick Start After Docker is Running

```bash
# 1. Verify Docker
docker ps

# 2. Run pre-flight check
./scripts/pre-flight-check.sh

# 3. Start demo and capture screenshots
./scripts/run-demo-screenshots.sh
```

## Next Steps

Once Docker is running:
1. ✅ Run pre-flight check
2. ✅ Start services
3. ✅ Validate environment
4. ✅ Capture screenshots

See [COMPLETE_WORKFLOW.md](./COMPLETE_WORKFLOW.md) for full instructions.
