import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable, BehaviorSubject, combineLatest, Subject } from 'rxjs';
import { map, tap, shareReplay, takeUntil } from 'rxjs/operators';
import { Clinical360PipelineService } from '@health-platform/shared/data-access';
import { EventBusService, ClinicalEventType } from '@health-platform/shared/data-access';

export interface QualityMeasure {
  id: string;
  name: string;
  description: string;
  status: 'MET' | 'NOT_MET' | 'EXCLUDED';
  target?: number;
  actual?: number;
  population?: number;
}

@Component({
  selector: 'app-quality-measures',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="quality-measures-container">
      <h2>Quality Measures</h2>

      @if (statuses?.length) {
        <div class="filters">
          @for (status of statuses; track status) {
            <button
              type="button"
              (click)="filterByStatus(status)"
              [class.active]="activeFilter === status"
              class="filter-btn"
            >
              {{ status }}
            </button>
          }
        </div>
      }

      @if (filteredMeasures$ | async; as measures) {
        <div class="measures-list">
          @if (measures.length > 0) {
            <div class="table-container">
              <table class="measures-table">
                <thead>
                  <tr>
                    <th>Measure Name</th>
                    <th>Status</th>
                    <th>Population</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  @for (measure of measures; track measure.id) {
                    <tr [class]="'status-' + measure.status">
                      <td class="measure-name">
                        <button type="button" (click)="showDetail(measure)" class="measure-link">
                          {{ measure.name }}
                        </button>
                      </td>
                      <td class="status">
                        <span [class]="'badge-' + measure.status">
                          {{ measure.status }}
                        </span>
                      </td>
                      <td>{{ measure.population || '-' }}</td>
                      <td>
                        <button type="button" (click)="showDetail(measure)" class="detail-btn">View</button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          } @else {
            <div class="no-data">No quality measures available</div>
          }
        </div>
      } @else {
        <div class="loading">Loading quality measures...</div>
      }

      @if (selectedMeasure$ | async; as measure) {
        <div class="detail-panel">
          <div class="detail-content">
            <button type="button" (click)="closeDetail()" class="close-btn">×</button>
            <h3>{{ measure.name }}</h3>
            <p>{{ measure.description }}</p>
            <div class="detail-metrics">
              <div>Status: <strong>{{ measure.status }}</strong></div>
              @if (measure.target) {
                <div>Target: <strong>{{ measure.target }}</strong></div>
              }
              @if (measure.actual) {
                <div>Actual: <strong>{{ measure.actual }}</strong></div>
              }
              @if (measure.population) {
                <div>Population: <strong>{{ measure.population }}</strong></div>
              }
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .quality-measures-container {
      padding: 20px;
      font-family: Arial, sans-serif;
    }

    h2 {
      margin-bottom: 20px;
      color: #333;
    }

    .filters {
      margin-bottom: 20px;
      display: flex;
      gap: 10px;
    }

    .filter-btn {
      padding: 8px 16px;
      border: 1px solid #ddd;
      background: white;
      cursor: pointer;
      border-radius: 4px;
      transition: all 0.3s;
    }

    .filter-btn.active {
      background: #007bff;
      color: white;
      border-color: #007bff;
    }

    .measures-table {
      width: 100%;
      border-collapse: collapse;
      background: white;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }

    .measures-table thead {
      background: #f5f5f5;
      border-bottom: 2px solid #ddd;
    }

    .measures-table th {
      padding: 12px;
      text-align: left;
      font-weight: 600;
      color: #333;
    }

    .measures-table td {
      padding: 12px;
      border-bottom: 1px solid #eee;
    }

    .measure-name {
      color: #007bff;
    }

    .measure-link {
      padding: 0;
      border: none;
      background: none;
      cursor: pointer;
      font: inherit;
      color: inherit;
      text-decoration: underline;
      text-align: left;
    }

    .measure-link:hover {
      text-decoration-thickness: 2px;
    }

    .status-MET {
      color: #28a745;
      font-weight: 600;
    }

    .status-NOT_MET {
      color: #dc3545;
      font-weight: 600;
    }

    .status-EXCLUDED {
      color: #6c757d;
      font-weight: 600;
    }

    .badge-MET {
      background: #d4edda;
      color: #155724;
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 12px;
    }

    .badge-NOT_MET {
      background: #f8d7da;
      color: #721c24;
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 12px;
    }

    .badge-EXCLUDED {
      background: #e2e3e5;
      color: #383d41;
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 12px;
    }

    .detail-btn {
      padding: 6px 12px;
      background: #007bff;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 12px;
    }

    .detail-btn:hover {
      background: #0056b3;
    }

    .loading, .no-data {
      padding: 20px;
      text-align: center;
      color: #666;
      background: #f9f9f9;
      border-radius: 4px;
    }

    .detail-panel {
      position: fixed;
      right: 0;
      top: 0;
      bottom: 0;
      width: 400px;
      background: white;
      box-shadow: -2px 0 8px rgba(0,0,0,0.2);
      z-index: 1000;
      overflow-y: auto;
      animation: slideIn 0.3s ease;
    }

    @keyframes slideIn {
      from {
        transform: translateX(100%);
      }
      to {
        transform: translateX(0);
      }
    }

    .detail-content {
      padding: 20px;
      position: relative;
    }

    .close-btn {
      position: absolute;
      top: 10px;
      right: 10px;
      background: none;
      border: none;
      font-size: 24px;
      cursor: pointer;
      color: #666;
    }

    .detail-content h3 {
      margin-top: 20px;
      margin-bottom: 10px;
      color: #333;
    }

    .detail-metrics {
      margin-top: 20px;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .detail-metrics div {
      padding: 8px;
      background: #f9f9f9;
      border-radius: 4px;
    }

    .loading {
      color: #999;
    }
  `]
})
export class QualityMeasuresComponent implements OnInit, OnDestroy {
  qualityMeasures$: Observable<QualityMeasure[]> | undefined;
  filteredMeasures$: Observable<QualityMeasure[]> | undefined;
  selectedMeasure$: Observable<QualityMeasure | null> | undefined;

  statuses = ['ALL', 'MET', 'NOT_MET', 'EXCLUDED'];
  activeFilter = 'ALL';
  private activeFilterSubject$ = new BehaviorSubject<string>('ALL');
  private selectedMeasureSubject$ = new BehaviorSubject<QualityMeasure | null>(null);
  private destroy$ = new Subject<void>();

  private pipeline = inject(Clinical360PipelineService);
  private eventBus = inject(EventBusService);

  constructor() {
    this.selectedMeasure$ = this.selectedMeasureSubject$.asObservable();
  }

  ngOnInit() {
    this.eventBus.on(ClinicalEventType.PATIENT_SELECTED)
      .pipe(takeUntil(this.destroy$))
      .subscribe((event: any) => {
        this.loadMeasures(event.data.patientId);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadMeasures(patientId: string) {
    this.qualityMeasures$ = this.pipeline.clinical360$.pipe(
      map(data => {
        if (!data || !data.qualityMeasures) {
          return [];
        }
        return data.qualityMeasures.measures.map((m: any, index: number) => ({
          id: m.id || `measure-${index}`,
          name: m.name || 'Unknown Measure',
          description: m.description || 'No description',
          status: m.status || 'NOT_MET',
          target: m.target,
          actual: m.actual,
          population: m.population,
        }));
      }),
      tap(measures => {
        this.eventBus.emit({
          type: ClinicalEventType.MEASURE_EVALUATION_COMPLETED,
          source: 'mfe-quality',
          data: {
            patientId,
            totalMeasures: measures.length,
            measuresMet: measures.filter((m: QualityMeasure) => m.status === 'MET').length,
          },
        } as any);
      }),
      shareReplay(1)
    );

    this.filteredMeasures$ = combineLatest([
      this.qualityMeasures$,
      this.activeFilterSubject$,
    ]).pipe(
      map(([measures, filter]) => {
        if (filter === 'ALL') {
          return measures;
        }
        return measures.filter((m) => m.status === filter);
      })
    );
  }

  filterByStatus(status: string) {
    this.activeFilter = status;
    this.activeFilterSubject$.next(status);
  }

  showDetail(measure: QualityMeasure) {
    this.selectedMeasureSubject$.next(measure);
  }

  closeDetail() {
    this.selectedMeasureSubject$.next(null);
  }
}
