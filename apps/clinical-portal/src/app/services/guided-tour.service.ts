import { Injectable, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject, Subject, Observable } from 'rxjs';

/**
 * Tour Step Definition
 * Issue #160: Provider Dashboard Help System
 */
export interface TourStep {
  id: string;
  targetSelector: string;
  title: string;
  content: string;
  position: 'top' | 'bottom' | 'left' | 'right' | 'center';
  highlightPadding?: number;
  action?: () => void;
  beforeShow?: () => Promise<boolean>;
  canSkipTo?: boolean;
}

/**
 * Tour Definition
 */
export interface TourDefinition {
  id: string;
  name: string;
  description: string;
  steps: TourStep[];
  triggerOnFirstVisit?: boolean;
  onComplete?: () => void;
  onSkip?: () => void;
}

/**
 * Active Tour State
 */
export interface TourState {
  tourId: string;
  currentStepIndex: number;
  totalSteps: number;
  isActive: boolean;
  startedAt: Date;
}

/**
 * Guided Tour Service
 *
 * Provides interactive step-by-step guided tours for the Clinical Portal.
 * Tours highlight UI elements and provide explanatory content to help users
 * learn the application.
 *
 * Features:
 * - Step-by-step UI element highlighting
 * - First-visit tour triggering
 * - Tour progress persistence
 * - Customizable tour content
 * - Accessible navigation
 *
 * Issue #160: Provider Dashboard Help System
 */
@Injectable({
  providedIn: 'root',
})
export class GuidedTourService implements OnDestroy {
  private readonly TOUR_COMPLETED_KEY = 'hdim_completed_tours';
  private readonly FIRST_VISIT_KEY = 'hdim_first_visit_complete';
  private readonly destroy$ = new Subject<void>();
  private readonly isBrowser: boolean;

  // Tour definitions registry
  private tours = new Map<string, TourDefinition>();

  // Active tour state
  private tourStateSubject = new BehaviorSubject<TourState | null>(null);
  readonly tourState$ = this.tourStateSubject.asObservable();

  // Current step
  private currentStepSubject = new BehaviorSubject<TourStep | null>(null);
  readonly currentStep$ = this.currentStepSubject.asObservable();

  // Overlay visibility
  private overlayVisibleSubject = new BehaviorSubject<boolean>(false);
  readonly overlayVisible$ = this.overlayVisibleSubject.asObservable();

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);
    this.initializeDefaultTours();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.endTour();
  }

  /**
   * Initialize default tours
   */
  private initializeDefaultTours(): void {
    // Provider Dashboard Tour
    this.registerTour({
      id: 'provider-dashboard-tour',
      name: 'Provider Dashboard Tour',
      description: 'Learn how to use the provider dashboard effectively',
      triggerOnFirstVisit: true,
      steps: [
        {
          id: 'welcome',
          targetSelector: 'app-page-header',
          title: 'Welcome to the Provider Dashboard!',
          content: 'This is your command center for managing patient care. Let\'s take a quick tour to help you get started.',
          position: 'bottom',
        },
        {
          id: 'metrics',
          targetSelector: '.metrics-grid',
          title: 'Key Metrics at a Glance',
          content: 'These cards show your most important metrics: patients scheduled today, results to review, high-priority care gaps, and your quality score. Click any card for more details.',
          position: 'bottom',
        },
        {
          id: 'critical-alerts',
          targetSelector: '.critical-alerts-banner',
          title: 'Critical Alerts',
          content: 'Critical alerts appear here when there are urgent items requiring your immediate attention, such as critical lab results or overdue care gaps.',
          position: 'bottom',
          beforeShow: async () => {
            const element = document.querySelector('.critical-alerts-banner');
            return element !== null;
          },
        },
        {
          id: 'schedule',
          targetSelector: '.todays-schedule-card',
          title: 'Today\'s Schedule',
          content: 'See all your appointments for today. Patients with care gaps are highlighted so you can address them during visits. Click a patient to view their full profile.',
          position: 'top',
        },
        {
          id: 'results',
          targetSelector: '.pending-results-card',
          title: 'Pending Results',
          content: 'Lab results and tests awaiting your review appear here. Critical results are highlighted in red. Use the Sign button to acknowledge results, or Sign All Normal to quickly handle routine results.',
          position: 'top',
        },
        {
          id: 'care-gaps',
          targetSelector: '.care-gaps-card',
          title: 'High Priority Care Gaps',
          content: 'Care gaps represent missed preventive care opportunities. Click "Close Gap" when a patient receives the recommended service. Priority is indicated by color: red for critical, orange for high.',
          position: 'top',
        },
        {
          id: 'quick-actions',
          targetSelector: '.quick-actions-panel',
          title: 'Quick Actions',
          content: 'Frequently used actions are just one click away. Hover over buttons to see keyboard shortcuts for even faster access.',
          position: 'top',
        },
        {
          id: 'keyboard-shortcuts',
          targetSelector: '[matTooltip="Keyboard Shortcuts (?)"]',
          title: 'Keyboard Shortcuts',
          content: 'Press "?" anytime to see all keyboard shortcuts. Use Ctrl+R to refresh data, Ctrl+S to sign results, and more. Welcome to the Clinical Portal!',
          position: 'bottom',
        },
      ],
      onComplete: () => {
        this.markTourCompleted('provider-dashboard-tour');
      },
    });

    // Care Gap Management Tour
    this.registerTour({
      id: 'care-gap-tour',
      name: 'Care Gap Management',
      description: 'Learn how to review and close care gaps',
      steps: [
        {
          id: 'gap-overview',
          targetSelector: '.care-gaps-card',
          title: 'Care Gap Overview',
          content: 'Care gaps represent patients who are overdue for preventive services. This section shows high-priority gaps needing your attention.',
          position: 'top',
        },
        {
          id: 'priority-levels',
          targetSelector: '.risk-chip',
          title: 'Priority Levels',
          content: 'Gaps are prioritized by urgency: Critical (red) gaps require immediate attention, High (orange) are important, and Moderate (yellow) are standard reminders.',
          position: 'right',
          beforeShow: async () => {
            const element = document.querySelector('.risk-chip');
            return element !== null;
          },
        },
        {
          id: 'close-gap',
          targetSelector: '.close-gap-btn',
          title: 'Closing Care Gaps',
          content: 'Click "Close Gap" when the patient receives care. You\'ll document what was done and add any notes. The gap status updates automatically.',
          position: 'left',
          beforeShow: async () => {
            const element = document.querySelector('.close-gap-btn');
            return element !== null;
          },
        },
        {
          id: 'view-all',
          targetSelector: '.care-gaps-card mat-card-actions button',
          title: 'View All Care Gaps',
          content: 'Click "View All" to see the complete list of care gaps, with filtering and sorting options.',
          position: 'top',
          beforeShow: async () => {
            const element = document.querySelector('.care-gaps-card mat-card-actions button');
            return element !== null;
          },
        },
      ],
    });

    // Results Review Tour
    this.registerTour({
      id: 'results-tour',
      name: 'Results Review',
      description: 'Learn how to efficiently review and sign results',
      steps: [
        {
          id: 'results-overview',
          targetSelector: '.pending-results-card',
          title: 'Pending Results',
          content: 'This section shows lab results, imaging, and other tests awaiting your review. Results are sorted by severity.',
          position: 'top',
        },
        {
          id: 'severity-indicator',
          targetSelector: '.severity-badge',
          title: 'Severity Indicators',
          content: 'Results are color-coded by severity: Critical (red), High (orange), Moderate (yellow), Normal (green). Critical results should be reviewed first.',
          position: 'right',
          beforeShow: async () => {
            const element = document.querySelector('.severity-badge');
            return element !== null;
          },
        },
        {
          id: 'sign-result',
          targetSelector: '[hdimShortcutHint="sign-result"]',
          title: 'Signing Results',
          content: 'Click "Sign" to acknowledge a result. Use Ctrl+S as a keyboard shortcut. Signed results are removed from this list.',
          position: 'left',
          beforeShow: async () => {
            const element = document.querySelector('[hdimShortcutHint="sign-result"]');
            return element !== null;
          },
        },
        {
          id: 'sign-all-normal',
          targetSelector: '[hdimShortcutHint="sign-all-normal"]',
          title: 'Sign All Normal',
          content: 'Use "Sign All Normal" (Ctrl+Shift+A) to quickly sign all results marked as normal. A confirmation dialog ensures you\'re certain before proceeding.',
          position: 'top',
          beforeShow: async () => {
            const element = document.querySelector('[hdimShortcutHint="sign-all-normal"]');
            return element !== null;
          },
        },
      ],
    });
  }

  // ==================== Tour Registration ====================

  /**
   * Register a tour definition
   */
  registerTour(tour: TourDefinition): void {
    this.tours.set(tour.id, tour);
  }

  /**
   * Unregister a tour
   */
  unregisterTour(tourId: string): void {
    this.tours.delete(tourId);
  }

  /**
   * Get all available tours
   */
  getAvailableTours(): TourDefinition[] {
    return Array.from(this.tours.values());
  }

  /**
   * Get a specific tour
   */
  getTour(tourId: string): TourDefinition | undefined {
    return this.tours.get(tourId);
  }

  // ==================== Tour Control ====================

  /**
   * Start a tour
   */
  async startTour(tourId: string): Promise<boolean> {
    if (!this.isBrowser) return false;

    const tour = this.tours.get(tourId);
    if (!tour || tour.steps.length === 0) {
      console.warn(`Tour "${tourId}" not found or has no steps`);
      return false;
    }

    // End any active tour
    if (this.tourStateSubject.getValue()?.isActive) {
      this.endTour();
    }

    // Initialize tour state
    const state: TourState = {
      tourId,
      currentStepIndex: 0,
      totalSteps: tour.steps.length,
      isActive: true,
      startedAt: new Date(),
    };

    this.tourStateSubject.next(state);
    this.overlayVisibleSubject.next(true);

    // Show first step
    await this.showStep(0);
    return true;
  }

  /**
   * Go to next step
   */
  async nextStep(): Promise<void> {
    const state = this.tourStateSubject.getValue();
    if (!state?.isActive) return;

    const tour = this.tours.get(state.tourId);
    if (!tour) return;

    if (state.currentStepIndex < tour.steps.length - 1) {
      const newIndex = state.currentStepIndex + 1;
      this.tourStateSubject.next({
        ...state,
        currentStepIndex: newIndex,
      });
      await this.showStep(newIndex);
    } else {
      // Tour complete
      this.completeTour();
    }
  }

  /**
   * Go to previous step
   */
  async previousStep(): Promise<void> {
    const state = this.tourStateSubject.getValue();
    if (!state?.isActive || state.currentStepIndex === 0) return;

    const newIndex = state.currentStepIndex - 1;
    this.tourStateSubject.next({
      ...state,
      currentStepIndex: newIndex,
    });
    await this.showStep(newIndex);
  }

  /**
   * Go to specific step
   */
  async goToStep(stepIndex: number): Promise<void> {
    const state = this.tourStateSubject.getValue();
    if (!state?.isActive) return;

    const tour = this.tours.get(state.tourId);
    if (!tour || stepIndex < 0 || stepIndex >= tour.steps.length) return;

    this.tourStateSubject.next({
      ...state,
      currentStepIndex: stepIndex,
    });
    await this.showStep(stepIndex);
  }

  /**
   * Skip/end the tour without completing
   */
  skipTour(): void {
    const state = this.tourStateSubject.getValue();
    if (!state?.isActive) return;

    const tour = this.tours.get(state.tourId);
    if (tour?.onSkip) {
      tour.onSkip();
    }

    this.endTour();
  }

  /**
   * Complete the tour
   */
  private completeTour(): void {
    const state = this.tourStateSubject.getValue();
    if (!state) return;

    const tour = this.tours.get(state.tourId);
    if (tour?.onComplete) {
      tour.onComplete();
    }

    this.markTourCompleted(state.tourId);
    this.endTour();
  }

  /**
   * End the tour (cleanup)
   */
  endTour(): void {
    this.currentStepSubject.next(null);
    this.tourStateSubject.next(null);
    this.overlayVisibleSubject.next(false);
    this.removeHighlight();
  }

  // ==================== Step Display ====================

  /**
   * Show a specific step
   */
  private async showStep(stepIndex: number): Promise<void> {
    const state = this.tourStateSubject.getValue();
    if (!state) return;

    const tour = this.tours.get(state.tourId);
    if (!tour) return;

    const step = tour.steps[stepIndex];
    if (!step) return;

    // Check beforeShow condition
    if (step.beforeShow) {
      const shouldShow = await step.beforeShow();
      if (!shouldShow) {
        // Skip this step, try next
        if (stepIndex < tour.steps.length - 1) {
          await this.goToStep(stepIndex + 1);
        } else {
          this.completeTour();
        }
        return;
      }
    }

    // Find target element
    const targetElement = document.querySelector(step.targetSelector) as HTMLElement;
    if (!targetElement) {
      console.warn(`Tour step target not found: ${step.targetSelector}`);
      // Try next step
      if (stepIndex < tour.steps.length - 1) {
        await this.goToStep(stepIndex + 1);
      } else {
        this.completeTour();
      }
      return;
    }

    // Highlight element
    this.highlightElement(targetElement, step.highlightPadding || 8);

    // Scroll element into view
    targetElement.scrollIntoView({
      behavior: 'smooth',
      block: 'center',
    });

    // Execute step action if any
    if (step.action) {
      step.action();
    }

    // Emit current step
    this.currentStepSubject.next(step);
  }

  /**
   * Highlight an element
   */
  private highlightElement(element: HTMLElement, padding: number): void {
    this.removeHighlight();

    const rect = element.getBoundingClientRect();

    // Create spotlight overlay
    const spotlight = document.createElement('div');
    spotlight.id = 'tour-spotlight';
    spotlight.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      z-index: 9998;
      pointer-events: none;
      background: rgba(0, 0, 0, 0.5);
      clip-path: polygon(
        0% 0%,
        0% 100%,
        ${rect.left - padding}px 100%,
        ${rect.left - padding}px ${rect.top - padding}px,
        ${rect.right + padding}px ${rect.top - padding}px,
        ${rect.right + padding}px ${rect.bottom + padding}px,
        ${rect.left - padding}px ${rect.bottom + padding}px,
        ${rect.left - padding}px 100%,
        100% 100%,
        100% 0%
      );
      transition: clip-path 0.3s ease;
    `;
    document.body.appendChild(spotlight);

    // Add highlight border
    const highlight = document.createElement('div');
    highlight.id = 'tour-highlight';
    highlight.style.cssText = `
      position: fixed;
      top: ${rect.top - padding}px;
      left: ${rect.left - padding}px;
      width: ${rect.width + padding * 2}px;
      height: ${rect.height + padding * 2}px;
      border: 2px solid #3b82f6;
      border-radius: 8px;
      z-index: 9999;
      pointer-events: none;
      box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.5);
      animation: pulse-border 2s infinite;
    `;
    document.body.appendChild(highlight);

    // Add pulse animation
    const style = document.createElement('style');
    style.id = 'tour-styles';
    style.textContent = `
      @keyframes pulse-border {
        0%, 100% { border-color: #3b82f6; }
        50% { border-color: #60a5fa; }
      }
    `;
    document.head.appendChild(style);
  }

  /**
   * Remove highlight
   */
  private removeHighlight(): void {
    const spotlight = document.getElementById('tour-spotlight');
    const highlight = document.getElementById('tour-highlight');
    const styles = document.getElementById('tour-styles');

    if (spotlight) spotlight.remove();
    if (highlight) highlight.remove();
    if (styles) styles.remove();
  }

  // ==================== Tour Completion Tracking ====================

  /**
   * Check if a tour has been completed
   */
  isTourCompleted(tourId: string): boolean {
    if (!this.isBrowser) return true;

    try {
      const stored = localStorage.getItem(this.TOUR_COMPLETED_KEY);
      if (!stored) return false;
      const completed: string[] = JSON.parse(stored);
      return completed.includes(tourId);
    } catch {
      return false;
    }
  }

  /**
   * Mark a tour as completed
   */
  markTourCompleted(tourId: string): void {
    if (!this.isBrowser) return;

    try {
      const stored = localStorage.getItem(this.TOUR_COMPLETED_KEY);
      const completed: string[] = stored ? JSON.parse(stored) : [];
      if (!completed.includes(tourId)) {
        completed.push(tourId);
        localStorage.setItem(this.TOUR_COMPLETED_KEY, JSON.stringify(completed));
      }
    } catch {
      // Ignore errors
    }
  }

  /**
   * Reset tour completion (for testing)
   */
  resetTourCompletion(tourId?: string): void {
    if (!this.isBrowser) return;

    if (tourId) {
      try {
        const stored = localStorage.getItem(this.TOUR_COMPLETED_KEY);
        if (stored) {
          const completed: string[] = JSON.parse(stored);
          const filtered = completed.filter(id => id !== tourId);
          localStorage.setItem(this.TOUR_COMPLETED_KEY, JSON.stringify(filtered));
        }
      } catch {
        // Ignore errors
      }
    } else {
      localStorage.removeItem(this.TOUR_COMPLETED_KEY);
    }
  }

  // ==================== First Visit ====================

  /**
   * Check if user's first visit
   */
  isFirstVisit(): boolean {
    if (!this.isBrowser) return false;
    return localStorage.getItem(this.FIRST_VISIT_KEY) !== 'true';
  }

  /**
   * Mark first visit complete
   */
  markFirstVisitComplete(): void {
    if (!this.isBrowser) return;
    localStorage.setItem(this.FIRST_VISIT_KEY, 'true');
  }

  /**
   * Reset first visit (for testing)
   */
  resetFirstVisit(): void {
    if (!this.isBrowser) return;
    localStorage.removeItem(this.FIRST_VISIT_KEY);
  }

  /**
   * Check and trigger first visit tour
   */
  checkFirstVisitTour(): boolean {
    if (!this.isFirstVisit()) return false;

    // Find a tour marked for first visit
    const firstVisitTour = Array.from(this.tours.values()).find(t => t.triggerOnFirstVisit);
    if (firstVisitTour && !this.isTourCompleted(firstVisitTour.id)) {
      this.markFirstVisitComplete();
      this.startTour(firstVisitTour.id);
      return true;
    }

    this.markFirstVisitComplete();
    return false;
  }

  // ==================== State Getters ====================

  /**
   * Check if tour is active
   */
  isActive(): boolean {
    return this.tourStateSubject.getValue()?.isActive ?? false;
  }

  /**
   * Get current tour state
   */
  getCurrentState(): TourState | null {
    return this.tourStateSubject.getValue();
  }

  /**
   * Get current step
   */
  getCurrentStep(): TourStep | null {
    return this.currentStepSubject.getValue();
  }
}
