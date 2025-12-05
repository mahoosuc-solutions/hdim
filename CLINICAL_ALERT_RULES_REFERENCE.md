# Clinical Alert Rules - Quick Reference

## Alert Thresholds & Actions

### Mental Health Alerts

| Condition | Threshold | Severity | Channels | Action Timeline |
|-----------|-----------|----------|----------|----------------|
| **Suicide Risk** | PHQ-9 item 9 > 0 | CRITICAL | WebSocket + Email + SMS | **IMMEDIATE** - Contact patient now |
| **Severe Depression** | PHQ-9 ≥ 20 | CRITICAL | WebSocket + Email + SMS | Within 24 hours |
| **Severe Anxiety** | GAD-7 ≥ 15 | HIGH | WebSocket + Email | Within 24-48 hours |
| **Moderate Depression** | PHQ-9 10-19 | No alert (tracked) | - | Next scheduled visit |
| **Moderate Anxiety** | GAD-7 10-14 | No alert (tracked) | - | Next scheduled visit |

### Risk Level Alerts

| Risk Level | Score Range | Severity | Channels | Action |
|------------|-------------|----------|----------|--------|
| **VERY_HIGH** | 75-100 | HIGH | WebSocket + Email | Enroll in care coordination, weekly monitoring |
| **HIGH** | 50-74 | No alert | - | Monthly check-ins |
| **MODERATE** | 25-49 | No alert | - | Routine monitoring |
| **LOW** | 0-24 | No alert | - | Standard care |

### Health Score Alerts

| Change | Threshold | Severity | Channels | Action |
|--------|-----------|----------|----------|--------|
| **Significant Decline** | Drop ≥ 15 points | MEDIUM | WebSocket | Review during next contact |
| **Moderate Decline** | Drop 10-14 points | No alert (tracked) | - | Monitor trend |
| **Improvement** | Any increase | No alert (celebrate!) | - | Continue current plan |

### Chronic Disease Alerts

| Condition | Trigger | Severity | Channels |
|-----------|---------|----------|----------|
| **Lab Deterioration** | Critical value or worsening trend | MEDIUM | WebSocket |
| **Disease Progression** | Multiple metrics declining | MEDIUM | WebSocket |

## Deduplication Rules

- **Window**: 24 hours
- **Scope**: Same patient + Same alert type
- **Logic**: If identical alert type triggered within 24 hours, suppress new alert
- **Exceptions**: None (prevents alert fatigue)

## Notification Channel Selection

```
Alert Severity → Notification Channels

CRITICAL  →  WebSocket (real-time)
          →  Email (care team)
          →  SMS (on-call provider)

HIGH      →  WebSocket (real-time)
          →  Email (care team)

MEDIUM    →  WebSocket (real-time)

LOW       →  WebSocket (real-time)
```

## Action Protocols by Severity

### CRITICAL Alerts

**Suicide Risk (PHQ-9 item 9 > 0)**
1. Contact patient IMMEDIATELY (phone call)
2. Assess immediate safety
3. Implement crisis intervention protocol
4. Consider emergency services if unable to reach
5. Document all actions in EMR
6. Schedule urgent follow-up within 24 hours

**Severe Depression/Anxiety (PHQ-9 ≥20, GAD-7 ≥15)**
1. Contact patient within 24 hours
2. Conduct brief safety assessment
3. Schedule mental health evaluation
4. Review medication management
5. Update care plan
6. Coordinate with behavioral health

### HIGH Alerts

**Risk Escalation (VERY_HIGH)**
1. Review patient chart and recent events
2. Contact patient within 48 hours
3. Assess contributing factors
4. Enroll in care coordination program
5. Schedule comprehensive care plan review
6. Implement weekly check-ins

### MEDIUM Alerts

**Health Score Decline**
1. Review alert during next scheduled contact
2. Assess trends and contributing factors
3. Update care plan if needed
4. Address identified gaps
5. Schedule follow-up as appropriate

## Clinical Decision Support

### PHQ-9 Scoring Guide

**Total Score Interpretation:**
- 0-4: Minimal depression
- 5-9: Mild depression
- 10-14: Moderate depression (positive screen)
- 15-19: Moderately severe depression
- 20-27: Severe depression (CRITICAL ALERT)

**Item 9 (Suicidal Ideation):**
- 0: Never → No alert
- 1: Several days → CRITICAL ALERT
- 2: More than half the days → CRITICAL ALERT
- 3: Nearly every day → CRITICAL ALERT

### GAD-7 Scoring Guide

**Total Score Interpretation:**
- 0-4: Minimal anxiety
- 5-9: Mild anxiety
- 10-14: Moderate anxiety (positive screen)
- 15-21: Severe anxiety (HIGH ALERT)

## Multi-Tenant Considerations

All alerts are isolated by `tenant_id`:
- Alerts only visible within tenant
- Notification routing per tenant configuration
- Recipient lists managed per tenant
- Analytics separated by tenant

## API Quick Reference

### Get Active Alerts
```bash
GET /quality-measure/alerts?patientId={id}
Headers: X-Tenant-ID: {tenant}
```

### Acknowledge Alert
```bash
POST /quality-measure/alerts/{id}/acknowledge
Headers: X-Tenant-ID: {tenant}
Body: {"acknowledgedBy": "provider-id"}
```

### Resolve Alert
```bash
POST /quality-measure/alerts/{id}/resolve
Headers: X-Tenant-ID: {tenant}
Body: {"resolvedBy": "provider-id"}
```

## Email Template Variables

Available in all email templates:
- `{severity}` - CRITICAL, HIGH, MEDIUM, LOW
- `{alertType}` - Alert type name
- `{patientId}` - Patient identifier
- `{title}` - Alert title
- `{message}` - Alert message
- `{triggeredAt}` - When alert was created
- `{escalated}` - Whether alert is escalated
- `{tenantId}` - Tenant identifier

## SMS Format

**Character Limit**: 160 characters
**Format**: `{Prefix} {Title} - Patient {ID}. Review alert immediately.`
**Prefix**:
- CRITICAL: "URGENT: "
- All others: None

**Example**:
```
URGENT: Suicide Risk Detected - Patient pat-123. Review alert immediately.
```

## Alert Lifecycle States

```
ACTIVE → ACKNOWLEDGED → RESOLVED

ACTIVE:
  - Just triggered
  - Requires attention
  - Appears in active alert lists

ACKNOWLEDGED:
  - Provider has seen alert
  - Action in progress
  - Still tracked

RESOLVED:
  - Issue addressed
  - No longer active
  - Moved to history
```

## Configuration Recommendations

### Production Settings

**Deduplication Window**: 24 hours (recommended)
- Too short: Alert fatigue
- Too long: Missed deterioration

**Email Recipients**:
- Primary: Care coordination team
- CC: Patient's care team
- BCC: Quality assurance

**SMS Recipients**:
- CRITICAL only: On-call provider
- Verify phone numbers quarterly

**WebSocket**:
- All alerts for real-time dashboard
- Filter by severity in UI

## Monitoring & Metrics

Track these KPIs:
1. **Alert Volume**: Alerts per day by severity
2. **Acknowledgement Time**: Time to acknowledge CRITICAL alerts
3. **Resolution Time**: Time from trigger to resolution
4. **False Positive Rate**: Alerts that didn't require action
5. **Channel Delivery Rate**: Notification success rate per channel

## Troubleshooting

### Alert Not Created
- Check assessment threshold
- Verify tenant_id matches
- Check deduplication window
- Review Kafka consumer logs

### Notification Not Received
- Verify channel configuration
- Check SMTP settings (email)
- Verify phone numbers (SMS)
- Review NotificationService logs

### Duplicate Alerts
- Check deduplication logic
- Verify 24-hour window calculation
- Review alert timestamps

## Support Contacts

- **Technical Issues**: engineering@healthdata.com
- **Clinical Questions**: clinical-support@healthdata.com
- **Emergency Escalation**: 1-800-HEALTH-1
