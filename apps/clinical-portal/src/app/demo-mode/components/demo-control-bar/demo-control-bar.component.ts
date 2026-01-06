import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DemoModeService, DemoScenario } from '../../services/demo-mode.service';

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
              {{ scenario.name }}
            </mat-chip-option>
          </mat-chip-listbox>
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
            <button mat-menu-item (click)="loadScenario('hedis-evaluation')">
              <mat-icon>assessment</mat-icon>
              <span>HEDIS Evaluation</span>
            </button>
            <button mat-menu-item (click)="loadScenario('patient-journey')">
              <mat-icon>person</mat-icon>
              <span>Patient Journey</span>
            </button>
            <button mat-menu-item (click)="loadScenario('risk-stratification')">
              <mat-icon>speed</mat-icon>
              <span>Risk Stratification</span>
            </button>
            <button mat-menu-item (click)="loadScenario('multi-tenant')">
              <mat-icon>business</mat-icon>
              <span>Multi-Tenant Admin</span>
            </button>
          </mat-menu>

          <button mat-icon-button [matMenuTriggerFor]="actionsMenu" matTooltip="Actions">
            <mat-icon>more_vert</mat-icon>
          </button>
          <mat-menu #actionsMenu="matMenu">
            <button mat-menu-item (click)="createSnapshot()">
              <mat-icon>photo_camera</mat-icon>
              <span>Create Snapshot</span>
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

      ::ng-deep .mat-mdc-chip.mat-mdc-standard-chip {
        background-color: rgba(255, 255, 255, 0.2) !important;
        color: white !important;
      }
    `,
  ],
})
export class DemoControlBarComponent implements OnInit {
  scenarios: DemoScenario[] = [];

  constructor(public demoService: DemoModeService) {}

  async ngOnInit(): Promise<void> {
    this.scenarios = await this.demoService.loadScenarios();
  }

  async loadScenario(scenarioId: string): Promise<void> {
    try {
      await this.demoService.loadScenario(scenarioId);
    } catch (err) {
      console.error('Failed to load scenario:', err);
    }
  }

  async resetDemo(): Promise<void> {
    if (confirm('Are you sure you want to reset all demo data?')) {
      try {
        await this.demoService.resetDemo();
      } catch (err) {
        console.error('Failed to reset demo:', err);
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
        console.error('Failed to create snapshot:', err);
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
}
