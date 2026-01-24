import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subject, takeUntil, forkJoin } from 'rxjs';

import { AgentBuilderService } from '../services/agent-builder.service';
import { AgentVersion } from '../models/agent.model';
import { ToastService } from '../../../services/toast.service';
import { LoggerService } from '../../../services/logger.service';

export interface VersionCompareDialogData {
  agentId: string;
  version1Id: string;
  version2Id: string;
  version1Label?: string;
  version2Label?: string;
}

interface ConfigDiff {
  field: string;
  label: string;
  oldValue: any;
  newValue: any;
  changed: boolean;
}

@Component({
  selector: 'app-version-compare-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatTabsModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="compare-dialog">
      <div class="dialog-header">
        <h2>
          <mat-icon>compare_arrows</mat-icon>
          Version Comparison
        </h2>
        <button mat-icon-button (click)="onClose()" aria-label="Close dialog">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <mat-divider></mat-divider>

      <div class="dialog-body">
        <!-- Loading State -->
        @if (loading) {
          <div class="loading-state">
            <mat-spinner diameter="40"></mat-spinner>
            <p>Loading version comparison...</p>
          </div>
        }

        <!-- Comparison Content -->
        @if (!loading && version1 && version2) {
          <!-- Version Headers -->
          <div class="version-headers">
            <div class="version-header old">
              <mat-chip class="version-chip">
                {{ data.version1Label || version1.versionNumber }}
              </mat-chip>
              <div class="version-meta">
                <span>{{ version1.createdBy }}</span>
                <span>{{ version1.createdAt | date: 'short' }}</span>
              </div>
            </div>

            <mat-icon class="compare-icon">compare_arrows</mat-icon>

            <div class="version-header new">
              <mat-chip class="version-chip">
                {{ data.version2Label || version2.versionNumber }}
              </mat-chip>
              <div class="version-meta">
                <span>{{ version2.createdBy }}</span>
                <span>{{ version2.createdAt | date: 'short' }}</span>
              </div>
            </div>
          </div>

          <mat-divider></mat-divider>

          <!-- Changes Summary -->
          <div class="changes-summary">
            <mat-chip [class]="changesCount > 0 ? 'changes-badge' : 'no-changes-badge'">
              <mat-icon>{{ changesCount > 0 ? 'warning' : 'check_circle' }}</mat-icon>
              {{ changesCount }} {{ changesCount === 1 ? 'change' : 'changes' }} detected
            </mat-chip>
          </div>

          <!-- Tabs for Different Sections -->
          <mat-tab-group class="compare-tabs">
            <!-- Basic Info Tab -->
            <mat-tab label="Basic Info">
              <div class="tab-content">
                <div class="diff-table">
                  @for (diff of basicInfoDiffs; track diff.field) {
                    <div class="diff-row" [class.changed]="diff.changed">
                      <div class="diff-label">{{ diff.label }}</div>
                      <div class="diff-values">
                        <div class="diff-value old">
                          @if (diff.changed) {
                            <mat-icon class="change-indicator">remove</mat-icon>
                          }
                          <span>{{ formatValue(diff.oldValue) }}</span>
                        </div>
                        <div class="diff-value new">
                          @if (diff.changed) {
                            <mat-icon class="change-indicator">add</mat-icon>
                          }
                          <span>{{ formatValue(diff.newValue) }}</span>
                        </div>
                      </div>
                    </div>
                  }
                </div>
              </div>
            </mat-tab>

            <!-- Model Config Tab -->
            <mat-tab label="Model">
              <div class="tab-content">
                <div class="diff-table">
                  @for (diff of modelConfigDiffs; track diff.field) {
                    <div class="diff-row" [class.changed]="diff.changed">
                      <div class="diff-label">{{ diff.label }}</div>
                      <div class="diff-values">
                        <div class="diff-value old">
                          @if (diff.changed) {
                            <mat-icon class="change-indicator">remove</mat-icon>
                          }
                          <span>{{ formatValue(diff.oldValue) }}</span>
                        </div>
                        <div class="diff-value new">
                          @if (diff.changed) {
                            <mat-icon class="change-indicator">add</mat-icon>
                          }
                          <span>{{ formatValue(diff.newValue) }}</span>
                        </div>
                      </div>
                    </div>
                  }
                </div>
              </div>
            </mat-tab>

            <!-- Prompts Tab -->
            <mat-tab label="Prompts">
              <div class="tab-content">
                <div class="prompt-diff">
                  <h3>System Prompt</h3>
                  <div class="side-by-side">
                    <div class="prompt-box old">
                      <pre>{{ getPromptFromSnapshot(version1) }}</pre>
                    </div>
                    <div class="prompt-box new">
                      <pre>{{ getPromptFromSnapshot(version2) }}</pre>
                    </div>
                  </div>
                </div>
              </div>
            </mat-tab>

            <!-- Tools Tab -->
            <mat-tab label="Tools">
              <div class="tab-content">
                <div class="tools-diff">
                  @if (getToolsChangeSummary()) {
                    <div class="change-summary">{{ getToolsChangeSummary() }}</div>
                  }
                  <div class="side-by-side">
                    <div class="tools-box old">
                      <h4>Previous Tools</h4>
                      @for (tool of getToolsFromSnapshot(version1); track tool) {
                        <mat-chip size="small">{{ tool }}</mat-chip>
                      }
                    </div>
                    <div class="tools-box new">
                      <h4>Current Tools</h4>
                      @for (tool of getToolsFromSnapshot(version2); track tool) {
                        <mat-chip size="small">{{ tool }}</mat-chip>
                      }
                    </div>
                  </div>
                </div>
              </div>
            </mat-tab>

            <!-- Guardrails Tab -->
            <mat-tab label="Guardrails">
              <div class="tab-content">
                <div class="diff-table">
                  @for (diff of guardrailDiffs; track diff.field) {
                    <div class="diff-row" [class.changed]="diff.changed">
                      <div class="diff-label">{{ diff.label }}</div>
                      <div class="diff-values">
                        <div class="diff-value old">
                          @if (diff.changed) {
                            <mat-icon class="change-indicator">remove</mat-icon>
                          }
                          <span>{{ formatValue(diff.oldValue) }}</span>
                        </div>
                        <div class="diff-value new">
                          @if (diff.changed) {
                            <mat-icon class="change-indicator">add</mat-icon>
                          }
                          <span>{{ formatValue(diff.newValue) }}</span>
                        </div>
                      </div>
                    </div>
                  }
                </div>
              </div>
            </mat-tab>
          </mat-tab-group>
        }
      </div>

      <mat-divider></mat-divider>

      <div class="dialog-footer">
        <button mat-button (click)="onClose()">Close</button>
      </div>
    </div>
  `,
  styles: [`
    .compare-dialog {
      display: flex;
      flex-direction: column;
      height: 80vh;
      width: 90vw;
      max-width: 1400px;
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
      flex: 1;
      display: flex;
      flex-direction: column;
      min-height: 0;
      padding: 0 24px;
    }

    .loading-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      gap: 16px;
      color: var(--mat-sys-on-surface-variant);
    }

    .version-headers {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px;
      gap: 16px;

      .version-header {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 8px;

        &.old {
          align-items: flex-start;
        }

        &.new {
          align-items: flex-end;
        }

        .version-chip {
          font-weight: 600;
        }

        .version-meta {
          display: flex;
          flex-direction: column;
          gap: 4px;
          font-size: 0.875rem;
          color: var(--mat-sys-on-surface-variant);
        }
      }

      .compare-icon {
        font-size: 32px;
        width: 32px;
        height: 32px;
        color: var(--mat-sys-primary);
      }
    }

    .changes-summary {
      padding: 8px 16px;
      text-align: center;

      .changes-badge {
        background: var(--mat-sys-tertiary-container);
        color: var(--mat-sys-on-tertiary-container);
        font-weight: 500;

        mat-icon {
          margin-right: 4px;
        }
      }

      .no-changes-badge {
        background: var(--mat-sys-tertiary-container);
        color: var(--mat-sys-on-tertiary-container);

        mat-icon {
          margin-right: 4px;
        }
      }
    }

    .compare-tabs {
      flex: 1;
      min-height: 0;

      ::ng-deep .mat-mdc-tab-body-wrapper {
        flex: 1;
        min-height: 0;
      }
    }

    .tab-content {
      padding: 16px;
      overflow-y: auto;
      max-height: 50vh;
    }

    .diff-table {
      display: flex;
      flex-direction: column;
      gap: 1px;
      background: var(--mat-sys-outline-variant);
      border-radius: 8px;
      overflow: hidden;

      .diff-row {
        display: flex;
        background: var(--mat-sys-surface);

        &.changed {
          background: var(--mat-sys-tertiary-container);
        }

        .diff-label {
          width: 200px;
          padding: 12px 16px;
          font-weight: 500;
          background: var(--mat-sys-surface-variant);
        }

        .diff-values {
          flex: 1;
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 1px;
          background: var(--mat-sys-outline-variant);

          .diff-value {
            padding: 12px 16px;
            display: flex;
            align-items: center;
            gap: 8px;
            background: var(--mat-sys-surface);

            &.old {
              background: rgba(239, 68, 68, 0.1);
            }

            &.new {
              background: rgba(34, 197, 94, 0.1);
            }

            .change-indicator {
              font-size: 18px;
              width: 18px;
              height: 18px;
            }
          }
        }
      }
    }

    .side-by-side {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;

      .prompt-box,
      .tools-box {
        padding: 12px;
        background: var(--mat-sys-surface-variant);
        border-radius: 8px;

        h4 {
          margin: 0 0 12px;
          font-size: 0.875rem;
          font-weight: 600;
          text-transform: uppercase;
          color: var(--mat-sys-on-surface-variant);
        }

        pre {
          margin: 0;
          white-space: pre-wrap;
          word-break: break-word;
          font-size: 0.875rem;
        }
      }

      .prompt-box {
        &.old {
          background: rgba(239, 68, 68, 0.05);
        }

        &.new {
          background: rgba(34, 197, 94, 0.05);
        }
      }

      .tools-box {
        display: flex;
        flex-direction: column;
        gap: 8px;

        mat-chip {
          width: fit-content;
        }
      }
    }

    .change-summary {
      padding: 8px 12px;
      margin-bottom: 16px;
      background: var(--mat-sys-tertiary-container);
      color: var(--mat-sys-on-tertiary-container);
      border-radius: 4px;
      font-size: 0.875rem;
    }

    .dialog-footer {
      display: flex;
      justify-content: flex-end;
      padding: 16px 24px;
    }
  `],
})
export class VersionCompareDialogComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private logger = this.loggerService.withContext('VersionCompareDialogComponent');

  version1: AgentVersion | null = null;
  version2: AgentVersion | null = null;
  loading = false;

  basicInfoDiffs: ConfigDiff[] = [];
  modelConfigDiffs: ConfigDiff[] = [];
  guardrailDiffs: ConfigDiff[] = [];
  changesCount = 0;

  constructor(
    private agentService: AgentBuilderService,
    private toast: ToastService,
    private loggerService: LoggerService,
    private dialogRef: MatDialogRef<VersionCompareDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: VersionCompareDialogData
  ) {}

  ngOnInit(): void {
    this.loadVersions();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadVersions(): void {
    this.loading = true;

    forkJoin({
      version1: this.agentService.getVersion(this.data.agentId, this.data.version1Id),
      version2: this.agentService.getVersion(this.data.agentId, this.data.version2Id),
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ version1, version2 }) => {
          this.version1 = version1;
          this.version2 = version2;
          this.computeDiffs();
          this.loading = false;
          this.logger.info('Versions loaded for comparison');
        },
        error: (err) => {
          this.loading = false;
          this.logger.error('Failed to load versions', err);
          this.toast.error('Failed to load versions for comparison');
        },
      });
  }

  private computeDiffs(): void {
    if (!this.version1 || !this.version2) return;

    const snap1 = this.version1.configurationSnapshot as any;
    const snap2 = this.version2.configurationSnapshot as any;

    // Basic Info Diffs
    this.basicInfoDiffs = [
      { field: 'name', label: 'Name', oldValue: snap1.name, newValue: snap2.name, changed: snap1.name !== snap2.name },
      { field: 'description', label: 'Description', oldValue: snap1.description, newValue: snap2.description, changed: snap1.description !== snap2.description },
      { field: 'personaName', label: 'Persona Name', oldValue: snap1.personaName, newValue: snap2.personaName, changed: snap1.personaName !== snap2.personaName },
      { field: 'personaRole', label: 'Persona Role', oldValue: snap1.personaRole, newValue: snap2.personaRole, changed: snap1.personaRole !== snap2.personaRole },
    ];

    // Model Config Diffs
    this.modelConfigDiffs = [
      { field: 'modelProvider', label: 'Provider', oldValue: snap1.modelProvider, newValue: snap2.modelProvider, changed: snap1.modelProvider !== snap2.modelProvider },
      { field: 'modelId', label: 'Model', oldValue: snap1.modelId, newValue: snap2.modelId, changed: snap1.modelId !== snap2.modelId },
      { field: 'temperature', label: 'Temperature', oldValue: snap1.temperature, newValue: snap2.temperature, changed: snap1.temperature !== snap2.temperature },
      { field: 'maxTokens', label: 'Max Tokens', oldValue: snap1.maxTokens, newValue: snap2.maxTokens, changed: snap1.maxTokens !== snap2.maxTokens },
    ];

    // Guardrail Diffs
    const guardrail1 = snap1.guardrailConfiguration || {};
    const guardrail2 = snap2.guardrailConfiguration || {};

    this.guardrailDiffs = [
      { field: 'phiFiltering', label: 'PHI Filtering', oldValue: guardrail1.phiFiltering, newValue: guardrail2.phiFiltering, changed: guardrail1.phiFiltering !== guardrail2.phiFiltering },
      { field: 'clinicalDisclaimer', label: 'Clinical Disclaimer', oldValue: guardrail1.clinicalDisclaimerRequired, newValue: guardrail2.clinicalDisclaimerRequired, changed: guardrail1.clinicalDisclaimerRequired !== guardrail2.clinicalDisclaimerRequired },
      { field: 'requireHumanReview', label: 'Human Review', oldValue: guardrail1.requireHumanReview, newValue: guardrail2.requireHumanReview, changed: guardrail1.requireHumanReview !== guardrail2.requireHumanReview },
      { field: 'riskThreshold', label: 'Risk Threshold', oldValue: guardrail1.riskThreshold, newValue: guardrail2.riskThreshold, changed: guardrail1.riskThreshold !== guardrail2.riskThreshold },
    ];

    // Count total changes
    this.changesCount = [
      ...this.basicInfoDiffs,
      ...this.modelConfigDiffs,
      ...this.guardrailDiffs,
    ].filter((d) => d.changed).length;

    // Check prompt changes
    if (snap1.systemPrompt !== snap2.systemPrompt) {
      this.changesCount++;
    }

    // Check tools changes
    if (JSON.stringify(snap1.toolConfiguration) !== JSON.stringify(snap2.toolConfiguration)) {
      this.changesCount++;
    }
  }

  formatValue(value: any): string {
    if (value === null || value === undefined) return '(not set)';
    if (typeof value === 'boolean') return value ? 'Yes' : 'No';
    if (typeof value === 'string' && value === '') return '(empty)';
    return String(value);
  }

  getPromptFromSnapshot(version: AgentVersion): string {
    const snap = version.configurationSnapshot as any;
    return snap.systemPrompt || '(no prompt)';
  }

  getToolsFromSnapshot(version: AgentVersion): string[] {
    const snap = version.configurationSnapshot as any;
    const tools = snap.toolConfiguration || [];
    return tools.filter((t: any) => t.enabled).map((t: any) => t.toolName);
  }

  getToolsChangeSummary(): string {
    if (!this.version1 || !this.version2) return '';

    const tools1 = new Set(this.getToolsFromSnapshot(this.version1));
    const tools2 = new Set(this.getToolsFromSnapshot(this.version2));

    const added = [...tools2].filter((t) => !tools1.has(t));
    const removed = [...tools1].filter((t) => !tools2.has(t));

    if (added.length === 0 && removed.length === 0) return '';

    const parts: string[] = [];
    if (added.length > 0) parts.push(`Added: ${added.join(', ')}`);
    if (removed.length > 0) parts.push(`Removed: ${removed.join(', ')}`);

    return parts.join(' | ');
  }

  onClose(): void {
    this.dialogRef.close();
  }
}
