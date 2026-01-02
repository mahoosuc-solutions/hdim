import { Component, Inject, OnInit, OnDestroy, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { LoadingButtonComponent } from '../../../shared/components/loading-button/loading-button.component';
import { CqlEngineService, ValueSetDisplay } from '../../../services/cql-engine.service';

export interface ValueSetPickerDialogData {
  measureId: string;
  currentValueSets: any[];
}

interface ValueSet {
  id: string;
  name: string;
  oid: string;
  category: string;
  version: string;
  codeCount: number;
  codeSystem?: string;
  selected?: boolean;
}

@Component({
  selector: 'app-value-set-picker-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatSelectModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    LoadingButtonComponent,
  ],
  template: `
    <div class="picker-dialog">
      <!-- Header -->
      <h2 mat-dialog-title>
        <mat-icon color="primary">category</mat-icon>
        Value Set Picker
      </h2>

      <!-- Content -->
      <mat-dialog-content class="dialog-content">
        <!-- Filters -->
        <div class="filters">
          <mat-form-field appearance="outline" class="search-field">
            <mat-label>Search Value Sets</mat-label>
            <input
              matInput
              [(ngModel)]="searchText"
              (keyup)="applyFilter()"
              placeholder="Search by name or OID">
            <mat-icon matPrefix>search</mat-icon>
          </mat-form-field>

          <mat-form-field appearance="outline" class="category-field">
            <mat-label>Category</mat-label>
            <mat-select [(ngModel)]="selectedCategory" (selectionChange)="applyFilter()">
              <mat-option value="">All Categories</mat-option>
              <mat-option value="Diagnoses">Diagnoses</mat-option>
              <mat-option value="Procedures">Procedures</mat-option>
              <mat-option value="Medications">Medications</mat-option>
              <mat-option value="Laboratory">Laboratory</mat-option>
              <mat-option value="Vitals">Vitals</mat-option>
            </mat-select>
          </mat-form-field>
        </div>

        <!-- Selected Count -->
        @if (getSelectedCount() > 0) {
          <div class="selected-info">
            <mat-icon color="primary">check_circle</mat-icon>
            <span>{{ getSelectedCount() }} value set(s) selected</span>
          </div>
        }

        <!-- Loading State -->
        @if (loading) {
          <div class="loading-container">
            <mat-spinner diameter="40"></mat-spinner>
            <p>Loading value sets...</p>
          </div>
        }

        <!-- Value Sets Table -->
        <div class="table-container" [class.hidden]="loading">
          <table mat-table [dataSource]="dataSource" matSort class="value-sets-table">

            <!-- Select Column -->
            <ng-container matColumnDef="select">
              <th mat-header-cell *matHeaderCellDef>Select</th>
              <td mat-cell *matCellDef="let valueSet">
                <mat-icon
                  class="select-icon"
                  [class.selected]="valueSet.selected"
                  (click)="toggleSelection(valueSet)">
                  {{ valueSet.selected ? 'check_box' : 'check_box_outline_blank' }}
                </mat-icon>
              </td>
            </ng-container>

            <!-- Name Column -->
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Name</th>
              <td mat-cell *matCellDef="let valueSet">
                <div class="value-set-name">
                  <span>{{ valueSet.name }}</span>
                  <span class="oid">{{ valueSet.oid }}</span>
                </div>
              </td>
            </ng-container>

            <!-- Category Column -->
            <ng-container matColumnDef="category">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Category</th>
              <td mat-cell *matCellDef="let valueSet">
                <mat-chip class="category-chip">{{ valueSet.category }}</mat-chip>
              </td>
            </ng-container>

            <!-- Version Column -->
            <ng-container matColumnDef="version">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Version</th>
              <td mat-cell *matCellDef="let valueSet">{{ valueSet.version }}</td>
            </ng-container>

            <!-- Code Count Column -->
            <ng-container matColumnDef="codeCount">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Codes</th>
              <td mat-cell *matCellDef="let valueSet">{{ valueSet.codeCount }}</td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr
              mat-row
              *matRowDef="let row; columns: displayedColumns;"
              [class.selected-row]="row.selected"
              (click)="toggleSelection(row)">
            </tr>
          </table>
        </div>

        <!-- Paginator -->
        <mat-paginator
          [pageSizeOptions]="[10, 20, 50]"
          [pageSize]="10"
          showFirstLastButtons
          aria-label="Select page of value sets">
        </mat-paginator>
      </mat-dialog-content>

      <!-- Actions -->
      <mat-dialog-actions align="end">
        <button mat-button mat-dialog-close>
          <mat-icon>close</mat-icon>
          Cancel
        </button>
        <app-loading-button
          text="Apply Selection"
          icon="check"
          color="primary"
          variant="raised"
          [disabled]="getSelectedCount() === 0"
          ariaLabel="Apply value set selection"
          (buttonClick)="save()">
        </app-loading-button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .picker-dialog {
      display: flex;
      flex-direction: column;
      height: 100%;
    }

    h2 {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0;
      padding: 16px 24px;
      background-color: #f5f5f5;
      border-bottom: 1px solid #e0e0e0;

      mat-icon {
        font-size: 28px;
        width: 28px;
        height: 28px;
      }
    }

    .dialog-content {
      flex: 1;
      min-height: 0;
      padding: 16px 24px;
      display: flex;
      flex-direction: column;
    }

    .filters {
      display: flex;
      gap: 16px;
      margin-bottom: 16px;

      .search-field {
        flex: 2;
      }

      .category-field {
        flex: 1;
        min-width: 200px;
      }
    }

    .selected-info {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px;
      background-color: #e3f2fd;
      border-radius: 4px;
      margin-bottom: 16px;
      color: #1976d2;
      font-weight: 500;

      mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }
    }

    .table-container {
      flex: 1;
      min-height: 0;
      overflow-y: auto;
      border: 1px solid #e0e0e0;
      border-radius: 4px;
      margin-bottom: 16px;

      .value-sets-table {
        width: 100%;

        th {
          font-weight: 600;
          color: #555;
          background-color: #f5f5f5;
          padding: 16px;
        }

        td {
          padding: 16px;
        }

        .select-icon {
          cursor: pointer;
          color: #999;
          transition: color 0.2s;

          &:hover {
            color: #666;
          }

          &.selected {
            color: #1976d2;
          }
        }

        .value-set-name {
          display: flex;
          flex-direction: column;
          gap: 4px;

          .oid {
            font-size: 12px;
            color: #999;
            font-family: monospace;
          }
        }

        .category-chip {
          font-size: 11px;
          min-height: 24px;
          padding: 0 8px;
        }

        tr.selected-row {
          background-color: #e3f2fd;
        }

        tr:hover {
          background-color: #f5f5f5;
          cursor: pointer;
        }
      }
    }

    mat-dialog-actions {
      padding: 16px 24px;
      border-top: 1px solid #e0e0e0;
      gap: 8px;
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px;

      mat-spinner {
        margin-bottom: 16px;
      }

      p {
        color: #666;
      }
    }

    .hidden {
      display: none;
    }

    :host ::ng-deep .mat-mdc-dialog-container {
      padding: 0;
    }
  `],
})
export class ValueSetPickerDialogComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  private destroy$ = new Subject<void>();

  dataSource = new MatTableDataSource<ValueSet>([]);
  displayedColumns = ['select', 'name', 'category', 'version', 'codeCount'];
  searchText = '';
  selectedCategory = '';
  loading = false;

  // Value sets loaded from API
  valueSets: ValueSet[] = [];

  constructor(
    private dialogRef: MatDialogRef<ValueSetPickerDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ValueSetPickerDialogData,
    private cqlEngineService: CqlEngineService
  ) {}

  ngOnInit(): void {
    this.loadValueSets();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load value sets from backend API
   */
  private loadValueSets(): void {
    this.loading = true;
    this.cqlEngineService.listValueSets().pipe(takeUntil(this.destroy$)).subscribe({
      next: (valueSets) => {
        // Map to component format and mark current selections
        this.valueSets = valueSets.map((vs) => ({
          id: vs.id,
          name: vs.name,
          oid: vs.oid,
          category: vs.category,
          version: vs.version,
          codeCount: vs.codeCount,
          codeSystem: vs.codeSystem,
          selected: this.data.currentValueSets?.some(
            (current) => current.id === vs.id || current.oid === vs.oid
          ) || false,
        }));
        this.dataSource.data = this.valueSets;
        this.loading = false;
      },
      error: () => {
        // Fallback handled in service
        this.loading = false;
      },
    });
  }

  /**
   * Apply filters to the table
   */
  applyFilter(): void {
    const searchLower = this.searchText.toLowerCase();
    this.dataSource.filterPredicate = (data: ValueSet) => {
      const matchesSearch = !this.searchText ||
        data.name.toLowerCase().includes(searchLower) ||
        data.oid.toLowerCase().includes(searchLower);
      const matchesCategory = !this.selectedCategory || data.category === this.selectedCategory;
      return matchesSearch && matchesCategory;
    };
    this.dataSource.filter = this.searchText + this.selectedCategory;
  }

  /**
   * Toggle value set selection
   */
  toggleSelection(valueSet: ValueSet): void {
    valueSet.selected = !valueSet.selected;
  }

  /**
   * Get count of selected value sets
   */
  getSelectedCount(): number {
    return this.valueSets.filter(vs => vs.selected).length;
  }

  /**
   * Save selected value sets
   */
  save(): void {
    const selected = this.valueSets.filter(vs => vs.selected);
    this.dialogRef.close(selected);
  }
}
