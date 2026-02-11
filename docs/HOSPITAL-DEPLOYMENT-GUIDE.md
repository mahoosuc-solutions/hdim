# Hospital Deployment Guide for HDIM Platform

**Version:** 1.0
**Date:** February 1, 2026
**Audience:** Hospital IT Teams, Clinical Leadership

---

## 📋 Table of Contents

1. [Pre-Deployment Checklist](#pre-deployment-checklist)
2. [System Requirements](#system-requirements)
3. [Deployment Procedures](#deployment-procedures)
4. [Configuration & Integration](#configuration--integration)
5. [HIPAA Compliance Setup](#hipaa-compliance-setup)
6. [Operational Runbooks](#operational-runbooks)
7. [Support & Escalation](#support--escalation)
8. [Troubleshooting Guide](#troubleshooting-guide)

---

## Pre-Deployment Checklist

### Hospital IT Preparation (2 Weeks Before)

- [ ] Assign HDIM project manager
- [ ] Assign IT operations lead
- [ ] Assign database administrator
- [ ] Assign security officer
- [ ] Schedule hospital network assessment
- [ ] Verify internet bandwidth (minimum 100 Mbps)
- [ ] Plan data migration from existing systems
- [ ] Document existing EHR integrations
- [ ] Review HIPAA policies with compliance team
- [ ] Schedule security penetration testing

### HDIM Team Preparation

- [ ] Assign deployment lead
- [ ] Assign on-call support engineer
- [ ] Prepare hospital-specific configuration
- [ ] Coordinate EHR integration testing
- [ ] Schedule pre-deployment walkthrough
- [ ] Prepare data migration scripts
- [ ] Finalize SLA agreements
- [ ] Schedule clinical team training

### Infrastructure Verification

- [ ] Network connectivity tested (hospital ↔ HDIM)
- [ ] Database connectivity working (PostgreSQL)
- [ ] Docker registry accessibility confirmed
- [ ] SSL/TLS certificates installed
- [ ] Firewall rules configured
- [ ] NTP time synchronization verified
- [ ] DNS resolution tested
- [ ] Load balancer (if applicable) configured

---

## System Requirements

### Minimum Infrastructure

**Compute:**
- 4 CPU cores (minimum), 8 cores (recommended)
- 16 GB RAM (minimum), 32 GB RAM (recommended)
- 500 GB SSD storage (minimum), 1 TB (recommended)

**Network:**
- 100 Mbps sustained bandwidth (minimum)
- <50ms latency to hospital network
- Redundant internet connectivity (for production)

**Database:**
- PostgreSQL 14+ (compatible with 16)
- 100 GB initial storage allocation
- Automated backup capability

**Load Balancing (Production):**
- Layer 7 (application) load balancer
- TLS termination
- Health check configuration

### Recommended Deployment Architecture

```
[Hospital Network]
        ↓
[Firewall + Load Balancer]
        ↓
[Docker Host 1 (Primary)]
        ↓
[Shared PostgreSQL Database]
        ↓
[Backup Database (optional)]
```

### Software Requirements

- Docker Engine 24.0+
- Docker Compose 2.20+
- PostgreSQL 14+ (or use Docker image)
- TLS 1.3 support in all components
- OpenTelemetry-compatible monitoring

---

## Deployment Procedures

### Step 1: Network Preparation (2 Hours)

```bash
# Verify hospital network connectivity
ping -c 4 your-hospital-network.example.com

# Test DNS resolution
nslookup hdim-api.hospital.example.com

# Test bandwidth (using iperf)
iperf -s  # On HDIM side
iperf -c your-hospital-network.example.com -t 60  # On hospital side
# Expected: >100 Mbps throughput

# Configure firewall rules
# Allow ports: 443 (HTTPS), 5435 (PostgreSQL internal)
# Block all other inbound ports
```

### Step 2: Database Setup (1 Hour)

**Option A: Hospital-Managed PostgreSQL**
```bash
# Create databases for all 29 HDIM schemas
createdb -U postgres hdim_patient_db
createdb -U postgres hdim_caregap_db
# ... (repeat for all 29 databases)

# Create HDIM service user
createuser hdim_service -P
# Grant permissions (HDIM team provides SQL script)
```

**Option B: Docker-Based PostgreSQL**
```bash
# Use provided docker-compose configuration
docker compose up -d postgres

# Verify database is ready
docker compose exec postgres \
  psql -U healthdata -d patient_db -c "\dt"
```

### Step 3: Configure HDIM Services (1-2 Hours)

**Create environment configuration:**
```bash
# Copy template configuration
cp docker-compose.example.yml docker-compose.yml

# Edit configuration for hospital environment
nano docker-compose.yml

# Key variables to set:
# - POSTGRES_PASSWORD: (your-secure-password)
# - SPRING_PROFILES_ACTIVE: hospital-prod
# - SERVER_SERVLET_CONTEXT_PATH: /hdim
# - LOGGING_LEVEL_ROOT: INFO
# - HIPAA_ENABLED: true
```

**Configure hospital-specific integration:**
```bash
# EHR System Connection (e.g., Epic/Cerner)
# FHIR_SERVER_URL: https://ehr.hospital.example.com/fhir/r4
# EHR_OAUTH_CLIENT_ID: (provided by hospital IT)
# EHR_OAUTH_CLIENT_SECRET: (secure vault)

# SSO Configuration (if using hospital AD/SSO)
# SPRING_SECURITY_OAUTH2_CLIENT_ID: (hospital SSO)
# SPRING_SECURITY_OAUTH2_CLIENT_SECRET: (secure vault)

# HIPAA Compliance
# HIPAA_PHI_ENCRYPTION_KEY: (secure vault)
# AUDIT_LOG_RETENTION_DAYS: 2190 (6 years)
```

### Step 4: Start Services (30 Minutes)

```bash
# Start all services
docker compose up -d

# Wait for services to initialize (2-3 minutes)
sleep 180

# Verify all services running
docker compose ps

# Expected output (all in "Up" state):
# postgres              Up
# patient-service      Up
# care-gap-service     Up
# fhir-service         Up
# gateway              Up
# (and 46 other services...)
```

### Step 5: Database Migration (30 Minutes)

```bash
# Run Liquibase migrations for all databases
docker compose exec patient-service \
  java -jar app.jar \
  --spring.liquibase.enabled=true \
  --spring.jpa.hibernate.ddl-auto=validate

# Verify migrations completed
docker compose logs patient-service | grep "Successfully created"

# Check database schema
docker compose exec postgres \
  psql -U healthdata -d patient_db -c "\dt"

# Expected: 20+ tables in patient_db, similarly for other databases
```

### Step 6: Verify Deployment (1 Hour)

```bash
# 1. Check API Gateway health
curl -k https://localhost:8001/hdim/actuator/health
# Expected response: {"status":"UP"}

# 2. Check all service health
curl -k https://localhost:8001/hdim/actuator/health/liveness
# Expected: all services "UP"

# 3. Test authentication
curl -X POST -k https://localhost:8001/hdim/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin@hospital.com",
    "password": "temp-password"
  }'
# Expected: JWT token returned

# 4. Test patient data access
curl -k https://localhost:8001/hdim/api/v1/patients \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Tenant-ID: hospital-123"
# Expected: [] (empty array, no patients yet)

# 5. Verify Swagger UI accessible
# Visit: https://hospital-hdim-server.example.com/hdim/swagger-ui/index.html
# You should see API documentation for all 62 endpoints
```

### Step 7: Clinical Team Access (30 Minutes)

```bash
# Create initial admin user
docker compose exec patient-service \
  java -jar app.jar \
  --spring.batch.job.names=CreateAdminUser

# Set admin user password (will be emailed/securely provided)

# Verify admin can login
# Visit: https://hospital-hdim-server.example.com/hdim
# Click "Login"
# Enter: admin@hospital.com / (provided password)
# Expected: Dashboard loads with 0 patients, 0 measures
```

---

## Configuration & Integration

### EHR System Integration

**Epic Integration Example:**
```yaml
# application.yml
spring:
  fhir:
    server:
      base-url: https://epic.hospital.example.com/api/FHIR/STU3
      client-id: ${EPIC_CLIENT_ID}
      client-secret: ${EPIC_CLIENT_SECRET}
    auth:
      token-url: https://epic.hospital.example.com/oauth/authorize
```

**Cerner Integration Example:**
```yaml
spring:
  fhir:
    server:
      base-url: https://cerner.hospital.example.com/fhir/r4
      client-id: ${CERNER_CLIENT_ID}
      client-secret: ${CERNER_CLIENT_SECRET}
```

**Validate Integration:**
```bash
# Test FHIR connectivity
curl -k "https://hospital-hdim.example.com/hdim/api/v1/fhir/Patient?_count=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Tenant-ID: hospital-123"

# Expected: FHIR resources returned from EHR
```

### Single Sign-On (SSO) Configuration

**Hospital Active Directory Integration:**
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          hospital-ad:
            client-id: ${AD_CLIENT_ID}
            client-secret: ${AD_CLIENT_SECRET}
            scope: openid,profile,email
            authorization-grant-type: authorization_code
        provider:
          hospital-ad:
            issuer-uri: https://ad.hospital.example.com
            authorization-uri: https://ad.hospital.example.com/oauth/authorize
            token-uri: https://ad.hospital.example.com/oauth/token
            user-info-uri: https://ad.hospital.example.com/oauth/userinfo
```

### Data Import Procedures

**Import Patient Cohort (CSV format):**
```bash
# Prepare CSV file
cat > patients.csv << 'EOF'
patient_id,mrn,first_name,last_name,date_of_birth
MRN-001,001234567,John,Doe,1965-03-15
MRN-002,001234568,Jane,Smith,1972-07-22
MRN-003,001234569,Robert,Johnson,1958-11-08
EOF

# Upload via API
curl -X POST \
  -F "file=@patients.csv" \
  "https://hospital-hdim.example.com/hdim/api/v1/patients/bulk-import" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Tenant-ID: hospital-123"
```

---

## HIPAA Compliance Setup

### Encryption Configuration

**Data at Rest (PostgreSQL):**
```sql
-- Enable PostgreSQL encryption plugin
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Create encrypted column for PII
ALTER TABLE patients ADD COLUMN
  first_name_encrypted bytea;

-- Encrypt existing data
UPDATE patients SET first_name_encrypted =
  pgp_pub_encrypt(first_name, dearmor(pubkey))
  WHERE first_name IS NOT NULL;
```

**Data in Transit (TLS):**
```yaml
server:
  ssl:
    key-store: /etc/hdim/keystore.jks
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: JKS
    key-alias: hdim-cert
    enabled: true
    protocol: TLSv1.3
```

### Audit Logging Setup

**Configure audit persistence:**
```yaml
logging:
  level:
    com.healthdata.audit: INFO
  file:
    name: /var/log/hdim/audit.log
    max-size: 100MB
    max-history: 2190  # 6 years for HIPAA

audit:
  events:
    enabled: true
    retention-days: 2190
    encryption: true
    index-by-patient: true
```

**Monitor audit logs:**
```bash
# View recent audit events
docker compose exec patient-service \
  tail -f /var/log/hdim/audit.log | \
  grep "PATIENT_ACCESS\|PHI_VIEW\|DATA_DOWNLOAD"

# Export audit report (HIPAA requirement)
curl -X GET \
  "https://hospital-hdim.example.com/hdim/api/v1/audit/report?startDate=2026-01-01" \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -H "X-Tenant-ID: hospital-123" \
  > audit-report-jan-2026.csv
```

### Access Control Configuration

**Role-Based Access Control (RBAC):**
```yaml
spring:
  security:
    roles:
      - SUPER_ADMIN      # Full system access
      - HOSPITAL_ADMIN   # Hospital-level admin
      - CLINICAL_LEAD    # Department/measure lead
      - QUALITY_ANALYST  # Quality measure analysis
      - CLINICIAN        # View own patient's measures
      - VIEWER           # Read-only access
```

**Example: Restrict Patient View by Department**
```yaml
security:
  patient-access:
    by-department: true
    clinician-scope: "own-patients"
    analyst-scope: "assigned-measures"
```

---

## Operational Runbooks

### Daily Operations

**Morning Health Check (5 minutes):**
```bash
#!/bin/bash
# Check all services are running
docker compose ps | grep -i "up"

# Check database connectivity
docker compose exec postgres psql -U healthdata -d patient_db -c "SELECT 1;"

# Check API gateway health
curl -sk https://localhost:8001/hdim/actuator/health

# Check disk space
df -h | grep sda1  # Should be <80%

# Check error rates
docker compose logs patient-service | grep -i error | tail -5
```

**Weekly Tasks:**
- [ ] Review audit logs for anomalies
- [ ] Check backup completion logs
- [ ] Verify SSL/TLS certificate expiration (>30 days remaining)
- [ ] Monitor performance metrics (response times, throughput)
- [ ] Test backup restoration procedure

**Monthly Tasks:**
- [ ] Full database integrity check
- [ ] Security patch updates
- [ ] Capacity planning review
- [ ] Performance optimization analysis
- [ ] HIPAA compliance audit review

### Backup & Recovery

**Automated Daily Backup:**
```bash
#!/bin/bash
# Backup all 29 HDIM databases
BACKUP_DIR="/backups/hdim-$(date +%Y%m%d)"
mkdir -p $BACKUP_DIR

for db in patient_db caregap_db fhir_db quality_db ...; do
  pg_dump -U healthdata $db | \
    gzip > $BACKUP_DIR/$db.sql.gz
done

# Verify backup integrity
for backup in $BACKUP_DIR/*.sql.gz; do
  gzip -t $backup && echo "✓ $backup OK" || echo "✗ $backup FAILED"
done

# Encrypt backup
gpg --encrypt --recipient backup@hospital.example.com $BACKUP_DIR/*

# Upload to secure location
aws s3 cp $BACKUP_DIR/*.gpg \
  s3://hospital-hdim-backups/$(date +%Y-%m-%d)/
```

**Disaster Recovery Procedure:**
```bash
#!/bin/bash
# 1. Stop all services
docker compose down

# 2. Restore databases from backup
BACKUP_FILE="hdim-backup-2026-02-01.sql.gz"
gunzip < $BACKUP_FILE | psql -U healthdata patient_db

# 3. Verify restore
docker compose exec postgres \
  psql -U healthdata patient_db -c "SELECT COUNT(*) FROM patients;"

# 4. Restart services
docker compose up -d

# 5. Verify functionality
curl -sk https://localhost:8001/hdim/actuator/health
```

---

## Support & Escalation

### Support Contact Information

| Role | Contact | Availability |
|------|---------|--------------|
| **Deployment Lead** | aaron@mahoosuc.solutions | During go-live (24h first week) |
| **Support Engineer** | aaron@mahoosuc.solutions | 24/7 on-call rotation |
| **Hospital IT Contact** | [Assigned by hospital] | Business hours + on-call |
| **Escalation Manager** | aaron@mahoosuc.solutions | During incidents |

### SLA Definitions

| Severity | Issue | Response Time | Resolution Time |
|----------|-------|---------------|----|
| **Critical** 🔴 | System down, data loss | 15 minutes | 2 hours |
| **High** 🟠 | Major feature broken | 1 hour | 4 hours |
| **Medium** 🟡 | Feature degradation | 4 hours | 1 day |
| **Low** 🟢 | Minor bug, documentation | 1 day | 5 days |

### Incident Escalation Path

```
Hospital User
    ↓
HDIM Support (aaron@mahoosuc.solutions)
    ↓ (if not resolved in 2h)
Aaron Bentley, Founder
    ↓ (if not resolved in 8h)
Hospital Chief Medical Information Officer
```

### Submitting Support Tickets

```bash
# Option 1: Email
To: aaron@mahoosuc.solutions
Subject: [HOSPITAL-NAME] [SEVERITY] Issue description
Body:
  Service Affected: [service name]
  Severity: Critical/High/Medium/Low
  Description: [detailed description]
  Steps to Reproduce: [steps]
  Impact: [business impact]
  Logs: [relevant log excerpts]

# Option 2: Support Portal
https://hospital-hdim.example.com/hdim/support
- Submit ticket form
- Attach logs and screenshots
- Track ticket status in real-time

# Option 3: Slack (if configured)
#hdim-support channel → Direct message support bot
```

---

## Troubleshooting Guide

### Service Won't Start

**Symptom:** Docker container exits immediately after starting

**Diagnosis:**
```bash
docker compose logs patient-service --tail=50

# Look for:
# - Connection refused (database not running)
# - OutOfMemoryError (increase Docker memory)
# - Port already in use (kill existing process)
```

**Solution:**
```bash
# Check database connectivity
docker compose logs postgres

# Verify database is running
docker compose exec postgres pg_isready

# Check logs for permission errors
docker compose logs patient-service | grep "Permission denied"

# Restart the service
docker compose restart patient-service
```

### Database Migration Failures

**Symptom:** Liquibase migration hangs or fails

**Diagnosis:**
```bash
docker compose logs patient-service | grep -i "liquibase\|migration"

# Check for locked tables
docker compose exec postgres psql -U healthdata -d patient_db -c \
  "SELECT * FROM information_schema.table_locks WHERE is_explicit = true;"
```

**Solution:**
```bash
# Kill blocking queries
docker compose exec postgres psql -U healthdata -d patient_db << EOF
SELECT pid, query FROM pg_stat_activity WHERE state != 'idle';
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state != 'idle';
EOF

# Retry migration
docker compose restart patient-service
```

### High Memory Usage

**Symptom:** Docker services consuming >90% available memory

**Diagnosis:**
```bash
docker stats

# Expected:
# CONTAINER  MEM USAGE  LIMIT
# patient-service  1.2G  2G (OK)
# patient-service  3.8G  2G (PROBLEM)
```

**Solution:**
```bash
# Increase Docker memory limit
# Edit docker-compose.yml
deploy:
  resources:
    limits:
      memory: 4G

# Restart services
docker compose up -d --force-recreate
```

### API Responding Slowly

**Symptom:** API endpoints responding in >5 seconds

**Diagnosis:**
```bash
# Check service logs
docker compose logs patient-service | grep "duration="

# Check database performance
docker compose exec postgres psql -U healthdata -d patient_db << EOF
SELECT query, calls, mean_time FROM pg_stat_statements
ORDER BY mean_time DESC LIMIT 10;
EOF

# Check network latency
ping -c 4 hospital-database.example.com | grep "avg"
```

**Solution:**
```bash
# Check indexes are present
docker compose exec postgres psql -U healthdata -d patient_db << EOF
\d patients  -- List table and indexes
EOF

# Add missing indexes
docker compose exec postgres psql -U healthdata -d patient_db << EOF
CREATE INDEX idx_patients_tenant_id ON patients(tenant_id);
ANALYZE;
EOF

# Restart services to clear caches
docker compose restart
```

### Login Not Working

**Symptom:** "Invalid credentials" or "User not found"

**Diagnosis:**
```bash
# Check if user exists
docker compose exec postgres psql -U healthdata -d patient_db << EOF
SELECT email, role FROM users WHERE email = 'admin@hospital.com';
EOF

# Check auth service logs
docker compose logs gateway | grep -i "authentication\|unauthorized"
```

**Solution:**
```bash
# Create user manually if missing
docker compose exec patient-service \
  java -jar app.jar \
  --spring.batch.job.names=CreateUser \
  --user.email=admin@hospital.com \
  --user.role=HOSPITAL_ADMIN

# Check SSO configuration if using external auth
docker compose logs gateway | grep -i "oauth\|sso"
```

---

## Monitoring & Maintenance

### Performance Monitoring Dashboard

**Access Grafana:**
```
URL: https://hospital-hdim.example.com:3001
Default credentials: admin / admin (change immediately!)
```

**Key Metrics to Monitor:**
- API response times (target: <500ms p95)
- Database query times (target: <100ms p95)
- Service error rates (target: <0.1%)
- Kafka queue depth (target: <1,000 messages)
- Cache hit rate (target: >80%)
- Disk usage (alert if >80%)
- Memory usage (alert if >85%)

### Log Aggregation

**Access logs:**
```bash
# View real-time logs
docker compose logs -f patient-service

# Search logs for errors
docker compose logs | grep -i "error\|exception\|failed"

# Filter by patient
docker compose logs | grep "patient_id=12345"
```

---

## Go-Live Checklist

**24 Hours Before Go-Live**
- [ ] Final backup created and verified
- [ ] All team members briefed
- [ ] Support team standing by
- [ ] Hospital clinical staff trained
- [ ] Monitoring dashboards accessible
- [ ] Escalation contacts confirmed

**Deployment Day**
- [ ] Backup of current system
- [ ] Services started successfully
- [ ] Health checks passing
- [ ] First 10 patients loaded successfully
- [ ] Clinical staff can login and run measures
- [ ] Reports generating correctly
- [ ] All monitoring metrics green
- [ ] No errors in logs

**Post-Deployment**
- [ ] Daily monitoring for first week
- [ ] Weekly status calls with hospital
- [ ] Performance optimization as needed
- [ ] Documentation of any issues
- [ ] Success celebration! 🎉

---

## Questions?

For technical support: aaron@mahoosuc.solutions
For hospital coordination: aaron@mahoosuc.solutions

---

**Document Version:** 1.0
**Last Updated:** February 1, 2026
**Approval:** ✅ Ready for Hospital Distribution

