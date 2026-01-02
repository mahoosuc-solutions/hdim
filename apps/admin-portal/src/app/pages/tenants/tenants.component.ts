import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../services/admin.service';
import { Tenant, CreateTenantRequest } from '../../models/admin.model';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-tenants',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="tenants-page">
      <div class="page-header">
        <div class="header-left">
          <h2>Tenant Management</h2>
          <span class="tenant-count">{{ tenants.length }} tenants</span>
        </div>
        <button class="btn-primary" (click)="openCreateModal()">
          <span class="icon">+</span> Add Tenant
        </button>
      </div>

      <!-- Stats Cards -->
      <div class="stats-grid">
        <div class="stat-card">
          <span class="stat-value">{{ tenants.length }}</span>
          <span class="stat-label">Total Tenants</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ getActiveTenants() }}</span>
          <span class="stat-label">Active</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ getTotalUsers() }}</span>
          <span class="stat-label">Total Users</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ getTotalPatients() }}</span>
          <span class="stat-label">Total Patients</span>
        </div>
      </div>

      <!-- Tenant Cards -->
      <div class="tenants-grid" *ngIf="!loading">
        <div
          *ngFor="let tenant of tenants"
          class="tenant-card"
          [class.active]="tenant.status === 'ACTIVE'"
          [class.inactive]="tenant.status !== 'ACTIVE'"
        >
          <div class="tenant-header">
            <div class="tenant-icon">{{ tenant.name.charAt(0) }}</div>
            <div class="tenant-info">
              <h3>{{ tenant.name }}</h3>
              <span class="tenant-id">{{ tenant.id }}</span>
            </div>
            <span class="status-badge" [class.active]="tenant.status === 'ACTIVE'">
              {{ tenant.status }}
            </span>
          </div>

          <div class="tenant-stats">
            <div class="stat">
              <span class="stat-num">{{ tenant.userCount }}</span>
              <span class="stat-txt">Users</span>
            </div>
            <div class="stat">
              <span class="stat-num">{{ tenant.patientCount | number }}</span>
              <span class="stat-txt">Patients</span>
            </div>
            <div class="stat">
              <span class="stat-num">{{ tenant.subscription }}</span>
              <span class="stat-txt">Plan</span>
            </div>
          </div>

          <div class="tenant-details">
            <div class="detail-row">
              <span class="label">Contact:</span>
              <span class="value">{{ tenant.contactEmail }}</span>
            </div>
            <div class="detail-row">
              <span class="label">Domain:</span>
              <span class="value">{{ tenant.domain || 'N/A' }}</span>
            </div>
            <div class="detail-row">
              <span class="label">Created:</span>
              <span class="value">{{ tenant.createdAt | date:'mediumDate' }}</span>
            </div>
          </div>

          <div class="feature-flags" *ngIf="tenant.featureFlags">
            <span class="flag" *ngIf="tenant.featureFlags.advancedAnalytics">Analytics</span>
            <span class="flag" *ngIf="tenant.featureFlags.aiAssistant">AI</span>
            <span class="flag" *ngIf="tenant.featureFlags.predictiveModels">Predictive</span>
            <span class="flag" *ngIf="tenant.featureFlags.customReports">Reports</span>
          </div>

          <div class="tenant-actions">
            <button class="btn-icon" title="Edit" (click)="editTenant(tenant)">
              <span>✏️</span>
            </button>
            <button class="btn-icon" title="Settings" (click)="openSettings(tenant)">
              <span>⚙️</span>
            </button>
            <button
              class="btn-icon"
              [title]="tenant.status === 'ACTIVE' ? 'Deactivate' : 'Activate'"
              (click)="toggleTenantStatus(tenant)"
            >
              <span>{{ tenant.status === 'ACTIVE' ? '🔒' : '🔓' }}</span>
            </button>
            <button class="btn-icon danger" title="Delete" (click)="confirmDelete(tenant)">
              <span>🗑️</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Loading State -->
      <div class="loading" *ngIf="loading">
        <div class="spinner"></div>
        <span>Loading tenants...</span>
      </div>

      <!-- Empty State -->
      <div class="empty-state" *ngIf="!loading && tenants.length === 0">
        <span class="empty-icon">🏢</span>
        <h3>No Tenants</h3>
        <p>Get started by creating your first tenant organization.</p>
        <button class="btn-primary" (click)="openCreateModal()">Create Tenant</button>
      </div>

      <!-- Create/Edit Modal -->
      <div class="modal-overlay" *ngIf="showModal" (click)="closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h3>{{ editingTenant ? 'Edit Tenant' : 'Create Tenant' }}</h3>
            <button class="close-btn" (click)="closeModal()">×</button>
          </div>

          <div class="modal-body">
            <div class="form-group">
              <label>Tenant Name *</label>
              <input
                type="text"
                [(ngModel)]="formData.name"
                placeholder="Organization Name"
                class="form-input"
              />
            </div>

            <div class="form-group">
              <label>Contact Email *</label>
              <input
                type="email"
                [(ngModel)]="formData.contactEmail"
                placeholder="contact@organization.com"
                class="form-input"
              />
            </div>

            <div class="form-group">
              <label>Domain</label>
              <input
                type="text"
                [(ngModel)]="formData.domain"
                placeholder="organization.com"
                class="form-input"
              />
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>Subscription Plan</label>
                <select [(ngModel)]="formData.subscription" class="form-select">
                  <option value="FREE">Free</option>
                  <option value="BASIC">Basic</option>
                  <option value="PROFESSIONAL">Professional</option>
                  <option value="ENTERPRISE">Enterprise</option>
                </select>
              </div>

              <div class="form-group">
                <label>Max Users</label>
                <input
                  type="number"
                  [(ngModel)]="formData.maxUsers"
                  placeholder="10"
                  class="form-input"
                />
              </div>
            </div>

            <div class="form-group">
              <label>Feature Flags</label>
              <div class="checkbox-group">
                <label class="checkbox-label">
                  <input type="checkbox" [(ngModel)]="formData.featureFlags.advancedAnalytics" />
                  Advanced Analytics
                </label>
                <label class="checkbox-label">
                  <input type="checkbox" [(ngModel)]="formData.featureFlags.aiAssistant" />
                  AI Assistant
                </label>
                <label class="checkbox-label">
                  <input type="checkbox" [(ngModel)]="formData.featureFlags.predictiveModels" />
                  Predictive Models
                </label>
                <label class="checkbox-label">
                  <input type="checkbox" [(ngModel)]="formData.featureFlags.customReports" />
                  Custom Reports
                </label>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn-secondary" (click)="closeModal()">Cancel</button>
            <button class="btn-primary" (click)="saveTenant()" [disabled]="!isFormValid()">
              {{ editingTenant ? 'Update' : 'Create' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Delete Confirmation Modal -->
      <div class="modal-overlay" *ngIf="showDeleteModal" (click)="showDeleteModal = false">
        <div class="modal delete-modal" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h3>Confirm Delete</h3>
            <button class="close-btn" (click)="showDeleteModal = false">×</button>
          </div>
          <div class="modal-body">
            <p>Are you sure you want to delete <strong>{{ tenantToDelete?.name }}</strong>?</p>
            <p class="warning">This action cannot be undone. All users and data associated with this tenant will be permanently deleted.</p>
          </div>
          <div class="modal-footer">
            <button class="btn-secondary" (click)="showDeleteModal = false">Cancel</button>
            <button class="btn-danger" (click)="deleteTenant()">Delete Tenant</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .tenants-page {
      max-width: 1400px;
      margin: 0 auto;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }

    .header-left h2 {
      margin: 0;
      color: #1a237e;
    }

    .tenant-count {
      color: #666;
      font-size: 14px;
    }

    .btn-primary {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 20px;
      background: #1a237e;
      color: white;
      border: none;
      border-radius: 8px;
      cursor: pointer;
      font-weight: 500;
      transition: background 0.2s;
    }

    .btn-primary:hover {
      background: #0d47a1;
    }

    .btn-primary:disabled {
      background: #ccc;
      cursor: not-allowed;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;
      margin-bottom: 24px;
    }

    .stat-card {
      background: white;
      padding: 20px;
      border-radius: 12px;
      text-align: center;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .stat-value {
      display: block;
      font-size: 28px;
      font-weight: 700;
      color: #1a237e;
    }

    .stat-label {
      font-size: 14px;
      color: #666;
    }

    .tenants-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
      gap: 20px;
    }

    .tenant-card {
      background: white;
      border-radius: 12px;
      padding: 20px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      border-left: 4px solid #4caf50;
    }

    .tenant-card.inactive {
      border-left-color: #ff9800;
      opacity: 0.8;
    }

    .tenant-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;
    }

    .tenant-icon {
      width: 48px;
      height: 48px;
      background: linear-gradient(135deg, #1a237e 0%, #0d47a1 100%);
      color: white;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      font-weight: 700;
    }

    .tenant-info {
      flex: 1;
    }

    .tenant-info h3 {
      margin: 0;
      font-size: 16px;
      color: #333;
    }

    .tenant-id {
      font-size: 12px;
      color: #999;
      font-family: monospace;
    }

    .status-badge {
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 600;
      background: #fff3e0;
      color: #e65100;
    }

    .status-badge.active {
      background: #e8f5e9;
      color: #2e7d32;
    }

    .tenant-stats {
      display: flex;
      gap: 16px;
      padding: 12px 0;
      border-top: 1px solid #eee;
      border-bottom: 1px solid #eee;
      margin-bottom: 12px;
    }

    .tenant-stats .stat {
      flex: 1;
      text-align: center;
    }

    .stat-num {
      display: block;
      font-size: 18px;
      font-weight: 700;
      color: #1a237e;
    }

    .stat-txt {
      font-size: 12px;
      color: #666;
    }

    .tenant-details {
      margin-bottom: 12px;
    }

    .detail-row {
      display: flex;
      font-size: 13px;
      margin-bottom: 4px;
    }

    .detail-row .label {
      color: #666;
      width: 70px;
    }

    .detail-row .value {
      color: #333;
    }

    .feature-flags {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-bottom: 12px;
    }

    .flag {
      padding: 2px 8px;
      background: #e3f2fd;
      color: #1565c0;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 500;
    }

    .tenant-actions {
      display: flex;
      gap: 8px;
      padding-top: 12px;
      border-top: 1px solid #eee;
    }

    .btn-icon {
      padding: 8px;
      background: #f5f5f5;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      transition: background 0.2s;
    }

    .btn-icon:hover {
      background: #e0e0e0;
    }

    .btn-icon.danger:hover {
      background: #ffebee;
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

    .empty-state {
      text-align: center;
      padding: 48px;
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .empty-icon {
      font-size: 48px;
    }

    .empty-state h3 {
      margin: 16px 0 8px;
      color: #333;
    }

    .empty-state p {
      color: #666;
      margin-bottom: 16px;
    }

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
    }

    .modal {
      background: white;
      border-radius: 12px;
      width: 500px;
      max-width: 90%;
      max-height: 90vh;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }

    .delete-modal {
      width: 400px;
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px;
      border-bottom: 1px solid #eee;
    }

    .modal-header h3 {
      margin: 0;
      color: #333;
    }

    .close-btn {
      background: none;
      border: none;
      font-size: 24px;
      cursor: pointer;
      color: #666;
    }

    .modal-body {
      padding: 20px;
      overflow-y: auto;
    }

    .modal-body .warning {
      color: #c62828;
      font-size: 13px;
      background: #ffebee;
      padding: 12px;
      border-radius: 6px;
      margin-top: 12px;
    }

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      padding: 20px;
      border-top: 1px solid #eee;
    }

    .btn-secondary {
      padding: 10px 20px;
      background: #f5f5f5;
      border: none;
      border-radius: 8px;
      cursor: pointer;
      font-weight: 500;
    }

    .btn-secondary:hover {
      background: #e0e0e0;
    }

    .btn-danger {
      padding: 10px 20px;
      background: #f44336;
      color: white;
      border: none;
      border-radius: 8px;
      cursor: pointer;
      font-weight: 500;
    }

    .btn-danger:hover {
      background: #d32f2f;
    }

    .form-group {
      margin-bottom: 16px;
    }

    .form-group label {
      display: block;
      margin-bottom: 6px;
      font-weight: 500;
      color: #333;
      font-size: 14px;
    }

    .form-input,
    .form-select {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid #ddd;
      border-radius: 6px;
      font-size: 14px;
      box-sizing: border-box;
    }

    .form-input:focus,
    .form-select:focus {
      outline: none;
      border-color: #1a237e;
    }

    .form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    .checkbox-group {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
    }

    .checkbox-label {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: normal;
      font-size: 13px;
      cursor: pointer;
    }

    .checkbox-label input {
      width: auto;
    }
  `],
})
export class TenantsComponent implements OnInit, OnDestroy {
  tenants: Tenant[] = [];
  loading = true;
  showModal = false;
  showDeleteModal = false;
  editingTenant: Tenant | null = null;
  tenantToDelete: Tenant | null = null;
  private destroy$ = new Subject<void>();

  formData: CreateTenantRequest = this.getEmptyFormData();

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadTenants();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadTenants(): void {
    this.loading = true;
    this.adminService
      .getTenants()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.tenants = response.content;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        },
      });
  }

  getActiveTenants(): number {
    return this.tenants.filter((t) => t.status === 'ACTIVE').length;
  }

  getTotalUsers(): number {
    return this.tenants.reduce((sum, t) => sum + t.userCount, 0);
  }

  getTotalPatients(): number {
    return this.tenants.reduce((sum, t) => sum + t.patientCount, 0);
  }

  openCreateModal(): void {
    this.editingTenant = null;
    this.formData = this.getEmptyFormData();
    this.showModal = true;
  }

  editTenant(tenant: Tenant): void {
    this.editingTenant = tenant;
    this.formData = {
      name: tenant.name,
      contactEmail: tenant.contactEmail,
      domain: tenant.domain,
      subscription: tenant.subscription,
      maxUsers: tenant.maxUsers,
      featureFlags: { ...tenant.featureFlags },
    };
    this.showModal = true;
  }

  openSettings(tenant: Tenant): void {
    // Navigate to tenant settings page
    console.log('Open settings for tenant:', tenant.id);
  }

  toggleTenantStatus(tenant: Tenant): void {
    const newStatus = tenant.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    this.adminService
      .updateTenant(tenant.id, { ...tenant, status: newStatus })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          const index = this.tenants.findIndex((t) => t.id === tenant.id);
          if (index !== -1) {
            this.tenants[index] = updated;
          }
        },
      });
  }

  confirmDelete(tenant: Tenant): void {
    this.tenantToDelete = tenant;
    this.showDeleteModal = true;
  }

  deleteTenant(): void {
    if (!this.tenantToDelete) return;

    this.adminService
      .deleteTenant(this.tenantToDelete.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.tenants = this.tenants.filter((t) => t.id !== this.tenantToDelete?.id);
          this.showDeleteModal = false;
          this.tenantToDelete = null;
        },
      });
  }

  closeModal(): void {
    this.showModal = false;
    this.editingTenant = null;
    this.formData = this.getEmptyFormData();
  }

  saveTenant(): void {
    if (!this.isFormValid()) return;

    if (this.editingTenant) {
      this.adminService
        .updateTenant(this.editingTenant.id, {
          ...this.editingTenant,
          ...this.formData,
        })
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (updated) => {
            const index = this.tenants.findIndex((t) => t.id === this.editingTenant?.id);
            if (index !== -1) {
              this.tenants[index] = updated;
            }
            this.closeModal();
          },
        });
    } else {
      this.adminService
        .createTenant(this.formData)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (created) => {
            this.tenants.unshift(created);
            this.closeModal();
          },
        });
    }
  }

  isFormValid(): boolean {
    return !!(this.formData.name && this.formData.contactEmail);
  }

  private getEmptyFormData(): CreateTenantRequest {
    return {
      name: '',
      contactEmail: '',
      domain: '',
      subscription: 'PROFESSIONAL',
      maxUsers: 10,
      featureFlags: {
        advancedAnalytics: false,
        aiAssistant: false,
        predictiveModels: false,
        customReports: false,
      },
    };
  }
}
