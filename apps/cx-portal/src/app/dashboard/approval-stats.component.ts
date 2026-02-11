import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { interval, Subscription } from 'rxjs';
import { startWith, switchMap } from 'rxjs/operators';

import { CxApiService } from '../shared/services/cx-api.service';
import { ApprovalStats } from '../shared/models';

@Component({
  selector: 'app-approval-stats',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule],
  templateUrl: './approval-stats.component.html',
  styleUrls: ['./approval-stats.component.scss'],
})
export class ApprovalStatsComponent implements OnInit, OnDestroy {
  stats: ApprovalStats | null = null;
  loading = true;

  private refreshSubscription?: Subscription;

  constructor(
    private cxApi: CxApiService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadStats();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
  }

  /**
   * Load approval statistics
   */
  loadStats(): void {
    this.cxApi.getApprovalStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load approval stats:', err);
        this.loading = false;
      },
    });
  }

  /**
   * Auto-refresh every 60 seconds
   */
  startAutoRefresh(): void {
    this.refreshSubscription = interval(60000)
      .pipe(
        startWith(0),
        switchMap(() => this.cxApi.getApprovalStats())
      )
      .subscribe({
        next: (stats) => (this.stats = stats),
        error: (err) => console.error('Auto-refresh failed:', err),
      });
  }

  /**
   * Stop auto-refresh
   */
  stopAutoRefresh(): void {
    this.refreshSubscription?.unsubscribe();
  }

  /**
   * Navigate to approval queue
   */
  viewQueue(): void {
    this.router.navigate(['/approvals']);
  }

  /**
   * Check if urgent items exist
   */
  hasUrgentItems(): boolean {
    return this.stats !== null && this.stats.urgent_count > 0;
  }

  /**
   * Check if oldest pending > 24 hours
   */
  hasOldPending(): boolean {
    return this.stats !== null && this.stats.oldest_pending_hours !== null && this.stats.oldest_pending_hours > 24;
  }

  /**
   * Format hours to human-readable
   */
  formatHours(hours: number | null): string {
    if (hours === null) return 'N/A';
    if (hours < 1) return `${Math.round(hours * 60)} min`;
    return `${hours.toFixed(1)} hrs`;
  }
}
