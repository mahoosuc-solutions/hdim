/**
 * Safe HTML Pipe
 *
 * Bypasses Angular's built-in sanitization for trusted HTML.
 * Use with caution - only for trusted content.
 *
 * @example
 * <div [innerHTML]="htmlContent | safeHtml"></div>
 */
import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Pipe({
  name: 'safeHtml',
  standalone: true
})
export class SafeHtmlPipe implements PipeTransform {
  constructor(private sanitizer: DomSanitizer) {}

  transform(value: string | null | undefined): SafeHtml {
    if (!value) {
      return '';
    }

    return this.sanitizer.bypassSecurityTrustHtml(value);
  }
}
