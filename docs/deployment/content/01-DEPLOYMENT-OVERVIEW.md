# HDIM On-Premise Deployment Guide
## Platform Integration & Architecture Overview

---

## Executive Summary

The HDIM (HealthData-in-Motion) platform is a clinical data interoperability and quality measurement system designed to integrate seamlessly into existing healthcare infrastructure. This guide demonstrates how HDIM deploys on-premise while leveraging your existing FHIR servers, EHR systems, and clinical workflows.

### Key Platform Capabilities

✅ **Gateway-Centric Architecture** - Single entry point for all clinical requests  
✅ **FHIR-Native** - Direct integration with existing FHIR servers  
✅ **Multi-Tenant Isolation** - Support for multiple healthcare organizations  
✅ **HIPAA Compliant** - Built-in audit logging, encryption, data isolation  
✅ **Flexible Deployment** - Single-node, clustered, Kubernetes, custom infrastructure  
✅ **Real-Time Measures** - Calculate HEDIS and custom measures on-demand  
✅ **Zero Data Duplication** - Queries FHIR server directly, no ETL pipeline needed  

---

## How It Works: The Gateway Pattern

The HDIM platform uses a **Gateway-first architecture** that acts as an intelligent router between your existing systems:

```
Your EHR System (Epic, Cerner, etc.)
         ↓
    FHIR Server
    (Your existing server)
         ↓
  HDIM Gateway Service (8001)
  ├─ Routes clinical requests
  ├─ Validates authentication
  ├─ Orchestrates measure calculation
  └─ Tracks audit logs
         ↓
  Core Services:
  ├─ Quality Measure Service
  ├─ CQL Engine (Clinical Quality Language)
  ├─ Care Gap Detection
  ├─ Risk Adjustment (HCC)
  └─ Reporting & Export
         ↓
  Clinical Portal / Applications
```

### Data Flow: From Request to Measure Result

1. **Request Initiation**
   - Clinical user requests measure evaluation (e.g., "Is patient eligible for breast cancer screening?")
   - Request goes to HDIM Gateway Service

2. **Gateway Processing**
   - Validates user authentication
   - Injects security headers with audit trail
   - Routes to appropriate service

3. **FHIR Data Retrieval**
   - Quality Measure Service queries YOUR existing FHIR Server
   - Fetches patient demographics, observations, conditions, medications
   - Results cached briefly for performance (5-minute HIPAA-compliant cache)

4. **Clinical Evaluation**
   - CQL Engine evaluates clinical logic (e.g., "Patient > 40 years AND no cancer history AND not pregnant")
   - Uses standard medical value sets (SNOMED, ICD-10, LOINC)
   - Determines patient classification: eligible, excluded, denominator exception

5. **Measure Result**
   - Returns clinical decision support to application
   - Care Gap Service identifies missing care
   - Result recorded in audit logs for HIPAA compliance

6. **No Data Persistence**
   - Patient data remains in YOUR FHIR Server
   - HDIM platform only stores measure metadata and configuration
   - Clinical data is queried in real-time, not copied or warehoused

---

## Deployment Flexibility

HDIM supports multiple deployment models to fit your existing infrastructure:

### Model 1: Single-Node On-Premise (Simplest)
- **Best for**: Smaller healthcare organizations, pilot deployments
- **Infrastructure**: Single server (4+ CPU, 16GB RAM)
- **Docker Compose deployment**: All services on one host
- **Time to production**: 2-3 hours

### Model 2: Clustered On-Premise (High Availability)
- **Best for**: Larger organizations, critical clinical operations
- **Infrastructure**: 3-5 servers with load balancer
- **Docker Compose with orchestration**: Multiple hosts
- **Time to production**: 1-2 days

### Model 3: Kubernetes On-Premise (Enterprise)
- **Best for**: Large healthcare systems, multi-tenant deployments
- **Infrastructure**: Kubernetes cluster (3+ nodes)
- **Auto-scaling**: Services scale based on demand
- **Time to production**: 3-5 days (with existing K8s cluster)

### Model 4: Hybrid Cloud
- **Best for**: Organizations with existing cloud infrastructure
- **Infrastructure**: On-premise gateway + cloud-based services
- **Flexibility**: Core services on-premise, analytics in cloud

### Model 5: Custom Infrastructure
- **Best for**: Organizations with unique requirements
- **Infrastructure**: Your existing servers, VMs, containers
- **Flexibility**: Deploy individual services as needed
- **Integration**: Custom connector to your FHIR server

---

## What Gets Deployed vs What You Keep

### HDIM Deploys (Runs On-Premise)
- ✅ Gateway Service (central router)
- ✅ Quality Measure Engine
- ✅ CQL Evaluation Engine
- ✅ Care Gap Detection
- ✅ Risk Adjustment Calculation
- ✅ Clinical Portal UI
- ✅ Admin Portal UI
- ✅ Reporting & Export Services
- ✅ Authentication Gateway (Kong)
- ✅ In-memory Cache (Redis)
- ✅ Audit Logging System

### You Keep Running (Your Existing Systems)
- ✅ Your FHIR Server
- ✅ Your EHR System (Epic, Cerner, Athena, etc.)
- ✅ Your Authentication System (Okta, AD, etc.)
- ✅ Your Database Infrastructure
- ✅ Your Firewall & Network Security
- ✅ Your Disaster Recovery Infrastructure

---

## Integration Points with Existing Systems

### 1. FHIR Server Integration (Direct)
```
HDIM Quality Measure Service
         ↓ (Direct FHIR API)
    Your FHIR Server
    (Epic, Cerner, or Generic FHIR)
         ↓ (Returns FHIR Resources)
    Patient demographics, labs, conditions, medications
```

**What HDIM Queries From Your FHIR Server:**
- Patient resources (demographics)
- Observations (lab results, vitals)
- Conditions (diagnoses)
- Medications (prescriptions)
- Immunizations (vaccinations)
- Care Plans (treatment plans)
- Encounters (visit history)
- Procedures (medical procedures)

### 2. Authentication Integration
```
Your Auth System (Okta, AD, Keycloak)
         ↓ (OAuth2/OIDC)
    Kong API Gateway
         ↓ (JWT token validation)
    HDIM Gateway Service
         ↓ (Header injection with user context)
    Core Services
```

### 3. Clinical Workflow Integration
```
Clinical User Opens Portal
         ↓
HDIM Clinical Portal
         ↓
Requests measure for patient
         ↓
HDIM Gateway routes to Quality Service
         ↓
Quality Service queries your FHIR Server
         ↓
Measure result displayed in portal
         ↓
Clinical user acts on result (order screening, close gap, etc.)
         ↓
Action updates your EHR System
```

---

## Security & Compliance Architecture

### HIPAA Compliance Built-In
- **Audit Logging**: Every PHI access logged (who, what, when, patient ID)
- **Data Isolation**: Multi-tenant architecture ensures no cross-organization data access
- **Encryption in Transit**: TLS 1.3 for all network communication
- **Cache Management**: PHI cached maximum 5 minutes
- **Role-Based Access**: ADMIN, EVALUATOR, ANALYST, VIEWER roles

### Authentication Security
- **JWT Validation**: Kong validates all incoming tokens
- **Header Signing**: Gateway signs headers with HMAC-SHA256 to prevent tampering
- **Service-to-Service**: Internal communication uses signed tokens
- **No Secrets in Logs**: Sensitive data excluded from audit trails

### Network Security
- **Firewall-Ready**: Gateway operates behind your existing firewall
- **Port Isolation**: Only Kong (8000) exposed to external traffic
- **Internal Services**: Quality, CQL, Care Gap services on internal network only
- **TLS Endpoints**: All service-to-service communication encrypted

---

## Performance Characteristics

### Latency Profile
- **Gateway Routing**: <5ms
- **FHIR Query** (single patient): 50-200ms (depends on your server)
- **CQL Evaluation** (typical measure): 100-500ms
- **End-to-End Response**: <500ms p95 for typical requests

### Caching Strategy
- **Patient FHIR Data**: 5 minutes (HIPAA compliant)
- **Measure Definitions**: 1 hour
- **Value Sets** (medical code lists): 1 hour
- **User Sessions**: 15 minutes

### Scalability
- **Single Node**: 1,000+ concurrent users
- **Clustered**: 10,000+ concurrent users
- **Kubernetes**: 100,000+ concurrent users (auto-scaling)

---

## Deployment Comparison Matrix

| Factor | Single-Node | Clustered | Kubernetes | Hybrid | Custom |
|--------|-------------|-----------|------------|--------|--------|
| **Upfront Cost** | Low | Medium | Medium-High | Medium | Variable |
| **Operational Complexity** | Low | Medium | High | High | Variable |
| **High Availability** | No | Yes | Yes | Yes | Variable |
| **Auto-Scaling** | No | Manual | Automatic | Automatic | Manual |
| **Disaster Recovery** | Manual | Semi-Auto | Automatic | Automatic | Manual |
| **Best For** | Pilots, SMB | Large Health Systems | Enterprise, Multi-Tenant | Hybrid Infrastructure | Unique Requirements |
| **Time to Production** | 2-3 hours | 1-2 days | 3-5 days | 1-2 weeks | Variable |
| **Team Size Needed** | 1-2 | 2-3 | 3-5 | 3-5 | 2-5 |

---

## Next Steps

This guide includes:

1. **Architecture Diagrams** - Visual representations of each deployment model
2. **Data Flow Diagrams** - How requests flow through the system
3. **Decision Tree** - Choose the right deployment for your organization
4. **Reference Architectures** - Detailed setup for each model
5. **Integration Guides** - Connect to your FHIR server, authentication system
6. **Deployment Instructions** - Step-by-step guides for each model
7. **Monitoring & Operations** - Keep the system running smoothly

**Start with the Decision Tree** → Choose your deployment model → Follow the reference architecture for your choice → Execute deployment guide

---

## Key Architectural Principles

### 1. No Data Centralization
HDIM does not copy or centralize clinical data from your FHIR server. Every query is direct, real-time access. Your data stays in your control.

### 2. Gateway-First Pattern
All requests flow through the Gateway Service, which provides:
- Unified authentication
- Request routing
- Circuit breaking (prevents cascade failures)
- Rate limiting (prevents resource exhaustion)
- Audit logging (HIPAA compliance)

### 3. Service-Oriented Architecture
Each capability is a separate service that can be:
- Scaled independently
- Deployed selectively
- Monitored separately
- Updated without downtime

### 4. Real-Time Evaluation
Measures are calculated on-demand, not pre-computed. This means:
- Always current data from FHIR server
- No stale measure results
- Lower storage requirements
- Dynamic population health insights

### 5. Multi-Tenancy by Default
The platform is built for multiple healthcare organizations to coexist:
- Complete data isolation (tenant ID on all queries)
- Separate audit trails per tenant
- Role-based access control per tenant
- Cost allocation per tenant

---

## Success Metrics

After deploying HDIM, you should see:

✅ **Clinical Efficiency**: 30-50% reduction in time to identify care gaps  
✅ **Data Quality**: 95%+ measure completion rates (vs. 70% manual)  
✅ **Care Improvement**: 20-30% improvement in measure scores within 6 months  
✅ **Operational Simplicity**: Single pane of glass for quality operations  
✅ **Compliance**: 100% HIPAA audit trail coverage  
✅ **Cost Control**: Reduce measurement infrastructure costs by 40%  

---

## Support & Integration Services

- **Setup Assistance**: Pre-deployment consultation
- **FHIR Server Integration**: Custom adapters if needed
- **Authentication Integration**: SSO setup with your provider
- **Clinical Workflow Integration**: Portal customization
- **Training**: Technical and clinical user training
- **Ongoing Support**: 24/7 support for production deployments

---

**Next Section:** Review the deployment decision tree to choose the right model for your organization.
