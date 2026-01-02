# Notification System End-to-End Testing Guide

**Date:** 2025-11-28
**Status:** Deployed to staging (healthdata-demo)
**Version:** 1.0

---

## Deployment Status

✅ **Backend Build:** Completed successfully (BUILD SUCCESSFUL in 5m 11s)
✅ **Quality-Measure-Service:** Rebuilt and restarted with notification triggers
✅ **Container Status:** healthdata-quality-measure Started
⚠️ **Service Health:** Requires authentication to verify (gcloud auth login)

---

## Pre-Test Checklist

Before running end-to-end tests, ensure:

1. **Service Health**
   ```bash
   curl http://localhost:8087/quality-measure/actuator/health
   ```
   Expected: `{"status":"UP"}`

2. **Database Connectivity**
   ```bash
   # Check PostgreSQL connection
   docker exec healthdata-postgres pg_isready
   ```

3. **Kafka Availability**
   ```bash
   # Check Kafka is running
   docker ps | grep healthdata-kafka
   ```

4. **Service Logs**
   ```bash
   # Tail logs for any startup errors
   docker logs healthdata-quality-measure --tail=50
   ```

---

## Test Scenarios

### Test 1: Care Gap Notification - HIGH Priority

**Objective:** Verify HIGH priority care gap triggers WebSocket + Email notification

**Steps:**
1. Create a HIGH priority care gap via API:
   ```bash
   curl -X POST http://localhost:8087/quality-measure/care-gaps \
     -H "Content-Type: application/json" \
     -H "X-Tenant-ID: test-tenant" \
     -d '{
       "patientId": "patient-test-001",
       "category": "SCREENING",
       "gapType": "colonoscopy-due",
       "title": "Colonoscopy Screening Overdue",
       "description": "Patient is overdue for colonoscopy screening",
       "priority": "HIGH",
       "dueDate": "2025-12-15T00:00:00Z",
       "matchingCodes": "45378,45380,45385"
     }'
   ```

2. **Expected Notification:**
   - **Channels:** WebSocket + Email
   - **Template:** care-gap
   - **Severity:** MEDIUM
   - **Title:** "Care Gap Identified: Colonoscopy Screening Overdue"
   - **Message:** Contains patient ID, gap type, due date, priority

3. **Verification:**
   - Check application logs for notification trigger:
     ```bash
     docker logs healthdata-quality-measure | grep "Care gap notification"
     ```
   - Verify NotificationService.sendNotification() was called
   - Check sendWebSocket = true, sendEmail = true, sendSms = false

**Expected Result:** ✅ Notification triggered with correct channels

---

### Test 2: Care Gap Notification - CRITICAL Priority with SMS

**Objective:** Verify CRITICAL priority care gap triggers all channels including SMS

**Steps:**
1. Create a CRITICAL priority care gap:
   ```bash
   curl -X POST http://localhost:8087/quality-measure/care-gaps \
     -H "Content-Type: application/json" \
     -H "X-Tenant-ID: test-tenant" \
     -d '{
       "patientId": "patient-test-002",
       "category": "MEDICATION",
       "gapType": "critical-medication-gap",
       "title": "Critical Medication Gap - Insulin Missing",
       "description": "Diabetic patient missing critical insulin prescription",
       "priority": "CRITICAL",
       "dueDate": "2025-11-29T00:00:00Z",
       "matchingCodes": "insulin"
     }'
   ```

2. **Expected Notification:**
   - **Channels:** WebSocket + Email + SMS
   - **Template:** care-gap
   - **Severity:** HIGH
   - **SMS Message:** < 500 characters, contains critical info

3. **Verification:**
   ```bash
   docker logs healthdata-quality-measure | grep "sendSms=true"
   ```

**Expected Result:** ✅ CRITICAL gap triggers SMS delivery

---

### Test 3: Care Gap Notification - LOW Priority Filtering

**Objective:** Verify LOW priority gaps not due soon are filtered (no notification)

**Steps:**
1. Create a LOW priority care gap due in 30 days:
   ```bash
   curl -X POST http://localhost:8087/quality-measure/care-gaps \
     -H "Content-Type: application/json" \
     -H "X-Tenant-ID: test-tenant" \
     -d '{
       "patientId": "patient-test-003",
       "category": "PREVENTIVE",
       "gapType": "routine-checkup",
       "title": "Routine Annual Checkup Due",
       "description": "Annual wellness visit due",
       "priority": "LOW",
       "dueDate": "2025-12-28T00:00:00Z"
     }'
   ```

2. **Expected Behavior:**
   - **Notification:** SKIPPED (due date > 7 days away)
   - **Log Message:** "Skipping notification for LOW priority gap not due soon"

3. **Verification:**
   ```bash
   docker logs healthdata-quality-measure | grep "Skipping notification"
   ```

**Expected Result:** ✅ No notification triggered (smart filtering works)

---

### Test 4: Mental Health Assessment - Severe PHQ-9

**Objective:** Verify severe depression assessment triggers SMS notification

**Steps:**
1. Submit PHQ-9 assessment with severe score (≥20):
   ```bash
   curl -X POST http://localhost:8087/quality-measure/mental-health/assessments \
     -H "Content-Type: application/json" \
     -H "X-Tenant-ID: test-tenant" \
     -d '{
       "patientId": "patient-test-004",
       "assessmentType": "PHQ-9",
       "assessedBy": "Dr. Test",
       "assessmentDate": "2025-11-28T10:00:00Z",
       "responses": {
         "q1": 3, "q2": 3, "q3": 3, "q4": 3, "q5": 3,
         "q6": 3, "q7": 3, "q8": 2, "q9": 2
       },
       "clinicalNotes": "Patient reports severe symptoms"
     }'
   ```
   *(Total score: 25 - Severe depression)*

2. **Expected Notification:**
   - **Channels:** WebSocket + Email + SMS
   - **Template:** care-gap (mental health uses care-gap template)
   - **Severity:** HIGH
   - **Title:** "⚠️ Positive PHQ-9 Screen: severe"
   - **Message:** "Score: 25/27 (severe). Positive screen (threshold: 10). Clinical follow-up required."

3. **Verification:**
   ```bash
   docker logs healthdata-quality-measure | grep "Mental health assessment notification sent"
   ```

**Expected Result:** ✅ Severe assessment triggers SMS for immediate follow-up

---

### Test 5: Mental Health Assessment - Negative Screen Filtering

**Objective:** Verify negative screens don't trigger notifications (prevent fatigue)

**Steps:**
1. Submit PHQ-9 assessment with minimal score (< 5):
   ```bash
   curl -X POST http://localhost:8087/quality-measure/mental-health/assessments \
     -H "Content-Type: application/json" \
     -H "X-Tenant-ID: test-tenant" \
     -d '{
       "patientId": "patient-test-005",
       "assessmentType": "PHQ-9",
       "assessedBy": "Dr. Test",
       "assessmentDate": "2025-11-28T11:00:00Z",
       "responses": {
         "q1": 0, "q2": 0, "q3": 1, "q4": 0, "q5": 1,
         "q6": 0, "q7": 0, "q8": 0, "q9": 0
       }
     }'
   ```
   *(Total score: 2 - Minimal depression)*

2. **Expected Behavior:**
   - **Notification:** SKIPPED
   - **Log Message:** "Skipping assessment notification for negative screen"

3. **Verification:**
   ```bash
   docker logs healthdata-quality-measure | grep "Skipping assessment notification"
   ```

**Expected Result:** ✅ No notification for negative screen (prevents notification fatigue)

---

### Test 6: Clinical Alert - Suicide Risk (CRITICAL)

**Objective:** Verify suicide risk alert triggers immediate SMS notification

**Steps:**
1. Trigger suicide risk alert via mental health assessment:
   ```bash
   curl -X POST http://localhost:8087/quality-measure/mental-health/assessments \
     -H "Content-Type: application/json" \
     -H "X-Tenant-ID: test-tenant" \
     -d '{
       "patientId": "patient-test-006",
       "assessmentType": "PHQ-9",
       "assessedBy": "Dr. Test",
       "assessmentDate": "2025-11-28T12:00:00Z",
       "responses": {
         "q1": 2, "q2": 2, "q3": 2, "q4": 2, "q5": 2,
         "q6": 2, "q7": 2, "q8": 2, "q9": 3
       },
       "clinicalNotes": "Patient endorsed suicidal ideation"
     }'
   ```
   *(Q9 = 3 triggers suicide risk alert)*

2. **Expected Notifications:**
   - **Assessment Notification:** Moderate depression (WebSocket + Email)
   - **Clinical Alert:** Suicide risk detected (WebSocket + Email + SMS)

3. **Alert Details:**
   - **Severity:** CRITICAL
   - **Title:** "URGENT: Suicide Risk Detected"
   - **Template:** critical-alert
   - **SMS:** Immediate delivery

4. **Verification:**
   ```bash
   docker logs healthdata-quality-measure | grep "Suicide risk alert"
   docker logs healthdata-quality-measure | grep "CRITICAL.*sendSms=true"
   ```

**Expected Result:** ✅ Immediate SMS notification for suicide risk

---

### Test 7: Alert Acknowledgment Notification

**Objective:** Verify CRITICAL alert acknowledgment triggers notification

**Steps:**
1. First, create a CRITICAL alert (reuse Test 6 to get alert ID)

2. Acknowledge the alert:
   ```bash
   curl -X POST "http://localhost:8087/quality-measure/clinical-alerts/{alertId}/acknowledge" \
     -H "Content-Type: application/json" \
     -H "X-Tenant-ID: test-tenant" \
     -d '{
       "acknowledgedBy": "Dr. Responder",
       "notes": "Contacted patient, emergency services notified"
     }'
   ```

3. **Expected Notification:**
   - **Channels:** WebSocket + Email (no SMS for acknowledgments)
   - **Title:** "Clinical Alert Acknowledged: Suicide Risk Detected"
   - **Message:** Contains acknowledger name and notes

4. **Verification:**
   ```bash
   docker logs healthdata-quality-measure | grep "Alert acknowledged notification"
   ```

**Expected Result:** ✅ Acknowledgment notification sent to care team

---

### Test 8: Template Rendering - Critical Alert Email

**Objective:** Verify email template renders correctly with all variables

**Steps:**
1. Trigger any CRITICAL alert (Tests 2 or 6)

2. **Check Template Variables:**
   - Patient ID, name
   - Alert severity, title, message
   - Timestamp, facility name
   - Action URL
   - HIPAA disclaimer
   - Mobile-responsive viewport meta tag

3. **Verification:**
   ```bash
   # Check logs for template rendering
   docker logs healthdata-quality-measure | grep "Template rendered"

   # Verify template variables populated
   docker logs healthdata-quality-measure | grep "patientId=patient-test"
   ```

**Expected Result:** ✅ Template renders with all required variables

---

### Test 9: Error Handling - Notification Failure

**Objective:** Verify business logic continues even if notification fails

**Steps:**
1. Temporarily break notification service (e.g., stop Kafka):
   ```bash
   docker stop healthdata-kafka
   ```

2. Create a care gap (reuse Test 1)

3. **Expected Behavior:**
   - **Care Gap Creation:** SUCCESS (business logic unaffected)
   - **Notification:** FAILED (logged but not thrown)
   - **Log Message:** "Failed to trigger care gap notification"

4. **Verification:**
   ```bash
   # Verify care gap was created despite notification failure
   curl "http://localhost:8087/quality-measure/care-gaps?patientId=patient-test-001" \
     -H "X-Tenant-ID: test-tenant"

   # Check error was logged
   docker logs healthdata-quality-measure | grep "Failed to trigger.*notification"
   ```

5. **Restore Kafka:**
   ```bash
   docker start healthdata-kafka
   ```

**Expected Result:** ✅ Business logic resilient to notification failures

---

### Test 10: Care Gap Auto-Closure Notification

**Objective:** Verify care gap addressed triggers "gap closed" notification

**Steps:**
1. Create a care gap (reuse Test 1 to get gap ID)

2. Address/close the care gap:
   ```bash
   curl -X POST "http://localhost:8087/quality-measure/care-gaps/{gapId}/address" \
     -H "Content-Type: application/json" \
     -H "X-Tenant-ID: test-tenant" \
     -d '{
       "addressedBy": "Dr. Provider",
       "evidence": "Colonoscopy completed 2025-11-28",
       "notes": "Procedure successful, no abnormalities found"
     }'
   ```

3. **Expected Notification:**
   - **Channels:** WebSocket + Email
   - **Template:** care-gap
   - **Title:** "Care Gap Addressed: Colonoscopy Screening Overdue"
   - **Message:** Contains addresser, evidence, notes

4. **Verification:**
   ```bash
   docker logs healthdata-quality-measure | grep "Care gap addressed notification"
   ```

**Expected Result:** ✅ Gap closure notification sent to care team

---

## Performance Testing

### Load Test: Notification Throughput

**Objective:** Verify notification system handles high volume

**Test Script:**
```bash
# Create 100 care gaps in rapid succession
for i in {1..100}; do
  curl -X POST http://localhost:8087/quality-measure/care-gaps \
    -H "Content-Type: application/json" \
    -H "X-Tenant-ID: test-tenant" \
    -d "{
      \"patientId\": \"patient-load-test-$i\",
      \"category\": \"SCREENING\",
      \"gapType\": \"routine-screening\",
      \"title\": \"Routine Screening $i\",
      \"priority\": \"MEDIUM\",
      \"dueDate\": \"2025-12-15T00:00:00Z\"
    }" &
done
wait
```

**Expected Results:**
- All 100 care gaps created successfully
- Notifications triggered asynchronously (non-blocking)
- No performance degradation of care gap creation API
- Average response time < 200ms per request

**Verification:**
```bash
# Check all care gaps were created
curl "http://localhost:8087/quality-measure/care-gaps?page=0&size=100" \
  -H "X-Tenant-ID: test-tenant" | jq '.totalElements'

# Check notification trigger log counts
docker logs healthdata-quality-measure | grep "Care gap notification" | wc -l
```

---

## Integration Test Checklist

- [ ] **Test 1:** HIGH priority care gap → WebSocket + Email
- [ ] **Test 2:** CRITICAL priority care gap → WebSocket + Email + SMS
- [ ] **Test 3:** LOW priority gap filtering → No notification
- [ ] **Test 4:** Severe PHQ-9 → WebSocket + Email + SMS
- [ ] **Test 5:** Negative PHQ-9 → No notification (filtering)
- [ ] **Test 6:** Suicide risk alert → CRITICAL SMS delivery
- [ ] **Test 7:** Alert acknowledgment → WebSocket + Email
- [ ] **Test 8:** Template rendering → All variables populated
- [ ] **Test 9:** Notification failure → Business logic continues
- [ ] **Test 10:** Care gap closure → Addressed notification
- [ ] **Load Test:** 100 concurrent notifications → No degradation

---

## Known Issues & Workarounds

### Issue 1: Template Variables Not Populated
**Symptom:** Email template missing patient name, facility name
**Workaround:** These variables require FHIR integration - currently use placeholders
**Resolution:** Implement FHIR patient lookup in notification triggers

### Issue 2: SMS Provider Not Configured
**Symptom:** SMS notifications fail silently
**Workaround:** Configure Twilio/SNS credentials in application.yml
**Resolution:** Add SMS provider configuration to staging environment

### Issue 3: Email Provider Not Configured
**Symptom:** Email notifications not delivered
**Workaround:** Configure SendGrid/SES credentials
**Resolution:** Add email provider configuration to staging environment

---

## Monitoring & Observability

### Key Metrics to Monitor

1. **Notification Success Rate**
   ```sql
   -- Query notification success/failure from audit logs
   SELECT
     COUNT(*) as total_notifications,
     SUM(CASE WHEN success = true THEN 1 ELSE 0 END) as successful,
     SUM(CASE WHEN success = false THEN 1 ELSE 0 END) as failed
   FROM notification_audit_log
   WHERE created_at > NOW() - INTERVAL '1 hour';
   ```

2. **Channel Delivery Rates**
   - WebSocket delivery rate
   - Email delivery rate
   - SMS delivery rate

3. **Notification Latency**
   - Time from trigger to delivery
   - Template rendering time
   - Channel-specific latency

4. **Smart Filtering Effectiveness**
   - % of notifications filtered (prevented)
   - % of CRITICAL alerts delivered
   - False positive rate

### Log Queries

```bash
# All notification triggers in last hour
docker logs healthdata-quality-measure --since 1h | grep "notification"

# Failed notifications
docker logs healthdata-quality-measure | grep "Failed to.*notification"

# SMS deliveries
docker logs healthdata-quality-measure | grep "sendSms=true"

# Filtered notifications
docker logs healthdata-quality-measure | grep "Skipping.*notification"
```

---

## Troubleshooting Guide

### Notifications Not Triggering

1. **Check Service Health:**
   ```bash
   curl http://localhost:8087/quality-measure/actuator/health
   ```

2. **Verify Notification Triggers Are Loaded:**
   ```bash
   docker logs healthdata-quality-measure | grep "CareGapNotificationTrigger\|ClinicalAlertNotificationTrigger\|MentalHealthNotificationTrigger"
   ```

3. **Check for Dependency Injection Errors:**
   ```bash
   docker logs healthdata-quality-measure | grep "NoSuchBeanDefinitionException"
   ```

### Templates Not Rendering

1. **Verify Thymeleaf Configuration:**
   ```bash
   docker logs healthdata-quality-measure | grep "ThymeleafTemplateRenderer"
   ```

2. **Check Template Files Exist:**
   ```bash
   docker exec healthdata-quality-measure ls -la /app/resources/templates/notifications/
   ```

### Smart Filtering Not Working

1. **Check Filter Logic:**
   ```bash
   docker logs healthdata-quality-measure | grep "shouldNotify\|shouldSend"
   ```

2. **Verify Priority/Severity Mapping:**
   ```bash
   docker logs healthdata-quality-measure | grep "priority=\|severity="
   ```

---

## Success Criteria

The notification system is considered fully validated when:

✅ All 10 integration tests pass
✅ Load test handles 100+ concurrent notifications
✅ Smart filtering prevents <70% of routine notifications
✅ CRITICAL alerts always deliver via SMS (<5s latency)
✅ Template rendering completes in <100ms
✅ Business logic resilient to notification failures (0 impact)
✅ No notification-related errors in production logs
✅ Email/SMS providers configured and operational

---

## Next Steps

Once all tests pass:

1. **Configure Real Notification Channels:**
   - Set up SendGrid/AWS SES for email
   - Set up Twilio/AWS SNS for SMS
   - Configure WebSocket server (if not already deployed)

2. **Set Up Monitoring & Alerts:**
   - Prometheus metrics for notification success rates
   - Grafana dashboards for channel delivery
   - PagerDuty alerts for notification failures

3. **User Acceptance Testing (UAT):**
   - Test with real clinicians
   - Validate notification content and formatting
   - Gather feedback on notification frequency

4. **Production Deployment:**
   - Deploy to production environment
   - Monitor for 24 hours
   - Gradually increase notification volume
   - Enable all notification types

---

**Test Guide Version:** 1.0
**Last Updated:** 2025-11-28
**Owner:** Development Team
**Status:** Ready for Execution
