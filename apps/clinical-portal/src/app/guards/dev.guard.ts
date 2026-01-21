import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { environment } from '../../environments/environment';

/**
 * Development Guard - Allows access in development mode
 * 
 * In development, this guard always allows access.
 * In production, it requires authentication via AuthGuard.
 * 
 * Usage:
 * ```typescript
 * {
 *   path: 'testing',
 *   component: TestingComponent,
 *   canActivate: [DevGuard] // or [AuthGuard, DevGuard] for production safety
 * }
 * ```
 */
@Injectable({
  providedIn: 'root',
})
export class DevGuard implements CanActivate {
  constructor(private router: Router) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean | UrlTree {
    // In development, always allow access
    if (!environment.production) {
      return true;
    }

    // In production, redirect to login (should be used with AuthGuard)
    console.warn('DevGuard: Production mode - access denied');
    return this.router.createUrlTree(['/login'], {
      queryParams: { returnUrl: state.url },
    });
  }
}
