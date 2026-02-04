import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { PLATFORM_ID } from '@angular/core';
import { AuthGuard } from './auth.guard';

// Define environment mock object that jest.mock can reference
let mockDemoMode = true;

// Mock the environment import - must be at top level
jest.mock('../../environments/environment', () => ({
  get environment() {
    return {
      features: {
        get demoMode() {
          return mockDemoMode;
        },
      },
    };
  },
}));

describe('AuthGuard - Admin Portal', () => {
  let guard: AuthGuard;
  let router: Router;
  let mockLocalStorage: { [key: string]: string };

  beforeEach(() => {
    // Reset demo mode to true (development) by default
    mockDemoMode = true;

    // Reset mock localStorage
    mockLocalStorage = {};

    // Mock localStorage
    Object.defineProperty(window, 'localStorage', {
      value: {
        getItem: jest.fn((key: string) => mockLocalStorage[key] || null),
        setItem: jest.fn((key: string, value: string) => {
          mockLocalStorage[key] = value;
        }),
        removeItem: jest.fn((key: string) => {
          delete mockLocalStorage[key];
        }),
        clear: jest.fn(() => {
          mockLocalStorage = {};
        }),
      },
      writable: true,
    });

    TestBed.configureTestingModule({
      providers: [
        AuthGuard,
        {
          provide: Router,
          useValue: {
            navigate: jest.fn(),
          },
        },
        {
          provide: PLATFORM_ID,
          useValue: 'browser', // Simulate browser environment
        },
      ],
    });

    guard = TestBed.inject(AuthGuard);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('Browser Environment', () => {
    describe('With Valid Token', () => {
      it('should allow access when auth_token exists', () => {
        mockLocalStorage['auth_token'] = 'valid-jwt-token';

        const result = guard.canActivate();

        expect(result).toBe(true);
        expect(router.navigate).not.toHaveBeenCalled();
      });

      it('should not modify localStorage when token exists', () => {
        mockLocalStorage['auth_token'] = 'existing-token';
        mockLocalStorage['user_role'] = 'EVALUATOR';

        guard.canActivate();

        expect(localStorage.setItem).not.toHaveBeenCalled();
      });
    });

    describe('Without Token - Demo Mode Enabled', () => {
      beforeEach(() => {
        mockDemoMode = true;
      });

      it('should create demo token when no token exists', () => {
        const result = guard.canActivate();

        expect(result).toBe(true);
        expect(localStorage.setItem).toHaveBeenCalledWith(
          'auth_token',
          'demo-admin-token'
        );
        expect(localStorage.setItem).toHaveBeenCalledWith('user_role', 'ADMIN');
      });

      it('should grant ADMIN role in demo mode', () => {
        guard.canActivate();

        expect(mockLocalStorage['user_role']).toBe('ADMIN');
      });
    });

    describe('Without Token - Demo Mode Disabled (Production)', () => {
      beforeEach(() => {
        mockDemoMode = false;
      });

      it('should redirect to login when no token and demo mode disabled', () => {
        const result = guard.canActivate();

        expect(result).toBe(false);
        expect(router.navigate).toHaveBeenCalledWith(['/sales/login']);
      });

      it('should not create demo token in production mode', () => {
        guard.canActivate();

        expect(localStorage.setItem).not.toHaveBeenCalled();
        expect(mockLocalStorage['auth_token']).toBeUndefined();
      });

      it('should deny access when redirecting to login', () => {
        const result = guard.canActivate();

        expect(result).toBe(false);
      });
    });
  });

  describe('SSR Environment (Server-Side Rendering)', () => {
    beforeEach(() => {
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          AuthGuard,
          {
            provide: Router,
            useValue: {
              navigate: jest.fn(),
            },
          },
          {
            provide: PLATFORM_ID,
            useValue: 'server', // Simulate SSR environment
          },
        ],
      });

      guard = TestBed.inject(AuthGuard);
      router = TestBed.inject(Router);
    });

    it('should allow access in SSR environment (defers to client-side check)', () => {
      const result = guard.canActivate();

      expect(result).toBe(true);
    });

    it('should not access localStorage in SSR environment', () => {
      guard.canActivate();

      expect(localStorage.getItem).not.toHaveBeenCalled();
      expect(localStorage.setItem).not.toHaveBeenCalled();
    });

    it('should not redirect in SSR environment', () => {
      mockDemoMode = false;

      guard.canActivate();

      expect(router.navigate).not.toHaveBeenCalled();
    });
  });

  describe('Security Scenarios', () => {
    beforeEach(() => {
      mockDemoMode = false;
    });

    it('should protect admin routes in production mode', () => {
      // Simulate production: no token, demo mode off
      const result = guard.canActivate();

      expect(result).toBe(false);
      expect(router.navigate).toHaveBeenCalledWith(['/sales/login']);
    });

    it('should allow access with any token in production (token validation is TODO)', () => {
      mockLocalStorage['auth_token'] = 'any-token-value';

      const result = guard.canActivate();

      expect(result).toBe(true);
      expect(router.navigate).not.toHaveBeenCalled();
    });
  });
});
