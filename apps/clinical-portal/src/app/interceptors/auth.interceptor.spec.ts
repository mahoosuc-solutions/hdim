import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { API_CONFIG, HTTP_HEADERS } from '../config/api.config';

describe('AuthInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    httpMock = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Basic Auth Header Injection', () => {
    it('should add Authorization header to CQL Engine requests', (done) => {
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries`;

      httpClient.get(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.headers.has(HTTP_HEADERS.AUTHORIZATION)).toBe(true);

      const authHeader = req.request.headers.get(HTTP_HEADERS.AUTHORIZATION);
      expect(authHeader).toContain('Basic ');

      // Verify it's properly base64 encoded credentials
      const base64Credentials = authHeader?.split(' ')[1];
      expect(base64Credentials).toBeTruthy();

      // Decode and verify credentials format (username:password)
      const credentials = atob(base64Credentials!);
      expect(credentials).toContain(':');
      expect(credentials).toBe('cql-service-user:cql-service-dev-password-change-in-prod');

      req.flush({});
    });

    it('should add Authorization header to Quality Measure Service requests', (done) => {
      const url = `${API_CONFIG.QUALITY_MEASURE_URL}/quality-measure/calculate`;

      httpClient.get(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.headers.has(HTTP_HEADERS.AUTHORIZATION)).toBe(true);

      const authHeader = req.request.headers.get(HTTP_HEADERS.AUTHORIZATION);
      expect(authHeader).toContain('Basic ');

      req.flush({});
    });

    it('should NOT add Authorization header to FHIR Server requests', (done) => {
      const url = `${API_CONFIG.FHIR_SERVER_URL}/Patient`;

      httpClient.get(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.headers.has(HTTP_HEADERS.AUTHORIZATION)).toBe(false);
      req.flush({});
    });

    it('should NOT add Authorization header to external URLs', (done) => {
      const url = 'https://external-api.example.com/data';

      httpClient.get(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.headers.has(HTTP_HEADERS.AUTHORIZATION)).toBe(false);
      req.flush({});
    });
  });

  describe('Credential Encoding', () => {
    it('should encode credentials correctly with btoa', (done) => {
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries`;
      const expectedCredentials = 'cql-service-user:cql-service-dev-password-change-in-prod';
      const expectedEncoded = btoa(expectedCredentials);

      httpClient.get(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      const authHeader = req.request.headers.get(HTTP_HEADERS.AUTHORIZATION);
      expect(authHeader).toBe(`Basic ${expectedEncoded}`);
      req.flush({});
    });
  });

  describe('HTTP Methods', () => {
    it('should add auth header to POST requests', (done) => {
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/evaluations`;
      const body = { libraryId: 'lib-1', patientId: 'patient-1' };

      httpClient.post(url, body).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.method).toBe('POST');
      expect(req.request.headers.has(HTTP_HEADERS.AUTHORIZATION)).toBe(true);
      req.flush({});
    });

    it('should add auth header to PUT requests', (done) => {
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries/lib-1`;
      const body = { name: 'Updated' };

      httpClient.put(url, body).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.method).toBe('PUT');
      expect(req.request.headers.has(HTTP_HEADERS.AUTHORIZATION)).toBe(true);
      req.flush({});
    });

    it('should add auth header to DELETE requests', (done) => {
      const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/libraries/lib-1`;

      httpClient.delete(url).subscribe(() => done());

      const req = httpMock.expectOne(url);
      expect(req.request.method).toBe('DELETE');
      expect(req.request.headers.has(HTTP_HEADERS.AUTHORIZATION)).toBe(true);
      req.flush(null);
    });
  });

  describe('Multiple Requests', () => {
    it('should add auth header to all backend requests', (done) => {
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
        });
      });

      urls.forEach((url) => {
        const req = httpMock.expectOne(url);
        expect(req.request.headers.has(HTTP_HEADERS.AUTHORIZATION)).toBe(true);
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
      expect(req.request.headers.has(HTTP_HEADERS.AUTHORIZATION)).toBe(true);
      expect(req.request.headers.get('X-Custom-Header')).toBe('CustomValue');
      req.flush({});
    });

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

      expect(cqlReq.request.headers.has(HTTP_HEADERS.AUTHORIZATION)).toBe(true);
      expect(fhirReq.request.headers.has(HTTP_HEADERS.AUTHORIZATION)).toBe(false);

      cqlReq.flush({});
      fhirReq.flush({});
    });
  });
});
