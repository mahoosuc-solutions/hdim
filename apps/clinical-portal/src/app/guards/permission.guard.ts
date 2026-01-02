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

/**
 * Permission Guard - Protects routes based on user permissions
 *
 * Usage:
 * ```typescript
 * {
 *   path: 'reports',
 *   component: ReportsComponent,
 *   canActivate: [AuthGuard, PermissionGuard],
 *   data: { permissions: ['reports:read', 'reports:create'] } // User must have at least one
 * }
 * ```
 */
@Injectable({
  providedIn: 'root',
})
export class PermissionGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    const requiredPermissions = route.data['permissions'] as string[];

    if (!requiredPermissions || requiredPermissions.length === 0) {
      // No permissions specified, allow access
      return true;
    }

    // Check if user has any of the required permissions
    const hasPermission = this.authService.hasAnyPermission(requiredPermissions);

    if (hasPermission) {
      return true;
    }

    // User doesn't have required permission
    console.warn(
      `User doesn't have required permissions: ${requiredPermissions.join(', ')}. Redirecting to unauthorized.`
    );

    // Redirect to unauthorized page
    return this.router.createUrlTree(['/unauthorized']);
  }
}
