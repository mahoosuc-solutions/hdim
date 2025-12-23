import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../services/auth.service';

/**
 * Unauthorized Component - Displayed when user lacks required permissions
 *
 * Shows a friendly error page when:
 * - User tries to access a route they don't have permission for
 * - User's role doesn't match the required route role
 */
@Component({
  selector: 'app-unauthorized',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <div class="unauthorized-container">
      <mat-card class="unauthorized-card">
        <div class="icon-container">
          <mat-icon class="lock-icon">lock</mat-icon>
        </div>

        <h1>Access Denied</h1>
        <p class="message">
          You don't have permission to access this page.
        </p>

        <div class="details">
          @if (currentUser) {
            <p>
              <strong>Logged in as:</strong> {{ currentUser.fullName || currentUser.username }}
            </p>
            <p>
              <strong>Role:</strong> {{ getUserRoles() }}
            </p>
          }
        </div>

        <div class="actions">
          <button mat-raised-button color="primary" (click)="goToDashboard()">
            <mat-icon>home</mat-icon>
            Go to Dashboard
          </button>
          <button mat-stroked-button (click)="goBack()">
            <mat-icon>arrow_back</mat-icon>
            Go Back
          </button>
          <button mat-stroked-button color="warn" (click)="logout()">
            <mat-icon>logout</mat-icon>
            Sign Out
          </button>
        </div>

        <p class="help-text">
          If you believe this is an error, please contact your administrator.
        </p>
      </mat-card>
    </div>
  `,
  styles: [`
    .unauthorized-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: calc(100vh - 64px);
      padding: 20px;
      background: #f5f5f5;
    }

    .unauthorized-card {
      max-width: 500px;
      text-align: center;
      padding: 40px;
    }

    .icon-container {
      width: 80px;
      height: 80px;
      margin: 0 auto 24px;
      background: #ffebee;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .lock-icon {
      font-size: 40px;
      width: 40px;
      height: 40px;
      color: #f44336;
    }

    h1 {
      font-size: 28px;
      font-weight: 500;
      color: #333;
      margin: 0 0 12px;
    }

    .message {
      font-size: 16px;
      color: #666;
      margin-bottom: 24px;
    }

    .details {
      background: #fafafa;
      padding: 16px;
      border-radius: 8px;
      margin-bottom: 24px;
      text-align: left;
    }

    .details p {
      margin: 8px 0;
      font-size: 14px;
      color: #555;
    }

    .actions {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-bottom: 24px;
    }

    .actions button {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }

    .help-text {
      font-size: 13px;
      color: #999;
      margin: 0;
    }
  `],
})
export class UnauthorizedComponent {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  get currentUser() {
    return this.authService.currentUserValue;
  }

  getUserRoles(): string {
    if (!this.currentUser?.roles) {
      return 'None';
    }
    return this.currentUser.roles.map((r) => r.name).join(', ') || 'None';
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  goBack(): void {
    window.history.back();
  }

  logout(): void {
    this.authService.logout();
  }
}
