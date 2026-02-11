import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { SalesAuthService } from '../../../services/sales-auth.service';

@Component({
  selector: 'app-sales-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <div class="logo">
            <span class="logo-icon">📊</span>
            <span class="logo-text">HDIM Sales</span>
          </div>
          <h1>Sales Automation Engine</h1>
          <p>Sign in to access your sales dashboard</p>
        </div>

        <form (ngSubmit)="onSubmit()" class="login-form">
          <div class="form-group">
            <label for="email">Email</label>
            <input
              type="email"
              id="email"
              [(ngModel)]="email"
              name="email"
              placeholder="Enter your email"
              required
              [disabled]="isLoading()"
            />
          </div>

          <div class="form-group">
            <label for="password">Password</label>
            <input
              type="password"
              id="password"
              [(ngModel)]="password"
              name="password"
              placeholder="Enter your password"
              required
              [disabled]="isLoading()"
            />
          </div>

          <div class="error-message" *ngIf="errorMessage()">
            {{ errorMessage() }}
          </div>

          <button type="submit" class="btn-login" [disabled]="isLoading()">
            <span *ngIf="!isLoading()">Sign In</span>
            <span *ngIf="isLoading()" class="loading-spinner"></span>
          </button>
        </form>

        <div class="login-footer">
          <p class="demo-credentials">
            Demo credentials:<br/>
            <strong>sales&#64;hdim.health</strong> / <strong>sales2026!</strong>
          </p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #1a237e 0%, #0d47a1 100%);
      padding: 20px;
    }

    .login-card {
      background: white;
      border-radius: 16px;
      padding: 48px;
      width: 100%;
      max-width: 420px;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    }

    .login-header {
      text-align: center;
      margin-bottom: 32px;
    }

    .logo {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      margin-bottom: 16px;
    }

    .logo-icon {
      font-size: 40px;
    }

    .logo-text {
      font-size: 28px;
      font-weight: 700;
      color: #1a237e;
    }

    .login-header h1 {
      margin: 0 0 8px 0;
      font-size: 24px;
      color: #333;
    }

    .login-header p {
      margin: 0;
      color: #666;
      font-size: 14px;
    }

    .login-form {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .form-group label {
      font-weight: 500;
      color: #333;
      font-size: 14px;
    }

    .form-group input {
      padding: 14px 16px;
      border: 1px solid #ddd;
      border-radius: 8px;
      font-size: 16px;
      transition: border-color 0.2s ease, box-shadow 0.2s ease;
    }

    .form-group input:focus {
      outline: none;
      border-color: #1a237e;
      box-shadow: 0 0 0 3px rgba(26, 35, 126, 0.1);
    }

    .form-group input:disabled {
      background: #f5f5f5;
      cursor: not-allowed;
    }

    .error-message {
      background: #ffebee;
      color: #c62828;
      padding: 12px 16px;
      border-radius: 8px;
      font-size: 14px;
      text-align: center;
    }

    .btn-login {
      padding: 16px;
      background: #1a237e;
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 16px;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s ease;
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 52px;
    }

    .btn-login:hover:not(:disabled) {
      background: #0d47a1;
    }

    .btn-login:disabled {
      background: #9fa8da;
      cursor: not-allowed;
    }

    .loading-spinner {
      width: 20px;
      height: 20px;
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .login-footer {
      margin-top: 32px;
      padding-top: 24px;
      border-top: 1px solid #eee;
      text-align: center;
    }

    .demo-credentials {
      color: #666;
      font-size: 13px;
      margin: 0;
      line-height: 1.6;
    }

    .demo-credentials strong {
      color: #1a237e;
    }
  `],
})
export class SalesLoginComponent {
  private readonly authService = inject(SalesAuthService);
  private readonly router = inject(Router);

  email = '';
  password = '';
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  onSubmit(): void {
    if (!this.email || !this.password) {
      this.errorMessage.set('Please enter email and password');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: () => {
        this.router.navigate(['/sales/dashboard']);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.message || 'Login failed. Please try again.');
      },
    });
  }
}
