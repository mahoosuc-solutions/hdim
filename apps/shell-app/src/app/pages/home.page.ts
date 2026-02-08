import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  loadCurrentUser,
  selectIsAuthenticated,
  selectSharedAuthError,
  selectSharedAuthLoading,
  selectSharedAuthTenantId,
  selectSharedAuthUser,
} from '@health-platform/shared/state';
import type { User } from '@health-platform/shared/util-auth';
import { getUserDisplayName } from '@health-platform/shared/util-auth';
import { ClinicalEventType, EventBusService } from '@health-platform/shared/data-access';

@Component({
  selector: 'health-platform-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="home-container">
      <div class="hero">
        <h1>Welcome to Health Data Platform</h1>
        <p class="subtitle">Modular Micro Frontend Architecture</p>
        <section class="auth-panel">
          <div class="auth-panel__header">
            <h3>Authentication Status</h3>
            <span
              class="status-pill"
              [class.is-online]="(isAuthenticated$ | async)"
            >
              {{ (isAuthenticated$ | async) ? 'Authenticated' : 'Anonymous' }}
            </span>
          </div>

          <div
            class="auth-panel__body"
            *ngIf="(user$ | async) as user; else anonymousState"
          >
            <p class="auth-panel__name">{{ getUserDisplayName(user) }}</p>
            <p class="auth-panel__meta">Tenant: {{ (tenantId$ | async) || 'detecting…' }}</p>
            <div class="auth-panel__roles" *ngIf="user.roles.length">
              <span class="role-chip" *ngFor="let role of user.roles">{{ role.name }}</span>
            </div>
          </div>

          <ng-template #anonymousState>
            <p class="auth-panel__empty" *ngIf="!(loading$ | async); else loadingState">
              No active session detected. Secure routes will redirect to the login experience automatically.
            </p>
          </ng-template>

          <ng-template #loadingState>
            <p class="auth-panel__empty">Syncing session…</p>
          </ng-template>

          <p class="auth-panel__error" *ngIf="(error$ | async) as error">
            {{ error }}
          </p>
        </section>
      </div>

      <div class="features">
        <div class="feature-card">
          <h2>🏥 Patient Management</h2>
          <p>Comprehensive patient data management with real-time updates</p>
          <a routerLink="/mfePatients" class="btn-primary">Access Patients Module</a>
        </div>

        <div class="feature-card">
          <h2>🧮 Measure Builder</h2>
          <p>Design, test, and publish custom measures with FHIR-driven inputs</p>
          <a routerLink="/mfeMeasureBuilder" class="btn-primary">Open Measure Builder</a>
        </div>

        <div class="feature-card feature-card--diagnostics">
          <h2>🧪 MFE Diagnostics</h2>
          <p>Broadcast a patient context and verify MFE event flow</p>
          <div class="diagnostic-actions">
            <button class="btn-secondary btn-secondary--active" (click)="broadcastPatient('demo-patient-001')">Select Demo Patient</button>
            <button class="btn-secondary btn-secondary--active" (click)="clearPatient()">Clear Patient</button>
          </div>
          <p class="diagnostic-status">
            Current Patient: {{ (currentPatientId$ | async) || 'none' }}
          </p>
        </div>

        <div class="feature-card">
          <h2>📊 Analytics</h2>
          <p>Advanced analytics and reporting capabilities</p>
          <button class="btn-secondary" disabled>Coming Soon</button>
        </div>

        <div class="feature-card">
          <h2>🔒 Security</h2>
          <p>HIPAA-compliant security with role-based access control</p>
          <button class="btn-secondary" disabled>Coming Soon</button>
        </div>
      </div>

      <div class="architecture-info">
        <h2>Micro Frontend Architecture</h2>
        <ul>
          <li>✅ Module Federation for dynamic loading</li>
          <li>✅ Shared libraries for common functionality</li>
          <li>✅ Independent deployment of each module</li>
          <li>✅ Centralized authentication and tenant management</li>
          <li>🔄 Incremental migration from monolith</li>
        </ul>
      </div>
    </div>
  `,
  styles: [`
    .home-container {
      padding: 2rem 0;
    }

    .hero {
      text-align: center;
      margin-bottom: 3rem;
      padding: 3rem 2rem;
      background: linear-gradient(135deg, #222a68 0%, #1d8cf8 100%);
      color: white;
      border-radius: 16px;
      position: relative;
      overflow: hidden;
    }

    .hero::after {
      content: '';
      position: absolute;
      inset: 0;
      background: radial-gradient(circle at top right, rgba(255,255,255,0.25), transparent 45%);
      pointer-events: none;
    }

    .hero h1 {
      font-size: 2.5rem;
      margin: 0 0 1rem 0;
    }

    .subtitle {
      font-size: 1.25rem;
      opacity: 0.9;
      margin: 0 0 2rem;
    }

    .auth-panel {
      text-align: left;
      background: rgba(255, 255, 255, 0.1);
      padding: 1.5rem;
      border-radius: 12px;
      border: 1px solid rgba(255, 255, 255, 0.2);
      backdrop-filter: blur(6px);
      max-width: 640px;
      margin: 0 auto;
    }

    .auth-panel__header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
    }

    .auth-panel__header h3 {
      margin: 0;
      font-size: 1.1rem;
      letter-spacing: 0.05em;
      text-transform: uppercase;
    }

    .status-pill {
      padding: 0.25rem 0.75rem;
      border-radius: 999px;
      font-size: 0.85rem;
      background: rgba(255, 255, 255, 0.2);
      border: 1px solid rgba(255, 255, 255, 0.4);
    }

    .status-pill.is-online {
      background: rgba(76, 217, 100, 0.2);
      border-color: rgba(76, 217, 100, 0.6);
    }

    .auth-panel__name {
      font-size: 1.4rem;
      margin: 0 0 0.25rem;
      font-weight: 600;
    }

    .auth-panel__meta {
      margin: 0.1rem 0;
      font-size: 0.95rem;
      opacity: 0.85;
    }

    .auth-panel__roles {
      display: flex;
      flex-wrap: wrap;
      gap: 0.4rem;
      margin-top: 0.75rem;
    }

    .role-chip {
      background: rgba(255, 255, 255, 0.15);
      border: 1px solid rgba(255, 255, 255, 0.3);
      border-radius: 999px;
      padding: 0.25rem 0.85rem;
      font-size: 0.85rem;
    }

    .auth-panel__empty,
    .auth-panel__error {
      margin: 0;
      font-size: 0.95rem;
    }

    .auth-panel__error {
      margin-top: 0.75rem;
      color: #ffb3b3;
    }

    .features {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 2rem;
      margin-bottom: 3rem;
    }

    .feature-card {
      background: white;
      padding: 2rem;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .feature-card h2 {
      margin: 0;
      font-size: 1.5rem;
      color: #333;
    }

    .feature-card p {
      margin: 0;
      color: #666;
      flex: 1;
    }

    .btn-primary, .btn-secondary {
      padding: 0.75rem 1.5rem;
      border-radius: 4px;
      border: none;
      font-size: 1rem;
      cursor: pointer;
      text-decoration: none;
      display: inline-block;
      text-align: center;
      transition: all 0.2s;
    }

    .btn-primary {
      background: #1976d2;
      color: white;
    }

    .btn-primary:hover {
      background: #1565c0;
    }

    .btn-secondary {
      background: #e0e0e0;
      color: #999;
      cursor: not-allowed;
    }

    .btn-secondary--active {
      color: #263238;
      cursor: pointer;
    }

    .feature-card--diagnostics {
      border: 1px dashed #7f8fa4;
      background: #f7f9fc;
    }

    .diagnostic-actions {
      display: flex;
      gap: 0.75rem;
      flex-wrap: wrap;
    }

    .diagnostic-status {
      margin: 0;
      font-size: 0.95rem;
      color: #52657a;
    }

    .architecture-info {
      background: #0d1117;
      color: #f0f3ff;
      padding: 2rem;
      border-radius: 16px;
      border: 1px solid rgba(255, 255, 255, 0.08);
      box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.05);
    }

    .architecture-info h2 {
      margin-top: 0;
      color: #8ab4ff;
    }

    .architecture-info ul {
      list-style: none;
      padding: 0;
      margin: 0;
    }

    .architecture-info li {
      padding: 0.5rem 0;
      font-size: 1.1rem;
    }
  `]
})
export class HomePage implements OnInit {
  readonly user$: Observable<User | null> = this.store.select(selectSharedAuthUser);
  readonly tenantId$: Observable<string | null> = this.store.select(selectSharedAuthTenantId);
  readonly loading$: Observable<boolean> = this.store.select(selectSharedAuthLoading);
  readonly error$: Observable<string | null> = this.store.select(selectSharedAuthError);
  readonly isAuthenticated$: Observable<boolean> = this.store.select(selectIsAuthenticated);
  readonly currentPatientId$: Observable<string | null> = this.eventBus.currentPatient$.pipe(
    map((context) => context.patientId)
  );

  constructor(private readonly store: Store, private readonly eventBus: EventBusService) {}

  ngOnInit(): void {
    this.store.dispatch(loadCurrentUser());
  }

  /**
   * Get user display name from User object
   */
  getUserDisplayName(user: User): string {
    return getUserDisplayName(user);
  }

  broadcastPatient(patientId: string): void {
    this.eventBus.emit({
      type: ClinicalEventType.PATIENT_SELECTED,
      source: 'shell-app',
      timestamp: Date.now(),
      data: { patientId, tenantId: 'demo' },
    });
  }

  clearPatient(): void {
    this.eventBus.clearPatientContext();
  }
}
