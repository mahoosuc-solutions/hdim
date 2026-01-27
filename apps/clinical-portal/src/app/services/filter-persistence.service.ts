import { Injectable } from '@angular/core';
import { LoggerService } from './logger.service';

/**
 * FilterPersistenceService - Manages filter state persistence
 *
 * Stores filter configurations in localStorage and restores them when users
 * navigate back to pages. Each page has its own filter namespace.
 */
@Injectable({
  providedIn: 'root'
})
export class FilterPersistenceService {
  private readonly logger: any;
  private readonly STORAGE_PREFIX = 'clinical-portal-filters';

  constructor(private readonly loggerService: LoggerService) {
    this.logger = this.loggerService.withContext('FilterPersistenceService');
  }

  /**
   * Save filters for a specific page
   */
  saveFilters(pageKey: string, filters: Record<string, any>): void {
    try {
      const key = this.getStorageKey(pageKey);
      const data = {
        filters,
        timestamp: new Date().toISOString()
      };
      localStorage.setItem(key, JSON.stringify(data));
    } catch (error) {
      this.logger.warn('Failed to save filters to localStorage:', error);
    }
  }

  /**
   * Load filters for a specific page
   */
  loadFilters(pageKey: string): Record<string, any> | null {
    try {
      const key = this.getStorageKey(pageKey);
      const stored = localStorage.getItem(key);

      if (!stored) {
        return null;
      }

      const data = JSON.parse(stored);

      // Check if filters are not too old (30 days)
      const timestamp = new Date(data.timestamp);
      const age = Date.now() - timestamp.getTime();
      const maxAge = 30 * 24 * 60 * 60 * 1000; // 30 days

      if (age > maxAge) {
        this.clearFilters(pageKey);
        return null;
      }

      return data.filters;
    } catch (error) {
      this.logger.warn('Failed to load filters from localStorage:', error);
      return null;
    }
  }

  /**
   * Clear filters for a specific page
   */
  clearFilters(pageKey: string): void {
    try {
      const key = this.getStorageKey(pageKey);
      localStorage.removeItem(key);
    } catch (error) {
      this.logger.warn('Failed to clear filters from localStorage:', error);
    }
  }

  /**
   * Clear all filters
   */
  clearAllFilters(): void {
    try {
      const keys = Object.keys(localStorage);
      keys.forEach(key => {
        if (key.startsWith(this.STORAGE_PREFIX)) {
          localStorage.removeItem(key);
        }
      });
    } catch (error) {
      this.logger.warn('Failed to clear all filters from localStorage:', error);
    }
  }

  /**
   * Get storage key for a page
   */
  private getStorageKey(pageKey: string): string {
    return `${this.STORAGE_PREFIX}-${pageKey}`;
  }
}
