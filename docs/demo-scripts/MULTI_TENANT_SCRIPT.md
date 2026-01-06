# Multi-Tenant Administration - Demo Script

**Scenario**: Secure SaaS Platform for Multiple Organizations
**Duration**: 2-3 minutes
**Target Audience**: IT Directors, Security Officers, Procurement
**Value Proposition**: Enterprise-grade security, complete data isolation, scalable architecture

---

## Pre-Recording Setup

### System State
- [ ] Demo environment running
- [ ] Logged in as: `demo_superadmin@hdim.ai`
- [ ] Demo mode enabled
- [ ] Scenario loaded: `./demo-cli load-scenario multi-tenant`
- [ ] Three demo tenants configured:
  - Acme Health Plan (5,000 patients)
  - Metro Medical Group (2,500 patients)
  - Regional ACO (8,000 patients)

### Browser Setup
- [ ] URL: `http://localhost:4200/admin?demo=true`
- [ ] Resolution: 1920x1080

---

## Narration Script

### INTRO (0:00 - 0:20)

**[Screen: Admin Dashboard]**

> "As a SaaS platform, HDIM serves multiple healthcare organizations on shared infrastructure while maintaining complete data isolation and security. Let me show you how."

---

## Step-by-Step Actions

### STEP 1: Tenant Overview (0:20 - 0:50)

**Action**: Navigate to Admin > Tenants

**Narration**:
> "From the admin console, we manage multiple tenants. Each organization - health plans, ACOs, medical groups - operates in a completely isolated environment.
>
> They share infrastructure for efficiency but can never see each other's data."

**Screen Elements**:
```
┌─────────────────────────────────────────────────────────────┐
│ TENANT MANAGEMENT                                           │
├─────────────────────────────────────────────────────────────┤
│ Tenant               Patients    Users    Status   Health  │
│ ─────────────────────────────────────────────────────────── │
│ Acme Health Plan     5,000       24       Active   ● 100%  │
│ Metro Medical Group  2,500       12       Active   ● 100%  │
│ Regional ACO         8,000       45       Active   ● 99.9% │
│                                                             │
│ [+ Add Tenant]                                              │
└─────────────────────────────────────────────────────────────┘
```

---

### STEP 2: Data Isolation Demo (0:50 - 1:30)

**Action**: Switch between tenants

**Narration**:
> "Watch what happens when I switch context to Acme Health Plan. The system automatically filters all data - patients, measures, results - to only show Acme's data.
>
> Now switching to Metro Medical Group - completely different data, different users, different configurations. No data leakage is possible."

**Key Points**:
- Demonstrate tenant switching
- Show different patient counts
- Show different user lists
- Emphasize database-level isolation

---

### STEP 3: Role-Based Access (1:30 - 2:00)

**Action**: View User Management

**Narration**:
> "Within each tenant, you have fine-grained role-based access control. Administrators manage users, Evaluators run quality measures, Analysts view reports, and Viewers have read-only access.
>
> Each user can belong to multiple tenants with different roles in each."

**Screen Elements**:
```
┌─────────────────────────────────────────────────────────────┐
│ USERS - Acme Health Plan                                    │
├─────────────────────────────────────────────────────────────┤
│ User                 Role           Last Active   Status   │
│ ─────────────────────────────────────────────────────────── │
│ john.admin@acme.com  ADMIN          Today         Active   │
│ sarah.eval@acme.com  EVALUATOR      Today         Active   │
│ mike.analyst@acme.com ANALYST       Yesterday     Active   │
│ view.only@acme.com   VIEWER         3 days ago    Active   │
└─────────────────────────────────────────────────────────────┘

Roles:
├── ADMIN: Full tenant management, user provisioning
├── EVALUATOR: Run evaluations, close care gaps
├── ANALYST: View reports, export data
└── VIEWER: Read-only dashboard access
```

---

### STEP 4: Security & Compliance (2:00 - 2:30)

**Action**: View Security Dashboard

**Narration**:
> "For healthcare, security is non-negotiable. HDIM is HIPAA compliant, SOC 2 Type II certified, and supports HITRUST controls.
>
> All PHI is encrypted at rest and in transit. We maintain complete audit logs of every access event. You can see the audit trail right here."

**Key Security Features**:
- End-to-end encryption (AES-256)
- Complete audit logging
- Session management
- MFA support
- IP allowlisting available

---

### CLOSING (2:30 - 3:00)

**Narration**:
> "HDIM provides enterprise-grade multi-tenancy with complete data isolation, role-based access control, and healthcare compliance built in.
>
> Whether you're managing one organization or dozens, you get the security and scalability of a true SaaS platform."

---

## Tenant Configuration

### Demo Tenants

| Tenant | ID | Patients | Users | Subscription |
|--------|-----|----------|-------|--------------|
| Acme Health Plan | ACME001 | 5,000 | 24 | Enterprise |
| Metro Medical Group | METRO001 | 2,500 | 12 | Professional |
| Regional ACO | RACO001 | 8,000 | 45 | Enterprise |

### User Roles

| Role | Permissions |
|------|-------------|
| SUPER_ADMIN | Full platform access, tenant management |
| ADMIN | Tenant settings, user management |
| EVALUATOR | Run evaluations, manage care gaps |
| ANALYST | View reports, analytics, exports |
| VIEWER | Read-only access |

---

## Security Compliance

### Certifications
- [ ] HIPAA Compliant
- [ ] SOC 2 Type II
- [ ] HITRUST CSF
- [ ] 21 CFR Part 11 (where applicable)

### Security Controls
- AES-256 encryption at rest
- TLS 1.3 in transit
- JWT tokens with refresh rotation
- Complete audit logging (HIPAA)
- PHI cache TTL ≤ 5 minutes

---

## Performance Metrics

| Action | Target Time |
|--------|-------------|
| Tenant switch | < 1s |
| User list load | < 500ms |
| Audit log query | < 2s |

---

**Last Updated**: January 2026
