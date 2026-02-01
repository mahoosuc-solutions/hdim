# Security & HIPAA Incident Response Runbook

**Service**: HDIM Platform (All Services)
**Version**: 1.0.0
**Date**: 2025-12-24
**Classification**: Internal Use Only
**Owner**: Security & Compliance Team

---

## Critical: HIPAA Breach Notification Requirements

**HIPAA requires notification within 60 days of breach discovery.**

| Breach Size | Notification Requirements |
|-------------|---------------------------|
| < 500 individuals | Annual report to HHS |
| >= 500 individuals | Notify HHS within 60 days, media notification required |
| Any size | Notify affected individuals within 60 days |

**Contact immediately if PHI breach is suspected:**
- Security Lead: @security-lead (Slack)
- Privacy Officer: privacy@healthdata.com
- Legal Counsel: legal@healthdata.com

---

## Quick Reference

### Security Incident Severity

| Level | Definition | Examples | Response |
|-------|------------|----------|----------|
| **CRITICAL** | Active breach, PHI exposed | Data exfiltration, ransomware, compromised credentials with PHI access | Immediate containment, executive notification |
| **HIGH** | Potential breach, security control failure | Suspicious access patterns, failed security controls, vulnerability actively exploited | < 1 hour response |
| **MEDIUM** | Security anomaly, policy violation | Unauthorized access attempt, misconfiguration discovered | < 4 hours response |
| **LOW** | Minor security event | Failed login attempts, policy reminder needed | Next business day |

### Emergency Contacts

| Role | Contact | When to Call |
|------|---------|--------------|
| Security Lead | @security-lead | All security incidents |
| Privacy Officer | @privacy-officer | Any PHI involvement |
| Legal Counsel | legal@healthdata.com | Breach notification decisions |
| CISO | @ciso | CRITICAL level incidents |
| FBI Cyber | ic3.gov | Criminal activity suspected |
| HHS OCR | ocrportal.hhs.gov | HIPAA breach reporting |

### Immediate Containment Commands

```bash
# EMERGENCY: Block IP address
kubectl exec -it istio-ingressgateway-xxx -- \
  iptables -A INPUT -s <attacker_ip> -j DROP

# EMERGENCY: Disable user account
curl -X PATCH http://auth-service:8080/admin/users/<user_id>/disable

# EMERGENCY: Revoke all sessions for user
curl -X POST http://auth-service:8080/admin/users/<user_id>/revoke-sessions

# EMERGENCY: Enable maintenance mode
kubectl set env deployment/gateway-service MAINTENANCE_MODE=true -n healthdata-prod

# EMERGENCY: Rotate secrets
kubectl delete secret db-credentials -n healthdata-prod
kubectl create secret generic db-credentials --from-literal=password=$(openssl rand -base64 32)
```

---

## Table of Contents

1. [Incident Classification](#incident-classification)
2. [Immediate Response](#immediate-response)
3. [Containment Procedures](#containment-procedures)
4. [Investigation](#investigation)
5. [Eradication & Recovery](#eradication--recovery)
6. [HIPAA Breach Assessment](#hipaa-breach-assessment)
7. [Notification Procedures](#notification-procedures)
8. [Evidence Preservation](#evidence-preservation)
9. [Post-Incident Activities](#post-incident-activities)

---

## Incident Classification

### Security Incident Types

| Type | Description | Initial Response |
|------|-------------|------------------|
| **Data Breach** | Unauthorized access to PHI | Contain, notify security lead, preserve evidence |
| **Malware/Ransomware** | Malicious software detected | Isolate system, do not pay ransom, engage IR team |
| **Account Compromise** | Credentials stolen or misused | Disable account, review access logs |
| **Insider Threat** | Employee misconduct | Preserve evidence, involve HR and legal |
| **DDoS Attack** | Service availability attack | Enable DDoS protection, scale infrastructure |
| **Vulnerability Exploitation** | Known CVE exploited | Patch immediately, assess damage |
| **Phishing** | Social engineering attack | Disable compromised accounts, warn users |
| **Physical Security** | Unauthorized physical access | Secure area, review camera footage |

### PHI Classification

| Data Type | Classification | Examples |
|-----------|---------------|----------|
| **Direct Identifiers** | HIGH | Name, SSN, MRN, email, phone |
| **Quasi-Identifiers** | MEDIUM | DOB, ZIP, gender, admission date |
| **Health Information** | HIGH | Diagnoses, medications, lab results |
| **Financial Information** | HIGH | Insurance ID, billing records |

---

## Immediate Response

### Step 1: Assess & Classify (< 5 minutes)

```markdown
## Initial Assessment Checklist

1. [ ] What type of incident is this?
2. [ ] Is PHI potentially affected?
3. [ ] Is the threat active or contained?
4. [ ] Which systems are affected?
5. [ ] How was the incident detected?
6. [ ] Who reported it?
7. [ ] What is the initial scope estimate?
8. [ ] Did a recent release change licensing or distribution scope?
```

### Step 2: Activate Response Team (< 10 minutes)

**For CRITICAL/HIGH incidents:**

```markdown
## Notification Chain

1. [ ] Security Lead: @security-lead
2. [ ] Privacy Officer (if PHI): @privacy-officer
3. [ ] Engineering Lead: @engineering-lead
4. [ ] Legal (if breach): legal@healthdata.com
5. [ ] CISO (CRITICAL only): @ciso
```

**Create Incident Channel:**
```
/channel create security-inc-YYYY-MM-DD
/invite @security-team @incident-commander
```

### Step 3: Document Everything

```markdown
## Incident Log Entry

**Time (UTC)**: YYYY-MM-DD HH:MM
**Reporter**: [Name]
**Description**: [What happened]
**Systems Affected**: [List]
**PHI Involvement**: Yes/No/Unknown
**Current Status**: Investigating/Contained/Resolved
**Actions Taken**: [List]
```

---

## Containment Procedures

### Account Compromise

```bash
# 1. Identify compromised account
curl http://auth-service:8080/admin/users/search?email=<email>

# 2. Disable the account immediately
curl -X PATCH http://auth-service:8080/admin/users/<user_id> \
  -H "Content-Type: application/json" \
  -d '{"status": "DISABLED", "reason": "SECURITY_INCIDENT"}'

# 3. Revoke all active sessions
curl -X POST http://auth-service:8080/admin/users/<user_id>/revoke-all-sessions

# 4. Rotate any API keys
curl -X POST http://auth-service:8080/admin/users/<user_id>/rotate-api-keys

# 5. Review recent activity
curl http://audit-service:8080/audit/search?userId=<user_id>&since=7d \
  | jq '.events[] | {timestamp, action, resource, ip}'
```

### Malware/Ransomware

```bash
# 1. Isolate affected pods immediately
kubectl cordon <node-name>
kubectl drain <node-name> --ignore-daemonsets --delete-emptydir-data

# 2. Preserve the affected container for forensics
kubectl logs <pod-name> > incident-logs-$(date +%Y%m%d-%H%M%S).txt
kubectl cp <pod-name>:/app/logs ./forensics/

# 3. Delete and replace the affected pods
kubectl delete pod <pod-name> --force

# 4. Scan all images for malware
for image in $(kubectl get pods -n healthdata-prod -o jsonpath='{.items[*].spec.containers[*].image}'); do
  trivy image --severity HIGH,CRITICAL $image
done

# 5. Enable read-only mode on storage
kubectl patch pvc <pvc-name> -p '{"spec":{"accessModes":["ReadOnlyMany"]}}'
```

### Network Attack (DDoS/Intrusion)

```bash
# 1. Enable rate limiting at gateway
kubectl set env deployment/gateway-service \
  RATE_LIMIT_ENABLED=true \
  RATE_LIMIT_REQUESTS_PER_SECOND=10 \
  -n healthdata-prod

# 2. Block attacking IPs (example with Istio)
kubectl apply -f - <<EOF
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: block-attackers
  namespace: healthdata-prod
spec:
  action: DENY
  rules:
  - from:
    - source:
        ipBlocks: ["<attacker_ip>/32"]
EOF

# 3. Enable additional logging
kubectl set env deployment/gateway-service LOG_LEVEL=DEBUG -n healthdata-prod

# 4. Scale up to handle load
kubectl scale deployment/gateway-service --replicas=10 -n healthdata-prod
```

### Data Exfiltration

```bash
# 1. Block outbound traffic from affected pods
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: block-egress-emergency
  namespace: healthdata-prod
spec:
  podSelector:
    matchLabels:
      app: <affected-app>
  policyTypes:
  - Egress
  egress: []  # Block all egress
EOF

# 2. Capture network traffic for forensics
kubectl exec -it <pod-name> -- tcpdump -w /tmp/capture.pcap &

# 3. Review database audit logs
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "SELECT * FROM audit_log WHERE action = 'SELECT' AND timestamp > now() - interval '24 hours' ORDER BY timestamp DESC;"

# 4. Identify data accessed
curl http://audit-service:8080/audit/data-access-report?since=24h
```

---

## Investigation

### Log Collection

```bash
# Collect all relevant logs
mkdir -p incident-$(date +%Y%m%d)/logs

# Application logs
kubectl logs -n healthdata-prod -l app.kubernetes.io/part-of=hdim --since=24h > incident-$(date +%Y%m%d)/logs/app.log

# Audit logs
curl http://audit-service:8080/audit/export?since=7d > incident-$(date +%Y%m%d)/logs/audit.json

# Authentication logs
curl http://auth-service:8080/admin/auth-logs?since=7d > incident-$(date +%Y%m%d)/logs/auth.json

# Database audit logs
kubectl exec postgres-0 -n healthdata-prod -- pg_dump -U healthdata -t audit_log > incident-$(date +%Y%m%d)/logs/db-audit.sql

# Network logs (if available)
kubectl logs -n istio-system -l app=istio-ingressgateway --since=24h > incident-$(date +%Y%m%d)/logs/gateway.log
```

### Audit Log Analysis

```bash
# Find suspicious access patterns
curl http://audit-service:8080/audit/search \
  -d '{
    "timeRange": {"start": "-7d", "end": "now"},
    "filters": {
      "action": ["READ", "EXPORT", "DOWNLOAD"],
      "resourceType": "Patient"
    },
    "aggregations": {
      "byUser": true,
      "byHour": true
    }
  }' | jq '.'

# Identify bulk data access
curl http://audit-service:8080/audit/anomalies/bulk-access | jq '.'

# Check for after-hours access
curl http://audit-service:8080/audit/search \
  -d '{"timeRange": {"start": "-7d"}, "filters": {"hour": {"gte": 22, "lte": 6}}}' | jq '.'

# Review failed access attempts
curl http://audit-service:8080/audit/search \
  -d '{"filters": {"outcome": "DENIED"}}' | jq '.'
```

### User Activity Timeline

```bash
# Generate timeline for specific user
curl http://audit-service:8080/audit/user-timeline/<user_id> \
  -d '{"start": "-30d", "end": "now"}' | jq '.events[] | {time: .timestamp, action: .action, resource: .resourceId, ip: .sourceIp}'
```

### Database Forensics

```sql
-- Recent bulk queries
SELECT username, query, calls, total_time, rows
FROM pg_stat_statements
WHERE query ILIKE '%patient%' OR query ILIKE '%phi%'
ORDER BY total_time DESC
LIMIT 20;

-- Connection history
SELECT usename, client_addr, backend_start, state, query
FROM pg_stat_activity
WHERE datname = 'healthdata'
ORDER BY backend_start DESC;

-- Table access patterns
SELECT schemaname, relname, seq_scan, seq_tup_read, idx_scan, idx_tup_fetch
FROM pg_stat_user_tables
WHERE relname IN ('patient', 'observation', 'condition')
ORDER BY seq_tup_read DESC;
```

---

## Eradication & Recovery

### Credential Rotation

```bash
# 1. Rotate database credentials
NEW_PASSWORD=$(openssl rand -base64 32)
kubectl exec postgres-0 -n healthdata-prod -- \
  psql -U postgres -c "ALTER USER healthdata PASSWORD '$NEW_PASSWORD';"
kubectl create secret generic db-credentials \
  --from-literal=password=$NEW_PASSWORD \
  --dry-run=client -o yaml | kubectl apply -f -

# 2. Rotate JWT signing key
kubectl delete secret jwt-signing-key -n healthdata-prod
kubectl create secret generic jwt-signing-key \
  --from-literal=key=$(openssl rand -base64 64) \
  -n healthdata-prod

# 3. Invalidate all sessions
curl -X POST http://auth-service:8080/admin/sessions/invalidate-all

# 4. Rotate API keys for external integrations
curl -X POST http://auth-service:8080/admin/api-keys/rotate-all

# 5. Restart all services to pick up new credentials
kubectl rollout restart deployment -n healthdata-prod
```

### System Recovery

```bash
# 1. Verify all secrets rotated
kubectl get secrets -n healthdata-prod -o json | jq '.items[] | {name: .metadata.name, age: .metadata.creationTimestamp}'

# 2. Deploy latest patched images
./scripts/deploy-latest.sh

# 3. Run security scan
trivy k8s --severity HIGH,CRITICAL -n healthdata-prod

# 4. Verify services healthy
kubectl get pods -n healthdata-prod
for svc in gateway fhir-service cql-engine-service; do
  curl -s http://$svc/actuator/health | jq '.status'
done

# 5. Re-enable normal operations
kubectl set env deployment/gateway-service MAINTENANCE_MODE=false -n healthdata-prod
```

---

## HIPAA Breach Assessment

### Breach Determination Flowchart

```
Was PHI accessed, acquired, used, or disclosed?
├── NO → Not a HIPAA breach (document and close)
└── YES → Was it permitted under HIPAA?
          ├── YES → Not a breach
          └── NO → Did the covered entity/BA make an unauthorized disclosure?
                   └── Apply the 4-factor risk assessment:

                   1. Nature and extent of PHI involved
                   2. Unauthorized person who accessed/received PHI
                   3. Whether PHI was actually acquired or viewed
                   4. Extent of risk mitigation
```

### Risk Assessment Documentation

```markdown
## HIPAA Breach Risk Assessment

**Incident ID**: INC-YYYY-XXXX
**Assessment Date**: YYYY-MM-DD
**Assessor**: [Name, Title]

### Factor 1: Nature and Extent of PHI

**Types of identifiers involved**:
- [ ] Names
- [ ] Social Security Numbers
- [ ] Medical Record Numbers
- [ ] Health information (diagnoses, treatments)
- [ ] Financial information
- [ ] Other: ___________

**Number of records potentially affected**: _____

**Sensitivity level**:
- [ ] Low (names only)
- [ ] Medium (contact info + limited health info)
- [ ] High (SSN, detailed health info, mental health, HIV, substance abuse)

### Factor 2: Unauthorized Person

**Who accessed the PHI?**
- [ ] External attacker (unknown)
- [ ] External attacker (known/identified)
- [ ] Unauthorized employee
- [ ] Business associate
- [ ] Other covered entity
- [ ] Unknown

**Likelihood of re-disclosure**: Low / Medium / High

### Factor 3: Acquisition or Viewing

**Evidence that PHI was**:
- [ ] Viewed (evidence of access)
- [ ] Downloaded/copied
- [ ] Transmitted externally
- [ ] No evidence of actual viewing

### Factor 4: Risk Mitigation

**Actions taken**:
- [ ] Obtained attestation of destruction
- [ ] Confirmed PHI not retained
- [ ] Rotated affected credentials
- [ ] Notified affected systems
- [ ] Other: ___________

### Conclusion

**Breach Determination**:
- [ ] LOW probability of compromise → Not a reportable breach
- [ ] SIGNIFICANT probability of compromise → Reportable breach

**Rationale**: [Explain reasoning]

**Reviewed by**: [Legal/Privacy Officer signature]
**Date**: YYYY-MM-DD
```

---

## Notification Procedures

### If Breach Confirmed

#### Individual Notification (Required for all breaches)

**Content requirements** (per 45 CFR 164.404):
1. Description of what happened
2. Types of PHI involved
3. Steps individuals should take
4. What the organization is doing
5. Contact information

**Template**:
```
Subject: Notice of Data Security Incident

Dear [Name],

We are writing to notify you of an incident that may have involved your
personal health information.

[WHAT HAPPENED]
On [date], we discovered [description of incident].

[INFORMATION INVOLVED]
The information that may have been involved includes: [list types of PHI]

[WHAT WE ARE DOING]
We have [describe remediation steps]. We have also notified [relevant authorities].

[WHAT YOU CAN DO]
We recommend that you [protective actions such as monitor statements,
consider credit freeze, etc.].

[CONTACT INFORMATION]
If you have questions, please contact our Privacy Office at:
[phone number]
[email]
[mailing address]

We sincerely apologize for this incident and any concern it may cause.

Sincerely,
[Privacy Officer Name]
[Title]
```

#### HHS OCR Notification

**For breaches affecting 500+ individuals:**
- Notify within 60 days
- Submit through: https://ocrportal.hhs.gov/ocr/breach/wizard_breach.jsf

**For breaches affecting <500 individuals:**
- Annual report to HHS
- Due within 60 days after end of calendar year

#### Media Notification

Required if breach affects 500+ residents of a state:
- Notify prominent media outlets in the state
- Within 60 days of discovery

---

## Evidence Preservation

### Evidence Collection Checklist

```markdown
## Evidence Preservation Log

**Incident ID**: INC-YYYY-XXXX
**Collector**: [Name]
**Date/Time**: YYYY-MM-DD HH:MM UTC

### Digital Evidence

- [ ] System logs (preserved to: _______)
- [ ] Application logs (preserved to: _______)
- [ ] Database audit logs (preserved to: _______)
- [ ] Network captures (preserved to: _______)
- [ ] Memory dumps (if applicable)
- [ ] License compliance verified (docs/compliance/THIRD_PARTY_NOTICES.md)
- [ ] Disk images (if applicable)
- [ ] Container images (preserved to: _______)

### Access Records

- [ ] Authentication logs
- [ ] Authorization decisions
- [ ] API access logs
- [ ] Database query logs

### Chain of Custody

| Item | Collected By | Date/Time | Hash (SHA256) | Storage Location |
|------|--------------|-----------|---------------|------------------|
| | | | | |
```

### Evidence Storage

```bash
# Create forensics directory with timestamp
INCIDENT_DIR="incident-$(date +%Y%m%d-%H%M%S)"
mkdir -p $INCIDENT_DIR/{logs,images,configs,database}

# Collect and hash all evidence
for file in $INCIDENT_DIR/**/*; do
  sha256sum "$file" >> $INCIDENT_DIR/checksums.sha256
done

# Sign the checksum file
gpg --sign $INCIDENT_DIR/checksums.sha256

# Upload to secure storage
aws s3 cp $INCIDENT_DIR s3://hdim-forensics/$INCIDENT_DIR/ --recursive
```

### Retention Requirements

| Evidence Type | Retention Period | Storage Location |
|---------------|------------------|------------------|
| HIPAA breach documentation | 6 years minimum | Secure archive |
| Security incident logs | 3 years | Cold storage |
| Forensic images | Until case closed + 1 year | Secure archive |
| Chain of custody docs | Until case closed + 1 year | Legal hold |

---

## Post-Incident Activities

### Immediate (24-48 hours)

1. **Complete incident documentation**
   - Timeline of events
   - Actions taken
   - Evidence collected
   - Personnel involved

2. **Initial assessment report to leadership**
   - Scope of incident
   - Breach determination (preliminary)
   - Immediate remediation steps
   - Notification timeline

### Short-term (1-2 weeks)

1. **Root cause analysis**
   - What failed?
   - How was it exploited?
   - What controls should have detected it?

2. **Remediation verification**
   - Confirm all vulnerabilities patched
   - Verify credentials rotated
   - Validate security controls

3. **Process improvements**
   - Update detection capabilities
   - Improve response procedures
   - Enhance monitoring

### Long-term

1. **Policy updates**
   - Update security policies
   - Revise incident response plan
   - Enhance training

2. **Technical improvements**
   - Implement additional controls
   - Enhance monitoring
   - Add detection rules

3. **Audit and compliance**
   - Update risk assessment
   - Document in HIPAA compliance records
   - Prepare for potential audits

---

## Appendix

### HIPAA Breach Safe Harbor

**Encryption Safe Harbor**: If PHI was encrypted per NIST standards and the encryption key was not compromised, the incident is NOT a breach requiring notification.

**Limited Data Set**: If only a limited data set (no direct identifiers) was involved, breach risk may be lower.

### Useful References

- HIPAA Breach Notification Rule: 45 CFR 164.400-414
- HHS Breach Reporting: https://ocrportal.hhs.gov/ocr/breach/
- NIST Cybersecurity Framework: https://www.nist.gov/cyberframework
- CISA Incident Reporting: https://www.cisa.gov/report

### Contact Quick Reference

```
Internal Contacts:
- Security: security@healthdata.com, Slack #security-oncall
- Privacy: privacy@healthdata.com
- Legal: legal@healthdata.com
- Communications: comms@healthdata.com

External Contacts:
- HHS OCR: 1-800-368-1019
- FBI IC3: ic3.gov
- CISA: central@cisa.dhs.gov
- Cyber Insurance: [carrier contact info]
```

---

**Runbook Version**: 1.0.0
**Classification**: Internal Use Only
**Last Updated**: 2025-12-24
**Next Review**: 2026-03-24
**Owner**: Security & Compliance Team
**Approved By**: [CISO Name], [Privacy Officer Name]
