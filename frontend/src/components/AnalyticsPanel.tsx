/**
 * AnalyticsPanel Component
 * Displays statistical analysis and insights for batch evaluation metrics
 */

import {
  Box,
  Card,
  CardContent,
  GridLegacy as Grid,
  Typography,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Chip,
  Stack,
  Divider,
} from '@mui/material';
import InsightsOutlinedIcon from '@mui/icons-material/InsightsOutlined';
import type { BatchProgressEvent } from '../types/events';
import {
  calculateMean,
  calculateMedian,
  calculateStdDev,
  calculatePercentile,
  detectOutliers,
} from '../utils/statistics';

interface AnalyticsPanelProps {
  batches: BatchProgressEvent[];
  metric: 'successRate' | 'complianceRate' | 'avgDuration' | 'throughput';
}

/**
 * Extract metric value from a batch based on the selected metric type
 */
function extractMetricValue(batch: BatchProgressEvent, metric: string): number {
  switch (metric) {
    case 'successRate':
      return batch.completedCount > 0
        ? (batch.successCount / batch.completedCount) * 100
        : 0;
    case 'complianceRate':
      return batch.cumulativeComplianceRate;
    case 'avgDuration':
      return batch.avgDurationMs;
    case 'throughput':
      return batch.currentThroughput;
    default:
      return 0;
  }
}

/**
 * Format a value based on the metric type
 */
function formatValue(value: number, metric: string): string {
  switch (metric) {
    case 'successRate':
    case 'complianceRate':
      return `${value.toFixed(1)}%`;
    case 'avgDuration':
      return `${value.toLocaleString('en-US', { maximumFractionDigits: 0 })} ms`;
    case 'throughput':
      return `${value.toFixed(1)}/s`;
    default:
      return value.toFixed(1);
  }
}

/**
 * Get display name for metric
 */
function getMetricDisplayName(metric: string): string {
  switch (metric) {
    case 'successRate':
      return 'Success Rate';
    case 'complianceRate':
      return 'Compliance Rate';
    case 'avgDuration':
      return 'Average Duration';
    case 'throughput':
      return 'Throughput';
    default:
      return metric;
  }
}

/**
 * Statistical summary card component
 */
function StatCard({
  title,
  value,
  subtitle,
}: {
  title: string;
  value: string;
  subtitle?: string;
}) {
  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="caption" color="text.secondary" gutterBottom>
          {title}
        </Typography>
        <Typography variant="h5" component="div" sx={{ fontWeight: 'bold', my: 1 }}>
          {value}
        </Typography>
        {subtitle && (
          <Typography variant="caption" color="text.secondary">
            {subtitle}
          </Typography>
        )}
      </CardContent>
    </Card>
  );
}

export function AnalyticsPanel({ batches, metric }: AnalyticsPanelProps) {
  // Handle empty batches
  if (batches.length === 0) {
    return (
      <Box sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Analytics
        </Typography>
        <Stack direction="row" spacing={1} alignItems="center" color="text.secondary">
          <InsightsOutlinedIcon fontSize="small" />
          <Typography>No data available</Typography>
        </Stack>
      </Box>
    );
  }

  // Extract metric values from all batches
  const values = batches.map((batch) => extractMetricValue(batch, metric));

  // Calculate statistics
  const mean = calculateMean(values);
  const median = calculateMedian(values);
  const stdDev = calculateStdDev(values);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min;
  const cv = mean !== 0 ? (stdDev / mean) * 100 : 0;

  // Calculate percentiles
  const q1 = calculatePercentile(values, 25);
  const q3 = calculatePercentile(values, 75);
  const iqr = q3 - q1;

  // Detect outliers
  const outliers = detectOutliers(values);

  return (
    <Box sx={{ p: 3 }}>
      {/* Header with Metric Selector */}
      <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 3 }}>
        <Typography variant="h6">Analytics - {getMetricDisplayName(metric)}</Typography>
        <FormControl size="small" sx={{ minWidth: 200 }}>
          <InputLabel id="metric-select-label">Metric</InputLabel>
          <Select
            labelId="metric-select-label"
            value={metric}
            label="Metric"
            aria-label="metric selector"
          >
            <MenuItem value="successRate">Success Rate</MenuItem>
            <MenuItem value="complianceRate">Compliance Rate</MenuItem>
            <MenuItem value="avgDuration">Average Duration</MenuItem>
            <MenuItem value="throughput">Throughput</MenuItem>
          </Select>
        </FormControl>
      </Stack>

      {/* Statistical Summary Cards */}
      <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 'bold' }}>
        Statistical Summary
      </Typography>
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard title="Mean" value={formatValue(mean, metric)} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard title="Median" value={formatValue(median, metric)} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard title="Std Dev" value={formatValue(stdDev, metric)} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Coefficient of Variation"
            value={`${cv.toFixed(1)}%`}
            subtitle="Relative variability"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard title="Min" value={formatValue(min, metric)} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard title="Max" value={formatValue(max, metric)} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard title="Range" value={formatValue(range, metric)} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Sample Size"
            value={batches.length.toString()}
            subtitle="Number of batches"
          />
        </Grid>
      </Grid>

      <Divider sx={{ my: 3 }} />

      {/* Percentiles */}
      <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 'bold' }}>
        Percentiles
      </Typography>
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={4}>
          <StatCard
            title="25th Percentile (Q1)"
            value={formatValue(q1, metric)}
            subtitle="Lower quartile"
          />
        </Grid>
        <Grid item xs={12} sm={4}>
          <StatCard
            title="75th Percentile (Q3)"
            value={formatValue(q3, metric)}
            subtitle="Upper quartile"
          />
        </Grid>
        <Grid item xs={12} sm={4}>
          <StatCard
            title="IQR"
            value={formatValue(iqr, metric)}
            subtitle="Interquartile Range"
          />
        </Grid>
      </Grid>

      <Divider sx={{ my: 3 }} />

      {/* Outliers Section */}
      <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 'bold' }}>
        Outliers
      </Typography>
      {outliers.length === 0 ? (
        <Typography color="text.secondary">No outliers detected</Typography>
      ) : (
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
          {outliers.map((outlier) => {
            const batch = batches[outlier.index];
            const isLow = outlier.type === 'low';
            return (
              <Chip
                key={batch.batchId}
                label={`${batch.batchId}: ${formatValue(outlier.value, metric)}`}
                color={isLow ? 'error' : 'success'}
                variant="outlined"
                size="small"
                sx={{
                  '&.MuiChip-colorError': {
                    borderColor: 'error.main',
                    color: 'error.main',
                  },
                  '&.MuiChip-colorSuccess': {
                    borderColor: 'success.main',
                    color: 'success.main',
                  },
                }}
              />
            );
          })}
        </Stack>
      )}
    </Box>
  );
}
