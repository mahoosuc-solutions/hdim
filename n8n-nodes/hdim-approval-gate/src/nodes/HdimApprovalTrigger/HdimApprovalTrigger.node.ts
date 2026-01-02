import {
  IHookFunctions,
  IWebhookFunctions,
  INodeType,
  INodeTypeDescription,
  IWebhookResponseData,
  NodeOperationError,
} from 'n8n-workflow';
import * as crypto from 'crypto';

interface ApprovalWebhookPayload {
  requestId: string;
  tenantId: string;
  status: 'APPROVED' | 'REJECTED' | 'EXPIRED' | 'ESCALATED';
  entityType: string;
  entityId?: string;
  actionRequested: string;
  riskLevel: string;
  payload: Record<string, unknown>;
  decidedBy?: string;
  decidedAt?: string;
  decisionReason?: string;
  correlationId?: string;
}

export class HdimApprovalTrigger implements INodeType {
  description: INodeTypeDescription = {
    displayName: 'HDIM Approval Trigger',
    name: 'hdimApprovalTrigger',
    icon: 'file:hdim-approval.svg',
    group: ['trigger'],
    version: 1,
    subtitle: '={{$parameter["event"]}}',
    description: 'Starts the workflow when an HDIM approval decision is made',
    defaults: {
      name: 'HDIM Approval Trigger',
    },
    inputs: [],
    outputs: ['main'],
    credentials: [
      {
        name: 'hdimApprovalApi',
        required: true,
      },
    ],
    webhooks: [
      {
        name: 'default',
        httpMethod: 'POST',
        responseMode: 'onReceived',
        path: 'hdim-approval',
      },
    ],
    properties: [
      {
        displayName: 'Events',
        name: 'events',
        type: 'multiOptions',
        options: [
          {
            name: 'Approved',
            value: 'APPROVED',
            description: 'Trigger when a request is approved',
          },
          {
            name: 'Rejected',
            value: 'REJECTED',
            description: 'Trigger when a request is rejected',
          },
          {
            name: 'Expired',
            value: 'EXPIRED',
            description: 'Trigger when a request expires',
          },
          {
            name: 'Escalated',
            value: 'ESCALATED',
            description: 'Trigger when a request is escalated',
          },
        ],
        default: ['APPROVED', 'REJECTED'],
        required: true,
        description: 'Which approval events should trigger this workflow',
      },
      {
        displayName: 'Filter by Request Type',
        name: 'filterByRequestType',
        type: 'boolean',
        default: false,
        description: 'Whether to only trigger for specific request types',
      },
      {
        displayName: 'Request Types',
        name: 'requestTypes',
        type: 'multiOptions',
        displayOptions: {
          show: {
            filterByRequestType: [true],
          },
        },
        options: [
          { name: 'Agent Action', value: 'AGENT_ACTION' },
          { name: 'Data Mutation', value: 'DATA_MUTATION' },
          { name: 'Export', value: 'EXPORT' },
          { name: 'Workflow Deploy', value: 'WORKFLOW_DEPLOY' },
          { name: 'DLQ Reprocess', value: 'DLQ_REPROCESS' },
          { name: 'Guardrail Review', value: 'GUARDRAIL_REVIEW' },
          { name: 'Consent Change', value: 'CONSENT_CHANGE' },
          { name: 'Emergency Access', value: 'EMERGENCY_ACCESS' },
        ],
        default: [],
        description: 'Only trigger for these request types',
      },
      {
        displayName: 'Filter by Risk Level',
        name: 'filterByRiskLevel',
        type: 'boolean',
        default: false,
        description: 'Whether to only trigger for specific risk levels',
      },
      {
        displayName: 'Risk Levels',
        name: 'riskLevels',
        type: 'multiOptions',
        displayOptions: {
          show: {
            filterByRiskLevel: [true],
          },
        },
        options: [
          { name: 'Low', value: 'LOW' },
          { name: 'Medium', value: 'MEDIUM' },
          { name: 'High', value: 'HIGH' },
          { name: 'Critical', value: 'CRITICAL' },
        ],
        default: [],
        description: 'Only trigger for these risk levels',
      },
      {
        displayName: 'Verify Webhook Signature',
        name: 'verifySignature',
        type: 'boolean',
        default: true,
        description: 'Whether to verify the webhook signature using the shared secret',
      },
      {
        displayName: 'Filter by Correlation ID Pattern',
        name: 'correlationIdPattern',
        type: 'string',
        default: '',
        placeholder: 'workflow-xyz-*',
        description: 'Optional glob pattern to match correlation IDs (e.g., "workflow-*")',
      },
    ],
  };

  webhookMethods = {
    default: {
      async checkExists(this: IHookFunctions): Promise<boolean> {
        // Webhook URL is managed by n8n, no external registration needed
        // But we could register with HDIM if we wanted to track webhooks
        return true;
      },
      async create(this: IHookFunctions): Promise<boolean> {
        // Could register webhook URL with HDIM Approval Service here
        const webhookUrl = this.getNodeWebhookUrl('default');
        console.log(`HDIM Approval Trigger webhook created at: ${webhookUrl}`);
        return true;
      },
      async delete(this: IHookFunctions): Promise<boolean> {
        // Could unregister webhook URL from HDIM Approval Service here
        return true;
      },
    },
  };

  async webhook(this: IWebhookFunctions): Promise<IWebhookResponseData> {
    const req = this.getRequestObject();
    const body = this.getBodyData() as ApprovalWebhookPayload;

    // Verify webhook signature if enabled
    const verifySignature = this.getNodeParameter('verifySignature', true) as boolean;
    if (verifySignature) {
      try {
        const credentials = await this.getCredentials('hdimApprovalApi');
        const webhookSecret = credentials.webhookSecret as string;

        if (webhookSecret) {
          const signature = req.headers['x-hdim-signature'] as string;
          if (!signature) {
            return {
              webhookResponse: { status: 'error', message: 'Missing signature' },
              workflowData: [],
            };
          }

          const expectedSignature = crypto
            .createHmac('sha256', webhookSecret)
            .update(JSON.stringify(body))
            .digest('hex');

          if (signature !== `sha256=${expectedSignature}`) {
            return {
              webhookResponse: { status: 'error', message: 'Invalid signature' },
              workflowData: [],
            };
          }
        }
      } catch (error) {
        // If credentials fail to load, continue without verification
        console.warn('Could not verify webhook signature:', error);
      }
    }

    // Check if event matches
    const events = this.getNodeParameter('events', []) as string[];
    if (!events.includes(body.status)) {
      return {
        webhookResponse: { status: 'filtered', message: 'Event not in filter list' },
        workflowData: [],
      };
    }

    // Check request type filter
    const filterByRequestType = this.getNodeParameter('filterByRequestType', false) as boolean;
    if (filterByRequestType) {
      const requestTypes = this.getNodeParameter('requestTypes', []) as string[];
      // Access requestType from payload if not directly on body
      const requestType = (body as any).requestType || body.payload?.requestType;
      if (requestTypes.length > 0 && !requestTypes.includes(requestType)) {
        return {
          webhookResponse: { status: 'filtered', message: 'Request type not in filter list' },
          workflowData: [],
        };
      }
    }

    // Check risk level filter
    const filterByRiskLevel = this.getNodeParameter('filterByRiskLevel', false) as boolean;
    if (filterByRiskLevel) {
      const riskLevels = this.getNodeParameter('riskLevels', []) as string[];
      if (riskLevels.length > 0 && !riskLevels.includes(body.riskLevel)) {
        return {
          webhookResponse: { status: 'filtered', message: 'Risk level not in filter list' },
          workflowData: [],
        };
      }
    }

    // Check correlation ID pattern
    const correlationIdPattern = this.getNodeParameter('correlationIdPattern', '') as string;
    if (correlationIdPattern && body.correlationId) {
      const pattern = correlationIdPattern.replace(/\*/g, '.*');
      const regex = new RegExp(`^${pattern}$`);
      if (!regex.test(body.correlationId)) {
        return {
          webhookResponse: { status: 'filtered', message: 'Correlation ID does not match pattern' },
          workflowData: [],
        };
      }
    }

    // Return the approval data to the workflow
    return {
      webhookResponse: { status: 'ok' },
      workflowData: [
        [
          {
            json: {
              requestId: body.requestId,
              tenantId: body.tenantId,
              status: body.status,
              entityType: body.entityType,
              entityId: body.entityId,
              actionRequested: body.actionRequested,
              riskLevel: body.riskLevel,
              payload: body.payload,
              decidedBy: body.decidedBy,
              decidedAt: body.decidedAt,
              decisionReason: body.decisionReason,
              correlationId: body.correlationId,
              // Helper flags
              isApproved: body.status === 'APPROVED',
              isRejected: body.status === 'REJECTED',
              isExpired: body.status === 'EXPIRED',
              isEscalated: body.status === 'ESCALATED',
            },
          },
        ],
      ],
    };
  }
}
