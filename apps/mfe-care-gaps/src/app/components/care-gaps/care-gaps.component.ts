import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable, BehaviorSubject } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { Clinical360PipelineService } from '@health-platform/shared/data-access';
import { EventBusService, ClinicalEventType } from '@health-platform/shared/data-access';

export interface CareGap {
  id: string;
  name: string;
  description: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  dueDate?: Date;
  status: 'OPEN' | 'SCHEDULED' | 'OVERDUE' | 'CLOSED';
  intervention?: string;
}

@Component({
  selector: 'app-care-gaps',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="care-gaps-container">
      <h2>Care Gaps</h2>

      @if (priorities?.length) {
        <div class="filters">
          @for (priority of priorities; track priority) {
            <button
              type="button"
              (click)="filterByPriority(priority)"
              [class.active]="activeFilter === priority"
              class="filter-btn"
            >
              {{ priority }}
            </button>
          }
        </div>
      }

      @if (filteredGaps$ | async; as gaps) {
        <div class="gaps-list">
          @if (gaps.length > 0) {
            <div class="gap-items">
              @for (gap of gaps; track gap.id) {
                <div class="gap-card" [class]="'priority-' + gap.priority">
                  <div class="gap-header">
                    <h3>
                      <button type="button" (click)="showDetail(gap)" class="gap-title">
                        {{ gap.name }}
                      </button>
                    </h3>
                    <span class="status-badge" [class]="'status-' + gap.status">{{ gap.status }}</span>
                  </div>
                  <p class="gap-description">{{ gap.description }}</p>
                  <div class="gap-actions">
                    <button type="button" (click)="showDetail(gap)" class="detail-btn">View</button>
                    <button type="button" (click)="closeGap(gap)" class="close-btn">Close</button>
                  </div>
                </div>
              }
            </div>
          } @else {
            <div class="no-gaps">No care gaps identified! ✅</div>
          }
        </div>
      } @else {
        <div class="loading">Loading care gaps...</div>
      }

      <!-- Detail Panel -->
      @if (selectedGap$ | async; as gap) {
        <div class="detail-panel">
          <div class="detail-content">
            <button type="button" (click)="closeDetail()" class="close-btn">×</button>
            <h3>{{ gap.name }}</h3>
            <p>{{ gap.description }}</p>
            <div class="gap-details">
              <div>Priority: <strong [class]="'priority-' + gap.priority">{{ gap.priority }}</strong></div>
              <div>Status: <strong>{{ gap.status }}</strong></div>
              @if (gap.dueDate) {
                <div>Due: <strong>{{ gap.dueDate | date }}</strong></div>
              }
              @if (gap.intervention) {
                <div>Intervention: <strong>{{ gap.intervention }}</strong></div>
              }
            </div>
            <button type="button" (click)="closeGap(gap)" class="action-btn">Close Gap</button>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .care-gaps-container {
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
      background: #dc3545;
      color: white;
      border-color: #dc3545;
    }

    .gap-items {
      display: grid;
      gap: 16px;
    }

    .gap-card {
      padding: 16px;
      border-left: 4px solid #ddd;
      background: white;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      border-radius: 4px;
    }

    .gap-card.priority-HIGH {
      border-left-color: #dc3545;
      background: #fff5f5;
    }

    .gap-card.priority-MEDIUM {
      border-left-color: #ffc107;
      background: #fffbf0;
    }

    .gap-card.priority-LOW {
      border-left-color: #28a745;
      background: #f5fdf7;
    }

    .gap-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 10px;
    }

    .gap-header h3 {
      margin: 0;
      color: #007bff;
    }

    .gap-title {
      padding: 0;
      border: none;
      background: none;
      cursor: pointer;
      font: inherit;
      color: inherit;
      text-align: left;
      text-decoration: underline;
    }

    .gap-title:hover {
      text-decoration-thickness: 2px;
    }

    .status-badge {
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 600;
    }

    .status-OPEN {
      background: #e7d4f5;
      color: #5e35b1;
    }

    .status-SCHEDULED {
      background: #bbdefb;
      color: #1565c0;
    }

    .status-OVERDUE {
      background: #ffccbc;
      color: #d84315;
    }

    .status-CLOSED {
      background: #c8e6c9;
      color: #2e7d32;
    }

    .gap-description {
      margin: 10px 0;
      color: #666;
      font-size: 14px;
    }

    .gap-actions {
      display: flex;
      gap: 10px;
      margin-top: 10px;
    }

    .detail-btn, .close-btn {
      padding: 6px 12px;
      background: #007bff;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 12px;
    }

    .close-btn {
      background: #6c757d;
    }

    .detail-btn:hover {
      background: #0056b3;
    }

    .close-btn:hover {
      background: #545b62;
    }

    .loading, .no-gaps {
      padding: 20px;
      text-align: center;
      color: #666;
      background: #f9f9f9;
      border-radius: 4px;
    }

    .no-gaps {
      color: #28a745;
      font-weight: 600;
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

    .detail-content .close-btn {
      position: absolute;
      top: 10px;
      right: 10px;
      background: none;
      border: none;
      font-size: 24px;
      cursor: pointer;
      color: #666;
      padding: 0;
    }

    .detail-content h3 {
      margin-top: 20px;
      margin-bottom: 10px;
      color: #333;
    }

    .gap-details {
      margin: 20px 0;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .gap-details div {
      padding: 8px;
      background: #f9f9f9;
      border-radius: 4px;
    }

    .priority-HIGH {
      color: #dc3545;
    }

    .priority-MEDIUM {
      color: #ffc107;
    }

    .priority-LOW {
      color: #28a745;
    }

    .action-btn {
      margin-top: 20px;
      padding: 10px 20px;
      background: #28a745;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      width: 100%;
    }

    .action-btn:hover {
      background: #218838;
    }
  `]
})
export class CareGapsComponent implements OnInit {
  careGaps$: Observable<CareGap[]> | undefined;
  filteredGaps$: Observable<CareGap[]> | undefined;
  selectedGap$: Observable<CareGap | null> | undefined;

  priorities = ['ALL', 'HIGH', 'MEDIUM', 'LOW'];
  activeFilter = 'ALL';
  private activeFilterSubject$ = new BehaviorSubject<string>('ALL');
  private selectedGapSubject$ = new BehaviorSubject<CareGap | null>(null);

  private pipeline = inject(Clinical360PipelineService);
  private eventBus = inject(EventBusService);

  constructor() {
    this.selectedGap$ = this.selectedGapSubject$.asObservable();
  }

  ngOnInit() {
    // Listen for patient selection events
    this.eventBus.on(ClinicalEventType.PATIENT_SELECTED).subscribe((event: any) => {
      this.loadGaps(event.data.patientId);
    });
  }

  private loadGaps(patientId: string) {
    this.careGaps$ = this.pipeline.clinical360$.pipe(
      map(data => {
        if (!data || !data.careGaps) {
          return [];
        }
        // Scenario 1: Load from 360 pipeline
        return data.careGaps.gaps.map((g: any, index: number) => ({
          id: g.id || `gap-${index}`,
          name: g.name || 'Unknown Gap',
          description: g.description || 'No description',
          priority: g.priority || 'MEDIUM',
          dueDate: g.dueDate,
          status: g.status || 'OPEN',
          intervention: g.intervention,
        }));
      }),
      tap(gaps => {
        // Scenario 5: Emit event when loaded
        this.eventBus.emit({
          type: ClinicalEventType.CARE_GAP_IDENTIFIED,
          source: 'mfe-care-gaps',
          data: {
            patientId,
            totalGaps: gaps.length,
            criticalGaps: gaps.filter((g: CareGap) => g.priority === 'HIGH').length,
          },
        } as any);
      })
    );

    // Scenario 2: Support filtering by priority
    this.filteredGaps$ = this.pipeline.clinical360$.pipe(
      map(data => {
        if (!data || !data.careGaps) {
          return [];
        }
        const gaps = data.careGaps.gaps.map((g: any, index: number) => ({
          id: g.id || `gap-${index}`,
          name: g.name || 'Unknown Gap',
          description: g.description || 'No description',
          priority: g.priority || 'MEDIUM',
          dueDate: g.dueDate,
          status: g.status || 'OPEN',
          intervention: g.intervention,
        }));

        if (this.activeFilter === 'ALL') {
          return gaps;
        }
        return gaps.filter((g: CareGap) => g.priority === this.activeFilter);
      })
    );
  }

  filterByPriority(priority: string) {
    this.activeFilter = priority;
    this.activeFilterSubject$.next(priority);
    // Trigger filter update
    this.careGaps$ = this.careGaps$?.pipe(
      map(gaps => {
        if (priority === 'ALL') return gaps;
        return gaps.filter(g => g.priority === priority);
      })
    );
  }

  showDetail(gap: CareGap) {
    // Scenario 3: Drill into gap details
    this.selectedGapSubject$.next(gap);
  }

  closeDetail() {
    this.selectedGapSubject$.next(null);
  }

  closeGap(gap: CareGap) {
    // Scenario 4: Close gap workflow
    this.selectedGapSubject$.next(null);
    this.eventBus.emit({
      type: ClinicalEventType.CARE_GAP_RESOLVED,
      source: 'mfe-care-gaps',
      data: {
        gapId: gap.id,
        gapName: gap.name,
      },
    } as any);
    console.log(`Care gap closed: ${gap.name}`);
  }
}
