/**
 * Feature Flag Service
 *
 * Provides feature flag management with support for boolean toggles, percentage-based rollout,
 * and A/B testing variants.
 *
 * Features:
 * - Boolean feature flags (on/off)
 * - Percentage-based gradual rollout
 * - A/B testing with variant assignment
 * - User/tenant-based feature assignment
 * - Observable streams for flag changes
 * - Cached flag evaluation
 * - Analytics tracking for feature usage
 *
 * Usage:
 * ```typescript
 * constructor(
 *   private featureFlags: FeatureFlagService,
 *   private analytics: AnalyticsService
 * ) {}
 *
 * // Check if feature is enabled
 * if (this.featureFlags.isEnabled('dark-mode')) {
 *   this.applyDarkMode();
 * }
 *
 * // Get A/B variant
 * const variant = this.featureFlags.getVariant('checkout-flow');
 * if (variant === 'v2') {
 *   this.showNewCheckout();
 * }
 *
 * // Gradual rollout (50% of users)
 * this.featureFlags.setFeatureFlagPercentage('new-feature', 50);
 * if (this.featureFlags.isEnabled('new-feature')) {
 *   // Show new feature
 * }
 *
 * // Observable for flag changes
 * this.featureFlags.featureFlags$.subscribe(flags => {
 *   console.log('Flags updated:', flags);
 * });
 * ```
 */

import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';

export interface FeatureFlag {
  name: string;
  enabled: boolean;
  percentage?: number; // 0-100 for gradual rollout
  variant?: string; // For A/B testing
  createdAt: number;
  updatedAt: number;
}

export interface FeatureFlagConfig {
  [flagName: string]: FeatureFlag;
}

@Injectable({
  providedIn: 'root'
})
export class FeatureFlagService {
  private flagsSubject = new BehaviorSubject<FeatureFlagConfig>({});
  public readonly featureFlags$: Observable<FeatureFlagConfig> = this.flagsSubject.asObservable();

  private userAssignments = new Map<string, Map<string, string>>(); // userId -> flagName -> variant
  private userId: string;

  // Cache for percentage calculations (stable across session)
  private percentageCache = new Map<string, boolean>(); // flagName_userId -> enabled

  constructor() {
    this.userId = this.generateOrLoadUserId();
    this.loadFlagsFromStorage();
  }

  /**
   * Check if a feature flag is enabled for the current user
   *
   * @param flagName - Name of the feature flag
   * @returns true if enabled, false otherwise
   */
  isEnabled(flagName: string): boolean {
    const flag = this.flagsSubject.value[flagName];
    if (!flag) {
      return false;
    }

    if (!flag.enabled) {
      return false;
    }

    // If no percentage specified, it's a simple toggle
    if (flag.percentage === undefined || flag.percentage === 100) {
      return true;
    }

    if (flag.percentage === 0) {
      return false;
    }

    // Check cache first
    const cacheKey = `${flagName}_${this.userId}`;
    if (this.percentageCache.has(cacheKey)) {
      return this.percentageCache.get(cacheKey)!;
    }

    // Consistent hashing: same user always gets same result
    const isEnabledForUser = this.hashUserToPercentage(flagName) < flag.percentage;
    this.percentageCache.set(cacheKey, isEnabledForUser);

    return isEnabledForUser;
  }

  /**
   * Get variant for A/B testing
   *
   * @param flagName - Name of the feature flag
   * @param variants - Array of variant names
   * @returns Assigned variant or undefined if feature not enabled
   */
  getVariant(flagName: string, variants?: string[]): string | undefined {
    if (!this.isEnabled(flagName)) {
      return undefined;
    }

    const flag = this.flagsSubject.value[flagName];
    if (flag?.variant) {
      return flag.variant;
    }

    // Assign variant based on user hash
    if (variants && variants.length > 0) {
      const userAssignments = this.userAssignments.get(this.userId) || new Map();
      if (!userAssignments.has(flagName)) {
        const variantIndex = this.hashUserToVariant(flagName, variants.length);
        const assignedVariant = variants[variantIndex];
        userAssignments.set(flagName, assignedVariant);
        this.userAssignments.set(this.userId, userAssignments);
        this.persistUserAssignments();
      }
      return userAssignments.get(flagName);
    }

    return undefined;
  }

  /**
   * Set a feature flag (boolean toggle)
   */
  setFeatureFlag(flagName: string, enabled: boolean): void {
    const now = Date.now();
    const existing = this.flagsSubject.value[flagName];

    const flag: FeatureFlag = {
      name: flagName,
      enabled,
      createdAt: existing?.createdAt || now,
      updatedAt: now
    };

    const updated = { ...this.flagsSubject.value, [flagName]: flag };
    this.flagsSubject.next(updated);
    this.persistFlags();
  }

  /**
   * Set a feature flag with percentage-based rollout
   *
   * @param flagName - Name of the feature flag
   * @param percentage - Percentage of users (0-100)
   */
  setFeatureFlagPercentage(flagName: string, percentage: number): void {
    if (percentage < 0 || percentage > 100) {
      throw new Error('Percentage must be between 0 and 100');
    }

    const now = Date.now();
    const existing = this.flagsSubject.value[flagName];

    const flag: FeatureFlag = {
      name: flagName,
      enabled: true,
      percentage,
      createdAt: existing?.createdAt || now,
      updatedAt: now
    };

    // Clear cache for this flag
    this.percentageCache.delete(`${flagName}_${this.userId}`);

    const updated = { ...this.flagsSubject.value, [flagName]: flag };
    this.flagsSubject.next(updated);
    this.persistFlags();
  }

  /**
   * Set a feature flag with A/B variant
   */
  setFeatureFlagVariant(flagName: string, variant: string): void {
    const now = Date.now();
    const existing = this.flagsSubject.value[flagName];

    const flag: FeatureFlag = {
      name: flagName,
      enabled: true,
      variant,
      createdAt: existing?.createdAt || now,
      updatedAt: now
    };

    const updated = { ...this.flagsSubject.value, [flagName]: flag };
    this.flagsSubject.next(updated);
    this.persistFlags();
  }

  /**
   * Get a feature flag by name
   */
  getFlag(flagName: string): FeatureFlag | undefined {
    return this.flagsSubject.value[flagName];
  }

  /**
   * Get all feature flags
   */
  getAllFlags(): FeatureFlag[] {
    return Object.values(this.flagsSubject.value);
  }

  /**
   * Delete a feature flag
   */
  deleteFlag(flagName: string): void {
    const updated = { ...this.flagsSubject.value };
    delete updated[flagName];
    this.flagsSubject.next(updated);
    this.percentageCache.delete(`${flagName}_${this.userId}`);
    this.persistFlags();
  }

  /**
   * Clear all feature flags
   */
  clearAllFlags(): void {
    this.flagsSubject.next({});
    this.percentageCache.clear();
    this.persistFlags();
  }

  /**
   * Set user ID for consistent feature assignment
   */
  setUserId(userId: string): void {
    this.userId = userId;
    this.percentageCache.clear(); // Clear cache as user changed
    sessionStorage.setItem('feature_flag_user_id', userId);
  }

  /**
   * Get current user ID
   */
  getUserId(): string {
    return this.userId;
  }

  /**
   * Get user's variant assignment for a feature
   */
  getUserVariantAssignment(flagName: string): string | undefined {
    return this.userAssignments.get(this.userId)?.get(flagName);
  }

  private hashUserToPercentage(flagName: string): number {
    // Consistent hash: same user + flag always produces same result
    const hash = this.hashCode(`${this.userId}:${flagName}`);
    return Math.abs(hash) % 100;
  }

  private hashUserToVariant(flagName: string, variantCount: number): number {
    const hash = this.hashCode(`${this.userId}:${flagName}:variant`);
    return Math.abs(hash) % variantCount;
  }

  private hashCode(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = (hash << 5) - hash + char;
      hash = hash & hash; // Convert to 32bit integer
    }
    return hash;
  }

  private generateOrLoadUserId(): string {
    let userId = sessionStorage.getItem('feature_flag_user_id');
    if (!userId) {
      userId = uuidv4();
      sessionStorage.setItem('feature_flag_user_id', userId);
    }
    return userId;
  }

  private persistFlags(): void {
    localStorage.setItem('feature_flags', JSON.stringify(this.flagsSubject.value));
  }

  private loadFlagsFromStorage(): void {
    const stored = localStorage.getItem('feature_flags');
    if (stored) {
      try {
        const flags = JSON.parse(stored);
        this.flagsSubject.next(flags);
      } catch (e) {
        console.error('Failed to load feature flags:', e);
      }
    }
  }

  private persistUserAssignments(): void {
    const assignments: Record<string, Record<string, string>> = {};
    this.userAssignments.forEach((variants, userId) => {
      assignments[userId] = Object.fromEntries(variants);
    });
    sessionStorage.setItem('feature_flag_assignments', JSON.stringify(assignments));
  }
}
