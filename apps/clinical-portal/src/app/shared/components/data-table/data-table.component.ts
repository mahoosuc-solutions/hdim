/**
 * Data Table Component
 *
 * Reusable table component with Material Design using matTable.
 * Features sorting, pagination, column visibility, row selection, and responsive design.
 *
 * @example
 * <app-data-table
 *   [data]="patients"
 *   [columns]="columnDefinitions"
 *   [loading]="isLoading"
 *   [pageSize]="25"
 *   [pageSizeOptions]="[10, 25, 50, 100]"
 *   (rowSelected)="onRowClick($event)"
 *   (actionClicked)="onAction($event)">
 * </app-data-table>
 */
import {
  Component,
  Input,
  Output,
  EventEmitter,
  ViewChild,
  OnInit,
  OnChanges,
  SimpleChanges,
  AfterViewInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { SelectionModel } from '@angular/cdk/collections';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

/**
 * Column definition interface
 * @template T The row data type
 */
export interface ColumnDefinition<T = Record<string, unknown>> {
  /** Unique column identifier */
  key: string;
  /** Display header text */
  label: string;
  /** Column data type */
  type?: 'text' | 'number' | 'date' | 'boolean' | 'custom' | 'actions';
  /** Is column sortable */
  sortable?: boolean;
  /** Is column visible by default */
  visible?: boolean;
  /** Column width (CSS value) */
  width?: string;
  /** Custom formatter function */
  formatter?: (value: unknown, row: T) => string;
  /** Custom template reference (for advanced usage) */
  template?: unknown;
  /** CSS classes for the column */
  cssClass?: string;
}

/**
 * Action button configuration
 * @template T The row data type
 */
export interface TableAction<T = Record<string, unknown>> {
  /** Action identifier */
  id: string;
  /** Display label */
  label: string;
  /** Material icon name */
  icon?: string;
  /** Action color */
  color?: 'primary' | 'accent' | 'warn';
  /** Is action disabled for row */
  disabled?: (row: T) => boolean;
  /** Show condition for row */
  show?: (row: T) => boolean;
}

/**
 * Action click event data
 * @template T The row data type
 */
export interface ActionClickEvent<T = Record<string, unknown>> {
  action: TableAction<T>;
  row: T;
}

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatCheckboxModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatMenuModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="data-table-container">
      <!-- Loading overlay -->
      <div *ngIf="loading" class="loading-overlay">
        <mat-spinner [diameter]="40"></mat-spinner>
      </div>

      <!-- Table wrapper with horizontal scroll -->
      <div class="table-wrapper">
        <table
          mat-table
          [dataSource]="dataSource"
          matSort
          (matSortChange)="onSortChange($event)"
          class="data-table"
          [class.striped]="striped"
          [class.hoverable]="hoverable">

          <!-- Checkbox column (if selectable) -->
          <ng-container matColumnDef="select" *ngIf="selectable">
            <th mat-header-cell *matHeaderCellDef class="checkbox-cell">
              <mat-checkbox
                (change)="$event ? toggleAllRows() : null"
                [checked]="selection.hasValue() && isAllSelected()"
                [indeterminate]="selection.hasValue() && !isAllSelected()"
                [aria-label]="'Select all rows'">
              </mat-checkbox>
            </th>
            <td mat-cell *matCellDef="let row" class="checkbox-cell">
              <mat-checkbox
                (click)="$event.stopPropagation()"
                (change)="$event ? selection.toggle(row) : null"
                [checked]="selection.isSelected(row)"
                [aria-label]="'Select row'">
              </mat-checkbox>
            </td>
          </ng-container>

          <!-- Data columns -->
          <ng-container *ngFor="let column of visibleColumns" [matColumnDef]="column.key">
            <th
              mat-header-cell
              *matHeaderCellDef
              [mat-sort-header]="column.sortable !== false ? column.key : ''"
              [disabled]="column.sortable === false"
              [style.width]="column.width"
              [ngClass]="column.cssClass">
              {{ column.label }}
            </th>
            <td
              mat-cell
              *matCellDef="let row"
              [style.width]="column.width"
              [ngClass]="column.cssClass">
              <!-- Custom formatter -->
              <span *ngIf="column.formatter">
                {{ column.formatter(row[column.key], row) }}
              </span>
              <!-- Default rendering -->
              <span *ngIf="!column.formatter && column.type !== 'actions'">
                {{ formatCellValue(row[column.key], column.type) }}
              </span>
              <!-- Actions column -->
              <div *ngIf="column.type === 'actions'" class="actions-cell">
                <button
                  *ngFor="let action of getVisibleActions(row)"
                  mat-icon-button
                  [color]="action.color || 'primary'"
                  [disabled]="isActionDisabled(action, row)"
                  [matTooltip]="action.label"
                  (click)="onActionClick(action, row, $event)">
                  <mat-icon>{{ action.icon || 'more_vert' }}</mat-icon>
                </button>
              </div>
            </td>
          </ng-container>

          <!-- Header row -->
          <tr mat-header-row *matHeaderRowDef="displayedColumns; sticky: stickyHeader"></tr>

          <!-- Data rows -->
          <tr
            mat-row
            *matRowDef="let row; columns: displayedColumns;"
            [class.selected]="selection.isSelected(row)"
            (click)="onRowClick(row)">
          </tr>

          <!-- Empty state row -->
          <tr class="mat-row" *matNoDataRow>
            <td class="mat-cell empty-state" [attr.colspan]="displayedColumns.length">
              <div class="empty-state-content">
                <mat-icon>inbox</mat-icon>
                <p>{{ emptyMessage }}</p>
              </div>
            </td>
          </tr>
        </table>
      </div>

      <!-- Paginator -->
      <mat-paginator
        *ngIf="showPaginator"
        [length]="dataSource.data.length"
        [pageSize]="pageSize"
        [pageSizeOptions]="pageSizeOptions"
        [showFirstLastButtons]="true"
        aria-label="Select page">
      </mat-paginator>
    </div>
  `,
  styles: [`
    .data-table-container {
      position: relative;
      width: 100%;
      background: white;
      border-radius: 4px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
    }

    .loading-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(255, 255, 255, 0.8);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 10;
    }

    .table-wrapper {
      overflow-x: auto;
      width: 100%;
    }

    .data-table {
      width: 100%;
      min-width: 600px;
    }

    /* Striped rows */
    .data-table.striped tr.mat-row:nth-child(even) {
      background-color: #f5f5f5;
    }

    /* Hoverable rows */
    .data-table.hoverable tr.mat-row:hover {
      background-color: #e3f2fd;
      cursor: pointer;
    }

    /* Selected row */
    tr.mat-row.selected {
      background-color: #bbdefb !important;
    }

    /* Checkbox cell */
    .checkbox-cell {
      width: 48px;
      padding-left: 8px !important;
      padding-right: 8px !important;
    }

    /* Actions cell */
    .actions-cell {
      display: flex;
      gap: 4px;
      align-items: center;
    }

    /* Empty state */
    .empty-state {
      text-align: center;
      padding: 48px 24px !important;
    }

    .empty-state-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      color: rgba(0, 0, 0, 0.38);
    }

    .empty-state-content mat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      opacity: 0.3;
    }

    .empty-state-content p {
      margin: 0;
      font-size: 14px;
    }

    /* Dark theme */
    @media (prefers-color-scheme: dark) {
      .data-table-container {
        background: #424242;
      }

      .data-table.striped tr.mat-row:nth-child(even) {
        background-color: #383838;
      }

      .data-table.hoverable tr.mat-row:hover {
        background-color: #616161;
      }

      tr.mat-row.selected {
        background-color: #546e7a !important;
      }

      .loading-overlay {
        background: rgba(66, 66, 66, 0.8);
      }
    }

    /* Responsive */
    @media (max-width: 600px) {
      .data-table {
        min-width: 100%;
        font-size: 12px;
      }

      .actions-cell button {
        padding: 4px;
      }
    }
  `]
})
export class DataTableComponent<T extends Record<string, unknown> = Record<string, unknown>> implements OnInit, OnChanges, AfterViewInit {
  /** Table data array */
  @Input() data: T[] = [];

  /** Column definitions */
  @Input() columns: ColumnDefinition<T>[] = [];

  /** Action buttons for each row */
  @Input() actions: TableAction<T>[] = [];

  /** Is table loading */
  @Input() loading: boolean = false;

  /** Enable row selection */
  @Input() selectable: boolean = false;

  /** Enable striped rows */
  @Input() striped: boolean = true;

  /** Enable row hover effect */
  @Input() hoverable: boolean = true;

  /** Show paginator */
  @Input() showPaginator: boolean = true;

  /** Sticky header */
  @Input() stickyHeader: boolean = false;

  /** Page size */
  @Input() pageSize: number = 25;

  /** Page size options */
  @Input() pageSizeOptions: number[] = [10, 25, 50, 100];

  /** Empty state message */
  @Input() emptyMessage: string = 'No data available';

  /** Row selected event */
  @Output() rowSelected = new EventEmitter<T>();

  /** Action clicked event */
  @Output() actionClicked = new EventEmitter<ActionClickEvent<T>>();

  /** Selection changed event */
  @Output() selectionChanged = new EventEmitter<T[]>();

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  dataSource = new MatTableDataSource<T>([]);
  selection = new SelectionModel<T>(true, []);

  ngOnInit(): void {
    this.initializeDataSource();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['data']) {
      this.dataSource.data = this.data || [];
    }
  }

  ngAfterViewInit(): void {
    if (this.paginator) {
      this.dataSource.paginator = this.paginator;
    }
    if (this.sort) {
      this.dataSource.sort = this.sort;
    }
  }

  private initializeDataSource(): void {
    this.dataSource.data = this.data || [];
  }

  get visibleColumns(): ColumnDefinition<T>[] {
    return this.columns.filter(col => col.visible !== false);
  }

  get displayedColumns(): string[] {
    const columns = this.visibleColumns.map(col => col.key);
    if (this.selectable) {
      return ['select', ...columns];
    }
    return columns;
  }

  formatCellValue(value: unknown, type?: string): string {
    if (value === null || value === undefined) return '';

    switch (type) {
      case 'date':
        return value instanceof Date ? value.toLocaleDateString() : String(value);
      case 'number':
        return typeof value === 'number' ? value.toLocaleString() : String(value);
      case 'boolean':
        return value ? 'Yes' : 'No';
      default:
        return String(value);
    }
  }

  getVisibleActions(row: T): TableAction<T>[] {
    return this.actions.filter(action =>
      !action.show || action.show(row)
    );
  }

  isActionDisabled(action: TableAction<T>, row: T): boolean {
    return action.disabled ? action.disabled(row) : false;
  }

  onRowClick(row: T): void {
    if (this.hoverable) {
      this.rowSelected.emit(row);
    }
  }

  onActionClick(action: TableAction<T>, row: T, event: Event): void {
    event.stopPropagation();
    this.actionClicked.emit({ action, row });
  }

  onSortChange(event: { active: string; direction: string }): void {
    // Sort change handled automatically by MatSort
  }

  /** Selection methods */
  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  toggleAllRows(): void {
    if (this.isAllSelected()) {
      this.selection.clear();
    } else {
      this.dataSource.data.forEach(row => this.selection.select(row));
    }
    this.selectionChanged.emit(this.selection.selected);
  }

  getSelectedRows(): T[] {
    return this.selection.selected;
  }

  clearSelection(): void {
    this.selection.clear();
    this.selectionChanged.emit([]);
  }
}
