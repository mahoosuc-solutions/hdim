/**
 * Issue #24: Offline Mode & Sync
 * Sync Queue Service
 *
 * Manages a queue of pending actions to be synced when connectivity is restored.
 * Handles conflict resolution and retry logic.
 */
import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, Subject, from, of, forkJoin, timer } from 'rxjs';
import {
  catchError,
  concatMap,
  filter,
  map,
  switchMap,
  take,
  takeUntil,
  tap,
} from 'rxjs/operators';
// Simple UUID v4 generator (no external dependency)
function generateUUID(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

import {
  OfflineStorageService,
  STORES,
  StoreName,
  SyncQueueItem,
} from './offline-storage.service';
import { NetworkStatusService } from './network-status.service';

export interface SyncResult {
  success: boolean;
  syncedCount: number;
  failedCount: number;
  errors: Array<{ id: string; error: string }>;
}

export interface SyncProgress {
  total: number;
  completed: number;
  failed: number;
  currentItem?: string;
  isRunning: boolean;
}

export interface ConflictResolution {
  strategy: 'client-wins' | 'server-wins' | 'merge' | 'manual';
  resolver?: (local: unknown, server: unknown) => unknown;
}

const MAX_RETRY_COUNT = 3;
const RETRY_DELAY_MS = 5000;

@Injectable({
  providedIn: 'root',
})
export class SyncQueueService implements OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly syncInProgress = new BehaviorSubject<boolean>(false);
  private readonly syncProgress = new BehaviorSubject<SyncProgress>({
    total: 0,
    completed: 0,
    failed: 0,
    isRunning: false,
  });
  private readonly pendingCount = new BehaviorSubject<number>(0);

  // Endpoint mapping for different stores
  private readonly endpoints: Record<StoreName, string> = {
    [STORES.PATIENTS]: '/api/v1/patients',
    [STORES.CARE_GAPS]: '/api/v1/care-gaps',
    [STORES.MEASURES]: '/api/v1/measures',
    [STORES.EVALUATIONS]: '/api/v1/evaluations',
    [STORES.SYNC_QUEUE]: '',
    [STORES.METADATA]: '',
  };

  // Default conflict resolution strategy
  private conflictResolution: ConflictResolution = { strategy: 'client-wins' };

  // Public observables
  readonly isSyncing$ = this.syncInProgress.asObservable();
  readonly progress$ = this.syncProgress.asObservable();
  readonly pendingCount$ = this.pendingCount.asObservable();
  readonly hasPendingChanges$ = this.pendingCount$.pipe(map((count) => count > 0));

  constructor(
    private http: HttpClient,
    private storage: OfflineStorageService,
    private networkStatus: NetworkStatusService
  ) {
    this.initializeAutoSync();
    this.updatePendingCount();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Add an action to the sync queue
   */
  addToQueue(
    action: 'create' | 'update' | 'delete',
    storeName: StoreName,
    data: unknown
  ): Observable<boolean> {
    const queueItem: SyncQueueItem = {
      id: generateUUID(),
      action,
      storeName,
      data,
      timestamp: Date.now(),
      retryCount: 0,
    };

    return this.storage.put(STORES.SYNC_QUEUE, queueItem).pipe(
      tap((success) => {
        if (success) {
          this.updatePendingCount();
          // Attempt immediate sync if online
          if (this.networkStatus.isOnline) {
            this.sync().pipe(take(1)).subscribe();
          }
        }
      })
    );
  }

  /**
   * Get all pending sync items
   */
  getPendingItems(): Observable<SyncQueueItem[]> {
    return this.storage.getAll<SyncQueueItem>(STORES.SYNC_QUEUE).pipe(
      map((items) => items.sort((a, b) => a.timestamp - b.timestamp))
    );
  }

  /**
   * Remove an item from the sync queue
   */
  removeFromQueue(id: string): Observable<boolean> {
    return this.storage.delete(STORES.SYNC_QUEUE, id).pipe(
      tap(() => this.updatePendingCount())
    );
  }

  /**
   * Clear the entire sync queue
   */
  clearQueue(): Observable<boolean> {
    return this.storage.clear(STORES.SYNC_QUEUE).pipe(
      tap(() => this.updatePendingCount())
    );
  }

  /**
   * Manually trigger a sync
   */
  sync(): Observable<SyncResult> {
    // Don't start if already syncing or offline
    if (this.syncInProgress.value || !this.networkStatus.isOnline) {
      return of({
        success: false,
        syncedCount: 0,
        failedCount: 0,
        errors: [{ id: '', error: 'Sync not possible: already syncing or offline' }],
      });
    }

    this.syncInProgress.next(true);

    return this.getPendingItems().pipe(
      switchMap((items) => {
        if (items.length === 0) {
          return of({
            success: true,
            syncedCount: 0,
            failedCount: 0,
            errors: [],
          });
        }

        this.syncProgress.next({
          total: items.length,
          completed: 0,
          failed: 0,
          isRunning: true,
        });

        // Process items sequentially to maintain order
        return this.processQueueItems(items);
      }),
      tap((result) => {
        this.syncInProgress.next(false);
        this.syncProgress.next({
          ...this.syncProgress.value,
          isRunning: false,
        });
        this.updatePendingCount();
      }),
      catchError((error) => {
        this.syncInProgress.next(false);
        this.syncProgress.next({
          ...this.syncProgress.value,
          isRunning: false,
        });
        return of({
          success: false,
          syncedCount: 0,
          failedCount: 0,
          errors: [{ id: '', error: error.message }],
        });
      })
    );
  }

  /**
   * Set conflict resolution strategy
   */
  setConflictResolution(resolution: ConflictResolution): void {
    this.conflictResolution = resolution;
  }

  /**
   * Process queue items sequentially
   */
  private processQueueItems(items: SyncQueueItem[]): Observable<SyncResult> {
    const result: SyncResult = {
      success: true,
      syncedCount: 0,
      failedCount: 0,
      errors: [],
    };

    return from(items).pipe(
      concatMap((item) => this.processSingleItem(item, result)),
      map(() => result)
    );
  }

  /**
   * Process a single queue item
   */
  private processSingleItem(
    item: SyncQueueItem,
    result: SyncResult
  ): Observable<void> {
    this.syncProgress.next({
      ...this.syncProgress.value,
      currentItem: item.id,
    });

    return this.syncItem(item).pipe(
      tap((success) => {
        if (success) {
          result.syncedCount++;
          this.removeFromQueue(item.id).pipe(take(1)).subscribe();
        } else {
          result.failedCount++;
          result.success = false;
        }

        this.syncProgress.next({
          ...this.syncProgress.value,
          completed: result.syncedCount,
          failed: result.failedCount,
        });
      }),
      catchError((error) => {
        result.failedCount++;
        result.success = false;
        result.errors.push({ id: item.id, error: error.message });

        // Update retry count
        this.handleSyncError(item, error);

        this.syncProgress.next({
          ...this.syncProgress.value,
          completed: result.syncedCount,
          failed: result.failedCount,
        });

        return of(void 0);
      }),
      map(() => void 0)
    );
  }

  /**
   * Sync a single item to the server
   */
  private syncItem(item: SyncQueueItem): Observable<boolean> {
    const endpoint = this.endpoints[item.storeName];
    if (!endpoint) {
      return of(false);
    }

    switch (item.action) {
      case 'create':
        return this.http.post(endpoint, item.data).pipe(
          map(() => true),
          catchError((error) => this.handleHttpError(error, item))
        );

      case 'update':
        const updateData = item.data as { id: string };
        return this.http.put(`${endpoint}/${updateData.id}`, item.data).pipe(
          map(() => true),
          catchError((error) => this.handleHttpError(error, item))
        );

      case 'delete':
        const deleteData = item.data as { id: string };
        return this.http.delete(`${endpoint}/${deleteData.id}`).pipe(
          map(() => true),
          catchError((error) => this.handleHttpError(error, item))
        );

      default:
        return of(false);
    }
  }

  /**
   * Handle HTTP errors during sync
   */
  private handleHttpError(
    error: HttpErrorResponse,
    item: SyncQueueItem
  ): Observable<boolean> {
    // 409 Conflict - need conflict resolution
    if (error.status === 409) {
      return this.resolveConflict(item, error.error);
    }

    // 400/422 - validation error, remove from queue (won't succeed on retry)
    if (error.status === 400 || error.status === 422) {
      console.warn(`Validation error for sync item ${item.id}, removing from queue`);
      return of(true); // Return true to remove from queue
    }

    // Other errors - throw to trigger retry logic
    throw error;
  }

  /**
   * Resolve sync conflict
   */
  private resolveConflict(
    item: SyncQueueItem,
    serverData: unknown
  ): Observable<boolean> {
    switch (this.conflictResolution.strategy) {
      case 'client-wins':
        // Force update with client data
        const clientData = item.data as { id: string };
        return this.http
          .put(`${this.endpoints[item.storeName]}/${clientData.id}?force=true`, item.data)
          .pipe(
            map(() => true),
            catchError(() => of(false))
          );

      case 'server-wins':
        // Discard client changes
        return of(true);

      case 'merge':
        if (this.conflictResolution.resolver) {
          const mergedData = this.conflictResolution.resolver(item.data, serverData);
          const mergeData = mergedData as { id: string };
          return this.http
            .put(`${this.endpoints[item.storeName]}/${mergeData.id}`, mergedData)
            .pipe(
              map(() => true),
              catchError(() => of(false))
            );
        }
        return of(false);

      case 'manual':
      default:
        // Keep in queue for manual resolution
        return of(false);
    }
  }

  /**
   * Handle sync error by updating retry count
   */
  private handleSyncError(item: SyncQueueItem, error: unknown): void {
    item.retryCount++;
    item.lastError = error instanceof Error ? error.message : String(error);

    if (item.retryCount >= MAX_RETRY_COUNT) {
      console.error(`Max retries reached for sync item ${item.id}`, item);
      // Move to failed items or notify user
    } else {
      // Update item in queue with new retry count
      this.storage.put(STORES.SYNC_QUEUE, item).pipe(take(1)).subscribe();
    }
  }

  /**
   * Initialize automatic sync when coming back online
   */
  private initializeAutoSync(): void {
    // Auto-sync when coming back online
    this.networkStatus.isOnline$
      .pipe(
        filter((isOnline) => isOnline),
        switchMap(() => timer(1000)), // Wait 1 second after coming online
        filter(() => !this.syncInProgress.value),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        console.log('Network restored - starting auto-sync');
        this.sync().pipe(take(1)).subscribe((result) => {
          console.log('Auto-sync completed:', result);
        });
      });
  }

  /**
   * Update the pending items count
   */
  private updatePendingCount(): void {
    this.storage
      .count(STORES.SYNC_QUEUE)
      .pipe(take(1))
      .subscribe((count) => this.pendingCount.next(count));
  }
}
