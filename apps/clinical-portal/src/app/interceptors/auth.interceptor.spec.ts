import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { API_CONFIG, HTTP_HEADERS } from '../config/api.config';
import { AuthService } from '../services/auth.service';
import { createMockHttpClient } from '../../testing/mocks';

/**
 * Auth Interceptor Tests
 *
 * The interceptor adds X-Tenant-ID header for multi-tenancy.
 * JWT auth is handled via HttpOnly cookies (not Authorization headers).
 */
describe('AuthInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;
  const authServiceMock = {
    getToken: jest.fn(() => 'test-token'),
    getTenantId: jest.fn(() => 'tenant-123'),
  };

  beforeEach(() => {
    authServiceMock.getToken.mockReset();
    authServiceMock.getTenantId.mockReset();
    authServiceMock.getToken.mockReturnValue('test-token');
    authServiceMock.getTenantId.mockReturnValue('tenant-123');

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceMock },
        { provide: HttpClient, useValue: createMockHttpClient() },
        HttpTestingController,
      ],
    });

    httpMock = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Tenant ID Header Injection', () => {
    it('should add X-Tenant-ID header to CQL Engine requests', (done) => {
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries`;

      httpClient.get(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.headers.has(HTTP_HEADERS.TENANT_ID)).toBe(true);
      expect(req.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe('tenant-123');

      req.flush({});
    }, 30000);

    it('should add X-Tenant-ID header to Quality Measure Service requests', (done) => {
      const url = `${API_CONFIG.QUALITY_MEASURE_URL}/quality-measure/calculate`;

      httpClient.get(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.headers.has(HTTP_HEADERS.TENANT_ID)).toBe(true);
      expect(req.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe('tenant-123');

      req.flush({});
    }, 30000);

    it('should add X-Tenant-ID header to FHIR Server requests', (done) => {
      const url = `${API_CONFIG.FHIR_SERVER_URL}/Patient`;

      httpClient.get(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.headers.has(HTTP_HEADERS.TENANT_ID)).toBe(true);
      expect(req.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe('tenant-123');
      req.flush({});
    });

    it('should NOT add X-Tenant-ID header to external URLs', (done) => {
      const url = 'https://external-api.example.com/data';

      httpClient.get(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.headers.has(HTTP_HEADERS.TENANT_ID)).toBe(false);
      req.flush({});
    });
  });

  describe('HTTP Methods', () => {
    it('should add tenant header to POST requests', (done) => {
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/evaluations`;
      const body = { libraryId: 'lib-1', patientId: 'patient-1' };

      httpClient.post(url, body).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.method).toBe('POST');
      expect(req.request.headers.has(HTTP_HEADERS.TENANT_ID)).toBe(true);
      expect(req.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe('tenant-123');
      req.flush({});
    });

    it('should add tenant header to PUT requests', (done) => {
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries/lib-1`;
      const body = { name: 'Updated' };

      httpClient.put(url, body).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.method).toBe('PUT');
      expect(req.request.headers.has(HTTP_HEADERS.TENANT_ID)).toBe(true);
      expect(req.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe('tenant-123');
      req.flush({});
    });

    it('should add tenant header to DELETE requests', (done) => {
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries/lib-1`;

      httpClient.delete(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.method).toBe('DELETE');
      expect(req.request.headers.has(HTTP_HEADERS.TENANT_ID)).toBe(true);
      expect(req.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe('tenant-123');
      req.flush(null);
    }, 30000);
  });

  describe('Multiple Requests', () => {
    it('should add tenant header to all backend requests', (done) => {
      const urls = [
        `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries`,
        `${API_CONFIG.QUALITY_MEASURE_URL}/quality-measure/calculate`,
        `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/evaluations`,
      ];

      let completedRequests = 0;
      urls.forEach((url) => {
        httpClient.get(url).subscribe(() => {
          completedRequests++;
          if (completedRequests === urls.length) done();
        }, 30000);
      });

      urls.forEach((url) => {
        const req = httpMock.expectOne(url);
        expect(req.request.headers.has(HTTP_HEADERS.TENANT_ID)).toBe(true);
        expect(req.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe('tenant-123');
        req.flush({});
      });
    });
  });

  describe('Edge Cases', () => {
    it('should not interfere with existing headers', (done) => {
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries`;
      const customHeader = { headers: { 'X-Custom-Header': 'CustomValue' } };

      httpClient.get(url, customHeader).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.headers.has(HTTP_HEADERS.TENANT_ID)).toBe(true);
      expect(req.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe('tenant-123');
      expect(req.request.headers.get('X-Custom-Header')).toBe('CustomValue');
      req.flush({});
    }, 30000);

    it('should handle concurrent requests independently', (done) => {
      const cqlUrl = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries`;
      const fhirUrl = `${API_CONFIG.FHIR_SERVER_URL}/Patient`;

      let completedRequests = 0;
      const checkDone = () => {
        completedRequests++;
        if (completedRequests === 2) done();
      };

      httpClient.get(cqlUrl).subscribe(() => checkDone());
      httpClient.get(fhirUrl).subscribe(() => checkDone());

      const cqlReq = httpMock.expectOne(cqlUrl);
      const fhirReq = httpMock.expectOne(fhirUrl);

      expect(cqlReq.request.headers.has(HTTP_HEADERS.TENANT_ID)).toBe(true);
      expect(cqlReq.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe('tenant-123');
      expect(fhirReq.request.headers.has(HTTP_HEADERS.TENANT_ID)).toBe(true);
      expect(fhirReq.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe('tenant-123');

      cqlReq.flush({});
      fhirReq.flush({});
    });
  });

  describe('Missing Tenant ID', () => {
    it('should not add X-Tenant-ID header when tenant is missing', (done) => {
      authServiceMock.getTenantId.mockReturnValueOnce(null);
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries`;

      httpClient.get(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.headers.has(HTTP_HEADERS.TENANT_ID)).toBe(false);
      req.flush({});
    });
  });

  describe('Cookie-based JWT Auth', () => {
    it('should NOT add Authorization header (JWT is in HttpOnly cookie)', (done) => {
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries`;

      httpClient.get(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      // Authorization header should NOT be set - JWT is handled via cookies
      expect(req.request.headers.has(HTTP_HEADERS.AUTHORIZATION)).toBe(false);
      // But tenant header should be set
      expect(req.request.headers.has(HTTP_HEADERS.TENANT_ID)).toBe(true);
      req.flush({});
    });
  });
});
