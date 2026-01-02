# Production Deployment Guide - Reports Feature

## Deployment Readiness Checklist

### Code Quality ✅
- [x] All unit tests passing (18/18)
- [x] Zero compiler warnings
- [x] TypeScript strict mode enabled
- [x] ESLint configured and passing
- [x] No console.log statements in production code
- [x] Error handling comprehensive

### Documentation ✅
- [x] API integration documented
- [x] Test coverage documented
- [x] Docker setup documented
- [x] Database schema documented

### Configuration ⚠️
- [ ] Environment variables configured
- [ ] JWT secrets rotated
- [ ] Database credentials secured
- [ ] CORS origins restricted
- [ ] Rate limiting configured
- [ ] Logging levels set

### Security 📋
- [ ] Security audit completed
- [ ] Dependencies updated
- [ ] Vulnerability scan passed
- [ ] HTTPS enforced
- [ ] CSP headers configured
- [ ] Authentication tested

## Build Process

### Frontend Build

```bash
# Production build
npm run build -- --project=clinical-portal --configuration=production

# Output location
dist/apps/clinical-portal/

# Build artifacts
├── index.html
├── main.*.js (minified)
├── polyfills.*.js
├── runtime.*.js
├── styles.*.css
└── assets/
```

### Build Configuration

**angular.json** (production optimization):
```json
{
  "configurations": {
    "production": {
      "optimization": true,
      "outputHashing": "all",
      "sourceMap": false,
      "namedChunks": false,
      "aot": true,
      "extractLicenses": true,
      "vendorChunk": false,
      "buildOptimizer": true,
      "budgets": [
        {
          "type": "initial",
          "maximumWarning": "2mb",
          "maximumError": "5mb"
        }
      ]
    }
  }
}
```

### Backend Build

```bash
# Build quality-measure-service
cd apps/backend/quality-measure-service
./mvnw clean package -DskipTests

# Output
target/quality-measure-service-1.0.0.jar

# Docker image
docker build -t healthdata/quality-measure-service:1.0.0 .
docker push healthdata/quality-measure-service:1.0.0
```

## Environment Configuration

### Frontend Environment Variables

**environment.prod.ts:**
```typescript
export const environment = {
  production: true,
  apiBaseUrl: 'https://api.healthdata.example.com',
  qualityMeasureServiceUrl: 'https://api.healthdata.example.com/quality-measure',
  patientServiceUrl: 'https://api.healthdata.example.com/patient',
  fhirServiceUrl: 'https://api.healthdata.example.com/fhir',
  authServiceUrl: 'https://api.healthdata.example.com/auth',
  enableDebugMode: false,
  logLevel: 'error',
  sessionTimeout: 3600000, // 1 hour
  apiTimeout: 30000, // 30 seconds
};
```

### Backend Environment Variables

**application-prod.yml:**
```yaml
server:
  port: 8087
  compression:
    enabled: true
  http2:
    enabled: true

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?ssl=true&sslmode=require
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    show-sql: false
    properties:
      hibernate:
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true

  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}
    ssl: true
    timeout: 5000ms

  kafka:
    bootstrap-servers: ${KAFKA_BROKERS}
    security:
      protocol: SASL_SSL
    sasl:
      mechanism: SCRAM-SHA-256
      jaas-config: ${KAFKA_JAAS_CONFIG}

jwt:
  secret: ${JWT_SECRET}
  expiration: 3600000 # 1 hour
  refresh-expiration: 604800000 # 7 days

logging:
  level:
    root: INFO
    com.healthdata: INFO
    org.springframework: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
  file:
    name: /var/log/quality-measure-service/application.log
    max-size: 10MB
    max-history: 30
```

### Required Environment Variables

```bash
# Database
DB_HOST=prod-postgres.example.com
DB_PORT=5432
DB_NAME=healthdata_prod
DB_USERNAME=healthdata_app
DB_PASSWORD=<secure-password>

# Redis
REDIS_HOST=prod-redis.example.com
REDIS_PORT=6379
REDIS_PASSWORD=<secure-password>

# Kafka
KAFKA_BROKERS=kafka-1.example.com:9093,kafka-2.example.com:9093
KAFKA_JAAS_CONFIG=<jaas-config-string>

# JWT
JWT_SECRET=<generate-with-openssl-rand-base64-64>

# External Services
PATIENT_SERVICE_URL=https://api.healthdata.example.com/patient
FHIR_SERVICE_URL=https://api.healthdata.example.com/fhir
CQL_ENGINE_URL=https://api.healthdata.example.com/cql-engine
CARE_GAP_SERVICE_URL=https://api.healthdata.example.com/care-gap

# Monitoring
SENTRY_DSN=<sentry-dsn>
DATADOG_API_KEY=<datadog-key>
```

## Database Migration

### Pre-Deployment Steps

```bash
# 1. Backup production database
pg_dump -h prod-postgres.example.com -U healthdata_app \
  -d healthdata_prod -F c -b -v \
  -f healthdata_prod_backup_$(date +%Y%m%d_%H%M%S).dump

# 2. Test migrations on staging
cd apps/backend/quality-measure-service
./mvnw liquibase:update -Dspring.profiles.active=staging

# 3. Verify schema changes
psql -h staging-postgres.example.com -U healthdata_app -d healthdata_staging
\d saved_reports
\d custom_measures
```

### Production Migration

```yaml
# liquibase.yml configuration
changeLogFile: classpath:db/changelog/db.changelog-master.xml
url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
username: ${DB_USERNAME}
password: ${DB_PASSWORD}
driver: org.postgresql.Driver
contexts: production
```

```bash
# Execute migration
./mvnw liquibase:update -Dspring.profiles.active=production

# Rollback if needed
./mvnw liquibase:rollback -Dliquibase.rollbackCount=1
```

### Schema Verification

```sql
-- Verify saved_reports table
SELECT COUNT(*) FROM saved_reports;
SELECT * FROM saved_reports LIMIT 1;

-- Verify custom_measures table
SELECT COUNT(*) FROM custom_measures;
SELECT * FROM custom_measures LIMIT 1;

-- Check indexes
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename IN ('saved_reports', 'custom_measures');

-- Verify constraints
SELECT conname, contype, conrelid::regclass 
FROM pg_constraint 
WHERE conrelid IN ('saved_reports'::regclass, 'custom_measures'::regclass);
```

## Docker Deployment

### Production Docker Compose

**docker-compose.prod.yml:**
```yaml
version: '3.8'

services:
  clinical-portal:
    image: healthdata/clinical-portal:${VERSION}
    ports:
      - "80:80"
      - "443:443"
    environment:
      - API_BASE_URL=${API_BASE_URL}
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  quality-measure-service:
    image: healthdata/quality-measure-service:${VERSION}
    ports:
      - "8087:8087"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - DB_HOST=${DB_HOST}
      - DB_PORT=${DB_PORT}
      - DB_NAME=${DB_NAME}
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - REDIS_HOST=${REDIS_HOST}
      - REDIS_PORT=${REDIS_PORT}
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8087/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    depends_on:
      - postgres
      - redis

  postgres:
    image: postgres:15-alpine
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backups:/backups
    environment:
      - POSTGRES_DB=${DB_NAME}
      - POSTGRES_USER=${DB_USERNAME}
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME}"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes
    volumes:
      - redis_data:/data
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
    driver: local
  redis_data:
    driver: local
```

### Deployment Commands

```bash
# Pull latest images
docker-compose -f docker-compose.prod.yml pull

# Stop services (rolling update)
docker-compose -f docker-compose.prod.yml stop quality-measure-service
docker-compose -f docker-compose.prod.yml up -d quality-measure-service

# Health check
curl -f http://localhost:8087/actuator/health

# Update frontend
docker-compose -f docker-compose.prod.yml stop clinical-portal
docker-compose -f docker-compose.prod.yml up -d clinical-portal

# View logs
docker-compose -f docker-compose.prod.yml logs -f --tail=100 quality-measure-service
```

## Kubernetes Deployment (Alternative)

### Deployment Manifests

**quality-measure-service-deployment.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: quality-measure-service
  namespace: healthdata
spec:
  replicas: 3
  selector:
    matchLabels:
      app: quality-measure-service
  template:
    metadata:
      labels:
        app: quality-measure-service
    spec:
      containers:
      - name: quality-measure-service
        image: healthdata/quality-measure-service:1.0.0
        ports:
        - containerPort: 8087
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DB_HOST
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: host
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8087
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8087
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: quality-measure-service
  namespace: healthdata
spec:
  selector:
    app: quality-measure-service
  ports:
  - port: 8087
    targetPort: 8087
  type: ClusterIP
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: quality-measure-service
  namespace: healthdata
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  tls:
  - hosts:
    - api.healthdata.example.com
    secretName: api-tls
  rules:
  - host: api.healthdata.example.com
    http:
      paths:
      - path: /quality-measure
        pathType: Prefix
        backend:
          service:
            name: quality-measure-service
            port:
              number: 8087
```

### Deployment Commands

```bash
# Create secrets
kubectl create secret generic database-credentials \
  --from-literal=host=prod-postgres.example.com \
  --from-literal=username=healthdata_app \
  --from-literal=password=<secure-password> \
  -n healthdata

kubectl create secret generic jwt-secret \
  --from-literal=secret=<jwt-secret> \
  -n healthdata

# Apply manifests
kubectl apply -f quality-measure-service-deployment.yaml

# Check status
kubectl get pods -n healthdata -l app=quality-measure-service
kubectl logs -n healthdata -l app=quality-measure-service --tail=100

# Rolling update
kubectl set image deployment/quality-measure-service \
  quality-measure-service=healthdata/quality-measure-service:1.1.0 \
  -n healthdata

# Rollback if needed
kubectl rollout undo deployment/quality-measure-service -n healthdata
```

## Monitoring & Observability

### Health Checks

```bash
# Application health
curl https://api.healthdata.example.com/quality-measure/actuator/health

# Expected response
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}

# Metrics endpoint
curl https://api.healthdata.example.com/quality-measure/actuator/metrics
```

### Logging Configuration

**logback-spring.xml:**
```xml
<configuration>
  <appender name="JSON" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>/var/log/quality-measure-service/application.json</file>
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
      <providers>
        <timestamp/>
        <version/>
        <logLevel/>
        <loggerName/>
        <threadName/>
        <message/>
        <logstashMarkers/>
        <arguments/>
        <stackTrace/>
      </providers>
    </encoder>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
      <fileNamePattern>/var/log/quality-measure-service/application-%d{yyyy-MM-dd}.json.gz</fileNamePattern>
      <maxHistory>30</maxHistory>
    </rollingPolicy>
  </appender>
  
  <root level="INFO">
    <appender-ref ref="JSON"/>
  </root>
</configuration>
```

### Application Performance Monitoring (APM)

**Integration with Datadog:**
```yaml
# application-prod.yml
management:
  metrics:
    export:
      datadog:
        enabled: true
        api-key: ${DATADOG_API_KEY}
        application-key: ${DATADOG_APP_KEY}
        step: 1m
    tags:
      application: quality-measure-service
      environment: production
```

**Integration with Sentry:**
```yaml
sentry:
  dsn: ${SENTRY_DSN}
  environment: production
  traces-sample-rate: 0.1
  logging:
    minimum-event-level: error
```

### Metrics to Monitor

**Application Metrics:**
- Request rate (req/sec)
- Response time (p50, p95, p99)
- Error rate (%)
- Active sessions
- Database connection pool usage
- Redis cache hit/miss rate

**Business Metrics:**
- Reports generated per hour
- Export requests per hour
- Average report generation time
- Failed report generations
- Active users

**Infrastructure Metrics:**
- CPU usage (%)
- Memory usage (%)
- Disk I/O (IOPS)
- Network throughput (Mbps)
- Database query time (ms)

### Alerting Rules

```yaml
# Prometheus AlertManager rules
groups:
- name: quality-measure-service
  rules:
  - alert: HighErrorRate
    expr: rate(http_server_requests_seconds_count{status="500"}[5m]) > 0.05
    for: 5m
    annotations:
      summary: "High error rate detected"
      
  - alert: SlowResponseTime
    expr: histogram_quantile(0.95, http_server_requests_seconds_bucket) > 2
    for: 5m
    annotations:
      summary: "95th percentile response time > 2s"
      
  - alert: DatabaseConnectionPoolExhausted
    expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
    for: 5m
    annotations:
      summary: "Database connection pool 90% utilized"
```

## Security Hardening

### HTTPS Configuration

**nginx.conf:**
```nginx
server {
    listen 443 ssl http2;
    server_name api.healthdata.example.com;

    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Content-Security-Policy "default-src 'self'" always;

    location /quality-measure {
        proxy_pass http://quality-measure-service:8087;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Rate Limiting

**application-prod.yml:**
```yaml
bucket4j:
  enabled: true
  filters:
  - url: /quality-measure/report/.*
    http-method: POST
    rate-limits:
    - bandwidths:
      - capacity: 10
        time: 1
        unit: minutes
  - url: /quality-measure/reports/.*/export/.*
    rate-limits:
    - bandwidths:
      - capacity: 20
        time: 1
        unit: minutes
```

### Input Validation

**Ensure all DTOs have validation:**
```java
@Data
public class SavePatientReportRequest {
    @NotBlank(message = "Patient ID is required")
    private String patientId;
    
    @NotBlank(message = "Measure ID is required")
    private String measureId;
    
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;
    
    @NotBlank(message = "Report name is required")
    @Size(max = 255, message = "Report name must be less than 255 characters")
    private String reportName;
}
```

## Rollback Plan

### Automated Rollback (Kubernetes)

```bash
# Check rollout history
kubectl rollout history deployment/quality-measure-service -n healthdata

# Rollback to previous version
kubectl rollout undo deployment/quality-measure-service -n healthdata

# Rollback to specific revision
kubectl rollout undo deployment/quality-measure-service --to-revision=2 -n healthdata
```

### Manual Rollback (Docker Compose)

```bash
# Stop current version
docker-compose -f docker-compose.prod.yml stop quality-measure-service

# Deploy previous version
export VERSION=0.9.0
docker-compose -f docker-compose.prod.yml up -d quality-measure-service

# Verify
curl http://localhost:8087/actuator/health
```

### Database Rollback

```bash
# Rollback last migration
./mvnw liquibase:rollback -Dliquibase.rollbackCount=1 -Dspring.profiles.active=production

# Rollback to specific tag
./mvnw liquibase:rollback -Dliquibase.rollbackTag=v1.0.0 -Dspring.profiles.active=production
```

## Post-Deployment Verification

### Smoke Tests

```bash
#!/bin/bash
# smoke-test.sh

BASE_URL="https://api.healthdata.example.com/quality-measure"
TOKEN="<jwt-token>"

# Test health endpoint
echo "Testing health endpoint..."
curl -f "$BASE_URL/actuator/health" || exit 1

# Test authentication
echo "Testing authentication..."
curl -f -H "Authorization: Bearer $TOKEN" "$BASE_URL/reports" || exit 1

# Test report generation
echo "Testing report generation..."
curl -f -X POST "$BASE_URL/report/patient/save" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"patientId":"test-patient","measureId":"CMS68v11","tenantId":"TENANT001","reportName":"Smoke Test","createdBy":"automation"}' || exit 1

echo "All smoke tests passed!"
```

### Performance Validation

```bash
# Load test with Apache Bench
ab -n 100 -c 10 -H "Authorization: Bearer $TOKEN" \
  https://api.healthdata.example.com/quality-measure/reports

# Expected results
# Requests per second: > 50
# Time per request: < 200ms (mean)
# Failed requests: 0
```

### User Acceptance Testing

**Checklist:**
- [ ] Login successful
- [ ] Reports page loads
- [ ] Can generate patient report
- [ ] Can generate population report
- [ ] Can view saved reports
- [ ] Can export to CSV
- [ ] Can export to Excel
- [ ] Can delete report
- [ ] Filters work correctly
- [ ] No console errors
- [ ] Page responsive on mobile

## Backup & Disaster Recovery

### Automated Backups

```bash
#!/bin/bash
# backup.sh - Run daily via cron

BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d_%H%M%S)

# Database backup
pg_dump -h prod-postgres.example.com -U healthdata_app \
  -d healthdata_prod -F c -b -v \
  -f "$BACKUP_DIR/healthdata_prod_$DATE.dump"

# Upload to S3
aws s3 cp "$BACKUP_DIR/healthdata_prod_$DATE.dump" \
  s3://healthdata-backups/database/

# Cleanup old backups (keep 30 days)
find $BACKUP_DIR -name "*.dump" -mtime +30 -delete
```

### Disaster Recovery Procedure

```bash
# 1. Provision new infrastructure
terraform apply -var-file=production.tfvars

# 2. Restore database
pg_restore -h new-postgres.example.com -U healthdata_app \
  -d healthdata_prod -v \
  healthdata_prod_20240115_120000.dump

# 3. Deploy services
kubectl apply -f k8s/

# 4. Update DNS
# Point api.healthdata.example.com to new infrastructure

# 5. Verify
./smoke-test.sh
```

## Support & Maintenance

### Log Analysis

```bash
# Find errors in last hour
grep ERROR /var/log/quality-measure-service/application.log | tail -100

# Find slow queries
grep "SlowQuery" /var/log/quality-measure-service/application.log

# Count requests by endpoint
jq -r '.message' /var/log/quality-measure-service/application.json | \
  grep "GET\|POST" | sort | uniq -c | sort -nr
```

### Common Issues

**Issue:** High memory usage
```bash
# Check heap usage
curl http://localhost:8087/actuator/metrics/jvm.memory.used
curl http://localhost:8087/actuator/metrics/jvm.memory.max

# Generate heap dump
jmap -dump:live,format=b,file=heap.bin <pid>
```

**Issue:** Database connection pool exhausted
```bash
# Check active connections
SELECT count(*) FROM pg_stat_activity 
WHERE datname = 'healthdata_prod';

# Kill idle connections
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE state = 'idle' AND state_change < now() - interval '10 minutes';
```

## Success Metrics

**Deployment Success Criteria:**
- ✅ All services healthy
- ✅ Zero errors in last 5 minutes
- ✅ Response time < 500ms (p95)
- ✅ All smoke tests passing
- ✅ Database migrations applied
- ✅ Monitoring alerts configured
- ✅ Backup job running

**Business Metrics (Week 1):**
- Report generation success rate > 99%
- Export success rate > 99%
- Average response time < 300ms
- Zero security incidents
- User satisfaction > 4/5

## Contact Information

**On-Call Rotation:**
- Primary: oncall-primary@healthdata.example.com
- Secondary: oncall-secondary@healthdata.example.com
- Escalation: engineering-lead@healthdata.example.com

**Runbook Location:**
https://wiki.healthdata.example.com/runbooks/quality-measure-service

**Status Page:**
https://status.healthdata.example.com
