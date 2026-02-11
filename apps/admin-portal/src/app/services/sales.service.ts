import { Injectable, signal, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, tap, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  SalesDashboard,
  Lead,
  LeadCreateRequest,
  LeadFilter,
  LeadConversionResult,
  Account,
  AccountCreateRequest,
  SalesContact,
  SalesContactCreateRequest,
  Opportunity,
  OpportunityCreateRequest,
  OpportunityFilter,
  OpportunityStageUpdate,
  Activity,
  ActivityCreateRequest,
  ActivityLogRequest,
  ActivityFilter,
  PipelineKanban,
  PipelineMetrics,
  PipelineForecast,
  PageRequest,
  PageResponse,
} from '../models/sales.model';

/**
 * Main Sales Automation Service
 *
 * Provides dashboard data and coordinates between entity-specific services.
 * Uses Angular signals for reactive state management.
 */
@Injectable({
  providedIn: 'root',
})
export class SalesService {
  private readonly http = inject(HttpClient);

  // API base URL - points to sales-automation-service backend
  private readonly apiBaseUrl = environment.apiConfig.salesApiUrl;

  // State signals
  private _dashboard = signal<SalesDashboard | null>(null);
  private _isLoading = signal<boolean>(false);
  private _error = signal<string | null>(null);

  // Public read-only signals
  readonly dashboard = this._dashboard.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();
  readonly error = this._error.asReadonly();

  // ==========================================
  // Dashboard
  // ==========================================

  loadDashboard(): Observable<SalesDashboard> {
    this._isLoading.set(true);
    this._error.set(null);

    return this.http.get<SalesDashboard>(`${this.apiBaseUrl}/api/sales/dashboard`).pipe(
      tap((dashboard) => {
        this._dashboard.set(dashboard);
        this._isLoading.set(false);
      }),
      catchError((error) => {
        this._isLoading.set(false);
        this._error.set('Failed to load dashboard');
        return throwError(() => error);
      })
    );
  }

  getDashboardLeads(): Observable<Lead[]> {
    return this.http.get<Lead[]>(`${this.apiBaseUrl}/api/sales/dashboard/leads`).pipe(
      catchError(() => of([]))
    );
  }

  getDashboardActivities(): Observable<Activity[]> {
    return this.http.get<Activity[]>(`${this.apiBaseUrl}/api/sales/dashboard/activities`).pipe(
      catchError(() => of([]))
    );
  }

  // ==========================================
  // Lead Management
  // ==========================================

  getLeads(filter?: LeadFilter, page?: PageRequest): Observable<PageResponse<Lead>> {
    let params = this.buildPageParams(page);
    if (filter) {
      params = this.addFilterParams(params, filter);
    }

    return this.http.get<PageResponse<Lead>>(`${this.apiBaseUrl}/api/sales/leads`, { params }).pipe(
      catchError((error) => {
        this._error.set('Failed to load leads');
        return throwError(() => error);
      })
    );
  }

  getLead(id: string): Observable<Lead> {
    return this.http.get<Lead>(`${this.apiBaseUrl}/api/sales/leads/${id}`).pipe(
      catchError((error) => {
        this._error.set('Failed to load lead');
        return throwError(() => error);
      })
    );
  }

  createLead(lead: LeadCreateRequest): Observable<Lead> {
    return this.http.post<Lead>(`${this.apiBaseUrl}/api/sales/leads`, lead).pipe(
      catchError((error) => {
        this._error.set('Failed to create lead');
        return throwError(() => error);
      })
    );
  }

  updateLead(id: string, lead: Partial<LeadCreateRequest>): Observable<Lead> {
    return this.http.put<Lead>(`${this.apiBaseUrl}/api/sales/leads/${id}`, lead).pipe(
      catchError((error) => {
        this._error.set('Failed to update lead');
        return throwError(() => error);
      })
    );
  }

  deleteLead(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/api/sales/leads/${id}`).pipe(
      catchError((error) => {
        this._error.set('Failed to delete lead');
        return throwError(() => error);
      })
    );
  }

  convertLead(id: string, createOpportunity: boolean = true): Observable<LeadConversionResult> {
    return this.http.post<LeadConversionResult>(
      `${this.apiBaseUrl}/api/sales/leads/${id}/convert`,
      { createOpportunity }
    ).pipe(
      catchError((error) => {
        this._error.set('Failed to convert lead');
        return throwError(() => error);
      })
    );
  }

  getHighScoreLeads(minScore: number = 70): Observable<Lead[]> {
    return this.http.get<Lead[]>(`${this.apiBaseUrl}/api/sales/leads/high-score`, {
      params: { minScore: minScore.toString() }
    }).pipe(
      catchError(() => of([]))
    );
  }

  // ==========================================
  // Account Management
  // ==========================================

  getAccounts(page?: PageRequest): Observable<PageResponse<Account>> {
    const params = this.buildPageParams(page);
    return this.http.get<PageResponse<Account>>(`${this.apiBaseUrl}/api/sales/accounts`, { params }).pipe(
      catchError((error) => {
        this._error.set('Failed to load accounts');
        return throwError(() => error);
      })
    );
  }

  getAccount(id: string): Observable<Account> {
    return this.http.get<Account>(`${this.apiBaseUrl}/api/sales/accounts/${id}`).pipe(
      catchError((error) => {
        this._error.set('Failed to load account');
        return throwError(() => error);
      })
    );
  }

  createAccount(account: AccountCreateRequest): Observable<Account> {
    return this.http.post<Account>(`${this.apiBaseUrl}/api/sales/accounts`, account).pipe(
      catchError((error) => {
        this._error.set('Failed to create account');
        return throwError(() => error);
      })
    );
  }

  updateAccount(id: string, account: Partial<AccountCreateRequest>): Observable<Account> {
    return this.http.put<Account>(`${this.apiBaseUrl}/api/sales/accounts/${id}`, account).pipe(
      catchError((error) => {
        this._error.set('Failed to update account');
        return throwError(() => error);
      })
    );
  }

  deleteAccount(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/api/sales/accounts/${id}`).pipe(
      catchError((error) => {
        this._error.set('Failed to delete account');
        return throwError(() => error);
      })
    );
  }

  // ==========================================
  // Contact Management
  // ==========================================

  getContacts(accountId?: string, page?: PageRequest): Observable<PageResponse<SalesContact>> {
    let params = this.buildPageParams(page);
    if (accountId) {
      params = params.set('accountId', accountId);
    }
    return this.http.get<PageResponse<SalesContact>>(`${this.apiBaseUrl}/api/sales/contacts`, { params }).pipe(
      catchError((error) => {
        this._error.set('Failed to load contacts');
        return throwError(() => error);
      })
    );
  }

  getContact(id: string): Observable<SalesContact> {
    return this.http.get<SalesContact>(`${this.apiBaseUrl}/api/sales/contacts/${id}`).pipe(
      catchError((error) => {
        this._error.set('Failed to load contact');
        return throwError(() => error);
      })
    );
  }

  createContact(contact: SalesContactCreateRequest): Observable<SalesContact> {
    return this.http.post<SalesContact>(`${this.apiBaseUrl}/api/sales/contacts`, contact).pipe(
      catchError((error) => {
        this._error.set('Failed to create contact');
        return throwError(() => error);
      })
    );
  }

  updateContact(id: string, contact: Partial<SalesContactCreateRequest>): Observable<SalesContact> {
    return this.http.put<SalesContact>(`${this.apiBaseUrl}/api/sales/contacts/${id}`, contact).pipe(
      catchError((error) => {
        this._error.set('Failed to update contact');
        return throwError(() => error);
      })
    );
  }

  deleteContact(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/api/sales/contacts/${id}`).pipe(
      catchError((error) => {
        this._error.set('Failed to delete contact');
        return throwError(() => error);
      })
    );
  }

  // ==========================================
  // Opportunity Management
  // ==========================================

  getOpportunities(filter?: OpportunityFilter, page?: PageRequest): Observable<PageResponse<Opportunity>> {
    let params = this.buildPageParams(page);
    if (filter) {
      params = this.addFilterParams(params, filter);
    }
    return this.http.get<PageResponse<Opportunity>>(`${this.apiBaseUrl}/api/sales/opportunities`, { params }).pipe(
      catchError((error) => {
        this._error.set('Failed to load opportunities');
        return throwError(() => error);
      })
    );
  }

  getOpportunity(id: string): Observable<Opportunity> {
    return this.http.get<Opportunity>(`${this.apiBaseUrl}/api/sales/opportunities/${id}`).pipe(
      catchError((error) => {
        this._error.set('Failed to load opportunity');
        return throwError(() => error);
      })
    );
  }

  createOpportunity(opportunity: OpportunityCreateRequest): Observable<Opportunity> {
    return this.http.post<Opportunity>(`${this.apiBaseUrl}/api/sales/opportunities`, opportunity).pipe(
      catchError((error) => {
        this._error.set('Failed to create opportunity');
        return throwError(() => error);
      })
    );
  }

  updateOpportunity(id: string, opportunity: Partial<OpportunityCreateRequest>): Observable<Opportunity> {
    return this.http.put<Opportunity>(`${this.apiBaseUrl}/api/sales/opportunities/${id}`, opportunity).pipe(
      catchError((error) => {
        this._error.set('Failed to update opportunity');
        return throwError(() => error);
      })
    );
  }

  deleteOpportunity(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/api/sales/opportunities/${id}`).pipe(
      catchError((error) => {
        this._error.set('Failed to delete opportunity');
        return throwError(() => error);
      })
    );
  }

  updateOpportunityStage(update: OpportunityStageUpdate): Observable<Opportunity> {
    return this.http.post<Opportunity>(
      `${this.apiBaseUrl}/api/sales/pipeline/opportunities/${update.opportunityId}/move`,
      { stage: update.newStage, notes: update.notes }
    ).pipe(
      catchError((error) => {
        this._error.set('Failed to update opportunity stage');
        return throwError(() => error);
      })
    );
  }

  // ==========================================
  // Activity Management
  // ==========================================

  getActivities(filter?: ActivityFilter, page?: PageRequest): Observable<PageResponse<Activity>> {
    let params = this.buildPageParams(page);
    if (filter) {
      params = this.addFilterParams(params, filter);
    }
    return this.http.get<PageResponse<Activity>>(`${this.apiBaseUrl}/api/sales/activities`, { params }).pipe(
      catchError((error) => {
        this._error.set('Failed to load activities');
        return throwError(() => error);
      })
    );
  }

  getActivity(id: string): Observable<Activity> {
    return this.http.get<Activity>(`${this.apiBaseUrl}/api/sales/activities/${id}`).pipe(
      catchError((error) => {
        this._error.set('Failed to load activity');
        return throwError(() => error);
      })
    );
  }

  createActivity(activity: ActivityCreateRequest): Observable<Activity> {
    return this.http.post<Activity>(`${this.apiBaseUrl}/api/sales/activities`, activity).pipe(
      catchError((error) => {
        this._error.set('Failed to create activity');
        return throwError(() => error);
      })
    );
  }

  logCall(request: ActivityLogRequest): Observable<Activity> {
    return this.http.post<Activity>(`${this.apiBaseUrl}/api/sales/activities/log/call`, request).pipe(
      catchError((error) => {
        this._error.set('Failed to log call');
        return throwError(() => error);
      })
    );
  }

  logEmail(request: ActivityLogRequest): Observable<Activity> {
    return this.http.post<Activity>(`${this.apiBaseUrl}/api/sales/activities/log/email`, request).pipe(
      catchError((error) => {
        this._error.set('Failed to log email');
        return throwError(() => error);
      })
    );
  }

  logMeeting(request: ActivityLogRequest): Observable<Activity> {
    return this.http.post<Activity>(`${this.apiBaseUrl}/api/sales/activities/log/meeting`, request).pipe(
      catchError((error) => {
        this._error.set('Failed to log meeting');
        return throwError(() => error);
      })
    );
  }

  scheduleDemo(request: ActivityCreateRequest): Observable<Activity> {
    return this.http.post<Activity>(`${this.apiBaseUrl}/api/sales/activities/schedule/demo`, request).pipe(
      catchError((error) => {
        this._error.set('Failed to schedule demo');
        return throwError(() => error);
      })
    );
  }

  getOverdueActivities(): Observable<Activity[]> {
    return this.http.get<Activity[]>(`${this.apiBaseUrl}/api/sales/activities/overdue`).pipe(
      catchError(() => of([]))
    );
  }

  getUpcomingActivities(): Observable<Activity[]> {
    return this.http.get<Activity[]>(`${this.apiBaseUrl}/api/sales/activities/upcoming`).pipe(
      catchError(() => of([]))
    );
  }

  completeActivity(id: string, outcome?: string): Observable<Activity> {
    return this.http.post<Activity>(`${this.apiBaseUrl}/api/sales/activities/${id}/complete`, { outcome }).pipe(
      catchError((error) => {
        this._error.set('Failed to complete activity');
        return throwError(() => error);
      })
    );
  }

  // ==========================================
  // Pipeline Management
  // ==========================================

  getPipelineKanban(): Observable<PipelineKanban> {
    return this.http.get<PipelineKanban>(`${this.apiBaseUrl}/api/sales/pipeline/kanban`).pipe(
      catchError((error) => {
        this._error.set('Failed to load pipeline');
        return throwError(() => error);
      })
    );
  }

  getPipelineMetrics(): Observable<PipelineMetrics> {
    return this.http.get<PipelineMetrics>(`${this.apiBaseUrl}/api/sales/pipeline/metrics`).pipe(
      catchError((error) => {
        this._error.set('Failed to load pipeline metrics');
        return throwError(() => error);
      })
    );
  }

  getPipelineForecast(): Observable<PipelineForecast[]> {
    return this.http.get<PipelineForecast[]>(`${this.apiBaseUrl}/api/sales/pipeline/forecast`).pipe(
      catchError(() => of([]))
    );
  }

  getAtRiskDeals(): Observable<Opportunity[]> {
    return this.http.get<Opportunity[]>(`${this.apiBaseUrl}/api/sales/pipeline/at-risk`).pipe(
      catchError(() => of([]))
    );
  }

  getClosingSoon(days: number = 30): Observable<Opportunity[]> {
    return this.http.get<Opportunity[]>(`${this.apiBaseUrl}/api/sales/pipeline/closing-soon`, {
      params: { days: days.toString() }
    }).pipe(
      catchError(() => of([]))
    );
  }

  // ==========================================
  // Private Helpers
  // ==========================================

  private buildPageParams(page?: PageRequest): HttpParams {
    let params = new HttpParams();
    if (page) {
      params = params.set('page', page.page.toString());
      params = params.set('size', page.size.toString());
      if (page.sort) {
        params = params.set('sort', page.sort);
      }
      if (page.direction) {
        params = params.set('direction', page.direction);
      }
    }
    return params;
  }

  private addFilterParams<T extends object>(params: HttpParams, filter: T): HttpParams {
    Object.entries(filter).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        if (Array.isArray(value)) {
          params = params.set(key, value.join(','));
        } else {
          params = params.set(key, String(value));
        }
      }
    });
    return params;
  }
}
