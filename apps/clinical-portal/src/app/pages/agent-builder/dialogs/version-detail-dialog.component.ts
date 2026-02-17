import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';

import { AgentVersion } from '../models/agent.model';

export interface VersionDetailDialogData {
  version: AgentVersion;
}

@Component({
  selector: 'app-version-detail-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatChipsModule, MatDividerModule],
  template: `
    <div class="version-detail-dialog">
      <div class="header">
        <h2>
          <mat-icon>history</mat-icon>
          Version Details
        </h2>
        <button mat-icon-button (click)="onClose()" aria-label="Close dialog">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <mat-divider></mat-divider>

      <div class="content">
        <div class="meta-row">
          <span class="label">Version</span>
          <mat-chip>{{ data.version.versionNumber }}</mat-chip>
        </div>
        <div class="meta-row">
          <span class="label">Status</span>
          <mat-chip>{{ data.version.status }}</mat-chip>
        </div>
        <div class="meta-row">
          <span class="label">Change Type</span>
          <mat-chip>{{ data.version.changeType }}</mat-chip>
        </div>
        <div class="meta-row">
          <span class="label">Created By</span>
          <span>{{ data.version.createdBy }}</span>
        </div>
        <div class="meta-row">
          <span class="label">Created At</span>
          <span>{{ data.version.createdAt | date: 'medium' }}</span>
        </div>
        <div class="meta-row">
          <span class="label">Summary</span>
          <span>{{ data.version.changeSummary || 'No summary provided' }}</span>
        </div>

        <h3>Configuration Snapshot</h3>
        <pre>{{ prettySnapshot }}</pre>
      </div>

      <mat-divider></mat-divider>

      <div class="footer">
        <button mat-button (click)="onClose()">Close</button>
      </div>
    </div>
  `,
  styles: [`
    .version-detail-dialog {
      width: min(1000px, 92vw);
      max-height: 85vh;
      display: flex;
      flex-direction: column;
    }

    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 20px;
    }

    .header h2 {
      margin: 0;
      display: flex;
      gap: 8px;
      align-items: center;
    }

    .content {
      padding: 16px 20px;
      overflow: auto;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .meta-row {
      display: grid;
      grid-template-columns: 140px 1fr;
      align-items: center;
      gap: 10px;
    }

    .label {
      font-weight: 600;
      color: var(--mat-sys-on-surface-variant);
    }

    h3 {
      margin: 14px 0 8px;
    }

    pre {
      margin: 0;
      padding: 12px;
      border-radius: 8px;
      background: var(--mat-sys-surface-variant);
      color: var(--mat-sys-on-surface-variant);
      font-size: 12px;
      line-height: 1.4;
      overflow: auto;
      white-space: pre-wrap;
      word-break: break-word;
    }

    .footer {
      padding: 12px 20px;
      display: flex;
      justify-content: flex-end;
    }
  `],
})
export class VersionDetailDialogComponent {
  prettySnapshot: string;

  constructor(
    private dialogRef: MatDialogRef<VersionDetailDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: VersionDetailDialogData
  ) {
    this.prettySnapshot = JSON.stringify(data.version.configurationSnapshot || {}, null, 2);
  }

  onClose(): void {
    this.dialogRef.close();
  }
}
