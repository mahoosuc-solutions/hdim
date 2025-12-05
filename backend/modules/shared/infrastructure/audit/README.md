# HIPAA Audit Module

A comprehensive HIPAA-compliant audit logging module for healthcare applications.

## Overview

This module provides automated audit logging that meets HIPAA requirements for tracking access to Protected Health Information (PHI).

### Compliance Standards

- **45 CFR § 164.312(b)** - Audit Controls
- **45 CFR § 164.308(a)(1)(ii)(D)** - Information System Activity Review
- **7-year retention requirement**
- **AES-256-GCM encryption** (NIST approved, FIPS 140-2 compliant)

## Features

- ✅ **Automatic audit logging** using AOP (@Audited annotation)
- ✅ **AES-256-GCM encryption** for sensitive data
- ✅ **Spring Security integration** for user context
- ✅ **HTTP context extraction** (IP address, user agent, request path)
- ✅ **FHIR AuditEvent support** (optional)
- ✅ **Comprehensive audit data** (who, what, when, where, why, outcome)
- ✅ **7-year retention policy** support
- ✅ **Emergency access tracking** (break-glass scenarios)

## Quick Start

### 1. Add Dependency

Add to your service's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":modules:shared:infrastructure:audit"))
}
```

### 2. Enable Audit Module

Add to your `application.yml`:

```yaml
audit:
  enabled: true
  encryption:
    # Generate with: openssl rand -base64 32
    key: ${AUDIT_ENCRYPTION_KEY:your-base64-encoded-256-bit-key}
```

### 3. Use @Audited Annotation

```java
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    @GetMapping("/{id}")
    @Audited(
        action = AuditAction.READ,
        resourceType = "Patient",
        purposeOfUse = "TREATMENT",
        includeResponsePayload = true,
        encryptPayload = true
    )
    public Patient getPatient(@PathVariable String id) {
        return patientService.findById(id);
    }

    @PostMapping
    @Audited(
        action = AuditAction.CREATE,
        resourceType = "Patient",
        purposeOfUse = "TREATMENT",
        includeRequestPayload = true,
        encryptPayload = true
    )
    public Patient createPatient(@RequestBody Patient patient) {
        return patientService.save(patient);
    }
}
```

## Architecture

### Components

1. **AuditEvent** - Immutable audit event model
2. **@Audited** - Annotation to mark methods for auditing
3. **AuditAspect** - AOP aspect that intercepts annotated methods
4. **AuditService** - Business logic for audit events
5. **AuditEncryptionService** - AES-256-GCM encryption for sensitive data
6. **AuditAutoConfiguration** - Spring Boot auto-configuration

### Data Model

Each audit event captures:

**WHO** - User performing the action:
- User ID
- Username
- Role
- IP address
- User agent

**WHAT** - Action performed:
- Action type (CREATE, READ, UPDATE, DELETE, etc.)
- Resource type (Patient, Observation, etc.)
- Resource ID
- Request/response payloads (optional, encrypted)

**WHEN** - Timestamp (ISO-8601)

**WHERE** - System context:
- Service name
- Method name
- Request path

**WHY** - Purpose of use:
- TREATMENT
- PAYMENT
- OPERATIONS
- RESEARCH
- etc.

**OUTCOME**:
- SUCCESS
- MINOR_FAILURE
- SERIOUS_FAILURE
- MAJOR_FAILURE

## Usage Examples

### Manual Audit Logging

```java
@Service
public class PatientService {

    private final AuditService auditService;

    public void processPatientData(String patientId) {
        // Manual audit logging
        auditService.logEvent(
            "tenant-1",           // tenantId
            "user-123",           // userId
            AuditAction.READ,     // action
            "Patient",            // resourceType
            patientId,            // resourceId
            AuditOutcome.SUCCESS  // outcome
        );
    }
}
```

### Login Audit

```java
@Service
public class AuthenticationService {

    private final AuditService auditService;

    public void login(String username, String ipAddress) {
        try {
            // Authenticate user
            authenticate(username);

            // Log successful login
            auditService.logLogin(username, ipAddress, true, null);
        } catch (AuthenticationException e) {
            // Log failed login
            auditService.logLogin(username, ipAddress, false, e.getMessage());
            throw e;
        }
    }
}
```

### Emergency Access

```java
@Service
public class EmergencyAccessService {

    private final AuditService auditService;

    public Patient emergencyAccess(String patientId, String justification) {
        // Log emergency access
        auditService.logEmergencyAccess(
            "tenant-1",
            "doctor-123",
            "Patient",
            patientId,
            justification
        );

        return patientService.findById(patientId);
    }
}
```

## Encryption

The module uses **AES-256-GCM** encryption for sensitive data:

- **Algorithm**: AES/GCM/NoPadding
- **Key size**: 256 bits
- **IV size**: 12 bytes (randomly generated for each encryption)
- **Tag length**: 128 bits

### Generate Encryption Key

```bash
# Generate a 256-bit key
openssl rand -base64 32

# Set as environment variable
export AUDIT_ENCRYPTION_KEY="<your-generated-key>"
```

**Production**: Use a secure key management service (AWS KMS, HashiCorp Vault, Azure Key Vault, etc.)

## Configuration

### application.yml

```yaml
audit:
  enabled: true  # Enable/disable audit module (default: true)
  encryption:
    key: ${AUDIT_ENCRYPTION_KEY}  # Base64-encoded 256-bit key

# Optional: Configure retention policy
audit:
  retention:
    years: 7  # HIPAA requirement (default: 7)
```

## Testing

Run tests:

```bash
./gradlew :modules:shared:infrastructure:audit:test
```

Current test coverage:
- ✅ Encryption/decryption
- ✅ Unique IV generation
- ✅ Unicode character support
- ✅ Audit event builder
- ✅ Audit service operations
- ✅ Login/access/emergency logging

## Database Schema

The audit events can be persisted to a database. Recommended schema:

```sql
CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    tenant_id VARCHAR(64),

    -- Who
    user_id VARCHAR(64),
    username VARCHAR(128),
    role VARCHAR(64),
    ip_address VARCHAR(45),
    user_agent TEXT,

    -- What
    action VARCHAR(32) NOT NULL,
    resource_type VARCHAR(64),
    resource_id VARCHAR(64),
    outcome VARCHAR(32) NOT NULL,

    -- Where
    service_name VARCHAR(128),
    method_name VARCHAR(128),
    request_path VARCHAR(512),

    -- Why
    purpose_of_use VARCHAR(64),

    -- Additional
    request_payload TEXT,  -- Encrypted if sensitive
    response_payload TEXT, -- Encrypted if sensitive
    error_message TEXT,
    duration_ms BIGINT,
    fhir_audit_event_id VARCHAR(64),
    encrypted BOOLEAN DEFAULT FALSE,

    -- Indexes for querying
    INDEX idx_audit_timestamp (timestamp),
    INDEX idx_audit_user (user_id, timestamp),
    INDEX idx_audit_resource (resource_type, resource_id, timestamp),
    INDEX idx_audit_tenant (tenant_id, timestamp)
);

-- Partitioning by month (recommended for large datasets)
-- PostgreSQL 10+
CREATE TABLE audit_events (
    -- columns as above
) PARTITION BY RANGE (timestamp);

CREATE TABLE audit_events_2025_01 PARTITION OF audit_events
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
```

## Retention Policy

HIPAA requires 7-year retention. Implement a scheduled job to purge old data:

```java
@Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
public void purgeOldAuditEvents() {
    int purgedCount = auditService.purgeOldAuditEvents();
    logger.info("Purged {} audit events older than 7 years", purgedCount);
}
```

## Best Practices

1. **Always encrypt PHI**: Set `encryptPayload = true` for PHI data
2. **Limit payload logging**: Only log payloads when necessary (storage costs)
3. **Use purpose of use**: Always specify the purpose (TREATMENT, PAYMENT, etc.)
4. **Secure key management**: Never hardcode encryption keys
5. **Monitor audit failures**: Set up alerts for audit logging failures
6. **Regular reviews**: Conduct periodic audit log reviews for compliance
7. **Retain for 7 years**: Meet HIPAA retention requirements

## Troubleshooting

### Audit logging not working

1. Check `audit.enabled=true` in configuration
2. Verify AspectJ weaver is on classpath
3. Ensure `@EnableAspectJAutoProxy` is configured
4. Check that methods are public and not called internally

### Encryption errors

1. Verify encryption key is Base64-encoded 256-bit key
2. Check key is properly set in environment/config
3. Ensure Java Cryptography Extension (JCE) is installed

### Performance issues

1. Disable payload logging for high-volume endpoints
2. Use asynchronous audit logging (Kafka, message queue)
3. Implement database partitioning for large datasets
4. Consider archiving old audit events to cold storage

## Roadmap

- [ ] Database persistence (JPA repository)
- [ ] Async audit logging (Kafka integration)
- [ ] FHIR AuditEvent resource generation
- [ ] Audit query API
- [ ] Audit dashboard (UI)
- [ ] Automated retention policy enforcement
- [ ] Integration with SIEM systems
- [ ] AWS KMS/Azure Key Vault integration

## License

MIT License
