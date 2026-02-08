import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WebSocketService, CareGapNotificationMessage } from '@health-platform/shared/realtime';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

/**
 * Care Gap Metrics Component
 *
 * Displays real-time care gap metrics from WebSocket updates.
 * Shows care gap counts, urgency breakdown, and top priority gaps.
 *
 * ★ Insight ─────────────────────────────────────
 * This component aggregates care gaps by urgency level in real-time.
 * Using a Map to track gaps by ID enables efficient updates without
 * rebuilding the entire list. The component only re-renders affected
 * metrics sections, improving performance under rapid updates.
 * ─────────────────────────────────────────────────
 */
@Component({
  selector: 'app-care-gap-metrics',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="metric-card care-gap-card">
      <div class="metric-header">
        <h3 class="metric-title">Care Gaps</h3>
        <span class="metric-updated">{{ updatedTime }}</span>
      </div>

      <div class="metric-content">
        <div class="gap-count-display" [attr.data-testid]="'care-gap-count'" [ngClass]="countClass">
          <div class="count-number">{{ totalGapCount }}</div>
          <div class="count-label">Open Gaps</div>
        </div>

        <div class="gap-urgency-breakdown" [attr.data-testid]="'care-gap-urgency-routine'">
          <div class="urgency-row">
            <span class="urgency-label routine">Routine</span>
            <span class="urgency-count">{{ urgencyBreakdown.routine }}</span>
          </div>
          <div class="urgency-row">
            <span class="urgency-label soon">Soon</span>
            <span class="urgency-count">{{ urgencyBreakdown.soon }}</span>
          </div>
          <div class="urgency-row">
            <span class="urgency-label overdue">Overdue</span>
            <span class="urgency-count">{{ urgencyBreakdown.overdue }}</span>
          </div>
          <div class="urgency-row" [attr.data-testid]="'care-gap-urgency-critical'">
            <span class="urgency-label critical">Critical</span>
            <span class="urgency-count">{{ urgencyBreakdown.critical }}</span>
          </div>
        </div>

        <div class="top-gaps" [attr.data-testid]="'top-priority-gaps'">
          <div class="gaps-label">Top Priority Gaps:</div>
          <div class="gaps-list">
            <div
              *ngFor="let gap of topGaps"
              class="gap-item"
              [ngClass]="'gap-urgency-' + gap.urgency"
              [attr.data-testid]="'gap-priority-item'"
            >
              <span class="gap-measure">{{ gap.measureId }}</span>
              <span class="gap-action">{{ gap.action }}</span>
            </div>
          </div>
        </div>

        <div class="gap-closure-rate" [attr.data-testid]="'care-gap-closure-rate'">
          Closure Rate: {{ closureRate }}%
        </div>
      </div>
    </div>
  `,
  styles: [`
    .metric-card {
      background: white;
      border-radius: 8px;
      padding: 1.5rem;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      transition: all 0.3s ease;
    }

    .care-gap-card.count-updating {
      background: #fff3f0;
      animation: pulse-highlight 0.6s ease;
    }

    @keyframes pulse-highlight {
      0% {
        background: #ffebee;
      }
      50% {
        background: #fff3f0;
      }
      100% {
        background: white;
      }
    }

    .metric-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
    }

    .metric-title {
      margin: 0;
      font-size: 1.1rem;
      font-weight: 600;
      color: #333;
    }

    .metric-updated {
      font-size: 0.75rem;
      color: #999;
    }

    .metric-content {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .gap-count-display {
      text-align: center;
      padding: 1rem;
      background: #f5f5f5;
      border-radius: 8px;
      transition: all 0.3s ease;
    }

    .gap-count-display.count-updating {
      background: #ffcdd2;
    }

    .count-number {
      font-size: 2.5rem;
      font-weight: bold;
      color: #d32f2f;
      line-height: 1;
    }

    .count-label {
      font-size: 0.9rem;
      color: #666;
      margin-top: 0.5rem;
    }

    .gap-urgency-breakdown {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.75rem;
    }

    .urgency-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.75rem;
      background: #f5f5f5;
      border-radius: 4px;
      font-size: 0.9rem;
    }

    .urgency-label {
      font-weight: 600;
      padding: 0.25rem 0.5rem;
      border-radius: 3px;
      color: white;
      font-size: 0.8rem;
    }

    .urgency-label.routine {
      background: #4caf50;
    }

    .urgency-label.soon {
      background: #ff9800;
    }

    .urgency-label.overdue {
      background: #ff5722;
    }

    .urgency-label.critical {
      background: #d32f2f;
    }

    .urgency-count {
      font-weight: bold;
      color: #333;
    }

    .top-gaps {
      border-top: 1px solid #e0e0e0;
      padding-top: 1rem;
    }

    .gaps-label {
      font-size: 0.85rem;
      font-weight: 600;
      color: #666;
      margin-bottom: 0.75rem;
    }

    .gaps-list {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .gap-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.75rem;
      background: #f5f5f5;
      border-left: 4px solid #ccc;
      border-radius: 2px;
      font-size: 0.85rem;
    }

    .gap-item.gap-urgency-routine {
      border-left-color: #4caf50;
    }

    .gap-item.gap-urgency-soon {
      border-left-color: #ff9800;
    }

    .gap-item.gap-urgency-overdue {
      border-left-color: #ff5722;
    }

    .gap-item.gap-urgency-critical {
      border-left-color: #d32f2f;
      background: #ffebee;
    }

    .gap-measure {
      font-weight: 600;
      color: #333;
    }

    .gap-action {
      color: #666;
      font-size: 0.8rem;
    }

    .gap-closure-rate {
      text-align: center;
      padding: 0.75rem;
      background: #f5f5f5;
      border-radius: 4px;
      font-size: 0.9rem;
      color: #666;
    }
  `],
})
export class CareGapMetricsComponent implements OnInit, OnDestroy {
  totalGapCount = 0;
  urgencyBreakdown = { routine: 0, soon: 0, overdue: 0, critical: 0 };
  topGaps: any[] = [];
  closureRate = 0;
  countClass = '';
  updatedTime = '';

  private gaps = new Map<string, any>();
  private destroy$ = new Subject<void>();
  private updateTimeout: any;

  constructor(private websocket: WebSocketService) {}

  ngOnInit(): void {
    // Subscribe to care gap notifications
    this.websocket
      .ofType<CareGapNotificationMessage>('CARE_GAP_NOTIFICATION')
      .pipe(takeUntil(this.destroy$))
      .subscribe((message) => {
        this.addOrUpdateGap(message.data);
      });
  }

  ngOnDestroy(): void {
    if (this.updateTimeout) {
      clearTimeout(this.updateTimeout);
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  private addOrUpdateGap(data: CareGapNotificationMessage['data']): void {
    // Add or update gap in map
    const gapKey = data.gapType
      ? `${data.patientId}-${data.gapType}`
      : `${data.patientId}-${data.measureName ?? 'unknown'}`;
    this.gaps.set(gapKey, data);

    // Recalculate metrics
    this.updateMetrics();

    // Update timestamp
    this.updatedTime = new Date().toLocaleTimeString();

    // Add update highlight
    this.countClass = 'count-updating';
    this.updateTimeout = setTimeout(() => {
      this.countClass = '';
    }, 600);
  }

  private updateMetrics(): void {
    // Reset breakdown
    this.urgencyBreakdown = { routine: 0, soon: 0, overdue: 0, critical: 0 };
    const urgencyMap = { routine: 0, soon: 0, overdue: 0, critical: 0 };

    // Count by urgency
    this.gaps.forEach((gap) => {
      const urgency = this.normalizeUrgency(gap.urgency);
      urgencyMap[urgency]++;
    });

    this.urgencyBreakdown = urgencyMap;
    this.totalGapCount = this.gaps.size;

    // Get top 5 critical and overdue gaps
    const sorted = Array.from(this.gaps.values()).sort((a, b) => {
      const urgencyOrder = { critical: 4, overdue: 3, soon: 2, routine: 1 };
      return (
        urgencyOrder[this.normalizeUrgency(b.urgency)] -
        urgencyOrder[this.normalizeUrgency(a.urgency)]
      );
    });

    this.topGaps = sorted.slice(0, 5).map((gap) => ({
      gapId: gap.gapType ?? gap.measureName ?? 'Unknown',
      measureId: gap.measureName ?? 'Unknown',
      urgency: this.normalizeUrgency(gap.urgency),
      action: gap.recommendedAction || 'Review needed',
    }));

    // Calculate closure rate (assuming gaps come with closure status)
    const closedCount = Array.from(this.gaps.values()).filter(
      (g) => g.status === 'closed'
    ).length;
    this.closureRate = this.totalGapCount > 0
      ? Math.round((closedCount / this.totalGapCount) * 100)
      : 0;
  }

  private normalizeUrgency(value: unknown): 'routine' | 'soon' | 'overdue' | 'critical' {
    const normalized = typeof value === 'string' ? value.toLowerCase() : 'routine';
    if (normalized === 'critical' || normalized === 'overdue' || normalized === 'soon') {
      return normalized;
    }
    return 'routine';
  }
}
