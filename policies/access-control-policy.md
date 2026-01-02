# Access Control Policy

**HDIM - HealthData-in-Motion**

| Version | Date | Author | Status |
|---------|------|--------|--------|
| 1.0 | December 2025 | Security Team | Active |

---

## 1. Purpose

This policy establishes requirements for controlling access to HDIM information systems and data, ensuring that only authorized users can access resources appropriate to their roles.

---

## 2. Scope

This policy applies to:
- All HDIM information systems and applications
- All users including employees, contractors, and third parties
- All data including customer data and PHI

---

## 3. Access Control Principles

### 3.1 Least Privilege
Users receive the minimum access necessary to perform their job functions.

### 3.2 Need-to-Know
Access is granted only when there is a legitimate business requirement.

### 3.3 Separation of Duties
Critical functions are divided among multiple individuals to prevent fraud.

### 3.4 Defense in Depth
Multiple layers of access controls protect sensitive resources.

---

## 4. User Account Management

### 4.1 Account Provisioning

| Step | Requirement |
|------|-------------|
| Request | Manager submits access request with business justification |
| Approval | IT Security reviews and approves based on role |
| Creation | Account created with role-appropriate permissions |
| Notification | User receives credentials via secure channel |
| Training | User completes security awareness training |

### 4.2 Account Types

| Type | Description | Approval Required |
|------|-------------|-------------------|
| Standard User | Regular application access | Manager |
| Privileged User | Administrative access | Manager + Security |
| Service Account | Application-to-application | Security + Architecture |
| Contractor | Third-party access | Manager + Legal + Security |

### 4.3 Account Deprovisioning

| Trigger | Action | Timeframe |
|---------|--------|-----------|
| Termination | Disable all access | Within 24 hours |
| Role Change | Modify access | Within 5 days |
| Leave of Absence | Disable temporarily | Before leave starts |
| Contract End | Disable access | On contract end date |

---

## 5. Authentication Requirements

### 5.1 Password Policy

| Requirement | Standard |
|-------------|----------|
| Minimum Length | 12 characters |
| Complexity | Upper, lower, number, special |
| History | Cannot reuse last 12 passwords |
| Maximum Age | 90 days |
| Lockout | After 5 failed attempts |
| Lockout Duration | 30 minutes |

### 5.2 Multi-Factor Authentication (MFA)

MFA is **required** for:
- Administrative access
- Remote access (VPN)
- Cloud console access
- Access to PHI systems
- Privileged account use

### 5.3 Session Management

| Setting | Value |
|---------|-------|
| Session Timeout | 15 minutes idle |
| Maximum Session | 8 hours |
| Concurrent Sessions | Limited to 3 |
| Re-authentication | Required for sensitive actions |

---

## 6. Authorization

### 6.1 Role-Based Access Control (RBAC)

| Role | Description | Access Level |
|------|-------------|--------------|
| **Viewer** | Read-only access | View data only |
| **Evaluator** | Clinical user | Read + evaluate |
| **Analyst** | Quality analyst | Read + reports |
| **Admin** | Administrator | Full tenant access |
| **Super Admin** | System admin | Cross-tenant + config |

### 6.2 Role Assignment

- Roles assigned based on job function
- Users may have multiple roles if justified
- Role combinations must not violate separation of duties
- Role assignments documented and auditable

### 6.3 Multi-Tenant Isolation

- Users can only access assigned tenants
- Cross-tenant access requires explicit authorization
- Tenant isolation enforced at application and database levels

---

## 7. Access Reviews

### 7.1 Review Schedule

| Review Type | Frequency | Scope |
|-------------|-----------|-------|
| User Access | Quarterly | All users |
| Privileged Access | Monthly | Admin accounts |
| Service Accounts | Quarterly | All service accounts |
| Third-Party Access | Quarterly | Vendor accounts |

### 7.2 Review Process

1. IT generates access report
2. Manager reviews user access
3. Manager certifies or revokes
4. IT implements changes
5. Security documents review

### 7.3 Review Documentation

- Reviewer name and date
- Users reviewed
- Access confirmed or revoked
- Justification for access
- Follow-up actions

---

## 8. Privileged Access

### 8.1 Privileged Account Types

| Account | Purpose | Controls |
|---------|---------|----------|
| System Admin | Server management | MFA, logging, time-limited |
| Database Admin | Database management | MFA, logging, monitored |
| Security Admin | Security tools | MFA, dual approval |
| Root/Super | Emergency access | Break-glass, dual custody |

### 8.2 Privileged Access Requirements

- Separate from daily-use accounts
- MFA required
- All actions logged
- Regular access review
- Time-limited sessions
- Monitored in real-time

### 8.3 Break-Glass Procedures

- Emergency access only
- Dual authorization required
- All access fully logged
- Post-use review required
- Password changed after use

---

## 9. Remote Access

### 9.1 Requirements

- VPN required for network access
- MFA required for VPN
- Corporate device required (or approved BYOD)
- Endpoint protection required
- Split tunneling prohibited

### 9.2 Third-Party Remote Access

- Time-limited access only
- Sponsored by employee
- MFA required
- Session recording for sensitive access
- Access revoked when project complete

---

## 10. Physical Access

### 10.1 Cloud Environment

- Cloud providers must be SOC2 certified
- Physical access controls verified via SOC reports
- No HDIM personnel physical access to data centers

### 10.2 Workstations

- Screen lock after 5 minutes
- Cable locks for laptops
- Clean desk policy
- Visitor escorts required

---

## 11. Logging and Monitoring

### 11.1 Access Events Logged

- Login success/failure
- Logout
- Permission changes
- Password resets
- MFA enrollment/changes
- Administrative actions

### 11.2 Log Retention

| Log Type | Retention |
|----------|-----------|
| Authentication | 1 year |
| Authorization | 1 year |
| Admin Actions | 7 years |
| PHI Access | 6 years |

---

## 12. Enforcement

Violations may result in:
- Immediate access suspension
- Disciplinary action
- Termination
- Legal action

---

## Appendix A: Access Request Form

```
ACCESS REQUEST FORM

Requestor Information:
- Name:
- Title:
- Department:
- Manager:

Access Requested:
- System(s):
- Role(s):
- Tenant(s):
- Duration:

Business Justification:


Approvals:
- Manager: _____________ Date: _______
- Security: _____________ Date: _______
```

---

*Document Classification: Internal*
*Next Review Date: December 2026*
