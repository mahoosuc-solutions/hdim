import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, tap, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserInfo;
}

export interface UserInfo {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface LinkedInConnectionStatus {
  connected: boolean;
  linkedInMemberId?: string;
  profileUrl?: string;
  expiresAt?: string;
  lastSync?: string;
  syncError?: string;
}

/**
 * Authentication service for the Investor Dashboard.
 * Handles login, logout, token management, and LinkedIn OAuth.
 */
@Injectable({
  providedIn: 'root',
})
export class InvestorAuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly STORAGE_KEYS = {
    ACCESS_TOKEN: 'investor_access_token',
    REFRESH_TOKEN: 'investor_refresh_token',
    USER: 'investor_user',
  };

  // API base URL - points to investor-dashboard-service backend
  // Uses environment configuration for both dev and production
  private readonly apiBaseUrl = environment.apiConfig.investorApiUrl;

  // State signals
  private _isAuthenticated = signal<boolean>(this.hasValidToken());
  private _currentUser = signal<UserInfo | null>(this.loadStoredUser());
  private _isLoading = signal<boolean>(false);
  private _linkedInStatus = signal<LinkedInConnectionStatus | null>(null);

  // Public read-only signals
  readonly isAuthenticated = this._isAuthenticated.asReadonly();
  readonly currentUser = this._currentUser.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();
  readonly linkedInStatus = this._linkedInStatus.asReadonly();

  // Computed signals
  readonly userDisplayName = computed(() => {
    const user = this._currentUser();
    return user ? `${user.firstName} ${user.lastName}` : '';
  });

  readonly isLinkedInConnected = computed(() => {
    const status = this._linkedInStatus();
    return status?.connected ?? false;
  });

  /**
   * Login with email and password.
   */
  login(credentials: LoginRequest): Observable<LoginResponse> {
    this._isLoading.set(true);

    return this.http
      .post<LoginResponse>(`${this.apiBaseUrl}/api/auth/login`, credentials)
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
  refreshToken(): Observable<LoginResponse> {
    const refreshToken = localStorage.getItem(this.STORAGE_KEYS.REFRESH_TOKEN);
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    return this.http
      .post<LoginResponse>(`${this.apiBaseUrl}/api/auth/refresh`, {
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
    this._linkedInStatus.set(null);

    this.router.navigate(['/investor-login']);
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

    // Decode JWT and check expiration
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const exp = payload.exp * 1000; // Convert to milliseconds
      return Date.now() < exp;
    } catch {
      return false;
    }
  }

  /**
   * Get the current user info from the server.
   */
  fetchCurrentUser(): Observable<UserInfo> {
    return this.http.get<UserInfo>(`${this.apiBaseUrl}/api/auth/me`).pipe(
      tap((user) => {
        this._currentUser.set(user);
        localStorage.setItem(this.STORAGE_KEYS.USER, JSON.stringify(user));
      }),
      catchError(this.handleError)
    );
  }

  // ==========================================
  // LinkedIn OAuth Methods
  // ==========================================

  /**
   * Get the LinkedIn OAuth authorization URL.
   */
  getLinkedInAuthUrl(): Observable<{ authorizationUrl: string; state: string }> {
    return this.http
      .get<{ authorizationUrl: string; state: string }>(
        `${this.apiBaseUrl}/api/linkedin/auth-url`
      )
      .pipe(catchError(this.handleError));
  }

  /**
   * Handle LinkedIn OAuth callback.
   */
  handleLinkedInCallback(
    code: string,
    state: string
  ): Observable<LinkedInConnectionStatus> {
    return this.http
      .post<LinkedInConnectionStatus>(
        `${this.apiBaseUrl}/api/linkedin/callback`,
        { code, state }
      )
      .pipe(
        tap((status) => this._linkedInStatus.set(status)),
        catchError(this.handleError)
      );
  }

  /**
   * Get current LinkedIn connection status.
   */
  fetchLinkedInStatus(): Observable<LinkedInConnectionStatus> {
    return this.http
      .get<LinkedInConnectionStatus>(
        `${this.apiBaseUrl}/api/linkedin/status`
      )
      .pipe(
        tap((status) => this._linkedInStatus.set(status)),
        catchError(this.handleError)
      );
  }

  /**
   * Disconnect LinkedIn account.
   */
  disconnectLinkedIn(): Observable<void> {
    return this.http
      .post<void>(`${this.apiBaseUrl}/api/linkedin/disconnect`, {})
      .pipe(
        tap(() =>
          this._linkedInStatus.set({ connected: false })
        ),
        catchError(this.handleError)
      );
  }

  // ==========================================
  // Private Methods
  // ==========================================

  private storeTokens(response: LoginResponse): void {
    localStorage.setItem(this.STORAGE_KEYS.ACCESS_TOKEN, response.accessToken);
    localStorage.setItem(this.STORAGE_KEYS.REFRESH_TOKEN, response.refreshToken);
    localStorage.setItem(this.STORAGE_KEYS.USER, JSON.stringify(response.user));
  }

  private loadStoredUser(): UserInfo | null {
    const stored = localStorage.getItem(this.STORAGE_KEYS.USER);
    return stored ? JSON.parse(stored) : null;
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let message = 'An unexpected error occurred';

    if (error.error instanceof ErrorEvent) {
      // Client-side error
      message = error.error.message;
    } else {
      // Server-side error
      message = error.error?.message || `Error: ${error.status}`;
    }

    return throwError(() => new Error(message));
  }
}
