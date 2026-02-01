/**
 * Card Component
 *
 * Wrapper around Material mat-card with consistent styling.
 *
 * @example
 * <app-card [title]="'Patient Information'" [bordered]="true" [padding]="'normal'">
 *   <!-- Card content here -->
 * </app-card>
 */
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-card',
  standalone: true,
  imports: [CommonModule, MatCardModule],
  template: `
    <mat-card [class.bordered]="bordered" [ngClass]="paddingClass">
      <mat-card-header *ngIf="title || subtitle">
        <mat-card-title *ngIf="title">{{ title }}</mat-card-title>
        <mat-card-subtitle *ngIf="subtitle">{{ subtitle }}</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <ng-content></ng-content>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    mat-card {
      margin-bottom: 16px;
    }

    .bordered {
      border: 1px solid #e0e0e0;
    }

    .padding-small ::ng-deep mat-card-content {
      padding: 12px;
    }

    .padding-normal ::ng-deep mat-card-content {
      padding: 16px;
    }

    .padding-large ::ng-deep mat-card-content {
      padding: 24px;
    }

    /* Dark theme */
    @media (prefers-color-scheme: dark) {
      .bordered {
        border-color: #424242;
      }
    }
  `]
})
export class CardComponent {
  @Input() title?: string;
  @Input() subtitle?: string;
  @Input() bordered = false;
  @Input() padding: 'small' | 'normal' | 'large' = 'normal';

  get paddingClass(): string {
    return `padding-${this.padding}`;
  }
}
