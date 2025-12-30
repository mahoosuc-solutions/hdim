import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../services/admin.service';
import { AuditLog } from '../../models/admin.model';
import { Subject, takeUntil, debounceTime, distinctUntilChanged } from 'rxjs';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="audit-page">
      <div class="page-header">
        <h2>Audit Logs</h2>
        <div class="header-actions">
          <button class="btn-export" (click)="exportLogs()">
            <span>📥</span> Export
          </button>
        </div>
      </div>

      <!-- Filters -->
      <div class="filters-section">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input
            type="text"
            [(ngModel)]="searchQuery"
            (ngModelChange)="onSearchChange($event)"
            placeholder="Search by user, action, or resource..."
            class="search-input"
          />
        </div>

        <div class="filter-group">
          <select [(ngModel)]="selectedAction" (change)="applyFilters()" class="filter-select">
            <option value="">All Actions</option>
            <option value="CREATE">Create</option>
            <option value="READ">Read</option>
            <option value="UPDATE">Update</option>
            <option value="DELETE">Delete</option>
            <option value="LOGIN">Login</option>
            <option value="LOGOUT">Logout</option>
            <option value="EXPORT">Export</option>
          </select>

          <select [(ngModel)]="selectedSeverity" (change)="applyFilters()" class="filter-select">
            <option value="">All Severities</option>
            <option value="INFO">Info</option>
            <option value="WARNING">Warning</option>
            <option value="ERROR">Error</option>
            <option value="CRITICAL">Critical</option>
          </select>

          <select [(ngModel)]="selectedTimeRange" (change)="applyFilters()" class="filter-select">
            <option value="1h">Last Hour</option>
            <option value="24h">Last 24 Hours</option>
            <option value="7d">Last 7 Days</option>
            <option value="30d">Last 30 Days</option>
            <option value="all">All Time</option>
          </select>
        </div>
      </div>

      <!-- Stats Row -->
      <div class="stats-row">
        <div class="stat-item">
          <span class="stat-num">{{ totalLogs | number }}</span>
          <span class="stat-label">Total Events</span>
        </div>
        <div class="stat-item info">
          <span class="stat-num">{{ getCountBySeverity('INFO') }}</span>
          <span class="stat-label">Info</span>
        </div>
        <div class="stat-item warning">
          <span class="stat-num">{{ getCountBySeverity('WARNING') }}</span>
          <span class="stat-label">Warning</span>
        </div>
        <div class="stat-item error">
          <span class="stat-num">{{ getCountBySeverity('ERROR') }}</span>
          <span class="stat-label">Error</span>
        </div>
        <div class="stat-item critical">
          <span class="stat-num">{{ getCountBySeverity('CRITICAL') }}</span>
          <span class="stat-label">Critical</span>
        </div>
      </div>

      <!-- Logs Table -->
      <div class="logs-section">
        <table class="logs-table" *ngIf="!loading && filteredLogs.length > 0">
          <thead>
            <tr>
              <th class="col-time">Timestamp</th>
              <th class="col-severity">Severity</th>
              <th class="col-action">Action</th>
              <th class="col-user">User</th>
              <th class="col-resource">Resource</th>
              <th class="col-details">Details</th>
              <th class="col-ip">IP Address</th>
            </tr>
          </thead>
          <tbody>
            <tr
              *ngFor="let log of paginatedLogs"
              class="log-row"
              [class.info]="log.severity === 'INFO'"
              [class.warning]="log.severity === 'WARNING'"
              [class.error]="log.severity === 'ERROR'"
              [class.critical]="log.severity === 'CRITICAL'"
              (click)="viewDetails(log)"
            >
              <td class="col-time">
                <div class="time-cell">
                  <span class="date">{{ log.timestamp | date:'shortDate' }}</span>
                  <span class="time">{{ log.timestamp | date:'mediumTime' }}</span>
                </div>
              </td>
              <td class="col-severity">
                <span class="severity-badge" [class]="log.severity.toLowerCase()">
                  {{ log.severity }}
                </span>
              </td>
              <td class="col-action">
                <span class="action-badge" [class]="log.action.toLowerCase()">
                  {{ log.action }}
                </span>
              </td>
              <td class="col-user">
                <div class="user-cell">
                  <span class="user-avatar">{{ log.userId.charAt(0).toUpperCase() }}</span>
                  <span class="user-id">{{ log.userId }}</span>
                </div>
              </td>
              <td class="col-resource">
                <div class="resource-cell">
                  <span class="resource-type">{{ log.resourceType }}</span>
                  <span class="resource-id">{{ log.resourceId }}</span>
                </div>
              </td>
              <td class="col-details">
                <span class="details-text">{{ log.details | slice:0:50 }}{{ log.details.length > 50 ? '...' : '' }}</span>
              </td>
              <td class="col-ip">
                <span class="ip-address">{{ log.ipAddress }}</span>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Empty State -->
        <div class="empty-state" *ngIf="!loading && filteredLogs.length === 0">
          <span class="empty-icon">📋</span>
          <h3>No Logs Found</h3>
          <p>No audit logs match your current filters.</p>
        </div>

        <!-- Pagination -->
        <div class="pagination" *ngIf="filteredLogs.length > pageSize">
          <button
            class="page-btn"
            (click)="goToPage(currentPage - 1)"
            [disabled]="currentPage === 1"
          >
            Previous
          </button>
          <div class="page-numbers">
            <button
              *ngFor="let page of getPageNumbers()"
              class="page-num"
              [class.active]="page === currentPage"
              (click)="goToPage(page)"
            >
              {{ page }}
            </button>
          </div>
          <button
            class="page-btn"
            (click)="goToPage(currentPage + 1)"
            [disabled]="currentPage === totalPages"
          >
            Next
          </button>
        </div>

        <!-- Loading State -->
        <div class="loading" *ngIf="loading">
          <div class="spinner"></div>
          <span>Loading audit logs...</span>
        </div>
      </div>

      <!-- Details Modal -->
      <div class="modal-overlay" *ngIf="selectedLog" (click)="selectedLog = null">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h3>Log Details</h3>
            <button class="close-btn" (click)="selectedLog = null">×</button>
          </div>
          <div class="modal-body">
            <div class="detail-row">
              <span class="label">ID</span>
              <span class="value mono">{{ selectedLog.id }}</span>
            </div>
            <div class="detail-row">
              <span class="label">Timestamp</span>
              <span class="value">{{ selectedLog.timestamp | date:'full' }}</span>
            </div>
            <div class="detail-row">
              <span class="label">Severity</span>
              <span class="severity-badge" [class]="selectedLog.severity.toLowerCase()">
                {{ selectedLog.severity }}
              </span>
            </div>
            <div class="detail-row">
              <span class="label">Action</span>
              <span class="action-badge" [class]="selectedLog.action.toLowerCase()">
                {{ selectedLog.action }}
              </span>
            </div>
            <div class="detail-row">
              <span class="label">User</span>
              <span class="value">{{ selectedLog.userId }}</span>
            </div>
            <div class="detail-row">
              <span class="label">Tenant</span>
              <span class="value">{{ selectedLog.tenantId }}</span>
            </div>
            <div class="detail-row">
              <span class="label">Resource Type</span>
              <span class="value">{{ selectedLog.resourceType }}</span>
            </div>
            <div class="detail-row">
              <span class="label">Resource ID</span>
              <span class="value mono">{{ selectedLog.resourceId }}</span>
            </div>
            <div class="detail-row">
              <span class="label">IP Address</span>
              <span class="value mono">{{ selectedLog.ipAddress }}</span>
            </div>
            <div class="detail-row">
              <span class="label">User Agent</span>
              <span class="value small">{{ selectedLog.userAgent }}</span>
            </div>
            <div class="detail-row full">
              <span class="label">Details</span>
              <pre class="value code">{{ selectedLog.details }}</pre>
            </div>
            <div class="detail-row full" *ngIf="selectedLog.metadata">
              <span class="label">Metadata</span>
              <pre class="value code">{{ selectedLog.metadata | json }}</pre>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .audit-page {
      max-width: 1400px;
      margin: 0 auto;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }

    .page-header h2 {
      margin: 0;
      color: #1a237e;
    }

    .btn-export {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 20px;
      background: white;
      border: 1px solid #ddd;
      border-radius: 8px;
      cursor: pointer;
      font-weight: 500;
      transition: all 0.2s;
    }

    .btn-export:hover {
      background: #f5f5f5;
      border-color: #1a237e;
    }

    .filters-section {
      display: flex;
      gap: 16px;
      margin-bottom: 20px;
      flex-wrap: wrap;
    }

    .search-box {
      flex: 1;
      min-width: 300px;
      position: relative;
    }

    .search-icon {
      position: absolute;
      left: 12px;
      top: 50%;
      transform: translateY(-50%);
    }

    .search-input {
      width: 100%;
      padding: 10px 12px 10px 40px;
      border: 1px solid #ddd;
      border-radius: 8px;
      font-size: 14px;
      box-sizing: border-box;
    }

    .search-input:focus {
      outline: none;
      border-color: #1a237e;
    }

    .filter-group {
      display: flex;
      gap: 12px;
    }

    .filter-select {
      padding: 10px 12px;
      border: 1px solid #ddd;
      border-radius: 8px;
      font-size: 14px;
      background: white;
      cursor: pointer;
    }

    .filter-select:focus {
      outline: none;
      border-color: #1a237e;
    }

    .stats-row {
      display: flex;
      gap: 16px;
      margin-bottom: 20px;
      padding: 16px;
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .stat-item {
      flex: 1;
      text-align: center;
      padding: 12px;
      border-radius: 8px;
      background: #f5f7fa;
    }

    .stat-item.info { background: #e3f2fd; }
    .stat-item.warning { background: #fff3e0; }
    .stat-item.error { background: #ffebee; }
    .stat-item.critical { background: #fce4ec; }

    .stat-num {
      display: block;
      font-size: 24px;
      font-weight: 700;
      color: #1a237e;
    }

    .stat-item.info .stat-num { color: #1565c0; }
    .stat-item.warning .stat-num { color: #e65100; }
    .stat-item.error .stat-num { color: #c62828; }
    .stat-item.critical .stat-num { color: #880e4f; }

    .stat-label {
      font-size: 13px;
      color: #666;
    }

    .logs-section {
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      overflow: hidden;
    }

    .logs-table {
      width: 100%;
      border-collapse: collapse;
    }

    .logs-table th {
      background: #f5f7fa;
      padding: 14px 12px;
      text-align: left;
      font-weight: 600;
      color: #333;
      font-size: 13px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      border-bottom: 1px solid #eee;
    }

    .logs-table td {
      padding: 12px;
      border-bottom: 1px solid #f5f5f5;
      font-size: 13px;
    }

    .log-row {
      cursor: pointer;
      transition: background 0.2s;
    }

    .log-row:hover {
      background: #f5f7fa;
    }

    .log-row.warning {
      border-left: 3px solid #ff9800;
    }

    .log-row.error {
      border-left: 3px solid #f44336;
    }

    .log-row.critical {
      border-left: 3px solid #880e4f;
      background: #fce4ec;
    }

    .time-cell {
      display: flex;
      flex-direction: column;
    }

    .time-cell .date {
      color: #333;
      font-weight: 500;
    }

    .time-cell .time {
      color: #999;
      font-size: 12px;
    }

    .severity-badge,
    .action-badge {
      display: inline-block;
      padding: 4px 10px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 600;
      text-transform: uppercase;
    }

    .severity-badge.info { background: #e3f2fd; color: #1565c0; }
    .severity-badge.warning { background: #fff3e0; color: #e65100; }
    .severity-badge.error { background: #ffebee; color: #c62828; }
    .severity-badge.critical { background: #fce4ec; color: #880e4f; }

    .action-badge.create { background: #e8f5e9; color: #2e7d32; }
    .action-badge.read { background: #e3f2fd; color: #1565c0; }
    .action-badge.update { background: #fff3e0; color: #e65100; }
    .action-badge.delete { background: #ffebee; color: #c62828; }
    .action-badge.login { background: #e8eaf6; color: #3949ab; }
    .action-badge.logout { background: #fce4ec; color: #880e4f; }
    .action-badge.export { background: #f3e5f5; color: #7b1fa2; }

    .user-cell {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .user-avatar {
      width: 28px;
      height: 28px;
      background: #1a237e;
      color: white;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: 600;
    }

    .user-id {
      color: #333;
    }

    .resource-cell {
      display: flex;
      flex-direction: column;
    }

    .resource-type {
      color: #333;
      font-weight: 500;
    }

    .resource-id {
      color: #999;
      font-size: 11px;
      font-family: monospace;
    }

    .details-text {
      color: #666;
    }

    .ip-address {
      font-family: monospace;
      color: #666;
      font-size: 12px;
    }

    .pagination {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 8px;
      padding: 20px;
      border-top: 1px solid #eee;
    }

    .page-btn {
      padding: 8px 16px;
      background: white;
      border: 1px solid #ddd;
      border-radius: 6px;
      cursor: pointer;
      font-size: 14px;
    }

    .page-btn:hover:not(:disabled) {
      background: #f5f5f5;
    }

    .page-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .page-numbers {
      display: flex;
      gap: 4px;
    }

    .page-num {
      width: 36px;
      height: 36px;
      background: white;
      border: 1px solid #ddd;
      border-radius: 6px;
      cursor: pointer;
      font-size: 14px;
    }

    .page-num:hover {
      background: #f5f5f5;
    }

    .page-num.active {
      background: #1a237e;
      color: white;
      border-color: #1a237e;
    }

    .empty-state {
      text-align: center;
      padding: 48px;
    }

    .empty-icon {
      font-size: 48px;
    }

    .empty-state h3 {
      margin: 16px 0 8px;
      color: #333;
    }

    .empty-state p {
      color: #666;
    }

    .loading {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 48px;
      color: #666;
    }

    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid #e0e0e0;
      border-top-color: #1a237e;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin-bottom: 16px;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }

    .modal {
      background: white;
      border-radius: 12px;
      width: 600px;
      max-width: 90%;
      max-height: 90vh;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px;
      border-bottom: 1px solid #eee;
    }

    .modal-header h3 {
      margin: 0;
      color: #333;
    }

    .close-btn {
      background: none;
      border: none;
      font-size: 24px;
      cursor: pointer;
      color: #666;
    }

    .modal-body {
      padding: 20px;
      overflow-y: auto;
    }

    .detail-row {
      display: flex;
      align-items: flex-start;
      margin-bottom: 12px;
      gap: 12px;
    }

    .detail-row.full {
      flex-direction: column;
    }

    .detail-row .label {
      width: 100px;
      flex-shrink: 0;
      font-weight: 500;
      color: #666;
      font-size: 13px;
    }

    .detail-row .value {
      color: #333;
      font-size: 14px;
    }

    .detail-row .value.mono {
      font-family: monospace;
    }

    .detail-row .value.small {
      font-size: 12px;
      word-break: break-all;
    }

    .detail-row .value.code {
      background: #f5f7fa;
      padding: 12px;
      border-radius: 6px;
      font-family: monospace;
      font-size: 12px;
      white-space: pre-wrap;
      word-break: break-all;
      width: 100%;
      margin: 0;
      overflow-x: auto;
    }
  `],
})
export class AuditLogsComponent implements OnInit, OnDestroy {
  logs: AuditLog[] = [];
  filteredLogs: AuditLog[] = [];
  loading = true;
  searchQuery = '';
  selectedAction = '';
  selectedSeverity = '';
  selectedTimeRange = '24h';
  selectedLog: AuditLog | null = null;

  currentPage = 1;
  pageSize = 20;
  totalLogs = 0;

  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<string>();

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => this.applyFilters());

    this.loadLogs();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadLogs(): void {
    this.loading = true;
    this.adminService
      .getAuditLogs()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.logs = response.content;
          this.totalLogs = response.totalElements;
          this.applyFilters();
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        },
      });
  }

  onSearchChange(query: string): void {
    this.searchSubject.next(query);
  }

  applyFilters(): void {
    let filtered = [...this.logs];

    // Search filter
    if (this.searchQuery) {
      const query = this.searchQuery.toLowerCase();
      filtered = filtered.filter(
        (log) =>
          log.userId.toLowerCase().includes(query) ||
          log.action.toLowerCase().includes(query) ||
          log.resourceType.toLowerCase().includes(query) ||
          log.details.toLowerCase().includes(query)
      );
    }

    // Action filter
    if (this.selectedAction) {
      filtered = filtered.filter((log) => log.action === this.selectedAction);
    }

    // Severity filter
    if (this.selectedSeverity) {
      filtered = filtered.filter((log) => log.severity === this.selectedSeverity);
    }

    // Time range filter
    if (this.selectedTimeRange !== 'all') {
      const now = new Date();
      let cutoff: Date;

      switch (this.selectedTimeRange) {
        case '1h':
          cutoff = new Date(now.getTime() - 60 * 60 * 1000);
          break;
        case '24h':
          cutoff = new Date(now.getTime() - 24 * 60 * 60 * 1000);
          break;
        case '7d':
          cutoff = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
          break;
        case '30d':
          cutoff = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
          break;
        default:
          cutoff = new Date(0);
      }

      filtered = filtered.filter((log) => new Date(log.timestamp) >= cutoff);
    }

    this.filteredLogs = filtered;
    this.currentPage = 1;
  }

  getCountBySeverity(severity: string): number {
    return this.logs.filter((log) => log.severity === severity).length;
  }

  get paginatedLogs(): AuditLog[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredLogs.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.ceil(this.filteredLogs.length / this.pageSize);
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxPages = 5;
    let start = Math.max(1, this.currentPage - 2);
    const end = Math.min(this.totalPages, start + maxPages - 1);

    if (end - start < maxPages - 1) {
      start = Math.max(1, end - maxPages + 1);
    }

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }

    return pages;
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  viewDetails(log: AuditLog): void {
    this.selectedLog = log;
  }

  exportLogs(): void {
    const csvContent = this.generateCSV();
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `audit-logs-${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
  }

  private generateCSV(): string {
    const headers = [
      'ID',
      'Timestamp',
      'Severity',
      'Action',
      'User ID',
      'Tenant ID',
      'Resource Type',
      'Resource ID',
      'IP Address',
      'Details',
    ];

    const rows = this.filteredLogs.map((log) => [
      log.id,
      log.timestamp,
      log.severity,
      log.action,
      log.userId,
      log.tenantId,
      log.resourceType,
      log.resourceId,
      log.ipAddress,
      `"${log.details.replace(/"/g, '""')}"`,
    ]);

    return [headers.join(','), ...rows.map((row) => row.join(','))].join('\n');
  }
}
