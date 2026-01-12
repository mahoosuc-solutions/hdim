# Database Configuration Module

Standardized HikariCP connection pooling for HDIM microservices.

## Overview

This module provides centralized database connection pool configuration based on Phase 3 HikariCP standardization analysis (January 2026). It reduces configuration duplication while preserving service-specific customization options.

**Key Benefits:**
- Standardized connection pool sizing based on traffic patterns
- Sensible defaults for all timeout values
- Clear visibility into configuration via startup logging
- Zero-configuration for new services
- Non-breaking for existing services

## Quick Start

### For New Services

```yaml
healthdata:
  database:
    hikari:
      traffic-tier: MEDIUM  # HIGH (50), MEDIUM (20), LOW (10) connections

spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/healthdata_myservice
    username: healthdata
    password: ${DB_PASSWORD}
```

### For Existing Services

No changes required! Continue using explicit `spring.datasource.hikari.*` configuration.

Optional: Add traffic-tier property to enable configuration logging:

```yaml
healthdata:
  database:
    hikari:
      traffic-tier: MEDIUM  # Shows config in startup logs
```

## Traffic Tiers

Based on Phase 3 analysis of traffic patterns across 34 services:

| Tier | Connections | Min Idle | Services | Use Case |
|------|-------------|----------|----------|----------|
| **HIGH** | 50 | 10 | 3 | Core clinical data services |
| **MEDIUM** | 20 | 5 | 18 | Patient data, analytics, gateways |
| **LOW** | 10 | 5 | 13 | Supporting services |

### Tier Selection Guide

**HIGH** - Choose for services that:
- Handle FHIR R4 resource operations
- Perform CQL/HEDIS measure evaluations
- Process hundreds of concurrent requests
- Are critical path for clinical workflows

**MEDIUM** - Choose for services that:
- Handle patient demographics or clinical records
- Perform analytics or reporting
- Act as API gateways or routers
- Have steady but moderate traffic

**LOW** - Choose for services that:
- Send notifications (email, SMS)
- Handle consent or documentation
- Operate in batch mode
- Have sporadic or low traffic

## Configuration Properties

All timings in milliseconds:

| Property | Default | Description |
|----------|---------|-------------|
| `traffic-tier` | (required) | HIGH, MEDIUM, or LOW |
| `maximum-pool-size` | (tier-based) | Override tier default |
| `minimum-idle` | (tier-based) | Override tier default |
| `connection-timeout` | 20000 | Fail fast (20 sec) |
| `idle-timeout` | 300000 | Match network timeout (5 min) |
| `max-lifetime` | 1800000 | 6x safety margin (30 min) |
| `keepalive-time` | 240000 | Proactive health check (4 min) |
| `leak-detection-threshold` | 60000 | Detect leaks (60 sec) |
| `validation-timeout` | 5000 | Fail fast on dead connections (5 sec) |

## Critical Timing Rules

From Phase 3 bug analysis:

### Rule 1: 6x Safety Margin
```
max-lifetime >= 6x idle-timeout
```
**Why:** Prevents connection pool exhaustion when connections are evicted before they naturally expire. This bug affected notification-service, agent-builder-service, and demo-seeding-service in Phase 3.

### Rule 2: Proactive Health Checks
```
keepalive-time < idle-timeout
```
**Why:** Ensures connections are validated BEFORE they hit the idle timeout, preventing sudden connection failures.

### Rule 3: Fail Fast
```
connection-timeout: 20 seconds
```
**Why:** Prevents request queuing during connection pool exhaustion. Better to fail fast than queue indefinitely.

See Phase 3 Completion Report (`backend/docs/PHASE3_COMPLETION_REPORT.md`) for detailed analysis.

## Migration Guide

### Option 1: Keep Existing Config (Recommended for Now)

No changes needed. Existing `spring.datasource.hikari.*` configuration takes precedence.

**When to use:**
- Service already has working HikariCP configuration
- No immediate need to change
- Want to maintain explicit control

### Option 2: Add Logging Only

Add traffic-tier property for visibility, no functional change:

```yaml
# Existing configuration (keep this)
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      # ... existing config

# Add this for logging
healthdata:
  database:
    hikari:
      traffic-tier: MEDIUM  # Just enables startup logging
```

**When to use:**
- Want visibility into configuration
- Troubleshooting connection pool issues
- Operations team needs configuration visibility

**What you'll see in logs:**
```
╔════════════════════════════════════════════════════════════════╗
║  HealthData HikariCP Configuration                             ║
╠════════════════════════════════════════════════════════════════╣
║  Traffic Tier:        MEDIUM                                   ║
║  Pool Size:           20                                       ║
║  Min Idle:            5                                        ║
║  Connection Timeout:  20000ms (20 sec)                         ║
║  Idle Timeout:        300000ms (5 min)                         ║
║  Max Lifetime:        1800000ms (30 min)                       ║
║  Keepalive Time:      240000ms (4 min)                         ║
║  Leak Detection:      60000ms (60 sec)                         ║
╚════════════════════════════════════════════════════════════════╝
```

### Option 3: Full Migration (Future)

Remove explicit hikari config, use module:

```diff
- spring:
-   datasource:
-     hikari:
-       maximum-pool-size: 20
-       minimum-idle: 5
-       connection-timeout: 20000
-       idle-timeout: 300000
-       max-lifetime: 1800000
-       keepalive-time: 240000
-       leak-detection-threshold: 60000
-       validation-timeout: 5000
+ healthdata:
+   database:
+     hikari:
+       traffic-tier: MEDIUM
```

**When to use:**
- New service with no existing config
- Service needing configuration updates
- Want to reduce configuration duplication

**Rollback:** Simply restore explicit hikari configuration.

## Advanced Configuration

### Override Tier Defaults

```yaml
healthdata:
  database:
    hikari:
      traffic-tier: MEDIUM           # Use MEDIUM defaults...
      maximum-pool-size: 25          # ...but override pool size
      connection-timeout: 15000      # ...and connection timeout
```

### Disable Module

```yaml
healthdata:
  database:
    enabled: false  # Module won't apply any configuration
```

### Service-Specific Tuning

Some services may need custom tuning beyond tier defaults:

```yaml
healthdata:
  database:
    hikari:
      traffic-tier: HIGH
      maximum-pool-size: 60          # Slightly larger than HIGH default
      max-lifetime: 900000           # Shorter lifetime (15 min)
      leak-detection-threshold: 30000 # More aggressive leak detection
```

## Dependencies

Add to service's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":modules:shared:infrastructure:database-config"))
}
```

The module is already included in `settings.gradle.kts`, so no additional registration needed.

## Testing

Module includes comprehensive tests:

**Unit Tests:**
- Traffic tier logic (pool size calculation)
- Property defaults and overrides
- Effective value calculation

**Integration Tests:**
- Spring Boot auto-configuration
- Conditional bean creation
- Configuration property binding

Run tests:
```bash
./gradlew :modules:shared:infrastructure:database-config:test
```

## Troubleshooting

### Issue: Module configuration not being applied

**Symptom:** Traffic tier configured but defaults not used

**Cause:** Service has explicit `spring.datasource.hikari.*` configuration

**Solution:** This is by design. Explicit configuration takes precedence. Either:
1. Keep explicit configuration (recommended)
2. Remove explicit configuration to use module

### Issue: "Either trafficTier or maximumPoolSize must be configured"

**Symptom:** IllegalStateException on startup

**Cause:** Neither traffic tier nor explicit pool size configured

**Solution:** Add traffic-tier property:
```yaml
healthdata:
  database:
    hikari:
      traffic-tier: MEDIUM
```

### Issue: Configuration logging not showing

**Symptom:** Expected fancy box log not appearing

**Cause:** Module may be disabled or not on classpath

**Solution:** Verify:
1. Module is in dependencies: `./gradlew :your-service:dependencies | grep database-config`
2. Module not disabled: Check for `healthdata.database.enabled=false`
3. Logging level: Ensure INFO level enabled for `com.healthdata.database`

## Examples

### Example 1: New Service (Recommended)

```yaml
# application.yml
healthdata:
  database:
    hikari:
      traffic-tier: MEDIUM

spring:
  application:
    name: my-new-service
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5435/healthdata_mynew
    username: healthdata
    password: ${DB_PASSWORD}
```

### Example 2: Migrated Service

```yaml
# Before migration (explicit config)
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1800000
      keepalive-time: 240000
      leak-detection-threshold: 60000

# After migration (using module)
healthdata:
  database:
    hikari:
      traffic-tier: MEDIUM  # Provides all the same defaults
```

### Example 3: Service with Custom Requirements

```yaml
healthdata:
  database:
    hikari:
      traffic-tier: HIGH            # 50 connections baseline
      maximum-pool-size: 60         # But we need a bit more
      max-lifetime: 900000          # Shorter lifetime for faster recycling
      leak-detection-threshold: 30000  # More aggressive leak detection

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5435/healthdata_custom
    username: healthdata
    password: ${DB_PASSWORD}
```

## Phase 3 Context

This module is the result of Phase 3 HikariCP standardization work (January 2026):

**Analysis:**
- Reviewed 34 microservices
- Identified 3 traffic tiers (HIGH, MEDIUM, LOW)
- Fixed 4 critical connection pool bugs
- Standardized timeout patterns

**Key Findings:**
- 6x safety margin prevents pool exhaustion
- Proactive keepalive prevents connection failures
- 20-second connection timeout prevents queuing
- 5-minute idle timeout matches network timeouts

**Implementation:**
- 34/34 services now have standardized configuration
- 4 critical bugs fixed (notification, agent-builder, demo-seeding, care-gap)
- 100% coverage across all service tiers

## References

- **Phase 3 Completion Report:** `backend/docs/PHASE3_COMPLETION_REPORT.md`
- **Phase 4 Completion Report:** `backend/docs/PHASE4_COMPLETION_REPORT.md`
- **HikariCP Documentation:** https://github.com/brettwooldridge/HikariCP
- **Spring Boot DataSource:** https://docs.spring.io/spring-boot/docs/current/reference/html/data.html

## Support

For questions or issues:
1. Check this README
2. Review Phase 3 Completion Report
3. Check existing service configurations for examples
4. Ask in #hdim-platform Slack channel

---

**Module Version:** 1.0.0
**Phase:** Phase 3 HikariCP Standardization
**Last Updated:** January 2026
**Status:** Production Ready
