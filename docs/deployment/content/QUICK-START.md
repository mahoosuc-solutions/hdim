# HDIM Deployment: Quick Start Guide

A fast-track guide to understanding and deploying HDIM for your healthcare organization.

---

## The Big Picture: What is HDIM?

HDIM is an on-premise healthcare platform that:

1. **Integrates with your existing FHIR Server** (Epic, Cerner, etc.)
2. **Calculates clinical measures** in real-time (HEDIS, quality scores, risk)
3. **Detects care gaps** (overdue screenings, missing treatments)
4. **Stays on your infrastructure** (no cloud, no data sharing)
5. **Provides clinical decision support** for your care teams

**Key Pattern: Gateway Architecture**
```
Your EHR → Your FHIR Server
            ↓ (Direct query via REST API)
            HDIM Gateway Service
            ↓ (Route & orchestrate)
            Quality Measure Engine
            ↓ (Calculate measures)
            Clinical Portal
            ↓ (Display results)
            Clinician sees actionable insights
```

---

## 30-Second Decision

**How many patients?**
- < 50K → **Single-Node** (2-3 hours to deploy)
- 50K-500K → **Clustered** (1-2 days to deploy)
- > 500K → **Kubernetes** (3-5 days to deploy)

**Do you have existing Kubernetes?**
- Yes → Use Kubernetes
- No → Use Single-Node or Clustered

**Move to the deployment section for your choice →**

---

## Deployment Options at a Glance

### Single-Node: Fast & Simple
```
┌─────────────────────────────────┐
│        1 Server (4CPU, 16GB)     │
│  Docker Compose (all services)  │
│  PostgreSQL + Redis (local)     │
└─────────────────────────────────┘

✓ Deploy in 2-3 hours
✓ Cost: $200-500/month
✓ Best for: Pilots, testing, small orgs
✗ No high availability
✗ Single point of failure
```

### Clustered: Production Ready
```
┌─────────────────────────────────┐
│   3-5 Servers + Load Balancer   │
│   Docker Compose on each server │
│  PostgreSQL Cluster + Redis HA  │
└─────────────────────────────────┘

✓ Deploy in 1-2 days
✓ Cost: $2-5K/month
✓ Best for: Production, medium orgs
✓ High availability built-in
✗ More operational complexity
```

### Kubernetes: Enterprise Scale
```
┌─────────────────────────────────┐
│  K8s Cluster (3 CP + 5-10 WN)   │
│  Auto-scaling, self-healing     │
│  Cloud-native infrastructure    │
└─────────────────────────────────┘

✓ Deploy in 3-5 days
✓ Cost: $5-15K/month
✓ Best for: Large orgs, 500K+ patients
✓ Automatic scaling & failover
✗ High complexity, needs DevOps team
```

---

## What Gets Deployed?

### HDIM Provides:
```
✅ Gateway Service (request routing, auth)
✅ Quality Measure Engine (HEDIS, quality scores)
✅ CQL Evaluation Engine (clinical logic)
✅ Care Gap Detection (missing care alerts)
✅ Risk Adjustment Calculation (HCC scoring)
✅ Clinical Portal UI (view results)
✅ Reporting & Export Services
✅ Audit Logging System (HIPAA compliance)
```

### You Keep Running:
```
✅ Your FHIR Server (Epic, Cerner, etc.)
✅ Your EHR System
✅ Your Authentication System (Okta, AD)
✅ Your Database Infrastructure
✅ Your Firewall & Network Security
```

**Your data stays in your control.**
HDIM queries your FHIR server in real-time, doesn't copy or warehouse data.

---

## The Integration Story

### How Data Flows:

1. **Clinical user opens portal**
   → "Calculate screening status for patient"

2. **Request to HDIM**
   → /api/quality/calculate?patientId=123&measure=BCS

3. **HDIM queries your FHIR server**
   → GET https://your-ehr.example.com/fhir/Patient/123

4. **Your FHIR server responds with patient data**
   → Patient demographics, observations, conditions, meds

5. **HDIM evaluates measure logic**
   → "Is patient eligible? Did they get screening?"

6. **Result returned to clinical portal**
   → "Status: COMPLIANT" or "Status: OVERDUE"

7. **Clinical user sees actionable insight**
   → Patient needs mammography screening

8. **Clinician orders screening in your EHR**
   → Care gap closed

**Key**: Your FHIR server is queried in real-time. Data stays under your control.

---

## Security & Compliance

### Built-In HIPAA Features:

| Feature | Implementation |
|---------|---|
| **Audit Logging** | Every PHI access logged (who, what, when) |
| **Data Isolation** | Multi-tenant, no cross-organization access |
| **Encryption** | TLS 1.3 for all network traffic |
| **Cache Management** | PHI cached max 5 minutes |
| **Role-Based Access** | ADMIN, EVALUATOR, ANALYST, VIEWER roles |
| **Authentication** | Your existing SSO (Okta, AD, Keycloak) |
| **Authorization** | Header-based, signed with HMAC |

### Authentication Flow:

```
User clicks "Login"
    ↓
Redirected to your OIDC provider
(Okta, Azure AD, Keycloak)
    ↓
User logs in with corporate credentials
    ↓
OIDC provider returns JWT token
    ↓
Kong API Gateway validates token
    ↓
HDIM injects trust headers
    ↓
User authenticated in portal
    ↓
All queries filtered by tenant & user access
```

---

## Performance Expectations

### Real-Time Measure Evaluation:

```
Request initiation: 0ms
    ↓ Gateway routing: 5ms
    ↓ Quality service: 50ms
    ↓ FHIR query: 100-200ms (depends on your server)
    ↓ CQL evaluation: 100-200ms
    ↓ Response serialization: 30ms
    └─ Total: ~400-500ms
```

**Result**: Clinical user sees measure result in < 1 second

### Concurrent Users Supported:

| Deployment | Single Concurrent Users | Concurrent Evaluations |
|---|---|---|
| Single-Node | 50-500 | 10-50 |
| Clustered | 500-2,000 | 50-200 |
| Kubernetes | 2,000-10,000+ | 200-1,000+ |

---

## Quick Implementation Checklist

### Pre-Deployment (1-2 days)

- [ ] Identify your FHIR server type (Epic, Cerner, Generic?)
- [ ] Get FHIR API credentials from your EHR
- [ ] Test connectivity from HDIM server to your FHIR server
- [ ] Identify your authentication provider (Okta, AD, Keycloak?)
- [ ] Create HDIM OAuth2 client in auth provider
- [ ] Map user attributes (roles, tenant IDs)
- [ ] Determine infrastructure needs (single-node, clustered, or K8s)
- [ ] Provision servers/infrastructure
- [ ] Coordinate with firewall/network teams

### Deployment (2-3 hours to 3-5 days, depending on model)

- [ ] Install prerequisites (Docker, Docker Compose, etc.)
- [ ] Deploy HDIM services
- [ ] Configure FHIR server integration
- [ ] Configure authentication/SSO
- [ ] Run health checks
- [ ] Load sample patients (optional)
- [ ] Configure monitoring & alerting
- [ ] Backup and disaster recovery testing

### Post-Deployment (1-2 weeks)

- [ ] Validate measure calculations against manual reviews
- [ ] Train clinical users on portal
- [ ] Monitor performance metrics
- [ ] Setup escalation procedures
- [ ] Go live with pilot group
- [ ] Gather feedback and optimize

---

## Common Questions

### Q: Will HDIM copy my patient data?
**A:** No. HDIM queries your FHIR server on-demand. Data stays in your system.

### Q: Can I use my existing authentication system?
**A:** Yes. HDIM integrates with OIDC/OAuth2 providers (Okta, AD, Keycloak, etc.).

### Q: What if my FHIR server goes down?
**A:** HDIM will return a "FHIR server unavailable" error. Clinical portal still functions; measures just can't be calculated that moment.

### Q: Can multiple healthcare organizations use the same HDIM instance?
**A:** Yes. HDIM supports multi-tenant deployments with complete data isolation.

### Q: How do we keep HDIM running if it crashes?
**A:** Depends on deployment model:
- Single-Node: Manual restart
- Clustered: Automatic failover to another server
- Kubernetes: Automatic pod restart and rescheduling

### Q: How long to deploy?
**A:** 2-3 hours (Single-Node) to 3-5 days (Kubernetes), depending on model and infrastructure readiness.

### Q: What's the cost?
**A:** $200-500/month (Single-Node) to $10-15K/month (Kubernetes), depending on scale and infrastructure.

---

## Next Steps

### 1. Read the Deployment Decision Tree
📄 [Deployment Decision Tree](./03-DEPLOYMENT-DECISION-TREE.md)
→ Choose your deployment model based on your org size, IT capability, budget

### 2. Review Your Architecture Choice
📄 [Reference Architectures](./04-REFERENCE-ARCHITECTURES.md)
→ Understand the detailed topology for your chosen model

### 3. Plan Your FHIR Integration
📄 [Integration Patterns](./02-INTEGRATION-PATTERNS.md)
→ How to connect HDIM to your FHIR server (Epic, Cerner, etc.)

### 4. Understand Security & Compliance
📄 [Architecture Diagrams - Section 9](./01-ARCHITECTURE-DIAGRAMS.md)
→ How audit logging and multi-tenant isolation work

### 5. Deploy!
📄 [Deployment Guides](./07-DEPLOYMENT-GUIDES.md) *(Coming soon)*
→ Step-by-step instructions for your chosen model

---

## Key Concepts Explained

### What is a FHIR Server?
FHIR (Fast Healthcare Interoperability Resources) is a standard for healthcare data exchange. Your FHIR server stores patient data in a standardized format. HDIM queries it via REST API.

### What does the Gateway Service do?
Central routing point that:
- Validates user authentication
- Routes requests to appropriate services
- Injects security headers
- Logs all activity for HIPAA audit trail
- Implements circuit breaking (prevents cascading failures)

### What is CQL?
Clinical Quality Language - a language for expressing clinical logic. HDIM's CQL Engine evaluates expressions like:
- "Is patient > 40 years old AND not pregnant AND no cancer history?"
- Used to determine measure eligibility and compliance

### What are Measure Results?
Classification of patient against a measure:
- **DENOMINATOR**: Patient is eligible for the measure
- **NUMERATOR**: Patient is compliant (meets the quality standard)
- **EXCLUDED**: Patient excluded from measure
- **DENOMINATOR EXCEPTION**: Legitimate reason for non-compliance

### What is a Care Gap?
Gap in clinical care. Examples:
- Patient eligible for breast cancer screening but no screening in past year
- Patient with diabetes but no HbA1c test in 3 months
- Patient with hypertension but no recent BP reading

---

## HDIM Deployment Content Map

```
📦 HDIM Deployment Content
│
├── README.md (Start here!)
│   └─ Overview of all content
│
├── QUICK-START.md (You are here)
│   └─ 30-second overview + key concepts
│
├── 01-ARCHITECTURE-DIAGRAMS.md
│   ├─ System architecture
│   ├─ Gateway pattern
│   ├─ Data flows
│   ├─ Multi-tenant isolation
│   └─ Audit & compliance
│
├── 02-INTEGRATION-PATTERNS.md
│   ├─ FHIR server integration
│   ├─ EHR system integration
│   ├─ Authentication/SSO
│   ├─ Data ingestion patterns
│   └─ Outbound notifications
│
├── 03-DEPLOYMENT-DECISION-TREE.md
│   ├─ Decision questions & paths
│   ├─ Comparison matrices
│   ├─ Implementation examples
│   └─ Deployment model details
│
├── 04-REFERENCE-ARCHITECTURES.md
│   ├─ Single-Node architecture
│   ├─ Clustered architecture
│   ├─ Kubernetes architecture
│   ├─ Hybrid Cloud architecture
│   └─ Custom architecture patterns
│
├── 07-DEPLOYMENT-GUIDES.md (Coming soon)
│   ├─ Step-by-step setup for each model
│   ├─ Infrastructure provisioning
│   ├─ Service configuration
│   ├─ Health checks & validation
│   └─ Troubleshooting
│
└── 09-SECURITY-AND-COMPLIANCE.md (Coming soon)
    ├─ HIPAA compliance
    ├─ Data protection
    ├─ Audit logging
    └─ Network security
```

---

## Who Should Read What?

### Medical Leaders / CIOs
1. Read this Quick Start
2. Read [Architecture Diagrams](./01-ARCHITECTURE-DIAGRAMS.md) (Section 1 & 2)
3. Review [Decision Tree](./03-DEPLOYMENT-DECISION-TREE.md)
4. Check costs in [Reference Architectures](./04-REFERENCE-ARCHITECTURES.md)

### Technical Architects
1. Read this Quick Start
2. Study [Architecture Diagrams](./01-ARCHITECTURE-DIAGRAMS.md) (all sections)
3. Review [Integration Patterns](./02-INTEGRATION-PATTERNS.md)
4. Deep dive: [Reference Architectures](./04-REFERENCE-ARCHITECTURES.md) for chosen model

### Implementation Teams
1. Read this Quick Start
2. Follow [Reference Architectures](./04-REFERENCE-ARCHITECTURES.md) for chosen model
3. Execute [Deployment Guides](./07-DEPLOYMENT-GUIDES.md)
4. Reference [Integration Patterns](./02-INTEGRATION-PATTERNS.md) for FHIR setup

### Medical Informaticists
1. Read this Quick Start
2. Study [Integration Patterns](./02-INTEGRATION-PATTERNS.md)
3. Review [Architecture Diagrams - Data Flows](./01-ARCHITECTURE-DIAGRAMS.md)
4. Check audit/compliance in [Security & Compliance](./09-SECURITY-AND-COMPLIANCE.md)

---

## Success Metrics

After deploying HDIM, you should see:

| Metric | Expected Result |
|--------|---|
| **Measure Completion** | 95%+ (vs. 70% manual) |
| **Care Gap Detection Time** | < 1 hour from data entry |
| **Clinical Efficiency** | 30-50% less time finding gaps |
| **Quality Scores** | 20-30% improvement in 6 months |
| **Patient Compliance** | Faster gap closure |
| **Audit Coverage** | 100% HIPAA audit trail |
| **System Availability** | 99%+ uptime (for HA deployments) |

---

## Getting Help

| Question | Resource |
|----------|----------|
| "What deployment model is right for us?" | [Decision Tree](./03-DEPLOYMENT-DECISION-TREE.md) |
| "How does HDIM integrate with our FHIR server?" | [Integration Patterns](./02-INTEGRATION-PATTERNS.md) |
| "What does the architecture look like?" | [Architecture Diagrams](./01-ARCHITECTURE-DIAGRAMS.md) |
| "What servers do we need?" | [Reference Architectures](./04-REFERENCE-ARCHITECTURES.md) |
| "How do we deploy it?" | [Deployment Guides](./07-DEPLOYMENT-GUIDES.md) |
| "How is data secured?" | [Security & Compliance](./09-SECURITY-AND-COMPLIANCE.md) |

---

## One Last Thing

**HDIM follows the Gateway Pattern:**
```
All requests → Central Gateway → Route to services → Query FHIR server → Results

This means:
✓ Unified authentication (one login for everything)
✓ Centralized audit logging (HIPAA compliance)
✓ Request routing & orchestration (reliability)
✓ Real-time data (always fresh from your FHIR server)
✓ Data stays in your control (no copying)
```

Ready to get started? [Read the Decision Tree](./03-DEPLOYMENT-DECISION-TREE.md) to choose your deployment model!
