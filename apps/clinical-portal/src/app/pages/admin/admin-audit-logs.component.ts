import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditLogService, AuditLogEntry } from '../../services/audit-log.service';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-admin-audit-logs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="admin-page">
      <h1>Audit Logs</h1>

      <div class="filters">
        <label>
          Event Type
          <select data-test-id="event-type-filter" [(ngModel)]="eventTypeFilter"
                  (change)="loadLogs()" aria-label="Filter by event type">
            <option value="">All</option>
            <option value="LOGIN_SUCCESS">Login Success</option>
            <option value="LOGIN_FAILURE">Login Failure</option>
            <option value="LOGOUT">Logout</option>
            <option value="USER_CREATED">User Created</option>
            <option value="USER_UPDATED">User Updated</option>
            <option value="PASSWORD_CHANGED">Password Changed</option>
            <option value="ROLE_CHANGED">Role Changed</option>
            <option value="ACCESS_DENIED">Access Denied</option>
            <option value="PHI_ACCESS">PHI Access</option>
          </select>
        </label>
        <button type="button" data-test-id="refresh-logs-button"
                (click)="loadLogs()" aria-label="Refresh logs">Refresh</button>
        <button type="button" data-test-id="failed-logins-button"
                (click)="loadFailedLogins()" aria-label="Show failed logins">Failed Logins (24h)</button>
      </div>

      @if (loading) {
        <div class="loading">Loading audit logs...</div>
      }

      @if (errorMessage) {
        <div class="error-message" data-test-id="error-message">{{ errorMessage }}</div>
      }

      @if (!loading && logs.length === 0 && !errorMessage) {
        <div class="empty-state" data-test-id="empty-state">No audit logs found.</div>
      }

      @if (logs.length > 0) {
        <table data-test-id="audit-log-table" class="audit-table">
          <thead>
            <tr>
              <th>Timestamp</th>
              <th>Event Type</th>
              <th>User</th>
              <th>Action</th>
              <th>Resource</th>
              <th>IP Address</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            @for (log of logs; track log.id) {
              <tr data-test-id="audit-log-row" [class.failure]="!log.success">
                <td data-test-id="log-timestamp">{{ log.timestamp | date:'medium' }}</td>
                <td data-test-id="log-event-type">
                  <span class="event-chip" [class]="getEventClass(log.eventType)">
                    {{ log.eventType }}
                  </span>
                </td>
                <td data-test-id="log-user">{{ log.username || log.userId }}</td>
                <td data-test-id="log-action">{{ log.action }}</td>
                <td>{{ log.resourceType }}{{ log.resourceId ? ' #' + log.resourceId : '' }}</td>
                <td>{{ log.ipAddress }}</td>
                <td>
                  <span [class]="log.success ? 'status-success' : 'status-failure'">
                    {{ log.success ? 'OK' : 'FAIL' }}
                  </span>
                </td>
              </tr>
            }
          </tbody>
        </table>

        <div class="pagination">
          <button type="button" [disabled]="currentPage === 0" (click)="changePage(currentPage - 1)"
                  aria-label="Previous page">Previous</button>
          <span>Page {{ currentPage + 1 }} of {{ totalPages }}</span>
          <button type="button" [disabled]="currentPage >= totalPages - 1" (click)="changePage(currentPage + 1)"
                  aria-label="Next page">Next</button>
        </div>
      }
    </section>
  `,
  styles: [`
    .admin-page { padding: 24px; }
    .filters { display: flex; gap: 12px; align-items: flex-end; margin-bottom: 16px; }
    .filters label { display: grid; gap: 4px; font-size: 14px; }
    .filters select { padding: 6px 8px; border: 1px solid #ccc; border-radius: 4px; }
    .filters button { padding: 6px 12px; cursor: pointer; }
    .loading { padding: 16px; color: #666; }
    .empty-state { padding: 16px; color: #999; font-style: italic; }
    .error-message { padding: 12px; background: #fdecea; color: #b71c1c; border-radius: 4px; margin-bottom: 16px; }
    .audit-table { width: 100%; border-collapse: collapse; font-size: 13px; }
    .audit-table th, .audit-table td { border-bottom: 1px solid #e0e0e0; padding: 6px 8px; text-align: left; }
    .audit-table th { font-weight: 600; background: #fafafa; }
    tr.failure { background: #fff8f8; }
    .event-chip { display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 11px; font-weight: 500; }
    .event-login { background: #e8f5e9; color: #2e7d32; }
    .event-failure { background: #fdecea; color: #b71c1c; }
    .event-security { background: #fff3e0; color: #e65100; }
    .event-change { background: #e3f2fd; color: #1565c0; }
    .event-default { background: #f5f5f5; color: #616161; }
    .status-success { color: #2e7d32; font-weight: 500; }
    .status-failure { color: #c62828; font-weight: 500; }
    .pagination { display: flex; gap: 12px; align-items: center; margin-top: 12px; padding: 8px 0; }
    .pagination button { padding: 4px 12px; cursor: pointer; }
    .pagination button:disabled { opacity: 0.5; cursor: default; }
  `],
})
export class AdminAuditLogsComponent implements OnInit {
  logs: AuditLogEntry[] = [];
  loading = false;
  errorMessage = '';
  eventTypeFilter = '';
  currentPage = 0;
  totalPages = 1;
  pageSize = 20;

  private logger;

  constructor(
    private auditLogService: AuditLogService,
    private loggerService: LoggerService,
  ) {
    this.logger = this.loggerService.withContext('AdminAuditLogsComponent');
  }

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(): void {
    this.loading = true;
    this.errorMessage = '';
    this.auditLogService.queryLogs({
      eventType: this.eventTypeFilter || undefined,
      page: this.currentPage,
      size: this.pageSize,
    }).subscribe({
      next: (page) => {
        this.logs = page.content;
        this.totalPages = page.totalPages || 1;
        this.loading = false;
        this.logger.info('Loaded audit logs', { count: page.totalElements });
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Failed to load audit logs. The audit service may not be enabled.';
        this.logger.error('Failed to load audit logs');
      },
    });
  }

  loadFailedLogins(): void {
    this.loading = true;
    this.errorMessage = '';
    this.auditLogService.getFailedLogins(24).subscribe({
      next: (page) => {
        this.logs = page.content;
        this.totalPages = page.totalPages || 1;
        this.currentPage = 0;
        this.loading = false;
        this.logger.info('Loaded failed logins', { count: page.totalElements });
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Failed to load failed login attempts.';
        this.logger.error('Failed to load failed logins');
      },
    });
  }

  changePage(page: number): void {
    this.currentPage = page;
    this.loadLogs();
  }

  getEventClass(eventType: string): string {
    if (eventType.includes('LOGIN_SUCCESS') || eventType === 'LOGOUT') return 'event-login';
    if (eventType.includes('FAILURE') || eventType.includes('DENIED')) return 'event-failure';
    if (eventType.includes('ACCESS') || eventType === 'PHI_ACCESS') return 'event-security';
    if (eventType.includes('CREATED') || eventType.includes('CHANGED') || eventType.includes('UPDATED')) return 'event-change';
    return 'event-default';
  }
}
