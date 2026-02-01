# HDIM System Architecture Overview

High-level view of all 51 microservices, 4 gateways, 29 databases, and message flows.

---

## Core System Architecture

```mermaid
graph TB
    A["Clients<br/>(Web, Mobile, EHR)"] -->|"HTTP REST"| B["Load Balancer<br/>nginx/kubernetes"]

    B -->|"Routes"| C["4 Specialized Gateways"]

    C -->|"Admin"| C1["gateway-admin-service<br/>:8002"]
    C -->|"Clinical"| C2["gateway-clinical-service<br/>:8003"]
    C -->|"FHIR"| C3["gateway-fhir-service<br/>:8004"]
    C -->|"General"| C4["gateway-service<br/>:8001"]

    C1 --> D["Core Services<br/>28 microservices"]
    C2 --> D
    C3 --> D
    C4 --> D

    D -->|"Async Communication"| E["Kafka Cluster<br/>3 brokers"]

    D -->|"Persistence"| F["PostgreSQL 16<br/>29 logical databases"]

    D -->|"Caching"| G["Redis 7<br/>Session + PHI cache"]

    E -->|"Event Handler Services<br/>4 services"| H["Event Processors"]

    H -->|"Projections"| F

    style C1 fill:#e8f5e9
    style C2 fill:#e8f5e9
    style C3 fill:#e8f5e9
    style C4 fill:#e8f5e9
    style E fill:#fff9c4
    style F fill:#e1f5ff
    style G fill:#f3e5f5
```

---

## Core Microservices (28 Services)

```mermaid
graph TB
    A["API Layer<br/>(4 Gateways)"] -->|"Routes to"| B["Core Microservices"]

    B --> B1["Clinical Services"]
    B1 --> B1a["quality-measure-service:8087<br/>HEDIS evaluation"]
    B1 --> B1b["cql-engine-service:8081<br/>CQL execution"]
    B1 --> B1c["patient-service:8084<br/>Patient demographics"]
    B1 --> B1d["care-gap-service:8086<br/>Gap detection"]
    B1 --> B1e["consent-service<br/>Patient consent"]
    B1 --> B1f["encounter-service<br/>Visit records"]

    B --> B2["Integration Services"]
    B2 --> B2a["fhir-service:8085<br/>FHIR R4 API"]
    B2 --> B2b["ehr-connector-service<br/>EHR integration"]
    B2 --> B2c["interop-service<br/>External APIs"]

    B --> B3["Analytics Services"]
    B3 --> B3a["analytics-service:8089<br/>Reporting engine"]
    B3 --> B3b["predictive-analytics-service<br/>Risk stratification"]
    B3 --> B3c["data-warehouse-service<br/>Historical data"]

    B --> B4["Admin Services"]
    B4 --> B4a["tenant-service<br/>Org management"]
    B4 --> B4b["user-service<br/>User accounts"]
    B4 --> B4c["audit-service<br/>Compliance logging"]
    B4 --> B4d["config-service<br/>System config"]

    B --> B5["Support Services"]
    B5 --> B5a["notification-service<br/>Email/SMS"]
    B5 --> B5b["cache-service<br/>Redis management"]
    B5 --> B5c["logging-service<br/>Log aggregation"]
    B5 --> B5d["metrics-service<br/>Prometheus integration"]

    style B1a fill:#c8e6c9
    style B1b fill:#c8e6c9
    style B1c fill:#c8e6c9
    style B1d fill:#c8e6c9
    style B1e fill:#fff9c4
    style B1f fill:#fff9c4
```

---

## Event Services (Phase 5 - Event Sourcing Pattern)

```mermaid
graph TB
    A["Clinical Services"] -->|"Emit Events"| B["Kafka Topics"]

    A -->|"Patient lifecycle"| B1["patient.events"]
    A -->|"Measure evaluation"| B2["quality-measure.events"]
    A -->|"Gap detection"| B3["caregap.events"]
    A -->|"Workflow updates"| B4["clinical-workflow.events"]

    B1 --> C1["patient-event-handler-service"]
    B2 --> C2["quality-measure-event-handler-service"]
    B3 --> C3["care-gap-event-handler-service"]
    B4 --> C4["clinical-workflow-event-handler-service"]

    C1 -->|"Builds"| D1["patient_projection<br/>Read Model"]
    C2 -->|"Builds"| D2["measure_projection<br/>Read Model"]
    C3 -->|"Builds"| D3["gap_projection<br/>Read Model"]
    C4 -->|"Builds"| D4["workflow_projection<br/>Read Model"]

    style B1 fill:#fff9c4
    style B2 fill:#fff9c4
    style B3 fill:#fff9c4
    style B4 fill:#fff9c4
    style D1 fill:#c8e6c9
    style D2 fill:#c8e6c9
    style D3 fill:#c8e6c9
    style D4 fill:#c8e6c9
```

---

## Data Flow: Patient Creation to Care Gap Detection

```mermaid
graph TB
    A["1. Create Patient<br/>REST API"] -->|"POST /patients"| B["patient-service"]
    B -->|"2a. Persist"| C["PostgreSQL<br/>patient_db"]
    B -->|"2b. Publish"| D["patient.events<br/>PatientCreatedEvent"]

    D -->|"3. Async consume"| E["patient-event-handler"]
    E -->|"4. Build projection"| F["patient_projection"]

    D -->|"3. Async consume"| G["quality-measure-service"]
    G -->|"4. Evaluate measures"| H["quality_db"]
    G -->|"5. Publish"| I["quality-measure.events<br/>MeasureEvaluatedEvent"]

    I -->|"6. Async consume"| J["quality-event-handler"]
    J -->|"7. Build projection"| K["measure_projection"]

    I -->|"6. Async consume"| L["care-gap-service"]
    L -->|"7. Detect gaps"| M["caregap_db"]
    L -->|"8. Publish"| N["caregap.events<br/>GapDetectedEvent"]

    N -->|"9. Async consume"| O["caregap-event-handler"]
    O -->|"10. Build projection"| P["gap_projection"]

    P -->|"11. Query"| Q["analytics-service"]
    Q -->|"12. Reports"| R["Dashboard"]

    style C fill:#e1f5ff
    style H fill:#e1f5ff
    style M fill:#e1f5ff
    style D fill:#fff9c4
    style I fill:#fff9c4
    style N fill:#fff9c4
    style F fill:#c8e6c9
    style K fill:#c8e6c9
    style P fill:#c8e6c9
```

---

## Data Persistence: 29 Databases

```mermaid
graph LR
    A["PostgreSQL 16<br/>Single Instance"] --> B["Database-per-Service"]

    B --> C["Clinical Services"]
    C -->|"fhir_db"| C1["fhir-service"]
    C -->|"patient_db"| C2["patient-service"]
    C -->|"quality_db"| C3["quality-measure-service"]
    C -->|"cql_db"| C4["cql-engine-service"]
    C -->|"caregap_db"| C5["care-gap-service"]
    C -->|"consent_db"| C6["consent-service"]

    B --> D["Integration Services"]
    D -->|"ehr_db"| D1["ehr-connector-service"]
    D -->|"interop_db"| D2["interop-service"]

    B --> E["Analytics Services"]
    E -->|"analytics_db"| E1["analytics-service"]
    E -->|"warehouse_db"| E2["data-warehouse-service"]

    B --> F["Admin Services"]
    F -->|"gateway_db"| F1["gateway-services"]
    F -->|"tenant_db"| F2["tenant-service"]
    F -->|"user_db"| F3["user-service"]
    F -->|"audit_db"| F4["audit-service"]

    style A fill:#e1f5ff
    style C1 fill:#c8e6c9
    style D1 fill:#fff9c4
    style E1 fill:#b3e5fc
```

**Benefits**:
- Service isolation (no shared tables)
- Independent schema evolution
- Performance isolation (one slow service doesn't affect others)
- Compliance (per-database backups)

---

## Caching Layer (Redis)

```mermaid
graph TB
    A["Applications"] -->|"Cache operations"| B["Redis 7<br/>Session + PHI Cache"]

    B --> C["Cache Types"]
    C --> C1["Session Cache<br/>TTL: 30 minutes"]
    C --> C2["PHI Cache<br/>TTL: 5 minutes (HIPAA)"]
    C --> C3["Measure Cache<br/>TTL: 1 hour"]
    C --> C4["Patient Cache<br/>TTL: 5 minutes (PHI)"]

    C1 -->|"HAS KEY?"| D["Cache Hit"]
    C1 -->|"NO KEY?"| E["Cache Miss"]

    E -->|"Fetch from DB"| F["Database"]
    F -->|"Store in Cache"| C1

    style B fill:#f3e5f5
    style C2 fill:#ffcdd2
    style C4 fill:#ffcdd2
    style D fill:#c8e6c9
```

**PHI Cache Policy**:
- ✅ 5-minute TTL for all PHI (HIPAA requirement)
- ✅ Automatic expiration
- ✅ Cache-Control headers on responses
- ✅ Audit logging of cache access

---

## Messaging (Kafka) Topology

```mermaid
graph TB
    A["Kafka 3.x Cluster<br/>3 Brokers<br/>Replication Factor: 3<br/>Retention: 30 days"] --> B["Topics"]

    B --> T1["patient.events<br/>Partitions: 3"]
    B --> T2["quality-measure.events<br/>Partitions: 3"]
    B --> T3["caregap.events<br/>Partitions: 3"]
    B --> T4["clinical-workflow.events<br/>Partitions: 3"]

    T1 --> C1["Consumer: patient-event-handler"]
    T2 --> C2["Consumer: quality-event-handler"]
    T3 --> C3["Consumer: caregap-event-handler"]
    T4 --> C4["Consumer: workflow-event-handler"]

    T1 --> C5["Consumer: analytics-service"]
    T2 --> C5
    T3 --> C5
    T4 --> C5

    C1 --> D["Message Lag<br/>Monitoring"]
    C2 --> D
    C3 --> D
    C4 --> D
    C5 --> D

    style A fill:#fff9c4
    style T1 fill:#fff3e0
    style T2 fill:#fff3e0
    style T3 fill:#fff3e0
    style T4 fill:#fff3e0
```

**Kafka Configuration**:
- 3-broker cluster (no single point of failure)
- Replication factor 3 (safe from disk failures)
- 30-day retention (event replay capability)
- Partitioned by patient/entity ID (ordered processing)

---

## Monitoring and Observability

```mermaid
graph TB
    A["All Services"] -->|"Emit"| B["Observability Stack"]

    A -->|"Logs"| B1["Log Aggregation<br/>(ELK/Loki)"]
    A -->|"Metrics"| B2["Prometheus<br/>Scrapes :8080/metrics"]
    A -->|"Traces"| B3["OpenTelemetry<br/>→ Jaeger Backend"]

    B1 -->|"Kibana"| C1["Log Visualization"]
    B2 -->|"Grafana"| C2["Metrics Dashboards<br/>http://localhost:3001"]
    B3 -->|"Jaeger UI"| C3["Distributed Tracing<br/>http://localhost:16686"]

    B1 --> D["Alerting<br/>Rules"]
    B2 --> D
    D -->|"Alert"| E["On-Call Team"]

    style B2 fill:#fff9c4
    style B3 fill:#b3e5fc
    style C2 fill:#c8e6c9
    style C3 fill:#c8e6c9
```

---

## References

- **[System Architecture Guide](../SYSTEM_ARCHITECTURE.md)** - Detailed architecture
- **[Service Catalog](../../services/SERVICE_CATALOG.md)** - Complete service list
- **[Dependency Map](../../services/DEPENDENCY_MAP.md)** - Service interactions
- **[Round Trip Flows](../ROUND_TRIP_FLOWS.md)** - Request tracing

---

_Last Updated: January 19, 2026_
_Version: 1.0_
_Total Services: 51 microservices_
_Total Databases: 29 logical databases_
_Event Topics: 4 (+ error topics)_
