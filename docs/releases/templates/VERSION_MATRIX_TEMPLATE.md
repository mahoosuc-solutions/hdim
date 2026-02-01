# HDIM Platform Version Matrix - {VERSION}

**Release Version:** {VERSION}
**Release Date:** {DATE}

---

## Microservices (34 Services)

| Service | Port | Version | Image | Description |
|---------|------|---------|-------|-------------|
| gateway-service | 8001 | {VERSION} | hdim/gateway-service:{VERSION} | API Gateway |
| gateway-read-service | 8080 | {VERSION} | hdim/gateway-read-service:{VERSION} | Gateway Read Replica |
| cql-engine-service | 8081 | {VERSION} | hdim/cql-engine-service:{VERSION} | CQL Evaluation Engine |
| patient-service | 8084 | {VERSION} | hdim/patient-service:{VERSION} | Patient Data Management |
| fhir-service | 8085 | {VERSION} | hdim/fhir-service:{VERSION} | FHIR R4 Resource Server |
| care-gap-service | 8086 | {VERSION} | hdim/care-gap-service:{VERSION} | Care Gap Detection |
| quality-measure-service | 8087 | {VERSION} | hdim/quality-measure-service:{VERSION} | HEDIS Quality Measures |
{AUTO_GENERATE_ALL_34_SERVICES}

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
| Spring Boot | {SPRING_BOOT_VERSION} | gradle/libs.versions.toml |
| HAPI FHIR | {HAPI_FHIR_VERSION} | gradle/libs.versions.toml |
| Liquibase | {LIQUIBASE_VERSION} | gradle/libs.versions.toml |
| HikariCP | {HIKARICP_VERSION} | gradle/libs.versions.toml |
| Jackson | {JACKSON_VERSION} | gradle/libs.versions.toml |
| Lombok | {LOMBOK_VERSION} | gradle/libs.versions.toml |

{AUTO_GENERATE_FROM_GRADLE_LIBS_VERSIONS_TOML}

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

**Generated:** {TIMESTAMP}
**From:** gradle/libs.versions.toml, docker-compose.yml
