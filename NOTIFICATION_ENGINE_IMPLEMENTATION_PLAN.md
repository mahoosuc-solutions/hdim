# Production-Ready Notification Engine Implementation Plan

## Executive Summary

This document provides a comprehensive, step-by-step implementation guide to complete the production-ready notification engine for HealthData in Motion. The plan builds on existing architecture and entities to deliver a HIPAA-compliant, multi-channel notification system with enterprise-grade reliability.

**Timeline**: 8-10 weeks (5 phases)
**Team Size**: 2-3 developers
**Complexity**: Medium-High

---

## Current State Assessment

### What's Already Done ✅
- Architecture design complete (`NOTIFICATION_ENGINE_ARCHITECTURE.md`)
- Core entities implemented:
  - `NotificationEntity.java` - Full tracking with audit trail
  - `NotificationPreferenceEntity.java` - User preference management with HIPAA consent
- Basic notification channels:
  - `EmailNotificationChannel.java` - Basic JavaMailSender integration
  - `SmsNotificationChannel.java` - Mock SMS implementation
  - `NotificationService.java` - Multi-channel routing by severity
- Database schema designed (needs migration scripts)
- WebSocket integration for real-time alerts

### What's Missing ❌
1. **Template System** - No HTML/responsive templates, no rendering engine
2. **Provider Integrations** - No SendGrid, AWS SES, Twilio integration
3. **Delivery Pipeline** - No async queue, retry logic, or rate limiting
4. **User Preferences API** - No REST endpoints for preference management
5. **Monitoring & Analytics** - No metrics, dashboards, or delivery tracking
6. **Repositories** - Missing `NotificationRepository` and `NotificationPreferenceRepository`
7. **Database Migrations** - No Liquibase changesets for notification tables
8. **HIPAA Compliance** - PHI encryption, audit logs, consent workflows incomplete

---

## Implementation Phases

## PHASE 1: Foundation & Data Layer (Week 1-2)

### Goals
- Set up data persistence layer
- Create database migrations
- Implement repositories with query methods
- Add configuration infrastructure

### Tasks

#### 1.1 Database Migrations

**File**: `/backend/modules/services/quality-measure-service/src/main/resources/db/changelog/0014-create-notifications-table.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.9.xsd">

    <changeSet id="0014-create-notifications-table" author="notification-system">
        <createTable tableName="notifications">
            <column name="id" type="UUID">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="tenant_id" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="patient_id" type="VARCHAR(255)"/>
            <column name="user_id" type="VARCHAR(255)"/>
            <column name="channel" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="notification_type" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="severity" type="VARCHAR(20)"/>
            <column name="template_id" type="VARCHAR(255)"/>
            <column name="subject" type="VARCHAR(500)"/>
            <column name="message" type="TEXT"/>
            <column name="recipient" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="status" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="provider" type="VARCHAR(100)"/>
            <column name="provider_message_id" type="VARCHAR(255)"/>
            <column name="metadata" type="JSONB"/>
            <column name="error_message" type="TEXT"/>
            <column name="retry_count" type="INTEGER" defaultValue="0"/>
            <column name="max_retries" type="INTEGER" defaultValue="3"/>
            <column name="created_at" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="sent_at" type="TIMESTAMP"/>
            <column name="delivered_at" type="TIMESTAMP"/>
            <column name="failed_at" type="TIMESTAMP"/>
            <column name="next_retry_at" type="TIMESTAMP"/>
            <column name="expires_at" type="TIMESTAMP"/>
        </createTable>

        <!-- Indexes for performance -->
        <createIndex tableName="notifications" indexName="idx_notifications_patient">
            <column name="patient_id"/>
        </createIndex>
        <createIndex tableName="notifications" indexName="idx_notifications_user">
            <column name="user_id"/>
        </createIndex>
        <createIndex tableName="notifications" indexName="idx_notifications_status">
            <column name="status"/>
        </createIndex>
        <createIndex tableName="notifications" indexName="idx_notifications_channel">
            <column name="channel"/>
        </createIndex>
        <createIndex tableName="notifications" indexName="idx_notifications_created">
            <column name="created_at"/>
        </createIndex>
        <createIndex tableName="notifications" indexName="idx_notifications_tenant">
            <column name="tenant_id"/>
        </createIndex>
        <createIndex tableName="notifications" indexName="idx_notifications_next_retry">
            <column name="next_retry_at"/>
            <column name="status"/>
        </createIndex>

        <!-- Composite index for retry queue queries -->
        <createIndex tableName="notifications" indexName="idx_notifications_retry_queue">
            <column name="status"/>
            <column name="next_retry_at"/>
            <column name="tenant_id"/>
        </createIndex>

        <!-- GIN index for JSONB metadata queries -->
        <sql>
            CREATE INDEX idx_notifications_metadata_gin ON notifications USING GIN (metadata);
        </sql>
    </changeSet>

    <!-- Add row-level security for multi-tenancy -->
    <changeSet id="0014-notifications-rls" author="notification-system">
        <sql>
            ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
            
            CREATE POLICY notifications_tenant_isolation ON notifications
                USING (tenant_id = current_setting('app.current_tenant_id', TRUE));
        </sql>
    </changeSet>

    <!-- Add data retention policy -->
    <changeSet id="0014-notifications-retention" author="notification-system">
        <sql>
            -- Index for cleanup queries
            CREATE INDEX idx_notifications_cleanup ON notifications(status, delivered_at, failed_at)
            WHERE status IN ('DELIVERED', 'FAILED', 'BOUNCED', 'REJECTED');
        </sql>
        
        <comment>
            Retention Policy (implement in scheduled job):
            - DELIVERED: Keep 90 days
            - FAILED/BOUNCED/REJECTED: Keep 7 days
            - PENDING/SENDING: Never auto-delete (manual investigation required)
        </comment>
    </changeSet>
</databaseChangeLog>
```

**File**: `/backend/modules/services/quality-measure-service/src/main/resources/db/changelog/0015-create-notification-preferences-table.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.9.xsd">

    <changeSet id="0015-create-notification-preferences-table" author="notification-system">
        <createTable tableName="notification_preferences">
            <column name="id" type="UUID">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="tenant_id" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="user_id" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            
            <!-- Channel Preferences -->
            <column name="email_enabled" type="BOOLEAN" defaultValueBoolean="true"/>
            <column name="sms_enabled" type="BOOLEAN" defaultValueBoolean="false"/>
            <column name="push_enabled" type="BOOLEAN" defaultValueBoolean="true"/>
            <column name="in_app_enabled" type="BOOLEAN" defaultValueBoolean="true"/>
            
            <!-- Contact Information -->
            <column name="email_address" type="VARCHAR(255)"/>
            <column name="phone_number" type="VARCHAR(50)"/>
            <column name="push_token" type="TEXT"/>
            
            <!-- Notification Type Preferences -->
            <column name="enabled_types" type="JSONB"/>
            <column name="severity_threshold" type="VARCHAR(20)" defaultValue="MEDIUM"/>
            
            <!-- Quiet Hours -->
            <column name="quiet_hours_enabled" type="BOOLEAN" defaultValueBoolean="false"/>
            <column name="quiet_hours_start" type="TIME"/>
            <column name="quiet_hours_end" type="TIME"/>
            <column name="quiet_hours_override_critical" type="BOOLEAN" defaultValueBoolean="true"/>
            
            <!-- Digest Mode -->
            <column name="digest_mode_enabled" type="BOOLEAN" defaultValueBoolean="false"/>
            <column name="digest_frequency" type="VARCHAR(20)" defaultValue="DAILY"/>
            
            <!-- Custom Settings -->
            <column name="custom_settings" type="JSONB"/>
            
            <!-- HIPAA Compliance -->
            <column name="consent_given" type="BOOLEAN" defaultValueBoolean="false"/>
            <column name="consent_date" type="TIMESTAMP"/>
            
            <!-- Audit Fields -->
            <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
            <column name="updated_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
        </createTable>

        <!-- Unique constraint for user + tenant -->
        <addUniqueConstraint
            tableName="notification_preferences"
            columnNames="user_id, tenant_id"
            constraintName="uk_notification_pref_user_tenant"/>

        <!-- Indexes -->
        <createIndex tableName="notification_preferences" indexName="idx_notification_pref_user">
            <column name="user_id"/>
        </createIndex>
        <createIndex tableName="notification_preferences" indexName="idx_notification_pref_tenant">
            <column name="tenant_id"/>
        </createIndex>
        
        <!-- GIN indexes for JSONB columns -->
        <sql>
            CREATE INDEX idx_notification_pref_enabled_types_gin 
            ON notification_preferences USING GIN (enabled_types);
            
            CREATE INDEX idx_notification_pref_custom_settings_gin 
            ON notification_preferences USING GIN (custom_settings);
        </sql>
    </changeSet>

    <!-- Row-level security -->
    <changeSet id="0015-notification-preferences-rls" author="notification-system">
        <sql>
            ALTER TABLE notification_preferences ENABLE ROW LEVEL SECURITY;
            
            CREATE POLICY notification_preferences_tenant_isolation 
            ON notification_preferences
            USING (tenant_id = current_setting('app.current_tenant_id', TRUE));
        </sql>
    </changeSet>
</databaseChangeLog>
```

**File**: `/backend/modules/services/quality-measure-service/src/main/resources/db/changelog/0016-create-notification-templates-table.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.9.xsd">

    <changeSet id="0016-create-notification-templates-table" author="notification-system">
        <createTable tableName="notification_templates">
            <column name="id" type="UUID">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="template_key" type="VARCHAR(100)">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="name" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="description" type="TEXT"/>
            <column name="channel" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="notification_type" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            
            <!-- Template Content -->
            <column name="subject_template" type="TEXT"/>
            <column name="html_template" type="TEXT"/>
            <column name="text_template" type="TEXT"/>
            <column name="sms_template" type="VARCHAR(160)"/>
            
            <!-- Version Control -->
            <column name="version" type="INTEGER" defaultValue="1"/>
            <column name="is_active" type="BOOLEAN" defaultValueBoolean="true"/>
            
            <!-- Metadata -->
            <column name="variables" type="JSONB"/>
            <column name="settings" type="JSONB"/>
            
            <!-- Audit -->
            <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
            <column name="updated_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
            <column name="created_by" type="VARCHAR(255)"/>
        </createTable>

        <createIndex tableName="notification_templates" indexName="idx_template_key">
            <column name="template_key"/>
        </createIndex>
        <createIndex tableName="notification_templates" indexName="idx_template_channel">
            <column name="channel"/>
        </createIndex>
        <createIndex tableName="notification_templates" indexName="idx_template_type">
            <column name="notification_type"/>
        </createIndex>
        <createIndex tableName="notification_templates" indexName="idx_template_active">
            <column name="is_active"/>
        </createIndex>
    </changeSet>

    <!-- Insert default templates -->
    <changeSet id="0016-insert-default-templates" author="notification-system">
        <comment>Default notification templates for common scenarios</comment>
        
        <insert tableName="notification_templates">
            <column name="id" valueComputed="gen_random_uuid()"/>
            <column name="template_key" value="critical-alert-email"/>
            <column name="name" value="Critical Clinical Alert - Email"/>
            <column name="description" value="Email template for critical severity clinical alerts"/>
            <column name="channel" value="EMAIL"/>
            <column name="notification_type" value="CLINICAL_ALERT"/>
            <column name="subject_template" value="[URGENT] {{alert.title}} - Patient {{patient.name}}"/>
            <column name="text_template" value="CRITICAL ALERT: {{alert.message}}. Patient: {{patient.name}} (MRN: {{patient.mrn}}). Action required immediately."/>
            <column name="version" valueNumeric="1"/>
        </insert>

        <insert tableName="notification_templates">
            <column name="id" valueComputed="gen_random_uuid()"/>
            <column name="template_key" value="critical-alert-sms"/>
            <column name="name" value="Critical Clinical Alert - SMS"/>
            <column name="description" value="SMS template for critical alerts (160 char limit)"/>
            <column name="channel" value="SMS"/>
            <column name="notification_type" value="CLINICAL_ALERT"/>
            <column name="sms_template" value="URGENT: {{alert.title}} - {{patient.name}} ({{patient.mrn}}). Review immediately."/>
            <column name="version" valueNumeric="1"/>
        </insert>
    </changeSet>
</databaseChangeLog>
```

**Update master changelog**: Add these to `db.changelog-master.xml`:
```xml
<include file="db/changelog/0014-create-notifications-table.xml"/>
<include file="db/changelog/0015-create-notification-preferences-table.xml"/>
<include file="db/changelog/0016-create-notification-templates-table.xml"/>
```

#### 1.2 Repository Interfaces

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/persistence/NotificationRepository.java`

```java
package com.healthdata.quality.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Notification entities
 * 
 * Provides queries for notification tracking, retry management, and analytics
 */
@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {

    // ========== Basic Queries ==========
    
    /**
     * Find notifications by patient ID
     */
    List<NotificationEntity> findByPatientIdAndTenantId(String patientId, String tenantId);
    
    /**
     * Find notifications by user ID
     */
    Page<NotificationEntity> findByUserIdAndTenantId(String userId, String tenantId, Pageable pageable);
    
    /**
     * Find notifications by status
     */
    List<NotificationEntity> findByStatusAndTenantId(
        NotificationEntity.NotificationStatus status, 
        String tenantId
    );
    
    /**
     * Find notifications by channel
     */
    List<NotificationEntity> findByChannelAndTenantId(
        NotificationEntity.NotificationChannel channel, 
        String tenantId
    );

    // ========== Retry Queue Management ==========
    
    /**
     * Find notifications ready for retry
     * 
     * Finds FAILED notifications where:
     * - nextRetryAt is in the past (or null)
     * - retryCount < maxRetries
     * - Not expired
     */
    @Query("""
        SELECT n FROM NotificationEntity n 
        WHERE n.tenantId = :tenantId
        AND n.status = 'FAILED'
        AND n.retryCount < n.maxRetries
        AND (n.nextRetryAt IS NULL OR n.nextRetryAt <= :now)
        AND (n.expiresAt IS NULL OR n.expiresAt > :now)
        ORDER BY n.createdAt ASC
        """)
    List<NotificationEntity> findPendingRetries(
        @Param("tenantId") String tenantId,
        @Param("now") LocalDateTime now,
        Pageable pageable
    );
    
    /**
     * Find expired notifications that need to be marked as EXPIRED
     */
    @Query("""
        SELECT n FROM NotificationEntity n
        WHERE n.tenantId = :tenantId
        AND n.status IN ('PENDING', 'SENDING', 'FAILED')
        AND n.expiresAt IS NOT NULL
        AND n.expiresAt <= :now
        """)
    List<NotificationEntity> findExpiredNotifications(
        @Param("tenantId") String tenantId,
        @Param("now") LocalDateTime now
    );

    // ========== Analytics & Monitoring ==========
    
    /**
     * Count notifications by status for dashboard
     */
    @Query("""
        SELECT n.status, COUNT(n) 
        FROM NotificationEntity n 
        WHERE n.tenantId = :tenantId
        AND n.createdAt >= :since
        GROUP BY n.status
        """)
    List<Object[]> countByStatusSince(
        @Param("tenantId") String tenantId,
        @Param("since") LocalDateTime since
    );
    
    /**
     * Count notifications by channel
     */
    @Query("""
        SELECT n.channel, COUNT(n), n.status
        FROM NotificationEntity n 
        WHERE n.tenantId = :tenantId
        AND n.createdAt >= :since
        GROUP BY n.channel, n.status
        """)
    List<Object[]> countByChannelAndStatusSince(
        @Param("tenantId") String tenantId,
        @Param("since") LocalDateTime since
    );
    
    /**
     * Calculate delivery success rate
     */
    @Query("""
        SELECT 
            COUNT(CASE WHEN n.status = 'DELIVERED' THEN 1 END) * 100.0 / COUNT(n)
        FROM NotificationEntity n
        WHERE n.tenantId = :tenantId
        AND n.channel = :channel
        AND n.createdAt >= :since
        """)
    Double calculateDeliveryRate(
        @Param("tenantId") String tenantId,
        @Param("channel") NotificationEntity.NotificationChannel channel,
        @Param("since") LocalDateTime since
    );
    
    /**
     * Get average delivery time (sent to delivered)
     */
    @Query("""
        SELECT AVG(EXTRACT(EPOCH FROM (n.deliveredAt - n.sentAt)))
        FROM NotificationEntity n
        WHERE n.tenantId = :tenantId
        AND n.status = 'DELIVERED'
        AND n.channel = :channel
        AND n.createdAt >= :since
        """)
    Double calculateAverageDeliveryTimeSeconds(
        @Param("tenantId") String tenantId,
        @Param("channel") NotificationEntity.NotificationChannel channel,
        @Param("since") LocalDateTime since
    );

    // ========== Provider Performance ==========
    
    /**
     * Count by provider for performance comparison
     */
    @Query("""
        SELECT n.provider, COUNT(n), n.status
        FROM NotificationEntity n
        WHERE n.tenantId = :tenantId
        AND n.createdAt >= :since
        GROUP BY n.provider, n.status
        """)
    List<Object[]> countByProviderAndStatus(
        @Param("tenantId") String tenantId,
        @Param("since") LocalDateTime since
    );

    // ========== Data Cleanup ==========
    
    /**
     * Delete old delivered notifications (retention policy)
     */
    @Modifying
    @Query("""
        DELETE FROM NotificationEntity n
        WHERE n.tenantId = :tenantId
        AND n.status = 'DELIVERED'
        AND n.deliveredAt < :cutoffDate
        """)
    int deleteOldDeliveredNotifications(
        @Param("tenantId") String tenantId,
        @Param("cutoffDate") LocalDateTime cutoffDate
    );
    
    /**
     * Delete old failed notifications (shorter retention)
     */
    @Modifying
    @Query("""
        DELETE FROM NotificationEntity n
        WHERE n.tenantId = :tenantId
        AND n.status IN ('FAILED', 'BOUNCED', 'REJECTED')
        AND n.failedAt < :cutoffDate
        """)
    int deleteOldFailedNotifications(
        @Param("tenantId") String tenantId,
        @Param("cutoffDate") LocalDateTime cutoffDate
    );

    // ========== Rate Limiting Queries ==========
    
    /**
     * Count notifications sent to recipient in time window (for rate limiting)
     */
    @Query("""
        SELECT COUNT(n)
        FROM NotificationEntity n
        WHERE n.tenantId = :tenantId
        AND n.recipient = :recipient
        AND n.channel = :channel
        AND n.status IN ('SENDING', 'SENT', 'DELIVERED')
        AND n.createdAt >= :since
        """)
    long countRecentByRecipient(
        @Param("tenantId") String tenantId,
        @Param("recipient") String recipient,
        @Param("channel") NotificationEntity.NotificationChannel channel,
        @Param("since") LocalDateTime since
    );
}
```

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/persistence/NotificationPreferenceRepository.java`

```java
package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for NotificationPreference entities
 */
@Repository
public interface NotificationPreferenceRepository 
    extends JpaRepository<NotificationPreferenceEntity, String> {

    /**
     * Find preferences by user and tenant
     */
    Optional<NotificationPreferenceEntity> findByUserIdAndTenantId(
        String userId, 
        String tenantId
    );
    
    /**
     * Check if user has given consent for notifications
     */
    @Query("""
        SELECT p.consentGiven
        FROM NotificationPreferenceEntity p
        WHERE p.userId = :userId
        AND p.tenantId = :tenantId
        """)
    Optional<Boolean> hasConsent(
        @Param("userId") String userId,
        @Param("tenantId") String tenantId
    );
    
    /**
     * Find email address for user
     */
    @Query("""
        SELECT p.emailAddress
        FROM NotificationPreferenceEntity p
        WHERE p.userId = :userId
        AND p.tenantId = :tenantId
        AND p.emailEnabled = true
        """)
    Optional<String> findEmailAddress(
        @Param("userId") String userId,
        @Param("tenantId") String tenantId
    );
    
    /**
     * Find phone number for SMS
     */
    @Query("""
        SELECT p.phoneNumber
        FROM NotificationPreferenceEntity p
        WHERE p.userId = :userId
        AND p.tenantId = :tenantId
        AND p.smsEnabled = true
        """)
    Optional<String> findPhoneNumber(
        @Param("userId") String userId,
        @Param("tenantId") String tenantId
    );
}
```

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/persistence/NotificationTemplateRepository.java`

```java
package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for NotificationTemplate entities
 */
@Repository
public interface NotificationTemplateRepository 
    extends JpaRepository<NotificationTemplateEntity, String> {

    /**
     * Find template by key and channel
     */
    @Query("""
        SELECT t FROM NotificationTemplateEntity t
        WHERE t.templateKey = :templateKey
        AND t.channel = :channel
        AND t.isActive = true
        ORDER BY t.version DESC
        LIMIT 1
        """)
    Optional<NotificationTemplateEntity> findActiveTemplate(
        @Param("templateKey") String templateKey,
        @Param("channel") NotificationEntity.NotificationChannel channel
    );
    
    /**
     * Find template by notification type and channel
     */
    @Query("""
        SELECT t FROM NotificationTemplateEntity t
        WHERE t.notificationType = :type
        AND t.channel = :channel
        AND t.isActive = true
        ORDER BY t.version DESC
        LIMIT 1
        """)
    Optional<NotificationTemplateEntity> findByTypeAndChannel(
        @Param("type") NotificationEntity.NotificationType type,
        @Param("channel") NotificationEntity.NotificationChannel channel
    );
}
```

#### 1.3 Configuration Properties

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/config/NotificationProperties.java`

```java
package com.healthdata.quality.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration properties for notification system
 */
@Configuration
@ConfigurationProperties(prefix = "notification")
@Data
public class NotificationProperties {

    private Providers providers = new Providers();
    private RateLimits rateLimits = new RateLimits();
    private Retry retry = new Retry();
    private Queue queue = new Queue();
    private Templates templates = new Templates();

    @Data
    public static class Providers {
        private SendGrid sendgrid = new SendGrid();
        private AwsSes awsSes = new AwsSes();
        private Smtp smtp = new Smtp();
        private Twilio twilio = new Twilio();
        private AwsSns awsSns = new AwsSns();

        @Data
        public static class SendGrid {
            private boolean enabled = false;
            private String apiKey;
            private String fromEmail;
            private String fromName;
        }

        @Data
        public static class AwsSes {
            private boolean enabled = false;
            private String region = "us-east-1";
            private String accessKey;
            private String secretKey;
            private String fromEmail;
        }

        @Data
        public static class Smtp {
            private boolean enabled = true;
            private String host = "localhost";
            private int port = 1025;
            private String username;
            private String password;
            private boolean auth = false;
            private boolean starttls = false;
        }

        @Data
        public static class Twilio {
            private boolean enabled = false;
            private String accountSid;
            private String authToken;
            private String fromNumber;
        }

        @Data
        public static class AwsSns {
            private boolean enabled = false;
            private String region = "us-east-1";
            private String accessKey;
            private String secretKey;
        }
    }

    @Data
    public static class RateLimits {
        private int emailPerHour = 10;
        private int emailPerDay = 50;
        private int smsPerHour = 5;
        private int smsPerDay = 20;
        private int pushPerHour = 20;
        private int pushPerDay = 100;
    }

    @Data
    public static class Retry {
        private int maxAttempts = 5;
        private Duration initialDelay = Duration.ofMinutes(5);
        private Duration maxDelay = Duration.ofHours(4);
        private double multiplier = 3.0;
    }

    @Data
    public static class Queue {
        private Redis redis = new Redis();
        private Kafka kafka = new Kafka();

        @Data
        public static class Redis {
            private boolean enabled = true;
            private String keyPrefix = "notifications:";
        }

        @Data
        public static class Kafka {
            private boolean enabled = false;
            private String topic = "notifications";
        }
    }

    @Data
    public static class Templates {
        private String basePath = "classpath:/templates/notifications";
        private boolean cacheEnabled = true;
        private Duration cacheExpiry = Duration.ofHours(1);
    }
}
```

#### 1.4 Update application.yml

Add to `/backend/modules/services/quality-measure-service/src/main/resources/application.yml`:

```yaml
# Notification System Configuration
notification:
  providers:
    sendgrid:
      enabled: ${SENDGRID_ENABLED:false}
      api-key: ${SENDGRID_API_KEY:}
      from-email: ${SENDGRID_FROM_EMAIL:alerts@healthdata.com}
      from-name: ${SENDGRID_FROM_NAME:HealthData Clinical Alerts}
    
    aws-ses:
      enabled: ${AWS_SES_ENABLED:false}
      region: ${AWS_REGION:us-east-1}
      access-key: ${AWS_ACCESS_KEY:}
      secret-key: ${AWS_SECRET_KEY:}
      from-email: ${AWS_SES_FROM_EMAIL:alerts@healthdata.com}
    
    smtp:
      enabled: ${SMTP_ENABLED:true}
      host: ${SMTP_HOST:localhost}
      port: ${SMTP_PORT:1025}
      username: ${SMTP_USERNAME:}
      password: ${SMTP_PASSWORD:}
      auth: ${SMTP_AUTH:false}
      starttls: ${SMTP_STARTTLS:false}
    
    twilio:
      enabled: ${TWILIO_ENABLED:false}
      account-sid: ${TWILIO_ACCOUNT_SID:}
      auth-token: ${TWILIO_AUTH_TOKEN:}
      from-number: ${TWILIO_FROM_NUMBER:}
    
    aws-sns:
      enabled: ${AWS_SNS_ENABLED:false}
      region: ${AWS_REGION:us-east-1}
      access-key: ${AWS_ACCESS_KEY:}
      secret-key: ${AWS_SECRET_KEY:}
  
  rate-limits:
    email-per-hour: 10
    email-per-day: 50
    sms-per-hour: 5
    sms-per-day: 20
    push-per-hour: 20
    push-per-day: 100
  
  retry:
    max-attempts: 5
    initial-delay: 5m
    max-delay: 4h
    multiplier: 3.0
  
  queue:
    redis:
      enabled: true
      key-prefix: "notifications:"
    kafka:
      enabled: false
      topic: "notifications"
  
  templates:
    base-path: classpath:/templates/notifications
    cache-enabled: true
    cache-expiry: 1h
```

### Testing Phase 1

**Unit Tests**:
- Repository query methods
- Entity validation logic
- Configuration property binding

**Integration Tests**:
- Database migrations with Liquibase
- Repository queries with test data
- Multi-tenant data isolation

**Success Criteria**:
- ✅ All tables created with correct schema
- ✅ Indexes and RLS policies applied
- ✅ Repositories return expected results
- ✅ Configuration loads from application.yml

---

## PHASE 2: Template System (Week 2-3)

### Goals
- Create responsive HTML email templates
- Implement template rendering engine
- Add variable substitution
- Support multiple channels (Email, SMS, Push)

### Tasks

#### 2.1 Template Entity

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/persistence/NotificationTemplateEntity.java`

```java
package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "notification_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "template_key", nullable = false, unique = true)
    private String templateKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "channel", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationEntity.NotificationChannel channel;

    @Column(name = "notification_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationEntity.NotificationType notificationType;

    // Template Content
    @Column(name = "subject_template", columnDefinition = "TEXT")
    private String subjectTemplate;

    @Column(name = "html_template", columnDefinition = "TEXT")
    private String htmlTemplate;

    @Column(name = "text_template", columnDefinition = "TEXT")
    private String textTemplate;

    @Column(name = "sms_template", length = 160)
    private String smsTemplate;

    // Version Control
    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Metadata
    @Column(name = "variables", columnDefinition = "jsonb")
    private Map<String, String> variables; // Variable descriptions

    @Column(name = "settings", columnDefinition = "jsonb")
    private Map<String, Object> settings;

    // Audit
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

#### 2.2 Template Service

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/notification/NotificationTemplateService.java`

```java
package com.healthdata.quality.service.notification;

import com.healthdata.quality.persistence.NotificationEntity;
import com.healthdata.quality.persistence.NotificationTemplateEntity;
import com.healthdata.quality.persistence.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template Management and Rendering Service
 * 
 * Handles template loading, variable substitution, and rendering
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;
    
    // Pattern for variable substitution: {{variable.name}}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    /**
     * Get template by key and channel (cached)
     */
    @Cacheable(value = "notification-templates", key = "#templateKey + '-' + #channel")
    public NotificationTemplateEntity getTemplate(
        String templateKey, 
        NotificationEntity.NotificationChannel channel
    ) {
        return templateRepository.findActiveTemplate(templateKey, channel)
            .orElseThrow(() -> new TemplateNotFoundException(
                "Template not found: " + templateKey + " for channel: " + channel
            ));
    }

    /**
     * Get template by notification type and channel
     */
    @Cacheable(value = "notification-templates", key = "#type + '-' + #channel")
    public NotificationTemplateEntity getTemplateByType(
        NotificationEntity.NotificationType type,
        NotificationEntity.NotificationChannel channel
    ) {
        return templateRepository.findByTypeAndChannel(type, channel)
            .orElseThrow(() -> new TemplateNotFoundException(
                "Template not found for type: " + type + " and channel: " + channel
            ));
    }

    /**
     * Render template subject with variables
     */
    public String renderSubject(NotificationTemplateEntity template, Map<String, Object> variables) {
        if (template.getSubjectTemplate() == null) {
            return "";
        }
        return substituteVariables(template.getSubjectTemplate(), variables);
    }

    /**
     * Render HTML email template
     */
    public String renderHtml(NotificationTemplateEntity template, Map<String, Object> variables) {
        if (template.getHtmlTemplate() == null) {
            throw new TemplateRenderException("HTML template is null");
        }
        return substituteVariables(template.getHtmlTemplate(), variables);
    }

    /**
     * Render plain text email template (fallback)
     */
    public String renderText(NotificationTemplateEntity template, Map<String, Object> variables) {
        if (template.getTextTemplate() == null) {
            // Fallback to HTML template with HTML tags stripped
            String html = renderHtml(template, variables);
            return stripHtmlTags(html);
        }
        return substituteVariables(template.getTextTemplate(), variables);
    }

    /**
     * Render SMS template (160 char limit)
     */
    public String renderSms(NotificationTemplateEntity template, Map<String, Object> variables) {
        if (template.getSmsTemplate() == null) {
            throw new TemplateRenderException("SMS template is null");
        }
        
        String rendered = substituteVariables(template.getSmsTemplate(), variables);
        
        // Enforce 160 character limit for SMS
        if (rendered.length() > 160) {
            log.warn("SMS template exceeds 160 characters, truncating: {}", template.getTemplateKey());
            rendered = rendered.substring(0, 157) + "...";
        }
        
        return rendered;
    }

    /**
     * Substitute variables in template
     * 
     * Supports nested variables: {{patient.name}}, {{alert.severity}}
     */
    private String substituteVariables(String template, Map<String, Object> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variablePath = matcher.group(1).trim();
            Object value = resolveVariable(variablePath, variables);
            
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Resolve nested variable paths (e.g., "patient.name")
     */
    @SuppressWarnings("unchecked")
    private Object resolveVariable(String path, Map<String, Object> variables) {
        String[] parts = path.split("\\.");
        Object current = variables;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * Strip HTML tags for plain text fallback
     */
    private String stripHtmlTags(String html) {
        return html
            .replaceAll("<[^>]*>", "")  // Remove HTML tags
            .replaceAll("\\s+", " ")     // Normalize whitespace
            .trim();
    }

    /**
     * Template not found exception
     */
    public static class TemplateNotFoundException extends RuntimeException {
        public TemplateNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Template rendering exception
     */
    public static class TemplateRenderException extends RuntimeException {
        public TemplateRenderException(String message) {
            super(message);
        }
    }
}
```

#### 2.3 HTML Email Templates

Create responsive email templates in `/backend/modules/services/quality-measure-service/src/main/resources/templates/notifications/`

**File**: `email-base.html` (Base template with responsive design)

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title>{{subject}}</title>
    <style>
        /* Reset styles */
        body, table, td, a { -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }
        table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; }
        img { -ms-interpolation-mode: bicubic; border: 0; }

        /* Base styles */
        body {
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            font-size: 16px;
            line-height: 1.6;
            color: #333333;
            background-color: #f4f4f4;
        }

        .email-container {
            max-width: 600px;
            margin: 0 auto;
            background-color: #ffffff;
        }

        .header {
            background-color: #0066cc;
            color: #ffffff;
            padding: 20px;
            text-align: center;
        }

        .header h1 {
            margin: 0;
            font-size: 24px;
            font-weight: 600;
        }

        .content {
            padding: 30px;
        }

        .alert-banner {
            padding: 15px;
            margin-bottom: 20px;
            border-radius: 4px;
            font-weight: 600;
        }

        .alert-critical {
            background-color: #dc3545;
            color: #ffffff;
            border-left: 4px solid #a71d2a;
        }

        .alert-high {
            background-color: #ff9800;
            color: #ffffff;
            border-left: 4px solid #cc7a00;
        }

        .alert-medium {
            background-color: #ffc107;
            color: #000000;
            border-left: 4px solid #cc9a06;
        }

        .info-box {
            background-color: #f8f9fa;
            border: 1px solid #dee2e6;
            border-radius: 4px;
            padding: 15px;
            margin: 15px 0;
        }

        .info-box-label {
            font-weight: 600;
            color: #495057;
            margin-bottom: 5px;
        }

        .button {
            display: inline-block;
            padding: 12px 24px;
            background-color: #0066cc;
            color: #ffffff !important;
            text-decoration: none;
            border-radius: 4px;
            font-weight: 600;
            margin: 15px 0;
        }

        .button:hover {
            background-color: #0052a3;
        }

        .footer {
            background-color: #f8f9fa;
            padding: 20px;
            text-align: center;
            font-size: 14px;
            color: #6c757d;
            border-top: 1px solid #dee2e6;
        }

        .footer a {
            color: #0066cc;
            text-decoration: none;
        }

        /* Mobile responsive */
        @media only screen and (max-width: 600px) {
            .content {
                padding: 20px !important;
            }
            
            .button {
                display: block !important;
                width: 100% !important;
                text-align: center !important;
            }
        }
    </style>
</head>
<body>
    <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%">
        <tr>
            <td align="center" style="padding: 20px 0;">
                <table role="presentation" class="email-container" cellspacing="0" cellpadding="0" border="0" width="600">
                    
                    <!-- Header -->
                    <tr>
                        <td class="header">
                            <h1>HealthData Clinical Portal</h1>
                        </td>
                    </tr>

                    <!-- Content -->
                    <tr>
                        <td class="content">
                            {{content}}
                        </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                        <td class="footer">
                            <p>This is an automated notification from the HealthData Clinical Alert System.</p>
                            <p>
                                <a href="{{unsubscribe_url}}">Unsubscribe</a> | 
                                <a href="{{preferences_url}}">Manage Preferences</a>
                            </p>
                            <p style="margin-top: 15px; font-size: 12px;">
                                © 2025 HealthData in Motion. All rights reserved.<br>
                                HIPAA Compliant | Secure Healthcare Communications
                            </p>
                        </td>
                    </tr>

                </table>
            </td>
        </tr>
    </table>
</body>
</html>
```

**File**: `critical-alert.html`

```html
<div class="alert-banner alert-critical">
    ⚠️ CRITICAL ALERT - IMMEDIATE ACTION REQUIRED
</div>

<h2>{{alert.title}}</h2>

<div class="info-box">
    <div class="info-box-label">Patient Information</div>
    <strong>Name:</strong> {{patient.name}}<br>
    <strong>MRN:</strong> {{patient.mrn}}<br>
    <strong>DOB:</strong> {{patient.dob}}
</div>

<div class="info-box">
    <div class="info-box-label">Alert Details</div>
    <strong>Severity:</strong> <span style="color: #dc3545; font-weight: 600;">{{alert.severity}}</span><br>
    <strong>Type:</strong> {{alert.type}}<br>
    <strong>Triggered:</strong> {{alert.triggered_at}}
</div>

<p style="font-size: 18px; line-height: 1.6;">
    {{alert.message}}
</p>

<div style="background-color: #fff3cd; border: 1px solid #ffc107; border-radius: 4px; padding: 15px; margin: 20px 0;">
    <strong>Action Required:</strong><br>
    {{alert.action_guidance}}
</div>

<a href="{{action.url}}" class="button">View Patient Record</a>

<p style="margin-top: 30px; font-size: 14px; color: #6c757d;">
    This alert requires immediate clinical review. Please respond within the designated timeframe.
</p>
```

**File**: `care-gap.html`

```html
<h2>Care Gap Identified</h2>

<p>A quality measure care gap has been identified for one of your patients.</p>

<div class="info-box">
    <div class="info-box-label">Patient Information</div>
    <strong>Name:</strong> {{patient.name}}<br>
    <strong>MRN:</strong> {{patient.mrn}}
</div>

<div class="info-box">
    <div class="info-box-label">Care Gap Details</div>
    <strong>Measure:</strong> {{gap.measure_name}}<br>
    <strong>Type:</strong> {{gap.type}}<br>
    <strong>Priority:</strong> {{gap.priority}}<br>
    <strong>Due Date:</strong> {{gap.due_date}}
</div>

<p>{{gap.description}}</p>

<div style="background-color: #e7f3ff; border-left: 4px solid #0066cc; padding: 15px; margin: 20px 0;">
    <strong>Recommended Actions:</strong><br>
    {{gap.recommendations}}
</div>

<a href="{{action.url}}" class="button">Address Care Gap</a>
```

**File**: `health-score-update.html`

```html
<h2>Patient Health Score Update</h2>

<p>The health score for {{patient.name}} has changed significantly.</p>

<div class="info-box">
    <div class="info-box-label">Patient Information</div>
    <strong>Name:</strong> {{patient.name}}<br>
    <strong>MRN:</strong> {{patient.mrn}}
</div>

<table role="presentation" style="width: 100%; border-collapse: collapse; margin: 20px 0;">
    <tr style="background-color: #f8f9fa;">
        <th style="padding: 10px; text-align: left; border: 1px solid #dee2e6;">Metric</th>
        <th style="padding: 10px; text-align: left; border: 1px solid #dee2e6;">Previous</th>
        <th style="padding: 10px; text-align: left; border: 1px solid #dee2e6;">Current</th>
        <th style="padding: 10px; text-align: left; border: 1px solid #dee2e6;">Change</th>
    </tr>
    <tr>
        <td style="padding: 10px; border: 1px solid #dee2e6;">Overall Health Score</td>
        <td style="padding: 10px; border: 1px solid #dee2e6;">{{score.previous}}</td>
        <td style="padding: 10px; border: 1px solid #dee2e6;">{{score.current}}</td>
        <td style="padding: 10px; border: 1px solid #dee2e6;">{{score.change}}</td>
    </tr>
</table>

<p>{{score.summary}}</p>

<a href="{{action.url}}" class="button">View Full Report</a>
```

**File**: `digest.html`

```html
<h2>Daily Notification Digest</h2>

<p>Here's a summary of notifications from the past 24 hours.</p>

<div style="margin: 20px 0;">
    <h3 style="color: #0066cc; margin-bottom: 10px;">Critical Alerts ({{digest.critical_count}})</h3>
    {{#each digest.critical_alerts}}
    <div style="padding: 10px; border-left: 3px solid #dc3545; margin-bottom: 10px; background-color: #f8f9fa;">
        <strong>{{this.title}}</strong><br>
        <span style="font-size: 14px; color: #6c757d;">{{this.patient_name}} - {{this.time}}</span>
    </div>
    {{/each}}
</div>

<div style="margin: 20px 0;">
    <h3 style="color: #0066cc; margin-bottom: 10px;">Care Gaps ({{digest.care_gap_count}})</h3>
    {{#each digest.care_gaps}}
    <div style="padding: 10px; border-left: 3px solid #ffc107; margin-bottom: 10px; background-color: #f8f9fa;">
        <strong>{{this.measure}}</strong><br>
        <span style="font-size: 14px; color: #6c757d;">{{this.patient_name}} - Due: {{this.due_date}}</span>
    </div>
    {{/each}}
</div>

<a href="{{action.url}}" class="button">View All Notifications</a>
```

### Testing Phase 2

**Unit Tests**:
- Template variable substitution
- HTML rendering correctness
- SMS character limit enforcement
- Nested variable resolution

**Integration Tests**:
- Template loading from database
- Cache effectiveness
- Template fallback logic

**Success Criteria**:
- ✅ All templates render correctly
- ✅ Variables substituted properly
- ✅ Responsive design works on mobile
- ✅ Plain text fallback generates properly

---

## PHASE 3: Provider Integrations (Week 3-5)

### Goals
- Integrate SendGrid for email
- Integrate Twilio for SMS
- Implement auto-failover logic
- Add provider health monitoring

### Tasks

#### 3.1 Provider Interface

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/notification/provider/NotificationProvider.java`

```java
package com.healthdata.quality.service.notification.provider;

import com.healthdata.quality.persistence.NotificationEntity;

/**
 * Interface for notification delivery providers
 */
public interface NotificationProvider {

    /**
     * Send notification via this provider
     * 
     * @return ProviderResponse with delivery status and metadata
     */
    ProviderResponse send(NotificationRequest request);

    /**
     * Check if provider is healthy and ready to send
     */
    boolean isHealthy();

    /**
     * Get provider name
     */
    String getProviderName();

    /**
     * Get supported channel
     */
    NotificationEntity.NotificationChannel getChannel();

    /**
     * Get priority (lower = higher priority for failover)
     */
    int getPriority();
}
```

**File**: `NotificationRequest.java`

```java
package com.healthdata.quality.service.notification.provider;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class NotificationRequest {
    private String tenantId;
    private String recipient;
    private String subject;
    private String htmlBody;
    private String textBody;
    private String smsBody;
    private Map<String, Object> metadata;
}
```

**File**: `ProviderResponse.java`

```java
package com.healthdata.quality.service.notification.provider;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ProviderResponse {
    private boolean success;
    private String providerMessageId;
    private String providerName;
    private String errorMessage;
    private Map<String, Object> metadata;
}
```

#### 3.2 SendGrid Provider

Add dependency to `build.gradle.kts`:
```kotlin
implementation("com.sendgrid:sendgrid-java:4.9.3")
```

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/notification/provider/SendGridEmailProvider.java`

```java
package com.healthdata.quality.service.notification.provider;

import com.healthdata.quality.config.NotificationProperties;
import com.healthdata.quality.persistence.NotificationEntity;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * SendGrid Email Provider
 * 
 * Primary email delivery provider with high deliverability and analytics
 */
@Component
@ConditionalOnProperty(prefix = "notification.providers.sendgrid", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SendGridEmailProvider implements NotificationProvider {

    private final NotificationProperties properties;
    private final SendGrid sendGridClient;

    public SendGridEmailProvider(NotificationProperties properties) {
        this.properties = properties;
        this.sendGridClient = new SendGrid(properties.getProviders().getSendgrid().getApiKey());
    }

    @Override
    public ProviderResponse send(NotificationRequest request) {
        try {
            Email from = new Email(
                properties.getProviders().getSendgrid().getFromEmail(),
                properties.getProviders().getSendgrid().getFromName()
            );
            Email to = new Email(request.getRecipient());
            
            Content htmlContent = new Content("text/html", request.getHtmlBody());
            Mail mail = new Mail(from, request.getSubject(), to, htmlContent);
            
            // Add plain text alternative
            if (request.getTextBody() != null) {
                Content textContent = new Content("text/plain", request.getTextBody());
                mail.addContent(textContent);
            }

            // Add custom headers for tracking
            Map<String, String> customArgs = new HashMap<>();
            customArgs.put("tenant_id", request.getTenantId());
            customArgs.put("timestamp", String.valueOf(System.currentTimeMillis()));
            mail.setCustomArgs(customArgs);

            Request sendRequest = new Request();
            sendRequest.setMethod(Method.POST);
            sendRequest.setEndpoint("mail/send");
            sendRequest.setBody(mail.build());

            Response response = sendGridClient.api(sendRequest);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("SendGrid email sent successfully to {}", request.getRecipient());
                
                return ProviderResponse.builder()
                    .success(true)
                    .providerName(getProviderName())
                    .providerMessageId(response.getHeaders().get("X-Message-Id"))
                    .build();
            } else {
                log.error("SendGrid API error: {} - {}", response.getStatusCode(), response.getBody());
                
                return ProviderResponse.builder()
                    .success(false)
                    .providerName(getProviderName())
                    .errorMessage("SendGrid API error: " + response.getStatusCode())
                    .build();
            }

        } catch (IOException e) {
            log.error("SendGrid send failed: {}", e.getMessage());
            
            return ProviderResponse.builder()
                .success(false)
                .providerName(getProviderName())
                .errorMessage(e.getMessage())
                .build();
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            // Simple health check - verify API key is valid
            Request request = new Request();
            request.setMethod(Method.GET);
            request.setEndpoint("scopes");
            
            Response response = sendGridClient.api(request);
            return response.getStatusCode() == 200;
            
        } catch (Exception e) {
            log.warn("SendGrid health check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "SendGrid";
    }

    @Override
    public NotificationEntity.NotificationChannel getChannel() {
        return NotificationEntity.NotificationChannel.EMAIL;
    }

    @Override
    public int getPriority() {
        return 1; // Highest priority
    }
}
```

#### 3.3 AWS SES Provider

Add dependency to `build.gradle.kts`:
```kotlin
implementation("software.amazon.awssdk:ses:2.20.0")
```

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/notification/provider/AwsSesEmailProvider.java`

```java
package com.healthdata.quality.service.notification.provider;

import com.healthdata.quality.config.NotificationProperties;
import com.healthdata.quality.persistence.NotificationEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

/**
 * AWS SES Email Provider
 * 
 * Failover email provider with excellent deliverability
 */
@Component
@ConditionalOnProperty(prefix = "notification.providers.aws-ses", name = "enabled", havingValue = "true")
@Slf4j
public class AwsSesEmailProvider implements NotificationProvider {

    private final NotificationProperties properties;
    private final SesClient sesClient;

    public AwsSesEmailProvider(NotificationProperties properties) {
        this.properties = properties;
        
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
            properties.getProviders().getAwsSes().getAccessKey(),
            properties.getProviders().getAwsSes().getSecretKey()
        );

        this.sesClient = SesClient.builder()
            .region(Region.of(properties.getProviders().getAwsSes().getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
            .build();
    }

    @Override
    public ProviderResponse send(NotificationRequest request) {
        try {
            Destination destination = Destination.builder()
                .toAddresses(request.getRecipient())
                .build();

            Content subjectContent = Content.builder()
                .data(request.getSubject())
                .build();

            Content htmlContent = Content.builder()
                .data(request.getHtmlBody())
                .build();

            Body body = Body.builder()
                .html(htmlContent)
                .build();

            // Add text alternative if available
            if (request.getTextBody() != null) {
                Content textContent = Content.builder()
                    .data(request.getTextBody())
                    .build();
                body = body.toBuilder().text(textContent).build();
            }

            Message message = Message.builder()
                .subject(subjectContent)
                .body(body)
                .build();

            SendEmailRequest emailRequest = SendEmailRequest.builder()
                .source(properties.getProviders().getAwsSes().getFromEmail())
                .destination(destination)
                .message(message)
                .build();

            SendEmailResponse response = sesClient.sendEmail(emailRequest);

            log.info("AWS SES email sent successfully to {}, messageId: {}", 
                request.getRecipient(), response.messageId());

            return ProviderResponse.builder()
                .success(true)
                .providerName(getProviderName())
                .providerMessageId(response.messageId())
                .build();

        } catch (SesException e) {
            log.error("AWS SES send failed: {}", e.awsErrorDetails().errorMessage());
            
            return ProviderResponse.builder()
                .success(false)
                .providerName(getProviderName())
                .errorMessage(e.awsErrorDetails().errorMessage())
                .build();
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            GetAccountSendingEnabledRequest request = 
                GetAccountSendingEnabledRequest.builder().build();
            GetAccountSendingEnabledResponse response = sesClient.getAccountSendingEnabled(request);
            return response.enabled();
            
        } catch (Exception e) {
            log.warn("AWS SES health check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "AWS-SES";
    }

    @Override
    public NotificationEntity.NotificationChannel getChannel() {
        return NotificationEntity.NotificationChannel.EMAIL;
    }

    @Override
    public int getPriority() {
        return 2; // Second priority (failover)
    }
}
```

#### 3.4 Twilio SMS Provider

Add dependency to `build.gradle.kts`:
```kotlin
implementation("com.twilio.sdk:twilio:9.2.0")
```

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/notification/provider/TwilioSmsProvider.java`

```java
package com.healthdata.quality.service.notification.provider;

import com.healthdata.quality.config.NotificationProperties;
import com.healthdata.quality.persistence.NotificationEntity;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Twilio SMS Provider
 * 
 * Primary SMS delivery provider with global coverage
 */
@Component
@ConditionalOnProperty(prefix = "notification.providers.twilio", name = "enabled", havingValue = "true")
@Slf4j
public class TwilioSmsProvider implements NotificationProvider {

    private final NotificationProperties properties;

    public TwilioSmsProvider(NotificationProperties properties) {
        this.properties = properties;
        
        Twilio.init(
            properties.getProviders().getTwilio().getAccountSid(),
            properties.getProviders().getTwilio().getAuthToken()
        );
    }

    @Override
    public ProviderResponse send(NotificationRequest request) {
        try {
            Message message = Message.creator(
                new PhoneNumber(request.getRecipient()),
                new PhoneNumber(properties.getProviders().getTwilio().getFromNumber()),
                request.getSmsBody()
            ).create();

            log.info("Twilio SMS sent successfully to {}, sid: {}", 
                request.getRecipient(), message.getSid());

            return ProviderResponse.builder()
                .success(true)
                .providerName(getProviderName())
                .providerMessageId(message.getSid())
                .build();

        } catch (Exception e) {
            log.error("Twilio SMS send failed: {}", e.getMessage());
            
            return ProviderResponse.builder()
                .success(false)
                .providerName(getProviderName())
                .errorMessage(e.getMessage())
                .build();
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            // Health check - fetch account details
            com.twilio.rest.api.v2010.Account account = 
                com.twilio.rest.api.v2010.Account.fetcher(
                    properties.getProviders().getTwilio().getAccountSid()
                ).fetch();
            
            return account != null && "active".equalsIgnoreCase(account.getStatus().toString());
            
        } catch (Exception e) {
            log.warn("Twilio health check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "Twilio";
    }

    @Override
    public NotificationEntity.NotificationChannel getChannel() {
        return NotificationEntity.NotificationChannel.SMS;
    }

    @Override
    public int getPriority() {
        return 1; // Highest priority
    }
}
```

#### 3.5 Provider Registry & Auto-Failover

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/notification/provider/NotificationProviderRegistry.java`

```java
package com.healthdata.quality.service.notification.provider;

import com.healthdata.quality.persistence.NotificationEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provider Registry with Auto-Failover
 * 
 * Manages notification providers and handles automatic failover
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProviderRegistry {

    private final List<NotificationProvider> providers;

    /**
     * Send notification with automatic failover
     * 
     * Tries providers in priority order until one succeeds
     */
    public ProviderResponse sendWithFailover(
        NotificationEntity.NotificationChannel channel,
        NotificationRequest request
    ) {
        List<NotificationProvider> channelProviders = getProvidersForChannel(channel);

        if (channelProviders.isEmpty()) {
            log.error("No providers available for channel: {}", channel);
            return ProviderResponse.builder()
                .success(false)
                .errorMessage("No providers configured for channel: " + channel)
                .build();
        }

        // Try each provider in priority order
        for (NotificationProvider provider : channelProviders) {
            if (!provider.isHealthy()) {
                log.warn("Provider {} is unhealthy, skipping", provider.getProviderName());
                continue;
            }

            log.info("Attempting to send via provider: {}", provider.getProviderName());
            ProviderResponse response = provider.send(request);

            if (response.isSuccess()) {
                log.info("Notification sent successfully via {}", provider.getProviderName());
                return response;
            } else {
                log.warn("Provider {} failed: {}, trying next provider",
                    provider.getProviderName(), response.getErrorMessage());
            }
        }

        // All providers failed
        log.error("All providers failed for channel: {}", channel);
        return ProviderResponse.builder()
            .success(false)
            .errorMessage("All providers failed for channel: " + channel)
            .build();
    }

    /**
     * Get providers for specific channel, sorted by priority
     */
    private List<NotificationProvider> getProvidersForChannel(
        NotificationEntity.NotificationChannel channel
    ) {
        return providers.stream()
            .filter(p -> p.getChannel() == channel)
            .sorted(Comparator.comparingInt(NotificationProvider::getPriority))
            .collect(Collectors.toList());
    }

    /**
     * Check health of all providers
     */
    public List<ProviderHealthStatus> checkAllProvidersHealth() {
        return providers.stream()
            .map(provider -> ProviderHealthStatus.builder()
                .providerName(provider.getProviderName())
                .channel(provider.getChannel())
                .healthy(provider.isHealthy())
                .priority(provider.getPriority())
                .build())
            .collect(Collectors.toList());
    }
}
```

### Testing Phase 3

**Unit Tests**:
- Provider send logic
- Failover mechanism
- Health check responses

**Integration Tests**:
- SendGrid API integration (with test mode)
- Twilio SMS sending (with test numbers)
- Provider failover scenarios

**Success Criteria**:
- ✅ Emails send via SendGrid
- ✅ SMS send via Twilio
- ✅ Failover works when primary provider fails
- ✅ Provider health checks accurate

---

## PHASE 4: Delivery Pipeline & Queue (Week 5-6)

### Goals
- Implement async notification queue
- Add retry logic with exponential backoff
- Implement rate limiting
- Create dead letter queue for failures

### Tasks

#### 4.1 Notification Queue Service

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/notification/NotificationQueueService.java`

```java
package com.healthdata.quality.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.config.NotificationProperties;
import com.healthdata.quality.persistence.NotificationEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed Notification Queue
 * 
 * Provides async queuing with priority support
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationQueueService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationProperties properties;

    private static final String QUEUE_KEY_PREFIX = "notifications:queue:";
    private static final String DLQ_KEY = "notifications:dlq";
    private static final String PROCESSING_KEY = "notifications:processing";

    /**
     * Enqueue notification for async processing
     */
    public void enqueue(NotificationEntity notification) {
        try {
            String queueKey = getQueueKey(notification);
            String json = objectMapper.writeValueAsString(notification);

            // Add to queue with score = timestamp (for FIFO with priority)
            long score = System.currentTimeMillis();
            
            // Critical alerts get higher priority (lower score)
            if (notification.getSeverity() == NotificationEntity.NotificationSeverity.CRITICAL) {
                score -= 1000000; // Prioritize by 1000 seconds
            }

            redisTemplate.opsForZSet().add(queueKey, json, score);
            
            log.info("Notification {} enqueued to {}", notification.getId(), queueKey);

        } catch (Exception e) {
            log.error("Failed to enqueue notification: {}", e.getMessage());
            throw new RuntimeException("Failed to enqueue notification", e);
        }
    }

    /**
     * Enqueue notification for retry with delay
     */
    public void enqueueForRetry(NotificationEntity notification, Duration delay) {
        try {
            notification.setNextRetryAt(LocalDateTime.now().plus(delay));
            notification.setRetryCount(notification.getRetryCount() + 1);

            String queueKey = getQueueKey(notification);
            String json = objectMapper.writeValueAsString(notification);

            // Schedule for future processing
            long score = System.currentTimeMillis() + delay.toMillis();
            redisTemplate.opsForZSet().add(queueKey, json, score);

            log.info("Notification {} scheduled for retry in {}", 
                notification.getId(), delay);

        } catch (Exception e) {
            log.error("Failed to enqueue notification for retry: {}", e.getMessage());
        }
    }

    /**
     * Dequeue notification for processing
     */
    public NotificationEntity dequeue(NotificationEntity.NotificationChannel channel) {
        try {
            String queueKey = getQueueKey(channel);
            long now = System.currentTimeMillis();

            // Get oldest notification that's ready for processing
            var entries = redisTemplate.opsForZSet()
                .rangeByScoreWithScores(queueKey, 0, now, 0, 1);

            if (entries == null || entries.isEmpty()) {
                return null;
            }

            var entry = entries.iterator().next();
            String json = entry.getValue();

            // Remove from queue and add to processing set
            redisTemplate.opsForZSet().remove(queueKey, json);
            redisTemplate.opsForSet().add(PROCESSING_KEY, json);
            redisTemplate.expire(PROCESSING_KEY, 1, TimeUnit.HOURS);

            NotificationEntity notification = objectMapper.readValue(
                json, NotificationEntity.class
            );

            log.debug("Notification {} dequeued from {}", 
                notification.getId(), queueKey);

            return notification;

        } catch (Exception e) {
            log.error("Failed to dequeue notification: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Mark notification processing complete
     */
    public void markComplete(NotificationEntity notification) {
        try {
            String json = objectMapper.writeValueAsString(notification);
            redisTemplate.opsForSet().remove(PROCESSING_KEY, json);
            
            log.debug("Notification {} marked complete", notification.getId());

        } catch (Exception e) {
            log.error("Failed to mark notification complete: {}", e.getMessage());
        }
    }

    /**
     * Move notification to dead letter queue
     */
    public void moveToDLQ(NotificationEntity notification, String reason) {
        try {
            String json = objectMapper.writeValueAsString(notification);
            
            // Remove from processing
            redisTemplate.opsForSet().remove(PROCESSING_KEY, json);
            
            // Add to DLQ with timestamp
            redisTemplate.opsForZSet().add(DLQ_KEY, json, System.currentTimeMillis());
            
            log.warn("Notification {} moved to DLQ: {}", notification.getId(), reason);

        } catch (Exception e) {
            log.error("Failed to move notification to DLQ: {}", e.getMessage());
        }
    }

    /**
     * Get queue size for monitoring
     */
    public long getQueueSize(NotificationEntity.NotificationChannel channel) {
        String queueKey = getQueueKey(channel);
        Long size = redisTemplate.opsForZSet().size(queueKey);
        return size != null ? size : 0;
    }

    /**
     * Get dead letter queue size
     */
    public long getDLQSize() {
        Long size = redisTemplate.opsForZSet().size(DLQ_KEY);
        return size != null ? size : 0;
    }

    /**
     * Get queue key for channel
     */
    private String getQueueKey(NotificationEntity.NotificationChannel channel) {
        return QUEUE_KEY_PREFIX + channel.name().toLowerCase();
    }

    /**
     * Get queue key from notification
     */
    private String getQueueKey(NotificationEntity notification) {
        return getQueueKey(notification.getChannel());
    }
}
```

#### 4.2 Retry Logic Service

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/notification/NotificationRetryService.java`

```java
package com.healthdata.quality.service.notification;

import com.healthdata.quality.config.NotificationProperties;
import com.healthdata.quality.persistence.NotificationEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Retry Logic with Exponential Backoff
 * 
 * Calculates retry delays and determines if notification should be retried
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryService {

    private the NotificationProperties properties;

    /**
     * Calculate retry delay using exponential backoff with jitter
     * 
     * Formula: delay = min(initial * (multiplier ^ attempt), maxDelay) + jitter
     */
    public Duration calculateRetryDelay(int retryCount) {
        var retryConfig = properties.getRetry();
        
        // Calculate base delay
        long baseDelayMs = retryConfig.getInitialDelay().toMillis();
        double multiplier = retryConfig.getMultiplier();
        
        long calculatedDelay = (long) (baseDelayMs * Math.pow(multiplier, retryCount));
        
        // Cap at max delay
        long maxDelayMs = retryConfig.getMaxDelay().toMillis();
        long delayMs = Math.min(calculatedDelay, maxDelayMs);
        
        // Add jitter (±20% random variation to prevent thundering herd)
        double jitter = 0.8 + (Math.random() * 0.4); // 0.8 to 1.2
        delayMs = (long) (delayMs * jitter);
        
        return Duration.ofMillis(delayMs);
    }

    /**
     * Check if notification should be retried
     */
    public boolean shouldRetry(NotificationEntity notification, String errorMessage) {
        // Check retry count
        if (notification.getRetryCount() >= notification.getMaxRetries()) {
            log.info("Notification {} exceeded max retries ({})", 
                notification.getId(), notification.getMaxRetries());
            return false;
        }

        // Check if notification is expired
        if (notification.getExpiresAt() != null && 
            notification.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            log.info("Notification {} expired, not retrying", notification.getId());
            return false;
        }

        // Check if error is retryable
        if (!isRetryableError(errorMessage)) {
            log.info("Notification {} has non-retryable error, not retrying", 
                notification.getId());
            return false;
        }

        return true;
    }

    /**
     * Determine if error is retryable
     */
    private boolean isRetryableError(String errorMessage) {
        if (errorMessage == null) {
            return true;
        }

        String lowerError = errorMessage.toLowerCase();

        // Non-retryable errors
        if (lowerError.contains("invalid recipient") ||
            lowerError.contains("bounced") ||
            lowerError.contains("rejected") ||
            lowerError.contains("unsubscribed") ||
            lowerError.contains("invalid email") ||
            lowerError.contains("invalid phone")) {
            return false;
        }

        // Retryable errors (temporary failures)
        if (lowerError.contains("timeout") ||
            lowerError.contains("rate limit") ||
            lowerError.contains("service unavailable") ||
            lowerError.contains("502") ||
            lowerError.contains("503") ||
            lowerError.contains("504")) {
            return true;
        }

        // Default: retry on unknown errors
        return true;
    }
}
```

#### 4.3 Rate Limiting Service

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/notification/NotificationRateLimiter.java`

```java
package com.healthdata.quality.service.notification;

import com.healthdata.quality.config.NotificationProperties;
import com.healthdata.quality.persistence.NotificationEntity;
import com.healthdata.quality.persistence.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Rate Limiting Service
 * 
 * Prevents notification spam by enforcing per-user and system-wide limits
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRateLimiter {

    private final NotificationRepository notificationRepository;
    private final NotificationProperties properties;

    /**
     * Check if sending notification would exceed rate limit
     */
    public boolean isRateLimited(
        String tenantId,
        String recipient,
        NotificationEntity.NotificationChannel channel,
        NotificationEntity.NotificationSeverity severity
    ) {
        // CRITICAL alerts bypass rate limiting
        if (severity == NotificationEntity.NotificationSeverity.CRITICAL) {
            return false;
        }

        var limits = properties.getRateLimits();

        switch (channel) {
            case EMAIL:
                return isEmailRateLimited(tenantId, recipient, limits);
            case SMS:
                return isSmsRateLimited(tenantId, recipient, limits);
            case PUSH:
                return isPushRateLimited(tenantId, recipient, limits);
            default:
                return false; // No rate limiting for IN_APP and WEBSOCKET
        }
    }

    /**
     * Check email rate limit
     */
    private boolean isEmailRateLimited(
        String tenantId,
        String recipient,
        NotificationProperties.RateLimits limits
    ) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long hourlyCount = notificationRepository.countRecentByRecipient(
            tenantId, recipient, NotificationEntity.NotificationChannel.EMAIL, oneHourAgo
        );

        if (hourlyCount >= limits.getEmailPerHour()) {
            log.warn("Email rate limit exceeded for recipient {}: {} emails in past hour",
                recipient, hourlyCount);
            return true;
        }

        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        long dailyCount = notificationRepository.countRecentByRecipient(
            tenantId, recipient, NotificationEntity.NotificationChannel.EMAIL, oneDayAgo
        );

        if (dailyCount >= limits.getEmailPerDay()) {
            log.warn("Email rate limit exceeded for recipient {}: {} emails in past day",
                recipient, dailyCount);
            return true;
        }

        return false;
    }

    /**
     * Check SMS rate limit
     */
    private boolean isSmsRateLimited(
        String tenantId,
        String recipient,
        NotificationProperties.RateLimits limits
    ) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long hourlyCount = notificationRepository.countRecentByRecipient(
            tenantId, recipient, NotificationEntity.NotificationChannel.SMS, oneHourAgo
        );

        if (hourlyCount >= limits.getSmsPerHour()) {
            log.warn("SMS rate limit exceeded for recipient {}: {} SMS in past hour",
                recipient, hourlyCount);
            return true;
        }

        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        long dailyCount = notificationRepository.countRecentByRecipient(
            tenantId, recipient, NotificationEntity.NotificationChannel.SMS, oneDayAgo
        );

        if (dailyCount >= limits.getSmsPerDay()) {
            log.warn("SMS rate limit exceeded for recipient {}: {} SMS in past day",
                recipient, dailyCount);
            return true;
        }

        return false;
    }

    /**
     * Check push notification rate limit
     */
    private boolean isPushRateLimited(
        String tenantId,
        String recipient,
        NotificationProperties.RateLimits limits
    ) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long hourlyCount = notificationRepository.countRecentByRecipient(
            tenantId, recipient, NotificationEntity.NotificationChannel.PUSH, oneHourAgo
        );

        if (hourlyCount >= limits.getPushPerHour()) {
            log.warn("Push rate limit exceeded for recipient {}: {} push in past hour",
                recipient, hourlyCount);
            return true;
        }

        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        long dailyCount = notificationRepository.countRecentByRecipient(
            tenantId, recipient, NotificationEntity.NotificationChannel.PUSH, oneDayAgo
        );

        if (dailyCount >= limits.getPushPerDay()) {
            log.warn("Push rate limit exceeded for recipient {}: {} push in past day",
                recipient, dailyCount);
            return true;
        }

        return false;
    }
}
```

---

Due to length constraints, I'll continue with the remaining phases in the implementation plan document.

### Continue writing to file...

#### 4.4 Async Notification Processor

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/notification/AsyncNotificationProcessor.java`

```java
package com.healthdata.quality.service.notification;

import com.healthdata.quality.persistence.NotificationEntity;
import com.healthdata.quality.persistence.NotificationRepository;
import com.healthdata.quality.service.notification.provider.NotificationProviderRegistry;
import com.healthdata.quality.service.notification.provider.NotificationRequest;
import com.healthdata.quality.service.notification.provider.ProviderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Async Notification Processor
 * 
 * Processes notifications from queue asynchronously with retry logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncNotificationProcessor {

    private final NotificationQueueService queueService;
    private final NotificationRepository notificationRepository;
    private final NotificationProviderRegistry providerRegistry;
    private final NotificationTemplateService templateService;
    private final NotificationRetryService retryService;
    private final NotificationRateLimiter rateLimiter;

    /**
     * Process notifications from queue (scheduled every 10 seconds)
     */
    @Scheduled(fixedDelay = 10000) // 10 seconds
    public void processQueue() {
        for (NotificationEntity.NotificationChannel channel : NotificationEntity.NotificationChannel.values()) {
            // Skip WEBSOCKET (real-time, not queued)
            if (channel == NotificationEntity.NotificationChannel.WEBSOCKET) {
                continue;
            }

            processChannelQueue(channel);
        }
    }

    /**
     * Process queue for specific channel
     */
    private void processChannelQueue(NotificationEntity.NotificationChannel channel) {
        try {
            while (true) {
                NotificationEntity notification = queueService.dequeue(channel);
                
                if (notification == null) {
                    break; // Queue empty
                }

                processNotificationAsync(notification);
            }
        } catch (Exception e) {
            log.error("Error processing {} queue: {}", channel, e.getMessage());
        }
    }

    /**
     * Process individual notification asynchronously
     */
    @Async("notificationExecutor")
    @Transactional
    public void processNotificationAsync(NotificationEntity notification) {
        try {
            log.info("Processing notification {}", notification.getId());

            // Update status to SENDING
            notification.setStatus(NotificationEntity.NotificationStatus.SENDING);
            notificationRepository.save(notification);

            // Check rate limiting
            if (rateLimiter.isRateLimited(
                notification.getTenantId(),
                notification.getRecipient(),
                notification.getChannel(),
                notification.getSeverity()
            )) {
                log.warn("Notification {} rate limited, requeueing for later", notification.getId());
                notification.setStatus(NotificationEntity.NotificationStatus.PENDING);
                queueService.enqueueForRetry(notification, Duration.ofMinutes(15));
                return;
            }

            // Render template
            NotificationRequest request = buildNotificationRequest(notification);

            // Send via provider with failover
            ProviderResponse response = providerRegistry.sendWithFailover(
                notification.getChannel(),
                request
            );

            if (response.isSuccess()) {
                // Success
                notification.setStatus(NotificationEntity.NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notification.setProvider(response.getProviderName());
                notification.setProviderMessageId(response.getProviderMessageId());
                
                log.info("Notification {} sent successfully via {}", 
                    notification.getId(), response.getProviderName());

            } else {
                // Failure - check if should retry
                if (retryService.shouldRetry(notification, response.getErrorMessage())) {
                    notification.setStatus(NotificationEntity.NotificationStatus.FAILED);
                    notification.setErrorMessage(response.getErrorMessage());
                    notification.setFailedAt(LocalDateTime.now());
                    
                    Duration retryDelay = retryService.calculateRetryDelay(notification.getRetryCount());
                    queueService.enqueueForRetry(notification, retryDelay);
                    
                    log.warn("Notification {} failed, retrying in {}", 
                        notification.getId(), retryDelay);

                } else {
                    // Permanent failure - move to DLQ
                    notification.setStatus(NotificationEntity.NotificationStatus.FAILED);
                    notification.setErrorMessage(response.getErrorMessage());
                    notification.setFailedAt(LocalDateTime.now());
                    
                    queueService.moveToDLQ(notification, response.getErrorMessage());
                    
                    log.error("Notification {} permanently failed: {}", 
                        notification.getId(), response.getErrorMessage());
                }
            }

            notificationRepository.save(notification);
            queueService.markComplete(notification);

        } catch (Exception e) {
            log.error("Error processing notification {}: {}", 
                notification.getId(), e.getMessage());
            
            notification.setStatus(NotificationEntity.NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);
        }
    }

    /**
     * Build notification request from entity
     */
    private NotificationRequest buildNotificationRequest(NotificationEntity notification) {
        // Get template variables from metadata
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = notification.getMetadata() != null 
            ? notification.getMetadata() 
            : new HashMap<>();

        NotificationRequest.NotificationRequestBuilder builder = NotificationRequest.builder()
            .tenantId(notification.getTenantId())
            .recipient(notification.getRecipient())
            .metadata(notification.getMetadata());

        // Render templates based on channel
        if (notification.getChannel() == NotificationEntity.NotificationChannel.EMAIL) {
            var template = templateService.getTemplateByType(
                notification.getType(),
                NotificationEntity.NotificationChannel.EMAIL
            );

            builder.subject(templateService.renderSubject(template, variables))
                   .htmlBody(templateService.renderHtml(template, variables))
                   .textBody(templateService.renderText(template, variables));

        } else if (notification.getChannel() == NotificationEntity.NotificationChannel.SMS) {
            var template = templateService.getTemplateByType(
                notification.getType(),
                NotificationEntity.NotificationChannel.SMS
            );

            builder.smsBody(templateService.renderSms(template, variables));
        }

        return builder.build();
    }

    /**
     * Process retry queue (every 5 minutes)
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    @Transactional
    public void processRetryQueue() {
        log.debug("Processing retry queue");
        
        LocalDateTime now = LocalDateTime.now();
        
        // Find notifications ready for retry
        var notifications = notificationRepository.findPendingRetries(
            "all", // Process for all tenants
            now,
            org.springframework.data.domain.PageRequest.of(0, 100)
        );

        for (NotificationEntity notification : notifications) {
            log.info("Retrying notification {}", notification.getId());
            queueService.enqueue(notification);
        }
    }

    /**
     * Clean up expired notifications (daily)
     */
    @Scheduled(cron = "0 0 2 * * *") // 2 AM daily
    @Transactional
    public void cleanupExpiredNotifications() {
        log.info("Cleaning up expired notifications");
        
        LocalDateTime now = LocalDateTime.now();
        
        var expiredNotifications = notificationRepository.findExpiredNotifications("all", now);
        
        for (NotificationEntity notification : expiredNotifications) {
            notification.setStatus(NotificationEntity.NotificationStatus.EXPIRED);
            notificationRepository.save(notification);
        }
        
        log.info("Marked {} notifications as expired", expiredNotifications.size());
    }
}
```

#### 4.5 Async Configuration

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/config/NotificationAsyncConfig.java`

```java
package com.healthdata.quality.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration for Notification Processing
 */
@Configuration
@EnableAsync
@EnableScheduling
public class NotificationAsyncConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notification-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
```

### Testing Phase 4

**Unit Tests**:
- Queue enqueue/dequeue operations
- Retry delay calculation
- Rate limiting logic
- Exponential backoff algorithm

**Integration Tests**:
- End-to-end notification processing
- Failover scenarios
- Retry queue processing
- Dead letter queue handling

**Load Tests**:
- 1000 notifications/minute
- Queue performance under load
- Rate limiting effectiveness

**Success Criteria**:
- ✅ Notifications process asynchronously
- ✅ Retries work with exponential backoff
- ✅ Rate limiting prevents spam
- ✅ DLQ captures permanent failures

---

## PHASE 5: User Preferences API & Management (Week 6-7)

### Goals
- Create REST API for preference management
- Implement quiet hours enforcement
- Add consent management workflows
- Build unsubscribe handling

### Tasks

#### 5.1 Preference Service

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/notification/NotificationPreferenceService.java`

```java
package com.healthdata.quality.service.notification;

import com.healthdata.quality.persistence.NotificationEntity;
import com.healthdata.quality.persistence.NotificationPreferenceEntity;
import com.healthdata.quality.persistence.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

/**
 * Notification Preference Management Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    /**
     * Get user preferences (create default if not exists)
     */
    @Transactional
    public NotificationPreferenceEntity getOrCreatePreferences(String userId, String tenantId) {
        return preferenceRepository.findByUserIdAndTenantId(userId, tenantId)
            .orElseGet(() -> createDefaultPreferences(userId, tenantId));
    }

    /**
     * Create default preferences for new user
     */
    private NotificationPreferenceEntity createDefaultPreferences(String userId, String tenantId) {
        NotificationPreferenceEntity preferences = NotificationPreferenceEntity.builder()
            .userId(userId)
            .tenantId(tenantId)
            .emailEnabled(true)
            .smsEnabled(false)
            .pushEnabled(true)
            .inAppEnabled(true)
            .severityThreshold(NotificationEntity.NotificationSeverity.MEDIUM)
            .quietHoursEnabled(false)
            .quietHoursOverrideCritical(true)
            .digestModeEnabled(false)
            .digestFrequency(NotificationPreferenceEntity.DigestFrequency.DAILY)
            .consentGiven(false) // Must explicitly opt-in
            .enabledTypes(new HashSet<>()) // All types enabled by default
            .build();

        return preferenceRepository.save(preferences);
    }

    /**
     * Update user preferences
     */
    @Transactional
    public NotificationPreferenceEntity updatePreferences(
        String userId,
        String tenantId,
        NotificationPreferenceEntity updates
    ) {
        NotificationPreferenceEntity existing = getOrCreatePreferences(userId, tenantId);

        // Update channel preferences
        if (updates.getEmailEnabled() != null) {
            existing.setEmailEnabled(updates.getEmailEnabled());
        }
        if (updates.getSmsEnabled() != null) {
            existing.setSmsEnabled(updates.getSmsEnabled());
        }
        if (updates.getPushEnabled() != null) {
            existing.setPushEnabled(updates.getPushEnabled());
        }
        if (updates.getInAppEnabled() != null) {
            existing.setInAppEnabled(updates.getInAppEnabled());
        }

        // Update contact information
        if (updates.getEmailAddress() != null) {
            existing.setEmailAddress(updates.getEmailAddress());
        }
        if (updates.getPhoneNumber() != null) {
            existing.setPhoneNumber(updates.getPhoneNumber());
        }

        // Update severity threshold
        if (updates.getSeverityThreshold() != null) {
            existing.setSeverityThreshold(updates.getSeverityThreshold());
        }

        // Update quiet hours
        if (updates.getQuietHoursEnabled() != null) {
            existing.setQuietHoursEnabled(updates.getQuietHoursEnabled());
        }
        if (updates.getQuietHoursStart() != null) {
            existing.setQuietHoursStart(updates.getQuietHoursStart());
        }
        if (updates.getQuietHoursEnd() != null) {
            existing.setQuietHoursEnd(updates.getQuietHoursEnd());
        }
        if (updates.getQuietHoursOverrideCritical() != null) {
            existing.setQuietHoursOverrideCritical(updates.getQuietHoursOverrideCritical());
        }

        // Update digest mode
        if (updates.getDigestModeEnabled() != null) {
            existing.setDigestModeEnabled(updates.getDigestModeEnabled());
        }
        if (updates.getDigestFrequency() != null) {
            existing.setDigestFrequency(updates.getDigestFrequency());
        }

        // Update type filters
        if (updates.getEnabledTypes() != null) {
            existing.setEnabledTypes(updates.getEnabledTypes());
        }

        return preferenceRepository.save(existing);
    }

    /**
     * Grant notification consent (HIPAA compliance)
     */
    @Transactional
    public void grantConsent(String userId, String tenantId) {
        NotificationPreferenceEntity preferences = getOrCreatePreferences(userId, tenantId);
        preferences.setConsentGiven(true);
        preferences.setConsentDate(LocalDateTime.now());
        preferenceRepository.save(preferences);

        log.info("User {} granted notification consent for tenant {}", userId, tenantId);
    }

    /**
     * Revoke notification consent
     */
    @Transactional
    public void revokeConsent(String userId, String tenantId) {
        NotificationPreferenceEntity preferences = getOrCreatePreferences(userId, tenantId);
        preferences.setConsentGiven(false);
        preferenceRepository.save(preferences);

        log.info("User {} revoked notification consent for tenant {}", userId, tenantId);
    }

    /**
     * Unsubscribe from specific channel
     */
    @Transactional
    public void unsubscribeChannel(
        String userId,
        String tenantId,
        NotificationEntity.NotificationChannel channel
    ) {
        NotificationPreferenceEntity preferences = getOrCreatePreferences(userId, tenantId);

        switch (channel) {
            case EMAIL -> preferences.setEmailEnabled(false);
            case SMS -> preferences.setSmsEnabled(false);
            case PUSH -> preferences.setPushEnabled(false);
            case IN_APP -> preferences.setInAppEnabled(false);
        }

        preferenceRepository.save(preferences);
        log.info("User {} unsubscribed from {} for tenant {}", userId, channel, tenantId);
    }

    /**
     * Check if user should receive notification
     */
    public boolean shouldSendNotification(
        String userId,
        String tenantId,
        NotificationEntity.NotificationChannel channel,
        NotificationEntity.NotificationType type,
        NotificationEntity.NotificationSeverity severity
    ) {
        Optional<NotificationPreferenceEntity> preferencesOpt = 
            preferenceRepository.findByUserIdAndTenantId(userId, tenantId);

        if (preferencesOpt.isEmpty()) {
            // No preferences set - use defaults (don't send without consent)
            return false;
        }

        NotificationPreferenceEntity preferences = preferencesOpt.get();
        return preferences.shouldReceive(channel, type, severity);
    }
}
```

#### 5.2 Preference REST Controller

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/controller/NotificationPreferenceController.java`

```java
package com.healthdata.quality.controller;

import com.healthdata.quality.dto.NotificationPreferenceDTO;
import com.healthdata.quality.persistence.NotificationEntity;
import com.healthdata.quality.persistence.NotificationPreferenceEntity;
import com.healthdata.quality.service.notification.NotificationPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Notification Preference Management API
 */
@RestController
@RequestMapping("/api/notifications/preferences")
@RequiredArgsConstructor
@Tag(name = "Notification Preferences", description = "User notification preference management")
@SecurityRequirement(name = "bearer-jwt")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    /**
     * Get current user's notification preferences
     */
    @GetMapping
    @Operation(summary = "Get notification preferences")
    public ResponseEntity<NotificationPreferenceDTO> getPreferences(
        Authentication authentication,
        @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        String userId = authentication.getName();
        
        NotificationPreferenceEntity preferences = 
            preferenceService.getOrCreatePreferences(userId, tenantId);
        
        return ResponseEntity.ok(NotificationPreferenceDTO.fromEntity(preferences));
    }

    /**
     * Update notification preferences
     */
    @PutMapping
    @Operation(summary = "Update notification preferences")
    public ResponseEntity<NotificationPreferenceDTO> updatePreferences(
        @RequestBody NotificationPreferenceDTO dto,
        Authentication authentication,
        @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        String userId = authentication.getName();
        
        NotificationPreferenceEntity updates = dto.toEntity();
        NotificationPreferenceEntity updated = 
            preferenceService.updatePreferences(userId, tenantId, updates);
        
        return ResponseEntity.ok(NotificationPreferenceDTO.fromEntity(updated));
    }

    /**
     * Grant notification consent
     */
    @PostMapping("/consent")
    @Operation(summary = "Grant notification consent")
    public ResponseEntity<Void> grantConsent(
        Authentication authentication,
        @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        String userId = authentication.getName();
        preferenceService.grantConsent(userId, tenantId);
        return ResponseEntity.ok().build();
    }

    /**
     * Revoke notification consent
     */
    @DeleteMapping("/consent")
    @Operation(summary = "Revoke notification consent")
    public ResponseEntity<Void> revokeConsent(
        Authentication authentication,
        @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        String userId = authentication.getName();
        preferenceService.revokeConsent(userId, tenantId);
        return ResponseEntity.ok().build();
    }

    /**
     * Unsubscribe from specific channel
     */
    @PostMapping("/unsubscribe/{channel}")
    @Operation(summary = "Unsubscribe from notification channel")
    public ResponseEntity<Void> unsubscribeChannel(
        @PathVariable NotificationEntity.NotificationChannel channel,
        Authentication authentication,
        @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        String userId = authentication.getName();
        preferenceService.unsubscribeChannel(userId, tenantId, channel);
        return ResponseEntity.ok().build();
    }

    /**
     * One-click unsubscribe (for email links)
     */
    @GetMapping("/unsubscribe")
    @Operation(summary = "One-click unsubscribe from email link")
    public ResponseEntity<String> unsubscribeFromEmail(
        @RequestParam String token
    ) {
        // TODO: Implement token-based unsubscribe for email links
        // Decode token to get userId, tenantId, and channel
        // Then call preferenceService.unsubscribeChannel()
        
        return ResponseEntity.ok("You have been successfully unsubscribed.");
    }
}
```

#### 5.3 Preference DTO

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/dto/NotificationPreferenceDTO.java`

```java
package com.healthdata.quality.dto;

import com.healthdata.quality.persistence.NotificationEntity;
import com.healthdata.quality.persistence.NotificationPreferenceEntity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Data
@Builder
public class NotificationPreferenceDTO {
    
    // Channel Preferences
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private Boolean pushEnabled;
    private Boolean inAppEnabled;
    
    // Contact Information
    private String emailAddress;
    private String phoneNumber;
    
    // Filters
    private Set<String> enabledTypes;
    private String severityThreshold;
    
    // Quiet Hours
    private Boolean quietHoursEnabled;
    private String quietHoursStart;
    private String quietHoursEnd;
    private Boolean quietHoursOverrideCritical;
    
    // Digest Mode
    private Boolean digestModeEnabled;
    private String digestFrequency;
    
    // Consent
    private Boolean consentGiven;
    private LocalDateTime consentDate;

    public static NotificationPreferenceDTO fromEntity(NotificationPreferenceEntity entity) {
        return NotificationPreferenceDTO.builder()
            .emailEnabled(entity.getEmailEnabled())
            .smsEnabled(entity.getSmsEnabled())
            .pushEnabled(entity.getPushEnabled())
            .inAppEnabled(entity.getInAppEnabled())
            .emailAddress(entity.getEmailAddress())
            .phoneNumber(entity.getPhoneNumber())
            .enabledTypes(entity.getEnabledTypes())
            .severityThreshold(entity.getSeverityThreshold() != null 
                ? entity.getSeverityThreshold().name() : null)
            .quietHoursEnabled(entity.getQuietHoursEnabled())
            .quietHoursStart(entity.getQuietHoursStart() != null 
                ? entity.getQuietHoursStart().toString() : null)
            .quietHoursEnd(entity.getQuietHoursEnd() != null 
                ? entity.getQuietHoursEnd().toString() : null)
            .quietHoursOverrideCritical(entity.getQuietHoursOverrideCritical())
            .digestModeEnabled(entity.getDigestModeEnabled())
            .digestFrequency(entity.getDigestFrequency() != null 
                ? entity.getDigestFrequency().name() : null)
            .consentGiven(entity.getConsentGiven())
            .consentDate(entity.getConsentDate())
            .build();
    }

    public NotificationPreferenceEntity toEntity() {
        return NotificationPreferenceEntity.builder()
            .emailEnabled(emailEnabled)
            .smsEnabled(smsEnabled)
            .pushEnabled(pushEnabled)
            .inAppEnabled(inAppEnabled)
            .emailAddress(emailAddress)
            .phoneNumber(phoneNumber)
            .enabledTypes(enabledTypes)
            .severityThreshold(severityThreshold != null 
                ? NotificationEntity.NotificationSeverity.valueOf(severityThreshold) : null)
            .quietHoursEnabled(quietHoursEnabled)
            .quietHoursStart(quietHoursStart != null 
                ? LocalTime.parse(quietHoursStart) : null)
            .quietHoursEnd(quietHoursEnd != null 
                ? LocalTime.parse(quietHoursEnd) : null)
            .quietHoursOverrideCritical(quietHoursOverrideCritical)
            .digestModeEnabled(digestModeEnabled)
            .digestFrequency(digestFrequency != null 
                ? NotificationPreferenceEntity.DigestFrequency.valueOf(digestFrequency) : null)
            .build();
    }
}
```

### Testing Phase 5

**Unit Tests**:
- Preference update logic
- Quiet hours calculation
- Consent management
- shouldReceive filtering logic

**Integration Tests**:
- REST API endpoints
- Preference persistence
- Unsubscribe workflows

**Success Criteria**:
- ✅ Users can manage preferences via API
- ✅ Quiet hours enforced correctly
- ✅ Consent tracked properly
- ✅ Unsubscribe links work

---

## PHASE 6: Monitoring, Analytics & HIPAA Compliance (Week 7-8)

### Goals
- Implement delivery metrics collection
- Create monitoring dashboard endpoints
- Add audit logging
- Ensure HIPAA compliance

### Tasks

#### 6.1 Notification Analytics Service

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/notification/NotificationAnalyticsService.java`

```java
package com.healthdata.quality.service.notification;

import com.healthdata.quality.persistence.NotificationEntity;
import com.healthdata.quality.persistence.NotificationRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Notification Analytics Service
 * 
 * Provides metrics and analytics for notification delivery
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationAnalyticsService {

    private final NotificationRepository notificationRepository;

    /**
     * Get notification metrics for time period
     */
    public NotificationMetrics getMetrics(String tenantId, LocalDateTime since) {
        // Count by status
        Map<String, Long> statusCounts = new HashMap<>();
        List<Object[]> statusResults = notificationRepository.countByStatusSince(tenantId, since);
        for (Object[] row : statusResults) {
            NotificationEntity.NotificationStatus status = (NotificationEntity.NotificationStatus) row[0];
            Long count = (Long) row[1];
            statusCounts.put(status.name(), count);
        }

        // Count by channel and status
        Map<String, Map<String, Long>> channelMetrics = new HashMap<>();
        List<Object[]> channelResults = notificationRepository.countByChannelAndStatusSince(tenantId, since);
        for (Object[] row : channelResults) {
            NotificationEntity.NotificationChannel channel = (NotificationEntity.NotificationChannel) row[0];
            Long count = (Long) row[1];
            NotificationEntity.NotificationStatus status = (NotificationEntity.NotificationStatus) row[2];
            
            channelMetrics.computeIfAbsent(channel.name(), k -> new HashMap<>())
                .put(status.name(), count);
        }

        // Calculate delivery rates
        Map<String, Double> deliveryRates = new HashMap<>();
        for (NotificationEntity.NotificationChannel channel : NotificationEntity.NotificationChannel.values()) {
            Double rate = notificationRepository.calculateDeliveryRate(tenantId, channel, since);
            if (rate != null) {
                deliveryRates.put(channel.name(), rate);
            }
        }

        // Calculate average delivery times
        Map<String, Double> avgDeliveryTimes = new HashMap<>();
        for (NotificationEntity.NotificationChannel channel : NotificationEntity.NotificationChannel.values()) {
            Double avgTime = notificationRepository.calculateAverageDeliveryTimeSeconds(tenantId, channel, since);
            if (avgTime != null) {
                avgDeliveryTimes.put(channel.name(), avgTime);
            }
        }

        return NotificationMetrics.builder()
            .tenantId(tenantId)
            .period(since.toString() + " to " + LocalDateTime.now().toString())
            .totalNotifications(statusCounts.values().stream().mapToLong(Long::longValue).sum())
            .statusCounts(statusCounts)
            .channelMetrics(channelMetrics)
            .deliveryRates(deliveryRates)
            .averageDeliveryTimes(avgDeliveryTimes)
            .build();
    }

    /**
     * Get provider performance metrics
     */
    public List<ProviderMetrics> getProviderMetrics(String tenantId, LocalDateTime since) {
        List<Object[]> results = notificationRepository.countByProviderAndStatus(tenantId, since);
        
        Map<String, Map<String, Long>> providerStats = new HashMap<>();
        for (Object[] row : results) {
            String provider = (String) row[0];
            Long count = (Long) row[1];
            NotificationEntity.NotificationStatus status = (NotificationEntity.NotificationStatus) row[2];
            
            providerStats.computeIfAbsent(provider, k -> new HashMap<>())
                .put(status.name(), count);
        }

        return providerStats.entrySet().stream()
            .map(entry -> {
                String provider = entry.getKey();
                Map<String, Long> stats = entry.getValue();
                
                long total = stats.values().stream().mapToLong(Long::longValue).sum();
                long delivered = stats.getOrDefault("DELIVERED", 0L);
                long failed = stats.getOrDefault("FAILED", 0L);
                
                double successRate = total > 0 ? (delivered * 100.0 / total) : 0.0;
                
                return ProviderMetrics.builder()
                    .providerName(provider)
                    .totalSent(total)
                    .delivered(delivered)
                    .failed(failed)
                    .successRate(successRate)
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Notification metrics DTO
     */
    @Data
    @Builder
    public static class NotificationMetrics {
        private String tenantId;
        private String period;
        private Long totalNotifications;
        private Map<String, Long> statusCounts;
        private Map<String, Map<String, Long>> channelMetrics;
        private Map<String, Double> deliveryRates;
        private Map<String, Double> averageDeliveryTimes;
    }

    /**
     * Provider performance metrics
     */
    @Data
    @Builder
    public static class ProviderMetrics {
        private String providerName;
        private Long totalSent;
        private Long delivered;
        private Long failed;
        private Double successRate;
    }
}
```

#### 6.2 Monitoring Controller

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/controller/NotificationMonitoringController.java`

```java
package com.healthdata.quality.controller;

import com.healthdata.quality.service.notification.NotificationAnalyticsService;
import com.healthdata.quality.service.notification.NotificationQueueService;
import com.healthdata.quality.service.notification.provider.NotificationProviderRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Notification Monitoring API
 */
@RestController
@RequestMapping("/api/notifications/monitoring")
@RequiredArgsConstructor
@Tag(name = "Notification Monitoring", description = "Notification system monitoring and metrics")
@SecurityRequirement(name = "bearer-jwt")
public class NotificationMonitoringController {

    private final NotificationAnalyticsService analyticsService;
    private final NotificationQueueService queueService;
    private final NotificationProviderRegistry providerRegistry;

    /**
     * Get notification metrics for last 24 hours
     */
    @GetMapping("/metrics")
    @Operation(summary = "Get notification delivery metrics")
    public ResponseEntity<NotificationAnalyticsService.NotificationMetrics> getMetrics(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam(required = false, defaultValue = "24") int hours
    ) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        var metrics = analyticsService.getMetrics(tenantId, since);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get provider performance comparison
     */
    @GetMapping("/providers")
    @Operation(summary = "Get provider performance metrics")
    public ResponseEntity<?> getProviderMetrics(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam(required = false, defaultValue = "24") int hours
    ) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        var metrics = analyticsService.getProviderMetrics(tenantId, since);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get provider health status
     */
    @GetMapping("/providers/health")
    @Operation(summary = "Get provider health status")
    public ResponseEntity<?> getProviderHealth() {
        var health = providerRegistry.checkAllProvidersHealth();
        return ResponseEntity.ok(health);
    }

    /**
     * Get queue status
     */
    @GetMapping("/queues")
    @Operation(summary = "Get queue status")
    public ResponseEntity<QueueStatus> getQueueStatus() {
        return ResponseEntity.ok(QueueStatus.builder()
            .emailQueueSize(queueService.getQueueSize(
                com.healthdata.quality.persistence.NotificationEntity.NotificationChannel.EMAIL))
            .smsQueueSize(queueService.getQueueSize(
                com.healthdata.quality.persistence.NotificationEntity.NotificationChannel.SMS))
            .pushQueueSize(queueService.getQueueSize(
                com.healthdata.quality.persistence.NotificationEntity.NotificationChannel.PUSH))
            .deadLetterQueueSize(queueService.getDLQSize())
            .timestamp(LocalDateTime.now())
            .build());
    }

    @Data
    @Builder
    private static class QueueStatus {
        private Long emailQueueSize;
        private Long smsQueueSize;
        private Long pushQueueSize;
        private Long deadLetterQueueSize;
        private LocalDateTime timestamp;
    }
}
```

#### 6.3 HIPAA Audit Logging

**File**: `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/notification/NotificationAuditService.java`

```java
package com.healthdata.quality.service.notification;

import com.healthdata.quality.persistence.NotificationEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * HIPAA-Compliant Audit Logging for Notifications
 * 
 * Logs all notification events for compliance and security auditing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationAuditService {

    /**
     * Log notification sent event
     */
    public void logNotificationSent(NotificationEntity notification) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event", "NOTIFICATION_SENT");
        auditData.put("notification_id", notification.getId());
        auditData.put("tenant_id", notification.getTenantId());
        auditData.put("user_id", notification.getUserId());
        auditData.put("patient_id", notification.getPatientId());
        auditData.put("channel", notification.getChannel());
        auditData.put("type", notification.getType());
        auditData.put("severity", notification.getSeverity());
        auditData.put("provider", notification.getProvider());
        auditData.put("timestamp", LocalDateTime.now());

        log.info("AUDIT: {}", auditData);
    }

    /**
     * Log notification failed event
     */
    public void logNotificationFailed(NotificationEntity notification, String reason) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event", "NOTIFICATION_FAILED");
        auditData.put("notification_id", notification.getId());
        auditData.put("tenant_id", notification.getTenantId());
        auditData.put("user_id", notification.getUserId());
        auditData.put("channel", notification.getChannel());
        auditData.put("reason", reason);
        auditData.put("retry_count", notification.getRetryCount());
        auditData.put("timestamp", LocalDateTime.now());

        log.warn("AUDIT: {}", auditData);
    }

    /**
     * Log preference change event
     */
    public void logPreferenceChange(String userId, String tenantId, String changeType, Map<String, Object> changes) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event", "PREFERENCE_CHANGED");
        auditData.put("user_id", userId);
        auditData.put("tenant_id", tenantId);
        auditData.put("change_type", changeType);
        auditData.put("changes", changes);
        auditData.put("timestamp", LocalDateTime.now());

        log.info("AUDIT: {}", auditData);
    }

    /**
     * Log consent event
     */
    public void logConsentChange(String userId, String tenantId, boolean granted) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event", granted ? "CONSENT_GRANTED" : "CONSENT_REVOKED");
        auditData.put("user_id", userId);
        auditData.put("tenant_id", tenantId);
        auditData.put("timestamp", LocalDateTime.now());

        log.info("AUDIT: {}", auditData);
    }

    /**
     * Log unsubscribe event
     */
    public void logUnsubscribe(String userId, String tenantId, NotificationEntity.NotificationChannel channel) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event", "UNSUBSCRIBED");
        auditData.put("user_id", userId);
        auditData.put("tenant_id", tenantId);
        auditData.put("channel", channel);
        auditData.put("timestamp", LocalDateTime.now());

        log.info("AUDIT: {}", auditData);
    }
}
```

### Testing Phase 6

**Unit Tests**:
- Metrics calculation accuracy
- Audit log formatting
- Queue size monitoring

**Integration Tests**:
- Monitoring API endpoints
- Provider health checks
- Analytics queries

**Compliance Tests**:
- Audit log completeness
- PHI redaction in logs
- Data retention policies

**Success Criteria**:
- ✅ Metrics dashboard functional
- ✅ All events logged for audit
- ✅ HIPAA compliance verified
- ✅ Monitoring alerts configured

---

## DEPLOYMENT GUIDE

### Prerequisites
1. PostgreSQL 14+ with JSONB support
2. Redis 6+ for queue management
3. SendGrid account (optional, for email)
4. Twilio account (optional, for SMS)
5. AWS account (optional, for SES/SNS)

### Environment Variables

```bash
# Database
DB_PASSWORD=your_db_password

# SendGrid (optional)
SENDGRID_ENABLED=true
SENDGRID_API_KEY=SG.xxxxxxxxx
SENDGRID_FROM_EMAIL=alerts@healthdata.com
SENDGRID_FROM_NAME=HealthData Alerts

# AWS (optional)
AWS_SES_ENABLED=false
AWS_SNS_ENABLED=false
AWS_REGION=us-east-1
AWS_ACCESS_KEY=your_access_key
AWS_SECRET_KEY=your_secret_key

# Twilio (optional)
TWILIO_ENABLED=false
TWILIO_ACCOUNT_SID=ACxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token
TWILIO_FROM_NUMBER=+1234567890

# SMTP (development)
SMTP_ENABLED=true
SMTP_HOST=localhost
SMTP_PORT=1025
```

### Step-by-Step Deployment

#### 1. Database Setup
```bash
# Run Liquibase migrations
cd backend/modules/services/quality-measure-service
./gradlew update
```

#### 2. Build Application
```bash
cd backend
./gradlew :quality-measure-service:build
```

#### 3. Start Redis
```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

#### 4. Start MailHog (development)
```bash
docker run -d --name mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

#### 5. Run Application
```bash
java -jar quality-measure-service/build/libs/quality-measure-service-*.jar
```

#### 6. Verify Deployment
```bash
# Check health
curl http://localhost:8087/quality-measure/actuator/health

# Check provider health
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8087/quality-measure/api/notifications/monitoring/providers/health
```

---

## TESTING STRATEGY

### Unit Tests (Target: 80% coverage)
- Service layer logic
- Template rendering
- Rate limiting calculations
- Retry delay algorithm
- Provider failover logic

### Integration Tests
- Database operations
- Provider API calls (with mocks)
- Queue processing
- End-to-end notification flow

### E2E Tests
- Send notification via API
- Verify delivery to MailHog
- Check database tracking
- Verify retry on failure
- Test preference enforcement

### Load Tests
- 1,000 notifications/minute
- Queue performance
- Database query optimization
- Provider rate limiting

### Security Tests
- HIPAA compliance verification
- PHI encryption at rest
- TLS for all external calls
- Audit log completeness

---

## HIPAA COMPLIANCE CHECKLIST

- [ ] **Encryption at Rest**: All PHI encrypted in database (AES-256)
- [ ] **Encryption in Transit**: TLS 1.3 for all provider API calls
- [ ] **Access Control**: Role-based access to notification data
- [ ] **Audit Logging**: All notification events logged
- [ ] **Data Minimization**: Only necessary PHI in notifications
- [ ] **Consent Management**: Explicit user consent required
- [ ] **Unsubscribe**: Easy opt-out mechanism
- [ ] **Data Retention**: Automated cleanup after retention period
- [ ] **PHI Redaction**: No PHI in application logs
- [ ] **Business Associate Agreement**: BAA with SendGrid, Twilio
- [ ] **Breach Notification**: Process for notification failures
- [ ] **Data Integrity**: Checksums for notification content

---

## PERFORMANCE OPTIMIZATION

### Database Optimizations
1. **Indexes**: 
   - Composite indexes on (status, next_retry_at, tenant_id)
   - GIN indexes on JSONB columns
2. **Partitioning**: Partition notifications table by created_at (monthly)
3. **Connection Pooling**: HikariCP with 10-20 connections

### Queue Optimizations
1. **Redis Pipeline**: Batch queue operations
2. **Priority Queue**: Use sorted sets for priority handling
3. **Worker Scaling**: Auto-scale workers based on queue depth

### Template Caching
1. **Spring Cache**: Cache rendered templates
2. **Template Versioning**: Invalidate cache on template updates
3. **CDN**: Cache static assets for email templates

### Provider Optimizations
1. **Connection Reuse**: Persistent HTTP connections
2. **Async Calls**: Non-blocking I/O for provider APIs
3. **Circuit Breaker**: Prevent cascading failures

---

## MONITORING & ALERTING

### Metrics to Monitor
- Notifications sent per minute
- Delivery success rate by channel
- Average delivery time
- Queue depth
- Dead letter queue size
- Provider health status
- Error rate by provider

### Alert Thresholds
- **Critical**: Delivery rate < 90% for 10 minutes
- **Warning**: Queue depth > 1000 notifications
- **Warning**: DLQ size > 100
- **Critical**: All providers unhealthy
- **Warning**: Provider failure rate > 10%

### Dashboards
1. **Operational Dashboard**:
   - Real-time delivery metrics
   - Queue depths
   - Provider health
2. **Analytics Dashboard**:
   - Historical trends
   - Channel comparison
   - Cost analysis
3. **Compliance Dashboard**:
   - Audit log summary
   - Consent statistics
   - Data retention status

---

## ROLLOUT PLAN

### Phase 1: Internal Testing (Week 9)
- Deploy to staging environment
- Test with internal users only
- Verify all providers work
- Load test with synthetic traffic

### Phase 2: Pilot Rollout (Week 10)
- Enable for 1-2 pilot tenants
- Monitor metrics closely
- Gather user feedback
- Fix any issues

### Phase 3: Gradual Rollout (Week 11-12)
- Enable for 25% of tenants
- Monitor performance
- Scale resources as needed
- Enable for 50%, then 100%

### Phase 4: Full Production (Week 13+)
- All tenants migrated
- Legacy notification system deprecated
- Ongoing monitoring and optimization

---

## MAINTENANCE & SUPPORT

### Daily Tasks
- Monitor queue depths
- Check provider health
- Review error logs
- Verify delivery rates

### Weekly Tasks
- Review analytics trends
- Check DLQ for patterns
- Update templates as needed
- Performance optimization

### Monthly Tasks
- Provider cost analysis
- Audit log review
- Security assessment
- Compliance verification

---

## TROUBLESHOOTING GUIDE

### Issue: Notifications Not Sending
**Symptoms**: Queue growing, no deliveries
**Diagnosis**:
1. Check provider health: `/api/notifications/monitoring/providers/health`
2. Check queue status: `/api/notifications/monitoring/queues`
3. Review application logs for errors
**Resolution**:
- Verify provider credentials
- Check network connectivity
- Restart async workers

### Issue: High Delivery Failures
**Symptoms**: Low delivery rate, many failures
**Diagnosis**:
1. Check provider metrics: `/api/notifications/monitoring/providers`
2. Review error messages in DLQ
3. Check rate limiting
**Resolution**:
- Switch to backup provider
- Adjust rate limits
- Fix template issues

### Issue: Performance Degradation
**Symptoms**: Slow delivery, high latency
**Diagnosis**:
1. Check database query performance
2. Monitor Redis performance
3. Review thread pool utilization
**Resolution**:
- Optimize slow queries
- Increase worker pool size
- Scale Redis if needed

---

## SUCCESS METRICS

### Technical Metrics
- **Delivery Rate**: > 95% successful delivery
- **Average Delivery Time**: < 30 seconds
- **Queue Processing**: < 60 seconds end-to-end
- **Uptime**: 99.9% availability

### Business Metrics
- **User Satisfaction**: > 4.5/5 rating
- **Unsubscribe Rate**: < 2%
- **Engagement**: > 80% open rate for critical alerts
- **Cost**: < $0.02 per notification

### Compliance Metrics
- **Audit Coverage**: 100% of events logged
- **Data Retention**: 100% compliance
- **Consent**: 100% users have explicit consent
- **Security**: Zero PHI breaches

---

## CONCLUSION

This implementation plan provides a comprehensive, production-ready notification engine for HealthData in Motion. The phased approach allows for incremental development and testing, while the HIPAA compliance focus ensures regulatory adherence.

**Key Takeaways**:
1. **Multi-Channel**: Email, SMS, Push, In-App, WebSocket
2. **Reliable**: Auto-failover, retry logic, queue-based processing
3. **User-Controlled**: Preferences, quiet hours, consent management
4. **Compliant**: HIPAA audit logging, encryption, data minimization
5. **Scalable**: Async processing, Redis queue, optimized queries
6. **Observable**: Comprehensive metrics, monitoring, alerting

**Timeline Summary**:
- **Weeks 1-2**: Foundation & Data Layer
- **Weeks 2-3**: Template System
- **Weeks 3-5**: Provider Integrations
- **Weeks 5-6**: Delivery Pipeline
- **Weeks 6-7**: User Preferences API
- **Weeks 7-8**: Monitoring & Compliance
- **Weeks 9-13**: Testing & Rollout

**Estimated Effort**: 400-500 development hours (2-3 developers)

---

**Document Version**: 1.0.0
**Last Updated**: November 26, 2025
**Status**: Ready for Implementation
