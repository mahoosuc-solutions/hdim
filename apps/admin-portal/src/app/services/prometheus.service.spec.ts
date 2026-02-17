import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PrometheusService } from './prometheus.service';
import { LoggerService } from './logger.service';
import { PrometheusQueryResult } from '../models/prometheus.model';

describe('PrometheusService', () => {
  let service: PrometheusService;
  let httpMock: HttpTestingController;
  let loggerService: jasmine.SpyObj<LoggerService>;

  const mockPrometheusUrl = 'http://localhost:9090';
  const mockServiceName = 'patient-service';

  beforeEach(() => {
    const loggerSpy = jasmine.createSpyObj('LoggerService', ['withContext']);
    const contextLoggerSpy = jasmine.createSpyObj('ContextLogger', ['info', 'error', 'warn']);
    loggerSpy.withContext.and.returnValue(contextLoggerSpy);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        PrometheusService,
        { provide: LoggerService, useValue: loggerSpy }
      ]
    });

    service = TestBed.inject(PrometheusService);
    httpMock = TestBed.inject(HttpTestingController);
    loggerService = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('query', () => {
    it('should execute PromQL query successfully', (done) => {
      const mockQuery = 'up{job="hdim-services"}';
      const mockResponse: PrometheusQueryResult = {
        status: 'success',
        data: {
          resultType: 'vector',
          result: [
            {
              metric: { __name__: 'up', job: 'hdim-services', instance: 'patient-service:8084' },
              value: [1706140800, '1']
            }
          ]
        }
      };

      service.query(mockQuery).subscribe((result) => {
        expect(result).toEqual(mockResponse);
        expect(result.status).toBe('success');
        expect(result.data.result.length).toBe(1);
        done();
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes('/api/v1/query') && request.params.get('query') === mockQuery
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should handle Prometheus API errors gracefully', (done) => {
      const mockQuery = 'invalid_query{';
      const mockError = { error: 'parse error', errorType: 'bad_data' };

      service.query(mockQuery).subscribe({
        next: () => fail('Should have failed with error'),
        error: (error) => {
          expect(error.status).toBe(400);
          done();
        }
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/v1/query'));
      req.flush(mockError, { status: 400, statusText: 'Bad Request' });
    });

    it('should include timestamp parameter when provided', (done) => {
      const mockQuery = 'up';
      const timestamp = 1706140800;
      const mockResponse: PrometheusQueryResult = {
        status: 'success',
        data: { resultType: 'vector', result: [] }
      };

      service.query(mockQuery, timestamp).subscribe(() => done());

      const req = httpMock.expectOne((request) =>
        request.url.includes('/api/v1/query') &&
        request.params.get('time') === timestamp.toString()
      );
      req.flush(mockResponse);
    });
  });

  describe('getCpuUsage', () => {
    it('should return CPU usage percentage for a service', (done) => {
      const mockResponse: PrometheusQueryResult = {
        status: 'success',
        data: {
          resultType: 'vector',
          result: [
            {
              metric: { instance: 'patient-service:8084' },
              value: [1706140800, '0.45']
            }
          ]
        }
      };

      service.getCpuUsage(mockServiceName).subscribe((cpuUsage) => {
        expect(cpuUsage).toBe(45); // 0.45 * 100 = 45%
        done();
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes('/api/v1/query') &&
        request.params.get('query')?.includes('process_cpu_seconds_total')
      );
      req.flush(mockResponse);
    });

    it('should return 0 when no data available', (done) => {
      const mockResponse: PrometheusQueryResult = {
        status: 'success',
        data: { resultType: 'vector', result: [] }
      };

      service.getCpuUsage(mockServiceName).subscribe((cpuUsage) => {
        expect(cpuUsage).toBe(0);
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/v1/query'));
      req.flush(mockResponse);
    });

    it('should handle non-numeric values gracefully', (done) => {
      const mockResponse: PrometheusQueryResult = {
        status: 'success',
        data: {
          resultType: 'vector',
          result: [
            {
              metric: { instance: 'patient-service:8084' },
              value: [1706140800, 'NaN']
            }
          ]
        }
      };

      service.getCpuUsage(mockServiceName).subscribe((cpuUsage) => {
        expect(cpuUsage).toBe(0);
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/v1/query'));
      req.flush(mockResponse);
    });
  });

  describe('getMemoryUsage', () => {
    it('should return memory usage in MB for a service', (done) => {
      const mockResponse: PrometheusQueryResult = {
        status: 'success',
        data: {
          resultType: 'vector',
          result: [
            {
              metric: { instance: 'patient-service:8084' },
              value: [1706140800, '524288000'] // 500 MB in bytes
            }
          ]
        }
      };

      service.getMemoryUsage(mockServiceName).subscribe((memoryUsage) => {
        expect(memoryUsage).toBeCloseTo(500, 0); // ~500 MB
        done();
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes('/api/v1/query') &&
        request.params.get('query')?.includes('process_resident_memory_bytes')
      );
      req.flush(mockResponse);
    });
  });

  describe('getRequestRate', () => {
    it('should return request rate per second for a service', (done) => {
      const mockResponse: PrometheusQueryResult = {
        status: 'success',
        data: {
          resultType: 'vector',
          result: [
            {
              metric: { instance: 'patient-service:8084' },
              value: [1706140800, '25.5']
            }
          ]
        }
      };

      service.getRequestRate(mockServiceName).subscribe((requestRate) => {
        expect(requestRate).toBe(25.5);
        done();
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes('/api/v1/query') &&
        request.params.get('query')?.includes('http_server_requests_seconds_count')
      );
      req.flush(mockResponse);
    });
  });

  describe('getErrorRate', () => {
    it('should return error rate per second for a service', (done) => {
      const mockResponse: PrometheusQueryResult = {
        status: 'success',
        data: {
          resultType: 'vector',
          result: [
            {
              metric: { instance: 'patient-service:8084' },
              value: [1706140800, '2.3']
            }
          ]
        }
      };

      service.getErrorRate(mockServiceName).subscribe((errorRate) => {
        expect(errorRate).toBe(2.3);
        done();
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes('/api/v1/query') &&
        request.params.get('query')?.includes('status=~"5.."')
      );
      req.flush(mockResponse);
    });
  });

  describe('getP95Latency', () => {
    it('should return P95 latency in milliseconds for a service', (done) => {
      const mockResponse: PrometheusQueryResult = {
        status: 'success',
        data: {
          resultType: 'vector',
          result: [
            {
              metric: { instance: 'patient-service:8084' },
              value: [1706140800, '0.125'] // 125ms in seconds
            }
          ]
        }
      };

      service.getP95Latency(mockServiceName).subscribe((latency) => {
        expect(latency).toBe(125);
        done();
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes('/api/v1/query') &&
        request.params.get('query')?.includes('histogram_quantile(0.95')
      );
      req.flush(mockResponse);
    });
  });

  describe('getP99Latency', () => {
    it('should return P99 latency in milliseconds for a service', (done) => {
      const mockResponse: PrometheusQueryResult = {
        status: 'success',
        data: {
          resultType: 'vector',
          result: [
            {
              metric: { instance: 'patient-service:8084' },
              value: [1706140800, '0.25'] // 250ms in seconds
            }
          ]
        }
      };

      service.getP99Latency(mockServiceName).subscribe((latency) => {
        expect(latency).toBe(250);
        done();
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes('/api/v1/query') &&
        request.params.get('query')?.includes('histogram_quantile(0.99')
      );
      req.flush(mockResponse);
    });
  });

  describe('isPrometheusAvailable', () => {
    it('should return true when Prometheus is reachable', (done) => {
      service.isPrometheusAvailable().subscribe((available) => {
        expect(available).toBe(true);
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/-/healthy'));
      req.flush('Prometheus is Healthy');
    });

    it('should return false when Prometheus is unreachable', (done) => {
      service.isPrometheusAvailable().subscribe((available) => {
        expect(available).toBe(false);
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/-/healthy'));
      req.flush(null, { status: 503, statusText: 'Service Unavailable' });
    });

    it('should return false on network errors', (done) => {
      service.isPrometheusAvailable().subscribe((available) => {
        expect(available).toBe(false);
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/-/healthy'));
      req.error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });
    });
  });

  describe('PromQL query construction', () => {
    it('should correctly escape service name in queries', (done) => {
      const serviceWithSpecialChars = 'patient-service.v2';
      const mockResponse: PrometheusQueryResult = {
        status: 'success',
        data: { resultType: 'vector', result: [] }
      };

      service.getCpuUsage(serviceWithSpecialChars).subscribe(() => done());

      const req = httpMock.expectOne((request) => request.url.includes('/api/v1/query'));
      const query = req.request.params.get('query');
      expect(query).toContain('patient-service\\.v2');
      req.flush(mockResponse);
    });

    it('should use correct time range for rate calculations', (done) => {
      const mockResponse: PrometheusQueryResult = {
        status: 'success',
        data: { resultType: 'vector', result: [] }
      };

      service.getRequestRate(mockServiceName).subscribe(() => done());

      const req = httpMock.expectOne((request) => request.url.includes('/api/v1/query'));
      const query = req.request.params.get('query');
      expect(query).toContain('[1m]'); // 1-minute rate window
      req.flush(mockResponse);
    });
  });
});
