/**
 * MRN Pipe
 *
 * Formats Medical Record Numbers with standard prefix.
 *
 * @example
 * {{ '001234567' | mrn }}  // "MRN-001234567"
 */
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'mrn',
  standalone: true
})
export class MrnPipe implements PipeTransform {
  transform(value: string | number | null | undefined, prefix: string = 'MRN'): string {
    if (!value) {
      return '';
    }

    const mrnStr = String(value).padStart(9, '0');
    return `${prefix}-${mrnStr}`;
  }
}
