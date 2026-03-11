import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TenantManagementService, TenantResponse } from '../../services/tenant-management.service';
import { UserResponse } from '../../services/user-management.service';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-admin-tenant-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="admin-page">
      <h1>Tenant Settings</h1>

      @if (loading) {
        <div class="loading">Loading tenants...</div>
      }

      @if (errorMessage) {
        <div class="error-message" data-test-id="error-message">{{ errorMessage }}</div>
      }

      @if (successMessage) {
        <div class="success-message" data-test-id="success-message">{{ successMessage }}</div>
      }

      @if (tenants.length > 0) {
        <table class="tenant-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Status</th>
              <th>Users</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            @for (tenant of tenants; track tenant.id) {
              <tr data-test-id="tenant-row">
                <td>{{ tenant.name }}</td>
                <td>
                  <span [class]="tenant.status === 'ACTIVE' ? 'status-active' : 'status-inactive'">
                    {{ tenant.status }}
                  </span>
                </td>
                <td>{{ tenant.userCount }}</td>
                <td>{{ tenant.createdAt | date:'short' }}</td>
                <td class="action-cell">
                  <button type="button" data-test-id="edit-tenant-button"
                          (click)="editTenant(tenant)" aria-label="Edit tenant">Edit</button>
                  <button type="button" data-test-id="view-users-button"
                          (click)="viewTenantUsers(tenant)" aria-label="View tenant users">Users</button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      }

      @if (showEditForm && editingTenant) {
        <div class="edit-panel">
          <h3>Edit Tenant: {{ editingTenant.name }}</h3>
          <form [formGroup]="editForm" (ngSubmit)="saveEdit()">
            <label>
              Tenant Name
              <input data-test-id="tenant-name" formControlName="name" />
            </label>
            <div class="form-actions">
              <button type="submit" data-test-id="save-tenant-button"
                      [disabled]="editForm.invalid">Save</button>
              <button type="button" (click)="cancelEdit()">Cancel</button>
            </div>
          </form>
        </div>
      }

      @if (showUsersPanel && editingTenant) {
        <div class="edit-panel">
          <h3>Users in: {{ editingTenant.name }}</h3>
          @if (tenantUsers.length === 0) {
            <p>No users in this tenant.</p>
          } @else {
            <table class="user-table">
              <thead>
                <tr>
                  <th>Username</th>
                  <th>Email</th>
                  <th>Roles</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                @for (user of tenantUsers; track user.id) {
                  <tr data-test-id="tenant-user-row">
                    <td>{{ user.username }}</td>
                    <td>{{ user.email }}</td>
                    <td>
                      @for (role of user.roles; track role) {
                        <span class="role-chip">{{ role }}</span>
                      }
                    </td>
                    <td>{{ user.active ? 'Active' : 'Inactive' }}</td>
                  </tr>
                }
              </tbody>
            </table>
          }
          <div class="form-actions">
            <button type="button" (click)="showUsersPanel = false">Close</button>
          </div>
        </div>
      }
    </section>
  `,
  styles: [`
    .admin-page { padding: 24px; }
    .loading { padding: 16px; color: #666; }
    .error-message { padding: 12px; background: #fdecea; color: #b71c1c; border-radius: 4px; margin-bottom: 16px; }
    .success-message { padding: 12px; background: #e8f5e9; color: #1b5e20; border-radius: 4px; margin-bottom: 16px; }
    .tenant-table, .user-table { width: 100%; border-collapse: collapse; font-size: 14px; margin-bottom: 16px; }
    .tenant-table th, .tenant-table td, .user-table th, .user-table td { border-bottom: 1px solid #e0e0e0; padding: 8px; text-align: left; }
    .tenant-table th, .user-table th { font-weight: 600; background: #fafafa; }
    .status-active { color: #2e7d32; font-weight: 500; }
    .status-inactive { color: #c62828; font-weight: 500; }
    .role-chip { display: inline-block; padding: 2px 8px; background: #e3f2fd; border-radius: 12px; font-size: 12px; margin: 1px 2px; }
    .edit-panel { padding: 16px; background: #f5f5f5; border-radius: 4px; margin-bottom: 16px; max-width: 480px; }
    .edit-panel form { display: grid; gap: 12px; }
    .edit-panel label { display: grid; gap: 4px; font-size: 14px; }
    .edit-panel input { padding: 8px; border: 1px solid #ccc; border-radius: 4px; }
    .form-actions { display: flex; gap: 8px; margin-top: 8px; }
    .action-cell { white-space: nowrap; }
    .action-cell button { margin-right: 4px; font-size: 12px; padding: 4px 8px; cursor: pointer; }
  `],
})
export class AdminTenantSettingsComponent implements OnInit {
  tenants: TenantResponse[] = [];
  tenantUsers: UserResponse[] = [];
  loading = false;
  errorMessage = '';
  successMessage = '';

  showEditForm = false;
  showUsersPanel = false;
  editingTenant: TenantResponse | null = null;
  editForm: FormGroup;

  private logger;

  constructor(
    private fb: FormBuilder,
    private tenantService: TenantManagementService,
    private loggerService: LoggerService,
  ) {
    this.logger = this.loggerService.withContext('AdminTenantSettingsComponent');
    this.editForm = this.fb.group({
      name: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    this.loadTenants();
  }

  loadTenants(): void {
    this.loading = true;
    this.errorMessage = '';
    this.tenantService.getTenants().subscribe({
      next: (tenants) => {
        this.tenants = tenants;
        this.loading = false;
        this.logger.info('Loaded tenants', { count: tenants.length });
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Failed to load tenants';
        this.logger.error('Failed to load tenants');
      },
    });
  }

  editTenant(tenant: TenantResponse): void {
    this.editingTenant = tenant;
    this.showEditForm = true;
    this.showUsersPanel = false;
    this.editForm.patchValue({ name: tenant.name });
  }

  cancelEdit(): void {
    this.showEditForm = false;
    this.editingTenant = null;
  }

  saveEdit(): void {
    if (!this.editingTenant || this.editForm.invalid) return;
    this.tenantService.updateTenant(this.editingTenant.id, this.editForm.value).subscribe({
      next: () => {
        this.successMessage = 'Tenant updated successfully';
        this.showEditForm = false;
        this.loadTenants();
      },
      error: (err: { error?: { message?: string } }) => {
        this.errorMessage = err.error?.message || 'Failed to update tenant';
      },
    });
  }

  viewTenantUsers(tenant: TenantResponse): void {
    this.editingTenant = tenant;
    this.showUsersPanel = true;
    this.showEditForm = false;
    this.tenantService.getTenantUsers(tenant.id).subscribe({
      next: (users) => {
        this.tenantUsers = users;
        this.logger.info('Loaded tenant users', { tenantId: tenant.id, count: users.length });
      },
      error: (err: { error?: { message?: string } }) => {
        this.errorMessage = err.error?.message || 'Failed to load tenant users';
      },
    });
  }
}
