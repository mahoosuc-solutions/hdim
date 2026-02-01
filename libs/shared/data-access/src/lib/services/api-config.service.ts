import { Injectable } from '@angular/core';

/**
 * API Configuration Service (Shared)
 * Manages API endpoints and configuration across all micro frontends
 */
@Injectable({
  providedIn: 'root',
})
export class ApiConfigService {
  private readonly config = {
    defaultTenantId: 'acme-health',
    backendPatterns: [
      '/fhir',
      '/quality-measure',
      '/cql-engine',
      '/care-gap',
      '/patient',
      '/qrda',
      '/analytics',
      '/ai-assistant',
      '/gateway',
    ],
    headers: {
      tenantId: 'X-Tenant-ID',
      authorization: 'Authorization',
    },
  };

  private tenantId: string = this.config.defaultTenantId;

  /**
   * Get current tenant ID
   */
  getTenantId(): string {
    return this.tenantId;
  }

  /**
   * Set tenant ID (e.g., from user session)
   */
  setTenantId(tenantId: string): void {
    this.tenantId = tenantId;
  }

  /**
   * Check if URL is a backend API call
   */
  isBackendUrl(url: string): boolean {
    return this.config.backendPatterns.some((pattern) => url.includes(pattern));
  }

  /**
   * Get configuration
   */
  getConfig() {
    return { ...this.config };
  }
}
