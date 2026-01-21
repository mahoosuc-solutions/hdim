import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { Clinical360PipelineService } from '@health-platform/shared/data-access';
import { EventBusService, ClinicalEventType } from '@health-platform/shared/data-access';

export interface ReportMetrics {
  careReadinessScore: number;
  totalMeasures: number;
  measuresMet: number;
  totalGaps: number;
  criticalGaps: number;
  populationSize: number;
}

@Component({
  selector: 'app-reports-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="reports-dashboard">
      <h2>Care Readiness Dashboard</h2>

      <div *ngIf="metrics$ | async as metrics; else loading" class="metrics-grid">
        <!-- Care Readiness Score -->
        <div class="metric-card score-card">
          <h3>Care Readiness Score</h3>
          <div class="score-display">{{ metrics.careReadinessScore }}%</div>
          <div class="score-bar">
            <div class="score-fill" [style.width.%]="metrics.careReadinessScore"></div>
          </div>
        </div>

        <!-- Quality Measures Summary -->
        <div class="metric-card">
          <h3>Quality Measures</h3>
          <div class="metric-stat">
            <span class="label">Measures Met:</span>
            <span class="value met">{{ metrics.measuresMet }}/{{ metrics.totalMeasures }}</span>
          </div>
          <div class="progress-bar">
            <div class="progress" [style.width.%]="(metrics.measuresMet / metrics.totalMeasures * 100)"></div>
          </div>
        </div>

        <!-- Care Gaps Summary -->
        <div class="metric-card">
          <h3>Care Gaps</h3>
          <div class="metric-stat">
            <span class="label">Total Gaps:</span>
            <span class="value">{{ metrics.totalGaps }}</span>
          </div>
          <div class="metric-stat">
            <span class="label">Critical:</span>
            <span class="value critical">{{ metrics.criticalGaps }}</span>
          </div>
        </div>

        <!-- Population Info -->
        <div class="metric-card">
          <h3>Population</h3>
          <div class="metric-stat">
            <span class="label">Size:</span>
            <span class="value">{{ metrics.populationSize }}</span>
          </div>
          <button class="drill-down-btn">Drill Down</button>
        </div>
      </div>

      <!-- Summary Table -->
      <div *ngIf="metrics$ | async as metrics" class="summary-section">
        <h3>Performance Summary</h3>
        <table class="summary-table">
          <tr>
            <td>Care Readiness Score</td>
            <td>{{ metrics.careReadinessScore }}%</td>
          </tr>
          <tr>
            <td>Quality Measures Compliance</td>
            <td>{{ ((metrics.measuresMet / metrics.totalMeasures) * 100) | number: '1.0-0' }}%</td>
          </tr>
          <tr>
            <td>Open Care Gaps</td>
            <td>{{ metrics.totalGaps }}</td>
          </tr>
          <tr>
            <td>Critical Gaps</td>
            <td>{{ metrics.criticalGaps }}</td>
          </tr>
        </table>
      </div>

      <!-- Export Section -->
      <div class="export-section">
        <button (click)="exportToPDF()" class="export-btn">📥 Export to PDF</button>
        <button class="filter-btn">📅 Date Range Filter</button>
      </div>
    </div>
  `,
  styles: [`
    .reports-dashboard {
      padding: 20px;
      font-family: Arial, sans-serif;
      max-width: 1200px;
      margin: 0 auto;
    }

    h2 {
      color: #333;
      margin-bottom: 20px;
      border-bottom: 2px solid #007bff;
      padding-bottom: 10px;
    }

    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 20px;
      margin-bottom: 30px;
    }

    .metric-card {
      background: white;
      border-radius: 8px;
      padding: 20px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      border-top: 4px solid #007bff;
    }

    .metric-card h3 {
      margin: 0 0 15px 0;
      color: #333;
      font-size: 16px;
    }

    .score-card {
      border-top-color: #28a745;
      grid-column: span 2;
    }

    .score-display {
      font-size: 48px;
      font-weight: bold;
      color: #28a745;
      text-align: center;
      margin: 15px 0;
    }

    .score-bar {
      width: 100%;
      height: 20px;
      background: #eee;
      border-radius: 10px;
      overflow: hidden;
    }

    .score-fill {
      height: 100%;
      background: linear-gradient(90deg, #ffc107, #28a745);
      transition: width 0.3s;
    }

    .metric-stat {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      border-bottom: 1px solid #eee;
    }

    .label {
      font-weight: 600;
      color: #666;
    }

    .value {
      color: #333;
      font-weight: 600;
    }

    .value.met {
      color: #28a745;
    }

    .value.critical {
      color: #dc3545;
    }

    .progress-bar {
      width: 100%;
      height: 15px;
      background: #eee;
      border-radius: 8px;
      overflow: hidden;
      margin-top: 10px;
    }

    .progress {
      height: 100%;
      background: #007bff;
      transition: width 0.3s;
    }

    .drill-down-btn, .export-btn, .filter-btn {
      width: 100%;
      padding: 10px;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 600;
      margin-top: 10px;
      transition: all 0.3s;
    }

    .drill-down-btn {
      background: #007bff;
      color: white;
    }

    .drill-down-btn:hover {
      background: #0056b3;
    }

    .summary-section {
      background: white;
      border-radius: 8px;
      padding: 20px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      margin-bottom: 20px;
    }

    .summary-section h3 {
      margin: 0 0 15px 0;
      color: #333;
    }

    .summary-table {
      width: 100%;
      border-collapse: collapse;
    }

    .summary-table tr {
      border-bottom: 1px solid #eee;
    }

    .summary-table tr:last-child {
      border-bottom: none;
    }

    .summary-table td {
      padding: 12px;
      text-align: left;
    }

    .summary-table td:first-child {
      font-weight: 600;
      color: #666;
      width: 50%;
    }

    .summary-table td:last-child {
      text-align: right;
      color: #333;
      font-weight: 600;
    }

    .export-section {
      display: flex;
      gap: 10px;
      justify-content: center;
      margin-top: 20px;
    }

    .export-btn, .filter-btn {
      width: auto;
      padding: 10px 20px;
      background: #28a745;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 600;
    }

    .filter-btn {
      background: #6c757d;
    }

    .export-btn:hover {
      background: #218838;
    }

    .filter-btn:hover {
      background: #545b62;
    }
  `]
})
export class ReportsDashboardComponent implements OnInit {
  metrics$: Observable<ReportMetrics> | undefined;

  constructor(
    private pipeline: Clinical360PipelineService,
    private eventBus: EventBusService
  ) {}

  ngOnInit() {
    // Listen for patient selection events
    this.eventBus.on(ClinicalEventType.PATIENT_SELECTED).subscribe((event: any) => {
      this.loadDashboard(event.data.patientId);
    });
  }

  private loadDashboard(patientId: string) {
    this.metrics$ = this.pipeline.clinical360$.pipe(
      map(data => {
        if (!data) {
          return {
            careReadinessScore: 0,
            totalMeasures: 0,
            measuresMet: 0,
            totalGaps: 0,
            criticalGaps: 0,
            populationSize: 1,
          };
        }

        const totalMeasures = data.qualityMeasures?.measures?.length || 0;
        const measuresMet = data.qualityMeasures?.measures?.filter((m: any) => m.status === 'MET').length || 0;
        const totalGaps = data.careGaps?.gaps?.length || 0;
        const criticalGaps = data.careGaps?.gaps?.filter((g: any) => g.priority === 'HIGH').length || 0;

        // Scenario 1: Care Readiness Dashboard - Calculate composite score
        const measureScore = totalMeasures > 0 ? (measuresMet / totalMeasures) * 100 : 0;
        const gapScore = Math.max(0, 100 - (totalGaps * 10));
        const careReadinessScore = Math.round((measureScore * 0.6) + (gapScore * 0.4));

        return {
          careReadinessScore,
          totalMeasures,
          measuresMet,
          totalGaps,
          criticalGaps,
          populationSize: 1,
        };
      }),
      tap(metrics => {
        // Scenario 10: Real-time updates via EventBus
        this.eventBus.emit({
          type: ClinicalEventType.DATA_PIPELINE_READY,
          source: 'mfe-reports',
          data: {
            dashboardReady: true,
            careReadinessScore: metrics.careReadinessScore,
          },
        } as any);
      })
    );
  }

  exportToPDF() {
    // Scenario 6: Export to PDF
    console.log('Exporting dashboard to PDF...');
    alert('PDF export functionality ready for implementation');
  }
}
