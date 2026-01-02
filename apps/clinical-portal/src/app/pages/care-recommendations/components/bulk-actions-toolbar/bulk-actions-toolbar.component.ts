import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-bulk-actions-toolbar',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatDividerModule,
  ],
  template: `
    <mat-toolbar class="bulk-actions-toolbar" color="primary">
      <span class="selected-count">{{ selectedCount }} selected</span>

      <mat-divider vertical></mat-divider>

      <button
        mat-button
        (click)="accept.emit()"
        matTooltip="Accept selected recommendations">
        <mat-icon>check_circle</mat-icon>
        Accept
      </button>

      <button
        mat-button
        (click)="decline.emit()"
        matTooltip="Decline selected recommendations">
        <mat-icon>cancel</mat-icon>
        Decline
      </button>

      <button
        mat-button
        (click)="complete.emit()"
        matTooltip="Mark selected as complete">
        <mat-icon>task_alt</mat-icon>
        Complete
      </button>

      <span class="spacer"></span>

      <button
        mat-icon-button
        (click)="clearSelection.emit()"
        matTooltip="Clear selection">
        <mat-icon>close</mat-icon>
      </button>
    </mat-toolbar>
  `,
  styles: [
    `
      .bulk-actions-toolbar {
        position: sticky;
        top: 0;
        z-index: 100;
        margin-bottom: 16px;
        border-radius: 4px;

        .selected-count {
          font-weight: 500;
          margin-right: 16px;
        }

        mat-divider {
          height: 24px;
          margin: 0 16px;
        }

        button {
          mat-icon {
            margin-right: 4px;
          }
        }

        .spacer {
          flex: 1;
        }
      }
    `,
  ],
})
export class BulkActionsToolbarComponent {
  @Input() selectedCount = 0;

  @Output() accept = new EventEmitter<void>();
  @Output() decline = new EventEmitter<void>();
  @Output() complete = new EventEmitter<void>();
  @Output() clearSelection = new EventEmitter<void>();
}
