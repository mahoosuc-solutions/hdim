import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil, forkJoin } from 'rxjs';
import { SalesService } from '../../../services/sales.service';
import { Activity, ActivityType, ActivityStatus, ActivityLogRequest } from '../../../models/sales.model';

@Component({
  selector: 'app-sales-activities',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="activities-page">
      <div class="page-header">
        <div class="header-content">
          <h2>Activities</h2>
          <p class="subtitle">Track calls, emails, meetings, and tasks</p>
        </div>
        <div class="header-actions">
          <button class="btn btn-outline" (click)="showLogDialog('CALL')">
            📞 Log Call
          </button>
          <button class="btn btn-outline" (click)="showLogDialog('EMAIL')">
            📧 Log Email
          </button>
          <button class="btn btn-outline" (click)="showLogDialog('MEETING')">
            📅 Log Meeting
          </button>
          <button class="btn btn-primary" (click)="showLogDialog('TASK')">
            + New Task
          </button>
        </div>
      </div>

      <!-- Summary Cards -->
      <div class="summary-cards">
        <div class="summary-card today">
          <div class="summary-icon">📋</div>
          <div class="summary-content">
            <span class="summary-value">{{ todayCount() }}</span>
            <span class="summary-label">Due Today</span>
          </div>
        </div>
        <div class="summary-card overdue" *ngIf="overdueActivities().length > 0">
          <div class="summary-icon">⚠️</div>
          <div class="summary-content">
            <span class="summary-value warning">{{ overdueActivities().length }}</span>
            <span class="summary-label">Overdue</span>
          </div>
        </div>
        <div class="summary-card upcoming">
          <div class="summary-icon">🗓️</div>
          <div class="summary-content">
            <span class="summary-value">{{ upcomingActivities().length }}</span>
            <span class="summary-label">Upcoming (7 days)</span>
          </div>
        </div>
        <div class="summary-card completed">
          <div class="summary-icon">✅</div>
          <div class="summary-content">
            <span class="summary-value">{{ completedThisWeek() }}</span>
            <span class="summary-label">Completed This Week</span>
          </div>
        </div>
      </div>

      <!-- Filters -->
      <div class="filters-bar">
        <div class="filter-tabs">
          <button
            class="tab"
            [class.active]="activeTab === 'all'"
            (click)="setTab('all')"
          >
            All
          </button>
          <button
            class="tab"
            [class.active]="activeTab === 'overdue'"
            (click)="setTab('overdue')"
          >
            Overdue
            <span class="badge" *ngIf="overdueActivities().length">{{ overdueActivities().length }}</span>
          </button>
          <button
            class="tab"
            [class.active]="activeTab === 'today'"
            (click)="setTab('today')"
          >
            Today
          </button>
          <button
            class="tab"
            [class.active]="activeTab === 'upcoming'"
            (click)="setTab('upcoming')"
          >
            Upcoming
          </button>
          <button
            class="tab"
            [class.active]="activeTab === 'completed'"
            (click)="setTab('completed')"
          >
            Completed
          </button>
        </div>
        <div class="filter-group">
          <select [(ngModel)]="typeFilter" (change)="applyFilters()">
            <option value="">All Types</option>
            <option *ngFor="let type of activityTypes" [value]="type">
              {{ formatType(type) }}
            </option>
          </select>
        </div>
      </div>

      <!-- Loading State -->
      <div class="loading" *ngIf="isLoading()">
        <div class="spinner"></div>
        <span>Loading activities...</span>
      </div>

      <!-- Activities List -->
      <div class="activities-list" *ngIf="!isLoading()">
        <div
          class="activity-item"
          *ngFor="let activity of filteredActivities()"
          [class.overdue]="isOverdue(activity)"
          [class.completed]="activity.status === 'COMPLETED'"
          (click)="selectActivity(activity)"
        >
          <div class="activity-icon" [class]="activity.type.toLowerCase()">
            {{ getTypeIcon(activity.type) }}
          </div>

          <div class="activity-content">
            <div class="activity-header">
              <span class="activity-subject">{{ activity.subject }}</span>
              <span class="activity-type">{{ formatType(activity.type) }}</span>
            </div>
            <div class="activity-meta">
              <span class="due-date" [class.overdue]="isOverdue(activity)">
                {{ activity.dueDate | date:'short' }}
              </span>
              <span class="priority" [class]="activity.priority.toLowerCase()">
                {{ activity.priority }}
              </span>
            </div>
            <p class="activity-description" *ngIf="activity.description">
              {{ activity.description | slice:0:100 }}{{ activity.description.length > 100 ? '...' : '' }}
            </p>
          </div>

          <div class="activity-actions" (click)="$event.stopPropagation()">
            <button
              class="complete-btn"
              *ngIf="activity.status !== 'COMPLETED'"
              (click)="completeActivity(activity)"
              title="Mark Complete"
            >
              ✓
            </button>
          </div>
        </div>

        <div class="empty-state" *ngIf="!filteredActivities().length">
          <span class="empty-icon">📋</span>
          <span>No activities found</span>
          <button class="btn btn-primary" (click)="showLogDialog('TASK')">Create Activity</button>
        </div>
      </div>

      <!-- Log Activity Dialog -->
      <div class="dialog-overlay" *ngIf="showDialog" (click)="closeDialog()">
        <div class="dialog" (click)="$event.stopPropagation()">
          <div class="dialog-header">
            <h3>{{ dialogTitle }}</h3>
            <button class="close-btn" (click)="closeDialog()">×</button>
          </div>
          <form (ngSubmit)="saveActivity()" class="dialog-form">
            <div class="form-group">
              <label>Subject *</label>
              <input type="text" [(ngModel)]="activityForm.subject" name="subject" required />
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>Type</label>
                <select [(ngModel)]="activityForm.type" name="type">
                  <option *ngFor="let type of activityTypes" [value]="type">
                    {{ formatType(type) }}
                  </option>
                </select>
              </div>
              <div class="form-group">
                <label>Priority</label>
                <select [(ngModel)]="activityForm.priority" name="priority">
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                </select>
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>Due Date</label>
                <input type="datetime-local" [(ngModel)]="activityForm.dueDate" name="dueDate" />
              </div>
              <div class="form-group">
                <label>Duration (minutes)</label>
                <input type="number" [(ngModel)]="activityForm.duration" name="duration" min="0" />
              </div>
            </div>

            <div class="form-group">
              <label>Description</label>
              <textarea [(ngModel)]="activityForm.description" name="description" rows="3"></textarea>
            </div>

            <div class="form-group" *ngIf="activityForm.type === 'CALL' || activityForm.type === 'EMAIL' || activityForm.type === 'MEETING'">
              <label>Outcome</label>
              <textarea [(ngModel)]="activityForm.outcome" name="outcome" rows="2" placeholder="What was the result?"></textarea>
            </div>

            <div class="dialog-actions">
              <button type="button" class="btn btn-secondary" (click)="closeDialog()">Cancel</button>
              <button type="submit" class="btn btn-primary">Save Activity</button>
            </div>
          </form>
        </div>
      </div>

      <!-- Activity Detail Panel -->
      <div class="detail-panel" *ngIf="selectedActivity()" (click)="selectedActivity.set(null)">
        <div class="detail-content" (click)="$event.stopPropagation()">
          <div class="detail-header">
            <div class="activity-type-icon" [class]="selectedActivity()!.type.toLowerCase()">
              {{ getTypeIcon(selectedActivity()!.type) }}
            </div>
            <div class="header-info">
              <h3>{{ selectedActivity()!.subject }}</h3>
              <span class="type-badge">{{ formatType(selectedActivity()!.type) }}</span>
            </div>
            <button class="close-btn" (click)="selectedActivity.set(null)">×</button>
          </div>

          <div class="detail-body">
            <div class="detail-section">
              <div class="detail-row">
                <span class="label">Status:</span>
                <span class="status-badge" [class]="selectedActivity()!.status.toLowerCase()">
                  {{ selectedActivity()!.status }}
                </span>
              </div>
              <div class="detail-row">
                <span class="label">Priority:</span>
                <span class="priority" [class]="selectedActivity()!.priority.toLowerCase()">
                  {{ selectedActivity()!.priority }}
                </span>
              </div>
              <div class="detail-row">
                <span class="label">Due Date:</span>
                <span [class.overdue]="isOverdue(selectedActivity()!)">
                  {{ selectedActivity()!.dueDate | date:'medium' }}
                </span>
              </div>
              <div class="detail-row" *ngIf="selectedActivity()!.duration">
                <span class="label">Duration:</span>
                <span>{{ selectedActivity()!.duration }} minutes</span>
              </div>
            </div>

            <div class="detail-section" *ngIf="selectedActivity()!.description">
              <h4>Description</h4>
              <p>{{ selectedActivity()!.description }}</p>
            </div>

            <div class="detail-section" *ngIf="selectedActivity()!.outcome">
              <h4>Outcome</h4>
              <p>{{ selectedActivity()!.outcome }}</p>
            </div>
          </div>

          <div class="detail-actions">
            <button
              class="btn btn-primary"
              *ngIf="selectedActivity()!.status !== 'COMPLETED'"
              (click)="completeActivity(selectedActivity()!)"
            >
              Mark as Complete
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .activities-page {
      max-width: 1200px;
      margin: 0 auto;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 24px;
      flex-wrap: wrap;
      gap: 16px;
    }

    .header-content h2 {
      margin: 0;
      color: #1a237e;
    }

    .subtitle {
      margin: 4px 0 0;
      color: #666;
      font-size: 14px;
    }

    .header-actions {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }

    .btn {
      padding: 10px 16px;
      border-radius: 8px;
      font-weight: 500;
      cursor: pointer;
      border: none;
      transition: all 0.2s ease;
      font-size: 14px;
    }

    .btn-primary {
      background: #1a237e;
      color: white;
    }

    .btn-outline {
      background: white;
      color: #333;
      border: 1px solid #ddd;
    }

    .btn-outline:hover {
      background: #f5f5f5;
    }

    .btn-secondary {
      background: #f5f5f5;
      color: #333;
      border: 1px solid #ddd;
    }

    .summary-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }

    .summary-card {
      background: white;
      border-radius: 12px;
      padding: 20px;
      display: flex;
      align-items: center;
      gap: 16px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .summary-icon {
      font-size: 28px;
    }

    .summary-content {
      display: flex;
      flex-direction: column;
    }

    .summary-value {
      font-size: 24px;
      font-weight: 700;
      color: #1a237e;
    }

    .summary-value.warning {
      color: #f57c00;
    }

    .summary-label {
      font-size: 12px;
      color: #666;
    }

    .filters-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      flex-wrap: wrap;
      gap: 16px;
    }

    .filter-tabs {
      display: flex;
      background: #f5f5f5;
      border-radius: 8px;
      padding: 4px;
    }

    .tab {
      padding: 8px 16px;
      border: none;
      background: none;
      border-radius: 6px;
      cursor: pointer;
      font-weight: 500;
      color: #666;
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .tab.active {
      background: white;
      color: #1a237e;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    }

    .tab .badge {
      background: #f44336;
      color: white;
      font-size: 11px;
      padding: 2px 6px;
      border-radius: 10px;
    }

    .filter-group select {
      padding: 10px 14px;
      border: 1px solid #ddd;
      border-radius: 8px;
      background: white;
      font-size: 14px;
    }

    .activities-list {
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      overflow: hidden;
    }

    .activity-item {
      display: flex;
      align-items: flex-start;
      gap: 16px;
      padding: 20px;
      border-bottom: 1px solid #eee;
      cursor: pointer;
      transition: background 0.2s ease;
    }

    .activity-item:last-child {
      border-bottom: none;
    }

    .activity-item:hover {
      background: #f8f9fa;
    }

    .activity-item.overdue {
      background: #fff8f8;
    }

    .activity-item.completed {
      opacity: 0.7;
    }

    .activity-icon {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      flex-shrink: 0;
    }

    .activity-icon.call { background: #e3f2fd; }
    .activity-icon.email { background: #f3e5f5; }
    .activity-icon.meeting { background: #e8f5e9; }
    .activity-icon.demo { background: #fff3e0; }
    .activity-icon.task { background: #fce4ec; }
    .activity-icon.follow_up { background: #e0f7fa; }
    .activity-icon.note { background: #f5f5f5; }

    .activity-content {
      flex: 1;
    }

    .activity-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 4px;
    }

    .activity-subject {
      font-weight: 600;
      color: #333;
    }

    .activity-type {
      font-size: 12px;
      color: #666;
      background: #f5f5f5;
      padding: 2px 8px;
      border-radius: 4px;
    }

    .activity-meta {
      display: flex;
      gap: 12px;
      margin-bottom: 8px;
    }

    .due-date {
      font-size: 13px;
      color: #666;
    }

    .due-date.overdue {
      color: #c62828;
      font-weight: 500;
    }

    .priority {
      font-size: 12px;
      padding: 2px 8px;
      border-radius: 4px;
    }

    .priority.low { background: #f5f5f5; color: #757575; }
    .priority.medium { background: #fff3e0; color: #ef6c00; }
    .priority.high { background: #ffebee; color: #c62828; }

    .activity-description {
      margin: 0;
      font-size: 13px;
      color: #666;
      line-height: 1.4;
    }

    .activity-actions {
      display: flex;
      align-items: center;
    }

    .complete-btn {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      border: 2px solid #ddd;
      background: white;
      cursor: pointer;
      font-size: 16px;
      color: #666;
      transition: all 0.2s ease;
    }

    .complete-btn:hover {
      border-color: #4caf50;
      color: #4caf50;
      background: #e8f5e9;
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 48px;
      color: #666;
      gap: 16px;
    }

    .empty-icon {
      font-size: 48px;
    }

    /* Dialog Styles */
    .dialog-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }

    .dialog {
      background: white;
      border-radius: 12px;
      width: 90%;
      max-width: 500px;
      max-height: 90vh;
      overflow-y: auto;
    }

    .dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px 24px;
      border-bottom: 1px solid #eee;
    }

    .dialog-header h3 {
      margin: 0;
    }

    .close-btn {
      background: none;
      border: none;
      font-size: 24px;
      cursor: pointer;
      color: #666;
    }

    .dialog-form {
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .form-group label {
      font-size: 13px;
      font-weight: 500;
      color: #333;
    }

    .form-group input,
    .form-group select,
    .form-group textarea {
      padding: 10px 12px;
      border: 1px solid #ddd;
      border-radius: 6px;
      font-size: 14px;
    }

    .dialog-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 8px;
    }

    /* Detail Panel */
    .detail-panel {
      position: fixed;
      top: 0;
      right: 0;
      bottom: 0;
      width: 100%;
      background: rgba(0, 0, 0, 0.3);
      display: flex;
      justify-content: flex-end;
      z-index: 1000;
    }

    .detail-content {
      width: 400px;
      background: white;
      height: 100%;
      overflow-y: auto;
      box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
    }

    .detail-header {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 24px;
      border-bottom: 1px solid #eee;
    }

    .activity-type-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
    }

    .header-info {
      flex: 1;
    }

    .header-info h3 {
      margin: 0 0 4px 0;
    }

    .type-badge {
      font-size: 12px;
      color: #666;
    }

    .detail-body {
      padding: 24px;
    }

    .detail-section {
      margin-bottom: 24px;
    }

    .detail-section h4 {
      margin: 0 0 12px 0;
      color: #1a237e;
      font-size: 14px;
    }

    .detail-row {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      border-bottom: 1px solid #f5f5f5;
    }

    .detail-row .label {
      color: #666;
    }

    .status-badge {
      padding: 4px 10px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
    }

    .status-badge.scheduled { background: #e3f2fd; color: #1565c0; }
    .status-badge.completed { background: #e8f5e9; color: #2e7d32; }
    .status-badge.cancelled { background: #fafafa; color: #757575; }
    .status-badge.no_show { background: #ffebee; color: #c62828; }

    .detail-section p {
      margin: 0;
      color: #333;
      line-height: 1.6;
    }

    .detail-actions {
      padding: 24px;
      border-top: 1px solid #eee;
    }

    .loading {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 48px;
      color: #666;
    }

    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid #e0e0e0;
      border-top-color: #1a237e;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin-bottom: 16px;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    @media (max-width: 768px) {
      .form-row {
        grid-template-columns: 1fr;
      }

      .detail-content {
        width: 100%;
      }
    }
  `],
})
export class SalesActivitiesComponent implements OnInit, OnDestroy {
  private readonly salesService = inject(SalesService);
  private destroy$ = new Subject<void>();

  activities = signal<Activity[]>([]);
  overdueActivities = signal<Activity[]>([]);
  upcomingActivities = signal<Activity[]>([]);
  selectedActivity = signal<Activity | null>(null);
  isLoading = signal(false);

  showDialog = false;
  dialogTitle = 'Log Activity';
  activeTab = 'all';
  typeFilter = '';

  activityTypes: ActivityType[] = ['CALL', 'EMAIL', 'MEETING', 'DEMO', 'TASK', 'FOLLOW_UP', 'NOTE'];

  activityForm = this.getEmptyForm();

  ngOnInit(): void {
    this.loadActivities();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadActivities(): void {
    this.isLoading.set(true);

    forkJoin({
      all: this.salesService.getActivities({}, { page: 0, size: 100 }),
      overdue: this.salesService.getOverdueActivities(),
      upcoming: this.salesService.getUpcomingActivities(),
    }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.activities.set(result.all.content);
          this.overdueActivities.set(result.overdue);
          this.upcomingActivities.set(result.upcoming);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
        },
      });
  }

  todayCount(): number {
    const today = new Date().toISOString().split('T')[0];
    return this.activities().filter((a) => a.dueDate?.startsWith(today) && a.status !== 'COMPLETED').length;
  }

  completedThisWeek(): number {
    const weekAgo = new Date();
    weekAgo.setDate(weekAgo.getDate() - 7);
    return this.activities().filter((a) =>
      a.status === 'COMPLETED' && a.completedAt && new Date(a.completedAt) >= weekAgo
    ).length;
  }

  filteredActivities(): Activity[] {
    let result = this.activities();

    // Apply tab filter
    switch (this.activeTab) {
      case 'overdue':
        result = this.overdueActivities();
        break;
      case 'today':
        const today = new Date().toISOString().split('T')[0];
        result = result.filter((a) => a.dueDate?.startsWith(today));
        break;
      case 'upcoming':
        result = this.upcomingActivities();
        break;
      case 'completed':
        result = result.filter((a) => a.status === 'COMPLETED');
        break;
    }

    // Apply type filter
    if (this.typeFilter) {
      result = result.filter((a) => a.type === this.typeFilter);
    }

    return result;
  }

  setTab(tab: string): void {
    this.activeTab = tab;
  }

  applyFilters(): void {
    // Filters are applied reactively through filteredActivities()
  }

  showLogDialog(type: ActivityType): void {
    this.activityForm = this.getEmptyForm();
    this.activityForm.type = type;
    this.dialogTitle = this.getDialogTitle(type);
    this.showDialog = true;
  }

  closeDialog(): void {
    this.showDialog = false;
    this.activityForm = this.getEmptyForm();
  }

  saveActivity(): void {
    const request = {
      ...this.activityForm,
    };

    this.salesService.createActivity(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.closeDialog();
          this.loadActivities();
        },
      });
  }

  completeActivity(activity: Activity): void {
    this.salesService.completeActivity(activity.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.selectedActivity.set(null);
          this.loadActivities();
        },
      });
  }

  selectActivity(activity: Activity): void {
    this.selectedActivity.set(activity);
  }

  isOverdue(activity: Activity): boolean {
    if (!activity.dueDate || activity.status === 'COMPLETED') return false;
    return new Date(activity.dueDate) < new Date();
  }

  getEmptyForm(): any {
    return {
      type: 'TASK' as ActivityType,
      subject: '',
      description: '',
      dueDate: '',
      duration: undefined,
      priority: 'MEDIUM',
      outcome: '',
    };
  }

  getDialogTitle(type: ActivityType): string {
    const titles: Record<ActivityType, string> = {
      CALL: 'Log Call',
      EMAIL: 'Log Email',
      MEETING: 'Log Meeting',
      DEMO: 'Schedule Demo',
      TASK: 'Create Task',
      FOLLOW_UP: 'Schedule Follow-up',
      NOTE: 'Add Note',
      OTHER: 'Log Activity',
    };
    return titles[type];
  }

  formatType(type: string): string {
    return type.replace('_', ' ').toLowerCase()
      .replace(/\b\w/g, (l) => l.toUpperCase());
  }

  getTypeIcon(type: ActivityType): string {
    const icons: Record<ActivityType, string> = {
      CALL: '📞',
      EMAIL: '📧',
      MEETING: '📅',
      DEMO: '💻',
      TASK: '✅',
      FOLLOW_UP: '🔄',
      NOTE: '📝',
      OTHER: '📌',
    };
    return icons[type];
  }
}
