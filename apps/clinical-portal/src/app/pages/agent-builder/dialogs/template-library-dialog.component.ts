import { Component, Inject, OnInit, OnDestroy, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormControl } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subject, takeUntil, debounceTime, distinctUntilChanged } from 'rxjs';

import { AgentBuilderService } from '../services/agent-builder.service';
import { PromptTemplate, TemplateCategory } from '../models/agent.model';
import { ToastService } from '../../../services/toast.service';
import { LoggerService } from '../../../services/logger.service';
import { MatDialog } from '@angular/material/dialog';
import { CreateTemplateDialogComponent } from './create-template-dialog.component';

export interface TemplateLibraryDialogData {
  mode: 'select' | 'browse';
}

@Component({
  selector: 'app-template-library-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatChipsModule,
    MatCardModule,
    MatDividerModule,
    MatSelectModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="template-library-dialog">
      <div class="dialog-header">
        <h2>
          <mat-icon>library_books</mat-icon>
          Prompt Template Library
        </h2>
        <button mat-icon-button (click)="onClose()" aria-label="Close dialog">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <mat-divider></mat-divider>

      <div class="dialog-body">
        <!-- Left Panel: Table -->
        <div class="table-panel">
          <!-- Search and Filters -->
          <div class="filters-row">
            <mat-form-field appearance="outline" class="search-field">
              <mat-label>Search templates</mat-label>
              <mat-icon matPrefix>search</mat-icon>
              <input
                matInput
                placeholder="Search by name or description..."
                [formControl]="searchControl"
                aria-label="Search templates" />
              @if (searchControl.value) {
                <button matSuffix mat-icon-button (click)="searchControl.reset()" aria-label="Clear search">
                  <mat-icon>close</mat-icon>
                </button>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="category-filter">
              <mat-label>Category</mat-label>
              <mat-select [(value)]="selectedCategory" (selectionChange)="onCategoryChange()">
                <mat-option value="">All Categories</mat-option>
                @for (category of categories; track category.value) {
                  <mat-option [value]="category.value">
                    {{ category.label }}
                  </mat-option>
                }
              </mat-select>
            </mat-form-field>
          </div>

          <!-- Loading State -->
          @if (loading) {
            <div class="loading-state">
              <mat-spinner diameter="40"></mat-spinner>
              <p>Loading templates...</p>
            </div>
          }

          <!-- Templates Table -->
          @if (!loading && dataSource.data.length > 0) {
            <div class="table-container">
              <table mat-table [dataSource]="dataSource" matSort class="templates-table">
                <!-- Name Column -->
                <ng-container matColumnDef="name">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Name</th>
                  <td mat-cell *matCellDef="let template">
                    <div class="template-name-cell">
                      <span class="template-name">{{ template.name }}</span>
                      @if (template.isSystem) {
                        <mat-chip size="small" class="system-badge">System</mat-chip>
                      }
                    </div>
                  </td>
                </ng-container>

                <!-- Category Column -->
                <ng-container matColumnDef="category">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Category</th>
                  <td mat-cell *matCellDef="let template">
                    <mat-chip size="small">{{ formatCategory(template.category) }}</mat-chip>
                  </td>
                </ng-container>

                <!-- Usage Column -->
                <ng-container matColumnDef="usageCount">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Used</th>
                  <td mat-cell *matCellDef="let template">
                    {{ template.usageCount || 0 }}
                  </td>
                </ng-container>

                <!-- Actions Column -->
                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef>Actions</th>
                  <td mat-cell *matCellDef="let template">
                    <button
                      mat-icon-button
                      (click)="selectTemplate(template)"
                      [matTooltip]="data.mode === 'select' ? 'Use template' : 'View template'"
                      [attr.aria-label]="data.mode === 'select' ? 'Use template ' + template.name : 'View template ' + template.name">
                      <mat-icon>{{ data.mode === 'select' ? 'check_circle' : 'visibility' }}</mat-icon>
                    </button>
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr
                  mat-row
                  *matRowDef="let row; columns: displayedColumns"
                  class="template-row"
                  [class.selected]="selectedTemplate?.id === row.id"
                  (click)="selectTemplate(row)">
                </tr>
              </table>
            </div>

            <mat-paginator
              [pageSizeOptions]="[10, 25, 50]"
              [pageSize]="10"
              showFirstLastButtons
              aria-label="Select page of templates">
            </mat-paginator>
          }

          <!-- Empty State -->
          @if (!loading && dataSource.data.length === 0) {
            <div class="empty-state">
              <mat-icon>library_books</mat-icon>
              <h3>No Templates Found</h3>
              @if (searchControl.value || selectedCategory) {
                <p>Try adjusting your search or filters</p>
              } @else {
                <p>No templates available yet</p>
              }
            </div>
          }
        </div>

        <!-- Right Panel: Preview -->
        <div class="preview-panel">
          @if (selectedTemplate) {
            <mat-card class="preview-card">
              <mat-card-header>
                <mat-card-title>{{ selectedTemplate.name }}</mat-card-title>
                <mat-card-subtitle>{{ formatCategory(selectedTemplate.category) }}</mat-card-subtitle>
              </mat-card-header>

              <mat-card-content>
                @if (selectedTemplate.description) {
                  <div class="template-description">
                    <strong>Description:</strong>
                    <p>{{ selectedTemplate.description }}</p>
                  </div>
                }

                <mat-divider></mat-divider>

                <div class="template-content">
                  <strong>Content:</strong>
                  <pre class="content-preview">{{ selectedTemplate.content }}</pre>
                </div>

                @if (selectedTemplate.variables && selectedTemplate.variables.length > 0) {
                  <mat-divider></mat-divider>

                  <div class="template-variables">
                    <strong>Variables:</strong>
                    <div class="variables-list">
                      @for (variable of selectedTemplate.variables; track variable.name) {
                        <div class="variable-item">
                          <code>{{ '{{' + variable.name + '}}' }}</code>
                          @if (variable.description) {
                            <span class="variable-desc">{{ variable.description }}</span>
                          }
                          @if (variable.required) {
                            <mat-chip size="small" class="required-chip">Required</mat-chip>
                          }
                        </div>
                      }
                    </div>
                  </div>
                }

                <mat-divider></mat-divider>

                <div class="template-meta">
                  <div class="meta-item">
                    <mat-icon>person</mat-icon>
                    <span>Created by: {{ selectedTemplate.createdBy }}</span>
                  </div>
                  <div class="meta-item">
                    <mat-icon>schedule</mat-icon>
                    <span>{{ selectedTemplate.createdAt | date: 'short' }}</span>
                  </div>
                  @if (selectedTemplate.usageCount) {
                    <div class="meta-item">
                      <mat-icon>trending_up</mat-icon>
                      <span>Used {{ selectedTemplate.usageCount }} times</span>
                    </div>
                  }
                </div>
              </mat-card-content>

              @if (data.mode === 'select') {
                <mat-card-actions>
                  <button mat-flat-button color="primary" (click)="useTemplate()">
                    <mat-icon>check_circle</mat-icon>
                    Use This Template
                  </button>
                </mat-card-actions>
              }
            </mat-card>
          } @else {
            <div class="no-selection">
              <mat-icon>preview</mat-icon>
              <p>Select a template to preview</p>
            </div>
          }
        </div>
      </div>

      <mat-divider></mat-divider>

      <div class="dialog-footer">
        <button mat-button (click)="onClose()">
          {{ data.mode === 'select' ? 'Cancel' : 'Close' }}
        </button>
        @if (data.mode === 'browse') {
          <button mat-flat-button color="primary" (click)="openCreateDialog()">
            <mat-icon>add</mat-icon>
            Create Template
          </button>
        }
      </div>
    </div>
  `,
  styles: [`
    .template-library-dialog {
      display: flex;
      flex-direction: column;
      height: 80vh;
      width: 90vw;
      max-width: 1200px;
    }

    .dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 24px;

      h2 {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0;
      }
    }

    .dialog-body {
      display: flex;
      flex: 1;
      min-height: 0;
      gap: 16px;
      padding: 16px 24px;
    }

    .table-panel {
      flex: 1;
      display: flex;
      flex-direction: column;
      min-width: 0;
    }

    .filters-row {
      display: flex;
      gap: 16px;
      margin-bottom: 16px;

      .search-field {
        flex: 1;
      }

      .category-filter {
        width: 200px;
      }

      mat-form-field {
        ::ng-deep .mat-mdc-form-field-subscript-wrapper {
          display: none;
        }
      }
    }

    .loading-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 400px;
      gap: 16px;
      color: var(--mat-sys-on-surface-variant);
    }

    .table-container {
      flex: 1;
      overflow: auto;
      border: 1px solid var(--mat-sys-outline-variant);
      border-radius: 8px;
    }

    .templates-table {
      width: 100%;

      .template-name-cell {
        display: flex;
        align-items: center;
        gap: 8px;

        .template-name {
          font-weight: 500;
        }

        .system-badge {
          background: var(--mat-sys-tertiary-container);
          color: var(--mat-sys-on-tertiary-container);
        }
      }

      .template-row {
        cursor: pointer;
        transition: background-color 0.2s;

        &:hover {
          background: var(--mat-sys-surface-variant);
        }

        &.selected {
          background: var(--mat-sys-primary-container);
          color: var(--mat-sys-on-primary-container);
        }
      }
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 400px;
      text-align: center;
      color: var(--mat-sys-on-surface-variant);

      mat-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        opacity: 0.5;
      }

      h3 {
        margin: 16px 0 8px;
      }

      p {
        margin: 0;
      }
    }

    .preview-panel {
      width: 400px;
      display: flex;
      flex-direction: column;
    }

    .preview-card {
      flex: 1;
      overflow-y: auto;

      .template-description {
        margin-bottom: 16px;

        p {
          margin: 8px 0 0;
          color: var(--mat-sys-on-surface-variant);
        }
      }

      mat-divider {
        margin: 16px 0;
      }

      .template-content {
        .content-preview {
          margin: 8px 0 0;
          padding: 12px;
          background: var(--mat-sys-surface-variant);
          border-radius: 8px;
          overflow-x: auto;
          font-size: 0.875rem;
          max-height: 200px;
          overflow-y: auto;
        }
      }

      .template-variables {
        .variables-list {
          margin-top: 8px;
        }

        .variable-item {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 8px 0;
          border-bottom: 1px solid var(--mat-sys-outline-variant);

          code {
            font-family: monospace;
            background: var(--mat-sys-surface-variant);
            padding: 2px 6px;
            border-radius: 4px;
          }

          .variable-desc {
            flex: 1;
            font-size: 0.8125rem;
            color: var(--mat-sys-on-surface-variant);
          }

          .required-chip {
            background: var(--mat-sys-error-container);
            color: var(--mat-sys-on-error-container);
          }

          &:last-child {
            border-bottom: none;
          }
        }
      }

      .template-meta {
        display: flex;
        flex-direction: column;
        gap: 8px;

        .meta-item {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 0.8125rem;
          color: var(--mat-sys-on-surface-variant);

          mat-icon {
            font-size: 18px;
            width: 18px;
            height: 18px;
          }
        }
      }

      mat-card-actions {
        padding: 16px;
      }
    }

    .no-selection {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      text-align: center;
      color: var(--mat-sys-on-surface-variant);

      mat-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        opacity: 0.5;
      }

      p {
        margin: 16px 0 0;
      }
    }

    .dialog-footer {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
      padding: 16px 24px;
    }
  `],
})
export class TemplateLibraryDialogComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  private destroy$ = new Subject<void>();
  private logger = this.logger.withContext('TemplateLibraryDialogComponent');

  dataSource = new MatTableDataSource<PromptTemplate>([]);
  displayedColumns = ['name', 'category', 'usageCount', 'actions'];

  templates: PromptTemplate[] = [];
  selectedTemplate: PromptTemplate | null = null;
  loading = false;

  searchControl = new FormControl('');
  selectedCategory: TemplateCategory | '' = '';

  categories = [
    { value: 'SYSTEM_PROMPT', label: 'System Prompt' },
    { value: 'CAPABILITIES', label: 'Capabilities' },
    { value: 'CONSTRAINTS', label: 'Constraints' },
    { value: 'RESPONSE_FORMAT', label: 'Response Format' },
    { value: 'CLINICAL_SAFETY', label: 'Clinical Safety' },
    { value: 'TOOL_USAGE', label: 'Tool Usage' },
    { value: 'PERSONA', label: 'Persona' },
    { value: 'CUSTOM', label: 'Custom' },
  ];

  constructor(
    private agentService: AgentBuilderService,
    private toast: ToastService,
    private logger: LoggerService,
    private dialog: MatDialog,
    private dialogRef: MatDialogRef<TemplateLibraryDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: TemplateLibraryDialogData
  ) {}

  ngOnInit(): void {
    this.loadTemplates();
    this.setupSearch();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupSearch(): void {
    this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.applyFilters();
      });
  }

  loadTemplates(): void {
    this.loading = true;
    this.agentService
      .listTemplates()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (templates) => {
          this.templates = templates;
          this.dataSource.data = templates;
          this.loading = false;
          this.logger.info('Loaded templates', { count: templates.length });
        },
        error: (err) => {
          this.loading = false;
          this.logger.error('Failed to load templates', err);
          this.toast.error('Failed to load templates');
        },
      });
  }

  onCategoryChange(): void {
    this.applyFilters();
  }

  private applyFilters(): void {
    let filtered = this.templates;

    // Filter by category
    if (this.selectedCategory) {
      filtered = filtered.filter((t) => t.category === this.selectedCategory);
    }

    // Filter by search
    const search = this.searchControl.value?.toLowerCase() || '';
    if (search) {
      filtered = filtered.filter(
        (t) =>
          t.name.toLowerCase().includes(search) ||
          t.description?.toLowerCase().includes(search)
      );
    }

    this.dataSource.data = filtered;
  }

  selectTemplate(template: PromptTemplate): void {
    this.selectedTemplate = template;
    this.logger.info('Selected template', { templateId: template.id });
  }

  useTemplate(): void {
    if (this.selectedTemplate) {
      this.logger.info('Using template', { templateId: this.selectedTemplate.id });
      this.dialogRef.close(this.selectedTemplate);
    }
  }

  formatCategory(category: TemplateCategory): string {
    return category
      .split('_')
      .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
      .join(' ');
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(CreateTemplateDialogComponent, {
      width: '700px',
      data: { isEdit: false },
    });

    dialogRef.afterClosed().subscribe((newTemplate) => {
      if (newTemplate) {
        this.logger.info('Template created', { templateId: newTemplate.id });
        this.loadTemplates(); // Refresh list
        this.selectTemplate(newTemplate); // Auto-select new template
      }
    });
  }

  onClose(): void {
    this.dialogRef.close();
  }
}
