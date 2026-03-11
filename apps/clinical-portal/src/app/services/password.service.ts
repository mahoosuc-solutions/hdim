import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ForceChangePasswordRequest {
  newPassword: string;
}

@Injectable({ providedIn: 'root' })
export class PasswordService {
  private readonly baseUrl = '/api/v1/auth/password';

  constructor(private http: HttpClient) {}

  changePassword(request: ChangePasswordRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/change`, request);
  }

  forceChangePassword(request: ForceChangePasswordRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/force-change`, request);
  }
}
