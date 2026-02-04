import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { SalesSequenceService } from '../../../services/sales-sequence.service';
import { SalesService } from '../../../services/sales.service';
import {
  EmailSequence,
  SequenceCreateRequest,
  SequenceEnrollment,
  SequenceAnalytics,
  SequenceStatus,
  Lead,
} from '../../../models/sales.model';

@Component({
  selector: 'app-sales-sequences',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="sequences-page">
      <div class="page-header">
        <div class="header-content">
          <h2>Email Sequences</h2>
          <p class="subtitle">Automate your outreach with multi-step email campaigns</p>
        </div>
        <div class="header-actions">
          <button class="btn btn-primary" (click)="openCreateDialog()">
            + New Sequence
          </button>
        </div>
      </div>

      <!-- Filters -->
      <div class="filters-bar">
        <div class="search-box">
          <input
            type="text"
            [(ngModel)]="searchQuery"
            placeholder="Search sequences..."
            (input)="onSearch()"
          />
        </div>
        <div class="filter-group">
          <select [(ngModel)]="statusFilter" (change)="applyFilters()">
            <option value="">All Statuses</option>
            <option *ngFor="let status of statuses" [value]="status">
              {{ formatStatus(status) }}
            </option>
          </select>
          <button class="btn btn-secondary" (click)="loadSequences()">
            Refresh
          </button>
        </div>
      </div>

      <!-- Loading State -->
      <div class="loading" *ngIf="isLoading()">
        <div class="spinner"></div>
        <span>Loading sequences...</span>
      </div>

      <!-- Sequences Table -->
      <div class="table-container" *ngIf="!isLoading()">
        <table class="sequences-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Status</th>
              <th>Steps</th>
              <th>Enrolled</th>
              <th>Completed</th>
              <th>Reply Rate</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let sequence of filteredSequences()" (click)="selectSequence(sequence)">
              <td class="name-cell">
                <div class="sequence-icon">📋</div>
                <div class="sequence-name">
                  <span class="name">{{ sequence.name }}</span>
                  <span class="description" *ngIf="sequence.description">{{ sequence.description }}</span>
                </div>
              </td>
              <td>
                <span class="status-badge" [class]="sequence.status.toLowerCase()">
                  {{ formatStatus(sequence.status) }}
                </span>
              </td>
              <td>{{ sequence.steps?.length || 0 }}</td>
              <td>{{ sequence.enrollmentCount }}</td>
              <td>{{ sequence.completedCount }}</td>
              <td>
                <span class="rate-value">{{ (sequence.replyRate * 100) | number:'1.1-1' }}%</span>
              </td>
              <td class="actions-cell" (click)="$event.stopPropagation()">
                <button
                  class="action-btn"
                  [title]="sequence.status === 'ACTIVE' ? 'Deactivate' : 'Activate'"
                  (click)="toggleSequenceStatus(sequence)"
                >
                  {{ sequence.status === 'ACTIVE' ? '⏸️' : '▶️' }}
                </button>
                <button class="action-btn" title="Edit" (click)="editSequence(sequence)">✏️</button>
                <button class="action-btn" title="Enroll" (click)="openEnrollDialog(sequence)">👤</button>
                <button class="action-btn delete" title="Delete" (click)="deleteSequence(sequence)">🗑️</button>
              </td>
            </tr>
            <tr *ngIf="!filteredSequences().length">
              <td colspan="7" class="empty-state">
                No sequences found. Create your first sequence to get started.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="pagination" *ngIf="totalPages > 1">
        <button [disabled]="currentPage === 0" (click)="changePage(currentPage - 1)">Previous</button>
        <span>Page {{ currentPage + 1 }} of {{ totalPages }}</span>
        <button [disabled]="currentPage >= totalPages - 1" (click)="changePage(currentPage + 1)">Next</button>
      </div>

      <!-- Create/Edit Dialog -->
      <div class="dialog-overlay" *ngIf="showCreateDialog" (click)="closeDialog()">
        <div class="dialog large" (click)="$event.stopPropagation()">
          <div class="dialog-header">
            <h3>{{ editingSequence ? 'Edit Sequence' : 'Create New Sequence' }}</h3>
            <button class="close-btn" (click)="closeDialog()">×</button>
          </div>
          <form (ngSubmit)="saveSequence()" class="dialog-form">
            <div class="form-group">
              <label>Sequence Name *</label>
              <input type="text" [(ngModel)]="sequenceForm.name" name="name" required />
            </div>
            <div class="form-group">
              <label>Description</label>
              <textarea [(ngModel)]="sequenceForm.description" name="description" rows="2"></textarea>
            </div>

            <!-- Steps Builder -->
            <div class="steps-section">
              <div class="steps-header">
                <label>Sequence Steps</label>
                <button type="button" class="btn btn-secondary btn-sm" (click)="addStep()">
                  + Add Step
                </button>
              </div>

              <div class="steps-list" *ngIf="sequenceForm.steps.length > 0">
                <div
                  class="step-item"
                  *ngFor="let step of sequenceForm.steps; let i = index"
                  [class.dragging]="draggingIndex === i"
                >
                  <div class="step-number">{{ i + 1 }}</div>
                  <div class="step-content">
                    <div class="step-row">
                      <select [(ngModel)]="step.type" [name]="'stepType' + i">
                        <option value="EMAIL">📧 Email</option>
                        <option value="WAIT">⏰ Wait</option>
                        <option value="TASK">✅ Task</option>
                        <option value="LINKEDIN">🔗 LinkedIn</option>
                      </select>
                      <div class="delay-inputs">
                        <input
                          type="number"
                          [(ngModel)]="step.delayDays"
                          [name]="'delayDays' + i"
                          min="0"
                          placeholder="Days"
                        />
                        <span>days</span>
                        <input
                          type="number"
                          [(ngModel)]="step.delayHours"
                          [name]="'delayHours' + i"
                          min="0"
                          max="23"
                          placeholder="Hours"
                        />
                        <span>hours</span>
                      </div>
                    </div>
                    <div class="step-row" *ngIf="step.type === 'EMAIL'">
                      <input
                        type="text"
                        [(ngModel)]="step.subject"
                        [name]="'subject' + i"
                        placeholder="Email subject"
                        class="full-width"
                      />
                    </div>
                    <div class="step-row" *ngIf="step.type === 'EMAIL' || step.type === 'TASK'">
                      <textarea
                        [(ngModel)]="step.content"
                        [name]="'content' + i"
                        placeholder="{{ step.type === 'EMAIL' ? 'Email content...' : 'Task description...' }}"
                        rows="3"
                        class="full-width"
                      ></textarea>
                    </div>
                  </div>
                  <button
                    type="button"
                    class="remove-step-btn"
                    (click)="removeStep(i)"
                    title="Remove step"
                  >
                    ×
                  </button>
                </div>
              </div>

              <div class="empty-steps" *ngIf="sequenceForm.steps.length === 0">
                <p>No steps yet. Add steps to build your sequence.</p>
              </div>
            </div>

            <div class="dialog-actions">
              <button type="button" class="btn btn-secondary" (click)="closeDialog()">Cancel</button>
              <button type="submit" class="btn btn-primary">{{ editingSequence ? 'Save Changes' : 'Create Sequence' }}</button>
            </div>
          </form>
        </div>
      </div>

      <!-- Enroll Dialog -->
      <div class="dialog-overlay" *ngIf="showEnrollDialog" (click)="closeEnrollDialog()">
        <div class="dialog" (click)="$event.stopPropagation()">
          <div class="dialog-header">
            <h3>Enroll in Sequence</h3>
            <button class="close-btn" (click)="closeEnrollDialog()">×</button>
          </div>
          <div class="dialog-form">
            <div class="form-group">
              <label>Sequence</label>
              <input type="text" [value]="enrollingSequence?.name" disabled />
            </div>
            <div class="form-group">
              <label>Select Lead to Enroll</label>
              <select [(ngModel)]="selectedLeadId">
                <option value="">-- Select a Lead --</option>
                <option *ngFor="let lead of leads()" [value]="lead.id">
                  {{ lead.firstName }} {{ lead.lastName }} ({{ lead.company }})
                </option>
              </select>
            </div>
            <div class="dialog-actions">
              <button type="button" class="btn btn-secondary" (click)="closeEnrollDialog()">Cancel</button>
              <button
                type="button"
                class="btn btn-primary"
                [disabled]="!selectedLeadId"
                (click)="enrollLead()"
              >
                Enroll Lead
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Sequence Detail Panel -->
      <div class="detail-panel" *ngIf="selectedSequence()" (click)="selectedSequence.set(null)">
        <div class="detail-content" (click)="$event.stopPropagation()">
          <div class="detail-header">
            <div class="sequence-info">
              <div class="sequence-icon large">📋</div>
              <div>
                <h3>{{ selectedSequence()!.name }}</h3>
                <p *ngIf="selectedSequence()!.description">{{ selectedSequence()!.description }}</p>
              </div>
            </div>
            <button class="close-btn" (click)="selectedSequence.set(null)">×</button>
          </div>

          <div class="detail-body">
            <div class="detail-section">
              <h4>Status</h4>
              <div class="status-toggle">
                <span class="status-badge large" [class]="selectedSequence()!.status.toLowerCase()">
                  {{ formatStatus(selectedSequence()!.status) }}
                </span>
                <button
                  class="btn btn-secondary btn-sm"
                  (click)="toggleSequenceStatus(selectedSequence()!)"
                >
                  {{ selectedSequence()!.status === 'ACTIVE' ? 'Deactivate' : 'Activate' }}
                </button>
              </div>
            </div>

            <div class="detail-section">
              <h4>Steps ({{ selectedSequence()!.steps?.length || 0 }})</h4>
              <div class="steps-timeline">
                <div class="timeline-item" *ngFor="let step of selectedSequence()!.steps; let i = index">
                  <div class="timeline-icon" [class]="step.type.toLowerCase()">
                    {{ getStepIcon(step.type) }}
                  </div>
                  <div class="timeline-content">
                    <span class="timeline-label">{{ formatStepType(step.type) }}</span>
                    <span class="timeline-subject" *ngIf="step.subject">{{ step.subject }}</span>
                    <span class="timeline-delay">
                      {{ step.delayDays > 0 ? step.delayDays + 'd' : '' }}
                      {{ step.delayHours > 0 ? step.delayHours + 'h' : '' }}
                      {{ step.delayDays === 0 && step.delayHours === 0 ? 'Immediate' : 'delay' }}
                    </span>
                  </div>
                </div>
                <div class="empty-timeline" *ngIf="!selectedSequence()!.steps?.length">
                  No steps defined
                </div>
              </div>
            </div>

            <div class="detail-section" *ngIf="sequenceAnalytics()">
              <h4>Analytics</h4>
              <div class="analytics-grid">
                <div class="analytics-item">
                  <span class="analytics-value">{{ sequenceAnalytics()!.emailsSent }}</span>
                  <span class="analytics-label">Sent</span>
                </div>
                <div class="analytics-item">
                  <span class="analytics-value">{{ sequenceAnalytics()!.emailsOpened }}</span>
                  <span class="analytics-label">Opened</span>
                </div>
                <div class="analytics-item">
                  <span class="analytics-value">{{ sequenceAnalytics()!.emailsClicked }}</span>
                  <span class="analytics-label">Clicked</span>
                </div>
                <div class="analytics-item">
                  <span class="analytics-value">{{ sequenceAnalytics()!.replies }}</span>
                  <span class="analytics-label">Replies</span>
                </div>
              </div>
              <div class="analytics-rates">
                <div class="rate-item">
                  <span class="rate-label">Open Rate:</span>
                  <span class="rate-value">{{ (sequenceAnalytics()!.openRate * 100) | number:'1.1-1' }}%</span>
                </div>
                <div class="rate-item">
                  <span class="rate-label">Click Rate:</span>
                  <span class="rate-value">{{ (sequenceAnalytics()!.clickRate * 100) | number:'1.1-1' }}%</span>
                </div>
                <div class="rate-item">
                  <span class="rate-label">Reply Rate:</span>
                  <span class="rate-value">{{ (sequenceAnalytics()!.replyRate * 100) | number:'1.1-1' }}%</span>
                </div>
              </div>
            </div>

            <div class="detail-section">
              <h4>Enrollments ({{ enrollments().length }})</h4>
              <div class="enrollments-list">
                <div class="enrollment-item" *ngFor="let enrollment of enrollments()">
                  <div class="enrollment-info">
                    <span class="enrollment-step">Step {{ enrollment.currentStep }}</span>
                    <span class="enrollment-status" [class]="enrollment.status.toLowerCase()">
                      {{ formatEnrollmentStatus(enrollment.status) }}
                    </span>
                  </div>
                  <div class="enrollment-actions">
                    <button
                      class="btn btn-sm"
                      *ngIf="enrollment.status === 'ACTIVE'"
                      (click)="pauseEnrollment(enrollment)"
                    >
                      Pause
                    </button>
                    <button
                      class="btn btn-sm"
                      *ngIf="enrollment.status === 'PAUSED'"
                      (click)="resumeEnrollment(enrollment)"
                    >
                      Resume
                    </button>
                  </div>
                </div>
                <div class="empty-enrollments" *ngIf="!enrollments().length">
                  No active enrollments
                </div>
              </div>
            </div>
          </div>

          <div class="detail-actions">
            <button class="btn btn-secondary" (click)="editSequence(selectedSequence()!)">Edit Sequence</button>
            <button class="btn btn-primary" (click)="openEnrollDialog(selectedSequence()!)">Enroll Lead</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .sequences-page {
      max-width: 1400px;
      margin: 0 auto;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 24px;
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

    .btn {
      padding: 10px 20px;
      border-radius: 8px;
      font-weight: 500;
      cursor: pointer;
      border: none;
      transition: all 0.2s ease;
    }

    .btn-primary {
      background: #1a237e;
      color: white;
    }

    .btn-primary:hover {
      background: #0d47a1;
    }

    .btn-secondary {
      background: #f5f5f5;
      color: #333;
      border: 1px solid #ddd;
    }

    .btn-sm {
      padding: 6px 12px;
      font-size: 12px;
    }

    .filters-bar {
      display: flex;
      gap: 16px;
      margin-bottom: 24px;
      flex-wrap: wrap;
    }

    .search-box {
      flex: 1;
      min-width: 250px;
    }

    .search-box input {
      width: 100%;
      padding: 12px 16px;
      border: 1px solid #ddd;
      border-radius: 8px;
      font-size: 14px;
    }

    .filter-group {
      display: flex;
      gap: 12px;
    }

    .filter-group select {
      padding: 12px 16px;
      border: 1px solid #ddd;
      border-radius: 8px;
      background: white;
      font-size: 14px;
      min-width: 150px;
    }

    .table-container {
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      overflow: hidden;
    }

    .sequences-table {
      width: 100%;
      border-collapse: collapse;
    }

    .sequences-table th {
      background: #f8f9fa;
      padding: 16px;
      text-align: left;
      font-weight: 600;
      color: #333;
      font-size: 13px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .sequences-table td {
      padding: 16px;
      border-top: 1px solid #eee;
      font-size: 14px;
    }

    .sequences-table tbody tr {
      cursor: pointer;
      transition: background 0.2s ease;
    }

    .sequences-table tbody tr:hover {
      background: #f8f9fa;
    }

    .name-cell {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .sequence-icon {
      width: 40px;
      height: 40px;
      border-radius: 8px;
      background: #e3f2fd;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
    }

    .sequence-icon.large {
      width: 60px;
      height: 60px;
      font-size: 28px;
    }

    .sequence-name {
      display: flex;
      flex-direction: column;
    }

    .sequence-name .name {
      font-weight: 500;
      color: #333;
    }

    .sequence-name .description {
      font-size: 12px;
      color: #666;
      max-width: 200px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .status-badge {
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
    }

    .status-badge.active { background: #e8f5e9; color: #2e7d32; }
    .status-badge.inactive { background: #fafafa; color: #757575; }
    .status-badge.draft { background: #fff3e0; color: #ef6c00; }
    .status-badge.large { padding: 8px 16px; font-size: 14px; }

    .rate-value {
      font-weight: 600;
      color: #1a237e;
    }

    .actions-cell {
      display: flex;
      gap: 8px;
    }

    .action-btn {
      background: none;
      border: none;
      cursor: pointer;
      padding: 4px;
      font-size: 16px;
      opacity: 0.7;
      transition: opacity 0.2s ease;
    }

    .action-btn:hover {
      opacity: 1;
    }

    .action-btn.delete:hover {
      color: #c62828;
    }

    .empty-state {
      text-align: center;
      padding: 48px !important;
      color: #999;
    }

    .pagination {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 16px;
      margin-top: 24px;
    }

    .pagination button {
      padding: 8px 16px;
      border: 1px solid #ddd;
      background: white;
      border-radius: 6px;
      cursor: pointer;
    }

    .pagination button:disabled {
      opacity: 0.5;
      cursor: not-allowed;
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

    .dialog.large {
      max-width: 700px;
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

    /* Steps Builder */
    .steps-section {
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      padding: 16px;
    }

    .steps-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }

    .steps-header label {
      font-weight: 600;
      color: #333;
    }

    .steps-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .step-item {
      display: flex;
      gap: 12px;
      padding: 12px;
      background: #f8f9fa;
      border-radius: 8px;
      border: 1px solid #e0e0e0;
    }

    .step-number {
      width: 28px;
      height: 28px;
      background: #1a237e;
      color: white;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      font-size: 14px;
      flex-shrink: 0;
    }

    .step-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .step-row {
      display: flex;
      gap: 8px;
      align-items: center;
      flex-wrap: wrap;
    }

    .step-row select {
      min-width: 140px;
    }

    .delay-inputs {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .delay-inputs input {
      width: 60px;
      padding: 6px 8px;
      border: 1px solid #ddd;
      border-radius: 4px;
    }

    .delay-inputs span {
      font-size: 12px;
      color: #666;
    }

    .step-row .full-width {
      width: 100%;
    }

    .remove-step-btn {
      background: none;
      border: none;
      color: #999;
      font-size: 20px;
      cursor: pointer;
      padding: 4px;
    }

    .remove-step-btn:hover {
      color: #c62828;
    }

    .empty-steps {
      text-align: center;
      padding: 24px;
      color: #999;
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
      width: 520px;
      background: white;
      height: 100%;
      overflow-y: auto;
      box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
    }

    .detail-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      padding: 24px;
      border-bottom: 1px solid #eee;
    }

    .sequence-info {
      display: flex;
      gap: 16px;
      align-items: center;
    }

    .sequence-info h3 {
      margin: 0;
      color: #333;
    }

    .sequence-info p {
      margin: 4px 0 0;
      color: #666;
      font-size: 14px;
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
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .status-toggle {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    /* Steps Timeline */
    .steps-timeline {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .timeline-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 12px;
      background: #f8f9fa;
      border-radius: 8px;
    }

    .timeline-icon {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
      flex-shrink: 0;
    }

    .timeline-icon.email { background: #e3f2fd; }
    .timeline-icon.wait { background: #fff3e0; }
    .timeline-icon.task { background: #e8f5e9; }
    .timeline-icon.linkedin { background: #e0f2f1; }

    .timeline-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .timeline-label {
      font-weight: 500;
      color: #333;
    }

    .timeline-subject {
      font-size: 13px;
      color: #666;
    }

    .timeline-delay {
      font-size: 12px;
      color: #999;
    }

    .empty-timeline {
      padding: 16px;
      text-align: center;
      color: #999;
    }

    /* Analytics */
    .analytics-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 12px;
      margin-bottom: 16px;
    }

    .analytics-item {
      text-align: center;
      padding: 12px;
      background: #f8f9fa;
      border-radius: 8px;
    }

    .analytics-value {
      display: block;
      font-size: 20px;
      font-weight: 700;
      color: #1a237e;
    }

    .analytics-label {
      font-size: 11px;
      color: #666;
    }

    .analytics-rates {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .rate-item {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      border-bottom: 1px solid #f0f0f0;
    }

    .rate-label {
      color: #666;
    }

    /* Enrollments */
    .enrollments-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .enrollment-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px;
      background: #f8f9fa;
      border-radius: 8px;
    }

    .enrollment-info {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .enrollment-step {
      font-weight: 500;
    }

    .enrollment-status {
      font-size: 12px;
      padding: 2px 8px;
      border-radius: 4px;
    }

    .enrollment-status.active { background: #e8f5e9; color: #2e7d32; }
    .enrollment-status.paused { background: #fff3e0; color: #ef6c00; }
    .enrollment-status.completed { background: #e3f2fd; color: #1565c0; }

    .empty-enrollments {
      padding: 16px;
      text-align: center;
      color: #999;
    }

    .detail-actions {
      padding: 24px;
      border-top: 1px solid #eee;
      display: flex;
      gap: 12px;
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
      .detail-content {
        width: 100%;
      }

      .analytics-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }
  `],
})
export class SalesSequencesComponent implements OnInit, OnDestroy {
  private readonly sequenceService = inject(SalesSequenceService);
  private readonly salesService = inject(SalesService);
  private destroy$ = new Subject<void>();

  // State
  sequences = signal<EmailSequence[]>([]);
  selectedSequence = signal<EmailSequence | null>(null);
  sequenceAnalytics = signal<SequenceAnalytics | null>(null);
  enrollments = signal<SequenceEnrollment[]>([]);
  leads = signal<Lead[]>([]);
  isLoading = this.sequenceService.isLoading;

  // Dialog state
  showCreateDialog = false;
  showEnrollDialog = false;
  editingSequence: EmailSequence | null = null;
  enrollingSequence: EmailSequence | null = null;
  selectedLeadId = '';
  draggingIndex: number | null = null;

  // Filters
  searchQuery = '';
  statusFilter = '';

  // Pagination
  currentPage = 0;
  totalPages = 1;
  pageSize = 20;

  // Form
  sequenceForm: SequenceCreateRequest = this.getEmptyForm();

  statuses: SequenceStatus[] = ['ACTIVE', 'INACTIVE', 'DRAFT'];

  ngOnInit(): void {
    this.loadSequences();
    this.loadLeads();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadSequences(): void {
    this.sequenceService.getSequences({ page: this.currentPage, size: this.pageSize })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.sequences.set(response.content);
          this.totalPages = response.totalPages;
        },
      });
  }

  loadLeads(): void {
    this.salesService.getLeads({}, { page: 0, size: 100 })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.leads.set(response.content);
        },
      });
  }

  filteredSequences(): EmailSequence[] {
    let filtered = this.sequences();

    if (this.searchQuery) {
      const query = this.searchQuery.toLowerCase();
      filtered = filtered.filter(
        (s) => s.name.toLowerCase().includes(query) ||
               s.description?.toLowerCase().includes(query)
      );
    }

    if (this.statusFilter) {
      filtered = filtered.filter((s) => s.status === this.statusFilter);
    }

    return filtered;
  }

  onSearch(): void {
    // Filtering is done client-side for simplicity
  }

  applyFilters(): void {
    // Filtering is done client-side for simplicity
  }

  changePage(page: number): void {
    this.currentPage = page;
    this.loadSequences();
  }

  selectSequence(sequence: EmailSequence): void {
    this.selectedSequence.set(sequence);
    this.loadSequenceDetails(sequence.id);
  }

  loadSequenceDetails(sequenceId: string): void {
    // Load analytics
    this.sequenceService.getSequenceAnalytics(sequenceId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (analytics) => this.sequenceAnalytics.set(analytics),
        error: () => this.sequenceAnalytics.set(null),
      });

    // Load enrollments
    this.sequenceService.getEnrollments(sequenceId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (enrollments) => this.enrollments.set(enrollments),
        error: () => this.enrollments.set([]),
      });
  }

  openCreateDialog(): void {
    this.editingSequence = null;
    this.sequenceForm = this.getEmptyForm();
    this.showCreateDialog = true;
  }

  editSequence(sequence: EmailSequence): void {
    this.editingSequence = sequence;
    this.sequenceForm = {
      name: sequence.name,
      description: sequence.description,
      steps: sequence.steps.map((step) => ({
        stepNumber: step.stepNumber,
        type: step.type,
        subject: step.subject,
        content: step.content,
        templateId: step.templateId,
        delayDays: step.delayDays,
        delayHours: step.delayHours,
      })),
    };
    this.showCreateDialog = true;
  }

  saveSequence(): void {
    // Renumber steps
    this.sequenceForm.steps = this.sequenceForm.steps.map((step, index) => ({
      ...step,
      stepNumber: index + 1,
    }));

    if (this.editingSequence) {
      this.sequenceService.updateSequence(this.editingSequence.id, this.sequenceForm)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.closeDialog();
            this.loadSequences();
            if (this.selectedSequence()?.id === this.editingSequence?.id) {
              this.sequenceService.getSequence(this.editingSequence!.id)
                .pipe(takeUntil(this.destroy$))
                .subscribe((seq) => this.selectedSequence.set(seq));
            }
          },
        });
    } else {
      this.sequenceService.createSequence(this.sequenceForm)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.closeDialog();
            this.loadSequences();
          },
        });
    }
  }

  deleteSequence(sequence: EmailSequence): void {
    if (confirm(`Delete sequence "${sequence.name}"?`)) {
      this.sequenceService.deleteSequence(sequence.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            if (this.selectedSequence()?.id === sequence.id) {
              this.selectedSequence.set(null);
            }
            this.loadSequences();
          },
        });
    }
  }

  toggleSequenceStatus(sequence: EmailSequence): void {
    const action = sequence.status === 'ACTIVE'
      ? this.sequenceService.deactivateSequence(sequence.id)
      : this.sequenceService.activateSequence(sequence.id);

    action.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => this.loadSequences(),
    });
  }

  addStep(): void {
    this.sequenceForm.steps.push({
      stepNumber: this.sequenceForm.steps.length + 1,
      type: 'EMAIL',
      subject: '',
      content: '',
      delayDays: 0,
      delayHours: 0,
    });
  }

  removeStep(index: number): void {
    this.sequenceForm.steps.splice(index, 1);
  }

  closeDialog(): void {
    this.showCreateDialog = false;
    this.editingSequence = null;
    this.sequenceForm = this.getEmptyForm();
  }

  // Enroll Dialog
  openEnrollDialog(sequence: EmailSequence): void {
    this.enrollingSequence = sequence;
    this.selectedLeadId = '';
    this.showEnrollDialog = true;
  }

  closeEnrollDialog(): void {
    this.showEnrollDialog = false;
    this.enrollingSequence = null;
    this.selectedLeadId = '';
  }

  enrollLead(): void {
    if (!this.enrollingSequence || !this.selectedLeadId) return;

    this.sequenceService.enrollLead(this.enrollingSequence.id, this.selectedLeadId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.closeEnrollDialog();
          if (this.selectedSequence()?.id === this.enrollingSequence?.id) {
            this.loadSequenceDetails(this.enrollingSequence!.id);
          }
          this.loadSequences();
        },
      });
  }

  pauseEnrollment(enrollment: SequenceEnrollment): void {
    this.sequenceService.pauseEnrollment(enrollment.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          if (this.selectedSequence()) {
            this.loadSequenceDetails(this.selectedSequence()!.id);
          }
        },
      });
  }

  resumeEnrollment(enrollment: SequenceEnrollment): void {
    this.sequenceService.resumeEnrollment(enrollment.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          if (this.selectedSequence()) {
            this.loadSequenceDetails(this.selectedSequence()!.id);
          }
        },
      });
  }

  getEmptyForm(): SequenceCreateRequest {
    return {
      name: '',
      description: '',
      steps: [],
    };
  }

  formatStatus(status: string): string {
    return status.replace('_', ' ').toLowerCase()
      .replace(/\b\w/g, (l) => l.toUpperCase());
  }

  formatStepType(type: string): string {
    const types: Record<string, string> = {
      EMAIL: 'Send Email',
      WAIT: 'Wait',
      TASK: 'Create Task',
      LINKEDIN: 'LinkedIn Action',
    };
    return types[type] || type;
  }

  getStepIcon(type: string): string {
    const icons: Record<string, string> = {
      EMAIL: '📧',
      WAIT: '⏰',
      TASK: '✅',
      LINKEDIN: '🔗',
    };
    return icons[type] || '📌';
  }

  formatEnrollmentStatus(status: string): string {
    return status.toLowerCase().replace(/\b\w/g, (l) => l.toUpperCase());
  }
}
