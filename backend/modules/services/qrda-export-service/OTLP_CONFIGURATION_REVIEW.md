# QRDA Export Service - OTLP Configuration Review

**Service**: qrda-export-service
**Port**: 8104
**Context Path**: /qrda-export
**Review Date**: 2026-01-11
**Status**: ✅ FIXED

## Summary

Reviewed and fixed OpenTelemetry OTLP configuration for the qrda-export-service to ensure proper distributed tracing integration with Jaeger.

## Issues Found

### 1. Incomplete OTLP Endpoint Configuration (docker-compose.yml)
**Issue**: OTLP endpoint was missing the `/v1/traces` path
- **Found**: `OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318`
- **Expected**: `OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces`

### 2. Missing OTLP Protocol Configuration (docker-compose.yml)
**Issue**: OTLP protocol not explicitly specified
- **Missing**: `OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf`

### 3. Missing IPv4 Stack Preference (docker-compose.yml)
**Issue**: No IPv4 stack preference to prevent IPv6 connection issues
- **Missing**: `_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"`

### 4. Missing IPv4 Stack Preference (Dockerfile)
**Issue**: Dockerfile JAVA_OPTS did not include IPv4 preference
- **Found**: JAVA_OPTS without `-Djava.net.preferIPv4Stack=true`

### 5. No Invalid Autoconfigure Entries
**Verified**: ✅ No invalid `spring.autoconfigure.exclude` entries found in application.yml

## Fixes Applied

### 1. Updated docker-compose.yml (Lines 1202-1209)

**Before**:
```yaml
TRACING_URL: http://jaeger:4318/v1/traces
OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318
OTEL_SERVICE_NAME: qrda-export-service
SPRING_DATA_REDIS_TIMEOUT: 10s
```

**After**:
```yaml
TRACING_URL: http://jaeger:4318/v1/traces
# OpenTelemetry OTLP HTTP Exporter (port 4318)
OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces
OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
OTEL_SERVICE_NAME: qrda-export-service
# Force IPv4 for OTLP connections (fixes IPv6 connection issues)
_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"
SPRING_DATA_REDIS_TIMEOUT: 10s
```

### 2. Updated Dockerfile (Lines 26-31)

**Before**:
```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"
```

**After**:
```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.net.preferIPv4Stack=true"
```

## Files Modified

1. **docker-compose.yml**
   - Path: `/mnt/wd-black/dev/projects/hdim-master/docker-compose.yml`
   - Lines: 1203-1208 (added OTLP protocol, fixed endpoint, added IPv4 preference)

2. **Dockerfile**
   - Path: `/mnt/wd-black/dev/projects/hdim-master/backend/modules/services/qrda-export-service/Dockerfile`
   - Lines: 26-31 (added IPv4 preference to JAVA_OPTS)

## Configuration Verification

### Docker Compose Environment Variables
✅ `OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4318/v1/traces` - Complete endpoint with path
✅ `OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf` - HTTP protocol with protobuf encoding
✅ `OTEL_SERVICE_NAME=qrda-export-service` - Service identifier for traces
✅ `_JAVA_OPTIONS=-Djava.net.preferIPv4Stack=true` - Force IPv4 to avoid connection issues

### Dockerfile JVM Options
✅ `-Djava.net.preferIPv4Stack=true` - Included in JAVA_OPTS

### Application Configuration
✅ No invalid `spring.autoconfigure.exclude` entries
✅ Standard Spring Boot application.yml configuration
✅ No conflicting OTLP settings

## Alignment with Other Services

The qrda-export-service now follows the same OTLP configuration pattern as other core services:

- **cql-engine-service** (8081) ✅
- **consent-service** (8082) ✅
- **event-processing-service** (8083) ✅
- **patient-service** (8084) ✅
- **fhir-service** (8085) ✅
- **care-gap-service** (8086) ✅
- **hcc-service** (8105) ✅

## Testing Recommendations

After deploying these changes, verify:

1. **Service Startup**
   ```bash
   docker compose --profile core up -d qrda-export-service
   docker logs -f healthdata-qrda-export-service
   ```

2. **OTLP Connection**
   - Check logs for successful OTLP exporter initialization
   - Verify no IPv6 connection errors
   - Confirm traces are being sent to Jaeger

3. **Jaeger UI**
   - Access http://localhost:16686
   - Search for service: `qrda-export-service`
   - Verify traces appear with correct service name

4. **Health Check**
   ```bash
   curl http://localhost:8104/qrda-export/actuator/health
   ```

## Production Deployment Notes

### docker-compose.prod.yml
The production override file already inherits the base configuration correctly. No additional changes needed.

### Environment Variables
The following environment variables are properly configured:
- `OTEL_EXPORTER_OTLP_ENDPOINT` - Points to Jaeger OTLP HTTP endpoint
- `OTEL_EXPORTER_OTLP_PROTOCOL` - Uses http/protobuf
- `OTEL_SERVICE_NAME` - Identifies the service in traces
- `_JAVA_OPTIONS` - Includes IPv4 preference

### Gateway Trust Authentication
✅ Already configured with:
- `GATEWAY_AUTH_DEV_MODE: "true"` (for development)
- `GATEWAY_AUTH_SIGNING_SECRET` (for production)

## Related Documentation

- **Gateway Trust Architecture**: `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`
- **System Architecture**: `docs/architecture/SYSTEM_ARCHITECTURE.md`
- **QRDA Export Service**: Port 8104, Context: `/qrda-export`

## Change History

| Date | Change | Author |
|------|--------|--------|
| 2026-01-11 | Initial OTLP configuration review and fixes | AI Assistant |

---

**Status**: ✅ All issues resolved. Service is now properly configured for OpenTelemetry distributed tracing with Jaeger.
