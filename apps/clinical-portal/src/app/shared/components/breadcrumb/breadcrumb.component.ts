/**
 * Breadcrumb Navigation Component
 *
 * Provides contextual navigation showing the current location in the app hierarchy
 * and allowing quick navigation to parent pages
 */

import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, takeUntil } from 'rxjs';
import { BreadcrumbService, Breadcrumb } from '../../services/breadcrumb.service';

@Component({
  selector: 'app-breadcrumb',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  templateUrl: './breadcrumb.component.html',
  styleUrls: ['./breadcrumb.component.scss'],
})
export class BreadcrumbComponent implements OnInit, OnDestroy {
  breadcrumbs: Breadcrumb[] = [];
  private destroy$ = new Subject<void>();

  constructor(private breadcrumbService: BreadcrumbService) {}

  ngOnInit(): void {
    this.breadcrumbService.breadcrumbs$
      .pipe(takeUntil(this.destroy$))
      .subscribe((breadcrumbs) => {
        this.breadcrumbs = breadcrumbs;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Get icon for breadcrumb based on route segment
   */
  getIcon(breadcrumb: Breadcrumb): string {
    if (breadcrumb.icon) {
      return breadcrumb.icon;
    }

    // Auto-detect icon based on label
    const label = breadcrumb.label.toLowerCase();
    if (label.includes('dashboard') || label.includes('home')) return 'dashboard';
    if (label.includes('patient')) return 'person';
    if (label.includes('evaluation')) return 'assessment';
    if (label.includes('result')) return 'analytics';
    if (label.includes('report')) return 'description';
    if (label.includes('measure')) return 'rule';
    if (label.includes('knowledge')) return 'menu_book';
    if (label.includes('ai') || label.includes('assistant')) return 'smart_toy';
    return 'chevron_right';
  }

  /**
   * Check if breadcrumb is the last (current page)
   */
  isLast(index: number): boolean {
    return index === this.breadcrumbs.length - 1;
  }

  /**
   * Track by function for ngFor optimization
   */
  trackByUrl(index: number, breadcrumb: Breadcrumb): string {
    return breadcrumb.url;
  }
}
