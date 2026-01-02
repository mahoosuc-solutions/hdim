# 🚀 Production Deployment Guide - Modular Monolith

## Overview
This guide provides step-by-step instructions for deploying the HealthData Platform modular monolith to production.

## Prerequisites

### Infrastructure Requirements
- **Server**: 4 vCPUs, 8GB RAM minimum
- **Storage**: 100GB SSD
- **OS**: Ubuntu 22.04 LTS or RHEL 8+
- **Docker**: 24.0+ with Docker Compose 2.20+
- **Network**: HTTPS certificate for domain

### Software Requirements
- Java 21 (for local builds)
- PostgreSQL 16 client tools
- Git 2.30+

## 📋 Pre-Deployment Checklist

### 1. Environment Configuration
```bash
# Create production environment file
cat > .env.production <<EOF
# Database
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=healthdata
POSTGRES_USER=healthdata
POSTGRES_PASSWORD=$(openssl rand -base64 32)

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# JWT
JWT_SECRET=$(openssl rand -base64 64)
JWT_EXPIRY=3600000

# Application
SPRING_PROFILES_ACTIVE=production
SERVER_PORT=8080
MANAGEMENT_PORT=8081

# Monitoring
METRICS_ENABLED=true
TRACING_ENABLED=true
EOF
```

### 2. SSL/TLS Configuration
```bash
# Generate or obtain SSL certificates
mkdir -p ./ssl
# Copy your certificates to ./ssl/
# - certificate.crt
# - private.key
# - ca-bundle.crt (if applicable)
```

## 🔧 Build Process

### Step 1: Clone Repository
```bash
git clone https://github.com/healthdata/platform.git
cd platform/healthdata-platform
```

### Step 2: Build Application
```bash
# Option A: Build with Gradle
./gradlew clean build

# Option B: Build with Docker
docker compose build --no-cache healthdata-platform
```

### Step 3: Run Tests
```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew integrationTest

# Module verification
./gradlew verifyModules
```

## 🚀 Deployment Steps

### Step 1: Prepare Production Docker Compose
```yaml
# docker-compose.production.yml
version: '3.9'

services:
  postgres:
    image: postgres:16-alpine
    container_name: healthdata-postgres
    env_file: .env.production
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backup:/backup
    restart: always
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER}"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: healthdata-redis
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    restart: always
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  healthdata-platform:
    image: healthdata/platform:2.0.0
    container_name: healthdata-platform
    env_file: .env.production
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    ports:
      - "443:8443"
      - "80:8080"
    volumes:
      - ./logs:/app/logs
      - ./ssl:/app/ssl:ro
    restart: always
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s

  nginx:
    image: nginx:alpine
    container_name: healthdata-nginx
    ports:
      - "443:443"
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    depends_on:
      - healthdata-platform
    restart: always

volumes:
  postgres_data:
  redis_data:
```

### Step 2: Deploy Application
```bash
# Deploy with zero downtime
docker compose -f docker-compose.production.yml up -d

# Monitor deployment
docker compose -f docker-compose.production.yml logs -f
```

### Step 3: Database Migration
```bash
# Run database migrations
docker exec healthdata-platform ./gradlew liquibaseMigrate

# Verify schemas
docker exec healthdata-postgres psql -U healthdata -c "
SELECT schema_name FROM information_schema.schemata
WHERE schema_name IN ('patient', 'fhir', 'quality', 'caregap', 'notification', 'audit');
"
```

## 📊 Monitoring Setup

### Health Checks
```bash
# Application health
curl https://your-domain.com/actuator/health

# Database health
docker exec healthdata-postgres pg_isready

# Redis health
docker exec healthdata-redis redis-cli ping
```

### Metrics Endpoints
- Prometheus: `http://localhost:8081/actuator/prometheus`
- Health: `http://localhost:8081/actuator/health`
- Info: `http://localhost:8081/actuator/info`
- Metrics: `http://localhost:8081/actuator/metrics`

### Log Aggregation
```bash
# Configure log shipping to ELK/Splunk
docker logs healthdata-platform --follow
```

## 🔒 Security Hardening

### 1. Network Security
```bash
# Configure firewall
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
```

### 2. Database Security
```sql
-- Create read-only user for reporting
CREATE USER healthdata_readonly WITH PASSWORD 'secure_password';
GRANT CONNECT ON DATABASE healthdata TO healthdata_readonly;
GRANT USAGE ON SCHEMA patient, fhir, quality TO healthdata_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA patient, fhir, quality TO healthdata_readonly;
```

### 3. Application Security
```yaml
# Add to application.yml
security:
  headers:
    frame-options: DENY
    xss-protection: "1; mode=block"
    content-type-options: nosniff
  cors:
    allowed-origins: ["https://your-domain.com"]
    allowed-methods: ["GET", "POST", "PUT", "DELETE"]
```

## 🔄 Backup & Recovery

### Automated Backups
```bash
# Create backup script
cat > backup.sh <<'EOF'
#!/bin/bash
BACKUP_DIR="/backup/$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR

# Database backup
docker exec healthdata-postgres pg_dump -U healthdata healthdata > $BACKUP_DIR/database.sql

# Application data
docker cp healthdata-platform:/app/data $BACKUP_DIR/app-data

# Compress
tar -czf $BACKUP_DIR.tar.gz $BACKUP_DIR
rm -rf $BACKUP_DIR

# Keep only last 30 days
find /backup -name "*.tar.gz" -mtime +30 -delete
EOF

# Schedule with cron
crontab -e
# Add: 0 2 * * * /path/to/backup.sh
```

### Recovery Procedure
```bash
# Restore database
docker exec -i healthdata-postgres psql -U healthdata < backup.sql

# Restore application data
docker cp backup/app-data healthdata-platform:/app/data
```

## 📈 Performance Tuning

### JVM Options
```dockerfile
ENV JAVA_OPTS="-Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/app/logs"
```

### Database Tuning
```sql
-- PostgreSQL configuration
ALTER SYSTEM SET shared_buffers = '2GB';
ALTER SYSTEM SET effective_cache_size = '6GB';
ALTER SYSTEM SET maintenance_work_mem = '512MB';
ALTER SYSTEM SET max_connections = 200;
```

### Redis Tuning
```redis
# redis.conf
maxmemory 1gb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
```

## 🚨 Rollback Plan

### Quick Rollback
```bash
# Tag current version
docker tag healthdata/platform:2.0.0 healthdata/platform:rollback

# If issues occur, rollback
docker compose -f docker-compose.production.yml down
docker tag healthdata/platform:1.9.0 healthdata/platform:2.0.0
docker compose -f docker-compose.production.yml up -d
```

### Database Rollback
```bash
# Restore previous backup
docker exec -i healthdata-postgres psql -U healthdata < previous_backup.sql
```

## ✅ Post-Deployment Validation

### 1. Functional Tests
```bash
# Test patient API
curl -X GET https://your-domain.com/api/patients

# Test quality measures
curl -X POST https://your-domain.com/api/measures/calculate

# Test health check
curl https://your-domain.com/actuator/health
```

### 2. Performance Tests
```bash
# Load testing with Apache Bench
ab -n 1000 -c 10 https://your-domain.com/api/patients

# Expected results:
# - Response time: <10ms
# - Throughput: >1000 req/s
# - Error rate: <0.1%
```

### 3. Monitoring Verification
- ✅ All health checks passing
- ✅ Metrics being collected
- ✅ Logs aggregating properly
- ✅ Alerts configured

## 📞 Support Escalation

### Issues During Deployment
1. Check logs: `docker logs healthdata-platform`
2. Verify health: `curl /actuator/health`
3. Database connection: `docker exec healthdata-postgres pg_isready`

### Emergency Contacts
- DevOps Team: devops@healthdata.com
- Platform Team: platform@healthdata.com
- On-call: +1-xxx-xxx-xxxx

## 📋 Deployment Checklist

- [ ] Environment variables configured
- [ ] SSL certificates installed
- [ ] Backup system tested
- [ ] Monitoring configured
- [ ] Security hardening applied
- [ ] Load testing completed
- [ ] Rollback plan verified
- [ ] Documentation updated
- [ ] Team notified

---

## 🎉 Success Criteria

The deployment is considered successful when:

1. **All services healthy** for 30+ minutes
2. **Response times** < 10ms (p95)
3. **Error rate** < 0.1%
4. **Memory usage** < 80%
5. **CPU usage** < 70%
6. **All integration tests** passing

---

*Last Updated: December 2024*
*Version: 2.0.0*