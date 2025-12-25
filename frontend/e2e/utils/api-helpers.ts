import { APIRequestContext } from '@playwright/test';
import { TEST_TENANT_ID } from './test-data';

const SALES_API_URL = process.env.SALES_API_URL || 'http://localhost:8106/sales-automation';

/**
 * API client for test data setup and teardown
 */
export class SalesApiClient {
  constructor(
    private request: APIRequestContext,
    private tenantId: string = TEST_TENANT_ID
  ) {}

  private getHeaders() {
    return {
      'Content-Type': 'application/json',
      'X-Tenant-ID': this.tenantId,
    };
  }

  // Lead operations
  async createLead(data: Record<string, unknown>) {
    const response = await this.request.post(`${SALES_API_URL}/api/sales/leads`, {
      headers: this.getHeaders(),
      data,
    });
    if (!response.ok()) {
      throw new Error(`Failed to create lead: ${response.status()}`);
    }
    return response.json();
  }

  async getLead(id: string) {
    const response = await this.request.get(`${SALES_API_URL}/api/sales/leads/${id}`, {
      headers: this.getHeaders(),
    });
    return response.json();
  }

  async getLeads(params?: Record<string, string>) {
    const queryString = params ? '?' + new URLSearchParams(params).toString() : '';
    const response = await this.request.get(`${SALES_API_URL}/api/sales/leads${queryString}`, {
      headers: this.getHeaders(),
    });
    return response.json();
  }

  async deleteLead(id: string) {
    await this.request.delete(`${SALES_API_URL}/api/sales/leads/${id}`, {
      headers: this.getHeaders(),
    });
  }

  async convertLead(id: string) {
    const response = await this.request.post(`${SALES_API_URL}/api/sales/leads/${id}/convert`, {
      headers: this.getHeaders(),
      data: {},
    });
    return response.json();
  }

  // Account operations
  async createAccount(data: Record<string, unknown>) {
    const response = await this.request.post(`${SALES_API_URL}/api/sales/accounts`, {
      headers: this.getHeaders(),
      data,
    });
    if (!response.ok()) {
      throw new Error(`Failed to create account: ${response.status()}`);
    }
    return response.json();
  }

  async getAccount(id: string) {
    const response = await this.request.get(`${SALES_API_URL}/api/sales/accounts/${id}`, {
      headers: this.getHeaders(),
    });
    return response.json();
  }

  async getAccounts(params?: Record<string, string>) {
    const queryString = params ? '?' + new URLSearchParams(params).toString() : '';
    const response = await this.request.get(`${SALES_API_URL}/api/sales/accounts${queryString}`, {
      headers: this.getHeaders(),
    });
    return response.json();
  }

  async deleteAccount(id: string) {
    await this.request.delete(`${SALES_API_URL}/api/sales/accounts/${id}`, {
      headers: this.getHeaders(),
    });
  }

  // Opportunity operations
  async createOpportunity(data: Record<string, unknown>) {
    const response = await this.request.post(`${SALES_API_URL}/api/sales/opportunities`, {
      headers: this.getHeaders(),
      data,
    });
    if (!response.ok()) {
      throw new Error(`Failed to create opportunity: ${response.status()}`);
    }
    return response.json();
  }

  async getOpportunity(id: string) {
    const response = await this.request.get(`${SALES_API_URL}/api/sales/opportunities/${id}`, {
      headers: this.getHeaders(),
    });
    return response.json();
  }

  async moveOpportunityStage(id: string, stage: string, lostReason?: string) {
    const response = await this.request.post(`${SALES_API_URL}/api/sales/pipeline/opportunities/${id}/move`, {
      headers: this.getHeaders(),
      data: { newStage: stage, lostReason },
    });
    return response.json();
  }

  async deleteOpportunity(id: string) {
    await this.request.delete(`${SALES_API_URL}/api/sales/opportunities/${id}`, {
      headers: this.getHeaders(),
    });
  }

  // Pipeline operations
  async getPipelineKanban() {
    const response = await this.request.get(`${SALES_API_URL}/api/sales/pipeline/kanban`, {
      headers: this.getHeaders(),
    });
    return response.json();
  }

  async getDashboard() {
    const response = await this.request.get(`${SALES_API_URL}/api/sales/dashboard`, {
      headers: this.getHeaders(),
    });
    return response.json();
  }

  // Sequence operations
  async createSequence(data: Record<string, unknown>) {
    const response = await this.request.post(`${SALES_API_URL}/api/sales/sequences`, {
      headers: this.getHeaders(),
      data,
    });
    if (!response.ok()) {
      throw new Error(`Failed to create sequence: ${response.status()}`);
    }
    return response.json();
  }

  async getSequences() {
    const response = await this.request.get(`${SALES_API_URL}/api/sales/sequences`, {
      headers: this.getHeaders(),
    });
    return response.json();
  }

  async activateSequence(id: string) {
    const response = await this.request.post(`${SALES_API_URL}/api/sales/sequences/${id}/activate`, {
      headers: this.getHeaders(),
    });
    return response.json();
  }

  async deactivateSequence(id: string) {
    const response = await this.request.post(`${SALES_API_URL}/api/sales/sequences/${id}/deactivate`, {
      headers: this.getHeaders(),
    });
    return response.json();
  }

  async deleteSequence(id: string) {
    await this.request.delete(`${SALES_API_URL}/api/sales/sequences/${id}`, {
      headers: this.getHeaders(),
    });
  }

  // Cleanup helper
  async cleanupTestData() {
    try {
      // Clean up leads
      const leads = await this.getLeads();
      for (const lead of leads.content || []) {
        if (lead.email?.includes('e2etest.com')) {
          await this.deleteLead(lead.id);
        }
      }

      // Clean up opportunities
      const opps = await this.request.get(`${SALES_API_URL}/api/sales/opportunities`, {
        headers: this.getHeaders(),
      });
      const oppsData = await opps.json();
      for (const opp of oppsData.content || []) {
        if (opp.name?.startsWith('E2E Test')) {
          await this.deleteOpportunity(opp.id);
        }
      }

      // Clean up accounts
      const accounts = await this.getAccounts();
      for (const account of accounts.content || []) {
        if (account.name?.startsWith('E2E Test') || account.name?.startsWith('Test')) {
          await this.deleteAccount(account.id);
        }
      }

      // Clean up sequences
      const sequences = await this.getSequences();
      for (const seq of sequences.content || []) {
        if (seq.name?.startsWith('E2E Test')) {
          await this.deleteSequence(seq.id);
        }
      }
    } catch (error) {
      console.error('Error during cleanup:', error);
    }
  }
}
