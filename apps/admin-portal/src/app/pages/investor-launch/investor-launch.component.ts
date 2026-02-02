import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { InvestorService } from '../../services/investor.service';
import { InvestorAuthService } from '../../services/investor-auth.service';
import { InvestorTask, Contact, OutreachActivity } from '../../models/investor.model';
import { ContactDialogComponent, ContactDialogData, ContactDialogResult } from './dialogs/contact-dialog/contact-dialog.component';
import { TaskDialogComponent, TaskDialogData, TaskDialogResult } from './dialogs/task-dialog/task-dialog.component';
import { ActivityDialogComponent, ActivityDialogData, ActivityDialogResult } from './dialogs/activity-dialog/activity-dialog.component';
import { SearchBarComponent } from './components/search-bar/search-bar.component';
import { ProgressChartComponent, ProgressChartData } from './components/progress-chart/progress-chart.component';

@Component({
  selector: 'app-investor-launch',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    SearchBarComponent,
    ProgressChartComponent,
  ],
  template: `
    <div class="investor-launch">
      <!-- Header -->
      <div class="header">
        <div class="header-content">
          <h1>🚀 Investor Launch Dashboard</h1>
          <p class="subtitle">Series A Fundraise & Customer Acquisition Tracker</p>
        </div>
        <div class="header-actions">
          <app-search-bar (search)="onSearch($event)"></app-search-bar>
          <select [(ngModel)]="selectedView" class="view-select">
            <option value="overview">Overview</option>
            <option value="tasks">Task Tracker</option>
            <option value="contacts">Contacts CRM</option>
            <option value="documents">Documents</option>
          </select>
          <button class="btn-export" (click)="exportData()">
            <mat-icon>download</mat-icon> Export
          </button>
          <button mat-icon-button [matMenuTriggerFor]="userMenu" matTooltip="Account">
            <mat-icon>account_circle</mat-icon>
          </button>
          <mat-menu #userMenu="matMenu">
            <div class="user-info">
              <strong>{{ userDisplayName() }}</strong>
              <small>{{ currentUser()?.email }}</small>
            </div>
            <mat-divider></mat-divider>
            @if (linkedInConnected()) {
              <button mat-menu-item (click)="disconnectLinkedIn()">
                <mat-icon>link_off</mat-icon> Disconnect LinkedIn
              </button>
            } @else {
              <button mat-menu-item (click)="connectLinkedIn()">
                <mat-icon>link</mat-icon> Connect LinkedIn
              </button>
            }
            <button mat-menu-item (click)="logout()">
              <mat-icon>logout</mat-icon> Sign Out
            </button>
          </mat-menu>
        </div>
      </div>

      <!-- Loading State -->
      @if (isLoading()) {
        <div class="loading-overlay">
          <mat-spinner diameter="48"></mat-spinner>
          <p>Loading dashboard...</p>
        </div>
      }

      <!-- Stats Cards -->
      <div class="stats-grid">
        <div class="stat-card primary">
          <div class="stat-icon">✅</div>
          <div class="stat-content">
            <span class="stat-value">{{ stats().tasksCompleted }}/{{ stats().tasksTotal }}</span>
            <span class="stat-label">Tasks Completed</span>
            <div class="progress-bar">
              <div class="progress-fill" [style.width.%]="taskProgressPercent()"></div>
            </div>
          </div>
        </div>
        <div class="stat-card accent">
          <div class="stat-icon">🔗</div>
          <div class="stat-content">
            <span class="stat-value">{{ stats().linkedInRequestsSent }}</span>
            <span class="stat-label">LinkedIn Requests</span>
            <span class="stat-detail">{{ stats().linkedInConnectionsAccepted }} accepted</span>
          </div>
        </div>
        <div class="stat-card success">
          <div class="stat-icon">📅</div>
          <div class="stat-content">
            <span class="stat-value">{{ stats().meetingsScheduled }}</span>
            <span class="stat-label">Meetings Booked</span>
          </div>
        </div>
        <div class="stat-card warning">
          <div class="stat-icon">🔥</div>
          <div class="stat-content">
            <span class="stat-value">{{ stats().warmLeads }}</span>
            <span class="stat-label">Warm Leads</span>
          </div>
        </div>
      </div>

      <!-- Search Results -->
      @if (searchQuery() && (searchResults().tasks.length || searchResults().contacts.length)) {
        <div class="search-results card">
          <div class="card-header">
            <h2>🔍 Search Results for "{{ searchQuery() }}"</h2>
            <button mat-button (click)="clearSearch()">Clear</button>
          </div>
          <div class="search-results-content">
            @if (searchResults().tasks.length) {
              <div class="search-section">
                <h3>Tasks ({{ searchResults().tasks.length }})</h3>
                @for (task of searchResults().tasks; track task.id) {
                  <div class="search-item" (click)="openTaskDialog('edit', task)">
                    <span class="status-icon">{{ getStatusIcon(task.status) }}</span>
                    <span>{{ task.subject }}</span>
                    <span class="category-badge" [class]="task.category">{{ task.category }}</span>
                  </div>
                }
              </div>
            }
            @if (searchResults().contacts.length) {
              <div class="search-section">
                <h3>Contacts ({{ searchResults().contacts.length }})</h3>
                @for (contact of searchResults().contacts; track contact.id) {
                  <div class="search-item" (click)="openContactDialog('edit', contact)">
                    <span>{{ contact.name }}</span>
                    <span class="org">{{ contact.organization }}</span>
                  </div>
                }
              </div>
            }
          </div>
        </div>
      }

      <!-- Main Content -->
      @if (selectedView === 'overview') {
        <div class="overview-grid">
          <!-- Progress Chart -->
          <div class="card">
            <div class="card-header">
              <h2>📊 Task Progress</h2>
            </div>
            <div class="chart-wrapper">
              <app-progress-chart [data]="chartData()"></app-progress-chart>
            </div>
            <div class="chart-legend">
              <div class="legend-item">
                <span class="legend-color completed"></span>
                <span>Completed ({{ stats().tasksCompleted }})</span>
              </div>
              <div class="legend-item">
                <span class="legend-color in-progress"></span>
                <span>In Progress ({{ stats().tasksInProgress }})</span>
              </div>
              <div class="legend-item">
                <span class="legend-color pending"></span>
                <span>Pending ({{ stats().tasksPending }})</span>
              </div>
            </div>
          </div>

          <!-- Week 1 Progress -->
          <div class="card">
            <div class="card-header">
              <h2>📋 Week 1 Tasks</h2>
              <span class="badge completed">{{ week1CompletedCount() }}/{{ week1Tasks().length }}</span>
            </div>
            <div class="task-list">
              @for (task of week1Tasks(); track task.id) {
                <div class="task-item" [class.completed]="task.status === 'completed'" [class.in-progress]="task.status === 'in_progress'" (click)="openTaskDialog('edit', task)">
                  <button class="task-toggle" (click)="toggleTask(task); $event.stopPropagation()">
                    {{ getStatusIcon(task.status) }}
                  </button>
                  <div class="task-content">
                    <span class="task-subject">{{ task.subject }}</span>
                    @if (task.deliverable) {
                      <span class="task-deliverable">📄 {{ task.deliverable }}</span>
                    }
                  </div>
                  <span class="task-category" [class]="task.category">{{ task.category }}</span>
                </div>
              }
            </div>
          </div>

          <!-- Week 2 Progress -->
          <div class="card">
            <div class="card-header">
              <h2>📋 Week 2 Tasks</h2>
              <span class="badge pending">{{ week2CompletedCount() }}/{{ week2Tasks().length }}</span>
            </div>
            <div class="task-list">
              @for (task of week2Tasks(); track task.id) {
                <div class="task-item" [class.completed]="task.status === 'completed'" [class.in-progress]="task.status === 'in_progress'" [class.blocked]="hasBlockers(task)" (click)="openTaskDialog('edit', task)">
                  <button class="task-toggle" (click)="toggleTask(task); $event.stopPropagation()">
                    {{ hasBlockers(task) ? '🔒' : getStatusIcon(task.status) }}
                  </button>
                  <div class="task-content">
                    <span class="task-subject">{{ task.subject }}</span>
                    @if (hasBlockers(task)) {
                      <span class="task-blocked">Blocked by #{{ getBlockerIds(task) }}</span>
                    }
                  </div>
                  <span class="task-category" [class]="task.category">{{ task.category }}</span>
                </div>
              }
            </div>
          </div>

          <!-- Quick Actions -->
          <div class="card full-width">
            <div class="card-header">
              <h2>⚡ Quick Actions</h2>
            </div>
            <div class="quick-actions">
              <button class="action-btn" (click)="openActivityDialog()">
                <span class="action-icon">🔗</span>
                <span>Log LinkedIn Activity</span>
              </button>
              <button class="action-btn" (click)="openActivityDialogWithType('email')">
                <span class="action-icon">📧</span>
                <span>Log Email Sent</span>
              </button>
              <button class="action-btn" (click)="openActivityDialogWithType('meeting')">
                <span class="action-icon">📅</span>
                <span>Schedule Meeting</span>
              </button>
              <button class="action-btn" (click)="openContactDialog('create')">
                <span class="action-icon">👤</span>
                <span>Add Contact</span>
              </button>
              <button class="action-btn" (click)="openTaskDialog('create')">
                <span class="action-icon">✏️</span>
                <span>Add Task</span>
              </button>
            </div>
          </div>

          <!-- Recent Activity -->
          <div class="card full-width">
            <div class="card-header">
              <h2>📊 Recent Activity</h2>
              <button mat-button color="primary" (click)="openActivityDialog()">+ Log Activity</button>
            </div>
            @if (activities().length === 0) {
              <div class="empty-state">
                <p>No activities logged yet. Use Quick Actions to start tracking!</p>
              </div>
            } @else {
              <div class="activity-list">
                @for (activity of recentActivities(); track activity.id) {
                  <div class="activity-item">
                    <span class="activity-icon">{{ getActivityIcon(activity.type) }}</span>
                    <div class="activity-content">
                      <span class="activity-contact">{{ activity.contactName }}</span>
                      <span class="activity-type">{{ activity.type.replace('_', ' ') }}</span>
                    </div>
                    <span class="activity-date">{{ activity.date }}</span>
                    <span class="activity-status" [class]="activity.status">{{ activity.status }}</span>
                  </div>
                }
              </div>
            }
          </div>
        </div>
      }

      @if (selectedView === 'tasks') {
        <div class="tasks-view">
          <div class="card full-width">
            <div class="card-header">
              <h2>📋 All Tasks</h2>
              <div class="header-right">
                <div class="filter-tabs">
                  <button [class.active]="taskFilter === 'all'" (click)="taskFilter = 'all'">All</button>
                  <button [class.active]="taskFilter === 'pending'" (click)="taskFilter = 'pending'">Pending</button>
                  <button [class.active]="taskFilter === 'in_progress'" (click)="taskFilter = 'in_progress'">In Progress</button>
                  <button [class.active]="taskFilter === 'completed'" (click)="taskFilter = 'completed'">Completed</button>
                </div>
                <button mat-raised-button color="primary" (click)="openTaskDialog('create')">
                  <mat-icon>add</mat-icon> Add Task
                </button>
              </div>
            </div>
            <div class="tasks-table">
              <div class="table-header">
                <span class="col-status">Status</span>
                <span class="col-task">Task</span>
                <span class="col-category">Category</span>
                <span class="col-week">Week</span>
                <span class="col-deliverable">Deliverable</span>
              </div>
              @for (task of filteredTasks(); track task.id) {
                <div class="table-row" [class.completed]="task.status === 'completed'" (click)="openTaskDialog('edit', task)">
                  <span class="col-status">
                    <select [ngModel]="task.status" (ngModelChange)="updateTaskStatus(task.id, $event)" (click)="$event.stopPropagation()">
                      <option value="pending">⬜ Pending</option>
                      <option value="in_progress">🔄 In Progress</option>
                      <option value="completed">✅ Completed</option>
                      <option value="blocked">🔒 Blocked</option>
                    </select>
                  </span>
                  <span class="col-task">
                    <strong>#{{ task.taskNumber }}</strong> {{ task.subject }}
                    <br><small>{{ task.description }}</small>
                  </span>
                  <span class="col-category">
                    <span class="category-badge" [class]="task.category">{{ task.category }}</span>
                  </span>
                  <span class="col-week">Week {{ task.week }}</span>
                  <span class="col-deliverable">
                    @if (task.deliverable) {
                      <a [href]="'vscode://file/' + task.deliverable" class="doc-link" (click)="$event.stopPropagation()">📄 View</a>
                    }
                  </span>
                </div>
              }
            </div>
          </div>
        </div>
      }

      @if (selectedView === 'contacts') {
        <div class="contacts-view">
          <div class="card full-width">
            <div class="card-header">
              <h2>👥 Contact CRM</h2>
              <div class="header-right">
                <div class="filter-tabs">
                  <button [class.active]="contactFilter === 'all'" (click)="contactFilter = 'all'">All ({{ contacts().length }})</button>
                  <button [class.active]="contactFilter === 'quality_leader'" (click)="contactFilter = 'quality_leader'">Quality Leaders</button>
                  <button [class.active]="contactFilter === 'investor'" (click)="contactFilter = 'investor'">Investors</button>
                  <button [class.active]="contactFilter === 'angel'" (click)="contactFilter = 'angel'">Angels</button>
                </div>
                <button mat-raised-button color="primary" (click)="openContactDialog('create')">
                  <mat-icon>person_add</mat-icon> Add Contact
                </button>
              </div>
            </div>

            @if (filteredContacts().length === 0) {
              <div class="empty-state">
                <h3>No contacts yet</h3>
                <p>Import contacts from your quality-leader-profiles.md or add them manually.</p>
                <button mat-raised-button color="primary" (click)="openContactDialog('create')">Add First Contact</button>
              </div>
            } @else {
              <div class="contacts-table">
                @for (contact of filteredContacts(); track contact.id) {
                  <div class="contact-row" (click)="openContactDialog('edit', contact)">
                    <div class="contact-info">
                      <span class="contact-name">{{ contact.name }}</span>
                      <span class="contact-title">{{ contact.title }}</span>
                      <span class="contact-org">{{ contact.organization }}</span>
                    </div>
                    <span class="contact-status" [class]="contact.status">{{ contact.status.replace('_', ' ') }}</span>
                    <div class="contact-actions" (click)="$event.stopPropagation()">
                      @if (contact.linkedInUrl) {
                        <a [href]="contact.linkedInUrl" target="_blank" class="action-link" matTooltip="Open LinkedIn">🔗</a>
                      }
                      <button class="action-btn-sm" (click)="logActivityForContact(contact, 'linkedin_request')" matTooltip="Log LinkedIn">📤</button>
                      <button class="action-btn-sm" (click)="updateContactStatus(contact.id, 'connected')" matTooltip="Mark Connected">✅</button>
                    </div>
                  </div>
                }
              </div>
            }
          </div>
        </div>
      }

      @if (selectedView === 'documents') {
        <div class="documents-view">
          <div class="card full-width">
            <div class="card-header">
              <h2>📚 Investor Documents</h2>
            </div>
            <div class="documents-grid">
              @for (doc of documents(); track doc.id) {
                <div class="document-card" [class]="doc.category">
                  <div class="doc-icon">{{ getDocIcon(doc.category) }}</div>
                  <div class="doc-content">
                    <h3>{{ doc.name }}</h3>
                    <p>{{ doc.description }}</p>
                    <span class="doc-path">{{ doc.filePath }}</span>
                  </div>
                </div>
              }
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .investor-launch { max-width: 1400px; margin: 0 auto; padding: 24px; }
    .header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 32px; padding-bottom: 24px; border-bottom: 2px solid #e5e7eb; flex-wrap: wrap; gap: 16px; }
    .header h1 { margin: 0; font-size: 32px; color: #1f2937; }
    .subtitle { color: #6b7280; margin-top: 4px; }
    .header-actions { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
    .view-select { padding: 10px 16px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 14px; background: white; }
    .btn-export { display: flex; align-items: center; gap: 4px; padding: 10px 16px; background: #f3f4f6; border: 1px solid #d1d5db; border-radius: 8px; cursor: pointer; font-size: 14px; }
    .btn-export:hover { background: #e5e7eb; }
    .btn-export mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .user-info { padding: 12px 16px; display: flex; flex-direction: column; gap: 2px; }
    .user-info strong { color: #1f2937; }
    .user-info small { color: #6b7280; }
    .loading-overlay { position: fixed; inset: 0; background: rgba(255,255,255,0.9); display: flex; flex-direction: column; align-items: center; justify-content: center; z-index: 1000; }
    .loading-overlay p { margin-top: 16px; color: #6b7280; }
    .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 32px; }
    .stat-card { background: white; border-radius: 16px; padding: 24px; display: flex; gap: 16px; align-items: flex-start; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1); border-left: 4px solid; }
    .stat-card.primary { border-left-color: #3b82f6; }
    .stat-card.accent { border-left-color: #8b5cf6; }
    .stat-card.success { border-left-color: #10b981; }
    .stat-card.warning { border-left-color: #f59e0b; }
    .stat-icon { font-size: 32px; }
    .stat-content { display: flex; flex-direction: column; flex: 1; }
    .stat-value { font-size: 28px; font-weight: 700; color: #1f2937; }
    .stat-label { font-size: 14px; color: #6b7280; }
    .stat-detail { font-size: 12px; color: #9ca3af; margin-top: 4px; }
    .progress-bar { height: 6px; background: #e5e7eb; border-radius: 3px; margin-top: 8px; overflow: hidden; }
    .progress-fill { height: 100%; background: #3b82f6; border-radius: 3px; transition: width 0.3s ease; }
    .search-results { margin-bottom: 24px; }
    .search-results-content { padding: 16px; }
    .search-section { margin-bottom: 16px; }
    .search-section h3 { font-size: 14px; color: #6b7280; margin-bottom: 8px; }
    .search-item { padding: 12px; background: #f9fafb; border-radius: 8px; margin-bottom: 8px; cursor: pointer; display: flex; align-items: center; gap: 12px; }
    .search-item:hover { background: #f3f4f6; }
    .search-item .org { color: #6b7280; font-size: 13px; margin-left: auto; }
    .overview-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 24px; }
    .card { background: white; border-radius: 16px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1); overflow: hidden; }
    .card.full-width { grid-column: 1 / -1; }
    .card-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid #f3f4f6; flex-wrap: wrap; gap: 12px; }
    .card-header h2 { margin: 0; font-size: 18px; color: #1f2937; }
    .header-right { display: flex; align-items: center; gap: 12px; }
    .chart-wrapper { padding: 24px; display: flex; justify-content: center; }
    .chart-legend { display: flex; justify-content: center; gap: 24px; padding: 0 24px 24px; }
    .legend-item { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #6b7280; }
    .legend-color { width: 12px; height: 12px; border-radius: 2px; }
    .legend-color.completed { background: #10b981; }
    .legend-color.in-progress { background: #3b82f6; }
    .legend-color.pending { background: #f59e0b; }
    .badge { padding: 4px 12px; border-radius: 20px; font-size: 13px; font-weight: 600; }
    .badge.completed { background: #d1fae5; color: #065f46; }
    .badge.pending { background: #fef3c7; color: #92400e; }
    .task-list { padding: 12px; max-height: 400px; overflow-y: auto; }
    .task-item { display: flex; align-items: center; gap: 12px; padding: 12px; border-radius: 8px; transition: background 0.2s; cursor: pointer; }
    .task-item:hover { background: #f9fafb; }
    .task-item.completed { opacity: 0.7; }
    .task-item.completed .task-subject { text-decoration: line-through; }
    .task-item.blocked { background: #fef2f2; }
    .task-toggle { width: 32px; height: 32px; border: none; background: none; font-size: 18px; cursor: pointer; border-radius: 4px; }
    .task-toggle:hover { background: #e5e7eb; }
    .task-content { flex: 1; display: flex; flex-direction: column; gap: 4px; }
    .task-subject { font-weight: 500; color: #1f2937; }
    .task-deliverable { font-size: 12px; color: #3b82f6; }
    .task-blocked { font-size: 12px; color: #dc2626; }
    .task-category, .category-badge { padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; text-transform: uppercase; }
    .task-category.investor, .category-badge.investor { background: #dbeafe; color: #1d4ed8; }
    .task-category.customer, .category-badge.customer { background: #d1fae5; color: #065f46; }
    .task-category.content, .category-badge.content { background: #fce7f3; color: #be185d; }
    .task-category.application, .category-badge.application { background: #fef3c7; color: #92400e; }
    .task-category.manual, .category-badge.manual { background: #f3f4f6; color: #4b5563; }
    .quick-actions { display: flex; gap: 16px; padding: 24px; flex-wrap: wrap; }
    .action-btn { display: flex; align-items: center; gap: 8px; padding: 12px 20px; background: #f3f4f6; border: 1px solid #e5e7eb; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
    .action-btn:hover { background: #e5e7eb; border-color: #d1d5db; }
    .action-icon { font-size: 20px; }
    .activity-list { padding: 12px; }
    .activity-item { display: flex; align-items: center; gap: 12px; padding: 12px; border-bottom: 1px solid #f3f4f6; }
    .activity-item:last-child { border-bottom: none; }
    .activity-icon { font-size: 20px; }
    .activity-content { flex: 1; display: flex; flex-direction: column; }
    .activity-contact { font-weight: 500; }
    .activity-type { font-size: 12px; color: #6b7280; text-transform: capitalize; }
    .activity-date { font-size: 12px; color: #9ca3af; }
    .activity-status { padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; text-transform: capitalize; }
    .activity-status.sent { background: #dbeafe; color: #1d4ed8; }
    .activity-status.responded { background: #d1fae5; color: #065f46; }
    .activity-status.no_response { background: #f3f4f6; color: #6b7280; }
    .empty-state { padding: 48px; text-align: center; color: #6b7280; }
    .empty-state h3 { color: #1f2937; margin-bottom: 8px; }
    .filter-tabs { display: flex; gap: 8px; }
    .filter-tabs button { padding: 6px 12px; background: #f3f4f6; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; }
    .filter-tabs button.active { background: #3b82f6; color: white; }
    .tasks-table { padding: 0; }
    .table-header, .table-row { display: grid; grid-template-columns: 150px 1fr 100px 80px 100px; padding: 12px 24px; align-items: center; }
    .table-header { background: #f9fafb; font-weight: 600; font-size: 13px; color: #6b7280; }
    .table-row { border-bottom: 1px solid #f3f4f6; cursor: pointer; }
    .table-row:hover { background: #f9fafb; }
    .table-row.completed { opacity: 0.6; }
    .col-task small { color: #9ca3af; }
    .doc-link { color: #3b82f6; text-decoration: none; }
    .contact-row { display: flex; align-items: center; padding: 16px 24px; border-bottom: 1px solid #f3f4f6; cursor: pointer; }
    .contact-row:hover { background: #f9fafb; }
    .contact-info { flex: 1; display: flex; flex-direction: column; gap: 2px; }
    .contact-name { font-weight: 600; color: #1f2937; }
    .contact-title { font-size: 13px; color: #6b7280; }
    .contact-org { font-size: 12px; color: #9ca3af; }
    .contact-status { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; margin-right: 16px; text-transform: capitalize; }
    .contact-status.not_contacted { background: #f3f4f6; color: #6b7280; }
    .contact-status.connection_sent { background: #dbeafe; color: #1d4ed8; }
    .contact-status.connected { background: #d1fae5; color: #065f46; }
    .contact-status.in_conversation { background: #fef3c7; color: #92400e; }
    .contact-status.meeting_scheduled { background: #ede9fe; color: #6d28d9; }
    .contact-status.warm_lead { background: #fee2e2; color: #dc2626; }
    .contact-actions { display: flex; gap: 8px; }
    .action-link { text-decoration: none; font-size: 18px; }
    .action-btn-sm { width: 32px; height: 32px; border: 1px solid #e5e7eb; background: white; border-radius: 6px; cursor: pointer; }
    .action-btn-sm:hover { background: #f3f4f6; }
    .documents-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; padding: 24px; }
    .document-card { background: white; border: 1px solid #e5e7eb; border-radius: 12px; padding: 20px; display: flex; gap: 16px; border-left: 4px solid; }
    .document-card.financial { border-left-color: #10b981; }
    .document-card.outreach { border-left-color: #3b82f6; }
    .document-card.target_list { border-left-color: #8b5cf6; }
    .document-card.content { border-left-color: #ec4899; }
    .document-card.application { border-left-color: #f59e0b; }
    .doc-icon { font-size: 32px; }
    .doc-content { flex: 1; }
    .doc-content h3 { margin: 0 0 8px 0; font-size: 16px; color: #1f2937; }
    .doc-content p { margin: 0 0 8px 0; font-size: 13px; color: #6b7280; }
    .doc-path { font-size: 11px; color: #9ca3af; font-family: monospace; }
    @media (max-width: 1200px) { .overview-grid { grid-template-columns: repeat(2, 1fr); } }
    @media (max-width: 1024px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }
    @media (max-width: 768px) { .overview-grid { grid-template-columns: 1fr; } .stats-grid { grid-template-columns: 1fr; } .table-header, .table-row { grid-template-columns: 1fr; gap: 8px; } }
  `],
})
export class InvestorLaunchComponent {
  private investorService = inject(InvestorService);
  private authService = inject(InvestorAuthService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  selectedView = 'overview';
  taskFilter: 'all' | 'pending' | 'in_progress' | 'completed' = 'all';
  contactFilter: 'all' | 'quality_leader' | 'investor' | 'angel' | 'partner' | 'customer' = 'all';

  // Signals from services
  stats = this.investorService.stats;
  activities = this.investorService.activities;
  contacts = this.investorService.contacts;
  documents = this.investorService.documents;
  isLoading = this.investorService.isLoading;
  currentUser = this.authService.currentUser;
  userDisplayName = this.authService.userDisplayName;
  linkedInConnected = this.authService.isLinkedInConnected;

  // Search
  searchQuery = signal('');
  searchResults = signal<{ tasks: InvestorTask[]; contacts: Contact[] }>({ tasks: [], contacts: [] });

  // Computed signals
  week1Tasks = computed(() => this.investorService.tasksByWeek().week1);
  week2Tasks = computed(() => this.investorService.tasksByWeek().week2);

  week1CompletedCount = computed(() =>
    this.week1Tasks().filter(t => t.status === 'completed').length
  );

  week2CompletedCount = computed(() =>
    this.week2Tasks().filter(t => t.status === 'completed').length
  );

  taskProgressPercent = computed(() => {
    const s = this.stats();
    return s.tasksTotal > 0 ? (s.tasksCompleted / s.tasksTotal) * 100 : 0;
  });

  recentActivities = computed(() => this.activities().slice(0, 10));

  filteredTasks = computed(() => {
    const tasks = [...this.week1Tasks(), ...this.week2Tasks()];
    if (this.taskFilter === 'all') return tasks;
    return tasks.filter(t => t.status === this.taskFilter);
  });

  filteredContacts = computed(() => {
    const contacts = this.contacts();
    if (this.contactFilter === 'all') return contacts;
    return contacts.filter(c => c.category === this.contactFilter);
  });

  chartData = computed<ProgressChartData>(() => ({
    completed: this.stats().tasksCompleted,
    inProgress: this.stats().tasksInProgress,
    pending: this.stats().tasksPending,
    blocked: 0,
  }));

  // Helper methods
  hasBlockers(task: InvestorTask): boolean {
    return !!(task.blockedBy && task.blockedBy.length > 0);
  }

  getBlockerIds(task: InvestorTask): string {
    return task.blockedBy?.join(', #') || '';
  }

  getStatusIcon(status: string): string {
    const icons: Record<string, string> = {
      completed: '✅',
      in_progress: '🔄',
      pending: '⬜',
      blocked: '🔒',
    };
    return icons[status] || '⬜';
  }

  getActivityIcon(type: string): string {
    const icons: Record<string, string> = {
      linkedin_request: '🔗',
      linkedin_message: '💬',
      email: '📧',
      call: '📞',
      meeting: '📅',
      follow_up: '📝',
    };
    return icons[type] || '📝';
  }

  getDocIcon(category: string): string {
    const icons: Record<string, string> = {
      financial: '💰',
      outreach: '📧',
      target_list: '📋',
      content: '✍️',
      application: '📝',
    };
    return icons[category] || '📄';
  }

  // Task operations
  toggleTask(task: InvestorTask): void {
    const nextStatus: Record<string, InvestorTask['status']> = {
      pending: 'in_progress',
      in_progress: 'completed',
      completed: 'pending',
      blocked: 'pending',
    };
    this.investorService.updateTaskStatus(task.id, nextStatus[task.status]);
  }

  updateTaskStatus(taskId: string, status: InvestorTask['status']): void {
    this.investorService.updateTaskStatus(taskId, status);
  }

  updateContactStatus(contactId: string, status: Contact['status']): void {
    this.investorService.updateContactStatus(contactId, status);
    this.snackBar.open('Contact status updated', 'OK', { duration: 2000 });
  }

  // Dialog methods
  openTaskDialog(mode: 'create' | 'edit', task?: InvestorTask): void {
    const dialogRef = this.dialog.open(TaskDialogComponent, {
      width: '600px',
      data: { mode, task } as TaskDialogData,
    });

    dialogRef.afterClosed().subscribe((result: TaskDialogResult) => {
      if (result?.action === 'save' && result.task) {
        if (mode === 'create') {
          this.investorService.addTask(result.task as Omit<InvestorTask, 'id'>);
          this.snackBar.open('Task created', 'OK', { duration: 2000 });
        } else if (task) {
          this.investorService.updateTask(task.id, result.task);
          this.snackBar.open('Task updated', 'OK', { duration: 2000 });
        }
      }
    });
  }

  openContactDialog(mode: 'create' | 'edit', contact?: Contact): void {
    const dialogRef = this.dialog.open(ContactDialogComponent, {
      width: '600px',
      data: { mode, contact } as ContactDialogData,
    });

    dialogRef.afterClosed().subscribe((result: ContactDialogResult) => {
      if (result?.action === 'save' && result.contact) {
        if (mode === 'create') {
          this.investorService.addContact(result.contact as Omit<Contact, 'id'>);
          this.snackBar.open('Contact added', 'OK', { duration: 2000 });
        } else if (contact) {
          this.investorService.updateContact(contact.id, result.contact);
          this.snackBar.open('Contact updated', 'OK', { duration: 2000 });
        }
      }
    });
  }

  openActivityDialog(preselectedContactId?: string): void {
    const dialogRef = this.dialog.open(ActivityDialogComponent, {
      width: '500px',
      data: {
        mode: 'create',
        contacts: this.contacts(),
        preselectedContactId,
      } as ActivityDialogData,
    });

    dialogRef.afterClosed().subscribe((result: ActivityDialogResult) => {
      if (result?.action === 'save' && result.activity) {
        this.investorService.logActivity(result.activity as Omit<OutreachActivity, 'id'>);
        this.snackBar.open('Activity logged', 'OK', { duration: 2000 });
      }
    });
  }

  openActivityDialogWithType(type: OutreachActivity['type']): void {
    const dialogRef = this.dialog.open(ActivityDialogComponent, {
      width: '500px',
      data: {
        mode: 'create',
        contacts: this.contacts(),
        activity: { type },
      } as ActivityDialogData,
    });

    dialogRef.afterClosed().subscribe((result: ActivityDialogResult) => {
      if (result?.action === 'save' && result.activity) {
        this.investorService.logActivity(result.activity as Omit<OutreachActivity, 'id'>);
        this.snackBar.open('Activity logged', 'OK', { duration: 2000 });
      }
    });
  }

  logActivityForContact(contact: Contact, type: OutreachActivity['type']): void {
    this.investorService.logActivity({
      contactId: contact.id,
      contactName: contact.name,
      type,
      status: 'sent',
      date: new Date().toISOString().split('T')[0],
    });
    this.snackBar.open(`${type.replace('_', ' ')} logged for ${contact.name}`, 'OK', { duration: 2000 });
  }

  // Search
  onSearch(query: string): void {
    this.searchQuery.set(query);

    if (!query.trim()) {
      this.searchResults.set({ tasks: [], contacts: [] });
      return;
    }

    // Search tasks locally for now
    const tasks = [...this.week1Tasks(), ...this.week2Tasks()].filter(
      (t) =>
        t.subject.toLowerCase().includes(query.toLowerCase()) ||
        t.description?.toLowerCase().includes(query.toLowerCase())
    );

    const contacts = this.contacts().filter(
      (c) =>
        c.name.toLowerCase().includes(query.toLowerCase()) ||
        c.organization?.toLowerCase().includes(query.toLowerCase())
    );

    this.searchResults.set({ tasks, contacts });
  }

  clearSearch(): void {
    this.searchQuery.set('');
    this.searchResults.set({ tasks: [], contacts: [] });
  }

  // LinkedIn
  connectLinkedIn(): void {
    this.authService.getLinkedInAuthUrl().subscribe({
      next: (response) => {
        window.location.href = response.authorizationUrl;
      },
      error: (error) => {
        this.snackBar.open('Failed to connect LinkedIn: ' + error.message, 'OK', { duration: 3000 });
      },
    });
  }

  disconnectLinkedIn(): void {
    this.authService.disconnectLinkedIn().subscribe({
      next: () => {
        this.snackBar.open('LinkedIn disconnected', 'OK', { duration: 2000 });
      },
      error: (error) => {
        this.snackBar.open('Failed to disconnect: ' + error.message, 'OK', { duration: 3000 });
      },
    });
  }

  // Auth
  logout(): void {
    this.authService.logout();
  }

  // Export
  exportData(): void {
    const data = this.investorService.exportData();
    const blob = new Blob([data], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `hdim-investor-data-${new Date().toISOString().split('T')[0]}.json`;
    a.click();
    URL.revokeObjectURL(url);
    this.snackBar.open('Data exported', 'OK', { duration: 2000 });
  }
}
