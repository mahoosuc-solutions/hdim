import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

/**
 * Measure Matrix Component (Placeholder)
 *
 * Future 3D visualization for patient-measure performance heatmap.
 */
@Component({
  selector: 'app-measure-matrix',
  imports: [CommonModule, MatCardModule, MatIconModule],
  template: `
    <div class="placeholder-container">
      <mat-card class="placeholder-card">
        <mat-card-content>
          <mat-icon class="placeholder-icon">grid_on</mat-icon>
          <h2>Measure Matrix</h2>
          <p>Patient-measure performance heatmap visualization</p>

          <div class="icon-grid">
            <div class="icon-chip success">
              <mat-icon>check_circle</mat-icon>
              <span>Compliant</span>
            </div>
            <div class="icon-chip warn">
              <mat-icon>error_outline</mat-icon>
              <span>Gaps</span>
            </div>
            <div class="icon-chip neutral">
              <mat-icon>insights</mat-icon>
              <span>Trends</span>
            </div>
            <div class="icon-chip accent">
              <mat-icon>heat_pump</mat-icon>
              <span>Heatmap</span>
            </div>
          </div>

          <p class="coming-soon">Interactive view coming soon</p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .placeholder-container {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100vh;
      background: transparent;
    }

    .placeholder-card {
      background: rgba(255, 255, 255, 0.05);
      backdrop-filter: blur(10px);
      border: 1px solid rgba(255, 255, 255, 0.1);
      text-align: center;
      padding: 2rem 2rem 2.5rem;
      max-width: 500px;
    }

    .placeholder-icon {
      font-size: 6rem;
      width: 6rem;
      height: 6rem;
      color: #4caf50;
      margin-bottom: 1rem;
    }

    h2 {
      color: #ffffff;
      font-size: 2rem;
      margin-bottom: 1rem;
    }

    p {
      color: rgba(255, 255, 255, 0.7);
      font-size: 1.125rem;
      margin: 0.5rem 0;
    }

    .coming-soon {
      display: inline-block;
      margin-top: 1.25rem;
      padding: 0.5rem 1.5rem;
      background: rgba(76, 175, 80, 0.2);
      border: 1px solid rgba(76, 175, 80, 0.4);
      border-radius: 24px;
      font-weight: 600;
      color: #81c784;
      text-transform: uppercase;
      letter-spacing: 1px;
    }

    .icon-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
      gap: 0.75rem;
      margin-top: 1.25rem;
    }

    .icon-chip {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      padding: 0.6rem 0.75rem;
      border-radius: 12px;
      border: 1px solid rgba(255, 255, 255, 0.08);
      background: rgba(255, 255, 255, 0.05);
      color: #e0f2f1;
      font-weight: 600;

      mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }
    }

    .icon-chip.success {
      color: #81c784;
      border-color: rgba(129, 199, 132, 0.4);
    }

    .icon-chip.warn {
      color: #ffb74d;
      border-color: rgba(255, 183, 77, 0.4);
    }

    .icon-chip.neutral {
      color: #90caf9;
      border-color: rgba(144, 202, 249, 0.4);
    }

    .icon-chip.accent {
      color: #f48fb1;
      border-color: rgba(244, 143, 177, 0.4);
    }
  `],
})
export class MeasureMatrixComponent {}
