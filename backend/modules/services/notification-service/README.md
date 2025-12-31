# Notification Service

## Overview

The Notification Service provides multi-channel notification delivery for the HDIM platform, supporting email, SMS, push notifications, and in-app messaging. It handles template management, user preferences, and event-driven notification delivery via Kafka.

## Responsibilities

- Deliver notifications across multiple channels (Email, SMS, Push, In-App)
- Manage notification templates with variable substitution
- Store and enforce user notification preferences
- Route notifications to appropriate channels based on user preferences
- Process notification events from Kafka for async delivery
- Track notification delivery status and retry failed deliveries

## Technology Stack

| Component | Technology | Version | Why This Choice |
|-----------|------------|---------|-----------------|
| Runtime | Java | 21 LTS | Platform standard, virtual threads support |
| Framework | Spring Boot | 3.x | Ecosystem integration, async support |
| Database | PostgreSQL | 15 | Multi-tenant, ACID compliant |
| Messaging | Apache Kafka | 3.x | Event-driven notification triggers |
| Email | SMTP / SendGrid | - | Reliable email delivery |
| SMS | Twilio | - | Enterprise SMS provider |
| Push | Firebase Cloud Messaging | - | Cross-platform push notifications |

## API Endpoints

### Notifications

#### POST /api/v1/notifications/send
**Purpose**: Send a single notification
**Auth Required**: Yes (roles: ADMIN, SYSTEM)
**Request Headers**:
- `X-Tenant-ID` (required)
- `Authorization: Bearer <token>` (required)

**Request Body**:
```json
{
  "recipientId": "user-uuid",
  "templateCode": "CARE_GAP_ALERT",
  "channel": "EMAIL",
  "variables": {
    "patientName": "John Doe",
    "measureName": "Breast Cancer Screening"
  }
}
```

#### POST /api/v1/notifications/bulk
**Purpose**: Send bulk notifications
**Auth Required**: Yes (roles: ADMIN, SYSTEM)

#### GET /api/v1/notifications/{id}
**Purpose**: Get notification status
**Auth Required**: Yes (roles: ADMIN, EVALUATOR)

### Templates

#### GET /api/v1/templates
**Purpose**: List notification templates
**Auth Required**: Yes (roles: ADMIN)

#### POST /api/v1/templates
**Purpose**: Create notification template
**Auth Required**: Yes (roles: ADMIN)

#### PUT /api/v1/templates/{id}
**Purpose**: Update notification template
**Auth Required**: Yes (roles: ADMIN)

### Preferences

#### GET /api/v1/preferences/{userId}
**Purpose**: Get user notification preferences
**Auth Required**: Yes (roles: ADMIN, or self)

#### PUT /api/v1/preferences/{userId}
**Purpose**: Update user notification preferences
**Auth Required**: Yes (roles: ADMIN, or self)

## Database Schema

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| notifications | Notification records | id, tenant_id, recipient_id, channel, status, sent_at |
| notification_templates | Template definitions | id, tenant_id, code, channel, subject, body |
| notification_preferences | User preferences | id, tenant_id, user_id, channel, enabled |

## Kafka Topics

### Consumes

| Topic | Event Type | Handler |
|-------|------------|---------|
| notification.requests | NotificationRequestEvent | NotificationEventConsumer |
| care-gap.detected | CareGapDetectedEvent | Triggers care gap notifications |
| measure.evaluation.complete | MeasureEvaluationEvent | Triggers quality alerts |

### Publishes

| Topic | Event Type | Payload |
|-------|------------|---------|
| notification.sent | NotificationSentEvent | Notification delivery confirmation |
| notification.failed | NotificationFailedEvent | Delivery failure details |

## Notification Channels

| Channel | Provider | Configuration |
|---------|----------|---------------|
| EMAIL | SMTP / SendGrid | `notification.email.*` properties |
| SMS | Twilio | `notification.sms.twilio.*` properties |
| PUSH | Firebase | `notification.push.firebase.*` properties |
| IN_APP | Internal | Stored in database for portal display |

## Configuration

```yaml
# application.yml
server:
  port: 8104

spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/healthdata_notification

notification:
  email:
    enabled: true
    from: noreply@healthdatainmotion.com
    smtp:
      host: smtp.sendgrid.net
      port: 587
  sms:
    twilio:
      enabled: true
      account-sid: ${TWILIO_ACCOUNT_SID}
      auth-token: ${TWILIO_AUTH_TOKEN}
      from-number: ${TWILIO_FROM_NUMBER}
  push:
    firebase:
      enabled: true
      credentials-path: ${FIREBASE_CREDENTIALS_PATH}
```

## Testing

```bash
# Unit tests
./gradlew :modules:services:notification-service:test

# Integration tests
./gradlew :modules:services:notification-service:integrationTest
```

## Monitoring

- **Health Check**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`
- **Prometheus**: `GET /actuator/prometheus`

### Key Metrics

| Metric | Description |
|--------|-------------|
| `notifications.sent.total` | Total notifications sent by channel |
| `notifications.failed.total` | Failed notification attempts |
| `notifications.delivery.time` | Delivery latency histogram |

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Email not sending | SMTP credentials invalid | Verify `notification.email.smtp.*` config |
| SMS delivery failure | Twilio quota exceeded | Check Twilio dashboard, upgrade plan |
| Kafka consumer lag | High notification volume | Scale service instances |

## Security Considerations

- **No PHI in notifications**: Templates should not include PHI directly
- **Audit logging**: All notification sends are logged
- **Multi-tenant isolation**: All queries filtered by tenantId
- **Credential security**: Provider credentials stored in Vault

## References

- [Notification Events Schema](../../shared/api-contracts/notification-events.md)
- [Kafka Configuration](../../shared/infrastructure/messaging/README.md)
- [Gateway Trust Architecture](../../../docs/GATEWAY_TRUST_ARCHITECTURE.md)

---

*Last Updated: December 2025*
*Service Version: 1.0*
