# Production Security Guide
**HealthData-in-Motion CQL Quality Measure Evaluation System**

**Version:** 1.0
**Last Updated:** November 5, 2025
**Status:** Production Hardening

---

## Table of Contents

1. [Security Checklist](#security-checklist)
2. [Credential Management](#credential-management)
3. [SSL/TLS Configuration](#ssltls-configuration)
4. [Authentication & Authorization](#authentication--authorization)
5. [Network Security](#network-security)
6. [Data Encryption](#data-encryption)
7. [Monitoring & Auditing](#monitoring--auditing)
8. [Incident Response](#incident-response)

---

## Security Checklist

### Pre-Deployment Checklist

- [ ] **All default credentials changed**
  - [ ] PostgreSQL passwords
  - [ ] Redis password
  - [ ] Grafana admin password
  - [ ] Service API keys
  - [ ] JWT secret generated

- [ ] **SSL/TLS certificates generated and installed**
  - [ ] Backend services (8081, 8087)
  - [ ] Frontend (3002)
  - [ ] Database connections
  - [ ] Redis connections

- [ ] **Authentication implemented**
  - [ ] JWT-based authentication on all APIs
  - [ ] Service-to-service authentication
  - [ ] Role-based access control (RBAC)

- [ ] **Network security configured**
  - [ ] Firewall rules applied
  - [ ] CORS properly configured
  - [ ] Rate limiting enabled
  - [ ] DDoS protection enabled

- [ ] **Monitoring & alerting configured**
  - [ ] Prometheus alerting rules
  - [ ] Grafana dashboards
  - [ ] Log aggregation (ELK/Loki)
  - [ ] Alert channels (Slack, PagerDuty, email)

- [ ] **Compliance & audit**
  - [ ] HIPAA compliance reviewed
  - [ ] Audit logging enabled
  - [ ] Data retention policies configured
  - [ ] Security audit completed

---

## Credential Management

### 1. Generate Strong Passwords

Use these commands to generate secure credentials:

```bash
# Generate 32-character alphanumeric password
openssl rand -base64 32

# Generate 64-character hex string (for JWT secret)
openssl rand -hex 64

# Generate API key
openssl rand -base64 48 | tr -d "=+/" | cut -c1-40
```

### 2. Update .env.production

Copy `.env.production.example` to `.env.production` and update ALL `CHANGE_ME_*` values:

```bash
cp .env.production.example .env.production
chmod 600 .env.production  # Restrict file permissions
```

**Critical Values to Change:**

1. **PostgreSQL Passwords** (3 databases)
   ```
   POSTGRES_PASSWORD=<strong-password-here>
   QM_POSTGRES_PASSWORD=<strong-password-here>
   FHIR_POSTGRES_PASSWORD=<strong-password-here>
   ```

2. **Redis Password**
   ```
   REDIS_PASSWORD=<strong-password-here>
   ```

3. **JWT Secret** (MUST be unique and random)
   ```
   JWT_SECRET=$(openssl rand -hex 64)
   ```

4. **Service Credentials**
   ```
   CQL_SERVICE_PASSWORD=<strong-password-here>
   QM_SERVICE_PASSWORD=<strong-password-here>
   SERVICE_API_KEY=$(openssl rand -base64 48 | tr -d "=+/" | cut -c1-40)
   ```

5. **Monitoring Credentials**
   ```
   GRAFANA_ADMIN_PASSWORD=<strong-password-here>
   GRAFANA_SECRET_KEY=$(openssl rand -hex 32)
   ```

### 3. Docker Secrets (Recommended for Production)

Instead of using `.env` files, use Docker secrets:

```bash
# Create secrets
echo "my-secure-postgres-password" | docker secret create postgres_password -
echo "my-secure-redis-password" | docker secret create redis_password -
echo $(openssl rand -hex 64) | docker secret create jwt_secret -

# Update docker-compose to use secrets
# (see docker-compose.secrets.yml example)
```

### 4. Password Rotation Policy

- **Database passwords:** Rotate every 90 days
- **API keys:** Rotate every 180 days
- **JWT secrets:** Rotate every 365 days
- **Service accounts:** Rotate every 90 days

**Password Requirements:**
- Minimum 16 characters
- Mix of uppercase, lowercase, numbers, symbols
- No dictionary words
- No reuse of previous 5 passwords

---

## SSL/TLS Configuration

### 1. Generate SSL Certificates

#### Option A: Self-Signed Certificate (Development/Internal)

```bash
# Create SSL directory
mkdir -p ssl

# Generate private key and certificate
openssl req -x509 -newkey rsa:4096 -keyout ssl/healthdata.key \
  -out ssl/healthdata.crt -days 365 -nodes \
  -subj "/C=US/ST=State/L=City/O=HealthData/CN=healthdata.local"

# Create PKCS12 keystore for Java applications
openssl pkcs12 -export -in ssl/healthdata.crt -inkey ssl/healthdata.key \
  -out ssl/keystore.p12 -name healthdata \
  -passout pass:your-keystore-password
```

#### Option B: Let's Encrypt Certificate (Production)

```bash
# Install certbot
sudo apt-get install certbot

# Generate certificate
sudo certbot certonly --standalone \
  -d api.healthdata.example.com \
  -d fhir.healthdata.example.com \
  -d grafana.healthdata.example.com

# Convert to PKCS12 for Java
sudo openssl pkcs12 -export \
  -in /etc/letsencrypt/live/api.healthdata.example.com/fullchain.pem \
  -inkey /etc/letsencrypt/live/api.healthdata.example.com/privkey.pem \
  -out ssl/keystore.p12 -name healthdata \
  -passout pass:your-keystore-password

# Set up auto-renewal
sudo certbot renew --dry-run
```

### 2. Configure Spring Boot for SSL

Add to `application-production.yml`:

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEY_STORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: healthdata

  # Redirect HTTP to HTTPS
  http:
    port: 8080
  http2:
    enabled: true
```

### 3. Nginx Reverse Proxy with SSL (Recommended)

```nginx
# /etc/nginx/sites-available/healthdata

upstream cql_engine {
    server localhost:8081;
}

upstream quality_measure {
    server localhost:8087;
}

server {
    listen 80;
    server_name api.healthdata.example.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.healthdata.example.com;

    ssl_certificate /etc/letsencrypt/live/api.healthdata.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.healthdata.example.com/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # API endpoints
    location /cql-engine/ {
        proxy_pass http://cql_engine/cql-engine/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket support
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 86400;
    }

    location /quality-measure/ {
        proxy_pass http://quality_measure/quality-measure/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## Authentication & Authorization

### 1. JWT Implementation

#### Generate JWT Secret

```bash
# Generate a secure 64-character hex string
JWT_SECRET=$(openssl rand -hex 64)
echo "JWT_SECRET=$JWT_SECRET" >> .env.production
```

#### JWT Configuration (Spring Boot)

```yaml
# application-production.yml
jwt:
  secret: ${JWT_SECRET}
  expiration-ms: 3600000  # 1 hour
  refresh-expiration-ms: 86400000  # 24 hours
  header: Authorization
  prefix: Bearer
  issuer: healthdata-platform
  audience: healthdata-api
```

### 2. API Authentication Flow

```
1. Client → POST /auth/login (username, password)
2. Server validates credentials
3. Server generates JWT with claims:
   {
     "sub": "user@example.com",
     "tenant_id": "TENANT001",
     "roles": ["USER", "ADMIN"],
     "exp": 1699564800,
     "iat": 1699561200
   }
4. Client stores JWT securely
5. Client → GET /api/resource (Authorization: Bearer <JWT>)
6. Server validates JWT signature and expiration
7. Server extracts tenant_id and roles for authorization
8. Server processes request
```

### 3. Role-Based Access Control (RBAC)

**Roles Defined:**

| Role | Permissions |
|------|-------------|
| **ADMIN** | Full system access, manage users, configure measures |
| **CLINICAL_MANAGER** | View all reports, export data, manage care gaps |
| **EVALUATOR** | Run evaluations, view results (tenant-scoped) |
| **VIEWER** | Read-only access to dashboards and reports |
| **API_CLIENT** | Service-to-service communication |

**Implementation Example:**

```java
@PreAuthorize("hasRole('ADMIN') or hasRole('EVALUATOR')")
@PostMapping("/api/v1/cql/evaluations")
public ResponseEntity<CqlEvaluation> createEvaluation(
    @RequestHeader("X-Tenant-ID") String tenantId,
    @AuthenticationPrincipal JwtUser user,
    @RequestBody CqlEvaluationRequest request
) {
    // Validate tenant access
    if (!user.getTenantIds().contains(tenantId)) {
        throw new ForbiddenException("Access denied to tenant: " + tenantId);
    }

    // Process evaluation
    return ResponseEntity.ok(evaluationService.createAndExecute(request));
}
```

### 4. Multi-Tenant Security

**Tenant Isolation Strategy:**

1. **Database Level:**
   - Tenant ID column on all tables
   - Row-level security policies
   - Separate database schemas per tenant (optional)

2. **Application Level:**
   - JWT contains tenant_id claim
   - All queries filtered by tenant_id
   - API endpoints require X-Tenant-ID header
   - Validation: user has access to specified tenant

3. **Cache Level:**
   - Cache keys include tenant_id
   - Redis ACL rules per tenant (optional)

**Tenant Access Validation:**

```java
@Component
public class TenantSecurityFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) {
        String tenantId = request.getHeader("X-Tenant-ID");
        JwtUser user = SecurityContextHolder.getContext().getAuthentication();

        if (tenantId == null || !user.getTenantIds().contains(tenantId)) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Invalid tenant access");
            return;
        }

        TenantContext.setCurrentTenant(tenantId);
        filterChain.doFilter(request, response);
        TenantContext.clear();
    }
}
```

---

## Network Security

### 1. Firewall Configuration

**UFW (Ubuntu) Example:**

```bash
# Default deny all incoming
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Allow SSH (change port if needed)
sudo ufw allow 22/tcp

# Allow HTTPS
sudo ufw allow 443/tcp

# Allow HTTP (for redirect to HTTPS)
sudo ufw allow 80/tcp

# Allow Grafana (restrict to VPN IP range)
sudo ufw allow from 10.0.0.0/8 to any port 3001

# Allow Prometheus (restrict to VPN IP range)
sudo ufw allow from 10.0.0.0/8 to any port 9090

# Enable firewall
sudo ufw enable
```

### 2. CORS Configuration

**Production CORS Settings:**

```yaml
cors:
  allowed-origins:
    - https://dashboard.healthdata.example.com
    - https://app.healthdata.example.com
  allowed-methods:
    - GET
    - POST
    - PUT
    - DELETE
    - OPTIONS
  allowed-headers:
    - Authorization
    - Content-Type
    - X-Tenant-ID
    - X-Request-ID
  allow-credentials: true
  max-age: 3600
```

### 3. Rate Limiting

**Spring Boot Rate Limiting (Bucket4j):**

```java
@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimitInterceptor rateLimitInterceptor() {
        return RateLimitInterceptor.builder()
            .limit(Bandwidth.classic(100, Duration.ofMinutes(1)))
            .keyExtractor(request -> {
                String tenantId = request.getHeader("X-Tenant-ID");
                String userId = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
                return tenantId + ":" + userId;
            })
            .build();
    }
}
```

**Nginx Rate Limiting:**

```nginx
# Define rate limit zones
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;
limit_req_zone $http_x_tenant_id zone=tenant_limit:10m rate=100r/s;

# Apply to API endpoints
location /api/ {
    limit_req zone=api_limit burst=20 nodelay;
    limit_req zone=tenant_limit burst=50 nodelay;
    limit_req_status 429;

    proxy_pass http://backend;
}
```

### 4. DDoS Protection

**Cloudflare Integration (Recommended):**

1. Sign up for Cloudflare
2. Add your domain
3. Enable "Under Attack" mode if needed
4. Configure rate limiting rules
5. Enable Bot Fight Mode
6. Set up Page Rules for API endpoints

**Fail2ban Configuration:**

```ini
# /etc/fail2ban/jail.local

[healthdata-api]
enabled = true
port = http,https
filter = healthdata-api
logpath = /var/log/nginx/access.log
maxretry = 5
bantime = 3600
findtime = 600

[healthdata-auth]
enabled = true
port = http,https
filter = healthdata-auth
logpath = /var/log/healthdata/application.log
maxretry = 3
bantime = 7200
findtime = 300
```

---

## Data Encryption

### 1. Encryption at Rest

**PostgreSQL Encryption:**

```bash
# Enable transparent data encryption (TDE)
# Option 1: Full disk encryption (LUKS)
sudo cryptsetup luksFormat /dev/sdb
sudo cryptsetup luksOpen /dev/sdb postgres_data

# Option 2: PostgreSQL-level encryption (pgcrypto)
# In SQL:
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Encrypt sensitive columns
ALTER TABLE cql_evaluations
  ALTER COLUMN evaluation_result TYPE bytea
  USING pgp_sym_encrypt(evaluation_result::text, 'encryption-key');

-- Decrypt when querying
SELECT pgp_sym_decrypt(evaluation_result, 'encryption-key')
FROM cql_evaluations;
```

**Redis Encryption:**

```bash
# Redis doesn't support built-in encryption at rest
# Use disk encryption or AWS ElastiCache with encryption
# For manual setup, use dm-crypt:
sudo cryptsetup luksFormat /dev/sdc
sudo cryptsetup luksOpen /dev/sdc redis_data
```

### 2. Encryption in Transit

**Database SSL Connections:**

```yaml
# application-production.yml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/healthdata_cql?ssl=true&sslmode=require
    hikari:
      data-source-properties:
        ssl: true
        sslmode: require
        sslrootcert: /etc/ssl/certs/ca-certificates.crt
```

**Redis TLS:**

```yaml
spring:
  data:
    redis:
      ssl:
        enabled: true
      client-type: lettuce
      lettuce:
        pool:
          enabled: true
```

### 3. Sensitive Data Handling

**PII/PHI Data Protection:**

1. **Tokenization:** Replace sensitive data with tokens
2. **Masking:** Display only last 4 digits of IDs
3. **Hashing:** Store one-way hashes where possible
4. **Audit Logging:** Log all access to sensitive data

**Example:**

```java
@Component
public class DataMaskingService {

    public String maskPatientId(String patientId) {
        if (patientId.length() <= 4) return "****";
        return "*".repeat(patientId.length() - 4) +
               patientId.substring(patientId.length() - 4);
    }

    public String hashSensitiveData(String data) {
        return BCrypt.hashpw(data, BCrypt.gensalt(12));
    }
}
```

---

## Monitoring & Auditing

### 1. Audit Logging

**What to Log:**

- Authentication attempts (success/failure)
- Authorization failures
- Data access (patient records)
- Configuration changes
- User management actions
- API calls with tenant context

**Log Format (JSON):**

```json
{
  "timestamp": "2025-11-05T10:30:00Z",
  "event_type": "DATA_ACCESS",
  "user_id": "user@example.com",
  "tenant_id": "TENANT001",
  "resource": "Patient/12345",
  "action": "READ",
  "ip_address": "192.168.1.100",
  "status": "SUCCESS",
  "details": {
    "measure_id": "HEDIS-CDC",
    "evaluation_id": "uuid-here"
  }
}
```

**Implementation:**

```java
@Aspect
@Component
public class AuditLoggingAspect {

    @Around("@annotation(audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audited audited) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        String tenantId = TenantContext.getCurrentTenant();

        AuditLog log = AuditLog.builder()
            .timestamp(Instant.now())
            .eventType(audited.eventType())
            .userId(userId)
            .tenantId(tenantId)
            .resource(joinPoint.getSignature().toShortString())
            .build();

        try {
            Object result = joinPoint.proceed();
            log.setStatus("SUCCESS");
            return result;
        } catch (Exception e) {
            log.setStatus("FAILURE");
            log.setDetails(e.getMessage());
            throw e;
        } finally {
            auditLogRepository.save(log);
        }
    }
}
```

### 2. Security Monitoring

**Prometheus Metrics:**

- `authentication_attempts_total{status="success|failure"}`
- `authorization_failures_total`
- `rate_limit_exceeded_total`
- `suspicious_activity_total`
- `data_access_total{resource_type}`

**Grafana Alerts:**

- Failed login spike (>10 in 5 minutes)
- Unauthorized access attempts (>50 in 5 minutes)
- Rate limit violations (>100 in 5 minutes)
- Data breach indicators

### 3. SIEM Integration

**Forward logs to SIEM (e.g., Splunk, ELK):**

```yaml
# Filebeat configuration
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /var/log/healthdata/*.log
    json.keys_under_root: true
    json.add_error_key: true
    fields:
      environment: production
      application: healthdata-cql

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "healthdata-%{+yyyy.MM.dd}"
  username: "elastic"
  password: "${ELASTIC_PASSWORD}"
```

---

## Incident Response

### 1. Security Incident Response Plan

**Severity Levels:**

| Level | Description | Response Time | Actions |
|-------|-------------|---------------|---------|
| **P1 - Critical** | Data breach, system compromise | Immediate | Isolate system, notify CISO, engage forensics |
| **P2 - High** | Multiple failed breach attempts | 15 minutes | Enable monitoring, review logs, patch vulnerabilities |
| **P3 - Medium** | Suspicious activity detected | 1 hour | Investigate, document, adjust rules |
| **P4 - Low** | Policy violations | 4 hours | Review, educate users, update policies |

### 2. Incident Response Steps

**1. Detection & Analysis:**
- Identify indicators of compromise (IoC)
- Determine scope and impact
- Classify incident severity
- Engage incident response team

**2. Containment:**
- Isolate affected systems
- Revoke compromised credentials
- Block malicious IPs/domains
- Enable "Under Attack" mode

**3. Eradication:**
- Remove malware/backdoors
- Patch vulnerabilities
- Reset compromised credentials
- Update firewall rules

**4. Recovery:**
- Restore from backups (if needed)
- Verify system integrity
- Re-enable services gradually
- Monitor for re-infection

**5. Post-Incident:**
- Document lessons learned
- Update security policies
- Conduct training
- Improve detection capabilities

### 3. Emergency Contacts

```
Security Team:
- CISO: security-chief@healthdata.example.com
- Security Engineer: security-team@healthdata.example.com
- On-Call: +1-555-SECURITY

Incident Hotline: +1-555-INCIDENT
Incident Email: incidents@healthdata.example.com

External Resources:
- Legal Counsel: legal@healthdata.example.com
- PR Team: pr@healthdata.example.com
- Insurance Provider: [Contact Info]
```

### 4. Communication Template

**Initial Notification:**

```
Subject: [P1 SECURITY INCIDENT] - Brief Description

INCIDENT SUMMARY:
- Incident ID: INC-2025-1105-001
- Severity: P1 (Critical)
- Detected: 2025-11-05 10:30 UTC
- Status: Contained
- Impact: [Brief description]

IMMEDIATE ACTIONS TAKEN:
- [List actions]

NEXT STEPS:
- [List next steps]

INCIDENT COMMANDER: [Name]
CONTACT: [Email/Phone]

Next update in: 1 hour
```

---

## Security Audit Checklist

### Pre-Production Audit

- [ ] Penetration testing completed
- [ ] Vulnerability scanning passed
- [ ] Code security review completed
- [ ] Dependency vulnerability scan clean
- [ ] OWASP Top 10 compliance verified
- [ ] HIPAA compliance audit passed
- [ ] Security policies documented
- [ ] Incident response plan tested
- [ ] Disaster recovery plan validated
- [ ] All security controls implemented

### Ongoing Audits

- [ ] **Weekly:** Review security logs
- [ ] **Monthly:** Vulnerability scanning
- [ ] **Quarterly:** Penetration testing
- [ ] **Annually:** Comprehensive security audit
- [ ] **Continuously:** Automated security monitoring

---

## Compliance

### HIPAA Compliance

**Technical Safeguards:**
- ✅ Access Control (Unique user IDs, automatic logoff)
- ✅ Audit Controls (Audit logs for all PHI access)
- ✅ Integrity (Data checksums, tamper detection)
- ✅ Transmission Security (TLS 1.2+, VPN)

**Administrative Safeguards:**
- Security management process
- Workforce security (training, awareness)
- Information access management
- Security awareness training
- Contingency planning

**Physical Safeguards:**
- Facility access controls
- Workstation security
- Device and media controls

### GDPR Compliance (if applicable)

- Right to access (data export)
- Right to erasure (data deletion)
- Data portability
- Consent management
- Data breach notification (72 hours)

---

## Conclusion

This security guide provides comprehensive coverage of security hardening for production deployment. Follow each section carefully and maintain ongoing security vigilance.

**Next Steps:**
1. Complete security checklist
2. Test all security controls
3. Conduct security audit
4. Document any deviations
5. Obtain security sign-off

**Questions or Concerns:**
Contact the Security Team: security-team@healthdata.example.com

---

**Document Version:** 1.0
**Last Review:** 2025-11-05
**Next Review:** 2025-12-05
