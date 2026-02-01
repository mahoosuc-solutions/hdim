/**
 * Ripple Directive
 *
 * Adds Material Design ripple effect to elements on click.
 *
 * @example
 * <div appRipple [rippleColor]="'rgba(0, 0, 0, 0.1)'">
 *   Click me
 * </div>
 */
import {
  Directive,
  ElementRef,
  HostListener,
  Input,
  Renderer2,
  OnInit
} from '@angular/core';

@Directive({
  selector: '[appRipple]',
  standalone: true
})
export class RippleDirective implements OnInit {
  /** Ripple color */
  @Input() rippleColor = 'rgba(255, 255, 255, 0.5)';

  /** Ripple duration in ms */
  @Input() rippleDuration = 600;

  constructor(
    private el: ElementRef,
    private renderer: Renderer2
  ) {}

  ngOnInit(): void {
    // Set position relative if not already positioned
    const position = window.getComputedStyle(this.el.nativeElement).position;
    if (position === 'static') {
      this.renderer.setStyle(this.el.nativeElement, 'position', 'relative');
    }

    // Ensure overflow is hidden
    this.renderer.setStyle(this.el.nativeElement, 'overflow', 'hidden');
  }

  @HostListener('click', ['$event'])
  onClick(event: MouseEvent): void {
    this.createRipple(event);
  }

  private createRipple(event: MouseEvent): void {
    const element = this.el.nativeElement;
    const rect = element.getBoundingClientRect();

    // Calculate ripple position
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;

    // Calculate ripple size
    const size = Math.max(rect.width, rect.height);

    // Create ripple element
    const ripple = this.renderer.createElement('span');
    this.renderer.addClass(ripple, 'ripple-effect');

    // Set ripple styles
    this.renderer.setStyle(ripple, 'width', `${size}px`);
    this.renderer.setStyle(ripple, 'height', `${size}px`);
    this.renderer.setStyle(ripple, 'left', `${x - size / 2}px`);
    this.renderer.setStyle(ripple, 'top', `${y - size / 2}px`);
    this.renderer.setStyle(ripple, 'position', 'absolute');
    this.renderer.setStyle(ripple, 'border-radius', '50%');
    this.renderer.setStyle(ripple, 'background-color', this.rippleColor);
    this.renderer.setStyle(ripple, 'transform', 'scale(0)');
    this.renderer.setStyle(ripple, 'animation', `ripple-animation ${this.rippleDuration}ms ease-out`);
    this.renderer.setStyle(ripple, 'pointer-events', 'none');

    // Append ripple to element
    this.renderer.appendChild(element, ripple);

    // Remove ripple after animation
    setTimeout(() => {
      this.renderer.removeChild(element, ripple);
    }, this.rippleDuration);

    // Add animation keyframes if not already added
    this.ensureAnimationStyles();
  }

  private ensureAnimationStyles(): void {
    const styleId = 'ripple-animation-styles';

    if (!document.getElementById(styleId)) {
      const style = document.createElement('style');
      style.id = styleId;
      style.textContent = `
        @keyframes ripple-animation {
          0% {
            transform: scale(0);
            opacity: 1;
          }
          100% {
            transform: scale(2);
            opacity: 0;
          }
        }
      `;
      document.head.appendChild(style);
    }
  }
}
