import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

interface AuditLogEntry {
  timestamp: string;
  action: string;
  user: string;
}

@Component({
  selector: 'app-admin-audit-logs',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="admin-page">
      <h1>Audit Logs</h1>

      <table data-test-id="audit-log-table" class="audit-table">
        <thead>
          <tr>
            <th>Timestamp</th>
            <th>Action</th>
            <th>User</th>
          </tr>
        </thead>
        <tbody>
          @for (log of logs; track log.timestamp) {
            <tr data-test-id="audit-log-row">
              <td data-test-id="log-timestamp">{{ log.timestamp }}</td>
              <td data-test-id="log-action">{{ log.action }}</td>
              <td data-test-id="log-user">{{ log.user }}</td>
            </tr>
          }
        </tbody>
      </table>
    </section>
  `,
  styles: [
    `
      .admin-page { padding: 24px; }
      .audit-table { width: 100%; border-collapse: collapse; }
      .audit-table th, .audit-table td { border-bottom: 1px solid #e0e0e0; padding: 8px; }
    `,
  ],
})
export class AdminAuditLogsComponent {
  logs: AuditLogEntry[] = [
    {
      timestamp: new Date().toISOString(),
      action: 'USER_LOGIN',
      user: 'demo_admin',
    },
    {
      timestamp: new Date(Date.now() - 60000).toISOString(),
      action: 'USER_CREATED',
      user: 'demo_admin',
    },
  ];
}
