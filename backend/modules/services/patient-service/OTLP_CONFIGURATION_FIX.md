# Patient Service - OTLP Configuration Fix

**Date:** 2026-01-11
**Service:** patient-service (Port 8084)
**Status:** ✅ Fixed

## Issues Identified

### 1. docker-compose.yml Configuration Issues
- **Missing**: `OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf` environment variable
- **Missing**: `_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"` environment variable
- **Incorrect**: OTLP endpoint path was incomplete (`http://jaeger:4318` instead of `http://jaeger:4318/v1/traces`)

### 2. Dockerfile Configuration Issue
- **Missing**: IPv4 preference flag (`-Djava.net.preferIPv4Stack=true`) in JAVA_OPTS

## Root Cause

The patient-service was not properly configured for OpenTelemetry OTLP export to Jaeger, which could cause:
1. Connection failures due to IPv6/IPv4 networking issues in Docker
2. Incorrect OTLP protocol negotiation
3. Traces not being exported to the Jaeger backend at `/v1/traces` endpoint

## Fixes Applied

### 1. docker-compose.yml Changes

**Before:**
```yaml
# OpenTelemetry - use Jaeger container hostname
OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318
OTEL_SERVICE_NAME: patient-service
```

**After:**
```yaml
# OpenTelemetry OTLP HTTP Exporter (port 4318)
OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces
OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
OTEL_SERVICE_NAME: patient-service
# Force IPv4 for OTLP connections (fixes IPv6 connection issues)
_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"
```

**Changes:**
- ✅ Added `/v1/traces` path to OTLP endpoint URL
- ✅ Added `OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf` for protocol specification
- ✅ Added `_JAVA_OPTIONS` with IPv4 preference flag
- ✅ Updated comment to be more descriptive

### 2. Dockerfile Changes

**Before:**
```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"
```

**After:**
```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.net.preferIPv4Stack=true"
```

**Changes:**
- ✅ Added `-Djava.net.preferIPv4Stack=true` flag to force IPv4 networking

### 3. application-docker.yml Review

**Existing Configuration (No Changes Required):**
```yaml
# OpenTelemetry Tracing Configuration
tracing:
  url: http://jaeger:4318/v1/traces
```

**Status:** The application-docker.yml already has the correct tracing URL configuration. No changes needed.

## Configuration Alignment

The patient-service OTLP configuration is now aligned with the reference service (cql-engine-service):

| Configuration Item | patient-service | cql-engine-service | Status |
|-------------------|-----------------|--------------------| -------|
| OTLP Endpoint | `http://jaeger:4318/v1/traces` | `http://jaeger:4318/v1/traces` | ✅ Aligned |
| OTLP Protocol | `http/protobuf` | `http/protobuf` | ✅ Aligned |
| Service Name | `patient-service` | `cql-engine-service` | ✅ Unique |
| IPv4 Preference | `true` | `true` | ✅ Aligned |

## Files Modified

1. **docker-compose.yml**
   - Location: `/mnt/wd-black/dev/projects/hdim-master/docker-compose.yml`
   - Lines: 400-404
   - Changes: Added OTLP protocol, IPv4 preference, corrected endpoint path

2. **Dockerfile**
   - Location: `/mnt/wd-black/dev/projects/hdim-master/backend/modules/services/patient-service/Dockerfile`
   - Lines: 26-31
   - Changes: Added IPv4 preference flag to JAVA_OPTS

## Testing Recommendations

### 1. Verify OTLP Connectivity
```bash
# Start patient-service with the new configuration
docker compose --profile core up -d patient-service

# Check logs for OTLP initialization
docker compose logs patient-service | grep -i "otlp\|telemetry\|trace"

# Verify no IPv6 connection errors
docker compose logs patient-service | grep -i "ipv6\|preferIPv4"
```

### 2. Verify Traces in Jaeger
```bash
# 1. Access Jaeger UI
open http://localhost:16686

# 2. Select service: patient-service
# 3. Trigger some API calls to generate traces
curl -H "X-Tenant-ID: acme-health" http://localhost:8084/patient/actuator/health

# 4. Search for traces in Jaeger UI
```

### 3. Integration Test
```bash
# Full stack test with tracing
docker compose --profile core up -d
docker compose logs -f patient-service jaeger

# Make API calls and verify traces appear in Jaeger
curl -H "X-Tenant-ID: acme-health" \
     -H "Authorization: Bearer <JWT_TOKEN>" \
     http://localhost:8080/patient/api/v1/patients/<PATIENT_ID>
```

## Expected Behavior After Fix

1. **OTLP Connection**: Patient service should successfully connect to Jaeger at `http://jaeger:4318/v1/traces`
2. **Protocol Negotiation**: Should use `http/protobuf` protocol for efficient trace export
3. **IPv4 Networking**: Should use IPv4 stack, avoiding IPv6-related connection issues
4. **Trace Visibility**: Traces should appear in Jaeger UI under service name `patient-service`
5. **No Warnings**: No IPv6 or OTLP connection warnings in logs

## Related Services

The following services have **correct** OTLP configuration (used as reference):
- ✅ cql-engine-service (Port 8081)
- ✅ gateway-service (Port 8080)
- ✅ event-processing-service (Port 8083)
- ✅ quality-measure-service (Port 8087)
- ✅ care-gap-service (Port 8086)
- ✅ fhir-service (Port 8085)

## References

- **OTLP Specification**: https://opentelemetry.io/docs/specs/otlp/
- **Jaeger OTLP Receiver**: https://www.jaegertracing.io/docs/latest/deployment/#collector
- **Docker IPv4 Preference**: Required for containerized Java apps to avoid IPv6 connection delays

## Rollback Instructions

If issues occur, rollback using the backup:

```bash
# Restore docker-compose.yml from backup
cp docker-compose.yml.backup-* docker-compose.yml

# Revert Dockerfile changes
git checkout backend/modules/services/patient-service/Dockerfile

# Restart service
docker compose --profile core up -d --force-recreate patient-service
```

## Compliance Notes

This fix ensures:
- ✅ **Observability**: Proper distributed tracing for HIPAA audit trails
- ✅ **Performance**: No IPv6 connection delays impacting response times
- ✅ **Monitoring**: Healthcare transactions are properly traced and monitored
- ✅ **Debugging**: Complete trace visibility for troubleshooting PHI access patterns

---

**Reviewed by:** Claude Code Agent
**Approved by:** [Pending Review]
**Next Review Date:** 2026-02-11
