---
name: service-startup-doctor
description: Diagnoses and fixes cascading startup failures in HDIM microservices caused by shared module integration issues (duplicate YAML keys, bean conflicts, Liquibase misconfiguration, entity scan overreach, missing optional beans)
---

# Service Startup Doctor

## When This Skill Activates

- User reports a service crash loop, startup failure, or "application failed to start"
- Error mentions: `DuplicateKeyException`, `missing table`, `bean not found`, `BeanCreationException`
- User asks to debug a service that won't start or keeps restarting
- Docker logs show repeated restarts with Spring Boot startup errors

**Trigger phrases:** "service won't start", "crash loop", "startup failure", "application failed to start", "bean not found", "missing table", "schema-validation", "duplicate key"

## Background

HDIM shared modules (`authentication`, `audit`, `persistence`) use `@AutoConfiguration` with broad `@EntityScan` and `@ComponentScan`. When a service has its own database that doesn't contain tables expected by those entities, failures cascade through 5 layers — each masked by the previous one. This skill encodes the proven 8-step fix sequence discovered in notification-service and prior-auth-service.

## Diagnostic Checklist

Run these steps IN ORDER. Each step may unmask the next failure.

### Step 1: YAML Structure Audit

Check `application.yml` for duplicate top-level keys. YAML silently drops the first block when keys are duplicated.

```bash
# Find the service's application.yml
SERVICE_NAME="<service-name>"
YAML_PATH="backend/modules/services/${SERVICE_NAME}/src/main/resources/application.yml"

# Check for duplicate top-level keys
grep -n "^[a-z]" "$YAML_PATH" | sort -t: -k2 | uniq -f1 -d
```

**Common duplicates:** `spring:`, `management:`, `feign:`, `server:`

**Fix:** Merge duplicate blocks into one. The second occurrence silently overwrites the first.

### Step 2: Bean Conflict Scan

Check for duplicate `@Bean` definitions, especially `objectMapper`.

```bash
# Find all @Bean methods in the service
grep -rn "@Bean" backend/modules/services/${SERVICE_NAME}/src/main/java/ | grep -v test
```

**Common conflict:** Service defines its own `objectMapper` bean, but the shared `authentication` module's `JacksonConfiguration` already provides one with `JavaTimeModule`.

**Fix:** Remove the redundant `@Bean` method from the service config.

### Step 3: Liquibase Configuration Check

Verify Liquibase is enabled and correctly configured.

```bash
# Check Docker env
grep -A5 "${SERVICE_NAME}" docker-compose.yml | grep -i liquibase

# Check application.yml
grep -A5 "liquibase" "$YAML_PATH"
```

**Check these:**
- `SPRING_LIQUIBASE_ENABLED` is `"true"` in docker-compose.yml (not `"false"`)
- Changelog master file has a unique service-specific name (not generic `db.changelog-master.xml`)
- `liquibase-schema: public` is set when using a custom `default-schema`
- `sqlFile` paths use relative paths from the changelog XML (not from classpath root)

**Fix for wrong sqlFile paths:**
```xml
<!-- When relativeToChangelogFile="true", path is relative to the XML file -->
<!-- Good: -->  <sqlFile path="sql/0001-create-schema.sql" relativeToChangelogFile="true"/>
<!-- Bad:  -->  <sqlFile path="db/changelog/sql/0001-create-schema.sql" relativeToChangelogFile="true"/>
```

### Step 4: Entity Scan Audit

Verify `@EntityScan` only includes packages with tables in THIS service's database.

```bash
# Check entity scan configuration
grep -n "EntityScan\|EnableJpaRepositories" backend/modules/services/${SERVICE_NAME}/src/main/java/**/*Application.java
```

**Red flags:**
- `com.healthdata.authentication.domain` — pulls in `User`, `Tenant`, `ApiKey` entities
- `com.healthdata.authentication.entity` — same problem
- `com.healthdata.audit.entity` — pulls in 10+ audit entities

**Fix:** Narrow to service-specific packages only:
```java
@EntityScan("com.healthdata.yourservice.persistence")
@EnableJpaRepositories("com.healthdata.yourservice.persistence")
```

### Step 5: Auto-Configuration Exclusion

Check if auth/audit auto-configs force-scan unrelated entities.

```bash
# Check current exclusions
grep -A5 "SpringBootApplication" backend/modules/services/${SERVICE_NAME}/src/main/java/**/*Application.java
```

**Fix:** If the service's DB doesn't have `users`/`tenants`/`api_keys` tables:
```java
@SpringBootApplication(exclude = {
    AuthenticationAutoConfiguration.class,
    AuthenticationControllerAutoConfiguration.class
})
```

### Step 6: Optional Dependency Check

Verify components from optional modules are properly guarded.

```bash
# Find required injections of optional beans
grep -rn "@Autowired" backend/modules/services/${SERVICE_NAME}/src/main/java/ | grep -v test | grep -v "required = false"

# Find conditional annotations
grep -rn "ConditionalOnBean\|ConditionalOnProperty" backend/modules/services/${SERVICE_NAME}/src/main/java/
```

**Common missing guards:**
- `AIAuditEventPublisher` — needs `@ConditionalOnBean` on the integration class
- `PriorAuthAuditIntegration` / similar — needs `@Autowired(required = false)` in consuming service
- Messaging/SMS/email services — need `@ConditionalOnProperty` guards

**Fix pattern:**
```java
// On the integration class itself
@ConditionalOnBean(AIAuditEventPublisher.class)
public class ServiceAuditIntegration { ... }

// In the consuming service
@Autowired(required = false)
private ServiceAuditIntegration auditIntegration;

// At call sites
if (auditIntegration != null) {
    auditIntegration.publishEvent(event);
}
```

### Step 7: Resilience4j Configuration

Check for conflicting retry configuration.

```bash
grep -n "waitDuration\|intervalFunction" backend/modules/services/${SERVICE_NAME}/src/main/java/**/*.java
```

**Rule:** Never combine `.waitDuration()` and `.intervalFunction()` in the same `RetryConfig.Builder`. Use only `intervalFunction` for exponential backoff.

### Step 8: Docker Environment Override

Verify docker-compose.yml environment variables match intended config.

```bash
# Show all env vars for the service
grep -A30 "^  ${SERVICE_NAME}:" docker-compose.yml | grep -E "^\s+-?\s*[A-Z_]+:"
```

**Common issues:**
- `audit.kafka.consumer.enabled` not disabled when Kafka audit is unused
- `SPRING_DATASOURCE_URL` pointing to wrong database
- Missing `SPRING_LIQUIBASE_CHANGE_LOG` override

## After Fixing

1. Rebuild the service: `docker compose build ${SERVICE_NAME}`
2. Restart: `docker compose up -d ${SERVICE_NAME}`
3. Check logs: `docker compose logs -f ${SERVICE_NAME} | head -100`
4. Verify health: `curl http://localhost:PORT/actuator/health`
5. Run entity validation: `./gradlew :modules:services:${SERVICE_NAME}:test --tests "*EntityMigrationValidationTest"`

## Reference

- **ADR-011**: [Shared Module Integration](docs/architecture/decisions/ADR-011-shared-module-integration.md)
- **Proven on**: notification-service (`f212b4327`, `e8a2cc88b`, `46ea65b9b`), prior-auth-service (`8c6918ea4`)
- **Troubleshooting**: [Cascading Startup Failures](docs/troubleshooting/README.md#9-cascading-startup-failures-shared-module-pattern)
