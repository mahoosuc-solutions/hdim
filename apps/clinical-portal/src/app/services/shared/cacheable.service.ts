import { Injectable } from '@angular/core';

/**
 * Cache entry with data and timestamp
 */
export interface CacheEntry<T> {
  data: T;
  timestamp: number;
}

/**
 * Cache configuration options
 */
export interface CacheConfig {
  ttlMs?: number;
  maxEntries?: number;
}

/**
 * CacheableService - Base class for services that need caching
 *
 * Provides a standardized caching infrastructure with:
 * - TTL-based expiration (default 5 minutes)
 * - Max entries limit to prevent memory leaks
 * - Pattern-based cache invalidation
 * - Type-safe cache operations
 *
 * Usage:
 * ```typescript
 * @Injectable({ providedIn: 'root' })
 * export class MyService extends CacheableService {
 *   constructor() {
 *     super({ ttlMs: 5 * 60 * 1000 });
 *   }
 *
 *   getData(id: string): Observable<Data> {
 *     const cached = this.getCached<Data>(`data:${id}`);
 *     if (cached) return of(cached);
 *
 *     return this.http.get<Data>(`/api/data/${id}`).pipe(
 *       tap(data => this.setCache(`data:${id}`, data))
 *     );
 *   }
 * }
 * ```
 */
@Injectable()
export abstract class CacheableService {
  protected cache = new Map<string, CacheEntry<unknown>>();
  protected readonly cacheTtlMs: number;
  protected readonly maxEntries: number;

  constructor(config: CacheConfig = {}) {
    this.cacheTtlMs = config.ttlMs ?? 5 * 60 * 1000; // 5 minutes default
    this.maxEntries = config.maxEntries ?? 1000;
  }

  /**
   * Get cached data if valid
   */
  protected getCached<T>(key: string): T | null {
    const entry = this.cache.get(key);
    if (!entry) return null;

    if (this.isCacheValid(key)) {
      return entry.data as T;
    }

    // Remove expired entry
    this.cache.delete(key);
    return null;
  }

  /**
   * Set cache entry
   */
  protected setCache<T>(key: string, data: T): void {
    // Enforce max entries limit
    if (this.cache.size >= this.maxEntries) {
      this.evictOldestEntries(Math.floor(this.maxEntries * 0.1)); // Remove 10%
    }

    this.cache.set(key, {
      data,
      timestamp: Date.now(),
    });
  }

  /**
   * Check if cache entry is still valid
   */
  protected isCacheValid(key: string): boolean {
    const entry = this.cache.get(key);
    if (!entry) return false;

    return Date.now() - entry.timestamp < this.cacheTtlMs;
  }

  /**
   * Invalidate cache entries matching a pattern
   * If no pattern provided, clears all cache
   */
  protected invalidateCache(keyPattern?: string | RegExp): void {
    if (!keyPattern) {
      this.cache.clear();
      return;
    }

    const pattern =
      typeof keyPattern === 'string' ? new RegExp(keyPattern) : keyPattern;

    for (const key of this.cache.keys()) {
      if (pattern.test(key)) {
        this.cache.delete(key);
      }
    }
  }

  /**
   * Invalidate cache for a specific patient
   */
  protected invalidatePatientCache(patientId: string): void {
    this.invalidateCache(new RegExp(`^.*:${patientId}$|^${patientId}:`));
  }

  /**
   * Get cache statistics
   */
  protected getCacheStats(): {
    size: number;
    validEntries: number;
    expiredEntries: number;
  } {
    let validEntries = 0;
    let expiredEntries = 0;

    for (const key of this.cache.keys()) {
      if (this.isCacheValid(key)) {
        validEntries++;
      } else {
        expiredEntries++;
      }
    }

    return {
      size: this.cache.size,
      validEntries,
      expiredEntries,
    };
  }

  /**
   * Evict oldest cache entries
   */
  private evictOldestEntries(count: number): void {
    const entries = Array.from(this.cache.entries())
      .sort((a, b) => a[1].timestamp - b[1].timestamp)
      .slice(0, count);

    for (const [key] of entries) {
      this.cache.delete(key);
    }
  }

  /**
   * Clean up expired entries (call periodically if needed)
   */
  protected cleanupExpiredEntries(): number {
    let removed = 0;
    for (const key of this.cache.keys()) {
      if (!this.isCacheValid(key)) {
        this.cache.delete(key);
        removed++;
      }
    }
    return removed;
  }
}
