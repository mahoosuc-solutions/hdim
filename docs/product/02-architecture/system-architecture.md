---
id: "product-system-architecture"
title: "System Architecture Overview"
portalType: "product"
path: "product/02-architecture/system-architecture.md"
category: "architecture"
subcategory: "system-design"
tags: ["architecture", "system-design", "infrastructure", "microservices", "modular-monolith", "cloud-native"]
summary: "Complete technical architecture of HealthData in Motion platform. Describes modular monolith design, service boundaries, data flows, integration patterns, and scalability approach for quality measure evaluation and care gap management at enterprise scale."
estimatedReadTime: 18
difficulty: "intermediate"
targetAudience: ["cio", "architect", "technical-lead"]
owner: "Product Architecture"
reviewCycle: "quarterly"
status: "published"
version: "1.0"
seoKeywords: ["system architecture", "microservices", "modular monolith", "healthcare IT", "FHIR integration", "cloud infrastructure"]
relatedDocuments: ["product-core-capabilities", "integration-patterns", "data-model-specification"]
lastUpdated: "2025-12-01"
---

# System Architecture Overview

## Executive Summary

HealthData in Motion employs a **modular monolith architecture** optimized for healthcare quality measure evaluation and care gap detection. This design provides service-oriented organization within a single, horizontally-scalable deployment, enabling rapid feature development while maintaining enterprise reliability and HIPAA compliance.

**Key Architectural Principles**:
- Modular service boundaries with clear contracts
- Event-driven data flows for real-time insights
- FHIR-native data representation
- Horizontal scalability at the container level
- Zero single points of failure

## Core Architecture Components

### 1. API Gateway Layer
**Technology**: Kong 3.x
**Purpose**: Single entry point for all client requests

**Responsibilities**:
- SSL/TLS termination and HTTPS enforcement
- JWT token validation and refresh
- Rate limiting (API quotas per customer tier)
- Request routing to appropriate services
- Audit logging of all API calls
- CORS policy enforcement

**Configuration**:
- 2 independent Kong instances (active-active)
- PostgreSQL backend for configuration
- Redis for request tracking and rate limiting
- Average latency: <50ms p99

### 2. Service Modules (Modular Monolith)

The application is organized into logical service modules within a single Spring Boot application:

#### **FHIR Service Module**
Manages patient demographics, encounters, observations, conditions with HL7 FHIR R4 standard compliance.

**Key Responsibilities**:
- Patient demographic data management
- Clinical observations and results
- Condition/diagnosis tracking
- Medication and procedure records
- Encounter documentation

**Database**: PostgreSQL with JSONB columns for flexible FHIR resource storage

**Key Endpoints**:
- `GET /fhir/Patient/{id}` - Retrieve patient
- `GET /fhir/Observation?patient={id}&date={range}` - Patient observations
- `GET /fhir/Condition?patient={id}` - Patient conditions
- `POST /fhir/Bundle` - Batch submissions

#### **Quality Measure Service Module**
Evaluates quality measures using CQL (Clinical Quality Language) with real-time performance tracking.

**Key Workflows**:
1. Load measure definition (FHIR MeasureDefinition)
2. Query patient data from FHIR module
3. Execute CQL logic against patient data
4. Calculate individual and population metrics
5. Publish results via Kafka event stream

**Features**:
- 50+ pre-built HEDIS quality measures
- Custom measure definition support
- WebSocket streaming for real-time results
- Batch and on-demand evaluation modes

#### **Care Gap Service Module**
Identifies patients with unmet quality measure criteria and manages gap prioritization.

**Care Gap Lifecycle**:
1. Patient Data Analysis
2. CQL Evaluation
3. Gap Detection
4. Prioritization by urgency/ROI
5. Outreach Assignment
6. Gap Closure Tracking
7. Auto-Closure Validation

**Features**:
- Intelligent gap prioritization algorithm
- Automatic gap closure when criteria met
- Patient-level and practice-level dashboards
- Integration with EHR workflows

#### **Patient Service Module**
Maintains patient master data and deduplication logic.

**Key Functions**:
- Patient demographics management
- Patient-provider relationship tracking
- Probabilistic matching for deduplication
- Patient search and filtering

#### **Event Processing Service Module**
Consumes and transforms clinical events from external systems.

**Event Types Handled**:
- PatientAdmitted, PatientDischarged
- ObservationRecorded, ResultAbnormal
- MedicationPrescribed, LabResultReceived
- CareGapDetected, GapClosure

**Features**:
- Event sourcing for audit trail
- Dead-letter queue for failed processing
- Automatic retry with exponential backoff
- Event schema validation

### 3. Data Layer

**Primary Database**: PostgreSQL 14+

**Schemas**:
- `fhir_data` - FHIR resources and clinical data
- `quality_measures` - Measure definitions, results, calculations
- `care_gaps` - Gap tracking and history
- `patients` - Merged patient records and demographics
- `events` - Immutable event log for audit
- `documentation` - Portal content and metadata

**Performance Features**:
- Composite indexes for patient + date range queries
- GIN indexes for JSONB fast search
- Partitioning by date for large tables
- Connection pooling via HikariCP

### 4. Message Queue Layer

**Technology**: Apache Kafka 3.x
**Purpose**: Asynchronous event processing and service decoupling

**Topics**:
- `fhir-events` - Inbound clinical data changes
- `domain-events` - Internal system events
- `care-gap-events` - Gap detection and closure
- `notification-events` - Outreach triggers
- `measure-results` - Quality measure evaluations
- `analytics-events` - Usage and engagement tracking

**Configuration**:
- 3-node cluster (minimum for HA)
- Replication factor: 3
- Retention: 7 days for transient, 30+ days for analytics
- Partitioning by patient ID for ordering

### 5. Caching Layer

**Technology**: Redis 7.x
**Purpose**: Reduce database load and improve response times

**Cache Types**:
- Patient data cache (1-hour TTL, LRU eviction)
- Measure definition cache (24-hour TTL)
- User sessions and JWT tokens
- Complex query result cache (4-hour TTL)
- Rate limit counters

**Performance**:
- Average hit rate: 87% for patient queries
- Master-replica setup with sentinel monitoring
- Automatic failover <5 seconds

### 6. Search Engine

**Technology**: Elasticsearch 8.x
**Purpose**: Full-text search and analytics

**Indices**:
- `documents` - Documentation content and metadata
- `search_queries` - Search analytics
- `logs` - Application logs (30-day retention)
- `metrics` - Performance metrics

**Features**:
- Real-time indexing
- Synonym mappings
- Fuzzy matching and typo tolerance
- Faceted search support

## Data Flow Patterns

### Real-Time Data Ingest
```
EHR System → FHIR API → PostgreSQL →
Kafka Events → Quality Measures → Care Gaps →
Notifications → Healthcare Providers
```

**Latency Targets**:
- Data ingest to database: <100ms
- Event processing: <500ms
- Measure evaluation: <2s per patient
- Care gap detection: <5s
- End-to-end: <10 seconds

### Batch Processing
```
Scheduler → Load Measures → Query FHIR Data →
Execute CQL (16 threads) → Store Results →
Update Dashboards → Generate Notifications
```

**Configuration**:
- Default batch: All patients nightly (2 AM UTC)
- Duration: 15-45 minutes
- Throughput: 500K patient evaluations per hour

## Scalability & Performance

### Horizontal Scaling
- **Stateless Application**: Can scale to 10+ instances
- **Load Balancer**: HAProxy distributes traffic
- **Database Read Replicas**: For analytics queries
- **Message Queue Partitioning**: By patient ID

### Performance Characteristics (p95)
- Patient lookup: 50ms
- Measure evaluation: 2s
- Care gap detection: 5s
- Search: 200ms
- API: <100ms

### Throughput Capacity
- 1,000 concurrent users per instance
- 10,000+ API requests per second at peak
- 500K patient evaluations per hour batch

## High Availability & Disaster Recovery

### Availability Targets
- **Uptime SLA**: 99.9% (8.76 hours downtime/year)
- **RTO**: 15 minutes
- **RPO**: 5 minutes

### Redundancy Strategy
- Minimum 2 application instances
- Primary-standby database replication
- 3-node Kafka cluster with replication
- 2 independent Kong gateway instances

### Backup Strategy
- Continuous replication to standby server
- Daily snapshots to S3
- 30-day retention policy
- Monthly recovery testing

## Security Architecture

### Network Security
- VPC with private subnets
- Security groups for traffic control
- WAF at API Gateway
- DDoS protection (AWS Shield)

### Data Encryption
- **In Transit**: TLS 1.2+
- **At Rest**: AES-256 encryption
- **Database**: Native PostgreSQL encryption
- **Backups**: S3 KMS encryption

### Access Control
- OAuth 2.0 / OIDC single sign-on
- Role-based access control (5 roles)
- Multi-factor authentication for admins
- Comprehensive audit logging

### Compliance
- HIPAA Business Associate Agreement
- HITRUST CSF certification
- SOC 2 Type II compliance
- Regular penetration testing

## Monitoring & Observability

### Metrics Collection
- **Prometheus**: Application metrics
- **Grafana**: Visualization and dashboards
- **Key Metrics**: Response times, database performance, queue lag, cache hit rates, error rates

### Logging
- **ELK Stack**: Log aggregation
- **Retention**: 30 days
- **Key Logs**: Errors, API audit trail, slow queries, security events

### Alerting
- **Critical**: Response time >5s, error rate >1%, disk <10%
- **Kafka**: Topic lag >5 minutes
- **Database**: CPU >80%

## Deployment Architecture

### Containerization
- **Docker**: All services containerized (~400MB image)
- **Security Scanning**: Trivy vulnerability scanning
- **Registry**: AWS ECR

### Orchestration
- **Kubernetes/EKS**: Container orchestration
- **Namespaces**: Production, staging, development
- **Resource Limits**: 4GB memory, 2 cores per pod

### CI/CD Pipeline
- **Source**: Git (GitHub)
- **Build**: Maven (15 minutes)
- **Testing**: Unit + integration (20 minutes)
- **Deployment**: <5 minutes rolling update

### Infrastructure as Code
- **Terraform**: AWS provisioning
- **CloudFormation**: Kubernetes manifests
- **Version Control**: All changes in Git

## Technology Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| API Gateway | Kong | 3.x | Routing, auth, rate limiting |
| Application | Spring Boot | 3.2+ | Core logic |
| Language | Java | 21 LTS | Type-safe, JVM ecosystem |
| Database | PostgreSQL | 14+ | Relational data, FHIR storage |
| Message Queue | Kafka | 3.x | Event streaming |
| Cache | Redis | 7.x | Session, query caching |
| Search | Elasticsearch | 8.x | Full-text search |
| Container | Docker | Latest | Containerization |
| Orchestration | Kubernetes/EKS | Latest | Container orchestration |
| Monitoring | Prometheus + Grafana | Latest | Metrics & visualization |
| Logging | ELK Stack | Latest | Log aggregation |

## Architectural Decisions

### Modular Monolith vs. Microservices
**Chosen**: Modular monolith
**Rationale**: Simpler operations than microservices, easier debugging, shared transactions where needed, can evolve later.

### FHIR as Primary Data Model
**Chosen**: FHIR R4 compliance
**Rationale**: Interoperability with EHRs, national standards, reduces vendor lock-in, enables health information exchange.

### Event-Driven Architecture
**Chosen**: Kafka for async processing
**Rationale**: Decouples services, supports real-time and batch, built-in failure recovery, scales to high throughput.

## Scalability Headroom

Current architecture supports:
- 100M patient records (with partitioning)
- 1M concurrent users
- 100K API requests/second
- Processing 500K patients/hour in batch

## Future Evolution Roadmap

**2026**:
- Multi-region deployment for geographic redundancy
- GraphQL API layer for improved client efficiency
- Machine learning integration for predictive care gaps
- Mobile applications (iOS/Android)

**2027**:
- Advanced analytics with BigQuery integration
- Real-time data warehouse
- AI-powered care optimization

---

## Conclusion

The modular monolith architecture balances operational simplicity with enterprise scalability and healthcare compliance. Event-driven design enables real-time insights while maintaining consistency, and FHIR-native approach ensures interoperability with the broader healthcare ecosystem.

This architecture has been validated in production with 5M+ patients and 100K+ daily clinical events.

**Next Steps**:
- See [Integration Patterns](integration-patterns.md) for implementation details
- Review [Data Model Specification](data-model-specification.md) for complete schema
- Consult [Performance Benchmarks](performance-benchmarks.md) for capacity planning
