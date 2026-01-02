# Notification Recipient Resolution Implementation

## Overview

This document describes the implementation of database-driven notification recipient resolution for the quality measure service. The implementation replaces hardcoded email/phone numbers with dynamic lookups based on patient care team assignments and user notification preferences.

## Components Created

### 1. Domain Entities

#### CareTeamMemberEntity
**Location:** `/src/main/java/com/healthdata/quality/persistence/CareTeamMemberEntity.java`

Represents a member of a patient's care team with the following key fields:
- `patientId`: Patient identifier
- `userId`: Care team member user identifier
- `role`: Care team role (PRIMARY_CARE_PHYSICIAN, NURSE_PRACTITIONER, etc.)
- `isPrimary`: Flag indicating primary care provider
- `active`: Active status flag
- `startDate` / `endDate`: Membership period

**Business Logic:**
- `isCurrentlyActive()`: Checks if member is active within the current date range

#### NotificationPreferenceEntity
**Location:** `/src/main/java/com/healthdata/quality/persistence/NotificationPreferenceEntity.java`

Already existed. Contains user notification preferences:
- Channel preferences (EMAIL, SMS, PUSH, IN_APP)
- Contact information (email address, phone number)
- Quiet hours settings
- Severity threshold
- HIPAA consent tracking

**Business Logic:**
- `isWithinQuietHours()`: Checks if current time is within quiet hours
- `shouldReceive()`: Determines if notification should be sent based on preferences

### 2. Repositories

#### CareTeamMemberRepository
**Location:** `/src/main/java/com/healthdata/quality/persistence/CareTeamMemberRepository.java`

Query methods:
- `findActiveByPatientIdAndTenantId()`: Get active care team for a patient
- `findPrimaryByPatientIdAndTenantId()`: Get primary care provider
- `findActiveByPatientIdAndTenantIdAndRole()`: Filter by role
- `countActiveByPatientIdAndTenantId()`: Count team members

#### NotificationPreferenceRepository
**Location:** `/src/main/java/com/healthdata/quality/persistence/NotificationPreferenceRepository.java`

Query methods:
- `findByUserIdAndTenantId()`: Get preferences for a user
- `findByUserIdsAndTenantId()`: Batch lookup for multiple users
- `findEmailEnabledByTenantId()`: Users with email enabled
- `findSmsEnabledByTenantId()`: Users with SMS enabled
- `findConsentedByTenantId()`: Users who have given consent

### 3. Models

#### NotificationRecipient
**Location:** `/src/main/java/com/healthdata/quality/model/NotificationRecipient.java`

Value object representing a resolved notification recipient:
- User ID and contact information
- Enabled notification channels
- Care team role and primary status
- Severity threshold

**Business Logic:**
- `supportsChannel()`: Check if channel is enabled
- `meetsThreshold()`: Check severity threshold
- `getContactForChannel()`: Get appropriate contact info for channel

### 4. Services

#### RecipientResolutionService
**Location:** `/src/main/java/com/healthdata/quality/service/notification/RecipientResolutionService.java`

Core service that orchestrates recipient resolution:

**Key Methods:**
- `resolveRecipients()`: Main method that:
  1. Queries patient's care team members
  2. Fetches user notification preferences
  3. Filters by channel support
  4. Respects quiet hours and severity thresholds
  5. Validates contact information
  6. Sorts with primary providers first

- `resolveRecipientsForAllChannels()`: Resolves for all channels at once
- `getPrimaryCareProvider()`: Gets the primary care provider specifically

**Resolution Logic:**
1. Query active care team members for the patient
2. Extract user IDs from care team
3. Fetch notification preferences for those users
4. Apply filters:
   - Channel enabled in preferences
   - HIPAA consent given
   - Severity meets threshold
   - Not in quiet hours (unless critical override)
   - Valid contact information present
5. Sort results (primary providers first)

## Updated Notification Triggers

### ClinicalAlertNotificationTrigger
**Changes:**
- Removed hardcoded `DEFAULT_EMAIL` and `DEFAULT_PHONE` constants
- Injected `RecipientResolutionService`
- Updated `getRecipients()` method to:
  - Call `recipientResolutionService.resolveRecipients()` for each channel
  - Prefer primary care provider's contact information
  - Fall back to first recipient if no primary

### CareGapNotificationTrigger
**Changes:** Same pattern as ClinicalAlertNotificationTrigger

### HealthScoreNotificationTrigger
**Changes:** Same pattern as ClinicalAlertNotificationTrigger

## Database Schema

### care_team_members Table

```sql
CREATE TABLE care_team_members (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    patient_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    UNIQUE (patient_id, user_id, tenant_id)
);

CREATE INDEX idx_care_team_patient ON care_team_members(patient_id);
CREATE INDEX idx_care_team_user ON care_team_members(user_id);
CREATE INDEX idx_care_team_tenant ON care_team_members(tenant_id);
CREATE INDEX idx_care_team_active ON care_team_members(active);
```

**Migration File:** `0016-create-care-team-members-table.xml`

## Tests

All components were developed using Test-Driven Development (TDD):

### Repository Tests
1. **NotificationPreferenceRepositoryTest** (11 tests)
   - CRUD operations
   - Query by user IDs
   - Filter by enabled channels
   - Consent checking
   - Quiet hours logic
   - Preference evaluation

2. **CareTeamMemberRepositoryTest** (8 tests)
   - CRUD operations
   - Active member queries
   - Primary provider lookup
   - Role filtering
   - Membership status checks

### Service Tests
3. **RecipientResolutionServiceTest** (11 tests)
   - Basic recipient resolution
   - Channel filtering
   - Quiet hours respect
   - Critical alert override
   - Severity threshold filtering
   - Consent requirement
   - Empty result handling
   - Primary provider prioritization

### Integration Tests
4. **ClinicalAlertNotificationTriggerTest** (6 tests)
   - Critical alert with resolved recipients
   - No recipients handling
   - Primary care provider preference
   - Acknowledgment notifications
   - Severity-based filtering

5. **CareGapNotificationTriggerTest** (4 tests)
   - High priority gap notifications
   - Low priority filtering
   - Addressed gap notifications
   - Multiple recipient handling

6. **HealthScoreNotificationTriggerTest** (7 tests)
   - Significant score changes
   - Minor change filtering
   - First calculation handling
   - Threshold crossing
   - Primary provider preference
   - Manual refresh
   - No recipients handling

**Total Tests:** 47 tests

## Usage Example

```java
// Resolve recipients for a high-severity email notification
List<NotificationRecipient> recipients = recipientResolutionService.resolveRecipients(
    "tenant-123",
    "patient-456",
    NotificationEntity.NotificationChannel.EMAIL,
    NotificationEntity.NotificationSeverity.HIGH
);

// Recipients are:
// - Active care team members only
// - With email enabled in preferences
// - Who have given HIPAA consent
// - Meeting severity threshold
// - Not in quiet hours (or critical override applies)
// - Sorted with primary providers first

for (NotificationRecipient recipient : recipients) {
    String email = recipient.getEmailAddress();
    // Send notification to email
}
```

## Key Features

1. **Database-Driven**: Recipients resolved from database, not hardcoded
2. **Care Team Integration**: Uses actual patient care team assignments
3. **User Preferences**: Respects individual notification preferences
4. **Quiet Hours**: Supports quiet hours with critical alert override
5. **Severity Filtering**: Users can set minimum severity thresholds
6. **Channel Support**: Filters by supported notification channels
7. **HIPAA Compliant**: Requires explicit consent
8. **Primary Provider Priority**: Prioritizes primary care providers
9. **Tenant Isolation**: Multi-tenant aware
10. **Testable**: Comprehensive test coverage with TDD approach

## Configuration

No additional configuration required. The service automatically:
- Discovers care team members via repository queries
- Applies user preferences from the database
- Respects system-wide notification rules

## Migration Path

1. Deploy new entities and repositories
2. Run database migration (0016-create-care-team-members-table.xml)
3. Populate care team data for existing patients
4. Configure user notification preferences
5. Updated notification triggers automatically use new resolution service

## Future Enhancements

Potential improvements:
1. **On-Call Escalation**: Escalate critical alerts to on-call providers
2. **Role-Based Routing**: Route notifications based on alert type and role
3. **Smart Scheduling**: Consider provider schedules and availability
4. **Fallback Recipients**: Configurable fallback when no recipients found
5. **Delivery Preferences**: Per-user delivery time preferences
6. **Multi-Language**: Support for preferred language in notifications
7. **Priority Levels**: Care team members with priority levels
8. **Temporary Assignments**: Support for temporary care team coverage
