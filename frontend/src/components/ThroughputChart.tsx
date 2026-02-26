/**
 * ThroughputChart component
 *
 * Real-time line chart showing evaluation throughput over time using Recharts
 */

import {
  LineChart,
  Line,
  Area,
  AreaChart,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ComposedChart,
} from 'recharts';
import { format } from 'date-fns';

export interface ThroughputDataPoint {
  timestamp: number;
  throughput: number;
}

interface ThroughputChartProps {
  data: ThroughputDataPoint[];
}

/**
 * Format timestamp as HH:mm:ss
 */
const formatTime = (timestamp: number): string => {
  return format(new Date(timestamp), 'HH:mm:ss');
};

/**
 * Custom tooltip component for the chart
 */
const CustomTooltip = ({ active, payload }: any) => {
  const data = payload?.[0]?.payload;
  if (active && data) {
    return (
      <div
        style={{
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          border: '1px solid #ccc',
          padding: '10px',
          borderRadius: '4px',
        }}
      >
        <p style={{ margin: 0, fontWeight: 'bold' }}>
          {formatTime(data.timestamp)}
        </p>
        <p style={{ margin: '4px 0 0 0', color: '#1976d2' }}>
          Throughput: {data.throughput.toFixed(2)} eval/s
        </p>
      </div>
    );
  }
  return null;
};

/**
 * ThroughputChart component
 *
 * Displays a real-time line chart with area fill showing throughput over time.
 * Automatically limits to the last 20 data points and formats timestamps.
 */
const ThroughputChart: React.FC<ThroughputChartProps> = ({ data }) => {
  // Limit to last 20 data points
  const limitedData = data.slice(-20);

  return (
    <div data-testid="throughput-chart" style={{ width: '100%', height: 300 }}>
      <ResponsiveContainer width="100%" height="100%" minWidth={300} minHeight={250}>
        <ComposedChart
          data={limitedData}
          margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
          <XAxis
            dataKey="timestamp"
            tickFormatter={formatTime}
            label={{ value: 'Time', position: 'insideBottom', offset: -5 }}
            stroke="#666"
          />
          <YAxis
            label={{
              value: 'Throughput (eval/s)',
              angle: -90,
              position: 'insideLeft',
            }}
            stroke="#666"
            domain={['auto', 'auto']}
          />
          <Tooltip content={<CustomTooltip />} />
          <Area
            type="monotone"
            dataKey="throughput"
            fill="#1976d2"
            fillOpacity={0.2}
            stroke="none"
          />
          <Line
            type="monotone"
            dataKey="throughput"
            stroke="#1976d2"
            strokeWidth={2}
            dot={{ fill: '#1976d2', r: 4 }}
            activeDot={{ r: 6 }}
          />
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  );
};

export default ThroughputChart;
