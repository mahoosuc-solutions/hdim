import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

/**
 * Evaluation Flow Network Component (Placeholder)
 *
 * Future 3D visualization for CQL evaluation dependency graphs.
 */
@Component({
  selector: 'app-flow-network',
  imports: [CommonModule, MatCardModule, MatIconModule],
  template: `
    <div class="placeholder-container">
      <mat-card class="placeholder-card">
        <mat-card-content>
          <mat-icon class="placeholder-icon">account_tree</mat-icon>
          <h2>Evaluation Flow Network</h2>
          <p>CQL evaluation dependency graph visualization</p>

          <div class="icon-row">
            <div class="icon-chip">
              <mat-icon>settings_input_component</mat-icon>
              <span>Library Nodes</span>
            </div>
            <div class="icon-chip">
              <mat-icon>sync_alt</mat-icon>
              <span>Data Flows</span>
            </div>
            <div class="icon-chip">
              <mat-icon>analytics</mat-icon>
              <span>Metrics</span>
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
      padding: 2rem;
      padding-bottom: 2.5rem;
      max-width: 500px;
    }

    .placeholder-icon {
      font-size: 6rem;
      width: 6rem;
      height: 6rem;
      color: #ff9800;
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
      background: rgba(255, 152, 0, 0.2);
      border: 1px solid rgba(255, 152, 0, 0.4);
      border-radius: 24px;
      font-weight: 600;
      color: #ffb74d;
      text-transform: uppercase;
      letter-spacing: 1px;
    }

    .icon-row {
      display: flex;
      gap: 0.75rem;
      justify-content: center;
      margin-top: 1.25rem;
      flex-wrap: wrap;
    }

    .icon-chip {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.5rem 0.75rem;
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.08);
      color: #ffe0b2;
      font-weight: 500;

      mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }
    }
  `],
})
export class FlowNetworkComponent {}
