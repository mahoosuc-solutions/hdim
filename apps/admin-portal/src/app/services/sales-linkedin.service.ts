import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  LinkedInCampaign,
  LinkedInOutreach,
  LinkedInConnectionRequest,
  LinkedInInMailRequest,
  LinkedInAnalytics,
  PageRequest,
  PageResponse,
} from '../models/sales.model';

/**
 * LinkedIn Outreach Management Service
 *
 * Manages LinkedIn campaigns, connection requests, and InMail messages.
 */
@Injectable({
  providedIn: 'root',
})
export class SalesLinkedInService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = environment.apiConfig.salesApiUrl;

  // State signals
  private _campaigns = signal<LinkedInCampaign[]>([]);
  private _outreachList = signal<LinkedInOutreach[]>([]);
  private _analytics = signal<LinkedInAnalytics | null>(null);
  private _isLoading = signal<boolean>(false);
  private _error = signal<string | null>(null);

  // Public read-only signals
  readonly campaigns = this._campaigns.asReadonly();
  readonly outreachList = this._outreachList.asReadonly();
  readonly analytics = this._analytics.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();
  readonly error = this._error.asReadonly();

  // ==========================================
  // Campaign Management
  // ==========================================

  getCampaigns(page?: PageRequest, status?: string, search?: string): Observable<PageResponse<LinkedInCampaign>> {
    this._isLoading.set(true);
    let url = `${this.apiBaseUrl}/api/sales/linkedin/campaigns`;
    const params: string[] = [];

    if (page) {
      params.push(`page=${page.page}&size=${page.size}`);
    }
    if (status) {
      params.push(`status=${status}`);
    }
    if (search) {
      params.push(`search=${encodeURIComponent(search)}`);
    }
    if (params.length > 0) {
      url += '?' + params.join('&');
    }

    return this.http.get<PageResponse<LinkedInCampaign>>(url).pipe(
      tap((response) => {
        this._campaigns.set(response.content);
        this._isLoading.set(false);
      }),
      catchError((error) => {
        this._isLoading.set(false);
        this._error.set('Failed to load campaigns');
        return throwError(() => error);
      })
    );
  }

  getCampaign(id: string): Observable<LinkedInCampaign> {
    return this.http.get<LinkedInCampaign>(`${this.apiBaseUrl}/api/sales/linkedin/campaigns/${id}`).pipe(
      catchError((error) => {
        this._error.set('Failed to load campaign');
        return throwError(() => error);
      })
    );
  }

  createCampaign(campaign: Partial<LinkedInCampaign>): Observable<LinkedInCampaign> {
    return this.http.post<LinkedInCampaign>(`${this.apiBaseUrl}/api/sales/linkedin/campaigns`, campaign).pipe(
      tap((created) => {
        this._campaigns.update((list) => [...list, created]);
      }),
      catchError((error) => {
        this._error.set('Failed to create campaign');
        return throwError(() => error);
      })
    );
  }

  updateCampaign(id: string, campaign: Partial<LinkedInCampaign>): Observable<LinkedInCampaign> {
    return this.http.put<LinkedInCampaign>(`${this.apiBaseUrl}/api/sales/linkedin/campaigns/${id}`, campaign).pipe(
      tap((updated) => {
        this._campaigns.update((list) =>
          list.map((c) => (c.id === id ? updated : c))
        );
      }),
      catchError((error) => {
        this._error.set('Failed to update campaign');
        return throwError(() => error);
      })
    );
  }

  deleteCampaign(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/api/sales/linkedin/campaigns/${id}`).pipe(
      tap(() => {
        this._campaigns.update((list) => list.filter((c) => c.id !== id));
      }),
      catchError((error) => {
        this._error.set('Failed to delete campaign');
        return throwError(() => error);
      })
    );
  }

  activateCampaign(id: string): Observable<LinkedInCampaign> {
    return this.http.post<LinkedInCampaign>(`${this.apiBaseUrl}/api/sales/linkedin/campaigns/${id}/activate`, {}).pipe(
      tap((updated) => {
        this._campaigns.update((list) =>
          list.map((c) => (c.id === id ? { ...c, status: 'ACTIVE' as const } : c))
        );
      }),
      catchError((error) => {
        this._error.set('Failed to activate campaign');
        return throwError(() => error);
      })
    );
  }

  pauseCampaign(id: string): Observable<LinkedInCampaign> {
    return this.http.post<LinkedInCampaign>(`${this.apiBaseUrl}/api/sales/linkedin/campaigns/${id}/pause`, {}).pipe(
      tap((updated) => {
        this._campaigns.update((list) =>
          list.map((c) => (c.id === id ? { ...c, status: 'PAUSED' as const } : c))
        );
      }),
      catchError((error) => {
        this._error.set('Failed to pause campaign');
        return throwError(() => error);
      })
    );
  }

  // ==========================================
  // Connection Requests
  // ==========================================

  sendConnectionRequest(request: LinkedInConnectionRequest): Observable<LinkedInOutreach> {
    const endpoint = request.leadId
      ? `${this.apiBaseUrl}/api/sales/linkedin/connect/lead/${request.leadId}`
      : `${this.apiBaseUrl}/api/sales/linkedin/connect/contact/${request.contactId}`;

    return this.http.post<LinkedInOutreach>(endpoint, {
      linkedInProfileUrl: request.linkedInProfileUrl,
      message: request.message,
      campaignId: request.campaignId,
    }).pipe(
      tap((outreach) => {
        this._outreachList.update((list) => [outreach, ...list]);
      }),
      catchError((error) => {
        this._error.set('Failed to send connection request');
        return throwError(() => error);
      })
    );
  }

  // ==========================================
  // InMail Messages
  // ==========================================

  sendInMail(request: LinkedInInMailRequest): Observable<LinkedInOutreach> {
    const endpoint = request.leadId
      ? `${this.apiBaseUrl}/api/sales/linkedin/inmail/lead/${request.leadId}`
      : `${this.apiBaseUrl}/api/sales/linkedin/inmail/contact/${request.contactId}`;

    return this.http.post<LinkedInOutreach>(endpoint, {
      linkedInProfileUrl: request.linkedInProfileUrl,
      subject: request.subject,
      message: request.message,
      campaignId: request.campaignId,
    }).pipe(
      tap((outreach) => {
        this._outreachList.update((list) => [outreach, ...list]);
      }),
      catchError((error) => {
        this._error.set('Failed to send InMail');
        return throwError(() => error);
      })
    );
  }

  // ==========================================
  // Outreach Management
  // ==========================================

  getOutreach(campaignId?: string, page?: PageRequest): Observable<PageResponse<LinkedInOutreach>> {
    this._isLoading.set(true);
    let url = `${this.apiBaseUrl}/api/sales/linkedin/outreach`;
    const params: string[] = [];

    if (campaignId) {
      params.push(`campaignId=${campaignId}`);
    }
    if (page) {
      params.push(`page=${page.page}&size=${page.size}`);
    }
    if (params.length > 0) {
      url += '?' + params.join('&');
    }

    return this.http.get<PageResponse<LinkedInOutreach>>(url).pipe(
      tap((response) => {
        this._outreachList.set(response.content);
        this._isLoading.set(false);
      }),
      catchError((error) => {
        this._isLoading.set(false);
        this._error.set('Failed to load outreach');
        return throwError(() => error);
      })
    );
  }

  updateOutreachStatus(id: string, status: LinkedInOutreach['status']): Observable<LinkedInOutreach> {
    return this.http.patch<LinkedInOutreach>(
      `${this.apiBaseUrl}/api/sales/linkedin/outreach/${id}/status`,
      { status }
    ).pipe(
      tap((updated) => {
        this._outreachList.update((list) =>
          list.map((o) => (o.id === id ? updated : o))
        );
      }),
      catchError((error) => {
        this._error.set('Failed to update outreach status');
        return throwError(() => error);
      })
    );
  }

  // ==========================================
  // Analytics
  // ==========================================

  getAnalytics(): Observable<LinkedInAnalytics> {
    return this.http.get<LinkedInAnalytics>(`${this.apiBaseUrl}/api/sales/linkedin/analytics`).pipe(
      tap((analytics) => this._analytics.set(analytics)),
      catchError((error) => {
        this._error.set('Failed to load analytics');
        return throwError(() => error);
      })
    );
  }

  getCampaignAnalytics(campaignId: string): Observable<LinkedInAnalytics> {
    return this.http.get<LinkedInAnalytics>(
      `${this.apiBaseUrl}/api/sales/linkedin/campaign/${campaignId}/analytics`
    ).pipe(
      catchError((error) => {
        this._error.set('Failed to load campaign analytics');
        return throwError(() => error);
      })
    );
  }
}
