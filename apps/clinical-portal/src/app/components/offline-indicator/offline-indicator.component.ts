/**
 * Issue #24: Offline Mode & Sync
 * Offline Indicator Component
 *
 * Displays network status and pending sync information to users.
 * Shows as a toast/banner when offline and provides sync controls.
 */
import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import {
  trigger,
  state,
  style,
  transition,
  animate,
} from '@angular/animations';
import { Subject, combineLatest } from 'rxjs';
import { takeUntil, distinctUntilChanged, skip, map } from 'rxjs/operators';

import { NetworkStatusService, NetworkState } from '../../services/offline/network-status.service';
import { SyncQueueService, SyncProgress } from '../../services/offline/sync-queue.service';

@Component({
  selector: 'app-offline-indicator',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatBadgeModule,
    MatSnackBarModule,
  ],
  template: `
    <!-- Offline Banner - Shows when offline -->
    @if (!isOnline) {
      <div class="offline-banner" [@slideIn]>
        <div class="banner-content">
          <mat-icon>cloud_off</mat-icon>
          <span class="offline-text">You're offline</span>
          <span class="offline-subtext">Changes will sync when connection is restored</span>
        </div>
        @if (pendingCount > 0) {
          <div class="pending-badge">
            <mat-icon>pending</mat-icon>
            <span>{{ pendingCount }} pending</span>
          </div>
        }
      </div>
    }

    <!-- Sync Progress Banner - Shows during sync -->
    @if (isSyncing) {
      <div class="sync-banner" [@slideIn]>
        <div class="banner-content">
          <mat-icon class="sync-icon">sync</mat-icon>
          <span class="sync-text">Syncing changes...</span>
          <span class="sync-progress-text">
            {{ syncProgress.completed }}/{{ syncProgress.total }}
          </span>
        </div>
        <mat-progress-bar
          mode="determinate"
          [value]="(syncProgress.completed / syncProgress.total) * 100"
        ></mat-progress-bar>
      </div>
    }

    <!-- Connection Quality Warning -->
    @if (isOnline && connectionQuality === 'poor' && !isSyncing) {
      <div class="poor-connection-banner" [@slideIn]>
        <mat-icon>signal_cellular_connected_no_internet_4_bar</mat-icon>
        <span>Poor connection - some features may be slow</span>
      </div>
    }

    <!-- Status Indicator Button (always visible in toolbar) -->
    <button
      mat-icon-button
      class="status-button"
      [class.offline]="!isOnline"
      [class.syncing]="isSyncing"
      [class.has-pending]="pendingCount > 0 && isOnline"
      [matTooltip]="getStatusTooltip()"
      (click)="handleStatusClick()"
    >
      @if (!isOnline) {
        <mat-icon>cloud_off</mat-icon>
      } @else if (isSyncing) {
        <mat-icon class="sync-icon">sync</mat-icon>
      } @else if (pendingCount > 0) {
        <mat-icon [matBadge]="pendingCount" matBadgeSize="small" matBadgeColor="warn">
          cloud_queue
        </mat-icon>
      } @else {
        <mat-icon>cloud_done</mat-icon>
      }
    </button>
  `,
  styles: [`
    :host {
      display: contents;
    }

    /* Offline Banner */
    .offline-banner {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      z-index: 1001;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 8px 16px;
      background: linear-gradient(135deg, #ef5350 0%, #e53935 100%);
      color: white;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
    }

    .banner-content {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .offline-text {
      font-weight: 600;
      font-size: 14px;
    }

    .offline-subtext {
      font-size: 12px;
      opacity: 0.9;
    }

    .pending-badge {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 4px 12px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 16px;
      font-size: 13px;

      mat-icon {
        font-size: 18px;
        width: 18px;
        height: 18px;
      }
    }

    /* Sync Banner */
    .sync-banner {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      z-index: 1001;
      background: linear-gradient(135deg, #42a5f5 0%, #1e88e5 100%);
      color: white;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
    }

    .sync-banner .banner-content {
      padding: 8px 16px;
    }

    .sync-text {
      font-weight: 500;
      font-size: 14px;
    }

    .sync-progress-text {
      font-size: 13px;
      opacity: 0.9;
    }

    .sync-icon {
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }

    .sync-banner mat-progress-bar {
      height: 3px;
    }

    ::ng-deep .sync-banner .mat-mdc-progress-bar {
      --mdc-linear-progress-active-indicator-color: rgba(255, 255, 255, 0.9);
      --mdc-linear-progress-track-color: rgba(255, 255, 255, 0.3);
    }

    /* Poor Connection Banner */
    .poor-connection-banner {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      z-index: 1000;
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 6px 16px;
      background: linear-gradient(135deg, #ffa726 0%, #fb8c00 100%);
      color: white;
      font-size: 13px;

      mat-icon {
        font-size: 18px;
        width: 18px;
        height: 18px;
      }
    }

    /* Status Button */
    .status-button {
      position: relative;

      &.offline mat-icon {
        color: #ef5350;
      }

      &.syncing mat-icon {
        color: #42a5f5;
      }

      &.has-pending mat-icon {
        color: #ffa726;
      }

      mat-icon:not([class*="mat-badge"]) {
        transition: color 0.2s ease;
      }
    }

    /* Badge styling */
    ::ng-deep .status-button .mat-badge-content {
      font-size: 10px;
      font-weight: 600;
    }
  `],
  animations: [
    trigger('slideIn', [
      state('void', style({ transform: 'translateY(-100%)', opacity: 0 })),
      state('*', style({ transform: 'translateY(0)', opacity: 1 })),
      transition('void => *', animate('200ms ease-out')),
      transition('* => void', animate('150ms ease-in')),
    ]),
  ],
})
export class OfflineIndicatorComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly networkStatus = inject(NetworkStatusService);
  private readonly syncQueue = inject(SyncQueueService);
  private readonly snackBar = inject(MatSnackBar);

  isOnline = true;
  isSyncing = false;
  pendingCount = 0;
  connectionQuality: 'good' | 'poor' | 'offline' = 'good';
  syncProgress: SyncProgress = {
    total: 0,
    completed: 0,
    failed: 0,
    isRunning: false,
  };

  ngOnInit(): void {
    // Subscribe to network status
    this.networkStatus.state$
      .pipe(takeUntil(this.destroy$))
      .subscribe((state: NetworkState) => {
        this.isOnline = state.isOnline;
      });

    // Subscribe to connection quality
    this.networkStatus.connectionQuality$
      .pipe(takeUntil(this.destroy$))
      .subscribe((quality) => {
        this.connectionQuality = quality;
      });

    // Subscribe to sync status
    this.syncQueue.isSyncing$
      .pipe(takeUntil(this.destroy$))
      .subscribe((syncing) => {
        this.isSyncing = syncing;
      });

    // Subscribe to pending count
    this.syncQueue.pendingCount$
      .pipe(takeUntil(this.destroy$))
      .subscribe((count) => {
        this.pendingCount = count;
      });

    // Subscribe to sync progress
    this.syncQueue.progress$
      .pipe(takeUntil(this.destroy$))
      .subscribe((progress) => {
        this.syncProgress = progress;
      });

    // Show notification when coming back online
    this.networkStatus.isOnline$
      .pipe(
        distinctUntilChanged(),
        skip(1), // Skip initial value
        takeUntil(this.destroy$)
      )
      .subscribe((isOnline) => {
        if (isOnline) {
          this.showNotification('Back online', 'sync');
        } else {
          this.showNotification('You are offline', 'cloud_off');
        }
      });

    // Show notification when sync completes
    combineLatest([
      this.syncQueue.isSyncing$,
      this.syncQueue.progress$,
    ])
      .pipe(
        map(([syncing, progress]) => ({ syncing, progress })),
        distinctUntilChanged((prev, curr) => prev.syncing === curr.syncing),
        skip(1),
        takeUntil(this.destroy$)
      )
      .subscribe(({ syncing, progress }) => {
        if (!syncing && progress.completed > 0) {
          const message = progress.failed > 0
            ? `Synced ${progress.completed} changes (${progress.failed} failed)`
            : `Synced ${progress.completed} changes`;
          this.showNotification(message, 'cloud_done');
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Get tooltip text based on current status
   */
  getStatusTooltip(): string {
    if (!this.isOnline) {
      return `Offline - ${this.pendingCount} changes pending`;
    }
    if (this.isSyncing) {
      return `Syncing ${this.syncProgress.completed}/${this.syncProgress.total}...`;
    }
    if (this.pendingCount > 0) {
      return `${this.pendingCount} changes pending - click to sync`;
    }
    return 'All changes synced';
  }

  /**
   * Handle click on status button
   */
  handleStatusClick(): void {
    if (!this.isOnline) {
      this.showNotification(
        'Cannot sync while offline. Changes will sync when connection is restored.',
        'cloud_off'
      );
      return;
    }

    if (this.isSyncing) {
      this.showNotification('Sync already in progress...', 'sync');
      return;
    }

    if (this.pendingCount > 0) {
      this.syncQueue.sync().subscribe((result) => {
        if (result.success) {
          this.showNotification(`Successfully synced ${result.syncedCount} changes`, 'cloud_done');
        } else {
          this.showNotification(
            `Sync completed with ${result.failedCount} errors`,
            'error'
          );
        }
      });
    } else {
      this.showNotification('Everything is up to date', 'cloud_done');
    }
  }

  /**
   * Show a snackbar notification
   */
  private showNotification(message: string, icon: string): void {
    this.snackBar.open(message, 'Dismiss', {
      duration: 3000,
      horizontalPosition: 'center',
      verticalPosition: 'bottom',
      panelClass: ['offline-snackbar'],
    });
  }
}
