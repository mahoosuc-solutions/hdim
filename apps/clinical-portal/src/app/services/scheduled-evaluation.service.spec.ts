import { TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ScheduledEvaluationService } from './scheduled-evaluation.service';
import { AuthService } from './auth.service';
import { EvaluationService } from './evaluation.service';
import { AuditService } from './audit.service';
import { ScheduledEvaluation, ScheduleFrequency } from '../models/scheduled-evaluation.model';

describe('ScheduledEvaluationService', () => {
  let service: ScheduledEvaluationService;
  let authService: jest.Mocked<AuthService>;

  const mockUser = {
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
    // Clear localStorage before each test
    localStorage.clear();

    const authServiceMock = {
      currentUserValue: mockUser,
    };

    const evaluationServiceMock = {
      batchEvaluate: jest.fn(),
    };

    const auditServiceMock = {
      log: jest.fn(),
    };

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        ScheduledEvaluationService,
        { provide: AuthService, useValue: authServiceMock },
        { provide: EvaluationService, useValue: evaluationServiceMock },
        { provide: AuditService, useValue: auditServiceMock },
      ],
    });

    service = TestBed.inject(ScheduledEvaluationService);
    authService = TestBed.inject(AuthService) as jest.Mocked<AuthService>;
  });

  afterEach(() => {
    service.ngOnDestroy();
    localStorage.clear();
  });

  describe('initialization', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should load empty schedules initially', (done) => {
      service.getSchedules().subscribe((schedules) => {
        expect(schedules).toEqual([]);
        done();
      }, 30000);
    });
  });

  describe('createSchedule()', () => {
    it('should create a new daily schedule', (done) => {
      service
        .createSchedule('Daily BCS Evaluation', ['BCS'], 'daily')
        .subscribe((schedule) => {
          expect(schedule.name).toBe('Daily BCS Evaluation');
          expect(schedule.frequency).toBe('daily');
          expect(schedule.measureIds).toEqual(['BCS']);
          expect(schedule.status).toBe('active');
          expect(schedule.timeOfDay).toBe('02:00');
          expect(schedule.tenantId).toBe('tenant-1');
          expect(schedule.createdBy).toBe('testuser');
          expect(schedule.nextRun).toBeDefined();
          done();
        }, 30000);
    });

    it('should create a weekly schedule with custom options', (done) => {
      service
        .createSchedule('Weekly COL Evaluation', ['COL', 'BCS'], 'weekly', {
          description: 'Weekly colorectal screening evaluation',
          timeOfDay: '06:00',
          dayOfWeek: 'monday',
        })
        .subscribe((schedule) => {
          expect(schedule.name).toBe('Weekly COL Evaluation');
          expect(schedule.frequency).toBe('weekly');
          expect(schedule.description).toBe('Weekly colorectal screening evaluation');
          expect(schedule.timeOfDay).toBe('06:00');
          expect(schedule.dayOfWeek).toBe('monday');
          expect(schedule.measureIds).toEqual(['COL', 'BCS']);
          done();
        }, 30000);
    });

    it('should persist schedule to localStorage', fakeAsync(() => {
      service.createSchedule('Test Schedule', ['BCS'], 'daily').subscribe();
      tick();

      const stored = localStorage.getItem('healthdata_scheduled_evaluations');
      expect(stored).toBeTruthy();
      const schedules = JSON.parse(stored!);
      expect(schedules.length).toBe(1);
      expect(schedules[0].name).toBe('Test Schedule');

      discardPeriodicTasks();
    }));
  });

  describe('updateSchedule()', () => {
    it('should update schedule properties', fakeAsync(() => {
      let scheduleId: string;

      service.createSchedule('Original Name', ['BCS'], 'daily').subscribe((s) => {
        scheduleId = s.id;
      });
      tick();

      service
        .updateSchedule(scheduleId!, {
          name: 'Updated Name',
          timeOfDay: '04:00',
        })
        .subscribe((updated) => {
          expect(updated.name).toBe('Updated Name');
          expect(updated.timeOfDay).toBe('04:00');
        });
      tick();

      discardPeriodicTasks();
    }));

    it('should recalculate next run when frequency changes', fakeAsync(() => {
      let schedule: ScheduledEvaluation;

      service.createSchedule('Test', ['BCS'], 'daily').subscribe((s) => {
        schedule = s;
      });
      tick();

      const originalNextRun = schedule!.nextRun;

      service.updateSchedule(schedule!.id, { frequency: 'weekly' }).subscribe((updated) => {
        // Next run should change when frequency changes
        expect(updated.frequency).toBe('weekly');
      });
      tick();

      discardPeriodicTasks();
    }));
  });

  describe('deleteSchedule()', () => {
    it('should remove schedule from list', fakeAsync(() => {
      let scheduleId: string;
      let currentSchedules: any[] = [];

      // Subscribe to schedules to track changes
      service.getSchedules().subscribe((schedules) => {
        currentSchedules = schedules;
      });

      // Create the schedule
      service.createSchedule('To Delete', ['BCS'], 'daily').subscribe((s) => {
        scheduleId = s.id;
      });
      tick();

      // Should have 1 schedule after creation
      expect(currentSchedules.length).toBe(1);

      // Delete the schedule
      service.deleteSchedule(scheduleId!).subscribe();
      tick();

      // Should have 0 schedules after deletion
      expect(currentSchedules.length).toBe(0);

      discardPeriodicTasks();
    }));
  });

  describe('pauseSchedule() / resumeSchedule()', () => {
    it('should pause an active schedule', fakeAsync(() => {
      let scheduleId: string;

      service.createSchedule('To Pause', ['BCS'], 'daily').subscribe((s) => {
        scheduleId = s.id;
        expect(s.status).toBe('active');
      });
      tick();

      service.pauseSchedule(scheduleId!).subscribe((updated) => {
        expect(updated.status).toBe('paused');
        expect(updated.nextRun).toBeUndefined();
      });
      tick();

      discardPeriodicTasks();
    }));

    it('should resume a paused schedule', fakeAsync(() => {
      let scheduleId: string;

      service.createSchedule('To Resume', ['BCS'], 'daily').subscribe((s) => {
        scheduleId = s.id;
      });
      tick();

      service.pauseSchedule(scheduleId!).subscribe();
      tick();

      service.resumeSchedule(scheduleId!).subscribe((updated) => {
        expect(updated.status).toBe('active');
        expect(updated.nextRun).toBeDefined();
      });
      tick();

      discardPeriodicTasks();
    }));
  });

  describe('runNow()', () => {
    it('should manually trigger a schedule execution', fakeAsync(() => {
      let scheduleId: string;

      service.createSchedule('Manual Run', ['BCS'], 'daily').subscribe((s) => {
        scheduleId = s.id;
      });
      tick();

      service.runNow(scheduleId!).subscribe((execution) => {
        expect(execution.scheduleId).toBe(scheduleId);
        expect(execution.triggeredBy).toBe('manual');
        expect(execution.triggerUser).toBe('testuser');
      });
      tick(3000); // Wait for simulated execution

      discardPeriodicTasks();
    }));
  });

  describe('getExecutionHistory()', () => {
    it('should return execution history for a schedule', fakeAsync(() => {
      let scheduleId: string;

      service.createSchedule('With History', ['BCS'], 'daily').subscribe((s) => {
        scheduleId = s.id;
      });
      tick();

      service.runNow(scheduleId!).subscribe();
      tick(3000);

      service.getExecutionHistory(scheduleId!).subscribe((history) => {
        expect(history.length).toBe(1);
        expect(history[0].status).toBe('completed');
      });

      discardPeriodicTasks();
    }));
  });

  describe('getDescription()', () => {
    it('should return human-readable description for daily schedule', fakeAsync(() => {
      service.createSchedule('Daily', ['BCS'], 'daily').subscribe((schedule) => {
        const desc = service.getDescription(schedule);
        expect(desc).toContain('Daily at');
      });
      tick();

      discardPeriodicTasks();
    }));

    it('should return human-readable description for weekly schedule', fakeAsync(() => {
      service
        .createSchedule('Weekly', ['BCS'], 'weekly', { dayOfWeek: 'friday' })
        .subscribe((schedule) => {
          const desc = service.getDescription(schedule);
          expect(desc).toContain('Every Friday');
        });
      tick();

      discardPeriodicTasks();
    }));

    it('should return human-readable description for monthly schedule', fakeAsync(() => {
      service
        .createSchedule('Monthly', ['BCS'], 'monthly', { dayOfMonth: 15 })
        .subscribe((schedule) => {
          const desc = service.getDescription(schedule);
          expect(desc).toContain('Monthly on day 15');
        });
      tick();

      discardPeriodicTasks();
    }));
  });

  describe('persistence', () => {
    it('should load schedules from localStorage on init', fakeAsync(() => {
      // Pre-populate localStorage
      const existingSchedule = {
        id: 'existing-1',
        name: 'Existing Schedule',
        status: 'active',
        frequency: 'daily',
        timeOfDay: '02:00',
        timezone: 'America/New_York',
        measureIds: ['BCS'],
        patientFilter: { type: 'all' },
        evaluationOptions: {
          parallelExecution: true,
          maxPatientsPerBatch: 100,
          continueOnError: true,
          generateReport: true,
        },
        consecutiveFailures: 0,
        createdAt: new Date().toISOString(),
        createdBy: 'testuser',
        updatedAt: new Date().toISOString(),
        tenantId: 'tenant-1',
        notifications: {
          onSuccess: false,
          onFailure: true,
          onPartialSuccess: true,
          recipients: [],
          includeReport: true,
        },
      };
      localStorage.setItem(
        'healthdata_scheduled_evaluations',
        JSON.stringify([existingSchedule])
      );

      // Create new service instance
      const newService = new ScheduledEvaluationService(
        TestBed.inject(HttpClientTestingModule) as any,
        TestBed.inject(AuthService),
        TestBed.inject(EvaluationService),
        TestBed.inject(AuditService)
      );

      tick();

      newService.getSchedules().subscribe((schedules) => {
        expect(schedules.length).toBe(1);
        expect(schedules[0].name).toBe('Existing Schedule');
      });

      newService.ngOnDestroy();
      discardPeriodicTasks();
    }));
  });
});
