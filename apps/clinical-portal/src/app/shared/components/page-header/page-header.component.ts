import { Component, Input, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

export interface Breadcrumb {
  label: string;
  route?: string;
  icon?: string;
}

/**
 * PageHeader Component
 *
 * A reusable page header component with title, subtitle, breadcrumbs, and action buttons.
 * Provides consistent header layout across all pages.
 *
 * Features:
 * - Title and subtitle display
 * - Breadcrumb navigation with router links
 * - Action buttons area (content projection)
 * - Material toolbar styling
 * - Responsive design
 * - Accessible with ARIA attributes
 *
 * @example
 * <app-page-header
 *   title="Patients"
 *   subtitle="Manage patient records"
 *   [breadcrumbs]="breadcrumbs">
 *   <button mat-raised-button color="primary">
 *     <mat-icon>add</mat-icon>
 *     Add Patient
 *   </button>
 * </app-page-header>
 */
@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule
  ],
  templateUrl: './page-header.component.html',
  styleUrls: ['./page-header.component.scss']
})
export class PageHeaderComponent {
  /** Page title */
  @Input() title = '';

  /** Optional subtitle */
  @Input() subtitle?: string;

  /** Breadcrumb navigation items */
  @Input() breadcrumbs: Breadcrumb[] = [];

  /** Show back button */
  @Input() showBackButton = false;

  /** Custom CSS class */
  @Input() customClass?: string;

  /**
   * Get the last breadcrumb (current page)
   */
  getLastBreadcrumb(): Breadcrumb | undefined {
    return this.breadcrumbs[this.breadcrumbs.length - 1];
  }

  /**
   * Get all breadcrumbs except the last one
   */
  getParentBreadcrumbs(): Breadcrumb[] {
    return this.breadcrumbs.slice(0, -1);
  }

  /**
   * Check if breadcrumb has a route
   */
  hasRoute(breadcrumb: Breadcrumb): boolean {
    return !!breadcrumb.route;
  }
}
