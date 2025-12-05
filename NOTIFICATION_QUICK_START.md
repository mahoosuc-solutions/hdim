# Notification Engine Quick Start Guide

## For Developers - Get Started in 10 Minutes

### Prerequisites
```bash
# Start required services
docker run -d --name redis -p 6379:6379 redis:7-alpine
docker run -d --name mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

### 1. Run Database Migrations
```bash
cd backend/modules/services/quality-measure-service
./gradlew update  # Runs Liquibase migrations
```

### 2. Configure Application
Set environment variables in `application.yml` or via env vars:
```yaml
notification:
  providers:
    smtp:
      enabled: true
      host: localhost
      port: 1025
```

### 3. Send Your First Notification

#### Via Service (Java)
```java
@Autowired
private NotificationService notificationService;

public void sendAlert() {
    ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
        .id("alert-123")
        .severity("CRITICAL")
        .title("Blood Pressure Alert")
        .message("Patient BP reading: 180/120")
        .patientId("patient-456")
        .build();
    
    notificationService.sendNotification("tenant-1", alert);
}
```

#### Via REST API
```bash
curl -X POST http://localhost:8087/quality-measure/api/notifications/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: tenant-1" \
  -d '{
    "userId": "user123",
    "channel": "EMAIL",
    "type": "CLINICAL_ALERT",
    "severity": "HIGH",
    "templateId": "critical-alert",
    "recipient": "doctor@example.com",
    "variables": {
      "patient": {
        "name": "John Doe",
        "mrn": "MRN123456"
      },
      "alert": {
        "title": "Blood Pressure Critical",
        "message": "BP reading: 180/120"
      }
    }
  }'
```

### 4. View Email in MailHog
Open browser to `http://localhost:8025` to see sent emails.

### 5. Check Notification Status
```bash
curl http://localhost:8087/quality-measure/api/notifications/{id}/status \
  -H "Authorization: Bearer $TOKEN"
```

### 6. Manage User Preferences
```bash
# Get preferences
curl http://localhost:8087/quality-measure/api/notifications/preferences \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: tenant-1"

# Update preferences
curl -X PUT http://localhost:8087/quality-measure/api/notifications/preferences \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: tenant-1" \
  -d '{
    "emailEnabled": true,
    "smsEnabled": false,
    "quietHoursEnabled": true,
    "quietHoursStart": "22:00",
    "quietHoursEnd": "08:00",
    "severityThreshold": "MEDIUM"
  }'
```

---

## Common Use Cases

### Use Case 1: Send Critical Clinical Alert
```java
// Sends via WebSocket + Email + SMS
ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
    .severity("CRITICAL")
    .title("Mental Health Crisis Detected")
    .message("Patient PHQ-9 score indicates severe depression with suicidal ideation")
    .patientId("patient-789")
    .build();

notificationService.sendNotification(tenantId, alert);
```

### Use Case 2: Send Care Gap Notification
```java
// Sends via WebSocket + Email only
ClinicalAlertDTO careGap = ClinicalAlertDTO.builder()
    .severity("MEDIUM")
    .alertType("CARE_GAP")
    .title("Annual Wellness Visit Overdue")
    .message("Patient has not had annual wellness visit in 13 months")
    .patientId("patient-456")
    .build();

notificationService.sendNotification(tenantId, careGap);
```

### Use Case 3: Batch Notifications
```java
List<ClinicalAlertDTO> alerts = Arrays.asList(alert1, alert2, alert3);
notificationService.sendBatchNotification(tenantId, alerts);
```

---

## Monitoring & Debugging

### Check Queue Status
```bash
curl http://localhost:8087/quality-measure/api/notifications/monitoring/queues \
  -H "Authorization: Bearer $TOKEN"
```

Response:
```json
{
  "emailQueueSize": 5,
  "smsQueueSize": 0,
  "pushQueueSize": 2,
  "deadLetterQueueSize": 1,
  "timestamp": "2025-11-26T14:30:00"
}
```

### Check Provider Health
```bash
curl http://localhost:8087/quality-measure/api/notifications/monitoring/providers/health \
  -H "Authorization: Bearer $TOKEN"
```

Response:
```json
[
  {
    "providerName": "SendGrid",
    "channel": "EMAIL",
    "healthy": true,
    "priority": 1
  },
  {
    "providerName": "Twilio",
    "channel": "SMS",
    "healthy": true,
    "priority": 1
  }
]
```

### Get Delivery Metrics
```bash
curl http://localhost:8087/quality-measure/api/notifications/monitoring/metrics?hours=24 \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: tenant-1"
```

### View Application Logs
```bash
tail -f logs/quality-measure-service.log | grep NOTIFICATION
```

---

## Troubleshooting

### Problem: Notifications not sending
**Check**: Provider health
```bash
curl http://localhost:8087/quality-measure/api/notifications/monitoring/providers/health
```

**Solution**: If unhealthy, verify credentials in application.yml

### Problem: High queue depth
**Check**: Queue status
```bash
curl http://localhost:8087/quality-measure/api/notifications/monitoring/queues
```

**Solution**: Increase worker pool size in `NotificationAsyncConfig.java`

### Problem: Rate limiting blocking notifications
**Check**: Application logs for "rate limited" messages

**Solution**: Adjust rate limits in application.yml or mark as CRITICAL severity

---

## Testing

### Unit Test Example
```java
@Test
public void testCriticalAlertSendsViaAllChannels() {
    ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
        .severity("CRITICAL")
        .title("Test Alert")
        .build();
    
    NotificationService.NotificationStatus status = 
        notificationService.sendNotificationWithStatus(tenantId, alert);
    
    assertTrue(status.isAllSuccessful());
    assertTrue(status.getChannelStatus().get("websocket"));
    assertTrue(status.getChannelStatus().get("email"));
    assertTrue(status.getChannelStatus().get("sms"));
}
```

### Integration Test Example
```java
@SpringBootTest
@Transactional
class NotificationIntegrationTest {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Test
    void testNotificationPersistence() {
        NotificationEntity notification = NotificationEntity.builder()
            .tenantId("test-tenant")
            .channel(NotificationEntity.NotificationChannel.EMAIL)
            .status(NotificationEntity.NotificationStatus.PENDING)
            .recipient("test@example.com")
            .build();
        
        NotificationEntity saved = notificationRepository.save(notification);
        
        assertNotNull(saved.getId());
        assertEquals("PENDING", saved.getStatus().name());
    }
}
```

---

## Performance Tips

1. **Use Async Processing**: All notifications are queued by default
2. **Batch Operations**: Use `sendBatchNotification()` for multiple alerts
3. **Template Caching**: Templates are cached automatically
4. **Database Indexing**: Ensure indexes are created (via Liquibase)
5. **Connection Pooling**: Configure HikariCP for optimal performance

---

## Security Best Practices

1. **JWT Authentication**: Always require valid JWT token
2. **Tenant Isolation**: Use X-Tenant-ID header for multi-tenancy
3. **PHI Minimization**: Only include necessary patient data
4. **TLS Encryption**: Use HTTPS in production
5. **Audit Logging**: All events are logged automatically

---

## Next Steps

1. Review full implementation plan: `NOTIFICATION_ENGINE_IMPLEMENTATION_PLAN.md`
2. Review architecture: `NOTIFICATION_ENGINE_ARCHITECTURE.md`
3. Set up production providers (SendGrid, Twilio)
4. Configure monitoring alerts
5. Run load tests
6. Deploy to production

---

**Quick Links**:
- Architecture: [NOTIFICATION_ENGINE_ARCHITECTURE.md](NOTIFICATION_ENGINE_ARCHITECTURE.md)
- Implementation Plan: [NOTIFICATION_ENGINE_IMPLEMENTATION_PLAN.md](NOTIFICATION_ENGINE_IMPLEMENTATION_PLAN.md)
- API Documentation: http://localhost:8087/quality-measure/swagger-ui.html

**Need Help?**
- Check logs: `logs/quality-measure-service.log`
- View MailHog: http://localhost:8025
- Monitor Redis: `redis-cli monitor`
- API Health: http://localhost:8087/quality-measure/actuator/health
