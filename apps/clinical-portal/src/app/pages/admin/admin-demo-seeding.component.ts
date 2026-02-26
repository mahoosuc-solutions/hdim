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
import {
  OperationRunDetail,
  OperationRunSummary,
  OperationsService,
  OperationSystemStatus,
  RunQueuedResponse,
  ValidationGate,
} from '../../services/operations.service';

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

      <section class="panel operations">
        <div class="panel-header">
          <h2>Operational Runs</h2>
          <span class="hint">Run stack, seeding, and validation jobs with persisted history.</span>
        </div>

        <div class="ops-controls">
          <button class="primary" type="button" (click)="runStackStart()" [disabled]="loading">
            Start Stack
          </button>
          <button class="ghost" type="button" (click)="runStackRestart()" [disabled]="loading">
            Restart Stack
          </button>
          <button class="danger" type="button" (click)="runStackStop()" [disabled]="loading">
            Stop Stack
          </button>
          <button class="ghost" type="button" (click)="runValidate()" [disabled]="loading">
            Validate System
          </button>
        </div>

        <div class="ops-controls seed-config">
          <label>
            Seed Profile
            <select [(ngModel)]="seedProfile">
              <option value="smoke">Smoke</option>
              <option value="full">Full</option>
            </select>
          </label>
          <label>
            Schedule Mode
            <select [(ngModel)]="seedScheduleMode">
              <option value="none">None</option>
              <option value="appointment-task">Appointment + Task</option>
              <option value="encounter">Encounter</option>
              <option value="both">Both</option>
            </select>
          </label>
          <button class="primary" type="button" (click)="runSeed()" [disabled]="loading">
            Run Seed Job
          </button>
        </div>

        <div class="status-row">
          <div>
            <p class="label">Running Jobs</p>
            <p class="value">{{ operationsStatus?.runningCount ?? 0 }}</p>
          </div>
          <div>
            <p class="label">Latest Validation</p>
            <p class="value">{{ operationsStatus?.latestValidate?.status || '—' }}</p>
          </div>
          <div>
            <p class="label">Latest Full Seed</p>
            <p class="value">{{ operationsStatus?.latestSeedFull?.status || '—' }}</p>
          </div>
          <div>
            <p class="label">Latest Stack Start</p>
            <p class="value">{{ operationsStatus?.latestStackStart?.status || '—' }}</p>
          </div>
        </div>

        <div class="run-history">
          <h3>Recent Runs</h3>
          @if (operationRuns.length === 0) {
            <p class="hint">No runs recorded yet.</p>
          } @else {
            <div class="run-list">
              @for (run of operationRuns; track run.id) {
                <button
                  type="button"
                  class="run-item"
                  [class.active]="selectedRunId === run.id"
                  (click)="selectRun(run.id)"
                >
                  <span>{{ run.operationType }}</span>
                  <span>{{ run.status }}</span>
                  <span>{{ run.requestedAt | date:'short' }}</span>
                </button>
              }
            </div>
          }

          @if (selectedRun) {
            <div class="run-detail">
              <h4>Run Detail</h4>
              <p class="summary">{{ selectedRun.run.summary || 'No summary.' }}</p>
              <p class="hint">Requested by {{ selectedRun.run.requestedBy }} at {{ selectedRun.run.requestedAt | date:'short' }}</p>
              @if (canCancelSelectedRun()) {
                <button type="button" class="danger" (click)="cancelSelectedRun()" [disabled]="loading">
                  Cancel Run
                </button>
              }

              @if (selectedRun.run.validation) {
                <div class="validation-card">
                  <div class="validation-header">
                    <h5>Readiness Scorecard</h5>
                    <span class="pill" [class.ready]="selectedRun.run.validation.passed">
                      {{ selectedRun.run.validation.grade }} / {{ selectedRun.run.validation.score }}
                    </span>
                  </div>
                  <p class="hint">
                    Critical: {{ selectedRun.run.validation.criticalPass ? 'PASS' : 'FAIL' }} |
                    Overall: {{ selectedRun.run.validation.passed ? 'PASS' : 'FAIL' }}
                  </p>

                  <label class="validation-filter">
                    <input type="checkbox" [(ngModel)]="showFailedGatesOnly" />
                    Show failed gates only
                  </label>

                  <div class="gate-list">
                    @for (gate of visibleValidationGates(); track gate.gateKey) {
                      <article class="gate-item" [class.fail]="gate.status === 'FAIL'">
                        <div class="gate-main">
                          <strong>{{ gate.gateName }}</strong>
                          <span>{{ gate.status }} · {{ gate.weight }} pts{{ gate.critical ? ' · critical' : '' }}</span>
                        </div>
                        @if (gate.evidenceText) {
                          <p>{{ gate.evidenceText }}</p>
                        }
                      </article>
                    }
                  </div>
                </div>
              }

              <div class="scenario-list">
                @for (step of selectedRun.steps; track step.id) {
                  <article class="scenario-card">
                    <div>
                      <h3>{{ step.stepOrder }}. {{ step.stepName }}</h3>
                      <p>{{ step.status }}{{ step.message ? ' - ' + step.message : '' }}</p>
                    </div>
                    @if (step.output) {
                      <pre class="step-output">{{ step.output }}</pre>
                    }
                  </article>
                }
              </div>
            </div>
          }
        </div>
      </section>

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

      .operations {
        margin-top: 24px;
      }

      .ops-controls {
        display: flex;
        gap: 12px;
        flex-wrap: wrap;
        margin-bottom: 14px;
      }

      .seed-config {
        align-items: end;
      }

      .run-history {
        margin-top: 18px;
      }

      .run-list {
        display: grid;
        gap: 8px;
      }

      .run-item {
        display: grid;
        grid-template-columns: 1fr auto auto;
        gap: 10px;
        text-align: left;
        border: 1px solid #e2e8f0;
        background: #f8fafc;
      }

      .run-item.active {
        border-color: #4338ca;
        background: #eef2ff;
      }

      .run-detail {
        margin-top: 14px;
      }

      .summary {
        margin: 8px 0;
        color: #334155;
      }

      .step-output {
        margin: 0;
        background: #0f172a;
        color: #e2e8f0;
        border-radius: 10px;
        padding: 10px;
        max-height: 180px;
        overflow: auto;
        font-size: 12px;
        white-space: pre-wrap;
      }

      .validation-card {
        margin: 14px 0;
        border: 1px solid #dbeafe;
        background: #f8fbff;
        border-radius: 12px;
        padding: 12px;
      }

      .validation-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 6px;
      }

      .validation-header h5 {
        margin: 0;
        font-size: 14px;
      }

      .validation-filter {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 10px 0;
        font-size: 12px;
      }

      .validation-filter input {
        margin: 0;
      }

      .gate-list {
        display: grid;
        gap: 8px;
      }

      .gate-item {
        border: 1px solid #d1fae5;
        border-radius: 10px;
        padding: 8px 10px;
        background: #f0fdf4;
      }

      .gate-item.fail {
        border-color: #fecaca;
        background: #fef2f2;
      }

      .gate-main {
        display: flex;
        justify-content: space-between;
        gap: 10px;
        align-items: center;
      }

      .gate-main span {
        font-size: 12px;
        color: #64748b;
      }

      .gate-item p {
        margin: 6px 0 0;
        font-size: 12px;
        color: #475569;
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
  seedProfile: 'smoke' | 'full' = 'smoke';
  seedScheduleMode: 'none' | 'appointment-task' | 'encounter' | 'both' = 'none';
  operationsStatus: OperationSystemStatus | null = null;
  operationRuns: OperationRunSummary[] = [];
  selectedRun: OperationRunDetail | null = null;
  selectedRunId: string | null = null;
  showFailedGatesOnly = false;
  loading = false;
  errorMessage = '';
  actionMessage = '';

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly demoSeedingService: DemoSeedingService,
    private readonly operationsService: OperationsService
  ) {}

  ngOnInit(): void {
    this.refreshAll();
    timer(0, 5000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.refreshStatus();
        this.refreshOperations();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  refreshAll(): void {
    this.refreshStatus();
    this.refreshOperations();
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

  refreshOperations(): void {
    this.operationsService.getSystemStatus().subscribe({
      next: (status) => {
        this.operationsStatus = status;
      },
      error: (error) => {
        this.errorMessage = this.formatError(error);
      },
    });

    this.operationsService.listRuns(25).subscribe({
      next: (runs) => {
        this.operationRuns = runs;
        if (this.selectedRunId && !runs.some((run) => run.id === this.selectedRunId)) {
          this.selectedRunId = null;
          this.selectedRun = null;
        }
      },
      error: (error) => {
        this.errorMessage = this.formatError(error);
      },
    });
  }

  selectRun(runId: string): void {
    this.selectedRunId = runId;
    this.operationsService.getRun(runId).subscribe({
      next: (detail) => {
        this.selectedRun = detail;
      },
      error: (error) => {
        this.errorMessage = this.formatError(error);
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

  runStackStart(): void {
    this.runOperation(this.operationsService.startStack(), 'Stack start requested.');
  }

  runStackStop(): void {
    this.runOperation(this.operationsService.stopStack(), 'Stack stop requested.');
  }

  runStackRestart(): void {
    this.runOperation(this.operationsService.restartStack(), 'Stack restart requested.');
  }

  runSeed(): void {
    this.runOperation(
      this.operationsService.runSeed({
        profile: this.seedProfile,
        scheduleMode: this.seedScheduleMode,
      }),
      `Seed job requested (${this.seedProfile}, ${this.seedScheduleMode}).`
    );
  }

  runValidate(): void {
    this.runOperation(this.operationsService.runValidate(), 'Validation requested.');
  }

  canCancelSelectedRun(): boolean {
    const status = this.selectedRun?.run?.status;
    return status === 'QUEUED' || status === 'RUNNING';
  }

  cancelSelectedRun(): void {
    if (!this.selectedRun?.run?.id) return;
    this.runOperation(
      this.operationsService.cancelRun(this.selectedRun.run.id),
      'Cancellation requested.'
    );
  }

  visibleValidationGates(): ValidationGate[] {
    const gates = this.selectedRun?.run?.validation?.gates ?? [];
    if (!this.showFailedGatesOnly) {
      return gates;
    }
    return gates.filter((gate) => gate.status === 'FAIL');
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

  private runOperation(action$: any, message: string): void {
    this.loading = true;
    this.errorMessage = '';
    this.actionMessage = '';

    action$
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (response: RunQueuedResponse) => {
          this.actionMessage = response?.message || message;
          this.refreshOperations();
          if (response?.runId) {
            this.selectRun(response.runId);
          }
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
