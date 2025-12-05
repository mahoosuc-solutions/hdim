/**
 * Container Component
 *
 * Responsive width container with consistent padding.
 * Used as wrapper for page content.
 *
 * @example
 * <app-container [maxWidth]="'1400px'" [padding]="'normal'">
 *   <!-- Page content here -->
 * </app-container>
 */
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-container',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container" [ngClass]="paddingClass" [style.max-width]="maxWidth">
      <ng-content></ng-content>
    </div>
  `,
  styles: [`
    .container {
      width: 100%;
      margin: 0 auto;
    }

    .padding-small {
      padding: 12px;
    }

    .padding-normal {
      padding: 24px;
    }

    .padding-large {
      padding: 32px;
    }

    /* Responsive padding */
    @media (max-width: 960px) {
      .padding-normal {
        padding: 16px;
      }

      .padding-large {
        padding: 24px;
      }
    }

    @media (max-width: 600px) {
      .padding-small {
        padding: 8px;
      }

      .padding-normal {
        padding: 12px;
      }

      .padding-large {
        padding: 16px;
      }
    }
  `]
})
export class ContainerComponent {
  /** Maximum width */
  @Input() maxWidth: string = '1400px';

  /** Padding size */
  @Input() padding: 'small' | 'normal' | 'large' = 'normal';

  get paddingClass(): string {
    return `padding-${this.padding}`;
  }
}
