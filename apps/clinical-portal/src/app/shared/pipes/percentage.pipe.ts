/**
 * Percentage Pipe
 *
 * Formats decimal numbers as percentages.
 *
 * @example
 * {{ 0.856 | percentage }}       // "85.6%"
 * {{ 0.856 | percentage:0 }}     // "86%"
 * {{ 0.856 | percentage:2 }}     // "85.60%"
 */
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'percentage',
  standalone: true
})
export class PercentagePipe implements PipeTransform {
  transform(value: number | null | undefined, decimals: number = 1): string {
    if (value === null || value === undefined || isNaN(value)) {
      return '0%';
    }

    const percentage = value * 100;
    return `${percentage.toFixed(decimals)}%`;
  }
}
