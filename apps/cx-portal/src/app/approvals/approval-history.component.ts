import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';

import { CxApiService } from '../shared/services/cx-api.service';
import { PendingAction, ActionStatus } from '../shared/models';
import { ApprovalDetailModalComponent } from './approval-detail-modal.component';

@Component({
  selector: 'app-approval-history',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './approval-history.component.html',
  styleUrls: ['./approval-history.component.scss'],
})
export class ApprovalHistoryComponent implements OnInit {
  displayedColumns: string[] = ['target_name', 'action_type', 'status', 'reviewed_by', 'reviewed_at', 'actions'];
  actions: PendingAction[] = [];
  filteredActions: PendingAction[] = [];
  paginatedActions: PendingAction[] = [];

  // Filters
  statusFilter: ActionStatus | 'all' = 'all';
  searchQuery = '';

  // Pagination
  pageSize = 50;
  pageIndex = 0;
  totalItems = 0;

  constructor(
    private cxApi: CxApiService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadHistory();
  }

  /**
   * Load approval history from API
   */
  loadHistory(): void {
    this.cxApi.getApprovalHistory({ limit: 1000 }).subscribe({
      next: (actions) => {
        this.actions = actions;
        this.applyFilters();
      },
      error: (err) => console.error('Failed to load approval history:', err),
    });
  }

  /**
   * Apply status and search filters
   */
  applyFilters(): void {
    this.filteredActions = this.actions.filter((action) => {
      const statusMatch =
        this.statusFilter === 'all' ||
        action.status === this.statusFilter;

      const searchMatch =
        !this.searchQuery ||
        action.target_name.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        (action.subject && action.subject.toLowerCase().includes(this.searchQuery.toLowerCase()));

      return statusMatch && searchMatch;
    });

    this.totalItems = this.filteredActions.length;
    this.updatePagination();
  }

  /**
   * Update pagination
   */
  updatePagination(): void {
    const startIndex = this.pageIndex * this.pageSize;
    const endIndex = startIndex + this.pageSize;
    this.paginatedActions = this.filteredActions.slice(startIndex, endIndex);
  }

  /**
   * Handle page change
   */
  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.updatePagination();
  }

  /**
   * Set status filter
   */
  setStatusFilter(status: ActionStatus | 'all'): void {
    this.statusFilter = status;
    this.pageIndex = 0;
    this.applyFilters();
  }

  /**
   * Handle search input
   */
  onSearchChange(): void {
    this.pageIndex = 0;
    this.applyFilters();
  }

  /**
   * View action details (read-only)
   */
  viewDetails(action: PendingAction): void {
    this.dialog.open(ApprovalDetailModalComponent, {
      width: '800px',
      maxHeight: '80vh',
      data: { ...action, readOnly: true },
    });
  }

  /**
   * Get status chip color
   */
  getStatusColor(status: ActionStatus): string {
    switch (status) {
      case 'approved':
        return 'primary';
      case 'rejected':
        return 'warn';
      case 'edited':
        return 'accent';
      default:
        return '';
    }
  }

  /**
   * Format action type for display
   */
  formatActionType(actionType: string): string {
    return actionType.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
  }

  /**
   * Format date for display
   */
  formatDate(dateString: string | null): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString();
  }
}
