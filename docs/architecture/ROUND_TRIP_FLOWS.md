# HDIM Platform Round-Trip Flow Diagrams

**Purpose:** Systematic documentation of complete round-trip flows from UI button press through data sources, processing, and response  
**Version:** 1.0  
**Last Updated:** January 2025

---

## Table of Contents

1. [Overview](#overview)
2. [Evaluation Flow (CQL + FHIR)](#1-evaluation-flow-cql--fhir)
3. [Patient Search Flow](#2-patient-search-flow)
4. [Care Gap Detection Flow](#3-care-gap-detection-flow)
5. [Quality Measure Calculation Flow](#4-quality-measure-calculation-flow)
6. [Batch Evaluation Flow](#5-batch-evaluation-flow)
7. [Data Flow Visualization Flow](#6-data-flow-visualization-flow)
8. [Authentication & Authorization Flow](#7-authentication--authorization-flow)
9. [Event Processing Flow (Kafka)](#8-event-processing-flow-kafka)
10. [FHIR Resource Retrieval Flow](#9-fhir-resource-retrieval-flow)

---

## Overview

This document provides **complete round-trip flow diagrams** showing how user actions in the UI trigger backend processing, data retrieval, and responses. Each diagram shows:

- **UI Layer**: Button clicks, form submissions, user interactions
- **API Gateway**: Request routing, authentication, rate limiting
- **Service Layer**: Business logic, orchestration
- **Data Layer**: Database queries, FHIR requests, cache lookups
- **Event Layer**: Kafka message publishing/consumption
- **Response Path**: Data transformation, aggregation, UI updates

---

## 1. Evaluation Flow (CQL + FHIR)

**Scenario:** User clicks "Run Evaluation" button for a quality measure

```mermaid
sequenceDiagram
    participant User
    participant UI as Clinical Portal<br/>(Angular)
    participant Kong as Kong Gateway<br/>(Port 8000)
    participant Gateway as Gateway Service<br/>(Port 8001)
    participant CQL as CQL Engine Service<br/>(Port 8081)
    participant FHIR as FHIR Service<br/>(Port 8085)
    participant DB as PostgreSQL<br/>(CQL Library DB)
    participant Redis as Redis Cache<br/>(Template Cache)
    participant Kafka as Kafka<br/>(Event Bus)
    participant WS as WebSocket<br/>(Real-time Updates)
    participant Audit as Audit Service<br/>(HIPAA Logging)

    User->>UI: Click "Run Evaluation"<br/>(Measure: CDC, Patient: P123)
    UI->>UI: Validate form<br/>Show loading state
    UI->>Kong: POST /api/v1/cql/evaluations<br/>?libraryId=lib-123&patientId=P123<br/>Headers: X-Tenant-ID, Authorization
    
    Kong->>Kong: Validate JWT token<br/>Check rate limits<br/>Inject X-Auth-* headers
    Kong->>Gateway: Forward request<br/>with tenant context
    
    Gateway->>Gateway: Service discovery<br/>Load balancing<br/>Circuit breaker check
    Gateway->>CQL: POST /api/v1/cql/evaluations<br/>?libraryId=lib-123&patientId=P123
    
    Note over CQL: Start Data Flow Tracking
    CQL->>Audit: Start evaluation audit<br/>(evaluationId, tenantId, patientId)
    
    CQL->>DB: Query CQL Library<br/>SELECT * FROM cql_library<br/>WHERE id = lib-123
    DB-->>CQL: Library metadata<br/>(name, version, status)
    
    CQL->>Redis: Check template cache<br/>GET template:lib-123:tenant-001
    alt Cache Hit
        Redis-->>CQL: Cached template<br/>(ELM JSON)
        CQL->>Audit: Record step: CACHE_LOOKUP<br/>"Template loaded from cache"
    else Cache Miss
        Redis-->>CQL: Cache miss
        CQL->>DB: Load template from DB<br/>SELECT elm_json FROM measure_template
        DB-->>CQL: ELM template JSON
        CQL->>Redis: SET template:lib-123:tenant-001<br/>TTL: 24 hours
        CQL->>Audit: Record step: DATA_FETCH<br/>"Template loaded from database"
    end
    
    Note over CQL: Load Patient Context
    CQL->>Audit: Record step: DATA_FETCH<br/>"Load Patient FHIR Context"
    CQL->>FHIR: GET /fhir/Patient/P123<br/>Header: X-Tenant-ID
    FHIR->>FHIR: Query patient DB<br/>Apply tenant isolation
    FHIR-->>CQL: Patient Resource (FHIR R4)
    CQL->>Audit: Record step: DATA_FETCH<br/>Resources: [Patient]
    
    CQL->>FHIR: GET /fhir/Observation<br/>?patient=P123&code=4548-4<br/>(HbA1c observations)
    FHIR->>FHIR: Query observations DB<br/>Filter by LOINC code
    FHIR-->>CQL: Observation Bundle<br/>(FHIR R4 Bundle)
    CQL->>Audit: Record step: DATA_FETCH<br/>Resources: [Observation]
    
    CQL->>FHIR: GET /fhir/Condition<br/>?patient=P123&code=E11<br/>(Diabetes conditions)
    FHIR-->>CQL: Condition Bundle
    CQL->>Audit: Record step: DATA_FETCH<br/>Resources: [Condition]
    
    Note over CQL: Execute CQL Measure Logic
    CQL->>CQL: Evaluate denominator criteria<br/>(Age 18-75, Diabetes diagnosis)
    CQL->>Audit: Record step: LOGIC_DECISION<br/>"Patient in denominator: true"
    
    CQL->>CQL: Evaluate numerator criteria<br/>(HbA1c < 7% within 12 months)
    CQL->>Audit: Record step: LOGIC_DECISION<br/>"Patient in numerator: true"
    
    CQL->>CQL: Calculate compliance rate<br/>Build MeasureResult object
    CQL->>Audit: Record step: EXPRESSION_EVAL<br/>"Compliance: 100%"
    
    Note over CQL: Publish Results
    CQL->>Kafka: Publish evaluation.completed<br/>Topic: evaluation.completed<br/>Key: tenant-001<br/>Payload: {evaluationId, result, durationMs}
    CQL->>Audit: Record step: KAFKA_PUBLISH<br/>"Published to evaluation.completed"
    
    Kafka->>WS: Forward to WebSocket<br/>(if clients connected)
    WS-->>UI: Real-time update<br/>(evaluation completed)
    
    CQL->>DB: Save evaluation result<br/>UPDATE cql_evaluation<br/>SET status='SUCCESS', result=...
    DB-->>CQL: Update confirmed
    
    CQL->>Audit: Complete audit event<br/>Include all data flow steps
    Audit->>Kafka: Publish audit event<br/>Topic: healthdata.audit.events
    
    CQL-->>Gateway: HTTP 200 OK<br/>{id, status, result, durationMs}
    Gateway-->>Kong: Forward response
    Kong-->>UI: HTTP 200 OK<br/>with evaluation result
    
    UI->>UI: Parse response<br/>Update UI state<br/>Display results
    UI->>User: Show evaluation result<br/>"Patient is compliant"
    
    Note over UI,Audit: Complete round-trip:<br/>~500-2000ms depending on<br/>cache hits and FHIR queries
```

**Key Data Points:**
- **Total Steps**: 15-20 steps depending on cache hits
- **FHIR Queries**: 3-5 resource types (Patient, Observation, Condition, Procedure, MedicationRequest)
- **Database Queries**: 2-3 (library lookup, template load, result save)
- **Cache Lookups**: 1-2 (template cache, patient context cache)
- **Kafka Messages**: 2-3 (evaluation events, audit events)
- **WebSocket Updates**: Real-time step-by-step progress

---

## 2. Patient Search Flow

**Scenario:** User searches for a patient by name or MRN

```mermaid
sequenceDiagram
    participant User
    participant UI as Clinical Portal
    participant Kong as Kong Gateway
    participant Gateway as Gateway Service
    participant Patient as Patient Service<br/>(Port 8084)
    participant FHIR as FHIR Service<br/>(Port 8085)
    participant DB as PostgreSQL<br/>(Patient DB)
    participant Redis as Redis Cache<br/>(Search Cache)
    participant Elastic as Elasticsearch<br/>(Full-text Search)

    User->>UI: Type "John Smith"<br/>in patient search
    UI->>UI: Debounce input<br/>(300ms delay)
    UI->>Kong: GET /api/v1/patients/search<br/>?q=John+Smith<br/>Headers: X-Tenant-ID
    
    Kong->>Kong: Validate JWT<br/>Rate limit: 100 req/min
    Kong->>Gateway: Forward request
    
    Gateway->>Patient: GET /api/v1/patients/search<br/>?q=John+Smith
    
    Patient->>Redis: Check search cache<br/>GET search:tenant-001:John+Smith
    alt Cache Hit (< 5 min)
        Redis-->>Patient: Cached results
        Patient-->>Gateway: HTTP 200<br/>(cached results)
    else Cache Miss
        Patient->>Elastic: Search patients index<br/>query: {match: {name: "John Smith"}}<br/>filter: {tenantId: "tenant-001"}
        Elastic-->>Patient: Search results<br/>(patient IDs + scores)
        
        Patient->>DB: SELECT * FROM patient<br/>WHERE id IN (ids...)<br/>AND tenant_id = 'tenant-001'
        DB-->>Patient: Patient records<br/>(demographics, MRN, etc.)
        
        Patient->>FHIR: GET /fhir/Patient/{id}<br/>(for each patient)
        FHIR-->>Patient: Patient Resources<br/>(FHIR R4)
        
        Patient->>Patient: Transform to summary format<br/>Combine DB + FHIR data
        Patient->>Redis: Cache results<br/>SET search:tenant-001:John+Smith<br/>TTL: 5 minutes
        
        Patient-->>Gateway: HTTP 200<br/>(patient summaries)
    end
    
    Gateway-->>Kong: Forward response
    Kong-->>UI: HTTP 200<br/>(patient list)
    
    UI->>UI: Update autocomplete dropdown<br/>Show matching patients
    UI->>User: Display patient suggestions<br/>"John Smith (MRN: 12345)"
```

**Key Data Points:**
- **Cache Strategy**: 5-minute TTL for search results
- **Search Backend**: Elasticsearch for full-text search
- **Data Sources**: PostgreSQL (demographics) + FHIR (clinical data)
- **Response Time**: <100ms (cached) or 200-500ms (uncached)

---

## 3. Care Gap Detection Flow

**Scenario:** System automatically detects care gaps after evaluation

```mermaid
sequenceDiagram
    participant CQL as CQL Engine
    participant Kafka as Kafka<br/>(Event Bus)
    participant CareGap as Care Gap Service<br/>(Port 8086)
    participant FHIR as FHIR Service
    participant Quality as Quality Measure Service<br/>(Port 8087)
    participant DB as PostgreSQL<br/>(Care Gap DB)
    participant Notify as Notification Service
    participant UI as Clinical Portal<br/>(WebSocket)

    Note over CQL: Evaluation completes<br/>Patient NOT in numerator
    CQL->>Kafka: Publish evaluation.completed<br/>{patientId, measureId, inNumerator: false}
    
    Kafka->>CareGap: Consume evaluation.completed<br/>Topic: evaluation.completed<br/>Consumer Group: care-gap-detection
    
    CareGap->>CareGap: Parse event<br/>Check if gap exists
    alt Gap Detected
        CareGap->>FHIR: GET /fhir/Patient/{id}<br/>GET /fhir/Condition?patient={id}
        FHIR-->>CareGap: Patient context
        
        CareGap->>Quality: GET /api/v1/measures/{id}<br/>Get measure details
        Quality-->>CareGap: Measure metadata<br/>(description, category)
        
        CareGap->>CareGap: Determine gap type<br/>Calculate priority<br/>Generate recommendations
        
        CareGap->>DB: INSERT INTO care_gap<br/>(patient_id, measure_id, type,<br/>priority, status, created_at)
        DB-->>CareGap: Gap ID
        
        CareGap->>Kafka: Publish gap.identified<br/>Topic: gap.identified<br/>{gapId, patientId, measureId, type}
        
        Kafka->>Notify: Consume gap.identified
        Notify->>Notify: Determine notification rules<br/>Check user preferences
        Notify->>UI: WebSocket push<br/>{type: "CARE_GAP", gapId, patientId}
        
        Notify->>Notify: Send email/SMS<br/>(if configured)
        
        UI->>UI: Display care gap alert<br/>Show in notifications panel
    end
```

**Key Data Points:**
- **Trigger**: Evaluation result (patient not in numerator)
- **Processing**: Asynchronous via Kafka
- **Latency**: 100-500ms from evaluation to gap creation
- **Notifications**: Real-time WebSocket + email/SMS

---

## 4. Quality Measure Calculation Flow

**Scenario:** User triggers quality measure calculation for a patient

```mermaid
sequenceDiagram
    participant User
    participant UI as Clinical Portal
    participant Kong as Kong Gateway
    participant Quality as Quality Measure Service<br/>(Port 8087)
    participant CQL as CQL Engine Service<br/>(Port 8081)
    participant FHIR as FHIR Service<br/>(Port 8085)
    participant DB as PostgreSQL<br/>(Results DB)
    participant Kafka as Kafka
    participant Audit as Audit Service

    User->>UI: Click "Calculate Measure"<br/>(Measure: CDC, Patient: P123)
    UI->>Kong: POST /api/v1/quality-measure/calculate<br/>?patient=P123&measure=CDC
    
    Kong->>Quality: Forward request<br/>with tenant context
    
    Quality->>DB: Check existing result<br/>SELECT * FROM quality_measure_result<br/>WHERE patient_id=P123 AND measure_id=CDC<br/>AND calculation_date > NOW() - INTERVAL '1 day'
    
    alt Result Exists (< 24 hours)
        DB-->>Quality: Cached result
        Quality-->>UI: HTTP 200<br/>(cached result)
    else No Recent Result
        Quality->>CQL: POST /api/v1/cql/evaluate<br/>?library=CDC&patient=P123
        
        Note over CQL: Execute evaluation<br/>(see Evaluation Flow)
        CQL->>FHIR: Fetch patient data<br/>(Patient, Observation, Condition)
        FHIR-->>CQL: FHIR resources
        CQL->>CQL: Evaluate measure logic
        CQL-->>Quality: Evaluation result<br/>{inNumerator, inDenominator, complianceRate}
        
        Quality->>Quality: Transform result<br/>Calculate compliance percentage<br/>Determine outcome status
        
        Quality->>DB: INSERT INTO quality_measure_result<br/>(patient_id, measure_id, outcome,<br/>compliance_rate, calculation_date)
        DB-->>Quality: Result ID
        
        Quality->>Kafka: Publish measure.calculated<br/>Topic: measure.calculated<br/>{resultId, patientId, measureId, outcome}
        
        Quality->>Audit: Log calculation event<br/>(HIPAA compliance)
        
        Quality-->>UI: HTTP 200<br/>(calculation result)
    end
    
    UI->>UI: Display result<br/>Show compliance status
    UI->>User: "Patient is compliant"<br/>or "Care gap identified"
```

**Key Data Points:**
- **Caching**: 24-hour result cache
- **Dependencies**: CQL Engine + FHIR Service
- **Audit**: All calculations logged for HIPAA
- **Response Time**: 200-800ms (cached) or 1-3s (uncached)

---

## 5. Batch Evaluation Flow

**Scenario:** User runs evaluation for 1000 patients

```mermaid
sequenceDiagram
    participant User
    participant UI as Clinical Portal
    participant Kong as Kong Gateway
    participant CQL as CQL Engine Service
    participant FHIR as FHIR Service
    participant DB as PostgreSQL
    participant Redis as Redis Cache
    participant Kafka as Kafka
    participant WS as WebSocket<br/>(Progress Updates)
    participant ThreadPool as Thread Pool<br/>(Concurrent Execution)

    User->>UI: Click "Batch Evaluate"<br/>(Measure: CDC, 1000 patients)
    UI->>Kong: POST /api/v1/cql/evaluations/batch<br/>?libraryId=lib-123<br/>Body: {patientIds: [P1...P1000]}
    
    Kong->>CQL: Forward batch request
    
    CQL->>CQL: Generate batch ID<br/>Initialize progress tracking
    CQL->>Kafka: Publish batch.started<br/>{batchId, totalPatients: 1000}
    
    Kafka->>WS: Forward to WebSocket
    WS-->>UI: Real-time update<br/>"Batch started: 0/1000"
    
    CQL->>DB: Load template once<br/>(shared for all patients)
    DB-->>CQL: Template data
    
    CQL->>Redis: Cache template<br/>(for batch duration)
    
    par Concurrent Evaluation (10 threads)
        loop For each patient (1000 total)
            CQL->>ThreadPool: Submit evaluation task<br/>(patientId, measureId)
            
            ThreadPool->>FHIR: GET /fhir/Patient/{id}<br/>GET /fhir/Observation?patient={id}
            FHIR-->>ThreadPool: Patient data
            
            ThreadPool->>ThreadPool: Evaluate measure<br/>(using cached template)
            ThreadPool->>DB: Save result<br/>(async, non-blocking)
            
            ThreadPool->>Kafka: Publish evaluation.completed<br/>{patientId, result}
            
            ThreadPool->>CQL: Update progress counter
        end
    end
    
    CQL->>CQL: Aggregate batch results<br/>Calculate statistics
    CQL->>Kafka: Publish batch.progress<br/>{batchId, completed: 100, total: 1000}<br/>(every 10 patients or 5 seconds)
    
    Kafka->>WS: Forward progress updates
    WS-->>UI: Real-time progress<br/>"100/1000 completed (10%)"
    
    Note over CQL: Batch completes
    CQL->>Kafka: Publish batch.completed<br/>{batchId, total: 1000, success: 950, failed: 50}
    
    CQL-->>UI: HTTP 202 Accepted<br/>{batchId, status: "PROCESSING"}
    
    UI->>UI: Poll for completion<br/>or listen to WebSocket
    UI->>User: Show progress bar<br/>"950/1000 completed"
```

**Key Data Points:**
- **Concurrency**: 10-40 threads (2x CPU cores)
- **Template Caching**: Loaded once, reused for all patients
- **Progress Updates**: Every 10 patients or 5 seconds
- **Total Time**: ~5-15 minutes for 1000 patients
- **Throughput**: 100-200 evaluations/second

---

## 6. Data Flow Visualization Flow

**Scenario:** User clicks "Run with Data Flow" to see real-time processing

```mermaid
sequenceDiagram
    participant User
    participant UI as Clinical Portal
    participant DataFlow as Data Flow Component
    participant WS as WebSocket Service
    participant CQL as CQL Engine Service
    participant DataTracker as DataFlowTracker<br/>(ThreadLocal)
    participant StepPublisher as DataFlowStepPublisher
    participant WSHandler as WebSocket Handler
    participant Kafka as Kafka

    User->>UI: Click "Run with Data Flow"
    UI->>UI: Show data flow component<br/>Initialize WebSocket connection
    
    UI->>WS: Connect WebSocket<br/>ws://host/ws/evaluation-progress<br/>?evaluationId=eval-123&tenantId=tenant-001
    
    WS->>WSHandler: Establish connection<br/>Register session
    WSHandler-->>WS: Connection confirmed
    WS-->>UI: WebSocket connected
    
    UI->>CQL: POST /api/v1/cql/evaluations<br/>?libraryId=lib-123&patientId=P123
    
    Note over CQL: Start evaluation<br/>Initialize DataFlowTracker
    CQL->>DataTracker: startTracking(evaluationId)
    
    Note over CQL: Step 1: Load Template
    CQL->>DataTracker: recordStep("Load Template", "CACHE_LOOKUP")
    DataTracker->>StepPublisher: publishStep(step, evaluationId)
    StepPublisher->>WSHandler: broadcastDataFlowStep(step)
    WSHandler-->>WS: Send step update
    WS-->>UI: {type: "DATA_FLOW_STEP", data: step1}
    UI->>DataFlow: addStep(step1)
    DataFlow->>User: Display "Load Template" step<br/>with icon and timing
    
    Note over CQL: Step 2: Fetch FHIR Data
    CQL->>DataTracker: recordStep("Load Patient Context", "DATA_FETCH",<br/>resources: [Patient, Observation])
    DataTracker->>StepPublisher: publishStep(step2)
    StepPublisher->>WSHandler: broadcastDataFlowStep(step2)
    WSHandler-->>WS: Send step update
    WS-->>UI: {type: "DATA_FLOW_STEP", data: step2}
    UI->>DataFlow: addStep(step2)
    DataFlow->>User: Display "Load Patient Context"<br/>Show FHIR resources accessed
    
    Note over CQL: Step 3: Evaluate CQL
    CQL->>DataTracker: recordStep("Evaluate Denominator", "LOGIC_DECISION")
    DataTracker->>StepPublisher: publishStep(step3)
    StepPublisher->>WSHandler: broadcastDataFlowStep(step3)
    WSHandler-->>WS: Send step update
    WS-->>UI: {type: "DATA_FLOW_STEP", data: step3}
    UI->>DataFlow: addStep(step3)
    DataFlow->>User: Display "Evaluate Denominator"<br/>Show decision and reasoning
    
    Note over CQL: Step 4: Publish to Kafka
    CQL->>Kafka: Publish evaluation.completed
    CQL->>DataTracker: recordStep("Publish to Kafka", "KAFKA_PUBLISH")
    DataTracker->>StepPublisher: publishStep(step4)
    StepPublisher->>WSHandler: broadcastDataFlowStep(step4)
    WSHandler-->>WS: Send step update
    WS-->>UI: {type: "DATA_FLOW_STEP", data: step4}
    UI->>DataFlow: addStep(step4)
    DataFlow->>User: Display "Publish to Kafka"<br/>Show topic and partition info
    
    Note over CQL: Evaluation completes
    CQL->>DataTracker: getSteps()<br/>clearTracking()
    CQL-->>UI: HTTP 200<br/>{evaluation result}
    
    UI->>DataFlow: Mark processing complete
    DataFlow->>User: Show final statistics<br/>"4 steps, 3 FHIR resources, 1 Kafka message"
```

**Key Data Points:**
- **Real-time Updates**: Steps published as they occur
- **WebSocket Latency**: <50ms from step to UI
- **Step Types**: DATA_FETCH, CQL_EXECUTION, LOGIC_DECISION, KAFKA_PUBLISH
- **Visualization**: Grouped by type (FHIR, Kafka, CQL)

---

## 7. Authentication & Authorization Flow

**Scenario:** User logs in and accesses protected resource

```mermaid
sequenceDiagram
    participant User
    participant UI as Clinical Portal
    participant Kong as Kong Gateway
    participant Auth as Auth Service<br/>(Port 8082)
    participant DB as PostgreSQL<br/>(User DB)
    participant Redis as Redis Cache<br/>(Session Cache)
    participant Gateway as Gateway Service
    participant Service as Backend Service

    User->>UI: Enter credentials<br/>Click "Login"
    UI->>Kong: POST /api/v1/auth/login<br/>Body: {username, password}
    
    Kong->>Auth: Forward login request
    
    Auth->>DB: SELECT * FROM user<br/>WHERE username = ?<br/>AND tenant_id = ?
    DB-->>Auth: User record<br/>(hashed password, roles)
    
    Auth->>Auth: Verify password<br/>bcrypt.compare()
    
    alt Invalid Credentials
        Auth-->>Kong: HTTP 401 Unauthorized
        Kong-->>UI: Login failed
        UI->>User: Show error message
    else Valid Credentials
        Auth->>Auth: Generate JWT token<br/>{userId, tenantId, roles, exp: 1h}
        Auth->>Redis: Cache session<br/>SET session:{token}<br/>TTL: 1 hour
        
        Auth->>DB: INSERT INTO audit_log<br/>(action: "LOGIN", userId, timestamp)
        
        Auth-->>Kong: HTTP 200 OK<br/>{token, user, roles}
        Kong-->>UI: Login success<br/>with JWT token
        
        UI->>UI: Store token<br/>localStorage.setItem('token')
        UI->>User: Redirect to dashboard
    end
    
    Note over User,Service: Subsequent Requests
    User->>UI: Click "View Patients"
    UI->>Kong: GET /api/v1/patients<br/>Header: Authorization: Bearer {token}
    
    Kong->>Kong: Extract JWT token<br/>Validate signature<br/>Check expiration
    Kong->>Redis: Check session cache<br/>GET session:{token}
    
    alt Token Invalid or Expired
        Kong-->>UI: HTTP 401 Unauthorized
        UI->>UI: Clear token<br/>Redirect to login
    else Token Valid
        Kong->>Kong: Extract claims<br/>{userId, tenantId, roles}
        Kong->>Kong: Inject headers<br/>X-Auth-User-Id: {userId}<br/>X-Auth-Tenant-Id: {tenantId}<br/>X-Auth-Roles: {roles}
        
        Kong->>Gateway: Forward request<br/>with auth headers
        
        Gateway->>Service: GET /api/v1/patients<br/>Header: X-Auth-Tenant-Id
        
        Service->>Service: Trust gateway headers<br/>Apply tenant isolation<br/>Check role permissions
        
        Service->>DB: SELECT * FROM patient<br/>WHERE tenant_id = 'tenant-001'<br/>(from X-Auth-Tenant-Id header)
        DB-->>Service: Patient records
        
        Service-->>Gateway: HTTP 200<br/>(patient list)
        Gateway-->>Kong: Forward response
        Kong-->>UI: HTTP 200<br/>(patient data)
        
        UI->>UI: Render patient list
        UI->>User: Display patients
    end
```

**Key Data Points:**
- **JWT Expiration**: 1 hour (configurable)
- **Session Cache**: Redis with 1-hour TTL
- **Gateway Trust**: Services trust X-Auth-* headers
- **Tenant Isolation**: Enforced at database query level

---

## 8. Event Processing Flow (Kafka)

**Scenario:** Evaluation event triggers downstream processing

```mermaid
sequenceDiagram
    participant CQL as CQL Engine
    participant Kafka as Kafka<br/>(Event Bus)
    participant Consumer1 as Care Gap Service<br/>(Consumer)
    participant Consumer2 as Analytics Service<br/>(Consumer)
    participant Consumer3 as Notification Service<br/>(Consumer)
    participant DLQ as Dead Letter Queue
    participant DB as PostgreSQL
    participant UI as Clinical Portal<br/>(WebSocket)

    CQL->>Kafka: Publish evaluation.completed<br/>Topic: evaluation.completed<br/>Partition: 0 (by tenantId)<br/>Key: tenant-001<br/>Payload: {evaluationId, patientId, result}
    
    Kafka->>Kafka: Store message<br/>Replicate to 3 brokers<br/>Commit offset
    
    par Parallel Consumption
        Kafka->>Consumer1: Consume message<br/>Consumer Group: care-gap-detection<br/>Offset: 12345
        
        Consumer1->>Consumer1: Parse event<br/>Check if gap exists
        alt Gap Detected
            Consumer1->>DB: INSERT INTO care_gap
            DB-->>Consumer1: Gap created
            Consumer1->>Kafka: Publish gap.identified
        else No Gap
            Consumer1->>Consumer1: Skip processing
        end
        
        Consumer1->>Kafka: Commit offset: 12346
        
    and
        Kafka->>Consumer2: Consume message<br/>Consumer Group: analytics-processing
        
        Consumer2->>Consumer2: Aggregate metrics<br/>Update statistics
        Consumer2->>DB: UPDATE analytics_summary<br/>SET evaluation_count = evaluation_count + 1
        DB-->>Consumer2: Updated
        Consumer2->>Kafka: Commit offset
        
    and
        Kafka->>Consumer3: Consume message<br/>Consumer Group: notification-service
        
        Consumer3->>Consumer3: Check notification rules<br/>User preferences
        alt Notification Required
            Consumer3->>UI: WebSocket push<br/>{type: "EVALUATION_COMPLETE"}
            Consumer3->>Consumer3: Send email<br/>(if configured)
        end
        Consumer3->>Kafka: Commit offset
    end
    
    alt Consumer Failure
        Consumer1->>Consumer1: Processing error<br/>Exception thrown
        Consumer1->>DLQ: Send to DLQ<br/>Topic: evaluation.completed.dlq<br/>Reason: "Database connection failed"
        
        Note over DLQ: DLQ Monitor<br/>Retries failed messages<br/>Alerts ops team
    end
```

**Key Data Points:**
- **Partitioning**: By tenantId for ordering
- **Consumer Groups**: Independent processing per service
- **At-Least-Once Delivery**: Messages may be processed multiple times
- **DLQ**: Failed messages go to dead letter queue
- **Parallelism**: Multiple consumers process same message

---

## 9. FHIR Resource Retrieval Flow

**Scenario:** Service needs to fetch FHIR resources for evaluation

```mermaid
sequenceDiagram
    participant CQL as CQL Engine
    participant FHIR as FHIR Service<br/>(Port 8085)
    participant Cache as Redis Cache<br/>(FHIR Cache)
    participant DB as PostgreSQL<br/>(FHIR Resource DB)
    participant HAPI as HAPI FHIR<br/>(Validation)
    participant Audit as Audit Service

    CQL->>FHIR: GET /fhir/Patient/P123<br/>Header: X-Tenant-ID: tenant-001
    
    FHIR->>Cache: Check cache<br/>GET fhir:Patient:P123:tenant-001
    alt Cache Hit (< 5 min)
        Cache-->>FHIR: Cached resource<br/>(FHIR R4 JSON)
        FHIR->>Audit: Log access<br/>(cached read)
        FHIR-->>CQL: HTTP 200<br/>(cached resource)
    else Cache Miss
        FHIR->>DB: SELECT resource_data FROM fhir_resource<br/>WHERE resource_type = 'Patient'<br/>AND resource_id = 'P123'<br/>AND tenant_id = 'tenant-001'
        
        alt Resource Not Found
            DB-->>FHIR: No rows
            FHIR->>HAPI: Validate request<br/>Check resource ID format
            FHIR-->>CQL: HTTP 404 Not Found
        else Resource Found
            DB-->>FHIR: Resource JSON<br/>(stored as JSONB)
            
            FHIR->>HAPI: Validate resource<br/>HapiFhirValidator.validate()
            HAPI-->>FHIR: Validation result
            
            alt Validation Failed
                FHIR->>Audit: Log validation error
                FHIR-->>CQL: HTTP 422 Unprocessable Entity<br/>{errors: [...]}
            else Validation Passed
                FHIR->>Cache: Cache resource<br/>SET fhir:Patient:P123:tenant-001<br/>TTL: 5 minutes
                
                FHIR->>Audit: Log access<br/>(database read, tenant, resource type)
                
                FHIR-->>CQL: HTTP 200 OK<br/>(FHIR R4 Patient resource)
            end
        end
    end
    
    CQL->>CQL: Use resource in evaluation<br/>Extract data (age, gender, etc.)
```

**Key Data Points:**
- **Cache TTL**: 5 minutes (HIPAA compliance)
- **Validation**: HAPI FHIR validates all resources
- **Storage**: JSONB in PostgreSQL
- **Tenant Isolation**: Enforced at query level
- **Audit**: All reads logged (cached and uncached)

---

## Summary

### Common Patterns Across All Flows

1. **Request Path**: UI → Kong → Gateway → Service
2. **Authentication**: JWT validation at Kong, header injection
3. **Caching**: Redis for templates, search results, FHIR resources
4. **Database**: PostgreSQL for persistent data
5. **Events**: Kafka for async processing and notifications
6. **Real-time**: WebSocket for progress updates
7. **Audit**: All operations logged for HIPAA compliance
8. **Tenant Isolation**: Enforced at every layer

### Performance Characteristics

| Flow Type | Typical Latency | Cache Hit Latency | Throughput |
|-----------|----------------|-------------------|------------|
| Evaluation | 500-2000ms | 200-500ms | 10-50/sec |
| Patient Search | 200-500ms | <100ms | 100/sec |
| Batch Evaluation | 5-15 min (1000 patients) | N/A | 100-200/sec |
| Care Gap Detection | 100-500ms (async) | N/A | 1000/sec |
| FHIR Retrieval | 50-200ms | <20ms | 500/sec |

### Data Flow Summary

```
User Action
    ↓
UI Component (Angular/React)
    ↓
Kong Gateway (Auth, Rate Limit)
    ↓
Gateway Service (Routing, Load Balancing)
    ↓
Backend Service (Business Logic)
    ↓
├─→ PostgreSQL (Persistent Data)
├─→ Redis (Cache)
├─→ FHIR Service (Clinical Data)
├─→ Kafka (Events)
└─→ WebSocket (Real-time Updates)
    ↓
Response Path (Reverse)
    ↓
UI Update
```

---

**Next Steps:**
- Add more flow diagrams for specific scenarios
- Create interactive diagrams (using Mermaid Live Editor)
- Generate sequence diagrams for error scenarios
- Document retry and failure handling flows
