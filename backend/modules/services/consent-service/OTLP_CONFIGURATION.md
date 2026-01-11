# Consent Service - OpenTelemetry OTLP Configuration

## Date: 2026-01-11
## Status: ✅ Fixed

---

## Issues Found

### 1. Missing OTLP Environment Variables in docker-compose.yml
**Location:** `/docker-compose.yml` (Lines 284-318)

**Problem:**
- ❌ No `OTEL_EXPORTER_OTLP_ENDPOINT` configured
- ❌ No `OTEL_EXPORTER_OTLP_PROTOCOL` configured
- ❌ No `OTEL_SERVICE_NAME` configured
- ❌ No `_JAVA_OPTIONS` with IPv4 preference

**Impact:**
- Service cannot export traces to Jaeger
- IPv6 connection issues may occur when attempting OTLP connections
- Traces not visible in Jaeger UI (http://localhost:16686)

### 2. Missing IPv4 Preference in Dockerfile
**Location:** `/backend/modules/services/consent-service/Dockerfile` (Line 26)

**Problem:**
- ❌ `JAVA_OPTS` did not include `-Djava.net.preferIPv4Stack=true`

**Impact:**
- JVM may attempt IPv6 connections to Jaeger OTLP endpoint
- Connection timeouts or failures in Docker networking

---

## Fixes Applied

### docker-compose.yml - Added OTLP Configuration

```yaml
# OpenTelemetry OTLP HTTP Exporter (port 4318)
OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces
OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
OTEL_SERVICE_NAME: consent-service
# Force IPv4 for OTLP connections (fixes IPv6 connection issues)
_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"
```

**Reasoning:**
- `OTEL_EXPORTER_OTLP_ENDPOINT`: Points to Jaeger container's OTLP HTTP endpoint (port 4318)
- `OTEL_EXPORTER_OTLP_PROTOCOL`: Uses HTTP with protobuf serialization (standard for OTLP)
- `OTEL_SERVICE_NAME`: Identifies traces in Jaeger as "consent-service"
- `_JAVA_OPTIONS`: Forces IPv4 stack to avoid Docker IPv6 networking issues

### Dockerfile - Added IPv4 Preference to JAVA_OPTS

```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.net.preferIPv4Stack=true"
```

**Reasoning:**
- Ensures IPv4 preference is set even if `_JAVA_OPTIONS` environment variable is not present
- Provides defense-in-depth for OTLP connectivity

---

## Verification

### 1. Verify OTLP Configuration

```bash
# Start consent-service with Jaeger
docker compose --profile core up -d consent-service jaeger

# Check environment variables
docker exec healthdata-consent-service env | grep OTEL

# Expected output:
# OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4318/v1/traces
# OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
# OTEL_SERVICE_NAME=consent-service
```

### 2. Verify IPv4 Preference

```bash
# Check JVM options
docker exec healthdata-consent-service env | grep JAVA

# Expected output should include:
# _JAVA_OPTIONS=-Djava.net.preferIPv4Stack=true
```

### 3. Verify Tracing in Jaeger UI

```bash
# Access Jaeger UI
open http://localhost:16686

# Steps:
# 1. Select "consent-service" from Service dropdown
# 2. Click "Find Traces"
# 3. Create some consent activity:
curl -X POST http://localhost:8082/consent/api/consents \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-User-Id: user-123" \
  -H "X-Auth-Roles: ADMIN" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "patient-test-001",
    "scope": "treatment",
    "category": "clinical-notes",
    "dataClass": "general",
    "validFrom": "2024-01-01",
    "validTo": "2025-01-01",
    "consentDate": "2024-01-01"
  }'

# 4. Refresh Jaeger UI - should see traces for consent creation
```

### 4. Check Service Logs

```bash
# Check for OTLP connection logs
docker logs healthdata-consent-service 2>&1 | grep -i "otel\|trace\|jaeger"

# Look for successful connections (no errors about unreachable endpoints)
```

---

## Configuration Details

### OpenTelemetry OTLP Exporter

| Variable | Value | Purpose |
|----------|-------|---------|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://jaeger:4318/v1/traces` | Jaeger OTLP HTTP endpoint |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | `http/protobuf` | OTLP over HTTP with protobuf |
| `OTEL_SERVICE_NAME` | `consent-service` | Service identifier in Jaeger |
| `_JAVA_OPTIONS` | `-Djava.net.preferIPv4Stack=true` | Force IPv4 networking |

### Jaeger Ports

| Port | Protocol | Purpose |
|------|----------|---------|
| 16686 | HTTP | Jaeger UI |
| 4317 | gRPC | OTLP gRPC (not used) |
| 4318 | HTTP | OTLP HTTP (used by consent-service) |
| 14250 | gRPC | Jaeger model |
| 6831 | UDP | Thrift compact |

---

## Related Services Using Similar Configuration

These services have correct OTLP configuration and can be used as reference:

1. **cql-engine-service** (Port 8081)
   - ✅ OTLP endpoint configured
   - ✅ IPv4 preference enabled
   - Reference: `docker-compose.yml` lines 266-271

2. **gateway-service** (Port 8080)
   - ✅ OTLP endpoint configured
   - Reference: `docker-compose.yml` lines 224-226

3. **event-processing-service** (Port 8083)
   - ✅ OTLP endpoint configured
   - Reference: `docker-compose.yml` lines 346-348

---

## Consistency Check

The consent-service OTLP configuration now matches the standard pattern used across the platform:

```yaml
# Standard OTLP configuration pattern (all services)
OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces
OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf  # Optional but recommended
OTEL_SERVICE_NAME: <service-name>
_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"
```

---

## Troubleshooting

### Issue: No traces appearing in Jaeger

**Check:**
1. Verify Jaeger is running: `docker ps | grep jaeger`
2. Verify consent-service can reach Jaeger: `docker exec healthdata-consent-service ping jaeger`
3. Check OTLP endpoint: `docker exec healthdata-consent-service curl -v http://jaeger:4318/v1/traces`

### Issue: IPv6 connection errors in logs

**Check:**
1. Verify `_JAVA_OPTIONS` is set: `docker exec healthdata-consent-service env | grep _JAVA_OPTIONS`
2. Verify Dockerfile includes IPv4 preference: `grep preferIPv4Stack backend/modules/services/consent-service/Dockerfile`

### Issue: Environment variable not being applied

**Fix:**
1. Rebuild service: `docker compose build consent-service`
2. Restart service: `docker compose up -d consent-service`
3. Verify environment: `docker exec healthdata-consent-service env | grep OTEL`

---

## Files Modified

1. `/docker-compose.yml`
   - Added OTLP environment variables to consent-service configuration
   - Lines 309-314

2. `/backend/modules/services/consent-service/Dockerfile`
   - Added `-Djava.net.preferIPv4Stack=true` to `JAVA_OPTS`
   - Line 31

---

## References

- **OTLP Specification:** https://opentelemetry.io/docs/specs/otlp/
- **Jaeger OTLP Support:** https://www.jaegertracing.io/docs/latest/apis/#opentelemetry-protocol-stable
- **Gateway Trust Architecture:** `/backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`
- **Service README:** `/backend/modules/services/consent-service/README.md`

---

*Last Updated: 2026-01-11*
*Status: Configuration fixed and documented*
