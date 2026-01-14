# Mermaid Diagram Review & Update Plan

**Date:** January 14, 2026  
**Purpose:** Review and update architecture diagrams for marketing visualizations

---

## Executive Summary

**Current Status:**
- ✅ Main architecture diagrams exist in `docs/architecture/diagrams/ARCHITECTURE_DIAGRAMS.md`
- ✅ Platform overview with diagrams in `docs/roadmap/architecture/platform-overview.md`
- ⚠️ **Gap:** Diagrams only show 9-10 services, but platform has **33+ microservices**
- ⚠️ **Gap:** Missing recent improvements (audit integration, AI services, observability)
- ⚠️ **Gap:** Marketing-focused visualizations not created

**Action Required:**
1. Update diagrams to include all 33+ services
2. Add recent improvements (audit, AI, observability)
3. Create marketing-focused architecture visualizations
4. Generate image prompts based on updated diagrams

---

## Complete Service Inventory

### All 33 Microservices Found

#### Core Clinical Services (7)
1. ✅ **fhir-service** - FHIR R4 resource management
2. ✅ **cql-engine-service** - Clinical Quality Language evaluation
3. ✅ **care-gap-service** - Care gap identification
4. ✅ **quality-measure-service** - HEDIS measure calculation
5. ✅ **patient-service** - Patient 360° view
6. ✅ **consent-service** - HIPAA consent management
7. ✅ **hcc-service** - RAF calculation, HCC coding

#### AI & Agent Services (4)
8. ✅ **ai-assistant-service** - AI-powered clinical assistance
9. ✅ **agent-runtime-service** - Multi-LLM agent execution
10. ✅ **agent-builder-service** - No-code agent platform
11. ✅ **predictive-analytics-service** - Risk models, predictions

#### Workflow Services (5)
12. ✅ **prior-auth-service** - Prior authorization workflows
13. ✅ **approval-service** - Approval workflows (HITL)
14. ✅ **payer-workflows-service** - Payer workflow orchestration
15. ✅ **notification-service** - Email/SMS/Push notifications
16. ✅ **migration-workflow-service** - Data migration workflows

#### Integration Services (6)
17. ✅ **ehr-connector-service** - Epic, Cerner connectors
18. ✅ **cdr-processor-service** - HL7 v2, FHIR processing
19. ✅ **cms-connector-service** - BCDA, DPC integration
20. ✅ **qrda-export-service** - Quality reporting export
21. ✅ **ecr-service** - Electronic case reporting
22. ✅ **data-enrichment-service** - AI data enrichment

#### Analytics & Data Services (3)
23. ✅ **analytics-service** - Real-time dashboards
24. ✅ **sdoh-service** - Social determinants of health
25. ✅ **cost-analysis-service** - Cost analysis

#### Infrastructure Services (6)
26. ✅ **gateway-service** - Main API gateway
27. ✅ **gateway-admin-service** - Admin gateway
28. ✅ **gateway-clinical-service** - Clinical gateway
29. ✅ **gateway-fhir-service** - FHIR gateway
30. ✅ **event-processing-service** - Kafka event processing
31. ✅ **event-router-service** - Event routing

#### Supporting Services (2)
32. ✅ **documentation-service** - Documentation management
33. ✅ **demo-seeding-service** - Demo data generation

#### Non-PHI Services (1)
34. ✅ **sales-automation-service** - Sales automation (non-PHI)

**Total: 34 Services** (33 microservices + 1 non-PHI)

---

## Current Diagram Coverage

### ARCHITECTURE_DIAGRAMS.md (Last Updated: Dec 5, 2025)

**Services Shown:**
- ✅ FHIR Service
- ✅ CQL Engine Service
- ✅ Quality Measure Service
- ✅ Care Gap Service
- ✅ Patient Service
- ✅ Consent Service
- ✅ Event Processing Service
- ✅ Analytics Service
- ✅ AI Assistant Service

**Missing Services:** 25 services (73% missing)

### platform-overview.md (Last Updated: Jan 14, 2026)

**Services Shown:**
- ✅ FHIR Service
- ✅ CQL Engine
- ✅ Care Gap Service
- ✅ Quality Measure
- ✅ Patient Service
- ✅ HCC Service
- ✅ Consent Service
- ✅ AI Assistant
- ✅ Agent Runtime
- ✅ Agent Builder
- ✅ Predictive Analytics
- ✅ Prior Authorization
- ✅ Approval Workflows
- ✅ Payer Workflows
- ✅ Notification
- ✅ Event Processing
- ✅ EHR Connector
- ✅ CDR Processor
- ✅ CMS Connector
- ✅ QRDA Export
- ✅ X12 Processing

**Missing Services:** 13 services (38% missing)

---

## Recent Improvements Not Documented

### 1. Audit Integration (Completed Jan 2026)
- ✅ Comprehensive audit infrastructure across 36 services
- ✅ Kafka-based audit event streaming
- ✅ 7-year retention for HIPAA compliance
- ✅ Event replay capability
- **Status:** Not shown in diagrams

### 2. Observability Stack (Completed)
- ✅ Jaeger distributed tracing
- ✅ Prometheus metrics
- ✅ Grafana dashboards
- ✅ ELK stack for logs
- **Status:** Mentioned but not visualized

### 3. Real-Time Platform Features
- ✅ WebSocket support for real-time updates
- ✅ Kafka event streaming
- ✅ Real-time audit trail
- ✅ Real-time data synchronization
- **Status:** Partially shown

### 4. Multi-Gateway Architecture
- ✅ Gateway Service (main)
- ✅ Gateway Admin Service
- ✅ Gateway Clinical Service
- ✅ Gateway FHIR Service
- **Status:** Only main gateway shown

### 5. AI Services Expansion
- ✅ Agent Runtime (multi-LLM)
- ✅ Agent Builder (no-code)
- ✅ Predictive Analytics
- ✅ Data Enrichment (AI-powered)
- **Status:** Partially shown

---

## Update Plan

### Phase 1: Update Core Architecture Diagrams

#### 1.1 System Context Diagram
**File:** `docs/architecture/diagrams/ARCHITECTURE_DIAGRAMS.md`

**Updates Needed:**
- Add all 34 services
- Show audit infrastructure
- Show observability stack
- Show multi-gateway architecture

#### 1.2 Container Diagram
**Updates Needed:**
- Group services by category (7 groups)
- Show all gateways
- Show audit pipeline
- Show observability connections

#### 1.3 Service Communication Diagram
**Updates Needed:**
- Show REST vs Kafka communication
- Show audit event flow
- Show tracing flow
- Show real-time WebSocket connections

### Phase 2: Create Marketing-Focused Visualizations

#### 2.1 High-Level Platform Overview
**Purpose:** Executive/CTO audience
- Show all 34 services grouped logically
- Highlight key capabilities
- Show scalability features
- Show compliance features

#### 2.2 Real-Time Platform Architecture
**Purpose:** Technical buyers
- Show Kafka event streaming
- Show WebSocket real-time updates
- Show distributed tracing
- Show audit trail flow

#### 2.3 AI & Agent Architecture
**Purpose:** AI/ML buyers
- Show multi-LLM architecture
- Show agent runtime
- Show guardrails
- Show audit integration

#### 2.4 Integration Architecture
**Purpose:** Integration teams
- Show EHR connectors
- Show data flow
- Show transformation pipeline
- Show quality reporting

### Phase 3: Generate Image Prompts

Based on updated Mermaid diagrams, create prompts for:
1. **System Architecture Diagram** - High-level overview
2. **Service Communication Flow** - How services interact
3. **Real-Time Platform Flow** - Kafka, WebSockets, tracing
4. **AI Agent Architecture** - Multi-LLM, guardrails, audit
5. **Integration Architecture** - EHR, data flow, reporting
6. **Security & Compliance** - Audit, encryption, HIPAA

---

## Next Steps

1. ✅ **Review Complete** - This document
2. ⏳ **Update Mermaid Diagrams** - Add all services and improvements
3. ⏳ **Create Marketing Visualizations** - Extract from Mermaid
4. ⏳ **Generate Image Prompts** - Based on updated diagrams
5. ⏳ **Generate Images** - Using Gemini 3 PRO

---

## Files to Update

1. `docs/architecture/diagrams/ARCHITECTURE_DIAGRAMS.md`
   - Update C4 Level 2 Container Diagram
   - Add all 34 services
   - Add audit infrastructure
   - Add observability stack

2. `docs/roadmap/architecture/platform-overview.md`
   - Update System Architecture diagram
   - Add missing services
   - Add audit flow
   - Add observability connections

3. `docs/marketing/ag-ui/prompts/architecture-prompts.md` (NEW)
   - Create prompts for marketing visualizations
   - Based on updated Mermaid diagrams

---

**Review Complete - Ready for Updates**
