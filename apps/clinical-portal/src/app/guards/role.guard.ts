import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { LoggerService } from '../services/logger.service';

/**
 * Role Guard - Protects routes based on user roles
 *
 * Usage:
 * ```typescript
 * {
  path: 'admin',
  component: AdminComponent,
  canActivate: [AuthGuard, RoleGuard],
  data: { roles: ['ADMIN', 'MANAGER'] } // User must have at least one of these roles
 * }
 * ```
 */
@Injectable({
  providedIn: 'root',
})
export class RoleGuard implements CanActivate {
  private get logger() {
    return this.loggerService.withContext('RoleGuard');
  }

  constructor(
    private authService: AuthService,
    private router: Router,
    private loggerService: LoggerService
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    const requiredRoles = route.data['roles'] as string[];

    if (!requiredRoles || requiredRoles.length === 0) {
      // No roles specified, allow access
      return true;
    }

    // Check if user has any of the required roles
    const hasRole = this.authService.hasAnyRole(requiredRoles);

    if (hasRole) {
      return true;
    }

    // User doesn't have required role
    this.logger.warn(
      `User doesn't have required roles: ${requiredRoles.join(', ')}. Redirecting to unauthorized.`
    );

    // Redirect to unauthorized page
    return this.router.createUrlTree(['/unauthorized']);
  }
}
