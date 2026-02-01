# HDIM Skills & Learning Center

Welcome to the HDIM Skills Development Hub! This directory contains comprehensive learning guides for all major technical skills required to work effectively with the HealthData-in-Motion platform.

## 🎯 Quick Navigation

### Choose Your Role

- **👨‍💻 Backend Engineer** → Start: [CQRS + Event Sourcing](#architecture--design)
- **🏗️ DevOps/Infrastructure** → Start: [Docker & Compose](#infrastructure--devops)
- **🔐 Security/Compliance** → Start: [HIPAA Compliance](#security--compliance)
- **✅ QA/Test Engineer** → Start: [Testing & QA](#testing--quality-assurance)
- **🏥 Healthcare Domain** → Start: [HEDIS Quality Measures](#healthcare-domain)

---

## 📚 Skill Categories

### 1. Architecture & Design (🔴 CRITICAL)

**Foundation patterns that define system behavior**

| Guide | Priority | Duration | Status |
|-------|----------|----------|--------|
| [CQRS + Event Sourcing](#) | 🔴 Critical | 1 week | Coming soon |
| [Microservices Patterns](#) | 🔴 Critical | 1 week | Coming soon |
| [Multi-Gateway Architecture](#) | 🔴 Critical | 1 week | Coming soon |
| [Multi-Tenant Architecture](#) | 🔴 Critical | 1 week | Coming soon |
| [Distributed Tracing & Observability](#) | 🟡 Important | 3 days | Coming soon |

**Key Services:** patient-event-service, quality-measure-event-service, care-gap-event-service, clinical-workflow-event-service

**Recommended Learning Order:**
1. Start with CQRS/Event Sourcing (foundation)
2. Then Multi-Tenant Architecture (infrastructure pattern)
3. Then Microservices Patterns (service communication)
4. Then Multi-Gateway Architecture (January 2026 redesign)
5. Finally Distributed Tracing (observability)

---

### 2. Healthcare Domain (🔴 CRITICAL)

**Business logic: HEDIS measures, FHIR standards, care gaps, risk assessment**

| Guide | Priority | Duration | Status |
|-------|----------|----------|--------|
| [HEDIS Quality Measures](#) | 🔴 Critical | 2 weeks | Coming soon |
| [CQL Execution](#) | 🔴 Critical | 1.5 weeks | Coming soon |
| [FHIR R4 Integration](#) | 🔴 Critical | 2 weeks | Coming soon |
| [Care Gap Detection](#) | 🔴 Critical | 1.5 weeks | Coming soon |
| [Risk Stratification Models](#) | 🟡 Important | 1 week | Coming soon |
| [EHR Integration & Data Standards](#) | 🟡 Important | 1 week | Coming soon |
| [SDOH Screening](#) | 🟡 Important | 3 days | Coming soon |

**Key Services:** quality-measure-service, cql-engine-service, fhir-service, care-gap-service, analytics-service

**Recommended Learning Order:**
1. HEDIS Quality Measures (what problem are we solving?)
2. FHIR R4 Integration (where does data come from?)
3. CQL Execution (how do we evaluate measures?)
4. Care Gap Detection (what do we do with results?)
5. Risk Stratification (how do we prioritize?)
6. EHR Integration (where do we get data?)

---

### 3. Security & Compliance (🔴 CRITICAL)

**HIPAA requirements, authentication, data protection, audit logging**

| Guide | Priority | Duration | Status |
|-------|----------|----------|--------|
| [HIPAA Compliance](#) | 🔴 Critical | 1.5 weeks | Coming soon |
| [Authentication & Authorization](#) | 🟡 Important | 1 week | Coming soon |
| [Data Protection & Privacy](#) | 🟡 Important | 1 week | Coming soon |
| [Audit & Compliance Logging](#) | 🟡 Important | 1 week | Coming soon |

**Key Concepts:**
- PHI (Protected Health Information) protection
- Cache control for sensitive data
- Multi-tenant isolation enforcement
- HIPAA audit trail requirements
- Encryption at rest & in transit

**Recommended Learning Order:**
1. HIPAA Compliance (non-negotiable requirements)
2. Authentication & Authorization (Gateway trust pattern)
3. Data Protection (encryption, key management)
4. Audit Logging (compliance trail)

---

### 4. Data Persistence (🔴 CRITICAL)

**PostgreSQL, Liquibase, JPA/Hibernate, Redis caching**

| Guide | Priority | Duration | Status |
|-------|----------|----------|--------|
| [PostgreSQL Multi-Tenant Database](#) | 🔴 Critical | 1.5 weeks | Coming soon |
| [Liquibase Migration Management](#) | 🔴 Critical | 1.5 weeks | Coming soon |
| [JPA/Hibernate Entity Design](#) | 🔴 Critical | 1 week | Coming soon |
| [Redis Caching Strategy](#) | 🟡 Important | 3 days | Coming soon |

**Key Concepts:**
- Multi-tenant query filtering
- Entity-migration synchronization (CRITICAL)
- Liquibase rollback coverage (100%)
- Cache key namespacing
- HIPAA-compliant TTL (≤5 min for PHI)

**Recommended Learning Order:**
1. PostgreSQL Multi-Tenant Database (foundation)
2. Liquibase Migration Management (schema evolution)
3. JPA/Hibernate Entity Design (ORM mapping)
4. Redis Caching Strategy (performance optimization)

---

### 5. Messaging & Event-Driven (🟡 IMPORTANT)

**Apache Kafka, event sourcing, async processing, resilience patterns**

| Guide | Priority | Duration | Status |
|-------|----------|----------|--------|
| [Apache Kafka Event Streaming](#) | 🟡 Important | 1.5 weeks | Coming soon |
| [Event-Driven Patterns](#) | 🟡 Important | 1 week | Coming soon |
| [Async Processing & Resilience](#) | 🟡 Important | 1 week | Coming soon |

**Key Concepts:**
- Topics: patient.events, measure.evaluation.complete, care-gap.detected, audit.events
- Producer/consumer patterns
- Consumer groups & offset management
- Dead-letter queues (DLQ)
- Idempotent event processing

**Recommended Learning Order:**
1. Apache Kafka Event Streaming (foundation)
2. Event-Driven Patterns (design patterns)
3. Async Processing & Resilience (error handling)

---

### 6. Testing & Quality Assurance (🔴 CRITICAL)

**Unit testing, integration testing, entity-migration validation, performance testing**

| Guide | Priority | Duration | Status |
|-------|----------|----------|--------|
| [Unit Testing](#) | 🔴 Critical | 1 week | Coming soon |
| [Integration Testing](#) | 🔴 Critical | 1.5 weeks | Coming soon |
| [Entity-Migration Validation Testing](#) | 🔴 Critical | 1.5 weeks | Coming soon |
| [Performance & Load Testing](#) | 🟡 Important | 1 week | Coming soon |
| [Security Testing](#) | 🟡 Important | 1 week | Coming soon |

**Key Concepts:**
- JUnit 5 + Mockito patterns
- MockMvc integration testing
- Liquibase validation at test time
- Multi-tenant isolation testing
- RBAC permission testing

**Recommended Learning Order:**
1. Unit Testing (service layer)
2. Integration Testing (API endpoints)
3. Entity-Migration Validation Testing (CRITICAL - prevents production failures)
4. Performance Testing (latency profiling)
5. Security Testing (cross-tenant, RBAC)

---

### 7. Spring Boot & Java (🔴 CRITICAL)

**Spring Boot 3.x, Spring Data JPA, Spring Security, REST API design**

| Guide | Priority | Duration | Status |
|-------|----------|----------|--------|
| [Spring Boot 3.x Microservices](#) | 🔴 Critical | 2 weeks | Coming soon |
| [Spring Data JPA & Repository Pattern](#) | 🔴 Critical | 1 week | Coming soon |
| [Spring Security & RBAC](#) | 🔴 Critical | 1.5 weeks | Coming soon |
| [REST API Design](#) | 🟡 Important | 1 week | Coming soon |
| [Gradle & Build Automation](#) | 🟡 Important | 1 week | Coming soon |

**Key Concepts:**
- Dependency injection & Spring beans
- Spring Boot configuration & auto-configuration
- Spring Data repository interfaces
- Spring Security authorization
- RESTful API design patterns

**Recommended Learning Order:**
1. Spring Boot 3.x Microservices (foundation)
2. Spring Data JPA (data access layer)
3. Spring Security & RBAC (authorization)
4. REST API Design (API contracts)
5. Gradle & Build Automation (build system)

---

### 8. Infrastructure & DevOps (🟡 IMPORTANT)

**Docker/Compose, Kubernetes, monitoring, observability**

| Guide | Priority | Duration | Status |
|-------|----------|----------|--------|
| [Docker & Docker Compose](#) | 🟡 Important | 1 week | Coming soon |
| [Kubernetes Deployment](#) | 🟢 Optional | 2 weeks | Coming soon |
| [Monitoring & Observability](#) | 🟡 Important | 1 week | Coming soon |

**Key Concepts:**
- Container configuration & orchestration
- Service dependencies
- Volume management
- Health checks & readiness probes
- Metrics collection (Prometheus)
- Dashboards (Grafana)

**Recommended Learning Order:**
1. Docker & Docker Compose (local development)
2. Monitoring & Observability (production insights)
3. Kubernetes Deployment (optional - production orchestration)

---

### 9. API Design & Integration (🟡 IMPORTANT)

**OpenAPI/Swagger, API integration testing, API contracts**

| Guide | Priority | Duration | Status |
|-------|----------|----------|--------|
| [OpenAPI/Swagger Documentation](#) | 🟡 Important | 3 days | Coming soon |
| [API Integration Testing](#) | 🟡 Important | 1 week | Coming soon |

---

### 10. Coding Standards & Best Practices (🟡 IMPORTANT)

**HDIM coding patterns, code review, best practices**

| Guide | Priority | Duration | Status |
|-------|----------|----------|--------|
| [HDIM Coding Patterns](#) | 🟡 Important | 1 week | Coming soon |
| [Code Review Checklist](#) | 🟡 Important | 1 week | Coming soon |
| [Architecture & Design Patterns](#) | 🟡 Important | 1 week | Coming soon |

---

## 🎓 Learning Paths

### Backend Engineer (11 weeks)

1. **Week 1:** CQRS + Event Sourcing
2. **Weeks 2-3:** HEDIS Quality Measures + HIPAA Compliance
3. **Weeks 4-5:** FHIR R4 + CQL Execution
4. **Week 6:** Multi-Tenant + PostgreSQL
5. **Weeks 7-8:** Spring Boot + Security
6. **Week 9:** Kafka + Testing
7. **Weeks 10-11:** Care Gap + Risk Stratification

### DevOps/Infrastructure (8 weeks)

1. **Week 1:** Docker/Compose + Multi-Tenant Architecture
2. **Weeks 2-3:** OpenTelemetry + Monitoring
3. **Weeks 4-6:** Kubernetes (2 weeks)
4. **Weeks 7-8:** Performance Testing + Disaster Recovery

### Security/Compliance (6 weeks)

1. **Weeks 1-2:** HIPAA Compliance + Data Protection
2. **Weeks 3-4:** Authentication/Authorization + Spring Security
3. **Weeks 5-6:** Container Security + Audit Logging

### QA/Test Engineer (7 weeks)

1. **Weeks 1-2:** Unit + Integration Testing
2. **Week 3:** Entity-Migration Validation
3. **Weeks 4-5:** Performance + Security Testing
4. **Weeks 6-7:** Test Automation + CI/CD

---

## 📋 How to Use This Repository

### For Individual Learning

1. **Identify your role** (Backend, DevOps, Security, QA)
2. **Follow the recommended learning path** for your role
3. **Read guides in order** - they build on each other
4. **Study the code examples** - they're from actual HDIM services
5. **Practice what you learn** - write code, run tests, make changes
6. **Review the quick reference** - checklists at end of each guide

### For Team Onboarding

1. **New team member?** → Start with this README
2. **First week:** Core skills (CQRS, HIPAA, multi-tenant)
3. **Second week:** Role-specific skills (backend, DevOps, etc.)
4. **Third week:** Hands-on with real codebase
5. **Productive by week 4** - Contributing features

### For Code Reviews

- Reference guides when reviewing code
- Use checklists to verify compliance
- Link specific guide sections in PR comments
- Example: "See [HIPAA Compliance guide](#) section on Cache Control"

---

## 📚 Guide Structure

Each skill guide follows this structure:

```
# Skill Name

## Overview
- What is this skill?
- Why is it important for HDIM?
- Business impact

## Key Concepts
- Core concept 1
- Core concept 2
- Core concept 3

## Architecture Pattern
- How does it work?
- Diagrams/flows
- Design decisions
- Trade-offs

## Implementation Guide
- Step-by-step implementation
- Code examples from HDIM
- Best practices

## Real-World Examples
- Example 1 from HDIM codebase
- Example 2 from HDIM codebase
- References to actual services

## Best Practices
- Do's and Don'ts
- Common mistakes
- Performance considerations

## Testing Strategies
- Unit test examples
- Integration test examples
- Test patterns

## Troubleshooting
- Common issues
- Solutions
- Debug techniques

## References
- HDIM documentation links
- External resources
- Related skills

## Quick Reference Checklist
- [ ] Concept 1
- [ ] Concept 2
- [ ] Concept 3
```

---

## 🔗 Related Documentation

### HDIM Documentation Hub
- **Main Skills Roadmap:** `docs/SKILLS_ROADMAP.md` (Comprehensive skill matrix)
- **Documentation Portal:** `docs/README.md` (1,411+ pages)
- **Service Catalog:** `docs/services/SERVICE_CATALOG.md` (All 51 services)
- **Architecture Hub:** `docs/architecture/` (System design patterns)

### Backend Technical Guides
- ⭐ **Liquibase Workflow:** `backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md`
- ⭐ **Entity-Migration:** `backend/docs/ENTITY_MIGRATION_GUIDE.md`
- **Event Sourcing:** `docs/architecture/EVENT_SOURCING_ARCHITECTURE.md`
- **Gateway Design:** `docs/architecture/GATEWAY_ARCHITECTURE.md`
- **Coding Standards:** `backend/docs/CODING_STANDARDS.md`
- **Command Reference:** `backend/docs/COMMAND_REFERENCE.md`

### Compliance & Security
- **HIPAA Compliance:** `backend/HIPAA-CACHE-COMPLIANCE.md`
- **Distributed Tracing:** `backend/docs/DISTRIBUTED_TRACING_GUIDE.md`
- **Gateway Trust:** `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`

---

## 🚀 Getting Started Today

### Option 1: I'm New to HDIM
1. Read: `docs/SKILLS_ROADMAP.md` (30 min overview)
2. Read: This README (you're here!)
3. Choose your role
4. Start with the first guide in your learning path

### Option 2: I Know What I Need
1. Find the specific skill guide you need
2. Jump to the "Implementation Guide" section
3. Use "Quick Reference Checklist" for quick review

### Option 3: I'm Stuck on Something
1. Search for your topic in the skills directory
2. Check the "Troubleshooting" section
3. Reference the external links
4. Ask for help with specific context

---

## 📖 Quick Reference by Topic

### "How do I...?"

| Question | Guide | Section |
|----------|-------|---------|
| Create a service? | Spring Boot 3.x | Implementation Guide |
| Create a database table? | Liquibase Migrations | Implementation Guide |
| Publish an event? | Kafka Event Streaming | Implementation Guide |
| Handle PHI safely? | HIPAA Compliance | Cache Control |
| Test my code? | Unit Testing | Implementation Guide |
| Deploy to production? | Docker & Compose | Coming Soon |
| Debug performance? | Monitoring & Observability | Coming Soon |
| Isolate tenants? | Multi-Tenant Architecture | Implementation Guide |

---

## ✅ Verify Your Learning

After completing each guide, verify your understanding:

1. **Can you explain it?** - Teach it to a colleague
2. **Can you code it?** - Write a small example
3. **Can you test it?** - Write unit + integration tests
4. **Can you review it?** - Review a PR for that skill
5. **Can you troubleshoot it?** - Solve a related problem

---

## 📝 Contributing

Found an issue? Have a suggestion?
1. Check existing guides
2. File an issue with specific suggestions
3. Submit PR with improvements
4. Help newer team members learn!

---

## 📞 Need Help?

- **Technical Question?** → Refer to specific skill guide
- **Stuck on Code?** → Check "Troubleshooting" section
- **Still Lost?** → Read `docs/SKILLS_ROADMAP.md` executive summary
- **Architecture Question?** → Check `docs/architecture/` guides

---

**Last Updated:** January 20, 2026
**Status:** Foundation Release (Guides Coming Soon)
**Maintained by:** HDIM Development Team

---

**👉 [Start Learning Now](#-learning-paths)**
