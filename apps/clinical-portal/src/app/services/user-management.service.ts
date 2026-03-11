import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserResponse {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  tenantIds: string[];
  active: boolean;
  emailVerified: boolean;
  mfaEnabled: boolean;
  forcePasswordChange: boolean;
  lastLoginAt: string | null;
  failedLoginAttempts: number;
  accountLockedUntil: string | null;
  notes: string | null;
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  notes?: string;
}

export interface TempPasswordResponse {
  temporaryPassword: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class UserManagementService {
  private readonly baseUrl = '/api/v1/users';

  constructor(private http: HttpClient) {}

  getUsers(tenantId?: string): Observable<UserResponse[]> {
    let params = new HttpParams();
    if (tenantId) params = params.set('tenantId', tenantId);
    return this.http.get<UserResponse[]>(this.baseUrl, { params });
  }

  getUser(id: string): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/${id}`);
  }

  updateUser(id: string, request: UpdateUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.baseUrl}/${id}`, request);
  }

  deactivateUser(id: string): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.baseUrl}/${id}/deactivate`, {});
  }

  reactivateUser(id: string): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.baseUrl}/${id}/reactivate`, {});
  }

  updateRoles(id: string, roles: string[]): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.baseUrl}/${id}/roles`, roles);
  }

  updateTenants(id: string, tenantIds: string[]): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.baseUrl}/${id}/tenants`, tenantIds);
  }

  unlockAccount(id: string): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.baseUrl}/${id}/unlock`, {});
  }

  resetPassword(id: string): Observable<TempPasswordResponse> {
    return this.http.post<TempPasswordResponse>(`${this.baseUrl}/${id}/reset-password`, {});
  }
}
