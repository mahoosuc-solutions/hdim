/**
 * Medical Assistant Dashboard Component
 *
 * Optimized for MA workflows:
 * - Patient check-in and preparation
 * - Vital signs recording
 * - Room management
 * - Today's schedule overview
 */

import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatBadgeModule } from '@angular/material/badge';
import { Subject, takeUntil, forkJoin } from 'rxjs';
import { PatientService } from '../../../services/patient.service';
import { EvaluationService } from '../../../services/evaluation.service';
import { CareGapService, CareGap, CareGapStatus, GapPriority } from '../../../services/care-gap.service';
import { ToastService } from '../../../services/toast.service';
import { SchedulingService, ScheduleAppointment, ScheduleTask } from '../../../services/scheduling.service';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { DialogService } from '../../../services/dialog.service';
import { NotificationService } from '../../../services/notification.service';
import { TrackInteraction } from '../../../utils/ai-tracking.decorator';
import { LoggerService } from '../../../services/logger.service';

export interface MATaskItem {
  id: string;
  patientName: string;
  patientMRN: string;
  appointmentTime: string;
  taskType: 'check-in' | 'vitals' | 'prep' | 'complete';
  status: 'pending' | 'in-progress' | 'completed';
  priority: 'high' | 'normal' | 'low';
  room?: string;
  ownerName?: string;
  practitionerName?: string;
}

export interface PreVisitTask {
  id: string;
  patientId: string;
  task: string;
  completed: boolean;
  priority: 'high' | 'normal' | 'low';
  dueTime?: string;
}

export interface PreVisitSummary {
  totalTasks: number;
  completedTasks: number;
  pendingTasks: number;
  completionRate: number;
}

export interface OutreachItem {
  id: string;
  patientId: string;
  patientName: string;
  reason: string;
  status: 'pending' | 'in-progress' | 'completed' | 'cancelled';
  priority: 'high' | 'normal' | 'low';
  scheduledDate: string;
  lastAttempt?: string;
  outcome?: string;
  notes?: string;
}

@Component({
  selector: 'app-ma-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatBadgeModule,
    StatCardComponent,
    PageHeaderComponent,
    EmptyStateComponent
  ],
  templateUrl: './ma-dashboard.component.html',
  styleUrls: ['./ma-dashboard.component.scss']
})
export class MADashboardComponent implements OnInit, OnDestroy {
  loading = true;
  todaySchedule: MATaskItem[] = [];
  displayedColumns = ['time', 'patient', 'provider', 'task', 'room', 'status', 'actions'];

  // Dashboard metrics
  patientsScheduledToday = 0;
  patientsCheckedIn = 0;
  vitalsPending = 0;
  roomsReady = 0;

  // Phase 6.1: New workflow data
  preVisitTasks: PreVisitTask[] = [];
  careGaps: CareGap[] = [];
  outreachItems: OutreachItem[] = [];

  private destroy$ = new Subject<void>();  constructor(
    private router: Router,
    private patientService: PatientService,
    private evaluationService: EvaluationService,
    private careGapService: CareGapService,
    private schedulingService: SchedulingService,
    private dialogService: DialogService,
    private notificationService: NotificationService,
    private toastService: ToastService,
    private logger: LoggerService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load MA dashboard data
   */
  private loadDashboardData(): void {
    this.loading = true;

    // Load today's schedule
    this.loadTodaySchedule();
  }

  /**
   * Load today's patient schedule
   */
  private loadTodaySchedule(): void {
    const today = new Date();
    forkJoin({
      appointments: this.schedulingService.getAppointmentsForDate(today),
      tasks: this.schedulingService.getTasksForDate(today)
    }).pipe(takeUntil(this.destroy$)).subscribe(({ appointments, tasks }) => {
      this.todaySchedule = this.buildScheduleItems(appointments, tasks);
      this.updateMetricsFromSchedule();
      this.loading = false;
    });
  }

  private buildScheduleItems(
    appointments: ScheduleAppointment[],
    tasks: ScheduleTask[]
  ): MATaskItem[] {
    if (appointments.length === 0 && tasks.length === 0) {
      return [];
    }

    const appointmentByPatient = new Map<string, ScheduleAppointment>();
    appointments.forEach((appointment) => {
      if (appointment.patientId) {
        appointmentByPatient.set(appointment.patientId, appointment);
      }
    });

    const itemsFromTasks = tasks.map((task) => {
      const appointment = appointmentByPatient.get(task.patientId);
      const appointmentTime = appointment?.start
        ? this.formatTime(appointment.start)
        : task.scheduledStart
          ? this.formatTime(task.scheduledStart)
          : 'TBD';
      return {
        id: task.id,
        patientName: task.patientName,
        patientMRN: task.patientMRN,
        appointmentTime,
        taskType: this.mapTaskType(task.type),
        status: this.mapTaskStatus(task.status),
        priority: task.priority,
        room: appointment ? this.assignRoomFromTime(appointment.start) : undefined,
        ownerName: task.ownerName,
        practitionerName: appointment?.practitionerName,
      };
    });

    if (itemsFromTasks.length > 0) {
      return itemsFromTasks.sort((a, b) => a.appointmentTime.localeCompare(b.appointmentTime));
    }

    return appointments.map((appointment, index) => ({
      id: appointment.id,
      patientName: appointment.patientName,
      patientMRN: appointment.patientMRN,
      appointmentTime: this.formatTime(appointment.start),
      taskType: 'check-in',
      status: this.mapAppointmentStatus(appointment.status),
      priority: index === 0 ? 'high' : 'normal',
      room: this.assignRoomFromTime(appointment.start),
      practitionerName: appointment.practitionerName,
    }));
  }

  private updateMetricsFromSchedule(): void {
    this.patientsScheduledToday = this.todaySchedule.length;
    this.patientsCheckedIn = this.todaySchedule.filter((task) => task.status === 'completed').length;
    this.vitalsPending = this.todaySchedule.filter((task) => task.taskType === 'vitals' && task.status !== 'completed').length;
    this.roomsReady = Math.max(0, this.todaySchedule.length - this.patientsCheckedIn);
  }

  private mapTaskType(type: string): MATaskItem['taskType'] {
    const normalized = type.toLowerCase();
    if (normalized.includes('vital')) return 'vitals';
    if (normalized.includes('prep')) return 'prep';
    if (normalized.includes('complete')) return 'complete';
    return 'check-in';
  }

  private mapTaskStatus(status: string): MATaskItem['status'] {
    const normalized = status.toLowerCase();
    if (normalized === 'completed') return 'completed';
    if (normalized === 'in-progress') return 'in-progress';
    if (normalized === 'accepted') return 'in-progress';
    return 'pending';
  }

  private mapAppointmentStatus(status: string): MATaskItem['status'] {
    const normalized = status.toLowerCase();
    if (normalized === 'fulfilled' || normalized === 'completed') return 'completed';
    if (normalized === 'arrived' || normalized === 'checked-in') return 'in-progress';
    return 'pending';
  }

  private assignRoomFromTime(date?: Date): string | undefined {
    if (!date) return undefined;
    const hour = date.getHours();
    if (hour < 10) return 'Room 1';
    if (hour < 12) return 'Room 2';
    return 'Room 3';
  }

  private formatTime(date: Date): string {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  /**
   * Handle patient check-in
   */
  checkInPatient(task: MATaskItem): void {
    this.dialogService.confirm(
      'Check In Patient',
      `Confirm check-in for <strong>${task.patientName}</strong> (${task.patientMRN})?<br><br>` +
      `Appointment: ${task.appointmentTime}<br>Room: ${task.room || 'Not assigned'}`,
      'Check In',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        this.updateTaskStatus(task.id, 'completed');
        task.taskType = 'vitals'; // Move to next step
        this.patientsCheckedIn++;
        this.notificationService.success(`${task.patientName} has been checked in`);
      }
    });
  }

  /**
   * Record vital signs
   */
  recordVitals(task: MATaskItem): void {
    // Navigate to patient detail with vitals recording action
    this.router.navigate(['/patients', task.id], {
      queryParams: { action: 'record-vitals' }
    });
    this.notificationService.info(`Opening vitals recording for ${task.patientName}`);
  }

  /**
   * Prepare exam room
   */
  prepareRoom(task: MATaskItem): void {
    this.dialogService.confirm(
      'Prepare Exam Room',
      `Mark ${task.room} as ready for <strong>${task.patientName}</strong>?<br><br>` +
      `Please ensure the room is clean and all necessary supplies are available.`,
      'Mark Ready',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (confirmed) {
        this.updateTaskStatus(task.id, 'in-progress');
        this.roomsReady++;
        this.notificationService.success(`${task.room} is now ready for ${task.patientName}`);
      }
    });
  }

  /**
   * Navigate to patient detail
   */
  viewPatient(task: MATaskItem): void {
    this.router.navigate(['/patients', task.id]);
  }

  /**
   * Update task status
   */
  private updateTaskStatus(taskId: string, status: MATaskItem['status']): void {
    const task = this.todaySchedule.find(t => t.id === taskId);
    if (task) {
      task.status = status;
      this.updateMetricsFromSchedule(); // Refresh metrics
    }
  }

  /**
   * Get status color
   */
  getStatusColor(status: string): string {
    switch (status) {
      case 'completed': return 'success';
      case 'in-progress': return 'warn';
      case 'pending': return 'primary';
      default: return '';
    }
  }

  /**
   * Get priority icon
   */
  getPriorityIcon(priority: string): string {
    return priority === 'high' ? 'priority_high' : 'schedule';
  }

  /**
   * Get task type icon
   */
  getTaskIcon(taskType: string): string {
    switch (taskType) {
      case 'check-in': return 'how_to_reg';
      case 'vitals': return 'favorite';
      case 'prep': return 'meeting_room';
      case 'complete': return 'check_circle';
      default: return 'task';
    }
  }

  /**
   * Get task type label
   */
  getTaskLabel(taskType: string): string {
    switch (taskType) {
      case 'check-in': return 'Check-in';
      case 'vitals': return 'Record Vitals';
      case 'prep': return 'Prep Room';
      case 'complete': return 'Complete';
      default: return taskType;
    }
  }

  /**
   * Quick action: View all patients
   */
  viewAllPatients(): void {
    this.router.navigate(['/patients']);
  }

  /**
   * Quick action: View schedule
   */
  viewSchedule(): void {
    // Navigate to dashboard with schedule view filter
    this.router.navigate(['/dashboard'], {
      queryParams: { view: 'schedule' }
    });
  }

  /**
   * Refresh dashboard data
   */
  refreshData(): void {
    this.loadDashboardData();
  }

  // ============================================================================
  // Phase 6.1: Patient Scheduling Workflows
  // ============================================================================

  /**
   * Load today's patient schedule
   */
  @TrackInteraction('ma-dashboard', 'load-schedule')
  loadScheduleData(): void {
    // In production, this would call PatientService.getTodaySchedule()
    // For now, use the existing loadTodaySchedule() method with mock data
    this.loadTodaySchedule();
  }

  /**
   * Get tasks filtered by status
   */
  getTasksByStatus(status: string): MATaskItem[] {
    return this.todaySchedule.filter(task => task.status === status);
  }

  /**
   * Reschedule an appointment
   */
  @TrackInteraction('ma-dashboard', 'reschedule-appointment')
  rescheduleAppointment(task: MATaskItem): void {
    // Use confirm dialog as a placeholder - in production, would use a custom time picker dialog
    this.dialogService.confirm(
      'Reschedule Appointment',
      `Would you like to reschedule the appointment for <strong>${task.patientName}</strong>?<br><br>Current time: ${task.appointmentTime}`,
      'Reschedule',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (confirmed) => {
          if (confirmed) {
            // In production, this would open a time picker and update the appointment
            this.toastService.success('Appointment rescheduling initiated');
          }
        },
        error: (error) => {
          this.logger.error('Error rescheduling appointment', error);
          this.toastService.error('Failed to reschedule appointment');
        }
      });
  }

  /**
   * Confirm an appointment
   */
  @TrackInteraction('ma-dashboard', 'confirm-appointment')
  confirmAppointment(task: MATaskItem): void {
    this.dialogService.confirm(
      'Confirm Appointment',
      `Confirm appointment for <strong>${task.patientName}</strong>?<br><br>Time: ${task.appointmentTime}`,
      'Confirm',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (confirmed) => {
          if (confirmed) {
            // In production, would update appointment status via API
            task.status = 'completed';
            this.toastService.success(`Appointment confirmed for ${task.patientName}`);
          }
        }
      });
  }

  /**
   * Cancel an appointment
   */
  @TrackInteraction('ma-dashboard', 'cancel-appointment')
  cancelAppointment(task: MATaskItem): void {
    this.dialogService.confirmWarning(
      'Cancel Appointment',
      `Are you sure you want to cancel the appointment for <strong>${task.patientName}</strong>?<br><br>` +
      `Time: ${task.appointmentTime}<br><br>This action cannot be undone.`
    ).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (confirmed) => {
          if (confirmed) {
            this.toastService.info('Appointment cancelled');
            // Remove from schedule or mark as cancelled
            const index = this.todaySchedule.indexOf(task);
            if (index > -1) {
              this.todaySchedule.splice(index, 1);
              this.patientsScheduledToday = this.todaySchedule.length;
            }
          }
        }
      });
  }

  // ============================================================================
  // Phase 6.1: Pre-visit Preparation Tasks
  // ============================================================================

  /**
   * Load pre-visit checklist for a patient
   */
  @TrackInteraction('ma-dashboard', 'load-previsit-checklist')
  loadPreVisitChecklist(patientId: string): void {
    // Mock implementation - in production, this would call an API
    this.preVisitTasks = [
      {
        id: '1',
        patientId,
        task: 'Review medical history',
        completed: false,
        priority: 'high',
      },
      {
        id: '2',
        patientId,
        task: 'Verify insurance eligibility',
        completed: false,
        priority: 'high',
      },
      {
        id: '3',
        patientId,
        task: 'Prepare vitals equipment',
        completed: false,
        priority: 'normal',
      },
      {
        id: '4',
        patientId,
        task: 'Review care gaps',
        completed: false,
        priority: 'high',
      },
    ];
  }

  /**
   * Mark pre-visit task as complete
   */
  @TrackInteraction('ma-dashboard', 'complete-previsit-task')
  completePreVisitTask(task: PreVisitTask): void {
    task.completed = true;
    this.toastService.success(`Task completed: ${task.task}`);
  }

  /**
   * Add custom pre-visit task
   */
  @TrackInteraction('ma-dashboard', 'add-previsit-task')
  addPreVisitTask(patientId: string): void {
    // In production, this would open a custom dialog to add a task
    // For now, use confirm as placeholder
    this.dialogService.confirm(
      'Add Pre-visit Task',
      'Would you like to add a custom pre-visit task?',
      'Add Task',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (confirmed) => {
          if (confirmed) {
            const newTask: PreVisitTask = {
              id: `custom-${Date.now()}`,
              patientId,
              task: 'Custom task',
              completed: false,
              priority: 'normal',
            };
            this.preVisitTasks.push(newTask);
            this.toastService.success('Task added to checklist');
          }
        }
      });
  }

  /**
   * Get pre-visit completion rate
   */
  getPreVisitCompletionRate(): number {
    if (this.preVisitTasks.length === 0) return 0;
    const completed = this.preVisitTasks.filter(t => t.completed).length;
    return (completed / this.preVisitTasks.length) * 100;
  }

  /**
   * Get critical incomplete tasks
   */
  getCriticalIncompleteTasks(): PreVisitTask[] {
    return this.preVisitTasks.filter(
      task => task.priority === 'high' && !task.completed
    );
  }

  /**
   * Get pre-visit summary for a patient
   */
  getPreVisitSummary(patientId: string): PreVisitSummary {
    const patientTasks = this.preVisitTasks.filter(t => t.patientId === patientId);
    const completedTasks = patientTasks.filter(t => t.completed).length;
    const totalTasks = patientTasks.length;
    const pendingTasks = totalTasks - completedTasks;
    const completionRate = totalTasks > 0 ? (completedTasks / totalTasks) * 100 : 0;

    return {
      totalTasks,
      completedTasks,
      pendingTasks,
      completionRate,
    };
  }

  // ============================================================================
  // Phase 6.1: Care Gap Closure Workflows
  // ============================================================================

  /**
   * Load care gaps for display
   */
  @TrackInteraction('ma-dashboard', 'load-care-gaps')
  loadCareGaps(): void {
    this.careGapService.getHighPriorityGaps(20)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (gaps) => {
          this.careGaps = gaps;
        },
        error: (error) => {
          this.logger.error('Error loading care gaps', error);
          this.toastService.error('Failed to load care gaps');
        }
      });
  }

  /**
   * Close a care gap with reason and notes
   */
  @TrackInteraction('ma-dashboard', 'close-care-gap')
  closeCareGap(gap: CareGap): void {
    this.dialogService.confirm(
      'Close Care Gap',
      `Close care gap for <strong>${gap.measureName}</strong>?<br><br>` +
      `Patient: ${gap.patientId}<br>` +
      `Priority: ${gap.priority}<br><br>` +
      `Please ensure all required interventions have been completed.`,
      'Close Gap',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (confirmed) => {
          if (confirmed) {
            this.careGapService.closeGap(gap.id, {
              reason: 'Closed by MA',
              notes: 'Gap addressed',
              closedBy: 'MA-USER', // In production, get from auth service
            }).pipe(takeUntil(this.destroy$))
              .subscribe({
                next: () => {
                  this.toastService.success('Care gap closed successfully');
                  // Remove from list or refresh
                  const index = this.careGaps.indexOf(gap);
                  if (index > -1) {
                    this.careGaps.splice(index, 1);
                  }
                },
                error: (error) => {
                  this.logger.error('Error closing care gap', error);
                  this.toastService.error('Failed to close care gap');
                }
              });
          }
        }
      });
  }

  /**
   * Filter care gaps by priority
   */
  getCareGapsByPriority(priority: GapPriority): CareGap[] {
    return this.careGaps.filter(gap => gap.priority === priority);
  }

  /**
   * Assign intervention to a care gap
   */
  @TrackInteraction('ma-dashboard', 'assign-intervention')
  assignInterventionToGap(gap: CareGap): void {
    this.dialogService.confirm(
      'Assign Intervention',
      `Assign an intervention for <strong>${gap.measureName}</strong>?<br><br>` +
      `This will schedule a follow-up action to address this care gap.`,
      'Assign',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (confirmed) => {
          if (confirmed) {
            this.careGapService.assignIntervention(gap.id, {
              type: 'OUTREACH' as any,
              description: 'Patient outreach scheduled',
            }).pipe(takeUntil(this.destroy$))
              .subscribe({
                next: () => {
                  this.toastService.success('Intervention assigned successfully');
                  gap.intervention = {
                    type: 'OUTREACH' as any,
                    description: 'Patient outreach scheduled',
                  };
                },
                error: (error) => {
                  this.logger.error('Error assigning intervention', error);
                  this.toastService.error('Failed to assign intervention');
                }
              });
          }
        }
      });
  }

  /**
   * Get count of open care gaps
   */
  getOpenCareGapCount(): number {
    return this.careGaps.filter(gap => gap.status === CareGapStatus.OPEN).length;
  }

  /**
   * Navigate to patient detail from care gap
   */
  @TrackInteraction('ma-dashboard', 'view-patient-from-gap')
  viewPatientFromGap(gap: CareGap): void {
    this.router.navigate(['/patients', gap.patientId], {
      queryParams: { gapId: gap.id }
    });
  }

  // ============================================================================
  // Phase 6.1: Patient Outreach Tracking
  // ============================================================================

  /**
   * Load outreach list for today
   */
  @TrackInteraction('ma-dashboard', 'load-outreach-list')
  loadOutreachList(): void {
    // Mock implementation - in production, this would call an API
    this.outreachItems = [
      {
        id: '1',
        patientId: 'patient-001',
        patientName: 'Smith, John',
        reason: 'Care gap follow-up: Diabetes screening',
        status: 'pending',
        priority: 'high',
        scheduledDate: new Date().toISOString().split('T')[0],
      },
      {
        id: '2',
        patientId: 'patient-002',
        patientName: 'Johnson, Mary',
        reason: 'Missed appointment follow-up',
        status: 'pending',
        priority: 'high',
        scheduledDate: new Date().toISOString().split('T')[0],
      },
      {
        id: '3',
        patientId: 'patient-003',
        patientName: 'Williams, Robert',
        reason: 'Preventive screening reminder',
        status: 'completed',
        priority: 'normal',
        scheduledDate: new Date().toISOString().split('T')[0],
        outcome: 'Contacted - Appointment scheduled',
      },
    ];
  }

  /**
   * Record outreach attempt with outcome
   */
  @TrackInteraction('ma-dashboard', 'record-outreach-attempt')
  recordOutreachAttempt(outreach: OutreachItem): void {
    this.dialogService.confirm(
      'Record Outreach Attempt',
      `Record outcome for <strong>${outreach.patientName}</strong>?<br><br>` +
      `Reason: ${outreach.reason}<br>` +
      `Scheduled: ${outreach.scheduledDate}`,
      'Record',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (confirmed) => {
          if (confirmed) {
            outreach.lastAttempt = new Date().toISOString();
            outreach.outcome = 'Contacted';
            outreach.notes = 'Outreach attempt completed';
            outreach.status = 'in-progress';
            this.toastService.success('Outreach attempt recorded');
          }
        }
      });
  }

  /**
   * Mark outreach as completed
   */
  @TrackInteraction('ma-dashboard', 'complete-outreach')
  completeOutreach(outreach: OutreachItem): void {
    outreach.status = 'completed';
    this.toastService.success(`Outreach completed for ${outreach.patientName}`);
  }

  /**
   * Schedule next outreach attempt
   */
  @TrackInteraction('ma-dashboard', 'schedule-outreach')
  scheduleOutreach(outreach: OutreachItem): void {
    this.dialogService.confirm(
      'Schedule Outreach',
      `Schedule next attempt for <strong>${outreach.patientName}</strong>?<br><br>` +
      `This will create a follow-up task for tomorrow.`,
      'Schedule',
      'Cancel',
      'primary'
    ).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (confirmed) => {
          if (confirmed) {
            // Schedule for tomorrow
            const tomorrow = new Date();
            tomorrow.setDate(tomorrow.getDate() + 1);
            outreach.scheduledDate = tomorrow.toISOString().split('T')[0];
            outreach.status = 'pending';
            this.toastService.success('Outreach scheduled');
          }
        }
      });
  }

  /**
   * Filter outreach by status
   */
  getOutreachByStatus(status: string): OutreachItem[] {
    return this.outreachItems.filter(item => item.status === status);
  }

  /**
   * Calculate outreach completion rate
   */
  getOutreachCompletionRate(): number {
    if (this.outreachItems.length === 0) return 0;
    const completed = this.outreachItems.filter(item => item.status === 'completed').length;
    return (completed / this.outreachItems.length) * 100;
  }

  /**
   * Refresh all dashboard data (Phase 6.1)
   */
  @TrackInteraction('ma-dashboard', 'refresh-all-data')
  refreshAllData(): void {
    this.loadScheduleData();
    this.loadCareGaps();
    this.loadOutreachList();
    this.toastService.info('Dashboard data refreshed');
  }
}
