# Prior Authorization Service - OTLP Configuration

**Service:** prior-auth-service
**Port:** 8102
**Date:** 2026-01-11
**Status:** Fixed and Standardized

## Overview

This document describes the OpenTelemetry Protocol (OTLP) configuration for the Prior Authorization Service, which enables distributed tracing to Jaeger.

## Issues Found and Fixed

### 1. Docker Compose Configuration Issues

**Previous Configuration:**
```yaml
TRACING_URL: http://jaeger:4318/v1/traces
OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318
OTEL_SERVICE_NAME: prior-auth-service
```

**Issues:**
- ❌ Missing `OTEL_EXPORTER_OTLP_PROTOCOL` specification
- ❌ Incomplete OTLP endpoint (missing `/v1/traces` path)
- ❌ No IPv4 stack preference (causes connection failures)
- ❌ Duplicate `TRACING_URL` (legacy, not used by Spring Boot OTLP)
- ❌ Missing Gateway Trust Authentication configuration

**Fixed Configuration:**
```yaml
# OpenTelemetry OTLP HTTP Exporter (port 4318)
OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces
OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
OTEL_SERVICE_NAME: prior-auth-service
# Force IPv4 for OTLP connections (fixes IPv6 connection issues)
_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"
# Gateway Trust Authentication - trusts gateway-injected X-Auth-* headers
GATEWAY_AUTH_DEV_MODE: "true"
GATEWAY_AUTH_SIGNING_SECRET: ${GATEWAY_AUTH_SIGNING_SECRET:-}
```

### 2. Dockerfile JVM Options

**Previous Configuration:**
```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"
```

**Issue:**
- ❌ Missing `-Djava.net.preferIPv4Stack=true` for consistent IPv4 networking

**Fixed Configuration:**
```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.net.preferIPv4Stack=true"
```

### 3. Application.yml Configuration

**Status:** ✅ No changes needed

The `application.yml` correctly delegates OTLP configuration to environment variables, which is the preferred pattern for containerized deployments.

## Current Configuration

### Environment Variables (docker-compose.yml)

| Variable | Value | Purpose |
|----------|-------|---------|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://jaeger:4318/v1/traces` | Jaeger OTLP HTTP endpoint with full path |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | `http/protobuf` | Use HTTP with Protocol Buffers encoding |
| `OTEL_SERVICE_NAME` | `prior-auth-service` | Service identifier in Jaeger UI |
| `_JAVA_OPTIONS` | `-Djava.net.preferIPv4Stack=true` | Force IPv4 networking (fixes connection issues) |

### Jaeger Integration

**Jaeger Container:** `healthdata-jaeger`
**OTLP HTTP Endpoint:** Port 4318
**Jaeger UI:** http://localhost:16686

**How to View Traces:**
1. Access Jaeger UI at http://localhost:16686
2. Select "prior-auth-service" from the Service dropdown
3. Click "Find Traces" to view distributed traces
4. Traces include:
   - Prior authorization request processing
   - Payer API interactions (with circuit breaker status)
   - Provider access validations
   - Database operations
   - Kafka message publishing

### IPv4 Preference

**Why This Matters:**
- Docker containers may prefer IPv6 by default
- Jaeger OTLP exporter had issues with IPv6 connections
- Setting `java.net.preferIPv4Stack=true` ensures consistent IPv4 usage
- Applied in both `_JAVA_OPTIONS` (runtime) and `JAVA_OPTS` (Dockerfile)

## Files Modified

1. `/mnt/wd-black/dev/projects/hdim-master/docker-compose.yml`
   - Updated prior-auth-service environment variables (lines 1310-1320)
   - Added OTLP protocol specification
   - Fixed OTLP endpoint path
   - Added IPv4 preference
   - Added Gateway Trust Authentication

2. `/mnt/wd-black/dev/projects/hdim-master/backend/modules/services/prior-auth-service/Dockerfile`
   - Added `-Djava.net.preferIPv4Stack=true` to JAVA_OPTS (line 31)

## Validation

### Test OTLP Connection

```bash
# Start the service
docker compose --profile core up -d prior-auth-service

# Check logs for OTLP initialization
docker logs healthdata-prior-auth-service | grep -i "otlp\|telemetry\|tracing"

# Verify Jaeger connectivity
docker logs healthdata-prior-auth-service | grep -i "jaeger"

# Trigger a traced operation
curl -X POST http://localhost:8102/prior-auth/api/v1/requests \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: acme-health" \
  -d '{...}'

# View traces in Jaeger UI
open http://localhost:16686
```

### Expected Log Output

```
INFO  o.s.b.a.t.o.OpenTelemetryAutoConfiguration - Enabling OpenTelemetry
INFO  i.o.e.otlp.http.trace.OtlpHttpSpanExporter - Initialized OTLP HTTP exporter with endpoint: http://jaeger:4318/v1/traces
```

## Comparison with Other Services

The prior-auth-service now matches the OTLP configuration pattern used by:
- ✅ cql-engine-service
- ✅ consent-service
- ✅ patient-service
- ✅ fhir-service
- ✅ care-gap-service
- ✅ hcc-service

## Troubleshooting

### Issue: No traces appearing in Jaeger

**Check:**
1. Verify Jaeger container is running: `docker ps | grep jaeger`
2. Check OTLP endpoint configuration: `docker exec healthdata-prior-auth-service env | grep OTEL`
3. Review service logs for connection errors: `docker logs healthdata-prior-auth-service`

### Issue: IPv6 connection errors

**Symptom:**
```
ERROR i.o.e.otlp.http.trace.OtlpHttpSpanExporter - Failed to export spans
java.net.ConnectException: Connection refused (Connection refused)
```

**Solution:**
- Ensure `_JAVA_OPTIONS` includes `-Djava.net.preferIPv4Stack=true` in docker-compose.yml
- Rebuild and restart the container: `docker compose up -d --build prior-auth-service`

### Issue: 404 errors from Jaeger

**Symptom:**
```
WARN i.o.e.otlp.http.trace.OtlpHttpSpanExporter - Failed to export spans. Server responded with HTTP status code 404
```

**Solution:**
- Endpoint was missing `/v1/traces` path (now fixed)
- Correct endpoint: `http://jaeger:4318/v1/traces`

## Related Documentation

- **Gateway Trust Architecture:** `/mnt/wd-black/dev/projects/hdim-master/backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`
- **System Architecture:** `/mnt/wd-black/dev/projects/hdim-master/docs/architecture/SYSTEM_ARCHITECTURE.md`
- **CLAUDE.md (Project Guidelines):** `/mnt/wd-black/dev/projects/hdim-master/CLAUDE.md`

## References

- OpenTelemetry OTLP Specification: https://opentelemetry.io/docs/specs/otlp/
- Jaeger OTLP Support: https://www.jaegertracing.io/docs/1.52/apis/#opentelemetry-protocol-stable
- Spring Boot OpenTelemetry: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.micrometer-tracing

---

**Last Updated:** 2026-01-11
**Author:** HDIM Platform Team (via Claude Code)
