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
 * - JWT token management
 * - Token refresh
 * - Role-based authorization
 * - Permission checking
 * - Auto-logout on token expiration
 * - Authentication state management
 *
 * SECURITY NOTE: Current implementation stores JWT in localStorage which is
 * vulnerable to XSS attacks. Production deployments should migrate to HttpOnly
 * cookies with the following changes:
 * 1. Backend login endpoint sets HttpOnly, Secure, SameSite=Strict cookies
 * 2. Backend JWT filter reads token from cookies instead of Authorization header
 * 3. Frontend uses withCredentials: true for all authenticated requests
 * 4. Frontend removes direct token access - authentication state only
 *
 * TODO: Implement HttpOnly cookie-based JWT storage (HIGH PRIORITY for HIPAA)
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
   */
  login(username: string, password: string): Observable<LoginResponse> {
    const url = '/auth/login';
    const body: LoginRequest = { username, password };

    return this.apiService.post<LoginResponse>(url, body).pipe(
      tap((response) => {
        this.handleLoginSuccess(response);
      }),
      catchError((error) => {
        console.error('Login error:', error);
        return throwError(() => error);
      })
    );
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
   */
  private hasValidToken(): boolean {
    const token = this.getToken();
    if (!token) return false;

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
