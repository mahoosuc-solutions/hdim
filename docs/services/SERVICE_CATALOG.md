# HDIM Service Catalog

Central index of all 57 microservices in the HDIM platform. Use this to find a specific service and access its documentation.

**Last Updated**: March 3, 2026
**Total Services**: 57 microservices (including event sourcing, AI agents, gateways, and sales services)
**Coverage**: 100% documented with architecture patterns

---

## Quick Search

Use Ctrl+F (Cmd+F) to search for:
- Service name (e.g., "quality-measure")
- Port number (e.g., "8087")
- Purpose/keyword (e.g., "HEDIS", "patient", "FHIR")

---

## Core Clinical Services

These are the foundation services that handle clinical data and quality measures.

| Service | Port | Purpose | Tech Stack | Documentation |
|---------|------|---------|-----------|-----------------|
| **quality-measure-service** | 8087 | HEDIS measures, quality calculations, CQL evaluation | PostgreSQL, Redis, Kafka | [README](../../backend/modules/services/quality-measure-service/README.md) |
| **patient-service** | 8084 | Patient demographics, registries, care team | PostgreSQL, Redis | [README](../../backend/modules/services/patient-service/README.md) |
| **cql-engine-service** | 8081 | CQL expression evaluation, logic processing | PostgreSQL | [README](../../backend/modules/services/cql-engine-service/README.md) |
| **fhir-service** | 8085 | FHIR R4 resource management, clinical documents | PostgreSQL, Redis | [README](../../backend/modules/services/fhir-service/README.md) |
| **care-gap-service** | 8086 | Care gap detection, closure recommendations | PostgreSQL, Kafka | [README](../../backend/modules/services/care-gap-service/README.md) |
| **consent-service** | 8082 | Patient consent management, privacy controls | PostgreSQL | [README](../../backend/modules/services/consent-service/README.md) |
| **clinical-workflow-service** | 8093 | Clinical workflow orchestration, care coordination | PostgreSQL, Kafka, WebSocket | [README](../../backend/modules/services/clinical-workflow-service/README.md) |
| **nurse-workflow-service** | TBD | Nurse-specific workflows, task management | PostgreSQL | [README](../../backend/modules/services/nurse-workflow-service/README.md) |
| **event-processing-service** | TBD | Event processing pipeline, stream handling | Kafka, PostgreSQL | [README](../../backend/modules/services/event-processing-service/README.md) |

---

## Analytics & Reporting Services

Quality reporting, data analysis, and business intelligence.

| Service | Port | Purpose | Tech Stack | Documentation |
|---------|------|---------|-----------|-----------------|
| **analytics-service** | TBD | Quality reporting, dashboards, KPI calculation | PostgreSQL, Elasticsearch | [README](../../backend/modules/services/analytics-service/README.md) |
| **qrda-export-service** | TBD | QRDA I/III export for CMS submission | PostgreSQL | [README](../../backend/modules/services/qrda-export-service/README.md) |
| **predictive-analytics-service** | TBD | Predictive modeling, ML-based forecasting | PostgreSQL, TensorFlow | [README](../../backend/modules/services/predictive-analytics-service/README.md) |

---

## Risk & Cost Management Services

HCC coding, cost monitoring, risk stratification.

| Service | Port | Purpose | Tech Stack | Documentation |
|---------|------|---------|-----------|-----------------|
| **hcc-service** | TBD | HCC risk adjustment, coding optimization | PostgreSQL, ML models | [README](../../backend/modules/services/hcc-service/README.md) |
| **cost-analysis-service** | TBD | Cost tracking, budget management, financial analysis | PostgreSQL | [README](../../backend/modules/services/cost-analysis-service/README.md) |

---

## Workflow & Authorization Services

Prior authorization, approval workflows, transitions of care.

| Service | Port | Purpose | Tech Stack | Documentation |
|---------|------|---------|-----------|-----------------|
| **prior-auth-service** | TBD | Prior authorization management, appeal tracking | PostgreSQL | [README](../../backend/modules/services/prior-auth-service/README.md) |
| **approval-service** | TBD | Workflow approval engine, task management | PostgreSQL, Kafka | [README](../../backend/modules/services/approval-service/README.md) |
| **payer-workflows-service** | TBD | Payer-specific workflows, business process orchestration | PostgreSQL, Kafka | [README](../../backend/modules/services/payer-workflows-service/README.md) |
| **migration-workflow-service** | TBD | Patient migration workflows between payers | PostgreSQL, Kafka | [README](../../backend/modules/services/migration-workflow-service/README.md) |

---

## Integration & Data Services

External system integrations, data enrichment, health information exchange.

| Service | Port | Purpose | Tech Stack | Documentation |
|---------|------|---------|-----------|-----------------|
| **ehr-connector-service** | TBD | Electronic Health Record integration, HL7 v2/v3 | PostgreSQL, Kafka | [README](../../backend/modules/services/ehr-connector-service/README.md) |
| **cms-connector-service** | TBD | CMS BCDA/DPC integration, bulk data export | PostgreSQL, Kafka | [README](../../backend/modules/services/cms-connector-service/README.md) |
| **data-ingestion-service** | TBD | Bulk data ingestion, NDJSON import, synthetic data | PostgreSQL, Kafka | [README](../../backend/modules/services/data-ingestion-service/README.md) |
| **data-enrichment-service** | TBD | Data validation, enrichment, deduplication | PostgreSQL | [README](../../backend/modules/services/data-enrichment-service/README.md) |
| **ecr-service** | TBD | Electronic Case Reporting for public health | PostgreSQL | [README](../../backend/modules/services/ecr-service/README.md) |
| **sdoh-service** | TBD | Social determinants of health tracking | PostgreSQL, Redis | [README](../../backend/modules/services/sdoh-service/README.md) |
| **documentation-service** | TBD | Document management, OCR, clinical documentation | PostgreSQL, S3 | [README](../../backend/modules/services/documentation-service/README.md) |

---

## Platform & Infrastructure Services

Core platform infrastructure, messaging, routing, authentication.

| Service | Port | Purpose | Tech Stack | Documentation |
|---------|------|---------|-----------|-----------------|
| **gateway-clinical-service** | 8001 | Clinical API gateway, authentication, routing | Spring Cloud Gateway, PostgreSQL | [README](../../backend/modules/services/gateway-clinical-service/README.md) |
| **gateway-fhir-service** | TBD | FHIR-specific API gateway | Spring Cloud Gateway | [README](../../backend/modules/services/gateway-fhir-service/README.md) |
| **gateway-admin-service** | TBD | Admin API gateway, configuration management | Spring Cloud Gateway, PostgreSQL | [README](../../backend/modules/services/gateway-admin-service/README.md) |
| **admin-service** | TBD | Platform administration, tenant management | PostgreSQL | [README](../../backend/modules/services/admin-service/README.md) |
| **event-router-service** | TBD | Event routing, topic management, message transformation | Kafka | [README](../../backend/modules/services/event-router-service/README.md) |
| **event-store-service** | TBD | Immutable event log, event persistence | PostgreSQL, Kafka | [README](../../backend/modules/services/event-store-service/README.md) |
| **event-replay-service** | TBD | Event replay, projection rebuilding | PostgreSQL, Kafka | [README](../../backend/modules/services/event-replay-service/README.md) |
| **fhir-event-bridge-service** | TBD | Bridge between FHIR resources and event streams | Kafka, PostgreSQL | [README](../../backend/modules/services/fhir-event-bridge-service/README.md) |
| **notification-service** | TBD | Email, SMS, in-app notifications | PostgreSQL, Redis | [README](../../backend/modules/services/notification-service/README.md) |
| **audit-query-service** | TBD | Audit log querying, compliance reporting, NLQ | PostgreSQL, Elasticsearch | [README](../../backend/modules/services/audit-query-service/README.md) |
| **cdr-processor-service** | TBD | Clinical Data Repository processing, HL7 v2/CDA | PostgreSQL | [README](../../backend/modules/services/cdr-processor-service/README.md) |
| **cqrs-query-service** | TBD | CQRS read-side query API | PostgreSQL | [README](../../backend/modules/services/cqrs-query-service/README.md) |
| **query-api-service** | TBD | Unified query API with JWT auth, role hierarchy | PostgreSQL | [README](../../backend/modules/services/query-api-service/README.md) |

---

## Sales & CRM Services ✨ NEW

Sales automation, lead management, and CRM integrations.

| Service | Port | Purpose | Tech Stack | Documentation |
|---------|------|---------|-----------|-----------------|
| **sales-automation-service** | 8106 | CRM, lead capture, email sequences, LinkedIn outreach, Zoho sync | PostgreSQL, Redis, Kafka | [README](../../backend/modules/services/sales-automation-service/README.md) |
| **investor-dashboard-service** | TBD | Investor metrics, Zoho CRM sync, pipeline tracking | PostgreSQL, Redis | [README](../../backend/modules/services/investor-dashboard-service/README.md) |

---

## AI & Advanced Services

Machine learning, natural language processing, intelligent agents.

| Service | Port | Purpose | Tech Stack | Documentation |
|---------|------|---------|-----------|-----------------|
| **ai-assistant-service** | TBD | AI chatbot, clinical decision support | PostgreSQL, ML models | [README](../../backend/modules/services/ai-assistant-service/README.md) |
| **ai-sales-agent** | 8090 | AI sales agent, demo personas, objection handling | Python, FastAPI | [README](../../backend/modules/services/ai-sales-agent/README.md) |
| **live-call-sales-agent** | 8095 | Real-time AI call agent, WebSocket, Chrome automation | Python, FastAPI, WebSocket | [README](../../backend/modules/services/live-call-sales-agent/README.md) |
| **agent-runtime-service** | TBD | Agent execution engine, autonomous workflows | PostgreSQL, Kafka | [README](../../backend/modules/services/agent-runtime-service/README.md) |
| **agent-builder-service** | TBD | Agent creation and configuration UI | PostgreSQL | [README](../../backend/modules/services/agent-builder-service/README.md) |
| **agent-validation-service** | TBD | Agent output validation, safety checks | PostgreSQL | [README](../../backend/modules/services/agent-validation-service/README.md) |
| **devops-agent-service** | TBD | DevOps automation, infrastructure management agent | PostgreSQL | [README](../../backend/modules/services/devops-agent-service/README.md) |

---

## Event Services (Phase 5 - Event Sourcing) ✨ NEW

Modern microservices using Event Sourcing and CQRS patterns. These services implement the immutable event log architecture with denormalized projections for queries. See [Event Sourcing Architecture Guide](../architecture/EVENT_SOURCING_ARCHITECTURE.md) for detailed explanation.

| Service | Port | Purpose | Tech Stack | Documentation |
|---------|------|---------|-----------|-----------------|
| **patient-event-service** | 8110 | Patient events and projections | PostgreSQL, Kafka, Event Store | [README](../../backend/modules/services/patient-event-service/README.md) |
| **quality-measure-event-service** | 8191 | Quality measure events and calculations | PostgreSQL, Kafka, Event Store | [README](../../backend/modules/services/quality-measure-event-service/README.md) |
| **care-gap-event-service** | 8111 | Care gap events and detection | PostgreSQL, Kafka, Event Store | [README](../../backend/modules/services/care-gap-event-service/README.md) |
| **clinical-workflow-event-service** | 8193 | Clinical workflow events and orchestration | PostgreSQL, Kafka, Event Store | [README](../../backend/modules/services/clinical-workflow-event-service/README.md) |

### Event Handler Services

Event handlers consume domain events and trigger side effects (projections, notifications, integrations).

| Service | Port | Purpose | Tech Stack | Documentation |
|---------|------|---------|-----------|-----------------|
| **patient-event-handler-service** | TBD | Patient event projections, identity resolution | PostgreSQL, Kafka | [README](../../backend/modules/services/patient-event-handler-service/README.md) |
| **quality-measure-event-handler-service** | TBD | Quality measure event projections | PostgreSQL, Kafka | [README](../../backend/modules/services/quality-measure-event-handler-service/README.md) |
| **care-gap-event-handler-service** | TBD | Care gap event projections, closure tracking | PostgreSQL, Kafka | [README](../../backend/modules/services/care-gap-event-handler-service/README.md) |
| **clinical-workflow-event-handler-service** | TBD | Clinical workflow event projections | PostgreSQL, Kafka | [README](../../backend/modules/services/clinical-workflow-event-handler-service/README.md) |

---

## Demo & Orchestration Services

Services for demo environments, data seeding, and orchestration.

| Service | Port | Purpose | Tech Stack | Documentation |
|---------|------|---------|-----------|-----------------|
| **demo-orchestrator-service** | TBD | Demo environment orchestration, scenario management | PostgreSQL | [README](../../backend/modules/services/demo-orchestrator-service/README.md) |
| **demo-seeding-service** | TBD | Synthetic data generation, demo data seeding | PostgreSQL | [README](../../backend/modules/services/demo-seeding-service/README.md) |

---

## Shared Libraries & Modules

Shared code used across multiple services.

| Module | Purpose | Documentation |
|--------|---------|-----------------|
| **shared:domain:common** | Common domain models, value objects | [README](../../backend/modules/shared/domain/common/README.md) |
| **shared:domain:fhir-models** | FHIR R4 entity models | [README](../../backend/modules/shared/domain/fhir-models/README.md) |
| **shared:infrastructure:authentication** | Authentication filters and JWT handling | [README](../../backend/modules/shared/infrastructure/authentication/README.md) |
| **shared:infrastructure:database-config** | Database connection pooling and configuration | [README](../../backend/modules/shared/infrastructure/database-config/README.md) |
| **shared:infrastructure:persistence** | JPA repositories, Liquibase, database utilities | [README](../../backend/modules/shared/infrastructure/persistence/README.md) |
| **shared:infrastructure:messaging** | Kafka producer/consumer configuration | [README](../../backend/modules/shared/infrastructure/messaging/README.md) |
| **shared:infrastructure:cache** | Redis caching utilities and configuration | [README](../../backend/modules/shared/infrastructure/cache/README.md) |
| **shared:infrastructure:security** | Security configurations, RBAC, authorization | [README](../../backend/modules/shared/infrastructure/security/README.md) |
| **shared:infrastructure:audit** | Audit logging and compliance tracking | [README](../../backend/modules/shared/infrastructure/audit/README.md) |
| **shared:infrastructure:tracing** | OpenTelemetry distributed tracing | [README](../../backend/modules/shared/infrastructure/tracing/README.md) |
| **shared:infrastructure:event-sourcing** | Event sourcing patterns and utilities | [README](../../backend/modules/shared/infrastructure/event-sourcing/README.md) |

---

## Service By Purpose

### Clinical Quality Measures
- quality-measure-service
- cql-engine-service
- quality-measure-event-service

### Patient Management
- patient-service
- patient-event-service
- data-enrichment-service
- sdoh-service

### Care Delivery
- care-gap-service
- care-gap-event-service
- clinical-workflow-event-service
- prior-auth-service
- notification-service

### Analytics & Reporting
- analytics-service
- qrda-export-service
- predictive-analytics-service

### Risk Management
- hcc-service
- risk-stratification-service
- cost-monitoring-service

### Data Integration
- ehr-connector-service
- cms-connector-service
- fhir-service
- ecr-service
- cdr-processor-service
- data-ingestion-service
- documentation-service

### Platform Infrastructure
- gateway-clinical-service
- gateway-fhir-service
- gateway-admin-service
- admin-service
- event-router-service
- event-store-service
- event-replay-service
- event-processing-service
- fhir-event-bridge-service
- audit-query-service
- notification-service
- cqrs-query-service
- query-api-service

### AI & Automation
- ai-assistant-service
- ai-sales-agent
- live-call-sales-agent
- agent-runtime-service
- agent-builder-service
- agent-validation-service
- devops-agent-service

### Sales & CRM
- sales-automation-service
- investor-dashboard-service

### Demo & Orchestration
- demo-orchestrator-service
- demo-seeding-service

---

## Databases Mapping

Each service has its own database:

| Service | Database | Purpose |
|---------|----------|---------|
| quality-measure-service | quality_db | HEDIS measures and calculations |
| patient-service | patient_db | Patient demographics and records |
| cql-engine-service | cql_db | CQL expressions and evaluation |
| fhir-service | fhir_db | FHIR R4 resources |
| care-gap-service | care_gap_db | Care gap data |
| consent-service | consent_db | Patient consent records |
| gateway-service | gateway_db | User authentication and tokens |
| sales-automation-service | sales_automation_db | Leads, accounts, opportunities, sequences |
| [See full list](../architecture/database/) | [30 total](../architecture/database/) | Database architecture reference |

---

## Infrastructure Services

| Service | Port | Container | Purpose |
|---------|------|-----------|---------|
| PostgreSQL | 5435 | healthdata-postgres | Primary database (29 schemas) |
| Redis | 6380 | healthdata-redis | Caching layer |
| Kafka | 9094 | healthdata-kafka | Event messaging |
| Zookeeper | 2182 | healthdata-zookeeper | Kafka coordination |
| Jaeger | 16686 | healthdata-jaeger | Distributed tracing |
| Prometheus | 9090 | healthdata-prometheus | Metrics collection |
| Grafana | 3001 | healthdata-grafana | Metrics visualization |
| Vault | 8200 | healthdata-vault | Secrets management |

---

## Service Dependencies

### Most Connected Services
1. **gateway-service** - Routes to all other services
2. **patient-service** - Data used by most clinical services
3. **quality-measure-service** - Depends on patient, FHIR, CQL data
4. **fhir-service** - Clinical data source for many services
5. **Kafka** - Event backbone connecting async services

See [Service Dependency Map](DEPENDENCY_MAP.md) for complete interaction graph.

---

## Finding a Service

### By Purpose
- "I need to calculate quality measures" → quality-measure-service
- "I need to find care gaps" → care-gap-service
- "I need to integrate with an EHR" → ehr-connector-service
- "I need to export QRDA reports" → qrda-export-service
- "I need patient data" → patient-service
- "I need FHIR resources" → fhir-service
- "I need to manage workflows" → payer-workflows-service
- "I need AI assistance" → ai-assistant-service
- "I need CRM/sales automation" → sales-automation-service
- "I need email sequences" → sales-automation-service
- "I need LinkedIn outreach" → sales-automation-service

### By Technology
- "I'm working with PostgreSQL" → See [Database Architecture](../architecture/database/)
- "I'm working with Kafka" → event-router-service, event-processing-service
- "I'm working with CQL" → cql-engine-service, quality-measure-service
- "I'm working with FHIR" → fhir-service, ehr-connector-service

### By Category
- "I'm working on core clinical logic" → See Core Clinical Services
- "I'm working on analytics" → See Analytics & Reporting Services
- "I'm working on integrations" → See Integration & Data Services
- "I'm working on infrastructure" → See Platform & Infrastructure Services
- "I'm working on sales/CRM" → See Sales & CRM Services

---

## Service Status

- ✅ All services compiled and tested (as of February 2026)
- ✅ All services have READMEs
- ✅ All services use consistent Spring Boot architecture
- ✅ All services support HIPAA compliance requirements
- ✅ All services integrated with distributed tracing

---

## Adding a New Service

When adding a new service:

1. **Create the service module** in `backend/modules/services/{service-name}/`
2. **Create a README** with:
   - Service description
   - Tech stack
   - API endpoints
   - Dependencies on other services
   - Database schema (if applicable)
   - Configuration guide
3. **Update this catalog** - Add entry in appropriate section
4. **Update Service Catalog** with port, purpose, and link

See [CONTRIBUTING.md](../../CONTRIBUTING.md) for detailed guidelines.

---

## Service Documentation Quality Checklist

Each service README should include:

- [ ] Service description and purpose
- [ ] Tech stack (frameworks, databases, messaging)
- [ ] API endpoints with examples
- [ ] Configuration guide
- [ ] Database schema (if applicable)
- [ ] Dependencies on other services
- [ ] Deployment instructions
- [ ] Troubleshooting guide
- [ ] Links to relevant documentation

---

**Last Updated**: March 3, 2026
**Maintained by**: HDIM Platform Team
**Status**: Complete Service Catalog — 57 services (100% coverage including event sourcing, event handlers, AI agents, gateways, and demo services)
