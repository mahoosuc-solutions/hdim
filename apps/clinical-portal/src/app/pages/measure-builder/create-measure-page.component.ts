import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import {
  CreateCustomMeasureRequest,
  CustomMeasureService,
} from '../../services/custom-measure.service';
import { ToastService } from '../../services/toast.service';
import {
  MeasureTemplate,
  MeasureTemplateCategory,
  PRIMARY_CARE_TEMPLATES,
  getTemplatesByCategory,
  searchTemplates,
} from '../../models/measure-template.model';
import {
  extractApiFieldErrors,
  getApiErrorMessage,
} from './utils/api-error-parser';

type MeasureMode = 'template' | 'scratch';

@Component({
  selector: 'app-create-measure-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTabsModule,
    LoadingButtonComponent,
  ],
  templateUrl: './create-measure-page.component.html',
  styleUrls: ['./create-measure-page.component.scss'],
})
export class CreateMeasurePageComponent implements OnInit {
  mode: MeasureMode = 'template';
  selectedTabIndex = 0;
  loading = false;

  // Template fields
  templates: MeasureTemplate[] = PRIMARY_CARE_TEMPLATES;
  filteredTemplates: MeasureTemplate[] = PRIMARY_CARE_TEMPLATES;
  selectedTemplate?: MeasureTemplate;
  templateSearch = '';
  selectedCategory: MeasureTemplateCategory | '' = '';

  // Core measure fields
  name = '';
  description = '';
  category = 'CUSTOM';
  year = new Date().getFullYear();

  // Extended configuration fields
  owner = '';
  clinicalFocus = '';
  reportingCadence: 'MONTHLY' | 'QUARTERLY' | 'ANNUAL' = 'MONTHLY';
  targetThreshold = '';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' = 'MEDIUM';
  implementationNotes = '';
  tags = '';
  apiFieldErrors: Record<string, string> = {};

  constructor(
    private router: Router,
    private customMeasureService: CustomMeasureService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.filteredTemplates = [...this.templates];
  }

  onTabChange(index: number): void {
    this.selectedTabIndex = index;
    this.mode = index === 0 ? 'template' : 'scratch';
  }

  selectTemplate(template: MeasureTemplate): void {
    this.selectedTemplate = template;
    this.name = template.name;
    this.description = template.description;
    this.category = template.category;
  }

  filterTemplates(): void {
    const byCategory = this.selectedCategory
      ? getTemplatesByCategory(this.selectedCategory)
      : PRIMARY_CARE_TEMPLATES;

    this.filteredTemplates = this.templateSearch.trim()
      ? searchTemplates(this.templateSearch).filter((candidate) =>
          byCategory.some((allowed) => allowed.id === candidate.id)
        )
      : [...byCategory];
  }

  canSave(): boolean {
    if (!this.isYearValid()) {
      return false;
    }

    if (this.mode === 'template') {
      return !!this.selectedTemplate;
    }
    return this.name.trim().length > 0;
  }

  cancel(): void {
    void this.router.navigate(['/measure-builder']);
  }

  save(): void {
    if (!this.canSave()) {
      return;
    }

    this.apiFieldErrors = {};
    const request = this.buildRequest();
    const cqlTemplate =
      this.mode === 'template' ? this.selectedTemplate?.cqlTemplate : undefined;

    this.loading = true;
    this.customMeasureService.createDraft(request).subscribe({
      next: (saved) => {
        if (!cqlTemplate) {
          this.toast.success('Measure draft created');
          this.loading = false;
          void this.router.navigate(['/measure-builder']);
          return;
        }

        this.customMeasureService.updateCql(saved.id, cqlTemplate).subscribe({
          next: () => {
            this.toast.success('Measure draft created from template');
            this.loading = false;
            void this.router.navigate(['/measure-builder']);
          },
          error: () => {
            this.toast.warning(
              'Measure created, but template CQL could not be saved'
            );
            this.loading = false;
            void this.router.navigate(['/measure-builder']);
          },
        });
      },
      error: (err) => {
        this.loading = false;
        this.apiFieldErrors = extractApiFieldErrors(err);
        this.toast.error(getApiErrorMessage(err, 'Failed to create measure'));
      },
    });
  }

  isYearValid(): boolean {
    return !this.year || (this.year >= 2000 && this.year <= 2100);
  }

  getCategoryIcon(category: MeasureTemplateCategory): string {
    const categoryIcons: Record<MeasureTemplateCategory, string> = {
      PREVENTIVE_CARE: 'health_and_safety',
      CHRONIC_DISEASE: 'monitor_heart',
      BEHAVIORAL_HEALTH: 'psychology',
      MEDICATION_MANAGEMENT: 'medication',
      WOMENS_HEALTH: 'female',
      PEDIATRIC: 'child_care',
      GERIATRIC: 'elderly',
      CARE_COORDINATION: 'groups',
      PATIENT_SAFETY: 'verified_user',
    };

    return categoryIcons[category] || 'folder';
  }

  private buildRequest(): CreateCustomMeasureRequest {
    return {
      name:
        this.mode === 'template'
          ? this.selectedTemplate?.name || this.name
          : this.name.trim(),
      description:
        this.mode === 'template'
          ? this.selectedTemplate?.description || this.description
          : this.description,
      category:
        this.mode === 'template'
          ? this.selectedTemplate?.category || this.category
          : this.category,
      year: this.year || undefined,
      owner: this.owner || undefined,
      clinicalFocus: this.clinicalFocus || undefined,
      reportingCadence: this.reportingCadence || undefined,
      targetThreshold: this.targetThreshold || undefined,
      priority: this.priority || undefined,
      implementationNotes: this.implementationNotes || undefined,
      tags: this.tags || undefined,
      createdBy: 'measure-builder-user',
    };
  }
}
