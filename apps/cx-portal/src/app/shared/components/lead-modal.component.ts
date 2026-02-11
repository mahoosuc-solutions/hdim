import { Component, EventEmitter, Input, Output, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Lead } from '../models';
import { CxApiService } from '../services/cx-api.service';

@Component({
  selector: 'cx-lead-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="modal-overlay" *ngIf="isOpen" (click)="onOverlayClick($event)">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h2>{{ isEditMode ? 'Edit Lead' : 'Lead Details' }}</h2>
          <button class="close-btn" (click)="close()">&times;</button>
        </div>

        <div class="modal-body">
          <div class="lead-info" *ngIf="lead">
            <div class="info-section">
              <h3>Contact Information</h3>
              <div class="form-grid">
                <div class="form-field">
                  <label>First Name</label>
                  <input
                    type="text"
                    [(ngModel)]="lead.first_name"
                    [readonly]="!isEditMode"
                    [class.editable]="isEditMode"
                  />
                </div>
                <div class="form-field">
                  <label>Last Name</label>
                  <input
                    type="text"
                    [(ngModel)]="lead.last_name"
                    [readonly]="!isEditMode"
                    [class.editable]="isEditMode"
                  />
                </div>
                <div class="form-field">
                  <label>Email</label>
                  <input
                    type="email"
                    [(ngModel)]="lead.email"
                    [readonly]="!isEditMode"
                    [class.editable]="isEditMode"
                  />
                </div>
                <div class="form-field">
                  <label>Company</label>
                  <input
                    type="text"
                    [(ngModel)]="lead.company"
                    [readonly]="!isEditMode"
                    [class.editable]="isEditMode"
                  />
                </div>
                <div class="form-field">
                  <label>Phone</label>
                  <input
                    type="tel"
                    [(ngModel)]="lead.phone"
                    [readonly]="!isEditMode"
                    [class.editable]="isEditMode"
                    placeholder="Not provided"
                  />
                </div>
              </div>
            </div>

            <div class="info-section">
              <h3>Deal Information</h3>
              <div class="form-grid">
                <div class="form-field">
                  <label>Status</label>
                  <select
                    [(ngModel)]="lead.status"
                    [disabled]="!isEditMode"
                    [class.editable]="isEditMode"
                  >
                    <option value="new">New</option>
                    <option value="contacted">Contacted</option>
                    <option value="engaged">Engaged</option>
                    <option value="qualified">Qualified</option>
                    <option value="demo_scheduled">Demo Scheduled</option>
                    <option value="demo_completed">Demo Completed</option>
                    <option value="proposal_sent">Proposal Sent</option>
                    <option value="negotiation">Negotiation</option>
                    <option value="closed_won">Closed Won</option>
                    <option value="closed_lost">Closed Lost</option>
                  </select>
                </div>
                <div class="form-field">
                  <label>Tier</label>
                  <select
                    [(ngModel)]="lead.tier"
                    [disabled]="!isEditMode"
                    [class.editable]="isEditMode"
                  >
                    <option value="A">Tier A</option>
                    <option value="B">Tier B</option>
                    <option value="C">Tier C</option>
                  </select>
                </div>
                <div class="form-field">
                  <label>Deal Size</label>
                  <input
                    type="number"
                    [(ngModel)]="lead.deal_size"
                    [readonly]="!isEditMode"
                    [class.editable]="isEditMode"
                    placeholder="0"
                  />
                </div>
                <div class="form-field">
                  <label>Close Probability (%)</label>
                  <input
                    type="number"
                    [(ngModel)]="lead.close_probability"
                    [readonly]="!isEditMode"
                    [class.editable]="isEditMode"
                    min="0"
                    max="100"
                    placeholder="0"
                  />
                </div>
              </div>
            </div>

            <div class="info-section">
              <h3>Additional Details</h3>
              <div class="form-field full-width">
                <label>Source</label>
                <input
                  type="text"
                  [(ngModel)]="lead.source"
                  [readonly]="!isEditMode"
                  [class.editable]="isEditMode"
                  placeholder="Not specified"
                />
              </div>
              <div class="form-field full-width">
                <label>Tags</label>
                <input
                  type="text"
                  [value]="lead.tags?.join(', ') || ''"
                  [readonly]="!isEditMode"
                  [class.editable]="isEditMode"
                  placeholder="No tags"
                />
              </div>
            </div>

            <div class="info-section" *ngIf="!isEditMode">
              <h3>Activity History</h3>
              <div class="activity-list" *ngIf="activities.length > 0">
                <div class="activity-item" *ngFor="let activity of activities">
                  <div class="activity-icon">📧</div>
                  <div class="activity-details">
                    <div class="activity-subject">{{ activity.subject }}</div>
                    <div class="activity-meta">
                      {{ activity.activity_type }} • {{ activity.created_at | date:'short' }}
                    </div>
                  </div>
                </div>
              </div>
              <div class="empty-state" *ngIf="activities.length === 0">
                No activity recorded yet
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-secondary" (click)="close()">
            {{ isEditMode ? 'Cancel' : 'Close' }}
          </button>
          <button
            class="btn-primary"
            *ngIf="!isEditMode"
            (click)="toggleEditMode()"
          >
            Edit Lead
          </button>
          <button
            class="btn-primary"
            *ngIf="isEditMode"
            (click)="saveLead()"
            [disabled]="isSaving"
          >
            {{ isSaving ? 'Saving...' : 'Save Changes' }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .modal-overlay {
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
      padding: 20px;
    }

    .modal-content {
      background: white;
      border-radius: 12px;
      max-width: 800px;
      width: 100%;
      max-height: 90vh;
      display: flex;
      flex-direction: column;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 24px;
      border-bottom: 1px solid #e0e0e0;
    }

    .modal-header h2 {
      margin: 0;
      color: #1a1a2e;
      font-size: 24px;
    }

    .close-btn {
      background: none;
      border: none;
      font-size: 32px;
      color: #999;
      cursor: pointer;
      padding: 0;
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      line-height: 1;
    }

    .close-btn:hover {
      color: #333;
    }

    .modal-body {
      padding: 24px;
      overflow-y: auto;
      flex: 1;
    }

    .info-section {
      margin-bottom: 32px;
    }

    .info-section:last-child {
      margin-bottom: 0;
    }

    .info-section h3 {
      margin: 0 0 16px 0;
      font-size: 16px;
      color: #666;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      font-weight: 600;
    }

    .form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    .form-field {
      display: flex;
      flex-direction: column;
    }

    .form-field.full-width {
      grid-column: 1 / -1;
    }

    .form-field label {
      font-size: 13px;
      color: #666;
      margin-bottom: 6px;
      font-weight: 500;
    }

    .form-field input,
    .form-field select {
      padding: 10px 12px;
      border: 1px solid #e0e0e0;
      border-radius: 6px;
      font-size: 14px;
      color: #1a1a2e;
      background: #f5f7fa;
    }

    .form-field input.editable,
    .form-field select.editable {
      background: white;
      border-color: #4361ee;
    }

    .form-field input:focus,
    .form-field select:focus {
      outline: none;
      border-color: #4361ee;
      background: white;
    }

    .form-field input[readonly] {
      cursor: default;
    }

    .activity-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .activity-item {
      display: flex;
      gap: 12px;
      padding: 12px;
      background: #f5f7fa;
      border-radius: 8px;
    }

    .activity-icon {
      font-size: 20px;
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: white;
      border-radius: 6px;
      flex-shrink: 0;
    }

    .activity-details {
      flex: 1;
    }

    .activity-subject {
      font-weight: 600;
      color: #1a1a2e;
      margin-bottom: 4px;
    }

    .activity-meta {
      font-size: 12px;
      color: #999;
    }

    .empty-state {
      text-align: center;
      padding: 32px;
      color: #999;
    }

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      padding: 24px;
      border-top: 1px solid #e0e0e0;
    }

    .btn-secondary,
    .btn-primary {
      padding: 10px 20px;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: background 0.2s;
    }

    .btn-secondary {
      background: #f5f7fa;
      color: #666;
    }

    .btn-secondary:hover {
      background: #e0e0e0;
    }

    .btn-primary {
      background: #4361ee;
      color: white;
    }

    .btn-primary:hover {
      background: #3651d4;
    }

    .btn-primary:disabled {
      background: #ccc;
      cursor: not-allowed;
    }
  `],
})
export class LeadModalComponent implements OnInit {
  @Input() lead: Lead | null = null;
  @Input() isOpen = false;
  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<Lead>();

  isEditMode = false;
  isSaving = false;
  activities: any[] = [];
  originalLead: Lead | null = null;

  constructor(private cxApi: CxApiService) {}

  ngOnInit(): void {
    if (this.lead) {
      this.loadActivities();
    }
  }

  loadActivities(): void {
    if (!this.lead) return;

    // TODO: Load activities for this lead from API
    // this.cxApi.getActivitiesForLead(this.lead.id).subscribe({
    //   next: (activities) => this.activities = activities,
    //   error: (err) => console.error('Failed to load activities', err)
    // });
  }

  toggleEditMode(): void {
    this.isEditMode = true;
    this.originalLead = { ...this.lead! };
  }

  saveLead(): void {
    if (!this.lead) return;

    this.isSaving = true;
    this.cxApi.updateLead(this.lead.id, this.lead).subscribe({
      next: (updatedLead) => {
        this.isSaving = false;
        this.isEditMode = false;
        this.saved.emit(updatedLead);
      },
      error: (err) => {
        console.error('Failed to save lead', err);
        alert('Failed to save changes. Please try again.');
        this.isSaving = false;
      },
    });
  }

  close(): void {
    if (this.isEditMode && this.originalLead) {
      // Restore original values if editing was cancelled
      Object.assign(this.lead!, this.originalLead);
    }
    this.isEditMode = false;
    this.closed.emit();
  }

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-overlay')) {
      this.close();
    }
  }
}
