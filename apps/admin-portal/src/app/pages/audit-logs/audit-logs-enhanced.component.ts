/**
 * Enhanced Audit Logs Viewer Component
 * HIPAA-compliant audit log viewer with comprehensive search, filtering, and export capabilities
 * Integrates with audit-query-service (port 8093)
 */

import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { AdminService } from '../../services/admin.service';
import {
  AuditEvent,
  AuditSearchRequest,
  AuditAction,
  AuditOutcome,
  AuditStatistics,
} from '../../models/admin.model';
import { Subject, takeUntil, debounceTime, distinctUntilChanged } from 'rxjs';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-audit-logs-enhanced',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './audit-logs-enhanced.component.html',
  styleUrls: ['./audit-logs-enhanced.component.scss'],
})
export class AuditLogsEnhancedComponent implements OnInit, OnDestroy {
  // Data
  events: AuditEvent[] = [];
  statistics: AuditStatistics | null = null;
  selectedEvent: AuditEvent | null = null;

  // UI State
  loading = false;
  exportLoading = false;
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;
  sortBy = 'timestamp';
  sortDirection: 'ASC' | 'DESC' = 'DESC';

  // Forms
  searchForm: FormGroup;

  // Enums for templates
  auditActions = Object.values(AuditAction);
  auditOutcomes = Object.values(AuditOutcome);

  // Expose Math for template
  Math = Math;

  // Display columns
  displayedColumns = [
    'timestamp',
    'username',
    'action',
    'resourceType',
    'outcome',
    'serviceName',
    'durationMs',
    'actions',
  ];

  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<void>();

  // Use inject() instead of constructor injection
  private adminService = inject(AdminService);
  private fb = inject(FormBuilder);
  private loggerService = inject(LoggerService);
  private logger = this.loggerService.withContext('AuditLogsEnhancedComponent');

  constructor() {
    this.searchForm = this.fb.group({
      username: [''],
      role: [''],
      resourceType: [''],
      resourceId: [''],
      actions: [[]],
      outcomes: [[]],
      serviceName: [''],
      startTime: [null],
      endTime: [null],
      searchText: [''],
    });
  }

  ngOnInit(): void {
    // Debounce search form changes
    this.searchSubject
      .pipe(debounceTime(500), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => {
        this.currentPage = 0;
        this.loadAuditLogs();
      });

    // Load initial data
    this.loadAuditLogs();
    this.loadStatistics();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load audit logs with current filters
   */
  loadAuditLogs(): void {
    this.loading = true;
    const request = this.buildSearchRequest();

    this.adminService
      .searchAuditLogs(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.events = response.content;
          this.totalElements = response.totalElements;
          this.totalPages = response.totalPages;
          this.currentPage = response.number;
          this.loading = false;
        },
        error: (error) => {
          this.logger.error('Failed to load audit logs', {
            error: error.message,
            status: error.status
          });
          this.loading = false;
        },
      });
  }

  /**
   * Load statistics for dashboard
   */
  loadStatistics(): void {
    const formValue = this.searchForm.value;
    const startTime = formValue.startTime
      ? new Date(formValue.startTime).toISOString()
      : undefined;
    const endTime = formValue.endTime
      ? new Date(formValue.endTime).toISOString()
      : undefined;

    this.adminService
      .getAuditStatistics(startTime, endTime)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (stats) => {
          this.statistics = stats;
        },
        error: (error) => {
          this.logger.error('Failed to load statistics', {
            error: error.message
          });
        },
      });
  }

  /**
   * Build search request from form values
   */
  private buildSearchRequest(): AuditSearchRequest {
    const formValue = this.searchForm.value;

    return {
      username: formValue.username || undefined,
      role: formValue.role || undefined,
      resourceType: formValue.resourceType || undefined,
      resourceId: formValue.resourceId || undefined,
      actions: formValue.actions?.length > 0 ? formValue.actions : undefined,
      outcomes: formValue.outcomes?.length > 0 ? formValue.outcomes : undefined,
      serviceName: formValue.serviceName || undefined,
      startTime: formValue.startTime
        ? new Date(formValue.startTime).toISOString()
        : undefined,
      endTime: formValue.endTime
        ? new Date(formValue.endTime).toISOString()
        : undefined,
      searchText: formValue.searchText || undefined,
      page: this.currentPage,
      size: this.pageSize,
      sortBy: this.sortBy,
      sortDirection: this.sortDirection,
    };
  }

  /**
   * Handle search form changes
   */
  onSearchFormChange(): void {
    this.searchSubject.next();
  }

  /**
   * Reset all filters
   */
  resetFilters(): void {
    this.searchForm.reset({
      username: '',
      role: '',
      resourceType: '',
      resourceId: '',
      actions: [],
      outcomes: [],
      serviceName: '',
      startTime: null,
      endTime: null,
      searchText: '',
    });
    this.currentPage = 0;
    this.loadAuditLogs();
    this.loadStatistics();
  }

  /**
   * Change page
   */
  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadAuditLogs();
  }

  /**
   * Change sort
   */
  onSortChange(column: string): void {
    if (this.sortBy === column) {
      this.sortDirection = this.sortDirection === 'ASC' ? 'DESC' : 'ASC';
    } else {
      this.sortBy = column;
      this.sortDirection = 'DESC';
    }
    this.loadAuditLogs();
  }

  /**
   * View event details
   */
  viewEventDetails(event: AuditEvent): void {
    this.selectedEvent = event;
  }

  /**
   * Close event details modal
   */
  closeEventDetails(): void {
    this.selectedEvent = null;
  }

  /**
   * Export audit logs to CSV
   */
  exportCsv(): void {
    this.exportLoading = true;
    const request = this.buildSearchRequest();

    this.adminService
      .exportAuditLogsCsv(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          this.downloadFile(blob, 'audit-logs.csv');
          this.exportLoading = false;
        },
        error: (error) => {
          this.logger.error('CSV export failed', {
            error: error.message,
            recordCount: this.events.length
          });
          this.exportLoading = false;
          alert('Export failed. Using fallback method.');
          this.exportCsvFallback();
        },
      });
  }

  /**
   * Export audit logs to JSON
   */
  exportJson(): void {
    this.exportLoading = true;
    const request = this.buildSearchRequest();

    this.adminService
      .exportAuditLogsJson(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          this.downloadFile(blob, 'audit-logs.json');
          this.exportLoading = false;
        },
        error: (error) => {
          this.logger.error('JSON export failed', {
            error: error.message,
            recordCount: this.events.length
          });
          this.exportLoading = false;
          alert('Export failed. Using fallback method.');
          this.exportJsonFallback();
        },
      });
  }

  /**
   * Export audit logs to PDF
   */
  exportPdf(): void {
    this.exportLoading = true;
    const request = this.buildSearchRequest();

    this.adminService
      .exportAuditLogsPdf(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          this.downloadFile(blob, 'audit-logs.pdf');
          this.exportLoading = false;
        },
        error: (error) => {
          this.logger.error('PDF export failed', {
            error: error.message
          });
          this.exportLoading = false;
          alert('PDF export not available. Please use CSV or JSON export.');
        },
      });
  }

  /**
   * Fallback CSV export (client-side)
   */
  private exportCsvFallback(): void {
    const headers = [
      'ID',
      'Timestamp',
      'Tenant ID',
      'User ID',
      'Username',
      'Role',
      'IP Address',
      'Action',
      'Resource Type',
      'Resource ID',
      'Outcome',
      'Service Name',
      'Duration (ms)',
    ];

    const rows = this.events.map((event) => [
      event.id,
      event.timestamp,
      event.tenantId,
      event.userId,
      event.username,
      event.role,
      event.ipAddress,
      event.action,
      event.resourceType,
      event.resourceId || '',
      event.outcome,
      event.serviceName,
      event.durationMs?.toString() || '',
    ]);

    const csvContent = [
      headers.join(','),
      ...rows.map((row) =>
        row.map((cell) => `"${cell.toString().replace(/"/g, '""')}"`).join(',')
      ),
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    this.downloadFile(blob, 'audit-logs.csv');
  }

  /**
   * Fallback JSON export (client-side)
   */
  private exportJsonFallback(): void {
    const jsonContent = JSON.stringify(this.events, null, 2);
    const blob = new Blob([jsonContent], { type: 'application/json' });
    this.downloadFile(blob, 'audit-logs.json');
  }

  /**
   * Download file helper
   */
  private downloadFile(blob: Blob, filename: string): void {
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `${filename.replace('.', `-${new Date().toISOString().split('T')[0]}.`)}`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(link.href);
  }

  /**
   * Get array of page numbers for pagination
   */
  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxPagesToShow = 5;
    let startPage = Math.max(0, this.currentPage - 2);
    const endPage = Math.min(this.totalPages - 1, startPage + maxPagesToShow - 1);

    if (endPage - startPage < maxPagesToShow - 1) {
      startPage = Math.max(0, endPage - maxPagesToShow + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }

    return pages;
  }

  /**
   * Get badge color for action
   */
  getActionBadgeClass(action: AuditAction): string {
    const classes: { [key in AuditAction]: string } = {
      CREATE: 'badge-success',
      READ: 'badge-info',
      UPDATE: 'badge-warning',
      DELETE: 'badge-danger',
      LOGIN: 'badge-primary',
      LOGOUT: 'badge-secondary',
      EXPORT: 'badge-purple',
      SEARCH: 'badge-cyan',
      EXECUTE: 'badge-orange',
    };
    return classes[action] || 'badge-default';
  }

  /**
   * Get badge color for outcome
   */
  getOutcomeBadgeClass(outcome: AuditOutcome): string {
    const classes: { [key in AuditOutcome]: string } = {
      SUCCESS: 'badge-success',
      FAILURE: 'badge-danger',
      PARTIAL: 'badge-warning',
    };
    return classes[outcome] || 'badge-default';
  }

  /**
   * Format duration in milliseconds
   */
  formatDuration(ms: number | undefined): string {
    if (!ms) return 'N/A';
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
  }
}
