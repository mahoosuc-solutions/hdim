import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Lead } from '../models';
import { CxApiService } from '../services/cx-api.service';

@Component({
  selector: 'cx-add-lead-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="modal-overlay" *ngIf="isOpen" (click)="onOverlayClick($event)">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h2>Add New Lead</h2>
          <button class="close-btn" (click)="close()">&times;</button>
        </div>

        <div class="modal-body">
          <form #leadForm="ngForm">
            <div class="info-section">
              <h3>Contact Information</h3>
              <div class="form-grid">
                <div class="form-field">
                  <label>First Name *</label>
                  <input
                    type="text"
                    [(ngModel)]="newLead.first_name"
                    name="first_name"
                    required
                    class="editable"
                  />
                </div>
                <div class="form-field">
                  <label>Last Name *</label>
                  <input
                    type="text"
                    [(ngModel)]="newLead.last_name"
                    name="last_name"
                    required
                    class="editable"
                  />
                </div>
                <div class="form-field">
                  <label>Email *</label>
                  <input
                    type="email"
                    [(ngModel)]="newLead.email"
                    name="email"
                    required
                    class="editable"
                  />
                </div>
                <div class="form-field">
                  <label>Company *</label>
                  <input
                    type="text"
                    [(ngModel)]="newLead.company"
                    name="company"
                    required
                    class="editable"
                  />
                </div>
                <div class="form-field">
                  <label>Phone</label>
                  <input
                    type="tel"
                    [(ngModel)]="newLead.phone"
                    name="phone"
                    class="editable"
                    placeholder="Optional"
                  />
                </div>
                <div class="form-field">
                  <label>Title</label>
                  <input
                    type="text"
                    [(ngModel)]="newLead.title"
                    name="title"
                    class="editable"
                    placeholder="Job title"
                  />
                </div>
              </div>
            </div>

            <div class="info-section">
              <h3>Deal Information</h3>
              <div class="form-grid">
                <div class="form-field">
                  <label>Tier *</label>
                  <select
                    [(ngModel)]="newLead.tier"
                    name="tier"
                    required
                    class="editable"
                  >
                    <option value="">Select tier...</option>
                    <option value="A">Tier A</option>
                    <option value="B">Tier B</option>
                    <option value="C">Tier C</option>
                    <option value="U">Unqualified</option>
                  </select>
                </div>
                <div class="form-field">
                  <label>Source *</label>
                  <select
                    [(ngModel)]="newLead.source"
                    name="source"
                    required
                    class="editable"
                  >
                    <option value="">Select source...</option>
                    <option value="cold_outreach">Cold Outreach</option>
                    <option value="warm_intro">Warm Intro</option>
                    <option value="inbound">Inbound</option>
                    <option value="conference">Conference</option>
                    <option value="referral">Referral</option>
                    <option value="content">Content</option>
                    <option value="partner">Partner</option>
                    <option value="other">Other</option>
                  </select>
                </div>
                <div class="form-field">
                  <label>Deal Size ($)</label>
                  <input
                    type="number"
                    [(ngModel)]="newLead.deal_size"
                    name="deal_size"
                    class="editable"
                    placeholder="0"
                    min="0"
                  />
                </div>
                <div class="form-field">
                  <label>Close Probability (%)</label>
                  <input
                    type="number"
                    [(ngModel)]="newLead.close_probability"
                    name="close_probability"
                    class="editable"
                    placeholder="0"
                    min="0"
                    max="100"
                  />
                </div>
              </div>
            </div>

            <div class="info-section">
              <h3>Additional Details</h3>
              <div class="form-field full-width">
                <label>LinkedIn URL</label>
                <input
                  type="url"
                  [(ngModel)]="newLead.linkedin_url"
                  name="linkedin_url"
                  class="editable"
                  placeholder="https://linkedin.com/in/..."
                />
              </div>
              <div class="form-grid">
                <div class="form-field">
                  <label>City</label>
                  <input
                    type="text"
                    [(ngModel)]="newLead.city"
                    name="city"
                    class="editable"
                    placeholder="City"
                  />
                </div>
                <div class="form-field">
                  <label>State</label>
                  <input
                    type="text"
                    [(ngModel)]="newLead.state"
                    name="state"
                    class="editable"
                    placeholder="State"
                    maxlength="2"
                  />
                </div>
              </div>
              <div class="form-field full-width">
                <label>Notes</label>
                <textarea
                  [(ngModel)]="newLead.notes"
                  name="notes"
                  class="editable"
                  placeholder="Optional notes..."
                  rows="3"
                ></textarea>
              </div>
            </div>

            <div class="validation-error" *ngIf="errorMessage">
              {{ errorMessage }}
            </div>
          </form>
        </div>

        <div class="modal-footer">
          <button class="btn-secondary" (click)="close()">Cancel</button>
          <button
            class="btn-primary"
            (click)="saveLead()"
            [disabled]="isSaving"
          >
            {{ isSaving ? 'Creating...' : 'Create Lead' }}
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
    .form-field select,
    .form-field textarea {
      padding: 10px 12px;
      border: 1px solid #e0e0e0;
      border-radius: 6px;
      font-size: 14px;
      color: #1a1a2e;
      background: white;
      font-family: inherit;
    }

    .form-field input.editable,
    .form-field select.editable,
    .form-field textarea.editable {
      border-color: #4361ee;
    }

    .form-field input:focus,
    .form-field select:focus,
    .form-field textarea:focus {
      outline: none;
      border-color: #4361ee;
      box-shadow: 0 0 0 3px rgba(67, 97, 238, 0.1);
    }

    .form-field textarea {
      resize: vertical;
      min-height: 60px;
    }

    .validation-error {
      background: #fee;
      color: #c33;
      padding: 12px;
      border-radius: 6px;
      margin-top: 16px;
      font-size: 14px;
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
export class AddLeadModalComponent {
  @Input() isOpen = false;
  @Output() closed = new EventEmitter<void>();
  @Output() created = new EventEmitter<Lead>();

  isSaving = false;
  errorMessage = '';

  newLead: Partial<Lead> = {
    first_name: '',
    last_name: '',
    email: '',
    company: '',
    phone: '',
    title: '',
    tier: 'B',
    source: 'cold_outreach',
    status: 'new',
    deal_size: null,
    close_probability: 0,
    linkedin_url: '',
    city: '',
    state: '',
    notes: '',
    org_type: '',
    patient_count: null,
    contract_types: [],
    score: 50,
    tags: [],
  };

  constructor(private cxApi: CxApiService) {}

  saveLead(): void {
    // Basic validation
    if (!this.newLead.first_name || !this.newLead.last_name ||
        !this.newLead.email || !this.newLead.company) {
      this.errorMessage = 'Please fill in all required fields (marked with *)';
      return;
    }

    // Email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.newLead.email || '')) {
      this.errorMessage = 'Please enter a valid email address';
      return;
    }

    this.errorMessage = '';
    this.isSaving = true;

    this.cxApi.createLead(this.newLead).subscribe({
      next: (createdLead) => {
        this.isSaving = false;
        this.created.emit(createdLead);
        this.resetForm();
      },
      error: (err) => {
        console.error('Failed to create lead', err);
        this.errorMessage = err.error?.detail || 'Failed to create lead. Please try again.';
        this.isSaving = false;
      },
    });
  }

  close(): void {
    this.resetForm();
    this.closed.emit();
  }

  resetForm(): void {
    this.newLead = {
      first_name: '',
      last_name: '',
      email: '',
      company: '',
      phone: '',
      title: '',
      tier: 'B',
      source: 'cold_outreach',
      status: 'new',
      deal_size: null,
      close_probability: 0,
      linkedin_url: '',
      city: '',
      state: '',
      notes: '',
      org_type: '',
      patient_count: null,
      contract_types: [],
      score: 50,
      tags: [],
    };
    this.errorMessage = '';
  }

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-overlay')) {
      this.close();
    }
  }
}
