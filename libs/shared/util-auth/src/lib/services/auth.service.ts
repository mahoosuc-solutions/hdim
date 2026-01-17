import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, throwError, of } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';

/**
 * User interface
 */
export interface User {
  id: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  tenantId?: string;
  tenantIds?: string[];
  roles: Role[];
  mfaEnabled?: boolean;
}

export interface Role {
  id: string;
  name: string;
  permissions?: Permission[];
}

export interface Permission {
  id: string;
  name: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  user: User;
  message?: string;
}

export interface MfaRequiredResponse {
  mfaRequired: true;
  mfaToken: string;
}

/**
 * Authentication Service (Shared across MFEs)
 *
 * SECURITY (HIPAA Compliant):
 * - JWT tokens stored in HttpOnly cookies (XSS protected)
 * - Cookies automatically sent with withCredentials: true
 * - User profile stored in localStorage (not sensitive)
 * - Token refresh handled via cookie rotation
 */
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly USER_KEY = 'healthdata_user';
  private readonly TOKEN_REFRESH_INTERVAL = 5 * 60 * 1000; // 5 minutes

  private http = inject(HttpClient);
  private router = inject(Router);

  private currentUserSubject = new BehaviorSubject<User | null>(
    this.getUserFromStorage()
  );
  public currentUser$ = this.currentUserSubject.asObservable();

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(
    this.hasValidSession()
  );
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  private tokenRefreshTimer: any;

  constructor() {
    if (this.isAuthenticated()) {
      this.startTokenRefresh();
    }
  }

  /**
   * Login with username and password
   */
  login(
    username: string,
    password: string
  ): Observable<LoginResponse | MfaRequiredResponse> {
    return this.http
      .post<LoginResponse | MfaRequiredResponse>('/auth/login', {
        username,
        password,
      })
      .pipe(
        tap((response) => {
          if (!('mfaRequired' in response)) {
            this.handleLoginSuccess(response as LoginResponse);
          }
        }),
        catchError((error) => throwError(() => error))
      );
  }

  /**
   * Logout current user
   */
  logout(): void {
    this.stopTokenRefresh();

    this.http.post('/auth/logout', {}).subscribe({
      next: () => console.log('Logout successful'),
      error: (err) => console.warn('Logout failed', err),
    });

    this.clearAuthData();
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
    this.router.navigate(['/login']);
  }

  /**
   * Refresh authentication token
   */
  refreshToken(): Observable<any> {
    return this.http.post('/auth/refresh', {}).pipe(
      tap(() => {
        this.isAuthenticatedSubject.next(true);
      }),
      catchError((error) => {
        this.logout();
        return throwError(() => error);
      })
    );
  }

  /**
   * Get current user
   */
  getCurrentUser(): Observable<User> {
    const user = this.currentUserSubject.value;
    if (user) {
      return of(user);
    }

    return this.http.get<User>('/auth/me').pipe(
      tap((user) => {
        this.setUser(user);
        this.currentUserSubject.next(user);
      })
    );
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return this.hasValidSession();
  }

  /**
   * Get tenant ID from current user
   */
  getTenantId(): string | null {
    const user = this.currentUserSubject.value;
    if (user?.tenantIds && user.tenantIds.length > 0) {
      return user.tenantIds[0];
    }
    return user?.tenantId || null;
  }

  /**
   * Get current user value synchronously
   */
  get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  /**
   * Check if user has specific role
   */
  hasRole(role: string): boolean {
    const user = this.currentUserSubject.value;
    if (!user || !user.roles) return false;
    return user.roles.some((r) => r.name === role || r.name === 'ADMIN');
  }

  /**
   * Check if user has any of the specified roles
   */
  hasAnyRole(roles: string[]): boolean {
    return roles.some((role) => this.hasRole(role));
  }

  /**
   * Check if user has specific permission
   */
  hasPermission(permission: string): boolean {
    const user = this.currentUserSubject.value;
    if (!user) return false;
    if (this.hasRole('ADMIN')) return true;
    return user.roles.some((role) =>
      role.permissions?.some((p) => p.name === permission)
    );
  }

  // Private helper methods

  private handleLoginSuccess(response: LoginResponse): void {
    this.setUser(response.user);
    this.currentUserSubject.next(response.user);
    this.isAuthenticatedSubject.next(true);
    this.startTokenRefresh();
  }

  private getUserFromStorage(): User | null {
    try {
      const userJson = localStorage.getItem(this.USER_KEY);
      return userJson ? JSON.parse(userJson) : null;
    } catch {
      return null;
    }
  }

  private setUser(user: User): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  private clearAuthData(): void {
    localStorage.removeItem(this.USER_KEY);
  }

  private hasValidSession(): boolean {
    return this.getUserFromStorage() !== null;
  }

  private startTokenRefresh(): void {
    this.stopTokenRefresh();
    this.tokenRefreshTimer = setInterval(() => {
      this.refreshToken().subscribe();
    }, this.TOKEN_REFRESH_INTERVAL);
  }

  private stopTokenRefresh(): void {
    if (this.tokenRefreshTimer) {
      clearInterval(this.tokenRefreshTimer);
      this.tokenRefreshTimer = null;
    }
  }
}
