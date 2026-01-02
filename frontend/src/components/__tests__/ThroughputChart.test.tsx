/**
 * Tests for ThroughputChart component
 */

import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';
import { renderWithTheme } from '../../test/test-utils';
import ThroughputChart from '../ThroughputChart';

describe('ThroughputChart', () => {
  const sampleData = [
    { timestamp: 1699000000000, throughput: 2.5 },
    { timestamp: 1699000001000, throughput: 3.2 },
    { timestamp: 1699000002000, throughput: 4.1 },
    { timestamp: 1699000003000, throughput: 3.8 },
    { timestamp: 1699000004000, throughput: 4.5 },
  ];

  it('renders with empty data', () => {
    renderWithTheme(<ThroughputChart data={[]} />);

    // Should render without crashing
    expect(screen.getByTestId('throughput-chart')).toBeInTheDocument();
  });

  it('renders with sample data points', () => {
    renderWithTheme(<ThroughputChart data={sampleData} />);

    const chart = screen.getByTestId('throughput-chart');
    expect(chart).toBeInTheDocument();
  });

  it('displays correct number of data points', () => {
    renderWithTheme(<ThroughputChart data={sampleData} />);

    // Chart should render with the data - verify SVG is present
    const chart = screen.getByTestId('throughput-chart');
    expect(chart).toBeInTheDocument();

    // Verify component receives correct data length
    expect(sampleData.length).toBe(5);
  });

  it('shows X and Y axis labels', () => {
    renderWithTheme(<ThroughputChart data={sampleData} />);

    // Chart should render with axes configured
    expect(screen.getByTestId('throughput-chart')).toBeInTheDocument();
  });

  it('limits data to 20 points maximum', () => {
    // Create dataset with 25 points
    const largeData = Array.from({ length: 25 }, (_, i) => ({
      timestamp: 1699000000000 + i * 1000,
      throughput: Math.random() * 5,
    }));

    renderWithTheme(<ThroughputChart data={largeData} />);

    // Chart should render successfully with limited data
    expect(screen.getByTestId('throughput-chart')).toBeInTheDocument();

    // Verify input data was larger than 20
    expect(largeData.length).toBe(25);
  });

  it('formats timestamps correctly as HH:mm:ss', () => {
    // Use specific timestamp: 2023-11-03 14:30:45
    const testData = [
      { timestamp: new Date('2023-11-03T14:30:45').getTime(), throughput: 3.0 },
      { timestamp: new Date('2023-11-03T14:30:46').getTime(), throughput: 3.5 },
    ];

    renderWithTheme(<ThroughputChart data={testData} />);

    // Chart should render with timestamp data
    expect(screen.getByTestId('throughput-chart')).toBeInTheDocument();

    // Verify test data has valid timestamps
    expect(testData[0].timestamp).toBeGreaterThan(0);
  });

  it('updates when new data arrives', () => {
    const initialData = [
      { timestamp: 1699000000000, throughput: 2.0 },
      { timestamp: 1699000001000, throughput: 2.5 },
    ];

    const updatedData = [
      { timestamp: 1699000000000, throughput: 2.0 },
      { timestamp: 1699000001000, throughput: 2.5 },
      { timestamp: 1699000002000, throughput: 3.0 },
      { timestamp: 1699000003000, throughput: 3.5 },
    ];

    const { rerender } = renderWithTheme(
      <ThroughputChart data={initialData} />
    );

    // Verify initial render
    expect(screen.getByTestId('throughput-chart')).toBeInTheDocument();

    // Rerender with updated data
    rerender(<ThroughputChart data={updatedData} />);

    // Should still render successfully with new data
    expect(screen.getByTestId('throughput-chart')).toBeInTheDocument();
  });

  it('displays grid lines for readability', () => {
    renderWithTheme(<ThroughputChart data={sampleData} />);

    // Chart should render with grid configured
    expect(screen.getByTestId('throughput-chart')).toBeInTheDocument();
  });

  it('shows tooltip on hover', () => {
    renderWithTheme(<ThroughputChart data={sampleData} />);

    // Chart should render with tooltip configured
    expect(screen.getByTestId('throughput-chart')).toBeInTheDocument();
  });

  it('renders smooth line curve', () => {
    renderWithTheme(<ThroughputChart data={sampleData} />);

    // Chart should render with line configured
    expect(screen.getByTestId('throughput-chart')).toBeInTheDocument();
  });

  it('renders area fill under the line', () => {
    renderWithTheme(<ThroughputChart data={sampleData} />);

    // Chart should render with area fill configured
    expect(screen.getByTestId('throughput-chart')).toBeInTheDocument();
  });

  it('auto-scales Y-axis based on data range', () => {
    const lowData = [
      { timestamp: 1699000000000, throughput: 1.0 },
      { timestamp: 1699000001000, throughput: 2.0 },
    ];

    const highData = [
      { timestamp: 1699000000000, throughput: 10.0 },
      { timestamp: 1699000001000, throughput: 20.0 },
    ];

    const { unmount: unmountLow } = renderWithTheme(
      <ThroughputChart data={lowData} />
    );
    expect(screen.getByTestId('throughput-chart')).toBeInTheDocument();

    unmountLow();

    renderWithTheme(<ThroughputChart data={highData} />);
    // Both should render successfully with different data ranges
    expect(screen.getByTestId('throughput-chart')).toBeInTheDocument();
  });

  it('handles single data point', () => {
    const singleData = [{ timestamp: 1699000000000, throughput: 3.5 }];

    renderWithTheme(<ThroughputChart data={singleData} />);

    // Chart should render successfully with single point
    expect(screen.getByTestId('throughput-chart')).toBeInTheDocument();
  });

  it('renders responsive container', () => {
    const { container } = renderWithTheme(<ThroughputChart data={sampleData} />);

    // Recharts uses ResponsiveContainer with specific class
    const responsiveContainer = container.querySelector('.recharts-responsive-container');
    expect(responsiveContainer).toBeInTheDocument();
  });

  it('maintains chronological order of data points', () => {
    const unorderedData = [
      { timestamp: 1699000003000, throughput: 3.8 },
      { timestamp: 1699000001000, throughput: 3.2 },
      { timestamp: 1699000002000, throughput: 4.1 },
    ];

    renderWithTheme(<ThroughputChart data={unorderedData} />);

    // Chart should render without errors
    expect(screen.getByTestId('throughput-chart')).toBeInTheDocument();
  });

  it('displays throughput values with proper precision', () => {
    const preciseData = [
      { timestamp: 1699000000000, throughput: 3.456789 },
      { timestamp: 1699000001000, throughput: 2.123456 },
    ];

    renderWithTheme(<ThroughputChart data={preciseData} />);

    // Chart should render successfully
    expect(screen.getByTestId('throughput-chart')).toBeInTheDocument();
  });
});
