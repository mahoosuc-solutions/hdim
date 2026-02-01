# Docker Crash Recovery Guide

**Date**: January 15, 2026  
**Status**: Docker daemon not running

## 🔧 Restart Docker

### Option 1: WSL2 (Recommended)
```bash
# Check if Docker Desktop is running on Windows
# If not, start Docker Desktop from Windows

# Then verify in WSL2:
docker ps
```

### Option 2: Linux Service
```bash
# Start Docker service
sudo service docker start

# Check status
sudo service docker status

# If service doesn't exist, try:
sudo systemctl start docker
sudo systemctl status docker
```

### Option 3: Docker Desktop (Windows/Mac)
- Open Docker Desktop application
- Wait for it to fully start (whale icon in system tray)
- Verify: `docker ps` should work

## 🔍 After Docker Restarts

### Step 1: Check Container Status
```bash
cd /home/webemo-aaron/projects/hdim-master
docker compose -f demo/docker-compose.demo.yml ps
```

### Step 2: Check for Crashed Containers
```bash
docker compose -f demo/docker-compose.demo.yml ps -a
```

### Step 3: Review Crash Logs (if any)
```bash
# Check Docker daemon logs
sudo journalctl -u docker.service --no-pager | tail -50

# Or on WSL2:
cat /var/log/docker.log 2>/dev/null || echo "Logs may be in Windows Docker Desktop"
```

### Step 4: Restart Services
```bash
# Start all services
docker compose -f demo/docker-compose.demo.yml up -d

# Or restart specific services
docker compose -f demo/docker-compose.demo.yml restart
```

## 🚨 Common Issues After Crash

### Issue 1: Containers in "Exited" State
**Solution**:
```bash
# Remove stopped containers
docker compose -f demo/docker-compose.demo.yml down

# Start fresh
docker compose -f demo/docker-compose.demo.yml up -d
```

### Issue 2: Network Issues
**Solution**:
```bash
# Remove old network
docker network rm hdim-demo-network 2>/dev/null || true

# Recreate with compose
docker compose -f demo/docker-compose.demo.yml up -d
```

### Issue 3: Volume Corruption
**Solution**:
```bash
# Check volume status
docker volume ls | grep demo

# If needed, remove and recreate
docker compose -f demo/docker-compose.demo.yml down -v
docker compose -f demo/docker-compose.demo.yml up -d
```

## 📋 Recovery Checklist

- [ ] Docker daemon started
- [ ] `docker ps` works
- [ ] Check container status
- [ ] Review any crash logs
- [ ] Restart services
- [ ] Verify services are healthy
- [ ] Check network connectivity
- [ ] Verify database connections

## 🔄 Quick Recovery Commands

```bash
# Full recovery sequence
cd /home/webemo-aaron/projects/hdim-master

# 1. Start Docker (if not running)
# (Do this manually based on your system)

# 2. Clean up and restart
docker compose -f demo/docker-compose.demo.yml down
docker compose -f demo/docker-compose.demo.yml up -d

# 3. Wait for services
sleep 30

# 4. Check status
docker compose -f demo/docker-compose.demo.yml ps

# 5. Check logs
docker compose -f demo/docker-compose.demo.yml logs --tail 20
```

## 📊 Expected Service Startup Order

1. **Infrastructure** (should start first):
   - PostgreSQL
   - Redis
   - Zookeeper
   - Elasticsearch

2. **Message Queue**:
   - Kafka (depends on Zookeeper)

3. **Application Services** (after infrastructure):
   - Gateway Service
   - FHIR Service
   - Patient Service
   - CQL Engine
   - Care Gap Service
   - Quality Measure Service

4. **Frontend** (last):
   - Clinical Portal

## ⚠️ Important Notes

### Before Crash
- ✅ Nginx config fixed (gateway-edge → gateway-service)
- ⏳ Services were starting (DNS resolution timing)
- ✅ Infrastructure services were healthy

### After Recovery
- Services will need to restart
- DNS resolution may take 60-90 seconds
- Clinical Portal needs rebuild for nginx.conf changes

## 🎯 Next Steps After Recovery

1. **Verify Docker is running**
   ```bash
   docker ps
   ```

2. **Check service status**
   ```bash
   docker compose -f demo/docker-compose.demo.yml ps
   ```

3. **Review service health**
   ```bash
   docker compose -f demo/docker-compose.demo.yml logs --tail 20
   ```

4. **Rebuild Clinical Portal** (for nginx.conf fix)
   ```bash
   docker compose -f demo/docker-compose.demo.yml build clinical-portal
   docker compose -f demo/docker-compose.demo.yml up -d clinical-portal
   ```

5. **Wait for services to stabilize** (60-90 seconds)

6. **Proceed with screenshots**

---

**Status**: Waiting for Docker restart
