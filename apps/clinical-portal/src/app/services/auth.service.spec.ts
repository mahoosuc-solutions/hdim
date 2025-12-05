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
import { of, throwError } from 'rxjs';

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
      post: jest.fn(),
      get: jest.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerMock },
        { provide: ApiService, useValue: apiServiceMock },
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
      // Create a valid JWT token with future expiration
      const validToken = createMockJWT({ exp: Math.floor(Date.now() / 1000) + 3600 });
      const loginResponse = { ...mockLoginResponse, accessToken: validToken };

      apiService.post.mockReturnValue(of(loginResponse));

      service.login('testuser', 'password').subscribe({
        next: (response) => {
          expect(response).toEqual(loginResponse);
          expect(service.getToken()).toBe(validToken);
          expect(service.getRefreshToken()).toBe(mockLoginResponse.refreshToken);
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

    it('should clear session storage completely', () => {
      service.logout();

      expect(localStorage.getItem('healthdata_auth_token')).toBeNull();
      expect(localStorage.getItem('healthdata_refresh_token')).toBeNull();
      expect(localStorage.getItem('healthdata_user')).toBeNull();
    });
  });

  describe('Token Management', () => {
    it('should get token from localStorage', () => {
      const token = 'test-token';
      localStorage.setItem('healthdata_auth_token', token);

      expect(service.getToken()).toBe(token);
    });

    it('should set token in localStorage', () => {
      const token = 'new-token';
      service.setToken(token);

      expect(localStorage.getItem('healthdata_auth_token')).toBe(token);
    });

    it('should remove token from localStorage', () => {
      localStorage.setItem('healthdata_auth_token', 'token');
      service.removeToken();

      expect(localStorage.getItem('healthdata_auth_token')).toBeNull();
    });

    it('should get refresh token from localStorage', () => {
      const token = 'refresh-token';
      localStorage.setItem('healthdata_refresh_token', token);

      expect(service.getRefreshToken()).toBe(token);
    });

    it('should set refresh token in localStorage', () => {
      const token = 'new-refresh-token';
      service.setRefreshToken(token);

      expect(localStorage.getItem('healthdata_refresh_token')).toBe(token);
    });

    it('should remove refresh token from localStorage', () => {
      localStorage.setItem('healthdata_refresh_token', 'token');
      service.removeRefreshToken();

      expect(localStorage.getItem('healthdata_refresh_token')).toBeNull();
    });
  });

  describe('Token Refresh', () => {
    it('should refresh token successfully', (done) => {
      localStorage.setItem('healthdata_refresh_token', 'old-refresh');
      apiService.post.mockReturnValue(of(mockTokenResponse));

      service.refreshToken().subscribe({
        next: (response) => {
          expect(response).toEqual(mockTokenResponse);
          expect(service.getToken()).toBe(mockTokenResponse.accessToken);
          expect(service.getRefreshToken()).toBe(mockTokenResponse.refreshToken);
          done();
        },
        error: () => fail('should not fail'),
      });
    });

    it('should logout on refresh token error', (done) => {
      localStorage.setItem('healthdata_refresh_token', 'old-refresh');
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

    it('should return error if no refresh token available', (done) => {
      service.refreshToken().subscribe({
        next: () => fail('should not succeed'),
        error: (error) => {
          expect(error.message).toBe('No refresh token available');
          done();
        },
      });
    });

    it('should update only access token if refresh token not provided in response', (done) => {
      localStorage.setItem('healthdata_refresh_token', 'old-refresh');
      const responseWithoutRefresh = { ...mockTokenResponse, refreshToken: undefined };
      apiService.post.mockReturnValue(of(responseWithoutRefresh));

      service.refreshToken().subscribe({
        next: () => {
          expect(service.getToken()).toBe(mockTokenResponse.accessToken);
          expect(service.getRefreshToken()).toBe('old-refresh'); // Unchanged
          done();
        },
      });
    });
  });

  describe('Authentication State', () => {
    it('should return false when no token exists', () => {
      expect(service.isAuthenticated()).toBe(false);
    });

    it('should return false when token is expired', () => {
      // Create an expired token (expired 1 hour ago)
      const expiredToken = createMockJWT({ exp: Math.floor(Date.now() / 1000) - 3600 });
      localStorage.setItem('healthdata_auth_token', expiredToken);

      expect(service.isAuthenticated()).toBe(false);
    });

    it('should return true when token is valid', () => {
      // Create a valid token (expires in 1 hour)
      const validToken = createMockJWT({ exp: Math.floor(Date.now() / 1000) + 3600 });
      localStorage.setItem('healthdata_auth_token', validToken);

      expect(service.isAuthenticated()).toBe(true);
    });

    it('should return false when token is malformed', () => {
      localStorage.setItem('healthdata_auth_token', 'invalid-token');

      expect(service.isAuthenticated()).toBe(false);
    });
  });

  describe('Tenant ID Management', () => {
    it('should return tenant ID from current user', (done) => {
      const validToken = createMockJWT({ exp: Math.floor(Date.now() / 1000) + 3600 });
      const loginResponse = { ...mockLoginResponse, accessToken: validToken };
      apiService.post.mockReturnValue(of(loginResponse));

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
      const validToken = createMockJWT({ exp: Math.floor(Date.now() / 1000) + 3600 });
      const loginResponse = {
        ...mockLoginResponse,
        accessToken: validToken,
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
      const validToken = createMockJWT({ exp: Math.floor(Date.now() / 1000) + 3600 });
      const loginResponse = { ...mockLoginResponse, accessToken: validToken };
      apiService.post.mockReturnValue(of(loginResponse));

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
      const validToken = createMockJWT({ exp: Math.floor(Date.now() / 1000) + 3600 });
      const loginResponse = { ...mockLoginResponse, accessToken: validToken };
      apiService.post.mockReturnValue(of(loginResponse));

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
      const validToken = createMockJWT({ exp: Math.floor(Date.now() / 1000) + 3600 });
      const loginResponse = {
        ...mockLoginResponse,
        accessToken: validToken,
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
      const validToken = createMockJWT({ exp: Math.floor(Date.now() / 1000) + 3600 });
      const loginResponse = { ...mockLoginResponse, accessToken: validToken };
      apiService.post.mockReturnValue(of(loginResponse));

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
      const validToken = createMockJWT({ exp: Math.floor(Date.now() / 1000) + 3600 });
      const loginResponse = {
        ...mockLoginResponse,
        accessToken: validToken,
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

      const validToken = createMockJWT({ exp: Math.floor(Date.now() / 1000) + 3600 });
      const loginResponse = { ...mockLoginResponse, accessToken: validToken };
      apiService.post.mockReturnValue(of(loginResponse));
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

      const validToken = createMockJWT({ exp: Math.floor(Date.now() / 1000) + 3600 });
      const loginResponse = { ...mockLoginResponse, accessToken: validToken };
      apiService.post.mockReturnValue(of(loginResponse));
      service.login('testuser', 'password').subscribe();
    });
  });
});

/**
 * Helper function to create a mock JWT token with custom payload
 */
function createMockJWT(payload: any): string {
  const header = { alg: 'HS256', typ: 'JWT' };
  const encodedHeader = btoa(JSON.stringify(header));
  const encodedPayload = btoa(JSON.stringify(payload));
  const signature = 'mock-signature';
  return `${encodedHeader}.${encodedPayload}.${signature}`;
}
