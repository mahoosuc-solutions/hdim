import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { LoadingButtonComponent } from '../../../shared/components/loading-button/loading-button.component';

export interface PublishConfirmDialogData {
  measureId: string;
  measureName: string;
  currentVersion: string;
}

@Component({
  selector: 'app-publish-confirm-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    LoadingButtonComponent,
  ],
  template: `
    <div class="publish-dialog">
      <!-- Header -->
      <h2 mat-dialog-title>
        <mat-icon color="primary">cloud_upload</mat-icon>
        Publish Measure
      </h2>

      <!-- Content -->
      <mat-dialog-content class="dialog-content">
        <!-- Measure Info -->
        <div class="measure-info">
          <h3>{{ data.measureName }}</h3>
          <p>Current Version: <strong>{{ data.currentVersion || '1.0.0' }}</strong></p>
        </div>

        <!-- Warning Message -->
        <div class="warning-box">
          <mat-icon>warning</mat-icon>
          <div>
            <h4>Publishing will make this measure available for production use</h4>
            <p>Once published, this measure will be available in the Evaluations module and can be run against live patient data.</p>
          </div>
        </div>

        <!-- Form -->
        <form #publishForm="ngForm">
          <!-- New Version -->
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>New Version</mat-label>
            <input
              matInput
              [(ngModel)]="newVersion"
              name="newVersion"
              required
              placeholder="e.g., 1.1.0"
              pattern="^[0-9]+\\.[0-9]+\\.[0-9]+$"
              #versionInput="ngModel">
            <mat-hint>Use semantic versioning (MAJOR.MINOR.PATCH)</mat-hint>
            @if (versionInput.invalid && versionInput.touched) {
              <mat-error>
                @if (versionInput.errors?.['required']) {
                  Version is required
                }
                @if (versionInput.errors?.['pattern']) {
                  Invalid version format (use x.y.z)
                }
              </mat-error>
            }
          </mat-form-field>

          <!-- Release Notes -->
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Release Notes</mat-label>
            <textarea
              matInput
              [(ngModel)]="releaseNotes"
              name="releaseNotes"
              required
              rows="4"
              placeholder="Describe the changes and improvements in this version"
              #notesInput="ngModel"></textarea>
            <mat-hint>Explain what's new or changed in this version</mat-hint>
            @if (notesInput.invalid && notesInput.touched) {
              <mat-error>Release notes are required</mat-error>
            }
          </mat-form-field>

          <!-- Checklist -->
          <div class="checklist">
            <h4>Pre-publish Checklist</h4>
            <div class="checklist-item">
              <mat-checkbox [(ngModel)]="cqlTested" name="cqlTested">
                CQL logic has been tested against sample patients
              </mat-checkbox>
            </div>
            <div class="checklist-item">
              <mat-checkbox [(ngModel)]="valueSetsVerified" name="valueSetsVerified">
                Value sets have been verified and are up to date
              </mat-checkbox>
            </div>
            <div class="checklist-item">
              <mat-checkbox [(ngModel)]="documentationComplete" name="documentationComplete">
                Documentation and measure specifications are complete
              </mat-checkbox>
            </div>
            <div class="checklist-item">
              <mat-checkbox [(ngModel)]="peerReviewed" name="peerReviewed">
                Measure has been peer reviewed and approved
              </mat-checkbox>
            </div>
          </div>
        </form>

        <!-- Info Message -->
        <div class="info-box">
          <mat-icon>info</mat-icon>
          <p>Published measures can be versioned and updated, but previous versions remain immutable for audit purposes.</p>
        </div>
      </mat-dialog-content>

      <!-- Actions -->
      <mat-dialog-actions align="end">
        <button mat-button mat-dialog-close>
          <mat-icon>close</mat-icon>
          Cancel
        </button>
        <app-loading-button
          text="Publish Measure"
          icon="cloud_upload"
          color="primary"
          variant="raised"
          [disabled]="!canPublish()"
          [loading]="publishing"
          loadingText="Publishing..."
          successText="Published!"
          ariaLabel="Publish measure"
          (buttonClick)="publish()">
        </app-loading-button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .publish-dialog {
      display: flex;
      flex-direction: column;
      max-height: 90vh;
    }

    h2 {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0;
      padding: 16px 24px;
      background-color: #f5f5f5;
      border-bottom: 1px solid #e0e0e0;

      mat-icon {
        font-size: 28px;
        width: 28px;
        height: 28px;
      }
    }

    .dialog-content {
      padding: 24px;
      overflow-y: auto;
    }

    .measure-info {
      margin-bottom: 24px;

      h3 {
        margin: 0 0 8px 0;
        font-size: 18px;
        font-weight: 500;
      }

      p {
        margin: 0;
        color: #666;

        strong {
          color: #333;
          font-family: monospace;
        }
      }
    }

    .warning-box {
      display: flex;
      gap: 12px;
      padding: 16px;
      background-color: #fff3e0;
      border-left: 4px solid #ff9800;
      border-radius: 4px;
      margin-bottom: 24px;

      mat-icon {
        color: #e65100;
        font-size: 24px;
        width: 24px;
        height: 24px;
        flex-shrink: 0;
      }

      h4 {
        margin: 0 0 8px 0;
        font-size: 14px;
        font-weight: 600;
        color: #e65100;
      }

      p {
        margin: 0;
        font-size: 13px;
        color: #666;
      }
    }

    .info-box {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      background-color: #e3f2fd;
      border-radius: 4px;
      margin-top: 16px;

      mat-icon {
        color: #1976d2;
        font-size: 20px;
        width: 20px;
        height: 20px;
        flex-shrink: 0;
      }

      p {
        margin: 0;
        font-size: 13px;
        color: #555;
      }
    }

    .full-width {
      width: 100%;
      margin-bottom: 16px;
    }

    .checklist {
      margin: 24px 0;
      padding: 16px;
      background-color: #f5f5f5;
      border-radius: 4px;

      h4 {
        margin: 0 0 16px 0;
        font-size: 14px;
        font-weight: 600;
        color: #555;
      }

      .checklist-item {
        margin-bottom: 12px;

        &:last-child {
          margin-bottom: 0;
        }

        mat-checkbox {
          font-size: 14px;
        }
      }
    }

    mat-dialog-actions {
      padding: 16px 24px;
      border-top: 1px solid #e0e0e0;
      gap: 8px;
    }

    :host ::ng-deep .mat-mdc-dialog-container {
      padding: 0;
    }
  `],
})
export class PublishConfirmDialogComponent {
  newVersion = '';
  releaseNotes = '';
  cqlTested = false;
  valueSetsVerified = false;
  documentationComplete = false;
  peerReviewed = false;
  publishing = false;

  constructor(
    private dialogRef: MatDialogRef<PublishConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: PublishConfirmDialogData
  ) {
    // Suggest next version
    this.newVersion = this.suggestNextVersion(data.currentVersion);
  }

  /**
   * Suggest next version based on current version
   */
  private suggestNextVersion(currentVersion: string): string {
    if (!currentVersion) return '1.0.0';

    const parts = currentVersion.split('.');
    if (parts.length !== 3) return '1.0.0';

    const [major, minor, patch] = parts.map(Number);
    return `${major}.${minor}.${patch + 1}`;
  }

  /**
   * Check if all requirements are met for publishing
   */
  canPublish(): boolean {
    const versionValid = /^[0-9]+\.[0-9]+\.[0-9]+$/.test(this.newVersion);
    const notesValid = this.releaseNotes.trim().length > 0;
    const checklistComplete =
      this.cqlTested &&
      this.valueSetsVerified &&
      this.documentationComplete &&
      this.peerReviewed;

    return versionValid && notesValid && checklistComplete;
  }

  /**
   * Publish the measure
   */
  publish(): void {
    if (!this.canPublish()) return;

    this.publishing = true;

    // Simulate API call
    setTimeout(() => {
      this.publishing = false;
      this.dialogRef.close(true);
    }, 2000);
  }
}
