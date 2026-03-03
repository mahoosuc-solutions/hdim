# HDIM Technical Architecture Overview

**For Y Combinator Application Review**

---

## What We've Built

HDIM is real-time quality measurement infrastructure for value-based healthcare. We calculate all 52 HEDIS quality measures in <200ms when a patient chart opens, vs. overnight batch processing from legacy systems.

---

## Core Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CLINICAL PORTAL (Angular)                            │
│  Patient 360 │ Care Gaps │ Quality Dashboard │ Analytics │ AI Agents        │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
┌──────────────────────────────────▼──────────────────────────────────────────┐
│                           API GATEWAY                                        │
│                    Auth │ Rate Limiting │ Routing                            │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         ▼                         ▼                         ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   FHIR Service  │    │   CQL Engine    │    │  Quality Svc    │
│                 │    │                 │    │                 │
│ • FHIR R4 API   │    │ • Template-     │    │ • 52 HEDIS      │
│ • Patient Data  │    │   driven logic  │    │   measures      │
│ • Clinical Obs  │    │ • <200ms eval   │    │ • Compliance    │
└────────┬────────┘    └────────┬────────┘    └────────┬────────┘
         │                      │                      │
         └──────────────────────┼──────────────────────┘
                                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          DATA LAYER                                          │
│  PostgreSQL (Multi-tenant)  │  Redis (5-min TTL)  │  Kafka (Events)         │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Technical Differentiators

| Capability | HDIM | Epic/Cerner |
|------------|------|-------------|
| **Measure Evaluation** | <200ms real-time | 24-48 hour batch |
| **Data Model** | FHIR R4 native | Proprietary |
| **Deployment** | Days (SaaS) | 6-12 months |
| **New Measures** | Hours (template) | Weeks (code deploy) |
| **Multi-Tenant** | Built-in | Single-tenant |
| **Starting Price** | $80/month | $50K+/month |

---

## Key Services (13 Microservices)

| Service | Purpose | Tech |
|---------|---------|------|
| **CQL Engine** | Execute quality measure logic | Spring Boot, CQL |
| **FHIR Service** | Patient data API | HAPI FHIR 7.x |
| **Quality Measure** | 52 HEDIS measures | Template-driven |
| **Care Gap** | Identify/prioritize gaps | Event-driven |
| **Patient Service** | Patient registry | PostgreSQL |
| **Gateway** | Auth, routing | JWT, Spring Security |
| **Agent Builder** | No-code AI agents | Tenant-isolated |
| **Agent Runtime** | LLM orchestration | Claude/Azure/Bedrock |

---

## Technology Stack

- **Backend**: Java 21, Spring Boot 3.x, Gradle 8.11
- **Frontend**: Angular 20, Material Design 3
- **Database**: PostgreSQL 16 (multi-tenant), Redis 7
- **Messaging**: Apache Kafka 3.x
- **FHIR**: HAPI FHIR 7.x (R4 compliant)
- **CQL**: Template-driven engine with <200ms evaluation
- **Security**: JWT, HIPAA audit logging, AES-256 encryption
- **AI**: Multi-provider (Claude, Azure OpenAI, AWS Bedrock)

---

## Compliance & Security

- **HIPAA**: Full compliance with PHI handling
  - 5-minute max cache TTL
  - AES-256 encryption at rest
  - Audit logging (7-year retention)
  - Multi-tenant data isolation

- **Standards**: FHIR R4, CQL 1.5, HL7 v2

- **Multi-Tenancy**:
  - Row-level tenant isolation
  - X-Tenant-ID header enforcement
  - No cross-tenant data leakage

---

## Scalability

- **Horizontal**: Stateless services, Kubernetes-ready
- **Performance**: <200ms P95 for measure evaluation
- **Batch**: 10-40x faster than legacy (concurrent CQL)
- **Throughput**: 10K+ patients/minute batch processing

---

## What This Enables

1. **Point-of-Care Alerts**: Care gaps appear when chart opens
2. **Real-Time Dashboards**: Quality scores update instantly
3. **AI Assistance**: Clinical decision support via LLM agents
4. **Rapid Deployment**: Integrate any EHR in days, not months
5. **Cost Reduction**: 100x cheaper than enterprise alternatives

---

## Code Repository

- **Total Services**: 13 microservices + 2 frontends
- **Lines of Code**: ~150K (Java + TypeScript)
- **Test Coverage**: Unit + Integration + TDD approach
- **CI/CD**: GitHub Actions, Docker, Kubernetes

---

## Founder

**Aaron Bentley** - Founder & CEO

Years of public health IT experience. Built this entire platform using AI-assisted development—proving that modern tools can democratize healthcare technology.

*"Built in memory of my mother, who died at 54 from breast cancer. I believe with better data and care, she and many others could have lived longer, more productive lives."*

---

## Contact

Ready for technical deep-dive with YC reviewers.

- Email: aaron@hdim.health
- GitHub access available upon request
