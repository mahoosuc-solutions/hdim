/**
 * Time Ago Pipe
 *
 * Transforms dates to relative time strings.
 *
 * @example
 * {{ date | timeAgo }}  // "2 hours ago", "3 days ago", "just now"
 */
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'timeAgo',
  standalone: true
})
export class TimeAgoPipe implements PipeTransform {
  transform(value: Date | string | number | null | undefined): string {
    if (!value) {
      return '';
    }

    const date = value instanceof Date ? value : new Date(value);

    if (isNaN(date.getTime())) {
      return '';
    }

    const seconds = Math.floor((new Date().getTime() - date.getTime()) / 1000);

    if (seconds < 10) {
      return 'just now';
    }

    const intervals = {
      year: 31536000,
      month: 2592000,
      week: 604800,
      day: 86400,
      hour: 3600,
      minute: 60,
      second: 1
    };

    for (const [unit, secondsInUnit] of Object.entries(intervals)) {
      const interval = Math.floor(seconds / secondsInUnit);

      if (interval >= 1) {
        return interval === 1
          ? `1 ${unit} ago`
          : `${interval} ${unit}s ago`;
      }
    }

    return 'just now';
  }
}
