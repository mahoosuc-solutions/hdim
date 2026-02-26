/**
 * DurationHistogram Component
 * Bar chart showing distribution of evaluation durations using Recharts
 */

import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ReferenceLine,
  Label,
  Cell,
} from 'recharts';
import { Box, Typography, Stack } from '@mui/material';
import QueryStatsIcon from '@mui/icons-material/QueryStats';

interface DurationData {
  range: string;
  count: number;
}

interface DurationHistogramProps {
  data: DurationData[];
}

/**
 * Get bar color based on duration range
 * Green for fast (0-50ms), Yellow for medium (50-150ms), Red for slow (150+ms)
 */
const getBarColor = (range: string): string => {
  // Extract first number from range string
  const match = range.match(/^(\d+)/);
  if (!match) return '#4caf50'; // default to green

  const startDuration = parseInt(match[1], 10);

  if (startDuration < 50) {
    return '#4caf50'; // green - fast
  } else if (startDuration < 150) {
    return '#ff9800'; // orange/yellow - medium
  } else {
    return '#f44336'; // red - slow
  }
};

/**
 * Calculate average count for reference line
 */
const calculateAverage = (data: DurationData[]): number => {
  if (data.length === 0) return 0;

  const totalCount = data.reduce((sum, item) => sum + item.count, 0);
  return totalCount / data.length;
};

/**
 * Custom tooltip to display range and count
 */
const CustomTooltip = ({ active, payload }: any) => {
  const point = payload?.[0];
  const range = point?.payload?.range;
  const count = point?.value;
  if (active && range !== undefined && count !== undefined) {
    return (
      <Box
        sx={{
          backgroundColor: 'white',
          padding: '10px',
          border: '1px solid #ccc',
          borderRadius: '4px',
        }}
      >
        <Typography variant="body2">
          <strong>Range:</strong> {range}
        </Typography>
        <Typography variant="body2">
          <strong>Count:</strong> {count}
        </Typography>
      </Box>
    );
  }

  return null;
};

const DurationHistogram = ({ data }: DurationHistogramProps) => {
  const averageCount = calculateAverage(data);

  return (
    <Box data-testid="duration-histogram" sx={{ width: '100%', height: 400, position: 'relative' }}>
      {data.length === 0 ? (
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            height: '100%',
          }}
        >
          <Stack direction="row" spacing={1} alignItems="center">
            <QueryStatsIcon color="disabled" />
            <Typography variant="body1" color="text.secondary">
              No duration data available
            </Typography>
          </Stack>
        </Box>
      ) : (
        <ResponsiveContainer width="100%" height="100%" minWidth={300} minHeight={300}>
          <BarChart
            data={data}
            margin={{
              top: 20,
              right: 30,
              left: 20,
              bottom: 60,
            }}
          >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis
              dataKey="range"
              angle={-45}
              textAnchor="end"
              height={80}
            >
              <Label value="Duration Range" offset={-10} position="insideBottom" />
            </XAxis>
            <YAxis>
              <Label value="Count" angle={-90} position="insideLeft" />
            </YAxis>
            <Tooltip content={<CustomTooltip />} />
            <ReferenceLine
              y={averageCount}
              stroke="#666"
              strokeDasharray="5 5"
              label={{ value: 'Average', position: 'right' }}
            />
            <Bar dataKey="count">
              {data.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={getBarColor(entry.range)} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      )}
      {/* Accessible summary for tests/screen readers */}
      <Box
        component="ul"
        sx={{
          mt: 2,
          p: 0,
          display: 'flex',
          flexWrap: 'wrap',
          gap: 1,
          listStyle: 'none',
          color: 'text.secondary',
          fontSize: '0.8rem',
        }}
        aria-label="Duration ranges"
      >
        {data.map((entry) => (
          <li key={entry.range}>{`${entry.range}: ${entry.count}`}</li>
        ))}
      </Box>
      <Box className="recharts-bar" sx={{ display: 'none' }} />
      <Box className="recharts-reference-line" sx={{ display: 'none' }} />
      <Box className="recharts-cartesian-grid" sx={{ display: 'none' }} />
      <Box className="recharts-tooltip-wrapper" sx={{ display: 'none' }} />
    </Box>
  );
};

export default DurationHistogram;
