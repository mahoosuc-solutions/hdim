# Phase 5 Week 3: Production Hardening Guide

**Status**: ✅ COMPLETE
**Date**: January 1, 2026
**Phase**: Phase 5 Week 3 - CI/CD Integration & Production Hardening

---

## Overview

This guide provides comprehensive instructions for hardening the CMS Connector Service for production deployment. It covers HTTPS/TLS configuration, rate limiting, security headers, and compliance validation.

---

## 1. HTTPS/TLS Configuration

### 1.1 Kong API Gateway - HTTPS Setup

**Objective**: Enable HTTPS at the gateway level for all client connections.

#### Prerequisites
- Kong API Gateway installed (version 3.x+)
- SSL/TLS certificate (self-signed, Let's Encrypt, or commercial)
- Kong Admin API access

#### Implementation Steps

**Step 1: Generate or Obtain Certificate**

For development/testing (self-signed):
```bash
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes \
  -subj "/CN=cms-connector.local/O=HealthData/C=US"
```

For production (Let's Encrypt via Certbot):
```bash
certbot certonly --standalone -d cms-connector.example.com --email admin@example.com
# Certificates stored in: /etc/letsencrypt/live/cms-connector.example.com/
```

**Step 2: Create Kong Certificate Object**

```bash
curl -X POST http://localhost:8001/certificates \
  -F "cert=@/path/to/cert.pem" \
  -F "key=@/path/to/key.pem" \
  -F "tags=cms-connector"
```

Response:
```json
{
  "id": "cert-uuid",
  "cert": "-----BEGIN CERTIFICATE-----...",
  "key": "-----BEGIN RSA PRIVATE KEY-----...",
  "tags": ["cms-connector"]
}
```

**Step 3: Create HTTPS Route in Kong**

```bash
# Get service ID first
SERVICE_ID=$(curl -s http://localhost:8001/services | jq -r '.data[] | select(.name=="cms-connector") | .id')

# Create HTTPS route
curl -X POST http://localhost:8001/services/${SERVICE_ID}/routes \
  -d "protocols=https" \
  -d "hosts=cms-connector.example.com" \
  -d "name=cms-connector-https"
```

**Step 4: Enable HTTPS Port in Kong**

Edit `kong.conf`:
```yaml
proxy_listen = 0.0.0.0:80, 0.0.0.0:443 ssl http2
admin_listen = 127.0.0.1:8001
admin_api_enabled_header = off
```

**Step 5: Verify HTTPS Configuration**

```bash
# Test HTTPS endpoint (with self-signed cert: add -k flag)
curl -k https://cms-connector.example.com/api/v1/actuator/health

# View certificate info
openssl s_client -connect cms-connector.example.com:443 </dev/null | openssl x509 -text -noout
```

### 1.2 Spring Boot HTTPS Configuration (Optional - Behind Kong)

If running Spring Boot directly with HTTPS:

```yaml
# application-prod.yml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.jks
    key-store-password: ${KEY_STORE_PASSWORD}
    key-store-type: JKS
    key-alias: tomcat
    protocol: TLSv1.2
```

### 1.3 TLS Version Enforcement

**Minimum TLS Version**: 1.2+

Kong configuration:
```yaml
# kong.conf
ssl_protocols = TLSv1.2 TLSv1.3
ssl_ciphers = ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384
ssl_prefer_server_ciphers = on
```

### 1.4 HTTP to HTTPS Redirect

In Kong, create redirect from HTTP to HTTPS:

```bash
curl -X POST http://localhost:8001/plugins \
  -d "name=request-transformer" \
  -d "config.append.headers=X-Forwarded-Proto: https" \
  -d "service.id=${SERVICE_ID}"
```

Or use redirect plugin:
```bash
curl -X POST http://localhost:8001/plugins \
  -d "name=request-size-limiting" \
  -d "config.allowed_payload_size=100" \
  -d "service.id=${SERVICE_ID}"
```

### 1.5 HSTS (HTTP Strict Transport Security)

Add HSTS header in Kong:

```bash
curl -X POST http://localhost:8001/plugins \
  -d "name=response-transformer" \
  -d "config.add.headers=Strict-Transport-Security: max-age=31536000; includeSubDomains; preload" \
  -d "service.id=${SERVICE_ID}"
```

**Verification**:
```bash
curl -I https://cms-connector.example.com/api/v1/actuator/health | grep Strict-Transport-Security
```

---

## 2. Rate Limiting Configuration

### 2.1 Kong Rate Limiting Plugin

**Objective**: Prevent DDoS and brute force attacks by limiting request rates.

#### Public Endpoints: 100 requests/minute per IP

```bash
curl -X POST http://localhost:8001/routes/cms-connector-public/plugins \
  -d "name=rate-limiting" \
  -d "config.minute=100" \
  -d "config.policy=local" \
  -d "config.limit_by=ip"
```

#### Authenticated Endpoints: 1000 requests/minute per user

```bash
curl -X POST http://localhost:8001/routes/cms-connector-auth/plugins \
  -d "name=rate-limiting" \
  -d "config.minute=1000" \
  -d "config.policy=local" \
  -d "config.limit_by=header" \
  -d "config.header_name=X-Auth-User-Id"
```

#### Admin Endpoints: 500 requests/minute per user

```bash
curl -X POST http://localhost:8001/routes/cms-connector-admin/plugins \
  -d "name=rate-limiting" \
  -d "config.minute=500" \
  -d "config.policy=cluster" \
  -d "config.limit_by=header" \
  -d "config.header_name=X-Auth-User-Id"
```

### 2.2 Rate Limit Response Headers

Kong automatically adds:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 87
X-RateLimit-Reset: 1672531200
```

When limit exceeded (HTTP 429):
```json
{
  "message": "API rate limit exceeded"
}
```

### 2.3 Advanced Rate Limiting (Optional)

**Per-endpoint rate limits**:

```bash
# CMS Claims Search: 50 req/min (expensive operation)
curl -X POST http://localhost:8001/routes/claims-search/plugins \
  -d "name=rate-limiting" \
  -d "config.minute=50"

# Health Check: 1000 req/min (cheap operation)
curl -X POST http://localhost:8001/routes/health-check/plugins \
  -d "name=rate-limiting" \
  -d "config.minute=1000"
```

### 2.4 Whitelist/Bypass Rate Limiting

For internal services or specific IPs:

```bash
curl -X POST http://localhost:8001/plugins \
  -d "name=ip-restriction" \
  -d "config.allow=10.0.0.0/8,192.168.0.0/16" \
  -d "service.id=${SERVICE_ID}"
```

### 2.5 Monitoring Rate Limiting

```bash
# Check current rate limit metrics in Kong
curl http://localhost:8001/routes | jq '.data[] | {id, name, rate_limits: .plugins[] | select(.name=="rate-limiting")}'

# Monitor with Prometheus (if enabled)
curl http://localhost:8001/metrics | grep rate_limit
```

---

## 3. Security Headers Configuration

### 3.1 Required Security Headers

All responses must include these headers:

```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'
```

### 3.2 Kong Configuration for Security Headers

```bash
# Single command to add all headers
curl -X POST http://localhost:8001/plugins \
  -d "name=response-transformer" \
  -d "config.add.headers=X-Content-Type-Options: nosniff" \
  -d "config.add.headers=X-Frame-Options: DENY" \
  -d "config.add.headers=X-XSS-Protection: 1; mode=block" \
  -d "config.add.headers=Strict-Transport-Security: max-age=31536000; includeSubDomains; preload" \
  -d "config.add.headers=Content-Security-Policy: default-src 'self'" \
  -d "service.id=${SERVICE_ID}"
```

### 3.3 Spring Boot Application Headers (Backup)

```yaml
# application-prod.yml
spring:
  security:
    headers:
      cache-control:
        disabled: false
      content-type-options:
        enabled: true
      frame-options:
        enabled: true
        mode: DENY
      xss-protection:
        enabled: true
        block: true
```

### 3.4 Verification

```bash
curl -I https://cms-connector.example.com/api/v1/actuator/health

# Should see:
# x-content-type-options: nosniff
# x-frame-options: DENY
# x-xss-protection: 1; mode=block
# strict-transport-security: max-age=31536000; includeSubDomains; preload
```

---

## 4. CORS Configuration

### 4.1 Kong CORS Plugin

Restrict API access to authorized origins only:

```bash
curl -X POST http://localhost:8001/plugins \
  -d "name=cors" \
  -d "config.origins=https://app.example.com,https://admin.example.com" \
  -d "config.methods=GET,POST,PUT,DELETE,PATCH" \
  -d "config.headers=Content-Type,Authorization,X-Tenant-ID" \
  -d "config.credentials=true" \
  -d "config.max_age=3600" \
  -d "service.id=${SERVICE_ID}"
```

### 4.2 Prohibited Origins

❌ Never allow:
```
Access-Control-Allow-Origin: *
```

✅ Instead use explicit allowlist:
```
Access-Control-Allow-Origin: https://trusted-domain.com
```

---

## 5. Compliance Validation

### 5.1 HIPAA Compliance

- ✅ Authentication enforced (JWT/Bearer tokens)
- ✅ HTTPS/TLS enabled (in-transit encryption)
- ✅ Access logging available (audit trail)
- ⚠️ At-rest encryption (database level - must be configured separately)
- ⚠️ PHI not exposed in error messages (application responsibility)

### 5.2 PCI DSS Compliance

- ✅ Strong authentication (JWT with expiration)
- ✅ Secure communication (HTTPS/TLS 1.2+)
- ✅ Firewall configuration (Kong gateway)
- ✅ Regular vulnerability scanning (Trivy in CI/CD)
- ✅ Role-based access control (JWT claims + Spring Security)
- ⚠️ Rate limiting (gateway level - just configured)
- ⚠️ Logging and monitoring (see Observability guide)

### 5.3 Pre-Deployment Checklist

- [ ] HTTPS enabled on Kong gateway
- [ ] All HTTP traffic redirects to HTTPS
- [ ] TLS 1.2+ configured (1.3 preferred)
- [ ] Rate limiting policies deployed
- [ ] Security headers present on all responses
- [ ] CORS restricted to known domains
- [ ] Certificate validity verified
- [ ] Certificate expiration monitoring configured
- [ ] Load balancer SSL pass-through confirmed
- [ ] Fire wall rules allow 443 (HTTPS)

---

## 6. Certificate Renewal Process

### 6.1 Let's Encrypt Auto-Renewal

```bash
# Install certbot with Kong plugin
sudo apt install certbot python3-certbot-nginx

# Enable auto-renewal
certbot renew --dry-run  # Test renewal process

# Add to crontab to renew automatically
# 0 12 * * * /usr/bin/certbot renew --quiet
```

### 6.2 Kong Certificate Update

After certificate renewal:
```bash
# Delete old certificate
curl -X DELETE http://localhost:8001/certificates/{cert-id}

# Upload new certificate
curl -X POST http://localhost:8001/certificates \
  -F "cert=@/etc/letsencrypt/live/cms-connector.example.com/fullchain.pem" \
  -F "key=@/etc/letsencrypt/live/cms-connector.example.com/privkey.pem"

# Reload Kong
kong reload
```

---

## 7. Troubleshooting

### 7.1 HTTPS Connection Refused

```bash
# Check Kong is listening on 443
sudo netstat -tlnp | grep 443

# Verify certificate is loaded
curl -v https://localhost:443 2>&1 | grep certificate_verify_failed

# Test with self-signed (development only)
curl -k https://cms-connector.example.com/api/v1/actuator/health
```

### 7.2 Rate Limit Not Working

```bash
# Check plugin is enabled
curl http://localhost:8001/plugins | jq '.data[] | select(.name=="rate-limiting")'

# View rate limit metrics
curl http://localhost:8001/routes/{route-id} | jq '.plugins'

# Test rate limit manually
for i in {1..150}; do
  curl -s -w "%{http_code}\n" -o /dev/null https://cms-connector.example.com/api/v1/actuator/health
  sleep 0.5
done
```

### 7.3 Certificate Expired

```bash
# Check expiration date
openssl s_client -connect cms-connector.example.com:443 </dev/null | \
  openssl x509 -noout -dates

# Output should show:
# notBefore=Jan  1 00:00:00 2024 GMT
# notAfter=Jan  1 23:59:59 2025 GMT
```

---

## 8. Performance Impact

| Configuration | Impact | Recommendation |
|---|---|---|
| HTTPS/TLS | +5-10ms per request | ✅ Required for production |
| Rate Limiting (local) | <1ms per request | ✅ Minimal impact |
| Rate Limiting (cluster) | +2-3ms per request | ⚠️ Use for distributed systems |
| Security Headers | <0.5ms per request | ✅ No meaningful impact |

**Total HTTPS + Security Overhead**: ~5-10ms (acceptable)

---

## 9. Next Steps

1. **Immediate** (Week of deployment):
   - [ ] Enable HTTPS on Kong gateway
   - [ ] Configure rate limiting plugins
   - [ ] Add security headers
   - [ ] Test certificate renewal

2. **Short-term** (1-2 weeks):
   - [ ] Enable monitoring of certificate expiration
   - [ ] Configure alerting for rate limit breaches
   - [ ] Document runbook for certificate renewal

3. **Long-term** (monthly):
   - [ ] Review rate limit metrics and adjust if needed
   - [ ] Monitor HTTPS certificate validation in logs
   - [ ] Quarterly penetration testing with hardened config

---

## Support & Documentation

- **Kong Documentation**: https://docs.konghq.com/gateway/latest/secure-services/
- **Let's Encrypt**: https://letsencrypt.org/
- **OWASP Security Headers**: https://owasp.org/www-project-secure-headers/
- **TLS Best Practices**: https://ssl-config.mozilla.org/
- **PCI DSS Requirements**: https://www.pcisecuritystandards.org/
- **HIPAA Compliance**: https://www.hhs.gov/hipaa/

---

**Document Version**: 1.0
**Last Updated**: January 1, 2026
**Status**: ✅ Phase 5 Week 3 - Complete
