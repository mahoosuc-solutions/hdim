import { Component, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatTableModule } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Router } from '@angular/router';
import { LoggerService } from '../../services/logger.service';

/**
 * Issue #153: Measure Performance Comparison Dashboard
 *
 * Allows providers to compare performance across custom measures:
 * - Side-by-side compliance rates for up to 5 measures
 * - Trend lines over last 12 months
 * - Patient overlap analysis (Venn diagram)
 * - Drill-down to patient lists per measure
 * - Export to PDF/PNG for reporting
 * - Benchmark comparison (CMS targets)
 */

interface MeasureData {
  id: string;
  code: string;
  name: string;
  category: string;
  complianceRate: number;
  denominatorCount: number;
  numeratorCount: number;
  benchmark: number;
  trend: 'up' | 'down' | 'stable';
  trendPercent: number;
  patientIds: string[];
  monthlyData: MonthlyDataPoint[];
}

interface MonthlyDataPoint {
  month: string;
  rate: number;
}

interface PatientOverlap {
  measureIds: string[];
  measureNames: string[];
  patientCount: number;
  patientIds: string[];
}

interface ComparisonMetrics {
  averageCompliance: number;
  bestPerforming: MeasureData | null;
  worstPerforming: MeasureData | null;
  totalUniquePatients: number;
  overlapPercentage: number;
}

@Component({
  selector: 'app-measure-comparison',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatTabsModule,
    MatSelectModule,
    MatFormFieldModule,
    MatMenuModule,
    MatDividerModule,
    MatTableModule,
    MatCheckboxModule,
  ],
  template: `
    <div class="page">
      <!-- Page Header -->
      <div class="page-header">
        <div class="title">
          <mat-icon color="primary">compare_arrows</mat-icon>
          <div>
            <h1>Measure Performance Comparison</h1>
            <p>Compare compliance rates and trends across your quality measures</p>
          </div>
        </div>
        <div class="header-actions">
          <button mat-stroked-button [matMenuTriggerFor]="exportMenu">
            <mat-icon>download</mat-icon>
            Export
          </button>
          <mat-menu #exportMenu="matMenu">
            <button mat-menu-item (click)="exportToPdf()">
              <mat-icon>picture_as_pdf</mat-icon>
              Export as PDF
            </button>
            <button mat-menu-item (click)="exportToPng()">
              <mat-icon>image</mat-icon>
              Export as PNG
            </button>
            <button mat-menu-item (click)="exportToCsv()">
              <mat-icon>table_chart</mat-icon>
              Export as CSV
            </button>
          </mat-menu>
        </div>
      </div>

      <!-- Measure Selection -->
      <mat-card class="selection-card">
        <mat-card-header>
          <mat-card-title>Select Measures to Compare</mat-card-title>
          <mat-card-subtitle>Choose up to 5 measures for comparison</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <div class="measure-selector">
            <mat-form-field appearance="outline" class="measure-select">
              <mat-label>Add Measure</mat-label>
              <mat-select [(ngModel)]="selectedMeasureToAdd" (selectionChange)="addMeasure($event.value)">
                @for (measure of availableMeasures(); track measure.id) {
                  <mat-option [value]="measure.id" [disabled]="isMeasureSelected(measure.id)">
                    {{ measure.code }} - {{ measure.name }}
                  </mat-option>
                }
              </mat-select>
            </mat-form-field>

            <div class="selected-measures">
              @for (measure of selectedMeasures(); track measure.id) {
                <mat-chip class="measure-chip" [style.background-color]="getMeasureColor(measure.id)">
                  <span class="chip-text">{{ measure.code }}</span>
                  <button matChipRemove (click)="removeMeasure(measure.id)">
                    <mat-icon>cancel</mat-icon>
                  </button>
                </mat-chip>
              }
              @if (selectedMeasures().length === 0) {
                <span class="no-selection">No measures selected. Add up to 5 measures to compare.</span>
              }
            </div>
          </div>
        </mat-card-content>
      </mat-card>

      @if (selectedMeasures().length >= 2) {
        <!-- Comparison Summary -->
        <div class="summary-cards">
          <mat-card class="summary-card">
            <div class="card-icon">
              <mat-icon>speed</mat-icon>
            </div>
            <div class="card-content">
              <div class="card-value">{{ metrics().averageCompliance | number:'1.1-1' }}%</div>
              <div class="card-label">Avg Compliance</div>
            </div>
          </mat-card>

          <mat-card class="summary-card best">
            <div class="card-icon">
              <mat-icon>trending_up</mat-icon>
            </div>
            <div class="card-content">
              <div class="card-value">{{ metrics().bestPerforming?.code || '-' }}</div>
              <div class="card-label">Best Performing</div>
              <div class="card-subvalue">{{ metrics().bestPerforming?.complianceRate | number:'1.1-1' }}%</div>
            </div>
          </mat-card>

          <mat-card class="summary-card worst">
            <div class="card-icon">
              <mat-icon>trending_down</mat-icon>
            </div>
            <div class="card-content">
              <div class="card-value">{{ metrics().worstPerforming?.code || '-' }}</div>
              <div class="card-label">Needs Attention</div>
              <div class="card-subvalue">{{ metrics().worstPerforming?.complianceRate | number:'1.1-1' }}%</div>
            </div>
          </mat-card>

          <mat-card class="summary-card">
            <div class="card-icon">
              <mat-icon>groups</mat-icon>
            </div>
            <div class="card-content">
              <div class="card-value">{{ metrics().totalUniquePatients | number }}</div>
              <div class="card-label">Unique Patients</div>
              <div class="card-subvalue">{{ metrics().overlapPercentage | number:'1.0-0' }}% overlap</div>
            </div>
          </mat-card>
        </div>

        <!-- Main Comparison Content -->
        <mat-card class="comparison-card">
          <mat-tab-group animationDuration="200ms">
            <!-- Compliance Rates Tab -->
            <mat-tab>
              <ng-template mat-tab-label>
                <mat-icon class="tab-icon">bar_chart</mat-icon>
                Compliance Rates
              </ng-template>

              <div class="tab-content">
                <div class="chart-container">
                  <h3>Compliance Rate Comparison</h3>
                  <div class="bar-chart">
                    @for (measure of selectedMeasures(); track measure.id) {
                      <div class="bar-row">
                        <div class="bar-label">
                          <span class="measure-code" [style.color]="getMeasureColor(measure.id)">
                            {{ measure.code }}
                          </span>
                          <span class="measure-name">{{ measure.name }}</span>
                        </div>
                        <div class="bar-container">
                          <div class="bar"
                               [style.width.%]="measure.complianceRate"
                               [style.background-color]="getMeasureColor(measure.id)">
                          </div>
                          <div class="benchmark-line" [style.left.%]="measure.benchmark"></div>
                          <span class="bar-value">{{ measure.complianceRate | number:'1.1-1' }}%</span>
                        </div>
                        <div class="bar-stats">
                          <span class="numerator">{{ measure.numeratorCount | number }}/{{ measure.denominatorCount | number }}</span>
                          <span class="benchmark">Target: {{ measure.benchmark }}%</span>
                        </div>
                      </div>
                    }
                  </div>
                  <div class="chart-legend">
                    <div class="legend-item">
                      <div class="legend-line benchmark"></div>
                      <span>CMS Benchmark Target</span>
                    </div>
                  </div>
                </div>
              </div>
            </mat-tab>

            <!-- Trends Tab -->
            <mat-tab>
              <ng-template mat-tab-label>
                <mat-icon class="tab-icon">show_chart</mat-icon>
                12-Month Trends
              </ng-template>

              <div class="tab-content">
                <div class="chart-container">
                  <h3>Performance Trends (Last 12 Months)</h3>
                  <div class="trend-chart">
                    <!-- Simple SVG Line Chart -->
                    <svg viewBox="0 0 800 400" class="line-chart-svg">
                      <!-- Grid lines -->
                      @for (i of [0, 1, 2, 3, 4]; track i) {
                        <line [attr.x1]="50" [attr.y1]="50 + i * 75"
                              [attr.x2]="750" [attr.y2]="50 + i * 75"
                              class="grid-line"/>
                        <text [attr.x]="45" [attr.y]="55 + i * 75" class="axis-label">
                          {{ 100 - i * 25 }}%
                        </text>
                      }

                      <!-- X-axis labels -->
                      @for (month of monthLabels; track month; let idx = $index) {
                        <text [attr.x]="75 + idx * 60" [attr.y]="380" class="axis-label x-axis">
                          {{ month }}
                        </text>
                      }

                      <!-- Lines for each measure -->
                      @for (measure of selectedMeasures(); track measure.id; let mIdx = $index) {
                        <polyline
                          [attr.points]="getTrendLinePoints(measure)"
                          fill="none"
                          [attr.stroke]="getMeasureColor(measure.id)"
                          stroke-width="3"
                          class="trend-line"/>

                        <!-- Data points -->
                        @for (point of measure.monthlyData; track point.month; let pIdx = $index) {
                          <circle
                            [attr.cx]="75 + pIdx * 60"
                            [attr.cy]="350 - (point.rate * 3)"
                            r="5"
                            [attr.fill]="getMeasureColor(measure.id)"
                            class="data-point"/>
                        }
                      }
                    </svg>
                  </div>

                  <!-- Legend -->
                  <div class="trend-legend">
                    @for (measure of selectedMeasures(); track measure.id) {
                      <div class="legend-item">
                        <div class="legend-color" [style.background-color]="getMeasureColor(measure.id)"></div>
                        <span>{{ measure.code }} - {{ measure.name }}</span>
                        <span class="trend-indicator" [class]="measure.trend">
                          <mat-icon>{{ getTrendIcon(measure.trend) }}</mat-icon>
                          {{ measure.trendPercent > 0 ? '+' : '' }}{{ measure.trendPercent }}%
                        </span>
                      </div>
                    }
                  </div>
                </div>
              </div>
            </mat-tab>

            <!-- Patient Overlap Tab -->
            <mat-tab>
              <ng-template mat-tab-label>
                <mat-icon class="tab-icon">hub</mat-icon>
                Patient Overlap
              </ng-template>

              <div class="tab-content">
                <div class="overlap-container">
                  <h3>Patient Overlap Analysis</h3>
                  <p class="overlap-description">
                    Shows patients who appear in multiple measure denominators.
                    Click any segment to view the patient list.
                  </p>

                  <!-- Venn-style visualization -->
                  <div class="venn-container">
                    @for (measure of selectedMeasures(); track measure.id; let i = $index) {
                      <div class="venn-circle"
                           [style.background-color]="getMeasureColor(measure.id) + '40'"
                           [style.border-color]="getMeasureColor(measure.id)"
                           [style.left.px]="100 + (i * 80)"
                           [style.top.px]="50 + ((i % 2) * 60)"
                           (click)="showPatientsForMeasure(measure)">
                        <div class="venn-label">
                          <span class="code">{{ measure.code }}</span>
                          <span class="count">{{ measure.denominatorCount }}</span>
                        </div>
                      </div>
                    }

                    <!-- Center overlap indicator -->
                    @if (selectedMeasures().length >= 2) {
                      <div class="overlap-center" (click)="showOverlapPatients()">
                        <mat-icon>people</mat-icon>
                        <span>{{ getOverlapCount() }}</span>
                        <span class="overlap-label">in all</span>
                      </div>
                    }
                  </div>

                  <!-- Overlap Matrix -->
                  <div class="overlap-matrix">
                    <h4>Overlap Matrix</h4>
                    <table class="matrix-table">
                      <thead>
                        <tr>
                          <th></th>
                          @for (measure of selectedMeasures(); track measure.id) {
                            <th [style.color]="getMeasureColor(measure.id)">{{ measure.code }}</th>
                          }
                        </tr>
                      </thead>
                      <tbody>
                        @for (rowMeasure of selectedMeasures(); track rowMeasure.id; let rowIdx = $index) {
                          <tr>
                            <td class="row-header" [style.color]="getMeasureColor(rowMeasure.id)">
                              {{ rowMeasure.code }}
                            </td>
                            @for (colMeasure of selectedMeasures(); track colMeasure.id; let colIdx = $index) {
                              <td [class.diagonal]="rowIdx === colIdx"
                                  [class.clickable]="rowIdx !== colIdx"
                                  (click)="rowIdx !== colIdx && showPairOverlap(rowMeasure, colMeasure)">
                                @if (rowIdx === colIdx) {
                                  {{ rowMeasure.denominatorCount }}
                                } @else {
                                  {{ getPairOverlap(rowMeasure, colMeasure) }}
                                }
                              </td>
                            }
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </mat-tab>

            <!-- Detailed Table Tab -->
            <mat-tab>
              <ng-template mat-tab-label>
                <mat-icon class="tab-icon">table_chart</mat-icon>
                Detailed Data
              </ng-template>

              <div class="tab-content">
                <div class="table-container">
                  <table mat-table [dataSource]="selectedMeasures()" class="comparison-table">
                    <!-- Code Column -->
                    <ng-container matColumnDef="code">
                      <th mat-header-cell *matHeaderCellDef>Code</th>
                      <td mat-cell *matCellDef="let measure" [style.color]="getMeasureColor(measure.id)">
                        <strong>{{ measure.code }}</strong>
                      </td>
                    </ng-container>

                    <!-- Name Column -->
                    <ng-container matColumnDef="name">
                      <th mat-header-cell *matHeaderCellDef>Measure Name</th>
                      <td mat-cell *matCellDef="let measure">{{ measure.name }}</td>
                    </ng-container>

                    <!-- Category Column -->
                    <ng-container matColumnDef="category">
                      <th mat-header-cell *matHeaderCellDef>Category</th>
                      <td mat-cell *matCellDef="let measure">{{ measure.category }}</td>
                    </ng-container>

                    <!-- Compliance Column -->
                    <ng-container matColumnDef="compliance">
                      <th mat-header-cell *matHeaderCellDef>Compliance</th>
                      <td mat-cell *matCellDef="let measure">
                        <div class="compliance-cell">
                          <span class="rate">{{ measure.complianceRate | number:'1.1-1' }}%</span>
                          <mat-progress-bar mode="determinate" [value]="measure.complianceRate"
                                           [color]="measure.complianceRate >= measure.benchmark ? 'primary' : 'warn'">
                          </mat-progress-bar>
                        </div>
                      </td>
                    </ng-container>

                    <!-- Numerator/Denominator Column -->
                    <ng-container matColumnDef="counts">
                      <th mat-header-cell *matHeaderCellDef>Num / Denom</th>
                      <td mat-cell *matCellDef="let measure">
                        {{ measure.numeratorCount | number }} / {{ measure.denominatorCount | number }}
                      </td>
                    </ng-container>

                    <!-- Benchmark Column -->
                    <ng-container matColumnDef="benchmark">
                      <th mat-header-cell *matHeaderCellDef>Benchmark</th>
                      <td mat-cell *matCellDef="let measure">
                        <span [class.above]="measure.complianceRate >= measure.benchmark"
                              [class.below]="measure.complianceRate < measure.benchmark">
                          {{ measure.benchmark }}%
                          @if (measure.complianceRate >= measure.benchmark) {
                            <mat-icon class="status-icon">check_circle</mat-icon>
                          } @else {
                            <mat-icon class="status-icon">warning</mat-icon>
                          }
                        </span>
                      </td>
                    </ng-container>

                    <!-- Trend Column -->
                    <ng-container matColumnDef="trend">
                      <th mat-header-cell *matHeaderCellDef>Trend</th>
                      <td mat-cell *matCellDef="let measure">
                        <span class="trend-badge" [class]="measure.trend">
                          <mat-icon>{{ getTrendIcon(measure.trend) }}</mat-icon>
                          {{ measure.trendPercent > 0 ? '+' : '' }}{{ measure.trendPercent }}%
                        </span>
                      </td>
                    </ng-container>

                    <!-- Actions Column -->
                    <ng-container matColumnDef="actions">
                      <th mat-header-cell *matHeaderCellDef>Actions</th>
                      <td mat-cell *matCellDef="let measure">
                        <button mat-icon-button matTooltip="View patients" (click)="showPatientsForMeasure(measure)">
                          <mat-icon>people</mat-icon>
                        </button>
                        <button mat-icon-button matTooltip="View measure details" (click)="viewMeasureDetails(measure)">
                          <mat-icon>open_in_new</mat-icon>
                        </button>
                      </td>
                    </ng-container>

                    <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                    <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
                  </table>
                </div>
              </div>
            </mat-tab>
          </mat-tab-group>
        </mat-card>
      } @else {
        <!-- Empty State -->
        <mat-card class="empty-state">
          <mat-icon>compare_arrows</mat-icon>
          <h2>Select Measures to Compare</h2>
          <p>Add at least 2 measures using the selector above to see comparison charts, trends, and patient overlap analysis.</p>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .page {
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 24px;
      max-width: 1600px;
      margin: 0 auto;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;
    }

    .title {
      display: flex;
      align-items: center;
      gap: 12px;

      mat-icon {
        font-size: 36px;
        width: 36px;
        height: 36px;
      }

      h1 {
        margin: 0;
        font-size: 24px;
        font-weight: 500;
      }

      p {
        margin: 4px 0 0;
        color: rgba(0, 0, 0, 0.6);
        font-size: 14px;
      }
    }

    .selection-card {
      mat-card-content {
        padding-top: 16px;
      }
    }

    .measure-selector {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .measure-select {
      width: 100%;
      max-width: 500px;
    }

    .selected-measures {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      min-height: 40px;
      align-items: center;
    }

    .measure-chip {
      color: white;
      font-weight: 500;
    }

    .no-selection {
      color: rgba(0, 0, 0, 0.5);
      font-style: italic;
    }

    .summary-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
    }

    .summary-card {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 20px;

      .card-icon {
        width: 48px;
        height: 48px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: rgba(63, 81, 181, 0.1);

        mat-icon {
          font-size: 28px;
          width: 28px;
          height: 28px;
          color: #3f51b5;
        }
      }

      &.best .card-icon {
        background: rgba(76, 175, 80, 0.1);
        mat-icon { color: #4caf50; }
      }

      &.worst .card-icon {
        background: rgba(244, 67, 54, 0.1);
        mat-icon { color: #f44336; }
      }

      .card-value {
        font-size: 24px;
        font-weight: 600;
        color: #333;
      }

      .card-label {
        font-size: 12px;
        color: rgba(0, 0, 0, 0.6);
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }

      .card-subvalue {
        font-size: 12px;
        color: rgba(0, 0, 0, 0.5);
        margin-top: 2px;
      }
    }

    .comparison-card {
      mat-tab-group {
        min-height: 500px;
      }
    }

    .tab-icon {
      margin-right: 8px;
    }

    .tab-content {
      padding: 24px;
    }

    .chart-container {
      h3 {
        margin: 0 0 24px;
        font-size: 18px;
        font-weight: 500;
      }
    }

    /* Bar Chart Styles */
    .bar-chart {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }

    .bar-row {
      display: grid;
      grid-template-columns: 200px 1fr 150px;
      gap: 16px;
      align-items: center;
    }

    .bar-label {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .measure-code {
        font-weight: 600;
        font-size: 14px;
      }

      .measure-name {
        font-size: 12px;
        color: rgba(0, 0, 0, 0.6);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .bar-container {
      position: relative;
      height: 32px;
      background: #f5f5f5;
      border-radius: 4px;
      overflow: hidden;

      .bar {
        height: 100%;
        border-radius: 4px;
        transition: width 0.5s ease;
      }

      .benchmark-line {
        position: absolute;
        top: 0;
        bottom: 0;
        width: 3px;
        background: #333;
        opacity: 0.7;

        &::after {
          content: '';
          position: absolute;
          top: -4px;
          left: -4px;
          width: 11px;
          height: 11px;
          background: #333;
          border-radius: 50%;
        }
      }

      .bar-value {
        position: absolute;
        right: 8px;
        top: 50%;
        transform: translateY(-50%);
        font-weight: 600;
        font-size: 14px;
        color: #333;
      }
    }

    .bar-stats {
      display: flex;
      flex-direction: column;
      gap: 2px;
      font-size: 12px;

      .numerator {
        color: rgba(0, 0, 0, 0.7);
      }

      .benchmark {
        color: rgba(0, 0, 0, 0.5);
      }
    }

    .chart-legend {
      display: flex;
      gap: 24px;
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #eee;

      .legend-item {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 12px;
        color: rgba(0, 0, 0, 0.6);
      }

      .legend-line {
        width: 24px;
        height: 3px;
        border-radius: 2px;

        &.benchmark {
          background: #333;
        }
      }
    }

    /* Trend Chart Styles */
    .trend-chart {
      background: #fafafa;
      border-radius: 8px;
      padding: 16px;
    }

    .line-chart-svg {
      width: 100%;
      max-height: 400px;

      .grid-line {
        stroke: #e0e0e0;
        stroke-width: 1;
      }

      .axis-label {
        font-size: 12px;
        fill: rgba(0, 0, 0, 0.5);
        text-anchor: end;

        &.x-axis {
          text-anchor: middle;
        }
      }

      .trend-line {
        stroke-linecap: round;
        stroke-linejoin: round;
      }

      .data-point {
        cursor: pointer;

        &:hover {
          r: 7;
        }
      }
    }

    .trend-legend {
      display: flex;
      flex-wrap: wrap;
      gap: 16px;
      margin-top: 16px;

      .legend-item {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 13px;

        .legend-color {
          width: 16px;
          height: 4px;
          border-radius: 2px;
        }

        .trend-indicator {
          display: flex;
          align-items: center;
          gap: 2px;
          font-size: 12px;
          padding: 2px 6px;
          border-radius: 4px;

          mat-icon {
            font-size: 14px;
            width: 14px;
            height: 14px;
          }

          &.up {
            background: rgba(76, 175, 80, 0.1);
            color: #4caf50;
          }

          &.down {
            background: rgba(244, 67, 54, 0.1);
            color: #f44336;
          }

          &.stable {
            background: rgba(158, 158, 158, 0.1);
            color: #9e9e9e;
          }
        }
      }
    }

    /* Venn Diagram Styles */
    .overlap-container {
      h3 {
        margin: 0 0 8px;
        font-size: 18px;
        font-weight: 500;
      }

      .overlap-description {
        margin: 0 0 24px;
        color: rgba(0, 0, 0, 0.6);
        font-size: 14px;
      }
    }

    .venn-container {
      position: relative;
      height: 300px;
      background: #fafafa;
      border-radius: 8px;
      margin-bottom: 24px;
    }

    .venn-circle {
      position: absolute;
      width: 180px;
      height: 180px;
      border-radius: 50%;
      border: 3px solid;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: transform 0.2s, box-shadow 0.2s;

      &:hover {
        transform: scale(1.05);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      }

      .venn-label {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;

        .code {
          font-weight: 600;
          font-size: 14px;
        }

        .count {
          font-size: 24px;
          font-weight: 700;
        }
      }
    }

    .overlap-center {
      position: absolute;
      left: 50%;
      top: 50%;
      transform: translate(-50%, -50%);
      width: 80px;
      height: 80px;
      border-radius: 50%;
      background: rgba(63, 81, 181, 0.9);
      color: white;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
      transition: transform 0.2s;

      &:hover {
        transform: translate(-50%, -50%) scale(1.1);
      }

      mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }

      span {
        font-size: 16px;
        font-weight: 600;
      }

      .overlap-label {
        font-size: 10px;
        opacity: 0.8;
      }
    }

    /* Overlap Matrix */
    .overlap-matrix {
      h4 {
        margin: 0 0 16px;
        font-size: 16px;
        font-weight: 500;
      }
    }

    .matrix-table {
      width: 100%;
      border-collapse: collapse;
      background: white;
      border-radius: 8px;
      overflow: hidden;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);

      th, td {
        padding: 12px 16px;
        text-align: center;
        border: 1px solid #eee;
      }

      th {
        background: #fafafa;
        font-weight: 600;
      }

      .row-header {
        font-weight: 600;
        background: #fafafa;
        text-align: left;
      }

      .diagonal {
        background: #f5f5f5;
        font-weight: 600;
      }

      .clickable {
        cursor: pointer;
        transition: background 0.2s;

        &:hover {
          background: rgba(63, 81, 181, 0.1);
        }
      }
    }

    /* Detailed Table */
    .table-container {
      overflow-x: auto;
    }

    .comparison-table {
      width: 100%;

      .compliance-cell {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .rate {
          font-weight: 600;
        }

        mat-progress-bar {
          height: 6px;
          border-radius: 3px;
        }
      }

      .above {
        color: #4caf50;
      }

      .below {
        color: #f44336;
      }

      .status-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
        vertical-align: middle;
        margin-left: 4px;
      }

      .trend-badge {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        padding: 4px 8px;
        border-radius: 4px;
        font-size: 12px;

        mat-icon {
          font-size: 14px;
          width: 14px;
          height: 14px;
        }

        &.up {
          background: rgba(76, 175, 80, 0.1);
          color: #4caf50;
        }

        &.down {
          background: rgba(244, 67, 54, 0.1);
          color: #f44336;
        }

        &.stable {
          background: rgba(158, 158, 158, 0.1);
          color: #9e9e9e;
        }
      }
    }

    /* Empty State */
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 64px 24px;
      text-align: center;

      mat-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        color: rgba(0, 0, 0, 0.2);
        margin-bottom: 16px;
      }

      h2 {
        margin: 0 0 8px;
        font-size: 20px;
        font-weight: 500;
        color: rgba(0, 0, 0, 0.7);
      }

      p {
        margin: 0;
        color: rgba(0, 0, 0, 0.5);
        max-width: 400px;
      }
    }
  `],
})
export class MeasureComparisonComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  // Measure colors for consistent visualization
  private measureColors = ['#3f51b5', '#e91e63', '#00bcd4', '#ff9800', '#4caf50'];

  // Table columns
  displayedColumns = ['code', 'name', 'category', 'compliance', 'counts', 'benchmark', 'trend', 'actions'];

  // Month labels for trend chart
  monthLabels = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

  // State
  selectedMeasureToAdd: string | null = null;
  availableMeasures = signal<MeasureData[]>([]);
  selectedMeasures = signal<MeasureData[]>([]);

  // Computed metrics
  metrics = computed<ComparisonMetrics>(() => {
    const measures = this.selectedMeasures();
    if (measures.length === 0) {
      return {
        averageCompliance: 0,
        bestPerforming: null,
        worstPerforming: null,
        totalUniquePatients: 0,
        overlapPercentage: 0,
      };
    }

    const avgCompliance = measures.reduce((sum, m) => sum + m.complianceRate, 0) / measures.length;
    const sorted = [...measures].sort((a, b) => b.complianceRate - a.complianceRate);

    // Calculate unique patients and overlap
    const allPatientIds = new Set<string>();
    let totalPatients = 0;
    measures.forEach(m => {
      m.patientIds.forEach(id => allPatientIds.add(id));
      totalPatients += m.patientIds.length;
    });

    const uniquePatients = allPatientIds.size;
    const overlapPercentage = totalPatients > 0
      ? ((totalPatients - uniquePatients) / totalPatients) * 100
      : 0;

    return {
      averageCompliance: avgCompliance,
      bestPerforming: sorted[0],
      worstPerforming: sorted[sorted.length - 1],
      totalUniquePatients: uniquePatients,
      overlapPercentage,
    };
  });

  private get logger() {
    return this.loggerService.withContext('MeasureComparisonComponent');
  }

  constructor(
    private router: Router,
    private loggerService: LoggerService
  ) {}

  ngOnInit(): void {
    this.loadAvailableMeasures();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load available measures for selection
   */
  private loadAvailableMeasures(): void {
    // Sample data - in production would fetch from quality-measure-service
    const sampleMeasures: MeasureData[] = [
      {
        id: 'bcs',
        code: 'BCS',
        name: 'Breast Cancer Screening',
        category: 'Screening',
        complianceRate: 78.5,
        denominatorCount: 1250,
        numeratorCount: 981,
        benchmark: 75,
        trend: 'up',
        trendPercent: 3.2,
        patientIds: this.generatePatientIds(1250),
        monthlyData: this.generateMonthlyData(78.5, 3.2),
      },
      {
        id: 'col',
        code: 'COL',
        name: 'Colorectal Cancer Screening',
        category: 'Screening',
        complianceRate: 65.2,
        denominatorCount: 2100,
        numeratorCount: 1369,
        benchmark: 70,
        trend: 'down',
        trendPercent: -2.1,
        patientIds: this.generatePatientIds(2100),
        monthlyData: this.generateMonthlyData(65.2, -2.1),
      },
      {
        id: 'cdc-hba1c',
        code: 'CDC-HbA1c',
        name: 'Diabetes Care - HbA1c Control',
        category: 'Chronic Disease',
        complianceRate: 71.8,
        denominatorCount: 890,
        numeratorCount: 639,
        benchmark: 80,
        trend: 'stable',
        trendPercent: 0.5,
        patientIds: this.generatePatientIds(890),
        monthlyData: this.generateMonthlyData(71.8, 0.5),
      },
      {
        id: 'cdc-eye',
        code: 'CDC-Eye',
        name: 'Diabetes Care - Eye Exam',
        category: 'Chronic Disease',
        complianceRate: 58.3,
        denominatorCount: 890,
        numeratorCount: 519,
        benchmark: 65,
        trend: 'up',
        trendPercent: 4.7,
        patientIds: this.generatePatientIds(890),
        monthlyData: this.generateMonthlyData(58.3, 4.7),
      },
      {
        id: 'cbp',
        code: 'CBP',
        name: 'Controlling Blood Pressure',
        category: 'Chronic Disease',
        complianceRate: 82.1,
        denominatorCount: 3200,
        numeratorCount: 2627,
        benchmark: 75,
        trend: 'up',
        trendPercent: 1.8,
        patientIds: this.generatePatientIds(3200),
        monthlyData: this.generateMonthlyData(82.1, 1.8),
      },
      {
        id: 'ccs',
        code: 'CCS',
        name: 'Cervical Cancer Screening',
        category: 'Screening',
        complianceRate: 69.4,
        denominatorCount: 1800,
        numeratorCount: 1249,
        benchmark: 72,
        trend: 'stable',
        trendPercent: -0.3,
        patientIds: this.generatePatientIds(1800),
        monthlyData: this.generateMonthlyData(69.4, -0.3),
      },
      {
        id: 'spc',
        code: 'SPC',
        name: 'Statin Therapy for CVD',
        category: 'Medication Adherence',
        complianceRate: 76.9,
        denominatorCount: 1450,
        numeratorCount: 1115,
        benchmark: 80,
        trend: 'up',
        trendPercent: 2.4,
        patientIds: this.generatePatientIds(1450),
        monthlyData: this.generateMonthlyData(76.9, 2.4),
      },
    ];

    this.availableMeasures.set(sampleMeasures);
  }

  /**
   * Generate sample patient IDs for overlap simulation
   */
  private generatePatientIds(count: number): string[] {
    const ids: string[] = [];
    // Create some overlap by using predictable ID ranges
    const startId = Math.floor(Math.random() * 1000);
    for (let i = 0; i < count; i++) {
      ids.push(`patient-${startId + i}`);
    }
    return ids;
  }

  /**
   * Generate monthly trend data
   */
  private generateMonthlyData(currentRate: number, trendPercent: number): MonthlyDataPoint[] {
    const data: MonthlyDataPoint[] = [];
    const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

    for (let i = 0; i < 12; i++) {
      const variance = (Math.random() - 0.5) * 5;
      const progressToNow = i / 11;
      const rate = currentRate - (trendPercent * (1 - progressToNow)) + variance;
      data.push({
        month: monthNames[i],
        rate: Math.max(0, Math.min(100, rate)),
      });
    }
    return data;
  }

  /**
   * Add a measure to comparison
   */
  addMeasure(measureId: string): void {
    const measure = this.availableMeasures().find(m => m.id === measureId);
    if (measure && this.selectedMeasures().length < 5) {
      this.selectedMeasures.update(measures => [...measures, measure]);
    }
    this.selectedMeasureToAdd = null;
  }

  /**
   * Remove a measure from comparison
   */
  removeMeasure(measureId: string): void {
    this.selectedMeasures.update(measures => measures.filter(m => m.id !== measureId));
  }

  /**
   * Check if measure is already selected
   */
  isMeasureSelected(measureId: string): boolean {
    return this.selectedMeasures().some(m => m.id === measureId);
  }

  /**
   * Get consistent color for a measure
   */
  getMeasureColor(measureId: string): string {
    const index = this.selectedMeasures().findIndex(m => m.id === measureId);
    return this.measureColors[index % this.measureColors.length];
  }

  /**
   * Get trend icon
   */
  getTrendIcon(trend: string): string {
    switch (trend) {
      case 'up': return 'trending_up';
      case 'down': return 'trending_down';
      default: return 'trending_flat';
    }
  }

  /**
   * Get SVG polyline points for trend line
   */
  getTrendLinePoints(measure: MeasureData): string {
    return measure.monthlyData
      .map((point, index) => `${75 + index * 60},${350 - (point.rate * 3)}`)
      .join(' ');
  }

  /**
   * Calculate overlap count between all selected measures
   */
  getOverlapCount(): number {
    const measures = this.selectedMeasures();
    if (measures.length < 2) return 0;

    // Find patients in ALL measures
    let commonPatients = new Set(measures[0].patientIds);
    for (let i = 1; i < measures.length; i++) {
      const measureIds = new Set(measures[i].patientIds);
      commonPatients = new Set([...commonPatients].filter(id => measureIds.has(id)));
    }
    return commonPatients.size;
  }

  /**
   * Get overlap between two specific measures
   */
  getPairOverlap(measure1: MeasureData, measure2: MeasureData): number {
    const set1 = new Set(measure1.patientIds);
    return measure2.patientIds.filter(id => set1.has(id)).length;
  }

  /**
   * Show patients for a specific measure
   */
  showPatientsForMeasure(measure: MeasureData): void {
    // Navigate to patients page with filter
    this.router.navigate(['/patients'], {
      queryParams: { measureId: measure.id, measureCode: measure.code },
    });
  }

  /**
   * Show overlap patients
   */
  showOverlapPatients(): void {
    // Would open dialog or navigate with overlap patient IDs
    this.logger.info('Showing overlap patients');
  }

  /**
   * Show pair overlap
   */
  showPairOverlap(measure1: MeasureData, measure2: MeasureData): void {
    this.logger.info('Showing overlap between measures', `${measure1.code} and ${measure2.code}`);
  }

  /**
   * View measure details
   */
  viewMeasureDetails(measure: MeasureData): void {
    this.router.navigate(['/quality-measures', measure.id]);
  }

  /**
   * Export to PDF
   */
  exportToPdf(): void {
    this.logger.info('Exporting to PDF');
    // Would use a library like jsPDF or html2pdf
  }

  /**
   * Export to PNG
   */
  exportToPng(): void {
    this.logger.info('Exporting to PNG');
    // Would use html2canvas
  }

  /**
   * Export to CSV
   */
  exportToCsv(): void {
    const measures = this.selectedMeasures();
    if (measures.length === 0) return;

    const headers = ['Code', 'Name', 'Category', 'Compliance Rate', 'Numerator', 'Denominator', 'Benchmark', 'Trend'];
    const rows = measures.map(m => [
      m.code,
      m.name,
      m.category,
      m.complianceRate.toFixed(1) + '%',
      m.numeratorCount.toString(),
      m.denominatorCount.toString(),
      m.benchmark + '%',
      `${m.trend} (${m.trendPercent > 0 ? '+' : ''}${m.trendPercent}%)`,
    ]);

    const csvContent = [headers, ...rows]
      .map(row => row.join(','))
      .join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'measure-comparison.csv';
    a.click();
    window.URL.revokeObjectURL(url);
  }
}
