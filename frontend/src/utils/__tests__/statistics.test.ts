/**
 * Tests for statistics utility functions
 * Following TDD approach - tests written first
 */

import { describe, it, expect } from 'vitest';
import {
  calculateMean,
  calculateMedian,
  calculateStdDev,
  calculatePercentile,
  detectOutliers,
} from '../statistics';

describe('statistics utilities', () => {
  describe('calculateMean', () => {
    it('calculates mean of simple values', () => {
      expect(calculateMean([1, 2, 3, 4, 5])).toBe(3);
    });

    it('calculates mean of decimal values', () => {
      expect(calculateMean([10.5, 20.5, 30.5])).toBeCloseTo(20.5, 1);
    });

    it('handles single value', () => {
      expect(calculateMean([42])).toBe(42);
    });

    it('handles negative values', () => {
      expect(calculateMean([-5, -10, -15])).toBe(-10);
    });
  });

  describe('calculateMedian', () => {
    it('calculates median for odd count', () => {
      expect(calculateMedian([1, 2, 3, 4, 5])).toBe(3);
    });

    it('calculates median for even count', () => {
      expect(calculateMedian([1, 2, 3, 4])).toBe(2.5);
    });

    it('calculates median for unsorted array', () => {
      expect(calculateMedian([5, 1, 3, 2, 4])).toBe(3);
    });

    it('handles single value', () => {
      expect(calculateMedian([42])).toBe(42);
    });

    it('handles two values', () => {
      expect(calculateMedian([10, 20])).toBe(15);
    });
  });

  describe('calculateStdDev', () => {
    it('calculates standard deviation correctly', () => {
      // Values: [2, 4, 6, 8, 10]
      // Mean: 6
      // Variance: [(2-6)^2 + (4-6)^2 + (6-6)^2 + (8-6)^2 + (10-6)^2] / 5
      //         = [16 + 4 + 0 + 4 + 16] / 5 = 40 / 5 = 8
      // StdDev: sqrt(8) ≈ 2.828
      expect(calculateStdDev([2, 4, 6, 8, 10])).toBeCloseTo(2.828, 2);
    });

    it('returns 0 for identical values', () => {
      expect(calculateStdDev([5, 5, 5, 5])).toBe(0);
    });

    it('handles single value (returns 0)', () => {
      expect(calculateStdDev([42])).toBe(0);
    });

    it('calculates std dev for decimal values', () => {
      expect(calculateStdDev([1.5, 2.5, 3.5])).toBeCloseTo(0.816, 2);
    });
  });

  describe('calculatePercentile', () => {
    it('calculates 25th percentile (Q1)', () => {
      const values = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
      // Linear interpolation: 25% of 9 (n-1) = 2.25, so between index 2 and 3
      // Result: 3 * 0.75 + 4 * 0.25 = 3.25
      expect(calculatePercentile(values, 25)).toBeCloseTo(3.25, 1);
    });

    it('calculates 50th percentile (median)', () => {
      const values = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
      expect(calculatePercentile(values, 50)).toBe(5.5);
    });

    it('calculates 75th percentile (Q3)', () => {
      const values = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
      // Linear interpolation: 75% of 9 (n-1) = 6.75, so between index 6 and 7
      // Result: 7 * 0.25 + 8 * 0.75 = 7.75
      expect(calculatePercentile(values, 75)).toBeCloseTo(7.75, 1);
    });

    it('handles unsorted array', () => {
      const values = [10, 5, 8, 2, 7, 1, 9, 3, 6, 4];
      expect(calculatePercentile(values, 50)).toBe(5.5);
    });

    it('handles edge case: 0th percentile', () => {
      expect(calculatePercentile([1, 2, 3, 4, 5], 0)).toBe(1);
    });

    it('handles edge case: 100th percentile', () => {
      expect(calculatePercentile([1, 2, 3, 4, 5], 100)).toBe(5);
    });
  });

  describe('detectOutliers', () => {
    it('detects no outliers in normal distribution', () => {
      const values = [10, 12, 14, 16, 18, 20, 22];
      const outliers = detectOutliers(values);
      expect(outliers).toHaveLength(0);
    });

    it('detects low outliers', () => {
      // Values: [1, 50, 52, 54, 56, 58, 60, 62]
      // Q1 ≈ 51, Q3 ≈ 59, IQR ≈ 8, Lower fence ≈ 51 - 1.5*8 = 39
      // Value 1 is below 39
      const values = [1, 50, 52, 54, 56, 58, 60, 62];
      const outliers = detectOutliers(values);

      expect(outliers.length).toBeGreaterThan(0);
      expect(outliers[0].type).toBe('low');
      expect(outliers[0].value).toBe(1);
    });

    it('detects high outliers', () => {
      // Q1 = 12, Q3 = 20, IQR = 8, Upper fence = 20 + 1.5*8 = 32
      // Value 50 is above 32
      const values = [10, 12, 14, 16, 18, 20, 22, 50];
      const outliers = detectOutliers(values);

      expect(outliers.length).toBeGreaterThan(0);
      const highOutlier = outliers.find(o => o.type === 'high');
      expect(highOutlier).toBeDefined();
      expect(highOutlier?.value).toBe(50);
    });

    it('detects both low and high outliers', () => {
      // Values: [1, 50, 52, 54, 56, 58, 60, 62, 100]
      // This should have both low (1) and high (100) outliers
      const values = [1, 50, 52, 54, 56, 58, 60, 62, 100];
      const outliers = detectOutliers(values);

      expect(outliers.length).toBeGreaterThanOrEqual(2);
      expect(outliers.some(o => o.type === 'low')).toBe(true);
      expect(outliers.some(o => o.type === 'high')).toBe(true);
    });

    it('returns correct indices for outliers', () => {
      const values = [1, 50, 52, 54, 56, 58, 60, 62, 100];
      const outliers = detectOutliers(values);

      // Index 0 should be low outlier (value 1)
      const lowOutlier = outliers.find(o => o.value === 1);
      expect(lowOutlier?.index).toBe(0);

      // Index 8 should be high outlier (value 100)
      const highOutlier = outliers.find(o => o.value === 100);
      expect(highOutlier?.index).toBe(8);
    });

    it('handles single value (no outliers)', () => {
      expect(detectOutliers([42])).toHaveLength(0);
    });

    it('handles identical values (no outliers)', () => {
      expect(detectOutliers([5, 5, 5, 5, 5])).toHaveLength(0);
    });
  });
});
