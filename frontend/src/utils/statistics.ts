/**
 * Statistical calculation utilities for analytics
 */

/**
 * Calculate the arithmetic mean (average) of an array of numbers
 */
export function calculateMean(values: number[]): number {
  if (values.length === 0) return 0;
  const sum = values.reduce((acc, val) => acc + val, 0);
  return sum / values.length;
}

/**
 * Calculate the median (50th percentile) of an array of numbers
 */
export function calculateMedian(values: number[]): number {
  if (values.length === 0) return 0;

  const sorted = [...values].sort((a, b) => a - b);
  const mid = Math.floor(sorted.length / 2);

  if (sorted.length % 2 === 0) {
    // Even count: average of two middle values
    return (sorted[mid - 1] + sorted[mid]) / 2;
  } else {
    // Odd count: middle value
    return sorted[mid];
  }
}

/**
 * Calculate the population standard deviation of an array of numbers
 */
export function calculateStdDev(values: number[]): number {
  if (values.length === 0) return 0;
  if (values.length === 1) return 0;

  const mean = calculateMean(values);
  const squaredDiffs = values.map(value => Math.pow(value - mean, 2));
  const variance = squaredDiffs.reduce((acc, val) => acc + val, 0) / values.length;

  return Math.sqrt(variance);
}

/**
 * Calculate a specific percentile of an array of numbers
 * Uses the linear interpolation method (R-7, Excel method)
 *
 * @param values - Array of numbers
 * @param percentile - Percentile to calculate (0-100)
 */
export function calculatePercentile(values: number[], percentile: number): number {
  if (values.length === 0) return 0;
  if (values.length === 1) return values[0];

  const sorted = [...values].sort((a, b) => a - b);

  // Handle edge cases
  if (percentile <= 0) return sorted[0];
  if (percentile >= 100) return sorted[sorted.length - 1];

  // Calculate position using linear interpolation
  const position = (percentile / 100) * (sorted.length - 1);
  const lower = Math.floor(position);
  const upper = Math.ceil(position);

  // If position is exact, return that value
  if (lower === upper) {
    return sorted[lower];
  }

  // Interpolate between lower and upper
  const weight = position - lower;
  return sorted[lower] * (1 - weight) + sorted[upper] * weight;
}

/**
 * Detect outliers using the IQR (Interquartile Range) method
 * Outliers are values that fall below Q1 - 1.5*IQR or above Q3 + 1.5*IQR
 */
export function detectOutliers(
  values: number[]
): Array<{ value: number; index: number; type: 'low' | 'high' }> {
  if (values.length < 4) return []; // Need at least 4 values for meaningful outlier detection

  const q1 = calculatePercentile(values, 25);
  const q3 = calculatePercentile(values, 75);
  const iqr = q3 - q1;

  // If IQR is 0, all values are the same or very similar - no outliers
  if (iqr === 0) return [];

  const lowerFence = q1 - 1.5 * iqr;
  const upperFence = q3 + 1.5 * iqr;

  const outliers: Array<{ value: number; index: number; type: 'low' | 'high' }> = [];

  values.forEach((value, index) => {
    if (value < lowerFence) {
      outliers.push({ value, index, type: 'low' });
    } else if (value > upperFence) {
      outliers.push({ value, index, type: 'high' });
    }
  });

  return outliers;
}
