# Quick Fix Guide - Critical Issues

**Based on**: HDIM Docker SaaS Validation Report (Dec 30, 2025)

## 🔴 Critical Issues (Fix Today)

### 1. Add Restart Policies (5 minutes)

**Problem**: All containers have `restart: no` - services won't restart after failure

**Fix**:
```bash
cd hdim-master

# Edit docker-compose.yml
# Add this line to EVERY service:
restart: unless-stopped
```

**Example**:
```yaml
services:
  postgres:
    container_name: healthdata-postgres
    image: postgres:15
    restart: unless-stopped  # <-- ADD THIS LINE
    ports:
      - "5435:5432"
    # ... rest of config
```

**Verify**:
```bash
# Restart services
docker-compose down
docker-compose up -d

# Check restart policy
docker inspect healthdata-postgres --format='{{.HostConfig.RestartPolicy.Name}}'
# Should show: unless-stopped
```

---

### 2. Implement Database Backup (2 hours)

**Problem**: No backup strategy - risk of data loss

**Fix**:
```bash
# 1. Create backup script
sudo mkdir -p /backups/postgres
sudo cat > /scripts/backup-hdim-postgres.sh << 'EOF'
#!/bin/bash
set -e

BACKUP_DIR="/backups/postgres"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

echo "[$(date)] Starting HDIM PostgreSQL backup..."

# Backup database
docker exec healthdata-postgres pg_dump \
  -U healthdata \
  -d healthdata_db \
  --format=custom \
  --file=/tmp/backup_${DATE}.dump

# Copy backup to host
docker cp healthdata-postgres:/tmp/backup_${DATE}.dump \
  ${BACKUP_DIR}/backup_${DATE}.dump

# Clean up container
docker exec healthdata-postgres rm /tmp/backup_${DATE}.dump

# Delete old backups
find ${BACKUP_DIR} -name "*.dump" -mtime +${RETENTION_DAYS} -delete

echo "[$(date)] Backup complete: ${BACKUP_DIR}/backup_${DATE}.dump"
echo "[$(date)] Retention: ${RETENTION_DAYS} days"
EOF

# 2. Make executable
sudo chmod +x /scripts/backup-hdim-postgres.sh

# 3. Test backup
sudo /scripts/backup-hdim-postgres.sh

# 4. Add to cron (daily at 2 AM)
sudo crontab -e
# Add this line:
0 2 * * * /scripts/backup-hdim-postgres.sh >> /var/log/hdim-backup.log 2>&1
```

**Verify**:
```bash
# Check backup exists
ls -lh /backups/postgres/

# Test restore (CAUTION: This will replace current data)
# docker exec -i healthdata-postgres pg_restore \
#   -U healthdata \
#   -d healthdata_db \
#   --clean \
#   /tmp/backup_YYYYMMDD_HHMMSS.dump
```

---

## 🔴 High Priority (Fix This Week)

### 3. Secure Port Exposure (30 minutes)

**Problem**: Database and Redis exposed to all interfaces (0.0.0.0)

**Fix**:
```bash
cd hdim-master

# Edit docker-compose.yml
# Change port bindings to localhost only:
```

**Before**:
```yaml
postgres:
  ports:
    - "5435:5432"  # Exposed to 0.0.0.0

redis:
  ports:
    - "6380:6379"  # Exposed to 0.0.0.0
```

**After**:
```yaml
postgres:
  ports:
    - "127.0.0.1:5435:5432"  # Localhost only

redis:
  ports:
    - "127.0.0.1:6380:6379"  # Localhost only
```

**Or remove ports entirely** (recommended for production):
```yaml
postgres:
  # Remove ports section entirely
  # Access only through Docker network
```

**Verify**:
```bash
# Check port bindings
docker port healthdata-postgres
# Should show: 5432/tcp -> 127.0.0.1:5435

# From host
psql -h localhost -p 5435 -U healthdata -d healthdata_db  # ✅ Works

# From network (should fail)
psql -h <server-ip> -p 5435 -U healthdata -d healthdata_db  # ❌ Fails
```

---

### 4. Verify Secrets Management (1 hour)

**Problem**: Need to ensure secrets aren't hardcoded

**Check**:
```bash
cd hdim-master

# Look for hardcoded passwords in docker-compose.yml
grep -i "password.*:" docker-compose.yml

# Check .env file exists and has proper permissions
ls -la .env
# Should be: -rw------- (600) or -r-------- (400)

# If world-readable:
chmod 600 .env
```

**Fix (if secrets are hardcoded)**:
```bash
# 1. Create .env file (if not exists)
cat > .env << 'EOF'
POSTGRES_PASSWORD=your_secure_password_here
REDIS_PASSWORD=your_secure_redis_password
JWT_SECRET=your_jwt_secret_here
EOF

# 2. Secure it
chmod 600 .env

# 3. Update docker-compose.yml to use environment variables
```

**Before**:
```yaml
postgres:
  environment:
    POSTGRES_PASSWORD: hardcoded_password  # ❌ BAD
```

**After**:
```yaml
postgres:
  environment:
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}  # ✅ GOOD
```

**Verify**:
```bash
# No hardcoded secrets
grep -E "(password|secret|key).*: [a-zA-Z0-9]" docker-compose.yml
# Should return nothing (or only ${VAR} references)
```

---

### 5. Run Multi-Tenant Isolation Tests (4-8 hours)

**Problem**: Multi-tenancy not validated

**Current Status**: Test templates exist but not implemented

**Fix**:
```bash
cd test-harness/validation

# 1. Review the test templates
cat HDIM_DEPLOYMENT_VALIDATION_PROMPT.md
# Search for "multi-tenant-isolation" section

# 2. Implement the tests (copy from prompt template)
# Create: multi-tenant-isolation/tenant-isolation.test.ts

# 3. Run the tests
npm run test:functional
```

**What to Implement**:
1. Database RLS (Row-Level Security) validation
2. API tenant context isolation
3. Cross-tenant data leakage tests
4. Audit logging verification

**See**: `HDIM_DEPLOYMENT_VALIDATION_PROMPT.md` for complete code templates

---

## 🟡 Medium Priority (Next Sprint)

### 6. Optimize Docker Images (4-6 hours)

**Problem**: Images 650MB - 1.63GB (too large)

**Target**: < 400MB per service

**Fix**:
```dockerfile
# Example Dockerfile optimization
# backend/modules/services/quality-measure-service/Dockerfile

# === BEFORE ===
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]

# === AFTER ===
# Multi-stage build
FROM gradle:8.11-jdk21-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine  # JRE, not JDK
WORKDIR /app
COPY --from=builder /app/build/libs/app.jar app.jar

# Optimize JVM for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8087
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**Rebuild**:
```bash
# Rebuild single service
docker-compose build quality-measure-service

# Check new size
docker images | grep quality-measure-service

# Rebuild all services
docker-compose build
```

---

### 7. Configure CPU Prioritization (1 hour)

**Problem**: All services have equal CPU priority

**Fix**:
```yaml
# docker-compose.yml
services:
  postgres:
    cpu_shares: 2048  # Highest priority

  gateway-service:
    cpu_shares: 1536  # High priority

  quality-measure-service:
    cpu_shares: 1024  # Normal priority

  notification-service:
    cpu_shares: 512   # Lower priority
```

**Apply**:
```bash
docker-compose down
docker-compose up -d
```

---

## Validation Checklist

After implementing fixes:

### Immediate Validation
```bash
cd test-harness/validation

# 1. Run smoke tests
npm run test:smoke

# 2. Check restart policies
docker inspect healthdata-postgres --format='{{.HostConfig.RestartPolicy.Name}}'

# 3. Verify backup exists
ls -lh /backups/postgres/

# 4. Check port bindings
docker port healthdata-postgres
docker port healthdata-redis

# 5. Verify secrets not hardcoded
grep -E "(password|secret).*: [a-zA-Z0-9]" ../../hdim-master/docker-compose.yml
```

### Full Validation
```bash
# Once Tier 2-4 tests are implemented:
./run-validation.sh --tier all
```

---

## Quick Reference

### Critical Files
- `hdim-master/docker-compose.yml` - Main configuration
- `hdim-master/.env` - Environment variables
- `/scripts/backup-hdim-postgres.sh` - Backup script
- `test-harness/validation/` - Validation harness

### Important Commands
```bash
# Restart all services
docker-compose restart

# View logs
docker-compose logs -f

# Check health
docker-compose ps

# Run validation
cd test-harness/validation && npm run test:smoke

# Backup database
sudo /scripts/backup-hdim-postgres.sh
```

### Support Resources
- **Full Report**: `reports/HDIM_DOCKER_SAAS_VALIDATION_REPORT.md`
- **Implementation Guide**: `HDIM_DEPLOYMENT_VALIDATION_PROMPT.md`
- **Test Documentation**: `README.md`

---

**Priority Order**:
1. ✅ Add restart policies (5 min)
2. ✅ Implement backup (2 hours)
3. ✅ Secure ports (30 min)
4. ✅ Verify secrets (1 hour)
5. ⏳ Multi-tenant tests (4-8 hours)
6. ⏳ Optimize images (4-6 hours)
7. ⏳ CPU prioritization (1 hour)

**Total Estimated Time**: 1-2 days

---

*Last Updated: December 30, 2025*
