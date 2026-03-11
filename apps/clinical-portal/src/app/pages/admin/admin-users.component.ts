import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserManagementService, UserResponse, TempPasswordResponse } from '../../services/user-management.service';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="admin-page">
      <h1>User Management</h1>

      <div class="actions">
        <button type="button" data-test-id="refresh-users-button" (click)="loadUsers()">Refresh</button>
      </div>

      @if (loading) {
        <div class="loading">Loading users...</div>
      }

      @if (errorMessage) {
        <div class="error-message" data-test-id="error-message">{{ errorMessage }}</div>
      }

      @if (successMessage) {
        <div class="success-message" data-test-id="success-message">{{ successMessage }}</div>
      }

      @if (tempPassword) {
        <div class="temp-password-banner" data-test-id="temp-password-banner">
          <strong>Temporary Password:</strong> {{ tempPassword }}
          <br><small>This will only be shown once. Copy it now.</small>
          <button type="button" (click)="tempPassword = ''">Dismiss</button>
        </div>
      }

      @if (showEditForm && editingUser) {
        <div class="edit-panel">
          <h3>Edit User: {{ editingUser.username }}</h3>
          <form [formGroup]="editForm" (ngSubmit)="saveEdit()">
            <label>
              First Name
              <input data-test-id="edit-first-name" formControlName="firstName" />
            </label>
            <label>
              Last Name
              <input data-test-id="edit-last-name" formControlName="lastName" />
            </label>
            <label>
              Email
              <input data-test-id="edit-email" formControlName="email" type="email" />
            </label>
            <label>
              Notes
              <textarea data-test-id="edit-notes" formControlName="notes" rows="3"></textarea>
            </label>
            <div class="form-actions">
              <button type="submit" data-test-id="save-edit-button" [disabled]="editForm.invalid">Save</button>
              <button type="button" (click)="cancelEdit()">Cancel</button>
            </div>
          </form>
        </div>
      }

      @if (showRoleForm && editingUser) {
        <div class="edit-panel">
          <h3>Manage Roles: {{ editingUser.username }}</h3>
          <div class="role-checkboxes">
            @for (role of availableRoles; track role) {
              <label class="role-label">
                <input type="checkbox" [checked]="selectedRoles.has(role)"
                       (change)="toggleRole(role)" [attr.data-test-id]="'role-' + role" />
                {{ role }}
              </label>
            }
          </div>
          <div class="form-actions">
            <button type="button" data-test-id="save-roles-button" (click)="saveRoles()">Save Roles</button>
            <button type="button" (click)="showRoleForm = false">Cancel</button>
          </div>
        </div>
      }

      <table class="user-table">
        <thead>
          <tr>
            <th>Username</th>
            <th>Email</th>
            <th>Name</th>
            <th>Roles</th>
            <th>Status</th>
            <th>Last Login</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          @for (user of users; track user.id) {
            <tr data-test-id="user-row" [class.inactive]="!user.active">
              <td>{{ user.username }}</td>
              <td>{{ user.email }}</td>
              <td>{{ user.firstName }} {{ user.lastName }}</td>
              <td>
                @for (role of user.roles; track role) {
                  <span class="role-chip">{{ role }}</span>
                }
              </td>
              <td>
                <span [class]="user.active ? 'status-active' : 'status-inactive'">
                  {{ user.active ? 'Active' : 'Inactive' }}
                </span>
                @if (user.accountLockedUntil) {
                  <span class="status-locked">Locked</span>
                }
              </td>
              <td>{{ user.lastLoginAt ? (user.lastLoginAt | date:'short') : 'Never' }}</td>
              <td class="action-cell">
                <button type="button" data-test-id="edit-user-button" (click)="editUser(user)"
                        aria-label="Edit user">Edit</button>
                <button type="button" data-test-id="roles-button" (click)="manageRoles(user)"
                        aria-label="Manage roles">Roles</button>
                @if (user.active) {
                  <button type="button" data-test-id="deactivate-button" (click)="deactivateUser(user)"
                          aria-label="Deactivate user">Deactivate</button>
                } @else {
                  <button type="button" data-test-id="reactivate-button" (click)="reactivateUser(user)"
                          aria-label="Reactivate user">Reactivate</button>
                }
                <button type="button" data-test-id="reset-password-button" (click)="resetPassword(user)"
                        aria-label="Reset password">Reset PW</button>
                @if (user.accountLockedUntil) {
                  <button type="button" data-test-id="unlock-button" (click)="unlockAccount(user)"
                          aria-label="Unlock account">Unlock</button>
                }
              </td>
            </tr>
          }
        </tbody>
      </table>
    </section>
  `,
  styles: [`
    .admin-page { padding: 24px; }
    .actions { margin-bottom: 16px; }
    .loading { padding: 16px; color: #666; }
    .error-message { padding: 12px; background: #fdecea; color: #b71c1c; border-radius: 4px; margin-bottom: 16px; }
    .success-message { padding: 12px; background: #e8f5e9; color: #1b5e20; border-radius: 4px; margin-bottom: 16px; }
    .temp-password-banner { padding: 16px; background: #fff3e0; border: 2px solid #ff9800; border-radius: 4px; margin-bottom: 16px; }
    .edit-panel { padding: 16px; background: #f5f5f5; border-radius: 4px; margin-bottom: 16px; max-width: 480px; }
    .edit-panel form { display: grid; gap: 12px; }
    .edit-panel label { display: grid; gap: 4px; font-size: 14px; }
    .edit-panel input, .edit-panel textarea { padding: 8px; border: 1px solid #ccc; border-radius: 4px; }
    .form-actions { display: flex; gap: 8px; }
    .role-checkboxes { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 12px; }
    .role-label { display: flex; align-items: center; gap: 4px; font-size: 13px; }
    .user-table { width: 100%; border-collapse: collapse; font-size: 14px; }
    .user-table th, .user-table td { border-bottom: 1px solid #e0e0e0; padding: 8px; text-align: left; }
    .user-table th { font-weight: 600; background: #fafafa; }
    tr.inactive { opacity: 0.6; }
    .role-chip { display: inline-block; padding: 2px 8px; background: #e3f2fd; border-radius: 12px; font-size: 12px; margin: 1px 2px; }
    .status-active { color: #2e7d32; font-weight: 500; }
    .status-inactive { color: #c62828; font-weight: 500; }
    .status-locked { color: #e65100; font-size: 12px; margin-left: 4px; }
    .action-cell { white-space: nowrap; }
    .action-cell button { margin-right: 4px; font-size: 12px; padding: 4px 8px; cursor: pointer; }
  `],
})
export class AdminUsersComponent implements OnInit {
  users: UserResponse[] = [];
  loading = false;
  errorMessage = '';
  successMessage = '';
  tempPassword = '';

  showEditForm = false;
  showRoleForm = false;
  editingUser: UserResponse | null = null;
  editForm: FormGroup;
  selectedRoles = new Set<string>();

  availableRoles = [
    'SUPER_ADMIN', 'ADMIN', 'EVALUATOR', 'ANALYST', 'VIEWER',
    'CARE_MANAGER', 'PROVIDER', 'QUALITY_OFFICER', 'DATA_STEWARD',
    'COMPLIANCE_OFFICER', 'MEASURE_DEVELOPER', 'AUDITOR', 'API_CONSUMER',
  ];

  private logger;

  constructor(
    private fb: FormBuilder,
    private userService: UserManagementService,
    private loggerService: LoggerService,
  ) {
    this.logger = this.loggerService.withContext('AdminUsersComponent');
    this.editForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      notes: [''],
    });
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.errorMessage = '';
    this.userService.getUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.loading = false;
        this.logger.info('Loaded users', { count: users.length });
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Failed to load users';
        this.logger.error('Failed to load users');
      },
    });
  }

  editUser(user: UserResponse): void {
    this.editingUser = user;
    this.showEditForm = true;
    this.showRoleForm = false;
    this.editForm.patchValue({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      notes: user.notes || '',
    });
  }

  cancelEdit(): void {
    this.showEditForm = false;
    this.editingUser = null;
  }

  saveEdit(): void {
    if (!this.editingUser || this.editForm.invalid) return;
    this.userService.updateUser(this.editingUser.id, this.editForm.value).subscribe({
      next: () => {
        this.successMessage = 'User updated successfully';
        this.showEditForm = false;
        this.loadUsers();
      },
      error: (err: { error?: { message?: string } }) => {
        this.errorMessage = err.error?.message || 'Failed to update user';
      },
    });
  }

  manageRoles(user: UserResponse): void {
    this.editingUser = user;
    this.showRoleForm = true;
    this.showEditForm = false;
    this.selectedRoles = new Set(user.roles);
  }

  toggleRole(role: string): void {
    if (this.selectedRoles.has(role)) {
      this.selectedRoles.delete(role);
    } else {
      this.selectedRoles.add(role);
    }
  }

  saveRoles(): void {
    if (!this.editingUser) return;
    this.userService.updateRoles(this.editingUser.id, Array.from(this.selectedRoles)).subscribe({
      next: () => {
        this.successMessage = 'Roles updated successfully';
        this.showRoleForm = false;
        this.loadUsers();
      },
      error: (err: { error?: { message?: string } }) => {
        this.errorMessage = err.error?.message || 'Failed to update roles';
      },
    });
  }

  deactivateUser(user: UserResponse): void {
    if (!confirm(`Deactivate user ${user.username}?`)) return;
    this.userService.deactivateUser(user.id).subscribe({
      next: () => {
        this.successMessage = `User ${user.username} deactivated`;
        this.loadUsers();
      },
      error: (err: { error?: { message?: string } }) => {
        this.errorMessage = err.error?.message || 'Failed to deactivate user';
      },
    });
  }

  reactivateUser(user: UserResponse): void {
    this.userService.reactivateUser(user.id).subscribe({
      next: () => {
        this.successMessage = `User ${user.username} reactivated`;
        this.loadUsers();
      },
      error: (err: { error?: { message?: string } }) => {
        this.errorMessage = err.error?.message || 'Failed to reactivate user';
      },
    });
  }

  resetPassword(user: UserResponse): void {
    if (!confirm(`Reset password for ${user.username}? A temporary password will be generated.`)) return;
    this.userService.resetPassword(user.id).subscribe({
      next: (response: TempPasswordResponse) => {
        this.tempPassword = response.temporaryPassword;
        this.successMessage = `Password reset for ${user.username}. See temp password above.`;
      },
      error: (err: { error?: { message?: string } }) => {
        this.errorMessage = err.error?.message || 'Failed to reset password';
      },
    });
  }

  unlockAccount(user: UserResponse): void {
    this.userService.unlockAccount(user.id).subscribe({
      next: () => {
        this.successMessage = `Account ${user.username} unlocked`;
        this.loadUsers();
      },
      error: (err: { error?: { message?: string } }) => {
        this.errorMessage = err.error?.message || 'Failed to unlock account';
      },
    });
  }
}
