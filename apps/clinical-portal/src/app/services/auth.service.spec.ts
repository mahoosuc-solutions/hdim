import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService, User, LoginResponse, TokenResponse } from './auth.service';
import { ApiService } from './api.service';
import { LoggerService } from './logger.service';
import { of, throwError } from 'rxjs';

const mockLoggerService = {
  withContext: jest.fn().mockReturnValue({
    debug: jest.fn(),
    info: jest.fn(),
    warn: jest.fn(),
    error: jest.fn(),
  }),
  debug: jest.fn(),
  info: jest.fn(),
  warn: jest.fn(),
  error: jest.fn(),
};

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let router: jest.Mocked<Router>;
  let apiService: jest.Mocked<ApiService>;

  const mockUser: User = {
    id: '123',
    username: 'testuser',
    email: 'test@example.com',
    firstName: 'Test',
    lastName: 'User',
    fullName: 'Test User',
    roles: [
      {
        id: 'role1',
        name: 'USER',
        permissions: [
          {
            id: 'perm1',
            name: 'READ_PATIENTS',
            resource: 'patients',
            action: 'read',
          },
        ],
      },
    ],
    tenantId: 'tenant-123',
    active: true,
  };

  const mockLoginResponse: LoginResponse = {
    accessToken: 'mock-access-token',
    refreshToken: 'mock-refresh-token',
    tokenType: 'Bearer',
    expiresIn: 3600,
    user: mockUser,
  };

  const mockTokenResponse: TokenResponse = {
    accessToken: 'new-access-token',
    refreshToken: 'new-refresh-token',
    tokenType: 'Bearer',
    expiresIn: 3600,
  };

  beforeEach(() => {
    const routerMock = {
      navigate: jest.fn(),
      url: '/dashboard',
    };

    const apiServiceMock = {
      post: jest.fn().mockReturnValue(of({})), // Default mock for API calls
      get: jest.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerMock },
        { provide: ApiService, useValue: apiServiceMock },
        { provide: LoggerService, useValue: mockLoggerService },
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router) as jest.Mocked<Router>;
    apiService = TestBed.inject(ApiService) as jest.Mocked<ApiService>;

    // Clear localStorage before each test
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('Login Functionality', () => {
    it('should login successfully and store auth data', (done) => {
      // With HttpOnly cookies, tokens are not accessible from JavaScript
      // getToken() intentionally returns null for XSS protection
      apiService.post.mockReturnValue(of(mockLoginResponse));

      service.login('testuser', 'password').subscribe({
        next: (response) => {
          expect(response).toEqual(mockLoginResponse);
          // Tokens are stored in HttpOnly cookies, not accessible from JS
          expect(service.getToken()).toBeNull();
          // User is stored and accessible
          expect(service.isAuthenticated()).toBe(true);
          expect(service.currentUserValue).toEqual(mockUser);
          done();
        },
        error: () => fail('should not fail'),
      });
    });

    it('should handle login error', (done) => {
      const error = { status: 401, message: 'Invalid credentials' };
      apiService.post.mockReturnValue(throwError(() => error));

      service.login('testuser', 'wrongpassword').subscribe({
        next: () => fail('should not succeed'),
        error: (err) => {
          expect(err).toEqual(error);
          expect(service.isAuthenticated()).toBe(false);
          done();
        },
      });
    });

    it('should emit authentication state on successful login', (done) => {
      apiService.post.mockReturnValue(of(mockLoginResponse));

      service.isAuthenticated$.subscribe((isAuth) => {
        if (isAuth) {
          expect(isAuth).toBe(true);
          done();
        }
      });

      service.login('testuser', 'password').subscribe();
    });

    it('should emit current user on successful login', (done) => {
      apiService.post.mockReturnValue(of(mockLoginResponse));

      let emissionCount = 0;
      service.currentUser$.subscribe((user) => {
        emissionCount++;
        if (emissionCount === 2) {
          // First emission is null, second is the user
          expect(user).toEqual(mockUser);
          done();
        }
      });

      service.login('testuser', 'password').subscribe();
    });
  });

  describe('Logout Functionality', () => {
    beforeEach(() => {
      // Set up authenticated state
      localStorage.setItem('healthdata_auth_token', 'mock-token');
      localStorage.setItem('healthdata_refresh_token', 'mock-refresh');
      localStorage.setItem('healthdata_user', JSON.stringify(mockUser));
      // Mock the logout API call (to clear HttpOnly cookies on backend)
      apiService.post.mockReturnValue(of({}));
    });

    it('should clear all auth data on logout', () => {
      service.logout();

      expect(service.getToken()).toBeNull();
      expect(service.getRefreshToken()).toBeNull();
      expect(service.currentUserValue).toBeNull();
      expect(service.isAuthenticated()).toBe(false);
    });

    it('should redirect to login page on logout', () => {
      service.logout();

      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('should emit authentication state on logout', (done) => {
      let emissionCount = 0;
      service.isAuthenticated$.subscribe((isAuth) => {
        emissionCount++;
        if (emissionCount === 2) {
          // First emission is true (from storage), second is false
          expect(isAuth).toBe(false);
          done();
        }
      });

      service.logout();
    });

    it('should emit null user on logout', (done) => {
      let emissionCount = 0;
      service.currentUser$.subscribe((user) => {
        emissionCount++;
        if (emissionCount === 2) {
          // First emission is stored user, second is null
          expect(user).toBeNull();
          done();
        }
      });

      service.logout();
    });

    it('should clear user from session storage', () => {
      // With HttpOnly cookies, only user profile is stored in localStorage
      // Tokens are in HttpOnly cookies cleared by backend
      service.logout();

      expect(localStorage.getItem('healthdata_user')).toBeNull();
    });
  });

  describe('Token Management', () => {
    /**
     * With HttpOnly cookies, token management methods are either no-ops
     * or return null for security (XSS protection). Tokens are managed
     * by the backend via HttpOnly cookies, not accessible from JavaScript.
     */
    it('should return null from getToken (HttpOnly cookies)', () => {
      // Even if we try to set something in localStorage, getToken returns null
      localStorage.setItem('healthdata_auth_token', 'test-token');
      expect(service.getToken()).toBeNull();
    });

    it('should be a no-op for setToken (HttpOnly cookies)', () => {
      // setToken is a no-op - tokens are set by backend as HttpOnly cookies
      service.setToken('new-token');
      // The method doesn't throw, it's just a no-op for backwards compatibility
      expect(service.getToken()).toBeNull();
    });

    it('should handle removeToken gracefully', () => {
      localStorage.setItem('healthdata_auth_token', 'token');
      service.removeToken();
      // The token removal is a no-op but should not throw
      expect(true).toBe(true);
    });

    it('should return null from getRefreshToken (HttpOnly cookies)', () => {
      localStorage.setItem('healthdata_refresh_token', 'refresh-token');
      expect(service.getRefreshToken()).toBeNull();
    });

    it('should be a no-op for setRefreshToken (HttpOnly cookies)', () => {
      service.setRefreshToken('new-refresh-token');
      expect(service.getRefreshToken()).toBeNull();
    });

    it('should handle removeRefreshToken gracefully', () => {
      localStorage.setItem('healthdata_refresh_token', 'token');
      service.removeRefreshToken();
      expect(true).toBe(true);
    });
  });

  describe('Token Refresh', () => {
    /**
     * With HttpOnly cookies, token refresh is handled via cookie exchange.
     * The refresh token is sent as an HttpOnly cookie by the browser,
     * and new tokens are set as HttpOnly cookies by the backend.
     */
    it('should refresh token successfully via cookie exchange', (done) => {
      // Store user to indicate valid session
      localStorage.setItem('healthdata_user', JSON.stringify(mockUser));
      apiService.post.mockReturnValue(of(mockTokenResponse));

      service.refreshToken().subscribe({
        next: (response) => {
          expect(response).toEqual(mockTokenResponse);
          // Tokens are in HttpOnly cookies, not accessible from JS
          expect(service.getToken()).toBeNull();
          expect(service.getRefreshToken()).toBeNull();
          done();
        },
        error: () => fail('should not fail'),
      });
    });

    it('should logout on refresh token error', (done) => {
      localStorage.setItem('healthdata_user', JSON.stringify(mockUser));
      const error = { status: 401, message: 'Invalid token' };
      apiService.post.mockReturnValue(throwError(() => error));

      service.refreshToken().subscribe({
        next: () => fail('should not succeed'),
        error: () => {
          expect(router.navigate).toHaveBeenCalledWith(['/login']);
          done();
        },
      });
    });

    it('should return error if no valid session exists', (done) => {
      // No user in storage = no valid session
      // With HttpOnly cookies, the API call will fail with 401
      const error = { status: 401, message: 'Unauthorized' };
      apiService.post.mockReturnValue(throwError(() => error));

      service.refreshToken().subscribe({
        next: () => fail('should not succeed'),
        error: (err) => {
          expect(err.status).toBe(401);
          done();
        },
      });
    });

    it('should handle response without explicit refresh token', (done) => {
      localStorage.setItem('healthdata_user', JSON.stringify(mockUser));
      const responseWithoutRefresh = { ...mockTokenResponse, refreshToken: undefined };
      apiService.post.mockReturnValue(of(responseWithoutRefresh));

      service.refreshToken().subscribe({
        next: () => {
          // Tokens are managed via HttpOnly cookies
          expect(service.getToken()).toBeNull();
          expect(service.getRefreshToken()).toBeNull();
          done();
        },
      });
    });
  });

  describe('Authentication State', () => {
    /**
     * With HttpOnly cookies, authentication state is determined by
     * the presence of a valid user in storage, not by token presence.
     * The backend validates actual token validity on each request.
     */
    it('should return false when no user exists in storage', () => {
      expect(service.isAuthenticated()).toBe(false);
    });

    it('should return true when user exists in storage', () => {
      localStorage.setItem('healthdata_user', JSON.stringify(mockUser));
      // Need to create a new service instance to pick up the stored user
      const newService = new AuthService(
        TestBed.inject(HttpClient),
        TestBed.inject(ApiService),
        TestBed.inject(Router),
        mockLoggerService as unknown as LoggerService
      );
      expect(newService.isAuthenticated()).toBe(true);
    });

    it('should return false when user storage is invalid JSON', () => {
      localStorage.setItem('healthdata_user', 'invalid-json');
      const newService = new AuthService(
        TestBed.inject(HttpClient),
        TestBed.inject(ApiService),
        TestBed.inject(Router),
        mockLoggerService as unknown as LoggerService
      );
      expect(newService.isAuthenticated()).toBe(false);
    });

    it('should return false after logout clears user', () => {
      localStorage.setItem('healthdata_user', JSON.stringify(mockUser));
      // Logout clears the user
      service.logout();
      expect(service.isAuthenticated()).toBe(false);
    });
  });

  describe('Tenant ID Management', () => {
    it('should return tenant ID from current user', (done) => {
      apiService.post.mockReturnValue(of(mockLoginResponse));

      service.login('testuser', 'password').subscribe({
        next: () => {
          expect(service.getTenantId()).toBe('tenant-123');
          done();
        },
      });
    });

    it('should return null when no user is logged in', () => {
      expect(service.getTenantId()).toBeNull();
    });

    it('should return null when user has no tenant ID', (done) => {
      const userWithoutTenant = { ...mockUser, tenantId: undefined };
      const loginResponse = {
        ...mockLoginResponse,
        user: userWithoutTenant,
      };
      apiService.post.mockReturnValue(of(loginResponse));

      service.login('testuser', 'password').subscribe({
        next: () => {
          expect(service.getTenantId()).toBeNull();
          done();
        },
      });
    });
  });

  describe('Current User', () => {
    it('should get current user from API', (done) => {
      apiService.get.mockReturnValue(of(mockUser));

      service.getCurrentUser().subscribe({
        next: (user) => {
          expect(user).toEqual(mockUser);
          expect(service.currentUserValue).toEqual(mockUser);
          done();
        },
      });
    });

    it('should return cached user if available', (done) => {
      apiService.post.mockReturnValue(of(mockLoginResponse));

      // Login first to cache the user
      service.login('testuser', 'password').subscribe({
        next: () => {
          // Now get current user - should use cached value
          service.getCurrentUser().subscribe({
            next: (user) => {
              expect(user).toEqual(mockUser);
              // API get should only have been called once (by login), not by getCurrentUser
              expect(apiService.get).not.toHaveBeenCalled();
              done();
            },
          });
        },
      });
    });

    it('should handle error when fetching current user', (done) => {
      const error = { status: 401, message: 'Unauthorized' };
      apiService.get.mockReturnValue(throwError(() => error));

      service.getCurrentUser().subscribe({
        next: () => fail('should not succeed'),
        error: (err) => {
          expect(err).toEqual(error);
          done();
        },
      });
    });
  });

  describe('Role-Based Authorization', () => {
    beforeEach((done) => {
      apiService.post.mockReturnValue(of(mockLoginResponse));

      service.login('testuser', 'password').subscribe({
        next: () => done(),
      });
    });

    it('should check if user has specific role', () => {
      expect(service.hasRole('USER')).toBe(true);
      expect(service.hasRole('ADMIN')).toBe(false);
    });

    it('should return false for role check when not authenticated', () => {
      service.logout();
      expect(service.hasRole('USER')).toBe(false);
    });

    it('should check if user has any of specified roles', () => {
      expect(service.hasAnyRole(['USER', 'ADMIN'])).toBe(true);
      expect(service.hasAnyRole(['ADMIN', 'SUPERUSER'])).toBe(false);
    });

    it('should check if user has all specified roles', () => {
      expect(service.hasAllRoles(['USER'])).toBe(true);
      expect(service.hasAllRoles(['USER', 'ADMIN'])).toBe(false);
    });

    it('should grant ADMIN role all other roles', (done) => {
      const adminUser = {
        ...mockUser,
        roles: [{ id: 'admin', name: 'ADMIN', permissions: [] }],
      };
      const loginResponse = {
        ...mockLoginResponse,
        user: adminUser,
      };

      // Create a new service instance to clear previous state
      service.logout();
      apiService.post.mockReturnValue(of(loginResponse));

      service.login('admin', 'password').subscribe({
        next: () => {
          expect(service.hasRole('USER')).toBe(true);
          expect(service.hasRole('ADMIN')).toBe(true);
          done();
        },
      });
    });
  });

  describe('Permission-Based Authorization', () => {
    beforeEach((done) => {
      apiService.post.mockReturnValue(of(mockLoginResponse));

      service.login('testuser', 'password').subscribe({
        next: () => done(),
      });
    });

    it('should check if user has specific permission', () => {
      expect(service.hasPermission('READ_PATIENTS')).toBe(true);
      expect(service.hasPermission('DELETE_PATIENTS')).toBe(false);
    });

    it('should return false for permission check when not authenticated', () => {
      service.logout();
      expect(service.hasPermission('READ_PATIENTS')).toBe(false);
    });

    it('should check if user has any of specified permissions', () => {
      expect(service.hasAnyPermission(['READ_PATIENTS', 'WRITE_PATIENTS'])).toBe(true);
      expect(service.hasAnyPermission(['DELETE_PATIENTS', 'ADMIN_ACCESS'])).toBe(false);
    });

    it('should grant ADMIN role all permissions', (done) => {
      const adminUser = {
        ...mockUser,
        roles: [{ id: 'admin', name: 'ADMIN', permissions: [] }],
      };
      const loginResponse = {
        ...mockLoginResponse,
        user: adminUser,
      };

      service.logout();
      apiService.post.mockReturnValue(of(loginResponse));

      service.login('admin', 'password').subscribe({
        next: () => {
          expect(service.hasPermission('ANY_PERMISSION')).toBe(true);
          done();
        },
      });
    });
  });

  describe('Observable Streams', () => {
    it('should emit authentication state changes', (done) => {
      const states: boolean[] = [];

      service.isAuthenticated$.subscribe((isAuth) => {
        states.push(isAuth);
        if (states.length === 2) {
          expect(states).toEqual([false, true]);
          done();
        }
      });

      apiService.post.mockReturnValue(of(mockLoginResponse));
      service.login('testuser', 'password').subscribe();
    });

    it('should emit current user changes', (done) => {
      const users: (User | null)[] = [];

      service.currentUser$.subscribe((user) => {
        users.push(user);
        if (users.length === 2) {
          expect(users[0]).toBeNull();
          expect(users[1]).toEqual(mockUser);
          done();
        }
      });

      apiService.post.mockReturnValue(of(mockLoginResponse));
      service.login('testuser', 'password').subscribe();
    });
  });
});
