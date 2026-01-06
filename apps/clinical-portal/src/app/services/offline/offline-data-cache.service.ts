/**
 * Issue #24: Offline Mode & Sync
 * Offline Data Cache Service
 *
 * Provides caching layer for HTTP requests with offline support.
 * Automatically serves cached data when offline and queues updates for sync.
 */
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError, from } from 'rxjs';
import { catchError, map, switchMap, tap, take } from 'rxjs/operators';

import { OfflineStorageService, STORES, StoreName } from './offline-storage.service';
import { NetworkStatusService } from './network-status.service';
import { SyncQueueService } from './sync-queue.service';

export interface CachedItem<T> {
  id: string;
  data: T;
  cachedAt: number;
  expiresAt: number;
  source: 'server' | 'cache';
}

export interface CacheOptions {
  /** Cache TTL in milliseconds (default: 5 minutes for HIPAA compliance) */
  ttl?: number;
  /** Force refresh from server even if cached */
  forceRefresh?: boolean;
  /** Store name for persistence */
  storeName?: StoreName;
}

// Default TTL: 5 minutes (HIPAA compliant for PHI)
const DEFAULT_TTL = 5 * 60 * 1000;

@Injectable({
  providedIn: 'root',
})
export class OfflineDataCacheService {
  private readonly http = inject(HttpClient);
  private readonly storage = inject(OfflineStorageService);
  private readonly networkStatus = inject(NetworkStatusService);
  private readonly syncQueue = inject(SyncQueueService);

  /**
   * GET request with offline caching
   */
  get<T extends { id: string }>(
    url: string,
    id: string,
    options: CacheOptions = {}
  ): Observable<CachedItem<T>> {
    const { ttl = DEFAULT_TTL, forceRefresh = false, storeName = STORES.METADATA } = options;

    // If online and force refresh, go directly to server
    if (this.networkStatus.isOnline && forceRefresh) {
      return this.fetchAndCache<T>(url, id, storeName, ttl);
    }

    // Try to get from cache first
    return this.storage.get<CachedItem<T>>(storeName, id).pipe(
      switchMap((cached) => {
        const now = Date.now();

        // If cached and not expired, return it
        if (cached && cached.expiresAt > now) {
          // If online, refresh in background
          if (this.networkStatus.isOnline) {
            this.fetchAndCache<T>(url, id, storeName, ttl)
              .pipe(take(1))
              .subscribe();
          }
          return of({ ...cached, source: 'cache' as const });
        }

        // If offline, return stale cache or error
        if (!this.networkStatus.isOnline) {
          if (cached) {
            console.log(`Serving stale cache for ${id} (offline)`);
            return of({ ...cached, source: 'cache' as const });
          }
          return throwError(() => new Error('No cached data available offline'));
        }

        // Online and no valid cache, fetch from server
        return this.fetchAndCache<T>(url, id, storeName, ttl);
      })
    );
  }

  /**
   * GET all items with offline caching
   */
  getAll<T extends { id: string }>(
    url: string,
    options: CacheOptions = {}
  ): Observable<T[]> {
    const { forceRefresh = false, storeName = STORES.METADATA, ttl = DEFAULT_TTL } = options;

    // If offline, return from cache
    if (!this.networkStatus.isOnline) {
      return this.storage.getAll<CachedItem<T>>(storeName).pipe(
        map((items) => items.map((item) => item.data))
      );
    }

    // If force refresh or online, fetch from server
    if (forceRefresh) {
      return this.fetchAllAndCache<T>(url, storeName, ttl);
    }

    // Try cache first, refresh in background
    return this.storage.getAll<CachedItem<T>>(storeName).pipe(
      switchMap((cached) => {
        if (cached.length > 0) {
          // Refresh in background
          this.fetchAllAndCache<T>(url, storeName, ttl)
            .pipe(take(1))
            .subscribe();
          return of(cached.map((item) => item.data));
        }
        return this.fetchAllAndCache<T>(url, storeName, ttl);
      })
    );
  }

  /**
   * POST/CREATE with offline queueing
   */
  create<T>(
    url: string,
    data: T & { id?: string },
    storeName: StoreName
  ): Observable<T> {
    // Generate temp ID if not provided
    const itemWithId: T & { id: string } = {
      ...data,
      id: data.id || `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    };

    // If offline, save locally and queue for sync
    if (!this.networkStatus.isOnline) {
      return this.saveLocallyAndQueue<T & { id: string }>('create', itemWithId, storeName);
    }

    // Online, try server first
    return this.http.post<T>(url, data).pipe(
      tap((result) => {
        // Cache the result
        const resultWithId = result as T & { id: string };
        this.cacheItem(resultWithId, storeName, DEFAULT_TTL)
          .pipe(take(1))
          .subscribe();
      }),
      catchError((error: HttpErrorResponse) => {
        // If server error, save locally and queue
        if (error.status === 0 || error.status >= 500) {
          console.log('Server unavailable, saving locally');
          return this.saveLocallyAndQueue<T & { id: string }>('create', itemWithId, storeName);
        }
        return throwError(() => error);
      })
    );
  }

  /**
   * PUT/UPDATE with offline queueing
   */
  update<T extends { id: string }>(
    url: string,
    data: T,
    storeName: StoreName
  ): Observable<T> {
    // If offline, update locally and queue
    if (!this.networkStatus.isOnline) {
      return this.saveLocallyAndQueue<T>('update', data, storeName);
    }

    // Online, try server first
    return this.http.put<T>(`${url}/${data.id}`, data).pipe(
      tap((result) => {
        this.cacheItem(result, storeName, DEFAULT_TTL)
          .pipe(take(1))
          .subscribe();
      }),
      catchError((error: HttpErrorResponse) => {
        if (error.status === 0 || error.status >= 500) {
          return this.saveLocallyAndQueue<T>('update', data, storeName);
        }
        return throwError(() => error);
      })
    );
  }

  /**
   * DELETE with offline queueing
   */
  delete<T extends { id: string }>(
    url: string,
    id: string,
    storeName: StoreName
  ): Observable<void> {
    // If offline, delete locally and queue
    if (!this.networkStatus.isOnline) {
      return this.deleteLocallyAndQueue(id, storeName);
    }

    // Online, try server first
    return this.http.delete<void>(`${url}/${id}`).pipe(
      tap(() => {
        this.storage.delete(storeName, id).pipe(take(1)).subscribe();
      }),
      catchError((error: HttpErrorResponse) => {
        if (error.status === 0 || error.status >= 500) {
          return this.deleteLocallyAndQueue(id, storeName);
        }
        return throwError(() => error);
      })
    );
  }

  /**
   * Clear cache for a specific store
   */
  clearCache(storeName: StoreName): Observable<boolean> {
    return this.storage.clear(storeName);
  }

  /**
   * Get cache statistics
   */
  async getCacheStats(): Promise<{
    patients: number;
    careGaps: number;
    measures: number;
    evaluations: number;
    pendingSync: number;
    storageUsed: string;
  }> {
    const [patients, careGaps, measures, evaluations, pendingSync, storageEstimate] =
      await Promise.all([
        this.storage.count(STORES.PATIENTS).pipe(take(1)).toPromise(),
        this.storage.count(STORES.CARE_GAPS).pipe(take(1)).toPromise(),
        this.storage.count(STORES.MEASURES).pipe(take(1)).toPromise(),
        this.storage.count(STORES.EVALUATIONS).pipe(take(1)).toPromise(),
        this.storage.count(STORES.SYNC_QUEUE).pipe(take(1)).toPromise(),
        this.storage.getStorageEstimate(),
      ]);

    const usedMB = storageEstimate
      ? (storageEstimate.usage / (1024 * 1024)).toFixed(2)
      : 'unknown';

    return {
      patients: patients || 0,
      careGaps: careGaps || 0,
      measures: measures || 0,
      evaluations: evaluations || 0,
      pendingSync: pendingSync || 0,
      storageUsed: `${usedMB} MB`,
    };
  }

  /**
   * Fetch from server and cache
   */
  private fetchAndCache<T extends { id: string }>(
    url: string,
    id: string,
    storeName: StoreName,
    ttl: number
  ): Observable<CachedItem<T>> {
    return this.http.get<T>(`${url}/${id}`).pipe(
      switchMap((data) => this.cacheItem(data, storeName, ttl)),
      map((cached) => ({ ...cached, source: 'server' as const }))
    );
  }

  /**
   * Fetch all from server and cache
   */
  private fetchAllAndCache<T extends { id: string }>(
    url: string,
    storeName: StoreName,
    ttl: number
  ): Observable<T[]> {
    return this.http.get<T[]>(url).pipe(
      tap((items) => {
        // Cache all items
        items.forEach((item) => {
          this.cacheItem(item, storeName, ttl).pipe(take(1)).subscribe();
        });
        // Update last sync time
        this.storage.setLastSyncTime(storeName).pipe(take(1)).subscribe();
      })
    );
  }

  /**
   * Cache a single item
   */
  private cacheItem<T extends { id: string }>(
    data: T,
    storeName: StoreName,
    ttl: number
  ): Observable<CachedItem<T>> {
    const now = Date.now();
    const cached: CachedItem<T> = {
      id: data.id,
      data,
      cachedAt: now,
      expiresAt: now + ttl,
      source: 'server',
    };

    return this.storage.put(storeName, cached).pipe(map(() => cached));
  }

  /**
   * Save locally and queue for sync
   */
  private saveLocallyAndQueue<T extends { id: string }>(
    action: 'create' | 'update',
    data: T,
    storeName: StoreName
  ): Observable<T> {
    const cached: CachedItem<T> = {
      id: data.id,
      data,
      cachedAt: Date.now(),
      expiresAt: Date.now() + DEFAULT_TTL,
      source: 'cache',
    };

    return this.storage.put(storeName, cached).pipe(
      switchMap(() => this.syncQueue.addToQueue(action, storeName, data)),
      map(() => data)
    );
  }

  /**
   * Delete locally and queue for sync
   */
  private deleteLocallyAndQueue(id: string, storeName: StoreName): Observable<void> {
    return this.storage.delete(storeName, id).pipe(
      switchMap(() => this.syncQueue.addToQueue('delete', storeName, { id })),
      map(() => void 0)
    );
  }
}
