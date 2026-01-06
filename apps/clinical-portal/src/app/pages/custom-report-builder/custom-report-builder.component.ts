import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Subject, finalize, takeUntil } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatRadioModule } from '@angular/material/radio';
import { MatChipsModule } from '@angular/material/chips';
import { MatSliderModule } from '@angular/material/slider';
import { MatStepperModule } from '@angular/material/stepper';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { MatChipInputEvent } from '@angular/material/chips';
import { ReportTemplatesService, ReportTemplate, ReportTemplateCategory, ReportTemplateConfig } from '../../services/report-templates.service';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';

/**
 * Available quality measures that can be added to custom reports
 */
interface QualityMeasure {
  id: string;
  name: string;
  category: string;
  description: string;
  type: 'HEDIS' | 'CMS' | 'CUSTOM';
}

/**
 * Report Section Configuration
 */
interface ReportSection {
  id: string;
  name: string;
  description: string;
  icon: string;
  enabled: boolean;
}

/**
 * Custom Report Builder Component
 *
 * Provides a step-by-step wizard for creating custom quality measure reports.
 *
 * Features:
 * - Step 1: Basic Info - Name, description, category, report type
 * - Step 2: Measure Selection - Drag-and-drop measure picker
 * - Step 3: Report Options - Configure sections, grouping, sorting
 * - Step 4: Preview & Save - Review configuration and save template
 */
@Component({
  selector: 'app-custom-report-builder',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatRadioModule,
    MatChipsModule,
    MatSliderModule,
    MatStepperModule,
    MatExpansionModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatDividerModule,
    DragDropModule,
    LoadingButtonComponent,
  ],
  template: `
    <div class="page">
      <!-- Page Header -->
      <div class="page-header">
        <div class="header-content">
          <button mat-icon-button (click)="goBack()" class="back-button">
            <mat-icon>arrow_back</mat-icon>
          </button>
          <div class="title-section">
            <mat-icon class="page-icon">build</mat-icon>
            <div>
              <h1>Custom Report Builder</h1>
              <p>Create personalized quality measure reports tailored to your needs</p>
            </div>
          </div>
        </div>
        <div class="header-actions">
          @if (editMode) {
            <button mat-stroked-button color="warn" (click)="confirmDelete()">
              <mat-icon>delete</mat-icon>
              Delete Template
            </button>
          }
        </div>
      </div>

      <!-- Stepper Wizard -->
      <mat-stepper [linear]="true" #stepper class="report-stepper">
        <!-- Step 1: Basic Information -->
        <mat-step [stepControl]="basicInfoForm" label="Basic Information">
          <ng-template matStepLabel>
            <mat-icon>info</mat-icon>
            Basic Info
          </ng-template>

          <div class="step-content">
            <mat-card class="form-card">
              <mat-card-header>
                <mat-icon mat-card-avatar>description</mat-icon>
                <mat-card-title>Report Details</mat-card-title>
                <mat-card-subtitle>Define the basic properties of your custom report</mat-card-subtitle>
              </mat-card-header>
              <mat-card-content>
                <form [formGroup]="basicInfoForm" class="form-grid">
                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Report Name</mat-label>
                    <input matInput formControlName="name" placeholder="e.g., Q1 Diabetes Dashboard">
                    <mat-icon matSuffix>badge</mat-icon>
                    @if (basicInfoForm.get('name')?.hasError('required')) {
                      <mat-error>Report name is required</mat-error>
                    }
                  </mat-form-field>

                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Description</mat-label>
                    <textarea matInput formControlName="description" rows="3"
                              placeholder="Describe the purpose of this report..."></textarea>
                    <mat-icon matSuffix>notes</mat-icon>
                  </mat-form-field>

                  <div class="form-row">
                    <mat-form-field appearance="outline">
                      <mat-label>Category</mat-label>
                      <mat-select formControlName="category">
                        @for (category of categories; track category.value) {
                          <mat-option [value]="category.value">
                            <mat-icon>{{ category.icon }}</mat-icon>
                            {{ category.label }}
                          </mat-option>
                        }
                      </mat-select>
                    </mat-form-field>

                    <mat-form-field appearance="outline">
                      <mat-label>Report Type</mat-label>
                      <mat-select formControlName="type">
                        <mat-option value="PATIENT">
                          <mat-icon>person</mat-icon>
                          Patient Report
                        </mat-option>
                        <mat-option value="POPULATION">
                          <mat-icon>groups</mat-icon>
                          Population Report
                        </mat-option>
                        <mat-option value="COMPARATIVE">
                          <mat-icon>compare_arrows</mat-icon>
                          Comparative Report
                        </mat-option>
                      </mat-select>
                    </mat-form-field>
                  </div>

                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Icon</mat-label>
                    <mat-select formControlName="icon">
                      @for (icon of availableIcons; track icon) {
                        <mat-option [value]="icon">
                          <mat-icon>{{ icon }}</mat-icon>
                          {{ icon }}
                        </mat-option>
                      }
                    </mat-select>
                  </mat-form-field>

                  <div class="tags-section">
                    <mat-label>Tags</mat-label>
                    <mat-chip-grid #chipGrid>
                      @for (tag of tags; track tag) {
                        <mat-chip-row (removed)="removeTag(tag)">
                          {{ tag }}
                          <button matChipRemove>
                            <mat-icon>cancel</mat-icon>
                          </button>
                        </mat-chip-row>
                      }
                      <input placeholder="Add tag..."
                             [matChipInputFor]="chipGrid"
                             [matChipInputSeparatorKeyCodes]="separatorKeysCodes"
                             (matChipInputTokenEnd)="addTag($event)">
                    </mat-chip-grid>
                    <mat-hint>Press Enter or comma to add tags</mat-hint>
                  </div>
                </form>
              </mat-card-content>
            </mat-card>
          </div>

          <div class="step-actions">
            <button mat-button matStepperNext [disabled]="!basicInfoForm.valid">
              Next
              <mat-icon>arrow_forward</mat-icon>
            </button>
          </div>
        </mat-step>

        <!-- Step 2: Measure Selection -->
        <mat-step [stepControl]="measuresForm" label="Select Measures">
          <ng-template matStepLabel>
            <mat-icon>checklist</mat-icon>
            Measures
          </ng-template>

          <div class="step-content">
            <div class="measure-selection-container">
              <!-- Available Measures -->
              <mat-card class="measures-card available">
                <mat-card-header>
                  <mat-icon mat-card-avatar>inventory_2</mat-icon>
                  <mat-card-title>Available Measures</mat-card-title>
                  <mat-card-subtitle>Drag measures to add them to your report</mat-card-subtitle>
                </mat-card-header>
                <mat-card-content>
                  <mat-form-field appearance="outline" class="search-field">
                    <mat-label>Search Measures</mat-label>
                    <input matInput [(ngModel)]="measureSearchQuery" placeholder="Search by name or category...">
                    <mat-icon matSuffix>search</mat-icon>
                  </mat-form-field>

                  <div class="measure-filters">
                    <mat-chip-listbox [(value)]="selectedMeasureFilter" class="filter-chips">
                      <mat-chip-option value="all">All</mat-chip-option>
                      <mat-chip-option value="HEDIS">HEDIS</mat-chip-option>
                      <mat-chip-option value="CMS">CMS</mat-chip-option>
                      <mat-chip-option value="CUSTOM">Custom</mat-chip-option>
                    </mat-chip-listbox>
                  </div>

                  <div cdkDropList #availableList="cdkDropList"
                       [cdkDropListData]="filteredAvailableMeasures"
                       [cdkDropListConnectedTo]="[selectedList]"
                       class="measure-list"
                       (cdkDropListDropped)="dropMeasure($event)">
                    @for (measure of filteredAvailableMeasures; track measure.id) {
                      <div class="measure-item" cdkDrag>
                        <div class="measure-drag-preview" *cdkDragPreview>
                          <mat-icon>{{ getMeasureIcon(measure.type) }}</mat-icon>
                          {{ measure.name }}
                        </div>
                        <div class="measure-drag-placeholder" *cdkDragPlaceholder></div>
                        <mat-icon class="drag-handle" cdkDragHandle>drag_indicator</mat-icon>
                        <div class="measure-info">
                          <span class="measure-name">{{ measure.name }}</span>
                          <span class="measure-category">{{ measure.category }}</span>
                        </div>
                        <mat-chip [class]="measure.type.toLowerCase()">{{ measure.type }}</mat-chip>
                        <button mat-icon-button (click)="addMeasure(measure)" matTooltip="Add to report">
                          <mat-icon>add_circle</mat-icon>
                        </button>
                      </div>
                    } @empty {
                      <div class="empty-list">
                        <mat-icon>search_off</mat-icon>
                        <p>No measures found</p>
                      </div>
                    }
                  </div>
                </mat-card-content>
              </mat-card>

              <!-- Selected Measures -->
              <mat-card class="measures-card selected">
                <mat-card-header>
                  <mat-icon mat-card-avatar>playlist_add_check</mat-icon>
                  <mat-card-title>Selected Measures ({{ selectedMeasures.length }})</mat-card-title>
                  <mat-card-subtitle>Drag to reorder measures in your report</mat-card-subtitle>
                </mat-card-header>
                <mat-card-content>
                  <div cdkDropList #selectedList="cdkDropList"
                       [cdkDropListData]="selectedMeasures"
                       [cdkDropListConnectedTo]="[availableList]"
                       class="measure-list selected-list"
                       (cdkDropListDropped)="dropMeasure($event)">
                    @for (measure of selectedMeasures; track measure.id; let i = $index) {
                      <div class="measure-item selected" cdkDrag>
                        <div class="measure-drag-preview" *cdkDragPreview>
                          <mat-icon>{{ getMeasureIcon(measure.type) }}</mat-icon>
                          {{ measure.name }}
                        </div>
                        <div class="measure-drag-placeholder" *cdkDragPlaceholder></div>
                        <span class="measure-order">{{ i + 1 }}</span>
                        <mat-icon class="drag-handle" cdkDragHandle>drag_indicator</mat-icon>
                        <div class="measure-info">
                          <span class="measure-name">{{ measure.name }}</span>
                          <span class="measure-category">{{ measure.category }}</span>
                        </div>
                        <button mat-icon-button color="warn" (click)="removeMeasure(measure)" matTooltip="Remove from report">
                          <mat-icon>remove_circle</mat-icon>
                        </button>
                      </div>
                    } @empty {
                      <div class="empty-list">
                        <mat-icon>playlist_add</mat-icon>
                        <p>Drag measures here or click + to add</p>
                      </div>
                    }
                  </div>

                  @if (selectedMeasures.length > 0) {
                    <div class="selection-actions">
                      <button mat-stroked-button color="warn" (click)="clearAllMeasures()">
                        <mat-icon>clear_all</mat-icon>
                        Clear All
                      </button>
                      <span class="selection-count">{{ selectedMeasures.length }} measures selected</span>
                    </div>
                  }
                </mat-card-content>
              </mat-card>
            </div>
          </div>

          <div class="step-actions">
            <button mat-button matStepperPrevious>
              <mat-icon>arrow_back</mat-icon>
              Back
            </button>
            <button mat-button matStepperNext [disabled]="selectedMeasures.length === 0">
              Next
              <mat-icon>arrow_forward</mat-icon>
            </button>
          </div>
        </mat-step>

        <!-- Step 3: Report Options -->
        <mat-step [stepControl]="optionsForm" label="Configure Options">
          <ng-template matStepLabel>
            <mat-icon>tune</mat-icon>
            Options
          </ng-template>

          <div class="step-content">
            <div class="options-grid">
              <!-- Report Sections -->
              <mat-card class="options-card">
                <mat-card-header>
                  <mat-icon mat-card-avatar>view_module</mat-icon>
                  <mat-card-title>Report Sections</mat-card-title>
                  <mat-card-subtitle>Choose which sections to include</mat-card-subtitle>
                </mat-card-header>
                <mat-card-content>
                  <div class="sections-list">
                    @for (section of reportSections; track section.id) {
                      <div class="section-item" [class.enabled]="section.enabled">
                        <mat-checkbox [(ngModel)]="section.enabled" color="primary">
                          <div class="section-content">
                            <mat-icon>{{ section.icon }}</mat-icon>
                            <div class="section-text">
                              <span class="section-name">{{ section.name }}</span>
                              <span class="section-desc">{{ section.description }}</span>
                            </div>
                          </div>
                        </mat-checkbox>
                      </div>
                    }
                  </div>
                </mat-card-content>
              </mat-card>

              <!-- Grouping & Sorting -->
              <mat-card class="options-card">
                <mat-card-header>
                  <mat-icon mat-card-avatar>sort</mat-icon>
                  <mat-card-title>Organization</mat-card-title>
                  <mat-card-subtitle>Configure how data is grouped and sorted</mat-card-subtitle>
                </mat-card-header>
                <mat-card-content>
                  <form [formGroup]="optionsForm" class="options-form">
                    <div class="option-group">
                      <label class="option-label">Group By</label>
                      <mat-radio-group formControlName="groupBy" class="radio-group">
                        <mat-radio-button value="measure">
                          <mat-icon>assignment</mat-icon>
                          Measure
                        </mat-radio-button>
                        <mat-radio-button value="category">
                          <mat-icon>category</mat-icon>
                          Category
                        </mat-radio-button>
                        <mat-radio-button value="patient">
                          <mat-icon>person</mat-icon>
                          Patient
                        </mat-radio-button>
                      </mat-radio-group>
                    </div>

                    <mat-divider></mat-divider>

                    <div class="option-group">
                      <label class="option-label">Sort By</label>
                      <mat-radio-group formControlName="sortBy" class="radio-group">
                        <mat-radio-button value="compliance">
                          <mat-icon>trending_up</mat-icon>
                          Compliance %
                        </mat-radio-button>
                        <mat-radio-button value="alphabetical">
                          <mat-icon>sort_by_alpha</mat-icon>
                          Alphabetical
                        </mat-radio-button>
                        <mat-radio-button value="category">
                          <mat-icon>category</mat-icon>
                          Category
                        </mat-radio-button>
                      </mat-radio-group>
                    </div>

                    <mat-divider></mat-divider>

                    <div class="option-group">
                      <label class="option-label">Date Range</label>
                      <mat-radio-group formControlName="dateRange" class="radio-group horizontal">
                        <mat-radio-button value="ytd">Year to Date</mat-radio-button>
                        <mat-radio-button value="last-year">Last Year</mat-radio-button>
                        <mat-radio-button value="custom">Custom Range</mat-radio-button>
                      </mat-radio-group>
                    </div>

                    <mat-divider></mat-divider>

                    <div class="option-group">
                      <label class="option-label">Compliance Threshold</label>
                      <div class="slider-container">
                        <mat-slider min="0" max="100" step="5" discrete>
                          <input matSliderThumb formControlName="complianceThreshold">
                        </mat-slider>
                        <span class="threshold-value">{{ optionsForm.get('complianceThreshold')?.value }}%</span>
                      </div>
                      <mat-hint>Highlight measures below this threshold</mat-hint>
                    </div>
                  </form>
                </mat-card-content>
              </mat-card>
            </div>
          </div>

          <div class="step-actions">
            <button mat-button matStepperPrevious>
              <mat-icon>arrow_back</mat-icon>
              Back
            </button>
            <button mat-button matStepperNext>
              Next
              <mat-icon>arrow_forward</mat-icon>
            </button>
          </div>
        </mat-step>

        <!-- Step 4: Preview & Save -->
        <mat-step label="Preview & Save">
          <ng-template matStepLabel>
            <mat-icon>save</mat-icon>
            Save
          </ng-template>

          <div class="step-content">
            <mat-card class="preview-card">
              <mat-card-header>
                <mat-icon mat-card-avatar>{{ basicInfoForm.get('icon')?.value || 'description' }}</mat-icon>
                <mat-card-title>{{ basicInfoForm.get('name')?.value || 'Untitled Report' }}</mat-card-title>
                <mat-card-subtitle>{{ basicInfoForm.get('description')?.value || 'No description' }}</mat-card-subtitle>
              </mat-card-header>
              <mat-card-content>
                <div class="preview-grid">
                  <!-- Summary Stats -->
                  <div class="preview-section">
                    <h3><mat-icon>summarize</mat-icon> Summary</h3>
                    <div class="preview-stats">
                      <div class="stat">
                        <span class="stat-value">{{ selectedMeasures.length }}</span>
                        <span class="stat-label">Measures</span>
                      </div>
                      <div class="stat">
                        <span class="stat-value">{{ getEnabledSectionsCount() }}</span>
                        <span class="stat-label">Sections</span>
                      </div>
                      <div class="stat">
                        <span class="stat-value">{{ basicInfoForm.get('type')?.value }}</span>
                        <span class="stat-label">Report Type</span>
                      </div>
                    </div>
                  </div>

                  <!-- Configuration Details -->
                  <div class="preview-section">
                    <h3><mat-icon>settings</mat-icon> Configuration</h3>
                    <div class="config-list">
                      <div class="config-item">
                        <span class="config-label">Category:</span>
                        <span class="config-value">{{ getCategoryLabel(basicInfoForm.get('category')?.value) }}</span>
                      </div>
                      <div class="config-item">
                        <span class="config-label">Group By:</span>
                        <span class="config-value">{{ optionsForm.get('groupBy')?.value | titlecase }}</span>
                      </div>
                      <div class="config-item">
                        <span class="config-label">Sort By:</span>
                        <span class="config-value">{{ optionsForm.get('sortBy')?.value | titlecase }}</span>
                      </div>
                      <div class="config-item">
                        <span class="config-label">Date Range:</span>
                        <span class="config-value">{{ getDateRangeLabel(optionsForm.get('dateRange')?.value) }}</span>
                      </div>
                      <div class="config-item">
                        <span class="config-label">Threshold:</span>
                        <span class="config-value">{{ optionsForm.get('complianceThreshold')?.value }}%</span>
                      </div>
                    </div>
                  </div>

                  <!-- Included Sections -->
                  <div class="preview-section">
                    <h3><mat-icon>view_list</mat-icon> Included Sections</h3>
                    <div class="section-tags">
                      @for (section of getEnabledSections(); track section.id) {
                        <mat-chip>
                          <mat-icon>{{ section.icon }}</mat-icon>
                          {{ section.name }}
                        </mat-chip>
                      }
                    </div>
                  </div>

                  <!-- Selected Measures -->
                  <div class="preview-section full-width">
                    <h3><mat-icon>checklist</mat-icon> Selected Measures</h3>
                    <div class="measures-preview">
                      @for (measure of selectedMeasures; track measure.id) {
                        <div class="measure-preview-item">
                          <mat-icon>{{ getMeasureIcon(measure.type) }}</mat-icon>
                          <span>{{ measure.name }}</span>
                          <mat-chip size="small">{{ measure.category }}</mat-chip>
                        </div>
                      }
                    </div>
                  </div>

                  <!-- Tags -->
                  @if (tags.length > 0) {
                    <div class="preview-section full-width">
                      <h3><mat-icon>label</mat-icon> Tags</h3>
                      <div class="tag-chips">
                        @for (tag of tags; track tag) {
                          <mat-chip>{{ tag }}</mat-chip>
                        }
                      </div>
                    </div>
                  }
                </div>
              </mat-card-content>
            </mat-card>

            <!-- Save Options -->
            <mat-card class="save-options-card">
              <mat-card-content>
                <div class="save-options">
                  <mat-checkbox [(ngModel)]="saveAsFavorite" color="primary">
                    <mat-icon>star</mat-icon>
                    Add to Favorites
                  </mat-checkbox>
                  <mat-checkbox [(ngModel)]="generateImmediately" color="primary">
                    <mat-icon>play_arrow</mat-icon>
                    Generate Report Immediately
                  </mat-checkbox>
                </div>
              </mat-card-content>
            </mat-card>
          </div>

          <div class="step-actions final">
            <button mat-button matStepperPrevious>
              <mat-icon>arrow_back</mat-icon>
              Back
            </button>
            <app-loading-button
              [loading]="saving"
              [disabled]="!isFormValid()"
              color="primary"
              icon="save"
              (clicked)="saveTemplate()">
              {{ editMode ? 'Update Template' : 'Save Template' }}
            </app-loading-button>
          </div>
        </mat-step>
      </mat-stepper>
    </div>
  `,
  styles: [`
    .page {
      padding: 24px;
      max-width: 1400px;
      margin: 0 auto;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 24px;
      gap: 16px;
      flex-wrap: wrap;
    }

    .header-content {
      display: flex;
      align-items: flex-start;
      gap: 16px;
    }

    .back-button {
      margin-top: 4px;
    }

    .title-section {
      display: flex;
      align-items: flex-start;
      gap: 12px;
    }

    .page-icon {
      font-size: 36px;
      width: 36px;
      height: 36px;
      color: #1976d2;
    }

    h1 {
      margin: 0;
      font-size: 28px;
      font-weight: 500;
      color: #333;
    }

    .title-section p {
      margin: 4px 0 0 0;
      color: #666;
      font-size: 14px;
    }

    .report-stepper {
      background: white;
      border-radius: 12px;
      padding: 24px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .step-content {
      padding: 24px 0;
    }

    .step-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      padding-top: 16px;
      border-top: 1px solid #e0e0e0;
      margin-top: 16px;

      &.final {
        justify-content: space-between;
      }
    }

    /* Form Card */
    .form-card {
      max-width: 700px;
      margin: 0 auto;
    }

    .form-grid {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .full-width {
      width: 100%;
    }

    .form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    .tags-section {
      display: flex;
      flex-direction: column;
      gap: 8px;

      mat-chip-grid {
        border: 1px solid #ccc;
        border-radius: 8px;
        padding: 8px;
        min-height: 48px;
      }
    }

    /* Measure Selection */
    .measure-selection-container {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 24px;
    }

    .measures-card {
      height: fit-content;
    }

    .search-field {
      width: 100%;
      margin-bottom: 12px;
    }

    .measure-filters {
      margin-bottom: 16px;
    }

    .filter-chips {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .measure-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
      min-height: 300px;
      max-height: 400px;
      overflow-y: auto;
      padding: 8px;
      background: #fafafa;
      border-radius: 8px;
      border: 2px dashed #e0e0e0;
    }

    .selected-list {
      background: #e8f5e9;
      border-color: #4caf50;
    }

    .measure-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      background: white;
      border-radius: 8px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
      cursor: grab;

      &:active {
        cursor: grabbing;
      }

      &.selected {
        border-left: 4px solid #4caf50;
      }
    }

    .measure-order {
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #4caf50;
      color: white;
      border-radius: 50%;
      font-size: 12px;
      font-weight: 600;
    }

    .drag-handle {
      color: #999;
      cursor: grab;
    }

    .measure-info {
      flex: 1;
      display: flex;
      flex-direction: column;
    }

    .measure-name {
      font-weight: 500;
      color: #333;
    }

    .measure-category {
      font-size: 12px;
      color: #666;
    }

    mat-chip.hedis {
      background: #e3f2fd !important;
      color: #1565c0 !important;
    }

    mat-chip.cms {
      background: #f3e5f5 !important;
      color: #7b1fa2 !important;
    }

    mat-chip.custom {
      background: #fff3e0 !important;
      color: #e65100 !important;
    }

    .empty-list {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px;
      color: #999;

      mat-icon {
        font-size: 48px;
        width: 48px;
        height: 48px;
        margin-bottom: 12px;
      }
    }

    .selection-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #e0e0e0;
    }

    .selection-count {
      color: #666;
      font-size: 14px;
    }

    .measure-drag-preview {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      background: white;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    .measure-drag-placeholder {
      height: 52px;
      background: #e3f2fd;
      border-radius: 8px;
      border: 2px dashed #1976d2;
    }

    /* Options Grid */
    .options-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 24px;
    }

    .options-card {
      height: fit-content;
    }

    .sections-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .section-item {
      padding: 12px;
      border-radius: 8px;
      background: #fafafa;
      transition: all 0.2s;

      &.enabled {
        background: #e8f5e9;
      }
    }

    .section-content {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .section-text {
      display: flex;
      flex-direction: column;
    }

    .section-name {
      font-weight: 500;
      color: #333;
    }

    .section-desc {
      font-size: 12px;
      color: #666;
    }

    .options-form {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .option-group {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .option-label {
      font-weight: 500;
      color: #333;
      font-size: 14px;
    }

    .radio-group {
      display: flex;
      flex-direction: column;
      gap: 8px;

      &.horizontal {
        flex-direction: row;
        flex-wrap: wrap;
      }

      mat-radio-button {
        mat-icon {
          margin-right: 4px;
          font-size: 18px;
          width: 18px;
          height: 18px;
          vertical-align: middle;
        }
      }
    }

    .slider-container {
      display: flex;
      align-items: center;
      gap: 16px;

      mat-slider {
        flex: 1;
      }
    }

    .threshold-value {
      min-width: 48px;
      font-weight: 600;
      color: #1976d2;
    }

    /* Preview Card */
    .preview-card {
      margin-bottom: 24px;
    }

    .preview-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 24px;
    }

    .preview-section {
      padding: 16px;
      background: #fafafa;
      border-radius: 8px;

      &.full-width {
        grid-column: span 3;
      }

      h3 {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0 0 16px 0;
        font-size: 16px;
        font-weight: 500;
        color: #333;

        mat-icon {
          color: #1976d2;
        }
      }
    }

    .preview-stats {
      display: flex;
      gap: 24px;
    }

    .stat {
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    .stat-value {
      font-size: 28px;
      font-weight: 600;
      color: #1976d2;
    }

    .stat-label {
      font-size: 12px;
      color: #666;
    }

    .config-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .config-item {
      display: flex;
      justify-content: space-between;
      padding: 4px 0;
    }

    .config-label {
      color: #666;
    }

    .config-value {
      font-weight: 500;
      color: #333;
    }

    .section-tags,
    .tag-chips {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .measures-preview {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
      gap: 8px;
    }

    .measure-preview-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      background: white;
      border-radius: 8px;
      border: 1px solid #e0e0e0;

      mat-icon {
        color: #1976d2;
      }

      span {
        flex: 1;
        font-size: 14px;
      }
    }

    .save-options-card {
      .save-options {
        display: flex;
        gap: 32px;

        mat-checkbox {
          mat-icon {
            margin-right: 8px;
            color: #1976d2;
          }
        }
      }
    }

    /* CDK Drag Drop */
    .cdk-drag-animating {
      transition: transform 250ms cubic-bezier(0, 0, 0.2, 1);
    }

    .measure-list.cdk-drop-list-dragging .measure-item:not(.cdk-drag-placeholder) {
      transition: transform 250ms cubic-bezier(0, 0, 0.2, 1);
    }

    /* Responsive */
    @media (max-width: 1024px) {
      .measure-selection-container,
      .options-grid {
        grid-template-columns: 1fr;
      }

      .preview-grid {
        grid-template-columns: 1fr;
      }

      .preview-section.full-width {
        grid-column: span 1;
      }
    }

    @media (max-width: 600px) {
      .page {
        padding: 16px;
      }

      .form-row {
        grid-template-columns: 1fr;
      }

      .header-content {
        flex-direction: column;
      }
    }
  `],
})
export class CustomReportBuilderComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  // Form groups
  basicInfoForm!: FormGroup;
  measuresForm!: FormGroup;
  optionsForm!: FormGroup;

  // Tags
  tags: string[] = [];
  readonly separatorKeysCodes = [ENTER, COMMA] as const;

  // Measures
  availableMeasures: QualityMeasure[] = [];
  selectedMeasures: QualityMeasure[] = [];
  measureSearchQuery = '';
  selectedMeasureFilter = 'all';

  // Report Sections
  reportSections: ReportSection[] = [
    { id: 'charts', name: 'Charts & Visualizations', description: 'Include bar charts, pie charts, and trend lines', icon: 'bar_chart', enabled: true },
    { id: 'details', name: 'Detailed Breakdown', description: 'Show measure-by-measure results with patient counts', icon: 'table_chart', enabled: true },
    { id: 'trends', name: 'Historical Trends', description: 'Compare performance over time periods', icon: 'trending_up', enabled: true },
    { id: 'careGaps', name: 'Care Gap Analysis', description: 'Highlight open care gaps and opportunities', icon: 'warning', enabled: true },
    { id: 'recommendations', name: 'AI Recommendations', description: 'Include AI-generated improvement suggestions', icon: 'psychology', enabled: true },
  ];

  // Categories
  categories: { value: ReportTemplateCategory; label: string; icon: string }[] = [];

  // Available icons
  availableIcons = [
    'assessment', 'bar_chart', 'pie_chart', 'analytics', 'insights',
    'trending_up', 'health_and_safety', 'medical_services', 'bloodtype',
    'favorite', 'psychology', 'medication', 'biotech', 'vaccines',
    'person', 'groups', 'compare_arrows', 'star', 'verified',
  ];

  // Save options
  saveAsFavorite = false;
  generateImmediately = false;
  saving = false;

  // Edit mode
  editMode = false;
  editTemplateId: string | null = null;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar,
    private reportTemplatesService: ReportTemplatesService,
  ) {}

  ngOnInit(): void {
    this.initializeForms();
    this.loadCategories();
    this.loadAvailableMeasures();
    this.checkEditMode();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForms(): void {
    this.basicInfoForm = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      category: ['CUSTOM'],
      type: ['POPULATION'],
      icon: ['assessment'],
    });

    this.measuresForm = this.fb.group({
      measures: [[]],
    });

    this.optionsForm = this.fb.group({
      groupBy: ['measure'],
      sortBy: ['compliance'],
      dateRange: ['ytd'],
      complianceThreshold: [80],
    });
  }

  private loadCategories(): void {
    this.categories = this.reportTemplatesService.getCategories();
  }

  private loadAvailableMeasures(): void {
    // Sample measures - in production, this would come from a service
    this.availableMeasures = [
      { id: 'BCS', name: 'Breast Cancer Screening', category: 'Preventive', type: 'HEDIS', description: 'Women 50-74 with mammogram' },
      { id: 'COL', name: 'Colorectal Cancer Screening', category: 'Preventive', type: 'HEDIS', description: 'Adults 50-75 with appropriate screening' },
      { id: 'CCS', name: 'Cervical Cancer Screening', category: 'Preventive', type: 'HEDIS', description: 'Women 21-64 with Pap test or HPV test' },
      { id: 'CDC-HBA1C', name: 'Diabetes HbA1c Testing', category: 'Diabetes', type: 'HEDIS', description: 'Diabetics with HbA1c test' },
      { id: 'CDC-HBA1C-CONTROL', name: 'Diabetes HbA1c Control <8%', category: 'Diabetes', type: 'HEDIS', description: 'Diabetics with HbA1c <8%' },
      { id: 'CDC-EYE', name: 'Diabetes Eye Exam', category: 'Diabetes', type: 'HEDIS', description: 'Diabetics with retinal exam' },
      { id: 'CDC-BP', name: 'Diabetes Blood Pressure Control', category: 'Diabetes', type: 'HEDIS', description: 'Diabetics with BP <140/90' },
      { id: 'CBP', name: 'Controlling Blood Pressure', category: 'Cardiovascular', type: 'HEDIS', description: 'Adults with controlled BP <140/90' },
      { id: 'SPC', name: 'Statin Therapy - Cardiovascular', category: 'Cardiovascular', type: 'HEDIS', description: 'Statin adherence for CVD patients' },
      { id: 'SPD', name: 'Statin Therapy - Diabetes', category: 'Cardiovascular', type: 'HEDIS', description: 'Statin adherence for diabetics' },
      { id: 'FUH-7', name: 'Follow-Up After Hospitalization - 7 Day', category: 'Behavioral Health', type: 'HEDIS', description: 'Mental health follow-up within 7 days' },
      { id: 'FUH-30', name: 'Follow-Up After Hospitalization - 30 Day', category: 'Behavioral Health', type: 'HEDIS', description: 'Mental health follow-up within 30 days' },
      { id: 'AMM', name: 'Antidepressant Medication Management', category: 'Behavioral Health', type: 'HEDIS', description: 'Antidepressant adherence' },
      { id: 'MRP', name: 'Medication Reconciliation Post-Discharge', category: 'Medication', type: 'HEDIS', description: 'Med rec within 30 days of discharge' },
      { id: 'ACO-01', name: 'ACO CAHPS Survey', category: 'CMS', type: 'CMS', description: 'Patient experience survey' },
      { id: 'ACO-08', name: 'Risk-Standardized Acute Admission Rate', category: 'CMS', type: 'CMS', description: 'Acute admission rate measure' },
      { id: 'ACO-38', name: 'All-Cause Unplanned Admissions', category: 'CMS', type: 'CMS', description: 'Risk-adjusted unplanned admissions' },
      { id: 'ACO-40', name: 'Depression Remission at 12 Months', category: 'Behavioral Health', type: 'CMS', description: 'PHQ-9 score improvement' },
    ];
  }

  private checkEditMode(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      if (params['id']) {
        this.editMode = true;
        this.editTemplateId = params['id'];
        this.loadTemplate(params['id']);
      }
    });
  }

  private loadTemplate(templateId: string): void {
    const template = this.reportTemplatesService.getTemplate(templateId);
    if (template) {
      this.basicInfoForm.patchValue({
        name: template.name,
        description: template.description,
        category: template.category,
        type: template.type,
        icon: template.icon,
      });
      this.tags = [...template.tags];

      // Load measures
      this.selectedMeasures = this.availableMeasures.filter(m =>
        template.measures.includes(m.id)
      );

      // Load options
      this.optionsForm.patchValue({
        groupBy: template.configuration.groupBy,
        sortBy: template.configuration.sortBy,
        dateRange: template.configuration.dateRange || 'ytd',
        complianceThreshold: template.configuration.complianceThreshold || 80,
      });

      // Load sections
      this.reportSections.forEach(section => {
        switch (section.id) {
          case 'charts': section.enabled = template.configuration.includeCharts; break;
          case 'details': section.enabled = template.configuration.includeDetails; break;
          case 'trends': section.enabled = template.configuration.includeTrends; break;
          case 'careGaps': section.enabled = template.configuration.includeCareGaps; break;
          case 'recommendations': section.enabled = template.configuration.includeRecommendations; break;
        }
      });

      this.saveAsFavorite = template.isFavorite;
    }
  }

  get filteredAvailableMeasures(): QualityMeasure[] {
    return this.availableMeasures.filter(m => {
      // Filter out already selected
      if (this.selectedMeasures.some(s => s.id === m.id)) return false;

      // Filter by type
      if (this.selectedMeasureFilter !== 'all' && m.type !== this.selectedMeasureFilter) return false;

      // Filter by search query
      if (this.measureSearchQuery) {
        const query = this.measureSearchQuery.toLowerCase();
        return m.name.toLowerCase().includes(query) ||
               m.category.toLowerCase().includes(query) ||
               m.id.toLowerCase().includes(query);
      }

      return true;
    });
  }

  // Tag management
  addTag(event: MatChipInputEvent): void {
    const value = (event.value || '').trim();
    if (value && !this.tags.includes(value)) {
      this.tags.push(value);
    }
    event.chipInput.clear();
  }

  removeTag(tag: string): void {
    const index = this.tags.indexOf(tag);
    if (index >= 0) {
      this.tags.splice(index, 1);
    }
  }

  // Measure management
  dropMeasure(event: CdkDragDrop<QualityMeasure[]>): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex,
      );
    }
  }

  addMeasure(measure: QualityMeasure): void {
    this.selectedMeasures.push(measure);
  }

  removeMeasure(measure: QualityMeasure): void {
    const index = this.selectedMeasures.findIndex(m => m.id === measure.id);
    if (index >= 0) {
      this.selectedMeasures.splice(index, 1);
    }
  }

  clearAllMeasures(): void {
    this.selectedMeasures = [];
  }

  getMeasureIcon(type: string): string {
    switch (type) {
      case 'HEDIS': return 'assessment';
      case 'CMS': return 'verified';
      case 'CUSTOM': return 'edit';
      default: return 'description';
    }
  }

  // Helpers
  getEnabledSections(): ReportSection[] {
    return this.reportSections.filter(s => s.enabled);
  }

  getEnabledSectionsCount(): number {
    return this.reportSections.filter(s => s.enabled).length;
  }

  getCategoryLabel(value: string): string {
    const category = this.categories.find(c => c.value === value);
    return category?.label || value;
  }

  getDateRangeLabel(value: string): string {
    switch (value) {
      case 'ytd': return 'Year to Date';
      case 'last-year': return 'Last Year';
      case 'custom': return 'Custom Range';
      default: return value;
    }
  }

  isFormValid(): boolean {
    return this.basicInfoForm.valid && this.selectedMeasures.length > 0;
  }

  // Actions
  goBack(): void {
    this.router.navigate(['/reports']);
  }

  confirmDelete(): void {
    if (this.editTemplateId && confirm('Are you sure you want to delete this template?')) {
      const success = this.reportTemplatesService.deleteCustomTemplate(this.editTemplateId);
      if (success) {
        this.snackBar.open('Template deleted successfully', 'Close', { duration: 3000 });
        this.router.navigate(['/reports']);
      } else {
        this.snackBar.open('Failed to delete template', 'Close', { duration: 3000 });
      }
    }
  }

  saveTemplate(): void {
    if (!this.isFormValid()) return;

    this.saving = true;

    const config: ReportTemplateConfig = {
      includeCharts: this.reportSections.find(s => s.id === 'charts')?.enabled ?? true,
      includeDetails: this.reportSections.find(s => s.id === 'details')?.enabled ?? true,
      includeTrends: this.reportSections.find(s => s.id === 'trends')?.enabled ?? true,
      includeCareGaps: this.reportSections.find(s => s.id === 'careGaps')?.enabled ?? true,
      includeRecommendations: this.reportSections.find(s => s.id === 'recommendations')?.enabled ?? true,
      groupBy: this.optionsForm.get('groupBy')?.value,
      sortBy: this.optionsForm.get('sortBy')?.value,
      dateRange: this.optionsForm.get('dateRange')?.value,
      complianceThreshold: this.optionsForm.get('complianceThreshold')?.value,
    };

    const templateData = {
      name: this.basicInfoForm.get('name')?.value,
      description: this.basicInfoForm.get('description')?.value,
      category: this.basicInfoForm.get('category')?.value as ReportTemplateCategory,
      type: this.basicInfoForm.get('type')?.value as 'PATIENT' | 'POPULATION' | 'COMPARATIVE',
      icon: this.basicInfoForm.get('icon')?.value,
      tags: this.tags,
      measures: this.selectedMeasures.map(m => m.id),
      isFavorite: this.saveAsFavorite,
      configuration: config,
    };

    // Simulate save delay
    setTimeout(() => {
      try {
        if (this.editMode && this.editTemplateId) {
          this.reportTemplatesService.updateCustomTemplate(this.editTemplateId, templateData);
          this.snackBar.open('Template updated successfully!', 'Close', { duration: 3000 });
        } else {
          this.reportTemplatesService.createCustomTemplate(templateData);
          this.snackBar.open('Template created successfully!', 'Close', { duration: 3000 });
        }

        if (this.generateImmediately) {
          this.router.navigate(['/reports'], { queryParams: { generate: 'true' } });
        } else {
          this.router.navigate(['/reports']);
        }
      } catch (error) {
        this.snackBar.open('Failed to save template', 'Close', { duration: 3000 });
      } finally {
        this.saving = false;
      }
    }, 1000);
  }
}
