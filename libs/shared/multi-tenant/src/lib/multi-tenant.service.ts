/**
 * Multi-Tenant Service
 *
 * Provides tenant-aware filtering, isolation, and data management across the platform.
 *
 * Features:
 * - Tenant context management (set/get current tenant)
 * - Tenant-aware data filtering
 * - Per-tenant preferences and state
 * - Cross-tenant access prevention
 * - Tenant information retrieval
 *
 * Usage:
 * ```typescript
 * constructor(private multiTenant: MultiTenantService) {}
 *
 * // Set current tenant context
 * this.multiTenant.setCurrentTenant('tenant-123');
 *
 * // Get current tenant
 * const tenantId = this.multiTenant.getCurrentTenant();
 *
 * // Filter data by tenant
 * const filteredData = this.multiTenant.filterByTenant(allData, 'tenantId');
 *
 * // Store per-tenant preferences
 * this.multiTenant.setTenantPreference('feature-flags', { darkMode: true });
 *
 * // Retrieve per-tenant preferences
 * const prefs = this.multiTenant.getTenantPreference('feature-flags');
 *
 * // Get tenant info
 * const info = this.multiTenant.getTenantInfo();
 *
 * // Observable for tenant changes
 * this.multiTenant.currentTenant$.subscribe(tenantId => {
 *   console.log('Switched to:', tenantId);
 * });
 * ```
 */

import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface TenantInfo {
  id: string;
  name: string;
  createdAt: number;
  region?: string;
  features?: string[];
}

export interface TenantPreferences {
  [key: string]: any;
}

@Injectable({
  providedIn: 'root'
})
export class MultiTenantService {
  private currentTenantSubject = new BehaviorSubject<string>('default-tenant');
  public readonly currentTenant$: Observable<string> = this.currentTenantSubject.asObservable();

  private tenantPreferences = new Map<string, TenantPreferences>();
  private tenantInfo = new Map<string, TenantInfo>();

  constructor() {
    this.loadTenantContext();
  }

  /**
   * Set current tenant context
   */
  setCurrentTenant(tenantId: string): void {
    this.currentTenantSubject.next(tenantId);
    localStorage.setItem('current_tenant', tenantId);
  }

  /**
   * Get current tenant context
   */
  getCurrentTenant(): string {
    return this.currentTenantSubject.value;
  }

  /**
   * Filter array of objects by current tenant
   *
   * @param data - Array of objects to filter
   * @param tenantField - Field name containing tenant ID (default: 'tenantId')
   * @returns Filtered array containing only items for current tenant
   */
  filterByTenant<T extends Record<string, any>>(
    data: T[],
    tenantField: string = 'tenantId'
  ): T[] {
    const currentTenant = this.getCurrentTenant();
    return data.filter(item => item[tenantField] === currentTenant);
  }

  /**
   * Check if object belongs to current tenant
   */
  belongsToCurrentTenant<T extends Record<string, any>>(
    item: T,
    tenantField: string = 'tenantId'
  ): boolean {
    return item[tenantField] === this.getCurrentTenant();
  }

  /**
   * Check if user has access to tenant
   */
  hasAccessToTenant(tenantId: string): boolean {
    // This would typically check against user's authorized tenants
    // For now, allow access if tenant ID is valid
    return tenantId && tenantId.length > 0;
  }

  /**
   * Set tenant preference
   */
  setTenantPreference(key: string, value: any): void {
    const tenantId = this.getCurrentTenant();
    if (!this.tenantPreferences.has(tenantId)) {
      this.tenantPreferences.set(tenantId, {});
    }
    const prefs = this.tenantPreferences.get(tenantId)!;
    prefs[key] = value;

    // Persist to localStorage
    const allPrefs = this.getAllPersistedPreferences();
    allPrefs[tenantId] = prefs;
    localStorage.setItem('tenant_preferences', JSON.stringify(allPrefs));
  }

  /**
   * Get tenant preference
   */
  getTenantPreference<T = any>(key: string, defaultValue?: T): T | undefined {
    const tenantId = this.getCurrentTenant();
    const prefs = this.tenantPreferences.get(tenantId);
    if (!prefs) {
      return defaultValue;
    }
    return prefs[key] !== undefined ? prefs[key] : defaultValue;
  }

  /**
   * Get all preferences for current tenant
   */
  getAllTenantPreferences(): TenantPreferences {
    const tenantId = this.getCurrentTenant();
    return { ...(this.tenantPreferences.get(tenantId) || {}) };
  }

  /**
   * Clear all preferences for current tenant
   */
  clearTenantPreferences(): void {
    const tenantId = this.getCurrentTenant();
    this.tenantPreferences.delete(tenantId);

    const allPrefs = this.getAllPersistedPreferences();
    delete allPrefs[tenantId];
    localStorage.setItem('tenant_preferences', JSON.stringify(allPrefs));
  }

  /**
   * Set tenant information
   */
  setTenantInfo(tenantId: string, info: TenantInfo): void {
    this.tenantInfo.set(tenantId, info);
  }

  /**
   * Get tenant information
   */
  getTenantInfo(tenantId?: string): TenantInfo | undefined {
    const id = tenantId || this.getCurrentTenant();
    return this.tenantInfo.get(id);
  }

  /**
   * Get current tenant information
   */
  getCurrentTenantInfo(): TenantInfo | undefined {
    return this.getTenantInfo();
  }

  /**
   * Validate cross-tenant access (throws if not allowed)
   */
  validateCrossTenantAccess(sourceId: string, targetId: string): void {
    if (sourceId !== targetId) {
      throw new Error(
        `Cross-tenant access denied: Cannot access tenant ${targetId} from context of ${sourceId}`
      );
    }
  }

  /**
   * Validate current tenant context matches required tenant
   */
  validateTenantContext(requiredTenantId: string): void {
    const currentTenant = this.getCurrentTenant();
    if (currentTenant !== requiredTenantId) {
      throw new Error(
        `Tenant context mismatch: Expected ${requiredTenantId}, but current context is ${currentTenant}`
      );
    }
  }

  /**
   * Get all authorized tenants for user
   * Note: This would typically come from user's JWT token
   */
  getAuthorizedTenants(): string[] {
    // For now, return just the current tenant
    // In a real app, this would come from user claims
    return [this.getCurrentTenant()];
  }

  private loadTenantContext(): void {
    const stored = localStorage.getItem('current_tenant');
    if (stored) {
      this.currentTenantSubject.next(stored);
    }

    // Load preferences from localStorage
    const storedPrefs = localStorage.getItem('tenant_preferences');
    if (storedPrefs) {
      try {
        const allPrefs = JSON.parse(storedPrefs);
        Object.entries(allPrefs).forEach(([tenantId, prefs]) => {
          if (typeof prefs === 'object') {
            this.tenantPreferences.set(tenantId, prefs as TenantPreferences);
          }
        });
      } catch (e) {
        console.error('Failed to load tenant preferences:', e);
      }
    }
  }

  private getAllPersistedPreferences(): Record<string, TenantPreferences> {
    const result: Record<string, TenantPreferences> = {};
    this.tenantPreferences.forEach((prefs, tenantId) => {
      result[tenantId] = prefs;
    });
    return result;
  }
}
