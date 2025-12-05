import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { FormsModule } from '@angular/forms';
import { LoadingButtonComponent } from '../../../shared/components/loading-button/loading-button.component';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';

export interface CqlEditorDialogData {
  measureId: string;
  measureName: string;
  cqlText: string;
  readOnly?: boolean;
}

@Component({
  selector: 'app-cql-editor-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatSlideToggleModule,
    LoadingButtonComponent,
    MonacoEditorModule,
  ],
  template: `
    <div class="editor-dialog">
      <!-- Header -->
      <div class="dialog-header">
        <div class="header-left">
          <mat-icon color="primary">code</mat-icon>
          <div>
            <h2>CQL Editor</h2>
            <p class="measure-name">{{ data.measureName }}</p>
          </div>
        </div>
        <div class="header-actions">
          <button
            mat-icon-button
            [matTooltip]="isFullScreen ? 'Exit Full Screen' : 'Full Screen'"
            (click)="toggleFullScreen()">
            <mat-icon>{{ isFullScreen ? 'fullscreen_exit' : 'fullscreen' }}</mat-icon>
          </button>
          <button
            mat-icon-button
            matTooltip="Close"
            (click)="cancel()">
            <mat-icon>close</mat-icon>
          </button>
        </div>
      </div>

      <!-- Editor Controls -->
      <div class="editor-controls">
        <div class="control-group">
          <mat-slide-toggle
            [(ngModel)]="showLineNumbers"
            (change)="updateEditorOptions()">
            Line Numbers
          </mat-slide-toggle>
          <mat-slide-toggle
            [(ngModel)]="wordWrap"
            (change)="updateEditorOptions()">
            Word Wrap
          </mat-slide-toggle>
          <mat-slide-toggle
            [(ngModel)]="minimap"
            (change)="updateEditorOptions()">
            Minimap
          </mat-slide-toggle>
        </div>
        <div class="status-info">
          <span class="status-badge" [class.readonly]="data.readOnly">
            {{ data.readOnly ? 'Read-Only' : 'Editable' }}
          </span>
        </div>
      </div>

      <!-- Monaco Editor -->
      <div class="editor-container">
        <ngx-monaco-editor
          [(ngModel)]="cqlText"
          [options]="editorOptions"
          class="monaco-editor">
        </ngx-monaco-editor>
      </div>

      <!-- Footer Actions -->
      <div class="dialog-footer">
        <div class="footer-info">
          <mat-icon>info</mat-icon>
          <span>Changes are auto-saved when you click Save</span>
        </div>
        <div class="footer-actions">
          <button mat-button (click)="cancel()">
            <mat-icon>close</mat-icon>
            Cancel
          </button>
          <app-loading-button
            text="Save CQL"
            icon="save"
            color="primary"
            variant="raised"
            [disabled]="data.readOnly || !hasChanges()"
            ariaLabel="Save CQL changes"
            (buttonClick)="save()">
          </app-loading-button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .editor-dialog {
      display: flex;
      flex-direction: column;
      height: 100%;
      max-height: 85vh;
    }

    .dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 24px;
      background-color: #f5f5f5;
      border-bottom: 1px solid #e0e0e0;

      .header-left {
        display: flex;
        align-items: center;
        gap: 12px;

        mat-icon {
          font-size: 32px;
          width: 32px;
          height: 32px;
        }

        h2 {
          margin: 0;
          font-size: 20px;
          font-weight: 500;
        }

        .measure-name {
          margin: 4px 0 0 0;
          color: #666;
          font-size: 13px;
        }
      }

      .header-actions {
        display: flex;
        gap: 4px;
      }
    }

    .editor-controls {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 24px;
      background-color: #fafafa;
      border-bottom: 1px solid #e0e0e0;

      .control-group {
        display: flex;
        gap: 24px;
      }

      .status-badge {
        display: inline-block;
        padding: 4px 12px;
        border-radius: 12px;
        background-color: #e8f5e9;
        color: #2e7d32;
        font-size: 12px;
        font-weight: 500;

        &.readonly {
          background-color: #fff3e0;
          color: #e65100;
        }
      }
    }

    .editor-container {
      flex: 1;
      min-height: 0;
      position: relative;

      .monaco-editor {
        height: 100%;
        width: 100%;
      }
    }

    .dialog-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 24px;
      border-top: 1px solid #e0e0e0;
      background-color: #f5f5f5;

      .footer-info {
        display: flex;
        align-items: center;
        gap: 8px;
        color: #666;
        font-size: 13px;

        mat-icon {
          font-size: 18px;
          width: 18px;
          height: 18px;
        }
      }

      .footer-actions {
        display: flex;
        gap: 8px;
      }
    }

    :host ::ng-deep .mat-mdc-dialog-container {
      padding: 0;
    }
  `],
})
export class CqlEditorDialogComponent implements OnInit {
  cqlText: string;
  originalCqlText: string;
  isFullScreen = false;
  showLineNumbers = true;
  wordWrap = false;
  minimap = true;

  editorOptions: any = {
    theme: 'vs',
    language: 'sql', // Using SQL as closest to CQL syntax
    automaticLayout: true,
    fontSize: 14,
    lineNumbers: 'on',
    wordWrap: 'off',
    minimap: { enabled: true },
    readOnly: false,
    scrollBeyondLastLine: false,
    renderWhitespace: 'selection',
    tabSize: 2,
    insertSpaces: true,
  };

  constructor(
    private dialogRef: MatDialogRef<CqlEditorDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CqlEditorDialogData
  ) {
    this.cqlText = data.cqlText || this.getDefaultCqlTemplate();
    this.originalCqlText = this.cqlText;
  }

  ngOnInit(): void {
    if (this.data.readOnly) {
      this.editorOptions.readOnly = true;
    }
  }

  /**
   * Get default CQL template for new measures
   */
  private getDefaultCqlTemplate(): string {
    return `library ${this.data.measureName.replace(/\s+/g, '')} version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

// Value sets
codesystem "LOINC": 'http://loinc.org'
codesystem "SNOMEDCT": 'http://snomed.info/sct'

// Parameters
parameter "Measurement Period" Interval<DateTime>

// Define your measure logic here
context Patient

define "Initial Population":
  true

define "Denominator":
  "Initial Population"

define "Numerator":
  "Denominator"
`;
  }

  /**
   * Update editor options based on toggles
   */
  updateEditorOptions(): void {
    this.editorOptions = {
      ...this.editorOptions,
      lineNumbers: this.showLineNumbers ? 'on' : 'off',
      wordWrap: this.wordWrap ? 'on' : 'off',
      minimap: { enabled: this.minimap },
    };
  }

  /**
   * Toggle full screen mode
   */
  toggleFullScreen(): void {
    this.isFullScreen = !this.isFullScreen;
    const dialogElement = document.querySelector('.mat-mdc-dialog-container') as HTMLElement;
    if (dialogElement) {
      if (this.isFullScreen) {
        dialogElement.style.maxWidth = '100vw';
        dialogElement.style.maxHeight = '100vh';
        dialogElement.style.width = '100vw';
        dialogElement.style.height = '100vh';
      } else {
        dialogElement.style.maxWidth = '1400px';
        dialogElement.style.maxHeight = '85vh';
        dialogElement.style.width = '90vw';
        dialogElement.style.height = '85vh';
      }
    }
  }

  /**
   * Check if there are unsaved changes
   */
  hasChanges(): boolean {
    return this.cqlText !== this.originalCqlText;
  }

  /**
   * Save CQL changes
   */
  save(): void {
    if (!this.hasChanges() || this.data.readOnly) return;
    this.dialogRef.close(this.cqlText);
  }

  /**
   * Cancel and close dialog
   */
  cancel(): void {
    if (this.hasChanges()) {
      if (confirm('You have unsaved changes. Are you sure you want to close?')) {
        this.dialogRef.close();
      }
    } else {
      this.dialogRef.close();
    }
  }
}
