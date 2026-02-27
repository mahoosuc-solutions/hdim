# Enterprise SaaS Implementation Architecture (Client + Product)

**Platform:** HealthData-in-Motion (HDIM)  
**Audience:** Client solution architects, platform engineering, security/compliance reviewers  
**Format:** Mermaid diagrams (Draw.io compatible via Mermaid import)

---

## 1. Goals and Scope

This document defines an enterprise SaaS architecture for HDIM across:
- Client-facing implementation surfaces (clinical operations, admin, analytics, API consumers)
- Product implementation layers (gateway, domain services, data/event platform, control plane)
- Cross-cutting concerns (tenant isolation, security, observability, release governance, DR)

---

## 2. System Context (Client + External Ecosystem)

```mermaid
graph TB
    subgraph Client_Org["Client Organization (Tenant)"]
      Clinician[Clinical Users]
      Admin[Client Admin]
      Analyst[Quality Analyst]
      Integrator[Client Integrations Team]
    end

    subgraph HDIM["HDIM SaaS Platform"]
      Portal[Clinical Portal / Admin UX]
      API[API Gateway + Auth]
      Core[Domain Services]
      Data[Data + Event Platform]
    end

    EHR[EHR / HIE Systems]
    Payer[Payer / Claims]
    Identity[Enterprise IdP / SSO]
    Notify[Notification Providers]

    Clinician --> Portal
    Admin --> Portal
    Analyst --> Portal
    Integrator --> API

    Portal --> API
    API --> Core
    Core --> Data

    EHR <--> API
    Payer <--> API
    Identity <--> API
    Core --> Notify
```

---

## 3. Product Architecture (Control Plane and Data Plane)

```mermaid
flowchart LR
    subgraph ControlPlane["Control Plane"]
      TenantMgmt[Tenant Provisioning]
      Policy[Policy / RBAC Management]
      Config[Config + Feature Flags]
      Release[Release Gate + Evidence Pack]
      Observability[Metrics / Logs / Traces]
    end

    subgraph DataPlane["Data Plane"]
      Edge[Gateway Edge / Routing]
      Services[FHIR + Patient + Quality + Care Gap + Audit + Events]
      Cache[(Redis Cache)]
      EventBus[(Kafka)]
      DB[(PostgreSQL)]
    end

    TenantMgmt --> Config
    Policy --> Edge
    Config --> Edge
    Release --> Edge
    Observability --> Release

    Edge --> Services
    Services --> Cache
    Services --> DB
    Services <--> EventBus
```

---

## 4. Tenant Isolation and Security Boundaries

```mermaid
flowchart TB
    Request[Inbound Request]
    TLS[TLS Termination]
    AuthN[AuthN: JWT/OIDC Validation]
    AuthZ[AuthZ: RBAC + Endpoint Policies]
    Tenant[Tenant Context Enforcement\nX-Tenant-ID + Access Filter]
    Service[Domain Service]
    Data[(Tenant-Scoped Data Access)]
    Audit[Immutable Audit Event]

    Request --> TLS --> AuthN --> AuthZ --> Tenant --> Service --> Data
    Service --> Audit
```

**Best-practice controls**
- Authentication at gateway and service trust boundaries.
- Authorization at endpoint and method level (least privilege).
- Tenant context validated before data access.
- Audit trail for read/write and privileged operations.
- Encryption in transit and at rest.

---

## 5. Client Workflow Data Flow (Clinical Gap Closure)

```mermaid
sequenceDiagram
    participant U as Clinical User
    participant UI as Clinical Portal
    participant GW as API Gateway
    participant QM as Quality Measure Service
    participant FHIR as FHIR Service
    participant CG as Care Gap Service
    participant EV as Event Bus
    participant AU as Audit Service

    U->>UI: Open patient quality dashboard
    UI->>GW: GET /api/quality/results (tenant + token)
    GW->>QM: Authorized request
    QM->>FHIR: Fetch patient clinical resources
    FHIR-->>QM: Patient data bundle
    QM->>CG: Evaluate open gaps
    CG-->>QM: Gap status + recommendations
    QM-->>GW: Aggregated result
    GW-->>UI: Response payload
    QM->>EV: Publish evaluation event
    EV->>AU: Persist audit/event record
```

---

## 6. Ingestion to Analytics Flow (Operational Intelligence)

```mermaid
flowchart LR
    Source[EHR / Claims / External Feeds] --> Ingress[Gateway Ingestion APIs]
    Ingress --> Normalize[FHIR Normalization + Validation]
    Normalize --> Persist[(Operational Databases)]
    Normalize --> Stream[(Kafka Topics)]
    Stream --> Process[Event Processing / Rules]
    Process --> Metrics[Care Gap + Quality Metrics]
    Metrics --> Dashboard[Clinical & Ops Dashboards]
    Process --> Alerts[Notifications / Workflow Tasks]
```

---

## 7. Enterprise SaaS Deployment Pattern

```mermaid
flowchart TB
    subgraph ClientAccess["Client Access"]
      Browser[Web/Mobile Browser]
      APIClient[System-to-System API Clients]
    end

    subgraph Edge["Edge + Security"]
      WAF[WAF / Rate Limiting]
      Gateway[Gateway Edge]
    end

    subgraph Platform["SaaS Runtime"]
      Services[Microservices]
      Cache[(Redis)]
      DB[(PostgreSQL)]
      Kafka[(Kafka)]
    end

    subgraph Ops["Operations"]
      CI[CI/CD + Release Gate]
      Obs[Observability Stack]
      Backup[Backup / DR]
      IR[Incident Response Runbooks]
    end

    Browser --> WAF --> Gateway
    APIClient --> WAF --> Gateway
    Gateway --> Services
    Services --> Cache
    Services --> DB
    Services --> Kafka
    CI --> Services
    Obs --> Services
    Backup --> DB
    IR --> Obs
```

---

## 8. Operational Best-Practice Checklist

- Define separate control-plane and data-plane ownership.
- Enforce tenant isolation before every data access path.
- Use policy-as-code for authZ and release gating.
- Require artifact-backed go/no-go decisions per release.
- Track SLOs (availability, latency, error budget) per client-facing capability.
- Run periodic DR, security, and compliance validation drills.
- Keep architecture docs aligned with executable validation scripts.

---

## 9. Draw.io Usage Notes

If your team prefers Draw.io:
1. Open Draw.io.
2. Use **Arrange → Insert → Advanced → Mermaid**.
3. Paste any Mermaid block from this file.
4. Save `.drawio` source for governance-controlled diagram versions.
