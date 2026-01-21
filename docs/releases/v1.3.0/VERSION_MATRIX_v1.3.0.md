# HDIM Platform Version Matrix - v1.3.0

**Release Version:** v1.3.0
**Release Date:** 2026-01-20

---

## Microservices (34 Services)

| Service | Port | Version | Image | Description |
|---------|------|---------|-------|-------------|
| gateway-service | 8001 | v1.3.0 | hdim/gateway-service:v1.3.0 | API Gateway |
| gateway-read-service | 8080 | v1.3.0 | hdim/gateway-read-service:v1.3.0 | Gateway Read Replica |
| cql-engine-service | 8081 | v1.3.0 | hdim/cql-engine-service:v1.3.0 | CQL Evaluation Engine |
| patient-service | 8084 | v1.3.0 | hdim/patient-service:v1.3.0 | Patient Data Management |
| fhir-service | 8085 | v1.3.0 | hdim/fhir-service:v1.3.0 | FHIR R4 Resource Server |
| care-gap-service | 8086 | v1.3.0 | hdim/care-gap-service:v1.3.0 | Care Gap Detection |
| quality-measure-service | 8087 | v1.3.0 | hdim/quality-measure-service:v1.3.0 | HEDIS Quality Measures |
| agent-runtime-service | 8088 | v1.3.0 | hdim/agent-runtime-service:v1.3.0 | AI Agent Runtime |
| data-enrichment-service | 8089 | v1.3.0 | hdim/data-enrichment-service:v1.3.0 | Data Enrichment Pipeline |
| ai-assistant-service | 8090 | v1.3.0 | hdim/ai-assistant-service:v1.3.0 | AI Clinical Assistant |
| documentation-service | 8091 | v1.3.0 | hdim/documentation-service:v1.3.0 | Clinical Documentation |
| analytics-service | 8092 | v1.3.0 | hdim/analytics-service:v1.3.0 | Analytics & Reporting |
| predictive-analytics-service | 8093 | v1.3.0 | hdim/predictive-analytics-service:v1.3.0 | Predictive Analytics |
| sdoh-service | 8094 | v1.3.0 | hdim/sdoh-service:v1.3.0 | Social Determinants of Health |
| event-router-service | 8095 | v1.3.0 | hdim/event-router-service:v1.3.0 | Event Routing |
| agent-builder-service | 8096 | v1.3.0 | hdim/agent-builder-service:v1.3.0 | AI Agent Builder |
| approval-service | 8097 | v1.3.0 | hdim/approval-service:v1.3.0 | Workflow Approvals |
| payer-workflows-service | 8098 | v1.3.0 | hdim/payer-workflows-service:v1.3.0 | Payer Workflows |
| cdr-processor-service | 8099 | v1.3.0 | hdim/cdr-processor-service:v1.3.0 | Clinical Data Repository Processor |
| ehr-connector-service | 8100 | v1.3.0 | hdim/ehr-connector-service:v1.3.0 | EHR Integration |
| ecr-service | 8101 | v1.3.0 | hdim/ecr-service:v1.3.0 | Electronic Case Reporting |
| prior-auth-service | 8102 | v1.3.0 | hdim/prior-auth-service:v1.3.0 | Prior Authorization |
| migration-workflow-service | 8103 | v1.3.0 | hdim/migration-workflow-service:v1.3.0 | Data Migration |
| qrda-export-service | 8104 | v1.3.0 | hdim/qrda-export-service:v1.3.0 | QRDA I/III Export |
| hcc-service | 8105 | v1.3.0 | hdim/hcc-service:v1.3.0 | HCC Risk Adjustment |
| sales-automation-service | 8106 | v1.3.0 | hdim/sales-automation-service:v1.3.0 | Sales Automation |
| notification-service | 8107 | v1.3.0 | hdim/notification-service:v1.3.0 | Notification Delivery |
| patient-event-service | 8110 | v1.3.0 | hdim/patient-event-service:v1.3.0 | Patient Event Projections (CQRS) |
| care-gap-event-service | 8111 | v1.3.0 | hdim/care-gap-event-service:v1.3.0 | Care Gap Event Projections (CQRS) |
| quality-measure-event-service | 8112 | v1.3.0 | hdim/quality-measure-event-service:v1.3.0 | Quality Measure Event Projections (CQRS) |
| clinical-workflow-event-service | 8113 | v1.3.0 | hdim/clinical-workflow-event-service:v1.3.0 | Clinical Workflow Event Projections (CQRS) |
| consent-service | 8082 | v1.3.0 | hdim/consent-service:v1.3.0 | Patient Consent Management |
| event-processing-service | 8083 | v1.3.0 | hdim/event-processing-service:v1.3.0 | Event Processing Engine |
| demo-seeding-service | 8098 | v1.3.0 | hdim/demo-seeding-service:v1.3.0 | Demo Data Seeding |

---

## Infrastructure Components

| Component | Version | Port(s) | Image |
|-----------|---------|---------|-------|
| PostgreSQL | 16-alpine | 5435 | postgres:16-alpine |
| Redis | 7-alpine | 6380 | redis:7-alpine |
| Apache Kafka | 3.6 | 9094 | confluentinc/cp-kafka:7.5.0 |
| Zookeeper | 3.6 | 2181 | confluentinc/cp-zookeeper:7.5.0 |
| Jaeger | latest | 16686, 4318 | jaegertracing/all-in-one:latest |

---

## Backend Dependencies

| Dependency | Version | Source |
|------------|---------|--------|
| Java | 21 (LTS) | Eclipse Temurin |
| Kotlin | 2.0.21 | gradle/libs.versions.toml |
| Spring Boot | 3.3.6 | gradle/libs.versions.toml |
| Spring Cloud | 2023.0.6 | gradle/libs.versions.toml |
| Spring Security | 6.5.7 | gradle/libs.versions.toml |
| HAPI FHIR | 7.6.0 | gradle/libs.versions.toml |
| CQL Engine | 3.3.1 | gradle/libs.versions.toml |
| PostgreSQL Driver | 42.7.7 | gradle/libs.versions.toml |
| HikariCP | 6.0.0 | gradle/libs.versions.toml |
| Hibernate | 6.6.1.Final | gradle/libs.versions.toml |
| Liquibase | 4.29.2 | gradle/libs.versions.toml |
| Redis Client | 3.5.7 | gradle/libs.versions.toml |
| Kafka | 3.8.0 | gradle/libs.versions.toml |
| Spring Kafka | 3.3.11 | gradle/libs.versions.toml |
| SpringDoc OpenAPI | 2.6.0 | gradle/libs.versions.toml |
| Jackson | 2.17.2 | gradle/libs.versions.toml |
| Resilience4j | 2.2.0 | gradle/libs.versions.toml |
| JJWT | 0.12.6 | gradle/libs.versions.toml |
| JUnit | 5.14.1 | gradle/libs.versions.toml |
| Mockito | 5.2.0 | gradle/libs.versions.toml |
| Testcontainers | 1.20.4 | gradle/libs.versions.toml |
| Lombok | 1.18.34 | gradle/libs.versions.toml |
| MapStruct | 1.6.2 | gradle/libs.versions.toml |
| Micrometer | 1.13.6 | gradle/libs.versions.toml |
| Micrometer Tracing | 1.3.5 | gradle/libs.versions.toml |
| OpenTelemetry | 1.32.0 | gradle/libs.versions.toml |
| AWS SDK | 2.25.0 | gradle/libs.versions.toml |

---

## Docker Base Images

| Image | Tag | Purpose |
|-------|-----|---------|
| eclipse-temurin | 21-jre-alpine | Java runtime for services |
| postgres | 16-alpine | Database |
| redis | 7-alpine | Cache |
| confluentinc/cp-kafka | 7.5.0 | Message streaming |

---

## Compatibility Matrix

| Component | Minimum Version | Recommended Version | Maximum Tested |
|-----------|----------------|---------------------|----------------|
| Java | 21 | 21 | 21 |
| Gradle | 8.5 | 8.11+ | 8.11 |
| Docker | 24.0 | 24.0+ | 25.0 |
| Docker Compose | 2.20 | 2.20+ | 2.24 |
| PostgreSQL | 15 | 16 | 16 |
| Redis | 7.0 | 7.2 | 7.2 |
| Kafka | 3.5 | 3.6 | 3.6 |

---

## Environment-Specific Configurations

### Development
- Profile: `dev`
- Trace Sampling: 100% (1.0)
- Connection Pool: HIGH=50, MEDIUM=20, LOW=10

### Staging
- Profile: `staging`
- Trace Sampling: 50% (0.5)
- Connection Pool: Same as production

### Production
- Profile: `prod`
- Trace Sampling: 10% (0.1)
- Connection Pool: HIGH=50, MEDIUM=20, LOW=10

---

**Generated:** 2026-01-21 03:45:00
**From:** gradle/libs.versions.toml, docker-compose.yml
**Services:** 33 microservices
**Dependencies:** 27 key backend dependencies
