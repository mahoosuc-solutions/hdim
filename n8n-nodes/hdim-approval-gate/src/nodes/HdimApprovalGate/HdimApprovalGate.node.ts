import {
  IExecuteFunctions,
  INodeExecutionData,
  INodeType,
  INodeTypeDescription,
  NodeOperationError,
} from 'n8n-workflow';

interface ApprovalRequest {
  id: string;
  tenantId: string;
  requestType: string;
  entityType: string;
  entityId?: string;
  actionRequested: string;
  payload: Record<string, unknown>;
  riskLevel: string;
  status: string;
  requestedBy: string;
  requestedAt: string;
  expiresAt?: string;
  correlationId?: string;
}

interface ApprovalResponse {
  requestId: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED' | 'ESCALATED';
  decidedBy?: string;
  decidedAt?: string;
  decisionReason?: string;
}

export class HdimApprovalGate implements INodeType {
  description: INodeTypeDescription = {
    displayName: 'HDIM Approval Gate',
    name: 'hdimApprovalGate',
    icon: 'file:hdim-approval.svg',
    group: ['transform'],
    version: 1,
    subtitle: '={{$parameter["operation"] + ": " + $parameter["requestType"]}}',
    description: 'Create approval requests and wait for human decisions in HDIM workflows',
    defaults: {
      name: 'HDIM Approval Gate',
    },
    inputs: ['main'],
    outputs: ['main', 'main', 'main'],
    outputNames: ['Approved', 'Rejected', 'Timeout/Error'],
    credentials: [
      {
        name: 'hdimApprovalApi',
        required: true,
      },
    ],
    properties: [
      {
        displayName: 'Operation',
        name: 'operation',
        type: 'options',
        noDataExpression: true,
        options: [
          {
            name: 'Create & Wait',
            value: 'createAndWait',
            description: 'Create an approval request and wait for decision',
            action: 'Create approval request and wait for decision',
          },
          {
            name: 'Create Only',
            value: 'createOnly',
            description: 'Create an approval request without waiting (use with webhook trigger)',
            action: 'Create approval request without waiting',
          },
          {
            name: 'Check Status',
            value: 'checkStatus',
            description: 'Check the status of an existing approval request',
            action: 'Check status of approval request',
          },
        ],
        default: 'createAndWait',
      },
      // Request Type
      {
        displayName: 'Request Type',
        name: 'requestType',
        type: 'options',
        displayOptions: {
          show: {
            operation: ['createAndWait', 'createOnly'],
          },
        },
        options: [
          {
            name: 'Agent Action',
            value: 'AGENT_ACTION',
            description: 'AI agent wants to perform an action',
          },
          {
            name: 'Data Mutation',
            value: 'DATA_MUTATION',
            description: 'Creating, updating, or deleting clinical data',
          },
          {
            name: 'Export Request',
            value: 'EXPORT',
            description: 'Exporting patient data or reports',
          },
          {
            name: 'Workflow Deploy',
            value: 'WORKFLOW_DEPLOY',
            description: 'Deploying a new workflow to production',
          },
          {
            name: 'DLQ Reprocess',
            value: 'DLQ_REPROCESS',
            description: 'Reprocessing failed messages from dead letter queue',
          },
          {
            name: 'Guardrail Review',
            value: 'GUARDRAIL_REVIEW',
            description: 'AI content flagged by guardrails needs review',
          },
          {
            name: 'Consent Change',
            value: 'CONSENT_CHANGE',
            description: 'Changes to patient consent records',
          },
          {
            name: 'Emergency Access',
            value: 'EMERGENCY_ACCESS',
            description: 'Emergency "break glass" access request',
          },
        ],
        default: 'DATA_MUTATION',
        required: true,
      },
      // Risk Level
      {
        displayName: 'Risk Level',
        name: 'riskLevel',
        type: 'options',
        displayOptions: {
          show: {
            operation: ['createAndWait', 'createOnly'],
          },
        },
        options: [
          {
            name: 'Low',
            value: 'LOW',
            description: 'Low risk - may auto-approve after delay',
          },
          {
            name: 'Medium',
            value: 'MEDIUM',
            description: 'Medium risk - single approver required',
          },
          {
            name: 'High',
            value: 'HIGH',
            description: 'High risk - clinical supervisor required',
          },
          {
            name: 'Critical',
            value: 'CRITICAL',
            description: 'Critical - multiple approvers required',
          },
        ],
        default: 'MEDIUM',
        required: true,
      },
      // Entity Type
      {
        displayName: 'Entity Type',
        name: 'entityType',
        type: 'string',
        displayOptions: {
          show: {
            operation: ['createAndWait', 'createOnly'],
          },
        },
        default: '',
        placeholder: 'Patient, Observation, Workflow',
        description: 'The type of entity this approval relates to',
        required: true,
      },
      // Entity ID
      {
        displayName: 'Entity ID',
        name: 'entityId',
        type: 'string',
        displayOptions: {
          show: {
            operation: ['createAndWait', 'createOnly'],
          },
        },
        default: '',
        placeholder: 'patient-123',
        description: 'Optional identifier of the specific entity',
      },
      // Action Requested
      {
        displayName: 'Action Requested',
        name: 'actionRequested',
        type: 'options',
        displayOptions: {
          show: {
            operation: ['createAndWait', 'createOnly'],
          },
        },
        options: [
          { name: 'Create', value: 'CREATE' },
          { name: 'Update', value: 'UPDATE' },
          { name: 'Delete', value: 'DELETE' },
          { name: 'Execute', value: 'EXECUTE' },
          { name: 'Deploy', value: 'DEPLOY' },
          { name: 'Export', value: 'EXPORT' },
          { name: 'Access', value: 'ACCESS' },
        ],
        default: 'EXECUTE',
        required: true,
      },
      // Payload
      {
        displayName: 'Payload',
        name: 'payload',
        type: 'json',
        displayOptions: {
          show: {
            operation: ['createAndWait', 'createOnly'],
          },
        },
        default: '{}',
        description: 'JSON payload with context for the reviewer',
        typeOptions: {
          alwaysOpenEditWindow: true,
        },
      },
      // Requester
      {
        displayName: 'Requested By',
        name: 'requestedBy',
        type: 'string',
        displayOptions: {
          show: {
            operation: ['createAndWait', 'createOnly'],
          },
        },
        default: 'n8n-workflow',
        description: 'Identifier of who/what is making this request',
        required: true,
      },
      // Assigned Role
      {
        displayName: 'Assigned Role',
        name: 'assignedRole',
        type: 'string',
        displayOptions: {
          show: {
            operation: ['createAndWait', 'createOnly'],
          },
        },
        default: '',
        placeholder: 'CLINICAL_REVIEWER, ADMIN',
        description: 'Optional role required to approve this request',
      },
      // Timeout
      {
        displayName: 'Timeout (Hours)',
        name: 'timeoutHours',
        type: 'number',
        displayOptions: {
          show: {
            operation: ['createAndWait', 'createOnly'],
          },
        },
        default: 24,
        description: 'Hours until the approval request expires',
        typeOptions: {
          minValue: 1,
          maxValue: 168, // 7 days
        },
      },
      // Polling Interval (for createAndWait)
      {
        displayName: 'Polling Interval (Seconds)',
        name: 'pollingInterval',
        type: 'number',
        displayOptions: {
          show: {
            operation: ['createAndWait'],
          },
        },
        default: 30,
        description: 'How often to check for approval decision',
        typeOptions: {
          minValue: 10,
          maxValue: 3600,
        },
      },
      // Max Wait Time (for createAndWait)
      {
        displayName: 'Max Wait Time (Minutes)',
        name: 'maxWaitMinutes',
        type: 'number',
        displayOptions: {
          show: {
            operation: ['createAndWait'],
          },
        },
        default: 60,
        description: 'Maximum time to wait before continuing to timeout output',
        typeOptions: {
          minValue: 1,
          maxValue: 1440, // 24 hours
        },
      },
      // Approval Request ID (for checkStatus)
      {
        displayName: 'Approval Request ID',
        name: 'approvalRequestId',
        type: 'string',
        displayOptions: {
          show: {
            operation: ['checkStatus'],
          },
        },
        default: '',
        required: true,
        description: 'The ID of the approval request to check',
      },
      // Correlation ID
      {
        displayName: 'Correlation ID',
        name: 'correlationId',
        type: 'string',
        displayOptions: {
          show: {
            operation: ['createAndWait', 'createOnly'],
          },
        },
        default: '',
        description: 'Optional correlation ID for tracking across systems',
      },
      // Source Service
      {
        displayName: 'Source Service',
        name: 'sourceService',
        type: 'string',
        displayOptions: {
          show: {
            operation: ['createAndWait', 'createOnly'],
          },
        },
        default: 'n8n',
        description: 'Name of the source service/workflow',
      },
      // Callback URL (for createOnly with webhook)
      {
        displayName: 'Callback URL',
        name: 'callbackUrl',
        type: 'string',
        displayOptions: {
          show: {
            operation: ['createOnly'],
          },
        },
        default: '',
        placeholder: 'https://your-n8n-instance/webhook/approval-callback',
        description: 'URL to call when approval decision is made',
      },
    ],
  };

  async execute(this: IExecuteFunctions): Promise<INodeExecutionData[][]> {
    const items = this.getInputData();
    const returnData: INodeExecutionData[][] = [[], [], []]; // [Approved, Rejected, Timeout/Error]

    const credentials = await this.getCredentials('hdimApprovalApi');
    const baseUrl = credentials.baseUrl as string;
    const tenantId = credentials.tenantId as string;

    for (let i = 0; i < items.length; i++) {
      const operation = this.getNodeParameter('operation', i) as string;

      try {
        if (operation === 'createOnly' || operation === 'createAndWait') {
          const result = await this.createApprovalRequest(i, baseUrl, tenantId, operation === 'createAndWait');

          if (result.status === 'APPROVED') {
            returnData[0].push({ json: result });
          } else if (result.status === 'REJECTED') {
            returnData[1].push({ json: result });
          } else {
            // PENDING, EXPIRED, or timeout
            returnData[2].push({ json: result });
          }
        } else if (operation === 'checkStatus') {
          const requestId = this.getNodeParameter('approvalRequestId', i) as string;
          const result = await this.checkApprovalStatus(requestId, baseUrl, tenantId);

          if (result.status === 'APPROVED') {
            returnData[0].push({ json: result });
          } else if (result.status === 'REJECTED') {
            returnData[1].push({ json: result });
          } else {
            returnData[2].push({ json: result });
          }
        }
      } catch (error) {
        if (this.continueOnFail()) {
          returnData[2].push({
            json: {
              error: error instanceof Error ? error.message : String(error),
              itemIndex: i,
            },
          });
          continue;
        }
        throw error;
      }
    }

    return returnData;
  }

  private async createApprovalRequest(
    this: IExecuteFunctions,
    itemIndex: number,
    baseUrl: string,
    tenantId: string,
    waitForDecision: boolean
  ): Promise<ApprovalResponse> {
    const requestType = this.getNodeParameter('requestType', itemIndex) as string;
    const riskLevel = this.getNodeParameter('riskLevel', itemIndex) as string;
    const entityType = this.getNodeParameter('entityType', itemIndex) as string;
    const entityId = this.getNodeParameter('entityId', itemIndex, '') as string;
    const actionRequested = this.getNodeParameter('actionRequested', itemIndex) as string;
    const payloadStr = this.getNodeParameter('payload', itemIndex, '{}') as string;
    const requestedBy = this.getNodeParameter('requestedBy', itemIndex) as string;
    const assignedRole = this.getNodeParameter('assignedRole', itemIndex, '') as string;
    const timeoutHours = this.getNodeParameter('timeoutHours', itemIndex, 24) as number;
    const correlationId = this.getNodeParameter('correlationId', itemIndex, '') as string;
    const sourceService = this.getNodeParameter('sourceService', itemIndex, 'n8n') as string;
    const callbackUrl = this.getNodeParameter('callbackUrl', itemIndex, '') as string;

    let payload: Record<string, unknown>;
    try {
      payload = JSON.parse(payloadStr);
    } catch {
      payload = { raw: payloadStr };
    }

    // Add callback URL to payload if provided
    if (callbackUrl) {
      payload.n8nCallbackUrl = callbackUrl;
    }

    const expiresAt = new Date(Date.now() + timeoutHours * 60 * 60 * 1000).toISOString();

    const requestBody: Partial<ApprovalRequest> = {
      tenantId,
      requestType,
      entityType,
      actionRequested,
      payload,
      riskLevel,
      requestedBy,
      expiresAt,
      correlationId: correlationId || undefined,
    };

    if (entityId) {
      requestBody.entityId = entityId;
    }

    // Create the approval request
    const createResponse = await this.helpers.httpRequest({
      method: 'POST',
      url: `${baseUrl}/api/v1/approvals`,
      body: requestBody,
      headers: {
        'X-Tenant-Id': tenantId,
        'Content-Type': 'application/json',
      },
    });

    const approvalRequest = createResponse as ApprovalRequest;

    // If assignedRole is specified, assign the request
    if (assignedRole) {
      await this.helpers.httpRequest({
        method: 'POST',
        url: `${baseUrl}/api/v1/approvals/${approvalRequest.id}/assign`,
        body: { assignedRole },
        headers: {
          'X-Tenant-Id': tenantId,
          'Content-Type': 'application/json',
        },
      });
    }

    // If not waiting, return immediately
    if (!waitForDecision) {
      return {
        requestId: approvalRequest.id,
        status: 'PENDING',
      };
    }

    // Poll for decision
    const pollingInterval = this.getNodeParameter('pollingInterval', itemIndex, 30) as number;
    const maxWaitMinutes = this.getNodeParameter('maxWaitMinutes', itemIndex, 60) as number;
    const maxWaitMs = maxWaitMinutes * 60 * 1000;
    const startTime = Date.now();

    while (Date.now() - startTime < maxWaitMs) {
      // Wait for polling interval
      await new Promise(resolve => setTimeout(resolve, pollingInterval * 1000));

      // Check status
      const statusResponse = await this.helpers.httpRequest({
        method: 'GET',
        url: `${baseUrl}/api/v1/approvals/${approvalRequest.id}`,
        headers: {
          'X-Tenant-Id': tenantId,
          'Content-Type': 'application/json',
        },
      });

      const currentRequest = statusResponse as ApprovalRequest;

      if (currentRequest.status !== 'PENDING' && currentRequest.status !== 'ASSIGNED') {
        return {
          requestId: currentRequest.id,
          status: currentRequest.status as ApprovalResponse['status'],
        };
      }
    }

    // Timeout reached
    return {
      requestId: approvalRequest.id,
      status: 'PENDING',
    };
  }

  private async checkApprovalStatus(
    this: IExecuteFunctions,
    requestId: string,
    baseUrl: string,
    tenantId: string
  ): Promise<ApprovalResponse> {
    const response = await this.helpers.httpRequest({
      method: 'GET',
      url: `${baseUrl}/api/v1/approvals/${requestId}`,
      headers: {
        'X-Tenant-Id': tenantId,
        'Content-Type': 'application/json',
      },
    });

    const approvalRequest = response as ApprovalRequest;

    return {
      requestId: approvalRequest.id,
      status: approvalRequest.status as ApprovalResponse['status'],
    };
  }
}
