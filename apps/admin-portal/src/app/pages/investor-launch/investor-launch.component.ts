import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InvestorService } from '../../services/investor.service';
import { InvestorTask, Contact, OutreachActivity, InvestorDocument } from '../../models/investor.model';

@Component({
  selector: 'app-investor-launch',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="investor-launch">
      <!-- Header -->
      <div class="header">
        <div class="header-content">
          <h1>🚀 Investor Launch Dashboard</h1>
          <p class="subtitle">Series A Fundraise & Customer Acquisition Tracker</p>
        </div>
        <div class="header-actions">
          <select [(ngModel)]="selectedView" class="view-select">
            <option value="overview">Overview</option>
            <option value="tasks">Task Tracker</option>
            <option value="contacts">Contacts CRM</option>
            <option value="documents">Documents</option>
          </select>
          <button class="btn-export" (click)="exportData()">📥 Export</button>
        </div>
      </div>

      <!-- Stats Cards -->
      <div class="stats-grid">
        <div class="stat-card primary">
          <div class="stat-icon">✅</div>
          <div class="stat-content">
            <span class="stat-value">{{ stats().tasksCompleted }}/{{ stats().tasksTotal }}</span>
            <span class="stat-label">Tasks Completed</span>
            <div class="progress-bar">
              <div class="progress-fill" [style.width.%]="(stats().tasksCompleted / stats().tasksTotal) * 100"></div>
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

      <!-- Main Content -->
      @if (selectedView === 'overview') {
        <div class="overview-grid">
          <!-- Week Progress -->
          <div class="card">
            <div class="card-header">
              <h2>📋 Week 1 Tasks</h2>
              <span class="badge completed">{{ tasksByWeek().week1.filter(t => t.status === 'completed').length }}/{{ tasksByWeek().week1.length }}</span>
            </div>
            <div class="task-list">
              @for (task of tasksByWeek().week1; track task.id) {
                <div class="task-item" [class.completed]="task.status === 'completed'" [class.in-progress]="task.status === 'in_progress'">
                  <button class="task-toggle" (click)="toggleTask(task)">
                    @if (task.status === 'completed') { ✅ }
                    @else if (task.status === 'in_progress') { 🔄 }
                    @else { ⬜ }
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

          <div class="card">
            <div class="card-header">
              <h2>📋 Week 2 Tasks</h2>
              <span class="badge pending">{{ tasksByWeek().week2.filter(t => t.status === 'completed').length }}/{{ tasksByWeek().week2.length }}</span>
            </div>
            <div class="task-list">
              @for (task of tasksByWeek().week2; track task.id) {
                <div class="task-item" [class.completed]="task.status === 'completed'" [class.in-progress]="task.status === 'in_progress'" [class.blocked]="task.blockedBy?.length">
                  <button class="task-toggle" (click)="toggleTask(task)">
                    @if (task.status === 'completed') { ✅ }
                    @else if (task.status === 'in_progress') { 🔄 }
                    @else if (task.blockedBy?.length) { 🔒 }
                    @else { ⬜ }
                  </button>
                  <div class="task-content">
                    <span class="task-subject">{{ task.subject }}</span>
                    @if (task.blockedBy?.length) {
                      <span class="task-blocked">Blocked by #{{ task.blockedBy.join(', #') }}</span>
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
              <button class="action-btn" (click)="logLinkedInRequest()">
                <span class="action-icon">🔗</span>
                <span>Log LinkedIn Request</span>
              </button>
              <button class="action-btn" (click)="logEmail()">
                <span class="action-icon">📧</span>
                <span>Log Email Sent</span>
              </button>
              <button class="action-btn" (click)="logMeeting()">
                <span class="action-icon">📅</span>
                <span>Schedule Meeting</span>
              </button>
              <button class="action-btn" (click)="addContact()">
                <span class="action-icon">👤</span>
                <span>Add Contact</span>
              </button>
            </div>
          </div>

          <!-- Recent Activity -->
          <div class="card full-width">
            <div class="card-header">
              <h2>📊 Recent Activity</h2>
            </div>
            @if (activities().length === 0) {
              <div class="empty-state">
                <p>No activities logged yet. Use Quick Actions to start tracking!</p>
              </div>
            } @else {
              <div class="activity-list">
                @for (activity of activities().slice(0, 10); track activity.id) {
                  <div class="activity-item">
                    <span class="activity-icon">
                      @switch (activity.type) {
                        @case ('linkedin_request') { 🔗 }
                        @case ('linkedin_message') { 💬 }
                        @case ('email') { 📧 }
                        @case ('call') { 📞 }
                        @case ('meeting') { 📅 }
                        @default { 📝 }
                      }
                    </span>
                    <div class="activity-content">
                      <span class="activity-contact">{{ activity.contactName }}</span>
                      <span class="activity-type">{{ activity.type | titlecase }}</span>
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
              <div class="filter-tabs">
                <button [class.active]="taskFilter === 'all'" (click)="taskFilter = 'all'">All</button>
                <button [class.active]="taskFilter === 'pending'" (click)="taskFilter = 'pending'">Pending</button>
                <button [class.active]="taskFilter === 'in_progress'" (click)="taskFilter = 'in_progress'">In Progress</button>
                <button [class.active]="taskFilter === 'completed'" (click)="taskFilter = 'completed'">Completed</button>
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
                <div class="table-row" [class.completed]="task.status === 'completed'">
                  <span class="col-status">
                    <select [ngModel]="task.status" (ngModelChange)="updateTaskStatus(task.id, $event)">
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
                      <a [href]="'vscode://file/' + task.deliverable" class="doc-link">📄 View</a>
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
              <div class="filter-tabs">
                <button [class.active]="contactFilter === 'all'" (click)="contactFilter = 'all'">All ({{ contacts().length }})</button>
                <button [class.active]="contactFilter === 'quality_leader'" (click)="contactFilter = 'quality_leader'">Quality Leaders</button>
                <button [class.active]="contactFilter === 'investor'" (click)="contactFilter = 'investor'">Investors</button>
                <button [class.active]="contactFilter === 'angel'" (click)="contactFilter = 'angel'">Angels</button>
              </div>
            </div>

            @if (filteredContacts().length === 0) {
              <div class="empty-state">
                <h3>No contacts yet</h3>
                <p>Import contacts from your quality-leader-profiles.md or add them manually.</p>
                <button class="btn-primary" (click)="addContact()">Add First Contact</button>
              </div>
            } @else {
              <div class="contacts-table">
                @for (contact of filteredContacts(); track contact.id) {
                  <div class="contact-row">
                    <div class="contact-info">
                      <span class="contact-name">{{ contact.name }}</span>
                      <span class="contact-title">{{ contact.title }}</span>
                      <span class="contact-org">{{ contact.organization }}</span>
                    </div>
                    <span class="contact-status" [class]="contact.status">{{ contact.status | titlecase }}</span>
                    <div class="contact-actions">
                      @if (contact.linkedInUrl) {
                        <a [href]="contact.linkedInUrl" target="_blank" class="action-link">🔗</a>
                      }
                      <button class="action-btn-sm" (click)="updateContactStatus(contact.id, 'connection_sent')">📤</button>
                      <button class="action-btn-sm" (click)="updateContactStatus(contact.id, 'connected')">✅</button>
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
                  <div class="doc-icon">
                    @switch (doc.category) {
                      @case ('financial') { 💰 }
                      @case ('outreach') { 📧 }
                      @case ('target_list') { 📋 }
                      @case ('content') { ✍️ }
                      @case ('application') { 📝 }
                      @default { 📄 }
                    }
                  </div>
                  <div class="doc-content">
                    <h3>{{ doc.name }}</h3>
                    <p>{{ doc.description }}</p>
                    <span class="doc-path">{{ doc.filePath }}</span>
                  </div>
                  <div class="doc-actions">
                    <a [href]="'vscode://file/' + projectRoot + '/' + doc.filePath" class="btn-sm">Open in VS Code</a>
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
    .investor-launch {
      max-width: 1400px;
      margin: 0 auto;
      padding: 24px;
    }

    .header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 32px;
      padding-bottom: 24px;
      border-bottom: 2px solid #e5e7eb;
    }

    .header h1 {
      margin: 0;
      font-size: 32px;
      color: #1f2937;
    }

    .subtitle {
      color: #6b7280;
      margin-top: 4px;
    }

    .header-actions {
      display: flex;
      gap: 12px;
      align-items: center;
    }

    .view-select {
      padding: 10px 16px;
      border: 1px solid #d1d5db;
      border-radius: 8px;
      font-size: 14px;
      background: white;
    }

    .btn-export {
      padding: 10px 16px;
      background: #f3f4f6;
      border: 1px solid #d1d5db;
      border-radius: 8px;
      cursor: pointer;
      font-size: 14px;
    }

    .btn-export:hover {
      background: #e5e7eb;
    }

    /* Stats Grid */
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 20px;
      margin-bottom: 32px;
    }

    .stat-card {
      background: white;
      border-radius: 16px;
      padding: 24px;
      display: flex;
      gap: 16px;
      align-items: flex-start;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
      border-left: 4px solid;
    }

    .stat-card.primary { border-left-color: #3b82f6; }
    .stat-card.accent { border-left-color: #8b5cf6; }
    .stat-card.success { border-left-color: #10b981; }
    .stat-card.warning { border-left-color: #f59e0b; }

    .stat-icon {
      font-size: 32px;
    }

    .stat-content {
      display: flex;
      flex-direction: column;
      flex: 1;
    }

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: #1f2937;
    }

    .stat-label {
      font-size: 14px;
      color: #6b7280;
    }

    .stat-detail {
      font-size: 12px;
      color: #9ca3af;
      margin-top: 4px;
    }

    .progress-bar {
      height: 6px;
      background: #e5e7eb;
      border-radius: 3px;
      margin-top: 8px;
      overflow: hidden;
    }

    .progress-fill {
      height: 100%;
      background: #3b82f6;
      border-radius: 3px;
      transition: width 0.3s ease;
    }

    /* Overview Grid */
    .overview-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 24px;
    }

    .card {
      background: white;
      border-radius: 16px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
      overflow: hidden;
    }

    .card.full-width {
      grid-column: 1 / -1;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px 24px;
      border-bottom: 1px solid #f3f4f6;
    }

    .card-header h2 {
      margin: 0;
      font-size: 18px;
      color: #1f2937;
    }

    .badge {
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 13px;
      font-weight: 600;
    }

    .badge.completed { background: #d1fae5; color: #065f46; }
    .badge.pending { background: #fef3c7; color: #92400e; }

    /* Task List */
    .task-list {
      padding: 12px;
    }

    .task-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      border-radius: 8px;
      transition: background 0.2s;
    }

    .task-item:hover {
      background: #f9fafb;
    }

    .task-item.completed {
      opacity: 0.7;
    }

    .task-item.completed .task-subject {
      text-decoration: line-through;
    }

    .task-item.blocked {
      background: #fef2f2;
    }

    .task-toggle {
      width: 32px;
      height: 32px;
      border: none;
      background: none;
      font-size: 18px;
      cursor: pointer;
      border-radius: 4px;
    }

    .task-toggle:hover {
      background: #e5e7eb;
    }

    .task-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .task-subject {
      font-weight: 500;
      color: #1f2937;
    }

    .task-deliverable {
      font-size: 12px;
      color: #3b82f6;
    }

    .task-blocked {
      font-size: 12px;
      color: #dc2626;
    }

    .task-category {
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 600;
      text-transform: uppercase;
    }

    .task-category.investor { background: #dbeafe; color: #1d4ed8; }
    .task-category.customer { background: #d1fae5; color: #065f46; }
    .task-category.content { background: #fce7f3; color: #be185d; }
    .task-category.application { background: #fef3c7; color: #92400e; }
    .task-category.manual { background: #f3f4f6; color: #4b5563; }

    /* Quick Actions */
    .quick-actions {
      display: flex;
      gap: 16px;
      padding: 24px;
      flex-wrap: wrap;
    }

    .action-btn {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 20px;
      background: #f3f4f6;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;
    }

    .action-btn:hover {
      background: #e5e7eb;
      border-color: #d1d5db;
    }

    .action-icon {
      font-size: 20px;
    }

    /* Activity List */
    .activity-list {
      padding: 12px;
    }

    .activity-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      border-bottom: 1px solid #f3f4f6;
    }

    .activity-item:last-child {
      border-bottom: none;
    }

    .activity-icon {
      font-size: 20px;
    }

    .activity-content {
      flex: 1;
      display: flex;
      flex-direction: column;
    }

    .activity-contact {
      font-weight: 500;
    }

    .activity-type {
      font-size: 12px;
      color: #6b7280;
    }

    .activity-date {
      font-size: 12px;
      color: #9ca3af;
    }

    .activity-status {
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 600;
    }

    .activity-status.sent { background: #dbeafe; color: #1d4ed8; }
    .activity-status.responded { background: #d1fae5; color: #065f46; }
    .activity-status.no_response { background: #f3f4f6; color: #6b7280; }

    /* Empty State */
    .empty-state {
      padding: 48px;
      text-align: center;
      color: #6b7280;
    }

    .empty-state h3 {
      color: #1f2937;
      margin-bottom: 8px;
    }

    .btn-primary {
      margin-top: 16px;
      padding: 12px 24px;
      background: #3b82f6;
      color: white;
      border: none;
      border-radius: 8px;
      cursor: pointer;
      font-weight: 500;
    }

    .btn-primary:hover {
      background: #2563eb;
    }

    /* Filter Tabs */
    .filter-tabs {
      display: flex;
      gap: 8px;
    }

    .filter-tabs button {
      padding: 6px 12px;
      background: #f3f4f6;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      font-size: 13px;
    }

    .filter-tabs button.active {
      background: #3b82f6;
      color: white;
    }

    /* Tasks Table */
    .tasks-table {
      padding: 0;
    }

    .table-header, .table-row {
      display: grid;
      grid-template-columns: 150px 1fr 100px 80px 100px;
      padding: 12px 24px;
      align-items: center;
    }

    .table-header {
      background: #f9fafb;
      font-weight: 600;
      font-size: 13px;
      color: #6b7280;
    }

    .table-row {
      border-bottom: 1px solid #f3f4f6;
    }

    .table-row:hover {
      background: #f9fafb;
    }

    .table-row.completed {
      opacity: 0.6;
    }

    .col-task small {
      color: #9ca3af;
    }

    .category-badge {
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 600;
    }

    .category-badge.investor { background: #dbeafe; color: #1d4ed8; }
    .category-badge.customer { background: #d1fae5; color: #065f46; }
    .category-badge.content { background: #fce7f3; color: #be185d; }
    .category-badge.application { background: #fef3c7; color: #92400e; }
    .category-badge.manual { background: #f3f4f6; color: #4b5563; }

    .doc-link {
      color: #3b82f6;
      text-decoration: none;
    }

    /* Contacts View */
    .contact-row {
      display: flex;
      align-items: center;
      padding: 16px 24px;
      border-bottom: 1px solid #f3f4f6;
    }

    .contact-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .contact-name {
      font-weight: 600;
      color: #1f2937;
    }

    .contact-title {
      font-size: 13px;
      color: #6b7280;
    }

    .contact-org {
      font-size: 12px;
      color: #9ca3af;
    }

    .contact-status {
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 500;
      margin-right: 16px;
    }

    .contact-status.not_contacted { background: #f3f4f6; color: #6b7280; }
    .contact-status.connection_sent { background: #dbeafe; color: #1d4ed8; }
    .contact-status.connected { background: #d1fae5; color: #065f46; }
    .contact-status.in_conversation { background: #fef3c7; color: #92400e; }
    .contact-status.meeting_scheduled { background: #ede9fe; color: #6d28d9; }
    .contact-status.warm_lead { background: #fee2e2; color: #dc2626; }

    .contact-actions {
      display: flex;
      gap: 8px;
    }

    .action-link {
      text-decoration: none;
      font-size: 18px;
    }

    .action-btn-sm {
      width: 32px;
      height: 32px;
      border: 1px solid #e5e7eb;
      background: white;
      border-radius: 6px;
      cursor: pointer;
    }

    .action-btn-sm:hover {
      background: #f3f4f6;
    }

    /* Documents Grid */
    .documents-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 16px;
      padding: 24px;
    }

    .document-card {
      background: white;
      border: 1px solid #e5e7eb;
      border-radius: 12px;
      padding: 20px;
      display: flex;
      gap: 16px;
      border-left: 4px solid;
    }

    .document-card.financial { border-left-color: #10b981; }
    .document-card.outreach { border-left-color: #3b82f6; }
    .document-card.target_list { border-left-color: #8b5cf6; }
    .document-card.content { border-left-color: #ec4899; }
    .document-card.application { border-left-color: #f59e0b; }

    .doc-icon {
      font-size: 32px;
    }

    .doc-content {
      flex: 1;
    }

    .doc-content h3 {
      margin: 0 0 8px 0;
      font-size: 16px;
      color: #1f2937;
    }

    .doc-content p {
      margin: 0 0 8px 0;
      font-size: 13px;
      color: #6b7280;
    }

    .doc-path {
      font-size: 11px;
      color: #9ca3af;
      font-family: monospace;
    }

    .doc-actions {
      display: flex;
      align-items: flex-end;
    }

    .btn-sm {
      padding: 6px 12px;
      background: #f3f4f6;
      border: 1px solid #e5e7eb;
      border-radius: 6px;
      font-size: 12px;
      text-decoration: none;
      color: #374151;
    }

    .btn-sm:hover {
      background: #e5e7eb;
    }

    /* Responsive */
    @media (max-width: 1024px) {
      .stats-grid {
        grid-template-columns: repeat(2, 1fr);
      }

      .overview-grid {
        grid-template-columns: 1fr;
      }
    }

    @media (max-width: 640px) {
      .stats-grid {
        grid-template-columns: 1fr;
      }

      .table-header, .table-row {
        grid-template-columns: 1fr;
        gap: 8px;
      }
    }
  `],
})
export class InvestorLaunchComponent {
  projectRoot = '/mnt/wdblack/dev/projects/hdim-master';

  selectedView = 'overview';
  taskFilter: 'all' | 'pending' | 'in_progress' | 'completed' = 'all';
  contactFilter: 'all' | 'quality_leader' | 'investor' | 'angel' | 'partner' | 'customer' = 'all';

  // Signals from service
  stats = this.investorService.stats;
  tasksByWeek = this.investorService.tasksByWeek;
  activities = this.investorService.activities;
  contacts = this.investorService.contacts;
  documents = this.investorService.documents;

  constructor(private investorService: InvestorService) {}

  filteredTasks = computed(() => {
    const tasks = [...this.investorService.tasksByWeek().week1, ...this.investorService.tasksByWeek().week2];
    if (this.taskFilter === 'all') return tasks;
    return tasks.filter((t) => t.status === this.taskFilter);
  });

  filteredContacts = computed(() => {
    const contacts = this.contacts();
    if (this.contactFilter === 'all') return contacts;
    return contacts.filter((c) => c.category === this.contactFilter);
  });

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
  }

  logLinkedInRequest(): void {
    const name = prompt('Contact name:');
    if (name) {
      this.investorService.logActivity({
        contactId: `temp_${Date.now()}`,
        contactName: name,
        type: 'linkedin_request',
        status: 'sent',
        date: new Date().toISOString().split('T')[0],
      });
    }
  }

  logEmail(): void {
    const name = prompt('Contact name:');
    const subject = prompt('Email subject:');
    if (name && subject) {
      this.investorService.logActivity({
        contactId: `temp_${Date.now()}`,
        contactName: name,
        type: 'email',
        status: 'sent',
        date: new Date().toISOString().split('T')[0],
        subject,
      });
    }
  }

  logMeeting(): void {
    const name = prompt('Contact name:');
    if (name) {
      this.investorService.logActivity({
        contactId: `temp_${Date.now()}`,
        contactName: name,
        type: 'meeting',
        status: 'scheduled',
        date: new Date().toISOString().split('T')[0],
      });
    }
  }

  addContact(): void {
    const name = prompt('Contact name:');
    const title = prompt('Title:');
    const org = prompt('Organization:');
    const category = prompt('Category (quality_leader/investor/angel):') as Contact['category'];

    if (name && title && org) {
      this.investorService.addContact({
        name,
        title,
        organization: org,
        category: category || 'quality_leader',
        status: 'not_contacted',
      });
    }
  }

  exportData(): void {
    const data = this.investorService.exportData();
    const blob = new Blob([data], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `hdim-investor-data-${new Date().toISOString().split('T')[0]}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }
}
