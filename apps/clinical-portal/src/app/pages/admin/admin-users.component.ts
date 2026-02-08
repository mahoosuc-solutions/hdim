import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

interface AdminUser {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="admin-page">
      <h1>Admin Users</h1>

      <div class="actions">
        <button type="button" data-test-id="create-user-button" (click)="startCreate()">Create User</button>
      </div>

      @if (showForm) {
        <form [formGroup]="userForm" class="user-form" (ngSubmit)="saveUser()">
          <label>
            Username
            <input data-test-id="user-username" formControlName="username" />
          </label>
          <label>
            Email
            <input data-test-id="user-email" formControlName="email" type="email" />
          </label>
          <label>
            First Name
            <input data-test-id="user-first-name" formControlName="firstName" />
          </label>
          <label>
            Last Name
            <input data-test-id="user-last-name" formControlName="lastName" />
          </label>
          <label>
            Role
            <select data-test-id="user-role-select" formControlName="role">
              <option data-test-id="role-option-EVALUATOR" value="EVALUATOR">EVALUATOR</option>
              <option data-test-id="role-option-ANALYST" value="ANALYST">ANALYST</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </label>
          <button type="submit" data-test-id="save-user-button">Save</button>
        </form>
      }

      @if (successMessage) {
        <div data-test-id="success-message">{{ successMessage }}</div>
      }

      <table class="user-table">
        <thead>
          <tr>
            <th>Username</th>
            <th>Email</th>
            <th>Name</th>
            <th>Role</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          @for (user of users; track user.username) {
            <tr data-test-id="user-row">
              <td>{{ user.username }}</td>
              <td>{{ user.email }}</td>
              <td>{{ user.firstName }} {{ user.lastName }}</td>
              <td>{{ user.role }}</td>
              <td>
                <button type="button" data-test-id="edit-user-button" (click)="editUser(user)">Edit</button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </section>
  `,
  styles: [
    `
      .admin-page { padding: 24px; }
      .actions { margin-bottom: 16px; }
      .user-form { display: grid; gap: 12px; margin-bottom: 16px; max-width: 420px; }
      .user-form label { display: grid; gap: 6px; }
      .user-table { width: 100%; border-collapse: collapse; }
      .user-table th, .user-table td { border-bottom: 1px solid #e0e0e0; padding: 8px; }
    `,
  ],
})
export class AdminUsersComponent {
  users: AdminUser[] = [
    {
      username: 'demo_admin',
      email: 'demo_admin@hdim.ai',
      firstName: 'Demo',
      lastName: 'Admin',
      role: 'ADMIN',
    },
  ];
  showForm = false;
  editingUser: AdminUser | null = null;
  successMessage = '';

  userForm: FormGroup;

  constructor(private fb: FormBuilder) {
    this.userForm = this.fb.group({
      username: ['', Validators.required],
      email: ['', Validators.required],
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      role: ['EVALUATOR', Validators.required],
    });
  }

  startCreate(): void {
    this.editingUser = null;
    this.showForm = true;
    this.successMessage = '';
    this.userForm.reset({ role: 'EVALUATOR' });
  }

  editUser(user: AdminUser): void {
    this.editingUser = user;
    this.showForm = true;
    this.successMessage = '';
    this.userForm.setValue({
      username: user.username,
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      role: user.role,
    });
  }

  saveUser(): void {
    if (this.userForm.invalid) return;

    const value = this.userForm.value as AdminUser;

    if (this.editingUser) {
      this.editingUser.username = value.username;
      this.editingUser.email = value.email;
      this.editingUser.firstName = value.firstName;
      this.editingUser.lastName = value.lastName;
      this.editingUser.role = value.role;
      this.successMessage = 'User updated successfully';
    } else {
      this.users = [...this.users, { ...value }];
      this.successMessage = 'User created successfully';
    }

    this.showForm = false;
  }
}
