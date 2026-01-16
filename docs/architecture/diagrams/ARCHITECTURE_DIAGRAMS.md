# HDIM Architecture Diagrams

## HealthData-in-Motion (HDIM) Visual Architecture Documentation

**Generated:** December 5, 2025
**Platform Version:** Production-Ready
**Diagram Format:** Mermaid (GitHub/GitLab compatible)

---

## Table of Contents

1. [C4 Level 1: System Context Diagram](#1-c4-level-1-system-context-diagram)
2. [C4 Level 2: Container Diagram](#2-c4-level-2-container-diagram)
3. [C4 Level 3: Component Diagrams](#3-c4-level-3-component-diagrams)
4. [Sequence Diagrams](#4-sequence-diagrams)
5. [Database ERD](#5-database-erd)
6. [Deployment Architecture](#6-deployment-architecture)
7. [Security Architecture](#7-security-architecture)
8. [Data Flow Diagrams](#8-data-flow-diagrams)

## Related Flow Documentation

- **[Platform Flow Overview](../PLATFORM_FLOW_OVERVIEW.md)**: High-level flow architecture and patterns
- **[Round-Trip Flows](../ROUND_TRIP_FLOWS.md)**: Complete sequence diagrams from button press to response

---

## 1. C4 Level 1: System Context Diagram

**Purpose:** Shows HDIM in the context of its users and external systems
**Audience:** All stakeholders (technical and non-technical)

```mermaid
C4Context
    title System Context Diagram - HealthData-in-Motion (HDIM)

    Person(clinician, "Clinical Staff", "Physicians, nurses, care coordinators who manage patient care and close care gaps")
    Person(admin, "System Administrator", "IT staff managing platform configuration and user access")
    Person(analyst, "Quality Analyst", "Staff reviewing HEDIS measures and quality metrics")
    Person(patient, "Patient", "Healthcare consumer accessing their health data")

    Enterprise_Boundary(hdim, "HealthData-in-Motion Platform") {
        System(hdim_platform, "HDIM Platform", "Enterprise Healthcare Interoperability & Quality Measurement Platform. Manages FHIR data, calculates quality measures, and identifies care gaps.")
    }

    System_Ext(ehr, "EHR Systems", "Epic, Cerner, Athenahealth - Source of patient clinical data")
    System_Ext(lab, "Laboratory Systems", "Lab results and diagnostic data")
    System_Ext(pharmacy, "Pharmacy Systems", "Medication dispense and fill data")
    System_Ext(hie, "Health Information Exchange", "Regional/state HIE for data sharing")
    System_Ext(payer, "Payer Systems", "Insurance claims and eligibility data")
    System_Ext(cms, "CMS/NCQA", "Quality reporting and certification")
    System_Ext(notification, "Notification Services", "Email, SMS for patient outreach")

    Rel(clinician, hdim_platform, "Views care gaps, patient dashboards", "HTTPS")
    Rel(admin, hdim_platform, "Configures measures, manages users", "HTTPS")
    Rel(analyst, hdim_platform, "Reviews quality reports, exports data", "HTTPS")
    Rel(patient, hdim_platform, "Views health records via patient portal", "HTTPS")

    Rel(hdim_platform, ehr, "Retrieves patient data", "FHIR R4/HL7 v2")
    Rel(hdim_platform, lab, "Retrieves lab results", "FHIR R4")
    Rel(hdim_platform, pharmacy, "Retrieves medication data", "FHIR R4")
    Rel(hdim_platform, hie, "Exchanges patient data", "FHIR R4")
    Rel(hdim_platform, payer, "Retrieves claims data", "FHIR R4/X12")
    Rel(hdim_platform, cms, "Submits quality reports", "QRDA/FHIR")
    Rel(hdim_platform, notification, "Sends patient outreach", "SMTP/SMS API")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

### Context Diagram - Simplified View

```mermaid
graph TB
    subgraph "Users"
        C[Clinical Staff]
        A[Administrators]
        Q[Quality Analysts]
        P[Patients]
    end

    subgraph "HDIM Platform"
        HDIM[HealthData-in-Motion<br/>Enterprise Healthcare Platform]
    end

    subgraph "External Systems"
        EHR[EHR Systems<br/>Epic, Cerner]
        LAB[Laboratory Systems]
        PHARM[Pharmacy Systems]
        HIE[Health Information<br/>Exchange]
        PAYER[Payer Systems]
        CMS[CMS / NCQA]
    end

    C -->|Manage Care Gaps| HDIM
    A -->|Configure Platform| HDIM
    Q -->|Review Measures| HDIM
    P -->|Access Health Data| HDIM

    HDIM <-->|FHIR R4| EHR
    HDIM <-->|FHIR R4| LAB
    HDIM <-->|FHIR R4| PHARM
    HDIM <-->|FHIR R4| HIE
    HDIM <-->|FHIR R4/X12| PAYER
    HDIM -->|QRDA/FHIR| CMS

    style HDIM fill:#1168bd,stroke:#0b4884,color:#ffffff
    style EHR fill:#999999,stroke:#666666,color:#ffffff
    style LAB fill:#999999,stroke:#666666,color:#ffffff
    style PHARM fill:#999999,stroke:#666666,color:#ffffff
    style HIE fill:#999999,stroke:#666666,color:#ffffff
    style PAYER fill:#999999,stroke:#666666,color:#ffffff
    style CMS fill:#999999,stroke:#666666,color:#ffffff
```

---

## 2. C4 Level 2: Container Diagram

**Purpose:** Shows the high-level technology choices and how containers interact
**Audience:** Technical stakeholders, architects, developers

```mermaid
C4Container
    title Container Diagram - HealthData-in-Motion (HDIM)

    Person(clinician, "Clinical Staff", "Manages patient care")
    Person(admin, "Administrator", "Manages platform")

    Container_Boundary(frontend, "Frontend Applications") {
        Container(clinical_portal, "Clinical Portal", "Angular 20, Material UI", "Primary web application for clinical staff")
        Container(admin_portal, "Admin Portal", "Angular 20", "Administrative configuration interface")
        Container(patient_portal, "Patient Portal", "React 19, Vite", "Patient-facing health dashboard")
    }

    Container_Boundary(gateway, "API Gateway Layer") {
        Container(api_gateway, "API Gateway", "Spring Boot, Spring Cloud Gateway", "Authentication, rate limiting, routing")
        Container(kong, "Kong Gateway", "Kong", "External API management, OAuth2")
    }

    Container_Boundary(services, "Microservices") {
        Container(fhir_service, "FHIR Service", "Java 21, HAPI FHIR", "FHIR R4 resource management, Bulk Data API")
        Container(cql_engine, "CQL Engine Service", "Java 21, CQL Engine", "Clinical Quality Language evaluation")
        Container(quality_measure, "Quality Measure Service", "Java 21, Spring Boot", "HEDIS measure calculation, orchestration")
        Container(care_gap, "Care Gap Service", "Java 21, Spring Boot", "Gap identification and prioritization")
        Container(patient_service, "Patient Service", "Java 21, Spring Boot", "Patient data management, 360 view")
        Container(consent_service, "Consent Service", "Java 21, Spring Boot", "Patient consent management")
        Container(event_processing, "Event Processing Service", "Java 21, Kafka", "Real-time event handling, DLQ")
        Container(analytics, "Analytics Service", "Java 21, Spring Boot", "Real-time dashboards, population health")
        Container(ai_assistant, "AI Assistant Service", "Java 21, LLM Integration", "AI-powered clinical assistance")
    }

    Container_Boundary(data, "Data Layer") {
        ContainerDb(postgres, "PostgreSQL", "PostgreSQL 16", "Primary data store for all services")
        ContainerDb(redis, "Redis Cache", "Redis 7", "Session cache, query cache, measure results")
        ContainerQueue(kafka, "Kafka", "Apache Kafka 3.x", "Event streaming, async processing")
    }

    Rel(clinician, clinical_portal, "Uses", "HTTPS")
    Rel(admin, admin_portal, "Uses", "HTTPS")

    Rel(clinical_portal, api_gateway, "API calls", "HTTPS/JSON")
    Rel(admin_portal, api_gateway, "API calls", "HTTPS/JSON")
    Rel(patient_portal, kong, "API calls", "HTTPS/JSON")

    Rel(api_gateway, fhir_service, "Routes", "HTTP")
    Rel(api_gateway, quality_measure, "Routes", "HTTP")
    Rel(api_gateway, care_gap, "Routes", "HTTP")
    Rel(api_gateway, patient_service, "Routes", "HTTP")

    Rel(quality_measure, cql_engine, "Evaluates measures", "HTTP")
    Rel(quality_measure, fhir_service, "Fetches patient data", "HTTP")
    Rel(care_gap, quality_measure, "Gets measure results", "HTTP")

    Rel(fhir_service, postgres, "Reads/Writes", "JDBC")
    Rel(patient_service, postgres, "Reads/Writes", "JDBC")
    Rel(quality_measure, postgres, "Reads/Writes", "JDBC")

    Rel(cql_engine, redis, "Caches templates", "TCP")
    Rel(quality_measure, redis, "Caches results", "TCP")

    Rel(event_processing, kafka, "Consumes/Produces", "TCP")
    Rel(analytics, kafka, "Consumes", "TCP")

    UpdateLayoutConfig($c4ShapeInRow="4", $c4BoundaryInRow="1")
```

### Container Diagram - Simplified Microservices View

```mermaid
graph TB
    subgraph "Frontend Layer"
        CP[Clinical Portal<br/>Angular 20]
        AP[Admin Portal<br/>Angular 20]
        PP[Patient Portal<br/>React 19]
    end

    subgraph "Gateway Layer"
        GW[API Gateway<br/>Spring Cloud Gateway<br/>:9000]
    end

    subgraph "Core Services"
        FHIR[FHIR Service<br/>HAPI FHIR R4<br/>:8081]
        CQL[CQL Engine Service<br/>CQL Evaluation<br/>:8086]
        QM[Quality Measure Service<br/>HEDIS Orchestration<br/>:8087]
        CG[Care Gap Service<br/>Gap Detection<br/>:8083]
        PS[Patient Service<br/>360° Patient View<br/>:8082]
    end

    subgraph "Supporting Services"
        EVT[Event Processing<br/>Kafka Consumer<br/>:8085]
        AN[Analytics Service<br/>Real-time Metrics<br/>:8089]
        AI[AI Assistant<br/>LLM Integration<br/>:8090]
        CON[Consent Service<br/>:8084]
    end

    subgraph "Data Layer"
        PG[(PostgreSQL 16<br/>:5432)]
        RD[(Redis 7<br/>:6379)]
        KF[Kafka<br/>:9092]
    end

    CP --> GW
    AP --> GW
    PP --> GW

    GW --> FHIR
    GW --> QM
    GW --> CG
    GW --> PS

    QM --> CQL
    QM --> FHIR
    CG --> QM
    PS --> FHIR

    FHIR --> PG
    QM --> PG
    PS --> PG
    CQL --> RD
    QM --> RD

    EVT --> KF
    AN --> KF
    QM --> KF

    style GW fill:#1168bd,stroke:#0b4884,color:#ffffff
    style FHIR fill:#438dd5,stroke:#2e6295,color:#ffffff
    style CQL fill:#438dd5,stroke:#2e6295,color:#ffffff
    style QM fill:#438dd5,stroke:#2e6295,color:#ffffff
    style CG fill:#438dd5,stroke:#2e6295,color:#ffffff
    style PS fill:#438dd5,stroke:#2e6295,color:#ffffff
    style PG fill:#85bbf0,stroke:#5d82a8,color:#000000
    style RD fill:#85bbf0,stroke:#5d82a8,color:#000000
    style KF fill:#f5a623,stroke:#b07818,color:#000000
```

---

## 3. C4 Level 3: Component Diagrams

### 3.1 Quality Measure Service - Component Diagram

**Purpose:** Internal structure of the Quality Measure Service
**Audience:** Developers working on quality measurement features

```mermaid
graph TB
    subgraph "Quality Measure Service"
        subgraph "API Layer"
            MC[MeasureController<br/>REST API endpoints]
            WS[WebSocketController<br/>Real-time updates]
        end

        subgraph "Business Logic"
            MS[MeasureService<br/>Orchestration logic]
            ES[EvaluationService<br/>Batch evaluation]
            CS[CalculationService<br/>Result computation]
            NS[NotificationService<br/>Alert generation]
        end

        subgraph "Integration"
            FC[FhirServiceClient<br/>FHIR data retrieval]
            CC[CqlEngineClient<br/>CQL evaluation]
            KC[KafkaProducer<br/>Event publishing]
        end

        subgraph "Data Access"
            MR[MeasureRepository<br/>Measure definitions]
            RR[ResultRepository<br/>Evaluation results]
            TR[TemplateRepository<br/>CQL templates]
        end

        subgraph "Caching"
            RC[RedisCacheManager<br/>Result caching]
            TC[TemplateCacheService<br/>Template caching]
        end
    end

    MC --> MS
    WS --> MS
    MS --> ES
    MS --> CS
    ES --> CC
    ES --> FC
    CS --> RR
    MS --> NS
    NS --> KC

    FC -->|HTTP| FHIR[FHIR Service]
    CC -->|HTTP| CQL[CQL Engine]
    KC -->|Kafka| KF[Kafka]

    MR --> PG[(PostgreSQL)]
    RR --> PG
    TR --> PG

    RC --> RD[(Redis)]
    TC --> RD

    style MC fill:#85bbf0,stroke:#5d82a8
    style WS fill:#85bbf0,stroke:#5d82a8
    style MS fill:#438dd5,stroke:#2e6295,color:#ffffff
    style ES fill:#438dd5,stroke:#2e6295,color:#ffffff
    style CS fill:#438dd5,stroke:#2e6295,color:#ffffff
```

### 3.2 CQL Engine Service - Component Diagram

```mermaid
graph TB
    subgraph "CQL Engine Service"
        subgraph "API Layer"
            EC[EvaluationController<br/>REST endpoints]
            TC[TemplateController<br/>Template management]
        end

        subgraph "Core Engine"
            CE[CqlEvaluator<br/>Expression evaluation]
            LP[LibraryProvider<br/>CQL library loading]
            DP[DataProvider<br/>FHIR data access]
            TP[TerminologyProvider<br/>Code system lookup]
        end

        subgraph "Template Management"
            TS[TemplateService<br/>Template CRUD]
            TV[TemplateValidator<br/>CQL validation]
            TCS[TemplateCacheService<br/>Template caching]
        end

        subgraph "Data Access"
            FR[FhirRepository<br/>Patient data]
            TR[TemplateRepository<br/>CQL templates]
        end
    end

    EC --> CE
    TC --> TS

    CE --> LP
    CE --> DP
    CE --> TP

    TS --> TV
    TS --> TCS
    TS --> TR

    DP -->|FHIR R4| FHIR[FHIR Service]
    FR --> PG[(PostgreSQL)]
    TR --> PG
    TCS --> RD[(Redis)]

    style CE fill:#1168bd,stroke:#0b4884,color:#ffffff
    style LP fill:#438dd5,stroke:#2e6295,color:#ffffff
    style DP fill:#438dd5,stroke:#2e6295,color:#ffffff
```

---

## 4. Sequence Diagrams

### 4.1 Quality Measure Evaluation Flow

**Purpose:** Shows the complete flow of evaluating a HEDIS measure for a patient
**Use Case:** Understanding measure calculation process

```mermaid
sequenceDiagram
    autonumber
    participant User as Clinical Staff
    participant Portal as Clinical Portal
    participant GW as API Gateway
    participant QM as Quality Measure Service
    participant CQL as CQL Engine Service
    participant FHIR as FHIR Service
    participant Redis as Redis Cache
    participant Kafka as Kafka
    participant DB as PostgreSQL

    User->>Portal: Request patient measure evaluation
    Portal->>GW: POST /api/measures/evaluate/{patientId}
    GW->>GW: Validate JWT token
    GW->>QM: Forward request with tenant context

    QM->>Redis: Check cache for recent result
    alt Cache Hit
        Redis-->>QM: Return cached result
        QM-->>GW: Return cached measure result
    else Cache Miss
        QM->>CQL: POST /cql/evaluate
        CQL->>Redis: Load CQL template

        alt Template Cached
            Redis-->>CQL: Return cached template
        else Template Not Cached
            CQL->>DB: Load template from DB
            DB-->>CQL: Return template
            CQL->>Redis: Cache template (24h TTL)
        end

        CQL->>FHIR: GET /Patient/{id}/$everything
        FHIR->>DB: Query patient resources
        DB-->>FHIR: Return FHIR resources
        FHIR-->>CQL: Return patient bundle

        CQL->>CQL: Evaluate CQL expressions
        CQL->>CQL: Calculate numerator/denominator
        CQL-->>QM: Return evaluation result

        QM->>DB: Store evaluation result
        QM->>Redis: Cache result (5min TTL - HIPAA)
        QM->>Kafka: Publish evaluation event

        QM-->>GW: Return measure result
    end

    GW-->>Portal: JSON response
    Portal-->>User: Display measure result
```

### 4.2 Care Gap Identification Flow

```mermaid
sequenceDiagram
    autonumber
    participant Scheduler as Scheduled Job
    participant CG as Care Gap Service
    participant QM as Quality Measure Service
    participant FHIR as FHIR Service
    participant DB as PostgreSQL
    participant Kafka as Kafka
    participant Alert as Alert Service

    Scheduler->>CG: Trigger daily care gap scan

    loop For each patient cohort
        CG->>FHIR: GET /Patient?_count=100
        FHIR-->>CG: Return patient list

        loop For each patient
            CG->>QM: GET /measures/patient/{id}/all
            QM-->>CG: Return measure results

            CG->>CG: Identify gaps (numerator not met)
            CG->>CG: Calculate priority score

            alt New Gap Found
                CG->>DB: Insert care gap record
                CG->>Kafka: Publish gap.identified event
            else Existing Gap
                CG->>DB: Update gap status
            end
        end
    end

    CG->>DB: Update gap scan timestamp
    CG->>Kafka: Publish scan.completed event

    Kafka-->>Alert: Consume gap events
    Alert->>Alert: Generate care team notifications
    Alert->>DB: Store notifications
```

### 4.3 Authentication Flow

```mermaid
sequenceDiagram
    autonumber
    participant User as User
    participant Portal as Clinical Portal
    participant GW as API Gateway
    participant Auth as Auth Service
    participant DB as PostgreSQL
    participant Redis as Redis

    User->>Portal: Enter credentials
    Portal->>GW: POST /auth/login {email, password}
    GW->>Auth: Forward login request

    Auth->>DB: Query user by email
    DB-->>Auth: Return user record

    Auth->>Auth: Verify password (bcrypt)

    alt Valid Credentials
        Auth->>Auth: Generate JWT (access + refresh)
        Auth->>Redis: Store refresh token
        Auth->>DB: Log successful login (audit)
        Auth-->>GW: {accessToken, refreshToken, user}
        GW-->>Portal: Return tokens
        Portal->>Portal: Store token in HttpOnly cookie
        Portal-->>User: Redirect to dashboard
    else Invalid Credentials
        Auth->>DB: Log failed attempt (audit)
        Auth-->>GW: 401 Unauthorized
        GW-->>Portal: Error response
        Portal-->>User: Show error message
    end

    Note over Portal,Redis: Token Refresh Flow
    Portal->>GW: Request with expired token
    GW->>Auth: POST /auth/refresh {refreshToken}
    Auth->>Redis: Validate refresh token
    Redis-->>Auth: Token valid
    Auth->>Auth: Generate new access token
    Auth-->>GW: {accessToken}
    GW-->>Portal: New token
```

### 4.4 FHIR Bulk Data Export Flow

```mermaid
sequenceDiagram
    autonumber
    participant Client as API Client
    participant GW as API Gateway
    participant FHIR as FHIR Service
    participant Export as Bulk Export Job
    participant DB as PostgreSQL
    participant S3 as S3 Storage

    Client->>GW: POST /fhir/$export
    GW->>FHIR: Initiate bulk export
    FHIR->>FHIR: Validate request parameters
    FHIR->>DB: Create export job record
    FHIR-->>GW: 202 Accepted + Content-Location header
    GW-->>Client: Job URL for polling

    Note over FHIR,S3: Async Export Processing

    FHIR->>Export: Start async export job

    loop For each resource type
        Export->>DB: Query resources (paginated)
        DB-->>Export: Return resource batch
        Export->>Export: Convert to NDJSON
        Export->>S3: Upload NDJSON file
        Export->>DB: Update job progress
    end

    Export->>DB: Mark job complete
    Export->>S3: Generate signed URLs

    Note over Client,S3: Client Polling

    loop Until complete
        Client->>GW: GET /fhir/$export-status/{jobId}
        GW->>FHIR: Check job status
        FHIR->>DB: Query job status
        DB-->>FHIR: Return status

        alt Still Processing
            FHIR-->>GW: 202 Accepted + progress
            GW-->>Client: In progress
        else Complete
            FHIR-->>GW: 200 OK + file URLs
            GW-->>Client: Download URLs
        end
    end

    Client->>S3: Download NDJSON files
    S3-->>Client: Return data files
```

---

## 5. Database ERD

### 5.1 Core Domain Model

```mermaid
erDiagram
    TENANT ||--o{ USER : has
    TENANT ||--o{ PATIENT : manages
    TENANT ||--o{ MEASURE_DEFINITION : owns

    TENANT {
        uuid id PK
        string name
        string identifier UK
        enum status
        jsonb settings
        timestamp created_at
    }

    USER ||--o{ AUDIT_LOG : creates
    USER {
        uuid id PK
        uuid tenant_id FK
        string email UK
        string password_hash
        string first_name
        string last_name
        enum role
        boolean mfa_enabled
        timestamp last_login
        timestamp created_at
    }

    PATIENT ||--o{ MEASURE_RESULT : has
    PATIENT ||--o{ CARE_GAP : has
    PATIENT ||--o{ CONSENT : gives
    PATIENT {
        uuid id PK
        uuid tenant_id FK
        string mrn UK
        string fhir_id
        string first_name
        string last_name
        date birth_date
        enum gender
        jsonb demographics
        timestamp created_at
    }

    MEASURE_DEFINITION ||--o{ MEASURE_RESULT : evaluates
    MEASURE_DEFINITION ||--o{ CQL_TEMPLATE : uses
    MEASURE_DEFINITION {
        uuid id PK
        uuid tenant_id FK
        string measure_id UK
        string name
        string version
        enum type
        text description
        jsonb metadata
        boolean active
        timestamp created_at
    }

    CQL_TEMPLATE {
        uuid id PK
        uuid measure_id FK
        string name
        text cql_content
        string fhir_version
        enum status
        timestamp compiled_at
        timestamp created_at
    }

    MEASURE_RESULT ||--o{ CARE_GAP : identifies
    MEASURE_RESULT {
        uuid id PK
        uuid patient_id FK
        uuid measure_id FK
        uuid tenant_id FK
        date measurement_period_start
        date measurement_period_end
        boolean in_initial_population
        boolean in_denominator
        boolean in_numerator
        boolean in_exclusion
        jsonb evaluation_details
        timestamp evaluated_at
    }

    CARE_GAP {
        uuid id PK
        uuid patient_id FK
        uuid measure_id FK
        uuid result_id FK
        uuid tenant_id FK
        string gap_type
        enum status
        enum priority
        date identified_at
        date due_date
        date closed_at
        jsonb metadata
    }

    CONSENT {
        uuid id PK
        uuid patient_id FK
        uuid tenant_id FK
        enum consent_type
        enum status
        date effective_from
        date effective_to
        jsonb scope
        timestamp created_at
    }

    AUDIT_LOG {
        uuid id PK
        uuid user_id FK
        uuid tenant_id FK
        string action
        string resource_type
        uuid resource_id
        jsonb old_value
        jsonb new_value
        string ip_address
        timestamp created_at
    }
```

### 5.2 Event Processing Domain

```mermaid
erDiagram
    DLQ_EVENT ||--o{ DLQ_RETRY : has

    DLQ_EVENT {
        uuid id PK
        uuid tenant_id FK
        string topic
        string event_type
        jsonb payload
        string error_message
        text stack_trace
        enum status
        int retry_count
        timestamp failed_at
        timestamp resolved_at
    }

    DLQ_RETRY {
        uuid id PK
        uuid event_id FK
        int attempt_number
        string error_message
        timestamp attempted_at
    }

    KAFKA_OFFSET {
        uuid id PK
        string topic
        int partition
        bigint offset_value
        string consumer_group
        timestamp updated_at
    }

    EVENT_SUBSCRIPTION {
        uuid id PK
        uuid tenant_id FK
        string event_type
        string webhook_url
        jsonb headers
        enum status
        int retry_count
        timestamp created_at
    }
```

---

## 6. Deployment Architecture

### 6.1 Kubernetes Deployment

```mermaid
graph TB
    subgraph "Internet"
        Users[Users / API Clients]
    end

    subgraph "Cloud Provider"
        subgraph "Ingress Layer"
            LB[Load Balancer<br/>AWS ALB / GCP LB]
            Ingress[NGINX Ingress Controller]
        end

        subgraph "Kubernetes Cluster"
            subgraph "healthdata-prod namespace"
                subgraph "Frontend Pods"
                    CP1[Clinical Portal Pod 1]
                    CP2[Clinical Portal Pod 2]
                end

                subgraph "Gateway Pods"
                    GW1[API Gateway Pod 1]
                    GW2[API Gateway Pod 2]
                    GW3[API Gateway Pod 3]
                end

                subgraph "Core Service Pods"
                    FHIR1[FHIR Service Pod 1]
                    FHIR2[FHIR Service Pod 2]
                    CQL1[CQL Engine Pod 1]
                    CQL2[CQL Engine Pod 2]
                    CQL3[CQL Engine Pod 3]
                    QM1[Quality Measure Pod 1]
                    QM2[Quality Measure Pod 2]
                end

                subgraph "Event Processing Pods"
                    EVT1[Event Processor Pod 1]
                    EVT2[Event Processor Pod 2]
                end
            end

            subgraph "healthdata-data namespace"
                PG_Primary[(PostgreSQL Primary)]
                PG_Replica1[(PostgreSQL Replica 1)]
                PG_Replica2[(PostgreSQL Replica 2)]
                Redis_Primary[(Redis Primary)]
                Redis_Replica[(Redis Replica)]
            end
        end

        subgraph "Managed Services"
            Kafka[Amazon MSK<br/>Kafka Cluster]
            S3[S3 Bucket<br/>Bulk Export Storage]
            Secrets[AWS Secrets Manager]
        end

        subgraph "Monitoring"
            Prometheus[Prometheus]
            Grafana[Grafana Dashboards]
            AlertManager[AlertManager]
        end
    end

    Users --> LB
    LB --> Ingress
    Ingress --> CP1
    Ingress --> CP2
    Ingress --> GW1
    Ingress --> GW2
    Ingress --> GW3

    GW1 --> FHIR1
    GW2 --> FHIR2
    GW1 --> CQL1
    GW2 --> CQL2
    GW3 --> CQL3
    GW1 --> QM1
    GW2 --> QM2

    FHIR1 --> PG_Primary
    FHIR2 --> PG_Replica1
    QM1 --> PG_Primary
    QM2 --> PG_Replica2

    CQL1 --> Redis_Primary
    CQL2 --> Redis_Primary
    CQL3 --> Redis_Replica

    EVT1 --> Kafka
    EVT2 --> Kafka
    QM1 --> Kafka

    Prometheus --> GW1
    Prometheus --> FHIR1
    Prometheus --> CQL1
    Grafana --> Prometheus

    style LB fill:#f5a623,stroke:#b07818
    style Ingress fill:#f5a623,stroke:#b07818
    style PG_Primary fill:#336791,stroke:#1e3d5c,color:#ffffff
    style Redis_Primary fill:#dc382d,stroke:#a02a23,color:#ffffff
    style Kafka fill:#231f20,stroke:#000000,color:#ffffff
```

### 6.2 Docker Compose Local Development

```mermaid
graph TB
    subgraph "Developer Machine"
        subgraph "Frontend Containers"
            Portal[Clinical Portal<br/>:4200]
            Admin[Admin Portal<br/>:4201]
        end

        subgraph "Backend Containers"
            GW[API Gateway<br/>:9000]
            FHIR[FHIR Service<br/>:8081]
            CQL[CQL Engine<br/>:8086]
            QM[Quality Measure<br/>:8087]
            CG[Care Gap<br/>:8083]
            PS[Patient Service<br/>:8082]
        end

        subgraph "Infrastructure Containers"
            PG[(PostgreSQL<br/>:5432)]
            Redis[(Redis<br/>:6379)]
            Kafka[Kafka<br/>:9092]
            ZK[Zookeeper<br/>:2181]
        end

        subgraph "Monitoring Containers"
            Prom[Prometheus<br/>:9090]
            Graf[Grafana<br/>:3000]
        end
    end

    Portal --> GW
    Admin --> GW
    GW --> FHIR
    GW --> CQL
    GW --> QM
    GW --> CG
    GW --> PS

    FHIR --> PG
    QM --> PG
    PS --> PG
    CQL --> Redis
    QM --> Kafka
    Kafka --> ZK

    Prom --> GW
    Prom --> FHIR
    Graf --> Prom

    style PG fill:#336791,stroke:#1e3d5c,color:#ffffff
    style Redis fill:#dc382d,stroke:#a02a23,color:#ffffff
    style Kafka fill:#231f20,stroke:#000000,color:#ffffff
```

---

## 7. Security Architecture

### 7.1 Authentication & Authorization Flow

```mermaid
flowchart TB
    subgraph "Client Layer"
        Browser[Browser / Mobile App]
        API_Client[API Client]
    end

    subgraph "Edge Security"
        WAF[WAF<br/>AWS WAF / Cloudflare]
        CDN[CDN<br/>CloudFront]
        Rate[Rate Limiter]
    end

    subgraph "Authentication"
        OAuth[OAuth2 / OIDC Provider]
        JWT[JWT Validation]
        MFA[MFA Service]
        Session[Session Manager<br/>Redis]
    end

    subgraph "Authorization"
        RBAC[Role-Based Access Control]
        ABAC[Attribute-Based Access Control]
        Tenant[Tenant Isolation Filter]
    end

    subgraph "API Gateway"
        GW[Spring Cloud Gateway]
    end

    subgraph "Services"
        Service1[Microservice 1]
        Service2[Microservice 2]
    end

    subgraph "Audit"
        Audit[Audit Service]
        AuditDB[(Audit DB<br/>Encrypted)]
    end

    Browser --> WAF
    API_Client --> WAF
    WAF --> CDN
    CDN --> Rate
    Rate --> OAuth

    OAuth --> JWT
    OAuth --> MFA
    JWT --> Session
    JWT --> GW

    GW --> RBAC
    RBAC --> ABAC
    ABAC --> Tenant
    Tenant --> Service1
    Tenant --> Service2

    Service1 --> Audit
    Service2 --> Audit
    Audit --> AuditDB

    style WAF fill:#ff6b6b,stroke:#c92a2a
    style OAuth fill:#51cf66,stroke:#2f9e44
    style JWT fill:#51cf66,stroke:#2f9e44
    style Audit fill:#339af0,stroke:#1864ab
```

### 7.2 Data Protection Layers

```mermaid
flowchart LR
    subgraph "Network Layer"
        TLS[TLS 1.3<br/>In Transit]
        VPC[VPC / Private Subnet]
        SG[Security Groups]
    end

    subgraph "Application Layer"
        Input[Input Validation]
        Sanitize[Data Sanitization]
        Encrypt[Field Encryption<br/>AES-256-GCM]
    end

    subgraph "Data Layer"
        PG_Encrypt[PostgreSQL<br/>Encryption at Rest]
        Redis_TLS[Redis TLS]
        S3_Encrypt[S3 SSE-KMS]
    end

    subgraph "Key Management"
        Vault[HashiCorp Vault /<br/>AWS KMS]
        Rotate[Key Rotation<br/>90 days]
    end

    subgraph "Compliance"
        HIPAA[HIPAA Controls]
        Audit[Audit Logging<br/>7 year retention]
        Consent[Consent Management]
    end

    TLS --> VPC
    VPC --> SG
    SG --> Input
    Input --> Sanitize
    Sanitize --> Encrypt
    Encrypt --> PG_Encrypt
    Encrypt --> Redis_TLS
    Encrypt --> S3_Encrypt

    Vault --> Encrypt
    Rotate --> Vault

    PG_Encrypt --> HIPAA
    Audit --> HIPAA
    Consent --> HIPAA

    style TLS fill:#51cf66,stroke:#2f9e44
    style Vault fill:#f59f00,stroke:#e67700
    style HIPAA fill:#339af0,stroke:#1864ab
```

---

## 8. Data Flow Diagrams

### 8.1 Patient Data Flow

```mermaid
flowchart TB
    subgraph "Data Sources"
        EHR[EHR Systems]
        LAB[Lab Systems]
        Claims[Claims Data]
        HIE[HIE Network]
    end

    subgraph "Data Ingestion"
        Connectors[EHR Connectors]
        Transform[FHIR Transformation]
        Validate[Data Validation]
        Dedup[Deduplication]
    end

    subgraph "Data Storage"
        FHIR_Store[(FHIR Data Store)]
        Cache[(Redis Cache)]
    end

    subgraph "Processing"
        CQL[CQL Engine]
        Measures[Measure Calculation]
        Gaps[Gap Detection]
        Analytics[Analytics Engine]
    end

    subgraph "Consumption"
        Portal[Clinical Portal]
        API[REST API]
        Reports[Reports / Exports]
        Alerts[Notifications]
    end

    EHR --> Connectors
    LAB --> Connectors
    Claims --> Connectors
    HIE --> Connectors

    Connectors --> Transform
    Transform --> Validate
    Validate --> Dedup
    Dedup --> FHIR_Store

    FHIR_Store --> CQL
    CQL --> Measures
    Measures --> Gaps
    Measures --> Analytics
    Measures --> Cache

    Cache --> Portal
    Cache --> API
    Analytics --> Reports
    Gaps --> Alerts

    style EHR fill:#999999,stroke:#666666
    style FHIR_Store fill:#336791,stroke:#1e3d5c,color:#ffffff
    style Cache fill:#dc382d,stroke:#a02a23,color:#ffffff
```

### 8.2 Event-Driven Architecture Flow

```mermaid
flowchart LR
    subgraph "Event Producers"
        QM[Quality Measure Service]
        CG[Care Gap Service]
        PS[Patient Service]
        AUTH[Auth Service]
    end

    subgraph "Event Bus"
        Kafka[Apache Kafka]
        Topics["Topics:<br/>- measure.evaluated<br/>- gap.identified<br/>- patient.updated<br/>- user.action"]
    end

    subgraph "Event Consumers"
        EVT[Event Processing Service]
        Analytics[Analytics Service]
        Notify[Notification Service]
        Audit[Audit Service]
    end

    subgraph "Failure Handling"
        DLQ[Dead Letter Queue]
        Retry[Retry Handler]
        Monitor[DLQ Monitor]
    end

    subgraph "Outputs"
        Dashboard[Real-time Dashboards]
        Email[Email Notifications]
        SMS[SMS Alerts]
        Logs[(Audit Logs)]
    end

    QM -->|measure.evaluated| Kafka
    CG -->|gap.identified| Kafka
    PS -->|patient.updated| Kafka
    AUTH -->|user.action| Kafka

    Kafka --> Topics
    Topics --> EVT
    Topics --> Analytics
    Topics --> Notify
    Topics --> Audit

    EVT -->|On Failure| DLQ
    DLQ --> Retry
    Retry --> EVT
    Monitor --> DLQ

    Analytics --> Dashboard
    Notify --> Email
    Notify --> SMS
    Audit --> Logs

    style Kafka fill:#231f20,stroke:#000000,color:#ffffff
    style DLQ fill:#ff6b6b,stroke:#c92a2a
```

---

## Diagram Index

| Diagram | Type | Level | Purpose |
|---------|------|-------|---------|
| System Context | C4 | 1 | HDIM in the world |
| Container | C4 | 2 | Technology architecture |
| Quality Measure Components | C4 | 3 | Internal structure |
| CQL Engine Components | C4 | 3 | Internal structure |
| Measure Evaluation | Sequence | - | Request flow |
| Care Gap Identification | Sequence | - | Batch processing |
| Authentication | Sequence | - | Login flow |
| Bulk Export | Sequence | - | Async export |
| Core Domain ERD | ERD | - | Data model |
| Event Processing ERD | ERD | - | DLQ model |
| Kubernetes Deployment | Deployment | - | Production infra |
| Docker Compose | Deployment | - | Local dev |
| Security Flow | Security | - | Auth/authz |
| Data Protection | Security | - | Encryption layers |
| Patient Data Flow | Data Flow | - | E2E data journey |
| Event Architecture | Data Flow | - | Kafka events |

---

## Rendering Instructions

### GitHub / GitLab
Mermaid diagrams render natively in markdown files.

### Local Rendering
```bash
# Install mermaid-cli
npm install -g @mermaid-js/mermaid-cli

# Render to PNG
mmdc -i ARCHITECTURE_DIAGRAMS.md -o architecture.png

# Render to SVG
mmdc -i ARCHITECTURE_DIAGRAMS.md -o architecture.svg

# Render to PDF
mmdc -i ARCHITECTURE_DIAGRAMS.md -o architecture.pdf
```

### Online Editors
- **Mermaid Live Editor:** https://mermaid.live
- **Draw.io:** https://app.diagrams.net (import mermaid)

---

**Generated by:** Architecture Review Team
**Last Updated:** December 5, 2025
**Version:** 1.0
