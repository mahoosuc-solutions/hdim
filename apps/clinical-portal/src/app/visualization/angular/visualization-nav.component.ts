import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { trigger, state, style, transition, animate } from '@angular/animations';

/**
 * Visualization Mode Configuration
 */
export interface VisualizationMode {
  id: string;
  label: string;
  icon: string;
  route: string;
  description: string;
  enabled: boolean;
  color: string;
  comingSoon?: boolean;
}

/**
 * Camera Transition Options
 */
export interface CameraTransitionOptions {
  duration: number;
  easing: 'linear' | 'ease-in' | 'ease-out' | 'ease-in-out';
  preservePosition: boolean;
}

/**
 * Visualization Navigation Component
 *
 * Provides a floating navigation interface for switching between
 * different 3D visualization modes with smooth transitions and
 * common toolbar controls.
 *
 * Features:
 * - Mode switching with visual feedback
 * - Camera control tools (reset, fullscreen, screenshot)
 * - Performance monitoring (FPS toggle)
 * - Dark theme with glassmorphism design
 * - Responsive and non-obstructive layout
 * - Future VR mode support
 */
@Component({
  selector: 'app-visualization-nav',
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatMenuModule,
    MatDividerModule,
    MatSlideToggleModule,
  ],
  templateUrl: './visualization-nav.component.html',
  styleUrl: './visualization-nav.component.scss',
  animations: [
    trigger('slideIn', [
      state('collapsed', style({
        transform: 'translateX(-100%)',
        opacity: 0,
      })),
      state('expanded', style({
        transform: 'translateX(0)',
        opacity: 1,
      })),
      transition('collapsed <=> expanded', animate('300ms ease-in-out')),
    ]),
    trigger('fadeIn', [
      transition(':enter', [
        style({ opacity: 0, transform: 'scale(0.9)' }),
        animate('200ms ease-out', style({ opacity: 1, transform: 'scale(1)' })),
      ]),
      transition(':leave', [
        animate('150ms ease-in', style({ opacity: 0, transform: 'scale(0.9)' })),
      ]),
    ]),
  ],
})
export class VisualizationNavComponent implements OnInit {
  // Event emitters for toolbar actions
  @Output() resetCamera = new EventEmitter<void>();
  @Output() toggleFullscreen = new EventEmitter<void>();
  @Output() captureScreenshot = new EventEmitter<void>();
  @Output() toggleFPS = new EventEmitter<boolean>();
  @Output() toggleVR = new EventEmitter<void>();
  @Output() modeChanged = new EventEmitter<string>();

  // UI state
  isExpanded = true;
  isFullscreen = false;
  showFPS = false;
  isTransitioning = false;
  currentModeId = 'live-monitor';

  // Visualization modes
  visualizationModes: VisualizationMode[] = [
    {
      id: 'live-monitor',
      label: 'Live Batch Monitor',
      icon: 'show_chart',
      route: '/visualization/live-monitor',
      description: 'Real-time CQL evaluation batch progress',
      enabled: true,
      color: '#2196f3',
    },
    {
      id: 'quality-constellation',
      label: 'Quality Constellation',
      icon: 'scatter_plot',
      route: '/visualization/quality-constellation',
      description: 'Multi-dimensional measure quality analysis',
      enabled: true,
      color: '#9c27b0',
      comingSoon: false,
    },
    {
      id: 'flow-network',
      label: 'Evaluation Flow Network',
      icon: 'account_tree',
      route: '/visualization/flow-network',
      description: 'CQL evaluation dependency graph',
      enabled: false,
      color: '#ff9800',
      comingSoon: true,
    },
    {
      id: 'measure-matrix',
      label: 'Measure Matrix',
      icon: 'grid_on',
      route: '/visualization/measure-matrix',
      description: 'Patient-measure performance heatmap',
      enabled: false,
      color: '#4caf50',
      comingSoon: true,
    },
  ];

  // Camera transition settings
  cameraTransition: CameraTransitionOptions = {
    duration: 1000,
    easing: 'ease-in-out',
    preservePosition: false,
  };

  constructor(private router: Router) {}

  ngOnInit(): void {
    // Detect current mode from route
    this.detectCurrentMode();

    // Listen for fullscreen changes
    document.addEventListener('fullscreenchange', () => {
      this.isFullscreen = !!document.fullscreenElement;
    });
  }

  /**
   * Detect current visualization mode from route
   */
  private detectCurrentMode(): void {
    const currentRoute = this.router.url;
    const mode = this.visualizationModes.find(m => currentRoute.includes(m.id));
    if (mode) {
      this.currentModeId = mode.id;
    }
  }

  /**
   * Switch to a different visualization mode
   */
  switchMode(mode: VisualizationMode): void {
    if (!mode.enabled || mode.id === this.currentModeId || this.isTransitioning) {
      return;
    }

    // Start transition
    this.isTransitioning = true;
    this.currentModeId = mode.id;

    // Emit mode change event
    this.modeChanged.emit(mode.id);

    // Navigate to new mode with transition delay
    setTimeout(() => {
      this.router.navigate([mode.route]).then(() => {
        setTimeout(() => {
          this.isTransitioning = false;
        }, this.cameraTransition.duration);
      });
    }, 300);
  }

  /**
   * Get current visualization mode
   */
  getCurrentMode(): VisualizationMode {
    return this.visualizationModes.find(m => m.id === this.currentModeId) || this.visualizationModes[0];
  }

  /**
   * Check if mode is active
   */
  isModeActive(modeId: string): boolean {
    return this.currentModeId === modeId;
  }

  /**
   * Toggle navigation panel expansion
   */
  toggleExpansion(): void {
    this.isExpanded = !this.isExpanded;
  }

  /**
   * Handle reset camera action
   */
  onResetCamera(): void {
    this.resetCamera.emit();
  }

  /**
   * Handle fullscreen toggle
   */
  onToggleFullscreen(): void {
    this.toggleFullscreen.emit();

    const elem = document.documentElement;
    if (!this.isFullscreen) {
      if (elem.requestFullscreen) {
        elem.requestFullscreen();
      }
    } else {
      if (document.exitFullscreen) {
        document.exitFullscreen();
      }
    }
  }

  /**
   * Handle screenshot capture
   */
  onCaptureScreenshot(): void {
    this.captureScreenshot.emit();
  }

  /**
   * Handle FPS display toggle
   */
  onToggleFPS(enabled: boolean): void {
    this.showFPS = enabled;
    this.toggleFPS.emit(enabled);
  }

  /**
   * Handle VR mode toggle
   */
  onToggleVR(): void {
    this.toggleVR.emit();
  }

  /**
   * Toggle camera position preservation
   */
  togglePreservePosition(): void {
    this.cameraTransition.preservePosition = !this.cameraTransition.preservePosition;
  }

  /**
   * Get icon for fullscreen button
   */
  getFullscreenIcon(): string {
    return this.isFullscreen ? 'fullscreen_exit' : 'fullscreen';
  }

  /**
   * Get tooltip for fullscreen button
   */
  getFullscreenTooltip(): string {
    return this.isFullscreen ? 'Exit Fullscreen' : 'Enter Fullscreen';
  }
}
