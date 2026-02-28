# Customer UAT Test Pack Template

**Customer:** `<customer-name>`  
**UAT Window:** `<date-range>`  
**Environment:** `<env-name/url>`  
**Version/Release:** `<version>`  
**Approved Test Lead:** `<name>`

---

## 1. UAT Objectives

- Validate end-to-end clinical workflows for pilot scope.
- Confirm role-based access and expected data visibility.
- Verify operational reporting supports executive decisions.
- Confirm no critical defects block production use.

---

## 2. Test Scope

### In Scope
- Care gap triage and closure
- Evaluation submission and result review
- Patient panel insights and prioritization
- Executive dashboard visibility (CMO/CTO views)

### Out Of Scope
- `<explicitly out-of-scope flow>`
- `<explicitly out-of-scope flow>`

---

## 3. Entry And Exit Criteria

### Entry Criteria
- [ ] Test data seeded and validated
- [ ] User roles provisioned
- [ ] UAT environment stable
- [ ] Known defects reviewed

### Exit Criteria
- [ ] All critical test cases pass
- [ ] No open Sev1 defects
- [ ] Sev2 defects have approved mitigation/workaround
- [ ] Stakeholder sign-off complete

---

## 4. Test Cases

| TC ID | Workflow | Role | Preconditions | Steps | Expected Result | Priority | Status |
|---|---|---|---|---|---|---|---|
| UAT-001 | Login + MFA | Admin | Account active | Login and complete MFA | Successful access, audit event logged | High | `Not Run` |
| UAT-002 | Patient panel load | Clinical user | Patient data available | Open patient panel | Correct cohort and summary metrics | High | `Not Run` |
| UAT-003 | Care gap closure | Care manager | Open care gap exists | Complete intervention + close gap | Gap status updates and appears in reporting | High | `Not Run` |
| UAT-004 | Evaluation execution | Quality lead | Measure configured | Run evaluation for cohort | Results generated and traceable | High | `Not Run` |
| UAT-005 | Executive KPI view | CMO | KPI data loaded | Open executive dashboard | KPIs match latest validated data | High | `Not Run` |
| UAT-006 | Access boundary | Non-privileged user | Restricted role | Try admin/reporting route | Access denied with expected behavior | High | `Not Run` |
| UAT-007 | Audit traceability | Auditor | User activity exists | Review audit logs | Complete and searchable event history | Medium | `Not Run` |

---

## 5. Defect Log

| Defect ID | Severity | Summary | Repro Steps | Owner | Target Fix Date | Status |
|---|---|---|---|---|---|---|
| `<id>` | `Sev1|Sev2|Sev3` | `<summary>` | `<steps>` | `<name>` | `<date>` | `Open|Fixed|Deferred` |

---

## 6. Sign-Off

- Customer UAT Lead: `<name/date>`
- Customer Clinical Lead: `<name/date>`
- Customer IT Lead: `<name/date>`
- HDIM QA Lead: `<name/date>`
- HDIM Product Owner: `<name/date>`

