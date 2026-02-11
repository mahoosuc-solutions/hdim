import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subject, takeUntil } from 'rxjs';

import { AgentBuilderService } from '../services/agent-builder.service';
import { PromptTemplate, TemplateCategory, PromptVariable } from '../models/agent.model';
import { PromptEditorComponent } from '../components/prompt-editor/prompt-editor.component';
import { ToastService } from '../../../services/toast.service';
import { LoggerService } from '../../../services/logger.service';

export interface CreateTemplateDialogData {
  isEdit?: boolean;
  template?: PromptTemplate;
}

@Component({
  selector: 'app-create-template-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    PromptEditorComponent,
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>{{ data.isEdit ? 'edit' : 'add' }}</mat-icon>
      {{ data.isEdit ? 'Edit' : 'Create' }} Template
    </h2>

    <mat-dialog-content>
      <form [formGroup]="templateForm" class="template-form">
        <!-- Name -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Template Name</mat-label>
          <input
            matInput
            formControlName="name"
            placeholder="e.g., Care Coordinator System Prompt"
            required
            aria-label="Template name" />
          @if (templateForm.get('name')?.hasError('required') && templateForm.get('name')?.touched) {
            <mat-error>Name is required</mat-error>
          }
        </mat-form-field>

        <!-- Description -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Description</mat-label>
          <textarea
            matInput
            formControlName="description"
            rows="2"
            placeholder="Brief description of what this template does..."
            aria-label="Template description">
          </textarea>
        </mat-form-field>

        <!-- Category -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Category</mat-label>
          <mat-select formControlName="category" required aria-label="Select template category">
            @for (cat of categories; track cat.value) {
              <mat-option [value]="cat.value">
                {{ cat.label }}
              </mat-option>
            }
          </mat-select>
          @if (templateForm.get('category')?.hasError('required') && templateForm.get('category')?.touched) {
            <mat-error>Category is required</mat-error>
          }
        </mat-form-field>

        <!-- Content -->
        <app-prompt-editor
          formControlName="content"
          label="Template Content"
          icon="description"
          [hint]="'Use {{variable_name}} for placeholders. Variables will be auto-detected.'"
          [required]="true"
          (variablesDetected)="onVariablesDetected($event)"
          height="300px">
        </app-prompt-editor>

        @if (templateForm.get('content')?.hasError('required') && templateForm.get('content')?.touched) {
          <div class="form-error">Template content is required</div>
        }

        <!-- Detected Variables -->
        @if (detectedVariables.length > 0) {
          <div class="variables-section">
            <h3>
              <mat-icon>data_object</mat-icon>
              Detected Variables
            </h3>
            <p class="variables-hint">
              These variables will be automatically available when using this template.
            </p>
            <div class="variables-list">
              @for (variable of detectedVariables; track variable.name) {
                <div class="variable-item">
                  <code>{{ '{{' + variable.name + '}}' }}</code>
                </div>
              }
            </div>
          </div>
        }
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()" [disabled]="saving">
        Cancel
      </button>
      <button
        mat-flat-button
        color="primary"
        (click)="onSave()"
        [disabled]="!templateForm.valid || saving">
        @if (saving) {
          <mat-spinner diameter="20"></mat-spinner>
        } @else {
          <mat-icon>{{ data.isEdit ? 'save' : 'add' }}</mat-icon>
        }
        @if (!saving) {
          {{ data.isEdit ? 'Save Changes' : 'Create Template' }}
        }
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2[mat-dialog-title] {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    mat-dialog-content {
      min-width: 600px;
      max-width: 800px;
    }

    .template-form {
      display: flex;
      flex-direction: column;
      gap: 16px;
      padding: 8px 0;
    }

    .full-width {
      width: 100%;
    }

    .form-error {
      color: var(--mat-sys-error);
      font-size: 0.75rem;
      margin-top: -12px;
      margin-bottom: 8px;
    }

    .variables-section {
      margin-top: 8px;
      padding: 16px;
      background: var(--mat-sys-surface-variant);
      border-radius: 8px;

      h3 {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0 0 8px;
        font-size: 1rem;
        font-weight: 500;

        mat-icon {
          font-size: 20px;
          width: 20px;
          height: 20px;
        }
      }

      .variables-hint {
        margin: 0 0 12px;
        font-size: 0.875rem;
        color: var(--mat-sys-on-surface-variant);
      }

      .variables-list {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
      }

      .variable-item {
        code {
          font-family: monospace;
          background: var(--mat-sys-surface);
          padding: 4px 8px;
          border-radius: 4px;
          font-size: 0.875rem;
        }
      }
    }

    mat-dialog-actions {
      padding: 16px 24px;

      button {
        mat-icon {
          margin-right: 8px;
        }
      }
    }
  `],
})
export class CreateTemplateDialogComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  templateForm!: FormGroup;
  detectedVariables: PromptVariable[] = [];
  saving = false;

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
    private fb: FormBuilder,
    private agentService: AgentBuilderService,
    private toast: ToastService,
    private logger: LoggerService,
    private dialogRef: MatDialogRef<CreateTemplateDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CreateTemplateDialogData
  ) {}

  ngOnInit(): void {
    this.initForm();

    if (this.data.isEdit && this.data.template) {
      this.loadTemplate();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initForm(): void {
    this.templateForm = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      category: ['CUSTOM', Validators.required],
      content: ['', Validators.required],
    });
  }

  private loadTemplate(): void {
    const template = this.data.template!;
    this.templateForm.patchValue({
      name: template.name,
      description: template.description || '',
      category: template.category,
      content: template.content,
    });

    // Extract variables from template
    if (template.variables) {
      this.detectedVariables = template.variables.map((v) => ({
        name: v.name,
        startIndex: 0,
        endIndex: 0,
        required: v.required || false,
      }));
    }
  }

  onVariablesDetected(variables: PromptVariable[]): void {
    this.detectedVariables = variables;
    this.logger.info('Variables detected', { count: variables.length });
  }

  onSave(): void {
    if (!this.templateForm.valid) {
      this.templateForm.markAllAsTouched();
      return;
    }

    this.saving = true;

    const templateData = {
      name: this.templateForm.value.name,
      description: this.templateForm.value.description,
      category: this.templateForm.value.category as TemplateCategory,
      content: this.templateForm.value.content,
      variables: this.detectedVariables.map((v) => ({
        name: v.name,
        description: '',
        required: true,
      })),
    };

    this.agentService
      .createTemplate(templateData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (template) => {
          this.logger.info('Template created', { templateId: template.id });
          this.toast.success('Template created successfully');
          this.saving = false;
          this.dialogRef.close(template);
        },
        error: (err) => {
          this.logger.error('Failed to create template', err);
          this.toast.error('Failed to create template');
          this.saving = false;
        },
      });
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
