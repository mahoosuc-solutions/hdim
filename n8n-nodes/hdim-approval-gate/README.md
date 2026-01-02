# n8n-nodes-hdim-approval-gate

This is an n8n community node package that provides Human-in-the-Loop (HITL) approval workflow capabilities for HDIM Healthcare Platform.

## Features

### HDIM Approval Gate Node

The Approval Gate node allows you to pause n8n workflows until a human approves or rejects the action. This is critical for healthcare workflows where certain actions require clinical oversight.

**Operations:**
- **Create & Wait**: Creates an approval request and pauses the workflow until a decision is made
- **Create Only**: Creates an approval request and continues (use with webhook trigger)
- **Check Status**: Checks the status of an existing approval request

**Outputs:**
- Output 1: Approved - continues when the request is approved
- Output 2: Rejected - continues when the request is rejected
- Output 3: Timeout/Error - continues when the request times out or an error occurs

### HDIM Approval Trigger Node

A webhook trigger that starts workflows when approval decisions are made. Use this in combination with "Create Only" for event-driven approval workflows.

**Filters:**
- Filter by event type (Approved, Rejected, Expired, Escalated)
- Filter by request type (Agent Action, Data Mutation, Export, etc.)
- Filter by risk level (Low, Medium, High, Critical)
- Filter by correlation ID pattern

## Installation

### In n8n

1. Go to **Settings** > **Community Nodes**
2. Select **Install a community node**
3. Enter `n8n-nodes-hdim-approval-gate`
4. Click **Install**

### Manual Installation

```bash
cd ~/.n8n/nodes
npm install n8n-nodes-hdim-approval-gate
```

## Configuration

### Credentials

Create HDIM Approval API credentials with:
- **Base URL**: Your HDIM Approval Service URL (e.g., `https://approval-api.hdim.health`)
- **Tenant ID**: Your HDIM tenant identifier
- **API Key**: Service-to-service API key
- **Webhook Secret**: (Optional) For signature verification

### Environment Variables

The HDIM Approval Service should be configured with:
```env
APPROVAL_SERVICE_URL=http://localhost:8097
HDIM_TENANT_ID=your-tenant-id
```

## Usage Examples

### Example 1: AI Agent Action Approval

```
[Trigger] → [AI Agent] → [HDIM Approval Gate] → [Execute Action]
                                    ↓ Rejected
                              [Log Rejection]
```

1. AI agent proposes an action
2. Approval Gate creates request and waits
3. Clinical staff reviews in HDIM dashboard
4. On approval: action executes
5. On rejection: rejection is logged

### Example 2: Async Approval with Webhook

```
[Trigger] → [Create Approval] → [Continue Processing]

[HDIM Approval Trigger] → [Handle Decision]
```

1. Workflow creates approval request (Create Only)
2. Workflow continues with other processing
3. Separate workflow triggered when decision is made

### Example 3: Data Export Approval

```
[API Request] → [HDIM Approval Gate (EXPORT/HIGH)] → [Generate Report]
                              ↓ Rejected
                        [Return Error]
```

1. User requests data export
2. High-risk export request created
3. Compliance officer reviews
4. Approved: Generate and deliver report
5. Rejected: Return error to user

## Request Types

| Type | Description | Default Risk |
|------|-------------|--------------|
| AGENT_ACTION | AI agent wants to perform an action | MEDIUM |
| DATA_MUTATION | Creating/updating/deleting clinical data | HIGH |
| EXPORT | Exporting patient data or reports | HIGH |
| WORKFLOW_DEPLOY | Deploying a new workflow to production | MEDIUM |
| DLQ_REPROCESS | Reprocessing failed messages | LOW |
| GUARDRAIL_REVIEW | AI content flagged by guardrails | HIGH |
| CONSENT_CHANGE | Changes to patient consent | HIGH |
| EMERGENCY_ACCESS | Break glass emergency access | CRITICAL |

## Risk Levels

| Level | Behavior |
|-------|----------|
| LOW | May auto-approve after 30-minute delay |
| MEDIUM | Single approver required |
| HIGH | Clinical supervisor role required |
| CRITICAL | Multiple approvers required |

## API Reference

### Create Approval Request

```javascript
POST /api/v1/approvals
{
  "tenantId": "tenant-123",
  "requestType": "DATA_MUTATION",
  "entityType": "Patient",
  "entityId": "patient-456",
  "actionRequested": "UPDATE",
  "payload": { ... },
  "riskLevel": "HIGH",
  "requestedBy": "n8n-workflow",
  "expiresAt": "2025-01-15T12:00:00Z"
}
```

### Check Approval Status

```javascript
GET /api/v1/approvals/{requestId}
```

### Webhook Callback

When an approval decision is made, HDIM sends:

```javascript
POST {callbackUrl}
X-HDIM-Signature: sha256=...

{
  "requestId": "approval-123",
  "tenantId": "tenant-123",
  "status": "APPROVED",
  "entityType": "Patient",
  "actionRequested": "UPDATE",
  "riskLevel": "HIGH",
  "payload": { ... },
  "decidedBy": "dr.smith",
  "decidedAt": "2025-01-14T10:30:00Z",
  "decisionReason": "Reviewed patient history, update is appropriate"
}
```

## Development

```bash
# Install dependencies
npm install

# Build
npm run build

# Watch mode
npm run dev

# Lint
npm run lint
```

## License

MIT

## Support

- Documentation: https://docs.hdim.health/integrations/n8n
- Issues: https://github.com/hdim-health/n8n-nodes-hdim-approval-gate/issues
- Email: support@hdim.health
