import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterOutlet } from '@angular/router';
import { AuthTokenService } from './shared/services/auth-token.service';

/**
 * CX Portal Root Component
 *
 * Main layout with navigation and router outlet.
 */
@Component({
  selector: 'cx-root',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterOutlet],
  template: `
    <div class="app-container">
      <nav class="sidebar">
        <div class="logo">
          <span class="logo-icon">📊</span>
          <span class="logo-text">HDIM CX</span>
        </div>

        <ul class="nav-menu">
          <li>
            <a routerLink="/dashboard" routerLinkActive="active">
              <span class="nav-icon">🏠</span>
              <span class="nav-text">Dashboard</span>
            </a>
          </li>
          <li>
            <a routerLink="/pipeline" routerLinkActive="active">
              <span class="nav-icon">📈</span>
              <span class="nav-text">Sales Pipeline</span>
            </a>
          </li>
          <li>
            <a routerLink="/investors" routerLinkActive="active">
              <span class="nav-icon">💼</span>
              <span class="nav-text">Investors</span>
            </a>
          </li>
          <li>
            <a routerLink="/customers" routerLinkActive="active">
              <span class="nav-icon">🏢</span>
              <span class="nav-text">Customers</span>
            </a>
          </li>
          <li>
            <a routerLink="/campaigns" routerLinkActive="active">
              <span class="nav-icon">🎯</span>
              <span class="nav-text">Campaigns</span>
            </a>
          </li>
          <li>
            <a routerLink="/agents" routerLinkActive="active">
              <span class="nav-icon">🤖</span>
              <span class="nav-text">Agent Activity</span>
            </a>
          </li>
          <li>
            <a routerLink="/approvals" routerLinkActive="active">
              <span class="nav-icon">✅</span>
              <span class="nav-text">Approvals</span>
            </a>
          </li>
          <li>
            <a routerLink="/admin/access" routerLinkActive="active">
              <span class="nav-icon">🔐</span>
              <span class="nav-text">Access Admin</span>
            </a>
          </li>
        </ul>

        <div class="sidebar-footer">
          <div class="status-indicator online">
            <span class="dot"></span>
            <span>API Connected</span>
          </div>
          <div class="auth-tools">
            <label class="auth-label" for="authToken">Admin Token</label>
            <input
              id="authToken"
              class="auth-input"
              type="password"
              autocomplete="off"
              [value]="authTokenDraft"
              (input)="authTokenDraft = ($any($event.target).value || '')"
              placeholder="Paste JWT to enable admin calls"
            />
            <div class="auth-actions">
              <button class="auth-btn primary" type="button" (click)="saveToken()">
                Save
              </button>
              <button class="auth-btn" type="button" (click)="clearToken()">
                Clear
              </button>
            </div>
            <div class="auth-hint" *ngIf="hasToken">Token set for this browser.</div>
          </div>
        </div>
      </nav>

      <main class="main-content">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      height: 100vh;
    }

    .app-container {
      display: flex;
      height: 100%;
      background: #f5f7fa;
    }

    .sidebar {
      width: 240px;
      background: #1a1a2e;
      color: white;
      display: flex;
      flex-direction: column;
      flex-shrink: 0;
    }

    .logo {
      padding: 24px;
      display: flex;
      align-items: center;
      gap: 12px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }

    .logo-icon {
      font-size: 24px;
    }

    .logo-text {
      font-size: 20px;
      font-weight: 600;
    }

    .nav-menu {
      list-style: none;
      padding: 16px 0;
      margin: 0;
      flex: 1;
    }

    .nav-menu li {
      margin: 4px 0;
    }

    .nav-menu a {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 24px;
      color: rgba(255, 255, 255, 0.7);
      text-decoration: none;
      transition: all 0.2s;
    }

    .nav-menu a:hover {
      background: rgba(255, 255, 255, 0.1);
      color: white;
    }

    .nav-menu a.active {
      background: #4361ee;
      color: white;
    }

    .nav-icon {
      font-size: 18px;
    }

    .nav-text {
      font-size: 14px;
    }

    .sidebar-footer {
      padding: 16px 24px;
      border-top: 1px solid rgba(255, 255, 255, 0.1);
    }

    .status-indicator {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 12px;
      color: rgba(255, 255, 255, 0.7);
    }

    .status-indicator .dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }

    .status-indicator.online .dot {
      background: #2ecc71;
    }

    .status-indicator.offline .dot {
      background: #e74c3c;
    }

    .auth-tools {
      margin-top: 14px;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .auth-label {
      font-size: 12px;
      color: rgba(255, 255, 255, 0.75);
    }

    .auth-input {
      border: 1px solid rgba(255, 255, 255, 0.15);
      background: rgba(255, 255, 255, 0.06);
      border-radius: 8px;
      padding: 8px 10px;
      font-size: 12px;
      color: rgba(255, 255, 255, 0.9);
      outline: none;
    }

    .auth-input::placeholder {
      color: rgba(255, 255, 255, 0.45);
    }

    .auth-actions {
      display: flex;
      gap: 8px;
    }

    .auth-btn {
      border: 1px solid rgba(255, 255, 255, 0.15);
      background: transparent;
      color: rgba(255, 255, 255, 0.85);
      border-radius: 8px;
      padding: 7px 10px;
      font-size: 12px;
      cursor: pointer;
    }

    .auth-btn.primary {
      background: #4361ee;
      border-color: transparent;
      color: #fff;
    }

    .auth-hint {
      font-size: 11px;
      color: rgba(255, 255, 255, 0.6);
    }

    .main-content {
      flex: 1;
      overflow-y: auto;
    }
  `],
})
export class App {
  protected title = 'HDIM CX Portal';

  protected authTokenDraft = '';
  protected hasToken = false;

  constructor(private tokens: AuthTokenService) {
    this.hasToken = this.tokens.has();
  }

  saveToken(): void {
    const token = (this.authTokenDraft || '').trim();
    if (!token) return;
    this.tokens.set(token);
    this.authTokenDraft = '';
    this.hasToken = true;
    // A reload keeps existing components simple (they'll re-fetch with Authorization).
    window.location.reload();
  }

  clearToken(): void {
    this.tokens.clear();
    this.authTokenDraft = '';
    this.hasToken = false;
    window.location.reload();
  }
}
