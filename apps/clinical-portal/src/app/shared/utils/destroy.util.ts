import { DestroyRef, inject } from '@angular/core';
import { Subject } from 'rxjs';

/**
 * Creates a destroy$ Subject that automatically completes when the component is destroyed.
 * Uses Angular's DestroyRef for automatic cleanup - no ngOnDestroy implementation needed.
 *
 * @example
 * ```typescript
 * @Component({...})
 * export class MyComponent implements OnInit {
 *   private destroy$ = injectDestroy();
 *
 *   ngOnInit(): void {
 *     this.someService.data$
 *       .pipe(takeUntil(this.destroy$))
 *       .subscribe(data => { ... });
 *   }
 *   // No ngOnDestroy needed!
 * }
 * ```
 *
 * @returns Subject<void> that emits and completes on component destruction
 */
export function injectDestroy(): Subject<void> {
  const destroy$ = new Subject<void>();
  const destroyRef = inject(DestroyRef);

  destroyRef.onDestroy(() => {
    destroy$.next();
    destroy$.complete();
  });

  return destroy$;
}
