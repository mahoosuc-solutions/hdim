import { Component, Inject, OnInit, OnDestroy, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subject, takeUntil } from 'rxjs';

import { AgentBuilderService } from '../services/agent-builder.service';
import { AgentConfiguration, AgentVersion, VersionStatus } from '../models/agent.model';
import { ToastService } from '../../../services/toast.service';
import { LoggerService } from '../../../services/logger.service';
import { ConfirmationDialogComponent } from '../../../components/dialogs/confirmation-dialog.component';
import { VersionCompareDialogComponent } from './version-compare-dialog.component';

export interface AgentVersionsDialogData {
  agent: AgentConfiguration;
}

@Component({
  selector: 'app-agent-versions-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatChipsModule,
    MatDividerModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="versions-dialog">
      <div class="dialog-header">
        <div class="header-info">
          <h2>
            <mat-icon>history</mat-icon>
            Version History
          </h2>
          <p class="subtitle">{{ data.agent.name }}</p>
        </div>
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
            <p>Loading version history...</p>
          </div>
        }

        <!-- Versions Table -->
        @if (!loading && dataSource.data.length > 0) {
          <div class="table-container">
            <table mat-table [dataSource]="dataSource" matSort class="versions-table">
              <!-- Version Number Column -->
              <ng-container matColumnDef="versionNumber">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Version</th>
                <td mat-cell *matCellDef="let version">
                  <div class="version-cell">
                    <strong>{{ version.versionNumber }}</strong>
                    @if (version.id === data.agent.version) {
                      <mat-chip size="small" class="current-badge">Current</mat-chip>
                    }
                  </div>
                </td>
              </ng-container>

              <!-- Status Column -->
              <ng-container matColumnDef="status">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Status</th>
                <td mat-cell *matCellDef="let version">
                  <mat-chip
                    size="small"
                    [class]="'status-chip-' + version.status.toLowerCase()">
                    {{ formatStatus(version.status) }}
                  </mat-chip>
                </td>
              </ng-container>

              <!-- Change Type Column -->
              <ng-container matColumnDef="changeType">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Type</th>
                <td mat-cell *matCellDef="let version">
                  <mat-chip
                    size="small"
                    [class]="'change-type-' + version.changeType.toLowerCase()">
                    {{ version.changeType }}
                  </mat-chip>
                </td>
              </ng-container>

              <!-- Change Summary Column -->
              <ng-container matColumnDef="changeSummary">
                <th mat-header-cell *matHeaderCellDef>Changes</th>
                <td mat-cell *matCellDef="let version">
                  <span class="change-summary">
                    {{ version.changeSummary || 'No description provided' }}
                  </span>
                </td>
              </ng-container>

              <!-- Created By Column -->
              <ng-container matColumnDef="createdBy">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Created By</th>
                <td mat-cell *matCellDef="let version">
                  <div class="user-cell">
                    <mat-icon>person</mat-icon>
                    {{ version.createdBy }}
                  </div>
                </td>
              </ng-container>

              <!-- Created At Column -->
              <ng-container matColumnDef="createdAt">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Date</th>
                <td mat-cell *matCellDef="let version">
                  {{ version.createdAt | date: 'short' }}
                </td>
              </ng-container>

              <!-- Actions Column -->
              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>Actions</th>
                <td mat-cell *matCellDef="let version">
                  <div class="action-buttons">
                    <button
                      mat-icon-button
                      (click)="viewVersion(version)"
                      matTooltip="View details"
                      [attr.aria-label]="'View version ' + version.versionNumber + ' details'">
                      <mat-icon>visibility</mat-icon>
                    </button>
                    @if (versions.length > 1) {
                      <button
                        mat-icon-button
                        (click)="compareVersions(version)"
                        matTooltip="Compare with current"
                        [attr.aria-label]="'Compare version ' + version.versionNumber + ' with current version'">
                        <mat-icon>compare_arrows</mat-icon>
                      </button>
                    }
                    @if (version.id !== data.agent.version) {
                      <button
                        mat-icon-button
                        (click)="rollbackToVersion(version)"
                        matTooltip="Rollback to this version"
                        [attr.aria-label]="'Rollback to version ' + version.versionNumber"
                        [disabled]="rollingBack">
                        <mat-icon>restore</mat-icon>
                      </button>
                    }
                  </div>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr
                mat-row
                *matRowDef="let row; columns: displayedColumns"
                class="version-row">
              </tr>
            </table>
          </div>

          <mat-paginator
            [pageSizeOptions]="[10, 25, 50]"
            [pageSize]="10"
            showFirstLastButtons
            aria-label="Select page of versions">
          </mat-paginator>
        }

        <!-- Empty State -->
        @if (!loading && dataSource.data.length === 0) {
          <div class="empty-state">
            <mat-icon>history</mat-icon>
            <h3>No Version History</h3>
            <p>This agent has no saved versions yet</p>
          </div>
        }
      </div>

      <mat-divider></mat-divider>

      <div class="dialog-footer">
        <div class="footer-info">
          <span>Total Versions: {{ versions.length }}</span>
          <span>Current: v{{ data.agent.version }}</span>
        </div>
        <button mat-button (click)="onClose()">Close</button>
      </div>
    </div>
  `,
  styles: [`
    .versions-dialog {
      display: flex;
      flex-direction: column;
      height: 80vh;
      width: 90vw;
      max-width: 1200px;
    }

    .dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      padding: 16px 24px;

      .header-info {
        h2 {
          display: flex;
          align-items: center;
          gap: 8px;
          margin: 0;
        }

        .subtitle {
          margin: 4px 0 0;
          color: var(--mat-sys-on-surface-variant);
        }
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

    .table-container {
      flex: 1;
      overflow: auto;
      border: 1px solid var(--mat-sys-outline-variant);
      border-radius: 8px;
      margin: 16px 0;
    }

    .versions-table {
      width: 100%;

      .version-cell {
        display: flex;
        align-items: center;
        gap: 8px;

        .current-badge {
          background: var(--mat-sys-primary-container);
          color: var(--mat-sys-on-primary-container);
        }
      }

      .status-chip-published {
        background: var(--mat-sys-tertiary-container);
        color: var(--mat-sys-on-tertiary-container);
      }

      .status-chip-draft {
        background: var(--mat-sys-secondary-container);
        color: var(--mat-sys-on-secondary-container);
      }

      .status-chip-rolled_back {
        background: var(--mat-sys-error-container);
        color: var(--mat-sys-on-error-container);
      }

      .status-chip-superseded {
        background: var(--mat-sys-surface-variant);
        color: var(--mat-sys-on-surface-variant);
      }

      .change-type-major {
        background: var(--mat-sys-error-container);
        color: var(--mat-sys-on-error-container);
      }

      .change-type-minor {
        background: var(--mat-sys-tertiary-container);
        color: var(--mat-sys-on-tertiary-container);
      }

      .change-type-patch {
        background: var(--mat-sys-secondary-container);
        color: var(--mat-sys-on-secondary-container);
      }

      .change-summary {
        display: block;
        max-width: 300px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .user-cell {
        display: flex;
        align-items: center;
        gap: 6px;

        mat-icon {
          font-size: 18px;
          width: 18px;
          height: 18px;
          color: var(--mat-sys-on-surface-variant);
        }
      }

      .action-buttons {
        display: flex;
        gap: 4px;
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

    .dialog-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 24px;

      .footer-info {
        display: flex;
        gap: 16px;
        font-size: 0.875rem;
        color: var(--mat-sys-on-surface-variant);
      }
    }
  `],
})
export class AgentVersionsDialogComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  private destroy$ = new Subject<void>();
  private logger = this.loggerService.withContext('AgentVersionsDialogComponent');

  dataSource = new MatTableDataSource<AgentVersion>([]);
  displayedColumns = [
    'versionNumber',
    'status',
    'changeType',
    'changeSummary',
    'createdBy',
    'createdAt',
    'actions',
  ];

  versions: AgentVersion[] = [];
  loading = false;
  rollingBack = false;

  constructor(
    private agentService: AgentBuilderService,
    private toast: ToastService,
    private loggerService: LoggerService,
    private dialog: MatDialog,
    private dialogRef: MatDialogRef<AgentVersionsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AgentVersionsDialogData
  ) {}

  ngOnInit(): void {
    this.loadVersions();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadVersions(): void {
    this.loading = true;
    this.agentService
      .listVersions(this.data.agent.id, 0, 100)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page) => {
          this.versions = page.content;
          this.dataSource.data = this.versions;
          this.loading = false;
          this.logger.info('Loaded versions', { count: this.versions.length });
        },
        error: (err) => {
          this.loading = false;
          this.logger.error('Failed to load versions', err);
          this.toast.error('Failed to load version history');
        },
      });
  }

  viewVersion(version: AgentVersion): void {
    this.logger.info('Viewing version', { versionId: version.id });
    // Will open version detail dialog or expand inline
    this.toast.info('Version detail view coming soon');
  }

  compareVersions(version: AgentVersion): void {
    this.logger.info('Comparing versions', { versionId: version.id });

    // Find current version
    const currentVersion = this.versions.find((v) => v.id === this.data.agent.version);
    if (!currentVersion) {
      this.toast.error('Current version not found');
      return;
    }

    const dialogRef = this.dialog.open(VersionCompareDialogComponent, {
      width: '95vw',
      maxWidth: '1400px',
      height: '85vh',
      data: {
        agentId: this.data.agent.id,
        version1Id: version.id,
        version2Id: currentVersion.id,
        version1Label: version.versionNumber,
        version2Label: currentVersion.versionNumber + ' (Current)',
      },
    });

    dialogRef.afterClosed().subscribe(() => {
      this.logger.info('Version comparison closed');
    });
  }

  rollbackToVersion(version: AgentVersion): void {
    const confirmDialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '500px',
      data: {
        title: 'Rollback to Version ' + version.versionNumber,
        message: `Are you sure you want to rollback to version ${version.versionNumber}? This will create a new version with the configuration from ${version.versionNumber}.`,
        confirmText: 'Rollback',
        cancelText: 'Cancel',
        confirmColor: 'warn',
      },
    });

    confirmDialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.performRollback(version);
      }
    });
  }

  private performRollback(version: AgentVersion): void {
    this.rollingBack = true;
    this.logger.info('Rolling back to version', { versionId: version.id });

    this.agentService
      .rollbackToVersion(this.data.agent.id, version.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updatedAgent) => {
          this.rollingBack = false;
          this.toast.success(`Rolled back to version ${version.versionNumber}`);
          this.logger.info('Rollback successful', { newVersion: updatedAgent.version });
          this.dialogRef.close(true); // Close and signal refresh
        },
        error: (err) => {
          this.rollingBack = false;
          this.logger.error('Rollback failed', err);
          this.toast.error('Failed to rollback version');
        },
      });
  }

  formatStatus(status: VersionStatus): string {
    return status
      .split('_')
      .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
      .join(' ');
  }

  onClose(): void {
    this.dialogRef.close();
  }
}
