---
description: Generate complete CQRS event service with Kafka integration and read model projections
arguments:
  domain:
    description: Domain name in lowercase (e.g., medication, appointment, billing)
    type: string
    required: true
  port:
    description: Service port (8110-8119 range for event services)
    type: number
    required: true
  database:
    description: Database name (e.g., medication_event_db)
    type: string
    required: true
  events:
    description: Comma-separated event types (e.g., "medication.created,medication.updated")
    type: string
    required: false
---

# Create Event Service Command

Generate a complete CQRS event service following HDIM event-driven architecture patterns.

## What This Command Does

**Generates a complete event-driven microservice** that consumes domain events from Kafka and builds optimized read model projections for fast queries.

**Files Created (10+):**
1. **Application Class** - Spring Boot app with `@EnableKafka`
2. **Projection Entity** - JPA entity with tenant isolation and denormalization
3. **Repository** - Spring Data JPA with tenant-filtered queries
4. **Event Listener** - Kafka consumer with idempotent event handling
5. **REST Controller** - Query API for read model
6. **Statistics DTO** - Aggregate metrics
7. **application.yml** - Kafka + database configuration
8. **build.gradle.kts** - Dependencies with shared messaging module
9. **Liquibase Migrations** - Projection table schema
10. **Dockerfile** - Containerization
11. **docker-compose.yml entry** - Service definition
12. **Integration Test** - Event flow testing

**Time Savings:** 2 hours → 5 minutes (95% faster)

---

## Usage

```bash
/create-event-service <domain> <port> <database> [events]
```

## Examples

```bash
# Create medication event service
/create-event-service medication 8114 medication_event_db "medication.created,medication.updated,medication.discontinued"

# Create appointment event service
/create-event-service appointment 8115 appointment_event_db "appointment.scheduled,appointment.cancelled,appointment.completed"

# Create billing event service
/create-event-service billing 8116 billing_event_db
```

---

## Architecture

**CQRS Pattern:**
```
Command Side (Write)          Event Store (Kafka)          Query Side (Read)
┌─────────────────┐           ┌──────────────┐            ┌─────────────────┐
│ medication-     │──events──>│ Kafka Topics │──consume──>│ medication-     │
│ service         │           │              │            │ event-service   │
│ (writes)        │           │ - med.created│            │ (reads)         │
└─────────────────┘           │ - med.updated│            └─────────────────┘
                              └──────────────┘                    │
                                                                  v
                                                          ┌─────────────────┐
                                                          │ MedicationProj- │
                                                          │ ection (denorm) │
                                                          │ < 100ms queries │
                                                          └─────────────────┘
```

**Benefits:**
- **Fast reads** - Denormalized projections optimized for queries (< 100ms)
- **Scalability** - Read and write sides scale independently
- **Eventual consistency** - < 500ms from event to projection update
- **Event replay** - Rebuild projections by replaying Kafka events

---

## Implementation

You are tasked with generating a complete CQRS event service.

### Step 1: Validate Inputs

**Check service doesn't exist:**
```bash
if [ -d "backend/modules/services/{{domain}}-event-service" ]; then
    echo "Error: Service {{domain}}-event-service already exists"
    exit 1
fi
```

**Validate port range:**
- Must be 8110-8119 (reserved for event services)
- Must not be in use by existing services

**Validate naming:**
- Domain must be lowercase, single word (e.g., `medication`, NOT `Medication-Service`)
- Database must end with `_event_db`

### Step 2: Generate Variables

From inputs, derive:
- `DOMAIN` = `{{domain}}` (lowercase, e.g., "medication")
- `DOMAIN_PASCAL` = PascalCase (e.g., "Medication")
- `SERVICE_NAME` = `{{domain}}-event-service`
- `PORT` = `{{port}}`
- `DATABASE` = `{{database}}`
- `TABLE_NAME` = `{{domain}}_projections`
- `TABLE_ABBR` = Abbreviation (e.g., "mp" for medication_projections)
- `KAFKA_TOPICS` = Parse from events argument or use defaults

**Event types:**
- If provided: Use comma-separated list
- If not provided: Generate defaults (`{{domain}}.created`, `{{domain}}.updated`)

### Step 3: Create Directory Structure

```bash
SERVICE_DIR="backend/modules/services/{{SERVICE_NAME}}"

mkdir -p $SERVICE_DIR/src/main/{java,resources}
mkdir -p $SERVICE_DIR/src/test/java

# Java package structure
PKG_DIR="$SERVICE_DIR/src/main/java/com/healthdata/{{domain}}event"
mkdir -p $PKG_DIR/{api,projection,repository,listener,dto,config}

# Resources
mkdir -p $SERVICE_DIR/src/main/resources/db/changelog
mkdir -p $SERVICE_DIR/src/test/{java,resources}
```

### Step 4: Generate Application Class

Use template: `.claude/plugins/hdim-accelerator/templates/event-service/application/application-template.java`

**File:** `{{SERVICE_NAME}}/src/main/java/com/healthdata/{{domain}}event/{{DOMAIN_PASCAL}}EventServiceApplication.java`

**Replace variables:**
- `{{DOMAIN}}` → `medication`
- `{{DOMAIN_PASCAL}}` → `Medication`
- `{{PORT}}` → `8114`
- `{{DATABASE}}` → `medication_event_db`
- `{{KAFKA_TOPICS}}` → Event list

### Step 5: Generate Projection Entity

Use template: `.claude/plugins/hdim-accelerator/templates/event-service/projection/projection-template.java`

**File:** `{{SERVICE_NAME}}/src/main/java/com/healthdata/{{domain}}event/projection/{{DOMAIN_PASCAL}}Projection.java`

**Replace variables:**
- `{{DOMAIN}}` → `medication`
- `{{DOMAIN_PASCAL}}` → `Medication`
- `{{TABLE_NAME}}` → `medication_projections`
- `{{TABLE_ABBR}}` → `mp`
- `{{EVENT_SOURCES_COMMENT}}` → Generated from event types

**Add default fields:**
```java
// Example denormalized fields (customize per domain)
@Column(name = "name", length = 255)
private String name;

@Column(name = "status", length = 50)
@Builder.Default
private String status = "ACTIVE";

@Column(name = "is_active", nullable = false)
@Builder.Default
private Boolean isActive = true;
```

### Step 6: Generate Repository

Use template: `.claude/plugins/hdim-accelerator/templates/event-service/projection/repository-template.java`

**File:** `{{SERVICE_NAME}}/src/main/java/com/healthdata/{{domain}}event/repository/{{DOMAIN_PASCAL}}ProjectionRepository.java`

**Replace variables:**
- `{{DOMAIN}}` → `medication`
- `{{DOMAIN_PASCAL}}` → `Medication`

### Step 7: Generate Event Listener

Use template: `.claude/plugins/hdim-accelerator/templates/event-service/listener/listener-template.java`

**File:** `{{SERVICE_NAME}}/src/main/java/com/healthdata/{{domain}}event/listener/{{DOMAIN_PASCAL}}EventListener.java`

**Replace variables:**
- `{{DOMAIN}}` → `medication`
- `{{DOMAIN_PASCAL}}` → `Medication`
- `{{SERVICE_NAME}}` → `medication-event-service`
- `{{TOPICS_COMMENT}}` → Generated list

**Generate event handler methods** for each event type:

```java
@KafkaListener(
    topics = "medication.created",
    groupId = "medication-event-service",
    containerFactory = "kafkaListenerContainerFactory"
)
@Transactional
public void onMedicationCreated(String message) {
    try {
        JsonNode event = objectMapper.readTree(message);

        String tenantId = extractTenantId(event);
        UUID medicationId = extractEntityId(event, "medicationId");

        log.debug("Processing medication.created for medication={} in tenant={}", medicationId, tenantId);

        // Create new projection (idempotent - skip if exists)
        projectionRepository.findByTenantIdAndMedicationId(tenantId, medicationId)
            .ifPresentOrElse(
                projection -> log.warn("Projection already exists for medication={}, skipping duplicate", medicationId),
                () -> {
                    MedicationProjection projection = MedicationProjection.builder()
                        .tenantId(tenantId)
                        .medicationId(medicationId)
                        .name(extractString(event, "name"))
                        .status("ACTIVE")
                        .isActive(true)
                        .eventVersion(0L)
                        .build();

                    projectionRepository.save(projection);
                    log.info("Created projection for medication={} in tenant={}", medicationId, tenantId);
                }
            );
    } catch (Exception e) {
        log.error("Failed to process medication.created event: {}", message, e);
        throw new RuntimeException("Event processing failed", e);
    }
}
```

### Step 8: Generate REST Controller

Use template: `.claude/plugins/hdim-accelerator/templates/event-service/controller/controller-template.java`

**File:** `{{SERVICE_NAME}}/src/main/java/com/healthdata/{{domain}}event/api/{{DOMAIN_PASCAL}}ProjectionController.java`

**Replace variables:**
- `{{DOMAIN}}` → `medication`
- `{{DOMAIN_PASCAL}}` → `Medication`

### Step 9: Generate Statistics DTO

Use template: `.claude/plugins/hdim-accelerator/templates/event-service/controller/statistics-template.java`

**File:** `{{SERVICE_NAME}}/src/main/java/com/healthdata/{{domain}}event/dto/{{DOMAIN_PASCAL}}Statistics.java`

### Step 10: Generate application.yml

**File:** `{{SERVICE_NAME}}/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: {{domain}}-event-service

  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5435}/{{database}}
    username: ${POSTGRES_USER:healthdata}
    password: ${POSTGRES_PASSWORD:healthdata123}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000

  jpa:
    hibernate:
      ddl-auto: validate  # CRITICAL: Never use create/update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          batch_size: 20

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9094}
    consumer:
      group-id: {{domain}}-event-service
      auto-offset-reset: earliest  # Replay from beginning (idempotent handlers)
      enable-auto-commit: false    # Manual commit after processing
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"  # Security: trust all packages (dev only)
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
    listener:
      ack-mode: record  # Commit after each record

  cache:
    type: none  # No Redis caching for event services (stateless projections)

server:
  port: {{port}}
  servlet:
    context-path: /{{domain}}-event

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
  tracing:
    sampling:
      probability: 1.0  # 100% trace sampling for event services

logging:
  level:
    root: INFO
    com.healthdata: DEBUG
    org.springframework.kafka: DEBUG
    org.hibernate.SQL: DEBUG

---
# Development Profile
spring:
  config:
    activate:
      on-profile: dev

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

---
# Production Profile
spring:
  config:
    activate:
      on-profile: prod

spring:
  jpa:
    show-sql: false

logging:
  level:
    root: WARN
    com.healthdata: INFO
    org.springframework.kafka: INFO
```

### Step 11: Generate build.gradle.kts

**File:** `{{SERVICE_NAME}}/build.gradle.kts`

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

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Shared Modules (HDIM Platform)
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    implementation(project(":modules:shared:infrastructure:messaging"))  // Kafka auto-config
    implementation(project(":modules:shared:infrastructure:tracing"))

    // Database
    implementation("org.liquibase:liquibase-core")
    runtimeOnly("org.postgresql:postgresql")

    // Jackson (JSON)
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Utilities
    implementation("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // OpenAPI/Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.awaitility:awaitility:4.2.0")  // Eventual consistency testing
}

tasks.test {
    useJUnitPlatform()
}

tasks.bootJar {
    archiveFileName.set("{{domain}}-event-service.jar")
}
```

### Step 12: Generate Liquibase Migrations

**File:** `{{SERVICE_NAME}}/src/main/resources/db/changelog/db.changelog-master.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <!-- Authentication tables (shared across all services) -->
    <include file="db/changelog/0000-create-authentication-tables.xml"/>

    <!-- Projection table -->
    <include file="db/changelog/0001-create-{{table_name}}-table.xml"/>

</databaseChangeLog>
```

**File:** `{{SERVICE_NAME}}/src/main/resources/db/changelog/0001-create-{{table_name}}-table.xml`

Use template: `.claude/plugins/hdim-accelerator/templates/event-service/migration/projection-migration-template.xml`

**Replace variables:**
- `{{TABLE_NAME}}` → `medication_projections`
- `{{TABLE_ABBR}}` → `mp`
- `{{DOMAIN}}` → `medication`
- `{{AUTHOR}}` → Current git user
- `{{EVENT_SOURCES_COMMENT}}` → Generated list

### Step 13: Generate Dockerfile

**File:** `{{SERVICE_NAME}}/Dockerfile`

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy JAR
COPY build/libs/{{domain}}-event-service.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:{{port}}/{{domain}}-event/actuator/health || exit 1

# Run
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Step 14: Add to docker-compose.yml

Add service entry:

```yaml
  {{domain}}-event-service:
    build:
      context: ./backend/modules/services/{{domain}}-event-service
      dockerfile: Dockerfile
    container_name: hdim-{{domain}}-event-service
    ports:
      - "{{port}}:{{port}}"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      POSTGRES_HOST: postgres
      POSTGRES_PORT: 5432
      POSTGRES_DB: {{database}}
      POSTGRES_USER: healthdata
      POSTGRES_PASSWORD: healthdata123
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      GATEWAY_AUTH_DEV_MODE: "true"
    depends_on:
      - postgres
      - kafka
    networks:
      - hdim-network
    restart: unless-stopped
```

### Step 15: Add Database to init-multi-db.sh

Add to `docker/postgres/init-multi-db.sh`:

```bash
# Create {{domain}}-event-service database
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE {{database}};
    GRANT ALL PRIVILEGES ON DATABASE {{database}} TO $POSTGRES_USER;
EOSQL
```

### Step 16: Generate Integration Test

**File:** `{{SERVICE_NAME}}/src/test/java/com/healthdata/{{domain}}event/integration/{{DOMAIN_PASCAL}}EventFlowIntegrationTest.java`

```java
package com.healthdata.{{domain}}event.integration;

import com.healthdata.{{domain}}event.projection.{{DOMAIN_PASCAL}}Projection;
import com.healthdata.{{domain}}event.repository.{{DOMAIN_PASCAL}}ProjectionRepository;
import com.healthdata.shared.authentication.test.GatewayTrustTestHeaders;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class {{DOMAIN_PASCAL}}EventFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("{{database}}")
            .withUsername("healthdata")
            .withPassword("testpassword");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private {{DOMAIN_PASCAL}}ProjectionRepository repository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private GatewayTrustTestHeaders testHeaders;
    private static final String TENANT_ID = "test-tenant-001";

    @BeforeEach
    void setUp() {
        testHeaders = GatewayTrustTestHeaders.builder()
                .userId(UUID.randomUUID())
                .username("test_admin")
                .tenantIds(TENANT_ID)
                .roles("ADMIN,EVALUATOR")
                .build();

        repository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Should create projection from {{domain}}.created event")
    void shouldCreateProjectionFromEvent() throws Exception {
        // Given: Event JSON
        UUID {{domain}}Id = UUID.randomUUID();
        String eventJson = String.format("""
            {
                "eventId": "%s",
                "eventType": "{{domain}}.created",
                "tenantId": "%s",
                "{{domain}}Id": "%s",
                "name": "Test {{DOMAIN_PASCAL}}",
                "timestamp": "2024-01-20T10:00:00Z"
            }
            """, UUID.randomUUID(), TENANT_ID, {{domain}}Id);

        // When: Publish event to Kafka
        kafkaTemplate.send("{{domain}}.created", eventJson).get(5, TimeUnit.SECONDS);

        // Then: Wait for eventual consistency (< 500ms SLA)
        await().atMost(2, TimeUnit.SECONDS)
               .pollInterval(100, TimeUnit.MILLISECONDS)
               .until(() -> repository.findByTenantIdAnd{{DOMAIN_PASCAL}}Id(TENANT_ID, {{domain}}Id).isPresent());

        // Verify projection via REST API
        mockMvc.perform(get("/api/v1/{{domain}}-projections/{id}", {{domain}}Id)
                        .headers(testHeaders.toHttpHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.{{domain}}Id").value({{domain}}Id.toString()))
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID))
                .andExpect(jsonPath("$.name").value("Test {{DOMAIN_PASCAL}}"));
    }

    @Test
    @Order(2)
    @DisplayName("Should measure eventual consistency timing (< 500ms SLA)")
    void shouldMeetEventualConsistencySLA() throws Exception {
        UUID {{domain}}Id = UUID.randomUUID();
        String eventJson = String.format("""
            {
                "tenantId": "%s",
                "{{domain}}Id": "%s",
                "timestamp": "2024-01-20T10:00:00Z"
            }
            """, TENANT_ID, {{domain}}Id);

        long startTime = System.currentTimeMillis();
        kafkaTemplate.send("{{domain}}.created", eventJson).get();

        // Poll until projection exists
        await().atMost(500, TimeUnit.MILLISECONDS)
               .pollInterval(10, TimeUnit.MILLISECONDS)
               .until(() -> repository.findByTenantIdAnd{{DOMAIN_PASCAL}}Id(TENANT_ID, {{domain}}Id).isPresent());

        long latency = System.currentTimeMillis() - startTime;
        System.out.println("Eventual consistency latency: " + latency + "ms");

        // Assert: < 500ms SLA
        Assertions.assertTrue(latency < 500, "Eventual consistency exceeded 500ms SLA: " + latency + "ms");
    }
}
```

### Step 17: Summary

Provide comprehensive summary:

```
✅ Event Service Created: {{domain}}-event-service

**Service Details:**
- Name: {{domain}}-event-service
- Port: {{port}}
- Context Path: /{{domain}}-event
- Database: {{database}}
- Kafka Topics: {{event_list}}

**Files Created:**
- Application: {{DOMAIN_PASCAL}}EventServiceApplication.java
- Projection: {{DOMAIN_PASCAL}}Projection.java
- Repository: {{DOMAIN_PASCAL}}ProjectionRepository.java
- Event Listener: {{DOMAIN_PASCAL}}EventListener.java
- Controller: {{DOMAIN_PASCAL}}ProjectionController.java
- Statistics: {{DOMAIN_PASCAL}}Statistics.java
- application.yml (Kafka + DB configured)
- build.gradle.kts (shared messaging module included)
- Liquibase migrations (projection table)
- Dockerfile
- Integration test

**Files Updated:**
- docker-compose.yml (service entry added)
- docker/postgres/init-multi-db.sh (database added)

**Next Steps:**
1. Customize projection fields in {{DOMAIN_PASCAL}}Projection.java
2. Add domain-specific columns to migration XML
3. Implement event handlers in {{DOMAIN_PASCAL}}EventListener.java
4. Add custom query methods to repository
5. Build service: ./gradlew :modules:services:{{domain}}-event-service:build
6. Run service: docker compose up {{domain}}-event-service
7. Test event flow: Run integration tests
8. Publish test event to Kafka topic

**API Endpoints:**
- GET /{{domain}}-event/api/v1/{{domain}}-projections/{id} - Get projection by ID
- GET /{{domain}}-event/api/v1/{{domain}}-projections - List projections (paginated)
- GET /{{domain}}-event/api/v1/{{domain}}-projections/stats - Get statistics
- GET /{{domain}}-event/actuator/health - Health check

**Architecture:**
- Pattern: CQRS with eventual consistency
- Denormalization: Optimized for fast reads (< 100ms)
- Idempotency: Find-or-create pattern
- Multi-tenant: All queries filtered by tenant_id
- HIPAA: Cache-Control: no-store headers

**Time Saved:** ~2 hours of manual development
```

---

## Best Practices (Built Into Templates)

### 1. Eventual Consistency
- **SLA:** < 500ms from event publication to projection update
- **Testing:** Use Awaitility to verify timing
- **Monitoring:** Track consumer lag metrics

### 2. Idempotency
- **Pattern:** Find-or-create in event handlers
- **Duplicate events:** Skip if projection exists
- **Out-of-order events:** Handle gracefully

### 3. Multi-Tenant Isolation
- **All queries:** MUST filter by tenantId
- **Indexes:** tenant_id indexed for performance
- **404 not 403:** Prevent information disclosure

### 4. HIPAA Compliance
- **Cache headers:** Cache-Control: no-store
- **Audit logging:** Recommended for PHI access
- **Encryption:** TLS in transit, encrypted at rest

### 5. Performance
- **Query response:** < 100ms (99th percentile)
- **Denormalization:** Pre-calculate aggregates
- **Indexes:** Optimize for common queries

---

## Related Commands

- `/add-event-handler` - Add new event listener to service
- `/add-projection-query` - Add custom query method
- `/validate-schema` - Validate entity-migration sync

## Related Skills

- `cqrs-event-driven` - CQRS patterns and best practices
- `kafka-messaging` - Kafka configuration and troubleshooting
- `database-migrations` - Liquibase migration patterns

## Documentation

- **CQRS Guide:** `backend/docs/CQRS_INTEGRATION_TESTING_GUIDE.md`
- **Event Catalog:** `docs/events/EVENT_CATALOG.md` (TODO: create)
- **Kafka Setup:** `backend/modules/shared/infrastructure/messaging/README.md`
