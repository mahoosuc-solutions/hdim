import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatStepperModule } from '@angular/material/stepper';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';

import { MeasureBuilderState } from '../../models/measure-builder.model';

/**
 * Left sidebar component showing measure metadata and building steps
 */
@Component({
  selector: 'app-measure-preview-panel',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCardModule,
    MatStepperModule,
    MatDividerModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatTooltipModule,
  ],
  templateUrl: './measure-preview-panel.component.html',
  styleUrls: ['./measure-preview-panel.component.scss'],
})
export class MeasurePreviewPanelComponent implements OnInit {
  @Input() state: MeasureBuilderState | null = null;
  @Output() stateChanged = new EventEmitter<Partial<MeasureBuilderState>>();

  metadataForm: FormGroup;
  currentStep = 0;

  // Category options
  categoryOptions = [
    { label: 'Custom', value: 'CUSTOM' },
    { label: 'HEDIS', value: 'HEDIS' },
    { label: 'CMS', value: 'CMS' },
    { label: 'Quality', value: 'QUALITY' },
    { label: 'Compliance', value: 'COMPLIANCE' },
  ];

  // Builder steps
  steps = [
    {
      number: 1,
      title: 'Measure Metadata',
      description: 'Define measure name, description, and category',
      icon: 'info',
    },
    {
      number: 2,
      title: 'Algorithm Structure',
      description: 'Define population criteria (Initial, Denominator, Numerator)',
      icon: 'schema',
    },
    {
      number: 3,
      title: 'Configure Parameters',
      description: 'Use sliders to set clinical thresholds and demographics',
      icon: 'tune',
    },
    {
      number: 4,
      title: 'Exclusions & Exceptions',
      description: 'Add exclusions and exceptions to refine populations',
      icon: 'filter_list',
    },
    {
      number: 5,
      title: 'Test & Publish',
      description: 'Test against sample patients and publish',
      icon: 'done',
    },
  ];

  constructor(private fb: FormBuilder) {
    this.metadataForm = this.fb.group({
      name: [''],
      description: [''],
      category: ['CUSTOM'],
      year: [new Date().getFullYear()],
    });
  }

  ngOnInit(): void {
    if (this.state) {
      this.metadataForm.patchValue({
        name: this.state.name,
        description: this.state.description,
        category: this.state.category,
        year: this.state.name,
      });
    }

    // Emit form changes
    this.metadataForm.valueChanges.subscribe((values) => {
      this.stateChanged.emit({
        name: values.name,
        description: values.description,
        category: values.category,
      });
    });
  }

  /**
   * Move to next step
   */
  nextStep(): void {
    if (this.currentStep < this.steps.length - 1) {
      this.currentStep++;
    }
  }

  /**
   * Move to previous step
   */
  previousStep(): void {
    if (this.currentStep > 0) {
      this.currentStep--;
    }
  }

  /**
   * Jump to specific step
   */
  goToStep(stepNumber: number): void {
    this.currentStep = stepNumber - 1;
  }

  /**
   * Get step status
   */
  getStepStatus(stepNumber: number): 'completed' | 'current' | 'pending' {
    if (stepNumber - 1 < this.currentStep) {
      return 'completed';
    } else if (stepNumber - 1 === this.currentStep) {
      return 'current';
    } else {
      return 'pending';
    }
  }

  /**
   * Get step icon color
   */
  getStepIconColor(stepNumber: number): string {
    const status = this.getStepStatus(stepNumber);
    switch (status) {
      case 'completed':
        return 'primary';
      case 'current':
        return 'accent';
      default:
        return '';
    }
  }

  /**
   * Check if measure is valid for publishing
   */
  canPublish(): boolean {
    return (
      !!this.state?.name &&
      !!this.state?.algorithm?.denominator?.condition &&
      !!this.state?.algorithm?.numerator?.condition
    );
  }

  /**
   * Get completion percentage
   */
  getCompletionPercentage(): number {
    let completed = 0;

    if (this.state?.name) completed++;
    if (this.state?.description) completed++;
    if (this.state?.algorithm?.initialPopulation?.condition) completed++;
    if (this.state?.algorithm?.denominator?.condition) completed++;
    if (this.state?.algorithm?.numerator?.condition) completed++;

    return Math.round((completed / 5) * 100);
  }

  /**
   * Format category label
   */
  getCategoryLabel(category: string): string {
    const categoryMap: Record<string, string> = {
      CUSTOM: 'Custom Measure',
      HEDIS: 'HEDIS Aligned',
      CMS: 'CMS Measure',
      QUALITY: 'Quality Measure',
      COMPLIANCE: 'Compliance Measure',
    };
    return categoryMap[category] || category;
  }
}
