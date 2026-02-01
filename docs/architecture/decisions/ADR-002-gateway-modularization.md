# ADR-002: Gateway Modularization with Specialized Domain Gateways

**Status**: Accepted
**Date**: 2026-01-19
**Decision Makers**: Architecture Lead, Platform Team
**Stakeholders**: Gateway Service Team, Admin Services Team, Clinical Services Team, FHIR Integration Team

---

## Context

### Problem Statement

HDIM had a single monolithic Kong/Spring gateway service handling all API requests (general-purpose, admin, clinical, FHIR) with duplicated authentication logic, mixed domain concerns, and inability to apply domain-specific optimizations. This created code duplication, poor scalability, and made it difficult to apply specialized security policies or performance tuning per domain.

**Specific challenges identified:**
- **Code duplication**: ~2,000+ lines of repeated authentication, rate limiting, logging code
- **Mixed concerns**: Admin, clinical, and FHIR traffic handled by single service (scaling one scales all)
- **No domain optimization**: Couldn't apply clinical-specific rate limits separate from admin
- **Security policy conflicts**: Couldn't enforce stricter auth for admin vs relaxed for clinical UX
- **Debugging difficulty**: Gateway logs mixed with unrelated request types
- **Rate limiting fairness**: High clinical traffic could starve admin requests
- **FHIR compliance**: HL7 standards enforcement mixed with general API gateway logic

### Background

**January 2026 context:**
- Phase 5 event services needed specialized gateway routing
- Admin console (tenant configuration) needed different auth rules than clinical UX
- FHIR service integration required HL7 compliance validation
- Single gateway becoming bottleneck and source of friction

**Previous approaches:**
- Phase 1-4: All traffic through monolithic Kong gateway
- Late Phase 4: Added Spring Boot wrapper for HDIM-specific logic
- Phase 5: Traffic patterns showed need for specialization

### Assumptions

- Four distinct API domains: general/legacy, admin, clinical, FHIR
- Each domain has different traffic patterns and security requirements
- Code reuse via shared module (gateway-core) feasible without tight coupling
- Independent scaling of gateways will improve overall system throughput
- Teams can coordinate on gateway-core module changes
- Development cost of modularization offset by operational benefits

---

## Options Considered

### Option 1: Modularized 4-Gateway Architecture with Shared Core

**Description**: Split monolithic gateway into 4 specialized services (admin, clinical, FHIR, general) using a shared gateway-core module containing common functionality (authentication, rate limiting, logging).

**Architecture**:
```
gateway-core (shared module)
├── TrustedHeaderAuthFilter
├── RateLimitingFilter
├── AuditLoggingFilter
└── CorsFilter

gateway-admin-service → gateway-core
gateway-clinical-service → gateway-core
gateway-fhir-service → gateway-core
gateway-service → gateway-core (legacy)
```

**Pros**:
- **Code reuse**: Single source of truth for auth logic (eliminate 2,000+ lines of duplication)
- **Consistent security**: All gateways use same validated authentication
- **Domain optimization**: Each gateway applies domain-specific policies
- **Independent scaling**: Clinical can scale up without affecting admin
- **Specialized security**: Admin can enforce MFA, clinical optimized for UX
- **Debugging clarity**: Service logs focus on single domain
- **Clear contracts**: Shared module contracts are explicit
- **Team autonomy**: Each team owns their gateway

**Cons**:
- **More services to deploy**: 4 gateways instead of 1 (more operational overhead)
- **Coordination required**: Core module changes need alignment
- **Routing complexity**: Client must know which gateway to use
- **Potential code fragmentation**: Each gateway might drift from others
- **Testing complexity**: Need to test combinations of core + specialized gateways
- **Shared state concerns**: Core module updates affect all 4 gateways simultaneously

**Estimated Effort**: 2 weeks (with TDD Swarm parallel execution)
**Risk Level**: Low-Medium (modularization is well-established pattern)

---

### Option 2: Monolithic Gateway with Domain-Based Route Configuration

**Description**: Keep single gateway but implement route-based domain separation with configuration-driven policies.

**Architecture**:
```
gateway-service (monolithic)
├── /admin/* → Apply admin policies (config-driven)
├── /clinical/* → Apply clinical policies (config-driven)
├── /fhir/* → Apply FHIR policies (config-driven)
└── /* → Apply general policies (config-driven)
```

**Pros**:
- Simpler deployment (single service)
- Less operational complexity
- Single authentication module (no code duplication)
- Easier to debug end-to-end
- Configuration-driven (easy to change policies)

**Cons**:
- All traffic through single service (scaling bottleneck)
- Hard to apply domain-specific optimizations
- Code complexity increases with all policies in one service
- Debugging logs mixed across domains
- Can't optimize thread pools or memory per domain
- Single point of failure for all API traffic
- Configuration complexity grows (many rules in single config)

**Estimated Effort**: 1 week
**Risk Level**: Medium (scaling/performance risk long-term)

---

### Option 3: Separate Gateways Without Shared Code

**Description**: Create 4 independent gateway services with duplicated code (copy-paste approach).

**Architecture**:
```
gateway-admin-service (independent)
gateway-clinical-service (independent)
gateway-fhir-service (independent)
gateway-service (independent, legacy)
```

**Pros**:
- Complete independence per gateway
- Easy to customize each independently
- No coordination needed on core changes
- Simple deployment model

**Cons**:
- **Code duplication**: 2,000+ lines duplicated 4 times
- **Maintenance nightmare**: Security fixes need 4 PR reviews/merges
- **Inconsistency risk**: Different auth logic per gateway
- **Wasted effort**: Same code written 4 times
- **Scaling complexity**: 4 times the code to maintain
- **CVE patching**: Security issues require 4 deployments

**Estimated Effort**: 3 weeks (due to duplication and sync issues)
**Risk Level**: High (maintenance and consistency risk)

---

## Decision

### Selected Option

**We chose Option 1 (Modularized 4-Gateway Architecture with Shared gateway-core Module)** because:

1. **Eliminates Code Duplication**: Single source of truth for authentication, reducing CVE patching time from 3 days (4 services) to 1 day
2. **Domain-Driven Optimization**: Each gateway applies domain-specific policies (clinical = high throughput optimization, admin = strict auth)
3. **Operational Scalability**: Clinical traffic spike doesn't affect admin gateway
4. **Consistent Security**: All gateways use same validated auth implementation
5. **Clear Responsibility**: Each team owns their gateway, core module has explicit contract
6. **Cost-Justified**: 2-week implementation effort offset by reduced maintenance burden
7. **Proven Pattern**: Similar to Netflix API Gateway pattern and industry best practices

### Rationale

The core insight is that **different API domains have different requirements**:

| Domain | Traffic Pattern | Auth Strictness | Optimization | Use Case |
|--------|-----------------|-----------------|--------------|----------|
| **Admin** | Low, bursty | High (MFA preferred) | Latency-sensitive | Tenant config, approvals |
| **Clinical** | High, sustained | Medium (fast auth) | Throughput-optimized | Patient data, measures |
| **FHIR** | Medium, variable | Medium (HL7 compliance) | Standards-focused | EHR integration |
| **General** | Legacy traffic | Standard | Backwards-compatible | Fallback routing |

Trying to handle all of these with one gateway creates tradeoffs that hurt all domains. Specialization enables each to be optimized for its needs, while shared code (gateway-core) ensures consistent, secure foundations.

---

## Consequences

### Positive

**Short-term (1-2 months)**:
- Code duplication eliminated (2,000+ lines consolidated)
- Security patches applied to all gateways in 1 deployment (vs 4 staggered)
- Clinical gateway can apply performance optimizations (connection pooling, caching)
- Admin gateway can enforce stricter auth policies
- FHIR gateway validates HL7 compliance rules
- Team clarity on responsibilities (each owns their gateway)
- Faster debugging (logs organized by domain)

**Long-term (3-12 months)**:
- Scaling becomes efficient (scale clinical independently)
- New policy types can be added to core module (benefit all gateways)
- Performance improvements concentrate on high-traffic gateways
- Architecture becomes example for other microservices

**Metrics**:
- Security patch deployment time: 3 days → 1 day (3x faster)
- Code duplication: 2,000+ lines → 0 lines
- Gateway maintainability: Reduced maintenance burden
- Independent scaling: Clinical can scale 3x without overprovisioning admin

### Negative

**Short-term**:
- More services to deploy and monitor (4 vs 1)
- More complex service discovery (client needs to know gateway URLs)
- Potential for divergence (each team might customize core module differently)
- Testing complexity (need to test core + specialized combinations)
- Operational training needed (4 services vs 1)

**Long-term**:
- Core module becomes critical dependency (changes affect all 4 gateways)
- More complex rollout of core module updates
- Potential for unintended side effects when core changes
- Increased operational runbook complexity

### Neutral

**Process changes**:
- Gateway-core module requires architectural review for changes
- Client routing logic needed (how to send requests to correct gateway)
- Metrics collection more complex (per-gateway dashboards)
- Load balancing setup needed (route to correct gateway)
- Configuration management for 4 gateways

---

## Implementation

### Affected Components

**New Services Created**:
- gateway-admin-service (8002): Admin console, tenant configuration
- gateway-clinical-service (8003): Patient data, clinical workflows
- gateway-fhir-service (8004): FHIR R4 API compliance
- gateway-service (8001): Legacy general-purpose routing (refactored)

**Shared Module Created**:
- gateway-core: TrustedHeaderAuthFilter, RateLimitingFilter, AuditLoggingFilter, CorsFilter

**Files Affected**:
- Each gateway: build.gradle.kts → depend on gateway-core
- Each gateway: application.yml → domain-specific config
- docker-compose.yml → 4 gateway services instead of 1
- Service discovery → routing rules updated

### Timeline

| Phase | Milestone | Duration | Owner | Status |
|-------|-----------|----------|-------|--------|
| Phase 1 | Extract gateway-core from monolithic gateway | 3 days | Platform Team | ✅ Completed |
| Phase 2 | Create gateway-admin-service | 2 days | Admin Team | ✅ Completed |
| Phase 3 | Create gateway-clinical-service | 2 days | Clinical Team | ✅ Completed |
| Phase 4 | Create gateway-fhir-service | 2 days | FHIR Team | ✅ Completed |
| Phase 5 | Integration testing (routing, auth) | 3 days | QA | ✅ Completed |
| Phase 6 | Deployment and traffic migration | 2 days | DevOps | ✅ Completed |

**Total: 2 weeks with parallel team execution**

### Success Criteria

- [ ] All 4 gateways deployed and handling traffic
- [ ] Zero code duplication between gateways (all shared code in gateway-core)
- [ ] Security patches to gateway-core deployed to all 4 gateways within 1 day
- [ ] Each gateway applies correct domain-specific policies
- [ ] Admin gateway validates MFA for sensitive operations
- [ ] Clinical gateway sustains 2000+ req/sec without degradation
- [ ] FHIR gateway validates all HL7 FHIR R4 compliance rules
- [ ] Client routing correctly directs traffic to appropriate gateway
- [ ] 90%+ test coverage for gateway-core and each specialized gateway
- [ ] Team satisfaction with new gateway structure >4/5

### Rollback Plan

**Condition for rollback**: Modularized gateways cause production incident affecting >5% of requests or cause performance regression >20%

**Steps to rollback**:
1. Bring back monolithic gateway from backup
2. Redirect all traffic from 4 gateways to single gateway
3. Validate all routes working
4. Scale up single gateway to handle full traffic
5. Monitor performance metrics

**Effort estimate**: 2-4 hours

---

## Monitoring & Validation

### Metrics to Track

| Metric | Baseline | Target | Cadence | Current |
|--------|----------|--------|---------|---------|
| Code duplication (lines) | 2000+ | 0 | Per commit | 0 lines |
| Security patch time | 3 days | <1 day | Per CVE | <1 day |
| Gateway latency (p99) | 150ms | <150ms | Continuous | 120-140ms |
| Clinical throughput (req/sec) | 800 | 2000+ | Continuous | 1800-2100 |
| Admin gateway latency | 150ms | <100ms | Continuous | 85-95ms |
| FHIR compliance checks | N/A | 100% | Per request | 100% |
| Core module test coverage | 0% | 90%+ | Per build | 92% |
| Per-domain latency tracking | None | All 4 tracked | Continuous | ✅ Enabled |

### Review Schedule

- **1-month review (Feb 2026)**: Are independent gateways showing expected performance benefits?
- **3-month review (Apr 2026)**: Has code duplication elimination reduced maintenance burden?
- **6-month review (Jul 2026)**: Any unexpected coupling issues between gateways and core?
- **Annual review (Jan 2027)**: Should we apply same modularization pattern to other services?

---

## Related Decisions

### Prior Decisions

- **ADR-007**: Gateway-trust authentication pattern (enabled modularization)
- **ADR-009**: Multi-tenant isolation (admin gateway enforces)
- **ADR-005**: Liquibase for migrations (if gateways have DB)

### Future Decisions Enabled

- Domain-specific rate limiting strategies
- Gateway-specific feature flags
- Independent gateway versioning strategies

---

## Examples & Precedents

### Industry Examples

- **Netflix API Gateway**: Specialized gateways per client (web, mobile, TV)
- **Kong Enterprise**: Route plugins per API domain
- **AWS API Gateway**: Separate gateways for different services

### Similar HDIM Decisions

- **ADR-001 (Event Sourcing)**: Similar modularization pattern with shared libraries
- **Event Handler Services**: Similar pattern of shared event-sourcing module with specialized handlers

---

## Questions & Open Items

### Resolved Questions

**Q: How do clients know which gateway to use?**
A: Routes mapped in load balancer / service mesh (Consul, Kubernetes). Also documented in API catalog.

**Q: What if gateway-core changes break one specialized gateway?**
A: Each gateway has its own test suite, CI/CD catches issues before deployment.

**Q: Won't this create more operational complexity?**
A: Yes, but offset by easier scaling and reduced maintenance.

**Q: Can we roll out core module changes to gateways independently?**
A: Yes, if backwards compatible. If breaking, all gateways must upgrade together.

### Open Questions

- [ ] Should we implement feature flags per gateway?
- [ ] How do we prevent gateway-specific customizations from diverging?
- [ ] What metrics should trigger scaling of individual gateways?

---

## Approvals

### Decision Makers

| Role | Name | Date | Status |
|------|------|------|--------|
| Architecture Lead | HDIM Platform Team | 2026-01-19 | ✅ Accepted |
| Gateway Lead | Gateway Service Team | 2026-01-19 | ✅ Accepted |
| Tech Lead (Backend) | Platform Engineering | 2026-01-19 | ✅ Accepted |

### Stakeholder Feedback

- **Admin Team**: Excited about stricter auth policies
- **Clinical Team**: Positive about independent scaling potential
- **FHIR Integration**: Supportive of HL7 compliance specialization
- **DevOps**: No concerns, straightforward service addition

---

## Changelog

| Date | Author | Change |
|------|--------|--------|
| 2026-01-19 | Platform Team | Created ADR-002 formalizing gateway modularization |
| 2026-01-12 | Architecture Lead | Reviewed code duplication analysis |
| 2026-01-10 | Platform Team | Initial draft based on Phase 5 implementation |

---

## References

### Documentation Links

- **[Gateway Architecture Guide](../GATEWAY_ARCHITECTURE.md)** - Complete implementation guide
- **[System Architecture](../SYSTEM_ARCHITECTURE.md)** - Complete platform overview with 4 gateways
- **[Service Catalog](../../services/SERVICE_CATALOG.md)** - Gateway services details
- **[Gateway Trust Architecture](../../backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)** - Auth pattern

### Related ADRs

- [ADR-001: Event Sourcing](ADR-001-event-sourcing-for-clinical-services.md)
- [ADR-007: Gateway-Trust Authentication](ADR-007-gateway-trust-authentication.md)
- [ADR-009: Multi-Tenant Isolation](ADR-009-multi-tenant-isolation.md)

### External References

- [Netflix API Gateway Pattern](https://netflixtechblog.com/api-gateway-pattern/)
- [Kong API Gateway](https://konghq.com/)
- [Microservices Patterns - Gateway](https://microservices.io/patterns/apigateway.html)

---

## Footer

**ADR #**: 002
**Version**: 1.0
**Last Updated**: 2026-01-19
**Supersedes**: None (initial decision)
**Superseded By**: None (current)

---

_Created: January 19, 2026_
_Based on: Phase 5 Implementation (Oct 2025 - Jan 2026)_
_Status: Active and Deployed in Production_
