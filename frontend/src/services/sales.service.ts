/**
 * Sales Automation Service
 * Handles communication with the Sales Automation API
 */

import type {
  Lead,
  Contact,
  Account,
  Opportunity,
  Activity,
  EmailSequence,
  SequenceEnrollment,
  SequenceAnalytics,
  PipelineKanbanDTO,
  PipelineForecastDTO,
  SalesDashboardDTO,
  StageTransitionRequest,
  LeadCaptureRequest,
  LeadCaptureResponse,
  PageResponse,
  SalesFilters,
} from '../types/sales';

const API_BASE_URL = import.meta.env.VITE_SALES_API_URL || 'http://localhost:8106';

interface ApiOptions {
  tenantId: string;
  userId?: string;
}

class SalesService {
  private baseUrl: string;

  constructor(baseUrl: string = API_BASE_URL) {
    this.baseUrl = baseUrl;
  }

  private getHeaders(options: ApiOptions): HeadersInit {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      'X-Tenant-ID': options.tenantId,
    };
    if (options.userId) {
      headers['X-User-ID'] = options.userId;
    }
    return headers;
  }

  // ==================== Leads ====================

  async getLeads(
    options: ApiOptions,
    filters: SalesFilters = {},
    page: number = 0,
    size: number = 20
  ): Promise<PageResponse<Lead>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    if (filters.status) params.append('status', filters.status);
    if (filters.source) params.append('source', filters.source);
    if (filters.search) params.append('search', filters.search);

    const response = await fetch(`${this.baseUrl}/api/sales/leads?${params.toString()}`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch leads: ${response.statusText}`);
    }

    return response.json();
  }

  async getLead(options: ApiOptions, leadId: string): Promise<Lead> {
    const response = await fetch(`${this.baseUrl}/api/sales/leads/${leadId}`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch lead: ${response.statusText}`);
    }

    return response.json();
  }

  async createLead(options: ApiOptions, lead: Partial<Lead>): Promise<Lead> {
    const response = await fetch(`${this.baseUrl}/api/sales/leads`, {
      method: 'POST',
      headers: this.getHeaders(options),
      body: JSON.stringify(lead),
    });

    if (!response.ok) {
      throw new Error(`Failed to create lead: ${response.statusText}`);
    }

    return response.json();
  }

  async updateLead(options: ApiOptions, leadId: string, lead: Partial<Lead>): Promise<Lead> {
    const response = await fetch(`${this.baseUrl}/api/sales/leads/${leadId}`, {
      method: 'PUT',
      headers: this.getHeaders(options),
      body: JSON.stringify(lead),
    });

    if (!response.ok) {
      throw new Error(`Failed to update lead: ${response.statusText}`);
    }

    return response.json();
  }

  async deleteLead(options: ApiOptions, leadId: string): Promise<void> {
    const response = await fetch(`${this.baseUrl}/api/sales/leads/${leadId}`, {
      method: 'DELETE',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to delete lead: ${response.statusText}`);
    }
  }

  async convertLead(options: ApiOptions, leadId: string): Promise<Opportunity> {
    const response = await fetch(`${this.baseUrl}/api/sales/leads/${leadId}/convert`, {
      method: 'POST',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to convert lead: ${response.statusText}`);
    }

    return response.json();
  }

  async captureLead(request: LeadCaptureRequest): Promise<LeadCaptureResponse> {
    const response = await fetch(`${this.baseUrl}/api/sales/public/capture`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      throw new Error(`Failed to capture lead: ${response.statusText}`);
    }

    return response.json();
  }

  // ==================== Contacts ====================

  async getContacts(
    options: ApiOptions,
    filters: SalesFilters = {},
    page: number = 0,
    size: number = 20
  ): Promise<PageResponse<Contact>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    if (filters.search) params.append('search', filters.search);

    const response = await fetch(`${this.baseUrl}/api/sales/contacts?${params.toString()}`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch contacts: ${response.statusText}`);
    }

    return response.json();
  }

  async getContact(options: ApiOptions, contactId: string): Promise<Contact> {
    const response = await fetch(`${this.baseUrl}/api/sales/contacts/${contactId}`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch contact: ${response.statusText}`);
    }

    return response.json();
  }

  async createContact(options: ApiOptions, contact: Partial<Contact>): Promise<Contact> {
    const response = await fetch(`${this.baseUrl}/api/sales/contacts`, {
      method: 'POST',
      headers: this.getHeaders(options),
      body: JSON.stringify(contact),
    });

    if (!response.ok) {
      throw new Error(`Failed to create contact: ${response.statusText}`);
    }

    return response.json();
  }

  async updateContact(options: ApiOptions, contactId: string, contact: Partial<Contact>): Promise<Contact> {
    const response = await fetch(`${this.baseUrl}/api/sales/contacts/${contactId}`, {
      method: 'PUT',
      headers: this.getHeaders(options),
      body: JSON.stringify(contact),
    });

    if (!response.ok) {
      throw new Error(`Failed to update contact: ${response.statusText}`);
    }

    return response.json();
  }

  // ==================== Accounts ====================

  async getAccounts(
    options: ApiOptions,
    filters: SalesFilters = {},
    page: number = 0,
    size: number = 20
  ): Promise<PageResponse<Account>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    if (filters.stage) params.append('stage', filters.stage);
    if (filters.search) params.append('search', filters.search);

    const response = await fetch(`${this.baseUrl}/api/sales/accounts?${params.toString()}`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch accounts: ${response.statusText}`);
    }

    return response.json();
  }

  async getAccount(options: ApiOptions, accountId: string): Promise<Account> {
    const response = await fetch(`${this.baseUrl}/api/sales/accounts/${accountId}`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch account: ${response.statusText}`);
    }

    return response.json();
  }

  async createAccount(options: ApiOptions, account: Partial<Account>): Promise<Account> {
    const response = await fetch(`${this.baseUrl}/api/sales/accounts`, {
      method: 'POST',
      headers: this.getHeaders(options),
      body: JSON.stringify(account),
    });

    if (!response.ok) {
      throw new Error(`Failed to create account: ${response.statusText}`);
    }

    return response.json();
  }

  async updateAccount(options: ApiOptions, accountId: string, account: Partial<Account>): Promise<Account> {
    const response = await fetch(`${this.baseUrl}/api/sales/accounts/${accountId}`, {
      method: 'PUT',
      headers: this.getHeaders(options),
      body: JSON.stringify(account),
    });

    if (!response.ok) {
      throw new Error(`Failed to update account: ${response.statusText}`);
    }

    return response.json();
  }

  // ==================== Opportunities ====================

  async getOpportunities(
    options: ApiOptions,
    filters: SalesFilters = {},
    page: number = 0,
    size: number = 20
  ): Promise<PageResponse<Opportunity>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    if (filters.stage) params.append('stage', filters.stage);
    if (filters.search) params.append('search', filters.search);

    const response = await fetch(`${this.baseUrl}/api/sales/opportunities?${params.toString()}`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch opportunities: ${response.statusText}`);
    }

    return response.json();
  }

  async getOpportunity(options: ApiOptions, opportunityId: string): Promise<Opportunity> {
    const response = await fetch(`${this.baseUrl}/api/sales/opportunities/${opportunityId}`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch opportunity: ${response.statusText}`);
    }

    return response.json();
  }

  async createOpportunity(options: ApiOptions, opportunity: Partial<Opportunity>): Promise<Opportunity> {
    const response = await fetch(`${this.baseUrl}/api/sales/opportunities`, {
      method: 'POST',
      headers: this.getHeaders(options),
      body: JSON.stringify(opportunity),
    });

    if (!response.ok) {
      throw new Error(`Failed to create opportunity: ${response.statusText}`);
    }

    return response.json();
  }

  async updateOpportunity(options: ApiOptions, opportunityId: string, opportunity: Partial<Opportunity>): Promise<Opportunity> {
    const response = await fetch(`${this.baseUrl}/api/sales/opportunities/${opportunityId}`, {
      method: 'PUT',
      headers: this.getHeaders(options),
      body: JSON.stringify(opportunity),
    });

    if (!response.ok) {
      throw new Error(`Failed to update opportunity: ${response.statusText}`);
    }

    return response.json();
  }

  async moveOpportunityStage(
    options: ApiOptions,
    opportunityId: string,
    request: StageTransitionRequest
  ): Promise<Opportunity> {
    const response = await fetch(`${this.baseUrl}/api/sales/pipeline/opportunities/${opportunityId}/move`, {
      method: 'POST',
      headers: this.getHeaders(options),
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      throw new Error(`Failed to move opportunity: ${response.statusText}`);
    }

    return response.json();
  }

  // ==================== Activities ====================

  async getActivities(
    options: ApiOptions,
    filters: { leadId?: string; contactId?: string; opportunityId?: string } = {},
    page: number = 0,
    size: number = 20
  ): Promise<PageResponse<Activity>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    if (filters.leadId) params.append('leadId', filters.leadId);
    if (filters.contactId) params.append('contactId', filters.contactId);
    if (filters.opportunityId) params.append('opportunityId', filters.opportunityId);

    const response = await fetch(`${this.baseUrl}/api/sales/activities?${params.toString()}`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch activities: ${response.statusText}`);
    }

    return response.json();
  }

  async createActivity(options: ApiOptions, activity: Partial<Activity>): Promise<Activity> {
    const response = await fetch(`${this.baseUrl}/api/sales/activities`, {
      method: 'POST',
      headers: this.getHeaders(options),
      body: JSON.stringify(activity),
    });

    if (!response.ok) {
      throw new Error(`Failed to create activity: ${response.statusText}`);
    }

    return response.json();
  }

  async completeActivity(options: ApiOptions, activityId: string): Promise<Activity> {
    const response = await fetch(`${this.baseUrl}/api/sales/activities/${activityId}/complete`, {
      method: 'PUT',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to complete activity: ${response.statusText}`);
    }

    return response.json();
  }

  // ==================== Pipeline ====================

  async getPipelineKanban(options: ApiOptions): Promise<PipelineKanbanDTO> {
    const response = await fetch(`${this.baseUrl}/api/sales/pipeline/kanban`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch pipeline: ${response.statusText}`);
    }

    return response.json();
  }

  async getPipelineForecast(options: ApiOptions): Promise<PipelineForecastDTO> {
    const response = await fetch(`${this.baseUrl}/api/sales/pipeline/forecast`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch forecast: ${response.statusText}`);
    }

    return response.json();
  }

  async getAtRiskDeals(options: ApiOptions): Promise<Opportunity[]> {
    const response = await fetch(`${this.baseUrl}/api/sales/pipeline/at-risk`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch at-risk deals: ${response.statusText}`);
    }

    return response.json();
  }

  // ==================== Dashboard ====================

  async getDashboard(options: ApiOptions): Promise<SalesDashboardDTO> {
    const response = await fetch(`${this.baseUrl}/api/sales/dashboard`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch dashboard: ${response.statusText}`);
    }

    return response.json();
  }

  // ==================== Email Sequences ====================

  async getSequences(
    options: ApiOptions,
    page: number = 0,
    size: number = 20
  ): Promise<PageResponse<EmailSequence>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    const response = await fetch(`${this.baseUrl}/api/sales/sequences?${params.toString()}`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch sequences: ${response.statusText}`);
    }

    return response.json();
  }

  async getSequence(options: ApiOptions, sequenceId: string): Promise<EmailSequence> {
    const response = await fetch(`${this.baseUrl}/api/sales/sequences/${sequenceId}`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch sequence: ${response.statusText}`);
    }

    return response.json();
  }

  async createSequence(options: ApiOptions, sequence: Partial<EmailSequence>): Promise<EmailSequence> {
    const response = await fetch(`${this.baseUrl}/api/sales/sequences`, {
      method: 'POST',
      headers: this.getHeaders(options),
      body: JSON.stringify(sequence),
    });

    if (!response.ok) {
      throw new Error(`Failed to create sequence: ${response.statusText}`);
    }

    return response.json();
  }

  async updateSequence(options: ApiOptions, sequenceId: string, sequence: Partial<EmailSequence>): Promise<EmailSequence> {
    const response = await fetch(`${this.baseUrl}/api/sales/sequences/${sequenceId}`, {
      method: 'PUT',
      headers: this.getHeaders(options),
      body: JSON.stringify(sequence),
    });

    if (!response.ok) {
      throw new Error(`Failed to update sequence: ${response.statusText}`);
    }

    return response.json();
  }

  async activateSequence(options: ApiOptions, sequenceId: string): Promise<EmailSequence> {
    const response = await fetch(`${this.baseUrl}/api/sales/sequences/${sequenceId}/activate`, {
      method: 'POST',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to activate sequence: ${response.statusText}`);
    }

    return response.json();
  }

  async getSequenceAnalytics(options: ApiOptions, sequenceId: string): Promise<SequenceAnalytics> {
    const response = await fetch(`${this.baseUrl}/api/sales/sequences/${sequenceId}/analytics`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch analytics: ${response.statusText}`);
    }

    return response.json();
  }

  async enrollLeadInSequence(
    options: ApiOptions,
    sequenceId: string,
    leadId: string
  ): Promise<SequenceEnrollment> {
    const response = await fetch(`${this.baseUrl}/api/sales/sequences/${sequenceId}/enroll/lead/${leadId}`, {
      method: 'POST',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to enroll lead: ${response.statusText}`);
    }

    return response.json();
  }

  async enrollContactInSequence(
    options: ApiOptions,
    sequenceId: string,
    contactId: string
  ): Promise<SequenceEnrollment> {
    const response = await fetch(`${this.baseUrl}/api/sales/sequences/${sequenceId}/enroll/contact/${contactId}`, {
      method: 'POST',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to enroll contact: ${response.statusText}`);
    }

    return response.json();
  }

  async getLeadEnrollments(options: ApiOptions, leadId: string): Promise<SequenceEnrollment[]> {
    const response = await fetch(`${this.baseUrl}/api/sales/sequences/leads/${leadId}/enrollments`, {
      method: 'GET',
      headers: this.getHeaders(options),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch enrollments: ${response.statusText}`);
    }

    return response.json();
  }
}

// Export singleton instance
export const salesService = new SalesService();

// Export class for testing
export { SalesService };
