# Feature Flags Infrastructure

Tenant-based feature flag system for enabling/disabling integrations and features per customer.

## Overview

The Feature Flags infrastructure provides:
- **Tenant-level feature control** - Enable/disable features per customer
- **Configuration storage** - Store feature-specific config as JSON
- **AOP-based enforcement** - @FeatureFlag annotation for method-level control
- **Redis caching** - 5-minute cache TTL for performance
- **REST API** - Admin endpoints for feature flag management
- **Multi-tenant isolation** - HIPAA-compliant tenant isolation

## Supported Features

| Feature Key | Description | Configuration Fields |
|-------------|-------------|----------------------|
| `twilio-sms-reminders` | Twilio SMS appointment reminders | `reminder_days`, `default_from_number` |
| `smart-on-fhir` | SMART on FHIR embedding | `client_id`, `launch_url` |
| `cds-hooks` | CDS Hooks integration | `discovery_url`, `enabled_hooks` |
| `nowpow-sdoh` | NowPow SDOH integration | `api_key`, `region` |
| `validic-rpm` | Validic RPM integration | `org_id`, `api_key` |

## Usage

### 1. Add Dependency

In your service's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":modules:shared:infrastructure:feature-flags"))
}
```

### 2. Enable Feature Flags

In your Spring Boot application class:

```java
@SpringBootApplication
@EnableFeatureFlags
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
```

### 3. Use @FeatureFlag Annotation

```java
@Service
@RequiredArgsConstructor
public class AppointmentReminderService {

    @FeatureFlag("twilio-sms-reminders")
    public void sendSmsReminder(
            @RequestHeader("X-Tenant-ID") String tenantId,
            String patientId,
            String appointmentId) {
        // Send SMS reminder
        // Only executed if feature is enabled for tenant
    }
}
```

### 4. Programmatic Check

```java
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final TenantFeatureFlagService featureFlagService;

    public void sendNotification(String tenantId, String message) {
        if (featureFlagService.isFeatureEnabled(tenantId, "twilio-sms-reminders")) {
            // Send SMS via Twilio
        } else {
            // Send in-app notification
        }
    }
}
```

### 5. Get Feature Configuration

```java
Map<String, Object> config = featureFlagService.getFeatureConfig(tenantId, "twilio-sms-reminders");
List<Integer> reminderDays = (List<Integer>) config.get("reminder_days");
String fromNumber = (String) config.get("default_from_number");
```

## REST API

### Get All Feature Flags

```bash
GET /api/v1/tenant-features
Headers:
  X-Tenant-ID: tenant1
  Authorization: Bearer <token>
```

Response:
```json
[
  {
    "featureKey": "twilio-sms-reminders",
    "enabled": true,
    "config": {
      "reminder_days": [1, 3, 7],
      "default_from_number": "+11234567890"
    },
    "createdAt": "2026-01-23T10:00:00Z",
    "updatedAt": "2026-01-23T10:00:00Z"
  }
]
```

### Get Single Feature Flag

```bash
GET /api/v1/tenant-features/twilio-sms-reminders
Headers:
  X-Tenant-ID: tenant1
  Authorization: Bearer <token>
```

### Enable Feature

```bash
PUT /api/v1/tenant-features/twilio-sms-reminders/enable
Headers:
  X-Tenant-ID: tenant1
  X-User-ID: admin-user
  Authorization: Bearer <token>
Content-Type: application/json

{
  "config": {
    "reminder_days": [1, 3, 7],
    "default_from_number": "+11234567890"
  }
}
```

### Disable Feature

```bash
PUT /api/v1/tenant-features/twilio-sms-reminders/disable
Headers:
  X-Tenant-ID: tenant1
  X-User-ID: admin-user
  Authorization: Bearer <token>
```

### Update Configuration

```bash
PUT /api/v1/tenant-features/twilio-sms-reminders/config
Headers:
  X-Tenant-ID: tenant1
  X-User-ID: admin-user
  Authorization: Bearer <token>
Content-Type: application/json

{
  "config": {
    "reminder_days": [1, 7],
    "default_from_number": "+11234567890"
  }
}
```

## Database Schema

```sql
CREATE TABLE tenant_feature_flags (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    feature_key VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT false,
    config_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT uk_tenant_feature_flags_tenant_feature UNIQUE (tenant_id, feature_key)
);
```

## Tenant Extraction

The @FeatureFlag annotation automatically extracts tenant ID using:

1. **@RequestHeader("X-Tenant-ID")** parameter
2. **SecurityContextHolder** (principal.getTenantId())

Example with RequestHeader:
```java
@FeatureFlag("twilio-sms-reminders")
public void sendReminder(@RequestHeader("X-Tenant-ID") String tenantId, ...) {
    // ...
}
```

Example with SecurityContext:
```java
@FeatureFlag("twilio-sms-reminders")
public void sendReminder() {
    // Tenant ID extracted from SecurityContextHolder
}
```

## Fail Silently Option

By default, @FeatureFlag throws `FeatureFlagDisabledException` if the feature is disabled.
Use `failSilently=true` to return null instead:

```java
@FeatureFlag(value = "twilio-sms-reminders", failSilently = true)
public void sendReminder(...) {
    // Returns null if feature disabled, doesn't throw exception
}
```

## Caching

Feature flag checks are cached in Redis for 5 minutes:
- Cache key: `{tenantId}:{featureKey}`
- TTL: 300 seconds (5 minutes)
- Cache names: `tenant-feature-flags`, `tenant-feature-config`

Cache is automatically evicted when:
- Feature is enabled/disabled
- Configuration is updated

## Scheduled Jobs

To find all tenants with a feature enabled (useful for scheduled jobs):

```java
@Service
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private final TenantFeatureFlagService featureFlagService;

    @Scheduled(cron = "0 0 9 * * *") // Daily at 9 AM
    public void sendReminders() {
        List<String> tenants = featureFlagService.findTenantsWithFeatureEnabled("twilio-sms-reminders");

        for (String tenantId : tenants) {
            // Send reminders for tenant
        }
    }
}
```

## HIPAA Compliance

- **Multi-tenant isolation**: All queries filter by tenant_id
- **Audit trail**: created_at, updated_at, created_by, updated_by
- **Non-PHI data**: Feature flags are configuration data, not PHI
- **Cache TTL**: 5 minutes (acceptable for non-PHI)

## Security

- **ADMIN role required**: Only admins can enable/disable features
- **ADMIN, EVALUATOR, ANALYST**: Can view feature flags
- **Audit logging**: All changes tracked with user ID

## Testing

Unit tests with Mockito:
```java
@Test
void shouldEnableFeature_WhenAdminUser() {
    // Test feature flag service
}
```

Integration tests with Testcontainers:
```java
@Test
@Testcontainers
void shouldEnableFeature_EndToEnd() {
    // Test with real PostgreSQL
}
```

## Example: Twilio SMS Integration

```java
@Service
@RequiredArgsConstructor
public class TwilioAppointmentReminderService {

    private final TenantFeatureFlagService featureFlagService;
    private final TwilioSmsProvider twilioSmsProvider;

    @FeatureFlag("twilio-sms-reminders")
    public void sendReminder(
            @RequestHeader("X-Tenant-ID") String tenantId,
            String patientPhone,
            String appointmentTime) {

        // Get tenant-specific configuration
        Map<String, Object> config = featureFlagService.getFeatureConfig(tenantId, "twilio-sms-reminders");
        String fromNumber = (String) config.get("default_from_number");

        // Send SMS
        String message = String.format("Appointment reminder: %s", appointmentTime);
        twilioSmsProvider.send(patientPhone, message);
    }
}
```

## Migration from Existing Services

If a service already has feature-specific configuration:

1. **Keep existing config for defaults**
2. **Override with tenant-specific flags**
3. **Gradual migration**

Example:
```java
// Old: application.properties
twilio.enabled=true
twilio.from-number=+11234567890

// New: Per-tenant via feature flags
PUT /api/v1/tenant-features/twilio-sms-reminders/enable
{
  "config": {
    "default_from_number": "+19876543210"
  }
}
```

## Adding New Features

1. **Add default flag in migration** (`0001-insert-default-feature-flags`)
2. **Update README** (this file) with feature key and config fields
3. **Use @FeatureFlag annotation** in your service methods
4. **Test with feature enabled/disabled**

Example migration:
```xml
<insert tableName="tenant_feature_flags">
    <column name="id" valueComputed="gen_random_uuid()"/>
    <column name="tenant_id" value="default"/>
    <column name="feature_key" value="new-feature"/>
    <column name="enabled" valueBoolean="false"/>
    <column name="config_json" value='{"key": "value"}'/>
    <column name="created_by" value="system"/>
    <column name="updated_by" value="system"/>
</insert>
```

## Troubleshooting

### Feature flag not working

1. Check `@EnableFeatureFlags` is present
2. Verify database migration ran
3. Check cache (clear if needed)
4. Verify tenant ID extraction

### Performance issues

1. Check Redis cache is enabled
2. Monitor cache hit rate
3. Increase cache size if needed

### Database locked

1. Check Liquibase migrations completed
2. Verify unique constraint on (tenant_id, feature_key)

---

**Last Updated**: January 23, 2026
**Version**: 1.0.0
**Author**: Claude Code
