---
id: "product-security-architecture"
title: "Security Architecture & HIPAA Compliance"
portalType: "product"
path: "product/02-architecture/security-architecture.md"
category: "architecture"
subcategory: "security"
tags: ["security", "HIPAA", "encryption", "compliance", "access-control", "audit-logging"]
summary: "Comprehensive security architecture for HealthData in Motion covering HIPAA compliance, authentication, authorization, encryption, audit logging, incident response, and risk management for healthcare data protection."
estimatedReadTime: 20
difficulty: "advanced"
targetAudience: ["cio", "security-officer", "compliance-officer"]
owner: "Product Architecture"
reviewCycle: "quarterly"
status: "published"
version: "1.0"
seoKeywords: ["HIPAA compliance", "healthcare security", "encryption", "access control", "audit logging", "data protection"]
relatedDocuments: ["system-architecture", "disaster-recovery", "data-model"]
lastUpdated: "2025-12-01"
---

# Security Architecture & HIPAA Compliance

## Executive Summary

HealthData in Motion implements a **defense-in-depth security architecture** with multiple layers protecting patient data and ensuring HIPAA compliance. The platform achieves **HITRUST CSF certification** and maintains **SOC 2 Type II compliance**, with continuous monitoring and automatic threat response.

**Security Pillars**:
- HIPAA Business Associate Agreement (BAA) compliant
- Defense-in-depth: Network, application, data, endpoint security
- Encryption: In transit (TLS 1.2+), at rest (AES-256), backups
- Access control: OAuth 2.0, MFA, role-based (RBAC)
- Audit logging: Immutable 7-year retention
- Incident response: 24/7 monitoring, <30 minute response SLA

## Network Security

### Virtual Private Cloud (VPC)
```
Public Subnet:
  - API Gateway (Kong) × 2 instances
  - NAT Gateway (outbound traffic)

Private Subnet:
  - Application servers (Spring Boot)
  - Database (PostgreSQL)
  - Cache (Redis)
  - Message queue (Kafka)

Security Groups:
  - Internet → Gateway (443 only)
  - Gateway → App servers (8080 only)
  - App servers → Database (5432 only)
  - App servers → Kafka (9092 only)
```

**Benefits**:
- No direct internet access to application or database
- All traffic routed through API Gateway
- Network-level traffic control
- Automatic DDoS protection (AWS Shield)

### Web Application Firewall (WAF)
Protects API Gateway from common attacks:

**Rules Enabled**:
- SQL injection protection (detects common payloads)
- Cross-site scripting (XSS) protection
- Cross-site request forgery (CSRF) protection
- Rate limiting (10,000 req/hour per IP)
- Bot protection (block suspicious patterns)
- Geo-blocking (restrict by country if needed)

**Response**: Block malicious requests, log to CloudWatch, alert security team

### DDoS Protection
**AWS Shield Standard** (automatic):
- Layer 3/4 attack detection
- Automatic mitigation

**AWS Shield Advanced** (optional):
- Layer 7 attack detection
- DDoS cost protection
- 24/7 AWS DRT support

## Authentication & Authorization

### OAuth 2.0 / OIDC Integration
Primary authentication mechanism:

**Authorization Code Flow** (user-initiated):
1. User clicks "Sign in with [IdP]"
2. Redirected to identity provider (Azure AD, Okta, etc.)
3. User enters credentials (+ MFA if enabled)
4. Authorization code returned to app
5. App exchanges code for access token
6. User logged in, token cached locally

**Client Credentials Flow** (server-to-server):
1. Service A needs to call Service B API
2. Service A provides client_id + client_secret
3. IdP returns access token
4. Service A uses token for API calls
5. Token expires in 1 hour

**Refresh Token Rotation**:
- Access token: 1 hour expiration
- Refresh token: 30 days expiration
- Automatic rotation on use
- Revocation on logout

### Multi-Factor Authentication (MFA)
**For Sensitive Operations**:
- Admin user creation/modification
- Data exports (>1000 records)
- System configuration changes
- Security audit reviews

**Supported Methods**:
- TOTP (Time-based One-Time Password) - authenticator app
- SMS OTP (SMS text message)
- Hardware keys (FIDO2/U2F)

**Implementation**:
```
User action → MFA challenge → 30-second window →
User enters code → Verified → Action allowed
```

### Role-Based Access Control (RBAC)
**5 Primary Roles**:

1. **Super Admin**
   - Full platform access
   - User management
   - System configuration
   - Audit log access
   - Requires MFA always

2. **Facility Admin**
   - Facility-level configuration
   - User management for facility
   - Reports and dashboards
   - Cannot access other facilities

3. **Physician / Provider**
   - View own patients
   - View care gaps for own patients
   - Recommend care actions
   - View own performance metrics

4. **Care Manager**
   - View all patients in facility
   - Create outreach interventions
   - Track gap closure
   - Cannot modify physician data

5. **Medical Assistant**
   - View patient lists
   - Document patient interactions
   - Cannot access reports or analytics

**Permission Model**:
```
Role → Resource Type → Action (read, write, delete)
Physician → Patient → Read own patients only
Care Manager → CareGap → Write (create/update gaps)
Facility Admin → User → Full CRUD
```

## Data Encryption

### In-Transit Encryption
**TLS 1.2+** for all network communication:

```
Client → API Gateway: TLS 1.3
API Gateway → Application: TLS 1.2
Application → Database: TLS 1.2
Application → Kafka: TLS 1.2
Backups → S3: TLS 1.2
```

**Certificate Management**:
- Certificates issued by AWS Certificate Manager
- Auto-renewal 30 days before expiration
- No manual certificate management needed
- Supports wildcard certificates

**Cipher Suites** (ordered by preference):
1. TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
2. TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
3. TLS_DHE_RSA_WITH_AES_256_GCM_SHA384

### At-Rest Encryption
**PostgreSQL Native Encryption**:
- Database encryption at storage level
- 256-bit AES encryption
- Transparent (no application changes)
- Encryption key managed by AWS KMS

**Sensitive Field Encryption** (application-level):
```
Fields requiring additional encryption:
  - Email addresses
  - Phone numbers
  - Social Security numbers
  - Genetic information

Mechanism:
  - AES-256-GCM encryption
  - Unique IV per field
  - Key in AWS Secrets Manager
  - Encrypt on write, decrypt on read
```

### Backup Encryption
**S3 Backup Storage**:
- Default KMS encryption (AES-256)
- Customer master key (CMK) option available
- Versioning enabled (30-day retention)
- Encryption in transit during backup

**Backup Process**:
```
1. PostgreSQL continuous replication
2. Daily snapshots to S3
3. Automatic compression
4. KMS encryption on storage
5. Immutable (no deletion for 30 days)
6. Cross-region replication option
```

## Audit Logging & Monitoring

### Comprehensive Audit Trail
**Every action logged**:
```
{
  "timestamp": "2025-12-01T10:30:45Z",
  "userId": "user-123",
  "action": "patient_data_viewed",
  "resourceType": "Patient",
  "resourceId": "patient-456",
  "details": {
    "fields": ["name", "mrn", "dob"],
    "result": "SUCCESS"
  },
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0..."
}
```

**Log Retention**:
- Active logs: PostgreSQL (real-time queries)
- Archive logs: S3 (1-7 year retention)
- Search logs: Elasticsearch (30-day index)
- Immutable: Cannot be modified after 30 days

### Real-Time Monitoring
**ELK Stack** (Elasticsearch, Logstash, Kibana):
- All application logs centralized
- Real-time alerting on suspicious activity
- Performance metrics tracked
- Query execution tracked

**Key Metrics Monitored**:
- Failed authentication attempts (alert on >5/5min)
- Unusual data access patterns
- Large data exports (>10K records)
- Configuration changes
- API rate limit violations
- Database slow queries (>5s)

### Security Incident Response

**24/7 Monitoring**:
- CloudWatch monitors application metrics
- Security Group flow logs analyzed
- VPC Flow Logs checked for anomalies
- Database audit logs reviewed

**Incident Response Process**:
```
1. Detection: Alert triggered by monitoring
2. Investigation: Security team reviews logs (< 5 min)
3. Containment: Isolate affected resource (< 15 min)
4. Response: Apply remediation (< 30 min)
5. Communication: Notify customer if needed (< 1 hour)
6. Post-mortem: Document lessons learned
```

**SLA**: All security incidents responded to within 30 minutes

## HIPAA Compliance

### Business Associate Agreement (BAA)
HealthData in Motion is a Business Associate under HIPAA:
- Signed BAA with all customers required
- Defines responsibilities and liabilities
- Standard HIPAA limitations apply
- Regular audits performed

### Administrative Safeguards
**Workforce Security**:
- User authentication (OAuth 2.0)
- Unique user IDs (no shared accounts)
- Access levels assigned based on role
- MFA for sensitive operations

**Information Access Management**:
- Minimum necessary principle
- Role-based access control
- Data access logged and audited
- Regular access reviews (quarterly)

### Physical Safeguards
**Data Center Security**:
- AWS-managed data centers (multiple regions)
- Physical access restricted (biometric, badge)
- Video surveillance
- Environmental controls (temperature, humidity)
- Fire suppression systems

### Technical Safeguards
**Access Control**:
- Unique user identification
- Emergency access procedures (admin break-glass)
- Encryption and decryption mechanisms
- Automatic logoff (15 minutes inactivity)

**Audit Controls**:
- Hardware and software inventoried
- Configuration management
- Log monitoring and analysis
- Vulnerability scanning (automated)

**Integrity Verification**:
- Checksums on critical data transfers
- Redundant backups (verification testing)
- Change logs for configuration
- Version control for applications

### Physical Safeguards - Application Level
**De-identification**:
- Data export tool removes identifiers on request
- HIPAA Safe Harbor method supported
- Custom field selection available

**Encryption & Decryption**:
- All data encrypted in transit and at rest
- Only authenticated users can decrypt
- Decryption logged for audit trail

## Vulnerability Management

### Continuous Scanning
**Automated Scanning**:
- Container image scanning (on push)
- Dependency scanning (weekly)
- Application code scanning (weekly)
- Infrastructure scanning (daily)

**Scanning Tools**:
- Trivy (container images)
- Dependabot (dependencies)
- SonarQube (code quality & security)
- AWS Inspector (infrastructure)

### Patch Management
**Critical Vulnerabilities**: Patched within 24 hours
**High Severity**: Patched within 1 week
**Medium Severity**: Patched within 2 weeks
**Low Severity**: Patched within 30 days

### Penetration Testing
- Quarterly external penetration tests
- Annual red team exercise
- Bug bounty program active
- Results reported to executives quarterly

## Incident Response Plan

### Incident Classification
**Critical**: Data breach, service outage, ransomware
- Response time: <15 minutes
- Escalation: Immediate to CISO

**High**: Authentication bypass, unauthorized access
- Response time: <1 hour
- Escalation: Within 2 hours

**Medium**: Failed security control, policy violation
- Response time: <4 hours
- Escalation: Daily review

**Low**: Configuration drift, outdated patch
- Response time: <7 days
- Escalation: Weekly review

### Breach Notification Procedures
**Potential Breach Detected**:
1. Isolate affected system (immediately)
2. Investigate scope of breach (within 24 hours)
3. Notify affected individuals (within 60 days per HIPAA)
4. Notify HHS (within 60 days for breaches >500 people)
5. Notify media (if >500 residents in state)
6. Document findings in breach log

**Breach Log** (maintained perpetually):
- Date discovered
- Date of breach
- Description of breach
- Number of people affected
- Mitigation steps taken

## Security Certifications

### HIPAA Compliance
- Business Associate Agreement in place
- Annual HIPAA risk assessment performed
- Workforce training completed (annual)
- Sanction policy established

### HITRUST CSF Certification
- **Common Security Framework** certification
- Demonstrates HIPAA, HITECH, and other healthcare standards compliance
- Third-party audit performed annually
- 267 security requirements validated

### SOC 2 Type II Compliance
- System and Organization Controls audit
- 6-month assessment period (typical)
- Areas tested:
  - Security (confidentiality, integrity, availability)
  - Availability
  - Processing integrity
  - Confidentiality
  - Privacy
- Annual re-certification required

### Third-Party Audits
- Penetration testing: Quarterly
- Vulnerability assessment: Monthly
- Security audit: Annual (customer-requested)
- Compliance audit: Annual

## Compliance Documentation

### Security Policies
Available to Business Associates:
- Access Control Policy
- Incident Response Plan
- Business Continuity Plan
- Disaster Recovery Plan
- Data Retention & Disposal Policy
- Encryption Policy
- Change Management Policy

### Training & Awareness
- Annual HIPAA training (mandatory)
- Security awareness training (quarterly)
- Role-specific training (new hires)
- Incident simulation drills (semi-annual)

### Risk Management
- Annual risk assessment
- Vulnerability management program
- Patch management program
- Change management procedures
- Incident response procedures

## Data Protection Standards

### PII Protection
**Personally Identifiable Information** handling:
- Minimize collection (only what's necessary)
- Encrypt sensitive fields
- Access control enforced
- Logs maintained
- Deletion on request (GDPR-compliant)

### GDPR Compliance
- Right to access (data export in 30 days)
- Right to be forgotten (data deletion)
- Data portability (export in standard format)
- Consent management (opt-in tracking)

### State Privacy Laws
- California CCPA: Supported
- Virginia VCDPA: Supported
- Colorado CPA: Supported
- Other state laws: Compliant or in progress

## Conclusion

HealthData in Motion's security architecture provides enterprise-grade protection for patient data with multiple layers of defense, continuous monitoring, and automatic threat response. HIPAA, HITRUST, and SOC 2 certifications demonstrate commitment to healthcare security standards.

**Next Steps**:
- See [System Architecture](system-architecture.md) for deployment security
- Review [Disaster Recovery](disaster-recovery.md) for continuity planning
- Check [Data Model](data-model.md) for data protection details
