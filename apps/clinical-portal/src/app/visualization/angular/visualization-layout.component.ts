import { Component, OnInit, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { LoggerService } from '../../../services/logger.service';
import { CommonModule } from '@angular/common';
import { LoggerService } from '../../../services/logger.service';
import { RouterModule } from '@angular/router';
import { LoggerService } from '../../../services/logger.service';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { LoggerService } from '../../../services/logger.service';

import { VisualizationNavComponent } from './visualization-nav.component';
import { LoggerService } from '../../../services/logger.service';
import { ThreeSceneService } from '../core/three-scene.service';
import { LoggerService } from '../../../services/logger.service';

/**
 * Visualization Layout Component
 *
 * Provides a shared layout wrapper for all 3D visualizations,
 * including the navigation component and common functionality.
 *
 * Features:
 * - Navigation integration
 * - Camera control handlers
 * - Screenshot capture
 * - Fullscreen management
 * - FPS display toggle
 * - Consistent dark theme
 */
@Component({
  selector: 'app-visualization-layout',
  imports: [
    CommonModule,
    RouterModule,
    MatSnackBarModule,
    VisualizationNavComponent,
  ],
  template: `
    <div class="visualization-layout">
      <!-- Navigation Component -->
      <app-visualization-nav
        (resetCamera)="handleResetCamera()"
        (toggleFullscreen)="handleToggleFullscreen()"
        (captureScreenshot)="handleCaptureScreenshot()"
        (toggleFPS)="handleToggleFPS($event)"
        (toggleVR)="handleToggleVR()"
        (modeChanged)="handleModeChanged($event)"
      ></app-visualization-nav>

      <!-- Visualization Content -->
      <div class="visualization-content" #visualizationContent>
        <router-outlet></router-outlet>
      </div>
    </div>
  `,
  styles: [`
    .visualization-layout {
      position: relative;
      width: 100%;
      height: calc(100vh - 64px); /* Account for toolbar */
      overflow: hidden;
      background: linear-gradient(135deg, #0a0e1a 0%, #1a1e2e 100%);
    }

    .visualization-content {
      width: 100%;
      height: 100%;
      overflow: hidden;
    }
  `],
})
export class VisualizationLayoutComponent implements OnInit, AfterViewInit {
  @ViewChild('visualizationContent') visualizationContent!: ElementRef<HTMLDivElement>;

  constructor(
    private loggerService: LoggerService,
    private sceneService: ThreeSceneService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.logger.info('Visualization Layout initialized');
  }

  ngAfterViewInit(): void {
    // Setup complete
  }

  /**
   * Handle reset camera action
   */
  handleResetCamera(): void {
    this.logger.info('Reset camera requested');
    this.sceneService.resetCamera();
    this.showNotification('Camera reset to default position');
  }

  /**
   * Handle fullscreen toggle
   */
  handleToggleFullscreen(): void {
    this.logger.info('Fullscreen toggle requested');
    // Fullscreen is handled in navigation component
  }

  /**
   * Handle screenshot capture
   */
  handleCaptureScreenshot(): void {
    this.logger.info('Screenshot capture requested');
    try {
      const canvas = this.sceneService.getRenderer()?.domElement;
      if (canvas) {
        // Convert canvas to blob
        canvas.toBlob((blob) => {
          if (blob) {
            // Create download link
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
            link.download = `visualization-${timestamp}.png`;
            link.href = url;
            link.click();
            URL.revokeObjectURL(url);

            this.showNotification('Screenshot captured successfully');
          }
        });
      } else {
        this.showNotification('No active visualization to capture', 'error');
      }
    } catch (error) {
      this.logger.error('Screenshot capture failed:', { error });
      this.showNotification('Failed to capture screenshot', 'error');
    }
  }

  /**
   * Handle FPS display toggle
   */
  handleToggleFPS(enabled: boolean): void {
    this.logger.info('FPS display toggle:', enabled);
    this.sceneService.toggleStats(enabled);
    this.showNotification(enabled ? 'FPS display enabled' : 'FPS display disabled');
  }

  /**
   * Handle VR mode toggle
   */
  handleToggleVR(): void {
    this.logger.info('VR mode requested');
    this.showNotification('VR mode coming soon!', 'info');
  }

  /**
   * Handle visualization mode change
   */
  handleModeChanged(modeId: string): void {
    this.logger.info('Visualization mode changed:', modeId);
    this.showNotification(`Switching to ${modeId}...`);
  }

  /**
   * Show notification to user
   */
  private showNotification(message: string, type: 'success' | 'error' | 'info' = 'success'): void {
    const config = {
      duration: 2000,
      horizontalPosition: 'right' as const,
      verticalPosition: 'top' as const,
      panelClass: [`snackbar-${type}`],
    };

    this.snackBar.open(message, 'Close', config);
  }
}
