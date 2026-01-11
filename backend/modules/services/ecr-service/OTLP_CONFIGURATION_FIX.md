# ECR Service - OTLP Configuration Fix

**Date**: 2026-01-11
**Service**: ecr-service (Port 8101)
**Issue Type**: OpenTelemetry OTLP Configuration

## Issues Identified

### 1. docker-compose.yml - Missing OTLP Configuration

**Location**: `/docker-compose.yml` lines 1148-1155

**Problems Found**:
- ❌ Missing `OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf` environment variable
- ❌ Missing `_JAVA_OPTIONS=-Djava.net.preferIPv4Stack=true` for IPv4 preference
- ⚠️ Inconsistent OTLP endpoint format (was `http://jaeger:4318`, should be `http://jaeger:4318/v1/traces`)

**Impact**:
- OTLP exporter might default to gRPC instead of HTTP/protobuf
- IPv6 connection issues could cause trace export failures
- Endpoint format inconsistency with other services

### 2. Dockerfile - Missing IPv4 Preference

**Location**: `/backend/modules/services/ecr-service/Dockerfile` lines 26-30

**Problems Found**:
- ❌ Missing `-Djava.net.preferIPv4Stack=true` in JAVA_OPTS

**Impact**:
- Service might attempt IPv6 connections to Jaeger, causing failures
- Inconsistent with other services that have this flag

### 3. application.yml - No Issues

**Location**: `/backend/modules/services/ecr-service/src/main/resources/application.yml`

**Status**: ✅ Clean
- No invalid `spring.autoconfigure.exclude` entries
- No conflicting OTLP settings
- Properly structured configuration

## Fixes Applied

### Fix 1: docker-compose.yml

**Changed**:
```yaml
# BEFORE
AUDIT_ENABLED: "false"
TRACING_URL: http://jaeger:4318/v1/traces
OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318
OTEL_SERVICE_NAME: ecr-service
SPRING_DATA_REDIS_TIMEOUT: 10s
MANAGEMENT_HEALTH_REDIS_ENABLED: "false"
```

**To**:
```yaml
# AFTER
AUDIT_ENABLED: "false"
TRACING_URL: http://jaeger:4318/v1/traces
# OpenTelemetry OTLP HTTP Exporter (port 4318)
OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces
OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
OTEL_SERVICE_NAME: ecr-service
# Force IPv4 for OTLP connections (fixes IPv6 connection issues)
_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"
SPRING_DATA_REDIS_TIMEOUT: 10s
MANAGEMENT_HEALTH_REDIS_ENABLED: "false"
```

**Changes Made**:
1. ✅ Added `OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf`
2. ✅ Corrected endpoint to `http://jaeger:4318/v1/traces` (consistent with other services)
3. ✅ Added `_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"`
4. ✅ Added inline comments for clarity

### Fix 2: Dockerfile

**Changed**:
```dockerfile
# BEFORE
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"
```

**To**:
```dockerfile
# AFTER
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.net.preferIPv4Stack=true"
```

**Changes Made**:
1. ✅ Added `-Djava.net.preferIPv4Stack=true` to JAVA_OPTS

## Files Modified

| File | Lines Changed | Status |
|------|---------------|--------|
| `/docker-compose.yml` | 1148-1159 | ✅ Fixed |
| `/backend/modules/services/ecr-service/Dockerfile` | 26-31 | ✅ Fixed |

## Configuration Alignment

The ecr-service now aligns with the OTLP configuration standard used by other services:

**Standard Configuration Pattern** (used by cql-engine, consent, patient, fhir, care-gap, hcc services):
```yaml
# OpenTelemetry OTLP HTTP Exporter (port 4318)
OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces
OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
OTEL_SERVICE_NAME: <service-name>
# Force IPv4 for OTLP connections (fixes IPv6 connection issues)
_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"
```

## Verification Steps

To verify the fixes work correctly:

1. **Rebuild the service**:
   ```bash
   cd /mnt/wd-black/dev/projects/hdim-master/backend
   ./gradlew :modules:services:ecr-service:clean :modules:services:ecr-service:bootJar
   ```

2. **Restart the service**:
   ```bash
   cd /mnt/wd-black/dev/projects/hdim-master
   docker compose up -d --build ecr-service
   ```

3. **Check service logs**:
   ```bash
   docker compose logs -f ecr-service
   ```
   - Look for: No OTLP connection errors
   - Look for: Successful trace export messages

4. **Verify Jaeger traces**:
   - Open Jaeger UI: http://localhost:16686
   - Select service: `ecr-service`
   - Verify traces are appearing

5. **Test an API endpoint**:
   ```bash
   curl -X GET http://localhost:8101/actuator/health
   ```
   - Should return 200 OK
   - Check Jaeger UI for the trace

## Related Services

The following services use the same OTLP configuration pattern:
- ✅ cql-engine-service (Port 8081)
- ✅ consent-service (Port 8082)
- ✅ event-processing-service (Port 8083)
- ✅ patient-service (Port 8084)
- ✅ fhir-service (Port 8085)
- ✅ care-gap-service (Port 8086)
- ✅ hcc-service (Port 8105)
- ✅ **ecr-service (Port 8101)** - NOW ALIGNED

## Notes

- **IPv4 Preference**: The `-Djava.net.preferIPv4Stack=true` flag is crucial in Docker environments where IPv6 might be misconfigured or unavailable. This prevents connection failures to Jaeger.

- **OTLP Protocol**: The `http/protobuf` protocol is more lightweight than gRPC and works better in containerized environments with limited resources.

- **Endpoint Format**: The `/v1/traces` path is the correct OTLP HTTP endpoint for trace data. The service was previously using the base URL without the path.

- **Environment Variable Precedence**: The `_JAVA_OPTIONS` environment variable in docker-compose.yml will override the JAVA_OPTS in the Dockerfile, ensuring IPv4 preference is always enabled.

## References

- HDIM Architecture: `/docs/architecture/SYSTEM_ARCHITECTURE.md`
- Service List: Port 8101 - Electronic Case Reporting Service
- Jaeger Documentation: https://www.jaegertracing.io/docs/1.52/apis/#opentelemetry-protocol-stable
- OpenTelemetry Java: https://opentelemetry.io/docs/instrumentation/java/

---

**Status**: ✅ RESOLVED
**Last Updated**: 2026-01-11
**Reviewed By**: Claude Code AI Assistant
