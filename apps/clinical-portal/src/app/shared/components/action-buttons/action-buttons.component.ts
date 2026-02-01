/**
 * Action Buttons Component
 *
 * Displays a set of action buttons with icons and tooltips.
 * Responsive: inline on desktop, stacked on mobile.
 *
 * @example
 * <app-action-buttons
 *   [actions]="actionButtons"
 *   [layout]="'row'"
 *   (actionClicked)="handleAction($event)">
 * </app-action-buttons>
 */
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';

/**
 * Action button definition
 */
export interface ActionButton {
  /** Unique identifier */
  id: string;
  /** Display label */
  label: string;
  /** Material icon name */
  icon?: string;
  /** Button color theme */
  color?: 'primary' | 'accent' | 'warn';
  /** Is button disabled */
  disabled?: boolean;
  /** Tooltip text */
  tooltip?: string;
  /** Show in menu instead of inline */
  inMenu?: boolean;
}

@Component({
  selector: 'app-action-buttons',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatMenuModule
  ],
  template: `
    <div class="action-buttons" [class.layout-row]="layout === 'row'" [class.layout-column]="layout === 'column'">
      <!-- Inline buttons -->
      <ng-container *ngFor="let action of inlineActions">
        <button
          mat-raised-button
          [color]="action.color || 'primary'"
          [disabled]="action.disabled"
          [matTooltip]="action.tooltip || action.label"
          (click)="onActionClick(action)"
          class="action-button">
          <mat-icon *ngIf="action.icon">{{ action.icon }}</mat-icon>
          <span class="button-label">{{ action.label }}</span>
        </button>
      </ng-container>

      <!-- Menu button for overflow actions -->
      <button
        *ngIf="menuActions.length > 0"
        mat-icon-button
        [matMenuTriggerFor]="actionMenu"
        matTooltip="More actions"
        class="menu-button">
        <mat-icon>more_vert</mat-icon>
      </button>

      <mat-menu #actionMenu="matMenu">
        <button
          *ngFor="let action of menuActions"
          mat-menu-item
          [disabled]="action.disabled"
          (click)="onActionClick(action)">
          <mat-icon *ngIf="action.icon">{{ action.icon }}</mat-icon>
          <span>{{ action.label }}</span>
        </button>
      </mat-menu>
    </div>
  `,
  styles: [`
    .action-buttons {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .layout-row {
      flex-direction: row;
      align-items: center;
    }

    .layout-column {
      flex-direction: column;
      align-items: stretch;
    }

    .action-button {
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }

    .button-label {
      font-size: 14px;
    }

    .menu-button {
      flex-shrink: 0;
    }

    /* Responsive */
    @media (max-width: 600px) {
      .action-buttons.layout-row {
        flex-direction: column;
        align-items: stretch;
      }

      .action-button {
        width: 100%;
        justify-content: center;
      }

      .button-label {
        font-size: 13px;
      }
    }
  `]
})
export class ActionButtonsComponent {
  /** Array of action button definitions */
  @Input() actions: ActionButton[] = [];

  /** Layout direction */
  @Input() layout: 'row' | 'column' = 'row';

  /** Maximum inline buttons before overflow menu */
  @Input() maxInline = 5;

  /** Action clicked event */
  @Output() actionClicked = new EventEmitter<ActionButton>();

  get inlineActions(): ActionButton[] {
    return this.actions
      .filter(action => !action.inMenu)
      .slice(0, this.maxInline);
  }

  get menuActions(): ActionButton[] {
    const overflow = this.actions.filter(action => !action.inMenu).slice(this.maxInline);
    const menuItems = this.actions.filter(action => action.inMenu);
    return [...overflow, ...menuItems];
  }

  onActionClick(action: ActionButton): void {
    if (!action.disabled) {
      this.actionClicked.emit(action);
    }
  }
}
