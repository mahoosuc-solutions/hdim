# SMS Appointment Reminders

**Feature:** Automated SMS appointment reminders via Twilio
**Issue:** #304
**Status:** ✅ Production Ready
**Version:** 1.0.0

## Quick Start

### Enable for a Tenant

```bash
curl -X POST http://localhost:8088/api/v1/feature-flags/tenant/acme-health/features \
  -H "Content-Type: application/json" \
  -d '{
    "featureName": "twilio-sms-reminders",
    "enabled": true,
    "config": {
      "reminder_days": [1, 3, 7]
    }
  }'
```

### Trigger Reminders Manually

```bash
curl -X POST http://localhost:8088/api/v1/notifications/appointment-reminders/process \
  -H "X-Tenant-ID: acme-health" \
  -d '{"daysBefore": 1}'
```

## Features

- ✅ **Configurable intervals** - Send 1, 3, 7 days before appointment
- ✅ **Tenant-based feature flags** - Enable/disable per customer
- ✅ **Idempotency** - Prevents duplicate SMS sends
- ✅ **HIPAA compliant** - PHI masking, audit logging, consent checks
- ✅ **Error handling** - Database failures, service failures, SMS failures
- ✅ **Multi-tenant isolation** - Tenant-specific configurations

## Architecture

```
┌───────────────────────────────────────────────────────┐
│              Appointment Reminder Flow                │
├───────────────────────────────────────────────────────┤
│                                                        │
│  1. Scheduler (daily @ 9 AM)                         │
│     └──> processReminders(tenant, daysBefore)        │
│                                                        │
│  2. Check Feature Flag                                │
│     └──> isFeatureEnabled("twilio-sms-reminders")    │
│                                                        │
│  3. Query FHIR for Appointments                       │
│     └──> startTime = today + daysBefore              │
│                                                        │
│  4. For each appointment:                             │
│     ├──> Check idempotency (already sent?)           │
│     ├──> Get patient contact info                     │
│     ├──> Validate SMS opt-in                          │
│     ├──> Send SMS via Twilio                          │
│     ├──> Log audit event                              │
│     └──> Record reminder sent                         │
│                                                        │
└───────────────────────────────────────────────────────┘
```

## Configuration

### Feature Flag Schema

```json
{
  "featureName": "twilio-sms-reminders",
  "enabled": true,
  "config": {
    "reminder_days": [1, 3, 7]  // Must be positive integers
  }
}
```

### Validation Rules

| Config Value | Valid? | Fallback Behavior |
|--------------|--------|-------------------|
| `[1, 3, 7]` | ✅ Yes | Use as-is |
| `[]` (empty) | ❌ No | Fallback to `[1]` |
| `["1", "3"]` (strings) | ❌ No | Fallback to `[1]` |
| `[-1, 0, 3]` (negative/zero) | ❌ No | Fallback to `[1]` |
| `null` | ❌ No | Fallback to `[1]` |

### Environment Variables

```bash
# Twilio Credentials
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token_here
TWILIO_PHONE_NUMBER=+1234567890

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/notification_db
SPRING_DATASOURCE_USERNAME=healthdata
SPRING_DATASOURCE_PASSWORD=<secret>

# Feature Flags (Redis)
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
```

## Database Schema

### `appointment_reminders_sent`

**Purpose:** Idempotency tracking and audit trail

```sql
CREATE TABLE appointment_reminders_sent (
    id                 UUID PRIMARY KEY,
    tenant_id          VARCHAR(100) NOT NULL,
    appointment_id     UUID NOT NULL,
    patient_id         UUID NOT NULL,
    phone_number       VARCHAR(20),
    reminder_type      VARCHAR(50) NOT NULL,
    days_before        INTEGER NOT NULL,
    channel            VARCHAR(20) NOT NULL,
    status             VARCHAR(20) NOT NULL,  -- SENT, FAILED
    message_sid        VARCHAR(100),          -- Twilio message SID
    error_message      TEXT,
    sent_at            TIMESTAMP DEFAULT NOW(),

    -- Prevent duplicate sends
    CONSTRAINT uk_reminder_idempotency
        UNIQUE (tenant_id, appointment_id, reminder_type, days_before)
);

CREATE INDEX idx_reminder_tenant ON appointment_reminders_sent(tenant_id);
CREATE INDEX idx_reminder_status ON appointment_reminders_sent(status);
```

## HIPAA Compliance

### PHI Protection

**1. PHI Masking in Logs**
```java
// UUIDs truncated to first 8 characters
log.info("Sent SMS for appointment {} to patient {}",
    maskUuid(appointmentId),  // "a1b2c3d4-****"
    maskUuid(patientId));      // "f9e8d7c6-****"
```

**2. Audit Logging**
```java
// Every SMS send logged to audit service
auditService.logAuditEvent(AuditEvent.builder()
    .tenantId(tenantId)
    .action(AuditAction.CREATE)
    .resourceType("Notification")
    .resourceId(appointmentId.toString())
    .serviceName("notification-service")
    .methodName("sendReminder")
    .outcome(AuditOutcome.SUCCESS)
    .build());
```

**3. Consent Enforcement**
```java
// Only send to patients who explicitly opted in
if (patient.getSmsOptIn() == null || !patient.getSmsOptIn()) {
    log.debug("Patient has not opted in to SMS, skipping");
    return false;
}
```

**4. Multi-Tenant Isolation**
```sql
-- All queries filter by tenant_id
SELECT * FROM appointment_reminders_sent
WHERE tenant_id = :tenantId
  AND appointment_id = :appointmentId;
```

## Error Handling

### Error Propagation Strategy

| Error Scenario | Behavior | Logging Level |
|----------------|----------|---------------|
| SMS send fails | Record failure, continue batch | ERROR |
| Database save fails (after SMS sent) | Throw exception | **CRITICAL** |
| Database save fails (SMS already failed) | Log error, continue | **CRITICAL** |
| Patient service unavailable | Record failure, continue | ERROR |
| FHIR service unavailable | Log error, stop batch | ERROR |

### Critical Alerts

Operators should be alerted on:
- `CRITICAL: Failed to record successful reminder` - Audit gap (SMS sent but not recorded)
- `CRITICAL: Failed to record failed reminder` - Double-failure scenario

Monitor logs for `CRITICAL:` prefix.

## SMS Message Template

**Format:**
```
Appointment Reminder: {patient_name}, you have an appointment with
{provider_name} on {date} at {time} at {location}. Reply STOP to opt out.
```

**Example:**
```
Appointment Reminder: John Doe, you have an appointment with
Dr. Smith on Sunday, February 15, 2026 at 2:30 PM at Main Clinic.
Reply STOP to opt out.
```

## Testing

### Unit Tests (16 tests)

```bash
./gradlew :modules:services:notification-service:test \
    --tests "*AppointmentReminderServiceTest"
```

**Coverage:**
- Feature flag enabled/disabled
- Idempotency checks
- SMS opt-in validation
- Phone number validation
- Successful/failed reminder recording
- Error handling (database, service, SMS failures)
- Configuration parsing (edge cases)
- Audit logging verification

### Integration Tests (7 tests)

```bash
./gradlew :modules:services:notification-service:test \
    --tests "*AppointmentReminderSchedulerIntegrationTest"
```

**Coverage:**
- Database persistence
- Multi-tenant isolation
- Idempotency at database level
- Duplicate prevention via unique constraint
- Multiple reminder intervals

## Monitoring

### Key Metrics

**Business Metrics:**
- `notification.sms.sent.count` - Total SMS sent
- `notification.sms.failed.count` - Total SMS failures
- `notification.sms.skipped.count` - Skipped (no opt-in/phone)
- `notification.sms.cost` - Estimated Twilio cost

**Technical Metrics:**
- `notification.batch.duration` - Batch processing time
- `notification.database.save.failures` - Database failures
- `notification.twilio.api.failures` - Twilio API failures

**Audit Metrics:**
- `notification.audit.gap.count` - SMS sent but audit failed (**CRITICAL**)
- `notification.duplicate.prevented.count` - Idempotency checks passed

### Dashboard Queries

**Daily SMS Volume:**
```sql
SELECT DATE(sent_at) as date, COUNT(*) as sent
FROM appointment_reminders_sent
WHERE status = 'SENT'
  AND sent_at >= NOW() - INTERVAL '30 days'
GROUP BY DATE(sent_at)
ORDER BY date DESC;
```

**Failure Rate:**
```sql
SELECT
    status,
    COUNT(*) as count,
    ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 2) as percentage
FROM appointment_reminders_sent
WHERE sent_at >= NOW() - INTERVAL '7 days'
GROUP BY status;
```

**Tenant Usage:**
```sql
SELECT
    tenant_id,
    COUNT(*) as total_reminders,
    SUM(CASE WHEN status = 'SENT' THEN 1 ELSE 0 END) as successful,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed
FROM appointment_reminders_sent
WHERE sent_at >= NOW() - INTERVAL '30 days'
GROUP BY tenant_id
ORDER BY total_reminders DESC;
```

## Troubleshooting

### SMS Not Sent to Patient

**Checklist:**
1. ✅ Feature flag enabled?
   ```bash
   curl http://localhost:8088/api/v1/feature-flags/tenant/{tenantId}/features/twilio-sms-reminders
   ```

2. ✅ Patient opted in? (`smsOptIn = true`)
   ```bash
   curl http://localhost:8084/api/v1/patients/{patientId}/contact \
     -H "X-Tenant-ID: {tenantId}"
   ```

3. ✅ Valid phone number? (E.164 format: `+1234567890`)

4. ✅ Reminder not already sent?
   ```sql
   SELECT * FROM appointment_reminders_sent
   WHERE tenant_id = 'tenant1'
     AND appointment_id = 'uuid'
     AND days_before = 1;
   ```

5. ✅ Twilio credentials valid?
   ```bash
   curl -X GET "https://api.twilio.com/2010-04-01/Accounts/{AccountSid}/Messages.json" \
     -u "{AccountSid}:{AuthToken}"
   ```

### Database Save Failures

**Symptoms:** `CRITICAL: Failed to record successful reminder` in logs

**Causes:**
- Database connection pool exhausted
- Transaction timeout
- Network issues

**Fix:**
```bash
# Check database connections
docker compose logs notification-service | grep -i "database"

# Verify HikariCP pool
curl http://localhost:8088/actuator/metrics/hikari.connections.active
```

### Twilio API Failures

**Symptoms:** `Failed to send SMS reminder` in logs

**Causes:**
- Invalid phone number format
- Twilio account suspended
- Rate limits exceeded

**Fix:**
```bash
# Check Twilio dashboard
# https://console.twilio.com/us1/monitor/logs/errors

# Verify phone number format (must be E.164)
echo "+1234567890" | grep -E '^\+[1-9]\d{1,14}$'
```

## Performance

### Batch Processing

**Current:** Single batch per tenant
**Recommended for large tenants (>1000 appointments/day):** Pagination

```java
// Paginate FHIR queries for large datasets
int pageSize = 100;
int pageNumber = 0;

while (true) {
    List<Appointment> appointments = fhirClient.getAppointments(
        tenantId, startTime, endTime, "booked", pageNumber, pageSize);

    if (appointments.isEmpty()) break;

    // Process batch
    appointments.forEach(apt -> sendReminder(tenantId, apt, daysBefore));

    pageNumber++;
}
```

### Twilio Rate Limits

| Tier | Rate Limit | Recommended Action |
|------|------------|-------------------|
| Free | 1 msg/sec | Use for testing only |
| Paid | 100+ msg/sec | Production use |

**For high-volume tenants:**
- Implement Resilience4j circuit breaker
- Add retry logic with exponential backoff
- Use Twilio Messaging Services (load balancing)

## Security

### Twilio Best Practices

- [x] Store credentials in environment variables (never commit)
- [x] Use HTTPS for Twilio API calls
- [x] Rotate auth tokens regularly (quarterly)
- [x] Enable Twilio geo-permissions (restrict to US)
- [x] Monitor Twilio usage dashboard for anomalies

### Access Control

**Feature Flag Management:**
- Only `ADMIN` role can enable/disable feature flags
- Tenant users cannot modify their own feature flags

**API Endpoints:**
- `POST /api/v1/notifications/appointment-reminders/process` - Requires `X-Tenant-ID` header
- Authenticated requests only (JWT token required)

## Related Documentation

- [Notification Service Overview](./README.md)
- [Feature Flags Service](../feature-flags-service/README.md)
- [FHIR Service](../fhir-service/README.md)
- [Patient Service](../patient-service/README.md)
- [HIPAA Compliance Guide](../../docs/HIPAA-CACHE-COMPLIANCE.md)

## Support

**Issues:** https://github.com/webemo-aaron/hdim/issues/304
**On-call:** PagerDuty rotation
**Twilio Support:** https://support.twilio.com

---

**Last Updated:** January 23, 2026
**Implementation:** Complete (Parts 1-4)
**Status:** ✅ Production Ready
