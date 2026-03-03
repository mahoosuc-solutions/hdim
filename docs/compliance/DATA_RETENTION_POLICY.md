# Data Retention Policy

## Document Purpose

This document defines HDIM's data retention requirements for electronic Protected Health Information (ePHI) and related records in compliance with HIPAA Security Rule and applicable state regulations.

**Document Version:** 1.0
**Last Updated:** December 2024
**Policy Owner:** Compliance Team
**Review Schedule:** Annual

---

## 1. Retention Requirements

### HIPAA Minimum Requirements

Under HIPAA (45 CFR § 164.530(j)), covered entities and business associates must retain:

- **Policies and Procedures:** 6 years from creation or last effective date
- **Documentation of Required Actions:** 6 years from creation or last effective date
- **Audit Logs:** 6 years (recommended, not explicitly mandated)

### HDIM Retention Schedule

| Data Category | Retention Period | Justification |
|---------------|------------------|---------------|
| **PHI/Clinical Data** | 6 years minimum | HIPAA requirement |
| **Audit Logs** | 6 years | Compliance & investigation support |
| **Authentication Logs** | 6 years | Security incident investigation |
| **Security Incident Records** | 6 years | Post-incident analysis |
| **BAA Documentation** | 6 years after termination | HIPAA requirement |
| **Training Records** | 6 years | Compliance verification |
| **System Backups** | 30 days minimum | Disaster recovery |
| **Transaction Logs** | 1 year | Operational troubleshooting |

---

## 2. Data Classification

### PHI Data Elements

Data requiring 6-year retention:

| Element | Encryption | Storage Location |
|---------|------------|------------------|
| Patient Names | AES-256-GCM | PostgreSQL |
| Dates (DOB, admission, discharge) | None (masked in logs) | PostgreSQL |
| Contact Information | AES-256-GCM | PostgreSQL |
| SSN | AES-256-GCM | PostgreSQL |
| Medical Record Numbers | AES-256-GCM | PostgreSQL |
| Health Plan IDs | AES-256-GCM | PostgreSQL |
| Account Numbers | AES-256-GCM | PostgreSQL |
| Certificate/License Numbers | AES-256-GCM | PostgreSQL |
| Device Identifiers | None | PostgreSQL |
| URLs, IP Addresses | None | PostgreSQL |
| Biometric Identifiers | AES-256-GCM | PostgreSQL |
| Photographs | AES-256-GCM | Object Storage |

### Audit Data Elements

| Element | Retention | Purpose |
|---------|-----------|---------|
| User ID | 6 years | Accountability |
| Action Type | 6 years | Audit trail |
| Timestamp | 6 years | Timeline reconstruction |
| IP Address | 6 years | Forensic investigation |
| Resource Accessed | 6 years | Access tracking |
| Success/Failure | 6 years | Security monitoring |
| Tenant ID | 6 years | Multi-tenant isolation |

---

## 3. Storage Requirements

### Primary Storage

| System | Data Type | Encryption | Backup Frequency |
|--------|-----------|------------|------------------|
| PostgreSQL | PHI, Audit Logs | TDE / Volume | Daily |
| Redis | Session Cache | TLS | Not persisted |
| Object Storage (S3/GCS) | Documents, Images | SSE-S3/SSE-KMS | Daily |

### Backup Storage

| Backup Type | Retention | Encryption | Location |
|-------------|-----------|------------|----------|
| Daily Incremental | 30 days | GPG | Cloud storage |
| Weekly Full | 12 weeks | GPG | Cloud storage |
| Monthly Archive | 6 years | GPG | Cold storage |

### Archive Storage (Long-term)

For data older than 1 year:
- Move to cold storage tier (S3 Glacier / GCS Archive)
- Maintain encryption at rest
- Verify restoration capability quarterly

---

## 4. Retention Implementation

### Database Implementation

```sql
-- Audit log retention policy (PostgreSQL)
-- Retain for 6 years, archive older records

-- Create audit archive table
CREATE TABLE audit.audit_log_archive (LIKE audit.audit_log);

-- Monthly archive job
CREATE OR REPLACE FUNCTION audit.archive_old_records()
RETURNS void AS $$
BEGIN
    -- Move records older than 2 years to archive
    INSERT INTO audit.audit_log_archive
    SELECT * FROM audit.audit_log
    WHERE created_at < NOW() - INTERVAL '2 years';

    -- Delete archived records from main table
    DELETE FROM audit.audit_log
    WHERE created_at < NOW() - INTERVAL '2 years';
END;
$$ LANGUAGE plpgsql;

-- 6-year deletion policy (run on archive table)
CREATE OR REPLACE FUNCTION audit.delete_expired_archives()
RETURNS void AS $$
BEGIN
    DELETE FROM audit.audit_log_archive
    WHERE created_at < NOW() - INTERVAL '6 years';
END;
$$ LANGUAGE plpgsql;
```

### Backup Configuration

```yaml
# backup-policy.yml
backup:
  schedule:
    daily:
      time: "02:00"
      retention_days: 30
      encryption: gpg
    weekly:
      day: sunday
      time: "03:00"
      retention_weeks: 12
      encryption: gpg
    monthly:
      day: 1
      time: "04:00"
      retention_years: 6
      encryption: gpg
      storage_class: glacier
```

---

## 5. Data Destruction

### Secure Deletion Requirements

When retention period expires:

1. **Database Records:** Secure deletion via PostgreSQL `DELETE` with vacuuming
2. **Backup Files:** Cryptographic erasure (key destruction)
3. **Object Storage:** S3/GCS object deletion with versioning disabled
4. **Physical Media:** Certificate of destruction from provider

### Destruction Verification

| Method | Frequency | Documentation |
|--------|-----------|---------------|
| Deletion Logs | Per event | Automated logging |
| Audit Review | Quarterly | Compliance report |
| Certificate of Destruction | As needed | Vendor certificate |

### Exception Handling

Data may be retained beyond standard period if:
- Subject to legal hold
- Required for ongoing investigation
- Regulatory audit in progress

Document all exceptions with:
- Reason for extended retention
- Authorized approver
- Expected release date

---

## 6. Tenant Data Handling

### Multi-Tenant Isolation

Each tenant's data is:
- Logically separated via tenant_id
- Subject to same retention policy
- Deleted independently on tenant termination

### Tenant Offboarding

When tenant relationship ends:

1. **Notification:** 30-day advance notice
2. **Data Export:** Provide data in standard format (FHIR, CSV)
3. **Retention Hold:** 90-day hold for retrieval requests
4. **Secure Deletion:** Cryptographic erasure after hold period
5. **Certificate:** Provide destruction certificate

---

## 7. Legal Hold Procedures

### Hold Implementation

When legal hold is required:

1. Identify scope (users, date range, data types)
2. Suspend automated deletion for in-scope data
3. Document hold with:
   - Authorizing party
   - Legal matter reference
   - Scope definition
   - Start date
4. Notify data custodians
5. Preserve chain of custody

### Hold Release

When hold is released:

1. Obtain written release authorization
2. Resume normal retention/deletion schedule
3. Document release date and authorizer
4. Apply standard retention from original date

---

## 8. Compliance Monitoring

### Automated Checks

| Check | Frequency | Alert Threshold |
|-------|-----------|-----------------|
| Backup Success | Daily | Any failure |
| Archive Job Completion | Monthly | Any failure |
| Retention Policy Execution | Monthly | >100 exceptions |
| Storage Utilization | Weekly | >80% capacity |

### Manual Reviews

| Review | Frequency | Reviewer |
|--------|-----------|----------|
| Policy Compliance | Quarterly | Compliance team |
| Exception Review | Monthly | Security team |
| Destruction Verification | Quarterly | Audit team |

---

## 9. Responsibilities

| Role | Responsibilities |
|------|------------------|
| **Compliance Officer** | Policy ownership, regulatory updates |
| **Security Team** | Technical implementation, monitoring |
| **Engineering** | Backup/restore procedures, automation |
| **Legal** | Hold management, regulatory guidance |
| **Operations** | Daily execution, issue escalation |

---

## 10. Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | Dec 2024 | Compliance Team | Initial creation |

---

## Appendix A: State-Specific Requirements

Some states require longer retention periods:

| State | Requirement | HDIM Compliance |
|-------|-------------|-----------------|
| California | 7 years (minors: until age 25) | Apply extended retention |
| New York | 6 years | Standard policy |
| Texas | 7 years | Apply extended retention |
| Florida | 5 years | Standard policy |

For patients in states with longer requirements, apply the more stringent standard.

---

## Appendix B: Quick Reference

### Minimum Retention Summary

- **PHI:** 6 years
- **Audit Logs:** 6 years
- **Backups:** 30 days (daily), 6 years (monthly archive)
- **BAAs:** 6 years after termination

### Key Contacts

| Role | Contact |
|------|---------|
| Compliance | compliance@hdim.health |
| Security | security@hdim.health |
| Legal | legal@hdim.health |
