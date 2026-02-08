import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WebSocketService, HealthScoreUpdateMessage } from '@health-platform/shared/realtime';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

/**
 * Health Score Metrics Component
 *
 * Displays real-time health score metrics from WebSocket updates.
 * Shows current score, category, trend, and contributing factors.
 *
 * ★ Insight ─────────────────────────────────────
 * This component uses type-safe RxJS observables with ofType()
 * to filter only HEALTH_SCORE_UPDATE messages from WebSocket stream.
 * The takeUntil pattern ensures automatic cleanup when component
 * is destroyed, preventing memory leaks from long-lived subscriptions.
 * ─────────────────────────────────────────────────
 */
@Component({
  selector: 'app-health-score-metrics',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="metric-card health-score-card" [ngClass]="scoreClass">
      <div class="metric-header">
        <h3 class="metric-title">Health Score</h3>
        <span class="metric-updated">{{ updatedTime }}</span>
      </div>

      <div class="metric-content">
        <div class="score-display" [attr.data-testid]="'health-score-display'">
          <div class="score-number" [attr.data-testid]="'health-score-value'">
            {{ currentScore }}
          </div>
          <div class="score-category" [attr.data-testid]="'health-score-category'">
            {{ scoreCategory }}
          </div>
        </div>

        <div class="score-progress">
          <div
            class="progress-bar"
            [style.width.%]="currentScore"
            [attr.data-testid]="'health-score-progress'"
          ></div>
        </div>

        <div class="score-trend" [attr.data-testid]="'health-score-trend'">
          <span class="trend-indicator" [ngClass]="trendClass">
            {{ trendText }}
          </span>
          <span class="trend-value">{{ trendValue }}</span>
        </div>

        <div class="score-factors" [attr.data-testid]="'health-score-factors'">
          <div class="factors-label">Contributing Factors:</div>
          <div class="factors-list">
            <span
              *ngFor="let factor of factors"
              class="factor-badge"
              [attr.data-testid]="'health-score-factor'"
            >
              {{ formatFactorName(factor) }}
            </span>
          </div>
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

    .health-score-card.score-updating {
      background: #f0f8ff;
      animation: pulse-highlight 0.6s ease;
    }

    @keyframes pulse-highlight {
      0% {
        background: #e3f2fd;
      }
      50% {
        background: #f0f8ff;
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

    .score-display {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .score-number {
      font-size: 3rem;
      font-weight: bold;
      color: #1976d2;
      line-height: 1;
    }

    .score-category {
      font-size: 0.9rem;
      padding: 0.25rem 0.75rem;
      border-radius: 20px;
      font-weight: 500;
    }

    .score-category.excellent {
      background: #4caf50;
      color: white;
    }

    .score-category.good {
      background: #81c784;
      color: white;
    }

    .score-category.fair {
      background: #ff9800;
      color: white;
    }

    .score-category.poor {
      background: #f44336;
      color: white;
    }

    .score-progress {
      height: 8px;
      background: #e0e0e0;
      border-radius: 4px;
      overflow: hidden;
    }

    .progress-bar {
      height: 100%;
      background: linear-gradient(90deg, #81c784, #4caf50);
      transition: width 0.6s ease;
    }

    .score-trend {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.9rem;
    }

    .trend-indicator {
      font-weight: 600;
      padding: 0.25rem 0.5rem;
      border-radius: 4px;
    }

    .trend-improving {
      color: #4caf50;
      background: #f1f8f6;
    }

    .trend-stable {
      color: #ff9800;
      background: #fff3f0;
    }

    .trend-declining {
      color: #f44336;
      background: #ffebee;
    }

    .trend-value {
      color: #666;
    }

    .score-factors {
      border-top: 1px solid #e0e0e0;
      padding-top: 1rem;
    }

    .factors-label {
      font-size: 0.85rem;
      font-weight: 600;
      color: #666;
      margin-bottom: 0.5rem;
    }

    .factors-list {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .factor-badge {
      background: #f5f5f5;
      border: 1px solid #ddd;
      border-radius: 4px;
      padding: 0.25rem 0.75rem;
      font-size: 0.8rem;
      color: #666;
    }
  `],
})
export class HealthScoreMetricsComponent implements OnInit, OnDestroy {
  currentScore = 0;
  scoreCategory = 'Unknown';
  trendText = 'Stable';
  trendValue = '';
  trendClass = '';
  factors: string[] = [];
  scoreClass = '';
  updatedTime = '';

  private destroy$ = new Subject<void>();
  private updateTimeout: any;

  constructor(private websocket: WebSocketService) {}

  ngOnInit(): void {
    // Subscribe to health score updates
    this.websocket
      .ofType<HealthScoreUpdateMessage>('HEALTH_SCORE_UPDATE')
      .pipe(takeUntil(this.destroy$))
      .subscribe((message) => {
        this.updateHealthScore(message.data);
      });
  }

  ngOnDestroy(): void {
    if (this.updateTimeout) {
      clearTimeout(this.updateTimeout);
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  private updateHealthScore(data: any): void {
    const oldScore = this.currentScore;

    // Update score and category
    this.currentScore = data.score || 0;
    this.scoreCategory = this.getCategoryLabel(data.category);

    // Determine trend
    if (data.score > oldScore) {
      this.trendText = 'Improving';
      this.trendClass = 'trend-improving';
      this.trendValue = `+${data.score - oldScore}`;
    } else if (data.score < oldScore) {
      this.trendText = 'Declining';
      this.trendClass = 'trend-declining';
      this.trendValue = `${data.score - oldScore}`;
    } else {
      this.trendText = 'Stable';
      this.trendClass = 'trend-stable';
      this.trendValue = '±0';
    }

    // Update factors
    this.factors = data.factors || [];

    // Update timestamp
    this.updatedTime = new Date().toLocaleTimeString();

    // Add update highlight animation
    this.scoreClass = 'score-updating';

    // Remove highlight after animation
    this.updateTimeout = setTimeout(() => {
      this.scoreClass = '';
    }, 600);
  }

  private getCategoryLabel(category: string): string {
    const categoryMap: { [key: string]: string } = {
      excellent: 'Excellent',
      good: 'Good',
      fair: 'Fair',
      poor: 'Poor',
    };
    return categoryMap[category?.toLowerCase()] || 'Unknown';
  }

  formatFactorName(factor: string): string {
    // Convert snake_case to Title Case
    return factor
      .split('-')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }
}
