import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { interval, Subject, forkJoin, of } from 'rxjs';
import { takeUntil, switchMap, catchError } from 'rxjs/operators';
import { PrometheusService, ServiceMetrics } from '../../services/prometheus.service';
import { LoggerService } from '../../services/logger.service';
import { SERVICE_DEFINITIONS, ServiceDefinitionMetadata } from '../../models/service-definitions';

/**
 * Real-Time Metrics Dashboard
 *
 * Displays live metrics from Prometheus for all HDIM services:
 * - CPU usage
 * - Memory usage
 * - Request rate
 * - Error rate
 * - P95 latency
 *
 * Auto-refreshes every 5 seconds to provide real-time visibility.
 *
 * HIPAA Compliance: No PHI is displayed in this component.
 * Only system performance metrics are shown.
 */
@Component({
  selector: 'app-real-time-metrics',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './real-time-metrics.component.html',
  styleUrls: ['./real-time-metrics.component.scss'],
})
export class RealTimeMetricsComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private logger!: ReturnType<LoggerService['withContext']>;

  // State
  serviceMetrics: Map<string, ServiceMetrics> = new Map();
  loading = true;
  prometheusAvailable = false;
  selectedTimeRange: TimeRange = '1h';
  selectedCategory = 'all';
  autoRefresh = true;
  lastUpdate: Date | null = null;
  errorMessage: string | null = null;

  // Service definitions for filtering and categorization
  allServices: ServiceDefinitionMetadata[] = SERVICE_DEFINITIONS;

  // Time range options
  timeRanges: { label: string; value: TimeRange }[] = [
    { label: '15 minutes', value: '15m' },
    { label: '1 hour', value: '1h' },
    { label: '6 hours', value: '6h' },
    { label: '24 hours', value: '24h' },
  ];

  constructor(
    private prometheusService: PrometheusService,
    private loggerService: LoggerService
  ) {
    this.logger = this.loggerService.withContext('RealTimeMetricsComponent');
  }

  ngOnInit(): void {
    this.logger.info('Initializing Real-Time Metrics Dashboard');
    this.checkPrometheusAvailability();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Check if Prometheus is available
   * If not, display mock data for development
   */
  checkPrometheusAvailability(): void {
    this.prometheusService.isPrometheusAvailable().subscribe({
      next: (available) => {
        this.prometheusAvailable = available;
        if (available) {
          this.startAutoRefresh();
        } else {
          this.loadMockMetrics();
        }
      },
      error: () => {
        this.prometheusAvailable = false;
        this.logger.warn('Prometheus not available, using mock data');
        this.loadMockMetrics();
      },
    });
  }

  /**
   * Start auto-refresh timer
   * Fetches metrics every 5 seconds
   */
  startAutoRefresh(): void {
    if (!this.autoRefresh) return;

    this.logger.info('Starting auto-refresh (5 second interval)');

    // Fetch immediately
    this.fetchAllMetrics();

    // Then fetch every 5 seconds
    interval(5000)
      .pipe(
        takeUntil(this.destroy$),
        switchMap(() => this.fetchAllMetricsObservable())
      )
      .subscribe({
        next: (metricsArray) => {
          this.updateMetrics(metricsArray);
          this.lastUpdate = new Date();
          this.loading = false;
          this.errorMessage = null;
        },
        error: (error) => {
          this.logger.error('Failed to fetch metrics', error);
          this.errorMessage = 'Failed to fetch metrics from Prometheus';
          this.loading = false;
        },
      });
  }

  /**
   * Fetch metrics for all services
   * Uses forkJoin to parallelize Prometheus queries
   */
  fetchAllMetrics(): void {
    this.loading = true;

    this.fetchAllMetricsObservable().subscribe({
      next: (metricsArray) => {
        this.updateMetrics(metricsArray);
        this.lastUpdate = new Date();
        this.loading = false;
        this.errorMessage = null;
      },
      error: (error) => {
        this.logger.error('Failed to fetch metrics', error);
        this.errorMessage = 'Failed to fetch metrics from Prometheus';
        this.loading = false;
      },
    });
  }

  /**
   * Fetch all metrics as an observable
   * Used by both manual fetch and auto-refresh
   */
  private fetchAllMetricsObservable() {
    const servicesWithPorts = this.allServices.filter((s) => s.port !== null);
    const metricsObservables = servicesWithPorts.map((service) =>
      this.prometheusService.getAllMetrics(service.id).pipe(
        catchError((error) => {
          this.logger.warn(`Failed to fetch metrics for ${service.id}`, error);
          return of(null); // Return null for failed services, don't fail entire request
        })
      )
    );

    return forkJoin(metricsObservables);
  }

  /**
   * Update metrics map with new data
   */
  private updateMetrics(metricsArray: (ServiceMetrics | null)[]): void {
    metricsArray.forEach((metrics) => {
      if (metrics) {
        this.serviceMetrics.set(metrics.serviceName, metrics);
      }
    });
  }

  /**
   * Load mock metrics for development
   * Used when Prometheus is not available
   */
  loadMockMetrics(): void {
    this.logger.info('Loading mock metrics');
    this.loading = true;

    const servicesWithPorts = this.allServices.filter((s) => s.port !== null);

    servicesWithPorts.forEach((service) => {
      const mockMetrics: ServiceMetrics = {
        serviceName: service.id,
        cpuUsage: Math.random() * 60 + 10, // 10-70%
        memoryUsageMb: Math.random() * 800 + 200, // 200-1000 MB
        requestsPerSecond: Math.random() * 100 + 5, // 5-105 req/s
        errorsPerSecond: Math.random() * 2, // 0-2 err/s
        p95LatencyMs: Math.random() * 500 + 50, // 50-550 ms
      };
      this.serviceMetrics.set(service.id, mockMetrics);
    });

    this.lastUpdate = new Date();
    this.loading = false;
  }

  /**
   * Get filtered services based on selected category
   */
  getFilteredServices(): ServiceDefinitionMetadata[] {
    if (this.selectedCategory === 'all') {
      return this.allServices.filter((s) => s.port !== null);
    }
    return this.allServices.filter(
      (s) => s.category === this.selectedCategory && s.port !== null
    );
  }

  /**
   * Get unique categories for filtering
   */
  getCategories(): string[] {
    const categories = new Set(this.allServices.map((s) => s.category));
    return Array.from(categories).sort();
  }

  /**
   * Toggle auto-refresh
   */
  toggleAutoRefresh(): void {
    this.autoRefresh = !this.autoRefresh;
    if (this.autoRefresh && this.prometheusAvailable) {
      this.startAutoRefresh();
    } else {
      this.destroy$.next(); // Stop current auto-refresh
    }
  }

  /**
   * Manual refresh button handler
   */
  refresh(): void {
    if (this.prometheusAvailable) {
      this.fetchAllMetrics();
    } else {
      this.loadMockMetrics();
    }
  }

  /**
   * Get status color based on metric value
   */
  getMetricStatus(
    type: 'cpu' | 'memory' | 'errors' | 'latency',
    value: number
  ): 'healthy' | 'warning' | 'critical' {
    switch (type) {
      case 'cpu':
        if (value > 80) return 'critical';
        if (value > 60) return 'warning';
        return 'healthy';
      case 'memory':
        if (value > 850) return 'critical';
        if (value > 600) return 'warning';
        return 'healthy';
      case 'errors':
        if (value > 5) return 'critical';
        if (value > 1) return 'warning';
        return 'healthy';
      case 'latency':
        if (value > 500) return 'critical';
        if (value > 300) return 'warning';
        return 'healthy';
      default:
        return 'healthy';
    }
  }

  /**
   * Format number to fixed decimal places
   */
  formatNumber(value: number, decimals: number = 2): string {
    return value.toFixed(decimals);
  }
}

/**
 * Time Range Type
 */
export type TimeRange = '15m' | '1h' | '6h' | '24h';
