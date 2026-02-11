import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatListModule } from '@angular/material/list';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { interval, Subscription } from 'rxjs';
import { startWith, switchMap } from 'rxjs/operators';

import { CxApiService } from '../shared/services/cx-api.service';
import { PendingAction, ActionType, Urgency } from '../shared/models';
import { ApprovalDetailModalComponent } from './approval-detail-modal.component';

@Component({
  selector: 'app-approval-queue',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatListModule,
    MatChipsModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatCheckboxModule,
  ],
  templateUrl: './approval-queue.component.html',
  styleUrls: ['./approval-queue.component.scss'],
})
export class ApprovalQueueComponent implements OnInit, OnDestroy {
  actions: PendingAction[] = [];
  filteredActions: PendingAction[] = [];
  selectedActions = new Set<string>();
  loading = false;

  // Filters
  urgencyFilter: Urgency | 'all' = 'all';
  actionTypeFilter: ActionType | 'all' = 'all';

  private refreshSubscription?: Subscription;

  constructor(
    private cxApi: CxApiService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadActions();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
  }

  /**
   * Load pending actions from API
   */
  loadActions(): void {
    this.loading = true;
    this.cxApi.getPendingApprovals().subscribe({
      next: (actions) => {
        this.actions = actions;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load pending actions:', err);
        this.loading = false;
      },
    });
  }

  /**
   * Auto-refresh every 30 seconds
   */
  startAutoRefresh(): void {
    this.refreshSubscription = interval(30000)
      .pipe(
        startWith(0),
        switchMap(() => this.cxApi.getPendingApprovals())
      )
      .subscribe({
        next: (actions) => {
          this.actions = actions;
          this.applyFilters();
        },
        error: (err) => console.error('Auto-refresh failed:', err),
      });
  }

  /**
   * Stop auto-refresh
   */
  stopAutoRefresh(): void {
    this.refreshSubscription?.unsubscribe();
  }

  /**
   * Apply urgency and action type filters
   */
  applyFilters(): void {
    this.filteredActions = this.actions.filter((action) => {
      const urgencyMatch = this.urgencyFilter === 'all' || action.urgency === this.urgencyFilter;
      const typeMatch = this.actionTypeFilter === 'all' || action.action_type === this.actionTypeFilter;
      return urgencyMatch && typeMatch;
    });

    // Sort by urgency (urgent first), then by created_at
    this.filteredActions.sort((a, b) => {
      const urgencyOrder = { urgent: 0, normal: 1, low: 2 };
      if (urgencyOrder[a.urgency] !== urgencyOrder[b.urgency]) {
        return urgencyOrder[a.urgency] - urgencyOrder[b.urgency];
      }
      return new Date(a.created_at).getTime() - new Date(b.created_at).getTime();
    });
  }

  /**
   * Set urgency filter
   */
  setUrgencyFilter(urgency: Urgency | 'all'): void {
    this.urgencyFilter = urgency;
    this.applyFilters();
  }

  /**
   * Set action type filter
   */
  setActionTypeFilter(type: ActionType | 'all'): void {
    this.actionTypeFilter = type;
    this.applyFilters();
  }

  /**
   * Toggle action selection
   */
  toggleSelection(actionId: string): void {
    if (this.selectedActions.has(actionId)) {
      this.selectedActions.delete(actionId);
    } else {
      this.selectedActions.add(actionId);
    }
  }

  /**
   * Check if action is selected
   */
  isSelected(actionId: string): boolean {
    return this.selectedActions.has(actionId);
  }

  /**
   * Open detail modal for action
   */
  viewDetails(action: PendingAction): void {
    const dialogRef = this.dialog.open(ApprovalDetailModalComponent, {
      width: '800px',
      maxHeight: '80vh',
      data: action,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result === 'approved' || result === 'rejected' || result === 'edited') {
        this.loadActions();
        this.selectedActions.clear();
      }
    });
  }

  /**
   * Batch approve selected actions
   */
  batchApprove(): void {
    if (this.selectedActions.size === 0) return;

    this.loading = true;
    const actionIds = Array.from(this.selectedActions);
    this.cxApi.batchApprove(actionIds, 'Batch approved').subscribe({
      next: () => {
        this.loadActions();
        this.selectedActions.clear();
        this.loading = false;
      },
      error: (err) => {
        console.error('Batch approve failed:', err);
        this.loading = false;
      },
    });
  }

  /**
   * Get urgency badge color
   */
  getUrgencyColor(urgency: Urgency): string {
    switch (urgency) {
      case 'urgent':
        return 'warn';
      case 'normal':
        return 'primary';
      case 'low':
        return 'accent';
    }
  }

  /**
   * Get action type icon
   */
  getActionIcon(actionType: ActionType): string {
    const icons: Record<ActionType, string> = {
      email: 'mail',
      linkedin_message: 'message',
      linkedin_connection: 'person_add',
      calendar_invite: 'event',
      social_post: 'public',
      document_share: 'description',
      external_api: 'api',
    };
    return icons[actionType] || 'help';
  }

  /**
   * Format action type for display
   */
  formatActionType(actionType: ActionType): string {
    return actionType.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
  }

  /**
   * Format relative time (e.g., "2 hours ago")
   */
  timeAgo(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (seconds < 60) return `${seconds} seconds ago`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)} minutes ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)} hours ago`;
    return `${Math.floor(seconds / 86400)} days ago`;
  }
}
