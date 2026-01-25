/**
 * Grid Component
 *
 * Responsive CSS Grid layout with automatic breakpoints.
 *
 * @example
 * <app-grid [columns]="3" [gap]="16">
 *   <div>Item 1</div>
 *   <div>Item 2</div>
 *   <div>Item 3</div>
 * </app-grid>
 */
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-grid',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="grid" [style.gap.px]="gap" [attr.data-columns]="columns">
      <ng-content></ng-content>
    </div>
  `,
  styles: [`
    .grid {
      display: grid;
      width: 100%;
    }

    /* 1 column */
    .grid[data-columns="1"] {
      grid-template-columns: repeat(1, 1fr);
    }

    /* 2 columns */
    .grid[data-columns="2"] {
      grid-template-columns: repeat(2, 1fr);
    }

    @media (max-width: 600px) {
      .grid[data-columns="2"] {
        grid-template-columns: 1fr;
      }
    }

    /* 3 columns */
    .grid[data-columns="3"] {
      grid-template-columns: repeat(3, 1fr);
    }

    @media (max-width: 960px) {
      .grid[data-columns="3"] {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    @media (max-width: 600px) {
      .grid[data-columns="3"] {
        grid-template-columns: 1fr;
      }
    }

    /* 4 columns */
    .grid[data-columns="4"] {
      grid-template-columns: repeat(4, 1fr);
    }

    @media (max-width: 1280px) {
      .grid[data-columns="4"] {
        grid-template-columns: repeat(3, 1fr);
      }
    }

    @media (max-width: 960px) {
      .grid[data-columns="4"] {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    @media (max-width: 600px) {
      .grid[data-columns="4"] {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class GridComponent {
  @Input() columns: 1 | 2 | 3 | 4 = 3;
  @Input() gap = 16;
}
