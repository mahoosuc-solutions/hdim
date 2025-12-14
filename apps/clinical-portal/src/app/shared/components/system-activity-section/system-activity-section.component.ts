import { Component, OnInit, OnDestroy, Input, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { Subject, takeUntil } from 'rxjs';
import { SystemEventsService } from '../../../services/system-events.service';
import { LiveTransactionFeedComponent } from '../live-transaction-feed/live-transaction-feed.component';
import { LiveMetricsPanelComponent } from '../live-metrics-panel/live-metrics-panel.component';
import { DataFlowPipelineComponent } from '../data-flow-pipeline/data-flow-pipeline.component';
import { StatusIndicatorComponent } from '../status-indicator/status-indicator.component';

/**
 * SystemActivitySection Component
 *
 * A collapsible dashboard section that displays real-time system activity:
 * - Live Transaction Feed (scrolling events)
 * - Data Flow Pipeline (animated service diagram)
 * - Live Metrics Panel (counters and gauges)
 *
 * Features:
 * - Collapsible section with user preference persistence
 * - Start/stop simulation for demo mode
 * - Connect/disconnect from live data
 * - Responsive 3-column layout
 *
 * @example
 * <app-system-activity-section [collapsed]="false"></app-system-activity-section>
 */
@Component({
  selector: 'app-system-activity-section',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatSlideToggleModule,
    LiveTransactionFeedComponent,
    LiveMetricsPanelComponent,
    DataFlowPipelineComponent,
    StatusIndicatorComponent,
  ],
  template: `
    <section class="system-activity-section" [class.collapsed]="isCollapsed">
      <!-- Section Header -->
      <div class="section-header" (click)="toggleCollapse()">
        <div class="header-left">
          <mat-icon class="section-icon">monitor_heart</mat-icon>
          <h2 class="section-title">System Activity</h2>
          <app-status-indicator
            [status]="connectionStatus"
            size="small"
            [showLabel]="!isCollapsed">
          </app-status-indicator>
        </div>

        <div class="header-controls" (click)="$event.stopPropagation()">
          <!-- Simulation Toggle -->
          <div class="control-group" *ngIf="!isCollapsed">
            <button mat-icon-button
                    [matTooltip]="isSimulating ? 'Stop demo' : 'Start demo'"
                    [color]="isSimulating ? 'warn' : 'primary'"
                    (click)="toggleSimulation()">
              <mat-icon>{{ isSimulating ? 'stop' : 'play_arrow' }}</mat-icon>
            </button>

            <button mat-icon-button
                    *ngIf="!isSimulating"
                    [matTooltip]="connectionStatus === 'connected' ? 'Disconnect' : 'Connect'"
                    (click)="toggleConnection()">
              <mat-icon>{{ connectionStatus === 'connected' ? 'cloud_off' : 'cloud' }}</mat-icon>
            </button>
          </div>

          <!-- Collapse Toggle -->
          <button mat-icon-button
                  [matTooltip]="isCollapsed ? 'Expand' : 'Collapse'">
            <mat-icon>{{ isCollapsed ? 'expand_more' : 'expand_less' }}</mat-icon>
          </button>
        </div>
      </div>

      <!-- Section Content -->
      <div class="section-content" *ngIf="!isCollapsed">
        <div class="activity-grid">
          <!-- Live Transaction Feed (40%) -->
          <div class="grid-item feed-column">
            <app-live-transaction-feed></app-live-transaction-feed>
          </div>

          <!-- Data Flow Pipeline (30%) -->
          <div class="grid-item pipeline-column">
            <app-data-flow-pipeline></app-data-flow-pipeline>
          </div>

          <!-- Live Metrics Panel (30%) -->
          <div class="grid-item metrics-column">
            <app-live-metrics-panel></app-live-metrics-panel>
          </div>
        </div>
      </div>

      <!-- Collapsed Preview -->
      <div class="collapsed-preview" *ngIf="isCollapsed">
        <div class="preview-stat">
          <mat-icon>trending_up</mat-icon>
          <span>{{ eventsCount }} events</span>
        </div>
        <div class="preview-stat">
          <mat-icon>speed</mat-icon>
          <span>{{ throughput }}/sec</span>
        </div>
        <div class="preview-stat">
          <mat-icon>verified</mat-icon>
          <span>{{ complianceRate }}% compliance</span>
        </div>
        <span class="preview-hint">Click to expand</span>
      </div>
    </section>
  `,
  styles: [`
    .system-activity-section {
      background-color: var(--bg-secondary);
      border-radius: 12px;
      margin-bottom: 24px;
      overflow: hidden;
      box-shadow: 0 2px 8px var(--card-shadow);
      transition: all 0.3s ease;
      border: 1px solid var(--border-color);
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      background-color: var(--card-background);
      cursor: pointer;
      border-bottom: 1px solid var(--border-color);
      transition: background-color 0.2s ease;
    }

    .section-header:hover {
      background-color: var(--bg-hover);
    }

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .section-icon {
      color: var(--primary-color);
      font-size: 24px;
    }

    .section-title {
      margin: 0;
      font-size: 1.1rem;
      font-weight: 600;
      color: var(--text-primary);
    }

    .header-controls {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .control-group {
      display: flex;
      align-items: center;
      gap: 4px;
      padding-right: 8px;
      border-right: 1px solid var(--border-color);
    }

    .section-content {
      padding: 16px;
      animation: slideDown 0.3s ease;
    }

    @keyframes slideDown {
      from {
        opacity: 0;
        transform: translateY(-10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .activity-grid {
      display: grid;
      grid-template-columns: 2fr 1.5fr 1.5fr;
      gap: 16px;
      min-height: 350px;
    }

    .grid-item {
      display: flex;
      flex-direction: column;
    }

    .grid-item > * {
      height: 100%;
    }

    .collapsed-preview {
      display: flex;
      align-items: center;
      gap: 24px;
      padding: 8px 16px;
      background-color: var(--card-background);
    }

    .preview-stat {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 0.85rem;
      color: var(--text-secondary);
    }

    .preview-stat mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: var(--primary-color);
    }

    .preview-hint {
      margin-left: auto;
      font-size: 0.75rem;
      color: var(--text-hint);
      font-style: italic;
    }

    /* Collapsed state */
    .system-activity-section.collapsed .section-header {
      border-bottom: none;
    }

    /* Responsive */
    @media (max-width: 1200px) {
      .activity-grid {
        grid-template-columns: 1fr 1fr;
        grid-template-rows: auto auto;
      }

      .feed-column {
        grid-column: span 2;
      }
    }

    @media (max-width: 768px) {
      .activity-grid {
        grid-template-columns: 1fr;
      }

      .feed-column {
        grid-column: span 1;
      }

      .collapsed-preview {
        flex-wrap: wrap;
        gap: 12px;
      }

      .preview-hint {
        width: 100%;
        text-align: center;
      }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SystemActivitySectionComponent implements OnInit, OnDestroy {
  /** Whether the section starts collapsed */
  @Input() collapsed: boolean = false;

  isCollapsed: boolean = false;
  isSimulating: boolean = false;
  connectionStatus: 'connected' | 'disconnected' | 'simulating' = 'disconnected';

  // Preview stats for collapsed state
  eventsCount: number = 0;
  throughput: string = '0';
  complianceRate: string = '0';

  private readonly STORAGE_KEY = 'hdim-system-activity-collapsed';
  private destroy$ = new Subject<void>();

  constructor(
    private eventsService: SystemEventsService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Restore collapsed state from localStorage
    const savedState = localStorage.getItem(this.STORAGE_KEY);
    this.isCollapsed = savedState !== null ? savedState === 'true' : this.collapsed;

    // Subscribe to events for preview stats
    this.eventsService.events$
      .pipe(takeUntil(this.destroy$))
      .subscribe(events => {
        this.eventsCount = events.length;
        this.cdr.markForCheck();
      });

    // Subscribe to metrics for preview stats
    this.eventsService.metrics$
      .pipe(takeUntil(this.destroy$))
      .subscribe(metrics => {
        this.throughput = metrics.throughputPerSecond.toFixed(1);
        this.complianceRate = metrics.complianceRate.toFixed(1);
        this.cdr.markForCheck();
      });

    // Subscribe to simulation state
    this.eventsService.isSimulating$
      .pipe(takeUntil(this.destroy$))
      .subscribe(isSimulating => {
        this.isSimulating = isSimulating;
        this.cdr.markForCheck();
      });

    // Subscribe to connection status
    this.eventsService.connectionStatus$
      .pipe(takeUntil(this.destroy$))
      .subscribe(status => {
        this.connectionStatus = status;
        this.cdr.markForCheck();
      });

    // Auto-start simulation if not collapsed and not connected
    if (!this.isCollapsed) {
      this.eventsService.startSimulation();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Toggle section collapse state
   */
  toggleCollapse(): void {
    this.isCollapsed = !this.isCollapsed;
    localStorage.setItem(this.STORAGE_KEY, String(this.isCollapsed));

    // Start/stop simulation based on visibility
    if (this.isCollapsed) {
      this.eventsService.stopSimulation();
    } else if (!this.isSimulating && this.connectionStatus !== 'connected') {
      this.eventsService.startSimulation();
    }

    this.cdr.markForCheck();
  }

  /**
   * Toggle simulation mode
   */
  toggleSimulation(): void {
    if (this.isSimulating) {
      this.eventsService.stopSimulation();
    } else {
      this.eventsService.disconnect(); // Disconnect from live if connected
      this.eventsService.startSimulation();
    }
  }

  /**
   * Toggle live connection
   */
  toggleConnection(): void {
    if (this.connectionStatus === 'connected') {
      this.eventsService.disconnect();
    } else {
      this.eventsService.stopSimulation(); // Stop simulation if running
      this.eventsService.connect();
    }
  }
}
