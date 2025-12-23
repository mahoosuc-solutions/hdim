import { TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuditService, AuditAction, AuditOutcome, AuditEvent } from './audit.service';
import { AuthService, User } from './auth.service';

describe('AuditService', () => {
  let service: AuditService;
  let httpMock: HttpTestingController;
  let authService: jest.Mocked<AuthService>;

  const mockUser: User = {
    id: 'user-1',
    username: 'testuser',
    email: 'test@example.com',
    firstName: 'Test',
    lastName: 'User',
    fullName: 'Test User',
    roles: [{ id: 'role-1', name: 'CLINICIAN' }],
    tenantId: 'tenant-1',
    active: true,
  };

  beforeEach(() => {
    const authServiceMock = {
      currentUserValue: mockUser,
    };

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuditService,
        { provide: AuthService, useValue: authServiceMock },
      ],
    });

    service = TestBed.inject(AuditService);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService) as jest.Mocked<AuthService>;

    // Clear localStorage before each test
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('initialization', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should flush pending events from localStorage on init', fakeAsync(() => {
      // Store pending events
      const pendingEvents: Partial<AuditEvent>[] = [
        { id: 'event-1', action: AuditAction.LOGIN, outcome: AuditOutcome.SUCCESS },
      ];
      localStorage.setItem('healthdata_pending_audit_events', JSON.stringify(pendingEvents));

      // Create new service instance to trigger flush
      const newService = new AuditService(
        TestBed.inject(HttpTestingController) as any,
        authService
      );

      tick(100);

      // The events should be flushed (but we can't easily verify the request here)
      expect(localStorage.getItem('healthdata_pending_audit_events')).toBeNull();
      discardPeriodicTasks();
    }));
  });

  describe('log()', () => {
    it('should buffer events and send in batches', fakeAsync(() => {
      service.log({ action: AuditAction.READ, resourceType: 'Patient' });
      service.log({ action: AuditAction.READ, resourceType: 'Observation' });

      // Events are buffered, no immediate request
      httpMock.expectNone('/audit/events');

      // Wait for batch interval (5 seconds)
      tick(5000);

      const req = httpMock.expectOne('/audit/events');
      expect(req.request.method).toBe('POST');
      expect(req.request.body.events.length).toBe(2);
      req.flush({});

      discardPeriodicTasks();
    }));

    it('should include user context in events', fakeAsync(() => {
      service.log({ action: AuditAction.READ, resourceType: 'Patient' });

      tick(5000);

      const req = httpMock.expectOne('/audit/events');
      const event = req.request.body.events[0];

      expect(event.userId).toBe('user-1');
      expect(event.username).toBe('testuser');
      expect(event.role).toBe('CLINICIAN');
      expect(event.tenantId).toBe('tenant-1');
      expect(event.serviceName).toBe('clinical-portal');

      req.flush({});
      discardPeriodicTasks();
    }));

    it('should generate UUID for event ID', fakeAsync(() => {
      service.log({ action: AuditAction.READ });

      tick(5000);

      const req = httpMock.expectOne('/audit/events');
      const event = req.request.body.events[0];

      expect(event.id).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i);

      req.flush({});
      discardPeriodicTasks();
    }));
  });

  describe('logImmediate()', () => {
    it('should send event immediately without batching', fakeAsync(() => {
      service.logImmediate({ action: AuditAction.LOGIN }).subscribe();

      tick();

      const req = httpMock.expectOne('/audit/events');
      expect(req.request.method).toBe('POST');
      expect(req.request.body.events.length).toBe(1);
      req.flush({});

      discardPeriodicTasks();
    }));

    it('should store event locally on failure', fakeAsync(() => {
      service.logImmediate({ action: AuditAction.LOGIN }).subscribe();

      tick();

      const req = httpMock.expectOne('/audit/events');
      req.error(new ErrorEvent('Network error'));

      tick();

      const stored = localStorage.getItem('healthdata_pending_audit_events');
      expect(stored).toBeTruthy();
      const events = JSON.parse(stored!);
      expect(events.length).toBe(1);

      discardPeriodicTasks();
    }));
  });

  describe('convenience methods', () => {
    describe('logLogin()', () => {
      it('should log successful login', fakeAsync(() => {
        service.logLogin(true);

        tick(5000);

        const req = httpMock.expectOne('/audit/events');
        const event = req.request.body.events[0];

        expect(event.action).toBe(AuditAction.LOGIN);
        expect(event.outcome).toBe(AuditOutcome.SUCCESS);
        expect(event.purposeOfUse).toBe('AUTHENTICATION');

        req.flush({});
        discardPeriodicTasks();
      }));

      it('should log failed login with error message', fakeAsync(() => {
        service.logLogin(false, 'Invalid credentials');

        tick(5000);

        const req = httpMock.expectOne('/audit/events');
        const event = req.request.body.events[0];

        expect(event.action).toBe(AuditAction.LOGIN_FAILED);
        expect(event.outcome).toBe(AuditOutcome.MINOR_FAILURE);
        expect(event.errorMessage).toBe('Invalid credentials');

        req.flush({});
        discardPeriodicTasks();
      }));
    });

    describe('logPatientAccess()', () => {
      it('should log patient view access', fakeAsync(() => {
        service.logPatientAccess('patient-123', 'view');

        tick(5000);

        const req = httpMock.expectOne('/audit/events');
        const event = req.request.body.events[0];

        expect(event.action).toBe(AuditAction.READ);
        expect(event.resourceType).toBe('Patient');
        expect(event.resourceId).toBe('patient-123');
        expect(event.purposeOfUse).toBe('TREATMENT');

        req.flush({});
        discardPeriodicTasks();
      }));

      it('should log patient search', fakeAsync(() => {
        service.logPatientAccess('patient-123', 'search');

        tick(5000);

        const req = httpMock.expectOne('/audit/events');
        const event = req.request.body.events[0];

        expect(event.action).toBe(AuditAction.SEARCH);

        req.flush({});
        discardPeriodicTasks();
      }));
    });

    describe('logEvaluation()', () => {
      it('should log successful evaluation', fakeAsync(() => {
        service.logEvaluation({
          evaluationId: 'eval-1',
          measureId: 'BCS',
          patientCount: 100,
          success: true,
          durationMs: 5000,
        });

        tick(5000);

        const req = httpMock.expectOne('/audit/events');
        const event = req.request.body.events[0];

        expect(event.action).toBe(AuditAction.EXECUTE);
        expect(event.resourceType).toBe('CqlEvaluation');
        expect(event.resourceId).toBe('eval-1');
        expect(event.outcome).toBe(AuditOutcome.SUCCESS);
        expect(event.durationMs).toBe(5000);
        expect(event.metadata.measureId).toBe('BCS');
        expect(event.metadata.patientCount).toBe(100);

        req.flush({});
        discardPeriodicTasks();
      }));

      it('should log failed evaluation with error', fakeAsync(() => {
        service.logEvaluation({
          measureId: 'BCS',
          success: false,
          errorMessage: 'CQL execution failed',
        });

        tick(5000);

        const req = httpMock.expectOne('/audit/events');
        const event = req.request.body.events[0];

        expect(event.outcome).toBe(AuditOutcome.SERIOUS_FAILURE);
        expect(event.errorMessage).toBe('CQL execution failed');

        req.flush({});
        discardPeriodicTasks();
      }));
    });

    describe('logQrdaExport()', () => {
      it('should log QRDA Category I export', fakeAsync(() => {
        service.logQrdaExport({
          jobId: 'job-1',
          category: 'I',
          measureIds: ['BCS', 'COL'],
          patientCount: 50,
          success: true,
        });

        tick(5000);

        const req = httpMock.expectOne('/audit/events');
        const event = req.request.body.events[0];

        expect(event.action).toBe(AuditAction.EXPORT);
        expect(event.resourceType).toBe('QRDA_Category_I');
        expect(event.resourceId).toBe('job-1');
        expect(event.methodName).toBe('exportQrdaCategoryI');
        expect(event.metadata.measureIds).toEqual(['BCS', 'COL']);

        req.flush({});
        discardPeriodicTasks();
      }));

      it('should log QRDA Category III export', fakeAsync(() => {
        service.logQrdaExport({
          jobId: 'job-2',
          category: 'III',
          measureIds: ['BCS'],
          success: true,
        });

        tick(5000);

        const req = httpMock.expectOne('/audit/events');
        const event = req.request.body.events[0];

        expect(event.resourceType).toBe('QRDA_Category_III');
        expect(event.methodName).toBe('exportQrdaCategoryIII');

        req.flush({});
        discardPeriodicTasks();
      }));
    });

    describe('logQrdaDownload()', () => {
      it('should log QRDA file download', fakeAsync(() => {
        service.logQrdaDownload('job-1', 'qrda-export-2024-01-15.zip');

        tick(5000);

        const req = httpMock.expectOne('/audit/events');
        const event = req.request.body.events[0];

        expect(event.action).toBe(AuditAction.DOWNLOAD);
        expect(event.resourceType).toBe('QRDA_Export');
        expect(event.resourceId).toBe('job-1');
        expect(event.metadata.filename).toBe('qrda-export-2024-01-15.zip');

        req.flush({});
        discardPeriodicTasks();
      }));
    });

    describe('logBatchEvaluationStart()', () => {
      it('should log batch evaluation start', fakeAsync(() => {
        service.logBatchEvaluationStart({
          jobId: 'batch-1',
          measureIds: ['BCS', 'COL', 'CDC'],
          patientCount: 1000,
        });

        tick(5000);

        const req = httpMock.expectOne('/audit/events');
        const event = req.request.body.events[0];

        expect(event.action).toBe(AuditAction.EXECUTE);
        expect(event.resourceType).toBe('BatchEvaluation');
        expect(event.resourceId).toBe('batch-1');
        expect(event.metadata.measureIds).toEqual(['BCS', 'COL', 'CDC']);
        expect(event.metadata.patientCount).toBe(1000);

        req.flush({});
        discardPeriodicTasks();
      }));
    });

    describe('logBatchEvaluationComplete()', () => {
      it('should log batch evaluation completion', fakeAsync(() => {
        service.logBatchEvaluationComplete({
          jobId: 'batch-1',
          success: true,
          totalEvaluations: 3000,
          successCount: 2950,
          failureCount: 50,
          durationMs: 120000,
        });

        tick(5000);

        const req = httpMock.expectOne('/audit/events');
        const event = req.request.body.events[0];

        expect(event.outcome).toBe(AuditOutcome.SUCCESS);
        expect(event.durationMs).toBe(120000);
        expect(event.metadata.totalEvaluations).toBe(3000);
        expect(event.metadata.successCount).toBe(2950);
        expect(event.metadata.failureCount).toBe(50);

        req.flush({});
        discardPeriodicTasks();
      }));
    });

    describe('logCareGapAction()', () => {
      it('should log care gap address action', fakeAsync(() => {
        service.logCareGapAction({
          gapId: 'gap-1',
          patientId: 'patient-1',
          action: 'address',
          success: true,
        });

        tick(5000);

        const req = httpMock.expectOne('/audit/events');
        const event = req.request.body.events[0];

        expect(event.action).toBe(AuditAction.UPDATE);
        expect(event.resourceType).toBe('CareGap');
        expect(event.resourceId).toBe('gap-1');
        expect(event.purposeOfUse).toBe('TREATMENT');
        expect(event.metadata.patientId).toBe('patient-1');
        expect(event.metadata.gapAction).toBe('address');

        req.flush({});
        discardPeriodicTasks();
      }));
    });

    describe('logAccessDenied()', () => {
      it('should log access denied event', fakeAsync(() => {
        service.logAccessDenied({
          requestPath: '/admin/users',
          requiredRole: 'ADMIN',
        });

        tick(5000);

        const req = httpMock.expectOne('/audit/events');
        const event = req.request.body.events[0];

        expect(event.action).toBe(AuditAction.ACCESS_DENIED);
        expect(event.outcome).toBe(AuditOutcome.MINOR_FAILURE);
        expect(event.requestPath).toBe('/admin/users');
        expect(event.metadata.requiredRole).toBe('ADMIN');

        req.flush({});
        discardPeriodicTasks();
      }));
    });

    describe('logReportExport()', () => {
      it('should log report export', fakeAsync(() => {
        service.logReportExport({
          reportId: 'report-1',
          format: 'pdf',
          success: true,
        });

        tick(5000);

        const req = httpMock.expectOne('/audit/events');
        const event = req.request.body.events[0];

        expect(event.action).toBe(AuditAction.EXPORT);
        expect(event.resourceType).toBe('Report');
        expect(event.metadata.format).toBe('pdf');

        req.flush({});
        discardPeriodicTasks();
      }));
    });
  });

  describe('error handling', () => {
    it('should store events locally when batch fails', fakeAsync(() => {
      service.log({ action: AuditAction.READ });

      tick(5000);

      const req = httpMock.expectOne('/audit/events');
      req.error(new ErrorEvent('Network error'));

      tick();

      const stored = localStorage.getItem('healthdata_pending_audit_events');
      expect(stored).toBeTruthy();

      discardPeriodicTasks();
    }));

    it('should limit stored events to prevent storage bloat', fakeAsync(() => {
      // Simulate storing many events
      const manyEvents: Partial<AuditEvent>[] = [];
      for (let i = 0; i < 150; i++) {
        manyEvents.push({ id: `event-${i}`, action: AuditAction.READ, outcome: AuditOutcome.SUCCESS });
      }
      localStorage.setItem('healthdata_pending_audit_events', JSON.stringify(manyEvents));

      // Log more events
      service.log({ action: AuditAction.READ });

      tick(5000);

      const req = httpMock.expectOne('/audit/events');
      req.error(new ErrorEvent('Network error'));

      tick();

      const stored = localStorage.getItem('healthdata_pending_audit_events');
      const events = JSON.parse(stored!);
      expect(events.length).toBeLessThanOrEqual(100);

      discardPeriodicTasks();
    }));
  });
});
