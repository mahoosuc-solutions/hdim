/**
 * Approval Types for Human-in-the-Loop Dashboard
 */

export enum RequestType {
  AGENT_ACTION = 'AGENT_ACTION',
  GUARDRAIL_REVIEW = 'GUARDRAIL_REVIEW',
  DATA_MUTATION = 'DATA_MUTATION',
  EXPORT = 'EXPORT',
  WORKFLOW_DEPLOY = 'WORKFLOW_DEPLOY',
  DLQ_REPROCESS = 'DLQ_REPROCESS',
  CONSENT_CHANGE = 'CONSENT_CHANGE',
  EMERGENCY_ACCESS = 'EMERGENCY_ACCESS',
}

export enum RiskLevel {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL',
}

export enum ApprovalStatus {
  PENDING = 'PENDING',
  ASSIGNED = 'ASSIGNED',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  EXPIRED = 'EXPIRED',
  ESCALATED = 'ESCALATED',
}

export interface ApprovalRequest {
  id: string;
  tenantId: string;
  requestType: RequestType;
  entityType: string;
  entityId: string | null;
  actionRequested: string;
  payload: Record<string, unknown>;
  confidenceScore: number | null;
  riskLevel: RiskLevel;
  status: ApprovalStatus;
  requestedBy: string;
  requestedAt: string;
  sourceService: string | null;
  correlationId: string | null;
  assignedTo: string | null;
  assignedAt: string | null;
  assignedRole: string | null;
  decisionBy: string | null;
  decisionAt: string | null;
  decisionReason: string | null;
  escalationCount: number;
  escalatedTo: string | null;
  escalatedAt: string | null;
  expiresAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ApprovalHistory {
  id: string;
  approvalRequestId: string;
  action: string;
  actor: string;
  details: Record<string, unknown> | null;
  createdAt: string;
}

export interface ApprovalStats {
  pending: number;
  assigned: number;
  approved: number;
  rejected: number;
  expired: number;
  escalated: number;
  avgResponseTimeHours: number;
  approvalRate: number;
}

export interface ApprovalDecision {
  reason: string;
}

export interface ApprovalAssignment {
  assignedTo: string;
}

export interface ApprovalEscalation {
  escalatedTo: string;
  reason: string;
}

export interface ApprovalFilters {
  status?: ApprovalStatus;
  requestType?: RequestType;
  riskLevel?: RiskLevel;
  assignedTo?: string;
  startDate?: string;
  endDate?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export type NotificationType = 'CREATED' | 'ASSIGNED' | 'STATUS_CHANGED' | 'EXPIRING_SOON';

export interface ApprovalNotification {
  requestId: string;
  type: NotificationType;
  tenantId: string;
  entityType: string;
  actionRequested: string;
  riskLevel: RiskLevel;
  status: ApprovalStatus;
  assignedTo?: string;
  assignedRole?: string;
  actor?: string;
  message: string;
  timestamp: string;
  expiresAt?: string;
  metadata?: Record<string, unknown>;
}
