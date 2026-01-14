# Architecture Visualization Prompts

**Purpose:** Generate high-quality architecture diagrams for marketing materials  
**Based On:** Updated Mermaid diagrams from `docs/roadmap/architecture/platform-overview.md`  
**Target Audience:** CTOs, VPs of Engineering, Solution Architects, Technical Buyers

---

## 1. Complete Platform Architecture Overview

### Prompt

```
Create a modern, professional healthcare platform architecture diagram visualization.

LAYOUT:
- Top section: Frontend layer showing 7 portals (Clinical, Patient, Admin, Agent Studio, Analytics, Developer, Mobile Apps) in a horizontal row
- Second section: API Gateway layer showing 5 gateways (Kong, Main Gateway, Admin Gateway, Clinical Gateway, FHIR Gateway) with connections
- Third section: Service layers organized in 6 groups:
  - Core Clinical Services (7 services): FHIR, CQL Engine, Care Gap, Quality Measure, Patient, HCC, Consent
  - AI & Agent Services (6 services): AI Assistant, Agent Runtime, Agent Builder, Predictive Analytics, Data Enrichment, Guardrails
  - Workflow Services (7 services): Prior Auth, Approval, Payer Workflows, Notification, Event Processing, Event Router, Migration Workflow
  - Integration Services (5 services): EHR Connector, CDR Processor, CMS Connector, QRDA Export, ECR
  - Analytics & Data Services (3 services): Analytics, SDOH, Cost Analysis
  - Supporting Services (2 services): Documentation, Demo Seeding
- Bottom section: Infrastructure layer showing PostgreSQL, Redis, Kafka, S3, Elasticsearch
- Observability layer: Prometheus, Grafana, Jaeger, ELK Stack
- Audit Infrastructure: Audit Publisher, Kafka, Audit Consumer, Audit Store, Audit Query API

STYLE:
- Clean, modern SaaS architecture diagram aesthetic (similar to AWS Architecture Diagrams, Azure Reference Architectures, or Google Cloud Architecture)
- Primary color: Deep Blue #1E3A5F
- Accent color: Teal #00A9A5
- Service boxes: Rounded corners, subtle shadows, clear labels
- Connection lines: Arrows showing data flow direction
- Grouping: Clear visual boundaries with subtle background colors
- Professional, technical, enterprise-grade appearance
- Clear hierarchy and organization
- Realistic architecture diagram, not wireframe

TECHNICAL:
- Resolution: 1920x1080 pixels
- Format: High-quality PNG
- Clear text labels for all services
- Visible connection lines between components
- Professional color scheme
- Enterprise software architecture appearance

AVOID:
- Cluttered layout
- Unclear connections
- Generic architecture diagrams
- Wireframe appearance
- Too many colors
- Unreadable text
```

---

## 2. Real-Time Platform Flow

### Prompt

```
Create a modern real-time healthcare platform data flow diagram.

LAYOUT:
- Left side: Data sources (EHR Systems, Lab Systems, Claims Data)
- Center: HDIM platform showing:
  - Data ingestion layer: EHR Connector, CDR Processor receiving data
  - Event streaming: Kafka cluster with multiple topics (fhir.resources.patient, caregap.detected, audit.events)
  - Real-time processing: Event Processing Service, Event Router Service
  - WebSocket connections: Real-time updates to frontend portals
- Right side: Consumers showing:
  - Clinical Portal receiving real-time care gap updates
  - Analytics Service processing events
  - Audit Consumer storing events
  - Notification Service sending alerts
- Bottom: Observability showing Jaeger tracing, Prometheus metrics, Grafana dashboards

STYLE:
- Modern event-driven architecture diagram
- Primary color: Deep Blue #1E3A5F
- Accent color: Teal #00A9A5
- Event flow: Animated-style arrows showing data movement
- Kafka: Central hub with multiple topic connections
- Real-time indicators: WebSocket icons, streaming indicators
- Professional, technical, modern

TECHNICAL:
- Resolution: 1920x1080 pixels
- Format: High-quality PNG
- Clear event flow direction
- Visible Kafka topics and partitions
- Real-time indicators
- Professional architecture diagram

AVOID:
- Static appearance
- Unclear data flow
- Generic diagrams
- Missing real-time indicators
```

---

## 3. AI & Agent Architecture

### Prompt

```
Create a modern AI agent architecture diagram for healthcare.

LAYOUT:
- Top: Agent Builder (No-Code Platform) creating agents
- Center: Agent Runtime Service showing:
  - Multi-LLM abstraction layer (Claude, Azure OpenAI, AWS Bedrock)
  - Tool registry with healthcare tools (FHIR queries, CQL evaluation, care gap detection)
  - Guardrails layer (PHI protection, clinical safety)
  - Memory management (Redis + PostgreSQL)
- Right: AI Assistant Service providing clinical assistance
- Left: Predictive Analytics Service with risk models
- Bottom: Audit integration showing all AI decisions logged to Kafka audit events
- Connections: All AI services connected to audit infrastructure

STYLE:
- Modern AI architecture diagram
- Primary color: Deep Blue #1E3A5F
- Accent color: Teal #00A9A5
- AI services: Distinct visual style (rounded boxes, AI icons)
- Guardrails: Security-focused visual (shields, locks)
- Multi-LLM: Multiple provider logos/icons
- Professional, secure, AI-focused

TECHNICAL:
- Resolution: 1920x1080 pixels
- Format: High-quality PNG
- Clear AI service boundaries
- Visible guardrail layer
- Multi-LLM abstraction shown
- Audit connections visible

AVOID:
- Generic AI diagrams
- Missing guardrails
- Unclear LLM abstraction
- No audit integration shown
```

---

## 4. Integration Architecture

### Prompt

```
Create a healthcare integration architecture diagram showing EHR connectivity.

LAYOUT:
- Left: External systems in a vertical stack:
  - EHR Systems (Epic, Cerner, Athenahealth logos/icons)
  - Lab Systems
  - Pharmacy Systems
  - HIE Networks
  - Payer Systems
  - CMS Systems
- Center: HDIM Integration Hub showing:
  - EHR Connector Service (Epic, Cerner connectors)
  - CDR Processor (HL7 v2, FHIR transformation)
  - CMS Connector (BCDA, DPC)
  - QRDA Export (Quality reporting)
  - ECR Service (Electronic case reporting)
- Right: HDIM Core Services receiving integrated data
- Bottom: Data flow showing transformation pipeline (HL7 → FHIR → CQL → Quality Measures)

STYLE:
- Modern integration architecture
- Primary color: Deep Blue #1E3A5F
- Accent color: Teal #00A9A5
- External systems: Gray/neutral colors
- HDIM hub: Prominent, central position
- Data transformation: Clear pipeline visualization
- Professional, integration-focused

TECHNICAL:
- Resolution: 1920x1080 pixels
- Format: High-quality PNG
- Clear external system connections
- Visible transformation pipeline
- Integration hub prominence
- Professional healthcare integration diagram

AVOID:
- Unclear connections
- Missing transformation steps
- Generic integration diagrams
- No data flow shown
```

---

## 5. Security & Compliance Architecture

### Prompt

```
Create a healthcare security and compliance architecture diagram.

LAYOUT:
- Top: Authentication layer showing:
  - OAuth 2.0 / SAML flow
  - JWT token validation
  - MFA (TOTP, Recovery Codes)
  - Identity Provider (Okta/Azure AD)
- Center: Authorization layer showing:
  - RBAC (Role-Based Access Control)
  - Multi-tenant isolation
  - Field-level consent enforcement
- Bottom: Audit & Compliance layer showing:
  - Audit Publisher (all services)
  - Kafka audit events
  - Audit Consumer
  - Audit Store (7-year retention)
  - Audit Query API (compliance queries)
- Right: Encryption layers:
  - At Rest: AES-256 (PostgreSQL TDE, S3 encryption)
  - In Transit: TLS 1.3
  - PHI Fields: AES-256-GCM
- Left: Security monitoring:
  - Prometheus metrics
  - Grafana dashboards
  - Vulnerability scanning

STYLE:
- Security-focused architecture diagram
- Primary color: Deep Blue #1E3A5F
- Accent color: Teal #00A9A5
- Security elements: Shield icons, lock icons
- Compliance: HIPAA, SOC 2 badges/icons
- Encryption: Visual encryption indicators
- Professional, secure, compliance-focused

TECHNICAL:
- Resolution: 1920x1080 pixels
- Format: High-quality PNG
- Clear security layers
- Visible compliance features
- Encryption indicators
- Professional security architecture

AVOID:
- Generic security diagrams
- Missing compliance features
- Unclear encryption layers
- No audit trail shown
```

---

## 6. Microservices Communication Flow

### Prompt

```
Create a microservices communication flow diagram showing REST and Kafka patterns.

LAYOUT:
- Top: Client requests coming to API Gateway
- Center: Service communication showing:
  - Synchronous REST calls (solid arrows): Gateway → FHIR → CQL → Quality Measure
  - Asynchronous Kafka events (dashed arrows): Services → Kafka Topics → Consumers
  - WebSocket connections: Real-time updates to frontend
- Left: REST communication pattern:
  - Request/Response flow
  - Service-to-service calls
  - Error handling
- Right: Kafka event pattern:
  - Event producers (services)
  - Kafka topics (multiple topics shown)
  - Event consumers (services)
  - Dead Letter Queue (DLQ)
- Bottom: Observability showing distributed tracing across services

STYLE:
- Modern microservices communication diagram
- Primary color: Deep Blue #1E3A5F
- Accent color: Teal #00A9A5
- REST calls: Solid arrows, request/response style
- Kafka events: Dashed arrows, event flow style
- WebSocket: Real-time connection indicators
- Professional, technical, microservices-focused

TECHNICAL:
- Resolution: 1920x1080 pixels
- Format: High-quality PNG
- Clear REST vs Kafka distinction
- Visible service boundaries
- Communication patterns clear
- Professional microservices diagram

AVOID:
- Unclear communication patterns
- Missing Kafka topics
- No REST/Kafka distinction
- Generic service diagrams
```

---

## Usage Instructions

### Generate Images

```bash
cd docs/marketing/ag-ui/scripts
python3 generate-with-gemini-pro.py --prompt-file ../prompts/architecture-prompts.md
```

### Customize Prompts

1. Copy prompt template
2. Modify LAYOUT section for specific needs
3. Adjust STYLE section for brand colors
4. Update TECHNICAL section for resolution
5. Add AVOID section for specific exclusions

### Output

All images will be saved to:
```
docs/marketing/ag-ui/assets/generated/
├── platform-architecture-overview.png
├── real-time-platform-flow.png
├── ai-agent-architecture.png
├── integration-architecture.png
├── security-compliance-architecture.png
└── microservices-communication-flow.png
```

---

**Prompts Ready for Image Generation**
