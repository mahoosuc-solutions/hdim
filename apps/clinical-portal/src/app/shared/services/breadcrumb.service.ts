/**
 * Breadcrumb Service
 *
 * Manages breadcrumb navigation state by listening to route changes
 * and building breadcrumb trails based on route configuration
 */

import { Injectable } from '@angular/core';
import { Router, NavigationEnd, ActivatedRoute, Params } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { filter, distinctUntilChanged } from 'rxjs/operators';

export interface Breadcrumb {
  label: string;
  url: string;
  icon?: string;
  queryParams?: Params;
}

@Injectable({
  providedIn: 'root',
})
export class BreadcrumbService {
  private breadcrumbsSubject = new BehaviorSubject<Breadcrumb[]>([]);
  public breadcrumbs$: Observable<Breadcrumb[]> = this.breadcrumbsSubject.asObservable();

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        distinctUntilChanged()
      )
      .subscribe(() => {
        const breadcrumbs = this.buildBreadcrumbs(this.activatedRoute.root);
        this.breadcrumbsSubject.next(breadcrumbs);
      });
  }

  /**
   * Build breadcrumb trail from route tree
   */
  private buildBreadcrumbs(
    route: ActivatedRoute,
    url: string = '',
    breadcrumbs: Breadcrumb[] = []
  ): Breadcrumb[] {
    // Get the child routes
    const children: ActivatedRoute[] = route.children;

    if (children.length === 0) {
      return breadcrumbs;
    }

    for (const child of children) {
      // Skip routes without path
      const routePath = child.snapshot.url.map((segment) => segment.path).join('/');
      if (!routePath) {
        return this.buildBreadcrumbs(child, url, breadcrumbs);
      }

      // Build URL
      url += `/${routePath}`;

      // Get breadcrumb data from route config
      const breadcrumb = this.getBreadcrumbFromRoute(child, url);
      if (breadcrumb) {
        breadcrumbs.push(breadcrumb);
      }

      // Recursive
      return this.buildBreadcrumbs(child, url, breadcrumbs);
    }

    return breadcrumbs;
  }

  /**
   * Extract breadcrumb information from route
   */
  private getBreadcrumbFromRoute(route: ActivatedRoute, url: string): Breadcrumb | null {
    // Check if route has breadcrumb data
    const data = route.snapshot.data;
    const params = route.snapshot.params;
    const queryParams = route.snapshot.queryParams;

    // Get label from route data or use path segment
    let label = data['breadcrumb'] || this.getLabelFromUrl(url);

    // Replace parameter placeholders with actual values
    if (params) {
      Object.keys(params).forEach((key) => {
        label = label.replace(`:${key}`, params[key]);
      });
    }

    // Get icon from route data
    const icon = data['breadcrumbIcon'];

    // Skip if label is explicitly false
    if (label === false || label === 'false') {
      return null;
    }

    return {
      label,
      url,
      icon,
      queryParams: Object.keys(queryParams).length > 0 ? queryParams : undefined,
    };
  }

  /**
   * Generate human-readable label from URL segment
   */
  private getLabelFromUrl(url: string): string {
    const segments = url.split('/').filter((s) => s);
    const lastSegment = segments[segments.length - 1];

    // Remove query params
    const cleanSegment = lastSegment.split('?')[0];

    // Handle common routes
    const routeLabels: Record<string, string> = {
      '': 'Home',
      'dashboard': 'Dashboard',
      'patients': 'Patients',
      'patient-detail': 'Patient Details',
      'patient-health-overview': 'Health Overview',
      'evaluations': 'Evaluations',
      'results': 'Results',
      'reports': 'Reports',
      'knowledge-base': 'Knowledge Base',
      'ai-dashboard': 'AI Assistant',
      'measure-builder': 'Measure Builder',
      'provider-dashboard': 'Provider Dashboard',
    };

    if (routeLabels[cleanSegment]) {
      return routeLabels[cleanSegment];
    }

    // Convert kebab-case to Title Case
    return cleanSegment
      .split('-')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  /**
   * Manually set breadcrumbs (for dynamic routes)
   */
  setBreadcrumbs(breadcrumbs: Breadcrumb[]): void {
    this.breadcrumbsSubject.next(breadcrumbs);
  }

  /**
   * Add a breadcrumb to the current trail
   */
  addBreadcrumb(breadcrumb: Breadcrumb): void {
    const current = this.breadcrumbsSubject.value;
    this.breadcrumbsSubject.next([...current, breadcrumb]);
  }

  /**
   * Clear all breadcrumbs
   */
  clearBreadcrumbs(): void {
    this.breadcrumbsSubject.next([]);
  }

  /**
   * Get current breadcrumbs (synchronous)
   */
  getCurrentBreadcrumbs(): Breadcrumb[] {
    return this.breadcrumbsSubject.value;
  }
}
