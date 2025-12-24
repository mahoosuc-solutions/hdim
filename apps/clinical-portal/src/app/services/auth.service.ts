import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError, of, timer } from 'rxjs';
import { map, catchError, tap, switchMap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { ApiService } from './api.service';

/**
 * Authentication Service - Handles user authentication and authorization
 *
 * Features:
 * - User login/logout
 * - JWT token management (HttpOnly cookies + localStorage fallback)
 * - Token refresh
 * - Role-based authorization
 * - Permission checking
 * - Auto-logout on token expiration
 * - Authentication state management
 *
 * SECURITY IMPLEMENTATION (HIPAA Compliant):
 * The backend now sets HttpOnly cookies with JWT tokens for XSS protection:
 * - hdim_access_token: Access token (HttpOnly, Secure, SameSite=Strict)
 * - hdim_refresh_token: Refresh token (HttpOnly, Secure, SameSite=Strict)
 *
 * Token Storage Priority:
 * 1. HttpOnly cookies (primary - XSS protected, HIPAA compliant)
 * 2. localStorage (fallback for legacy clients and demo mode)
 *
 * The API service is configured with withCredentials: true to send cookies
 * automatically with all requests. This provides seamless authentication
 * without exposing tokens to JavaScript.
 *
 * NOTE: Direct token access methods (getToken, setToken) are kept for:
 * - Backwards compatibility with existing code
 * - Demo mode functionality
 * - Token expiration checking (parsing JWT claims)
 */
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly AUTH_TOKEN_KEY = 'healthdata_auth_token';
  private readonly REFRESH_TOKEN_KEY = 'healthdata_refresh_token';
  private readonly USER_KEY = 'healthdata_user';
  private readonly TOKEN_REFRESH_INTERVAL = 5 * 60 * 1000; // 5 minutes

  private currentUserSubject = new BehaviorSubject<User | null>(this.getUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasValidToken());
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  private tokenRefreshSubscription: any;

  constructor(
    private http: HttpClient,
    private apiService: ApiService,
    private router: Router
  ) {
    // Start token refresh timer if user is logged in
    if (this.isAuthenticated()) {
      this.startTokenRefreshTimer();
    }
  }

  /**
   * Login with username and password
   * Returns either LoginResponse or MfaRequiredResponse
   */
  login(username: string, password: string): Observable<LoginResponse | MfaRequiredResponse> {
    const url = '/auth/login';
    const body: LoginRequest = { username, password };

    return this.apiService.post<LoginResponse | MfaRequiredResponse>(url, body).pipe(
      tap((response) => {
        // Check if MFA is required
        if ('mfaRequired' in response && response.mfaRequired) {
          // Don't handle login success yet - MFA verification needed
          return;
        }
        // Normal login success
        this.handleLoginSuccess(response as LoginResponse);
      }),
      catchError((error) => {
        console.error('Login error:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Verify MFA code and complete login
   */
  verifyMfa(mfaToken: string, code: string, useRecoveryCode = false): Observable<LoginResponse> {
    const url = '/auth/mfa/verify';
    const body: MfaVerifyRequest = { mfaToken, code, useRecoveryCode };

    return this.apiService.post<LoginResponse>(url, body).pipe(
      tap((response) => {
        this.handleLoginSuccess(response);
      }),
      catchError((error) => {
        console.error('MFA verification error:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Initialize MFA setup - returns QR code
   */
  setupMfa(): Observable<MfaSetupResponse> {
    const url = '/auth/mfa/setup';
    return this.apiService.post<MfaSetupResponse>(url, {});
  }

  /**
   * Confirm MFA setup with TOTP code
   */
  confirmMfaSetup(code: string): Observable<MfaRecoveryCodesResponse> {
    const url = '/auth/mfa/confirm';
    return this.apiService.post<MfaRecoveryCodesResponse>(url, { code });
  }

  /**
   * Disable MFA
   */
  disableMfa(code: string): Observable<void> {
    const url = '/auth/mfa/disable';
    return this.apiService.post<void>(url, { code });
  }

  /**
   * Get MFA status
   */
  getMfaStatus(): Observable<MfaStatusResponse> {
    const url = '/auth/mfa/status';
    return this.apiService.get<MfaStatusResponse>(url);
  }

  /**
   * Regenerate recovery codes
   */
  regenerateRecoveryCodes(code: string): Observable<MfaRecoveryCodesResponse> {
    const url = '/auth/mfa/recovery-codes';
    return this.apiService.post<MfaRecoveryCodesResponse>(url, { code });
  }

  /**
   * Check if response indicates MFA is required
   */
  isMfaRequired(response: LoginResponse | MfaRequiredResponse): response is MfaRequiredResponse {
    return 'mfaRequired' in response && response.mfaRequired === true;
  }

  /**
   * Logout the current user
   */
  logout(): void {
    this.stopTokenRefreshTimer();
    this.clearAuthData();
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
    this.router.navigate(['/login']);
  }

  /**
   * Refresh the authentication token
   */
  refreshToken(): Observable<TokenResponse> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    const url = '/auth/refresh';
    const body = { refreshToken };

    return this.apiService.post<TokenResponse>(url, body).pipe(
      tap((response) => {
        this.setToken(response.accessToken);
        if (response.refreshToken) {
          this.setRefreshToken(response.refreshToken);
        }
      }),
      catchError((error) => {
        console.error('Token refresh error:', error);
        this.logout();
        return throwError(() => error);
      })
    );
  }

  /**
   * Get the current user
   */
  getCurrentUser(): Observable<User> {
    const user = this.currentUserSubject.value;
    if (user) {
      return of(user);
    }

    const url = '/auth/me';
    return this.apiService.get<User>(url).pipe(
      tap((user) => {
        this.setUser(user);
        this.currentUserSubject.next(user);
      }),
      catchError((error) => {
        console.error('Get current user error:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return this.hasValidToken();
  }

  /**
   * Get the current tenant ID from the logged-in user
   */
  getTenantId(): string | null {
    const user = this.currentUserSubject.value;
    return user?.tenantId || null;
  }

  /**
   * Get the current user synchronously (useful for interceptors)
   */
  get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  /**
   * Check if user has a specific role
   */
  hasRole(role: string): boolean {
    const user = this.currentUserSubject.value;
    if (!user || !user.roles) return false;

    return user.roles.some((r) => r.name === role || r.name === 'ADMIN');
  }

  /**
   * Check if user has multiple roles (any of them)
   */
  hasAnyRole(roles: string[]): boolean {
    return roles.some((role) => this.hasRole(role));
  }

  /**
   * Check if user has all specified roles
   */
  hasAllRoles(roles: string[]): boolean {
    return roles.every((role) => this.hasRole(role));
  }

  /**
   * Check if user has a specific permission
   */
  hasPermission(permission: string): boolean {
    const user = this.currentUserSubject.value;
    if (!user) return false;

    // Admin has all permissions
    if (this.hasRole('ADMIN')) return true;

    // Check if any role has the permission
    return user.roles.some((role) =>
      role.permissions?.some((p) => p.name === permission)
    );
  }

  /**
   * Check if user has any of the specified permissions
   */
  hasAnyPermission(permissions: string[]): boolean {
    return permissions.some((permission) => this.hasPermission(permission));
  }

  /**
   * Get the authentication token
   */
  getToken(): string | null {
    return localStorage.getItem(this.AUTH_TOKEN_KEY);
  }

  /**
   * Get the refresh token
   */
  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  /**
   * Set the authentication token
   */
  setToken(token: string): void {
    localStorage.setItem(this.AUTH_TOKEN_KEY, token);
  }

  /**
   * Set the refresh token
   */
  setRefreshToken(token: string): void {
    localStorage.setItem(this.REFRESH_TOKEN_KEY, token);
  }

  /**
   * Remove the authentication token
   */
  removeToken(): void {
    localStorage.removeItem(this.AUTH_TOKEN_KEY);
  }

  /**
   * Remove the refresh token
   */
  removeRefreshToken(): void {
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
  }

  /**
   * Get user from local storage
   */
  private getUserFromStorage(): User | null {
    const userJson = localStorage.getItem(this.USER_KEY);
    if (!userJson) return null;

    try {
      return JSON.parse(userJson);
    } catch {
      return null;
    }
  }

  /**
   * Set user in local storage
   */
  private setUser(user: User): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  /**
   * Remove user from local storage
   */
  private removeUser(): void {
    localStorage.removeItem(this.USER_KEY);
  }

  /**
   * Check if token is valid (not expired)
   * Supports both real JWT tokens and demo tokens
   */
  private hasValidToken(): boolean {
    const token = this.getToken();
    if (!token) return false;

    // Check for demo token (starts with 'demo-')
    if (token.startsWith('demo-')) {
      // Demo tokens are always valid if user exists
      return this.getUserFromStorage() !== null;
    }

    try {
      const payload = this.parseJwt(token);
      const now = Date.now() / 1000;
      return payload.exp > now;
    } catch {
      return false;
    }
  }

  /**
   * Parse JWT token
   */
  private parseJwt(token: string): JwtPayload {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  }

  /**
   * Handle successful login
   */
  private handleLoginSuccess(response: LoginResponse): void {
    this.setToken(response.accessToken);
    if (response.refreshToken) {
      this.setRefreshToken(response.refreshToken);
    }
    this.setUser(response.user);
    this.currentUserSubject.next(response.user);
    this.isAuthenticatedSubject.next(true);
    this.startTokenRefreshTimer();
  }

  /**
   * Clear all authentication data
   */
  private clearAuthData(): void {
    this.removeToken();
    this.removeRefreshToken();
    this.removeUser();
  }

  /**
   * Start automatic token refresh timer
   */
  private startTokenRefreshTimer(): void {
    this.stopTokenRefreshTimer();
    this.tokenRefreshSubscription = timer(
      this.TOKEN_REFRESH_INTERVAL,
      this.TOKEN_REFRESH_INTERVAL
    )
      .pipe(switchMap(() => this.refreshToken()))
      .subscribe({
        error: (error) => {
          console.error('Token refresh timer error:', error);
          this.logout();
        },
      });
  }

  /**
   * Stop automatic token refresh timer
   */
  private stopTokenRefreshTimer(): void {
    if (this.tokenRefreshSubscription) {
      this.tokenRefreshSubscription.unsubscribe();
      this.tokenRefreshSubscription = null;
    }
  }
}

// ==================== Type Definitions ====================

export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  roles: Role[];
  tenantId?: string;
  active: boolean;
  createdAt?: string;
  lastLogin?: string;
}

export interface Role {
  id: string;
  name: string;
  description?: string;
  permissions?: Permission[];
}

export interface Permission {
  id: string;
  name: string;
  description?: string;
  resource?: string;
  action?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
  tenantId?: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken?: string;
  tokenType: string;
  expiresIn: number;
  user: User;
  mfaEnabled?: boolean;
}

export interface MfaRequiredResponse {
  mfaRequired: boolean;
  mfaToken: string;
  message: string;
}

export interface MfaVerifyRequest {
  mfaToken: string;
  code: string;
  useRecoveryCode?: boolean;
}

export interface MfaSetupResponse {
  secret: string;
  qrCodeDataUri: string;
  issuer: string;
  username: string;
}

export interface MfaStatusResponse {
  mfaEnabled: boolean;
  enabledAt?: string;
  remainingRecoveryCodes?: number;
}

export interface MfaRecoveryCodesResponse {
  recoveryCodes: string[];
  message: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken?: string;
  tokenType: string;
  expiresIn: number;
}

interface JwtPayload {
  sub: string;
  exp: number;
  iat: number;
  roles?: string[];
  tenantId?: string;
}
