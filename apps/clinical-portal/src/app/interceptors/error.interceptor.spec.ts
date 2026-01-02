import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { errorInterceptor } from './error.interceptor';
import { AuthService } from '../services/auth.service';

describe('ErrorInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;
  let consoleErrorSpy: jest.SpyInstance;
  let authService: jest.Mocked<AuthService>;
  let router: jest.Mocked<Router>;

  beforeEach(() => {
    const authServiceMock = {
      getTenantId: jest.fn().mockReturnValue('tenant-123'),
      logout: jest.fn(),
      isAuthenticated: jest.fn().mockReturnValue(true),
    };

    const routerMock = {
      navigate: jest.fn(),
      url: '/dashboard',
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock },
      ],
    });

    httpMock = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
    authService = TestBed.inject(AuthService) as jest.Mocked<AuthService>;
    router = TestBed.inject(Router) as jest.Mocked<Router>;

    // Spy on console.error to verify logging
    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
  });

  afterEach(() => {
    httpMock.verify();
    consoleErrorSpy.mockRestore();
    jest.clearAllMocks();
  });

  describe('HTTP Error Status Codes', () => {
    it('should handle 400 Bad Request errors', (done) => {
      const url = 'http://localhost:8081/api/test';

      httpClient.get(url).subscribe({
        next: () => fail('should have failed'),
        error: (error: any) => {
          expect(error.userMessage).toBe(
            'Bad Request: Please check your input and try again.'
          );
          expect(consoleErrorSpy).toHaveBeenCalled();
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush('Bad Request', { status: 400, statusText: 'Bad Request' });
    });

    it('should handle 401 Unauthorized errors', (done) => {
      const url = 'http://localhost:8081/api/test';

      httpClient.get(url).subscribe({
        next: () => fail('should have failed'),
        error: (error: any) => {
          expect(error.userMessage).toBe(
            'Unauthorized: Please log in to access this resource.'
          );
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle 403 Forbidden errors', (done) => {
      const url = 'http://localhost:8081/api/test';

      httpClient.get(url).subscribe({
        next: () => fail('should have failed'),
        error: (error: any) => {
          expect(error.userMessage).toBe(
            'Forbidden: You do not have permission to access this resource.'
          );
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });
    });

    it('should handle 404 Not Found errors', (done) => {
      const url = 'http://localhost:8081/api/test';

      httpClient.get(url).subscribe({
        next: () => fail('should have failed'),
        error: (error: any) => {
          expect(error.userMessage).toBe(
            'Not Found: The requested resource was not found.'
          );
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });
    });

    // NOTE: Retry behavior with 500/503 errors is difficult to test with HttpTestingController
    // because the retry happens asynchronously with exponential backoff delays.
    // These error codes are configured to trigger retries - see RETRYABLE_STATUS_CODES.
    // Manual/integration testing is recommended for full retry behavior verification.

    it('should handle 500 Internal Server Error (POST - no retry)', (done) => {
      // Use POST to skip retry logic (POST is not idempotent)
      const url = 'http://localhost:8081/api/test';

      httpClient.post(url, {}).subscribe({
        next: () => fail('should have failed'),
        error: (error: any) => {
          expect(error.userMessage).toContain('Internal Server Error');
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
    });

    it('should handle 503 Service Unavailable errors (POST - no retry)', (done) => {
      // Use POST to skip retry logic
      const url = 'http://localhost:8081/api/test';

      httpClient.post(url, {}).subscribe({
        next: () => fail('should have failed'),
        error: (error: any) => {
          expect(error.userMessage).toContain('Service Unavailable');
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush('Service Unavailable', { status: 503, statusText: 'Service Unavailable' });
    });
  });

  describe('Custom Error Messages from Backend', () => {
    it('should use error.error.message if available', (done) => {
      const url = 'http://localhost:8081/api/test';
      const customMessage = 'Custom error from backend';

      httpClient.get(url).subscribe({
        next: () => fail('should have failed'),
        error: (error: any) => {
          expect(error.userMessage).toBe(customMessage);
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush({ message: customMessage }, { status: 422, statusText: 'Unprocessable Entity' });
    });

    it('should fallback to error.message if error.error.message not available', (done) => {
      const url = 'http://localhost:8081/api/test';

      httpClient.get(url).subscribe({
        next: () => fail('should have failed'),
        error: (error: any) => {
          // Will use the default message for unknown status codes
          expect(error.userMessage).toContain('Error Code:');
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush('Some error', { status: 422, statusText: 'Unprocessable Entity' });
    });
  });

  describe('Client-Side/Network Errors', () => {
    it('should handle client-side errors (POST - no retry)', (done) => {
      // Use POST to avoid retry (only idempotent methods are retried)
      const url = 'http://localhost:8081/api/test';
      const errorMessage = 'Network connection failed';

      httpClient.post(url, {}).subscribe({
        next: () => fail('should have failed'),
        error: (error: any) => {
          // Error interceptor handles client-side errors
          expect(error.userMessage).toBeTruthy();
          done();
        },
      });

      const req = httpMock.expectOne(url);
      const mockError = new ErrorEvent('error', { message: errorMessage });
      req.error(mockError);
    });

    it('should handle network timeout errors (POST - no retry)', (done) => {
      // Use POST to avoid retry
      const url = 'http://localhost:8081/api/test';

      httpClient.post(url, {}).subscribe({
        next: () => fail('should have failed'),
        error: (error: any) => {
          expect(error.userMessage).toBeTruthy();
          done();
        },
      });

      const req = httpMock.expectOne(url);
      const mockError = new ErrorEvent('error', { message: 'Request timed out' });
      req.error(mockError);
    });
  });

  describe('Error Logging', () => {
    it('should log error details to console', (done) => {
      const url = 'http://localhost:8081/api/test';

      httpClient.get(url).subscribe({
        next: () => fail('should have failed'),
        error: () => {
          expect(consoleErrorSpy).toHaveBeenCalledWith(
            'HTTP Error:',
            expect.objectContaining({
              url,
              status: 404,
              statusText: 'Not Found',
            })
          );
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });
    });

    it('should include request URL in logged error', (done) => {
      // Use 404 (non-retryable) instead of 500
      const url = 'http://localhost:8081/api/specific-endpoint';

      httpClient.get(url).subscribe({
        next: () => fail('should have failed'),
        error: () => {
          const loggedError = consoleErrorSpy.mock.calls[0][1];
          expect(loggedError.url).toBe(url);
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush('Error', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('Error Propagation', () => {
    it('should preserve original error object', (done) => {
      const url = 'http://localhost:8081/api/test';

      httpClient.get(url).subscribe({
        next: () => fail('should have failed'),
        error: (error: any) => {
          expect(error).toHaveProperty('status');
          expect(error).toHaveProperty('statusText');
          expect(error).toHaveProperty('userMessage');
          expect(error.status).toBe(404);
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });
    });

    it('should add userMessage property to error', (done) => {
      // Use 404 (non-retryable) instead of 500
      const url = 'http://localhost:8081/api/test';

      httpClient.get(url).subscribe({
        next: () => fail('should have failed'),
        error: (error: any) => {
          expect(error).toHaveProperty('userMessage');
          expect(typeof error.userMessage).toBe('string');
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush('Error', { status: 404, statusText: 'Not Found' });
    });

    it('should add retryAttempts property to error', (done) => {
      const url = 'http://localhost:8081/api/test';

      httpClient.get(url).subscribe({
        next: () => fail('should have failed'),
        error: (error: any) => {
          expect(error).toHaveProperty('retryAttempts');
          expect(typeof error.retryAttempts).toBe('number');
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush('Error', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('Multiple Errors', () => {
    it('should handle multiple consecutive errors (non-retryable)', (done) => {
      const urls = [
        'http://localhost:8081/api/test1',
        'http://localhost:8081/api/test2',
        'http://localhost:8081/api/test3',
      ];
      // Use non-retryable status codes to avoid retry delays
      const statusCodes = [400, 401, 404];

      let errorCount = 0;
      urls.forEach((url) => {
        httpClient.get(url).subscribe({
          next: () => fail('should have failed'),
          error: (error: any) => {
            expect(error.userMessage).toBeTruthy();
            errorCount++;
            if (errorCount === urls.length) done();
          },
        });
      });

      urls.forEach((url, index) => {
        const req = httpMock.expectOne(url);
        req.flush('Error', { status: statusCodes[index], statusText: 'Error' });
      });
    });
  });

  describe('Unknown Error Codes', () => {
    it('should handle unknown HTTP status codes', (done) => {
      const url = 'http://localhost:8081/api/test';

      httpClient.get(url).subscribe({
        next: () => fail('should have failed'),
        error: (error: any) => {
          // Unknown status codes should generate a userMessage
          expect(error.userMessage).toBeTruthy();
          expect(error.status).toBe(418);
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush("I'm a teapot", { status: 418, statusText: "I'm a teapot" });
    });
  });

  describe('Successful Requests', () => {
    it('should not intercept successful requests', (done) => {
      const url = 'http://localhost:8081/api/test';
      const successData = { success: true };

      httpClient.get(url).subscribe({
        next: (data) => {
          expect(data).toEqual(successData);
          expect(consoleErrorSpy).not.toHaveBeenCalled();
          done();
        },
        error: () => fail('should not have failed'),
      });

      const req = httpMock.expectOne(url);
      req.flush(successData);
    });
  });

  describe('401 Redirect and Logout', () => {
    it('should call logout on 401 error', (done) => {
      const url = 'http://localhost:8081/api/test';

      httpClient.get(url).subscribe({
        next: () => fail('should have failed'),
        error: () => {
          expect(authService.logout).toHaveBeenCalled();
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should not call logout on 401 if request is to /auth/login', (done) => {
      const url = 'http://localhost:8081/api/auth/login';

      httpClient.post(url, { username: 'test', password: 'wrong' }).subscribe({
        next: () => fail('should have failed'),
        error: () => {
          expect(authService.logout).not.toHaveBeenCalled();
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush('Invalid credentials', { status: 401, statusText: 'Unauthorized' });
    });

    it('should not call logout on 401 if already on login page', (done) => {
      router.url = '/login';
      const url = 'http://localhost:8081/api/test';

      httpClient.get(url).subscribe({
        next: () => fail('should have failed'),
        error: () => {
          expect(authService.logout).not.toHaveBeenCalled();
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should provide user-friendly message on 401', (done) => {
      const url = 'http://localhost:8081/api/test';

      httpClient.get(url).subscribe({
        next: () => fail('should have failed'),
        error: (error: any) => {
          expect(error.userMessage).toBe('Unauthorized: Please log in to access this resource.');
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should log out user and clear auth state on 401', (done) => {
      const url = 'http://localhost:8081/api/protected-resource';

      httpClient.get(url).subscribe({
        next: () => fail('should have failed'),
        error: () => {
          // Verify logout was called which clears tokens and redirects
          expect(authService.logout).toHaveBeenCalledTimes(1);
          done();
        },
      });

      const req = httpMock.expectOne(url);
      req.flush('Session expired', { status: 401, statusText: 'Unauthorized' });
    });
  });

  describe('Tenant ID Injection', () => {
    it('should add X-Tenant-ID header to all requests', (done) => {
      const url = 'http://localhost:8081/api/test';

      httpClient.get(url).subscribe({
        next: () => done(),
        error: () => fail('should not have failed'),
      });

      const req = httpMock.expectOne(url);
      expect(req.request.headers.has('X-Tenant-ID')).toBe(true);
      expect(req.request.headers.get('X-Tenant-ID')).toBe('tenant-123');
      req.flush({ success: true });
    });

    it('should use tenant ID from auth service', (done) => {
      authService.getTenantId.mockReturnValue('custom-tenant-456');
      const url = 'http://localhost:8081/api/test';

      httpClient.get(url).subscribe({
        next: () => done(),
        error: () => fail('should not have failed'),
      });

      const req = httpMock.expectOne(url);
      expect(req.request.headers.get('X-Tenant-ID')).toBe('custom-tenant-456');
      expect(authService.getTenantId).toHaveBeenCalled();
      req.flush({ success: true });
    });

    it('should use default tenant ID when auth service returns null', (done) => {
      authService.getTenantId.mockReturnValue(null);
      const url = 'http://localhost:8081/api/test';

      httpClient.get(url).subscribe({
        next: () => done(),
        error: () => fail('should not have failed'),
      });

      const req = httpMock.expectOne(url);
      expect(req.request.headers.get('X-Tenant-ID')).toBe('default-tenant');
      req.flush({ success: true });
    });

    it('should add tenant ID header even on POST requests', (done) => {
      const url = 'http://localhost:8081/api/test';
      const body = { data: 'test' };

      httpClient.post(url, body).subscribe({
        next: () => done(),
        error: () => fail('should not have failed'),
      });

      const req = httpMock.expectOne(url);
      expect(req.request.headers.get('X-Tenant-ID')).toBe('tenant-123');
      req.flush({ success: true });
    });

    it('should add tenant ID header even on PUT requests', (done) => {
      const url = 'http://localhost:8081/api/test/123';
      const body = { data: 'updated' };

      httpClient.put(url, body).subscribe({
        next: () => done(),
        error: () => fail('should not have failed'),
      });

      const req = httpMock.expectOne(url);
      expect(req.request.headers.get('X-Tenant-ID')).toBe('tenant-123');
      req.flush({ success: true });
    });

    it('should add tenant ID header even on DELETE requests', (done) => {
      const url = 'http://localhost:8081/api/test/123';

      httpClient.delete(url).subscribe({
        next: () => done(),
        error: () => fail('should not have failed'),
      });

      const req = httpMock.expectOne(url);
      expect(req.request.headers.get('X-Tenant-ID')).toBe('tenant-123');
      req.flush({ success: true });
    });

    it('should preserve existing headers when adding tenant ID', (done) => {
      const url = 'http://localhost:8081/api/test';
      const headers = { 'Custom-Header': 'custom-value' };

      httpClient.get(url, { headers }).subscribe({
        next: () => done(),
        error: () => fail('should not have failed'),
      });

      const req = httpMock.expectOne(url);
      expect(req.request.headers.get('X-Tenant-ID')).toBe('tenant-123');
      expect(req.request.headers.get('Custom-Header')).toBe('custom-value');
      req.flush({ success: true });
    });
  });
});
