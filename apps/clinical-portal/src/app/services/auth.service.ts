import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError, of, timer } from 'rxjs';
import { map, catchError, tap, switchMap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { ApiService } from './api.service';
import { LoggerService, ContextualLogger } from './logger.service';

/**
 * Authentication Service - Handles user authentication and authorization
 *
 * Features:
 * - User login/logout
 * - JWT token management via HttpOnly cookies (XSS protected)
 * - Automatic token refresh
 * - Role-based authorization
 * - Permission checking
 * - Auto-logout on token expiration
 * - Authentication state management
 *
 * SECURITY IMPLEMENTATION (HIPAA Compliant):
 * JWT tokens are stored in HttpOnly cookies to prevent XSS attacks:
 * - hdim_access_token: Access token (HttpOnly, Secure, SameSite=Strict)
 * - hdim_refresh_token: Refresh token (HttpOnly, Secure, SameSite=Strict)
 *
 * The API service is configured with withCredentials: true to send cookies
 * automatically with all requests. This provides seamless authentication
 * without exposing tokens to JavaScript.
 *
 * Authentication Flow:
 * 1. Login → Backend sets HttpOnly cookies + returns user info
 * 2. Subsequent requests → Browser automatically includes cookies
 * 3. 401 response → Trigger refresh (backend reads cookie, sets new one)
 * 4. Logout → Backend clears cookies
 *
 * NOTE: localStorage is only used for user profile data, NOT tokens.
 */
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  // NOTE: Tokens are now stored in HttpOnly cookies, not localStorage
  // Only user profile is stored in localStorage
  private readonly USER_KEY = 'healthdata_user';
  private readonly TENANT_KEY = 'healthdata_tenant';
  private readonly TOKEN_REFRESH_INTERVAL = 5 * 60 * 1000; // 5 minutes

  private currentUserSubject = new BehaviorSubject<User | null>(this.getUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasValidSession());
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  private selectedTenantSubject = new BehaviorSubject<string | null>(this.getTenantFromStorage());
  public selectedTenant$ = this.selectedTenantSubject.asObservable();

  private tokenRefreshSubscription: any;

  constructor(
    private http: HttpClient,
    private apiService: ApiService,
    private router: Router,
    private logger: LoggerService
  ) {    // Start token refresh timer if user session exists
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
        this.logger.error('Login failed', error);
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
        this.logger.error('MFA verification failed', error);
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
   * Calls backend to clear HttpOnly cookies and clears local user data
   */
  logout(): void {
    this.stopTokenRefreshTimer();

    // Call backend to clear HttpOnly cookies
    this.apiService.post('/auth/logout', {}).subscribe({
      next: () => this.logger.info('Logout successful - cookies cleared'),
      error: (err) => this.logger.warn('Logout API call failed', err),
    });

    // Clear local state regardless of API result
    this.clearAuthData();
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
    this.router.navigate(['/login']);
  }

  /**
   * Refresh the authentication token
   * Refresh token is sent via HttpOnly cookie automatically
   * New tokens are set as HttpOnly cookies by the backend
   */
  refreshToken(): Observable<TokenResponse> {
    const url = '/auth/refresh';

    // No body needed - refresh token is in HttpOnly cookie
    // Backend reads cookie and sets new cookies in response
    return this.apiService.post<TokenResponse>(url, {}).pipe(
      tap((response) => {
        // Tokens are set as HttpOnly cookies by backend
        // Just update auth state based on successful refresh
        this.isAuthenticatedSubject.next(true);
        this.logger.info('Token refreshed successfully');
      }),
      catchError((error) => {
        this.logger.error('Token refresh failed', error);
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
        this.logger.error('Failed to get current user', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Check if user is authenticated
   * With HttpOnly cookies, we check for user session presence
   * Actual token validity is verified by the backend on each request
   */
  isAuthenticated(): boolean {
    return this.hasValidSession();
  }

  /**
   * Get the current tenant ID from the logged-in user
   */
  getTenantId(): string | null {
    const selectedTenant = this.selectedTenantSubject.value;
    if (selectedTenant) {
      return selectedTenant;
    }

    const user = this.currentUserSubject.value;
    // Use tenantIds array from gateway (UUIDs) if available
    if (user?.tenantIds && user.tenantIds.length > 0) {
      return user.tenantIds[0];  // Use first tenant UUID
    }
    // Fallback to tenantId for backward compatibility
    return user?.tenantId || null;
  }

  /**
   * Get all tenant IDs available to the current user
   */
  getAvailableTenantIds(): string[] {
    const user = this.currentUserSubject.value;
    if (user?.tenantIds && user.tenantIds.length > 0) {
      return user.tenantIds;
    }
    if (user?.tenantId) {
      return [user.tenantId];
    }
    return [];
  }

  /**
   * Set the active tenant for this session
   */
  setTenantId(tenantId: string): void {
    const availableTenants = this.getAvailableTenantIds();
    if (availableTenants.length > 0 && !availableTenants.includes(tenantId)) {
      this.logger.warn('Attempted to set tenant not in user scope', { tenantId });
      return;
    }

    localStorage.setItem(this.TENANT_KEY, tenantId);
    this.selectedTenantSubject.next(tenantId);
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
   * @deprecated Tokens are now stored in HttpOnly cookies (XSS protected)
   * This method returns null - authentication is handled via cookies
   */
  getToken(): string | null {
    // Tokens are in HttpOnly cookies - not accessible from JavaScript
    // This is intentional for XSS protection
    return null;
  }

  /**
   * Get the refresh token
   * @deprecated Tokens are now stored in HttpOnly cookies (XSS protected)
   * This method returns null - refresh is handled via cookies
   */
  getRefreshToken(): string | null {
    // Refresh tokens are in HttpOnly cookies - not accessible from JavaScript
    return null;
  }

  /**
   * Set the authentication token
   * @deprecated Tokens are now set by the backend as HttpOnly cookies
   * This method is a no-op for backwards compatibility
   */
  setToken(_token: string): void {
    // No-op: Tokens are set by backend as HttpOnly cookies
  }

  /**
   * Set the refresh token
   * @deprecated Tokens are now set by the backend as HttpOnly cookies
   * This method is a no-op for backwards compatibility
   */
  setRefreshToken(_token: string): void {
    // No-op: Tokens are set by backend as HttpOnly cookies
  }

  /**
   * Remove the authentication token
   * @deprecated Token removal is handled by backend cookie clearing
   * This method is a no-op - call logout() to clear cookies
   */
  removeToken(): void {
    // No-op: Cookies are cleared by backend on logout
  }

  /**
   * Remove the refresh token
   * @deprecated Token removal is handled by backend cookie clearing
   * This method is a no-op - call logout() to clear cookies
   */
  removeRefreshToken(): void {
    // No-op: Cookies are cleared by backend on logout
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

  private getTenantFromStorage(): string | null {
    const tenantId = localStorage.getItem(this.TENANT_KEY);
    return tenantId || null;
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

  private clearTenant(): void {
    localStorage.removeItem(this.TENANT_KEY);
    this.selectedTenantSubject.next(null);
  }

  /**
   * Check if user session is valid
   * With HttpOnly cookies, we check for user presence in storage
   * Actual token validity is verified by backend on each request
   * If the token is expired, the backend returns 401 and we trigger refresh
   */
  private hasValidSession(): boolean {
    // With HttpOnly cookies, we can't check token expiration directly
    // We rely on user presence in storage as session indicator
    // Backend validates actual token validity on each request
    const user = this.getUserFromStorage();
    return user !== null;
  }

  /**
   * Handle successful login
   * Tokens are set as HttpOnly cookies by the backend
   * We only store user profile data in localStorage
   */
  private handleLoginSuccess(response: LoginResponse): void {
    // Tokens are set as HttpOnly cookies by backend - no localStorage storage needed
    // Build user object from response data
    const user: User = {
      id: '', // ID not returned in login response, will be set from /auth/me if needed
      username: response.username,
      email: response.email,
      firstName: response.username.split('@')[0], // Extract from username as fallback
      lastName: '',
      fullName: response.username,
      // Use roleDetails if provided by backend, otherwise build from role names
      roles: response.roleDetails || (response.roles || []).map(roleName => ({ id: '', name: roleName } as Role)),
      tenantId: response.tenantIds?.[0] || '', // Use first tenant ID
      tenantIds: response.tenantIds || [],
      active: true,
    };

    // Store user profile for UI display and permission checks
    this.setUser(user);
    this.currentUserSubject.next(user);
    this.isAuthenticatedSubject.next(true);
    this.applyDefaultTenant(user);
    this.startTokenRefreshTimer();
    this.logger.info('Login successful', { username: user.username });
  }

  /**
   * Clear all authentication data
   * Note: HttpOnly cookies are cleared by the backend on logout
   */
  private clearAuthData(): void {
    // Only clear user profile from localStorage
    // HttpOnly cookies are cleared by backend via Set-Cookie with Max-Age=0
    this.removeUser();
    this.clearTenant();
  }

  private applyDefaultTenant(user: User): void {
    const storedTenant = this.getTenantFromStorage();
    const availableTenants = user.tenantIds && user.tenantIds.length > 0 ? user.tenantIds : (user.tenantId ? [user.tenantId] : []);

    if (storedTenant && availableTenants.includes(storedTenant)) {
      this.selectedTenantSubject.next(storedTenant);
      return;
    }

    const fallbackTenant = availableTenants[0] || null;
    if (fallbackTenant) {
      localStorage.setItem(this.TENANT_KEY, fallbackTenant);
    } else {
      localStorage.removeItem(this.TENANT_KEY);
    }
    this.selectedTenantSubject.next(fallbackTenant);
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
          this.logger.error('Token refresh timer failed', error);
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
  tenantIds?: string[];  // Tenant UUID array from gateway
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
  username: string;
  email: string;
  roles: string[];
  tenantIds: string[];
  roleDetails?: Role[];
  mfaEnabled?: boolean;
  message?: string;
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
