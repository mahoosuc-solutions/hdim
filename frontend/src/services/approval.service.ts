/**
 * Approval Service
 * Handles communication with the HITL Approval API
 */

import type {
  ApprovalRequest,
  ApprovalHistory,
  ApprovalStats,
  ApprovalDecision,
  ApprovalAssignment,
  ApprovalEscalation,
  ApprovalFilters,
  PageResponse,
} from '../types/approval';

const API_BASE_URL = import.meta.env.VITE_APPROVAL_API_URL || 'http://localhost:8097';

interface ApiOptions {
  tenantId: string;
  userId?: string;
}

class ApprovalService {
  private baseUrl: string;

  constructor(baseUrl: string = API_BASE_URL) {
    this.baseUrl = baseUrl;
  }

  private getHeaders(options: ApiOptions): HeadersInit {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      'X-Tenant-Id': options.tenantId,
    };
    if (options.userId) {
      headers['X-User-Id'] = options.userId;
    }
    return headers;
  }

  /**
   * Fetch pending approvals for current user
   */
  async getPendingApprovals(
    options: ApiOptions,
    page: number = 0,
    size: number = 20
  ): Promise<PageResponse<ApprovalRequest>> {
    const response = await fetch(
      `${this.baseUrl}/api/v1/approvals/pending?page=${page}&size=${size}`,
      {
        method: 'GET',
        headers: this.getHeaders(options),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch pending approvals: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Fetch approvals assigned to current user
   */
  async getMyApprovals(
    options: ApiOptions,
    page: number = 0,
    size: number = 20
  ): Promise<PageResponse<ApprovalRequest>> {
    const response = await fetch(
      `${this.baseUrl}/api/v1/approvals/my?page=${page}&size=${size}`,
      {
        method: 'GET',
        headers: this.getHeaders(options),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch my approvals: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Fetch all approvals with filters
   */
  async getApprovals(
    options: ApiOptions,
    filters: ApprovalFilters = {},
    page: number = 0,
    size: number = 20
  ): Promise<PageResponse<ApprovalRequest>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    if (filters.status) params.append('status', filters.status);
    if (filters.requestType) params.append('requestType', filters.requestType);
    if (filters.riskLevel) params.append('riskLevel', filters.riskLevel);
    if (filters.assignedTo) params.append('assignedTo', filters.assignedTo);
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);

    const response = await fetch(`${this.baseUrl}/api/v1/approvals?${params.toString()}`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch approvals: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get a single approval by ID
   */
  async getApproval(options: ApiOptions, approvalId: string): Promise<ApprovalRequest> {
    const response = await fetch(`${this.baseUrl}/api/v1/approvals/${approvalId}`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch approval: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Assign an approval request to a user
   */
  async assignApproval(
    options: ApiOptions,
    approvalId: string,
    assignment: ApprovalAssignment
  ): Promise<ApprovalRequest> {
    const response = await fetch(`${this.baseUrl}/api/v1/approvals/${approvalId}/assign`, {
      method: 'POST',
      headers: this.getHeaders(options),
      body: JSON.stringify(assignment),
    });

    if (!response.ok) {
      throw new Error(`Failed to assign approval: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Approve a request
   */
  async approve(
    options: ApiOptions,
    approvalId: string,
    decision: ApprovalDecision
  ): Promise<ApprovalRequest> {
    const response = await fetch(`${this.baseUrl}/api/v1/approvals/${approvalId}/approve`, {
      method: 'POST',
      headers: this.getHeaders(options),
      body: JSON.stringify(decision),
    });

    if (!response.ok) {
      throw new Error(`Failed to approve request: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Reject a request
   */
  async reject(
    options: ApiOptions,
    approvalId: string,
    decision: ApprovalDecision
  ): Promise<ApprovalRequest> {
    const response = await fetch(`${this.baseUrl}/api/v1/approvals/${approvalId}/reject`, {
      method: 'POST',
      headers: this.getHeaders(options),
      body: JSON.stringify(decision),
    });

    if (!response.ok) {
      throw new Error(`Failed to reject request: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Escalate a request
   */
  async escalate(
    options: ApiOptions,
    approvalId: string,
    escalation: ApprovalEscalation
  ): Promise<ApprovalRequest> {
    const response = await fetch(`${this.baseUrl}/api/v1/approvals/${approvalId}/escalate`, {
      method: 'POST',
      headers: this.getHeaders(options),
      body: JSON.stringify(escalation),
    });

    if (!response.ok) {
      throw new Error(`Failed to escalate request: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get approval history
   */
  async getApprovalHistory(
    options: ApiOptions,
    approvalId: string
  ): Promise<ApprovalHistory[]> {
    const response = await fetch(
      `${this.baseUrl}/api/v1/approvals/${approvalId}/history`,
      {
        method: 'GET',
        headers: this.getHeaders(options),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch approval history: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get approval statistics
   */
  async getStatistics(options: ApiOptions): Promise<ApprovalStats> {
    const response = await fetch(`${this.baseUrl}/api/v1/approvals/stats`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch approval statistics: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get approval statistics with period filter (for analytics)
   */
  async getStats(tenantId: string, days: number = 30): Promise<ApprovalStats> {
    const response = await fetch(`${this.baseUrl}/api/v1/approvals/stats?days=${days}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'X-Tenant-Id': tenantId,
      },
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch approval statistics: ${response.statusText}`);
    }

    return response.json();
  }
}

// Export singleton instance
export const approvalService = new ApprovalService();

// Export class for testing
export { ApprovalService };
