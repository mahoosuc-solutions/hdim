# Health Data In Motion - Architecture Documentation

## Overview

Health Data In Motion is a best-in-class enterprise healthcare information exchange platform following modern microservices architecture patterns with Java 21 backend and Angular frontend.

## Architecture Principles

### 1. **Monorepo Organization**
- Single repository for all code (backend + frontend)
- Nx for frontend orchestration
- Gradle multi-module for backend organization
- Shared dependencies and build tools

### 2. **Microservices Architecture**
- 9 independent, deployable services
- Domain-driven design boundaries
- Event-driven communication via Kafka
- API Gateway for external access

### 3. **Healthcare Standards Compliance**
- FHIR R4 for data exchange
- HEDIS for quality measures
- CQL for clinical logic
- HIPAA for privacy and security

### 4. **Cloud-Native Design**
- Containerized with Docker
- Kubernetes-ready
- Horizontal scalability
- Infrastructure as Code

## System Components

### Backend Services (Java 21 + Spring Boot)

#### 1. FHIR Service (Port 8085)
**Purpose**: Manage FHIR R4 resources (Patient, Observation, Condition, etc.)

**Responsibilities**:
- CRUD operations for 150+ FHIR resource types
- FHIR search parameter support
- Bundle operations (transaction, batch, searchset)
- Resource validation
- HIPAA audit logging

**Dependencies**:
- PostgreSQL (healthdata_fhir database)
- Redis (resource caching)
- Kafka (resource change events)

**Key Technologies**:
- HAPI FHIR 7.6.0
- Spring Boot 3.3.5
- PostgreSQL 15

#### 2. CQL Engine Service (Port 8084)
**Purpose**: Evaluate clinical quality measures using CQL

**Responsibilities**:
- Execute CQL expressions
- Evaluate 52 HEDIS measures
- Calculate STAR ratings
- Measure reporting

**Dependencies**:
- FHIR Service (data retrieval)
- PostgreSQL (measure definitions)
- Redis (measure caching)

**Key Technologies**:
- OpenCDS CQL Engine
- CQL Evaluator
- HAPI FHIR integration

#### 3. Consent Service (Port 8090)
**Purpose**: Manage patient consent and data access policies

**Responsibilities**:
- HIPAA 42 CFR Part 2 compliance
- GDPR consent management
- Field-level consent enforcement
- RBAC (13 roles, 31 permissions)
- Consent audit trail

**Dependencies**:
- PostgreSQL (consent policies)
- Kafka (consent change events)

**Key Technologies**:
- Spring Security 6.3.4
- Custom consent engine
- JWT authentication

#### 4. Event Processing Service (Port 8081)
**Purpose**: Real-time event processing and care gap detection

**Responsibilities**:
- Consume Kafka events
- Care gap detection (<5 seconds)
- Quality measure triggers
- Alert generation
- Webhook notifications

**Dependencies**:
- Kafka (event streaming)
- FHIR Service (data access)
- CQL Engine (measure evaluation)

**Key Technologies**:
- Spring Kafka
- Custom event processing engine

#### 5-9. Additional Services
- **Patient Service**: Patient demographics and enrollment
- **Quality Measure Service**: Quality measure orchestration
- **Care Gap Service**: Care gap closure workflows
- **Analytics Service**: Data analytics and reporting
- **Gateway Service**: Kong API Gateway configuration

### Shared Modules

#### Domain Modules
- **fhir-models**: FHIR R4 resource models and utilities
- **hedis-models**: HEDIS measure definitions
- **cql-models**: CQL expression models
- **common**: Shared domain logic and value objects

#### Infrastructure Modules
- **security**: HIPAA security, JWT, encryption
- **audit**: HIPAA-compliant audit logging
- **messaging**: Kafka abstractions and event schemas
- **cache**: Redis abstractions and caching strategies
- **persistence**: JPA base classes and database utilities

#### API Contracts
- **fhir-api**: OpenAPI 3.0 specs for FHIR endpoints
- **cql-api**: CQL evaluation API contracts
- **consent-api**: Consent management API contracts
- **events**: AsyncAPI schemas for Kafka events

### Frontend (Angular + Nx)

#### Admin Portal Application
- Service health monitoring
- API playground with 20+ quick actions
- System diagnostics
- Service management UIs
- Integration examples

#### Shared Libraries
- **UI Components**: Reusable Angular components
- **Data Access**: HTTP services and state management
- **Models**: TypeScript interfaces matching backend DTOs
- **Utils**: HIPAA utilities, validators, formatters

#### Feature Libraries
- FHIR resource management
- CQL measure evaluation
- Consent management
- Care gap workflows
- Quality measure dashboards

## Data Architecture

### Database Schema Organization

#### PostgreSQL Databases (Port 5435)
1. **healthdata_fhir**: FHIR resources
   - patient, observation, condition, medication_request tables
   - FHIR resource metadata
   - Audit tables

2. **healthdata_cql**: CQL engine data
   - measure_definitions
   - measure_results
   - cql_libraries

3. **healthdata_consent**: Consent policies
   - consent_policies
   - access_rules
   - rbac_roles_permissions

4. **healthdata_events**: Event processing
   - care_gaps
   - quality_triggers
   - event_audit

### Caching Strategy (Redis)

**Cache Layers**:
1. **L1 Cache**: JVM-level (Caffeine)
2. **L2 Cache**: Redis (distributed)

**Cached Data**:
- FHIR resources (TTL: 1 hour)
- CQL measure results (TTL: 24 hours)
- User sessions (TTL: 4 hours)
- API responses (TTL: 5 minutes)

**Cache Patterns**:
- Cache-Aside for FHIR resources
- Write-Through for user sessions
- Cache invalidation via Kafka events

### Event Streaming (Kafka)

**Topics**:
1. `fhir.resources.patient` - Patient resource changes
2. `fhir.resources.observation` - Observation changes
3. `cql.measure.evaluation` - Measure evaluation results
4. `consent.policy.changes` - Consent policy updates
5. `caregap.detected` - Care gaps detected
6. `quality.alert` - Quality measure alerts
7. `audit.events` - Audit trail events
8. `system.events` - System-level events

**Partitioning Strategy**:
- 3 partitions per topic (24 total)
- Partition by tenant/organization ID
- Enables parallel processing

## Security Architecture

### Authentication & Authorization

**JWT Authentication**:
```
Client → Gateway → Consent Service (validate JWT)
                 ↓
              Extract Claims (user ID, roles, permissions)
                 ↓
              Downstream Services (propagate security context)
```

**RBAC Model**:
- 13 Roles: Admin, Clinician, Nurse, Patient, etc.
- 31 Permissions: read:fhir, write:fhir, evaluate:cql, etc.
- Role-Permission matrix stored in consent service

### HIPAA Compliance

**Data Encryption**:
- **At Rest**: PostgreSQL transparent data encryption (TDE)
- **In Transit**: TLS 1.3 for all inter-service communication
- **PHI Fields**: Additional field-level encryption using AES-256

**Audit Logging**:
- Every PHI access logged
- 7-year retention (HIPAA requirement)
- Immutable audit trail (append-only)
- Logs include: who, what, when, why (purpose of access)

**Minimum Necessary Rule**:
- Field-level consent filtering
- Only return data user is authorized to see
- Implemented in FHIR Service + Consent Service

### Network Security

**Service Mesh** (Future):
- Istio for mutual TLS
- Service-to-service authentication
- Traffic encryption

**API Gateway** (Kong):
- Rate limiting (100 req/min per user)
- IP whitelisting (configurable)
- Request validation
- Response filtering

## Integration Patterns

### API Integration

**RESTful APIs**:
- OpenAPI 3.0 specifications
- Standard HTTP methods (GET, POST, PUT, DELETE)
- FHIR-compliant search parameters
- JSON payloads

**Example FHIR Search**:
```
GET /fhir/Patient?name=Smith&birthdate=1990-01-01&_count=20
Authorization: Bearer <jwt-token>
```

### Event-Driven Integration

**Event Publishing**:
```java
@Service
public class PatientService {
    @Autowired
    private KafkaTemplate<String, PatientEvent> kafka;

    public void createPatient(Patient patient) {
        // Save to database
        repository.save(patient);

        // Publish event
        kafka.send("fhir.resources.patient",
            new PatientEvent("CREATED", patient));
    }
}
```

**Event Consumption**:
```java
@Service
public class CareGapService {
    @KafkaListener(topics = "fhir.resources.observation")
    public void handleObservation(ObservationEvent event) {
        // Evaluate if new observation closes care gaps
        evaluateCareGaps(event.getPatientId());
    }
}
```

## Performance Characteristics

### Scalability

**Horizontal Scaling**:
- All services are stateless (except sessions in Redis)
- Can scale independently based on load
- Kubernetes HPA (Horizontal Pod Autoscaler) ready

**Load Balancing**:
- Kong Gateway: Round-robin
- Kubernetes Service: IP hash (sticky sessions)

### Caching Performance

**Cache Hit Rates**:
- FHIR Resources: 67% (measured)
- CQL Results: 85% (measured)
- User Sessions: 98% (measured)

**Response Time Improvements**:
- Cache hit: <10ms
- Cache miss + DB: ~150ms
- 15x faster with cache

### Database Performance

**Connection Pooling**:
- HikariCP with 10 max connections per service
- Connection timeout: 30 seconds
- Idle timeout: 10 minutes

**Query Optimization**:
- Indexes on all foreign keys
- Composite indexes for common searches
- Query execution plan analysis

## Deployment Architecture

### Local Development

```
Developer Machine
├── Docker Compose
│   ├── PostgreSQL (5435)
│   ├── Redis (6381)
│   ├── Kafka (9092)
│   └── Kafka UI (8086)
├── Backend Services (Gradle bootRun)
│   ├── FHIR Service (8085)
│   ├── CQL Engine (8084)
│   └── Other services
└── Frontend (Nx serve)
    └── Admin Portal (4200)
```

### Production (Future Kubernetes)

```
Kubernetes Cluster
├── Ingress Controller (NGINX)
│   └── TLS Termination
├── Namespaces
│   ├── healthdata-prod
│   │   ├── FHIR Service (3 replicas)
│   │   ├── CQL Engine (2 replicas)
│   │   ├── Consent Service (2 replicas)
│   │   └── Other services
│   └── healthdata-infra
│       ├── PostgreSQL (StatefulSet)
│       ├── Redis (StatefulSet)
│       └── Kafka (StatefulSet)
└── Monitoring
    ├── Prometheus
    ├── Grafana
    └── Jaeger (distributed tracing)
```

## Disaster Recovery

### Backup Strategy

**PostgreSQL**:
- Daily full backups (retained 30 days)
- Continuous WAL archiving
- Point-in-time recovery capability
- Cross-region replication

**Redis**:
- RDB snapshots every 6 hours
- AOF (Append-Only File) enabled
- Automatic failover with Redis Sentinel

**Kafka**:
- Topic replication factor: 3
- 168-hour (7-day) retention
- Cross-cluster mirroring

### High Availability

**Target SLAs**:
- Availability: 99.9% (8.76 hours downtime/year)
- RTO (Recovery Time Objective): 1 hour
- RPO (Recovery Point Objective): 15 minutes

**HA Architecture** (Future):
- Multi-AZ deployment
- Auto-scaling based on metrics
- Health checks and auto-restart
- Circuit breakers for graceful degradation

## Monitoring & Observability

### Metrics (Prometheus)

**Application Metrics**:
- Request rate, latency, error rate (RED metrics)
- JVM metrics (heap, GC, threads)
- Database connection pool utilization
- Cache hit/miss rates

**Business Metrics**:
- FHIR resources created/updated per minute
- CQL measure evaluations per hour
- Care gaps detected per day
- Active user sessions

### Logging

**Structured Logging** (JSON):
```json
{
  "timestamp": "2025-01-15T10:30:45.123Z",
  "level": "INFO",
  "service": "fhir-service",
  "traceId": "abc123",
  "message": "Patient created",
  "userId": "user-456",
  "patientId": "patient-789"
}
```

**Log Aggregation** (Future):
- ELK Stack (Elasticsearch, Logstash, Kibana)
- Centralized log storage
- Full-text search capabilities

### Distributed Tracing (Future)

**OpenTelemetry + Jaeger**:
- Trace requests across services
- Identify performance bottlenecks
- Understand service dependencies

## Technology Decisions

### Why Java 21?
- Virtual threads for better concurrency
- Pattern matching for cleaner code
- Record types for immutable DTOs
- Long-term support (LTS)

### Why Spring Boot 3.3?
- Native support for Java 21
- Improved observability
- Spring Security 6.x
- Jakarta EE migration complete

### Why Gradle over Maven?
- Faster builds (incremental compilation)
- Better dependency management
- Kotlin DSL for type safety
- More flexible build logic

### Why Nx for Frontend?
- Best-in-class monorepo tool for Angular
- Smart build caching (30-70% faster)
- Dependency graph visualization
- Excellent DX (developer experience)

### Why PostgreSQL?
- ACID compliance critical for healthcare
- Excellent JSON support (for FHIR resources)
- Mature, stable, well-documented
- Strong open-source community

### Why Kafka?
- High throughput, low latency
- Durable message storage
- Horizontal scalability
- Strong ecosystem (Kafka Connect, Streams)

## Future Enhancements

### Phase 2: Advanced Features
- [ ] Real-time collaboration (WebSockets)
- [ ] GraphQL API for flexible querying
- [ ] Machine learning care gap predictions
- [ ] Advanced analytics dashboards

### Phase 3: Platform Expansion
- [ ] Patient portal application
- [ ] Provider portal application
- [ ] Mobile apps (iOS/Android)
- [ ] HL7 v2 integration
- [ ] X12 claims integration

### Phase 4: Enterprise Features
- [ ] Multi-tenancy support
- [ ] White-labeling
- [ ] Custom reporting engine
- [ ] ETL pipelines for data warehousing
- [ ] Advanced FHIR profiles (US Core, USCDI)

## References

- [FHIR R4 Specification](https://hl7.org/fhir/R4/)
- [HEDIS Measures](https://www.ncqa.org/hedis/)
- [CQL Specification](https://cql.hl7.org/)
- [HIPAA Compliance](https://www.hhs.gov/hipaa/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Nx Documentation](https://nx.dev/)

---

**Document Version**: 1.0
**Last Updated**: 2025-01-15
**Author**: Health Data In Motion Team
