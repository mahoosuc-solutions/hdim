import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import 'fake-indexeddb/auto';
import { IDBFactory } from 'fake-indexeddb';
import {
  initCache,
  setCache,
  getCache,
  deleteCache,
  clearCache,
  getCacheKeys,
  isCacheExpired,
  resetCacheState,
} from '../dataCache.service';

describe('dataCache.service', () => {
  const testConfig = {
    dbName: 'test-cache-db',
    version: 1,
    storeName: 'test-store',
    maxAge: 1000, // 1 second for testing
  };

  beforeEach(async () => {
    vi.useRealTimers();
    // Reset service state
    resetCacheState();

    // Reset IndexedDB before each test
    globalThis.indexedDB = new IDBFactory();
  });

  afterEach(async () => {
    // Clean up service state
    resetCacheState();

    // Clean up after each test
    try {
      const databases = await indexedDB.databases();
      for (const db of databases) {
        if (db.name) {
          indexedDB.deleteDatabase(db.name);
        }
      }
    } catch (error) {
      // Ignore errors during cleanup
    }
  });

  describe('initCache', () => {
    it('should create database successfully', async () => {
      await expect(initCache(testConfig)).resolves.not.toThrow();
    });

    it('should handle database upgrade', async () => {
      await initCache(testConfig);

      // Initialize again with higher version
      const upgradeConfig = { ...testConfig, version: 2 };
      await expect(initCache(upgradeConfig)).resolves.not.toThrow();
    });

    it('should throw error if dbName is empty', async () => {
      const invalidConfig = { ...testConfig, dbName: '' };
      await expect(initCache(invalidConfig)).rejects.toThrow();
    });

    it('should throw error if storeName is empty', async () => {
      const invalidConfig = { ...testConfig, storeName: '' };
      await expect(initCache(invalidConfig)).rejects.toThrow();
    });
  });

  describe('setCache', () => {
    beforeEach(async () => {
      await initCache(testConfig);
    });

    it('should store data with timestamp', async () => {
      const testData = { name: 'John', age: 30 };
      await setCache('user', testData);

      const result = await getCache<typeof testData>('user');
      expect(result).toEqual(testData);
    });

    it('should overwrite existing data', async () => {
      await setCache('counter', 1);
      await setCache('counter', 2);

      const result = await getCache<number>('counter');
      expect(result).toBe(2);
    });

    it('should handle complex data structures', async () => {
      const complexData = {
        users: [{ id: 1, name: 'Alice' }, { id: 2, name: 'Bob' }],
        metadata: { total: 2, page: 1 },
        nested: { deep: { value: 'test' } },
      };

      await setCache('complex', complexData);
      const result = await getCache<typeof complexData>('complex');
      expect(result).toEqual(complexData);
    });

    it('should handle null and undefined values', async () => {
      await setCache('null-value', null);
      await setCache('undefined-value', undefined);

      const nullResult = await getCache('null-value');
      const undefinedResult = await getCache('undefined-value');

      expect(nullResult).toBeNull();
      expect(undefinedResult).toBeUndefined();
    });
  });

  describe('getCache', () => {
    beforeEach(async () => {
      await initCache(testConfig);
    });

    it('should retrieve stored data', async () => {
      const testData = { value: 'test' };
      await setCache('key1', testData);

      const result = await getCache<typeof testData>('key1');
      expect(result).toEqual(testData);
    });

    it('should return null for non-existent key', async () => {
      const result = await getCache('non-existent');
      expect(result).toBeNull();
    });

    it('should return null for expired data', async () => {
      await setCache('expired-key', 'test-value');

      // Wait for cache to expire (maxAge is 1000ms)
      await new Promise(resolve => setTimeout(resolve, 1100));

      const result = await getCache('expired-key');
      expect(result).toBeNull();
    });

    it('should return data before expiration', async () => {
      await setCache('fresh-key', 'fresh-value');

      // Wait less than maxAge
      await new Promise(resolve => setTimeout(resolve, 500));

      const result = await getCache('fresh-key');
      expect(result).toBe('fresh-value');
    });

    it('should auto-cleanup expired entries on get', async () => {
      await setCache('cleanup-key', 'cleanup-value');

      // Wait for expiration
      await new Promise(resolve => setTimeout(resolve, 1100));

      // This should return null and remove the entry
      await getCache('cleanup-key');

      // Verify the key is removed
      const keys = await getCacheKeys();
      expect(keys).not.toContain('cleanup-key');
    });
  });

  describe('deleteCache', () => {
    beforeEach(async () => {
      await initCache(testConfig);
    });

    it('should remove specific key', async () => {
      await setCache('key1', 'value1');
      await setCache('key2', 'value2');

      await deleteCache('key1');

      const result1 = await getCache('key1');
      const result2 = await getCache('key2');

      expect(result1).toBeNull();
      expect(result2).toBe('value2');
    });

    it('should not throw error for non-existent key', async () => {
      await expect(deleteCache('non-existent')).resolves.not.toThrow();
    });

    it('should handle deletion of multiple keys sequentially', async () => {
      await setCache('key1', 'value1');
      await setCache('key2', 'value2');
      await setCache('key3', 'value3');

      await deleteCache('key1');
      await deleteCache('key2');
      await deleteCache('key3');

      const keys = await getCacheKeys();
      expect(keys).toHaveLength(0);
    });
  });

  describe('clearCache', () => {
    beforeEach(async () => {
      await initCache(testConfig);
    });

    it('should remove all data', async () => {
      await setCache('key1', 'value1');
      await setCache('key2', 'value2');
      await setCache('key3', 'value3');

      await clearCache();

      const keys = await getCacheKeys();
      expect(keys).toHaveLength(0);
    });

    it('should not throw error on empty cache', async () => {
      await expect(clearCache()).resolves.not.toThrow();
    });

    it('should allow adding data after clear', async () => {
      await setCache('key1', 'value1');
      await clearCache();
      await setCache('key2', 'value2');

      const result = await getCache('key2');
      expect(result).toBe('value2');
    });
  });

  describe('getCacheKeys', () => {
    beforeEach(async () => {
      await initCache(testConfig);
    });

    it('should return all stored keys', async () => {
      await setCache('key1', 'value1');
      await setCache('key2', 'value2');
      await setCache('key3', 'value3');

      const keys = await getCacheKeys();
      expect(keys).toHaveLength(3);
      expect(keys).toContain('key1');
      expect(keys).toContain('key2');
      expect(keys).toContain('key3');
    });

    it('should return empty array when cache is empty', async () => {
      const keys = await getCacheKeys();
      expect(keys).toEqual([]);
    });

    it('should return updated keys after deletion', async () => {
      await setCache('key1', 'value1');
      await setCache('key2', 'value2');

      let keys = await getCacheKeys();
      expect(keys).toHaveLength(2);

      await deleteCache('key1');

      keys = await getCacheKeys();
      expect(keys).toHaveLength(1);
      expect(keys).toContain('key2');
      expect(keys).not.toContain('key1');
    });
  });

  describe('isCacheExpired', () => {
    beforeEach(async () => {
      await initCache(testConfig);
    });

    it('should return false for fresh data', async () => {
      await setCache('fresh-key', 'fresh-value');

      const expired = await isCacheExpired('fresh-key');
      expect(expired).toBe(false);
    });

    it('should return true for expired data', async () => {
      await setCache('expired-key', 'expired-value');

      // Wait for expiration
      await new Promise(resolve => setTimeout(resolve, 1100));

      const expired = await isCacheExpired('expired-key');
      expect(expired).toBe(true);
    });

    it('should return true for non-existent key', async () => {
      const expired = await isCacheExpired('non-existent');
      expect(expired).toBe(true);
    });

    it('should handle boundary condition (exactly at maxAge)', async () => {
      await setCache('boundary-key', 'boundary-value');

      // Wait exactly maxAge
      await new Promise(resolve => setTimeout(resolve, 1000));

      const expired = await isCacheExpired('boundary-key');
      // At or after maxAge should be expired
      expect(expired).toBe(true);
    });
  });

  describe('Error Handling', () => {
    it('should handle IndexedDB not available gracefully', async () => {
      // Remove IndexedDB
      const originalIndexedDB = globalThis.indexedDB;
      // @ts-expect-error - Testing undefined scenario
      globalThis.indexedDB = undefined;

      await expect(initCache(testConfig)).rejects.toThrow();

      // Restore
      globalThis.indexedDB = originalIndexedDB;
    });

    it('should return null on getCache error', async () => {
      // Don't initialize cache first
      const result = await getCache('any-key');
      expect(result).toBeNull();
    });

    it('should handle concurrent operations', async () => {
      await initCache(testConfig);

      const operations = [
        setCache('key1', 'value1'),
        setCache('key2', 'value2'),
        setCache('key3', 'value3'),
        setCache('key4', 'value4'),
        setCache('key5', 'value5'),
      ];

      await Promise.all(operations);

      const keys = await getCacheKeys();
      expect(keys).toHaveLength(5);
    });

    it('should handle rapid successive operations on same key', async () => {
      await initCache(testConfig);

      await setCache('rapid', 1);
      await setCache('rapid', 2);
      await setCache('rapid', 3);
      await setCache('rapid', 4);
      await setCache('rapid', 5);

      const result = await getCache<number>('rapid');
      expect(result).toBe(5);
    });
  });

  describe('SSR Safety', () => {
    it('should be SSR-safe (no window errors)', async () => {
      // This test verifies that our service doesn't throw when window is undefined
      // fake-indexeddb provides a mock, but in real SSR, indexedDB would be undefined

      const originalIndexedDB = globalThis.indexedDB;

      try {
        // @ts-expect-error - Testing undefined scenario
        globalThis.indexedDB = undefined;

        await expect(initCache(testConfig)).rejects.toThrow();

        // These should handle gracefully
        const getResult = await getCache('any-key');
        expect(getResult).toBeNull();

      } finally {
        globalThis.indexedDB = originalIndexedDB;
      }
    });
  });

  describe('Multiple Stores Support', () => {
    it('should support multiple stores in same database', async () => {
      const config1 = {
        dbName: 'multi-store-db',
        version: 1,
        storeName: 'store1',
        maxAge: 5000,
      };

      const config2 = {
        dbName: 'multi-store-db',
        version: 1,
        storeName: 'store2',
        maxAge: 5000,
      };

      await initCache(config1);
      await initCache(config2);

      await setCache('key1', 'value1');
      await setCache('key2', 'value2');

      const result1 = await getCache('key1');
      const result2 = await getCache('key2');

      expect(result1).toBe('value1');
      expect(result2).toBe('value2');
    });
  });

  describe('Default maxAge', () => {
    it('should use default maxAge of 24 hours when not specified', async () => {
      const configNoMaxAge = {
        dbName: 'default-maxage-db',
        version: 1,
        storeName: 'default-store',
      };

      await initCache(configNoMaxAge);
      await setCache('key', 'value');

      // Should not be expired immediately
      const expired = await isCacheExpired('key');
      expect(expired).toBe(false);

      // Should still be retrievable
      const result = await getCache('key');
      expect(result).toBe('value');
    });
  });
});
