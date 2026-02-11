import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, map, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Lead, Investor, Customer, Activity, Bead, DashboardData, PipelineView, PendingAction, ApprovalDecision, ApprovalStats } from '../models';

/**
 * CX API Service
 *
 * Provides HTTP client for the CX FastAPI backend.
 */
@Injectable({
  providedIn: 'root',
})
export class CxApiService {
  private baseUrl =
    (window as any).__CX_API_URL || environment.cxApiUrl || 'http://localhost:8201';
  private wsUrl =
    (window as any).__CX_WS_URL || environment.cxWsUrl || 'ws://localhost:8201';

  // WebSocket connection
  private ws: WebSocket | null = null;
  private wsMessages$ = new Subject<any>();

  constructor(private http: HttpClient) {}

  // ============================================================================
  // DASHBOARD
  // ============================================================================

  getDashboard(): Observable<DashboardData> {
    return this.http.get<DashboardData>(`${this.baseUrl}/api/dashboard`);
  }

  getPipelineView(): Observable<PipelineView> {
    return this.http.get<PipelineView>(`${this.baseUrl}/api/dashboard/pipeline`);
  }

  getInvestorPipeline(): Observable<PipelineView> {
    return this.http.get<PipelineView>(`${this.baseUrl}/api/dashboard/investor-pipeline`);
  }

  getCustomerHealth(): Observable<any> {
    return this.http.get(`${this.baseUrl}/api/dashboard/customer-health`);
  }

  getAgentActivity(limit = 50): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/api/dashboard/agent-activity?limit=${limit}`);
  }

  // ============================================================================
  // LEADS
  // ============================================================================

  getLeads(params?: {
    status?: string;
    tier?: string;
    source?: string;
    search?: string;
    limit?: number;
    offset?: number;
  }): Observable<{ items: Lead[]; total: number }> {
    const queryParams = new URLSearchParams();
    if (params?.status) queryParams.set('status', params.status);
    if (params?.tier) queryParams.set('tier', params.tier);
    if (params?.source) queryParams.set('source', params.source);
    if (params?.search) queryParams.set('search', params.search);
    if (params?.limit) queryParams.set('limit', params.limit.toString());
    if (params?.offset) queryParams.set('offset', params.offset.toString());

    return this.http.get<{ items: Lead[]; total: number }>(
      `${this.baseUrl}/api/leads?${queryParams.toString()}`
    );
  }

  getLead(id: string): Observable<Lead> {
    return this.http.get<Lead>(`${this.baseUrl}/api/leads/${id}`);
  }

  createLead(lead: Partial<Lead>): Observable<Lead> {
    return this.http.post<Lead>(`${this.baseUrl}/api/leads`, lead);
  }

  updateLead(id: string, updates: Partial<Lead>): Observable<Lead> {
    return this.http.put<Lead>(`${this.baseUrl}/api/leads/${id}`, updates);
  }

  deleteLead(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/leads/${id}`);
  }

  advanceLeadStatus(id: string): Observable<Lead> {
    return this.http.post<Lead>(`${this.baseUrl}/api/leads/${id}/advance`, {});
  }

  markLeadWon(id: string, dealSize?: number): Observable<Lead> {
    return this.http.post<Lead>(`${this.baseUrl}/api/leads/${id}/won`, { deal_size: dealSize });
  }

  markLeadLost(id: string, reason?: string): Observable<Lead> {
    return this.http.post<Lead>(`${this.baseUrl}/api/leads/${id}/lost`, { reason });
  }

  getLeadStats(): Observable<any> {
    return this.http.get(`${this.baseUrl}/api/leads/stats`);
  }

  // ============================================================================
  // INVESTORS
  // ============================================================================

  getInvestors(params?: {
    status?: string;
    tier?: string;
    investor_type?: string;
    search?: string;
    limit?: number;
    offset?: number;
  }): Observable<{ items: Investor[]; total: number }> {
    const queryParams = new URLSearchParams();
    if (params?.status) queryParams.set('status', params.status);
    if (params?.tier) queryParams.set('tier', params.tier);
    if (params?.investor_type) queryParams.set('investor_type', params.investor_type);
    if (params?.search) queryParams.set('search', params.search);
    if (params?.limit) queryParams.set('limit', params.limit.toString());
    if (params?.offset) queryParams.set('offset', params.offset.toString());

    return this.http.get<{ items: Investor[]; total: number }>(
      `${this.baseUrl}/api/investors?${queryParams.toString()}`
    );
  }

  getInvestor(id: string): Observable<Investor> {
    return this.http.get<Investor>(`${this.baseUrl}/api/investors/${id}`);
  }

  createInvestor(investor: Partial<Investor>): Observable<Investor> {
    return this.http.post<Investor>(`${this.baseUrl}/api/investors`, investor);
  }

  updateInvestor(id: string, updates: Partial<Investor>): Observable<Investor> {
    return this.http.put<Investor>(`${this.baseUrl}/api/investors/${id}`, updates);
  }

  deleteInvestor(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/investors/${id}`);
  }

  advanceInvestorStatus(id: string): Observable<Investor> {
    return this.http.post<Investor>(`${this.baseUrl}/api/investors/${id}/advance`, {});
  }

  markInvestorCommitted(id: string, amount?: number): Observable<Investor> {
    return this.http.post<Investor>(`${this.baseUrl}/api/investors/${id}/committed`, { amount });
  }

  markInvestorPassed(id: string, reason?: string): Observable<Investor> {
    return this.http.post<Investor>(`${this.baseUrl}/api/investors/${id}/passed`, { reason });
  }

  getInvestorStats(): Observable<any> {
    return this.http.get(`${this.baseUrl}/api/investors/stats`);
  }

  // ============================================================================
  // CUSTOMERS
  // ============================================================================

  getCustomers(params?: {
    status?: string;
    organization_type?: string;
    search?: string;
    limit?: number;
    offset?: number;
  }): Observable<{ items: Customer[]; total: number }> {
    const queryParams = new URLSearchParams();
    if (params?.status) queryParams.set('status', params.status);
    if (params?.organization_type) queryParams.set('organization_type', params.organization_type);
    if (params?.search) queryParams.set('search', params.search);
    if (params?.limit) queryParams.set('limit', params.limit.toString());
    if (params?.offset) queryParams.set('offset', params.offset.toString());

    return this.http.get<{ items: Customer[]; total: number }>(
      `${this.baseUrl}/api/customers?${queryParams.toString()}`
    );
  }

  getCustomer(id: string): Observable<Customer> {
    return this.http.get<Customer>(`${this.baseUrl}/api/customers/${id}`);
  }

  createCustomer(customer: Partial<Customer>): Observable<Customer> {
    return this.http.post<Customer>(`${this.baseUrl}/api/customers`, customer);
  }

  updateCustomer(id: string, updates: Partial<Customer>): Observable<Customer> {
    return this.http.put<Customer>(`${this.baseUrl}/api/customers/${id}`, updates);
  }

  deleteCustomer(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/customers/${id}`);
  }

  getCustomerStats(): Observable<any> {
    return this.http.get(`${this.baseUrl}/api/customers/stats`);
  }

  // ============================================================================
  // ACTIVITIES
  // ============================================================================

  getActivities(params?: {
    activity_type?: string;
    lead_id?: string;
    investor_id?: string;
    customer_id?: string;
    limit?: number;
    offset?: number;
  }): Observable<{ items: Activity[]; total: number }> {
    const queryParams = new URLSearchParams();
    if (params?.activity_type) queryParams.set('activity_type', params.activity_type);
    if (params?.lead_id) queryParams.set('lead_id', params.lead_id);
    if (params?.investor_id) queryParams.set('investor_id', params.investor_id);
    if (params?.customer_id) queryParams.set('customer_id', params.customer_id);
    if (params?.limit) queryParams.set('limit', params.limit.toString());
    if (params?.offset) queryParams.set('offset', params.offset.toString());

    return this.http.get<{ items: Activity[]; total: number }>(
      `${this.baseUrl}/api/activities?${queryParams.toString()}`
    );
  }

  createActivity(activity: Partial<Activity>): Observable<Activity> {
    return this.http.post<Activity>(`${this.baseUrl}/api/activities`, activity);
  }

  getRecentActivities(limit = 20): Observable<Activity[]> {
    return this.http.get<Activity[]>(`${this.baseUrl}/api/activities/recent?limit=${limit}`);
  }

  // ============================================================================
  // BEADS
  // ============================================================================

  getBeads(params?: {
    status?: string;
    priority?: number;
    bead_type?: string;
    limit?: number;
    offset?: number;
  }): Observable<{ items: Bead[]; total: number }> {
    const queryParams = new URLSearchParams();
    if (params?.status) queryParams.set('status', params.status);
    if (params?.priority !== undefined) queryParams.set('priority', params.priority.toString());
    if (params?.bead_type) queryParams.set('bead_type', params.bead_type);
    if (params?.limit) queryParams.set('limit', params.limit.toString());
    if (params?.offset) queryParams.set('offset', params.offset.toString());

    return this.http.get<{ items: Bead[]; total: number }>(
      `${this.baseUrl}/api/beads?${queryParams.toString()}`
    );
  }

  getReadyBeads(): Observable<Bead[]> {
    return this.http.get<Bead[]>(`${this.baseUrl}/api/beads/ready`);
  }

  createBead(bead: Partial<Bead>): Observable<Bead> {
    return this.http.post<Bead>(`${this.baseUrl}/api/beads`, bead);
  }

  updateBead(id: string, updates: Partial<Bead>): Observable<Bead> {
    return this.http.put<Bead>(`${this.baseUrl}/api/beads/${id}`, updates);
  }

  startBead(id: string, agentId?: string): Observable<Bead> {
    return this.http.post<Bead>(`${this.baseUrl}/api/beads/${id}/start`, { agent_id: agentId });
  }

  completeBead(id: string, notes?: string): Observable<Bead> {
    return this.http.post<Bead>(`${this.baseUrl}/api/beads/${id}/complete`, { notes });
  }

  getBeadStats(): Observable<any> {
    return this.http.get(`${this.baseUrl}/api/beads/stats`);
  }

  // ============================================================================
  // WEBSOCKET
  // ============================================================================

  connectWebSocket(channel: string = ''): Observable<any> {
    const url = channel ? `${this.wsUrl}/ws/${channel}` : `${this.wsUrl}/ws`;

    if (this.ws) {
      this.ws.close();
    }

    this.ws = new WebSocket(url);

    this.ws.onopen = () => {
      console.log('[CX WS] Connected to', url);
    };

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        this.wsMessages$.next(data);
      } catch {
        this.wsMessages$.next({ type: 'raw', data: event.data });
      }
    };

    this.ws.onerror = (error) => {
      console.error('[CX WS] Error:', error);
    };

    this.ws.onclose = () => {
      console.log('[CX WS] Disconnected');
    };

    return this.wsMessages$.asObservable();
  }

  disconnectWebSocket(): void {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }

  sendWebSocketMessage(message: any): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    }
  }

  // ============================================================================
  // APPROVAL WORKFLOW
  // ============================================================================

  /**
   * Get pending approval actions with optional filters
   */
  getPendingApprovals(filters?: {
    action_type?: string;
    urgency?: string;
    target_type?: string;
    limit?: number;
    offset?: number;
  }): Observable<PendingAction[]> {
    const queryParams = new URLSearchParams();
    if (filters?.action_type) queryParams.set('action_type', filters.action_type);
    if (filters?.urgency) queryParams.set('urgency', filters.urgency);
    if (filters?.target_type) queryParams.set('target_type', filters.target_type);
    if (filters?.limit) queryParams.set('limit', filters.limit.toString());
    if (filters?.offset) queryParams.set('offset', filters.offset.toString());

    return this.http.get<PendingAction[]>(
      `${this.baseUrl}/api/approvals/pending?${queryParams.toString()}`
    );
  }

  /**
   * Get specific pending action by ID
   */
  getPendingApproval(id: string): Observable<PendingAction> {
    return this.http.get<PendingAction>(`${this.baseUrl}/api/approvals/pending/${id}`);
  }

  /**
   * Submit approval decision (approve/edit/reject)
   */
  submitApprovalDecision(decision: ApprovalDecision): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/api/approvals/decision`, decision);
  }

  /**
   * Batch approve multiple actions at once
   */
  batchApprove(actionIds: string[], notes?: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/api/approvals/batch`, {
      action_ids: actionIds,
      decision_notes: notes,
    });
  }

  /**
   * Get approval history with optional filters
   */
  getApprovalHistory(filters?: {
    status?: string;
    limit?: number;
    offset?: number;
  }): Observable<PendingAction[]> {
    const queryParams = new URLSearchParams();
    if (filters?.status) queryParams.set('status', filters.status);
    if (filters?.limit) queryParams.set('limit', filters.limit.toString());
    if (filters?.offset) queryParams.set('offset', filters.offset.toString());

    return this.http.get<PendingAction[]>(
      `${this.baseUrl}/api/approvals/history?${queryParams.toString()}`
    );
  }

  /**
   * Get approval system statistics
   */
  getApprovalStats(): Observable<ApprovalStats> {
    return this.http.get<ApprovalStats>(`${this.baseUrl}/api/approvals/stats`);
  }

  // Legacy method for backward compatibility
  getPendingActions(): Observable<PendingAction[]> {
    return this.getPendingApprovals();
  }

  // ============================================================================
  // CAMPAIGNS
  // ============================================================================

  /**
   * List campaigns with optional filtering
   */
  getCampaigns(params?: {
    status?: string;
    campaign_type?: string;
    search?: string;
    limit?: number;
    offset?: number;
  }): Observable<any[]> {
    const queryParams = new URLSearchParams();
    if (params?.status) queryParams.set('status', params.status);
    if (params?.campaign_type) queryParams.set('campaign_type', params.campaign_type);
    if (params?.search) queryParams.set('search', params.search);
    if (params?.limit) queryParams.set('limit', params.limit.toString());
    if (params?.offset) queryParams.set('offset', params.offset.toString());

    return this.http.get<any[]>(
      `${this.baseUrl}/api/campaigns?${queryParams.toString()}`
    );
  }

  /**
   * Get campaign statistics
   */
  getCampaignStats(): Observable<any> {
    return this.http.get(`${this.baseUrl}/api/campaigns/stats`);
  }

  /**
   * Get single campaign details
   */
  getCampaign(id: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/api/campaigns/${id}`);
  }

  /**
   * Create a new campaign
   */
  createCampaign(campaign: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/campaigns`, campaign);
  }

  /**
   * Update campaign details
   */
  updateCampaign(id: string, updates: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/api/campaigns/${id}`, updates);
  }

  /**
   * Delete campaign (draft only)
   */
  deleteCampaign(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/campaigns/${id}`);
  }

  /**
   * Start campaign execution
   */
  startCampaign(id: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/campaigns/${id}/start`, {});
  }

  /**
   * Pause active campaign
   */
  pauseCampaign(id: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/campaigns/${id}/pause`, {});
  }

  /**
   * Resume paused campaign
   */
  resumeCampaign(id: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/campaigns/${id}/resume`, {});
  }

  /**
   * Mark campaign as completed
   */
  completeCampaign(id: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/campaigns/${id}/complete`, {});
  }

  /**
   * Archive campaign
   */
  archiveCampaign(id: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/campaigns/${id}/archive`, {});
  }

  /**
   * Get campaign beads
   */
  getCampaignBeads(id: string): Observable<Bead[]> {
    return this.http.get<Bead[]>(`${this.baseUrl}/api/campaigns/${id}/beads`);
  }

  /**
   * Create bead for campaign
   */
  createCampaignBead(id: string, bead: Partial<Bead>): Observable<Bead> {
    return this.http.post<Bead>(`${this.baseUrl}/api/campaigns/${id}/beads`, bead);
  }

  /**
   * Get campaign metrics (optionally refresh)
   */
  getCampaignMetrics(id: string, refresh = false): Observable<any> {
    return this.http.get(`${this.baseUrl}/api/campaigns/${id}/metrics?refresh=${refresh}`);
  }

  /**
   * List campaign templates
   */
  getCampaignTemplates(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/api/campaigns/templates`);
  }

  /**
   * Create campaign template
   */
  createCampaignTemplate(template: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/campaigns/templates`, template);
  }

  /**
   * Clone existing campaign
   */
  cloneCampaign(id: string, newName: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/campaigns/${id}/clone`, { new_name: newName });
  }
}
