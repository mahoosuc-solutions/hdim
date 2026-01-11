# Care Gap Service - OpenTelemetry OTLP Configuration

## Overview

This document describes the OpenTelemetry OTLP (OpenTelemetry Protocol) configuration for the care-gap-service, which enables distributed tracing to Jaeger.

## Configuration Status

✅ **FIXED** - 2026-01-11

The care-gap-service OTLP configuration has been standardized to match other HDIM services (cql-engine-service, fhir-service, patient-service).

## Issues Found and Fixed

### 1. Missing OTLP Protocol Specification

**Issue:** The docker-compose.yml was missing the `OTEL_EXPORTER_OTLP_PROTOCOL` environment variable.

**Impact:** OTLP client might default to gRPC protocol instead of HTTP, causing connection failures to Jaeger's HTTP endpoint (port 4318).

**Fix:** Added `OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf` environment variable.

### 2. Incomplete OTLP Endpoint

**Issue:** The `OTEL_EXPORTER_OTLP_ENDPOINT` was set to `http://jaeger:4318` instead of `http://jaeger:4318/v1/traces`.

**Impact:** Missing the required path `/v1/traces` can cause the exporter to fail or send traces to the wrong endpoint.

**Fix:** Updated to `http://jaeger:4318/v1/traces`.

### 3. Missing IPv4 Preference

**Issue:** Both docker-compose.yml and Dockerfile were missing the `-Djava.net.preferIPv4Stack=true` JVM option.

**Impact:** Java applications can attempt IPv6 connections first, which may fail in Docker environments where IPv6 is not properly configured, causing connection timeouts.

**Fix:**
- Added `_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"` in docker-compose.yml
- Added `-Djava.net.preferIPv4Stack=true` to `JAVA_OPTS` in Dockerfile

## Current Configuration

### docker-compose.yml

```yaml
environment:
  # OpenTelemetry OTLP HTTP Exporter (port 4318)
  OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces
  OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
  OTEL_SERVICE_NAME: care-gap-service
  # Force IPv4 for OTLP connections (fixes IPv6 connection issues)
  _JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"
```

### Dockerfile

```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.net.preferIPv4Stack=true"
```

### application.yml

No OTLP-specific configuration required in application.yml. All tracing configuration is provided via environment variables in docker-compose.yml.

## Verification

To verify OTLP tracing is working:

1. **Start the services:**
   ```bash
   docker compose --profile core up -d
   ```

2. **Trigger API requests to care-gap-service:**
   ```bash
   curl -X GET http://localhost:8086/care-gap/actuator/health
   ```

3. **Check Jaeger UI:**
   - Open http://localhost:16686
   - Select "care-gap-service" from the Service dropdown
   - Click "Find Traces"
   - Verify traces appear with proper span details

4. **Check service logs for OTLP errors:**
   ```bash
   docker logs healthdata-care-gap-service 2>&1 | grep -i otlp
   docker logs healthdata-care-gap-service 2>&1 | grep -i "connection refused"
   ```

## Related Services

The following services have the same OTLP configuration pattern:

- ✅ cql-engine-service (reference implementation)
- ✅ fhir-service
- ✅ patient-service
- ✅ quality-measure-service
- ✅ event-processing-service
- ✅ event-router-service
- ✅ ecr-service
- ✅ qrda-export-service
- ✅ hcc-service
- ✅ prior-auth-service
- ✅ gateway-service

## Dependencies

- **Jaeger All-in-One:** jaegertracing/all-in-one:1.52
- **OTLP Port:** 4318 (HTTP)
- **Jaeger UI:** http://localhost:16686

## References

- [OpenTelemetry Java SDK](https://opentelemetry.io/docs/instrumentation/java/)
- [Jaeger OTLP Support](https://www.jaegertracing.io/docs/1.52/apis/#opentelemetry-protocol-stable)
- [Spring Boot Actuator + OTLP](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics.export.otlp)

## Changelog

### 2026-01-11
- ✅ Added `OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf`
- ✅ Fixed `OTEL_EXPORTER_OTLP_ENDPOINT` to include `/v1/traces` path
- ✅ Added `_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"` in docker-compose.yml
- ✅ Added `-Djava.net.preferIPv4Stack=true` to Dockerfile `JAVA_OPTS`
- ✅ Updated comments to match cql-engine-service format
- ✅ Removed obsolete `TRACING_URL` environment variable (redundant with OTEL_EXPORTER_OTLP_ENDPOINT)

---

**Maintained by:** HDIM Platform Team
**Last Updated:** 2026-01-11
