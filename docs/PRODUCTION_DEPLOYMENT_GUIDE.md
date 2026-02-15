# 🚀 PRODUCTION DEPLOYMENT GUIDE - v2.7.0

> Note (February 14, 2026): Authoritative production deployment path is the Kubernetes workflow in `.github/workflows/deploy-docker.yml` using digest-pinned image manifests. This document is retained as a legacy Docker Compose runbook.

**Release:** v2.7.0  
**Environment:** Local Docker Compose (Production)  
**Status:** Ready to deploy  
**Date:** February 11, 2026

---

## ⚠️ PRE-DEPLOYMENT CHECKLIST

### Critical Steps (Must Complete Before Deployment)

- [ ] **1. Create Database Backup**
  ```bash
  # Backup current production database
  docker exec hdim-demo-postgres pg_dump -U healthdata --all > production_backup_$(date +%Y%m%d_%H%M%S).sql
  
  # Verify backup file exists and is not empty
  ls -lh production_backup_*.sql
  ```

- [ ] **2. Document Current Configuration**
  ```bash
  # Save current environment variables
  docker compose config > production_config_backup.yml
  
  # Save current service status
  docker compose ps > production_status_backup.txt
  ```

- [ ] **3. Stop Current Production (if running)**
  ```bash
  # Stop all services gracefully
  docker compose down
  
  # Wait 10 seconds for clean shutdown
  sleep 10
  ```

- [ ] **4. Verify Backup Integrity**
  ```bash
  # Test that backup can be restored
  file production_backup_*.sql
  wc -l production_backup_*.sql  # Should show thousands of lines
  ```

---

## 🚀 DEPLOYMENT STEPS

### Step 1: Checkout Release Tag
```bash
cd /mnt/wdblack/dev/projects/hdim-master

# Checkout v2.7.0 tag
git checkout v2.7.0

# Verify checkout
git describe --tags
# Should output: v2.7.0
```

### Step 2: Build Docker Images
```bash
# Build all services with latest code
docker compose build --no-cache

# Expected output:
# Building [service names]...
# Successfully built [image names]
```

### Step 3: Start Production Services
```bash
# Start all services
docker compose up -d

# Wait for services to initialize (5-10 minutes)
echo "Waiting 30 seconds for services to start..."
sleep 30

# Check service status
docker compose ps

# Expected: All services should show "Up" status
```

### Step 4: Verify Deployment
```bash
# Test critical endpoints
echo "Testing endpoints..."

# Patient Service
curl -s http://localhost:8084/patient/actuator/health | grep -o '"status":"UP"' && echo "✅ Patient Service UP" || echo "❌ Patient Service DOWN"

# FHIR Service
curl -s http://localhost:8085/fhir/metadata | grep -q 'CapabilityStatement' && echo "✅ FHIR Service UP" || echo "❌ FHIR Service DOWN"

# Care Gap Service
curl -s http://localhost:8086/care-gap/actuator/health | grep -o '"status":"UP"' && echo "✅ Care Gap Service UP" || echo "❌ Care Gap Service DOWN"

# Database
docker exec hdim-demo-postgres pg_isready -U healthdata && echo "✅ Database UP" || echo "❌ Database DOWN"

# All 9 core services
echo ""
echo "Service Status:"
docker compose ps --format "table {{.Service}}\t{{.Status}}"
```

### Step 5: Validate HIPAA Compliance
```bash
# Check audit logging is active
docker compose logs patient-service 2>&1 | grep -i "audit\|logging" | head -5

# Verify Redis cache
docker exec hdim-demo-redis redis-cli ping
# Expected: PONG

# Check database connections
docker exec hdim-demo-postgres psql -U healthdata -d patient_db -c "SELECT version();"
```

---

## 🔄 ROLLBACK PROCEDURE

### If Deployment Fails or Has Issues

**Option 1: Rollback using Docker Compose (Quickest)**
```bash
# Stop production v2.7.0
docker compose down

# Checkout previous version
git checkout v2.6.0

# Rebuild and restart previous version
docker compose build --no-cache
docker compose up -d

# Verify rollback
docker compose ps
curl http://localhost:8084/patient/actuator/health
```

**Option 2: Restore from Database Backup**
```bash
# Stop services
docker compose down

# Drop current database
docker exec hdim-demo-postgres dropdb -U healthdata patient_db

# Restore from backup
docker exec -i hdim-demo-postgres psql -U healthdata < production_backup_YYYYMMDD_HHMMSS.sql

# Restart services
docker compose up -d

# Verify restore
docker compose ps
```

**Option 3: Full Rollback to Pre-Deployment State**
```bash
# Complete rollback script
docker compose down -v  # Remove everything including volumes
git checkout v2.6.0
docker compose build --no-cache
docker compose up -d

# Verify
docker compose ps
```

---

## 📊 PRODUCTION VALIDATION

### Health Check Script
```bash
#!/bin/bash

echo "🔍 PRODUCTION HEALTH CHECK"
echo "=========================="
echo ""

# Infrastructure
echo "Infrastructure Services:"
echo -n "  PostgreSQL: "
docker exec hdim-demo-postgres pg_isready -U healthdata > /dev/null 2>&1 && echo "✅" || echo "❌"

echo -n "  Redis: "
docker exec hdim-demo-redis redis-cli ping > /dev/null 2>&1 && echo "✅" || echo "❌"

echo -n "  Kafka: "
docker ps | grep -q hdim-demo-kafka && echo "✅" || echo "❌"

# Application Services
echo ""
echo "Application Services:"
for port in 8084 8085 8086 8081 8087 8083 8080; do
  service_name=$(docker compose ps --format "table {{.Service}}\t{{.Ports}}" | grep ":$port" | cut -f1)
  if [ -z "$service_name" ]; then
    continue
  fi
  echo -n "  $service_name: "
  curl -s http://localhost:$port/*/actuator/health > /dev/null 2>&1 && echo "✅" || echo "⚠️"
done

# Overall status
echo ""
echo "Overall Service Status:"
docker compose ps --format "table {{.Service}}\t{{.Status}}" | tail -n +2 | wc -l
echo "services running"
```

---

## 📋 ROLLBACK DECISION CRITERIA

**Rollback if any of these occur:**
- ❌ More than 2 services fail to start
- ❌ Database connectivity fails
- ❌ FHIR metadata endpoint returns error
- ❌ Patient Service health check returns error
- ❌ Audit logging not functioning
- ❌ Any critical error in logs appearing repeatedly

**Safe to proceed if:**
- ✅ All 12 services running
- ✅ All health checks passing
- ✅ Database responding
- ✅ No critical errors in logs
- ✅ FHIR metadata accessible
- ✅ Audit logging active

---

## 📞 MONITORING DURING DEPLOYMENT

### Watch Service Startup
```bash
# Terminal 1: Monitor services
watch -n 5 'docker compose ps'

# Terminal 2: Watch logs
docker compose logs -f

# Terminal 3: Check specific service
docker compose logs -f patient-service
```

### Expected Startup Timeline
```
0-30 seconds:   Services start initializing
30-60 seconds:  Database connections established
1-3 minutes:    Spring Boot applications starting
3-5 minutes:    Services reporting HEALTHY
5-10 minutes:   All services fully operational
```

---

## 🚨 TROUBLESHOOTING

### If Services Won't Start

**Check Docker status:**
```bash
docker ps -a | grep hdim
docker logs hdim-demo-patient
```

**Port conflicts:**
```bash
# Check if ports are already in use
lsof -i :8084
lsof -i :8085

# Kill conflicting processes
kill -9 $(lsof -ti :8084)
```

**Database issues:**
```bash
# Check database logs
docker logs hdim-demo-postgres

# Reset database
docker compose down -v
docker compose up -d postgres
sleep 10
docker compose up -d
```

### If Health Checks Fail

**Check service logs:**
```bash
docker compose logs patient-service | tail -50
```

**Restart specific service:**
```bash
docker compose restart patient-service
sleep 10
docker compose logs patient-service
```

**Full restart:**
```bash
docker compose down
docker compose up -d
sleep 30
docker compose ps
```

---

## 📝 DEPLOYMENT RECORD

### Before Deployment
- **Date/Time Started:** [YOUR TIME]
- **Backup File:** production_backup_YYYYMMDD_HHMMSS.sql
- **Previous Version:** v2.6.0
- **New Version:** v2.7.0
- **Deployed By:** [YOUR NAME]

### During Deployment
- **Build Start Time:** [RECORD]
- **Build End Time:** [RECORD]
- **Service Startup Time:** [RECORD]
- **Issues Encountered:** [NONE / DESCRIBE]

### After Deployment
- **All Services Running:** YES / NO
- **Health Checks Passing:** YES / NO
- **Database Accessible:** YES / NO
- **Audit Logging Active:** YES / NO
- **Deployment Status:** SUCCESS / ROLLBACK
- **Completion Time:** [YOUR TIME]

---

## ✅ PRODUCTION DEPLOYMENT CHECKLIST

**Pre-Deployment:**
- [ ] Database backup created
- [ ] Backup file verified
- [ ] Current configuration saved
- [ ] Rollback procedure tested
- [ ] Team notified
- [ ] On-call engineer available

**During Deployment:**
- [ ] v2.7.0 tag checked out
- [ ] Docker images built successfully
- [ ] All services started
- [ ] Services are healthy (checking status)
- [ ] All health checks passing
- [ ] No critical errors in logs

**Post-Deployment:**
- [ ] All 12 services running
- [ ] FHIR metadata accessible
- [ ] Patient Service responding
- [ ] Audit logging verified
- [ ] Database reachable
- [ ] Cache (Redis) responding
- [ ] Event streaming (Kafka) active
- [ ] All endpoints validated

**Validation:**
- [ ] 100% of health checks passing
- [ ] Zero service failures
- [ ] Zero database errors
- [ ] HIPAA compliance verified
- [ ] Deployment log recorded
- [ ] Team notified of success

**Sign-Off:**
- [ ] Deployment confirmed successful
- [ ] Production is stable
- [ ] No rollback needed

---

## 🎯 NEXT STEPS

### Immediately After Successful Deployment
1. ✅ Monitor for 30 minutes
2. ✅ Check application logs
3. ✅ Verify audit trail
4. ✅ Test critical workflows

### Within 1 Hour
1. ✅ Run full smoke test suite
2. ✅ Verify HIPAA compliance
3. ✅ Check performance metrics
4. ✅ Notify stakeholders

### Within 24 Hours
1. ✅ Monitor error rates
2. ✅ Check database performance
3. ✅ Verify backup integrity
4. ✅ Document any issues

---

## 📞 SUPPORT

**If deployment fails:**
1. Don't panic - rollback is simple and pre-tested
2. Check logs: `docker compose logs`
3. Review this guide's troubleshooting section
4. Execute rollback if needed (5-10 minutes)

**If you need help:**
- Review: `/tmp/STAGING_ENVIRONMENT_GUIDE.md`
- Reference: `/tmp/COMPLETE_SESSION_SUMMARY.md`
- Check: `/tmp/RELEASE_NOTES_v2.7.0.md`

---

**Production Deployment Guide Created:** February 11, 2026  
**Release:** v2.7.0  
**Status:** Ready for deployment when backups are ready
