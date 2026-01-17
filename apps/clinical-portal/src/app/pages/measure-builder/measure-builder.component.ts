import { Component, OnInit, OnDestroy, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDividerModule } from '@angular/material/divider';
import { SelectionModel } from '@angular/cdk/collections';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { ToastService } from '../../services/toast.service';
import { DialogService } from '../../services/dialog.service';
import { AIAssistantService } from '../../services/ai-assistant.service';
import {
  CustomMeasureService,
  CustomMeasure,
  CreateCustomMeasureRequest,
} from '../../services/custom-measure.service';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { LoadingOverlayComponent } from '../../shared/components/loading-overlay/loading-overlay.component';
import { CSVHelper } from '../../utils/csv-helper';
import { TrackInteraction } from '../../utils/ai-tracking.decorator';
import { Patient } from '../../models/patient.model';
import { selectCurrentPatient } from '../../store/selectors/patient.selectors';

// Import dialog components
import { NewMeasureDialogComponent } from './dialogs/new-measure-dialog.component';
import { CqlEditorDialogComponent } from './dialogs/cql-editor-dialog.component';
import { ValueSetPickerDialogComponent } from './dialogs/value-set-picker-dialog.component';
import { TestPreviewDialogComponent } from './dialogs/test-preview-dialog.component';
import { PublishConfirmDialogComponent } from './dialogs/publish-confirm-dialog.component';
import { VersionHistoryDialogComponent } from './dialogs/version-history-dialog.component';
import { PerformanceDashboardDialogComponent } from './dialogs/performance-dashboard-dialog.component';

@Component({
  selector: 'app-measure-builder',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatDialogModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatTooltipModule,
    MatMenuModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatDividerModule,
    LoadingButtonComponent,
    LoadingOverlayComponent,
  ],
  templateUrl: './measure-builder.component.html',
  styleUrls: ['./measure-builder.component.scss'],
})
export class MeasureBuilderComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  private destroy$ = new Subject<void>();

  drafts: CustomMeasure[] = [];
  measures: CustomMeasure[] = [];
  dataSource = new MatTableDataSource<CustomMeasure>([]);
  selection = new SelectionModel<CustomMeasure>(true, []);
  loading = false;

  // Table columns
  displayedColumns: string[] = [
    'select',
    'name',
    'category',
    'status',
    'version',
    'updatedAt',
    'actions',
  ];

  // Button loading states
  publishSelectedLoading = false;
  publishSelectedSuccess = false;
  exportSelectedLoading = false;
  exportSelectedSuccess = false;
  deleteSelectedLoading = false;
  deleteSelectedSuccess = false;

  // Status color mapping
  statusColors: { [key: string]: string } = {
    DRAFT: 'accent',
    PUBLISHED: 'primary',
    ARCHIVED: 'warn',
  };

  // Current patient context from store (for patient-aware measure testing)
  currentPatient: Patient | null = null;

  constructor(
    private dialog: MatDialog,
    private customMeasureService: CustomMeasureService,
    private toast: ToastService,
    private dialogService: DialogService,
    public aiAssistant: AIAssistantService,
    private store: Store
  ) {}

  ngOnInit(): void {
    this.loadDrafts();
    // Subscribe to current patient from the store for context-aware testing
    this.store.select(selectCurrentPatient).pipe(takeUntil(this.destroy$)).subscribe((patient) => {
      this.currentPatient = patient;
    });
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
   * Load draft measures from the backend
   */
  private loadDrafts(): void {
    this.loading = true;
    this.customMeasureService.list('DRAFT').pipe(takeUntil(this.destroy$)).subscribe({
      next: (drafts) => {
        this.drafts = drafts;
        this.measures = drafts;
        this.dataSource.data = drafts;
        this.loading = false;
      },
      error: () => {
        this.toast.error('Failed to load draft measures');
        this.loading = false;
      },
    });
  }

  /**
   * Backward compatibility wrapper for existing load flows
   */
  private loadMeasures(): void {
    this.loadDrafts();
  }

  /**
   * Open dialog to create a new measure
   */
  @TrackInteraction('measure-builder', 'create-measure')
  openNewMeasureDialog(): void {
    const dialogRef = this.dialog.open(NewMeasureDialogComponent, {
      width: '900px',
      maxHeight: '85vh',
      disableClose: true,
      autoFocus: true,
    });

    dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe((draft?: CreateCustomMeasureRequest & { cqlTemplate?: string }) => {
      if (!draft) return;
      this.loading = true;

      // Extract cqlTemplate before sending to backend (it's not part of the DTO)
      const cqlTemplate = draft.cqlTemplate;
      delete draft.cqlTemplate;

      this.customMeasureService.createDraft(draft).pipe(takeUntil(this.destroy$)).subscribe({
        next: (saved) => {
          // If created from template, also save the CQL
          if (cqlTemplate) {
            this.customMeasureService.updateCql(saved.id, cqlTemplate).pipe(takeUntil(this.destroy$)).subscribe({
              next: (updatedMeasure) => {
                this.drafts = [updatedMeasure, ...this.drafts];
                this.measures = this.drafts;
                this.dataSource.data = this.drafts;
                this.toast.success('Measure created from template successfully');
                this.loading = false;
              },
              error: () => {
                // Measure created but CQL save failed - still add to list
                this.drafts = [saved, ...this.drafts];
                this.measures = this.drafts;
                this.dataSource.data = this.drafts;
                this.toast.warning('Measure created, but template CQL could not be saved');
                this.loading = false;
              },
            });
          } else {
            this.drafts = [saved, ...this.drafts];
            this.measures = this.drafts;
            this.dataSource.data = this.drafts;
            this.toast.success('Measure created successfully');
            this.loading = false;
          }
        },
        error: () => {
          this.toast.error('Failed to create measure');
          this.loading = false;
        },
      });
    });
  }

  /**
   * Open CQL editor for a measure
   */
  openDraft(measure: CustomMeasure): void {
    if (!measure) {
      return;
    }
    this.editCql(measure);
  }

  @TrackInteraction('measure-builder', 'edit-cql')
  editCql(measure: CustomMeasure): void {
    const dialogRef = this.dialog.open(CqlEditorDialogComponent, {
      width: '90vw',
      height: '85vh',
      maxWidth: '1400px',
      data: {
        measureId: measure.id,
        measureName: measure.name,
        cqlText: measure.cqlText || '',
        readOnly: measure.status === 'PUBLISHED',
        // Pass current patient context for patient-aware measure testing
        contextPatient: this.currentPatient ?? undefined,
      },
    });

    dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe((updatedCql?: string) => {
      if (updatedCql !== undefined) {
        // Persist the CQL changes to the backend
        this.loading = true;
        this.customMeasureService.updateCql(measure.id, updatedCql).pipe(takeUntil(this.destroy$)).subscribe({
          next: (updatedMeasure) => {
            // Update local data with the returned measure
            const index = this.drafts.findIndex(m => m.id === measure.id);
            if (index !== -1) {
              this.drafts[index] = updatedMeasure;
              this.measures = this.drafts;
              this.dataSource.data = this.drafts;
            }
            this.toast.success('CQL saved successfully');
            this.loading = false;
          },
          error: (err) => {
            this.toast.error(`Failed to save CQL: ${err.userMessage || err.message || 'Unknown error'}`);
            this.loading = false;
          },
        });
      }
    });
  }

  /**
   * Open value set picker
   */
  openValueSetPicker(measure: CustomMeasure): void {
    const dialogRef = this.dialog.open(ValueSetPickerDialogComponent, {
      width: '800px',
      height: '600px',
      data: {
        measureId: measure.id,
        currentValueSets: measure.valueSets || [],
      },
    });

    dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe((selectedValueSets?: any[]) => {
      if (selectedValueSets) {
        // Persist the value set changes to the backend
        this.loading = true;
        this.customMeasureService.updateValueSets(measure.id, selectedValueSets).pipe(takeUntil(this.destroy$)).subscribe({
          next: (updatedMeasure) => {
            // Update local data with the returned measure
            const index = this.drafts.findIndex(m => m.id === measure.id);
            if (index !== -1) {
              this.drafts[index] = updatedMeasure;
              this.measures = this.drafts;
              this.dataSource.data = this.drafts;
            }
            this.toast.success('Value sets updated successfully');
            this.loading = false;
          },
          error: (err) => {
            this.toast.error(`Failed to update value sets: ${err.userMessage || err.message || 'Unknown error'}`);
            this.loading = false;
          },
        });
      }
    });
  }

  /**
   * Test measure against sample patients
   */
  testMeasure(measure: CustomMeasure): void {
    this.dialog.open(TestPreviewDialogComponent, {
      width: '1000px',
      height: '700px',
      data: {
        measureId: measure.id,
        measureName: measure.name,
      },
    });
  }

  /**
   * Open version history and audit trail dialog
   */
  @TrackInteraction('measure-builder', 'view-version-history')
  openVersionHistory(measure: CustomMeasure): void {
    this.dialog.open(VersionHistoryDialogComponent, {
      width: '900px',
      maxHeight: '80vh',
      data: {
        measure,
      },
    });
  }

  /**
   * Clone a measure to create a copy
   */
  @TrackInteraction('measure-builder', 'clone-measure')
  cloneMeasure(measure: CustomMeasure): void {
    this.loading = true;
    this.customMeasureService.cloneMeasure(measure.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (clonedMeasure) => {
        this.drafts = [clonedMeasure, ...this.drafts];
        this.measures = this.drafts;
        this.dataSource.data = this.drafts;
        this.toast.success(`Measure "${measure.name}" cloned successfully`);
        this.loading = false;
      },
      error: (err) => {
        this.toast.error(`Failed to clone measure: ${err.userMessage || err.message || 'Unknown error'}`);
        this.loading = false;
      },
    });
  }

  /**
   * Open performance comparison dashboard
   */
  @TrackInteraction('measure-builder', 'view-performance-dashboard')
  openPerformanceDashboard(): void {
    this.dialog.open(PerformanceDashboardDialogComponent, {
      width: '1200px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      data: {
        measures: this.measures,
      },
    });
  }

  /**
   * Publish a measure
   */
  @TrackInteraction('measure-builder', 'publish-measure')
  publishMeasure(measure: CustomMeasure): void {
    const dialogRef = this.dialog.open(PublishConfirmDialogComponent, {
      width: '600px',
      data: {
        measureId: measure.id,
        measureName: measure.name,
        currentVersion: measure.version,
      },
    });

    dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe((published?: boolean) => {
      if (published) {
        this.toast.success('Measure published successfully');
        this.loadMeasures();
      }
    });
  }

  /**
   * Delete a measure
   */
  deleteMeasure(measure: CustomMeasure): void {
    this.dialogService
      .confirmDelete(measure.name, 'measure')
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }

        this.loading = true;
        this.customMeasureService.delete(measure.id).pipe(takeUntil(this.destroy$)).subscribe({
          next: () => {
            // Remove from local arrays
            this.measures = this.measures.filter((m) => m.id !== measure.id);
            this.drafts = this.drafts.filter((m) => m.id !== measure.id);
            this.dataSource.data = this.measures;
            this.toast.success(`Measure "${measure.name}" deleted successfully`);
            this.loading = false;
          },
          error: (err) => {
            this.toast.error(`Failed to delete measure: ${err.userMessage || err.message || 'Unknown error'}`);
            this.loading = false;
          },
        });
      });
  }

  /**
   * Get status badge color
   */
  getStatusColor(status: string): string {
    return this.statusColors[status] || 'primary';
  }

  /**
   * Apply filter to table
   */
  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  // ===== Row Selection Methods =====

  /**
   * Whether the number of selected elements matches the total number of rows
   */
  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  /**
   * Selects all rows if they are not all selected; otherwise clear selection
   */
  masterToggle(): void {
    this.isAllSelected()
      ? this.selection.clear()
      : this.dataSource.data.forEach((row) => this.selection.select(row));
  }

  /**
   * The label for the checkbox on the passed row
   */
  checkboxLabel(row?: CustomMeasure): string {
    if (!row) {
      return `${this.isAllSelected() ? 'deselect' : 'select'} all`;
    }
    return `${this.selection.isSelected(row) ? 'deselect' : 'select'} row ${row.id}`;
  }

  /**
   * Get the count of selected rows
   */
  getSelectionCount(): number {
    return this.selection.selected.length;
  }

  /**
   * Clear all selected rows
   */
  clearSelection(): void {
    this.selection.clear();
  }

  /**
   * Export selected rows to CSV
   */
  exportSelectedToCSV(): void {
    if (!this.selection.hasValue()) {
      return;
    }

    this.exportSelectedLoading = true;
    this.exportSelectedSuccess = false;

    // Simulate processing time for UX feedback
    setTimeout(() => {
      const selectedMeasures = this.selection.selected;

      // CSV header
      const headers = [
        'Name',
        'Category',
        'Version',
        'Status',
        'CQL Library',
        'Last Modified',
      ];

      // CSV rows
      const rows = selectedMeasures.map((measure) => [
        measure.name,
        measure.category || 'CUSTOM',
        measure.version || '1.0.0',
        measure.status || 'DRAFT',
        measure.cqlText ? 'Yes' : 'No',
        measure.updatedAt || measure.createdAt,
      ]);

      // Combine headers and rows using CSVHelper
      const csvData = [headers, ...rows];
      const csvContent = CSVHelper.arrayToCSV(csvData);
      const filename = `selected-measures-${new Date().toISOString().split('T')[0]}.csv`;

      CSVHelper.downloadCSV(filename, csvContent);

      this.exportSelectedLoading = false;
      this.exportSelectedSuccess = true;
      this.toast.success(
        `Exported ${selectedMeasures.length} measure(s) to CSV`
      );
    }, 500);
  }

  /**
   * Publish selected measures
   */
  publishSelected(): void {
    if (!this.selection.hasValue()) {
      return;
    }

    const selectedMeasures = this.selection.selected;
    const draftMeasures = selectedMeasures.filter(
      (m) => m.status === 'DRAFT'
    );
    const alreadyPublished = selectedMeasures.filter(
      (m) => m.status === 'PUBLISHED'
    );

    if (draftMeasures.length === 0) {
      this.toast.warning('All selected measures are already published');
      return;
    }

    let message = `Are you sure you want to publish ${draftMeasures.length} measure(s)?`;
    if (alreadyPublished.length > 0) {
      message += `<br><br><em>${alreadyPublished.length} measure(s) are already published and will be skipped.</em>`;
    }

    this.dialogService
      .confirm('Publish Selected Measures', message, 'Publish', 'Cancel')
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }

        this.publishSelectedLoading = true;
        this.publishSelectedSuccess = false;

        const idsToPublish = draftMeasures.map((m) => m.id);
        this.customMeasureService.batchPublish(idsToPublish).pipe(takeUntil(this.destroy$)).subscribe({
          next: (result) => {
            this.publishSelectedLoading = false;
            this.publishSelectedSuccess = true;
            if (result.failed.length > 0) {
              this.toast.warning(
                `Published ${result.published} measure(s), ${result.failed.length} failed`
              );
            } else {
              this.toast.success(
                `Successfully published ${result.published} measure(s)`
              );
            }
            this.selection.clear();
            this.loadMeasures();
          },
          error: (err) => {
            this.publishSelectedLoading = false;
            this.toast.error(
              `Failed to publish measures: ${err.userMessage || err.message || 'Unknown error'}`
            );
          },
        });
      });
  }

  /**
   * Delete selected measures
   */
  deleteSelected(): void {
    if (!this.selection.hasValue()) {
      return;
    }

    const selectedMeasures = this.selection.selected;
    const measureNames = selectedMeasures
      .map((m) => m.name)
      .slice(0, 3)
      .join(', ');
    const displayNames =
      selectedMeasures.length > 3
        ? `${measureNames}, and ${selectedMeasures.length - 3} more`
        : measureNames;

    this.dialogService
      .confirm(
        'Delete Selected Measures',
        `Are you sure you want to delete ${selectedMeasures.length} measure(s)?<br><br><strong>${displayNames}</strong><br><br>This action cannot be undone.`,
        'Delete',
        'Cancel',
        'warn'
      )
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }

        this.deleteSelectedLoading = true;
        this.deleteSelectedSuccess = false;

        const idsToDelete = selectedMeasures.map((m) => m.id);
        this.customMeasureService.batchDelete(idsToDelete).pipe(takeUntil(this.destroy$)).subscribe({
          next: (result) => {
            // Remove deleted measures from the data source
            const deletedIds = new Set(idsToDelete.filter((id) => !result.failed.includes(id)));
            this.measures = this.measures.filter(
              (m) => !deletedIds.has(m.id)
            );
            this.drafts = this.drafts.filter((m) => !deletedIds.has(m.id));
            this.dataSource.data = this.measures;

            this.deleteSelectedLoading = false;
            this.deleteSelectedSuccess = true;
            if (result.failed.length > 0) {
              this.toast.warning(
                `Deleted ${result.deleted} measure(s), ${result.failed.length} failed`
              );
            } else {
              this.toast.success(
                `Successfully deleted ${result.deleted} measure(s)`
              );
            }
            this.selection.clear();
          },
          error: (err) => {
            this.deleteSelectedLoading = false;
            this.toast.error(
              `Failed to delete measures: ${err.userMessage || err.message || 'Unknown error'}`
            );
          },
        });
      });
  }
}
