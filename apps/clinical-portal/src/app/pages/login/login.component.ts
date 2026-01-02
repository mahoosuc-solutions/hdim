import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService, MfaRequiredResponse } from '../../services/auth.service';
import { MfaVerifyComponent } from './mfa-verify.component';

/**
 * Login Component - Handles user authentication
 *
 * Features:
 * - Username/password login form
 * - Form validation
 * - Loading state
 * - Error handling with user feedback
 * - Return URL redirect after successful login
 * - Demo mode bypass for development
 */
@Component({
  selector: 'app-login',
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
    MatSnackBarModule,
    MatCheckboxModule,
    MfaVerifyComponent,
  ],
  template: `
    <div class="login-container">
      <mat-card class="login-card">
        <mat-card-header>
          <div class="logo-container">
            <mat-icon class="logo-icon">health_and_safety</mat-icon>
            <h1>Health Data Intelligence</h1>
          </div>
        </mat-card-header>

        <mat-card-content>
          <!-- MFA Verification Step -->
          @if (showMfaVerify) {
            <app-mfa-verify
              [isLoading]="isLoading"
              [errorMessage]="mfaErrorMessage"
              (verify)="onMfaVerify($event)"
              (cancel)="onMfaCancelled()"
            ></app-mfa-verify>
          } @else {
          <!-- Login Form -->
          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Username</mat-label>
              <input
                matInput
                formControlName="username"
                placeholder="Enter your username"
                autocomplete="username"
              />
              <mat-icon matPrefix>person</mat-icon>
              @if (loginForm.get('username')?.hasError('required') && loginForm.get('username')?.touched) {
                <mat-error>Username is required</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Password</mat-label>
              <input
                matInput
                [type]="hidePassword ? 'password' : 'text'"
                formControlName="password"
                placeholder="Enter your password"
                autocomplete="current-password"
              />
              <mat-icon matPrefix>lock</mat-icon>
              <button
                mat-icon-button
                matSuffix
                type="button"
                (click)="hidePassword = !hidePassword"
                [attr.aria-label]="hidePassword ? 'Show password' : 'Hide password'"
              >
                <mat-icon>{{ hidePassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              @if (loginForm.get('password')?.hasError('required') && loginForm.get('password')?.touched) {
                <mat-error>Password is required</mat-error>
              }
            </mat-form-field>

            <div class="form-options">
              <mat-checkbox formControlName="rememberMe">Remember me</mat-checkbox>
              <a routerLink="/forgot-password" class="forgot-link">Forgot password?</a>
            </div>

            <button
              mat-raised-button
              color="primary"
              type="submit"
              class="login-button full-width"
              [disabled]="loginForm.invalid || isLoading"
            >
              @if (isLoading) {
                <mat-spinner diameter="20"></mat-spinner>
                <span>Signing in...</span>
              } @else {
                <mat-icon>login</mat-icon>
                <span>Sign In</span>
              }
            </button>
          </form>

          <!-- Demo mode notice -->
          @if (isDemoMode) {
            <div class="demo-notice">
              <mat-icon>info</mat-icon>
              <div class="demo-text">
                <strong>Demo Mode</strong>
                <p>Use any credentials or click "Demo Login" for quick access.</p>
              </div>
            </div>
            <button
              mat-stroked-button
              color="accent"
              class="demo-button full-width"
              (click)="demoLogin()"
              [disabled]="isLoading"
            >
              <mat-icon>science</mat-icon>
              <span>Demo Login</span>
            </button>
          }
          }
        </mat-card-content>

        <mat-card-footer>
          <p class="security-notice">
            <mat-icon>security</mat-icon>
            Secure HIPAA-compliant authentication
          </p>
        </mat-card-footer>
      </mat-card>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      padding: 20px;
    }

    .login-card {
      width: 100%;
      max-width: 400px;
      padding: 24px;
    }

    .logo-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      width: 100%;
      margin-bottom: 24px;
    }

    .logo-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #667eea;
      margin-bottom: 12px;
    }

    .logo-container h1 {
      font-size: 20px;
      font-weight: 500;
      color: #333;
      text-align: center;
      margin: 0;
    }

    .full-width {
      width: 100%;
    }

    mat-form-field {
      margin-bottom: 8px;
    }

    .form-options {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      font-size: 14px;
    }

    .forgot-link {
      color: #667eea;
      text-decoration: none;
    }

    .forgot-link:hover {
      text-decoration: underline;
    }

    .login-button {
      height: 48px;
      font-size: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }

    .login-button mat-spinner {
      margin-right: 8px;
    }

    .demo-notice {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 16px;
      margin: 16px 0;
      background: #fff3e0;
      border-radius: 8px;
      border-left: 4px solid #ff9800;
    }

    .demo-notice mat-icon {
      color: #ff9800;
    }

    .demo-text {
      flex: 1;
    }

    .demo-text strong {
      display: block;
      margin-bottom: 4px;
    }

    .demo-text p {
      margin: 0;
      font-size: 13px;
      color: #666;
    }

    .demo-button {
      margin-top: 8px;
      height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }

    .security-notice {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      font-size: 12px;
      color: #666;
      margin: 16px 0 0;
    }

    .security-notice mat-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
      color: #4caf50;
    }

    mat-card-header {
      display: block;
    }

    mat-card-footer {
      padding: 0 16px;
    }
  `],
})
export class LoginComponent implements OnInit, OnDestroy {
  loginForm: FormGroup;
  isLoading = false;
  hidePassword = true;
  isDemoMode = true; // Enable demo mode for development
  returnUrl = '/dashboard';

  // MFA state
  showMfaVerify = false;
  mfaToken = '';
  mfaErrorMessage = '';

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]],
      rememberMe: [false],
    });
  }

  ngOnInit(): void {
    // Get return URL from route parameters or default to dashboard
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';

    // If already authenticated, redirect to return URL
    if (this.authService.isAuthenticated()) {
      this.router.navigate([this.returnUrl]);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      return;
    }

    this.isLoading = true;
    const { username, password } = this.loginForm.value;

    this.authService
      .login(username, password)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          // Check if MFA is required
          if (this.authService.isMfaRequired(response)) {
            this.isLoading = false;
            this.mfaToken = (response as MfaRequiredResponse).mfaToken;
            this.showMfaVerify = true;
            this.mfaErrorMessage = '';
            return;
          }

          // Normal login success
          this.snackBar.open('Login successful!', 'Close', {
            duration: 3000,
            panelClass: ['success-snackbar'],
          });
          this.router.navigate([this.returnUrl]);
        },
        error: (error) => {
          this.isLoading = false;
          let message = 'Login failed. Please check your credentials.';

          if (error.status === 401) {
            message = 'Invalid username or password.';
          } else if (error.status === 403) {
            message = 'Account is locked. Please contact support.';
          } else if (error.status === 0) {
            message = 'Unable to connect to server. Please try again.';
          }

          this.snackBar.open(message, 'Close', {
            duration: 5000,
            panelClass: ['error-snackbar'],
          });
        },
      });
  }

  /**
   * Handle MFA verification
   */
  onMfaVerify(event: { code: string; useRecoveryCode: boolean }): void {
    this.isLoading = true;
    this.mfaErrorMessage = '';

    this.authService
      .verifyMfa(this.mfaToken, event.code, event.useRecoveryCode)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.snackBar.open('Login successful!', 'Close', {
            duration: 3000,
            panelClass: ['success-snackbar'],
          });
          this.router.navigate([this.returnUrl]);
        },
        error: (error) => {
          this.isLoading = false;
          if (error.status === 401) {
            this.mfaErrorMessage = 'Invalid code. Please try again.';
          } else {
            this.mfaErrorMessage = 'Verification failed. Please try again.';
          }
        },
      });
  }

  /**
   * Handle MFA cancellation - go back to login form
   */
  onMfaCancelled(): void {
    this.showMfaVerify = false;
    this.mfaToken = '';
    this.mfaErrorMessage = '';
    this.loginForm.reset();
  }

  demoLogin(): void {
    // For demo mode, bypass authentication and set demo user
    this.isLoading = true;

    // Simulate a brief loading state
    setTimeout(() => {
      // Set demo token and user in localStorage
      const demoToken = 'demo-jwt-token-' + Date.now();
      const demoUser = {
        id: 'demo-user-1',
        username: 'demo',
        email: 'demo@healthdata.com',
        firstName: 'Demo',
        lastName: 'User',
        fullName: 'Demo User',
        roles: [
          {
            id: 'role-admin',
            name: 'ADMIN',
            description: 'Administrator',
            permissions: [
              { id: 'perm-1', name: 'VIEW_PATIENTS', description: 'View patients' },
              { id: 'perm-2', name: 'EDIT_PATIENTS', description: 'Edit patients' },
              { id: 'perm-3', name: 'VIEW_EVALUATIONS', description: 'View evaluations' },
              { id: 'perm-4', name: 'RUN_EVALUATIONS', description: 'Run evaluations' },
              { id: 'perm-5', name: 'EXPORT_DATA', description: 'Export data' },
            ],
          },
        ],
        tenantId: 'demo-tenant',
        active: true,
      };

      // Store demo credentials
      localStorage.setItem('healthdata_auth_token', demoToken);
      localStorage.setItem('healthdata_user', JSON.stringify(demoUser));

      this.snackBar.open('Demo login successful!', 'Close', {
        duration: 3000,
        panelClass: ['success-snackbar'],
      });

      // Force page reload to reinitialize auth state
      window.location.href = this.returnUrl;
    }, 500);
  }
}
