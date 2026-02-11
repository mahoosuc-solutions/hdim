import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTabsModule } from '@angular/material/tabs';
import { MatBadgeModule } from '@angular/material/badge';
import { FormsModule } from '@angular/forms';
import { Subject, of } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import {
  CqlGenerationService,
  CqlGenerationResponse,
  CqlValidationResult,
} from '../../services/cql-generation.service';
import {
  LivePreviewService,
  LivePreviewResponse,
  PatientEvaluationResult,
  PreviewPatient,
  MatchedCriterion,
} from '../../services/live-preview.service';
import { PatientService } from '../../services/patient.service';
import { Patient } from '../../models/patient.model';

/**
 * Data passed to the CQL Editor Dialog
 */
export interface CqlEditorDialogData {
  measureId: string;
  measureName: string;
  cqlText: string;
  readOnly?: boolean;
  /** Optional: Patient context passed from external navigation (e.g., patient chart) */
  contextPatient?: Patient;
}

@Component({
  selector: 'app-cql-editor-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatSlideToggleModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatExpansionModule,
    MatTabsModule,
    MatBadgeModule,
    LoadingButtonComponent,
    MonacoEditorModule,
  ],
  template: `
    <div class="editor-dialog" [class.ai-panel-open]="showAiPanel" [class.preview-panel-open]="showPreviewPanel">
      <!-- Patient Context Banner -->
      @if (data.contextPatient) {
        <div class="patient-context-banner">
          <div class="patient-context-info">
            <mat-icon>person</mat-icon>
            <span class="patient-name">{{ getPatientDisplayName(data.contextPatient) }}</span>
            <span class="patient-meta">{{ getPatientMRN(data.contextPatient) }} • {{ getPatientAge(data.contextPatient) }}yo {{ data.contextPatient.gender }}</span>
          </div>
          <div class="patient-context-actions">
            <button
              mat-flat-button
              color="primary"
              (click)="testContextPatient()"
              [disabled]="!cqlText.trim() || isTestingContextPatient"
              matTooltip="Evaluate this CQL against the current patient">
              @if (isTestingContextPatient) {
                <mat-spinner diameter="16"></mat-spinner>
              } @else {
                <mat-icon>play_arrow</mat-icon>
              }
              Test This Patient
            </button>
            <button mat-icon-button (click)="clearContextPatient()" matTooltip="Use sample patients instead">
              <mat-icon>close</mat-icon>
            </button>
          </div>
        </div>
      }

      @if (contextPatientResult) {
        <div class="context-patient-result" [class]="'outcome-' + contextPatientResult.outcome">
          <mat-icon>{{ getOutcomeIcon(contextPatientResult.outcome) }}</mat-icon>
          <span class="outcome-label">{{ contextPatientResult.outcome | uppercase }}</span>
          <span class="outcome-message">{{ contextPatientResult.message }}</span>
          <button mat-icon-button (click)="clearContextPatientResult()" matTooltip="Dismiss">
            <mat-icon>close</mat-icon>
          </button>
        </div>
      }

      <!-- Header -->
      <div class="dialog-header">
        <div class="header-left">
          <mat-icon color="primary">code</mat-icon>
          <div>
            <h2>CQL Editor</h2>
            <p class="measure-name">{{ data.measureName }}</p>
          </div>
        </div>
        <div class="header-actions">
          <button
            mat-icon-button
            [matTooltip]="showPreviewPanel ? 'Hide Live Preview' : 'Live Preview'"
            [color]="showPreviewPanel ? 'accent' : 'primary'"
            (click)="togglePreviewPanel()"
            [matBadge]="previewBadge"
            [matBadgeHidden]="!previewBadge"
            matBadgeColor="accent"
            matBadgeSize="small">
            <mat-icon>{{ showPreviewPanel ? 'preview' : 'visibility' }}</mat-icon>
          </button>
          <button
            mat-icon-button
            [matTooltip]="showAiPanel ? 'Hide AI Assistant' : 'AI Assistant'"
            [color]="showAiPanel ? 'accent' : 'primary'"
            (click)="toggleAiPanel()">
            <mat-icon>{{ showAiPanel ? 'smart_toy' : 'auto_awesome' }}</mat-icon>
          </button>
          <button
            mat-icon-button
            [matTooltip]="isFullScreen ? 'Exit Full Screen' : 'Full Screen'"
            (click)="toggleFullScreen()">
            <mat-icon>{{ isFullScreen ? 'fullscreen_exit' : 'fullscreen' }}</mat-icon>
          </button>
          <button
            mat-icon-button
            matTooltip="Close"
            (click)="cancel()">
            <mat-icon>close</mat-icon>
          </button>
        </div>
      </div>

      <!-- Main Content Area -->
      <div class="main-content">
        <!-- AI Panel -->
        @if (showAiPanel) {
          <div class="ai-panel">
            <div class="ai-panel-header">
              <mat-icon color="accent">auto_awesome</mat-icon>
              <span>AI CQL Generator</span>
            </div>

            <div class="ai-panel-content">
              <!-- Prompt Input -->
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Describe your measure in plain English</mat-label>
                <textarea
                  matInput
                  [(ngModel)]="aiPrompt"
                  rows="4"
                  placeholder="e.g., Create a measure for adults 18-75 with diabetes who had an HbA1c test in the measurement period"
                  [disabled]="aiGenerating"></textarea>
                <mat-hint>Be specific about population, conditions, and interventions</mat-hint>
              </mat-form-field>

              <!-- Quick Prompts -->
              <div class="quick-prompts">
                <span class="quick-prompt-label">Quick prompts:</span>
                <div class="prompt-chips">
                  @for (prompt of quickPrompts; track prompt.label) {
                    <mat-chip (click)="useQuickPrompt(prompt.text)" [disabled]="aiGenerating">
                      {{ prompt.label }}
                    </mat-chip>
                  }
                </div>
              </div>

              <!-- Generate Button -->
              <div class="ai-actions">
                <app-loading-button
                  text="Generate CQL"
                  icon="auto_awesome"
                  color="accent"
                  variant="raised"
                  [loading]="aiGenerating"
                  loadingText="Generating..."
                  [disabled]="!aiPrompt.trim() || (data.readOnly ?? false)"
                  ariaLabel="Generate CQL from description"
                  (buttonClick)="generateCql()">
                </app-loading-button>

                <button
                  mat-button
                  (click)="validateCql()"
                  [disabled]="!cqlText.trim() || validating">
                  <mat-icon>check_circle</mat-icon>
                  Validate
                </button>
              </div>

              <!-- Generation Result -->
              @if (aiResult) {
                <div class="ai-result">
                  <div class="confidence-badge">
                    <mat-icon>psychology</mat-icon>
                    <span>Confidence: {{ (aiResult.confidence * 100).toFixed(0) }}%</span>
                  </div>

                  @if (aiResult.explanation) {
                    <mat-expansion-panel class="explanation-panel">
                      <mat-expansion-panel-header>
                        <mat-panel-title>
                          <mat-icon>info</mat-icon>
                          Explanation
                        </mat-panel-title>
                      </mat-expansion-panel-header>
                      <div class="explanation-content" [innerHTML]="formatExplanation(aiResult.explanation)"></div>
                    </mat-expansion-panel>
                  }

                  @if (aiResult.suggestedValueSets && aiResult.suggestedValueSets.length > 0) {
                    <mat-expansion-panel class="valueset-panel">
                      <mat-expansion-panel-header>
                        <mat-panel-title>
                          <mat-icon>list</mat-icon>
                          Suggested Value Sets ({{ aiResult.suggestedValueSets.length }})
                        </mat-panel-title>
                      </mat-expansion-panel-header>
                      <div class="valueset-list">
                        @for (vs of aiResult.suggestedValueSets; track vs.oid) {
                          <div class="valueset-item">
                            <div class="valueset-name">{{ vs.name }}</div>
                            <div class="valueset-meta">
                              <span class="oid">{{ vs.oid }}</span>
                              <span class="codesystem">{{ vs.codeSystem }}</span>
                            </div>
                            <div class="valueset-desc">{{ vs.description }}</div>
                          </div>
                        }
                      </div>
                    </mat-expansion-panel>
                  }

                  @if (aiResult.warnings && aiResult.warnings.length > 0) {
                    <div class="ai-warnings">
                      @for (warning of aiResult.warnings; track warning) {
                        <div class="warning-item">
                          <mat-icon>warning</mat-icon>
                          {{ warning }}
                        </div>
                      }
                    </div>
                  }
                </div>
              }

              <!-- Validation Result -->
              @if (validationResult) {
                <div class="validation-result" [class.valid]="validationResult.valid" [class.invalid]="!validationResult.valid">
                  <div class="validation-header">
                    <mat-icon>{{ validationResult.valid ? 'check_circle' : 'error' }}</mat-icon>
                    <span>{{ validationResult.valid ? 'CQL is valid' : 'Validation issues found' }}</span>
                  </div>

                  @if (validationResult.errors.length > 0) {
                    <div class="validation-section errors">
                      <h5>Errors ({{ validationResult.errors.length }})</h5>
                      @for (error of validationResult.errors; track error.line) {
                        <div class="validation-item error">
                          <span class="location">Line {{ error.line }}:{{ error.column }}</span>
                          <span class="message">{{ error.message }}</span>
                        </div>
                      }
                    </div>
                  }

                  @if (validationResult.warnings.length > 0) {
                    <div class="validation-section warnings">
                      <h5>Warnings ({{ validationResult.warnings.length }})</h5>
                      @for (warning of validationResult.warnings; track warning.line) {
                        <div class="validation-item warning">
                          <span class="location">Line {{ warning.line }}:{{ warning.column }}</span>
                          <span class="message">{{ warning.message }}</span>
                        </div>
                      }
                    </div>
                  }

                  @if (validationResult.suggestions.length > 0) {
                    <div class="validation-section suggestions">
                      <h5>Suggestions ({{ validationResult.suggestions.length }})</h5>
                      @for (suggestion of validationResult.suggestions; track suggestion.line) {
                        <div class="validation-item suggestion">
                          <span class="location">Line {{ suggestion.line }}:{{ suggestion.column }}</span>
                          <span class="message">{{ suggestion.message }}</span>
                        </div>
                      }
                    </div>
                  }
                </div>
              }
            </div>
          </div>
        }

        <!-- Editor Section -->
        <div class="editor-section">
          <!-- Editor Controls -->
          <div class="editor-controls">
            <div class="control-group">
              <mat-slide-toggle
                [(ngModel)]="showLineNumbers"
                (change)="updateEditorOptions()">
                Line Numbers
              </mat-slide-toggle>
              <mat-slide-toggle
                [(ngModel)]="wordWrap"
                (change)="updateEditorOptions()">
                Word Wrap
              </mat-slide-toggle>
              <mat-slide-toggle
                [(ngModel)]="minimap"
                (change)="updateEditorOptions()">
                Minimap
              </mat-slide-toggle>
            </div>
            <div class="status-info">
              <span class="status-badge" [class.readonly]="data.readOnly">
                {{ data.readOnly ? 'Read-Only' : 'Editable' }}
              </span>
              @if (hasChanges()) {
                <span class="unsaved-badge">Unsaved changes</span>
              }
            </div>
          </div>

          <!-- Monaco Editor -->
          <div class="editor-container">
            <ngx-monaco-editor
              [(ngModel)]="cqlText"
              [options]="editorOptions"
              (ngModelChange)="onCqlChange($event)"
              class="monaco-editor">
            </ngx-monaco-editor>
          </div>
        </div>

        <!-- Live Preview Panel -->
        @if (showPreviewPanel) {
          <div class="preview-panel">
            <div class="preview-panel-header">
              <mat-icon color="primary">preview</mat-icon>
              <span>Live Patient Preview</span>
              @if (isPreviewEvaluating) {
                <mat-spinner diameter="16" class="header-spinner"></mat-spinner>
              }
            </div>

            <div class="preview-panel-content">
              <!-- Preview Summary -->
              @if (previewResult) {
                <div class="preview-summary">
                  <div class="summary-row">
                    <div class="summary-stat pass">
                      <mat-icon>check_circle</mat-icon>
                      <span class="stat-value">{{ previewResult.summary.passed }}</span>
                      <span class="stat-label">Pass</span>
                    </div>
                    <div class="summary-stat fail">
                      <mat-icon>cancel</mat-icon>
                      <span class="stat-value">{{ previewResult.summary.failed }}</span>
                      <span class="stat-label">Fail</span>
                    </div>
                    <div class="summary-stat not-eligible">
                      <mat-icon>block</mat-icon>
                      <span class="stat-value">{{ previewResult.summary.notEligible }}</span>
                      <span class="stat-label">N/A</span>
                    </div>
                  </div>

                  @if (!previewResult.cqlValid && previewResult.cqlErrors) {
                    <div class="cql-errors">
                      <mat-icon>error</mat-icon>
                      <div class="error-list">
                        @for (err of previewResult.cqlErrors; track err) {
                          <div class="error-item">{{ err }}</div>
                        }
                      </div>
                    </div>
                  }

                  <div class="execution-info">
                    <mat-icon>timer</mat-icon>
                    <span>{{ previewResult.executionTimeMs }}ms</span>
                  </div>
                </div>
              }

              <!-- Patient Results -->
              <div class="patient-results">
                <h4>Sample Patients</h4>

                @if (isPreviewEvaluating && !previewResult) {
                  <div class="evaluating-placeholder">
                    <mat-spinner diameter="32"></mat-spinner>
                    <p>Evaluating CQL...</p>
                  </div>
                }

                @if (previewResult && previewResult.results.length > 0) {
                  <mat-accordion class="patient-accordion" multi>
                    @for (result of previewResult.results; track result.patientId) {
                      <mat-expansion-panel class="patient-panel">
                        <mat-expansion-panel-header>
                          <mat-panel-title>
                            <div class="patient-row">
                              <mat-icon [class]="'outcome-icon ' + result.outcome">
                                {{ getOutcomeIcon(result.outcome) }}
                              </mat-icon>
                              <div class="patient-info">
                                <span class="patient-name">{{ result.patientName }}</span>
                                <span class="patient-mrn">{{ result.mrn }}</span>
                              </div>
                              <mat-chip [class]="'outcome-chip ' + result.outcome" size="small">
                                {{ result.outcome | uppercase }}
                              </mat-chip>
                            </div>
                          </mat-panel-title>
                        </mat-expansion-panel-header>

                        <div class="patient-details">
                          <!-- Population Indicators -->
                          <div class="population-indicators">
                            <div class="indicator" [class.met]="result.inInitialPopulation">
                              <mat-icon>{{ result.inInitialPopulation ? 'check_circle' : 'cancel' }}</mat-icon>
                              <span>Initial Pop</span>
                            </div>
                            <mat-icon class="arrow">arrow_forward</mat-icon>
                            <div class="indicator" [class.met]="result.inDenominator">
                              <mat-icon>{{ result.inDenominator ? 'check_circle' : 'cancel' }}</mat-icon>
                              <span>Denominator</span>
                            </div>
                            <mat-icon class="arrow">arrow_forward</mat-icon>
                            <div class="indicator" [class.met]="result.inNumerator">
                              <mat-icon>{{ result.inNumerator ? 'check_circle' : 'cancel' }}</mat-icon>
                              <span>Numerator</span>
                            </div>
                          </div>

                          <!-- Matched Criteria -->
                          @if (result.matchedCriteria && result.matchedCriteria.length > 0) {
                            <div class="criteria-section">
                              <h5>Evaluation Details</h5>
                              @for (criterion of result.matchedCriteria; track criterion.criterionName) {
                                <div class="criterion-item" [class.matched]="criterion.matched" [class.not-matched]="!criterion.matched">
                                  <mat-icon>{{ criterion.matched ? 'check' : 'close' }}</mat-icon>
                                  <div class="criterion-content">
                                    <span class="criterion-name">{{ criterion.criterionName }}</span>
                                    <span class="criterion-reason">{{ criterion.reason }}</span>
                                    @if (criterion.matchedResources && criterion.matchedResources.length > 0) {
                                      <div class="matched-resources">
                                        @for (resource of criterion.matchedResources; track resource) {
                                          <mat-chip size="small">{{ resource }}</mat-chip>
                                        }
                                      </div>
                                    }
                                  </div>
                                </div>
                              }
                            </div>
                          }
                        </div>
                      </mat-expansion-panel>
                    }
                  </mat-accordion>
                }

                @if (!previewResult && !isPreviewEvaluating) {
                  <div class="no-results">
                    <mat-icon>edit</mat-icon>
                    <p>Start editing CQL to see live preview results</p>
                  </div>
                }
              </div>

              <!-- Test Specific Patient -->
              <div class="specific-patient-test">
                <h4>Test Specific Patient</h4>
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Search by name or MRN</mat-label>
                  <input
                    matInput
                    [(ngModel)]="specificPatientSearch"
                    (ngModelChange)="onPatientSearchChange($event)"
                    placeholder="e.g., John Smith or PRV-001">
                  <mat-icon matSuffix>search</mat-icon>
                </mat-form-field>

                @if (patientSearchResults.length > 0) {
                  <div class="search-results">
                    @for (patient of patientSearchResults; track patient.id) {
                      <div
                        class="search-result-item"
                        [class.selected]="selectedSpecificPatient?.id === patient.id"
                        (click)="selectSpecificPatient(patient)">
                        <div class="patient-info">
                          <span class="name">{{ patient.name }}</span>
                          <span class="mrn">{{ patient.mrn }}</span>
                        </div>
                        <div class="patient-meta">
                          <span>{{ patient.age }}yo {{ patient.gender }}</span>
                        </div>
                      </div>
                    }
                  </div>
                }

                @if (selectedSpecificPatient) {
                  <div class="selected-patient-card">
                    <div class="card-header">
                      <mat-icon>person</mat-icon>
                      <span>{{ selectedSpecificPatient.name }}</span>
                      <button mat-icon-button (click)="clearSpecificPatient()" matTooltip="Clear selection">
                        <mat-icon>close</mat-icon>
                      </button>
                    </div>
                    <div class="card-body">
                      <div class="patient-details-row">
                        <span class="label">MRN:</span>
                        <span class="value">{{ selectedSpecificPatient.mrn }}</span>
                      </div>
                      <div class="patient-details-row">
                        <span class="label">Age/Gender:</span>
                        <span class="value">{{ selectedSpecificPatient.age }}yo {{ selectedSpecificPatient.gender }}</span>
                      </div>
                      <div class="patient-conditions">
                        @for (condition of selectedSpecificPatient.conditions; track condition) {
                          <mat-chip size="small">{{ condition }}</mat-chip>
                        }
                      </div>
                    </div>
                    <div class="card-actions">
                      <app-loading-button
                        text="Test This Patient"
                        icon="play_arrow"
                        color="primary"
                        variant="flat"
                        [loading]="isTestingSpecificPatient"
                        loadingText="Testing..."
                        [disabled]="!cqlText.trim()"
                        ariaLabel="Test CQL against selected patient"
                        (buttonClick)="testSpecificPatient()">
                      </app-loading-button>
                    </div>
                  </div>
                }

                @if (specificPatientResult) {
                  <div class="specific-patient-result" [class]="'outcome-' + (specificPatientResult.results[0]?.outcome ?? 'unknown')">
                    <div class="result-header">
                      <mat-icon>{{ getOutcomeIcon(specificPatientResult.results[0]?.outcome ?? '') }}</mat-icon>
                      <span>{{ (specificPatientResult.results[0]?.outcome ?? 'unknown') | uppercase }}</span>
                    </div>
                    @if (specificPatientResult.results[0]?.matchedCriteria) {
                      <div class="criteria-list">
                        @for (criterion of specificPatientResult.results[0].matchedCriteria; track criterion.criterionName) {
                          <div class="criterion" [class.matched]="criterion.matched">
                            <mat-icon>{{ criterion.matched ? 'check' : 'close' }}</mat-icon>
                            <div class="criterion-text">
                              <span class="name">{{ criterion.criterionName }}</span>
                              <span class="reason">{{ criterion.reason }}</span>
                            </div>
                          </div>
                        }
                      </div>
                    }
                  </div>
                }
              </div>

              <!-- Sample Patient Info -->
              <div class="sample-patients-info">
                <mat-expansion-panel>
                  <mat-expansion-panel-header>
                    <mat-panel-title>
                      <mat-icon>people</mat-icon>
                      Sample Patient Data ({{ samplePatients.length }})
                    </mat-panel-title>
                  </mat-expansion-panel-header>
                  <div class="sample-patients-list">
                    @for (patient of samplePatients; track patient.id) {
                      <div class="sample-patient-item" (click)="selectSpecificPatient(patient)">
                        <div class="patient-header">
                          <span class="name">{{ patient.name }}</span>
                          <span class="demographics">{{ patient.age }}yo {{ patient.gender }}</span>
                        </div>
                        <div class="patient-conditions">
                          @for (condition of patient.conditions; track condition) {
                            <mat-chip size="small">{{ condition }}</mat-chip>
                          }
                        </div>
                      </div>
                    }
                  </div>
                </mat-expansion-panel>
              </div>
            </div>
          </div>
        }
      </div>

      <!-- Footer Actions -->
      <div class="dialog-footer">
        <div class="footer-info">
          <mat-icon>info</mat-icon>
          <span>Changes are auto-saved when you click Save</span>
        </div>
        <div class="footer-actions">
          <button mat-button (click)="cancel()">
            <mat-icon>close</mat-icon>
            Cancel
          </button>
          <app-loading-button
            text="Save CQL"
            icon="save"
            color="primary"
            variant="raised"
            [disabled]="data.readOnly || !hasChanges()"
            ariaLabel="Save CQL changes"
            (buttonClick)="save()">
          </app-loading-button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .editor-dialog {
      display: flex;
      flex-direction: column;
      height: 100%;
      max-height: 85vh;
    }

    /* Patient Context Banner */
    .patient-context-banner {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 24px;
      background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
      border-bottom: 2px solid #2196f3;

      .patient-context-info {
        display: flex;
        align-items: center;
        gap: 8px;

        mat-icon {
          color: #1976d2;
        }

        .patient-name {
          font-weight: 500;
          color: #1565c0;
        }

        .patient-meta {
          color: #546e7a;
          font-size: 13px;
        }
      }

      .patient-context-actions {
        display: flex;
        align-items: center;
        gap: 8px;

        button[mat-flat-button] {
          gap: 6px;

          mat-spinner {
            margin-right: 4px;
          }
        }
      }
    }

    .context-patient-result {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 8px 24px;
      border-bottom: 1px solid #e0e0e0;

      &.outcome-pass {
        background-color: #e8f5e9;
        .outcome-label { color: #2e7d32; font-weight: 500; }
        mat-icon { color: #4caf50; }
      }

      &.outcome-fail {
        background-color: #ffebee;
        .outcome-label { color: #c62828; font-weight: 500; }
        mat-icon { color: #f44336; }
      }

      &.outcome-not-eligible, &.outcome-excluded {
        background-color: #fff3e0;
        .outcome-label { color: #ef6c00; font-weight: 500; }
        mat-icon { color: #ff9800; }
      }

      &.outcome-error, &.outcome-unknown {
        background-color: #fce4ec;
        .outcome-label { color: #ad1457; font-weight: 500; }
        mat-icon { color: #e91e63; }
      }

      .outcome-message {
        flex: 1;
        color: #616161;
        font-size: 13px;
      }
    }

    .dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 24px;
      background-color: #f5f5f5;
      border-bottom: 1px solid #e0e0e0;

      .header-left {
        display: flex;
        align-items: center;
        gap: 12px;

        mat-icon {
          font-size: 32px;
          width: 32px;
          height: 32px;
        }

        h2 {
          margin: 0;
          font-size: 20px;
          font-weight: 500;
        }

        .measure-name {
          margin: 4px 0 0 0;
          color: #666;
          font-size: 13px;
        }
      }

      .header-actions {
        display: flex;
        gap: 4px;
      }
    }

    .main-content {
      flex: 1;
      display: flex;
      min-height: 0;
      overflow: hidden;
    }

    /* AI Panel */
    .ai-panel {
      width: 380px;
      border-right: 1px solid #e0e0e0;
      display: flex;
      flex-direction: column;
      background-color: #fafafa;
    }

    .ai-panel-header {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      background-color: #fff3e0;
      border-bottom: 1px solid #ffcc80;
      font-weight: 500;

      mat-icon {
        color: #ff9800;
      }
    }

    .ai-panel-content {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
    }

    .quick-prompts {
      margin-bottom: 16px;

      .quick-prompt-label {
        display: block;
        font-size: 12px;
        color: #666;
        margin-bottom: 8px;
      }

      .prompt-chips {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        mat-chip {
          font-size: 11px;
          cursor: pointer;

          &:hover {
            background-color: #e3f2fd;
          }
        }
      }
    }

    .ai-actions {
      display: flex;
      gap: 8px;
      margin-bottom: 16px;
    }

    .ai-result {
      .confidence-badge {
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 8px 12px;
        background-color: #e8f5e9;
        border-radius: 4px;
        margin-bottom: 12px;
        font-size: 13px;
        color: #2e7d32;

        mat-icon {
          font-size: 18px;
          width: 18px;
          height: 18px;
        }
      }

      .explanation-panel, .valueset-panel {
        margin-bottom: 12px;

        mat-panel-title {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 13px;

          mat-icon {
            font-size: 18px;
            width: 18px;
            height: 18px;
          }
        }
      }

      .explanation-content {
        font-size: 13px;
        line-height: 1.5;
        white-space: pre-wrap;
      }

      .valueset-list {
        .valueset-item {
          padding: 8px 0;
          border-bottom: 1px solid #e0e0e0;

          &:last-child {
            border-bottom: none;
          }

          .valueset-name {
            font-weight: 500;
            font-size: 13px;
          }

          .valueset-meta {
            display: flex;
            gap: 12px;
            font-size: 11px;
            color: #666;
            margin: 4px 0;

            .oid {
              font-family: monospace;
            }

            .codesystem {
              background-color: #e3f2fd;
              padding: 2px 6px;
              border-radius: 4px;
            }
          }

          .valueset-desc {
            font-size: 12px;
            color: #666;
          }
        }
      }

      .ai-warnings {
        .warning-item {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 8px;
          background-color: #fff3e0;
          border-radius: 4px;
          margin-top: 8px;
          font-size: 12px;
          color: #e65100;

          mat-icon {
            font-size: 18px;
            width: 18px;
            height: 18px;
          }
        }
      }
    }

    .validation-result {
      padding: 12px;
      border-radius: 8px;
      margin-top: 16px;

      &.valid {
        background-color: #e8f5e9;
      }

      &.invalid {
        background-color: #ffebee;
      }

      .validation-header {
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: 500;
        margin-bottom: 12px;

        mat-icon {
          font-size: 20px;
          width: 20px;
          height: 20px;
        }
      }

      .validation-section {
        margin-top: 12px;

        h5 {
          margin: 0 0 8px 0;
          font-size: 12px;
          text-transform: uppercase;
          color: #666;
        }

        .validation-item {
          display: flex;
          flex-direction: column;
          padding: 6px 8px;
          border-radius: 4px;
          margin-bottom: 6px;
          font-size: 12px;

          &.error {
            background-color: #ffcdd2;
          }

          &.warning {
            background-color: #fff9c4;
          }

          &.suggestion {
            background-color: #e3f2fd;
          }

          .location {
            font-family: monospace;
            font-size: 11px;
            color: #666;
          }

          .message {
            margin-top: 2px;
          }
        }
      }
    }

    /* Editor Section */
    .editor-section {
      flex: 1;
      display: flex;
      flex-direction: column;
      min-width: 0;
    }

    .editor-controls {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 24px;
      background-color: #fafafa;
      border-bottom: 1px solid #e0e0e0;

      .control-group {
        display: flex;
        gap: 24px;
      }

      .status-info {
        display: flex;
        gap: 8px;
      }

      .status-badge {
        display: inline-block;
        padding: 4px 12px;
        border-radius: 12px;
        background-color: #e8f5e9;
        color: #2e7d32;
        font-size: 12px;
        font-weight: 500;

        &.readonly {
          background-color: #fff3e0;
          color: #e65100;
        }
      }

      .unsaved-badge {
        display: inline-block;
        padding: 4px 12px;
        border-radius: 12px;
        background-color: #fff3e0;
        color: #e65100;
        font-size: 12px;
        font-weight: 500;
      }
    }

    .editor-container {
      flex: 1;
      min-height: 0;
      position: relative;

      .monaco-editor {
        height: 100%;
        width: 100%;
      }
    }

    /* Preview Panel */
    .preview-panel {
      width: 400px;
      border-left: 1px solid #e0e0e0;
      display: flex;
      flex-direction: column;
      background-color: #fafafa;
    }

    .preview-panel-header {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      background-color: #e3f2fd;
      border-bottom: 1px solid #90caf9;
      font-weight: 500;

      mat-icon {
        color: #1976d2;
      }

      .header-spinner {
        margin-left: auto;
      }
    }

    .preview-panel-content {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
    }

    .preview-summary {
      margin-bottom: 16px;

      .summary-row {
        display: flex;
        gap: 8px;
        margin-bottom: 12px;

        .summary-stat {
          flex: 1;
          display: flex;
          flex-direction: column;
          align-items: center;
          padding: 12px 8px;
          border-radius: 8px;
          background-color: #fff;
          border: 1px solid #e0e0e0;

          mat-icon {
            font-size: 20px;
            width: 20px;
            height: 20px;
          }

          .stat-value {
            font-size: 24px;
            font-weight: 600;
            margin: 4px 0;
          }

          .stat-label {
            font-size: 10px;
            text-transform: uppercase;
            color: #666;
          }

          &.pass {
            border-color: #a5d6a7;
            mat-icon { color: #2e7d32; }
            .stat-value { color: #2e7d32; }
          }

          &.fail {
            border-color: #ef9a9a;
            mat-icon { color: #c62828; }
            .stat-value { color: #c62828; }
          }

          &.not-eligible {
            border-color: #90caf9;
            mat-icon { color: #1565c0; }
            .stat-value { color: #1565c0; }
          }
        }
      }

      .cql-errors {
        display: flex;
        gap: 8px;
        padding: 12px;
        background-color: #ffebee;
        border-radius: 8px;
        margin-bottom: 12px;

        mat-icon {
          color: #c62828;
          flex-shrink: 0;
        }

        .error-list {
          .error-item {
            font-size: 12px;
            color: #c62828;
            margin-bottom: 4px;

            &:last-child {
              margin-bottom: 0;
            }
          }
        }
      }

      .execution-info {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 12px;
        color: #666;

        mat-icon {
          font-size: 14px;
          width: 14px;
          height: 14px;
        }
      }
    }

    .patient-results {
      h4 {
        margin: 0 0 12px 0;
        font-size: 14px;
        font-weight: 600;
      }

      .evaluating-placeholder {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 32px;
        color: #666;

        p {
          margin: 12px 0 0 0;
        }
      }

      .no-results {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 32px;
        color: #999;

        mat-icon {
          font-size: 48px;
          width: 48px;
          height: 48px;
          margin-bottom: 12px;
        }

        p {
          text-align: center;
          margin: 0;
        }
      }

      .patient-accordion {
        .patient-panel {
          margin-bottom: 8px;

          .patient-row {
            display: flex;
            align-items: center;
            gap: 8px;
            width: 100%;

            .outcome-icon {
              font-size: 20px;
              width: 20px;
              height: 20px;

              &.pass { color: #2e7d32; }
              &.fail { color: #c62828; }
              &.not-eligible { color: #1565c0; }
              &.excluded { color: #f57c00; }
              &.error { color: #d32f2f; }
            }

            .patient-info {
              flex: 1;
              display: flex;
              flex-direction: column;

              .patient-name {
                font-size: 13px;
                font-weight: 500;
              }

              .patient-mrn {
                font-size: 11px;
                color: #666;
              }
            }

            .outcome-chip {
              font-size: 10px;
              min-height: 20px;
              padding: 0 8px;

              &.pass { background-color: #e8f5e9; color: #2e7d32; }
              &.fail { background-color: #ffebee; color: #c62828; }
              &.not-eligible { background-color: #e3f2fd; color: #1565c0; }
              &.excluded { background-color: #fff3e0; color: #f57c00; }
              &.error { background-color: #ffebee; color: #d32f2f; }
            }
          }
        }

        .patient-details {
          padding: 12px 0;

          .population-indicators {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 4px;
            margin-bottom: 16px;
            padding: 12px;
            background-color: #f5f5f5;
            border-radius: 8px;

            .indicator {
              display: flex;
              flex-direction: column;
              align-items: center;
              gap: 4px;

              mat-icon {
                font-size: 24px;
                width: 24px;
                height: 24px;
                color: #bdbdbd;
              }

              span {
                font-size: 10px;
                color: #666;
              }

              &.met {
                mat-icon {
                  color: #2e7d32;
                }
              }
            }

            .arrow {
              font-size: 16px;
              width: 16px;
              height: 16px;
              color: #bdbdbd;
            }
          }

          .criteria-section {
            h5 {
              margin: 0 0 8px 0;
              font-size: 12px;
              text-transform: uppercase;
              color: #666;
            }

            .criterion-item {
              display: flex;
              gap: 8px;
              padding: 8px;
              border-radius: 4px;
              margin-bottom: 8px;

              &.matched {
                background-color: #e8f5e9;

                mat-icon {
                  color: #2e7d32;
                }
              }

              &.not-matched {
                background-color: #ffebee;

                mat-icon {
                  color: #c62828;
                }
              }

              mat-icon {
                font-size: 18px;
                width: 18px;
                height: 18px;
                flex-shrink: 0;
              }

              .criterion-content {
                flex: 1;

                .criterion-name {
                  display: block;
                  font-weight: 500;
                  font-size: 12px;
                }

                .criterion-reason {
                  display: block;
                  font-size: 11px;
                  color: #666;
                  margin-top: 2px;
                }

                .matched-resources {
                  display: flex;
                  flex-wrap: wrap;
                  gap: 4px;
                  margin-top: 6px;

                  mat-chip {
                    font-size: 10px;
                    min-height: 18px;
                    padding: 0 6px;
                  }
                }
              }
            }
          }
        }
      }
    }

    /* Specific Patient Test */
    .specific-patient-test {
      margin-bottom: 16px;
      padding: 12px;
      background-color: #fff;
      border-radius: 8px;
      border: 1px solid #e0e0e0;

      h4 {
        margin: 0 0 12px 0;
        font-size: 14px;
        font-weight: 600;
        color: #1976d2;
      }

      .search-results {
        margin-top: 8px;
        border: 1px solid #e0e0e0;
        border-radius: 4px;
        max-height: 150px;
        overflow-y: auto;

        .search-result-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 8px 12px;
          cursor: pointer;
          border-bottom: 1px solid #e0e0e0;

          &:last-child {
            border-bottom: none;
          }

          &:hover {
            background-color: #e3f2fd;
          }

          &.selected {
            background-color: #bbdefb;
          }

          .patient-info {
            .name {
              font-weight: 500;
              font-size: 13px;
            }

            .mrn {
              font-size: 11px;
              color: #666;
              margin-left: 8px;
            }
          }

          .patient-meta {
            font-size: 11px;
            color: #666;
          }
        }
      }

      .selected-patient-card {
        margin-top: 12px;
        border: 1px solid #1976d2;
        border-radius: 8px;
        overflow: hidden;

        .card-header {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 8px 12px;
          background-color: #e3f2fd;
          font-weight: 500;

          mat-icon {
            color: #1976d2;
          }

          span {
            flex: 1;
          }

          button {
            margin: -4px;
          }
        }

        .card-body {
          padding: 12px;

          .patient-details-row {
            display: flex;
            gap: 8px;
            margin-bottom: 6px;
            font-size: 12px;

            .label {
              color: #666;
              min-width: 80px;
            }

            .value {
              font-weight: 500;
            }
          }

          .patient-conditions {
            display: flex;
            flex-wrap: wrap;
            gap: 4px;
            margin-top: 8px;

            mat-chip {
              font-size: 10px;
              min-height: 18px;
              padding: 0 6px;
              background-color: #e3f2fd;
            }
          }
        }

        .card-actions {
          padding: 8px 12px;
          border-top: 1px solid #e0e0e0;
          background-color: #fafafa;
        }
      }

      .specific-patient-result {
        margin-top: 12px;
        border-radius: 8px;
        padding: 12px;

        &.outcome-pass {
          background-color: #e8f5e9;
          border: 1px solid #a5d6a7;
        }

        &.outcome-fail {
          background-color: #ffebee;
          border: 1px solid #ef9a9a;
        }

        &.outcome-not-eligible {
          background-color: #e3f2fd;
          border: 1px solid #90caf9;
        }

        &.outcome-excluded {
          background-color: #fff3e0;
          border: 1px solid #ffcc80;
        }

        &.outcome-error {
          background-color: #ffebee;
          border: 1px solid #ef9a9a;
        }

        .result-header {
          display: flex;
          align-items: center;
          gap: 8px;
          font-weight: 600;
          margin-bottom: 12px;

          mat-icon {
            font-size: 24px;
            width: 24px;
            height: 24px;
          }
        }

        .criteria-list {
          .criterion {
            display: flex;
            gap: 8px;
            padding: 6px 0;
            border-top: 1px solid rgba(0, 0, 0, 0.1);

            &:first-child {
              border-top: none;
            }

            mat-icon {
              font-size: 16px;
              width: 16px;
              height: 16px;
              flex-shrink: 0;
              margin-top: 2px;
            }

            &.matched mat-icon {
              color: #2e7d32;
            }

            &:not(.matched) mat-icon {
              color: #c62828;
            }

            .criterion-text {
              flex: 1;

              .name {
                display: block;
                font-weight: 500;
                font-size: 12px;
              }

              .reason {
                display: block;
                font-size: 11px;
                color: #666;
                margin-top: 2px;
              }
            }
          }
        }
      }
    }

    .sample-patients-info {
      margin-top: 16px;

      mat-panel-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 13px;

        mat-icon {
          font-size: 18px;
          width: 18px;
          height: 18px;
        }
      }

      .sample-patients-list {
        .sample-patient-item {
          padding: 8px 0;
          border-bottom: 1px solid #e0e0e0;

          &:last-child {
            border-bottom: none;
          }

          .patient-header {
            display: flex;
            justify-content: space-between;
            margin-bottom: 6px;

            .name {
              font-weight: 500;
              font-size: 13px;
            }

            .demographics {
              font-size: 12px;
              color: #666;
            }
          }

          .patient-conditions {
            display: flex;
            flex-wrap: wrap;
            gap: 4px;

            mat-chip {
              font-size: 10px;
              min-height: 18px;
              padding: 0 6px;
              background-color: #e3f2fd;
            }
          }
        }
      }
    }

    .dialog-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 24px;
      border-top: 1px solid #e0e0e0;
      background-color: #f5f5f5;

      .footer-info {
        display: flex;
        align-items: center;
        gap: 8px;
        color: #666;
        font-size: 13px;

        mat-icon {
          font-size: 18px;
          width: 18px;
          height: 18px;
        }
      }

      .footer-actions {
        display: flex;
        gap: 8px;
      }
    }

    .full-width {
      width: 100%;
    }

    :host ::ng-deep .mat-mdc-dialog-container {
      padding: 0;
    }

    /* Responsive adjustments when panels are open */
    .ai-panel-open {
      .editor-section {
        min-width: 400px;
      }
    }

    .preview-panel-open {
      .editor-section {
        min-width: 400px;
      }
    }

    .ai-panel-open.preview-panel-open {
      .editor-section {
        min-width: 300px;
      }
    }
  `],
})
export class CqlEditorDialogComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private cqlChange$ = new Subject<string>();
  private patientSearch$ = new Subject<string>();
  isSearchingPatients = false;

  // Editor state
  cqlText: string;
  originalCqlText: string;
  isFullScreen = false;
  showLineNumbers = true;
  wordWrap = false;
  minimap = true;

  // AI Panel state
  showAiPanel = false;
  aiPrompt = '';
  aiGenerating = false;
  aiResult: CqlGenerationResponse | null = null;
  validating = false;
  validationResult: CqlValidationResult | null = null;

  // Preview Panel state
  showPreviewPanel = false;
  isPreviewEvaluating = false;
  previewResult: LivePreviewResponse | null = null;
  samplePatients: PreviewPatient[] = [];
  previewBadge: string | null = null;

  // Specific Patient Test state
  specificPatientSearch = '';
  patientSearchResults: PreviewPatient[] = [];
  selectedSpecificPatient: PreviewPatient | null = null;
  isTestingSpecificPatient = false;
  specificPatientResult: LivePreviewResponse | null = null;

  // Context Patient state (patient passed from external navigation)
  isTestingContextPatient = false;
  contextPatientResult: { outcome: string; message: string } | null = null;

  // Quick prompts for common measure types
  quickPrompts = [
    { label: 'Diabetes HbA1c', text: 'Create a measure for adults 18-75 with diabetes who had an HbA1c test in the measurement period' },
    { label: 'BP Control', text: 'Create a measure for adults with hypertension who had blood pressure adequately controlled below 140/90' },
    { label: 'Cancer Screening', text: 'Create a breast cancer screening measure for women 50-74 who had a mammogram in the past 2 years' },
    { label: 'Depression Screen', text: 'Create a measure for patients 12+ screened for depression using PHQ-9 with follow-up if positive' },
    { label: 'Statin Therapy', text: 'Create a measure for adults with cardiovascular disease or diabetes who are on statin therapy' },
  ];

  editorOptions: any = {
    theme: 'vs',
    language: 'sql', // Using SQL as closest to CQL syntax
    automaticLayout: true,
    fontSize: 14,
    lineNumbers: 'on',
    wordWrap: 'off',
    minimap: { enabled: true },
    readOnly: false,
    scrollBeyondLastLine: false,
    renderWhitespace: 'selection',
    tabSize: 2,
    insertSpaces: true,
  };

  constructor(
    private dialogRef: MatDialogRef<CqlEditorDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CqlEditorDialogData,
    private cqlGenService: CqlGenerationService,
    private livePreviewService: LivePreviewService,
    private patientService: PatientService
  ) {
    this.cqlText = data.cqlText || this.getDefaultCqlTemplate();
    this.originalCqlText = this.cqlText;
  }

  ngOnInit(): void {
    if (this.data.readOnly) {
      this.editorOptions.readOnly = true;
    }

    // Load sample patients
    this.samplePatients = this.livePreviewService.getSamplePatients();

    // Subscribe to preview results
    this.livePreviewService.getResults().pipe(takeUntil(this.destroy$)).subscribe((result) => {
      this.previewResult = result;
      this.updatePreviewBadge();
    });

    // Subscribe to evaluation status
    this.livePreviewService.isEvaluating().pipe(takeUntil(this.destroy$)).subscribe((evaluating) => {
      this.isPreviewEvaluating = evaluating;
    });

    // Set up debounced CQL change handler for preview
    this.cqlChange$.pipe(
      debounceTime(800),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe((cql) => {
      if (this.showPreviewPanel && cql.trim()) {
        this.livePreviewService.submitCql(cql);
      }
    });

    // Trigger initial preview if panel is open and CQL exists
    if (this.showPreviewPanel && this.cqlText.trim()) {
      this.livePreviewService.submitCql(this.cqlText);
    }

    // Set up debounced patient search with real API
    this.patientSearch$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((query) => {
        if (!query || query.length < 2) {
          return of([]);
        }
        this.isSearchingPatients = true;
        // Try searching by name first, then identifier if looks like MRN
        const isIdentifier = /^[A-Z]{2,3}-?\d+$/i.test(query);
        if (isIdentifier) {
          return this.patientService.searchPatientsByIdentifier(query).pipe(
            catchError(() => of([] as Patient[]))
          );
        }
        return this.patientService.searchPatientsByName(query).pipe(
          catchError(() => of([] as Patient[]))
        );
      }),
      takeUntil(this.destroy$)
    ).subscribe((patients: Patient[]) => {
      this.isSearchingPatients = false;
      // Convert FHIR Patient to PreviewPatient format
      this.patientSearchResults = patients.map((p) => this.toPreviewPatient(p));
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Handle CQL text changes
   */
  onCqlChange(cql: string): void {
    this.cqlChange$.next(cql);
  }

  /**
   * Update preview badge based on results
   */
  private updatePreviewBadge(): void {
    if (this.previewResult && this.showPreviewPanel) {
      const passCount = this.previewResult.summary.passed;
      const failCount = this.previewResult.summary.failed;
      if (passCount > 0 || failCount > 0) {
        this.previewBadge = `${passCount}/${passCount + failCount}`;
      } else {
        this.previewBadge = null;
      }
    } else {
      this.previewBadge = null;
    }
  }

  /**
   * Toggle preview panel visibility
   */
  togglePreviewPanel(): void {
    this.showPreviewPanel = !this.showPreviewPanel;

    if (this.showPreviewPanel && this.cqlText.trim()) {
      // Trigger evaluation when panel opens
      this.livePreviewService.submitCql(this.cqlText);
    }

    this.updatePreviewBadge();
  }

  /**
   * Handle patient search input change - uses real patient API with debouncing
   */
  onPatientSearchChange(query: string): void {
    this.patientSearch$.next(query);
  }

  /**
   * Convert FHIR Patient to PreviewPatient format for display
   */
  private toPreviewPatient(patient: Patient): PreviewPatient {
    const name = this.getPatientDisplayName(patient);
    const mrn = this.getPatientMRN(patient);
    const age = this.getPatientAge(patient);
    const gender = patient.gender || 'unknown';

    return {
      id: patient.id,
      name,
      mrn,
      age,
      gender: gender === 'male' || gender === 'female' || gender === 'other' ? gender : 'other',
      conditions: [], // Would need separate call to get conditions
      medications: [],
      recentProcedures: [],
      recentObservations: [],
    };
  }

  /**
   * Select a specific patient for testing
   */
  selectSpecificPatient(patient: PreviewPatient): void {
    this.selectedSpecificPatient = patient;
    this.specificPatientSearch = '';
    this.patientSearchResults = [];
    this.specificPatientResult = null;
  }

  /**
   * Clear selected specific patient
   */
  clearSpecificPatient(): void {
    this.selectedSpecificPatient = null;
    this.specificPatientResult = null;
    this.specificPatientSearch = '';
  }

  /**
   * Test CQL against the selected specific patient
   */
  testSpecificPatient(): void {
    if (!this.selectedSpecificPatient || !this.cqlText.trim()) return;

    this.isTestingSpecificPatient = true;
    this.specificPatientResult = null;

    this.livePreviewService.evaluateSpecificPatient(
      this.cqlText,
      this.selectedSpecificPatient.id
    ).pipe(takeUntil(this.destroy$)).subscribe({
      next: (result) => {
        this.specificPatientResult = result;
        this.isTestingSpecificPatient = false;
      },
      error: () => {
        this.isTestingSpecificPatient = false;
      },
    });
  }

  /**
   * Get outcome icon for patient result
   */
  getOutcomeIcon(outcome: string): string {
    switch (outcome) {
      case 'pass':
        return 'check_circle';
      case 'fail':
        return 'cancel';
      case 'not-eligible':
        return 'block';
      case 'excluded':
        return 'remove_circle';
      case 'error':
        return 'error';
      default:
        return 'help';
    }
  }

  /**
   * Get default CQL template for new measures
   */
  private getDefaultCqlTemplate(): string {
    return `library ${this.data.measureName.replace(/\s+/g, '')} version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

// Value sets
codesystem "LOINC": 'http://loinc.org'
codesystem "SNOMEDCT": 'http://snomed.info/sct'

// Parameters
parameter "Measurement Period" Interval<DateTime>

// Define your measure logic here
context Patient

define "Initial Population":
  true

define "Denominator":
  "Initial Population"

define "Numerator":
  "Denominator"
`;
  }

  /**
   * Toggle AI panel visibility
   */
  toggleAiPanel(): void {
    this.showAiPanel = !this.showAiPanel;
  }

  /**
   * Use a quick prompt
   */
  useQuickPrompt(text: string): void {
    this.aiPrompt = text;
  }

  /**
   * Generate CQL from natural language
   */
  generateCql(): void {
    if (!this.aiPrompt.trim() || this.aiGenerating) return;

    this.aiGenerating = true;
    this.aiResult = null;
    this.validationResult = null;

    this.cqlGenService.generateCql({
      prompt: this.aiPrompt,
      context: {
        measureName: this.data.measureName,
      },
      options: {
        includeComments: true,
        includeValueSets: true,
        fhirVersion: '4.0.1',
      },
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (result) => {
        this.aiResult = result;
        // Replace editor content with generated CQL
        this.cqlText = result.cql;
        this.aiGenerating = false;

        // Trigger preview update
        if (this.showPreviewPanel) {
          this.livePreviewService.submitCql(this.cqlText);
        }
      },
      error: () => {
        this.aiGenerating = false;
      },
    });
  }

  /**
   * Validate current CQL
   */
  validateCql(): void {
    if (!this.cqlText.trim() || this.validating) return;

    this.validating = true;
    this.validationResult = null;

    this.cqlGenService.validateCql(this.cqlText).pipe(takeUntil(this.destroy$)).subscribe({
      next: (result) => {
        this.validationResult = result;
        this.validating = false;
      },
      error: () => {
        this.validating = false;
      },
    });
  }

  /**
   * Format explanation text for HTML display
   */
  formatExplanation(text: string): string {
    return text
      .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
      .replace(/\n/g, '<br>');
  }

  /**
   * Update editor options based on toggles
   */
  updateEditorOptions(): void {
    this.editorOptions = {
      ...this.editorOptions,
      lineNumbers: this.showLineNumbers ? 'on' : 'off',
      wordWrap: this.wordWrap ? 'on' : 'off',
      minimap: { enabled: this.minimap },
    };
  }

  /**
   * Toggle full screen mode
   */
  toggleFullScreen(): void {
    this.isFullScreen = !this.isFullScreen;
    const dialogElement = document.querySelector('.mat-mdc-dialog-container') as HTMLElement;
    if (dialogElement) {
      if (this.isFullScreen) {
        dialogElement.style.maxWidth = '100vw';
        dialogElement.style.maxHeight = '100vh';
        dialogElement.style.width = '100vw';
        dialogElement.style.height = '100vh';
      } else {
        dialogElement.style.maxWidth = '1400px';
        dialogElement.style.maxHeight = '85vh';
        dialogElement.style.width = '90vw';
        dialogElement.style.height = '85vh';
      }
    }
  }

  /**
   * Check if there are unsaved changes
   */
  hasChanges(): boolean {
    return this.cqlText !== this.originalCqlText;
  }

  /**
   * Save CQL changes
   */
  save(): void {
    if (!this.hasChanges() || this.data.readOnly) return;
    this.dialogRef.close(this.cqlText);
  }

  /**
   * Cancel and close dialog
   */
  cancel(): void {
    if (this.hasChanges()) {
      if (confirm('You have unsaved changes. Are you sure you want to close?')) {
        this.dialogRef.close();
      }
    } else {
      this.dialogRef.close();
    }
  }

  // ==========================================
  // Context Patient Methods
  // ==========================================

  /**
   * Get display name from FHIR Patient resource
   */
  getPatientDisplayName(patient: Patient): string {
    if (!patient?.name?.length) return 'Unknown Patient';
    const name = patient.name[0];
    if (name.text) return name.text;
    const given = name.given?.join(' ') || '';
    const family = name.family || '';
    return `${given} ${family}`.trim() || 'Unknown Patient';
  }

  /**
   * Get MRN from FHIR Patient resource
   */
  getPatientMRN(patient: Patient): string {
    const mrn = patient?.identifier?.find(
      (id) => id.type?.coding?.[0]?.code === 'MR' || id.system?.toLowerCase().includes('mrn')
    );
    return mrn?.value ?? patient?.identifier?.[0]?.value ?? patient?.id ?? 'No MRN';
  }

  /**
   * Calculate patient age from birthDate
   */
  getPatientAge(patient: Patient): number {
    if (!patient?.birthDate) return 0;
    const birthDate = new Date(patient.birthDate);
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }
    return age;
  }

  /**
   * Test CQL against the context patient from navigation
   */
  testContextPatient(): void {
    if (!this.data.contextPatient || !this.cqlText.trim()) return;

    this.isTestingContextPatient = true;
    this.contextPatientResult = null;

    this.livePreviewService.evaluateSpecificPatient(
      this.cqlText,
      this.data.contextPatient.id
    ).pipe(takeUntil(this.destroy$)).subscribe({
      next: (result) => {
        const patientResult = result.results[0];
        this.contextPatientResult = {
          outcome: patientResult?.outcome ?? 'unknown',
          message: this.getOutcomeMessage(patientResult?.outcome ?? '', patientResult?.matchedCriteria),
        };
        this.isTestingContextPatient = false;
      },
      error: (err) => {
        this.contextPatientResult = {
          outcome: 'error',
          message: err?.message || 'Failed to evaluate patient',
        };
        this.isTestingContextPatient = false;
      },
    });
  }

  /**
   * Generate outcome message based on result
   */
  private getOutcomeMessage(outcome: string, matchedCriteria?: MatchedCriterion[]): string {
    const matchedCount = matchedCriteria?.filter(c => c.matched).length ?? 0;
    const totalCount = matchedCriteria?.length ?? 0;

    switch (outcome) {
      case 'pass':
        return `Patient meets all ${totalCount} criteria for this measure.`;
      case 'fail':
        return `Patient meets ${matchedCount}/${totalCount} criteria. Does not qualify for numerator.`;
      case 'not-eligible':
        return `Patient does not meet initial population criteria.`;
      case 'excluded':
        return `Patient is excluded from this measure.`;
      case 'error':
        return 'Error evaluating CQL against this patient.';
      default:
        return 'Unable to determine outcome.';
    }
  }

  /**
   * Clear context patient (switch to sample patients mode)
   */
  clearContextPatient(): void {
    // We can't actually remove the context patient from data,
    // but we can clear the result and focus on sample patients
    this.contextPatientResult = null;
    this.data.contextPatient = undefined;
  }

  /**
   * Clear context patient result
   */
  clearContextPatientResult(): void {
    this.contextPatientResult = null;
  }
}
