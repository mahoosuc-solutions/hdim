import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
  FormArray,
} from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatStepperModule } from '@angular/material/stepper';
import { MatSliderModule } from '@angular/material/slider';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatDividerModule } from '@angular/material/divider';
import { MatCardModule } from '@angular/material/card';
import { Subject, takeUntil } from 'rxjs';

import { AgentBuilderService } from '../services/agent-builder.service';
import {
  AgentConfiguration,
  ToolInfo,
  ProviderInfo,
  CreateAgentRequest,
  UpdateAgentRequest,
  ModelProvider,
  ToolConfig,
  GuardrailConfig,
} from '../models/agent.model';
import { LoadingButtonComponent } from '../../../shared/components/loading-button/loading-button.component';
import { PromptEditorComponent } from '../components/prompt-editor/prompt-editor.component';

export interface CreateAgentDialogData {
  agent?: AgentConfiguration;
  tools: ToolInfo[];
  providers: ProviderInfo[];
  isEdit?: boolean;
}

@Component({
  selector: 'app-create-agent-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatStepperModule,
    MatSliderModule,
    MatCheckboxModule,
    MatChipsModule,
    MatTooltipModule,
    MatExpansionModule,
    MatDividerModule,
    MatCardModule,
    LoadingButtonComponent,
    PromptEditorComponent,
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>{{ data.isEdit ? 'edit' : 'add' }}</mat-icon>
      {{ data.isEdit ? 'Edit Agent' : 'Create Agent' }}
    </h2>

    <mat-dialog-content>
      <mat-stepper #stepper linear="false">
        <!-- Step 1: Basic Info -->
        <mat-step [stepControl]="basicForm" label="Basic Info">
          <form [formGroup]="basicForm" class="step-content">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Agent Name</mat-label>
              <input matInput formControlName="name" placeholder="e.g., Clinical Decision Assistant" />
              <mat-hint>A unique name for this agent</mat-hint>
              @if (basicForm.get('name')?.hasError('required')) {
                <mat-error>Name is required</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Description</mat-label>
              <textarea
                matInput
                formControlName="description"
                rows="3"
                placeholder="Describe what this agent does...">
              </textarea>
            </mat-form-field>

            <div class="form-row">
              <mat-form-field appearance="outline">
                <mat-label>Persona Name</mat-label>
                <input matInput formControlName="personaName" placeholder="e.g., Dr. Health" />
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Persona Role</mat-label>
                <input matInput formControlName="personaRole" placeholder="e.g., Clinical Advisor" />
              </mat-form-field>
            </div>

            <div class="step-actions">
              <button mat-button matStepperNext type="button">
                Next
                <mat-icon>arrow_forward</mat-icon>
              </button>
            </div>
          </form>
        </mat-step>

        <!-- Step 2: Model Configuration -->
        <mat-step [stepControl]="modelForm" label="Model">
          <form [formGroup]="modelForm" class="step-content">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>LLM Provider</mat-label>
              <mat-select formControlName="modelProvider" (selectionChange)="onProviderChange($event.value)">
                @for (provider of data.providers; track provider.name) {
                  <mat-option [value]="provider.name" [disabled]="!provider.available">
                    <div class="provider-option">
                      <span>{{ provider.displayName }}</span>
                      @if (provider.hipaaCompliant) {
                        <mat-icon matTooltip="HIPAA Compliant" class="hipaa-badge">verified_user</mat-icon>
                      }
                      @if (!provider.available) {
                        <span class="unavailable">(Unavailable)</span>
                      }
                    </div>
                  </mat-option>
                }
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Model</mat-label>
              <mat-select formControlName="modelId">
                @for (model of availableModels; track model.id) {
                  <mat-option [value]="model.id">
                    {{ model.name }}
                    <span class="model-context">({{ model.contextWindow | number }} tokens)</span>
                  </mat-option>
                }
              </mat-select>
            </mat-form-field>

            <div class="slider-field">
              <label id="temperature-label">Temperature: {{ modelForm.get('temperature')?.value }}</label>
              <mat-slider min="0" max="1" step="0.1" discrete>
                <input matSliderThumb formControlName="temperature" aria-label="Temperature setting" aria-labelledby="temperature-label" />
              </mat-slider>
              <div class="slider-hints">
                <span>Precise</span>
                <span>Creative</span>
              </div>
            </div>

            <div class="slider-field">
              <label id="max-tokens-label">Max Tokens: {{ modelForm.get('maxTokens')?.value }}</label>
              <mat-slider min="256" max="4096" step="256" discrete>
                <input matSliderThumb formControlName="maxTokens" aria-label="Maximum tokens setting" aria-labelledby="max-tokens-label" />
              </mat-slider>
            </div>

            <div class="step-actions">
              <button mat-button matStepperPrevious type="button">
                <mat-icon>arrow_back</mat-icon>
                Back
              </button>
              <button mat-button matStepperNext type="button">
                Next
                <mat-icon>arrow_forward</mat-icon>
              </button>
            </div>
          </form>
        </mat-step>

        <!-- Step 3: System Prompt -->
        <mat-step [stepControl]="promptForm" label="Prompts">
          <form [formGroup]="promptForm" class="step-content">
            <app-prompt-editor
              formControlName="systemPrompt"
              label="System Prompt"
              icon="psychology"
              [hint]="'Define the agent\\'s behavior, capabilities, and constraints. Use {{variable_name}} for dynamic content.'"
              [required]="true"
              height="250px">
            </app-prompt-editor>

            @if (promptForm.get('systemPrompt')?.hasError('required') && promptForm.get('systemPrompt')?.touched) {
              <div class="form-error">System prompt is required</div>
            }

            <div class="welcome-message-section">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Welcome Message</mat-label>
                <textarea
                  matInput
                  formControlName="welcomeMessage"
                  rows="3"
                  placeholder="Hello! How can I help you today?">
                </textarea>
                <mat-hint>Initial message shown to users when they start a conversation</mat-hint>
              </mat-form-field>
            </div>

            <div class="step-actions">
              <button mat-button matStepperPrevious type="button">
                <mat-icon>arrow_back</mat-icon>
                Back
              </button>
              <button mat-button matStepperNext type="button">
                Next
                <mat-icon>arrow_forward</mat-icon>
              </button>
            </div>
          </form>
        </mat-step>

        <!-- Step 4: Tools -->
        <mat-step label="Tools">
          <div class="step-content">
            <p class="step-description">
              Select which tools this agent can use to accomplish tasks.
            </p>

            <div class="tools-grid">
              @for (tool of data.tools; track tool.name) {
                <mat-card class="tool-card" [class.selected]="isToolEnabled(tool.name)">
                  <mat-card-header>
                    <mat-checkbox
                      [checked]="isToolEnabled(tool.name)"
                      (change)="toggleTool(tool.name, $event.checked)">
                    </mat-checkbox>
                    <mat-card-title>{{ tool.displayName }}</mat-card-title>
                  </mat-card-header>
                  <mat-card-content>
                    <p>{{ tool.description }}</p>
                    <div class="tool-meta">
                      <mat-chip size="small">{{ tool.category }}</mat-chip>
                      @if (tool.requiresApproval) {
                        <mat-icon matTooltip="Requires approval">gpp_maybe</mat-icon>
                      }
                      @if (tool.riskLevel === 'HIGH' || tool.riskLevel === 'CRITICAL') {
                        <mat-icon matTooltip="High risk" color="warn">warning</mat-icon>
                      }
                    </div>
                  </mat-card-content>
                </mat-card>
              }
            </div>

            <div class="step-actions">
              <button mat-button matStepperPrevious type="button">
                <mat-icon>arrow_back</mat-icon>
                Back
              </button>
              <button mat-button matStepperNext type="button">
                Next
                <mat-icon>arrow_forward</mat-icon>
              </button>
            </div>
          </div>
        </mat-step>

        <!-- Step 5: Guardrails -->
        <mat-step [stepControl]="guardrailForm" label="Guardrails">
          <form [formGroup]="guardrailForm" class="step-content">
            <p class="step-description">
              Configure safety guardrails to ensure compliant AI behavior.
            </p>

            <div class="guardrail-toggles">
              <mat-checkbox formControlName="phiFiltering">
                <div class="checkbox-content">
                  <span class="checkbox-label">PHI Filtering</span>
                  <span class="checkbox-hint">Automatically detect and redact protected health information</span>
                </div>
              </mat-checkbox>

              <mat-checkbox formControlName="clinicalDisclaimerRequired">
                <div class="checkbox-content">
                  <span class="checkbox-label">Clinical Disclaimer</span>
                  <span class="checkbox-hint">Append clinical safety disclaimer to responses</span>
                </div>
              </mat-checkbox>

              <mat-checkbox formControlName="requireHumanReview">
                <div class="checkbox-content">
                  <span class="checkbox-label">Human Review Required</span>
                  <span class="checkbox-hint">Flag high-risk responses for human review</span>
                </div>
              </mat-checkbox>
            </div>

            <mat-divider></mat-divider>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Risk Threshold</mat-label>
              <mat-select formControlName="riskThreshold">
                <mat-option value="LOW">Low - Block only critical content</mat-option>
                <mat-option value="MEDIUM">Medium - Block high and critical</mat-option>
                <mat-option value="HIGH">High - Block medium and above</mat-option>
                <mat-option value="CRITICAL">Critical - Block everything suspicious</mat-option>
              </mat-select>
            </mat-form-field>

            <div class="slider-field">
              <label id="max-output-tokens-label">Max Output Tokens: {{ guardrailForm.get('maxOutputTokens')?.value }}</label>
              <mat-slider min="256" max="4096" step="256" discrete>
                <input matSliderThumb formControlName="maxOutputTokens" aria-label="Maximum output tokens setting" aria-labelledby="max-output-tokens-label" />
              </mat-slider>
            </div>

            <div class="step-actions">
              <button mat-button matStepperPrevious type="button">
                <mat-icon>arrow_back</mat-icon>
                Back
              </button>
            </div>
          </form>
        </mat-step>
      </mat-stepper>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <app-loading-button
        [loading]="saving"
        color="primary"
        (clicked)="onSave()">
        {{ data.isEdit ? 'Save Changes' : 'Create Agent' }}
      </app-loading-button>
    </mat-dialog-actions>
  `,
  styles: [`
    :host {
      display: block;
    }

    h2[mat-dialog-title] {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    mat-dialog-content {
      min-width: 600px;
      max-height: 70vh;
    }

    .step-content {
      padding: 16px 0;
    }

    .step-description {
      color: var(--mat-sys-on-surface-variant);
      margin-bottom: 16px;
    }

    .full-width {
      width: 100%;
    }

    .form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    .mono-font {
      font-family: monospace;
    }

    .form-error {
      color: var(--mat-sys-error);
      font-size: 0.75rem;
      margin-top: 4px;
      padding-left: 12px;
    }

    .welcome-message-section {
      margin-top: 24px;
    }

    .slider-field {
      margin: 16px 0;

      label {
        display: block;
        margin-bottom: 8px;
        font-weight: 500;
      }

      mat-slider {
        width: 100%;
      }

      .slider-hints {
        display: flex;
        justify-content: space-between;
        font-size: 0.75rem;
        color: var(--mat-sys-on-surface-variant);
      }
    }

    .step-actions {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
      margin-top: 24px;
    }

    .provider-option {
      display: flex;
      align-items: center;
      gap: 8px;

      .hipaa-badge {
        font-size: 16px;
        width: 16px;
        height: 16px;
        color: var(--mat-sys-primary);
      }

      .unavailable {
        color: var(--mat-sys-outline);
        font-size: 0.875rem;
      }
    }

    .model-context {
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.875rem;
      margin-left: 8px;
    }

    .tools-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
      gap: 16px;
    }

    .tool-card {
      cursor: pointer;
      transition: border-color 0.2s;
      border: 2px solid transparent;

      &.selected {
        border-color: var(--mat-sys-primary);
      }

      mat-card-header {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      mat-card-content p {
        font-size: 0.875rem;
        color: var(--mat-sys-on-surface-variant);
        margin-bottom: 8px;
      }

      .tool-meta {
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }

    .guardrail-toggles {
      display: flex;
      flex-direction: column;
      gap: 16px;
      margin-bottom: 24px;

      mat-checkbox {
        .checkbox-content {
          display: flex;
          flex-direction: column;
        }

        .checkbox-label {
          font-weight: 500;
        }

        .checkbox-hint {
          font-size: 0.8125rem;
          color: var(--mat-sys-on-surface-variant);
        }
      }
    }

    mat-divider {
      margin: 24px 0;
    }
  `],
})
export class CreateAgentDialogComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  basicForm!: FormGroup;
  modelForm!: FormGroup;
  promptForm!: FormGroup;
  guardrailForm!: FormGroup;

  saving = false;
  availableModels: { id: string; name: string; contextWindow: number }[] = [];
  enabledTools: Set<string> = new Set();

  constructor(
    private fb: FormBuilder,
    private agentService: AgentBuilderService,
    private dialogRef: MatDialogRef<CreateAgentDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CreateAgentDialogData
  ) {}

  ngOnInit(): void {
    this.initForms();

    if (this.data.isEdit && this.data.agent) {
      this.populateFormWithAgent(this.data.agent);
    }

    // Set initial models based on default provider
    const defaultProvider = this.data.providers.find((p) => p.available);
    if (defaultProvider) {
      this.availableModels = defaultProvider.models;
      if (!this.data.isEdit) {
        this.modelForm.patchValue({
          modelProvider: defaultProvider.name,
          modelId: defaultProvider.models[0]?.id,
        });
      }
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initForms(): void {
    this.basicForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: [''],
      personaName: [''],
      personaRole: [''],
    });

    this.modelForm = this.fb.group({
      modelProvider: ['claude', Validators.required],
      modelId: [''],
      temperature: [0.7],
      maxTokens: [2048],
    });

    this.promptForm = this.fb.group({
      systemPrompt: ['', Validators.required],
      welcomeMessage: [''],
    });

    this.guardrailForm = this.fb.group({
      phiFiltering: [true],
      clinicalDisclaimerRequired: [true],
      requireHumanReview: [false],
      riskThreshold: ['MEDIUM'],
      maxOutputTokens: [2048],
    });
  }

  private populateFormWithAgent(agent: AgentConfiguration): void {
    this.basicForm.patchValue({
      name: agent.name,
      description: agent.description,
      personaName: agent.personaName,
      personaRole: agent.personaRole,
    });

    this.modelForm.patchValue({
      modelProvider: agent.modelProvider,
      modelId: agent.modelId,
      temperature: agent.temperature ?? 0.7,
      maxTokens: agent.maxTokens ?? 2048,
    });

    this.promptForm.patchValue({
      systemPrompt: agent.systemPrompt,
      welcomeMessage: agent.welcomeMessage,
    });

    if (agent.guardrailConfiguration) {
      this.guardrailForm.patchValue(agent.guardrailConfiguration);
    }

    // Populate enabled tools
    if (agent.toolConfiguration) {
      agent.toolConfiguration
        .filter((t) => t.enabled)
        .forEach((t) => this.enabledTools.add(t.toolName));
    }

    // Update available models
    this.onProviderChange(agent.modelProvider);
  }

  onProviderChange(providerName: string): void {
    const provider = this.data.providers.find((p) => p.name === providerName);
    if (provider) {
      this.availableModels = provider.models;
      // Select first model if current selection not available
      const currentModel = this.modelForm.get('modelId')?.value;
      if (!provider.models.find((m) => m.id === currentModel)) {
        this.modelForm.patchValue({ modelId: provider.models[0]?.id });
      }
    }
  }

  isToolEnabled(toolName: string): boolean {
    return this.enabledTools.has(toolName);
  }

  toggleTool(toolName: string, enabled: boolean): void {
    if (enabled) {
      this.enabledTools.add(toolName);
    } else {
      this.enabledTools.delete(toolName);
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    if (!this.isFormValid()) {
      return;
    }

    this.saving = true;

    const toolConfiguration: ToolConfig[] = this.data.tools.map((tool) => ({
      toolName: tool.name,
      enabled: this.enabledTools.has(tool.name),
    }));

    const guardrailConfiguration: GuardrailConfig = {
      phiFiltering: this.guardrailForm.value.phiFiltering,
      clinicalDisclaimerRequired: this.guardrailForm.value.clinicalDisclaimerRequired,
      requireHumanReview: this.guardrailForm.value.requireHumanReview,
      riskThreshold: this.guardrailForm.value.riskThreshold,
      maxOutputTokens: this.guardrailForm.value.maxOutputTokens,
      blockedPatterns: [],
    };

    if (this.data.isEdit && this.data.agent) {
      const updateRequest: UpdateAgentRequest = {
        ...this.basicForm.value,
        ...this.modelForm.value,
        ...this.promptForm.value,
        toolConfiguration,
        guardrailConfiguration,
      };

      this.agentService
        .updateAgent(this.data.agent.id, updateRequest)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.saving = false;
            this.dialogRef.close(true);
          },
          error: () => {
            this.saving = false;
          },
        });
    } else {
      const createRequest: CreateAgentRequest = {
        ...this.basicForm.value,
        ...this.modelForm.value,
        ...this.promptForm.value,
        toolConfiguration,
        guardrailConfiguration,
      };

      this.agentService
        .createAgent(createRequest)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.saving = false;
            this.dialogRef.close(true);
          },
          error: () => {
            this.saving = false;
          },
        });
    }
  }

  private isFormValid(): boolean {
    return (
      this.basicForm.valid &&
      this.modelForm.valid &&
      this.promptForm.valid &&
      this.guardrailForm.valid
    );
  }
}
