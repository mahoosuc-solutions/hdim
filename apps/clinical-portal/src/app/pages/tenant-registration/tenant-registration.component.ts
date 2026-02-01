import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { API_CONFIG } from '../../config/api.config';
import { catchError, finalize } from 'rxjs/operators';
import { throwError } from 'rxjs';

interface TenantRegistrationRequest {
  tenantId: string;
  tenantName: string;
  adminUser: {
    username: string;
    email: string;
    password: string;
    firstName: string;
    lastName: string;
  };
}

interface TenantRegistrationResponse {
  tenantId: string;
  tenantName: string;
  status: string;
  adminUser: {
    userId: string;
    username: string;
    email: string;
    roles: string[];
    tenantIds: string[];
  };
}

@Component({
  selector: 'app-tenant-registration',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="registration-container">
      <mat-card class="registration-card">
        <mat-card-header>
          <div class="header-content">
            <mat-icon class="header-icon" aria-hidden="true">business</mat-icon>
            <div>
              <mat-card-title id="registration-title">Register Your Organization</mat-card-title>
              <mat-card-subtitle>Create a new tenant account to get started</mat-card-subtitle>
            </div>
          </div>
        </mat-card-header>

        <mat-card-content>
          @if (successMessage()) {
            <div class="success-message" role="alert" aria-live="polite">
              <mat-icon aria-hidden="true">check_circle</mat-icon>
              <div>
                <strong>Registration Successful!</strong>
                <p>{{ successMessage() }}</p>
              </div>
            </div>
          }

          @if (errorMessage()) {
            <div class="error-message" role="alert" aria-live="assertive">
              <mat-icon aria-hidden="true">error</mat-icon>
              <div>
                <strong>Registration Failed</strong>
                <p>{{ errorMessage() }}</p>
              </div>
            </div>
          }

          <form [formGroup]="registrationForm" (ngSubmit)="onSubmit()" aria-labelledby="registration-title">
            <!-- Organization Information -->
            <div class="section-header" role="group" aria-labelledby="org-info-heading">
              <mat-icon aria-hidden="true">business</mat-icon>
              <h3 id="org-info-heading">Organization Information</h3>
            </div>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Tenant ID</mat-label>
              <input
                matInput
                formControlName="tenantId"
                placeholder="e.g., acme-health"
                required
                aria-describedby="tenantId-hint tenantId-error"
                [attr.aria-invalid]="registrationForm.get('tenantId')?.invalid && registrationForm.get('tenantId')?.touched">
              <mat-icon matPrefix aria-hidden="true">badge</mat-icon>
              <mat-hint id="tenantId-hint">Unique identifier for your organization (3-63 characters, lowercase, hyphens allowed)</mat-hint>
              @if (registrationForm.get('tenantId')?.hasError('required') && registrationForm.get('tenantId')?.touched) {
                <mat-error id="tenantId-error">Tenant ID is required</mat-error>
              }
              @if (registrationForm.get('tenantId')?.hasError('pattern') && registrationForm.get('tenantId')?.touched) {
                <mat-error id="tenantId-error">Must be 3-63 characters, lowercase letters, numbers, and hyphens only</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Organization Name</mat-label>
              <input
                matInput
                formControlName="tenantName"
                placeholder="e.g., Acme Health Clinic"
                required
                aria-describedby="tenantName-error"
                [attr.aria-invalid]="registrationForm.get('tenantName')?.invalid && registrationForm.get('tenantName')?.touched">
              <mat-icon matPrefix aria-hidden="true">domain</mat-icon>
              @if (registrationForm.get('tenantName')?.hasError('required') && registrationForm.get('tenantName')?.touched) {
                <mat-error id="tenantName-error">Organization name is required</mat-error>
              }
            </mat-form-field>

            <!-- Administrator Account -->
            <div class="section-header" role="group" aria-labelledby="admin-account-heading">
              <mat-icon aria-hidden="true">person</mat-icon>
              <h3 id="admin-account-heading">Administrator Account</h3>
            </div>

            <div class="name-row">
              <mat-form-field appearance="outline" class="half-width">
                <mat-label>First Name</mat-label>
                <input
                  matInput
                  formControlName="firstName"
                  required
                  aria-describedby="firstName-error"
                  [attr.aria-invalid]="registrationForm.get('firstName')?.invalid && registrationForm.get('firstName')?.touched">
                <mat-icon matPrefix aria-hidden="true">person</mat-icon>
                @if (registrationForm.get('firstName')?.hasError('required') && registrationForm.get('firstName')?.touched) {
                  <mat-error id="firstName-error">First name is required</mat-error>
                }
              </mat-form-field>

              <mat-form-field appearance="outline" class="half-width">
                <mat-label>Last Name</mat-label>
                <input
                  matInput
                  formControlName="lastName"
                  required
                  aria-describedby="lastName-error"
                  [attr.aria-invalid]="registrationForm.get('lastName')?.invalid && registrationForm.get('lastName')?.touched">
                @if (registrationForm.get('lastName')?.hasError('required') && registrationForm.get('lastName')?.touched) {
                  <mat-error id="lastName-error">Last name is required</mat-error>
                }
              </mat-form-field>
            </div>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Username</mat-label>
              <input
                matInput
                formControlName="username"
                placeholder="e.g., admin@acme-health"
                required
                aria-describedby="username-error"
                [attr.aria-invalid]="registrationForm.get('username')?.invalid && registrationForm.get('username')?.touched">
              <mat-icon matPrefix aria-hidden="true">account_circle</mat-icon>
              @if (registrationForm.get('username')?.hasError('required') && registrationForm.get('username')?.touched) {
                <mat-error id="username-error">Username is required</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Email</mat-label>
              <input
                matInput
                type="email"
                formControlName="email"
                placeholder="e.g., admin@acme-health.com"
                required
                aria-describedby="email-error"
                [attr.aria-invalid]="registrationForm.get('email')?.invalid && registrationForm.get('email')?.touched">
              <mat-icon matPrefix aria-hidden="true">email</mat-icon>
              @if (registrationForm.get('email')?.hasError('required') && registrationForm.get('email')?.touched) {
                <mat-error id="email-error">Email is required</mat-error>
              }
              @if (registrationForm.get('email')?.hasError('email') && registrationForm.get('email')?.touched) {
                <mat-error id="email-error">Must be a valid email address</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Password</mat-label>
              <input
                matInput
                [type]="hidePassword() ? 'password' : 'text'"
                formControlName="password"
                required
                aria-describedby="password-hint password-error"
                [attr.aria-invalid]="registrationForm.get('password')?.invalid && registrationForm.get('password')?.touched">
              <mat-icon matPrefix aria-hidden="true">lock</mat-icon>
              <button
                mat-icon-button
                matSuffix
                type="button"
                (click)="togglePasswordVisibility()"
                [attr.aria-label]="hidePassword() ? 'Show password' : 'Hide password'">
                <mat-icon aria-hidden="true">{{ hidePassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              <mat-hint id="password-hint">Must be at least 8 characters with uppercase, lowercase, number, and special character</mat-hint>
              @if (registrationForm.get('password')?.hasError('required') && registrationForm.get('password')?.touched) {
                <mat-error id="password-error">Password is required</mat-error>
              }
              @if (registrationForm.get('password')?.hasError('minlength') && registrationForm.get('password')?.touched) {
                <mat-error id="password-error">Password must be at least 8 characters</mat-error>
              }
              @if (registrationForm.get('password')?.hasError('pattern') && registrationForm.get('password')?.touched) {
                <mat-error id="password-error">Password must include uppercase, lowercase, number, and special character</mat-error>
              }
            </mat-form-field>

            <!-- Actions -->
            <div class="actions">
              <button
                mat-stroked-button
                type="button"
                routerLink="/login"
                [disabled]="isLoading()"
                aria-label="Cancel registration and return to login">
                <mat-icon aria-hidden="true">arrow_back</mat-icon>
                Back to Login
              </button>

              <button
                mat-raised-button
                color="primary"
                type="submit"
                [disabled]="registrationForm.invalid || isLoading()"
                [attr.aria-label]="isLoading() ? 'Registration in progress' : 'Submit registration form'">
                @if (isLoading()) {
                  <mat-spinner diameter="20" aria-hidden="true"></mat-spinner>
                  <span>Registering...</span>
                } @else {
                  <mat-icon aria-hidden="true">business</mat-icon>
                  <span>Register Organization</span>
                }
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>

      <div class="login-link">
        Already have an account? <a routerLink="/login" aria-label="Sign in to existing account">Sign in here</a>
      </div>
    </div>
  `,
  styles: [`
    .registration-container {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 2rem;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }

    .registration-card {
      width: 100%;
      max-width: 600px;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
    }

    .header-content {
      display: flex;
      align-items: center;
      gap: 1rem;
      width: 100%;
    }

    .header-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #667eea;
    }

    mat-card-content {
      padding-top: 1rem;
    }

    .section-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin: 1.5rem 0 1rem;
      color: #667eea;
    }

    .section-header h3 {
      margin: 0;
      font-size: 1.1rem;
      font-weight: 500;
    }

    .section-header mat-icon {
      font-size: 24px;
      width: 24px;
      height: 24px;
    }

    .full-width {
      width: 100%;
    }

    .name-row {
      display: flex;
      gap: 1rem;
    }

    .half-width {
      flex: 1;
    }

    .actions {
      display: flex;
      justify-content: space-between;
      margin-top: 2rem;
      gap: 1rem;
    }

    .actions button {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .actions mat-spinner {
      margin-right: 0.5rem;
    }

    .success-message,
    .error-message {
      display: flex;
      align-items: flex-start;
      gap: 1rem;
      padding: 1rem;
      margin-bottom: 1.5rem;
      border-radius: 4px;
    }

    .success-message {
      background-color: #e8f5e9;
      color: #2e7d32;
    }

    .success-message mat-icon {
      color: #4caf50;
    }

    .error-message {
      background-color: #ffebee;
      color: #c62828;
    }

    .error-message mat-icon {
      color: #f44336;
    }

    .success-message p,
    .error-message p {
      margin: 0.25rem 0 0;
    }

    .login-link {
      margin-top: 1rem;
      color: white;
      text-align: center;
    }

    .login-link a {
      color: white;
      font-weight: 500;
      text-decoration: underline;
    }

    @media (max-width: 768px) {
      .registration-container {
        padding: 1rem;
      }

      .name-row {
        flex-direction: column;
        gap: 0;
      }

      .actions {
        flex-direction: column;
      }

      .actions button {
        width: 100%;
      }
    }
  `],
})
export class TenantRegistrationComponent {
  registrationForm: FormGroup;
  isLoading = signal(false);
  hidePassword = signal(true);
  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private router: Router
  ) {
    this.registrationForm = this.fb.group({
      tenantId: ['', [Validators.required, Validators.pattern(/^[a-z0-9-]{3,63}$/)]],
      tenantName: ['', Validators.required],
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      username: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/),
        ],
      ],
    });
  }

  togglePasswordVisibility(): void {
    this.hidePassword.set(!this.hidePassword());
  }

  onSubmit(): void {
    if (this.registrationForm.invalid) {
      return;
    }

    this.isLoading.set(true);
    this.successMessage.set(null);
    this.errorMessage.set(null);

    const formValue = this.registrationForm.value;
    const request: TenantRegistrationRequest = {
      tenantId: formValue.tenantId,
      tenantName: formValue.tenantName,
      adminUser: {
        username: formValue.username,
        email: formValue.email,
        password: formValue.password,
        firstName: formValue.firstName,
        lastName: formValue.lastName,
      },
    };

    this.http
      .post<TenantRegistrationResponse>('/tenants/register', request)
      .pipe(
        catchError((error: HttpErrorResponse) => {
          let errorMsg = 'An error occurred during registration. Please try again.';

          if (error.error?.message) {
            errorMsg = error.error.message;
          } else if (error.status === 409) {
            errorMsg = 'This tenant ID or username already exists. Please choose different values.';
          } else if (error.status === 400) {
            errorMsg = 'Invalid registration data. Please check all fields and try again.';
          } else if (error.status === 0) {
            errorMsg = 'Unable to connect to the server. Please check your connection.';
          }

          this.errorMessage.set(errorMsg);
          return throwError(() => error);
        }),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (response) => {
          this.successMessage.set(
            `Organization "${response.tenantName}" has been registered successfully! ` +
            `Redirecting to login...`
          );

          // Redirect to login after 3 seconds
          setTimeout(() => {
            this.router.navigate(['/login'], {
              queryParams: {
                registered: 'true',
                tenantId: response.tenantId,
                username: response.adminUser.username,
              },
            });
          }, 3000);
        },
      });
  }
}
