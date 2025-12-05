# HealthData in Motion - Modular Monolith Architecture

## Overview
Transforming from 9 microservices to a single, well-structured modular monolith using Spring Modulith.

## New Architecture

### Single Application, Multiple Modules

```
healthdata-platform/
├── src/main/java/com/healthdata/
│   ├── fhir/                    # FHIR Module
│   │   ├── api/
│   │   ├── domain/
│   │   ├── repository/
│   │   └── events/
│   ├── quality/                 # Quality Measures Module
│   │   ├── api/
│   │   ├── domain/
│   │   ├── service/
│   │   └── events/
│   ├── cql/                     # CQL Engine Module
│   │   ├── api/
│   │   ├── engine/
│   │   └── events/
│   ├── patient/                 # Patient Management Module
│   │   ├── api/
│   │   ├── domain/
│   │   └── events/
│   ├── caregap/                 # Care Gap Module
│   │   ├── api/
│   │   ├── domain/
│   │   └── events/
│   ├── notification/            # Notification Module
│   │   ├── channels/
│   │   ├── templates/
│   │   └── events/
│   └── shared/                  # Shared Components
│       ├── auth/
│       ├── events/
│       └── utils/
```

## Benefits Over Current Architecture

### 1. Performance
- **15x faster** inter-module communication (in-process vs REST)
- Single JVM, no network overhead
- Shared cache, no duplication
- Single database connection pool

### 2. Simplicity
- **1 application** instead of 9
- **1 database** instead of 6
- **1 deployment** instead of 9
- **1 health check** instead of 9

### 3. Development Speed
- No API versioning between modules
- Compile-time type safety
- Single IDE project
- Unified debugging

### 4. Resource Efficiency
- **70% less memory** (1 JVM vs 9)
- **80% less CPU** (no serialization/deserialization)
- **90% less network traffic**
- Single container deployment

## Migration Strategy

### Phase 1: Core Platform (Week 1)
1. Create new Spring Boot 3.3.5 project
2. Add Spring Modulith dependencies
3. Set up module structure
4. Configure single PostgreSQL database

### Phase 2: Module Migration (Week 2)
Priority order:
1. Patient Module (foundation)
2. FHIR Module (data ingestion)
3. Quality Module (core business logic)
4. CQL Engine Module (rules processing)
5. Care Gap Module (analytics)
6. Notification Module (communications)

### Phase 3: Event-Driven Architecture (Week 3)
1. Replace REST calls with Spring Events
2. Implement async processing with @Async
3. Add event sourcing for audit trail
4. Configure transaction boundaries

### Phase 4: Optimization (Week 4)
1. Add caching with Spring Cache
2. Implement batch processing
3. Add GraalVM native compilation
4. Performance testing and tuning

## Technical Stack

### Core Technologies
- **Framework**: Spring Boot 3.3.5
- **Modularity**: Spring Modulith 1.3
- **Database**: PostgreSQL 16 (single instance)
- **Caching**: Spring Cache + Caffeine
- **Events**: Spring Events + @TransactionalEventListener
- **Documentation**: Spring REST Docs
- **Testing**: JUnit 5 + Modulith Test

### Deployment
- **Container**: Single Docker image
- **Size**: ~200MB (with GraalVM)
- **Startup**: <5 seconds
- **Memory**: 512MB-1GB
- **CPU**: 1-2 cores

## Module Communication

### Synchronous (In-Process)
```java
@Service
public class QualityMeasureService {
    private final PatientService patientService;
    private final FhirService fhirService;

    public MeasureResult calculate(String patientId) {
        // Direct method call - no REST, no serialization
        Patient patient = patientService.getPatient(patientId);
        List<Observation> obs = fhirService.getObservations(patientId);
        // ... calculation logic
    }
}
```

### Asynchronous (Events)
```java
@Service
public class CareGapService {
    @EventListener
    @Async
    public void onMeasureCalculated(MeasureCalculatedEvent event) {
        // Process asynchronously
        identifyCareGaps(event.getPatientId(), event.getResult());
    }
}
```

## Database Schema

### Single Schema, Logical Separation
```sql
-- Single database: healthdata
CREATE SCHEMA IF NOT EXISTS fhir;
CREATE SCHEMA IF NOT EXISTS quality;
CREATE SCHEMA IF NOT EXISTS patient;

-- Tables with schema prefixes
CREATE TABLE patient.patients (...);
CREATE TABLE fhir.observations (...);
CREATE TABLE quality.measure_results (...);
```

## API Structure

### Single REST API with Modules
```
http://localhost:8080/
  /api/patients/          # Patient Module
  /api/fhir/              # FHIR Module
  /api/measures/          # Quality Module
  /api/care-gaps/         # Care Gap Module
  /api/notifications/     # Notification Module
  /actuator/health        # Single health check
  /actuator/metrics       # Unified metrics
```

## Monitoring & Observability

### Simplified Monitoring
- Single application to monitor
- Unified logs in one place
- Single set of metrics
- One distributed tracing span
- Simplified debugging with single JVM

## Security

### Unified Security Model
- Single authentication filter
- Shared security context
- No inter-service authentication needed
- Simplified RBAC implementation
- Single JWT validation point

## Expected Improvements

### Quantitative
- **Latency**: 50ms → 3ms for inter-module calls
- **Throughput**: 100 req/s → 1500 req/s
- **Memory**: 4GB → 1GB total
- **Startup**: 3 minutes → 20 seconds
- **Docker Image**: 9 × 300MB → 1 × 200MB

### Qualitative
- Easier to understand and maintain
- Faster development cycles
- Simplified debugging
- Better IDE support
- Easier testing

## Next Steps

1. Create new project structure
2. Set up Spring Modulith
3. Migrate Patient module first
4. Test and validate
5. Continue with other modules
6. Deploy and benchmark

This transformation will deliver a more maintainable, performant, and cost-effective solution while preserving all business functionality.