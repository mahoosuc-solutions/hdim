import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { tenantInterceptor } from './tenant.interceptor';
import { API_CONFIG, HTTP_HEADERS } from '../config/api.config';
import { AuthService } from '../services/auth.service';

describe('TenantInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([tenantInterceptor])),
        provideHttpClientTesting(),
        HttpTestingController,
        {
          provide: AuthService,
          useValue: {
            getTenantId: () => API_CONFIG.DEFAULT_TENANT_ID,
          },
        },
      ],
    });

    httpMock = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Tenant Header Injection', () => {
    it('should add X-Tenant-ID header to CQL Engine requests', (done) => {
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries`;

      httpClient.get(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.headers.has(HTTP_HEADERS.TENANT_ID)).toBe(true);
      expect(req.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe(
        API_CONFIG.DEFAULT_TENANT_ID
      );
      req.flush({});
    };

    it('should add X-Tenant-ID header to Quality Measure Service requests', (done) => {
      const url = `${API_CONFIG.QUALITY_MEASURE_URL}/quality-measure/calculate`;

      httpClient.get(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.headers.has(HTTP_HEADERS.TENANT_ID)).toBe(true);
      expect(req.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe(
        API_CONFIG.DEFAULT_TENANT_ID
      );
      req.flush({});
    };

    it('should add X-Tenant-ID header to FHIR Server requests', (done) => {
      const url = `${API_CONFIG.FHIR_SERVER_URL}/Patient`;

      httpClient.get(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.headers.has(HTTP_HEADERS.TENANT_ID)).toBe(true);
      expect(req.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe(
        API_CONFIG.DEFAULT_TENANT_ID
      );
      req.flush({});
    };

    it('should NOT add X-Tenant-ID header to external URLs', (done) => {
      const url = 'https://external-api.example.com/data';

      httpClient.get(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.headers.has(HTTP_HEADERS.TENANT_ID)).toBe(false);
      req.flush({});
    };
  });

  describe('Multiple Requests', () => {
    it('should add tenant header to multiple CQL Engine requests', (done) => {
      const urls = [
        `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries`,
        `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/evaluations`,
        `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries/active`,
      ];

      let completedRequests = 0;
      urls.forEach((url) => {
        httpClient.get(url).subscribe(() => {
          completedRequests++;
          if (completedRequests === urls.length) done();
        });
      });

      urls.forEach((url) => {
        const req = httpMock.expectOne(url);
        expect(req.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe(
          API_CONFIG.DEFAULT_TENANT_ID
        );
        req.flush({});
      });
    };
  });

  describe('HTTP Methods', () => {
    it('should add tenant header to POST requests', (done) => {
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/evaluations`;
      const body = { libraryId: 'lib-1', patientId: 'patient-1' };

      httpClient.post(url, body).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.method).toBe('POST');
      expect(req.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe(
        API_CONFIG.DEFAULT_TENANT_ID
      );
      req.flush({});
    };

    it('should add tenant header to PUT requests', (done) => {
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries/lib-1`;
      const body = { name: 'Updated' };

      httpClient.put(url, body).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.method).toBe('PUT');
      expect(req.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe(
        API_CONFIG.DEFAULT_TENANT_ID
      );
      req.flush({});
    };

    it('should add tenant header to DELETE requests', (done) => {
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries/lib-1`;

      httpClient.delete(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.method).toBe('DELETE');
      expect(req.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe(
        API_CONFIG.DEFAULT_TENANT_ID
      );
      req.flush(null);
    };
  });

  describe('Edge Cases', () => {
    it('should handle URLs with query parameters', (done) => {
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries?page=0&size=20`;

      httpClient.get(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe(
        API_CONFIG.DEFAULT_TENANT_ID
      );
      req.flush({});
    };

    it('should handle URLs with fragments', (done) => {
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries#section`;

      httpClient.get(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe(
        API_CONFIG.DEFAULT_TENANT_ID
      );
      req.flush({});
    };

    it('should not interfere with existing headers', (done) => {
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries`;
      const customHeader = { headers: { 'X-Custom-Header': 'CustomValue' } };

      httpClient.get(url, customHeader).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.headers.get(HTTP_HEADERS.TENANT_ID)).toBe(
        API_CONFIG.DEFAULT_TENANT_ID
      );
      expect(req.request.headers.get('X-Custom-Header')).toBe('CustomValue');
      req.flush({});
    };
  });
});
