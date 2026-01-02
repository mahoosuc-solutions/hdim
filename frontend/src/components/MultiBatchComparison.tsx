/**
 * MultiBatchComparison component
 *
 * Compare 3 or more batches simultaneously with statistical insights
 */

import { useState, useMemo } from 'react';
import {
  Card,
  CardContent,
  Typography,
  Button,
  Box,
  Checkbox,
  FormControlLabel,
  Stack,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableCell,
  TableSortLabel,
  Chip,
} from '@mui/material';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import type { BatchProgressEvent } from '../types/events';

export interface MultiBatchComparisonProps {
  batches: BatchProgressEvent[];
  maxBatches?: number;
  onClose?: () => void;
}

type MetricType = 'successRate' | 'compliance' | 'duration';

interface StatisticalData {
  mean: number;
  median: number;
  stdDev: number;
  min: number;
  max: number;
}

interface MetricRow {
  label: string;
  values: number[];
  stats: StatisticalData;
  formatter: (value: number) => string;
  lowerIsBetter?: boolean;
}

const MultiBatchComparison: React.FC<MultiBatchComparisonProps> = ({
  batches,
  maxBatches = 5,
  onClose,
}) => {
  const [selectedBatchIds, setSelectedBatchIds] = useState<string[]>([]);
  const [sortColumn, setSortColumn] = useState<string>('');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');
  const [chartMetric, setChartMetric] = useState<MetricType>('successRate');

  // Show empty state if fewer than 3 batches
  if (batches.length < 3) {
    return (
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Multi-Batch Comparison
          </Typography>
          <Typography color="textSecondary">
            At least 3 batches are required for multi-batch comparison
          </Typography>
        </CardContent>
      </Card>
    );
  }

  const selectedBatches = batches.filter((b) =>
    selectedBatchIds.includes(b.batchId)
  );

  // Calculate statistical metrics
  const calculateStats = (values: number[]): StatisticalData => {
    if (values.length === 0) {
      return { mean: 0, median: 0, stdDev: 0, min: 0, max: 0 };
    }

    const sorted = [...values].sort((a, b) => a - b);
    const mean = values.reduce((sum, val) => sum + val, 0) / values.length;
    const median =
      values.length % 2 === 0
        ? (sorted[values.length / 2 - 1] + sorted[values.length / 2]) / 2
        : sorted[Math.floor(values.length / 2)];

    const variance =
      values.reduce((sum, val) => sum + Math.pow(val - mean, 2), 0) /
      values.length;
    const stdDev = Math.sqrt(variance);

    return {
      mean,
      median,
      stdDev,
      min: sorted[0],
      max: sorted[sorted.length - 1],
    };
  };

  // Prepare metrics data
  const metricsData: MetricRow[] = useMemo(() => {
    if (selectedBatches.length < 3) return [];

    const successRates = selectedBatches.map(
      (b) => (b.successCount / b.completedCount) * 100
    );
    const complianceRates = selectedBatches.map(
      (b) => b.cumulativeComplianceRate
    );
    const durations = selectedBatches.map((b) => b.avgDurationMs);
    const throughputs = selectedBatches.map((b) => b.currentThroughput);

    return [
      {
        label: 'Success Rate',
        values: successRates,
        stats: calculateStats(successRates),
        formatter: (val) => `${val.toFixed(1)}%`,
      },
      {
        label: 'Compliance',
        values: complianceRates,
        stats: calculateStats(complianceRates),
        formatter: (val) => `${val.toFixed(1)}%`,
      },
      {
        label: 'Avg Duration',
        values: durations,
        stats: calculateStats(durations),
        formatter: (val) => `${val.toFixed(1)} ms`,
        lowerIsBetter: true,
      },
      {
        label: 'Throughput',
        values: throughputs,
        stats: calculateStats(throughputs),
        formatter: (val) => `${val.toFixed(2)} eval/s`,
      },
    ];
  }, [selectedBatches]);

  // Check if value is outlier (>1 std dev from mean)
  const isOutlier = (value: number, stats: StatisticalData): boolean => {
    if (stats.stdDev === 0) return false;
    return Math.abs(value - stats.mean) > stats.stdDev;
  };

  // Get cell color based on value (best/worst)
  const getCellColor = (
    value: number,
    stats: StatisticalData,
    lowerIsBetter = false
  ): string => {
    if (stats.stdDev === 0) return 'transparent';

    const isBest = lowerIsBetter ? value === stats.min : value === stats.max;
    const isWorst = lowerIsBetter ? value === stats.max : value === stats.min;

    if (isBest) return 'rgb(76, 175, 80)'; // Green
    if (isWorst) return 'rgb(244, 67, 54)'; // Red
    return 'transparent';
  };

  // Prepare chart data
  const chartData = useMemo(() => {
    if (selectedBatches.length < 3) return [];

    return selectedBatches.map((batch) => {
      const successRate = (batch.successCount / batch.completedCount) * 100;
      return {
        timestamp: new Date(batch.timestamp).toLocaleDateString(),
        batchId: batch.batchId,
        successRate,
        compliance: batch.cumulativeComplianceRate,
        duration: batch.avgDurationMs,
      };
    });
  }, [selectedBatches]);

  const handleToggleBatch = (batchId: string) => {
    setSelectedBatchIds((prev) => {
      if (prev.includes(batchId)) {
        return prev.filter((id) => id !== batchId);
      } else if (prev.length < maxBatches) {
        return [...prev, batchId];
      }
      return prev;
    });
  };

  const handleSelectAll = () => {
    const idsToSelect = batches.slice(0, maxBatches).map((b) => b.batchId);
    setSelectedBatchIds(idsToSelect);
  };

  const handleDeselectAll = () => {
    setSelectedBatchIds([]);
  };

  const handleSort = (column: string) => {
    if (sortColumn === column) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortColumn(column);
      setSortDirection('asc');
    }
  };

  const exportToCSV = () => {
    if (metricsData.length === 0) return;

    // Build CSV content
    const headers = [
      'Metric',
      ...selectedBatches.map((b) => b.batchId),
      'Mean',
      'Median',
      'Std Dev',
      'Min',
      'Max',
    ];

    const rows = metricsData.map((metric) => {
      return [
        metric.label,
        ...metric.values.map((v) => metric.formatter(v)),
        metric.formatter(metric.stats.mean),
        metric.formatter(metric.stats.median),
        metric.stats.stdDev.toFixed(2),
        metric.formatter(metric.stats.min),
        metric.formatter(metric.stats.max),
      ];
    });

    const csvContent = [headers.join(','), ...rows.map((r) => r.join(','))].join(
      '\n'
    );

    // Trigger download
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `batch-comparison-${Date.now()}.csv`;
    link.style.display = 'none';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const getChartMetricKey = (): 'successRate' | 'compliance' | 'duration' => {
    return chartMetric;
  };

  return (
    <Card>
      <CardContent>
        <Box
          sx={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            mb: 3,
          }}
        >
          <Typography variant="h6">Multi-Batch Comparison</Typography>
          {onClose && (
            <Button onClick={onClose} variant="outlined" size="small">
              Close
            </Button>
          )}
        </Box>

        <Stack spacing={3}>
          {/* Batch Selection */}
          <Box>
            <Box
              sx={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                mb: 2,
              }}
            >
              <Typography variant="subtitle1">Select Batches</Typography>
              <Box sx={{ display: 'flex', gap: 1 }}>
                <Button
                  onClick={handleSelectAll}
                  variant="outlined"
                  size="small"
                >
                  Select All
                </Button>
                <Button
                  onClick={handleDeselectAll}
                  variant="outlined"
                  size="small"
                >
                  Deselect All
                </Button>
              </Box>
            </Box>

            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
                gap: 1,
              }}
            >
              {batches.map((batch) => {
                const isSelected = selectedBatchIds.includes(batch.batchId);
                const isDisabled =
                  !isSelected && selectedBatchIds.length >= maxBatches;

                return (
                  <FormControlLabel
                    key={batch.batchId}
                    control={
                      <Checkbox
                        checked={isSelected}
                        onChange={() => handleToggleBatch(batch.batchId)}
                        disabled={isDisabled}
                      />
                    }
                    label={batch.batchId}
                  />
                );
              })}
            </Box>

            <Box sx={{ mt: 2 }}>
              <Typography variant="body2" color="textSecondary">
                {selectedBatchIds.length} batches selected
              </Typography>
              {selectedBatchIds.length >= maxBatches && (
                <Typography variant="body2" color="warning.main">
                  Maximum of {maxBatches} batches reached
                </Typography>
              )}
            </Box>
          </Box>

          {/* Show message if less than 3 selected */}
          {selectedBatches.length > 0 && selectedBatches.length < 3 && (
            <Box sx={{ p: 2, bgcolor: 'info.light', borderRadius: 1 }}>
              <Typography variant="body2">
                Please select at least 3 batches to view comparison
              </Typography>
            </Box>
          )}

          {/* Comparison Table */}
          {selectedBatches.length >= 3 && (
            <>
              <Box>
                <Box
                  sx={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    mb: 2,
                  }}
                >
                  <Typography variant="h6">Comparison Table</Typography>
                  <Button
                    onClick={exportToCSV}
                    variant="contained"
                    size="small"
                  >
                    Export CSV
                  </Button>
                </Box>

                <Box sx={{ overflowX: 'auto' }}>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>
                          <TableSortLabel
                            active={sortColumn === 'metric'}
                            direction={sortDirection}
                            onClick={() => handleSort('metric')}
                          >
                            Metric
                          </TableSortLabel>
                        </TableCell>
                        {selectedBatches.map((batch) => (
                          <TableCell key={batch.batchId}>
                            <TableSortLabel
                              active={sortColumn === batch.batchId}
                              direction={sortDirection}
                              onClick={() => handleSort(batch.batchId)}
                            >
                              {batch.batchId}
                            </TableSortLabel>
                          </TableCell>
                        ))}
                        <TableCell>Mean</TableCell>
                        <TableCell>Median</TableCell>
                        <TableCell>Std Dev</TableCell>
                        <TableCell>Min</TableCell>
                        <TableCell>Max</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {metricsData.map((metric) => (
                        <TableRow key={metric.label}>
                          <TableCell>{metric.label}</TableCell>
                          {metric.values.map((value, idx) => {
                            const batch = selectedBatches[idx];
                            const cellColor = getCellColor(
                              value,
                              metric.stats,
                              metric.lowerIsBetter
                            );
                            const outlier = isOutlier(value, metric.stats);

                            return (
                              <TableCell
                                key={batch.batchId}
                                sx={{
                                  backgroundColor: cellColor,
                                  color:
                                    cellColor !== 'transparent'
                                      ? 'white'
                                      : 'inherit',
                                  fontWeight: outlier ? 'bold' : 'normal',
                                  position: 'relative',
                                }}
                                data-outlier={outlier}
                              >
                                {metric.formatter(value)}
                                {outlier && (
                                  <Chip
                                    label="!"
                                    size="small"
                                    sx={{
                                      position: 'absolute',
                                      top: 4,
                                      right: 4,
                                      width: 16,
                                      height: 16,
                                      fontSize: 10,
                                    }}
                                  />
                                )}
                              </TableCell>
                            );
                          })}
                          <TableCell>{metric.formatter(metric.stats.mean)}</TableCell>
                          <TableCell>
                            {metric.formatter(metric.stats.median)}
                          </TableCell>
                          <TableCell>{metric.stats.stdDev.toFixed(2)}</TableCell>
                          <TableCell>{metric.formatter(metric.stats.min)}</TableCell>
                          <TableCell>{metric.formatter(metric.stats.max)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </Box>
              </Box>

              {/* Multi-Series Chart */}
              <Box>
                <Typography variant="h6" gutterBottom>
                  Comparison Chart
                </Typography>

                <Box sx={{ mb: 2, display: 'flex', gap: 1 }}>
                  <Button
                    onClick={() => setChartMetric('successRate')}
                    variant={
                      chartMetric === 'successRate' ? 'contained' : 'outlined'
                    }
                    size="small"
                  >
                    Success Rate
                  </Button>
                  <Button
                    onClick={() => setChartMetric('compliance')}
                    variant={
                      chartMetric === 'compliance' ? 'contained' : 'outlined'
                    }
                    size="small"
                  >
                    Compliance
                  </Button>
                  <Button
                    onClick={() => setChartMetric('duration')}
                    variant={
                      chartMetric === 'duration' ? 'contained' : 'outlined'
                    }
                    size="small"
                  >
                    Duration
                  </Button>
                </Box>

                <Box sx={{ width: '100%', height: 300 }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={chartData}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="timestamp" />
                      <YAxis />
                      <Tooltip />
                      <Legend />
                      {selectedBatches.map((batch, idx) => {
                        const colors = [
                          '#1976d2',
                          '#2e7d32',
                          '#d32f2f',
                          '#f57c00',
                          '#7b1fa2',
                        ];
                        return (
                          <Line
                            key={batch.batchId}
                            type="monotone"
                            dataKey={getChartMetricKey()}
                            data={chartData.filter(
                              (d) => d.batchId === batch.batchId
                            )}
                            name={batch.batchId}
                            stroke={colors[idx % colors.length]}
                            strokeWidth={2}
                          />
                        );
                      })}
                    </LineChart>
                  </ResponsiveContainer>
                </Box>
              </Box>
            </>
          )}
        </Stack>
      </CardContent>
    </Card>
  );
};

export default MultiBatchComparison;
