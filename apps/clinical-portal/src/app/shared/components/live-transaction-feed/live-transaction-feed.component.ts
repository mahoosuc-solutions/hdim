import { Component, OnInit, OnDestroy, Input, ViewChild, ElementRef, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDividerModule } from '@angular/material/divider';
import { MatBadgeModule } from '@angular/material/badge';
import { Subject, takeUntil } from 'rxjs';
import { SystemEventsService } from '../../../services/system-events.service';
import {
  SystemEvent,
  EventCategory,
  EventSeverity,
  getEventIcon,
  formatRelativeTime,
  getSeverityColor,
} from '../../../models/system-event.model';
import { StatusIndicatorComponent } from '../status-indicator/status-indicator.component';

/**
 * LiveTransactionFeed Component
 *
 * Displays a real-time scrolling list of system events.
 * Events are color-coded by type and severity.
 *
 * Features:
 * - Auto-scrolling event list (last 100 events)
 * - Pause/resume functionality
 * - Filter by event category
 * - Expandable event details
 * - Connection status indicator
 *
 * @example
 * <app-live-transaction-feed [maxEvents]="50"></app-live-transaction-feed>
 */
@Component({
  selector: 'app-live-transaction-feed',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatMenuModule,
    MatCheckboxModule,
    MatDividerModule,
    MatBadgeModule,
    StatusIndicatorComponent,
  ],
  template: `
    <mat-card class="live-feed-card">
      <mat-card-header class="feed-header">
        <mat-card-title class="feed-title">
          <mat-icon class="title-icon">dynamic_feed</mat-icon>
          Live Activity Feed
        </mat-card-title>
        <div class="feed-controls">
          <app-status-indicator
            [status]="connectionStatus"
            size="small"
            [showLabel]="true">
          </app-status-indicator>

          <button mat-icon-button
                  [matTooltip]="isPaused ? 'Resume' : 'Pause'"
                  (click)="togglePause()">
            <mat-icon>{{ isPaused ? 'play_arrow' : 'pause' }}</mat-icon>
          </button>

          <button mat-icon-button
                  [matMenuTriggerFor]="filterMenu"
                  matTooltip="Filter events">
            <mat-icon [matBadge]="getActiveFilterCount()"
                      [matBadgeHidden]="getActiveFilterCount() === 0"
                      matBadgeSize="small"
                      matBadgeColor="primary">
              filter_list
            </mat-icon>
          </button>

          <mat-menu #filterMenu="matMenu" class="filter-menu">
            <div class="filter-section" (click)="$event.stopPropagation()">
              <div class="filter-label">Categories</div>
              <mat-checkbox *ngFor="let cat of categories"
                            [checked]="categoryFilters.has(cat.value)"
                            (change)="toggleCategoryFilter(cat.value)">
                {{ cat.label }}
              </mat-checkbox>
            </div>
            <mat-divider></mat-divider>
            <div class="filter-section" (click)="$event.stopPropagation()">
              <div class="filter-label">Severity</div>
              <mat-checkbox *ngFor="let sev of severities"
                            [checked]="severityFilters.has(sev.value)"
                            (change)="toggleSeverityFilter(sev.value)">
                {{ sev.label }}
              </mat-checkbox>
            </div>
            <mat-divider></mat-divider>
            <button mat-menu-item (click)="clearFilters()">
              <mat-icon>clear_all</mat-icon>
              Clear Filters
            </button>
          </mat-menu>

          <button mat-icon-button
                  matTooltip="Clear all events"
                  (click)="clearEvents()">
            <mat-icon>delete_sweep</mat-icon>
          </button>
        </div>
      </mat-card-header>

      <mat-card-content class="feed-content" #feedContainer>
        <div class="events-list" *ngIf="filteredEvents.length > 0; else emptyState">
          <div class="event-item"
               *ngFor="let event of filteredEvents; trackBy: trackByEventId"
               [class]="'severity-' + event.severity"
               [class.expanded]="expandedEventId === event.id"
               (click)="toggleEventDetails(event.id)">

            <div class="event-main">
              <div class="event-indicator">
                <span class="severity-dot" [class]="'dot-' + event.severity"></span>
              </div>

              <div class="event-time">
                {{ formatTime(event.timestamp) }}
              </div>

              <div class="event-icon">
                <mat-icon [class]="'icon-' + event.category">{{ getIcon(event.type) }}</mat-icon>
              </div>

              <div class="event-content">
                <div class="event-title">{{ event.title }}</div>
                <div class="event-description">{{ event.description }}</div>
                <div class="event-patient" *ngIf="event.patient?.name">
                  <mat-icon class="patient-icon">person</mat-icon>
                  {{ event.patient?.name }}
                  <span class="mrn" *ngIf="event.patient?.mrn">({{ event.patient?.mrn }})</span>
                </div>
              </div>

              <mat-icon class="expand-icon">
                {{ expandedEventId === event.id ? 'expand_less' : 'expand_more' }}
              </mat-icon>
            </div>

            <div class="event-details" *ngIf="expandedEventId === event.id">
              <div class="detail-row">
                <span class="detail-label">Source:</span>
                <span class="detail-value">{{ event.source }}</span>
              </div>
              <div class="detail-row" *ngIf="event.measure">
                <span class="detail-label">Measure:</span>
                <span class="detail-value">{{ event.measure.name }}</span>
              </div>
              <div class="detail-row" *ngIf="event.durationMs">
                <span class="detail-label">Duration:</span>
                <span class="detail-value">{{ event.durationMs }}ms</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">Event ID:</span>
                <span class="detail-value mono">{{ event.id }}</span>
              </div>
            </div>
          </div>
        </div>

        <ng-template #emptyState>
          <div class="empty-state">
            <mat-icon class="empty-icon">inbox</mat-icon>
            <div class="empty-text">
              {{ isPaused ? 'Feed paused' : 'Waiting for events...' }}
            </div>
            <div class="empty-hint" *ngIf="!isPaused">
              Events will appear here as they occur
            </div>
          </div>
        </ng-template>
      </mat-card-content>

      <div class="feed-footer" *ngIf="events.length > 0">
        <span class="event-count">{{ filteredEvents.length }} of {{ events.length }} events</span>
        <span class="last-update" *ngIf="lastEventTime">
          Last: {{ formatTime(lastEventTime) }}
        </span>
      </div>
    </mat-card>
  `,
  styles: [`
    .live-feed-card {
      height: 100%;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      background-color: var(--card-background);
    }

    .feed-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid var(--border-color);
    }

    .feed-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 1rem;
      margin: 0;
      color: var(--text-primary);
    }

    .title-icon {
      color: var(--primary-color);
    }

    .feed-controls {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .feed-content {
      flex: 1;
      overflow-y: auto;
      padding: 0;
      scrollbar-width: thin;
    }

    .feed-content::-webkit-scrollbar {
      width: 6px;
    }

    .feed-content::-webkit-scrollbar-thumb {
      background-color: var(--text-disabled);
      border-radius: 3px;
    }

    .events-list {
      display: flex;
      flex-direction: column;
    }

    .event-item {
      padding: 10px 16px;
      border-bottom: 1px solid var(--divider-color);
      cursor: pointer;
      transition: background-color 0.15s ease;
    }

    .event-item:hover {
      background-color: var(--bg-hover);
    }

    .event-item.expanded {
      background-color: var(--bg-tertiary);
    }

    .event-main {
      display: flex;
      align-items: flex-start;
      gap: 10px;
    }

    .event-indicator {
      padding-top: 4px;
    }

    .severity-dot {
      display: inline-block;
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }

    .dot-info { background-color: var(--status-info); }
    .dot-success { background-color: var(--status-success); }
    .dot-warning { background-color: var(--status-warning); }
    .dot-error { background-color: var(--status-error); }

    .event-time {
      font-size: 0.75rem;
      color: var(--text-secondary);
      min-width: 50px;
      padding-top: 2px;
    }

    .event-icon {
      padding-top: 1px;
    }

    .event-icon mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
    }

    .icon-fhir { color: var(--primary-color); }
    .icon-evaluation { color: #7b1fa2; }
    .icon-quality { color: var(--status-success); }
    .icon-care-gap { color: var(--status-warning); }
    .icon-consent { color: #00897b; }
    .icon-system { color: var(--text-secondary); }

    .event-content {
      flex: 1;
      min-width: 0;
    }

    .event-title {
      font-weight: 500;
      font-size: 0.875rem;
      color: var(--text-primary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .event-description {
      font-size: 0.8rem;
      color: var(--text-secondary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .event-patient {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 0.75rem;
      color: var(--text-hint);
      margin-top: 4px;
    }

    .patient-icon {
      font-size: 14px;
      width: 14px;
      height: 14px;
    }

    .mrn {
      font-family: monospace;
      font-size: 0.7rem;
    }

    .expand-icon {
      color: var(--text-hint);
      font-size: 20px;
    }

    .event-details {
      margin-top: 10px;
      padding: 10px;
      background-color: var(--bg-tertiary);
      border-radius: 4px;
      font-size: 0.8rem;
    }

    .detail-row {
      display: flex;
      gap: 8px;
      margin-bottom: 4px;
    }

    .detail-row:last-child {
      margin-bottom: 0;
    }

    .detail-label {
      color: var(--text-secondary);
      min-width: 80px;
    }

    .detail-value {
      color: var(--text-primary);
    }

    .detail-value.mono {
      font-family: monospace;
      font-size: 0.7rem;
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 40px 20px;
      color: var(--text-hint);
    }

    .empty-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      margin-bottom: 12px;
      opacity: 0.5;
    }

    .empty-text {
      font-size: 0.9rem;
      margin-bottom: 4px;
    }

    .empty-hint {
      font-size: 0.8rem;
      opacity: 0.7;
    }

    .feed-footer {
      display: flex;
      justify-content: space-between;
      padding: 8px 16px;
      font-size: 0.75rem;
      color: var(--text-secondary);
      border-top: 1px solid var(--border-color);
      background-color: var(--bg-tertiary);
    }

    .filter-section {
      padding: 8px 16px;
    }

    .filter-label {
      font-size: 0.75rem;
      color: var(--text-secondary);
      margin-bottom: 8px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    ::ng-deep .filter-menu mat-checkbox {
      display: block;
      margin-bottom: 4px;
    }

    /* Severity-based left border */
    .severity-info { border-left: 3px solid var(--status-info); }
    .severity-success { border-left: 3px solid var(--status-success); }
    .severity-warning { border-left: 3px solid var(--status-warning); }
    .severity-error { border-left: 3px solid var(--status-error); }

    /* Animation for new events */
    @keyframes slideIn {
      from {
        opacity: 0;
        transform: translateY(-10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .event-item:first-child {
      animation: slideIn 0.3s ease-out;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LiveTransactionFeedComponent implements OnInit, OnDestroy {
  /** Maximum events to display */
  @Input() maxEvents: number = 100;

  @ViewChild('feedContainer') feedContainer!: ElementRef;

  // State
  events: SystemEvent[] = [];
  filteredEvents: SystemEvent[] = [];
  isPaused: boolean = false;
  expandedEventId: string | null = null;
  connectionStatus: 'connected' | 'disconnected' | 'simulating' = 'disconnected';
  lastEventTime: string | null = null;

  // Filters
  categoryFilters: Set<EventCategory> = new Set();
  severityFilters: Set<EventSeverity> = new Set();

  // Filter options
  categories: { value: EventCategory; label: string }[] = [
    { value: 'fhir', label: 'FHIR Resources' },
    { value: 'evaluation', label: 'Evaluations' },
    { value: 'quality', label: 'Quality Measures' },
    { value: 'care-gap', label: 'Care Gaps' },
    { value: 'consent', label: 'Consent' },
    { value: 'system', label: 'System' },
  ];

  severities: { value: EventSeverity; label: string }[] = [
    { value: 'info', label: 'Info' },
    { value: 'success', label: 'Success' },
    { value: 'warning', label: 'Warning' },
    { value: 'error', label: 'Error' },
  ];

  private destroy$ = new Subject<void>();

  // Helper functions exposed to template
  getIcon = getEventIcon;
  formatTime = formatRelativeTime;

  constructor(
    private eventsService: SystemEventsService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Subscribe to events
    this.eventsService.events$
      .pipe(takeUntil(this.destroy$))
      .subscribe(events => {
        this.events = events;
        this.applyFilters();
        if (events.length > 0) {
          this.lastEventTime = events[0].timestamp;
        }
        this.cdr.markForCheck();
      });

    // Subscribe to pause state
    this.eventsService.isPaused$
      .pipe(takeUntil(this.destroy$))
      .subscribe(isPaused => {
        this.isPaused = isPaused;
        this.cdr.markForCheck();
      });

    // Subscribe to connection status
    this.eventsService.connectionStatus$
      .pipe(takeUntil(this.destroy$))
      .subscribe(status => {
        this.connectionStatus = status;
        this.cdr.markForCheck();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Toggle pause state
   */
  togglePause(): void {
    this.eventsService.togglePause();
  }

  /**
   * Clear all events
   */
  clearEvents(): void {
    this.eventsService.clearEvents();
    this.expandedEventId = null;
  }

  /**
   * Toggle category filter
   */
  toggleCategoryFilter(category: EventCategory): void {
    if (this.categoryFilters.has(category)) {
      this.categoryFilters.delete(category);
    } else {
      this.categoryFilters.add(category);
    }
    this.applyFilters();
  }

  /**
   * Toggle severity filter
   */
  toggleSeverityFilter(severity: EventSeverity): void {
    if (this.severityFilters.has(severity)) {
      this.severityFilters.delete(severity);
    } else {
      this.severityFilters.add(severity);
    }
    this.applyFilters();
  }

  /**
   * Clear all filters
   */
  clearFilters(): void {
    this.categoryFilters.clear();
    this.severityFilters.clear();
    this.applyFilters();
  }

  /**
   * Get count of active filters
   */
  getActiveFilterCount(): number {
    return this.categoryFilters.size + this.severityFilters.size;
  }

  /**
   * Apply filters to events
   */
  private applyFilters(): void {
    this.filteredEvents = this.events.filter(event => {
      // If no category filters, show all categories
      if (this.categoryFilters.size > 0 && !this.categoryFilters.has(event.category)) {
        return false;
      }
      // If no severity filters, show all severities
      if (this.severityFilters.size > 0 && !this.severityFilters.has(event.severity)) {
        return false;
      }
      return true;
    });
  }

  /**
   * Toggle event details expansion
   */
  toggleEventDetails(eventId: string): void {
    this.expandedEventId = this.expandedEventId === eventId ? null : eventId;
    this.cdr.markForCheck();
  }

  /**
   * Track by function for ngFor
   */
  trackByEventId(index: number, event: SystemEvent): string {
    return event.id;
  }
}
