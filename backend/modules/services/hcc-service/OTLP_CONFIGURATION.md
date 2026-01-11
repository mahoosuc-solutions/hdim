# HCC Service - OpenTelemetry OTLP Configuration

## Overview

This document details the OpenTelemetry (OTLP) configuration for the HCC Risk Adjustment Service, including fixes applied to resolve IPv6 connection issues and ensure proper distributed tracing integration with Jaeger.

## Date: 2026-01-11

## Issues Identified and Fixed

### 1. Missing OTLP Protocol Configuration

**Issue**: The `OTEL_EXPORTER_OTLP_PROTOCOL` environment variable was not set in `docker-compose.yml`.

**Impact**: Without this setting, the OTLP exporter may default to gRPC instead of HTTP, causing connection issues with Jaeger's HTTP endpoint (port 4318).

**Fix Applied**:
```yaml
OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
```

### 2. Incomplete OTLP Endpoint URL

**Issue**: The `OTEL_EXPORTER_OTLP_ENDPOINT` was set to `http://jaeger:4318` instead of the full path `http://jaeger:4318/v1/traces`.

**Impact**: Missing the `/v1/traces` path suffix can cause OTLP HTTP requests to fail with 404 or routing errors.

**Fix Applied**:
```yaml
OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces
```

### 3. Missing IPv4 Stack Preference

**Issue**: The `_JAVA_OPTIONS` environment variable with `-Djava.net.preferIPv4Stack=true` was missing in `docker-compose.yml`.

**Impact**: In Docker containers with dual-stack networking (IPv4/IPv6), Java may attempt IPv6 connections to Jaeger, which can fail if Jaeger only binds to IPv4. This causes OTLP trace export failures.

**Fix Applied** (docker-compose.yml):
```yaml
_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"
```

### 4. Missing IPv4 Stack Preference in Dockerfiles

**Issue**: Neither `Dockerfile` nor `Dockerfile.optimized` included `-Djava.net.preferIPv4Stack=true` in the `JAVA_OPTS` environment variable.

**Impact**: When running containers built from these Dockerfiles without the docker-compose override, OTLP connections may fail due to IPv6 attempts.

**Fix Applied** (both Dockerfiles):
```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.net.preferIPv4Stack=true"
```

## Files Modified

### 1. `/mnt/wd-black/dev/projects/hdim-master/docker-compose.yml`

**Changes**:
- Added `OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf`
- Updated `OTEL_EXPORTER_OTLP_ENDPOINT` from `http://jaeger:4318` to `http://jaeger:4318/v1/traces`
- Added `_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"`
- Added explanatory comments for OTLP configuration

**Location**: Lines 1245-1250 (hcc-service environment section)

### 2. `/mnt/wd-black/dev/projects/hdim-master/backend/modules/services/hcc-service/Dockerfile`

**Changes**:
- Added `-Djava.net.preferIPv4Stack=true` to `JAVA_OPTS` environment variable

**Location**: Line 31

### 3. `/mnt/wd-black/dev/projects/hdim-master/backend/modules/services/hcc-service/Dockerfile.optimized`

**Changes**:
- Added `-Djava.net.preferIPv4Stack=true` to `JAVA_OPTS` environment variable

**Location**: Line 70

## Current OTLP Configuration (Correct)

### Docker Compose Configuration

```yaml
environment:
  # OpenTelemetry OTLP HTTP Exporter (port 4318)
  OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces
  OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
  OTEL_SERVICE_NAME: hcc-service
  # Force IPv4 for OTLP connections (fixes IPv6 connection issues)
  _JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"
```

### Dockerfile Configuration

```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.net.preferIPv4Stack=true"
```

## Testing & Verification

### 1. Verify OTLP Connection

After starting the hcc-service container:

```bash
# Check container logs for OTLP export errors
docker logs healthdata-hcc-service | grep -i "otlp\|trace\|span"

# Should NOT see errors like:
# - "Failed to export spans"
# - "Connection refused"
# - "IPv6 connection timeout"
```

### 2. Verify Traces in Jaeger UI

1. Access Jaeger UI: http://localhost:16686
2. Select "hcc-service" from the Service dropdown
3. Click "Find Traces"
4. Verify that HCC service traces appear with proper span data

### 3. Test IPv4 Preference

```bash
# Exec into the running container
docker exec -it healthdata-hcc-service sh

# Check Java system properties
java -XshowSettings:properties -version 2>&1 | grep preferIPv4Stack
# Should output: java.net.preferIPv4Stack = true
```

## Related Services

The following services have identical OTLP configuration:

- **cql-engine-service** (port 8081) - ✅ Reference implementation
- **consent-service** (port 8082) - ✅ Correct configuration
- **event-processing-service** (port 8083) - ✅ Correct configuration
- **patient-service** (port 8084) - ⚠️ Missing protocol/IPv4
- **fhir-service** (port 8085) - ⚠️ Missing protocol/IPv4
- **care-gap-service** (port 8086) - ⚠️ Missing protocol/IPv4
- **quality-measure-service** (port 8087) - ⚠️ Missing protocol/IPv4
- **event-router-service** (port 8095) - ⚠️ Missing protocol/IPv4
- **ecr-service** (port 8101) - ⚠️ Missing protocol/IPv4
- **prior-auth-service** (port 8102) - ⚠️ Missing protocol/IPv4
- **qrda-export-service** (port 8104) - ⚠️ Missing protocol/IPv4
- **hcc-service** (port 8105) - ✅ **FIXED** (this service)
- **sales-automation-service** (port 8106) - ⚠️ Missing OTLP entirely
- **notification-service** (port 8107) - ⚠️ Missing protocol/IPv4

## Best Practices

1. **Always use the full OTLP endpoint path**: `http://jaeger:4318/v1/traces` (not just `http://jaeger:4318`)
2. **Always specify the protocol**: `OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf`
3. **Always force IPv4 in Docker**: `_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"`
4. **Include IPv4 in Dockerfile JAVA_OPTS**: Ensures consistency across deployment methods
5. **Add comments**: Clearly document OTLP configuration blocks for future maintainers

## References

- OpenTelemetry Java SDK: https://opentelemetry.io/docs/instrumentation/java/
- Jaeger OTLP Receiver: https://www.jaegertracing.io/docs/latest/apis/#opentelemetry-protocol-stable
- HDIM Gateway Trust Architecture: `/mnt/wd-black/dev/projects/hdim-master/backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`
- HDIM System Architecture: `/mnt/wd-black/dev/projects/hdim-master/docs/architecture/SYSTEM_ARCHITECTURE.md`

## Rollback Procedure

If OTLP configuration causes issues, you can disable tracing temporarily:

```yaml
# In docker-compose.yml, comment out OTLP variables:
environment:
  # OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces
  # OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
  # OTEL_SERVICE_NAME: hcc-service
```

Or set the exporter to `none`:

```yaml
environment:
  OTEL_TRACES_EXPORTER: none
```

## Next Steps

Consider applying the same fixes to other services listed in "Related Services" section to ensure platform-wide OTLP consistency.

---

**Author**: Claude Code (AI Assistant)
**Date**: January 11, 2026
**Service**: hcc-service (HCC Risk Adjustment Service)
**Version**: 1.0
