/**
 * Phone Pipe
 *
 * Formats phone numbers to standard US format.
 *
 * @example
 * {{ '1234567890' | phone }}  // "(123) 456-7890"
 */
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'phone',
  standalone: true
})
export class PhonePipe implements PipeTransform {
  transform(value: string | number | null | undefined): string {
    if (!value) {
      return '';
    }

    const phoneStr = String(value).replace(/\D/g, '');

    if (phoneStr.length === 10) {
      return `(${phoneStr.slice(0, 3)}) ${phoneStr.slice(3, 6)}-${phoneStr.slice(6)}`;
    }

    if (phoneStr.length === 11 && phoneStr[0] === '1') {
      return `+1 (${phoneStr.slice(1, 4)}) ${phoneStr.slice(4, 7)}-${phoneStr.slice(7)}`;
    }

    return String(value);
  }
}
