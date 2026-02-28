# Phase Gate Review Template

**Customer:** `<customer-name>`  
**Phase:** `Phase 0|1|2|3|4`  
**Review Date:** `<yyyy-mm-dd>`  
**Decision:** `Go|Conditional Go|No-Go`

---

## 1. Gate Criteria Checklist

### Data Trust Gate
- [ ] Required field completeness >= 95%
- [ ] Data refresh SLA met
- [ ] Reconciliation spot checks passed

### Workflow Reliability Gate
- [ ] Core workflows execute without manual workaround
- [ ] Error/rework rate within threshold
- [ ] Runbooks validated by operations team

### Adoption Gate
- [ ] Role-based weekly active usage targets met
- [ ] Workflow completion SLA met
- [ ] Training completion targets met

### Outcome Gate
- [ ] KPI movement visible from baseline or intervention validated
- [ ] Leadership agrees outcome trend is directionally acceptable

### Compliance Gate
- [ ] Control evidence package complete for phase scope
- [ ] Access/audit controls validated
- [ ] Required approvals documented

---

## 2. Evidence Links

| Artifact | Link | Owner | Verified |
|---|---|---|---|
| KPI report | `<url/path>` | `<name>` | `Yes|No` |
| Test results | `<url/path>` | `<name>` | `Yes|No` |
| Data quality report | `<url/path>` | `<name>` | `Yes|No` |
| Compliance evidence | `<url/path>` | `<name>` | `Yes|No` |

---

## 3. Open Risks / Blockers

| Item | Severity | Owner | Mitigation | Target Date |
|---|---|---|---|---|
| `<risk or blocker>` | `Low|Medium|High|Critical` | `<name>` | `<action>` | `<date>` |
| `<risk or blocker>` | `Low|Medium|High|Critical` | `<name>` | `<action>` | `<date>` |

---

## 4. Decision Summary

- Decision rationale: `<summary>`
- Conditions (if conditional go): `<conditions>`
- Rollback trigger: `<trigger>`
- Next gate review date: `<date>`

---

## 5. Sign-Off

- Customer Executive Sponsor: `<name/date>`
- Customer CMO: `<name/date>`
- Customer CTO: `<name/date>`
- HDIM CS Lead: `<name/date>`
- HDIM Engineering Lead: `<name/date>`
- Compliance Lead: `<name/date>`

