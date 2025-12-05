/**
 * DataCache Service
 *
 * Client-side caching with IndexedDB for offline support and performance.
 * Provides a simple key-value cache with automatic expiration.
 */

export interface CacheConfig {
  dbName: string;
  version: number;
  storeName: string;
  maxAge?: number; // Cache expiration in milliseconds (default: 24 hours)
}

export interface CachedData<T> {
  data: T;
  timestamp: number;
  key: string;
}

// Global state to track current configuration and database
// Support multiple databases and stores
const databases = new Map<string, IDBDatabase>();
const configs = new Map<string, CacheConfig>();
let currentDbKey: string | null = null;

const DEFAULT_MAX_AGE = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

/**
 * Get a unique key for the database + store combination
 */
function getDbKey(dbName: string, storeName: string): string {
  return `${dbName}::${storeName}`;
}

/**
 * Initialize the cache database
 */
export async function initCache(config: CacheConfig): Promise<void> {
  return new Promise((resolve, reject) => {
    // Validate configuration
    if (!config.dbName || config.dbName.trim() === '') {
      reject(new Error('Database name is required'));
      return;
    }

    if (!config.storeName || config.storeName.trim() === '') {
      reject(new Error('Store name is required'));
      return;
    }

    // Check if IndexedDB is available
    if (typeof indexedDB === 'undefined') {
      reject(new Error('IndexedDB is not available'));
      return;
    }

    const dbKey = getDbKey(config.dbName, config.storeName);

    // Store configuration
    const fullConfig: CacheConfig = {
      ...config,
      maxAge: config.maxAge ?? DEFAULT_MAX_AGE,
    };
    configs.set(dbKey, fullConfig);
    currentDbKey = dbKey;

    // Check if we already have a database connection for this dbName
    const existingDbKey = Array.from(databases.keys()).find(key =>
      key.startsWith(`${config.dbName}::`)
    );

    if (existingDbKey) {
      const existingDb = databases.get(existingDbKey);
      if (existingDb) {
        // Check if the store already exists
        if (existingDb.objectStoreNames.contains(config.storeName)) {
          // Store exists, reuse the database connection
          databases.set(dbKey, existingDb);
          currentDbKey = dbKey;
          resolve();
          return;
        } else {
          // Need to create a new store, close the existing connection
          existingDb.close();
          databases.delete(existingDbKey);
        }
      }
    }

    try {
      const request = indexedDB.open(config.dbName, config.version);

      request.onerror = () => {
        reject(new Error('Failed to open database'));
      };

      request.onsuccess = (event) => {
        const database = (event.target as IDBOpenDBRequest).result;

        // Check if the store exists
        if (!database.objectStoreNames.contains(config.storeName)) {
          // Store doesn't exist but we're not in upgrade, need to upgrade
          database.close();

          // Increment version and reopen
          const upgradeRequest = indexedDB.open(config.dbName, database.version + 1);

          upgradeRequest.onerror = () => {
            reject(new Error('Failed to open database'));
          };

          upgradeRequest.onsuccess = (e) => {
            const upgradedDb = (e.target as IDBOpenDBRequest).result;
            databases.set(dbKey, upgradedDb);
            // Update all database keys with same dbName to point to new connection
            for (const key of databases.keys()) {
              if (key.startsWith(`${config.dbName}::`)) {
                databases.set(key, upgradedDb);
              }
            }
            currentDbKey = dbKey;
            resolve();
          };

          upgradeRequest.onupgradeneeded = (e) => {
            const upgradedDb = (e.target as IDBOpenDBRequest).result;
            if (!upgradedDb.objectStoreNames.contains(config.storeName)) {
              upgradedDb.createObjectStore(config.storeName, { keyPath: 'key' });
            }
          };
        } else {
          // Store exists
          databases.set(dbKey, database);
          // Update all database keys with same dbName to point to same connection
          for (const key of databases.keys()) {
            if (key.startsWith(`${config.dbName}::`)) {
              databases.set(key, database);
            }
          }
          currentDbKey = dbKey;
          resolve();
        }
      };

      request.onupgradeneeded = (event) => {
        const database = (event.target as IDBOpenDBRequest).result;

        // Create object store if it doesn't exist
        if (!database.objectStoreNames.contains(config.storeName)) {
          database.createObjectStore(config.storeName, { keyPath: 'key' });
        }
      };
    } catch (error) {
      reject(error);
    }
  });
}

/**
 * Get the current database instance
 */
function getDB(): IDBDatabase {
  if (!currentDbKey || !databases.has(currentDbKey)) {
    throw new Error('Cache not initialized. Call initCache first.');
  }
  return databases.get(currentDbKey)!;
}

/**
 * Get the current store name
 */
function getStoreName(): string {
  if (!currentDbKey || !configs.has(currentDbKey)) {
    throw new Error('Cache not initialized. Call initCache first.');
  }
  return configs.get(currentDbKey)!.storeName;
}

/**
 * Get the current maxAge setting
 */
function getMaxAge(): number {
  if (!currentDbKey || !configs.has(currentDbKey)) {
    return DEFAULT_MAX_AGE;
  }
  return configs.get(currentDbKey)!.maxAge ?? DEFAULT_MAX_AGE;
}

/**
 * Store data in cache
 */
export async function setCache<T>(key: string, data: T): Promise<void> {
  return new Promise((resolve, reject) => {
    try {
      const database = getDB();
      const storeName = getStoreName();
      const transaction = database.transaction([storeName], 'readwrite');
      const store = transaction.objectStore(storeName);

      const cachedData: CachedData<T> = {
        key,
        data,
        timestamp: Date.now(),
      };

      const request = store.put(cachedData);

      request.onsuccess = () => {
        resolve();
      };

      request.onerror = () => {
        reject(new Error('Failed to set cache'));
      };
    } catch (error) {
      reject(error);
    }
  });
}

/**
 * Retrieve data from cache
 * Returns null if key doesn't exist or data is expired
 */
export async function getCache<T>(key: string): Promise<T | null> {
  try {
    const database = getDB();
    const storeName = getStoreName();
    const maxAge = getMaxAge();

    return new Promise((resolve) => {
      const transaction = database.transaction([storeName], 'readonly');
      const store = transaction.objectStore(storeName);
      const request = store.get(key);

      request.onsuccess = async () => {
        const result = request.result as CachedData<T> | undefined;

        if (!result) {
          resolve(null);
          return;
        }

        // Check if data is expired
        const age = Date.now() - result.timestamp;
        if (age >= maxAge) {
          // Auto-cleanup expired entry
          await deleteCache(key);
          resolve(null);
          return;
        }

        resolve(result.data);
      };

      request.onerror = () => {
        resolve(null);
      };
    });
  } catch (error) {
    // Return null on error (graceful degradation)
    return null;
  }
}

/**
 * Delete a specific key from cache
 */
export async function deleteCache(key: string): Promise<void> {
  return new Promise((resolve, reject) => {
    try {
      const database = getDB();
      const storeName = getStoreName();
      const transaction = database.transaction([storeName], 'readwrite');
      const store = transaction.objectStore(storeName);

      const request = store.delete(key);

      request.onsuccess = () => {
        resolve();
      };

      request.onerror = () => {
        reject(new Error('Failed to delete cache'));
      };
    } catch (error) {
      reject(error);
    }
  });
}

/**
 * Clear all data from cache
 */
export async function clearCache(): Promise<void> {
  return new Promise((resolve, reject) => {
    try {
      const database = getDB();
      const storeName = getStoreName();
      const transaction = database.transaction([storeName], 'readwrite');
      const store = transaction.objectStore(storeName);

      const request = store.clear();

      request.onsuccess = () => {
        resolve();
      };

      request.onerror = () => {
        reject(new Error('Failed to clear cache'));
      };
    } catch (error) {
      reject(error);
    }
  });
}

/**
 * Get all keys in cache
 */
export async function getCacheKeys(): Promise<string[]> {
  return new Promise((resolve, reject) => {
    try {
      const database = getDB();
      const storeName = getStoreName();
      const transaction = database.transaction([storeName], 'readonly');
      const store = transaction.objectStore(storeName);

      const request = store.getAllKeys();

      request.onsuccess = () => {
        resolve(request.result as string[]);
      };

      request.onerror = () => {
        reject(new Error('Failed to get cache keys'));
      };
    } catch (error) {
      reject(error);
    }
  });
}

/**
 * Check if cached data is expired
 * Returns true if expired or doesn't exist
 */
export async function isCacheExpired(key: string): Promise<boolean> {
  try {
    const database = getDB();
    const storeName = getStoreName();
    const maxAge = getMaxAge();

    return new Promise((resolve) => {
      const transaction = database.transaction([storeName], 'readonly');
      const store = transaction.objectStore(storeName);
      const request = store.get(key);

      request.onsuccess = () => {
        const result = request.result as CachedData<unknown> | undefined;

        if (!result) {
          resolve(true); // Non-existent data is considered expired
          return;
        }

        const age = Date.now() - result.timestamp;
        resolve(age >= maxAge);
      };

      request.onerror = () => {
        resolve(true); // Error is considered expired
      };
    });
  } catch (error) {
    return true; // Error is considered expired
  }
}

/**
 * Reset the cache service state (for testing purposes)
 * @internal
 */
export function resetCacheState(): void {
  // Close all database connections
  for (const db of databases.values()) {
    try {
      db.close();
    } catch (error) {
      // Ignore errors
    }
  }

  databases.clear();
  configs.clear();
  currentDbKey = null;
}
