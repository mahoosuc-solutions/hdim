# Event Processing Service - OTLP Configuration

## Overview
This document describes the OpenTelemetry Protocol (OTLP) configuration for the event-processing-service, including fixes applied to resolve IPv6 connection issues with Jaeger.

## Configuration Files

### 1. docker-compose.yml

**Location**: `/mnt/wd-black/dev/projects/hdim-master/docker-compose.yml` (lines 346-351)

**Environment Variables**:
```yaml
# OpenTelemetry OTLP HTTP Exporter (port 4318)
OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces
OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
OTEL_SERVICE_NAME: event-processing-service
# Force IPv4 for OTLP connections (fixes IPv6 connection issues)
_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"
```

**Purpose**:
- `OTEL_EXPORTER_OTLP_ENDPOINT`: Points to Jaeger container using HTTP protocol on port 4318
- `OTEL_EXPORTER_OTLP_PROTOCOL`: Specifies HTTP with protobuf encoding (required for proper serialization)
- `OTEL_SERVICE_NAME`: Identifies traces from this service in Jaeger UI
- `_JAVA_OPTIONS`: Forces IPv4 stack to avoid IPv6 connection failures

### 2. Dockerfile

**Location**: `/mnt/wd-black/dev/projects/hdim-master/backend/modules/services/event-processing-service/Dockerfile` (lines 26-31)

**JAVA_OPTS**:
```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.net.preferIPv4Stack=true"
```

**Purpose**:
- Added `-Djava.net.preferIPv4Stack=true` to ensure IPv4 preference at the JVM level
- Works in conjunction with `_JAVA_OPTIONS` environment variable for redundancy

### 3. application.yml

**Location**: `/mnt/wd-black/dev/projects/hdim-master/backend/modules/services/event-processing-service/src/main/resources/application.yml`

**Status**: ✅ No OTLP-related configuration needed
- No conflicting OTLP settings
- No invalid autoconfigure exclusions
- Configuration is handled entirely via environment variables

## Issues Fixed

### Issue 1: Missing OTLP Protocol Specification
**Problem**: `OTEL_EXPORTER_OTLP_PROTOCOL` was not set, potentially causing default serialization issues.

**Fix**: Added `OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf` to docker-compose.yml

**Impact**: Ensures consistent protobuf encoding for trace data sent to Jaeger.

### Issue 2: IPv6 Connection Failures
**Problem**: JVM was attempting IPv6 connections to Jaeger, which can fail in Docker environments.

**Fix**: Added `_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"` to docker-compose.yml and Dockerfile

**Impact**: Forces IPv4-only connections, preventing connection timeouts and trace data loss.

### Issue 3: Dockerfile IPv4 Preference
**Problem**: Dockerfile did not include IPv4 preference in JAVA_OPTS.

**Fix**: Added `-Djava.net.preferIPv4Stack=true` to JAVA_OPTS in Dockerfile

**Impact**: Ensures IPv4 preference even when container is run outside docker-compose context.

## Verification

### Check Service Logs
```bash
docker logs healthdata-event-processing-service | grep -i "otel\|trace"
```

**Expected Output**:
- No "Connection refused" or "Connection timeout" errors
- Successful trace export messages (if any)

### Verify Jaeger Traces
1. Access Jaeger UI: http://localhost:16686
2. Select service: `event-processing-service`
3. Click "Find Traces"
4. Verify traces appear for service operations

### Test Configuration
```bash
# Restart service to apply configuration
docker compose restart event-processing-service

# Check environment variables
docker exec healthdata-event-processing-service env | grep -E "OTEL|JAVA_OPTIONS"
```

**Expected Output**:
```
OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4318/v1/traces
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
OTEL_SERVICE_NAME=event-processing-service
_JAVA_OPTIONS=-Djava.net.preferIPv4Stack=true
```

## Architecture Reference

### Jaeger Ports
- **16686**: Jaeger UI (web interface)
- **4317**: OTLP gRPC receiver (not used by this service)
- **4318**: OTLP HTTP receiver (used by this service)
- **14250**: Jaeger model protocol
- **6831/udp**: Thrift compact protocol

### Service Integration
```
event-processing-service (Port 8083)
    │
    ├─ Consumes: Kafka events
    ├─ Produces: Processed events
    │
    └─ Traces to → Jaeger OTLP HTTP (Port 4318)
                      │
                      └─ Viewed via → Jaeger UI (Port 16686)
```

## Troubleshooting

### Problem: Traces Not Appearing in Jaeger

**Diagnosis**:
```bash
# Check if Jaeger is running
docker ps | grep jaeger

# Check Jaeger logs
docker logs healthdata-jaeger

# Check service logs for OTLP errors
docker logs healthdata-event-processing-service 2>&1 | grep -i "otlp\|trace\|span"
```

**Common Causes**:
1. Jaeger container not running
2. Network connectivity issues (check `healthdata-network`)
3. IPv6 connection issues (should be fixed with IPv4 preference)
4. Incorrect OTLP endpoint URL

### Problem: IPv6 Connection Errors

**Symptoms**:
```
java.net.ConnectException: Connection refused (Connection refused)
```

**Solution**:
Verify both `_JAVA_OPTIONS` and `JAVA_OPTS` include `-Djava.net.preferIPv4Stack=true`

### Problem: Service Won't Start

**Diagnosis**:
```bash
docker logs healthdata-event-processing-service --tail 100
```

**Common Causes**:
1. Database connection issues (check PostgreSQL)
2. Kafka connection issues (check Kafka broker)
3. Invalid environment variable syntax

## Compliance Notes

### HIPAA Considerations
- **PHI in Traces**: Ensure no Protected Health Information (PHI) is included in trace attributes
- **Trace Retention**: Jaeger traces should have appropriate retention policies
- **Access Control**: Jaeger UI should be secured in production environments

### Production Recommendations

1. **Enable Authentication**: Secure Jaeger UI with OAuth/SSO
2. **Configure Sampling**: Use adaptive sampling to reduce trace volume
3. **Set Retention Policy**: Configure trace retention based on compliance requirements
4. **Monitor Performance**: Track OTLP export latency and failure rates

**Production docker-compose.yml additions**:
```yaml
environment:
  # Sampling configuration (production)
  OTEL_TRACES_SAMPLER: parentbased_traceidratio
  OTEL_TRACES_SAMPLER_ARG: "0.1"  # Sample 10% of traces

  # Batch export configuration (reduce overhead)
  OTEL_BSP_SCHEDULE_DELAY: "5000"
  OTEL_BSP_MAX_QUEUE_SIZE: "2048"
  OTEL_BSP_MAX_EXPORT_BATCH_SIZE: "512"
```

## Related Documentation

- **Gateway Trust Architecture**: `/mnt/wd-black/dev/projects/hdim-master/backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`
- **System Architecture**: `/mnt/wd-black/dev/projects/hdim-master/docs/architecture/SYSTEM_ARCHITECTURE.md`
- **CLAUDE.md**: `/mnt/wd-black/dev/projects/hdim-master/CLAUDE.md`

## Change History

| Date | Change | Author |
|------|--------|--------|
| 2026-01-11 | Initial configuration audit and fixes | Claude Sonnet 4.5 |
| 2026-01-11 | Added OTEL_EXPORTER_OTLP_PROTOCOL environment variable | Claude Sonnet 4.5 |
| 2026-01-11 | Added IPv4 preference to docker-compose.yml and Dockerfile | Claude Sonnet 4.5 |
| 2026-01-11 | Created OTLP_CONFIGURATION.md documentation | Claude Sonnet 4.5 |

---

**Last Updated**: 2026-01-11
**Service**: event-processing-service
**Port**: 8083
**Jaeger UI**: http://localhost:16686
