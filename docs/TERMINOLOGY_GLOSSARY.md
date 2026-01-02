# HDIM Terminology Glossary

**Purpose**: Single source of truth for all terminology used across documentation, GTM materials, and code.
**Last Updated**: December 30, 2025
**Version**: 1.0

---

## Quick Reference

| Metric | Official Value | Notes |
|--------|----------------|-------|
| Total Microservices | **28** | Excludes Gradle build directory |
| HEDIS Measures | **56** | Seeded in backend database |
| FHIR Specification | **FHIR R4** | Always include "R4" |
| PHI Cache TTL | **5 minutes** | Maximum, enforced at infrastructure level |
| Database Port (Dev) | **5435** | PostgreSQL |
| Redis Port (Dev) | **6380** | Cache |
| Kafka Port (Dev) | **9094** | Messaging |

---

## Service Names (Official)

### Core Clinical Services (7 services)

| Official Name | Directory Name | Port | Purpose |
|--------------|----------------|------|---------|
| Quality Measure Service | quality-measure-service | 8087 | HEDIS measure evaluation |
| CQL Engine Service | cql-engine-service | 8081 | Clinical Quality Language execution |
| Care Gap Service | care-gap-service | 8086 | Care gap detection and tracking |
| HCC Service | hcc-service | 8088 | Hierarchical Condition Category risk scoring |
| Prior Authorization Service | prior-auth-service | 8089 | Prior auth workflow management |
| SDOH Service | sdoh-service | 8090 | Social determinants of health |
| Consent Service | consent-service | 8091 | Patient consent management (HIPAA) |

### Integration Services (8 services)

| Official Name | Directory Name | Port | Purpose |
|--------------|----------------|------|---------|
| FHIR Service | fhir-service | 8085 | FHIR R4 resource management |
| Patient Service | patient-service | 8084 | Patient demographics and registry |
| EHR Connector Service | ehr-connector-service | 8092 | EHR integration (Epic, Cerner) |
| CDR Processor Service | cdr-processor-service | 8093 | Clinical data repository processing |
| ECR Service | ecr-service | 8094 | Electronic case reporting |
| Data Enrichment Service | data-enrichment-service | 8095 | Clinical data enrichment |
| Event Processing Service | event-processing-service | 8096 | Clinical event processing |
| Event Router Service | event-router-service | 8097 | Event routing and orchestration |

### Analytics Services (5 services)

| Official Name | Directory Name | Port | Purpose |
|--------------|----------------|------|---------|
| Analytics Service | analytics-service | 8098 | Quality measure reporting and dashboards |
| Predictive Analytics Service | predictive-analytics-service | 8099 | Risk prediction and ML models |
| QRDA Export Service | qrda-export-service | 8100 | QRDA I/III export generation |
| Payer Workflows Service | payer-workflows-service | 8101 | Payer-specific workflow automation |
| Migration Workflow Service | migration-workflow-service | 8102 | Data migration orchestration |

### Platform Services (5 services)

| Official Name | Directory Name | Port | Purpose |
|--------------|----------------|------|---------|
| Gateway Service | gateway-service | 8001 | Internal routing and service discovery |
| Notification Service | notification-service | 8103 | Email/SMS notifications |
| Approval Service | approval-service | 8104 | Workflow approvals |
| Documentation Service | documentation-service | 8105 | Documentation generation |
| AI Assistant Service | ai-assistant-service | 8106 | AI-powered clinical assistance |

### Business Services (3 services)

| Official Name | Directory Name | Port | Purpose |
|--------------|----------------|------|---------|
| Sales Automation Service | sales-automation-service | 8107 | CRM and sales pipeline |
| Agent Builder Service | agent-builder-service | 8108 | AI agent configuration |
| Agent Runtime Service | agent-runtime-service | 8109 | AI agent execution |

---

## Terms to NEVER Use

| Wrong Term | Correct Term | Why |
|------------|--------------|-----|
| 27 microservices | 28 microservices | Outdated count |
| 29 microservices | 28 microservices | Includes build directory |
| 82 HEDIS measures | 56 HEDIS measures | Outdated; current seeded count is 56 |
| FHIR compliant | FHIR R4 compliant | Too vague; specify version |
| AI-powered | CQL-native execution | Most features are deterministic, not ML |
| Real-time | Sub-200ms / 5-minute cache | Specify actual latency |
| Industry-leading | Specific benchmark | Unsubstantiated claim |
| FHIR Mock Service | FHIR Service | Internal test name leaked |
| QM Service | Quality Measure Service | Use full name |

---

## Technical Terms

### Healthcare Interoperability

| Term | Definition | When to Use | Example |
|------|------------|-------------|---------|
| **FHIR R4** | FHIR Release 4 specification (HL7) | Always include "R4" for precision | "FHIR R4 compliant" |
| **FHIR-native** | No FHIR translation layer; resources used directly | Architecture differentiator | "FHIR-native architecture (no v2->FHIR translation)" |
| **CQL** | Clinical Quality Language | Measure logic description | "CQL-based measure execution" |
| **CQL-native** | Direct CQL execution without proprietary translation | Positioning vs competitors | "CQL-native execution engine" |
| **HEDIS** | Healthcare Effectiveness Data and Information Set | Quality measure context | "56 HEDIS quality measures" |
| **QRDA** | Quality Reporting Document Architecture | CMS reporting format | "QRDA III export" |
| **HCC** | Hierarchical Condition Category | Risk adjustment | "HCC v28 risk stratification" |

### Security & Compliance

| Term | Definition | When to Use | Example |
|------|------------|-------------|---------|
| **PHI** | Protected Health Information | Legal/compliance contexts | Always capitalize as "PHI" |
| **BAA** | Business Associate Agreement | HIPAA compliance | "BAA required before PHI ingestion" |
| **PHI Cache TTL** | Time-to-live for cached PHI | HIPAA compliance | "5-minute PHI cache TTL" |
| **Gateway Trust** | Authentication pattern where services trust gateway headers | Architecture | "Gateway Trust authentication model" |
| **Multi-tenant** | Data isolation by tenant ID | Architecture | "Multi-tenant isolation" |

### Architecture

| Term | Definition | When to Use | Example |
|------|------------|-------------|---------|
| **Microservices** | Independent, deployable services | Architecture description | "28 microservices architecture" |
| **Event-driven** | Asynchronous communication via events | Kafka integration | "Event-driven via Apache Kafka" |
| **HAPI FHIR** | Open-source FHIR implementation | Technology stack | "HAPI FHIR 7.x" |

---

## Database Configuration

### Development Environment

| Component | Value | Notes |
|-----------|-------|-------|
| Database Name | healthdata_qm | PostgreSQL |
| Database Port | 5435 | Non-standard to avoid conflicts |
| Database User | healthdata | Service account |
| Redis Port | 6380 | Non-standard to avoid conflicts |
| Kafka Port | 9094 | External listener |

### Production Environment

| Component | Value | Notes |
|-----------|-------|-------|
| Database Name | healthdata_qm_prod | PostgreSQL |
| Database Port | 5432 | Standard PostgreSQL port |
| Redis Port | 6379 | Standard Redis port |
| Kafka Port | 9092 | Standard Kafka port |

---

## Infrastructure Ports

### External (Client-Facing)

| Service | Port | Protocol |
|---------|------|----------|
| Kong API Gateway | 8000 | HTTPS |
| Clinical Portal (Angular) | 4200 | HTTPS |
| Admin Portal (Angular) | 4201 | HTTPS |
| Grafana | 3001 | HTTP |
| Prometheus | 9090 | HTTP |

### Internal (Service-to-Service)

| Service | Port | Notes |
|---------|------|-------|
| Gateway Service | 8001 | Internal routing |
| CQL Engine Service | 8081 | CQL execution |
| Patient Service | 8084 | Patient data |
| FHIR Service | 8085 | FHIR R4 resources |
| Care Gap Service | 8086 | Care gap detection |
| Quality Measure Service | 8087 | HEDIS evaluation |

---

## Technology Stack Versions

| Technology | Version | Notes |
|------------|---------|-------|
| Java | 21 LTS | Required |
| Spring Boot | 3.x | Required |
| HAPI FHIR | 7.x | FHIR R4 certified |
| PostgreSQL | 15 | Database |
| Redis | 7 | Cache |
| Apache Kafka | 3.x | Messaging |
| Kong | Latest | API Gateway |
| Angular | 17+ | Frontend |
| Docker | 24.0+ | Containers |
| Gradle | 8.11+ | Build (Kotlin DSL) |

---

## Messaging Topics (Kafka)

| Topic | Producer | Consumer | Payload |
|-------|----------|----------|---------|
| patient.events | Patient Service | Care Gap, Analytics | Patient CRUD events |
| measure.evaluation.complete | Quality Measure | Care Gap, Analytics | Evaluation results |
| care-gap.detected | Care Gap Service | Notification | Gap identification |
| audit.events | All services | Audit Service | PHI access logs |
| notification.requests | Various | Notification Service | Email/SMS requests |

---

## Performance Benchmarks

| Metric | Target | Actual (Dec 2024) |
|--------|--------|-------------------|
| FHIR Query Latency (p95) | <200ms | 180ms |
| CQL Execution Time (avg) | <600ms/patient/measure | 500ms |
| Throughput | 150+ evaluations/sec/tenant | 220 evaluations/sec |
| Cache Hit Rate | >80% | 87% |
| API Gateway Auth Overhead | <20ms | <10ms |
| System Uptime | 99.9% | 99.94% |

---

## Compliance References

| Regulation | Section | HDIM Control |
|------------|---------|--------------|
| HIPAA | 45 CFR 164.312 | Technical safeguards |
| HIPAA | 45 CFR 164.502 | Minimum necessary (5-min cache) |
| HIPAA | 45 CFR 164.530 | Administrative requirements |
| CMS | Star Ratings | HEDIS measure submission |
| HL7 | FHIR R4 | Interoperability standard |
| NCQA | HEDIS MY2024 | Quality measure specifications |

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-30 | Documentation Team | Initial creation |

---

*This glossary is the authoritative source for terminology. All documentation, GTM materials, and code comments should reference this document for official terms.*
