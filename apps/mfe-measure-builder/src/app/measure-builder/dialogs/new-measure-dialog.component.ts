import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { CreateCustomMeasureRequest } from '../../services/custom-measure.service';
import {
  MeasureTemplate,
  MeasureTemplateCategory,
  PRIMARY_CARE_TEMPLATES,
  getTemplatesByCategory,
  searchTemplates,
} from '../../models/measure-template.model';

@Component({
  selector: 'app-new-measure-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatTabsModule,
    MatChipsModule,
    MatExpansionModule,
    MatTooltipModule,
    LoadingButtonComponent,
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon color="primary">add_circle</mat-icon>
      Create New Measure
    </h2>

    <mat-dialog-content class="dialog-content">
      <mat-tab-group [(selectedIndex)]="selectedTabIndex" (selectedIndexChange)="onTabChange($event)">
        <!-- Tab 1: Start from Template -->
        <mat-tab>
          <ng-template mat-tab-label>
            <mat-icon>library_books</mat-icon>
            <span>From Template</span>
          </ng-template>

          <div class="template-tab-content">
            <!-- Search and Filter -->
            <div class="template-filters">
              <mat-form-field appearance="outline" class="search-field">
                <mat-label>Search templates</mat-label>
                <input matInput [(ngModel)]="templateSearch" (ngModelChange)="filterTemplates()" placeholder="e.g., diabetes, screening">
                <mat-icon matPrefix>search</mat-icon>
              </mat-form-field>

              <mat-form-field appearance="outline" class="category-field">
                <mat-label>Category</mat-label>
                <mat-select [(ngModel)]="selectedCategory" (selectionChange)="filterTemplates()">
                  <mat-option value="">All Categories</mat-option>
                  <mat-option value="PREVENTIVE_CARE">Preventive Care</mat-option>
                  <mat-option value="CHRONIC_DISEASE">Chronic Disease</mat-option>
                  <mat-option value="BEHAVIORAL_HEALTH">Behavioral Health</mat-option>
                  <mat-option value="MEDICATION_MANAGEMENT">Medication Management</mat-option>
                  <mat-option value="GERIATRIC">Geriatric</mat-option>
                </mat-select>
              </mat-form-field>
            </div>

            <!-- Template List -->
            <div class="template-list">
              @if (filteredTemplates.length === 0) {
                <div class="no-templates">
                  <mat-icon>search_off</mat-icon>
                  <p>No templates match your search criteria</p>
                </div>
              }

              @for (template of filteredTemplates; track template.id) {
                <div class="template-card" [class.selected]="selectedTemplate?.id === template.id" (click)="selectTemplate(template)">
                  <div class="template-header">
                    <div class="template-title">
                      <mat-icon [class]="'category-icon ' + template.category.toLowerCase()">
                        {{ getCategoryIcon(template.category) }}
                      </mat-icon>
                      <div>
                        <h4>{{ template.name }}</h4>
                        <span class="template-source">
                          @if (template.hedisAligned) {
                            <mat-chip class="hedis-chip">HEDIS: {{ template.hedisAligned }}</mat-chip>
                          }
                          @if (template.cmsAligned) {
                            <mat-chip class="cms-chip">CMS: {{ template.cmsAligned }}</mat-chip>
                          }
                        </span>
                      </div>
                    </div>
                    <div class="template-meta">
                      <mat-chip [class]="'complexity-' + template.complexity">
                        {{ template.complexity | titlecase }}
                      </mat-chip>
                      @if (template.estimatedPatients) {
                        <span class="patient-estimate" matTooltip="Estimated eligible patients">
                          <mat-icon>people</mat-icon>
                          {{ template.estimatedPatients }}
                        </span>
                      }
                    </div>
                  </div>

                  <p class="template-description">{{ template.description }}</p>

                  <div class="template-tags">
                    @for (tag of template.tags.slice(0, 4); track tag) {
                      <span class="tag">{{ tag }}</span>
                    }
                  </div>

                  <!-- Expanded Details -->
                  @if (selectedTemplate?.id === template.id) {
                    <div class="template-details">
                      <h5>Clinical Rationale</h5>
                      <p>{{ template.clinicalRationale }}</p>

                      <h5>Population Criteria</h5>
                      <ul>
                        <li><strong>Initial Population:</strong> {{ template.populationCriteria.initialPopulation }}</li>
                        <li><strong>Denominator:</strong> {{ template.populationCriteria.denominator }}</li>
                        <li><strong>Numerator:</strong> {{ template.populationCriteria.numerator }}</li>
                        @if (template.populationCriteria.denominatorExclusions) {
                          <li><strong>Exclusions:</strong> {{ template.populationCriteria.denominatorExclusions }}</li>
                        }
                      </ul>

                      <h5>Required Value Sets ({{ template.requiredValueSets.length }})</h5>
                      <div class="value-sets">
                        @for (vs of template.requiredValueSets; track vs.oid) {
                          <span class="value-set" [matTooltip]="vs.oid">
                            {{ vs.name }} ({{ vs.codeSystem }})
                          </span>
                        }
                      </div>
                    </div>
                  }
                </div>
              }
            </div>
          </div>
        </mat-tab>

        <!-- Tab 2: Start from Scratch -->
        <mat-tab>
          <ng-template mat-tab-label>
            <mat-icon>edit</mat-icon>
            <span>From Scratch</span>
          </ng-template>

          <div class="scratch-tab-content">
            <form #measureForm="ngForm">
              <!-- Measure Name -->
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Measure Name</mat-label>
                <input
                  matInput
                  [(ngModel)]="name"
                  name="name"
                  required
                  placeholder="e.g., Custom Diabetes Screening"
                  #nameInput="ngModel">
                <mat-hint>A descriptive name for your quality measure</mat-hint>
                @if (nameInput.invalid && nameInput.touched) {
                  <mat-error>Measure name is required</mat-error>
                }
              </mat-form-field>

              <!-- Description -->
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Description</mat-label>
                <textarea
                  matInput
                  [(ngModel)]="description"
                  name="description"
                  rows="3"
                  placeholder="What this measure evaluates and why it matters"></textarea>
                <mat-hint>Explain the purpose and clinical significance</mat-hint>
              </mat-form-field>

              <!-- Category -->
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Category</mat-label>
                <mat-select [(ngModel)]="category" name="category">
                  <mat-option value="CUSTOM">Custom</mat-option>
                  <mat-option value="HEDIS">HEDIS-Derived</mat-option>
                  <mat-option value="CMS">CMS-Derived</mat-option>
                  <mat-option value="QUALITY">Quality Improvement</mat-option>
                  <mat-option value="COMPLIANCE">Compliance</mat-option>
                </mat-select>
                <mat-hint>Categorize your measure for organization</mat-hint>
              </mat-form-field>

              <!-- Year (Optional) -->
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Year (Optional)</mat-label>
                <input
                  matInput
                  type="number"
                  [(ngModel)]="year"
                  name="year"
                  [min]="2000"
                  [max]="2030"
                  placeholder="2024">
                <mat-hint>Reporting year or specification version</mat-hint>
              </mat-form-field>
            </form>
          </div>
        </mat-tab>
      </mat-tab-group>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>
        <mat-icon>close</mat-icon>
        Cancel
      </button>
      <app-loading-button
        [text]="selectedTabIndex === 0 ? 'Use Template' : 'Create Draft'"
        icon="save"
        color="primary"
        variant="raised"
        [disabled]="!canSave()"
        ariaLabel="Create measure draft"
        (buttonClick)="save()">
      </app-loading-button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-content {
      min-width: 700px;
      max-width: 900px;
      padding: 0;
      max-height: 70vh;
      overflow: hidden;
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

    mat-tab-group {
      height: 100%;
    }

    ::ng-deep .mat-mdc-tab-body-wrapper {
      flex: 1;
      overflow: hidden;
    }

    ::ng-deep .mat-mdc-tab-label {
      mat-icon {
        margin-right: 8px;
      }
    }

    /* Template Tab */
    .template-tab-content {
      padding: 16px 24px;
      height: calc(70vh - 180px);
      display: flex;
      flex-direction: column;
    }

    .template-filters {
      display: flex;
      gap: 16px;
      margin-bottom: 16px;

      .search-field {
        flex: 2;
      }

      .category-field {
        flex: 1;
      }
    }

    .template-list {
      flex: 1;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .no-templates {
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
        margin-bottom: 16px;
      }
    }

    .template-card {
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      padding: 16px;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        border-color: #1976d2;
        background-color: #f8f9fa;
      }

      &.selected {
        border-color: #1976d2;
        border-width: 2px;
        background-color: #e3f2fd;
      }
    }

    .template-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 8px;
    }

    .template-title {
      display: flex;
      align-items: flex-start;
      gap: 12px;

      .category-icon {
        font-size: 24px;
        width: 24px;
        height: 24px;
        margin-top: 2px;

        &.preventive_care { color: #4caf50; }
        &.chronic_disease { color: #ff9800; }
        &.behavioral_health { color: #9c27b0; }
        &.medication_management { color: #2196f3; }
        &.geriatric { color: #795548; }
      }

      h4 {
        margin: 0 0 4px 0;
        font-size: 15px;
        font-weight: 500;
      }

      .template-source {
        display: flex;
        gap: 8px;

        .hedis-chip, .cms-chip {
          font-size: 10px;
          height: 20px;
          padding: 0 8px;
        }

        .hedis-chip {
          background-color: #e8f5e9;
          color: #2e7d32;
        }

        .cms-chip {
          background-color: #e3f2fd;
          color: #1565c0;
        }
      }
    }

    .template-meta {
      display: flex;
      align-items: center;
      gap: 12px;

      .complexity-basic {
        background-color: #e8f5e9;
        color: #2e7d32;
      }

      .complexity-intermediate {
        background-color: #fff3e0;
        color: #e65100;
      }

      .complexity-advanced {
        background-color: #fce4ec;
        color: #c2185b;
      }

      .patient-estimate {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: #666;

        mat-icon {
          font-size: 16px;
          width: 16px;
          height: 16px;
        }
      }
    }

    .template-description {
      margin: 0 0 12px 0;
      font-size: 13px;
      color: #666;
      line-height: 1.4;
    }

    .template-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;

      .tag {
        font-size: 11px;
        padding: 2px 8px;
        background-color: #f5f5f5;
        border-radius: 12px;
        color: #666;
      }
    }

    .template-details {
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #e0e0e0;

      h5 {
        margin: 12px 0 8px 0;
        font-size: 13px;
        font-weight: 600;
        color: #333;

        &:first-child {
          margin-top: 0;
        }
      }

      p {
        margin: 0;
        font-size: 13px;
        color: #666;
        line-height: 1.4;
      }

      ul {
        margin: 0;
        padding-left: 20px;
        font-size: 13px;
        color: #666;

        li {
          margin-bottom: 4px;
        }
      }

      .value-sets {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;

        .value-set {
          font-size: 11px;
          padding: 4px 8px;
          background-color: #fff3e0;
          border-radius: 4px;
          color: #e65100;
          cursor: help;
        }
      }
    }

    /* Scratch Tab */
    .scratch-tab-content {
      padding: 24px;
    }

    .full-width {
      width: 100%;
      margin-bottom: 16px;
    }

    mat-dialog-actions {
      padding: 16px 24px;
      border-top: 1px solid #e0e0e0;
      gap: 8px;
    }

    @media (max-width: 800px) {
      .dialog-content {
        min-width: 100%;
        max-width: 100%;
      }

      .template-filters {
        flex-direction: column;
      }
    }
  `],
})
export class NewMeasureDialogComponent implements OnInit {
  // Tab selection
  selectedTabIndex = 0;

  // Template tab
  templateSearch = '';
  selectedCategory = '';
  selectedTemplate: MeasureTemplate | null = null;
  filteredTemplates: MeasureTemplate[] = [];

  // Scratch tab
  name = '';
  description = '';
  category = 'CUSTOM';
  year?: number;

  constructor(private dialogRef: MatDialogRef<NewMeasureDialogComponent>) {}

  ngOnInit(): void {
    this.filteredTemplates = [...PRIMARY_CARE_TEMPLATES];
  }

  onTabChange(index: number): void {
    this.selectedTabIndex = index;
    if (index === 0) {
      // Reset scratch form when switching to templates
    } else {
      // Clear template selection when switching to scratch
      this.selectedTemplate = null;
    }
  }

  filterTemplates(): void {
    let templates = [...PRIMARY_CARE_TEMPLATES];

    // Filter by category
    if (this.selectedCategory) {
      templates = getTemplatesByCategory(this.selectedCategory as MeasureTemplateCategory);
    }

    // Filter by search
    if (this.templateSearch.trim()) {
      templates = searchTemplates(this.templateSearch).filter(t =>
        !this.selectedCategory || t.category === this.selectedCategory
      );
    }

    this.filteredTemplates = templates;
  }

  selectTemplate(template: MeasureTemplate): void {
    if (this.selectedTemplate?.id === template.id) {
      this.selectedTemplate = null;
    } else {
      this.selectedTemplate = template;
    }
  }

  getCategoryIcon(category: MeasureTemplateCategory): string {
    const icons: Record<string, string> = {
      PREVENTIVE_CARE: 'health_and_safety',
      CHRONIC_DISEASE: 'medical_services',
      BEHAVIORAL_HEALTH: 'psychology',
      MEDICATION_MANAGEMENT: 'medication',
      GERIATRIC: 'elderly',
      WOMENS_HEALTH: 'female',
      PEDIATRIC: 'child_care',
      CARE_COORDINATION: 'group_work',
      PATIENT_SAFETY: 'verified_user',
    };
    return icons[category] || 'assessment';
  }

  canSave(): boolean {
    if (this.selectedTabIndex === 0) {
      return this.selectedTemplate !== null;
    } else {
      return this.name.trim().length > 0;
    }
  }

  save(): void {
    if (!this.canSave()) return;

    let draft: CreateCustomMeasureRequest & { cqlTemplate?: string };

    if (this.selectedTabIndex === 0 && this.selectedTemplate) {
      // From template
      draft = {
        name: this.selectedTemplate.name,
        description: this.selectedTemplate.description,
        category: this.mapTemplateCategory(this.selectedTemplate.category),
        createdBy: 'clinical-portal',
        cqlTemplate: this.selectedTemplate.cqlTemplate,
      };
    } else {
      // From scratch
      const trimmed = this.name.trim();
      if (!trimmed) return;

      draft = {
        name: trimmed,
        description: this.description.trim(),
        category: this.category,
        year: this.year,
        createdBy: 'clinical-portal',
      };
    }

    this.dialogRef.close(draft);
  }

  private mapTemplateCategory(category: MeasureTemplateCategory): string {
    const mapping: Record<MeasureTemplateCategory, string> = {
      PREVENTIVE_CARE: 'HEDIS',
      CHRONIC_DISEASE: 'HEDIS',
      BEHAVIORAL_HEALTH: 'QUALITY',
      MEDICATION_MANAGEMENT: 'HEDIS',
      WOMENS_HEALTH: 'HEDIS',
      PEDIATRIC: 'HEDIS',
      GERIATRIC: 'HEDIS',
      CARE_COORDINATION: 'QUALITY',
      PATIENT_SAFETY: 'QUALITY',
    };
    return mapping[category] || 'CUSTOM';
  }
}
