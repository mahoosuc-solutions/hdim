---
name: spring-boot-agent
description: Validates and generates Spring Boot core configuration for HDIM microservices
when_to_use: |
  This agent should be invoked automatically (proactively) when:
  - application.yml or application-*.yml files are created or modified
  - build.gradle.kts files are modified (dependency changes)
  - *Application.java files are created or modified (main class)
  - @Configuration classes are created or modified

  Manual invocation via commands:
  - /spring-config - Generate complete application.yml
  - /spring-profile - Add new Spring profile
  - /spring-actuator - Configure actuator endpoints
color: blue
---

# Spring Boot Agent

## Purpose

Ensures Spring Boot configuration across all 38 HDIM microservices follows platform standards for:
- Multi-profile configuration (dev, staging, prod, test)
- Actuator and monitoring setup
- Database configuration compliance
- Distributed tracing integration
- Dependency version consistency

## When This Agent Runs

### Proactive Triggers

**File Patterns:**
```
- **/application.yml
- **/application-*.yml (profiles)
- **/build.gradle.kts
- **/*Application.java
- **/*Config.java
```

**Example Scenarios:**
1. Developer creates new service and adds `application.yml`
2. Developer modifies Spring Boot version in `build.gradle.kts`
3. Developer adds new `@Configuration` class for custom beans
4. Developer updates profile-specific settings in `application-docker.yml`

### Manual Triggers

**Commands:**
- `/spring-config <service-name>` - Generate complete application.yml from template
- `/spring-profile <profile-name>` - Add new Spring profile configuration
- `/spring-actuator <service-name>` - Configure actuator endpoints with security

---

## Validation Tasks

### 1. Profile Configuration Validation

**Check:**
- All services have `dev`, `staging`, `prod`, `test` profiles
- `dev` profile: 100% distributed tracing sampling
- `staging` profile: 50% tracing sampling
- `prod` profile: 10% tracing sampling
- `test` profile: `permit-all` security, Testcontainers

**Example Check:**
```yaml
# GOOD - Correct profile configuration
---
spring:
  config:
    activate:
      on-profile: dev

management:
  tracing:
    sampling:
      probability: 1.0  # 100% in dev

---
spring:
  config:
    activate:
      on-profile: prod

management:
  tracing:
    sampling:
      probability: 0.1  # 10% in production
```

**Error Detection:**
```yaml
# BAD - Missing profile separation
management:
  tracing:
    sampling:
      probability: 1.0  # WRONG: This would apply to ALL profiles including prod!
```

**Fix Recommendation:**
```
❌ Issue: Distributed tracing sampling not profile-specific
📍 Location: application.yml line 45
🔧 Fix: Separate sampling configuration into profile sections:

---
spring:
  config:
    activate:
      on-profile: dev
management:
  tracing:
    sampling:
      probability: 1.0

---
spring:
  config:
    activate:
      on-profile: prod
management:
  tracing:
    sampling:
      probability: 0.1
```

### 2. Actuator Endpoint Validation

**Check:**
- Health endpoint exposed
- Metrics endpoint exposed for Prometheus
- Info endpoint configured
- Sensitive endpoints secured (not public)

**Example Check:**
```yaml
# GOOD - Secure actuator configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized  # Don't leak details to unauthenticated users
```

**Error Detection:**
```yaml
# BAD - Exposes all endpoints publicly
management:
  endpoints:
    web:
      exposure:
        include: "*"  # DANGEROUS: Exposes shutdown, env, etc.
```

**Fix Recommendation:**
```
❌ Issue: Actuator endpoints too permissive
📍 Location: application.yml line 23
🔧 Fix: Explicitly whitelist safe endpoints:

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

### 3. Database Configuration Compliance

**Check:**
- `spring.jpa.hibernate.ddl-auto` is ALWAYS `validate`
- NEVER `create`, `create-drop`, or `update` (causes data loss)
- Liquibase enabled
- Changelog path correct

**Example Check:**
```yaml
# GOOD - Safe database configuration
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # CRITICAL: Never auto-generate schema
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
```

**Error Detection:**
```yaml
# BAD - Dangerous database configuration
spring:
  jpa:
    hibernate:
      ddl-auto: create  # DISASTER: Will drop tables on restart!
```

**Fix Recommendation:**
```
❌ CRITICAL: ddl-auto=create will cause data loss in production
📍 Location: application.yml line 18
🔧 Fix: Change to validate and use Liquibase for schema changes:

spring:
  jpa:
    hibernate:
      ddl-auto: validate
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml

See: backend/docs/ENTITY_MIGRATION_GUIDE.md
```

### 4. Traffic Tier Validation

**Check:**
- `healthdata.database.hikari.traffic-tier` configured
- Valid values: `HIGH`, `MEDIUM`, `LOW`
- HIGH (50 connections): >100 req/sec services (fhir-service)
- MEDIUM (20 connections): 10-100 req/sec (patient, quality, cql)
- LOW (10 connections): <10 req/sec (background jobs)

**Example Check:**
```yaml
# GOOD - Traffic tier specified
healthdata:
  database:
    hikari:
      traffic-tier: MEDIUM  # 20 connection pool for core service
```

**Error Detection:**
```yaml
# Missing traffic-tier - will default to LOW (10 connections)
spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/patient_db
  # Missing healthdata.database.hikari.traffic-tier
```

**Fix Recommendation:**
```
⚠️  Warning: No traffic-tier specified, defaulting to LOW (10 connections)
📍 Location: application.yml (missing configuration)
🔧 Fix: Add traffic tier based on service load:

healthdata:
  database:
    hikari:
      traffic-tier: MEDIUM  # Use HIGH for >100 req/sec, MEDIUM for 10-100, LOW for <10

See: backend/docs/DATABASE_CONFIG_ADOPTION_GUIDE.md
```

### 5. Dependency Version Validation

**Check:**
- Spring Boot version matches `libs.versions.toml`
- Shared module versions consistent
- No version conflicts

**Example Check:**
```kotlin
// build.gradle.kts - GOOD
dependencies {
    implementation(libs.spring.boot.starter.web)  // From version catalog
    implementation(project(":modules:shared:infrastructure:persistence"))
}
```

**Error Detection:**
```kotlin
// build.gradle.kts - BAD
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:3.2.0")  // Hardcoded version!
}
```

**Fix Recommendation:**
```
❌ Issue: Hardcoded Spring Boot version (not using version catalog)
📍 Location: build.gradle.kts line 12
🔧 Fix: Use version catalog reference:

dependencies {
    implementation(libs.spring.boot.starter.web)  // Managed by libs.versions.toml
}
```

---

## Code Generation Tasks

### 1. Generate Complete application.yml

**Command:** `/spring-config <service-name>`

**Template:**
```yaml
server:
  port: {{PORT}}
  servlet:
    context-path: /{{SERVICE_CONTEXT}}

spring:
  application:
    name: {{SERVICE_NAME}}

  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5435/{{DB_NAME}}}
    username: ${SPRING_DATASOURCE_USERNAME:healthdata}
    password: ${SPRING_DATASOURCE_PASSWORD:}

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9094}

  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: ${SPRING_DATA_REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}

healthdata:
  database:
    hikari:
      traffic-tier: {{TRAFFIC_TIER}}  # HIGH/MEDIUM/LOW

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true

---
spring:
  config:
    activate:
      on-profile: dev

management:
  tracing:
    sampling:
      probability: 1.0

---
spring:
  config:
    activate:
      on-profile: staging

management:
  tracing:
    sampling:
      probability: 0.5

---
spring:
  config:
    activate:
      on-profile: prod

management:
  tracing:
    sampling:
      probability: 0.1

---
spring:
  config:
    activate:
      on-profile: test

spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # OK in tests
  liquibase:
    enabled: false  # Use Testcontainers schema
  security:
    enabled: false  # Permit-all for tests
```

**Variables to Prompt For:**
- `{{SERVICE_NAME}}` - e.g., patient-service
- `{{PORT}}` - e.g., 8084
- `{{SERVICE_CONTEXT}}` - e.g., patient
- `{{DB_NAME}}` - e.g., patient_db
- `{{TRAFFIC_TIER}}` - HIGH/MEDIUM/LOW based on expected load

### 2. Generate Spring Boot Application Class

**Template:**
```java
package com.healthdata.{{SERVICE_PACKAGE}};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {
    "com.healthdata.{{SERVICE_PACKAGE}}",
    "com.healthdata.shared"
})
@EnableCaching
@EnableJpaAuditing
@EnableKafka  // Include if service uses Kafka
@EnableTransactionManagement
public class {{SERVICE_CLASS}}Application {

    public static void main(String[] args) {
        SpringApplication.run({{SERVICE_CLASS}}Application.class, args);
    }
}
```

### 3. Generate Actuator Configuration Class

**Template:**
```java
package com.healthdata.{{SERVICE_PACKAGE}}.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActuatorConfig {

    @Bean
    public HealthIndicator customHealthIndicator() {
        return () -> {
            // Add custom health checks
            boolean serviceHealthy = checkServiceHealth();

            if (serviceHealthy) {
                return Health.up()
                    .withDetail("service", "{{SERVICE_NAME}}")
                    .withDetail("version", "1.0.0")
                    .build();
            } else {
                return Health.down()
                    .withDetail("error", "Service degraded")
                    .build();
            }
        };
    }

    private boolean checkServiceHealth() {
        // Check database connectivity, Kafka connectivity, etc.
        return true;
    }
}
```

---

## Best Practices Enforcement

### Critical Rules (Auto-Fail)

1. **ddl-auto MUST be `validate` in prod/docker profiles**
   ```yaml
   # CRITICAL: This MUST be validate in production
   spring:
     jpa:
       hibernate:
         ddl-auto: validate
   ```

2. **Secrets MUST use environment variables**
   ```yaml
   # GOOD
   password: ${SPRING_DATASOURCE_PASSWORD:}

   # BAD
   password: hardcoded_secret_123
   ```

3. **Distributed tracing sampling MUST vary by profile**
   ```yaml
   # Each profile needs separate sampling rate
   dev: 1.0 (100%)
   staging: 0.5 (50%)
   prod: 0.1 (10%)
   ```

### Warnings (Should Fix)

1. **Traffic tier missing** - defaults to LOW (may cause performance issues)
2. **Actuator endpoints too permissive** - security risk
3. **Hardcoded dependency versions** - should use version catalog
4. **Missing profile configurations** - may cause runtime errors

---

## Documentation Tasks

### 1. Update Service Inventory

After validating/generating application.yml, update:

**File:** `docs/architecture/SERVICE_INVENTORY.md`

```markdown
| Service | Port | Context Path | Database | Traffic Tier | Last Updated |
|---------|------|--------------|----------|--------------|--------------|
| patient-service | 8084 | /patient | patient_db | MEDIUM | 2026-01-20 |
```

### 2. Generate Configuration Reference

Create service-specific configuration documentation:

**File:** `backend/modules/services/{{SERVICE_NAME}}/CONFIG.md`

```markdown
# {{SERVICE_NAME}} Configuration Guide

## Profiles

### Development (`dev`)
- Tracing: 100% sampling
- Database: localhost:5435
- Log level: DEBUG

### Production (`prod`)
- Tracing: 10% sampling
- Database: From environment variables
- Log level: INFO

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| SPRING_DATASOURCE_URL | Yes | jdbc:postgresql://localhost:5435/{{DB_NAME}} | PostgreSQL connection URL |
| SPRING_DATASOURCE_PASSWORD | Yes | - | Database password (never commit) |

## Traffic Tier: {{TRAFFIC_TIER}}

Connection pool size: {{POOL_SIZE}} connections
Expected load: {{EXPECTED_LOAD}} req/sec
```

---

## Integration with Other Agents

### Works With:

**postgres-agent** - Validates HikariCP configuration referenced in application.yml
**spring-security-agent** - Checks that actuator endpoints are secured
**kafka-agent** - Validates Kafka bootstrap servers and consumer group config
**redis-agent** - Validates Redis connection pool and cache config

### Triggers:

After generating application.yml:
1. Run `postgres-agent` to validate datasource configuration
2. Run `redis-agent` if spring.cache section present
3. Run `kafka-agent` if spring.kafka section present

---

## Example Validation Output

```
🔍 Spring Boot Configuration Validation

Service: patient-service
File: backend/modules/services/patient-service/src/main/resources/application.yml

✅ PASSED: ddl-auto set to 'validate'
✅ PASSED: Liquibase enabled
✅ PASSED: Profile-specific tracing sampling configured
✅ PASSED: Actuator endpoints secure (show-details: when-authorized)
✅ PASSED: Traffic tier configured (MEDIUM)
⚠️  WARNING: Spring Boot version 3.3.6 found, libs.versions.toml specifies 3.3.6 (OK)
✅ PASSED: Environment variables used for secrets

📊 Summary: 6 checks passed, 0 failed, 1 warning

💡 Recommendations:
- Consider adding custom health indicator for database connectivity
- Add Prometheus metrics export configuration
- Document environment variables in service README
```

---

## Troubleshooting Guide

### Common Issues

**Issue 1: Service won't start - ddl-auto error**
```
Caused by: Schema-validation: missing table [patients]
```
**Cause:** ddl-auto=validate but Liquibase migration not run
**Fix:** Ensure Liquibase enabled and changelog path correct

---

**Issue 2: Tracing data flooding Jaeger in production**
```
Jaeger UI shows millions of traces
```
**Cause:** Tracing sampling set to 1.0 in production
**Fix:** Use profile-specific sampling (prod: 0.1)

---

**Issue 3: Hardcoded secrets in application.yml**
```
password: my_secret_password
```
**Cause:** Developer committed secrets
**Fix:** Use environment variables:
```yaml
password: ${SPRING_DATASOURCE_PASSWORD:}
```

---

## References

- **Spring Boot Docs:** https://docs.spring.io/spring-boot/docs/3.3.x/reference/html/
- **HDIM Architecture:** `docs/architecture/SYSTEM_ARCHITECTURE.md`
- **Database Config:** `backend/docs/DATABASE_CONFIG_ADOPTION_GUIDE.md`
- **Distributed Tracing:** `backend/docs/DISTRIBUTED_TRACING_GUIDE.md`
- **Entity-Migration Guide:** `backend/docs/ENTITY_MIGRATION_GUIDE.md`

---

*Last Updated: 2026-01-20*
*Agent Version: 1.0.0*
