import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, timer } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import {
  DemoActionResponse,
  DemoProgressResponse,
  DemoScenarioResponse,
  DemoSeedingService,
  DemoStatusResponse,
} from '../../services/demo-seeding.service';

@Component({
  selector: 'app-admin-demo-seeding',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="demo-seeding">
      <header class="hero">
        <div>
          <p class="eyebrow">Demo Operations</p>
          <h1>Seeding Control Center</h1>
          <p class="subhead">
            Monitor live seeding progress, switch scenarios, and reset seeded data without leaving the portal.
          </p>
        </div>
        <div class="hero-actions">
          <button class="ghost" type="button" (click)="refreshAll()" [disabled]="loading">
            Refresh
          </button>
          <button class="danger" type="button" (click)="resetCurrentTenant()" [disabled]="loading">
            Clear Seeded Data
          </button>
        </div>
      </header>

      <div class="grid">
        <section class="panel status">
          <div class="panel-header">
            <h2>Live Status</h2>
            <span class="pill" [class.ready]="status?.ready">{{ status?.ready ? 'Ready' : 'Busy' }}</span>
          </div>

          <div class="status-row">
            <div>
              <p class="label">Current Scenario</p>
              <p class="value">{{ status?.currentScenario || 'None' }}</p>
            </div>
            <div>
              <p class="label">Session</p>
              <p class="value">{{ status?.currentSessionId || '—' }}</p>
            </div>
          </div>

          <div class="status-row">
            <div>
              <p class="label">Session Status</p>
              <p class="value">{{ status?.sessionStatus || '—' }}</p>
            </div>
            <div>
              <p class="label">Scenarios</p>
              <p class="value">{{ status?.scenarioCount ?? 0 }}</p>
            </div>
          </div>

          <div class="progress">
            <div class="progress-meta">
              <span>{{ progress?.stage || 'Idle' }}</span>
              <span>{{ progress?.progressPercent ?? 0 }}%</span>
            </div>
            <div class="bar">
              <div class="fill" [style.width.%]="progress?.progressPercent ?? 0"></div>
            </div>
            <p class="progress-message">{{ progress?.message || 'Waiting for activity…' }}</p>
          </div>

          <div class="metrics">
            <div>
              <p class="label">Patients Generated</p>
              <p class="value">{{ progress?.patientsGenerated ?? '—' }}</p>
            </div>
            <div>
              <p class="label">Patients Persisted</p>
              <p class="value">{{ progress?.patientsPersisted ?? '—' }}</p>
            </div>
            <div>
              <p class="label">Care Gaps Created</p>
              <p class="value">{{ progress?.careGapsCreated ?? '—' }}</p>
            </div>
            <div>
              <p class="label">Measures Seeded</p>
              <p class="value">{{ progress?.measuresSeeded ?? '—' }}</p>
            </div>
          </div>

          <div class="session-actions">
            <button type="button" class="ghost" (click)="reloadScenario()" [disabled]="loading">
              Reload Current Scenario
            </button>
            <button type="button" class="ghost" (click)="cancelCurrent()" [disabled]="loading">
              Cancel Load
            </button>
            <button type="button" class="danger" (click)="stopCurrent()" [disabled]="loading">
              Stop Session
            </button>
          </div>
        </section>

        <section class="panel scenarios">
          <div class="panel-header">
            <h2>Scenarios</h2>
            <span class="hint">Choose a scenario and start seeding instantly.</span>
          </div>

          <div class="scenario-picker">
            <label>
              Scenario
              <select [(ngModel)]="selectedScenario">
                <option value="" disabled>Select scenario</option>
                @for (scenario of scenarios; track scenario.name) {
                  <option [value]="scenario.name">{{ scenario.displayName || scenario.name }}</option>
                }
              </select>
            </label>
            <button type="button" class="primary" (click)="loadSelectedScenario()" [disabled]="!selectedScenario || loading">
              Start Seeding
            </button>
          </div>

          <div class="scenario-list">
            @for (scenario of scenarios; track scenario.name) {
              <article class="scenario-card" [class.active]="scenario.name === status?.currentScenario">
                <div>
                  <h3>{{ scenario.displayName || scenario.name }}</h3>
                  <p>{{ scenario.description || 'No description available.' }}</p>
                </div>
                <div class="scenario-meta">
                  <span>Type: {{ scenario.scenarioType || 'custom' }}</span>
                  <span>Patients: {{ scenario.patientCount ?? '—' }}</span>
                  <span>Tenant: {{ scenario.tenantId || 'default' }}</span>
                </div>
                <button type="button" class="ghost" (click)="loadScenario(scenario.name)" [disabled]="loading">
                  Seed This Scenario
                </button>
              </article>
            }
          </div>
        </section>
      </div>

      @if (actionMessage) {
        <div class="toast success">{{ actionMessage }}</div>
      }

      @if (errorMessage) {
        <div class="toast error">{{ errorMessage }}</div>
      }
    </section>
  `,
  styles: [
    `
      :host {
        display: block;
        color: #0e1216;
      }

      .demo-seeding {
        padding: 28px;
        min-height: 100vh;
        background: radial-gradient(circle at top left, #f8efe3, #f3f5ff 45%, #eef7f4 90%);
      }

      .hero {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 24px;
        padding: 24px 28px;
        border-radius: 24px;
        background: linear-gradient(135deg, #111827, #1f2937);
        color: #f9fafb;
        box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25);
      }

      .eyebrow {
        text-transform: uppercase;
        letter-spacing: 0.2em;
        font-size: 11px;
        color: #cbd5f5;
        margin-bottom: 8px;
      }

      h1 {
        margin: 0 0 6px;
        font-size: 30px;
        font-weight: 700;
      }

      .subhead {
        margin: 0;
        max-width: 520px;
        color: #dbe2f5;
        line-height: 1.5;
      }

      .hero-actions {
        display: flex;
        gap: 12px;
        flex-wrap: wrap;
      }

      .grid {
        margin-top: 28px;
        display: grid;
        gap: 24px;
        grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      }

      .panel {
        background: #ffffff;
        border-radius: 20px;
        padding: 22px;
        box-shadow: 0 18px 50px rgba(15, 23, 42, 0.08);
      }

      .panel-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        margin-bottom: 18px;
      }

      .panel-header h2 {
        margin: 0;
        font-size: 20px;
      }

      .hint {
        color: #6b7280;
        font-size: 13px;
      }

      .pill {
        padding: 4px 10px;
        border-radius: 999px;
        background: #f3f4f6;
        color: #6b7280;
        font-size: 12px;
        font-weight: 600;
      }

      .pill.ready {
        background: #dcfce7;
        color: #166534;
      }

      .status-row {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
        gap: 16px;
        margin-bottom: 16px;
      }

      .label {
        text-transform: uppercase;
        letter-spacing: 0.12em;
        font-size: 10px;
        color: #94a3b8;
        margin: 0 0 6px;
      }

      .value {
        font-size: 16px;
        margin: 0;
        font-weight: 600;
      }

      .progress {
        margin: 20px 0;
        padding: 14px;
        border-radius: 14px;
        background: #f8fafc;
      }

      .progress-meta {
        display: flex;
        justify-content: space-between;
        font-size: 13px;
        color: #475569;
        margin-bottom: 10px;
      }

      .bar {
        height: 10px;
        border-radius: 999px;
        background: #e2e8f0;
        overflow: hidden;
      }

      .fill {
        height: 100%;
        background: linear-gradient(90deg, #4f46e5, #22c55e);
        transition: width 0.4s ease;
      }

      .progress-message {
        margin: 10px 0 0;
        color: #6b7280;
        font-size: 13px;
      }

      .metrics {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
        gap: 16px;
      }

      .session-actions {
        margin-top: 20px;
        display: flex;
        gap: 12px;
        flex-wrap: wrap;
      }

      .scenario-picker {
        display: grid;
        grid-template-columns: 1fr auto;
        gap: 16px;
        align-items: end;
        margin-bottom: 18px;
      }

      label {
        display: grid;
        gap: 8px;
        font-size: 13px;
        color: #475569;
      }

      select {
        padding: 10px 12px;
        border-radius: 12px;
        border: 1px solid #e2e8f0;
        font-size: 14px;
        background: #fff;
      }

      .scenario-list {
        display: grid;
        gap: 14px;
      }

      .scenario-card {
        border-radius: 16px;
        border: 1px solid #e5e7eb;
        padding: 16px;
        background: #ffffff;
        display: grid;
        gap: 12px;
      }

      .scenario-card.active {
        border-color: #4338ca;
        box-shadow: 0 10px 30px rgba(67, 56, 202, 0.12);
      }

      .scenario-card h3 {
        margin: 0 0 6px;
        font-size: 16px;
      }

      .scenario-card p {
        margin: 0;
        font-size: 13px;
        color: #6b7280;
      }

      .scenario-meta {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
        font-size: 12px;
        color: #475569;
      }

      button {
        border: none;
        border-radius: 12px;
        padding: 10px 16px;
        font-weight: 600;
        cursor: pointer;
      }

      button.primary {
        background: #4f46e5;
        color: #fff;
      }

      button.ghost {
        background: #f1f5f9;
        color: #1f2937;
      }

      button.danger {
        background: #ef4444;
        color: #fff;
      }

      button:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }

      .toast {
        margin-top: 18px;
        padding: 12px 16px;
        border-radius: 12px;
        font-weight: 600;
      }

      .toast.success {
        background: #ecfdf3;
        color: #166534;
      }

      .toast.error {
        background: #fee2e2;
        color: #991b1b;
      }

      @media (max-width: 720px) {
        .hero {
          flex-direction: column;
        }

        .scenario-picker {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class AdminDemoSeedingComponent implements OnInit, OnDestroy {
  status: DemoStatusResponse | null = null;
  progress: DemoProgressResponse | null = null;
  scenarios: DemoScenarioResponse[] = [];
  selectedScenario = '';
  loading = false;
  errorMessage = '';
  actionMessage = '';

  private readonly destroy$ = new Subject<void>();

  constructor(private readonly demoSeedingService: DemoSeedingService) {}

  ngOnInit(): void {
    this.refreshAll();
    timer(0, 5000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.refreshStatus();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  refreshAll(): void {
    this.refreshStatus();
    this.demoSeedingService.listScenarios().subscribe({
      next: (scenarios) => {
        this.scenarios = scenarios;
        if (!this.selectedScenario && scenarios.length > 0) {
          this.selectedScenario = scenarios[0].name;
        }
      },
      error: (error) => {
        this.errorMessage = this.formatError(error);
      },
    });
  }

  refreshStatus(): void {
    this.demoSeedingService.getStatus().subscribe({
      next: (status) => {
        this.status = status;
      },
      error: (error) => {
        this.errorMessage = this.formatError(error);
      },
    });

    this.demoSeedingService.getProgress().subscribe({
      next: (progress) => {
        this.progress = progress;
      },
      error: () => {
        // Ignore missing progress when no session exists
      },
    });
  }

  loadSelectedScenario(): void {
    if (!this.selectedScenario) return;
    this.loadScenario(this.selectedScenario);
  }

  loadScenario(name: string): void {
    this.runAction(this.demoSeedingService.loadScenario(name), `Scenario ${name} requested.`);
  }

  reloadScenario(): void {
    this.runAction(this.demoSeedingService.reloadScenario(), 'Reload requested.');
  }

  cancelCurrent(): void {
    this.runAction(this.demoSeedingService.cancelCurrent(), 'Cancellation requested.');
  }

  stopCurrent(): void {
    this.runAction(this.demoSeedingService.stopCurrent(), 'Session stop requested.');
  }

  resetCurrentTenant(): void {
    this.runAction(this.demoSeedingService.resetCurrentTenant(), 'Resetting demo data for current tenant.');
  }

  private runAction(action$: any, message: string): void {
    this.loading = true;
    this.errorMessage = '';
    this.actionMessage = '';

    action$
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (response: DemoActionResponse) => {
          this.actionMessage = response?.errorMessage ? response.errorMessage : message;
          this.refreshStatus();
        },
        error: (error: any) => {
          this.errorMessage = this.formatError(error);
        },
      });
  }

  private formatError(error: any): string {
    if (!error) return 'Unknown error.';
    if (error.error?.errorMessage) return error.error.errorMessage;
    if (typeof error.error === 'string') return error.error;
    return error.message || 'Request failed.';
  }
}
