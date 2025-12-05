/**
 * Performance Metrics Panel
 *
 * Aggregates all visualization components into a comprehensive dashboard panel
 */

import { useState, useEffect } from 'react';
import { GridLegacy as Grid, Paper, Typography, Box } from '@mui/material';
import { BatchProgressBar } from './BatchProgressBar';
import { ComplianceGauge } from './ComplianceGauge';
import ThroughputChart from './ThroughputChart';
import DurationHistogram from './DurationHistogram';
import type { BatchProgressEvent } from '../types/events';

interface ThroughputDataPoint {
  timestamp: number;
  throughput: number;
}

interface DurationData {
  range: string;
  count: number;
}

interface PerformanceMetricsPanelProps {
  batchProgress: BatchProgressEvent | null;
}

export const PerformanceMetricsPanel = ({ batchProgress }: PerformanceMetricsPanelProps) => {
  // State for chart data (accumulated over time)
  const [throughputData, setThroughputData] = useState<ThroughputDataPoint[]>([]);
  const [durationData, setDurationData] = useState<DurationData[]>([]);

  // Update throughput data when batch progress changes
  useEffect(() => {
    if (batchProgress && batchProgress.currentThroughput > 0) {
      setThroughputData(prev => {
        // Only add if timestamp is different from last entry
        if (prev.length > 0 && prev[prev.length - 1].timestamp === batchProgress.timestamp) {
          return prev;
        }

        const newData = [
          ...prev,
          {
            timestamp: batchProgress.timestamp,
            throughput: batchProgress.currentThroughput,
          }
        ];
        // Keep last 20 data points
        return newData.slice(-20);
      });
    }
  }, [batchProgress?.timestamp, batchProgress?.currentThroughput]); // More specific dependencies

  // Generate duration histogram data from average duration
  useEffect(() => {
    if (batchProgress && batchProgress.avgDurationMs > 0 && batchProgress.completedCount > 0) {
      // Create synthetic histogram data based on average duration
      // In a real implementation, this would come from the backend
      const avgDuration = batchProgress.avgDurationMs;
      const distribution = [
        { range: '0-50ms', count: avgDuration < 50 ? batchProgress.completedCount * 0.6 : batchProgress.completedCount * 0.2 },
        { range: '50-100ms', count: avgDuration < 100 ? batchProgress.completedCount * 0.3 : batchProgress.completedCount * 0.3 },
        { range: '100-150ms', count: avgDuration < 150 ? batchProgress.completedCount * 0.1 : batchProgress.completedCount * 0.3 },
        { range: '150-200ms', count: Math.max(0, batchProgress.completedCount * 0.15) },
        { range: '200+ms', count: Math.max(0, batchProgress.completedCount * 0.05) },
      ].map(item => ({ ...item, count: Math.round(item.count) }));

      setDurationData(distribution);
    }
  }, [batchProgress?.avgDurationMs, batchProgress?.completedCount]); // More specific dependencies

  if (!batchProgress) {
    return (
      <Box sx={{ p: 3, textAlign: 'center' }}>
        <Typography variant="body1" color="textSecondary">
          No active batch evaluation. Start a batch evaluation to see real-time metrics.
        </Typography>
      </Box>
    );
  }

  return (
    <Grid container spacing={3}>
      {/* Progress Bar - Full Width */}
      <Grid item xs={12}>
        <Paper elevation={2} sx={{ p: 3 }}>
          <BatchProgressBar progress={batchProgress} />
        </Paper>
      </Grid>

      {/* Compliance Gauge - Left Column */}
      <Grid item xs={12} md={4}>
        <Paper elevation={2} sx={{ p: 3, height: '100%' }}>
          <Typography variant="h6" gutterBottom>
            Compliance Rate
          </Typography>
          <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 250 }}>
            <ComplianceGauge
              complianceRate={batchProgress.cumulativeComplianceRate}
              label="Overall Compliance"
              numerator={batchProgress.numeratorCount}
              denominator={batchProgress.denominatorCount}
            />
          </Box>
        </Paper>
      </Grid>

      {/* Throughput Chart - Right Column */}
      <Grid item xs={12} md={8}>
        <Paper elevation={2} sx={{ p: 3, height: '100%' }}>
          <Typography variant="h6" gutterBottom>
            Evaluation Throughput
          </Typography>
          <ThroughputChart data={throughputData} />
        </Paper>
      </Grid>

      {/* Duration Histogram - Full Width */}
      <Grid item xs={12}>
        <Paper elevation={2} sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Evaluation Duration Distribution
          </Typography>
          <Box sx={{ minHeight: 300 }}>
            <DurationHistogram data={durationData} />
          </Box>
        </Paper>
      </Grid>

      {/* Summary Statistics */}
      <Grid item xs={12}>
        <Paper elevation={2} sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Batch Summary
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={6} sm={3}>
              <Typography variant="body2" color="textSecondary">
                Average Duration
              </Typography>
              <Typography variant="h6">
                {batchProgress.avgDurationMs.toFixed(1)}ms
              </Typography>
            </Grid>
            <Grid item xs={6} sm={3}>
              <Typography variant="body2" color="textSecondary">
                Success Rate
              </Typography>
              <Typography variant="h6">
                {((batchProgress.successCount / batchProgress.completedCount) * 100).toFixed(1)}%
              </Typography>
            </Grid>
            <Grid item xs={6} sm={3}>
              <Typography variant="body2" color="textSecondary">
                Elapsed Time
              </Typography>
              <Typography variant="h6">
                {Math.floor(batchProgress.elapsedTimeMs / 1000)}s
              </Typography>
            </Grid>
            <Grid item xs={6} sm={3}>
              <Typography variant="body2" color="textSecondary">
                Batch ID
              </Typography>
              <Typography variant="body2" sx={{ wordBreak: 'break-all' }}>
                {batchProgress.batchId.substring(0, 8)}...
              </Typography>
            </Grid>
          </Grid>
        </Paper>
      </Grid>
    </Grid>
  );
};
