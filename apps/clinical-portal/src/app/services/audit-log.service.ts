import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AuditLogEntry {
  id: string;
  userId: string;
  username: string;
  tenantId: string;
  eventType: string;
  action: string;
  resourceType: string;
  resourceId: string;
  ipAddress: string;
  success: boolean;
  timestamp: string;
}

export interface AuditLogPage {
  content: AuditLogEntry[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class AuditLogService {
  private readonly baseUrl = '/api/v1/audit/logs';

  constructor(private http: HttpClient) {}

  queryLogs(params: {
    eventType?: string;
    page?: number;
    size?: number;
  }): Observable<AuditLogPage> {
    let httpParams = new HttpParams();
    if (params.eventType) httpParams = httpParams.set('eventType', params.eventType);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
    return this.http.get<AuditLogPage>(this.baseUrl, { params: httpParams });
  }

  getFailedLogins(hoursBack = 24): Observable<AuditLogPage> {
    return this.http.get<AuditLogPage>(`${this.baseUrl}/failed-logins`, {
      params: new HttpParams().set('hoursBack', hoursBack.toString()),
    });
  }

  getAccessDenied(hoursBack = 24): Observable<AuditLogPage> {
    return this.http.get<AuditLogPage>(`${this.baseUrl}/access-denied`, {
      params: new HttpParams().set('hoursBack', hoursBack.toString()),
    });
  }
}
