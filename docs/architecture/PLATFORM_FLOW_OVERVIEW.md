# HDIM Platform Flow Overview

**Purpose:** High-level visual overview of platform flows from button press to data processing  
**Version:** 1.0  
**Last Updated:** January 2025

---

## Complete Round-Trip Flow Architecture

```mermaid
graph TB
    subgraph "User Interface Layer"
        UI1[Clinical Portal<br/>Angular]
        UI2[Admin Portal<br/>Angular]
        UI3[Patient Portal<br/>React]
        UI4[Mobile Apps<br/>React Native]
    end

    subgraph "API Gateway Layer"
        Kong[Kong Gateway<br/>Port 8000<br/>JWT Auth, Rate Limiting]
        Gateway[Gateway Service<br/>Port 8001<br/>Routing, Load Balancing]
    end

    subgraph "Service Layer"
        subgraph "Core Clinical"
            CQL[CQL Engine<br/>Port 8081<br/>Measure Evaluation]
            Quality[Quality Measure<br/>Port 8087<br/>HEDIS Calculation]
            CareGap[Care Gap<br/>Port 8086<br/>Gap Detection]
        end
        
        subgraph "Data Services"
            FHIR[FHIR Service<br/>Port 8085<br/>R4 Resources]
            Patient[Patient Service<br/>Port 8084<br/>Demographics]
        end
        
        subgraph "Integration"
            EHR[EHR Connector<br/>Port 8092<br/>EHR Integration]
            CDR[CDR Processor<br/>Port 8093<br/>Data Transformation]
        end
    end

    subgraph "Data Layer"
        PostgreSQL[(PostgreSQL<br/>Primary Database)]
        Redis[(Redis<br/>Cache & Sessions)]
        Elastic[(Elasticsearch<br/>Full-text Search)]
    end

    subgraph "Event Layer"
        Kafka[Apache Kafka<br/>Event Bus]
        WS[WebSocket<br/>Real-time Updates]
    end

    subgraph "External Systems"
        EHR_Ext[External EHRs<br/>Epic, Cerner]
        Lab[Laboratory Systems]
        Pharmacy[Pharmacy Systems]
    end

    %% User Interactions
    UI1 -->|HTTPS| Kong
    UI2 -->|HTTPS| Kong
    UI3 -->|HTTPS| Kong
    UI4 -->|HTTPS| Kong

    %% Gateway Flow
    Kong -->|Validate JWT<br/>Rate Limit| Gateway
    Gateway -->|Route Request| CQL
    Gateway -->|Route Request| Quality
    Gateway -->|Route Request| CareGap
    Gateway -->|Route Request| FHIR
    Gateway -->|Route Request| Patient

    %% Service to Data Layer
    CQL -->|Query| PostgreSQL
    CQL -->|Cache| Redis
    CQL -->|Search| Elastic
    Quality -->|Query| PostgreSQL
    FHIR -->|Query| PostgreSQL
    FHIR -->|Cache| Redis
    Patient -->|Query| PostgreSQL
    Patient -->|Search| Elastic

    %% Service Interactions
    CQL -->|Fetch Data| FHIR
    Quality -->|Evaluate| CQL
    CareGap -->|Consume Events| Kafka

    %% Event Publishing
    CQL -->|Publish Events| Kafka
    Quality -->|Publish Events| Kafka
    CareGap -->|Publish Events| Kafka
    FHIR -->|Publish Events| Kafka

    %% Real-time Updates
    Kafka -->|Forward| WS
    WS -->|Push Updates| UI1
    WS -->|Push Updates| UI2

    %% External Integrations
    EHR -->|FHIR R4| EHR_Ext
    CDR -->|Transform| Lab
    CDR -->|Transform| Pharmacy

    %% Response Path (dashed)
    PostgreSQL -.->|Results| CQL
    Redis -.->|Cached Data| FHIR
    Kafka -.->|Events| CareGap
    WS -.->|Updates| UI1

    style Kong fill:#1E3A5F,stroke:#000,color:#fff
    style Gateway fill:#00A9A5,stroke:#000,color:#fff
    style Kafka fill:#231f20,stroke:#000,color:#fff
    style PostgreSQL fill:#336791,stroke:#000,color:#fff
    style Redis fill:#DC382D,stroke:#000,color:#fff
```

---

## Request Flow Pattern

```mermaid
sequenceDiagram
    autonumber
    participant U as User
    participant UI as UI Component
    participant K as Kong Gateway
    participant G as Gateway Service
    participant S as Backend Service
    participant D as Data Layer
    participant E as Event Layer
    participant WS as WebSocket

    U->>UI: Button Click / Action
    UI->>UI: Validate Input<br/>Show Loading State
    UI->>K: HTTP Request<br/>+ JWT Token
    
    K->>K: Validate JWT<br/>Check Rate Limits
    K->>G: Forward Request<br/>+ Auth Headers
    
    G->>G: Service Discovery<br/>Load Balancing
    G->>S: Route to Service<br/>+ Tenant Context
    
    S->>D: Query Database<br/>or Cache Lookup
    D-->>S: Return Data
    
    S->>S: Business Logic<br/>Processing
    
    S->>E: Publish Event<br/>(if async)
    E->>WS: Forward Update
    WS-->>UI: Real-time Update
    
    S-->>G: HTTP Response<br/>+ Data
    G-->>K: Forward Response
    K-->>UI: HTTP Response
    
    UI->>UI: Update State<br/>Render UI
    UI->>U: Display Results
```

---

## Data Processing Workflow

```mermaid
flowchart TD
    Start[User Action] --> Validate{Input Valid?}
    Validate -->|No| Error[Show Error]
    Validate -->|Yes| Auth{Authenticated?}
    
    Auth -->|No| Login[Redirect to Login]
    Auth -->|Yes| Route[Route to Service]
    
    Route --> Cache{Check Cache}
    Cache -->|Hit| ReturnCache[Return Cached Data]
    Cache -->|Miss| Query[Query Database]
    
    Query --> Process[Process Business Logic]
    Process --> External{Need External Data?}
    
    External -->|Yes| FetchFHIR[Fetch from FHIR Service]
    External -->|Yes| FetchEHR[Fetch from EHR]
    External -->|No| Continue[Continue Processing]
    
    FetchFHIR --> Continue
    FetchEHR --> Continue
    
    Continue --> Evaluate{Need CQL Evaluation?}
    Evaluate -->|Yes| CQL[Execute CQL Measure]
    Evaluate -->|No| Transform[Transform Data]
    
    CQL --> Transform
    Transform --> Publish{Publish Event?}
    
    Publish -->|Yes| Kafka[Publish to Kafka]
    Publish -->|No| Save[Save to Database]
    
    Kafka --> Save
    Save --> CacheUpdate[Update Cache]
    CacheUpdate --> Response[Return Response]
    ReturnCache --> Response
    
    Response --> UI[Update UI]
    UI --> End[User Sees Result]
    
    Error --> End
    Login --> End
    
    style Start fill:#4CAF50,stroke:#000,color:#fff
    style End fill:#4CAF50,stroke:#000,color:#fff
    style Error fill:#f44336,stroke:#000,color:#fff
    style Kafka fill:#231f20,stroke:#000,color:#fff
    style CQL fill:#2196F3,stroke:#000,color:#fff
```

---

## Component Interaction Matrix

| User Action | Primary Service | Data Sources | Events Published | Response Time |
|------------|----------------|--------------|------------------|---------------|
| Run Evaluation | CQL Engine | FHIR, PostgreSQL, Redis | evaluation.completed | 500-2000ms |
| Search Patient | Patient Service | PostgreSQL, Elasticsearch, Redis | - | 100-500ms |
| Calculate Measure | Quality Measure | CQL Engine, PostgreSQL | measure.calculated | 200-800ms |
| View Care Gaps | Care Gap Service | PostgreSQL, Kafka (events) | gap.viewed | 100-300ms |
| Batch Evaluate | CQL Engine | FHIR, PostgreSQL, Redis | batch.progress, batch.completed | 5-15 min |
| Fetch FHIR Resource | FHIR Service | PostgreSQL, Redis | resource.accessed | 20-200ms |

---

## Key Flow Characteristics

### 1. **Synchronous Flows** (User waits for response)
- Patient search
- Single evaluation
- FHIR resource retrieval
- Authentication

### 2. **Asynchronous Flows** (Background processing)
- Batch evaluations
- Care gap detection
- Event processing
- Data enrichment

### 3. **Real-time Flows** (WebSocket updates)
- Evaluation progress
- Data flow visualization
- Care gap alerts
- Notification delivery

### 4. **Hybrid Flows** (Sync + Async)
- Evaluation with real-time progress
- Batch operations with progress updates
- Long-running queries with status updates

---

## Performance Optimization Strategies

1. **Caching Layers**
   - Redis: Templates (24h), FHIR resources (5min), search results (5min)
   - Application: In-memory caches for frequently accessed data

2. **Database Optimization**
   - Indexed queries by tenant_id
   - Connection pooling
   - Read replicas for analytics

3. **Async Processing**
   - Kafka for non-blocking operations
   - Thread pools for concurrent evaluations
   - Background jobs for heavy processing

4. **Real-time Updates**
   - WebSocket for progress tracking
   - Server-Sent Events (SSE) for notifications
   - Polling fallback for WebSocket failures

---

## Error Handling Patterns

```mermaid
flowchart TD
    Request[Incoming Request] --> Try{Try Processing}
    Try -->|Success| Success[Return Success]
    Try -->|Error| Catch[Error Handler]
    
    Catch --> Retry{Retryable?}
    Retry -->|Yes| RetryLogic[Exponential Backoff<br/>Max 3 retries]
    Retry -->|No| LogError[Log Error]
    
    RetryLogic -->|Success| Success
    RetryLogic -->|Failed| DLQ[Send to DLQ]
    
    LogError --> DLQ
    DLQ --> Alert[Alert Operations]
    Alert --> Monitor[Monitor & Investigate]
    
    style Error fill:#f44336,stroke:#000,color:#fff
    style DLQ fill:#ff9800,stroke:#000,color:#fff
    style Success fill:#4CAF50,stroke:#000,color:#fff
```

---

## Related Documentation

- **[Round-Trip Flows](./ROUND_TRIP_FLOWS.md)**: Detailed sequence diagrams for each workflow
- **[Architecture Diagrams](./diagrams/ARCHITECTURE_DIAGRAMS.md)**: C4 models and system diagrams
- **[System Architecture](./SYSTEM_ARCHITECTURE.md)**: High-level system overview
- **[Service Documentation](../services/)**: Individual service specifications

---

**Last Updated:** January 2025  
**Maintained By:** Platform Architecture Team
