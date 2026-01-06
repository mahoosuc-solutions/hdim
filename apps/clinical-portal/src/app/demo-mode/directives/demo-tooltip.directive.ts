import {
  Directive,
  Input,
  ElementRef,
  OnInit,
  OnDestroy,
  Renderer2,
  HostListener,
} from '@angular/core';
import { DemoModeService } from '../services/demo-mode.service';

/**
 * Demo Tooltip Directive
 *
 * Adds demo-specific tooltips to elements when demo mode is active.
 * Provides additional context for demo recordings.
 *
 * Usage:
 * <div appDemoTooltip="This metric shows the HEDIS compliance rate">
 *   71.7%
 * </div>
 *
 * With position:
 * <div appDemoTooltip="Patient count" demoTooltipPosition="bottom">
 *   5,000
 * </div>
 */
@Directive({
  selector: '[appDemoTooltip]',
  standalone: true,
})
export class DemoTooltipDirective implements OnInit, OnDestroy {
  @Input('appDemoTooltip') tooltipContent = '';
  @Input() demoTooltipPosition: 'top' | 'bottom' | 'left' | 'right' = 'top';
  @Input() demoTooltipDelay = 500;

  private tooltipElement: HTMLElement | null = null;
  private showTimeout: ReturnType<typeof setTimeout> | null = null;
  private isVisible = false;

  constructor(
    private el: ElementRef,
    private renderer: Renderer2,
    private demoService: DemoModeService
  ) {}

  ngOnInit(): void {
    // Add demo tooltip attribute for styling
    this.renderer.setAttribute(
      this.el.nativeElement,
      'data-demo-tooltip',
      this.tooltipContent
    );
  }

  ngOnDestroy(): void {
    this.hideTooltip();
    if (this.showTimeout) {
      clearTimeout(this.showTimeout);
    }
  }

  @HostListener('mouseenter')
  onMouseEnter(): void {
    if (!this.demoService.isDemoMode() || !this.demoService.showTooltips()) {
      return;
    }

    this.showTimeout = setTimeout(() => {
      this.showTooltip();
    }, this.demoTooltipDelay);
  }

  @HostListener('mouseleave')
  onMouseLeave(): void {
    if (this.showTimeout) {
      clearTimeout(this.showTimeout);
      this.showTimeout = null;
    }
    this.hideTooltip();
  }

  private showTooltip(): void {
    if (this.isVisible || !this.tooltipContent) return;

    // Create tooltip element
    this.tooltipElement = this.renderer.createElement('div');
    this.renderer.addClass(this.tooltipElement, 'demo-tooltip');
    this.renderer.addClass(this.tooltipElement, `demo-tooltip-${this.demoTooltipPosition}`);

    // Add content
    const text = this.renderer.createText(this.tooltipContent);
    this.renderer.appendChild(this.tooltipElement, text);

    // Add demo indicator
    const indicator = this.renderer.createElement('span');
    this.renderer.addClass(indicator, 'demo-tooltip-indicator');
    this.renderer.appendChild(indicator, this.renderer.createText('DEMO'));
    this.renderer.appendChild(this.tooltipElement, indicator);

    // Position tooltip
    const hostRect = this.el.nativeElement.getBoundingClientRect();
    this.renderer.setStyle(this.tooltipElement, 'position', 'fixed');
    this.renderer.setStyle(this.tooltipElement, 'z-index', '10000');

    // Calculate position based on direction
    switch (this.demoTooltipPosition) {
      case 'top':
        this.renderer.setStyle(this.tooltipElement, 'bottom', `${window.innerHeight - hostRect.top + 8}px`);
        this.renderer.setStyle(this.tooltipElement, 'left', `${hostRect.left + hostRect.width / 2}px`);
        this.renderer.setStyle(this.tooltipElement, 'transform', 'translateX(-50%)');
        break;
      case 'bottom':
        this.renderer.setStyle(this.tooltipElement, 'top', `${hostRect.bottom + 8}px`);
        this.renderer.setStyle(this.tooltipElement, 'left', `${hostRect.left + hostRect.width / 2}px`);
        this.renderer.setStyle(this.tooltipElement, 'transform', 'translateX(-50%)');
        break;
      case 'left':
        this.renderer.setStyle(this.tooltipElement, 'top', `${hostRect.top + hostRect.height / 2}px`);
        this.renderer.setStyle(this.tooltipElement, 'right', `${window.innerWidth - hostRect.left + 8}px`);
        this.renderer.setStyle(this.tooltipElement, 'transform', 'translateY(-50%)');
        break;
      case 'right':
        this.renderer.setStyle(this.tooltipElement, 'top', `${hostRect.top + hostRect.height / 2}px`);
        this.renderer.setStyle(this.tooltipElement, 'left', `${hostRect.right + 8}px`);
        this.renderer.setStyle(this.tooltipElement, 'transform', 'translateY(-50%)');
        break;
    }

    // Add to body
    this.renderer.appendChild(document.body, this.tooltipElement);

    // Animate in
    requestAnimationFrame(() => {
      if (this.tooltipElement) {
        this.renderer.addClass(this.tooltipElement, 'visible');
      }
    });

    this.isVisible = true;
  }

  private hideTooltip(): void {
    if (!this.isVisible || !this.tooltipElement) return;

    this.renderer.removeClass(this.tooltipElement, 'visible');

    // Remove after animation
    setTimeout(() => {
      if (this.tooltipElement && this.tooltipElement.parentNode) {
        this.renderer.removeChild(document.body, this.tooltipElement);
        this.tooltipElement = null;
      }
    }, 200);

    this.isVisible = false;
  }
}
