import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../services/admin.service';
import { ConfigApproval, ConfigVersion, ServiceDefinition } from '../../models/admin.model';
import { Subject, forkJoin, takeUntil } from 'rxjs';

@Component({
  selector: 'app-config-versions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="config-page">
      <div class="page-header">
        <div class="header-left">
          <h2>Config Versions</h2>
          <span class="subtitle">Demo-first configuration governance</span>
        </div>
        <div class="header-actions">
          <button class="btn-secondary" (click)="loadVersions()">Refresh</button>
        </div>
      </div>

      <div class="alert" *ngIf="errorMessage">{{ errorMessage }}</div>
      <div class="alert success" *ngIf="successMessage">{{ successMessage }}</div>

      <div class="selector-card">
        <div class="field">
          <label>Tenant</label>
          <input type="text" [(ngModel)]="tenantId" placeholder="demo-clinic" />
        </div>
        <div class="field">
          <label>Service</label>
          <input
            type="text"
            list="service-options"
            [(ngModel)]="serviceName"
            placeholder="quality-measure"
          />
          <datalist id="service-options">
            <option *ngFor="let service of serviceOptions" [value]="service"></option>
          </datalist>
        </div>
        <div class="field">
          <label>Target Tenant</label>
          <input type="text" [(ngModel)]="targetTenantId" placeholder="tenant-id" />
        </div>
        <button class="btn-primary" (click)="loadVersions()">Load</button>
      </div>

      <div class="grid">
        <div class="panel">
          <div class="panel-header">
            <h3>Current Active Version</h3>
            <span class="status" [class.active]="currentVersion?.status === 'ACTIVE'">
              {{ currentVersion?.status || 'None' }}
            </span>
          </div>
          <div class="panel-body" *ngIf="currentVersion; else emptyCurrent">
            <div class="meta-row">
              <span class="label">Version</span>
              <span class="value">v{{ currentVersion?.versionNumber }}</span>
            </div>
            <div class="meta-row">
              <span class="label">Change</span>
              <span class="value">{{ currentVersion?.changeSummary || 'No summary' }}</span>
            </div>
            <div class="meta-row">
              <span class="label">Hash</span>
              <span class="value monospace">{{ currentVersion?.configHash }}</span>
            </div>
            <div class="meta-row">
              <span class="label">Created By</span>
              <span class="value">{{ currentVersion?.createdBy }}</span>
            </div>
            <div class="meta-row">
              <span class="label">Created</span>
              <span class="value">{{ currentVersion?.createdAt | date:'medium' }}</span>
            </div>
          </div>
          <ng-template #emptyCurrent>
            <div class="empty-state">No active version for this tenant/service.</div>
          </ng-template>
        </div>

        <div class="panel">
          <div class="panel-header">
            <h3>Versions</h3>
            <span class="meta">{{ versions.length }} total</span>
          </div>
          <div class="panel-body" *ngIf="!loading">
            <table class="versions-table" *ngIf="versions.length > 0">
              <thead>
                <tr>
                  <th>Version</th>
                  <th>Status</th>
                  <th>Summary</th>
                  <th>Source</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  *ngFor="let version of versions"
                  (click)="selectVersion(version)"
                  [class.selected]="selectedVersion?.id === version.id"
                >
                  <td>v{{ version.versionNumber }}</td>
                  <td>
                    <span class="status-badge" [class]="version.status.toLowerCase()">
                      {{ version.status }}
                    </span>
                  </td>
                  <td>{{ version.changeSummary || 'No summary' }}</td>
                  <td>{{ version.sourceVersionId ? 'Demo' : 'Direct' }}</td>
                  <td>
                    <button
                      class="btn-mini"
                      (click)="requestApproval(version); $event.stopPropagation()"
                    >
                      Request
                    </button>
                    <button
                      class="btn-mini"
                      (click)="approveVersion(version); $event.stopPropagation()"
                    >
                      Approve
                    </button>
                    <button
                      class="btn-mini danger"
                      (click)="rejectVersion(version); $event.stopPropagation()"
                    >
                      Reject
                    </button>
                    <button
                      class="btn-mini primary"
                      (click)="activateVersion(version); $event.stopPropagation()"
                    >
                      Activate
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
            <div class="empty-state" *ngIf="versions.length === 0">No versions found.</div>
          </div>
          <div class="panel-body" *ngIf="loading">
            <div class="loading">Loading versions...</div>
          </div>
        </div>
      </div>

      <div class="grid">
        <div class="panel">
          <div class="panel-header">
            <h3>Selected Version</h3>
            <span class="meta" *ngIf="selectedVersion">v{{ selectedVersion.versionNumber }}</span>
          </div>
          <div class="panel-body" *ngIf="selectedVersion; else emptySelection">
            <div class="meta-row">
              <span class="label">Status</span>
              <span class="value">{{ selectedVersion.status }}</span>
            </div>
            <div class="meta-row">
              <span class="label">Created By</span>
              <span class="value">{{ selectedVersion.createdBy }}</span>
            </div>
            <div class="meta-row">
              <span class="label">Change Summary</span>
              <span class="value">{{ selectedVersion.changeSummary || 'No summary' }}</span>
            </div>
            <div class="meta-row">
              <span class="label">Config Hash</span>
              <span class="value monospace">{{ selectedVersion.configHash }}</span>
            </div>
            <div class="config-json">
              <div class="config-header">Config Payload</div>
              <pre>{{ formatConfig(selectedVersion.config) }}</pre>
            </div>
          </div>
          <ng-template #emptySelection>
            <div class="empty-state">Select a version to review details.</div>
          </ng-template>
        </div>

        <div class="panel approvals">
          <div class="panel-header">
            <h3>Approvals</h3>
            <span class="meta">{{ approvalStats }}</span>
          </div>
          <div class="panel-body">
            <div class="approval-actions">
              <textarea
                [(ngModel)]="approvalComment"
                placeholder="Add approval or rejection notes"
              ></textarea>
              <div class="button-row">
                <button class="btn-secondary" (click)="requestApproval(selectedVersion)">
                  Request Approval
                </button>
                <button class="btn-primary" (click)="approveVersion(selectedVersion)">
                  Approve
                </button>
                <button class="btn-secondary danger" (click)="rejectVersion(selectedVersion)">
                  Reject
                </button>
              </div>
            </div>
            <div class="approval-list" *ngIf="approvals.length > 0">
              <div class="approval-item" *ngFor="let approval of approvals">
                <div class="approval-main">
                  <span class="badge" [class]="approval.action.toLowerCase()">{{ approval.action }}</span>
                  <span class="actor">{{ approval.actor }}</span>
                </div>
                <div class="approval-meta">
                  <span>{{ approval.comment || 'No comment' }}</span>
                  <span>{{ approval.createdAt | date:'short' }}</span>
                </div>
              </div>
            </div>
            <div class="empty-state" *ngIf="approvals.length === 0">
              No approvals recorded yet.
            </div>
          </div>
        </div>
      </div>

      <div class="grid">
        <div class="panel">
          <div class="panel-header">
            <h3>Create Demo Version</h3>
            <span class="meta">Demo tenant only</span>
          </div>
          <div class="panel-body">
            <label class="inline-label">Change summary</label>
            <input type="text" [(ngModel)]="changeSummary" placeholder="Describe what changed" />
            <label class="inline-label">Config JSON</label>
            <textarea [(ngModel)]="configPayload" rows="8"></textarea>
            <div class="button-row">
              <label class="checkbox">
                <input type="checkbox" [(ngModel)]="activateOnCreate" />
                Activate immediately
              </label>
              <button class="btn-primary" (click)="createVersion()">Create Version</button>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-header">
            <h3>Promote From Demo</h3>
            <span class="meta">Two-person approval required</span>
          </div>
          <div class="panel-body">
            <label class="inline-label">Source Version ID</label>
            <input type="text" [(ngModel)]="sourceVersionId" placeholder="Select from demo versions" />
            <label class="inline-label">Promotion summary</label>
            <input type="text" [(ngModel)]="promotionSummary" placeholder="Describe promotion intent" />
            <div class="button-row">
              <label class="checkbox">
                <input type="checkbox" [(ngModel)]="activateOnPromote" />
                Activate after promotion
              </label>
              <button class="btn-primary" (click)="promoteVersion()">Promote</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .config-page {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .page-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .header-left h2 {
      margin: 0;
      font-size: 24px;
      color: #1a237e;
    }

    .subtitle {
      font-size: 14px;
      color: #666;
    }

    .alert {
      background: #ffebee;
      color: #b71c1c;
      padding: 12px 16px;
      border-radius: 8px;
      font-size: 14px;
    }

    .alert.success {
      background: #e8f5e9;
      color: #1b5e20;
    }

    .selector-card {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr)) auto;
      gap: 16px;
      background: white;
      padding: 16px;
      border-radius: 12px;
      align-items: end;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
    }

    .field {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .field label {
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: #607d8b;
      font-weight: 600;
    }

    input,
    textarea {
      border: 1px solid #dbe2f1;
      border-radius: 8px;
      padding: 10px 12px;
      font-size: 14px;
      font-family: inherit;
      background: #f9fbff;
    }

    textarea {
      resize: vertical;
      min-height: 120px;
    }

    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 20px;
    }

    .panel {
      background: white;
      border-radius: 16px;
      box-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .panel-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .panel-header h3 {
      margin: 0;
      font-size: 18px;
      color: #1a237e;
    }

    .panel-body {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .meta {
      font-size: 12px;
      color: #78909c;
    }

    .status {
      font-size: 12px;
      padding: 4px 10px;
      border-radius: 999px;
      background: #e3f2fd;
      color: #1565c0;
    }

    .status.active {
      background: #e8f5e9;
      color: #2e7d32;
    }

    .versions-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 13px;
    }

    .versions-table th {
      text-align: left;
      color: #78909c;
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      padding-bottom: 8px;
    }

    .versions-table td {
      padding: 8px 0;
      border-top: 1px solid #eef1f7;
    }

    .versions-table tr.selected {
      background: #f5f7ff;
    }

    .status-badge {
      display: inline-flex;
      align-items: center;
      padding: 4px 8px;
      border-radius: 999px;
      font-size: 11px;
      font-weight: 600;
      text-transform: uppercase;
    }

    .status-badge.active {
      background: #e8f5e9;
      color: #2e7d32;
    }

    .status-badge.pending_approval {
      background: #fff8e1;
      color: #f57f17;
    }

    .status-badge.approved {
      background: #e3f2fd;
      color: #1565c0;
    }

    .status-badge.rejected {
      background: #ffebee;
      color: #c62828;
    }

    .status-badge.draft {
      background: #eceff1;
      color: #546e7a;
    }

    .status-badge.superseded {
      background: #f3e5f5;
      color: #6a1b9a;
    }

    .btn-primary,
    .btn-secondary {
      border: none;
      border-radius: 8px;
      padding: 10px 16px;
      font-weight: 600;
      cursor: pointer;
    }

    .btn-primary {
      background: #1a237e;
      color: white;
    }

    .btn-secondary {
      background: #e3f2fd;
      color: #1a237e;
    }

    .btn-secondary.danger {
      background: #ffebee;
      color: #c62828;
    }

    .btn-mini {
      background: #e3f2fd;
      color: #1a237e;
      border: none;
      border-radius: 6px;
      padding: 4px 8px;
      margin-right: 4px;
      font-size: 11px;
      cursor: pointer;
    }

    .btn-mini.primary {
      background: #1a237e;
      color: white;
    }

    .btn-mini.danger {
      background: #ffebee;
      color: #c62828;
    }

    .meta-row {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      font-size: 13px;
    }

    .meta-row .label {
      color: #78909c;
      font-weight: 600;
    }

    .meta-row .value {
      color: #263238;
      flex: 1;
      text-align: right;
    }

    .config-json {
      background: #f5f7ff;
      border-radius: 10px;
      padding: 12px;
    }

    .config-header {
      font-size: 12px;
      font-weight: 600;
      color: #607d8b;
      margin-bottom: 8px;
    }

    pre {
      margin: 0;
      font-size: 12px;
      white-space: pre-wrap;
      word-break: break-word;
    }

    .approval-actions textarea {
      min-height: 80px;
    }

    .button-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      flex-wrap: wrap;
    }

    .checkbox {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      color: #546e7a;
    }

    .approval-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .approval-item {
      background: #f9fbff;
      border-radius: 8px;
      padding: 10px 12px;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .approval-main {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .badge {
      padding: 3px 8px;
      border-radius: 999px;
      font-size: 11px;
      text-transform: uppercase;
      font-weight: 600;
    }

    .badge.requested {
      background: #fff8e1;
      color: #f57f17;
    }

    .badge.approved {
      background: #e8f5e9;
      color: #2e7d32;
    }

    .badge.rejected {
      background: #ffebee;
      color: #c62828;
    }

    .approval-meta {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: #607d8b;
    }

    .empty-state {
      color: #90a4ae;
      font-size: 13px;
      text-align: center;
      padding: 12px 0;
    }

    .monospace {
      font-family: 'Courier New', monospace;
    }

    @media (max-width: 900px) {
      .selector-card {
        grid-template-columns: 1fr;
      }

      .button-row {
        flex-direction: column;
        align-items: stretch;
      }
    }
  `],
})
export class ConfigVersionsComponent implements OnInit, OnDestroy {
  tenantId = 'demo-clinic';
  serviceName = 'quality-measure';
  targetTenantId = 'demo-clinic';
  sourceVersionId = '';
  changeSummary = '';
  promotionSummary = '';
  configPayload = '{\n  "feature": "value"\n}';
  approvalComment = '';
  activateOnCreate = false;
  activateOnPromote = false;

  versions: ConfigVersion[] = [];
  currentVersion: ConfigVersion | null = null;
  approvals: ConfigApproval[] = [];
  selectedVersion: ConfigVersion | null = null;
  serviceOptions: string[] = [];

  loading = false;
  errorMessage = '';
  successMessage = '';

  private destroy$ = new Subject<void>();

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.adminService.getServiceCatalog()
      .pipe(takeUntil(this.destroy$))
      .subscribe((services: ServiceDefinition[]) => {
        this.serviceOptions = services.map((service) => service.id || service.name).filter(Boolean);
      });

    this.loadVersions();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get approvalStats(): string {
    if (!this.approvals.length) {
      return 'No approvals';
    }
    const approved = this.approvals.filter((approval) => approval.action === 'APPROVED').length;
    const rejected = this.approvals.filter((approval) => approval.action === 'REJECTED').length;
    const requested = this.approvals.filter((approval) => approval.action === 'REQUESTED').length;
    return `${approved} approved / ${requested} requested / ${rejected} rejected`;
  }

  loadVersions(): void {
    this.resetMessages();
    if (!this.tenantId || !this.serviceName) {
      this.errorMessage = 'Tenant and service are required.';
      return;
    }

    this.loading = true;
    forkJoin({
      versions: this.adminService.getConfigVersions(this.serviceName, this.tenantId),
      current: this.adminService.getConfigCurrent(this.serviceName, this.tenantId),
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe(({ versions, current }) => {
        this.versions = versions;
        this.currentVersion = current;
        this.loading = false;
        if (this.selectedVersion) {
          const match = this.versions.find((version) => version.id === this.selectedVersion?.id);
          this.selectedVersion = match || null;
        }
        if (!this.selectedVersion && this.versions.length > 0) {
          this.selectVersion(this.versions[0]);
        } else if (this.selectedVersion) {
          this.loadApprovals(this.selectedVersion);
        }
      });

    if (!this.targetTenantId) {
      this.targetTenantId = this.tenantId;
    }
  }

  selectVersion(version: ConfigVersion): void {
    this.selectedVersion = version;
    this.sourceVersionId = version.id;
    this.loadApprovals(version);
  }

  createVersion(): void {
    this.resetMessages();
    if (!this.tenantId || !this.serviceName) {
      this.errorMessage = 'Tenant and service are required.';
      return;
    }

    let config: Record<string, unknown>;
    try {
      config = JSON.parse(this.configPayload);
    } catch (error) {
      this.errorMessage = 'Config JSON is invalid. Please fix the payload and try again.';
      return;
    }

    this.adminService
      .createConfigVersion(this.serviceName, this.tenantId, {
        config,
        changeSummary: this.changeSummary,
        activate: this.activateOnCreate,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.successMessage = 'Version created successfully.';
        this.loadVersions();
      });
  }

  promoteVersion(): void {
    this.resetMessages();
    if (!this.targetTenantId || !this.serviceName || !this.sourceVersionId) {
      this.errorMessage = 'Target tenant, service, and source version ID are required.';
      return;
    }

    this.adminService
      .promoteConfigVersion(this.serviceName, this.targetTenantId, {
        sourceVersionId: this.sourceVersionId,
        changeSummary: this.promotionSummary,
        activate: this.activateOnPromote,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.successMessage = 'Promotion request submitted.';
        if (this.targetTenantId === this.tenantId) {
          this.loadVersions();
        }
      });
  }

  requestApproval(version: ConfigVersion | null): void {
    if (!version) {
      this.errorMessage = 'Select a version to request approval.';
      return;
    }
    this.resetMessages();
    this.adminService
      .requestConfigApproval(version.serviceName, version.tenantId, version.id, this.approvalComment)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.successMessage = 'Approval requested.';
        this.approvalComment = '';
        this.loadApprovals(version);
        this.loadVersions();
      });
  }

  approveVersion(version: ConfigVersion | null): void {
    if (!version) {
      this.errorMessage = 'Select a version to approve.';
      return;
    }
    this.resetMessages();
    this.adminService
      .approveConfigVersion(version.serviceName, version.tenantId, version.id, this.approvalComment)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.successMessage = 'Approval recorded.';
        this.approvalComment = '';
        this.loadApprovals(version);
        this.loadVersions();
      });
  }

  rejectVersion(version: ConfigVersion | null): void {
    if (!version) {
      this.errorMessage = 'Select a version to reject.';
      return;
    }
    this.resetMessages();
    this.adminService
      .rejectConfigVersion(version.serviceName, version.tenantId, version.id, this.approvalComment)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.successMessage = 'Rejection recorded.';
        this.approvalComment = '';
        this.loadApprovals(version);
        this.loadVersions();
      });
  }

  activateVersion(version: ConfigVersion | null): void {
    if (!version) {
      this.errorMessage = 'Select a version to activate.';
      return;
    }
    this.resetMessages();
    this.adminService
      .activateConfigVersion(version.serviceName, version.tenantId, version.id, 'Activated via admin portal')
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.successMessage = 'Version activated.';
        this.loadVersions();
      });
  }

  formatConfig(config: Record<string, unknown> | null): string {
    if (!config) {
      return 'No config payload.';
    }
    return JSON.stringify(config, null, 2);
  }

  private loadApprovals(version: ConfigVersion): void {
    this.adminService
      .getConfigApprovals(version.serviceName, version.tenantId, version.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe((approvals) => {
        this.approvals = approvals;
      });
  }

  private resetMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
