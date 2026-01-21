# Platform Architecture Overview

**Health Data In Motion (HDIM)** - Enterprise Healthcare Interoperability Platform

---

## System Architecture

```mermaid
graph TB
    subgraph Frontend[Frontend Layer]
        ClinicalPortal[Clinical Portal<br/>React + TypeScript]
        PatientPortal[Patient Portal<br/>React + TypeScript]
        AdminPortal[Admin Portal<br/>Angular]
        AgentStudio[Agent Studio<br/>React + TypeScript]
        AnalyticsPortal[Analytics Portal<br/>React + TypeScript]
        DeveloperPortal[Developer Portal<br/>React + TypeScript]
        MobileApps[Mobile Apps<br/>React Native]
    end
    
    subgraph APIGateway[API Gateway Layer]
        Kong[Kong Gateway<br/>Rate Limiting, Auth]
        GatewayService[Gateway Service<br/>Main API Gateway]
        GatewayAdmin[Gateway Admin<br/>Admin Routes]
        GatewayClinical[Gateway Clinical<br/>Clinical Routes]
        GatewayFHIR[Gateway FHIR<br/>FHIR Routes]
        GraphQL[GraphQL Gateway<br/>Schema Stitching]
    end
    
    subgraph CoreServices[Core Clinical Services]
        FHIR[FHIR Service<br/>R4 Resources]
        CQL[CQL Engine<br/>Measure Evaluation]
        CareGap[Care Gap Service<br/>Gap Detection]
        QualityMeasure[Quality Measure<br/>HEDIS/CMS]
        Patient[Patient Service<br/>Demographics]
        HCC[HCC Service<br/>RAF Calculation]
        Consent[Consent Service<br/>HIPAA Compliance]
    end
    
    subgraph AIServices[AI & Agent Services]
        AIAssistant[AI Assistant<br/>Claude API]
        AgentRuntime[Agent Runtime<br/>Multi-LLM]
        AgentBuilder[Agent Builder<br/>No-Code Platform]
        Predictive[Predictive Analytics<br/>Risk Models]
        DataEnrichment[Data Enrichment<br/>AI-Powered NLP]
        Guardrails[Clinical Guardrails<br/>PHI Protection]
    end
    
    subgraph WorkflowServices[Workflow Services]
        PriorAuth[Prior Authorization<br/>CMS-0057-F]
        Approval[Approval Workflows<br/>HITL]
        PayerWorkflows[Payer Workflows<br/>STAR Ratings]
        Notification[Notifications<br/>Email/SMS/Push]
        EventProcessing[Event Processing<br/>Kafka Streams]
        EventRouter[Event Router<br/>Event Routing]
        MigrationWorkflow[Migration Workflow<br/>Data Migration]
    end
    
    subgraph IntegrationServices[Integration Services]
        EHRConnector[EHR Connector<br/>Epic, Cerner]
        CDRProcessor[CDR Processor<br/>HL7 v2, FHIR]
        CMSConnector[CMS Connector<br/>BCDA, DPC]
        QRDAExport[QRDA Export<br/>Quality Reporting]
        ECR[ECR Service<br/>Electronic Case Reporting]
    end
    
    subgraph AnalyticsData[Analytics & Data Services]
        Analytics[Analytics Service<br/>Real-time Dashboards]
        SDOH[SDOH Service<br/>Social Determinants]
        CostAnalysis[Cost Analysis<br/>Cost Analytics]
    end
    
    subgraph DataLayer[Data & Infrastructure]
        PostgreSQL[(PostgreSQL<br/>Primary Database)]
        Redis[(Redis<br/>Cache & Sessions)]
        Kafka[Apache Kafka<br/>Event Streaming]
        S3[S3 Storage<br/>Documents & Files]
        Elasticsearch[(Elasticsearch<br/>Search & Logs)]
    end
    
    subgraph Observability[Observability Stack]
        Prometheus[Prometheus<br/>Metrics]
        Grafana[Grafana<br/>Dashboards]
        Jaeger[Jaeger<br/>Distributed Tracing]
        ELK[ELK Stack<br/>Log Aggregation]
    end
    
    subgraph AuditInfra[Audit Infrastructure]
        AuditPublisher[Audit Publisher<br/>Event Publishing]
        AuditConsumer[Audit Consumer<br/>Event Processing]
        AuditStore[(Audit Store<br/>7-Year Retention)]
        AuditAPI[Audit Query API<br/>Compliance Queries]
    end
    
    subgraph Supporting[Supporting Services]
        Documentation[Documentation<br/>Doc Management]
        DemoSeeding[Demo Seeding<br/>Test Data]
    end
    
    Frontend --> APIGateway
    APIGateway --> CoreServices
    APIGateway --> AIServices
    APIGateway --> WorkflowServices
    APIGateway --> IntegrationServices
    APIGateway --> AnalyticsData
    
    CoreServices --> DataLayer
    AIServices --> DataLayer
    WorkflowServices --> DataLayer
    IntegrationServices --> DataLayer
    AnalyticsData --> DataLayer
    
    CoreServices -.->|Audit Events| AuditPublisher
    AIServices -.->|Audit Events| AuditPublisher
    WorkflowServices -.->|Audit Events| AuditPublisher
    IntegrationServices -.->|Audit Events| AuditPublisher
    
    AuditPublisher --> Kafka
    Kafka --> AuditConsumer
    AuditConsumer --> AuditStore
    AuditStore --> AuditAPI
    
    CoreServices --> Observability
    AIServices --> Observability
    WorkflowServices --> Observability
    IntegrationServices --> Observability
```

---

## Technology Stack

### Frontend Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| **React** | 18.x | Clinical, Patient, Agent Studio portals |
| **Angular** | 17.x | Admin portal (existing) |
| **React Native** | 0.73.x | iOS & Android mobile apps |
| **TypeScript** | 5.x | Type safety across all frontends |
| **Material-UI (MUI)** | 5.x | React component library |
| **Recharts** | 2.x | Data visualization |
| **Zustand** | 4.x | State management |
| **React Query** | 5.x | Server state management |
| **Vite** | 5.x | Build tool for React apps |

### Backend Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21 LTS | Primary backend language |
| **Spring Boot** | 3.3.x | Microservices framework |
| **Spring Cloud** | 2023.x | Service discovery, config |
| **HAPI FHIR** | 7.6.0 | FHIR R4 implementation |
| **OpenCDS CQL Engine** | Latest | Clinical Quality Language |
| **Kafka** | 3.6.x | Event streaming |
| **PostgreSQL** | 16.x | Primary database |
| **Redis** | 7.x | Caching & sessions |
| **Gradle** | 8.x | Build automation |

### AI & ML Technologies

| Technology | Purpose |
|------------|---------|
| **Anthropic Claude** | Primary LLM (Claude 3.5 Sonnet) |
| **Azure OpenAI** | Secondary LLM (GPT-4 Turbo) |
| **AWS Bedrock** | Tertiary LLM provider |
| **Scikit-learn** | Predictive analytics models |
| **TensorFlow** | Deep learning models (future) |

### Infrastructure & DevOps

| Technology | Purpose |
|------------|---------|
| **Docker** | Containerization |
| **Kubernetes** | Container orchestration |
| **Helm** | Kubernetes package manager |
| **Terraform** | Infrastructure as Code |
| **GitHub Actions** | CI/CD pipelines |
| **Prometheus** | Metrics collection |
| **Grafana** | Monitoring dashboards |
| **Jaeger** | Distributed tracing |
| **ELK Stack** | Log aggregation |

---

## Service Communication Patterns

### Synchronous Communication (REST)

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant FHIR
    participant CQL
    participant CareGap
    
    Client->>Gateway: GET /patients/{id}/care-gaps
    Gateway->>FHIR: GET /Patient/{id}
    FHIR-->>Gateway: Patient resource
    Gateway->>CQL: POST /evaluate-measures
    CQL->>FHIR: GET /Observation?patient={id}
    FHIR-->>CQL: Observations
    CQL-->>Gateway: Measure results
    Gateway->>CareGap: POST /identify-gaps
    CareGap-->>Gateway: Care gaps
    Gateway-->>Client: Care gaps response
```

### Asynchronous Communication (Kafka)

```mermaid
graph LR
    FHIR[FHIR Service] -->|Patient Created| Kafka[Kafka Topic:<br/>fhir.resources.patient]
    Kafka --> CareGap[Care Gap Service]
    Kafka --> Notification[Notification Service]
    Kafka --> Audit[Audit Service]
    Kafka --> Analytics[Analytics Service]
    
    CareGap -->|Gap Detected| Kafka2[Kafka Topic:<br/>caregap.detected]
    Kafka2 --> Notification2[Notification Service]
    Kafka2 --> Workflow[Workflow Service]
```

---

## Data Architecture

### Database Schema Organization

```mermaid
erDiagram
    PATIENT ||--o{ OBSERVATION : has
    PATIENT ||--o{ CONDITION : has
    PATIENT ||--o{ CARE_GAP : has
    PATIENT ||--o{ QUALITY_MEASURE_RESULT : has
    PATIENT {
        uuid id PK
        string tenant_id
        string mrn
        string name
        date birth_date
        string gender
    }
    OBSERVATION {
        uuid id PK
        uuid patient_id FK
        string code
        string value
        timestamp effective_date
    }
    CONDITION {
        uuid id PK
        uuid patient_id FK
        string code
        string clinical_status
        timestamp onset_date
    }
    CARE_GAP {
        uuid id PK
        uuid patient_id FK
        string measure_id
        string status
        string priority
        timestamp due_date
    }
    QUALITY_MEASURE_RESULT {
        uuid id PK
        uuid patient_id FK
        string measure_id
        boolean measure_met
        timestamp evaluated_at
    }
```

### Kafka Topic Structure

| Topic | Partitions | Retention | Purpose |
|-------|------------|-----------|---------|
| `fhir.resources.patient` | 3 | 7 days | Patient resource changes |
| `fhir.resources.observation` | 3 | 7 days | Observation changes |
| `caregap.detected` | 3 | 30 days | Care gaps detected |
| `quality.measure.result` | 3 | 30 days | Measure evaluation results |
| `audit.events` | 5 | 7 years | Audit trail (HIPAA) |
| `ai.agent.decisions` | 3 | 7 years | AI agent decisions |
| `priorauth.status.changed` | 3 | 30 days | Prior auth status updates |
| `notification.outbound` | 2 | 7 days | Outbound notifications |

---

## Security Architecture

### Authentication Flow

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant Gateway
    participant IDP as Identity Provider<br/>(Okta/Azure AD)
    participant Backend
    
    User->>Frontend: Login
    Frontend->>IDP: OAuth 2.0 / SAML
    IDP->>IDP: Authenticate User
    IDP-->>Frontend: JWT Token
    Frontend->>Gateway: API Request + JWT
    Gateway->>Gateway: Validate JWT
    Gateway->>Backend: Request + User Context
    Backend->>Backend: RBAC Check
    Backend-->>Gateway: Response
    Gateway-->>Frontend: Response
    Frontend-->>User: Display Data
```

### Authorization (RBAC)

| Role | Permissions | Use Case |
|------|-------------|----------|
| **SUPER_ADMIN** | All permissions | Platform administrators |
| **TENANT_ADMIN** | Tenant-wide admin | Organization IT admins |
| **CLINICIAN** | Read/write clinical data | Physicians, NPs |
| **NURSE** | Read clinical data, update vitals | Nurses, MAs |
| **CARE_MANAGER** | Manage care gaps, outreach | Care coordinators |
| **QUALITY_ANALYST** | Read quality measures | Quality teams |
| **BILLING_ADMIN** | Manage prior auth, claims | Billing staff |
| **DEVELOPER** | API access, webhooks | Integration partners |
| **PATIENT** | Read own data, message providers | Patients |
| **VIEWER** | Read-only access | Auditors, compliance |

### Data Encryption

| Layer | Encryption | Standard |
|-------|------------|----------|
| **At Rest** | AES-256 | PostgreSQL TDE, S3 encryption |
| **In Transit** | TLS 1.3 | All HTTP traffic |
| **PHI Fields** | AES-256-GCM | Additional field-level encryption |
| **Agent Memory** | AES-256-GCM | Conversation history in Redis |

---

## Scalability & Performance

### Horizontal Scaling Strategy

```mermaid
graph TB
    subgraph LoadBalancer[Load Balancer - NGINX]
        LB[Round Robin + Health Checks]
    end
    
    subgraph ServiceReplicas[Service Replicas]
        FHIR1[FHIR Service<br/>Instance 1]
        FHIR2[FHIR Service<br/>Instance 2]
        FHIR3[FHIR Service<br/>Instance 3]
    end
    
    subgraph Database[Database Layer]
        Primary[(Primary<br/>Read/Write)]
        Replica1[(Replica 1<br/>Read Only)]
        Replica2[(Replica 2<br/>Read Only)]
    end
    
    LB --> FHIR1
    LB --> FHIR2
    LB --> FHIR3
    
    FHIR1 --> Primary
    FHIR2 --> Replica1
    FHIR3 --> Replica2
    
    Primary -.->|Replication| Replica1
    Primary -.->|Replication| Replica2
```

### Caching Strategy

| Cache Layer | Technology | TTL | Purpose |
|-------------|------------|-----|---------|
| **L1 Cache** | Caffeine (in-memory) | 5 min | JVM-level caching |
| **L2 Cache** | Redis | 15 min | Distributed caching |
| **CDN Cache** | CloudFront | 24 hours | Static assets |
| **Query Cache** | Redis | 5 min | Database query results |

### Performance Targets

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| **API Latency (p95)** | <100ms | ~150ms | 🟡 In Progress |
| **API Latency (p99)** | <250ms | ~400ms | 🟡 In Progress |
| **Database Query (p95)** | <50ms | ~75ms | 🟡 In Progress |
| **Cache Hit Rate** | >80% | 67% | 🟡 In Progress |
| **Uptime SLA** | 99.9% | 99.5% | 🟡 In Progress |
| **Clinical Query Time** | <5sec | ~8sec | 🟡 In Progress |
| **Concurrent Users** | 10,000+ | ~500 | 🔴 Planned |

---

## Deployment Architecture

### Development Environment

```
Developer Machine
├── Docker Compose (local)
│   ├── PostgreSQL (5432)
│   ├── Redis (6379)
│   ├── Kafka (9092)
│   └── Kafka UI (8080)
├── Backend Services (Gradle bootRun)
│   └── All 30+ services on localhost
└── Frontend (npm run dev)
    └── All portals on localhost:3000-5173
```

### Staging Environment

```
Kubernetes Cluster (AWS EKS)
├── Namespace: hdim-staging
├── Services: 3 replicas each
├── PostgreSQL: RDS Multi-AZ
├── Redis: ElastiCache Cluster
├── Kafka: MSK (Managed Streaming for Kafka)
└── Load Balancer: Application Load Balancer
```

### Production Environment

```
Kubernetes Cluster (AWS EKS)
├── Namespace: hdim-production
├── Services: 5-10 replicas (auto-scaling)
├── PostgreSQL: RDS Multi-AZ + Read Replicas
├── Redis: ElastiCache Multi-AZ Cluster
├── Kafka: MSK Multi-AZ
├── Load Balancer: ALB + WAF
├── CDN: CloudFront
└── Monitoring: Prometheus + Grafana + Jaeger
```

---

## Disaster Recovery

### Backup Strategy

| Component | Backup Frequency | Retention | RTO | RPO |
|-----------|------------------|-----------|-----|-----|
| **PostgreSQL** | Continuous WAL + Daily Full | 30 days | 1 hour | 15 min |
| **Redis** | RDB snapshots | 6 hours | 30 min | 6 hours |
| **Kafka** | Topic replication (RF=3) | 7 days | Immediate | 0 |
| **S3 Documents** | Cross-region replication | Indefinite | 15 min | 0 |
| **Configuration** | Git + Terraform state | Indefinite | 5 min | 0 |

### High Availability SLA

- **Target Uptime**: 99.9% (8.76 hours downtime/year)
- **Recovery Time Objective (RTO)**: 1 hour
- **Recovery Point Objective (RPO)**: 15 minutes

---

## Compliance & Audit

### HIPAA Compliance

| Requirement | Implementation |
|-------------|----------------|
| **Access Control** | JWT + RBAC + MFA |
| **Audit Trail** | Kafka audit events (7-year retention) |
| **Encryption** | AES-256 at rest, TLS 1.3 in transit |
| **Minimum Necessary** | Field-level consent enforcement |
| **Breach Notification** | Automated detection + notification workflow |
| **Business Associate Agreements** | BAA with all subprocessors |

### SOC 2 Type II Controls

- **Security**: Firewall, IDS/IPS, vulnerability scanning
- **Availability**: HA infrastructure, monitoring, incident response
- **Processing Integrity**: Data validation, error handling
- **Confidentiality**: Encryption, access controls
- **Privacy**: Consent management, data retention policies

---

## Next Steps

1. Review [Frontend Architecture](./frontend-architecture.md)
2. Review [Backend Microservices](./backend-services.md)
3. Review [AI & Agent Architecture](./ai-agents.md)
4. Check [Security & Compliance](./security-compliance.md)

---

**Document Version**: 1.0.0  
**Last Updated**: January 14, 2026  
**Maintained By**: HDIM Architecture Team
