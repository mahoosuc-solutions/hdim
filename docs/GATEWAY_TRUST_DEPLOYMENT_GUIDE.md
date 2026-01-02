# Gateway Trust Authentication - Production Deployment Guide

**Version**: 1.0.0
**Last Updated**: December 30, 2025
**Status**: Production Ready

---

## Overview

The HDIM system uses a **gateway-trust authentication architecture** where:
1. **Gateway** (single point) validates JWT tokens and injects trusted X-Auth-* headers
2. **Backend Services** trust gateway-injected headers without re-validating JWT or database lookups
3. **Performance**: No duplicate authentication validation per request
4. **Security**: Single point of authentication with cryptographic validation (HMAC-SHA256)

This deployment guide provides step-by-step instructions for setting up production gateway trust authentication.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT                                    │
│              (Web Browser / Mobile App / CLI)                    │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                    (JWT Token in header)
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                   API GATEWAY                                    │
│  (gateway-service:8080 or Kong:8000)                             │
│                                                                   │
│  1. Validate JWT signature with HMAC secret                     │
│  2. Extract user context (ID, username, roles, tenant IDs)      │
│  3. Inject X-Auth-* headers with HMAC signature                 │
│  4. Forward request to backend service                          │
└──────────────────┬───────────────────────────────────────────────┘
                   │
        (Trusted X-Auth-* headers)
                   ▼
┌─────────────────────────────────────────────────────────────────┐
│              BACKEND SERVICES (Microservices)                    │
│  • Quality Measure Service (8087)                               │
│  • Care Gap Service (8086)                                      │
│  • Patient Service (8084)                                       │
│  • CQL Engine Service (8081)                                    │
│                                                                   │
│  1. TrustedHeaderAuthFilter validates HMAC signature            │
│  2. Extract user from X-Auth-* headers (no DB lookup)           │
│  3. TrustedTenantAccessFilter validates tenant access          │
│  4. Authorize request based on roles/permissions               │
└─────────────────────────────────────────────────────────────────┘
```

---

## Deployment Steps

### Step 1: Generate HMAC Signing Secret

Production deployments require a secure HMAC signing secret for header validation.

```bash
# Generate a 64-character hex string (256-bit key for HMAC-SHA256)
openssl rand -hex 32 > /path/to/secrets/gateway_auth_signing_secret.txt

# Export the secret for use in docker-compose
export GATEWAY_AUTH_SIGNING_SECRET=$(cat /path/to/secrets/gateway_auth_signing_secret.txt)

# Verify the secret (should be 64 hex characters)
echo $GATEWAY_AUTH_SIGNING_SECRET | wc -c
```

**Security Considerations:**
- Store the secret securely (HashiCorp Vault, AWS Secrets Manager, etc.)
- Use the SAME secret across all backend services
- Rotate secrets periodically (quarterly minimum)
- Never commit secrets to git repository

### Step 2: Configure docker-compose.production.yml

The production composition file includes gateway trust configuration for key services:

```yaml
# CQL Engine Service
cql-engine-service:
  environment:
    # Development: accepts any gateway header with valid prefix
    # GATEWAY_AUTH_DEV_MODE: "true"

    # Production: validates HMAC signature
    GATEWAY_AUTH_DEV_MODE: "false"
    GATEWAY_AUTH_SIGNING_SECRET: ${GATEWAY_AUTH_SIGNING_SECRET}

# Quality Measure Service
quality-measure-service:
  environment:
    GATEWAY_AUTH_DEV_MODE: "false"
    GATEWAY_AUTH_SIGNING_SECRET: ${GATEWAY_AUTH_SIGNING_SECRET}
```

**Key Points:**
- `GATEWAY_AUTH_DEV_MODE: "false"` - **MUST** be false in production
- `GATEWAY_AUTH_SIGNING_SECRET` - Must match the secret configured in the gateway

### Step 3: Deploy Services with Environment Variables

```bash
# 1. Set environment variables
export GATEWAY_AUTH_SIGNING_SECRET=$(cat /path/to/secrets/gateway_auth_signing_secret.txt)
export DB_ADMIN_PASSWORD="$(openssl rand -base64 32)"
export REDIS_PASSWORD="$(openssl rand -base64 32)"
export JWT_SECRET="$(openssl rand -hex 32)"

# 2. Start services using production composition
cd /home/mahoosuc-solutions/projects/hdim-master/hdim-master
docker compose -f docker-compose.production.yml up -d

# 3. Verify services are healthy
docker compose -f docker-compose.production.yml ps
```

### Step 4: Verify Gateway Trust Authentication

#### 4.1 Check Service Health

```bash
# Quality Measure Service
curl -s http://localhost:8087/quality-measure/actuator/health | jq .

# Expected Response:
# {
#   "status": "UP",
#   "components": {
#     "db": {"status": "UP"},
#     "redis": {"status": "UP"},
#     "diskSpace": {"status": "UP"}
#   }
# }
```

#### 4.2 Test Unauthenticated Request (Should Fail)

```bash
# Try accessing protected endpoint without gateway headers
curl -X GET http://localhost:8087/quality-measure/api/v1/measures

# Expected Response (401 Unauthorized):
# {
#   "error": "Unauthorized",
#   "message": "Missing or invalid authentication",
#   "timestamp": "2025-12-30T23:45:00Z"
# }
```

#### 4.3 Test Authenticated Request Through Gateway

```bash
# 1. Get JWT token from gateway
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test_admin","password":"password123"}' \
  | jq -r '.token')

# 2. Make request through gateway (gateway injects X-Auth-* headers)
curl -s -X GET http://localhost:8080/quality-measure/api/v1/measures \
  -H "Authorization: Bearer $TOKEN"

# Expected Response (200 OK):
# [
#   {"id": "measure-001", "name": "HbA1c Test", ...},
#   ...
# ]
```

#### 4.4 Test Multi-Tenant Isolation

```bash
# Test that tenant isolation is enforced
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"tenant1_user","password":"password123"}' \
  | jq -r '.token')

# Try to access another tenant's data (should fail)
curl -s -X GET "http://localhost:8080/quality-measure/api/v1/patients/patient-999?tenantId=tenant2" \
  -H "Authorization: Bearer $TOKEN"

# Expected Response (403 Forbidden):
# {
#   "error": "Forbidden",
#   "message": "Access denied: not authorized for tenant",
#   "timestamp": "2025-12-30T23:45:00Z"
# }
```

---

## Migrating Services to Gateway Trust Pattern

### Phase 1: Core Services (Complete)
- ✅ Quality Measure Service (8087)
- ✅ Care Gap Service (8086)
- ✅ Patient Service (8084)

### Phase 2: Supporting Services (Next)
- [ ] CQL Engine Service (8081)
- [ ] FHIR Service (8085)
- [ ] Consent Service (8082)
- [ ] Event Processing Service (8083)

### Phase 3: Extended Services (Later)
- [ ] Notification Service
- [ ] Prior Authorization Service
- [ ] QRDA Export Service
- [ ] HCC Service
- [ ] Predictive Analytics Service

### Migration Procedure

For each service to be migrated:

```bash
# 1. Update service security config
# - Replace JwtAuthenticationFilter with TrustedHeaderAuthFilter
# - Replace TenantAccessFilter with TrustedTenantAccessFilter
# - Add @Value for gateway.auth properties

# 2. Update test configuration
# File: src/test/java/.../config/*SecurityConfigTest.java
# - Update mock bean names
# - Update filter chain method signatures

# 3. Update docker-compose configuration
# Add environment variables:
GATEWAY_AUTH_DEV_MODE: "true"
GATEWAY_AUTH_SIGNING_SECRET: ${GATEWAY_AUTH_SIGNING_SECRET:-}

# 4. Build and test
./gradlew :modules:services:SERVICE_NAME:build
./gradlew :modules:services:SERVICE_NAME:test

# 5. Deploy to staging
docker-compose --profile core up -d service-name

# 6. Verify through gateway
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/service-name/api/v1/endpoint
```

---

## Configuration Reference

### Environment Variables

| Variable | Production Value | Development Value | Purpose |
|----------|------------------|-------------------|---------|
| `GATEWAY_AUTH_DEV_MODE` | `"false"` | `"true"` | Enable/disable HMAC validation |
| `GATEWAY_AUTH_SIGNING_SECRET` | 64-char hex | Empty/any | HMAC signing key |
| `SPRING_PROFILES_ACTIVE` | `production` | `docker` | Spring profile |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://...?ssl=true&sslmode=require` | Standard | Database SSL requirement |

### Security Filter Chain Order

```
HTTP Request
    ↓
[1] CORS Configuration
    ↓
[2] CSRF Protection (disabled for APIs)
    ↓
[3] TrustedHeaderAuthFilter
    ├─ Validate X-Auth-Validated header (HMAC signature)
    ├─ Extract X-Auth-* headers
    ├─ Create Authentication in SecurityContext
    ↓
[4] Authorization Filter (Spring Security)
    ├─ Check if request matches permitted patterns
    ├─ Allow: health, docs, public endpoints
    ├─ Deny: unauthenticated requests to protected endpoints
    ↓
[5] TrustedTenantAccessFilter
    ├─ Extract tenant ID from request
    ├─ Validate tenant access from X-Auth-Tenant-Ids
    ├─ Store tenant in request attributes
    ↓
[6] Controller
    └─ Service handling
```

---

## Monitoring & Observability

### Prometheus Metrics

The gateway and backend services export metrics to Prometheus:

```bash
# Quality Measure Service metrics
curl http://localhost:8087/quality-measure/actuator/prometheus | grep -E "http_requests_total|authentication"

# Expected metrics:
# http_requests_total{method="GET",path="/api/v1/measures",status="200"} 125
# http_requests_total{method="GET",path="/api/v1/patients",status="401"} 3
# auth_filter_validation_total{filter="TrustedHeaderAuthFilter",result="success"} 125
# auth_filter_validation_total{filter="TrustedHeaderAuthFilter",result="failed"} 1
```

### Logging

Enable debug logging for authentication filters:

```yaml
# src/main/resources/application-production.yml
logging:
  level:
    com.healthdata.authentication: DEBUG
    org.springframework.security: INFO
```

### Audit Trail

All authenticated requests are logged in the audit table:

```sql
SELECT * FROM audit_log
WHERE event_type = 'AUTH_FILTER'
  AND timestamp > NOW() - INTERVAL '1 hour'
ORDER BY timestamp DESC
LIMIT 20;
```

---

## Troubleshooting

### Issue: "Missing or invalid authentication" for all requests

**Cause**: HMAC signature validation failed
**Solution**:
```bash
# 1. Verify gateway is injecting headers
docker logs healthdata-gateway-service | grep X-Auth

# 2. Verify secret matches between gateway and service
# Gateway: echo $GATEWAY_AUTH_SIGNING_SECRET
# Service: Check docker compose environment variable

# 3. Check service logs
docker logs healthdata-quality-measure-service | grep -i "signature\|validation"
```

### Issue: Tenant isolation not working (can access other tenant's data)

**Cause**: TrustedTenantAccessFilter not in filter chain
**Solution**:
```bash
# 1. Verify filter is configured in SecurityFilterChain bean
# Check: securityFilterChain() method includes:
#   http.addFilterAfter(trustedTenantAccessFilter, TrustedHeaderAuthFilter.class)

# 2. Verify filter bean is created
docker exec healthdata-quality-measure-service \
  curl -s http://localhost:8087/quality-measure/actuator/beans | jq '.[] | select(.name | contains("TrustedTenant"))'

# 3. Test with logging
# Update application.yml: logging.level.com.healthdata.authentication = DEBUG
# Restart service and check logs
```

### Issue: Services unable to communicate with Gateway

**Cause**: Network isolation or URL misconfiguration
**Solution**:
```bash
# 1. Verify network connectivity
docker exec healthdata-quality-measure-service \
  curl -v http://healthdata-gateway-service:8080/actuator/health

# 2. Check gateway service logs
docker logs healthdata-gateway-service | tail -50

# 3. Verify service can reach database
docker exec healthdata-quality-measure-service \
  curl -s http://localhost:8087/quality-measure/actuator/health
```

---

## Compliance & Security

### HIPAA Compliance (§164.312(d) - Person or Entity Authentication)

- ✅ Single-factor authentication via gateway
- ✅ JWT token validation with HMAC signature
- ✅ Automatic session timeout (HIPAA_AUTO_LOGOUT_TIMEOUT)
- ✅ Audit logging of all access attempts

### Security Best Practices

1. **Rotate Secrets**: Quarterly or after staff changes
2. **Monitor Access**: Review audit logs for suspicious patterns
3. **Update Dependencies**: Apply security patches within 30 days
4. **Network Segmentation**: Restrict backend service access to gateway only
5. **Encryption in Transit**: All internal traffic encrypted (TLS 1.2+)

---

## Support & Documentation

### Related Documentation
- `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md` - Technical architecture
- `CLAUDE.md` - Project guidelines and conventions
- `docs/PRODUCTION_SECURITY_GUIDE.md` - Security hardening guide

### Getting Help
- Review logs: `docker logs <service-name> | grep -i error`
- Check health: `curl http://localhost:<port>/actuator/health`
- Run tests: `./gradlew :modules:services:SERVICE_NAME:test`

---

*Deployment Guide v1.0.0 - December 30, 2025*
