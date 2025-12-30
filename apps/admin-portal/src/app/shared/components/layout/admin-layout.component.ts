import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  badge?: number;
}

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="admin-layout">
      <!-- Sidebar -->
      <aside class="sidebar" [class.collapsed]="sidebarCollapsed">
        <div class="sidebar-header">
          <div class="logo">
            <span class="logo-icon">H</span>
            <span class="logo-text" *ngIf="!sidebarCollapsed">HDIM Admin</span>
          </div>
          <button class="toggle-btn" (click)="toggleSidebar()">
            <span class="icon">{{ sidebarCollapsed ? '>' : '<' }}</span>
          </button>
        </div>

        <nav class="sidebar-nav">
          <a
            *ngFor="let item of navItems"
            [routerLink]="item.route"
            routerLinkActive="active"
            class="nav-item"
            [title]="item.label"
          >
            <span class="nav-icon">{{ item.icon }}</span>
            <span class="nav-label" *ngIf="!sidebarCollapsed">{{ item.label }}</span>
            <span class="nav-badge" *ngIf="item.badge && !sidebarCollapsed">{{ item.badge }}</span>
          </a>
        </nav>

        <div class="sidebar-footer">
          <div class="user-info" *ngIf="!sidebarCollapsed">
            <span class="user-avatar">A</span>
            <div class="user-details">
              <span class="user-name">Admin User</span>
              <span class="user-role">Administrator</span>
            </div>
          </div>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="main-content">
        <header class="top-header">
          <div class="header-left">
            <h1 class="page-title">Admin Portal</h1>
          </div>
          <div class="header-right">
            <span class="status-indicator online"></span>
            <span class="status-text">All Systems Operational</span>
          </div>
        </header>

        <div class="content-area">
          <router-outlet></router-outlet>
        </div>
      </main>
    </div>
  `,
  styles: [`
    .admin-layout {
      display: flex;
      min-height: 100vh;
      background: #f5f7fa;
    }

    .sidebar {
      width: 260px;
      background: linear-gradient(180deg, #1a237e 0%, #0d47a1 100%);
      color: white;
      display: flex;
      flex-direction: column;
      transition: width 0.3s ease;
    }

    .sidebar.collapsed {
      width: 70px;
    }

    .sidebar-header {
      padding: 20px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }

    .logo {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .logo-icon {
      width: 36px;
      height: 36px;
      background: white;
      color: #1a237e;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      font-size: 18px;
    }

    .logo-text {
      font-size: 18px;
      font-weight: 600;
    }

    .toggle-btn {
      background: none;
      border: none;
      color: white;
      cursor: pointer;
      padding: 8px;
      border-radius: 4px;
    }

    .toggle-btn:hover {
      background: rgba(255, 255, 255, 0.1);
    }

    .sidebar-nav {
      flex: 1;
      padding: 16px 12px;
    }

    .nav-item {
      display: flex;
      align-items: center;
      padding: 12px 16px;
      color: rgba(255, 255, 255, 0.8);
      text-decoration: none;
      border-radius: 8px;
      margin-bottom: 4px;
      transition: all 0.2s ease;
    }

    .nav-item:hover {
      background: rgba(255, 255, 255, 0.1);
      color: white;
    }

    .nav-item.active {
      background: rgba(255, 255, 255, 0.2);
      color: white;
    }

    .nav-icon {
      font-size: 20px;
      width: 24px;
      text-align: center;
    }

    .nav-label {
      margin-left: 12px;
      font-size: 14px;
    }

    .nav-badge {
      margin-left: auto;
      background: #ff5252;
      color: white;
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 12px;
    }

    .sidebar-footer {
      padding: 16px;
      border-top: 1px solid rgba(255, 255, 255, 0.1);
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .user-avatar {
      width: 40px;
      height: 40px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
    }

    .user-details {
      display: flex;
      flex-direction: column;
    }

    .user-name {
      font-weight: 500;
      font-size: 14px;
    }

    .user-role {
      font-size: 12px;
      opacity: 0.7;
    }

    .main-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }

    .top-header {
      background: white;
      padding: 16px 24px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    }

    .page-title {
      font-size: 20px;
      font-weight: 600;
      color: #1a237e;
      margin: 0;
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .status-indicator {
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }

    .status-indicator.online {
      background: #4caf50;
    }

    .status-text {
      font-size: 14px;
      color: #666;
    }

    .content-area {
      flex: 1;
      padding: 24px;
      overflow-y: auto;
    }
  `],
})
export class AdminLayoutComponent {
  sidebarCollapsed = false;

  navItems: NavItem[] = [
    { label: 'Dashboard', icon: '📊', route: '/dashboard' },
    { label: 'Users', icon: '👥', route: '/users' },
    { label: 'Tenants', icon: '🏢', route: '/tenants' },
    { label: 'System Health', icon: '💚', route: '/system-health' },
    { label: 'Audit Logs', icon: '📋', route: '/audit-logs' },
  ];

  toggleSidebar(): void {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }
}
