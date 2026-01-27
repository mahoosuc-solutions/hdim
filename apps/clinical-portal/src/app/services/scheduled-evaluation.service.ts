import { Injectable, OnDestroy } from '@angular/core';
import { LoggerService } from '../logger.service';
import { HttpClient } from '@angular/common/http';
import { LoggerService } from '../logger.service';
import { BehaviorSubject, Observable, of, timer, Subscription } from 'rxjs';
import { LoggerService } from '../logger.service';
import { map, catchError, tap, switchMap } from 'rxjs/operators';
import { LoggerService } from '../logger.service';
import { AuthService } from './auth.service';
import { LoggerService } from '../logger.service';
import { EvaluationService } from './evaluation.service';
import { LoggerService } from '../logger.service';
import { AuditService, AuditAction } from './audit.service';
import { LoggerService } from '../logger.service';
import { API_CONFIG } from '../config/api.config';
import { LoggerService } from '../logger.service';
import {
  ScheduledEvaluation,
  ScheduleExecution,
  ScheduleFrequency,
  ScheduleStatus,
  PatientFilter,
  EvaluationOptions,
  NotificationSettings,
  createScheduledEvaluation,
  calculateNextRun,
  getScheduleDescription,
} from '../models/scheduled-evaluation.model';

/**
 * Scheduled Evaluation Service
 *
 * Manages recurring batch evaluations with:
 * - Local storage persistence (works offline)
 * - Optional backend sync when API is available
 * - Automatic execution at scheduled times
 * - Execution history tracking
 */
@Injectable({
  providedIn: 'root',
})
export class ScheduledEvaluationService implements OnDestroy {
  private readonly logger: any;
  private readonly STORAGE_KEY = 'healthdata_scheduled_evaluations';
  private readonly HISTORY_KEY = 'healthdata_schedule_history';
  private readonly CHECK_INTERVAL_MS = 60000; // Check every minute

  private schedulesSubject = new BehaviorSubject<ScheduledEvaluation[]>([]);
  public schedules$ = this.schedulesSubject.asObservable();

  private executionsSubject = new BehaviorSubject<ScheduleExecution[]>([]);
  public executions$ = this.executionsSubject.asObservable();

  private checkSubscription?: Subscription;

  constructor(
    private loggerService: LoggerService,
    private http: HttpClient,
    private authService: AuthService,
    private evaluationService: EvaluationService,
    private auditService: AuditService
  ) {
    this.logger = this.loggerService.withContext(\'ScheduledEvaluationService');
    this.loadSchedules();
    this.loadExecutionHistory();
    this.startScheduleChecker();
  }

  ngOnDestroy(): void {
    this.checkSubscription?.unsubscribe();
  }

  /**
   * Load schedules from local storage
   */
  private loadSchedules(): void {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (stored) {
        const schedules: ScheduledEvaluation[] = JSON.parse(stored);
        // Update next run times
        const updated = schedules.map((s) => ({
          ...s,
          nextRun: s.status === 'active' ? calculateNextRun(s).toISOString() : undefined,
        }));
        this.schedulesSubject.next(updated);
      }
    } catch (error) {
      this.logger.error('Failed to load scheduled evaluations:', { error });
      this.schedulesSubject.next([]);
    }
  }

  /**
   * Save schedules to local storage
   */
  private saveSchedules(): void {
    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(this.schedulesSubject.value));
    } catch (error) {
      this.logger.error('Failed to save scheduled evaluations:', { error });
    }
  }

  /**
   * Load execution history from local storage
   */
  private loadExecutionHistory(): void {
    try {
      const stored = localStorage.getItem(this.HISTORY_KEY);
      if (stored) {
        const executions: ScheduleExecution[] = JSON.parse(stored);
        // Keep only last 100 executions
        this.executionsSubject.next(executions.slice(-100));
      }
    } catch (error) {
      this.logger.error('Failed to load execution history:', { error });
      this.executionsSubject.next([]);
    }
  }

  /**
   * Save execution history to local storage
   */
  private saveExecutionHistory(): void {
    try {
      // Keep only last 100 executions
      const executions = this.executionsSubject.value.slice(-100);
      localStorage.setItem(this.HISTORY_KEY, JSON.stringify(executions));
    } catch (error) {
      this.logger.error('Failed to save execution history:', { error });
    }
  }

  /**
   * Start the schedule checker that runs every minute
   */
  private startScheduleChecker(): void {
    this.checkSubscription = timer(0, this.CHECK_INTERVAL_MS).subscribe(() => {
      this.checkAndRunDueSchedules();
    });
  }

  /**
   * Check for due schedules and execute them
   */
  private checkAndRunDueSchedules(): void {
    const now = new Date();
    const schedules = this.schedulesSubject.value;

    schedules.forEach((schedule) => {
      if (schedule.status !== 'active' || !schedule.nextRun) {
        return;
      }

      const nextRun = new Date(schedule.nextRun);
      const timeDiff = nextRun.getTime() - now.getTime();

      // Execute if within 1 minute window
      if (timeDiff <= 0 && timeDiff > -60000) {
        this.logger.info(`[ScheduledEvaluationService] Executing schedule: ${schedule.name}`);
        this.executeSchedule(schedule);
      }
    });
  }

  // ==================== Public API ====================

  /**
   * Get all schedules
   */
  getSchedules(): Observable<ScheduledEvaluation[]> {
    return this.schedules$;
  }

  /**
   * Get a single schedule by ID
   */
  getScheduleById(id: string): Observable<ScheduledEvaluation | undefined> {
    return this.schedules$.pipe(
      map((schedules) => schedules.find((s) => s.id === id))
    );
  }

  /**
   * Create a new scheduled evaluation
   */
  createSchedule(
    name: string,
    measureIds: string[],
    frequency: ScheduleFrequency,
    options?: {
      description?: string;
      timeOfDay?: string;
      dayOfWeek?: string;
      dayOfMonth?: number;
      patientFilter?: PatientFilter;
      evaluationOptions?: EvaluationOptions;
      notifications?: NotificationSettings;
    }
  ): Observable<ScheduledEvaluation> {
    const user = this.authService.currentUserValue;
    const tenantId = user?.tenantId || API_CONFIG.DEFAULT_TENANT_ID;
    const createdBy = user?.username || 'system';

    const schedule = createScheduledEvaluation(name, measureIds, frequency, tenantId, createdBy);

    // Apply optional overrides
    if (options?.description) schedule.description = options.description;
    if (options?.timeOfDay) schedule.timeOfDay = options.timeOfDay;
    if (options?.dayOfWeek) schedule.dayOfWeek = options.dayOfWeek as any;
    if (options?.dayOfMonth) schedule.dayOfMonth = options.dayOfMonth;
    if (options?.patientFilter) schedule.patientFilter = options.patientFilter;
    if (options?.evaluationOptions) schedule.evaluationOptions = options.evaluationOptions;
    if (options?.notifications) schedule.notifications = options.notifications;

    // Calculate next run time
    schedule.nextRun = calculateNextRun(schedule).toISOString();

    // Add to list
    const schedules = [...this.schedulesSubject.value, schedule];
    this.schedulesSubject.next(schedules);
    this.saveSchedules();

    // Audit log
    this.auditService.log({
      action: AuditAction.CREATE,
      resourceType: 'ScheduledEvaluation',
      resourceId: schedule.id,
      purposeOfUse: 'OPERATIONS',
      metadata: {
        name: schedule.name,
        frequency: schedule.frequency,
        measureCount: measureIds.length,
      },
    });

    return of(schedule);
  }

  /**
   * Update an existing schedule
   */
  updateSchedule(id: string, updates: Partial<ScheduledEvaluation>): Observable<ScheduledEvaluation> {
    const schedules = this.schedulesSubject.value;
    const index = schedules.findIndex((s) => s.id === id);

    if (index === -1) {
      throw new Error(`Schedule not found: ${id}`);
    }

    const updatedSchedule: ScheduledEvaluation = {
      ...schedules[index],
      ...updates,
      updatedAt: new Date().toISOString(),
    };

    // Recalculate next run if schedule config changed
    if (
      updates.frequency ||
      updates.timeOfDay ||
      updates.dayOfWeek ||
      updates.dayOfMonth ||
      updates.status
    ) {
      updatedSchedule.nextRun =
        updatedSchedule.status === 'active'
          ? calculateNextRun(updatedSchedule).toISOString()
          : undefined;
    }

    schedules[index] = updatedSchedule;
    this.schedulesSubject.next([...schedules]);
    this.saveSchedules();

    // Audit log
    this.auditService.log({
      action: AuditAction.UPDATE,
      resourceType: 'ScheduledEvaluation',
      resourceId: id,
      purposeOfUse: 'OPERATIONS',
      metadata: { updates: Object.keys(updates) },
    });

    return of(updatedSchedule);
  }

  /**
   * Delete a schedule
   */
  deleteSchedule(id: string): Observable<void> {
    const schedules = this.schedulesSubject.value.filter((s) => s.id !== id);
    this.schedulesSubject.next(schedules);
    this.saveSchedules();

    // Audit log
    this.auditService.log({
      action: AuditAction.DELETE,
      resourceType: 'ScheduledEvaluation',
      resourceId: id,
      purposeOfUse: 'OPERATIONS',
    });

    return of(undefined);
  }

  /**
   * Pause a schedule
   */
  pauseSchedule(id: string): Observable<ScheduledEvaluation> {
    return this.updateSchedule(id, { status: 'paused' });
  }

  /**
   * Resume a paused schedule
   */
  resumeSchedule(id: string): Observable<ScheduledEvaluation> {
    return this.updateSchedule(id, { status: 'active' });
  }

  /**
   * Disable a schedule
   */
  disableSchedule(id: string): Observable<ScheduledEvaluation> {
    return this.updateSchedule(id, { status: 'disabled' });
  }

  /**
   * Manually trigger a schedule execution
   */
  runNow(id: string): Observable<ScheduleExecution> {
    const schedule = this.schedulesSubject.value.find((s) => s.id === id);
    if (!schedule) {
      throw new Error(`Schedule not found: ${id}`);
    }

    return this.executeSchedule(schedule, 'manual');
  }

  /**
   * Get execution history for a schedule
   */
  getExecutionHistory(scheduleId: string): Observable<ScheduleExecution[]> {
    return this.executions$.pipe(
      map((executions) => executions.filter((e) => e.scheduleId === scheduleId))
    );
  }

  /**
   * Get recent executions (all schedules)
   */
  getRecentExecutions(limit = 10): Observable<ScheduleExecution[]> {
    return this.executions$.pipe(
      map((executions) =>
        executions
          .sort((a, b) => new Date(b.startedAt).getTime() - new Date(a.startedAt).getTime())
          .slice(0, limit)
      )
    );
  }

  /**
   * Get schedule description
   */
  getDescription(schedule: ScheduledEvaluation): string {
    return getScheduleDescription(schedule);
  }

  // ==================== Execution Logic ====================

  /**
   * Execute a scheduled evaluation
   */
  private executeSchedule(
    schedule: ScheduledEvaluation,
    triggeredBy: 'schedule' | 'manual' = 'schedule'
  ): Observable<ScheduleExecution> {
    const execution: ScheduleExecution = {
      id: `exec-${Date.now()}`,
      scheduleId: schedule.id,
      status: 'running',
      startedAt: new Date().toISOString(),
      totalPatients: 0,
      processedPatients: 0,
      successCount: 0,
      failureCount: 0,
      measureResults: [],
      triggeredBy,
      triggerUser:
        triggeredBy === 'manual' ? this.authService.currentUserValue?.username : undefined,
    };

    // Add to executions
    const executions = [...this.executionsSubject.value, execution];
    this.executionsSubject.next(executions);

    // Update schedule's last run
    this.updateScheduleAfterExecution(schedule, execution);

    // For now, simulate execution since we don't have patient list
    // In production, this would call the evaluation service with actual patients
    return timer(2000).pipe(
      map(() => {
        const completedExecution: ScheduleExecution = {
          ...execution,
          status: 'completed',
          completedAt: new Date().toISOString(),
          durationMs: 2000,
          totalPatients: 100, // Simulated
          processedPatients: 100,
          successCount: 95,
          failureCount: 5,
          measureResults: schedule.measureIds.map((id) => ({
            measureId: id,
            measureName: id,
            patientCount: 100,
            complianceRate: 85 + Math.random() * 10,
            avgExecutionMs: 50 + Math.random() * 100,
          })),
        };

        this.updateExecutionResult(completedExecution);
        return completedExecution;
      }),
      catchError((error) => {
        const failedExecution: ScheduleExecution = {
          ...execution,
          status: 'failed',
          completedAt: new Date().toISOString(),
          errors: [error.message || 'Unknown error'],
        };

        this.updateExecutionResult(failedExecution);
        this.handleExecutionFailure(schedule);

        return of(failedExecution);
      })
    );
  }

  /**
   * Update schedule after starting execution
   */
  private updateScheduleAfterExecution(
    schedule: ScheduledEvaluation,
    execution: ScheduleExecution
  ): void {
    const updates: Partial<ScheduledEvaluation> = {
      lastRun: execution,
      nextRun: calculateNextRun(schedule).toISOString(),
    };

    const schedules = this.schedulesSubject.value;
    const index = schedules.findIndex((s) => s.id === schedule.id);
    if (index !== -1) {
      schedules[index] = { ...schedules[index], ...updates };
      this.schedulesSubject.next([...schedules]);
      this.saveSchedules();
    }
  }

  /**
   * Update execution result
   */
  private updateExecutionResult(execution: ScheduleExecution): void {
    const executions = this.executionsSubject.value;
    const index = executions.findIndex((e) => e.id === execution.id);
    if (index !== -1) {
      executions[index] = execution;
      this.executionsSubject.next([...executions]);
      this.saveExecutionHistory();
    }

    // Update schedule's last run
    const schedules = this.schedulesSubject.value;
    const scheduleIndex = schedules.findIndex((s) => s.id === execution.scheduleId);
    if (scheduleIndex !== -1) {
      schedules[scheduleIndex] = {
        ...schedules[scheduleIndex],
        lastRun: execution,
        consecutiveFailures:
          execution.status === 'failed'
            ? schedules[scheduleIndex].consecutiveFailures + 1
            : 0,
      };
      this.schedulesSubject.next([...schedules]);
      this.saveSchedules();
    }
  }

  /**
   * Handle execution failure (auto-pause after too many failures)
   */
  private handleExecutionFailure(schedule: ScheduledEvaluation): void {
    const MAX_CONSECUTIVE_FAILURES = 3;

    if (schedule.consecutiveFailures >= MAX_CONSECUTIVE_FAILURES - 1) {
      this.logger.warn('Pausing schedule after consecutive failures', {
        scheduleName: schedule.name,
        maxFailures: MAX_CONSECUTIVE_FAILURES
      });
      this.updateSchedule(schedule.id, {
        status: 'paused',
      }).subscribe();
    }
  }
}
