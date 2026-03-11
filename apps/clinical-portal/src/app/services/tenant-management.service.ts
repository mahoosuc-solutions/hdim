import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserResponse } from './user-management.service';

export interface TenantResponse {
  id: string;
  name: string;
  status: string;
  userCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateTenantRequest {
  name?: string;
}

@Injectable({ providedIn: 'root' })
export class TenantManagementService {
  private readonly baseUrl = '/api/v1/tenants';

  constructor(private http: HttpClient) {}

  getTenants(): Observable<TenantResponse[]> {
    return this.http.get<TenantResponse[]>(this.baseUrl);
  }

  getTenant(id: string): Observable<TenantResponse> {
    return this.http.get<TenantResponse>(`${this.baseUrl}/${id}`);
  }

  updateTenant(id: string, request: UpdateTenantRequest): Observable<TenantResponse> {
    return this.http.put<TenantResponse>(`${this.baseUrl}/${id}`, request);
  }

  getTenantUsers(id: string): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.baseUrl}/${id}/users`);
  }
}
