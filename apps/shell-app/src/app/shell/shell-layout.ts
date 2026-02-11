import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ConnectionStatusComponent } from '../components/connection-status.component';

@Component({
  selector: 'health-platform-shell-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, ConnectionStatusComponent],
  template: `
    <div class="shell-layout">
      <header class="shell-header">
        <div class="shell-header-content">
          <h1 class="shell-title">Health Data Platform</h1>
          <nav class="shell-nav">
            <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">Home</a>
            <a routerLink="/mfePatients" routerLinkActive="active">Patients</a>
            <a routerLink="/mfeMeasureBuilder" routerLinkActive="active">Measure Builder</a>
          </nav>
          <app-connection-status></app-connection-status>
        </div>
      </header>
      <main class="shell-main">
        <router-outlet></router-outlet>
      </main>
      <footer class="shell-footer">
        <p>&copy; 2025 Health Data Platform - Micro Frontend Architecture</p>
      </footer>
    </div>
  `,
  styles: [`
    .shell-layout {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }

    .shell-header {
      background: #1976d2;
      color: white;
      padding: 1rem 2rem;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }

    .shell-header-content {
      max-width: 1200px;
      margin: 0 auto;
      width: 100%;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 2rem;
    }

    .shell-title {
      margin: 0;
      font-size: 1.5rem;
      font-weight: 500;
      flex-shrink: 0;
    }

    .shell-nav {
      display: flex;
      gap: 2rem;
      flex: 1;
    }

    app-connection-status {
      flex-shrink: 0;
      margin-left: auto;
    }

    .shell-nav a {
      color: white;
      text-decoration: none;
      padding: 0.5rem 1rem;
      border-radius: 4px;
      transition: background 0.2s;
    }

    .shell-nav a:hover {
      background: rgba(255,255,255,0.1);
    }

    .shell-nav a.active {
      background: rgba(255,255,255,0.2);
    }

    .shell-main {
      flex: 1;
      max-width: 1200px;
      width: 100%;
      margin: 0 auto;
      padding: 2rem;
    }

    .shell-footer {
      background: #f5f5f5;
      padding: 1rem 2rem;
      text-align: center;
      border-top: 1px solid #e0e0e0;
    }

    .shell-footer p {
      margin: 0;
      color: #666;
      font-size: 0.875rem;
    }
  `]
})
export class ShellLayoutComponent {}
