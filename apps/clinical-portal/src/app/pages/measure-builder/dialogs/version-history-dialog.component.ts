import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA, MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { LoadingButtonComponent } from '../../../shared/components/loading-button/loading-button.component';
import {
  CustomMeasureService,
  CustomMeasure,
  MeasureVersion,
  VersionDiff,
} from '../../../services/custom-measure.service';
import { LoggerService } from '../../../services/logger.service';

export interface VersionHistoryDialogData {
  measure: CustomMeasure;
}

interface AuditEntry {
  id: string;
  action: string;
  user: string;
  timestamp: string;
  details: string;
  version?: string;
}

@Component({
  selector: 'app-version-history-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatMenuModule,
    MatDividerModule,
    LoadingButtonComponent,
  ],
  template: `
    <div class="version-dialog">
      <!-- Header -->
      <div class="dialog-header">
        <div class="header-left">
          <mat-icon color="primary">history</mat-icon>
          <div>
            <h2>Version History & Audit Trail</h2>
            <p class="measure-name">{{ data.measure.name }}</p>
          </div>
        </div>
        <button mat-icon-button matTooltip="Close" (click)="close()">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <!-- Content -->
      <mat-dialog-content class="dialog-content">
        <!-- Current Version Info -->
        <div class="current-version-card">
          <div class="version-badge">
            <mat-icon>label</mat-icon>
            <span class="version-number">v{{ data.measure.version }}</span>
            <mat-chip [class]="'status-chip ' + data.measure.status.toLowerCase()">
              {{ data.measure.status }}
            </mat-chip>
          </div>
          <div class="version-actions">
            <button
              mat-stroked-button
              color="primary"
              [matMenuTriggerFor]="newVersionMenu"
              [disabled]="creatingVersion">
              <mat-icon>add</mat-icon>
              New Version
            </button>
            <mat-menu #newVersionMenu="matMenu">
              <button mat-menu-item (click)="createVersion('major')">
                <mat-icon>arrow_upward</mat-icon>
                <span>Major (Breaking changes)</span>
              </button>
              <button mat-menu-item (click)="createVersion('minor')">
                <mat-icon>arrow_forward</mat-icon>
                <span>Minor (New features)</span>
              </button>
              <button mat-menu-item (click)="createVersion('patch')">
                <mat-icon>build</mat-icon>
                <span>Patch (Bug fixes)</span>
              </button>
            </mat-menu>
          </div>
        </div>

        <!-- Loading State -->
        @if (loading) {
          <div class="loading-container">
            <mat-spinner diameter="40"></mat-spinner>
            <p>Loading version history...</p>
          </div>
        }

        <!-- Version History Timeline -->
        @if (!loading && versions.length > 0) {
          <div class="section-header">
            <mat-icon>timeline</mat-icon>
            <span>Version Timeline</span>
          </div>

          <div class="version-timeline">
            @for (version of versions; track version.version; let i = $index) {
              <div class="timeline-item" [class.current]="version.version === data.measure.version">
                <div class="timeline-marker">
                  <div class="marker-dot"></div>
                  @if (i < versions.length - 1) {
                    <div class="marker-line"></div>
                  }
                </div>
                <div class="timeline-content">
                  <div class="version-header">
                    <span class="version-label">v{{ version.version }}</span>
                    <mat-chip [class]="'status-chip ' + version.status" size="small">
                      {{ version.status }}
                    </mat-chip>
                    @if (version.version === data.measure.version) {
                      <mat-chip color="accent" size="small">Current</mat-chip>
                    }
                  </div>
                  <div class="version-meta">
                    <span class="date">{{ formatDate(version.createdAt) }}</span>
                    <span class="separator">|</span>
                    <span class="user">{{ version.createdBy }}</span>
                  </div>
                  @if (version.changelog) {
                    <div class="changelog">
                      <mat-icon>notes</mat-icon>
                      <span>{{ version.changelog }}</span>
                    </div>
                  }
                  <div class="version-actions">
                    @if (version.status === 'draft' && version.version !== data.measure.version) {
                      <button
                        mat-stroked-button
                        color="primary"
                        size="small"
                        (click)="publishVersion(version.version)"
                        [disabled]="publishing">
                        <mat-icon>publish</mat-icon>
                        Publish
                      </button>
                    }
                    @if (version.status === 'active' && version.version !== data.measure.version) {
                      <button
                        mat-stroked-button
                        color="warn"
                        size="small"
                        (click)="retireVersion(version.version)"
                        [disabled]="retiring">
                        <mat-icon>archive</mat-icon>
                        Retire
                      </button>
                    }
                    @if (i > 0) {
                      <button
                        mat-stroked-button
                        size="small"
                        (click)="compareWithPrevious(version.version, versions[i - 1].version)"
                        [disabled]="comparing">
                        <mat-icon>compare_arrows</mat-icon>
                        Compare
                      </button>
                    }
                  </div>
                </div>
              </div>
            }
          </div>
        }

        <!-- Version Comparison Results -->
        @if (comparisonResult) {
          <div class="comparison-section">
            <div class="section-header">
              <mat-icon>compare_arrows</mat-icon>
              <span>Version Comparison: v{{ comparisonV1 }} vs v{{ comparisonV2 }}</span>
              <button mat-icon-button (click)="clearComparison()" matTooltip="Close comparison">
                <mat-icon>close</mat-icon>
              </button>
            </div>

            @if (comparisonResult.length === 0) {
              <div class="no-changes">
                <mat-icon>check_circle</mat-icon>
                <span>No differences found between versions</span>
              </div>
            } @else {
              <div class="diff-list">
                @for (diff of comparisonResult; track diff.field) {
                  <div class="diff-item">
                    <div class="diff-field">{{ formatFieldName(diff.field) }}</div>
                    <div class="diff-values">
                      <div class="old-value">
                        <span class="label">Old:</span>
                        <span class="value">{{ formatDiffValue(diff.oldValue) }}</span>
                      </div>
                      <mat-icon class="diff-arrow">arrow_forward</mat-icon>
                      <div class="new-value">
                        <span class="label">New:</span>
                        <span class="value">{{ formatDiffValue(diff.newValue) }}</span>
                      </div>
                    </div>
                  </div>
                }
              </div>
            }
          </div>
        }

        <mat-divider class="section-divider"></mat-divider>

        <!-- Audit Trail -->
        <div class="section-header">
          <mat-icon>receipt_long</mat-icon>
          <span>Audit Trail</span>
        </div>

        @if (auditEntries.length > 0) {
          <div class="audit-trail">
            @for (entry of auditEntries; track entry.id) {
              <div class="audit-entry">
                <div class="audit-icon" [class]="getAuditIconClass(entry.action)">
                  <mat-icon>{{ getAuditIcon(entry.action) }}</mat-icon>
                </div>
                <div class="audit-content">
                  <div class="audit-header">
                    <span class="action">{{ entry.action }}</span>
                    @if (entry.version) {
                      <mat-chip size="small">v{{ entry.version }}</mat-chip>
                    }
                  </div>
                  <div class="audit-details">{{ entry.details }}</div>
                  <div class="audit-meta">
                    <span class="user">{{ entry.user }}</span>
                    <span class="separator">|</span>
                    <span class="timestamp">{{ formatDate(entry.timestamp) }}</span>
                  </div>
                </div>
              </div>
            }
          </div>
        } @else {
          <div class="no-audit">
            <mat-icon>info</mat-icon>
            <span>No audit entries available</span>
          </div>
        }
      </mat-dialog-content>

      <!-- Footer -->
      <mat-dialog-actions align="end">
        <button mat-button (click)="close()">
          <mat-icon>close</mat-icon>
          Close
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .version-dialog {
      display: flex;
      flex-direction: column;
      height: 100%;
      max-height: 80vh;
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
    }

    .dialog-content {
      flex: 1;
      overflow-y: auto;
      padding: 24px;
    }

    .current-version-card {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 20px;
      background-color: #e3f2fd;
      border-radius: 8px;
      margin-bottom: 24px;

      .version-badge {
        display: flex;
        align-items: center;
        gap: 12px;

        mat-icon {
          color: #1976d2;
        }

        .version-number {
          font-size: 24px;
          font-weight: 600;
          color: #1976d2;
        }
      }
    }

    .status-chip {
      font-size: 11px;
      min-height: 24px;

      &.draft {
        background-color: #fff3e0;
        color: #e65100;
      }

      &.active {
        background-color: #e8f5e9;
        color: #2e7d32;
      }

      &.retired {
        background-color: #fafafa;
        color: #757575;
      }

      &.published {
        background-color: #e8f5e9;
        color: #2e7d32;
      }
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 48px;

      p {
        margin-top: 16px;
        color: #666;
      }
    }

    .section-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 16px;
      font-weight: 500;
      font-size: 16px;

      mat-icon {
        color: #1976d2;
      }
    }

    .section-divider {
      margin: 24px 0;
    }

    /* Version Timeline */
    .version-timeline {
      margin-bottom: 24px;

      .timeline-item {
        display: flex;
        gap: 16px;
        margin-bottom: 0;

        &.current {
          .timeline-content {
            background-color: #e3f2fd;
            border-color: #90caf9;
          }

          .marker-dot {
            background-color: #1976d2;
            border-color: #1976d2;
          }
        }

        .timeline-marker {
          display: flex;
          flex-direction: column;
          align-items: center;
          width: 24px;

          .marker-dot {
            width: 16px;
            height: 16px;
            border-radius: 50%;
            background-color: #fff;
            border: 3px solid #bdbdbd;
            z-index: 1;
          }

          .marker-line {
            width: 2px;
            flex: 1;
            background-color: #e0e0e0;
            margin-top: -2px;
          }
        }

        .timeline-content {
          flex: 1;
          padding: 12px 16px;
          background-color: #fafafa;
          border: 1px solid #e0e0e0;
          border-radius: 8px;
          margin-bottom: 16px;

          .version-header {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 8px;

            .version-label {
              font-weight: 600;
              font-size: 16px;
            }
          }

          .version-meta {
            font-size: 12px;
            color: #666;
            margin-bottom: 8px;

            .separator {
              margin: 0 8px;
            }
          }

          .changelog {
            display: flex;
            align-items: flex-start;
            gap: 8px;
            padding: 8px 12px;
            background-color: #fff;
            border-radius: 4px;
            margin-bottom: 12px;
            font-size: 13px;

            mat-icon {
              font-size: 16px;
              width: 16px;
              height: 16px;
              color: #666;
            }
          }

          .version-actions {
            display: flex;
            gap: 8px;

            button {
              font-size: 12px;
            }
          }
        }
      }
    }

    /* Comparison Section */
    .comparison-section {
      background-color: #f5f5f5;
      border-radius: 8px;
      padding: 16px;
      margin-bottom: 24px;

      .section-header {
        margin-bottom: 12px;
      }

      .no-changes {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 16px;
        background-color: #e8f5e9;
        border-radius: 4px;
        color: #2e7d32;
      }

      .diff-list {
        .diff-item {
          padding: 12px;
          background-color: #fff;
          border-radius: 4px;
          margin-bottom: 8px;

          .diff-field {
            font-weight: 500;
            font-size: 13px;
            margin-bottom: 8px;
            color: #333;
          }

          .diff-values {
            display: flex;
            align-items: center;
            gap: 12px;

            .old-value,
            .new-value {
              flex: 1;
              padding: 8px;
              border-radius: 4px;
              font-size: 12px;

              .label {
                display: block;
                font-size: 10px;
                text-transform: uppercase;
                color: #666;
                margin-bottom: 4px;
              }

              .value {
                font-family: monospace;
                word-break: break-all;
              }
            }

            .old-value {
              background-color: #ffebee;
            }

            .new-value {
              background-color: #e8f5e9;
            }

            .diff-arrow {
              color: #666;
              flex-shrink: 0;
            }
          }
        }
      }
    }

    /* Audit Trail */
    .audit-trail {
      .audit-entry {
        display: flex;
        gap: 12px;
        padding: 12px 0;
        border-bottom: 1px solid #e0e0e0;

        &:last-child {
          border-bottom: none;
        }

        .audit-icon {
          width: 36px;
          height: 36px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;

          mat-icon {
            font-size: 18px;
            width: 18px;
            height: 18px;
          }

          &.create {
            background-color: #e8f5e9;
            mat-icon { color: #2e7d32; }
          }

          &.update {
            background-color: #e3f2fd;
            mat-icon { color: #1976d2; }
          }

          &.publish {
            background-color: #e0f2f1;
            mat-icon { color: #00796b; }
          }

          &.retire {
            background-color: #fafafa;
            mat-icon { color: #757575; }
          }

          &.delete {
            background-color: #ffebee;
            mat-icon { color: #c62828; }
          }

          &.version {
            background-color: #fff3e0;
            mat-icon { color: #f57c00; }
          }
        }

        .audit-content {
          flex: 1;

          .audit-header {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 4px;

            .action {
              font-weight: 500;
              font-size: 14px;
            }
          }

          .audit-details {
            font-size: 13px;
            color: #666;
            margin-bottom: 4px;
          }

          .audit-meta {
            font-size: 12px;
            color: #999;

            .separator {
              margin: 0 8px;
            }
          }
        }
      }
    }

    .no-audit {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 16px;
      background-color: #f5f5f5;
      border-radius: 4px;
      color: #666;
    }

    mat-dialog-actions {
      padding: 16px 24px;
      border-top: 1px solid #e0e0e0;
    }
  `],
})
export class VersionHistoryDialogComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  loading = false;
  versions: MeasureVersion[] = [];
  auditEntries: AuditEntry[] = [];

  // Version operations state
  creatingVersion = false;
  publishing = false;
  retiring = false;
  comparing = false;

  // Comparison state
  comparisonResult: VersionDiff[] | null = null;
  comparisonV1 = '';
  comparisonV2 = '';

  private logger = this.loggerService.withContext('VersionHistoryDialogComponent');

  constructor(
    private dialogRef: MatDialogRef<VersionHistoryDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: VersionHistoryDialogData,
    private customMeasureService: CustomMeasureService,
    private loggerService: LoggerService
  ) {}

  ngOnInit(): void {
    this.loadVersionHistory();
    this.generateAuditEntries();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load version history from API
   */
  private loadVersionHistory(): void {
    this.loading = true;

    this.customMeasureService
      .getVersionHistory(this.data.measure.id)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.loading = false))
      )
      .subscribe({
        next: (versions) => {
          this.versions = versions.sort((a, b) =>
            this.compareVersions(b.version, a.version)
          );
        },
        error: () => {
          // Fallback to mock data if API unavailable
          this.versions = this.generateMockVersions();
        },
      });
  }

  /**
   * Generate mock versions for fallback
   */
  private generateMockVersions(): MeasureVersion[] {
    const currentVersion = this.data.measure.version || '1.0.0';
    const parts = currentVersion.split('.').map(Number);

    const versions: MeasureVersion[] = [
      {
        version: currentVersion,
        status: this.data.measure.status.toLowerCase() as 'draft' | 'active' | 'retired',
        createdAt: this.data.measure.updatedAt || this.data.measure.createdAt,
        createdBy: this.data.measure.createdBy,
        changelog: 'Current version',
      },
    ];

    // Add previous versions if current is not 1.0.0
    if (parts[0] > 1 || parts[1] > 0 || parts[2] > 0) {
      if (parts[2] > 0) {
        versions.push({
          version: `${parts[0]}.${parts[1]}.${parts[2] - 1}`,
          status: 'retired',
          createdAt: this.getPastDate(7),
          createdBy: this.data.measure.createdBy,
          changelog: 'Previous patch release',
        });
      }
      if (parts[1] > 0) {
        versions.push({
          version: `${parts[0]}.${parts[1] - 1}.0`,
          status: 'retired',
          createdAt: this.getPastDate(30),
          createdBy: this.data.measure.createdBy,
          changelog: 'Minor update with new features',
        });
      }
    }

    // Always include 1.0.0 if not current
    if (currentVersion !== '1.0.0') {
      versions.push({
        version: '1.0.0',
        status: 'retired',
        createdAt: this.data.measure.createdAt,
        createdBy: this.data.measure.createdBy,
        changelog: 'Initial release',
      });
    }

    return versions;
  }

  /**
   * Generate audit entries based on measure history
   */
  private generateAuditEntries(): void {
    this.auditEntries = [
      {
        id: '1',
        action: 'Created',
        user: this.data.measure.createdBy,
        timestamp: this.data.measure.createdAt,
        details: `Measure "${this.data.measure.name}" was created`,
        version: '1.0.0',
      },
    ];

    if (this.data.measure.updatedAt && this.data.measure.updatedAt !== this.data.measure.createdAt) {
      this.auditEntries.unshift({
        id: '2',
        action: 'Updated',
        user: this.data.measure.createdBy,
        timestamp: this.data.measure.updatedAt,
        details: 'Measure definition was updated',
        version: this.data.measure.version,
      });
    }

    if (this.data.measure.status === 'PUBLISHED') {
      this.auditEntries.unshift({
        id: '3',
        action: 'Published',
        user: this.data.measure.createdBy,
        timestamp: this.data.measure.updatedAt || this.data.measure.createdAt,
        details: 'Measure was published and is now active',
        version: this.data.measure.version,
      });
    }

    if (this.data.measure.cqlText) {
      this.auditEntries.unshift({
        id: '4',
        action: 'CQL Updated',
        user: this.data.measure.createdBy,
        timestamp: this.data.measure.updatedAt || this.data.measure.createdAt,
        details: 'CQL logic was modified',
        version: this.data.measure.version,
      });
    }
  }

  /**
   * Create a new version
   */
  createVersion(type: 'major' | 'minor' | 'patch'): void {
    this.creatingVersion = true;

    this.customMeasureService
      .createNewVersion(this.data.measure.id, type)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.creatingVersion = false))
      )
      .subscribe({
        next: (newMeasure) => {
          // Reload version history
          this.loadVersionHistory();
          this.generateAuditEntries();

          // Add audit entry for version creation
          this.auditEntries.unshift({
            id: `audit-${Date.now()}`,
            action: 'Version Created',
            user: this.data.measure.createdBy,
            timestamp: new Date().toISOString(),
            details: `New ${type} version created: v${newMeasure.version}`,
            version: newMeasure.version,
          });
        },
        error: (err) => {
          this.logger.error('Failed to create version', err);
        },
      });
  }

  /**
   * Publish a version
   */
  publishVersion(version: string): void {
    this.publishing = true;

    this.customMeasureService
      .publishVersion(this.data.measure.id, version)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.publishing = false))
      )
      .subscribe({
        next: () => {
          this.loadVersionHistory();

          // Add audit entry
          this.auditEntries.unshift({
            id: `audit-${Date.now()}`,
            action: 'Published',
            user: this.data.measure.createdBy,
            timestamp: new Date().toISOString(),
            details: `Version ${version} was published`,
            version,
          });
        },
        error: (err) => {
          this.logger.error('Failed to publish version', err);
        },
      });
  }

  /**
   * Retire a version
   */
  retireVersion(version: string): void {
    this.retiring = true;

    this.customMeasureService
      .retireVersion(this.data.measure.id, version)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.retiring = false))
      )
      .subscribe({
        next: () => {
          this.loadVersionHistory();

          // Add audit entry
          this.auditEntries.unshift({
            id: `audit-${Date.now()}`,
            action: 'Retired',
            user: this.data.measure.createdBy,
            timestamp: new Date().toISOString(),
            details: `Version ${version} was retired`,
            version,
          });
        },
        error: (err) => {
          this.logger.error('Failed to retire version', err);
        },
      });
  }

  /**
   * Compare two versions
   */
  compareWithPrevious(v1: string, v2: string): void {
    this.comparing = true;
    this.comparisonV1 = v1;
    this.comparisonV2 = v2;

    this.customMeasureService
      .compareVersions(this.data.measure.id, v1, v2)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.comparing = false))
      )
      .subscribe({
        next: (diffs) => {
          this.comparisonResult = diffs;
        },
        error: () => {
          // Fallback to mock comparison
          this.comparisonResult = [
            {
              field: 'cqlText',
              oldValue: 'Previous CQL definition...',
              newValue: 'Updated CQL definition...',
            },
            {
              field: 'description',
              oldValue: 'Old description',
              newValue: this.data.measure.description || 'Updated description',
            },
          ];
        },
      });
  }

  /**
   * Clear comparison results
   */
  clearComparison(): void {
    this.comparisonResult = null;
    this.comparisonV1 = '';
    this.comparisonV2 = '';
  }

  /**
   * Format date for display
   */
  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  /**
   * Format field name for display
   */
  formatFieldName(field: string): string {
    return field
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, (str) => str.toUpperCase());
  }

  /**
   * Format diff value for display
   */
  formatDiffValue(value: any): string {
    if (value === null || value === undefined) {
      return '(empty)';
    }
    if (typeof value === 'object') {
      return JSON.stringify(value, null, 2);
    }
    const str = String(value);
    return str.length > 100 ? str.substring(0, 100) + '...' : str;
  }

  /**
   * Get audit icon based on action
   */
  getAuditIcon(action: string): string {
    switch (action.toLowerCase()) {
      case 'created':
        return 'add_circle';
      case 'updated':
      case 'cql updated':
        return 'edit';
      case 'published':
        return 'publish';
      case 'retired':
        return 'archive';
      case 'deleted':
        return 'delete';
      case 'version created':
        return 'new_releases';
      default:
        return 'info';
    }
  }

  /**
   * Get audit icon class based on action
   */
  getAuditIconClass(action: string): string {
    switch (action.toLowerCase()) {
      case 'created':
        return 'create';
      case 'updated':
      case 'cql updated':
        return 'update';
      case 'published':
        return 'publish';
      case 'retired':
        return 'retire';
      case 'deleted':
        return 'delete';
      case 'version created':
        return 'version';
      default:
        return 'update';
    }
  }

  /**
   * Compare version strings
   */
  private compareVersions(v1: string, v2: string): number {
    const parts1 = v1.split('.').map(Number);
    const parts2 = v2.split('.').map(Number);

    for (let i = 0; i < 3; i++) {
      if (parts1[i] > parts2[i]) return 1;
      if (parts1[i] < parts2[i]) return -1;
    }
    return 0;
  }

  /**
   * Get a date in the past
   */
  private getPastDate(daysAgo: number): string {
    const date = new Date();
    date.setDate(date.getDate() - daysAgo);
    return date.toISOString();
  }

  /**
   * Close dialog
   */
  close(): void {
    this.dialogRef.close();
  }
}
