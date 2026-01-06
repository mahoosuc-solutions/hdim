import {
  Directive,
  Input,
  ElementRef,
  OnInit,
  OnDestroy,
  Renderer2,
  effect,
} from '@angular/core';
import { DemoModeService } from '../services/demo-mode.service';

/**
 * Demo Highlight Directive
 *
 * Adds a subtle highlight/glow effect to important elements during demo mode.
 * Helps draw attention to key metrics and features during recordings.
 *
 * Usage:
 * <div appDemoHighlight>
 *   Important metric here
 * </div>
 *
 * With color:
 * <div appDemoHighlight="success">
 *   71.7% Compliance Rate
 * </div>
 *
 * With pulse animation:
 * <div appDemoHighlight="primary" [demoPulse]="true">
 *   New Feature!
 * </div>
 */
@Directive({
  selector: '[appDemoHighlight]',
  standalone: true,
})
export class DemoHighlightDirective implements OnInit, OnDestroy {
  @Input('appDemoHighlight') highlightType: 'primary' | 'success' | 'warning' | 'info' | '' = '';
  @Input() demoPulse = false;

  private originalBoxShadow: string | null = null;
  private originalTransition: string | null = null;

  constructor(
    private el: ElementRef,
    private renderer: Renderer2,
    private demoService: DemoModeService
  ) {
    // React to demo mode changes
    effect(() => {
      if (this.demoService.isDemoMode()) {
        this.applyHighlight();
      } else {
        this.removeHighlight();
      }
    });
  }

  ngOnInit(): void {
    // Store original styles
    this.originalBoxShadow = this.el.nativeElement.style.boxShadow;
    this.originalTransition = this.el.nativeElement.style.transition;
  }

  ngOnDestroy(): void {
    this.removeHighlight();
  }

  private applyHighlight(): void {
    const colors: Record<string, string> = {
      primary: 'rgba(33, 150, 243, 0.4)',
      success: 'rgba(76, 175, 80, 0.4)',
      warning: 'rgba(255, 152, 0, 0.4)',
      info: 'rgba(3, 169, 244, 0.4)',
      '': 'rgba(33, 150, 243, 0.3)',
    };

    const color = colors[this.highlightType] || colors[''];

    // Add smooth transition
    this.renderer.setStyle(
      this.el.nativeElement,
      'transition',
      'box-shadow 0.3s ease, transform 0.3s ease'
    );

    // Apply glow effect
    this.renderer.setStyle(
      this.el.nativeElement,
      'box-shadow',
      `0 0 20px ${color}, 0 0 40px ${color.replace('0.4', '0.2')}`
    );

    // Add demo highlight class for additional styling
    this.renderer.addClass(this.el.nativeElement, 'demo-highlighted');

    // Add pulse animation if enabled
    if (this.demoPulse) {
      this.renderer.addClass(this.el.nativeElement, 'demo-pulse');
    }
  }

  private removeHighlight(): void {
    // Restore original styles
    if (this.originalBoxShadow !== null) {
      this.renderer.setStyle(this.el.nativeElement, 'box-shadow', this.originalBoxShadow);
    } else {
      this.renderer.removeStyle(this.el.nativeElement, 'box-shadow');
    }

    if (this.originalTransition !== null) {
      this.renderer.setStyle(this.el.nativeElement, 'transition', this.originalTransition);
    } else {
      this.renderer.removeStyle(this.el.nativeElement, 'transition');
    }

    // Remove classes
    this.renderer.removeClass(this.el.nativeElement, 'demo-highlighted');
    this.renderer.removeClass(this.el.nativeElement, 'demo-pulse');
  }
}
