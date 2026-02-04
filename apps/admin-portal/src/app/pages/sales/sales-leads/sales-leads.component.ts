import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { SalesService } from '../../../services/sales.service';
import { Lead, LeadStatus, LeadSource, LeadFilter, LeadCreateRequest } from '../../../models/sales.model';

@Component({
  selector: 'app-sales-leads',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="leads-page">
      <div class="page-header">
        <div class="header-content">
          <h2>Leads</h2>
          <p class="subtitle">Manage and qualify your leads</p>
        </div>
        <div class="header-actions">
          <button class="btn btn-primary" (click)="showCreateDialog = true">
            + New Lead
          </button>
        </div>
      </div>

      <!-- Filters -->
      <div class="filters-bar">
        <div class="search-box">
          <input
            type="text"
            [(ngModel)]="searchQuery"
            placeholder="Search leads..."
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
          <select [(ngModel)]="sourceFilter" (change)="applyFilters()">
            <option value="">All Sources</option>
            <option *ngFor="let source of sources" [value]="source">
              {{ formatSource(source) }}
            </option>
          </select>
        </div>
      </div>

      <!-- Loading State -->
      <div class="loading" *ngIf="isLoading()">
        <div class="spinner"></div>
        <span>Loading leads...</span>
      </div>

      <!-- Leads Table -->
      <div class="table-container" *ngIf="!isLoading()">
        <table class="leads-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Company</th>
              <th>Email</th>
              <th>Source</th>
              <th>Score</th>
              <th>Status</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let lead of leads()" (click)="selectLead(lead)">
              <td class="name-cell">
                <div class="lead-avatar">{{ getInitials(lead.firstName, lead.lastName) }}</div>
                <div class="lead-name">
                  <span class="name">{{ lead.firstName }} {{ lead.lastName }}</span>
                  <span class="title" *ngIf="lead.title">{{ lead.title }}</span>
                </div>
              </td>
              <td>{{ lead.company }}</td>
              <td>{{ lead.email }}</td>
              <td>
                <span class="source-badge" [class]="lead.source.toLowerCase()">
                  {{ formatSource(lead.source) }}
                </span>
              </td>
              <td>
                <div class="score-cell">
                  <div class="score-bar">
                    <div class="score-fill" [style.width.%]="lead.score" [class.high]="lead.score >= 70" [class.medium]="lead.score >= 40 && lead.score < 70"></div>
                  </div>
                  <span class="score-value">{{ lead.score }}</span>
                </div>
              </td>
              <td>
                <span class="status-badge" [class]="lead.status.toLowerCase()">
                  {{ formatStatus(lead.status) }}
                </span>
              </td>
              <td>{{ lead.createdAt | date:'shortDate' }}</td>
              <td class="actions-cell" (click)="$event.stopPropagation()">
                <button class="action-btn" title="Edit" (click)="editLead(lead)">✏️</button>
                <button class="action-btn" title="Convert" (click)="convertLead(lead)" *ngIf="lead.status === 'QUALIFIED'">🔄</button>
                <button class="action-btn delete" title="Delete" (click)="deleteLead(lead)">🗑️</button>
              </td>
            </tr>
            <tr *ngIf="!leads().length">
              <td colspan="8" class="empty-state">
                No leads found. Create your first lead to get started.
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
        <div class="dialog" (click)="$event.stopPropagation()">
          <div class="dialog-header">
            <h3>{{ editingLead ? 'Edit Lead' : 'Create New Lead' }}</h3>
            <button class="close-btn" (click)="closeDialog()">×</button>
          </div>
          <form (ngSubmit)="saveLead()" class="dialog-form">
            <div class="form-row">
              <div class="form-group">
                <label>First Name *</label>
                <input type="text" [(ngModel)]="leadForm.firstName" name="firstName" required />
              </div>
              <div class="form-group">
                <label>Last Name *</label>
                <input type="text" [(ngModel)]="leadForm.lastName" name="lastName" required />
              </div>
            </div>
            <div class="form-group">
              <label>Email *</label>
              <input type="email" [(ngModel)]="leadForm.email" name="email" required />
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>Company *</label>
                <input type="text" [(ngModel)]="leadForm.company" name="company" required />
              </div>
              <div class="form-group">
                <label>Title</label>
                <input type="text" [(ngModel)]="leadForm.title" name="title" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>Phone</label>
                <input type="tel" [(ngModel)]="leadForm.phone" name="phone" />
              </div>
              <div class="form-group">
                <label>Source *</label>
                <select [(ngModel)]="leadForm.source" name="source" required>
                  <option *ngFor="let source of sources" [value]="source">
                    {{ formatSource(source) }}
                  </option>
                </select>
              </div>
            </div>
            <div class="form-group">
              <label>LinkedIn URL</label>
              <input type="url" [(ngModel)]="leadForm.linkedInUrl" name="linkedInUrl" />
            </div>
            <div class="form-group">
              <label>Notes</label>
              <textarea [(ngModel)]="leadForm.notes" name="notes" rows="3"></textarea>
            </div>
            <div class="dialog-actions">
              <button type="button" class="btn btn-secondary" (click)="closeDialog()">Cancel</button>
              <button type="submit" class="btn btn-primary">{{ editingLead ? 'Save Changes' : 'Create Lead' }}</button>
            </div>
          </form>
        </div>
      </div>

      <!-- Lead Detail Panel -->
      <div class="detail-panel" *ngIf="selectedLead()" (click)="selectedLead.set(null)">
        <div class="detail-content" (click)="$event.stopPropagation()">
          <div class="detail-header">
            <div class="lead-info">
              <div class="lead-avatar large">{{ getInitials(selectedLead()!.firstName, selectedLead()!.lastName) }}</div>
              <div>
                <h3>{{ selectedLead()!.firstName }} {{ selectedLead()!.lastName }}</h3>
                <p>{{ selectedLead()!.title }} at {{ selectedLead()!.company }}</p>
              </div>
            </div>
            <button class="close-btn" (click)="selectedLead.set(null)">×</button>
          </div>

          <div class="detail-body">
            <div class="detail-section">
              <h4>Contact Information</h4>
              <div class="detail-row">
                <span class="label">Email:</span>
                <a [href]="'mailto:' + selectedLead()!.email">{{ selectedLead()!.email }}</a>
              </div>
              <div class="detail-row" *ngIf="selectedLead()!.phone">
                <span class="label">Phone:</span>
                <span>{{ selectedLead()!.phone }}</span>
              </div>
              <div class="detail-row" *ngIf="selectedLead()!.linkedInUrl">
                <span class="label">LinkedIn:</span>
                <a [href]="selectedLead()!.linkedInUrl" target="_blank">View Profile</a>
              </div>
            </div>

            <div class="detail-section">
              <h4>Lead Details</h4>
              <div class="detail-row">
                <span class="label">Source:</span>
                <span class="source-badge" [class]="selectedLead()!.source.toLowerCase()">
                  {{ formatSource(selectedLead()!.source) }}
                </span>
              </div>
              <div class="detail-row">
                <span class="label">Score:</span>
                <span class="score-value large">{{ selectedLead()!.score }}</span>
              </div>
              <div class="detail-row">
                <span class="label">Status:</span>
                <select [(ngModel)]="selectedLead()!.status" (change)="updateLeadStatus(selectedLead()!)">
                  <option *ngFor="let status of statuses" [value]="status">
                    {{ formatStatus(status) }}
                  </option>
                </select>
              </div>
            </div>

            <div class="detail-section" *ngIf="selectedLead()!.notes">
              <h4>Notes</h4>
              <p class="notes-text">{{ selectedLead()!.notes }}</p>
            </div>
          </div>

          <div class="detail-actions">
            <button class="btn btn-secondary" (click)="editLead(selectedLead()!)">Edit Lead</button>
            <button class="btn btn-primary" (click)="convertLead(selectedLead()!)" *ngIf="selectedLead()!.status === 'QUALIFIED'">
              Convert to Opportunity
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .leads-page {
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

    .leads-table {
      width: 100%;
      border-collapse: collapse;
    }

    .leads-table th {
      background: #f8f9fa;
      padding: 16px;
      text-align: left;
      font-weight: 600;
      color: #333;
      font-size: 13px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .leads-table td {
      padding: 16px;
      border-top: 1px solid #eee;
      font-size: 14px;
    }

    .leads-table tbody tr {
      cursor: pointer;
      transition: background 0.2s ease;
    }

    .leads-table tbody tr:hover {
      background: #f8f9fa;
    }

    .name-cell {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .lead-avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: #1a237e;
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      font-size: 14px;
    }

    .lead-avatar.large {
      width: 60px;
      height: 60px;
      font-size: 20px;
    }

    .lead-name {
      display: flex;
      flex-direction: column;
    }

    .lead-name .name {
      font-weight: 500;
      color: #333;
    }

    .lead-name .title {
      font-size: 12px;
      color: #666;
    }

    .score-cell {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .score-bar {
      width: 60px;
      height: 6px;
      background: #e0e0e0;
      border-radius: 3px;
      overflow: hidden;
    }

    .score-fill {
      height: 100%;
      background: #ff9800;
      border-radius: 3px;
    }

    .score-fill.high {
      background: #4caf50;
    }

    .score-fill.medium {
      background: #ff9800;
    }

    .score-value {
      font-weight: 600;
      color: #333;
    }

    .score-value.large {
      font-size: 24px;
      color: #1a237e;
    }

    .status-badge {
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
    }

    .status-badge.open { background: #e3f2fd; color: #1565c0; }
    .status-badge.qualified { background: #e8f5e9; color: #2e7d32; }
    .status-badge.unqualified { background: #fafafa; color: #757575; }
    .status-badge.converted { background: #f3e5f5; color: #7b1fa2; }
    .status-badge.lost { background: #ffebee; color: #c62828; }

    .source-badge {
      padding: 4px 10px;
      border-radius: 12px;
      font-size: 11px;
      font-weight: 500;
      background: #f5f5f5;
      color: #666;
    }

    .source-badge.website { background: #e3f2fd; color: #1565c0; }
    .source-badge.referral { background: #e8f5e9; color: #2e7d32; }
    .source-badge.event { background: #fff3e0; color: #ef6c00; }
    .source-badge.demo_request { background: #fce4ec; color: #c2185b; }

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
      max-width: 600px;
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
      width: 480px;
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

    .lead-info {
      display: flex;
      gap: 16px;
      align-items: center;
    }

    .lead-info h3 {
      margin: 0;
      color: #333;
    }

    .lead-info p {
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

    .detail-row {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      border-bottom: 1px solid #f5f5f5;
    }

    .detail-row .label {
      color: #666;
    }

    .detail-row a {
      color: #1a237e;
      text-decoration: none;
    }

    .detail-row a:hover {
      text-decoration: underline;
    }

    .notes-text {
      color: #333;
      line-height: 1.6;
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
      .form-row {
        grid-template-columns: 1fr;
      }

      .detail-content {
        width: 100%;
      }
    }
  `],
})
export class SalesLeadsComponent implements OnInit, OnDestroy {
  private readonly salesService = inject(SalesService);
  private destroy$ = new Subject<void>();

  leads = signal<Lead[]>([]);
  selectedLead = signal<Lead | null>(null);
  isLoading = signal(false);

  showCreateDialog = false;
  editingLead: Lead | null = null;

  // Filters
  searchQuery = '';
  statusFilter = '';
  sourceFilter = '';

  // Pagination
  currentPage = 0;
  totalPages = 1;
  pageSize = 20;

  // Form
  leadForm: LeadCreateRequest = this.getEmptyForm();

  statuses: LeadStatus[] = ['OPEN', 'QUALIFIED', 'UNQUALIFIED', 'CONVERTED', 'LOST'];
  sources: LeadSource[] = ['WEBSITE', 'REFERRAL', 'EVENT', 'DEMO_REQUEST', 'ROI_CALCULATOR', 'PHONE', 'PARTNER', 'EMAIL', 'OTHER'];

  ngOnInit(): void {
    this.loadLeads();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadLeads(): void {
    this.isLoading.set(true);

    const filter: LeadFilter = {};
    if (this.statusFilter) {
      filter.status = [this.statusFilter as LeadStatus];
    }
    if (this.sourceFilter) {
      filter.source = [this.sourceFilter as LeadSource];
    }

    this.salesService.getLeads(filter, { page: this.currentPage, size: this.pageSize })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.leads.set(response.content);
          this.totalPages = response.totalPages;
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
        },
      });
  }

  onSearch(): void {
    this.currentPage = 0;
    this.loadLeads();
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadLeads();
  }

  changePage(page: number): void {
    this.currentPage = page;
    this.loadLeads();
  }

  selectLead(lead: Lead): void {
    this.selectedLead.set(lead);
  }

  editLead(lead: Lead): void {
    this.editingLead = lead;
    this.leadForm = {
      firstName: lead.firstName,
      lastName: lead.lastName,
      email: lead.email,
      phone: lead.phone,
      company: lead.company,
      title: lead.title,
      source: lead.source,
      linkedInUrl: lead.linkedInUrl,
      notes: lead.notes,
    };
    this.showCreateDialog = true;
  }

  saveLead(): void {
    if (this.editingLead) {
      this.salesService.updateLead(this.editingLead.id, this.leadForm)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.closeDialog();
            this.loadLeads();
          },
        });
    } else {
      this.salesService.createLead(this.leadForm)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.closeDialog();
            this.loadLeads();
          },
        });
    }
  }

  deleteLead(lead: Lead): void {
    if (confirm(`Delete ${lead.firstName} ${lead.lastName}?`)) {
      this.salesService.deleteLead(lead.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => this.loadLeads(),
        });
    }
  }

  convertLead(lead: Lead): void {
    if (confirm(`Convert ${lead.firstName} ${lead.lastName} to an opportunity?`)) {
      this.salesService.convertLead(lead.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.selectedLead.set(null);
            this.loadLeads();
          },
        });
    }
  }

  updateLeadStatus(lead: Lead): void {
    this.salesService.updateLead(lead.id, { source: lead.source })
      .pipe(takeUntil(this.destroy$))
      .subscribe();
  }

  closeDialog(): void {
    this.showCreateDialog = false;
    this.editingLead = null;
    this.leadForm = this.getEmptyForm();
  }

  getEmptyForm(): LeadCreateRequest {
    return {
      firstName: '',
      lastName: '',
      email: '',
      company: '',
      source: 'WEBSITE',
    };
  }

  getInitials(firstName: string, lastName: string): string {
    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
  }

  formatStatus(status: string): string {
    return status.replace('_', ' ').toLowerCase()
      .replace(/\b\w/g, (l) => l.toUpperCase());
  }

  formatSource(source: string): string {
    return source.replace('_', ' ').toLowerCase()
      .replace(/\b\w/g, (l) => l.toUpperCase());
  }
}
