# ADR Template: Architecture Decision Record

**Status**: [Proposed | Accepted | Deprecated | Superseded by ADR-XXX]
**Date**: YYYY-MM-DD
**Decision Makers**: [Names/Roles of people involved]
**Stakeholders**: [List of affected teams]

---

## Context

### Problem Statement

Describe the specific problem or challenge that prompted this decision.

**Example:**
"HDIM services were duplicate authentication logic across patient-service, quality-measure-service, care-gap-service, and fhir-service. This duplication made it difficult to apply security fixes consistently and increased maintenance burden."

### Background

Provide relevant background information:
- What triggered this decision?
- What constraints exist?
- What have we tried before?
- What is the current state?

**Example:**
"January 2026 code review identified:
- 2,000+ lines of duplicate auth code
- 4 services with slightly different implementations
- 3 CVEs required patching in all 4 places
- New team onboarding slowed by multiple auth implementations"

### Assumptions

List key assumptions underlying this decision.

**Example:**
- All gateway services use JWT tokens
- Multi-tenant isolation enforced at header level (X-Tenant-ID)
- Rate limiting needed per tenant, not global
- Development teams can coordinate on shared module changes

---

## Options Considered

### Option 1: [Name of First Option]

**Description**: Brief description of approach

**Pros**:
- Pro 1
- Pro 2
- Pro 3

**Cons**:
- Con 1
- Con 2
- Con 3

**Estimated Effort**: X weeks
**Risk Level**: Low | Medium | High

---

### Option 2: [Name of Second Option]

**Description**: Brief description of approach

**Pros**:
- Pro 1
- Pro 2

**Cons**:
- Con 1
- Con 2

**Estimated Effort**: Y weeks
**Risk Level**: Low | Medium | High

---

### Option 3: [Name of Third Option]

**Description**: Brief description of approach

**Pros**:
- Pro 1
- Pro 2

**Cons**:
- Con 1
- Con 2

**Estimated Effort**: Z weeks
**Risk Level**: Low | Medium | High

---

## Decision

### Selected Option

**We chose Option [X] because...**

Explain why this option was selected over alternatives. Address:
- Why it best solves the problem
- How it aligns with HDIM principles
- Why it's better than alternatives

**Example:**
"We chose Option 1 (Create gateway-core shared module) because:
1. **Code Reuse**: Eliminates 2,000+ lines of duplication
2. **Consistency**: Single source of truth for auth logic
3. **Maintainability**: Security fixes applied in one place
4. **Team Autonomy**: Specialized gateways can still customize as needed
5. **Low Risk**: Shared module contains proven patterns"

### Rationale

Explain the reasoning:
- Business justification
- Technical justification
- Alignment with architecture principles
- Impact on team structure/processes

---

## Consequences

### Positive

**Short-term (1-2 months)**:
- Immediate benefit 1
- Immediate benefit 2

**Long-term (3-12 months)**:
- Lasting benefit 1
- Lasting benefit 2

**Example:**
- Short-term: Reduced code duplication, faster security patches
- Long-term: Easier to add new gateway services, consistent auth across platform

### Negative

**Short-term**:
- Challenge or tradeoff 1
- Challenge or tradeoff 2

**Long-term**:
- Potential issue 1
- Potential issue 2

**Example:**
- Short-term: Module changes require coordination, shared dependencies
- Long-term: Core module could become bottleneck if overloaded

### Neutral

**Changes in how we work**:
- Process change 1
- Process change 2

**Example:**
- Gateway services must update together when core module changes
- Code reviews should include core module maintainers

---

## Implementation

### Affected Components

List services, modules, or systems that will change:
- Component 1: Change type
- Component 2: Change type
- Component 3: Change type

**Example:**
- gateway-service: Add dependency on gateway-core
- gateway-admin-service: Create new service using gateway-core
- gateway-clinical-service: Create new service using gateway-core
- gateway-fhir-service: Create new service using gateway-core

### Timeline

| Phase | Milestone | Duration | Owner |
|-------|-----------|----------|-------|
| Phase 1 | Extract auth code to gateway-core | 1 week | Platform Team |
| Phase 2 | Update gateway-service | 3 days | Team 1 |
| Phase 3 | Create gateway-admin-service | 4 days | Team 1 |
| Phase 4 | Create gateway-clinical-service | 4 days | Team 2 |
| Phase 5 | Create gateway-fhir-service | 4 days | Team 2 |
| Phase 6 | Integration testing | 1 week | QA |
| Phase 7 | Deployment | 2 days | DevOps |

### Success Criteria

How will we measure success?

- [ ] All 4 gateways use gateway-core
- [ ] 0 duplicate auth code across services
- [ ] Security patches applied in <1 day (vs 3 days before)
- [ ] Tests pass for all affected services
- [ ] Performance metrics unchanged or improved
- [ ] Team satisfaction with new architecture

### Rollback Plan

If this decision needs to be reversed:

1. **Condition for rollback**: [When would we reconsider?]
   - Example: "If performance regression > 10% in gateway latency"

2. **Steps to rollback**:
   - Example: "Remove gateway-core dependency, restore duplicate code"

3. **Effort estimate**: X weeks

---

## Monitoring & Validation

### Metrics to Track

Define how we'll measure success:

| Metric | Baseline | Target | Cadence |
|--------|----------|--------|---------|
| Code duplication (lines) | 2000+ | <100 | Quarterly |
| Auth implementation time (new gateway) | 3 weeks | 1 week | Per service |
| Security patch rollout time | 3 days | <1 day | Per incident |
| Auth code test coverage | 60% | 90%+ | Quarterly |
| Gateway latency (p99) | 150ms | <150ms | Continuous |

### Review Schedule

- **1-month review**: Did implementation go as planned?
- **3-month review**: Are metrics showing expected improvements?
- **6-month review**: Any unexpected consequences?
- **Annual review**: Is decision still valid?

---

## Related Decisions

### Prior Decisions

- [ADR-XXX](ADR-XXX.md): [How prior decision influenced this one]

**Example:**
- ADR-002: Gateway-trust authentication pattern (informed our approach)
- ADR-010: Kafka for event streaming (doesn't directly impact this)

### Related Decisions

- [ADR-YYY](ADR-YYY.md): [How this decision influences another]

**Example:**
- ADR-XXX: New specialized gateways (depends on this core module)

### Future Decisions Enabled

- Security policy enforcement options expanded
- New domain-specific gateways possible
- API versioning strategies improved

---

## Examples & Precedents

### Similar Decisions in Industry

- **Spring Cloud Config Server**: Centralized config across services (similar pattern)
- **Netflix API Gateway**: Domain-specific gateways with shared core (similar approach)

### Within HDIM

- **Event Sourcing (ADR-001)**: Shared event-sourcing module (pattern to follow)
- **Liquibase migrations**: Standardized across all services (similar modularity)

---

## Questions & Open Items

### Resolved Questions

**Q: Will this slow down development?**
A: No, it will speed it up. Shared module changes are coordinated in ~1 day, versus patching 4 services in 3 days.

**Q: What if gateway-core becomes a bottleneck?**
A: We can split into more focused modules (auth, routing, rate-limiting separately) if needed.

### Open Questions

- [ ] How do we handle gateway-core version upgrades across 4 services?
- [ ] Who owns gateway-core maintenance long-term?
- [ ] How do we test interactions between core and specialized services?

---

## Approvals

### Decision Makers

| Role | Name | Date | Status |
|------|------|------|--------|
| Architecture Lead | [Name] | YYYY-MM-DD | ✅ Approved |
| Tech Lead (Backend) | [Name] | YYYY-MM-DD | ✅ Approved |
| Engineering Manager | [Name] | YYYY-MM-DD | ✅ Approved |
| Security (if applicable) | [Name] | YYYY-MM-DD | ✅ Approved |

### Stakeholder Feedback

- **Gateway Team**: Concerns about shared module changes → Addressed in implementation plan
- **Quality Measure Team**: No concerns, supports consolidation
- **DevOps**: Supports modular approach

---

## Changelog

| Date | Author | Change |
|------|--------|--------|
| 2026-01-10 | [Name] | Created ADR, proposed extraction of gateway-core |
| 2026-01-12 | [Name] | Updated with feedback from security review |
| 2026-01-15 | [Name] | Marked as Accepted after team approval |

---

## References

### Documentation Links

- [CLAUDE.md](../../CLAUDE.md) - Project overview and standards
- [Gateway Architecture Guide](GATEWAY_ARCHITECTURE.md) - Related documentation
- [System Architecture](SYSTEM_ARCHITECTURE.md) - Complete platform overview

### External References

- [Spring Framework Security Best Practices](https://spring.io/projects/spring-security)
- [Microservices Patterns by Chris Richardson](https://microservices.io/)
- [API Gateway Pattern](https://microservices.io/patterns/apigateway.html)

### Issue Tracker

- GitHub Issues: [Link to related issues]
- Jira: [Link to epic/story]

---

## Footer

**ADR #**: [Will be assigned during review]
**Version**: 1.0
**Last Updated**: YYYY-MM-DD
**Supersedes**: [If applicable]
**Superseded By**: [If applicable]

---

## How to Use This Template

1. **Copy this template** and save as `ADR-NNN-descriptive-name.md` in `docs/architecture/decisions/`
2. **Fill in each section** with decision-specific details
3. **Be concise** - Keep ADRs focused and readable
4. **Include examples** - Help readers understand the decision
5. **Get approval** - Have architecture team review before marking as Accepted
6. **Update related docs** - Link to this ADR from affected architecture guides
7. **Archive superseded ADRs** - Keep history but clearly mark as superseded

---

_ADR Template Version: 1.0_
_Created: January 19, 2026_
_For questions, see: [Architecture Review Process](ARCHITECTURE_REVIEW_PROCESS.md)_
