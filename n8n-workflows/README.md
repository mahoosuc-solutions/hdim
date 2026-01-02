# HDIM n8n Workflows

This directory contains pre-built n8n workflows for HDIM Healthcare Platform integrations.

## Prerequisites

1. n8n installed (self-hosted or cloud)
2. HDIM Approval Gate custom node installed (`n8n-nodes-hdim-approval-gate`)
3. Credentials configured for:
   - HDIM Approval API
   - DLQ Service API
   - Slack (optional, for notifications)

## Available Workflows

### 1. DLQ Approval Handler (`dlq-approval-handler.json`)

**Purpose:** Automatically processes failed messages from the Dead Letter Queue (DLQ) with human approval workflow.

**Flow:**
1. Polls DLQ every minute for failed messages
2. For messages under retry limit (< 3 retries): Auto-retry
3. For messages exceeding retry limit: Create approval request
4. On approval: Reprocess the message
5. On rejection: Archive the message
6. On timeout: Mark as timed out
7. Send Slack notifications for outcomes

**Environment Variables:**
- `DLQ_SERVICE_URL`: URL of your DLQ service (e.g., `http://dlq-service:8098`)

**Credentials Required:**
- `dlq-api-creds`: HTTP Header Auth with X-API-Key and X-Tenant-Id
- `hdim-approval-creds`: HDIM Approval API credentials
- `slack-creds`: Slack API credentials (optional)

### Import Instructions

1. Open n8n dashboard
2. Go to **Workflows** > **Import from File**
3. Select the JSON workflow file
4. Configure credentials
5. Set environment variables
6. Activate the workflow

## Creating Custom Workflows

### Using HDIM Approval Gate Node

```javascript
// Example: Creating an approval request in a workflow
{
  "operation": "createAndWait",
  "requestType": "DATA_MUTATION",
  "riskLevel": "HIGH",
  "entityType": "Patient",
  "entityId": "patient-123",
  "actionRequested": "UPDATE",
  "payload": "{ \"field\": \"value\" }",
  "requestedBy": "n8n-workflow",
  "timeoutHours": 24,
  "pollingInterval": 60,
  "maxWaitMinutes": 1440
}
```

### Using HDIM Approval Trigger Node

```javascript
// Example: Webhook trigger configuration
{
  "events": ["APPROVED", "REJECTED"],
  "filterByRequestType": true,
  "requestTypes": ["AGENT_ACTION", "DATA_MUTATION"],
  "filterByRiskLevel": true,
  "riskLevels": ["HIGH", "CRITICAL"],
  "verifySignature": true
}
```

## Workflow Patterns

### Pattern 1: Synchronous Approval (Wait for Decision)

```
[Trigger] → [HDIM Approval Gate (createAndWait)] → [Continue on Approved]
                                                 → [Handle Rejection]
                                                 → [Handle Timeout]
```

### Pattern 2: Asynchronous Approval (Webhook)

```
Workflow A:
[Trigger] → [HDIM Approval Gate (createOnly)] → [Continue Processing]

Workflow B:
[HDIM Approval Trigger] → [Handle Decision]
```

### Pattern 3: Batch Approval

```
[Schedule] → [Fetch Items] → [Split] → [Create Approval (batch)] → [Wait] → [Process Results]
```

## Troubleshooting

### Common Issues

1. **Approval request not created**
   - Check HDIM Approval API credentials
   - Verify tenant ID is correct
   - Check network connectivity to approval service

2. **Webhook not triggering**
   - Verify webhook URL is accessible
   - Check webhook secret matches
   - Ensure events are correctly filtered

3. **Timeout before decision**
   - Increase `timeoutHours` parameter
   - Adjust `pollingInterval` for faster response
   - Check reviewer pool has available reviewers

### Debugging

Enable debug logging in n8n:
```bash
export N8N_LOG_LEVEL=debug
```

## Best Practices

1. **Set appropriate risk levels** - Critical actions should use HIGH or CRITICAL
2. **Include context in payload** - Help reviewers make informed decisions
3. **Use correlation IDs** - Track related requests across systems
4. **Set reasonable timeouts** - Match SLA requirements
5. **Implement error handling** - Always handle rejection and timeout cases
6. **Notify stakeholders** - Use Slack/email for important decisions
