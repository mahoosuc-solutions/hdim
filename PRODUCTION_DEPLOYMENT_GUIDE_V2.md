# Production Deployment Guide

**Application:** HealthData-in-Motion Clinical Portal
**Version:** 1.0.0
**Document Version:** 2.0
**Last Updated:** _____________

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Build Instructions](#2-build-instructions)
3. [Database Migration](#3-database-migration)
4. [Environment Configuration](#4-environment-configuration)
5. [Docker Deployment](#5-docker-deployment)
6. [Kubernetes Deployment](#6-kubernetes-deployment)
7. [Security Configuration](#7-security-configuration)
8. [Monitoring Setup](#8-monitoring-setup)
9. [Backup Configuration](#9-backup-configuration)
10. [Deployment Checklist](#10-deployment-checklist)
11. [Rollback Procedures](#11-rollback-procedures)
12. [Post-Deployment Verification](#12-post-deployment-verification)

---

## 1. Prerequisites

### 1.1 Infrastructure Requirements

**Hardware (Minimum):**
- CPU: 4 cores
- RAM: 16 GB
- Disk: 100 GB SSD
- Network: 1 Gbps

**Hardware (Recommended):**
- CPU: 8 cores
- RAM: 32 GB
- Disk: 500 GB SSD
- Network: 1 Gbps

### 1.2 Software Requirements

**Operating System:**
- Ubuntu 22.04 LTS or RHEL 8+
- Docker Engine 24.0+
- Docker Compose 2.20+
- Or Kubernetes 1.28+

**Development Tools (Build Server):**
- Node.js v20 LTS
- npm 10+
- Java JDK 21
- Gradle 8.5+
- Git 2.40+

**Database:**
- PostgreSQL 16+

**Message Queue (Optional):**
- Apache Kafka 3.5+ (if using event streaming)

**API Gateway:**
- Kong 3.4+ or NGINX

### 1.3 Network Requirements

**Ports:**
- 80 (HTTP)
- 443 (HTTPS)
- 5432 (PostgreSQL - internal only)
- 6379 (Redis - internal only)
- 8081 (CQL Engine Service - internal only)
- 8083 (FHIR Service - internal only)
- 8087 (Quality Measure Service - internal only)
- 9092 (Kafka - internal only)

**Firewall Rules:**
- Allow inbound 80, 443 from internet
- Allow internal service communication
- Block all other inbound traffic
- Allow outbound HTTPS for external API calls

### 1.4 SSL/TLS Certificates

**Obtain certificates:**
```bash
# Using Let's Encrypt (certbot)
sudo apt-get install certbot
sudo certbot certonly --standalone -d api.healthdata.example.com -d portal.healthdata.example.com

# Or provide your own certificate
# Place certificates in /etc/ssl/certs/
```

**Certificate files needed:**
- `fullchain.pem` - Full certificate chain
- `privkey.pem` - Private key
- `chain.pem` - Intermediate certificates

---

## 2. Build Instructions

### 2.1 Frontend Build

**Clone repository:**
```bash
git clone https://github.com/your-org/healthdata-in-motion.git
cd healthdata-in-motion
git checkout tags/v1.0.0  # Or specific version
```

**Install dependencies:**
```bash
npm install
```

**Create production environment file:**

Create: `apps/clinical-portal/src/environments/environment.prod.ts`

```typescript
export const environment = {
  production: true,
  apiBaseUrl: 'https://api.healthdata.example.com',
  qualityMeasureServiceUrl: 'https://api.healthdata.example.com/quality-measure',
  cqlEngineServiceUrl: 'https://api.healthdata.example.com/cql-engine',
  fhirServiceUrl: 'https://api.healthdata.example.com/fhir',
  patientServiceUrl: 'https://api.healthdata.example.com/patient',

  // Feature flags
  enableMeasureBuilder: true,
  enableReports: true,
  enableWebSocket: true,

  // Security
  enableCORS: false,
  enableCSP: true,

  // Performance
  enableServiceWorker: true,
  enableLazyLoading: true,

  // Logging
  logLevel: 'error',
  enableAnalytics: true,

  // Session
  sessionTimeout: 3600000, // 1 hour in ms
  sessionWarningTime: 300000, // 5 minutes before timeout

  // API timeouts
  apiTimeout: 30000, // 30 seconds
};
```

**Build frontend:**
```bash
cd apps/clinical-portal

# Production build
npm run build -- --configuration=production

# Verify build output
ls -lh dist/clinical-portal/browser/

# Expected output:
# index.html
# main.[hash].js
# polyfills.[hash].js
# styles.[hash].css
# assets/
```

**Build output location:**
```
dist/clinical-portal/browser/
```

### 2.2 Backend Build

**Build all services:**
```bash
cd backend

# Clean previous builds
./gradlew clean

# Build all services (skip tests for faster build)
./gradlew build -x test

# Or run with tests
./gradlew build

# Build specific service
./gradlew :modules:services:quality-measure-service:build
./gradlew :modules:services:cql-engine-service:build
./gradlew :modules:services:fhir-service:build
```

**Build output locations:**
```
backend/modules/services/quality-measure-service/build/libs/quality-measure-service-1.0.0.jar
backend/modules/services/cql-engine-service/build/libs/cql-engine-service-1.0.0.jar
backend/modules/services/fhir-service/build/libs/fhir-service-1.0.0.jar
```

### 2.3 Docker Image Build

**Build frontend Docker image:**

Create: `apps/clinical-portal/Dockerfile.prod`

```dockerfile
# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./
COPY apps/clinical-portal/package*.json ./apps/clinical-portal/

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build application
RUN npm run build -- --project=clinical-portal --configuration=production

# Stage 2: Production
FROM nginx:alpine

# Copy custom nginx config
COPY apps/clinical-portal/nginx.conf /etc/nginx/nginx.conf

# Copy built application
COPY --from=builder /app/dist/clinical-portal/browser /usr/share/nginx/html

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q --spider http://localhost/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
```

Create: `apps/clinical-portal/nginx.conf`

```nginx
events {
  worker_connections 1024;
}

http {
  include /etc/nginx/mime.types;
  default_type application/octet-stream;

  # Logging
  access_log /var/log/nginx/access.log;
  error_log /var/log/nginx/error.log;

  # Performance
  sendfile on;
  tcp_nopush on;
  tcp_nodelay on;
  keepalive_timeout 65;
  types_hash_max_size 2048;
  client_max_body_size 20M;

  # Gzip compression
  gzip on;
  gzip_vary on;
  gzip_min_length 1024;
  gzip_types text/plain text/css text/xml text/javascript application/json application/javascript application/xml+rss application/rss+xml font/truetype font/opentype application/vnd.ms-fontobject image/svg+xml;

  server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;

    # Cache control for static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
      expires 1y;
      add_header Cache-Control "public, immutable";
    }

    # No cache for index.html
    location /index.html {
      add_header Cache-Control "no-cache, no-store, must-revalidate";
    }

    # Health check endpoint
    location /health {
      access_log off;
      return 200 "healthy\n";
      add_header Content-Type text/plain;
    }

    # Angular routing
    location / {
      try_files $uri $uri/ /index.html;
    }

    # API proxy (if needed)
    location /api/ {
      proxy_pass http://api-gateway:8080/;
      proxy_http_version 1.1;
      proxy_set_header Upgrade $http_upgrade;
      proxy_set_header Connection 'upgrade';
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
      proxy_cache_bypass $http_upgrade;
    }
  }
}
```

**Build Docker images:**
```bash
# Frontend
docker build -f apps/clinical-portal/Dockerfile.prod -t healthdata/clinical-portal:1.0.0 .

# Backend services (use existing Dockerfiles)
cd backend

# Quality Measure Service
docker build -f Dockerfile \
  --build-arg SERVICE_NAME=quality-measure-service \
  -t healthdata/quality-measure-service:1.0.0 .

# CQL Engine Service
docker build -f Dockerfile \
  --build-arg SERVICE_NAME=cql-engine-service \
  -t healthdata/cql-engine-service:1.0.0 .

# Tag images for registry
docker tag healthdata/clinical-portal:1.0.0 registry.example.com/healthdata/clinical-portal:1.0.0
docker tag healthdata/quality-measure-service:1.0.0 registry.example.com/healthdata/quality-measure-service:1.0.0
docker tag healthdata/cql-engine-service:1.0.0 registry.example.com/healthdata/cql-engine-service:1.0.0

# Push to registry
docker push registry.example.com/healthdata/clinical-portal:1.0.0
docker push registry.example.com/healthdata/quality-measure-service:1.0.0
docker push registry.example.com/healthdata/cql-engine-service:1.0.0
```

---

## 3. Database Migration

### 3.1 Pre-Migration Checklist

- [ ] Backup current database
- [ ] Test migrations on staging
- [ ] Review all changesets
- [ ] Verify rollback procedures
- [ ] Schedule maintenance window
- [ ] Notify stakeholders

### 3.2 Backup Production Database

```bash
# Create backup directory
mkdir -p /backups/pre-migration

# Backup all databases
pg_dump -h postgres.example.com -U healthdata_admin \
  -d healthdata_cql \
  -F c -b -v \
  -f /backups/pre-migration/healthdata_cql_$(date +%Y%m%d_%H%M%S).dump

pg_dump -h postgres.example.com -U healthdata_admin \
  -d healthdata_quality_measure \
  -F c -b -v \
  -f /backups/pre-migration/healthdata_quality_measure_$(date +%Y%m%d_%H%M%S).dump

pg_dump -h postgres.example.com -U healthdata_admin \
  -d healthdata_fhir \
  -F c -b -v \
  -f /backups/pre-migration/healthdata_fhir_$(date +%Y%m%d_%H%M%S).dump

# Verify backups
ls -lh /backups/pre-migration/

# Upload to S3 (or backup storage)
aws s3 cp /backups/pre-migration/ s3://healthdata-backups/pre-migration/ --recursive
```

### 3.3 Run Liquibase Migrations

**Quality Measure Service migrations:**

```bash
cd backend/modules/services/quality-measure-service

# Review pending changesets
./gradlew liquibaseStatus -Pspring.profiles.active=production

# Expected changesets:
# - 0001-initial-schema.xml (if new database)
# - 0002-create-saved-reports-table.xml
# - 0003-create-custom-measures-table.xml

# Run migrations
./gradlew liquibaseUpdate -Pspring.profiles.active=production

# Verify migrations
psql -h postgres.example.com -U healthdata_admin -d healthdata_quality_measure

# Check tables
\dt

# Verify saved_reports table
\d saved_reports

# Verify custom_measures table
\d custom_measures

# Check migration history
SELECT * FROM databasechangelog ORDER BY dateexecuted DESC LIMIT 10;
```

**CQL Engine Service migrations:**

```bash
cd backend/modules/services/cql-engine-service

# Run migrations
./gradlew liquibaseUpdate -Pspring.profiles.active=production

# Verify
psql -h postgres.example.com -U healthdata_admin -d healthdata_cql
\dt
SELECT * FROM databasechangelog ORDER BY dateexecuted DESC LIMIT 10;
```

### 3.4 Verify Database Schema

**Run verification queries:**

```sql
-- Connect to database
psql -h postgres.example.com -U healthdata_admin -d healthdata_quality_measure

-- Verify saved_reports table
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'saved_reports'
ORDER BY ordinal_position;

-- Expected columns:
-- id (uuid, NOT NULL)
-- patient_id (varchar, NULL)
-- measure_id (varchar, NOT NULL)
-- tenant_id (varchar, NOT NULL)
-- report_name (varchar, NOT NULL)
-- report_data (jsonb, NOT NULL)
-- created_date (timestamp, NOT NULL)
-- created_by (varchar, NOT NULL)

-- Verify indexes
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'saved_reports';

-- Expected indexes:
-- saved_reports_pkey
-- idx_saved_reports_tenant_created
-- idx_saved_reports_patient

-- Verify custom_measures table
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'custom_measures'
ORDER BY ordinal_position;

-- Test data insertion
INSERT INTO saved_reports (
  id, patient_id, measure_id, tenant_id, report_name,
  report_data, created_date, created_by
) VALUES (
  gen_random_uuid(),
  'TEST-PATIENT-001',
  'TEST-MEASURE',
  'TENANT001',
  'Migration Test Report',
  '{"test": true}'::jsonb,
  NOW(),
  'migration-test'
);

-- Verify insertion
SELECT * FROM saved_reports WHERE report_name = 'Migration Test Report';

-- Clean up test data
DELETE FROM saved_reports WHERE report_name = 'Migration Test Report';
```

### 3.5 Rollback Migrations (if needed)

```bash
# Rollback last changeset
./gradlew liquibaseRollback -Pspring.profiles.active=production -PliquibaseRollbackCount=1

# Rollback to specific tag
./gradlew liquibaseRollback -Pspring.profiles.active=production -PliquibaseRollbackTag=v1.0.0

# Or restore from backup
pg_restore -h postgres.example.com -U healthdata_admin \
  -d healthdata_quality_measure -c \
  /backups/pre-migration/healthdata_quality_measure_20240115_120000.dump
```

---

## 4. Environment Configuration

### 4.1 Environment Variables

Create: `/home/webemo-aaron/projects/healthdata-in-motion/.env.production`

```bash
# =============================================================================
# PRODUCTION ENVIRONMENT VARIABLES
# =============================================================================

# ----------------------------------------------------------------------------
# Application
# ----------------------------------------------------------------------------
APP_ENV=production
APP_VERSION=1.0.0
APP_PORT=443

# ----------------------------------------------------------------------------
# Database - PostgreSQL
# ----------------------------------------------------------------------------
DB_HOST=postgres.example.com
DB_PORT=5432

# CQL Engine Database
CQL_DB_NAME=healthdata_cql
CQL_DB_USERNAME=healthdata_cql_user
CQL_DB_PASSWORD=<CHANGE_ME_STRONG_PASSWORD>

# Quality Measure Database
QUALITY_DB_NAME=healthdata_quality_measure
QUALITY_DB_USERNAME=healthdata_quality_user
QUALITY_DB_PASSWORD=<CHANGE_ME_STRONG_PASSWORD>

# FHIR Database
FHIR_DB_NAME=healthdata_fhir
FHIR_DB_USERNAME=healthdata_fhir_user
FHIR_DB_PASSWORD=<CHANGE_ME_STRONG_PASSWORD>

# Database Pool Settings
DB_POOL_SIZE=20
DB_MIN_IDLE=5
DB_CONNECTION_TIMEOUT=30000
DB_IDLE_TIMEOUT=600000
DB_MAX_LIFETIME=1800000

# ----------------------------------------------------------------------------
# Redis Cache
# ----------------------------------------------------------------------------
REDIS_HOST=redis.example.com
REDIS_PORT=6379
REDIS_PASSWORD=<CHANGE_ME_REDIS_PASSWORD>
REDIS_SSL=true
REDIS_TTL=3600000  # 1 hour

# ----------------------------------------------------------------------------
# Kafka (if using event streaming)
# ----------------------------------------------------------------------------
KAFKA_BOOTSTRAP_SERVERS=kafka-1.example.com:9093,kafka-2.example.com:9093,kafka-3.example.com:9093
KAFKA_SECURITY_PROTOCOL=SASL_SSL
KAFKA_SASL_MECHANISM=SCRAM-SHA-256
KAFKA_SASL_JAAS_CONFIG=org.apache.kafka.common.security.scram.ScramLoginModule required username="healthdata" password="<CHANGE_ME_KAFKA_PASSWORD>";

# ----------------------------------------------------------------------------
# Security - JWT
# ----------------------------------------------------------------------------
# Generate with: openssl rand -base64 64
JWT_SECRET=<CHANGE_ME_GENERATE_WITH_OPENSSL_RAND_BASE64_64>
JWT_EXPIRATION=3600000  # 1 hour
JWT_REFRESH_EXPIRATION=604800000  # 7 days
JWT_ISSUER=healthdata-in-motion
JWT_AUDIENCE=clinical-portal

# ----------------------------------------------------------------------------
# Security - Encryption
# ----------------------------------------------------------------------------
# Generate with: openssl rand -base64 32
ENCRYPTION_KEY=<CHANGE_ME_GENERATE_WITH_OPENSSL_RAND_BASE64_32>
ENCRYPTION_ALGORITHM=AES-256-GCM

# ----------------------------------------------------------------------------
# HIPAA Compliance
# ----------------------------------------------------------------------------
HIPAA_AUDIT_ENABLED=true
HIPAA_PHI_ENCRYPTION_ENABLED=true
HIPAA_AUTO_LOGOUT_ENABLED=true
HIPAA_AUTO_LOGOUT_TIMEOUT=900000  # 15 minutes
HIPAA_SESSION_TIMEOUT=3600000  # 1 hour
HIPAA_PASSWORD_MIN_LENGTH=12
HIPAA_PASSWORD_REQUIRE_SPECIAL=true
HIPAA_FAILED_LOGIN_LOCKOUT=5
HIPAA_LOCKOUT_DURATION=1800000  # 30 minutes

# ----------------------------------------------------------------------------
# Service URLs (Internal)
# ----------------------------------------------------------------------------
CQL_ENGINE_URL=http://cql-engine-service:8081/cql-engine
QUALITY_MEASURE_URL=http://quality-measure-service:8087/quality-measure
FHIR_SERVICE_URL=http://fhir-service:8083/fhir
PATIENT_SERVICE_URL=http://patient-service:8085/patient

# ----------------------------------------------------------------------------
# Service URLs (External/API Gateway)
# ----------------------------------------------------------------------------
API_GATEWAY_URL=https://api.healthdata.example.com
FRONTEND_URL=https://portal.healthdata.example.com

# ----------------------------------------------------------------------------
# CORS Settings
# ----------------------------------------------------------------------------
CORS_ALLOWED_ORIGINS=https://portal.healthdata.example.com
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS
CORS_ALLOWED_HEADERS=Authorization,Content-Type,X-Requested-With
CORS_ALLOW_CREDENTIALS=true
CORS_MAX_AGE=3600

# ----------------------------------------------------------------------------
# Rate Limiting
# ----------------------------------------------------------------------------
RATE_LIMIT_ENABLED=true
RATE_LIMIT_REQUESTS_PER_MINUTE=60
RATE_LIMIT_REQUESTS_PER_HOUR=1000

# ----------------------------------------------------------------------------
# Logging
# ----------------------------------------------------------------------------
LOG_LEVEL=INFO
LOG_LEVEL_APP=INFO
LOG_LEVEL_SPRING=WARN
LOG_LEVEL_HIBERNATE=WARN
LOG_FORMAT=json
LOG_FILE=/var/log/healthdata/application.log
LOG_MAX_SIZE=10MB
LOG_MAX_HISTORY=30

# ----------------------------------------------------------------------------
# Monitoring - Datadog (Optional)
# ----------------------------------------------------------------------------
DATADOG_ENABLED=true
DATADOG_API_KEY=<YOUR_DATADOG_API_KEY>
DATADOG_APP_KEY=<YOUR_DATADOG_APP_KEY>
DATADOG_SITE=datadoghq.com
DATADOG_SERVICE_NAME=healthdata-in-motion
DATADOG_ENV=production

# ----------------------------------------------------------------------------
# Monitoring - Sentry (Optional)
# ----------------------------------------------------------------------------
SENTRY_ENABLED=true
SENTRY_DSN=<YOUR_SENTRY_DSN>
SENTRY_ENVIRONMENT=production
SENTRY_TRACES_SAMPLE_RATE=0.1
SENTRY_RELEASE=1.0.0

# ----------------------------------------------------------------------------
# Email (for notifications)
# ----------------------------------------------------------------------------
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USERNAME=noreply@healthdata.example.com
SMTP_PASSWORD=<CHANGE_ME_SMTP_PASSWORD>
SMTP_FROM=noreply@healthdata.example.com
SMTP_TLS=true

# ----------------------------------------------------------------------------
# Backup
# ----------------------------------------------------------------------------
BACKUP_ENABLED=true
BACKUP_SCHEDULE=0 2 * * *  # Daily at 2 AM
BACKUP_RETENTION_DAYS=30
BACKUP_S3_BUCKET=healthdata-backups-prod
BACKUP_S3_REGION=us-east-1

# ----------------------------------------------------------------------------
# Feature Flags
# ----------------------------------------------------------------------------
FEATURE_MEASURE_BUILDER=true
FEATURE_REPORTS=true
FEATURE_WEBSOCKET=true
FEATURE_BATCH_OPERATIONS=true
FEATURE_EXPORT_EXCEL=true
FEATURE_EXPORT_PDF=false  # Not yet implemented

# ----------------------------------------------------------------------------
# Performance
# ----------------------------------------------------------------------------
THREAD_POOL_SIZE=50
CONNECTION_TIMEOUT=30000
READ_TIMEOUT=30000
WRITE_TIMEOUT=30000

# ----------------------------------------------------------------------------
# SSL/TLS
# ----------------------------------------------------------------------------
SSL_ENABLED=true
SSL_CERT_PATH=/etc/ssl/certs/fullchain.pem
SSL_KEY_PATH=/etc/ssl/certs/privkey.pem
SSL_PROTOCOLS=TLSv1.2,TLSv1.3
SSL_CIPHERS=TLS_AES_128_GCM_SHA256:TLS_AES_256_GCM_SHA384:TLS_CHACHA20_POLY1305_SHA256
```

### 4.2 Secure Environment Variables

**Never commit .env files to git!**

```bash
# Add to .gitignore
echo ".env.production" >> .gitignore
echo ".env.*.local" >> .gitignore
```

**Store secrets securely:**

Option 1: Use AWS Secrets Manager
```bash
aws secretsmanager create-secret \
  --name healthdata/production/database \
  --secret-string '{"username":"healthdata_user","password":"STRONG_PASSWORD"}'

aws secretsmanager create-secret \
  --name healthdata/production/jwt \
  --secret-string '{"secret":"JWT_SECRET_KEY"}'
```

Option 2: Use HashiCorp Vault
```bash
vault kv put secret/healthdata/production/database \
  username=healthdata_user \
  password=STRONG_PASSWORD

vault kv put secret/healthdata/production/jwt \
  secret=JWT_SECRET_KEY
```

Option 3: Kubernetes Secrets
```bash
kubectl create secret generic healthdata-db-secret \
  --from-literal=username=healthdata_user \
  --from-literal=password=STRONG_PASSWORD \
  -n healthdata

kubectl create secret generic healthdata-jwt-secret \
  --from-literal=secret=JWT_SECRET_KEY \
  -n healthdata
```

### 4.3 Generate Secure Keys

```bash
# Generate JWT secret (64 bytes, base64 encoded)
openssl rand -base64 64

# Generate encryption key (32 bytes, base64 encoded)
openssl rand -base64 32

# Generate database passwords
openssl rand -base64 32

# Generate Redis password
openssl rand -base64 24
```

---

## 5. Docker Deployment

See `docker-compose.production.yml` (created separately)

**Deploy with Docker Compose:**

```bash
# Pull latest images
docker-compose -f docker-compose.production.yml pull

# Start services
docker-compose -f docker-compose.production.yml up -d

# View logs
docker-compose -f docker-compose.production.yml logs -f

# Check service health
docker-compose -f docker-compose.production.yml ps

# Scale services
docker-compose -f docker-compose.production.yml up -d --scale quality-measure-service=3
```

---

## 6. Kubernetes Deployment

See `k8s/` directory (created separately)

**Deploy to Kubernetes:**

```bash
# Create namespace
kubectl create namespace healthdata

# Apply configurations
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml

# Check deployment
kubectl get pods -n healthdata
kubectl get services -n healthdata
kubectl get ingress -n healthdata

# View logs
kubectl logs -n healthdata -l app=quality-measure-service --tail=100

# Scale deployment
kubectl scale deployment quality-measure-service --replicas=3 -n healthdata
```

---

## 7. Security Configuration

### 7.1 HTTPS/TLS Setup

**NGINX SSL configuration:**

```nginx
server {
    listen 443 ssl http2;
    server_name api.healthdata.example.com;

    ssl_certificate /etc/ssl/certs/fullchain.pem;
    ssl_certificate_key /etc/ssl/certs/privkey.pem;

    # SSL protocols and ciphers
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers 'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384';
    ssl_prefer_server_ciphers off;

    # SSL session cache
    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:50m;
    ssl_session_tickets off;

    # OCSP stapling
    ssl_stapling on;
    ssl_stapling_verify on;
    ssl_trusted_certificate /etc/ssl/certs/chain.pem;

    # Security headers
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:;" always;

    # Proxy to backend
    location / {
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;

        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name api.healthdata.example.com;
    return 301 https://$server_name$request_uri;
}
```

### 7.2 Firewall Configuration

**UFW (Ubuntu):**

```bash
# Allow SSH
sudo ufw allow 22/tcp

# Allow HTTP and HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Enable firewall
sudo ufw enable

# Check status
sudo ufw status
```

### 7.3 Database Security

```sql
-- Create dedicated user for application
CREATE USER healthdata_quality_user WITH PASSWORD 'STRONG_PASSWORD';

-- Grant minimal permissions
GRANT CONNECT ON DATABASE healthdata_quality_measure TO healthdata_quality_user;
GRANT USAGE ON SCHEMA public TO healthdata_quality_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO healthdata_quality_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO healthdata_quality_user;

-- Revoke public access
REVOKE ALL ON DATABASE healthdata_quality_measure FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM PUBLIC;

-- Enable SSL connections only
ALTER SYSTEM SET ssl = on;
ALTER SYSTEM SET ssl_cert_file = '/etc/ssl/certs/server.crt';
ALTER SYSTEM SET ssl_key_file = '/etc/ssl/private/server.key';

-- Require SSL for connections
ALTER USER healthdata_quality_user SET ssl TO on;

-- Enable audit logging
ALTER SYSTEM SET log_connections = on;
ALTER SYSTEM SET log_disconnections = on;
ALTER SYSTEM SET log_statement = 'mod';  # Log all modifications
```

---

## 8. Monitoring Setup

See `MONITORING_SETUP.md` (created separately)

---

## 9. Backup Configuration

### 9.1 Automated Database Backups

Create: `/usr/local/bin/backup-healthdata.sh`

```bash
#!/bin/bash
# HealthData Database Backup Script

set -e

# Configuration
BACKUP_DIR="/backups/healthdata"
S3_BUCKET="s3://healthdata-backups-prod"
RETENTION_DAYS=30
DATE=$(date +%Y%m%d_%H%M%S)

# Database credentials (loaded from environment or secrets)
DB_HOST="${DB_HOST:-postgres.example.com}"
DB_USER="${DB_ADMIN_USER:-healthdata_admin}"
export PGPASSWORD="${DB_ADMIN_PASSWORD}"

# Create backup directory
mkdir -p $BACKUP_DIR

# Backup each database
for DB in healthdata_cql healthdata_quality_measure healthdata_fhir; do
    echo "Backing up $DB..."

    pg_dump -h $DB_HOST -U $DB_USER -d $DB \
        -F c -b -v \
        -f "$BACKUP_DIR/${DB}_${DATE}.dump"

    # Compress backup
    gzip "$BACKUP_DIR/${DB}_${DATE}.dump"

    echo "Backup complete: ${DB}_${DATE}.dump.gz"
done

# Upload to S3
echo "Uploading backups to S3..."
aws s3 cp $BACKUP_DIR/ $S3_BUCKET/$(date +%Y/%m/%d)/ --recursive

# Clean up old local backups
echo "Cleaning up old backups..."
find $BACKUP_DIR -name "*.dump.gz" -mtime +$RETENTION_DAYS -delete

# Send notification (optional)
echo "Backup completed successfully at $(date)" | mail -s "HealthData Backup Success" ops@healthdata.example.com

echo "All backups completed successfully!"
```

**Set up cron job:**

```bash
# Make script executable
chmod +x /usr/local/bin/backup-healthdata.sh

# Add to crontab
crontab -e

# Run daily at 2 AM
0 2 * * * /usr/local/bin/backup-healthdata.sh >> /var/log/healthdata-backup.log 2>&1
```

---

## 10. Deployment Checklist

### Pre-Deployment

- [ ] Code freeze implemented
- [ ] All tests passing (unit, integration, E2E)
- [ ] Security scan completed
- [ ] Performance testing completed
- [ ] Accessibility audit completed
- [ ] Documentation updated
- [ ] Stakeholders notified
- [ ] Maintenance window scheduled
- [ ] Rollback plan documented
- [ ] Team on standby

### Build

- [ ] Frontend built successfully
- [ ] Backend built successfully
- [ ] Docker images built and tagged
- [ ] Images pushed to registry
- [ ] Build artifacts archived

### Database

- [ ] Database backed up
- [ ] Migrations tested on staging
- [ ] Migrations executed on production
- [ ] Schema verified
- [ ] Indexes created
- [ ] Performance validated

### Configuration

- [ ] Environment variables configured
- [ ] Secrets generated and stored securely
- [ ] SSL certificates installed
- [ ] CORS origins configured
- [ ] Rate limiting configured
- [ ] Logging configured

### Deployment

- [ ] Services deployed (Docker/Kubernetes)
- [ ] Health checks passing
- [ ] Services reachable
- [ ] Load balancer configured
- [ ] DNS updated (if needed)
- [ ] CDN configured (if applicable)

### Security

- [ ] HTTPS enforced
- [ ] Security headers configured
- [ ] Firewall rules applied
- [ ] Database access restricted
- [ ] Secrets rotated
- [ ] Audit logging enabled

### Monitoring

- [ ] APM configured (Datadog/New Relic)
- [ ] Alerts configured
- [ ] Dashboards created
- [ ] Log aggregation working
- [ ] Error tracking enabled (Sentry)
- [ ] Uptime monitoring enabled

### Verification

- [ ] Smoke tests passing
- [ ] Health endpoints responding
- [ ] API endpoints accessible
- [ ] Frontend loads correctly
- [ ] Database queries working
- [ ] Cache working
- [ ] Authentication working
- [ ] Authorization working

### Post-Deployment

- [ ] Monitor error rates
- [ ] Monitor response times
- [ ] Monitor resource usage
- [ ] Verify user access
- [ ] Test critical workflows
- [ ] Backup verified
- [ ] Documentation updated
- [ ] Stakeholders notified
- [ ] Post-mortem scheduled (if issues)

---

## 11. Rollback Procedures

### 11.1 Kubernetes Rollback

```bash
# View deployment history
kubectl rollout history deployment/quality-measure-service -n healthdata

# Rollback to previous version
kubectl rollout undo deployment/quality-measure-service -n healthdata

# Rollback to specific revision
kubectl rollout undo deployment/quality-measure-service --to-revision=2 -n healthdata

# Check rollout status
kubectl rollout status deployment/quality-measure-service -n healthdata
```

### 11.2 Docker Compose Rollback

```bash
# Stop current version
docker-compose -f docker-compose.production.yml down

# Pull previous version
export VERSION=0.9.0
docker-compose -f docker-compose.production.yml pull

# Start previous version
docker-compose -f docker-compose.production.yml up -d

# Verify
docker-compose -f docker-compose.production.yml ps
curl http://localhost:8087/quality-measure/actuator/health
```

### 11.3 Database Rollback

```bash
# Option 1: Liquibase rollback
cd backend/modules/services/quality-measure-service
./gradlew liquibaseRollback -Pspring.profiles.active=production -PliquibaseRollbackCount=1

# Option 2: Restore from backup
pg_restore -h postgres.example.com -U healthdata_admin \
  -d healthdata_quality_measure -c \
  /backups/healthdata_quality_measure_20240115_120000.dump.gz

# Verify
psql -h postgres.example.com -U healthdata_admin -d healthdata_quality_measure
SELECT * FROM databasechangelog ORDER BY dateexecuted DESC LIMIT 10;
```

---

## 12. Post-Deployment Verification

### 12.1 Smoke Tests

Create: `scripts/smoke-tests.sh`

```bash
#!/bin/bash
# Smoke tests for production deployment

set -e

BASE_URL="https://api.healthdata.example.com"
FRONTEND_URL="https://portal.healthdata.example.com"
TOKEN="<jwt-token>"  # Get from login endpoint

echo "Starting smoke tests..."

# Test health endpoints
echo "Testing health endpoints..."
curl -f $BASE_URL/quality-measure/actuator/health || exit 1
curl -f $BASE_URL/cql-engine/actuator/health || exit 1

# Test frontend
echo "Testing frontend..."
curl -f $FRONTEND_URL || exit 1

# Test authentication
echo "Testing authentication..."
curl -f -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test@example.com","password":"testpass"}' || exit 1

# Test API endpoints
echo "Testing API endpoints..."
curl -f -H "Authorization: Bearer $TOKEN" $BASE_URL/quality-measure/reports || exit 1
curl -f -H "Authorization: Bearer $TOKEN" $BASE_URL/quality-measure/results || exit 1

# Test database connectivity
echo "Testing database connectivity..."
curl -f $BASE_URL/quality-measure/actuator/health/db || exit 1

# Test cache connectivity
echo "Testing cache connectivity..."
curl -f $BASE_URL/quality-measure/actuator/health/redis || exit 1

echo "All smoke tests passed!"
```

### 12.2 Functional Testing

**Critical User Flows:**

1. **Login Flow**
   - [ ] User can log in
   - [ ] JWT token received
   - [ ] User redirected to dashboard

2. **Dashboard**
   - [ ] Dashboard loads
   - [ ] Metrics display
   - [ ] Charts render

3. **Patients**
   - [ ] Patient list loads
   - [ ] Can search/filter patients
   - [ ] Can view patient details

4. **Evaluations**
   - [ ] Can run evaluation
   - [ ] Results display correctly
   - [ ] Can view evaluation history

5. **Reports**
   - [ ] Can generate patient report
   - [ ] Can generate population report
   - [ ] Can export to CSV
   - [ ] Can save reports

6. **Measure Builder**
   - [ ] Editor loads
   - [ ] Can create custom measure
   - [ ] Can test measure
   - [ ] Can save measure

### 12.3 Performance Validation

```bash
# Load test critical endpoints
ab -n 100 -c 10 -H "Authorization: Bearer $TOKEN" \
  https://api.healthdata.example.com/quality-measure/reports

# Expected results:
# - Requests per second: > 50
# - Time per request: < 200ms (mean)
# - Failed requests: 0
```

### 12.4 Monitoring Verification

- [ ] Metrics appearing in Datadog/New Relic
- [ ] Logs flowing to log aggregation system
- [ ] Alerts configured and tested
- [ ] Dashboards displaying data
- [ ] Error tracking working in Sentry

---

## Support Contacts

**On-Call Rotation:**
- Primary: oncall-primary@healthdata.example.com
- Secondary: oncall-secondary@healthdata.example.com
- Escalation: engineering-lead@healthdata.example.com

**Runbook Location:**
https://wiki.healthdata.example.com/runbooks/deployment

**Status Page:**
https://status.healthdata.example.com

---

## Appendix

### Useful Commands

**Docker:**
```bash
# View logs
docker logs -f <container_id>

# Execute command in container
docker exec -it <container_id> /bin/bash

# Inspect container
docker inspect <container_id>

# View resource usage
docker stats
```

**Kubernetes:**
```bash
# Get pods
kubectl get pods -n healthdata

# View logs
kubectl logs -f <pod_name> -n healthdata

# Execute command in pod
kubectl exec -it <pod_name> -n healthdata -- /bin/bash

# Port forward
kubectl port-forward <pod_name> 8080:8080 -n healthdata

# View events
kubectl get events -n healthdata --sort-by='.lastTimestamp'
```

**PostgreSQL:**
```bash
# Connect to database
psql -h postgres.example.com -U healthdata_admin -d healthdata_quality_measure

# List databases
\l

# List tables
\dt

# Describe table
\d table_name

# View running queries
SELECT * FROM pg_stat_activity WHERE state = 'active';
```

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-01-01 | DevOps Team | Initial version |
| 2.0 | ___________ | Team C | Updated for v1.0.0 release |

---

**End of Production Deployment Guide**
