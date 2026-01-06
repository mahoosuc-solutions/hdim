import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Subject, takeUntil, combineLatest } from 'rxjs';
import {
  GuidedTourService,
  TourState,
  TourStep,
} from '../../services/guided-tour.service';

/**
 * Tour Overlay Component
 *
 * Displays the tour step content as an overlay positioned near the highlighted element.
 * Includes navigation controls, progress indicator, and keyboard navigation.
 *
 * Issue #160: Provider Dashboard Help System
 */
@Component({
  selector: 'app-tour-overlay',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    @if (isVisible && currentStep) {
      <div class="tour-overlay" [class]="'position-' + currentStep.position">
        <div class="tour-content" [style.top.px]="overlayTop" [style.left.px]="overlayLeft">
          <!-- Progress bar -->
          <mat-progress-bar
            mode="determinate"
            [value]="progressPercent"
            color="primary">
          </mat-progress-bar>

          <!-- Header -->
          <div class="tour-header">
            <div class="step-indicator">
              Step {{ currentStepIndex + 1 }} of {{ totalSteps }}
            </div>
            <button mat-icon-button class="close-btn" (click)="skipTour()" aria-label="Skip tour">
              <mat-icon>close</mat-icon>
            </button>
          </div>

          <!-- Content -->
          <div class="tour-body">
            <h3 class="tour-title">{{ currentStep.title }}</h3>
            <p class="tour-description">{{ currentStep.content }}</p>
          </div>

          <!-- Navigation -->
          <div class="tour-navigation">
            <button
              mat-stroked-button
              (click)="previousStep()"
              [disabled]="currentStepIndex === 0">
              <mat-icon>chevron_left</mat-icon>
              Back
            </button>

            <div class="step-dots">
              @for (step of steps; track step.id; let i = $index) {
                <span
                  class="dot"
                  [class.active]="i === currentStepIndex"
                  [class.completed]="i < currentStepIndex"
                  (click)="goToStep(i)">
                </span>
              }
            </div>

            @if (currentStepIndex < totalSteps - 1) {
              <button mat-raised-button color="primary" (click)="nextStep()">
                Next
                <mat-icon>chevron_right</mat-icon>
              </button>
            } @else {
              <button mat-raised-button color="primary" (click)="completeTour()">
                Done
                <mat-icon>check</mat-icon>
              </button>
            }
          </div>

          <!-- Keyboard hint -->
          <div class="keyboard-hint">
            <kbd>←</kbd> / <kbd>→</kbd> navigate &nbsp;&nbsp; <kbd>Esc</kbd> skip
          </div>

          <!-- Arrow pointer -->
          <div class="tour-arrow" [class]="'arrow-' + currentStep.position"></div>
        </div>
      </div>
    }
  `,
  styles: [`
    .tour-overlay {
      position: fixed;
      z-index: 10000;
      pointer-events: none;
    }

    .tour-content {
      position: fixed;
      width: 380px;
      background: white;
      border-radius: 12px;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
      pointer-events: auto;
      animation: slideIn 0.3s ease;
      overflow: hidden;
    }

    @keyframes slideIn {
      from {
        opacity: 0;
        transform: translateY(10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    mat-progress-bar {
      height: 4px;
    }

    .tour-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 16px 8px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
    }

    .step-indicator {
      font-size: 12px;
      font-weight: 600;
      opacity: 0.9;
    }

    .close-btn {
      color: white;
      width: 32px;
      height: 32px;
      line-height: 32px;

      mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }
    }

    .tour-body {
      padding: 20px;
    }

    .tour-title {
      margin: 0 0 12px 0;
      font-size: 18px;
      font-weight: 600;
      color: #1f2937;
    }

    .tour-description {
      margin: 0;
      font-size: 14px;
      line-height: 1.6;
      color: #4b5563;
    }

    .tour-navigation {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px 20px;
      background: #f9fafb;
      border-top: 1px solid #e5e7eb;
    }

    .step-dots {
      display: flex;
      gap: 6px;
    }

    .dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #d1d5db;
      cursor: pointer;
      transition: all 0.2s;
    }

    .dot:hover {
      background: #9ca3af;
    }

    .dot.active {
      background: #667eea;
      width: 24px;
      border-radius: 4px;
    }

    .dot.completed {
      background: #10b981;
    }

    .keyboard-hint {
      padding: 8px 20px 12px;
      text-align: center;
      font-size: 11px;
      color: #9ca3af;
      background: #f9fafb;
    }

    kbd {
      display: inline-block;
      padding: 2px 6px;
      background: #e5e7eb;
      border: 1px solid #d1d5db;
      border-radius: 3px;
      font-family: monospace;
      font-size: 10px;
      font-weight: 600;
    }

    /* Arrow styles */
    .tour-arrow {
      position: absolute;
      width: 0;
      height: 0;
      border-style: solid;
    }

    .arrow-top {
      bottom: -12px;
      left: 50%;
      transform: translateX(-50%);
      border-width: 12px 12px 0 12px;
      border-color: #f9fafb transparent transparent transparent;
    }

    .arrow-bottom {
      top: -12px;
      left: 50%;
      transform: translateX(-50%);
      border-width: 0 12px 12px 12px;
      border-color: transparent transparent white transparent;
    }

    .arrow-left {
      right: -12px;
      top: 50%;
      transform: translateY(-50%);
      border-width: 12px 0 12px 12px;
      border-color: transparent transparent transparent white;
    }

    .arrow-right {
      left: -12px;
      top: 50%;
      transform: translateY(-50%);
      border-width: 12px 12px 12px 0;
      border-color: transparent white transparent transparent;
    }

    /* Button styling */
    button mat-icon {
      margin-right: 4px;
    }

    button:last-child mat-icon {
      margin-right: 0;
      margin-left: 4px;
    }

    /* Responsive */
    @media (max-width: 480px) {
      .tour-content {
        width: calc(100vw - 32px);
        left: 16px !important;
      }
    }
  `],
})
export class TourOverlayComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  isVisible = false;
  currentStep: TourStep | null = null;
  currentStepIndex = 0;
  totalSteps = 0;
  steps: TourStep[] = [];
  progressPercent = 0;

  overlayTop = 0;
  overlayLeft = 0;

  constructor(private tourService: GuidedTourService) {}

  ngOnInit(): void {
    combineLatest([
      this.tourService.tourState$,
      this.tourService.currentStep$,
    ])
      .pipe(takeUntil(this.destroy$))
      .subscribe(([state, step]) => {
        this.updateState(state, step);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  @HostListener('document:keydown', ['$event'])
  handleKeydown(event: KeyboardEvent): void {
    if (!this.isVisible) return;

    switch (event.key) {
      case 'ArrowRight':
      case 'Enter':
        event.preventDefault();
        this.nextStep();
        break;
      case 'ArrowLeft':
        event.preventDefault();
        this.previousStep();
        break;
      case 'Escape':
        event.preventDefault();
        this.skipTour();
        break;
    }
  }

  private updateState(state: TourState | null, step: TourStep | null): void {
    if (!state || !step) {
      this.isVisible = false;
      return;
    }

    const tour = this.tourService.getTour(state.tourId);
    if (!tour) {
      this.isVisible = false;
      return;
    }

    this.isVisible = state.isActive;
    this.currentStep = step;
    this.currentStepIndex = state.currentStepIndex;
    this.totalSteps = state.totalSteps;
    this.steps = tour.steps;
    this.progressPercent = ((state.currentStepIndex + 1) / state.totalSteps) * 100;

    // Position the overlay
    this.positionOverlay(step);
  }

  private positionOverlay(step: TourStep): void {
    const target = document.querySelector(step.targetSelector) as HTMLElement;
    if (!target) return;

    const targetRect = target.getBoundingClientRect();
    const overlayWidth = 380;
    const overlayHeight = 300; // Approximate
    const offset = 20;

    switch (step.position) {
      case 'top':
        this.overlayTop = targetRect.top - overlayHeight - offset;
        this.overlayLeft = targetRect.left + (targetRect.width / 2) - (overlayWidth / 2);
        break;
      case 'bottom':
        this.overlayTop = targetRect.bottom + offset;
        this.overlayLeft = targetRect.left + (targetRect.width / 2) - (overlayWidth / 2);
        break;
      case 'left':
        this.overlayTop = targetRect.top + (targetRect.height / 2) - (overlayHeight / 2);
        this.overlayLeft = targetRect.left - overlayWidth - offset;
        break;
      case 'right':
        this.overlayTop = targetRect.top + (targetRect.height / 2) - (overlayHeight / 2);
        this.overlayLeft = targetRect.right + offset;
        break;
      case 'center':
        this.overlayTop = window.innerHeight / 2 - overlayHeight / 2;
        this.overlayLeft = window.innerWidth / 2 - overlayWidth / 2;
        break;
    }

    // Keep within viewport bounds
    const padding = 16;
    this.overlayLeft = Math.max(padding, Math.min(this.overlayLeft, window.innerWidth - overlayWidth - padding));
    this.overlayTop = Math.max(padding, Math.min(this.overlayTop, window.innerHeight - overlayHeight - padding));
  }

  nextStep(): void {
    this.tourService.nextStep();
  }

  previousStep(): void {
    this.tourService.previousStep();
  }

  goToStep(index: number): void {
    this.tourService.goToStep(index);
  }

  skipTour(): void {
    this.tourService.skipTour();
  }

  completeTour(): void {
    this.tourService.nextStep(); // This will trigger completion on last step
  }
}
