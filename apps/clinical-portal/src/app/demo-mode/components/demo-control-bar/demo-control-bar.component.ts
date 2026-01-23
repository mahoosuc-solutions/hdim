import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDividerModule } from '@angular/material/divider';
import { LoggerService } from '../../../services/logger.service';
import { DemoModeService } from '../../services/demo-mode.service';
import { DemoSeedingDataFlowComponent } from '../demo-seeding-data-flow/demo-seeding-data-flow.component';

/**
 * Demo Control Bar Component
 *
 * Displayed at the top of the screen when demo mode is active.
 * Provides controls for:
 * - Recording timer (start/stop/reset)
 * - Scenario information
 * - Quick actions (reset, snapshot)
 * - Exit demo mode
 */
@Component({
  selector: 'app-demo-control-bar',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatDividerModule,
  ],
  template: `
    <div class="demo-control-bar" *ngIf="demoService.isDemoMode()">
      <div class="demo-bar-content">
        <!-- Demo Mode Indicator -->
        <div class="demo-indicator">
          <mat-icon class="demo-icon">videocam</mat-icon>
          <span class="demo-label">DEMO MODE</span>
        </div>

        <!-- Scenario Info -->
        <div class="scenario-info" *ngIf="demoService.activeScenario() as scenario">
          <mat-chip-listbox>
            <mat-chip-option selected disabled>
              {{ scenario.displayName || scenario.name }}
            </mat-chip-option>
          </mat-chip-listbox>
        </div>

        <!-- Session Status -->
        <div class="scenario-info" *ngIf="demoService.status() as status">
          <mat-chip-listbox>
            <mat-chip-option selected disabled>
              {{ status.sessionStatus || 'IDLE' }}
            </mat-chip-option>
          </mat-chip-listbox>
        </div>

        <!-- Last Load Results -->
        <div class="scenario-info" *ngIf="demoService.lastLoadResult() as result">
          <span class="load-summary">
            {{ result.patientCount }} patients • {{ result.careGapCount }} care gaps • {{ result.loadTimeMs }}ms
          </span>
        </div>

        <!-- Progress -->
        <div class="scenario-info progress-info" *ngIf="demoService.progress() as progress">
          <div class="progress-text">
            <span class="progress-stage">{{ progress.stage }}</span>
            <span class="progress-message" *ngIf="progress.message">{{ progress.message }}</span>
            <span class="progress-message" *ngIf="progress.cancelRequested">Cancel requested</span>
          </div>
          <mat-progress-bar
            mode="determinate"
            [value]="progress.progressPercent"
            class="demo-progress-bar"
          ></mat-progress-bar>
          <span class="progress-metrics">
            {{ progress.patientsGenerated || 0 }} patients • {{ progress.careGapsCreated || 0 }} care gaps
          </span>
          <span class="progress-metrics" *ngIf="progress.patientsPersisted || progress.measuresSeeded">
            {{ progress.patientsPersisted || 0 }} persisted • {{ progress.measuresSeeded || 0 }} measures
          </span>
          <span class="progress-metrics" *ngIf="progress.updatedAt">
            Updated {{ formatTimestamp(progress.updatedAt) }}
          </span>
          <span class="progress-metrics" *ngIf="progress.tenantId">
            Tenant {{ progress.tenantId }}
          </span>
          <span class="progress-metrics" *ngIf="progress.sessionId">
            Session {{ progress.sessionId }}
          </span>
        </div>

        <!-- Recording Controls -->
        <div class="recording-controls">
          <div class="recording-timer" [class.recording]="demoService.isRecording()">
            <mat-icon *ngIf="demoService.isRecording()" class="recording-dot">fiber_manual_record</mat-icon>
            <span class="timer-display">{{ demoService.formattedRecordingTime() }}</span>
          </div>

          <button
            mat-icon-button
            *ngIf="!demoService.isRecording()"
            (click)="demoService.startRecording()"
            matTooltip="Start Recording Timer"
          >
            <mat-icon>play_arrow</mat-icon>
          </button>

          <button
            mat-icon-button
            *ngIf="demoService.isRecording()"
            (click)="demoService.stopRecording()"
            matTooltip="Stop Recording Timer"
          >
            <mat-icon>stop</mat-icon>
          </button>

          <button
            mat-icon-button
            (click)="demoService.resetRecording()"
            matTooltip="Reset Timer"
            [disabled]="demoService.isRecording()"
          >
            <mat-icon>replay</mat-icon>
          </button>
        </div>

        <!-- Quick Actions -->
        <div class="quick-actions">
          <button mat-icon-button [matMenuTriggerFor]="scenarioMenu" matTooltip="Load Scenario">
            <mat-icon>movie</mat-icon>
          </button>
          <mat-menu #scenarioMenu="matMenu">
            <button
              mat-menu-item
              *ngFor="let scenario of scenarioOptions"
              (click)="loadScenario(scenario.name)"
            >
              <mat-icon>{{ scenario.icon }}</mat-icon>
              <span>{{ scenario.label }}</span>
            </button>
          </mat-menu>

          <button mat-icon-button [matMenuTriggerFor]="actionsMenu" matTooltip="Actions">
            <mat-icon>more_vert</mat-icon>
          </button>
          <mat-menu #actionsMenu="matMenu">
            <button mat-menu-item (click)="cancelLoad()" [disabled]="!canCancelLoad()">
              <mat-icon>cancel</mat-icon>
              <span>Cancel Load</span>
            </button>
            <button mat-menu-item (click)="stopSession()" [disabled]="!demoService.status()">
              <mat-icon>stop_circle</mat-icon>
              <span>Stop Session</span>
            </button>
            <mat-divider></mat-divider>
            <button mat-menu-item (click)="createSnapshot()">
              <mat-icon>photo_camera</mat-icon>
              <span>Create Snapshot</span>
            </button>
            <button mat-menu-item (click)="resetCurrentTenant()">
              <mat-icon>delete_sweep</mat-icon>
              <span>Reset Current Scenario</span>
            </button>
            <button mat-menu-item (click)="resetDemo()">
              <mat-icon>refresh</mat-icon>
              <span>Reset Demo Data</span>
            </button>
            <mat-divider></mat-divider>
            <button mat-menu-item (click)="toggleTooltips()">
              <mat-icon>{{ demoService.showTooltips() ? 'visibility_off' : 'visibility' }}</mat-icon>
              <span>{{ demoService.showTooltips() ? 'Hide' : 'Show' }} Tooltips</span>
            </button>
          </mat-menu>

          <!-- Loading indicator -->
          <mat-spinner *ngIf="demoService.isLoading()" diameter="24"></mat-spinner>
        </div>

        <!-- Exit Demo Mode -->
        <button
          mat-stroked-button
          class="exit-demo-btn"
          (click)="exitDemoMode()"
          matTooltip="Exit Demo Mode"
        >
          <mat-icon>close</mat-icon>
          Exit Demo
        </button>
      </div>

      <!-- Error display -->
      <div class="demo-error" *ngIf="demoService.error() as error">
        <mat-icon>warning</mat-icon>
        {{ error }}
        <button mat-icon-button (click)="clearError()">
          <mat-icon>close</mat-icon>
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .demo-control-bar {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        z-index: 9999;
        background: linear-gradient(135deg, #1e3a5f 0%, #2d5a87 100%);
        color: white;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
      }

      .demo-bar-content {
        display: flex;
        align-items: center;
        gap: 16px;
        padding: 8px 16px;
        max-width: 100%;
        overflow-x: auto;
      }

      .demo-indicator {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 4px 12px;
        background: rgba(255, 255, 255, 0.1);
        border-radius: 20px;
        white-space: nowrap;
      }

      .demo-icon {
        color: #ff5252;
        animation: pulse 2s infinite;
      }

      @keyframes pulse {
        0%,
        100% {
          opacity: 1;
        }
        50% {
          opacity: 0.5;
        }
      }

      .demo-label {
        font-weight: 600;
        font-size: 12px;
        letter-spacing: 1px;
      }

      .scenario-info {
        flex-shrink: 0;
      }

      .progress-info {
        display: flex;
        flex-direction: column;
        gap: 4px;
        min-width: 220px;
      }

      .progress-text {
        display: flex;
        gap: 8px;
        font-size: 12px;
        white-space: nowrap;
      }

      .progress-stage {
        font-weight: 600;
      }

      .progress-message {
        opacity: 0.8;
      }

      .progress-metrics {
        font-size: 11px;
        opacity: 0.8;
      }

      .demo-progress-bar {
        height: 6px;
        border-radius: 4px;
        background: rgba(255, 255, 255, 0.2);
      }

      .load-summary {
        font-size: 12px;
        opacity: 0.9;
        white-space: nowrap;
      }

      .recording-controls {
        display: flex;
        align-items: center;
        gap: 4px;
        padding: 4px 8px;
        background: rgba(255, 255, 255, 0.1);
        border-radius: 8px;
      }

      .recording-timer {
        display: flex;
        align-items: center;
        gap: 4px;
        font-family: monospace;
        font-size: 16px;
        min-width: 60px;
      }

      .recording-timer.recording {
        color: #ff5252;
      }

      .recording-dot {
        color: #ff5252;
        animation: blink 1s infinite;
        font-size: 12px;
      }

      @keyframes blink {
        0%,
        100% {
          opacity: 1;
        }
        50% {
          opacity: 0;
        }
      }

      .timer-display {
        font-weight: 600;
      }

      .quick-actions {
        display: flex;
        align-items: center;
        gap: 4px;
        margin-left: auto;
      }

      .exit-demo-btn {
        border-color: rgba(255, 255, 255, 0.5);
        color: white;
      }

      .exit-demo-btn:hover {
        background: rgba(255, 255, 255, 0.1);
      }

      .demo-error {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 4px 16px;
        background: #ff5252;
        font-size: 12px;
      }

      .data-flow-container {
        position: fixed;
        top: 60px;
        left: 0;
        right: 0;
        bottom: 0;
        background: white;
        z-index: 9998;
        overflow-y: auto;
        padding: 16px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      }

      ::ng-deep .mat-mdc-chip.mat-mdc-standard-chip {
        background-color: rgba(255, 255, 255, 0.2) !important;
        color: white !important;
      }
    `,
  ],
})
export class DemoControlBarComponent implements OnInit {
  scenarioOptions: Array<{ name: string; label: string; icon: string }> = [];
  showDataFlow = false;

  private get logger() {
    return this.loggerService.withContext('DemoControlBarComponent');
  }

  constructor(
    public demoService: DemoModeService,
    private loggerService: LoggerService
  ) {}

  async ngOnInit(): Promise<void> {
    const scenarios = await this.demoService.loadScenarios();
    this.scenarioOptions = scenarios.length
      ? scenarios.map((scenario) => ({
          name: scenario.name,
          label: scenario.displayName || scenario.name,
          icon: this.getScenarioIcon(scenario.name),
        }))
      : [
          { name: 'hedis-evaluation', label: 'HEDIS Evaluation', icon: 'assessment' },
          { name: 'patient-journey', label: 'Patient Journey', icon: 'person' },
          { name: 'risk-stratification', label: 'Risk Stratification', icon: 'speed' },
          { name: 'multi-tenant', label: 'Multi-Tenant Admin', icon: 'business' },
        ];
  }

  async loadScenario(scenarioId: string): Promise<void> {
    try {
      // Auto-show data flow when loading starts
      this.showDataFlow = true;
      await this.demoService.loadScenario(scenarioId);
    } catch (err) {
      this.logger.error('Failed to load scenario:', err);
    }
  }

  async resetDemo(): Promise<void> {
    if (confirm('Are you sure you want to reset all demo data?')) {
      try {
        await this.demoService.resetDemo();
      } catch (err) {
        this.logger.error('Failed to reset demo:', err);
      }
    }
  }

  canCancelLoad(): boolean {
    const progress = this.demoService.progress();
    if (!progress) {
      return false;
    }
    return !['COMPLETE', 'FAILED', 'CANCELLED'].includes(progress.stage) && !progress.cancelRequested;
  }

  async cancelLoad(): Promise<void> {
    if (!confirm('Cancel the current scenario load?')) {
      return;
    }
    try {
      await this.demoService.cancelCurrentLoad();
    } catch (err) {
      this.logger.error('Failed to cancel scenario load:', err);
    }
  }

  async stopSession(): Promise<void> {
    if (!confirm('Stop the current demo session?')) {
      return;
    }
    try {
      await this.demoService.stopCurrentSession();
    } catch (err) {
      this.logger.error('Failed to stop demo session:', err);
    }
  }

  async resetCurrentTenant(): Promise<void> {
    if (confirm('Reset current scenario data for its tenant?')) {
      try {
        await this.demoService.resetCurrentTenant();
      } catch (err) {
        this.logger.error('Failed to reset current tenant:', err);
      }
    }
  }

  async createSnapshot(): Promise<void> {
    const name = prompt('Enter snapshot name:', `demo-snapshot-${Date.now()}`);
    if (name) {
      try {
        await this.demoService.createSnapshot(name);
        alert('Snapshot created successfully!');
      } catch (err) {
        this.logger.error('Failed to create snapshot:', err);
      }
    }
  }

  toggleTooltips(): void {
    this.demoService.toggleTooltips();
  }

  exitDemoMode(): void {
    if (this.demoService.isRecording()) {
      if (!confirm('Recording in progress. Are you sure you want to exit demo mode?')) {
        return;
      }
    }
    this.demoService.disableDemoMode();
  }

  clearError(): void {
    // Clear error by reloading status
    this.demoService.loadStatus();
  }

  formatTimestamp(value: string): string {
    const parsed = Date.parse(value);
    if (Number.isNaN(parsed)) {
      return value;
    }
    return new Date(parsed).toLocaleTimeString();
  }

  private getScenarioIcon(name: string): string {
    if (name.includes('hedis')) return 'assessment';
    if (name.includes('risk')) return 'speed';
    if (name.includes('journey')) return 'person';
    if (name.includes('tenant')) return 'business';
    return 'movie';
  }

  toggleDataFlow(): void {
    this.showDataFlow = !this.showDataFlow;
    // Auto-show data flow when loading starts
    if (this.showDataFlow && this.demoService.isLoading()) {
      // Data flow will be shown
    }
  }
}
