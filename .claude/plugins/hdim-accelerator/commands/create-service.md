---
description: Generate complete HDIM microservice with security, persistence, and Docker configuration
arguments:
  service_name:
    description: Service name in kebab-case (e.g., appointment-service, medication-service)
    type: string
    required: true
  port:
    description: Service port number (8000-9000 range)
    type: number
    required: true
  description:
    description: Brief description of service purpose
    type: string
    required: false
    default: "HDIM microservice"
---

# Create Service Command

Generate a complete HDIM microservice following platform patterns for security, persistence, and multi-tenant isolation.

## What This Command Does

1. **Creates Service Structure** - Full package hierarchy with proper naming
2. **Generates SecurityConfig** - Gateway trust authentication pre-configured
3. **Sets up Persistence** - PostgreSQL + Liquibase configuration
4. **Adds Docker Support** - Dockerfile + docker-compose.yml entry
5. **Creates build.gradle.kts** - Uses version catalog, includes shared modules
6. **Generates application.yml** - Multi-profile configuration (dev/staging/prod)
7. **Creates README** - Service documentation template

## Usage

```bash
/create-service {{service_name}} {{port}} ["{{description}}"]
```

## Examples

```bash
# Create appointment scheduling service
/create-service appointment-service 8090 "Manages patient appointment scheduling"

# Create medication tracking service
/create-service medication-service 8091 "Tracks patient medications and prescriptions"

# Create billing service
/create-service billing-service 8092 "Handles medical billing and claims"
```

## Pre-requisites

- Port must be available (not used by existing services)
- Service name must be unique (check `backend/modules/services/`)
- Service name must follow kebab-case convention

## Implementation

You are tasked with creating a complete HDIM microservice following platform standards.

### Step 1: Validate Inputs

**Check service doesn't already exist:**
```bash
if [ -d "backend/modules/services/{{service_name}}" ]; then
    echo "Error: Service {{service_name}} already exists"
    exit 1
fi
```

**Check port isn't in use:**
```bash
grep -r "{{port}}" backend/modules/services/*/src/main/resources/application.yml
# Should return no results
```

**Validate naming:**
- Must be kebab-case (e.g., `appointment-service`)
- Must end with `-service`
- Must not contain underscores or spaces

### Step 2: Create Directory Structure

```bash
mkdir -p backend/modules/services/{{service_name}}/src/main/{java,resources}
mkdir -p backend/modules/services/{{service_name}}/src/test/java

# Java package structure
SERVICE_PACKAGE="com/healthdata/{{SERVICE_NAME_CAMEL}}"
mkdir -p backend/modules/services/{{service_name}}/src/main/java/$SERVICE_PACKAGE/{api/v1,config,domain/{model,repository},service,dto}
mkdir -p backend/modules/services/{{service_name}}/src/test/java/$SERVICE_PACKAGE/{api/v1,service}

# Resources
mkdir -p backend/modules/services/{{service_name}}/src/main/resources/db/changelog
mkdir -p backend/modules/services/{{service_name}}/src/test/resources
```

**Package naming:**
- Extract base name: `appointment-service` → `appointment`
- Convert to camelCase: `appointment` → `appointment`
- Package: `com.healthdata.appointment`

### Step 3: Generate build.gradle.kts

**Template:**
```kotlin
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

group = "com.healthdata"
version = "1.0.0-SNAPSHOT"

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Shared Modules (HDIM Platform)
    implementation(project(":modules:shared:domain"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:infrastructure:monitoring"))
    implementation(project(":modules:shared:infrastructure:caching"))

    // Database
    implementation("org.liquibase:liquibase-core")
    runtimeOnly("org.postgresql:postgresql")

    // Utilities
    implementation("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
```

### Step 4: Generate Application Configuration

**application.yml:**
```yaml
spring:
  application:
    name: {{service_name}}

  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5435}/{{service_db}}
    username: ${POSTGRES_USER:healthdata}
    password: ${POSTGRES_PASSWORD:healthdata123}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate  # CRITICAL: Never use create/update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml

  cache:
    type: redis
    redis:
      time-to-live: 300000  # 5 minutes (HIPAA compliance for PHI)

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6380}
      password: ${REDIS_PASSWORD:}

server:
  port: {{port}}
  servlet:
    context-path: /{{service_name_base}}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    root: INFO
    com.healthdata: DEBUG
    org.springframework.security: DEBUG

---
# Development Profile
spring:
  config:
    activate:
      on-profile: dev

  jpa:
    show-sql: true

logging:
  level:
    org.hibernate.SQL: DEBUG

---
# Production Profile
spring:
  config:
    activate:
      on-profile: prod

  jpa:
    show-sql: false

logging:
  level:
    root: WARN
    com.healthdata: INFO
```

### Step 5: Generate SecurityConfig

**SecurityConfig.java:**
```java
package com.healthdata.{{SERVICE_NAME_CAMEL}}.config;

import com.healthdata.shared.authentication.filter.TrustedHeaderAuthFilter;
import com.healthdata.shared.authentication.filter.TrustedTenantAccessFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for {{service_name}}.
 *
 * Uses gateway trust authentication pattern:
 * - Gateway validates JWT and injects trusted X-Auth-* headers
 * - Service trusts headers from gateway (no JWT validation)
 * - TrustedHeaderAuthFilter validates headers and sets SecurityContext
 * - TrustedTenantAccessFilter ensures tenant isolation
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final TrustedHeaderAuthFilter trustedHeaderAuthFilter;
    private final TrustedTenantAccessFilter trustedTenantAccessFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            // Gateway trust filter chain
            .addFilterBefore(trustedHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(trustedTenantAccessFilter, TrustedHeaderAuthFilter.class);

        return http.build();
    }
}
```

### Step 6: Generate Main Application Class

**{{ServiceName}}ServiceApplication.java:**
```java
package com.healthdata.{{SERVICE_NAME_CAMEL}};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * {{service_name}} - {{description}}
 *
 * Port: {{port}}
 * Context Path: /{{service_name_base}}
 * Database: {{service_db}}
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.{{SERVICE_NAME_CAMEL}}",
    "com.healthdata.shared"  // Scan shared modules
})
@EnableJpaRepositories
@EnableCaching
public class {{ServiceNamePascal}}ServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run({{ServiceNamePascal}}ServiceApplication.class, args);
    }
}
```

### Step 7: Generate Liquibase Master Changelog

**db.changelog-master.xml:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <!-- Enable PostgreSQL extensions -->
    <include file="db/changelog/0000-enable-extensions.xml"/>

    <!-- Add table migrations here using /add-entity or /add-migration -->

</databaseChangeLog>
```

**0000-enable-extensions.xml:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="0000-enable-extensions" author="hdim-platform-team">
        <comment>Enable PostgreSQL extensions for {{service_name}}</comment>

        <sql>CREATE EXTENSION IF NOT EXISTS pg_trgm;</sql>

        <rollback>
            <sql>DROP EXTENSION IF EXISTS pg_trgm;</sql>
        </rollback>
    </changeSet>

</databaseChangeLog>
```

### Step 8: Generate Dockerfile

**Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy JAR
COPY build/libs/{{service_name}}-*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:{{port}}/{{service_name_base}}/actuator/health || exit 1

# Run
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Step 9: Add to docker-compose.yml

Add entry to `docker-compose.yml`:

```yaml
  {{service_name}}:
    build:
      context: ./backend/modules/services/{{service_name}}
      dockerfile: Dockerfile
    container_name: hdim-{{service_name}}
    ports:
      - "{{port}}:{{port}}"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      POSTGRES_HOST: postgres
      POSTGRES_PORT: 5432
      POSTGRES_DB: {{service_db}}
      POSTGRES_USER: healthdata
      POSTGRES_PASSWORD: healthdata123
      REDIS_HOST: redis
      REDIS_PORT: 6379
      GATEWAY_AUTH_DEV_MODE: "true"
    depends_on:
      - postgres
      - redis
    networks:
      - hdim-network
    restart: unless-stopped
```

### Step 10: Generate README

**README.md:**
```markdown
# {{ServiceNamePascal}} Service

{{description}}

## Overview

- **Port:** {{port}}
- **Context Path:** `/{{service_name_base}}`
- **Database:** `{{service_db}}`

## Quick Start

### Build
```bash
cd backend
./gradlew :modules:services:{{service_name}}:build
```

### Run Locally
```bash
docker compose up {{service_name}}
```

### Run Tests
```bash
./gradlew :modules:services:{{service_name}}:test
```

## API Endpoints

- `GET /{{service_name_base}}/actuator/health` - Health check
- `GET /{{service_name_base}}/actuator/info` - Service info

## Security

Uses gateway trust authentication:
- Gateway validates JWT
- Service trusts X-Auth-* headers from gateway
- All endpoints require authentication (except actuator health/info)

## Multi-Tenant

All data is tenant-isolated. Every table MUST include `tenant_id` column.

## Development

### Add Entity
```bash
/add-entity {{service_name}} EntityName "Description"
```

### Add Migration
```bash
/add-migration {{service_name}} "description"
```

### Validate Schema
```bash
/validate-schema {{service_name}}
```

## Database

- **Type:** PostgreSQL 16
- **Schema Management:** Liquibase
- **Migrations:** `src/main/resources/db/changelog/`

## Technology Stack

- **Framework:** Spring Boot 3.x
- **Language:** Java 21
- **Database:** PostgreSQL 16 + Liquibase
- **Cache:** Redis 7
- **Security:** Gateway Trust Authentication
- **Testing:** JUnit 5 + Testcontainers

## Documentation

- Architecture: `docs/architecture/SYSTEM_ARCHITECTURE.md`
- Security: `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`
- Database: `backend/docs/ENTITY_MIGRATION_GUIDE.md`
```

### Step 11: Generate Test Class

**{{ServiceName}}ServiceApplicationTests.java:**
```java
package com.healthdata.{{SERVICE_NAME_CAMEL}};

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class {{ServiceNamePascal}}ServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring context loads successfully
    }
}
```

### Step 12: Add Database to init-multi-db.sh

Add database creation to `docker/postgres/init-multi-db.sh`:

```bash
# Create {{service_name}} database
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE {{service_db}};
    GRANT ALL PRIVILEGES ON DATABASE {{service_db}} TO $POSTGRES_USER;
EOSQL
```

### Step 13: Summary

Provide comprehensive summary:

```
✅ Service created successfully: {{service_name}}

**Service Details:**
- Name: {{service_name}}
- Port: {{port}}
- Context Path: /{{service_name_base}}
- Database: {{service_db}}
- Package: com.healthdata.{{SERVICE_NAME_CAMEL}}

**Files Created:**
- build.gradle.kts
- Application class
- SecurityConfig (gateway trust pre-configured)
- application.yml (multi-profile)
- Dockerfile
- README.md
- Liquibase master changelog
- Test class

**Files Updated:**
- docker-compose.yml (service entry added)
- docker/postgres/init-multi-db.sh (database added)

**Next Steps:**
1. Add entities: /add-entity {{service_name}} EntityName
2. Add REST endpoints: /add-endpoint {{service_name}} /resource GET
3. Build service: ./gradlew :modules:services:{{service_name}}:build
4. Run service: docker compose up {{service_name}}
5. Test service: ./gradlew :modules:services:{{service_name}}:test

**Access:**
- Health: http://localhost:{{port}}/{{service_name_base}}/actuator/health
- Metrics: http://localhost:{{port}}/{{service_name_base}}/actuator/prometheus
```

## Related Commands

- `/add-entity` - Add entity with migration
- `/add-endpoint` - Add REST endpoint
- `/validate-schema` - Validate entity-migration sync

## Related Skills

- `gateway-trust-auth` - Security patterns
- `database-migrations` - Liquibase best practices
- `hipaa-compliance` - PHI handling
