# Service Creation Template: Event Sourcing Pattern

**Purpose**: Scaffold a new Event Sourcing microservice following HDIM patterns
**Last Updated**: January 19, 2026
**Template Version**: 1.0

---

## Overview

This template guides creation of a new service using Event Sourcing + CQRS architecture (as used in Phase 5 event services):
- **patient-event-service** - Patient lifecycle events
- **quality-measure-event-service** - Quality measure events
- **care-gap-event-service** - Care gap events
- **clinical-workflow-event-service** - Workflow events

For **CRUD services**, follow the standard Spring Boot microservice pattern instead.

---

## Step 1: Service Planning

### Define Your Domain

```
Service: {YourService}-event-service

Domain: {Bounded Context}
Example: Patient management, Quality measure evaluation, Care gap detection

Events: {List of domain events}
Example:
  - PatientCreatedEvent
  - PatientUpdatedEvent
  - PatientMergedEvent
  - PatientLinkedEvent

Projections: {Denormalized read models}
Example:
  - PatientProjection (current patient state)
  - PatientTimelineProjection (historical changes)
```

### Service Components

```
{service-name}-event-service/
├── src/main/java/com/healthdata/{domain}/
│   ├── {Service}Application.java
│   ├── api/v1/
│   │   ├── controller/
│   │   │   └── {Domain}EventController.java
│   │   └── dto/
│   │       ├── {Event}Request.java
│   │       └── {Event}Response.java
│   ├── domain/
│   │   ├── event/
│   │   │   ├── {DomainEvent}.java
│   │   │   └── {DomainEvent}Handler.java
│   │   ├── model/
│   │   │   └── {DomainAggregate}.java
│   │   └── repository/
│   │       └── {DomainAggregate}Repository.java
│   ├── projection/
│   │   ├── {Entity}Projection.java
│   │   └── {Entity}ProjectionRepository.java
│   ├── eventhandler/
│   │   └── {DomainEvent}EventHandler.java
│   ├── service/
│   │   └── {Domain}EventApplicationService.java
│   └── config/
│       ├── EventHandlerConfig.java
│       ├── KafkaConfig.java
│       ├── SecurityConfig.java
│       └── LiquibaseConfig.java
├── src/main/resources/
│   ├── application.yml
│   ├── db/changelog/
│   │   ├── 0000-enable-extensions.xml
│   │   ├── 0001-create-event-store-table.xml
│   │   └── 0002-create-projections-tables.xml
│   └── db/changelog/db.changelog-master.xml
├── Dockerfile
└── README.md
```

---

## Step 2: Gradle Build Configuration

### Add to `build.gradle.kts`

```kotlin
// Place in: backend/modules/services/{service-name}-event-service/build.gradle.kts

plugins {
    id("org.springframework.boot") version "3.2.1"
    id("java")
}

group = "com.healthdata.{domain}"
version = "0.1.0"

repositories {
    mavenCentral()
    maven { url = uri("https://build.fhir.org/ig/") }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // PostgreSQL & Database
    implementation("org.postgresql:postgresql:42.7.1")
    implementation("org.liquibase:liquibase-core:4.27.0")
    runtimeOnly("com.zaxxer:HikariCP:5.1.0")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // Tracing
    implementation("io.opentelemetry.javaagent:opentelemetry-javaagent:1.32.1")

    // HDIM Shared
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:infrastructure:event-sourcing"))
    implementation(project(":modules:shared:infrastructure:messaging"))

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
}

tasks.bootJar {
    archiveFileName.set("${project.name}-${project.version}.jar")
}
```

---

## Step 3: Application Main Class

### `src/main/java/com/healthdata/{domain}/{Domain}EventServiceApplication.java`

```java
package com.healthdata.{domain};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * {Domain} Event Service - Event Sourcing microservice
 *
 * Purpose:
 * - Maintains immutable event log for {domain} domain
 * - Provides REST API for {domain} events
 * - Builds denormalized projections for efficient queries
 * - Publishes events to Kafka for other services
 *
 * Event Flow:
 * 1. REST API receives command (Create{Domain}, Update{Domain})
 * 2. ApplicationService validates and publishes event
 * 3. EventStore persists event to database
 * 4. Event is published to Kafka
 * 5. Projection handlers subscribe and update denormalized views
 * 6. Services query projections (not raw events)
 *
 * Port: {PORT}
 * Database: {domain}_db
 */
@SpringBootApplication
@EnableKafka
@EnableAsync
public class {Domain}EventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run({Domain}EventServiceApplication.class, args);
    }
}
```

---

## Step 4: Domain Event Definition

### `src/main/java/com/healthdata/{domain}/domain/event/{Domain}CreatedEvent.java`

```java
package com.healthdata.{domain}.domain.event;

import com.healthdata.eventsourcing.core.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event: {Domain} created
 *
 * Properties:
 * - Immutable (no setters)
 * - Serializable (Jackson annotations)
 * - Versioned (eventVersion field for schema evolution)
 * - Tenant-aware (tenantId for multi-tenant isolation)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class {Domain}CreatedEvent implements DomainEvent {

    private UUID aggregateId;              // Primary ID for this {domain} instance
    private String tenantId;               // Multi-tenant isolation
    private int eventVersion;              // Schema version (1 initially)
    private Instant occurredAt;            // When event happened
    private String correlationId;          // Trace events across services

    // Domain-specific fields
    private String name;
    private String description;
    // ... other fields specific to your domain

    @Override
    public String getAggregateType() {
        return "{Domain}";
    }

    @Override
    public String getEventType() {
        return "{DOMAIN}_CREATED";
    }

    @Override
    public int getVersion() {
        return eventVersion;
    }
}
```

---

## Step 5: REST Controller

### `src/main/java/com/healthdata/{domain}/api/v1/controller/{Domain}EventController.java`

```java
package com.healthdata.{domain}.api.v1.controller;

import com.healthdata.{domain}.api.v1.dto.*;
import com.healthdata.{domain}.service.{Domain}EventApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/{domains}")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearer-jwt")
public class {Domain}EventController {

    private final {Domain}EventApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLINICIAN')")
    @Operation(summary = "Create {domain} event")
    public ResponseEntity<{Domain}EventResponse> create{Domain}(
            @Valid @RequestBody Create{Domain}Request request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        {Domain}EventResponse response = applicationService.create{Domain}(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLINICIAN', 'VIEWER')")
    @Operation(summary = "Get {domain} by ID")
    public ResponseEntity<{Domain}EventResponse> get{Domain}(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        {Domain}EventResponse response = applicationService.get{Domain}(id, tenantId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLINICIAN')")
    @Operation(summary = "Update {domain}")
    public ResponseEntity<{Domain}EventResponse> update{Domain}(
            @PathVariable UUID id,
            @Valid @RequestBody Update{Domain}Request request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        {Domain}EventResponse response = applicationService.update{Domain}(id, request, tenantId);
        return ResponseEntity.ok(response);
    }
}
```

---

## Step 6: Application Service (Business Logic)

### `src/main/java/com/healthdata/{domain}/service/{Domain}EventApplicationService.java`

```java
package com.healthdata.{domain}.service;

import com.healthdata.{domain}.api.v1.dto.*;
import com.healthdata.{domain}.domain.event.*;
import com.healthdata.{domain}.projection.{Domain}Projection;
import com.healthdata.{domain}.projection.{Domain}ProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Application service for {domain} event operations
 *
 * Responsibilities:
 * 1. Validate input (CreateRequest → Event)
 * 2. Publish event to Event Store
 * 3. Publish to Kafka for async processing
 * 4. Read from projections (not raw events)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class {Domain}EventApplicationService {

    private final {Domain}ProjectionRepository projectionRepository;
    private final EventStore eventStore;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    @Transactional
    public {Domain}EventResponse create{Domain}(
            Create{Domain}Request request,
            String tenantId) {

        UUID aggregateId = UUID.randomUUID();

        // Create domain event
        {Domain}CreatedEvent event = {Domain}CreatedEvent.builder()
            .aggregateId(aggregateId)
            .tenantId(tenantId)
            .eventVersion(1)
            .occurredAt(Instant.now())
            .correlationId(UUID.randomUUID().toString())
            .name(request.getName())
            .description(request.getDescription())
            // ... map other fields
            .build();

        // Persist to event store
        eventStore.append(event);
        log.info("Published event: {} with id: {}", event.getEventType(), aggregateId);

        // Publish to Kafka for other services
        kafkaTemplate.send("{domain}-events", event);

        // Query projection (which was updated by event handler)
        {Domain}Projection projection = projectionRepository.findById(aggregateId)
            .orElseThrow(() -> new RuntimeException("Projection not found"));

        return mapToResponse(projection);
    }

    public {Domain}EventResponse get{Domain}(UUID id, String tenantId) {
        {Domain}Projection projection = projectionRepository.findByIdAndTenant(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("{Domain}", id.toString()));

        return mapToResponse(projection);
    }

    private {Domain}EventResponse mapToResponse({Domain}Projection projection) {
        return {Domain}EventResponse.builder()
            .id(projection.getId())
            .name(projection.getName())
            // ... map other fields
            .build();
    }
}
```

---

## Step 7: Projection (Denormalized Read Model)

### `src/main/java/com/healthdata/{domain}/projection/{Domain}Projection.java`

```java
package com.healthdata.{domain}.projection;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Denormalized projection of {domain} for efficient queries
 *
 * Updated by event handlers in real-time:
 * 1. Event published to Kafka
 * 2. ProjectionHandler subscribes
 * 3. Handler updates this projection
 * 4. Queries read from here (not from raw events)
 *
 * Benefits:
 * - Fast queries (no event stream processing)
 * - Optimized for read patterns (columns for common queries)
 * - Up-to-date via event handlers
 */
@Entity
@Table(name = "{domains}", indexes = {
    @Index(name = "idx_{domain}_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_{domain}_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class {Domain}Projection {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // ... other fields optimized for queries
}
```

---

## Step 8: Event Handler (Updates Projection)

### `src/main/java/com/healthdata/{domain}/eventhandler/{Domain}CreatedEventHandler.java`

```java
package com.healthdata.{domain}.eventhandler;

import com.healthdata.{domain}.domain.event.{Domain}CreatedEvent;
import com.healthdata.{domain}.projection.{Domain}Projection;
import com.healthdata.{domain}.projection.{Domain}ProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Event handler: Updates projections when {Domain}CreatedEvent is published
 *
 * Flow:
 * 1. Event published to Kafka topic: {domain}-events
 * 2. This listener consumes the event
 * 3. Creates/updates projection
 * 4. Commits transaction
 * 5. Projection now available for queries
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class {Domain}CreatedEventHandler {

    private final {Domain}ProjectionRepository projectionRepository;

    @KafkaListener(
        topics = "{domain}-events",
        groupId = "{domain}-projection-handler",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handle({Domain}CreatedEvent event) {
        log.info("Handling {Domain}CreatedEvent for aggregate: {}", event.getAggregateId());

        {Domain}Projection projection = {Domain}Projection.builder()
            .id(event.getAggregateId())
            .tenantId(event.getTenantId())
            .name(event.getName())
            .description(event.getDescription())
            .createdAt(event.getOccurredAt())
            .updatedAt(Instant.now())
            // ... map other fields
            .build();

        projectionRepository.save(projection);
        log.info("Projection saved for {}: {}", event.getEventType(), event.getAggregateId());
    }
}
```

---

## Step 9: Database Migrations (Liquibase)

### `src/main/resources/db/changelog/0001-create-event-store-table.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="0001-create-event-store-table" author="your-name">
        <comment>Create event store table for immutable event log</comment>

        <createTable tableName="event_store">
            <column name="id" type="BIGINT">
                <constraints primaryKey="true" primaryKeyName="pk_event_store"/>
            </column>
            <column name="aggregate_id" type="UUID">
                <constraints nullable="false"/>
            </column>
            <column name="aggregate_type" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="event_type" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="event_data" type="JSONB">
                <constraints nullable="false"/>
            </column>
            <column name="tenant_id" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="occurred_at" type="TIMESTAMP WITH TIME ZONE">
                <constraints nullable="false"/>
            </column>
            <column name="version" type="INT">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <createIndex indexName="idx_event_store_aggregate" tableName="event_store">
            <column name="aggregate_id"/>
            <column name="aggregate_type"/>
        </createIndex>

        <createIndex indexName="idx_event_store_tenant" tableName="event_store">
            <column name="tenant_id"/>
        </createIndex>

        <rollback>
            <dropTable tableName="event_store"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

### `src/main/resources/db/changelog/0002-create-projections-table.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog">

    <changeSet id="0002-create-{domain}s-table" author="your-name">
        <comment>Create {domain} projections table</comment>

        <createTable tableName="{domains}">
            <column name="id" type="UUID">
                <constraints primaryKey="true" primaryKeyName="pk_{domain}s"/>
            </column>
            <column name="tenant_id" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="description" type="TEXT"/>
            <column name="created_at" type="TIMESTAMP WITH TIME ZONE">
                <constraints nullable="false"/>
            </column>
            <column name="updated_at" type="TIMESTAMP WITH TIME ZONE">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <createIndex indexName="idx_{domain}_tenant" tableName="{domains}">
            <column name="tenant_id"/>
        </createIndex>

        <rollback>
            <dropTable tableName="{domains}"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

---

## Step 10: Configuration Files

### `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: {service-name}-event-service

  datasource:
    url: jdbc:postgresql://postgres:5432/{domain}_db
    username: healthdata
    password: ${DB_PASSWORD:healthdata_password}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml

  kafka:
    bootstrap-servers: kafka:29092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      bootstrap-servers: kafka:29092
      group-id: {domain}-projection-handler
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"

server:
  port: ${SERVER_PORT:8110}

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    root: INFO
    com.healthdata: DEBUG
```

---

## Step 11: Dockerfile

```dockerfile
# Place in: backend/modules/services/{service-name}-event-service/Dockerfile

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/{service-name}-event-service-*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=5 --start-period=60s \
  CMD wget --spider -q http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Step 12: README Documentation

Create `README.md` documenting:
- Service purpose and domain
- Event types published
- Projections available
- API endpoints with examples
- Database schema
- Configuration options
- Deployment instructions

See: [Service Catalog](../../docs/services/SERVICE_CATALOG.md) for README template

---

## Testing Checklist

- [ ] Unit tests for domain events
- [ ] Integration tests for event store persistence
- [ ] Event handler tests (projection updates)
- [ ] REST controller tests
- [ ] Multi-tenant isolation tests
- [ ] Kafka publisher/subscriber tests
- [ ] Test coverage >= 80%

---

## Deployment Checklist

- [ ] Service builds successfully: `./gradlew :modules:services:{service-name}-event-service:build`
- [ ] Docker image builds: `docker compose build {service-name}-event-service`
- [ ] Migrations run cleanly
- [ ] Service starts: `docker compose up {service-name}-event-service`
- [ ] Health check passes: `curl http://localhost:PORT/actuator/health`
- [ ] API responds: `curl http://localhost:PORT/api/v1/{domains}`

---

## Related Documentation

- **Event Sourcing Architecture**: [Complete guide](../../architecture/EVENT_SOURCING_ARCHITECTURE.md)
- **Coding Standards**: [Code patterns](../backend/docs/CODING_STANDARDS.md)
- **TDD Swarm**: [Development methodology](TDD_SWARM.md)
- **Example Services**: patient-event-service, quality-measure-event-service, care-gap-event-service

---

_Template Version: 1.0_
_Last Updated: January 19, 2026_
_Based on Phase 5 Event Services Implementation (Oct 2025 - Jan 2026)_
