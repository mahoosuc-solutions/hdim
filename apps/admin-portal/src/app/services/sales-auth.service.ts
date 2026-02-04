import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface SalesLoginRequest {
  email: string;
  password: string;
}

export interface SalesLoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: SalesUserInfo;
}

export interface SalesUserInfo {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  tenantId: string;
}

/**
 * Authentication service for the Sales Automation Engine.
 * Handles login, logout, token management for sales functionality.
 */
@Injectable({
  providedIn: 'root',
})
export class SalesAuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly STORAGE_KEYS = {
    ACCESS_TOKEN: 'sales_access_token',
    REFRESH_TOKEN: 'sales_refresh_token',
    USER: 'sales_user',
  };

  // API base URL - points to sales-automation-service backend
  private readonly apiBaseUrl = environment.apiConfig.salesApiUrl;

  // State signals
  private _isAuthenticated = signal<boolean>(this.hasValidToken());
  private _currentUser = signal<SalesUserInfo | null>(this.loadStoredUser());
  private _isLoading = signal<boolean>(false);

  // Public read-only signals
  readonly isAuthenticated = this._isAuthenticated.asReadonly();
  readonly currentUser = this._currentUser.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();

  // Computed signals
  readonly userDisplayName = computed(() => {
    const user = this._currentUser();
    return user ? `${user.firstName} ${user.lastName}` : '';
  });

  readonly userRole = computed(() => {
    const user = this._currentUser();
    return user?.role ?? '';
  });

  readonly tenantId = computed(() => {
    const user = this._currentUser();
    return user?.tenantId ?? environment.apiConfig.defaultTenantId;
  });

  /**
   * Login with email and password.
   */
  login(credentials: SalesLoginRequest): Observable<SalesLoginResponse> {
    this._isLoading.set(true);

    return this.http
      .post<SalesLoginResponse>(`${this.apiBaseUrl}/api/auth/login`, credentials)
      .pipe(
        tap((response) => {
          this.storeTokens(response);
          this._currentUser.set(response.user);
          this._isAuthenticated.set(true);
          this._isLoading.set(false);
        }),
        catchError((error) => {
          this._isLoading.set(false);
          return this.handleError(error);
        })
      );
  }

  /**
   * Refresh the access token using the refresh token.
   */
  refreshToken(): Observable<SalesLoginResponse> {
    const refreshToken = localStorage.getItem(this.STORAGE_KEYS.REFRESH_TOKEN);
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    return this.http
      .post<SalesLoginResponse>(`${this.apiBaseUrl}/api/auth/refresh`, {
        refreshToken,
      })
      .pipe(
        tap((response) => {
          this.storeTokens(response);
          this._currentUser.set(response.user);
        }),
        catchError((error) => {
          this.logout();
          return this.handleError(error);
        })
      );
  }

  /**
   * Logout and clear all stored data.
   */
  logout(): void {
    localStorage.removeItem(this.STORAGE_KEYS.ACCESS_TOKEN);
    localStorage.removeItem(this.STORAGE_KEYS.REFRESH_TOKEN);
    localStorage.removeItem(this.STORAGE_KEYS.USER);

    this._isAuthenticated.set(false);
    this._currentUser.set(null);

    this.router.navigate(['/sales/login']);
  }

  /**
   * Get the current access token for API requests.
   */
  getAccessToken(): string | null {
    return localStorage.getItem(this.STORAGE_KEYS.ACCESS_TOKEN);
  }

  /**
   * Check if the user has a valid (non-expired) token.
   */
  hasValidToken(): boolean {
    const token = localStorage.getItem(this.STORAGE_KEYS.ACCESS_TOKEN);
    if (!token) return false;

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const exp = payload.exp * 1000;
      return Date.now() < exp;
    } catch {
      return false;
    }
  }

  /**
   * Get the current user info from the server.
   */
  fetchCurrentUser(): Observable<SalesUserInfo> {
    return this.http.get<SalesUserInfo>(`${this.apiBaseUrl}/api/auth/me`).pipe(
      tap((user) => {
        this._currentUser.set(user);
        localStorage.setItem(this.STORAGE_KEYS.USER, JSON.stringify(user));
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Check if user has a specific role.
   */
  hasRole(role: string): boolean {
    const user = this._currentUser();
    return user?.role === role;
  }

  /**
   * Check if user has any of the specified roles.
   */
  hasAnyRole(roles: string[]): boolean {
    const user = this._currentUser();
    return user ? roles.includes(user.role) : false;
  }

  private storeTokens(response: SalesLoginResponse): void {
    localStorage.setItem(this.STORAGE_KEYS.ACCESS_TOKEN, response.accessToken);
    localStorage.setItem(this.STORAGE_KEYS.REFRESH_TOKEN, response.refreshToken);
    localStorage.setItem(this.STORAGE_KEYS.USER, JSON.stringify(response.user));
  }

  private loadStoredUser(): SalesUserInfo | null {
    const stored = localStorage.getItem(this.STORAGE_KEYS.USER);
    return stored ? JSON.parse(stored) : null;
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let message = 'An unexpected error occurred';

    if (error.error instanceof ErrorEvent) {
      message = error.error.message;
    } else {
      message = error.error?.message || `Error: ${error.status}`;
    }

    return throwError(() => new Error(message));
  }
}
