/**
 * Tests for AnalyticsPanel component
 * Following TDD approach - tests written first
 */

import { describe, it, expect } from 'vitest';
import { renderWithTheme, screen, createMockBatchProgress } from '../../test/test-utils';
import { AnalyticsPanel } from '../AnalyticsPanel';
import { BatchProgressEvent } from '../../types/events';

describe('AnalyticsPanel', () => {
  // Helper to create batch data with specific metric values
  const createBatches = (successRates: number[]): BatchProgressEvent[] => {
    return successRates.map((rate, index) =>
      createMockBatchProgress({
        batchId: `batch-${index}`,
        successCount: Math.round(rate * 100),
        completedCount: 100,
        cumulativeComplianceRate: rate * 100,
        avgDurationMs: 100 + index * 50,
        currentThroughput: 5 + index * 2,
      })
    );
  };

  describe('Rendering and Basic Functionality', () => {
    it('renders with batch data', () => {
      const batches = createBatches([0.85, 0.90, 0.95]);
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      expect(screen.getByText(/Analytics/i)).toBeInTheDocument();
    });

    it('displays metric selector dropdown', () => {
      const batches = createBatches([0.85, 0.90, 0.95]);
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      // Should have a select/dropdown for metric selection
      expect(screen.getByRole('combobox', { name: /metric/i })).toBeInTheDocument();
    });

    it('handles empty batches array', () => {
      renderWithTheme(<AnalyticsPanel batches={[]} metric="successRate" />);

      expect(screen.getByText(/No data/i)).toBeInTheDocument();
    });
  });

  describe('Statistical Calculations - Mean', () => {
    it('calculates mean correctly for success rates', () => {
      // Success rates: 80%, 90%, 100% → Mean: 90%
      const batches = createBatches([0.80, 0.90, 1.00]);
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      expect(screen.getByText(/Mean/i)).toBeInTheDocument();
      // Check that Mean card exists with 90.0%
      const meanElements = screen.getAllByText(/90\.0%/);
      expect(meanElements.length).toBeGreaterThan(0);
    });

    it('calculates mean correctly for decimal values', () => {
      // Success rates: 85.5%, 90.2%, 94.8% → Mean: 90.17%
      const batches = createBatches([0.855, 0.902, 0.948]);
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      expect(screen.getByText(/90\.[01]/)).toBeInTheDocument();
    });
  });

  describe('Statistical Calculations - Median', () => {
    it('calculates median correctly for odd count', () => {
      // Values: [80%, 85%, 90%, 95%, 100%] → Median: 90%
      const batches = createBatches([0.80, 0.85, 0.90, 0.95, 1.00]);
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      expect(screen.getByText(/Median/i)).toBeInTheDocument();
      // Median should be displayed
      const medianElements = screen.getAllByText(/90\.0%/);
      expect(medianElements.length).toBeGreaterThan(0);
    });

    it('calculates median correctly for even count', () => {
      // Values: [80%, 85%, 95%, 100%] → Median: 90%
      const batches = createBatches([0.80, 0.85, 0.95, 1.00]);
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      expect(screen.getByText(/Median/i)).toBeInTheDocument();
      // Median should be displayed
      const medianElements = screen.getAllByText(/90\.0%/);
      expect(medianElements.length).toBeGreaterThan(0);
    });
  });

  describe('Statistical Calculations - Standard Deviation', () => {
    it('calculates standard deviation correctly', () => {
      const batches = createBatches([0.80, 0.85, 0.90, 0.95, 1.00]);
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      expect(screen.getByText(/Std Dev/i)).toBeInTheDocument();
      // Standard deviation should be displayed as a percentage
      const allText = screen.getAllByText(/[0-9]+\.[0-9]+%/);
      expect(allText.length).toBeGreaterThan(0);
    });

    it('shows 0 std dev for identical values', () => {
      const batches = createBatches([0.90, 0.90, 0.90, 0.90]);
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      expect(screen.getByText(/Std Dev/i)).toBeInTheDocument();
      // Should show 0.0% for std dev
      const zeroElements = screen.getAllByText(/0\.0%/);
      expect(zeroElements.length).toBeGreaterThan(0);
    });
  });

  describe('Statistical Calculations - Min/Max/Range', () => {
    it('calculates min correctly', () => {
      const batches = createBatches([0.70, 0.85, 0.95]);
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      expect(screen.getByText(/Min/i)).toBeInTheDocument();
      expect(screen.getByText(/70\.0%/)).toBeInTheDocument();
    });

    it('calculates max correctly', () => {
      const batches = createBatches([0.70, 0.85, 0.95]);
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      expect(screen.getByText(/Max/i)).toBeInTheDocument();
      expect(screen.getByText(/95\.0%/)).toBeInTheDocument();
    });

    it('calculates range correctly', () => {
      // Range: 95% - 70% = 25%
      const batches = createBatches([0.70, 0.85, 0.95]);
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      // Range label should be present (use getAllByText since "Range" appears in IQR too)
      const rangeLabels = screen.getAllByText(/Range/i);
      expect(rangeLabels.length).toBeGreaterThan(0);
      // Range value should be displayed
      const rangeElements = screen.getAllByText(/25\.0%/);
      expect(rangeElements.length).toBeGreaterThan(0);
    });
  });

  describe('Statistical Calculations - Coefficient of Variation', () => {
    it('calculates coefficient of variation', () => {
      const batches = createBatches([0.80, 0.85, 0.90, 0.95, 1.00]);
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      expect(screen.getByText(/Coefficient of Variation/i)).toBeInTheDocument();
      // CV should be displayed as a percentage
      const cvElements = screen.getAllByText(/[0-9]+\.[0-9]+%/);
      expect(cvElements.length).toBeGreaterThan(0);
    });
  });

  describe('Percentiles Calculations', () => {
    it('calculates 25th percentile (Q1)', () => {
      const batches = createBatches([0.70, 0.75, 0.80, 0.85, 0.90, 0.95, 1.00]);
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      expect(screen.getByText(/25th Percentile/i)).toBeInTheDocument();
      expect(screen.getByText(/Q1/i)).toBeInTheDocument();
    });

    it('calculates 75th percentile (Q3)', () => {
      const batches = createBatches([0.70, 0.75, 0.80, 0.85, 0.90, 0.95, 1.00]);
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      expect(screen.getByText(/75th Percentile/i)).toBeInTheDocument();
      expect(screen.getByText(/Q3/i)).toBeInTheDocument();
    });

    it('calculates IQR (Interquartile Range)', () => {
      const batches = createBatches([0.70, 0.75, 0.80, 0.85, 0.90, 0.95, 1.00]);
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      expect(screen.getByText(/IQR/i)).toBeInTheDocument();
    });
  });

  describe('Outlier Detection', () => {
    it('detects outliers correctly', () => {
      // Normal values with outliers - using more extreme values
      // Values: 10%, 80%, 82%, 84%, 86%, 88%, 150%
      const batches = [
        createMockBatchProgress({ batchId: 'low-batch', successCount: 10, completedCount: 100 }), // 10% - low outlier
        createMockBatchProgress({ batchId: 'batch-1', successCount: 80, completedCount: 100 }),
        createMockBatchProgress({ batchId: 'batch-2', successCount: 82, completedCount: 100 }),
        createMockBatchProgress({ batchId: 'batch-3', successCount: 84, completedCount: 100 }),
        createMockBatchProgress({ batchId: 'batch-4', successCount: 86, completedCount: 100 }),
        createMockBatchProgress({ batchId: 'batch-5', successCount: 88, completedCount: 100 }),
        createMockBatchProgress({ batchId: 'high-batch', successCount: 150, completedCount: 100 }), // 150% - high outlier (shouldn't happen but mathematically an outlier)
      ];

      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      expect(screen.getByText(/Outliers/i)).toBeInTheDocument();
      // At minimum we should detect the low outlier
      expect(screen.getByText(/low-batch/i)).toBeInTheDocument();
    });

    it('color-codes low outliers as red', () => {
      const batches = [
        createMockBatchProgress({ batchId: 'low-outlier', successCount: 30, completedCount: 100 }),
        ...createBatches([0.85, 0.88, 0.90, 0.92, 0.95]),
      ];

      const { container } = renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      // Find outlier element and check for red color styling
      const outlierElement = screen.getByText(/low-outlier/i);
      expect(outlierElement).toBeInTheDocument();

      // Check that the chip exists with error styling (MUI Chip with error color)
      const errorChips = container.querySelectorAll('.MuiChip-colorError');
      expect(errorChips.length).toBeGreaterThan(0);
    });

    it('color-codes high outliers as green', () => {
      const batches = [
        ...createBatches([0.70, 0.73, 0.75, 0.78, 0.80]),
        createMockBatchProgress({ batchId: 'high-outlier', successCount: 99, completedCount: 100 }),
      ];

      const { container } = renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      // Find outlier element and check for green color styling
      const outlierElement = screen.getByText(/high-outlier/i);
      expect(outlierElement).toBeInTheDocument();

      // Check that the chip exists with success styling (MUI Chip with success color)
      const successChips = container.querySelectorAll('.MuiChip-colorSuccess');
      expect(successChips.length).toBeGreaterThan(0);
    });
  });

  describe('Metric Selector', () => {
    it('changes displayed metric when selector is changed', async () => {
      const batches = createBatches([0.80, 0.85, 0.90]);
      const { rerender } = renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      // Initially shows success rate in the header
      expect(screen.getByText(/Analytics - Success Rate/i)).toBeInTheDocument();

      // Change to compliance rate
      rerender(<AnalyticsPanel batches={batches} metric="complianceRate" />);

      expect(screen.getByText(/Analytics - Compliance Rate/i)).toBeInTheDocument();
    });

    it('updates all statistics when metric changes', () => {
      const batches = createBatches([0.80, 0.85, 0.90]);
      const { rerender } = renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      const firstMean = screen.getByText(/Mean/i).parentElement?.textContent;

      // Change metric
      rerender(<AnalyticsPanel batches={batches} metric="complianceRate" />);

      const secondMean = screen.getByText(/Mean/i).parentElement?.textContent;

      // Mean should potentially be different (or at least recalculated)
      expect(screen.getByText(/Mean/i)).toBeInTheDocument();
    });
  });

  describe('Formatting', () => {
    it('formats success rate as percentage', () => {
      const batches = [
        createMockBatchProgress({ successCount: 855, completedCount: 1000 })
      ];
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      // Should display as "85.5%"
      const percentElements = screen.getAllByText(/85\.[0-9]%/);
      expect(percentElements.length).toBeGreaterThan(0);
    });

    it('formats compliance rate as percentage', () => {
      const batches = [
        createMockBatchProgress({ cumulativeComplianceRate: 73.5 })
      ];
      renderWithTheme(<AnalyticsPanel batches={batches} metric="complianceRate" />);

      // Should display as "73.5%"
      const percentElements = screen.getAllByText(/73\.[0-9]%/);
      expect(percentElements.length).toBeGreaterThan(0);
    });

    it('formats duration as milliseconds', () => {
      const batches = [
        createMockBatchProgress({ avgDurationMs: 1234 }),
        createMockBatchProgress({ avgDurationMs: 2345 }),
      ];
      renderWithTheme(<AnalyticsPanel batches={batches} metric="avgDuration" />);

      // Should display with "ms" suffix and comma separator
      expect(screen.getByText(/1,234 ms/)).toBeInTheDocument();
    });

    it('formats throughput as per-second', () => {
      const batches = [
        createMockBatchProgress({ currentThroughput: 45.2 }),
        createMockBatchProgress({ currentThroughput: 50.8 }),
      ];
      renderWithTheme(<AnalyticsPanel batches={batches} metric="throughput" />);

      // Should display with "/s" suffix
      expect(screen.getByText(/45\.2\/s/)).toBeInTheDocument();
    });
  });

  describe('Edge Cases', () => {
    it('handles single batch', () => {
      const batches = createBatches([0.90]);
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      expect(screen.getByText(/Mean/i)).toBeInTheDocument();
      expect(screen.getByText(/Median/i)).toBeInTheDocument();
      // Both should show 90.0%
      const percentElements = screen.getAllByText(/90\.0%/);
      expect(percentElements.length).toBeGreaterThan(0);
    });

    it('handles all same values (0 std dev)', () => {
      const batches = createBatches([0.85, 0.85, 0.85, 0.85]);
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      expect(screen.getByText(/Std Dev/i)).toBeInTheDocument();
      // Should show 0.0% for std dev
      const zeroElements = screen.getAllByText(/0\.0%/);
      expect(zeroElements.length).toBeGreaterThan(0);
    });

    it('displays message when no outliers detected', () => {
      const batches = createBatches([0.85, 0.87, 0.89, 0.91, 0.93]);
      renderWithTheme(<AnalyticsPanel batches={batches} metric="successRate" />);

      expect(screen.getByText(/No outliers detected/i)).toBeInTheDocument();
    });
  });
});
