import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { InvestorAuthService } from '../../services/investor-auth.service';

/**
 * Login page for the Investor Dashboard.
 * Simple email/password authentication with JWT tokens.
 */
@Component({
  selector: 'app-investor-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="login-container">
      <mat-card class="login-card">
        <mat-card-header>
          <mat-card-title>
            <span class="logo">🚀</span>
            Investor Dashboard
          </mat-card-title>
          <mat-card-subtitle>Sign in to manage your fundraise</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          @if (errorMessage()) {
            <div class="error-message">
              <mat-icon>error_outline</mat-icon>
              {{ errorMessage() }}
            </div>
          }

          <form (ngSubmit)="onSubmit()" #loginForm="ngForm">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Email</mat-label>
              <input
                matInput
                type="email"
                name="email"
                [(ngModel)]="email"
                required
                email
                placeholder="admin@hdim.ai"
                [disabled]="isLoading()"
              />
              <mat-icon matSuffix>email</mat-icon>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Password</mat-label>
              <input
                matInput
                [type]="hidePassword ? 'password' : 'text'"
                name="password"
                [(ngModel)]="password"
                required
                minlength="6"
                [disabled]="isLoading()"
              />
              <button
                mat-icon-button
                matSuffix
                type="button"
                (click)="hidePassword = !hidePassword"
              >
                <mat-icon>{{ hidePassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>

            <button
              mat-raised-button
              color="primary"
              type="submit"
              class="login-button"
              [disabled]="!loginForm.valid || isLoading()"
            >
              @if (isLoading()) {
                <mat-spinner diameter="20"></mat-spinner>
                Signing in...
              } @else {
                Sign In
              }
            </button>
          </form>

          <div class="demo-credentials">
            <p><strong>Demo credentials:</strong></p>
            <p>Email: admin&#64;hdim.ai</p>
            <p>Password: admin123</p>
          </div>
        </mat-card-content>
      </mat-card>

      <div class="powered-by">
        <span>Powered by</span>
        <strong>HealthData-in-Motion</strong>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      padding: 24px;
    }

    .login-card {
      width: 100%;
      max-width: 400px;
      padding: 24px;
    }

    mat-card-header {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      margin-bottom: 24px;
    }

    .logo {
      font-size: 48px;
      display: block;
      margin-bottom: 8px;
    }

    mat-card-title {
      font-size: 24px !important;
      font-weight: 600;
    }

    mat-card-subtitle {
      margin-top: 4px;
    }

    .full-width {
      width: 100%;
      margin-bottom: 16px;
    }

    .login-button {
      width: 100%;
      height: 48px;
      font-size: 16px;
      margin-top: 8px;
    }

    .login-button mat-spinner {
      display: inline-block;
      margin-right: 8px;
    }

    .error-message {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px;
      margin-bottom: 16px;
      background: #ffebee;
      color: #c62828;
      border-radius: 4px;
      font-size: 14px;
    }

    .error-message mat-icon {
      font-size: 20px;
      height: 20px;
      width: 20px;
    }

    .demo-credentials {
      margin-top: 24px;
      padding: 16px;
      background: #f5f5f5;
      border-radius: 4px;
      font-size: 13px;
      text-align: center;
    }

    .demo-credentials p {
      margin: 4px 0;
    }

    .powered-by {
      margin-top: 24px;
      color: white;
      font-size: 14px;
      opacity: 0.9;
    }

    .powered-by strong {
      margin-left: 4px;
    }
  `],
})
export class InvestorLoginComponent {
  private authService = inject(InvestorAuthService);
  private router = inject(Router);

  email = '';
  password = '';
  hidePassword = true;

  isLoading = this.authService.isLoading;
  errorMessage = signal<string>('');

  constructor() {
    // Redirect if already logged in
    if (this.authService.hasValidToken()) {
      this.router.navigate(['/investor-launch']);
    }
  }

  onSubmit(): void {
    this.errorMessage.set('');

    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: () => {
        this.router.navigate(['/investor-launch']);
      },
      error: (error) => {
        this.errorMessage.set(error.message || 'Login failed. Please try again.');
      },
    });
  }
}
