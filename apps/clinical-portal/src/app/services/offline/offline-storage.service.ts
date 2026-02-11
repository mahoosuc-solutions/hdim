/**
 * Issue #24: Offline Mode & Sync
 * IndexedDB-based offline storage service for the Clinical Portal
 *
 * Provides persistent local storage for:
 * - Patient data cache
 * - Care gap information
 * - Pending actions queue
 * - User preferences
 */
import { Injectable } from '@angular/core';
import { LoggerService } from '../logger.service';
import { BehaviorSubject, Observable, from, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

// Database configuration
const DB_NAME = 'hdim-clinical-portal';
const DB_VERSION = 1;

// Store names
export const STORES = {
  PATIENTS: 'patients',
  CARE_GAPS: 'careGaps',
  MEASURES: 'measures',
  EVALUATIONS: 'evaluations',
  SYNC_QUEUE: 'syncQueue',
  METADATA: 'metadata',
} as const;

export type StoreName = (typeof STORES)[keyof typeof STORES];

export interface SyncQueueItem {
  id: string;
  action: 'create' | 'update' | 'delete';
  storeName: StoreName;
  data: unknown;
  timestamp: number;
  retryCount: number;
  lastError?: string;
}

export interface OfflineMetadata {
  key: string;
  value: unknown;
  updatedAt: number;
}

@Injectable({
  providedIn: 'root',
})
export class OfflineStorageService {
  private db: IDBDatabase | null = null;
  private dbReady = new BehaviorSubject<boolean>(false);

  readonly isReady$ = this.dbReady.asObservable();

  constructor(
    private logger: LoggerService
  ) {
    this.initDatabase();
  }

  /**
   * Initialize the IndexedDB database
   */
  private initDatabase(): void {
    if (!('indexedDB' in window)) {
      this.logger.warn('IndexedDB not supported - offline mode disabled');
      return;
    }

    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onerror = (event) => {
      this.logger.error('IndexedDB error:', event);
      this.dbReady.next(false);
    };

    request.onsuccess = (event) => {
      this.db = (event.target as IDBOpenDBRequest).result;
      this.dbReady.next(true);
      this.logger.info('IndexedDB initialized successfully');
    };

    request.onupgradeneeded = (event) => {
      const db = (event.target as IDBOpenDBRequest).result;
      this.createStores(db);
    };
  }

  /**
   * Create object stores for the database
   */
  private createStores(db: IDBDatabase): void {
    // Patients store
    if (!db.objectStoreNames.contains(STORES.PATIENTS)) {
      const patientsStore = db.createObjectStore(STORES.PATIENTS, { keyPath: 'id' });
      patientsStore.createIndex('mrn', 'mrn', { unique: true });
      patientsStore.createIndex('name', 'name', { unique: false });
      patientsStore.createIndex('updatedAt', 'updatedAt', { unique: false });
    }

    // Care gaps store
    if (!db.objectStoreNames.contains(STORES.CARE_GAPS)) {
      const careGapsStore = db.createObjectStore(STORES.CARE_GAPS, { keyPath: 'id' });
      careGapsStore.createIndex('patientId', 'patientId', { unique: false });
      careGapsStore.createIndex('status', 'status', { unique: false });
      careGapsStore.createIndex('priority', 'priority', { unique: false });
    }

    // Measures store
    if (!db.objectStoreNames.contains(STORES.MEASURES)) {
      const measuresStore = db.createObjectStore(STORES.MEASURES, { keyPath: 'id' });
      measuresStore.createIndex('name', 'name', { unique: false });
      measuresStore.createIndex('category', 'category', { unique: false });
    }

    // Evaluations store
    if (!db.objectStoreNames.contains(STORES.EVALUATIONS)) {
      const evaluationsStore = db.createObjectStore(STORES.EVALUATIONS, { keyPath: 'id' });
      evaluationsStore.createIndex('measureId', 'measureId', { unique: false });
      evaluationsStore.createIndex('patientId', 'patientId', { unique: false });
      evaluationsStore.createIndex('timestamp', 'timestamp', { unique: false });
    }

    // Sync queue store
    if (!db.objectStoreNames.contains(STORES.SYNC_QUEUE)) {
      const syncStore = db.createObjectStore(STORES.SYNC_QUEUE, { keyPath: 'id' });
      syncStore.createIndex('timestamp', 'timestamp', { unique: false });
      syncStore.createIndex('action', 'action', { unique: false });
      syncStore.createIndex('storeName', 'storeName', { unique: false });
    }

    // Metadata store
    if (!db.objectStoreNames.contains(STORES.METADATA)) {
      db.createObjectStore(STORES.METADATA, { keyPath: 'key' });
    }
  }

  /**
   * Get an item from a store by key
   */
  get<T>(storeName: StoreName, key: string): Observable<T | undefined> {
    if (!this.db) {
      return of(undefined);
    }

    return from(
      new Promise<T | undefined>((resolve, reject) => {
        const transaction = this.db!.transaction(storeName, 'readonly');
        const store = transaction.objectStore(storeName);
        const request = store.get(key);

        request.onsuccess = () => resolve(request.result as T | undefined);
        request.onerror = () => reject(request.error);
      })
    ).pipe(
      catchError((error) => {
        this.logger.error(`Error getting item from ${storeName}:`, { error });
        return of(undefined);
      })
    );
  }

  /**
   * Get all items from a store
   */
  getAll<T>(storeName: StoreName): Observable<T[]> {
    if (!this.db) {
      return of([]);
    }

    return from(
      new Promise<T[]>((resolve, reject) => {
        const transaction = this.db!.transaction(storeName, 'readonly');
        const store = transaction.objectStore(storeName);
        const request = store.getAll();

        request.onsuccess = () => resolve(request.result as T[]);
        request.onerror = () => reject(request.error);
      })
    ).pipe(
      catchError((error) => {
        this.logger.error(`Error getting all items from ${storeName}:`, { error });
        return of([]);
      })
    );
  }

  /**
   * Get items by index
   */
  getByIndex<T>(
    storeName: StoreName,
    indexName: string,
    value: IDBValidKey
  ): Observable<T[]> {
    if (!this.db) {
      return of([]);
    }

    return from(
      new Promise<T[]>((resolve, reject) => {
        const transaction = this.db!.transaction(storeName, 'readonly');
        const store = transaction.objectStore(storeName);
        const index = store.index(indexName);
        const request = index.getAll(value);

        request.onsuccess = () => resolve(request.result as T[]);
        request.onerror = () => reject(request.error);
      })
    ).pipe(
      catchError((error) => {
        this.logger.error(`Error getting items by index from ${storeName}:`, { error });
        return of([]);
      })
    );
  }

  /**
   * Put (add or update) an item in a store
   */
  put<T>(storeName: StoreName, item: T): Observable<boolean> {
    if (!this.db) {
      return of(false);
    }

    return from(
      new Promise<boolean>((resolve, reject) => {
        const transaction = this.db!.transaction(storeName, 'readwrite');
        const store = transaction.objectStore(storeName);
        const request = store.put(item);

        request.onsuccess = () => resolve(true);
        request.onerror = () => reject(request.error);
      })
    ).pipe(
      catchError((error) => {
        this.logger.error(`Error putting item in ${storeName}:`, { error });
        return of(false);
      })
    );
  }

  /**
   * Put multiple items in a store (bulk operation)
   */
  putMany<T>(storeName: StoreName, items: T[]): Observable<boolean> {
    if (!this.db || items.length === 0) {
      return of(items.length === 0);
    }

    return from(
      new Promise<boolean>((resolve, reject) => {
        const transaction = this.db!.transaction(storeName, 'readwrite');
        const store = transaction.objectStore(storeName);

        let completed = 0;
        let hasError = false;

        items.forEach((item) => {
          const request = store.put(item);
          request.onsuccess = () => {
            completed++;
            if (completed === items.length && !hasError) {
              resolve(true);
            }
          };
          request.onerror = () => {
            hasError = true;
            reject(request.error);
          };
        });
      })
    ).pipe(
      catchError((error) => {
        this.logger.error(`Error putting items in ${storeName}:`, { error });
        return of(false);
      })
    );
  }

  /**
   * Delete an item from a store
   */
  delete(storeName: StoreName, key: string): Observable<boolean> {
    if (!this.db) {
      return of(false);
    }

    return from(
      new Promise<boolean>((resolve, reject) => {
        const transaction = this.db!.transaction(storeName, 'readwrite');
        const store = transaction.objectStore(storeName);
        const request = store.delete(key);

        request.onsuccess = () => resolve(true);
        request.onerror = () => reject(request.error);
      })
    ).pipe(
      catchError((error) => {
        this.logger.error(`Error deleting item from ${storeName}:`, { error });
        return of(false);
      })
    );
  }

  /**
   * Clear all items from a store
   */
  clear(storeName: StoreName): Observable<boolean> {
    if (!this.db) {
      return of(false);
    }

    return from(
      new Promise<boolean>((resolve, reject) => {
        const transaction = this.db!.transaction(storeName, 'readwrite');
        const store = transaction.objectStore(storeName);
        const request = store.clear();

        request.onsuccess = () => resolve(true);
        request.onerror = () => reject(request.error);
      })
    ).pipe(
      catchError((error) => {
        this.logger.error(`Error clearing ${storeName}:`, { error });
        return of(false);
      })
    );
  }

  /**
   * Get the count of items in a store
   */
  count(storeName: StoreName): Observable<number> {
    if (!this.db) {
      return of(0);
    }

    return from(
      new Promise<number>((resolve, reject) => {
        const transaction = this.db!.transaction(storeName, 'readonly');
        const store = transaction.objectStore(storeName);
        const request = store.count();

        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
      })
    ).pipe(
      catchError((error) => {
        this.logger.error(`Error counting items in ${storeName}:`, { error });
        return of(0);
      })
    );
  }

  /**
   * Set metadata value
   */
  setMetadata(key: string, value: unknown): Observable<boolean> {
    const metadata: OfflineMetadata = {
      key,
      value,
      updatedAt: Date.now(),
    };
    return this.put(STORES.METADATA, metadata);
  }

  /**
   * Get metadata value
   */
  getMetadata<T>(key: string): Observable<T | undefined> {
    return this.get<OfflineMetadata>(STORES.METADATA, key).pipe(
      map((metadata) => metadata?.value as T | undefined)
    );
  }

  /**
   * Get last sync timestamp for a store
   */
  getLastSyncTime(storeName: StoreName): Observable<number | undefined> {
    return this.getMetadata<number>(`lastSync_${storeName}`);
  }

  /**
   * Set last sync timestamp for a store
   */
  setLastSyncTime(storeName: StoreName): Observable<boolean> {
    return this.setMetadata(`lastSync_${storeName}`, Date.now());
  }

  /**
   * Get total storage usage estimate
   */
  async getStorageEstimate(): Promise<{ usage: number; quota: number } | null> {
    if ('storage' in navigator && 'estimate' in navigator.storage) {
      const estimate = await navigator.storage.estimate();
      return {
        usage: estimate.usage || 0,
        quota: estimate.quota || 0,
      };
    }
    return null;
  }

  /**
   * Delete the entire database (for troubleshooting)
   */
  deleteDatabase(): Observable<boolean> {
    return from(
      new Promise<boolean>((resolve, reject) => {
        if (this.db) {
          this.db.close();
          this.db = null;
        }

        const request = indexedDB.deleteDatabase(DB_NAME);
        request.onsuccess = () => {
          this.dbReady.next(false);
          resolve(true);
        };
        request.onerror = () => reject(request.error);
      })
    ).pipe(
      tap(() => {
        // Reinitialize after deletion
        setTimeout(() => this.initDatabase(), 100);
      }),
      catchError((error) => {
        this.logger.error('Error deleting database:', { error });
        return of(false);
      })
    );
  }
}
