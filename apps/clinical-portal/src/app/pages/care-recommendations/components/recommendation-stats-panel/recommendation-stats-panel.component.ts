import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

import { StatCardComponent } from '../../../../shared/components/stat-card/stat-card.component';
import {
  RecommendationDashboardStats,
  URGENCY_COLORS,
} from '../../../../models/care-recommendation.model';

@Component({
  selector: 'app-recommendation-stats-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatTooltipModule,
    StatCardComponent,
  ],
  template: `
    <div class="stats-panel">
      <div class="stats-grid">
        <!-- Total Recommendations -->
        <app-stat-card
          title="Total Recommendations"
          [value]="stats.totalRecommendations.toString()"
          icon="recommend"
          trend="none">
        </app-stat-card>

        <!-- Emergent -->
        <app-stat-card
          title="Emergent"
          [value]="stats.byUrgency.emergent.toString()"
          icon="error"
          [iconColor]="URGENCY_COLORS['emergent']"
          trend="none"
          (click)="onStatClick('emergent')">
        </app-stat-card>

        <!-- Urgent -->
        <app-stat-card
          title="Urgent"
          [value]="stats.byUrgency.urgent.toString()"
          icon="warning"
          [iconColor]="URGENCY_COLORS['urgent']"
          trend="none"
          (click)="onStatClick('urgent')">
        </app-stat-card>

        <!-- Overdue -->
        <app-stat-card
          title="Overdue"
          [value]="stats.overdueSummary.total.toString()"
          icon="schedule"
          iconColor="#f44336"
          trend="none"
          (click)="onStatClick('overdue')">
        </app-stat-card>

        <!-- Pending -->
        <app-stat-card
          title="Pending"
          [value]="stats.byStatus.pending.toString()"
          icon="pending"
          trend="none">
        </app-stat-card>

        <!-- Completed -->
        <app-stat-card
          title="Completed"
          [value]="stats.byStatus.completed.toString()"
          icon="task_alt"
          iconColor="#4caf50"
          trend="none">
        </app-stat-card>
      </div>
    </div>
  `,
  styles: [
    `
      .stats-panel {
        margin-bottom: 24px;
      }

      .stats-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
        gap: 16px;
      }

      app-stat-card {
        cursor: pointer;
      }

      @media (max-width: 768px) {
        .stats-grid {
          grid-template-columns: repeat(2, 1fr);
        }
      }
    `,
  ],
})
export class RecommendationStatsPanelComponent {
  @Input() stats!: RecommendationDashboardStats;
  @Output() statClick = new EventEmitter<string>();

  readonly URGENCY_COLORS = URGENCY_COLORS;

  onStatClick(stat: string): void {
    this.statClick.emit(stat);
  }
}
