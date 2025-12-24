# Change Management Policy

**HDIM - HealthData-in-Motion**

| Version | Date | Author | Status |
|---------|------|--------|--------|
| 1.0 | December 2025 | Engineering Team | Active |

---

## 1. Purpose

This policy establishes procedures for managing changes to HDIM production systems, ensuring changes are properly authorized, tested, and implemented with minimal risk.

---

## 2. Scope

This policy applies to:
- All production systems and infrastructure
- Application code deployments
- Configuration changes
- Database changes
- Network changes
- Security control changes

---

## 3. Change Categories

### 3.1 Standard Changes

Pre-approved, low-risk, routine changes.

| Examples | Approval | Testing |
|----------|----------|---------|
| Security patches | Pre-approved | Verified |
| Minor config updates | Pre-approved | Verified |
| User provisioning | Pre-approved | N/A |

### 3.2 Normal Changes

Planned changes requiring review and approval.

| Examples | Approval | Testing |
|----------|----------|---------|
| Feature releases | CAB approval | Full test |
| Infrastructure changes | CAB approval | Full test |
| Major upgrades | CAB approval | Full test |

### 3.3 Emergency Changes

Urgent changes to restore service or address security.

| Examples | Approval | Testing |
|----------|----------|---------|
| Critical security fixes | Manager approval | Expedited |
| Service restoration | On-call approval | Post-change |
| Incident response | Incident Commander | As possible |

---

## 4. Change Advisory Board (CAB)

### 4.1 Composition

| Role | Responsibility |
|------|----------------|
| Engineering Lead | Technical review |
| Security Lead | Security review |
| Operations Lead | Operational impact |
| Product Lead | Business impact |

### 4.2 Meeting Schedule

- Weekly CAB meeting for normal changes
- Emergency CAB convened as needed
- Async approval for standard changes

---

## 5. Change Request Process

### 5.1 Request Submission

All changes require:
- [ ] Change description
- [ ] Business justification
- [ ] Risk assessment
- [ ] Test plan
- [ ] Rollback plan
- [ ] Implementation schedule
- [ ] Communication plan

### 5.2 Review Criteria

| Criteria | Requirement |
|----------|-------------|
| Testing | Passed in staging |
| Security | Security review complete |
| Documentation | Updated as needed |
| Rollback | Plan verified |
| Communication | Stakeholders notified |

### 5.3 Approval Requirements

| Change Type | Approvers |
|-------------|-----------|
| Standard | Pre-approved |
| Normal | CAB (2+ members) |
| Emergency | Manager + Post-CAB review |

---

## 6. Implementation Process

### 6.1 Pre-Implementation

- [ ] Verify approvals obtained
- [ ] Confirm maintenance window
- [ ] Notify stakeholders
- [ ] Prepare rollback
- [ ] Backup current state

### 6.2 Implementation

- [ ] Follow documented steps
- [ ] Log all actions
- [ ] Monitor for issues
- [ ] Verify success criteria
- [ ] Document completion

### 6.3 Post-Implementation

- [ ] Verify functionality
- [ ] Update documentation
- [ ] Close change ticket
- [ ] Communicate completion
- [ ] Schedule post-change review

---

## 7. Code Deployment

### 7.1 Deployment Pipeline

```
Developer → Pull Request → Code Review → CI Tests →
Staging Deploy → QA → CAB Approval → Production Deploy
```

### 7.2 Requirements

| Stage | Requirement |
|-------|-------------|
| Pull Request | Minimum 1 reviewer |
| CI Tests | All tests pass |
| Code Review | Security review for sensitive changes |
| Staging | 24-hour bake time |
| Production | Approved change ticket |

### 7.3 Deployment Windows

| Type | Window |
|------|--------|
| Standard | Tuesday-Thursday, 10am-4pm |
| Critical | As needed with approval |
| Excluded | Fridays, weekends, holidays |

---

## 8. Rollback Procedures

### 8.1 Rollback Triggers

- Failed health checks
- Error rate increase >5%
- Response time increase >50%
- Customer-reported issues
- Security vulnerability discovered

### 8.2 Rollback Process

1. Declare rollback decision
2. Notify stakeholders
3. Execute rollback steps
4. Verify restoration
5. Document issues
6. Schedule post-mortem

---

## 9. Emergency Changes

### 9.1 Criteria

Emergency changes are only for:
- Service restoration
- Security incident response
- Critical vulnerability remediation
- Regulatory compliance

### 9.2 Process

1. Manager verbal approval
2. Implement change
3. Document change within 24 hours
4. Post-change CAB review
5. Update procedures if needed

---

## 10. Documentation

### 10.1 Required Records

- Change request form
- Approval records
- Implementation logs
- Test results
- Rollback procedures
- Post-change review

### 10.2 Retention

- Change records: 3 years
- Audit logs: 7 years

---

## 11. Metrics

| Metric | Target |
|--------|--------|
| Change success rate | >95% |
| Emergency change rate | <10% |
| Mean time to implement | <2 hours |
| Changes with rollback | <5% |

---

## Appendix: Change Request Template

```
CHANGE REQUEST

Change ID: CHG-YYYY-NNNN
Requestor:
Date:

CHANGE DETAILS
Title:
Description:
Category: [ ] Standard  [ ] Normal  [ ] Emergency
Systems Affected:
Risk Level: [ ] Low  [ ] Medium  [ ] High

JUSTIFICATION
Business Need:
Impact of Not Making Change:

IMPLEMENTATION
Proposed Date/Time:
Duration:
Steps:
1.
2.
3.

TESTING
Test Plan:
Test Results:

ROLLBACK
Rollback Steps:
1.
2.
3.
Rollback Time:

APPROVALS
Requestor: _____________ Date: _______
Reviewer:  _____________ Date: _______
CAB:       _____________ Date: _______
```

---

*Document Classification: Internal*
*Next Review Date: December 2026*
