/**
 * Debounce Click Directive
 *
 * Prevents multiple rapid clicks on buttons.
 *
 * @example
 * <button (debounceClick)="handleClick()" [debounceTime]="500">
 *   Click Me
 * </button>
 */
import {
  Directive,
  EventEmitter,
  HostListener,
  Input,
  Output,
  OnDestroy
} from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

@Directive({
  selector: '[debounceClick]',
  standalone: true
})
export class DebounceClickDirective implements OnDestroy {
  /** Debounce time in milliseconds */
  @Input() debounceTime: number = 500;

  /** Debounced click event */
  @Output() debounceClick = new EventEmitter<MouseEvent>();

  private clicks = new Subject<MouseEvent>();
  private subscription: Subscription;

  constructor() {
    this.subscription = this.clicks
      .pipe(debounceTime(this.debounceTime))
      .subscribe(event => this.debounceClick.emit(event));
  }

  @HostListener('click', ['$event'])
  onClick(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.clicks.next(event);
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }
}
