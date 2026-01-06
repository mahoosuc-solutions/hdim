import {
  Directive,
  Input,
  ElementRef,
  OnInit,
  OnDestroy,
  Renderer2,
  HostListener,
} from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import {
  KeyboardShortcutsService,
  KeyboardShortcut,
} from '../services/keyboard-shortcuts.service';

/**
 * Shortcut Hint Directive
 *
 * Adds keyboard shortcut hints to elements on hover.
 * Displays the shortcut binding in a tooltip-style overlay.
 *
 * Usage:
 * <button hdimShortcutHint="sign-result">Sign</button>
 * <button [hdimShortcutHint]="'refresh'">Refresh</button>
 *
 * Issue #159: Keyboard Shortcuts for Provider Workflows
 */
@Directive({
  selector: '[hdimShortcutHint]',
  standalone: true,
})
export class ShortcutHintDirective implements OnInit, OnDestroy {
  @Input('hdimShortcutHint') shortcutId = '';
  @Input() shortcutPosition: 'top' | 'bottom' | 'left' | 'right' = 'bottom';
  @Input() shortcutShowOnFocus = false;

  private readonly destroy$ = new Subject<void>();
  private hintElement: HTMLElement | null = null;
  private shortcut: KeyboardShortcut | undefined;

  constructor(
    private el: ElementRef<HTMLElement>,
    private renderer: Renderer2,
    private shortcutsService: KeyboardShortcutsService
  ) {}

  ngOnInit(): void {
    this.shortcutsService.shortcuts$
      .pipe(takeUntil(this.destroy$))
      .subscribe(shortcuts => {
        this.shortcut = shortcuts.find(s => s.id === this.shortcutId);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.removeHint();
  }

  @HostListener('mouseenter')
  onMouseEnter(): void {
    this.showHint();
  }

  @HostListener('mouseleave')
  onMouseLeave(): void {
    this.removeHint();
  }

  @HostListener('focus')
  onFocus(): void {
    if (this.shortcutShowOnFocus) {
      this.showHint();
    }
  }

  @HostListener('blur')
  onBlur(): void {
    if (this.shortcutShowOnFocus) {
      this.removeHint();
    }
  }

  private showHint(): void {
    if (!this.shortcut || !this.shortcut.enabled) {
      return;
    }

    // Remove any existing hint
    this.removeHint();

    // Create hint element
    this.hintElement = this.renderer.createElement('div');
    this.renderer.addClass(this.hintElement, 'shortcut-hint');
    this.renderer.addClass(this.hintElement, `shortcut-hint-${this.shortcutPosition}`);

    // Create keyboard key element
    const kbdElement = this.renderer.createElement('kbd');
    const shortcutText = this.renderer.createText(
      this.shortcutsService.formatShortcut(this.shortcut)
    );
    this.renderer.appendChild(kbdElement, shortcutText);
    this.renderer.appendChild(this.hintElement, kbdElement);

    // Apply styles
    this.applyHintStyles();

    // Position the hint
    this.positionHint();

    // Add to DOM
    this.renderer.appendChild(document.body, this.hintElement);

    // Animate in
    requestAnimationFrame(() => {
      if (this.hintElement) {
        this.renderer.addClass(this.hintElement, 'shortcut-hint-visible');
      }
    });
  }

  private removeHint(): void {
    if (this.hintElement && this.hintElement.parentNode) {
      this.renderer.removeChild(document.body, this.hintElement);
      this.hintElement = null;
    }
  }

  private applyHintStyles(): void {
    if (!this.hintElement) return;

    const styles: { [key: string]: string } = {
      position: 'fixed',
      zIndex: '10000',
      padding: '4px 8px',
      background: 'rgba(0, 0, 0, 0.85)',
      borderRadius: '4px',
      fontSize: '11px',
      fontWeight: '600',
      color: 'white',
      fontFamily: '"SF Mono", "Monaco", "Inconsolata", "Fira Mono", monospace',
      whiteSpace: 'nowrap',
      pointerEvents: 'none',
      opacity: '0',
      transition: 'opacity 0.15s ease-in-out',
      boxShadow: '0 2px 8px rgba(0,0,0,0.3)',
    };

    Object.entries(styles).forEach(([key, value]) => {
      this.renderer.setStyle(this.hintElement, key, value);
    });

    // Style the kbd element inside
    const kbdElement = this.hintElement?.querySelector('kbd');
    if (kbdElement) {
      const kbdStyles: { [key: string]: string } = {
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '2px 6px',
        minWidth: '18px',
        background: 'linear-gradient(180deg, #555, #333)',
        border: '1px solid #666',
        borderRadius: '3px',
        boxShadow: '0 1px 0 #888, inset 0 1px 0 #666',
        fontSize: '10px',
        fontWeight: '700',
        color: '#fff',
      };

      Object.entries(kbdStyles).forEach(([key, value]) => {
        this.renderer.setStyle(kbdElement, key, value);
      });
    }
  }

  private positionHint(): void {
    if (!this.hintElement) return;

    const hostRect = this.el.nativeElement.getBoundingClientRect();
    const offset = 8;

    let top: number;
    let left: number;

    switch (this.shortcutPosition) {
      case 'top':
        top = hostRect.top - offset;
        left = hostRect.left + hostRect.width / 2;
        this.renderer.setStyle(this.hintElement, 'transform', 'translate(-50%, -100%)');
        break;
      case 'bottom':
        top = hostRect.bottom + offset;
        left = hostRect.left + hostRect.width / 2;
        this.renderer.setStyle(this.hintElement, 'transform', 'translateX(-50%)');
        break;
      case 'left':
        top = hostRect.top + hostRect.height / 2;
        left = hostRect.left - offset;
        this.renderer.setStyle(this.hintElement, 'transform', 'translate(-100%, -50%)');
        break;
      case 'right':
        top = hostRect.top + hostRect.height / 2;
        left = hostRect.right + offset;
        this.renderer.setStyle(this.hintElement, 'transform', 'translateY(-50%)');
        break;
    }

    this.renderer.setStyle(this.hintElement, 'top', `${top}px`);
    this.renderer.setStyle(this.hintElement, 'left', `${left}px`);
  }
}
