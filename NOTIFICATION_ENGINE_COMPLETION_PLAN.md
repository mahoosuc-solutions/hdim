# Notification Engine - Complete Implementation Plan

**Role**: Software Architect
**Date**: November 27, 2025
**Status**: Implementation Roadmap

---

## Executive Summary

This document provides a complete implementation roadmap for the production-ready notification engine. The architecture (defined in `NOTIFICATION_ENGINE_ARCHITECTURE.md`) is comprehensive. This plan breaks down implementation into 7 phases with detailed tasks, timelines, and acceptance criteria.

---

## Current Status

### Phase 1: Core Infrastructure ✅ COMPLETE
- [x] NotificationEntity with full tracking
- [x] NotificationPreferenceEntity with user control
- [x] Database migration scripts (notifications, notification_preferences tables)
- [x] Repository interfaces (NotificationRepository, NotificationPreferenceRepository)
- [x] Basic channel implementations (EmailNotificationChannel exists)

**Status**: Foundation is solid. Entities and repositories are implemented and tested.

---

## Phase 2: Template System (Week 1-2)

### Objectives
Implement a flexible, maintainable template system for all notification channels.

### Tasks

#### 2.1 Template Engine Setup
```java
// Create TemplateRenderer interface
public interface TemplateRenderer {
    String render(String templateId, Map<String, Object> variables);
    boolean templateExists(String templateId);
}

// Implement with Thymeleaf
@Service
public class ThymeleafTemplateRenderer implements TemplateRenderer {
    private final SpringTemplateEngine templateEngine;
    // Template caching, variable substitution, error handling
}
```

#### 2.2 HTML Email Templates
Create responsive email templates in `src/main/resources/templates/notifications/email/`:

1. **critical-alert.html** - Urgent clinical alerts
   - Red header, large icon
   - Patient name, MRN
   - Alert details with action button
   - Mobile-responsive

2. **care-gap.html** - Care gap notifications
   - Orange header
   - Gap description, recommended actions
   - Due date, priority indicator

3. **health-score.html** - Health score updates
   - Color-coded based on score change
   - Trend visualization
   - Contributing factors

4. **appointment-reminder.html** - Appointment reminders
   - Blue header
   - Date, time, location
   - Add to calendar links

5. **medication-reminder.html** - Medication adherence
   - Purple header
   - Medication name, dosage, instructions

6. **lab-result.html** - Lab result notifications
   - Green header
   - Result summary, normal ranges
   - Link to full results

7. **digest.html** - Daily/weekly digests
   - Summary of all notifications
   - Grouped by type and priority

#### 2.3 SMS Templates
Create concise SMS templates in `src/main/resources/templates/notifications/sms/`:

```
critical-alert.txt:
"URGENT: {{patient.name}} (MRN:{{patient.mrn}}) - {{alert.title}}. Review immediately: {{action.url}}"

care-gap.txt:
"Care Gap: {{gap.title}} for {{patient.name}}. Action needed by {{gap.dueDate}}. Details: {{action.url}}"
```

#### 2.4 Template Versioning
```java
@Entity
public class NotificationTemplate {
    private String templateId;
    private String version;
    private NotificationChannel channel;
    private String content;
    private LocalDateTime createdAt;
    private boolean active;
}
```

### Acceptance Criteria
- [ ] All 7 email templates created and tested
- [ ] SMS templates created for all types
- [ ] Template rendering with variable substitution working
- [ ] Template versioning implemented
- [ ] Unit tests for template rendering (>90% coverage)
- [ ] Preview endpoint for template testing

### Timeline: 10 days
### Effort: 1 developer

---

## Phase 3: Provider Integration (Week 3-4)

### Objectives
Integrate multiple notification providers with auto-failover.

### Tasks

#### 3.1 SendGrid Integration (Primary Email Provider)
```java
@Service
@ConditionalOnProperty("notification.providers.sendgrid.enabled")
public class SendGridEmailProvider implements EmailProvider {

    @Override
    public SendResult send(String to, String subject, String htmlBody, String textBody) {
        SendGrid sg = new SendGrid(apiKey);
        Mail mail = new Mail(fromEmail, to, subject, new Content("text/html", htmlBody));
        // Error handling, delivery tracking
    }

    @Override
    public boolean isHealthy() {
        // Health check endpoint
    }
}
```

#### 3.2 AWS SES Integration (Failover Email)
```java
@Service
@ConditionalOnProperty("notification.providers.aws-ses.enabled")
public class AwsSesEmailProvider implements EmailProvider {
    private final AmazonSimpleEmailService sesClient;
    // Implementation with AWS SDK
}
```

#### 3.3 SMTP Fallback
```java
@Service
public class SmtpEmailProvider implements EmailProvider {
    private final JavaMailSender mailSender;
    // Basic SMTP implementation
}
```

#### 3.4 Twilio SMS Integration
```java
@Service
@ConditionalOnProperty("notification.providers.twilio.enabled")
public class TwilioSmsProvider implements SmsProvider {

    @Override
    public SendResult send(String phoneNumber, String message) {
        Message message = Message.creator(
            new PhoneNumber(phoneNumber),
            new PhoneNumber(fromNumber),
            messageContent
        ).create();
    }
}
```

#### 3.5 AWS SNS SMS Failover
```java
@Service
@ConditionalOnProperty("notification.providers.aws-sns.enabled")
public class AwsSnsProvider implements SmsProvider {
    private final AmazonSNS snsClient;
    // SMS implementation via SNS
}
```

#### 3.6 Provider Health Monitoring
```java
@Component
public class ProviderHealthMonitor {
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    public boolean isProviderHealthy(String providerName) {
        // Circuit breaker pattern
        // Health check results cached for 1 minute
    }
}
```

#### 3.7 Auto-Failover Logic
```java
@Service
public class EmailNotificationService {
    private final List<EmailProvider> providers; // Ordered by priority

    public void send(NotificationEntity notification) {
        for (EmailProvider provider : providers) {
            if (healthMonitor.isProviderHealthy(provider.getName())) {
                try {
                    SendResult result = provider.send(...);
                    if (result.isSuccess()) {
                        updateNotificationStatus(notification, SENT, provider.getName());
                        return;
                    }
                } catch (Exception e) {
                    log.warn("Provider {} failed, trying next", provider.getName(), e);
                }
            }
        }
        // All providers failed - queue for retry
        scheduleRetry(notification);
    }
}
```

### Acceptance Criteria
- [ ] SendGrid integration tested with real API
- [ ] AWS SES integration tested
- [ ] SMTP fallback working
- [ ] Twilio SMS integration tested
- [ ] Provider health monitoring implemented
- [ ] Auto-failover tested with all providers
- [ ] Circuit breaker pattern working
- [ ] Integration tests for all providers

### Timeline: 10 days
### Effort: 1-2 developers

---

## Phase 4: Delivery Pipeline (Week 5-6)

### Objectives
Build robust async delivery pipeline with retry logic and rate limiting.

### Tasks

#### 4.1 Redis-Backed Queue
```java
@Service
public class RedisNotificationQueue {
    private final RedisTemplate<String, NotificationEntity> redisTemplate;

    public void enqueue(NotificationEntity notification) {
        redisTemplate.opsForList().leftPush("notifications:pending", notification);
    }

    public NotificationEntity dequeue() {
        return redisTemplate.opsForList().rightPop("notifications:pending", 5, TimeUnit.SECONDS);
    }
}
```

#### 4.2 Async Worker Pool
```java
@Configuration
public class NotificationWorkerConfig {

    @Bean
    public Executor notificationWorkerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("notification-worker-");
        executor.initialize();
        return executor;
    }
}

@Service
public class NotificationWorker {

    @Async("notificationWorkerExecutor")
    public void processNotification(NotificationEntity notification) {
        // Process based on channel
        switch (notification.getChannel()) {
            case EMAIL -> emailService.send(notification);
            case SMS -> smsService.send(notification);
            case PUSH -> pushService.send(notification);
            case IN_APP -> inAppService.send(notification);
        }
    }
}
```

#### 4.3 Retry Logic with Exponential Backoff
```java
@Service
public class NotificationRetryService {

    public void scheduleRetry(NotificationEntity notification) {
        int retryCount = notification.getRetryCount();
        if (retryCount >= notification.getMaxRetries()) {
            moveToDeadLetterQueue(notification);
            return;
        }

        // Exponential backoff: 5min, 15min, 1hr, 4hr
        long delayMillis = calculateDelay(retryCount);
        notification.setNextRetryAt(LocalDateTime.now().plusSeconds(delayMillis / 1000));
        notification.setRetryCount(retryCount + 1);
        notificationRepository.save(notification);

        // Schedule for retry
        scheduler.schedule(() -> retryNotification(notification), delayMillis);
    }

    private long calculateDelay(int retryCount) {
        return (long) (Math.pow(3, retryCount) * 5 * 60 * 1000); // 5min * 3^retry
    }
}
```

#### 4.4 Rate Limiting
```java
@Service
public class NotificationRateLimiter {
    private final RedisTemplate<String, String> redisTemplate;

    public boolean allowNotification(String userId, NotificationChannel channel) {
        String key = String.format("rate:limit:%s:%s:hour", userId, channel);
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
        }

        int hourlyLimit = getHourlyLimit(channel);
        return count <= hourlyLimit;
    }

    private int getHourlyLimit(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> 10;
            case SMS -> 5;
            case PUSH -> 20;
            case IN_APP -> Integer.MAX_VALUE;
            default -> 10;
        };
    }
}
```

#### 4.5 Dead Letter Queue
```java
@Service
public class DeadLetterQueueService {

    public void moveToDeadLetterQueue(NotificationEntity notification) {
        notification.setStatus(NotificationStatus.FAILED);
        notification.setFailedAt(LocalDateTime.now());
        notificationRepository.save(notification);

        // Alert operations team
        alertService.sendAlert("Notification failed after max retries: " + notification.getId());
    }

    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void reviewDeadLetterQueue() {
        // Admin dashboard for manual review/retry
    }
}
```

### Acceptance Criteria
- [ ] Redis queue implemented and tested
- [ ] Worker pool processing notifications
- [ ] Retry logic with exponential backoff working
- [ ] Rate limiting enforced per user/channel
- [ ] Dead letter queue implemented
- [ ] Load testing: 1000 notifications/second
- [ ] Monitoring dashboard for queue metrics

### Timeline: 10 days
### Effort: 2 developers

---

## Phase 5: User Preferences (Week 7)

### Objectives
Implement comprehensive user preference management.

### Tasks

#### 5.1 Preference Management API
```java
@RestController
@RequestMapping("/api/notifications/preferences")
public class NotificationPreferenceController {

    @GetMapping
    public NotificationPreferenceDTO getPreferences(@AuthenticationPrincipal User user) {
        return preferenceService.getPreferences(user.getTenantId(), user.getId());
    }

    @PutMapping
    public NotificationPreferenceDTO updatePreferences(
        @RequestBody UpdatePreferenceRequest request,
        @AuthenticationPrincipal User user
    ) {
        return preferenceService.updatePreferences(user.getTenantId(), user.getId(), request);
    }
}
```

#### 5.2 Quiet Hours Implementation
```java
@Service
public class QuietHoursService {

    public boolean isWithinQuietHours(NotificationPreferenceEntity prefs) {
        if (!prefs.getQuietHoursEnabled()) {
            return false;
        }

        LocalTime now = LocalTime.now();
        LocalTime start = prefs.getQuietHoursStart();
        LocalTime end = prefs.getQuietHoursEnd();

        // Handle midnight crossing
        if (start.isAfter(end)) {
            return now.isAfter(start) || now.isBefore(end);
        }

        return now.isAfter(start) && now.isBefore(end);
    }

    public boolean shouldSend(NotificationEntity notification, NotificationPreferenceEntity prefs) {
        if (isWithinQuietHours(prefs)) {
            // Critical alerts override quiet hours
            if (notification.getSeverity() == NotificationSeverity.CRITICAL &&
                prefs.getQuietHoursOverrideCritical()) {
                return true;
            }
            return false;
        }
        return true;
    }
}
```

#### 5.3 Digest Mode Aggregation
```java
@Service
public class DigestAggregationService {

    @Scheduled(cron = "0 0 8 * * *") // Daily at 8am
    public void sendDailyDigests() {
        List<NotificationPreferenceEntity> digestUsers =
            preferenceRepository.findByDigestModeEnabledAndDigestFrequency(true, DigestFrequency.DAILY);

        for (NotificationPreferenceEntity pref : digestUsers) {
            List<NotificationEntity> pending =
                notificationRepository.findPendingDigestNotifications(pref.getUserId());

            if (!pending.isEmpty()) {
                sendDigest(pref, pending);
            }
        }
    }

    private void sendDigest(NotificationPreferenceEntity pref, List<NotificationEntity> notifications) {
        Map<String, Object> variables = Map.of(
            "notifications", notifications,
            "count", notifications.size(),
            "date", LocalDate.now()
        );

        String html = templateRenderer.render("digest", variables);
        emailService.send(pref.getEmailAddress(), "Daily Notification Digest", html);
    }
}
```

#### 5.4 Consent Management
```java
@Service
public class ConsentManagementService {

    public void giveConsent(String tenantId, String userId) {
        NotificationPreferenceEntity pref = getOrCreatePreference(tenantId, userId);
        pref.setConsentGiven(true);
        pref.setConsentDate(LocalDateTime.now());
        preferenceRepository.save(pref);

        // Audit log
        auditService.log("NOTIFICATION_CONSENT_GIVEN", userId);
    }

    public void revokeConsent(String tenantId, String userId) {
        NotificationPreferenceEntity pref = getOrCreatePreference(tenantId, userId);
        pref.setConsentGiven(false);
        pref.setEmailEnabled(false);
        pref.setSmsEnabled(false);
        pref.setPushEnabled(false);
        preferenceRepository.save(pref);

        // Audit log
        auditService.log("NOTIFICATION_CONSENT_REVOKED", userId);
    }
}
```

#### 5.5 Unsubscribe Handling
```java
@RestController
@RequestMapping("/api/notifications/unsubscribe")
public class UnsubscribeController {

    @GetMapping("/{token}")
    public String unsubscribe(@PathVariable String token) {
        // Decrypt token to get userId + tenantId
        UnsubscribeToken decodedToken = tokenService.decrypt(token);

        consentService.revokeConsent(decodedToken.getTenantId(), decodedToken.getUserId());

        return "You have been successfully unsubscribed from notifications.";
    }

    // Generate tamper-proof unsubscribe token
    private String generateUnsubscribeToken(String tenantId, String userId) {
        return tokenService.encrypt(new UnsubscribeToken(tenantId, userId));
    }
}
```

### Acceptance Criteria
- [ ] Preference management API implemented
- [ ] Quiet hours working with critical override
- [ ] Digest mode aggregating notifications
- [ ] Consent management HIPAA-compliant
- [ ] Unsubscribe links in all emails
- [ ] Frontend UI for preference management
- [ ] E2E tests for all preference scenarios

### Timeline: 5 days
### Effort: 1 developer

---

## Phase 6: Monitoring & Analytics (Week 8)

### Objectives
Implement comprehensive monitoring, metrics, and alerting.

### Tasks

#### 6.1 Metrics Collection
```java
@Service
public class NotificationMetricsService {
    private final MeterRegistry meterRegistry;

    public void recordNotificationSent(NotificationChannel channel, NotificationType type) {
        meterRegistry.counter("notifications.sent",
            "channel", channel.name(),
            "type", type.name()
        ).increment();
    }

    public void recordNotificationFailed(NotificationChannel channel, String reason) {
        meterRegistry.counter("notifications.failed",
            "channel", channel.name(),
            "reason", reason
        ).increment();
    }

    public void recordDeliveryTime(NotificationChannel channel, long milliseconds) {
        meterRegistry.timer("notifications.delivery.time",
            "channel", channel.name()
        ).record(milliseconds, TimeUnit.MILLISECONDS);
    }
}
```

#### 6.2 Analytics Repository
```java
@Repository
public interface NotificationAnalyticsRepository {

    @Query("SELECT n.channel, COUNT(n) FROM NotificationEntity n " +
           "WHERE n.createdAt BETWEEN :start AND :end " +
           "GROUP BY n.channel")
    List<Object[]> countByChannelBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT n.status, COUNT(n) FROM NotificationEntity n " +
           "WHERE n.createdAt BETWEEN :start AND :end " +
           "GROUP BY n.status")
    List<Object[]> countByStatusBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT AVG(EXTRACT(EPOCH FROM (n.deliveredAt - n.createdAt))) " +
           "FROM NotificationEntity n " +
           "WHERE n.deliveredAt IS NOT NULL " +
           "AND n.channel = :channel")
    Double averageDeliveryTime(NotificationChannel channel);
}
```

#### 6.3 Alerting Service
```java
@Service
public class NotificationAlertService {

    @Scheduled(fixedDelay = 60000) // Every minute
    public void checkFailureRate() {
        long totalSent = analyticsRepo.countSentLastHour();
        long totalFailed = analyticsRepo.countFailedLastHour();

        double failureRate = (double) totalFailed / totalSent;

        if (failureRate > 0.05) { // 5% threshold
            sendAlert("High notification failure rate: " + (failureRate * 100) + "%");
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void checkQueueBacklog() {
        long queueSize = redisQueue.size();

        if (queueSize > 1000) {
            sendAlert("Notification queue backup: " + queueSize + " pending");
        }
    }

    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void checkProviderHealth() {
        for (EmailProvider provider : emailProviders) {
            if (!provider.isHealthy()) {
                sendAlert("Provider unhealthy: " + provider.getName());
            }
        }
    }
}
```

#### 6.4 Analytics Dashboard API
```java
@RestController
@RequestMapping("/api/notifications/analytics")
public class NotificationAnalyticsController {

    @GetMapping("/overview")
    public AnalyticsOverview getOverview(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return AnalyticsOverview.builder()
            .totalSent(analyticsRepo.countSentBetween(start, end))
            .totalFailed(analyticsRepo.countFailedBetween(start, end))
            .byChannel(analyticsRepo.countByChannelBetween(start, end))
            .byType(analyticsRepo.countByTypeBetween(start, end))
            .deliveryRate(analyticsRepo.calculateDeliveryRate(start, end))
            .averageDeliveryTime(analyticsRepo.averageDeliveryTimeBetween(start, end))
            .build();
    }

    @GetMapping("/providers")
    public List<ProviderMetrics> getProviderMetrics() {
        return emailProviders.stream()
            .map(provider -> ProviderMetrics.builder()
                .name(provider.getName())
                .healthy(provider.isHealthy())
                .totalSent(analyticsRepo.countByProvider(provider.getName()))
                .failureRate(analyticsRepo.failureRateByProvider(provider.getName()))
                .averageLatency(analyticsRepo.averageLatencyByProvider(provider.getName()))
                .build())
            .collect(Collectors.toList());
    }
}
```

### Acceptance Criteria
- [ ] Prometheus metrics exposed
- [ ] Grafana dashboard created
- [ ] Alert thresholds configured
- [ ] Analytics API endpoints working
- [ ] Real-time metrics dashboard
- [ ] Historical trend analysis
- [ ] Cost monitoring per channel

### Timeline: 5 days
### Effort: 1 developer

---

## Phase 7: Security & Compliance (Week 9)

### Objectives
Ensure HIPAA compliance and production-grade security.

### Tasks

#### 7.1 PHI Encryption at Rest
```java
@Configuration
public class EncryptionConfig {

    @Bean
    public AttributeConverter<String, String> phiEncryptor() {
        return new AbstractAttributeConverter<String, String>() {
            @Override
            public String convertToDatabaseColumn(String attribute) {
                return encryptionService.encrypt(attribute);
            }

            @Override
            public String convertToEntityAttribute(String dbData) {
                return encryptionService.decrypt(dbData);
            }
        };
    }
}

@Entity
public class NotificationEntity {
    @Convert(converter = PhiEncryptor.class)
    private String recipient;

    @Convert(converter = PhiEncryptor.class)
    private String message;
}
```

#### 7.2 TLS for Provider Calls
```java
@Configuration
public class SslConfig {

    @Bean
    public RestTemplate sslRestTemplate() throws Exception {
        SSLContext sslContext = SSLContextBuilder.create()
            .loadTrustMaterial(null, (certificate, authType) -> true)
            .build();

        SSLConnectionSocketFactory socketFactory =
            new SSLConnectionSocketFactory(sslContext,
                new String[]{"TLSv1.3"}, null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier());

        CloseableHttpClient httpClient = HttpClients.custom()
            .setSSLSocketFactory(socketFactory)
            .build();

        HttpComponentsClientHttpRequestFactory factory =
            new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(factory);
    }
}
```

#### 7.3 Access Control Enforcement
```java
@Service
public class NotificationAuthorizationService {

    public boolean canViewNotification(User user, NotificationEntity notification) {
        // Tenant isolation
        if (!user.getTenantId().equals(notification.getTenantId())) {
            return false;
        }

        // Patient notifications only viewable by patient or their care team
        if (notification.getPatientId() != null) {
            return user.getId().equals(notification.getUserId()) ||
                   careTeamService.isOnCareTeam(user.getId(), notification.getPatientId());
        }

        return user.getId().equals(notification.getUserId());
    }
}
```

#### 7.4 HIPAA Audit Trail
```java
@Aspect
@Component
public class NotificationAuditAspect {

    @Around("@annotation(Audited)")
    public Object auditNotificationAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        User user = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String action = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        try {
            Object result = joinPoint.proceed();

            auditLogService.log(AuditLog.builder()
                .tenantId(user.getTenantId())
                .userId(user.getId())
                .action("NOTIFICATION_" + action.toUpperCase())
                .resourceId(extractNotificationId(args))
                .timestamp(LocalDateTime.now())
                .ipAddress(requestContext.getClientIp())
                .userAgent(requestContext.getUserAgent())
                .status("SUCCESS")
                .build());

            return result;
        } catch (Exception e) {
            auditLogService.log(/* ... failure audit ... */);
            throw e;
        }
    }
}
```

#### 7.5 Penetration Testing Checklist
- [ ] SQL injection testing on all endpoints
- [ ] XSS testing on template rendering
- [ ] CSRF protection verified
- [ ] Rate limiting bypass attempts
- [ ] Unsubscribe token tampering
- [ ] PHI leakage in logs/errors
- [ ] HIPAA compliance verification

### Acceptance Criteria
- [ ] All PHI encrypted at rest (AES-256)
- [ ] TLS 1.3 enforced for all provider calls
- [ ] Access control tests passing
- [ ] HIPAA audit trail complete
- [ ] Penetration testing passed
- [ ] Security review approved
- [ ] HIPAA compliance documentation

### Timeline: 5 days
### Effort: 1 developer + 1 security specialist

---

## Database Migration Scripts

### Phase 1: Already Complete ✅
```sql
-- notifications table
-- notification_preferences table
```

### Phase 2: Template System
```sql
CREATE TABLE notification_templates (
    id UUID PRIMARY KEY,
    template_id VARCHAR(255) NOT NULL,
    version VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    variables JSONB,
    created_at TIMESTAMP NOT NULL,
    active BOOLEAN DEFAULT true,
    UNIQUE(template_id, version)
);

CREATE INDEX idx_templates_active ON notification_templates(template_id, active);
```

### Phase 6: Analytics
```sql
-- Add indexes for analytics queries
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_notifications_channel_status ON notifications(channel, status);
CREATE INDEX idx_notifications_delivered_at ON notifications(delivered_at) WHERE delivered_at IS NOT NULL;
```

---

## Configuration Updates

### application.yml additions
```yaml
notification:
  # Provider Configuration
  providers:
    sendgrid:
      enabled: ${SENDGRID_ENABLED:true}
      api-key: ${SENDGRID_API_KEY}
      from-email: ${SENDGRID_FROM_EMAIL:alerts@healthdata.com}
      from-name: ${SENDGRID_FROM_NAME:HealthData Clinical Alerts}
      webhook-url: ${SENDGRID_WEBHOOK_URL:http://localhost:8087/api/notifications/webhooks/sendgrid}

    aws-ses:
      enabled: ${AWS_SES_ENABLED:false}
      region: ${AWS_REGION:us-east-1}
      access-key: ${AWS_ACCESS_KEY}
      secret-key: ${AWS_SECRET_KEY}
      from-email: ${AWS_SES_FROM_EMAIL:alerts@healthdata.com}

    smtp:
      enabled: ${SMTP_ENABLED:true}
      host: ${SMTP_HOST:localhost}
      port: ${SMTP_PORT:1025}
      username: ${SMTP_USERNAME:}
      password: ${SMTP_PASSWORD:}
      from-email: ${SMTP_FROM_EMAIL:alerts@localhost}

    twilio:
      enabled: ${TWILIO_ENABLED:false}
      account-sid: ${TWILIO_ACCOUNT_SID}
      auth-token: ${TWILIO_AUTH_TOKEN}
      from-number: ${TWILIO_FROM_NUMBER}

    aws-sns:
      enabled: ${AWS_SNS_ENABLED:false}
      region: ${AWS_REGION:us-east-1}
      access-key: ${AWS_ACCESS_KEY}
      secret-key: ${AWS_SECRET_KEY}

  # Rate Limiting
  rate-limits:
    email-per-hour: 10
    email-per-day: 50
    sms-per-hour: 5
    sms-per-day: 20
    push-per-hour: 20
    push-per-day: 100

  # Retry Configuration
  retry:
    max-attempts: 5
    initial-delay-ms: 300000  # 5 minutes
    max-delay-ms: 14400000    # 4 hours
    multiplier: 3.0

  # Queue Configuration
  queue:
    redis:
      enabled: true
      key-prefix: "notifications:"
    kafka:
      enabled: false
      topic: "notifications"

  # Templates
  templates:
    base-path: classpath:/templates/notifications
    cache-enabled: true
    cache-ttl-seconds: 3600

  # Worker Pool
  workers:
    core-pool-size: 10
    max-pool-size: 50
    queue-capacity: 1000

  # Security
  encryption:
    enabled: true
    algorithm: AES/GCM/NoPadding
    key: ${NOTIFICATION_ENCRYPTION_KEY}

  # Monitoring
  metrics:
    enabled: true
    export-interval-seconds: 60
```

---

## Testing Strategy

### Unit Tests (Target: >90% coverage)
- Template rendering
- Provider implementations
- Retry logic
- Rate limiting
- Preference evaluation

### Integration Tests
- End-to-end notification flow
- Provider failover
- Queue processing
- Database interactions

### Load Tests
- 1000 notifications/second sustained
- Queue backlog recovery
- Database performance
- Memory usage

### E2E Tests
- Full user journey (consent → notification → delivery)
- Unsubscribe flow
- Preference changes
- Digest aggregation

---

## Deployment Strategy

### Phase-by-Phase Rollout
1. Deploy Phase 2 (Templates) - Low risk
2. Deploy Phase 3 (Providers) - Start with SMTP, add SendGrid
3. Deploy Phase 4 (Pipeline) - Monitor queue metrics
4. Deploy Phase 5 (Preferences) - Enable gradually
5. Deploy Phase 6 (Monitoring) - Essential for production
6. Deploy Phase 7 (Security) - Before PHI notifications

### Rollback Plan
- Each phase has database migration rollback scripts
- Feature flags for provider enablement
- Circuit breakers prevent cascading failures

---

## Resource Requirements

### Development Team
- 2 Backend Developers (Java/Spring Boot)
- 1 Frontend Developer (Angular - for preference UI)
- 1 DevOps Engineer (deployment, monitoring)
- 1 Security Specialist (Phase 7)
- 1 QA Engineer (testing strategy)

### Infrastructure
- **Development**: Existing resources sufficient
- **Staging**:
  - Redis instance (2GB)
  - SendGrid sandbox account
  - MailHog for SMTP testing
- **Production**:
  - Redis cluster (4GB, HA)
  - SendGrid production account ($50-200/month)
  - AWS SES backup account ($0-50/month)
  - Twilio account if SMS enabled ($0.0075/SMS)

### Budget Estimate
- Development: 9 weeks × 2 devs = 18 dev-weeks
- SendGrid: $100-500/month (volume-dependent)
- AWS SES: $0.10 per 1000 emails
- Twilio SMS: $0.0075 per message
- Infrastructure: $100-300/month

---

## Success Metrics

### Technical Metrics
- Notification delivery rate: >99%
- Average delivery time: <30 seconds
- Queue processing latency: <5 seconds
- Provider failover time: <1 second
- System throughput: >1000 notifications/second

### Business Metrics
- User engagement: >40% open rate (email)
- Unsubscribe rate: <2%
- Critical alert delivery: 100% within 1 minute
- Cost per notification: <$0.001
- User satisfaction: >4.5/5 on preference management

---

## Risk Assessment

### High Risks
1. **Provider Rate Limits**: Mitigated by multiple providers and queuing
2. **PHI Breach**: Mitigated by encryption and audit logging
3. **Alert Fatigue**: Mitigated by preference management and rate limiting
4. **Cost Overrun**: Mitigated by budget caps and monitoring

### Medium Risks
1. **Template Maintenance**: Need versioning and preview tools
2. **Queue Overflow**: Need dead letter queue and alerting
3. **Provider Downtime**: Auto-failover handles this

---

## Timeline Summary

| Phase | Duration | Dependencies | Critical Path |
|-------|----------|--------------|---------------|
| Phase 1 | Complete | - | ✅ |
| Phase 2 | 10 days | Phase 1 | Yes |
| Phase 3 | 10 days | Phase 2 | Yes |
| Phase 4 | 10 days | Phase 3 | Yes |
| Phase 5 | 5 days | Phase 4 | No |
| Phase 6 | 5 days | Phase 4 | No |
| Phase 7 | 5 days | All phases | Yes |

**Total Duration**: 9 weeks (45 days)
**Critical Path**: Phases 2 → 3 → 4 → 7

---

## Conclusion

This implementation plan provides a complete roadmap for a production-ready, HIPAA-compliant notification engine. The phased approach allows for incremental delivery and validation. Each phase builds upon the previous, with clear acceptance criteria and testing requirements.

**Next Actions**:
1. Stakeholder review and approval
2. Resource allocation
3. Sprint planning for Phase 2
4. Development kickoff

**Status**: Ready for Implementation
**Last Updated**: November 27, 2025
**Version**: 1.0.0
