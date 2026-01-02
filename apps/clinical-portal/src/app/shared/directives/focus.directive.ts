/**
 * Auto Focus Directive
 *
 * Automatically focuses an input element when the component loads.
 *
 * @example
 * <input appFocus type="text" placeholder="Search...">
 */
import { Directive, ElementRef, OnInit, Input } from '@angular/core';

@Directive({
  selector: '[appFocus]',
  standalone: true
})
export class FocusDirective implements OnInit {
  /** Delay before focusing (ms) */
  @Input() focusDelay: number = 0;

  constructor(private el: ElementRef) {}

  ngOnInit(): void {
    if (this.focusDelay > 0) {
      setTimeout(() => this.focus(), this.focusDelay);
    } else {
      this.focus();
    }
  }

  private focus(): void {
    const element = this.el.nativeElement as HTMLElement;
    if (element && typeof element.focus === 'function') {
      element.focus();
    }
  }
}
