# HDIM 90-Day Implementation Roadmap

**Document Purpose**: Executive guide for CTOs, CIOs, and Implementation Directors
**Target Audience**: Healthcare IT decision-makers and implementation teams
**Last Updated**: January 2026

---

## Executive Summary

HDIM can be implemented in **90 days or less** from contract signing to production go-live. This roadmap provides three deployment paths tailored to your organization's scope and complexity:

| Path | Scope | Timeline | Best For |
|------|-------|----------|----------|
| **Pilot** | 1 measure, 1 facility, 5K patients | 6 weeks | Initial evaluation, proof of concept |
| **Production** | Full HEDIS measure set, organization-wide | 12 weeks | Standard deployment, single EHR |
| **Enterprise** | Multi-region, multi-EHR, 500K+ patients | 16 weeks | Health systems, ACOs, health plans |

**Key Success Factors**:
- Early FHIR endpoint access (Week 1 blocker if delayed)
- Dedicated technical resource for authentication setup
- Executive sponsor for organizational alignment

---

## Visual Timeline Overview

```
                                    HDIM 90-Day Implementation

    ┌─────────────────────────────────────────────────────────────────────────────┐
    │  WEEKS 1-4                    WEEKS 5-8                    WEEKS 9-12       │
    │  Foundation                   Integration                  Production       │
    ├─────────────────────────────────────────────────────────────────────────────┤
    │                                                                             │
    │  Week 1-2: Infrastructure     Week 5-6: Data Flow        Week 9-10: UAT    │
    │  ├─ Server provisioning       ├─ FHIR queries            ├─ User testing   │
    │  ├─ Docker/K8s setup          ├─ Patient matching        ├─ Feedback       │
    │  ├─ Database deployment       ├─ Bulk data loading       ├─ Bug fixes      │
    │  └─ Network/firewall          └─ Cache optimization      └─ Documentation  │
    │                                                                             │
    │  Week 3-4: Authentication     Week 7-8: Validation       Week 11-12: Go-Live│
    │  ├─ OAuth2/SMART setup        ├─ Measure testing         ├─ Production data│
    │  ├─ Gateway configuration     ├─ Manual comparison       ├─ Cutover        │
    │  ├─ FHIR endpoint testing     ├─ Performance tuning      ├─ Training       │
    │  └─ User provisioning         └─ Security audit          └─ Hypercare      │
    │                                                                             │
    │  [GATE 1]                     [GATE 2]                   [GATE 3]          │
    │  Technical Readiness          Data Quality               Go-Live Approval   │
    │  Week 2                       Week 6                     Week 10           │
    │                                                                             │
    └─────────────────────────────────────────────────────────────────────────────┘
```

---

## Phase 1: Foundation (Weeks 1-4)

### Week 1-2: Infrastructure Setup

**Objective**: Deploy HDIM platform and configure core infrastructure.

**Deliverables**:

| Task | Owner | Time Estimate | Dependencies |
|------|-------|---------------|--------------|
| Provision servers (cloud VM or on-prem) | DevOps | 2-4 hours | Server specs finalized |
| Install Docker Engine or Kubernetes | DevOps | 1-2 hours | Root access confirmed |
| Deploy PostgreSQL 15 with multi-tenant schemas | DBA | 2-4 hours | Storage allocated |
| Deploy Redis 7 with Sentinel HA | DevOps | 1-2 hours | Network configured |
| Deploy Kong API Gateway | DevOps | 1-2 hours | TLS certificates ready |
| Pull HDIM container images | DevOps | 30 minutes | Registry access |
| Start core services (Gateway, FHIR, Quality Measure) | DevOps | 1 hour | All dependencies up |
| Configure Prometheus + Grafana monitoring | DevOps | 2-4 hours | Optional but recommended |

**Infrastructure Requirements** (Minimum for Pilot):

```
Server Specs:
├─ CPU: 4 cores (8 recommended)
├─ RAM: 16GB (32GB recommended)
├─ Storage: 500GB SSD
├─ Network: 100 Mbps
└─ OS: Ubuntu 20.04 LTS or CentOS 8+

Ports Required:
├─ 8000: Kong API Gateway (external)
├─ 8001: HDIM Gateway Service (internal)
├─ 8081-8100: Microservices (internal)
├─ 5435: PostgreSQL (internal)
├─ 6380: Redis (internal)
├─ 9090: Prometheus (internal)
└─ 3001: Grafana (internal)
```

**Validation Checklist**:
- [ ] All containers running: `docker-compose ps`
- [ ] Health check passing: `curl http://localhost:8000/health`
- [ ] Database accessible: `psql -U healthdata -d healthdata_qm`
- [ ] Redis cache operational: `redis-cli ping`

---

### Week 3-4: Authentication & FHIR Configuration

**Objective**: Establish secure connectivity to your EHR FHIR endpoints.

**Deliverables**:

| Task | Owner | Time Estimate | Dependencies |
|------|-------|---------------|--------------|
| Obtain FHIR endpoint URLs from EHR vendor | IT Lead | 1-2 days | Vendor relationship |
| Register OAuth2 client application | IT Lead | 1-2 days | Epic/Cerner admin portal |
| Generate OAuth2 client credentials | IT Lead | 1 hour | App registration complete |
| Configure HDIM FHIR connector with credentials | Integration Engineer | 2-4 hours | Credentials received |
| Test authentication flow (OAuth2 token exchange) | Integration Engineer | 2-4 hours | Credentials configured |
| Execute sample FHIR queries (Patient, Condition) | Integration Engineer | 2-4 hours | Auth working |
| Configure gateway trust authentication (HMAC signing) | Integration Engineer | 2-4 hours | Gateway deployed |
| Provision user accounts (admin, evaluator, viewer) | IT Lead | 1-2 hours | SSO integration |

**Authentication Configuration Example** (Epic):

```yaml
# application.yml - FHIR Connector Configuration
hdim:
  fhir:
    server:
      url: https://fhir.your-hospital.org/api/FHIR/R4
      auth:
        type: oauth2
        client-id: ${EPIC_CLIENT_ID}
        private-key-path: /secrets/epic-private-key.pem
        token-url: https://fhir.your-hospital.org/oauth2/token
        scopes:
          - patient/*.read
          - user/*.read
        jwt-algorithm: RS384
        token-cache-ttl: 55m  # Tokens valid for 60 minutes
```

**Authentication Flow Diagram**:

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ HDIM FHIR   │     │ OAuth2      │     │ Epic        │     │ Epic FHIR   │
│ Connector   │     │ Token Cache │     │ Auth Server │     │ Server      │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │                   │
       │ 1. Check cache    │                   │                   │
       │──────────────────>│                   │                   │
       │                   │                   │                   │
       │   [Cache Miss]    │                   │                   │
       │<──────────────────│                   │                   │
       │                   │                   │                   │
       │ 2. Request token (JWT assertion)      │                   │
       │───────────────────────────────────────>                   │
       │                   │                   │                   │
       │ 3. Access token (valid 60 min)        │                   │
       │<───────────────────────────────────────                   │
       │                   │                   │                   │
       │ 4. Store in cache │                   │                   │
       │──────────────────>│                   │                   │
       │                   │                   │                   │
       │ 5. FHIR query (Bearer token)                              │
       │────────────────────────────────────────────────────────────>
       │                   │                   │                   │
       │ 6. FHIR response (Patient, Condition, etc.)               │
       │<────────────────────────────────────────────────────────────
       │                   │                   │                   │
```

**Validation Checklist**:
- [ ] OAuth2 token acquired successfully
- [ ] Patient query returns data: `GET /Patient?_count=10`
- [ ] Condition query returns data: `GET /Condition?_count=10`
- [ ] Token refresh working after 55 minutes
- [ ] Error handling for 401/429 responses configured

---

### Decision Gate 1: Technical Readiness (End of Week 2)

**Go Criteria**:
- [ ] All HDIM services running and healthy
- [ ] Database accessible with correct schemas
- [ ] Redis cache operational
- [ ] Monitoring dashboards showing metrics
- [ ] DevOps team comfortable with Docker/K8s operations

**No-Go Indicators**:
- Infrastructure provisioning delays > 3 days
- Network connectivity issues to cloud services
- Storage capacity insufficient
- Team not trained on HDIM operations

**Escalation Path**: If No-Go, schedule emergency call with HDIM technical support.

---

## Phase 2: Integration & Validation (Weeks 5-8)

### Week 5-6: Data Flow & Patient Matching

**Objective**: Establish reliable data flow from EHR to HDIM with accurate patient matching.

**Deliverables**:

| Task | Owner | Time Estimate | Dependencies |
|------|-------|---------------|--------------|
| Configure FHIR resource type mappings | Integration Engineer | 4-8 hours | FHIR queries working |
| Set up patient matching via MPI or FHIR identifiers | Integration Engineer | 4-8 hours | MRN mapping confirmed |
| Execute bulk FHIR $export (if available) | Integration Engineer | 2-4 hours | Bulk export enabled |
| Load initial patient population (sample 1,000 patients) | Integration Engineer | 2-4 hours | Data agreements signed |
| Validate FHIR data quality (completeness, accuracy) | QA Engineer | 8-16 hours | Sample data loaded |
| Configure CQL measure library (HEDIS 2024) | Clinical Analyst | 4-8 hours | Measure set selected |
| Execute first measure evaluation (BCS-E: Breast Cancer Screening) | Clinical Analyst | 2-4 hours | CQL library loaded |
| Compare results to manual calculation | Clinical Analyst | 4-8 hours | Manual baseline ready |

**FHIR Resource Types Required for HEDIS Measures**:

```
Core Resources (Required):
├─ Patient           - Demographics, identifiers
├─ Condition         - Active diagnoses (ICD-10)
├─ Observation       - Lab results (HbA1c, LDL, BMI)
├─ Procedure         - Completed procedures (CPT)
├─ MedicationRequest - Prescriptions
├─ Immunization      - Vaccines administered
├─ Encounter         - Visit history
└─ Practitioner      - Attending provider

Extended Resources (Optional but Helpful):
├─ DiagnosticReport  - Lab and imaging reports
├─ AllergyIntolerance- Drug allergies
├─ CarePlan          - Care coordination
└─ Goal              - Treatment goals
```

**Data Quality Validation Queries**:

```bash
# Check patient count
curl "http://localhost:8000/fhir/Patient?_summary=count"
# Expected: {"total": 1000+}

# Check condition coverage (should have active diagnoses)
curl "http://localhost:8000/fhir/Condition?clinical-status=active&_summary=count"
# Expected: {"total": 2000+} (avg 2+ conditions per patient)

# Check observation coverage (should have recent labs)
curl "http://localhost:8000/fhir/Observation?date=ge2025-01-01&_summary=count"
# Expected: {"total": 500+} (recent lab results)
```

**Validation Checklist**:
- [ ] Patient count matches source EHR
- [ ] Condition resources have ICD-10 codes
- [ ] Observation resources have LOINC codes
- [ ] MedicationRequest resources have RxNorm codes
- [ ] First measure evaluation completes without errors
- [ ] Results within 10% of manual calculation

---

### Week 7-8: Measure Validation & Performance

**Objective**: Validate measure calculations against manual baselines and optimize performance.

**Deliverables**:

| Task | Owner | Time Estimate | Dependencies |
|------|-------|---------------|--------------|
| Execute all selected measures against test population | Clinical Analyst | 8-16 hours | All measures configured |
| Compare results to historical HEDIS submissions | Clinical Analyst | 8-16 hours | Previous year data available |
| Identify and resolve calculation discrepancies | Clinical Analyst + Engineer | 8-24 hours | Discrepancies documented |
| Load production-scale patient population (10K-100K) | Integration Engineer | 4-8 hours | Infrastructure scaled |
| Performance test: evaluate 1,000 patients in <10 minutes | QA Engineer | 4-8 hours | Production data loaded |
| Configure Redis caching for FHIR queries (5-min TTL) | Integration Engineer | 2-4 hours | Performance baseline set |
| Security audit: HIPAA compliance verification | Security Engineer | 8-16 hours | Audit checklist ready |
| Document any configuration deviations | Technical Writer | 4-8 hours | Implementation notes |

**Performance Benchmarks (Target)**:

| Metric | Pilot | Production | Enterprise |
|--------|-------|------------|------------|
| **Patients** | 5,000 | 50,000 | 500,000 |
| **Measures** | 1-5 | 25-50 | 50+ |
| **Evaluation Time (per patient)** | <600ms | <500ms | <400ms |
| **Full Population Evaluation** | <30 min | <4 hours | <12 hours |
| **FHIR Query Latency (p95)** | <200ms | <150ms | <100ms |
| **Cache Hit Rate** | >60% | >80% | >90% |

**Performance Optimization Checklist**:
- [ ] Redis caching enabled for FHIR queries
- [ ] Connection pooling configured for database
- [ ] JVM heap size optimized for CQL engine (1GB-4GB)
- [ ] Bulk FHIR queries using `_include` for related resources
- [ ] Pagination implemented for large result sets

---

### Decision Gate 2: Data Quality (End of Week 6)

**Go Criteria**:
- [ ] Patient data matches source EHR (>98% accuracy)
- [ ] Measure calculations within 5% of manual baseline
- [ ] All selected measures execute without errors
- [ ] Performance meets or exceeds targets
- [ ] No critical security vulnerabilities identified

**No-Go Indicators**:
- Data discrepancies > 10% from source
- Measure calculations systematically incorrect
- Performance < 50% of target benchmarks
- HIPAA compliance issues identified

**Escalation Path**: If No-Go, extend Phase 2 by 2 weeks, assign dedicated data quality resource.

---

## Phase 3: Production Go-Live (Weeks 9-12)

### Week 9-10: User Acceptance Testing

**Objective**: Validate system meets clinical and operational requirements with end users.

**Deliverables**:

| Task | Owner | Time Estimate | Dependencies |
|------|-------|---------------|--------------|
| Deploy HDIM Clinical Portal (Angular frontend) | DevOps | 2-4 hours | Backend validated |
| Configure user roles (admin, evaluator, analyst, viewer) | IT Lead | 2-4 hours | SSO configured |
| Train care managers on care gap workflow | Training Lead | 4-8 hours | Training materials ready |
| Train quality team on measure evaluation | Training Lead | 4-8 hours | Training materials ready |
| Execute UAT test scenarios (10-20 scenarios) | QA + Clinical | 8-16 hours | Test scripts ready |
| Document defects and prioritize fixes | QA + Engineering | 8-16 hours | Bug tracking configured |
| Address high/critical defects | Engineering | 8-24 hours | Defects prioritized |
| Conduct UAT sign-off meeting | Project Manager | 2 hours | All defects addressed |

**UAT Test Scenarios (Sample)**:

| # | Scenario | Expected Result | Pass/Fail |
|---|----------|-----------------|-----------|
| 1 | Login as care manager via SSO | Dashboard displays | |
| 2 | View patient care gaps | Gap list populated | |
| 3 | Mark care gap as closed | Gap status updated | |
| 4 | Run measure evaluation for 100 patients | Results displayed in <2 min | |
| 5 | Export QRDA Category III report | Valid XML generated | |
| 6 | Search patient by MRN | Patient record found | |
| 7 | View measure dashboard | Charts render correctly | |
| 8 | Switch between tenants | Data isolated correctly | |
| 9 | Access denied for unauthorized user | 403 error returned | |
| 10 | API rate limiting triggered | 429 error with retry-after | |

**Validation Checklist**:
- [ ] All UAT test scenarios passed
- [ ] No high/critical defects open
- [ ] Clinical users signed off on workflow
- [ ] Quality team validated measure results
- [ ] Training completion documented

---

### Week 11-12: Production Deployment & Go-Live

**Objective**: Deploy to production environment and transition to operational support.

**Deliverables**:

| Task | Owner | Time Estimate | Dependencies |
|------|-------|---------------|--------------|
| Provision production infrastructure (HA configuration) | DevOps | 4-8 hours | Capacity planning complete |
| Deploy HDIM to production environment | DevOps | 2-4 hours | Production infra ready |
| Configure production FHIR endpoint (prod OAuth2 credentials) | Integration Engineer | 2-4 hours | Prod credentials received |
| Execute production data migration (from pilot) | DBA | 4-8 hours | Backup/restore tested |
| Configure production monitoring and alerts | DevOps | 4-8 hours | PagerDuty/Slack configured |
| Execute production smoke tests | QA | 2-4 hours | Test scripts ready |
| Cutover from legacy system (if applicable) | Project Manager | 4-8 hours | Cutover plan approved |
| Announce go-live to organization | Project Manager | 1 hour | Executive approval |
| Begin 30-day hypercare period | Support Team | Ongoing | Support team on standby |

**Go-Live Checklist**:

```
Pre-Go-Live (Day -3 to -1):
├─ [ ] Production infrastructure validated
├─ [ ] Production FHIR connectivity confirmed
├─ [ ] Production data migrated and verified
├─ [ ] Monitoring and alerting configured
├─ [ ] Support team briefed and on-call schedule set
├─ [ ] Rollback plan documented and tested
├─ [ ] Executive sign-off obtained
└─ [ ] Go/No-Go decision made

Go-Live Day:
├─ [ ] DNS cutover (if applicable)
├─ [ ] User notifications sent
├─ [ ] First production logins confirmed
├─ [ ] First production measure evaluation executed
├─ [ ] No critical issues in first 2 hours
└─ [ ] Go-live announcement sent

Post-Go-Live (Day +1 to +30):
├─ [ ] Daily health check (8 AM)
├─ [ ] Issue triage and resolution
├─ [ ] User feedback collection
├─ [ ] Performance monitoring
├─ [ ] Weekly status report to stakeholders
└─ [ ] 30-day hypercare sign-off
```

---

### Decision Gate 3: Go-Live Approval (End of Week 10)

**Go Criteria**:
- [ ] All UAT scenarios passed
- [ ] No open high/critical defects
- [ ] Clinical sign-off obtained
- [ ] IT sign-off obtained
- [ ] Security sign-off obtained (HIPAA compliance)
- [ ] Support team trained and ready
- [ ] Rollback plan tested

**No-Go Indicators**:
- Open high/critical defects affecting patient safety
- Performance issues under production load
- Security vulnerabilities not remediated
- Clinical team not confident in results
- Support team not ready

**Escalation Path**: If No-Go, delay go-live by 1-2 weeks, convene emergency steering committee.

---

## Resource Allocation Guide

### Team Composition (Standard 12-Week Production Deployment)

| Role | Allocation | Weeks Active | Responsibilities |
|------|------------|--------------|------------------|
| **Project Manager** | 50% | 1-12 | Timeline, stakeholders, go/no-go decisions |
| **Integration Engineer** | 100% | 1-8 | FHIR connectivity, data flow, performance |
| **DevOps Engineer** | 50% | 1-4, 11-12 | Infrastructure, Docker/K8s, monitoring |
| **DBA** | 25% | 1-4 | Database setup, backups, migrations |
| **Clinical Analyst** | 50% | 5-10 | Measure configuration, validation, UAT |
| **QA Engineer** | 50% | 5-12 | Testing, defect tracking, UAT |
| **IT Lead** | 25% | 1-4 | Authentication, user provisioning, SSO |
| **Security Engineer** | 25% | 7-8 | HIPAA audit, security review |
| **Training Lead** | 25% | 9-10 | User training, documentation |
| **Support Team** | 25% | 11-12 | Hypercare, issue triage |

### Estimated Effort (Hours by Phase)

| Phase | Engineering | Clinical | IT/DevOps | Total |
|-------|-------------|----------|-----------|-------|
| **Phase 1: Foundation** | 40-60 | 0 | 20-40 | 60-100 |
| **Phase 2: Integration** | 60-80 | 40-60 | 10-20 | 110-160 |
| **Phase 3: Go-Live** | 20-40 | 20-40 | 30-50 | 70-130 |
| **Total** | **120-180** | **60-100** | **60-110** | **240-390** |

---

## Risk Mitigation

### Common Risks and Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **FHIR endpoint not ready** | High | Critical | Early engagement with EHR vendor (Week -4) |
| **OAuth2 credentials delayed** | Medium | High | Escalate to vendor relationship manager |
| **Data quality issues** | High | High | Early validation (Week 5), automated quality checks |
| **Performance below target** | Medium | Medium | Performance testing early (Week 7), scaling plan |
| **Clinical resistance to new workflow** | Medium | Medium | Champion identification, early UAT involvement |
| **Security audit findings** | Low | High | Engage security team early (Week 5), HIPAA checklist |
| **Key resource unavailable** | Medium | Medium | Cross-training, documented runbooks |
| **Scope creep** | Medium | Medium | Strict change control, steering committee approval |

### Contingency Timeline

If delays occur in Phase 1 or Phase 2, the following contingency options are available:

| Scenario | Contingency | Timeline Impact |
|----------|-------------|-----------------|
| **FHIR delay (1-2 weeks)** | Extend Phase 1, compress Phase 2 validation | +1-2 weeks |
| **Data quality issues** | Add data remediation sprint | +2 weeks |
| **Performance issues** | Infrastructure scaling, optimization sprint | +1-2 weeks |
| **Critical defects at UAT** | Extended UAT, bug fix sprint | +1-2 weeks |

---

## Success Metrics

### Implementation Success Criteria

| Metric | Target | Measurement |
|--------|--------|-------------|
| **On-time delivery** | Within 10% of planned timeline | Project schedule tracking |
| **Budget adherence** | Within 15% of planned budget | Financial tracking |
| **Data accuracy** | >98% match to source EHR | Data quality report |
| **Measure accuracy** | Within 5% of manual baseline | Validation comparison |
| **User adoption** | >80% of trained users active in Week 1 | Login analytics |
| **System availability** | >99% uptime in first 30 days | Monitoring dashboard |
| **Support tickets** | <20 tickets in first 30 days | Help desk metrics |

### Post-Go-Live KPIs (30-60-90 Day)

| KPI | 30 Day | 60 Day | 90 Day |
|-----|--------|--------|--------|
| **Patient evaluations completed** | 10,000 | 50,000 | 100,000 |
| **Care gaps identified** | 2,000 | 10,000 | 25,000 |
| **Care gaps closed** | 200 | 2,000 | 7,500 |
| **HEDIS reporting time reduction** | 30% | 50% | 67% |
| **User satisfaction (NPS)** | >30 | >40 | >50 |

---

## Support & Escalation

### HDIM Support Channels

| Tier | Response Time | Channel | Use For |
|------|---------------|---------|---------|
| **Tier 1: Documentation** | Self-service | docs.hdim.io | How-to questions, reference |
| **Tier 2: Community** | 24-48 hours | community.hdim.io | Best practices, peer advice |
| **Tier 3: Support** | 4-8 hours | support@hdim.io | Technical issues, bugs |
| **Tier 4: Escalation** | 1-2 hours | emergency@hdim.io | Production outages, blockers |

### Escalation Matrix

| Issue Severity | Response Time | Escalation Path |
|----------------|---------------|-----------------|
| **Critical** (production down) | 1 hour | Support -> Engineering Lead -> VP Engineering |
| **High** (major functionality blocked) | 4 hours | Support -> Engineering Lead |
| **Medium** (functionality degraded) | 8 hours | Support |
| **Low** (minor issue, workaround exists) | 24-48 hours | Support |

---

## Appendix: Deployment Path Details

### Pilot Path (6 Weeks)

**Scope**: 1 measure, 1 facility, 5,000 patients

**Week-by-Week**:
- Week 1-2: Infrastructure + FHIR connectivity
- Week 3-4: Single measure configuration + validation
- Week 5: UAT with small user group
- Week 6: Pilot go-live + evaluation

**Resources**: 1 Integration Engineer (100%), 1 Clinical Analyst (50%)

**Total Effort**: ~80-120 hours

---

### Production Path (12 Weeks)

**Scope**: Full HEDIS measure set, organization-wide, 50,000 patients

**Detailed in main roadmap above.**

**Resources**: Full team as described in Resource Allocation Guide

**Total Effort**: ~240-390 hours

---

### Enterprise Path (16 Weeks)

**Scope**: Multi-region, multi-EHR, 500,000+ patients

**Additional Weeks**:
- Week 13-14: Second EHR integration (Cerner, Athena, etc.)
- Week 15: Multi-region data synchronization
- Week 16: Federated reporting + go-live

**Additional Resources**: +1 Integration Engineer, +1 DBA, +1 Clinical Analyst

**Total Effort**: ~400-600 hours

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | January 2026 | HDIM Implementation Team | Initial version |

---

*For questions about this roadmap, contact your HDIM implementation manager or email implementations@hdim.io*
