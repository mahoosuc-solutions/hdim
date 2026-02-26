/**
 * TrendsChart component
 *
 * Display historical analytics and trends over time using Recharts
 */

import {
  LineChart,
  Line,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { format } from 'date-fns';
import type { BatchProgressEvent } from '../types/events';

export interface TrendsChartProps {
  batches: BatchProgressEvent[];
  metric: 'successRate' | 'avgDuration' | 'complianceRate';
  timeRange?: 'day' | 'week' | 'month' | 'all';
  chartType?: 'line' | 'area';
}

interface ChartDataPoint {
  timestamp: number;
  value: number;
  formattedDate: string;
}

/**
 * Calculate success rate from batch data
 */
const calculateSuccessRate = (batch: BatchProgressEvent): number => {
  if (batch.completedCount === 0) return 0;
  return (batch.successCount / batch.completedCount) * 100;
};

/**
 * Get metric value from batch based on selected metric
 */
const getMetricValue = (
  batch: BatchProgressEvent,
  metric: 'successRate' | 'avgDuration' | 'complianceRate'
): number => {
  switch (metric) {
    case 'successRate':
      return calculateSuccessRate(batch);
    case 'avgDuration':
      return batch.avgDurationMs;
    case 'complianceRate':
      return batch.cumulativeComplianceRate;
    default:
      return 0;
  }
};

/**
 * Filter batches by time range
 */
const filterByTimeRange = (
  batches: BatchProgressEvent[],
  timeRange?: 'day' | 'week' | 'month' | 'all'
): BatchProgressEvent[] => {
  if (!timeRange || timeRange === 'all') {
    return batches;
  }

  const now = Date.now();
  const oneDay = 24 * 60 * 60 * 1000;
  const cutoffTimes = {
    day: now - oneDay,
    week: now - 7 * oneDay,
    month: now - 30 * oneDay,
  };

  const cutoff = cutoffTimes[timeRange];
  return batches.filter((batch) => batch.timestamp >= cutoff);
};

/**
 * Format timestamp as readable date
 */
const formatDate = (timestamp: number, timeRange?: string): string => {
  const date = new Date(timestamp);

  // For day view, show time
  if (timeRange === 'day') {
    return format(date, 'HH:mm');
  }

  // For week view, show day and time
  if (timeRange === 'week') {
    return format(date, 'MM/dd HH:mm');
  }

  // For month and all view, show date
  return format(date, 'MM/dd/yy');
};

/**
 * Get metric label and unit
 */
const getMetricLabel = (
  metric: 'successRate' | 'avgDuration' | 'complianceRate'
): { label: string; unit: string } => {
  switch (metric) {
    case 'successRate':
      return { label: 'Success Rate', unit: '%' };
    case 'avgDuration':
      return { label: 'Avg Duration', unit: 'ms' };
    case 'complianceRate':
      return { label: 'Compliance Rate', unit: '%' };
    default:
      return { label: '', unit: '' };
  }
};

/**
 * Format Y-axis tick with appropriate unit
 */
const formatYAxisTick = (
  value: number,
  metric: 'successRate' | 'avgDuration' | 'complianceRate'
): string => {
  const { unit } = getMetricLabel(metric);

  if (metric === 'avgDuration') {
    return `${value}${unit}`;
  }

  // For percentages, format with 1 decimal place
  return `${value.toFixed(1)}${unit}`;
};

/**
 * Custom tooltip component
 */
const CustomTooltip = ({
  active,
  payload,
  metric,
}: any) => {
  const data = payload?.[0]?.payload;
  if (active && data) {
    const { label, unit } = getMetricLabel(metric);

    return (
      <div
        style={{
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          border: '1px solid #ccc',
          padding: '10px',
          borderRadius: '4px',
        }}
      >
        <p style={{ margin: 0, fontWeight: 'bold', fontSize: '12px' }}>
          {data.formattedDate}
        </p>
        <p style={{ margin: '4px 0 0 0', color: '#1976d2', fontSize: '12px' }}>
          {label}: {data.value.toFixed(metric === 'avgDuration' ? 0 : 2)}
          {unit}
        </p>
      </div>
    );
  }
  return null;
};

/**
 * TrendsChart component
 *
 * Displays historical trends for batch metrics over time.
 * Supports multiple metrics, time ranges, and chart types.
 */
const TrendsChart: React.FC<TrendsChartProps> = ({
  batches,
  metric,
  timeRange = 'all',
  chartType = 'line',
}) => {
  // Filter data by time range
  const filteredBatches = filterByTimeRange(batches, timeRange);

  // Transform and sort data
  const chartData: ChartDataPoint[] = filteredBatches
    .map((batch) => ({
      timestamp: batch.timestamp,
      value: getMetricValue(batch, metric),
      formattedDate: formatDate(batch.timestamp, timeRange),
    }))
    .sort((a, b) => a.timestamp - b.timestamp);

  const { label } = getMetricLabel(metric);
  const yAxisLabel = `${label}`;

  // Chart component to use
  const ChartComponent = chartType === 'area' ? AreaChart : LineChart;

  return (
    <div data-testid="trends-chart" style={{ width: '100%', height: 300 }}>
      <ResponsiveContainer width="100%" height="100%" minWidth={300} minHeight={250}>
        <ChartComponent
          data={chartData}
          margin={{ top: 10, right: 30, left: 10, bottom: 20 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
          <XAxis
            dataKey="formattedDate"
            label={{ value: 'Time', position: 'insideBottom', offset: -10 }}
            stroke="#666"
            tick={{ fontSize: 12 }}
          />
          <YAxis
            label={{
              value: yAxisLabel,
              angle: -90,
              position: 'insideLeft',
            }}
            stroke="#666"
            tickFormatter={(value) => formatYAxisTick(value, metric)}
            domain={['auto', 'auto']}
            tick={{ fontSize: 12 }}
          />
          <Tooltip content={(props) => <CustomTooltip {...props} metric={metric} />} />

          {chartType === 'area' ? (
            <Area
              type="monotone"
              dataKey="value"
              fill="#1976d2"
              fillOpacity={0.3}
              stroke="#1976d2"
              strokeWidth={2}
              dot={{ fill: '#1976d2', r: 3 }}
              activeDot={{ r: 5 }}
            />
          ) : (
            <Line
              type="monotone"
              dataKey="value"
              stroke="#1976d2"
              strokeWidth={2}
              dot={{ fill: '#1976d2', r: 3 }}
              activeDot={{ r: 5 }}
            />
          )}
        </ChartComponent>
      </ResponsiveContainer>
    </div>
  );
};

export default TrendsChart;
