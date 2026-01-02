/**
 * Highlight Directive
 *
 * Highlights text on mouse hover and optionally matches search terms.
 *
 * @example
 * <div appHighlight [searchTerm]="'important'" [highlightColor]="'yellow'">
 *   This is important text
 * </div>
 */
import { Directive, ElementRef, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';

@Directive({
  selector: '[appHighlight]',
  standalone: true
})
export class HighlightDirective implements OnInit, OnChanges {
  /** Search term to highlight */
  @Input() searchTerm?: string;

  /** Highlight color */
  @Input() highlightColor: string = '#ffeb3b';

  /** Text color */
  @Input() textColor: string = '#000000';

  private originalContent: string = '';

  constructor(private el: ElementRef) {}

  ngOnInit(): void {
    this.originalContent = this.el.nativeElement.textContent || '';
    this.applyHighlight();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['searchTerm'] || changes['highlightColor']) {
      this.applyHighlight();
    }
  }

  private applyHighlight(): void {
    if (!this.searchTerm || this.searchTerm.trim() === '') {
      this.el.nativeElement.innerHTML = this.originalContent;
      return;
    }

    const content = this.originalContent;
    const regex = new RegExp(`(${this.escapeRegex(this.searchTerm)})`, 'gi');

    const highlighted = content.replace(
      regex,
      `<mark style="background-color: ${this.highlightColor}; color: ${this.textColor}; padding: 2px 4px; border-radius: 2px;">$1</mark>`
    );

    this.el.nativeElement.innerHTML = highlighted;
  }

  private escapeRegex(str: string): string {
    return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }
}
