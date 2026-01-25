import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { LoggerService } from '../../services/logger.service';
import { AdminService } from '../../services/admin.service';
import { ServiceHealth } from '../../models/admin.model';
import { SERVICE_DEFINITIONS, ServiceDefinitionMetadata } from '../../models/service-definitions';
import {
  SERVICE_DEPENDENCIES,
  ServiceLink,
  getDependenciesForService,
  getDependentsOfService,
  calculateImpact,
} from '../../models/service-dependencies';

/**
 * Service Dependency Graph Component
 *
 * Visualizes service dependencies and shows impact analysis.
 * Helps understand how services interact and what would be affected if a service fails.
 *
 * Features:
 * - Service dependency visualization
 * - Impact analysis (what breaks if service X fails)
 * - Category-based filtering
 * - Health status integration
 *
 * HIPAA Compliance: No PHI is displayed in this component.
 */
@Component({
  selector: 'app-service-graph',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './service-graph.component.html',
  styleUrls: ['./service-graph.component.scss'],
})
export class ServiceGraphComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private logger = this.loggerService.withContext('ServiceGraphComponent');

  // State
  services: ServiceDefinitionMetadata[] = SERVICE_DEFINITIONS.filter((s) => s.port !== null);
  dependencies: ServiceLink[] = SERVICE_DEPENDENCIES;
  serviceHealth: Map<string, ServiceHealth> = new Map();
  selectedService: string | null = null;
  selectedCategory: string = 'all';
  loading = true;

  // Computed
  serviceDependencies: ServiceLink[] = [];
  serviceDependents: ServiceLink[] = [];
  impactedServices: Set<string> = new Set();

  constructor(
    private loggerService: LoggerService,
    private adminService: AdminService
  ) {}

  ngOnInit(): void {
    this.logger.info('Initializing Service Dependency Graph');
    this.loadServiceHealth();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load service health status
   */
  loadServiceHealth(): void {
    this.loading = true;
    this.adminService
      .getSystemHealthV2()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (health) => {
          health.services.forEach((service) => {
            this.serviceHealth.set(service.name, service);
          });
          this.loading = false;
          this.logger.info('Loaded service health', { count: health.services.length });
        },
        error: (error) => {
          this.logger.error('Failed to load service health', error);
          this.loading = false;
        },
      });
  }

  /**
   * Get filtered services based on selected category
   */
  getFilteredServices(): ServiceDefinitionMetadata[] {
    if (this.selectedCategory === 'all') {
      return this.services;
    }
    return this.services.filter((s) => s.category === this.selectedCategory);
  }

  /**
   * Get unique categories
   */
  getCategories(): string[] {
    const categories = new Set(this.services.map((s) => s.category));
    return Array.from(categories).sort();
  }

  /**
   * Select a service to view its dependencies
   */
  selectService(serviceId: string): void {
    this.selectedService = serviceId;
    this.serviceDependencies = getDependenciesForService(serviceId);
    this.serviceDependents = getDependentsOfService(serviceId);
    this.impactedServices = calculateImpact(serviceId);

    this.logger.info('Selected service', {
      serviceId,
      dependencies: this.serviceDependencies.length,
      dependents: this.serviceDependents.length,
      impacted: this.impactedServices.size,
    });
  }

  /**
   * Clear service selection
   */
  clearSelection(): void {
    this.selectedService = null;
    this.serviceDependencies = [];
    this.serviceDependents = [];
    this.impactedServices.clear();
  }

  /**
   * Get service by ID
   */
  getService(serviceId: string): ServiceDefinitionMetadata | undefined {
    return this.services.find((s) => s.id === serviceId);
  }

  /**
   * Get service health status
   */
  getServiceStatus(serviceId: string): 'UP' | 'DOWN' | 'DEGRADED' | 'UNKNOWN' {
    const health = this.serviceHealth.get(serviceId);
    return health?.status || 'UNKNOWN';
  }

  /**
   * Check if service is in impacted list
   */
  isImpacted(serviceId: string): boolean {
    return this.impactedServices.has(serviceId);
  }

  /**
   * Get dependency type icon
   */
  getDependencyTypeIcon(type: string): string {
    switch (type) {
      case 'HTTP':
        return '🌐';
      case 'KAFKA':
        return '📨';
      case 'DATABASE':
        return '🗄️';
      case 'CACHE':
        return '⚡';
      default:
        return '🔗';
    }
  }

  /**
   * Get count of dependencies by type
   */
  getDependencyCountByType(): Record<string, number> {
    const counts: Record<string, number> = {
      HTTP: 0,
      KAFKA: 0,
      DATABASE: 0,
      CACHE: 0,
    };

    this.dependencies.forEach((dep) => {
      counts[dep.type]++;
    });

    return counts;
  }

  /**
   * Get total dependency count
   */
  getTotalDependencies(): number {
    return this.dependencies.length;
  }
}
