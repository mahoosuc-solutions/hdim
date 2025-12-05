import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Loading Service - Manages global loading state
 *
 * Features:
 * - Track loading state across the application
 * - Support for multiple concurrent loading operations
 * - Observable loading state for UI updates
 */
@Injectable({
  providedIn: 'root',
})
export class LoadingService {
  private loadingCountSubject = new BehaviorSubject<number>(0);
  private loadingCount = 0;

  public isLoading$: Observable<boolean> = new BehaviorSubject<boolean>(false);

  constructor() {
    this.loadingCountSubject.subscribe((count) => {
      (this.isLoading$ as BehaviorSubject<boolean>).next(count > 0);
    });
  }

  /**
   * Show loading indicator
   */
  show(): void {
    this.loadingCount++;
    this.loadingCountSubject.next(this.loadingCount);
  }

  /**
   * Hide loading indicator
   */
  hide(): void {
    if (this.loadingCount > 0) {
      this.loadingCount--;
    }
    this.loadingCountSubject.next(this.loadingCount);
  }

  /**
   * Reset loading state
   */
  reset(): void {
    this.loadingCount = 0;
    this.loadingCountSubject.next(0);
  }

  /**
   * Get current loading count
   */
  getLoadingCount(): number {
    return this.loadingCount;
  }
}
