import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { environment } from '../../environments/environment';
import { LoggerService } from '../services/logger.service';

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
  private logger = this.loggerService.withContext('DevGuard');

  constructor(
    private router: Router,
    private loggerService: LoggerService
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean | UrlTree {
    // In development, always allow access
    if (!environment.production) {
      return true;
    }

    // In production, redirect to login (should be used with AuthGuard)
    this.logger.warn('DevGuard: Production mode - access denied');
    return this.router.createUrlTree(['/login'], {
      queryParams: { returnUrl: state.url },
    });
  }
}
