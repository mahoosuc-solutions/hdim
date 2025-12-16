# Approval Service

Human-in-the-loop approval workflows for AI agent actions, data mutations, and clinical decisions requiring manual review.

## Overview

The Approval Service provides comprehensive workflow management for approvals requiring human oversight. It supports multiple approval types including AI agent actions, data mutations, exports, emergency access, and guardrail reviews. The service includes routing, escalation, notifications, and comprehensive audit trails.

## Key Features

### Approval Request Management
- Create approval requests with risk levels
- Assign to specific users or roles
- Auto-assignment based on request type
- Support for multiple approval types
- Correlation tracking for distributed workflows

### Approval Workflows
- Approve/reject decisions with reasoning
- Request reassignment
- Escalation to higher authority
- Expiration handling with auto-rejection
- Multi-stage approval chains

### Notification System
- Email notifications for new requests
- Expiration reminders (4 hours before)
- SMS alerts for critical requests
- Webhook integrations (n8n, Zapier)
- Real-time WebSocket updates

### Risk-Based Routing
- 4 risk levels: LOW, MEDIUM, HIGH, CRITICAL
- Risk-specific timeout durations
- Required approver roles by risk level
- Multi-approver requirements for CRITICAL
- Auto-approval for LOW risk after delay

### Analytics and Reporting
- Approval statistics by tenant
- Average approval time metrics
- Approval rate tracking
- Pending request monitoring
- Escalation frequency analysis

### Audit Trail
- Complete approval history
- State transition tracking
- Decision reasoning capture
- User attribution for all actions
- Immutable audit log

## Technology Stack

- **Spring Boot 3.x**: Core framework
- **PostgreSQL**: Approval and history storage
- **Redis**: Real-time updates and caching
- **Apache Kafka**: Event streaming
- **Spring Mail**: Email notifications
- **Thymeleaf**: Email templates
- **Flyway**: Database migrations

## API Endpoints

### Approval Requests
```
POST   /api/v1/approvals
       - Create new approval request

GET    /api/v1/approvals/{id}
       - Get specific approval request

GET    /api/v1/approvals?status={status}
       - Get all approvals for tenant

GET    /api/v1/approvals/pending?role={role}
       - Get pending approvals for role

GET    /api/v1/approvals/assigned?status={status}
       - Get approvals assigned to current user
```

### Approval Actions
```
POST   /api/v1/approvals/{id}/assign
       - Assign request to reviewer

POST   /api/v1/approvals/{id}/approve
       - Approve the request

POST   /api/v1/approvals/{id}/reject
       - Reject the request

POST   /api/v1/approvals/{id}/escalate
       - Escalate to higher authority
```

### History and Analytics
```
GET    /api/v1/approvals/{id}/history
       - Get approval history for request

GET    /api/v1/approvals/stats?days={days}
       - Get approval statistics

GET    /api/v1/approvals/expiring?hours={hours}
       - Get requests expiring soon
```

## Configuration

### Application Properties
```yaml
server.port: 8097
spring.datasource.url: jdbc:postgresql://localhost:5432/hdim_approval
hdim.approval.default-timeout-hours: 24
hdim.approval.auto-escalation-hours: 4
```

### Risk Level Configuration
```yaml
hdim.approval.risk-levels:
  LOW:
    auto-approve-delay-minutes: 30
    requires-multiple-approvers: false
  MEDIUM:
    requires-multiple-approvers: false
  HIGH:
    requires-multiple-approvers: false
    required-role: CLINICAL_SUPERVISOR
  CRITICAL:
    requires-multiple-approvers: true
    min-approvers: 2
    required-role: CLINICAL_DIRECTOR
```

### Request Type Rules
```yaml
hdim.approval.rules:
  AGENT_ACTION:
    default-risk: MEDIUM
    clinical-role-required: true
  DATA_MUTATION:
    default-risk: HIGH
    audit-required: true
  EXPORT:
    default-risk: HIGH
    compliance-review: true
  EMERGENCY_ACCESS:
    default-risk: CRITICAL
    requires-multiple-approvers: true
```

### Email Notifications
```yaml
hdim.approval.email:
  enabled: true
  from: noreply@hdim.health
  dashboard-url: http://localhost:5173/approvals
```

### Webhook Configuration
```yaml
hdim.approval.webhook:
  secret: ${WEBHOOK_SECRET}
  timeout-seconds: 30
  max-retries: 3
```

## Request Types

### Supported Request Types
- **AGENT_ACTION**: AI agent actions requiring approval
- **DATA_MUTATION**: Changes to clinical or administrative data
- **EXPORT**: Data export requests (PHI compliance)
- **WORKFLOW_DEPLOY**: Custom workflow deployment
- **DLQ_REPROCESS**: Dead letter queue reprocessing
- **GUARDRAIL_REVIEW**: AI guardrail violation review
- **CONSENT_CHANGE**: Patient consent modifications
- **EMERGENCY_ACCESS**: Break-glass emergency access

## Risk Levels

### LOW Risk
- Auto-approval after 30 minutes if no action
- Single approver required
- Standard timeout (24 hours)
- Example: DLQ reprocessing

### MEDIUM Risk
- Manual approval required
- Single approver
- Standard timeout
- Example: Agent actions

### HIGH Risk
- Clinical supervisor role required
- Single approver
- Standard timeout
- Example: Data mutations, exports

### CRITICAL Risk
- Clinical director role required
- Multiple approvers (minimum 2)
- Short timeout (4 hours)
- Example: Emergency access

## Running Locally

### Prerequisites
- Java 21
- PostgreSQL 14+
- Redis 6+
- Kafka 3+
- SMTP server (for email notifications)

### Environment Variables
```bash
export DB_PASSWORD=postgres
export WEBHOOK_SECRET=your-webhook-secret
export SMTP_USERNAME=your-email@example.com
export SMTP_PASSWORD=your-email-password
```

### Build
```bash
./gradlew :modules:services:approval-service:build
```

### Run
```bash
./gradlew :modules:services:approval-service:bootRun
```

### Run Tests
```bash
./gradlew :modules:services:approval-service:test
```

## Notification Templates

### Email Templates
- New approval request notification
- Request assigned notification
- Expiration reminder (4 hours before)
- Approval/rejection confirmation
- Escalation notification

### Template Location
```
src/main/resources/templates/
├── approval-request.html
├── approval-assigned.html
├── approval-reminder.html
└── approval-decision.html
```

## Integration

### Kafka Events
Published events:
- `approval.request.created`
- `approval.request.assigned`
- `approval.decision.made`
- `approval.escalated`
- `approval.expired`

### Webhook Integration
- n8n workflow triggers
- Zapier actions
- Custom webhook handlers
- Retry with exponential backoff

### Service Integrations
This service is called by:
- **Agent Runtime Service**: AI action approvals
- **FHIR Service**: Data mutation approvals
- **Export Service**: PHI export approvals
- **Access Control Service**: Emergency access

## Scheduled Tasks

### Background Jobs
- Expiration check (every minute)
- Auto-escalation (every 5 minutes)
- Reminder notifications (every hour)
- Statistics aggregation (daily)

## Security

### Access Control
- JWT-based authentication
- Role-based approval routing
- Tenant isolation
- User attribution on all actions
- IP address logging

### Audit Compliance
- Immutable approval history
- Decision reasoning required
- Timestamp tracking (created, assigned, decided)
- State transition log
- HIPAA-compliant audit trail

## Metrics and Monitoring

### Key Metrics
- Pending approval count by risk level
- Average time to decision
- Approval/rejection rate
- Escalation rate
- Expiration rate

### Actuator Endpoints
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## API Documentation

Swagger UI available at:
```
http://localhost:8097/swagger-ui.html
```

OpenAPI specification:
```
http://localhost:8097/api-docs
```

## Best Practices

### Request Creation
- Provide detailed context in payload
- Set appropriate risk level
- Include correlation ID for tracing
- Set reasonable expiration times

### Approval Decisions
- Always provide decision reasoning
- Document any concerns or conditions
- Escalate when uncertain
- Respond promptly to critical requests

### Configuration
- Adjust timeout based on organizational needs
- Configure notification channels appropriately
- Set up role-based routing rules
- Monitor approval metrics regularly

## License

Copyright (c) 2024 Mahoosuc Solutions
