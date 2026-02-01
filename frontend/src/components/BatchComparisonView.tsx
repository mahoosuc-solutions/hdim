/**
 * BatchComparisonView component
 *
 * Side-by-side comparison of two batch evaluations
 */

import { useState, useMemo } from 'react';
import {
  Card,
  CardContent,
  Typography,
  Select,
  MenuItem,
  Button,
  Box,
  FormControl,
  InputLabel,
  Stack,
} from '@mui/material';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import type { BatchProgressEvent } from '../types/events';

export interface BatchComparisonViewProps {
  batches: BatchProgressEvent[];
  onClose?: () => void;
}

interface ComparisonMetric {
  label: string;
  valueA: number | string;
  valueB: number | string;
  diff: number | string;
  diffColor: string;
}

const BatchComparisonView: React.FC<BatchComparisonViewProps> = ({
  batches,
  onClose,
}) => {
  // All hooks must be called before any conditional returns
  const [selectedBatchA, setSelectedBatchA] = useState<string>('');
  const [selectedBatchB, setSelectedBatchB] = useState<string>('');

  const batchA = batches.find((b) => b.batchId === selectedBatchA);
  const batchB = batches.find((b) => b.batchId === selectedBatchB);

  // Calculate metrics - hook always called, returns empty array if no batches
  const metrics: ComparisonMetric[] = useMemo(() => {
    if (!batchA || !batchB) return [];

    const totalEvalsDiff = batchB.completedCount - batchA.completedCount;
    const successRateA = (batchA.successCount / batchA.completedCount) * 100;
    const successRateB = (batchB.successCount / batchB.completedCount) * 100;
    const successRateDiff = successRateB - successRateA;
    const durationDiff = batchB.avgDurationMs - batchA.avgDurationMs;
    const complianceDiff =
      batchB.cumulativeComplianceRate - batchA.cumulativeComplianceRate;

    return [
      {
        label: 'Total Evaluations',
        valueA: batchA.completedCount,
        valueB: batchB.completedCount,
        diff: `${totalEvalsDiff >= 0 ? '+' : ''}${totalEvalsDiff}`,
        diffColor: totalEvalsDiff >= 0 ? 'green' : 'red',
      },
      {
        label: 'Success Rate',
        valueA: `${successRateA.toFixed(1)}%`,
        valueB: `${successRateB.toFixed(1)}%`,
        diff: `${successRateDiff >= 0 ? '+' : ''}${successRateDiff.toFixed(1)}%`,
        diffColor: successRateDiff >= 0 ? 'green' : 'red',
      },
      {
        label: 'Average Duration',
        valueA: `${batchA.avgDurationMs.toFixed(1)} ms`,
        valueB: `${batchB.avgDurationMs.toFixed(1)} ms`,
        diff: `${durationDiff >= 0 ? '+' : ''}${durationDiff.toFixed(1)} ms`,
        diffColor: durationDiff <= 0 ? 'green' : 'red', // Lower is better
      },
      {
        label: 'Compliance Rate',
        valueA: `${batchA.cumulativeComplianceRate.toFixed(1)}%`,
        valueB: `${batchB.cumulativeComplianceRate.toFixed(1)}%`,
        diff: `${complianceDiff >= 0 ? '+' : ''}${complianceDiff.toFixed(1)}%`,
        diffColor: complianceDiff >= 0 ? 'green' : 'red',
      },
    ];
  }, [batchA, batchB]);

  // Chart data - hook always called, returns empty array if no batches
  const chartData = useMemo(() => {
    if (!batchA || !batchB) return [];

    return [
      {
        metric: 'Success Rate',
        'Batch A': (batchA.successCount / batchA.completedCount) * 100,
        'Batch B': (batchB.successCount / batchB.completedCount) * 100,
      },
      {
        metric: 'Compliance',
        'Batch A': batchA.cumulativeComplianceRate,
        'Batch B': batchB.cumulativeComplianceRate,
      },
      {
        metric: 'Throughput',
        'Batch A': batchA.currentThroughput,
        'Batch B': batchB.currentThroughput,
      },
    ];
  }, [batchA, batchB]);

  // NOW we can do conditional returns after all hooks are called
  if (batches.length < 2) {
    return (
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Batch Comparison
          </Typography>
          <Typography color="textSecondary">
            At least 2 batches are required for comparison
          </Typography>
        </CardContent>
      </Card>
    );
  }

  const handleClearA = () => {
    setSelectedBatchA('');
  };

  const handleClearB = () => {
    setSelectedBatchB('');
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
          <Typography variant="h6">Batch Comparison</Typography>
          {onClose && (
            <Button onClick={onClose} variant="outlined" size="small">
              Close
            </Button>
          )}
        </Box>

        <Stack spacing={3}>
          {/* Batch Selectors */}
          <Box
            sx={{
              display: 'flex',
              flexDirection: { xs: 'column', md: 'row' },
              gap: 3,
            }}
          >
            {/* Batch A Selector */}
            <Box sx={{ flex: 1 }}>
              <FormControl fullWidth>
                <InputLabel>Batch A</InputLabel>
                <Select
                  value={selectedBatchA}
                  onChange={(e) => setSelectedBatchA(e.target.value)}
                  label="Batch A"
                >
                  {batches.map((batch) => (
                    <MenuItem
                      key={batch.batchId}
                      value={batch.batchId}
                      disabled={batch.batchId === selectedBatchB}
                    >
                      {batch.batchId}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <Button
                onClick={handleClearA}
                variant="text"
                size="small"
                sx={{ mt: 1 }}
                disabled={!selectedBatchA}
              >
                Clear
              </Button>
            </Box>

            {/* Batch B Selector */}
            <Box sx={{ flex: 1 }}>
              <FormControl fullWidth>
                <InputLabel>Batch B</InputLabel>
                <Select
                  value={selectedBatchB}
                  onChange={(e) => setSelectedBatchB(e.target.value)}
                  label="Batch B"
                >
                  {batches.map((batch) => (
                    <MenuItem
                      key={batch.batchId}
                      value={batch.batchId}
                      disabled={batch.batchId === selectedBatchA}
                    >
                      {batch.batchId}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <Button
                onClick={handleClearB}
                variant="text"
                size="small"
                sx={{ mt: 1 }}
                disabled={!selectedBatchB}
              >
                Clear
              </Button>
            </Box>
          </Box>

          {/* Comparison Metrics */}
          {batchA && batchB && (
            <>
              <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>
                Comparison Metrics
              </Typography>

              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: {
                    xs: '1fr',
                    sm: 'repeat(2, 1fr)',
                    md: 'repeat(4, 1fr)',
                  },
                  gap: 2,
                }}
              >
                {metrics.map((metric) => (
                  <Card variant="outlined" key={metric.label}>
                    <CardContent>
                      <Typography variant="subtitle2" color="textSecondary">
                        {metric.label}
                      </Typography>
                      <Typography variant="body2" sx={{ mt: 1 }}>
                        A: {metric.valueA}
                      </Typography>
                      <Typography variant="body2">
                        B: {metric.valueB}
                      </Typography>
                      <Typography
                        variant="body2"
                        sx={{
                          mt: 1,
                          fontWeight: 'bold',
                          color: metric.diffColor,
                        }}
                      >
                        Diff: {metric.diff}
                      </Typography>
                    </CardContent>
                  </Card>
                ))}
              </Box>

              {/* Comparison Chart */}
              <Box>
                <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>
                  Visual Comparison
                </Typography>
                <Box sx={{ width: '100%', height: 300, mt: 2 }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={chartData}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="metric" />
                      <YAxis />
                      <Tooltip />
                      <Legend />
                      <Bar dataKey="Batch A" fill="#1976d2" />
                      <Bar dataKey="Batch B" fill="#2e7d32" />
                    </BarChart>
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

export default BatchComparisonView;
