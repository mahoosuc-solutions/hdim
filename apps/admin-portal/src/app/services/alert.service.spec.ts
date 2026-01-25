import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AlertService } from './alert.service';
import { LoggerService } from './logger.service';
import { AlertConfig, AlertType, AlertSeverity, NotificationChannel } from '../models/alert-config.model';

describe('AlertService', () => {
  let service: AlertService;
  let httpMock: HttpTestingController;
  let loggerService: jasmine.SpyObj<LoggerService>;

  const mockAlertConfig: AlertConfig = {
    id: '123e4567-e89b-12d3-a456-426614174000',
    serviceName: 'patient-service',
    displayName: 'Patient Service CPU Alert',
    alertType: 'CPU_USAGE' as AlertType,
    threshold: 80,
    durationMinutes: 5,
    severity: 'WARNING' as AlertSeverity,
    enabled: true,
    notificationChannels: ['EMAIL', 'SLACK'] as NotificationChannel[],
    createdAt: new Date('2026-01-24T12:00:00Z'),
    updatedAt: new Date('2026-01-24T12:00:00Z')
  };

  beforeEach(() => {
    const loggerSpy = jasmine.createSpyObj('LoggerService', ['withContext']);
    const contextLoggerSpy = jasmine.createSpyObj('ContextLogger', ['info', 'error', 'warn']);
    loggerSpy.withContext.and.returnValue(contextLoggerSpy);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AlertService,
        { provide: LoggerService, useValue: loggerSpy }
      ]
    });

    service = TestBed.inject(AlertService);
    httpMock = TestBed.inject(HttpTestingController);
    loggerService = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getAllAlertConfigs', () => {
    it('should retrieve all alert configurations', (done) => {
      const mockConfigs: AlertConfig[] = [mockAlertConfig];

      service.getAllAlertConfigs().subscribe((configs) => {
        expect(configs).toEqual(mockConfigs);
        expect(configs.length).toBe(1);
        done();
      });

      const req = httpMock.expectOne('/api/v1/admin/alerts/configs');
      expect(req.request.method).toBe('GET');
      expect(req.request.headers.get('X-Tenant-ID')).toBeTruthy();
      req.flush(mockConfigs);
    });

    it('should handle empty alert configurations', (done) => {
      service.getAllAlertConfigs().subscribe((configs) => {
        expect(configs).toEqual([]);
        expect(configs.length).toBe(0);
        done();
      });

      const req = httpMock.expectOne('/api/v1/admin/alerts/configs');
      req.flush([]);
    });

    it('should handle HTTP errors gracefully', (done) => {
      service.getAllAlertConfigs().subscribe({
        next: () => fail('Should have failed with error'),
        error: (error) => {
          expect(error.status).toBe(500);
          done();
        }
      });

      const req = httpMock.expectOne('/api/v1/admin/alerts/configs');
      req.flush({ message: 'Internal Server Error' }, { status: 500, statusText: 'Server Error' });
    });
  });

  describe('getAlertConfig', () => {
    it('should retrieve a specific alert configuration by ID', (done) => {
      const alertId = mockAlertConfig.id;

      service.getAlertConfig(alertId).subscribe((config) => {
        expect(config).toEqual(mockAlertConfig);
        expect(config.id).toBe(alertId);
        done();
      });

      const req = httpMock.expectOne(`/api/v1/admin/alerts/configs/${alertId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockAlertConfig);
    });

    it('should handle not found errors (404)', (done) => {
      const alertId = 'non-existent-id';

      service.getAlertConfig(alertId).subscribe({
        next: () => fail('Should have failed with 404'),
        error: (error) => {
          expect(error.status).toBe(404);
          done();
        }
      });

      const req = httpMock.expectOne(`/api/v1/admin/alerts/configs/${alertId}`);
      req.flush({ message: 'Alert configuration not found' }, { status: 404, statusText: 'Not Found' });
    });
  });

  describe('createAlertConfig', () => {
    it('should create a new alert configuration', (done) => {
      const createRequest = {
        serviceName: 'patient-service',
        displayName: 'Patient Service CPU Alert',
        alertType: 'CPU_USAGE' as AlertType,
        threshold: 80,
        durationMinutes: 5,
        severity: 'WARNING' as AlertSeverity,
        enabled: true,
        notificationChannels: ['EMAIL', 'SLACK'] as NotificationChannel[]
      };

      service.createAlertConfig(createRequest).subscribe((config) => {
        expect(config).toEqual(mockAlertConfig);
        expect(config.serviceName).toBe(createRequest.serviceName);
        done();
      });

      const req = httpMock.expectOne('/api/v1/admin/alerts/configs');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(createRequest);
      expect(req.request.headers.get('Content-Type')).toBe('application/json');
      req.flush(mockAlertConfig);
    });

    it('should handle validation errors (400)', (done) => {
      const invalidRequest = {
        serviceName: '',
        displayName: 'Test',
        alertType: 'CPU_USAGE' as AlertType,
        threshold: -10,
        durationMinutes: 0,
        severity: 'WARNING' as AlertSeverity,
        enabled: true,
        notificationChannels: [] as NotificationChannel[]
      };

      service.createAlertConfig(invalidRequest).subscribe({
        next: () => fail('Should have failed with validation error'),
        error: (error) => {
          expect(error.status).toBe(400);
          done();
        }
      });

      const req = httpMock.expectOne('/api/v1/admin/alerts/configs');
      req.flush(
        { message: 'Validation failed', errors: ['Service name is required', 'Threshold must be greater than 0'] },
        { status: 400, statusText: 'Bad Request' }
      );
    });
  });

  describe('updateAlertConfig', () => {
    it('should update an existing alert configuration', (done) => {
      const alertId = mockAlertConfig.id;
      const updateRequest = {
        threshold: 90,
        severity: 'CRITICAL' as AlertSeverity,
        enabled: false
      };

      const updatedConfig = { ...mockAlertConfig, ...updateRequest };

      service.updateAlertConfig(alertId, updateRequest).subscribe((config) => {
        expect(config.threshold).toBe(90);
        expect(config.severity).toBe('CRITICAL');
        expect(config.enabled).toBe(false);
        done();
      });

      const req = httpMock.expectOne(`/api/v1/admin/alerts/configs/${alertId}`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updateRequest);
      req.flush(updatedConfig);
    });

    it('should handle not found errors when updating', (done) => {
      const alertId = 'non-existent-id';
      const updateRequest = { threshold: 90 };

      service.updateAlertConfig(alertId, updateRequest).subscribe({
        next: () => fail('Should have failed with 404'),
        error: (error) => {
          expect(error.status).toBe(404);
          done();
        }
      });

      const req = httpMock.expectOne(`/api/v1/admin/alerts/configs/${alertId}`);
      req.flush({ message: 'Alert configuration not found' }, { status: 404, statusText: 'Not Found' });
    });
  });

  describe('deleteAlertConfig', () => {
    it('should delete an alert configuration', (done) => {
      const alertId = mockAlertConfig.id;

      service.deleteAlertConfig(alertId).subscribe(() => {
        expect(true).toBe(true); // Verify completion
        done();
      });

      const req = httpMock.expectOne(`/api/v1/admin/alerts/configs/${alertId}`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null, { status: 204, statusText: 'No Content' });
    });

    it('should handle not found errors when deleting', (done) => {
      const alertId = 'non-existent-id';

      service.deleteAlertConfig(alertId).subscribe({
        next: () => fail('Should have failed with 404'),
        error: (error) => {
          expect(error.status).toBe(404);
          done();
        }
      });

      const req = httpMock.expectOne(`/api/v1/admin/alerts/configs/${alertId}`);
      req.flush({ message: 'Alert configuration not found' }, { status: 404, statusText: 'Not Found' });
    });
  });

  describe('toggleAlertConfig', () => {
    it('should enable an alert configuration', (done) => {
      const alertId = mockAlertConfig.id;
      const updatedConfig = { ...mockAlertConfig, enabled: true };

      service.toggleAlertConfig(alertId, true).subscribe((config) => {
        expect(config.enabled).toBe(true);
        done();
      });

      const req = httpMock.expectOne(`/api/v1/admin/alerts/configs/${alertId}`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ enabled: true });
      req.flush(updatedConfig);
    });

    it('should disable an alert configuration', (done) => {
      const alertId = mockAlertConfig.id;
      const updatedConfig = { ...mockAlertConfig, enabled: false };

      service.toggleAlertConfig(alertId, false).subscribe((config) => {
        expect(config.enabled).toBe(false);
        done();
      });

      const req = httpMock.expectOne(`/api/v1/admin/alerts/configs/${alertId}`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ enabled: false });
      req.flush(updatedConfig);
    });
  });

  describe('tenant isolation', () => {
    it('should include X-Tenant-ID header in all requests', (done) => {
      service.getAllAlertConfigs().subscribe(() => done());

      const req = httpMock.expectOne('/api/v1/admin/alerts/configs');
      expect(req.request.headers.get('X-Tenant-ID')).toBe('tenant1'); // Default tenant from service
      req.flush([]);
    });

    it('should include X-Tenant-ID header in create requests', (done) => {
      const createRequest = {
        serviceName: 'test-service',
        displayName: 'Test Alert',
        alertType: 'CPU_USAGE' as AlertType,
        threshold: 80,
        durationMinutes: 5,
        severity: 'WARNING' as AlertSeverity,
        enabled: true,
        notificationChannels: ['EMAIL'] as NotificationChannel[]
      };

      service.createAlertConfig(createRequest).subscribe(() => done());

      const req = httpMock.expectOne('/api/v1/admin/alerts/configs');
      expect(req.request.headers.get('X-Tenant-ID')).toBe('tenant1');
      req.flush(mockAlertConfig);
    });
  });

  describe('error handling and logging', () => {
    it('should log errors when API calls fail', (done) => {
      service.getAllAlertConfigs().subscribe({
        next: () => fail('Should have failed'),
        error: () => {
          const contextLogger = loggerService.withContext('AlertService');
          expect(contextLogger.error).toHaveBeenCalled();
          done();
        }
      });

      const req = httpMock.expectOne('/api/v1/admin/alerts/configs');
      req.flush({ message: 'Server error' }, { status: 500, statusText: 'Internal Server Error' });
    });

    it('should log successful operations', (done) => {
      service.getAllAlertConfigs().subscribe(() => {
        const contextLogger = loggerService.withContext('AlertService');
        expect(contextLogger.info).toHaveBeenCalled();
        done();
      });

      const req = httpMock.expectOne('/api/v1/admin/alerts/configs');
      req.flush([mockAlertConfig]);
    });
  });
});
