import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../services/admin.service';
import { User, UserRole, CreateUserRequest, PagedResponse } from '../../models/admin.model';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="users-page">
      <div class="page-header">
        <div class="header-left">
          <h2>User Management</h2>
          <span class="user-count" *ngIf="usersResponse">
            {{ usersResponse.totalElements }} users
          </span>
        </div>
        <button class="btn-primary" (click)="openCreateModal()">
          <span class="btn-icon">+</span>
          Add User
        </button>
      </div>

      <!-- Search and Filters -->
      <div class="filters-bar">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input
            type="text"
            placeholder="Search users..."
            [(ngModel)]="searchQuery"
            (input)="filterUsers()"
          />
        </div>
        <div class="filter-group">
          <select [(ngModel)]="roleFilter" (change)="filterUsers()">
            <option value="">All Roles</option>
            <option *ngFor="let role of roles" [value]="role">{{ role }}</option>
          </select>
          <select [(ngModel)]="statusFilter" (change)="filterUsers()">
            <option value="">All Status</option>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
          </select>
        </div>
      </div>

      <!-- Users Table -->
      <div class="table-container">
        <table class="users-table">
          <thead>
            <tr>
              <th>User</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th>Last Login</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let user of filteredUsers" [class.inactive]="!user.active">
              <td class="user-cell">
                <div class="user-avatar">{{ getInitials(user) }}</div>
                <div class="user-info">
                  <span class="user-name">{{ user.firstName }} {{ user.lastName }}</span>
                  <span class="user-username">&#64;{{ user.username }}</span>
                </div>
              </td>
              <td>{{ user.email }}</td>
              <td>
                <span class="role-badge" [class]="user.role.toLowerCase()">
                  {{ user.role }}
                </span>
              </td>
              <td>
                <span class="status-badge" [class.active]="user.active" [class.inactive]="!user.active">
                  {{ user.active ? 'Active' : 'Inactive' }}
                </span>
              </td>
              <td>{{ user.lastLogin | date:'short' }}</td>
              <td class="actions-cell">
                <button class="action-btn edit" (click)="editUser(user)" title="Edit">✏️</button>
                <button class="action-btn toggle" (click)="toggleUserStatus(user)" [title]="user.active ? 'Deactivate' : 'Activate'">
                  {{ user.active ? '🔒' : '🔓' }}
                </button>
                <button class="action-btn delete" (click)="confirmDelete(user)" title="Delete">🗑️</button>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="empty-state" *ngIf="filteredUsers.length === 0 && !loading">
          <span class="empty-icon">👥</span>
          <span>No users found</span>
        </div>

        <div class="loading-state" *ngIf="loading">
          <div class="spinner"></div>
          <span>Loading users...</span>
        </div>
      </div>

      <!-- Pagination -->
      <div class="pagination" *ngIf="usersResponse && usersResponse.totalPages > 1">
        <button
          class="page-btn"
          [disabled]="currentPage === 0"
          (click)="goToPage(currentPage - 1)"
        >
          Previous
        </button>
        <span class="page-info">
          Page {{ currentPage + 1 }} of {{ usersResponse.totalPages }}
        </span>
        <button
          class="page-btn"
          [disabled]="currentPage >= usersResponse.totalPages - 1"
          (click)="goToPage(currentPage + 1)"
        >
          Next
        </button>
      </div>

      <!-- Create/Edit Modal -->
      <div class="modal-overlay" *ngIf="showModal" (click)="closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h3>{{ editingUser ? 'Edit User' : 'Create User' }}</h3>
            <button class="close-btn" (click)="closeModal()">×</button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label>First Name</label>
              <input type="text" [(ngModel)]="userForm.firstName" placeholder="Enter first name" />
            </div>
            <div class="form-group">
              <label>Last Name</label>
              <input type="text" [(ngModel)]="userForm.lastName" placeholder="Enter last name" />
            </div>
            <div class="form-group">
              <label>Email</label>
              <input type="email" [(ngModel)]="userForm.email" placeholder="Enter email" [disabled]="!!editingUser" />
            </div>
            <div class="form-group" *ngIf="!editingUser">
              <label>Username</label>
              <input type="text" [(ngModel)]="userForm.username" placeholder="Enter username" />
            </div>
            <div class="form-group" *ngIf="!editingUser">
              <label>Password</label>
              <input type="password" [(ngModel)]="userForm.password" placeholder="Enter password" />
            </div>
            <div class="form-group">
              <label>Role</label>
              <select [(ngModel)]="userForm.role">
                <option *ngFor="let role of roles" [value]="role">{{ role }}</option>
              </select>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-secondary" (click)="closeModal()">Cancel</button>
            <button class="btn-primary" (click)="saveUser()" [disabled]="!isFormValid()">
              {{ editingUser ? 'Update' : 'Create' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Delete Confirmation Modal -->
      <div class="modal-overlay" *ngIf="showDeleteConfirm" (click)="closeDeleteConfirm()">
        <div class="modal confirm-modal" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h3>Confirm Delete</h3>
          </div>
          <div class="modal-body">
            <p>Are you sure you want to delete user <strong>{{ userToDelete?.firstName }} {{ userToDelete?.lastName }}</strong>?</p>
            <p class="warning-text">This action cannot be undone.</p>
          </div>
          <div class="modal-footer">
            <button class="btn-secondary" (click)="closeDeleteConfirm()">Cancel</button>
            <button class="btn-danger" (click)="deleteUser()">Delete</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .users-page {
      max-width: 1400px;
      margin: 0 auto;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }

    .header-left {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .page-header h2 {
      margin: 0;
      color: #1a237e;
    }

    .user-count {
      background: #e3f2fd;
      color: #1565c0;
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 14px;
      font-weight: 500;
    }

    .btn-primary {
      display: flex;
      align-items: center;
      gap: 8px;
      background: #1a237e;
      color: white;
      border: none;
      padding: 10px 20px;
      border-radius: 8px;
      font-weight: 500;
      cursor: pointer;
      transition: background 0.2s;
    }

    .btn-primary:hover {
      background: #0d1757;
    }

    .btn-icon {
      font-size: 18px;
    }

    .filters-bar {
      display: flex;
      gap: 16px;
      margin-bottom: 20px;
    }

    .search-box {
      flex: 1;
      display: flex;
      align-items: center;
      background: white;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      padding: 0 12px;
    }

    .search-icon {
      font-size: 16px;
      margin-right: 8px;
    }

    .search-box input {
      flex: 1;
      border: none;
      padding: 10px 0;
      font-size: 14px;
      outline: none;
    }

    .filter-group {
      display: flex;
      gap: 12px;
    }

    .filter-group select {
      padding: 10px 16px;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      background: white;
      font-size: 14px;
      cursor: pointer;
    }

    .table-container {
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      overflow: hidden;
    }

    .users-table {
      width: 100%;
      border-collapse: collapse;
    }

    .users-table th {
      text-align: left;
      padding: 16px;
      background: #f5f7fa;
      color: #666;
      font-weight: 600;
      font-size: 13px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .users-table td {
      padding: 16px;
      border-bottom: 1px solid #f0f0f0;
    }

    .users-table tr:hover {
      background: #f8f9fa;
    }

    .users-table tr.inactive {
      opacity: 0.6;
    }

    .user-cell {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .user-avatar {
      width: 40px;
      height: 40px;
      background: linear-gradient(135deg, #1a237e, #3949ab);
      color: white;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      font-size: 14px;
    }

    .user-info {
      display: flex;
      flex-direction: column;
    }

    .user-name {
      font-weight: 600;
      color: #333;
    }

    .user-username {
      font-size: 13px;
      color: #666;
    }

    .role-badge {
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 500;
      background: #e0e0e0;
      color: #666;
    }

    .role-badge.admin { background: #e8eaf6; color: #3f51b5; }
    .role-badge.super_admin { background: #fce4ec; color: #c2185b; }
    .role-badge.evaluator { background: #e8f5e9; color: #388e3c; }
    .role-badge.analyst { background: #fff3e0; color: #f57c00; }
    .role-badge.viewer { background: #f5f5f5; color: #757575; }

    .status-badge {
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 500;
    }

    .status-badge.active {
      background: #c8e6c9;
      color: #2e7d32;
    }

    .status-badge.inactive {
      background: #ffcdd2;
      color: #c62828;
    }

    .actions-cell {
      display: flex;
      gap: 8px;
    }

    .action-btn {
      width: 32px;
      height: 32px;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: background 0.2s;
    }

    .action-btn.edit {
      background: #e3f2fd;
    }

    .action-btn.edit:hover {
      background: #bbdefb;
    }

    .action-btn.toggle {
      background: #fff3e0;
    }

    .action-btn.toggle:hover {
      background: #ffe0b2;
    }

    .action-btn.delete {
      background: #ffebee;
    }

    .action-btn.delete:hover {
      background: #ffcdd2;
    }

    .empty-state, .loading-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px;
      color: #666;
    }

    .empty-icon {
      font-size: 48px;
      margin-bottom: 12px;
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

    .pagination {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 16px;
      margin-top: 24px;
    }

    .page-btn {
      padding: 8px 16px;
      border: 1px solid #e0e0e0;
      border-radius: 6px;
      background: white;
      cursor: pointer;
    }

    .page-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .page-info {
      color: #666;
    }

    /* Modal Styles */
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
      width: 100%;
      max-width: 480px;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px 24px;
      border-bottom: 1px solid #e0e0e0;
    }

    .modal-header h3 {
      margin: 0;
      color: #1a237e;
    }

    .close-btn {
      width: 32px;
      height: 32px;
      border: none;
      background: none;
      font-size: 24px;
      cursor: pointer;
      color: #666;
    }

    .modal-body {
      padding: 24px;
    }

    .form-group {
      margin-bottom: 20px;
    }

    .form-group label {
      display: block;
      margin-bottom: 8px;
      font-weight: 500;
      color: #333;
    }

    .form-group input,
    .form-group select {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid #e0e0e0;
      border-radius: 6px;
      font-size: 14px;
      box-sizing: border-box;
    }

    .form-group input:focus,
    .form-group select:focus {
      border-color: #1a237e;
      outline: none;
    }

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      padding: 16px 24px;
      border-top: 1px solid #e0e0e0;
    }

    .btn-secondary {
      padding: 10px 20px;
      border: 1px solid #e0e0e0;
      border-radius: 6px;
      background: white;
      cursor: pointer;
    }

    .btn-danger {
      padding: 10px 20px;
      border: none;
      border-radius: 6px;
      background: #f44336;
      color: white;
      cursor: pointer;
    }

    .confirm-modal .modal-body p {
      margin: 0 0 12px 0;
    }

    .warning-text {
      color: #f44336;
      font-size: 14px;
    }
  `],
})
export class UsersComponent implements OnInit, OnDestroy {
  usersResponse: PagedResponse<User> | null = null;
  filteredUsers: User[] = [];
  loading = true;
  currentPage = 0;

  searchQuery = '';
  roleFilter = '';
  statusFilter = '';

  showModal = false;
  editingUser: User | null = null;
  userForm: Partial<CreateUserRequest> = {};

  showDeleteConfirm = false;
  userToDelete: User | null = null;

  roles: UserRole[] = ['SUPER_ADMIN', 'ADMIN', 'EVALUATOR', 'ANALYST', 'VIEWER'];

  private destroy$ = new Subject<void>();

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadUsers(): void {
    this.loading = true;
    this.adminService.getUsers(this.currentPage)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.usersResponse = response;
          this.filteredUsers = response.content;
          this.filterUsers();
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        },
      });
  }

  filterUsers(): void {
    if (!this.usersResponse) return;

    this.filteredUsers = this.usersResponse.content.filter((user) => {
      const matchesSearch =
        !this.searchQuery ||
        user.firstName.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        user.lastName.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        user.email.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        user.username.toLowerCase().includes(this.searchQuery.toLowerCase());

      const matchesRole = !this.roleFilter || user.role === this.roleFilter;

      const matchesStatus =
        !this.statusFilter ||
        (this.statusFilter === 'active' && user.active) ||
        (this.statusFilter === 'inactive' && !user.active);

      return matchesSearch && matchesRole && matchesStatus;
    });
  }

  getInitials(user: User): string {
    return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
  }

  goToPage(page: number): void {
    this.currentPage = page;
    this.loadUsers();
  }

  openCreateModal(): void {
    this.editingUser = null;
    this.userForm = {
      firstName: '',
      lastName: '',
      email: '',
      username: '',
      password: '',
      role: 'VIEWER',
      tenantId: 'TENANT001',
    };
    this.showModal = true;
  }

  editUser(user: User): void {
    this.editingUser = user;
    this.userForm = {
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      role: user.role,
    };
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.editingUser = null;
    this.userForm = {};
  }

  isFormValid(): boolean {
    if (this.editingUser) {
      return !!(this.userForm.firstName && this.userForm.lastName);
    }
    return !!(
      this.userForm.firstName &&
      this.userForm.lastName &&
      this.userForm.email &&
      this.userForm.username &&
      this.userForm.password
    );
  }

  saveUser(): void {
    if (!this.isFormValid()) return;

    if (this.editingUser) {
      this.adminService
        .updateUser(this.editingUser.id, {
          firstName: this.userForm.firstName,
          lastName: this.userForm.lastName,
          role: this.userForm.role,
        })
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.closeModal();
            this.loadUsers();
          },
        });
    } else {
      this.adminService
        .createUser(this.userForm as CreateUserRequest)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.closeModal();
            this.loadUsers();
          },
        });
    }
  }

  toggleUserStatus(user: User): void {
    this.adminService
      .updateUser(user.id, { active: !user.active })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loadUsers();
        },
      });
  }

  confirmDelete(user: User): void {
    this.userToDelete = user;
    this.showDeleteConfirm = true;
  }

  closeDeleteConfirm(): void {
    this.showDeleteConfirm = false;
    this.userToDelete = null;
  }

  deleteUser(): void {
    if (!this.userToDelete) return;

    this.adminService
      .deleteUser(this.userToDelete.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.closeDeleteConfirm();
          this.loadUsers();
        },
      });
  }
}
