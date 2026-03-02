# ADR-011: Shared Module Integration for Microservices

**Status**: Accepted
**Date**: 2026-03-02
**Decision Makers**: Platform Team
**Stakeholders**: All Backend Services

---

## Context

### Problem Statement

HDIM uses shared modules (`authentication`, `audit`, `persistence`) to provide cross-cutting concerns. These modules use `@AutoConfiguration` with `@EntityScan` and `@ComponentScan` that force-register entities and beans in every consuming service. When a service has its own database that doesn't contain the tables those entities expect (e.g., `tenants`, `users`, `api_key_allowed_ips`, audit tables), Hibernate validation fails at startup.

This creates **cascading startup failures** — a chain of 5 distinct errors that only surface one at a time as you fix each layer:

1. **Duplicate YAML keys** — SnakeYAML strict mode throws `DuplicateKeyException` before Spring context loads
2. **Bean conflicts** — Duplicate `@Bean` definitions (e.g., `objectMapper`) from shared and service configs
3. **Liquibase misconfiguration** — Disabled in Docker, wrong `sqlFile` paths, missing `liquibase-schema: public`
4. **Entity scan overreach** — `@EntityScan` pulling in auth/audit entities whose tables don't exist in this DB
5. **Missing optional beans** — Components requiring beans from disabled modules (e.g., `AIAuditEventPublisher`)

This pattern was discovered fixing `notification-service` (commits `f212b4327`, `e8a2cc88b`, `46ea65b9b`) and identically reproduced in `prior-auth-service` (commit `8c6918ea4`). It will recur as new services onboard shared modules.

---

## Options Considered

### Option 1: Implicit Opt-In (Current Broken State)

**Description**: Shared modules auto-configure everything via classpath scanning; services inherit all entities and beans.

**Pros**:
- Zero configuration for new services
- Consistent behavior across services

**Cons**:
- Forces entity registration for tables that don't exist in the service's database
- Hibernate `validate` mode fails on missing tables
- Requires every service to have every shared table — defeats microservice independence
- Cascading failures are hard to diagnose (5 layers deep)

**Risk Level**: High

---

### Option 2: Explicit Opt-In with Guards (Chosen)

**Description**: Services must explicitly opt in to shared module features. Shared modules guard optional components with `@ConditionalOnBean`/`@ConditionalOnProperty`. Services narrow their entity/repository scans.

**Pros**:
- Services only load what they need
- No phantom entity validation failures
- Clear dependency boundaries
- Follows microservice isolation principles

**Cons**:
- More boilerplate in service application classes
- Developers must understand which auto-configurations to exclude

**Risk Level**: Low

---

## Decision

**We chose Option 2 (Explicit Opt-In with Guards)** because:

1. **Microservice Independence**: Each service owns its database schema; it must not validate entities from other services
2. **Diagnosability**: Explicit configuration makes failures obvious instead of cascading 5 layers deep
3. **Onboarding Safety**: New services get a checklist instead of mysterious startup crashes
4. **Production Stability**: Prevents runtime failures from schema drift in unrelated modules

---

## Consequences

### Positive

- Services start reliably with only their own entities
- New service onboarding follows a repeatable checklist
- Optional integrations (audit, messaging) degrade gracefully
- Reduced coupling between services and shared modules

### Negative

- More explicit configuration per service (~15-20 lines of annotations)
- Developers must understand auto-configuration exclusions
- Existing services may need retrofitting (see Implementation Checklist)

---

## Implementation

### 8-Step Service Onboarding Checklist

When a service uses shared modules (`authentication`, `audit`, `persistence`), apply these steps:

#### Step 1: YAML Structure Audit

Check `application.yml` for duplicate top-level keys (`spring:`, `management:`, `feign:`, etc.). YAML silently drops the first block when keys are duplicated; merge into one.

```bash
# Find duplicate top-level keys
grep -n "^[a-z]" src/main/resources/application.yml | sort -t: -k2 | uniq -f1 -d
```

#### Step 2: Auth Auto-Configuration Exclusion

If the service's database does NOT have `users`/`tenants`/`api_keys` tables, exclude authentication auto-configurations:

```java
@SpringBootApplication(exclude = {
    AuthenticationAutoConfiguration.class,
    AuthenticationControllerAutoConfiguration.class
})
```

Narrow `scanBasePackages` to only the auth sub-packages actually needed (filters, security). Do NOT include root `com.healthdata.authentication`.

#### Step 3: Narrow Entity and Repository Scans

Restrict `@EntityScan` and `@EnableJpaRepositories` to service-specific packages only:

```java
@EntityScan("com.healthdata.yourservice.persistence")
@EnableJpaRepositories("com.healthdata.yourservice.persistence")
```

Never include `com.healthdata.authentication.domain` or `com.healthdata.authentication.entity` unless the service's DB has those tables.

#### Step 4: Unique Liquibase Changelog Filename

Every service must have a service-named master changelog to avoid classpath collision:

```
# Good
yourservice-changelog-master.xml

# Bad (collides with shared JARs)
db.changelog-master.xml
```

#### Step 5: Liquibase Schema Configuration

When using a custom `default-schema` (e.g., `prior_auth`), add `liquibase-schema: public` so Liquibase tracking tables (`databasechangelog`, `databasechangeloglock`) are created in `public` before the first migration creates the custom schema:

```yaml
spring:
  liquibase:
    enabled: true
    liquibase-schema: public
```

#### Step 6: Correct Liquibase SQL File Paths

When using `relativeToChangelogFile="true"`, paths must be relative to the changelog XML, not the classpath root:

```xml
<!-- Good (relative to changelog XML) -->
<sqlFile path="sql/0001-create-schema.sql" relativeToChangelogFile="true"/>

<!-- Bad (includes the parent directory that's already the context) -->
<sqlFile path="db/changelog/sql/0001-create-schema.sql" relativeToChangelogFile="true"/>
```

#### Step 7: Guard Optional Dependencies

Any component requiring beans from optional modules must be guarded:

```java
// Guard the entire class
@ConditionalOnBean(AIAuditEventPublisher.class)
public class MyAuditIntegration { ... }

// Or make injection optional
@Autowired(required = false)
private MyAuditIntegration auditIntegration;

// Then null-check at call sites
if (auditIntegration != null) {
    auditIntegration.publishEvent(event);
}
```

#### Step 8: Resilience4j Retry Configuration

Never combine `.waitDuration()` and `.intervalFunction()` in the same `RetryConfig.Builder`. Use only `intervalFunction` for exponential backoff:

```java
// Good
RetryConfig.custom()
    .intervalFunction(IntervalFunction.ofExponentialBackoff(1000, 2))
    .build();

// Bad — throws IllegalArgumentException
RetryConfig.custom()
    .waitDuration(Duration.ofMillis(1000))
    .intervalFunction(IntervalFunction.ofExponentialBackoff(1000, 2))
    .build();
```

### Docker Compose Environment

Verify `docker-compose.yml` environment variables match intended config. Common issues:

- `SPRING_LIQUIBASE_ENABLED: "false"` when it should be `"true"`
- Missing `SPRING_LIQUIBASE_CHANGE_LOG` override
- `audit.kafka.consumer.enabled` not set to `false` when Kafka audit is unused

---

## Affected Services

All 51+ services using shared modules are potentially affected. Priority services:

| Service | Status | Commit |
|---------|--------|--------|
| notification-service | Fixed | `f212b4327`, `e8a2cc88b`, `46ea65b9b` |
| prior-auth-service | Fixed | `8c6918ea4` |
| All others | Apply checklist when issues arise | — |

---

## Monitoring & Validation

### Pre-Build Validation

```bash
# Run entity-migration validation
./gradlew :modules:services:YOUR-SERVICE:test --tests "*EntityMigrationValidationTest"

# Run comprehensive pre-Docker validation
./scripts/validate-before-docker-build.sh
```

### Startup Health Check

```bash
docker compose up -d YOUR-SERVICE
docker compose logs -f YOUR-SERVICE | head -100
curl http://localhost:PORT/actuator/health
```

---

## References

- **[ADR-005: Liquibase Migrations](./ADR-005-liquibase-migrations.md)** — Foundation migration strategy
- **[Troubleshooting Guide: Cascading Startup Failures](../../troubleshooting/README.md#9-cascading-startup-failures-shared-module-pattern)** — Decision tree
- **[Entity-Migration Guide](../../../backend/docs/ENTITY_MIGRATION_GUIDE.md)** — Entity validation procedures
- **Notification-service fix commits**: `f212b4327`, `e8a2cc88b`, `46ea65b9b`
- **Prior-auth-service fix commit**: `8c6918ea4`

---

## Footer

**ADR #**: 011
**Version**: 1.0
**Status**: Active and Validated
**Pattern**: Explicit Opt-In for Shared Module Integration

_Decision Date: March 2026_
_Validated on: notification-service, prior-auth-service_
